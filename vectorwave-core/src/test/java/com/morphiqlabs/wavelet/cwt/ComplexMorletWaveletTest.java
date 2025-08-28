package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ComplexContinuousWavelet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for ComplexMorletWavelet.
 */
class ComplexMorletWaveletTest {

    private static final double TOLERANCE = 1e-12;

    @Test
    void testConstructor() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        assertNotNull(wavelet);
    }

    @Test
    void testConstructorWithInvalidBandwidth() {
        assertThrows(IllegalArgumentException.class, () -> 
            new ComplexMorletWavelet(0.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> 
            new ComplexMorletWavelet(-1.0, 1.0));
    }

    @Test
    void testConstructorWithInvalidCenterFrequency() {
        assertThrows(IllegalArgumentException.class, () -> 
            new ComplexMorletWavelet(1.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> 
            new ComplexMorletWavelet(1.0, -1.0));
    }

    @Test
    void testGetBandwidth() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(2.5, 1.0);
        assertEquals(2.5, wavelet.bandwidth(), TOLERANCE);
    }

    @Test
    void testGetCenterFrequency() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 3.5);
        assertEquals(3.5, wavelet.centerFrequency(), TOLERANCE);
    }

    @Test
    void testGetName() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        assertEquals("Complex Morlet", wavelet.name());
    }

    @Test
    void testValueAtZero() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        ComplexNumber value = wavelet.psiComplex(0.0);
        
        // At t=0, the Morlet wavelet should be real and positive
        assertTrue(value.real() > 0);
        assertEquals(0.0, value.imag(), TOLERANCE);
    }

    @Test
    void testValueSymmetry() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        
        // The Gaussian envelope should be symmetric
        double t = 1.0;
        ComplexNumber value1 = wavelet.psiComplex(t);
        ComplexNumber value2 = wavelet.psiComplex(-t);
        
        // Magnitudes should be equal due to Gaussian symmetry
        assertEquals(value1.magnitude(), value2.magnitude(), TOLERANCE);
    }

    @Test
    void testValueDecayWithDistance() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        
        ComplexNumber center = wavelet.psiComplex(0.0);
        ComplexNumber far = wavelet.psiComplex(5.0);
        
        // Value should decay with distance from center
        assertTrue(center.magnitude() > far.magnitude());
    }

    @Test
    void testDifferentBandwidthsAffectDecay() {
        ComplexMorletWavelet narrow = new ComplexMorletWavelet(0.5, 1.0);
        ComplexMorletWavelet wide = new ComplexMorletWavelet(2.0, 1.0);
        
        double t = 2.0;
        ComplexNumber narrowValue = narrow.psiComplex(t);
        ComplexNumber wideValue = wide.psiComplex(t);
        
        // Narrower bandwidth should decay faster
        assertTrue(narrowValue.magnitude() < wideValue.magnitude());
    }

    @Test
    void testCenterFrequencyAffectsOscillation() {
        ComplexMorletWavelet lowFreq = new ComplexMorletWavelet(1.0, 0.5);
        ComplexMorletWavelet highFreq = new ComplexMorletWavelet(1.0, 2.0);
        
        double t = 1.0;
        
        // Both should have similar magnitude (same bandwidth)
        double lowMag = lowFreq.psiComplex(t).magnitude();
        double highMag = highFreq.psiComplex(t).magnitude();
        assertEquals(lowMag, highMag, 0.1); // Allow some tolerance
        
        // But different phases due to different center frequencies
        double lowPhase = lowFreq.psiComplex(t).phase();
        double highPhase = highFreq.psiComplex(t).phase();
        assertNotEquals(lowPhase, highPhase, TOLERANCE);
    }

    @Test
    void testDiscretization() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        
        // Test various discretization sizes - discretize returns double[], not ComplexNumber[]
        double[] discrete8 = wavelet.discretize(8);
        assertEquals(8, discrete8.length);
        
        double[] discrete16 = wavelet.discretize(16);
        assertEquals(16, discrete16.length);
        
        double[] discrete32 = wavelet.discretize(32);
        assertEquals(32, discrete32.length);
    }

    @Test
    void testDiscretizationInvalidSize() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        
        assertThrows(IllegalArgumentException.class, () -> wavelet.discretize(0));
        assertThrows(IllegalArgumentException.class, () -> wavelet.discretize(-1));
    }

    @Test
    void testDiscretizationSymmetry() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        double[] discrete = wavelet.discretize(16);
        
        // Check that values are somewhat symmetric around the center
        // Note: complex Morlet may not be perfectly symmetric due to oscillations
        int center = discrete.length / 2;
        double sumLeft = 0, sumRight = 0;
        for (int i = 0; i < center; i++) {
            sumLeft += Math.abs(discrete[i]);
            sumRight += Math.abs(discrete[discrete.length - 1 - i]);
        }
        // Total energy should be roughly balanced
        assertEquals(sumLeft, sumRight, 0.5); // Allow more tolerance for asymmetry
    }

    @Test
    void testGetType() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        // getType() doesn't exist, use name() instead
        assertEquals("Complex Morlet", wavelet.name());
    }

    @Test
    void testImplementsInterface() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        assertTrue(wavelet instanceof ComplexContinuousWavelet);
    }

    @Test
    void testMathematicalProperties() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        
        // Test that the wavelet has the expected frequency content
        // At t=0, the phase should be 0 (real part is maximum)
        ComplexNumber atZero = wavelet.psiComplex(0.0);
        assertTrue(atZero.real() > 0);
        assertEquals(0.0, atZero.imag(), TOLERANCE);
        
        // Test that the wavelet has proper scaling with different parameters
        ComplexMorletWavelet scaled = new ComplexMorletWavelet(2.0, 1.0);
        ComplexNumber scaledAtZero = scaled.psiComplex(0.0);
        
        // Different bandwidth should affect the normalization
        assertNotEquals(atZero.magnitude(), scaledAtZero.magnitude(), TOLERANCE);
    }

    @Test
    void testExtremeParameterValues() {
        // Test with very small bandwidth
        ComplexMorletWavelet veryNarrow = new ComplexMorletWavelet(0.1, 1.0);
        assertNotNull(veryNarrow.psiComplex(0.0));
        
        // Test with very large bandwidth
        ComplexMorletWavelet veryWide = new ComplexMorletWavelet(10.0, 1.0);
        assertNotNull(veryWide.psiComplex(0.0));
        
        // Test with very high frequency
        ComplexMorletWavelet highFreq = new ComplexMorletWavelet(1.0, 100.0);
        assertNotNull(highFreq.psiComplex(0.0));
    }

    @Test
    void testNumericalStability() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        
        // Test at points far from center where values should be very small
        ComplexNumber farValue = wavelet.psiComplex(10.0);
        assertTrue(farValue.magnitude() < 1e-10);
        assertFalse(Double.isNaN(farValue.real()));
        assertFalse(Double.isNaN(farValue.imag()));
        assertFalse(Double.isInfinite(farValue.real()));
        assertFalse(Double.isInfinite(farValue.imag()));
    }
}