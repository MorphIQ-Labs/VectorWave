package com.morphiqlabs.wavelet.cwt;

/**
 * Configuration for signal analysis parameters used in adaptive scale selection.
 * 
 * <p>This configuration allows fine-tuning of signal processing parameters
 * that affect how signals are analyzed for adaptive scale selection.</p>
 * 
 * <p><strong>Parameter Guidelines:</strong></p>
 * <ul>
 *   <li><strong>Energy Threshold:</strong> Fraction of total energy considered significant.
 *       Typical values: 0.001 (0.1%) for high sensitivity, 0.01 (1%) for standard,
 *       0.05 (5%) for noise suppression.</li>
 *   <li><strong>Spectral Analysis Size:</strong> FFT window size for frequency analysis.
 *       Must be power of 2. Common: 512 (fast), 1024 (balanced), 2048 (detailed).</li>
 *   <li><strong>Scale Density Factor:</strong> Controls scale spacing density.
 *       Higher values = more scales. Typical: 1.0 (sparse), 1.5 (standard), 2.0 (dense).</li>
 * </ul>
 */
public final class SignalAnalysisConfig {
    
    private final double energyThreshold;
    private final int spectralAnalysisSize;
    private final double scaleDensityFactor;
    
    private SignalAnalysisConfig(Builder builder) {
        if (builder.energyThreshold == null) {
            throw new IllegalStateException("Energy threshold must be specified");
        }
        if (builder.spectralAnalysisSize == null) {
            throw new IllegalStateException("Spectral analysis size must be specified");
        }
        if (builder.scaleDensityFactor == null) {
            throw new IllegalStateException("Scale density factor must be specified");
        }
        
        this.energyThreshold = builder.energyThreshold;
        this.spectralAnalysisSize = builder.spectralAnalysisSize;
        this.scaleDensityFactor = builder.scaleDensityFactor;
    }
    
    /**
     * Creates a new builder for signal analysis configuration.
     * 
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Gets the energy threshold for significant frequency components.
     * 
     * @return the energy threshold as a fraction of total energy
     */
    public double getEnergyThreshold() {
        return energyThreshold;
    }
    
    /**
     * Gets the FFT window size for spectral analysis.
     * 
     * @return the spectral analysis window size
     */
    public int getSpectralAnalysisSize() {
        return spectralAnalysisSize;
    }
    
    /**
     * Gets the scale density factor for adaptive scale generation.
     * 
     * @return the scale density factor
     */
    public double getScaleDensityFactor() {
        return scaleDensityFactor;
    }
    
    /**
     * Builder for SignalAnalysisConfig.
     */
    public static final class Builder {
        private Double energyThreshold;
        private Integer spectralAnalysisSize;
        private Double scaleDensityFactor;

        /**
         * Creates a new Builder.
         */
        public Builder() {
            // Default constructor
        }
        
        /**
         * Sets the energy threshold for significant frequency components.
         * 
         * @param threshold the energy threshold (must be between 0 and 1)
         * @return this builder
         * @throws IllegalArgumentException if threshold is not in valid range
         */
        public Builder energyThreshold(double threshold) {
            if (threshold <= 0 || threshold >= 1) {
                throw new IllegalArgumentException(
                    "Energy threshold must be between 0 and 1 (exclusive): " + threshold);
            }
            this.energyThreshold = threshold;
            return this;
        }
        
        /**
         * Sets the FFT window size for spectral analysis.
         * 
         * @param size the window size (must be power of 2, at least 64)
         * @return this builder
         * @throws IllegalArgumentException if size is not valid
         */
        public Builder spectralAnalysisSize(int size) {
            if (size < 64 || (size & (size - 1)) != 0) {
                throw new IllegalArgumentException(
                    "Spectral analysis size must be a power of 2 and at least 64: " + size);
            }
            this.spectralAnalysisSize = size;
            return this;
        }
        
        /**
         * Sets the scale density factor.
         * 
         * @param factor the scale density factor (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if factor is not positive
         */
        public Builder scaleDensityFactor(double factor) {
            if (factor <= 0) {
                throw new IllegalArgumentException(
                    "Scale density factor must be positive: " + factor);
            }
            this.scaleDensityFactor = factor;
            return this;
        }
        
        /**
         * Builds the SignalAnalysisConfig.
         * 
         * @return a new SignalAnalysisConfig instance
         * @throws IllegalStateException if any required parameter is not set
         */
        public SignalAnalysisConfig build() {
            return new SignalAnalysisConfig(this);
        }
    }
}