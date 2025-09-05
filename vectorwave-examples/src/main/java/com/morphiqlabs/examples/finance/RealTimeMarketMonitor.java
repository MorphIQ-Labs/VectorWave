package com.morphiqlabs.examples.finance;

import com.morphiqlabs.financial.*;
import com.morphiqlabs.wavelet.WaveletOperations;
import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.modwt.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Real-time market monitoring application using wavelet analysis.
 * 
 * This example demonstrates:
 * - Concurrent processing of multiple assets
 * - Real-time crash detection using wavelet asymmetry
 * - Volatility regime detection
 * - Thread-safe design for production use
 */
public class RealTimeMarketMonitor {
    
    // Configuration
    private final FinancialConfig financialConfig;
    private final FinancialAnalysisConfig analysisConfig;
    
    // Thread-safe analyzers
    private final ConcurrentHashMap<String, FinancialWaveletAnalyzer> waveletAnalyzers;
    private final ConcurrentHashMap<String, FinancialAnalyzer> standardAnalyzers;
    private final ConcurrentHashMap<String, MODWTTransform> transforms;
    
    // Market data storage (in production, this would be a time-series database)
    private final ConcurrentHashMap<String, CircularBuffer> priceBuffers;
    
    // Alert callbacks
    private final List<AlertListener> alertListeners;
    
    // Executor for parallel processing
    private final ExecutorService executor;
    
    public RealTimeMarketMonitor(double riskFreeRate) {
        // Initialize configuration
        this.financialConfig = new FinancialConfig(riskFreeRate);
        this.analysisConfig = FinancialAnalysisConfig.builder()
            .crashAsymmetryThreshold(0.7)
            .volatilityLowThreshold(0.5)
            .volatilityHighThreshold(2.0)
            .regimeTrendThreshold(0.02)
            .anomalyDetectionThreshold(3.0)
            .windowSize(252) // 1 year of daily data
            .confidenceLevel(0.95)
            .build();
        
        // Initialize thread-safe collections
        this.waveletAnalyzers = new ConcurrentHashMap<>();
        this.standardAnalyzers = new ConcurrentHashMap<>();
        this.transforms = new ConcurrentHashMap<>();
        this.priceBuffers = new ConcurrentHashMap<>();
        this.alertListeners = new CopyOnWriteArrayList<>();
        
        // Use ForkJoinPool for work-stealing parallelism
        this.executor = ForkJoinPool.commonPool();
        
        // Check platform capabilities
        WaveletOperations.PerformanceInfo perfInfo = WaveletOperations.getPerformanceInfo();
        System.out.println("Market Monitor initialized with: " + perfInfo.description());
    }
    
    /**
     * Add a symbol to monitor.
     */
    public void addSymbol(String symbol, int bufferSize) {
        priceBuffers.putIfAbsent(symbol, new CircularBuffer(bufferSize));
        
        // Pre-create analyzers for thread safety
        waveletAnalyzers.computeIfAbsent(symbol, 
            k -> new FinancialWaveletAnalyzer(financialConfig));
        standardAnalyzers.computeIfAbsent(symbol,
            k -> new FinancialAnalyzer(analysisConfig));
        transforms.computeIfAbsent(symbol,
            k -> new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC));
    }
    
    /**
     * Process incoming price tick.
     */
    public void onPriceTick(String symbol, double price, long timestamp) {
        CircularBuffer buffer = priceBuffers.get(symbol);
        if (buffer == null) {
            throw new IllegalArgumentException("Symbol not registered: " + symbol);
        }
        
        // Add price to buffer
        buffer.add(price);
        
        // Analyze if we have enough data
        if (buffer.size() >= 100) { // Minimum data for meaningful analysis
            executor.submit(() -> analyzeSymbol(symbol, timestamp));
        }
    }
    
    /**
     * Batch update for multiple symbols (e.g., from market data feed).
     */
    public void onMarketSnapshot(Map<String, Double> prices, long timestamp) {
        // Process all updates in parallel
        CompletableFuture<?>[] futures = prices.entrySet().stream()
            .map(entry -> CompletableFuture.runAsync(() -> 
                onPriceTick(entry.getKey(), entry.getValue(), timestamp), executor))
            .toArray(CompletableFuture[]::new);
        
        // Wait for all updates to complete
        CompletableFuture.allOf(futures).join();
    }
    
    /**
     * Analyze a single symbol.
     */
    private void analyzeSymbol(String symbol, long timestamp) {
        try {
            CircularBuffer buffer = priceBuffers.get(symbol);
            double[] prices = buffer.toArray();
            double[] returns = calculateReturns(prices);
            
            // Standard analysis
            FinancialAnalyzer analyzer = standardAnalyzers.get(symbol);
            double asymmetry = analyzer.analyzeCrashAsymmetry(returns);
            double volatility = analyzer.analyzeVolatility(returns);
            boolean hasAnomalies = analyzer.detectAnomalies(returns);
            
            // Wavelet analysis
            FinancialWaveletAnalyzer waveletAnalyzer = waveletAnalyzers.get(symbol);
            double sharpeRatio = waveletAnalyzer.calculateSharpeRatio(returns);
            double waveletSharpe = waveletAnalyzer.calculateWaveletSharpeRatio(returns);
            
            // MODWT analysis for pattern detection
            MODWTTransform transform = transforms.get(symbol);
            MODWTResult modwtResult = transform.forward(returns);
            
            // Analyze wavelet coefficients for patterns
            MarketCondition condition = analyzeWaveletCoefficients(
                modwtResult, asymmetry, volatility, hasAnomalies);
            
            // Create analysis result
            AnalysisResult result = new AnalysisResult(
                symbol, timestamp, prices[prices.length - 1],
                asymmetry, volatility, sharpeRatio, waveletSharpe,
                hasAnomalies, condition
            );
            
            // Check for alerts
            checkAlerts(result);
            
        } catch (Exception e) {
            System.err.println("Error analyzing " + symbol + ": " + e.getMessage());
        }
    }
    
    /**
     * Analyze wavelet coefficients to determine market condition.
     */
    private MarketCondition analyzeWaveletCoefficients(
            MODWTResult result, double asymmetry, double volatility, boolean anomalies) {
        
        double[] detail = result.detailCoeffs();
        
        // Calculate energy in detail coefficients
        double energy = 0;
        for (double coeff : detail) {
            energy += coeff * coeff;
        }
        energy = Math.sqrt(energy / detail.length);
        
        // Determine market condition based on multiple factors
        if (anomalies && asymmetry > analysisConfig.getCrashAsymmetryThreshold()) {
            return MarketCondition.CRASH_WARNING;
        } else if (volatility > analysisConfig.getVolatilityHighThreshold()) {
            return MarketCondition.HIGH_VOLATILITY;
        } else if (energy > 2.0 * volatility) {
            return MarketCondition.REGIME_CHANGE;
        } else if (volatility < analysisConfig.getVolatilityLowThreshold()) {
            return MarketCondition.LOW_VOLATILITY;
        } else {
            return MarketCondition.NORMAL;
        }
    }
    
    /**
     * Check if alerts should be triggered.
     */
    private void checkAlerts(AnalysisResult result) {
        // Alert on crash warning
        if (result.condition == MarketCondition.CRASH_WARNING) {
            Alert alert = new Alert(
                AlertType.CRASH_WARNING,
                result.symbol,
                String.format("Crash warning: asymmetry=%.2f, volatility=%.2f",
                    result.asymmetry, result.volatility),
                result.timestamp
            );
            notifyListeners(alert);
        }
        
        // Alert on regime change
        if (result.condition == MarketCondition.REGIME_CHANGE) {
            Alert alert = new Alert(
                AlertType.REGIME_CHANGE,
                result.symbol,
                "Significant regime change detected in wavelet coefficients",
                result.timestamp
            );
            notifyListeners(alert);
        }
        
        // Alert on anomalies
        if (result.hasAnomalies) {
            Alert alert = new Alert(
                AlertType.ANOMALY,
                result.symbol,
                "Statistical anomaly detected in returns",
                result.timestamp
            );
            notifyListeners(alert);
        }
    }
    
    /**
     * Get current market summary.
     */
    public MarketSummary getMarketSummary() {
        Map<String, AnalysisResult> latestResults = new ConcurrentHashMap<>();
        
        // Analyze all symbols in parallel
        priceBuffers.keySet().parallelStream().forEach(symbol -> {
            try {
                CircularBuffer buffer = priceBuffers.get(symbol);
                if (buffer.size() >= 100) {
                    double[] prices = buffer.toArray();
                    double[] returns = calculateReturns(prices);
                    
                    FinancialAnalyzer analyzer = standardAnalyzers.get(symbol);
                    FinancialWaveletAnalyzer waveletAnalyzer = waveletAnalyzers.get(symbol);
                    
                    AnalysisResult result = new AnalysisResult(
                        symbol, System.currentTimeMillis(), prices[prices.length - 1],
                        analyzer.analyzeCrashAsymmetry(returns),
                        analyzer.analyzeVolatility(returns),
                        waveletAnalyzer.calculateSharpeRatio(returns),
                        waveletAnalyzer.calculateWaveletSharpeRatio(returns),
                        analyzer.detectAnomalies(returns),
                        MarketCondition.NORMAL
                    );
                    
                    latestResults.put(symbol, result);
                }
            } catch (Exception e) {
                // Log error but continue with other symbols
            }
        });
        
        return new MarketSummary(latestResults);
    }
    
    // Helper methods
    
    private double[] calculateReturns(double[] prices) {
        if (prices.length < 2) return new double[0];
        
        double[] returns = new double[prices.length - 1];
        for (int i = 0; i < returns.length; i++) {
            returns[i] = (prices[i + 1] - prices[i]) / prices[i];
        }
        return returns;
    }
    
    private void notifyListeners(Alert alert) {
        for (AlertListener listener : alertListeners) {
            executor.submit(() -> listener.onAlert(alert));
        }
    }
    
    public void addAlertListener(AlertListener listener) {
        alertListeners.add(listener);
    }
    
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
        }
    }
    
    // Inner classes
    
    private static class CircularBuffer {
        private final double[] buffer;
        private int head = 0;
        private int size = 0;
        
        CircularBuffer(int capacity) {
            this.buffer = new double[capacity];
        }
        
        synchronized void add(double value) {
            buffer[head] = value;
            head = (head + 1) % buffer.length;
            if (size < buffer.length) size++;
        }
        
        synchronized double[] toArray() {
            double[] result = new double[size];
            if (size == buffer.length) {
                // Buffer is full, copy in order
                System.arraycopy(buffer, head, result, 0, buffer.length - head);
                System.arraycopy(buffer, 0, result, buffer.length - head, head);
            } else {
                // Buffer not full, copy from beginning
                System.arraycopy(buffer, 0, result, 0, size);
            }
            return result;
        }
        
        synchronized int size() {
            return size;
        }
    }
    
    public static class AnalysisResult {
        public final String symbol;
        public final long timestamp;
        public final double price;
        public final double asymmetry;
        public final double volatility;
        public final double sharpeRatio;
        public final double waveletSharpe;
        public final boolean hasAnomalies;
        public final MarketCondition condition;
        
        AnalysisResult(String symbol, long timestamp, double price,
                      double asymmetry, double volatility,
                      double sharpeRatio, double waveletSharpe,
                      boolean hasAnomalies, MarketCondition condition) {
            this.symbol = symbol;
            this.timestamp = timestamp;
            this.price = price;
            this.asymmetry = asymmetry;
            this.volatility = volatility;
            this.sharpeRatio = sharpeRatio;
            this.waveletSharpe = waveletSharpe;
            this.hasAnomalies = hasAnomalies;
            this.condition = condition;
        }
    }
    
    public enum MarketCondition {
        NORMAL,
        HIGH_VOLATILITY,
        LOW_VOLATILITY,
        CRASH_WARNING,
        REGIME_CHANGE
    }
    
    public static class Alert {
        public final AlertType type;
        public final String symbol;
        public final String message;
        public final long timestamp;
        
        Alert(AlertType type, String symbol, String message, long timestamp) {
            this.type = type;
            this.symbol = symbol;
            this.message = message;
            this.timestamp = timestamp;
        }
    }
    
    public enum AlertType {
        CRASH_WARNING,
        REGIME_CHANGE,
        ANOMALY,
        HIGH_VOLATILITY
    }
    
    public interface AlertListener {
        void onAlert(Alert alert);
    }
    
    public static class MarketSummary {
        public final Map<String, AnalysisResult> results;
        public final long timestamp;
        public final int totalSymbols;
        public final int warningCount;
        
        MarketSummary(Map<String, AnalysisResult> results) {
            this.results = Collections.unmodifiableMap(results);
            this.timestamp = System.currentTimeMillis();
            this.totalSymbols = results.size();
            this.warningCount = (int) results.values().stream()
                .filter(r -> r.condition != MarketCondition.NORMAL)
                .count();
        }
    }
    
    // Demo usage
    public static void main(String[] args) throws InterruptedException {
        // Create monitor with current risk-free rate
        RealTimeMarketMonitor monitor = new RealTimeMarketMonitor(0.045); // 4.5%
        
        // Add alert listener
        monitor.addAlertListener(alert -> 
            System.out.printf("[ALERT] %s - %s: %s%n", 
                alert.type, alert.symbol, alert.message));
        
        // Register symbols to monitor
        String[] symbols = {"AAPL", "GOOGL", "MSFT", "AMZN", "JPM", "GS"};
        for (String symbol : symbols) {
            monitor.addSymbol(symbol, 500); // Keep last 500 prices
        }
        
        // Simulate market data feed
        Random random = new Random(42);
        double[] basePrices = {150.0, 2800.0, 300.0, 3300.0, 150.0, 400.0};
        
        System.out.println("Starting market simulation...");
        
        for (int t = 0; t < 1000; t++) {
            Map<String, Double> snapshot = new HashMap<>();
            
            // Generate prices with occasional volatility spikes
            for (int i = 0; i < symbols.length; i++) {
                double volatility = (t > 500 && t < 550) ? 0.05 : 0.01; // Spike at t=500
                double return_ = volatility * random.nextGaussian();
                basePrices[i] *= (1 + return_);
                snapshot.put(symbols[i], basePrices[i]);
            }
            
            // Feed prices to monitor
            monitor.onMarketSnapshot(snapshot, System.currentTimeMillis());
            
            // Print summary every 100 ticks
            if (t % 100 == 0) {
                MarketSummary summary = monitor.getMarketSummary();
                System.out.printf("T=%d: Monitoring %d symbols, %d warnings%n",
                    t, summary.totalSymbols, summary.warningCount);
            }
            
            Thread.sleep(10); // Simulate real-time delay
        }
        
        // Final summary
        MarketSummary finalSummary = monitor.getMarketSummary();
        System.out.println("\nFinal Market Summary:");
        for (Map.Entry<String, AnalysisResult> entry : finalSummary.results.entrySet()) {
            AnalysisResult r = entry.getValue();
            System.out.printf("%s: Price=%.2f, Volatility=%.3f, Sharpe=%.2f, Condition=%s%n",
                r.symbol, r.price, r.volatility, r.waveletSharpe, r.condition);
        }
        
        monitor.shutdown();
    }
}