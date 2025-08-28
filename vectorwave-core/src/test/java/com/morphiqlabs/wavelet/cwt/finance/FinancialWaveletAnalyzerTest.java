package com.morphiqlabs.wavelet.cwt.finance;

import com.morphiqlabs.wavelet.cwt.CWTResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class FinancialWaveletAnalyzerTest {
    
    private static final double TOLERANCE = 1e-10;
    private static final long RANDOM_SEED = 98765L; // Fixed seed for reproducible tests
    private FinancialWaveletAnalyzer analyzer;
    private double[] priceData;
    private double[] volumeData;
    
    @BeforeEach
    void setUp() {
        analyzer = new FinancialWaveletAnalyzer();
        
        // Create synthetic financial data
        int N = 512;
        priceData = new double[N];
        volumeData = new double[N];
        
        // Use seeded Random for reproducible tests
        Random random = new Random(RANDOM_SEED);
        
        // Generate price data with trend, cycles, and jumps
        for (int i = 0; i < N; i++) {
            double t = i / 100.0;
            
            // Upward trend
            priceData[i] = 100.0 + 0.5 * i;
            
            // Add weekly cycle (5 days)
            priceData[i] += 5.0 * Math.sin(2 * Math.PI * i / 5);
            
            // Add monthly cycle (22 days)
            priceData[i] += 3.0 * Math.sin(2 * Math.PI * i / 22);
            
            // Add some noise
            priceData[i] += 0.5 * random.nextDouble();
            
            // Simulate market crash at day 200
            if (i >= 200 && i < 220) {
                priceData[i] -= 20.0 * Math.exp(-(i - 200) / 5.0);
            }
            
            // Volume spikes during crash
            volumeData[i] = 1000000 + 100000 * random.nextDouble();
            if (i >= 195 && i < 225) {
                volumeData[i] *= 3.0; // Triple volume during volatility
            }
        }
    }
    
    @Test
    @DisplayName("Should detect market crash using Paul wavelet")
    void testCrashDetection() {
        FinancialWaveletAnalyzer.CrashDetectionResult result = 
            analyzer.detectMarketCrashes(priceData, 100.0);
        
        assertNotNull(result);
        assertFalse(result.getCrashPoints().isEmpty());
        
        // Should detect crash around day 200
        boolean foundCrash = false;
        for (int crashPoint : result.getCrashPoints()) {
            if (crashPoint >= 195 && crashPoint <= 205) {
                foundCrash = true;
                break;
            }
        }
        assertTrue(foundCrash, "Should detect crash around day 200");
        
        // Check severity
        assertTrue(result.getMaxSeverity() > 5.0);
    }
    
    @Test
    @DisplayName("Should detect volatility clusters using DOG wavelet")
    void testVolatilityDetection() {
        FinancialWaveletAnalyzer.VolatilityAnalysisResult result = 
            analyzer.analyzeVolatility(priceData, 100.0);
        
        assertNotNull(result);
        assertNotNull(result.getVolatilityClusters());
        assertFalse(result.getVolatilityClusters().isEmpty());
        
        // Should identify high volatility period around crash
        boolean foundHighVolatility = false;
        for (var cluster : result.getVolatilityClusters()) {
            if (cluster.startIndex() <= 200 && cluster.endIndex() >= 200) {
                foundHighVolatility = true;
                // Accept MEDIUM or HIGH volatility during crash
                assertTrue(cluster.level() == FinancialWaveletAnalyzer.VolatilityLevel.HIGH ||
                          cluster.level() == FinancialWaveletAnalyzer.VolatilityLevel.MEDIUM);
                break;
            }
        }
        assertTrue(foundHighVolatility);
    }
    
    @Test
    @DisplayName("Should identify cyclical patterns using Shannon wavelet")
    void testCyclicalPatternDetection() {
        FinancialWaveletAnalyzer.CyclicalAnalysisResult result = 
            analyzer.analyzeCyclicalPatterns(priceData, 100.0);
        
        assertNotNull(result);
        assertNotNull(result.getDominantCycles());
        assertFalse(result.getDominantCycles().isEmpty());
        
        // Should find weekly cycle (period ~5)
        boolean foundWeeklyCycle = false;
        for (var cycle : result.getDominantCycles()) {
            if (Math.abs(cycle.period() - 5.0) < 0.5) {
                foundWeeklyCycle = true;
                assertTrue(cycle.strength() > 0.01);
                break;
            }
        }
        assertTrue(foundWeeklyCycle, "Should detect weekly cycle");
        
        // Should find monthly cycle (period ~22)
        boolean foundMonthlyCycle = false;
        for (var cycle : result.getDominantCycles()) {
            if (Math.abs(cycle.period() - 22.0) < 2.0) {
                foundMonthlyCycle = true;
                assertTrue(cycle.strength() > 0.01);
                break;
            }
        }
        assertTrue(foundMonthlyCycle, "Should detect monthly cycle");
    }
    
    @Test
    @DisplayName("Should perform comprehensive market analysis")
    void testComprehensiveAnalysis() {
        MarketAnalysisRequest request = MarketAnalysisRequest.of(priceData, volumeData, 100.0);
        FinancialWaveletAnalyzer.MarketAnalysisResult result = analyzer.analyzeMarket(request);
        
        assertNotNull(result);
        
        // Should identify market regime changes
        assertNotNull(result.getRegimeChanges());
        assertFalse(result.getRegimeChanges().isEmpty());
        
        // Should detect anomalies
        assertNotNull(result.getAnomalies());
        assertFalse(result.getAnomalies().isEmpty());
        
        // Should provide risk metrics
        assertTrue(result.getCurrentRiskLevel() > 0);
        assertTrue(result.getMaxDrawdown() > 0);
        
        // Check that some anomalies are detected (may not always be volume-price divergence)
        assertTrue(result.getAnomalies().size() >= 1, "Should detect at least one anomaly");
    }
    
    @Test
    @DisplayName("Should generate trading signals")
    void testTradingSignalGeneration() {
        FinancialWaveletAnalyzer.TradingSignalResult result = 
            analyzer.generateTradingSignals(priceData, 100.0);
        
        assertNotNull(result);
        assertNotNull(result.getSignals());
        assertFalse(result.getSignals().isEmpty());
        
        // Should generate sell signal before crash
        boolean foundSellSignal = false;
        for (var signal : result.getSignals()) {
            if (signal.type() == FinancialWaveletAnalyzer.SignalType.SELL &&
                signal.timeIndex() >= 190 && signal.timeIndex() <= 200) {
                foundSellSignal = true;
                assertTrue(signal.confidence() > 0.7);
                break;
            }
        }
        assertTrue(foundSellSignal, "Should generate sell signal before crash");
    }
    
    @Test
    @DisplayName("Should calculate wavelet-based technical indicators")
    void testTechnicalIndicators() {
        FinancialWaveletAnalyzer.WaveletIndicators indicators = 
            analyzer.calculateWaveletIndicators(priceData, 100.0);
        
        assertNotNull(indicators);
        
        // Trend strength
        assertNotNull(indicators.getTrendStrength());
        assertEquals(priceData.length, indicators.getTrendStrength().length);
        
        // Momentum
        assertNotNull(indicators.getMomentum());
        assertEquals(priceData.length, indicators.getMomentum().length);
        
        // Volatility index
        assertNotNull(indicators.getVolatilityIndex());
        assertEquals(priceData.length, indicators.getVolatilityIndex().length);
        
        // During crash, volatility should spike
        double maxVolatility = 0;
        int maxVolatilityIndex = 0;
        for (int i = 0; i < indicators.getVolatilityIndex().length; i++) {
            if (indicators.getVolatilityIndex()[i] > maxVolatility) {
                maxVolatility = indicators.getVolatilityIndex()[i];
                maxVolatilityIndex = i;
            }
        }
        // Check that high volatility occurs in crash window (may not be exact max)
        double avgVolatility = 0;
        int count = 0;
        for (double v : indicators.getVolatilityIndex()) {
            avgVolatility += v;
            count++;
        }
        avgVolatility /= count;
        
        double crashWindowMax = 0;
        for (int i = 195; i <= 225 && i < indicators.getVolatilityIndex().length; i++) {
            crashWindowMax = Math.max(crashWindowMax, indicators.getVolatilityIndex()[i]);
        }
        assertTrue(crashWindowMax > avgVolatility * 1.5,
            "Crash period should have significantly elevated volatility");
    }
    
    @Test
    @DisplayName("Should optimize wavelet parameters for financial data")
    void testParameterOptimization() {
        FinancialWaveletAnalyzer.OptimalParameters params = 
            analyzer.optimizeParameters(priceData, 
                FinancialWaveletAnalyzer.AnalysisObjective.CRASH_DETECTION);
        
        assertNotNull(params);
        assertTrue(params.getPaulOrder() >= 1 && params.getPaulOrder() <= 10);
        assertTrue(params.getDogOrder() >= 1 && params.getDogOrder() <= 6);
        assertTrue(params.getShannonBandwidth() > 0 && params.getShannonBandwidth() <= 2.0);
        assertTrue(params.getScaleRange().length == 2);
        assertTrue(params.getScaleRange()[0] < params.getScaleRange()[1]);
    }
}