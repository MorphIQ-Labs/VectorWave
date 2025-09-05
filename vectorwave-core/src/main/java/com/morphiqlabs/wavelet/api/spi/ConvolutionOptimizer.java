package com.morphiqlabs.wavelet.api.spi;

/**
 * Service Provider Interface for convolution optimizations.
 * Implementations can provide optimized convolution using Vector API or other techniques.
 *
 * @since 1.0.0
 */
public interface ConvolutionOptimizer {
    
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
     * Perform 1D convolution.
     * @param signal input signal
     * @param kernel convolution kernel
     * @return convolved signal
     */
    double[] convolve1D(double[] signal, double[] kernel);
    
    /**
     * Perform circular convolution.
     * @param signal input signal
     * @param kernel convolution kernel
     * @return circular convolved signal
     */
    double[] circularConvolve(double[] signal, double[] kernel);
    
    /**
     * Perform decimating convolution (downsampling by factor of 2).
     * @param signal input signal
     * @param kernel convolution kernel
     * @return decimated convolved signal
     */
    double[] decimatingConvolve(double[] signal, double[] kernel);
    
    /**
     * Perform upsampling convolution (upsampling by factor of 2).
     * @param signal input signal
     * @param kernel convolution kernel
     * @return upsampled convolved signal
     */
    double[] upsamplingConvolve(double[] signal, double[] kernel);
}
