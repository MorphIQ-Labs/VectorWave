package com.morphiqlabs.wavelet.cwt.finance;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Derivative of Gaussian (DOG) wavelet - optimal for volatility detection in financial data.
 * 
 * <p>The DOG wavelet is the n-th derivative of the Gaussian function. It's particularly
 * effective for detecting changes in volatility and identifying regime shifts in financial
 * markets. Applications include:</p>
 * <ul>
 *   <li>Volatility clustering detection</li>
 *   <li>Market regime identification</li>
 *   <li>Risk assessment and VaR calculations</li>
 *   <li>Detecting structural breaks in time series</li>
 * </ul>
 * 
 * <p>Mathematical definition:</p>
 * <pre>
 * ψ(t) = (-1)^(n+1) / √(2^n * √π * n!) * H_n(t) * exp(-t²/2)
 * </pre>
 * 
 * where H_n(t) is the n-th Hermite polynomial.
 * 
 * <p>Special case: DOG(2) is the Mexican Hat wavelet, widely used in finance.</p>
 * 
 * <h2>Standard Mathematical Form</h2>
 * <p>This implementation uses the standard mathematical parameterization with σ=1.
 * For MATLAB compatibility (σ=5/√8), use {@link MATLABMexicanHat} instead.</p>
 * 
 * <h2>Financial Applications</h2>
 * <p>The DOG(2) Mexican Hat wavelet is particularly effective for:</p>
 * <ul>
 *   <li>Detecting volatility regime changes</li>
 *   <li>Identifying market turning points</li>
 *   <li>Analyzing persistence in volatility clusters</li>
 * </ul>
 * 
 * @see MATLABMexicanHat for MATLAB-compatible Mexican Hat implementation
 * @see <a href="../../../../../../../docs/FINANCIAL_WAVELETS.md">Financial Wavelets Guide</a>
 */
public final class DOGWavelet implements ContinuousWavelet {
    
    private final int n; // Derivative order
    private final double normFactor;
    private final String name;
    
    /**
     * Creates a DOG wavelet with default order n=2 (Mexican Hat).
     * This is optimal for most volatility detection tasks.
     */
    public DOGWavelet() {
        this(2);
    }
    
    /**
     * Creates a DOG wavelet with specified derivative order.
     * 
     * @param n derivative order (must be positive, typically 1-10)
     * @throws IllegalArgumentException if n {@literal <} 1 or n {@literal >} 10
     */
    public DOGWavelet(int n) {
        if (n < 1) {
            throw new IllegalArgumentException("DOG wavelet order must be positive, got: " + n);
        }
        if (n > 10) {
            throw new IllegalArgumentException("DOG wavelet order too large (max 10), got: " + n);
        }
        
        this.n = n;
        this.name = "dog" + n;
        this.normFactor = calculateNormalizationFactor(n);
    }
    
    @Override
    public String name() {
        return name;
    }
    
    @Override
    public double psi(double t) {
        // Compute t² once and reuse
        double t_squared = t * t;
        
        // Special case for n=2 (Mexican Hat)
        if (n == 2) {
            // Standard mathematical Mexican Hat: ψ(t) = (2/(√3 * π^(1/4))) * (1 - t²) * exp(-t²/2)
            // This is the canonical form used in most academic literature
            return normFactor * (1 - t_squared) * Math.exp(-t_squared / 2);
        }
        
        // General DOG wavelet: normalized n-th derivative of Gaussian
        double gaussian = Math.exp(-t_squared / 2);
        double hermite = hermitePolynomial(n, t);
        
        // Apply sign correction: (-1)^(n+1)
        double sign = ((n + 1) % 2 == 0) ? 1.0 : -1.0;
        
        return sign * normFactor * hermite * gaussian;
    }
    
    @Override
    public double centerFrequency() {
        // Center frequency increases with derivative order
        // Approximate formula based on spectral peak
        return Math.sqrt(n) / (2 * Math.PI);
    }
    
    @Override
    public double bandwidth() {
        // Bandwidth parameter for DOG wavelet
        // Higher derivatives have narrower bandwidth
        return 1.0 / Math.sqrt(n);
    }
    
    @Override
    public boolean isComplex() {
        return false; // DOG is real-valued
    }
    
    @Override
    public double[] discretize(int length) {
        if (length <= 0) {
            throw new InvalidArgumentException("Length must be positive");
        }
        
        double[] samples = new double[length];
        int center = length / 2;
        
        // Support is approximately [-4√n, 4√n]
        double support = 4.0 * Math.sqrt(n);
        
        for (int i = 0; i < length; i++) {
            double t = (i - center) * 2.0 * support / length;
            samples[i] = psi(t);
        }
        
        return samples;
    }
    
    /**
     * Gets the derivative order.
     * 
     * @return derivative order n
     */
    public int getDerivativeOrder() {
        return n;
    }
    
    /**
     * Calculates the normalization factor for the DOG wavelet.
     */
    private static double calculateNormalizationFactor(int n) {
        // Special case for n=2 (Mexican Hat)
        if (n == 2) {
            // Standard mathematical normalization: 2/(√3 * π^(1/4))
            return 2.0 / (Math.sqrt(3.0) * Math.pow(Math.PI, 0.25));
        }
        
        // General case: norm = 1 / √(2^n * √π * n!)
        double pow2n = Math.pow(2, n);
        double nFactorial = factorial(n);
        double sqrtPi = Math.sqrt(Math.PI);
        
        return 1.0 / Math.sqrt(pow2n * sqrtPi * nFactorial);
    }
    
    /**
     * Computes the n-th Hermite polynomial using recurrence relation.
     */
    private static double hermitePolynomial(int n, double x) {
        if (n == 0) return 1.0;
        if (n == 1) return 2.0 * x;
        
        // Use recurrence: H_{n+1}(x) = 2x*H_n(x) - 2n*H_{n-1}(x)
        double h0 = 1.0;
        double h1 = 2.0 * x;
        
        for (int k = 2; k <= n; k++) {
            double h2 = 2.0 * x * h1 - 2.0 * (k - 1) * h0;
            h0 = h1;
            h1 = h2;
        }
        
        return h1;
    }
    
    /**
     * Computes factorial for small n.
     */
    private static double factorial(int n) {
        if (n <= 1) return 1.0;
        
        double result = 1.0;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}
