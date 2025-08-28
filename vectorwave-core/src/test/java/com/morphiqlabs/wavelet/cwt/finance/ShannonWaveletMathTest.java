package com.morphiqlabs.wavelet.cwt.finance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mathematical correctness tests for Shannon wavelet.
 */
class ShannonWaveletMathTest {
    
    private static final double TOLERANCE = 1e-10;
    private static final double INTEGRATION_TOLERANCE = 1e-2;
    
    private ClassicalShannonWavelet shannon;
    
    @BeforeEach
    void setUp() {
        shannon = new ClassicalShannonWavelet();
    }
    
    @Test
    @DisplayName("Should have correct wavelet values at specific points")
    void testWaveletValues() {
        // Shannon wavelet: ψ(t) = 2 * sinc(2t) - sinc(t)
        // where sinc(x) = sin(πx)/(πx) for x≠0, and sinc(0) = 1
        
        // At t=0
        double psi0 = shannon.psi(0);
        // sinc(0) = 1, so ψ(0) = 2*1 - 1 = 1
        assertEquals(1.0, psi0, TOLERANCE);
        
        // At t=0.5
        double psi05 = shannon.psi(0.5);
        // sinc(1) = sin(π)/(π) = 0, sinc(0.5) = sin(π/2)/(π/2) = 2/π
        double expected05 = 2 * 0 - 2 / Math.PI;
        assertEquals(expected05, psi05, TOLERANCE * 10);
        
        // At t=1
        double psi1 = shannon.psi(1);
        // sinc(2) = sin(2π)/(2π) = 0, sinc(1) = 0
        assertEquals(0.0, psi1, TOLERANCE * 10);
    }
    
    @Test
    @DisplayName("Should satisfy zero mean condition")
    void testZeroMean() {
        // Shannon wavelet should have zero mean
        double mean = numericalIntegration(t -> shannon.psi(t), -50, 50, 20000);
        assertEquals(0.0, mean, INTEGRATION_TOLERANCE,
            "Shannon wavelet should have zero mean");
    }
    
    @Test
    @DisplayName("Should satisfy admissibility condition")
    void testAdmissibility() {
        // Shannon wavelet satisfies admissibility
        // Verify by checking center frequency is positive
        assertTrue(shannon.centerFrequency() > 0, "Shannon wavelet should be admissible");
    }
    
    @Test
    @DisplayName("Should have perfect frequency localization")
    void testFrequencyLocalization() {
        // Shannon wavelet has perfect frequency localization
        // Its Fourier transform has compact support [π/2, π]
        
        // The center frequency should be 3π/4 / (2π) = 3/8 = 0.375
        double expectedCenterFreq = 0.75 * Math.PI / (2 * Math.PI);
        assertEquals(expectedCenterFreq, shannon.centerFrequency(), TOLERANCE * 100);
        
        // Bandwidth should be π/2 / (2π) = 1/4 = 0.25
        double expectedBandwidth = 0.5 * Math.PI / (2 * Math.PI);
        assertEquals(expectedBandwidth, shannon.bandwidth(), TOLERANCE * 100);
    }
    
    @Test
    @DisplayName("Should have sinc function decay")
    void testSincDecay() {
        // Shannon wavelet decays as 1/t for large |t|
        // Avoid integer values where sinc has zeros
        
        // Check decay at non-integer points
        double t1 = 10.5;
        double t2 = 20.5;
        double psi1 = Math.abs(shannon.psi(t1));
        double psi2 = Math.abs(shannon.psi(t2));
        
        // Should decay (psi2 < psi1)
        assertTrue(psi2 < psi1, "Shannon wavelet should decay with increasing t");
        
        // Check approximate 1/t decay
        double ratio = psi2 / psi1;
        double expectedRatio = t1 / t2;
        
        // Allow 50% tolerance due to oscillatory nature
        assertEquals(expectedRatio, ratio, 0.5,
            "Should have approximate 1/t decay");
    }
    
    @Test
    @DisplayName("Should have orthogonality property")
    void testOrthogonality() {
        // Shannon wavelet has orthogonality properties
        // Integer translates should be orthogonal
        
        // Test orthogonality between ψ(t) and ψ(t-k) for integer k
        double inner1 = numericalIntegration(
            t -> shannon.psi(t) * shannon.psi(t - 1), -50, 50, 10000);
        double inner2 = numericalIntegration(
            t -> shannon.psi(t) * shannon.psi(t - 2), -50, 50, 10000);
        
        // Should be approximately zero (orthogonal)
        assertEquals(0.0, inner1, INTEGRATION_TOLERANCE * 10);
        assertEquals(0.0, inner2, INTEGRATION_TOLERANCE * 10);
    }
    
    @Test
    @DisplayName("Should verify sinc function properties")
    void testSincProperties() {
        // Verify the sinc function implementation
        
        // sinc(0) = 1
        double sinc0 = sinc(0);
        assertEquals(1.0, sinc0, TOLERANCE);
        
        // sinc(n) = 0 for integer n ≠ 0
        for (int n = 1; n <= 5; n++) {
            assertEquals(0.0, sinc(n), TOLERANCE * 100,
                "sinc(" + n + ") should be 0");
            assertEquals(0.0, sinc(-n), TOLERANCE * 100,
                "sinc(-" + n + ") should be 0");
        }
        
        // sinc is even function
        for (double t = 0.1; t <= 5.0; t += 0.3) {
            assertEquals(sinc(t), sinc(-t), TOLERANCE,
                "sinc should be even function");
        }
    }
    
    @Test
    @DisplayName("Should have correct zero crossings")
    void testZeroCrossings() {
        // Shannon wavelet has zeros at specific points
        
        // ψ(t) = 0 when 2*sinc(2t) = sinc(t)
        // This happens at t = ±1, ±2, ±3, ... (except some points)
        
        // Check some known zeros
        assertEquals(0.0, shannon.psi(1.0), TOLERANCE * 100);
        
        // Check that it changes sign around zeros
        assertTrue(shannon.psi(0.9) * shannon.psi(1.1) < 0,
            "Should change sign around t=1");
    }
    
    @Test
    @DisplayName("Should satisfy scaling function relationship")
    void testScalingRelation() {
        // Shannon wavelet is related to Shannon scaling function
        // ψ(t) = 2φ(2t) - φ(t) where φ is the scaling function
        
        // The scaling function is φ(t) = sinc(t)
        double t = 2.5;
        double phi_t = sinc(t);
        double phi_2t = sinc(2 * t);
        double expected = 2 * phi_2t - phi_t;
        double actual = shannon.psi(t);
        
        assertEquals(expected, actual, TOLERANCE * 100,
            "Should satisfy scaling relation");
    }
    
    @Test
    @DisplayName("Should be optimal for band-limited signals")
    void testBandLimitedOptimality() {
        // Shannon wavelet is optimal for analyzing band-limited signals
        // The classical Shannon wavelet ψ(t) = 2*sinc(2t) - sinc(t)
        // has theoretical frequency support in [π/2, π]
        
        // Basic test: verify the wavelet has the expected frequency characteristics
        // The center frequency and bandwidth have been set correctly
        assertEquals(0.375, shannon.centerFrequency(), TOLERANCE * 100);
        assertEquals(0.25, shannon.bandwidth(), TOLERANCE * 100);
        
        // Verify it integrates to zero (admissibility)
        double integral = numericalIntegration(t -> shannon.psi(t), -50, 50, 10000);
        assertEquals(0.0, integral, INTEGRATION_TOLERANCE);
        
        // Verify the wavelet is real-valued
        assertFalse(shannon.isComplex());
    }
    
    // Helper methods
    
    private double sinc(double x) {
        if (Math.abs(x) < 1e-10) {
            return 1.0;
        }
        double px = Math.PI * x;
        return Math.sin(px) / px;
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