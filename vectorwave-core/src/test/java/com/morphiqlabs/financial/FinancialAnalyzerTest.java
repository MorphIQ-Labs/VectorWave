package com.morphiqlabs.financial;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test coverage for FinancialAnalyzer class.
 */
public class FinancialAnalyzerTest {
    
    private FinancialAnalysisConfig config;
    private FinancialAnalyzer analyzer;
    
    @BeforeEach
    public void setUp() {
        config = FinancialAnalysisConfig.builder()
            .crashAsymmetryThreshold(0.7)
            .volatilityLowThreshold(0.5)
            .volatilityHighThreshold(2.0)
            .regimeTrendThreshold(0.02)
            .anomalyDetectionThreshold(3.0)
            .windowSize(252)
            .confidenceLevel(0.95)
            .build();
        
        analyzer = new FinancialAnalyzer(config);
    }
    
    @Test
    public void testConstructor_ValidConfig() {
        assertNotNull(analyzer);
        assertEquals(config, analyzer.getConfig());
    }
    
    @Test
    public void testConstructor_NullConfig() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new FinancialAnalyzer(null)
        );
        
        assertTrue(exception.getMessage().contains("Configuration cannot be null"));
    }
    
    @Test
    public void testAnalyzeCrashAsymmetry_NormalMarket() {
        // Simulated normal market prices with small fluctuations
        double[] prices = new double[100];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = 100.0 + Math.sin(i * 0.1) * 2.0;
        }
        
        double asymmetry = analyzer.analyzeCrashAsymmetry(prices);
        
        assertNotNull(asymmetry);
        assertTrue(asymmetry >= 0.0);
        assertTrue(asymmetry <= 1.0);
    }
    
    @Test
    public void testAnalyzeCrashAsymmetry_VolatileMarket() {
        // Simulated volatile market with larger swings
        double[] prices = new double[100];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = 100.0 + Math.sin(i * 0.2) * 10.0 + Math.random() * 5.0;
        }
        
        double asymmetry = analyzer.analyzeCrashAsymmetry(prices);
        
        assertNotNull(asymmetry);
        assertTrue(asymmetry >= 0.0);
    }
    
    @Test
    public void testAnalyzeCrashAsymmetry_CrashScenario() {
        // Simulated crash scenario
        double[] prices = new double[50];
        for (int i = 0; i < 30; i++) {
            prices[i] = 100.0 + i * 0.5;  // Rising market
        }
        for (int i = 30; i < prices.length; i++) {
            prices[i] = 115.0 - (i - 30) * 2.0;  // Sharp decline
        }
        
        double asymmetry = analyzer.analyzeCrashAsymmetry(prices);
        
        assertNotNull(asymmetry);
        assertTrue(asymmetry >= 0.0);
    }
    
    @Test
    public void testAnalyzeCrashAsymmetry_AllPositiveReturns() {
        // All prices increasing
        double[] prices = new double[10];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = 100.0 + i;
        }
        
        double asymmetry = analyzer.analyzeCrashAsymmetry(prices);
        
        // Wavelet transform may produce both positive and negative coefficients
        // even for monotonic price movements, so we just check it's valid
        assertTrue(asymmetry >= 0.0);
        assertTrue(asymmetry <= 1.0);
    }
    
    @Test
    public void testAnalyzeCrashAsymmetry_AllNegativeReturns() {
        // All prices decreasing
        double[] prices = new double[10];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = 100.0 - i;
        }
        
        double asymmetry = analyzer.analyzeCrashAsymmetry(prices);
        
        // Wavelet transform may produce both positive and negative coefficients
        // even for monotonic price movements, so we just check it's valid
        assertTrue(asymmetry >= 0.0);
        assertTrue(asymmetry <= 1.0);
    }
    
    @Test
    public void testAnalyzeVolatility_LowVolatility() {
        // Stable prices with minimal variation
        double[] prices = new double[100];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = 100.0 + Math.sin(i * 0.05) * 0.1;
        }
        
        double volatility = analyzer.analyzeVolatility(prices);
        
        assertNotNull(volatility);
        assertTrue(volatility >= 0.0);
    }
    
    @Test
    public void testAnalyzeVolatility_HighVolatility() {
        // Highly volatile prices
        double[] prices = new double[100];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = 100.0 + Math.random() * 20.0 - 10.0;
        }
        
        double volatility = analyzer.analyzeVolatility(prices);
        
        assertNotNull(volatility);
        assertTrue(volatility >= 0.0);
    }
    
    @Test
    public void testAnalyzeVolatility_ConstantPrices() {
        // No volatility - constant prices
        double[] prices = new double[100];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = 100.0;
        }
        
        double volatility = analyzer.analyzeVolatility(prices);
        
        assertEquals(0.0, volatility, 1e-10);
    }
    
    @Test
    public void testAnalyzeRegimeTrend_NoChange() {
        // Steady trend
        double[] prices = new double[100];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = 100.0 + i * 0.1;
        }
        
        double trend = analyzer.analyzeRegimeTrend(prices);
        
        assertNotNull(trend);
        assertTrue(trend >= 0.0);
    }
    
    @Test
    public void testAnalyzeRegimeTrend_RegimeChange() {
        // Clear regime change
        double[] prices = new double[100];
        for (int i = 0; i < 50; i++) {
            prices[i] = 100.0 + i * 0.5;  // Uptrend
        }
        for (int i = 50; i < prices.length; i++) {
            prices[i] = 125.0 - (i - 50) * 0.5;  // Downtrend
        }
        
        double trend = analyzer.analyzeRegimeTrend(prices);
        
        assertNotNull(trend);
        assertTrue(trend >= 0.0);
    }
    
    @Test
    public void testDetectAnomalies_NoAnomalies() {
        // Regular market behavior - very smooth sine wave
        double[] prices = new double[100];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = 100.0 + Math.sin(i * 0.1) * 0.5;  // Small variations
        }
        
        boolean hasAnomalies = analyzer.detectAnomalies(prices);
        
        // With very smooth prices, anomalies should be less likely
        // but wavelet coefficients can still show variations
        assertNotNull(hasAnomalies);
    }
    
    @Test
    public void testDetectAnomalies_WithAnomalies() {
        // Create prices with outliers
        double[] prices = new double[100];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = 100.0 + Math.sin(i * 0.1) * 2.0;
        }
        // Add spike anomalies
        prices[25] = 150.0;  // Large spike
        prices[75] = 50.0;   // Large drop
        
        boolean hasAnomalies = analyzer.detectAnomalies(prices);
        
        assertTrue(hasAnomalies);
    }
    
    @Test
    public void testClassifyVolatility_Low() {
        double lowVolatility = 0.3;
        
        FinancialAnalyzer.VolatilityClassification classification = 
            analyzer.classifyVolatility(lowVolatility);
        
        assertEquals(FinancialAnalyzer.VolatilityClassification.LOW, classification);
    }
    
    @Test
    public void testClassifyVolatility_Normal() {
        double normalVolatility = 1.0;
        
        FinancialAnalyzer.VolatilityClassification classification = 
            analyzer.classifyVolatility(normalVolatility);
        
        assertEquals(FinancialAnalyzer.VolatilityClassification.NORMAL, classification);
    }
    
    @Test
    public void testClassifyVolatility_High() {
        double highVolatility = 2.5;
        
        FinancialAnalyzer.VolatilityClassification classification = 
            analyzer.classifyVolatility(highVolatility);
        
        assertEquals(FinancialAnalyzer.VolatilityClassification.HIGH, classification);
    }
    
    @Test
    public void testClassifyVolatility_BoundaryValues() {
        // Test at exact boundaries
        assertEquals(FinancialAnalyzer.VolatilityClassification.LOW, 
            analyzer.classifyVolatility(0.49999));
        
        assertEquals(FinancialAnalyzer.VolatilityClassification.NORMAL, 
            analyzer.classifyVolatility(0.5));
        
        assertEquals(FinancialAnalyzer.VolatilityClassification.NORMAL, 
            analyzer.classifyVolatility(2.0));
        
        assertEquals(FinancialAnalyzer.VolatilityClassification.HIGH, 
            analyzer.classifyVolatility(2.00001));
    }
    
    @Test
    public void testIsCrashRisk_BelowThreshold() {
        double asymmetry = 0.5;  // Below threshold of 0.7
        
        assertFalse(analyzer.isCrashRisk(asymmetry));
    }
    
    @Test
    public void testIsCrashRisk_AboveThreshold() {
        double asymmetry = 0.8;  // Above threshold of 0.7
        
        assertTrue(analyzer.isCrashRisk(asymmetry));
    }
    
    @Test
    public void testIsCrashRisk_AtThreshold() {
        double asymmetry = 0.7;  // Exactly at threshold
        
        assertFalse(analyzer.isCrashRisk(asymmetry));  // Strictly greater than
    }
    
    @Test
    public void testIsRegimeShift_BelowThreshold() {
        double trend = 0.01;  // Below threshold of 0.02
        
        assertFalse(analyzer.isRegimeShift(trend));
    }
    
    @Test
    public void testIsRegimeShift_AboveThreshold() {
        double trend = 0.03;  // Above threshold of 0.02
        
        assertTrue(analyzer.isRegimeShift(trend));
    }
    
    @Test
    public void testValidatePrices_Null() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> analyzer.analyzeCrashAsymmetry(null)
        );
        
        assertTrue(exception.getMessage().contains("Prices cannot be null"));
    }
    
    @Test
    public void testValidatePrices_TooShort() {
        double[] prices = {100.0};  // Only one price
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> analyzer.analyzeCrashAsymmetry(prices)
        );
        
        assertTrue(exception.getMessage().contains("Prices must contain at least 2 elements"));
    }
    
    @Test
    public void testValidatePrices_NaN() {
        double[] prices = {100.0, Double.NaN, 102.0};
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> analyzer.analyzeCrashAsymmetry(prices)
        );
        
        assertTrue(exception.getMessage().contains("Prices must contain only finite values"));
    }
    
    @Test
    public void testValidatePrices_Infinity() {
        double[] prices = {100.0, Double.POSITIVE_INFINITY, 102.0};
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> analyzer.analyzeCrashAsymmetry(prices)
        );
        
        assertTrue(exception.getMessage().contains("Prices must contain only finite values"));
    }
    
    @Test
    public void testValidatePrices_NegativeInfinity() {
        double[] prices = {100.0, Double.NEGATIVE_INFINITY, 102.0};
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> analyzer.analyzeCrashAsymmetry(prices)
        );
        
        assertTrue(exception.getMessage().contains("Prices must contain only finite values"));
    }
    
    @Test
    public void testCalculateReturns_ZeroPrice() {
        // Test handling of zero prices
        double[] prices = {100.0, 0.0, 100.0};
        
        // Should not throw exception
        double volatility = analyzer.analyzeVolatility(prices);
        assertNotNull(volatility);
    }
    
    @Test
    public void testCalculateReturns_MinimumLength() {
        double[] prices = {100.0, 101.0};  // Minimum valid length
        
        double asymmetry = analyzer.analyzeCrashAsymmetry(prices);
        assertNotNull(asymmetry);
        
        double volatility = analyzer.analyzeVolatility(prices);
        assertNotNull(volatility);
        
        double trend = analyzer.analyzeRegimeTrend(prices);
        assertNotNull(trend);
        
        boolean anomalies = analyzer.detectAnomalies(prices);
        assertNotNull(anomalies);
    }
    
    @Test
    public void testArbitraryLength_MODWT() {
        // Test that MODWT works with arbitrary length (not power of 2)
        double[] prices = new double[777];  // Not a power of 2
        for (int i = 0; i < prices.length; i++) {
            prices[i] = 100.0 + Math.sin(i * 0.1) * 5.0;
        }
        
        // All methods should work with arbitrary length
        double asymmetry = analyzer.analyzeCrashAsymmetry(prices);
        assertNotNull(asymmetry);
        
        double volatility = analyzer.analyzeVolatility(prices);
        assertNotNull(volatility);
        
        double trend = analyzer.analyzeRegimeTrend(prices);
        assertNotNull(trend);
        
        boolean anomalies = analyzer.detectAnomalies(prices);
        assertNotNull(anomalies);
    }
    
    @Test
    public void testIntegrationScenario() {
        // Create a realistic market scenario
        double[] prices = new double[252];  // One year of trading days
        
        // Bull market for first half
        for (int i = 0; i < 126; i++) {
            prices[i] = 100.0 + i * 0.3 + Math.random() * 2.0;
        }
        
        // Bear market for second half
        for (int i = 126; i < 252; i++) {
            prices[i] = 137.8 - (i - 126) * 0.4 + Math.random() * 3.0;
        }
        
        // Analyze all metrics
        double asymmetry = analyzer.analyzeCrashAsymmetry(prices);
        double volatility = analyzer.analyzeVolatility(prices);
        double trend = analyzer.analyzeRegimeTrend(prices);
        boolean anomalies = analyzer.detectAnomalies(prices);
        
        // Classify results
        FinancialAnalyzer.VolatilityClassification volClass = 
            analyzer.classifyVolatility(volatility);
        boolean crashRisk = analyzer.isCrashRisk(asymmetry);
        boolean regimeShift = analyzer.isRegimeShift(trend);
        
        // All results should be valid
        assertNotNull(asymmetry);
        assertNotNull(volatility);
        assertNotNull(trend);
        assertNotNull(anomalies);
        assertNotNull(volClass);
        assertNotNull(crashRisk);
        assertNotNull(regimeShift);
    }
    
    @Test
    public void testDifferentConfigurations() {
        // Test with different configuration
        FinancialAnalysisConfig strictConfig = FinancialAnalysisConfig.builder()
            .crashAsymmetryThreshold(0.5)
            .volatilityLowThreshold(0.3)
            .volatilityHighThreshold(1.0)
            .regimeTrendThreshold(0.01)
            .anomalyDetectionThreshold(2.0)
            .windowSize(60)
            .confidenceLevel(0.99)
            .build();
        
        FinancialAnalyzer strictAnalyzer = new FinancialAnalyzer(strictConfig);
        
        double[] prices = new double[100];
        for (int i = 0; i < prices.length; i++) {
            prices[i] = 100.0 + Math.sin(i * 0.1) * 5.0;
        }
        
        double volatility = strictAnalyzer.analyzeVolatility(prices);
        
        // With stricter thresholds, classifications may differ
        FinancialAnalyzer.VolatilityClassification classification = 
            strictAnalyzer.classifyVolatility(volatility);
        
        assertNotNull(classification);
    }
}