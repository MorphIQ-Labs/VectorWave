package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.morphiqlabs.wavelet.testing.TestSignals;

import static org.junit.jupiter.api.Assertions.*;

class ModwtPeriodicRoundTripTest {

    private static double[] makeSignal(int n, long seed) { return TestSignals.compositeSin(n, seed, 0.05); }

    private static double energy(double[] a) {
        double e = 0.0;
        for (double v : a) e += v * v;
        return e;
    }

    private static void assertRoundTrip(Wavelet wavelet, int n) {
        MODWTTransform tx = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        double[] x = makeSignal(n, 42L);
        MODWTResult res = tx.forward(x);
        double[] y = tx.inverse(res);

        assertEquals(x.length, y.length, "length");

        double maxAbs = 0.0;
        for (int i = 0; i < x.length; i++) {
            maxAbs = Math.max(maxAbs, Math.abs(x[i] - y[i]));
        }
        // Near machine precision for PERIODIC
        assertTrue(maxAbs < 1e-9, "max abs diff=" + maxAbs);

        double ex = energy(x);
        double ey = energy(y);
        assertEquals(ex, ey, 1e-8 * ex, "energy preserved");
    }

    @Test
    @DisplayName("Round-trip periodic: Haar on odd length")
    void roundTripHaarOdd() {
        assertRoundTrip(new Haar(), 129);
    }

    @Test
    @DisplayName("Round-trip periodic: Haar on even length")
    void roundTripHaarEven() {
        assertRoundTrip(new Haar(), 128);
    }

    @Test
    @DisplayName("Round-trip periodic: DB4 on odd length")
    void roundTripDb4Odd() {
        assertRoundTrip(Daubechies.DB4, 129);
    }

    @Test
    @DisplayName("Round-trip periodic: DB4 on even length")
    void roundTripDb4Even() {
        assertRoundTrip(Daubechies.DB4, 256);
    }
}
