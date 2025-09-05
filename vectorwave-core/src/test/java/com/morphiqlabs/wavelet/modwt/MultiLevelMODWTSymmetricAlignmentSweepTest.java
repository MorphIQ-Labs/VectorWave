package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.util.MathUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;

/**
 * Diagnostic sweep to explore symmetric reconstruction centering/orientation.
 * Not part of strict assertions; prints best RMSE and suggested (orientation, delta) pairs.
 */
class MultiLevelMODWTSymmetricAlignmentSweepTest {

    @Test
    @DisplayName("Symmetric inverse: sweep τ offsets and orientations (diagnostic)")
    void sweepSymmetricInverseAlignment() {
        Locale.setDefault(Locale.ROOT);
        Wavelet[] ws = {
                new Haar(),
                Daubechies.DB4, Daubechies.DB6, Daubechies.DB8,
                com.morphiqlabs.wavelet.api.Symlet.SYM4, com.morphiqlabs.wavelet.api.Symlet.SYM8,
                com.morphiqlabs.wavelet.api.Coiflet.COIF2, com.morphiqlabs.wavelet.api.Coiflet.COIF3
        };
        int[] Ns = {129, 193, 257, 383, 512};
        String[] orient = {"plus", "minus"};
        int[] deltas = {-1, 0, 1};

        for (Wavelet w : ws) {
            for (int n : Ns) {
                double[] x = randomSignal(n, 99L);
                MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(w, BoundaryMode.SYMMETRIC);
                int J = t.getMaximumLevels(n);
                if (J == 0) continue;
                MultiLevelMODWTResult r = t.decompose(x, J);

                double bestRmse = Double.POSITIVE_INFINITY;
                String bestCfg = "";
                for (String hOrient : orient) {
                    for (String gOrient : orient) {
                        for (int dH : deltas) {
                            for (int dG : deltas) {
                                double[] xr = reconstructSymmetricWithOffsets(w, r, hOrient, gOrient, dH, dG);
                                double e = rmse(x, xr);
                                if (e < bestRmse) {
                                    bestRmse = e;
                                    bestCfg = String.format("H:%s dH=%d | G:%s dG=%d", hOrient, dH, gOrient, dG);
                                }
                            }
                        }
                    }
                }
                System.out.printf("[Sweep] w=%s N=%d J=%d -> best RMSE=%.6g with %s%n",
                        w.name(), n, J, bestRmse, bestCfg);
            }
        }
    }

    private static double[] reconstructSymmetricWithOffsets(Wavelet w, MultiLevelMODWTResult r,
                                                            String hOrient, String gOrient,
                                                            int deltaH, int deltaG) {
        int n = r.getSignalLength();
        int J = r.getLevels();
        double[] current = r.getApproximationCoeffs().clone();
        for (int level = J; level >= 1; level--) {
            double[] details = r.getDetailCoeffsAtLevel(level);
            // Upsample recon filters with cascade scaling 1/sqrt(2)
            double[] h = w.lowPassReconstruction();
            double[] g = w.highPassReconstruction();
            int up = 1 << Math.max(0, level - 1);
            double scale = 1.0 / Math.sqrt(2.0);
            double[] H = upsample(h, up, scale);
            double[] G = upsample(g, up, scale);

            // Compute τ_j from base lengths with optional deltas
            int tauH = computeTau(h.length, level) + deltaH;
            int tauG = computeTau(g.length, level) + deltaG;

            // Reconstruct one level with symmetric extension
            double[] out = new double[n];
            for (int t = 0; t < n; t++) {
                double sum = 0.0;
                // Approx branch
                if ("plus".equals(hOrient)) {
                    for (int l = 0; l < H.length; l++) {
                        int idx = t + l - tauH;
                        idx = MathUtils.symmetricBoundaryExtension(idx, n);
                        sum += H[l] * current[idx];
                    }
                } else {
                    for (int l = 0; l < H.length; l++) {
                        int idx = t - l + tauH;
                        idx = MathUtils.symmetricBoundaryExtension(idx, n);
                        sum += H[l] * current[idx];
                    }
                }
                // Detail branch
                if ("plus".equals(gOrient)) {
                    for (int l = 0; l < G.length; l++) {
                        int idx = t + l - tauG;
                        idx = MathUtils.symmetricBoundaryExtension(idx, n);
                        sum += G[l] * details[idx];
                    }
                } else {
                    for (int l = 0; l < G.length; l++) {
                        int idx = t - l + tauG;
                        idx = MathUtils.symmetricBoundaryExtension(idx, n);
                        sum += G[l] * details[idx];
                    }
                }
                out[t] = sum;
            }
            current = out;
        }
        return current;
    }

    private static int computeTau(int baseL0, int level) {
        int Lm1 = baseL0 - 1;
        if (level <= 1) return Math.max(0, Lm1 / 2);
        long Lj = (long)Lm1 * (1L << (level - 1)) + 1L;
        return (int)((Lj - 1L) / 2L);
    }

    private static double[] upsample(double[] f, int up, double scale) {
        if (up <= 1) {
            double[] out = f.clone();
            for (int i = 0; i < out.length; i++) out[i] *= scale;
            return out;
        }
        int L = (f.length - 1) * up + 1;
        double[] out = new double[L];
        for (int i = 0; i < f.length; i++) out[i * up] = f[i] * scale;
        return out;
    }

    private static double rmse(double[] a, double[] b) {
        double s = 0.0; int n = a.length;
        for (int i = 0; i < n; i++) { double d = a[i] - b[i]; s += d*d; }
        return Math.sqrt(s / n);
    }

    private static double[] randomSignal(int n, long seed) {
        java.util.Random r = new java.util.Random(seed);
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = r.nextDouble() * 2 - 1;
        return x;
    }
}
