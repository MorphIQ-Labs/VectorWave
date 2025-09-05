package com.morphiqlabs.wavelet.swt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class VectorWaveSwtAdapterLifecycleTest {

    @Test
    @DisplayName("VectorWaveSwtAdapter: close() shuts down executor and clears caches")
    void executorLifecycle() {
        int n = 8192; int levels = 4;
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = Math.sin(2 * Math.PI * 3 * i / (double) n);

        try (VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC, true, 1)) {
            Map<String, Object> before = swt.getCacheStatistics();
            assertFalse((Boolean) before.get("parallelExecutorActive"));

            MutableMultiLevelMODWTResult res = swt.forward(x, levels);
            assertEquals(n, res.getSignalLength());

            Map<String, Object> mid = swt.getCacheStatistics();
            assertTrue((Boolean) mid.get("parallelExecutorActive"));
            assertTrue(((Integer) mid.get("filterCacheSize")) >= 1);
        }

        // After try-with-resources, executor is shutdown and caches cleared
        try (VectorWaveSwtAdapter swt2 = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC, true, 1)) {
            Map<String, Object> after = swt2.getCacheStatistics();
            // New instance should start clean
            assertFalse((Boolean) after.get("parallelExecutorActive"));
            assertEquals(0, ((Integer) after.get("filterCacheSize")).intValue());
        }
    }

    @Test
    @DisplayName("VectorWaveSwtAdapter: forward works after close (lazy re-init)")
    void forwardAfterCloseReinitializes() {
        int n = 4096; int levels = 3;
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = Math.sin(2 * Math.PI * 5 * i / (double) n) + 0.2 * Math.cos(2 * Math.PI * 13 * i / (double) n);

        VectorWaveSwtAdapter seq = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC, false, Integer.MAX_VALUE);
        VectorWaveSwtAdapter par = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC, true, 1);

        // Close parallel adapter to force re-init on next forward
        par.close();

        MutableMultiLevelMODWTResult rSeq = seq.forward(x, levels);
        MutableMultiLevelMODWTResult rPar = par.forward(x, levels);

        // Compare coefficients with a small tolerance
        for (int level = 1; level <= levels; level++) {
            double[] ds = rSeq.getDetailCoeffsAtLevel(level);
            double[] dp = rPar.getDetailCoeffsAtLevel(level);
            assertEquals(ds.length, dp.length);
            for (int i = 0; i < ds.length; i++) {
                assertEquals(ds[i], dp[i], 1e-9, "detail level=" + level);
            }
        }
        double[] as = rSeq.getApproximationCoeffs();
        double[] ap = rPar.getApproximationCoeffs();
        assertEquals(as.length, ap.length);
        for (int i = 0; i < as.length; i++) {
            assertEquals(as[i], ap[i], 1e-9, "approx coeff");
        }

        seq.close();
        par.close();
    }
}

