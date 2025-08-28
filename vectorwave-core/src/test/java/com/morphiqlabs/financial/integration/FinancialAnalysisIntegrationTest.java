package com.morphiqlabs.financial.integration;

import com.morphiqlabs.financial.FinancialAnalysisConfig;
import com.morphiqlabs.financial.FinancialAnalyzer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests demonstrating configurable thresholds in financial analysis.
 * These tests verify that the hardcoded threshold values mentioned in issue #65
 * have been successfully moved to configurable parameters.
 */
@DisplayName("Financial Analysis Integration Tests")
class FinancialAnalysisIntegrationTest {
    
    @Test
    @DisplayName("Issue #65: CRASH_ASYMMETRY_THRESHOLD should be configurable")
    void testCrashAsymmetryThresholdIsConfigurable() {
        // Create a config with explicit values (previously these were defaults)
        FinancialAnalysisConfig defaultConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(256)
                .confidenceLevel(0.95)
                .build();
        assertEquals(0.7, defaultConfig.getCrashAsymmetryThreshold(), 1e-10,
                "Crash asymmetry threshold should be set to 0.7");
        
        // Verify we can customize it with a realistic threshold
        // Note: analyzeCrashAsymmetry returns values between 0 and 1 (it's a ratio)
        FinancialAnalysisConfig customConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.5)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(256)
                .confidenceLevel(0.95)
                .build();
        assertEquals(0.5, customConfig.getCrashAsymmetryThreshold(), 1e-10,
                "Crash asymmetry threshold should be configurable");
        
        // Verify analyzers use the configured thresholds
        FinancialAnalyzer defaultAnalyzer = new FinancialAnalyzer(defaultConfig);
        FinancialAnalyzer customAnalyzer = new FinancialAnalyzer(customConfig);
        
        // Test with a realistic asymmetry value (between 0 and 1)
        double testAsymmetry = 0.75; // Above both thresholds (0.5 and 0.7)
        
        assertTrue(defaultAnalyzer.isCrashRisk(testAsymmetry), 
                "Default analyzer should detect crash risk at 0.75 (above 0.7 threshold)");
        assertTrue(customAnalyzer.isCrashRisk(testAsymmetry), 
                "Custom analyzer should detect crash risk at 0.75 (above 0.5 threshold)");
    }
    
    @Test
    @DisplayName("Issue #65: VOLATILITY_LOW_THRESHOLD = 0.5 should be configurable")
    void testVolatilityLowThresholdIsConfigurable() {
        // Create a config with explicit values
        FinancialAnalysisConfig defaultConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(256)
                .confidenceLevel(0.95)
                .build();
        assertEquals(0.5, defaultConfig.getVolatilityLowThreshold(), 1e-10,
                "Volatility low threshold should be set to 0.5");
        
        // Verify we can customize it
        FinancialAnalysisConfig customConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.3)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(256)
                .confidenceLevel(0.95)
                .build();
        assertEquals(0.3, customConfig.getVolatilityLowThreshold(), 1e-10,
                "Volatility low threshold should be configurable");
        
        // Verify analyzers use the configured thresholds
        FinancialAnalyzer defaultAnalyzer = new FinancialAnalyzer(defaultConfig);
        FinancialAnalyzer customAnalyzer = new FinancialAnalyzer(customConfig);
        
        double testVolatility = 0.4; // Between 0.3 and 0.5
        
        assertEquals(FinancialAnalyzer.VolatilityClassification.LOW, 
                defaultAnalyzer.classifyVolatility(testVolatility),
                "Default analyzer should classify 0.4 as LOW (below 0.5 threshold)");
        assertEquals(FinancialAnalyzer.VolatilityClassification.NORMAL, 
                customAnalyzer.classifyVolatility(testVolatility),
                "Custom analyzer should classify 0.4 as NORMAL (above 0.3 threshold)");
    }
    
    @Test
    @DisplayName("Issue #65: REGIME_TREND_THRESHOLD = 0.02 should be configurable")
    void testRegimeTrendThresholdIsConfigurable() {
        // Create a config with explicit values
        FinancialAnalysisConfig defaultConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(256)
                .confidenceLevel(0.95)
                .build();
        assertEquals(0.02, defaultConfig.getRegimeTrendThreshold(), 1e-10,
                "Regime trend threshold should be set to 0.02");
        
        // Verify we can customize it
        FinancialAnalysisConfig customConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.01)
                .anomalyDetectionThreshold(3.0)
                .windowSize(256)
                .confidenceLevel(0.95)
                .build();
        assertEquals(0.01, customConfig.getRegimeTrendThreshold(), 1e-10,
                "Regime trend threshold should be configurable");
        
        // Verify analyzers use the configured thresholds
        FinancialAnalyzer defaultAnalyzer = new FinancialAnalyzer(defaultConfig);
        FinancialAnalyzer customAnalyzer = new FinancialAnalyzer(customConfig);
        
        double testTrendChange = 0.015; // Between 0.01 and 0.02
        
        assertFalse(defaultAnalyzer.isRegimeShift(testTrendChange), 
                "Default analyzer should not detect regime shift at 0.015 (below 0.02 threshold)");
        assertTrue(customAnalyzer.isRegimeShift(testTrendChange), 
                "Custom analyzer should detect regime shift at 0.015 (above 0.01 threshold)");
    }
    
    @Test
    @DisplayName("Issue #65: All hardcoded thresholds should be moved to configuration")
    void testAllHardcodedThresholdsAreConfigurable() {
        // Create a completely custom configuration
        FinancialAnalysisConfig customConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.8)          // Custom threshold (values range 0-1)
                .volatilityLowThreshold(0.3)           // Custom threshold
                .volatilityHighThreshold(1.5)          // Additional configurable threshold
                .regimeTrendThreshold(0.015)           // Custom threshold
                .anomalyDetectionThreshold(2.5)        // Additional configurable threshold
                .windowSize(128)                       // Configurable window size
                .confidenceLevel(0.99)                 // Configurable confidence level
                .build();
        
        FinancialAnalyzer analyzer = new FinancialAnalyzer(customConfig);
        
        // Verify all configurations are applied
        assertEquals(0.8, analyzer.getConfig().getCrashAsymmetryThreshold(), 1e-10);
        assertEquals(0.3, analyzer.getConfig().getVolatilityLowThreshold(), 1e-10);
        assertEquals(1.5, analyzer.getConfig().getVolatilityHighThreshold(), 1e-10);
        assertEquals(0.015, analyzer.getConfig().getRegimeTrendThreshold(), 1e-10);
        assertEquals(2.5, analyzer.getConfig().getAnomalyDetectionThreshold(), 1e-10);
        assertEquals(128, analyzer.getConfig().getWindowSize());
        assertEquals(0.99, analyzer.getConfig().getConfidenceLevel(), 1e-10);
        
        // Verify analyzer works with custom configuration
        double[] testPrices = createTestData();
        assertDoesNotThrow(() -> {
            analyzer.analyzeCrashAsymmetry(testPrices);
            analyzer.analyzeVolatility(testPrices);
            analyzer.analyzeRegimeTrend(testPrices);
            analyzer.detectAnomalies(testPrices);
        }, "Analyzer should work with completely custom configuration");
    }
    
    @Test
    @DisplayName("Issue #65: Configuration values should be explicit")
    void testConfigurationValuesAreExplicit() {
        // Create configuration with standard values (previously defaults)
        FinancialAnalysisConfig standardConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(256)
                .confidenceLevel(0.95)
                .build();
        
        FinancialAnalyzer standardAnalyzer = new FinancialAnalyzer(standardConfig);
        
        // Verify values are as set
        FinancialAnalysisConfig config = standardAnalyzer.getConfig();
        assertEquals(0.7, config.getCrashAsymmetryThreshold(), 1e-10, 
                "CRASH_ASYMMETRY_THRESHOLD should be 0.7 (realistic range)");
        assertEquals(0.5, config.getVolatilityLowThreshold(), 1e-10, 
                "VOLATILITY_LOW_THRESHOLD should be 0.5");
        assertEquals(0.02, config.getRegimeTrendThreshold(), 1e-10, 
                "REGIME_TREND_THRESHOLD should be 0.02");
        
        // Verify analyzer functions work with standard configuration
        double[] testPrices = createTestData();
        assertDoesNotThrow(() -> {
            double asymmetry = standardAnalyzer.analyzeCrashAsymmetry(testPrices);
            double volatility = standardAnalyzer.analyzeVolatility(testPrices);
            double trendChange = standardAnalyzer.analyzeRegimeTrend(testPrices);
            boolean hasAnomalies = standardAnalyzer.detectAnomalies(testPrices);
            
            // All results should be finite and valid
            assertTrue(Double.isFinite(asymmetry) && asymmetry >= 0);
            assertTrue(Double.isFinite(volatility) && volatility >= 0);
            assertTrue(Double.isFinite(trendChange) && trendChange >= 0);
            // hasAnomalies is just a boolean, so any value is valid
        }, "Default analyzer should maintain backward compatibility");
    }
    
    @Test
    @DisplayName("Issue #65: Configuration should allow per-analysis customization")
    void testPerAnalysisCustomization() {
        // Demonstrate different configurations for different use cases
        
        // High-frequency trading configuration
        FinancialAnalysisConfig hftConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.3)       // More sensitive (lower threshold)
                .volatilityLowThreshold(0.1)        // Lower volatility range
                .volatilityHighThreshold(0.8)       // HFT volatility high threshold
                .regimeTrendThreshold(0.005)        // More sensitive to changes
                .anomalyDetectionThreshold(2.5)     // HFT anomaly threshold
                .windowSize(64)                     // Smaller window for faster analysis
                .confidenceLevel(0.99)              // High confidence for HFT
                .build();
        
        // Long-term investment analysis configuration
        FinancialAnalysisConfig longTermConfig = FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.9)       // Less sensitive to short-term fluctuations
                .volatilityLowThreshold(1.0)        // Higher volatility range
                .volatilityHighThreshold(3.0)       // Long-term volatility high threshold
                .regimeTrendThreshold(0.05)         // Less sensitive to minor changes
                .anomalyDetectionThreshold(4.0)     // Long-term anomaly threshold
                .windowSize(512)                    // Larger window for trend analysis
                .confidenceLevel(0.95)              // Standard confidence
                .build();
        
        FinancialAnalyzer hftAnalyzer = new FinancialAnalyzer(hftConfig);
        FinancialAnalyzer longTermAnalyzer = new FinancialAnalyzer(longTermConfig);
        
        double[] testPrices = createTestData();
        
        // Both analyzers should work but may produce different results due to different configurations
        assertDoesNotThrow(() -> {
            double hftAsymmetry = hftAnalyzer.analyzeCrashAsymmetry(testPrices);
            double longTermAsymmetry = longTermAnalyzer.analyzeCrashAsymmetry(testPrices);
            
            assertTrue(Double.isFinite(hftAsymmetry));
            assertTrue(Double.isFinite(longTermAsymmetry));
        }, "Different configurations should allow per-analysis customization");
    }
    
    private double[] createTestData() {
        // Create a simple test dataset with power-of-2 length
        double[] prices = new double[16];
        double basePrice = 100.0;
        prices[0] = basePrice;
        
        for (int i = 1; i < prices.length; i++) {
            // Simple random walk
            double change = (Math.random() - 0.5) * 0.02;
            prices[i] = prices[i-1] * (1 + change);
        }
        
        return prices;
    }
}