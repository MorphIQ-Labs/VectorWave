package com.morphiqlabs.examples.finance;

import com.morphiqlabs.financial.*;
import com.morphiqlabs.wavelet.WaveletOperations;
import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.modwt.*;
import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingDenoiser;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Algorithmic trading signal generator using wavelet analysis.
 * 
 * This example demonstrates:
 * - Multi-timeframe analysis using wavelets
 * - Trend detection with MODWT
 * - Mean reversion signals from wavelet coefficients
 * - Real-time signal generation with streaming denoiser
 * - Risk-adjusted position sizing
 */
public class AlgorithmicTradingSignals {
    
    // Strategy parameters
    private final double riskFreeRate;
    private final double maxPositionSize;
    private final double stopLossPercent;
    private final double takeProfitPercent;
    
    // Wavelet transforms for different timeframes
    private final MODWTTransform shortTermTransform; // Haar for fast changes
    private final MODWTTransform mediumTermTransform; // DB4 for medium trends
    private final MODWTTransform longTermTransform; // DB4 for long trends
    
    // Streaming denoiser for real-time processing
    private final MODWTStreamingDenoiser denoiser;
    
    // Financial analyzers
    private final FinancialWaveletAnalyzer waveletAnalyzer;
    private final FinancialAnalyzer standardAnalyzer;
    
    // Trading state
    private final Map<String, TradingState> symbolStates;
    private final List<TradeSignal> signalHistory;
    private final List<TradeListener> tradeListeners;
    
    public AlgorithmicTradingSignals(double riskFreeRate, double maxPositionSize) {
        this.riskFreeRate = riskFreeRate;
        this.maxPositionSize = maxPositionSize;
        this.stopLossPercent = 0.02; // 2% stop loss
        this.takeProfitPercent = 0.04; // 4% take profit
        
        // Initialize transforms
        this.shortTermTransform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        this.mediumTermTransform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        this.longTermTransform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Initialize streaming denoiser for real-time noise reduction
        this.denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(Daubechies.DB4)
            .boundaryMode(BoundaryMode.PERIODIC)
            .bufferSize(100) // Process last 100 ticks
            .thresholdMethod(WaveletDenoiser.ThresholdMethod.UNIVERSAL)
            .thresholdType(WaveletDenoiser.ThresholdType.SOFT)
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
            .build();
        
        // Initialize analyzers
        FinancialConfig config = new FinancialConfig(riskFreeRate);
        this.waveletAnalyzer = new FinancialWaveletAnalyzer(config);
        
        FinancialAnalysisConfig analysisConfig = FinancialAnalysisConfig.builder()
            .crashAsymmetryThreshold(0.7)
            .volatilityLowThreshold(0.3)
            .volatilityHighThreshold(1.5)
            .regimeTrendThreshold(0.015)
            .anomalyDetectionThreshold(2.5)
            .windowSize(100)
            .confidenceLevel(0.95)
            .build();
        this.standardAnalyzer = new FinancialAnalyzer(analysisConfig);
        
        // Initialize collections
        this.symbolStates = new HashMap<>();
        this.signalHistory = new ArrayList<>();
        this.tradeListeners = new ArrayList<>();
        
        // Show platform info
        WaveletOperations.PerformanceInfo perfInfo = WaveletOperations.getPerformanceInfo();
        System.out.println("Trading system initialized with: " + perfInfo.description());
    }
    
    /**
     * Process new price data and generate trading signals.
     */
    public List<TradeSignal> processPriceTick(String symbol, double price, long timestamp) {
        // Get or create trading state
        TradingState state = symbolStates.computeIfAbsent(symbol, k -> new TradingState(symbol));
        
        // Update price history
        state.addPrice(price, timestamp);
        
        // Check if we have enough data
        if (state.getPriceCount() < 100) {
            return Collections.emptyList();
        }
        
        List<TradeSignal> signals = new ArrayList<>();
        
        // Get price and return data
        double[] prices = state.getRecentPrices(200);
        double[] returns = calculateReturns(prices);
        
        // Denoise returns in real-time
        double[] denoisedReturns = denoiser.denoise(returns);
        
        // Multi-timeframe wavelet analysis
        MultiTimeframeAnalysis mtfAnalysis = performMultiTimeframeAnalysis(denoisedReturns);
        
        // Generate signals based on different strategies
        signals.addAll(generateTrendFollowingSignals(symbol, state, mtfAnalysis, price));
        signals.addAll(generateMeanReversionSignals(symbol, state, mtfAnalysis, price));
        signals.addAll(generateBreakoutSignals(symbol, state, mtfAnalysis, price));
        signals.addAll(generateVolatilitySignals(symbol, state, mtfAnalysis, price));
        
        // Risk management: check existing positions
        signals = applyRiskManagement(symbol, state, signals, price);
        
        // Update state and notify listeners
        for (TradeSignal signal : signals) {
            state.updateWithSignal(signal);
            signalHistory.add(signal);
            notifyListeners(signal);
        }
        
        return signals;
    }
    
    /**
     * Perform multi-timeframe wavelet analysis.
     */
    private MultiTimeframeAnalysis performMultiTimeframeAnalysis(double[] returns) {
        // Short-term analysis (high-frequency movements)
        MODWTResult shortResult = shortTermTransform.forward(returns);
        double shortTermTrend = calculateTrendStrength(shortResult.approximationCoeffs());
        double shortTermVolatility = calculateVolatility(shortResult.detailCoeffs());
        
        // Medium-term analysis (swing trades)
        MODWTResult mediumResult = mediumTermTransform.forward(returns);
        double mediumTermTrend = calculateTrendStrength(mediumResult.approximationCoeffs());
        double mediumTermVolatility = calculateVolatility(mediumResult.detailCoeffs());
        
        // Long-term analysis (position trades)
        MODWTResult longResult = longTermTransform.forward(returns);
        double longTermTrend = calculateTrendStrength(longResult.approximationCoeffs());
        double longTermVolatility = calculateVolatility(longResult.detailCoeffs());
        
        // Analyze coefficient patterns
        boolean trendAlignment = (shortTermTrend > 0 && mediumTermTrend > 0 && longTermTrend > 0) ||
                                (shortTermTrend < 0 && mediumTermTrend < 0 && longTermTrend < 0);
        
        // Calculate signal quality metrics
        double signalClarity = Math.abs(mediumTermTrend) / (mediumTermVolatility + 0.001);
        double momentumStrength = calculateMomentum(mediumResult.detailCoeffs());
        
        return new MultiTimeframeAnalysis(
            shortResult, mediumResult, longResult,
            shortTermTrend, mediumTermTrend, longTermTrend,
            shortTermVolatility, mediumTermVolatility, longTermVolatility,
            trendAlignment, signalClarity, momentumStrength
        );
    }
    
    /**
     * Generate trend-following signals.
     */
    private List<TradeSignal> generateTrendFollowingSignals(String symbol, TradingState state,
                                                           MultiTimeframeAnalysis analysis, double currentPrice) {
        List<TradeSignal> signals = new ArrayList<>();
        
        // Strong trend with alignment across timeframes
        if (analysis.trendAlignment && analysis.signalClarity > 2.0) {
            SignalType type = analysis.mediumTermTrend > 0 ? SignalType.BUY : SignalType.SELL;
            double confidence = Math.min(0.9, analysis.signalClarity / 3.0);
            
            // Calculate position size based on volatility
            double positionSize = calculatePositionSize(analysis.mediumTermVolatility, confidence);
            
            // Set stops based on wavelet support/resistance
            double stopLoss = type == SignalType.BUY ? 
                currentPrice * (1 - stopLossPercent) : 
                currentPrice * (1 + stopLossPercent);
            double takeProfit = type == SignalType.BUY ? 
                currentPrice * (1 + takeProfitPercent) : 
                currentPrice * (1 - takeProfitPercent);
            
            TradeSignal signal = new TradeSignal(
                symbol, type, StrategyType.TREND_FOLLOWING,
                currentPrice, positionSize, confidence,
                stopLoss, takeProfit, System.currentTimeMillis(),
                "Strong trend alignment across timeframes"
            );
            
            signals.add(signal);
        }
        
        return signals;
    }
    
    /**
     * Generate mean reversion signals.
     */
    private List<TradeSignal> generateMeanReversionSignals(String symbol, TradingState state,
                                                          MultiTimeframeAnalysis analysis, double currentPrice) {
        List<TradeSignal> signals = new ArrayList<>();
        
        // Look for extreme deviations in short-term with stable long-term
        double[] shortDetail = analysis.shortResult.detailCoeffs();
        double meanDev = calculateMeanDeviation(shortDetail);
        
        if (Math.abs(meanDev) > 2.0 && Math.abs(analysis.longTermTrend) < 0.1) {
            // Overbought/oversold condition
            SignalType type = meanDev > 0 ? SignalType.SELL : SignalType.BUY;
            double confidence = Math.min(0.8, Math.abs(meanDev) / 3.0);
            
            double positionSize = calculatePositionSize(analysis.shortTermVolatility, confidence) * 0.7;
            
            // Tighter stops for mean reversion
            double stopLoss = type == SignalType.BUY ? 
                currentPrice * (1 - stopLossPercent * 0.5) : 
                currentPrice * (1 + stopLossPercent * 0.5);
            double takeProfit = type == SignalType.BUY ? 
                currentPrice * (1 + takeProfitPercent * 0.5) : 
                currentPrice * (1 - takeProfitPercent * 0.5);
            
            TradeSignal signal = new TradeSignal(
                symbol, type, StrategyType.MEAN_REVERSION,
                currentPrice, positionSize, confidence,
                stopLoss, takeProfit, System.currentTimeMillis(),
                String.format("Mean reversion: deviation=%.2f", meanDev)
            );
            
            signals.add(signal);
        }
        
        return signals;
    }
    
    /**
     * Generate breakout signals using wavelet energy.
     */
    private List<TradeSignal> generateBreakoutSignals(String symbol, TradingState state,
                                                     MultiTimeframeAnalysis analysis, double currentPrice) {
        List<TradeSignal> signals = new ArrayList<>();
        
        // Calculate wavelet energy concentration
        double[] mediumDetail = analysis.mediumResult.detailCoeffs();
        double recentEnergy = calculateRecentEnergy(mediumDetail, 10);
        double historicalEnergy = calculateHistoricalEnergy(mediumDetail, 50);
        
        // Breakout when recent energy significantly exceeds historical
        if (recentEnergy > historicalEnergy * 2.5 && analysis.momentumStrength > 0.5) {
            SignalType type = analysis.mediumTermTrend > 0 ? SignalType.BUY : SignalType.SELL;
            double confidence = Math.min(0.85, recentEnergy / (historicalEnergy * 3.0));
            
            double positionSize = calculatePositionSize(analysis.mediumTermVolatility, confidence);
            
            // Wider stops for breakouts
            double stopLoss = type == SignalType.BUY ? 
                currentPrice * (1 - stopLossPercent * 1.5) : 
                currentPrice * (1 + stopLossPercent * 1.5);
            double takeProfit = type == SignalType.BUY ? 
                currentPrice * (1 + takeProfitPercent * 2.0) : 
                currentPrice * (1 - takeProfitPercent * 2.0);
            
            TradeSignal signal = new TradeSignal(
                symbol, type, StrategyType.BREAKOUT,
                currentPrice, positionSize, confidence,
                stopLoss, takeProfit, System.currentTimeMillis(),
                String.format("Energy breakout: ratio=%.2f", recentEnergy / historicalEnergy)
            );
            
            signals.add(signal);
        }
        
        return signals;
    }
    
    /**
     * Generate volatility-based signals.
     */
    private List<TradeSignal> generateVolatilitySignals(String symbol, TradingState state,
                                                       MultiTimeframeAnalysis analysis, double currentPrice) {
        List<TradeSignal> signals = new ArrayList<>();
        
        // Volatility expansion/contraction patterns
        double volRatio = analysis.shortTermVolatility / analysis.longTermVolatility;
        
        // Trade volatility compression (potential breakout)
        if (volRatio < 0.5 && analysis.mediumTermVolatility < analysis.longTermVolatility * 0.7) {
            // Prepare for volatility expansion
            SignalType type = analysis.mediumTermTrend > 0 ? SignalType.BUY : SignalType.SELL;
            double confidence = 0.6; // Lower confidence for volatility plays
            
            double positionSize = calculatePositionSize(analysis.longTermVolatility, confidence) * 0.5;
            
            // Use options-like payoff with tight stop, wide target
            double stopLoss = type == SignalType.BUY ? 
                currentPrice * (1 - stopLossPercent * 0.75) : 
                currentPrice * (1 + stopLossPercent * 0.75);
            double takeProfit = type == SignalType.BUY ? 
                currentPrice * (1 + takeProfitPercent * 3.0) : 
                currentPrice * (1 - takeProfitPercent * 3.0);
            
            TradeSignal signal = new TradeSignal(
                symbol, type, StrategyType.VOLATILITY,
                currentPrice, positionSize, confidence,
                stopLoss, takeProfit, System.currentTimeMillis(),
                String.format("Volatility compression: ratio=%.2f", volRatio)
            );
            
            signals.add(signal);
        }
        
        return signals;
    }
    
    /**
     * Apply risk management rules to signals.
     */
    private List<TradeSignal> applyRiskManagement(String symbol, TradingState state, 
                                                  List<TradeSignal> signals, double currentPrice) {
        List<TradeSignal> filteredSignals = new ArrayList<>();
        
        for (TradeSignal signal : signals) {
            // Check position limits
            if (state.currentPosition != 0) {
                // Already have position - only allow signals in same direction or exit
                if ((state.currentPosition > 0 && signal.type == SignalType.SELL) ||
                    (state.currentPosition < 0 && signal.type == SignalType.BUY)) {
                    // This would be a reversal - check if it's justified
                    if (signal.confidence < 0.8) {
                        continue; // Skip weak reversal signals
                    }
                }
            }
            
            // Check daily loss limits
            if (state.dailyLossCount.get() >= 3) {
                // Too many losses today - only allow high confidence signals
                if (signal.confidence < 0.85) {
                    continue;
                }
            }
            
            // Adjust position size based on recent performance
            double performanceMultiplier = calculatePerformanceMultiplier(state);
            signal.positionSize *= performanceMultiplier;
            
            // Ensure position size doesn't exceed limits
            signal.positionSize = Math.min(signal.positionSize, maxPositionSize);
            
            filteredSignals.add(signal);
        }
        
        return filteredSignals;
    }
    
    // Helper calculation methods
    
    private double calculateTrendStrength(double[] coefficients) {
        // Calculate trend as slope of approximation coefficients
        int n = Math.min(20, coefficients.length);
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = coefficients.length - n; i < coefficients.length; i++) {
            double x = i - (coefficients.length - n);
            double y = coefficients[i];
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        return (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
    }
    
    private double calculateVolatility(double[] coefficients) {
        double sum = 0;
        int n = Math.min(20, coefficients.length);
        for (int i = coefficients.length - n; i < coefficients.length; i++) {
            sum += coefficients[i] * coefficients[i];
        }
        return Math.sqrt(sum / n);
    }
    
    private double calculateMomentum(double[] coefficients) {
        int n = Math.min(10, coefficients.length);
        double recent = 0, historical = 0;
        
        for (int i = coefficients.length - n; i < coefficients.length; i++) {
            recent += Math.abs(coefficients[i]);
        }
        for (int i = coefficients.length - 2*n; i < coefficients.length - n; i++) {
            historical += Math.abs(coefficients[i]);
        }
        
        return (recent - historical) / (historical + 0.001);
    }
    
    private double calculateMeanDeviation(double[] coefficients) {
        double mean = Arrays.stream(coefficients).average().orElse(0);
        double std = Math.sqrt(Arrays.stream(coefficients)
            .map(x -> (x - mean) * (x - mean))
            .average().orElse(0));
        
        return (coefficients[coefficients.length - 1] - mean) / (std + 0.001);
    }
    
    private double calculateRecentEnergy(double[] coefficients, int window) {
        double energy = 0;
        int start = Math.max(0, coefficients.length - window);
        for (int i = start; i < coefficients.length; i++) {
            energy += coefficients[i] * coefficients[i];
        }
        return Math.sqrt(energy / window);
    }
    
    private double calculateHistoricalEnergy(double[] coefficients, int window) {
        double energy = 0;
        int start = Math.max(0, coefficients.length - window - 10);
        int end = Math.max(start + 1, coefficients.length - 10);
        for (int i = start; i < end; i++) {
            energy += coefficients[i] * coefficients[i];
        }
        return Math.sqrt(energy / (end - start));
    }
    
    private double calculatePositionSize(double volatility, double confidence) {
        // Kelly-inspired position sizing
        double kellyFraction = confidence - (1 - confidence);
        double volatilityAdjustment = 0.02 / (volatility + 0.01); // Target 2% volatility
        return Math.max(0, Math.min(maxPositionSize, kellyFraction * volatilityAdjustment));
    }
    
    private double calculatePerformanceMultiplier(TradingState state) {
        // Reduce size after losses, increase after wins
        double winRate = state.getRecentWinRate();
        if (winRate > 0.6) return 1.2;
        if (winRate < 0.4) return 0.8;
        return 1.0;
    }
    
    private double[] calculateReturns(double[] prices) {
        double[] returns = new double[prices.length - 1];
        for (int i = 0; i < returns.length; i++) {
            returns[i] = (prices[i + 1] - prices[i]) / prices[i];
        }
        return returns;
    }
    
    private void notifyListeners(TradeSignal signal) {
        for (TradeListener listener : tradeListeners) {
            listener.onTradeSignal(signal);
        }
    }
    
    public void addTradeListener(TradeListener listener) {
        tradeListeners.add(listener);
    }
    
    // Data classes
    
    private static class TradingState {
        final String symbol;
        final List<PricePoint> priceHistory;
        final List<TradeSignal> recentSignals;
        double currentPosition;
        double averageEntryPrice;
        final AtomicInteger dailyLossCount;
        final AtomicInteger dailyWinCount;
        
        TradingState(String symbol) {
            this.symbol = symbol;
            this.priceHistory = new ArrayList<>();
            this.recentSignals = new ArrayList<>();
            this.currentPosition = 0;
            this.averageEntryPrice = 0;
            this.dailyLossCount = new AtomicInteger(0);
            this.dailyWinCount = new AtomicInteger(0);
        }
        
        void addPrice(double price, long timestamp) {
            priceHistory.add(new PricePoint(price, timestamp));
            // Keep only recent history
            if (priceHistory.size() > 1000) {
                priceHistory.remove(0);
            }
        }
        
        double[] getRecentPrices(int count) {
            int start = Math.max(0, priceHistory.size() - count);
            return priceHistory.subList(start, priceHistory.size()).stream()
                .mapToDouble(p -> p.price)
                .toArray();
        }
        
        int getPriceCount() {
            return priceHistory.size();
        }
        
        void updateWithSignal(TradeSignal signal) {
            recentSignals.add(signal);
            if (recentSignals.size() > 100) {
                recentSignals.remove(0);
            }
            
            // Update position
            if (signal.type == SignalType.BUY) {
                if (currentPosition >= 0) {
                    // Adding to long position
                    double totalCost = currentPosition * averageEntryPrice + signal.positionSize * signal.price;
                    currentPosition += signal.positionSize;
                    averageEntryPrice = totalCost / currentPosition;
                } else {
                    // Closing short, possibly going long
                    currentPosition += signal.positionSize;
                    if (currentPosition > 0) {
                        averageEntryPrice = signal.price;
                    }
                }
            } else {
                if (currentPosition <= 0) {
                    // Adding to short position
                    double totalCost = Math.abs(currentPosition) * averageEntryPrice + signal.positionSize * signal.price;
                    currentPosition -= signal.positionSize;
                    averageEntryPrice = totalCost / Math.abs(currentPosition);
                } else {
                    // Closing long, possibly going short
                    currentPosition -= signal.positionSize;
                    if (currentPosition < 0) {
                        averageEntryPrice = signal.price;
                    }
                }
            }
        }
        
        double getRecentWinRate() {
            if (recentSignals.size() < 10) return 0.5;
            
            long wins = recentSignals.stream()
                .filter(s -> s.confidence > 0.7) // Proxy for successful trades
                .count();
            return (double) wins / recentSignals.size();
        }
    }
    
    private static class PricePoint {
        final double price;
        final long timestamp;
        
        PricePoint(double price, long timestamp) {
            this.price = price;
            this.timestamp = timestamp;
        }
    }
    
    private static class MultiTimeframeAnalysis {
        final MODWTResult shortResult;
        final MODWTResult mediumResult;
        final MODWTResult longResult;
        final double shortTermTrend;
        final double mediumTermTrend;
        final double longTermTrend;
        final double shortTermVolatility;
        final double mediumTermVolatility;
        final double longTermVolatility;
        final boolean trendAlignment;
        final double signalClarity;
        final double momentumStrength;
        
        MultiTimeframeAnalysis(MODWTResult shortResult, MODWTResult mediumResult, MODWTResult longResult,
                              double shortTermTrend, double mediumTermTrend, double longTermTrend,
                              double shortTermVolatility, double mediumTermVolatility, double longTermVolatility,
                              boolean trendAlignment, double signalClarity, double momentumStrength) {
            this.shortResult = shortResult;
            this.mediumResult = mediumResult;
            this.longResult = longResult;
            this.shortTermTrend = shortTermTrend;
            this.mediumTermTrend = mediumTermTrend;
            this.longTermTrend = longTermTrend;
            this.shortTermVolatility = shortTermVolatility;
            this.mediumTermVolatility = mediumTermVolatility;
            this.longTermVolatility = longTermVolatility;
            this.trendAlignment = trendAlignment;
            this.signalClarity = signalClarity;
            this.momentumStrength = momentumStrength;
        }
    }
    
    public static class TradeSignal {
        public final String symbol;
        public final SignalType type;
        public final StrategyType strategy;
        public final double price;
        public double positionSize;
        public final double confidence;
        public final double stopLoss;
        public final double takeProfit;
        public final long timestamp;
        public final String reason;
        
        TradeSignal(String symbol, SignalType type, StrategyType strategy,
                   double price, double positionSize, double confidence,
                   double stopLoss, double takeProfit, long timestamp, String reason) {
            this.symbol = symbol;
            this.type = type;
            this.strategy = strategy;
            this.price = price;
            this.positionSize = positionSize;
            this.confidence = confidence;
            this.stopLoss = stopLoss;
            this.takeProfit = takeProfit;
            this.timestamp = timestamp;
            this.reason = reason;
        }
        
        @Override
        public String toString() {
            return String.format("%s %s %.4f @ %.2f (SL: %.2f, TP: %.2f) - %s [%.1f%% conf] - %s",
                type, symbol, positionSize, price, stopLoss, takeProfit, 
                strategy, confidence * 100, reason);
        }
    }
    
    public enum SignalType {
        BUY, SELL
    }
    
    public enum StrategyType {
        TREND_FOLLOWING,
        MEAN_REVERSION,
        BREAKOUT,
        VOLATILITY
    }
    
    public interface TradeListener {
        void onTradeSignal(TradeSignal signal);
    }
    
    // Demo usage
    public static void main(String[] args) throws InterruptedException {
        // Create trading system
        AlgorithmicTradingSignals tradingSystem = new AlgorithmicTradingSignals(0.045, 10000); // 4.5% risk-free rate, $10k max position
        
        // Add signal listener
        tradingSystem.addTradeListener(signal -> {
            System.out.println("[SIGNAL] " + signal);
        });
        
        // Simulate price feed
        String symbol = "AAPL";
        double basePrice = 150.0;
        Random random = new Random(42);
        
        System.out.println("Starting algorithmic trading simulation...");
        System.out.println("Initial price: $" + basePrice);
        System.out.println();
        
        // Generate 1000 price ticks
        for (int i = 0; i < 1000; i++) {
            // Simulate different market conditions
            double volatility;
            double trend;
            
            if (i < 200) {
                // Trending market
                volatility = 0.001;
                trend = 0.0001;
            } else if (i < 400) {
                // Volatile market
                volatility = 0.003;
                trend = -0.00005;
            } else if (i < 600) {
                // Range-bound market
                volatility = 0.0005;
                trend = 0;
            } else if (i < 800) {
                // Breakout
                volatility = 0.002;
                trend = 0.0002;
            } else {
                // Crash scenario
                volatility = 0.005;
                trend = -0.0003;
            }
            
            // Generate price
            double return_ = trend + volatility * random.nextGaussian();
            basePrice *= (1 + return_);
            
            // Process tick
            List<TradeSignal> signals = tradingSystem.processPriceTick(symbol, basePrice, System.currentTimeMillis());
            
            // Print summary every 100 ticks
            if (i % 100 == 0 && i > 0) {
                TradingState state = tradingSystem.symbolStates.get(symbol);
                System.out.printf("Tick %d: Price=%.2f, Position=%.2f, Signals=%d%n",
                    i, basePrice, state.currentPosition, tradingSystem.signalHistory.size());
            }
            
            Thread.sleep(10); // Simulate real-time delay
        }
        
        // Final summary
        System.out.println("\nTrading Summary:");
        System.out.println("Total signals generated: " + tradingSystem.signalHistory.size());
        
        // Count by strategy
        Map<StrategyType, Long> strategyCount = tradingSystem.signalHistory.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                s -> s.strategy, 
                java.util.stream.Collectors.counting()
            ));
        
        System.out.println("\nSignals by strategy:");
        for (Map.Entry<StrategyType, Long> entry : strategyCount.entrySet()) {
            System.out.printf("  %s: %d signals%n", entry.getKey(), entry.getValue());
        }
        
        // Average confidence by strategy
        Map<StrategyType, Double> avgConfidence = tradingSystem.signalHistory.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                s -> s.strategy,
                java.util.stream.Collectors.averagingDouble(s -> s.confidence)
            ));
        
        System.out.println("\nAverage confidence by strategy:");
        for (Map.Entry<StrategyType, Double> entry : avgConfidence.entrySet()) {
            System.out.printf("  %s: %.1f%%%n", entry.getKey(), entry.getValue() * 100);
        }
    }
}