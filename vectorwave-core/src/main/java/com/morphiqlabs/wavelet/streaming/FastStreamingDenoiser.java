package com.morphiqlabs.wavelet.streaming;

import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingDenoiser;
import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingTransform;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * Fast implementation of streaming denoiser optimized for real-time processing.
 * 
 * <p>This implementation prioritizes low latency over denoising quality,
 * making it suitable for:</p>
 * <ul>
 *   <li>Live audio processing</li>
 *   <li>Real-time sensor data filtering</li>
 *   <li>Low-latency financial data processing</li>
 * </ul>
 */
final class FastStreamingDenoiser implements StreamingDenoiserStrategy {
    
    private final MODWTStreamingDenoiser denoiser;
    private final StreamingDenoiserConfig config;
    private final SubmissionPublisher<double[]> publisher;
    private final PerformanceProfile profile;
    
    /**
     * Calculates the optimal noise window size for fast streaming processing.
     * 
     * <p>For fast processing, we limit the noise window to at most half the block size
     * to minimize latency while maintaining reasonable noise estimation quality.
     * The window size is clamped between 1 and the configured maximum.</p>
     * 
     * @param config the streaming denoiser configuration
     * @return the optimal noise window size in samples
     */
    private static int calculateOptimalNoiseWindowSize(StreamingDenoiserConfig config) {
        int maxAllowedSize = (int) Math.round(config.getBlockSize() / 2.0);
        return Math.max(1, Math.min(config.getNoiseWindowSize(), maxAllowedSize));
    }
    
    FastStreamingDenoiser(StreamingDenoiserConfig config) {
        this.config = config;
        this.publisher = new SubmissionPublisher<>();
        
        // Create MODWT denoiser with fast settings
        this.denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(config.getWavelet())
            .bufferSize(config.getBlockSize())
            .thresholdMethod(config.getThresholdMethod())
            .thresholdType(config.getThresholdType())
            .thresholdMultiplier(config.getThresholdMultiplier())
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
            .noiseWindowSize(calculateOptimalNoiseWindowSize(config))
            .build();
        
        // Subscribe to denoiser output and forward to our subscribers
        this.denoiser.subscribe(new Flow.Subscriber<double[]>() {
            private Flow.Subscription subscription;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(double[] item) {
                publisher.submit(item);
            }
            
            @Override
            public void onError(Throwable throwable) {
                publisher.closeExceptionally(throwable);
            }
            
            @Override
            public void onComplete() {
                publisher.close();
            }
        });
        
        // Create performance profile
        this.profile = PerformanceProfile.fastProfile(config.getBlockSize());
    }
    
    @Override
    public void process(double[] samples) {
        denoiser.denoise(samples);
    }
    
    @Override
    public void flush() {
        // Close the denoiser which will trigger onComplete on our subscriber
        try {
            denoiser.close();
        } catch (Exception e) {
            // Log or handle error if needed
        }
    }
    
    @Override
    public PerformanceProfile getPerformanceProfile() {
        return profile;
    }
    
    @Override
    public MODWTStreamingTransform.StreamingStatistics getStatistics() {
        // Return basic statistics based on denoiser state
        final long samples = denoiser.getSamplesProcessed();
        final long blocks = samples / config.getBlockSize();
        
        return new MODWTStreamingTransform.StreamingStatistics() {
            @Override
            public long getSamplesProcessed() {
                return samples;
            }
            
            @Override
            public long getBlocksProcessed() {
                return blocks;
            }
            
            @Override
            public long getAverageProcessingTimeNanos() {
                // Estimate based on performance profile
                return (long)(profile.expectedLatencyMicros() * 1000);
            }
            
            @Override
            public long getMaxProcessingTimeNanos() {
                // Estimate max as 2x average for fast implementation
                return (long)(profile.expectedLatencyMicros() * 2000);
            }
            
            @Override
            public long getMinProcessingTimeNanos() {
                // Estimate min as 0.5x average for fast implementation
                return (long)(profile.expectedLatencyMicros() * 500);
            }
            
            @Override
            public double getThroughputSamplesPerSecond() {
                // Samples per second estimate
                return 1_000_000.0 / profile.expectedLatencyMicros();
            }
            
            @Override
            public void reset() {
                // Not implemented - statistics are read-only estimates
            }
        };
    }
    
    @Override
    public void subscribe(Flow.Subscriber<? super double[]> subscriber) {
        publisher.subscribe(subscriber);
    }
    
    @Override
    public void close() {
        denoiser.close();
        publisher.close();
    }
}