package com.morphiqlabs.wavelet.swt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class SwtAdapterParityTest {

    private static double[] makeSignal(int n) {
        double[] s = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            s[i] = Math.sin(2 * Math.PI * 3 * t) + 0.2 * Math.cos(2 * Math.PI * 13 * t);
        }
        return s;
    }

    @Test
    @DisplayName("SWT adapter matches multi-level MODWT decomposition (coefficients)")
    void swtMatchesModwt() {
        int n = 128;
        int levels = 3;

        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC);
        MultiLevelMODWTTransform modwt = new MultiLevelMODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);

        double[] x = makeSignal(n);

        MutableMultiLevelMODWTResult swtRes = swt.forward(x, levels);
        MultiLevelMODWTResult modwtRes = modwt.decompose(x, levels);

        // Compare coefficients per level and approximation
        for (int level = 1; level <= levels; level++) {
            double[] a = swtRes.getDetailCoeffsAtLevel(level);
            double[] b = modwtRes.getDetailCoeffsAtLevel(level);
            assertEquals(a.length, b.length);
            for (int i = 0; i < a.length; i++) {
                assertEquals(a[i], b[i], 1e-10, "detail level=" + level);
            }
        }

        double[] aApprox = swtRes.getApproximationCoeffs();
        double[] bApprox = modwtRes.getApproximationCoeffs();
        assertEquals(aApprox.length, bApprox.length);
        for (int i = 0; i < aApprox.length; i++) {
            assertEquals(aApprox[i], bApprox[i], 1e-10, "approx coeff");
        }
    }
}

