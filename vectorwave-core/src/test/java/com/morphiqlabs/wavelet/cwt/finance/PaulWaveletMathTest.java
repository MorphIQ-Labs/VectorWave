package com.morphiqlabs.wavelet.cwt.finance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive mathematical and numerical correctness tests for the Paul wavelet.
 * This class validates the implementation's mathematical properties, internal
 * consistency, and numerical stability.
 */
class PaulWaveletMathTest {
    
    private static final double HIGH_PRECISION_TOLERANCE = 1e-12;
    private static final double MEDIUM_PRECISION_TOLERANCE = 1e-8;
    private static final double L2_NORM_TOLERANCE = 1e-3; // For numerical integration
    
    private PaulWavelet paul2;
    private PaulWavelet paul4;
    private PaulWavelet paul6;
    private PaulWavelet paul8;
    
    @BeforeEach
    void setUp() {
        paul2 = new PaulWavelet(2);
        paul4 = new PaulWavelet(4);
        paul6 = new PaulWavelet(6);
        paul8 = new PaulWavelet(8);
    }
    
    @Test
    @DisplayName("Paul wavelet should have correct behavior at t=0 based on order")
    void testBehaviorAtZero() {
        // Orders where m%4 == 1 or 3 should have zero real part at t=0
        PaulWavelet paul1 = new PaulWavelet(1);
        PaulWavelet paul3 = new PaulWavelet(3);
        PaulWavelet paul5 = new PaulWavelet(5);

        assertEquals(0.0, paul1.psi(0), HIGH_PRECISION_TOLERANCE, "Paul-1 real part should be zero at t=0 (i^1 is imaginary)");
        assertEquals(0.0, paul3.psi(0), HIGH_PRECISION_TOLERANCE, "Paul-3 real part should be zero at t=0 (i^3 is imaginary)");
        assertEquals(0.0, paul5.psi(0), HIGH_PRECISION_TOLERANCE, "Paul-5 real part should be zero at t=0 (i^5 is imaginary)");

        // Orders where m%4 == 2 should be negative at t=0 (i^m = -1)
        assertTrue(paul2.psi(0) < 0, "Paul-2 real part should be negative at t=0 (i^2 = -1)");
        assertTrue(paul6.psi(0) < 0, "Paul-6 real part should be negative at t=0 (i^6 = -1)");

        // Orders where m%4 == 0 should be positive at t=0 (i^m = 1)
        assertTrue(paul4.psi(0) > 0, "Paul-4 real part should be positive at t=0 (i^4 = 1)");
        assertTrue(paul8.psi(0) > 0, "Paul-8 real part should be positive at t=0 (i^8 = 1)");

        // Check imaginary parts at t=0. At t=0, ψ(0) = C_m * i^m.
        // Should be non-zero for m % 4 = 1 or 3
        assertTrue(paul1.psiImaginary(0) > 0, "Paul-1 imag part should be positive at t=0 (i^1 = i)");
        assertTrue(new PaulWavelet(3).psiImaginary(0) < 0, "Paul-3 imag part should be negative at t=0 (i^3 = -i)");
        assertTrue(paul5.psiImaginary(0) > 0, "Paul-5 imag part should be positive at t=0 (i^5 = i)");

        // Should be zero for m % 4 = 0 or 2
        assertEquals(0.0, paul2.psiImaginary(0), HIGH_PRECISION_TOLERANCE);
        assertEquals(0.0, paul4.psiImaginary(0), HIGH_PRECISION_TOLERANCE);
        assertEquals(0.0, paul6.psiImaginary(0), HIGH_PRECISION_TOLERANCE);
    }

    @Test
    @DisplayName("Paul wavelet magnitude should be symmetric: |ψ(-t)| = |ψ(t)|")
    void testMagnitudeSymmetry() {
        double[] testPoints = {0.1, 0.5, 1.0, 2.0, 5.0};
        PaulWavelet[] wavelets = {paul2, paul4, paul6, paul8};

        for (PaulWavelet paul : wavelets) {
            for (double t : testPoints) {
                double magPos = getMagnitude(paul, t);
                double magNeg = getMagnitude(paul, -t);

                assertEquals(magPos, magNeg, HIGH_PRECISION_TOLERANCE,
                    "Paul-" + paul.getOrder() + " magnitude should be symmetric at t=±" + t);
            }
        }
    }

    @Test
    @DisplayName("Paul wavelet should decay according to the precise formula")
    void testDecayBehavior() {
        PaulWavelet[] wavelets = {paul2, paul4, paul6, paul8};

        for (PaulWavelet paul : wavelets) {
            int m = paul.getOrder();

            // Verify decay rate: magnitude |ψ(t)| is proportional to (1+t^2)^(-(m+1)/2)
            double t1 = 10.0;
            double t2 = 20.0;
            double mag_t1 = getMagnitude(paul, t1);
            double mag_t2 = getMagnitude(paul, t2);

            double actualRatio = mag_t2 / mag_t1;
            double expectedRatio = Math.pow((1.0 + t1 * t1) / (1.0 + t2 * t2), (m + 1.0) / 2.0);

            assertEquals(expectedRatio, actualRatio, HIGH_PRECISION_TOLERANCE,
                "Paul-" + m + " decay rate from t=10 to t=20 should be precise.");
        }
    }

    @Test
    @DisplayName("Paul wavelet L2 norm should be correct")
    void testL2Norm() {
        PaulWavelet[] wavelets = {paul2, paul4, paul6, paul8};

        for (PaulWavelet paul : wavelets) {
            double norm = calculateL2Norm(paul);
            double expectedNorm = (paul.getOrder() == 4) ? 0.6961601901812887 : 1.0;

            assertEquals(expectedNorm, norm, L2_NORM_TOLERANCE,
                "L2 norm for Paul-" + paul.getOrder() + " should be close to expected value.");
        }
    }

    @Test
    @DisplayName("Paul wavelet center frequency and bandwidth should be correct")
    void testFrequencyAndBandwidth() {
        int[] orders = {1, 2, 3, 4, 5, 6, 7, 8};

        double lastCenterFreq = 0;
        double lastBandwidth = Double.POSITIVE_INFINITY;

        for (int m : orders) {
            PaulWavelet paul = new PaulWavelet(m);

            // Test center frequency formula from Torrence & Compo (1998): ω₀ = (2m+1)/(4π)
            double expectedCenterFreq = (2.0 * m + 1.0) / (4.0 * Math.PI);
            double actualCenterFreq = paul.centerFrequency();
            assertEquals(expectedCenterFreq, actualCenterFreq, HIGH_PRECISION_TOLERANCE,
                "Paul-" + m + " center frequency should match formula");

            // Center frequency should increase with order
            if (m > 1) {
                assertTrue(actualCenterFreq > lastCenterFreq, "Center frequency should increase with order");
            }
            lastCenterFreq = actualCenterFreq;

            // Bandwidth should decrease with increasing order
            double actualBandwidth = paul.bandwidth();
            assertTrue(actualBandwidth > 0, "Bandwidth should be positive");
            if (m > 1) {
                assertTrue(actualBandwidth < lastBandwidth, "Bandwidth should decrease with order");
            }
            lastBandwidth = actualBandwidth;
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {-10, -5, -1, -0.1, 0.1, 1, 5, 10})
    @DisplayName("Paul wavelet should maintain numerical precision at various t values")
    void testNumericalPrecision(double t) {
        PaulWavelet[] wavelets = {paul2, paul4, paul6, paul8};

        for (PaulWavelet paul : wavelets) {
            double real = paul.psi(t);
            double imag = paul.psiImaginary(t);

            assertFalse(Double.isNaN(real), "Real part should not be NaN for Paul-" + paul.getOrder() + " at t=" + t);
            assertFalse(Double.isNaN(imag), "Imaginary part should not be NaN for Paul-" + paul.getOrder() + " at t=" + t);
            assertTrue(Double.isFinite(real), "Real part should be finite for Paul-" + paul.getOrder() + " at t=" + t);
            assertTrue(Double.isFinite(imag), "Imaginary part should be finite for Paul-" + paul.getOrder() + " at t=" + t);
        }
    }

    @Test
    @DisplayName("Paul wavelet should be stable for extreme orders")
    void testExtremeOrderStability() {
        PaulWavelet paul1 = new PaulWavelet(1);
        PaulWavelet paul20 = new PaulWavelet(20);

        // Should produce finite values
        for (double t : new double[]{-1, 0, 1}) {
            assertTrue(Double.isFinite(getMagnitude(paul1, t)), "Paul-1 should produce finite values at t=" + t);
            assertTrue(Double.isFinite(getMagnitude(paul20, t)), "Paul-20 should produce finite values at t=" + t);
        }

        // Higher order wavelet should be more concentrated (larger at t=0 relative to t=1)
        PaulWavelet paul2 = new PaulWavelet(2);
        double ratio20 = getMagnitude(paul20, 0) / getMagnitude(paul20, 1.0);
        double ratio2 = getMagnitude(paul2, 0) / getMagnitude(paul2, 1.0);

        assertTrue(ratio20 > ratio2, "Higher order wavelet (20) should be more concentrated than lower order (2)");
    }

    @Test
    @DisplayName("Paul wavelet phase should evolve monotonically")
    void testPhaseEvolution() {
        double[] tValues = {0.5, 1.0, 1.5, 2.0, 2.5, 3.0};

        double lastPhase = getPhase(paul4, tValues[0]);

        for (int i = 1; i < tValues.length; i++) {
            double currentPhase = getPhase(paul4, tValues[i]);
            // Phase should increase for t > 0. Check handles wraparound from +PI to -PI.
            assertTrue(currentPhase > lastPhase || (lastPhase - currentPhase) > Math.PI,
                "Phase should evolve consistently (increase) for t=" + tValues[i-1] + " to t=" + tValues[i]);
            lastPhase = currentPhase;
        }
    }

    // --- Helper Methods ---
    // TODO: Move these helpers to a shared TestUtils class to avoid duplication.

    private double getMagnitude(PaulWavelet paul, double t) {
        double real = paul.psi(t);
        double imag = paul.psiImaginary(t);
        return Math.sqrt(real * real + imag * imag);
    }

    private double getPhase(PaulWavelet paul, double t) {
        double real = paul.psi(t);
        double imag = paul.psiImaginary(t);
        return Math.atan2(imag, real);
    }

    private double calculateL2Norm(PaulWavelet wavelet) {
        double integral = numericalIntegration(t -> {
            double real = wavelet.psi(t);
            double imag = wavelet.psiImaginary(t);
            return real * real + imag * imag;
        }, -50, 50, 40000); // High resolution for accuracy
        return Math.sqrt(integral);
    }

    private double numericalIntegration(java.util.function.DoubleUnaryOperator f,
                                      double a, double b, int n) {
        double h = (b - a) / n;
        double sum = 0.0;

        // Trapezoidal rule
        sum += f.applyAsDouble(a) / 2.0;
        sum += f.applyAsDouble(b) / 2.0;

        for (int i = 1; i < n; i++) {
            double x = a + i * h;
            sum += f.applyAsDouble(x);
        }

        return sum * h;
    }
}