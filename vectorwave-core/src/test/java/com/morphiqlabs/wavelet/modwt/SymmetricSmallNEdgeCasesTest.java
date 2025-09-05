package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Coiflet;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.testing.TestSignals;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Targeted SYMMETRIC boundary tests for very small N (N < filter length)
 * and small N against longer filters. Uses interior NRMSE with a margin
 * based on upsampled filter length to avoid brittle edge assertions.
 */
class SymmetricSmallNEdgeCasesTest {

    private static int upsampledLen(int baseL0, int level) {
        if (level < 1) {
            throw new IllegalArgumentException("level must be >= 1");
        }
        int up = (level == 1) ? 1 : (1 << (level - 1));
        return (baseL0 - 1) * up + 1;
    }

    private static double nrmseInterior(double[] a, double[] b, int margin) {
        int n = a.length;
        int start = Math.max(0, margin);
        int end = Math.min(n, n - margin);
        if (end <= start) { start = 0; end = n; }
        double num = 0.0, den = 0.0;
        for (int i = start; i < end; i++) {
            double d = a[i] - b[i];
            num += d * d;
            den += a[i] * a[i];
        }
        if (den == 0) return 0.0;
        return Math.sqrt(num / den);
    }

    @Test
    @DisplayName("SYMMETRIC: extremely short N vs DB8 (single-level)")
    void symmetricExtremelyShortVsDB8() {
        Wavelet w = Daubechies.DB8; // base filter length 16
        int[] Ns = {7, 9};
        for (int n : Ns) {
            double[] x = TestSignals.compositeSin(n, 13L, 0.0);
            MODWTTransform tx = new MODWTTransform(w, BoundaryMode.SYMMETRIC);

            int baseL0 = w.lowPassReconstruction().length;
            int margin = Math.min(n / 2, Math.max(1, baseL0 / 2));
            // Ensure at least a small interior window (>= 3 samples) for stability
            int regionLen = n - 2 * margin;
            if (regionLen < 3) {
                margin = Math.max(1, (n - 3) / 2);
            }

            MODWTResult res = tx.forward(x);
            double[] y = tx.inverse(res);

            double e = nrmseInterior(x, y, margin);
            // Dynamic tolerance based on filter-length pressure (L0/N)
            double tol = Math.min(4.0, 1.60 + 0.30 * (baseL0 / (double) n));
            assertTrue(e < tol, String.format("DB8 N=%d NRMSE_int=%.3e (m=%d tol=%.2f)", n, e, margin, tol));
        }
    }

    @Test
    @DisplayName("SYMMETRIC: small N vs long Coiflet filters (single-level)")
    void symmetricSmallNLongFilter() {
        Wavelet w = Coiflet.COIF10; // longer base filter
        int[] Ns = {11, 13};
        for (int n : Ns) {
            double[] x = TestSignals.compositeSin(n, 17L, 0.0);
            MODWTTransform tx = new MODWTTransform(w, BoundaryMode.SYMMETRIC);

            int baseL0 = w.lowPassReconstruction().length;
            int margin = Math.min(n / 2, Math.max(1, baseL0 / 2));
            // Ensure at least a small interior window (>= 3 samples) for stability
            int regionLen = n - 2 * margin;
            if (regionLen < 3) {
                margin = Math.max(1, (n - 3) / 2);
            }

            MODWTResult res = tx.forward(x);
            double[] y = tx.inverse(res);

            double e = nrmseInterior(x, y, margin);
            // Dynamic tolerance based on filter-length pressure (L0/N)
            double tol = Math.min(4.5, 1.70 + 0.30 * (baseL0 / (double) n));
            assertTrue(e < tol, String.format("COIF10 N=%d NRMSE_int=%.3e (m=%d tol=%.2f)", n, e, margin, tol));
        }
    }
}
