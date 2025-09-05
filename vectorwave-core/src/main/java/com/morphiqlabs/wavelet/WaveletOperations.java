package com.morphiqlabs.wavelet;

import com.morphiqlabs.wavelet.internal.ScalarOps;
import com.morphiqlabs.wavelet.util.FftHeuristics;

/**
 * Public facade for wavelet transform operations (core module, scalar).
 *
 * <p>This class provides access to the core wavelet operations needed by
 * transform implementations while hiding internal implementation details.</p>
 *
 * <p><strong>SIMD Acceleration:</strong> Vector API–based acceleration lives in the optional
 * {@code vectorwave-extensions} module (Java 24 + incubator API). The core module remains
 * portable and scalar; use the extensions for high‑throughput SIMD paths.</p>
 */
public final class WaveletOperations {
    
    private WaveletOperations() {
        // Static utility class
    }
    
    /**
     * Performs circular convolution for MODWT without downsampling.
     * 
     * @param signal input signal
     * @param filter wavelet filter coefficients
     * @param output pre-allocated output array (same length as signal)
     */
    public static void circularConvolveMODWT(double[] signal, double[] filter, double[] output) {
        int n = signal.length;
        int l = filter.length;
        // Centralized heuristic: FFT wins when effective filter is large relative to N
        boolean useFFT = FftHeuristics.shouldUseModwtFFT(n, l);
        if (useFFT) {
            ScalarOps.circularConvolveMODWTFFT(signal, filter, output);
        } else {
            ScalarOps.circularConvolveMODWT(signal, filter, output);
        }
    }
    
    /**
     * Performs zero-padding convolution for MODWT without downsampling.
     * 
     * @param signal input signal
     * @param filter wavelet filter coefficients  
     * @param output pre-allocated output array (same length as signal)
     */
    public static void zeroPaddingConvolveMODWT(double[] signal, double[] filter, double[] output) {
        ScalarOps.zeroPaddingConvolveMODWT(signal, filter, output);
    }

    /**
     * Performs symmetric-extension convolution for MODWT without downsampling.
     *
     * @param signal input signal
     * @param filter wavelet filter coefficients
     * @param output pre-allocated output array (same length as signal)
     */
    public static void symmetricConvolveMODWT(double[] signal, double[] filter, double[] output) {
        ScalarOps.symmetricConvolveMODWT(signal, filter, output);
    }
    
    /**
     * Gets performance information about the current platform's capabilities.
     * 
     * @return performance information including vectorization status
     */
    public static PerformanceInfo getPerformanceInfo() {
        ScalarOps.PerformanceInfo internal = ScalarOps.getPerformanceInfo();
        String platformName = System.getProperty("os.arch", "unknown");
        // In core module, vectorization is always disabled
        String vectorSpecies = "N/A";
        String processingHint = "Vector API not available in core module";
        
        return new PerformanceInfo(
            false,  // vectorization always false in core module
            platformName,
            vectorSpecies,
            processingHint
        );
    }
    
    /**
     * Applies soft thresholding to wavelet coefficients.
     * 
     * <p>Soft thresholding shrinks coefficients toward zero by the threshold amount:
     * <ul>
     *   <li>If |x| ≤ threshold: result = 0</li>
     *   <li>If x {@literal >} threshold: result = x - threshold</li>
     *   <li>If x {@literal <} -threshold: result = x + threshold</li>
     * </ul>
     * 
     * @param coefficients the wavelet coefficients to threshold
     * @param threshold the threshold value (must be non-negative)
     * @return new array with thresholded coefficients
     * @throws IllegalArgumentException if threshold is negative
     */
    public static double[] softThreshold(double[] coefficients, double threshold) {
        if (coefficients == null) {
            throw new IllegalArgumentException("Coefficients array cannot be null");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold must be non-negative");
        }
        return ScalarOps.softThreshold(coefficients, threshold);
    }
    
    /**
     * Applies hard thresholding to wavelet coefficients.
     * 
     * <p>Hard thresholding sets coefficients to zero if their absolute value
     * is less than or equal to the threshold:
     * <ul>
     *   <li>If |x| ≤ threshold: result = 0</li>
     *   <li>If |x| > threshold: result = x</li>
     * </ul>
     * 
     * @param coefficients the wavelet coefficients to threshold
     * @param threshold the threshold value (must be non-negative)
     * @return new array with thresholded coefficients
     * @throws IllegalArgumentException if threshold is negative
     */
    public static double[] hardThreshold(double[] coefficients, double threshold) {
        if (coefficients == null) {
            throw new IllegalArgumentException("Coefficients array cannot be null");
        }
        if (threshold < 0) {
            throw new IllegalArgumentException("Threshold must be non-negative");
        }
        return ScalarOps.hardThreshold(coefficients, threshold);
    }
    
    /**
     * Performance information record about wavelet operations on this platform.
     *
     * @param vectorizationEnabled whether Vector API acceleration is enabled
     * @param platformName OS/arch name
     * @param vectorSpecies Vector species description (or N/A)
     * @param processingHint human-readable processing hint
     */
    public record PerformanceInfo(
        boolean vectorizationEnabled,
        String platformName,
        String vectorSpecies,
        String processingHint
    ) {
        /**
         * Returns a human-readable description of the performance capabilities.
         *
         * @return description of current performance configuration
         */
        public String description() {
            if (vectorizationEnabled) {
                return String.format("Vectorized operations enabled on %s with %s. %s",
                    platformName, vectorSpecies, processingHint);
            } else {
                return String.format("Scalar operations on %s. %s", 
                    platformName, processingHint);
            }
        }
        
        /**
         * Estimates the potential speedup for a given signal length.
         * 
         * @param signalLength the length of the signal
         * @return estimated speedup factor
         */
        public double estimateSpeedup(int signalLength) {
            if (signalLength < 0) {
                throw new IllegalArgumentException("Signal length cannot be negative");
            }
            if (!vectorizationEnabled || signalLength < 64) {
                return 1.0;
            }
            // Simplified estimate - real speedup depends on many factors
            return Math.min(4.0, 1.0 + (signalLength / 1024.0));
        }
    }
}
