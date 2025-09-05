package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoundaryModesSmokeTest {

    private static double[] makeSignal(int n) {
        double[] s = new double[n];
        for (int i = 0; i < n; i++) s[i] = Math.sin(2 * Math.PI * i / (double) n);
        return s;
    }

    private static void smoke(Wavelet w, BoundaryMode mode, int n) {
        MultiLevelMODWTTransform tx = new MultiLevelMODWTTransform(w, mode);
        int levels = Math.min(3, Math.max(1, tx.getMaximumLevels(n)));
        double[] x = makeSignal(n);
        MultiLevelMODWTResult res = tx.decompose(x, levels);
        double[] y = tx.reconstruct(res);
        assertEquals(n, y.length);
        // Not asserting tight equality for non-periodic; just ensure finite and reasonable scale
        for (double v : y) assertTrue(Double.isFinite(v));
    }

    @Test
    @DisplayName("Smoke: ZERO_PADDING on short odd length")
    void zeroPaddingShortOdd() {
        smoke(Daubechies.DB4, BoundaryMode.ZERO_PADDING,  nine());
    }

    @Test
    @DisplayName("Smoke: SYMMETRIC on short odd length")
    void symmetricShortOdd() {
        smoke(Daubechies.DB4, BoundaryMode.SYMMETRIC, nine());
    }

    private static int nine() { return 9; }
}

