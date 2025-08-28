package com.morphiqlabs.wavelet.denoising;

import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests specifically for the DenoisedMultiLevelResult inner class.
 * Uses the public API to exercise the inner class methods.
 */
class DenoisedMultiLevelResultTest {
    
    private WaveletDenoiser denoiser;
    private double[] testSignal;
    
    @BeforeEach
    void setUp() {
        denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Create a larger test signal for multi-level decomposition
        testSignal = new double[512];
        for (int i = 0; i < testSignal.length; i++) {
            // Multi-frequency signal
            testSignal[i] = Math.sin(2 * Math.PI * i / 64.0) +
                           0.5 * Math.sin(2 * Math.PI * i / 32.0) +
                           0.25 * Math.sin(2 * Math.PI * i / 16.0) +
                           0.1 * Math.random();
        }
    }
    
    @Test
    @DisplayName("Should test DenoisedMultiLevelResult through multi-level denoising")
    void testDenoisedResultCreation() {
        int levels = 4;
        
        // This creates and uses DenoisedMultiLevelResult internally
        double[] denoised = denoiser.denoiseMultiLevel(
            testSignal, levels,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(denoised);
        assertEquals(testSignal.length, denoised.length);
        
        // Verify signal is actually denoised (should have less energy in details)
        double originalEnergy = 0, denoisedEnergy = 0;
        for (int i = 0; i < testSignal.length; i++) {
            originalEnergy += testSignal[i] * testSignal[i];
            denoisedEnergy += denoised[i] * denoised[i];
        }
        
        // Denoised signal should have less energy (noise removed)
        assertTrue(denoisedEnergy < originalEnergy,
            "Denoised signal should have less energy than original");
    }
    
    @Test
    @DisplayName("Should exercise DenoisedMultiLevelResult methods through API")
    void testDenoisedResultMethods() {
        // Create wrapper that accesses internal result
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC);
        
        int levels = 3;
        MultiLevelMODWTResult originalResult = transform.decompose(testSignal, levels);
        
        // Now denoise which creates DenoisedMultiLevelResult wrapper
        double[] denoised = denoiser.denoiseMultiLevel(
            testSignal, levels,
            WaveletDenoiser.ThresholdMethod.BAYES,
            WaveletDenoiser.ThresholdType.HARD);
        
        assertNotNull(denoised);
        
        // Test with different methods to exercise more code paths
        denoised = denoiser.denoiseMultiLevel(
            testSignal, levels,
            WaveletDenoiser.ThresholdMethod.SURE,
            WaveletDenoiser.ThresholdType.SOFT);
        assertNotNull(denoised);
        
        denoised = denoiser.denoiseMultiLevel(
            testSignal, levels,
            WaveletDenoiser.ThresholdMethod.MINIMAX,
            WaveletDenoiser.ThresholdType.HARD);
        assertNotNull(denoised);
    }
    
    @Test
    @DisplayName("Should test energy calculations in DenoisedMultiLevelResult")
    void testEnergyCalculations() {
        int levels = 2;
        
        // Create a signal with known properties
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0);
        }
        
        // Denoise with soft thresholding
        double[] denoised = denoiser.denoiseMultiLevel(
            signal, levels,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(denoised);
        
        // The energy calculations are done internally
        // Verify the denoised signal maintains structure
        double maxValue = 0;
        for (double val : denoised) {
            maxValue = Math.max(maxValue, Math.abs(val));
        }
        assertTrue(maxValue > 0, "Denoised signal should not be all zeros");
        assertTrue(maxValue <= 1.5, "Denoised signal should be bounded");
    }
    
    @Test
    @DisplayName("Should test copy operation in DenoisedMultiLevelResult")
    void testCopyOperation() {
        int levels = 2;
        
        // Multiple denoising operations to ensure copy works
        double[] denoised1 = denoiser.denoiseMultiLevel(
            testSignal, levels,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdType.SOFT);
        
        double[] denoised2 = denoiser.denoiseMultiLevel(
            testSignal, levels,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(denoised1);
        assertNotNull(denoised2);
        
        // Results should be identical for same input and parameters
        assertArrayEquals(denoised1, denoised2, 1e-10,
            "Same denoising parameters should produce identical results");
    }
    
    @Test
    @DisplayName("Should test level validation in DenoisedMultiLevelResult")
    void testLevelValidation() {
        // Test with different valid levels
        for (int levels = 1; levels <= 5; levels++) {
            double[] denoised = denoiser.denoiseMultiLevel(
                testSignal, levels,
                WaveletDenoiser.ThresholdMethod.UNIVERSAL,
                WaveletDenoiser.ThresholdType.SOFT);
            assertNotNull(denoised, "Should work for level " + levels);
        }
    }
    
    @Test
    @DisplayName("Should test edge cases for multi-level denoising")
    void testMultiLevelEdgeCases() {
        // Single level (edge case)
        double[] denoised = denoiser.denoiseMultiLevel(
            testSignal, 1,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdType.SOFT);
        assertNotNull(denoised);
        assertEquals(testSignal.length, denoised.length);
        
        // Maximum practical levels
        int maxLevels = (int)(Math.log(testSignal.length) / Math.log(2)) - 2;
        denoised = denoiser.denoiseMultiLevel(
            testSignal, maxLevels,
            WaveletDenoiser.ThresholdMethod.BAYES,
            WaveletDenoiser.ThresholdType.HARD);
        assertNotNull(denoised);
        assertEquals(testSignal.length, denoised.length);
    }
    
    @Test
    @DisplayName("Should test all threshold combinations for multi-level")
    void testAllThresholdCombinations() {
        int levels = 3;
        WaveletDenoiser.ThresholdMethod[] methods = {
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdMethod.SURE,
            WaveletDenoiser.ThresholdMethod.MINIMAX,
            WaveletDenoiser.ThresholdMethod.BAYES
        };
        
        WaveletDenoiser.ThresholdType[] types = {
            WaveletDenoiser.ThresholdType.SOFT,
            WaveletDenoiser.ThresholdType.HARD
        };
        
        for (WaveletDenoiser.ThresholdMethod method : methods) {
            for (WaveletDenoiser.ThresholdType type : types) {
                double[] denoised = denoiser.denoiseMultiLevel(
                    testSignal, levels, method, type);
                assertNotNull(denoised,
                    "Should work for " + method + " with " + type);
                assertEquals(testSignal.length, denoised.length);
                
                // Verify some signal remains (not all zeros)
                double sum = 0;
                for (double val : denoised) {
                    sum += Math.abs(val);
                }
                assertTrue(sum > 0, 
                    "Denoised signal should not be all zeros for " + method + " with " + type);
            }
        }
    }
    
    @Test
    @DisplayName("Should test with signals of different characteristics")
    void testDifferentSignalCharacteristics() {
        int levels = 3;
        
        // Constant signal
        double[] constantSignal = new double[256];
        for (int i = 0; i < constantSignal.length; i++) {
            constantSignal[i] = 5.0;
        }
        double[] denoised = denoiser.denoiseMultiLevel(
            constantSignal, levels,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdType.SOFT);
        assertNotNull(denoised);
        
        // Linear trend
        double[] trendSignal = new double[256];
        for (int i = 0; i < trendSignal.length; i++) {
            trendSignal[i] = i * 0.1;
        }
        denoised = denoiser.denoiseMultiLevel(
            trendSignal, levels,
            WaveletDenoiser.ThresholdMethod.BAYES,
            WaveletDenoiser.ThresholdType.HARD);
        assertNotNull(denoised);
        
        // Step function
        double[] stepSignal = new double[256];
        for (int i = 0; i < stepSignal.length; i++) {
            stepSignal[i] = i < 128 ? 0.0 : 1.0;
        }
        denoised = denoiser.denoiseMultiLevel(
            stepSignal, levels,
            WaveletDenoiser.ThresholdMethod.SURE,
            WaveletDenoiser.ThresholdType.SOFT);
        assertNotNull(denoised);
        
        // Impulse
        double[] impulseSignal = new double[256];
        impulseSignal[128] = 10.0;
        denoised = denoiser.denoiseMultiLevel(
            impulseSignal, levels,
            WaveletDenoiser.ThresholdMethod.MINIMAX,
            WaveletDenoiser.ThresholdType.HARD);
        assertNotNull(denoised);
    }
    
    @Test
    @DisplayName("Should test very noisy signals")
    void testVeryNoisySignals() {
        // Create signal with high noise
        double[] noisySignal = new double[512];
        for (int i = 0; i < noisySignal.length; i++) {
            noisySignal[i] = 0.1 * Math.sin(2 * Math.PI * i / 64.0) + 
                            2.0 * (Math.random() - 0.5); // High noise
        }
        
        int levels = 4;
        double[] denoised = denoiser.denoiseMultiLevel(
            noisySignal, levels,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdType.HARD);
        
        assertNotNull(denoised);
        assertEquals(noisySignal.length, denoised.length);
        
        // Calculate noise reduction
        double originalVariance = calculateVariance(noisySignal);
        double denoisedVariance = calculateVariance(denoised);
        
        assertTrue(denoisedVariance < originalVariance,
            "Denoised signal should have lower variance than noisy signal");
    }
    
    private double calculateVariance(double[] signal) {
        double mean = 0;
        for (double val : signal) {
            mean += val;
        }
        mean /= signal.length;
        
        double variance = 0;
        for (double val : signal) {
            double diff = val - mean;
            variance += diff * diff;
        }
        return variance / signal.length;
    }
}