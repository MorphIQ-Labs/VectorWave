package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ComplexContinuousWavelet;

/**
 * Complex Shannon wavelet (CSHAN).
 * 
 * <p>The complex Shannon wavelet is the analytic signal version of the Shannon wavelet,
 * providing perfect frequency localization. It's characterized by a sinc function
 * modulated by a complex exponential, making it ideal for frequency-selective analysis.</p>
 * 
 * <p>Mathematical form: ψ(t) = (fb)^(-1/2) * sinc(fb*t) * exp(2πi*fc*t)</p>
 * 
 * <p>Parameters:
 * <ul>
 *   <li>fb: Bandwidth parameter (controls frequency resolution)</li>
 *   <li>fc: Center frequency (determines the wavelet's central frequency)</li>
 * </ul>
 */
public final class ComplexShannonWavelet implements ComplexContinuousWavelet {
    
    private final double fb; // Bandwidth parameter
    private final double fc; // Center frequency
    private final String name;
    
    /**
     * Creates a Complex Shannon wavelet with specified parameters.
     * 
     * @param fb Bandwidth parameter (must be positive)
     * @param fc Center frequency (must be positive)
     * @throws IllegalArgumentException if fb or fc are not positive
     */
    public ComplexShannonWavelet(double fb, double fc) {
        if (fb <= 0 || fc <= 0) {
            throw new IllegalArgumentException(
                "Bandwidth and center frequency must be positive");
        }
        this.fb = fb;
        this.fc = fc;
        this.name = String.format("cshan%.1f-%.1f", fb, fc);
    }
    
    /**
     * Creates a Complex Shannon wavelet with default parameters.
     * Default: fb=1.0, fc=1.0
     */
    public ComplexShannonWavelet() {
        this(1.0, 1.0);
    }
    
    @Override
    public String name() {
        return name;
    }
    
    @Override
    public double psi(double t) {
        // Real part: Shannon wavelet
        double sinc = computeSinc(fb * t);
        return sinc * Math.cos(2 * Math.PI * fc * t) / Math.sqrt(fb);
    }
    
    @Override
    public double psiImaginary(double t) {
        // Imaginary part for analytic signal
        double sinc = computeSinc(fb * t);
        return sinc * Math.sin(2 * Math.PI * fc * t) / Math.sqrt(fb);
    }
    
    
    @Override
    public double centerFrequency() {
        return fc;
    }
    
    @Override
    public double bandwidth() {
        return fb;
    }
    
    @Override
    public double[] discretize(int numCoeffs) {
        double[] coeffs = new double[numCoeffs];
        double tMax = 5.0 / fb; // Support region
        double dt = 2 * tMax / (numCoeffs - 1);
        
        for (int i = 0; i < numCoeffs; i++) {
            double t = -tMax + i * dt;
            coeffs[i] = psi(t);
        }
        
        // Normalize
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
     * Computes the sinc function: sin(πx)/(πx)
     */
    private double computeSinc(double x) {
        if (Math.abs(x) < 1e-10) {
            return 1.0;
        }
        double piX = Math.PI * x;
        return Math.sin(piX) / piX;
    }
}
