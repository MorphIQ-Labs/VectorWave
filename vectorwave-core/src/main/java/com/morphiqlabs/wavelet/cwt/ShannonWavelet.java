package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Classical Shannon wavelet (sinc function).
 * 
 * The Shannon wavelet is the sinc function, which has perfect 
 * band-pass characteristics in the frequency domain (rectangular window).
 * 
 * ψ(t) = sinc(t) * cos(3πt/2)
 * 
 * Properties:
 * - Perfect frequency localization (rectangular in frequency)
 * - Poor time localization (infinite support)
 * - Gibbs phenomenon at discontinuities
 */
public final class ShannonWavelet implements ContinuousWavelet {
    
    private final double fb; // Bandwidth parameter
    private final double fc; // Center frequency
    private final String name;
    
    /**
     * Create Shannon wavelet with default parameters.
     * Default: fb=1, fc=1.5
     */
    public ShannonWavelet() {
        this(1.0, 1.5);
    }
    
    /**
     * Create Shannon wavelet with specified parameters.
     *
     * @param fb bandwidth parameter (> 0)
     * @param fc center frequency (> 0)
     */
    public ShannonWavelet(double fb, double fc) {
        if (fb <= 0) {
            throw new IllegalArgumentException(
                "Bandwidth must be positive, got: " + fb);
        }
        if (fc <= 0) {
            throw new IllegalArgumentException(
                "Center frequency must be positive, got: " + fc);
        }
        this.fb = fb;
        this.fc = fc;
        this.name = String.format("shan%.1f-%.1f", fb, fc);
    }
    
    @Override
    public String name() {
        return name;
    }
    
    @Override
    public String description() {
        return String.format("Shannon wavelet (fb=%.1f, fc=%.1f)", fb, fc);
    }
    
    @Override
    public double psi(double t) {
        // Shannon wavelet: sinc(fb*t) * cos(2π*fc*t)
        double sinc = (Math.abs(fb * t) < 1e-10) ? 1.0 : 
                      Math.sin(Math.PI * fb * t) / (Math.PI * fb * t);
        return Math.sqrt(fb) * sinc * Math.cos(2 * Math.PI * fc * t);
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
    public boolean isComplex() {
        return false;
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
    
    /**
     * Gets the bandwidth parameter.
     *
     * @return the bandwidth parameter
     */
    public double getBandwidthParameter() {
        return fb;
    }
    
    /**
     * Gets the center frequency parameter.
     *
     * @return the center frequency parameter
     */
    public double getCenterFrequencyParameter() {
        return fc;
    }
}