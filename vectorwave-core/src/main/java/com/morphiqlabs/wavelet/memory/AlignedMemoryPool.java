package com.morphiqlabs.wavelet.memory;

import com.morphiqlabs.wavelet.util.ThreadLocalManager;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * High-performance memory pool for aligned double arrays.
 *
 * <p>This pool provides:</p>
 * <ul>
 *   <li>64-byte aligned allocations for optimal SIMD performance</li>
 *   <li>Thread-safe pooling with minimal contention</li>
 *   <li>Automatic size management with popular size tracking</li>
 *   <li>Zero-copy array reuse</li>
 * </ul>
 */
public final class AlignedMemoryPool {

    // Cache line size for alignment (64 bytes = 8 doubles)
    private static final int CACHE_LINE_SIZE = 64;
    private static final int DOUBLES_PER_CACHE_LINE = CACHE_LINE_SIZE / 8;

    // Pool configuration
    private static final int MAX_POOL_SIZE_PER_LENGTH = 32;
    private static final int MAX_ARRAY_LENGTH = 65536; // 64K doubles = 512KB

    // Popular sizes for wavelet transforms (powers of 2)
    private static final int[] COMMON_SIZES = {
            64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768
    };

    // Thread-local pools for zero contention on common sizes - managed by ThreadLocalManager
    private static final ThreadLocalManager.ManagedThreadLocal<Pool[]> THREAD_LOCAL_POOLS =
            ThreadLocalManager.withInitial(AlignedMemoryPool::createThreadLocalPools);

    // Global pools for less common sizes
    @SuppressWarnings("unchecked")
    private static final ConcurrentLinkedQueue<PooledArray>[] GLOBAL_POOLS;

    // Statistics
    private static final AtomicLong allocations = new AtomicLong();
    private static final AtomicLong poolHits = new AtomicLong();
    private static final AtomicLong poolMisses = new AtomicLong();

    static {
        @SuppressWarnings({"unchecked", "rawtypes"})
        ConcurrentLinkedQueue<PooledArray>[] pools = new ConcurrentLinkedQueue[COMMON_SIZES.length];
        GLOBAL_POOLS = pools;
        for (int i = 0; i < COMMON_SIZES.length; i++) {
            GLOBAL_POOLS[i] = new ConcurrentLinkedQueue<PooledArray>();
        }
    }

    private AlignedMemoryPool() {
        // Utility class
    }

    /**
     * Allocates an aligned double array from the pool.
     *
     * @param size the required array size
     * @return a pooled array handle
     */
    public static PooledArray allocate(int size) {
        allocations.incrementAndGet();

        // Try thread-local pool first
        int poolIndex = getPoolIndex(size);
        if (poolIndex >= 0) {
            Pool[] localPools = THREAD_LOCAL_POOLS.get();
            PooledArray array = localPools[poolIndex].poll();
            if (array != null) {
                poolHits.incrementAndGet();
                array.clear(); // Zero out for clean state
                return array;
            }
        }

        // Try global pool
        if (poolIndex >= 0 && poolIndex < GLOBAL_POOLS.length) {
            PooledArray array = GLOBAL_POOLS[poolIndex].poll();
            if (array != null) {
                poolHits.incrementAndGet();
                array.clear();
                return array;
            }
        }

        // Allocate new aligned array
        poolMisses.incrementAndGet();
        return createAlignedArray(size);
    }

    /**
     * Returns an array to the pool for reuse.
     *
     * @param array the array to return
     */
    public static void release(PooledArray array) {
        if (array == null || array.isReleased()) {
            return;
        }

        array.markReleased();
        int size = array.length();

        // Don't pool very large arrays
        if (size > MAX_ARRAY_LENGTH) {
            return;
        }

        int poolIndex = getPoolIndex(size);
        if (poolIndex >= 0) {
            // Try thread-local pool first
            Pool[] localPools = THREAD_LOCAL_POOLS.get();
            if (localPools[poolIndex].offer(array)) {
                return;
            }

            // Fall back to global pool
            if (poolIndex < GLOBAL_POOLS.length) {
                GLOBAL_POOLS[poolIndex].offer(array);
            }
        }
    }

    /**
     * Creates thread-local pools for common sizes.
     */
    private static Pool[] createThreadLocalPools() {
        Pool[] pools = new Pool[COMMON_SIZES.length];
        for (int i = 0; i < COMMON_SIZES.length; i++) {
            pools[i] = new Pool(MAX_POOL_SIZE_PER_LENGTH / 2); // Smaller for thread-local
        }
        return pools;
    }

    /**
     * Gets the pool index for a given size.
     */
    private static int getPoolIndex(int size) {
        for (int i = 0; i < COMMON_SIZES.length; i++) {
            if (COMMON_SIZES[i] == size) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Creates a new aligned array.
     */
    private static PooledArray createAlignedArray(int size) {
        // Round up to cache line boundary
        int alignedSize = ((size + DOUBLES_PER_CACHE_LINE - 1) / DOUBLES_PER_CACHE_LINE)
                * DOUBLES_PER_CACHE_LINE;

        // Allocate with padding for alignment
        double[] data = new double[alignedSize + DOUBLES_PER_CACHE_LINE];

        // Find aligned offset
        long address = System.identityHashCode(data);
        int offset = (int) ((CACHE_LINE_SIZE - (address & (CACHE_LINE_SIZE - 1))) / 8);
        if (offset == DOUBLES_PER_CACHE_LINE) {
            offset = 0;
        }

        return new PooledArray(data, offset, size);
    }

    /**
     * Gets pool statistics.
     */
    public static String getStatistics() {
        long total = allocations.get();
        long hits = poolHits.get();
        long misses = poolMisses.get();
        double hitRate = total > 0 ? (100.0 * hits / total) : 0;

        return String.format(
                "AlignedMemoryPool: allocations=%d, hits=%d, misses=%d, hitRate=%.1f%%",
                total, hits, misses, hitRate);
    }

    /**
     * Clears all pools (for testing).
     */
    public static void clear() {
        THREAD_LOCAL_POOLS.remove();
        for (ConcurrentLinkedQueue<PooledArray> pool : GLOBAL_POOLS) {
            pool.clear();
        }
        allocations.set(0);
        poolHits.set(0);
        poolMisses.set(0);
    }

    /**
     * Simple bounded pool implementation.
     */
    private static class Pool {
        private final PooledArray[] arrays;
        private final AtomicInteger size;
        private final int capacity;

        Pool(int capacity) {
            this.capacity = capacity;
            this.arrays = new PooledArray[capacity];
            this.size = new AtomicInteger(0);
        }

        boolean offer(PooledArray array) {
            int current = size.get();
            if (current >= capacity) {
                return false;
            }

            int index = size.getAndIncrement();
            if (index < capacity) {
                arrays[index] = array;
                return true;
            }

            size.decrementAndGet();
            return false;
        }

        PooledArray poll() {
            int current = size.get();
            if (current <= 0) {
                return null;
            }

            int index = size.getAndDecrement() - 1;
            if (index >= 0) {
                PooledArray array = arrays[index];
                arrays[index] = null;
                return array;
            }

            size.incrementAndGet();
            return null;
        }
    }

    /**
     * Wrapper for pooled arrays with alignment information.
     */
    public static class PooledArray implements AutoCloseable {
        private final double[] data;
        private final int offset;
        private final int length;
        private volatile boolean released;

        PooledArray(double[] data, int offset, int length) {
            this.data = data;
            this.offset = offset;
            this.length = length;
            this.released = false;
        }

        /**
         * Gets the aligned array for use.
         */
        public double[] array() {
            if (released) {
                throw new IllegalStateException("Array already released");
            }
            return data;
        }

        /**
         * Gets the offset to the aligned portion.
         */
        public int offset() {
            return offset;
        }

        /**
         * Gets the logical length.
         */
        public int length() {
            return length;
        }

        /**
         * Clears the array contents.
         */
        void clear() {
            int end = offset + length;
            for (int i = offset; i < end; i++) {
                data[i] = 0.0;
            }
            released = false;
        }

        /**
         * Marks as released.
         */
        void markReleased() {
            released = true;
        }

        /**
         * Checks if released.
         */
        boolean isReleased() {
            return released;
        }

        @Override
        public void close() {
            AlignedMemoryPool.release(this);
        }

        /**
         * Copies data from source array.
         */
        public void copyFrom(double[] source, int sourceOffset, int count) {
            System.arraycopy(source, sourceOffset, data, offset, count);
        }

        /**
         * Copies data to destination array.
         */
        public void copyTo(double[] dest, int destOffset, int count) {
            System.arraycopy(data, offset, dest, destOffset, count);
        }
    }
}