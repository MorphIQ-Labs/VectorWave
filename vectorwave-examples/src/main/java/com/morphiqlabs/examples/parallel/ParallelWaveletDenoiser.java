package com.morphiqlabs.examples.parallel;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;
// Avoid core-internal exceptions in examples
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResultImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.stream.IntStream;

/**
 * Parallel implementation of wavelet-based signal denoising.
 *
 * <p>This examples-module class mirrors the extensions API but does not implement
 * AutoCloseable. When constructing with a custom {@link ParallelConfig}, callers are
 * responsible for invoking {@link ParallelConfig#shutdown()} when finished to release
 * any dedicated executors (the common ForkJoinPool is never shut down). If you want
 * try-with-resources semantics, use the extensions module’s
 * {@code com.morphiqlabs.wavelet.parallel.ParallelWaveletDenoiser}.</p>
 *
 * <p>This class accelerates denoising operations by processing multiple
 * decomposition levels in parallel. Key features:</p>
 * <ul>
 *   <li>Parallel threshold calculation across levels</li>
 *   <li>Concurrent coefficient thresholding</li>
 *   <li>Batch denoising for multiple signals</li>
 *   <li>Adaptive parallelization based on signal/level count</li>
 *   <li>Virtual thread support for I/O-bound operations</li>
 * </ul>
 * 
 * <p>Performance characteristics:</p>
 * <ul>
 *   <li>2-4x speedup for multi-level denoising (4+ levels)</li>
 *   <li>Near-linear scaling with available cores</li>
 *   <li>Automatic fallback to sequential for small problems</li>
 *   <li>Memory-efficient parallel processing</li>
 * </ul>
 */
public class ParallelWaveletDenoiser extends WaveletDenoiser implements AutoCloseable {
    
    private final ParallelConfig parallelConfig;
    private final ParallelMultiLevelTransform parallelTransform;
    private final boolean useParallelThresholding;
    private final boolean ownsConfig;
    
    /**
     * Creates a parallel denoiser with auto-configuration.
     * 
     * @param wavelet the wavelet to use
     * @param boundaryMode the boundary handling mode
     */
    public ParallelWaveletDenoiser(Wavelet wavelet, BoundaryMode boundaryMode) {
        super(wavelet, boundaryMode);
        this.parallelConfig = ParallelConfig.auto();
        this.parallelTransform = new ParallelMultiLevelTransform(wavelet, boundaryMode, parallelConfig);
        this.useParallelThresholding = parallelConfig.isEnableParallelThresholding();
        this.ownsConfig = true;
    }
    
    /**
     * Creates a parallel denoiser with custom configuration.
     * 
     * @param wavelet the wavelet to use
     * @param boundaryMode the boundary handling mode
     * @param parallelConfig parallel execution configuration
     */
    public ParallelWaveletDenoiser(Wavelet wavelet, BoundaryMode boundaryMode,
                                   ParallelConfig parallelConfig) {
        super(wavelet, boundaryMode);
        this.parallelConfig = parallelConfig;
        this.parallelTransform = new ParallelMultiLevelTransform(wavelet, boundaryMode, parallelConfig);
        this.useParallelThresholding = parallelConfig.isEnableParallelThresholding();
        this.ownsConfig = false;
    }
    
    @Override
    public double[] denoise(double[] signal, WaveletDenoiser.ThresholdMethod method, WaveletDenoiser.ThresholdType type) {
        if (signal == null || signal.length == 0) {
            throw new IllegalArgumentException("Signal cannot be null or empty");
        }
        
        // Determine optimal decomposition level
        int levels = calculateOptimalLevels(signal.length);
        
        // Check if parallel processing is beneficial
        if (shouldUseParallelProcessing(signal.length, levels)) {
            parallelConfig.recordExecution(true);
            return denoiseParallel(signal, levels, method, type);
        } else {
            parallelConfig.recordExecution(false);
            return super.denoise(signal, method, type);
        }
    }
    
    @Override
    public double[] denoiseMultiLevel(double[] signal, int levels, 
                                     WaveletDenoiser.ThresholdMethod method, WaveletDenoiser.ThresholdType type) {
        if (signal == null || signal.length == 0) {
            throw new IllegalArgumentException("Signal cannot be null or empty");
        }
        
        // Check if parallel processing is beneficial
        if (shouldUseParallelProcessing(signal.length, levels)) {
            parallelConfig.recordExecution(true);
            return denoiseParallel(signal, levels, method, type);
        } else {
            parallelConfig.recordExecution(false);
            return super.denoiseMultiLevel(signal, levels, method, type);
        }
    }
    
    /**
     * Performs parallel denoising of a signal.
     * 
     * @param signal the noisy signal
     * @param levels number of decomposition levels
     * @param method threshold calculation method
     * @param type thresholding type (soft/hard)
     * @return denoised signal
     */
    private double[] denoiseParallel(double[] signal, int levels,
                                    WaveletDenoiser.ThresholdMethod method, WaveletDenoiser.ThresholdType type) {
        // Parallel decomposition
        MultiLevelMODWTResult decomposition = parallelTransform.decompose(signal, levels);
        
        // Estimate noise level from finest detail coefficients
        double sigma = estimateNoiseSigma(decomposition.getDetailCoeffsAtLevel(1));
        
        // Parallel thresholding across levels
        MultiLevelMODWTResult denoisedResult;
        if (useParallelThresholding) {
            denoisedResult = applyParallelThresholding(decomposition, sigma, method, type);
        } else {
            denoisedResult = applySequentialThresholding(decomposition, sigma, method, type);
        }
        
        // Parallel reconstruction
        return parallelTransform.reconstruct(denoisedResult);
    }

    /**
     * If this denoiser created its own ParallelConfig (default constructor),
     * closes owned resources by shutting down executors. If constructed with
     * an external ParallelConfig, the caller remains responsible for shutdown.
     */
    @Override
    public void close() {
        if (ownsConfig) {
            try { parallelConfig.shutdown(); } catch (Throwable ignored) {}
        }
    }
    
    /**
     * Applies thresholding to all levels in parallel.
     * 
     * @param decomposition the wavelet decomposition
     * @param sigma noise standard deviation
     * @param method threshold calculation method
     * @param type thresholding type
     * @return denoised result
     */
    private MultiLevelMODWTResult applyParallelThresholding(
            MultiLevelMODWTResult decomposition,
            double sigma, WaveletDenoiser.ThresholdMethod method, WaveletDenoiser.ThresholdType type) {
        
        ExecutorService executor = parallelConfig.getCPUExecutor();
        int levels = decomposition.getLevels();
        List<CompletableFuture<LevelThresholdResult>> futures = new ArrayList<>(levels);
        
        try {
            // Create tasks for each level
            for (int level = 1; level <= levels; level++) {
                final int currentLevel = level;
                final double[] details = decomposition.getDetailCoeffsAtLevel(level);
                
                CompletableFuture<LevelThresholdResult> future = CompletableFuture.supplyAsync(
                    () -> {
                        // Calculate level-dependent threshold
                        double levelScale = Math.sqrt(1 << (currentLevel - 1));
                        double threshold = calculateThreshold(details, sigma / levelScale, method);
                        
                        // Apply thresholding
                        double[] thresholded = applyThreshold(details, threshold, type);
                        
                        return new LevelThresholdResult(currentLevel, thresholded, threshold);
                    },
                    executor
                );
                
                futures.add(future);
            }
            
            // Wait for all thresholding operations to complete
            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture<?>[0])
            );
            allFutures.get();
            
            // Collect results and build denoised result
            MutableMultiLevelMODWTResultImpl denoisedResult = 
                new MutableMultiLevelMODWTResultImpl(
                    decomposition.getSignalLength(), 
                    decomposition.getLevels()
                );
            
            // Set approximation coefficients (not denoised)
            denoisedResult.setApproximationCoeffs(decomposition.getApproximationCoeffs());
            
            // Set denoised detail coefficients
            for (CompletableFuture<LevelThresholdResult> future : futures) {
                LevelThresholdResult result = future.join();
                denoisedResult.setDetailCoeffs(result.level, result.thresholdedCoeffs);
            }
            
            return denoisedResult;
            
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Parallel thresholding failed", e);
        } finally {
            // Cancel any remaining futures to free up resources
            for (CompletableFuture<LevelThresholdResult> future : futures) {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            }
        }
    }
    
    /**
     * Applies thresholding sequentially (fallback for small problems).
     * 
     * @param decomposition the wavelet decomposition
     * @param sigma noise standard deviation
     * @param method threshold calculation method
     * @param type thresholding type
     * @return denoised result
     */
    private MultiLevelMODWTResult applySequentialThresholding(
            MultiLevelMODWTResult decomposition,
            double sigma, WaveletDenoiser.ThresholdMethod method, WaveletDenoiser.ThresholdType type) {
        
        MutableMultiLevelMODWTResultImpl denoisedResult = 
            new MutableMultiLevelMODWTResultImpl(
                decomposition.getSignalLength(), 
                decomposition.getLevels()
            );
        
        // Set approximation coefficients (not denoised)
        denoisedResult.setApproximationCoeffs(decomposition.getApproximationCoeffs());
        
        // Process each level sequentially
        for (int level = 1; level <= decomposition.getLevels(); level++) {
            double[] details = decomposition.getDetailCoeffsAtLevel(level);
            
            // Calculate level-dependent threshold
            double levelScale = Math.sqrt(1 << (level - 1));
            double threshold = calculateThreshold(details, sigma / levelScale, method);
            
            // Apply thresholding
            double[] thresholded = applyThreshold(details, threshold, type);
            denoisedResult.setDetailCoeffs(level, thresholded);
        }
        
        return denoisedResult;
    }
    
    /**
     * Batch denoising of multiple signals in parallel.
     * 
     * @param signals array of noisy signals
     * @param method threshold calculation method
     * @param type thresholding type
     * @return array of denoised signals
     */
    public double[][] denoiseBatch(double[][] signals, 
                                  WaveletDenoiser.ThresholdMethod method, WaveletDenoiser.ThresholdType type) {
        if (signals == null || signals.length == 0) {
            throw new IllegalArgumentException("Signals cannot be null or empty");
        }
        
        ExecutorService executor = parallelConfig.getCPUExecutor();
        List<CompletableFuture<double[]>> futures = null;
        
        try {
            // Process signals in parallel
            futures = IntStream.range(0, signals.length)
                .mapToObj(i -> CompletableFuture.supplyAsync(
                    () -> denoise(signals[i], method, type),
                    executor
                ))
                .toList();
            
            // Wait for all completions
            CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).get();
            
            // Collect results
            double[][] denoised = new double[signals.length][];
            for (int i = 0; i < signals.length; i++) {
                denoised[i] = futures.get(i).join();
            }
            
            return denoised;
            
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Batch denoising failed", e);
        } finally {
            // Cancel any remaining futures to free up resources
            if (futures != null) {
                for (CompletableFuture<double[]> future : futures) {
                    if (!future.isDone()) {
                        future.cancel(true);
                    }
                }
            }
        }
    }
    
    /**
     * Streaming denoising with parallel processing of blocks.
     * 
     * @param signal continuous signal stream
     * @param blockSize size of each processing block
     * @param overlap overlap between consecutive blocks
     * @param method threshold calculation method
     * @param type thresholding type
     * @return denoised signal
     */
    public double[] denoiseStreaming(double[] signal, int blockSize, int overlap,
                                    WaveletDenoiser.ThresholdMethod method, WaveletDenoiser.ThresholdType type) {
        if (signal == null || signal.length == 0) {
            throw new IllegalArgumentException("Signal cannot be null or empty");
        }
        if (blockSize <= overlap) {
            throw new IllegalArgumentException("Block size must be greater than overlap");
        }
        
        int hopSize = blockSize - overlap;
        int numBlocks = (signal.length - overlap) / hopSize;
        
        ExecutorService executor = parallelConfig.getCPUExecutor();
        double[] result = new double[signal.length];
        List<CompletableFuture<BlockResult>> futures = new ArrayList<>();
        
        try {
            // Process blocks in parallel
            for (int i = 0; i < numBlocks; i++) {
                final int blockIndex = i;
                final int start = blockIndex * hopSize;
                final int end = Math.min(start + blockSize, signal.length);
                
                // Extract block
                double[] block = new double[end - start];
                System.arraycopy(signal, start, block, 0, block.length);
                
                // Submit denoising task
                CompletableFuture<BlockResult> future = CompletableFuture.supplyAsync(
                    () -> {
                        double[] denoised = denoise(block, method, type);
                        return new BlockResult(blockIndex, start, denoised);
                    },
                    executor
                );
                
                futures.add(future);
            }
            
            // Wait for all blocks to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).get();
            
            // Combine results with overlap-add
            double[] weights = new double[signal.length];
            
            for (CompletableFuture<BlockResult> future : futures) {
                BlockResult blockResult = future.join();
                
                // Add block to result with overlap handling
                for (int j = 0; j < blockResult.data.length; j++) {
                    int idx = blockResult.startIndex + j;
                    result[idx] += blockResult.data[j];
                    weights[idx] += 1.0;
                }
            }
            
            // Normalize by weights
            for (int i = 0; i < result.length; i++) {
                if (weights[i] > 0) {
                    result[i] /= weights[i];
                }
            }
            
            return result;
            
        } catch (ExecutionException | InterruptedException e) {
            throw new RuntimeException("Streaming denoising failed", e);
        } finally {
            // Cancel any remaining futures to free up resources
            for (CompletableFuture<BlockResult> future : futures) {
                if (!future.isDone()) {
                    future.cancel(true);
                }
            }
        }
    }
    
    /**
     * Determines if parallel processing should be used.
     * 
     * @param signalLength length of the signal
     * @param levels number of decomposition levels
     * @return true if parallel processing is beneficial
     */
    private boolean shouldUseParallelProcessing(int signalLength, int levels) {
        // Complexity increases with level count
        double complexity = Math.log(levels + 1);
        
        // Consider both signal size and level count
        return levels >= 3 && 
               parallelConfig.shouldParallelize(signalLength, complexity);
    }
    
    /**
     * Calculates optimal decomposition levels for signal length.
     * 
     * @param signalLength length of the signal
     * @return optimal number of levels
     */
    private int calculateOptimalLevels(int signalLength) {
        // Use log2 heuristic with safety margin - more conservative for MODWT
        int maxLevels = (int) (Math.log(signalLength) / Math.log(2)) - 4;
        return Math.max(1, Math.min(maxLevels, 6)); // Cap at 6 levels for stability
    }
    
    /**
     * Gets performance statistics.
     * 
     * @return execution statistics
     */
    public ParallelConfig.ExecutionStats getStats() {
        return parallelConfig.getStats();
    }
    
    /**
     * Result of thresholding a single level.
     */
    private record LevelThresholdResult(
        int level,
        double[] thresholdedCoeffs,
        double threshold
    ) {}
    
    /**
     * Result of processing a signal block.
     */
    private record BlockResult(
        int blockIndex,
        int startIndex,
        double[] data
    ) {}
    
    /**
     * Builder for creating parallel denoisers.
     */
    public static class Builder {
        public Builder() {}
        private Wavelet wavelet;
        private BoundaryMode boundaryMode = BoundaryMode.PERIODIC;
        private ParallelConfig parallelConfig = ParallelConfig.auto();
        
        public Builder wavelet(Wavelet wavelet) {
            this.wavelet = wavelet;
            return this;
        }
        
        public Builder boundaryMode(BoundaryMode mode) {
            this.boundaryMode = mode;
            return this;
        }
        
        public Builder parallelConfig(ParallelConfig config) {
            this.parallelConfig = config;
            return this;
        }
        
        public ParallelWaveletDenoiser build() {
            if (wavelet == null) {
                throw new IllegalStateException("Wavelet must be specified");
            }
            return new ParallelWaveletDenoiser(wavelet, boundaryMode, parallelConfig);
        }
    }
}
