package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compares current SYMMETRIC interior NRMSE against a stored baseline with 10% headroom.
 * Enabled when env var VECTORWAVE_SYM_BASELINE=compare is set and baseline resource is present.
 * Baseline format (properties): key="wavelet,N,level" value="nrmse"
 */
class SymmetricNRMSEBaselineGuardTest {

    @Test
    @DisplayName("Baseline guard: SYMMETRIC NRMSE drift <= 10% vs baseline (optional)")
    void guardAgainstDrift() throws IOException {
        String mode = System.getenv("VECTORWAVE_SYM_BASELINE");
        if (mode == null || !mode.equalsIgnoreCase("compare")) {
            return; // disabled
        }
        Properties baseline = new Properties();
        try (InputStream in = SymmetricNRMSEBaselineGuardTest.class.getResourceAsStream(
                "/baselines/symmetric_nrmse_baseline.properties")) {
            if (in == null) {
                return; // no baseline present
            }
            baseline.load(in);
        }
        if (baseline.isEmpty()) return;

        Map<String, Wavelet> registry = new HashMap<>();
        registry.put("haar", new com.morphiqlabs.wavelet.api.Haar());
        registry.put("db4", com.morphiqlabs.wavelet.api.Daubechies.DB4);
        registry.put("sym4", com.morphiqlabs.wavelet.api.Symlet.SYM4);
        registry.put("coif2", com.morphiqlabs.wavelet.api.Coiflet.COIF2);

        for (String key : baseline.stringPropertyNames()) {
            String[] parts = key.split(",");
            if (parts.length != 3) continue;
            String wname = parts[0].trim().toLowerCase(Locale.ROOT);
            int n = Integer.parseInt(parts[1].trim());
            int level = Integer.parseInt(parts[2].trim());
            double base = Double.parseDouble(baseline.getProperty(key).trim());
            Wavelet w = registry.get(wname);
            if (w == null) continue;

            double[] x = randomSignal(n, 123L);
            MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(w, BoundaryMode.SYMMETRIC);
            int J = Math.max(1, Math.min(level, t.getMaximumLevels(n)));
            MultiLevelMODWTResult r = t.decompose(x, J);
            double[] y = t.reconstruct(r);

            int baseL0 = w.lowPassReconstruction().length;
            int Lups = (baseL0 - 1) * (1 << Math.max(0, J - 1)) + 1;
            int margin = Math.min(n / 4, Math.max(1, Lups / 2));
            double cur = nrmseInterior(x, y, margin);

            double allowed = base * 1.10; // 10% headroom
            assertTrue(cur <= allowed, String.format(Locale.ROOT,
                    "NRMSE drift: w=%s N=%d L=%d cur=%.4f base=%.4f allowed=%.4f", wname, n, J, cur, base, allowed));
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

