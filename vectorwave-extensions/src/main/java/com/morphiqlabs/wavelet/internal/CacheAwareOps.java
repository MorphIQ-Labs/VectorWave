package com.morphiqlabs.wavelet.extensions.internal;

import com.morphiqlabs.wavelet.memory.AlignedMemoryPool;
import com.morphiqlabs.wavelet.memory.AlignedMemoryPool.PooledArray;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Cache-aware implementations of wavelet operations for large signals.
 *
 * <p>These implementations use blocking strategies to ensure data fits
 * in CPU cache levels, significantly improving performance for large
 * signals that exceed cache capacity.</p>
 */
final class CacheAwareOps {

    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();

    // Cache sizes (conservative estimates)
    private static final int L1_CACHE_SIZE = 32 * 1024;      // 32 KB L1 data cache
    private static final int L2_CACHE_SIZE = 256 * 1024;     // 256 KB L2 cache
    private static final int L3_CACHE_SIZE = 8 * 1024 * 1024; // 8 MB L3 cache

    // Block sizes in doubles (8 bytes each)
    private static final int L1_BLOCK_SIZE = L1_CACHE_SIZE / 8 / 4;  // Reserve 1/4 of L1
    private static final int L2_BLOCK_SIZE = L2_CACHE_SIZE / 8 / 4;  // Reserve 1/4 of L2
    private static final int L3_BLOCK_SIZE = L3_CACHE_SIZE / 8 / 4;  // Reserve 1/4 of L3

    // Prefetch distance in cache lines
    private static final int PREFETCH_DISTANCE = 8; // 8 cache lines = 512 bytes

    private CacheAwareOps() {
        // Utility class
    }

    /**
     * Cache-aware forward wavelet transform for large signals.
     * Uses multi-level blocking to fit in L1/L2/L3 caches.
     */
    public static void forwardTransformBlocked(double[] signal, double[] approx, double[] detail,
                                               double[] lowPass, double[] highPass,
                                               int signalLength, int filterLength) {
        int outputLength = signalLength / 2;

        // Choose block size based on signal size
        int blockSize = chooseBlockSize(signalLength);

        // Process in cache-friendly blocks
        for (int blockStart = 0; blockStart < outputLength; blockStart += blockSize) {
            int blockEnd = Math.min(blockStart + blockSize, outputLength);

            // Process this block
            processForwardBlock(signal, approx, detail, lowPass, highPass,
                    blockStart, blockEnd, signalLength, filterLength);
        }
    }

    /**
     * Cache-aware inverse wavelet transform.
     */
    public static void inverseTransformBlocked(double[] approx, double[] detail, double[] output,
                                               double[] lowPass, double[] highPass,
                                               int outputLength, int filterLength) {
        int inputLength = outputLength / 2;
        int blockSize = chooseBlockSize(outputLength);

        // Clear output first
        java.util.Arrays.fill(output, 0.0);

        // Process in blocks
        for (int blockStart = 0; blockStart < outputLength; blockStart += blockSize) {
            int blockEnd = Math.min(blockStart + blockSize, outputLength);

            // Process this block
            processInverseBlock(approx, detail, output, lowPass, highPass,
                    blockStart, blockEnd, inputLength, filterLength);
        }
    }

    /**
     * Multi-level decomposition with cache blocking.
     * Keeps intermediate results in cache between levels.
     */
    public static void multiLevelDecomposition(double[] signal, int levels,
                                               double[] lowPass, double[] highPass,
                                               int filterLength,
                                               double[][] approxCoeffs, double[][] detailCoeffs) {
        int currentLength = signal.length;
        double[] currentSignal = signal;

        // Use pooled arrays for intermediate results
        try (PooledArray pooledTemp1 = AlignedMemoryPool.allocate(signal.length / 2);
             PooledArray pooledTemp2 = AlignedMemoryPool.allocate(signal.length / 2)) {

            double[] temp1 = pooledTemp1.array();
            double[] temp2 = pooledTemp2.array();
            boolean useTemp1 = true;

            for (int level = 0; level < levels; level++) {
                int outputLength = currentLength / 2;

                // Allocate output arrays
                approxCoeffs[level] = new double[outputLength];
                detailCoeffs[level] = new double[outputLength];

                // Perform blocked transform
                forwardTransformBlocked(currentSignal, approxCoeffs[level], detailCoeffs[level],
                        lowPass, highPass, currentLength, filterLength);

                // Prepare for next level
                if (level < levels - 1) {
                    // Copy approximation to temp buffer for next iteration
                    if (useTemp1) {
                        System.arraycopy(approxCoeffs[level], 0, temp1, 0, outputLength);
                        currentSignal = temp1;
                    } else {
                        System.arraycopy(approxCoeffs[level], 0, temp2, 0, outputLength);
                        currentSignal = temp2;
                    }
                    useTemp1 = !useTemp1;
                }

                currentLength = outputLength;

                // Stop if signal becomes too small
                if (currentLength < filterLength * 2) {
                    break;
                }
            }
        }
    }

    /**
     * Cache-aware batch transform for multiple signals.
     * Processes signals in groups that fit in cache together.
     */
    public static void batchTransformCacheAware(double[][] signals, double[][] approxResults,
                                                double[][] detailResults, double[] lowPass,
                                                double[] highPass, int filterLength) {
        int numSignals = signals.length;
        int signalLength = signals[0].length;
        int outputLength = signalLength / 2;

        // Calculate how many signals fit in L2 cache
        int signalsPerBatch = L2_CACHE_SIZE / (signalLength * 8) / 2; // Divide by 2 for safety
        signalsPerBatch = Math.max(1, Math.min(signalsPerBatch, 8)); // Between 1 and 8

        // Process signals in cache-friendly batches
        for (int batchStart = 0; batchStart < numSignals; batchStart += signalsPerBatch) {
            int batchEnd = Math.min(batchStart + signalsPerBatch, numSignals);

            // Process this batch with blocking
            for (int s = batchStart; s < batchEnd; s++) {
                forwardTransformBlocked(signals[s], approxResults[s], detailResults[s],
                        lowPass, highPass, signalLength, filterLength);
            }
        }
    }

    /**
     * Process a forward transform block.
     */
    private static void processForwardBlock(double[] signal, double[] approx, double[] detail,
                                            double[] lowPass, double[] highPass,
                                            int blockStart, int blockEnd,
                                            int signalLength, int filterLength) {
        // Use pooled memory for temporary storage
        int blockSize = blockEnd - blockStart;

        try (PooledArray pooledTemp = AlignedMemoryPool.allocate(VECTOR_LENGTH)) {
            double[] temp = pooledTemp.array();
            int tempOffset = pooledTemp.offset();

            // Process block with vectorization
            for (int i = blockStart; i < blockEnd; i += VECTOR_LENGTH) {
                int remaining = Math.min(VECTOR_LENGTH, blockEnd - i);

                // Prefetch next iteration's data
                if (i + PREFETCH_DISTANCE < blockEnd) {
                    prefetchData(signal, 2 * (i + PREFETCH_DISTANCE), filterLength * 2);
                }

                // Process VECTOR_LENGTH outputs at once
                for (int v = 0; v < remaining; v++) {
                    double sumLow = 0.0;
                    double sumHigh = 0.0;

                    // Convolution
                    for (int k = 0; k < filterLength; k++) {
                        int idx = (2 * (i + v) + k) % signalLength;
                        double val = signal[idx];
                        sumLow += lowPass[k] * val;
                        sumHigh += highPass[k] * val;
                    }

                    approx[i + v] = sumLow;
                    detail[i + v] = sumHigh;
                }
            }
        }
    }

    /**
     * Process an inverse transform block.
     */
    private static void processInverseBlock(double[] approx, double[] detail, double[] output,
                                            double[] lowPass, double[] highPass,
                                            int blockStart, int blockEnd,
                                            int inputLength, int filterLength) {
        // Process block
        for (int i = blockStart; i < blockEnd; i++) {
            // Calculate which input coefficients affect this output sample
            for (int k = 0; k < filterLength; k++) {
                int inputIdx = (i - k) / 2;

                if (inputIdx >= 0 && inputIdx < inputLength && (i - k) % 2 == 0) {
                    // Even index - use low pass
                    output[i] += approx[inputIdx] * lowPass[k];
                    output[i] += detail[inputIdx] * highPass[k];
                }
            }
        }
    }

    /**
     * Choose optimal block size based on signal length.
     */
    private static int chooseBlockSize(int signalLength) {
        if (signalLength <= L1_BLOCK_SIZE * 2) {
            return L1_BLOCK_SIZE;
        } else if (signalLength <= L2_BLOCK_SIZE * 2) {
            return L2_BLOCK_SIZE;
        } else {
            return L3_BLOCK_SIZE;
        }
    }

    /**
     * Prefetch data into cache.
     * This is a hint to the processor - actual behavior is platform-dependent.
     */
    private static void prefetchData(double[] data, int offset, int length) {
        // Touch memory to trigger hardware prefetcher
        int end = Math.min(offset + length, data.length);
        for (int i = offset; i < end; i += 8) { // Every cache line
            @SuppressWarnings("unused")
            double dummy = data[i];
        }
    }

    /**
     * Tiled matrix-style convolution for 2D signals.
     * Useful for image processing applications.
     */
    public static void convolve2DTiled(double[][] signal, double[][] kernel,
                                       double[][] output, int tileSize) {
        int rows = signal.length;
        int cols = signal[0].length;
        int kernelSize = kernel.length;

        // Process in tiles
        for (int tileRow = 0; tileRow < rows; tileRow += tileSize) {
            for (int tileCol = 0; tileCol < cols; tileCol += tileSize) {
                // Process this tile
                int tileEndRow = Math.min(tileRow + tileSize, rows);
                int tileEndCol = Math.min(tileCol + tileSize, cols);

                processTile2D(signal, kernel, output,
                        tileRow, tileEndRow, tileCol, tileEndCol, kernelSize);
            }
        }
    }

    private static void processTile2D(double[][] signal, double[][] kernel, double[][] output,
                                      int startRow, int endRow, int startCol, int endCol,
                                      int kernelSize) {
        int halfKernel = kernelSize / 2;

        for (int r = startRow; r < endRow; r++) {
            for (int c = startCol; c < endCol; c++) {
                double sum = 0.0;

                // Apply kernel
                for (int kr = 0; kr < kernelSize; kr++) {
                    for (int kc = 0; kc < kernelSize; kc++) {
                        int sr = r + kr - halfKernel;
                        int sc = c + kc - halfKernel;

                        // Handle boundaries (zero padding)
                        if (sr >= 0 && sr < signal.length && sc >= 0 && sc < signal[0].length) {
                            sum += signal[sr][sc] * kernel[kr][kc];
                        }
                    }
                }

                output[r][c] = sum;
            }
        }
    }

    /**
     * Get cache blocking information.
     */
    public static String getCacheInfo() {
        return String.format(
                "Cache Blocking Configuration:%n" +
                        "L1 Block Size: %d doubles (%d KB)%n" +
                        "L2 Block Size: %d doubles (%d KB)%n" +
                        "L3 Block Size: %d doubles (%d KB)%n" +
                        "Vector Length: %d doubles%n" +
                        "Prefetch Distance: %d cache lines",
                L1_BLOCK_SIZE, L1_BLOCK_SIZE * 8 / 1024,
                L2_BLOCK_SIZE, L2_BLOCK_SIZE * 8 / 1024,
                L3_BLOCK_SIZE, L3_BLOCK_SIZE * 8 / 1024,
                VECTOR_LENGTH,
                PREFETCH_DISTANCE
        );
    }
}
