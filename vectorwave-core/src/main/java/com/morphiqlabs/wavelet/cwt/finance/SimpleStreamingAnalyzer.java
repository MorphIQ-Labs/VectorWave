package com.morphiqlabs.wavelet.cwt.finance;

import com.morphiqlabs.wavelet.cwt.*;
import com.morphiqlabs.wavelet.cwt.finance.FinancialWaveletAnalyzer.*;

import java.util.*;
import java.util.function.Consumer;

/**
 * Simple streaming financial analyzer optimized for minimal memory usage.
 * 
 * <p>This analyzer processes financial data in a streaming fashion using
 * fixed-size sliding windows and incremental calculations.</p>
 */
public class SimpleStreamingAnalyzer {
    
    /**
     * Streaming result emitted for each update.
     *
     * @param index sample index of this result
     * @param price current price
     * @param instantVolatility instantaneous volatility
     * @param avgVolatility average volatility over window
     * @param regime detected market regime
     * @param riskLevel current risk level
     * @param signal optional trading signal
     */
    public record StreamingResult(
        int index,
        double price,
        double instantVolatility,
        double avgVolatility,
        MarketRegime regime,
        double riskLevel,
        Optional<TradingSignal> signal
    ) {}
    
    // Configuration
    private final int windowSize;
    private final int updateInterval;
    private final FinancialAnalysisParameters parameters;
    private final FinancialAnalysisObjectPool pool;
    
    // Circular buffers (simple arrays with index tracking)
    private final double[] priceWindow;
    private final double[] volatilityWindow;
    private int windowIndex = 0;
    private int samplesProcessed = 0;
    
    // Running statistics
    private double volatilitySum = 0;
    private double volatilityMean = 0;
    private MarketRegime currentRegime = MarketRegime.RANGING;
    
    // Wavelets
    private final PaulWavelet crashWavelet;
    private final CWTTransform crashTransform;
    
    // Result consumer
    private Consumer<StreamingResult> resultConsumer;
    
    /**
     * Creates a simple streaming analyzer.
     * 
     * @param windowSize size of sliding window
     * @param updateInterval how often to emit results
     */
    public SimpleStreamingAnalyzer(int windowSize, int updateInterval) {
        this.windowSize = windowSize;
        this.updateInterval = updateInterval;
        this.parameters = FinancialAnalysisParameters.defaultParameters();
        this.pool = new FinancialAnalysisObjectPool();
        
        this.priceWindow = new double[windowSize];
        this.volatilityWindow = new double[windowSize];
        
        this.crashWavelet = new PaulWavelet(4);
        this.crashTransform = new CWTTransform(crashWavelet);
    }
    
    /**
     * Set the result consumer.
     */
    public void onResult(Consumer<StreamingResult> consumer) {
        this.resultConsumer = consumer;
    }
    
    /**
     * Process a new price sample.
     */
    public void processSample(double price) {
        samplesProcessed++;
        
        // Update circular buffer
        int oldIndex = windowIndex;
        windowIndex = (windowIndex + 1) % windowSize;
        
        // Calculate instant volatility
        double instantVol = 0;
        if (samplesProcessed > 1) {
            double prevPrice = priceWindow[oldIndex];
            if (prevPrice > 0) {
                instantVol = Math.abs((price - prevPrice) / prevPrice);
            }
        }
        
        // Update windows
        double oldVolatility = volatilityWindow[windowIndex];
        priceWindow[windowIndex] = price;
        volatilityWindow[windowIndex] = instantVol;
        
        // Update running sum
        volatilitySum = volatilitySum - oldVolatility + instantVol;
        int effectiveSize = Math.min(samplesProcessed, windowSize);
        volatilityMean = effectiveSize > 0 ? volatilitySum / effectiveSize : 0;
        
        // Analyze and emit results at intervals
        if (samplesProcessed % updateInterval == 0 && resultConsumer != null) {
            analyzeAndEmit(price, instantVol);
        }
    }
    
    /**
     * Process a batch of prices.
     */
    public void processBatch(double[] prices) {
        for (double price : prices) {
            processSample(price);
        }
    }
    
    /**
     * Analyze current state and emit result.
     */
    private void analyzeAndEmit(double currentPrice, double instantVol) {
        // Detect regime
        currentRegime = detectRegime(instantVol);
        
        // Calculate risk level
        double riskLevel = calculateRiskLevel(instantVol);
        
        // Generate signal
        Optional<TradingSignal> signal = generateSignal(currentPrice, instantVol);
        
        // Emit result
        StreamingResult result = new StreamingResult(
            samplesProcessed,
            currentPrice,
            instantVol,
            volatilityMean,
            currentRegime,
            riskLevel,
            signal
        );
        
        resultConsumer.accept(result);
    }
    
    /**
     * Detect market regime based on volatility.
     */
    private MarketRegime detectRegime(double instantVol) {
        if (instantVol > volatilityMean * 2) {
            return MarketRegime.VOLATILE;
        }
        
        // Simple trend detection using price window
        int effectiveSize = Math.min(samplesProcessed, windowSize);
        if (effectiveSize < 10) {
            return MarketRegime.RANGING;
        }
        
        // Calculate simple trend
        double oldPrice = priceWindow[(windowIndex - effectiveSize + 1 + windowSize) % windowSize];
        double currentPrice = priceWindow[windowIndex];
        double trend = (currentPrice - oldPrice) / oldPrice;
        
        if (trend > parameters.getRegimeTrendThreshold()) {
            return MarketRegime.TRENDING_UP;
        } else if (trend < -parameters.getRegimeTrendThreshold()) {
            return MarketRegime.TRENDING_DOWN;
        } else {
            return MarketRegime.RANGING;
        }
    }
    
    /**
     * Calculate risk level based on volatility.
     */
    private double calculateRiskLevel(double instantVol) {
        double baseRisk = parameters.getBaseRiskLevel();
        double volComponent = 0;
        
        if (volatilityMean > 0) {
            volComponent = Math.min(0.5, (instantVol / volatilityMean - 1) * 0.2);
        }
        
        return Math.min(1.0, baseRisk + volComponent);
    }
    
    /**
     * Generate trading signal.
     */
    private Optional<TradingSignal> generateSignal(double currentPrice, double instantVol) {
        int effectiveSize = Math.min(samplesProcessed, windowSize);
        if (effectiveSize < 20) {
            return Optional.empty();
        }
        
        // Simple momentum signal
        double oldPrice = priceWindow[(windowIndex - 20 + windowSize) % windowSize];
        double momentum = (currentPrice - oldPrice) / oldPrice;
        
        if (momentum > 0.02 && instantVol < volatilityMean * 1.5) {
            return Optional.of(new TradingSignal(
                samplesProcessed,
                SignalType.BUY,
                Math.min(0.8, momentum * 10),
                "Positive momentum with controlled volatility"
            ));
        } else if (momentum < -0.02 || instantVol > volatilityMean * 2) {
            return Optional.of(new TradingSignal(
                samplesProcessed,
                SignalType.SELL,
                Math.min(0.9, instantVol / volatilityMean),
                "Negative momentum or high volatility"
            ));
        }
        
        return Optional.empty();
    }
    
    /**
     * Get current statistics.
     */
    public StreamingStatistics getStatistics() {
        return new StreamingStatistics(
            samplesProcessed,
            volatilityMean,
            currentRegime
        );
    }
    
    /**
     * Simple statistics.
     *
     * @param samplesProcessed number of samples processed
     * @param averageVolatility running average volatility
     * @param currentRegime current detected regime
     */
    public record StreamingStatistics(
        int samplesProcessed,
        double averageVolatility,
        MarketRegime currentRegime
    ) {}
}
