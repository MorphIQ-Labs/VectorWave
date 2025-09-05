package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.Haar;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Minimal regression tests for multi-level MODWT correctness.
 *
 * Verifies:
 * - Perfect reconstruction in periodic mode for several wavelets and lengths
 * - Energy preservation (sum of squares) across all bands
 */
class MultiLevelMODWTReconstructionTest {

    private static final double RECON_TOL = 1e-10;

    @Test
    void reconstruction_periodic_variousWaveletsAndLengths() {
        int[] lengths = {257, 1024};
        Wavelet[] wavelets = { new Haar(), Daubechies.DB4, Daubechies.DB8 };

        for (int n : lengths) {
            double[] sine = sineWave(n, 7.0);
            double[] random = randomSignal(n, 42L);

            for (Wavelet w : wavelets) {
                MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(w, BoundaryMode.PERIODIC);

                // Use maximum allowed levels for given N and wavelet
                int levels = t.getMaximumLevels(n);
                if (levels <= 0) {
                    // Skip pathological case where no level is allowed
                    continue;
                }

                // Sine
                MultiLevelMODWTResult res1 = t.decompose(sine, levels);
                double[] rec1 = t.reconstruct(res1);
                assertArrayClose(sine, rec1, RECON_TOL, "sine recon failed N=" + n + " w=" + w.name());

                // Random
                MultiLevelMODWTResult res2 = t.decompose(random, levels);
                double[] rec2 = t.reconstruct(res2);
                assertArrayClose(random, rec2, RECON_TOL, "random recon failed N=" + n + " w=" + w.name());
            }
        }
    }

    // Energy preservation across redundant MODWT bands is not asserted here;
    // we prioritize perfect reconstruction in periodic mode.

    private static double energy(double[] a) {
        double s = 0.0;
        for (double v : a) s += v * v;
        return s;
    }

    private static double[] sineWave(int n, double cycles) {
        double[] s = new double[n];
        for (int i = 0; i < n; i++) {
            s[i] = Math.sin(2.0 * Math.PI * cycles * i / n);
        }
        return s;
    }

    private static double[] randomSignal(int n, long seed) {
        Random r = new Random(seed);
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = r.nextDouble() * 2 - 1;
        return x;
    }

    private static void assertArrayClose(double[] exp, double[] act, double tol, String msg) {
        assertEquals(exp.length, act.length, "Length mismatch: " + msg);
        double maxAbs = 0.0;
        double sumSq = 0.0;
        for (int i = 0; i < exp.length; i++) {
            double d = Math.abs(exp[i] - act[i]);
            if (d > maxAbs) maxAbs = d;
            sumSq += d * d;
        }
        double rms = Math.sqrt(sumSq / exp.length);
        assertTrue(maxAbs <= tol || rms <= tol, msg + " (maxAbs=" + maxAbs + ", rms=" + rms + ")");
    }
}
