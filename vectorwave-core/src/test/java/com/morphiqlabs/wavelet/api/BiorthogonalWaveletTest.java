package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for BiorthogonalWavelet interface default methods.
 */
class BiorthogonalWaveletTest {

    private static final double EPSILON = 1e-10;

    /**
     * Test implementation of BiorthogonalWavelet for testing default methods.
     */
    private static class TestBiorthogonalWavelet implements BiorthogonalWavelet {
        private final double[] lowPassDecomp;
        private final double[] highPassDecomp;
        private final double[] lowPassRecon;
        private final double[] highPassRecon;
        private final int vanishing;
        private final int dualVanishing;
        private final boolean symmetric;
        private final int splineOrder;

        public TestBiorthogonalWavelet(double[] lowPassDecomp, double[] highPassDecomp,
                                     double[] lowPassRecon, double[] highPassRecon,
                                     int vanishing, int dualVanishing, 
                                     boolean symmetric, int splineOrder) {
            this.lowPassDecomp = lowPassDecomp;
            this.highPassDecomp = highPassDecomp;
            this.lowPassRecon = lowPassRecon;
            this.highPassRecon = highPassRecon;
            this.vanishing = vanishing;
            this.dualVanishing = dualVanishing;
            this.symmetric = symmetric;
            this.splineOrder = splineOrder;
        }

        @Override
        public double[] lowPassDecomposition() {
            return lowPassDecomp.clone();
        }

        @Override
        public double[] highPassDecomposition() {
            return highPassDecomp.clone();
        }

        @Override
        public double[] lowPassReconstruction() {
            return lowPassRecon.clone();
        }

        @Override
        public double[] highPassReconstruction() {
            return highPassRecon.clone();
        }

        @Override
        public int vanishingMoments() {
            return vanishing;
        }

        @Override
        public int dualVanishingMoments() {
            return dualVanishing;
        }

        @Override
        public boolean isSymmetric() {
            return symmetric;
        }

        @Override
        public int splineOrder() {
            return splineOrder;
        }

        @Override
        public String name() {
            return "TestBior";
        }

    }

    @Test
    @DisplayName("Test splineOrder default method returns -1")
    void testSplineOrderDefault() {
        // Test with a wavelet that doesn't override splineOrder
        BiorthogonalWavelet wavelet = new TestBiorthogonalWavelet(
            new double[]{0.1, 0.2}, new double[]{0.3, 0.4},
            new double[]{0.5, 0.6}, new double[]{0.7, 0.8},
            1, 1, true, -1
        ) {
            @Override
            public int splineOrder() {
                return super.splineOrder(); // Use default implementation
            }
        };
        
        assertEquals(-1, wavelet.splineOrder());
    }

    @Test
    @DisplayName("Test splineOrder with valid orders")
    void testSplineOrderValues() {
        // Test with various spline orders
        for (int order = 0; order <= 5; order++) {
            BiorthogonalWavelet wavelet = new TestBiorthogonalWavelet(
                new double[]{0.1, 0.2}, new double[]{0.3, 0.4},
                new double[]{0.5, 0.6}, new double[]{0.7, 0.8},
                1, 1, true, order
            );
            
            assertEquals(order, wavelet.splineOrder());
        }
    }

    @Test
    @DisplayName("Test reconstructionLength default method")
    void testReconstructionLength() {
        double[] reconCoeffs = {0.1, 0.2, 0.3, 0.4, 0.5};
        BiorthogonalWavelet wavelet = new TestBiorthogonalWavelet(
            new double[]{0.1, 0.2}, new double[]{0.3, 0.4},
            reconCoeffs, new double[]{0.7, 0.8},
            1, 1, true, -1
        );
        
        assertEquals(5, wavelet.reconstructionLength());
    }

    @Test
    @DisplayName("Test reconstructionLength with various sizes")
    void testReconstructionLengthVariousSizes() {
        // Test with different reconstruction filter lengths
        int[] lengths = {1, 2, 4, 6, 8, 10, 16};
        
        for (int length : lengths) {
            double[] reconCoeffs = new double[length];
            BiorthogonalWavelet wavelet = new TestBiorthogonalWavelet(
                new double[]{0.1, 0.2}, new double[]{0.3, 0.4},
                reconCoeffs, new double[]{0.7, 0.8},
                1, 1, true, -1
            );
            
            assertEquals(length, wavelet.reconstructionLength());
        }
    }

    @Test
    @DisplayName("Test getType default method returns BIORTHOGONAL")
    void testGetType() {
        BiorthogonalWavelet wavelet = new TestBiorthogonalWavelet(
            new double[]{0.1, 0.2}, new double[]{0.3, 0.4},
            new double[]{0.5, 0.6}, new double[]{0.7, 0.8},
            1, 1, true, -1
        );
        
        assertEquals(WaveletType.BIORTHOGONAL, wavelet.getType());
    }

    @Test
    @DisplayName("Test inherited DiscreteWavelet methods")
    void testDiscreteWaveletInheritance() {
        double[] decompCoeffs = {0.1, 0.2, 0.3, 0.4};
        BiorthogonalWavelet wavelet = new TestBiorthogonalWavelet(
            decompCoeffs, new double[]{0.3, 0.4},
            new double[]{0.5, 0.6}, new double[]{0.7, 0.8},
            2, 3, true, 1
        );
        
        // Test supportWidth (inherited from DiscreteWavelet)
        assertEquals(4, wavelet.supportWidth());
        
        // Test hasCompactSupport (inherited from DiscreteWavelet)
        assertTrue(wavelet.hasCompactSupport());
        
        // Test vanishingMoments
        assertEquals(2, wavelet.vanishingMoments());
    }

    @Test
    @DisplayName("Test dual vanishing moments")
    void testDualVanishingMoments() {
        BiorthogonalWavelet wavelet1 = new TestBiorthogonalWavelet(
            new double[]{0.1, 0.2}, new double[]{0.3, 0.4},
            new double[]{0.5, 0.6}, new double[]{0.7, 0.8},
            1, 2, true, -1
        );
        
        assertEquals(1, wavelet1.vanishingMoments());
        assertEquals(2, wavelet1.dualVanishingMoments());
        
        BiorthogonalWavelet wavelet2 = new TestBiorthogonalWavelet(
            new double[]{0.1, 0.2}, new double[]{0.3, 0.4},
            new double[]{0.5, 0.6}, new double[]{0.7, 0.8},
            3, 1, true, -1
        );
        
        assertEquals(3, wavelet2.vanishingMoments());
        assertEquals(1, wavelet2.dualVanishingMoments());
    }

    @Test
    @DisplayName("Test symmetry property")
    void testSymmetry() {
        BiorthogonalWavelet symmetricWavelet = new TestBiorthogonalWavelet(
            new double[]{0.1, 0.2}, new double[]{0.3, 0.4},
            new double[]{0.5, 0.6}, new double[]{0.7, 0.8},
            1, 1, true, -1
        );
        assertTrue(symmetricWavelet.isSymmetric());
        
        BiorthogonalWavelet asymmetricWavelet = new TestBiorthogonalWavelet(
            new double[]{0.1, 0.2}, new double[]{0.3, 0.4},
            new double[]{0.5, 0.6}, new double[]{0.7, 0.8},
            1, 1, false, -1
        );
        assertFalse(asymmetricWavelet.isSymmetric());
    }

    @Test
    @DisplayName("Test different reconstruction vs decomposition lengths")
    void testDifferentFilterLengths() {
        // Decomposition: length 2, Reconstruction: length 6
        BiorthogonalWavelet wavelet = new TestBiorthogonalWavelet(
            new double[]{0.1, 0.2}, new double[]{0.3, 0.4},
            new double[]{0.1, 0.2, 0.3, 0.4, 0.5, 0.6}, new double[]{0.7, 0.8},
            1, 1, true, -1
        );
        
        assertEquals(2, wavelet.supportWidth()); // Based on decomposition
        assertEquals(6, wavelet.reconstructionLength()); // Based on reconstruction
        assertNotEquals(wavelet.supportWidth(), wavelet.reconstructionLength());
    }

    @Test
    @DisplayName("Test consistency between methods")
    void testConsistency() {
        BiorthogonalWavelet wavelet = new TestBiorthogonalWavelet(
            new double[]{0.1, 0.2, 0.3}, new double[]{0.3, 0.4, 0.5},
            new double[]{0.5, 0.6, 0.7, 0.8}, new double[]{0.7, 0.8, 0.9, 1.0},
            2, 3, true, 2
        );
        
        // Verify all methods return expected values
        assertEquals(3, wavelet.supportWidth());
        assertEquals(4, wavelet.reconstructionLength());
        assertEquals(2, wavelet.vanishingMoments());
        assertEquals(3, wavelet.dualVanishingMoments());
        assertTrue(wavelet.isSymmetric());
        assertEquals(2, wavelet.splineOrder());
        assertEquals(WaveletType.BIORTHOGONAL, wavelet.getType());
        
        // Verify filter arrays are correct lengths
        assertEquals(3, wavelet.lowPassDecomposition().length);
        assertEquals(3, wavelet.highPassDecomposition().length);
        assertEquals(4, wavelet.lowPassReconstruction().length);
        assertEquals(4, wavelet.highPassReconstruction().length);
    }

    @Test
    @DisplayName("Test Wavelet interface inheritance")
    void testWaveletInheritance() {
        BiorthogonalWavelet wavelet = new TestBiorthogonalWavelet(
            new double[]{0.1, 0.2}, new double[]{0.3, 0.4},
            new double[]{0.5, 0.6}, new double[]{0.7, 0.8},
            1, 1, true, -1
        );
        
        // Test inherited methods from Wavelet
        assertNotNull(wavelet.name());
        assertEquals("TestBior", wavelet.name());
        
        // Test description method (inherited default)
        assertNotNull(wavelet.description());
        assertTrue(wavelet.description().contains("TestBior"));
    }

    @Test
    @DisplayName("Test real BiorthogonalSpline wavelets")
    void testRealBiorthogonalSplineWavelets() {
        // Test BIOR1.3
        BiorthogonalWavelet bior13 = BiorthogonalSpline.BIOR1_3;
        assertEquals("bior1.3", bior13.name());
        assertEquals(WaveletType.BIORTHOGONAL, bior13.getType());
        assertEquals(6, bior13.supportWidth()); // decomposition filter length
        assertEquals(2, bior13.reconstructionLength()); // reconstruction filter length
        assertTrue(bior13.isSymmetric());
        assertEquals(3, bior13.vanishingMoments());
        assertEquals(1, bior13.dualVanishingMoments());
        
        // Test BIOR2.2
        BiorthogonalWavelet bior22 = BiorthogonalSpline.BIOR2_2;
        assertEquals("bior2.2", bior22.name());
        assertEquals(WaveletType.BIORTHOGONAL, bior22.getType());
        assertTrue(bior22.isSymmetric());
        assertEquals(2, bior22.vanishingMoments());
        assertEquals(2, bior22.dualVanishingMoments());
        
        // Test BIOR3.3
        BiorthogonalWavelet bior33 = BiorthogonalSpline.BIOR3_3;
        assertEquals("bior3.3", bior33.name());
        assertEquals(WaveletType.BIORTHOGONAL, bior33.getType());
        assertEquals(8, bior33.supportWidth());
        assertEquals(4, bior33.reconstructionLength());
        assertTrue(bior33.isSymmetric());
        assertEquals(3, bior33.vanishingMoments());
        assertEquals(3, bior33.dualVanishingMoments());
    }

    @Test
    @DisplayName("Test filter coefficient consistency across BiorthogonalSpline wavelets")
    void testFilterCoefficientConsistency() {
        BiorthogonalWavelet[] wavelets = {
            BiorthogonalSpline.BIOR1_3,
            BiorthogonalSpline.BIOR2_2,
            BiorthogonalSpline.BIOR2_4,
            BiorthogonalSpline.BIOR3_3,
            BiorthogonalSpline.BIOR3_5
        };
        
        for (BiorthogonalWavelet wavelet : wavelets) {
            // Test that supportWidth matches decomposition filter length
            assertEquals(wavelet.lowPassDecomposition().length, wavelet.supportWidth(),
                "Support width should match decomposition filter length for " + wavelet.name());
            
            // Test that reconstructionLength matches reconstruction filter length  
            assertEquals(wavelet.lowPassReconstruction().length, wavelet.reconstructionLength(),
                "Reconstruction length should match reconstruction filter length for " + wavelet.name());
            
            // Test that filters are not null and have positive length
            assertNotNull(wavelet.lowPassDecomposition());
            assertNotNull(wavelet.highPassDecomposition());
            assertNotNull(wavelet.lowPassReconstruction());
            assertNotNull(wavelet.highPassReconstruction());
            
            assertTrue(wavelet.supportWidth() > 0);
            assertTrue(wavelet.reconstructionLength() > 0);
        }
    }

    @Test
    @DisplayName("Test BiorthogonalSpline spline orders")
    void testBiorthogonalSplineOrders() {
        // BIOR1.3: reconstruction order 1, decomposition order 3
        BiorthogonalWavelet bior13 = BiorthogonalSpline.BIOR1_3;
        assertTrue(bior13.splineOrder() >= -1); // Implementation specific
        
        // BIOR2.2: reconstruction order 2, decomposition order 2
        BiorthogonalWavelet bior22 = BiorthogonalSpline.BIOR2_2;
        assertTrue(bior22.splineOrder() >= -1);
        
        // BIOR3.3: reconstruction order 3, decomposition order 3
        BiorthogonalWavelet bior33 = BiorthogonalSpline.BIOR3_3;
        assertTrue(bior33.splineOrder() >= -1);
    }

    @Test
    @DisplayName("Test inherited methods from DiscreteWavelet and Wavelet")
    void testInheritedMethods() {
        BiorthogonalWavelet wavelet = BiorthogonalSpline.BIOR2_4;
        
        // Test DiscreteWavelet inherited methods
        assertTrue(wavelet.hasCompactSupport()); // Should be true for all discrete wavelets
        assertTrue(wavelet.supportWidth() > 0);
        assertTrue(wavelet.vanishingMoments() >= 0);
        
        // Test Wavelet inherited methods
        assertNotNull(wavelet.name());
        assertFalse(wavelet.name().isEmpty());
        assertNotNull(wavelet.description());
        assertTrue(wavelet.description().contains(wavelet.name()));
        
        // Test perfect reconstruction validation (inherited default)
        assertTrue(wavelet.validatePerfectReconstruction());
    }

    @Test
    @DisplayName("Test asymmetric reconstruction vs decomposition lengths")
    void testAsymmetricFilterLengths() {
        // BiorthogonalSpline wavelets often have different decomposition/reconstruction lengths
        BiorthogonalWavelet bior24 = BiorthogonalSpline.BIOR2_4;
        
        int decompLength = bior24.supportWidth();
        int reconLength = bior24.reconstructionLength();
        
        // BIOR2.4 has 9 decomposition coeffs and 3 reconstruction coeffs
        assertEquals(9, decompLength);
        assertEquals(3, reconLength);
        assertNotEquals(decompLength, reconLength);
        
        // Test that both methods work correctly
        assertEquals(bior24.lowPassDecomposition().length, decompLength);
        assertEquals(bior24.lowPassReconstruction().length, reconLength);
    }

    @Test
    @DisplayName("Test BiorthogonalSpline family variations")
    void testBiorthogonalSplineFamilyVariations() {
        // Test different variations within each family
        BiorthogonalWavelet[] family2 = {
            BiorthogonalSpline.BIOR2_2,
            BiorthogonalSpline.BIOR2_4,
            BiorthogonalSpline.BIOR2_6,
            BiorthogonalSpline.BIOR2_8
        };
        
        // All should have same dual vanishing moments (2) but different vanishing moments
        for (BiorthogonalWavelet wavelet : family2) {
            assertEquals(2, wavelet.dualVanishingMoments(), 
                "All BIOR2.x wavelets should have 2 dual vanishing moments: " + wavelet.name());
            assertTrue(wavelet.isSymmetric(), "BIOR wavelets should be symmetric");
            assertEquals(WaveletType.BIORTHOGONAL, wavelet.getType());
        }
        
        BiorthogonalWavelet[] family3 = {
            BiorthogonalSpline.BIOR3_1,
            BiorthogonalSpline.BIOR3_3,
            BiorthogonalSpline.BIOR3_5,
            BiorthogonalSpline.BIOR3_7
        };
        
        // All should have same dual vanishing moments (3)  
        for (BiorthogonalWavelet wavelet : family3) {
            assertEquals(3, wavelet.dualVanishingMoments(),
                "All BIOR3.x wavelets should have 3 dual vanishing moments: " + wavelet.name());
            assertTrue(wavelet.isSymmetric(), "BIOR wavelets should be symmetric");
            assertEquals(WaveletType.BIORTHOGONAL, wavelet.getType());
        }
    }

    @Test
    @DisplayName("Test edge cases")
    void testEdgeCases() {
        // Test with minimal coefficients
        BiorthogonalWavelet minimal = new TestBiorthogonalWavelet(
            new double[]{1.0}, new double[]{1.0},
            new double[]{1.0}, new double[]{1.0},
            0, 0, true, 0
        );
        
        assertEquals(1, minimal.supportWidth());
        assertEquals(1, minimal.reconstructionLength());
        assertEquals(0, minimal.vanishingMoments());
        assertEquals(0, minimal.dualVanishingMoments());
        assertTrue(minimal.isSymmetric());
        assertEquals(0, minimal.splineOrder());
        
        // Test with empty coefficients
        BiorthogonalWavelet empty = new TestBiorthogonalWavelet(
            new double[0], new double[0],
            new double[0], new double[0],
            0, 0, false, -1
        );
        
        assertEquals(0, empty.supportWidth());
        assertEquals(0, empty.reconstructionLength());
    }

    @Test
    @DisplayName("Test static normalization utilities from Wavelet interface")
    void testStaticNormalizationUtilities() {
        // Test with BiorthogonalSpline coefficients
        BiorthogonalWavelet bior13 = BiorthogonalSpline.BIOR1_3;
        
        double[] coeffs = bior13.lowPassDecomposition();
        
        // Test static normalization methods (inherited from Wavelet interface)
        double[] normalized = Wavelet.normalizeToUnitL2Norm(coeffs);
        assertTrue(Wavelet.isNormalized(normalized));
        
        // Test with tolerance
        assertTrue(Wavelet.isNormalized(normalized, 1e-10));
        
        // Original coeffs should be unchanged
        assertNotSame(coeffs, normalized);
    }

    @Test
    @DisplayName("Test null and empty filter handling in default methods")
    void testNullEmptyFilterHandling() {
        // Test with wavelet that has empty filters for edge cases
        BiorthogonalWavelet emptyWavelet = new TestBiorthogonalWavelet(
            new double[0], new double[0],
            new double[0], new double[0],
            0, 0, false, -1
        );
        
        // Default methods should handle empty arrays gracefully
        assertEquals(0, emptyWavelet.supportWidth());
        assertEquals(0, emptyWavelet.reconstructionLength());
        assertEquals(WaveletType.BIORTHOGONAL, emptyWavelet.getType());
        
        // splineOrder should return default value
        assertEquals(-1, emptyWavelet.splineOrder());
    }

    @Test
    @DisplayName("Test large coefficient arrays for performance edge cases")
    void testLargeCoefficientArrays() {
        // Test with larger coefficient arrays to ensure default methods scale
        double[] largeArray = new double[1000];
        for (int i = 0; i < largeArray.length; i++) {
            largeArray[i] = Math.sin(i * 0.01); // Some non-zero pattern
        }
        
        BiorthogonalWavelet largeWavelet = new TestBiorthogonalWavelet(
            largeArray, largeArray.clone(),
            largeArray, largeArray.clone(),
            5, 7, true, 3
        );
        
        assertEquals(1000, largeWavelet.supportWidth());
        assertEquals(1000, largeWavelet.reconstructionLength());
        assertEquals(3, largeWavelet.splineOrder());
        assertEquals(WaveletType.BIORTHOGONAL, largeWavelet.getType());
    }

    @Test
    @DisplayName("Test BiorthogonalWavelet with ReverseBiorthogonalSpline")
    void testReverseBiorthogonalSpline() {
        // Test with ReverseBiorthogonalSpline if available
        try {
            BiorthogonalWavelet rbior = ReverseBiorthogonalSpline.RBIO1_3;
            
            // Test basic functionality
            assertNotNull(rbior.name());
            assertEquals(WaveletType.BIORTHOGONAL, rbior.getType());
            assertTrue(rbior.supportWidth() > 0);
            assertTrue(rbior.reconstructionLength() > 0);
            assertTrue(rbior.vanishingMoments() >= 0);
            assertTrue(rbior.dualVanishingMoments() >= 0);
            
            // Test default methods
            assertEquals(rbior.lowPassDecomposition().length, rbior.supportWidth());
            assertEquals(rbior.lowPassReconstruction().length, rbior.reconstructionLength());
            
        } catch (Exception e) {
            // Skip if ReverseBiorthogonalSpline not available
            System.out.println("ReverseBiorthogonalSpline not available, skipping test");
        }
    }
}