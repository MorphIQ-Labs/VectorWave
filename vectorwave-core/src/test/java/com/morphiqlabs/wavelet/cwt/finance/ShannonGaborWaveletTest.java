package com.morphiqlabs.wavelet.cwt.finance;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Shannon-Gabor wavelet implementation.
 */
class ShannonGaborWaveletTest {
    
    private static final double TOLERANCE = 1e-10;
    private ShannonGaborWavelet wavelet;
    
    @BeforeEach
    void setUp() {
        wavelet = new ShannonGaborWavelet(); // Default bandwidth fb=0.5
    }
    
    @Test
    @DisplayName("Should create Shannon wavelet with default bandwidth")
    void testCreateDefaultShannonGaborWavelet() {
        assertNotNull(wavelet);
        assertEquals("shan-gabor0.5-1.5", wavelet.name());
        assertEquals(0.5, wavelet.getBandwidth());
        assertEquals(1.5, wavelet.getCenterFrequencyParameter());
        assertFalse(wavelet.isComplex());
    }
    
    @Test
    @DisplayName("Should create Shannon wavelet with custom parameters")
    void testCreateCustomShannonGaborWavelet() {
        ShannonGaborWavelet custom = new ShannonGaborWavelet(1.0, 2.0);
        assertEquals("shan-gabor1.0-2.0", custom.name());
        assertEquals(1.0, custom.getBandwidth());
        assertEquals(2.0, custom.getCenterFrequencyParameter());
    }
    
    @Test
    @DisplayName("Should validate wavelet parameters")
    void testValidateParameters() {
        assertThrows(IllegalArgumentException.class, () -> new ShannonGaborWavelet(0, 1.5));
        assertThrows(IllegalArgumentException.class, () -> new ShannonGaborWavelet(-1, 1.5));
        assertThrows(IllegalArgumentException.class, () -> new ShannonGaborWavelet(0.5, 0));
        assertThrows(IllegalArgumentException.class, () -> new ShannonGaborWavelet(0.5, -1));
    }
    
    @Test
    @DisplayName("Should compute wavelet function values")
    void testWaveletFunction() {
        // Shannon wavelet at t=0: sqrt(fb) * sinc(0) * cos(0) = sqrt(0.5) * 1 * 1
        double psi0 = wavelet.psi(0.0);
        assertEquals(Math.sqrt(0.5), psi0, TOLERANCE);
        
        // Should be symmetric
        double psi1 = wavelet.psi(1.0);
        double psiMinus1 = wavelet.psi(-1.0);
        assertEquals(psi1, psiMinus1, TOLERANCE);
        
        // Should have oscillatory behavior (sinc-like)
        // Zero crossings should occur periodically
        boolean foundPositive = false;
        boolean foundNegative = false;
        for (double t = 0.1; t <= 10; t += 0.1) {
            double val = wavelet.psi(t);
            if (val > 0) foundPositive = true;
            if (val < 0) foundNegative = true;
        }
        assertTrue(foundPositive && foundNegative, "Should have oscillatory behavior");
        
        // Should decay as 1/t (test with points that aren't near zeros)
        double psi3 = Math.abs(wavelet.psi(3.0));
        double psi9 = Math.abs(wavelet.psi(9.0));
        assertTrue(psi9 < psi3); // Should decay
    }
    
    @Test
    @DisplayName("Should compute correct center frequency")
    void testCenterFrequency() {
        // Shannon wavelet center frequency: fc * fb
        double expectedFreq = 1.5 * 0.5;
        assertEquals(expectedFreq, wavelet.centerFrequency(), TOLERANCE);
        
        // Test with custom parameters
        ShannonGaborWavelet custom = new ShannonGaborWavelet(2.0, 3.0);
        assertEquals(6.0, custom.centerFrequency(), TOLERANCE);
    }
    
    @Test
    @DisplayName("Should have specified bandwidth")
    void testBandwidth() {
        assertEquals(0.5, wavelet.bandwidth(), TOLERANCE);
        
        // Narrowband version
        ShannonGaborWavelet narrow = new ShannonGaborWavelet(0.1, 1.5);
        assertEquals(0.1, narrow.bandwidth(), TOLERANCE);
        
        // Wideband version
        ShannonGaborWavelet wide = new ShannonGaborWavelet(2.0, 1.5);
        assertEquals(2.0, wide.bandwidth(), TOLERANCE);
    }
    
    @Test
    @DisplayName("Should properly discretize wavelet")
    void testDiscretization() {
        int numSamples = 256;
        double[] samples = wavelet.discretize(numSamples);
        
        assertNotNull(samples);
        assertEquals(numSamples, samples.length);
        
        // Should be centered and symmetric
        int center = numSamples / 2;
        
        // Center value should be maximum
        double maxVal = samples[center];
        for (int i = 0; i < numSamples; i++) {
            assertTrue(samples[i] <= maxVal);
        }
        
        // Check symmetry
        for (int i = 1; i < numSamples / 4; i++) {
            assertEquals(samples[center - i], samples[center + i], 
                Math.abs(samples[center + i]) * 1e-10);
        }
    }
    
    @Test
    @DisplayName("Should handle scaled wavelet function")
    void testScaledWaveletFunction() {
        double scale = 2.0;
        double translation = 1.0;
        
        // Test scaling property
        double t = 3.0;
        double scaledPsi = wavelet.psi(t, scale, translation);
        double expectedPsi = (1.0 / Math.sqrt(scale)) * wavelet.psi((t - translation) / scale);
        assertEquals(expectedPsi, scaledPsi, Math.abs(expectedPsi) * TOLERANCE);
    }
    
    @Test
    @DisplayName("Should analyze frequency bands in financial data")
    void testFrequencyBandAnalysis() {
        // Create a signal with multiple frequency components
        int N = 512;
        double[] signal = new double[N];
        double samplingRate = 100.0; // Hz
        
        // Add low frequency trend (0.5 Hz)
        for (int i = 0; i < N; i++) {
            double t = i / samplingRate;
            signal[i] = 100.0 + 10.0 * Math.sin(2 * Math.PI * 0.5 * t);
        }
        
        // Add mid frequency trading pattern (5 Hz)
        for (int i = 0; i < N; i++) {
            double t = i / samplingRate;
            signal[i] += 5.0 * Math.sin(2 * Math.PI * 5.0 * t);
        }
        
        // Add high frequency noise (20 Hz)
        for (int i = N/4; i < 3*N/4; i++) {
            double t = i / samplingRate;
            signal[i] += 2.0 * Math.sin(2 * Math.PI * 20.0 * t);
        }
        
        // Shannon wavelet configured for mid-frequency band should respond to 5Hz component
        ShannonGaborWavelet midBand = new ShannonGaborWavelet(2.0, 5.0 / 2.0); // Center at 5Hz
        
        // Simple convolution test around middle of signal
        double response = 0;
        int testPoint = N / 2;
        double scale = 1.0;
        for (int i = -20; i <= 20; i++) {
            if (testPoint + i >= 0 && testPoint + i < N) {
                response += Math.abs(signal[testPoint + i] * midBand.psi(i / scale));
            }
        }
        
        // Should have significant response
        assertTrue(response > 10.0);
    }
}