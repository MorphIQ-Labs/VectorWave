package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BiorthogonalSplineTest {

    @Test
    void testBior1_3_Properties() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        assertEquals("bior1.3", bior.name());
        assertEquals(WaveletType.BIORTHOGONAL, bior.getType());
        assertTrue(bior.description().contains("Biorthogonal spline wavelet bior1.3"));
        assertTrue(bior.description().contains("reconstruction order 1"));
        assertTrue(bior.description().contains("decomposition order 3"));
    }

    @Test
    void testBior1_3_FilterLengths() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        assertEquals(6, bior.lowPassDecomposition().length);
        assertEquals(2, bior.highPassDecomposition().length); // Generated from lowPassRecon which has length 2
        assertEquals(2, bior.lowPassReconstruction().length);
        assertEquals(6, bior.highPassReconstruction().length); // Generated from lowPassDecomp which has length 6
    }

    @Test
    void testBior1_3_FilterValues() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // Test decomposition low-pass filter (CDF 1,3 coefficients)
        double[] expectedLowPassDecomp = {
            -1.0/8, 1.0/8, 1.0, 1.0, 1.0/8, -1.0/8
        };
        assertArrayEquals(expectedLowPassDecomp, bior.lowPassDecomposition(), 1e-15);
        
        // Test reconstruction low-pass filter (CDF 1,3 coefficients)
        double[] expectedLowPassRecon = {
            1.0, 1.0
        };
        assertArrayEquals(expectedLowPassRecon, bior.lowPassReconstruction(), 1e-15);
    }

    @Test
    void testBior1_3_HighPassFilterGeneration() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // High-pass decomposition is generated from low-pass reconstruction
        double[] highPassDecomp = bior.highPassDecomposition();
        assertEquals(2, highPassDecomp.length);
        
        // Verify alternating sign pattern (now using the corrected formula)
        assertEquals(-1.0, highPassDecomp[0], 1e-15);
        assertEquals(1.0, highPassDecomp[1], 1e-15);
        
        // High-pass reconstruction is generated from low-pass decomposition
        double[] highPassRecon = bior.highPassReconstruction();
        assertEquals(6, highPassRecon.length);
        
        // Check first and last values to verify the generation logic with corrected formula
        // highPassRecon[0] = ((6-1-0) % 2 == 0 ? 1 : -1) * lowPassDecomp[6-1-0] = (5 % 2 == 0 ? 1 : -1) * lowPassDecomp[5] = -1 * (-1/8) = 1/8
        assertEquals(1.0/8, highPassRecon[0], 1e-15);
        // highPassRecon[5] = ((6-1-5) % 2 == 0 ? 1 : -1) * lowPassDecomp[6-1-5] = (0 % 2 == 0 ? 1 : -1) * lowPassDecomp[0] = 1 * (-1/8) = -1/8
        assertEquals(-1.0/8, highPassRecon[5], 1e-15);
    }

    @Test
    void testBior1_3_VanishingMoments() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        assertEquals(3, bior.vanishingMoments()); // Decomposition order
        assertEquals(1, bior.dualVanishingMoments()); // Reconstruction order
    }

    @Test
    void testBior1_3_SplineOrder() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // Spline order is max of reconstruction and decomposition orders
        assertEquals(3, bior.splineOrder());
    }

    @Test
    void testBior1_3_Symmetry() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        assertTrue(bior.isSymmetric());
    }

    @Test
    void testFilterDefensiveCopies() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // Get filters
        double[] lowPassDecomp1 = bior.lowPassDecomposition();
        double[] lowPassDecomp2 = bior.lowPassDecomposition();
        double[] lowPassRecon1 = bior.lowPassReconstruction();
        double[] lowPassRecon2 = bior.lowPassReconstruction();
        
        // Should be different arrays (defensive copies)
        assertNotSame(lowPassDecomp1, lowPassDecomp2);
        assertNotSame(lowPassRecon1, lowPassRecon2);
        
        // But with same content
        assertArrayEquals(lowPassDecomp1, lowPassDecomp2);
        assertArrayEquals(lowPassRecon1, lowPassRecon2);
        
        // Modifying returned array shouldn't affect internal state
        lowPassDecomp1[0] = 999.0;
        assertNotEquals(999.0, bior.lowPassDecomposition()[0]);
    }

    @Test
    void testInterfaceImplementation() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // Verify it implements all required interfaces
        assertTrue(bior instanceof BiorthogonalWavelet);
        assertTrue(bior instanceof DiscreteWavelet);
        assertTrue(bior instanceof Wavelet);
    }

    @Test
    void testFilterOrthogonality() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // For biorthogonal wavelets, decomposition and reconstruction filters
        // have different lengths and are not orthogonal to themselves,
        // but satisfy biorthogonality conditions
        
        double[] lowDecomp = bior.lowPassDecomposition();
        double[] highDecomp = bior.highPassDecomposition();
        double[] lowRecon = bior.lowPassReconstruction();
        double[] highRecon = bior.highPassReconstruction();
        
        // Basic sanity checks
        assertNotEquals(lowDecomp.length, lowRecon.length);
        assertNotEquals(highDecomp.length, highRecon.length);
        
        // Check that filters sum to 2 (for CDF biorthogonal wavelets)
        double sumRecon = 0;
        for (double v : lowRecon) {
            sumRecon += v;
        }
        assertEquals(2.0, sumRecon, 1e-10); // Sum should be 2 for CDF BIOR
    }

    @Test
    void testHighPassGenerationLogic() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // Verify the alternating sign pattern in high-pass generation
        double[] lowRecon = bior.lowPassReconstruction();
        double[] highDecomp = bior.highPassDecomposition();
        
        // Verify the corrected alternating sign pattern for biorthogonal wavelets
        for (int i = 0; i < highDecomp.length; i++) {
            int sign = ((lowRecon.length - 1 - i) % 2 == 0) ? 1 : -1;
            double expected = sign * lowRecon[lowRecon.length - 1 - i];
            assertEquals(expected, highDecomp[i], 1e-15);
        }
        
        // Similarly for the other high-pass filter
        double[] lowDecomp = bior.lowPassDecomposition();
        double[] highRecon = bior.highPassReconstruction();
        
        for (int i = 0; i < highRecon.length; i++) {
            int sign = ((lowDecomp.length - 1 - i) % 2 == 0) ? 1 : -1;
            double expected = sign * lowDecomp[lowDecomp.length - 1 - i];
            assertEquals(expected, highRecon[i], 1e-15);
        }
    }

    @Test
    void testStaticConstant() {
        // Verify BIOR1_3 is properly accessible as a static constant
        assertNotNull(BiorthogonalSpline.BIOR1_3);
        assertEquals("bior1.3", BiorthogonalSpline.BIOR1_3.name());
        
        // Multiple accesses should return the same instance
        assertSame(BiorthogonalSpline.BIOR1_3, BiorthogonalSpline.BIOR1_3);
    }
    
    @Test
    void testReconstructionScale() {
        BiorthogonalSpline bior = BiorthogonalSpline.BIOR1_3;
        
        // Verify the reconstruction scale is 0.5 for CDF wavelets
        assertEquals(0.5, bior.getReconstructionScale(), 1e-15);
    }
}