package com.morphiqlabs.wavelet.verification;

import com.morphiqlabs.wavelet.api.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Wavelet.validatePerfectReconstruction() implementation (Issue 013).
 */
class ValidatePerfectReconstructionTest {

    @Test
    @DisplayName("Orthogonal wavelets validate PR: Haar, DB4, SYM4, COIF2, DMEY")
    void testOrthogonalValidatePR() {
        Wavelet[] ws = new Wavelet[]{
            new Haar(), Daubechies.DB4, Symlet.SYM4, Coiflet.COIF2, DiscreteMeyer.DMEY
        };
        for (Wavelet w : ws) {
            assertTrue(w.validatePerfectReconstruction(),
                () -> w.name() + " should satisfy PR validation");
        }
    }

    @Test
    @DisplayName("Biorthogonal and Reverse-Biorthogonal validate PR: BIOR1.3, BIOR4.4, RBIO4.4")
    void testBiorthogonalValidatePR() {
        Wavelet[] ws = new Wavelet[]{
            BiorthogonalSpline.BIOR1_3, BiorthogonalSpline.BIOR4_4, ReverseBiorthogonalSpline.RBIO4_4
        };
        for (Wavelet w : ws) {
            assertTrue(w.validatePerfectReconstruction(),
                () -> w.name() + " should satisfy PR validation");
        }
    }

    @Test
    @DisplayName("Perturbed orthogonal wavelet fails PR validation")
    void testPerturbedOrthogonalFails() {
        // Start from DB4 and slightly perturb a coefficient; create a new Daubechies instance
        double[] base = Daubechies.DB4.lowPassDecomposition();
        double[] modified = base.clone();
        modified[0] += 1e-3;
        Wavelet bad = new Daubechies("db4-bad", modified, 4);
        assertFalse(bad.validatePerfectReconstruction(),
            "Perturbed orthogonal wavelet should fail PR validation");
    }

    @Test
    @DisplayName("Perturbed biorthogonal wavelet fails PR validation")
    void testPerturbedBiorthogonalFails() {
        Wavelet bad = new BadBiorthogonal(BiorthogonalSpline.BIOR4_4, 0, 1e-3);
        assertFalse(bad.validatePerfectReconstruction(),
            "Perturbed biorthogonal wavelet should fail PR validation");
    }

    // Helper: wrapper that perturbs a biorthogonal wavelet's low-pass decomp coeff
    private static final class BadBiorthogonal implements BiorthogonalWavelet {
        private final BiorthogonalWavelet base;
        private final double[] hd;
        BadBiorthogonal(BiorthogonalWavelet base, int idx, double delta) {
            this.base = base;
            double[] orig = base.lowPassDecomposition();
            this.hd = orig.clone();
            this.hd[Math.min(Math.max(0, idx), hd.length - 1)] += delta;
        }
        @Override public String name() { return base.name() + "_bad"; }
        @Override public String description() { return base.description(); }
        @Override public WaveletType getType() { return base.getType(); }
        @Override public double[] lowPassDecomposition() { return hd.clone(); }
        @Override public double[] highPassDecomposition() { return base.highPassDecomposition(); }
        @Override public double[] lowPassReconstruction() { return base.lowPassReconstruction(); }
        @Override public double[] highPassReconstruction() { return base.highPassReconstruction(); }
        @Override public int vanishingMoments() { return base.vanishingMoments(); }
        @Override public int dualVanishingMoments() { return base.dualVanishingMoments(); }
        @Override public int splineOrder() { return base.splineOrder(); }
        @Override public boolean isSymmetric() { return base.isSymmetric(); }
        @Override public int reconstructionLength() { return base.reconstructionLength(); }
    }
}
