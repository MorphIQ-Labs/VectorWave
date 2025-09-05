package com.morphiqlabs.wavelet.extensions.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Parity tests for SIMD streaming (ZERO_PADDING, SYMMETRIC) vs core transforms
 * when processing a continuous signal split into blocks.
 */
public class BatchStreamingMODWTStreamingParityTest {

    private static double[][] randomSignals(int batch, int n, long seed) {
        Random rnd = new Random(seed);
        double[][] s = new double[batch][n];
        for (int b = 0; b < batch; b++) {
            for (int i = 0; i < n; i++) {
                // Smooth-ish random signal to exercise filters
                s[b][i] = Math.sin(0.01 * (i + b)) + 0.1 * rnd.nextGaussian();
            }
        }
        return s;
    }

    private static void assertArrayClose(double[] a, double[] b, double tol) {
        assertEquals(a.length, b.length);
        for (int i = 0; i < a.length; i++) {
            double diff = Math.abs(a[i] + 0.0 - b[i]);
            assertTrue(diff <= tol, "Mismatch at index " + i + ": " + a[i] + " vs " + b[i]);
        }
    }

    @Test
    public void testSingleLevelZeroPadding_Haar() {
        singleLevelStreamingParity(new Haar(), BoundaryMode.ZERO_PADDING, 4, 512, 64, 1e-9);
    }

    @Test
    public void testSingleLevelSymmetric_DB4() {
        singleLevelStreamingParity(Daubechies.DB4, BoundaryMode.SYMMETRIC, 3, 512, 128, 1e-8);
    }

    private void singleLevelStreamingParity(DiscreteWavelet wavelet, BoundaryMode mode,
                                            int batch, int totalLen, int blockLen, double tol) {
        double[][] full = randomSignals(batch, totalLen, 42);

        // Reference using core single-level transform over the full signals
        MODWTTransform core = new MODWTTransform(wavelet, mode);
        double[][] refApprox = new double[batch][totalLen];
        double[][] refDetail = new double[batch][totalLen];
        for (int b = 0; b < batch; b++) {
            MODWTResult r = core.forward(full[b]);
            refApprox[b] = r.approximationCoeffs();
            refDetail[b] = r.detailCoeffs();
        }

        // Streaming using SIMD facade
        double[][] gotApprox = new double[batch][totalLen];
        double[][] gotDetail = new double[batch][totalLen];
        try (BatchStreamingMODWT streaming = new BatchStreamingMODWT.Builder()
                .wavelet(wavelet)
                .boundary(mode)
                .levels(1)
                .build()) {
            for (int start = 0; start < totalLen; start += blockLen) {
                int end = Math.min(totalLen, start + blockLen);
                int len = end - start;
                double[][] block = new double[batch][len];
                for (int b = 0; b < batch; b++) {
                    System.arraycopy(full[b], start, block[b], 0, len);
                }
                BatchMODWT.SingleLevelResult out = streaming.processSingleLevel(block);
                for (int b = 0; b < batch; b++) {
                    System.arraycopy(out.approx()[b], 0, gotApprox[b], start, len);
                    System.arraycopy(out.detail()[b], 0, gotDetail[b], start, len);
                }
            }
        }

        // Compare entire arrays
        for (int b = 0; b < batch; b++) {
            assertArrayClose(gotApprox[b], refApprox[b], tol);
            assertArrayClose(gotDetail[b], refDetail[b], tol);
        }
    }

    @Test
    public void testMultiLevelZeroPadding_DB4_L3() {
        multiLevelStreamingParity(Daubechies.DB4, BoundaryMode.ZERO_PADDING, 2, 512, 64, 3, 1e-8);
    }

    @Test
    public void testMultiLevelSymmetric_Haar_L2() {
        multiLevelStreamingParity(new Haar(), BoundaryMode.SYMMETRIC, 5, 384, 96, 2, 1e-9);
    }

    private void multiLevelStreamingParity(DiscreteWavelet wavelet, BoundaryMode mode,
                                           int batch, int totalLen, int blockLen, int levels, double tol) {
        double[][] full = randomSignals(batch, totalLen, 77);

        // Reference full-signal multi-level
        MultiLevelMODWTTransform core = new MultiLevelMODWTTransform(wavelet, mode);
        double[][][] refDetails = new double[levels][batch][totalLen];
        double[][] refApprox = new double[batch][totalLen];
        for (int b = 0; b < batch; b++) {
            MultiLevelMODWTResult res = core.decompose(full[b], levels);
            for (int L = 1; L <= levels; L++) refDetails[L - 1][b] = res.getDetailCoeffsAtLevel(L);
            refApprox[b] = res.getApproximationCoeffs();
        }

        // SIMD streaming
        double[][][] gotDetails = new double[levels][batch][totalLen];
        double[][] gotApprox = new double[batch][totalLen];
        try (BatchStreamingMODWT streaming = new BatchStreamingMODWT.Builder()
                .wavelet(wavelet)
                .boundary(mode)
                .levels(levels)
                .build()) {
            for (int start = 0; start < totalLen; start += blockLen) {
                int end = Math.min(totalLen, start + blockLen);
                int len = end - start;
                double[][] block = new double[batch][len];
                for (int b = 0; b < batch; b++) {
                    System.arraycopy(full[b], start, block[b], 0, len);
                }
                BatchMODWT.MultiLevelResult out = streaming.processMultiLevel(block);
                for (int L = 0; L < levels; L++) {
                    for (int b = 0; b < batch; b++) {
                        System.arraycopy(out.detailPerLevel()[L][b], 0, gotDetails[L][b], start, len);
                    }
                }
                for (int b = 0; b < batch; b++) {
                    System.arraycopy(out.finalApprox()[b], 0, gotApprox[b], start, len);
                }
            }
        }

        // Compare
        for (int L = 0; L < levels; L++) {
            for (int b = 0; b < batch; b++) {
                assertArrayClose(gotDetails[L][b], refDetails[L][b], tol);
            }
        }
        for (int b = 0; b < batch; b++) assertArrayClose(gotApprox[b], refApprox[b], tol);
    }
}
