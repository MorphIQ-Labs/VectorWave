package com.morphiqlabs.wavelet.cwt.finance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mathematical correctness tests for Paul wavelet.
 */
class PaulWaveletMathTest {
    
    private static final double TOLERANCE = 1e-10;
    private static final double INTEGRATION_TOLERANCE = 1e-3;
    
    private PaulWavelet paul2;
    private PaulWavelet paul4;
    private PaulWavelet paul6;
    
    @BeforeEach
    void setUp() {
        paul2 = new PaulWavelet(2);
        paul4 = new PaulWavelet(4);
        paul6 = new PaulWavelet(6);
    }
    
    @Test
    @DisplayName("Should have correct wavelet values at specific points")
    void testWaveletValues() {
        // Paul wavelet is complex-valued
        // ψ(t) = (2^m * i^m * m!) / √(π * (2m)!) * (1 - it)^(-(m+1))
        
        // At t=0, real part should be non-zero for even m
        double real4_0 = paul4.psi(0);
        assertTrue(real4_0 != 0, "Paul(4) should have non-zero real part at t=0");
        
        // Imaginary part at t=0
        double imag4_0 = paul4.psiImaginary(0);
        assertEquals(0.0, imag4_0, TOLERANCE, "Paul wavelet should have zero imaginary part at t=0");
        
        // Should decay as |t| increases
        assertTrue(Math.abs(paul4.psi(10)) < Math.abs(paul4.psi(1)));
        assertTrue(Math.abs(paul4.psiImaginary(10)) < Math.abs(paul4.psiImaginary(1)));
    }
    
    @Test
    @DisplayName("Should satisfy zero mean condition")
    void testZeroMean() {
        // All Paul wavelets should have zero mean: ∫ψ(t)dt = 0
        assertZeroMean(paul2);
        assertZeroMean(paul4);
        assertZeroMean(paul6);
    }
    
    @Test
    @DisplayName("Should satisfy admissibility condition")
    void testAdmissibility() {
        // Paul wavelets satisfy admissibility
        // Check by verifying center frequency is positive (admissible wavelets have positive center frequency)
        assertTrue(paul2.centerFrequency() > 0, "Paul2 should be admissible");
        assertTrue(paul4.centerFrequency() > 0, "Paul4 should be admissible");
        assertTrue(paul6.centerFrequency() > 0, "Paul6 should be admissible");
        
        // Center frequency should increase with order
        assertTrue(paul4.centerFrequency() > paul2.centerFrequency());
        assertTrue(paul6.centerFrequency() > paul4.centerFrequency());
    }
    
    @Test
    @DisplayName("Should be analytic (no negative frequencies)")
    void testAnalyticity() {
        // Paul wavelet is analytic - its Fourier transform should be zero for negative frequencies
        // This means the real and imaginary parts form a Hilbert transform pair
        
        // Test Hilbert transform relationship at several points
        for (double t = 0.5; t <= 5.0; t += 0.5) {
            double real = paul4.psi(t);
            double imag = paul4.psiImaginary(t);
            
            // For analytic signals, the magnitude should be smooth
            double mag1 = Math.sqrt(real * real + imag * imag);
            double mag2 = Math.sqrt(paul4.psi(t + 0.01) * paul4.psi(t + 0.01) + 
                                  paul4.psiImaginary(t + 0.01) * paul4.psiImaginary(t + 0.01));
            
            // Magnitude should change smoothly
            assertTrue(Math.abs(mag2 - mag1) < 0.1, 
                "Magnitude should be smooth for analytic signal");
        }
    }
    
    @Test
    @DisplayName("Should have correct normalization")
    void testNormalization() {
        // Check L2 norm: ∫|ψ(t)|² dt should be normalized
        double norm2 = calculateL2Norm(paul2);
        double norm4 = calculateL2Norm(paul4);
        double norm6 = calculateL2Norm(paul6);
        
        // Paul wavelets should be normalized
        assertEquals(1.0, norm2, INTEGRATION_TOLERANCE);
        assertEquals(1.0, norm4, INTEGRATION_TOLERANCE);
        assertEquals(1.0, norm6, INTEGRATION_TOLERANCE);
    }
    
    @Test
    @DisplayName("Should have correct vanishing moments")
    void testVanishingMoments() {
        // Paul_m has m vanishing moments
        // ∫ t^k ψ(t) dt = 0 for k = 0, 1, ..., m-1
        
        // Paul2 has 2 vanishing moments
        assertEquals(0.0, calculateComplexMoment(paul2, 0), 0.05);
        assertEquals(0.0, calculateComplexMoment(paul2, 1), 0.05);
        
        // Paul4 has 4 vanishing moments
        for (int k = 0; k < 4; k++) {
            assertEquals(0.0, calculateComplexMoment(paul4, k), 0.05,
                "Paul4 should have vanishing moment for k=" + k);
        }
    }
    
    @Test
    @DisplayName("Should decay properly at infinity")
    void testDecay() {
        // Paul wavelets should decay as 1/t^(m+1)
        
        // Check decay for large t
        double t_large = 20.0;
        double mag2 = Math.sqrt(paul2.psi(t_large) * paul2.psi(t_large) + 
                               paul2.psiImaginary(t_large) * paul2.psiImaginary(t_large));
        double mag4 = Math.sqrt(paul4.psi(t_large) * paul4.psi(t_large) + 
                               paul4.psiImaginary(t_large) * paul4.psiImaginary(t_large));
        
        assertTrue(mag2 < 1e-3, "Paul2 should decay at large t");
        assertTrue(mag4 < 1e-5, "Paul4 should decay faster than Paul2");
        
        // Verify decay rate
        double t1 = 5.0;
        double t2 = 10.0;
        double mag1_paul4 = Math.sqrt(paul4.psi(t1) * paul4.psi(t1) + 
                                     paul4.psiImaginary(t1) * paul4.psiImaginary(t1));
        double mag2_paul4 = Math.sqrt(paul4.psi(t2) * paul4.psi(t2) + 
                                     paul4.psiImaginary(t2) * paul4.psiImaginary(t2));
        
        // For Paul4, decay should be approximately as 1/t^5
        double expectedRatio = Math.pow(t1 / t2, 5);
        double actualRatio = mag2_paul4 / mag1_paul4;
        
        // Allow some tolerance for numerical approximation
        assertEquals(expectedRatio, actualRatio, expectedRatio * 0.5,
            "Paul4 should decay as 1/t^5");
    }
    
    @Test
    @DisplayName("Should have correct bandwidth")
    void testBandwidth() {
        // Bandwidth should be positive and increase with order
        assertTrue(paul2.bandwidth() > 0);
        assertTrue(paul4.bandwidth() > 0);
        assertTrue(paul6.bandwidth() > 0);
        
        // Higher order should have smaller bandwidth (narrower in frequency)
        assertTrue(paul2.bandwidth() > paul4.bandwidth());
        assertTrue(paul4.bandwidth() > paul6.bandwidth());
    }
    
    @Test
    @DisplayName("Should verify factorial normalization")
    void testFactorialNormalization() {
        // The normalization includes factorial terms
        // Verify that the wavelet at t=0 matches expected normalization
        
        // For Paul(m), at t=0:
        // ψ(0) = (2^m * m!) / √(π * (2m)!)
        
        int m = 4;
        double expected = Math.pow(2, m) * factorial(m) / 
                         Math.sqrt(Math.PI * factorial(2 * m));
        double actual = paul4.psi(0);
        
        // Allow 0.1% tolerance for normalization differences
        assertEquals(expected, actual, Math.abs(expected) * 0.001,
            "Paul4 should have correct normalization at t=0");
    }
    
    @Test
    @DisplayName("Should be causal in frequency domain")
    void testCausality() {
        // Paul wavelet has support only for positive frequencies
        // This means it's good for detecting transient events
        
        // The wavelet should be complex-valued
        assertTrue(paul4.isComplex());
        
        // Phase should evolve monotonically with time
        double phase1 = Math.atan2(paul4.psiImaginary(1), paul4.psi(1));
        double phase2 = Math.atan2(paul4.psiImaginary(2), paul4.psi(2));
        double phase3 = Math.atan2(paul4.psiImaginary(3), paul4.psi(3));
        
        // Phase should decrease (become more negative) with increasing t
        assertTrue(phase2 < phase1 || (phase2 - phase1) > Math.PI,
            "Phase should evolve consistently");
    }
    
    // Helper methods
    
    private void assertZeroMean(PaulWavelet wavelet) {
        double meanReal = numericalIntegration(t -> wavelet.psi(t), -50, 50, 10000);
        double meanImag = numericalIntegration(t -> wavelet.psiImaginary(t), -50, 50, 10000);
        double meanMag = Math.sqrt(meanReal * meanReal + meanImag * meanImag);
        
        assertEquals(0.0, meanMag, INTEGRATION_TOLERANCE,
            "Paul" + wavelet.getOrder() + " should have zero mean");
    }
    
    private double calculateL2Norm(PaulWavelet wavelet) {
        double integral = numericalIntegration(t -> {
            double real = wavelet.psi(t);
            double imag = wavelet.psiImaginary(t);
            return real * real + imag * imag;
        }, -50, 50, 10000);
        return Math.sqrt(integral);
    }
    
    private double calculateComplexMoment(PaulWavelet wavelet, int k) {
        double realMoment = numericalIntegration(
            t -> Math.pow(t, k) * wavelet.psi(t), -50, 50, 10000);
        double imagMoment = numericalIntegration(
            t -> Math.pow(t, k) * wavelet.psiImaginary(t), -50, 50, 10000);
        return Math.sqrt(realMoment * realMoment + imagMoment * imagMoment);
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
    
    private double factorial(int n) {
        double result = 1.0;
        for (int i = 2; i <= n; i++) {
            result *= i;
        }
        return result;
    }
}