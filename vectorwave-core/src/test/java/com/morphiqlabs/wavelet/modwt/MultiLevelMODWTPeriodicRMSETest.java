package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MultiLevelMODWTPeriodicRMSETest {

    @Test
    @DisplayName("Periodic multi-level recon achieves minimal circular RMSE < 1e-10")
    void periodicReconstructionIsAccurate() {
        int[] lengths = {129, 257, 512, 1000};
        Wavelet[] wavelets = { new Haar(), Daubechies.DB4, Daubechies.DB8 };

        for (int n : lengths) {
            double[] sine = sineWave(n, 7.0);
            double[] random = randomSignal(n, 1234L);

            for (Wavelet w : wavelets) {
                MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(w, BoundaryMode.PERIODIC);
                int J = Math.max(1, t.getMaximumLevels(n));

                MultiLevelMODWTResult r1 = t.decompose(sine, J);
                double[] x1 = t.reconstruct(r1);
                assertTrue(minCircularRMSE(sine, x1) < 1e-10,
                    "sine RMSE too high (N=" + n + ", w=" + w.name() + ")");

                MultiLevelMODWTResult r2 = t.decompose(random, J);
                double[] x2 = t.reconstruct(r2);
                assertTrue(minCircularRMSE(random, x2) < 1e-10,
                    "random RMSE too high (N=" + n + ", w=" + w.name() + ")");
            }
        }
    }

    private static double[] sineWave(int n, double cycles) {
        double[] s = new double[n];
        for (int i = 0; i < n; i++) s[i] = Math.sin(2.0 * Math.PI * cycles * i / n);
        return s;
    }

    private static double[] randomSignal(int n, long seed) {
        Random r = new Random(seed);
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = r.nextDouble() * 2 - 1;
        return x;
    }

    private static double minCircularRMSE(double[] a, double[] b) {
        int n = a.length; double min = Double.POSITIVE_INFINITY;
        for (int k = 0; k < n; k++) {
            double s = 0.0;
            for (int i = 0; i < n; i++) {
                double d = a[i] - b[(i + k) % n];
                s += d * d;
            }
            double rmse = Math.sqrt(s / n);
            if (rmse < min) min = rmse;
        }
        return min;
    }
}

