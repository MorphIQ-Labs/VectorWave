package com.morphiqlabs.wavelet.extensions.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.DiscreteWavelet;

/**
 * Streaming-friendly facade for batch MODWT in AoS form (Array-of-Arrays),
 * delegating to SIMD SoA kernels per chunk.
 *
 * <p>Each call processes a block of samples for a batch of signals and returns AoS outputs.</p>
 *
 * <p>Boundary modes:</p>
 * <ul>
 *   <li>PERIODIC: Fast SIMD SoA kernels, no state across chunks.</li>
 *   <li>ZERO_PADDING and SYMMETRIC: SIMD streaming with left-history (ring buffer) per level, providing
 *       continuity across blocks and parity with whole-signal transforms.</li>
 * </ul>
 */
public final class BatchStreamingMODWT implements AutoCloseable {

    private final DiscreteWavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final int levels; // >= 1 for multi-level; 1 for single-level use

    // Precomputed per-level analysis filters (upsampled + 1/sqrt(2) scaling to match core)
    private double[][] levelLow;
    private double[][] levelHigh;

    // Per-level left-history buffers in SoA layout; allocated lazily when first used
    private double[][] historySoA; // [level][histLen * batch]
    private int[] historyLen;      // [level] histLen = filterLen(level)-1
    private boolean[] historyInit; // [level] initialized per current stream

    // Last observed batch size to manage history buffer sizes
    private int lastBatch = -1;

    private BatchStreamingMODWT(Builder b) {
        this.wavelet = b.wavelet;
        this.boundaryMode = b.boundaryMode;
        this.levels = b.levels;
        if (levels < 1) throw new IllegalArgumentException("levels must be >= 1");
        this.levelLow = null;
        this.levelHigh = null;
        this.historySoA = null;
        this.historyLen = null;
        this.historyInit = null;
    }

    /**
     * Processes one block of samples at a single level.
     *
     * @param block input AoS block [batch][length]
     * @return single-level AoS outputs (approx, detail)
     */
    public BatchMODWT.SingleLevelResult processSingleLevel(double[][] block) {
        ensureLevels(1);
        validateAoS(block);
        int batch = block.length;
        int n = block[0].length;

        if (boundaryMode == BoundaryMode.PERIODIC) {
            return BatchMODWT.singleLevelAoS(wavelet, block);
        }

        ensureFilters();
        ensureHistoryCapacity(batch);

        // Prepare SoA inputs/outputs
        double[] inSoA = new double[batch * n];
        double[] approxSoA = new double[batch * n];
        double[] detailSoA = new double[batch * n];
        BatchSIMDMODWT.convertToSoA(block, inSoA);

        int levelIndex = 0; // single-level
        int histLen = historyLen[levelIndex];
        if (!historyInit[levelIndex]) {
            if (boundaryMode == BoundaryMode.ZERO_PADDING) {
                historySoA[levelIndex] = new double[histLen * batch]; // zeros
            } else {
                historySoA[levelIndex] = new double[histLen * batch];
                // Fill symmetric history from current block
                fillSymmetricHistoryFromSoA(inSoA, historySoA[levelIndex], histLen, batch, n);
            }
            historyInit[levelIndex] = true;
        }

        BatchSIMDMODWT.generalBatchMODWTSoAWithScaledFiltersAndHistory(
                historySoA[levelIndex], histLen,
                inSoA, approxSoA, detailSoA,
                levelLow[levelIndex], levelHigh[levelIndex],
                batch, n);

        // Update history for next block
        updateHistoryFromSoA(historySoA[levelIndex], inSoA, histLen, batch, n);

        // Convert outputs to AoS
        double[][] approx = new double[batch][n];
        double[][] detail = new double[batch][n];
        BatchSIMDMODWT.convertFromSoA(approxSoA, approx);
        BatchSIMDMODWT.convertFromSoA(detailSoA, detail);
        return new BatchMODWT.SingleLevelResult(approx, detail);
    }

    /**
     * Processes one block of samples for multi-level decomposition.
     *
     * @param block input AoS block [batch][length]
     * @return multi-level AoS outputs (per-level details, final approximation)
     */
    public BatchMODWT.MultiLevelResult processMultiLevel(double[][] block) {
        validateAoS(block);
        int batch = block.length;
        int n = block[0].length;

        if (boundaryMode == BoundaryMode.PERIODIC) {
            return BatchMODWT.multiLevelAoS(wavelet, block, levels);
        }

        ensureFilters();
        ensureHistoryCapacity(batch);

        // SoA input for level 1
        double[] currentSoA = new double[batch * n];
        BatchSIMDMODWT.convertToSoA(block, currentSoA);

        // Outputs per level
        double[][][] detailsAoS = new double[levels][batch][n];
        double[] approxSoA = null;

        for (int L = 0; L < levels; L++) {
            int histLen = historyLen[L];
            if (!historyInit[L]) {
                if (boundaryMode == BoundaryMode.ZERO_PADDING) {
                    historySoA[L] = new double[histLen * batch];
                } else {
                    historySoA[L] = new double[histLen * batch];
                    fillSymmetricHistoryFromSoA(currentSoA, historySoA[L], histLen, batch, n);
                }
                historyInit[L] = true;
            }

            approxSoA = new double[batch * n];
            double[] detailSoA = new double[batch * n];

            BatchSIMDMODWT.generalBatchMODWTSoAWithScaledFiltersAndHistory(
                    historySoA[L], histLen,
                    currentSoA, approxSoA, detailSoA,
                    levelLow[L], levelHigh[L], batch, n);

            // Update history for this level using the level input (currentSoA)
            updateHistoryFromSoA(historySoA[L], currentSoA, histLen, batch, n);

            // Convert detail for this level to AoS
            BatchSIMDMODWT.convertFromSoA(detailSoA, detailsAoS[L]);

            // Next level consumes this level's approx
            currentSoA = approxSoA;
        }

        // Convert final approx to AoS
        double[][] approxAoS = new double[batch][n];
        BatchSIMDMODWT.convertFromSoA(approxSoA, approxAoS);
        return new BatchMODWT.MultiLevelResult(detailsAoS, approxAoS);
    }

    /**
     * Flushes the stream by emitting a synthetic tail block of length {@code tailLength}
     * using the configured boundary semantics. This is optional and generally not required
     * for MODWT's (t-l) orientation, but can be useful when a downstream pipeline expects
     * a fixed-length tail extension at end-of-stream.
     * <p>Only applicable for {@link BoundaryMode#ZERO_PADDING} and {@link BoundaryMode#SYMMETRIC}.
     * For PERIODIC, this method throws {@link UnsupportedOperationException}.</p>
     * <p>Constraint: {@code tailLength} must be ≤ the minimum history length across levels,
     * i.e. min(levelLow[j].length - 1). This ensures the symmetric tail can be synthesized
     * using the maintained history. For ZERO_PADDING, this limit is still enforced for
     * consistency.</p>
     *
     * @param tailLength number of synthetic samples to emit (≤ history length)
     * @return single-level AoS outputs for the tail block
     */
    public BatchMODWT.SingleLevelResult flushSingleLevel(int tailLength) {
        ensureLevels(1);
        if (boundaryMode == BoundaryMode.PERIODIC) {
            throw new UnsupportedOperationException("Flush is only applicable to ZERO_PADDING/SYMMETRIC");
        }
        if (tailLength <= 0) {
            return new BatchMODWT.SingleLevelResult(new double[0][0], new double[0][0]);
        }
        if (historySoA == null) throw new IllegalStateException("No prior blocks processed; cannot flush");
        int levelIndex = 0;
        if (!historyInit[levelIndex]) throw new IllegalStateException("History not initialized; process at least one block before flush");
        int histLen = historyLen[levelIndex];
        if (tailLength > histLen) {
            throw new IllegalArgumentException(String.format(
                "tailLength (%d) exceeds maximum allowed for this level (%d). " +
                "Use getMinFlushTailLength() to choose a valid tail length.",
                tailLength, histLen));
        }

        int batch = lastBatch;
        if (batch <= 0) throw new IllegalStateException("Unknown batch size; process a block first");

        // Build tail SoA block
        double[] tailSoA = new double[batch * tailLength];
        buildTailFromHistorySoA(historySoA[levelIndex], histLen, batch, tailLength, tailSoA, boundaryMode);

        // Compute outputs
        double[] approxSoA = new double[batch * tailLength];
        double[] detailSoA = new double[batch * tailLength];
        BatchSIMDMODWT.generalBatchMODWTSoAWithScaledFiltersAndHistory(
                historySoA[levelIndex], histLen,
                tailSoA, approxSoA, detailSoA,
                levelLow[levelIndex], levelHigh[levelIndex],
                batch, tailLength);

        // Convert to AoS
        double[][] approxAoS = new double[batch][tailLength];
        double[][] detailAoS = new double[batch][tailLength];
        BatchSIMDMODWT.convertFromSoA(approxSoA, approxAoS);
        BatchSIMDMODWT.convertFromSoA(detailSoA, detailAoS);
        return new BatchMODWT.SingleLevelResult(approxAoS, detailAoS);
    }

    /**
     * Flushes a multi-level stream by emitting a synthetic tail block of length {@code tailLength}
     * using ZERO_PADDING/SYMMETRIC semantics. See {@link #flushSingleLevel(int)} for constraints.
     *
     * @param tailLength number of synthetic samples to emit (≤ min history length across levels)
     * @return multi-level AoS outputs for the tail block
     */
    public BatchMODWT.MultiLevelResult flushMultiLevel(int tailLength) {
        if (boundaryMode == BoundaryMode.PERIODIC) {
            throw new UnsupportedOperationException("Flush is only applicable to ZERO_PADDING/SYMMETRIC");
        }
        if (tailLength <= 0) {
            return new BatchMODWT.MultiLevelResult(new double[levels][0][0], new double[0][0]);
        }
        ensureFilters();
        if (historySoA == null) throw new IllegalStateException("No prior blocks processed; cannot flush");
        for (int L = 0; L < levels; L++) {
            if (!historyInit[L]) throw new IllegalStateException("History not initialized at level " + (L+1));
        }
        int minHist = minHistoryLen();
        if (tailLength > minHist) {
            throw new IllegalArgumentException(String.format(
                "tailLength (%d) exceeds maximum allowed across levels (%d). " +
                "Use getMinFlushTailLength() to choose a valid tail length.",
                tailLength, minHist));
        }
        int batch = lastBatch;
        if (batch <= 0) throw new IllegalStateException("Unknown batch size; process a block first");

        // Build base-level tail SoA
        double[] currentSoA = new double[batch * tailLength];
        buildTailFromHistorySoA(historySoA[0], historyLen[0], batch, tailLength, currentSoA, boundaryMode);

        double[][][] detailsAoS = new double[levels][batch][tailLength];
        double[] approxSoA = null;
        for (int L = 0; L < levels; L++) {
            int histLen = historyLen[L];
            approxSoA = new double[batch * tailLength];
            double[] detailSoA = new double[batch * tailLength];
            BatchSIMDMODWT.generalBatchMODWTSoAWithScaledFiltersAndHistory(
                    historySoA[L], histLen,
                    currentSoA, approxSoA, detailSoA,
                    levelLow[L], levelHigh[L],
                    batch, tailLength);
            // Convert detail to AoS and move to next level
            BatchSIMDMODWT.convertFromSoA(detailSoA, detailsAoS[L]);
            currentSoA = approxSoA;
        }
        double[][] approxAoS = new double[batch][tailLength];
        BatchSIMDMODWT.convertFromSoA(approxSoA, approxAoS);
        return new BatchMODWT.MultiLevelResult(detailsAoS, approxAoS);
    }

    private void ensureLevels(int expected) {
        if (levels != expected) {
            throw new IllegalStateException("This instance is configured for levels=" + levels + ", expected=" + expected);
        }
    }

    private static void validateAoS(double[][] a) {
        if (a == null || a.length == 0) throw new IllegalArgumentException("block must be non-null");
        int n = a[0].length;
        if (n == 0) throw new IllegalArgumentException("block length must be > 0");
        for (int i = 1; i < a.length; i++) {
            if (a[i] == null || a[i].length != n) throw new IllegalArgumentException("all rows must have equal length");
        }
    }

    private void ensureFilters() {
        if (levelLow != null && levelHigh != null) return;
        levelLow = new double[levels][];
        levelHigh = new double[levels][];
        double[] baseLow = wavelet.lowPassDecomposition();
        double[] baseHigh = wavelet.highPassDecomposition();
        for (int L = 1; L <= levels; L++) {
            levelLow[L - 1] = com.morphiqlabs.wavelet.internal.ScalarOps
                    .upsampleAndScaleForIMODWTSynthesis(baseLow, L);
            levelHigh[L - 1] = com.morphiqlabs.wavelet.internal.ScalarOps
                    .upsampleAndScaleForIMODWTSynthesis(baseHigh, L);
        }
        historyLen = new int[levels];
        for (int i = 0; i < levels; i++) historyLen[i] = levelLow[i].length - 1;
        historySoA = new double[levels][];
        historyInit = new boolean[levels];
    }

    private void ensureHistoryCapacity(int batch) {
        if (lastBatch == batch && historySoA != null) return;
        // If first use, just record batch size; per-level buffers will be allocated lazily
        if (historySoA == null) { lastBatch = batch; return; }
        // If batch changes, we need to re-allocate histories for all levels
        for (int i = 0; i < levels; i++) {
            if (historyInit[i]) {
                // Reinitialize history buffer with new batch size, preserving zeros
                int histLen = historyLen[i];
                historySoA[i] = new double[histLen * batch];
                historyInit[i] = false; // will be re-initialized from next block input
            }
        }
        lastBatch = batch;
    }

    private static void fillSymmetricHistoryFromSoA(double[] inSoA, double[] histSoA,
                                                     int histLen, int batch, int n) {
        // hist positions 0..histLen-1 correspond to indices -histLen..-1
        for (int p = 0; p < histLen; p++) {
            int idx = p - histLen; // negative index
            int src = com.morphiqlabs.wavelet.util.MathUtils.symmetricBoundaryExtension(idx, n);
            // Copy SoA slice at time src to hist position p
            System.arraycopy(inSoA, src * batch, histSoA, p * batch, batch);
        }
    }

    private static void updateHistoryFromSoA(double[] histSoA, double[] inSoA,
                                             int histLen, int batch, int n) {
        if (histLen <= 0) return;
        if (n >= histLen) {
            // Copy last histLen time-slices from input into history
            int start = (n - histLen) * batch;
            System.arraycopy(inSoA, start, histSoA, 0, histLen * batch);
        } else {
            // Carry over last (histLen - n) from previous history, append all n from input
            int carry = histLen - n;
            // Shift existing history forward: hist[n .. histLen-1] -> hist[0 .. carry-1]
            System.arraycopy(histSoA, n * batch, histSoA, 0, carry * batch);
            // Append entire input block: in[0..n-1] -> hist[carry .. histLen-1]
            System.arraycopy(inSoA, 0, histSoA, carry * batch, n * batch);
        }
    }

    private int minHistoryLen() {
        int m = Integer.MAX_VALUE;
        for (int i = 0; i < levels; i++) m = Math.min(m, historyLen[i]);
        return m == Integer.MAX_VALUE ? 0 : m;
    }

    private static void buildTailFromHistorySoA(double[] histSoA, int histLen, int batch,
                                                 int tailLen, double[] tailSoA, BoundaryMode mode) {
        if (mode == BoundaryMode.ZERO_PADDING) {
            // already zero-initialized by default; ensure array cleared
            java.util.Arrays.fill(tailSoA, 0.0);
            return;
        }
        // SYMMETRIC: reflect the last histLen samples across the end boundary.
        // We require tailLen <= histLen. Build tail time-slices t=0..tailLen-1 from
        // source positions src = histLen-1 - t in histSoA (whole-sample symmetry first reflection).
        for (int t = 0; t < tailLen; t++) {
            int src = histLen - 1 - t; // src in [0, histLen)
            System.arraycopy(histSoA, src * batch, tailSoA, t * batch, batch);
        }
    }

    /**
     * Returns the minimum allowable tail length for flush across all configured levels.
     * Ensures filters are prepared and returns min(levelFilterLen - 1).
     *
     * @return minimum valid tail length across levels
     */
    public int getMinFlushTailLength() {
        ensureFilters();
        return minHistoryLen();
    }

    /**
     * Returns the history length (filterLen - 1) for a specific level (1-based).
     * This is the maximum tail length allowed for a single-level flush at that level.
     *
     * @param level 1-based level index
     * @return history length for the level
     */
    public int getHistoryLengthForLevel(int level) {
        ensureFilters();
        if (level < 1 || level > levels) {
            throw new IllegalArgumentException("level must be in [1," + levels + "]");
        }
        return historyLen[level - 1];
    }

    /**
     * Suggests a valid tail length for {@code flushSingleLevel/flushMultiLevel} for this instance.
     * For single-level configurations, returns the level-1 history length.
     * For multi-level configurations, returns the minimum history length across all levels.
     *
     * @return suggested tail length that satisfies history constraints
     */
    public int suggestFlushTailLength() {
        ensureFilters();
        if (levels == 1) return historyLen[0];
        return minHistoryLen();
    }

    @Override
    public void close() {
        // No-op for now; reserved for future buffer pools / thread-locals.
    }

    /** Builder for {@link BatchStreamingMODWT}. */
    public static final class Builder {
        private DiscreteWavelet wavelet;
        private BoundaryMode boundaryMode = BoundaryMode.PERIODIC;
        private int levels = 1; // default to single-level

        /** Creates a new builder. */
        public Builder() {}

        /**
         * Sets the wavelet to use.
         * @param w discrete wavelet
         * @return this builder
         */
        public Builder wavelet(DiscreteWavelet w) {
            this.wavelet = w; return this;
        }
        /**
         * Sets the boundary mode (PERIODIC, ZERO_PADDING, SYMMETRIC).
         * @param mode boundary handling mode
         * @return this builder
         */
        public Builder boundary(BoundaryMode mode) {
            this.boundaryMode = mode; return this;
        }
        /**
         * Sets the number of decomposition levels.
         * @param levels number of levels (≥ 1)
         * @return this builder
         */
        public Builder levels(int levels) {
            this.levels = levels; return this;
        }
        /**
         * Builds the {@link BatchStreamingMODWT} instance.
         * @return new BatchStreamingMODWT
         * @throws IllegalArgumentException if wavelet is not set
         */
        public BatchStreamingMODWT build() {
            if (wavelet == null) throw new IllegalArgumentException("wavelet must be set");
            return new BatchStreamingMODWT(this);
        }
    }
}
