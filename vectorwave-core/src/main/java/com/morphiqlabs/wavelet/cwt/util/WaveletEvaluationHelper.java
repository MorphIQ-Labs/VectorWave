package com.morphiqlabs.wavelet.cwt.util;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.api.ComplexContinuousWavelet;

/**
 * Utility class demonstrating the use of the unified wavelet evaluation helper.
 * This shows how other parts of the codebase can leverage the helper method
 * to avoid duplicating instanceof checks for complex wavelets.
 */
public class WaveletEvaluationHelper {
    
    /**
     * Computes the magnitude of a wavelet at multiple points.
     * Works with both real and complex wavelets.
     * 
     * @param wavelet the wavelet to evaluate
     * @param points array of points to evaluate at
     * @return array of magnitudes
     */
    public static double[] computeMagnitudes(ContinuousWavelet wavelet, double[] points) {
        double[] magnitudes = new double[points.length];
        
        for (int i = 0; i < points.length; i++) {
            // Evaluate wavelet directly, handling complex wavelets
            if (wavelet instanceof ComplexContinuousWavelet complexWavelet) {
                double real = complexWavelet.psi(points[i]);
                double imag = complexWavelet.psiImaginary(points[i]);
                magnitudes[i] = Math.sqrt(real * real + imag * imag);
            } else {
                magnitudes[i] = Math.abs(wavelet.psi(points[i]));
            }
        }
        
        return magnitudes;
    }
    
    /**
     * Computes the phase of a wavelet at multiple points.
     * Returns 0 for real wavelets, actual phase for complex wavelets.
     * 
     * @param wavelet the wavelet to evaluate
     * @param points array of points to evaluate at
     * @return array of phases
     */
    public static double[] computePhases(ContinuousWavelet wavelet, double[] points) {
        double[] phases = new double[points.length];
        
        for (int i = 0; i < points.length; i++) {
            // Evaluate phase directly, handling complex wavelets
            if (wavelet instanceof ComplexContinuousWavelet complexWavelet) {
                double real = complexWavelet.psi(points[i]);
                double imag = complexWavelet.psiImaginary(points[i]);
                phases[i] = Math.atan2(imag, real);
            } else {
                // Real wavelets have phase 0 or π depending on sign
                double value = wavelet.psi(points[i]);
                phases[i] = value >= 0 ? 0.0 : Math.PI;
            }
        }
        
        return phases;
    }
    
    /**
     * Example of checking if a wavelet is complex without instanceof.
     * 
     * @param wavelet the wavelet to check
     * @param t point to evaluate at
     * @return true if the wavelet has non-zero imaginary part at t
     */
    public static boolean hasImaginaryComponent(ContinuousWavelet wavelet, double t) {
        if (wavelet instanceof ComplexContinuousWavelet complexWavelet) {
            double imag = complexWavelet.psiImaginary(t);
            return Math.abs(imag) > 1e-10;
        } else {
            return false; // Real wavelets have no imaginary component
        }
    }
}