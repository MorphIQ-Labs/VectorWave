package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.cwt.finance.PaulWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for adaptive scale selection functionality.
 */
class AdaptiveScaleSelectionTest {
    
    private MorletWavelet morletWavelet;
    private PaulWavelet paulWavelet;
    private double[] testSignal;
    private double samplingRate;
    
    @BeforeEach
    void setUp() {
        morletWavelet = new MorletWavelet(6.0, 1.0);
        paulWavelet = new PaulWavelet(4);
        samplingRate = 100.0; // 100 Hz
        
        // Create test signal - multi-component
        int N = 512;
        testSignal = new double[N];
        for (int i = 0; i < N; i++) {
            double t = i / samplingRate;
            testSignal[i] = 0.8 * Math.sin(2 * Math.PI * 5 * t) +
                           0.6 * Math.cos(2 * Math.PI * 15 * t) +
                           0.4 * Math.sin(2 * Math.PI * 30 * t);
        }
    }
    
    @Test
    @DisplayName("Should create dyadic scale selector")
    void testDyadicScaleSelectorCreation() {
        DyadicScaleSelector selector = DyadicScaleSelector.create();
        assertNotNull(selector);
    }
    
    @Test
    @DisplayName("Should generate dyadic scales correctly")
    void testDyadicScaleGeneration() {
        DyadicScaleSelector selector = new DyadicScaleSelector();
        double[] scales = selector.selectScales(testSignal, morletWavelet, samplingRate);
        
        assertNotNull(scales);
        assertTrue(scales.length > 0);
        
        // Check scales are in ascending order
        for (int i = 1; i < scales.length; i++) {
            assertTrue(scales[i] > scales[i-1], "Scales should be in ascending order");
        }
        
        // Check scales are approximately powers of 2 (with subdivisions)
        for (int i = 1; i < scales.length; i++) {
            double ratio = scales[i] / scales[i-1];
            // For dyadic with subdivisions, ratio should be consistent
            assertTrue(ratio >= 1.05 && ratio <= 2.1, 
                "Scale ratio should be reasonable: " + ratio);
        }
    }
    
    @Test
    @DisplayName("Should respect scale range limits in dyadic selection")
    void testDyadicScaleRangeLimits() {
        DyadicScaleSelector selector = new DyadicScaleSelector();
        
        AdaptiveScaleSelector.ScaleSelectionConfig config = 
            AdaptiveScaleSelector.ScaleSelectionConfig.builder(samplingRate)
                .frequencyRange(5.0, 25.0) // Limited frequency range
                .spacing(AdaptiveScaleSelector.ScaleSpacing.DYADIC)
                .build();
        
        double[] scales = selector.selectScales(testSignal, morletWavelet, config);
        
        // Convert scales to frequencies
        double centerFreq = morletWavelet.centerFrequency();
        for (double scale : scales) {
            double freq = centerFreq * samplingRate / scale;
            assertTrue(freq >= 4.0 && freq <= 26.0, // Allow some margin
                "Frequency should be within specified range: " + freq);
        }
    }
    
    @Test
    @DisplayName("Should create signal-adaptive scale selector")
    void testSignalAdaptiveScaleSelectorCreation() {
        SignalAdaptiveScaleSelector selector = new SignalAdaptiveScaleSelector();
        assertNotNull(selector);
    }
    
    @Test
    @DisplayName("Should adapt scales to signal characteristics")
    void testSignalAdaptiveScaleGeneration() {
        SignalAdaptiveScaleSelector selector = new SignalAdaptiveScaleSelector();
        double[] scales = selector.selectScales(testSignal, morletWavelet, samplingRate);
        
        assertNotNull(scales);
        assertTrue(scales.length > 0);
        assertTrue(scales.length <= 200); // Default max scales
        
        // Check scales are in ascending order
        for (int i = 1; i < scales.length; i++) {
            assertTrue(scales[i] > scales[i-1], "Scales should be in ascending order");
        }
        
        // For our test signal with components at 5, 15, 30 Hz,
        // we should have more scales around these frequencies
        double centerFreq = morletWavelet.centerFrequency();
        int scalesNear5Hz = 0, scalesNear15Hz = 0, scalesNear30Hz = 0;
        
        for (double scale : scales) {
            double freq = centerFreq * samplingRate / scale;
            if (Math.abs(freq - 5) < 2) scalesNear5Hz++;
            if (Math.abs(freq - 15) < 3) scalesNear15Hz++;
            if (Math.abs(freq - 30) < 5) scalesNear30Hz++;
        }
        
        // Should have scales near dominant frequencies
        assertTrue(scalesNear5Hz > 0, "Should have scales near 5 Hz");
        assertTrue(scalesNear15Hz > 0, "Should have scales near 15 Hz");
        assertTrue(scalesNear30Hz > 0, "Should have scales near 30 Hz");
    }
    
    @Test
    @DisplayName("Should create optimal scale selector")
    void testOptimalScaleSelectorCreation() {
        OptimalScaleSelector selector = OptimalScaleSelector.logarithmic(10);
        assertNotNull(selector);
    }
    
    @Test
    @DisplayName("Should generate logarithmic scales correctly")
    void testLogarithmicScaleGeneration() {
        OptimalScaleSelector selector = new OptimalScaleSelector();
        
        AdaptiveScaleSelector.ScaleSelectionConfig config = 
            AdaptiveScaleSelector.ScaleSelectionConfig.builder(samplingRate)
                .spacing(AdaptiveScaleSelector.ScaleSpacing.LOGARITHMIC)
                .scalesPerOctave(12)
                .maxScales(50)
                .build();
        
        double[] scales = selector.selectScales(testSignal, morletWavelet, config);
        
        assertNotNull(scales);
        assertTrue(scales.length > 0);
        assertTrue(scales.length <= 50);
        
        // Check logarithmic spacing
        if (scales.length > 2) {
            double expectedRatio = Math.pow(scales[scales.length-1] / scales[0], 1.0 / (scales.length - 1));
            
            for (int i = 1; i < scales.length - 1; i++) {
                double actualRatio = scales[i+1] / scales[i];
                // Allow some tolerance for logarithmic spacing
                assertTrue(Math.abs(actualRatio / expectedRatio - 1.0) < 0.2,
                    "Scale ratio should be approximately consistent");
            }
        }
    }
    
    @Test
    @DisplayName("Should generate linear scales correctly")
    void testLinearScaleGeneration() {
        OptimalScaleSelector selector = new OptimalScaleSelector();
        
        AdaptiveScaleSelector.ScaleSelectionConfig config = 
            AdaptiveScaleSelector.ScaleSelectionConfig.builder(samplingRate)
                .spacing(AdaptiveScaleSelector.ScaleSpacing.LINEAR)
                .frequencyRange(10.0, 40.0)
                .maxScales(20)
                .build();
        
        double[] scales = selector.selectScales(testSignal, morletWavelet, config);
        
        assertNotNull(scales);
        assertTrue(scales.length > 0);
        
        // Check linear spacing
        if (scales.length > 2) {
            double expectedDiff = (scales[scales.length-1] - scales[0]) / (scales.length - 1);
            
            for (int i = 1; i < scales.length; i++) {
                double actualDiff = scales[i] - scales[i-1];
                // Allow some tolerance for linear spacing
                assertTrue(Math.abs(actualDiff / expectedDiff - 1.0) < 0.1,
                    "Scale difference should be approximately consistent");
            }
        }
    }
    
    @Test
    @DisplayName("Should generate Mel-scale scales correctly")
    void testMelScaleGeneration() {
        OptimalScaleSelector selector = new OptimalScaleSelector();
        
        AdaptiveScaleSelector.ScaleSelectionConfig config = 
            AdaptiveScaleSelector.ScaleSelectionConfig.builder(samplingRate)
                .spacing(AdaptiveScaleSelector.ScaleSpacing.MEL_SCALE)
                .frequencyRange(5.0, 45.0)
                .maxScales(30)
                .build();
        
        double[] scales = selector.selectScales(testSignal, morletWavelet, config);
        
        assertNotNull(scales);
        assertTrue(scales.length > 0);
        assertTrue(scales.length <= 30);
        
        // Mel-scale should have reasonable frequency distribution
        double centerFreq = morletWavelet.centerFrequency();
        int lowFreqScales = 0, midFreqScales = 0, highFreqScales = 0;
        
        for (double scale : scales) {
            double freq = centerFreq * samplingRate / scale;
            if (freq < 15) lowFreqScales++;
            else if (freq < 30) midFreqScales++;
            else highFreqScales++;
        }
        
        // Should have a reasonable distribution across frequency bands
        assertTrue(lowFreqScales > 0 && midFreqScales > 0, 
            "Mel-scale should distribute scales across frequency bands");
    }
    
    @Test
    @DisplayName("Should handle edge cases gracefully")
    void testEdgeCases() {
        DyadicScaleSelector dyadicSelector = new DyadicScaleSelector();
        
        // Empty signal
        assertThrows(IllegalArgumentException.class, () -> {
            dyadicSelector.selectScales(new double[0], morletWavelet, samplingRate);
        });
        
        // Null signal
        assertThrows(IllegalArgumentException.class, () -> {
            dyadicSelector.selectScales(null, morletWavelet, samplingRate);
        });
        
        // Null wavelet
        assertThrows(IllegalArgumentException.class, () -> {
            dyadicSelector.selectScales(testSignal, null, samplingRate);
        });
        
        // Invalid sampling rate
        assertThrows(IllegalArgumentException.class, () -> {
            dyadicSelector.selectScales(testSignal, morletWavelet, 0);
        });
    }
    
    @Test
    @DisplayName("Should respect maximum scale count")
    void testMaxScaleLimit() {
        SignalAdaptiveScaleSelector selector = new SignalAdaptiveScaleSelector();
        
        AdaptiveScaleSelector.ScaleSelectionConfig config = 
            AdaptiveScaleSelector.ScaleSelectionConfig.builder(samplingRate)
                .maxScales(10) // Very limited
                .build();
        
        double[] scales = selector.selectScales(testSignal, morletWavelet, config);
        
        assertNotNull(scales);
        assertTrue(scales.length <= 10, "Should respect max scale limit: " + scales.length);
        assertTrue(scales.length > 0, "Should generate at least one scale");
    }
    
    @Test
    @DisplayName("Should work with different wavelets")
    void testDifferentWavelets() {
        DyadicScaleSelector selector = new DyadicScaleSelector();
        
        // Test with Morlet wavelet
        double[] morletScales = selector.selectScales(testSignal, morletWavelet, samplingRate);
        
        // Test with Paul wavelet
        double[] paulScales = selector.selectScales(testSignal, paulWavelet, samplingRate);
        
        assertNotNull(morletScales);
        assertNotNull(paulScales);
        assertTrue(morletScales.length > 0);
        assertTrue(paulScales.length > 0);
        
        // Scales might be different due to different wavelet properties
        // but both should be valid
        for (double scale : morletScales) {
            assertTrue(scale > 0, "All scales should be positive");
        }
        
        for (double scale : paulScales) {
            assertTrue(scale > 0, "All scales should be positive");
        }
    }
    
    @Test
    @DisplayName("Should generate scales for frequency resolution")
    void testFrequencyResolutionScales() {
        double minFreq = 5.0;
        double maxFreq = 45.0;
        double frequencyResolution = 2.0; // 2 Hz resolution
        
        double[] scales = OptimalScaleSelector.generateScalesForFrequencyResolution(
            minFreq, maxFreq, morletWavelet, samplingRate, frequencyResolution);
        
        assertNotNull(scales);
        assertTrue(scales.length > 0);
        
        // Check that frequency resolution is approximately maintained
        double centerFreq = morletWavelet.centerFrequency();
        for (int i = 1; i < scales.length; i++) {
            double freq1 = centerFreq * samplingRate / scales[i-1];
            double freq2 = centerFreq * samplingRate / scales[i];
            double freqDiff = Math.abs(freq1 - freq2);
            
            // Should be close to desired resolution (allowing some variation)
            assertTrue(freqDiff >= 1.5 && freqDiff <= 2.5,
                "Frequency difference should be close to desired resolution: " + freqDiff);
        }
    }
    
    @Test
    @DisplayName("Should generate critical sampling scales")
    void testCriticalSamplingScales() {
        int signalLength = 256;
        
        double[] scales = OptimalScaleSelector.generateCriticalSamplingScales(
            morletWavelet, signalLength, samplingRate);
        
        assertNotNull(scales);
        assertTrue(scales.length > 0);
        
        // Critical sampling should have exponentially increasing scales
        for (int i = 1; i < scales.length; i++) {
            double ratio = scales[i] / scales[i-1];
            assertTrue(ratio > 1.0, "Scales should be increasing");
            assertTrue(ratio < 5.0, "Scale ratio should be reasonable: " + ratio);
        }
    }
    
    @Test
    @DisplayName("Should handle configuration builder correctly")
    void testConfigurationBuilder() {
        AdaptiveScaleSelector.ScaleSelectionConfig config = 
            AdaptiveScaleSelector.ScaleSelectionConfig.builder(samplingRate)
                .frequencyRange(10.0, 30.0)
                .scalesPerOctave(8)
                .useSignalAdaptation(false)
                .frequencyResolution(1.5)
                .maxScales(25)
                .spacing(AdaptiveScaleSelector.ScaleSpacing.LOGARITHMIC)
                .build();
        
        assertEquals(samplingRate, config.getSamplingRate());
        assertEquals(10.0, config.getMinFrequency());
        assertEquals(30.0, config.getMaxFrequency());
        assertEquals(8, config.getScalesPerOctave());
        assertFalse(config.isUseSignalAdaptation());
        assertEquals(1.5, config.getFrequencyResolution());
        assertEquals(25, config.getMaxScales());
        assertEquals(AdaptiveScaleSelector.ScaleSpacing.LOGARITHMIC, config.getSpacing());
    }
    
    @Test
    @DisplayName("Should estimate scale count correctly")
    void testScaleCountEstimation() {
        AdaptiveScaleSelector selector = new DyadicScaleSelector();
        
        double minFreq = 5.0;
        double maxFreq = 40.0;
        int scalesPerOctave = 10;
        
        int estimatedCount = selector.estimateScaleCount(
            minFreq, maxFreq, morletWavelet, samplingRate, scalesPerOctave);
        
        assertTrue(estimatedCount > 0, "Should estimate positive scale count");
        
        // Manual calculation: log2(40/5) * 10 = log2(8) * 10 = 3 * 10 = 30
        assertTrue(estimatedCount >= 25 && estimatedCount <= 35,
            "Estimate should be reasonable: " + estimatedCount);
    }
    
    @Test
    @DisplayName("Should compute frequency range correctly")
    void testFrequencyRangeComputation() {
        double[] scales = {2.0, 4.0, 8.0, 16.0};
        
        AdaptiveScaleSelector selector = new DyadicScaleSelector();
        double[] freqRange = selector.getFrequencyRange(scales, morletWavelet, samplingRate);
        
        assertNotNull(freqRange);
        assertEquals(2, freqRange.length);
        assertTrue(freqRange[0] < freqRange[1], "Min frequency should be less than max");
        assertTrue(freqRange[0] > 0, "Frequencies should be positive");
        
        // Check approximate values
        double centerFreq = morletWavelet.centerFrequency();
        double expectedMinFreq = centerFreq * samplingRate / scales[scales.length-1];
        double expectedMaxFreq = centerFreq * samplingRate / scales[0];
        
        assertEquals(expectedMinFreq, freqRange[0], 0.01);
        assertEquals(expectedMaxFreq, freqRange[1], 0.01);
    }
    
    @Test
    @DisplayName("Should handle very small signals")
    void testSmallSignals() {
        double[] smallSignal = {1.0, -1.0, 1.0, -1.0}; // 4 samples
        
        DyadicScaleSelector selector = new DyadicScaleSelector();
        double[] scales = selector.selectScales(smallSignal, morletWavelet, samplingRate);
        
        assertNotNull(scales);
        assertTrue(scales.length > 0);
        assertTrue(scales.length < 10, "Should generate few scales for small signal");
    }
    
    @Test
    @DisplayName("Should handle single-frequency signals")
    void testSingleFrequencySignal() {
        // Pure 10 Hz sinusoid
        int N = 200;
        double[] sineSignal = new double[N];
        for (int i = 0; i < N; i++) {
            double t = i / samplingRate;
            sineSignal[i] = Math.sin(2 * Math.PI * 10 * t);
        }
        
        SignalAdaptiveScaleSelector selector = new SignalAdaptiveScaleSelector();
        double[] scales = selector.selectScales(sineSignal, morletWavelet, samplingRate);
        
        assertNotNull(scales);
        assertTrue(scales.length > 0);
        
        // Should have scales concentrated around 10 Hz
        double centerFreq = morletWavelet.centerFrequency();
        double target10HzScale = centerFreq * samplingRate / 10.0;
        
        // Count scales near the target
        int scalesNear10Hz = 0;
        for (double scale : scales) {
            if (Math.abs(scale - target10HzScale) / target10HzScale < 0.2) {
                scalesNear10Hz++;
            }
        }
        
        assertTrue(scalesNear10Hz > 0, "Should have scales near 10 Hz component");
    }
}