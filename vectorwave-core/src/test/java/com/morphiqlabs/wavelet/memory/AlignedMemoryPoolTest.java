package com.morphiqlabs.wavelet.memory;

import com.morphiqlabs.wavelet.memory.AlignedMemoryPool.PooledArray;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AlignedMemoryPoolTest {
    
    @BeforeEach
    void setUp() {
        AlignedMemoryPool.clear();
    }
    
    @AfterEach
    void tearDown() {
        AlignedMemoryPool.clear();
    }
    
    @Test
    void testBasicAllocation() {
        PooledArray array = AlignedMemoryPool.allocate(128);
        
        assertNotNull(array);
        assertNotNull(array.array());
        assertEquals(128, array.length());
        assertTrue(array.offset() >= 0);
        assertFalse(array.isReleased());
    }
    
    @Test
    void testPoolReuse() {
        // Allocate and release
        PooledArray first = AlignedMemoryPool.allocate(64);
        double[] firstData = first.array();
        firstData[first.offset()] = 42.0;
        AlignedMemoryPool.release(first);
        
        // Allocate again - should get from pool
        PooledArray second = AlignedMemoryPool.allocate(64);
        
        // Should be cleared
        assertEquals(0.0, second.array()[second.offset()]);
        
        // Check statistics show pool hit
        String stats = AlignedMemoryPool.getStatistics();
        assertTrue(stats.contains("hits=1"));
    }
    
    @Test
    void testCommonSizes() {
        int[] commonSizes = {64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768};
        
        List<PooledArray> arrays = new ArrayList<>();
        
        // Allocate all common sizes
        for (int size : commonSizes) {
            PooledArray array = AlignedMemoryPool.allocate(size);
            assertEquals(size, array.length());
            arrays.add(array);
        }
        
        // Release all
        for (PooledArray array : arrays) {
            AlignedMemoryPool.release(array);
        }
        
        // Allocate again - should all come from pool
        for (int size : commonSizes) {
            PooledArray array = AlignedMemoryPool.allocate(size);
            assertEquals(size, array.length());
            arrays.add(array);
        }
        
        // Check hit rate
        String stats = AlignedMemoryPool.getStatistics();
        assertTrue(stats.contains("hits=" + commonSizes.length));
    }
    
    @Test
    void testUncommonSizes() {
        // Non-power-of-2 sizes should still work
        PooledArray array = AlignedMemoryPool.allocate(100);
        
        assertNotNull(array);
        assertEquals(100, array.length());
        
        // Release and allocate again
        AlignedMemoryPool.release(array);
        PooledArray second = AlignedMemoryPool.allocate(100);
        
        // Won't be pooled (not a common size)
        String stats = AlignedMemoryPool.getStatistics();
        assertTrue(stats.contains("misses=2"));
    }
    
    @Test
    void testLargeArrayNotPooled() {
        // Very large array
        PooledArray large = AlignedMemoryPool.allocate(100000);
        assertNotNull(large);
        assertEquals(100000, large.length());
        
        // Release
        AlignedMemoryPool.release(large);
        
        // Allocate again - won't come from pool
        PooledArray second = AlignedMemoryPool.allocate(100000);
        
        String stats = AlignedMemoryPool.getStatistics();
        assertTrue(stats.contains("misses=2"));
    }
    
    @Test
    void testArrayOperations() {
        PooledArray array = AlignedMemoryPool.allocate(64);
        
        // Test clear
        double[] data = array.array();
        int offset = array.offset();
        for (int i = 0; i < array.length(); i++) {
            data[offset + i] = i;
        }
        
        array.clear();
        
        for (int i = 0; i < array.length(); i++) {
            assertEquals(0.0, data[offset + i]);
        }
        
        // Test copyFrom
        double[] source = new double[64];
        for (int i = 0; i < 64; i++) {
            source[i] = i * 2;
        }
        array.copyFrom(source, 0, 64);
        
        for (int i = 0; i < 64; i++) {
            assertEquals(i * 2, data[offset + i]);
        }
        
        // Test copyTo
        double[] dest = new double[64];
        array.copyTo(dest, 0, 64);
        
        for (int i = 0; i < 64; i++) {
            assertEquals(i * 2, dest[i]);
        }
    }
    
    @Test
    void testAutoCloseable() {
        PooledArray array;
        
        // Use try-with-resources
        try (PooledArray temp = AlignedMemoryPool.allocate(128)) {
            array = temp;
            assertFalse(array.isReleased());
        }
        
        // Should be released after try block
        assertTrue(array.isReleased());
    }
    
    @Test
    void testReleasedArrayAccess() {
        PooledArray array = AlignedMemoryPool.allocate(64);
        array.markReleased();
        
        assertTrue(array.isReleased());
        assertThrows(IllegalStateException.class, () -> array.array());
    }
    
    @Test
    void testDoubleRelease() {
        PooledArray array = AlignedMemoryPool.allocate(64);
        
        AlignedMemoryPool.release(array);
        assertTrue(array.isReleased());
        
        // Second release should be no-op
        assertDoesNotThrow(() -> AlignedMemoryPool.release(array));
    }
    
    @Test
    void testNullRelease() {
        // Releasing null should be no-op
        assertDoesNotThrow(() -> AlignedMemoryPool.release(null));
    }
    
    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testConcurrentAccess() throws Exception {
        int threadCount = 8;
        int allocationsPerThread = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();
        
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        
        try {
            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        List<PooledArray> arrays = new ArrayList<>();
                        
                        // Allocate
                        for (int i = 0; i < allocationsPerThread; i++) {
                            arrays.add(AlignedMemoryPool.allocate(128));
                        }
                        
                        // Use
                        for (PooledArray array : arrays) {
                            array.array()[array.offset()] = Thread.currentThread().threadId();
                        }
                        
                        // Release
                        for (PooledArray array : arrays) {
                            AlignedMemoryPool.release(array);
                        }
                        
                        successCount.incrementAndGet();
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }
            
            // Start all threads
            startLatch.countDown();
            
            // Wait for completion
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS));
            assertEquals(threadCount, successCount.get());
            
            // Verify statistics
            String stats = AlignedMemoryPool.getStatistics();
            assertTrue(stats.contains("allocations=" + (threadCount * allocationsPerThread)));
        } finally {
            executor.shutdown();
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 8, 64, 128, 256, 512, 1024})
    void testVariousSizes(int size) {
        PooledArray array = AlignedMemoryPool.allocate(size);
        
        assertNotNull(array);
        assertEquals(size, array.length());
        assertNotNull(array.array());
        assertTrue(array.array().length >= size + array.offset());
        
        // Test alignment - offset should position data on cache line
        int offsetBytes = array.offset() * 8;
        assertTrue(offsetBytes < 64); // Within one cache line
    }
    
    @Test
    void testPoolCapacity() {
        // Fill up thread-local pool
        List<PooledArray> arrays = new ArrayList<>();
        for (int i = 0; i < 20; i++) { // More than pool capacity
            arrays.add(AlignedMemoryPool.allocate(64));
        }
        
        // Release all
        for (PooledArray array : arrays) {
            AlignedMemoryPool.release(array);
        }
        
        // Allocate many - some will hit pool, some won't
        arrays.clear();
        for (int i = 0; i < 20; i++) {
            arrays.add(AlignedMemoryPool.allocate(64));
        }
        
        // Should have some hits and some misses
        String stats = AlignedMemoryPool.getStatistics();
        int allocations = 40; // 20 + 20
        assertTrue(stats.contains("allocations=" + allocations));
        
        // Clean up
        for (PooledArray array : arrays) {
            AlignedMemoryPool.release(array);
        }
    }
    
    @Test
    void testStatisticsFormat() {
        // Do some operations
        PooledArray array1 = AlignedMemoryPool.allocate(64);
        AlignedMemoryPool.release(array1);
        PooledArray array2 = AlignedMemoryPool.allocate(64); // Hit
        PooledArray array3 = AlignedMemoryPool.allocate(100); // Miss
        
        String stats = AlignedMemoryPool.getStatistics();
        
        assertTrue(stats.startsWith("AlignedMemoryPool:"));
        assertTrue(stats.contains("allocations=3"));
        assertTrue(stats.contains("hits=1"));
        assertTrue(stats.contains("misses=2"));
        assertTrue(stats.contains("hitRate="));
        assertTrue(stats.contains("%"));
    }
    
    @Test
    void testZeroHitRate() {
        // Only misses
        AlignedMemoryPool.allocate(100);
        AlignedMemoryPool.allocate(200);
        
        String stats = AlignedMemoryPool.getStatistics();
        assertTrue(stats.contains("hitRate=0.0%"));
    }
}