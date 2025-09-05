package com.morphiqlabs.wavelet.util;

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Centralized management for ThreadLocal instances to prevent memory leaks.
 * 
 * <p>This class provides lifecycle management for ThreadLocal variables used
 * throughout the VectorWave library. It ensures proper cleanup in thread pool
 * environments where threads are reused.</p>
 * 
 * <p>Key features:</p>
 * <ul>
 *   <li>Automatic registration of ThreadLocal instances</li>
 *   <li>Cleanup all registered ThreadLocals for current thread</li>
 *   <li>Try-with-resources support for scoped cleanup</li>
 *   <li>Memory leak detection and warnings</li>
 * </ul>
 */
public final class ThreadLocalManager {
    private ThreadLocalManager() { throw new AssertionError("No instances"); }
    
    private static final System.Logger LOG = com.morphiqlabs.wavelet.util.Logging.getLogger(ThreadLocalManager.class);
    
    // Track all managed ThreadLocal instances
    private static final Set<WeakReference<ManagedThreadLocal<?>>> REGISTERED_LOCALS = 
        ConcurrentHashMap.newKeySet();
    
    // Flag to enable memory leak detection
    private static volatile boolean LEAK_DETECTION_ENABLED = 
        Boolean.parseBoolean(System.getProperty("vectorwave.threadlocal.leak.detection", "true"));
    
    // Thread-local flag to track if cleanup has been performed
    private static final ThreadLocal<Boolean> CLEANUP_PERFORMED = 
        ThreadLocal.withInitial(() -> false);
    
    /**
     * Creates a managed ThreadLocal that automatically registers for cleanup.
     * 
     * @param <T> The type of the thread-local value
     * @param supplier Supplier for initial values
     * @return A managed ThreadLocal instance
     */
    public static <T> ManagedThreadLocal<T> withInitial(Supplier<T> supplier) {
        ManagedThreadLocal<T> local = new ManagedThreadLocal<>(supplier);
        REGISTERED_LOCALS.add(new WeakReference<>(local));
        return local;
    }
    
    /**
     * Cleans up all registered ThreadLocal instances for the current thread.
     * This should be called when a thread is done processing to prevent memory leaks.
     */
    public static void cleanupCurrentThread() {
        int cleanedCount = 0;
        
        // Clean up all registered ThreadLocals
        var iterator = REGISTERED_LOCALS.iterator();
        while (iterator.hasNext()) {
            WeakReference<ManagedThreadLocal<?>> ref = iterator.next();
            ManagedThreadLocal<?> local = ref.get();
            
            if (local == null) {
                // WeakReference has been garbage collected
                iterator.remove();
            } else {
                local.removeForCurrentThread();
                cleanedCount++;
            }
        }
        
        CLEANUP_PERFORMED.set(true);
        
        if (LOG.isLoggable(System.Logger.Level.DEBUG)) {
            LOG.log(System.Logger.Level.DEBUG, "Cleaned up " + cleanedCount +
                    " ThreadLocal instances for thread: " + Thread.currentThread().getName());
        }
    }
    
    /**
     * Checks if cleanup has been performed for the current thread.
     * 
     * @return true if cleanup was performed
     */
    public static boolean isCleanupPerformed() {
        return CLEANUP_PERFORMED.get();
    }
    
    /**
     * Resets the cleanup flag for the current thread.
     * Useful for thread pool scenarios where threads are reused.
     */
    public static void resetCleanupFlag() {
        CLEANUP_PERFORMED.set(false);
    }
    
    /**
     * Creates a cleanup scope that automatically cleans up ThreadLocals on close.
     * 
     * @return A CleanupScope for use with try-with-resources
     */
    public static CleanupScope createScope() {
        return new CleanupScope();
    }
    
    /**
     * Registers an existing ThreadLocal for management.
     * Useful for integrating with legacy code.
     * 
     * @param threadLocal The ThreadLocal to manage
     */
    public static void register(ThreadLocal<?> threadLocal) {
        if (threadLocal instanceof ManagedThreadLocal) {
            REGISTERED_LOCALS.add(new WeakReference<>((ManagedThreadLocal<?>) threadLocal));
        } else {
            // Wrap in a managed proxy
            ManagedThreadLocal<?> wrapper = new ManagedThreadLocal<Object>(() -> null) {
                @Override
                public void removeForCurrentThread() {
                    threadLocal.remove();
                }
            };
            REGISTERED_LOCALS.add(new WeakReference<>(wrapper));
        }
    }
    
    /**
     * Gets statistics about ThreadLocal usage.
     * 
     * @return Usage statistics
     */
    public static ThreadLocalStats getStats() {
        int registeredCount = 0;
        int activeCount = 0;
        
        for (WeakReference<ManagedThreadLocal<?>> ref : REGISTERED_LOCALS) {
            ManagedThreadLocal<?> local = ref.get();
            if (local != null) {
                registeredCount++;
                if (local.hasValueForCurrentThread()) {
                    activeCount++;
                }
            }
        }
        
        return new ThreadLocalStats(registeredCount, activeCount, CLEANUP_PERFORMED.get());
    }
    
    /**
     * Enables or disables memory leak detection.
     * 
     * @param enabled true to enable leak detection
     */
    public static void setLeakDetectionEnabled(boolean enabled) {
        LEAK_DETECTION_ENABLED = enabled;
        LOG.log(System.Logger.Level.INFO, () -> "Leak detection " + (enabled ? "enabled" : "disabled"));
    }
    
    /**
     * Checks if leak detection is currently enabled.
     * 
     * @return true if leak detection is enabled
     */
    public static boolean isLeakDetectionEnabled() {
        return LEAK_DETECTION_ENABLED;
    }
    
    /**
     * A ThreadLocal that can be explicitly managed and cleaned up.
     * 
     * @param <T> The type of the thread-local value
     */
    public static class ManagedThreadLocal<T> extends ThreadLocal<T> {
        private final Supplier<T> supplier;
        private final AtomicBoolean removed = new AtomicBoolean(false);
        
        /**
         * ThreadLocal to track whether a value has been explicitly set.
         * This avoids the side effects of calling get() to check existence.
         */
        private final ThreadLocal<Boolean> isSet = ThreadLocal.withInitial(() -> false);
        
        private ManagedThreadLocal(Supplier<T> supplier) {
            this.supplier = supplier;
        }
        
        @Override
        protected T initialValue() {
            return supplier.get();
        }
        
        @Override
        public void set(T value) {
            super.set(value);
            isSet.set(true);
        }
        
        @Override
        public T get() {
            T value = super.get();
            // If get() returns a value due to initialization, mark it as set
            if (!isSet.get() && value != null) {
                isSet.set(true);
            }
            return value;
        }
        
        /**
         * Removes the value for the current thread.
         */
        public void removeForCurrentThread() {
            super.remove();
            isSet.remove();
        }
        
        /**
         * Checks if this ThreadLocal has a value for the current thread.
         * This method has no side effects and doesn't trigger initialization.
         * 
         * @return true if a value has been set or initialized for the current thread
         */
        public boolean hasValueForCurrentThread() {
            return isSet.get();
        }
        
        @Override
        public void remove() {
            super.remove();
            isSet.remove();
            removed.set(true);
        }
        
        /**
         * Checks if this ThreadLocal has been removed.
         * 
         * @return true if removed
         */
        public boolean isRemoved() {
            return removed.get();
        }
    }
    
    /**
     * AutoCloseable scope for automatic ThreadLocal cleanup.
     * Use with try-with-resources for guaranteed cleanup.
     */
    public static class CleanupScope implements AutoCloseable {
        private final long startTime;
        private boolean closed = false;
        
        private CleanupScope() {
            this.startTime = System.nanoTime();
            resetCleanupFlag();
        }
        
        @Override
        public void close() {
            if (!closed) {
                closed = true;
                cleanupCurrentThread();
                
                if (LOG.isLoggable(System.Logger.Level.DEBUG)) {
                    long duration = System.nanoTime() - startTime;
                    LOG.log(System.Logger.Level.DEBUG, () -> "ThreadLocal scope lasted " + (duration / 1_000_000) + " ms");
                }
            }
        }
        
        /**
         * Checks if this scope has been closed.
         * 
         * @return true if closed
         */
        public boolean isClosed() {
            return closed;
        }
    }
    
    /**
     * Thread-local usage statistics.
     *
     * @param registeredCount number of registered ThreadLocals
     * @param activeCount number of ThreadLocals with values for current thread
     * @param cleanupPerformed whether cleanup has been performed
     */
    public record ThreadLocalStats(
        int registeredCount,
        int activeCount,
        boolean cleanupPerformed
    ) {
        /**
         * Gets a summary of the statistics.
         * 
         * @return Human-readable summary
         */
        public String summary() {
            return String.format(
                "ThreadLocal Stats: %d registered, %d active, cleanup %s",
                registeredCount, activeCount, 
                cleanupPerformed ? "performed" : "pending"
            );
        }
        
        /**
         * Checks if there might be a memory leak.
         * 
         * @return true if potential leak detected
         */
        public boolean hasPotentialLeak() {
            return activeCount > 0 && !cleanupPerformed;
        }
    }
    
    /**
     * Utility method to wrap operations with automatic cleanup.
     * 
     * @param <T> The return type
     * @param operation The operation to perform
     * @return The result of the operation
     * @throws Exception if the operation throws
     */
    public static <T> T withCleanup(ThrowingSupplier<T> operation) throws Exception {
        CleanupScope scope = createScope();
        try {
            return operation.get();
        } finally {
            scope.close();
        }
    }
    
    /**
     * Utility method to wrap void operations with automatic cleanup.
     * 
     * @param operation The operation to perform
     * @throws Exception if the operation throws
     */
    public static void withCleanup(ThrowingRunnable operation) throws Exception {
        CleanupScope scope = createScope();
        try {
            operation.run();
        } finally {
            scope.close();
        }
    }
    
    /**
     * Functional interface for operations that may throw exceptions.
     * 
     * @param <T> The return type
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T> {
        T get() throws Exception;
    }
    
    /**
     * Functional interface for void operations that may throw exceptions.
     */
    @FunctionalInterface
    public interface ThrowingRunnable {
        void run() throws Exception;
    }
}
