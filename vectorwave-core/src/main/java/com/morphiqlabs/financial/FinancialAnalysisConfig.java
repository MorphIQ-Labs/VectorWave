package com.morphiqlabs.financial;

/**
 * Configuration class for financial analysis parameters.
 * 
 * <p>This class provides configurable thresholds and parameters for various
 * financial analysis operations including crash detection, volatility analysis,
 * and regime change detection using wavelet transforms.</p>
 * 
 * <p>All parameters must be explicitly specified as they are highly dependent on
 * the specific market, instrument, time period, and analysis requirements.</p>
 * 
 * <p><strong>Parameter Guidelines:</strong></p>
 * <ul>
 *   <li><strong>Crash Asymmetry Threshold:</strong> Typically 0.6-0.8. Higher values indicate 
 *       more severe asymmetric behavior. Equity markets often use 0.7, while forex might use 0.8.</li>
 *   <li><strong>Volatility Thresholds:</strong> Market-specific. For equity indices: low ~0.5, 
 *       high ~2.0. For individual stocks: low ~1.0, high ~3.0. For forex: low ~0.3, high ~1.5.</li>
 *   <li><strong>Regime Trend Threshold:</strong> Typically 0.01-0.05. Lower for stable markets, 
 *       higher for volatile ones.</li>
 *   <li><strong>Anomaly Detection Threshold:</strong> Usually 2-4 standard deviations. 
 *       3.0 catches ~0.3% outliers, 2.0 catches ~5%.</li>
 *   <li><strong>Window Size:</strong> Can be any size with MODWT. Common: 60 (quarterly), 120 (semi-annual), 
 *       252 (annual trading days), 504 (2-year). Depends on data frequency.</li>
 *   <li><strong>Confidence Level:</strong> Typically 0.95 (95%) or 0.99 (99%). 
 *       Higher for risk-averse applications.</li>
 * </ul>
 */
public final class FinancialAnalysisConfig {
    
    private final double crashAsymmetryThreshold;
    private final double volatilityLowThreshold;
    private final double volatilityHighThreshold;
    private final double regimeTrendThreshold;
    private final double anomalyDetectionThreshold;
    private final int windowSize;
    private final double confidenceLevel;
    
    /**
     * Creates a new FinancialAnalysisConfig with custom parameters.
     */
    private FinancialAnalysisConfig(Builder builder) {
        // Validate that all required parameters have been set
        if (builder.crashAsymmetryThreshold == null) {
            throw new IllegalStateException("Crash asymmetry threshold must be specified");
        }
        if (builder.volatilityLowThreshold == null) {
            throw new IllegalStateException("Volatility low threshold must be specified");
        }
        if (builder.volatilityHighThreshold == null) {
            throw new IllegalStateException("Volatility high threshold must be specified");
        }
        if (builder.regimeTrendThreshold == null) {
            throw new IllegalStateException("Regime trend threshold must be specified");
        }
        if (builder.anomalyDetectionThreshold == null) {
            throw new IllegalStateException("Anomaly detection threshold must be specified");
        }
        if (builder.windowSize == null) {
            throw new IllegalStateException("Window size must be specified");
        }
        if (builder.confidenceLevel == null) {
            throw new IllegalStateException("Confidence level must be specified");
        }
        
        // Validate relationship between thresholds
        if (builder.volatilityLowThreshold >= builder.volatilityHighThreshold) {
            throw new IllegalArgumentException(
                "Volatility low threshold must be less than high threshold: low=" + 
                builder.volatilityLowThreshold + ", high=" + builder.volatilityHighThreshold);
        }
        
        this.crashAsymmetryThreshold = builder.crashAsymmetryThreshold;
        this.volatilityLowThreshold = builder.volatilityLowThreshold;
        this.volatilityHighThreshold = builder.volatilityHighThreshold;
        this.regimeTrendThreshold = builder.regimeTrendThreshold;
        this.anomalyDetectionThreshold = builder.anomalyDetectionThreshold;
        this.windowSize = builder.windowSize;
        this.confidenceLevel = builder.confidenceLevel;
    }
    
    /**
     * Creates a new builder for customizing configuration parameters.
     * 
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Gets the threshold for detecting market crash asymmetry.
     * Higher values indicate more severe asymmetric behavior in market movements.
     * 
     * @return the crash asymmetry threshold
     */
    public double getCrashAsymmetryThreshold() {
        return crashAsymmetryThreshold;
    }
    
    /**
     * Gets the threshold for low volatility detection.
     * Values below this threshold indicate low volatility periods.
     * 
     * @return the low volatility threshold
     */
    public double getVolatilityLowThreshold() {
        return volatilityLowThreshold;
    }
    
    /**
     * Gets the threshold for high volatility detection.
     * Values above this threshold indicate high volatility periods.
     * 
     * @return the high volatility threshold
     */
    public double getVolatilityHighThreshold() {
        return volatilityHighThreshold;
    }
    
    /**
     * Gets the threshold for regime trend detection.
     * Changes above this threshold indicate potential regime shifts.
     * 
     * @return the regime trend threshold
     */
    public double getRegimeTrendThreshold() {
        return regimeTrendThreshold;
    }
    
    /**
     * Gets the threshold for anomaly detection.
     * Values represent standard deviations from normal behavior.
     * 
     * @return the anomaly detection threshold
     */
    public double getAnomalyDetectionThreshold() {
        return anomalyDetectionThreshold;
    }
    
    /**
     * Gets the window size for analysis operations.
     * Can be any size with MODWT transforms.
     * 
     * @return the analysis window size
     */
    public int getWindowSize() {
        return windowSize;
    }
    
    /**
     * Gets the confidence level for statistical tests.
     * 
     * @return the confidence level (0.0 to 1.0)
     */
    public double getConfidenceLevel() {
        return confidenceLevel;
    }
    
    /**
     * Builder class for creating FinancialAnalysisConfig instances.
     * All parameters are required and must be explicitly set.
     */
    public static final class Builder {
        private Double crashAsymmetryThreshold;
        private Double volatilityLowThreshold;
        private Double volatilityHighThreshold;
        private Double regimeTrendThreshold;
        private Double anomalyDetectionThreshold;
        private Integer windowSize;
        private Double confidenceLevel;
        
        /**
         * Sets the crash asymmetry threshold.
         * 
         * @param threshold the crash asymmetry threshold (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if threshold is not positive
         */
        public Builder crashAsymmetryThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Crash asymmetry threshold must be positive");
            }
            this.crashAsymmetryThreshold = threshold;
            return this;
        }
        
        /**
         * Sets the low volatility threshold.
         * 
         * @param threshold the low volatility threshold (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if threshold is not positive
         */
        public Builder volatilityLowThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Volatility low threshold must be positive");
            }
            this.volatilityLowThreshold = threshold;
            return this;
        }
        
        /**
         * Sets the high volatility threshold.
         * 
         * @param threshold the high volatility threshold (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if threshold is not positive
         */
        public Builder volatilityHighThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Volatility high threshold must be positive");
            }
            this.volatilityHighThreshold = threshold;
            return this;
        }
        
        /**
         * Sets the regime trend threshold.
         * 
         * @param threshold the regime trend threshold (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if threshold is not positive
         */
        public Builder regimeTrendThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Regime trend threshold must be positive");
            }
            this.regimeTrendThreshold = threshold;
            return this;
        }
        
        /**
         * Sets the anomaly detection threshold.
         * 
         * @param threshold the anomaly detection threshold (must be positive)
         * @return this builder
         * @throws IllegalArgumentException if threshold is not positive
         */
        public Builder anomalyDetectionThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Anomaly detection threshold must be positive");
            }
            this.anomalyDetectionThreshold = threshold;
            return this;
        }
        
        /**
         * Sets the window size for analysis.
         * 
         * @param size the window size (must be at least 2)
         * @return this builder
         * @throws IllegalArgumentException if size is less than 2
         */
        public Builder windowSize(int size) {
            if (size < 2) {
                throw new IllegalArgumentException("Window size must be at least 2");
            }
            this.windowSize = size;
            return this;
        }
        
        /**
         * Sets the confidence level for statistical tests.
         * 
         * @param level the confidence level (must be between 0.0 and 1.0, exclusive)
         * @return this builder
         * @throws IllegalArgumentException if level is not between 0.0 and 1.0 (exclusive)
         */
        public Builder confidenceLevel(double level) {
            if (level <= 0.0 || level >= 1.0) {
                throw new IllegalArgumentException("Confidence level must be between 0.0 and 1.0");
            }
            this.confidenceLevel = level;
            return this;
        }
        
        /**
         * Builds the FinancialAnalysisConfig with the specified parameters.
         * All parameters must have been set before calling build().
         * 
         * @return a new FinancialAnalysisConfig instance
         * @throws IllegalStateException if any required parameter has not been set
         */
        public FinancialAnalysisConfig build() {
            return new FinancialAnalysisConfig(this);
        }
    }
}