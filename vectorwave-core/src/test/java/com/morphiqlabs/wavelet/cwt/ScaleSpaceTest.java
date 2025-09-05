package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class ScaleSpaceTest {
    
    private static final double TOLERANCE = 1e-10;
    
    @Test
    @DisplayName("Should create linear scale space")
    void testLinearScaleSpace() {
        // Given
        double minScale = 1.0;
        double maxScale = 10.0;
        int numScales = 10;
        
        // When
        ScaleSpace scaleSpace = ScaleSpace.linear(minScale, maxScale, numScales);
        
        // Then
        assertNotNull(scaleSpace);
        assertEquals(numScales, scaleSpace.getNumScales());
        assertEquals(minScale, scaleSpace.getMinScale(), TOLERANCE);
        assertEquals(maxScale, scaleSpace.getMaxScale(), TOLERANCE);
        
        double[] scales = scaleSpace.getScales();
        assertEquals(numScales, scales.length);
        
        // Verify linear spacing
        double step = (maxScale - minScale) / (numScales - 1);
        for (int i = 0; i < numScales; i++) {
            double expected = minScale + i * step;
            assertEquals(expected, scales[i], TOLERANCE);
        }
    }
    
    @Test
    @DisplayName("Should create logarithmic scale space")
    void testLogarithmicScaleSpace() {
        // Given
        double minScale = 1.0;
        double maxScale = 100.0;
        int numScales = 5;
        
        // When
        ScaleSpace scaleSpace = ScaleSpace.logarithmic(minScale, maxScale, numScales);
        
        // Then
        assertNotNull(scaleSpace);
        assertEquals(numScales, scaleSpace.getNumScales());
        
        double[] scales = scaleSpace.getScales();
        
        // Verify logarithmic spacing
        double logMin = Math.log(minScale);
        double logMax = Math.log(maxScale);
        double logStep = (logMax - logMin) / (numScales - 1);
        
        for (int i = 0; i < numScales; i++) {
            double expected = Math.exp(logMin + i * logStep);
            assertEquals(expected, scales[i], expected * 0.0001); // Relative tolerance
        }
    }
    
    @Test
    @DisplayName("Should create dyadic scale space")
    void testDyadicScaleSpace() {
        // Given
        int minLevel = 0;
        int maxLevel = 4;
        
        // When
        ScaleSpace scaleSpace = ScaleSpace.dyadic(minLevel, maxLevel);
        
        // Then
        assertNotNull(scaleSpace);
        assertEquals(maxLevel - minLevel + 1, scaleSpace.getNumScales());
        
        double[] scales = scaleSpace.getScales();
        
        // Verify powers of 2
        for (int i = 0; i <= maxLevel - minLevel; i++) {
            double expected = Math.pow(2, minLevel + i);
            assertEquals(expected, scales[i], TOLERANCE);
        }
    }
    
    @Test
    @DisplayName("Should create scale space for frequency range")
    void testScaleSpaceForFrequencyRange() {
        // Given
        double minFreq = 20.0;  // Hz
        double maxFreq = 2000.0; // Hz
        double samplingRate = 44100.0; // Hz
        MorletWavelet wavelet = new MorletWavelet(6.0, 1.0);
        int numScales = 50;
        
        // When
        ScaleSpace scaleSpace = ScaleSpace.forFrequencyRange(
            minFreq, maxFreq, samplingRate, wavelet, numScales);
        
        // Then
        assertNotNull(scaleSpace);
        assertEquals(numScales, scaleSpace.getNumScales());
        
        // Verify frequency mapping
        double[] frequencies = scaleSpace.toFrequencies(wavelet, samplingRate);
        
        // First scale should map to approximately maxFreq
        assertTrue(Math.abs(frequencies[0] - maxFreq) < maxFreq * 0.1);
        
        // Last scale should map to approximately minFreq
        assertTrue(Math.abs(frequencies[numScales - 1] - minFreq) < minFreq * 0.1);
    }
    
    @Test
    @DisplayName("Should convert scales to frequencies")
    void testScaleToFrequencyConversion() {
        // Given
        MorletWavelet wavelet = new MorletWavelet(6.0, 1.0);
        double samplingRate = 1000.0;
        ScaleSpace scaleSpace = ScaleSpace.linear(1.0, 10.0, 5);
        
        // When
        double[] frequencies = scaleSpace.toFrequencies(wavelet, samplingRate);
        
        // Then
        assertEquals(5, frequencies.length);
        
        // Verify inverse relationship: frequency = (centerFreq * samplingRate) / scale
        double centerFreq = wavelet.centerFrequency();
        double[] scales = scaleSpace.getScales();
        
        for (int i = 0; i < scales.length; i++) {
            double expectedFreq = (centerFreq * samplingRate) / scales[i];
            assertEquals(expectedFreq, frequencies[i], expectedFreq * 0.0001);
        }
    }
    
    @Test
    @DisplayName("Should validate scale space parameters")
    void testScaleSpaceValidation() {
        // Test invalid parameters
        assertThrows(IllegalArgumentException.class, 
            () -> ScaleSpace.linear(10.0, 1.0, 10)); // min > max
        
        assertThrows(IllegalArgumentException.class, 
            () -> ScaleSpace.linear(1.0, 10.0, 0)); // numScales = 0
        
        assertThrows(IllegalArgumentException.class, 
            () -> ScaleSpace.linear(0.0, 10.0, 10)); // minScale = 0
        
        assertThrows(IllegalArgumentException.class, 
            () -> ScaleSpace.logarithmic(-1.0, 10.0, 10)); // negative scale
        
        assertThrows(IllegalArgumentException.class, 
            () -> ScaleSpace.dyadic(5, 2)); // minLevel > maxLevel
    }
    
    @Test
    @DisplayName("Should support custom scale arrays")
    void testCustomScaleSpace() {
        // Given
        double[] customScales = {1.0, 1.5, 2.0, 3.0, 5.0, 8.0, 13.0};
        
        // When
        ScaleSpace scaleSpace = ScaleSpace.custom(customScales);
        
        // Then
        assertNotNull(scaleSpace);
        assertEquals(customScales.length, scaleSpace.getNumScales());
        assertArrayEquals(customScales, scaleSpace.getScales(), TOLERANCE);
        assertEquals(1.0, scaleSpace.getMinScale(), TOLERANCE);
        assertEquals(13.0, scaleSpace.getMaxScale(), TOLERANCE);
    }
    
    @Test
    @DisplayName("Should sort unsorted custom scales")
    void testCustomScaleSpaceWithUnsortedInput() {
        // Given - unsorted scales
        double[] unsortedScales = {5.0, 1.0, 13.0, 3.0, 8.0, 2.0, 1.5};
        double[] expectedSorted = {1.0, 1.5, 2.0, 3.0, 5.0, 8.0, 13.0};
        
        // When
        ScaleSpace scaleSpace = ScaleSpace.custom(unsortedScales);
        
        // Then
        assertNotNull(scaleSpace);
        assertEquals(unsortedScales.length, scaleSpace.getNumScales());
        assertArrayEquals(expectedSorted, scaleSpace.getScales(), TOLERANCE);
        assertEquals(1.0, scaleSpace.getMinScale(), TOLERANCE);
        assertEquals(13.0, scaleSpace.getMaxScale(), TOLERANCE);
    }
    
    @Test
    @DisplayName("Should efficiently handle pre-sorted scales")
    void testCustomSortedScaleSpace() {
        // Given - already sorted scales
        double[] sortedScales = {1.0, 1.5, 2.0, 3.0, 5.0, 8.0, 13.0};
        
        // When
        ScaleSpace scaleSpace = ScaleSpace.customSorted(sortedScales);
        
        // Then
        assertNotNull(scaleSpace);
        assertEquals(sortedScales.length, scaleSpace.getNumScales());
        assertArrayEquals(sortedScales, scaleSpace.getScales(), TOLERANCE);
        assertEquals(1.0, scaleSpace.getMinScale(), TOLERANCE);
        assertEquals(13.0, scaleSpace.getMaxScale(), TOLERANCE);
    }
    
    @Test
    @DisplayName("Should validate sorted order in customSorted")
    void testCustomSortedValidation() {
        // Test unsorted input
        double[] unsortedScales = {1.0, 3.0, 2.0};
        assertThrows(IllegalArgumentException.class,
            () -> ScaleSpace.customSorted(unsortedScales),
            "Should reject unsorted scales");
        
        // Test with duplicate values
        double[] duplicateScales = {1.0, 2.0, 2.0, 3.0};
        assertThrows(IllegalArgumentException.class,
            () -> ScaleSpace.customSorted(duplicateScales),
            "Should reject non-strictly ascending scales");
        
        // Test with negative values
        double[] negativeScales = {-1.0, 0.0, 1.0};
        assertThrows(IllegalArgumentException.class,
            () -> ScaleSpace.customSorted(negativeScales),
            "Should reject negative scales");
        
        // Test with zero
        double[] zeroScales = {0.0, 1.0, 2.0};
        assertThrows(IllegalArgumentException.class,
            () -> ScaleSpace.customSorted(zeroScales),
            "Should reject zero scales");
    }
    
    @Test
    @DisplayName("Should find scale index for given frequency")
    void testFindScaleForFrequency() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        double samplingRate = 1000.0;
        ScaleSpace scaleSpace = ScaleSpace.logarithmic(1.0, 100.0, 20);
        double targetFreq = 50.0; // Hz
        
        // When
        int scaleIndex = scaleSpace.findScaleIndexForFrequency(targetFreq, wavelet, samplingRate);
        
        // Then
        assertTrue(scaleIndex >= 0 && scaleIndex < 20);
        
        // Verify this is the closest scale
        double[] frequencies = scaleSpace.toFrequencies(wavelet, samplingRate);
        double selectedFreq = frequencies[scaleIndex];
        
        // Check neighbors to ensure we found the closest
        if (scaleIndex > 0) {
            double prevDiff = Math.abs(frequencies[scaleIndex - 1] - targetFreq);
            double currDiff = Math.abs(selectedFreq - targetFreq);
            assertTrue(currDiff <= prevDiff);
        }
        
        if (scaleIndex < frequencies.length - 1) {
            double nextDiff = Math.abs(frequencies[scaleIndex + 1] - targetFreq);
            double currDiff = Math.abs(selectedFreq - targetFreq);
            assertTrue(currDiff <= nextDiff);
        }
    }
}