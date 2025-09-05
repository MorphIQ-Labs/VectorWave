package com.morphiqlabs.wavelet.cwt.finance;

import com.morphiqlabs.wavelet.api.ComplexContinuousWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

import static org.junit.jupiter.api.Assertions.*;

class PaulWaveletTest {
    
    private static final double TOLERANCE = 1e-10;
    private PaulWavelet wavelet;
    
    @BeforeEach
    void setUp() {
        wavelet = new PaulWavelet(); // Default order m=4
    }
    
    @Test
    @DisplayName("Should create Paul wavelet with default order")
    void testCreateDefaultPaulWavelet() {
        assertNotNull(wavelet);
        assertEquals("paul4", wavelet.name());
        assertEquals(4, wavelet.getOrder());
        assertTrue(wavelet.isComplex());
        assertTrue(wavelet instanceof ComplexContinuousWavelet);
    }
    
    @Test
    @DisplayName("Should create Paul wavelet with custom order")
    void testCreateCustomOrderPaulWavelet() {
        PaulWavelet paul6 = new PaulWavelet(6);
        assertEquals("paul6", paul6.name());
        assertEquals(6, paul6.getOrder());
    }
    
    @Test
    @DisplayName("Should validate wavelet order")
    void testValidateOrder() {
        assertThrows(InvalidArgumentException.class, () -> new PaulWavelet(0));
        assertThrows(InvalidArgumentException.class, () -> new PaulWavelet(-1));
        assertThrows(InvalidArgumentException.class, () -> new PaulWavelet(21)); // Too high
    }
    
    @Test
    @DisplayName("Should compute wavelet function values")
    void testWaveletFunction() {
        // Paul wavelet at t=0: real part should be positive (normalization factor)
        double psi0 = wavelet.psi(0.0);
        assertTrue(psi0 > 0);
        
        // Should decay to zero as |t| increases
        assertTrue(Math.abs(wavelet.psi(10.0)) < 1e-5);
        assertTrue(Math.abs(wavelet.psi(-10.0)) < 1e-5);
        
        // Imaginary part should be zero at t=0 (arctan(0) = 0)
        assertEquals(0.0, wavelet.psiImaginary(0.0), TOLERANCE);
        
        // Check complex nature: imaginary part should be antisymmetric
        double imag1 = wavelet.psiImaginary(1.0);
        double imagMinus1 = wavelet.psiImaginary(-1.0);
        assertEquals(-imag1, imagMinus1, TOLERANCE * Math.abs(imag1));
    }
    
    @Test
    @DisplayName("Should compute correct center frequency")
    void testCenterFrequency() {
        // Paul wavelet center frequency: f_c = (2m + 1) / (4π)
        double expectedFreq = (2 * 4 + 1) / (4 * Math.PI);
        assertEquals(expectedFreq, wavelet.centerFrequency(), 1e-10);
        
        // Test with different order
        PaulWavelet paul6 = new PaulWavelet(6);
        double expectedFreq6 = (2 * 6 + 1) / (4 * Math.PI);
        assertEquals(expectedFreq6, paul6.centerFrequency(), 1e-10);
    }
    
    @Test
    @DisplayName("Should have correct bandwidth")
    void testBandwidth() {
        // Paul wavelet is narrowband
        double bandwidth = wavelet.bandwidth();
        assertTrue(bandwidth > 0);
        assertTrue(bandwidth < 2.0); // Relatively narrow bandwidth
    }
    
    @Test
    @DisplayName("Should properly discretize wavelet")
    void testDiscretization() {
        int numSamples = 128;
        double[] samples = wavelet.discretize(numSamples);
        
        assertNotNull(samples);
        assertEquals(numSamples, samples.length);
        
        // Should be centered
        int center = numSamples / 2;
        
        // Values should decay away from center
        double maxMag = 0;
        for (int i = 0; i < numSamples; i++) {
            double mag = Math.abs(samples[i]);
            if (mag > maxMag) {
                maxMag = mag;
            }
        }
        
        // Check decay
        assertTrue(Math.abs(samples[0]) < maxMag * 0.01);
        assertTrue(Math.abs(samples[numSamples - 1]) < maxMag * 0.01);
    }
    
    @Test
    @DisplayName("Should handle scaled wavelet function")
    void testScaledWaveletFunction() {
        double scale = 2.0;
        double translation = 1.0;
        
        // Test scaling property: ψ(a,b)(t) = (1/√a) ψ((t-b)/a)
        double t = 3.0;
        double scaledPsi = wavelet.psi(t, scale, translation);
        double expectedPsi = (1.0 / Math.sqrt(scale)) * wavelet.psi((t - translation) / scale);
        assertEquals(expectedPsi, scaledPsi, TOLERANCE);
        
        // Same for imaginary part
        double scaledPsiImag = wavelet.psiImaginary(t, scale, translation);
        double expectedPsiImag = (1.0 / Math.sqrt(scale)) * wavelet.psiImaginary((t - translation) / scale);
        assertEquals(expectedPsiImag, scaledPsiImag, TOLERANCE);
    }
    
    @Test
    @DisplayName("Should detect asymmetry in financial patterns")
    void testAsymmetryDetection() {
        // Create asymmetric financial pattern (e.g., sharp drop followed by gradual recovery)
        int N = 256;
        double[] signal = new double[N];
        
        // Sharp drop at t=64
        for (int i = 0; i < N; i++) {
            if (i < 64) {
                signal[i] = 100.0; // Baseline
            } else if (i < 80) {
                signal[i] = 100.0 - 5.0 * (i - 64); // Sharp drop
            } else {
                signal[i] = 20.0 + 0.5 * (i - 80); // Gradual recovery
            }
        }
        
        // Paul wavelet should respond strongly to asymmetric patterns
        // This is a characteristic test - actual CWT computation would be done elsewhere
        double response = 0;
        for (int i = 60; i < 85; i++) {
            double t = (i - 72) / 8.0; // Center around drop, scale=8
            response += signal[i] * wavelet.psi(t);
        }
        
        // Should have significant response to asymmetric pattern
        assertTrue(Math.abs(response) > 10.0);
    }
}