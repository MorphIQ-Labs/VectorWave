package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Gaussian Derivative wavelets - derivatives of the Gaussian function.
 * 
 * <p>These wavelets are particularly useful for:</p>
 * <ul>
 *   <li>Edge detection (1st derivative)</li>
 *   <li>Ridge/valley detection (2nd derivative)</li>
 *   <li>Feature extraction at multiple scales</li>
 *   <li>Singularity detection and characterization</li>
 *   <li>Image processing and computer vision</li>
 * </ul>
 * 
 * <p>Mathematical definition:</p>
 * <pre>
 * ψ_n(t) = C_n * H_n(t/σ) * exp(-t²/(2σ²))
 * </pre>
 * 
 * where:
 * <ul>
 *   <li>n is the derivative order</li>
 *   <li>σ is the scale parameter (standard deviation)</li>
 *   <li>H_n is related to the n-th derivative operator</li>
 *   <li>C_n is the normalization constant</li>
 * </ul>
 * 
 * <p>Special cases:</p>
 * <ul>
 *   <li>n=1: First derivative, optimal for edge detection</li>
 *   <li>n=2: Second derivative (similar to Mexican Hat), for ridge detection</li>
 *   <li>n=3: Third derivative, for detecting inflection points</li>
 *   <li>n=4: Fourth derivative, for higher-order feature detection</li>
 * </ul>
 */
public final class GaussianDerivativeWavelet implements ContinuousWavelet {
    
    private final int n; // Derivative order
    private final double sigma; // Scale parameter
    private final String name;
    private final double normFactor;
    
    // Pre-computed coefficients for performance
    private final double sigmaPower; // sigma^n for the denominator
    private final boolean isOdd; // Whether the derivative order is odd
    
    /**
     * Creates a first-order Gaussian derivative wavelet with σ=1.
     */
    public GaussianDerivativeWavelet() {
        this(1, 1.0);
    }
    
    /**
     * Creates a Gaussian derivative wavelet with specified order and σ=1.
     * 
     * @param n derivative order (must be between 1 and 8)
     * @throws IllegalArgumentException if n {@literal <} 1 or n {@literal >} 8
     */
    public GaussianDerivativeWavelet(int n) {
        this(n, 1.0);
    }
    
    /**
     * Creates a Gaussian derivative wavelet with specified order and scale.
     * 
     * @param n derivative order (must be between 1 and 8)
     * @param sigma scale parameter (must be positive)
     * @throws IllegalArgumentException if n {@literal <} 1, n {@literal >} 8, or sigma {@literal <=} 0
     */
    public GaussianDerivativeWavelet(int n, double sigma) {
        if (n < 1) {
            throw new IllegalArgumentException("Derivative order must be positive, got: " + n);
        }
        if (n > 8) {
            throw new IllegalArgumentException("Derivative order too large (max 8), got: " + n);
        }
        if (sigma <= 0) {
            throw new IllegalArgumentException("Scale parameter sigma must be positive, got: " + sigma);
        }
        
        this.n = n;
        this.sigma = sigma;
        this.name = "gaus" + n;
        this.normFactor = calculateNormalizationFactor(n, sigma);
        this.sigmaPower = Math.pow(sigma, n);
        this.isOdd = (n % 2) == 1;
    }
    
    @Override
    public String name() {
        return name;
    }
    
    @Override
    public String description() {
        return String.format("Gaussian derivative wavelet of order %d (σ=%.1f) - %s",
            n, sigma, switch (n) {
                case 1 -> "Edge detection";
                case 2 -> "Ridge/valley detection (Mexican Hat)";
                case 3 -> "Inflection point detection";
                case 4 -> "Higher-order feature detection";
                default -> "Order " + n + " feature detection";
            });
    }
    
    @Override
    public double psi(double t) {
        // Scaled variable
        double x = t / sigma;
        double x2 = x * x;
        
        // Gaussian envelope
        double gaussian = Math.exp(-0.5 * x2) * normFactor;
        
        // Compute polynomial coefficient for the derivative
        double polynomial = computeHermitePolynomial(x, x2);
        
        // Apply normalization with pre-computed sigma power
        return polynomial / sigmaPower * gaussian;
    }
    
    /**
     * Computes the Hermite polynomial component for the given derivative order.
     * This method is optimized to avoid repeated switch statements.
     */
    private double computeHermitePolynomial(double x, double x2) {
        // For odd orders, factor out x for efficiency
        if (isOdd) {
            return x * computeOddHermite(x2);
        } else {
            return computeEvenHermite(x2);
        }
    }
    
    /**
     * Computes odd Hermite polynomials (after factoring out x).
     */
    private double computeOddHermite(double x2) {
        return switch (n) {
            case 1 -> -1.0;
            case 3 -> 3.0 - x2;
            case 5 -> {
                double x4 = x2 * x2;
                yield x4 - 10.0 * x2 + 15.0;
            }
            case 7 -> {
                double x4 = x2 * x2;
                double x6 = x4 * x2;
                yield x6 - 21.0 * x4 + 105.0 * x2 - 105.0;
            }
            default -> throw new IllegalStateException("Invalid odd order: " + n);
        };
    }
    
    /**
     * Computes even Hermite polynomials.
     */
    private double computeEvenHermite(double x2) {
        return switch (n) {
            case 2 -> x2 - 1.0;
            case 4 -> {
                double x4 = x2 * x2;
                yield x4 - 6.0 * x2 + 3.0;
            }
            case 6 -> {
                double x4 = x2 * x2;
                double x6 = x4 * x2;
                yield x6 - 15.0 * x4 + 45.0 * x2 - 15.0;
            }
            case 8 -> {
                double x4 = x2 * x2;
                double x6 = x4 * x2;
                double x8 = x4 * x4;
                yield x8 - 28.0 * x6 + 210.0 * x4 - 420.0 * x2 + 105.0;
            }
            default -> throw new IllegalStateException("Invalid even order: " + n);
        };
    }
    
    @Override
    public double centerFrequency() {
        // Center frequency increases with derivative order and decreases with sigma
        // Approximation based on spectral peak location
        return Math.sqrt(n) / (2 * Math.PI * sigma);
    }
    
    @Override
    public double bandwidth() {
        // Bandwidth increases with derivative order
        // Higher derivatives have more oscillations and thus broader bandwidth
        return Math.sqrt(n) / (sigma * Math.sqrt(2.0));
    }
    
    @Override
    public boolean isComplex() {
        return false; // Gaussian derivatives are real-valued
    }
    
    @Override
    public double[] discretize(int length) {
        if (length <= 0) {
            throw new InvalidArgumentException("Length must be positive");
        }
        
        double[] samples = new double[length];
        int center = length / 2;
        
        // Support is approximately [-4σ√n, 4σ√n]
        double support = 4.0 * sigma * Math.sqrt(n);
        
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
     * Gets the scale parameter.
     * 
     * @return scale parameter sigma
     */
    public double getSigma() {
        return sigma;
    }
    
    /**
     * Calculates the normalization factor for the Gaussian derivative.
     * This ensures the wavelet has unit energy (L2 norm = 1).
     */
    private static double calculateNormalizationFactor(int n, double sigma) {
        // Basic Gaussian normalization
        double gaussNorm = 1.0 / Math.sqrt(2 * Math.PI * sigma * sigma);
        
        // Additional normalization factors for each derivative order
        // These ensure unit L2 norm for the complete wavelet
        double derivativeNorm = switch (n) {
            case 1 -> Math.sqrt(2.0);  // sqrt(2) for first derivative
            case 2 -> Math.sqrt(2.0 / 3.0);  // sqrt(2/3) for second derivative
            case 3 -> Math.sqrt(2.0 / 15.0);
            case 4 -> Math.sqrt(2.0 / 105.0);
            case 5 -> Math.sqrt(2.0 / 945.0);
            case 6 -> Math.sqrt(2.0 / 10395.0);
            case 7 -> Math.sqrt(2.0 / 135135.0);
            case 8 -> Math.sqrt(2.0 / 2027025.0);
            default -> 1.0;
        };
        
        return gaussNorm * derivativeNorm;
    }
}
