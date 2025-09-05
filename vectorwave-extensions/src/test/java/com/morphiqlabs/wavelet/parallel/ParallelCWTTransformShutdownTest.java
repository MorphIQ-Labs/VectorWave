package com.morphiqlabs.wavelet.extensions.parallel;

import com.morphiqlabs.wavelet.cwt.RickerWavelet;
import com.morphiqlabs.wavelet.cwt.CWTConfig;
import com.morphiqlabs.wavelet.cwt.CWTResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for proper executor shutdown hooks in ParallelCWTTransform.
 */
class ParallelCWTTransformShutdownTest {
    
    @Test
    @DisplayName("Should implement AutoCloseable")
    void testAutoCloseable() {
        ParallelCWTTransform transform = new ParallelCWTTransform(new RickerWavelet());
        assertInstanceOf(AutoCloseable.class, transform);
    }
    
    @Test
    @DisplayName("Try-with-resources should work correctly")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testTryWithResources() {
        assertDoesNotThrow(() -> {
            try (ParallelCWTTransform transform = new ParallelCWTTransform(new RickerWavelet())) {
                // Use the transform
                double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
                double[] scales = {1.0, 2.0, 4.0, 8.0};
                
                CWTResult result = transform.analyze(signal, scales);
                assertNotNull(result);
                
                // Transform should be functional
                assertFalse(transform.isClosed());
                assertFalse(transform.isShutdown());
            }
            // After try-with-resources, the transform should be closed
        });
    }
    
    @Test
    @DisplayName("Explicit close should work correctly")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testExplicitClose() {
        ParallelCWTTransform transform = new ParallelCWTTransform(new RickerWavelet());
        
        // Should not be closed initially
        assertFalse(transform.isClosed());
        assertFalse(transform.isShutdown());
        
        // Use the transform
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] scales = {1.0, 2.0, 4.0};
        
        CWTResult result = transform.analyze(signal, scales);
        assertNotNull(result);
        
        // Close the transform
        assertDoesNotThrow(transform::close);
        
        // Should be closed now
        assertTrue(transform.isClosed());
        assertTrue(transform.isShutdown());
    }
    
    @Test
    @DisplayName("Should throw exception when used after close")
    void testExceptionAfterClose() {
        ParallelConfig config = new ParallelConfig.Builder()
            .enableMetrics(true)
            .build();
        ParallelCWTTransform transform = new ParallelCWTTransform(
            new RickerWavelet(), CWTConfig.defaultConfig(), config);
        
        // Close the transform
        transform.close();
        
        // Should throw when trying to use
        assertThrows(IllegalStateException.class, () -> {
            double[] signal = {1.0, 2.0, 3.0, 4.0};
            double[] scales = {1.0, 2.0};
            transform.analyze(signal, scales);
        });
    }
    
    @Test
    @DisplayName("Multiple close calls should be safe")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testMultipleCloseCalls() {
        ParallelCWTTransform transform = new ParallelCWTTransform(new RickerWavelet());
        
        // Multiple close calls should not throw
        assertDoesNotThrow(() -> {
            transform.close();
            transform.close();
            transform.close();
        });
        
        assertTrue(transform.isClosed());
    }
    
    @Test
    @DisplayName("Shutdown should be idempotent")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testMultipleShutdownCalls() {
        ParallelConfig config = new ParallelConfig.Builder()
            .enableMetrics(true)
            .build();
        ParallelCWTTransform transform = new ParallelCWTTransform(
            new RickerWavelet(), CWTConfig.defaultConfig(), config);
        
        // Multiple shutdown calls should not throw
        assertDoesNotThrow(() -> {
            transform.shutdown();
            transform.shutdown();
            transform.shutdown();
        });
        
        assertTrue(transform.isShutdown());
    }
    
    @Test
    @DisplayName("Should handle concurrent access during shutdown")
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void testConcurrentShutdown() throws InterruptedException {
        ParallelConfig config = new ParallelConfig.Builder()
            .enableMetrics(true)
            .parallelThreshold(100)
            .build();
        ParallelCWTTransform transform = new ParallelCWTTransform(
            new RickerWavelet(), CWTConfig.defaultConfig(), config);
        
        int numThreads = 5;
        CountDownLatch latch = new CountDownLatch(numThreads);
        ExecutorService testExecutor = Executors.newFixedThreadPool(numThreads);
        
        // Start multiple threads trying to use and close the transform
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            testExecutor.submit(() -> {
                try {
                    if (threadId < 3) {
                        // Some threads try to use the transform
                        try {
                            double[] signal = new double[200];
                            double[] scales = {1.0, 2.0, 4.0};
                            transform.analyze(signal, scales);
                        } catch (IllegalStateException e) {
                            // Expected if transform is closed
                        }
                    } else {
                        // Some threads try to close it
                        transform.close();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS));
        assertTrue(transform.isClosed());
        
        testExecutor.shutdown();
        assertTrue(testExecutor.awaitTermination(5, TimeUnit.SECONDS));
    }
    
    @Test
    @DisplayName("Should work with different configurations")
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testDifferentConfigurations() {
        // Test with virtual threads
        ParallelConfig virtualConfig = new ParallelConfig.Builder()
            .useVirtualThreads(true)
            .enableMetrics(true)
            .build();
        
        try (ParallelCWTTransform virtualTransform = new ParallelCWTTransform(
                new RickerWavelet(), CWTConfig.defaultConfig(), virtualConfig)) {
            
            double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
            double[] scales = {1.0, 2.0};
            CWTResult result = virtualTransform.analyze(signal, scales);
            assertNotNull(result);
        }
        
        // Test with platform threads
        ParallelConfig platformConfig = new ParallelConfig.Builder()
            .useVirtualThreads(false)
            .enableMetrics(true)
            .build();
        
        try (ParallelCWTTransform platformTransform = new ParallelCWTTransform(
                new RickerWavelet(), CWTConfig.defaultConfig(), platformConfig)) {
            
            double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
            double[] scales = {1.0, 2.0};
            CWTResult result = platformTransform.analyze(signal, scales);
            assertNotNull(result);
        }
    }
    
    @Test
    @DisplayName("Should handle shutdown with common pool gracefully")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testCommonPoolShutdown() {
        // Test with configuration that uses common pool
        ParallelConfig commonPoolConfig = new ParallelConfig.Builder()
            .useVirtualThreads(false)
            .enableMetrics(false) // This should use common pool
            .build();
        
        ParallelCWTTransform transform = new ParallelCWTTransform(
            new RickerWavelet(), CWTConfig.defaultConfig(), commonPoolConfig);
        
        // Should shutdown gracefully even with common pool
        assertDoesNotThrow(transform::close);
        assertTrue(transform.isClosed());
    }
}
