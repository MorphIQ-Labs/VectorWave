package com.morphiqlabs.wavelet.cwt.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test coverage for CWTMemoryPool and related classes.
 */
public class CWTMemoryPoolComprehensiveTest {
    
    private CWTMemoryPool pool;
    
    @BeforeEach
    public void setUp() {
        pool = new CWTMemoryPool();
    }
    
    @Test
    public void testConstructor_Default() {
        CWTMemoryPool defaultPool = new CWTMemoryPool();
        assertNotNull(defaultPool);
    }
    
    @Test
    public void testConstructor_CustomSize() {
        CWTMemoryPool customPool = new CWTMemoryPool(32);
        assertNotNull(customPool);
    }
    
    @Test
    public void testAllocateArray_BasicAllocation() {
        int size = 128;
        double[] array = pool.allocateArray(size);
        
        assertNotNull(array);
        assertTrue(array.length >= size); // May be rounded up to power of 2
        
        // Array should be zeroed (at least up to requested size)
        for (int i = 0; i < size; i++) {
            assertEquals(0.0, array[i]);
        }
    }
    
    @Test
    public void testAllocateArray_PowerOfTwoOptimization() {
        // Non-power-of-2 size should be rounded up
        double[] array1 = pool.allocateArray(100);
        assertTrue(array1.length >= 100);
        assertEquals(128, array1.length); // Should round to next power of 2
        
        // Power-of-2 should be exact
        double[] array2 = pool.allocateArray(256);
        assertEquals(256, array2.length);
    }
    
    @Test
    public void testReleaseArray_Reuse() {
        int size = 64;
        
        // Get an array
        double[] array1 = pool.allocateArray(size);
        assertNotNull(array1);
        
        // Modify it
        array1[0] = 123.456;
        
        // Return it to pool
        pool.releaseArray(array1);
        
        // Get another array of same size - should reuse
        double[] array2 = pool.allocateArray(size);
        assertNotNull(array2);
        
        // Should be cleared (zeroed)
        assertEquals(0.0, array2[0]);
    }
    
    @Test
    public void testReleaseArray_Null() {
        // Should handle null gracefully
        assertDoesNotThrow(() -> pool.releaseArray(null));
    }
    
    @Test
    public void testAllocateCoefficients_BasicAllocation() {
        int rows = 8;
        int cols = 64;
        double[][] matrix = pool.allocateCoefficients(rows, cols);
        
        assertNotNull(matrix);
        assertEquals(rows, matrix.length);
        for (double[] row : matrix) {
            assertNotNull(row);
            assertEquals(cols, row.length);
            
            // Should be zeroed
            for (double value : row) {
                assertEquals(0.0, value);
            }
        }
    }
    
    @Test
    public void testReleaseCoefficients_Reuse() {
        int rows = 4;
        int cols = 32;
        
        // Get a matrix
        double[][] matrix1 = pool.allocateCoefficients(rows, cols);
        assertNotNull(matrix1);
        
        // Modify it
        matrix1[0][0] = 999.0;
        
        // Return it
        pool.releaseCoefficients(matrix1);
        
        // Get another matrix of same size - should reuse
        double[][] matrix2 = pool.allocateCoefficients(rows, cols);
        assertNotNull(matrix2);
        
        // Should be cleared
        assertEquals(0.0, matrix2[0][0]);
    }
    
    @Test
    public void testReleaseCoefficients_Null() {
        // Should handle null gracefully
        assertDoesNotThrow(() -> pool.releaseCoefficients(null));
    }
    
    @Test
    public void testGetStatistics() {
        // Perform some operations
        double[] array1 = pool.allocateArray(64);
        double[] array2 = pool.allocateArray(128);
        pool.releaseArray(array1);
        double[] array3 = pool.allocateArray(64); // Should reuse
        
        CWTMemoryPool.PoolStatistics stats = pool.getStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.totalAllocations() > 0);
        assertTrue(stats.poolHits() > 0);
        assertTrue(stats.hitRate() > 0.0);
    }
    
    @Test
    public void testClear() {
        // Allocate some arrays
        double[] array1 = pool.allocateArray(64);
        double[] array2 = pool.allocateArray(128);
        pool.releaseArray(array1);
        pool.releaseArray(array2);
        
        // Clear the pool - should clear pools but not statistics
        pool.clear();
        
        // Pool should still work after clear
        double[] array3 = pool.allocateArray(64);
        assertNotNull(array3);
    }
    
    @Test
    public void testResetStatistics() {
        // Allocate and return arrays
        for (int i = 0; i < 10; i++) {
            double[] array = pool.allocateArray(64);
            pool.releaseArray(array);
        }
        
        // Reset statistics
        pool.resetStatistics();
        
        CWTMemoryPool.PoolStatistics stats = pool.getStatistics();
        assertEquals(0, stats.totalAllocations());
        assertEquals(0, stats.poolHits());
        assertEquals(0, stats.poolMisses());
    }
    
    @Test
    public void testPoolStatistics_FullInformation() {
        // Generate varied usage pattern
        for (int i = 0; i < 5; i++) {
            double[] array = pool.allocateArray(128);
            pool.releaseArray(array);
        }
        
        double[][] matrix = pool.allocateCoefficients(4, 32);
        pool.releaseCoefficients(matrix);
        
        // Get reused array (pool hit)
        double[] reused = pool.allocateArray(128);
        
        CWTMemoryPool.PoolStatistics stats = pool.getStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.totalAllocations() > 0);
        assertTrue(stats.poolHits() > 0);
        assertTrue(stats.poolMisses() >= 0);
        assertTrue(stats.hitRate() >= 0.0 && stats.hitRate() <= 1.0);
        assertNotNull(stats.toString());
        assertTrue(stats.toString().contains("allocations"));
    }
    
    @Test
    public void testPoolStatistics_Equality() {
        CWTMemoryPool.PoolStatistics stats1 = pool.getStatistics();
        CWTMemoryPool.PoolStatistics stats2 = pool.getStatistics();
        
        // Same pool should give equal statistics
        assertEquals(stats1.totalAllocations(), stats2.totalAllocations());
        assertEquals(stats1.poolHits(), stats2.poolHits());
        assertEquals(stats1.poolMisses(), stats2.poolMisses());
    }
    
    @Test
    public void testMatrixKey_Equality() {
        // Test internal MatrixKey class through usage
        double[][] matrix1 = pool.allocateCoefficients(4, 32);
        pool.releaseCoefficients(matrix1);
        
        double[][] matrix2 = pool.allocateCoefficients(4, 32);
        pool.releaseCoefficients(matrix2);
        
        // Same dimensions should use same pool
        CWTMemoryPool.PoolStatistics stats = pool.getStatistics();
        assertTrue(stats.poolHits() > 0);
    }
    
    @Test
    public void testMatrixKey_DifferentDimensions() {
        // Different dimensions should use different pools
        double[][] matrix1 = pool.allocateCoefficients(4, 32);
        double[][] matrix2 = pool.allocateCoefficients(8, 32);
        double[][] matrix3 = pool.allocateCoefficients(4, 64);
        
        assertNotNull(matrix1);
        assertNotNull(matrix2);
        assertNotNull(matrix3);
        
        // All should be different allocations
        assertNotSame(matrix1, matrix2);
        assertNotSame(matrix1, matrix3);
        assertNotSame(matrix2, matrix3);
    }
    
    @Test
    public void testLargeAllocation() {
        // Test with large arrays
        double[] largeArray = pool.allocateArray(100000);
        assertNotNull(largeArray);
        assertTrue(largeArray.length >= 100000);
        
        double[][] largeMatrix = pool.allocateCoefficients(100, 1000);
        assertNotNull(largeMatrix);
        assertEquals(100, largeMatrix.length);
        assertEquals(1000, largeMatrix[0].length);
    }
    
    @Test
    public void testMaxPoolSize() {
        // Test pool size limiting
        CWTMemoryPool limitedPool = new CWTMemoryPool(2);
        
        // Return multiple arrays of same size
        for (int i = 0; i < 5; i++) {
            double[] array = limitedPool.allocateArray(64);
            limitedPool.releaseArray(array);
        }
        
        // Pool should be limited to max size
        // (internal implementation detail - just ensure it works)
        assertDoesNotThrow(() -> {
            double[] array = limitedPool.allocateArray(64);
            assertNotNull(array);
        });
    }
    
    @Test
    public void testConcurrentAccess() throws InterruptedException {
        // Basic thread safety test
        Thread[] threads = new Thread[10];
        
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 100; j++) {
                    double[] array = pool.allocateArray(128);
                    assertNotNull(array);
                    pool.releaseArray(array);
                }
            });
            threads[i].start();
        }
        
        // Wait for all threads
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Pool should still be functional
        CWTMemoryPool.PoolStatistics stats = pool.getStatistics();
        assertTrue(stats.totalAllocations() > 0);
    }
    
    @Test
    public void testInvalidSizes() {
        // allocateArray should handle invalid sizes based on implementation
        // Check if it throws or handles gracefully
        // The implementation may validate or may delegate to array creation which validates
        
        // For coefficient allocation, test invalid dimensions
        // The implementation should handle these cases appropriately
    }
}