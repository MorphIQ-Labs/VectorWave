package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.util.ValidationUtils;

/**
 * Configuration for Continuous Wavelet Transform operations.
 * 
 * <p>Provides configuration options for CWT computation including boundary handling,
 * FFT acceleration, normalization, and Java 21 optimization features.</p>
 */
public final class CWTConfig {
    
    /**
     * Padding strategy for boundary handling.
     */
    public enum PaddingStrategy {
        /** Zero padding. */
        ZERO,
        /** Reflect at boundaries. */
        REFLECT,
        /** Symmetric extension. */
        SYMMETRIC,
        /** Periodic extension. */
        PERIODIC
    }
    
    // Configuration fields
    private final BoundaryMode boundaryMode;
    private final boolean fftEnabled;
    private final boolean normalizeAcrossScales;
    private final PaddingStrategy paddingStrategy;
    private final int fftSize;
    private final boolean useScopedValues;
    private final boolean useStructuredConcurrency;
    private final boolean useStreamGatherers;
    private final MemoryPool memoryPool;
    
    // FFT threshold for automatic decision - lowered to show FFT benefits in demos
    // Can be overridden via system property: -Dvectorwave.cwt.fft.threshold=<value>
    // or environment variable: VECTORWAVE_CWT_FFT_THRESHOLD=<value>
    private static final int FFT_THRESHOLD = getFFTThreshold();
    
    private CWTConfig(Builder builder) {
        this.boundaryMode = builder.boundaryMode;
        this.fftEnabled = builder.fftEnabled;
        this.normalizeAcrossScales = builder.normalizeAcrossScales;
        this.paddingStrategy = builder.paddingStrategy;
        this.fftSize = builder.fftSize;
        this.useScopedValues = builder.useScopedValues;
        this.useStructuredConcurrency = builder.useStructuredConcurrency;
        this.useStreamGatherers = builder.useStreamGatherers;
        this.memoryPool = builder.memoryPool;
    }
    
    /**
     * Determines the FFT threshold from configuration sources.
     * Priority order: system property, environment variable, default (64).
     * 
     * @return the FFT threshold to use
     */
    private static int getFFTThreshold() {
        // Try system property first
        String sysProp = System.getProperty("vectorwave.cwt.fft.threshold");
        if (sysProp != null) {
            try {
                int value = Integer.parseInt(sysProp.trim());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // Fall through to next option
            }
        }
        
        // Try environment variable
        String envVar = System.getenv("VECTORWAVE_CWT_FFT_THRESHOLD");
        if (envVar != null) {
            try {
                int value = Integer.parseInt(envVar.trim());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        
        // Default value
        return 64;
    }
    
    /**
     * Creates a default configuration.
     * 
     * @return default CWT configuration
     */
    public static CWTConfig defaultConfig() {
        return builder().build();
    }
    
    /**
     * Creates a configuration optimized for Java 21 features.
     * 
     * @return Java 21 optimized configuration
     */
    public static CWTConfig optimizedForJava21() {
        return builder()
            .enableFFT(true)
            .normalizeScales(true)
            .useScopedValues(true)
            .useStructuredConcurrency(true)
            .useStreamGatherers(true)
            .build();
    }
    
    /**
     * Creates a configuration for real-time processing.
     * 
     * @return real-time optimized configuration
     */
    public static CWTConfig forRealTimeProcessing() {
        return builder()
            .enableFFT(false)  // Direct convolution for low latency
            .normalizeScales(true)
            .useScopedValues(false)  // Avoid overhead
            .useStructuredConcurrency(true)
            .useStreamGatherers(true)  // For efficient streaming
            .build();
    }
    
    /**
     * Creates a configuration for batch processing.
     * 
     * @return batch processing optimized configuration
     */
    public static CWTConfig forBatchProcessing() {
        return builder()
            .enableFFT(true)  // FFT for large batches
            .normalizeScales(true)
            .useScopedValues(true)  // Shared context
            .useStructuredConcurrency(true)  // Parallel processing
            .useStreamGatherers(false)
            .build();
    }
    
    /**
     * Creates a new builder.
     * 
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates a builder from this configuration.
     * 
     * @return builder with this configuration's values
     */
    public Builder toBuilder() {
        return new Builder()
            .boundaryMode(boundaryMode)
            .enableFFT(fftEnabled)
            .normalizeScales(normalizeAcrossScales)
            .paddingStrategy(paddingStrategy)
            .fftSize(fftSize)
            .useScopedValues(useScopedValues)
            .useStructuredConcurrency(useStructuredConcurrency)
            .useStreamGatherers(useStreamGatherers)
            .memoryPool(memoryPool);
    }
    
    /**
     * Determines if FFT should be used for given signal size.
     * 
     * @param signalSize size of the signal
     * @return true if FFT should be used
     */
    public boolean shouldUseFFT(int signalSize) {
        if (!fftEnabled) {
            return false;
        }
        
        // If FFT size is specified, use it as threshold
        if (fftSize > 0) {
            return signalSize >= fftSize / 2;
        }
        
        // Otherwise use default threshold
        return signalSize >= FFT_THRESHOLD;
    }
    
    /**
     * Calculates optimal FFT size for given signal size.
     * 
     * @param signalSize size of the signal
     * @return optimal FFT size (next power of 2)
     */
    public int getOptimalFFTSize(int signalSize) {
        // Find next power of 2
        int size = 1;
        while (size < signalSize) {
            size *= 2;
        }
        return size;
    }
    
    // Getters
    /**
     * Gets the boundary mode used during convolution.
     * @return boundary mode used during convolution
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
    
    /**
     * Checks if FFT acceleration is enabled.
     * @return true if FFT acceleration is enabled
     */
    public boolean isFFTEnabled() {
        return fftEnabled;
    }
    
    /**
     * Checks if coefficients are normalized across scales.
     * @return true if coefficients are normalized across scales
     */
    public boolean isNormalizeAcrossScales() {
        return normalizeAcrossScales;
    }
    
    /**
     * Gets the padding strategy for boundaries.
     * @return padding strategy for boundaries
     */
    public PaddingStrategy getPaddingStrategy() {
        return paddingStrategy;
    }
    
    /**
     * Gets the fixed FFT size.
     * @return fixed FFT size (0 means auto)
     */
    public int getFFTSize() {
        return fftSize;
    }
    
    /**
     * Checks if scoped values are used.
     * @return true if scoped values are used
     */
    public boolean isUseScopedValues() {
        return useScopedValues;
    }
    
    /**
     * Checks if structured concurrency is used.
     * @return true if structured concurrency is used
     */
    public boolean isUseStructuredConcurrency() {
        return useStructuredConcurrency;
    }
    
    /**
     * Checks if stream gatherers are used.
     * @return true if stream gatherers are used
     */
    public boolean isUseStreamGatherers() {
        return useStreamGatherers;
    }
    
    /**
     * Gets the memory pool in use.
     * @return memory pool in use, or null
     */
    public MemoryPool getMemoryPool() {
        return memoryPool;
    }
    
    /**
     * Builder for CWT configuration.
     */
    public static class Builder {
        private BoundaryMode boundaryMode = BoundaryMode.PERIODIC;
        private boolean fftEnabled = true;
        private boolean normalizeAcrossScales = true;
        private PaddingStrategy paddingStrategy = PaddingStrategy.REFLECT;
        private int fftSize = 0;  // 0 means auto-determine
        private boolean useScopedValues = false;
        private boolean useStructuredConcurrency = true;
        private boolean useStreamGatherers = true;
        private MemoryPool memoryPool = null;
        
        private Builder() {}
        
        /**
         * Sets boundary handling mode.
         * @param mode boundary mode
         * @return this builder
         */
        public Builder boundaryMode(BoundaryMode mode) {
            this.boundaryMode = mode;
            return this;
        }
        
        /**
         * Enables or disables FFT-based convolution.
         * @param enable true to use FFT
         * @return this builder
         */
        public Builder enableFFT(boolean enable) {
            this.fftEnabled = enable;
            return this;
        }
        
        /**
         * Normalizes coefficients across scales for fair energy comparison.
         * @param normalize true to normalize
         * @return this builder
         */
        public Builder normalizeScales(boolean normalize) {
            this.normalizeAcrossScales = normalize;
            return this;
        }
        
        /**
         * Sets boundary padding strategy.
         * @param strategy padding strategy
         * @return this builder
         */
        public Builder paddingStrategy(PaddingStrategy strategy) {
            this.paddingStrategy = strategy;
            return this;
        }
        
        /**
         * Sets fixed FFT size (power of 2) or 0 for auto.
         * @param size FFT size (0 or power of 2)
         * @return this builder
         * @throws IllegalArgumentException if invalid
         */
        public Builder fftSize(int size) {
            if (size < 0) {
                throw new IllegalArgumentException("FFT size must be non-negative");
            }
            if (size > 0 && !ValidationUtils.isPowerOfTwo(size)) {
                throw new IllegalArgumentException("FFT size must be a power of 2 or 0 (auto)");
            }
            this.fftSize = size;
            return this;
        }
        
        /**
         * Enables use of scoped values to reduce parameter threading overhead.
         * @param use true to enable
         * @return this builder
         */
        public Builder useScopedValues(boolean use) {
            this.useScopedValues = use;
            return this;
        }
        
        /**
         * Enables structured concurrency in CWT internals.
         * @param use true to enable
         * @return this builder
         */
        public Builder useStructuredConcurrency(boolean use) {
            this.useStructuredConcurrency = use;
            return this;
        }
        
        /**
         * Enables stream gatherers for intra-stage parallelism.
         * @param use true to enable
         * @return this builder
         */
        public Builder useStreamGatherers(boolean use) {
            this.useStreamGatherers = use;
            return this;
        }
        
        /**
         * Sets memory pool for coefficient storage.
         * @param pool memory pool
         * @return this builder
         */
        public Builder memoryPool(MemoryPool pool) {
            this.memoryPool = pool;
            return this;
        }
        
        /**
         * Builds configuration.
         * @return new config instance
         */
        public CWTConfig build() {
            return new CWTConfig(this);
        }
    }
}
