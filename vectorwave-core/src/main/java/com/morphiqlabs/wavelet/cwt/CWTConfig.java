package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.cwt.memory.CWTMemoryPool;
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
        ZERO,      // Zero padding
        REFLECT,   // Reflect at boundaries
        SYMMETRIC, // Symmetric extension
        PERIODIC   // Periodic extension
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
    private final CWTMemoryPool memoryPool;
    private final FFTAlgorithm fftAlgorithm;
    
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
        this.fftAlgorithm = builder.fftAlgorithm;
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
            .memoryPool(memoryPool)
            .fftAlgorithm(fftAlgorithm);
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
    
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
    
    public boolean isFFTEnabled() {
        return fftEnabled;
    }
    
    public boolean isNormalizeAcrossScales() {
        return normalizeAcrossScales;
    }
    
    public PaddingStrategy getPaddingStrategy() {
        return paddingStrategy;
    }
    
    public int getFFTSize() {
        return fftSize;
    }
    
    public boolean isUseScopedValues() {
        return useScopedValues;
    }
    
    public boolean isUseStructuredConcurrency() {
        return useStructuredConcurrency;
    }
    
    public boolean isUseStreamGatherers() {
        return useStreamGatherers;
    }
    
    public CWTMemoryPool getMemoryPool() {
        return memoryPool;
    }
    
    public FFTAlgorithm getFFTAlgorithm() {
        return fftAlgorithm;
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
        private CWTMemoryPool memoryPool = null;
        private FFTAlgorithm fftAlgorithm = FFTAlgorithm.AUTO;
        
        private Builder() {}
        
        public Builder boundaryMode(BoundaryMode mode) {
            this.boundaryMode = mode;
            return this;
        }
        
        public Builder enableFFT(boolean enable) {
            this.fftEnabled = enable;
            return this;
        }
        
        public Builder normalizeScales(boolean normalize) {
            this.normalizeAcrossScales = normalize;
            return this;
        }
        
        public Builder paddingStrategy(PaddingStrategy strategy) {
            this.paddingStrategy = strategy;
            return this;
        }
        
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
        
        public Builder useScopedValues(boolean use) {
            this.useScopedValues = use;
            return this;
        }
        
        public Builder useStructuredConcurrency(boolean use) {
            this.useStructuredConcurrency = use;
            return this;
        }
        
        public Builder useStreamGatherers(boolean use) {
            this.useStreamGatherers = use;
            return this;
        }
        
        public Builder memoryPool(CWTMemoryPool pool) {
            this.memoryPool = pool;
            return this;
        }
        
        public Builder fftAlgorithm(FFTAlgorithm algorithm) {
            this.fftAlgorithm = algorithm;
            return this;
        }
        
        public CWTConfig build() {
            return new CWTConfig(this);
        }
    }
}