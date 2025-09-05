package com.morphiqlabs.wavelet.api;

/**
 * The Haar wavelet, the simplest possible wavelet.
 *
 * <p>The Haar wavelet is a step function that represents the simplest
 * orthogonal wavelet. It has compact support and is particularly useful
 * for educational purposes and applications requiring fast computation.</p>
 *
 * <p>Properties:
 * <ul>
 *   <li>Orthogonal</li>
 *   <li>Compact support of width 2</li>
 *   <li>1 vanishing moment</li>
 *   <li>Discontinuous</li>
 * </ul>
 *
 * <h2>Mathematical Foundation and History</h2>
 * <p>The Haar wavelet was introduced by Alfréd Haar in 1909, making it the first
 * wavelet ever described. It predates the formal wavelet theory by many decades.</p>
 *
 * <h2>Coefficient Sources</h2>
 * <p>The Haar wavelet coefficients are derived from the mathematical definition:</p>
 * <ul>
 *   <li>Haar, A. (1910). "Zur Theorie der orthogonalen Funktionensysteme",
 *       Mathematische Annalen, 69, pp. 331-371.</li>
 *   <li>Mallat, S. (2008). "A Wavelet Tour of Signal Processing", 3rd edition,
 *       Academic Press, Section 7.2.</li>
 *   <li>The coefficients are normalized to satisfy h[0] + h[1] = √2</li>
 * </ul>
 *
 * <p>The normalized Haar scaling function coefficients are:</p>
 * <ul>
 *   <li>h[0] = 1/√2 ≈ 0.7071067811865476</li>
 *   <li>h[1] = 1/√2 ≈ 0.7071067811865476</li>
 * </ul>
 */
public record Haar() implements OrthogonalWavelet {
    private static final double SQRT2_INV = 1.0 / Math.sqrt(2);

    // Pre-computed filter coefficients to avoid allocations
    private static final double[] LOW_PASS_COEFFS = {SQRT2_INV, SQRT2_INV};
    private static final double[] HIGH_PASS_COEFFS = {SQRT2_INV, -SQRT2_INV};
    
    // Static instance for consistency with other wavelets
    public static final Haar INSTANCE = new Haar();

    @Override
    public String name() {
        return "Haar";
    }

    @Override
    public String description() {
        return "Haar wavelet - the simplest orthogonal wavelet";
    }

    @Override
    public double[] lowPassDecomposition() {
        return LOW_PASS_COEFFS.clone();
    }

    @Override
    public double[] highPassDecomposition() {
        return HIGH_PASS_COEFFS.clone();
    }

    @Override
    public int vanishingMoments() {
        return 1;
    }

    /**
     * Verifies that the Haar wavelet coefficients satisfy the required properties.
     * This method validates the mathematical correctness of the implementation.
     *
     * <p>Properties verified:</p>
     * <ul>
     *   <li>Sum of coefficients equals √2</li>
     *   <li>Sum of squared coefficients equals 1 (normalization)</li>
     *   <li>Orthogonality between low-pass and high-pass filters</li>
     * </ul>
     *
     * @return true if all properties are satisfied
     */
    public boolean verifyCoefficients() {
        double tolerance = 1e-15; // Very high precision for simple Haar coefficients

        // Check sum = √2
        double sum = LOW_PASS_COEFFS[0] + LOW_PASS_COEFFS[1];
        if (Math.abs(sum - Math.sqrt(2)) > tolerance) {
            return false;
        }

        // Check normalization: sum of squares = 1
        double sumSquares = LOW_PASS_COEFFS[0] * LOW_PASS_COEFFS[0] +
                LOW_PASS_COEFFS[1] * LOW_PASS_COEFFS[1];
        if (Math.abs(sumSquares - 1.0) > tolerance) {
            return false;
        }

        // Check orthogonality between low-pass and high-pass
        double dotProduct = LOW_PASS_COEFFS[0] * HIGH_PASS_COEFFS[0] +
                LOW_PASS_COEFFS[1] * HIGH_PASS_COEFFS[1];
        return !(Math.abs(dotProduct) > tolerance);
    }
}
