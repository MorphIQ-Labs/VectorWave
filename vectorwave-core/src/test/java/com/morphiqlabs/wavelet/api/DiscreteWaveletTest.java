package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DiscreteWavelet interface default methods using existing wavelet implementations.
 */
class DiscreteWaveletTest {

    private static final double EPSILON = 1e-10;

    @Test
    @DisplayName("Test supportWidth default method using Haar wavelet")
    void testSupportWidth() {
        DiscreteWavelet haar = new Haar();
        
        // Haar has 2 coefficients
        assertEquals(2, haar.supportWidth());
        assertEquals(haar.lowPassDecomposition().length, haar.supportWidth());
    }
    
    @Test
    @DisplayName("Test supportWidth with Daubechies wavelets")
    void testSupportWidthDaubechies() {
        // Test DB2 - has filter length 4
        DiscreteWavelet db2 = Daubechies.DB2;
        assertEquals(4, db2.supportWidth());
        
        // Test DB4 - has filter length 8
        DiscreteWavelet db4 = Daubechies.DB4;
        assertEquals(8, db4.supportWidth());
        
        // Test DB6 - has filter length 12
        DiscreteWavelet db6 = Daubechies.DB6;
        assertEquals(12, db6.supportWidth());
    }

    @Test
    @DisplayName("Test hasCompactSupport default method")
    void testHasCompactSupport() {
        // All discrete wavelets should have compact support by default
        DiscreteWavelet haar = new Haar();
        assertTrue(haar.hasCompactSupport());
        
        DiscreteWavelet db4 = Daubechies.DB4;
        assertTrue(db4.hasCompactSupport());
        
        DiscreteWavelet db6 = Daubechies.DB6;
        assertTrue(db6.hasCompactSupport());
    }

    @Test
    @DisplayName("Test vanishing moments with Daubechies wavelets")
    void testVanishingMoments() {
        // Daubechies wavelets have specific vanishing moments
        DiscreteWavelet db2 = Daubechies.DB2;
        assertEquals(2, db2.vanishingMoments());
        
        DiscreteWavelet db4 = Daubechies.DB4;
        assertEquals(4, db4.vanishingMoments());
        
        DiscreteWavelet db6 = Daubechies.DB6;
        assertEquals(6, db6.vanishingMoments());
    }
    
    @Test
    @DisplayName("Test interface inheritance from Wavelet")
    void testWaveletInheritance() {
        DiscreteWavelet wavelet = new Haar();
        
        // Test inherited methods
        assertNotNull(wavelet.name());
        assertEquals("Haar", wavelet.name());
        
        // Test type
        assertEquals(WaveletType.ORTHOGONAL, wavelet.getType());
        
        // Test description method (inherited default)
        assertNotNull(wavelet.description());
        assertTrue(wavelet.description().contains("Haar"));
    }

    @Test
    @DisplayName("Test consistency between supportWidth and lowPassDecomposition")
    void testSupportWidthConsistency() {
        DiscreteWavelet[] wavelets = {
            new Haar(),
            Daubechies.DB4,
            Daubechies.DB6,
            Daubechies.DB8
        };
        
        for (DiscreteWavelet wavelet : wavelets) {
            assertEquals(wavelet.lowPassDecomposition().length, wavelet.supportWidth(),
                "Support width should match low-pass decomposition length for " + wavelet.name());
        }
    }

    @Test
    @DisplayName("Test various discrete wavelet types")
    void testVariousDiscreteWavelets() {
        DiscreteWavelet[] wavelets = {
            new Haar(),
            Daubechies.DB4,
            Daubechies.DB6,
            Daubechies.DB8,
            Daubechies.DB10
        };
        
        for (DiscreteWavelet wavelet : wavelets) {
            // All should have compact support
            assertTrue(wavelet.hasCompactSupport());
            
            // Support width should be positive
            assertTrue(wavelet.supportWidth() > 0);
            
            // Vanishing moments should be non-negative
            assertTrue(wavelet.vanishingMoments() >= 0);
            
            // Should have a name
            assertNotNull(wavelet.name());
            assertFalse(wavelet.name().isEmpty());
        }
    }
}