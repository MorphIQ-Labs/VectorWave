package com.morphiqlabs.wavelet.cwt;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CWTRealOptimizedParityTest {

    private String prevFlag;

    @AfterEach
    void restore() {
        if (prevFlag != null) System.setProperty("vectorwave.fft.realOptimized", prevFlag);
        else System.clearProperty("vectorwave.fft.realOptimized");
    }

    @Test
    @DisplayName("CWT analyze parity with realOptimized RFFT on/off")
    void testCWTRealParity() {
        double[] signal = new double[512];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 37.0) + 0.3 * Math.sin(2 * Math.PI * i / 11.0);
        }
        double[] scales = {2, 4, 8, 16, 32};
        MorletWavelet wavelet = new MorletWavelet();

        CWTConfig cfg = CWTConfig.builder().enableFFT(true).build();

        prevFlag = System.getProperty("vectorwave.fft.realOptimized");

        // ON
        System.setProperty("vectorwave.fft.realOptimized", "true");
        CWTResult rOn = new CWTTransform(wavelet, cfg).analyze(signal, scales);
        // OFF
        System.setProperty("vectorwave.fft.realOptimized", "false");
        CWTResult rOff = new CWTTransform(wavelet, cfg).analyze(signal, scales);

        double[][] aOn = rOn.getCoefficients();
        double[][] aOff = rOff.getCoefficients();
        assertEquals(aOff.length, aOn.length);
        for (int s = 0; s < aOn.length; s++) {
            assertArrayEquals(aOff[s], aOn[s], 1e-10, "Mismatch at scale index " + s);
        }
    }

    @Test
    @DisplayName("CWT complex analyze parity with realOptimized RFFT on/off")
    void testCWTComplexParity() {
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 23.0);
        }
        double[] scales = {3, 5, 9, 17};
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        CWTConfig cfg = CWTConfig.builder().enableFFT(true).build();

        prevFlag = System.getProperty("vectorwave.fft.realOptimized");
        System.setProperty("vectorwave.fft.realOptimized", "true");
        ComplexCWTResult cOn = new CWTTransform(wavelet, cfg).analyzeComplex(signal, scales);
        System.setProperty("vectorwave.fft.realOptimized", "false");
        ComplexCWTResult cOff = new CWTTransform(wavelet, cfg).analyzeComplex(signal, scales);

        ComplexNumber[][] on = cOn.getCoefficients();
        ComplexNumber[][] off = cOff.getCoefficients();
        assertEquals(off.length, on.length);
        for (int s = 0; s < on.length; s++) {
            for (int t = 0; t < on[s].length; t++) {
                assertEquals(off[s][t].real(), on[s][t].real(), 1e-10, "Real mismatch at ("+s+","+t+")");
                assertEquals(off[s][t].imag(), on[s][t].imag(), 1e-10, "Imag mismatch at ("+s+","+t+")");
            }
        }
    }
}
