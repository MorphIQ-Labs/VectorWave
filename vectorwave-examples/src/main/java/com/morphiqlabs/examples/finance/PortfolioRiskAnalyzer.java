package com.morphiqlabs.examples.finance;

import com.morphiqlabs.financial.*;
import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.modwt.*;
import com.morphiqlabs.wavelet.WaveletOperations;
import com.morphiqlabs.wavelet.memory.MemoryPool;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Portfolio risk analysis using wavelet-based methods.
 * 
 * This example demonstrates:
 * - Value at Risk (VaR) calculation using wavelet coefficients
 * - Correlation analysis in wavelet domain
 * - Risk decomposition by time scales
 * - Stress testing with wavelet-based scenarios
 */
public class PortfolioRiskAnalyzer {
    
    private final FinancialConfig config;
    private final MODWTTransform transform;
    private final MemoryPool memoryPool;
    private final ExecutorService executor;
    
    // Portfolio data
    private final Map<String, Position> positions;
    private final Map<String, double[]> priceHistories;
    
    public PortfolioRiskAnalyzer(double riskFreeRate, Wavelet wavelet) {
        this.config = new FinancialConfig(riskFreeRate);
        this.transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        this.memoryPool = new MemoryPool();
        this.executor = ForkJoinPool.commonPool();
        this.positions = new ConcurrentHashMap<>();
        this.priceHistories = new ConcurrentHashMap<>();
        
        memoryPool.setMaxArraysPerSize(10); // Limit memory usage
    }
    
    /**
     * Add a position to the portfolio.
     */
    public void addPosition(String symbol, double quantity, double currentPrice, double[] priceHistory) {
        positions.put(symbol, new Position(symbol, quantity, currentPrice));
        priceHistories.put(symbol, Arrays.copyOf(priceHistory, priceHistory.length));
    }
    
    /**
     * Calculate comprehensive portfolio risk metrics.
     */
    public PortfolioRiskReport analyzeRisk() {
        // Calculate returns for each asset
        Map<String, double[]> returnsMap = calculateReturns();
        
        // Perform wavelet analysis on each asset
        Map<String, MODWTResult> waveletResults = performWaveletAnalysis(returnsMap);
        
        // Calculate portfolio metrics
        double portfolioValue = calculatePortfolioValue();
        double portfolioVolatility = calculatePortfolioVolatility(returnsMap);
        double portfolioSharpe = calculatePortfolioSharpe(returnsMap);
        
        // Calculate VaR using wavelet method
        VaRResult varResult = calculateWaveletVaR(waveletResults, 0.95, 1); // 95% 1-day VaR
        
        // Analyze correlations in wavelet domain
        CorrelationAnalysis correlations = analyzeWaveletCorrelations(waveletResults);
        
        // Decompose risk by time scales
        RiskDecomposition riskDecomp = decomposeRiskByScale(waveletResults);
        
        // Perform stress testing
        StressTestResults stressTests = performStressTesting(waveletResults);
        
        return new PortfolioRiskReport(
            portfolioValue,
            portfolioVolatility,
            portfolioSharpe,
            varResult,
            correlations,
            riskDecomp,
            stressTests,
            generateRiskAlerts(varResult, correlations, riskDecomp)
        );
    }
    
    /**
     * Calculate returns for all assets.
     */
    private Map<String, double[]> calculateReturns() {
        return priceHistories.entrySet().parallelStream()
            .collect(Collectors.toConcurrentMap(
                Map.Entry::getKey,
                entry -> {
                    double[] prices = entry.getValue();
                    double[] returns = memoryPool.borrowArray(prices.length - 1);
                    try {
                        for (int i = 0; i < returns.length; i++) {
                            returns[i] = (prices[i + 1] - prices[i]) / prices[i];
                        }
                        return Arrays.copyOf(returns, returns.length);
                    } finally {
                        memoryPool.returnArray(returns);
                    }
                }
            ));
    }
    
    /**
     * Perform wavelet analysis on all assets.
     */
    private Map<String, MODWTResult> performWaveletAnalysis(Map<String, double[]> returnsMap) {
        return returnsMap.entrySet().parallelStream()
            .collect(Collectors.toConcurrentMap(
                Map.Entry::getKey,
                entry -> transform.forward(entry.getValue())
            ));
    }
    
    /**
     * Calculate Value at Risk using wavelet coefficients.
     */
    private VaRResult calculateWaveletVaR(Map<String, MODWTResult> waveletResults, 
                                         double confidenceLevel, int horizon) {
        // Aggregate portfolio returns in wavelet domain
        int numAssets = positions.size();
        if (numAssets == 0) return new VaRResult(0, 0, 0);
        
        // Get coefficient length from first result
        int coeffLength = waveletResults.values().iterator().next().getSignalLength();
        double[] portfolioDetail = new double[coeffLength];
        double[] portfolioApprox = new double[coeffLength];
        
        // Weight coefficients by position values
        double totalValue = calculatePortfolioValue();
        
        for (Map.Entry<String, MODWTResult> entry : waveletResults.entrySet()) {
            String symbol = entry.getKey();
            MODWTResult result = entry.getValue();
            Position position = positions.get(symbol);
            double weight = (position.quantity * position.currentPrice) / totalValue;
            
            double[] detail = result.detailCoeffs();
            double[] approx = result.approximationCoeffs();
            
            for (int i = 0; i < coeffLength; i++) {
                portfolioDetail[i] += weight * detail[i];
                portfolioApprox[i] += weight * approx[i];
            }
        }
        
        // Reconstruct portfolio returns
        MODWTResult portfolioResult = MODWTResult.create(portfolioApprox, portfolioDetail);
        double[] portfolioReturns = transform.inverse(portfolioResult);
        
        // Calculate VaR from returns distribution
        Arrays.sort(portfolioReturns);
        int varIndex = (int) ((1 - confidenceLevel) * portfolioReturns.length);
        double var1Day = -portfolioReturns[varIndex] * totalValue;
        
        // Scale to desired horizon
        double varHorizon = var1Day * Math.sqrt(horizon);
        
        // Calculate Expected Shortfall (CVaR)
        double sum = 0;
        for (int i = 0; i <= varIndex; i++) {
            sum += portfolioReturns[i];
        }
        double expectedShortfall = -(sum / (varIndex + 1)) * totalValue * Math.sqrt(horizon);
        
        return new VaRResult(var1Day, varHorizon, expectedShortfall);
    }
    
    /**
     * Analyze correlations in wavelet domain.
     */
    private CorrelationAnalysis analyzeWaveletCorrelations(Map<String, MODWTResult> waveletResults) {
        List<String> symbols = new ArrayList<>(waveletResults.keySet());
        int n = symbols.size();
        
        // Calculate correlations for detail coefficients (high frequency)
        double[][] detailCorrelations = new double[n][n];
        
        // Calculate correlations for approximation coefficients (low frequency)
        double[][] approxCorrelations = new double[n][n];
        
        for (int i = 0; i < n; i++) {
            for (int j = i; j < n; j++) {
                if (i == j) {
                    detailCorrelations[i][j] = 1.0;
                    approxCorrelations[i][j] = 1.0;
                } else {
                    MODWTResult result1 = waveletResults.get(symbols.get(i));
                    MODWTResult result2 = waveletResults.get(symbols.get(j));
                    
                    detailCorrelations[i][j] = calculateCorrelation(
                        result1.detailCoeffs(), result2.detailCoeffs());
                    detailCorrelations[j][i] = detailCorrelations[i][j];
                    
                    approxCorrelations[i][j] = calculateCorrelation(
                        result1.approximationCoeffs(), result2.approximationCoeffs());
                    approxCorrelations[j][i] = approxCorrelations[i][j];
                }
            }
        }
        
        // Find highest correlations
        List<CorrelationPair> highCorrelations = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double avgCorr = (detailCorrelations[i][j] + approxCorrelations[i][j]) / 2;
                if (Math.abs(avgCorr) > 0.7) {
                    highCorrelations.add(new CorrelationPair(
                        symbols.get(i), symbols.get(j), avgCorr,
                        detailCorrelations[i][j], approxCorrelations[i][j]
                    ));
                }
            }
        }
        
        return new CorrelationAnalysis(symbols, detailCorrelations, approxCorrelations, highCorrelations);
    }
    
    /**
     * Decompose risk by time scale using wavelet coefficients.
     */
    private RiskDecomposition decomposeRiskByScale(Map<String, MODWTResult> waveletResults) {
        double totalRisk = 0;
        double shortTermRisk = 0; // Detail coefficients
        double longTermRisk = 0;  // Approximation coefficients
        
        double totalValue = calculatePortfolioValue();
        
        for (Map.Entry<String, MODWTResult> entry : waveletResults.entrySet()) {
            String symbol = entry.getKey();
            MODWTResult result = entry.getValue();
            Position position = positions.get(symbol);
            double weight = (position.quantity * position.currentPrice) / totalValue;
            
            // Calculate variance contribution from each scale
            double detailVar = calculateVariance(result.detailCoeffs());
            double approxVar = calculateVariance(result.approximationCoeffs());
            
            shortTermRisk += weight * weight * detailVar;
            longTermRisk += weight * weight * approxVar;
            totalRisk += weight * weight * (detailVar + approxVar);
        }
        
        // Convert to standard deviation
        shortTermRisk = Math.sqrt(shortTermRisk);
        longTermRisk = Math.sqrt(longTermRisk);
        totalRisk = Math.sqrt(totalRisk);
        
        // Calculate percentages
        double shortTermPct = (shortTermRisk / totalRisk) * 100;
        double longTermPct = (longTermRisk / totalRisk) * 100;
        
        return new RiskDecomposition(totalRisk, shortTermRisk, longTermRisk, 
                                    shortTermPct, longTermPct);
    }
    
    /**
     * Perform stress testing using wavelet-based scenarios.
     */
    private StressTestResults performStressTesting(Map<String, MODWTResult> waveletResults) {
        List<StressScenario> scenarios = new ArrayList<>();
        
        // Scenario 1: High-frequency shock (detail coefficients amplified)
        scenarios.add(testScenario("Market Flash Crash", waveletResults, 3.0, 1.0));
        
        // Scenario 2: Trend reversal (approximation coefficients reversed)
        scenarios.add(testScenario("Trend Reversal", waveletResults, 1.0, -1.0));
        
        // Scenario 3: Volatility spike (all coefficients amplified)
        scenarios.add(testScenario("Volatility Spike", waveletResults, 2.0, 2.0));
        
        // Scenario 4: Correlation breakdown (randomize cross-correlations)
        scenarios.add(testCorrelationBreakdown(waveletResults));
        
        return new StressTestResults(scenarios);
    }
    
    private StressScenario testScenario(String name, Map<String, MODWTResult> waveletResults,
                                      double detailMultiplier, double approxMultiplier) {
        double portfolioValue = calculatePortfolioValue();
        double stressedValue = 0;
        
        for (Map.Entry<String, MODWTResult> entry : waveletResults.entrySet()) {
            String symbol = entry.getKey();
            MODWTResult result = entry.getValue();
            Position position = positions.get(symbol);
            
            // Apply stress to coefficients
            double[] stressedDetail = new double[result.detailCoeffs().length];
            double[] stressedApprox = new double[result.approximationCoeffs().length];
            
            for (int i = 0; i < stressedDetail.length; i++) {
                stressedDetail[i] = result.detailCoeffs()[i] * detailMultiplier;
                stressedApprox[i] = result.approximationCoeffs()[i] * approxMultiplier;
            }
            
            // Reconstruct stressed returns
            MODWTResult stressedResult = MODWTResult.create(stressedApprox, stressedDetail);
            double[] stressedReturns = transform.inverse(stressedResult);
            
            // Calculate stressed position value
            double stressedPrice = position.currentPrice;
            for (double return_ : stressedReturns) {
                stressedPrice *= (1 + return_);
            }
            stressedValue += position.quantity * stressedPrice;
        }
        
        double loss = portfolioValue - stressedValue;
        double lossPct = (loss / portfolioValue) * 100;
        
        return new StressScenario(name, loss, lossPct);
    }
    
    private StressScenario testCorrelationBreakdown(Map<String, MODWTResult> waveletResults) {
        // Simulate correlation breakdown by randomizing phases
        Random random = new Random(42);
        double portfolioValue = calculatePortfolioValue();
        double stressedValue = 0;
        
        for (Map.Entry<String, MODWTResult> entry : waveletResults.entrySet()) {
            String symbol = entry.getKey();
            MODWTResult result = entry.getValue();
            Position position = positions.get(symbol);
            
            // Randomly shuffle detail coefficients to break correlations
            double[] shuffledDetail = Arrays.copyOf(result.detailCoeffs(), result.detailCoeffs().length);
            Collections.shuffle(Arrays.asList(shuffledDetail), random);
            
            // Keep approximation unchanged (trend intact)
            MODWTResult stressedResult = MODWTResult.create(result.approximationCoeffs(), shuffledDetail);
            double[] stressedReturns = transform.inverse(stressedResult);
            
            // Calculate stressed value
            double stressedPrice = position.currentPrice;
            for (double return_ : stressedReturns) {
                stressedPrice *= (1 + return_);
            }
            stressedValue += position.quantity * stressedPrice;
        }
        
        double loss = Math.abs(portfolioValue - stressedValue); // Can go either way
        double lossPct = (loss / portfolioValue) * 100;
        
        return new StressScenario("Correlation Breakdown", loss, lossPct);
    }
    
    /**
     * Generate risk alerts based on analysis.
     */
    private List<RiskAlert> generateRiskAlerts(VaRResult var, CorrelationAnalysis corr, 
                                              RiskDecomposition decomp) {
        List<RiskAlert> alerts = new ArrayList<>();
        
        // VaR alerts
        double portfolioValue = calculatePortfolioValue();
        if (var.var1Day > portfolioValue * 0.05) {
            alerts.add(new RiskAlert(RiskLevel.HIGH, 
                String.format("1-day VaR (%.0f) exceeds 5%% of portfolio value", var.var1Day)));
        }
        
        // Correlation alerts
        for (CorrelationPair pair : corr.highCorrelations) {
            if (Math.abs(pair.correlation) > 0.9) {
                alerts.add(new RiskAlert(RiskLevel.MEDIUM,
                    String.format("Very high correlation (%.2f) between %s and %s",
                        pair.correlation, pair.symbol1, pair.symbol2)));
            }
        }
        
        // Risk decomposition alerts
        if (decomp.shortTermRiskPct > 70) {
            alerts.add(new RiskAlert(RiskLevel.MEDIUM,
                String.format("High short-term risk concentration: %.1f%%", decomp.shortTermRiskPct)));
        }
        
        return alerts;
    }
    
    // Helper methods
    
    private double calculatePortfolioValue() {
        return positions.values().stream()
            .mapToDouble(p -> p.quantity * p.currentPrice)
            .sum();
    }
    
    private double calculatePortfolioVolatility(Map<String, double[]> returnsMap) {
        // Simplified: assumes equal weights
        double variance = 0;
        int count = 0;
        
        for (double[] returns : returnsMap.values()) {
            variance += calculateVariance(returns);
            count++;
        }
        
        return Math.sqrt(variance / count) * Math.sqrt(252); // Annualized
    }
    
    private double calculatePortfolioSharpe(Map<String, double[]> returnsMap) {
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        
        // Calculate weighted average Sharpe
        double totalValue = calculatePortfolioValue();
        double weightedSharpe = 0;
        
        for (Map.Entry<String, double[]> entry : returnsMap.entrySet()) {
            Position position = positions.get(entry.getKey());
            double weight = (position.quantity * position.currentPrice) / totalValue;
            double sharpe = analyzer.calculateWaveletSharpeRatio(entry.getValue());
            weightedSharpe += weight * sharpe;
        }
        
        return weightedSharpe;
    }
    
    private double calculateCorrelation(double[] x, double[] y) {
        double meanX = Arrays.stream(x).average().orElse(0);
        double meanY = Arrays.stream(y).average().orElse(0);
        
        double covXY = 0, varX = 0, varY = 0;
        for (int i = 0; i < x.length; i++) {
            double dx = x[i] - meanX;
            double dy = y[i] - meanY;
            covXY += dx * dy;
            varX += dx * dx;
            varY += dy * dy;
        }
        
        return covXY / Math.sqrt(varX * varY);
    }
    
    private double calculateVariance(double[] data) {
        double mean = Arrays.stream(data).average().orElse(0);
        return Arrays.stream(data)
            .map(x -> (x - mean) * (x - mean))
            .average().orElse(0);
    }
    
    // Data classes
    
    private static class Position {
        final String symbol;
        final double quantity;
        final double currentPrice;
        
        Position(String symbol, double quantity, double currentPrice) {
            this.symbol = symbol;
            this.quantity = quantity;
            this.currentPrice = currentPrice;
        }
    }
    
    public static class PortfolioRiskReport {
        public final double portfolioValue;
        public final double portfolioVolatility;
        public final double portfolioSharpe;
        public final VaRResult var;
        public final CorrelationAnalysis correlations;
        public final RiskDecomposition riskDecomposition;
        public final StressTestResults stressTests;
        public final List<RiskAlert> alerts;
        
        PortfolioRiskReport(double portfolioValue, double portfolioVolatility,
                           double portfolioSharpe, VaRResult var,
                           CorrelationAnalysis correlations, RiskDecomposition riskDecomposition,
                           StressTestResults stressTests, List<RiskAlert> alerts) {
            this.portfolioValue = portfolioValue;
            this.portfolioVolatility = portfolioVolatility;
            this.portfolioSharpe = portfolioSharpe;
            this.var = var;
            this.correlations = correlations;
            this.riskDecomposition = riskDecomposition;
            this.stressTests = stressTests;
            this.alerts = alerts;
        }
        
        public void printSummary() {
            System.out.println("Portfolio Risk Report");
            System.out.println("====================");
            System.out.printf("Portfolio Value: $%,.2f%n", portfolioValue);
            System.out.printf("Annualized Volatility: %.2f%%%n", portfolioVolatility * 100);
            System.out.printf("Sharpe Ratio: %.2f%n", portfolioSharpe);
            System.out.println();
            
            System.out.println("Value at Risk (95% confidence):");
            System.out.printf("  1-day VaR: $%,.2f (%.2f%%)%n", var.var1Day, 
                (var.var1Day / portfolioValue) * 100);
            System.out.printf("  Expected Shortfall: $%,.2f%n", var.expectedShortfall);
            System.out.println();
            
            System.out.println("Risk Decomposition:");
            System.out.printf("  Short-term risk: %.2f%% of total%n", riskDecomposition.shortTermRiskPct);
            System.out.printf("  Long-term risk: %.2f%% of total%n", riskDecomposition.longTermRiskPct);
            System.out.println();
            
            System.out.println("Stress Test Results:");
            for (StressScenario scenario : stressTests.scenarios) {
                System.out.printf("  %s: $%,.2f loss (%.2f%%)%n", 
                    scenario.name, scenario.loss, scenario.lossPct);
            }
            
            if (!alerts.isEmpty()) {
                System.out.println("\nRisk Alerts:");
                for (RiskAlert alert : alerts) {
                    System.out.printf("  [%s] %s%n", alert.level, alert.message);
                }
            }
        }
    }
    
    private static class VaRResult {
        final double var1Day;
        final double varHorizon;
        final double expectedShortfall;
        
        VaRResult(double var1Day, double varHorizon, double expectedShortfall) {
            this.var1Day = var1Day;
            this.varHorizon = varHorizon;
            this.expectedShortfall = expectedShortfall;
        }
    }
    
    private static class CorrelationAnalysis {
        final List<String> symbols;
        final double[][] detailCorrelations;
        final double[][] approxCorrelations;
        final List<CorrelationPair> highCorrelations;
        
        CorrelationAnalysis(List<String> symbols, double[][] detailCorrelations,
                           double[][] approxCorrelations, List<CorrelationPair> highCorrelations) {
            this.symbols = symbols;
            this.detailCorrelations = detailCorrelations;
            this.approxCorrelations = approxCorrelations;
            this.highCorrelations = highCorrelations;
        }
    }
    
    private static class CorrelationPair {
        final String symbol1;
        final String symbol2;
        final double correlation;
        final double detailCorr;
        final double approxCorr;
        
        CorrelationPair(String symbol1, String symbol2, double correlation,
                       double detailCorr, double approxCorr) {
            this.symbol1 = symbol1;
            this.symbol2 = symbol2;
            this.correlation = correlation;
            this.detailCorr = detailCorr;
            this.approxCorr = approxCorr;
        }
    }
    
    private static class RiskDecomposition {
        final double totalRisk;
        final double shortTermRisk;
        final double longTermRisk;
        final double shortTermRiskPct;
        final double longTermRiskPct;
        
        RiskDecomposition(double totalRisk, double shortTermRisk, double longTermRisk,
                         double shortTermRiskPct, double longTermRiskPct) {
            this.totalRisk = totalRisk;
            this.shortTermRisk = shortTermRisk;
            this.longTermRisk = longTermRisk;
            this.shortTermRiskPct = shortTermRiskPct;
            this.longTermRiskPct = longTermRiskPct;
        }
    }
    
    private static class StressTestResults {
        final List<StressScenario> scenarios;
        
        StressTestResults(List<StressScenario> scenarios) {
            this.scenarios = scenarios;
        }
    }
    
    private static class StressScenario {
        final String name;
        final double loss;
        final double lossPct;
        
        StressScenario(String name, double loss, double lossPct) {
            this.name = name;
            this.loss = loss;
            this.lossPct = lossPct;
        }
    }
    
    private static class RiskAlert {
        final RiskLevel level;
        final String message;
        
        RiskAlert(RiskLevel level, String message) {
            this.level = level;
            this.message = message;
        }
    }
    
    private enum RiskLevel {
        LOW, MEDIUM, HIGH
    }
    
    // Demo usage
    public static void main(String[] args) {
        // Create analyzer with current risk-free rate
        PortfolioRiskAnalyzer analyzer = new PortfolioRiskAnalyzer(0.045, Daubechies.DB4);
        
        // Simulate historical price data
        Random random = new Random(42);
        int historyLength = 252; // 1 year of daily data
        
        // Add portfolio positions with simulated price histories
        String[] symbols = {"AAPL", "GOOGL", "JPM", "GLD", "TLT"}; // Tech, Finance, Gold, Bonds
        double[] quantities = {100, 50, 200, 300, 500};
        double[] currentPrices = {150.0, 2800.0, 150.0, 180.0, 95.0};
        double[] volatilities = {0.02, 0.025, 0.018, 0.015, 0.008}; // Different volatilities
        
        for (int i = 0; i < symbols.length; i++) {
            double[] priceHistory = new double[historyLength];
            priceHistory[0] = currentPrices[i] * 0.9; // Start 10% lower
            
            for (int t = 1; t < historyLength; t++) {
                double return_ = volatilities[i] * random.nextGaussian();
                priceHistory[t] = priceHistory[t-1] * (1 + return_);
            }
            priceHistory[historyLength-1] = currentPrices[i]; // End at current price
            
            analyzer.addPosition(symbols[i], quantities[i], currentPrices[i], priceHistory);
        }
        
        // Analyze portfolio risk
        System.out.println("Analyzing portfolio risk using wavelet methods...");
        PortfolioRiskReport report = analyzer.analyzeRisk();
        
        // Print comprehensive report
        report.printSummary();
        
        // Print correlation matrix
        System.out.println("\nHigh Correlations:");
        for (CorrelationPair pair : report.correlations.highCorrelations) {
            System.out.printf("%s <-> %s: %.3f (detail: %.3f, trend: %.3f)%n",
                pair.symbol1, pair.symbol2, pair.correlation, pair.detailCorr, pair.approxCorr);
        }
    }
}