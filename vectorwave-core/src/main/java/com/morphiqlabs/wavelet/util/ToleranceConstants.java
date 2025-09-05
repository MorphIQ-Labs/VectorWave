package com.morphiqlabs.wavelet.util;

/**
 * Central repository for tolerance values used throughout the wavelet transform library.
 * 
 * <p>This class consolidates tolerance constants to avoid repetition of detailed explanations
 * and ensures consistent tolerance values across the codebase. Each tolerance includes
 * comprehensive documentation explaining its purpose and rationale.</p>
 */
public final class ToleranceConstants {
    private ToleranceConstants() {
        throw new AssertionError("No instances");
    }

    /**
     * Standard tolerance for basic floating-point comparisons in wavelet operations.
     * 
     * <p>This value (1e-10) is chosen based on typical reconstruction errors achievable
     * with double precision arithmetic in wavelet transforms. It accounts for:</p>
     * <ul>
     *   <li>Cumulative rounding errors in forward/inverse transform chains</li>
     *   <li>Numerical precision limits of IEEE 754 double-precision format</li>
     *   <li>Filter coefficient precision in wavelet implementations</li>
     * </ul>
     */
    public static final double DEFAULT_TOLERANCE = 1e-10;

    /**
     * Energy preservation tolerance for wavelet transforms.
     * 
     * <p>This stricter tolerance (1e-12) ensures that the energy (sum of squares) 
     * is preserved during wavelet transforms, which is a fundamental property 
     * of orthogonal wavelets. The tighter bound is necessary because energy 
     * calculations involve summing many terms, potentially amplifying errors.</p>
     */
    public static final double ENERGY_TOLERANCE = 1e-12;

    /**
     * Orthogonality tolerance for wavelet filter validation.
     * 
     * <p>This very strict tolerance (1e-14) is used when verifying that wavelet
     * filters satisfy orthogonality conditions. The tight bound ensures that
     * mathematical properties required for perfect reconstruction are met.</p>
     */
    public static final double ORTHOGONALITY_TOLERANCE = 1e-14;

    /**
     * Relative error tolerance for extreme value operations.
     * 
     * <p><strong>1% Relative Error Tolerance (0.01)</strong></p>
     * 
     * <p>This tolerance allows up to 1% relative error when dealing with extreme values
     * in signal processing operations. This relaxed tolerance is necessary because:</p>
     * <ul>
     *   <li><strong>Numerical Stability:</strong> Very large or very small values can cause
     *       precision loss in floating-point arithmetic, making exact comparisons unreliable</li>
     *   <li><strong>Energy Preservation:</strong> When signal energies approach the limits of
     *       double precision representation, small relative errors are acceptable to maintain
     *       overall transform validity</li>
     *   <li><strong>Boundary Effects:</strong> Near numerical boundaries (overflow/underflow regions),
     *       algorithms may use approximations that introduce controlled error</li>
     *   <li><strong>Filter Length Impact:</strong> Longer wavelet filters (like DB4) may accumulate
     *       slightly more numerical error than shorter filters (like Haar)</li>
     * </ul>
     * 
     * <p>This 1% threshold strikes a balance between mathematical rigor and practical
     * numerical limitations, ensuring tests remain meaningful while not failing due to
     * unavoidable precision constraints.</p>
     */
    public static final double EXTREME_VALUE_RELATIVE_TOLERANCE = 0.01;

    /**
     * Boundary effect tolerance for edge-sensitive operations.
     * 
     * <p><strong>20% Boundary Effect Tolerance (0.2)</strong></p>
     * 
     * <p>This relaxed tolerance accommodates numerical artifacts that occur near signal
     * boundaries in wavelet transforms. Boundary effects are inherent in discrete wavelet
     * transforms due to the finite nature of digital signals and the non-periodic assumption
     * at signal edges.</p>
     */
    public static final double BOUNDARY_EFFECT_TOLERANCE = 0.2;

    /**
     * Peak sharpness tolerance for signal characteristic preservation.
     * 
     * <p><strong>1% Peak Sharpness Tolerance (0.01)</strong></p>
     * 
     * <p>This tolerance ensures that sharp signal features (like peaks in ECG signals)
     * are preserved during wavelet decomposition and reconstruction. The 1% tolerance
     * allows for minor smoothing effects while ensuring that medically or scientifically
     * significant peak characteristics remain intact.</p>
     */
    public static final double PEAK_SHARPNESS_TOLERANCE = 0.01;

    /**
     * FFT parity tolerance for comparing scalar vs FFT paths.
     * 
     * <p>This tolerance (1e-11) is used in parity tests that verify numerical
     * equivalence between scalar convolution and FFT-based convolution paths.
     * It is slightly stricter than the default tolerance to catch subtle
     * discrepancies while remaining robust to floating-point roundoff.</p>
     */
    public static final double FFT_PARITY_TOLERANCE = 1e-11;

    

    /**
     * Provides detailed explanation for a given tolerance value.
     * This method can be used in error messages or logging to provide context.
     * 
     * @param tolerance the tolerance value to explain
     * @return detailed explanation of the tolerance purpose and rationale
     */
    public static String explainTolerance(double tolerance) {
        if (tolerance == DEFAULT_TOLERANCE) {
            return "Standard floating-point comparison tolerance for wavelet operations";
        } else if (tolerance == ENERGY_TOLERANCE) {
            return "Strict tolerance for energy preservation verification in orthogonal transforms";
        } else if (tolerance == ORTHOGONALITY_TOLERANCE) {
            return "Very strict tolerance for mathematical property verification";
        } else if (tolerance == EXTREME_VALUE_RELATIVE_TOLERANCE) {
            return "1% relative error tolerance for extreme value operations";
        } else if (tolerance == BOUNDARY_EFFECT_TOLERANCE) {
            return "20% tolerance for boundary effect accommodations";
        } else if (tolerance == PEAK_SHARPNESS_TOLERANCE) {
            return "1% tolerance for peak sharpness preservation in signal processing";
        } else if (tolerance == FFT_PARITY_TOLERANCE) {
            return "Strict tolerance for scalar vs FFT parity comparisons";
        } else {
            return "Custom tolerance value: " + tolerance;
        }
    }
}
