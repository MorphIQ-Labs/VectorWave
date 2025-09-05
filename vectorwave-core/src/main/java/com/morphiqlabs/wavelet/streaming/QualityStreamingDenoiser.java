package com.morphiqlabs.wavelet.streaming;

import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingDenoiser;
import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingTransform;
import com.morphiqlabs.wavelet.exception.WaveletTransformException;
import java.util.Arrays;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;

/**
 * Quality implementation of streaming denoiser optimized for SNR improvement.
 * 
 * <p>This implementation uses overlapping blocks and advanced processing
 * to achieve better denoising quality at the cost of higher latency.</p>
 * 
 * <p>Suitable for:</p>
 * <ul>
 *   <li>Offline or near-real-time processing</li>
 *   <li>High-quality audio production</li>
 *   <li>Scientific data analysis</li>
 * </ul>
 */
final class QualityStreamingDenoiser implements StreamingDenoiserStrategy {
    
    private final MODWTStreamingDenoiser denoiser;
    private final StreamingDenoiserConfig config;
    private final SubmissionPublisher<double[]> publisher;
    private final PerformanceProfile profile;
    
    // Overlap processing buffers
    private final int blockSize;
    private final int overlapSize;
    private final double[] overlapBuffer;
    private final double[] processBuffer;
    private final double[] window;
    private boolean hasOverlap = false;
    
    QualityStreamingDenoiser(StreamingDenoiserConfig config) {
        this.config = config;
        this.publisher = new SubmissionPublisher<>();
        this.blockSize = config.getBlockSize();
        this.overlapSize = (int)(blockSize * config.getOverlapFactor());
        
        // Create buffers for overlap-add processing
        this.overlapBuffer = new double[overlapSize];
        this.processBuffer = new double[blockSize];
        this.window = createWindow(blockSize);
        
        // Create MODWT denoiser with quality settings
        this.denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(config.getWavelet())
            .bufferSize(blockSize)
            .thresholdMethod(config.getThresholdMethod())
            .thresholdType(config.getThresholdType())
            .thresholdMultiplier(config.getThresholdMultiplier())
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
            .noiseWindowSize(config.getNoiseWindowSize())
            .build();
        
        // Subscribe to denoiser output
        this.denoiser.subscribe(new Flow.Subscriber<double[]>() {
            private Flow.Subscription subscription;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(double[] item) {
                processWithOverlap(item);
            }
            
            @Override
            public void onError(Throwable throwable) {
                publisher.closeExceptionally(throwable);
            }
            
            @Override
            public void onComplete() {
                // Flush any remaining overlap
                if (hasOverlap && overlapSize > 0) {
                    publisher.submit(Arrays.copyOf(overlapBuffer, overlapSize));
                }
                publisher.close();
            }
        });
        
        // Create performance profile
        this.profile = PerformanceProfile.qualityProfile(blockSize, config.getOverlapFactor());
    }
    
    @Override
    public void process(double[] samples) {
        // Process with overlap if configured
        if (config.getOverlapFactor() > 0) {
            processWithOverlapAdd(samples);
        } else {
            // Direct processing without overlap
            denoiser.denoise(samples);
        }
    }
    
    private void processWithOverlapAdd(double[] samples) {
        int pos = 0;
        
        while (pos < samples.length) {
            int copySize = Math.min(blockSize, samples.length - pos);
            
            // Fill process buffer
            if (hasOverlap && overlapSize > 0) {
                // Copy overlap from previous block
                System.arraycopy(overlapBuffer, 0, processBuffer, 0, overlapSize);
                // Copy new samples
                System.arraycopy(samples, pos, processBuffer, overlapSize, 
                    Math.min(blockSize - overlapSize, copySize));
            } else {
                // First block - no overlap yet
                System.arraycopy(samples, pos, processBuffer, 0, copySize);
                if (copySize < blockSize) {
                    Arrays.fill(processBuffer, copySize, blockSize, 0.0);
                }
            }
            
            // Process the block
            denoiser.denoise(processBuffer);
            
            // Save overlap for next block
            if (overlapSize > 0 && pos + copySize < samples.length) {
                System.arraycopy(samples, pos + copySize - overlapSize, 
                    overlapBuffer, 0, overlapSize);
                hasOverlap = true;
            }
            
            pos += (blockSize - overlapSize);
        }
    }
    
    private void processWithOverlap(double[] denoisedBlock) {
        if (config.getOverlapFactor() == 0) {
            // No overlap - direct output
            publisher.submit(denoisedBlock);
            return;
        }
        
        // Apply window for smooth overlap-add
        double[] windowed = new double[blockSize];
        for (int i = 0; i < blockSize; i++) {
            windowed[i] = denoisedBlock[i] * window[i];
        }
        
        // Output the non-overlapping portion
        int outputSize = blockSize - overlapSize;
        double[] output = Arrays.copyOfRange(windowed, 0, outputSize);
        publisher.submit(output);
    }
    
    private double[] createWindow(int size) {
        // Create a Hann window for overlap-add
        double[] win = new double[size];
        for (int i = 0; i < size; i++) {
            win[i] = 0.5 - 0.5 * Math.cos(2 * Math.PI * i / (size - 1));
        }
        return win;
    }
    
    @Override
    public void flush() {
        // Process any remaining overlap buffer
        if (hasOverlap && overlapSize > 0) {
            double[] lastBlock = new double[blockSize];
            System.arraycopy(overlapBuffer, 0, lastBlock, 0, overlapSize);
            Arrays.fill(lastBlock, overlapSize, blockSize, 0.0);
            denoiser.denoise(lastBlock);
        }
        // Close the denoiser which will trigger onComplete on our subscriber
        try {
            denoiser.close();
        } catch (Exception e) {
            throw new WaveletTransformException("Failed to close denoiser during flush", e);
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
        final long blocks = samples / blockSize;
        
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
                // Estimate max as 3x average for quality implementation
                return (long)(profile.expectedLatencyMicros() * 3000);
            }
            
            @Override
            public long getMinProcessingTimeNanos() {
                // Estimate min as 0.7x average for quality implementation
                return (long)(profile.expectedLatencyMicros() * 700);
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