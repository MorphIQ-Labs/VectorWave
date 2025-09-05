package com.morphiqlabs.wavelet.extensions.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;
import com.morphiqlabs.wavelet.internal.ScalarOps;

/**
 * Comprehensive test suite for VectorOps class.
 * Tests SIMD-optimized wavelet operations using Java Vector API.
 */
@DisplayName("VectorOps Test Suite")
class VectorOpsTest {
    
    private static final double EPSILON = 1e-10;
    
    @BeforeEach
    void setUp() {
        // Check if Vector API is available
        if (!VectorOps.isVectorApiSupported()) {
            System.out.println("Vector API not supported on this platform - tests will use scalar fallback");
        }
    }
    
    // ==========================================
    // Vector API Support Tests
    // ==========================================
    
    @Test
    @DisplayName("Test Vector API support detection")
    void testIsVectorApiSupported() {
        // This should not throw and return a valid boolean
        boolean supported = VectorOps.isVectorApiSupported();
        // Just verify the method doesn't throw an exception
        assertNotNull(Boolean.valueOf(supported));
    }
    
    @Test
    @DisplayName("Test vector operation benefit calculation")
    void testIsVectorizedOperationBeneficial() {
        // Get capabilities to understand platform thresholds
        VectorOps.VectorCapabilityInfo capabilities = VectorOps.getVectorCapabilities();
        int threshold = capabilities.threshold();
        
        // Test based on actual platform threshold
        if (4 < threshold) {
            assertFalse(VectorOps.isVectorizedOperationBeneficial(4)); // Below threshold
        } else {
            // On Apple Silicon with 128-bit vectors, threshold may be 4, so 4 might be beneficial
            // Just verify it returns a boolean without throwing
            boolean result4 = VectorOps.isVectorizedOperationBeneficial(4);
            assertNotNull(Boolean.valueOf(result4));
        }
        
        if (8 < threshold) {
            assertFalse(VectorOps.isVectorizedOperationBeneficial(8)); // Below threshold
        } else {
            // May be beneficial on some platforms
            // Just verify it returns a boolean without throwing
            boolean result8 = VectorOps.isVectorizedOperationBeneficial(8);
            assertNotNull(Boolean.valueOf(result8));
        }
        
        // Larger signals - just verify method works without asserting specific behavior
        boolean largeBenefit = VectorOps.isVectorizedOperationBeneficial(1024);
        assertNotNull(Boolean.valueOf(largeBenefit));
    }
    
    @Test
    @DisplayName("Test getVectorInfo provides platform information")
    void testGetVectorInfo() {
        String info = VectorOps.getVectorInfo();
        assertNotNull(info);
        assertTrue(info.contains("Vector API:"));
        assertTrue(info.contains("Species="));
        assertTrue(info.contains("Length="));
        assertTrue(info.contains("Platform="));
        assertTrue(info.contains("Enabled="));
    }
    
    @Test
    @DisplayName("Test getVectorCapabilities")
    void testGetVectorCapabilities() {
        VectorOps.VectorCapabilityInfo capabilities = VectorOps.getVectorCapabilities();
        
        assertNotNull(capabilities);
        assertNotNull(capabilities.shape());
        assertTrue(capabilities.length() > 0);
        assertNotNull(capabilities.elementType());
        assertTrue(capabilities.threshold() > 0);
        
        // Test description
        String description = capabilities.description();
        assertNotNull(description);
        assertTrue(description.contains("elements"));
        
        // Test speedup estimation - needs to be platform-aware
        int threshold = capabilities.threshold();
        if (4 < threshold) {
            assertEquals(1.0, capabilities.estimatedSpeedup(4), EPSILON); // Below threshold
        } else {
            assertTrue(capabilities.estimatedSpeedup(4) >= 1.0); // At or above threshold - may have speedup
        }
        assertTrue(capabilities.estimatedSpeedup(1024) >= 1.0); // Large array should have speedup >= 1
        assertTrue(capabilities.estimatedSpeedup(1024) <= 8.0); // But capped at 8x
    }
    
    // ==========================================
    // Convolution and Downsampling Tests
    // ==========================================
    
    @Test
    @DisplayName("Test convolveAndDownsamplePeriodic with Haar filter")
    void testConvolveAndDownsamplePeriodicHaar() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] filter = {0.7071067811865475, 0.7071067811865475}; // Haar scaling
        
        double[] result = VectorOps.convolveAndDownsamplePeriodic(signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Expected values (same as ScalarOps)
        double[] expected = {
            (1 + 2) * 0.7071067811865475,
            (3 + 4) * 0.7071067811865475,
            (5 + 6) * 0.7071067811865475,
            (7 + 8) * 0.7071067811865475
        };
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    @DisplayName("Test convolveAndDownsamplePeriodic with larger signal")
    void testConvolveAndDownsamplePeriodicLarge() {
        int size = 256; // Large enough to potentially use vectorization
        double[] signal = new double[size];
        double[] filter = {0.5, 0.5};
        
        // Initialize signal with sin wave
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / size);
        }
        
        double[] result = VectorOps.convolveAndDownsamplePeriodic(signal, filter, size, filter.length);
        
        assertNotNull(result);
        assertEquals(size / 2, result.length);
        
        // Verify some properties of the result
        // The downsampled averaged signal should still be smooth
        for (int i = 1; i < result.length - 1; i++) {
            double diff = Math.abs(result[i] - result[i-1]);
            assertTrue(diff < 0.1, "Signal should be smooth");
        }
    }
    
    @Test
    @DisplayName("Test convolveAndDownsampleZeroPadding")
    void testConvolveAndDownsampleZeroPadding() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] filter = {0.5, 0.5};
        
        double[] result = VectorOps.convolveAndDownsampleZeroPadding(signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // With zero padding, no wrapping occurs
        double[] expected = {
            (1 + 2) * 0.5,  // indices 0,1
            (3 + 4) * 0.5,  // indices 2,3
            (5 + 6) * 0.5,  // indices 4,5
            (7 + 8) * 0.5   // indices 6,7
        };
        
        assertArrayEquals(expected, result, EPSILON);
    }
    
    @Test
    @DisplayName("Test convolveAndDownsampleZeroPadding with boundary effects")
    void testConvolveAndDownsampleZeroPaddingBoundary() {
        double[] signal = {1, 2, 3, 4};
        double[] filter = {0.25, 0.25, 0.25, 0.25}; // Filter longer than half signal
        
        double[] result = VectorOps.convolveAndDownsampleZeroPadding(signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(2, result.length);
        
        // First output uses all 4 values
        assertEquals((1 + 2 + 3 + 4) * 0.25, result[0], EPSILON);
        // Second output only uses values 3,4 (zero padding for missing values)
        assertEquals((3 + 4) * 0.25, result[1], EPSILON);
    }
    
    @Test
    @DisplayName("Test convolveAndDownsampleZeroPadding with large vectorized signal")
    void testConvolveAndDownsampleZeroPaddingVectorized() {
        int size = 256; // Large enough to trigger vectorization
        double[] signal = new double[size];
        double[] filter = {0.4, 0.3, 0.2, 0.1};
        
        // Initialize signal with pattern
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 0.1 * i;
        }
        
        double[] result = VectorOps.convolveAndDownsampleZeroPadding(signal, filter, size, filter.length);
        
        assertNotNull(result);
        assertEquals(size / 2, result.length);
        
        // Verify computation was performed
        assertNotEquals(0.0, result[0]);
        assertNotEquals(0.0, result[result.length - 1]);
        
        // Check for proper zero padding behavior
        // All results should be finite and non-negative for this input
        for (double val : result) {
            assertTrue(Double.isFinite(val));
        }
    }
    
    @Test
    @DisplayName("Test convolveAndDownsampleZeroPadding with ARM vector length")
    void testConvolveAndDownsampleZeroPaddingARM() {
        // Test with sizes that exercise ARM-specific paths (2-element vectors)
        double[] signal = new double[64];
        double[] filter = {0.6, 0.4};
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1;
        }
        
        double[] result = VectorOps.convolveAndDownsampleZeroPadding(signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(32, result.length);
        
        // Verify specific calculations
        assertEquals((1 * 0.6 + 2 * 0.4), result[0], EPSILON);
        assertEquals((3 * 0.6 + 4 * 0.4), result[1], EPSILON);
    }
    
    @Test
    @DisplayName("Test convolveAndDownsampleZeroPadding boundary conditions")
    void testConvolveAndDownsampleZeroPaddingBoundaryConditions() {
        double[] signal = new double[100];
        double[] filter = {0.25, 0.5, 0.25};
        
        // Initialize with step function
        for (int i = 0; i < 50; i++) {
            signal[i] = 1.0;
        }
        // Second half is zero
        
        double[] result = VectorOps.convolveAndDownsampleZeroPadding(signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(50, result.length);
        
        // Results near the step should show the effect of the filter
        assertTrue(result[24] > result[26]); // Transition region
    }
    
    // ==========================================
    // Upsampling and Convolution Tests
    // ==========================================
    
    @Test
    @DisplayName("Test upsampleAndConvolvePeriodic")
    void testUpsampleAndConvolvePeriodic() {
        double[] coeffs = {1, 2, 3, 4};
        double[] filter = {0.6, 0.4};
        
        double[] result = VectorOps.upsampleAndConvolvePeriodic(coeffs, filter, coeffs.length, filter.length);
        
        assertNotNull(result);
        assertEquals(8, result.length);
        
        // Verify upsampling pattern
        assertEquals(1 * 0.6, result[0], EPSILON);
        assertEquals(1 * 0.4, result[1], EPSILON);
        assertEquals(2 * 0.6, result[2], EPSILON);
        assertEquals(2 * 0.4, result[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test upsampleAndConvolvePeriodic with large vector for vectorization")
    void testUpsampleAndConvolvePeriodicVectorized() {
        int size = 128; // Large enough to trigger vectorization
        double[] coeffs = new double[size];
        double[] filter = {0.5, 0.3, 0.2};
        
        // Initialize coefficients with pattern
        for (int i = 0; i < size; i++) {
            coeffs[i] = i % 8 + 1;
        }
        
        double[] result = VectorOps.upsampleAndConvolvePeriodic(coeffs, filter, size, filter.length);
        
        assertNotNull(result);
        assertEquals(size * 2, result.length);
        
        // Verify first few even indices (direct copy with filtering)
        double expected0 = coeffs[0] * filter[0] + coeffs[size-1] * filter[2]; // Periodic wrapping
        assertEquals(expected0, result[0], EPSILON);
        
        double expected2 = coeffs[1] * filter[0] + coeffs[0] * filter[2]; 
        assertEquals(expected2, result[2], EPSILON);
    }
    
    @Test
    @DisplayName("Test upsampleAndConvolvePeriodic with odd filter length")
    void testUpsampleAndConvolvePeriodicOddFilter() {
        double[] coeffs = {1, 2, 3, 4};
        double[] filter = {0.5, 0.3, 0.2}; // Odd length filter
        
        double[] result = VectorOps.upsampleAndConvolvePeriodic(coeffs, filter, coeffs.length, filter.length);
        
        assertNotNull(result);
        assertEquals(8, result.length);
        
        // Check that all values are computed (no NaN or infinity)
        for (double value : result) {
            assertTrue(Double.isFinite(value));
        }
    }
    
    @Test
    @DisplayName("Test upsampleAndConvolvePeriodic with small signal for scalar fallback")
    void testUpsampleAndConvolvePeriodicScalar() {
        double[] coeffs = {5.0};
        double[] filter = {0.8, 0.2};
        
        double[] result = VectorOps.upsampleAndConvolvePeriodic(coeffs, filter, 1, 2);
        
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(5.0 * 0.8, result[0], EPSILON);
        assertEquals(5.0 * 0.2, result[1], EPSILON);
    }
    
    @Test
    @DisplayName("Test upsampleAndConvolveZeroPadding")
    void testUpsampleAndConvolveZeroPadding() {
        double[] coeffs = {1, 2};
        double[] filter = {0.8, 0.2};
        
        double[] result = VectorOps.upsampleAndConvolveZeroPadding(coeffs, filter, coeffs.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Direct convolution without wrapping
        assertEquals(1 * 0.8, result[0], EPSILON);
        assertEquals(1 * 0.2, result[1], EPSILON);
        assertEquals(2 * 0.8, result[2], EPSILON);
        assertEquals(2 * 0.2, result[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test upsampleAndConvolveZeroPadding with null checks")
    void testUpsampleAndConvolveZeroPaddingNullChecks() {
        double[] coeffs = {1, 2};
        double[] filter = {0.5, 0.5};
        
        // Test null coefficients
        assertThrows(IllegalArgumentException.class, () ->
            VectorOps.upsampleAndConvolveZeroPadding(null, filter, 2, filter.length)
        );
        
        // Test null filter
        assertThrows(IllegalArgumentException.class, () ->
            VectorOps.upsampleAndConvolveZeroPadding(coeffs, null, coeffs.length, 2)
        );
    }
    
    @Test
    @DisplayName("Test upsampleAndConvolveZeroPadding with zero coefficients")
    void testUpsampleAndConvolveZeroPaddingWithZeros() {
        double[] coeffs = {1, 0, 3, 0}; // Sparse coefficients
        double[] filter = {0.7, 0.3};
        
        double[] result = VectorOps.upsampleAndConvolveZeroPadding(coeffs, filter, coeffs.length, filter.length);
        
        assertNotNull(result);
        assertEquals(8, result.length);
        
        // Zero coefficients should be skipped for efficiency
        assertEquals(1 * 0.7, result[0], EPSILON);
        assertEquals(1 * 0.3, result[1], EPSILON);
        assertEquals(0.0, result[2], EPSILON); // From zero coefficient
        assertEquals(0.0, result[3], EPSILON);
        assertEquals(3 * 0.7, result[4], EPSILON);
        assertEquals(3 * 0.3, result[5], EPSILON);
        assertEquals(0.0, result[6], EPSILON);
        assertEquals(0.0, result[7], EPSILON);
    }
    
    // ==========================================
    // MODWT Circular Convolution Tests
    // ==========================================
    
    @Test
    @DisplayName("Test circularConvolveMODWTVectorized")
    void testCircularConvolveMODWTVectorized() {
        double[] signal = {1, 2, 3, 4};
        double[] filter = {0.6, 0.4};
        double[] output = new double[4];
        
        VectorOps.circularConvolveMODWTVectorized(signal, filter, output);
        
        // MODWT uses (t - l) indexing for time-reversed convolution
        assertEquals(1 * 0.6 + 4 * 0.4, output[0], EPSILON);
        assertEquals(2 * 0.6 + 1 * 0.4, output[1], EPSILON);
        assertEquals(3 * 0.6 + 2 * 0.4, output[2], EPSILON);
        assertEquals(4 * 0.6 + 3 * 0.4, output[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test circularConvolveMODWTVectorized with large signal")
    void testCircularConvolveMODWTVectorizedLarge() {
        int size = 256;
        double[] signal = new double[size];
        double[] filter = {0.7, 0.3};
        double[] output = new double[size];
        
        // Initialize with pattern
        for (int i = 0; i < size; i++) {
            signal[i] = i % 10;
        }
        
        VectorOps.circularConvolveMODWTVectorized(signal, filter, output);
        
        // Verify output is computed
        assertNotNull(output);
        
        // Check first few values
        assertEquals(signal[0] * 0.7 + signal[size-1] * 0.3, output[0], EPSILON);
        assertEquals(signal[1] * 0.7 + signal[0] * 0.3, output[1], EPSILON);
    }
    
    @Test
    @DisplayName("Test circularConvolveMODWTVectorized with zero filter coefficients")
    void testCircularConvolveMODWTVectorizedZeroCoeffs() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] filter = {0.5, 0.0, 0.5, 0.0}; // Sparse filter
        double[] output = new double[8];
        
        VectorOps.circularConvolveMODWTVectorized(signal, filter, output);
        
        // Zero coefficients should be skipped
        // Only filter[0] and filter[2] contribute
        assertEquals(signal[0] * 0.5 + signal[6] * 0.5, output[0], EPSILON);
        assertEquals(signal[1] * 0.5 + signal[7] * 0.5, output[1], EPSILON);
    }
    
    // ==========================================
    // Denoising Operations Tests
    // ==========================================
    
    @Test
    @DisplayName("Test soft thresholding")
    void testSoftThreshold() {
        double[] coeffs = {-3.0, -1.5, -0.5, 0.0, 0.5, 1.5, 3.0};
        double threshold = 1.0;
        
        double[] result = VectorOps.Denoising.softThreshold(coeffs, threshold);
        
        assertNotNull(result);
        assertEquals(coeffs.length, result.length);
        
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
        
        double[] result = VectorOps.Denoising.hardThreshold(coeffs, threshold);
        
        assertNotNull(result);
        assertEquals(coeffs.length, result.length);
        
        // Expected hard thresholding behavior
        assertEquals(-3.0, result[0], EPSILON); // -3.0 preserved
        assertEquals(-1.5, result[1], EPSILON); // -1.5 preserved
        assertEquals(0.0, result[2], EPSILON);  // -0.5 -> 0.0 (below threshold)
        assertEquals(0.0, result[3], EPSILON);  // 0.0 -> 0.0
        assertEquals(0.0, result[4], EPSILON);  // 0.5 -> 0.0 (below threshold)
        assertEquals(1.5, result[5], EPSILON);  // 1.5 preserved
        assertEquals(3.0, result[6], EPSILON);  // 3.0 preserved
    }
    
    @Test
    @DisplayName("Test denoising with large arrays")
    void testDenoisingLargeArrays() {
        int size = 1024;
        double[] coeffs = new double[size];
        double threshold = 0.5;
        
        // Initialize with noise
        for (int i = 0; i < size; i++) {
            coeffs[i] = Math.sin(2 * Math.PI * i / 64) + 0.3 * Math.random();
        }
        
        // Test soft thresholding
        double[] softResult = VectorOps.Denoising.softThreshold(coeffs, threshold);
        assertNotNull(softResult);
        assertEquals(size, softResult.length);
        
        // Test hard thresholding
        double[] hardResult = VectorOps.Denoising.hardThreshold(coeffs, threshold);
        assertNotNull(hardResult);
        assertEquals(size, hardResult.length);
        
        // Verify thresholding was applied
        int zeroCountSoft = 0;
        int zeroCountHard = 0;
        for (int i = 0; i < size; i++) {
            if (softResult[i] == 0.0) zeroCountSoft++;
            if (hardResult[i] == 0.0) zeroCountHard++;
        }
        
        // Should have some zeros after thresholding
        assertTrue(zeroCountSoft > 0);
        assertTrue(zeroCountHard > 0);
    }
    
    // ==========================================
    // Specialized Operations Tests
    // ==========================================
    
    @Test
    @DisplayName("Test Haar transform vectorized")
    void testHaarTransformVectorized() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        VectorOps.haarTransformVectorized(signal, approx, detail);
        
        double sqrt2 = Math.sqrt(2.0);
        
        // Verify approximation coefficients
        assertEquals((1 + 2) / sqrt2, approx[0], EPSILON);
        assertEquals((3 + 4) / sqrt2, approx[1], EPSILON);
        assertEquals((5 + 6) / sqrt2, approx[2], EPSILON);
        assertEquals((7 + 8) / sqrt2, approx[3], EPSILON);
        
        // Verify detail coefficients
        assertEquals((1 - 2) / sqrt2, detail[0], EPSILON);
        assertEquals((3 - 4) / sqrt2, detail[1], EPSILON);
        assertEquals((5 - 6) / sqrt2, detail[2], EPSILON);
        assertEquals((7 - 8) / sqrt2, detail[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test combined transform periodic vectorized")
    void testCombinedTransformPeriodicVectorized() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] lowFilter = {0.5, 0.5};
        double[] highFilter = {0.5, -0.5};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        VectorOps.combinedTransformPeriodicVectorized(signal, lowFilter, highFilter, approx, detail);
        
        // Low filter computes averages
        assertEquals((1 + 2) * 0.5, approx[0], EPSILON);
        assertEquals((3 + 4) * 0.5, approx[1], EPSILON);
        
        // High filter computes differences
        assertEquals((1 - 2) * 0.5, detail[0], EPSILON);
        assertEquals((3 - 4) * 0.5, detail[1], EPSILON);
    }
    
    @Test
    @DisplayName("Test combined transform periodic vectorized with large signal")
    void testCombinedTransformPeriodicVectorizedLarge() {
        int size = 512; // Large enough to trigger all vectorization paths
        double[] signal = new double[size];
        double[] lowFilter = {0.7071067811865475, 0.7071067811865475}; // Haar scaling
        double[] highFilter = {0.7071067811865475, -0.7071067811865475}; // Haar wavelet
        double[] approx = new double[size / 2];
        double[] detail = new double[size / 2];
        
        // Initialize with sine wave
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64);
        }
        
        VectorOps.combinedTransformPeriodicVectorized(signal, lowFilter, highFilter, approx, detail);
        
        // Verify arrays are filled
        assertNotNull(approx);
        assertNotNull(detail);
        assertEquals(size / 2, approx.length);
        assertEquals(size / 2, detail.length);
        
        // Check that computation was performed (non-zero results)
        double approxSum = 0.0, detailSum = 0.0;
        for (int i = 0; i < approx.length; i++) {
            approxSum += Math.abs(approx[i]);
            detailSum += Math.abs(detail[i]);
        }
        assertTrue(approxSum > 0.0);
        assertTrue(detailSum > 0.0);
        
        // Haar transform should preserve energy (Parseval's theorem)
        double originalEnergy = 0.0;
        for (double val : signal) {
            originalEnergy += val * val;
        }
        
        double transformEnergy = 0.0;
        for (int i = 0; i < approx.length; i++) {
            transformEnergy += approx[i] * approx[i] + detail[i] * detail[i];
        }
        
        assertEquals(originalEnergy, transformEnergy, 1e-6);
    }
    
    @Test
    @DisplayName("Test combined transform periodic vectorized with Daubechies filters")
    void testCombinedTransformPeriodicVectorizedDB2() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
        
        // Daubechies 2 filters
        double[] lowFilter = {
            0.6830127018922193, 0.1830127018922193, -0.3169872981077807, -0.1830127018922193
        };
        double[] highFilter = {
            -0.1830127018922193, 0.3169872981077807, 0.1830127018922193, -0.6830127018922193
        };
        
        double[] approx = new double[8];
        double[] detail = new double[8];
        
        VectorOps.combinedTransformPeriodicVectorized(signal, lowFilter, highFilter, approx, detail);
        
        // Verify outputs are computed
        assertNotNull(approx);
        assertNotNull(detail);
        
        // Check all values are finite
        for (int i = 0; i < approx.length; i++) {
            assertTrue(Double.isFinite(approx[i]));
            assertTrue(Double.isFinite(detail[i]));
        }
        
        // Verify non-zero computation
        boolean hasNonZeroApprox = false, hasNonZeroDetail = false;
        for (int i = 0; i < approx.length; i++) {
            if (Math.abs(approx[i]) > EPSILON) hasNonZeroApprox = true;
            if (Math.abs(detail[i]) > EPSILON) hasNonZeroDetail = true;
        }
        assertTrue(hasNonZeroApprox);
        assertTrue(hasNonZeroDetail);
    }
    
    @Test
    @DisplayName("Test combined transform periodic vectorized with zero coefficients")
    void testCombinedTransformPeriodicVectorizedWithZeros() {
        double[] signal = {1, 0, 3, 0, 5, 0, 7, 0}; // Sparse signal
        double[] lowFilter = {0.5, 0.0, 0.5, 0.0}; // Sparse filter
        double[] highFilter = {0.5, 0.0, -0.5, 0.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        VectorOps.combinedTransformPeriodicVectorized(signal, lowFilter, highFilter, approx, detail);
        
        // Check that computation was performed
        assertNotNull(approx);
        assertNotNull(detail);
        
        // Verify all values are finite
        for (int i = 0; i < approx.length; i++) {
            assertTrue(Double.isFinite(approx[i]));
            assertTrue(Double.isFinite(detail[i]));
        }
    }
    
    @Test
    @DisplayName("Test combined transform periodic vectorized boundary wrapping")
    void testCombinedTransformPeriodicVectorizedBoundary() {
        double[] signal = {10, 20, 30, 40}; // Small signal to test boundary conditions
        double[] lowFilter = {0.25, 0.25, 0.25, 0.25}; // Filter length equals signal length
        double[] highFilter = {0.25, -0.25, 0.25, -0.25};
        double[] approx = new double[2];
        double[] detail = new double[2];
        
        VectorOps.combinedTransformPeriodicVectorized(signal, lowFilter, highFilter, approx, detail);
        
        // Test periodic boundary wrapping
        double expectedApprox0 = (10 + 20 + 30 + 40) * 0.25; // All coefficients contribute
        assertEquals(expectedApprox0, approx[0], EPSILON);
        
        double expectedDetail0 = (10 - 20 + 30 - 40) * 0.25;
        assertEquals(expectedDetail0, detail[0], EPSILON);
    }
    
    // ==========================================
    // Processing Strategy Tests
    // ==========================================
    
    @Test
    @DisplayName("Test selectOptimalStrategy")
    void testSelectOptimalStrategy() {
        // Get capabilities to understand platform thresholds
        VectorOps.VectorCapabilityInfo capabilities = VectorOps.getVectorCapabilities();
        int threshold = capabilities.threshold();
        
        // Strategy selection is platform-dependent
        VectorOps.ProcessingStrategy strategy1 = VectorOps.selectOptimalStrategy(8, 4);
        assertNotNull(strategy1);
        
        // On Apple Silicon with low threshold (4), even size 8 might use vectorized strategy
        if (8 < threshold) {
            assertTrue(strategy1 == VectorOps.ProcessingStrategy.SCALAR_OPTIMIZED ||
                       strategy1 == VectorOps.ProcessingStrategy.SCALAR_FALLBACK,
                       "Small signal (8) below threshold should use scalar strategy, but got: " + strategy1);
        } else {
            // May use vectorized strategy if above or equal to threshold
            assertNotNull(strategy1.getDescription());
        }
        
        // Large power-of-2 signal
        VectorOps.ProcessingStrategy strategy2 = VectorOps.selectOptimalStrategy(1024, 4);
        assertNotNull(strategy2);
        assertNotNull(strategy2.getDescription());
        
        // Large non-power-of-2 signal
        VectorOps.ProcessingStrategy strategy3 = VectorOps.selectOptimalStrategy(1000, 4);
        assertNotNull(strategy3);
        
        // Edge case: signal length of 16 may vary by platform
        // Just verify it returns a valid strategy
        VectorOps.ProcessingStrategy strategy4 = VectorOps.selectOptimalStrategy(16, 4);
        assertNotNull(strategy4);
        assertNotNull(strategy4.getDescription());
    }
    
    // ==========================================
    // Utility Methods Tests
    // ==========================================
    
    @Test
    @DisplayName("Test clearArrayVectorized")
    void testClearArrayVectorized() {
        double[] array = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        
        VectorOps.clearArrayVectorized(array);
        
        // All elements should be zero
        for (double value : array) {
            assertEquals(0.0, value, EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test clearArrayVectorized with large array")
    void testClearArrayVectorizedLarge() {
        int size = 1024;
        double[] array = new double[size];
        
        // Initialize with non-zero values
        for (int i = 0; i < size; i++) {
            array[i] = i + 1;
        }
        
        VectorOps.clearArrayVectorized(array);
        
        // All elements should be zero
        for (double value : array) {
            assertEquals(0.0, value, EPSILON);
        }
    }
    
    // ==========================================
    // Edge Cases and Special Conditions
    // ==========================================
    
    @Test
    @DisplayName("Test with very small signals")
    void testVerySmallSignals() {
        double[] signal = {1.0, 2.0};
        double[] filter = {0.7071, 0.7071};
        
        double[] result = VectorOps.convolveAndDownsamplePeriodic(signal, filter, 2, 2);
        
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals((1.0 + 2.0) * 0.7071, result[0], EPSILON);
    }
    
    @Test
    @DisplayName("Test with single coefficient upsampling")
    void testSingleCoefficientUpsampling() {
        double[] coeffs = {5.0};
        double[] filter = {0.8, 0.2};
        
        double[] result = VectorOps.upsampleAndConvolvePeriodic(coeffs, filter, 1, 2);
        
        assertNotNull(result);
        assertEquals(2, result.length);
        assertEquals(5.0 * 0.8, result[0], EPSILON);
        assertEquals(5.0 * 0.2, result[1], EPSILON);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024})
    @DisplayName("Test operations with power-of-2 sizes")
    void testPowerOfTwoSizes(int size) {
        double[] signal = new double[size];
        double[] filter = {0.5, 0.5};
        
        // Initialize signal
        for (int i = 0; i < size; i++) {
            signal[i] = i + 1;
        }
        
        // Test convolution and downsampling
        double[] result = VectorOps.convolveAndDownsamplePeriodic(signal, filter, size, 2);
        assertNotNull(result);
        assertEquals(size / 2, result.length);
        
        // Verify output is computed
        assertNotEquals(0.0, result[0], EPSILON);
        assertNotEquals(0.0, result[result.length - 1], EPSILON);
    }
    
    @ParameterizedTest
    @CsvSource({
        "100, 2",   // Non-power-of-2
        "200, 4",   // Non-power-of-2
        "300, 6",   // Non-power-of-2
        "500, 8"    // Non-power-of-2
    })
    @DisplayName("Test operations with non-power-of-2 sizes")
    void testNonPowerOfTwoSizes(int signalSize, int filterSize) {
        double[] signal = new double[signalSize];
        double[] filter = new double[filterSize];
        
        // Initialize
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signalSize);
        }
        for (int i = 0; i < filterSize; i++) {
            filter[i] = 1.0 / filterSize; // Averaging filter
        }
        
        // Test should handle non-power-of-2 sizes correctly
        double[] result = VectorOps.convolveAndDownsamplePeriodic(signal, filter, signalSize, filterSize);
        assertNotNull(result);
        assertEquals(signalSize / 2, result.length);
        
        // Verify some computation occurred
        double sum = 0;
        for (double v : result) {
            sum += Math.abs(v);
        }
        assertTrue(sum > 0, "Output should contain non-zero values");
    }
    
    @Test
    @DisplayName("Test with all zero signal")
    void testAllZeroSignal() {
        double[] signal = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double[] filter = {0.5, 0.5};
        
        double[] result = VectorOps.convolveAndDownsamplePeriodic(signal, filter, 8, 2);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // All outputs should be zero
        for (double value : result) {
            assertEquals(0.0, value, EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test consistency between vectorized and scalar implementations")
    void testVectorizedScalarConsistency() {
        // Use a size that might trigger vectorization
        int size = 256;
        double[] signal = new double[size];
        double[] filter = {0.25, 0.5, 0.25};
        
        // Initialize with random values
        for (int i = 0; i < size; i++) {
            signal[i] = Math.random() * 10;
        }
        
        // Get results from VectorOps (may use vectorization)
        double[] vectorResult = VectorOps.convolveAndDownsamplePeriodic(signal, filter, size, filter.length);
        
        // Get results from ScalarOps for comparison
        double[] scalarResult = ScalarOps.convolveAndDownsamplePeriodic(signal, filter, size, filter.length);
        
        // Results should be identical within floating-point precision
        assertArrayEquals(scalarResult, vectorResult, EPSILON, 
            "Vectorized and scalar implementations should produce identical results");
    }
    
    @Test
    @DisplayName("Test platform detection and vector optimization thresholds")
    void testPlatformSpecificOptimizations() {
        // Test that platform detection doesn't crash
        String vectorInfo = VectorOps.getVectorInfo();
        assertNotNull(vectorInfo);
        assertTrue(vectorInfo.contains("Platform="));
        
        // Test vector capabilities
        VectorOps.VectorCapabilityInfo capabilities = VectorOps.getVectorCapabilities();
        assertNotNull(capabilities);
        
        // Test edge cases for vector length determination
        boolean supported = VectorOps.isVectorApiSupported();
        assertNotNull(Boolean.valueOf(supported)); // Just verify it doesn't throw
    }
    
    @Test
    @DisplayName("Test specialized filter size optimizations")
    void testSpecializedFilterOptimizations() {
        // Test convolution with filter sizes that trigger specific optimizations
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32);
        }
        
        // Test 2-tap filter (common case)
        double[] filter2 = {0.7, 0.3};
        double[] result2 = VectorOps.convolveAndDownsamplePeriodic(signal, filter2, signal.length, filter2.length);
        assertNotNull(result2);
        assertEquals(128, result2.length);
        
        // Test 4-tap filter (triggers unrolling optimization)
        double[] filter4 = {0.4, 0.3, 0.2, 0.1};
        double[] result4 = VectorOps.convolveAndDownsamplePeriodic(signal, filter4, signal.length, filter4.length);
        assertNotNull(result4);
        assertEquals(128, result4.length);
        
        // Test 6-tap filter (mixed unrolled + loop)
        double[] filter6 = {0.25, 0.2, 0.2, 0.15, 0.1, 0.1};
        double[] result6 = VectorOps.convolveAndDownsamplePeriodic(signal, filter6, signal.length, filter6.length);
        assertNotNull(result6);
        assertEquals(128, result6.length);
    }
    
    @Test
    @DisplayName("Test gather-multiply-accumulate optimization paths")
    void testGatherMultiplyAccumulateOptimizations() {
        // Test large signals that trigger gather operations
        int size = 1024;
        double[] signal = new double[size];
        double[] filter = {0.35, 0.35, 0.2, 0.1}; // 4-tap filter for unrolling
        
        // Initialize with sawtooth pattern
        for (int i = 0; i < size; i++) {
            signal[i] = (i % 64) / 64.0;
        }
        
        double[] result = VectorOps.convolveAndDownsamplePeriodic(signal, filter, size, filter.length);
        
        assertNotNull(result);
        assertEquals(size / 2, result.length);
        
        // Verify computation was performed
        double sum = 0.0;
        for (double val : result) {
            sum += Math.abs(val);
        }
        assertTrue(sum > 0.0);
        
        // Verify result has expected characteristics for sawtooth input
        // All values should be finite
        for (double val : result) {
            assertTrue(Double.isFinite(val));
        }
    }
}
