package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.cwt.ComplexNumber;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Interface for complex-valued continuous wavelets.
 * 
 * <p>Complex wavelets provide both magnitude and phase information,
 * making them particularly useful for analyzing oscillatory signals
 * and extracting instantaneous frequency information.</p>
 */
public interface ComplexContinuousWavelet extends ContinuousWavelet {
    
    /**
     * Evaluates the complex wavelet function at a given point.
     * 
     * @param t the time/position parameter
     * @return complex value of the wavelet
     */
    default ComplexNumber psiComplex(double t) {
        return new ComplexNumber(psi(t), psiImaginary(t));
    }
    
    /**
     * Evaluates the imaginary part of the wavelet function at a given point.
     *
     * @param t the time/position parameter
     * @return the imaginary part of the wavelet function value at t
     */
    double psiImaginary(double t);
    
    /**
     * Evaluates the scaled and translated imaginary part of the wavelet function.
     *
     * @param t           the time/position parameter
     * @param scale       the scale parameter (a > 0)
     * @param translation the translation parameter
     * @return the imaginary part of the wavelet function value
     */
    default double psiImaginary(double t, double scale, double translation) {
        if (scale <= 0) {
            throw new InvalidArgumentException("Scale must be positive");
        }
        return (1.0 / Math.sqrt(scale)) * psiImaginary((t - translation) / scale);
    }
    
    @Override
    default boolean isComplex() {
        return true;
    }
}