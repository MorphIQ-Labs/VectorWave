package com.morphiqlabs.financial;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test coverage for FinancialAnalysisConfig class.
 */
public class FinancialAnalysisConfigTest {
    
    @Test
    public void testBuilder_AllParametersSet() {
        FinancialAnalysisConfig config = FinancialAnalysisConfig.builder()
            .crashAsymmetryThreshold(0.7)
            .volatilityLowThreshold(0.5)
            .volatilityHighThreshold(2.0)
            .regimeTrendThreshold(0.02)
            .anomalyDetectionThreshold(3.0)
            .windowSize(252)
            .confidenceLevel(0.95)
            .build();
        
        assertEquals(0.7, config.getCrashAsymmetryThreshold(), 1e-10);
        assertEquals(0.5, config.getVolatilityLowThreshold(), 1e-10);
        assertEquals(2.0, config.getVolatilityHighThreshold(), 1e-10);
        assertEquals(0.02, config.getRegimeTrendThreshold(), 1e-10);
        assertEquals(3.0, config.getAnomalyDetectionThreshold(), 1e-10);
        assertEquals(252, config.getWindowSize());
        assertEquals(0.95, config.getConfidenceLevel(), 1e-10);
    }
    
    @Test
    public void testBuilder_MissingCrashAsymmetryThreshold() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> FinancialAnalysisConfig.builder()
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(252)
                .confidenceLevel(0.95)
                .build()
        );
        
        assertTrue(exception.getMessage().contains("Crash asymmetry threshold must be specified"));
    }
    
    @Test
    public void testBuilder_MissingVolatilityLowThreshold() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(252)
                .confidenceLevel(0.95)
                .build()
        );
        
        assertTrue(exception.getMessage().contains("Volatility low threshold must be specified"));
    }
    
    @Test
    public void testBuilder_MissingVolatilityHighThreshold() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(252)
                .confidenceLevel(0.95)
                .build()
        );
        
        assertTrue(exception.getMessage().contains("Volatility high threshold must be specified"));
    }
    
    @Test
    public void testBuilder_MissingRegimeTrendThreshold() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .anomalyDetectionThreshold(3.0)
                .windowSize(252)
                .confidenceLevel(0.95)
                .build()
        );
        
        assertTrue(exception.getMessage().contains("Regime trend threshold must be specified"));
    }
    
    @Test
    public void testBuilder_MissingAnomalyDetectionThreshold() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .windowSize(252)
                .confidenceLevel(0.95)
                .build()
        );
        
        assertTrue(exception.getMessage().contains("Anomaly detection threshold must be specified"));
    }
    
    @Test
    public void testBuilder_MissingWindowSize() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .confidenceLevel(0.95)
                .build()
        );
        
        assertTrue(exception.getMessage().contains("Window size must be specified"));
    }
    
    @Test
    public void testBuilder_MissingConfidenceLevel() {
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(0.5)
                .volatilityHighThreshold(2.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(252)
                .build()
        );
        
        assertTrue(exception.getMessage().contains("Confidence level must be specified"));
    }
    
    @Test
    public void testBuilder_InvalidVolatilityThresholds() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(2.0)  // Higher than high threshold
                .volatilityHighThreshold(1.0)
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(252)
                .confidenceLevel(0.95)
                .build()
        );
        
        assertTrue(exception.getMessage().contains("Volatility low threshold must be less than high threshold"));
    }
    
    @Test
    public void testBuilder_EqualVolatilityThresholds() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.7)
                .volatilityLowThreshold(1.0)
                .volatilityHighThreshold(1.0)  // Equal to low threshold
                .regimeTrendThreshold(0.02)
                .anomalyDetectionThreshold(3.0)
                .windowSize(252)
                .confidenceLevel(0.95)
                .build()
        );
        
        assertTrue(exception.getMessage().contains("Volatility low threshold must be less than high threshold"));
    }
    
    @Test
    public void testBuilder_NegativeCrashAsymmetryThreshold() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(-0.1)
        );
        
        assertTrue(exception.getMessage().contains("Crash asymmetry threshold must be positive"));
    }
    
    @Test
    public void testBuilder_ZeroCrashAsymmetryThreshold() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .crashAsymmetryThreshold(0.0)
        );
        
        assertTrue(exception.getMessage().contains("Crash asymmetry threshold must be positive"));
    }
    
    @Test
    public void testBuilder_NegativeVolatilityLowThreshold() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .volatilityLowThreshold(-0.5)
        );
        
        assertTrue(exception.getMessage().contains("Volatility low threshold must be positive"));
    }
    
    @Test
    public void testBuilder_NegativeVolatilityHighThreshold() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .volatilityHighThreshold(-2.0)
        );
        
        assertTrue(exception.getMessage().contains("Volatility high threshold must be positive"));
    }
    
    @Test
    public void testBuilder_NegativeRegimeTrendThreshold() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .regimeTrendThreshold(-0.02)
        );
        
        assertTrue(exception.getMessage().contains("Regime trend threshold must be positive"));
    }
    
    @Test
    public void testBuilder_NegativeAnomalyDetectionThreshold() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .anomalyDetectionThreshold(-3.0)
        );
        
        assertTrue(exception.getMessage().contains("Anomaly detection threshold must be positive"));
    }
    
    @Test
    public void testBuilder_InvalidWindowSize() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .windowSize(1)  // Less than 2
        );
        
        assertTrue(exception.getMessage().contains("Window size must be at least 2"));
    }
    
    @Test
    public void testBuilder_ZeroWindowSize() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .windowSize(0)
        );
        
        assertTrue(exception.getMessage().contains("Window size must be at least 2"));
    }
    
    @Test
    public void testBuilder_InvalidConfidenceLevel_Zero() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .confidenceLevel(0.0)
        );
        
        assertTrue(exception.getMessage().contains("Confidence level must be between 0.0 and 1.0"));
    }
    
    @Test
    public void testBuilder_InvalidConfidenceLevel_One() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .confidenceLevel(1.0)
        );
        
        assertTrue(exception.getMessage().contains("Confidence level must be between 0.0 and 1.0"));
    }
    
    @Test
    public void testBuilder_InvalidConfidenceLevel_Negative() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .confidenceLevel(-0.1)
        );
        
        assertTrue(exception.getMessage().contains("Confidence level must be between 0.0 and 1.0"));
    }
    
    @Test
    public void testBuilder_InvalidConfidenceLevel_GreaterThanOne() {
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> FinancialAnalysisConfig.builder()
                .confidenceLevel(1.1)
        );
        
        assertTrue(exception.getMessage().contains("Confidence level must be between 0.0 and 1.0"));
    }
    
    @Test
    public void testBuilder_ValidBoundaryValues() {
        // Test minimum valid window size
        FinancialAnalysisConfig config1 = FinancialAnalysisConfig.builder()
            .crashAsymmetryThreshold(0.1)
            .volatilityLowThreshold(0.1)
            .volatilityHighThreshold(0.2)
            .regimeTrendThreshold(0.001)
            .anomalyDetectionThreshold(0.1)
            .windowSize(2)  // Minimum valid
            .confidenceLevel(0.001)  // Close to zero
            .build();
        
        assertEquals(2, config1.getWindowSize());
        assertEquals(0.001, config1.getConfidenceLevel(), 1e-10);
        
        // Test large values
        FinancialAnalysisConfig config2 = FinancialAnalysisConfig.builder()
            .crashAsymmetryThreshold(1000.0)
            .volatilityLowThreshold(1000.0)
            .volatilityHighThreshold(2000.0)
            .regimeTrendThreshold(100.0)
            .anomalyDetectionThreshold(100.0)
            .windowSize(1000000)  // Large window
            .confidenceLevel(0.999)  // Close to one
            .build();
        
        assertEquals(1000000, config2.getWindowSize());
        assertEquals(0.999, config2.getConfidenceLevel(), 1e-10);
    }
    
    @Test
    public void testBuilder_ChainedCalls() {
        // Test that builder methods can be chained properly
        FinancialAnalysisConfig.Builder builder = FinancialAnalysisConfig.builder();
        
        FinancialAnalysisConfig config = builder
            .crashAsymmetryThreshold(0.6)
            .volatilityLowThreshold(0.3)
            .volatilityHighThreshold(1.5)
            .regimeTrendThreshold(0.01)
            .anomalyDetectionThreshold(2.0)
            .windowSize(60)
            .confidenceLevel(0.90)
            .build();
        
        assertNotNull(config);
        assertEquals(0.6, config.getCrashAsymmetryThreshold(), 1e-10);
        assertEquals(60, config.getWindowSize());
    }
    
    @Test
    public void testImmutability() {
        FinancialAnalysisConfig config = FinancialAnalysisConfig.builder()
            .crashAsymmetryThreshold(0.7)
            .volatilityLowThreshold(0.5)
            .volatilityHighThreshold(2.0)
            .regimeTrendThreshold(0.02)
            .anomalyDetectionThreshold(3.0)
            .windowSize(252)
            .confidenceLevel(0.95)
            .build();
        
        // Verify values remain constant
        assertEquals(0.7, config.getCrashAsymmetryThreshold(), 1e-10);
        assertEquals(0.7, config.getCrashAsymmetryThreshold(), 1e-10);
        
        assertEquals(252, config.getWindowSize());
        assertEquals(252, config.getWindowSize());
    }
    
    @Test
    public void testTypicalMarketConfigurations() {
        // Test typical equity market configuration
        FinancialAnalysisConfig equityConfig = FinancialAnalysisConfig.builder()
            .crashAsymmetryThreshold(0.7)
            .volatilityLowThreshold(0.5)
            .volatilityHighThreshold(2.0)
            .regimeTrendThreshold(0.03)
            .anomalyDetectionThreshold(3.0)
            .windowSize(252)  // Annual trading days
            .confidenceLevel(0.95)
            .build();
        
        assertNotNull(equityConfig);
        
        // Test typical forex configuration
        FinancialAnalysisConfig forexConfig = FinancialAnalysisConfig.builder()
            .crashAsymmetryThreshold(0.8)
            .volatilityLowThreshold(0.3)
            .volatilityHighThreshold(1.5)
            .regimeTrendThreshold(0.01)
            .anomalyDetectionThreshold(2.5)
            .windowSize(120)  // Semi-annual
            .confidenceLevel(0.99)
            .build();
        
        assertNotNull(forexConfig);
        
        // Test high-frequency trading configuration
        FinancialAnalysisConfig hftConfig = FinancialAnalysisConfig.builder()
            .crashAsymmetryThreshold(0.6)
            .volatilityLowThreshold(0.1)
            .volatilityHighThreshold(0.5)
            .regimeTrendThreshold(0.005)
            .anomalyDetectionThreshold(4.0)
            .windowSize(1000)  // Arbitrary size with MODWT
            .confidenceLevel(0.999)
            .build();
        
        assertNotNull(hftConfig);
    }
}