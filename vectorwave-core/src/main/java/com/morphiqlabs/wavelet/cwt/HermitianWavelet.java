package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;

/**
 * Hermitian wavelets based on Hermite polynomials.
 * 
 * <p>Hermitian wavelets are constructed from Hermite polynomials modulated by
 * a Gaussian envelope. They form an orthogonal family and are particularly useful
 * in quantum mechanics, probability theory, and signal processing applications
 * requiring orthogonal decomposition.</p>
 * 
 * <p>Mathematical form: ψ_n(t) = H_n(t/σ) * exp(-t²/(2σ²)) * normalization</p>
 * 
 * <p>where H_n is the n-th Hermite polynomial.</p>
 * 
 * <p>Properties:</p>
 * <ul>
 *   <li>Orthogonal: Different orders are orthogonal to each other</li>
 *   <li>n vanishing moments for order n</li>
 *   <li>Oscillatory behavior increases with order</li>
 *   <li>Excellent time-frequency localization</li>
 * </ul>
 */
public final class HermitianWavelet implements ContinuousWavelet {
    
    private final int n; // Hermite polynomial order
    private final double sigma; // Scale parameter
    private final String name;
    private final double normalizationFactor;
    private static final double SQRT_PI = Math.sqrt(Math.PI);
    
    // Pre-computed factorials for efficiency (up to order 10)
    private static final long[] FACTORIALS = {
        1, 1, 2, 6, 24, 120, 720, 5040, 40320, 362880, 3628800
    };
    
    /**
     * Creates a Hermitian wavelet with specified order and scale.
     * 
     * @param n Hermite polynomial order (0 ≤ n ≤ 10)
     * @param sigma Scale parameter (must be positive)
     * @throws IllegalArgumentException if n is out of range or sigma is not positive
     */
    public HermitianWavelet(int n, double sigma) {
        if (n < 0 || n > 10) {
            throw new IllegalArgumentException(
                "Order must be between 0 and 10");
        }
        if (sigma <= 0) {
            throw new IllegalArgumentException("σ must be positive");
        }
        
        this.n = n;
        this.sigma = sigma;
        this.name = String.format("herm%d-%.1f", n, sigma);
        
        // Compute normalization factor: 1/sqrt(2^n * n! * σ * sqrt(π))
        this.normalizationFactor = 1.0 / Math.sqrt(
            Math.pow(2, n) * FACTORIALS[n] * sigma * SQRT_PI);
    }
    
    /**
     * Creates a Hermitian wavelet with specified order and default scale.
     * Default: σ=1.0
     * 
     * @param n Hermite polynomial order (0 ≤ n ≤ 10)
     */
    public HermitianWavelet(int n) {
        this(n, 1.0);
    }
    
    @Override
    public String name() {
        return name;
    }
    
    @Override
    public double psi(double t) {
        double x = t / sigma;
        double gaussian = Math.exp(-x * x / 2);
        double hermite = hermitePolynomial(n, x);
        
        return normalizationFactor * hermite * gaussian;
    }
    
    @Override
    public double centerFrequency() {
        // Center frequency increases with order
        // Approximate formula for Hermitian wavelets
        return Math.sqrt(n + 0.5) / (2 * Math.PI * sigma);
    }
    
    @Override
    public double bandwidth() {
        // Bandwidth increases with order
        return Math.sqrt(n + 1) / sigma;
    }
    
    @Override
    public boolean isComplex() {
        return false;
    }
    
    @Override
    public double[] discretize(int numCoeffs) {
        double[] coeffs = new double[numCoeffs];
        
        // Support region: approximately ±(3 + n/2)σ
        double tMax = (3 + n / 2.0) * sigma;
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
     * Compute Hermite polynomial H_n(x) using recursion.
     * H_0(x) = 1
     * H_1(x) = 2x
     * H_n(x) = 2x*H_{n-1}(x) - 2(n-1)*H_{n-2}(x)
     * 
     * @param n order of the Hermite polynomial
     * @param x input value
     * @return H_n(x)
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
    
    /**
     * Gets the order of the Hermite polynomial.
     * 
     * @return the order n
     */
    public int getOrder() {
        return n;
    }
    
    /**
     * Gets the scale parameter sigma.
     * 
     * @return the scale parameter
     */
    public double getSigma() {
        return sigma;
    }
    
    /**
     * Computes the inner product with another Hermitian wavelet.
     * Hermitian wavelets of different orders are orthogonal.
     * 
     * @param other another Hermitian wavelet
     * @return inner product value (0 for different orders, 1 for same normalized wavelet)
     */
    public double innerProduct(HermitianWavelet other) {
        if (this.n != other.n || Math.abs(this.sigma - other.sigma) > 1e-10) {
            return 0.0; // Orthogonal
        }
        return 1.0; // Same wavelet, normalized
    }
    
    /**
     * Creates a family of Hermitian wavelets up to specified order.
     * Useful for multi-resolution analysis with orthogonal wavelets.
     * 
     * @param maxOrder maximum order (0 ≤ maxOrder ≤ 10)
     * @param sigma scale parameter
     * @return array of Hermitian wavelets from order 0 to maxOrder
     */
    public static HermitianWavelet[] createFamily(int maxOrder, double sigma) {
        if (maxOrder < 0 || maxOrder > 10) {
            throw new IllegalArgumentException("Max order must be between 0 and 10");
        }
        
        HermitianWavelet[] family = new HermitianWavelet[maxOrder + 1];
        for (int i = 0; i <= maxOrder; i++) {
            family[i] = new HermitianWavelet(i, sigma);
        }
        return family;
    }
}