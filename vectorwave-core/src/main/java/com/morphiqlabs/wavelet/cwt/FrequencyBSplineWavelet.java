package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ComplexContinuousWavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Frequency B-Spline wavelet (FBSP).
 * 
 * The FBSP wavelet is defined by B-splines in the frequency domain,
 * providing smooth frequency characteristics with compact support.
 * 
 * ψ̂(ω) = √fb * [sinc(fb*ω/(2m))]^m * exp(i*fc*ω)
 * 
 * where m is the B-spline order.
 * 
 * For the time domain, we use a closed-form approximation based on 
 * the mathematical properties of B-splines and frequency modulation.
 * 
 * Properties:
 * - Smooth frequency response (B-spline shaped)
 * - Better time-frequency trade-off than Shannon
 * - Complex-valued for phase analysis
 * - Order m controls smoothness vs localization
 */
public final class FrequencyBSplineWavelet implements ComplexContinuousWavelet {
    
    private final int m;      // B-spline order
    private final double fb;  // Bandwidth parameter  
    private final double fc;  // Center frequency
    private final String name;
    
    /**
     * Create FBSP wavelet with default parameters.
     * Default: m=2 (quadratic), fb=1, fc=1
     */
    public FrequencyBSplineWavelet() {
        this(2, 1.0, 1.0);
    }
    
    /**
     * Create FBSP wavelet with specified parameters.
     *
     * @param m B-spline order (1-5)
     * @param fb bandwidth parameter (> 0)
     * @param fc center frequency (> 0)
     */
    public FrequencyBSplineWavelet(int m, double fb, double fc) {
        if (m < 1 || m > 5) {
            throw new IllegalArgumentException(
                "B-spline order must be between 1 and 5, got: " + m);
        }
        if (fb <= 0) {
            throw new IllegalArgumentException(
                "Bandwidth must be positive, got: " + fb);
        }
        if (fc <= 0) {
            throw new IllegalArgumentException(
                "Center frequency must be positive, got: " + fc);
        }
        this.m = m;
        this.fb = fb;
        this.fc = fc;
        this.name = String.format("fbsp%d-%.1f-%.1f", m, fb, fc);
    }
    
    @Override
    public String name() {
        return name;
    }
    
    @Override
    public String description() {
        return String.format("Frequency B-Spline wavelet (m=%d, fb=%.1f, fc=%.1f)", 
                            m, fb, fc);
    }
    
    @Override
    public double psi(double t) {
        // Real part - using mathematical approximation based on B-spline properties
        return computeTimeDomainReal(t);
    }
    
    @Override
    public double psiImaginary(double t) {
        // Imaginary part - using mathematical approximation
        return computeTimeDomainImaginary(t);
    }
    
    /**
     * FBSP in frequency domain.
     * Returns complex value as [real, imaginary] array.
     *
     * @param omega angular frequency (rad/s)
     * @return array of length 2: {@code [real, imaginary]}
     */
    public double[] psiHat(double omega) {
        // B-spline in frequency: [sinc(fb*ω/(2m))]^m
        double arg = fb * omega / (2 * m);
        double sinc = (Math.abs(arg) < 1e-10) ? 1.0 : 
                      Math.sin(Math.PI * arg) / (Math.PI * arg);
        double magnitude = Math.sqrt(fb) * Math.pow(Math.abs(sinc), m);
        
        // Phase modulation: exp(i*fc*ω)
        double phase = fc * omega;
        
        return new double[]{
            magnitude * Math.cos(phase),
            magnitude * Math.sin(phase)
        };
    }
    
    /**
     * Compute time domain real part using mathematical approximation.
     * 
     * For FBSP, we use the fact that the time domain can be approximated as:
     * ψ(t) ≈ √fb * B_m(fb*t) * cos(2π*fc*t - π/4)
     * 
     * where B_m is a normalized B-spline function of order m.
     */
    private double computeTimeDomainReal(double t) {
        // B-spline approximation in time domain
        double scaledT = fb * t;
        double bspline = computeBSpline(scaledT, m);
        
        // Frequency modulation with phase shift
        double phase = 2 * Math.PI * fc * t - Math.PI / 4;
        
        return Math.sqrt(fb) * bspline * Math.cos(phase);
    }
    
    /**
     * Compute time domain imaginary part using mathematical approximation.
     */
    private double computeTimeDomainImaginary(double t) {
        // B-spline approximation in time domain
        double scaledT = fb * t;
        double bspline = computeBSpline(scaledT, m);
        
        // Frequency modulation with phase shift
        double phase = 2 * Math.PI * fc * t - Math.PI / 4;
        
        return Math.sqrt(fb) * bspline * Math.sin(phase);
    }
    
    /**
     * Compute normalized B-spline function of order m.
     * This provides a good approximation to the time domain behavior.
     */
    private double computeBSpline(double t, int order) {
        // B-spline of order m has support in [-m/2, m/2]
        double support = order / 2.0;
        if (Math.abs(t) > support) {
            return 0.0;
        }
        
        // Approximate B-spline using recursive formula
        // For efficiency, we use closed-form approximations for common orders
        switch (order) {
            case 1:
                // Box function
                return Math.abs(t) < 0.5 ? 1.0 : 0.0;
            
            case 2:
                // Hat function (linear B-spline)
                double absT = Math.abs(t);
                return absT < 1.0 ? (1.0 - absT) : 0.0;
            
            case 3:
                // Quadratic B-spline
                absT = Math.abs(t);
                if (absT <= 0.5) {
                    return 0.75 - absT * absT;
                } else if (absT < 1.5) {
                    double temp = 1.5 - absT;
                    return 0.5 * temp * temp;
                } else {
                    return 0.0;
                }
            
            default:
                // General approximation using Gaussian-like function for higher orders
                // This approximates higher-order B-splines and has infinite support
                double sigma = order / 4.0; // Empirical scaling
                return Math.exp(-t * t / (2 * sigma * sigma)) / Math.sqrt(2 * Math.PI * sigma * sigma);
        }
    }
    
    @Override
    public double centerFrequency() {
        return fc;
    }
    
    @Override
    public double bandwidth() {
        return fb;
    }
    
    /**
     * Gets the B-spline order.
     * @return order m
     */
    public int getOrder() {
        return m;
    }
    
    @Override
    public double[] discretize(int length) {
        if (length <= 0) {
            throw new InvalidArgumentException("Length must be positive");
        }
        
        double[] samples = new double[length];
        int center = length / 2;
        
        // Effective support: [-8, 8] for good approximation
        double support = 8.0;
        
        for (int i = 0; i < length; i++) {
            double t = (i - center) * 2.0 * support / length;
            samples[i] = psi(t);
        }
        
        return samples;
    }
}
