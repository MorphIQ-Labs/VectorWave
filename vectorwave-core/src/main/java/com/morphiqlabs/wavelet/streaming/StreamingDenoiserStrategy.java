package com.morphiqlabs.wavelet.streaming;

import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingTransform;
import java.util.concurrent.Flow;

/**
 * Strategy interface for streaming wavelet denoisers.
 * 
 * <p>This interface defines the contract for different streaming denoiser
 * implementations, allowing for flexible selection between fast (real-time)
 * and quality (high SNR) implementations.</p>
 * 
 * <p>All implementations are based on MODWT for better streaming performance
 * and shift-invariance.</p>
 */
public interface StreamingDenoiserStrategy extends Flow.Publisher<double[]>, AutoCloseable {
    
    /**
     * Closes this streaming denoiser and releases any resources.
     * May block waiting for background threads to complete.
     */
    @Override
    void close();
    
    /**
     * Processes a block of input samples.
     * 
     * @param samples input samples to denoise
     */
    void process(double[] samples);
    
    /**
     * Gets the performance profile of this implementation.
     * 
     * @return performance characteristics
     */
    PerformanceProfile getPerformanceProfile();
    
    /**
     * Gets the current statistics.
     * 
     * @return streaming statistics
     */
    MODWTStreamingTransform.StreamingStatistics getStatistics();
    
    /**
     * Flushes any buffered data and completes processing.
     */
    void flush();
    
    /**
     * Performance characteristics of a streaming denoiser.
     *
     * @param expectedLatencyMicros expected latency per block in microseconds
     * @param expectedSNRImprovement expected SNR improvement in dB
     * @param memoryUsageBytes approximate memory usage in bytes
     * @param isRealTimeCapable whether this profile targets real-time usage
     */
    record PerformanceProfile(
        double expectedLatencyMicros,
        double expectedSNRImprovement,
        long memoryUsageBytes,
        boolean isRealTimeCapable
    ) {
        /**
         * Creates a profile for fast real-time processing.
         */
        public static PerformanceProfile fastProfile(int blockSize) {
            return new PerformanceProfile(
                0.1 * blockSize,  // ~0.1 µs per sample
                6.0,              // ~6 dB SNR improvement
                (long) blockSize * 8 * 4,  // Approx memory usage
                true              // Real-time capable
            );
        }
        
        /**
         * Creates a profile for quality processing.
         */
        public static PerformanceProfile qualityProfile(int blockSize, double overlap) {
            double processingFactor = 1.0 + overlap;
            return new PerformanceProfile(
                0.3 * blockSize * processingFactor,  // Higher latency
                9.0,              // ~9 dB SNR improvement
                (long) (blockSize * 8 * 6 * processingFactor),
                blockSize <= 512  // Real-time for smaller blocks
            );
        }
    }
}
