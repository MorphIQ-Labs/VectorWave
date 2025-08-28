package com.morphiqlabs.wavelet.cwt.memory;

import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.cwt.CWTConfig;
import com.morphiqlabs.wavelet.cwt.CWTTransform;
import com.morphiqlabs.wavelet.cwt.CWTResult;
import com.morphiqlabs.wavelet.cwt.ScaleSpace;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class CWTMemoryPoolTest {
    
    private CWTMemoryPool memoryPool;
    private MorletWavelet wavelet;
    
    // Simple real wavelet for testing
    private static class TestRealWavelet implements ContinuousWavelet {
        @Override
        public String name() { return "test"; }
        
        @Override
        public double psi(double t) {
            // Mexican hat wavelet formula
            double t2 = t * t;
            return (1 - t2) * Math.exp(-t2 / 2) / Math.sqrt(Math.sqrt(Math.PI));
        }
        
        @Override
        public double centerFrequency() { return 0.7; }
        
        @Override
        public double bandwidth() { return 1.0; }
        
        @Override
        public boolean isComplex() { return false; }
        
        @Override
        public double[] discretize(int length) {
            double[] samples = new double[length];
            int center = length / 2;
            for (int i = 0; i < length; i++) {
                samples[i] = psi((i - center) / 4.0);
            }
            return samples;
        }
    }
    
    private ContinuousWavelet realWavelet;
    
    @BeforeEach
    void setUp() {
        memoryPool = new CWTMemoryPool();
        wavelet = new MorletWavelet();
        realWavelet = new TestRealWavelet();
    }
    
    @Test
    @DisplayName("Should allocate coefficient arrays from pool")
    void testAllocateCoefficients() {
        // Given
        int rows = 10;
        int cols = 1024;
        
        // When
        double[][] coefficients = memoryPool.allocateCoefficients(rows, cols);
        
        // Then
        assertNotNull(coefficients);
        assertEquals(rows, coefficients.length);
        assertEquals(cols, coefficients[0].length);
        
        // Verify arrays are cleared
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                assertEquals(0.0, coefficients[i][j], "Array should be cleared");
            }
        }
    }
    
    @Test
    @DisplayName("Should reuse arrays from pool")
    void testArrayReuse() {
        // Given
        int size = 512;
        
        // When - allocate and release
        double[] array1 = memoryPool.allocateArray(size);
        array1[0] = 42.0; // Mark array
        memoryPool.releaseArray(array1);
        
        // When - allocate again
        double[] array2 = memoryPool.allocateArray(size);
        
        // Then - should get same array (cleared)
        assertSame(array1, array2, "Should reuse same array");
        assertEquals(0.0, array2[0], "Array should be cleared on reuse");
    }
    
    @Test
    @DisplayName("Should handle different array sizes")
    void testDifferentSizes() {
        // Test power-of-2 sizes
        int[] sizes = {128, 256, 512, 1024, 2048, 4096};
        double[][] arrays = new double[sizes.length][];
        
        // Allocate arrays of different sizes
        for (int i = 0; i < sizes.length; i++) {
            arrays[i] = memoryPool.allocateArray(sizes[i]);
            assertNotNull(arrays[i]);
            assertTrue(arrays[i].length >= sizes[i], 
                "Allocated array should be at least requested size");
        }
        
        // Release all arrays
        for (double[] array : arrays) {
            memoryPool.releaseArray(array);
        }
        
        // Allocate same sizes again to test reuse
        for (int size : sizes) {
            memoryPool.allocateArray(size);
        }
        
        // Verify pool statistics
        CWTMemoryPool.PoolStatistics stats = memoryPool.getStatistics();
        assertTrue(stats.totalAllocations() >= sizes.length * 2);
        assertTrue(stats.poolHits() > 0, "Should have pool hits on reuse");
    }
    
    @Test
    @DisplayName("Should work with CWT transform")
    void testWithCWTTransform() {
        // Given - use real wavelet and disable FFT to ensure direct convolution path is used
        CWTConfig config = CWTConfig.builder()
            .memoryPool(memoryPool)
            .normalizeScales(true)
            .enableFFT(false)  // Ensure direct convolution path
            .build();
            
        CWTTransform transform = new CWTTransform(realWavelet, config); // Use real wavelet
        
        double[] signal = new double[1024];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 10 * i / signal.length);
        }
        
        ScaleSpace scales = ScaleSpace.logarithmic(1.0, 64.0, 32);
        
        // Get initial stats
        CWTMemoryPool.PoolStatistics initialStats = memoryPool.getStatistics();
        
        // When
        CWTResult result = transform.analyze(signal, scales);
        
        // Then
        assertNotNull(result);
        assertEquals(32, result.getScales().length);
        assertEquals(1024, result.getCoefficients()[0].length);
        
        // Check pool was used - compare before and after
        CWTMemoryPool.PoolStatistics finalStats = memoryPool.getStatistics();
        assertTrue(finalStats.totalAllocations() > initialStats.totalAllocations(), 
            "Pool should have been used for allocations");
    }
    
    @Test
    @DisplayName("Should handle concurrent access")
    void testConcurrentAccess() throws InterruptedException {
        // Given
        int numThreads = 8;
        int allocationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        // When - multiple threads allocate and release arrays
        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < allocationsPerThread; i++) {
                        int size = 256 + (i % 4) * 256; // Vary sizes
                        double[] array = memoryPool.allocateArray(size);
                        
                        // Do some work
                        array[0] = Thread.currentThread().threadId();
                        array[size - 1] = i;
                        
                        // Release back to pool
                        memoryPool.releaseArray(array);
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for completion
        latch.await();
        executor.shutdown();
        
        // Then
        assertEquals(numThreads * allocationsPerThread, successCount.get(),
            "All allocations should succeed");
            
        CWTMemoryPool.PoolStatistics stats = memoryPool.getStatistics();
        assertTrue(stats.poolHits() > 0, "Should have pool hits with reuse");
    }
    
    @Test
    @DisplayName("Should limit pool size")
    void testPoolSizeLimit() {
        // Given - pool with size limit
        CWTMemoryPool limitedPool = new CWTMemoryPool(4); // Max 4 arrays per size
        
        // When - allocate and release many arrays
        double[][] arrays = new double[10][];
        for (int i = 0; i < 10; i++) {
            arrays[i] = limitedPool.allocateArray(512);
        }
        
        // Release all arrays
        for (double[] array : arrays) {
            limitedPool.releaseArray(array);
        }
        
        // Then - pool should only keep limited number
        CWTMemoryPool.PoolStatistics stats = limitedPool.getStatistics();
        assertTrue(stats.currentPoolSize() <= 4, 
            "Pool size should be limited to 4 arrays");
    }
    
    @Test
    @DisplayName("Should clear arrays on allocation")
    void testArrayClearing() {
        // Given
        double[] array = memoryPool.allocateArray(256);
        
        // Dirty the array
        for (int i = 0; i < array.length; i++) {
            array[i] = i;
        }
        
        // When - release and reallocate
        memoryPool.releaseArray(array);
        double[] reallocated = memoryPool.allocateArray(256);
        
        // Then - should be cleared
        for (int i = 0; i < reallocated.length; i++) {
            assertEquals(0.0, reallocated[i], 
                "Array should be cleared on reallocation");
        }
    }
    
    @Test
    @DisplayName("Should provide pool statistics")
    void testStatistics() {
        // Given/When - perform various operations
        double[] array1 = memoryPool.allocateArray(256);
        double[] array2 = memoryPool.allocateArray(512);
        memoryPool.releaseArray(array1);
        double[] array3 = memoryPool.allocateArray(256); // Should reuse array1
        memoryPool.releaseArray(array2);
        memoryPool.releaseArray(array3);
        
        // Then
        CWTMemoryPool.PoolStatistics stats = memoryPool.getStatistics();
        assertEquals(3, stats.totalAllocations());
        assertEquals(1, stats.poolHits()); // array3 reused array1
        assertEquals(2, stats.poolMisses()); // array1 and array2 were new
        assertEquals(2, stats.currentPoolSize()); // 2 arrays in pool
        assertTrue(stats.hitRate() > 0 && stats.hitRate() < 1.0);
    }
    
    @Test
    @DisplayName("Should support custom array factory")
    void testCustomArrayFactory() {
        // Given - pool with custom factory for aligned arrays
        CWTMemoryPool alignedPool = new CWTMemoryPool(
            size -> {
                // Allocate slightly larger for alignment
                int alignedSize = ((size + 7) / 8) * 8;
                return new double[alignedSize];
            }
        );
        
        // When
        double[] array = alignedPool.allocateArray(250);
        
        // Then
        assertNotNull(array);
        assertEquals(256, array.length, "Should be aligned to 8 elements");
    }
    
    @Test
    @DisplayName("Should handle memory pressure")
    void testMemoryPressure() {
        // Given
        System.gc(); // Clear memory before test
        long initialMemory = Runtime.getRuntime().totalMemory() - 
                           Runtime.getRuntime().freeMemory();
        
        // When - allocate many arrays
        for (int i = 0; i < 100; i++) {
            double[] array = memoryPool.allocateArray(4096);
            // Simulate some work
            array[0] = i;
            array[4095] = i;
            memoryPool.releaseArray(array);
        }
        
        // Force garbage collection
        System.gc();
        Thread.yield();
        System.gc();
        
        long finalMemory = Runtime.getRuntime().totalMemory() - 
                          Runtime.getRuntime().freeMemory();
        
        // Then - memory usage should be reasonable
        long memoryIncrease = finalMemory - initialMemory;
        assertTrue(memoryIncrease < 10 * 1024 * 1024, // Less than 10MB
            "Memory pool should not leak excessive memory");
        
        // Clear the pool
        memoryPool.clear();
        
        CWTMemoryPool.PoolStatistics stats = memoryPool.getStatistics();
        assertEquals(0, stats.currentPoolSize(), 
            "Pool should be empty after clear");
    }
}