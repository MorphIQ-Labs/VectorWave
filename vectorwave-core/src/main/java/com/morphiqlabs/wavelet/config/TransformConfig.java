package com.morphiqlabs.wavelet.config;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidConfigurationException;
import com.morphiqlabs.wavelet.util.NullChecks;

/**
 * Immutable configuration for wavelet transform operations.
 * 
 * <p>This class provides fine-grained control over wavelet transform behavior,
 * including performance optimizations, boundary handling, and decomposition limits.
 * It uses the builder pattern for flexible, readable construction.</p>
 * 
 * <p>Key configuration options:</p>
 * <ul>
 *   <li><b>Boundary Mode</b>: How to handle signal boundaries during convolution</li>
 *   <li><b>Optimization Path</b>: Force scalar or vector operations, or auto-detect</li>
 *   <li><b>Decomposition Levels</b>: Maximum allowed levels for multi-level transforms</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Auto-detect optimal path (recommended)
 * TransformConfig autoConfig = TransformConfig.defaultConfig();
 * 
 * // Force scalar operations for debugging
 * TransformConfig scalarConfig = TransformConfig.builder()
 *     .forceScalar(true)
 *     .boundaryMode(BoundaryMode.ZERO_PADDING)
 *     .build();
 * 
 * // Force Vector API for maximum performance
 * TransformConfig vectorConfig = TransformConfig.builder()
 *     .forceVector(true)
 *     .maxDecompositionLevels(5)
 *     .build();
 * 
 * // Use with transform
 * WaveletTransform transform = new WaveletTransform(
 *     Daubechies.DB4, 
 *     BoundaryMode.PERIODIC, 
 *     vectorConfig
 * );
 * }</pre>
 * 
 * <p>Performance notes:</p>
 * <ul>
 *   <li>Auto-detection usually provides optimal performance</li>
 *   <li>Force scalar for consistent behavior across platforms</li>
 *   <li>Force vector when you know your data size benefits from vectorization</li>
 * </ul>
 */
public final class TransformConfig {

    // Boundary handling mode for the transform.
    private final BoundaryMode boundaryMode;
    // Forces scalar engine if true, otherwise auto-detects.
    private final boolean forceScalar;
    // Forces use of Vector API operations when available.
    private final boolean forceVector;
    // Maximum allowed decomposition levels.
    private final int maxDecompositionLevels;

    /**
     * Private constructor used by Builder.
     *
     * @param builder Builder instance with configuration values.
     */
    private TransformConfig(Builder builder) {
        this.boundaryMode = builder.boundaryMode;
        this.forceScalar = builder.forceScalar;
        this.forceVector = builder.forceVector;
        this.maxDecompositionLevels = builder.maxDecompositionLevels;

        // Validate: can't force both scalar and vector
        if (forceScalar && forceVector) {
            throw InvalidConfigurationException.conflictingOptions("forceScalar", "forceVector");
        }
    }

    /**
     * Creates a new builder for TransformConfig.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default configuration.
     *
     * @return default config with periodic boundaries and auto-detected engine
     */
    public static TransformConfig defaultConfig() {
        return new Builder().build();
    }

    /**
     * @return boundary mode for the transform
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }

    /**
     * @return true if scalar engine is forced
     */
    public boolean isForceScalar() {
        return forceScalar;
    }

    /**
     * @return true if scalar operations are forced (same as isForceScalar)
     */
    public boolean isForceScalarOperations() {
        return forceScalar;
    }

    /**
     * @return true if Vector API operations are forced when available
     */
    public boolean isForceVector() {
        return forceVector;
    }

    /**
     * @return maximum decomposition levels
     */
    public int getMaxDecompositionLevels() {
        return maxDecompositionLevels;
    }

    /**
     * Returns a string representation of the configuration.
     *
     * @return string with field values
     */
    @Override
    public String toString() {
        return "TransformConfig{" +
                "boundaryMode=" + boundaryMode +
                ", forceScalar=" + forceScalar +
                ", forceVector=" + forceVector +
                ", maxDecompositionLevels=" + maxDecompositionLevels +
                '}';
    }

    /**
     * Builder for TransformConfig.
     * Allows stepwise construction of configuration.
     */
    public static class Builder {
        // Default boundary mode is PERIODIC.
        private BoundaryMode boundaryMode = BoundaryMode.PERIODIC;
        // Default: do not force scalar engine.
        private boolean forceScalar = false;
        // Default: auto-detect Vector API availability.
        private boolean forceVector = false;
        // Default: allow up to 20 decomposition levels (handles signals up to 2^20 = 1,048,576 samples).
        private int maxDecompositionLevels = 20;

        // Private constructor to enforce use via TransformConfig.
        private Builder() {
        }

        /**
         * Sets the boundary mode.
         *
         * @param boundaryMode boundary handling mode
         * @return this builder
         */
        public Builder boundaryMode(BoundaryMode boundaryMode) {
            this.boundaryMode = NullChecks.requireNonNull(boundaryMode, "boundaryMode");
            return this;
        }

        /**
         * Sets whether to force scalar engine.
         *
         * @param forceScalar true to force scalar engine
         * @return this builder
         */
        public Builder forceScalar(boolean forceScalar) {
            this.forceScalar = forceScalar;
            return this;
        }

        /**
         * Sets whether to force Vector API operations when available.
         * If Vector API is not available, this setting is ignored.
         *
         * @param forceVector true to force Vector API operations
         * @return this builder
         */
        public Builder forceVector(boolean forceVector) {
            this.forceVector = forceVector;
            return this;
        }

        /**
         * Sets the maximum decomposition levels.
         *
         * @param maxLevels maximum levels (must be >= 1)
         * @return this builder
         * @throws InvalidArgumentException if maxLevels {@literal <} 1
         */
        public Builder maxDecompositionLevels(int maxLevels) {
            if (maxLevels < 1) {
                throw InvalidArgumentException.notPositive(maxLevels);
            }
            this.maxDecompositionLevels = maxLevels;
            return this;
        }

        /**
         * Builds the TransformConfig instance.
         *
         * @return new TransformConfig
         */
        public TransformConfig build() {
            return new TransformConfig(this);
        }
    }
}
