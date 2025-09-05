package com.morphiqlabs.wavelet.api.spi;

import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.BoundaryMode;

/**
 * Service Provider Interface for MODWT transform optimizations.
 * Implementations can provide optimized versions using Vector API, GPU, or other techniques.
 *
 * @since 1.0.0
 */
public interface MODWTOptimizer {
    
    /**
     * Check if this optimizer is supported on the current platform.
     * @return true if the optimizer can be used
     */
    boolean isSupported();
    
    /**
     * Get the priority of this optimizer. Higher values have higher priority.
     * @return priority value (0-100)
     */
    int getPriority();
    
    /**
     * Get a descriptive name for this optimizer.
     * @return optimizer name
     */
    String getName();
    
    /**
     * Perform optimized forward MODWT transform.
     * @param signal input signal
     * @param wavelet wavelet to use
     * @param boundaryMode boundary handling mode
     * @return transform result with wavelet and scaling coefficients
     */
    MODWTOptimizedResult forward(double[] signal, Wavelet wavelet, BoundaryMode boundaryMode);
    
    /**
     * Perform optimized inverse MODWT transform.
     * @param waveletCoeffs wavelet coefficients
     * @param scalingCoeffs scaling coefficients  
     * @param wavelet wavelet to use
     * @param boundaryMode boundary handling mode
     * @return reconstructed signal
     */
    double[] inverse(double[] waveletCoeffs, double[] scalingCoeffs, 
                    Wavelet wavelet, BoundaryMode boundaryMode);
    
    /**
     * Perform batch forward transforms.
     * @param signals array of signals
     * @param wavelet wavelet to use
     * @param boundaryMode boundary handling mode
     * @return array of transform results
     */
    default MODWTOptimizedResult[] forwardBatch(double[][] signals, Wavelet wavelet, 
                                                BoundaryMode boundaryMode) {
        MODWTOptimizedResult[] results = new MODWTOptimizedResult[signals.length];
        for (int i = 0; i < signals.length; i++) {
            results[i] = forward(signals[i], wavelet, boundaryMode);
        }
        return results;
    }
    
    /**
     * Result interface for optimized MODWT transforms.
     */
    interface MODWTOptimizedResult {
        /**
         * Gets the wavelet coefficients.
         * @return the wavelet coefficients
         */
        double[] getWaveletCoefficients();

        /**
         * Gets the scaling coefficients.
         * @return the scaling coefficients
         */
        double[] getScalingCoefficients();
    }
}
