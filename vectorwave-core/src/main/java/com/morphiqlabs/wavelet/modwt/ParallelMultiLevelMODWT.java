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
 */
public class ParallelMultiLevelMODWT {
    
    private static final int MAX_DECOMPOSITION_LEVELS = 10;
    private static final int MAX_SAFE_SHIFT_BITS = 31;
    
    private final Executor executor;
    
    /**
     * Creates a parallel multi-level MODWT using the common ForkJoinPool.
     */
    public ParallelMultiLevelMODWT() {
        this(ForkJoinPool.commonPool());
    }
    
    /**
     * Creates a parallel multi-level MODWT with a custom executor.
     * 
     * @param executor The executor to use for parallel tasks
     */
    public ParallelMultiLevelMODWT(Executor executor) {
        this.executor = executor;
    }
    
    /**
     * Performs parallel multi-level MODWT decomposition.
     * 
     * @param signal Input signal
     * @param wavelet Wavelet to use
     * @param mode Boundary mode
     * @param levels Number of decomposition levels
     * @return Multi-level MODWT result
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
        
        // Pre-allocate all arrays upfront
        double[][] approxArrays = new double[levels + 1][signal.length];
        double[][] detailArrays = new double[levels][signal.length];
        
        // Copy input signal to first approximation
        System.arraycopy(signal, 0, approxArrays[0], 0, signal.length);
        
        // Create filter arrays for all levels upfront
        FilterSet[] filterSets = precomputeFilterSets(dw, levels);
        
        // Create CompletableFuture chain for level dependencies
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        
        for (int level = 1; level <= levels; level++) {
            final int currentLevel = level;
            final int prevLevel = level - 1;
            final FilterSet filters = filterSets[level - 1];
            
            chain = chain.thenCompose(v -> {
                // Parallelize low-pass and high-pass filtering
                CompletableFuture<Void> lowPassFuture = CompletableFuture.runAsync(() -> {
                    // Apply low-pass filter
                    if (mode == BoundaryMode.PERIODIC) {
                        WaveletOperations.circularConvolveMODWT(
                            approxArrays[prevLevel], 
                            filters.scaledLowPass, 
                            approxArrays[currentLevel]
                        );
                    } else {
                        // Zero-padding mode
                        applyZeroPaddingMODWT(
                            approxArrays[prevLevel],
                            filters.scaledLowPass,
                            approxArrays[currentLevel]
                        );
                    }
                }, executor);
                
                CompletableFuture<Void> highPassFuture = CompletableFuture.runAsync(() -> {
                    // Apply high-pass filter
                    if (mode == BoundaryMode.PERIODIC) {
                        WaveletOperations.circularConvolveMODWT(
                            approxArrays[prevLevel], 
                            filters.scaledHighPass, 
                            detailArrays[currentLevel - 1]
                        );
                    } else {
                        // Zero-padding mode
                        applyZeroPaddingMODWT(
                            approxArrays[prevLevel],
                            filters.scaledHighPass,
                            detailArrays[currentLevel - 1]
                        );
                    }
                }, executor);
                
                // Wait for both filters to complete before proceeding to next level
                return CompletableFuture.allOf(lowPassFuture, highPassFuture);
            });
        }
        
        // Wait for all levels to complete
        chain.join();
        
        // Copy results to output structure
        for (int level = 1; level <= levels; level++) {
            result.setDetailCoeffsAtLevel(level, detailArrays[level - 1]);
        }
        result.setApproximationCoeffs(approxArrays[levels]);
        
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
            filterSets[level - 1] = scaleFiltersForLevel(lowPass, highPass, level);
        }
        
        return filterSets;
    }
    
    /**
     * Scales filters for a specific level.
     */
    private FilterSet scaleFiltersForLevel(double[] lowFilter, double[] highFilter, int level) {
        double scale = 1.0 / Math.sqrt(2.0); // MODWT scaling factor
        
        if (level == 1) {
            // Level 1: no upsampling, just scaling
            double[] scaledLow = new double[lowFilter.length];
            double[] scaledHigh = new double[highFilter.length];
            
            for (int i = 0; i < lowFilter.length; i++) {
                scaledLow[i] = lowFilter[i] * scale;
            }
            for (int i = 0; i < highFilter.length; i++) {
                scaledHigh[i] = highFilter[i] * scale;
            }
            
            return new FilterSet(scaledLow, scaledHigh);
        }
        
        // For levels > 1, insert zeros between coefficients
        int upFactor = 1 << (level - 1);
        
        // Calculate new filter lengths
        int scaledLowLength = (lowFilter.length - 1) * upFactor + 1;
        int scaledHighLength = (highFilter.length - 1) * upFactor + 1;
        
        double[] scaledLow = new double[scaledLowLength];
        double[] scaledHigh = new double[scaledHighLength];
        
        // Insert zeros and scale
        for (int i = 0; i < lowFilter.length; i++) {
            scaledLow[i * upFactor] = lowFilter[i] * scale;
        }
        for (int i = 0; i < highFilter.length; i++) {
            scaledHigh[i * upFactor] = highFilter[i] * scale;
        }
        
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
        
        if (signalLength <= filterLength) {
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
}