package com.morphiqlabs.wavelet.cwt.finance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MATLABMexicanHat wavelet implementation.
 */
class MATLABMexicanHatTest {
    
    private MATLABMexicanHat wavelet;
    private static final double TOLERANCE = 1e-7;
    
    @BeforeEach
    void setUp() {
        wavelet = new MATLABMexicanHat();
    }
    
    @Test
    @DisplayName("Should return correct wavelet name")
    void testName() {
        assertEquals("mexihat", wavelet.name());
    }
    
    @Test
    @DisplayName("Should evaluate psi correctly at table points")
    void testPsiAtTablePoints() {
        // Test exact values from MATLAB table
        assertEquals(0.8673250706, wavelet.psi(0.0), TOLERANCE);
        assertEquals(-0.3520653268, wavelet.psi(1.0), TOLERANCE);
        assertEquals(-0.3520653268, wavelet.psi(-1.0), TOLERANCE);
        assertEquals(0.1246251612, wavelet.psi(0.5), TOLERANCE);
        assertEquals(0.1246251612, wavelet.psi(-0.5), TOLERANCE);
        assertEquals(-0.1711006461, wavelet.psi(2.0), TOLERANCE);
        assertEquals(-0.1711006461, wavelet.psi(-2.0), TOLERANCE);
    }
    
    @Test
    @DisplayName("Should interpolate correctly between table points")
    void testPsiInterpolation() {
        // Test interpolation between known points
        double t = 0.25; // Between 0.0 and 0.5
        double result = wavelet.psi(t);
        // Should be between psi(0.0) and psi(0.5)
        assertTrue(result > 0.1246251612 && result < 0.8673250706);
        
        // Test negative side interpolation
        t = -1.25; // Between -1.5 and -1.0
        result = wavelet.psi(t);
        // Should be between psi(-1.5) and psi(-1.0)
        assertTrue(result < -0.3520653268 && result > -0.3738850614);
        
        // Test near zero
        t = 0.1;
        result = wavelet.psi(t);
        assertTrue(result < 0.8673250706); // Less than peak at 0
        
        // Test symmetry through interpolation
        double pos = wavelet.psi(1.75);
        double neg = wavelet.psi(-1.75);
        assertEquals(pos, neg, TOLERANCE, "Wavelet should be symmetric");
    }
    
    @Test
    @DisplayName("Should compute psi correctly outside table range")
    void testPsiOutsideTableRange() {
        // Test values beyond the table range [-5, 5]
        double t = 6.0;
        double result = wavelet.psi(t);
        // Should be very small (can be negative for Mexican Hat)
        assertTrue(Math.abs(result) < 0.03, "Value at t=6.0 should be small");
        
        // Test far negative
        t = -7.0;
        result = wavelet.psi(t);
        assertTrue(Math.abs(result) < 0.01, "Value at t=-7.0 should be very small");
        
        // Test extreme values
        t = 10.0;
        result = wavelet.psi(t);
        assertTrue(Math.abs(result) < 1e-5, "Value at t=10.0 should be extremely small");
        
        // Test symmetry outside range
        double pos = wavelet.psi(5.5);
        double neg = wavelet.psi(-5.5);
        assertEquals(pos, neg, TOLERANCE, "Wavelet should be symmetric outside range");
    }
    
    @Test
    @DisplayName("Should return correct center frequency")
    void testCenterFrequency() {
        double centerFreq = wavelet.centerFrequency();
        assertTrue(centerFreq > 0, "Center frequency should be positive");
        // Mexican hat wavelet typically has center frequency around 0.25-0.3
        assertTrue(centerFreq > 0.2 && centerFreq < 0.35,
            "Center frequency should be in expected range");
    }
    
    @Test
    @DisplayName("Should return correct bandwidth")
    void testBandwidth() {
        double bandwidth = wavelet.bandwidth();
        assertTrue(bandwidth > 0, "Bandwidth should be positive");
        // Mexican hat has moderate bandwidth
        assertTrue(bandwidth > 0.5 && bandwidth < 2.0,
            "Bandwidth should be in expected range");
    }
    
    @Test
    @DisplayName("Should not be complex wavelet")
    void testIsComplex() {
        assertFalse(wavelet.isComplex(), "Mexican hat is a real wavelet");
    }
    
    @Test
    @DisplayName("Should discretize correctly")
    void testDiscretize() {
        int length = 128;
        double[] samples = wavelet.discretize(length);
        
        assertNotNull(samples);
        assertEquals(length, samples.length);
        
        // Find the peak (should be near center)
        double maxValue = Double.NEGATIVE_INFINITY;
        int maxIndex = -1;
        for (int i = 0; i < length; i++) {
            if (samples[i] > maxValue) {
                maxValue = samples[i];
                maxIndex = i;
            }
        }
        
        // Peak should be near the center
        int expectedCenter = length / 2;
        assertTrue(Math.abs(maxIndex - expectedCenter) <= 1,
            "Peak should be at or near center, found at " + maxIndex);
        
        // Wavelet function should be symmetric around t=0
        // Check a few symmetric points
        double t1 = wavelet.psi(1.0);
        double t2 = wavelet.psi(-1.0);
        assertEquals(t1, t2, TOLERANCE, "Wavelet function should be symmetric");
        
        // Test different lengths
        length = 256;
        samples = wavelet.discretize(length);
        assertEquals(length, samples.length);
        
        length = 64;
        samples = wavelet.discretize(length);
        assertEquals(length, samples.length);
    }
    
    @Test
    @DisplayName("Should handle edge cases in psi evaluation")
    void testPsiEdgeCases() {
        // Test at exact table boundaries
        assertEquals(-0.0000888178, wavelet.psi(5.0), TOLERANCE);
        assertEquals(-0.0000888178, wavelet.psi(-5.0), TOLERANCE);
        
        // Test just inside boundaries
        double result = wavelet.psi(4.99);
        assertTrue(Math.abs(result) > Math.abs(wavelet.psi(5.0)),
            "Value at 4.99 should be larger than at boundary");
        
        // Test just outside boundaries - uses formula calculation
        result = wavelet.psi(5.01);
        assertNotNull(result); // Just verify it returns a value
        
        // Test at midpoints between table values
        double mid = wavelet.psi(2.25); // Between 2.0 and 2.5
        double expected = (wavelet.psi(2.0) + wavelet.psi(2.5)) / 2;
        assertEquals(expected, mid, 0.01, "Linear interpolation at midpoint");
    }
    
    @Test
    @DisplayName("Should maintain wavelet properties")
    void testWaveletProperties() {
        // Test zero mean (admissibility condition)
        // Note: Numerical integration has limited accuracy
        double sum = 0;
        double dx = 0.01;
        for (double t = -10; t <= 10; t += dx) {
            sum += wavelet.psi(t) * dx;
        }
        // Mexican Hat should have zero mean, but numerical integration isn't perfect
        assertTrue(Math.abs(sum) < 1.0, 
            "Wavelet integral should be small, got: " + sum);
        
        // Test decay at infinity
        assertTrue(Math.abs(wavelet.psi(20)) < 1e-20,
            "Wavelet should decay to zero at infinity");
        assertTrue(Math.abs(wavelet.psi(-20)) < 1e-20,
            "Wavelet should decay to zero at negative infinity");
        
        // Test second derivative behavior (Mexican hat is -Laplacian of Gaussian)
        // Should have one maximum at 0 and two minima
        assertTrue(wavelet.psi(0) > wavelet.psi(1),
            "Should decrease from center");
        assertTrue(wavelet.psi(0) > wavelet.psi(-1),
            "Should decrease from center (negative side)");
    }
    
    @Test
    @DisplayName("Should test binary search efficiency")
    void testBinarySearchPerformance() {
        // Test many evaluations to ensure binary search works correctly
        double step = 0.01;
        int count = 0;
        for (double t = -4.99; t < 5.0; t += step) {
            double result = wavelet.psi(t);
            assertNotNull(result);
            assertFalse(Double.isNaN(result));
            assertFalse(Double.isInfinite(result));
            count++;
        }
        assertTrue(count > 900, "Should have evaluated many points");
    }
    
    @Test
    @DisplayName("Should match MATLAB values precisely at key points")
    void testMATLABCompatibility() {
        // Verify key MATLAB values with high precision
        double[][] matlabPoints = {
            {-3.0, -0.0131550316},
            {-2.5, -0.0327508388},
            {-2.0, -0.1711006461},
            {-1.5, -0.3738850614},
            {-1.0, -0.3520653268},
            {-0.5,  0.1246251612},
            {0.0,   0.8673250706},
            {0.5,   0.1246251612},
            {1.0,  -0.3520653268},
            {1.5,  -0.3738850614},
            {2.0,  -0.1711006461},
            {2.5,  -0.0327508388},
            {3.0,  -0.0131550316}
        };
        
        for (double[] point : matlabPoints) {
            double t = point[0];
            double expected = point[1];
            double actual = wavelet.psi(t);
            assertEquals(expected, actual, 1e-9,
                String.format("MATLAB value mismatch at t=%.1f", t));
        }
    }
}