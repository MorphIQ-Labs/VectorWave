package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Reverse Biorthogonal (RBIO) wavelets.
 * Verifies that RBIO wavelets correctly swap decomposition and reconstruction filters
 * compared to their BIOR counterparts.
 */
public class ReverseBiorthogonalSplineTest {
    
    /**
     * Test that all RBIO wavelets are properly registered.
     */
    @ParameterizedTest(name = "Registration: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^RBIO.*")
    @DisplayName("Verify RBIO wavelet registration")
    void testRBIORegistration(WaveletName waveletName) {
        Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
        assertNotNull(wavelet, "Wavelet " + waveletName + " should be registered");
        assertEquals(WaveletType.BIORTHOGONAL, wavelet.getType(), 
                    "Wavelet " + waveletName + " should be biorthogonal type");
        assertTrue(wavelet instanceof ReverseBiorthogonalSpline, 
                  "Wavelet " + waveletName + " should be ReverseBiorthogonalSpline instance");
    }
    
    /**
     * Test that RBIO filters are correctly swapped compared to BIOR.
     */
    @Test
    @DisplayName("Verify RBIO filter swapping")
    void testFilterSwapping() {
        // Test RBIO2.4 vs BIOR2.4
        BiorthogonalSpline bior24 = BiorthogonalSpline.BIOR2_4;
        ReverseBiorthogonalSpline rbio24 = ReverseBiorthogonalSpline.RBIO2_4;
        
        // RBIO decomposition should equal BIOR reconstruction
        assertArrayEquals(bior24.lowPassReconstruction(), rbio24.lowPassDecomposition(),
                         "RBIO2.4 decomposition should equal BIOR2.4 reconstruction");
        assertArrayEquals(bior24.highPassReconstruction(), rbio24.highPassDecomposition(),
                         "RBIO2.4 high-pass decomposition should equal BIOR2.4 high-pass reconstruction");
        
        // RBIO reconstruction should equal BIOR decomposition
        assertArrayEquals(bior24.lowPassDecomposition(), rbio24.lowPassReconstruction(),
                         "RBIO2.4 reconstruction should equal BIOR2.4 decomposition");
        assertArrayEquals(bior24.highPassDecomposition(), rbio24.highPassReconstruction(),
                         "RBIO2.4 high-pass reconstruction should equal BIOR2.4 high-pass decomposition");
    }
    
    /**
     * Test vanishing moments are swapped.
     */
    @Test
    @DisplayName("Verify vanishing moments swapping")
    void testVanishingMomentsSwapped() {
        // Test RBIO3.5 vs BIOR3.5
        BiorthogonalSpline bior35 = BiorthogonalSpline.BIOR3_5;
        ReverseBiorthogonalSpline rbio35 = ReverseBiorthogonalSpline.RBIO3_5;
        
        // Vanishing moments should be swapped
        assertEquals(bior35.vanishingMoments(), rbio35.dualVanishingMoments(),
                    "RBIO3.5 dual vanishing moments should equal BIOR3.5 primary vanishing moments");
        assertEquals(bior35.dualVanishingMoments(), rbio35.vanishingMoments(),
                    "RBIO3.5 primary vanishing moments should equal BIOR3.5 dual vanishing moments");
        
        // For BIOR3.5: decomposition=5, reconstruction=3
        // For RBIO3.5: decomposition=3, reconstruction=5
        assertEquals(3, rbio35.vanishingMoments(), "RBIO3.5 should have 3 vanishing moments");
        assertEquals(5, rbio35.dualVanishingMoments(), "RBIO3.5 should have 5 dual vanishing moments");
    }
    
    /**
     * Test filter lengths are correctly swapped.
     */
    @ParameterizedTest(name = "Filter lengths: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^RBIO.*")
    @DisplayName("Verify RBIO filter length relationships")
    void testFilterLengths(WaveletName waveletName) {
        ReverseBiorthogonalSpline rbio = (ReverseBiorthogonalSpline) WaveletRegistry.getWavelet(waveletName);
        BiorthogonalSpline bior = rbio.getOriginalBior();
        
        // RBIO decomposition length should equal BIOR reconstruction length
        assertEquals(bior.lowPassReconstruction().length, rbio.lowPassDecomposition().length,
                    "RBIO decomposition length should equal BIOR reconstruction length");
        
        // RBIO reconstruction length should equal BIOR decomposition length
        assertEquals(bior.lowPassDecomposition().length, rbio.lowPassReconstruction().length,
                    "RBIO reconstruction length should equal BIOR decomposition length");
    }
    
    /**
     * Test specific RBIO wavelets.
     */
    @Test
    @DisplayName("Test RBIO1.1 (Haar-like)")
    void testRBIO11() {
        ReverseBiorthogonalSpline rbio11 = ReverseBiorthogonalSpline.RBIO1_1;
        
        // RBIO1.1 should be identical to BIOR1.1 (symmetric case)
        double[] loD = rbio11.lowPassDecomposition();
        double[] loR = rbio11.lowPassReconstruction();
        
        assertArrayEquals(loD, loR, 1e-10,
                         "RBIO1.1 should have identical decomposition and reconstruction (like BIOR1.1)");
        assertEquals(2, loD.length, "RBIO1.1 should have 2 coefficients");
    }
    
    @Test
    @DisplayName("Test RBIO2.4 (fast analysis)")
    void testRBIO24() {
        ReverseBiorthogonalSpline rbio24 = ReverseBiorthogonalSpline.RBIO2_4;
        
        // RBIO2.4 has short decomposition (3 taps) and long reconstruction (9 taps)
        assertEquals(3, rbio24.lowPassDecomposition().length,
                    "RBIO2.4 should have 3-tap decomposition for fast analysis");
        assertEquals(9, rbio24.lowPassReconstruction().length,
                    "RBIO2.4 should have 9-tap reconstruction for smooth synthesis");
        
        // Vanishing moments: 2 for decomposition, 4 for reconstruction
        assertEquals(2, rbio24.vanishingMoments(), "RBIO2.4 should have 2 vanishing moments");
        assertEquals(4, rbio24.dualVanishingMoments(), "RBIO2.4 should have 4 dual vanishing moments");
    }
    
    @Test
    @DisplayName("Test RBIO4.4 (smooth reconstruction)")
    void testRBIO44() {
        ReverseBiorthogonalSpline rbio44 = ReverseBiorthogonalSpline.RBIO4_4;
        BiorthogonalSpline bior44 = BiorthogonalSpline.BIOR4_4;
        
        // RBIO4.4 swaps the CDF 9/7 filters
        assertEquals(7, rbio44.lowPassDecomposition().length,
                    "RBIO4.4 should have 7-tap decomposition");
        assertEquals(9, rbio44.lowPassReconstruction().length,
                    "RBIO4.4 should have 9-tap reconstruction");
        
        // Should be symmetric like BIOR4.4
        assertTrue(rbio44.isSymmetric(), "RBIO4.4 should be symmetric");
        
        // Vanishing moments are both 4
        assertEquals(4, rbio44.vanishingMoments(), "RBIO4.4 should have 4 vanishing moments");
        assertEquals(4, rbio44.dualVanishingMoments(), "RBIO4.4 should have 4 dual vanishing moments");
    }
    
    /**
     * Test properties preservation.
     */
    @ParameterizedTest(name = "Properties: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^RBIO.*")
    @DisplayName("Verify RBIO properties preservation")
    void testPropertiesPreservation(WaveletName waveletName) {
        ReverseBiorthogonalSpline rbio = (ReverseBiorthogonalSpline) WaveletRegistry.getWavelet(waveletName);
        BiorthogonalSpline bior = rbio.getOriginalBior();
        
        // Symmetry should be preserved
        assertEquals(bior.isSymmetric(), rbio.isSymmetric(),
                    "Symmetry property should be preserved");
        
        // Spline order should be preserved
        assertEquals(bior.splineOrder(), rbio.splineOrder(),
                    "Spline order should be preserved");
        
        // Type should remain biorthogonal
        assertEquals(WaveletType.BIORTHOGONAL, rbio.getType(),
                    "Type should remain biorthogonal");
    }
    
    /**
     * Test that all RBIO wavelets are in the biorthogonal list.
     */
    @Test
    @DisplayName("Verify RBIO wavelets in biorthogonal list")
    void testRBIOInBiorthogonalList() {
        var bioWavelets = WaveletRegistry.getBiorthogonalWavelets();
        
        // Check all RBIO wavelets are included
        assertTrue(bioWavelets.contains(WaveletName.RBIO1_1));
        assertTrue(bioWavelets.contains(WaveletName.RBIO1_3));
        assertTrue(bioWavelets.contains(WaveletName.RBIO1_5));
        assertTrue(bioWavelets.contains(WaveletName.RBIO2_2));
        assertTrue(bioWavelets.contains(WaveletName.RBIO2_4));
        assertTrue(bioWavelets.contains(WaveletName.RBIO2_6));
        assertTrue(bioWavelets.contains(WaveletName.RBIO2_8));
        assertTrue(bioWavelets.contains(WaveletName.RBIO3_1));
        assertTrue(bioWavelets.contains(WaveletName.RBIO3_3));
        assertTrue(bioWavelets.contains(WaveletName.RBIO3_5));
        assertTrue(bioWavelets.contains(WaveletName.RBIO3_7));
        assertTrue(bioWavelets.contains(WaveletName.RBIO3_9));
        assertTrue(bioWavelets.contains(WaveletName.RBIO4_4));
        assertTrue(bioWavelets.contains(WaveletName.RBIO5_5));
        assertTrue(bioWavelets.contains(WaveletName.RBIO6_8));
        
        // Should now have 30 total biorthogonal wavelets (15 BIOR + 15 RBIO)
        assertEquals(30, bioWavelets.size(), "Should have 30 total biorthogonal wavelets");
    }
    
    /**
     * Test reconstruction scaling factor is properly inverted.
     */
    @Test
    @DisplayName("Verify RBIO reconstruction scaling")
    void testReconstructionScaling() {
        // Test RBIO1.3 which has BIOR1.3 scaling of 0.5
        BiorthogonalSpline bior13 = BiorthogonalSpline.BIOR1_3;
        ReverseBiorthogonalSpline rbio13 = ReverseBiorthogonalSpline.RBIO1_3;
        
        double biorScale = bior13.getReconstructionScale();
        double rbioScale = rbio13.getReconstructionScale();
        
        // RBIO scale should be inverted (1/0.5 = 2.0)
        assertEquals(1.0 / biorScale, rbioScale, 1e-10,
                    "RBIO1.3 reconstruction scale should be inverse of BIOR1.3");
        assertEquals(2.0, rbioScale, 1e-10,
                    "RBIO1.3 should have reconstruction scale of 2.0");
        
        // Test a case where scale is 1.0 (should remain 1.0)
        BiorthogonalSpline bior22 = BiorthogonalSpline.BIOR2_2;
        ReverseBiorthogonalSpline rbio22 = ReverseBiorthogonalSpline.RBIO2_2;
        
        if (Math.abs(bior22.getReconstructionScale() - 1.0) < 1e-10) {
            assertEquals(1.0, rbio22.getReconstructionScale(), 1e-10,
                        "RBIO2.2 should maintain scale of 1.0 when BIOR2.2 has scale 1.0");
        }
    }
    
    /**
     * Test group delay calculation for RBIO wavelets.
     */
    @Test
    @DisplayName("Verify RBIO group delay")
    void testGroupDelay() {
        // Test several RBIO wavelets
        ReverseBiorthogonalSpline rbio24 = ReverseBiorthogonalSpline.RBIO2_4;
        ReverseBiorthogonalSpline rbio33 = ReverseBiorthogonalSpline.RBIO3_3;
        
        // Group delay should be non-negative
        assertTrue(rbio24.getGroupDelay() >= 0,
                  "RBIO2.4 group delay should be non-negative");
        assertTrue(rbio33.getGroupDelay() >= 0,
                  "RBIO3.3 group delay should be non-negative");
        
        // Verify the delay calculation makes sense
        // For RBIO2.4: decomp=3, recon=9
        // delay = ((3-1) + (9-1))/2 - 1 = (2+8)/2 - 1 = 5 - 1 = 4
        assertEquals(4, rbio24.getGroupDelay(),
                    "RBIO2.4 should have group delay of 4");
    }
    
    /**
     * Test filter DC response.
     */
    @ParameterizedTest(name = "DC Response: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^RBIO.*")
    @DisplayName("Verify RBIO filter DC response")
    void testFilterDCResponse(WaveletName waveletName) {
        ReverseBiorthogonalSpline rbio = (ReverseBiorthogonalSpline) WaveletRegistry.getWavelet(waveletName);
        
        double[] loD = rbio.lowPassDecomposition();
        double[] loR = rbio.lowPassReconstruction();
        double[] hiD = rbio.highPassDecomposition();
        double[] hiR = rbio.highPassReconstruction();
        
        // Sum of low-pass coefficients should be positive (pass DC)
        double sumLoD = sum(loD);
        double sumLoR = sum(loR);
        
        assertTrue(sumLoD > 0.1, 
            String.format("%s: Sum of decomposition low-pass should be positive (got %.6f)", 
                waveletName, sumLoD));
        assertTrue(sumLoR > 0.1,
            String.format("%s: Sum of reconstruction low-pass should be positive (got %.6f)", 
                waveletName, sumLoR));
        
        // Sum of high-pass coefficients should be close to zero (block DC)
        double sumHiD = sum(hiD);
        double sumHiR = sum(hiR);
        
        assertEquals(0.0, sumHiD, 1.0,
            String.format("%s: Sum of decomposition high-pass should be near zero", waveletName));
        assertEquals(0.0, sumHiR, 1.0,
            String.format("%s: Sum of reconstruction high-pass should be near zero", waveletName));
    }
    
    // Helper method
    private double sum(double[] array) {
        double sum = 0;
        for (double v : array) {
            sum += v;
        }
        return sum;
    }
}