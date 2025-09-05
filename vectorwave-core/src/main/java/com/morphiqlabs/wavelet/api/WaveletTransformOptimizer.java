package com.morphiqlabs.wavelet.api;

/**
 * Service Provider Interface for wavelet transform optimizations.
 * Implementations can provide optimized versions using Vector API or other techniques.
 *
 * @since 1.0.0
 */
public interface WaveletTransformOptimizer {
    
    /**
     * Check if this optimizer is supported on the current platform.
     * @return true if the optimizer can be used
     */
    boolean isSupported();
    
    /**
     * Get the priority of this optimizer. Higher values have higher priority.
     * @return priority value (0-100)
     */
    int getPriority();
    
    /**
     * Get a descriptive name for this optimizer.
     * @return optimizer name
     */
    String getName();
    
    /**
     * Get the type of optimization provided.
     * @return optimization type
     */
    OptimizationType getType();
    
    /**
     * Types of optimizations available.
     */
    enum OptimizationType {
        /** Pure scalar Java 21 implementation. */
        SCALAR,
        /** Vector API–based SIMD optimizations (extensions module). */
        VECTOR_API,
        /** Concurrency/parallelism optimizations (e.g., structured concurrency). */
        STRUCTURED_CONCURRENCY,
        /** Custom or platform-specific optimization type. */
        CUSTOM
    }
}
