package com.morphiqlabs.wavelet.api;

/**
 * Service Provider Interface for wavelet transform optimizations.
 * Implementations can provide optimized versions using Vector API or other techniques.
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
        SCALAR,
        VECTOR_API,
        STRUCTURED_CONCURRENCY,
        CUSTOM
    }
}