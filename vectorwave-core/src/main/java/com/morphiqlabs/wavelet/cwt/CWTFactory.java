package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.AbstractStaticFactory;
import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.ContinuousWavelet;

/**
 * Factory for creating CWT transform instances.
 * 
 * <p>Provides convenient methods for creating CWT transforms with
 * various configurations optimized for different use cases.</p>
 * 
 * <p>This factory supports two usage patterns:</p>
 * <ul>
 *   <li>Static methods for direct creation: {@code CWTFactory.create(wavelet)}</li>
 *   <li>Factory interface pattern: {@code CWTFactory.getInstance().create(wavelet)}</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Static method pattern (convenience)
 * CWTTransform cwt1 = CWTFactory.create(new MorletWavelet());
 * 
 * // Factory interface pattern (for dependency injection)
 * Factory<CWTTransform, ContinuousWavelet> factory = CWTFactory.getInstance();
 * CWTTransform cwt2 = factory.create(new MorletWavelet());
 * }</pre>
 */
public final class CWTFactory {
    
    private CWTFactory() {
        // Prevent instantiation
    }
    
    /**
     * Creates a CWT transform with default configuration.
     * 
     * @param wavelet the continuous wavelet to use
     * @return new CWT transform instance
     */
    public static CWTTransform create(ContinuousWavelet wavelet) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        return new CWTTransform(wavelet, CWTConfig.defaultConfig());
    }
    
    /**
     * Creates a CWT transform with custom configuration.
     * 
     * @param wavelet the continuous wavelet to use
     * @param config the configuration
     * @return new CWT transform instance
     */
    public static CWTTransform create(ContinuousWavelet wavelet, CWTConfig config) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        return new CWTTransform(wavelet, config);
    }
    
    /**
     * Creates a CWT transform optimized for real-time processing.
     * 
     * @param wavelet the continuous wavelet to use
     * @return new CWT transform instance optimized for real-time
     */
    public static CWTTransform createForRealTime(ContinuousWavelet wavelet) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        return new CWTTransform(wavelet, CWTConfig.forRealTimeProcessing());
    }
    
    /**
     * Creates a CWT transform optimized for batch processing.
     * 
     * @param wavelet the continuous wavelet to use
     * @return new CWT transform instance optimized for batch processing
     */
    public static CWTTransform createForBatchProcessing(ContinuousWavelet wavelet) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        return new CWTTransform(wavelet, CWTConfig.forBatchProcessing());
    }
    
    /**
     * Creates a CWT transform optimized for Java 21 features.
     * 
     * @param wavelet the continuous wavelet to use
     * @return new CWT transform instance with Java 21 optimizations
     */
    public static CWTTransform createOptimizedForJava21(ContinuousWavelet wavelet) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        return new CWTTransform(wavelet, CWTConfig.optimizedForJava21());
    }
    
    /**
     * Creates a new builder for custom CWT transform configuration.
     * 
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Gets the factory instance that implements the common Factory interface.
     *
     * @return the factory instance
     */
    public static Instance getInstance() {
        return Instance.INSTANCE;
    }

    /**
     * Factory instance that implements the common Factory interface.
     * This provides an alternative way to use the factory that follows
     * the standardized factory pattern.
     */
    public static final class Instance extends AbstractStaticFactory<CWTTransform, ContinuousWavelet> {
        private static final Instance INSTANCE = new Instance();

        private Instance() {
            // Singleton
        }

        @Override
        protected CWTTransform doCreate() {
            // No default wavelet, so throw exception
            throw new UnsupportedOperationException(
                "CWTFactory requires a ContinuousWavelet. Use create(wavelet) instead.");
        }

        @Override
        protected CWTTransform doCreate(ContinuousWavelet wavelet) {
            return CWTFactory.create(wavelet);
        }

        @Override
        public boolean isValidConfiguration(ContinuousWavelet wavelet) {
            return wavelet != null;
        }

        @Override
        public String getDescription() {
            return "Factory for creating CWT transform instances";
        }
    }
    
    /**
     * Builder for creating customized CWT transforms.
     */
    public static class Builder {
        private ContinuousWavelet wavelet;
        private final CWTConfig.Builder configBuilder;
        
        private Builder() {
            this.configBuilder = CWTConfig.builder();
        }
        
        /**
         * Sets the continuous wavelet.
         * 
         * @param wavelet the wavelet to use
         * @return this builder
         */
        public Builder wavelet(ContinuousWavelet wavelet) {
            this.wavelet = wavelet;
            return this;
        }
        
        /**
         * Sets the boundary mode.
         * 
         * @param mode boundary mode
         * @return this builder
         */
        public Builder boundaryMode(BoundaryMode mode) {
            configBuilder.boundaryMode(mode);
            return this;
        }
        
        /**
         * Enables or disables FFT acceleration.
         * 
         * @param enable true to enable FFT
         * @return this builder
         */
        public Builder enableFFT(boolean enable) {
            configBuilder.enableFFT(enable);
            return this;
        }
        
        /**
         * Enables or disables scale normalization.
         * 
         * @param normalize true to normalize across scales
         * @return this builder
         */
        public Builder normalizeScales(boolean normalize) {
            configBuilder.normalizeScales(normalize);
            return this;
        }
        
        /**
         * Sets the padding strategy.
         * 
         * @param strategy padding strategy
         * @return this builder
         */
        public Builder paddingStrategy(CWTConfig.PaddingStrategy strategy) {
            configBuilder.paddingStrategy(strategy);
            return this;
        }
        
        /**
         * Sets the FFT size.
         * 
         * @param size FFT size (must be power of 2 or 0 for auto)
         * @return this builder
         */
        public Builder fftSize(int size) {
            configBuilder.fftSize(size);
            return this;
        }
        
        /**
         * Enables or disables scoped values (Java 21).
         * 
         * @param use true to use scoped values
         * @return this builder
         */
        public Builder useScopedValues(boolean use) {
            configBuilder.useScopedValues(use);
            return this;
        }
        
        /**
         * Enables or disables structured concurrency (Java 21).
         * 
         * @param use true to use structured concurrency
         * @return this builder
         */
        public Builder useStructuredConcurrency(boolean use) {
            configBuilder.useStructuredConcurrency(use);
            return this;
        }
        
        /**
         * Enables or disables stream gatherers (Java 21).
         * 
         * @param use true to use stream gatherers
         * @return this builder
         */
        public Builder useStreamGatherers(boolean use) {
            configBuilder.useStreamGatherers(use);
            return this;
        }
        
        /**
         * Builds the CWT transform.
         * 
         * @return new CWT transform instance
         * @throws IllegalStateException if wavelet not set
         */
        public CWTTransform build() {
            if (wavelet == null) {
                throw new IllegalStateException("Wavelet must be set");
            }
            
            CWTConfig config = configBuilder.build();
            return new CWTTransform(wavelet, config);
        }
    }
}