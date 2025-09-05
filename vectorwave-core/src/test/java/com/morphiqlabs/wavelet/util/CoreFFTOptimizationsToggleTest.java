package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.fft.CoreFFT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CoreFFTOptimizationsToggleTest {

    private String prevRealOpt;
    private String prevStockham;

    @AfterEach
    void tearDown() {
        if (prevRealOpt != null) System.setProperty("vectorwave.fft.realOptimized", prevRealOpt);
        else System.clearProperty("vectorwave.fft.realOptimized");
        if (prevStockham != null) System.setProperty("vectorwave.fft.stockham", prevStockham);
        else System.clearProperty("vectorwave.fft.stockham");
    }

    @Test
    @DisplayName("realOptimized RFFT parity vs default")
    void testRealOptimizedParity() {
        prevRealOpt = System.getProperty("vectorwave.fft.realOptimized");
        Random rnd = new Random(42);
        int[] sizes = {64, 128, 256, 512};
        for (int n : sizes) {
            double[] signal = new double[n];
            for (int i = 0; i < n; i++) signal[i] = rnd.nextDouble() * 2 - 1;

            // Optimized ON
            System.setProperty("vectorwave.fft.realOptimized", "true");
            double[] opt = CoreFFT.rfft(signal);
            // Optimized OFF
            System.setProperty("vectorwave.fft.realOptimized", "false");
            double[] def = CoreFFT.rfft(signal);

            assertEquals(opt.length, def.length);
            for (int i = 0; i < opt.length; i++) {
                assertEquals(def[i], opt[i], 1e-12, "Mismatch at index " + i + " for n=" + n);
            }
        }
    }

    @Test
    @DisplayName("stockham FFT parity vs default")
    void testStockhamParity() {
        prevStockham = System.getProperty("vectorwave.fft.stockham");
        Random rnd = new Random(24);
        int[] sizes = {64, 128, 256, 512};
        for (int n : sizes) {
            double[] real = new double[n];
            double[] imag = new double[n];
            for (int i = 0; i < n; i++) { real[i] = rnd.nextDouble(); imag[i] = rnd.nextDouble(); }

            double[] r1 = real.clone();
            double[] i1 = imag.clone();
            double[] r2 = real.clone();
            double[] i2 = imag.clone();

            // Stockham ON
            System.setProperty("vectorwave.fft.stockham", "true");
            CoreFFT.fft(r1, i1);
            // Stockham OFF
            System.setProperty("vectorwave.fft.stockham", "false");
            CoreFFT.fft(r2, i2);

            for (int i = 0; i < n; i++) {
                assertEquals(r2[i], r1[i], 1e-12, "Real mismatch at index " + i + " for n=" + n);
                assertEquals(i2[i], i1[i], 1e-12, "Imag mismatch at index " + i + " for n=" + n);
            }
        }
    }
}
