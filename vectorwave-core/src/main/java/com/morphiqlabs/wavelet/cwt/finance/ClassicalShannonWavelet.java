package com.morphiqlabs.wavelet.cwt.finance;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Classical Shannon wavelet - the standard formulation.
 * 
 * <p>This implements the classical Shannon wavelet as defined in wavelet literature:
 * ψ(t) = 2*sinc(2t) - sinc(t)
 * where sinc(x) = sin(πx)/(πx)</p>
 * 
 * <p>This wavelet has perfect frequency localization with support in [π/2, π]
 * in the frequency domain.</p>
 * 
 * <p>For financial applications:</p>
 * <ul>
 *   <li>Use when you need perfect frequency separation (e.g., isolating trading cycles)</li>
 *   <li>Best for analyzing stationary periodic patterns</li>
 *   <li>May produce ringing artifacts (Gibbs phenomenon) around sharp transitions</li>
 *   <li>Not recommended for transient event detection</li>
 * </ul>
 * 
 * <p>Compare with Shannon-Gabor wavelet which provides better time localization
 * at the cost of some frequency resolution.</p>
 * 
 * @see ShannonGaborWavelet
 */
public final class ClassicalShannonWavelet implements ContinuousWavelet {
    
    private static final String NAME = "shan";
    
    @Override
    public String name() {
        return NAME;
    }
    
    @Override
    public double psi(double t) {
        // Shannon wavelet: ψ(t) = 2*sinc(2t) - sinc(t)
        return 2.0 * sinc(2.0 * t) - sinc(t);
    }
    
    @Override
    public double centerFrequency() {
        // Center frequency is at 3π/4 in angular frequency
        // In normalized frequency: 3/8 = 0.375
        return 0.75 * Math.PI / (2.0 * Math.PI);
    }
    
    @Override
    public double bandwidth() {
        // Bandwidth is π/2 in angular frequency
        // In normalized frequency: 1/4 = 0.25
        return 0.5 * Math.PI / (2.0 * Math.PI);
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
        
        // Shannon wavelet has slow decay, use large support
        double support = 20.0;
        
        for (int i = 0; i < length; i++) {
            double t = (i - center) * 2.0 * support / length;
            samples[i] = psi(t);
        }
        
        return samples;
    }
    
    /**
     * Sinc function: sin(πx)/(πx)
     */
    private double sinc(double x) {
        if (Math.abs(x) < 1e-10) {
            return 1.0;
        }
        double px = Math.PI * x;
        return Math.sin(px) / px;
    }
}