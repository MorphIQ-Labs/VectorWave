package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.math.Complex;
import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for WaveletEvaluationUtils.
 */
class WaveletEvaluationUtilsTest {

    @Test
    @DisplayName("evaluateAsComplex should throw NullPointerException for null wavelet")
    void testEvaluateAsComplexThrowsNullPointerExceptionForNullWavelet() {
        // Test the null check that was requested in the issue
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            WaveletEvaluationUtils.evaluateAsComplex(null, 1.0);
        });
        
        assertEquals("The wavelet parameter cannot be null.", exception.getMessage());
    }

    @Test
    @DisplayName("evaluateAsComplex should return complex result for Morlet wavelet")
    void testEvaluateAsComplexWithMorletWavelet() {
        MorletWavelet morlet = new MorletWavelet();
        double t = 1.0;
        
        Complex result = WaveletEvaluationUtils.evaluateAsComplex(morlet, t);
        
        assertNotNull(result);
        // Verify that both real and imaginary parts are calculated
        double expectedReal = morlet.psi(t);
        double expectedImaginary = morlet.psiImaginary(t);
        
        assertEquals(expectedReal, result.real(), 1e-10);
        assertEquals(expectedImaginary, result.imaginary(), 1e-10);
    }

    @Test
    @DisplayName("evaluateAsComplex should return real-only result for non-complex wavelets")
    void testEvaluateAsComplexWithRealWavelet() {
        // Create a simple real-valued continuous wavelet for testing
        ContinuousWavelet realWavelet = new TestRealWavelet();
        double t = 0.5;
        
        Complex result = WaveletEvaluationUtils.evaluateAsComplex(realWavelet, t);
        
        assertNotNull(result);
        assertEquals(Math.exp(-0.5 * t * t), result.real(), 1e-10); // Expected from TestRealWavelet
        assertEquals(0.0, result.imaginary(), 1e-10); // Should be zero for real wavelets
    }

    @Test
    @DisplayName("evaluateAsComplex with scale and translation should throw NullPointerException for null wavelet")
    void testEvaluateAsComplexWithScaleThrowsNullPointerExceptionForNullWavelet() {
        NullPointerException exception = assertThrows(NullPointerException.class, () -> {
            WaveletEvaluationUtils.evaluateAsComplex(null, 1.0, 2.0, 0.5);
        });
        
        assertEquals("The wavelet parameter cannot be null.", exception.getMessage());
    }

    @Test
    @DisplayName("evaluateAsComplex with scale and translation should throw IllegalArgumentException for non-positive scale")
    void testEvaluateAsComplexWithScaleThrowsForNonPositiveScale() {
        MorletWavelet morlet = new MorletWavelet();
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            WaveletEvaluationUtils.evaluateAsComplex(morlet, 1.0, 0.0, 0.5);
        });
        
        assertEquals("Scale must be positive", exception.getMessage());
        
        exception = assertThrows(IllegalArgumentException.class, () -> {
            WaveletEvaluationUtils.evaluateAsComplex(morlet, 1.0, -1.0, 0.5);
        });
        
        assertEquals("Scale must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("evaluateAsComplex with scale and translation should work correctly for Morlet wavelet")
    void testEvaluateAsComplexWithScaleAndTranslationForMorlet() {
        MorletWavelet morlet = new MorletWavelet();
        double t = 2.0;
        double scale = 4.0;
        double translation = 1.0;
        
        Complex result = WaveletEvaluationUtils.evaluateAsComplex(morlet, t, scale, translation);
        
        assertNotNull(result);
        
        // Manual calculation for verification
        double transformedT = (t - translation) / scale; // (2.0 - 1.0) / 4.0 = 0.25
        double scaleFactor = 1.0 / Math.sqrt(scale); // 1 / sqrt(4) = 0.5
        double expectedReal = scaleFactor * morlet.psi(transformedT);
        double expectedImaginary = scaleFactor * morlet.psiImaginary(transformedT);
        
        assertEquals(expectedReal, result.real(), 1e-10);
        assertEquals(expectedImaginary, result.imaginary(), 1e-10);
    }

    @Test
    @DisplayName("evaluateAsComplex with scale and translation should work correctly for real wavelets")
    void testEvaluateAsComplexWithScaleAndTranslationForRealWavelet() {
        ContinuousWavelet realWavelet = new TestRealWavelet();
        double t = 3.0;
        double scale = 2.0;
        double translation = 1.0;
        
        Complex result = WaveletEvaluationUtils.evaluateAsComplex(realWavelet, t, scale, translation);
        
        assertNotNull(result);
        
        // Manual calculation for verification
        double transformedT = (t - translation) / scale; // (3.0 - 1.0) / 2.0 = 1.0
        double scaleFactor = 1.0 / Math.sqrt(scale); // 1 / sqrt(2)
        double expectedReal = scaleFactor * Math.exp(-0.5 * transformedT * transformedT);
        
        assertEquals(expectedReal, result.real(), 1e-10);
        assertEquals(0.0, result.imaginary(), 1e-10);
    }

    @Test
    @DisplayName("evaluateAsComplex should handle edge cases")
    void testEvaluateAsComplexEdgeCases() {
        MorletWavelet morlet = new MorletWavelet();
        
        // Test at t = 0
        Complex resultAtZero = WaveletEvaluationUtils.evaluateAsComplex(morlet, 0.0);
        assertNotNull(resultAtZero);
        assertEquals(morlet.psi(0.0), resultAtZero.real(), 1e-10);
        assertEquals(morlet.psiImaginary(0.0), resultAtZero.imaginary(), 1e-10);
        
        // Test with very small scale
        Complex resultSmallScale = WaveletEvaluationUtils.evaluateAsComplex(morlet, 1.0, 0.001, 0.0);
        assertNotNull(resultSmallScale);
        assertTrue(Double.isFinite(resultSmallScale.real()));
        assertTrue(Double.isFinite(resultSmallScale.imaginary()));
        
        // Test with very large scale
        Complex resultLargeScale = WaveletEvaluationUtils.evaluateAsComplex(morlet, 1.0, 1000.0, 0.0);
        assertNotNull(resultLargeScale);
        assertTrue(Double.isFinite(resultLargeScale.real()));
        assertTrue(Double.isFinite(resultLargeScale.imaginary()));
    }

    /**
     * Simple test implementation of a real-valued continuous wavelet.
     * Uses a Gaussian function for testing purposes.
     */
    private static class TestRealWavelet implements ContinuousWavelet {
        @Override
        public String name() {
            return "test_real";
        }

        @Override
        public String description() {
            return "Test real-valued wavelet";
        }

        @Override
        public double psi(double t) {
            // Simple Gaussian function
            return Math.exp(-0.5 * t * t);
        }

        @Override
        public double centerFrequency() {
            return 1.0;
        }

        @Override
        public double bandwidth() {
            return 1.0;
        }

        @Override
        public boolean isComplex() {
            return false;
        }

        @Override
        public double[] discretize(int numCoeffs) {
            double[] coeffs = new double[numCoeffs];
            double t0 = -3.0;
            double dt = 6.0 / (numCoeffs - 1);
            
            for (int i = 0; i < numCoeffs; i++) {
                double t = t0 + i * dt;
                coeffs[i] = psi(t);
            }
            
            return coeffs;
        }
    }
}