package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ComplexContinuousWavelet;

/**
 * Generalized Morse wavelets with time-frequency concentration control.
 * 
 * <p>The Morse wavelets are a family of analytic wavelets parameterized by:
 * <ul>
 *   <li>β (beta): Time-bandwidth product (β > 0) - controls wavelet duration</li>
 *   <li>γ (gamma): Symmetry parameter (γ > 0) - controls wavelet symmetry</li>
 * </ul>
 * 
 * <p>Special cases:
 * <ul>
 *   <li>β = 1, γ = 3: Airy wavelet</li>
 *   <li>β → ∞, γ fixed: Approximate Gaussian</li>
 *   <li>β = 3, γ = 60: Standard Morse wavelet with good time-frequency properties</li>
 * </ul>
 * 
 * <p>Morse wavelets are particularly useful for adaptive time-frequency analysis
 * where different parameter values can be chosen to optimize the analysis for
 * specific signal characteristics.</p>
 */
public final class MorseWavelet implements ComplexContinuousWavelet {
    
    private static final double GAMMA_STIRLING_THRESHOLD = 10.0; // Threshold for using Stirling's approximation
    
    private final double beta;  // Time-bandwidth product
    private final double gamma; // Symmetry parameter
    private final double normalizationFactor;
    private final String name;
    
    /**
     * Creates a Morse wavelet with specified parameters.
     * 
     * @param beta Time-bandwidth product (must be positive)
     * @param gamma Symmetry parameter (must be positive)
     * @throws IllegalArgumentException if beta or gamma are not positive
     */
    public MorseWavelet(double beta, double gamma) {
        if (beta <= 0 || gamma <= 0) {
            throw new IllegalArgumentException(
                "β and γ must be positive");
        }
        this.beta = beta;
        this.gamma = gamma;
        this.name = String.format("morse%.1f-%.1f", beta, gamma);
        
        // Compute normalization factor
        this.normalizationFactor = computeNormalization(beta, gamma);
    }
    
    /**
     * Creates a standard Morse wavelet with default parameters.
     * Default: β=3, γ=60 (good time-frequency properties)
     */
    public MorseWavelet() {
        this(3.0, 60.0);
    }
    
    @Override
    public String name() {
        return name;
    }
    
    @Override
    public double psi(double t) {
        // Time domain Morse wavelet (real part)
        // Requires inverse Fourier transform - using approximation
        return computeTimeValue(t).real();
    }
    
    @Override
    public double psiImaginary(double t) {
        // Time domain Morse wavelet (imaginary part)
        return computeTimeValue(t).imag();
    }
    
    @Override
    public ComplexNumber psiComplex(double t) {
        return computeTimeValue(t);
    }
    
    /**
     * Morse wavelet in frequency domain (easier to define).
     * ψ̂(ω) = U(ω) * normalization * ω^β * exp(-ω^γ)
     * where U(ω) is the unit step function
     * 
     * @param omega frequency parameter (must be non-negative)
     * @return complex value in frequency domain
     */
    public ComplexNumber psiHat(double omega) {
        if (omega <= 0) {
            return new ComplexNumber(0, 0);
        }
        
        // Morse wavelet: ψ̂(ω) = U(ω) ω^β exp(-ω^γ)
        double magnitude = normalizationFactor * 
            Math.pow(omega, beta) * 
            Math.exp(-Math.pow(omega, gamma));
        
        return new ComplexNumber(magnitude, 0);
    }
    
    @Override
    public double centerFrequency() {
        // Peak frequency of the Morse wavelet
        return Math.pow(beta / gamma, 1.0 / gamma);
    }
    
    @Override
    public double bandwidth() {
        // Bandwidth related to time-frequency product
        return Math.sqrt(beta * gamma);
    }
    
    @Override
    public double[] discretize(int numCoeffs) {
        // Discretize the real part
        double[] coeffs = new double[numCoeffs];
        double tMax = 10.0 / centerFrequency(); // Support region
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
     * Computes the normalization factor for the Morse wavelet.
     */
    private double computeNormalization(double beta, double gamma) {
        // Normalization: sqrt(2 * pi * gamma * (2^(beta/gamma)) / Gamma(beta/gamma))
        // Use Lanczos approximation for all values for better accuracy
        double ratio = beta / gamma;
        double factor = Math.sqrt(2 * Math.PI * gamma) * Math.pow(2, ratio);
        factor /= gammaApprox(ratio);
        return factor;
    }
    
    /**
     * Approximation of Gamma function for small positive values.
     */
    private double gammaApprox(double x) {
        // Using Lanczos approximation coefficients
        double[] coef = {
            0.99999999999980993,
            676.5203681218851,
            -1259.1392167224028,
            771.32342877765313,
            -176.61502916214059,
            12.507343278686905,
            -0.13857109526572012,
            9.9843695780195716e-6,
            1.5056327351493116e-7
        };
        
        double tmp = x + 7.5;
        tmp = (x + 0.5) * Math.log(tmp) - tmp;
        double sum = coef[0];
        for (int i = 1; i < 9; i++) {
            sum += coef[i] / (x + i);
        }
        
        return Math.exp(tmp + Math.log(Math.sqrt(2 * Math.PI) * sum));
    }
    
    /**
     * Computes Morse wavelet value in time domain using approximation.
     */
    private ComplexNumber computeTimeValue(double t) {
        // Approximation based on the analytic properties of Morse wavelets
        // They are analytic (no negative frequencies) and concentrated around center frequency
        
        double omega0 = centerFrequency();
        double sigma = 1.0 / bandwidth();
        
        // Envelope function
        double envelope = Math.exp(-t * t / (2 * sigma * sigma));
        
        // Modulation by center frequency (complex exponential)
        double realPart = envelope * Math.cos(2 * Math.PI * omega0 * t);
        double imagPart = envelope * Math.sin(2 * Math.PI * omega0 * t);
        
        // Additional shaping based on beta and gamma
        double shapeFactor = Math.pow(Math.abs(t) + 1, -beta / gamma);
        
        return new ComplexNumber(realPart * shapeFactor, imagPart * shapeFactor);
    }
    
    /**
     * Gets the time-frequency product (Heisenberg uncertainty).
     * 
     * @return the time-frequency product
     */
    public double getTimeFrequencyProduct() {
        return Math.sqrt(beta * gamma);
    }
    
    /**
     * Gets the beta parameter.
     * 
     * @return the time-bandwidth product parameter
     */
    public double getBeta() {
        return beta;
    }
    
    /**
     * Gets the gamma parameter.
     * 
     * @return the symmetry parameter
     */
    public double getGamma() {
        return gamma;
    }
}
