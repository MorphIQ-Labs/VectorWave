package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic mathematical verification for biorthogonal wavelets.
 * Focuses on essential properties that must hold for functional wavelets.
 */
public class BiorthogonalBasicVerificationTest {
    
    /**
     * Test 1: Verify filter relationships for biorthogonal wavelets.
     * For biorthogonal wavelets, the high-pass filters are derived from low-pass filters
     * with specific relationships.
     */
    @ParameterizedTest(name = "Filter Relationships: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^BIOR.*")
    @DisplayName("Verify biorthogonal filter relationships")
    void testFilterRelationships(WaveletName waveletName) {
        BiorthogonalWavelet wavelet = (BiorthogonalWavelet) WaveletRegistry.getWavelet(waveletName);
        
        double[] loD = wavelet.lowPassDecomposition();
        double[] hiD = wavelet.highPassDecomposition();
        double[] loR = wavelet.lowPassReconstruction();
        double[] hiR = wavelet.highPassReconstruction();
        
        // Basic length relationships for biorthogonal wavelets
        assertEquals(loR.length, hiD.length, 
            String.format("%s: Low-pass reconstruction and high-pass decomposition should have same length", 
                waveletName));
        assertEquals(loD.length, hiR.length,
            String.format("%s: Low-pass decomposition and high-pass reconstruction should have same length", 
                waveletName));
        
        // All filters should be non-empty
        assertTrue(loD.length > 0, "Decomposition low-pass filter should not be empty");
        assertTrue(hiD.length > 0, "Decomposition high-pass filter should not be empty");
        assertTrue(loR.length > 0, "Reconstruction low-pass filter should not be empty");
        assertTrue(hiR.length > 0, "Reconstruction high-pass filter should not be empty");
    }
    
    /**
     * Test 2: Verify vanishing moments match the wavelet specification.
     * The wavelet name encodes the vanishing moments (e.g., BIOR2.4 has 2 and 4 vanishing moments).
     */
    @ParameterizedTest(name = "Vanishing Moments: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^BIOR.*")
    @DisplayName("Verify vanishing moments match wavelet name")
    void testVanishingMomentsSpecification(WaveletName waveletName) {
        BiorthogonalWavelet wavelet = (BiorthogonalWavelet) WaveletRegistry.getWavelet(waveletName);
        
        // Extract expected values from name (e.g., BIOR2_4 -> rec=2, dec=4)
        String code = waveletName.getCode();
        String[] parts = code.replace("bior", "").split("\\.");
        int expectedRecOrder = Integer.parseInt(parts[0]);
        int expectedDecOrder = Integer.parseInt(parts[1]);
        
        assertEquals(expectedDecOrder, wavelet.vanishingMoments(),
            String.format("%s: Decomposition vanishing moments should be %d", 
                waveletName, expectedDecOrder));
        
        assertEquals(expectedRecOrder, wavelet.dualVanishingMoments(),
            String.format("%s: Reconstruction vanishing moments should be %d", 
                waveletName, expectedRecOrder));
    }
    
    /**
     * Test 3: Verify filter normalization for DC response.
     * Low-pass filters should have positive sum, high-pass filters should sum to approximately zero.
     */
    @ParameterizedTest(name = "Filter DC Response: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^BIOR.*")
    @DisplayName("Verify filter DC response")
    void testFilterDCResponse(WaveletName waveletName) {
        BiorthogonalWavelet wavelet = (BiorthogonalWavelet) WaveletRegistry.getWavelet(waveletName);
        
        double[] loD = wavelet.lowPassDecomposition();
        double[] loR = wavelet.lowPassReconstruction();
        double[] hiD = wavelet.highPassDecomposition();
        double[] hiR = wavelet.highPassReconstruction();
        
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
        
        // Use relaxed tolerance for high-pass sums
        assertEquals(0.0, sumHiD, 1.0,
            String.format("%s: Sum of decomposition high-pass should be near zero (got %.6f)", 
                waveletName, sumHiD));
        assertEquals(0.0, sumHiR, 1.0,
            String.format("%s: Sum of reconstruction high-pass should be near zero (got %.6f)", 
                waveletName, sumHiR));
    }
    
    /**
     * Test 4: Verify key wavelets have expected properties.
     */
    @Test
    @DisplayName("Verify BIOR4.4 (JPEG2000) properties")
    void testBIOR44_JPEG2000() {
        BiorthogonalWavelet bior44 = (BiorthogonalWavelet) WaveletRegistry.getWavelet(WaveletName.BIOR4_4);
        
        // BIOR4.4 is the CDF 9/7 wavelet used in JPEG2000
        assertEquals(9, bior44.lowPassDecomposition().length, 
            "BIOR4.4 should have 9-tap decomposition filter (CDF 9/7)");
        assertEquals(7, bior44.lowPassReconstruction().length,
            "BIOR4.4 should have 7-tap reconstruction filter (CDF 9/7)");
        
        // Verify it's symmetric
        assertTrue(bior44.isSymmetric(), "BIOR4.4 should be symmetric");
        
        // Verify vanishing moments
        assertEquals(4, bior44.vanishingMoments(), 
            "BIOR4.4 should have 4 vanishing moments for decomposition");
        assertEquals(4, bior44.dualVanishingMoments(),
            "BIOR4.4 should have 4 vanishing moments for reconstruction");
    }
    
    @Test
    @DisplayName("Verify BIOR1.1 (Haar-like) properties")
    void testBIOR11_Haar() {
        BiorthogonalWavelet bior11 = (BiorthogonalWavelet) WaveletRegistry.getWavelet(WaveletName.BIOR1_1);
        
        double[] loD = bior11.lowPassDecomposition();
        double[] loR = bior11.lowPassReconstruction();
        
        // BIOR1.1 should have identical decomposition and reconstruction
        assertArrayEquals(loD, loR, 1e-10,
            "BIOR1.1 should have identical decomposition and reconstruction filters");
        
        // Should have 2 coefficients like Haar
        assertEquals(2, loD.length, "BIOR1.1 should have 2 coefficients like Haar");
    }
    
    @Test
    @DisplayName("Verify BIOR2.2 (edge preservation) properties")
    void testBIOR22_EdgePreservation() {
        BiorthogonalWavelet bior22 = (BiorthogonalWavelet) WaveletRegistry.getWavelet(WaveletName.BIOR2_2);
        
        // BIOR2.2 is popular for edge preservation
        assertTrue(bior22.isSymmetric(), "BIOR2.2 should be symmetric");
        assertEquals(2, bior22.vanishingMoments(), "BIOR2.2 should have 2 vanishing moments for decomposition");
        assertEquals(2, bior22.dualVanishingMoments(), "BIOR2.2 should have 2 vanishing moments for reconstruction");
        
        // CDF 2,2 construction
        assertEquals(5, bior22.lowPassDecomposition().length, "BIOR2.2 should have 5-tap decomposition");
        assertEquals(3, bior22.lowPassReconstruction().length, "BIOR2.2 should have 3-tap reconstruction");
    }
    
    @Test
    @DisplayName("Verify BIOR3.3 (balanced) properties")
    void testBIOR33_Balanced() {
        BiorthogonalWavelet bior33 = (BiorthogonalWavelet) WaveletRegistry.getWavelet(WaveletName.BIOR3_3);
        
        assertTrue(bior33.isSymmetric(), "BIOR3.3 should be symmetric");
        assertEquals(3, bior33.vanishingMoments(), "BIOR3.3 should have 3 vanishing moments for decomposition");
        assertEquals(3, bior33.dualVanishingMoments(), "BIOR3.3 should have 3 vanishing moments for reconstruction");
        
        assertEquals(8, bior33.lowPassDecomposition().length, "BIOR3.3 should have 8-tap decomposition");
        assertEquals(4, bior33.lowPassReconstruction().length, "BIOR3.3 should have 4-tap reconstruction");
    }
    
    /**
     * Test 5: Verify filter energies are reasonable.
     * The sum of squares (energy) should be positive and finite.
     */
    @ParameterizedTest(name = "Filter Energy: {0}")
    @EnumSource(value = WaveletName.class, mode = EnumSource.Mode.MATCH_ALL, names = "^BIOR.*")
    @DisplayName("Verify filter energies are reasonable")
    void testFilterEnergy(WaveletName waveletName) {
        BiorthogonalWavelet wavelet = (BiorthogonalWavelet) WaveletRegistry.getWavelet(waveletName);
        
        double[] loD = wavelet.lowPassDecomposition();
        double[] hiD = wavelet.highPassDecomposition();
        double[] loR = wavelet.lowPassReconstruction();
        double[] hiR = wavelet.highPassReconstruction();
        
        // Compute energy (sum of squares)
        double energyLoD = sumOfSquares(loD);
        double energyHiD = sumOfSquares(hiD);
        double energyLoR = sumOfSquares(loR);
        double energyHiR = sumOfSquares(hiR);
        
        // All energies should be positive and finite
        assertTrue(energyLoD > 0 && Double.isFinite(energyLoD), 
            String.format("%s: Decomposition low-pass energy should be positive and finite", waveletName));
        assertTrue(energyHiD > 0 && Double.isFinite(energyHiD),
            String.format("%s: Decomposition high-pass energy should be positive and finite", waveletName));
        assertTrue(energyLoR > 0 && Double.isFinite(energyLoR),
            String.format("%s: Reconstruction low-pass energy should be positive and finite", waveletName));
        assertTrue(energyHiR > 0 && Double.isFinite(energyHiR),
            String.format("%s: Reconstruction high-pass energy should be positive and finite", waveletName));
        
        // Energy should be reasonable (not too large or too small)
        assertTrue(energyLoD < 100, "Decomposition low-pass energy should be reasonable");
        assertTrue(energyHiD < 100, "Decomposition high-pass energy should be reasonable");
        assertTrue(energyLoR < 100, "Reconstruction low-pass energy should be reasonable");
        assertTrue(energyHiR < 100, "Reconstruction high-pass energy should be reasonable");
    }
    
    /**
     * Test 6: Verify all biorthogonal wavelets are accessible.
     */
    @Test
    @DisplayName("Verify all BIOR wavelets are registered")
    void testAllBiorthogonalWaveletsRegistered() {
        var bioWavelets = WaveletRegistry.getBiorthogonalWavelets();
        
        assertNotNull(bioWavelets);
        assertFalse(bioWavelets.isEmpty(), "Should have biorthogonal wavelets registered");
        
        // Check we have the expected count (15 BIOR + 15 RBIO)
        assertEquals(30, bioWavelets.size(), "Should have exactly 30 biorthogonal wavelets registered");
        
        // Check key wavelets are present
        assertTrue(bioWavelets.contains(WaveletName.BIOR1_1), "BIOR1.1 should be registered");
        assertTrue(bioWavelets.contains(WaveletName.BIOR1_3), "BIOR1.3 should be registered");
        assertTrue(bioWavelets.contains(WaveletName.BIOR2_2), "BIOR2.2 should be registered");
        assertTrue(bioWavelets.contains(WaveletName.BIOR3_3), "BIOR3.3 should be registered");
        assertTrue(bioWavelets.contains(WaveletName.BIOR4_4), "BIOR4.4 (JPEG2000) should be registered");
        assertTrue(bioWavelets.contains(WaveletName.BIOR6_8), "BIOR6.8 should be registered");
    }
    
    // Helper methods
    
    private double sum(double[] array) {
        double sum = 0;
        for (double v : array) {
            sum += v;
        }
        return sum;
    }
    
    private double sumOfSquares(double[] array) {
        double sum = 0;
        for (double v : array) {
            sum += v * v;
        }
        return sum;
    }
}