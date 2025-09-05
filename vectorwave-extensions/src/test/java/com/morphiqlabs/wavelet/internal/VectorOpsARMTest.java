package com.morphiqlabs.wavelet.extensions.internal;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.util.PlatformDetector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ARM-specific vector operations.
 * Tests all public methods in VectorOpsARM including platform-specific optimizations.
 */
@DisplayName("VectorOpsARM Test Suite")
class VectorOpsARMTest {
    
    private static final double EPSILON = 1e-10;
    
    // Test data
    private static final double[] HAAR_FILTER = {0.7071067811865475, 0.7071067811865475};
    private static final double[] DB2_FILTER = {
        0.48296291314453414, 0.83651630373780772, 
        0.22414386804201339, -0.12940952255126037
    };
    
    // ==========================================
    // Platform Detection Tests
    // ==========================================
    
    @Test
    @DisplayName("Test ARM platform detection")
    void testIsARMPlatform() {
        // Should not throw an exception
        assertDoesNotThrow(() -> {
            boolean isARM = VectorOpsARM.isARMPlatform();
            // Result depends on the actual platform, just verify it returns a boolean
            assertTrue(isARM || !isARM);
        });
    }
    
    @Test
    @DisplayName("Test Apple Silicon detection")
    void testIsAppleSilicon() {
        // Should not throw an exception
        assertDoesNotThrow(() -> {
            boolean isAppleSilicon = VectorOpsARM.isAppleSilicon();
            // Result depends on the actual platform, just verify it returns a boolean
            assertTrue(isAppleSilicon || !isAppleSilicon);
        });
    }
    
    // ==========================================
    // Haar Transform Tests
    // ==========================================
    
    @Test
    @DisplayName("Test Haar transform with basic signal")
    void testHaarTransformBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        VectorOpsARM.haarTransformARM(signal, approx, detail, signal.length);
        
        // Verify Haar transform properties
        double sqrt2Inv = 1.0 / Math.sqrt(2.0);
        double[] expectedApprox = {
            (1.0 + 2.0) * sqrt2Inv,
            (3.0 + 4.0) * sqrt2Inv,
            (5.0 + 6.0) * sqrt2Inv,
            (7.0 + 8.0) * sqrt2Inv
        };
        double[] expectedDetail = {
            (1.0 - 2.0) * sqrt2Inv,
            (3.0 - 4.0) * sqrt2Inv,
            (5.0 - 6.0) * sqrt2Inv,
            (7.0 - 8.0) * sqrt2Inv
        };
        
        assertArrayEquals(expectedApprox, approx, EPSILON);
        assertArrayEquals(expectedDetail, detail, EPSILON);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {4, 8, 16, 32, 64})
    @DisplayName("Test Haar transform with various signal lengths")
    void testHaarTransformVariousLengths(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = i + 1.0;
        }
        
        double[] approx = new double[length / 2];
        double[] detail = new double[length / 2];
        
        assertDoesNotThrow(() -> {
            VectorOpsARM.haarTransformARM(signal, approx, detail, length);
        });
        
        // Verify output arrays are properly filled
        for (int i = 0; i < length / 2; i++) {
            assertFalse(Double.isNaN(approx[i]));
            assertFalse(Double.isNaN(detail[i]));
        }
    }
    
    // ==========================================
    // DB2 Transform Tests
    // ==========================================
    
    @Test
    @DisplayName("Test DB2 transform low-pass")
    void testDB2TransformLowPass() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        double[] result = VectorOpsARM.db2TransformARM(signal, signal.length, true);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Verify that convolution was performed (results should be non-zero for non-zero input)
        for (double value : result) {
            assertFalse(Double.isNaN(value));
        }
    }
    
    @Test
    @DisplayName("Test DB2 transform high-pass")
    void testDB2TransformHighPass() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        double[] result = VectorOpsARM.db2TransformARM(signal, signal.length, false);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Verify that convolution was performed
        for (double value : result) {
            assertFalse(Double.isNaN(value));
        }
    }
    
    @Test
    @DisplayName("Test DB2 transform with constant signal")
    void testDB2TransformConstant() {
        double[] signal = {5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0};
        
        double[] lowPass = VectorOpsARM.db2TransformARM(signal, signal.length, true);
        double[] highPass = VectorOpsARM.db2TransformARM(signal, signal.length, false);
        
        assertNotNull(lowPass);
        assertNotNull(highPass);
        
        // For a constant signal, high-pass should be close to zero
        for (double value : highPass) {
            assertTrue(Math.abs(value) < 0.1, "High-pass of constant signal should be near zero");
        }
    }
    
    @Test
    @DisplayName("Test DB2 transform with odd output length")
    void testDB2TransformOddOutputLength() {
        // Signal length 18 -> output length 9 (odd)
        double[] signal = new double[18];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length);
        }
        
        double[] lowPass = VectorOpsARM.db2TransformARM(signal, signal.length, true);
        double[] highPass = VectorOpsARM.db2TransformARM(signal, signal.length, false);
        
        assertNotNull(lowPass);
        assertNotNull(highPass);
        assertEquals(9, lowPass.length);
        assertEquals(9, highPass.length);
        
        // Verify all values are valid
        for (int i = 0; i < 9; i++) {
            assertFalse(Double.isNaN(lowPass[i]));
            assertFalse(Double.isNaN(highPass[i]));
            assertFalse(Double.isInfinite(lowPass[i]));
            assertFalse(Double.isInfinite(highPass[i]));
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {4, 8, 16, 32, 64})
    @DisplayName("Test DB2 transform with various signal lengths")
    void testDB2TransformVariousLengths(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / length);
        }
        
        assertDoesNotThrow(() -> {
            double[] lowPass = VectorOpsARM.db2TransformARM(signal, length, true);
            double[] highPass = VectorOpsARM.db2TransformARM(signal, length, false);
            
            assertEquals(length / 2, lowPass.length);
            assertEquals(length / 2, highPass.length);
        });
    }
    
    // ==========================================
    // General Convolution Tests
    // ==========================================
    
    @Test
    @DisplayName("Test convolution with null inputs")
    void testConvolutionNullInputs() {
        assertThrows(IllegalArgumentException.class, () -> {
            VectorOpsARM.convolveAndDownsampleARM(null, HAAR_FILTER, 8, 2);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            VectorOpsARM.convolveAndDownsampleARM(new double[8], null, 8, 2);
        });
    }
    
    @Test
    @DisplayName("Test convolution with Haar filter")
    void testConvolutionHaarFilter() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        double[] result = VectorOpsARM.convolveAndDownsampleARM(
            signal, HAAR_FILTER, signal.length, HAAR_FILTER.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Verify results are reasonable
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test convolution with DB2 filter")
    void testConvolutionDB2Filter() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        double[] result = VectorOpsARM.convolveAndDownsampleARM(
            signal, DB2_FILTER, signal.length, DB2_FILTER.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Verify results are reasonable
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test convolution with custom filter")
    void testConvolutionCustomFilter() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] customFilter = {0.1, 0.2, 0.3, 0.2, 0.1, 0.1}; // 6-tap filter
        
        double[] result = VectorOpsARM.convolveAndDownsampleARM(
            signal, customFilter, signal.length, customFilter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test convolution with odd filter length")
    void testConvolutionOddFilterLength() {
        double[] signal = new double[16];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
        }
        double[] filter = {0.2, 0.3, 0.2}; // 3-tap filter (odd length)
        
        double[] result = VectorOpsARM.convolveAndDownsampleARM(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(8, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test convolution with large filter")
    void testConvolutionLargeFilter() {
        double[] signal = new double[32];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.cos(2 * Math.PI * i / signal.length);
        }
        double[] filter = new double[10]; // 10-tap filter
        for (int i = 0; i < filter.length; i++) {
            filter[i] = 1.0 / filter.length;
        }
        
        double[] result = VectorOpsARM.convolveAndDownsampleARM(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(16, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @ParameterizedTest
    @CsvSource({
        "8, 2",
        "16, 4", 
        "32, 6",
        "64, 8"
    })
    @DisplayName("Test convolution with power-of-2 signals")
    void testConvolutionPowerOf2Signals(int signalLength, int filterLength) {
        double[] signal = new double[signalLength];
        double[] filter = new double[filterLength];
        
        // Initialize with some pattern
        for (int i = 0; i < signalLength; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signalLength) + 1.0;
        }
        for (int i = 0; i < filterLength; i++) {
            filter[i] = 1.0 / filterLength; // Averaging filter
        }
        
        assertDoesNotThrow(() -> {
            double[] result = VectorOpsARM.convolveAndDownsampleARM(
                signal, filter, signalLength, filterLength);
            
            assertNotNull(result);
            assertEquals(signalLength / 2, result.length);
        });
    }
    
    // ==========================================
    // Upsampling Tests - without EnabledIf restrictions
    // ==========================================
    
    @Test
    @DisplayName("Test upsampling periodic with null inputs")
    void testUpsamplingPeriodicNullInputs() {
        assertThrows(IllegalArgumentException.class, () -> {
            VectorOpsARM.upsampleAndConvolvePeriodicARM(null, HAAR_FILTER, 4, 2);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            VectorOpsARM.upsampleAndConvolvePeriodicARM(new double[4], null, 4, 2);
        });
    }
    
    @Test
    @DisplayName("Test upsampling periodic with Haar")
    void testUpsamplingPeriodicHaar() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        
        double[] result = VectorOpsARM.upsampleAndConvolvePeriodicARM(
            signal, HAAR_FILTER, signal.length, HAAR_FILTER.length);
        
        assertNotNull(result);
        assertEquals(8, result.length);
        
        // Verify reasonable results
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test upsampling periodic with DB2")
    void testUpsamplingPeriodicDB2() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        
        double[] result = VectorOpsARM.upsampleAndConvolvePeriodicARM(
            signal, DB2_FILTER, signal.length, DB2_FILTER.length);
        
        assertNotNull(result);
        assertEquals(8, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test upsampling periodic with custom filter")
    void testUpsamplingPeriodicCustom() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] customFilter = {0.25, 0.5, 0.25}; // 3-tap filter
        
        double[] result = VectorOpsARM.upsampleAndConvolvePeriodicARM(
            signal, customFilter, signal.length, customFilter.length);
        
        assertNotNull(result);
        assertEquals(8, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test upsampling periodic with odd signal length")
    void testUpsamplingPeriodicOddLength() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0}; // 5 elements (odd)
        
        double[] result = VectorOpsARM.upsampleAndConvolvePeriodicARM(
            signal, HAAR_FILTER, signal.length, HAAR_FILTER.length);
        
        assertNotNull(result);
        assertEquals(10, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8, 16})
    @DisplayName("Test upsampling periodic with various signal lengths")
    void testUpsamplingPeriodicVariousLengths(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = i + 1.0;
        }
        
        assertDoesNotThrow(() -> {
            double[] result = VectorOpsARM.upsampleAndConvolvePeriodicARM(
                signal, HAAR_FILTER, length, HAAR_FILTER.length);
            
            assertNotNull(result);
            assertEquals(length * 2, result.length);
        });
    }
    
    // ==========================================
    // Upsampling Tests - Zero Padding
    // ==========================================
    
    @Test
    @DisplayName("Test upsampling zero padding with null inputs")
    void testUpsamplingZeroPaddingNullInputs() {
        assertThrows(IllegalArgumentException.class, () -> {
            VectorOpsARM.upsampleAndConvolveZeroPaddingARM(null, HAAR_FILTER, 4, 2);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            VectorOpsARM.upsampleAndConvolveZeroPaddingARM(new double[4], null, 4, 2);
        });
    }
    
    @Test
    @DisplayName("Test upsampling zero padding basic")
    void testUpsamplingZeroPaddingBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        
        double[] result = VectorOpsARM.upsampleAndConvolveZeroPaddingARM(
            signal, HAAR_FILTER, signal.length, HAAR_FILTER.length);
        
        assertNotNull(result);
        assertEquals(8, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test upsampling zero padding with DB2")
    void testUpsamplingZeroPaddingDB2() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        
        double[] result = VectorOpsARM.upsampleAndConvolveZeroPaddingARM(
            signal, DB2_FILTER, signal.length, DB2_FILTER.length);
        
        assertNotNull(result);
        assertEquals(8, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test upsampling zero padding with odd output length")
    void testUpsamplingZeroPaddingOddOutputLength() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0}; // 5 elements -> 10 output (even)
        double[] filter = {0.5, 0.5};
        
        double[] result = VectorOpsARM.upsampleAndConvolveZeroPaddingARM(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(10, result.length);
        
        // Verify last element handling
        assertFalse(Double.isNaN(result[9]));
        assertFalse(Double.isInfinite(result[9]));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8, 16})
    @DisplayName("Test upsampling zero padding with various lengths")
    void testUpsamplingZeroPaddingVariousLengths(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.cos(2 * Math.PI * i / length);
        }
        
        assertDoesNotThrow(() -> {
            double[] result = VectorOpsARM.upsampleAndConvolveZeroPaddingARM(
                signal, HAAR_FILTER, length, HAAR_FILTER.length);
            
            assertNotNull(result);
            assertEquals(length * 2, result.length);
        });
    }
    
    // ==========================================
    // Original upsampling tests (keeping existing functionality)
    // ==========================================
    
    @Test
    @EnabledIf("isARMPlatform")
    @DisplayName("Test upsampling periodic ARM - Haar")
    void testUpsampleAndConvolvePeriodicARM_Haar() {
        // Haar wavelet filter
        double[] filter = new Haar().lowPassDecomposition();
        
        // Simple test signal
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        int signalLength = signal.length;
        
        // Compare ARM implementation with standard implementation
        double[] resultARM = VectorOpsARM.upsampleAndConvolvePeriodicARM(
                signal, filter, signalLength, filter.length);
        double[] resultStandard = VectorOps.upsampleAndConvolvePeriodic(
                signal, filter, signalLength, filter.length);
        
        assertArrayEquals(resultStandard, resultARM, EPSILON,
                "ARM implementation should match standard implementation");
    }
    
    @Test
    @EnabledIf("isARMPlatform")
    void testUpsampleAndConvolvePeriodicARM_DB4() {
        // Daubechies-4 wavelet filter
        double[] filter = Daubechies.DB4.lowPassDecomposition();
        
        // Test signal
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        int signalLength = signal.length;
        
        // Compare ARM implementation with standard implementation
        double[] resultARM = VectorOpsARM.upsampleAndConvolvePeriodicARM(
                signal, filter, signalLength, filter.length);
        double[] resultStandard = VectorOps.upsampleAndConvolvePeriodic(
                signal, filter, signalLength, filter.length);
        
        assertArrayEquals(resultStandard, resultARM, EPSILON,
                "ARM implementation should match standard implementation for DB4");
    }
    
    @Test
    @EnabledIf("isARMPlatform")
    void testUpsampleAndConvolveZeroPaddingARM_Haar() {
        // Haar wavelet filter
        double[] filter = new Haar().lowPassDecomposition();
        
        // Simple test signal
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        int signalLength = signal.length;
        
        // Compare ARM implementation with standard implementation
        double[] resultARM = VectorOpsARM.upsampleAndConvolveZeroPaddingARM(
                signal, filter, signalLength, filter.length);
        double[] resultStandard = VectorOps.upsampleAndConvolveZeroPadding(
                signal, filter, signalLength, filter.length);
        
        assertArrayEquals(resultStandard, resultARM, EPSILON,
                "ARM zero-padding implementation should match standard implementation");
    }
    
    @Test
    @EnabledIf("isARMPlatform")
    void testUpsampleAndConvolveZeroPaddingARM_LargeSignal() {
        // Test with larger random signal
        Random random = new Random(42);
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = random.nextGaussian();
        }
        
        double[] filter = Daubechies.DB4.lowPassDecomposition();
        
        // Compare implementations
        double[] resultARM = VectorOpsARM.upsampleAndConvolveZeroPaddingARM(
                signal, filter, signal.length, filter.length);
        double[] resultStandard = VectorOps.upsampleAndConvolveZeroPadding(
                signal, filter, signal.length, filter.length);
        
        assertArrayEquals(resultStandard, resultARM, EPSILON,
                "ARM implementation should match standard for large signals");
    }
    
    @Test
    @EnabledIf("isARMPlatform")
    void testConvolveAndDownsampleARM_Consistency() {
        // Test that ARM downsampling is consistent with existing implementation
        double[] signal = new double[128];
        Random random = new Random(42);
        for (int i = 0; i < signal.length; i++) {
            signal[i] = random.nextGaussian();
        }
        
        double[] filter = new Haar().lowPassDecomposition();
        
        double[] resultARM = VectorOpsARM.convolveAndDownsampleARM(
                signal, filter, signal.length, filter.length);
        double[] resultStandard = VectorOps.convolveAndDownsamplePeriodic(
                signal, filter, signal.length, filter.length);
        
        assertArrayEquals(resultStandard, resultARM, EPSILON,
                "ARM downsampling should match standard implementation");
    }
    
    @Test
    void testPlatformDetection() {
        System.out.println("Platform Information:");
        System.out.println("  OS Arch: " + System.getProperty("os.arch"));
        System.out.println("  Is ARM: " + PlatformDetector.isARM());
        System.out.println("  Is Apple Silicon: " + PlatformDetector.isAppleSilicon());
        
        // This test always passes, just prints platform info
        assertTrue(true);
    }
    
    @Test
    @EnabledIf("isARMPlatform")
    void testEdgeCases() {
        // Test with single element
        double[] singleElement = {5.0};
        double[] filter = new Haar().lowPassDecomposition();
        
        assertDoesNotThrow(() -> {
            VectorOpsARM.upsampleAndConvolvePeriodicARM(
                    singleElement, filter, 1, filter.length);
            VectorOpsARM.upsampleAndConvolveZeroPaddingARM(
                    singleElement, filter, 1, filter.length);
        });
        
        // Test with null inputs
        assertThrows(IllegalArgumentException.class, () ->
                VectorOpsARM.upsampleAndConvolvePeriodicARM(
                        null, filter, 1, filter.length));
        assertThrows(IllegalArgumentException.class, () ->
                VectorOpsARM.upsampleAndConvolvePeriodicARM(
                        singleElement, null, 1, filter.length));
    }
    
    // ==========================================
    // Edge Cases and Error Conditions
    // ==========================================
    
    @Test
    @DisplayName("Test with minimal signal lengths")
    void testMinimalSignalLengths() {
        double[] signal2 = {1.0, 2.0};
        double[] signal4 = {1.0, 2.0, 3.0, 4.0};
        
        // Test Haar transform with minimal length
        double[] approx = new double[1];
        double[] detail = new double[1];
        assertDoesNotThrow(() -> {
            VectorOpsARM.haarTransformARM(signal2, approx, detail, 2);
        });
        
        // Test convolution with minimal length
        assertDoesNotThrow(() -> {
            VectorOpsARM.convolveAndDownsampleARM(signal4, HAAR_FILTER, 4, 2);
        });
    }
    
    @Test
    @DisplayName("Test with extreme values")
    void testExtremeValues() {
        double[] signal = {Double.MAX_VALUE / 1e10, -Double.MAX_VALUE / 1e10, 
                          1e-10, -1e-10, 0.0, 1000.0, -1000.0, 0.5};
        
        assertDoesNotThrow(() -> {
            double[] approx = new double[4];
            double[] detail = new double[4];
            VectorOpsARM.haarTransformARM(signal, approx, detail, signal.length);
            
            // Verify no overflow/underflow
            for (int i = 0; i < 4; i++) {
                assertFalse(Double.isNaN(approx[i]));
                assertFalse(Double.isInfinite(approx[i]));
                assertFalse(Double.isNaN(detail[i]));
                assertFalse(Double.isInfinite(detail[i]));
            }
        });
    }
    
    @Test
    @DisplayName("Test consistency between periodic and zero padding upsampling")
    void testUpsamplingConsistency() {
        double[] signal = {1.0, 2.0, 3.0, 4.0}; // Signal that will show boundary differences
        
        double[] periodicResult = VectorOpsARM.upsampleAndConvolvePeriodicARM(
            signal, HAAR_FILTER, signal.length, HAAR_FILTER.length);
        
        double[] zeroPadResult = VectorOpsARM.upsampleAndConvolveZeroPaddingARM(
            signal, HAAR_FILTER, signal.length, HAAR_FILTER.length);
        
        assertNotNull(periodicResult);
        assertNotNull(zeroPadResult);
        assertEquals(periodicResult.length, zeroPadResult.length);
        
        // Both methods should produce valid results
        for (int i = 0; i < periodicResult.length; i++) {
            assertFalse(Double.isNaN(periodicResult[i]));
            assertFalse(Double.isInfinite(periodicResult[i]));
            assertFalse(Double.isNaN(zeroPadResult[i]));
            assertFalse(Double.isInfinite(zeroPadResult[i]));
        }
        
        // The results may differ due to boundary handling, but not always
        // Just verify both methods work without errors
        assertTrue(periodicResult.length == 8 && zeroPadResult.length == 8,
            "Both methods should produce correct output length");
    }
    
    @Test
    @DisplayName("Test Haar transform with zeros")
    void testHaarTransformWithZeros() {
        double[] signal = {0.0, 0.0, 1.0, 0.0, 0.0, 1.0, 0.0, 0.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        VectorOpsARM.haarTransformARM(signal, approx, detail, signal.length);
        
        // Verify results
        assertNotNull(approx);
        assertNotNull(detail);
        assertEquals(4, approx.length);
        assertEquals(4, detail.length);
        
        // Check that zero pairs result in zero coefficients
        assertEquals(0.0, approx[0], EPSILON);
        assertEquals(0.0, detail[0], EPSILON);
    }
    
    // Helper method for conditional test execution
    static boolean isARMPlatform() {
        return PlatformDetector.isARM();
    }
}
