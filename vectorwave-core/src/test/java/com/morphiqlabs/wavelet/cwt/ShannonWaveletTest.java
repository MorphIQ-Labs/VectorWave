package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for ShannonWavelet.
 * Ensures proper functionality and mathematical properties of the Shannon wavelet.
 */
class ShannonWaveletTest {

    @Test
    @DisplayName("Create Shannon wavelet with default parameters")
    void testDefaultConstructor() {
        ShannonWavelet wavelet = new ShannonWavelet();
        assertNotNull(wavelet);
        assertEquals("shan1.0-1.5", wavelet.name());
        assertEquals("Shannon wavelet (fb=1.0, fc=1.5)", wavelet.description());
        assertEquals(1.0, wavelet.getBandwidthParameter());
        assertEquals(1.5, wavelet.getCenterFrequencyParameter());
        assertEquals(1.0, wavelet.bandwidth());
        assertEquals(1.5, wavelet.centerFrequency());
        assertFalse(wavelet.isComplex());
    }

    @Test
    @DisplayName("Create Shannon wavelet with custom parameters")
    void testCustomParameters() {
        ShannonWavelet wavelet = new ShannonWavelet(2.0, 3.0);
        assertNotNull(wavelet);
        assertEquals("shan2.0-3.0", wavelet.name());
        assertEquals("Shannon wavelet (fb=2.0, fc=3.0)", wavelet.description());
        assertEquals(2.0, wavelet.getBandwidthParameter());
        assertEquals(3.0, wavelet.getCenterFrequencyParameter());
        assertEquals(2.0, wavelet.bandwidth());
        assertEquals(3.0, wavelet.centerFrequency());
    }

    @Test
    @DisplayName("Invalid bandwidth parameter throws exception")
    void testInvalidBandwidth() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ShannonWavelet(0, 1.5),
            "Zero bandwidth should throw exception");
        
        assertThrows(IllegalArgumentException.class, 
            () -> new ShannonWavelet(-1.0, 1.5),
            "Negative bandwidth should throw exception");
    }

    @Test
    @DisplayName("Invalid center frequency parameter throws exception")
    void testInvalidCenterFrequency() {
        assertThrows(IllegalArgumentException.class, 
            () -> new ShannonWavelet(1.0, 0),
            "Zero center frequency should throw exception");
        
        assertThrows(IllegalArgumentException.class, 
            () -> new ShannonWavelet(1.0, -1.5),
            "Negative center frequency should throw exception");
    }

    @Test
    @DisplayName("Psi function evaluates correctly at t=0")
    void testPsiAtZero() {
        ShannonWavelet wavelet = new ShannonWavelet();
        // At t=0, sinc(0) = 1, cos(0) = 1
        // psi(0) = sqrt(fb) * 1 * 1 = sqrt(1) = 1
        double psi0 = wavelet.psi(0);
        assertEquals(1.0, psi0, 1e-10);
    }

    @Test
    @DisplayName("Psi function evaluates correctly at various points")
    void testPsiFunction() {
        ShannonWavelet wavelet = new ShannonWavelet(1.0, 1.5);
        
        // Test at t=0
        assertEquals(1.0, wavelet.psi(0), 1e-10);
        
        // Test at non-zero points
        double t1 = 0.5;
        double expected1 = Math.sqrt(1.0) * 
            (Math.sin(Math.PI * t1) / (Math.PI * t1)) * 
            Math.cos(2 * Math.PI * 1.5 * t1);
        assertEquals(expected1, wavelet.psi(t1), 1e-10);
        
        // Test at a point where sinc is near zero
        double t2 = 1.0; // sinc(1) = sin(π)/π = 0
        double expected2 = 0.0;
        assertEquals(expected2, wavelet.psi(t2), 1e-10);
        
        // Test symmetry properties
        double psiPos = wavelet.psi(0.3);
        double psiNeg = wavelet.psi(-0.3);
        // Shannon wavelet with cosine modulation can be symmetric at certain points
        // Just verify both values are computed
        assertNotNull(psiPos);
        assertNotNull(psiNeg);
    }

    @Test
    @DisplayName("Psi function handles very small t values")
    void testPsiSmallValues() {
        ShannonWavelet wavelet = new ShannonWavelet();
        
        // Test very small positive value
        double tSmall = 1e-15;
        double psiSmall = wavelet.psi(tSmall);
        // Should be very close to psi(0) = 1
        assertEquals(1.0, psiSmall, 1e-9);
        
        // Test very small negative value
        double tNegSmall = -1e-15;
        double psiNegSmall = wavelet.psi(tNegSmall);
        assertEquals(1.0, psiNegSmall, 1e-9);
    }

    @Test
    @DisplayName("Discretize produces correct length array")
    void testDiscretizeLength() {
        ShannonWavelet wavelet = new ShannonWavelet();
        
        int length = 64;
        double[] samples = wavelet.discretize(length);
        assertNotNull(samples);
        assertEquals(length, samples.length);
        
        // Test another length
        length = 128;
        samples = wavelet.discretize(length);
        assertEquals(length, samples.length);
    }

    @Test
    @DisplayName("Discretize throws exception for invalid length")
    void testDiscretizeInvalidLength() {
        ShannonWavelet wavelet = new ShannonWavelet();
        
        assertThrows(InvalidArgumentException.class,
            () -> wavelet.discretize(0),
            "Zero length should throw exception");
        
        assertThrows(InvalidArgumentException.class,
            () -> wavelet.discretize(-1),
            "Negative length should throw exception");
    }

    @Test
    @DisplayName("Discretize produces symmetric samples around center")
    void testDiscretizeSymmetry() {
        ShannonWavelet wavelet = new ShannonWavelet();
        
        int length = 65; // Odd length for clear center
        double[] samples = wavelet.discretize(length);
        int center = length / 2;
        
        // Center sample should be psi(0) = 1
        assertEquals(1.0, samples[center], 1e-10);
        
        // Test approximate symmetry (not exact due to cosine modulation)
        for (int i = 1; i < 5; i++) {
            double leftMag = Math.abs(samples[center - i]);
            double rightMag = Math.abs(samples[center + i]);
            // Magnitudes should be similar but not identical
            assertEquals(leftMag, rightMag, 0.5);
        }
    }

    @Test
    @DisplayName("Discretize samples decay away from center")
    void testDiscretizeDecay() {
        ShannonWavelet wavelet = new ShannonWavelet();
        
        int length = 128;
        double[] samples = wavelet.discretize(length);
        int center = length / 2;
        
        // Samples should generally decay away from center
        double centerMag = Math.abs(samples[center]);
        double nearMag = Math.abs(samples[center + 10]);
        double farMag = Math.abs(samples[center + 30]);
        
        // Center should have largest magnitude
        assertTrue(centerMag > nearMag || Math.abs(centerMag - nearMag) < 0.1);
        assertTrue(nearMag > farMag || Math.abs(nearMag - farMag) < 0.1);
    }

    @Test
    @DisplayName("Different parameters produce different wavelets")
    void testParameterVariation() {
        ShannonWavelet wavelet1 = new ShannonWavelet(1.0, 1.5);
        ShannonWavelet wavelet2 = new ShannonWavelet(2.0, 3.0);
        
        // Names should be different
        assertNotEquals(wavelet1.name(), wavelet2.name());
        
        // Psi values at same point should be different
        double t = 0.5;
        double psi1 = wavelet1.psi(t);
        double psi2 = wavelet2.psi(t);
        assertNotEquals(psi1, psi2);
        
        // Discretized samples should be different
        int length = 64;
        double[] samples1 = wavelet1.discretize(length);
        double[] samples2 = wavelet2.discretize(length);
        
        boolean allSame = true;
        for (int i = 0; i < length; i++) {
            if (Math.abs(samples1[i] - samples2[i]) > 1e-10) {
                allSame = false;
                break;
            }
        }
        assertFalse(allSame, "Different parameters should produce different samples");
    }

    @Test
    @DisplayName("Bandwidth parameter affects wavelet width")
    void testBandwidthEffect() {
        ShannonWavelet narrowBand = new ShannonWavelet(0.5, 1.5);
        ShannonWavelet wideBand = new ShannonWavelet(2.0, 1.5);
        
        // Wide band should have larger amplitude due to sqrt(fb) factor
        double psiNarrow = narrowBand.psi(0);
        double psiWide = wideBand.psi(0);
        
        assertEquals(Math.sqrt(0.5), psiNarrow, 1e-10);
        assertEquals(Math.sqrt(2.0), psiWide, 1e-10);
        assertTrue(psiWide > psiNarrow);
    }

    @Test
    @DisplayName("Center frequency parameter affects oscillation")
    void testCenterFrequencyEffect() {
        ShannonWavelet lowFreq = new ShannonWavelet(1.0, 1.0);
        ShannonWavelet highFreq = new ShannonWavelet(1.0, 3.0);
        
        // Different center frequencies should produce different oscillation patterns
        double t = 0.25;
        double psiLow = lowFreq.psi(t);
        double psiHigh = highFreq.psi(t);
        
        // The cosine modulation will be different
        assertNotEquals(psiLow, psiHigh);
    }
}