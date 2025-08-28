package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.cwt.finance.DOGWavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests to improve InverseCWT coverage to 70%.
 */
class InverseCWTAdditionalTest {
    
    private ContinuousWavelet morletWavelet;
    private CWTTransform cwtTransform;
    private InverseCWT inverseCWT;
    
    @BeforeEach
    void setUp() {
        morletWavelet = new MorletWavelet();
        cwtTransform = new CWTTransform(morletWavelet);
        inverseCWT = new InverseCWT(morletWavelet);
    }
    
    @Test
    @DisplayName("Should test reconstructBand method")
    void testReconstructBand() {
        // Create test signal
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 10 * i / signal.length) +
                       0.5 * Math.sin(2 * Math.PI * 20 * i / signal.length);
        }
        
        // Analyze with CWT
        double[] scales = new double[20];
        for (int i = 0; i < scales.length; i++) {
            scales[i] = 1.0 + i * 2.0;
        }
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        
        // Test band reconstruction
        double[] reconstructed = inverseCWT.reconstructBand(cwtResult, 5.0, 15.0);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Test with different bands
        reconstructed = inverseCWT.reconstructBand(cwtResult, 1.0, 10.0);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Test with very narrow band
        reconstructed = inverseCWT.reconstructBand(cwtResult, 8.0, 9.0);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
    }
    
    @Test
    @DisplayName("Should handle edge cases in reconstructBand")
    void testReconstructBandEdgeCases() {
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.cos(2 * Math.PI * 5 * i / signal.length);
        }
        
        double[] scales = {1.0, 2.0, 4.0, 8.0, 16.0};
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        
        // Test with scale range outside available scales
        double[] reconstructed = inverseCWT.reconstructBand(cwtResult, 20.0, 30.0);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        // Note: Current implementation uses all scales when range is completely outside
        // This may not be the intended behavior, but we're testing the actual behavior
        assertTrue(reconstructed.length > 0);
        
        // Test with scale range partially overlapping
        reconstructed = inverseCWT.reconstructBand(cwtResult, 3.0, 10.0);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Test with exact scale boundaries
        reconstructed = inverseCWT.reconstructBand(cwtResult, 2.0, 8.0);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
    }
    
    @Test
    @DisplayName("Should throw exceptions for invalid reconstructBand parameters")
    void testReconstructBandExceptions() {
        double[] signal = new double[64];
        double[] scales = {1.0, 2.0, 4.0};
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        
        // Null CWT result
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructBand(null, 1.0, 2.0));
        
        // Invalid scale range - negative minScale
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructBand(cwtResult, -1.0, 2.0));
        
        // Invalid scale range - minScale >= maxScale
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructBand(cwtResult, 2.0, 2.0));
        
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructBand(cwtResult, 3.0, 2.0));
    }
    
    @Test
    @DisplayName("Should test reconstructFrequencyBand method")
    void testReconstructFrequencyBand() {
        // Create signal with known frequency components
        int samplingRate = 1000; // Hz
        double[] signal = new double[512];
        for (int i = 0; i < signal.length; i++) {
            double t = i / (double)samplingRate;
            signal[i] = Math.sin(2 * Math.PI * 50 * t) +  // 50 Hz
                       0.5 * Math.sin(2 * Math.PI * 100 * t); // 100 Hz
        }
        
        // Analyze with CWT
        double[] scales = new double[30];
        for (int i = 0; i < scales.length; i++) {
            scales[i] = 1.0 + i * 0.5;
        }
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        
        // Reconstruct frequency band around 50 Hz
        double[] reconstructed = inverseCWT.reconstructFrequencyBand(
            cwtResult, samplingRate, 40.0, 60.0);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Reconstruct wider band
        reconstructed = inverseCWT.reconstructFrequencyBand(
            cwtResult, samplingRate, 20.0, 150.0);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Reconstruct high frequency band
        reconstructed = inverseCWT.reconstructFrequencyBand(
            cwtResult, samplingRate, 80.0, 120.0);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
    }
    
    @Test
    @DisplayName("Should handle edge cases in reconstructFrequencyBand")
    void testReconstructFrequencyBandEdgeCases() {
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.random() - 0.5;
        }
        
        double[] scales = new double[10];
        for (int i = 0; i < scales.length; i++) {
            scales[i] = 0.5 + i * 0.5;
        }
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        
        double samplingRate = 100.0;
        
        // Test at Nyquist frequency boundary
        double[] reconstructed = inverseCWT.reconstructFrequencyBand(
            cwtResult, samplingRate, 10.0, 50.0);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Test very narrow frequency band
        reconstructed = inverseCWT.reconstructFrequencyBand(
            cwtResult, samplingRate, 20.0, 21.0);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
    }
    
    @Test
    @DisplayName("Should throw exceptions for invalid reconstructFrequencyBand parameters")
    void testReconstructFrequencyBandExceptions() {
        double[] signal = new double[128];
        double[] scales = {1.0, 2.0, 4.0};
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        
        double samplingRate = 100.0;
        
        // Invalid sampling rate
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructFrequencyBand(cwtResult, 0.0, 10.0, 20.0));
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructFrequencyBand(cwtResult, -100.0, 10.0, 20.0));
        
        // Invalid frequency range - negative minFreq
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructFrequencyBand(cwtResult, samplingRate, -10.0, 20.0));
        
        // Invalid frequency range - minFreq >= maxFreq
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructFrequencyBand(cwtResult, samplingRate, 20.0, 20.0));
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructFrequencyBand(cwtResult, samplingRate, 30.0, 20.0));
        
        // Invalid frequency range - maxFreq > Nyquist
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructFrequencyBand(cwtResult, samplingRate, 10.0, 60.0));
    }
    
    @Test
    @DisplayName("Should test getAdmissibilityConstant method")
    void testGetAdmissibilityConstant() {
        double admissibility = inverseCWT.getAdmissibilityConstant();
        assertTrue(admissibility > 0, "Admissibility constant should be positive");
        assertFalse(Double.isInfinite(admissibility), "Admissibility constant should be finite");
        assertFalse(Double.isNaN(admissibility), "Admissibility constant should not be NaN");
        
        // Different wavelet should have different admissibility constant
        InverseCWT dogInverse = new InverseCWT(new DOGWavelet(2));
        double dogAdmissibility = dogInverse.getAdmissibilityConstant();
        assertNotEquals(admissibility, dogAdmissibility, 
            "Different wavelets should have different admissibility constants");
    }
    
    @Test
    @DisplayName("Should test isAdmissible method")
    void testIsAdmissible() {
        // Morlet wavelet should be admissible
        assertTrue(inverseCWT.isAdmissible(), "Morlet wavelet should be admissible");
        
        // DOG wavelet should also be admissible
        InverseCWT dogInverse = new InverseCWT(new DOGWavelet(3));
        assertTrue(dogInverse.isAdmissible(), "DOG wavelet should be admissible");
    }
    
    @Test
    @DisplayName("Should handle invalid CWT result parameters")
    void testInvalidCWTResultParameters() {
        // Test with signal having invalid length (0)
        double[] invalidSignal = new double[0];
        double[] scales = {1.0};
        
        // This should fail during CWT analysis
        assertThrows(Exception.class, 
            () -> cwtTransform.analyze(invalidSignal, scales));
        
        // Test with very small signal
        double[] tinySignal = new double[1];
        tinySignal[0] = 1.0;
        CWTResult tinyResult = cwtTransform.analyze(tinySignal, scales);
        
        // Should still be able to reconstruct even tiny signals
        double[] reconstructed = inverseCWT.reconstruct(tinyResult);
        assertNotNull(reconstructed);
        assertEquals(tinySignal.length, reconstructed.length);
    }
    
    @Test
    @DisplayName("Should test direct reconstruction for small signals")
    void testDirectReconstructionSmallSignals() {
        // Create small signal (< 128 samples to force direct method)
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 3 * i / signal.length);
        }
        
        // Small number of scales
        double[] scales = {1.0, 2.0, 3.0};
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        
        // Reconstruct using direct method
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Test band reconstruction with small signal
        reconstructed = inverseCWT.reconstructBand(cwtResult, 1.5, 2.5);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
    }
}