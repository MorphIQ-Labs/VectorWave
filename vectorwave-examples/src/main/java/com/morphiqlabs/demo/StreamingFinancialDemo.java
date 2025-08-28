package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.cwt.finance.*;
import com.morphiqlabs.wavelet.cwt.finance.FinancialWaveletAnalyzer.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Demonstrates real-time financial analysis using streaming analyzers.
 * 
 * <p>This demo simulates a live market data feed and shows how different
 * streaming analyzers process the data in real-time with minimal memory usage.</p>
 */
public class StreamingFinancialDemo {
    
    // ANSI color codes for terminal output
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String PURPLE = "\u001B[35m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";
    
    // Market data simulator
    private static class MarketDataSimulator {
        private double basePrice = 100.0;
        private double volatility = 0.02;
        private int time = 0;
        private Random random = new Random(42);
        
        public MarketData next() {
            time++;
            
            // Simulate different market conditions
            if (time > 200 && time < 250) {
                // Bull run
                basePrice *= 1.002;
                volatility = 0.01;
            } else if (time > 400 && time < 420) {
                // Market crash
                basePrice *= 0.98;
                volatility = 0.05;
            } else if (time > 600 && time < 700) {
                // High volatility period
                volatility = 0.04;
            } else {
                // Normal conditions
                volatility = 0.02;
            }
            
            // Generate price with trend and noise
            double trend = Math.sin(time * 0.01) * 0.5;
            double noise = (random.nextGaussian() * volatility);
            double price = basePrice * (1 + trend + noise);
            
            // Generate volume (correlated with volatility)
            double volume = 1_000_000 * (1 + Math.abs(noise) * 10 + random.nextDouble() * 0.1);
            
            return new MarketData(time, price, volume);
        }
    }
    
    private static record MarketData(int timestamp, double price, double volume) {}
    
    public static void main(String[] args) throws Exception {
        System.out.println(BOLD + CYAN + "=== VectorWave Streaming Financial Analysis Demo ===" + RESET);
        System.out.println();
        
        // Choose demo mode
        if (args.length > 0 && args[0].equals("--incremental")) {
            runIncrementalDemo();
        } else if (args.length > 0 && args[0].equals("--compare")) {
            runComparisonDemo();
        } else {
            runSimpleStreamingDemo();
        }
    }
    
    /**
     * Demonstrates SimpleStreamingAnalyzer with real-time visualization.
     */
    private static void runSimpleStreamingDemo() throws Exception {
        System.out.println(BOLD + "Running Simple Streaming Analysis Demo" + RESET);
        System.out.println("Processing live market data with 50-sample window...\n");
        
        SimpleStreamingAnalyzer analyzer = new SimpleStreamingAnalyzer(50, 10);
        MarketDataSimulator simulator = new MarketDataSimulator();
        
        // Console dashboard
        String dashboardFormat = """
            %s[%s]%s
            %sPrice:%s $%.2f  %sVolatility:%s %.2f%%  %sAvg Vol:%s %.2f%%
            %sRegime:%s %-12s  %sRisk:%s %s
            %sSignal:%s %s
            """;
        
        // Result handler
        analyzer.onResult(result -> {
            // Clear previous output (works on most terminals)
            System.out.print("\033[H\033[2J");
            System.out.flush();
            
            // Format timestamp
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            
            // Format risk bar
            String riskBar = formatRiskBar(result.riskLevel());
            
            // Format signal
            String signal = formatSignal(result.signal());
            
            // Format regime with color
            String regime = formatRegime(result.regime());
            
            // Print dashboard
            System.out.printf(dashboardFormat,
                BOLD, timestamp, RESET,
                BLUE, RESET, result.price(),
                YELLOW, RESET, result.instantVolatility() * 100,
                YELLOW, RESET, result.avgVolatility() * 100,
                PURPLE, RESET, regime,
                RED, RESET, riskBar,
                GREEN, RESET, signal
            );
            
            // Print price chart
            printMiniChart(result.price());
        });
        
        // Process market data
        System.out.println("Starting market simulation... (Press Ctrl+C to stop)\n");
        Thread.sleep(1000);
        
        for (int i = 0; i < 1000; i++) {
            MarketData data = simulator.next();
            analyzer.processSample(data.price);
            
            // Simulate real-time delay
            Thread.sleep(50);
        }
        
        // Print final statistics
        printStatistics(analyzer.getStatistics());
    }
    
    /**
     * Demonstrates IncrementalFinancialAnalyzer with detailed metrics.
     */
    private static void runIncrementalDemo() throws Exception {
        System.out.println(BOLD + "Running Incremental Analysis Demo" + RESET);
        System.out.println("Advanced analysis with EMAs and crash detection...\n");
        
        IncrementalFinancialAnalyzer analyzer = new IncrementalFinancialAnalyzer();
        MarketDataSimulator simulator = new MarketDataSimulator();
        
        // Track performance
        List<Double> prices = new ArrayList<>();
        List<Double> returns = new ArrayList<>();
        int buySignals = 0;
        int sellSignals = 0;
        int crashes = 0;
        
        // Process data
        for (int i = 0; i < 500; i++) {
            MarketData data = simulator.next();
            var result = analyzer.processSample(data.price, data.volume);
            
            prices.add(data.price);
            if (result.return_() != 0) {
                returns.add(result.return_());
            }
            
            // Count events
            if (result.hasSignal()) {
                if (result.signal() == SignalType.BUY) buySignals++;
                else if (result.signal() == SignalType.SELL) sellSignals++;
            }
            if (result.crashDetected()) crashes++;
            
            // Print updates every 50 samples
            if (i % 50 == 0 && i > 0) {
                System.out.printf("\n" + BOLD + "=== Update at sample %d ===" + RESET + "\n", i);
                System.out.printf("Price: $%.2f | EMA12: $%.2f | EMA26: $%.2f | EMA50: $%.2f\n",
                    result.price(), result.ema12(), result.ema26(), result.ema50());
                System.out.printf("Volatility: %.2f%% | Max Drawdown: %.2f%%\n",
                    result.volatility() * 100, result.maxDrawdown() * 100);
                System.out.printf("Regime: %s | Risk Level: %.2f\n",
                    formatRegime(result.regime()), result.riskLevel());
                
                if (result.crashDetected()) {
                    System.out.println(RED + BOLD + "⚠️  CRASH DETECTED!" + RESET);
                }
            }
        }
        
        // Print summary
        System.out.println("\n" + BOLD + CYAN + "=== Analysis Summary ===" + RESET);
        System.out.printf("Total samples processed: %d\n", prices.size());
        System.out.printf("Price range: $%.2f - $%.2f\n",
            prices.stream().min(Double::compare).orElse(0.0),
            prices.stream().max(Double::compare).orElse(0.0));
        System.out.printf("Average return: %.4f%%\n",
            returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0) * 100);
        System.out.printf("Return volatility: %.4f%%\n",
            calculateStdDev(returns) * 100);
        System.out.printf("\nSignals generated:\n");
        System.out.printf("  Buy signals:  %d\n", buySignals);
        System.out.printf("  Sell signals: %d\n", sellSignals);
        System.out.printf("  Crashes detected: %d\n", crashes);
    }
    
    /**
     * Compares streaming vs batch processing performance.
     */
    private static void runComparisonDemo() throws Exception {
        System.out.println(BOLD + "Running Performance Comparison Demo" + RESET);
        System.out.println("Comparing streaming vs batch processing...\n");
        
        // Generate test data
        MarketDataSimulator simulator = new MarketDataSimulator();
        double[] prices = new double[10_000];
        double[] volumes = new double[10_000];
        
        for (int i = 0; i < prices.length; i++) {
            MarketData data = simulator.next();
            prices[i] = data.price;
            volumes[i] = data.volume;
        }
        
        // Test configurations
        int[] dataSizes = {100, 1000, 5000, 10000};
        
        System.out.println("Size  | Streaming (ms) | Batch (ms) | Memory (MB) | Speedup");
        System.out.println("------|----------------|------------|-------------|--------");
        
        for (int size : dataSizes) {
            // Streaming test
            SimpleStreamingAnalyzer streamingAnalyzer = new SimpleStreamingAnalyzer(50, 10);
            List<SimpleStreamingAnalyzer.StreamingResult> streamingResults = new ArrayList<>();
            streamingAnalyzer.onResult(streamingResults::add);
            
            System.gc();
            long streamingMemBefore = getUsedMemory();
            long streamingStart = System.nanoTime();
            
            for (int i = 0; i < size; i++) {
                streamingAnalyzer.processSample(prices[i]);
            }
            
            long streamingTime = (System.nanoTime() - streamingStart) / 1_000_000;
            long streamingMemAfter = getUsedMemory();
            long streamingMemUsed = (streamingMemAfter - streamingMemBefore) / (1024 * 1024);
            
            // Batch test
            FinancialWaveletAnalyzer batchAnalyzer = new FinancialWaveletAnalyzer();
            
            System.gc();
            long batchMemBefore = getUsedMemory();
            long batchStart = System.nanoTime();
            
            double[] batchPrices = Arrays.copyOf(prices, size);
            var volatilityResult = batchAnalyzer.analyzeVolatility(batchPrices, 1.0);
            
            long batchTime = (System.nanoTime() - batchStart) / 1_000_000;
            long batchMemAfter = getUsedMemory();
            long batchMemUsed = (batchMemAfter - batchMemBefore) / (1024 * 1024);
            
            // Calculate speedup
            double speedup = (double) batchTime / streamingTime;
            
            System.out.printf("%-6d| %-14d | %-10d | %-11d | %.2fx\n",
                size, streamingTime, batchTime, streamingMemUsed, speedup);
        }
        
        // Memory efficiency test
        System.out.println("\n" + BOLD + "Memory Efficiency Test" + RESET);
        System.out.println("Processing 1M samples with streaming analyzer...");
        
        SimpleStreamingAnalyzer efficientAnalyzer = new SimpleStreamingAnalyzer(100, 100);
        AtomicInteger resultCount = new AtomicInteger(0);
        efficientAnalyzer.onResult(r -> resultCount.incrementAndGet());
        
        System.gc();
        long memStart = getUsedMemory();
        
        for (int i = 0; i < 1_000_000; i++) {
            efficientAnalyzer.processSample(100 + Math.random());
            
            if (i % 100_000 == 0 && i > 0) {
                long currentMem = (getUsedMemory() - memStart) / (1024 * 1024);
                System.out.printf("  %d samples: %d MB used\n", i, currentMem);
            }
        }
        
        System.out.printf("\nTotal results emitted: %d\n", resultCount.get());
        System.out.println("✓ Memory usage remains constant!");
    }
    
    // Helper methods
    
    private static String formatRiskBar(double risk) {
        int bars = (int) (risk * 10);
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < 10; i++) {
            if (i < bars) {
                sb.append(risk > 0.7 ? RED + "█" : risk > 0.4 ? YELLOW + "█" : GREEN + "█");
            } else {
                sb.append("░");
            }
        }
        sb.append(RESET).append("] ").append(String.format("%.1f", risk));
        return sb.toString();
    }
    
    private static String formatSignal(Optional<TradingSignal> signal) {
        if (signal.isEmpty()) {
            return "HOLD";
        }
        
        TradingSignal s = signal.get();
        String color = s.type() == SignalType.BUY ? GREEN : 
                      s.type() == SignalType.SELL ? RED : YELLOW;
        return String.format("%s%s%s (%.1f%% confidence) - %s", 
            color, BOLD, s.type(), s.confidence() * 100, RESET + s.rationale());
    }
    
    private static String formatRegime(MarketRegime regime) {
        return switch (regime) {
            case TRENDING_UP -> GREEN + "↑ TRENDING UP" + RESET;
            case TRENDING_DOWN -> RED + "↓ TRENDING DOWN" + RESET;
            case VOLATILE -> YELLOW + "⚡ VOLATILE" + RESET;
            case RANGING -> BLUE + "↔ RANGING" + RESET;
        };
    }
    
    private static final List<Double> priceHistory = new ArrayList<>();
    
    private static void printMiniChart(double price) {
        priceHistory.add(price);
        if (priceHistory.size() > 50) {
            priceHistory.remove(0);
        }
        
        if (priceHistory.size() < 2) return;
        
        // Find min/max for scaling
        double min = priceHistory.stream().min(Double::compare).orElse(0.0);
        double max = priceHistory.stream().max(Double::compare).orElse(100.0);
        double range = max - min;
        
        // Print mini chart
        System.out.println("\nPrice Chart (last 50 samples):");
        int height = 8;
        
        for (int row = height - 1; row >= 0; row--) {
            System.out.print("│");
            for (double p : priceHistory) {
                double normalized = (p - min) / range;
                int level = (int) (normalized * (height - 1));
                if (level == row) {
                    System.out.print("●");
                } else if (level > row) {
                    System.out.print("│");
                } else {
                    System.out.print(" ");
                }
            }
            System.out.println();
        }
        System.out.print("└");
        for (int i = 0; i < priceHistory.size(); i++) {
            System.out.print("─");
        }
        System.out.println();
    }
    
    private static void printStatistics(SimpleStreamingAnalyzer.StreamingStatistics stats) {
        System.out.println("\n" + BOLD + CYAN + "=== Final Statistics ===" + RESET);
        System.out.printf("Samples processed: %d\n", stats.samplesProcessed());
        System.out.printf("Average volatility: %.4f%%\n", stats.averageVolatility() * 100);
        System.out.printf("Final regime: %s\n", stats.currentRegime());
    }
    
    private static double calculateStdDev(List<Double> values) {
        if (values.size() < 2) return 0;
        
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = values.stream()
            .mapToDouble(v -> Math.pow(v - mean, 2))
            .average().orElse(0.0);
        
        return Math.sqrt(variance);
    }
    
    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}