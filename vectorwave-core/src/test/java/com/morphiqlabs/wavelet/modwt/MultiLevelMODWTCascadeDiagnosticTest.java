package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

@Disabled("Diagnostic: enable locally to analyze per-level cascade alignment")
class MultiLevelMODWTCascadeDiagnosticTest {
    @Test
    void inspectPerLevelReconstruction_Haar() {
        Wavelet w = new Haar();
        int[] Ns = {8, 16, 32};
        for (int N : Ns) {
            double[] x = randomSignal(N, 7L);
            MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(w, BoundaryMode.PERIODIC);
            int J = Math.min(3, t.getMaximumLevels(N));
            MultiLevelMODWTResult r = t.decompose(x, J);
            // Step-wise cascade: reconstruct v(j-1) from (vj, wj)
            double[] v = r.getApproximationCoeffs().clone();
            for (int level = J; level >= 1; level--) {
                double[] wj = r.getDetailCoeffsAtLevel(level);
                double[] vPrev = invokeSingleLevelInverse(t, v, wj, level);
                double err = rmse(x, vPrev); // compare to original only at final level
                System.out.printf("N=%d level=%d rmse-to-x=%.6f first=%.6f%n", N, level, err, vPrev[0]);
                v = vPrev;
            }
            System.out.printf("N=%d final rmse=%.6f first=%.6f%n", N, rmse(x, v), v[0]);
        }
    }

    private static double[] invokeSingleLevelInverse(MultiLevelMODWTTransform t,
                                                     double[] vj, double[] wj, int level) {
        try {
            var m = MultiLevelMODWTTransform.class.getDeclaredMethod(
                "reconstructSingleLevel", double[].class, double[].class, int.class);
            m.setAccessible(true);
            return (double[]) m.invoke(t, vj, wj, level);
        } catch (Exception e) {
            throw new RuntimeException(e);
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

