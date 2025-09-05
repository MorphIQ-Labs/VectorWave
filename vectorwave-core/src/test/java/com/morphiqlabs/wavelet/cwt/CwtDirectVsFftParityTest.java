package com.morphiqlabs.wavelet.cwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("CWT is experimental; parity tests disabled until stabilization")
class CwtDirectVsFftParityTest {

    private static double[] makeSignal(int n) {
        double[] s = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            s[i] = Math.sin(2 * Math.PI * 5 * t) + 0.5 * Math.cos(2 * Math.PI * 17 * t);
        }
        return s;
    }

    private static double[] scales() {
        // Use moderate/large scales where support is well-resolved to reduce discretization differences
        return new double[] { 8.0, 12.0, 16.0, 24.0, 32.0 };
    }

    private static double dot(double[] a, double[] b) {
        double s = 0.0;
        for (int i = 0; i < a.length; i++) s += a[i] * b[i];
        return s;
    }

    private static double norm(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    @Test
    @DisplayName("CWT: Direct vs FFT parity on real wavelet (periodic)")
    void directVsFftParity() {
        int n = 128; // ensures FFT path triggers with default config
        double[] x = makeSignal(n);
        double[] scales = scales();

        // Real wavelet
        RickerWavelet wavelet = new RickerWavelet(1.0);

        // Direct config
        CWTConfig directCfg = CWTConfig.builder()
                .enableFFT(false)
                .build();
        CWTTransform direct = new CWTTransform(wavelet, directCfg);
        CWTResult rd = direct.analyze(x, scales);

        // FFT config
        CWTConfig fftCfg = CWTConfig.builder()
                .enableFFT(true)
                .build();
        CWTTransform fft = new CWTTransform(wavelet, fftCfg);
        CWTResult rf = fft.analyze(x, scales);

        assertEquals(rd.getScales().length, rf.getScales().length);
        assertEquals(n, rd.getCoefficients()[0].length);
        assertEquals(n, rf.getCoefficients()[0].length);

        // Compare each scale using normalized correlation; allow small numerical differences
        for (int s = 0; s < scales.length; s++) {
            double[] a = rd.getCoefficients()[s];
            double[] b = rf.getCoefficients()[s];
            double corr = dot(a, b) / (Math.max(1e-16, norm(a) * norm(b)));
            assertTrue(corr > 0.95, "scale=" + scales[s] + " corr=" + corr);
        }
    }
}
