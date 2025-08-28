package com.morphiqlabs.financial;

import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.api.BoundaryMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests to improve FinancialWaveletAnalyzer coverage from 62% to 70%.
 */
class FinancialWaveletAnalyzerCoverageTest {
    
    private FinancialConfig config;
    private MODWTTransform transform;
    
    @BeforeEach
    void setUp() {
        config = new FinancialConfig(0.045); // 4.5% risk-free rate
        transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
    }
    
    @Test
    @DisplayName("Should throw exception for null config in primary constructor")
    void testNullConfigConstructor() {
        assertThrows(IllegalArgumentException.class, 
            () -> new FinancialWaveletAnalyzer(null),
            "Should throw exception for null config");
    }
    
    @Test
    @DisplayName("Should create analyzer with custom transform")
    void testConstructorWithTransform() {
        // Test valid construction
        DiscreteWavelet db4 = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.DB4);
        MODWTTransform customTransform = new MODWTTransform(db4, BoundaryMode.SYMMETRIC);
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config, customTransform);
        assertNotNull(analyzer);
        
        // Test getters
        assertEquals(config, analyzer.getConfig());
        assertEquals(customTransform, analyzer.getTransform());
    }
    
    @Test
    @DisplayName("Should throw exception for null config in transform constructor")
    void testNullConfigTransformConstructor() {
        assertThrows(IllegalArgumentException.class, 
            () -> new FinancialWaveletAnalyzer(null, transform),
            "Should throw exception for null config");
    }
    
    @Test
    @DisplayName("Should throw exception for null transform in constructor")
    void testNullTransformConstructor() {
        assertThrows(IllegalArgumentException.class, 
            () -> new FinancialWaveletAnalyzer(config, null),
            "Should throw exception for null transform");
    }
    
    @Test
    @DisplayName("Should throw exception for null returns in calculateSharpeRatio")
    void testSharpeRatioNullReturns() {
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        
        assertThrows(IllegalArgumentException.class,
            () -> analyzer.calculateSharpeRatio(null),
            "Should throw exception for null returns");
        
        assertThrows(IllegalArgumentException.class,
            () -> analyzer.calculateSharpeRatio(null, 0.05),
            "Should throw exception for null returns with custom rate");
    }
    
    @Test
    @DisplayName("Should throw exception for empty returns in calculateSharpeRatio")
    void testSharpeRatioEmptyReturns() {
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        double[] emptyReturns = new double[0];
        
        assertThrows(IllegalArgumentException.class,
            () -> analyzer.calculateSharpeRatio(emptyReturns),
            "Should throw exception for empty returns");
        
        assertThrows(IllegalArgumentException.class,
            () -> analyzer.calculateSharpeRatio(emptyReturns, 0.05),
            "Should throw exception for empty returns with custom rate");
    }
    
    @Test
    @DisplayName("Should throw exception for single return in calculateSharpeRatio")
    void testSharpeRatioSingleReturn() {
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        double[] singleReturn = {0.05};
        
        assertThrows(IllegalArgumentException.class,
            () -> analyzer.calculateSharpeRatio(singleReturn),
            "Should throw exception for single return");
        
        assertThrows(IllegalArgumentException.class,
            () -> analyzer.calculateSharpeRatio(singleReturn, 0.03),
            "Should throw exception for single return with custom rate");
    }
    
    @Test
    @DisplayName("Should handle zero standard deviation in calculateSharpeRatio")
    void testSharpeRatioZeroStdDev() {
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        
        // All returns are the same
        double[] constantReturns = {0.05, 0.05, 0.05, 0.05};
        
        // When mean equals risk-free rate
        double result1 = analyzer.calculateSharpeRatio(constantReturns, 0.05);
        assertEquals(0.0, result1, "Should return 0 when mean equals risk-free rate");
        
        // When mean > risk-free rate
        double result2 = analyzer.calculateSharpeRatio(constantReturns, 0.03);
        assertEquals(Double.POSITIVE_INFINITY, result2, 
            "Should return positive infinity when mean > risk-free rate with zero std");
        
        // When mean < risk-free rate
        double result3 = analyzer.calculateSharpeRatio(constantReturns, 0.07);
        assertEquals(Double.NEGATIVE_INFINITY, result3,
            "Should return negative infinity when mean < risk-free rate with zero std");
    }
    
    @Test
    @DisplayName("Should throw exception for null returns in wavelet Sharpe ratio")
    void testWaveletSharpeRatioNullReturns() {
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        
        assertThrows(IllegalArgumentException.class,
            () -> analyzer.calculateWaveletSharpeRatio(null),
            "Should throw exception for null returns");
        
        assertThrows(IllegalArgumentException.class,
            () -> analyzer.calculateWaveletSharpeRatio(null, 0.05),
            "Should throw exception for null returns with custom rate");
    }
    
    @Test
    @DisplayName("Should throw exception for empty returns in wavelet Sharpe ratio")
    void testWaveletSharpeRatioEmptyReturns() {
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        double[] emptyReturns = new double[0];
        
        assertThrows(IllegalArgumentException.class,
            () -> analyzer.calculateWaveletSharpeRatio(emptyReturns),
            "Should throw exception for empty returns");
        
        assertThrows(IllegalArgumentException.class,
            () -> analyzer.calculateWaveletSharpeRatio(emptyReturns, 0.05),
            "Should throw exception for empty returns with custom rate");
    }
    
    @Test
    @DisplayName("Should calculate valid Sharpe ratio")
    void testValidSharpeRatio() {
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        
        // Sample returns with some variance
        double[] returns = {0.02, 0.05, 0.03, -0.01, 0.04, 0.06, 0.01, 0.03};
        
        // Calculate standard Sharpe ratio
        double sharpe1 = analyzer.calculateSharpeRatio(returns);
        assertFalse(Double.isNaN(sharpe1));
        assertFalse(Double.isInfinite(sharpe1));
        
        // Calculate with custom risk-free rate
        double sharpe2 = analyzer.calculateSharpeRatio(returns, 0.02);
        assertFalse(Double.isNaN(sharpe2));
        assertFalse(Double.isInfinite(sharpe2));
        
        // Sharpe ratio with lower risk-free rate should be higher
        assertTrue(sharpe2 > sharpe1);
    }
    
    @Test
    @DisplayName("Should calculate wavelet Sharpe ratio")
    void testWaveletSharpeRatio() {
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        
        // Sample returns with noise
        double[] returns = new double[64];
        for (int i = 0; i < returns.length; i++) {
            returns[i] = 0.03 + 0.02 * Math.sin(2 * Math.PI * i / 16) 
                       + 0.01 * (Math.random() - 0.5);
        }
        
        // Calculate wavelet Sharpe ratio
        double waveletSharpe1 = analyzer.calculateWaveletSharpeRatio(returns);
        assertFalse(Double.isNaN(waveletSharpe1));
        
        // Calculate with custom risk-free rate
        double waveletSharpe2 = analyzer.calculateWaveletSharpeRatio(returns, 0.02);
        assertFalse(Double.isNaN(waveletSharpe2));
        
        // Wavelet denoising should typically produce different (often higher) Sharpe ratio
        double regularSharpe = analyzer.calculateSharpeRatio(returns);
        assertNotEquals(regularSharpe, waveletSharpe1, 0.001);
    }
    
    @Test
    @DisplayName("Should handle small returns arrays")
    void testSmallReturnsArrays() {
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        
        // Minimum valid array (2 elements)
        double[] minReturns = {0.02, 0.04};
        
        double sharpe = analyzer.calculateSharpeRatio(minReturns);
        assertFalse(Double.isNaN(sharpe));
        
        double waveletSharpe = analyzer.calculateWaveletSharpeRatio(minReturns);
        assertFalse(Double.isNaN(waveletSharpe));
    }
    
    @Test
    @DisplayName("Should handle returns with outliers")
    void testReturnsWithOutliers() {
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        
        // Returns with outliers
        double[] returns = {0.02, 0.03, 0.50, 0.02, -0.40, 0.03, 0.02, 0.04};
        
        double sharpe = analyzer.calculateSharpeRatio(returns);
        assertFalse(Double.isNaN(sharpe));
        
        double waveletSharpe = analyzer.calculateWaveletSharpeRatio(returns);
        assertFalse(Double.isNaN(waveletSharpe));
        
        // Wavelet denoising should smooth outliers
        assertNotEquals(sharpe, waveletSharpe);
    }
    
    @Test
    @DisplayName("Should test configuration getters and setters")
    void testGettersSetters() {
        // Create analyzer with config and transform
        DiscreteWavelet db6 = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.DB6);
        MODWTTransform customTransform = new MODWTTransform(db6, BoundaryMode.SYMMETRIC);
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config, customTransform);
        
        // Test getters
        assertSame(config, analyzer.getConfig());
        assertSame(customTransform, analyzer.getTransform());
        
        // Verify configuration values
        assertEquals(0.045, analyzer.getConfig().getRiskFreeRate());
    }
    
    @Test
    @DisplayName("Should handle negative returns")
    void testNegativeReturns() {
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        
        // All negative returns
        double[] negativeReturns = {-0.02, -0.05, -0.03, -0.01, -0.04};
        
        double sharpe = analyzer.calculateSharpeRatio(negativeReturns);
        assertTrue(sharpe < 0, "Sharpe ratio should be negative for negative returns");
        
        double waveletSharpe = analyzer.calculateWaveletSharpeRatio(negativeReturns);
        assertFalse(Double.isNaN(waveletSharpe));
    }
    
    @Test
    @DisplayName("Should handle mixed positive and negative returns")
    void testMixedReturns() {
        FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
        
        // Mixed returns
        double[] mixedReturns = {0.05, -0.03, 0.02, -0.01, 0.04, -0.02, 0.03, -0.01};
        
        double sharpe = analyzer.calculateSharpeRatio(mixedReturns);
        assertFalse(Double.isNaN(sharpe));
        
        double waveletSharpe = analyzer.calculateWaveletSharpeRatio(mixedReturns);
        assertFalse(Double.isNaN(waveletSharpe));
    }
}