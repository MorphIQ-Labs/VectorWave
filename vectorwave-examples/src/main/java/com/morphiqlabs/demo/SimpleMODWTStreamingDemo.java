package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser.ThresholdType;
import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingDenoiser;
import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingTransform;
import com.morphiqlabs.wavelet.modwt.MODWTResult;

import java.util.Random;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple demonstration of MODWT streaming capabilities.
 * 
 * <p>This demo shows basic usage of the MODWT streaming transform and denoiser
 * for real-time signal processing applications.</p>
 */
public class SimpleMODWTStreamingDemo {
    
    private static final Random random = new Random(42);
    
    public static void main(String[] args) throws Exception {
        System.out.println("=== Simple MODWT Streaming Demo ===\n");
        
        demonstrateStreamingTransform();
        System.out.println();
        
        demonstrateStreamingDenoiser();
    }
    
    private static void demonstrateStreamingTransform() {
        System.out.println("1. MODWT Streaming Transform");
        System.out.println("----------------------------");
        
        // Create streaming transform with flexible buffer size (not power of 2)
        int bufferSize = 300;  // Arbitrary size - MODWT handles it!
        try (MODWTStreamingTransform transform = MODWTStreamingTransform.create(
            new Haar(),
            BoundaryMode.PERIODIC,
            bufferSize
        )) {
        
        // Set up subscriber to receive results
        AtomicInteger resultCount = new AtomicInteger(0);
        transform.subscribe(new Flow.Subscriber<MODWTResult>() {
            private Flow.Subscription subscription;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(MODWTResult result) {
                int count = resultCount.incrementAndGet();
                System.out.printf("  Result %d: %d approx coeffs, %d detail coeffs\n",
                    count, 
                    result.approximationCoeffs().length,
                    result.detailCoeffs().length);
            }
            
            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {
                System.out.println("  Transform complete!");
            }
        });
        
        // Feed data in chunks
        System.out.println("Processing 3 chunks of data...");
        for (int i = 0; i < 3; i++) {
            double[] chunk = generateSignalChunk(bufferSize, i);
            transform.process(chunk);
        }
        
        // Flush remaining data
        transform.flush();
        
        // Show statistics
        MODWTStreamingTransform.StreamingStatistics stats = transform.getStatistics();
        System.out.println("\nStatistics:");
        System.out.printf("  Samples processed: %d\n", stats.getSamplesProcessed());
        System.out.printf("  Blocks processed: %d\n", stats.getBlocksProcessed());
        System.out.printf("  Throughput: %.0f samples/sec\n", stats.getThroughputSamplesPerSecond());
        
        }
    }
    
    private static void demonstrateStreamingDenoiser() throws Exception {
        System.out.println("2. MODWT Streaming Denoiser");
        System.out.println("---------------------------");
        
        // Create streaming denoiser with custom configuration
        try (MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
                .wavelet(Daubechies.DB4)
                .bufferSize(250)  // Non-power-of-2 buffer
                .thresholdType(ThresholdType.SOFT)
                .thresholdMethod(ThresholdMethod.UNIVERSAL)
                .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
                .build()) {
        
        System.out.println("Processing noisy signal in real-time...");
        
        // Process multiple chunks
        int chunks = 5;
        double totalNoiseReduction = 0;
        
        for (int i = 0; i < chunks; i++) {
            // Generate noisy data
            double[] clean = generateSignalChunk(250, i);
            double[] noisy = addNoise(clean, 0.3);
            
            // Denoise
            double[] denoised = denoiser.denoise(noisy);
            
            // Calculate noise reduction
            double noiseReduction = calculateNoiseReduction(noisy, denoised);
            totalNoiseReduction += noiseReduction;
            
            System.out.printf("  Chunk %d: Noise reduced by %.1f%%\n", 
                i + 1, noiseReduction * 100);
        }
        
        System.out.printf("\nAverage noise reduction: %.1f%%\n", 
            (totalNoiseReduction / chunks) * 100);
        System.out.printf("Estimated noise level: %.4f\n", 
            denoiser.getEstimatedNoiseLevel());
        System.out.printf("Total samples processed: %d\n", 
            denoiser.getSamplesProcessed());
        
        }
    }
    
    // Helper methods
    
    private static double[] generateSignalChunk(int length, int offset) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            // Create a signal with varying frequency
            double t = (offset * length + i) / 100.0;
            signal[i] = Math.sin(2 * Math.PI * t) + 
                       0.5 * Math.sin(6 * Math.PI * t);
        }
        return signal;
    }
    
    private static double[] addNoise(double[] signal, double noiseLevel) {
        double[] noisy = signal.clone();
        for (int i = 0; i < noisy.length; i++) {
            noisy[i] += noiseLevel * random.nextGaussian();
        }
        return noisy;
    }
    
    private static double calculateNoiseReduction(double[] noisy, double[] denoised) {
        double noisyVar = calculateVariance(noisy);
        double denoisedVar = calculateVariance(denoised);
        return Math.max(0, 1.0 - (denoisedVar / noisyVar));
    }
    
    private static double calculateVariance(double[] signal) {
        double mean = 0;
        for (double v : signal) mean += v;
        mean /= signal.length;
        
        double variance = 0;
        for (double v : signal) {
            double diff = v - mean;
            variance += diff * diff;
        }
        return variance / signal.length;
    }
}
