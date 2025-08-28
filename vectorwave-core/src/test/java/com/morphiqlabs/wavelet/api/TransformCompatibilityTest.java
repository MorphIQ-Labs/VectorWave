package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Transform Compatibility API in WaveletRegistry.
 */
class TransformCompatibilityTest {
    
    @Test
    @DisplayName("Orthogonal wavelets should support MODWT and SWT")
    void testOrthogonalWaveletCompatibility() {
        // Test with DB4 (orthogonal)
        Set<TransformType> supported = WaveletRegistry.getSupportedTransforms(WaveletName.DB4);
        
        assertTrue(supported.contains(TransformType.MODWT), 
            "DB4 should support MODWT");
        assertTrue(supported.contains(TransformType.SWT), 
            "DB4 should support SWT");
        assertTrue(supported.contains(TransformType.MULTI_LEVEL_MODWT),
            "DB4 should support Multi-level MODWT");
        assertTrue(supported.contains(TransformType.STREAMING_MODWT),
            "DB4 should support Streaming MODWT");
        assertFalse(supported.contains(TransformType.CWT),
            "DB4 should not support CWT");
    }
    
    @Test
    @DisplayName("Continuous wavelets should support CWT only")
    void testContinuousWaveletCompatibility() {
        // Test with Morlet (continuous)
        Set<TransformType> supported = WaveletRegistry.getSupportedTransforms(WaveletName.MORLET);
        
        assertTrue(supported.contains(TransformType.CWT),
            "Morlet should support CWT");
        assertFalse(supported.contains(TransformType.MODWT),
            "Morlet should not support MODWT");
        assertFalse(supported.contains(TransformType.SWT),
            "Morlet should not support SWT");
    }
    
    @Test
    @DisplayName("isCompatible should correctly check wavelet-transform pairs")
    void testIsCompatible() {
        // Orthogonal with MODWT - should be compatible
        assertTrue(WaveletRegistry.isCompatible(WaveletName.HAAR, TransformType.MODWT));
        assertTrue(WaveletRegistry.isCompatible(WaveletName.DB4, TransformType.MODWT));
        assertTrue(WaveletRegistry.isCompatible(WaveletName.SYM8, TransformType.SWT));
        
        // Continuous with CWT - should be compatible
        assertTrue(WaveletRegistry.isCompatible(WaveletName.MORLET, TransformType.CWT));
        assertTrue(WaveletRegistry.isCompatible(WaveletName.MEXICAN_HAT, TransformType.CWT));
        
        // Orthogonal with CWT - should NOT be compatible
        assertFalse(WaveletRegistry.isCompatible(WaveletName.DB4, TransformType.CWT));
        assertFalse(WaveletRegistry.isCompatible(WaveletName.HAAR, TransformType.CWT));
        
        // Continuous with MODWT - should NOT be compatible
        assertFalse(WaveletRegistry.isCompatible(WaveletName.MORLET, TransformType.MODWT));
        assertFalse(WaveletRegistry.isCompatible(WaveletName.MEXICAN_HAT, TransformType.SWT));
        
        // Null checks
        assertFalse(WaveletRegistry.isCompatible(null, TransformType.MODWT));
        assertFalse(WaveletRegistry.isCompatible(WaveletName.DB4, null));
        assertFalse(WaveletRegistry.isCompatible(null, null));
    }
    
    @Test
    @DisplayName("getWaveletsForTransform should return correct wavelets")
    void testGetWaveletsForTransform() {
        // Get wavelets for MODWT
        List<WaveletName> modwtWavelets = WaveletRegistry.getWaveletsForTransform(TransformType.MODWT);
        assertFalse(modwtWavelets.isEmpty(), "MODWT should have compatible wavelets");
        assertTrue(modwtWavelets.contains(WaveletName.DB4), "MODWT wavelets should include DB4");
        assertTrue(modwtWavelets.contains(WaveletName.HAAR), "MODWT wavelets should include HAAR");
        assertFalse(modwtWavelets.contains(WaveletName.MORLET), "MODWT wavelets should not include Morlet");
        
        // Get wavelets for CWT
        List<WaveletName> cwtWavelets = WaveletRegistry.getWaveletsForTransform(TransformType.CWT);
        assertFalse(cwtWavelets.isEmpty(), "CWT should have compatible wavelets");
        assertTrue(cwtWavelets.contains(WaveletName.MORLET), "CWT wavelets should include Morlet");
        assertFalse(cwtWavelets.contains(WaveletName.DB4), "CWT wavelets should not include DB4");
        
        // Null check
        List<WaveletName> nullResult = WaveletRegistry.getWaveletsForTransform(null);
        assertTrue(nullResult.isEmpty(), "Null transform should return empty list");
    }
    
    @Test
    @DisplayName("getRecommendedTransform should provide sensible recommendations")
    void testGetRecommendedTransform() {
        // Orthogonal wavelets should recommend MODWT
        assertEquals(TransformType.MODWT, 
            WaveletRegistry.getRecommendedTransform(WaveletName.DB4),
            "DB4 should recommend MODWT");
        assertEquals(TransformType.MODWT,
            WaveletRegistry.getRecommendedTransform(WaveletName.HAAR),
            "HAAR should recommend MODWT");
        
        // Continuous wavelets should recommend CWT
        assertEquals(TransformType.CWT,
            WaveletRegistry.getRecommendedTransform(WaveletName.MORLET),
            "Morlet should recommend CWT");
        assertEquals(TransformType.CWT,
            WaveletRegistry.getRecommendedTransform(WaveletName.MEXICAN_HAT),
            "Mexican Hat should recommend CWT");
        
        // Complex wavelets should recommend CWT
        assertEquals(TransformType.CWT,
            WaveletRegistry.getRecommendedTransform(WaveletName.CMOR),
            "Complex Morlet should recommend CWT");
        
        // Null check
        assertNull(WaveletRegistry.getRecommendedTransform(null),
            "Null wavelet should return null");
    }
    
    @Test
    @DisplayName("getSupportedTransforms should handle null input")
    void testGetSupportedTransformsWithNull() {
        Set<TransformType> result = WaveletRegistry.getSupportedTransforms(null);
        assertNotNull(result, "Should return non-null set");
        assertTrue(result.isEmpty(), "Should return empty set for null input");
    }
    
    @Test
    @DisplayName("All orthogonal wavelets should support the same transforms")
    void testAllOrthogonalWaveletsConsistency() {
        List<WaveletName> orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
        
        // Get expected transforms from first orthogonal wavelet
        Set<TransformType> expectedTransforms = WaveletRegistry.getSupportedTransforms(orthogonalWavelets.get(0));
        
        // Verify all orthogonal wavelets support the same transforms
        for (WaveletName wavelet : orthogonalWavelets) {
            Set<TransformType> supported = WaveletRegistry.getSupportedTransforms(wavelet);
            assertEquals(expectedTransforms, supported,
                wavelet + " should support same transforms as other orthogonal wavelets");
        }
    }
}