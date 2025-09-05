package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ComplexContinuousWavelet;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * The Morlet wavelet (also known as Gabor wavelet) is a complex-valued
 * continuous wavelet commonly used for time-frequency analysis.
 *
 * <p>The Morlet wavelet is defined as a complex exponential (carrier)
 * multiplied by a Gaussian window (envelope). It provides excellent
 * time-frequency localization.</p>
 *
 * <p>Properties:
 * <ul>
 *   <li>Complex-valued (can extract phase information)</li>
 *   <li>No compact support (but effectively localized)</li>
 *   <li>Smooth</li>
 *   <li>Good frequency resolution</li>
 * </ul>
 * 
 * <p><strong>Canonical References:</strong></p>
 * <ul>
 *   <li>Morlet, J., Arens, G., Fourgeau, E., {@literal &} Glard, D. (1982). "Wave propagation 
     *       and sampling theory." Geophysics, 47(2), 203-221.</li>
 *   <li>Mallat, S. (2008). "A Wavelet Tour of Signal Processing: The Sparse Way" 
 *       (3rd ed., pp. 88-91). Academic Press.</li>
 *   <li>Addison, P. S. (2002). "The Illustrated Wavelet Transform Handbook" 
 *       (pp. 50-58). IOP Publishing.</li>
 * </ul>
 */
public final class MorletWavelet implements ComplexContinuousWavelet {

    private final double omega0; // Central frequency parameter (typically 5-6)
    private final double sigma;  // Bandwidth parameter
    private final double normalizationFactor; // Pre-computed normalization constant
    private final double correctionTerm; // Pre-computed correction term

    /**
     * Creates a Morlet wavelet with specified parameters.
     *
     * @param omega0 central frequency (typically 5-6)
     * @param sigma  bandwidth parameter (typically 1.0)
     */
    public MorletWavelet(double omega0, double sigma) {
        this.omega0 = omega0;
        this.sigma = sigma;
        this.normalizationFactor = 1.0 / Math.pow(Math.PI * sigma * sigma, 0.25);
        this.correctionTerm = Math.exp(-0.5 * omega0 * omega0 * sigma * sigma);
    }

    /**
     * Creates a standard Morlet wavelet with omega0=6, sigma=1.
     */
    public MorletWavelet() {
        this(6.0, 1.0);
    }

    @Override
    public String name() {
        return "morl";
    }

    @Override
    public String description() {
        return String.format("Morlet wavelet (ω₀=%.1f, σ=%.1f)", omega0, sigma);
    }

    @Override
    public double psi(double t) {
        // Real part of Morlet wavelet
        double gaussianEnvelope = Math.exp(-0.5 * t * t / (sigma * sigma));
        double carrier = Math.cos(omega0 * t);

        return normalizationFactor * gaussianEnvelope * (carrier - correctionTerm);
    }

    /**
     * Returns the imaginary part of the Morlet wavelet.
     * This is useful for phase analysis.
     *
     * @param t the time parameter
     * @return the imaginary part of psi(t)
     */
    @Override
    public double psiImaginary(double t) {
        double gaussianEnvelope = Math.exp(-0.5 * t * t / (sigma * sigma));
        double carrier = Math.sin(omega0 * t);

        return normalizationFactor * gaussianEnvelope * carrier;
    }

    @Override
    public double centerFrequency() {
        return omega0 / (2 * Math.PI);
    }

    @Override
    public double bandwidth() {
        return sigma;
    }

    // isComplex() is already implemented by ComplexContinuousWavelet interface

    @Override
    public double[] discretize(int numCoeffs) {
        if (numCoeffs % 2 != 0) {
            throw new InvalidArgumentException("Number of coefficients must be even");
        }

        double[] coeffs = new double[numCoeffs];
        double t0 = -4.0 * sigma; // Start at -4 standard deviations
        double dt = 8.0 * sigma / (numCoeffs - 1);

        for (int i = 0; i < numCoeffs; i++) {
            double t = t0 + i * dt;
            coeffs[i] = psi(t);
        }

        // Use the shared helper method for normalization
        return Wavelet.normalizeToUnitL2Norm(coeffs);
    }
}
