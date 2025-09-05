package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class ParallelVsSequentialEquivalenceTest {

    @Test
    @DisplayName("Parallel vs sequential: coefficients and recon match (periodic/zero)")
    void parallelSequentialMatch() {
        int[] lengths = {256, 500};
        Wavelet[] wavelets = { new Haar(), Daubechies.DB4 };
        BoundaryMode[] modes = { BoundaryMode.PERIODIC, BoundaryMode.ZERO_PADDING };

        for (int n : lengths) {
            double[] x = randomSignal(n, 123);
            for (Wavelet w : wavelets) {
                for (BoundaryMode m : modes) {
                    MultiLevelMODWTTransform seq = new MultiLevelMODWTTransform(w, m);
                    int J = Math.max(1, seq.getMaximumLevels(n));
                    MultiLevelMODWTResult rs = seq.decompose(x, J);
                    double[] xs = seq.reconstruct(rs);

                    ParallelMultiLevelMODWT par = new ParallelMultiLevelMODWT();
                    MultiLevelMODWTResult rp = par.decompose(x, w, m, J);
                    double[] xp = seq.reconstruct(rp);

                    // Per-level coefficients close
                    for (int j = 1; j <= J; j++) {
                        assertArrayClose(rs.getDetailCoeffsAtLevel(j), rp.getDetailCoeffsAtLevel(j), 1e-12);
                    }
                    // Reconstructed close (use circular RMSE for periodic, absolute RMSE otherwise)
                    if (m == BoundaryMode.PERIODIC) {
                        assertTrue(minCircularRMSE(xs, xp) < 1e-12);
                    } else {
                        assertTrue(rmse(xs, xp) < 1e-12);
                    }
                }
            }
        }
    }

    private static double[] randomSignal(int n, long seed) {
        Random r = new Random(seed);
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = r.nextDouble() * 2 - 1;
        return x;
    }

    private static void assertArrayClose(double[] a, double[] b, double tol) {
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            if (Math.abs(a[i] - b[i]) > tol) {
                fail("arrays differ at index " + i + ": " + a[i] + " vs " + b[i]);
            }
        }
    }

    private static double rmse(double[] a, double[] b) {
        double s = 0.0; int n = a.length;
        for (int i = 0; i < n; i++) { double d = a[i] - b[i]; s += d*d; }
        return Math.sqrt(s / n);
    }

    private static double minCircularRMSE(double[] a, double[] b) {
        int n = a.length; double min = Double.POSITIVE_INFINITY;
        for (int k = 0; k < n; k++) {
            double s = 0.0;
            for (int i = 0; i < n; i++) {
                double d = a[i] - b[(i + k) % n]; s += d*d;
            }
            double r = Math.sqrt(s / n);
            if (r < min) min = r;
        }
        return min;
    }
}

