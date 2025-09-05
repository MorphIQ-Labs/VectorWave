package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

class CWTResultTest {
    
    private static final double TOLERANCE = 1e-10;
    private double[][] testCoefficients;
    private double[] testScales;
    private MorletWavelet testWavelet;
    
    @BeforeEach
    void setUp() {
        // Create test data: 3 scales x 5 time points
        testCoefficients = new double[][] {
            {1.0, 2.0, 3.0, 4.0, 5.0},    // Scale 1
            {2.0, 3.0, 4.0, 5.0, 6.0},    // Scale 2
            {3.0, 4.0, 5.0, 6.0, 7.0}     // Scale 3
        };
        testScales = new double[]{1.0, 2.0, 4.0};
        testWavelet = new MorletWavelet();
    }
    
    @Test
    @DisplayName("Should create CWT result with real coefficients")
    void testCreateRealCWTResult() {
        // When
        CWTResult result = new CWTResult(testCoefficients, testScales, testWavelet);
        
        // Then
        assertNotNull(result);
        assertEquals(3, result.getNumScales());
        assertEquals(5, result.getNumSamples());
        assertArrayEquals(testScales, result.getScales(), TOLERANCE);
        assertEquals(testWavelet, result.getWavelet());
        assertFalse(result.isComplex());
    }
    
    @Test
    @DisplayName("Should create CWT result with complex coefficients")
    void testCreateComplexCWTResult() {
        // Given
        ComplexMatrix complexCoeffs = new ComplexMatrix(3, 5);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                complexCoeffs.set(i, j, testCoefficients[i][j], i * 0.5); // Add imaginary part
            }
        }
        
        // When
        CWTResult result = new CWTResult(complexCoeffs, testScales, testWavelet);
        
        // Then
        assertNotNull(result);
        assertTrue(result.isComplex());
        assertEquals(3, result.getNumScales());
        assertEquals(5, result.getNumSamples());
    }
    
    @Test
    @DisplayName("Should compute magnitude from real coefficients")
    void testMagnitudeFromReal() {
        // Given
        CWTResult result = new CWTResult(testCoefficients, testScales, testWavelet);
        
        // When
        double[][] magnitude = result.getMagnitude();
        
        // Then
        assertNotNull(magnitude);
        assertEquals(3, magnitude.length);
        assertEquals(5, magnitude[0].length);
        
        // For real coefficients, magnitude = |coefficient|
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                assertEquals(Math.abs(testCoefficients[i][j]), magnitude[i][j], TOLERANCE);
            }
        }
    }
    
    @Test
    @DisplayName("Should compute magnitude from complex coefficients")
    void testMagnitudeFromComplex() {
        // Given
        ComplexMatrix complexCoeffs = new ComplexMatrix(2, 3);
        complexCoeffs.set(0, 0, 3.0, 4.0);  // |3+4i| = 5
        complexCoeffs.set(0, 1, 5.0, 12.0); // |5+12i| = 13
        complexCoeffs.set(1, 0, 8.0, 15.0); // |8+15i| = 17
        
        CWTResult result = new CWTResult(complexCoeffs, new double[]{1.0, 2.0}, testWavelet);
        
        // When
        double[][] magnitude = result.getMagnitude();
        
        // Then
        assertEquals(5.0, magnitude[0][0], TOLERANCE);
        assertEquals(13.0, magnitude[0][1], TOLERANCE);
        assertEquals(17.0, magnitude[1][0], TOLERANCE);
    }
    
    @Test
    @DisplayName("Should compute phase from complex coefficients")
    void testPhaseFromComplex() {
        // Given
        ComplexMatrix complexCoeffs = new ComplexMatrix(1, 4);
        complexCoeffs.set(0, 0, 1.0, 0.0);   // Phase = 0
        complexCoeffs.set(0, 1, 0.0, 1.0);   // Phase = π/2
        complexCoeffs.set(0, 2, -1.0, 0.0);  // Phase = π
        complexCoeffs.set(0, 3, 1.0, 1.0);   // Phase = π/4
        
        CWTResult result = new CWTResult(complexCoeffs, new double[]{1.0}, testWavelet);
        
        // When
        double[][] phase = result.getPhase();
        
        // Then
        assertNotNull(phase);
        assertEquals(0.0, phase[0][0], TOLERANCE);
        assertEquals(Math.PI / 2, phase[0][1], TOLERANCE);
        assertEquals(Math.PI, phase[0][2], TOLERANCE);
        assertEquals(Math.PI / 4, phase[0][3], TOLERANCE);
    }
    
    @Test
    @DisplayName("Should return null phase for real coefficients")
    void testPhaseFromReal() {
        // Given
        CWTResult result = new CWTResult(testCoefficients, testScales, testWavelet);
        
        // When
        double[][] phase = result.getPhase();
        
        // Then
        assertNull(phase, "Phase should be null for real-valued CWT");
    }
    
    @Test
    @DisplayName("Should extract scalogram at time index")
    void testGetScalogram() {
        // Given
        CWTResult result = new CWTResult(testCoefficients, testScales, testWavelet);
        
        // When
        double[] scalogram = result.getScalogram(2); // Time index 2
        
        // Then
        assertNotNull(scalogram);
        assertEquals(3, scalogram.length);
        assertEquals(3.0, scalogram[0], TOLERANCE); // Scale 1, time 2
        assertEquals(4.0, scalogram[1], TOLERANCE); // Scale 2, time 2
        assertEquals(5.0, scalogram[2], TOLERANCE); // Scale 3, time 2
    }
    
    @Test
    @DisplayName("Should extract time slice at scale index")
    void testGetTimeSlice() {
        // Given
        CWTResult result = new CWTResult(testCoefficients, testScales, testWavelet);
        
        // When
        double[] timeSlice = result.getTimeSlice(1); // Scale index 1
        
        // Then
        assertNotNull(timeSlice);
        assertEquals(5, timeSlice.length);
        assertArrayEquals(new double[]{2.0, 3.0, 4.0, 5.0, 6.0}, timeSlice, TOLERANCE);
    }
    
    @Test
    @DisplayName("Should validate bounds for scalogram")
    void testScalogramBoundsValidation() {
        // Given
        CWTResult result = new CWTResult(testCoefficients, testScales, testWavelet);
        
        // Then
        assertThrows(IndexOutOfBoundsException.class, () -> result.getScalogram(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> result.getScalogram(5));
    }
    
    @Test
    @DisplayName("Should validate bounds for time slice")
    void testTimeSliceBoundsValidation() {
        // Given
        CWTResult result = new CWTResult(testCoefficients, testScales, testWavelet);
        
        // Then
        assertThrows(IndexOutOfBoundsException.class, () -> result.getTimeSlice(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> result.getTimeSlice(3));
    }
    
    @Test
    @DisplayName("Should compute power spectrum")
    void testPowerSpectrum() {
        // Given
        CWTResult result = new CWTResult(testCoefficients, testScales, testWavelet);
        
        // When
        double[][] power = result.getPowerSpectrum();
        
        // Then
        assertNotNull(power);
        assertEquals(3, power.length);
        assertEquals(5, power[0].length);
        
        // Power = magnitude²
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 5; j++) {
                double expected = testCoefficients[i][j] * testCoefficients[i][j];
                assertEquals(expected, power[i][j], TOLERANCE);
            }
        }
    }
    
    @Test
    @DisplayName("Should find maximum coefficient")
    void testFindMaxCoefficient() {
        // Given
        CWTResult result = new CWTResult(testCoefficients, testScales, testWavelet);
        
        // When
        CWTResult.MaxCoefficient max = result.findMaxCoefficient();
        
        // Then
        assertNotNull(max);
        assertEquals(7.0, max.value(), TOLERANCE);
        assertEquals(2, max.scaleIndex());
        assertEquals(4, max.timeIndex());
        assertEquals(4.0, max.scale(), TOLERANCE);
    }
    
    @Test
    @DisplayName("Should convert scales to frequencies")
    void testScaleToFrequencyConversion() {
        // Given
        double samplingRate = 1000.0;
        CWTResult result = new CWTResult(testCoefficients, testScales, testWavelet);
        
        // When
        double[] frequencies = result.getFrequencies(samplingRate);
        
        // Then
        assertNotNull(frequencies);
        assertEquals(3, frequencies.length);
        
        // Verify frequency = (centerFreq * samplingRate) / scale
        double centerFreq = testWavelet.centerFrequency();
        for (int i = 0; i < testScales.length; i++) {
            double expected = (centerFreq * samplingRate) / testScales[i];
            assertEquals(expected, frequencies[i], expected * 0.0001);
        }
    }
    
    @Test
    @DisplayName("Should compute time-averaged spectrum")
    void testTimeAveragedSpectrum() {
        // Given
        CWTResult result = new CWTResult(testCoefficients, testScales, testWavelet);
        
        // When
        double[] avgSpectrum = result.getTimeAveragedSpectrum();
        
        // Then
        assertNotNull(avgSpectrum);
        assertEquals(3, avgSpectrum.length);
        
        // Verify averages
        assertEquals(3.0, avgSpectrum[0], TOLERANCE); // (1+2+3+4+5)/5
        assertEquals(4.0, avgSpectrum[1], TOLERANCE); // (2+3+4+5+6)/5
        assertEquals(5.0, avgSpectrum[2], TOLERANCE); // (3+4+5+6+7)/5
    }
    
    @Test
    @DisplayName("Should validate input parameters")
    void testInputValidation() {
        // Null coefficients
        assertThrows(IllegalArgumentException.class, 
            () -> new CWTResult((double[][])null, testScales, testWavelet));
        
        // Null scales
        assertThrows(IllegalArgumentException.class, 
            () -> new CWTResult(testCoefficients, null, testWavelet));
        
        // Null wavelet
        assertThrows(IllegalArgumentException.class, 
            () -> new CWTResult(testCoefficients, testScales, null));
        
        // Empty coefficients
        assertThrows(IllegalArgumentException.class, 
            () -> new CWTResult(new double[0][0], testScales, testWavelet));
        
        // Mismatched dimensions
        assertThrows(IllegalArgumentException.class, 
            () -> new CWTResult(testCoefficients, new double[]{1.0, 2.0}, testWavelet));
    }
    
    @Test
    @DisplayName("Should create immutable result")
    void testImmutability() {
        // Given
        double[][] coeffs = {{1.0, 2.0}, {3.0, 4.0}};
        double[] scales = {1.0, 2.0};
        CWTResult result = new CWTResult(coeffs, scales, testWavelet);
        
        // When - modify original arrays
        coeffs[0][0] = 99.0;
        scales[0] = 99.0;
        
        // Then - result should be unchanged
        double[][] resultCoeffs = result.getCoefficients();
        assertEquals(1.0, resultCoeffs[0][0], TOLERANCE);
        assertEquals(1.0, result.getScales()[0], TOLERANCE);
    }
}