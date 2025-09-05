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
 *   <li>At level j, filters are upsampled by inserting 2^(j−1)−1 zeros between taps
 *       and scaled such that the effective analysis factor is 2^(−j/2)</li>
 *   <li>Convolution without downsampling (periodic, zero-padding, or symmetric boundaries)</li>
 *   <li>Redundant representation provides more information</li>
 * </ul>
 *
 * <p><strong>Boundary modes:</strong></p>
 * <ul>
 *   <li>PERIODIC: circular convolution (exact perfect reconstruction across levels)</li>
 *   <li>ZERO_PADDING: linear convolution with implicit zeros beyond edges</li>
 *   <li>SYMMETRIC: mirrored extension with inverse alignment strategy guided by
 *       SYMMETRIC_ALIGNMENT.md</li>
 * </ul>
 *
 * <h2>Symmetric Boundary Policy (Short Version)</h2>
 * <p>For SYMMETRIC, inverse reconstruction applies a small level/family‑specific
 * alignment heuristic (±τ<sub>j</sub>) to reduce boundary error. We recommend asserting
 * equality in the interior region rather than at the edges; choose PERIODIC when
 * exact reconstruction is required. See {@code docs/guides/SYMMETRIC_ALIGNMENT.md}.</p>
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
 * @since 1.0.0
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
    
    // Per-instance caches for scaled/upsampled filters by level (Issue 008)
    private final ConcurrentHashMap<Integer, ScaledFilterPair> analysisFilterCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, ScaledFilterPair> synthesisFilterCache = new ConcurrentHashMap<>();
    
    // No truncation-related caches; levels must ensure L_j ≤ N, otherwise we throw.
    
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
    
    // Removed: truncation cache and filter type encoding; see Issue 003.
    
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
        
        // Perform multi-level decomposition using running approximation (pyramid)
        MultiLevelMODWTResultImpl result = new MultiLevelMODWTResultImpl(signal.length, levels);
        double[] current = signal.clone();
        for (int level = 1; level <= levels; level++) {
            // Use textbook analysis scaling (2^{-j/2}) with à trous upsampling
            ScaledFilterPair up = scaleFiltersForLevel(
                wavelet.lowPassDecomposition(), wavelet.highPassDecomposition(), level);
            MODWTResult levelResult = applyScaledMODWT(current, up.lowPass(), up.highPass());
            result.setDetailCoeffsAtLevel(level, levelResult.detailCoeffs());
            current = levelResult.approximationCoeffs();
        }
        result.setApproximationCoeffs(current);
        
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
        
        // Perform multi-level decomposition with mutable result (pyramid)
        MutableMultiLevelMODWTResultImpl result = new MutableMultiLevelMODWTResultImpl(signal.length, levels);
        double[] current = signal.clone();
        for (int level = 1; level <= levels; level++) {
            // Use textbook analysis scaling (2^{-j/2}) with à trous upsampling
            ScaledFilterPair up = scaleFiltersForLevel(
                wavelet.lowPassDecomposition(), wavelet.highPassDecomposition(), level);
            MODWTResult levelResult = applyScaledMODWT(current, up.lowPass(), up.highPass());
            result.setDetailCoeffsAtLevelDirect(level, levelResult.detailCoeffs().clone());
            current = levelResult.approximationCoeffs().clone();
        }
        result.setApproximationCoeffsDirect(current);
        
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
        int J = result.getLevels();
        // Cascade reconstruction from coarsest to finest for all boundary modes
        double[] current = result.getApproximationCoeffs().clone();
        for (int level = J; level >= 1; level--) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            current = reconstructSingleLevel(current, details, level);
        }
        return current;
    }
    
    /**
     * Reconstructs signal from a specific level, discarding finer details.
     * Useful for denoising by removing high-frequency components.
     * 
     * @param result The multi-level MODWT result
     * @param startLevel The level to start reconstruction from (1 = finest)
     * @return Reconstructed signal without details finer than startLevel
     * @throws NullPointerException if result is null
     * @throws InvalidArgumentException if {@code startLevel} is out of range
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
     * @throws NullPointerException if result is null
     * @throws InvalidArgumentException if the level range is invalid
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
        // For level j, scale both filters per stage (1/sqrt(2)) with à trous upsampling
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
        return applyScaledInverseMODWT(approx, details, upsampledFilters.lowPass(), upsampledFilters.highPass(), level);
    }
    
    
    /**
     * Upsample and scale IMODWT synthesis filters for cascade reconstruction.
     * Delegates to {@link com.morphiqlabs.wavelet.internal.ScalarOps#upsampleAndScaleForIMODWTSynthesis(double[], int)}
     * to keep scaling/upsampling logic centralized.
     */
    private ScaledFilterPair upsampleFiltersForLevel(double[] lowFilter, double[] highFilter, int level) {
        // Cache per level for synthesis path (1/sqrt(2) per stage)
        return synthesisFilterCache.computeIfAbsent(level, l -> {
            double[] upLow = com.morphiqlabs.wavelet.internal.ScalarOps
                .upsampleAndScaleForIMODWTSynthesis(lowFilter, l);
            double[] upHigh = com.morphiqlabs.wavelet.internal.ScalarOps
                .upsampleAndScaleForIMODWTSynthesis(highFilter, l);
            return new ScaledFilterPair(upLow, upHigh);
        });
    }
    
    
    /**
     * Applies inverse MODWT with scaled filters directly.
     */
    private double[] applyScaledInverseMODWT(double[] approx, double[] details,
                                            double[] scaledLowPassRecon, double[] scaledHighPassRecon,
                                            int level) {
        int signalLength = approx.length;
        double[] reconstructed = new double[signalLength];
        
        // Guard only: log once if upsampled filter length exceeds signal length
        if (scaledLowPassRecon.length > signalLength || scaledHighPassRecon.length > signalLength) {
            String msg = com.morphiqlabs.wavelet.exception.ErrorContext.builder(
                    "Upsampled reconstruction filter length exceeds signal length")
                .withWavelet(wavelet)
                .withBoundaryMode(boundaryMode)
                .withContext("Lh (recon)", scaledLowPassRecon.length)
                .withContext("Lg (recon)", scaledHighPassRecon.length)
                .withSignalInfo(signalLength)
                .withLevelInfo(level, getMaximumLevels(signalLength))
                .withSuggestion("Reduce decomposition levels or increase signal length")
                .build();
            throw new com.morphiqlabs.wavelet.exception.InvalidArgumentException(
                com.morphiqlabs.wavelet.exception.ErrorCode.VAL_TOO_LARGE, msg);
        }
        
        if (boundaryMode == BoundaryMode.PERIODIC) {
            // Periodic inverse with synthesis-style indexing (1/sqrt(2) per stage)
            for (int t = 0; t < signalLength; t++) {
                double sum = 0.0;
                for (int l = 0; l < scaledLowPassRecon.length; l++) {
                    int idx = (t + l) % signalLength;
                    sum += scaledLowPassRecon[l] * approx[idx];
                }
                for (int l = 0; l < scaledHighPassRecon.length; l++) {
                    int idx = (t + l) % signalLength;
                    sum += scaledHighPassRecon[l] * details[idx];
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
            // Symmetric synthesis using internal alignment strategy derived from sweep
            SymmetricAlignmentStrategy.Decision dec = SymmetricAlignmentStrategy.decide(wavelet, level);
            final int baseL0Low = wavelet.lowPassReconstruction().length;
            final int baseL0High = wavelet.highPassReconstruction().length;
            final int tauH = computeTauJ(baseL0Low, level) + dec.deltaApprox;
            final int tauG = computeTauJ(baseL0High, level) + dec.deltaDetail;

            for (int t = 0; t < signalLength; t++) {
                double sum = 0.0;
                // Approx branch
                if (dec.approxPlus) {
                    for (int l = 0; l < scaledLowPassRecon.length; l++) {
                        int idx = t + l - tauH;
                        idx = MathUtils.symmetricBoundaryExtension(idx, signalLength);
                        sum += scaledLowPassRecon[l] * approx[idx];
                    }
                } else {
                    for (int l = 0; l < scaledLowPassRecon.length; l++) {
                        int idx = t - l + tauH;
                        idx = MathUtils.symmetricBoundaryExtension(idx, signalLength);
                        sum += scaledLowPassRecon[l] * approx[idx];
                    }
                }
                // Detail branch
                if (dec.detailPlus) {
                    for (int l = 0; l < scaledHighPassRecon.length; l++) {
                        int idx = t + l - tauG;
                        idx = MathUtils.symmetricBoundaryExtension(idx, signalLength);
                        sum += scaledHighPassRecon[l] * details[idx];
                    }
                } else {
                    for (int l = 0; l < scaledHighPassRecon.length; l++) {
                        int idx = t - l + tauG;
                        idx = MathUtils.symmetricBoundaryExtension(idx, signalLength);
                        sum += scaledHighPassRecon[l] * details[idx];
                    }
                }
                reconstructed[t] = sum;
            }
        }
        
        return reconstructed;
    }
    
    private static void reverseInPlace(double[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) {
            double tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }
    
    
    private ScaledFilterPair scaleFiltersForLevel(double[] lowFilter, double[] highFilter, int level) {
        // Cache per level for analysis stage using per-stage scaling (1/sqrt(2))
        return analysisFilterCache.computeIfAbsent(level, l -> {
            double[] upLow = com.morphiqlabs.wavelet.internal.ScalarOps
                .upsampleAndScaleForIMODWTSynthesis(lowFilter, l);
            double[] upHigh = com.morphiqlabs.wavelet.internal.ScalarOps
                .upsampleAndScaleForIMODWTSynthesis(highFilter, l);
            return new ScaledFilterPair(upLow, upHigh);
        });
    }
    
    
    /**
     * Gets the wavelet used by this transform.
     *
     * @return the wavelet used by this transform
     */
    public Wavelet getWavelet() {
        return wavelet;
    }
    
    /**
     * Gets the boundary mode used by this transform.
     *
     * @return the boundary mode used by this transform
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
        
        // Guard only: log once if upsampled filter length exceeds signal length
        if (scaledLowPass.length > signalLength || scaledHighPass.length > signalLength) {
            String msg = com.morphiqlabs.wavelet.exception.ErrorContext.builder(
                    "Upsampled analysis filter length exceeds signal length")
                .withWavelet(wavelet)
                .withBoundaryMode(boundaryMode)
                .withContext("Lh (analysis)", scaledLowPass.length)
                .withContext("Lg (analysis)", scaledHighPass.length)
                .withSignalInfo(signalLength)
                .withSuggestion("Reduce decomposition levels or increase signal length")
                .build();
            throw new com.morphiqlabs.wavelet.exception.InvalidArgumentException(
                com.morphiqlabs.wavelet.exception.ErrorCode.VAL_TOO_LARGE, msg);
        }
        
        if (boundaryMode == BoundaryMode.PERIODIC) {
            // Use ScalarOps circular convolution for MODWT
            // For very short signals or long filters, use scalar implementation directly
            if (signal.length < 64 || scaledLowPass.length > signal.length / 2) {
                // Use scalar implementation directly
                circularConvolveMODWTDirect(signal, scaledLowPass, approximationCoeffs);
                circularConvolveMODWTDirect(signal, scaledHighPass, detailCoeffs);
            } else {
                com.morphiqlabs.wavelet.WaveletOperations.circularConvolveMODWT(
                    signal, scaledLowPass, approximationCoeffs);
                com.morphiqlabs.wavelet.WaveletOperations.circularConvolveMODWT(
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
    
    
    // (periodic synthesis helper removed; cascade path used instead)

    /**
     * Compute τ_j for periodic inverse alignment.
     */
    private static int computeTauJ(int baseFilterLength, int level) {
        int Lminus1 = baseFilterLength - 1;
        if (level <= 1) {
            return Math.max(0, Lminus1 / 2);
        }
        long up = 1L << (level - 1);
        long Lj = (long) Lminus1 * up + 1L;
        long tau = (Lj - 1L) / 2L;
        if (tau < 0) return 0;
        if (tau > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) tau;
    }

    /**
     * In-place circular left shift by k positions.
     */
    private static void circularShiftInPlace(double[] a, int k) {
        int n = a.length;
        if (n == 0) return;
        int s = ((k % n) + n) % n;
        if (s == 0) return;
        reverse(a, 0, s - 1);
        reverse(a, s, n - 1);
        reverse(a, 0, n - 1);
    }

    private static void reverse(double[] a, int i, int j) {
        while (i < j) { double t = a[i]; a[i] = a[j]; a[j] = t; i++; j--; }
    }
}
