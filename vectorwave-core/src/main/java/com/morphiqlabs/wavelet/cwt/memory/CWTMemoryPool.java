package com.morphiqlabs.wavelet.cwt.memory;

import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import com.morphiqlabs.wavelet.util.ValidationUtils;
import java.util.function.IntFunction;
import com.morphiqlabs.wavelet.cwt.MemoryPool;

/**
 * Memory pool for CWT operations to reduce allocation overhead.
 * 
 * <p>Provides efficient array allocation and reuse for repeated CWT
 * computations, particularly beneficial for real-time processing.</p>
 */
public final class CWTMemoryPool implements MemoryPool {
    
    // Pool organized by array size (power of 2)
    private final ConcurrentHashMap<Integer, Queue<double[]>> arrayPools;
    private final ConcurrentHashMap<MatrixKey, Queue<double[][]>> matrixPools;
    
    /**
     * Key for matrix pools to avoid collision issues with large dimensions.
     */
    private record MatrixKey(int rows, int cols) {
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj instanceof MatrixKey other) {
                return rows == other.rows && cols == other.cols;
            }
            return false;
        }
        
        @Override
        public int hashCode() {
            // Use Objects.hash for better distribution with large values
            return Objects.hash(rows, cols);
        }
    }
    
    // Statistics
    private final AtomicLong totalAllocations = new AtomicLong(0);
    private final AtomicLong poolHits = new AtomicLong(0);
    private final AtomicLong poolMisses = new AtomicLong(0);
    
    // Configuration
    private final int maxPoolSizePerBucket;
    private final IntFunction<double[]> arrayFactory;
    
    /**
     * Creates a memory pool with default settings.
     */
    public CWTMemoryPool() {
        this(16); // Default max 16 arrays per size bucket
    }
    
    /**
     * Creates a memory pool with specified max pool size.
     * 
     * @param maxPoolSizePerBucket maximum arrays to keep per size bucket
     */
    public CWTMemoryPool(int maxPoolSizePerBucket) {
        this(maxPoolSizePerBucket, double[]::new);
    }
    
    /**
     * Creates a memory pool with custom array factory.
     * 
     * @param arrayFactory factory for creating arrays
     */
    public CWTMemoryPool(IntFunction<double[]> arrayFactory) {
        this(16, arrayFactory);
    }
    
    /**
     * Creates a memory pool with specified settings.
     * 
     * @param maxPoolSizePerBucket maximum arrays to keep per size bucket
     * @param arrayFactory factory for creating arrays
     */
    public CWTMemoryPool(int maxPoolSizePerBucket, IntFunction<double[]> arrayFactory) {
        this.maxPoolSizePerBucket = maxPoolSizePerBucket;
        this.arrayFactory = arrayFactory;
        this.arrayPools = new ConcurrentHashMap<>();
        this.matrixPools = new ConcurrentHashMap<>();
    }
    
    /**
     * Allocates a double array of at least the specified size.
     * 
     * @param size minimum size needed
     * @return array from pool or newly allocated
     */
    public double[] allocateArray(int size) {
        totalAllocations.incrementAndGet();
        
        // Round up to next power of 2 for better pooling
        int poolSize = nextPowerOfTwo(size);
        
        // Try to get from pool
        Queue<double[]> pool = arrayPools.computeIfAbsent(poolSize, 
            k -> new ConcurrentLinkedQueue<>());
        
        double[] array = pool.poll();
        if (array != null) {
            poolHits.incrementAndGet();
            // Clear only the requested portion - array.length is always >= size
            Arrays.fill(array, 0, size, 0.0);
            return array;
        }
        
        // Allocate new array
        poolMisses.incrementAndGet();
        return arrayFactory.apply(poolSize);
    }
    
    /**
     * Releases an array back to the pool.
     * 
     * @param array array to release
     */
    public void releaseArray(double[] array) {
        if (array == null) return;
        
        int size = array.length;
        // Only pool power-of-2 sizes
        if (!ValidationUtils.isPowerOfTwo(size)) return;
        
        Queue<double[]> pool = arrayPools.get(size);
        if (pool != null && pool.size() < maxPoolSizePerBucket) {
            pool.offer(array);
        }
        // Otherwise let it be garbage collected
    }
    
    /**
     * Allocates a 2D coefficient array.
     * 
     * @param rows number of rows (scales)
     * @param cols number of columns (time points)
     * @return 2D array from pool or newly allocated
     */
    @Override
    public double[][] allocateCoefficients(int rows, int cols) {
        totalAllocations.incrementAndGet();
        
        // Use MatrixKey to handle large dimensions correctly
        MatrixKey key = new MatrixKey(rows, cols);
        
        Queue<double[][]> pool = matrixPools.computeIfAbsent(key,
            k -> new ConcurrentLinkedQueue<>());
        
        double[][] matrix = pool.poll();
        if (matrix != null && matrix.length >= rows && matrix[0].length >= cols) {
            poolHits.incrementAndGet();
            // Clear the matrix
            for (int i = 0; i < rows; i++) {
                Arrays.fill(matrix[i], 0, cols, 0.0);
            }
            return matrix;
        }
        
        // Allocate new matrix
        poolMisses.incrementAndGet();
        if (matrix != null) {
            // Return inadequate matrix to pool
            pool.offer(matrix);
        }
        
        double[][] newMatrix = new double[rows][];
        for (int i = 0; i < rows; i++) {
            // Allocate exact size needed - don't round up to power of 2 for matrix rows
            // Rationale: CWT coefficient matrices have dimensions determined by:
            // - rows = number of scales (typically 10-100, rarely power of 2)
            // - cols = signal length (often already power of 2)
            // Rounding up columns would waste significant memory (e.g., 1000→1024 = 2.4% waste per row)
            // across many rows, without cache line benefits since matrix access patterns in CWT
            // are typically row-wise (scale-by-scale) rather than column-wise
            newMatrix[i] = new double[cols];
        }
        return newMatrix;
    }
    
    /**
     * Releases a coefficient matrix back to the pool.
     * 
     * @param matrix matrix to release
     */
    public void releaseCoefficients(double[][] matrix) {
        if (matrix == null || matrix.length == 0) return;
        
        int rows = matrix.length;
        int cols = matrix[0].length;
        MatrixKey key = new MatrixKey(rows, cols);
        
        Queue<double[][]> pool = matrixPools.get(key);
        if (pool != null && pool.size() < maxPoolSizePerBucket) {
            pool.offer(matrix);
        }
        // Don't release individual arrays since they weren't allocated via allocateArray
    }
    
    /**
     * Clears all arrays from the pool.
     */
    public void clear() {
        arrayPools.clear();
        matrixPools.clear();
    }
    
    /**
     * Gets pool statistics.
     * 
     * @return current statistics
     */
    public PoolStatistics getStatistics() {
        long allocations = totalAllocations.get();
        long hits = poolHits.get();
        long misses = poolMisses.get();
        
        int poolSize = arrayPools.values().stream()
            .mapToInt(Queue::size)
            .sum();
        poolSize += matrixPools.values().stream()
            .mapToInt(Queue::size)
            .sum();
        
        return new PoolStatistics(allocations, hits, misses, poolSize);
    }
    
    /**
     * Resets statistics counters.
     */
    public void resetStatistics() {
        totalAllocations.set(0);
        poolHits.set(0);
        poolMisses.set(0);
    }
    
    // Utility methods
    
    private static int nextPowerOfTwo(int n) {
        if (n <= 1) return 1;
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return n + 1;
    }
    
    /**
     * Pool statistics.
     *
     * @param totalAllocations Total number of allocations served (hits + misses)
     * @param poolHits Number of allocations served from the pool
     * @param poolMisses Number of allocations requiring new allocation
     * @param currentPoolSize Current number of pooled objects
     */
    public record PoolStatistics(
        long totalAllocations,
        long poolHits,
        long poolMisses,
        int currentPoolSize
    ) {
        public double hitRate() {
            return totalAllocations > 0 ? 
                (double) poolHits / totalAllocations : 0.0;
        }
        
        @Override
        public String toString() {
            return String.format(
                "PoolStats[allocations=%d, hits=%d (%.1f%%), misses=%d, poolSize=%d]",
                totalAllocations, poolHits, hitRate() * 100, poolMisses, currentPoolSize
            );
        }
    }
}
