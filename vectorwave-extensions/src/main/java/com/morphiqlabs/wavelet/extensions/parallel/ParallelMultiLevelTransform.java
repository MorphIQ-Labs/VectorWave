package com.morphiqlabs.wavelet.extensions.parallel;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.modwt.*;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.IntStream;

/**
 * Parallel multi-level MODWT transform with managed executor lifecycle.
 *
 * <p>This class provides significant performance improvements for multi-level
 * decomposition by processing independent levels in parallel. It implements
 * {@link AutoCloseable} to manage the lifecycle of its internal thread executor.
 * When created with the convenience constructor that uses an implicit
 * {@link ParallelConfig#auto()}, the transform owns its configuration and will
 * release internal resources on {@link #close()}. When supplied an external
 * {@link ParallelConfig}, the caller remains responsible for its lifecycle.
 *
 * <p>Prefer a try-with-resources statement for automatic resource management:
 * <pre>{@code
 * try (var transform = new ParallelMultiLevelTransform(Daubechies.DB4, BoundaryMode.PERIODIC)) {
 *     var result = transform.decompose(signal, 4);
 *     var reconstructed = transform.reconstruct(result);
 * }
 * }</pre>
 *
 * <p><b>Key Features:</b></p>
 * <ul>
 *   <li>Parallel level computation using structured concurrency.</li>
 *   <li>Adaptive parallelization based on signal size and level count.</li>
 *   <li>Automatic fallback to sequential execution for small inputs.</li>
 * </ul>
 *
 * <p><b>Preview/Vector API Note:</b> This module targets Java 24 and may use
 * incubating Vector API and preview features. Running or generating Javadocs for
 * this module requires enabling preview and adding the vector module:
 * {@code --enable-preview --add-modules jdk.incubator.vector}.
 */
public class ParallelMultiLevelTransform extends MultiLevelMODWTTransform implements AutoCloseable {
    
    private final ParallelConfig parallelConfig;
    private final boolean ownsConfig;
    private volatile boolean closed = false;
    private final boolean useStructuredConcurrency;
    private final MODWTTransform singleLevelTransform;
    
    /**
     * Creates a parallel multi-level transform with auto-configuration.
     * 
     * @param wavelet the wavelet to use
     * @param boundaryMode the boundary handling mode
     */
    public ParallelMultiLevelTransform(Wavelet wavelet, BoundaryMode boundaryMode) {
        this(wavelet, boundaryMode, ParallelConfig.auto(), true);
    }
    
    /**
     * Creates a parallel multi-level transform with custom configuration.
     * 
     * @param wavelet the wavelet to use
     * @param boundaryMode the boundary handling mode
     * @param parallelConfig parallel execution configuration
     */
    public ParallelMultiLevelTransform(Wavelet wavelet, BoundaryMode boundaryMode, 
                                      ParallelConfig parallelConfig) {
        this(wavelet, boundaryMode, parallelConfig, false);
    }

    private ParallelMultiLevelTransform(Wavelet wavelet, BoundaryMode boundaryMode,
                                       ParallelConfig parallelConfig, boolean ownsConfig) {
        super(wavelet, boundaryMode);
        this.parallelConfig = parallelConfig;
        this.ownsConfig = ownsConfig;
        this.useStructuredConcurrency = parallelConfig.isEnableStructuredConcurrency();
        this.singleLevelTransform = new MODWTTransform(wavelet, boundaryMode);
    }
    
    @Override
    public MultiLevelMODWTResult decompose(double[] signal, int levels) {
        ensureOpen();
        validateInput(signal, levels);
        
        // Decide whether to use parallel execution
        boolean shouldParallelize = shouldUseParallelExecution(signal.length, levels);
        
        if (shouldParallelize) {
            parallelConfig.recordExecution(true);
            return useStructuredConcurrency ? 
                forwardStructuredConcurrency(signal, levels) :
                forwardParallelForkJoin(signal, levels);
        } else {
            parallelConfig.recordExecution(false);
            return super.decompose(signal, levels);
        }
    }
    
    /**
     * Parallel forward transform using Virtual Threads (stable in Java 24).
     * 
     * @param signal input signal
     * @param levels number of decomposition levels
     * @return multi-level transform result
     */
    private MultiLevelMODWTResult forwardStructuredConcurrency(double[] signal, int levels) {
        ExecutorService executor = parallelConfig.getIOExecutor();
        List<CompletableFuture<LevelResult>> futures = new ArrayList<>(levels);
        
        try {
            // Current signal for progressive decomposition
            double[] currentApprox = signal.clone();
            
            for (int level = 1; level <= levels; level++) {
                final double[] inputSignal = currentApprox.clone();
                final int currentLevel = level;
                
                // Submit task for this level
                CompletableFuture<LevelResult> future = CompletableFuture.supplyAsync(
                    () -> computeSingleLevel(inputSignal, currentLevel),
                    executor
                );
                futures.add(future);
                
                // For next level, we need the approximation from current level
                // In MODWT, we can compute this independently
                if (level < levels) {
                    MODWTResult levelResult = singleLevelTransform.forward(currentApprox);
                    currentApprox = levelResult.approximationCoeffs();
                }
            }
            
            // Wait for all tasks to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture<?>[0])
            );
            allFutures.get();
            
            // Collect results
            List<LevelResult> results = futures.stream()
                .map(CompletableFuture::join)
                .toList();
            
            return assembleResultsFromList(results, signal.length, levels);
            
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Parallel transform failed", e);
        } finally {
            // Cancel any remaining futures to free up resources
            for (CompletableFuture<LevelResult> future : futures) {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            }
        }
    }
    
    /**
     * Parallel forward transform using Fork/Join framework.
     * 
     * @param signal input signal
     * @param levels number of decomposition levels
     * @return multi-level transform result
     */
    private MultiLevelMODWTResult forwardParallelForkJoin(double[] signal, int levels) {
        ForkJoinPool pool = (ForkJoinPool) parallelConfig.getCPUExecutor();
        
        try {
            return pool.submit(() -> {
                // Use parallel stream for level computation
                List<CompletableFuture<LevelResult>> futures = 
                    IntStream.rangeClosed(1, levels)
                        .parallel()
                        .mapToObj(level -> 
                            CompletableFuture.supplyAsync(() -> 
                                computeLevelWithCascade(signal, level),
                                pool))
                        .toList();
                
                try {
                    // Wait for all completions
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
                    
                    // Collect results
                    List<LevelResult> levelResults = futures.stream()
                        .map(CompletableFuture::join)
                        .toList();
                    
                    return assembleResultsFromList(levelResults, signal.length, levels);
                } finally {
                    // Cancel any remaining futures to free up resources
                    for (CompletableFuture<LevelResult> future : futures) {
                        if (!future.isDone()) {
                            future.cancel(true);
                        }
                    }
                }
            }).get();
            
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Parallel transform failed", e);
        }
    }
    
    @Override
    public double[] reconstruct(MultiLevelMODWTResult result) {
        ensureOpen();
        if (result == null) {
            throw new InvalidArgumentException("Result cannot be null");
        }
        
        // Decide whether to use parallel execution
        boolean shouldParallelize = shouldUseParallelExecution(
            result.getApproximationCoeffs().length, 
            result.getLevels()
        );
        
        if (shouldParallelize) {
            parallelConfig.recordExecution(true);
            return inverseParallel(result);
        } else {
            parallelConfig.recordExecution(false);
            return super.reconstruct(result);
        }
    }
    
    /**
     * Parallel inverse transform.
     * 
     * @param result the multi-level result to reconstruct
     * @return reconstructed signal
     */
    private double[] inverseParallel(MultiLevelMODWTResult result) {
        ensureOpen();
        int levels = result.getLevels();
        ExecutorService executor = parallelConfig.getCPUExecutor();
        CompletableFuture<double[]> currentFuture = null;
        
        try {
            // Start with the coarsest approximation
            double[] reconstruction = result.getApproximationCoeffs().clone();
            
            // Process each level in parallel where possible
            for (int level = levels; level >= 1; level--) {
                final double[] approx = reconstruction;
                final double[] details = result.getDetailCoeffsAtLevel(level);
                final int currentLevel = level;
                
                // Submit reconstruction task
                currentFuture = CompletableFuture.supplyAsync(
                    () -> reconstructLevel(approx, details, currentLevel),
                    executor
                );
                
                reconstruction = currentFuture.get();
            }
            
            return reconstruction;
            
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Parallel inverse transform failed", e);
        } finally {
            // Cancel any remaining future to free up resources
            if (currentFuture != null && !currentFuture.isDone()) {
                currentFuture.cancel(true);
            }
        }
    }
    
    /**
     * Determines if parallel execution should be used.
     * 
     * @param signalLength length of the signal
     * @param levels number of levels
     * @return true if parallel execution is beneficial
     */
    private boolean shouldUseParallelExecution(int signalLength, int levels) {
        // Complexity factor increases with level count
        double complexity = Math.log(levels + 1);
        
        // Consider both signal size and level count
        return levels >= 3 && 
               parallelConfig.shouldParallelize(signalLength, complexity);
    }
    
    /**
     * Computes a single decomposition level.
     * 
     * @param signal input signal
     * @param level decomposition level (1-based)
     * @return level result containing approximation and detail coefficients
     */
    private LevelResult computeSingleLevel(double[] signal, int level) {
        MODWTResult result = singleLevelTransform.forward(signal);
        return new LevelResult(
            level,
            result.approximationCoeffs(),
            result.detailCoeffs()
        );
    }
    
    /**
     * Computes a level with cascaded decomposition.
     * 
     * @param signal original signal
     * @param targetLevel target decomposition level
     * @return level result
     */
    private LevelResult computeLevelWithCascade(double[] signal, int targetLevel) {
        double[] current = signal.clone();
        double[] details = null;
        
        // Cascade through levels
        for (int level = 1; level <= targetLevel; level++) {
            MODWTResult result = singleLevelTransform.forward(current);
            if (level == targetLevel) {
                details = result.detailCoeffs();
            }
            current = result.approximationCoeffs();
        }
        
        return new LevelResult(targetLevel, current, details);
    }
    
    /**
     * Reconstructs a single level.
     * 
     * @param approximation approximation coefficients
     * @param details detail coefficients
     * @param level current level
     * @return reconstructed signal for this level
     */
    private double[] reconstructLevel(double[] approximation, double[] details, int level) {
        // Create MODWT result for inverse transform
        MODWTResult levelResult = MODWTResult.create(approximation, details);
        return singleLevelTransform.inverse(levelResult);
    }
    
    /**
     * Assembles results from a list of level results.
     * 
     * @param levelResults list of level results
     * @param signalLength original signal length
     * @param levels number of levels
     * @return assembled multi-level result
     */
    private MultiLevelMODWTResult assembleResultsFromList(
            List<LevelResult> levelResults,
            int signalLength, int levels) {
        
        MutableMultiLevelMODWTResultImpl result = 
            new MutableMultiLevelMODWTResultImpl(signalLength, levels);
        
        for (LevelResult levelResult : levelResults) {
            result.setDetailCoeffs(levelResult.level, levelResult.details);
            if (levelResult.level == levels) {
                result.setApproximationCoeffs(levelResult.approximation);
            }
        }
        
        return result;
    }
    
    /**
     * Validates input parameters.
     * 
     * @param signal input signal
     * @param levels number of levels
     */
    private void validateInput(double[] signal, int levels) {
        if (signal == null || signal.length == 0) {
            throw new InvalidArgumentException("Signal cannot be null or empty");
        }
        if (levels < 1) {
            throw new InvalidArgumentException("Levels must be >= 1");
        }
        
        // Check maximum decomposition level
        int maxLevel = calculateMaxLevel(signal.length);
        if (levels > maxLevel) {
            throw new InvalidArgumentException(
                String.format("Maximum decomposition level for signal length %d is %d",
                    signal.length, maxLevel)
            );
        }
    }
    
    /**
     * Calculates maximum decomposition level for given signal length.
     * 
     * @param signalLength length of the signal
     * @return maximum decomposition level
     */
    private int calculateMaxLevel(int signalLength) {
        // For MODWT, we need at least as many samples as the filter length at each level
        // Get the actual filter length from the wavelet
        Wavelet wavelet = getWavelet();
        int filterLength;
        
        if (wavelet instanceof DiscreteWavelet) {
            filterLength = ((DiscreteWavelet) wavelet).supportWidth();
        } else {
            // Fallback for continuous wavelets or others
            filterLength = 8; // Conservative default
        }
        
        // Ensure we have enough samples for stable decomposition
        // MODWT requires at least 2^level * filterLength samples
        return (int) (Math.log(signalLength / (double) filterLength) / Math.log(2));
    }
    
    /**
     * Gets performance statistics.
     * 
     * @return execution statistics
     */
    public ParallelConfig.ExecutionStats getStats() {
        return parallelConfig.getStats();
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException("ParallelMultiLevelTransform is closed");
        }
    }

    /**
     * Releases owned resources. If the transform owns its {@link ParallelConfig}
     * (convenience constructor), this will shutdown any dedicated executors
     * created by the config (never the ForkJoin common pool). When supplied an
     * external config, this method only marks the transform closed; callers
     * should invoke {@link ParallelConfig#shutdown()} themselves.
     */
    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (ownsConfig && parallelConfig != null) {
            parallelConfig.shutdown();
        }
    }
    
    /**
     * Internal class to hold level results.
     */
    private record LevelResult(
        int level,
        double[] approximation,
        double[] details
    ) {}
    
    /**
     * Builder for creating parallel multi-level transforms.
     */
    public static class Builder {
        private Wavelet wavelet;
        private BoundaryMode boundaryMode = BoundaryMode.PERIODIC;
        private ParallelConfig parallelConfig = ParallelConfig.auto();
        
        /** Creates a builder for ParallelMultiLevelTransform. */
        public Builder() {}
        
        /**
         * Sets wavelet to use.
         * @param wavelet wavelet
         * @return this builder
         */
        public Builder wavelet(Wavelet wavelet) {
            this.wavelet = wavelet;
            return this;
        }
        
        /**
         * Sets boundary handling mode.
         * @param mode boundary mode
         * @return this builder
         */
        public Builder boundaryMode(BoundaryMode mode) {
            this.boundaryMode = mode;
            return this;
        }
        
        /**
         * Sets parallel configuration.
         * @param config parallel config
         * @return this builder
         */
        public Builder parallelConfig(ParallelConfig config) {
            this.parallelConfig = config;
            return this;
        }
        
        /**
         * Builds a configured ParallelMultiLevelTransform instance.
         * @return new multi-level transform
         */
        public ParallelMultiLevelTransform build() {
            if (wavelet == null) {
                throw new IllegalStateException("Wavelet must be specified");
            }
            return new ParallelMultiLevelTransform(wavelet, boundaryMode, parallelConfig);
        }
    }
}
