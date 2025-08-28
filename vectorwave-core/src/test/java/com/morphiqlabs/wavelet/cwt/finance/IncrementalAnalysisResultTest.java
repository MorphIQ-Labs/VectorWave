package com.morphiqlabs.wavelet.cwt.finance;

import com.morphiqlabs.wavelet.cwt.finance.FinancialWaveletAnalyzer.MarketRegime;
import com.morphiqlabs.wavelet.cwt.finance.FinancialWaveletAnalyzer.SignalType;
import com.morphiqlabs.wavelet.cwt.finance.IncrementalFinancialAnalyzer.IncrementalAnalysisResult;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for IncrementalAnalysisResult record to achieve >70% coverage.
 * 
 * Current coverage: 64% - needs testing of hasSignal() and isHighRisk() methods.
 * Both methods have 0% branch coverage with 4 branches each.
 */
class IncrementalAnalysisResultTest {

    private static final long SAMPLE_INDEX = 123L;
    private static final double PRICE = 100.50;
    private static final double VOLUME = 1000.0;
    private static final double RETURN = 0.02;
    private static final double VOLATILITY = 0.15;
    private static final MarketRegime REGIME = MarketRegime.TRENDING_UP;
    private static final double MAX_DRAWDOWN = -0.05;
    private static final double EMA12 = 99.8;
    private static final double EMA26 = 98.5;
    private static final double EMA50 = 97.2;

    @Test
    void testHasSignalWithHoldSignalAndPositiveStrength() {
        // Test branch: signal == HOLD && signalStrength > 0 → false
        IncrementalAnalysisResult result = new IncrementalAnalysisResult(
            SAMPLE_INDEX, PRICE, VOLUME, RETURN, VOLATILITY, REGIME,
            0.5, SignalType.HOLD, 0.7, false, MAX_DRAWDOWN, EMA12, EMA26, EMA50
        );
        
        assertFalse(result.hasSignal(), "HOLD signal should return false even with positive strength");
    }

    @Test
    void testHasSignalWithHoldSignalAndZeroStrength() {
        // Test branch: signal == HOLD && signalStrength == 0 → false
        IncrementalAnalysisResult result = new IncrementalAnalysisResult(
            SAMPLE_INDEX, PRICE, VOLUME, RETURN, VOLATILITY, REGIME,
            0.5, SignalType.HOLD, 0.0, false, MAX_DRAWDOWN, EMA12, EMA26, EMA50
        );
        
        assertFalse(result.hasSignal(), "HOLD signal with zero strength should return false");
    }

    @Test
    void testHasSignalWithBuySignalAndPositiveStrength() {
        // Test branch: signal != HOLD && signalStrength > 0 → true
        IncrementalAnalysisResult result = new IncrementalAnalysisResult(
            SAMPLE_INDEX, PRICE, VOLUME, RETURN, VOLATILITY, REGIME,
            0.3, SignalType.BUY, 0.8, false, MAX_DRAWDOWN, EMA12, EMA26, EMA50
        );
        
        assertTrue(result.hasSignal(), "BUY signal with positive strength should return true");
    }

    @Test
    void testHasSignalWithSellSignalAndPositiveStrength() {
        // Test branch: signal != HOLD && signalStrength > 0 → true
        IncrementalAnalysisResult result = new IncrementalAnalysisResult(
            SAMPLE_INDEX, PRICE, VOLUME, RETURN, VOLATILITY, REGIME,
            0.9, SignalType.SELL, 0.6, false, MAX_DRAWDOWN, EMA12, EMA26, EMA50
        );
        
        assertTrue(result.hasSignal(), "SELL signal with positive strength should return true");
    }

    @Test
    void testHasSignalWithBuySignalAndZeroStrength() {
        // Test branch: signal != HOLD && signalStrength == 0 → false
        IncrementalAnalysisResult result = new IncrementalAnalysisResult(
            SAMPLE_INDEX, PRICE, VOLUME, RETURN, VOLATILITY, REGIME,
            0.3, SignalType.BUY, 0.0, false, MAX_DRAWDOWN, EMA12, EMA26, EMA50
        );
        
        assertFalse(result.hasSignal(), "BUY signal with zero strength should return false");
    }

    @Test
    void testHasSignalWithSellSignalAndNegativeStrength() {
        // Test branch: signal != HOLD && signalStrength <= 0 → false
        IncrementalAnalysisResult result = new IncrementalAnalysisResult(
            SAMPLE_INDEX, PRICE, VOLUME, RETURN, VOLATILITY, REGIME,
            0.9, SignalType.SELL, -0.1, false, MAX_DRAWDOWN, EMA12, EMA26, EMA50
        );
        
        assertFalse(result.hasSignal(), "SELL signal with negative strength should return false");
    }

    @Test
    void testIsHighRiskWithLowRiskAndNoCrash() {
        // Test branch: riskLevel <= 0.8 && !crashDetected → false
        IncrementalAnalysisResult result = new IncrementalAnalysisResult(
            SAMPLE_INDEX, PRICE, VOLUME, RETURN, VOLATILITY, REGIME,
            0.7, SignalType.HOLD, 0.0, false, MAX_DRAWDOWN, EMA12, EMA26, EMA50
        );
        
        assertFalse(result.isHighRisk(), "Low risk level with no crash should return false");
    }

    @Test
    void testIsHighRiskWithExactThresholdAndNoCrash() {
        // Test edge case: riskLevel == 0.8 && !crashDetected → false
        IncrementalAnalysisResult result = new IncrementalAnalysisResult(
            SAMPLE_INDEX, PRICE, VOLUME, RETURN, VOLATILITY, REGIME,
            0.8, SignalType.HOLD, 0.0, false, MAX_DRAWDOWN, EMA12, EMA26, EMA50
        );
        
        assertFalse(result.isHighRisk(), "Risk level at threshold with no crash should return false");
    }

    @Test
    void testIsHighRiskWithHighRiskAndNoCrash() {
        // Test branch: riskLevel > 0.8 && !crashDetected → true
        IncrementalAnalysisResult result = new IncrementalAnalysisResult(
            SAMPLE_INDEX, PRICE, VOLUME, RETURN, VOLATILITY, REGIME,
            0.9, SignalType.HOLD, 0.0, false, MAX_DRAWDOWN, EMA12, EMA26, EMA50
        );
        
        assertTrue(result.isHighRisk(), "High risk level should return true even without crash");
    }

    @Test
    void testIsHighRiskWithLowRiskAndCrash() {
        // Test branch: riskLevel <= 0.8 && crashDetected → true
        IncrementalAnalysisResult result = new IncrementalAnalysisResult(
            SAMPLE_INDEX, PRICE, VOLUME, RETURN, VOLATILITY, REGIME,
            0.5, SignalType.HOLD, 0.0, true, MAX_DRAWDOWN, EMA12, EMA26, EMA50
        );
        
        assertTrue(result.isHighRisk(), "Crash detected should return true even with low risk level");
    }

    @Test
    void testIsHighRiskWithHighRiskAndCrash() {
        // Test branch: riskLevel > 0.8 && crashDetected → true
        IncrementalAnalysisResult result = new IncrementalAnalysisResult(
            SAMPLE_INDEX, PRICE, VOLUME, RETURN, VOLATILITY, REGIME,
            0.95, SignalType.SELL, 0.8, true, MAX_DRAWDOWN, EMA12, EMA26, EMA50
        );
        
        assertTrue(result.isHighRisk(), "High risk level with crash should return true");
    }

    @Test
    void testRecordFieldsAccessibility() {
        // Test the record constructor and field access (already 100% covered but good to verify)
        IncrementalAnalysisResult result = new IncrementalAnalysisResult(
            SAMPLE_INDEX, PRICE, VOLUME, RETURN, VOLATILITY, REGIME,
            0.6, SignalType.BUY, 0.7, false, MAX_DRAWDOWN, EMA12, EMA26, EMA50
        );
        
        assertEquals(SAMPLE_INDEX, result.sampleIndex());
        assertEquals(PRICE, result.price(), 1e-9);
        assertEquals(VOLUME, result.volume(), 1e-9);
        assertEquals(RETURN, result.return_(), 1e-9);
        assertEquals(VOLATILITY, result.volatility(), 1e-9);
        assertEquals(REGIME, result.regime());
        assertEquals(0.6, result.riskLevel(), 1e-9);
        assertEquals(SignalType.BUY, result.signal());
        assertEquals(0.7, result.signalStrength(), 1e-9);
        assertFalse(result.crashDetected());
        assertEquals(MAX_DRAWDOWN, result.maxDrawdown(), 1e-9);
        assertEquals(EMA12, result.ema12(), 1e-9);
        assertEquals(EMA26, result.ema26(), 1e-9);
        assertEquals(EMA50, result.ema50(), 1e-9);
    }

    @Test
    void testComplexScenarios() {
        // Test various realistic scenarios to ensure comprehensive coverage
        
        // Scenario 1: Strong buy signal in uptrend
        IncrementalAnalysisResult buySignal = new IncrementalAnalysisResult(
            100L, 105.0, 2000.0, 0.05, 0.12, MarketRegime.TRENDING_UP,
            0.2, SignalType.BUY, 0.9, false, -0.02, 104.5, 103.8, 102.1
        );
        assertTrue(buySignal.hasSignal());
        assertFalse(buySignal.isHighRisk());
        
        // Scenario 2: Crash detected with moderate risk
        IncrementalAnalysisResult crashScenario = new IncrementalAnalysisResult(
            200L, 95.0, 5000.0, -0.08, 0.35, MarketRegime.VOLATILE,
            0.6, SignalType.SELL, 0.8, true, -0.15, 96.2, 97.5, 98.8
        );
        assertTrue(crashScenario.hasSignal());
        assertTrue(crashScenario.isHighRisk());
        
        // Scenario 3: Ranging market with no signal
        IncrementalAnalysisResult neutralScenario = new IncrementalAnalysisResult(
            300L, 100.0, 1500.0, 0.001, 0.08, MarketRegime.RANGING,
            0.4, SignalType.HOLD, 0.3, false, -0.01, 100.1, 100.0, 99.9
        );
        assertFalse(neutralScenario.hasSignal());
        assertFalse(neutralScenario.isHighRisk());
    }

    @Test
    void testEdgeCasesForSignalStrength() {
        // Test very small positive signal strength
        IncrementalAnalysisResult smallSignal = new IncrementalAnalysisResult(
            50L, 98.0, 800.0, -0.01, 0.10, MarketRegime.TRENDING_DOWN,
            0.7, SignalType.SELL, 0.001, false, -0.03, 98.2, 98.5, 99.0
        );
        assertTrue(smallSignal.hasSignal(), "Even very small positive signal strength should return true");
        
        // Test exactly zero signal strength with non-HOLD signal
        IncrementalAnalysisResult zeroSignal = new IncrementalAnalysisResult(
            51L, 98.0, 800.0, -0.01, 0.10, MarketRegime.TRENDING_DOWN,
            0.7, SignalType.BUY, 0.0, false, -0.03, 98.2, 98.5, 99.0
        );
        assertFalse(zeroSignal.hasSignal(), "Zero signal strength should return false");
    }

    @Test
    void testEdgeCasesForRiskLevel() {
        // Test risk level just above threshold
        IncrementalAnalysisResult justAboveThreshold = new IncrementalAnalysisResult(
            75L, 102.0, 1200.0, 0.015, 0.18, MarketRegime.VOLATILE,
            0.8001, SignalType.HOLD, 0.0, false, -0.04, 101.8, 101.5, 101.0
        );
        assertTrue(justAboveThreshold.isHighRisk(), "Risk level just above 0.8 should return true");
        
        // Test risk level just below threshold  
        IncrementalAnalysisResult justBelowThreshold = new IncrementalAnalysisResult(
            76L, 102.0, 1200.0, 0.015, 0.18, MarketRegime.VOLATILE,
            0.7999, SignalType.HOLD, 0.0, false, -0.04, 101.8, 101.5, 101.0
        );
        assertFalse(justBelowThreshold.isHighRisk(), "Risk level just below 0.8 should return false");
    }
}