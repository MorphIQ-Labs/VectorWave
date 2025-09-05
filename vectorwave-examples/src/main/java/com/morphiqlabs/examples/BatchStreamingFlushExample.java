package com.morphiqlabs.examples;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.extensions.modwt.BatchMODWT;
import com.morphiqlabs.wavelet.extensions.modwt.BatchStreamingMODWT;

import java.util.Random;

/**
 * Minimal example demonstrating BatchStreamingMODWT flush usage.
 */
public class BatchStreamingFlushExample {
    public BatchStreamingFlushExample() {}
    public static void main(String[] args) {
        int batch = 2;
        int N = 256;
        int blockLen = 64;
        double[][] data = randomSignals(batch, N, 42);

        try (BatchStreamingMODWT streaming = new BatchStreamingMODWT.Builder()
                .wavelet(Daubechies.DB4)
                .boundary(BoundaryMode.SYMMETRIC)
                .levels(2)
                .build()) {

            for (int s = 0; s < N; s += blockLen) {
                int len = Math.min(blockLen, N - s);
                double[][] blk = new double[batch][len];
                for (int b = 0; b < batch; b++) System.arraycopy(data[b], s, blk[b], 0, len);
                BatchMODWT.MultiLevelResult out = streaming.processMultiLevel(blk);
                // ... consume out.detailPerLevel() / out.finalApprox() ...
            }

            int minTail = streaming.getMinFlushTailLength(); // e.g., 7 for DB4 at level 1
            BatchMODWT.MultiLevelResult tail = streaming.flushMultiLevel(minTail);
            // ... consume tail just like a normal block ...
            System.out.println("Emitted symmetric tail of length: " + minTail);
        }
    }

    private static double[][] randomSignals(int batch, int n, long seed) {
        Random rnd = new Random(seed);
        double[][] s = new double[batch][n];
        for (int b = 0; b < batch; b++) {
            for (int i = 0; i < n; i++) s[b][i] = Math.sin(0.02 * (i + b)) + 0.1 * rnd.nextGaussian();
        }
        return s;
    }
}
