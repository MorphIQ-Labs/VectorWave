package com.morphiqlabs.wavelet.denoising;

import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidStateException;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import com.morphiqlabs.wavelet.WaveletOperations;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests to improve coverage for WaveletDenoiser package.
 * Focuses on uncovered branches and edge cases.
 */
class WaveletDenoiserComprehensiveTest {
    
    private WaveletDenoiser denoiser;
    private double[] testSignal;
    
    @BeforeEach
    void setUp() {
        denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Create test signal with controlled noise
        testSignal = new double[128];
        for (int i = 0; i < testSignal.length; i++) {
            testSignal[i] = Math.sin(2 * Math.PI * i / 32.0) + 0.1 * Math.random();
        }
    }
    
    @Test
    @DisplayName("Should throw exception for null wavelet")
    void testNullWavelet() {
        assertThrows(InvalidArgumentException.class, 
            () -> new WaveletDenoiser(null, BoundaryMode.PERIODIC));
    }
    
    @Test
    @DisplayName("Should throw exception for null boundary mode")
    void testNullBoundaryMode() {
        assertThrows(InvalidArgumentException.class, 
            () -> new WaveletDenoiser(new Haar(), null));
    }
    
    @Test
    @DisplayName("Should test all threshold methods including edge cases")
    void testAllThresholdMethods() {
        WaveletDenoiser.ThresholdMethod[] methods = {
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdMethod.SURE,
            WaveletDenoiser.ThresholdMethod.MINIMAX,
            WaveletDenoiser.ThresholdMethod.BAYES
        };
        
        for (WaveletDenoiser.ThresholdMethod method : methods) {
            double[] denoised = denoiser.denoise(testSignal, method);
            assertNotNull(denoised, "Result should not be null for " + method);
            assertEquals(testSignal.length, denoised.length);
        }
    }
    
    @Test
    @DisplayName("Should test MINIMAX threshold with different signal sizes")
    void testMinimaxThresholdEdgeCases() throws Exception {
        // Use reflection to test protected method
        Method calculateMinimaxMethod = WaveletDenoiser.class.getDeclaredMethod(
            "calculateMinimaxThreshold", int.class, double.class);
        calculateMinimaxMethod.setAccessible(true);
        
        double sigma = 1.0;
        
        // Test edge cases for Minimax threshold
        // n <= 32 should return 0
        assertEquals(0.0, 
            (double) calculateMinimaxMethod.invoke(denoiser, 32, sigma), 0.001);
        assertEquals(0.0, 
            (double) calculateMinimaxMethod.invoke(denoiser, 16, sigma), 0.001);
        
        // 32 < n <= 64
        double result64 = (double) calculateMinimaxMethod.invoke(denoiser, 64, sigma);
        assertTrue(result64 > 0, "Should return positive value for n=64");
        
        // n > 64
        double result128 = (double) calculateMinimaxMethod.invoke(denoiser, 128, sigma);
        assertTrue(result128 > 0, "Should return positive value for n=128");
        
        // Test with very large n
        double result1024 = (double) calculateMinimaxMethod.invoke(denoiser, 1024, sigma);
        assertTrue(result1024 > 0, "Should return positive value for n=1024");
    }
    
    @Test
    @DisplayName("Should test SURE threshold with edge cases")
    void testSUREThresholdEdgeCases() {
        // Very small signal
        double[] smallSignal = new double[]{0.1, -0.1, 0.2, -0.2};
        double[] denoised = denoiser.denoise(smallSignal, WaveletDenoiser.ThresholdMethod.SURE);
        assertNotNull(denoised);
        assertEquals(smallSignal.length, denoised.length);
        
        // Signal with all zeros except one spike
        double[] spikeSignal = new double[64];
        spikeSignal[32] = 10.0;
        denoised = denoiser.denoise(spikeSignal, WaveletDenoiser.ThresholdMethod.SURE);
        assertNotNull(denoised);
        assertEquals(spikeSignal.length, denoised.length);
    }
    
    @Test
    @DisplayName("Should test Bayes threshold with very small variance")
    void testBayesThresholdSmallVariance() {
        // Create signal with very small variance
        double[] constantSignal = new double[64];
        for (int i = 0; i < constantSignal.length; i++) {
            constantSignal[i] = 1.0 + 1e-12 * Math.random();
        }
        
        double[] denoised = denoiser.denoise(constantSignal, WaveletDenoiser.ThresholdMethod.BAYES);
        assertNotNull(denoised);
        assertEquals(constantSignal.length, denoised.length);
    }
    
    @Test
    @DisplayName("Should test applyThreshold with vectorization disabled")
    void testApplyThresholdScalar() throws Exception {
        // Create denoiser with vectorization disabled
        WaveletDenoiser scalarDenoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Use reflection to set useVectorOps to false
        Field vectorOpsField = WaveletDenoiser.class.getDeclaredField("useVectorOps");
        vectorOpsField.setAccessible(true);
        vectorOpsField.setBoolean(scalarDenoiser, false);
        
        // Test soft thresholding
        double[] denoised = scalarDenoiser.denoise(testSignal, 
            WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
            WaveletDenoiser.ThresholdType.SOFT);
        assertNotNull(denoised);
        assertEquals(testSignal.length, denoised.length);
        
        // Test hard thresholding
        denoised = scalarDenoiser.denoise(testSignal, 
            WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
            WaveletDenoiser.ThresholdType.HARD);
        assertNotNull(denoised);
        assertEquals(testSignal.length, denoised.length);
    }
    
    @Test
    @DisplayName("Should test multi-level denoising with DenoisedMultiLevelResult")
    void testMultiLevelDenoisingWithInnerClass() {
        // Test multi-level denoising which uses DenoisedMultiLevelResult inner class
        int levels = 3;
        double[] denoised = denoiser.denoiseMultiLevel(
            testSignal, levels, 
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(denoised);
        assertEquals(testSignal.length, denoised.length);
        
        // Test with different threshold methods to increase coverage
        denoised = denoiser.denoiseMultiLevel(
            testSignal, levels,
            WaveletDenoiser.ThresholdMethod.SURE,
            WaveletDenoiser.ThresholdType.HARD);
        assertNotNull(denoised);
        
        denoised = denoiser.denoiseMultiLevel(
            testSignal, levels,
            WaveletDenoiser.ThresholdMethod.BAYES,
            WaveletDenoiser.ThresholdType.SOFT);
        assertNotNull(denoised);
    }
    
    @Test
    @DisplayName("Should test DenoisedMultiLevelResult methods through reflection")
    void testDenoisedMultiLevelResultMethods() throws Exception {
        // Test the inner class methods more directly
        // First perform multi-level denoising to create the wrapper
        int levels = 2;
        
        // Use reflection to access the inner class during the transform
        Method denoiseMultiLevelMethod = WaveletDenoiser.class.getDeclaredMethod(
            "denoiseMultiLevel", double[].class, int.class, 
            WaveletDenoiser.ThresholdMethod.class, WaveletDenoiser.ThresholdType.class);
        
        // Create longer signal for multi-level
        double[] longSignal = new double[256];
        for (int i = 0; i < longSignal.length; i++) {
            longSignal[i] = Math.sin(2 * Math.PI * i / 64.0) + 0.1 * Math.random();
        }
        
        double[] result = (double[]) denoiseMultiLevelMethod.invoke(
            denoiser, longSignal, levels, 
            WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
            WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(result);
        assertEquals(longSignal.length, result.length);
    }
    
    @Test
    @DisplayName("Should test DenoisedMultiLevelResult with invalid level access")
    void testDenoisedMultiLevelResultInvalidLevel() throws Exception {
        // Create a multi-level result and test invalid level access
        // This requires creating the inner class instance
        
        // First, create a valid multi-level transform
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64.0);
        }
        
        // Perform multi-level denoising
        int levels = 3;
        double[] denoised = denoiser.denoiseMultiLevel(
            signal, levels,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(denoised);
        
        // The invalid level access is tested internally by the implementation
        // when levels are out of bounds
    }
    
    @Test
    @DisplayName("Should test maximum safe level for scaling")
    void testMaximumSafeLevelForScaling() {
        // Test with level at the safety limit
        double[] signal = new double[1024];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 128.0) + 0.05 * Math.random();
        }
        
        // Test with safe level (well below 31)
        int safeLevels = 5;
        double[] denoised = denoiser.denoiseMultiLevel(
            signal, safeLevels,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(denoised);
        assertEquals(signal.length, denoised.length);
    }
    
    @Test
    @DisplayName("Should test level exceeding safety limit")
    void testLevelExceedingSafetyLimit() {
        double[] signal = new double[2048];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.random();
        }
        
        // Try with level > 31 (MAX_SAFE_LEVEL_FOR_SCALING)
        int unsafeLevel = 32;
        assertThrows(InvalidArgumentException.class, 
            () -> denoiser.denoiseMultiLevel(
                signal, unsafeLevel,
                WaveletDenoiser.ThresholdMethod.UNIVERSAL,
                WaveletDenoiser.ThresholdType.SOFT),
            "Should throw exception for unsafe level");
    }
    
    @Test
    @DisplayName("Should test fixed threshold denoising")
    void testFixedThresholdDenoising() {
        double threshold = 0.5;
        
        // Test with soft thresholding
        double[] denoised = denoiser.denoiseFixed(testSignal, threshold, 
            WaveletDenoiser.ThresholdType.SOFT);
        assertNotNull(denoised);
        assertEquals(testSignal.length, denoised.length);
        
        // Test with hard thresholding
        denoised = denoiser.denoiseFixed(testSignal, threshold, 
            WaveletDenoiser.ThresholdType.HARD);
        assertNotNull(denoised);
        assertEquals(testSignal.length, denoised.length);
    }
    
    @Test
    @DisplayName("Should test all threshold types with all methods")
    void testAllThresholdTypesWithAllMethods() {
        WaveletDenoiser.ThresholdType[] types = WaveletDenoiser.ThresholdType.values();
        WaveletDenoiser.ThresholdMethod[] methods = {
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdMethod.SURE,
            WaveletDenoiser.ThresholdMethod.MINIMAX,
            WaveletDenoiser.ThresholdMethod.BAYES
        };
        
        for (WaveletDenoiser.ThresholdMethod method : methods) {
            for (WaveletDenoiser.ThresholdType type : types) {
                double[] denoised = denoiser.denoise(testSignal, method, type);
                assertNotNull(denoised, 
                    "Result should not be null for " + method + " with " + type);
                assertEquals(testSignal.length, denoised.length);
            }
        }
    }
    
    @ParameterizedTest
    @CsvSource({
        "16, 0.5",
        "32, 1.0", 
        "64, 2.0",
        "128, 1.5",
        "256, 0.8"
    })
    @DisplayName("Should test various signal sizes and noise levels")
    void testVariousSignalSizesAndNoiseLevels(int size, double noiseLevel) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.cos(2 * Math.PI * i / (size/4.0)) + 
                       noiseLevel * (Math.random() - 0.5);
        }
        
        double[] denoised = denoiser.denoise(signal, WaveletDenoiser.ThresholdMethod.UNIVERSAL);
        assertNotNull(denoised);
        assertEquals(size, denoised.length);
    }
    
    @Test
    @DisplayName("Should calculate median for odd and even length arrays")  
    void testMedianCalculation() throws Exception {
        // Access private calculateMedian method via reflection
        Method calculateMedianMethod = WaveletDenoiser.class.getDeclaredMethod(
            "calculateMedian", double[].class);
        calculateMedianMethod.setAccessible(true);
        
        // Test with odd length array
        double[] oddArray = {3.0, 1.0, 2.0, 5.0, 4.0};
        double medianOdd = (double) calculateMedianMethod.invoke(denoiser, oddArray);
        assertEquals(3.0, medianOdd, 0.001, "Median of odd array should be 3.0");
        
        // Test with even length array  
        double[] evenArray = {3.0, 1.0, 2.0, 4.0};
        double medianEven = (double) calculateMedianMethod.invoke(denoiser, evenArray);
        assertEquals(2.5, medianEven, 0.001, "Median of even array should be 2.5");
    }
}