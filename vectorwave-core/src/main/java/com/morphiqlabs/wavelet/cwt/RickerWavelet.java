package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;

/**
 * Ricker wavelet (normalized second derivative of Gaussian).
 * 
 * <p>The Ricker wavelet, also known as the "Mexican Hat" wavelet with specific
 * normalization, is the negative normalized second derivative of a Gaussian function.
 * It's widely used in seismic analysis and geophysics for detecting reflection events
 * and layer boundaries.</p>
 * 
 * <p>Mathematical form: ψ(t) = (1 - t²/σ²) * exp(-t²/(2σ²)) / (σ√π)</p>
 * 
 * <p>The Ricker wavelet has:</p>
 * <ul>
 *   <li>Zero mean (admissibility condition)</li>
 *   <li>Single positive peak at t=0</li>
 *   <li>Two negative side lobes</li>
 *   <li>Excellent time localization</li>
 * </ul>
 */
public final class RickerWavelet implements ContinuousWavelet {
    
    private final double sigma; // Width parameter
    private final String name;
    private static final double SQRT_PI = Math.sqrt(Math.PI);
    
    /**
     * Creates a Ricker wavelet with specified width parameter.
     * 
     * @param sigma Width parameter (must be positive), controls the wavelet scale
     * @throws IllegalArgumentException if sigma is not positive
     */
    public RickerWavelet(double sigma) {
        if (sigma <= 0) {
            throw new IllegalArgumentException("σ must be positive");
        }
        this.sigma = sigma;
        this.name = String.format("ricker%.2f", sigma);
    }
    
    /**
     * Creates a Ricker wavelet with default width.
     * Default: σ=1.0
     */
    public RickerWavelet() {
        this(1.0);
    }
    
    @Override
    public String name() {
        return name;
    }
    
    @Override
    public double psi(double t) {
        double tNorm = t / sigma;
        double tNorm2 = tNorm * tNorm;
        
        // Ricker: (1 - t²/σ²) * exp(-t²/(2σ²)) / (σ√π)
        return (1 - tNorm2) * Math.exp(-tNorm2 / 2) / (sigma * SQRT_PI);
    }
    
    @Override
    public double centerFrequency() {
        // Peak frequency in Hz for σ in seconds
        // The Ricker wavelet has its peak frequency at f = 1/(2πσ√2)
        return 1.0 / (2 * Math.PI * sigma * Math.sqrt(2));
    }
    
    @Override
    public double bandwidth() {
        // Bandwidth is inversely proportional to sigma
        return 1.0 / sigma;
    }
    
    @Override
    public boolean isComplex() {
        return false;
    }
    
    @Override
    public double[] discretize(int numCoeffs) {
        double[] coeffs = new double[numCoeffs];
        
        // Support region: approximately ±4σ captures 99.99% of the energy
        double tMax = 4.0 * sigma;
        double dt = 2 * tMax / (numCoeffs - 1);
        
        for (int i = 0; i < numCoeffs; i++) {
            double t = -tMax + i * dt;
            coeffs[i] = psi(t);
        }
        
        // Normalize to unit energy
        double sum = 0;
        for (double c : coeffs) {
            sum += c * c;
        }
        if (sum > 0) {
            double norm = 1.0 / Math.sqrt(sum);
            for (int i = 0; i < numCoeffs; i++) {
                coeffs[i] *= norm;
            }
        }
        
        return coeffs;
    }
    
    
    /**
     * Gets the width parameter sigma.
     * 
     * @return the width parameter
     */
    public double getSigma() {
        return sigma;
    }
    
    /**
     * Computes the Fourier transform of the Ricker wavelet.
     * Useful for frequency domain analysis.
     * 
     * @param omega frequency parameter
     * @return Fourier transform value at frequency omega
     */
    public double fourierTransform(double omega) {
        // Fourier transform of Ricker wavelet:
        // ψ̂(ω) = -√(2/π) * σ³ * ω² * exp(-σ²ω²/2)
        double sigma2 = sigma * sigma;
        double omega2 = omega * omega;
        return -Math.sqrt(2.0 / Math.PI) * sigma * sigma2 * omega2 * 
               Math.exp(-sigma2 * omega2 / 2);
    }
    
    /**
     * Computes the scale corresponding to a given frequency for the Ricker wavelet.
     * Useful for relating CWT scales to physical frequencies.
     * 
     * @param frequency target frequency in Hz
     * @param samplingRate sampling rate in Hz
     * @return corresponding scale parameter
     */
    public static double frequencyToScale(double frequency, double samplingRate) {
        // For Ricker wavelet: scale = centerFreq * samplingRate / frequency
        // where centerFreq = 1/(2π√2) for unit sigma
        double centerFreq = 1.0 / (2 * Math.PI * Math.sqrt(2));
        return centerFreq * samplingRate / frequency;
    }
}