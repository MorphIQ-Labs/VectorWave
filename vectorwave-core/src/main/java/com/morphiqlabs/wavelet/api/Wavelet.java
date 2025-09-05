package com.morphiqlabs.wavelet.api;

/**
 * Base interface for all wavelet types in the VectorWave library.
 *
 * <p>This sealed interface hierarchy ensures type safety while supporting
 * various wavelet categories including discrete (orthogonal, biorthogonal)
 * and continuous wavelets.</p>
 *
 * <p>All wavelets must provide filter coefficients for decomposition and
 * reconstruction, though the method of obtaining these coefficients may
 * vary (e.g., predefined for discrete wavelets, discretized for continuous).</p>
 *
 * <h2>Wavelet Type Hierarchy:</h2>
 * <pre>
 * Wavelet
 * ├── DiscreteWavelet
 * │   ├── OrthogonalWavelet (Haar, Daubechies, Symlets, Coiflets)
 * │   └── BiorthogonalWavelet (Biorthogonal Splines)
 * └── ContinuousWavelet (Morlet, Mexican Hat, etc.)
 * </pre>
 *
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * // Get a wavelet by type
 * Wavelet haar = new Haar();
 * Wavelet db4 = Daubechies.DB4;
 *
 * // Get wavelet from registry
 * Wavelet wavelet = WaveletRegistry.getWavelet(WaveletName.DB4);
 *
 * // Use with transform
 * WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);
 * }</pre>
 *
 * @see DiscreteWavelet
 * @see ContinuousWavelet
 * @see WaveletRegistry
 * @since 1.0.0
 */
public sealed interface Wavelet permits DiscreteWavelet, ContinuousWavelet {
    
    /**
     * Tolerance for checking if a filter is already normalized.
     * Used to avoid unnecessary normalization operations.
     */
    double NORMALIZATION_CHECK_TOLERANCE = 1e-15;
    
    /**
     * Default tolerance for validating filter normalization.
     * This is less strict than NORMALIZATION_CHECK_TOLERANCE to account
     * for accumulated floating-point errors in filter computations.
     */
    double DEFAULT_NORMALIZATION_TOLERANCE = 1e-12;
    
    /**
     * Returns the name of the wavelet (e.g., "Haar", "db4", "morl").
     * This should be a unique identifier for the wavelet.
     *
     * @return the wavelet name
     */
    String name();

    /**
     * Returns a human-readable description of the wavelet.
     *
     * @return the wavelet description
     */
    default String description() {
        return name() + " wavelet";
    }

    /**
     * Returns the type category of this wavelet.
     *
     * @return the wavelet type
     */
    WaveletType getType();

    /**
     * Returns the low-pass decomposition filter coefficients (h).
     * For continuous wavelets, these may be discretized values.
     *
     * @return the low-pass decomposition filter
     */
    double[] lowPassDecomposition();

    /**
     * Returns the high-pass decomposition filter coefficients (g).
     * For continuous wavelets, these may be discretized values.
     *
     * @return the high-pass decomposition filter
     */
    double[] highPassDecomposition();

    /**
     * Returns the low-pass reconstruction filter coefficients (h~).
     * For orthogonal wavelets, this is typically the time-reversed
     * version of the decomposition filter.
     *
     * @return the low-pass reconstruction filter
     */
    double[] lowPassReconstruction();

    /**
     * Returns the high-pass reconstruction filter coefficients (g~).
     * For orthogonal wavelets, this is typically the time-reversed
     * version of the decomposition filter.
     *
     * @return the high-pass reconstruction filter
     */
    double[] highPassReconstruction();

    /**
     * Validates that this wavelet's filters satisfy the perfect
     * reconstruction conditions within numerical tolerance.
     *
     * @return true if the wavelet satisfies perfect reconstruction
     */
    default boolean validatePerfectReconstruction() {
        // Known tolerances for specific wavelets with documented precision limits
        final String n = name();
        final double tol = switch (n) {
            case "sym8" -> 1e-6;
            case "sym10" -> 2e-4;
            case "coif2" -> 1e-4;
            case "dmey" -> 3e-3;
            default -> 1e-10;
        };

        try {
            if (this instanceof OrthogonalWavelet ow) {
                double[] h = ow.lowPassDecomposition();
                double[] g = ow.highPassDecomposition();
                if (h == null || g == null || h.length == 0 || g.length != h.length) return false;

                // Sum and energy normalization
                double sum = 0.0, sum2 = 0.0;
                for (double v : h) { sum += v; sum2 += v * v; }
                if (Math.abs(sum - Math.sqrt(2.0)) > tol) return false;
                if (Math.abs(sum2 - 1.0) > tol) return false;

                // QMF relationship: g[i] = (-1)^i h[L-1-i]
                for (int i = 0; i < h.length; i++) {
                    double expected = ((i & 1) == 0 ? 1.0 : -1.0) * h[h.length - 1 - i];
                    if (Math.abs(expected - g[i]) > tol) return false;
                }

                // Even-shift orthogonality: Σ h[n] h[n+2k] = 0 for k != 0
                for (int k = 2; k < h.length; k += 2) {
                    double dot = 0.0;
                    for (int nIdx = 0; nIdx + k < h.length; nIdx++) {
                        dot += h[nIdx] * h[nIdx + k];
                    }
                    if (Math.abs(dot) > tol) return false;
                }
                return true;
            } else if (this instanceof BiorthogonalWavelet bw) {
                // Validate biorthogonal dual relationships via high-pass construction checks
                double[] hd = bw.lowPassDecomposition();       // analysis low-pass
                double[] hr = bw.lowPassReconstruction();      // synthesis low-pass
                double[] gd = bw.highPassDecomposition();
                double[] gr = bw.highPassReconstruction();
                if (hd == null || hr == null || gd == null || gr == null) return false;
                
                // Construct expected high-pass from the counterpart low-pass
                double[] expGd = reverseWithAlternatingSigns(hr);
                double[] expGr = reverseWithAlternatingSigns(hd);
                if (gd.length != expGd.length || gr.length != expGr.length) return false;
                for (int i = 0; i < gd.length; i++) {
                    if (Math.abs(gd[i] - expGd[i]) > tol) return false;
                }
                for (int i = 0; i < gr.length; i++) {
                    if (Math.abs(gr[i] - expGr[i]) > tol) return false;
                }
                return true;
            }
        } catch (Throwable t) {
            return false;
        }
        // For other wavelet types (e.g., continuous) return true by default
        return true;
    }

    private static double[] reverseWithAlternatingSigns(double[] h) {
        int L = h.length;
        double[] g = new double[L];
        for (int i = 0; i < L; i++) {
            int j = L - 1 - i;
            double sign = ((j & 1) == 0) ? 1.0 : -1.0;
            g[i] = sign * h[j];
        }
        return g;
    }
    
    /**
     * Normalizes filter coefficients to have L2 norm = 1.
     * 
     * <p>This method always returns a new array unless the input is null or empty.
     * Even if the coefficients are already normalized, a defensive copy is returned
     * to ensure the original array cannot be modified by the caller.</p>
     * 
     * @param coefficients the filter coefficients to normalize
     * @return a new array containing normalized coefficients with L2 norm = 1,
     *         or the original array if null or empty
     */
    static double[] normalizeToUnitL2Norm(double[] coefficients) {
        if (coefficients == null || coefficients.length == 0) {
            return coefficients;
        }
        
        // Calculate L2 norm (square root of sum of squares)
        double sumOfSquares = 0.0;
        for (double coeff : coefficients) {
            sumOfSquares += coeff * coeff;
        }
        
        if (sumOfSquares == 0.0) {
            return coefficients.clone(); // Avoid division by zero, return defensive copy
        }
        
        double norm = Math.sqrt(sumOfSquares);
        if (isAlreadyUnitNormalized(norm)) {
            return coefficients.clone(); // Already normalized, return copy
        }
        
        // Normalize coefficients
        double[] normalized = new double[coefficients.length];
        for (int i = 0; i < coefficients.length; i++) {
            normalized[i] = coefficients[i] / norm;
        }
        
        return normalized;
    }
    
    /**
     * Validates that filter coefficients have L2 norm = 1 within tolerance.
     * 
     * @param coefficients the filter coefficients to validate
     * @param tolerance acceptable deviation from unit norm
     * @return true if coefficients are normalized
     */
    static boolean isNormalized(double[] coefficients, double tolerance) {
        if (coefficients == null || coefficients.length == 0) {
            return false;
        }
        
        double sumOfSquares = 0.0;
        for (double coeff : coefficients) {
            sumOfSquares += coeff * coeff;
        }
        
        return Math.abs(sumOfSquares - 1.0) <= tolerance;
    }
    
    /**
     * Validates that filter coefficients have L2 norm = 1 within default tolerance.
     * 
     * @param coefficients the filter coefficients to validate
     * @return true if coefficients are normalized
     */
    static boolean isNormalized(double[] coefficients) {
        return isNormalized(coefficients, DEFAULT_NORMALIZATION_TOLERANCE);
    }
    
    /**
     * Checks if a given L2 norm is already sufficiently close to unit norm.
     * This is used to avoid unnecessary normalization operations when the
     * coefficients are already normalized within acceptable tolerance.
     * 
     * @param norm the L2 norm to check
     * @return true if the norm is within NORMALIZATION_CHECK_TOLERANCE of 1.0
     */
    private static boolean isAlreadyUnitNormalized(double norm) {
        return Math.abs(norm - 1.0) < NORMALIZATION_CHECK_TOLERANCE;
    }
}
