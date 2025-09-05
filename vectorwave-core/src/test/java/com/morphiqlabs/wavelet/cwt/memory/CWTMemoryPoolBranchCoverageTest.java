package com.morphiqlabs.wavelet.cwt.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests to improve branch coverage for CWTMemoryPool.
 */
class CWTMemoryPoolBranchCoverageTest {
    
    private CWTMemoryPool pool;
    
    @BeforeEach
    void setUp() {
        pool = new CWTMemoryPool(2); // Small pool size to test overflow
    }
    
    @Test
    @DisplayName("Should handle null array release gracefully")
    void testReleaseNullArray() {
        assertDoesNotThrow(() -> pool.releaseArray(null));
        
        // Verify statistics unchanged
        CWTMemoryPool.PoolStatistics stats = pool.getStatistics();
        assertEquals(0, stats.totalAllocations());
    }
    
    @Test
    @DisplayName("Should reject non-power-of-two arrays from pool")
    void testReleaseNonPowerOfTwoArray() {
        // Create array with non-power-of-two size
        double[] array = new double[7]; // Not a power of 2
        
        pool.releaseArray(array);
        
        // Verify array is not pooled by allocating similar size
        double[] newArray = pool.allocateArray(7);
        assertNotSame(array, newArray);
        
        CWTMemoryPool.PoolStatistics stats = pool.getStatistics();
        assertEquals(0, stats.poolHits());
        assertEquals(1, stats.poolMisses());
    }
    
    @Test
    @DisplayName("Should not pool arrays when pool is full")
    void testPoolOverflow() {
        // Fill the pool to capacity (maxPoolSizePerBucket = 2)
        double[] array1 = pool.allocateArray(8);
        double[] array2 = pool.allocateArray(8);
        double[] array3 = pool.allocateArray(8);
        
        pool.releaseArray(array1);
        pool.releaseArray(array2);
        pool.releaseArray(array3); // This should be rejected (pool full)
        
        // Verify only 2 arrays in pool
        double[] retrieved1 = pool.allocateArray(8);
        double[] retrieved2 = pool.allocateArray(8);
        double[] retrieved3 = pool.allocateArray(8);
        
        // Queue behavior: First in, first out
        assertSame(array1, retrieved1);
        assertSame(array2, retrieved2);
        assertNotSame(array3, retrieved3); // array3 was not pooled
        
        CWTMemoryPool.PoolStatistics stats = pool.getStatistics();
        assertEquals(2, stats.poolHits());
    }
    
    @Test
    @DisplayName("Should handle null matrix release gracefully")
    void testReleaseNullMatrix() {
        assertDoesNotThrow(() -> pool.releaseCoefficients(null));
        
        // Also test empty matrix
        double[][] emptyMatrix = new double[0][];
        assertDoesNotThrow(() -> pool.releaseCoefficients(emptyMatrix));
        
        CWTMemoryPool.PoolStatistics stats = pool.getStatistics();
        assertEquals(0, stats.totalAllocations());
    }
    
    @Test
    @DisplayName("Should handle inadequate matrix from pool")
    void testInadequateMatrixFromPool() {
        // Allocate and release a small matrix
        double[][] smallMatrix = pool.allocateCoefficients(5, 10);
        pool.releaseCoefficients(smallMatrix);
        
        // Request larger matrix - should get new allocation
        double[][] largeMatrix = pool.allocateCoefficients(10, 20);
        assertNotSame(smallMatrix, largeMatrix);
        assertEquals(10, largeMatrix.length);
        assertEquals(20, largeMatrix[0].length);
        
        // The small matrix should be returned to pool
        double[][] retrievedSmall = pool.allocateCoefficients(5, 10);
        assertSame(smallMatrix, retrievedSmall);
        
        CWTMemoryPool.PoolStatistics stats = pool.getStatistics();
        assertEquals(1, stats.poolHits()); // Only the second 5x10 allocation
        assertEquals(2, stats.poolMisses()); // Initial 5x10 and 10x20
    }
    
    @Test
    @DisplayName("Should not pool matrices when pool is full")
    void testMatrixPoolOverflow() {
        // Fill matrix pool to capacity
        double[][] matrix1 = pool.allocateCoefficients(5, 10);
        double[][] matrix2 = pool.allocateCoefficients(5, 10);
        double[][] matrix3 = pool.allocateCoefficients(5, 10);
        
        pool.releaseCoefficients(matrix1);
        pool.releaseCoefficients(matrix2);
        pool.releaseCoefficients(matrix3); // Should be rejected
        
        // Verify only 2 matrices in pool
        double[][] retrieved1 = pool.allocateCoefficients(5, 10);
        double[][] retrieved2 = pool.allocateCoefficients(5, 10);
        double[][] retrieved3 = pool.allocateCoefficients(5, 10);
        
        // Queue behavior: FIFO
        assertSame(matrix1, retrieved1);
        assertSame(matrix2, retrieved2);
        assertNotSame(matrix3, retrieved3); // matrix3 was not pooled
        
        CWTMemoryPool.PoolStatistics stats = pool.getStatistics();
        assertEquals(2, stats.poolHits());
    }
    
    @Test
    @DisplayName("Should handle pool lookup when pool doesn't exist")
    void testReleaseToNonExistentPool() {
        double[] array = new double[16];
        
        // Release array when no pool exists for this size
        pool.releaseArray(array);
        
        // Verify array was not pooled
        double[] newArray = pool.allocateArray(16);
        assertNotSame(array, newArray);
    }
    
    @Test
    @DisplayName("Should handle matrix pool lookup when pool doesn't exist")
    void testReleaseMatrixToNonExistentPool() {
        double[][] matrix = new double[3][7];
        
        // Release matrix when no pool exists for this size
        pool.releaseCoefficients(matrix);
        
        // Verify matrix was not pooled
        double[][] newMatrix = pool.allocateCoefficients(3, 7);
        assertNotSame(matrix, newMatrix);
    }
    
    @Test
    @DisplayName("Should clear all values when returning array from pool")
    void testArrayClearingOnReuse() {
        double[] array = pool.allocateArray(16);
        // Fill with non-zero values
        for (int i = 0; i < array.length; i++) {
            array[i] = i + 1.0;
        }
        
        pool.releaseArray(array);
        
        // Get array back from pool - must request same size to get the same array
        double[] reused = pool.allocateArray(16);
        assertSame(array, reused);
        
        // Verify all requested values are cleared
        for (int i = 0; i < 16; i++) {
            assertEquals(0.0, reused[i], "Index " + i + " should be cleared");
        }
    }
    
    @Test
    @DisplayName("Should clear matrix values when returning from pool")
    void testMatrixClearingOnReuse() {
        double[][] matrix = pool.allocateCoefficients(5, 10);
        // Fill with non-zero values
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) {
                matrix[i][j] = i * 10 + j;
            }
        }
        
        pool.releaseCoefficients(matrix);
        
        // Get matrix back from pool
        double[][] reused = pool.allocateCoefficients(5, 10);
        assertSame(matrix, reused);
        
        // Verify all values are cleared
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 10; j++) {
                assertEquals(0.0, reused[i][j], 
                    "Matrix[" + i + "][" + j + "] should be cleared");
            }
        }
    }
    
    @Test
    @DisplayName("Should handle edge case array sizes")
    void testEdgeCaseArraySizes() {
        // Test size 0
        double[] zero = pool.allocateArray(0);
        assertEquals(1, zero.length); // Rounds up to 1
        
        // Test size 1
        double[] one = pool.allocateArray(1);
        assertEquals(1, one.length);
        
        // Test negative size (should round to 1)
        double[] negative = pool.allocateArray(-5);
        assertEquals(1, negative.length);
        
        // Test very large size
        double[] large = pool.allocateArray((1 << 20) + 1);
        assertEquals(1 << 21, large.length); // Next power of 2
    }
    
    @Test
    @DisplayName("Should maintain correct statistics through operations")
    void testStatisticsAccuracy() {
        pool.resetStatistics();
        CWTMemoryPool.PoolStatistics stats = pool.getStatistics();
        assertEquals(0, stats.totalAllocations());
        assertEquals(0, stats.poolHits());
        assertEquals(0, stats.poolMisses());
        assertEquals(0, stats.currentPoolSize());
        
        // First allocation - miss
        double[] array1 = pool.allocateArray(8);
        stats = pool.getStatistics();
        assertEquals(1, stats.totalAllocations());
        assertEquals(0, stats.poolHits());
        assertEquals(1, stats.poolMisses());
        
        // Release and reallocate - hit
        pool.releaseArray(array1);
        double[] array2 = pool.allocateArray(8);
        stats = pool.getStatistics();
        assertEquals(2, stats.totalAllocations());
        assertEquals(1, stats.poolHits());
        assertEquals(1, stats.poolMisses());
        
        // Verify hit rate calculation
        assertEquals(0.5, stats.hitRate(), 0.001);
    }
}