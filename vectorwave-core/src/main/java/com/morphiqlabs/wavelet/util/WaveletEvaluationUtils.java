package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.math.Complex;

/**
 * Utility class for evaluating continuous wavelets and working with complex-valued wavelets.
 * 
 * <p>This class provides static methods for evaluating continuous wavelets,
 * including complex-valued wavelets that have both real and imaginary components.</p>
 */
public final class WaveletEvaluationUtils {

    private WaveletEvaluationUtils() {
        // Utility class, prevent instantiation
    }

    /**
     * Evaluates a continuous wavelet at a given point and returns the result as a complex number.
     * 
     * <p>This method uses pattern matching with instanceof to handle different wavelet types:</p>
     * <ul>
     *   <li>For complex wavelets like Morlet: returns both real and imaginary parts</li>
     *   <li>For real-valued wavelets: returns the real part with zero imaginary part</li>
     * </ul>
     *
     * @param wavelet the continuous wavelet to evaluate (must not be null)
     * @param t the time/position parameter
     * @return the complex-valued wavelet evaluation at point t
     * @throws NullPointerException if wavelet is null
     */
    public static Complex evaluateAsComplex(ContinuousWavelet wavelet, double t) {
        if (wavelet == null) {
            throw new NullPointerException("The wavelet parameter cannot be null.");
        }

        // Use pattern matching with instanceof to handle different wavelet types
        if (wavelet instanceof MorletWavelet morlet) {
            // Morlet wavelet is complex-valued, so we need both real and imaginary parts
            double realPart = morlet.psi(t);
            double imaginaryPart = morlet.psiImaginary(t);
            return new Complex(realPart, imaginaryPart);
        } else {
            // For other continuous wavelets that are real-valued
            double realPart = wavelet.psi(t);
            return Complex.real(realPart);
        }
    }
    
    /**
     * Evaluates a continuous wavelet at a given point with scale and translation,
     * returning the result as a complex number.
     * 
     * @param wavelet the continuous wavelet to evaluate (must not be null)
     * @param t the time/position parameter
     * @param scale the scale parameter (a > 0)
     * @param translation the translation parameter
     * @return the complex-valued wavelet evaluation
     * @throws NullPointerException if wavelet is null
     * @throws IllegalArgumentException if scale {@literal <=} 0
     */
    public static Complex evaluateAsComplex(ContinuousWavelet wavelet, double t, double scale, double translation) {
        if (wavelet == null) {
            throw new NullPointerException("The wavelet parameter cannot be null.");
        }
        
        if (scale <= 0) {
            throw new IllegalArgumentException("Scale must be positive");
        }

        // Transform the time parameter according to scale and translation
        double transformedT = (t - translation) / scale;
        double scaleFactor = 1.0 / Math.sqrt(scale);
        
        // Use pattern matching with instanceof to handle different wavelet types
        if (wavelet instanceof MorletWavelet morlet) {
            // Morlet wavelet is complex-valued
            double realPart = scaleFactor * morlet.psi(transformedT);
            double imaginaryPart = scaleFactor * morlet.psiImaginary(transformedT);
            return new Complex(realPart, imaginaryPart);
        } else {
            // For other continuous wavelets that are real-valued
            double realPart = scaleFactor * wavelet.psi(transformedT);
            return Complex.real(realPart);
        }
    }
}
