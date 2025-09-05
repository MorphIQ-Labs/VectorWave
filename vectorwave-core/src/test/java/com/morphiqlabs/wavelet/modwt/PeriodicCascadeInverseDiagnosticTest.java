package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Per-level periodic reconstruction diagnostics for the MODWT cascade inverse.
 * Focuses on small-N Haar and DB4 to catch centering/orientation issues early.
 */
class PeriodicCascadeInverseDiagnosticTest {

    private static double rmse(double[] a, double[] b) {
        assertEquals(a.length, b.length, "length mismatch");
        double s = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            s += d * d;
        }
        return Math.sqrt(s / a.length);
    }

    private static double[] seq(int n) {
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = i + 1; // simple non-symmetric ramp
        return x;
    }

    @Test
    @DisplayName("Periodic cascade: perfect recon, Haar small N")
    void periodicCascadePerfectReconHaar() {
        Wavelet w = new Haar();
        BoundaryMode mode = BoundaryMode.PERIODIC;
        int[] Ns = {4, 5, 6, 7, 8, 12, 16};
        for (int n : Ns) {
            double[] x = seq(n);
            MultiLevelMODWTTransform m = new MultiLevelMODWTTransform(w, mode);
            int J = m.getMaximumLevels(n);
            if (J == 0) continue;
            MultiLevelMODWTResult r = m.decompose(x, J);
            double[] xr = m.reconstruct(r);
            double e = rmse(x, xr);
            assertTrue(e < 1e-8, "RMSE too high for N=" + n + ": " + e);
        }
    }

    @Test
    @DisplayName("Periodic cascade: perfect recon, DB4 small N")
    void periodicCascadePerfectReconDB4() {
        Wavelet w = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.PERIODIC;
        int[] Ns = {8, 9, 10, 12, 16, 24, 32};
        for (int n : Ns) {
            double[] x = seq(n);
            MultiLevelMODWTTransform m = new MultiLevelMODWTTransform(w, mode);
            int J = m.getMaximumLevels(n);
            if (J == 0) continue; // too short for DB4
            MultiLevelMODWTResult r = m.decompose(x, J);
            double[] xr = m.reconstruct(r);
            double e = rmse(x, xr);
            assertTrue(e < 1e-8, "RMSE too high for N=" + n + ": " + e);
        }
    }
}
