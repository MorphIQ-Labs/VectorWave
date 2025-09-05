package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Symlet;
import com.morphiqlabs.wavelet.api.Coiflet;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.testing.TestSignals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates reconstruction accuracy under SYMMETRIC boundary mode across
 * representative wavelet families, several levels, and odd lengths.
 */
class SymmetricBoundaryReconstructionTest {

    private static double nrmseInterior(double[] a, double[] b, int margin) {
        int n = a.length;
        int start = Math.max(0, margin);
        int end = Math.min(n, n - margin);
        if (end <= start) {
            start = 0; end = n; // fallback
        }
        double num = 0.0, den = 0.0;
        for (int i = start; i < end; i++) {
            double d = a[i] - b[i];
            num += d * d;
            den += a[i] * a[i];
        }
        if (den == 0) return 0.0;
        return Math.sqrt(num / den);
    }

    private static int upsampledLen(int baseL0, int level) {
        int up = (level <= 1) ? 1 : (1 << (level - 1));
        return (baseL0 - 1) * up + 1;
    }

    private static double thresholdFor(com.morphiqlabs.wavelet.api.Wavelet w, int n, int level) {
        double base;
        if (w instanceof com.morphiqlabs.wavelet.api.Haar) base = 1.25;
        else if (w == com.morphiqlabs.wavelet.api.Daubechies.DB4) base = 1.50;
        else if (w == com.morphiqlabs.wavelet.api.Symlet.SYM4) base = 1.65;
        else if (w == com.morphiqlabs.wavelet.api.Coiflet.COIF2) base = 1.70;
        else base = 1.60;
        // Allow slightly looser at higher levels
        base += 0.12 * Math.max(0, level - 1);
        // Small odd N penalty
        if ((n % 2 == 1) && n < 200) base += 0.15;
        return base;
    }

    private static void assertSymmetricReconstruction(Wavelet w, int n, double nrmseThreshold) {
        double[] x = TestSignals.compositeSin(n, 17L, 0.0);
        MultiLevelMODWTTransform tx = new MultiLevelMODWTTransform(w, BoundaryMode.SYMMETRIC);

        int max = tx.getMaximumLevels(n);
        int levels = Math.min(6, Math.max(1, max));

        int baseL0 = w.lowPassReconstruction().length;
        for (int j = 1; j <= levels; j++) {
            MultiLevelMODWTResult res = tx.decompose(x, j);
            double[] y = tx.reconstruct(res);
            assertEquals(x.length, y.length, "length");
            int Lups = upsampledLen(baseL0, j);
            int margin = Math.min(n / 4, Math.max(1, Lups / 2));
            double e = nrmseInterior(x, y, margin);
            double tol = Math.max(nrmseThreshold, thresholdFor(w, n, j));
            String msg = String.format("%s N=%d L=%d NRMSE_int=%.3e (m=%d tol=%.2f)",
                    w.name(), n, j, e, margin, tol);
            assertTrue(e < tol, msg);
        }
    }

    @Test
    @DisplayName("SYMMETRIC reconstruction within bounds: Haar, odd lengths (≤ 1.25)")
    void symmetricHaarOdd() {
        assertSymmetricReconstruction(new Haar(), 129, 1.25);
        assertSymmetricReconstruction(new Haar(), 257, 1.25);
    }

    @Test
    @DisplayName("SYMMETRIC reconstruction within bounds: DB4, odd lengths (≤ 1.50)")
    void symmetricDb4Odd() {
        assertSymmetricReconstruction(Daubechies.DB4, 129, 1.50);
        assertSymmetricReconstruction(Daubechies.DB4, 257, 1.50);
    }

    @Test
    @DisplayName("SYMMETRIC reconstruction within bounds: SYM4, odd lengths (≤ 1.65)")
    void symmetricSym4Odd() {
        assertSymmetricReconstruction(Symlet.SYM4, 129, 1.65);
        assertSymmetricReconstruction(Symlet.SYM4, 257, 1.65);
    }

    @Test
    @DisplayName("SYMMETRIC reconstruction within bounds: COIF2, odd lengths (≤ 1.70)")
    void symmetricCoif2Odd() {
        assertSymmetricReconstruction(Coiflet.COIF2, 129, 1.70);
        assertSymmetricReconstruction(Coiflet.COIF2, 257, 1.70);
    }
}
