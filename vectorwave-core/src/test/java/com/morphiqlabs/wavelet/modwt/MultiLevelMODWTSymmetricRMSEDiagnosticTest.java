package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import java.util.Random;

// Diagnostic harness to sweep small τ_j deltas for symmetric mode. Disabled by default.
@Disabled("Diagnostic sweep; enable locally when tuning symmetric τ_j deltas")
class MultiLevelMODWTSymmetricRMSEDiagnosticTest {

    @Test
    void sweepTauDeltas() {
        int[] lengths = {129, 257, 512};
        Wavelet[] wavelets = { new Haar(), Daubechies.DB4 };

        int[] deltas = {-1, 0, 1};

        for (int dA : deltas) {
            for (int dD : deltas) {
                double worst = 0.0;
                for (Wavelet w : wavelets) {
                    for (int n : lengths) {
                        double[] x = randomSignal(n, 42L);
                        MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(w, BoundaryMode.SYMMETRIC);
                        // Set deltas via reflection (package-private fields)
                        try {
                            var fldA = MultiLevelMODWTTransform.class.getDeclaredField("SYM_TAU_DELTA_APPROX");
                            var fldD = MultiLevelMODWTTransform.class.getDeclaredField("SYM_TAU_DELTA_DETAIL");
                            fldA.setAccessible(true);
                            fldD.setAccessible(true);
                            fldA.setInt(null, dA);
                            fldD.setInt(null, dD);
                        } catch (Exception ignored) {}

                        int J = Math.max(1, t.getMaximumLevels(n));
                        MultiLevelMODWTResult r = t.decompose(x, J);
                        double[] rec = t.reconstruct(r);
                        double err = rmse(x, rec);
                        if (err > worst) worst = err;
                    }
                }
                System.out.printf("SYM deltas (approx=%d, detail=%d): worst RMSE=%.6f%n", dA, dD, worst);
            }
        }
    }

    private static double[] randomSignal(int n, long seed) {
        Random r = new Random(seed);
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = r.nextDouble() * 2 - 1;
        return x;
    }

    private static double rmse(double[] a, double[] b) {
        double s = 0.0; int n = a.length;
        for (int i = 0; i < n; i++) { double d = a[i] - b[i]; s += d*d; }
        return Math.sqrt(s / n);
    }
}
