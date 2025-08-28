package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to ensure all 17 Coiflet wavelets are properly registered in WaveletRegistry.
 */
class CoifletRegistryTest {

    @Test
    @DisplayName("Verify all 17 Coiflets are registered and accessible via WaveletRegistry")
    void testAllCoifletsInRegistry() {
        // Test all 17 Coiflet wavelets
        WaveletName[] coifletNames = {
            WaveletName.COIF1, WaveletName.COIF2, WaveletName.COIF3, WaveletName.COIF4,
            WaveletName.COIF5, WaveletName.COIF6, WaveletName.COIF7, WaveletName.COIF8,
            WaveletName.COIF9, WaveletName.COIF10, WaveletName.COIF11, WaveletName.COIF12,
            WaveletName.COIF13, WaveletName.COIF14, WaveletName.COIF15, WaveletName.COIF16,
            WaveletName.COIF17
        };
        
        for (int i = 0; i < coifletNames.length; i++) {
            WaveletName name = coifletNames[i];
            int order = i + 1;
            
            // Verify the wavelet can be retrieved from registry
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            assertNotNull(wavelet, "COIF" + order + " should be retrievable from registry");
            
            // Verify it's a Coiflet instance
            assertInstanceOf(Coiflet.class, wavelet, 
                "COIF" + order + " should be a Coiflet instance");
            
            // Cast to Coiflet and verify properties
            Coiflet coiflet = (Coiflet) wavelet;
            assertEquals("coif" + order, coiflet.name(),
                "COIF" + order + " should have correct name");
            assertEquals(6 * order, coiflet.lowPassDecomposition().length,
                "COIF" + order + " should have " + (6 * order) + " coefficients");
            assertEquals(2 * order, coiflet.vanishingMoments(),
                "COIF" + order + " should have " + (2 * order) + " vanishing moments");
            
            // Verify the WaveletName enum properties
            assertEquals("coif" + order, name.getCode(),
                "WaveletName.COIF" + order + " should have correct code");
            assertEquals("Coiflet " + order, name.getDescription(),
                "WaveletName.COIF" + order + " should have correct description");
            assertEquals(WaveletType.ORTHOGONAL, name.getType(),
                "WaveletName.COIF" + order + " should be ORTHOGONAL type");
        }
        
        System.out.println("✓ All 17 Coiflet wavelets (COIF1-COIF17) are properly registered");
    }
    
    @Test
    @DisplayName("Verify Coiflets can be retrieved by string code")
    void testCoifletRetrievalByCode() {
        for (int order = 1; order <= 17; order++) {
            String code = "coif" + order;
            
            // Find WaveletName by code
            WaveletName name = null;
            for (WaveletName wn : WaveletName.values()) {
                if (wn.getCode().equals(code)) {
                    name = wn;
                    break;
                }
            }
            
            assertNotNull(name, "WaveletName should exist for code: " + code);
            
            // Retrieve wavelet
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            assertNotNull(wavelet, "Wavelet should be retrievable for: " + code);
            assertEquals(code, wavelet.name(), "Wavelet name should match code");
        }
    }
    
    @Test
    @DisplayName("Verify Coiflets are listed in getOrthogonalWavelets()")
    void testCoifletsInOrthogonalList() {
        var orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
        
        // Count Coiflets in the list
        int coifletCount = 0;
        for (WaveletName name : orthogonalWavelets) {
            if (name.getCode().startsWith("coif")) {
                coifletCount++;
            }
        }
        
        assertEquals(17, coifletCount, 
            "Should have exactly 17 Coiflet wavelets in orthogonal wavelets list");
    }
    
    @Test
    @DisplayName("Verify COIF17 is the maximum Coiflet")
    void testCOIF17IsMaximum() {
        // COIF17 should exist
        Wavelet coif17 = WaveletRegistry.getWavelet(WaveletName.COIF17);
        assertNotNull(coif17, "COIF17 should exist in registry");
        
        // Cast to Coiflet and verify it's the maximum
        Coiflet coiflet17 = (Coiflet) coif17;
        assertEquals(102, coiflet17.lowPassDecomposition().length,
            "COIF17 should have 102 coefficients (maximum)");
        assertEquals(34, coiflet17.vanishingMoments(),
            "COIF17 should have 34 vanishing moments (maximum)");
        
        // Verify COIF18 doesn't exist
        boolean coif18Exists = false;
        for (WaveletName name : WaveletName.values()) {
            if (name.getCode().equals("coif18")) {
                coif18Exists = true;
                break;
            }
        }
        assertFalse(coif18Exists, "COIF18 should not exist (COIF17 is maximum)");
    }
}