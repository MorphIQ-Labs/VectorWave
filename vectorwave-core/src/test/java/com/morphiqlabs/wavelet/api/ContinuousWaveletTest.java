package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ContinuousWavelet interface default methods.
 */
class ContinuousWaveletTest {

    private static final double EPSILON = 1e-10;

    /**
     * Test implementation of ContinuousWavelet for testing default methods.
     */
    private static class TestContinuousWavelet implements ContinuousWavelet {
        private final double centerFreq;
        private final double bandwidth;
        private final boolean isComplex;

        public TestContinuousWavelet(double centerFreq, double bandwidth, boolean isComplex) {
            this.centerFreq = centerFreq;
            this.bandwidth = bandwidth;
            this.isComplex = isComplex;
        }

        @Override
        public double psi(double t) {
            // Simple Gaussian-like function for testing
            return Math.exp(-t * t / 2);
        }

        @Override
        public double centerFrequency() {
            return centerFreq;
        }

        @Override
        public double bandwidth() {
            return bandwidth;
        }

        @Override
        public boolean isComplex() {
            return isComplex;
        }

        @Override
        public double[] discretize(int numCoeffs) {
            double[] coeffs = new double[numCoeffs];
            for (int i = 0; i < numCoeffs; i++) {
                double t = (i - numCoeffs / 2.0) * 0.5;
                coeffs[i] = psi(t);
            }
            return coeffs;
        }

        @Override
        public String name() {
            return "TestContinuous";
        }

    }

    @Test
    @DisplayName("Test scaled and translated psi method with valid parameters")
    void testPsiScaledTranslated() {
        ContinuousWavelet wavelet = new TestContinuousWavelet(1.0, 1.0, false);
        
        // Test with scale=1, translation=0 (should match original psi)
        double t = 2.0;
        double original = wavelet.psi(t);
        double scaledTranslated = wavelet.psi(t, 1.0, 0.0);
        assertEquals(original, scaledTranslated, EPSILON);
        
        // Test with different scale
        double scale = 2.0;
        double translation = 1.0;
        double expected = (1.0 / Math.sqrt(scale)) * wavelet.psi((t - translation) / scale);
        double actual = wavelet.psi(t, scale, translation);
        assertEquals(expected, actual, EPSILON);
    }

    @Test
    @DisplayName("Test psi with zero scale throws exception")
    void testPsiWithZeroScale() {
        ContinuousWavelet wavelet = new TestContinuousWavelet(1.0, 1.0, false);
        
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class, () -> {
            wavelet.psi(1.0, 0.0, 0.0);
        });
        assertEquals("Scale must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Test psi with negative scale throws exception")
    void testPsiWithNegativeScale() {
        ContinuousWavelet wavelet = new TestContinuousWavelet(1.0, 1.0, false);
        
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class, () -> {
            wavelet.psi(1.0, -1.0, 0.0);
        });
        assertEquals("Scale must be positive", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.1, 0.5, 1.0, 2.0, 5.0, 10.0})
    @DisplayName("Test psi scaling with various positive scales")
    void testPsiScaling(double scale) {
        ContinuousWavelet wavelet = new TestContinuousWavelet(1.0, 1.0, false);
        double t = 1.0;
        double translation = 0.5;
        
        // Test that scaling works correctly
        double result = wavelet.psi(t, scale, translation);
        assertFalse(Double.isNaN(result));
        assertFalse(Double.isInfinite(result));
        
        // Verify scaling factor is applied
        double expectedScaleFactor = 1.0 / Math.sqrt(scale);
        double unscaledValue = wavelet.psi((t - translation) / scale);
        assertEquals(expectedScaleFactor * unscaledValue, result, EPSILON);
    }

    @Test
    @DisplayName("Test getType default method returns CONTINUOUS")
    void testGetType() {
        ContinuousWavelet wavelet = new TestContinuousWavelet(1.0, 1.0, false);
        assertEquals(WaveletType.CONTINUOUS, wavelet.getType());
    }

    @Test
    @DisplayName("Test lowPassDecomposition default method")
    void testLowPassDecomposition() {
        ContinuousWavelet wavelet = new TestContinuousWavelet(1.0, 1.0, false);
        
        // Default implementation should call discretize(8)
        double[] lowPass = wavelet.lowPassDecomposition();
        assertNotNull(lowPass);
        assertEquals(8, lowPass.length); // Default discretization size
        
        // Should match discretize(8)
        double[] discretized = wavelet.discretize(8);
        assertArrayEquals(discretized, lowPass, EPSILON);
    }

    @Test
    @DisplayName("Test highPassDecomposition default method")
    void testHighPassDecomposition() {
        ContinuousWavelet wavelet = new TestContinuousWavelet(1.0, 1.0, false);
        
        // Default implementation should call discretize(8) for high-pass too
        double[] highPass = wavelet.highPassDecomposition();
        assertNotNull(highPass);
        assertEquals(8, highPass.length);
    }

    @Test
    @DisplayName("Test lowPassReconstruction default method")
    void testLowPassReconstruction() {
        ContinuousWavelet wavelet = new TestContinuousWavelet(1.0, 1.0, false);
        
        double[] lowPassRecon = wavelet.lowPassReconstruction();
        assertNotNull(lowPassRecon);
        assertEquals(8, lowPassRecon.length);
    }

    @Test
    @DisplayName("Test highPassReconstruction default method")
    void testHighPassReconstruction() {
        ContinuousWavelet wavelet = new TestContinuousWavelet(1.0, 1.0, false);
        
        double[] highPassRecon = wavelet.highPassReconstruction();
        assertNotNull(highPassRecon);
        assertEquals(8, highPassRecon.length);
    }

    @Test
    @DisplayName("Test different center frequencies")
    void testCenterFrequencies() {
        ContinuousWavelet wavelet1 = new TestContinuousWavelet(0.5, 1.0, false);
        assertEquals(0.5, wavelet1.centerFrequency(), EPSILON);
        
        ContinuousWavelet wavelet2 = new TestContinuousWavelet(2.0, 1.0, false);
        assertEquals(2.0, wavelet2.centerFrequency(), EPSILON);
    }

    @Test
    @DisplayName("Test different bandwidths")
    void testBandwidths() {
        ContinuousWavelet wavelet1 = new TestContinuousWavelet(1.0, 0.5, false);
        assertEquals(0.5, wavelet1.bandwidth(), EPSILON);
        
        ContinuousWavelet wavelet2 = new TestContinuousWavelet(1.0, 2.0, false);
        assertEquals(2.0, wavelet2.bandwidth(), EPSILON);
    }

    @Test
    @DisplayName("Test complex and real wavelets")
    void testComplexity() {
        ContinuousWavelet realWavelet = new TestContinuousWavelet(1.0, 1.0, false);
        assertFalse(realWavelet.isComplex());
        
        ContinuousWavelet complexWavelet = new TestContinuousWavelet(1.0, 1.0, true);
        assertTrue(complexWavelet.isComplex());
    }

    @Test
    @DisplayName("Test translation effects on psi")
    void testTranslationEffects() {
        ContinuousWavelet wavelet = new TestContinuousWavelet(1.0, 1.0, false);
        double t = 3.0;
        double scale = 1.0;
        
        // Test different translations
        double result1 = wavelet.psi(t, scale, 0.0);   // no translation
        double result2 = wavelet.psi(t, scale, 1.0);   // translate by 1
        double result3 = wavelet.psi(t, scale, -1.0);  // translate by -1
        
        // Results should be different (unless wavelet is constant)
        assertNotEquals(result1, result2, EPSILON);
        assertNotEquals(result1, result3, EPSILON);
        
        // Verify translation is applied correctly
        assertEquals(wavelet.psi(t - 1.0), result2, EPSILON);
        assertEquals(wavelet.psi(t + 1.0), result3, EPSILON);
    }

    @Test
    @DisplayName("Test discretize with different sizes")
    void testDiscretizeVariousSizes() {
        ContinuousWavelet wavelet = new TestContinuousWavelet(1.0, 1.0, false);
        
        // Test various discretization sizes
        for (int size : new int[]{1, 2, 4, 8, 16, 32}) {
            double[] coeffs = wavelet.discretize(size);
            assertNotNull(coeffs);
            assertEquals(size, coeffs.length);
            
            // All coefficients should be finite
            for (double coeff : coeffs) {
                assertTrue(Double.isFinite(coeff));
            }
        }
    }

    @Test
    @DisplayName("Test inheritance from Wavelet interface")
    void testWaveletInheritance() {
        ContinuousWavelet wavelet = new TestContinuousWavelet(1.0, 1.0, false);
        
        // Test inherited methods
        assertNotNull(wavelet.name());
        assertEquals("TestContinuous", wavelet.name());
        
        // Test description method (inherited default)
        assertNotNull(wavelet.description());
        assertTrue(wavelet.description().contains("TestContinuous"));
        
        assertEquals(WaveletType.CONTINUOUS, wavelet.getType());
    }
}