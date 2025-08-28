package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletType;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidSignalException;
import com.morphiqlabs.wavelet.exception.ErrorCode;
import com.morphiqlabs.wavelet.exception.ErrorContext;
import com.morphiqlabs.wavelet.util.MathUtils;
import com.morphiqlabs.wavelet.util.ValidationUtils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Performs multi-level MODWT (Maximal Overlap Discrete Wavelet Transform) decomposition and reconstruction.
 * 
 * <p>Multi-level MODWT applies the wavelet transform recursively to the approximation coefficients,
 * creating a hierarchy of detail coefficients at different scales. Unlike standard DWT multi-level
 * decomposition, MODWT preserves the original signal length at each level.</p>
 * 
 * <p><strong>Key advantages over multi-level DWT:</strong></p>
 * <ul>
 *   <li><strong>Shift-invariant at all levels:</strong> Pattern detection is consistent regardless of signal position</li>
 *   <li><strong>No downsampling:</strong> All coefficients align with original time points</li>
 *   <li><strong>Arbitrary signal length:</strong> No power-of-2 restriction</li>
 *   <li><strong>Better for alignment:</strong> Easier to relate features to original signal</li>
 * </ul>
 * 
 * <p><strong>Mathematical foundation:</strong></p>
 * <ul>
 *   <li>At level j, filters are scaled by 2^(j/2)</li>
 *   <li>Circular convolution without downsampling</li>
 *   <li>Redundant representation provides more information</li>
 * </ul>
 * 
 * <p><strong>Usage example:</strong></p>
 * <pre>{@code
 * // Create multi-level MODWT transform
 * MultiLevelMODWTTransform mwt = new MultiLevelMODWTTransform(
 *     Daubechies.DB4, BoundaryMode.PERIODIC);
 * 
 * // Decompose signal to maximum depth
 * double[] signal = getFinancialTimeSeries(); // Any length!
 * MultiLevelMODWTResult result = mwt.decompose(signal);
 * 
 * // Analyze energy at different scales
 * for (int level = 1; level <= result.getLevels(); level++) {
 *     double[] details = result.getDetailCoeffsAtLevel(level);
 *     double energy = computeEnergy(details);
 *     System.out.println("Level " + level + " energy: " + energy);
 * }
 * 
 * // Reconstruct from specific level (denoising)
 * double[] denoised = mwt.reconstructFromLevel(result, 2);
 * 
 * // Partial reconstruction (only specific levels)
 * double[] bandpass = mwt.reconstructLevels(result, 2, 4);
 * }</pre>
 * 
 * @see MODWTTransform
 * @see MultiLevelMODWTResult
 */
public class MultiLevelMODWTTransform {
    
    /**
     * Maximum practical limit for decomposition levels.
     * 
     * <p>This limit is based on several practical considerations:</p>
     * <ul>
     *   <li><strong>Numerical stability:</strong> At level j, filters are upsampled by 2^(j-1),
     *       leading to very long filters at high levels (e.g., level 10 = 512x original length)</li>
     *   <li><strong>Signal resolution:</strong> Most real-world signals lose meaningful information
     *       beyond 8-10 decomposition levels due to finite precision</li>
     *   <li><strong>Memory requirements:</strong> Each level stores full-length coefficient arrays,
     *       so 10 levels require 10x the original signal memory</li>
     *   <li><strong>Practical usage:</strong> Financial and scientific applications rarely need
     *       more than 6-8 levels of decomposition</li>
     * </ul>
     * 
     * <p>For a signal of length N with filter length L, the maximum meaningful level J
     * is determined by the relationship (L-1)(2^J - 1) < N, which gives approximately
     * J ≤ log2(N/(L-1)) levels. This ensures the scaled filter doesn't exceed the signal length.</p>
     * 
     * <p>Examples:</p>
     * <ul>
     *   <li>Signal length 1024, Haar filter (L=2): max ≈ 9 levels</li>
     *   <li>Signal length 4096, DB4 filter (L=8): max ≈ 9 levels</li>
     *   <li>Signal length 65536, DB8 filter (L=16): max ≈ 12 levels</li>
     * </ul>
     * 
     * <p>The limit of 10 provides a reasonable balance between flexibility and practicality.
     * If higher levels are needed for specific applications, this constant can be increased,
     * though performance and numerical stability should be carefully evaluated.</p>
     * 
     * @see Percival, D.B. and Walden, A.T. (2000). "Wavelet Methods for Time Series Analysis",
     *      Cambridge University Press, Section 5.2, pp. 159-161.
     */
    private static final int MAX_DECOMPOSITION_LEVELS = 10;
    
    /**
     * Maximum safe bit shift amount to prevent integer overflow.
     * 
     * <p>In Java, left-shifting an integer by 31 or more bits results in undefined behavior
     * or integer overflow. Specifically, {@code 1 << 31} would overflow to a negative value
     * (Integer.MIN_VALUE). This constant is used to ensure bit shift operations remain safe
     * when calculating powers of 2 for filter upsampling at different decomposition levels.</p>
     * 
     * <p>For MODWT, at level j, filters are upsampled by 2^(j-1), so we need to ensure
     * that (j-1) never exceeds this limit to prevent overflow in scale factor calculations.</p>
     */
    private static final int MAX_SAFE_SHIFT_BITS = 31;
    
    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final MODWTTransform singleLevelTransform;
    
    /**
     * Cache for truncated filters to avoid repeated allocations.
     * Uses a long key encoding both filter type and length to minimize allocations.
     * 
     * Key encoding: upper 32 bits = filter type ordinal, lower 32 bits = length
     * This avoids object allocation on every cache lookup.
     */
    private final Map<Long, double[]> truncatedFilterCache = new ConcurrentHashMap<>();
    
    /**
     * Record to hold a pair of scaled filters for efficient computation.
     * Avoids redundant calculations when scaling both low-pass and high-pass filters.
     */
    private record ScaledFilterPair(double[] lowPass, double[] highPass) {}
    
    /**
     * Validates that a decomposition level is safe for bit shift operations.
     * Prevents integer overflow in calculations like 1 << (level - 1).
     * 
     * @param level the decomposition level to validate (1-based)
     * @param operationName descriptive name of the operation for error message
     * @throws InvalidArgumentException if level would cause overflow
     */
    private static void validateLevelForBitShift(int level, String operationName) {
        if (level - 1 >= MAX_SAFE_SHIFT_BITS) {
            throw new InvalidArgumentException(
                ErrorCode.VAL_TOO_LARGE,
                ErrorContext.builder("Decomposition level would cause integer overflow")
                    .withContext("Operation", operationName)
                    .withLevelInfo(level, MAX_SAFE_SHIFT_BITS)
                    .withContext("Calculation", "2^(" + (level-1) + ") filter upsampling")
                    .withSuggestion("Maximum safe decomposition level is " + MAX_SAFE_SHIFT_BITS)
                    .withSuggestion("For signals requiring extreme decomposition, consider alternative approaches")
                    .build()
            );
        }
    }
    
    /**
     * Filter types for cache key encoding.
     */
    private enum FilterType {
        LOW, HIGH, LOW_RECON, HIGH_RECON
    }
    
    /**
     * Creates a cache key by encoding filter type and target length into a single long value.
     * Upper 32 bits: filter type ordinal (0-3)
     * Lower 32 bits: target length
     * 
     * @param filterType the type of filter (LOW, HIGH, LOW_RECON, HIGH_RECON)
     * @param targetLength the target length for the truncated filter
     * @return encoded cache key
     */
    private static long createFilterCacheKey(FilterType filterType, int targetLength) {
        return (((long) filterType.ordinal()) << 32) | (targetLength & 0xFFFFFFFFL);
    }
    
    /**
     * Constructs a multi-level MODWT transformer.
     * 
     * @param wavelet The wavelet to use for transformations
     * @param boundaryMode The boundary handling mode (PERIODIC, ZERO_PADDING, or SYMMETRIC)
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if boundary mode is not supported
     */
    public MultiLevelMODWTTransform(Wavelet wavelet, BoundaryMode boundaryMode) {
        this.wavelet = Objects.requireNonNull(wavelet, "wavelet cannot be null");
        this.boundaryMode = Objects.requireNonNull(boundaryMode, "boundaryMode cannot be null");
        this.singleLevelTransform = new MODWTTransform(wavelet, boundaryMode);
    }
    
    /**
     * Performs full multi-level MODWT decomposition.
     * Decomposes to the maximum possible level based on signal length and filter length.
     * 
     * @param signal The input signal of any length
     * @return Multi-level decomposition result
     * @throws InvalidSignalException if signal is invalid
     */
    public MultiLevelMODWTResult decompose(double[] signal) {
        int maxLevels = calculateMaxLevels(signal.length);
        return decompose(signal, maxLevels);
    }
    
    /**
     * Performs multi-level MODWT decomposition to specified number of levels.
     * 
     * @param signal The input signal of any length
     * @param levels Number of decomposition levels
     * @return Multi-level decomposition result
     * @throws InvalidSignalException if signal is invalid
     * @throws InvalidArgumentException if levels is invalid
     */
    public MultiLevelMODWTResult decompose(double[] signal, int levels) {
        // Validate inputs
        ValidationUtils.validateFiniteValues(signal, "signal");
        if (signal.length == 0) {
            throw new InvalidSignalException(
                ErrorCode.VAL_EMPTY,
                ErrorContext.builder("Signal cannot be empty for multi-level MODWT")
                    .withContext("Transform type", "Multi-level MODWT")
                    .withWavelet(wavelet)
                    .withBoundaryMode(boundaryMode)
                    .withContext("Requested levels", levels)
                    .withSuggestion("Provide a signal with at least one sample")
                    .build()
            );
        }
        
        int maxLevels = calculateMaxLevels(signal.length);
        if (levels < 1 || levels > maxLevels) {
            throw new InvalidArgumentException(
                ErrorCode.CFG_INVALID_DECOMPOSITION_LEVEL,
                ErrorContext.builder("Invalid number of decomposition levels")
                    .withLevelInfo(levels, maxLevels)
                    .withSignalInfo(signal.length)
                    .withWavelet(wavelet)
                    .withContext("Filter length", wavelet instanceof Wavelet ? 
                        wavelet.lowPassDecomposition().length : "unknown")
                    .withSuggestion("Choose a level between 1 and " + maxLevels)
                    .withSuggestion("Maximum level is floor(log2(signalLength/filterLength)) = " + maxLevels)
                    .build()
            );
        }
        
        // Perform multi-level decomposition
        MultiLevelMODWTResultImpl result = new MultiLevelMODWTResultImpl(signal.length, levels);
        
        double[] currentApprox = signal.clone(); // Start with original signal
        
        for (int level = 1; level <= levels; level++) {
            // Apply single-level MODWT with scaled filters
            MODWTResult levelResult = transformAtLevel(currentApprox, level);
            
            // Store detail coefficients
            result.setDetailCoeffsAtLevel(level, levelResult.detailCoeffs());
            
            // Update approximation for next level
            currentApprox = levelResult.approximationCoeffs();
            
            // Store final approximation if at last level
            if (level == levels) {
                result.setApproximationCoeffs(currentApprox);
            }
        }
        
        return result;
    }
    
    /**
     * Performs multi-level MODWT decomposition producing a mutable result.
     * 
     * <p>This method is designed for applications that need to modify wavelet coefficients
     * directly, such as thresholding for denoising or implementing custom processing algorithms.</p>
     * 
     * @param signal The input signal of any length
     * @return Mutable multi-level decomposition result
     * @throws InvalidSignalException if signal is invalid
     */
    public MutableMultiLevelMODWTResult decomposeMutable(double[] signal) {
        int maxLevels = calculateMaxLevels(signal.length);
        return decomposeMutable(signal, maxLevels);
    }
    
    /**
     * Performs multi-level MODWT decomposition to specified levels, producing a mutable result.
     * 
     * <p>This method creates a result that allows direct modification of wavelet coefficients,
     * useful for applications like SWT (Stationary Wavelet Transform) adaptation.</p>
     * 
     * @param signal The input signal of any length
     * @param levels Number of decomposition levels
     * @return Mutable multi-level decomposition result
     * @throws InvalidSignalException if signal is invalid
     * @throws InvalidArgumentException if levels is invalid
     */
    public MutableMultiLevelMODWTResult decomposeMutable(double[] signal, int levels) {
        // Validate inputs
        ValidationUtils.validateFiniteValues(signal, "signal");
        if (signal.length == 0) {
            throw new InvalidSignalException(
                ErrorCode.VAL_EMPTY,
                ErrorContext.builder("Signal cannot be empty for multi-level MODWT")
                    .withContext("Transform type", "Multi-level MODWT (mutable)")
                    .withWavelet(wavelet)
                    .withBoundaryMode(boundaryMode)
                    .withContext("Requested levels", levels)
                    .withSuggestion("Provide a signal with at least one sample")
                    .build()
            );
        }
        
        int maxLevels = calculateMaxLevels(signal.length);
        if (levels < 1 || levels > maxLevels) {
            throw new InvalidArgumentException(
                ErrorCode.CFG_INVALID_DECOMPOSITION_LEVEL,
                ErrorContext.builder("Invalid number of decomposition levels")
                    .withLevelInfo(levels, maxLevels)
                    .withSignalInfo(signal.length)
                    .withWavelet(wavelet)
                    .withContext("Filter length", wavelet instanceof Wavelet ? 
                        wavelet.lowPassDecomposition().length : "unknown")
                    .withSuggestion("Choose a level between 1 and " + maxLevels)
                    .withSuggestion("Maximum level is floor(log2(signalLength/filterLength)) = " + maxLevels)
                    .build()
            );
        }
        
        // Perform multi-level decomposition with mutable result
        MutableMultiLevelMODWTResultImpl result = new MutableMultiLevelMODWTResultImpl(signal.length, levels);
        
        double[] currentApprox = signal.clone(); // Start with original signal
        
        for (int level = 1; level <= levels; level++) {
            // Apply single-level MODWT with scaled filters
            MODWTResult levelResult = transformAtLevel(currentApprox, level);
            
            // Store detail coefficients directly (avoiding unnecessary cloning)
            result.setDetailCoeffsAtLevelDirect(level, levelResult.detailCoeffs().clone());
            
            // Update approximation for next level
            currentApprox = levelResult.approximationCoeffs();
            
            // Store final approximation if at last level
            if (level == levels) {
                result.setApproximationCoeffsDirect(currentApprox.clone());
            }
        }
        
        return result;
    }
    
    /**
     * Reconstructs the original signal from multi-level MODWT result.
     * 
     * @param result The multi-level MODWT result
     * @return Reconstructed signal
     * @throws NullPointerException if result is null
     */
    public double[] reconstruct(MultiLevelMODWTResult result) {
        Objects.requireNonNull(result, "result cannot be null");
        
        // Start with approximation at coarsest level
        double[] reconstruction = result.getApproximationCoeffs().clone();
        
        // Reconstruct level by level (from coarsest to finest)
        for (int level = result.getLevels(); level >= 1; level--) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            reconstruction = reconstructSingleLevel(reconstruction, details, level);
        }
        
        return reconstruction;
    }
    
    /**
     * Reconstructs signal from a specific level, discarding finer details.
     * Useful for denoising by removing high-frequency components.
     * 
     * @param result The multi-level MODWT result
     * @param startLevel The level to start reconstruction from (1 = finest)
     * @return Reconstructed signal without details finer than startLevel
     */
    public double[] reconstructFromLevel(MultiLevelMODWTResult result, int startLevel) {
        Objects.requireNonNull(result, "result cannot be null");
        
        if (startLevel < 1 || startLevel > result.getLevels()) {
            throw new InvalidArgumentException(
                "Invalid start level: " + startLevel + 
                ". Must be between 1 and " + result.getLevels());
        }
        
        // Start with approximation
        double[] reconstruction = result.getApproximationCoeffs().clone();
        
        // Reconstruct from coarsest to startLevel (skip finer levels)
        for (int level = result.getLevels(); level >= startLevel; level--) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            reconstruction = reconstructSingleLevel(reconstruction, details, level);
        }
        
        // Add zeros for skipped levels to maintain correct length
        for (int level = startLevel - 1; level >= 1; level--) {
            double[] zeroDetails = new double[reconstruction.length];
            reconstruction = reconstructSingleLevel(reconstruction, zeroDetails, level);
        }
        
        return reconstruction;
    }
    
    /**
     * Reconstructs signal using only specific levels (bandpass filtering).
     * 
     * @param result The multi-level MODWT result
     * @param minLevel Minimum level to include (inclusive)
     * @param maxLevel Maximum level to include (inclusive)
     * @return Reconstructed signal containing only specified frequency bands
     */
    public double[] reconstructLevels(MultiLevelMODWTResult result, int minLevel, int maxLevel) {
        Objects.requireNonNull(result, "result cannot be null");
        
        if (minLevel < 1 || maxLevel > result.getLevels() || minLevel > maxLevel) {
            throw new InvalidArgumentException(
                ErrorCode.CFG_INVALID_DECOMPOSITION_LEVEL,
                ErrorContext.builder("Invalid level range for partial reconstruction")
                    .withContext("Requested range", "[" + minLevel + ", " + maxLevel + "]")
                    .withContext("Available levels", result.getLevels())
                    .withContext("Transform type", "Multi-level MODWT reconstruction")
                    .withSuggestion("Ensure minLevel >= 1")
                    .withSuggestion("Ensure maxLevel <= " + result.getLevels())
                    .withSuggestion("Ensure minLevel <= maxLevel")
                    .build()
            );
        }
        
        // Start with zeros for both approximation and details
        double[] reconstruction = new double[result.getSignalLength()];
        double[] zeroApprox = new double[result.getSignalLength()];
        double[] zeroDetails = new double[result.getSignalLength()];  // Reusable zero array
        
        // Reconstruct each level within the range properly
        for (int level = result.getLevels(); level >= 1; level--) {
            double[] details;
            
            if (level >= minLevel && level <= maxLevel) {
                // Use actual detail coefficients for levels in range
                details = result.getDetailCoeffsAtLevel(level);
            } else {
                // Use pre-allocated zeros for levels outside the range
                details = zeroDetails;
            }
            
            // Use proper convolution-based reconstruction
            // If this is the highest level and we're including it, start with approximation
            if (level == result.getLevels() && level <= maxLevel) {
                reconstruction = reconstructSingleLevel(result.getApproximationCoeffs(), details, level);
            } else if (level == result.getLevels()) {
                // Start with zero approximation if highest level is excluded
                reconstruction = reconstructSingleLevel(zeroApprox, details, level);
            } else {
                // Continue reconstruction with previous result
                reconstruction = reconstructSingleLevel(reconstruction, details, level);
            }
        }
        
        return reconstruction;
    }
    
    /**
     * Calculates the maximum number of decomposition levels.
     * For MODWT, this is based on filter length and signal length.
     * 
     * <p>Optimized implementation that avoids expensive operations in loops
     * by using direct calculation where possible.</p>
     */
    private int calculateMaxLevels(int signalLength) {
        int filterLength = wavelet.lowPassDecomposition().length;
        
        // Quick check for edge cases
        if (signalLength <= filterLength) {
            return 0;  // Can't even do one level
        }
        
        // For MODWT: at level j, effective filter length = (L-1)*2^(j-1)+1
        // We need (L-1)*2^(j-1)+1 <= N
        
        // Use the original algorithm's approach, but optimized
        // Start from level 1 and find the maximum valid level
        int maxLevel = 1;
        
        // Pre-compute values to avoid repeated calculations
        int filterLengthMinus1 = filterLength - 1;
        
        // Use bit shifting for powers of 2
        while (maxLevel < MAX_DECOMPOSITION_LEVELS) {
            // Check for potential overflow before shifting
            if (maxLevel - 1 >= MAX_SAFE_SHIFT_BITS) {
                break;  // Stop searching when bit shift would overflow - we've reached the mathematical limit
            }
            
            // Calculate scaled filter length using bit shift with overflow protection
            // This is equivalent to: (filterLength - 1) * 2^(maxLevel - 1) + 1
            try {
                // Use Math.multiplyExact to detect overflow in the multiplication
                long scaledFilterLength = Math.addExact(
                    Math.multiplyExact((long)filterLengthMinus1, 1L << (maxLevel - 1)), 
                    1L
                );
                
                if (scaledFilterLength > signalLength) {
                    break;
                }
            } catch (ArithmeticException e) {
                // Overflow occurred - we've exceeded the maximum possible level
                break;
            }
            
            maxLevel++;
        }
        
        return maxLevel - 1;
    }
    
    /**
     * Performs single-level MODWT with appropriately scaled filters.
     */
    private MODWTResult transformAtLevel(double[] signal, int level) {
        // For level j, scale both filters together to avoid redundant calculations
        ScaledFilterPair scaledFilters = scaleFiltersForLevel(
            wavelet.lowPassDecomposition(), 
            wavelet.highPassDecomposition(), 
            level
        );
        
        // Apply MODWT with scaled filters directly
        return applyScaledMODWT(signal, scaledFilters.lowPass(), scaledFilters.highPass());
    }
    
    /**
     * Reconstructs single level by combining approximation and details.
     */
    private double[] reconstructSingleLevel(double[] approx, double[] details, int level) {
        // For MODWT reconstruction, we need upsampled filters - process both together
        ScaledFilterPair upsampledFilters = upsampleFiltersForLevel(
            wavelet.lowPassReconstruction(), 
            wavelet.highPassReconstruction(), 
            level
        );
        
        // Apply inverse MODWT with the upsampled and scaled filters
        return applyScaledInverseMODWT(approx, details, upsampledFilters.lowPass(), upsampledFilters.highPass());
    }
    
    /**
     * Upsamples both low-pass and high-pass filters for MODWT at given level with scaling.
     * This optimized method avoids redundant calculations by processing both filters together.
     * At level j, insert 2^(j-1) - 1 zeros between coefficients and apply 1/sqrt(2) scaling.
     */
    private ScaledFilterPair upsampleFiltersForLevel(double[] lowFilter, double[] highFilter, int level) {
        if (level == 1) {
            // Level 1: no upsampling, just scale by 1/sqrt(2)
            double scale = 1.0 / Math.sqrt(2.0);
            
            double[] scaledLow = lowFilter.clone();
            double[] scaledHigh = highFilter.clone();
            
            for (int i = 0; i < scaledLow.length; i++) {
                scaledLow[i] *= scale;
            }
            for (int i = 0; i < scaledHigh.length; i++) {
                scaledHigh[i] *= scale;
            }
            
            return new ScaledFilterPair(scaledLow, scaledHigh);
        }
        
        // Check for potential overflow in bit shift (once for both filters)
        validateLevelForBitShift(level, "filter upsampling");
        
        try {
            // Calculate shared values once
            int upFactor = 1 << (level - 1);
            double scale = 1.0 / Math.sqrt(2.0); // MODWT scaling
            
            // Process low-pass filter
            int upsampledLowLength = Math.addExact(
                Math.multiplyExact(lowFilter.length - 1, upFactor), 
                1
            );
            double[] upsampledLow = new double[upsampledLowLength];
            
            for (int i = 0; i < lowFilter.length; i++) {
                upsampledLow[i * upFactor] = lowFilter[i] * scale;
            }
            
            // Process high-pass filter
            int upsampledHighLength = Math.addExact(
                Math.multiplyExact(highFilter.length - 1, upFactor), 
                1
            );
            double[] upsampledHigh = new double[upsampledHighLength];
            
            for (int i = 0; i < highFilter.length; i++) {
                upsampledHigh[i * upFactor] = highFilter[i] * scale;
            }
            
            return new ScaledFilterPair(upsampledLow, upsampledHigh);
            
        } catch (ArithmeticException e) {
            throw new InvalidArgumentException(
                "Arithmetic overflow when upsampling filters for level " + level + 
                ": " + e.getMessage());
        }
    }
    
    
    /**
     * Applies inverse MODWT with scaled filters directly.
     */
    private double[] applyScaledInverseMODWT(double[] approx, double[] details,
                                            double[] scaledLowPassRecon, double[] scaledHighPassRecon) {
        int signalLength = approx.length;
        double[] reconstructed = new double[signalLength];
        
        // For MODWT reconstruction, we need to be careful with long filters
        int maxFilterLength = Math.max(scaledLowPassRecon.length, scaledHighPassRecon.length);
        if (maxFilterLength > signalLength) {
            // Truncate filters if they exceed signal length
            if (scaledLowPassRecon.length > signalLength) {
                scaledLowPassRecon = getTruncatedFilter(scaledLowPassRecon, signalLength, FilterType.LOW_RECON);
            }
            if (scaledHighPassRecon.length > signalLength) {
                scaledHighPassRecon = getTruncatedFilter(scaledHighPassRecon, signalLength, FilterType.HIGH_RECON);
            }
        }
        
        if (boundaryMode == BoundaryMode.PERIODIC) {
            // MODWT reconstruction using circular indexing
            for (int t = 0; t < signalLength; t++) {
                double sum = 0.0;
                for (int l = 0; l < scaledLowPassRecon.length; l++) {
                    int idx = (t + l) % signalLength;
                    sum += scaledLowPassRecon[l] * approx[idx] +
                           scaledHighPassRecon[l] * details[idx];
                }
                reconstructed[t] = sum;
            }
        } else if (boundaryMode == BoundaryMode.ZERO_PADDING) {
            for (int t = 0; t < signalLength; t++) {
                double sum = 0.0;
                for (int l = 0; l < scaledLowPassRecon.length; l++) {
                    int idx = t + l;
                    if (idx < signalLength) {
                        sum += scaledLowPassRecon[l] * approx[idx] +
                               scaledHighPassRecon[l] * details[idx];
                    }
                }
                reconstructed[t] = sum;
            }
        } else {
            // Use symmetric extension for reconstruction
            // For MODWT reconstruction with symmetric boundaries
            for (int t = 0; t < signalLength; t++) {
                double sum = 0.0;
                
                // Apply convolution with reconstruction filters
                int maxLen = Math.max(scaledLowPassRecon.length, scaledHighPassRecon.length);
                for (int l = 0; l < maxLen; l++) {
                    // For reconstruction, we use (t - l) with time-reversed filters
                    int idx = t - l;
                    
                    // Apply symmetric boundary extension using utility method
                    idx = MathUtils.symmetricBoundaryExtension(idx, signalLength);
                    
                    // Add contributions from both filters
                    if (l < scaledLowPassRecon.length) {
                        sum += scaledLowPassRecon[l] * approx[idx];
                    }
                    if (l < scaledHighPassRecon.length) {
                        sum += scaledHighPassRecon[l] * details[idx];
                    }
                }
                reconstructed[t] = sum;
            }
        }
        
        return reconstructed;
    }
    
    /**
     * Scales both low-pass and high-pass filters for MODWT at given level.
     * This optimized method avoids redundant calculations by processing both filters together.
     * At level j, insert 2^(j-1) - 1 zeros between coefficients.
     * MODWT only uses 1/sqrt(2) scaling, regardless of level.
     */
    private ScaledFilterPair scaleFiltersForLevel(double[] lowFilter, double[] highFilter, int level) {
        if (level == 1) {
            // Level 1: no upsampling, just scale by 1/sqrt(2)
            double scale = 1.0 / Math.sqrt(2.0);
            
            double[] scaledLow = lowFilter.clone();
            double[] scaledHigh = highFilter.clone();
            
            for (int i = 0; i < scaledLow.length; i++) {
                scaledLow[i] *= scale;
            }
            for (int i = 0; i < scaledHigh.length; i++) {
                scaledHigh[i] *= scale;
            }
            
            return new ScaledFilterPair(scaledLow, scaledHigh);
        }
        
        // Check for potential overflow in bit shift (once for both filters)
        validateLevelForBitShift(level, "filter scaling");
        
        try {
            // Calculate shared values once
            int upFactor = 1 << (level - 1); // Safer than Math.pow for integer powers of 2
            double scale = 1.0 / Math.sqrt(2.0); // MODWT always uses 1/sqrt(2) scaling
            
            // Process low-pass filter
            int scaledLowLength = Math.addExact(
                Math.multiplyExact(lowFilter.length - 1, upFactor), 
                1
            );
            double[] scaledLow = new double[scaledLowLength];
            
            for (int i = 0; i < lowFilter.length; i++) {
                scaledLow[i * upFactor] = lowFilter[i] * scale;
            }
            
            // Process high-pass filter
            int scaledHighLength = Math.addExact(
                Math.multiplyExact(highFilter.length - 1, upFactor), 
                1
            );
            double[] scaledHigh = new double[scaledHighLength];
            
            for (int i = 0; i < highFilter.length; i++) {
                scaledHigh[i * upFactor] = highFilter[i] * scale;
            }
            
            return new ScaledFilterPair(scaledLow, scaledHigh);
            
        } catch (ArithmeticException e) {
            throw new InvalidArgumentException(
                "Arithmetic overflow when scaling filters for level " + level + 
                ": " + e.getMessage());
        }
    }
    
    
    /**
     * Gets the wavelet used by this transform.
     */
    public Wavelet getWavelet() {
        return wavelet;
    }
    
    /**
     * Gets the boundary mode used by this transform.
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
    
    /**
     * Calculates the theoretical maximum number of decomposition levels for a given signal length.
     * This is based on the mathematical constraint that the scaled filter should not exceed the signal length.
     * 
     * @param signalLength the length of the signal
     * @return the maximum number of decomposition levels (capped at MAX_DECOMPOSITION_LEVELS)
     */
    public int getMaximumLevels(int signalLength) {
        return Math.min(calculateMaxLevels(signalLength), MAX_DECOMPOSITION_LEVELS);
    }
    
    /**
     * Gets the maximum decomposition level limit.
     * 
     * @return the maximum allowed decomposition levels (currently 10)
     */
    public static int getMaxDecompositionLevels() {
        return MAX_DECOMPOSITION_LEVELS;
    }
    
    /**
     * Applies single-level MODWT with scaled filters directly.
     * This avoids the need to create a wavelet wrapper.
     */
    private MODWTResult applyScaledMODWT(double[] signal, double[] scaledLowPass, 
                                         double[] scaledHighPass) {
        int signalLength = signal.length;
        double[] approximationCoeffs = new double[signalLength];
        double[] detailCoeffs = new double[signalLength];
        
        // For very long filters (at high decomposition levels), we may need to truncate
        // to avoid issues with circular convolution
        if (scaledLowPass.length > signalLength) {
            scaledLowPass = getTruncatedFilter(scaledLowPass, signalLength, FilterType.LOW);
        }
        if (scaledHighPass.length > signalLength) {
            scaledHighPass = getTruncatedFilter(scaledHighPass, signalLength, FilterType.HIGH);
        }
        
        if (boundaryMode == BoundaryMode.PERIODIC) {
            // Use ScalarOps circular convolution for MODWT
            // For very short signals or long filters, use scalar implementation directly
            if (signal.length < 64 || scaledLowPass.length > signal.length / 2) {
                // Use scalar implementation directly
                circularConvolveMODWTDirect(signal, scaledLowPass, approximationCoeffs);
                circularConvolveMODWTDirect(signal, scaledHighPass, detailCoeffs);
            } else {
                com.morphiqlabs.wavelet.internal.ScalarOps.circularConvolveMODWT(
                    signal, scaledLowPass, approximationCoeffs);
                com.morphiqlabs.wavelet.internal.ScalarOps.circularConvolveMODWT(
                    signal, scaledHighPass, detailCoeffs);
            }
        } else if (boundaryMode == BoundaryMode.ZERO_PADDING) {
            com.morphiqlabs.wavelet.internal.ScalarOps.zeroPaddingConvolveMODWT(
                signal, scaledLowPass, approximationCoeffs);
            com.morphiqlabs.wavelet.internal.ScalarOps.zeroPaddingConvolveMODWT(
                signal, scaledHighPass, detailCoeffs);
        } else {
            com.morphiqlabs.wavelet.internal.ScalarOps.symmetricConvolveMODWT(
                signal, scaledLowPass, approximationCoeffs);
            com.morphiqlabs.wavelet.internal.ScalarOps.symmetricConvolveMODWT(
                signal, scaledHighPass, detailCoeffs);
        }
        
        return MODWTResult.create(approximationCoeffs, detailCoeffs);
    }
    
    /**
     * Direct circular convolution implementation for MODWT.
     * Used for small signals or when filters are very long.
     */
    private void circularConvolveMODWTDirect(double[] signal, double[] filter, double[] output) {
        int signalLen = signal.length;
        int filterLen = filter.length;
        
        // Ensure we don't go out of bounds with very long filters
        int effectiveFilterLen = Math.min(filterLen, signalLen);
        
        // Optimize for the common case where we don't need wrapping
        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;
            
            // Process coefficients that don't need wrapping
            int maxK = Math.min(effectiveFilterLen, t + 1);
            for (int k = 0; k < maxK; k++) {
                sum += filter[k] * signal[t - k];
            }
            
            // Process coefficients that need wrapping (circular convolution)
            for (int k = maxK; k < effectiveFilterLen; k++) {
                sum += filter[k] * signal[t - k + signalLen];
            }
            
            output[t] = sum;
        }
    }
    
    /**
     * Gets a truncated version of a filter, using cache to avoid repeated allocations.
     * 
     * @param filter the original filter
     * @param targetLength the desired length
     * @param filterType the type of filter for caching
     * @return truncated filter of the specified length
     * @throws IllegalArgumentException if targetLength is invalid
     */
    private double[] getTruncatedFilter(double[] filter, int targetLength, FilterType filterType) {
        if (targetLength <= 0) {
            throw new IllegalArgumentException("Target length must be positive: " + targetLength);
        }
        if (targetLength > filter.length) {
            throw new IllegalArgumentException(
                "Target length (" + targetLength + ") exceeds filter length (" + filter.length + ")");
        }
        
        // Use primitive long key to avoid object allocation
        long cacheKey = createFilterCacheKey(filterType, targetLength);
        
        return truncatedFilterCache.computeIfAbsent(cacheKey, key -> {
            double[] truncated = new double[targetLength];
            System.arraycopy(filter, 0, truncated, 0, targetLength);
            return truncated;
        });
    }
}