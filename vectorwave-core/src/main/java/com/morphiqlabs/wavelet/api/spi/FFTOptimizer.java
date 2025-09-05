package com.morphiqlabs.wavelet.api.spi;

/**
 * Service Provider Interface for FFT optimizations.
 * Implementations can provide optimized FFT using Vector API or other techniques.
 *
 * @since 1.0.0
 */
public interface FFTOptimizer {
    
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
     * Compute forward FFT.
     * @param real real part of input
     * @param imag imaginary part of input
     * @return FFT result with real and imaginary parts
     */
    FFTResult fft(double[] real, double[] imag);
    
    /**
     * Compute inverse FFT.
     * @param real real part of input
     * @param imag imaginary part of input
     * @return inverse FFT result
     */
    FFTResult ifft(double[] real, double[] imag);
    
    /**
     * Compute real-valued FFT (more efficient for real signals).
     * @param signal real input signal
     * @return FFT result
     */
    FFTResult rfft(double[] signal);
    
    /**
     * Compute inverse of real-valued FFT.
     * @param real real part of frequency domain
     * @param imag imaginary part of frequency domain
     * @return reconstructed real signal
     */
    double[] irfft(double[] real, double[] imag);
    
    /**
     * Result container for FFT operations.
     */
    interface FFTResult {
        /**
         * Gets the real part of the FFT output.
         * @return array of real components
         */
        double[] getReal();

        /**
         * Gets the imaginary part of the FFT output.
         * @return array of imaginary components
         */
        double[] getImag();
    }
}
