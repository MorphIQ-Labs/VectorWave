package com.morphiqlabs.wavelet.internal;

import com.morphiqlabs.wavelet.util.ThreadLocalManager;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Thread-safe object pool for reusable double arrays.
 * Optimized for small signal processing to reduce GC pressure.
 *
 * <p>This pool maintains arrays of common power-of-2 sizes used in
 * wavelet transforms. Arrays are borrowed and returned to avoid
 * frequent allocations during batch processing.</p>
 */
final class ArrayPool {

    // Common signal sizes for financial time series
    private static final int[] POOL_SIZES = {32, 64, 128, 256, 512, 1024};
    private static final int MAX_ARRAYS_PER_SIZE = 4; // Per thread

    // Thread-local pools to avoid contention - managed by ThreadLocalManager
    private static final ThreadLocalManager.ManagedThreadLocal<Pool> THREAD_LOCAL_POOL =
            ThreadLocalManager.withInitial(Pool::new);

    private ArrayPool() {
        // Prevent instantiation
    }

    /**
     * Borrows an array of the specified size.
     * If no pooled array is available, allocates a new one.
     *
     * @param size the required array size
     * @return a double array of the specified size
     */
    public static double[] borrow(int size) {
        return THREAD_LOCAL_POOL.get().borrow(size);
    }

    /**
     * Returns an array to the pool for reuse.
     * The array is cleared before being pooled.
     *
     * @param array the array to return
     */
    public static void release(double[] array) {
        if (array != null) {
            THREAD_LOCAL_POOL.get().release(array);
        }
    }

    /**
     * Clears all arrays from the current thread's pool.
     * Useful for explicit memory management.
     */
    public static void clear() {
        THREAD_LOCAL_POOL.get().clear();
    }

    /**
     * Thread-local pool implementation.
     */
    private static class Pool {
        @SuppressWarnings("unchecked")
        private final ConcurrentLinkedDeque<double[]>[] pools;

        @SuppressWarnings({"unchecked", "rawtypes"})
        Pool() {
            pools = new ConcurrentLinkedDeque[POOL_SIZES.length];
            for (int i = 0; i < POOL_SIZES.length; i++) {
                pools[i] = new ConcurrentLinkedDeque<double[]>();
            }
        }

        double[] borrow(int size) {
            int poolIndex = getPoolIndex(size);

            if (poolIndex >= 0) {
                double[] array = pools[poolIndex].poll();
                if (array != null) {
                    return array;
                }
            }

            // No pooled array available, allocate new
            return new double[size];
        }

        void release(double[] array) {
            if (array == null) return;

            int poolIndex = getPoolIndex(array.length);

            if (poolIndex >= 0) {
                // Clear the array before pooling
                java.util.Arrays.fill(array, 0.0);

                // Only pool if we haven't exceeded the limit
                if (pools[poolIndex].size() < MAX_ARRAYS_PER_SIZE) {
                    pools[poolIndex].offer(array);
                }
            }
            // Arrays of non-standard sizes are not pooled
        }

        void clear() {
            for (ConcurrentLinkedDeque<double[]> pool : pools) {
                pool.clear();
            }
        }

        private int getPoolIndex(int size) {
            for (int i = 0; i < POOL_SIZES.length; i++) {
                if (POOL_SIZES[i] == size) {
                    return i;
                }
            }
            return -1;
        }
    }
}