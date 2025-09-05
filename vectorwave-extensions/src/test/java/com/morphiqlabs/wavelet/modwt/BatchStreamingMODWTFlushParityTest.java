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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parity tests for BatchStreamingMODWT flush APIs versus
 * core transforms on explicitly extended signals.
 */
public class BatchStreamingMODWTFlushParityTest {

    private static double[][] randomSignals(int batch, int n, long seed) {
        Random rnd = new Random(seed);
        double[][] s = new double[batch][n];
        for (int b = 0; b < batch; b++) {
            for (int i = 0; i < n; i++) {
                s[b][i] = Math.cos(0.017 * (i + 2*b)) + 0.2 * rnd.nextGaussian();
            }
        }
        return s;
    }

    private static void assertClose(double[] a, double[] b, double tol) {
        assertEquals(a.length, b.length, "length");
        for (int i = 0; i < a.length; i++) {
            assertEquals(b[i], a[i], tol, "idx=" + i);
        }
    }

    @Test
    public void testFlushSingleLevel_ZERO_PADDING_Haar() {
        DiscreteWavelet w = new Haar();
        BoundaryMode mode = BoundaryMode.ZERO_PADDING;
        int batch = 3, N = 512, blockLen = 96, tailLen = 1; // Haar L1 histLen=1
        double tol = 1e-9;

        double[][] full = randomSignals(batch, N, 123);

        // Process stream across blocks
        try (BatchStreamingMODWT streaming = new BatchStreamingMODWT.Builder()
                .wavelet(w).boundary(mode).levels(1).build()) {
            for (int s = 0; s < N; s += blockLen) {
                int len = Math.min(blockLen, N - s);
                double[][] blk = new double[batch][len];
                for (int b = 0; b < batch; b++) System.arraycopy(full[b], s, blk[b], 0, len);
                streaming.processSingleLevel(blk);
            }
            BatchMODWT.SingleLevelResult tail = streaming.flushSingleLevel(tailLen);

            // Reference: extend with zeros and run core
            double[][] ext = new double[batch][N + tailLen];
            for (int b = 0; b < batch; b++) System.arraycopy(full[b], 0, ext[b], 0, N); // tail already zeros
            MODWTTransform core = new MODWTTransform(w, mode);
            for (int b = 0; b < batch; b++) {
                MODWTResult res = core.forward(ext[b]);
                double[] refA = slice(res.approximationCoeffs(), N, tailLen);
                double[] refD = slice(res.detailCoeffs(), N, tailLen);
                assertClose(tail.approx()[b], refA, tol);
                assertClose(tail.detail()[b], refD, tol);
            }
        }
    }

    @Test
    public void testFlushSingleLevel_SYMMETRIC_DB4() {
        DiscreteWavelet w = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.SYMMETRIC;
        int batch = 2, N = 400, blockLen = 128, tailLen = 7; // DB4 L1 histLen=7
        double tol = 1e-8;

        double[][] full = randomSignals(batch, N, 456);

        try (BatchStreamingMODWT streaming = new BatchStreamingMODWT.Builder()
                .wavelet(w).boundary(mode).levels(1).build()) {
            for (int s = 0; s < N; s += blockLen) {
                int len = Math.min(blockLen, N - s);
                double[][] blk = new double[batch][len];
                for (int b = 0; b < batch; b++) System.arraycopy(full[b], s, blk[b], 0, len);
                streaming.processSingleLevel(blk);
            }
            BatchMODWT.SingleLevelResult tail = streaming.flushSingleLevel(tailLen);

            // Reference: extend by symmetric reflection (first reflection only)
            double[][] ext = new double[batch][N + tailLen];
            for (int b = 0; b < batch; b++) {
                System.arraycopy(full[b], 0, ext[b], 0, N);
                for (int t = 0; t < tailLen; t++) {
                    ext[b][N + t] = full[b][N - 1 - t];
                }
            }
            MODWTTransform core = new MODWTTransform(w, mode);
            for (int b = 0; b < batch; b++) {
                MODWTResult res = core.forward(ext[b]);
                double[] refA = slice(res.approximationCoeffs(), N, tailLen);
                double[] refD = slice(res.detailCoeffs(), N, tailLen);
                assertClose(tail.approx()[b], refA, tol);
                assertClose(tail.detail()[b], refD, tol);
            }
        }
    }

    @Test
    public void testFlushMultiLevel_ZERO_PADDING_DB4_L3() {
        DiscreteWavelet w = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.ZERO_PADDING;
        int batch = 2, N = 480, blockLen = 96, tailLen = 7, levels = 3; // min histLen across levels = 7
        double tol = 1e-8;

        double[][] full = randomSignals(batch, N, 999);

        try (BatchStreamingMODWT streaming = new BatchStreamingMODWT.Builder()
                .wavelet(w).boundary(mode).levels(levels).build()) {
            for (int s = 0; s < N; s += blockLen) {
                int len = Math.min(blockLen, N - s);
                double[][] blk = new double[batch][len];
                for (int b = 0; b < batch; b++) System.arraycopy(full[b], s, blk[b], 0, len);
                streaming.processMultiLevel(blk);
            }
            BatchMODWT.MultiLevelResult tail = streaming.flushMultiLevel(tailLen);

            // Reference: zero-extend and run core multi-level
            double[][] ext = new double[batch][N + tailLen];
            for (int b = 0; b < batch; b++) System.arraycopy(full[b], 0, ext[b], 0, N);
            MultiLevelMODWTTransform core = new MultiLevelMODWTTransform(w, mode);
            for (int b = 0; b < batch; b++) {
                MultiLevelMODWTResult res = core.decompose(ext[b], levels);
                for (int L = 0; L < levels; L++) {
                    double[] refD = slice(res.getDetailCoeffsAtLevel(L + 1), N, tailLen);
                    assertClose(tail.detailPerLevel()[L][b], refD, tol);
                }
                double[] refA = slice(res.getApproximationCoeffs(), N, tailLen);
                assertClose(tail.finalApprox()[b], refA, tol);
            }
        }
    }

    @Test
    public void testFlushMultiLevel_SYMMETRIC_Haar_L2() {
        DiscreteWavelet w = new Haar();
        BoundaryMode mode = BoundaryMode.SYMMETRIC;
        int batch = 4, N = 384, blockLen = 128, tailLen = 1, levels = 2; // min histLen across levels = 1 (Haar)
        double tol = 1e-9;

        double[][] full = randomSignals(batch, N, 321);

        try (BatchStreamingMODWT streaming = new BatchStreamingMODWT.Builder()
                .wavelet(w).boundary(mode).levels(levels).build()) {
            for (int s = 0; s < N; s += blockLen) {
                int len = Math.min(blockLen, N - s);
                double[][] blk = new double[batch][len];
                for (int b = 0; b < batch; b++) System.arraycopy(full[b], s, blk[b], 0, len);
                streaming.processMultiLevel(blk);
            }
            BatchMODWT.MultiLevelResult tail = streaming.flushMultiLevel(tailLen);

            // Reference: symmetric extend and run core multi-level
            double[][] ext = new double[batch][N + tailLen];
            for (int b = 0; b < batch; b++) {
                System.arraycopy(full[b], 0, ext[b], 0, N);
                for (int t = 0; t < tailLen; t++) ext[b][N + t] = full[b][N - 1 - t];
            }
            MultiLevelMODWTTransform core = new MultiLevelMODWTTransform(w, mode);
            for (int b = 0; b < batch; b++) {
                MultiLevelMODWTResult res = core.decompose(ext[b], levels);
                for (int L = 0; L < levels; L++) {
                    double[] refD = slice(res.getDetailCoeffsAtLevel(L + 1), N, tailLen);
                    assertClose(tail.detailPerLevel()[L][b], refD, tol);
                }
                double[] refA = slice(res.getApproximationCoeffs(), N, tailLen);
                assertClose(tail.finalApprox()[b], refA, tol);
            }
        }
    }

    private static double[] slice(double[] a, int start, int len) {
        double[] out = new double[len];
        System.arraycopy(a, start, out, 0, len);
        return out;
    }
}
