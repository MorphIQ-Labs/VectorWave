package com.morphiqlabs.wavelet.streaming;

import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;
import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingTransform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for QualityStreamingDenoiser through the factory interface.
 */
class QualityStreamingDenoiserTest {

    private StreamingDenoiserConfig config;
    private StreamingDenoiserStrategy qualityDenoiser;

    @BeforeEach
    void setUp() {
        config = new StreamingDenoiserConfig.Builder()
            .wavelet(Daubechies.DB6)
            .blockSize(512)
            .thresholdMethod(WaveletDenoiser.ThresholdMethod.UNIVERSAL)
            .thresholdType(WaveletDenoiser.ThresholdType.SOFT)
            .thresholdMultiplier(0.8)
            .build();
        
        qualityDenoiser = StreamingDenoiserFactory.create(
            StreamingDenoiserFactory.Implementation.QUALITY, config);
    }

    @AfterEach
    void tearDown() {
        if (qualityDenoiser != null) {
            qualityDenoiser.close();
        }
    }

    @Test
    @DisplayName("Quality streaming denoiser should have correct performance profile")
    void testPerformanceProfile() {
        StreamingDenoiserStrategy.PerformanceProfile profile = qualityDenoiser.getPerformanceProfile();
        
        assertNotNull(profile);
        assertTrue(profile.isRealTimeCapable(), "Quality denoiser should be real-time capable for 512 block size");
        assertEquals(9.0, profile.expectedSNRImprovement(), 0.1, 
            "Quality denoiser should provide ~9dB SNR improvement");
        assertTrue(profile.expectedLatencyMicros() > 0, "Quality denoiser should have measurable latency");
        assertTrue(profile.memoryUsageBytes() > 0, "Quality denoiser should use memory");
    }

    @Test
    @DisplayName("Quality streaming denoiser should process samples with higher quality")
    void testProcessSamples() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<double[]> results = new CopyOnWriteArrayList<>();
        
        // Subscribe to results
        qualityDenoiser.subscribe(new Flow.Subscriber<double[]>() {
            private Flow.Subscription subscription;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(10);
            }
            
            @Override
            public void onNext(double[] item) {
                results.add(item);
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Unexpected error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {
                // Expected when denoiser is closed
            }
        });
        
        // Process noisy test signal
        double[] testSamples = new double[config.getBlockSize()];
        for (int i = 0; i < testSamples.length; i++) {
            // Clean signal plus noise
            double clean = Math.sin(2 * Math.PI * i / 32.0) + 0.5 * Math.sin(2 * Math.PI * i / 64.0);
            double noise = 0.2 * (Math.random() - 0.5);
            testSamples[i] = clean + noise;
        }
        
        qualityDenoiser.process(testSamples);
        
        // Wait for processing to complete
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Should receive denoised output");
        assertFalse(results.isEmpty(), "Should have received denoised data");
        // Quality denoiser with overlap outputs smaller blocks due to overlap-add processing
        int expectedOutputSize = config.getBlockSize() - (int)(config.getBlockSize() * config.getOverlapFactor());
        assertEquals(expectedOutputSize, results.get(0).length, 
            "Output should have reduced size due to overlap processing");
    }

    @Test
    @DisplayName("Quality streaming denoiser should provide statistics")
    void testStatistics() {
        MODWTStreamingTransform.StreamingStatistics stats = qualityDenoiser.getStatistics();
        
        assertNotNull(stats);
        assertEquals(0, stats.getSamplesProcessed(), "Initially no samples processed");
        assertEquals(0, stats.getBlocksProcessed(), "Initially no blocks processed");
        
        // Process some data
        double[] testSamples = new double[config.getBlockSize()];
        qualityDenoiser.process(testSamples);
        
        // Verify statistics interface is functional
        assertNotNull(qualityDenoiser.getStatistics(), "Statistics should always be available");
    }

    @Test
    @DisplayName("Quality streaming denoiser should handle large block sizes")
    void testLargeBlockSize() {
        StreamingDenoiserConfig largeConfig = new StreamingDenoiserConfig.Builder()
            .wavelet(Daubechies.DB4)
            .blockSize(2048) // > 512, should not be real-time capable
            .thresholdMethod(WaveletDenoiser.ThresholdMethod.UNIVERSAL)
            .thresholdType(WaveletDenoiser.ThresholdType.SOFT)
            .build();
        
        try (StreamingDenoiserStrategy largeDdenoiser = StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.QUALITY, largeConfig)) {
            
            StreamingDenoiserStrategy.PerformanceProfile profile = 
                largeDdenoiser.getPerformanceProfile();
            
            assertFalse(profile.isRealTimeCapable(), 
                "Quality denoiser should not be real-time capable for large blocks");
            assertEquals(9.0, profile.expectedSNRImprovement(), 0.1,
                "SNR improvement should be consistent regardless of block size");
        }
    }

    @Test
    @DisplayName("Quality streaming denoiser should have better SNR than fast")
    void testQualityVsFastComparison() {
        // Create fast denoiser for comparison
        try (StreamingDenoiserStrategy fastDenoiser = StreamingDenoiserFactory.create(
                StreamingDenoiserFactory.Implementation.FAST, config)) {
            
            StreamingDenoiserStrategy.PerformanceProfile qualityProfile = 
                qualityDenoiser.getPerformanceProfile();
            StreamingDenoiserStrategy.PerformanceProfile fastProfile = 
                fastDenoiser.getPerformanceProfile();
            
            assertTrue(qualityProfile.expectedSNRImprovement() > fastProfile.expectedSNRImprovement(),
                "Quality denoiser should have better SNR improvement than fast");
            assertTrue(qualityProfile.expectedLatencyMicros() > fastProfile.expectedLatencyMicros(),
                "Quality denoiser should have higher latency than fast");
            assertTrue(qualityProfile.memoryUsageBytes() > fastProfile.memoryUsageBytes(),
                "Quality denoiser should use more memory than fast");
        }
    }

    @Test
    @DisplayName("Quality streaming denoiser should handle flush correctly")
    void testFlush() throws InterruptedException {
        CountDownLatch completeLatch = new CountDownLatch(1);
        
        qualityDenoiser.subscribe(new Flow.Subscriber<double[]>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(double[] item) {
                // Process received data
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Unexpected error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {
                completeLatch.countDown();
            }
        });
        
        // Process some data
        double[] testSamples = new double[config.getBlockSize()];
        for (int i = 0; i < testSamples.length; i++) {
            testSamples[i] = Math.cos(2 * Math.PI * i / 64.0);
        }
        qualityDenoiser.process(testSamples);
        
        // Flush should trigger completion
        qualityDenoiser.flush();
        
        assertTrue(completeLatch.await(10, TimeUnit.SECONDS), 
            "Flush should trigger onComplete for subscribers");
    }

    @Test
    @DisplayName("Quality streaming denoiser should be closeable")
    void testClose() {
        assertDoesNotThrow(() -> qualityDenoiser.close(), 
            "Close should not throw exceptions");
        
        // After close, further operations should still be safe
        assertDoesNotThrow(() -> qualityDenoiser.getPerformanceProfile(),
            "Getting performance profile after close should be safe");
        assertDoesNotThrow(() -> qualityDenoiser.getStatistics(),
            "Getting statistics after close should be safe");
    }

    @Test
    @DisplayName("Quality streaming denoiser should handle multiple processing rounds")
    void testMultipleProcessingRounds() throws InterruptedException {
        // Quality denoiser with overlap processing may generate more outputs due to overlap-add
        CountDownLatch latch = new CountDownLatch(5); // Expect more outputs due to overlap processing
        List<double[]> results = new ArrayList<>();
        
        qualityDenoiser.subscribe(new Flow.Subscriber<double[]>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(double[] item) {
                results.add(item);
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Unexpected error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {}
        });
        
        // Process multiple rounds of data
        for (int round = 0; round < 3; round++) {
            double[] testSamples = new double[config.getBlockSize()];
            for (int i = 0; i < testSamples.length; i++) {
                testSamples[i] = Math.sin(2 * Math.PI * i / (32.0 + round * 8)) + 
                                0.1 * Math.random();
            }
            qualityDenoiser.process(testSamples);
        }
        
        assertTrue(latch.await(15, TimeUnit.SECONDS), 
            "Should receive all outputs from multiple processing rounds");
        assertTrue(results.size() >= 3, "Should receive at least as many outputs as inputs with overlap processing");
    }

    @Test
    @DisplayName("Quality streaming denoiser should handle various signal characteristics")
    void testVariousSignalCharacteristics() throws InterruptedException {
        // Quality denoiser with overlap processing generates more outputs due to overlap-add
        CountDownLatch latch = new CountDownLatch(8); // Expect more outputs due to overlap processing
        List<double[]> results = new ArrayList<>();
        
        qualityDenoiser.subscribe(new Flow.Subscriber<double[]>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(double[] item) {
                results.add(item);
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Unexpected error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {}
        });
        
        // Test different signal types that quality denoiser should handle well
        
        // 1. High-frequency noise
        double[] highFreqNoise = new double[config.getBlockSize()];
        for (int i = 0; i < highFreqNoise.length; i++) {
            highFreqNoise[i] = Math.sin(2 * Math.PI * i / 8.0) + 0.5 * Math.random();
        }
        qualityDenoiser.process(highFreqNoise);
        
        // 2. Low-frequency signal with noise
        double[] lowFreqSignal = new double[config.getBlockSize()];
        for (int i = 0; i < lowFreqSignal.length; i++) {
            lowFreqSignal[i] = Math.sin(2 * Math.PI * i / 128.0) + 0.1 * Math.random();
        }
        qualityDenoiser.process(lowFreqSignal);
        
        // 3. Chirp signal (changing frequency)
        double[] chirpSignal = new double[config.getBlockSize()];
        for (int i = 0; i < chirpSignal.length; i++) {
            double freq = 0.01 + 0.1 * i / chirpSignal.length;
            chirpSignal[i] = Math.sin(2 * Math.PI * freq * i) + 0.1 * Math.random();
        }
        qualityDenoiser.process(chirpSignal);
        
        // 4. Mixed frequency content
        double[] mixedSignal = new double[config.getBlockSize()];
        for (int i = 0; i < mixedSignal.length; i++) {
            mixedSignal[i] = 0.5 * Math.sin(2 * Math.PI * i / 16.0) +
                           0.3 * Math.sin(2 * Math.PI * i / 64.0) +
                           0.2 * Math.sin(2 * Math.PI * i / 128.0) +
                           0.1 * Math.random();
        }
        qualityDenoiser.process(mixedSignal);
        
        assertTrue(latch.await(20, TimeUnit.SECONDS), 
            "Should handle various signal characteristics");
        assertTrue(results.size() >= 4, "Should process all signal types with overlap processing");
        
        // Verify all outputs have expected overlap-reduced length
        int expectedOutputSize = config.getBlockSize() - (int)(config.getBlockSize() * config.getOverlapFactor());
        for (double[] result : results) {
            assertEquals(expectedOutputSize, result.length, 
                "All outputs should have overlap-reduced length");
        }
    }
}
