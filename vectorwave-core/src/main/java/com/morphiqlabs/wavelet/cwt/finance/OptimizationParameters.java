package com.morphiqlabs.wavelet.cwt.finance;

/**
 * Optimization parameters for financial wavelet analysis.
 * 
 * <p>Contains wavelet-specific parameters for different analysis types
 * (crash detection, volatility analysis, cycle detection, signal generation).</p>
 */
public final class OptimizationParameters {
    
    // Crash Detection
    private final int crashPaulOrder;
    private final int crashDogOrder;
    private final double crashThresholdFactor;
    private final double crashSeverityExponent;
    private final double[] crashScaleRange;
    
    // Volatility Analysis
    private final int volatilityPaulOrder;
    private final int volatilityDogOrder;
    private final double volatilityThresholdFactor;
    private final double volatilityExponent;
    private final double[] volatilityScaleRange;
    
    // Cycle Detection
    private final int cycleShannonFb;
    private final int cycleShannonFc;
    private final double cycleThresholdFactor;
    private final double cycleExponent;
    private final double[] cycleScaleRange;
    
    // Signal Generation
    private final int signalPaulOrder;
    private final int signalDogOrder;
    private final double signalThresholdFactor;
    private final double signalExponent;
    private final double[] signalScaleRange;
    
    private OptimizationParameters(Builder builder) {
        this.crashPaulOrder = builder.crashPaulOrder;
        this.crashDogOrder = builder.crashDogOrder;
        this.crashThresholdFactor = builder.crashThresholdFactor;
        this.crashSeverityExponent = builder.crashSeverityExponent;
        this.crashScaleRange = builder.crashScaleRange.clone();
        
        this.volatilityPaulOrder = builder.volatilityPaulOrder;
        this.volatilityDogOrder = builder.volatilityDogOrder;
        this.volatilityThresholdFactor = builder.volatilityThresholdFactor;
        this.volatilityExponent = builder.volatilityExponent;
        this.volatilityScaleRange = builder.volatilityScaleRange.clone();
        
        this.cycleShannonFb = builder.cycleShannonFb;
        this.cycleShannonFc = builder.cycleShannonFc;
        this.cycleThresholdFactor = builder.cycleThresholdFactor;
        this.cycleExponent = builder.cycleExponent;
        this.cycleScaleRange = builder.cycleScaleRange.clone();
        
        this.signalPaulOrder = builder.signalPaulOrder;
        this.signalDogOrder = builder.signalDogOrder;
        this.signalThresholdFactor = builder.signalThresholdFactor;
        this.signalExponent = builder.signalExponent;
        this.signalScaleRange = builder.signalScaleRange.clone();
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static Builder builder(OptimizationParameters source) {
        return new Builder(source);
    }
    
    // Getters
    public int getCrashPaulOrder() { return crashPaulOrder; }
    public int getCrashDogOrder() { return crashDogOrder; }
    public double getCrashThresholdFactor() { return crashThresholdFactor; }
    public double getCrashSeverityExponent() { return crashSeverityExponent; }
    public double[] getCrashScaleRange() { return crashScaleRange.clone(); }
    
    public int getVolatilityPaulOrder() { return volatilityPaulOrder; }
    public int getVolatilityDogOrder() { return volatilityDogOrder; }
    public double getVolatilityThresholdFactor() { return volatilityThresholdFactor; }
    public double getVolatilityExponent() { return volatilityExponent; }
    public double[] getVolatilityScaleRange() { return volatilityScaleRange.clone(); }
    
    public int getCycleShannonFb() { return cycleShannonFb; }
    public int getCycleShannonFc() { return cycleShannonFc; }
    public double getCycleThresholdFactor() { return cycleThresholdFactor; }
    public double getCycleExponent() { return cycleExponent; }
    public double[] getCycleScaleRange() { return cycleScaleRange.clone(); }
    
    public int getSignalPaulOrder() { return signalPaulOrder; }
    public int getSignalDogOrder() { return signalDogOrder; }
    public double getSignalThresholdFactor() { return signalThresholdFactor; }
    public double getSignalExponent() { return signalExponent; }
    public double[] getSignalScaleRange() { return signalScaleRange.clone(); }
    
    /**
     * Builder for OptimizationParameters.
     */
    public static final class Builder {
        // Initialize with defaults
        private int crashPaulOrder = 4;
        private int crashDogOrder = 2;
        private double crashThresholdFactor = 0.5;
        private double crashSeverityExponent = 1.5;
        private double[] crashScaleRange = {1.0, 10.0};
        
        private int volatilityPaulOrder = 3;
        private int volatilityDogOrder = 2;
        private double volatilityThresholdFactor = 0.3;
        private double volatilityExponent = 1.0;
        private double[] volatilityScaleRange = {1.0, 30.0};
        
        private int cycleShannonFb = 2;
        private int cycleShannonFc = 3;
        private double cycleThresholdFactor = 0.2;
        private double cycleExponent = 2.0;
        private double[] cycleScaleRange = {5.0, 50.0};
        
        private int signalPaulOrder = 4;
        private int signalDogOrder = 2;
        private double signalThresholdFactor = 0.4;
        private double signalExponent = 1.5;
        private double[] signalScaleRange = {2.0, 20.0};
        
        public Builder() {}
        
        public Builder(OptimizationParameters source) {
            this.crashPaulOrder = source.crashPaulOrder;
            this.crashDogOrder = source.crashDogOrder;
            this.crashThresholdFactor = source.crashThresholdFactor;
            this.crashSeverityExponent = source.crashSeverityExponent;
            this.crashScaleRange = source.crashScaleRange.clone();
            
            this.volatilityPaulOrder = source.volatilityPaulOrder;
            this.volatilityDogOrder = source.volatilityDogOrder;
            this.volatilityThresholdFactor = source.volatilityThresholdFactor;
            this.volatilityExponent = source.volatilityExponent;
            this.volatilityScaleRange = source.volatilityScaleRange.clone();
            
            this.cycleShannonFb = source.cycleShannonFb;
            this.cycleShannonFc = source.cycleShannonFc;
            this.cycleThresholdFactor = source.cycleThresholdFactor;
            this.cycleExponent = source.cycleExponent;
            this.cycleScaleRange = source.cycleScaleRange.clone();
            
            this.signalPaulOrder = source.signalPaulOrder;
            this.signalDogOrder = source.signalDogOrder;
            this.signalThresholdFactor = source.signalThresholdFactor;
            this.signalExponent = source.signalExponent;
            this.signalScaleRange = source.signalScaleRange.clone();
        }
        
        // Crash detection setters
        public Builder crashPaulOrder(int order) {
            if (order <= 0) {
                throw new IllegalArgumentException("Paul order must be positive");
            }
            this.crashPaulOrder = order;
            return this;
        }
        
        public Builder crashDogOrder(int order) {
            if (order <= 0) {
                throw new IllegalArgumentException("DOG order must be positive");
            }
            this.crashDogOrder = order;
            return this;
        }
        
        public Builder crashParameters(double thresholdFactor, double severityExponent) {
            if (thresholdFactor <= 0 || thresholdFactor >= 1) {
                throw new IllegalArgumentException("Threshold factor must be between 0 and 1");
            }
            if (severityExponent <= 0) {
                throw new IllegalArgumentException("Severity exponent must be positive");
            }
            this.crashThresholdFactor = thresholdFactor;
            this.crashSeverityExponent = severityExponent;
            return this;
        }
        
        public Builder crashScaleRange(double min, double max) {
            if (min <= 0 || max <= min) {
                throw new IllegalArgumentException("Invalid scale range");
            }
            this.crashScaleRange = new double[]{min, max};
            return this;
        }
        
        // Volatility analysis setters
        public Builder volatilityPaulOrder(int order) {
            if (order <= 0) {
                throw new IllegalArgumentException("Paul order must be positive");
            }
            this.volatilityPaulOrder = order;
            return this;
        }
        
        public Builder volatilityDogOrder(int order) {
            if (order <= 0) {
                throw new IllegalArgumentException("DOG order must be positive");
            }
            this.volatilityDogOrder = order;
            return this;
        }
        
        public Builder volatilityParameters(double thresholdFactor, double exponent) {
            if (thresholdFactor <= 0 || thresholdFactor >= 1) {
                throw new IllegalArgumentException("Threshold factor must be between 0 and 1");
            }
            if (exponent <= 0) {
                throw new IllegalArgumentException("Exponent must be positive");
            }
            this.volatilityThresholdFactor = thresholdFactor;
            this.volatilityExponent = exponent;
            return this;
        }
        
        public Builder volatilityScaleRange(double min, double max) {
            if (min <= 0 || max <= min) {
                throw new IllegalArgumentException("Invalid scale range");
            }
            this.volatilityScaleRange = new double[]{min, max};
            return this;
        }
        
        // Cycle detection setters
        public Builder cycleShannonParameters(int fb, int fc) {
            if (fb <= 0 || fc <= 0) {
                throw new IllegalArgumentException("Shannon parameters must be positive");
            }
            this.cycleShannonFb = fb;
            this.cycleShannonFc = fc;
            return this;
        }
        
        public Builder cycleParameters(double thresholdFactor, double exponent) {
            if (thresholdFactor <= 0 || thresholdFactor >= 1) {
                throw new IllegalArgumentException("Threshold factor must be between 0 and 1");
            }
            if (exponent <= 0) {
                throw new IllegalArgumentException("Exponent must be positive");
            }
            this.cycleThresholdFactor = thresholdFactor;
            this.cycleExponent = exponent;
            return this;
        }
        
        public Builder cycleScaleRange(double min, double max) {
            if (min <= 0 || max <= min) {
                throw new IllegalArgumentException("Invalid scale range");
            }
            this.cycleScaleRange = new double[]{min, max};
            return this;
        }
        
        // Signal generation setters
        public Builder signalPaulOrder(int order) {
            if (order <= 0) {
                throw new IllegalArgumentException("Paul order must be positive");
            }
            this.signalPaulOrder = order;
            return this;
        }
        
        public Builder signalDogOrder(int order) {
            if (order <= 0) {
                throw new IllegalArgumentException("DOG order must be positive");
            }
            this.signalDogOrder = order;
            return this;
        }
        
        public Builder signalParameters(double thresholdFactor, double exponent) {
            if (thresholdFactor <= 0 || thresholdFactor >= 1) {
                throw new IllegalArgumentException("Threshold factor must be between 0 and 1");
            }
            if (exponent <= 0) {
                throw new IllegalArgumentException("Exponent must be positive");
            }
            this.signalThresholdFactor = thresholdFactor;
            this.signalExponent = exponent;
            return this;
        }
        
        public Builder signalScaleRange(double min, double max) {
            if (min <= 0 || max <= min) {
                throw new IllegalArgumentException("Invalid scale range");
            }
            this.signalScaleRange = new double[]{min, max};
            return this;
        }
        
        public OptimizationParameters build() {
            return new OptimizationParameters(this);
        }
    }
}