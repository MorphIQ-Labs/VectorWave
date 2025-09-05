package com.morphiqlabs.wavelet.extensions.modwt;

import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Daubechies;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BatchMODWTApiTest {

    private static final double TOL = 1e-10;

    @Test
    @DisplayName("Single-level batch AoS API parity vs core MODWTTransform")
    void testSingleLevelParity() {
        int batch = 4, n = 128;
        double[][] signals = randomAoS(batch, n, 123);

        // Haar
        var outHaar = BatchMODWT.singleLevelAoS(new Haar(), signals);
        {
            var core = new com.morphiqlabs.wavelet.modwt.MODWTTransform(new Haar(), com.morphiqlabs.wavelet.api.BoundaryMode.PERIODIC);
            for (int b = 0; b < signals.length; b++) {
                var res = core.forward(signals[b]);
                for (int i = 0; i < signals[b].length; i++) {
                    assertEquals(res.approximationCoeffs()[i], outHaar.approx()[b][i], TOL);
                    assertEquals(res.detailCoeffs()[i], outHaar.detail()[b][i], TOL);
                }
            }
        }
        // DB4
        var outDb4 = BatchMODWT.singleLevelAoS(Daubechies.DB4, signals);
        {
            var core = new com.morphiqlabs.wavelet.modwt.MODWTTransform(Daubechies.DB4, com.morphiqlabs.wavelet.api.BoundaryMode.PERIODIC);
            for (int b = 0; b < signals.length; b++) {
                var res = core.forward(signals[b]);
                for (int i = 0; i < signals[b].length; i++) {
                    assertEquals(res.approximationCoeffs()[i], outDb4.approx()[b][i], TOL);
                    assertEquals(res.detailCoeffs()[i], outDb4.detail()[b][i], TOL);
                }
            }
        }
    }

    @Test
    @DisplayName("Multi-level batch AoS API returns correct shapes")
    void testMultiLevelShapes() {
        int batch = 2, n = 64, levels = 3;
        double[][] signals = randomAoS(batch, n, 777);
        var out = BatchMODWT.multiLevelAoS(new Haar(), signals, levels);
        org.junit.jupiter.api.Assertions.assertEquals(levels, out.detailPerLevel().length);
        for (int L = 0; L < levels; L++) {
            org.junit.jupiter.api.Assertions.assertEquals(batch, out.detailPerLevel()[L].length);
            for (int b = 0; b < batch; b++) {
                org.junit.jupiter.api.Assertions.assertEquals(n, out.detailPerLevel()[L][b].length);
            }
        }
        org.junit.jupiter.api.Assertions.assertEquals(batch, out.finalApprox().length);
        for (int b = 0; b < batch; b++) {
            org.junit.jupiter.api.Assertions.assertEquals(n, out.finalApprox()[b].length);
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
