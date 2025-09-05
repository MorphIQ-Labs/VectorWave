package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for Issue 006: Add MODWT tests for reconstruction and energy preservation.
 *
 * - Verify perfect reconstruction with periodic boundaries for {Haar, DB4, DB8}
 * - Verify energy(signal) ≈ energy(approx) + sum_j energy(detail_j) in periodic mode
 * - Evaluate on deterministic (sinusoid, impulse) and random signals
 */
class MODWTPerfectReconstructionEnergyTest {

    private static final double TOL = 1e-9;

    static class Case {
        final String name;
        final Wavelet wavelet;
        final int length;

        Case(String name, Wavelet wavelet, int length) {
            this.name = name;
            this.wavelet = wavelet;
            this.length = length;
        }

        @Override public String toString() {
            return name + "-N" + length;
        }
    }

    static Stream<Case> cases() {
        List<Case> list = new ArrayList<>();
        // Wavelets: Haar, DB4, DB8; Lengths: 257, 1024
        Wavelet[] wavelets = new Wavelet[]{new Haar(), Daubechies.DB4, Daubechies.DB8};
        int[] lengths = new int[]{257, 1024};
        for (Wavelet w : wavelets) {
            for (int n : lengths) {
                String name = w.name().toLowerCase();
                list.add(new Case(name, w, n));
            }
        }
        return list.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    @DisplayName("Periodic: perfect reconstruction and energy equality (sinusoid)")
    void testSinusoid(Case c) {
        double[] signal = makeSinusoid(c.length);
        runReconstructAndEnergyChecks(c.wavelet, signal);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    @DisplayName("Periodic: perfect reconstruction and energy equality (impulse)")
    void testImpulse(Case c) {
        double[] signal = makeImpulse(c.length);
        runReconstructAndEnergyChecks(c.wavelet, signal);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cases")
    @DisplayName("Periodic: perfect reconstruction and energy equality (random)")
    void testRandom(Case c) {
        double[] signal = makeRandom(c.length, 42L);
        runReconstructAndEnergyChecks(c.wavelet, signal);
    }

    private void runReconstructAndEnergyChecks(Wavelet wavelet, double[] signal) {
        MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(wavelet, BoundaryMode.PERIODIC);
        int levels = t.getMaximumLevels(signal.length);
        if (levels < 1) {
            // Nothing to test if we cannot decompose at least one level
            return;
        }
        MultiLevelMODWTResult r = t.decompose(signal, levels);

        // Perfect reconstruction
        double[] recon = t.reconstruct(r);
        assertArrayEquals(signal, recon, TOL, "Reconstruction must match original signal");

        // Energy equality: energy(signal) ≈ energy(approx) + sum_j energy(detail_j)
        double signalEnergy = energy(signal);
        double totalCoeffEnergy = r.getTotalEnergy();
        double delta = Math.abs(signalEnergy - totalCoeffEnergy);
        double relTol = 1e-9; // allow tiny relative error for large N
        double absTol = 1e-8; // and a small absolute floor
        if (!(delta <= absTol || delta <= relTol * Math.max(1.0, signalEnergy))) {
            assertEquals(signalEnergy, totalCoeffEnergy, TOL,
                    "Total coefficient energy must equal signal energy");
        }
    }

    private static double[] makeSinusoid(int n) {
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            // Two tones to exercise multiple bands
            x[i] = Math.sin(2 * Math.PI * i / n) + 0.5 * Math.sin(2 * Math.PI * 8 * i / n);
        }
        return x;
    }

    private static double[] makeImpulse(int n) {
        double[] x = new double[n];
        if (n > 0) x[n / 2] = 1.0;
        return x;
    }

    private static double[] makeRandom(int n, long seed) {
        Random rnd = new Random(seed);
        double[] x = new double[n];
        for (int i = 0; i < n; i++) x[i] = rnd.nextDouble() * 2 - 1; // Uniform [-1,1]
        return x;
    }

    private static double energy(double[] a) {
        double e = 0.0;
        for (double v : a) e += v * v;
        return e;
    }
}
