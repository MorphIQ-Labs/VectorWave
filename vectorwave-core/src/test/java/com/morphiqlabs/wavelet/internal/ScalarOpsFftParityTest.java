package com.morphiqlabs.wavelet.internal;

import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.util.ToleranceConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates numerical parity between scalar and FFT-based circular convolution for MODWT periodic mode.
 */
class ScalarOpsFftParityTest {

    private static final double TOL = ToleranceConstants.FFT_PARITY_TOLERANCE;

    @Test
    @DisplayName("FFT vs Scalar parity on random filters/signal")
    void testRandomParity() {
        int[] sizes = {2048, 4096, 8192};
        Random rnd = new Random(42);
        for (int n : sizes) {
            double[] signal = new double[n];
            for (int i = 0; i < n; i++) signal[i] = rnd.nextDouble() * 2 - 1;
            // Random dense filter with L ~ N/4 to exercise FFT path
            int L = Math.max(8, n / 4);
            double[] filter = new double[L];
            for (int i = 0; i < L; i++) filter[i] = rnd.nextDouble() * 2 - 1;

            double[] outScalar = new double[n];
            double[] outFFT = new double[n];
            ScalarOps.circularConvolveMODWT(signal, filter, outScalar);
            ScalarOps.circularConvolveMODWTFFT(signal, filter, outFFT);

            for (int i = 0; i < n; i++) {
                assertEquals(outScalar[i], outFFT[i], TOL, "Mismatch at index " + i + " for N=" + n);
            }
        }
    }

    @Test
    @DisplayName("FFT vs Scalar parity on upsampled DB8 analysis filters")
    void testWaveletUpsampledParity() {
        int n = 16384;
        double[] signal = new double[n];
        for (int i = 0; i < n; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64.0) + 0.5 * Math.sin(2 * Math.PI * i / 7.0);
        }
        // Upsample DB8 to a high level so L is large relative to N
        int level = 10; // yields L ~ 7681 for DB8
        double[] filter = ScalarOps.upsampleAndScaleForMODWTAnalysis(Daubechies.DB8.lowPassDecomposition(), level);

        double[] outScalar = new double[n];
        double[] outFFT = new double[n];
        ScalarOps.circularConvolveMODWT(signal, filter, outScalar);
        ScalarOps.circularConvolveMODWTFFT(signal, filter, outFFT);

        for (int i = 0; i < n; i++) {
            assertEquals(outScalar[i], outFFT[i], TOL, "Mismatch at index " + i);
        }
    }
}
