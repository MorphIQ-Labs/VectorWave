package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidSignalException;
import com.morphiqlabs.wavelet.WaveletOperations;
import com.morphiqlabs.wavelet.util.ValidationUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * Parallel implementation of multi-level MODWT with CompletableFuture chains.
 * 
 * <p>This class provides an optimized parallel version of multi-level MODWT that:</p>
 * <ul>
 *   <li>Uses CompletableFuture chains to handle level dependencies</li>
 *   <li>Parallelizes low-pass and high-pass filtering at each level</li>
 *   <li>Pre-allocates memory to avoid contention</li>
 *   <li>Properly handles filter upsampling at each level</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public class ParallelMultiLevelMODWT implements AutoCloseable {
    
    private static final int MAX_DECOMPOSITION_LEVELS = 10;
    private static final int MAX_SAFE_SHIFT_BITS = 31;
    
    private final Executor executor;
    private final boolean ownsExecutor;
    private final int minParallelSignalLength;
    // Per-instance cache of synthesis filters by (wavelet instance, level)
    private final java.util.concurrent.ConcurrentHashMap<Wavelet, java.util.concurrent.ConcurrentHashMap<Integer, FilterSet>> synthesisCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * Creates a parallel multi-level MODWT using the common ForkJoinPool.
     */
    public ParallelMultiLevelMODWT() {
        this(ForkJoinPool.commonPool(), false, 0);
    }
    
    /**
     * Creates a parallel multi-level MODWT with a custom executor.
     *
     * @param executor The executor to use for parallel tasks
     * @throws NullPointerException if {@code executor} is null
     */
    public ParallelMultiLevelMODWT(Executor executor) {
        this(executor, false, 0);
    }

    /**
     * Creates a parallel multi-level MODWT with a new ForkJoinPool of given parallelism.
     * If {@code parallelism <= 0}, falls back to the common pool.
     *
     * @param parallelism desired parallelism level
     */
    public ParallelMultiLevelMODWT(int parallelism) {
        this(parallelism > 0 ? new ForkJoinPool(parallelism) : ForkJoinPool.commonPool(),
            parallelism > 0, 0);
    }

    private ParallelMultiLevelMODWT(Executor executor, boolean ownsExecutor, int minParallelSignalLength) {
        this.executor = executor;
        this.ownsExecutor = ownsExecutor;
        this.minParallelSignalLength = Math.max(0, minParallelSignalLength);
    }
    
    /**
     * Performs parallel multi-level MODWT decomposition.
     *
     * @param signal Input signal
     * @param wavelet Wavelet to use
     * @param mode Boundary mode
     * @param levels Number of decomposition levels
     * @return Multi-level MODWT result
     * @throws com.morphiqlabs.wavelet.exception.InvalidSignalException if {@code signal} is invalid
     * @throws com.morphiqlabs.wavelet.exception.InvalidArgumentException if {@code wavelet} is not discrete or {@code levels} is invalid
     */
    public MultiLevelMODWTResult decompose(double[] signal, Wavelet wavelet,
                                           BoundaryMode mode, int levels) {
        // Validate inputs
        ValidationUtils.validateFiniteValues(signal, "signal");
        if (signal.length == 0) {
            throw new InvalidSignalException("Signal cannot be empty");
        }
        
        if (!(wavelet instanceof DiscreteWavelet dw)) {
            throw new InvalidArgumentException("Multi-level MODWT requires a discrete wavelet");
        }
        
        int maxLevels = calculateMaxLevels(signal.length, dw);
        if (levels < 1 || levels > maxLevels) {
            throw new InvalidArgumentException(
                "Invalid number of levels: " + levels + 
                ". Must be between 1 and " + maxLevels);
        }
        
        // Initialize result structure
        MultiLevelMODWTResultImpl result = new MultiLevelMODWTResultImpl(signal.length, levels);

        // Pre-allocate arrays and filters
        double[][] detailArrays = new double[levels][signal.length];
        double[] currentApprox = signal.clone();
        double[] nextApprox = new double[signal.length];
        FilterSet[] filterSets = precomputeFilterSets(dw, levels);

        boolean useParallel = signal.length >= minParallelSignalLength;

        if (useParallel) {
            // Build dependency chain across levels (cascade)
            CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
            for (int level = 1; level <= levels; level++) {
                final int li = level - 1;
                final FilterSet filters = filterSets[li];
                final double[] inputAtLevel = currentApprox;
                final double[] outputApprox = nextApprox;

                chain = chain.thenCompose(v -> {
                    CompletableFuture<Void> low = CompletableFuture.runAsync(() -> {
                        if (mode == BoundaryMode.PERIODIC) {
                            WaveletOperations.circularConvolveMODWT(inputAtLevel, filters.scaledLowPass, outputApprox);
                        } else {
                            applyZeroPaddingMODWT(inputAtLevel, filters.scaledLowPass, outputApprox);
                        }
                    }, executor);

                    CompletableFuture<Void> high = CompletableFuture.runAsync(() -> {
                        if (mode == BoundaryMode.PERIODIC) {
                            WaveletOperations.circularConvolveMODWT(inputAtLevel, filters.scaledHighPass, detailArrays[li]);
                        } else {
                            applyZeroPaddingMODWT(inputAtLevel, filters.scaledHighPass, detailArrays[li]);
                        }
                    }, executor);

                    return CompletableFuture.allOf(low, high).thenRun(() -> {
                        // Move to next level: copy nextApprox into currentApprox and clear nextApprox
                        System.arraycopy(outputApprox, 0, inputAtLevel, 0, inputAtLevel.length);
                        java.util.Arrays.fill(outputApprox, 0.0);
                    });
                });
            }
            chain.join();
        } else {
            // Sequential path for small signals to avoid async overhead
            for (int level = 1; level <= levels; level++) {
                final int li = level - 1;
                final FilterSet filters = filterSets[li];
                final double[] inputAtLevel = currentApprox;
                final double[] outputApprox = nextApprox;

                if (mode == BoundaryMode.PERIODIC) {
                    WaveletOperations.circularConvolveMODWT(inputAtLevel, filters.scaledLowPass, outputApprox);
                    WaveletOperations.circularConvolveMODWT(inputAtLevel, filters.scaledHighPass, detailArrays[li]);
                } else {
                    applyZeroPaddingMODWT(inputAtLevel, filters.scaledLowPass, outputApprox);
                    applyZeroPaddingMODWT(inputAtLevel, filters.scaledHighPass, detailArrays[li]);
                }

                System.arraycopy(outputApprox, 0, inputAtLevel, 0, inputAtLevel.length);
                java.util.Arrays.fill(outputApprox, 0.0);
            }
        }

        // Collect results
        for (int level = 1; level <= levels; level++) {
            result.setDetailCoeffsAtLevel(level, detailArrays[level - 1]);
        }
        result.setApproximationCoeffs(currentApprox);

        return result;
    }
    
    /**
     * Pre-computes all filter sets for all levels.
     */
    private FilterSet[] precomputeFilterSets(DiscreteWavelet wavelet, int levels) {
        FilterSet[] filterSets = new FilterSet[levels];
        
        double[] lowPass = wavelet.lowPassDecomposition();
        double[] highPass = wavelet.highPassDecomposition();
        
        for (int level = 1; level <= levels; level++) {
            // Use cache per wavelet instance and level
            var perWavelet = synthesisCache.computeIfAbsent(wavelet, w -> new java.util.concurrent.ConcurrentHashMap<>());
            FilterSet fs = perWavelet.computeIfAbsent(level, l -> upsampleFiltersForLevel(lowPass, highPass, l));
            filterSets[level - 1] = fs;
        }
        
        return filterSets;
    }
    
    
    /**
     * Upsample and scale filters per analysis stage (1/√2 per level) for parallel path.
     */
    private FilterSet upsampleFiltersForLevel(double[] lowFilter, double[] highFilter, int level) {
        double[] scaledLow = com.morphiqlabs.wavelet.internal.ScalarOps
            .upsampleAndScaleForIMODWTSynthesis(lowFilter, level);
        double[] scaledHigh = com.morphiqlabs.wavelet.internal.ScalarOps
            .upsampleAndScaleForIMODWTSynthesis(highFilter, level);
        return new FilterSet(scaledLow, scaledHigh);
    }
    
    /**
     * Applies MODWT with zero-padding boundary handling.
     *
     * <p>This uses the dedicated zero-padding convolution routine which mirrors
     * the behavior of the sequential {@code MultiLevelMODWTTransform}.</p>
     */
    private void applyZeroPaddingMODWT(double[] input, double[] filter, double[] output) {
        WaveletOperations.zeroPaddingConvolveMODWT(input, filter, output);
    }
    
    /**
     * Calculates maximum decomposition levels for the signal.
     */
    private int calculateMaxLevels(int signalLength, DiscreteWavelet wavelet) {
        int filterLength = wavelet.lowPassDecomposition().length;
        
        if (signalLength < filterLength) {
            return 0;
        }
        
        int maxLevel = 1;
        int filterLengthMinus1 = filterLength - 1;
        
        while (maxLevel < MAX_DECOMPOSITION_LEVELS) {
            if (maxLevel - 1 >= MAX_SAFE_SHIFT_BITS) {
                break;
            }
            
            try {
                long scaledFilterLength = Math.addExact(
                    Math.multiplyExact((long)filterLengthMinus1, 1L << (maxLevel - 1)), 
                    1L
                );
                
                if (scaledFilterLength > signalLength) {
                    break;
                }
            } catch (ArithmeticException e) {
                break;
            }
            
            maxLevel++;
        }
        
        return maxLevel - 1;
    }
    
    /**
     * Holds a pair of scaled filters for a specific level.
     */
    private record FilterSet(double[] scaledLowPass, double[] scaledHighPass) {}

    /**
     * Releases resources if this instance owns its executor.
     */
    @Override
    public void close() {
        if (ownsExecutor && executor instanceof java.util.concurrent.ExecutorService es) {
            es.shutdown();
        }
    }

    /**
     * Builder for configuring {@link ParallelMultiLevelMODWT}.
     */
    public static final class Builder {
        private Executor executor;
        private Integer parallelism;
        private int minParallelSignalLength;

        /**
         * Creates a new Builder.
         */
        public Builder() {
            // Default constructor
        }

        /**
         * Use a specific executor for parallel tasks.
         *
         * @param executor an {@link Executor} to run parallel tasks
         * @return this builder
         */
        public Builder executor(Executor executor) {
            this.executor = executor;
            return this;
        }

        /**
         * Create an internal ForkJoinPool with given parallelism. Ignored if executor is set.
         *
         * @param parallelism desired parallelism level
         * @return this builder
         */
        public Builder parallelism(int parallelism) {
            this.parallelism = parallelism;
            return this;
        }

        /**
         * Minimum signal length required to use parallel execution. Smaller inputs run sequentially.
         * Default is 0 (always parallel as before).
         *
         * @param minParallelSignalLength minimum length to trigger parallel path
         * @return this builder
         */
        public Builder minParallelSignalLength(int minParallelSignalLength) {
            this.minParallelSignalLength = Math.max(0, minParallelSignalLength);
            return this;
        }

        /**
         * Builds a configured {@link ParallelMultiLevelMODWT} instance.
         *
         * @return a new ParallelMultiLevelMODWT
         * @throws IllegalStateException if required parameters are missing
         */
        public ParallelMultiLevelMODWT build() {
            if (executor != null) {
                return new ParallelMultiLevelMODWT(executor, false, minParallelSignalLength);
            }
            int par = parallelism != null ? parallelism : 0;
            boolean owns = par > 0;
            Executor exec = owns ? new ForkJoinPool(par) : ForkJoinPool.commonPool();
            return new ParallelMultiLevelMODWT(exec, owns, minParallelSignalLength);
        }
    }
}
