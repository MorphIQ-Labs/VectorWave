package com.morphiqlabs.wavelet.cwt.finance;

import java.util.Optional;

/**
 * Parameter object for comprehensive market analysis requests.
 *
 * <p>This record encapsulates all parameters needed for market analysis,
 * replacing methods with many parameters and providing a fluent API.</p>
 *
 * @param priceData price time series
 * @param volumeData optional volume time series
 * @param samplingRate samples per unit time (e.g., Hz)
 * @param options analysis options
 */
public record MarketAnalysisRequest(
    double[] priceData,
    double[] volumeData,
    double samplingRate,
    AnalysisOptions options
) {
    
    /**
     * Analysis options with sensible defaults.
     *
     * @param detectCrashes enable crash detection
     * @param analyzeVolatility enable volatility analysis
     * @param findCycles enable cycle detection
     * @param generateSignals enable trading signal generation
     * @param crashThreshold asymmetry threshold for crash detection
     * @param volatilityWindow volatility window length (samples)
     * @param maxCycles maximum cycles to analyze
     * @param signalConfidence minimum confidence for signals
     */
    public record AnalysisOptions(
        boolean detectCrashes,
        boolean analyzeVolatility,
        boolean findCycles,
        boolean generateSignals,
        double crashThreshold,
        double volatilityWindow,
        int maxCycles,
        double signalConfidence
    ) {
        
        /**
         * Creates options with default values.
         */
        public static AnalysisOptions defaults() {
            return new AnalysisOptions(
                true,  // detectCrashes
                true,  // analyzeVolatility
                true,  // findCycles
                true,  // generateSignals
                10.0,  // crashThreshold (default asymmetry threshold)
                30.0,  // volatilityWindow (days)
                5,     // maxCycles
                0.8    // signalConfidence
            );
        }
        
        /**
         * Builder for custom analysis options.
         */
        public static class Builder {
            private boolean detectCrashes = true;
            private boolean analyzeVolatility = true;
            private boolean findCycles = true;
            private boolean generateSignals = true;
            private double crashThreshold = 10.0;
            private double volatilityWindow = 30.0;
            private int maxCycles = 5;
            private double signalConfidence = 0.8;
            
            public Builder detectCrashes(boolean detect) {
                this.detectCrashes = detect;
                return this;
            }
            
            public Builder analyzeVolatility(boolean analyze) {
                this.analyzeVolatility = analyze;
                return this;
            }
            
            public Builder findCycles(boolean find) {
                this.findCycles = find;
                return this;
            }
            
            public Builder generateSignals(boolean generate) {
                this.generateSignals = generate;
                return this;
            }
            
            public Builder crashThreshold(double threshold) {
                this.crashThreshold = threshold;
                return this;
            }
            
            public Builder volatilityWindow(double window) {
                this.volatilityWindow = window;
                return this;
            }
            
            public Builder maxCycles(int max) {
                this.maxCycles = max;
                return this;
            }
            
            public Builder signalConfidence(double confidence) {
                this.signalConfidence = confidence;
                return this;
            }
            
            public AnalysisOptions build() {
                return new AnalysisOptions(
                    detectCrashes, analyzeVolatility, findCycles, generateSignals,
                    crashThreshold, volatilityWindow, maxCycles, signalConfidence
                );
            }
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }
    
    /**
     * Creates a market analysis request with default options.
     */
    public static MarketAnalysisRequest of(double[] priceData, double samplingRate) {
        return new MarketAnalysisRequest(
            priceData, 
            null, 
            samplingRate, 
            AnalysisOptions.defaults()
        );
    }
    
    /**
     * Creates a market analysis request with price and volume data.
     */
    public static MarketAnalysisRequest of(double[] priceData, double[] volumeData, double samplingRate) {
        return new MarketAnalysisRequest(
            priceData, 
            volumeData, 
            samplingRate, 
            AnalysisOptions.defaults()
        );
    }
    
    /**
     * Builder for creating custom analysis requests.
     */
    public static class Builder {
        private double[] priceData;
        private double[] volumeData;
        private double samplingRate = 1.0; // Default daily sampling
        private AnalysisOptions options = AnalysisOptions.defaults();
        
        public Builder priceData(double[] data) {
            this.priceData = data;
            return this;
        }
        
        public Builder volumeData(double[] data) {
            this.volumeData = data;
            return this;
        }
        
        public Builder samplingRate(double rate) {
            this.samplingRate = rate;
            return this;
        }
        
        public Builder options(AnalysisOptions opts) {
            this.options = opts;
            return this;
        }
        
        public Builder configureOptions(java.util.function.Consumer<AnalysisOptions.Builder> configurator) {
            var builder = AnalysisOptions.builder();
            configurator.accept(builder);
            this.options = builder.build();
            return this;
        }
        
        public MarketAnalysisRequest build() {
            if (priceData == null || priceData.length == 0) {
                throw new IllegalStateException("Price data is required");
            }
            if (samplingRate <= 0) {
                throw new IllegalStateException("Sampling rate must be positive");
            }
            return new MarketAnalysisRequest(priceData, volumeData, samplingRate, options);
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Convenience methods for checking options.
     */
    public boolean hasVolumeData() {
        return volumeData != null && volumeData.length > 0;
    }
    
    public Optional<double[]> getVolumeData() {
        return Optional.ofNullable(volumeData);
    }
}
