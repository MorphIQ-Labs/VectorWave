package com.morphiqlabs.wavelet.extensions.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchMODWTMultiLevelParityTest {
    private static final double TOL = 1e-10;

    @Test
    @DisplayName("Multi-level batch AoS parity vs core (Haar, DB4) for periodic")
    void testParityMultiLevel() {
        assertParityForWavelet(new Haar());
        assertParityForWavelet(Daubechies.DB4);
    }

    private static void assertParityForWavelet(com.morphiqlabs.wavelet.api.DiscreteWavelet wavelet) {
        int batch = 3, n = 128, levels = 3;
        double[][] signals = randomAoS(batch, n, 42);

        var out = BatchMODWT.multiLevelAoS(wavelet, signals, levels);
        var core = new MultiLevelMODWTTransform(wavelet, BoundaryMode.PERIODIC);

        for (int b = 0; b < batch; b++) {
            var res = core.decompose(signals[b], levels);
            for (int L = 1; L <= levels; L++) {
                double[] exp = res.getDetailCoeffsAtLevel(L);
                for (int i = 0; i < n; i++) {
                    assertEquals(exp[i], out.detailPerLevel()[L-1][b][i], TOL,
                            "detail parity fail w="+wavelet+" L="+L+" b="+b+" i="+i);
                }
            }
            double[] approx = res.getApproximationCoeffs();
            for (int i = 0; i < n; i++) {
                assertEquals(approx[i], out.finalApprox()[b][i], TOL,
                        "approx parity fail w="+wavelet+" b="+b+" i="+i);
            }
        }
    }

    private static double[][] randomAoS(int batch, int n, long seed) {
        Random rnd = new Random(seed);
        double[][] s = new double[batch][n];
        for (int b = 0; b < batch; b++) {
            for (int i = 0; i < n; i++) s[b][i] = rnd.nextDouble() * 2 - 1;
        }
        return s;
    }
}
