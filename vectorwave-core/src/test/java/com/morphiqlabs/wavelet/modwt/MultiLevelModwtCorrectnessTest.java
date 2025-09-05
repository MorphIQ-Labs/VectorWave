package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Symlet;
import com.morphiqlabs.wavelet.api.Coiflet;
import com.morphiqlabs.wavelet.testing.TestSignals;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MultiLevelModwtCorrectnessTest {

    private static double[] makeSignal(int n) { return TestSignals.compositeSin(n, 7L, 0.0); }

    private static double energy(double[] a) {
        double e = 0.0;
        for (double v : a) e += v * v;
        return e;
    }

    private static void assertMultiRoundTrip(Wavelet wavelet, int n) {
        MultiLevelMODWTTransform tx = new MultiLevelMODWTTransform(wavelet, BoundaryMode.PERIODIC);
        int levels = Math.min(5, tx.getMaximumLevels(n));
        assertTrue(levels >= 1, "levels >= 1");
        double[] x = makeSignal(n);
        MultiLevelMODWTResult res = tx.decompose(x, levels);
        double[] y = tx.reconstruct(res);

        assertEquals(x.length, y.length);

        double maxAbs = 0.0;
        for (int i = 0; i < x.length; i++) {
            maxAbs = Math.max(maxAbs, Math.abs(x[i] - y[i]));
        }
        double eps;
        if (wavelet instanceof Haar || wavelet == Daubechies.DB4) {
            eps = 1e-9;
        } else if (wavelet == Symlet.SYM4) {
            eps = 1e-6;
        } else if (wavelet == Coiflet.COIF2) {
            eps = 5e-4; // documented lower precision for COIF2
        } else {
            eps = 1e-8;
        }
        assertTrue(maxAbs < eps, "max abs diff=" + maxAbs + ", eps=" + eps);

        // Energy accounting
        double totalEnergy = res.getTotalEnergy();
        double signalEnergy = energy(x);
        double tolRel;
        if (wavelet instanceof Haar || wavelet == Daubechies.DB4) {
            tolRel = 1e-8;
        } else if (wavelet == Symlet.SYM4) {
            tolRel = 1e-6;
        } else if (wavelet == Coiflet.COIF2) {
            tolRel = 5e-4; // allow small relative drift
        } else {
            tolRel = 1e-7;
        }
        assertEquals(signalEnergy, totalEnergy, tolRel * signalEnergy, "total energy");

        double[] dist = res.getRelativeEnergyDistribution();
        double sum = 0.0;
        for (double d : dist) sum += d;
        assertEquals(1.0, sum, 1e-12, "relative energy sums to 1");
    }

    @Test
    @DisplayName("Multi-level periodic: Haar round-trip and energy")
    void multiHaar() {
        assertMultiRoundTrip(new Haar(), 512);
    }

    @Test
    @DisplayName("Multi-level periodic: DB4 round-trip and energy")
    void multiDb4() {
        assertMultiRoundTrip(Daubechies.DB4, 512);
    }

    @Test
    @DisplayName("Multi-level periodic: SYM4 round-trip and energy")
    void multiSym4() {
        assertMultiRoundTrip(Symlet.SYM4, 512);
    }

    @Test
    @DisplayName("Multi-level periodic: COIF2 round-trip and energy")
    void multiCoif2() {
        assertMultiRoundTrip(Coiflet.COIF2, 512);
    }
}
