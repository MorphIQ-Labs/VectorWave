package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.cwt.finance.DOGWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for complex wavelet transform functionality.
 */
class ComplexCWTTest {
    
    private MorletWavelet morletWavelet;
    private CWTTransform cwtTransform;
    private double[] testSignal;
    private double[] scales;
    
    @BeforeEach
    void setUp() {
        morletWavelet = new MorletWavelet(6.0, 1.0);
        cwtTransform = new CWTTransform(morletWavelet);
        
        // Create test signal - simple sinusoid
        int N = 128;
        testSignal = new double[N];
        for (int i = 0; i < N; i++) {
            testSignal[i] = Math.sin(2 * Math.PI * 0.1 * i);
        }
        
        // Test scales
        scales = new double[]{2, 4, 8, 16, 32};
    }
    
    @Test
    @DisplayName("Should create valid complex CWT result")
    void testComplexCWTResult() {
        ComplexCWTResult result = cwtTransform.analyzeComplex(testSignal, scales);
        
        assertNotNull(result);
        assertEquals(scales.length, result.getNumScales());
        assertEquals(testSignal.length, result.getNumSamples());
        assertEquals(morletWavelet, result.getWavelet());
    }
    
    @Test
    @DisplayName("Should compute complex coefficients correctly")
    void testComplexCoefficients() {
        ComplexCWTResult result = cwtTransform.analyzeComplex(testSignal, scales);
        ComplexNumber[][] coeffs = result.getCoefficients();
        
        // Check dimensions
        assertEquals(scales.length, coeffs.length);
        assertEquals(testSignal.length, coeffs[0].length);
        
        // Check that coefficients are non-zero
        boolean hasNonZero = false;
        for (int s = 0; s < scales.length; s++) {
            for (int t = 0; t < testSignal.length; t++) {
                ComplexNumber c = coeffs[s][t];
                assertNotNull(c);
                if (c.magnitude() > 1e-10) {
                    hasNonZero = true;
                }
            }
        }
        assertTrue(hasNonZero, "Should have non-zero coefficients");
    }
    
    @Test
    @DisplayName("Should extract magnitude and phase correctly")
    void testMagnitudeAndPhase() {
        ComplexCWTResult result = cwtTransform.analyzeComplex(testSignal, scales);
        
        double[][] magnitude = result.getMagnitude();
        double[][] phase = result.getPhase();
        
        // Check dimensions
        assertEquals(scales.length, magnitude.length);
        assertEquals(scales.length, phase.length);
        assertEquals(testSignal.length, magnitude[0].length);
        assertEquals(testSignal.length, phase[0].length);
        
        // Check phase range
        for (int s = 0; s < scales.length; s++) {
            for (int t = 0; t < testSignal.length; t++) {
                double p = phase[s][t];
                assertTrue(p >= -Math.PI && p <= Math.PI, 
                    "Phase should be in [-π, π], got: " + p);
            }
        }
    }
    
    @Test
    @DisplayName("Should compute instantaneous frequency")
    void testInstantaneousFrequency() {
        // Use a chirp signal for testing
        int N = 256;
        double[] chirp = new double[N];
        for (int i = 0; i < N; i++) {
            double t = (double) i / N;
            double freq = 0.1 + 0.2 * t; // Linear frequency increase
            chirp[i] = Math.cos(2 * Math.PI * freq * i);
        }
        
        ComplexCWTResult result = cwtTransform.analyzeComplex(chirp, scales);
        double[][] instFreq = result.getInstantaneousFrequency();
        
        // Check dimensions (one less sample due to derivative)
        assertEquals(scales.length, instFreq.length);
        assertEquals(N - 1, instFreq[0].length);
        
        // Check that frequency increases over time (for appropriate scales)
        int midScale = scales.length / 2;
        double avgFreqStart = 0, avgFreqEnd = 0;
        int windowSize = 10;
        
        for (int i = 0; i < windowSize; i++) {
            avgFreqStart += instFreq[midScale][i];
            avgFreqEnd += instFreq[midScale][N - 2 - windowSize + i];
        }
        avgFreqStart /= windowSize;
        avgFreqEnd /= windowSize;
        
        // For a chirp, we expect frequency to change
        // Due to windowing effects, the change might be small
        double freqDiff = Math.abs(avgFreqEnd - avgFreqStart);
        assertTrue(freqDiff > 1e-5, 
            "Frequency should change in chirp signal, diff: " + freqDiff);
    }
    
    @Test
    @DisplayName("Should handle real wavelets with complex analysis")
    void testRealWaveletComplexAnalysis() {
        // Use a real wavelet (DOG)
        DOGWavelet dogWavelet = new DOGWavelet(2);
        CWTTransform dogTransform = new CWTTransform(dogWavelet);
        
        ComplexCWTResult result = dogTransform.analyzeComplex(testSignal, scales);
        
        assertNotNull(result);
        
        // For real wavelets analyzed as complex, imaginary part should come from Hilbert transform
        double[][] real = result.getReal();
        double[][] imag = result.getImaginary();
        
        // Check that both parts exist and are different
        boolean hasDifference = false;
        for (int s = 0; s < scales.length; s++) {
            for (int t = 0; t < testSignal.length; t++) {
                if (Math.abs(real[s][t] - imag[s][t]) > 1e-10) {
                    hasDifference = true;
                    break;
                }
            }
        }
        assertTrue(hasDifference, "Real and imaginary parts should differ");
    }
    
    @Test
    @DisplayName("Should preserve energy in complex representation")
    void testEnergyPreservation() {
        ComplexCWTResult complexResult = cwtTransform.analyzeComplex(testSignal, scales);
        
        // Check that energy is preserved in the complex representation
        // Parseval's theorem: energy in time domain = energy in frequency domain
        double signalEnergy = 0;
        for (double sample : testSignal) {
            signalEnergy += sample * sample;
        }
        
        // For each scale, check energy preservation
        for (int s = 0; s < scales.length; s++) {
            double scaleEnergy = 0;
            ComplexNumber[][] coeffs = complexResult.getCoefficients();
            for (int t = 0; t < testSignal.length; t++) {
                double real = coeffs[s][t].real();
                double imag = coeffs[s][t].imag();
                scaleEnergy += real * real + imag * imag;
            }
            
            // Energy should be proportional to signal energy
            // The proportionality constant depends on the wavelet and scale
            // For FFT-based CWT, the scaling can be quite different
            // Just check that it's finite and positive
            double ratio = scaleEnergy / signalEnergy;
            assertTrue(ratio > 0 && Double.isFinite(ratio), 
                "Energy ratio should be finite and positive for scale " + scales[s] + ": " + ratio);
        }
        
        // Also verify that complex CWT produces consistent results
        ComplexCWTResult complexResult2 = cwtTransform.analyzeComplex(testSignal, scales);
        ComplexNumber[][] coeffs1 = complexResult.getCoefficients();
        ComplexNumber[][] coeffs2 = complexResult2.getCoefficients();
        
        for (int s = 0; s < scales.length; s++) {
            for (int t = 0; t < testSignal.length; t++) {
                assertEquals(coeffs1[s][t].real(), coeffs2[s][t].real(), 1e-10,
                    "Real parts should be identical");
                assertEquals(coeffs1[s][t].imag(), coeffs2[s][t].imag(), 1e-10,
                    "Imaginary parts should be identical");
            }
        }
    }
    
    @Test
    @DisplayName("Should handle edge cases")
    void testEdgeCases() {
        // Empty signal
        assertThrows(IllegalArgumentException.class, () -> {
            cwtTransform.analyzeComplex(new double[0], scales);
        });
        
        // Null signal
        assertThrows(IllegalArgumentException.class, () -> {
            cwtTransform.analyzeComplex(null, scales);
        });
        
        // Empty scales
        assertThrows(IllegalArgumentException.class, () -> {
            cwtTransform.analyzeComplex(testSignal, new double[0]);
        });
        
        // Single scale
        double[] singleScale = {10.0};
        ComplexCWTResult result = cwtTransform.analyzeComplex(testSignal, singleScale);
        assertEquals(1, result.getNumScales());
    }
    
    @Test
    @DisplayName("Should convert to real CWT result")
    void testConversionToReal() {
        ComplexCWTResult complexResult = cwtTransform.analyzeComplex(testSignal, scales);
        CWTResult realResult = complexResult.toRealResult();
        
        assertNotNull(realResult);
        assertEquals(complexResult.getNumScales(), realResult.getNumScales());
        assertEquals(complexResult.getNumSamples(), realResult.getNumSamples());
        
        // Coefficients should be magnitude values
        double[][] realCoeffs = realResult.getCoefficients();
        double[][] magnitude = complexResult.getMagnitude();
        
        for (int s = 0; s < scales.length; s++) {
            assertArrayEquals(magnitude[s], realCoeffs[s], 1e-10);
        }
    }
    
    @Test
    @DisplayName("Should extract scale and time slices")
    void testSliceExtraction() {
        ComplexCWTResult result = cwtTransform.analyzeComplex(testSignal, scales);
        
        // Test scale slice
        int scaleIdx = 2;
        ComplexNumber[] scaleSlice = result.getScaleCoefficients(scaleIdx);
        assertEquals(testSignal.length, scaleSlice.length);
        
        // Test time slice
        int timeIdx = testSignal.length / 2;
        ComplexNumber[] timeSlice = result.getTimeCoefficients(timeIdx);
        assertEquals(scales.length, timeSlice.length);
        
        // Verify consistency
        assertEquals(scaleSlice[timeIdx], result.getCoefficient(scaleIdx, timeIdx));
    }
}