package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ComplexContinuousWavelet;

/**
 * Complex Gaussian wavelet (CGAU).
 * 
 * The complex Gaussian wavelet is the analytic signal version of the 
 * Gaussian derivative, providing both magnitude and phase information.
 * 
 * Mathematical definition:
 * ψ(t) = C_n * t^n * exp(-t²/2) * exp(i*ω₀*t)
 * 
 * where:
 * - n is the derivative order (typically 1-8)
 * - ω₀ is the modulation frequency
 * - C_n is the normalization constant
 * 
 * Properties:
 * - Complex-valued (analytic signal)
 * - Excellent time-frequency localization
 * - No negative frequency components
 * - Smooth in both time and frequency domains
 * 
 * Applications:
 * - Phase analysis in signal processing
 * - Edge detection with directional information
 * - Instantaneous frequency estimation
 * - Radar and sonar signal processing
 */
public final class ComplexGaussianWavelet implements ComplexContinuousWavelet {
    
    private final int n;           // Derivative order
    private final double sigma;    // Scale parameter
    private final double omega0;   // Modulation frequency
    private final double normFactor;
    
    /**
     * Create complex Gaussian wavelet with default parameters.
     * Default: n=1 (first derivative), sigma=1, omega0=5
     */
    public ComplexGaussianWavelet() {
        this(1, 1.0, 5.0);
    }
    
    /**
     * Create complex Gaussian wavelet with specified order.
     * 
     * @param n derivative order (1-8)
     */
    public ComplexGaussianWavelet(int n) {
        this(n, 1.0, 5.0);
    }
    
    /**
     * Create complex Gaussian wavelet with full parameters.
     * 
     * @param n derivative order (1-8)
     * @param sigma scale parameter (> 0)
     * @param omega0 modulation frequency
     */
    public ComplexGaussianWavelet(int n, double sigma, double omega0) {
        if (n < 1 || n > 8) {
            throw new IllegalArgumentException(
                "Derivative order must be between 1 and 8");
        }
        if (sigma <= 0) {
            throw new IllegalArgumentException(
                "Sigma must be positive");
        }
        
        this.n = n;
        this.sigma = sigma;
        this.omega0 = omega0;
        
        // Compute normalization factor
        this.normFactor = computeNormalization();
    }
    
    private double computeNormalization() {
        // Normalization to ensure unit energy
        // C_n = 1 / (σ^(n+1/2) * sqrt(2^n * n!))
        double factorial = 1;
        for (int i = 2; i <= n; i++) {
            factorial *= i;
        }
        
        return 1.0 / (Math.pow(sigma, n + 0.5) * 
                      Math.sqrt(Math.pow(2, n) * factorial * Math.sqrt(Math.PI)));
    }
    
    @Override
    public String name() {
        return String.format("cgau%d", n);
    }
    
    @Override
    public String description() {
        return String.format("Complex Gaussian wavelet (n=%d, σ=%.1f, ω₀=%.1f)", 
                            n, sigma, omega0);
    }
    
    @Override
    public double psi(double t) {
        // Real part of complex Gaussian wavelet
        double x = t / sigma;
        double gaussian = Math.exp(-x * x / 2);
        double polynomial = hermitePolynomial(n, x);
        double modulation = Math.cos(omega0 * t);
        
        return normFactor * polynomial * gaussian * modulation;
    }
    
    @Override
    public double psiImaginary(double t) {
        // Imaginary part of complex Gaussian wavelet
        double x = t / sigma;
        double gaussian = Math.exp(-x * x / 2);
        double polynomial = hermitePolynomial(n, x);
        double modulation = Math.sin(omega0 * t);
        
        return normFactor * polynomial * gaussian * modulation;
    }
    
    @Override
    public ComplexNumber psiComplex(double t) {
        double x = t / sigma;
        double gaussian = Math.exp(-x * x / 2);
        double polynomial = hermitePolynomial(n, x);
        
        // Complex exponential modulation
        double real = normFactor * polynomial * gaussian * Math.cos(omega0 * t);
        double imag = normFactor * polynomial * gaussian * Math.sin(omega0 * t);
        
        return new ComplexNumber(real, imag);
    }
    
    /**
     * Compute Hermite polynomial H_n(x) using recursion.
     * H_0(x) = 1
     * H_1(x) = 2x
     * H_n(x) = 2x*H_{n-1}(x) - 2(n-1)*H_{n-2}(x)
     */
    private double hermitePolynomial(int n, double x) {
        if (n == 0) return 1;
        if (n == 1) return 2 * x;
        
        double h0 = 1;
        double h1 = 2 * x;
        
        for (int k = 2; k <= n; k++) {
            double h2 = 2 * x * h1 - 2 * (k - 1) * h0;
            h0 = h1;
            h1 = h2;
        }
        
        return h1;
    }
    
    @Override
    public double centerFrequency() {
        // Effective center frequency considering modulation
        return omega0 / (2 * Math.PI);
    }
    
    @Override
    public double bandwidth() {
        // Bandwidth increases with derivative order
        return Math.sqrt(n + 0.5) / (sigma * Math.sqrt(2 * Math.PI));
    }
    
    /**
     * Complex Gaussian in frequency domain.
     * Ψ̂(ω) = C_n * (i*(ω-ω₀))^n * exp(-(ω-ω₀)²σ²/2)
     *
     * @param omega the frequency
     * @return the complex value of the wavelet in the frequency domain
     */
    public ComplexNumber psiHat(double omega) {
        double shift = omega - omega0;
        double gaussian = Math.exp(-shift * shift * sigma * sigma / 2);
        
        // For n=0, this would be just the Gaussian
        // For n=1,2,3... we need the derivative term
        
        // (i*(ω-ω₀))^n computation
        double magnitude;
        double phase;
        
        if (Math.abs(shift) < 1e-15) {
            // At center frequency, higher derivatives are 0
            magnitude = 0;
            phase = 0;
        } else {
            magnitude = normFactor * Math.pow(Math.abs(shift), n) * gaussian;
            phase = n * Math.PI / 2 * Math.signum(shift);
        }
        
        return ComplexNumber.fromPolar(magnitude, phase);
    }
    
    @Override
    public double[] discretize(int length) {
        if (length % 2 != 0) {
            throw new IllegalArgumentException("Length must be even for complex wavelet");
        }
        
        double[] samples = new double[length];
        
        // Effective support based on Gaussian envelope
        double tMin = -4 * sigma;
        double tMax = 4 * sigma;
        double dt = (tMax - tMin) / (length/2 - 1);
        
        // Store as [real, imag, real, imag, ...]
        for (int i = 0; i < length/2; i++) {
            double t = tMin + i * dt;
            ComplexNumber c = psiComplex(t);
            samples[2*i] = c.real();
            samples[2*i + 1] = c.imag();
        }
        
        return samples;
    }
    
    /**
     * Get the derivative order.
     *
     * @return the derivative order
     */
    public int getOrder() {
        return n;
    }
    
    /**
     * Get the modulation frequency.
     *
     * @return the modulation frequency
     */
    public double getModulationFrequency() {
        return omega0;
    }
}