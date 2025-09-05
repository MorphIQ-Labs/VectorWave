package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.cwt.FrequencyBSplineWavelet;
import com.morphiqlabs.wavelet.cwt.finance.ClassicalShannonWavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that Shannon and FBSP wavelets are properly separated
 * and produce different outputs.
 */
class WaveletSeparationTest {

    @Test
    @DisplayName("Shannon and FBSP wavelets should be different classes")
    void testWaveletsAreDifferentClasses() {
        Wavelet shannon = WaveletRegistry.getWavelet(WaveletName.SHANNON);
        Wavelet fbsp = WaveletRegistry.getWavelet(WaveletName.FBSP);
        
        // Should be different classes
        assertNotEquals(shannon.getClass(), fbsp.getClass());
        
        // Shannon should be ClassicalShannonWavelet
        assertTrue(shannon instanceof ClassicalShannonWavelet);
        
        // FBSP should be FrequencyBSplineWavelet  
        assertTrue(fbsp instanceof FrequencyBSplineWavelet);
    }
    
    @Test
    @DisplayName("Shannon and FBSP wavelets should produce different values")
    void testWaveletsProduceDifferentValues() {
        ContinuousWavelet shannon = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.SHANNON);
        ContinuousWavelet fbsp = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.FBSP);
        
        // Test at several points
        double[] testPoints = {0.0, 0.5, 1.0, 1.5, 2.0};
        
        for (double t : testPoints) {
            double shannonVal = shannon.psi(t);
            double fbspVal = fbsp.psi(t);
            
            // Values should be different (allowing for small numerical differences)
            if (Math.abs(shannonVal) > 1e-10 || Math.abs(fbspVal) > 1e-10) {
                assertNotEquals(shannonVal, fbspVal, 1e-10, 
                    "Values should be different at t=" + t);
            }
        }
    }
    
    @Test
    @DisplayName("Shannon and FBSP should have different discretized patterns")
    void testDiscretizedPatternsAreDifferent() {
        ContinuousWavelet shannon = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.SHANNON);
        ContinuousWavelet fbsp = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.FBSP);
        
        double[] shannonVals = shannon.discretize(100);
        double[] fbspVals = fbsp.discretize(100);
        
        // Arrays should have same length but different values
        assertEquals(shannonVals.length, fbspVals.length);
        
        // Calculate correlation coefficient
        double correlation = computeCorrelation(shannonVals, fbspVals);
        
        // Wavelets should be distinct (low correlation)
        assertTrue(correlation < 0.9, 
            "Wavelets should be distinct, correlation=" + correlation);
    }
    
    @Test
    @DisplayName("Shannon wavelet should be real-valued")
    void testShannonIsReal() {
        ContinuousWavelet shannon = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.SHANNON);
        assertFalse(shannon.isComplex(), "Shannon wavelet should be real-valued");
    }
    
    @Test
    @DisplayName("FBSP wavelet should be complex-valued")
    void testFBSPIsComplex() {
        ContinuousWavelet fbsp = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.FBSP);
        assertTrue(fbsp.isComplex(), "FBSP wavelet should be complex-valued");
    }
    
    @Test
    @DisplayName("Shannon sinc property at zero")
    void testShannonSincProperty() {
        ClassicalShannonWavelet shannon = new ClassicalShannonWavelet();
        
        // At t=0, Classical Shannon should equal 1.0 (2*sinc(0) - sinc(0) = 2*1 - 1 = 1)
        assertEquals(1.0, shannon.psi(0), 1e-10);
    }
    
    @Test 
    @DisplayName("FBSP B-spline order property")
    void testFBSPBSplineProperty() {
        FrequencyBSplineWavelet fbsp = new FrequencyBSplineWavelet(2, 1.0, 1.0);
        
        // Verify B-spline order can be retrieved
        assertEquals(2, fbsp.getOrder());
        
        // Verify frequency domain property at omega=0
        double[] psiHat = fbsp.psiHat(0);
        assertEquals(1.0, Math.sqrt(psiHat[0]*psiHat[0] + psiHat[1]*psiHat[1]), 1e-10);
    }
    
    private double computeCorrelation(double[] x, double[] y) {
        if (x.length != y.length) {
            throw new IllegalArgumentException("Arrays must have same length");
        }
        
        double meanX = 0, meanY = 0;
        for (int i = 0; i < x.length; i++) {
            meanX += x[i];
            meanY += y[i];
        }
        meanX /= x.length;
        meanY /= y.length;
        
        double numerator = 0, sumXX = 0, sumYY = 0;
        for (int i = 0; i < x.length; i++) {
            double dx = x[i] - meanX;
            double dy = y[i] - meanY;
            numerator += dx * dy;
            sumXX += dx * dx;
            sumYY += dy * dy;
        }
        
        double denominator = Math.sqrt(sumXX * sumYY);
        return denominator == 0 ? 0 : numerator / denominator;
    }
}