package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional coverage tests for BiorthogonalWavelet interface.
 * Focuses on covering all default methods and edge cases.
 */
class BiorthogonalWaveletCoverageTest {

    /**
     * Minimal implementation for testing default methods.
     */
    private static class MinimalBiorthogonalWavelet implements BiorthogonalWavelet {
        private final double[] lowDecomp = {0.5, 0.5};
        private final double[] highDecomp = {0.5, -0.5};
        private final double[] lowRecon = {0.5, 0.5};
        private final double[] highRecon = {-0.5, 0.5};
        
        @Override
        public String name() {
            return "MinimalBior";
        }
        
        @Override
        public double[] lowPassDecomposition() {
            return lowDecomp.clone();
        }
        
        @Override
        public double[] highPassDecomposition() {
            return highDecomp.clone();
        }
        
        @Override
        public double[] lowPassReconstruction() {
            return lowRecon.clone();
        }
        
        @Override
        public double[] highPassReconstruction() {
            return highRecon.clone();
        }
        
        @Override
        public int vanishingMoments() {
            return 1;
        }
        
        @Override
        public int dualVanishingMoments() {
            return 1;
        }
        
        @Override
        public boolean isSymmetric() {
            return true;
        }
    }
    
    /**
     * Custom implementation with spline order.
     */
    private static class SplineBiorthogonalWavelet extends MinimalBiorthogonalWavelet {
        private final int splineOrder;
        
        SplineBiorthogonalWavelet(int splineOrder) {
            this.splineOrder = splineOrder;
        }
        
        @Override
        public String name() {
            return "SplineBior" + splineOrder;
        }
        
        @Override
        public int splineOrder() {
            return splineOrder;
        }
    }
    
    /**
     * Asymmetric biorthogonal wavelet for testing.
     */
    private static class AsymmetricBiorthogonalWavelet extends MinimalBiorthogonalWavelet {
        @Override
        public String name() {
            return "AsymmetricBior";
        }
        
        @Override
        public boolean isSymmetric() {
            return false;
        }
        
        @Override
        public int dualVanishingMoments() {
            return 2; // Different from primary
        }
    }
    
    @Test
    @DisplayName("Should return correct default spline order (-1)")
    void testDefaultSplineOrder() {
        BiorthogonalWavelet wavelet = new MinimalBiorthogonalWavelet();
        assertEquals(-1, wavelet.splineOrder(), 
            "Default spline order should be -1");
    }
    
    @Test
    @DisplayName("Should return custom spline order when overridden")
    void testCustomSplineOrder() {
        BiorthogonalWavelet wavelet3 = new SplineBiorthogonalWavelet(3);
        assertEquals(3, wavelet3.splineOrder(), 
            "Should return custom spline order 3");
        
        BiorthogonalWavelet wavelet5 = new SplineBiorthogonalWavelet(5);
        assertEquals(5, wavelet5.splineOrder(), 
            "Should return custom spline order 5");
    }
    
    @Test
    @DisplayName("Should return correct reconstruction length")
    void testReconstructionLength() {
        BiorthogonalWavelet wavelet = new MinimalBiorthogonalWavelet();
        assertEquals(2, wavelet.reconstructionLength(),
            "Reconstruction length should match lowPassReconstruction filter length");
    }
    
    @Test
    @DisplayName("Should return BIORTHOGONAL as wavelet type")
    void testWaveletType() {
        BiorthogonalWavelet wavelet = new MinimalBiorthogonalWavelet();
        assertEquals(WaveletType.BIORTHOGONAL, wavelet.getType(),
            "Type should be BIORTHOGONAL");
    }
    
    @Test
    @DisplayName("Should handle symmetric wavelets correctly")
    void testSymmetricWavelet() {
        BiorthogonalWavelet symmetric = new MinimalBiorthogonalWavelet();
        assertTrue(symmetric.isSymmetric(), 
            "Should be symmetric");
        
        BiorthogonalWavelet asymmetric = new AsymmetricBiorthogonalWavelet();
        assertFalse(asymmetric.isSymmetric(), 
            "Should not be symmetric");
    }
    
    @Test
    @DisplayName("Should handle dual vanishing moments")
    void testDualVanishingMoments() {
        BiorthogonalWavelet wavelet = new MinimalBiorthogonalWavelet();
        assertEquals(1, wavelet.dualVanishingMoments(),
            "Should have 1 dual vanishing moment");
        
        BiorthogonalWavelet asymmetric = new AsymmetricBiorthogonalWavelet();
        assertEquals(2, asymmetric.dualVanishingMoments(),
            "Should have 2 dual vanishing moments");
        assertNotEquals(asymmetric.vanishingMoments(), asymmetric.dualVanishingMoments(),
            "Dual vanishing moments can differ from primary");
    }
    
    @Test
    @DisplayName("Should test all interface methods together")
    void testComprehensiveInterface() {
        BiorthogonalWavelet wavelet = new SplineBiorthogonalWavelet(4);
        
        // Test all methods
        assertNotNull(wavelet.name());
        assertNotNull(wavelet.lowPassDecomposition());
        assertNotNull(wavelet.highPassDecomposition());
        assertNotNull(wavelet.lowPassReconstruction());
        assertNotNull(wavelet.highPassReconstruction());
        assertTrue(wavelet.vanishingMoments() > 0);
        assertTrue(wavelet.dualVanishingMoments() > 0);
        assertEquals(4, wavelet.splineOrder());
        assertTrue(wavelet.isSymmetric());
        assertEquals(2, wavelet.reconstructionLength());
        assertEquals(WaveletType.BIORTHOGONAL, wavelet.getType());
    }
    
    @Test
    @DisplayName("Should handle varying reconstruction filter lengths")
    void testVaryingReconstructionLengths() {
        // Test with different reconstruction filter lengths
        BiorthogonalWavelet customWavelet = new BiorthogonalWavelet() {
            @Override
            public String name() { return "CustomBior"; }
            
            @Override
            public double[] lowPassDecomposition() {
                return new double[]{0.25, 0.25, 0.25, 0.25};
            }
            
            @Override
            public double[] highPassDecomposition() {
                return new double[]{0.5, -0.5};
            }
            
            @Override
            public double[] lowPassReconstruction() {
                // Different length from decomposition
                return new double[]{0.2, 0.2, 0.2, 0.2, 0.2};
            }
            
            @Override
            public double[] highPassReconstruction() {
                return new double[]{-0.3, 0.3, -0.3};
            }
            
            @Override
            public int vanishingMoments() { return 2; }
            
            @Override
            public int dualVanishingMoments() { return 3; }
            
            @Override
            public boolean isSymmetric() { return false; }
        };
        
        assertEquals(5, customWavelet.reconstructionLength(),
            "Should return correct reconstruction length for custom filter");
        assertNotEquals(customWavelet.lowPassDecomposition().length, 
                       customWavelet.reconstructionLength(),
            "Reconstruction length can differ from decomposition length");
    }
    
    @Test
    @DisplayName("Should test edge cases for spline order")
    void testSplineOrderEdgeCases() {
        // Zero spline order
        BiorthogonalWavelet zeroSpline = new SplineBiorthogonalWavelet(0);
        assertEquals(0, zeroSpline.splineOrder());
        
        // Negative spline order (for non-spline wavelets)
        BiorthogonalWavelet nonSpline = new SplineBiorthogonalWavelet(-1);
        assertEquals(-1, nonSpline.splineOrder());
        
        // Large spline order
        BiorthogonalWavelet largeSpline = new SplineBiorthogonalWavelet(10);
        assertEquals(10, largeSpline.splineOrder());
    }
}