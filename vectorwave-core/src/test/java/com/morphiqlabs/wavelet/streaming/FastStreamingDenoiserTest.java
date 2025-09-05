package com.morphiqlabs.wavelet.streaming;

import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;
import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingTransform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.Flow;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FastStreamingDenoiser through the factory interface.
 */
class FastStreamingDenoiserTest {

    private StreamingDenoiserConfig config;
    private StreamingDenoiserStrategy fastDenoiser;

    @BeforeEach
    void setUp() {
        config = new StreamingDenoiserConfig.Builder()
            .wavelet(Daubechies.DB4)
            .blockSize(512)
            .thresholdMethod(WaveletDenoiser.ThresholdMethod.UNIVERSAL)
            .thresholdType(WaveletDenoiser.ThresholdType.SOFT)
            .thresholdMultiplier(1.0)
            .build();
        
        fastDenoiser = StreamingDenoiserFactory.create(
            StreamingDenoiserFactory.Implementation.FAST, config);
    }

    @AfterEach
    void tearDown() {
        if (fastDenoiser != null) {
            fastDenoiser.close();
        }
    }

    @Test
    @DisplayName("FastStreaming denoiser should have correct performance profile")
    void testPerformanceProfile() {
        StreamingDenoiserStrategy.PerformanceProfile profile = fastDenoiser.getPerformanceProfile();
        
        assertNotNull(profile);
        assertTrue(profile.isRealTimeCapable(), "Fast denoiser should be real-time capable");
        assertEquals(6.0, profile.expectedSNRImprovement(), 0.1, 
            "Fast denoiser should provide ~6dB SNR improvement");
        assertEquals(0.1 * config.getBlockSize(), profile.expectedLatencyMicros(), 0.001,
            "Fast denoiser latency should be 0.1 * blockSize");
        assertEquals((long) config.getBlockSize() * 8 * 4, profile.memoryUsageBytes(),
            "Memory usage should be proportional to block size");
    }

    @Test
    @DisplayName("FastStreaming denoiser should process samples")
    void testProcessSamples() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        List<double[]> results = new CopyOnWriteArrayList<>();
        
        // Subscribe to results
        fastDenoiser.subscribe(new Flow.Subscriber<double[]>() {
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
        
        // Process some test data
        double[] testSamples = new double[config.getBlockSize()];
        for (int i = 0; i < testSamples.length; i++) {
            testSamples[i] = Math.sin(2 * Math.PI * i / 32.0) + 0.1 * Math.random();
        }
        
        fastDenoiser.process(testSamples);
        
        // Wait for processing to complete
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Should receive denoised output");
        assertFalse(results.isEmpty(), "Should have received denoised data");
        assertEquals(testSamples.length, results.get(0).length, 
            "Output should have same length as input");
    }

    @Test
    @DisplayName("FastStreaming denoiser should provide statistics")
    void testStatistics() {
        MODWTStreamingTransform.StreamingStatistics stats = fastDenoiser.getStatistics();
        
        assertNotNull(stats);
        assertEquals(0, stats.getSamplesProcessed(), "Initially no samples processed");
        assertEquals(0, stats.getBlocksProcessed(), "Initially no blocks processed");
        
        // Process some data
        double[] testSamples = new double[config.getBlockSize()];
        fastDenoiser.process(testSamples);
        
        // Note: Statistics might be updated asynchronously, so we just verify the method works
        assertNotNull(fastDenoiser.getStatistics(), "Statistics should always be available");
    }

    @Test
    @DisplayName("FastStreaming denoiser should handle multiple subscribers")
    void testMultipleSubscribers() throws InterruptedException {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        List<double[]> results1 = new CopyOnWriteArrayList<>();
        List<double[]> results2 = new CopyOnWriteArrayList<>();
        
        // First subscriber
        fastDenoiser.subscribe(new Flow.Subscriber<double[]>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(10);
            }
            
            @Override
            public void onNext(double[] item) {
                results1.add(item);
                latch1.countDown();
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Subscriber 1 error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {}
        });
        
        // Second subscriber
        fastDenoiser.subscribe(new Flow.Subscriber<double[]>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(10);
            }
            
            @Override
            public void onNext(double[] item) {
                results2.add(item);
                latch2.countDown();
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Subscriber 2 error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {}
        });
        
        // Process data
        double[] testSamples = new double[config.getBlockSize()];
        for (int i = 0; i < testSamples.length; i++) {
            testSamples[i] = Math.sin(2 * Math.PI * i / 16.0);
        }
        
        fastDenoiser.process(testSamples);
        
        // Both subscribers should receive the same data
        assertTrue(latch1.await(5, TimeUnit.SECONDS), "First subscriber should receive data");
        assertTrue(latch2.await(5, TimeUnit.SECONDS), "Second subscriber should receive data");
        
        assertFalse(results1.isEmpty(), "First subscriber should have results");
        assertFalse(results2.isEmpty(), "Second subscriber should have results");
    }

    @Test
    @DisplayName("FastStreaming denoiser should handle flush correctly")
    void testFlush() throws InterruptedException {
        CountDownLatch completeLatch = new CountDownLatch(1);
        
        fastDenoiser.subscribe(new Flow.Subscriber<double[]>() {
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
        fastDenoiser.process(testSamples);
        
        // Flush should trigger completion
        fastDenoiser.flush();
        
        assertTrue(completeLatch.await(5, TimeUnit.SECONDS), 
            "Flush should trigger onComplete for subscribers");
    }

    @Test
    @DisplayName("FastStreaming denoiser should be closeable")
    void testClose() {
        assertDoesNotThrow(() -> fastDenoiser.close(), 
            "Close should not throw exceptions");
        
        // After close, further operations should still be safe
        assertDoesNotThrow(() -> fastDenoiser.getPerformanceProfile(),
            "Getting performance profile after close should be safe");
        assertDoesNotThrow(() -> fastDenoiser.getStatistics(),
            "Getting statistics after close should be safe");
    }

    @Test
    @DisplayName("FastStreaming denoiser should handle various block sizes")
    void testDifferentBlockSizes() {
        int[] blockSizes = {128, 256, 512, 1024, 2048};
        
        for (int blockSize : blockSizes) {
            StreamingDenoiserConfig testConfig = new StreamingDenoiserConfig.Builder()
                .wavelet(Daubechies.DB4)
                .blockSize(blockSize)
                .thresholdMethod(WaveletDenoiser.ThresholdMethod.UNIVERSAL)
                .thresholdType(WaveletDenoiser.ThresholdType.SOFT)
                .build();
            
            try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
                    StreamingDenoiserFactory.Implementation.FAST, testConfig)) {
                
                StreamingDenoiserStrategy.PerformanceProfile profile = 
                    denoiser.getPerformanceProfile();
                
                assertTrue(profile.isRealTimeCapable(), 
                    "Fast denoiser should be real-time capable for block size: " + blockSize);
                assertEquals(0.1 * blockSize, profile.expectedLatencyMicros(), 0.001,
                    "Latency should scale with block size");
            }
        }
    }

    @Test
    @DisplayName("FastStreaming denoiser should handle edge case inputs")
    void testEdgeCaseInputs() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3); // Expect 3 outputs
        List<double[]> results = new ArrayList<>();
        
        fastDenoiser.subscribe(new Flow.Subscriber<double[]>() {
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
        
        // Test zero signal
        double[] zeros = new double[config.getBlockSize()];
        fastDenoiser.process(zeros);
        
        // Test constant signal
        double[] constants = new double[config.getBlockSize()];
        java.util.Arrays.fill(constants, 5.0);
        fastDenoiser.process(constants);
        
        // Test very small values
        double[] small = new double[config.getBlockSize()];
        for (int i = 0; i < small.length; i++) {
            small[i] = 1e-10 * Math.sin(2 * Math.PI * i / 64.0);
        }
        fastDenoiser.process(small);
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), 
            "Should handle all edge case inputs");
        assertEquals(3, results.size(), "Should receive output for all inputs");
    }
}
