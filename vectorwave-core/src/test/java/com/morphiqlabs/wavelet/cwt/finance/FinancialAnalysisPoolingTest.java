package com.morphiqlabs.wavelet.cwt.finance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.Random;
import java.util.concurrent.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to ensure object pooling doesn't introduce regressions.
 */
class FinancialAnalysisPoolingTest {
    
    private FinancialWaveletAnalyzer analyzer;
    private double[] testPriceData;
    private double[] testVolumeData;
    
    @BeforeEach
    void setUp() {
        analyzer = new FinancialWaveletAnalyzer();
        
        // Create test data with known patterns
        testPriceData = new double[500];
        testVolumeData = new double[500];
        
        // Synthetic price data with a crash
        for (int i = 0; i < testPriceData.length; i++) {
            if (i == 250) {
                // Crash point
                testPriceData[i] = testPriceData[i-1] * 0.90; // 10% drop
            } else if (i > 0) {
                // Normal random walk
                testPriceData[i] = testPriceData[i-1] * (1 + (Math.random() - 0.5) * 0.01);
            } else {
                testPriceData[i] = 100.0;
            }
            
            testVolumeData[i] = 1_000_000 * (1 + Math.random() * 0.1);
        }
    }
    
    @Test
    @DisplayName("Volatility analysis should produce consistent results")
    void testVolatilityAnalysisConsistency() {
        // Run analysis multiple times to ensure pooling doesn't affect results
        FinancialWaveletAnalyzer.VolatilityAnalysisResult result1 = 
            analyzer.analyzeVolatility(testPriceData, 1.0);
        FinancialWaveletAnalyzer.VolatilityAnalysisResult result2 = 
            analyzer.analyzeVolatility(testPriceData, 1.0);
            
        assertEquals(result1.averageVolatility(), result2.averageVolatility(), 1e-10);
        assertEquals(result1.maxVolatility(), result2.maxVolatility(), 1e-10);
        assertEquals(result1.instantaneousVolatility().length, 
                    result2.instantaneousVolatility().length);
        
        // Check volatility values are consistent
        double[] vol1 = result1.instantaneousVolatility();
        double[] vol2 = result2.instantaneousVolatility();
        for (int i = 0; i < vol1.length; i++) {
            assertEquals(vol1[i], vol2[i], 1e-10, 
                "Volatility mismatch at index " + i);
        }
    }
    
    @Test
    @DisplayName("Crash detection should identify known crashes")
    void testCrashDetection() {
        FinancialWaveletAnalyzer.CrashDetectionResult result = 
            analyzer.detectMarketCrashes(testPriceData, 1.0);
        
        assertNotNull(result);
        assertFalse(result.crashPoints().isEmpty(), 
            "Should detect at least one crash");
        
        // Check if crash around index 250 is detected (with some tolerance)
        boolean foundCrash = false;
        for (int crashPoint : result.crashPoints()) {
            if (Math.abs(crashPoint - 250) < 20) {
                foundCrash = true;
                break;
            }
        }
        assertTrue(foundCrash, "Should detect the crash around index 250");
    }
    
    @Test
    @DisplayName("Trading signals should be generated correctly")
    void testTradingSignals() {
        FinancialWaveletAnalyzer.TradingSignalResult result = 
            analyzer.generateTradingSignals(testPriceData, 1.0);
        
        assertNotNull(result);
        assertNotNull(result.signals());
        
        // Should generate some signals
        assertFalse(result.signals().isEmpty(), 
            "Should generate at least some trading signals");
        
        // Verify signal types
        for (var signal : result.signals()) {
            assertNotNull(signal.type());
            assertNotNull(signal.rationale());
            assertTrue(signal.confidence() >= 0 && signal.confidence() <= 1,
                "Confidence should be between 0 and 1");
            assertTrue(signal.timeIndex() >= 0 && signal.timeIndex() < testPriceData.length,
                "Time index should be within data range");
        }
    }
    
    @Test
    @DisplayName("Pool statistics should show hits after warm-up")
    void testPoolEfficiency() {
        // Warm up the pool
        for (int i = 0; i < 5; i++) {
            analyzer.analyzeVolatility(testPriceData, 1.0);
            analyzer.detectMarketCrashes(testPriceData, 1.0);
        }
        
        // Check pool statistics
        FinancialAnalysisObjectPool.PoolStatistics stats = analyzer.getPoolStatistics();
        
        assertTrue(stats.arrayHits() > 0, "Should have some array pool hits");
        assertTrue(stats.hitRate() > 0.5, "Hit rate should be > 50% after warm-up");
    }
    
    @Test
    @DisplayName("Concurrent analysis should work correctly")
    void testConcurrentAnalysis() throws InterruptedException, ExecutionException {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<FinancialWaveletAnalyzer.VolatilityAnalysisResult>> futures = 
            new CopyOnWriteArrayList<>();
        
        // Submit multiple concurrent analysis tasks
        for (int i = 0; i < 10; i++) {
            futures.add(executor.submit(() -> 
                analyzer.analyzeVolatility(testPriceData, 1.0)));
        }
        
        // Wait for all to complete
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        
        // Verify all results are consistent
        FinancialWaveletAnalyzer.VolatilityAnalysisResult firstResult = futures.get(0).get();
        for (var future : futures) {
            var result = future.get();
            assertEquals(firstResult.averageVolatility(), result.averageVolatility(), 1e-10);
            assertEquals(firstResult.maxVolatility(), result.maxVolatility(), 1e-10);
        }
    }
    
    @Test
    @DisplayName("Market analysis with all options should complete successfully")
    void testFullMarketAnalysis() {
        MarketAnalysisRequest request = MarketAnalysisRequest.builder()
            .priceData(testPriceData)
            .volumeData(testVolumeData)
            .samplingRate(1.0)
            .configureOptions(opts -> opts
                .detectCrashes(true)
                .analyzeVolatility(true)
                .findCycles(true)
                .generateSignals(true))
            .build();
            
        FinancialWaveletAnalyzer.MarketAnalysisResult result = analyzer.analyzeMarket(request);
        
        assertNotNull(result);
        assertNotNull(result.regimeChanges());
        assertNotNull(result.anomalies());
        assertTrue(result.currentRiskLevel() >= 0 && result.currentRiskLevel() <= 1);
        assertTrue(result.maxDrawdown() >= 0);
        
        // Should detect some anomalies including the crash
        assertFalse(result.anomalies().isEmpty(), 
            "Should detect some market anomalies");
    }
    
    @Test
    @DisplayName("Empty or small data should be handled gracefully")
    void testEdgeCases() {
        // Test with minimal data
        double[] smallData = {100.0, 101.0, 99.0};
        
        assertDoesNotThrow(() -> {
            analyzer.analyzeVolatility(smallData, 1.0);
            analyzer.detectMarketCrashes(smallData, 1.0);
        });
        
        // Test with single data point - should throw
        double[] singlePoint = {100.0};
        assertThrows(IllegalArgumentException.class, () -> {
            MarketAnalysisRequest request = MarketAnalysisRequest.of(singlePoint, 1.0);
            analyzer.analyzeMarket(request);
        });
    }
}