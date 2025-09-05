package com.morphiqlabs.wavelet.internal;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ScalarOps class.
 * Tests all public methods including edge cases, error conditions, and optimizations.
 */
@DisplayName("ScalarOps Test Suite")
class ScalarOpsTest {
    
    private static final double EPSILON = 1e-10;
    
    // ==========================================
    // Convolution and Downsampling Tests
    // ==========================================
    
    @Test
    @DisplayName("Test convolveAndDownsamplePeriodic with Haar filter")
    void testConvolveAndDownsamplePeriodicHaar() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] filter = {0.7071067811865475, 0.7071067811865475}; // Haar scaling
        double[] output = new double[4];
        
        ScalarOps.convolveAndDownsamplePeriodic(signal, filter, output);
        
        // Expected values calculated by hand
        double[] expected = {
            (1 + 2) * 0.7071067811865475,
            (3 + 4) * 0.7071067811865475,
            (5 + 6) * 0.7071067811865475,
            (7 + 8) * 0.7071067811865475
        };
        
        assertArrayEquals(expected, output, EPSILON);
    }
    
    @Test
    @DisplayName("Test convolveAndDownsamplePeriodic with DB2 filter")
    void testConvolveAndDownsamplePeriodicDB2() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] filter = {-0.1294095226, 0.2241438680, 0.8365163037, 0.4829629131}; // DB2
        double[] output = new double[4];
        
        ScalarOps.convolveAndDownsamplePeriodic(signal, filter, output);
        
        // Verify output has expected properties
        assertNotNull(output);
        assertEquals(4, output.length);
        // Check that convolution was performed (values should be non-zero)
        assertNotEquals(0.0, output[0], EPSILON);
        assertNotEquals(0.0, output[1], EPSILON);
    }
    
    @Test
    @DisplayName("Test convolveAndDownsamplePeriodic with slice parameters")
    void testConvolveAndDownsamplePeriodicSlice() {
        double[] signal = {0, 0, 1, 2, 3, 4, 0, 0}; // Padded signal
        double[] filter = {0.5, 0.5};
        double[] output = new double[2];
        
        // Process only the middle portion [1, 2, 3, 4]
        ScalarOps.convolveAndDownsamplePeriodic(signal, 2, 4, filter, output);
        
        // Expected: average of pairs from the slice with periodic wrapping
        double[] expected = {
            (1 + 2) * 0.5,  // indices 0,1 of slice
            (3 + 4) * 0.5   // indices 2,3 of slice
        };
        
        assertArrayEquals(expected, output, EPSILON);
    }
    
    @Test
    @DisplayName("Test convolveAndDownsampleDirect (zero padding)")
    void testConvolveAndDownsampleDirect() {
        double[] signal = {1, 2, 3, 4};
        double[] filter = {0.5, 0.5};
        double[] output = new double[2];
        
        ScalarOps.convolveAndDownsampleDirect(signal, filter, output);
        
        // With zero padding, no wrapping occurs
        double[] expected = {
            (1 + 2) * 0.5,  // indices 0,1
            (3 + 4) * 0.5   // indices 2,3
        };
        
        assertArrayEquals(expected, output, EPSILON);
    }
    
    @Test
    @DisplayName("Test convolveAndDownsampleDirect with boundary effects")
    void testConvolveAndDownsampleDirectBoundary() {
        double[] signal = {1, 2, 3, 4};
        double[] filter = {0.25, 0.25, 0.25, 0.25}; // Filter longer than half signal
        double[] output = new double[2];
        
        ScalarOps.convolveAndDownsampleDirect(signal, filter, output);
        
        // First output uses all 4 values
        assertEquals((1 + 2 + 3 + 4) * 0.25, output[0], EPSILON);
        // Second output only uses values 3,4 (zero padding for missing values)
        assertEquals((3 + 4) * 0.25, output[1], EPSILON);
    }
    
    // ==========================================
    // Upsampling and Convolution Tests
    // ==========================================
    
    @Test
    @DisplayName("Test upsampleAndConvolvePeriodic")
    void testUpsampleAndConvolvePeriodic() {
        double[] coeffs = {1, 2, 3, 4};
        double[] filter = {0.6, 0.4};
        double[] output = new double[8];
        
        ScalarOps.upsampleAndConvolvePeriodic(coeffs, filter, output);
        
        // Verify upsampling pattern: coeff affects positions 2*i and 2*i+1
        // With periodic boundary, wrapping occurs
        assertEquals(1 * 0.6, output[0], EPSILON);
        assertEquals(1 * 0.4, output[1], EPSILON);
        assertEquals(2 * 0.6, output[2], EPSILON);
        assertEquals(2 * 0.4, output[3], EPSILON);
        assertEquals(3 * 0.6, output[4], EPSILON);
        assertEquals(3 * 0.4, output[5], EPSILON);
        assertEquals(4 * 0.6, output[6], EPSILON);
        assertEquals(4 * 0.4, output[7], EPSILON);
    }
    
    @Test
    @DisplayName("Test upsampleAndConvolvePeriodic with zero coefficients")
    void testUpsampleAndConvolvePeriodicWithZeros() {
        double[] coeffs = {1, 0, 3, 0}; // Sparse coefficients
        double[] filter = {0.7, 0.3};
        double[] output = new double[8];
        
        ScalarOps.upsampleAndConvolvePeriodic(coeffs, filter, output);
        
        // Zero coefficients should be skipped for efficiency
        assertEquals(1 * 0.7, output[0], EPSILON);
        assertEquals(1 * 0.3, output[1], EPSILON);
        assertEquals(0.0, output[2], EPSILON); // From zero coefficient
        assertEquals(0.0, output[3], EPSILON);
        assertEquals(3 * 0.7, output[4], EPSILON);
        assertEquals(3 * 0.3, output[5], EPSILON);
        assertEquals(0.0, output[6], EPSILON);
        assertEquals(0.0, output[7], EPSILON);
    }
    
    @Test
    @DisplayName("Test upsampleAndConvolveDirect")
    void testUpsampleAndConvolveDirect() {
        double[] coeffs = {1, 2};
        double[] filter = {0.8, 0.2};
        double[] output = new double[4];
        
        ScalarOps.upsampleAndConvolveDirect(coeffs, filter, output);
        
        // Direct convolution without wrapping
        assertEquals(1 * 0.8, output[0], EPSILON);
        assertEquals(1 * 0.2, output[1], EPSILON);
        assertEquals(2 * 0.8, output[2], EPSILON);
        assertEquals(2 * 0.2, output[3], EPSILON);
    }
    
    // ==========================================
    // Combined Transform Tests
    // ==========================================
    
    @Test
    @DisplayName("Test combinedTransformPeriodic")
    void testCombinedTransformPeriodic() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] lowFilter = {0.5, 0.5};
        double[] highFilter = {0.5, -0.5};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        ScalarOps.combinedTransformPeriodic(signal, lowFilter, highFilter, approx, detail);
        
        // Low filter computes averages
        assertEquals((1 + 2) * 0.5, approx[0], EPSILON);
        assertEquals((3 + 4) * 0.5, approx[1], EPSILON);
        
        // High filter computes differences
        assertEquals((1 - 2) * 0.5, detail[0], EPSILON);
        assertEquals((3 - 4) * 0.5, detail[1], EPSILON);
    }
    
    @Test
    @DisplayName("Test combinedTransformPeriodic with different filter lengths")
    void testCombinedTransformPeriodicDifferentLengths() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] lowFilter = {0.25, 0.5, 0.25}; // Length 3
        double[] highFilter = {0.5, -0.5};      // Length 2
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        // Should handle biorthogonal wavelets with different filter lengths
        assertDoesNotThrow(() -> 
            ScalarOps.combinedTransformPeriodic(signal, lowFilter, highFilter, approx, detail)
        );
        
        // Verify outputs are computed
        assertNotEquals(0.0, approx[0], EPSILON);
        assertNotEquals(0.0, detail[0], EPSILON);
    }
    
    // ==========================================
    // MODWT Operations Tests
    // ==========================================
    
    @Test
    @DisplayName("Test circularConvolveMODWT")
    void testCircularConvolveMODWT() {
        double[] signal = {1, 2, 3, 4};
        double[] filter = {0.6, 0.4};
        double[] output = new double[4];
        
        ScalarOps.circularConvolveMODWT(signal, filter, output);
        
        // MODWT uses (t - l) indexing for time-reversed convolution
        // output[0] = signal[0]*filter[0] + signal[3]*filter[1] (wraps to index 3)
        assertEquals(1 * 0.6 + 4 * 0.4, output[0], EPSILON);
        // output[1] = signal[1]*filter[0] + signal[0]*filter[1]
        assertEquals(2 * 0.6 + 1 * 0.4, output[1], EPSILON);
        // output[2] = signal[2]*filter[0] + signal[1]*filter[1]
        assertEquals(3 * 0.6 + 2 * 0.4, output[2], EPSILON);
        // output[3] = signal[3]*filter[0] + signal[2]*filter[1]
        assertEquals(4 * 0.6 + 3 * 0.4, output[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test circularConvolveMODWTLevel for multi-level decomposition")
    void testCircularConvolveMODWTLevel() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] filter = {0.7, 0.3};
        double[] output = new double[8];
        
        // Level 2 means shift = 2^(2-1) = 2
        ScalarOps.circularConvolveMODWTLevel(signal, filter, output, 2);
        
        // At level 2, indices are shifted by 2
        // output[0] = signal[0]*filter[0] + signal[6]*filter[1] (0-2*1 wraps to 6)
        assertEquals(1 * 0.7 + 7 * 0.3, output[0], EPSILON);
        // output[1] = signal[1]*filter[0] + signal[7]*filter[1] (1-2*1 wraps to 7)
        assertEquals(2 * 0.7 + 8 * 0.3, output[1], EPSILON);
    }
    
    @Test
    @DisplayName("Test zeroPaddingConvolveMODWT")
    void testZeroPaddingConvolveMODWT() {
        double[] signal = {1, 2, 3, 4};
        double[] filter = {0.6, 0.4};
        double[] output = new double[4];
        
        ScalarOps.zeroPaddingConvolveMODWT(signal, filter, output);
        
        // Zero padding means no wrapping
        assertEquals(1 * 0.6 + 0 * 0.4, output[0], EPSILON); // signal[-1] = 0
        assertEquals(2 * 0.6 + 1 * 0.4, output[1], EPSILON);
        assertEquals(3 * 0.6 + 2 * 0.4, output[2], EPSILON);
        assertEquals(4 * 0.6 + 3 * 0.4, output[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test scaleFilterForMODWT")
    void testScaleFilterForMODWT() {
        double[] filter = {1.0, 1.0};
        
        // Level 1: scale by 1/sqrt(2)
        double[] scaled1 = ScalarOps.scaleFilterForMODWT(filter, 1);
        assertEquals(1.0 / Math.sqrt(2), scaled1[0], EPSILON);
        assertEquals(1.0 / Math.sqrt(2), scaled1[1], EPSILON);
        
        // Level 2: scale by 1/2
        double[] scaled2 = ScalarOps.scaleFilterForMODWT(filter, 2);
        assertEquals(0.5, scaled2[0], EPSILON);
        assertEquals(0.5, scaled2[1], EPSILON);
        
        // Level 3: scale by 1/(2*sqrt(2))
        double[] scaled3 = ScalarOps.scaleFilterForMODWT(filter, 3);
        assertEquals(1.0 / (2 * Math.sqrt(2)), scaled3[0], EPSILON);
        assertEquals(1.0 / (2 * Math.sqrt(2)), scaled3[1], EPSILON);
    }
    
    // ==========================================
    // Error Handling Tests
    // ==========================================
    
    @Test
    @DisplayName("Test null parameter validation")
    void testNullParameterValidation() {
        double[] signal = {1, 2, 3, 4};
        double[] filter = {0.5, 0.5};
        double[] output = new double[2];
        
        // Test null signal - the slice version has proper validation
        InvalidArgumentException e1 = assertThrows(InvalidArgumentException.class, () ->
            ScalarOps.convolveAndDownsamplePeriodic(null, 0, 4, filter, output)
        );
        assertEquals(ErrorCode.VAL_NULL_ARGUMENT, e1.getErrorCode());
        
        // Test null filter
        InvalidArgumentException e2 = assertThrows(InvalidArgumentException.class, () ->
            ScalarOps.convolveAndDownsamplePeriodic(signal, 0, 4, null, output)
        );
        assertEquals(ErrorCode.VAL_NULL_ARGUMENT, e2.getErrorCode());
        
        // Test null output
        InvalidArgumentException e3 = assertThrows(InvalidArgumentException.class, () ->
            ScalarOps.convolveAndDownsamplePeriodic(signal, 0, 4, filter, null)
        );
        assertEquals(ErrorCode.VAL_NULL_ARGUMENT, e3.getErrorCode());
    }
    
    @Test
    @DisplayName("Test array length validation")
    void testArrayLengthValidation() {
        double[] signal = {1, 2, 3, 4};
        double[] filter = {0.5, 0.5};
        double[] wrongOutput = new double[3]; // Wrong size
        
        InvalidArgumentException e = assertThrows(InvalidArgumentException.class, () ->
            ScalarOps.convolveAndDownsamplePeriodic(signal, filter, wrongOutput)
        );
        assertEquals(ErrorCode.VAL_LENGTH_MISMATCH, e.getErrorCode());
        assertTrue(e.getMessage().contains("incorrect length"));
    }
    
    @Test
    @DisplayName("Test slice bounds validation")
    void testSliceBoundsValidation() {
        double[] signal = {1, 2, 3, 4};
        double[] filter = {0.5, 0.5};
        double[] output = new double[2];
        
        // Negative offset
        assertThrows(IndexOutOfBoundsException.class, () ->
            ScalarOps.convolveAndDownsamplePeriodic(signal, -1, 4, filter, output)
        );
        
        // Offset + length > array length
        assertThrows(IndexOutOfBoundsException.class, () ->
            ScalarOps.convolveAndDownsamplePeriodic(signal, 2, 4, filter, output)
        );
        
        // Negative length
        assertThrows(IndexOutOfBoundsException.class, () ->
            ScalarOps.convolveAndDownsamplePeriodic(signal, 0, -1, filter, output)
        );
    }
    
    @Test
    @DisplayName("Test MODWT level overflow protection")
    void testMODWTLevelOverflow() {
        double[] signal = {1, 2, 3, 4};
        double[] filter = {0.5, 0.5};
        double[] output = new double[4];
        
        // Level 32 would cause integer overflow (2^31)
        InvalidArgumentException e = assertThrows(InvalidArgumentException.class, () ->
            ScalarOps.circularConvolveMODWTLevel(signal, filter, output, 32)
        );
        assertEquals(ErrorCode.VAL_TOO_LARGE, e.getErrorCode());
        assertTrue(e.getMessage().contains("overflow"));
    }
    
    // ==========================================
    // Wrapper Methods Tests
    // ==========================================
    
    @Test
    @DisplayName("Test wrapper methods for WaveletOpsFactory")
    void testWrapperMethods() {
        double[] signal = {1, 2, 3, 4};
        double[] filter = {0.5, 0.5};
        
        // Test periodic convolution wrapper
        double[] result1 = ScalarOps.convolveAndDownsamplePeriodic(signal, filter, 4, 2);
        assertNotNull(result1);
        assertEquals(2, result1.length);
        
        // Test zero-padding convolution wrapper
        double[] result2 = ScalarOps.convolveAndDownsampleZeroPadding(signal, filter, 4, 2);
        assertNotNull(result2);
        assertEquals(2, result2.length);
        
        // Test periodic upsampling wrapper
        double[] coeffs = {1, 2};
        double[] result3 = ScalarOps.upsampleAndConvolvePeriodic(coeffs, filter, 2, 2);
        assertNotNull(result3);
        assertEquals(4, result3.length);
        
        // Test zero-padding upsampling wrapper
        double[] result4 = ScalarOps.upsampleAndConvolveZeroPadding(coeffs, filter, 2, 2);
        assertNotNull(result4);
        assertEquals(4, result4.length);
    }
    
    @Test
    @DisplayName("Test wrapper methods null validation")
    void testWrapperMethodsNullValidation() {
        double[] signal = {1, 2, 3, 4};
        double[] filter = {0.5, 0.5};
        
        // Test null signal
        assertThrows(IllegalArgumentException.class, () ->
            ScalarOps.convolveAndDownsamplePeriodic(null, filter, 4, 2)
        );
        
        // Test null filter
        assertThrows(IllegalArgumentException.class, () ->
            ScalarOps.convolveAndDownsamplePeriodic(signal, null, 4, 2)
        );
    }
    
    // ==========================================
    // Performance and Optimization Tests
    // ==========================================
    
    @Test
    @DisplayName("Test getPerformanceInfo")
    void testGetPerformanceInfo() {
        ScalarOps.PerformanceInfo info = ScalarOps.getPerformanceInfo();
        
        assertNotNull(info);
        assertNotNull(info.description());
        assertTrue(info.availableProcessors() > 0);
        
        // Test speedup estimation for different array sizes
        double speedup1 = info.estimateSpeedup(1024);
        double speedup2 = info.estimateSpeedup(32);
        double speedup3 = info.estimateSpeedup(8192);
        
        assertTrue(speedup1 >= 1.0); // Should be at least 1x (no slowdown)
        assertTrue(speedup2 >= 1.0);
        assertTrue(speedup3 >= 1.0);
        
        // Test description contains useful information
        String desc = info.description();
        assertTrue(desc.contains("CPU cores") || desc.contains("core"));
        
        // Test that performance info is consistent across calls
        ScalarOps.PerformanceInfo info2 = ScalarOps.getPerformanceInfo();
        assertEquals(info.vectorizationEnabled(), info2.vectorizationEnabled());
        assertEquals(info.availableProcessors(), info2.availableProcessors());
    }
    
    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024})
    @DisplayName("Test operations with power-of-2 sizes")
    void testPowerOfTwoOptimizations(int size) {
        double[] signal = new double[size];
        double[] filter = {0.5, 0.5};
        double[] output = new double[size / 2];
        
        // Initialize signal
        for (int i = 0; i < size; i++) {
            signal[i] = i + 1;
        }
        
        // Should use optimized implementation for power-of-2
        assertDoesNotThrow(() -> 
            ScalarOps.convolveAndDownsamplePeriodic(signal, filter, output)
        );
        
        // Verify output is computed
        assertNotEquals(0.0, output[0], EPSILON);
        assertNotEquals(0.0, output[output.length - 1], EPSILON);
    }
    
    @ParameterizedTest
    @CsvSource({
        "8, 2",   // Haar
        "16, 4",  // DB2
        "32, 6",  // DB3
        "64, 8"   // DB4
    })
    @DisplayName("Test specialized implementations for different filter sizes")
    void testSpecializedImplementations(int signalSize, int filterSize) {
        double[] signal = new double[signalSize];
        double[] filter = new double[filterSize];
        double[] output = new double[signalSize / 2];
        
        // Initialize
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signalSize);
        }
        for (int i = 0; i < filterSize; i++) {
            filter[i] = 1.0 / filterSize; // Averaging filter
        }
        
        // Test runs without error and produces output
        assertDoesNotThrow(() -> 
            ScalarOps.convolveAndDownsamplePeriodic(signal, filter, output)
        );
        
        // Verify some computation occurred
        double sum = 0;
        for (double v : output) {
            sum += Math.abs(v);
        }
        assertTrue(sum > 0, "Output should contain non-zero values");
    }
    
    // ==========================================
    // Thresholding Tests
    // ==========================================
    
    @Test
    @DisplayName("Test soft thresholding")
    void testSoftThreshold() {
        double[] coeffs = {-3.0, -1.5, -0.5, 0.0, 0.5, 1.5, 3.0};
        double threshold = 1.0;
        
        double[] result = ScalarOps.softThreshold(coeffs, threshold);
        
        // Expected soft thresholding behavior
        assertEquals(-2.0, result[0], EPSILON); // -3.0 -> -2.0
        assertEquals(-0.5, result[1], EPSILON); // -1.5 -> -0.5
        assertEquals(0.0, result[2], EPSILON);  // -0.5 -> 0.0 (within threshold)
        assertEquals(0.0, result[3], EPSILON);  // 0.0 -> 0.0
        assertEquals(0.0, result[4], EPSILON);  // 0.5 -> 0.0 (within threshold)
        assertEquals(0.5, result[5], EPSILON);  // 1.5 -> 0.5
        assertEquals(2.0, result[6], EPSILON);  // 3.0 -> 2.0
    }
    
    @Test
    @DisplayName("Test hard thresholding")
    void testHardThreshold() {
        double[] coeffs = {-3.0, -1.5, -0.5, 0.0, 0.5, 1.5, 3.0};
        double threshold = 1.0;
        
        double[] result = ScalarOps.hardThreshold(coeffs, threshold);
        
        // Expected hard thresholding behavior
        assertEquals(-3.0, result[0], EPSILON); // -3.0 preserved
        assertEquals(-1.5, result[1], EPSILON); // -1.5 preserved
        assertEquals(0.0, result[2], EPSILON);  // -0.5 -> 0.0 (below threshold)
        assertEquals(0.0, result[3], EPSILON);  // 0.0 -> 0.0
        assertEquals(0.0, result[4], EPSILON);  // 0.5 -> 0.0 (below threshold)
        assertEquals(1.5, result[5], EPSILON);  // 1.5 preserved
        assertEquals(3.0, result[6], EPSILON);  // 3.0 preserved
    }
    
    // ==========================================
    // Edge Cases and Special Conditions
    // ==========================================
    
    @Test
    @DisplayName("Test with very small signals")
    void testVerySmallSignals() {
        double[] signal = {1.0, 2.0};
        double[] filter = {0.7071, 0.7071};
        double[] output = new double[1];
        
        ScalarOps.convolveAndDownsamplePeriodic(signal, filter, output);
        
        assertEquals((1.0 + 2.0) * 0.7071, output[0], EPSILON);
    }
    
    @Test
    @DisplayName("Test with single coefficient")
    void testSingleCoefficient() {
        double[] coeffs = {5.0};
        double[] filter = {0.8, 0.2};
        double[] output = new double[2];
        
        ScalarOps.upsampleAndConvolvePeriodic(coeffs, filter, output);
        
        assertEquals(5.0 * 0.8, output[0], EPSILON);
        assertEquals(5.0 * 0.2, output[1], EPSILON);
    }
    
    @Test
    @DisplayName("Test with all zero signal")
    void testAllZeroSignal() {
        double[] signal = {0.0, 0.0, 0.0, 0.0};
        double[] filter = {0.5, 0.5};
        double[] output = new double[2];
        
        ScalarOps.convolveAndDownsamplePeriodic(signal, filter, output);
        
        assertArrayEquals(new double[]{0.0, 0.0}, output, EPSILON);
    }
    
    @Test
    @DisplayName("Test with very large filter")
    void testLargeFilter() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] filter = new double[16]; // Filter longer than signal
        for (int i = 0; i < filter.length; i++) {
            filter[i] = 1.0 / filter.length;
        }
        double[] output = new double[4];
        
        // Should handle wrapping correctly with periodic boundaries
        assertDoesNotThrow(() -> 
            ScalarOps.convolveAndDownsamplePeriodic(signal, filter, output)
        );
        
        // All outputs should be the same (average of all values due to wrapping)
        double expectedAvg = (1 + 2 + 3 + 4 + 5 + 6 + 7 + 8) / 8.0;
        for (double val : output) {
            assertEquals(expectedAvg, val, EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test MODWT circular convolution with extreme levels")
    void testMODWTCircularConvolutionExtremeLevels() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] filter = {0.5, 0.3, 0.2};
        double[] output = new double[8];
        
        // Test very high level (should work but use fallback)
        assertDoesNotThrow(() -> 
            ScalarOps.circularConvolveMODWTLevel(signal, filter, output, 20)
        );
        
        // Verify output is valid
        for (double val : output) {
            assertTrue(Double.isFinite(val), "Output should be finite");
        }
    }
    
    @Test
    @DisplayName("Test MODWT filter scaling for multiple levels")
    void testMODWTFilterScalingMultipleLevels() {
        double[] originalFilter = {0.7071067811865475, 0.7071067811865475}; // Haar
        
        // Test scaling for levels 1-5
        for (int level = 1; level <= 5; level++) {
            double[] scaledFilter = ScalarOps.scaleFilterForMODWT(originalFilter, level);
            
            assertNotNull(scaledFilter, "Scaled filter should not be null for level " + level);
            assertEquals(originalFilter.length, scaledFilter.length, 
                        "Scaled filter should have same length as original");
            
            // Check that scaling factor is applied correctly
            double expectedScale = 1.0 / Math.sqrt(Math.pow(2, level));
            for (int i = 0; i < originalFilter.length; i++) {
                assertEquals(originalFilter[i] * expectedScale, scaledFilter[i], EPSILON,
                           "Filter coefficient " + i + " at level " + level + " should be scaled correctly");
            }
        }
    }
    
    @Test
    @DisplayName("Test zero padding convolution edge cases")
    void testZeroPaddingConvolutionEdgeCases() {
        // Test with signal shorter than filter
        double[] shortSignal = {1, 2};
        double[] longFilter = {0.25, 0.25, 0.25, 0.25};
        double[] output = new double[2];
        
        assertDoesNotThrow(() -> 
            ScalarOps.zeroPaddingConvolveMODWT(shortSignal, longFilter, output)
        );
        
        // Test with single element signal
        double[] singleSignal = {5.0};
        double[] filterForSingle = {0.5, 0.5};
        double[] singleOutput = new double[1];
        
        assertDoesNotThrow(() -> 
            ScalarOps.zeroPaddingConvolveMODWT(singleSignal, filterForSingle, singleOutput)
        );
    }
    
    @Test
    @DisplayName("Test combinedTransformPeriodic with different filter lengths")
    void testCombinedTransformPeriodicFilterLengths() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        // Test with different low-pass and high-pass filter lengths
        double[] shortLowFilter = {0.7071067811865475, 0.7071067811865475};
        double[] longHighFilter = {0.3536, -0.3536, 0.3536, -0.3536};
        
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        assertDoesNotThrow(() -> 
            ScalarOps.combinedTransformPeriodic(signal, shortLowFilter, longHighFilter, approx, detail)
        );
        
        // Verify outputs are valid
        for (int i = 0; i < 4; i++) {
            assertTrue(Double.isFinite(approx[i]), "Approximation coefficient should be finite");
            assertTrue(Double.isFinite(detail[i]), "Detail coefficient should be finite");
        }
    }
    
    @Test
    @DisplayName("Test performance estimation with extreme array sizes")
    void testPerformanceEstimationExtremeArraySizes() {
        ScalarOps.PerformanceInfo info = ScalarOps.getPerformanceInfo();
        
        // Test with very small arrays
        double speedup1 = info.estimateSpeedup(1);
        double speedup2 = info.estimateSpeedup(2);
        assertTrue(speedup1 >= 1.0);
        assertTrue(speedup2 >= 1.0);
        
        // Test with very large arrays
        double speedup3 = info.estimateSpeedup(100000);
        double speedup4 = info.estimateSpeedup(1000000);
        assertTrue(speedup3 >= 1.0);
        assertTrue(speedup4 >= 1.0);
        
        // Test with zero and negative sizes (should handle gracefully)
        double speedup5 = info.estimateSpeedup(0);
        double speedup6 = info.estimateSpeedup(-1);
        assertTrue(speedup5 >= 0.0); // May return 0 or positive
        assertTrue(speedup6 >= 0.0); // May return 0 or positive
    }
    
    @Test
    @DisplayName("Test wrapper method consistency")
    void testWrapperMethodConsistency() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] filter = {0.5, 0.5};
        
        // Test that wrapper methods return same results as direct methods
        double[] result1 = ScalarOps.convolveAndDownsamplePeriodic(signal, filter, signal.length, filter.length);
        double[] output = new double[4];
        ScalarOps.convolveAndDownsamplePeriodic(signal, filter, output);
        
        assertArrayEquals(result1, output, EPSILON, 
                         "Wrapper method should produce same result as direct method");
        
        // Test zero-padding wrapper
        double[] result2 = ScalarOps.convolveAndDownsampleZeroPadding(signal, filter, signal.length, filter.length);
        assertNotNull(result2);
        assertEquals(4, result2.length); // Should be signal.length / 2
        
        // Test upsample wrappers
        double[] coeffs = {1, 2, 3, 4};
        double[] result3 = ScalarOps.upsampleAndConvolvePeriodic(coeffs, filter, coeffs.length, filter.length);
        double[] result4 = ScalarOps.upsampleAndConvolveZeroPadding(coeffs, filter, coeffs.length, filter.length);
        
        assertNotNull(result3);
        assertNotNull(result4);
        assertEquals(8, result3.length); // Should be coeffs.length * 2
        assertEquals(8, result4.length); // Should be coeffs.length * 2
    }
    
    @Test
    @DisplayName("Test boundary condition handling in slice operations")
    void testSliceOperationBoundaryConditions() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        double[] filter = {0.5, 0.5};
        double[] output = new double[3];
        
        // Test operation on slice at the end of array
        assertDoesNotThrow(() -> 
            ScalarOps.convolveAndDownsamplePeriodic(signal, 4, 6, filter, output)
        );
        
        // Test slice that exactly fits
        double[] output2 = new double[2];
        assertDoesNotThrow(() -> 
            ScalarOps.convolveAndDownsamplePeriodic(signal, 6, 4, filter, output2)
        );
        
        // Verify results are valid
        for (double val : output) {
            assertTrue(Double.isFinite(val));
        }
        for (double val : output2) {
            assertTrue(Double.isFinite(val));
        }
    }
}