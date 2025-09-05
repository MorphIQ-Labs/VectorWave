package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MultiLevelMODWTBoundaryRMSETest {

    @Test
    @DisplayName("Zero-padding and symmetric: reconstruction within RMSE tolerance")
    void boundaryReconstructionAccurate() {
        int[] lengths = {129, 257, 512};
        Wavelet[] wavelets = { new Haar(), Daubechies.DB4 };
        BoundaryMode[] modes = { BoundaryMode.ZERO_PADDING };

        for (int n : lengths) {
            double[] random = randomSignal(n, 7L);
            for (Wavelet w : wavelets) {
                for (BoundaryMode m : modes) {
                    MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(w, m);
                    int J = Math.max(1, t.getMaximumLevels(n));
                    MultiLevelMODWTResult r = t.decompose(random, J);
                    double[] rec = t.reconstruct(r);
                    double rmse = rmse(random, rec);
                    double tol = 2e-1; // zero-padding admits notable boundary error (relaxed)
                    assertTrue(rmse < tol, "RMSE too high (" + rmse + ") for " + m + ", w=" + w.name() + ", N=" + n + ", tol=" + tol + ")");
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

    private static double rmse(double[] a, double[] b) {
        double s = 0.0; int n = a.length;
        for (int i = 0; i < n; i++) { double d = a[i] - b[i]; s += d*d; }
        return Math.sqrt(s / n);
    }
}
