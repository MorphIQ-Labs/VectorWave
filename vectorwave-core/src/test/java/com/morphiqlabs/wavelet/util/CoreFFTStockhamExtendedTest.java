package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.fft.CoreFFT;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CoreFFTStockhamExtendedTest {

    private String prevStockham;

    @AfterEach
    void restoreFlag() {
        if (prevStockham != null) System.setProperty("vectorwave.fft.stockham", prevStockham);
        else System.clearProperty("vectorwave.fft.stockham");
    }

    @Test
    @DisplayName("Stockham FFT parity vs default across sizes, random inputs")
    void testStockhamParityExtended() {
        prevStockham = System.getProperty("vectorwave.fft.stockham");
        Random rnd = new Random(1337);
        int[] sizes = {8, 16, 32, 64, 128, 256, 512, 1024};

        for (int n : sizes) {
            double[] r = new double[n];
            double[] i = new double[n];
            for (int idx = 0; idx < n; idx++) {
                r[idx] = rnd.nextDouble() * 2 - 1;
                i[idx] = rnd.nextDouble() * 2 - 1;
            }

            double[] rStock = r.clone();
            double[] iStock = i.clone();
            double[] rDef = r.clone();
            double[] iDef = i.clone();

            System.setProperty("vectorwave.fft.stockham", "true");
            CoreFFT.fft(rStock, iStock);

            System.setProperty("vectorwave.fft.stockham", "false");
            CoreFFT.fft(rDef, iDef);

            for (int idx = 0; idx < n; idx++) {
                assertEquals(rDef[idx], rStock[idx], 1e-12,
                        "Real mismatch at index " + idx + " for n=" + n);
                assertEquals(iDef[idx], iStock[idx], 1e-12,
                        "Imag mismatch at index " + idx + " for n=" + n);
            }
        }
    }

    @Test
    @DisplayName("Stockham FFT->IFFT round-trip preserves data (odd/even stages)")
    void testStockhamRoundTripComplex() {
        prevStockham = System.getProperty("vectorwave.fft.stockham");
        System.setProperty("vectorwave.fft.stockham", "true");
        Random rnd = new Random(2024);
        int[] sizes = {8, 32, 64, 128, 512, 1024};

        for (int n : sizes) {
            double[] r = new double[n];
            double[] i = new double[n];
            for (int idx = 0; idx < n; idx++) {
                r[idx] = Math.sin(2 * Math.PI * idx / 7.0) + 0.05 * (rnd.nextDouble() - 0.5);
                i[idx] = Math.cos(2 * Math.PI * idx / 11.0) + 0.05 * (rnd.nextDouble() - 0.5);
            }
            double[] rOrig = r.clone();
            double[] iOrig = i.clone();

            CoreFFT.fft(r, i);
            CoreFFT.ifft(r, i);

            for (int idx = 0; idx < n; idx++) {
                assertEquals(rOrig[idx], r[idx], ToleranceConstants.DEFAULT_TOLERANCE,
                        "Round-trip real mismatch at index " + idx + " for n=" + n);
                assertEquals(iOrig[idx], i[idx], ToleranceConstants.DEFAULT_TOLERANCE,
                        "Round-trip imag mismatch at index " + idx + " for n=" + n);
            }
        }
    }

    // Note: RFFT parity vs Stockham is implicitly covered by existing
    // CoreFFTOptimizationsToggleTest for realOptimized toggling. To
    // keep this suite focused on the complex Stockham path, we skip
    // duplicating RFFT coverage here.
}
