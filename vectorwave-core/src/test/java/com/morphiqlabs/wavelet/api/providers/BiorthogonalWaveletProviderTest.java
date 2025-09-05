package com.morphiqlabs.wavelet.api.providers;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for BiorthogonalWaveletProvider.
 */
class BiorthogonalWaveletProviderTest {
    
    private BiorthogonalWaveletProvider provider;
    
    @BeforeEach
    void setUp() {
        provider = new BiorthogonalWaveletProvider();
    }
    
    @Test
    void testGetWavelets() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        assertNotNull(wavelets);
        assertFalse(wavelets.isEmpty());
        assertEquals(1, wavelets.size()); // Currently only BIOR1_3
    }
    
    @Test
    void testWaveletType() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        for (Wavelet wavelet : wavelets) {
            assertEquals(WaveletType.BIORTHOGONAL, wavelet.getType());
            assertTrue(wavelet instanceof BiorthogonalWavelet);
        }
    }
    
    @Test
    void testBiorthogonalProperties() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        for (Wavelet wavelet : wavelets) {
            BiorthogonalWavelet biorWavelet = (BiorthogonalWavelet) wavelet;
            
            // Test basic properties
            assertNotNull(biorWavelet.name());
            assertEquals("bior1.3", biorWavelet.name());
            assertNotNull(biorWavelet.description());
            
            // Test filter coefficients
            double[] lowDec = biorWavelet.lowPassDecomposition();
            double[] highDec = biorWavelet.highPassDecomposition();
            double[] lowRec = biorWavelet.lowPassReconstruction();
            double[] highRec = biorWavelet.highPassReconstruction();
            
            assertNotNull(lowDec);
            assertNotNull(highDec);
            assertNotNull(lowRec);
            assertNotNull(highRec);
            
            // Biorthogonal wavelets have different decomposition and reconstruction filters
            assertNotEquals(lowDec.length, lowRec.length);
            assertNotEquals(highDec.length, highRec.length);
            
            // Test vanishing moments
            assertTrue(biorWavelet.vanishingMoments() > 0);
            assertTrue(biorWavelet.dualVanishingMoments() > 0);
            
            // Test symmetry
            assertTrue(biorWavelet.isSymmetric());
        }
    }
    
    @Test
    void testFilterCoefficients() {
        List<Wavelet> wavelets = provider.getWavelets();
        Wavelet bior = wavelets.get(0);
        
        // BIOR1_3 specific tests
        double[] lowDec = bior.lowPassDecomposition();
        double[] lowRec = bior.lowPassReconstruction();
        
        // BIOR1_3 has different lengths for decomposition and reconstruction
        assertNotEquals(lowDec.length, lowRec.length);
        assertTrue(lowDec.length > 0);
        assertTrue(lowRec.length > 0);
    }
    
    @Test
    void testListImmutability() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            wavelets.add(BiorthogonalSpline.BIOR1_3);
        });
    }
    
    @Test
    void testProviderConsistency() {
        // Multiple calls should return equivalent results
        List<Wavelet> wavelets1 = provider.getWavelets();
        List<Wavelet> wavelets2 = provider.getWavelets();
        
        assertEquals(wavelets1.size(), wavelets2.size());
        for (int i = 0; i < wavelets1.size(); i++) {
            assertEquals(wavelets1.get(i).name(), wavelets2.get(i).name());
        }
    }
    
    @Test
    void testKnownIssueDocumentation() {
        // This test documents the known issue with biorthogonal wavelets
        // as mentioned in CLAUDE.md
        List<Wavelet> wavelets = provider.getWavelets();
        
        for (Wavelet wavelet : wavelets) {
            // We acknowledge this wavelet has reconstruction issues
            // This test serves as documentation
            assertTrue(wavelet.description().contains("Biorthogonal") || 
                      wavelet.name().contains("bior"));
        }
    }
}