package com.morphiqlabs.wavelet.cwt.finance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FinancialAnalysisParameters configuration.
 */
class FinancialAnalysisParametersTest {
    
    @Test
    @DisplayName("Default parameters should have expected values")
    void testDefaultParametersValues() {
        FinancialAnalysisParameters params = FinancialAnalysisParameters.defaultParameters();
        
        // Crash detection
        assertEquals(10.0, params.getCrashAsymmetryThreshold());
        assertEquals(1.0, params.getCrashMinScale());
        assertEquals(10.0, params.getCrashMaxScale());
        assertEquals(20, params.getCrashNumScales());
        
        // Volatility
        assertEquals(0.5, params.getVolatilityLowThreshold());
        assertEquals(1.5, params.getVolatilityMediumThreshold());
        assertEquals(3.0, params.getVolatilityHighThreshold());
        
        // Market regime
        assertEquals(0.05, params.getRegimeTrendThreshold());
        assertEquals(20, params.getRegimeDetectionLookbackPeriod());
        
        // Risk assessment
        assertEquals(0.02, params.getDefaultAverageVolatility());
        assertEquals(0.5, params.getBaseRiskLevel());
        assertEquals(0.03, params.getAnnualRiskFreeRate());
    }
    
    @Test
    @DisplayName("Builder should allow customization")
    void testBuilderCustomization() {
        FinancialAnalysisParameters params = FinancialAnalysisParameters.builder()
            .crashAsymmetryThreshold(15.0)
            .volatilityLowThreshold(0.3)
            .regimeTrendThreshold(0.03)
            .build();
        
        assertEquals(15.0, params.getCrashAsymmetryThreshold());
        assertEquals(0.3, params.getVolatilityLowThreshold());
        assertEquals(0.03, params.getRegimeTrendThreshold());
        
        // Other values should still be defaults
        assertEquals(1.0, params.getCrashMinScale());
    }
    
    @Test
    @DisplayName("Builder should validate inputs")
    void testBuilderValidation() {
        var builder = FinancialAnalysisParameters.builder();
        
        // Negative threshold
        assertThrows(IllegalArgumentException.class, 
            () -> builder.crashAsymmetryThreshold(-1.0));
        
        // Invalid scale range
        assertThrows(IllegalArgumentException.class, 
            () -> builder.crashScaleRange(10.0, 5.0, 20));
        
        // Zero scales
        assertThrows(IllegalArgumentException.class, 
            () -> builder.crashScaleRange(1.0, 10.0, 0));
        
        // Invalid volatility thresholds
        assertThrows(IllegalArgumentException.class, 
            () -> builder.volatilityThresholds(1.0, 0.5, 2.0));
        
        // Invalid risk level
        assertThrows(IllegalArgumentException.class, 
            () -> builder.baseRiskLevel(1.5));
        
        // Negative risk-free rate
        assertThrows(IllegalArgumentException.class,
            () -> builder.annualRiskFreeRate(-0.01));
    }
    
    @Test
    @DisplayName("Cycle frequencies should be immutable")
    void testCycleFrequenciesImmutability() {
        double[] originalFreqs = {0.1, 0.2, 0.3};
        FinancialAnalysisParameters params = FinancialAnalysisParameters.builder()
            .cycleTestFrequencies(originalFreqs)
            .build();
        
        // Modify original array
        originalFreqs[0] = 0.5;
        
        // Parameters should not be affected
        assertNotEquals(originalFreqs[0], params.getCycleTestFrequencies()[0]);
        
        // Getting frequencies should return a copy
        double[] frequencies = params.getCycleTestFrequencies();
        frequencies[0] = 0.9;
        assertNotEquals(frequencies[0], params.getCycleTestFrequencies()[0]);
    }
    
    @Test
    @DisplayName("Optimization parameters should have defaults")
    void testOptimizationDefaults() {
        FinancialAnalysisParameters params = FinancialAnalysisParameters.defaultParameters();
        OptimizationParameters opt = params.getOptimization();
        
        assertEquals(4, opt.getCrashPaulOrder());
        assertEquals(2, opt.getVolatilityDogOrder());
        assertEquals(2, opt.getCycleShannonFb());
    }
    
    @Test
    @DisplayName("Should support market-specific configurations")
    void testMarketSpecificConfigurations() {
        // Stock market configuration
        FinancialAnalysisParameters stockParams = FinancialAnalysisParameters.builder()
            .crashAsymmetryThreshold(10.0)
            .volatilityThresholds(0.5, 1.5, 3.0)
            .defaultAverageVolatility(0.015)
            .build();
        
        // Crypto market configuration
        FinancialAnalysisParameters cryptoParams = FinancialAnalysisParameters.builder()
            .crashAsymmetryThreshold(20.0)
            .volatilityThresholds(1.0, 3.0, 6.0)
            .regimeTrendThreshold(0.10)
            .defaultAverageVolatility(0.05)
            .build();
        
        // Verify differences
        assertTrue(cryptoParams.getCrashAsymmetryThreshold() > 
                  stockParams.getCrashAsymmetryThreshold());
        assertTrue(cryptoParams.getVolatilityHighThreshold() > 
                  stockParams.getVolatilityHighThreshold());
        assertTrue(cryptoParams.getDefaultAverageVolatility() > 
                  stockParams.getDefaultAverageVolatility());
    }
    
    @Test
    @DisplayName("Should support custom optimization parameters")
    void testCustomOptimizationParameters() {
        OptimizationParameters customOpt = OptimizationParameters.builder()
            .crashPaulOrder(6)
            .volatilityDogOrder(3)
            .cycleShannonParameters(3, 4)
            .build();
        
        FinancialAnalysisParameters params = FinancialAnalysisParameters.builder()
            .withOptimization(customOpt)
            .build();
        
        assertEquals(6, params.getOptimization().getCrashPaulOrder());
        assertEquals(3, params.getOptimization().getVolatilityDogOrder());
        assertEquals(3, params.getOptimization().getCycleShannonFb());
        assertEquals(4, params.getOptimization().getCycleShannonFc());
    }
    
    @Test
    @DisplayName("Should support various risk-free rates")
    void testRiskFreeRateConfigurations() {
        // Test various risk-free rate scenarios
        
        // US Treasury rate scenario
        FinancialAnalysisParameters usTreasuryParams = FinancialAnalysisParameters.builder()
            .annualRiskFreeRate(0.045) // 4.5% annual rate
            .build();
        assertEquals(0.045, usTreasuryParams.getAnnualRiskFreeRate());
        
        // Zero risk-free rate (e.g., during zero interest rate policy)
        FinancialAnalysisParameters zeroRateParams = FinancialAnalysisParameters.builder()
            .annualRiskFreeRate(0.0)
            .build();
        assertEquals(0.0, zeroRateParams.getAnnualRiskFreeRate());
        
        // High interest rate environment
        FinancialAnalysisParameters highRateParams = FinancialAnalysisParameters.builder()
            .annualRiskFreeRate(0.08) // 8% annual rate
            .build();
        assertEquals(0.08, highRateParams.getAnnualRiskFreeRate());
        
        // European negative rate scenario (should validate to allow zero but not negative)
        assertThrows(IllegalArgumentException.class, () -> 
            FinancialAnalysisParameters.builder()
                .annualRiskFreeRate(-0.005) // -0.5% annual rate
                .build()
        );
    }
    
    @Test
    @DisplayName("Builder should handle all parameter combinations")
    void testCompleteBuilderCoverage() {
        // Test a builder that uses every available method to maximize coverage
        FinancialAnalysisParameters params = FinancialAnalysisParameters.builder()
            .crashAsymmetryThreshold(12.0)
            .crashScaleRange(0.5, 15.0, 25)
            .volatilityThresholds(0.4, 1.8, 3.5)
            .volumeDivergenceThreshold(2.5)
            .priceDivergenceThreshold(1.8)
            .cycleTestFrequencies(new double[]{0.05, 0.15, 0.25, 0.35})
            .trendScaleRange(2.0, 20.0, 15)
            .regimeTrendThreshold(0.04)
            .regimeDetectionLookbackPeriod(30)
            .defaultAverageVolatility(0.025)
            .baseRiskLevel(0.6)
            .annualRiskFreeRate(0.05)
            .build();
            
        // Verify all parameters were set correctly
        assertEquals(12.0, params.getCrashAsymmetryThreshold());
        assertEquals(0.5, params.getCrashMinScale());
        assertEquals(15.0, params.getCrashMaxScale());
        assertEquals(25, params.getCrashNumScales());
        assertEquals(0.4, params.getVolatilityLowThreshold());
        assertEquals(1.8, params.getVolatilityMediumThreshold());
        assertEquals(3.5, params.getVolatilityHighThreshold());
        assertEquals(2.5, params.getVolumeDivergenceThreshold());
        assertEquals(1.8, params.getPriceDivergenceThreshold());
        assertArrayEquals(new double[]{0.05, 0.15, 0.25, 0.35}, params.getCycleTestFrequencies());
        assertEquals(2.0, params.getTrendMinScale());
        assertEquals(20.0, params.getTrendMaxScale());
        assertEquals(15, params.getTrendNumScales());
        assertEquals(0.04, params.getRegimeTrendThreshold());
        assertEquals(30, params.getRegimeDetectionLookbackPeriod());
        assertEquals(0.025, params.getDefaultAverageVolatility());
        assertEquals(0.6, params.getBaseRiskLevel());
        assertEquals(0.05, params.getAnnualRiskFreeRate());
    }
    
    @Test
    @DisplayName("Builder should support method chaining for all parameters")
    void testBuilderMethodChaining() {
        // Test that every builder method returns the builder (for method chaining)
        var builder = FinancialAnalysisParameters.builder();
        
        // This tests method chaining and ensures each method returns a builder
        FinancialAnalysisParameters params = builder
            .crashAsymmetryThreshold(8.0)
            .volatilityLowThreshold(0.3)
            .regimeDetectionLookbackPeriod(25)
            .baseRiskLevel(0.4)
            .defaultAverageVolatility(0.018)
            .build();
        
        // Verify some key values to ensure chaining worked
        assertEquals(8.0, params.getCrashAsymmetryThreshold());
        assertEquals(0.3, params.getVolatilityLowThreshold());
        assertEquals(25, params.getRegimeDetectionLookbackPeriod());
        assertEquals(0.4, params.getBaseRiskLevel());
        assertEquals(0.018, params.getDefaultAverageVolatility());
    }
}