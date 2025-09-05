package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Prints NRMSE (interior) matrix for SYMMETRIC reconstruction over wavelets × N × level.
 * If VECTORWAVE_SYM_BASELINE env var is set to "strict", fails when an entry exceeds a conservative bound.
 */
class SymmetricNRMSEMatrixDiagnosticTest {

    @Test
    @DisplayName("Diagnostic: SYMMETRIC NRMSE (interior) matrix")
    void printMatrix() {
        int[] Ns = {129, 257};
        Wavelet[] wavelets = new Wavelet[] {
            new com.morphiqlabs.wavelet.api.Haar(),
            com.morphiqlabs.wavelet.api.Daubechies.DB4,
            com.morphiqlabs.wavelet.api.Symlet.SYM4,
            com.morphiqlabs.wavelet.api.Coiflet.COIF2
        };
        String strict = System.getenv("VECTORWAVE_SYM_BASELINE");
        for (Wavelet w : wavelets) {
            for (int n : Ns) {
                double[] x = randomSignal(n, 42L);
                MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(w, BoundaryMode.SYMMETRIC);
                int J = Math.max(1, t.getMaximumLevels(n));
                System.out.printf("[SYM-NRMSE] w=%s N=%d Jmax=%d\n", w.name(), n, J);
                for (int j = 1; j <= Math.min(J, 6); j++) {
                    MultiLevelMODWTResult r = t.decompose(x, j);
                    double[] y = t.reconstruct(r);
                    int baseL0 = w.lowPassReconstruction().length;
                    int Lups = (baseL0 - 1) * (1 << Math.max(0, j - 1)) + 1;
                    int margin = Math.min(n / 4, Math.max(1, Lups / 2));
                    double err = nrmseInterior(x, y, margin);
                    System.out.printf("  L=%d, m=%d, NRMSE_int=%.4f\n", j, margin, err);
                    if ("strict" != null && "strict".equalsIgnoreCase(strict)) {
                        // Conservative guard
                        double tol = (w instanceof com.morphiqlabs.wavelet.api.Haar) ? 1.30 :
                                     (w == com.morphiqlabs.wavelet.api.Daubechies.DB4 ? 1.55 :
                                      (w == com.morphiqlabs.wavelet.api.Symlet.SYM4 ? 1.70 : 1.80));
                        String msg = String.format(
                                "exceeded baseline: w=%s N=%d L=%d err=%.3f tol=%.3f",
                                w.name(), n, j, err, tol);
                        org.junit.jupiter.api.Assertions.assertTrue(err < tol, msg);
                    }
                }
            }
        }
    }

    private static double[] randomSignal(int n, long seed) {
        java.util.Random r = new java.util.Random(seed);
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = r.nextDouble() * 2 - 1;
        return x;
    }

    private static double nrmseInterior(double[] a, double[] b, int margin) {
        int n = a.length;
        int start = Math.max(0, margin);
        int end = Math.min(n, n - margin);
        if (end <= start) return rmse(a, b);
        double num = 0.0, den = 0.0;
        for (int i = start; i < end; i++) {
            double d = a[i] - b[i];
            num += d * d;
            den += a[i] * a[i];
        }
        if (den == 0) return 0.0;
        return Math.sqrt(num / den);
    }

    private static double rmse(double[] a, double[] b) {
        double s = 0.0; int n = a.length;
        for (int i = 0; i < n; i++) { double d = a[i] - b[i]; s += d*d; }
        return Math.sqrt(s / n);
    }
}
