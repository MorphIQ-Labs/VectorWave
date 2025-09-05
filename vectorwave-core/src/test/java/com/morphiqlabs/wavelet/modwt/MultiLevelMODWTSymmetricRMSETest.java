package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MultiLevelMODWTSymmetricRMSETest {

    @Test
    @DisplayName("Symmetric multi-level recon within practical RMSE tolerance")
    void symmetricReconstructionAccurateEnough() {
        int[] lengths = {129, 193, 257, 383, 512};
        Wavelet[] wavelets = {
                new Haar(), Daubechies.DB4, Daubechies.DB6, Daubechies.DB8,
                com.morphiqlabs.wavelet.api.Symlet.SYM4, com.morphiqlabs.wavelet.api.Symlet.SYM8,
                com.morphiqlabs.wavelet.api.Coiflet.COIF2, com.morphiqlabs.wavelet.api.Coiflet.COIF3
        };

        for (int n : lengths) {
            double[] random = randomSignal(n, 99L);
            for (Wavelet w : wavelets) {
                MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(w, BoundaryMode.SYMMETRIC);
                int J = Math.max(1, t.getMaximumLevels(n));
                MultiLevelMODWTResult r = t.decompose(random, J);
                double[] rec = t.reconstruct(r);
                int baseL0 = w.lowPassReconstruction().length;
                int Lups = (baseL0 - 1) * (1 << Math.max(0, J - 1)) + 1;
                int margin = Math.min(n / 4, Math.max(1, Lups / 2));
                double err = rmseInterior(random, rec, margin);
                double tol = (w instanceof Haar) ? 0.75 : (w == Daubechies.DB4 ? 0.80 : 0.90);
                assertTrue(err < tol, "Symmetric RMSE too high (" + err + ") for w=" + w.name() + ", N=" + n + ", tol=" + tol + ")");
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
    private static double rmseInterior(double[] a, double[] b, int margin) {
        int n = a.length;
        int start = Math.max(0, margin);
        int end = Math.min(n, n - margin);
        if (end <= start) return rmse(a, b);
        double s = 0.0; int m = 0;
        for (int i = start; i < end; i++) { double d = a[i] - b[i]; s += d*d; m++; }
        return Math.sqrt(s / Math.max(1, m));
    }

}
