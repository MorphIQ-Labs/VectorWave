package com.morphiqlabs.wavelet.extensions.modwt;

import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.util.ThreadLocalManager;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;
import jdk.incubator.vector.VectorMask;

/**
 * True SIMD batch MODWT implementation using Structure-of-Arrays (SoA) layout.
 * 
 * <p>This implementation processes multiple signals in parallel using SIMD instructions,
 * specifically optimized for MODWT (no downsampling). Key features:</p>
 * <ul>
 *   <li>Structure-of-Arrays layout for optimal SIMD access patterns</li>
 *   <li>Processes VECTOR_LENGTH signals simultaneously</li>
 *   <li>Specialized kernels for common wavelets (Haar, DB4)</li>
 *   <li>Adaptive algorithm selection based on batch size</li>
 * </ul>
 * 
 * <p><b>Memory Layout:</b></p>
 * <pre>
 * Traditional AoS:  [sig1[0], sig1[1], ..., sig1[N-1], sig2[0], sig2[1], ...]
 * SoA Layout:       [sig1[0], sig2[0], ..., sigM[0], sig1[1], sig2[1], ..., sigM[1], ...]
 * </pre>
 * 
 * <p>This layout allows loading all signals' values at position t into a single vector.</p>
 */
public final class BatchSIMDMODWT {
    
    private BatchSIMDMODWT() { throw new AssertionError("No instances"); }
    
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();
    
    // MODWT scaling factor
    private static final double MODWT_SCALE = 1.0 / Math.sqrt(2.0);
    
    // Thread-local storage for temporary arrays - now managed by ThreadLocalManager
    private static final ThreadLocalManager.ManagedThreadLocal<MODWTWorkArrays> WORK_ARRAYS = 
        ThreadLocalManager.withInitial(() -> new MODWTWorkArrays(VECTOR_LENGTH));
    
    private static class MODWTWorkArrays {
        final double[] tempSum;
        final double[] samples;
        
        MODWTWorkArrays(int vectorLength) {
            this.tempSum = new double[vectorLength];
            this.samples = new double[vectorLength];
        }
    }
    
    /**
     * Performs batch MODWT transform using Structure-of-Arrays layout.
     * 
     * @param soaSignals Input signals in SoA layout [batch_size × signal_length]
     * @param soaApprox Output approximation coefficients in SoA layout
     * @param soaDetail Output detail coefficients in SoA layout
     * @param wavelet The wavelet to use
     * @param batchSize Number of signals
     * @param signalLength Length of each signal
     */
    public static void batchMODWTSoA(double[] soaSignals, double[] soaApprox, 
                                    double[] soaDetail, DiscreteWavelet wavelet,
                                    int batchSize, int signalLength) {
        
        if (wavelet instanceof Haar) {
            haarBatchMODWTSoA(soaSignals, soaApprox, soaDetail, batchSize, signalLength);
        } else if (wavelet.lowPassDecomposition().length == 4) {
            db4BatchMODWTSoA(soaSignals, soaApprox, soaDetail, 
                           wavelet.lowPassDecomposition(), 
                           wavelet.highPassDecomposition(),
                           batchSize, signalLength);
        } else {
            generalBatchMODWTSoA(soaSignals, soaApprox, soaDetail,
                               wavelet.lowPassDecomposition(),
                               wavelet.highPassDecomposition(),
                               batchSize, signalLength);
        }
    }
    
    /**
     * Specialized Haar MODWT for batch processing.
     */
    private static void haarBatchMODWTSoA(double[] soaSignals, double[] soaApprox,
                                         double[] soaDetail, int batchSize, int signalLength) {
        
        // Haar coefficients are already scaled by 1/sqrt(2), so apply MODWT scaling
        final double h0 = 0.5;  // (1/sqrt(2)) * (1/sqrt(2)) = 1/2
        final double h1 = 0.5;
        final double g0 = 0.5;
        final double g1 = -0.5;
        
        // Process signals in groups of VECTOR_LENGTH
        int vectorBatches = batchSize / VECTOR_LENGTH;
        int remainder = batchSize % VECTOR_LENGTH;
        
        // Process each time point using MODWT (t - l) indexing
        for (int t = 0; t < signalLength; t++) {
            // For Haar: h[0] at t, h[1] at (t-1)
            int tMinus1 = (t - 1 + signalLength) % signalLength;
            
            // Process full vector batches
            for (int b = 0; b < vectorBatches; b++) {
                int baseIdx = b * VECTOR_LENGTH;
                
                // Load samples at t and (t-1) for all signals in batch
                DoubleVector s0 = DoubleVector.fromArray(SPECIES, soaSignals, 
                    t * batchSize + baseIdx);
                DoubleVector s1 = DoubleVector.fromArray(SPECIES, soaSignals, 
                    tMinus1 * batchSize + baseIdx);
                
                // Compute MODWT coefficients using (t - l) indexing
                DoubleVector approx = s0.mul(h0).add(s1.mul(h1));
                DoubleVector detail = s0.mul(g0).add(s1.mul(g1));
                
                // Store results
                approx.intoArray(soaApprox, t * batchSize + baseIdx);
                detail.intoArray(soaDetail, t * batchSize + baseIdx);
            }
            
            // Handle remainder with masked operations
            if (remainder > 0) {
                int baseIdx = vectorBatches * VECTOR_LENGTH;
                VectorMask<Double> mask = VectorMask.fromLong(SPECIES, (1L << remainder) - 1);
                
                DoubleVector s0 = DoubleVector.fromArray(SPECIES, soaSignals, 
                    t * batchSize + baseIdx, mask);
                DoubleVector s1 = DoubleVector.fromArray(SPECIES, soaSignals, 
                    tMinus1 * batchSize + baseIdx, mask);
                
                DoubleVector approx = s0.mul(h0).add(s1.mul(h1));
                DoubleVector detail = s0.mul(g0).add(s1.mul(g1));
                
                approx.intoArray(soaApprox, t * batchSize + baseIdx, mask);
                detail.intoArray(soaDetail, t * batchSize + baseIdx, mask);
            }
        }
    }
    
    /**
     * Specialized DB4 MODWT for batch processing.
     */
    private static void db4BatchMODWTSoA(double[] soaSignals, double[] soaApprox,
                                        double[] soaDetail, double[] lowPass, 
                                        double[] highPass, int batchSize, int signalLength) {
        
        // Scale filters for MODWT
        double h0 = lowPass[0] * MODWT_SCALE;
        double h1 = lowPass[1] * MODWT_SCALE;
        double h2 = lowPass[2] * MODWT_SCALE;
        double h3 = lowPass[3] * MODWT_SCALE;
        
        double g0 = highPass[0] * MODWT_SCALE;
        double g1 = highPass[1] * MODWT_SCALE;
        double g2 = highPass[2] * MODWT_SCALE;
        double g3 = highPass[3] * MODWT_SCALE;
        
        int vectorBatches = batchSize / VECTOR_LENGTH;
        int remainder = batchSize % VECTOR_LENGTH;
        
        // Process each time point using MODWT (t - l) indexing
        for (int t = 0; t < signalLength; t++) {
            // Compute circular indices for (t - l) where l = 0, 1, 2, 3
            int tMinus1 = (t - 1 + signalLength) % signalLength;
            int tMinus2 = (t - 2 + signalLength) % signalLength;
            int tMinus3 = (t - 3 + signalLength) % signalLength;
            
            // Process full vector batches
            for (int b = 0; b < vectorBatches; b++) {
                int baseIdx = b * VECTOR_LENGTH;
                
                // Load samples at different time offsets using (t - l) indexing
                DoubleVector s0 = DoubleVector.fromArray(SPECIES, soaSignals, t * batchSize + baseIdx);
                DoubleVector s1 = DoubleVector.fromArray(SPECIES, soaSignals, tMinus1 * batchSize + baseIdx);
                DoubleVector s2 = DoubleVector.fromArray(SPECIES, soaSignals, tMinus2 * batchSize + baseIdx);
                DoubleVector s3 = DoubleVector.fromArray(SPECIES, soaSignals, tMinus3 * batchSize + baseIdx);
                
                // Compute convolutions
                DoubleVector approx = s0.mul(h0).add(s1.mul(h1)).add(s2.mul(h2)).add(s3.mul(h3));
                DoubleVector detail = s0.mul(g0).add(s1.mul(g1)).add(s2.mul(g2)).add(s3.mul(g3));
                
                // Store results
                approx.intoArray(soaApprox, t * batchSize + baseIdx);
                detail.intoArray(soaDetail, t * batchSize + baseIdx);
            }
            
            // Handle remainder
            if (remainder > 0) {
                int baseIdx = vectorBatches * VECTOR_LENGTH;
                VectorMask<Double> mask = VectorMask.fromLong(SPECIES, (1L << remainder) - 1);
                
                DoubleVector s0 = DoubleVector.fromArray(SPECIES, soaSignals, t * batchSize + baseIdx, mask);
                DoubleVector s1 = DoubleVector.fromArray(SPECIES, soaSignals, tMinus1 * batchSize + baseIdx, mask);
                DoubleVector s2 = DoubleVector.fromArray(SPECIES, soaSignals, tMinus2 * batchSize + baseIdx, mask);
                DoubleVector s3 = DoubleVector.fromArray(SPECIES, soaSignals, tMinus3 * batchSize + baseIdx, mask);
                
                DoubleVector approx = s0.mul(h0).add(s1.mul(h1)).add(s2.mul(h2)).add(s3.mul(h3));
                DoubleVector detail = s0.mul(g0).add(s1.mul(g1)).add(s2.mul(g2)).add(s3.mul(g3));
                
                approx.intoArray(soaApprox, t * batchSize + baseIdx, mask);
                detail.intoArray(soaDetail, t * batchSize + baseIdx, mask);
            }
        }
    }
    
    /**
     * General MODWT for arbitrary filter lengths.
     */
    private static void generalBatchMODWTSoA(double[] soaSignals, double[] soaApprox,
                                           double[] soaDetail, double[] lowPass,
                                           double[] highPass, int batchSize, int signalLength) {
        
        int filterLength = lowPass.length;
        MODWTWorkArrays work = WORK_ARRAYS.get();
        
        // Scale filters
        double[] scaledLow = new double[filterLength];
        double[] scaledHigh = new double[filterLength];
        for (int i = 0; i < filterLength; i++) {
            scaledLow[i] = lowPass[i] * MODWT_SCALE;
            scaledHigh[i] = highPass[i] * MODWT_SCALE;
        }
        
        int vectorBatches = batchSize / VECTOR_LENGTH;
        int remainder = batchSize % VECTOR_LENGTH;
        
        // Process each output time point
        for (int t = 0; t < signalLength; t++) {
            
            // Process full vector batches
            for (int b = 0; b < vectorBatches; b++) {
                int baseIdx = b * VECTOR_LENGTH;
                
                DoubleVector approxSum = DoubleVector.zero(SPECIES);
                DoubleVector detailSum = DoubleVector.zero(SPECIES);
                
                // Convolve with filter using MODWT (t - l) indexing
                for (int l = 0; l < filterLength; l++) {
                    int srcT = (t - l + signalLength) % signalLength;
                    DoubleVector samples = DoubleVector.fromArray(SPECIES, soaSignals, 
                        srcT * batchSize + baseIdx);
                    
                    approxSum = approxSum.add(samples.mul(scaledLow[l]));
                    detailSum = detailSum.add(samples.mul(scaledHigh[l]));
                }
                
                approxSum.intoArray(soaApprox, t * batchSize + baseIdx);
                detailSum.intoArray(soaDetail, t * batchSize + baseIdx);
            }
            
            // Handle remainder
            if (remainder > 0) {
                int baseIdx = vectorBatches * VECTOR_LENGTH;
                VectorMask<Double> mask = VectorMask.fromLong(SPECIES, (1L << remainder) - 1);
                
                DoubleVector approxSum = DoubleVector.zero(SPECIES);
                DoubleVector detailSum = DoubleVector.zero(SPECIES);
                
                for (int l = 0; l < filterLength; l++) {
                    int srcT = (t - l + signalLength) % signalLength;
                    DoubleVector samples = DoubleVector.fromArray(SPECIES, soaSignals, 
                        srcT * batchSize + baseIdx, mask);
                    
                    approxSum = approxSum.add(samples.mul(scaledLow[l]));
                    detailSum = detailSum.add(samples.mul(scaledHigh[l]));
                }
                
                approxSum.intoArray(soaApprox, t * batchSize + baseIdx, mask);
                detailSum.intoArray(soaDetail, t * batchSize + baseIdx, mask);
            }
        }
    }
    
    /**
     * Converts Array-of-Structures to Structure-of-Arrays layout.
     * 
     * @param signals Input signals in AoS layout [batch_size][signal_length]
     * @param soaOutput Output buffer in SoA layout
     */
    public static void convertToSoA(double[][] signals, double[] soaOutput) {
        int batchSize = signals.length;
        int signalLength = signals[0].length;
        
        for (int t = 0; t < signalLength; t++) {
            for (int b = 0; b < batchSize; b++) {
                soaOutput[t * batchSize + b] = signals[b][t];
            }
        }
    }
    
    /**
     * Converts Structure-of-Arrays back to Array-of-Structures layout.
     * 
     * @param soaData Input data in SoA layout
     * @param output Output signals in AoS layout [batch_size][signal_length]
     */
    public static void convertFromSoA(double[] soaData, double[][] output) {
        int batchSize = output.length;
        int signalLength = output[0].length;
        
        for (int t = 0; t < signalLength; t++) {
            for (int b = 0; b < batchSize; b++) {
                output[b][t] = soaData[t * batchSize + b];
            }
        }
    }
    
    /**
     * Performs batch MODWT with automatic ThreadLocal cleanup.
     * Recommended for thread pool environments.
     * 
     * @param soaSignals Input signals in SoA layout
     * @param soaApprox Output approximation coefficients in SoA layout
     * @param soaDetail Output detail coefficients in SoA layout
     * @param wavelet The wavelet to use
     * @param batchSize Number of signals
     * @param signalLength Length of each signal
     */
    @SuppressWarnings("try")
    public static void batchMODWTWithCleanup(double[] soaSignals, double[] soaApprox, 
                                           double[] soaDetail, DiscreteWavelet wavelet,
                                           int batchSize, int signalLength) {
        try (ThreadLocalManager.CleanupScope scope = ThreadLocalManager.createScope()) {
            batchMODWTSoA(soaSignals, soaApprox, soaDetail, wavelet, batchSize, signalLength);
        }
    }

    /**
     * Performs batch multi-level MODWT using SoA layout. Detail outputs are provided per-level.
     * Approximation is cascaded across levels (à trous) and returned in soaApproxOut.
     * Analysis filters are upsampled and scaled by 2^(-j/2) for level j.
     *
     * @param soaSignals Input signals in SoA layout [batch*length]
     * @param soaDetailPerLevel Output details per level, each in SoA layout [levels][batch*length]
     * @param soaApproxOut Output final approximation in SoA layout [batch*length]
     * @param wavelet Discrete wavelet
     * @param batchSize Number of signals
     * @param signalLength Length of each signal
     * @param levels Number of decomposition levels
     */
    public static void batchMultiLevelMODWTSoA(double[] soaSignals,
                                               double[][] soaDetailPerLevel,
                                               double[] soaApproxOut,
                                               DiscreteWavelet wavelet,
                                               int batchSize,
                                               int signalLength,
                                               int levels) {
        if (soaDetailPerLevel.length != levels) {
            throw new IllegalArgumentException("soaDetailPerLevel length must equal levels");
        }
        // Working SoA buffers
        double[] current = new double[batchSize * signalLength];
        double[] next = new double[batchSize * signalLength];
        System.arraycopy(soaSignals, 0, current, 0, current.length);

        // Precompute base filters
        double[] baseLow = wavelet.lowPassDecomposition();
        double[] baseHigh = wavelet.highPassDecomposition();

        for (int level = 1; level <= levels; level++) {
            // Upsample and scale per core semantics for analysis at this level
            // Use the same helper as core MultiLevelMODWTTransform (ensures strict parity)
            double[] scaledLow = com.morphiqlabs.wavelet.internal.ScalarOps
                .upsampleAndScaleForIMODWTSynthesis(baseLow, level);
            double[] scaledHigh = com.morphiqlabs.wavelet.internal.ScalarOps
                .upsampleAndScaleForIMODWTSynthesis(baseHigh, level);

            // Compute approx (to next) and detail (to per-level target)
            generalBatchMODWTSoAWithScaledFilters(current, next, soaDetailPerLevel[level - 1],
                                                  scaledLow, scaledHigh, batchSize, signalLength);
            // Swap buffers (next becomes current)
            double[] tmp = current; current = next; next = tmp;
            // Clear next for reuse
            java.util.Arrays.fill(next, 0.0);
        }

        // Copy final approximation to output
        System.arraycopy(current, 0, soaApproxOut, 0, current.length);
    }

    // Vectorized generic convolution using pre-scaled filters (periodic, SoA layout)
    private static void generalBatchMODWTSoAWithScaledFilters(double[] soaSignals,
                                                              double[] soaApprox,
                                                              double[] soaDetail,
                                                              double[] scaledLow,
                                                              double[] scaledHigh,
                                                              int batchSize,
                                                              int signalLength) {
        int filterLength = scaledLow.length;
        int vectorBatches = batchSize / VECTOR_LENGTH;
        int remainder = batchSize % VECTOR_LENGTH;

        for (int t = 0; t < signalLength; t++) {
            for (int b = 0; b < vectorBatches; b++) {
                int baseIdx = b * VECTOR_LENGTH;
                DoubleVector approxSum = DoubleVector.zero(SPECIES);
                DoubleVector detailSum = DoubleVector.zero(SPECIES);
                for (int l = 0; l < filterLength; l++) {
                    int srcT = (t - l + signalLength) % signalLength;
                    DoubleVector samples = DoubleVector.fromArray(SPECIES, soaSignals, srcT * batchSize + baseIdx);
                    approxSum = approxSum.add(samples.mul(scaledLow[l]));
                    detailSum = detailSum.add(samples.mul(scaledHigh[l]));
                }
                approxSum.intoArray(soaApprox, t * batchSize + baseIdx);
                detailSum.intoArray(soaDetail, t * batchSize + baseIdx);
            }
            if (remainder > 0) {
                int baseIdx = vectorBatches * VECTOR_LENGTH;
                VectorMask<Double> mask = VectorMask.fromLong(SPECIES, (1L << remainder) - 1);
                DoubleVector approxSum = DoubleVector.zero(SPECIES);
                DoubleVector detailSum = DoubleVector.zero(SPECIES);
                for (int l = 0; l < filterLength; l++) {
                    int srcT = (t - l + signalLength) % signalLength;
                    DoubleVector samples = DoubleVector.fromArray(SPECIES, soaSignals, srcT * batchSize + baseIdx, mask);
                    approxSum = approxSum.add(samples.mul(scaledLow[l]));
                    detailSum = detailSum.add(samples.mul(scaledHigh[l]));
                }
                approxSum.intoArray(soaApprox, t * batchSize + baseIdx, mask);
                detailSum.intoArray(soaDetail, t * batchSize + baseIdx, mask);
            }
        }
    }

    /**
     * General MODWT for arbitrary (pre-scaled) filters with left-history support (streaming).
     * Uses Structure-of-Arrays layout for inputs and outputs. History is provided as the
     * last {@code historyLength} samples preceding the current block, ordered from oldest
     * to newest (i.e., history[t] corresponds to time index t-historyLength where t ∈ [0,historyLength)).
     *
     * <p>This variant enables SIMD streaming for non-periodic boundaries (ZERO_PADDING, SYMMETRIC)
     * by pre-filling the {@code soaHistory} buffer according to the desired boundary semantics
     * for the first block, and then updating it with the last {@code historyLength} samples after
     * each call.</p>
     *
     * @param soaHistory  Left history in SoA layout [historyLength*batch]
     * @param historyLength Number of left-context samples
     * @param soaSignals  Current block in SoA layout [batch*signalLength]
     * @param soaApprox   Output approximation in SoA layout [batch*signalLength]
     * @param soaDetail   Output detail in SoA layout [batch*signalLength]
     * @param scaledLow   Pre-scaled analysis low-pass filter (length L)
     * @param scaledHigh  Pre-scaled analysis high-pass filter (length L)
     * @param batchSize   Number of signals in batch
     * @param signalLength Length of each signal in the current block
     */
    public static void generalBatchMODWTSoAWithScaledFiltersAndHistory(double[] soaHistory,
                                                                       int historyLength,
                                                                       double[] soaSignals,
                                                                       double[] soaApprox,
                                                                       double[] soaDetail,
                                                                       double[] scaledLow,
                                                                       double[] scaledHigh,
                                                                       int batchSize,
                                                                       int signalLength) {
        int filterLength = scaledLow.length;
        int vectorBatches = batchSize / VECTOR_LENGTH;
        int remainder = batchSize % VECTOR_LENGTH;

        // historyLength is expected to be filterLength - 1 for level-1, and
        // (L0-1)*2^(j-1) for level j after upsampling (i.e., scaledLow.length-1)

        for (int t = 0; t < signalLength; t++) {
            for (int b = 0; b < vectorBatches; b++) {
                int baseIdx = b * VECTOR_LENGTH;
                DoubleVector approxSum = DoubleVector.zero(SPECIES);
                DoubleVector detailSum = DoubleVector.zero(SPECIES);
                for (int l = 0; l < filterLength; l++) {
                    int idx = t - l;
                    if (idx >= 0) {
                        DoubleVector samples = DoubleVector.fromArray(SPECIES, soaSignals, idx * batchSize + baseIdx);
                        approxSum = approxSum.add(samples.mul(scaledLow[l]));
                        detailSum = detailSum.add(samples.mul(scaledHigh[l]));
                    } else {
                        int histPos = historyLength + idx; // idx is negative
                        DoubleVector samples = DoubleVector.fromArray(SPECIES, soaHistory, histPos * batchSize + baseIdx);
                        approxSum = approxSum.add(samples.mul(scaledLow[l]));
                        detailSum = detailSum.add(samples.mul(scaledHigh[l]));
                    }
                }
                approxSum.intoArray(soaApprox, t * batchSize + baseIdx);
                detailSum.intoArray(soaDetail, t * batchSize + baseIdx);
            }

            if (remainder > 0) {
                int baseIdx = vectorBatches * VECTOR_LENGTH;
                VectorMask<Double> mask = VectorMask.fromLong(SPECIES, (1L << remainder) - 1);
                DoubleVector approxSum = DoubleVector.zero(SPECIES);
                DoubleVector detailSum = DoubleVector.zero(SPECIES);
                for (int l = 0; l < filterLength; l++) {
                    int idx = t - l;
                    if (idx >= 0) {
                        DoubleVector samples = DoubleVector.fromArray(SPECIES, soaSignals, idx * batchSize + baseIdx, mask);
                        approxSum = approxSum.add(samples.mul(scaledLow[l]));
                        detailSum = detailSum.add(samples.mul(scaledHigh[l]));
                    } else {
                        int histPos = historyLength + idx; // negative idx
                        DoubleVector samples = DoubleVector.fromArray(SPECIES, soaHistory, histPos * batchSize + baseIdx, mask);
                        approxSum = approxSum.add(samples.mul(scaledLow[l]));
                        detailSum = detailSum.add(samples.mul(scaledHigh[l]));
                    }
                }
                approxSum.intoArray(soaApprox, t * batchSize + baseIdx, mask);
                detailSum.intoArray(soaDetail, t * batchSize + baseIdx, mask);
            }
        }
    }
}
