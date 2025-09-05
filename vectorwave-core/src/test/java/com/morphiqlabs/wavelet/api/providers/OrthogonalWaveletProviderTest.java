package com.morphiqlabs.wavelet.api.providers;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for OrthogonalWaveletProvider.
 */
class OrthogonalWaveletProviderTest {
    
    private OrthogonalWaveletProvider provider;
    
    @BeforeEach
    void setUp() {
        provider = new OrthogonalWaveletProvider();
    }
    
    @Test
    void testGetWavelets() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        assertNotNull(wavelets);
        assertFalse(wavelets.isEmpty());
        assertEquals(9, wavelets.size()); // Haar + 2 DB + 3 SYM + 3 COIF
    }
    
    @Test
    void testWaveletTypes() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        // All should be orthogonal wavelets
        for (Wavelet wavelet : wavelets) {
            assertEquals(WaveletType.ORTHOGONAL, wavelet.getType());
            assertTrue(wavelet instanceof OrthogonalWavelet);
        }
    }
    
    @Test
    void testWaveletNames() {
        List<Wavelet> wavelets = provider.getWavelets();
        Set<String> expectedNames = Set.of(
            "Haar", "db2", "db4", "sym2", "sym3", "sym4", 
            "coif1", "coif2", "coif3"
        );
        
        Set<String> actualNames = new HashSet<>();
        for (Wavelet wavelet : wavelets) {
            actualNames.add(wavelet.name());
        }
        
        assertEquals(expectedNames, actualNames);
    }
    
    @Test
    void testWaveletProperties() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        for (Wavelet wavelet : wavelets) {
            // Test basic properties
            assertNotNull(wavelet.name());
            assertNotNull(wavelet.description());
            assertNotNull(wavelet.getType());
            
            // Test filter coefficients
            assertNotNull(wavelet.lowPassDecomposition());
            assertNotNull(wavelet.highPassDecomposition());
            assertNotNull(wavelet.lowPassReconstruction());
            assertNotNull(wavelet.highPassReconstruction());
            
            // Filters should not be empty
            assertTrue(wavelet.lowPassDecomposition().length > 0);
            assertTrue(wavelet.highPassDecomposition().length > 0);
            
            // For orthogonal wavelets, reconstruction = decomposition
            assertArrayEquals(wavelet.lowPassDecomposition(), 
                            wavelet.lowPassReconstruction());
            assertArrayEquals(wavelet.highPassDecomposition(), 
                            wavelet.highPassReconstruction());
        }
    }
    
    @Test
    void testSpecificWavelets() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        // Find specific wavelets and test their properties
        Wavelet haar = wavelets.stream()
            .filter(w -> w.name().equals("Haar"))
            .findFirst()
            .orElseThrow();
        
        assertTrue(haar instanceof Haar);
        assertEquals(2, haar.lowPassDecomposition().length);
        
        // Test Daubechies
        Wavelet db4 = wavelets.stream()
            .filter(w -> w.name().equals("db4"))
            .findFirst()
            .orElseThrow();
        
        assertTrue(db4 instanceof Daubechies);
        assertEquals(8, db4.lowPassDecomposition().length); // DB4 has 8 coefficients
    }
    
    @Test
    void testListImmutability() {
        List<Wavelet> wavelets1 = provider.getWavelets();
        List<Wavelet> wavelets2 = provider.getWavelets();
        
        // Should return the same immutable list
        assertThrows(UnsupportedOperationException.class, () -> {
            wavelets1.add(new Haar());
        });
    }
    
    @Test
    void testVanishingMoments() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        for (Wavelet wavelet : wavelets) {
            if (wavelet instanceof OrthogonalWavelet ow) {
                assertTrue(ow.vanishingMoments() > 0);
                
                // Check specific vanishing moments
                if (wavelet.name().equals("Haar")) {
                    assertEquals(1, ow.vanishingMoments());
                } else if (wavelet.name().equals("db2")) {
                    assertEquals(2, ow.vanishingMoments());
                } else if (wavelet.name().equals("db4")) {
                    assertEquals(4, ow.vanishingMoments());
                }
            }
        }
    }
    
    @Test
    void testFilterNormalization() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        for (Wavelet wavelet : wavelets) {
            double[] lowPass = wavelet.lowPassDecomposition();
            double[] highPass = wavelet.highPassDecomposition();
            
            // Check L2 normalization
            double lowPassNorm = 0;
            double highPassNorm = 0;
            
            for (double coeff : lowPass) {
                lowPassNorm += coeff * coeff;
            }
            for (double coeff : highPass) {
                highPassNorm += coeff * coeff;
            }
            
            // Should be normalized to 1 (within tolerance)
            assertEquals(1.0, lowPassNorm, 1e-4, 
                "Low-pass filter for " + wavelet.name() + " not normalized");
            assertEquals(1.0, highPassNorm, 1e-4,
                "High-pass filter for " + wavelet.name() + " not normalized");
        }
    }
}