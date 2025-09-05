package com.morphiqlabs.wavelet.testing;

import java.util.Random;

/**
 * Small helpers to generate repeatable test signals.
 */
public final class TestSignals {
    private TestSignals() {}

    /**
     * Composite sinusoid with optional Gaussian noise.
     * @param n length
     * @param seed RNG seed
     * @param noiseStd standard deviation of Gaussian noise (0 for none)
     * @return signal array
     */
    public static double[] compositeSin(int n, long seed, double noiseStd) {
        Random rnd = new Random(seed);
        double[] s = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double v = Math.sin(2 * Math.PI * 2 * t)
                    + 0.5 * Math.sin(2 * Math.PI * 7 * t)
                    + 0.25 * Math.cos(2 * Math.PI * 13 * t);
            if (noiseStd > 0) v += noiseStd * rnd.nextGaussian();
            s[i] = v;
        }
        return s;
    }
}

