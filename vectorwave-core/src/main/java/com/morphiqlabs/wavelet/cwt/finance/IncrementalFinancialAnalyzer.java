package com.morphiqlabs.wavelet.cwt.finance;

import com.morphiqlabs.wavelet.cwt.*;
import com.morphiqlabs.wavelet.cwt.finance.FinancialWaveletAnalyzer.*;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Incremental financial analyzer that updates analysis results efficiently
 * as new data arrives without reprocessing entire history.
 * 
 * <p>This analyzer maintains running statistics and incremental calculations
 * to minimize computational overhead for real-time applications.</p>
 * 
 * <p>Key optimizations:</p>
 * <ul>
 *   <li>Incremental volatility calculation using Welford's algorithm</li>
 *   <li>Sliding window crash detection with cached coefficients</li>
 *   <li>Exponential moving averages for trend detection</li>
 *   <li>Circular buffer reuse for memory efficiency</li>
 * </ul>
 */
public class IncrementalFinancialAnalyzer {
    
    /**
     * Incremental analysis state that can be updated efficiently.
     */
    public static class IncrementalState {
        // Price statistics
        private double lastPrice;
        private double ema12;  // 12-period EMA
        private double ema26;  // 26-period EMA
        private double ema50;  // 50-period EMA
        
        // Volatility statistics (Welford's algorithm)
        private int volCount;
        private double volMean;
        private double volM2;  // Sum of squares of differences from mean
        
        // Crash detection state
        private final CircularBuffer<CWTCoefficients> cwtHistory;
        private double maxAsymmetryScore;
        private int lastCrashIndex;
        
        // Risk metrics
        private double currentRiskLevel;
        private MarketRegime currentRegime;
        
        // Performance tracking
        private long samplesProcessed;
        private double maxDrawdown;
        private double peakPrice;
        
        IncrementalState(int historySize) {
            this.cwtHistory = new CircularBuffer<>(historySize);
            this.currentRegime = MarketRegime.RANGING;
            this.currentRiskLevel = 0.5;
            this.lastCrashIndex = -1;
        }
        
        /**
         * Update volatility using Welford's online algorithm.
         */
        void updateVolatility(double return_) {
            volCount++;
            double delta = return_ - volMean;
            volMean += delta / volCount;
            double delta2 = return_ - volMean;
            volM2 += delta * delta2;
        }
        
        /**
         * Get current volatility (standard deviation).
         */
        double getCurrentVolatility() {
            return volCount > 1 ? Math.sqrt(volM2 / (volCount - 1)) : 0.0;
        }
        
        /**
         * Update exponential moving averages.
         */
        void updateEMAs(double price) {
            if (ema12 == 0) {
                ema12 = ema26 = ema50 = price;
            } else {
                ema12 = updateEMA(price, ema12, 12);
                ema26 = updateEMA(price, ema26, 26);
                ema50 = updateEMA(price, ema50, 50);
            }
        }
        
        private double updateEMA(double price, double prevEMA, int period) {
            double alpha = 2.0 / (period + 1);
            return price * alpha + prevEMA * (1 - alpha);
        }
        
        /**
         * Update drawdown tracking.
         */
        void updateDrawdown(double price) {
            if (price > peakPrice) {
                peakPrice = price;
            } else {
                double drawdown = (peakPrice - price) / peakPrice;
                maxDrawdown = Math.max(maxDrawdown, drawdown);
            }
        }
    }
    
    /**
     * Cached CWT coefficients for sliding window.
     */
    private static class CWTCoefficients {
        final int timeIndex;
        final double[] coefficients;
        final double asymmetryScore;
        
        CWTCoefficients(int timeIndex, double[] coefficients, double asymmetryScore) {
            this.timeIndex = timeIndex;
            this.coefficients = coefficients;
            this.asymmetryScore = asymmetryScore;
        }
    }
    
    /**
     * Simple circular buffer implementation.
     */
    private static class CircularBuffer<T> {
        private final Object[] buffer;
        private int head = 0;
        private int size = 0;
        
        CircularBuffer(int capacity) {
            this.buffer = new Object[capacity];
        }
        
        void add(T item) {
            buffer[head] = item;
            head = (head + 1) % buffer.length;
            size = Math.min(size + 1, buffer.length);
        }
        
        @SuppressWarnings("unchecked")
        T get(int index) {
            if (index >= size) return null;
            int actualIndex = (head - size + index + buffer.length) % buffer.length;
            return (T) buffer[actualIndex];
        }
        
        int size() {
            return size;
        }
        
        void clear() {
            Arrays.fill(buffer, null);
            head = size = 0;
        }
    }
    
    // Core components
    private final FinancialAnalysisParameters parameters;
    private final FinancialAnalysisObjectPool pool;
    private final IncrementalState state;
    
    // Wavelets for analysis
    private final PaulWavelet crashWavelet;
    private final CWTTransform crashTransform;
    
    // Configuration
    private final int slidingWindowSize;
    private final int updateInterval;
    
    /**
     * Creates an incremental analyzer with default settings.
     */
    public IncrementalFinancialAnalyzer() {
        this(FinancialAnalysisParameters.defaultParameters(), 256, 10);
    }
    
    /**
     * Creates an incremental analyzer with custom settings.
     * 
     * @param parameters analysis parameters
     * @param windowSize sliding window size for CWT
     * @param updateInterval how often to update CWT (every N samples)
     */
    public IncrementalFinancialAnalyzer(
            FinancialAnalysisParameters parameters,
            int windowSize,
            int updateInterval) {
        this.parameters = parameters;
        this.pool = new FinancialAnalysisObjectPool();
        this.slidingWindowSize = windowSize;
        this.updateInterval = updateInterval;
        this.state = new IncrementalState(windowSize / updateInterval);
        
        // Initialize wavelets
        this.crashWavelet = new PaulWavelet(parameters.getOptimization().getCrashPaulOrder());
        this.crashTransform = new CWTTransform(crashWavelet);
    }
    
    /**
     * Process a new price sample and update all metrics incrementally.
     * 
     * @param price current price
     * @param volume current volume (optional, use 0 if not available)
     * @return updated analysis result
     */
    public IncrementalAnalysisResult processSample(double price, double volume) {
        state.samplesProcessed++;
        
        // Calculate return if we have previous price
        double return_ = 0;
        if (state.lastPrice > 0) {
            return_ = (price - state.lastPrice) / state.lastPrice;
            state.updateVolatility(Math.abs(return_));
        }
        
        // Update EMAs
        state.updateEMAs(price);
        
        // Update drawdown
        state.updateDrawdown(price);
        
        // Update crash detection periodically
        boolean crashDetected = false;
        if (state.samplesProcessed % updateInterval == 0) {
            crashDetected = updateCrashDetection(price);
        }
        
        // Update regime
        updateRegime(price, return_);
        
        // Update risk level
        updateRiskLevel(crashDetected);
        
        // Store current price for next iteration
        state.lastPrice = price;
        
        return createResult(price, volume, return_, crashDetected);
    }
    
    /**
     * Process a batch of samples efficiently.
     * 
     * @param prices array of prices
     * @param volumes array of volumes (can be null)
     * @return list of incremental results
     */
    public List<IncrementalAnalysisResult> processBatch(double[] prices, double[] volumes) {
        List<IncrementalAnalysisResult> results = new ArrayList<>(prices.length);
        
        for (int i = 0; i < prices.length; i++) {
            double volume = volumes != null ? volumes[i] : 0;
            results.add(processSample(prices[i], volume));
        }
        
        return results;
    }
    
    /**
     * Update crash detection using sliding window CWT.
     */
    private boolean updateCrashDetection(double currentPrice) {
        // Build price window from recent history
        int windowStart = (int) Math.max(0, state.samplesProcessed - slidingWindowSize);
        int windowSize = (int) (state.samplesProcessed - windowStart);
        
        if (windowSize < 10) return false;  // Need minimum data
        
        // This is simplified - in production, maintain a proper price history
        // For now, generate synthetic window based on EMAs
        FinancialAnalysisObjectPool.ArrayHolder windowHolder = pool.borrowArray(windowSize);
        
        try {
            double[] priceWindow = windowHolder.array;
            
            // Simplified: interpolate between EMAs
            for (int i = 0; i < windowSize; i++) {
                double t = (double) i / windowSize;
                priceWindow[i] = state.ema50 * (1 - t) + currentPrice * t;
            }
            
            // Analyze with CWT
            ScaleSpace scales = ScaleSpace.logarithmic(1.0, 10.0, 5);
            CWTResult result = crashTransform.analyze(
                Arrays.copyOfRange(priceWindow, 0, windowSize), scales);
            
            // Calculate asymmetry score
            double[][] coeffs = result.getCoefficients();
            double asymmetryScore = 0;
            
            for (int s = 0; s < scales.getNumScales(); s++) {
                double coeff = coeffs[s][windowSize - 1];
                if (coeff < 0) {
                    asymmetryScore += Math.abs(coeff) * scales.getScale(s);
                }
            }
            
            // Cache coefficients
            state.cwtHistory.add(new CWTCoefficients(
                (int) state.samplesProcessed,
                coeffs[coeffs.length - 1],  // Last scale
                asymmetryScore
            ));
            
            // Update max score
            state.maxAsymmetryScore = Math.max(state.maxAsymmetryScore, asymmetryScore);
            
            // Check for crash
            if (asymmetryScore > parameters.getCrashAsymmetryThreshold()) {
                state.lastCrashIndex = (int) state.samplesProcessed;
                return true;
            }
            
            return false;
            
        } finally {
            pool.returnArray(windowHolder);
        }
    }
    
    /**
     * Update market regime based on EMAs and volatility.
     */
    private void updateRegime(double price, double return_) {
        double volatility = state.getCurrentVolatility();
        double avgVolatility = parameters.getDefaultAverageVolatility();
        
        // MACD-like indicator
        double macd = state.ema12 - state.ema26;
        double signal = state.ema12 - state.ema50;
        
        if (volatility > avgVolatility * 2) {
            state.currentRegime = MarketRegime.VOLATILE;
        } else if (macd > 0 && signal > 0 && price > state.ema50) {
            state.currentRegime = MarketRegime.TRENDING_UP;
        } else if (macd < 0 && signal < 0 && price < state.ema50) {
            state.currentRegime = MarketRegime.TRENDING_DOWN;
        } else {
            state.currentRegime = MarketRegime.RANGING;
        }
    }
    
    /**
     * Update risk level based on multiple factors.
     */
    private void updateRiskLevel(boolean crashDetected) {
        double baseRisk = parameters.getBaseRiskLevel();
        double volatility = state.getCurrentVolatility();
        double avgVolatility = parameters.getDefaultAverageVolatility();
        
        // Start with base risk
        double risk = baseRisk;
        
        // Add volatility component
        risk += 0.3 * Math.min(volatility / avgVolatility, 2.0);
        
        // Add crash component
        if (crashDetected) {
            risk += 0.4;
        } else if (state.lastCrashIndex > 0 && 
                   state.samplesProcessed - state.lastCrashIndex < 50) {
            // Recent crash increases risk
            risk += 0.2;
        }
        
        // Add drawdown component
        risk += 0.1 * state.maxDrawdown;
        
        // Smooth risk level changes
        state.currentRiskLevel = 0.7 * state.currentRiskLevel + 0.3 * Math.min(risk, 1.0);
    }
    
    /**
     * Create analysis result from current state.
     */
    private IncrementalAnalysisResult createResult(
            double price, double volume, double return_, boolean crashDetected) {
        
        // Generate trading signal based on current state
        SignalType signal = SignalType.HOLD;
        double signalStrength = 0;
        
        if (state.currentRegime == MarketRegime.TRENDING_UP && 
            state.currentRiskLevel < 0.7) {
            signal = SignalType.BUY;
            signalStrength = Math.min(0.8, (state.ema12 - state.ema50) / state.ema50 * 100);
        } else if (state.currentRegime == MarketRegime.TRENDING_DOWN || 
                   state.currentRiskLevel > 0.8) {
            signal = SignalType.SELL;
            signalStrength = state.currentRiskLevel;
        }
        
        return new IncrementalAnalysisResult(
            state.samplesProcessed,
            price,
            volume,
            return_,
            state.getCurrentVolatility(),
            state.currentRegime,
            state.currentRiskLevel,
            signal,
            signalStrength,
            crashDetected,
            state.maxDrawdown,
            state.ema12,
            state.ema26,
            state.ema50
        );
    }
    
    /**
     * Get current state snapshot.
     */
    public IncrementalState getState() {
        return state;
    }
    
    /**
     * Reset analyzer state.
     */
    public void reset() {
        state.volCount = 0;
        state.volMean = 0;
        state.volM2 = 0;
        state.lastPrice = 0;
        state.ema12 = state.ema26 = state.ema50 = 0;
        state.cwtHistory.clear();
        state.maxAsymmetryScore = 0;
        state.lastCrashIndex = -1;
        state.currentRiskLevel = 0.5;
        state.currentRegime = MarketRegime.RANGING;
        state.samplesProcessed = 0;
        state.maxDrawdown = 0;
        state.peakPrice = 0;
    }
    
    /**
     * Result of incremental analysis.
     *
     * @param sampleIndex sequential sample index
     * @param price current price
     * @param volume current volume
     * @param return_ instantaneous return
     * @param volatility instantaneous volatility
     * @param regime detected market regime
     * @param riskLevel current risk level
     * @param signal detected signal type (if any)
     * @param signalStrength signal confidence/strength
     * @param crashDetected whether a crash event was detected
     * @param maxDrawdown running maximum drawdown
     * @param ema12 12-period EMA
     * @param ema26 26-period EMA
     * @param ema50 50-period EMA
     */
    public record IncrementalAnalysisResult(
        long sampleIndex,
        double price,
        double volume,
        double return_,
        double volatility,
        MarketRegime regime,
        double riskLevel,
        SignalType signal,
        double signalStrength,
        boolean crashDetected,
        double maxDrawdown,
        double ema12,
        double ema26,
        double ema50
    ) {
        /**
         * Check if this result contains a trading signal.
         */
        public boolean hasSignal() {
            return signal != SignalType.HOLD && signalStrength > 0;
        }
        
        /**
         * Check if this result indicates high risk.
         */
        public boolean isHighRisk() {
            return riskLevel > 0.8 || crashDetected;
        }
    }
}
