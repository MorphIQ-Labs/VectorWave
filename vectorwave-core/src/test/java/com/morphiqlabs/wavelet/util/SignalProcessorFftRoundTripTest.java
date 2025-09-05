package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.cwt.ComplexNumber;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SignalProcessorFftRoundTripTest {

    @Test
    @DisplayName("FFT -> IFFT round-trip preserves complex data (power-of-2 sizes)")
    void testRoundTripComplexData() {
        int[] sizes = {64, 256, 1024};
        Random rnd = new Random(123);

        for (int n : sizes) {
            ComplexNumber[] data = new ComplexNumber[n];
            for (int i = 0; i < n; i++) {
                double re = Math.sin(2 * Math.PI * i / 16.0) + 0.1 * rnd.nextDouble();
                double im = Math.cos(2 * Math.PI * i / 11.0) + 0.1 * rnd.nextDouble();
                data[i] = new ComplexNumber(re, im);
            }

            ComplexNumber[] original = new ComplexNumber[n];
            System.arraycopy(data, 0, original, 0, n);

            SignalProcessor.fft(data);
            SignalProcessor.ifft(data);

            for (int i = 0; i < n; i++) {
                assertEquals(original[i].real(), data[i].real(), ToleranceConstants.DEFAULT_TOLERANCE,
                        "Real mismatch at index " + i + " for n=" + n);
                assertEquals(original[i].imag(), data[i].imag(), ToleranceConstants.DEFAULT_TOLERANCE,
                        "Imag mismatch at index " + i + " for n=" + n);
            }
        }
    }
}

