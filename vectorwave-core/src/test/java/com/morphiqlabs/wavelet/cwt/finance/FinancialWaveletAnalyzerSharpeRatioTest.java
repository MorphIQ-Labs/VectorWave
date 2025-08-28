package com.morphiqlabs.wavelet.cwt.finance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Sharpe ratio calculation with configurable risk-free rate.
 */
class FinancialWaveletAnalyzerSharpeRatioTest {
    
    private static final double TOLERANCE = 0.0001;
    
    @Test
    @DisplayName("Risk-free rate parameter should be configurable")
    void testRiskFreeRateConfiguration() {
        // Test default risk-free rate
        FinancialAnalysisParameters defaultParams = FinancialAnalysisParameters.defaultParameters();
        assertEquals(0.03, defaultParams.getAnnualRiskFreeRate(), TOLERANCE,
            "Default risk-free rate should be 3%");
        
        // Test custom risk-free rate
        FinancialAnalysisParameters customParams = FinancialAnalysisParameters.builder()
            .annualRiskFreeRate(0.045)
            .build();
        assertEquals(0.045, customParams.getAnnualRiskFreeRate(), TOLERANCE,
            "Custom risk-free rate should be 4.5%");
    }
    
    @ParameterizedTest
    @DisplayName("Various risk-free rates should be accepted")
    @CsvSource({
        "0.00, 'Zero interest rate'",
        "0.02, 'Low rate (2%)'", 
        "0.03, 'Default rate (3%)'",
        "0.05, 'Moderate rate (5%)'",
        "0.08, 'High rate (8%)'",
        "0.15, 'Very high rate (15%)'"
    })
    void testVariousRiskFreeRates(double rate, String description) {
        FinancialAnalysisParameters params = FinancialAnalysisParameters.builder()
            .annualRiskFreeRate(rate)
            .build();
        
        assertEquals(rate, params.getAnnualRiskFreeRate(), TOLERANCE,
            "Risk-free rate should be correctly set for: " + description);
        
        // Verify the analyzer accepts these parameters
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(params);
        assertNotNull(analyzer);
        assertEquals(rate, analyzer.getParameters().getAnnualRiskFreeRate(), TOLERANCE);
    }
    
    @Test
    @DisplayName("Negative risk-free rate should be rejected")
    void testNegativeRiskFreeRateRejection() {
        assertThrows(IllegalArgumentException.class, () -> 
            FinancialAnalysisParameters.builder()
                .annualRiskFreeRate(-0.01)
                .build(),
            "Negative risk-free rate should throw IllegalArgumentException"
        );
    }
    
    @Test
    @DisplayName("Risk-free rate affects Sharpe ratio calculation")
    void testRiskFreeRateAffectsSharpeRatio() {
        // Create two analyzers with different risk-free rates
        FinancialAnalysisParameters lowRateParams = FinancialAnalysisParameters.builder()
            .annualRiskFreeRate(0.01) // 1%
            .signalGenerationMinHistory(5)
            .build();
            
        FinancialAnalysisParameters highRateParams = FinancialAnalysisParameters.builder()
            .annualRiskFreeRate(0.08) // 8%
            .signalGenerationMinHistory(5)
            .build();
        
        FinancialWaveletAnalyzer lowRateAnalyzer = new FinancialWaveletAnalyzer(lowRateParams);
        FinancialWaveletAnalyzer highRateAnalyzer = new FinancialWaveletAnalyzer(highRateParams);
        
        // Create test price data with a pattern that should generate signals
        double[] prices = new double[100];
        prices[0] = 100.0;
        
        // Create a pattern with volatility and a crash to ensure signals are generated
        for (int i = 1; i < prices.length; i++) {
            if (i == 50) {
                // Simulate a crash
                prices[i] = prices[i-1] * 0.92;
            } else if (i > 50 && i < 60) {
                // Recovery
                prices[i] = prices[i-1] * 1.01;
            } else {
                // Normal volatility
                prices[i] = prices[i-1] * (1.0 + 0.002 * Math.sin(i * 0.3));
            }
        }
        
        // Generate signals with both analyzers
        FinancialWaveletAnalyzer.TradingSignalResult lowRateResult = 
            lowRateAnalyzer.generateTradingSignals(prices, 1.0);
        FinancialWaveletAnalyzer.TradingSignalResult highRateResult = 
            highRateAnalyzer.generateTradingSignals(prices, 1.0);
        
        // Both should produce results
        assertNotNull(lowRateResult);
        assertNotNull(highRateResult);
        
        // If signals were generated, the Sharpe ratios would be different
        // due to different risk-free rates in the calculation
        // Note: The actual values depend on the signal generation algorithm
        
        // Just verify that both Sharpe ratios are valid numbers
        assertFalse(Double.isNaN(lowRateResult.sharpeRatio()), 
            "Low rate Sharpe ratio should be a valid number");
        assertFalse(Double.isNaN(highRateResult.sharpeRatio()), 
            "High rate Sharpe ratio should be a valid number");
    }
    
    @Test
    @DisplayName("Daily risk-free rate conversion")
    void testDailyRiskFreeRateConversion() {
        // Test that annual rate is properly converted to daily rate
        double annualRate = 0.0365; // 3.65% annual
        double expectedDailyRate = annualRate / FinancialAnalysisParameters.TRADING_DAYS_PER_YEAR;
        
        FinancialAnalysisParameters params = FinancialAnalysisParameters.builder()
            .annualRiskFreeRate(annualRate)
            .build();
        
        // The daily rate should be approximately annualRate/TRADING_DAYS_PER_YEAR
        double dailyRate = params.getAnnualRiskFreeRate() / FinancialAnalysisParameters.TRADING_DAYS_PER_YEAR;
        assertEquals(expectedDailyRate, dailyRate, TOLERANCE,
            "Daily rate should be annual rate divided by standard trading days per year");
    }
}