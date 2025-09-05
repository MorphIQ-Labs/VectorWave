package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Interface for continuous wavelets used in Continuous Wavelet Transform (CWT).
 *
 * <p>Continuous wavelets are defined by mathematical functions rather than
 * discrete filter coefficients. They are used for time-frequency analysis
 * and can provide better frequency resolution than discrete wavelets.</p>
 *
 * <p>Examples include: Morlet, Mexican Hat (Ricker), Gaussian derivatives</p>
 * 
 * @since 1.0.0
 */
public non-sealed interface ContinuousWavelet extends Wavelet {

    /**
     * Evaluates the wavelet function at a given point.
     *
     * @param t the time/position parameter
     * @return the wavelet function value at t
     */
    double psi(double t);

    /**
     * Evaluates the scaled and translated wavelet function.
     *
     * @param t           the time/position parameter
     * @param scale       the scale parameter (a > 0)
     * @param translation the translation parameter
     * @return the wavelet function value
     */
    default double psi(double t, double scale, double translation) {
        if (scale <= 0) {
            throw new InvalidArgumentException("Scale must be positive");
        }
        return (1.0 / Math.sqrt(scale)) * psi((t - translation) / scale);
    }

    /**
     * Returns the center frequency of the wavelet in Hz.
     * This is useful for relating scales to frequencies.
     *
     * @return the center frequency
     */
    double centerFrequency();

    /**
     * Returns the bandwidth parameter for the wavelet.
     * Higher values give better frequency resolution.
     *
     * @return the bandwidth parameter
     */
    double bandwidth();

    /**
     * Indicates if this wavelet is complex-valued.
     * Complex wavelets can capture phase information.
     *
     * @return true if the wavelet is complex-valued
     */
    boolean isComplex();

    /**
     * For continuous wavelets, we need to discretize for DWT.
     * This method provides discretized filter coefficients.
     *
     * @param numCoeffs the number of coefficients to generate
     * @return discretized filter coefficients
     */
    double[] discretize(int numCoeffs);

    @Override
    default WaveletType getType() {
        return WaveletType.CONTINUOUS;
    }

    // Default implementations that discretize the continuous wavelet
    @Override
    default double[] lowPassDecomposition() {
        return discretize(8); // Default discretization
    }

    @Override
    default double[] highPassDecomposition() {
        // Generate high-pass from low-pass using quadrature mirror filter
        double[] h = lowPassDecomposition();
        double[] g = new double[h.length];
        for (int i = 0; i < h.length; i++) {
            g[i] = (i % 2 == 0 ? 1 : -1) * h[h.length - 1 - i];
        }
        return g;
    }

    @Override
    default double[] lowPassReconstruction() {
        return lowPassDecomposition();
    }

    @Override
    default double[] highPassReconstruction() {
        return highPassDecomposition();
    }
}
