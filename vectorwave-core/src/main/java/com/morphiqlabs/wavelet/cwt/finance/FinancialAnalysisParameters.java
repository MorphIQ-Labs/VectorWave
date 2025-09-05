package com.morphiqlabs.wavelet.cwt.finance;

/**
 * Configurable parameters for financial wavelet analysis.
 * 
 * <p>This class provides a fluent builder interface to customize all thresholds
 * and parameters used in financial analysis. It replaces the static constants
 * in FinancialAnalysisConfig to allow per-analysis customization.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * FinancialAnalysisParameters params = FinancialAnalysisParameters.builder()
 *     .crashAsymmetryThreshold(15.0)  // More sensitive crash detection
 *     .volatilityLowThreshold(0.3)    // Tighter volatility bands
 *     .regimeTrendThreshold(0.03)     // 3% trend threshold
 *     .annualRiskFreeRate(0.045)      // 4.5% annual risk-free rate
 *     .build();
 * 
 * FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(params);
 * }</pre>
 */
public final class FinancialAnalysisParameters {
    
    /**
     * Standard number of trading days per year.
     * This is the conventional value used in financial markets for annualization.
     */
    public static final int TRADING_DAYS_PER_YEAR = 252;
    
    // Crash Detection Parameters
    private final double crashAsymmetryThreshold;
    private final double crashMinScale;
    private final double crashMaxScale;
    private final int crashNumScales;
    
    // Volatility Analysis Parameters
    private final double volatilityLowThreshold;
    private final double volatilityMediumThreshold;
    private final double volatilityHighThreshold;
    private final double volumeDivergenceThreshold;
    private final double priceDivergenceThreshold;
    
    // Cyclical Analysis Parameters
    private final double[] cycleTestFrequencies;
    
    // Trend Analysis Parameters
    private final double trendMinScale;
    private final double trendMaxScale;
    private final int trendNumScales;
    
    // Market Regime Detection Parameters
    private final int regimeDetectionLookbackPeriod;
    private final double regimeTrendThreshold;
    
    // Trading Signal Generation Parameters
    private final int signalGenerationMinHistory;
    private final int recentCrashLookbackWindow;
    private final double crashProbabilityNormalization;
    private final int riskAssessmentCrashWindow;
    private final int crashPredictionForwardWindow;
    
    // Technical Analysis Parameters
    private final int supportResistanceWindow;
    
    // Risk Assessment Parameters
    private final double defaultAverageVolatility;
    private final double baseRiskLevel;
    private final double annualRiskFreeRate;
    
    // Optimization Parameters
    private final OptimizationParameters optimization;
    
    /**
     * Private constructor - use builder to create instances.
     */
    private FinancialAnalysisParameters(Builder builder) {
        this.crashAsymmetryThreshold = builder.crashAsymmetryThreshold;
        this.crashMinScale = builder.crashMinScale;
        this.crashMaxScale = builder.crashMaxScale;
        this.crashNumScales = builder.crashNumScales;
        
        this.volatilityLowThreshold = builder.volatilityLowThreshold;
        this.volatilityMediumThreshold = builder.volatilityMediumThreshold;
        this.volatilityHighThreshold = builder.volatilityHighThreshold;
        this.volumeDivergenceThreshold = builder.volumeDivergenceThreshold;
        this.priceDivergenceThreshold = builder.priceDivergenceThreshold;
        
        this.cycleTestFrequencies = builder.cycleTestFrequencies.clone();
        
        this.trendMinScale = builder.trendMinScale;
        this.trendMaxScale = builder.trendMaxScale;
        this.trendNumScales = builder.trendNumScales;
        
        this.regimeDetectionLookbackPeriod = builder.regimeDetectionLookbackPeriod;
        this.regimeTrendThreshold = builder.regimeTrendThreshold;
        
        this.signalGenerationMinHistory = builder.signalGenerationMinHistory;
        this.recentCrashLookbackWindow = builder.recentCrashLookbackWindow;
        this.crashProbabilityNormalization = builder.crashProbabilityNormalization;
        this.riskAssessmentCrashWindow = builder.riskAssessmentCrashWindow;
        this.crashPredictionForwardWindow = builder.crashPredictionForwardWindow;
        
        this.supportResistanceWindow = builder.supportResistanceWindow;
        
        this.defaultAverageVolatility = builder.defaultAverageVolatility;
        this.baseRiskLevel = builder.baseRiskLevel;
        this.annualRiskFreeRate = builder.annualRiskFreeRate;
        
        this.optimization = builder.optimization.build();
    }
    
    /**
     * Creates a new builder with default values from FinancialAnalysisConfig.
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Creates default parameters matching the static configuration.
     */
    public static FinancialAnalysisParameters defaultParameters() {
        return builder().build();
    }
    
    // Getters
    public double getCrashAsymmetryThreshold() { return crashAsymmetryThreshold; }
    public double getCrashMinScale() { return crashMinScale; }
    public double getCrashMaxScale() { return crashMaxScale; }
    public int getCrashNumScales() { return crashNumScales; }
    
    public double getVolatilityLowThreshold() { return volatilityLowThreshold; }
    public double getVolatilityMediumThreshold() { return volatilityMediumThreshold; }
    public double getVolatilityHighThreshold() { return volatilityHighThreshold; }
    public double getVolumeDivergenceThreshold() { return volumeDivergenceThreshold; }
    public double getPriceDivergenceThreshold() { return priceDivergenceThreshold; }
    
    public double[] getCycleTestFrequencies() { return cycleTestFrequencies.clone(); }
    
    public double getTrendMinScale() { return trendMinScale; }
    public double getTrendMaxScale() { return trendMaxScale; }
    public int getTrendNumScales() { return trendNumScales; }
    
    public int getRegimeDetectionLookbackPeriod() { return regimeDetectionLookbackPeriod; }
    public double getRegimeTrendThreshold() { return regimeTrendThreshold; }
    
    public int getSignalGenerationMinHistory() { return signalGenerationMinHistory; }
    public int getRecentCrashLookbackWindow() { return recentCrashLookbackWindow; }
    public double getCrashProbabilityNormalization() { return crashProbabilityNormalization; }
    public int getRiskAssessmentCrashWindow() { return riskAssessmentCrashWindow; }
    public int getCrashPredictionForwardWindow() { return crashPredictionForwardWindow; }
    
    public int getSupportResistanceWindow() { return supportResistanceWindow; }
    
    public double getDefaultAverageVolatility() { return defaultAverageVolatility; }
    public double getBaseRiskLevel() { return baseRiskLevel; }
    public double getAnnualRiskFreeRate() { return annualRiskFreeRate; }
    
    public OptimizationParameters getOptimization() { return optimization; }
    
    /**
     * Builder for FinancialAnalysisParameters.
     */
    public static final class Builder {
        // Initialize with defaults
        private double crashAsymmetryThreshold = 10.0;
        private double crashMinScale = 1.0;
        private double crashMaxScale = 10.0;
        private int crashNumScales = 20;
        
        private double volatilityLowThreshold = 0.5;
        private double volatilityMediumThreshold = 1.5;
        private double volatilityHighThreshold = 3.0;
        private double volumeDivergenceThreshold = 2.0;
        private double priceDivergenceThreshold = 0.01;
        
        private double[] cycleTestFrequencies = {0.2, 0.1, 0.045, 0.02};
        
        private double trendMinScale = 10.0;
        private double trendMaxScale = 50.0;
        private int trendNumScales = 10;
        
        private int regimeDetectionLookbackPeriod = 20;
        private double regimeTrendThreshold = 0.05;
        
        private int signalGenerationMinHistory = 20;
        private int recentCrashLookbackWindow = 20;
        private double crashProbabilityNormalization = 20.0;
        private int riskAssessmentCrashWindow = 20;
        private int crashPredictionForwardWindow = 5;
        
        private int supportResistanceWindow = 20;
        
        private double defaultAverageVolatility = 0.02;
        private double baseRiskLevel = 0.5;
        private double annualRiskFreeRate = 0.03; // Default 3% annual risk-free rate
        
        private OptimizationParameters.Builder optimization = OptimizationParameters.builder();
        
        // Crash Detection setters
        public Builder crashAsymmetryThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Crash asymmetry threshold must be positive");
            }
            this.crashAsymmetryThreshold = threshold;
            return this;
        }
        
        public Builder crashScaleRange(double minScale, double maxScale, int numScales) {
            if (minScale <= 0 || maxScale <= minScale) {
                throw new IllegalArgumentException("Invalid scale range");
            }
            if (numScales <= 0) {
                throw new IllegalArgumentException("Number of scales must be positive");
            }
            this.crashMinScale = minScale;
            this.crashMaxScale = maxScale;
            this.crashNumScales = numScales;
            return this;
        }
        
        // Volatility Analysis setters
        public Builder volatilityThresholds(double low, double medium, double high) {
            if (low <= 0 || medium <= low || high <= medium) {
                throw new IllegalArgumentException("Volatility thresholds must be positive and increasing");
            }
            this.volatilityLowThreshold = low;
            this.volatilityMediumThreshold = medium;
            this.volatilityHighThreshold = high;
            return this;
        }
        
        public Builder volatilityLowThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Volatility threshold must be positive");
            }
            this.volatilityLowThreshold = threshold;
            return this;
        }
        
        public Builder volumeDivergenceThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Volume divergence threshold must be positive");
            }
            this.volumeDivergenceThreshold = threshold;
            return this;
        }
        
        public Builder priceDivergenceThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Price divergence threshold must be positive");
            }
            this.priceDivergenceThreshold = threshold;
            return this;
        }
        
        // Cyclical Analysis setters
        public Builder cycleTestFrequencies(double... frequencies) {
            if (frequencies == null || frequencies.length == 0) {
                throw new IllegalArgumentException("Cycle test frequencies cannot be empty");
            }
            for (double freq : frequencies) {
                if (freq <= 0 || freq >= 1) {
                    throw new IllegalArgumentException("Frequencies must be between 0 and 1");
                }
            }
            this.cycleTestFrequencies = frequencies.clone();
            return this;
        }
        
        // Trend Analysis setters
        public Builder trendScaleRange(double minScale, double maxScale, int numScales) {
            if (minScale <= 0 || maxScale <= minScale) {
                throw new IllegalArgumentException("Invalid scale range");
            }
            if (numScales <= 0) {
                throw new IllegalArgumentException("Number of scales must be positive");
            }
            this.trendMinScale = minScale;
            this.trendMaxScale = maxScale;
            this.trendNumScales = numScales;
            return this;
        }
        
        // Market Regime setters
        public Builder regimeDetectionLookbackPeriod(int period) {
            if (period <= 0) {
                throw new IllegalArgumentException("Lookback period must be positive");
            }
            this.regimeDetectionLookbackPeriod = period;
            return this;
        }
        
        public Builder regimeTrendThreshold(double threshold) {
            if (threshold <= 0) {
                throw new IllegalArgumentException("Regime trend threshold must be positive");
            }
            this.regimeTrendThreshold = threshold;
            return this;
        }
        
        // Trading Signal setters
        public Builder signalGenerationMinHistory(int minHistory) {
            if (minHistory <= 0) {
                throw new IllegalArgumentException("Minimum history must be positive");
            }
            this.signalGenerationMinHistory = minHistory;
            return this;
        }
        
        public Builder recentCrashLookbackWindow(int window) {
            if (window <= 0) {
                throw new IllegalArgumentException("Lookback window must be positive");
            }
            this.recentCrashLookbackWindow = window;
            return this;
        }
        
        public Builder crashProbabilityNormalization(double normalization) {
            if (normalization <= 0) {
                throw new IllegalArgumentException("Normalization factor must be positive");
            }
            this.crashProbabilityNormalization = normalization;
            return this;
        }
        
        // Technical Analysis setters
        public Builder supportResistanceWindow(int window) {
            if (window <= 0) {
                throw new IllegalArgumentException("Support/resistance window must be positive");
            }
            this.supportResistanceWindow = window;
            return this;
        }
        
        // Risk Assessment setters
        public Builder defaultAverageVolatility(double volatility) {
            if (volatility <= 0) {
                throw new IllegalArgumentException("Average volatility must be positive");
            }
            this.defaultAverageVolatility = volatility;
            return this;
        }
        
        public Builder baseRiskLevel(double risk) {
            if (risk < 0 || risk > 1) {
                throw new IllegalArgumentException("Base risk level must be between 0 and 1");
            }
            this.baseRiskLevel = risk;
            return this;
        }
        
        public Builder annualRiskFreeRate(double rate) {
            if (rate < 0) {
                throw new IllegalArgumentException("Risk-free rate cannot be negative");
            }
            this.annualRiskFreeRate = rate;
            return this;
        }
        
        // Optimization parameters
        public Builder withOptimization(OptimizationParameters optimization) {
            if (optimization == null) {
                throw new IllegalArgumentException("Optimization parameters cannot be null");
            }
            this.optimization = OptimizationParameters.builder(optimization);
            return this;
        }
        
        public Builder optimizationBuilder(OptimizationParameters.Builder builder) {
            if (builder == null) {
                throw new IllegalArgumentException("Optimization builder cannot be null");
            }
            this.optimization = builder;
            return this;
        }
        
        public FinancialAnalysisParameters build() {
            return new FinancialAnalysisParameters(this);
        }
    }
}