package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple tests for biorthogonal wavelets focusing on registration and filter properties.
 * Transform tests will be handled separately after MODWT biorthogonal support is verified.
 */
public class SimpleBiorthogonalWaveletTest {
    
    /**
     * Test that all registered biorthogonal wavelets can be retrieved from the registry.
     */
    @ParameterizedTest
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, 
                names = "^BIOR.*")
    void testBiorthogonalWaveletRegistration(WaveletName waveletName) {
        Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
        assertNotNull(wavelet, "Wavelet " + waveletName + " should be registered");
        assertEquals(WaveletType.BIORTHOGONAL, wavelet.getType(), 
                    "Wavelet " + waveletName + " should be biorthogonal type");
        assertTrue(wavelet instanceof BiorthogonalWavelet, 
                  "Wavelet " + waveletName + " should implement BiorthogonalWavelet interface");
    }
    
    /**
     * Test filter properties for biorthogonal wavelets.
     */
    @ParameterizedTest
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, 
                names = "^BIOR.*")
    void testFilterProperties(WaveletName waveletName) {
        Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
        BiorthogonalWavelet bioWavelet = (BiorthogonalWavelet) wavelet;
        
        // Get filters
        double[] loD = wavelet.lowPassDecomposition();
        double[] hiD = wavelet.highPassDecomposition();
        double[] loR = wavelet.lowPassReconstruction();
        double[] hiR = wavelet.highPassReconstruction();
        
        // Test filter lengths
        assertTrue(loD.length > 0, "Decomposition low-pass filter should not be empty");
        assertTrue(hiD.length > 0, "Decomposition high-pass filter should not be empty");
        assertTrue(loR.length > 0, "Reconstruction low-pass filter should not be empty");
        assertTrue(hiR.length > 0, "Reconstruction high-pass filter should not be empty");
        
        // For biorthogonal wavelets, decomposition and reconstruction filters have specific relationships
        assertEquals(loR.length, hiD.length, 
                    "Low-pass reconstruction and high-pass decomposition should have same length");
        assertEquals(loD.length, hiR.length,
                    "Low-pass decomposition and high-pass reconstruction should have same length");
    }
    
    /**
     * Test specific properties of key biorthogonal wavelets.
     */
    @Test
    void testSpecificWavelets() {
        // Test BIOR1.1 (should be identical to Haar)
        BiorthogonalWavelet bior11 = (BiorthogonalWavelet) WaveletRegistry.getWavelet(WaveletName.BIOR1_1);
        double[] loD = bior11.lowPassDecomposition();
        double[] loR = bior11.lowPassReconstruction();
        assertArrayEquals(loD, loR, 1e-10, "BIOR1.1 should have identical decomposition and reconstruction filters");
        
        // Test BIOR4.4 (JPEG2000 wavelet)
        BiorthogonalWavelet bior44 = (BiorthogonalWavelet) WaveletRegistry.getWavelet(WaveletName.BIOR4_4);
        assertEquals(4, bior44.vanishingMoments(), "BIOR4.4 should have 4 vanishing moments for decomposition");
        assertEquals(4, bior44.dualVanishingMoments(), "BIOR4.4 should have 4 vanishing moments for reconstruction");
        assertEquals(9, bior44.lowPassDecomposition().length, "BIOR4.4 decomposition filter should have 9 coefficients");
        assertEquals(7, bior44.lowPassReconstruction().length, "BIOR4.4 reconstruction filter should have 7 coefficients");
        
        // Test BIOR2.2 (popular for edge preservation)
        BiorthogonalWavelet bior22 = (BiorthogonalWavelet) WaveletRegistry.getWavelet(WaveletName.BIOR2_2);
        assertTrue(bior22.isSymmetric(), "BIOR2.2 should be symmetric");
        assertEquals(2, bior22.vanishingMoments(), "BIOR2.2 should have 2 vanishing moments for decomposition");
        assertEquals(2, bior22.dualVanishingMoments(), "BIOR2.2 should have 2 vanishing moments for reconstruction");
        
        // Test BIOR3.3 (balanced properties)
        BiorthogonalWavelet bior33 = (BiorthogonalWavelet) WaveletRegistry.getWavelet(WaveletName.BIOR3_3);
        assertTrue(bior33.isSymmetric(), "BIOR3.3 should be symmetric");
        assertEquals(3, bior33.vanishingMoments(), "BIOR3.3 should have 3 vanishing moments for decomposition");
        assertEquals(3, bior33.dualVanishingMoments(), "BIOR3.3 should have 3 vanishing moments for reconstruction");
    }
    
    /**
     * Test that the wavelet filter sums are appropriate.
     */
    @ParameterizedTest
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, 
                names = "^BIOR.*")
    void testFilterSums(WaveletName waveletName) {
        Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
        
        double[] loD = wavelet.lowPassDecomposition();
        double[] loR = wavelet.lowPassReconstruction();
        
        // Sum of low-pass filter should be positive
        double sumLoD = Arrays.stream(loD).sum();
        double sumLoR = Arrays.stream(loR).sum();
        
        assertTrue(sumLoD > 0, "Sum of decomposition low-pass filter should be positive");
        assertTrue(sumLoR > 0, "Sum of reconstruction low-pass filter should be positive");
    }
    
    /**
     * Test all biorthogonal wavelets are accessible via the registry method.
     */
    @Test
    void testGetBiorthogonalWavelets() {
        var bioWavelets = WaveletRegistry.getBiorthogonalWavelets();
        assertNotNull(bioWavelets);
        assertFalse(bioWavelets.isEmpty(), "Should have biorthogonal wavelets registered");
        
        // Check we have all the expected BIOR wavelets
        assertTrue(bioWavelets.contains(WaveletName.BIOR1_1));
        assertTrue(bioWavelets.contains(WaveletName.BIOR1_3));
        assertTrue(bioWavelets.contains(WaveletName.BIOR1_5));
        assertTrue(bioWavelets.contains(WaveletName.BIOR2_2));
        assertTrue(bioWavelets.contains(WaveletName.BIOR2_4));
        assertTrue(bioWavelets.contains(WaveletName.BIOR2_6));
        assertTrue(bioWavelets.contains(WaveletName.BIOR2_8));
        assertTrue(bioWavelets.contains(WaveletName.BIOR3_1));
        assertTrue(bioWavelets.contains(WaveletName.BIOR3_3));
        assertTrue(bioWavelets.contains(WaveletName.BIOR3_5));
        assertTrue(bioWavelets.contains(WaveletName.BIOR3_7));
        assertTrue(bioWavelets.contains(WaveletName.BIOR3_9));
        assertTrue(bioWavelets.contains(WaveletName.BIOR4_4));
        assertTrue(bioWavelets.contains(WaveletName.BIOR5_5));
        assertTrue(bioWavelets.contains(WaveletName.BIOR6_8));
        
        assertEquals(30, bioWavelets.size(), "Should have exactly 30 biorthogonal wavelets registered (15 BIOR + 15 RBIO)");
    }
    
    /**
     * Test vanishing moments for all biorthogonal wavelets.
     */
    @ParameterizedTest
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, 
                names = "^BIOR.*")
    void testVanishingMoments(WaveletName waveletName) {
        BiorthogonalWavelet wavelet = (BiorthogonalWavelet) WaveletRegistry.getWavelet(waveletName);
        
        // Extract the numbers from the wavelet name (e.g., BIOR2_4 -> 2 and 4)
        String code = waveletName.getCode();
        String[] parts = code.replace("bior", "").split("\\.");
        int expectedReconOrder = Integer.parseInt(parts[0]);
        int expectedDecompOrder = Integer.parseInt(parts[1]);
        
        assertEquals(expectedDecompOrder, wavelet.vanishingMoments(), 
                    "Decomposition vanishing moments should match the name");
        assertEquals(expectedReconOrder, wavelet.dualVanishingMoments(),
                    "Reconstruction vanishing moments should match the name");
    }
}