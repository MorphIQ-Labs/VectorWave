package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.fft.CoreFFT;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

class DebugRfftParityTest {
    @Test
    void printRfftN8() {
        String prev = System.getProperty("vectorwave.fft.realOptimized");
        try {
            int n = 8;
            double[] sig = {1,2,3,4,5,6,7,8};
            System.setProperty("vectorwave.fft.realOptimized", "true");
            double[] opt = CoreFFT.rfft(sig);
            System.setProperty("vectorwave.fft.realOptimized", "false");
            double[] def = CoreFFT.rfft(sig);
            System.out.println("opt: " + Arrays.toString(opt));
            System.out.println("def: " + Arrays.toString(def));

            // random length-64 check
            java.util.Random rnd = new java.util.Random(42);
            int n2 = 64;
            double[] sig2 = new double[n2];
            for (int i = 0; i < n2; i++) sig2[i] = rnd.nextDouble() * 2 - 1;
            System.setProperty("vectorwave.fft.realOptimized", "true");
            double[] opt2 = CoreFFT.rfft(sig2);
            System.setProperty("vectorwave.fft.realOptimized", "false");
            double[] def2 = CoreFFT.rfft(sig2);
            double maxDiff = 0.0; int idxMax = -1;
            for (int i = 0; i < opt2.length; i++) {
                double d = Math.abs(opt2[i] - def2[i]);
                if (d > maxDiff) { maxDiff = d; idxMax = i; }
            }
            System.out.println("n=64 maxDiff=" + maxDiff + " at index " + idxMax +
                    ", opt=" + opt2[idxMax] + ", def=" + def2[idxMax]);
        } finally {
            if (prev != null) System.setProperty("vectorwave.fft.realOptimized", prev);
            else System.clearProperty("vectorwave.fft.realOptimized");
        }
    }
}
