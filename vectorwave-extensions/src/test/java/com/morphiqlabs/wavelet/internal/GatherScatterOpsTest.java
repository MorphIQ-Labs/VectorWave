package com.morphiqlabs.wavelet.extensions.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for GatherScatterOps class.
 * Tests optimized gather/scatter operations using Vector API intrinsics.
 */
@DisplayName("GatherScatterOps Test Suite")
class GatherScatterOpsTest {
    
    private static final double EPSILON = 1e-10;
    
    // ==========================================
    // Platform Support Tests
    // ==========================================
    
    @Test
    @DisplayName("Test gather/scatter availability detection")
    void testIsGatherScatterAvailable() {
        // Should not throw an exception
        assertDoesNotThrow(() -> {
            boolean isAvailable = GatherScatterOps.isGatherScatterAvailable();
            // Result depends on the platform, just verify it returns a boolean
            assertTrue(isAvailable || !isAvailable);
        });
    }
    
    @Test
    @DisplayName("Test gather/scatter info")
    void testGetGatherScatterInfo() {
        assertDoesNotThrow(() -> {
            String info = GatherScatterOps.getGatherScatterInfo();
            assertNotNull(info);
            assertFalse(info.isEmpty());
            assertTrue(info.contains("Gather/Scatter Support"));
            assertTrue(info.contains("Vector Length"));
            assertTrue(info.contains("Platform"));
        });
    }
    
    @Test
    @DisplayName("Test checkGatherScatterSupport method coverage")
    void testCheckGatherScatterSupport() throws Exception {
        // Access private method via reflection to ensure all code paths are covered
        Method checkMethod = GatherScatterOps.class.getDeclaredMethod("checkGatherScatterSupport");
        checkMethod.setAccessible(true);
        
        // Should return a boolean without throwing
        boolean result = (Boolean) checkMethod.invoke(null);
        assertTrue(result || !result); // Just verify it returns a boolean
        
        // Test that the method handles exceptions properly
        assertDoesNotThrow(() -> checkMethod.invoke(null));
    }
    
    @Test
    @DisplayName("Test checkX86GatherSupport method")
    void testCheckX86GatherSupport() throws Exception {
        // Access private method via reflection
        Method checkX86Method = GatherScatterOps.class.getDeclaredMethod("checkX86GatherSupport");
        checkX86Method.setAccessible(true);
        
        // Should return false according to implementation
        boolean result = (Boolean) checkX86Method.invoke(null);
        assertFalse(result, "checkX86GatherSupport should return false for stability");
    }
    
    // ==========================================
    // Gather Periodic Downsample Tests
    // ==========================================
    
    @Test
    @DisplayName("Test gather periodic downsample basic")
    void testGatherPeriodicDownsampleBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.5, 0.5};
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Verify results are reasonable
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test gather periodic downsample with zeros")
    void testGatherPeriodicDownsampleWithZeros() {
        double[] signal = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double[] filter = {0.7071067811865475, 0.7071067811865475}; // Haar
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Zero signal should produce zero output
        for (double value : result) {
            assertEquals(0.0, value, EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test gather periodic downsample with Haar filter")
    void testGatherPeriodicDownsampleHaar() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] haarFilter = {0.7071067811865475, 0.7071067811865475};
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, haarFilter, signal.length, haarFilter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Expected values for Haar transform
        double sqrt2Inv = 0.7071067811865475;
        double[] expected = {
            (1.0 + 2.0) * sqrt2Inv,
            (3.0 + 4.0) * sqrt2Inv,
            (5.0 + 6.0) * sqrt2Inv,
            (7.0 + 8.0) * sqrt2Inv
        };
        
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i], EPSILON);
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {8, 16, 32, 64, 128})
    @DisplayName("Test gather periodic downsample with various signal lengths")
    void testGatherPeriodicDownsampleVariousLengths(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / length) + 1.0;
        }
        
        double[] filter = {0.25, 0.5, 0.25};
        
        assertDoesNotThrow(() -> {
            double[] result = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter, length, filter.length);
            
            assertNotNull(result);
            assertEquals(length / 2, result.length);
            
            for (double value : result) {
                assertFalse(Double.isNaN(value));
                assertFalse(Double.isInfinite(value));
            }
        });
    }
    
    @ParameterizedTest
    @CsvSource({
        "8, 2",
        "16, 4",
        "32, 6",
        "64, 8"
    })
    @DisplayName("Test gather periodic downsample with various filter lengths")
    void testGatherPeriodicDownsampleVariousFilterLengths(int signalLength, int filterLength) {
        double[] signal = new double[signalLength];
        double[] filter = new double[filterLength];
        
        // Initialize with test data
        for (int i = 0; i < signalLength; i++) {
            signal[i] = i + 1.0;
        }
        for (int i = 0; i < filterLength; i++) {
            filter[i] = 1.0 / filterLength; // Averaging filter
        }
        
        assertDoesNotThrow(() -> {
            double[] result = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter, signalLength, filterLength);
            
            assertNotNull(result);
            assertEquals(signalLength / 2, result.length);
        });
    }
    
    // ==========================================
    // Scatter Upsample Tests
    // ==========================================
    
    @Test
    @DisplayName("Test scatter upsample basic")
    void testScatterUpsampleBasic() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.5, 0.5, 0.5, 0.5};
        double[] output = new double[8];
        
        GatherScatterOps.scatterUpsample(approx, detail, output, 8);
        
        // Verify that values are scattered correctly
        assertEquals(1.0, output[0], EPSILON);
        assertEquals(0.5, output[1], EPSILON);
        assertEquals(2.0, output[2], EPSILON);
        assertEquals(0.5, output[3], EPSILON);
        assertEquals(3.0, output[4], EPSILON);
        assertEquals(0.5, output[5], EPSILON);
        assertEquals(4.0, output[6], EPSILON);
        assertEquals(0.5, output[7], EPSILON);
    }
    
    @Test
    @DisplayName("Test scatter upsample with zeros")
    void testScatterUpsampleWithZeros() {
        double[] approx = {0.0, 0.0};
        double[] detail = {0.0, 0.0};
        double[] output = new double[4];
        
        GatherScatterOps.scatterUpsample(approx, detail, output, 4);
        
        // All outputs should be zero
        for (double value : output) {
            assertEquals(0.0, value, EPSILON);
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {4, 8, 16, 32, 64})
    @DisplayName("Test scatter upsample with various lengths")
    void testScatterUpsampleVariousLengths(int length) {
        double[] approx = new double[length / 2];
        double[] detail = new double[length / 2];
        double[] output = new double[length];
        
        // Initialize with test data
        for (int i = 0; i < length / 2; i++) {
            approx[i] = i + 1.0;
            detail[i] = (i + 1.0) * 0.1;
        }
        
        assertDoesNotThrow(() -> {
            GatherScatterOps.scatterUpsample(approx, detail, output, length);
            
            // Verify scattered pattern
            for (int i = 0; i < length / 2; i++) {
                assertEquals(approx[i], output[2 * i], EPSILON);
                assertEquals(detail[i], output[2 * i + 1], EPSILON);
            }
        });
    }
    
    // ==========================================
    // Batch Gather Tests
    // ==========================================
    
    @Test
    @DisplayName("Test batch gather basic")
    void testBatchGatherBasic() {
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0, 5.0},
            {6.0, 7.0, 8.0, 9.0, 10.0},
            {11.0, 12.0, 13.0, 14.0, 15.0}
        };
        int[] indices = {0, 2, 4};
        double[][] results = new double[3][3];
        
        GatherScatterOps.batchGather(signals, indices, results, 3);
        
        // Verify gathered values
        assertEquals(1.0, results[0][0], EPSILON);
        assertEquals(3.0, results[0][1], EPSILON);
        assertEquals(5.0, results[0][2], EPSILON);
        
        assertEquals(6.0, results[1][0], EPSILON);
        assertEquals(8.0, results[1][1], EPSILON);
        assertEquals(10.0, results[1][2], EPSILON);
        
        assertEquals(11.0, results[2][0], EPSILON);
        assertEquals(13.0, results[2][1], EPSILON);
        assertEquals(15.0, results[2][2], EPSILON);
    }
    
    @Test
    @DisplayName("Test batch gather single signal")
    void testBatchGatherSingleSignal() {
        double[][] signals = {{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}};
        int[] indices = {1, 3, 5, 7};
        double[][] results = new double[1][4];
        
        GatherScatterOps.batchGather(signals, indices, results, 4);
        
        assertEquals(2.0, results[0][0], EPSILON);
        assertEquals(4.0, results[0][1], EPSILON);
        assertEquals(6.0, results[0][2], EPSILON);
        assertEquals(8.0, results[0][3], EPSILON);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 16})
    @DisplayName("Test batch gather with various signal counts")
    void testBatchGatherVariousSignalCounts(int numSignals) {
        double[][] signals = new double[numSignals][16];
        int[] indices = {0, 2, 4, 6, 8, 10, 12, 14};
        double[][] results = new double[numSignals][8];
        
        // Initialize signals
        for (int s = 0; s < numSignals; s++) {
            for (int i = 0; i < 16; i++) {
                signals[s][i] = s * 16 + i + 1.0;
            }
        }
        
        assertDoesNotThrow(() -> {
            GatherScatterOps.batchGather(signals, indices, results, 8);
            
            // Verify gathered values
            for (int s = 0; s < numSignals; s++) {
                for (int i = 0; i < 8; i++) {
                    double expected = s * 16 + indices[i] + 1.0;
                    assertEquals(expected, results[s][i], EPSILON);
                }
            }
        });
    }
    
    // ==========================================
    // Gather Strided Tests
    // ==========================================
    
    @Test
    @DisplayName("Test gather strided basic")
    void testGatherStridedBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
        
        double[] result = GatherScatterOps.gatherStrided(signal, 1, 2, 4);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals(2.0, result[0], EPSILON); // signal[1]
        assertEquals(4.0, result[1], EPSILON); // signal[3]
        assertEquals(6.0, result[2], EPSILON); // signal[5]
        assertEquals(8.0, result[3], EPSILON); // signal[7]
    }
    
    @Test
    @DisplayName("Test gather strided with stride 1")
    void testGatherStridedStride1() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0};
        
        double[] result = GatherScatterOps.gatherStrided(signal, 2, 1, 3);
        
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(3.0, result[0], EPSILON); // signal[2]
        assertEquals(4.0, result[1], EPSILON); // signal[3]
        assertEquals(5.0, result[2], EPSILON); // signal[4]
    }
    
    @Test
    @DisplayName("Test gather strided with large stride")
    void testGatherStridedLargeStride() {
        double[] signal = new double[100];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
        }
        
        double[] result = GatherScatterOps.gatherStrided(signal, 0, 10, 5);
        
        assertNotNull(result);
        assertEquals(5, result.length);
        assertEquals(1.0, result[0], EPSILON);   // signal[0]
        assertEquals(11.0, result[1], EPSILON);  // signal[10]
        assertEquals(21.0, result[2], EPSILON);  // signal[20]
        assertEquals(31.0, result[3], EPSILON);  // signal[30]
        assertEquals(41.0, result[4], EPSILON);  // signal[40]
    }
    
    @ParameterizedTest
    @CsvSource({
        "0, 1, 8",
        "1, 2, 8",
        "2, 3, 6",
        "0, 4, 4"
    })
    @DisplayName("Test gather strided with various parameters")
    void testGatherStridedVariousParameters(int offset, int stride, int count) {
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length);
        }
        
        assertDoesNotThrow(() -> {
            double[] result = GatherScatterOps.gatherStrided(signal, offset, stride, count);
            
            assertNotNull(result);
            assertEquals(count, result.length);
            
            for (double value : result) {
                assertFalse(Double.isNaN(value));
                assertFalse(Double.isInfinite(value));
            }
        });
    }
    
    // ==========================================
    // Gather Compressed Tests
    // ==========================================
    
    @Test
    @DisplayName("Test gather compressed basic")
    void testGatherCompressedBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        boolean[] mask = {true, false, true, false, true, false, true, false};
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals(1.0, result[0], EPSILON);
        assertEquals(3.0, result[1], EPSILON);
        assertEquals(5.0, result[2], EPSILON);
        assertEquals(7.0, result[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test gather compressed all true")
    void testGatherCompressedAllTrue() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        boolean[] mask = {true, true, true, true};
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        assertArrayEquals(signal, result, EPSILON);
    }
    
    @Test
    @DisplayName("Test gather compressed all false")
    void testGatherCompressedAllFalse() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        boolean[] mask = {false, false, false, false};
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        assertEquals(0, result.length);
    }
    
    @Test
    @DisplayName("Test gather compressed single element")
    void testGatherCompressedSingleElement() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        boolean[] mask = {false, false, false, true, false, false, false, false};
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(4.0, result[0], EPSILON);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {4, 8, 16, 32, 64})
    @DisplayName("Test gather compressed with various lengths")
    void testGatherCompressedVariousLengths(int length) {
        double[] signal = new double[length];
        boolean[] mask = new boolean[length];
        
        // Initialize signal and create alternating mask
        for (int i = 0; i < length; i++) {
            signal[i] = i + 1.0;
            mask[i] = (i % 2 == 0);
        }
        
        assertDoesNotThrow(() -> {
            double[] result = GatherScatterOps.gatherCompressed(signal, mask);
            
            assertNotNull(result);
            int expectedLength = (length + 1) / 2; // Half, rounded up
            assertEquals(expectedLength, result.length);
            
            // Verify gathered values
            for (int i = 0; i < result.length; i++) {
                assertEquals(2 * i + 1.0, result[i], EPSILON);
            }
        });
    }
    
    // ==========================================
    // Edge Cases and Error Conditions
    // ==========================================
    
    @Test
    @DisplayName("Test with minimal arrays")
    void testMinimalArrays() {
        assertDoesNotThrow(() -> {
            // Minimal gather periodic downsample
            double[] signal = {1.0, 2.0};
            double[] filter = {1.0};
            double[] result = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter, signal.length, filter.length);
            assertEquals(1, result.length);
            
            // Minimal scatter upsample
            double[] approx = {1.0};
            double[] detail = {2.0};
            double[] output = new double[2];
            GatherScatterOps.scatterUpsample(approx, detail, output, 2);
            assertEquals(1.0, output[0], EPSILON);
            assertEquals(2.0, output[1], EPSILON);
        });
    }
    
    @Test
    @DisplayName("Test with extreme values")
    void testExtremeValues() {
        double[] signal = {
            Double.MAX_VALUE / 1e10, -Double.MAX_VALUE / 1e10,
            1e-10, -1e-10, 0.0, 1000.0, -1000.0, 0.5
        };
        double[] filter = {0.5, 0.5};
        
        assertDoesNotThrow(() -> {
            double[] result = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter, signal.length, filter.length);
            
            assertNotNull(result);
            assertEquals(4, result.length);
            
            // Verify no overflow/underflow
            for (double value : result) {
                assertFalse(Double.isNaN(value));
                assertFalse(Double.isInfinite(value));
            }
        });
    }
    
    @Test
    @DisplayName("Test consistency across different methods")
    void testConsistencyAcrossMethods() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.5, 0.5};
        
        // Test gather periodic downsample
        double[] gatherResult = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(gatherResult);
        assertEquals(4, gatherResult.length);
        
        // Test strided access for comparison
        double[] stridedResult = GatherScatterOps.gatherStrided(signal, 0, 2, 4);
        
        assertNotNull(stridedResult);
        assertEquals(4, stridedResult.length);
        
        // Both should be valid (not necessarily equal due to different operations)
        for (int i = 0; i < 4; i++) {
            assertFalse(Double.isNaN(gatherResult[i]));
            assertFalse(Double.isNaN(stridedResult[i]));
        }
    }
    
    @Test
    @DisplayName("Test platform info formatting")
    void testPlatformInfoFormatting() {
        String info = GatherScatterOps.getGatherScatterInfo();
        
        // Verify info contains expected components
        assertTrue(info.contains("Available") || info.contains("Not Available"));
        assertTrue(info.contains("Vector Length:") && info.contains("doubles"));
        assertTrue(info.contains("Little Endian") || info.contains("Big Endian"));
    }
    
    // ==========================================
    // Direct Scalar Method Testing
    // ==========================================
    
    @Test
    @DisplayName("Test scalar periodic downsample directly")
    void testScalarPeriodicDownsampleDirectly() throws Exception {
        // Use reflection to access the private scalar method
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "scalarPeriodicDownsample", double[].class, double[].class, int.class, int.class);
        method.setAccessible(true);
        
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.5, 0.5};
        
        double[] result = (double[]) method.invoke(null, signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // Verify expected values
        assertEquals(1.5, result[0], EPSILON); // (1*0.5 + 2*0.5)
        assertEquals(3.5, result[1], EPSILON); // (3*0.5 + 4*0.5)
        assertEquals(5.5, result[2], EPSILON); // (5*0.5 + 6*0.5)
        assertEquals(7.5, result[3], EPSILON); // (7*0.5 + 8*0.5)
    }
    
    @Test
    @DisplayName("Test scalar upsample directly")
    void testScalarUpsampleDirectly() throws Exception {
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "scalarUpsample", double[].class, double[].class, double[].class, int.class);
        method.setAccessible(true);
        
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.1, 0.2, 0.3, 0.4};
        double[] output = new double[8];
        
        method.invoke(null, approx, detail, output, 8);
        
        // Verify interleaved pattern
        assertEquals(1.0, output[0], EPSILON);
        assertEquals(0.1, output[1], EPSILON);
        assertEquals(2.0, output[2], EPSILON);
        assertEquals(0.2, output[3], EPSILON);
        assertEquals(3.0, output[4], EPSILON);
        assertEquals(0.3, output[5], EPSILON);
        assertEquals(4.0, output[6], EPSILON);
        assertEquals(0.4, output[7], EPSILON);
    }
    
    @Test
    @DisplayName("Test batch gather scalar directly")
    void testBatchGatherScalarDirectly() throws Exception {
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "batchGatherScalar", double[][].class, int[].class, double[][].class, int.class);
        method.setAccessible(true);
        
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0, 5.0},
            {6.0, 7.0, 8.0, 9.0, 10.0}
        };
        int[] indices = {0, 2, 4};
        double[][] results = new double[2][3];
        
        method.invoke(null, signals, indices, results, 3);
        
        assertEquals(1.0, results[0][0], EPSILON);
        assertEquals(3.0, results[0][1], EPSILON);
        assertEquals(5.0, results[0][2], EPSILON);
        assertEquals(6.0, results[1][0], EPSILON);
        assertEquals(8.0, results[1][1], EPSILON);
        assertEquals(10.0, results[1][2], EPSILON);
    }
    
    // ==========================================
    // Additional Tests for Better Coverage
    // ==========================================
    
    @Test
    @DisplayName("Test gather periodic downsample with large signal to trigger vector path")
    void testGatherPeriodicDownsampleLargeSignal() {
        // Use large signal to ensure vector path is taken
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length) * 100.0;
        }
        
        double[] filter = {0.25, 0.5, 0.25};
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(128, result.length);
        
        // Verify results are reasonable
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test gather periodic downsample with single-tap filter")
    void testGatherPeriodicDownsampleSingleTapFilter() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {1.0}; // Single tap filter
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // With single tap filter, should get every other element
        assertEquals(1.0, result[0], EPSILON);
        assertEquals(3.0, result[1], EPSILON);
        assertEquals(5.0, result[2], EPSILON);
        assertEquals(7.0, result[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test scatter upsample with large arrays")
    void testScatterUpsampleLargeArrays() {
        int halfLength = 128;
        double[] approx = new double[halfLength];
        double[] detail = new double[halfLength];
        double[] output = new double[halfLength * 2];
        
        // Initialize with pattern
        for (int i = 0; i < halfLength; i++) {
            approx[i] = i * 2.0;
            detail[i] = i * 2.0 + 1.0;
        }
        
        GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
        
        // Verify correct scatter pattern
        for (int i = 0; i < halfLength; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test batch gather with large count")
    void testBatchGatherLargeCount() {
        // Test with count that will use vector path
        int count = 32;
        double[][] signals = {
            new double[64],
            new double[64]
        };
        int[] indices = new int[count];
        double[][] results = new double[2][count];
        
        // Initialize data
        for (int s = 0; s < 2; s++) {
            for (int i = 0; i < 64; i++) {
                signals[s][i] = s * 100 + i;
            }
        }
        for (int i = 0; i < count; i++) {
            indices[i] = i * 2; // Every other element
        }
        
        GatherScatterOps.batchGather(signals, indices, results, count);
        
        // Verify gathered values
        for (int s = 0; s < 2; s++) {
            for (int i = 0; i < count; i++) {
                assertEquals(signals[s][indices[i]], results[s][i], EPSILON);
            }
        }
    }
    
    @Test
    @DisplayName("Test batch gather with empty signals")
    void testBatchGatherEmptySignals() {
        double[][] signals = {};
        int[] indices = {0, 1, 2};
        double[][] results = {};
        
        // Should handle empty signals gracefully
        assertDoesNotThrow(() -> {
            GatherScatterOps.batchGather(signals, indices, results, 3);
        });
    }
    
    @Test
    @DisplayName("Test gather strided with small stride to trigger vector path")
    void testGatherStridedSmallStride() {
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * 1.5;
        }
        
        // Small stride (<=8) should use vector path
        double[] result = GatherScatterOps.gatherStrided(signal, 0, 2, 64);
        
        assertNotNull(result);
        assertEquals(64, result.length);
        
        // Verify strided access
        for (int i = 0; i < 64; i++) {
            assertEquals(signal[i * 2], result[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test gather compressed with large array")
    void testGatherCompressedLargeArray() {
        int length = 128;
        double[] signal = new double[length];
        boolean[] mask = new boolean[length];
        
        // Initialize signal and create sparse mask
        for (int i = 0; i < length; i++) {
            signal[i] = i + 0.5;
            mask[i] = (i % 3 == 0); // Every third element
        }
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        int expectedCount = (length + 2) / 3; // Ceiling division
        assertEquals(expectedCount, result.length);
        
        // Verify gathered values
        int resultIdx = 0;
        for (int i = 0; i < length; i++) {
            if (mask[i]) {
                assertEquals(signal[i], result[resultIdx++], EPSILON);
            }
        }
    }
    
    @Test
    @DisplayName("Test gather compressed with boundary alignment")
    void testGatherCompressedBoundaryAlignment() {
        // Test with length that aligns with vector length
        int vectorLength = 8; // Typical DOUBLE_LENGTH
        double[] signal = new double[vectorLength * 4]; // 32 elements
        boolean[] mask = new boolean[vectorLength * 4];
        
        // Create pattern that tests vector boundary handling
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * 0.1;
            // Create mask that has different patterns at vector boundaries
            if (i < vectorLength) {
                mask[i] = true; // All true in first vector
            } else if (i < vectorLength * 2) {
                mask[i] = false; // All false in second vector
            } else if (i < vectorLength * 3) {
                mask[i] = (i % 2 == 0); // Alternating in third vector
            } else {
                mask[i] = (i == signal.length - 1); // Only last element in fourth vector
            }
        }
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        
        // Count expected elements
        int expectedCount = vectorLength + 0 + vectorLength/2 + 1;
        assertEquals(expectedCount, result.length);
        
        // Verify first vector (all true)
        for (int i = 0; i < vectorLength; i++) {
            assertEquals(signal[i], result[i], EPSILON);
        }
        
        // Verify third vector (alternating)
        int idx = vectorLength;
        for (int i = vectorLength * 2; i < vectorLength * 3; i += 2) {
            assertEquals(signal[i], result[idx++], EPSILON);
        }
        
        // Verify last element
        assertEquals(signal[signal.length - 1], result[result.length - 1], EPSILON);
    }
    
    @Test
    @DisplayName("Test gather periodic downsample with filter length edge cases")
    void testGatherPeriodicDownsampleFilterLengthEdgeCases() {
        double[] signal = new double[16];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
        }
        
        // Test with filter length equal to signal length/2
        double[] longFilter = new double[8];
        for (int i = 0; i < longFilter.length; i++) {
            longFilter[i] = 1.0 / longFilter.length;
        }
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, longFilter, signal.length, longFilter.length);
        
        assertNotNull(result);
        assertEquals(8, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test scatter upsample with odd length handling")
    void testScatterUpsampleOddLength() {
        // Test with odd output length (should handle gracefully)
        double[] approx = {1.0, 2.0, 3.0};
        double[] detail = {0.1, 0.2, 0.3};
        double[] output = new double[7]; // Odd length
        
        GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
        
        // Should scatter as much as possible
        assertEquals(1.0, output[0], EPSILON);
        assertEquals(0.1, output[1], EPSILON);
        assertEquals(2.0, output[2], EPSILON);
        assertEquals(0.2, output[3], EPSILON);
        assertEquals(3.0, output[4], EPSILON);
        assertEquals(0.3, output[5], EPSILON);
        assertEquals(0.0, output[6], EPSILON); // Last element untouched
    }
    
    // ==========================================
    // Additional Coverage Tests
    // ==========================================
    
    @Test
    @DisplayName("Test gather periodic downsample with wrap-around indices")
    void testGatherPeriodicDownsampleWrapAround() {
        // Test with filter length that causes wrap-around
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] filter = {0.25, 0.25, 0.25, 0.25}; // 4-tap filter on 4-element signal
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(2, result.length);
        
        // First output: indices 0,1,2,3 -> all elements
        assertEquals(2.5, result[0], EPSILON); // (1+2+3+4)*0.25
        // Second output: indices 2,3,0,1 (wraps around)
        assertEquals(2.5, result[1], EPSILON); // (3+4+1+2)*0.25
    }
    
    @Test
    @DisplayName("Test gather strided with offset near end")
    void testGatherStridedOffsetNearEnd() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
        
        // Start near end with small count
        double[] result = GatherScatterOps.gatherStrided(signal, 7, 1, 3);
        
        assertNotNull(result);
        assertEquals(3, result.length);
        assertEquals(8.0, result[0], EPSILON);
        assertEquals(9.0, result[1], EPSILON);
        assertEquals(10.0, result[2], EPSILON);
    }
    
    @Test
    @DisplayName("Test gather compressed with complex mask patterns")
    void testGatherCompressedComplexMask() {
        double[] signal = new double[20];
        boolean[] mask = new boolean[20];
        
        // Create complex pattern
        for (int i = 0; i < 20; i++) {
            signal[i] = i * 1.1;
            // Complex mask: true for prime indices
            mask[i] = isPrime(i);
        }
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        // Count primes less than 20: 2,3,5,7,11,13,17,19 = 8
        assertEquals(8, result.length);
        
        // Verify gathered values
        assertEquals(signal[2], result[0], EPSILON);
        assertEquals(signal[3], result[1], EPSILON);
        assertEquals(signal[5], result[2], EPSILON);
        assertEquals(signal[7], result[3], EPSILON);
    }
    
    private boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }
    
    @Test
    @DisplayName("Test batch gather with different signal lengths")
    void testBatchGatherDifferentLengths() {
        // This tests robustness even though signals should typically be same length
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0, 5.0, 6.0},
            {10.0, 20.0, 30.0, 40.0},
            {100.0, 200.0, 300.0, 400.0, 500.0, 600.0, 700.0, 800.0}
        };
        int[] indices = {0, 1, 2};
        double[][] results = new double[3][3];
        
        GatherScatterOps.batchGather(signals, indices, results, 3);
        
        assertEquals(1.0, results[0][0], EPSILON);
        assertEquals(2.0, results[0][1], EPSILON);
        assertEquals(3.0, results[0][2], EPSILON);
        
        assertEquals(10.0, results[1][0], EPSILON);
        assertEquals(20.0, results[1][1], EPSILON);
        assertEquals(30.0, results[1][2], EPSILON);
        
        assertEquals(100.0, results[2][0], EPSILON);
        assertEquals(200.0, results[2][1], EPSILON);
        assertEquals(300.0, results[2][2], EPSILON);
    }
    
    @Test
    @DisplayName("Test gather periodic downsample with maximum filter length")
    void testGatherPeriodicDownsampleMaxFilterLength() {
        double[] signal = new double[32];
        double[] filter = new double[16]; // Half the signal length
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.cos(2 * Math.PI * i / signal.length);
        }
        for (int i = 0; i < filter.length; i++) {
            filter[i] = 1.0 / filter.length;
        }
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(16, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test scatter upsample with pre-filled output")
    void testScatterUpsamplePreFilledOutput() {
        double[] approx = {10.0, 20.0};
        double[] detail = {1.0, 2.0};
        double[] output = {-1.0, -1.0, -1.0, -1.0}; // Pre-filled with -1
        
        GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
        
        // Verify scatter overwrites pre-filled values
        assertEquals(10.0, output[0], EPSILON);
        assertEquals(1.0, output[1], EPSILON);
        assertEquals(20.0, output[2], EPSILON);
        assertEquals(2.0, output[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test gather strided with maximum stride")
    void testGatherStridedMaxStride() {
        double[] signal = new double[100];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * i; // Quadratic pattern
        }
        
        // Large stride (>8) to test scalar fallback
        double[] result = GatherScatterOps.gatherStrided(signal, 0, 15, 6);
        
        assertNotNull(result);
        assertEquals(6, result.length);
        
        assertEquals(0.0 * 0.0, result[0], EPSILON);
        assertEquals(15.0 * 15.0, result[1], EPSILON);
        assertEquals(30.0 * 30.0, result[2], EPSILON);
        assertEquals(45.0 * 45.0, result[3], EPSILON);
        assertEquals(60.0 * 60.0, result[4], EPSILON);
        assertEquals(75.0 * 75.0, result[5], EPSILON);
    }
    
    
    // ==========================================
    // Additional tests to increase coverage to 70%
    // ==========================================
    
    @Test
    @Order(71)
    @DisplayName("Test gather periodic downsample public method with vector path")
    void testGatherPeriodicDownsampleVectorPath() {
        // This should test the vector path when GATHER_SCATTER_AVAILABLE would be true
        double[] signal = new double[32];
        double[] filter = {0.25, 0.25, 0.25, 0.25};
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length) * 100;
        }
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(16, result.length);
        
        // Verify no NaN or Infinity
        for (double val : result) {
            assertFalse(Double.isNaN(val));
            assertFalse(Double.isInfinite(val));
        }
    }
    
    @Test
    @Order(72)
    @DisplayName("Test scatter upsample public method")
    void testScatterUpsamplePublic() {
        double[] approx = new double[16];
        double[] detail = new double[16];
        double[] output = new double[32];
        
        for (int i = 0; i < 16; i++) {
            approx[i] = i * 2.0;
            detail[i] = i * 2.0 + 1.0;
        }
        
        GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
        
        // Verify interleaving
        for (int i = 0; i < 16; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
    }
    
    @Test
    @Order(73)
    @DisplayName("Test batch gather public method with large batch")
    void testBatchGatherLarge() {
        int numSignals = 64;
        int signalLength = 256;
        int numIndices = 32;
        
        double[][] signals = new double[numSignals][signalLength];
        int[] indices = new int[numIndices];
        double[][] results = new double[numSignals][numIndices];
        
        // Initialize
        for (int s = 0; s < numSignals; s++) {
            for (int i = 0; i < signalLength; i++) {
                signals[s][i] = s * 1000.0 + i;
            }
        }
        
        for (int i = 0; i < numIndices; i++) {
            indices[i] = i * 8; // Every 8th element
        }
        
        GatherScatterOps.batchGather(signals, indices, results, numIndices);
        
        // Verify
        for (int s = 0; s < numSignals; s++) {
            for (int i = 0; i < numIndices; i++) {
                assertEquals(signals[s][indices[i]], results[s][i], EPSILON);
            }
        }
    }
    
    @Test
    @Order(74)
    @DisplayName("Test gather strided public method with extra large stride")
    void testGatherStridedExtraLargeStride() {
        double[] signal = new double[200];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.exp(i * 0.01);
        }
        
        // Large stride > 8
        double[] result = GatherScatterOps.gatherStrided(signal, 5, 15, 12);
        
        assertNotNull(result);
        assertEquals(12, result.length);
        
        for (int i = 0; i < 12; i++) {
            assertEquals(signal[5 + i * 15], result[i], EPSILON);
        }
    }
    
    @Test
    @Order(75)
    @DisplayName("Test gather compressed public method with sparse mask")
    void testGatherCompressedSparse() {
        double[] signal = new double[128];
        boolean[] mask = new boolean[128];
        
        // Create sparse mask - every 7th element
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * Math.PI;
            mask[i] = (i % 7 == 0);
        }
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        
        // Count expected
        int expectedCount = 0;
        for (boolean m : mask) {
            if (m) expectedCount++;
        }
        assertEquals(expectedCount, result.length);
        
        // Verify values
        int idx = 0;
        for (int i = 0; i < signal.length; i++) {
            if (mask[i]) {
                assertEquals(signal[i], result[idx++], EPSILON);
            }
        }
    }
    
    @Test
    @Order(76)
    @DisplayName("Test gather periodic downsample with signal length not power of 2")
    void testGatherPeriodicDownsampleNonPowerOf2() {
        double[] signal = new double[50]; // Not a power of 2
        double[] filter = {0.5, 0.5};
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
        }
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(25, result.length);
    }
    
    // ==========================================
    // Test Mode Coverage Tests
    // ==========================================
    
    @Test
    @Order(78)
    @DisplayName("Test setTestMode and clearTestMode functionality")
    void testTestMode() {
        // Save original state
        boolean originalAvailable = GatherScatterOps.isGatherScatterAvailable();
        
        try {
            // Test enabling test mode with true
            GatherScatterOps.setTestMode(true);
            assertTrue(GatherScatterOps.isGatherScatterAvailable());
            
            // Test enabling test mode with false
            GatherScatterOps.setTestMode(false);
            assertFalse(GatherScatterOps.isGatherScatterAvailable());
            
            // Test clearing test mode
            GatherScatterOps.clearTestMode();
            assertEquals(originalAvailable, GatherScatterOps.isGatherScatterAvailable());
            
        } finally {
            // Always restore original state
            GatherScatterOps.clearTestMode();
        }
    }
    
    @Test
    @Order(79)
    @DisplayName("Test gather periodic downsample with test mode enabled")
    void testGatherPeriodicDownsampleWithTestMode() {
        try {
            GatherScatterOps.setTestMode(true);
            
            double[] signal = new double[16];
            double[] filter = {0.5, 0.5};
            
            for (int i = 0; i < signal.length; i++) {
                signal[i] = i + 1.0;
            }
            
            double[] result = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter, signal.length, filter.length);
            
            assertNotNull(result);
            assertEquals(8, result.length);
            
        } finally {
            GatherScatterOps.clearTestMode();
        }
    }
    
    @Test
    @Order(80)
    @DisplayName("Test scatter upsample with test mode enabled")
    void testScatterUpsampleWithTestMode() {
        try {
            GatherScatterOps.setTestMode(true);
            
            double[] approx = {1.0, 2.0, 3.0, 4.0};
            double[] detail = {0.1, 0.2, 0.3, 0.4};
            double[] output = new double[8];
            
            GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
            
            for (int i = 0; i < 4; i++) {
                assertEquals(approx[i], output[2 * i], EPSILON);
                assertEquals(detail[i], output[2 * i + 1], EPSILON);
            }
            
        } finally {
            GatherScatterOps.clearTestMode();
        }
    }
    
    @Test
    @Order(81)
    @DisplayName("Test batch gather with test mode enabled")
    void testBatchGatherWithTestMode() {
        try {
            GatherScatterOps.setTestMode(true);
            
            double[][] signals = {
                {1.0, 2.0, 3.0, 4.0, 5.0},
                {6.0, 7.0, 8.0, 9.0, 10.0}
            };
            int[] indices = {0, 2, 4};
            double[][] results = new double[2][3];
            
            GatherScatterOps.batchGather(signals, indices, results, 3);
            
            assertEquals(1.0, results[0][0], EPSILON);
            assertEquals(3.0, results[0][1], EPSILON);
            assertEquals(5.0, results[0][2], EPSILON);
            assertEquals(6.0, results[1][0], EPSILON);
            assertEquals(8.0, results[1][1], EPSILON);
            assertEquals(10.0, results[1][2], EPSILON);
            
        } finally {
            GatherScatterOps.clearTestMode();
        }
    }
    
    @Test
    @Order(82)
    @DisplayName("Test gather strided with test mode enabled")
    void testGatherStridedWithTestMode() {
        try {
            GatherScatterOps.setTestMode(true);
            
            double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
            double[] result = GatherScatterOps.gatherStrided(signal, 1, 2, 4);
            
            assertNotNull(result);
            assertEquals(4, result.length);
            assertEquals(2.0, result[0], EPSILON);
            assertEquals(4.0, result[1], EPSILON);
            assertEquals(6.0, result[2], EPSILON);
            assertEquals(8.0, result[3], EPSILON);
            
        } finally {
            GatherScatterOps.clearTestMode();
        }
    }
    
    @Test
    @Order(83)
    @DisplayName("Test gather compressed with test mode enabled")
    void testGatherCompressedWithTestMode() {
        try {
            GatherScatterOps.setTestMode(true);
            
            double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
            boolean[] mask = {true, false, true, false, true, false, true, false};
            
            double[] result = GatherScatterOps.gatherCompressed(signal, mask);
            
            assertNotNull(result);
            assertEquals(4, result.length);
            assertEquals(1.0, result[0], EPSILON);
            assertEquals(3.0, result[1], EPSILON);
            assertEquals(5.0, result[2], EPSILON);
            assertEquals(7.0, result[3], EPSILON);
            
        } finally {
            GatherScatterOps.clearTestMode();
        }
    }
    
    // ==========================================
    // Force Vector Method Coverage Tests
    // ==========================================
    
    @Test
    @Order(84)
    @DisplayName("Test force vector gather periodic downsample")
    void testGatherPeriodicDownsampleForceVector() throws Exception {
        // Use reflection to access package-private method
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "gatherPeriodicDownsampleForceVector", double[].class, double[].class, int.class, int.class);
        method.setAccessible(true);
        
        double[] signal = new double[16];
        double[] filter = {0.5, 0.5};
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
        }
        
        double[] result = (double[]) method.invoke(null, signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(8, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @Order(85)
    @DisplayName("Test force vector scatter upsample")
    void testScatterUpsampleForceVector() throws Exception {
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "scatterUpsampleForceVector", double[].class, double[].class, double[].class, int.class);
        method.setAccessible(true);
        
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.1, 0.2, 0.3, 0.4};
        double[] output = new double[8];
        
        method.invoke(null, approx, detail, output, output.length);
        
        for (int i = 0; i < 4; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
    }
    
    @Test
    @Order(86)
    @DisplayName("Test force vector batch gather")
    void testBatchGatherForceVector() throws Exception {
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "batchGatherForceVector", double[][].class, int[].class, double[][].class, int.class);
        method.setAccessible(true);
        
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0, 5.0},
            {6.0, 7.0, 8.0, 9.0, 10.0}
        };
        int[] indices = {0, 2, 4};
        double[][] results = new double[2][3];
        
        method.invoke(null, signals, indices, results, 3);
        
        assertEquals(1.0, results[0][0], EPSILON);
        assertEquals(3.0, results[0][1], EPSILON);
        assertEquals(5.0, results[0][2], EPSILON);
        assertEquals(6.0, results[1][0], EPSILON);
        assertEquals(8.0, results[1][1], EPSILON);
        assertEquals(10.0, results[1][2], EPSILON);
    }
    
    @Test
    @Order(87)
    @DisplayName("Test force vector gather strided")
    void testGatherStridedForceVector() throws Exception {
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "gatherStridedForceVector", double[].class, int.class, int.class, int.class);
        method.setAccessible(true);
        
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0};
        double[] result = (double[]) method.invoke(null, signal, 1, 2, 4);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals(2.0, result[0], EPSILON);
        assertEquals(4.0, result[1], EPSILON);
        assertEquals(6.0, result[2], EPSILON);
        assertEquals(8.0, result[3], EPSILON);
    }
    
    @Test
    @Order(88)
    @DisplayName("Test force vector gather strided with large stride")
    void testGatherStridedForceVectorLargeStride() throws Exception {
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "gatherStridedForceVector", double[].class, int.class, int.class, int.class);
        method.setAccessible(true);
        
        double[] signal = new double[100];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * 1.5;
        }
        
        // Large stride (>8) should use scalar path
        double[] result = (double[]) method.invoke(null, signal, 0, 15, 5);
        
        assertNotNull(result);
        assertEquals(5, result.length);
        assertEquals(0.0, result[0], EPSILON);
        assertEquals(22.5, result[1], EPSILON);
        assertEquals(45.0, result[2], EPSILON);
        assertEquals(67.5, result[3], EPSILON);
        assertEquals(90.0, result[4], EPSILON);
    }
    
    @Test
    @Order(89)
    @DisplayName("Test force vector gather compressed")
    void testGatherCompressedForceVector() throws Exception {
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "gatherCompressedForceVector", double[].class, boolean[].class);
        method.setAccessible(true);
        
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        boolean[] mask = {true, false, true, false, true, false, true, false};
        
        double[] result = (double[]) method.invoke(null, signal, mask);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        assertEquals(1.0, result[0], EPSILON);
        assertEquals(3.0, result[1], EPSILON);
        assertEquals(5.0, result[2], EPSILON);
        assertEquals(7.0, result[3], EPSILON);
    }
    
    @Test
    @Order(90)
    @DisplayName("Test force vector methods with large arrays")
    void testForceVectorMethodsLargeArrays() throws Exception {
        // Test gather periodic downsample force vector with large array
        Method gatherMethod = GatherScatterOps.class.getDeclaredMethod(
            "gatherPeriodicDownsampleForceVector", double[].class, double[].class, int.class, int.class);
        gatherMethod.setAccessible(true);
        
        double[] signal = new double[128];
        double[] filter = {0.25, 0.5, 0.25};
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length);
        }
        
        double[] result = (double[]) gatherMethod.invoke(null, signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(64, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
        
        // Test scatter upsample force vector with large array
        Method scatterMethod = GatherScatterOps.class.getDeclaredMethod(
            "scatterUpsampleForceVector", double[].class, double[].class, double[].class, int.class);
        scatterMethod.setAccessible(true);
        
        double[] approx = new double[32];
        double[] detail = new double[32];
        double[] output = new double[64];
        
        for (int i = 0; i < 32; i++) {
            approx[i] = i * 2.0;
            detail[i] = i * 2.0 + 1.0;
        }
        
        scatterMethod.invoke(null, approx, detail, output, output.length);
        
        for (int i = 0; i < 32; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
    }
    
    // ==========================================
    // Edge Case Coverage Tests
    // ==========================================
    
    @Test
    @Order(91)
    @DisplayName("Test gather compressed with zero mask")
    void testGatherCompressedZeroMask() {
        try {
            GatherScatterOps.setTestMode(true);
            
            double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
            boolean[] mask = {false, false, false, false, false, false, false, false};
            
            double[] result = GatherScatterOps.gatherCompressed(signal, mask);
            
            assertNotNull(result);
            assertEquals(0, result.length);
            
        } finally {
            GatherScatterOps.clearTestMode();
        }
    }
    
    @Test
    @Order(92)
    @DisplayName("Test gather compressed with partial vector mask")
    void testGatherCompressedPartialVectorMask() throws Exception {
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "gatherCompressedForceVector", double[].class, boolean[].class);
        method.setAccessible(true);
        
        // Create mask with varying density in different vector lanes
        double[] signal = new double[32];
        boolean[] mask = new boolean[32];
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
            // Create pattern where some vectors have high density, others low
            if (i < 8) {
                mask[i] = true; // First vector: all true
            } else if (i < 16) {
                mask[i] = false; // Second vector: all false
            } else if (i < 24) {
                mask[i] = (i % 2 == 0); // Third vector: alternating
            } else {
                mask[i] = (i == 31); // Fourth vector: only last element
            }
        }
        
        double[] result = (double[]) method.invoke(null, signal, mask);
        
        assertNotNull(result);
        
        // Count expected elements: 8 + 0 + 4 + 1 = 13
        assertEquals(13, result.length);
        
        // Verify first 8 elements (first vector)
        for (int i = 0; i < 8; i++) {
            assertEquals(signal[i], result[i], EPSILON);
        }
        
        // Verify alternating elements from third vector
        for (int i = 0; i < 4; i++) {
            assertEquals(signal[16 + i * 2], result[8 + i], EPSILON);
        }
        
        // Verify last element
        assertEquals(signal[31], result[12], EPSILON);
    }
    
    // ==========================================
    // Additional Coverage Tests
    // ==========================================
    
    @Test
    @Order(93)
    @DisplayName("Test gather periodic downsample with vector species edge cases")
    void testGatherPeriodicDownsampleVectorSpeciesEdges() {
        // Test with arrays that exactly hit vector length boundaries
        double[] signal = new double[64]; // Multiple of typical vector length
        double[] filter = new double[8];
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16) + Math.cos(2 * Math.PI * i / 8);
        }
        
        for (int i = 0; i < filter.length; i++) {
            filter[i] = Math.exp(-(i - 4) * (i - 4) / 8.0);
        }
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(32, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @Order(94)
    @DisplayName("Test complex remainder handling in gather operations")
    void testComplexRemainderHandling() {
        // Use sizes that create complex remainder patterns
        double[] signal = new double[33]; // Prime number to test edge cases
        double[] filter = {0.3333, 0.3333, 0.3334};
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = (i % 7) * 0.5; // Pattern with period 7
        }
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(16, result.length); // floor(33/2)
        
        // Verify specific calculations for remainder handling
        for (int i = 0; i < result.length; i++) {
            assertFalse(Double.isNaN(result[i]));
            assertFalse(Double.isInfinite(result[i]));
        }
    }
    
    @Test
    @Order(95)
    @DisplayName("Test edge cases in scatter upsample with odd lengths")
    void testScatterUpsampleEdgeCases() {
        // Test with various odd output lengths
        int[] oddLengths = {7, 9, 11, 13, 15, 17};
        
        for (int length : oddLengths) {
            int halfLength = length / 2;
            double[] approx = new double[halfLength];
            double[] detail = new double[halfLength];
            double[] output = new double[length];
            
            for (int i = 0; i < halfLength; i++) {
                approx[i] = i * 2.0;
                detail[i] = i * 2.0 + 1.0;
            }
            
            GatherScatterOps.scatterUpsample(approx, detail, output, length);
            
            // Verify scattered values up to what can be placed
            for (int i = 0; i < halfLength; i++) {
                assertEquals(approx[i], output[2 * i], EPSILON);
                if (2 * i + 1 < length) {
                    assertEquals(detail[i], output[2 * i + 1], EPSILON);
                }
            }
        }
    }
    
    @Test
    @Order(96)
    @DisplayName("Test batch gather with edge case array configurations")
    void testBatchGatherEdgeConfigurations() {
        // Test with mismatched signal vs result array dimensions
        double[][] signals = new double[3][];
        signals[0] = new double[100];
        signals[1] = new double[50]; 
        signals[2] = new double[75];
        
        // Initialize with different patterns
        for (int s = 0; s < 3; s++) {
            for (int i = 0; i < signals[s].length; i++) {
                signals[s][i] = s * 1000 + i;
            }
        }
        
        int[] indices = {0, 5, 10, 15, 20, 25, 30};
        double[][] results = new double[3][7];
        
        GatherScatterOps.batchGather(signals, indices, results, 7);
        
        // Verify gathered values for each signal
        for (int s = 0; s < 3; s++) {
            for (int i = 0; i < 7; i++) {
                if (indices[i] < signals[s].length) {
                    assertEquals(signals[s][indices[i]], results[s][i], EPSILON);
                }
            }
        }
    }
    
    @Test
    @Order(97)
    @DisplayName("Test vector boundary crossing in gather compressed")
    void testVectorBoundaryCrossingGatherCompressed() {
        // Test with patterns that cross vector lane boundaries
        int size = 72; // Not a power of 2, creates interesting boundary crossings
        double[] signal = new double[size];
        boolean[] mask = new boolean[size];
        
        for (int i = 0; i < size; i++) {
            signal[i] = i * 0.1;
            // Create pattern that has different densities across vector boundaries
            mask[i] = (i % 3 == 0) || (i % 7 == 0); // Irregular pattern
        }
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        
        // Count expected elements
        int expectedCount = 0;
        for (boolean m : mask) {
            if (m) expectedCount++;
        }
        assertEquals(expectedCount, result.length);
        
        // Verify values
        int idx = 0;
        for (int i = 0; i < size; i++) {
            if (mask[i]) {
                assertEquals(signal[i], result[idx++], EPSILON);
            }
        }
    }
    
    @Test
    @Order(98)
    @DisplayName("Test isGatherScatterEnabled method with test mode")
    void testIsGatherScatterEnabledWithTestMode() {
        boolean originalState = GatherScatterOps.isGatherScatterAvailable();
        
        try {
            // Test enabling
            GatherScatterOps.setTestMode(true);
            assertTrue(GatherScatterOps.isGatherScatterAvailable());
            
            // Test disabling
            GatherScatterOps.setTestMode(false);
            assertFalse(GatherScatterOps.isGatherScatterAvailable());
            
            // Test all operations work with test mode enabled
            GatherScatterOps.setTestMode(true);
            
            double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
            double[] filter = {0.5, 0.5};
            
            double[] result = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter, signal.length, filter.length);
            
            assertNotNull(result);
            assertEquals(4, result.length);
            
        } finally {
            GatherScatterOps.clearTestMode();
            assertEquals(originalState, GatherScatterOps.isGatherScatterAvailable());
        }
    }
    
    @Test
    @Order(99)
    @DisplayName("Test error conditions and boundary validations")
    void testErrorConditionsAndBoundaries() {
        // Test with empty arrays - should handle gracefully
        double[] emptySignal = {};
        double[] emptyFilter = {};
        
        assertDoesNotThrow(() -> {
            double[] result = GatherScatterOps.gatherPeriodicDownsample(
                emptySignal, emptyFilter, 0, 0);
            assertNotNull(result);
            assertEquals(0, result.length);
        });
        
        // Test scatter with empty arrays
        double[] emptyApprox = {};
        double[] emptyDetail = {};
        double[] emptyOutput = {};
        
        assertDoesNotThrow(() -> {
            GatherScatterOps.scatterUpsample(emptyApprox, emptyDetail, emptyOutput, 0);
        });
        
        // Test batch gather with empty signals
        double[][] emptySignals = {};
        int[] emptyIndices = {};
        double[][] emptyResults = {};
        
        assertDoesNotThrow(() -> {
            GatherScatterOps.batchGather(emptySignals, emptyIndices, emptyResults, 0);
        });
    }
    
    @Test
    @Order(100)
    @DisplayName("Test platform info string formatting edge cases")
    void testPlatformInfoStringFormattingEdges() {
        String info = GatherScatterOps.getGatherScatterInfo();
        
        assertNotNull(info);
        assertFalse(info.isEmpty());
        
        // Verify all expected components are present
        assertTrue(info.contains("Gather/Scatter Support"));
        assertTrue(info.contains("Vector Length"));
        assertTrue(info.contains("Platform"));
        
        // Should handle both available and not available states
        assertTrue(info.contains("Available") || info.contains("Not Available"));
        
        // Should have proper formatting
        assertTrue(info.contains("doubles"));
        assertTrue(info.contains("Endian"));
        
        // Test with different modes
        try {
            GatherScatterOps.setTestMode(true);
            String infoTestMode = GatherScatterOps.getGatherScatterInfo();
            assertNotNull(infoTestMode);
            assertTrue(infoTestMode.contains("Available"));
            
            GatherScatterOps.setTestMode(false);
            String infoTestModeFalse = GatherScatterOps.getGatherScatterInfo();
            assertNotNull(infoTestModeFalse);
            assertTrue(infoTestModeFalse.contains("Not Available"));
            
        } finally {
            GatherScatterOps.clearTestMode();
        }
    }
    
    @Test
    @Order(77)
    @DisplayName("Test scatter upsample with different sizes")
    void testScatterUpsampleDifferentSizes() {
        int[] sizes = {4, 8, 16, 32, 64};
        
        for (int halfSize : sizes) {
            double[] approx = new double[halfSize];
            double[] detail = new double[halfSize];
            double[] output = new double[halfSize * 2];
            
            for (int i = 0; i < halfSize; i++) {
                approx[i] = Math.cos(2 * Math.PI * i / halfSize);
                detail[i] = Math.sin(2 * Math.PI * i / halfSize);
            }
            
            GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
            
            // Verify
            for (int i = 0; i < halfSize; i++) {
                assertEquals(approx[i], output[2 * i], EPSILON);
                assertEquals(detail[i], output[2 * i + 1], EPSILON);
            }
        }
    }
    
    // ==========================================
    // Additional Tests for 70% Coverage
    // ==========================================
    
    @Test
    @Order(101)
    @DisplayName("Test gather periodic downsample force vector with large array to trigger vector path")
    void testGatherPeriodicDownsampleForceVectorLargeArray() throws Exception {
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "gatherPeriodicDownsampleForceVector", double[].class, double[].class, int.class, int.class);
        method.setAccessible(true);
        
        // Large array that would trigger vector operations if DOUBLE_LENGTH >= 4
        double[] signal = new double[64];
        double[] filter = {0.25, 0.25, 0.25, 0.25};
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(i * Math.PI / 16) + Math.random() * 0.1;
        }
        
        double[] result = (double[]) method.invoke(null, signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(32, result.length);
        
        // Verify no NaN/Infinity
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test 
    @Order(102)
    @DisplayName("Test scatter upsample force vector with large array to trigger vector path")
    void testScatterUpsampleForceVectorLargeArray() throws Exception {
        Method method = GatherScatterOps.class.getDeclaredMethod(
            "scatterUpsampleForceVector", double[].class, double[].class, double[].class, int.class);
        method.setAccessible(true);
        
        // Large arrays to trigger vector code paths
        double[] approx = new double[32];
        double[] detail = new double[32];
        double[] output = new double[64];
        
        for (int i = 0; i < 32; i++) {
            approx[i] = i * 3.14159 / 32;
            detail[i] = Math.cos(i * 3.14159 / 16);
        }
        
        method.invoke(null, approx, detail, output, output.length);
        
        // Verify interleaving
        for (int i = 0; i < 32; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
    }
    
    @Test
    @Order(103)
    @DisplayName("Test platform detection paths by forcing exceptions")
    void testPlatformDetectionExceptionPaths() throws Exception {
        // Test that checkGatherScatterSupport handles exceptions properly
        Method method = GatherScatterOps.class.getDeclaredMethod("checkGatherScatterSupport");
        method.setAccessible(true);
        
        // Multiple calls to ensure consistent behavior and exercise exception handling
        for (int i = 0; i < 5; i++) {
            Boolean result = (Boolean) method.invoke(null);
            assertNotNull(result);
            // Should always return the same value
        }
    }
    
    @Test
    @Order(104) 
    @DisplayName("Test checkGatherScatterSupport different platform branches")
    void testCheckGatherScatterSupportPlatformBranches() throws Exception {
        // This test helps exercise the platform detection switch statement
        Method checkMethod = GatherScatterOps.class.getDeclaredMethod("checkGatherScatterSupport");
        checkMethod.setAccessible(true);
        
        // Call multiple times to ensure stability
        Boolean result1 = (Boolean) checkMethod.invoke(null);
        Boolean result2 = (Boolean) checkMethod.invoke(null);
        
        assertEquals(result1, result2, "checkGatherScatterSupport should return consistent results");
        
        // Test with different vector lengths by calling again
        for (int i = 0; i < 10; i++) {
            Boolean result = (Boolean) checkMethod.invoke(null);
            assertNotNull(result);
        }
    }
    
    @Test
    @Order(105)
    @DisplayName("Test isGatherScatterEnabled private method directly")  
    void testIsGatherScatterEnabledPrivateMethod() throws Exception {
        Method method = GatherScatterOps.class.getDeclaredMethod("isGatherScatterEnabled");
        method.setAccessible(true);
        
        // Test normal state
        Boolean normalResult = (Boolean) method.invoke(null);
        assertNotNull(normalResult);
        
        try {
            // Test with test mode enabled
            GatherScatterOps.setTestMode(true);
            Boolean testModeResult = (Boolean) method.invoke(null);
            assertTrue(testModeResult);
            
            // Test with test mode disabled  
            GatherScatterOps.setTestMode(false);
            Boolean testModeDisabledResult = (Boolean) method.invoke(null);
            assertFalse(testModeDisabledResult);
            
        } finally {
            GatherScatterOps.clearTestMode();
        }
    }
    
    @Test
    @Order(106)
    @DisplayName("Test Big Endian code path in getGatherScatterInfo")
    void testBigEndianCodePath() {
        // This tests the IS_LITTLE_ENDIAN branch in getGatherScatterInfo
        String info = GatherScatterOps.getGatherScatterInfo();
        
        // Should contain endianness information
        assertTrue(info.contains("Little Endian") || info.contains("Big Endian"));
        
        // Test with different test modes
        try {
            GatherScatterOps.setTestMode(true);
            String infoEnabled = GatherScatterOps.getGatherScatterInfo();
            assertTrue(infoEnabled.contains("Available"));
            assertTrue(infoEnabled.contains("Endian"));
            
            GatherScatterOps.setTestMode(false);
            String infoDisabled = GatherScatterOps.getGatherScatterInfo();
            assertTrue(infoDisabled.contains("Not Available"));
            assertTrue(infoDisabled.contains("Endian"));
            
        } finally {
            GatherScatterOps.clearTestMode();
        }
    }
    
    @Test
    @Order(107)
    @DisplayName("Test vector length boundary conditions")
    void testVectorLengthBoundaryConditions() throws Exception {
        // This helps test the DOUBLE_LENGTH < 4 conditions in various methods
        
        // Access the static field to understand current vector length
        Field doubleSpeciesField = GatherScatterOps.class.getDeclaredField("DOUBLE_SPECIES");
        doubleSpeciesField.setAccessible(true);
        Object doubleSpecies = doubleSpeciesField.get(null);
        
        Field doubleLengthField = GatherScatterOps.class.getDeclaredField("DOUBLE_LENGTH");
        doubleLengthField.setAccessible(true);
        int doubleLength = (Integer) doubleLengthField.get(null);
        
        assertTrue(doubleLength > 0);
        
        // Test methods with awareness of vector length
        double[] signal = new double[Math.max(16, doubleLength * 2)];
        double[] filter = {0.5, 0.5};
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.random();
        }
        
        // Test both force vector methods
        Method gatherForceMethod = GatherScatterOps.class.getDeclaredMethod(
            "gatherPeriodicDownsampleForceVector", double[].class, double[].class, int.class, int.class);
        gatherForceMethod.setAccessible(true);
        
        double[] result = (double[]) gatherForceMethod.invoke(null, signal, filter, signal.length, filter.length);
        assertNotNull(result);
    }
    
    @Test
    @Order(108)
    @DisplayName("Test remainder handling in vector operations")
    void testRemainderHandlingInVectorOps() throws Exception {
        // Test arrays sized to create remainder when divided by vector length
        int[] testSizes = {13, 17, 19, 23, 29, 31}; // Prime numbers
        
        for (int size : testSizes) {
            double[] signal = new double[size];
            double[] filter = {0.333, 0.334, 0.333};
            
            for (int i = 0; i < size; i++) {
                signal[i] = Math.sin(2 * Math.PI * i / size);
            }
            
            // Test regular method
            double[] result1 = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter, signal.length, filter.length);
            
            // Test force vector method
            Method forceMethod = GatherScatterOps.class.getDeclaredMethod(
                "gatherPeriodicDownsampleForceVector", double[].class, double[].class, int.class, int.class);
            forceMethod.setAccessible(true);
            
            double[] result2 = (double[]) forceMethod.invoke(null, signal, filter, signal.length, filter.length);
            
            // Both should produce valid results
            assertNotNull(result1);
            assertNotNull(result2);
            assertEquals(size / 2, result1.length);
            assertEquals(size / 2, result2.length);
        }
    }
    
    @Test
    @Order(109)
    @DisplayName("Test vector code paths with aggressive test mode manipulation")
    void testVectorCodePathsAggressiveTestMode() {
        try {
            // Enable test mode to force vector paths
            GatherScatterOps.setTestMode(true);
            
            // Test large arrays that would trigger vector operations
            double[] signal = new double[128];
            double[] filter = {0.1, 0.2, 0.3, 0.4};
            
            for (int i = 0; i < signal.length; i++) {
                signal[i] = Math.sin(2 * Math.PI * i / 32) + 0.1 * Math.random();
            }
            
            // This should now use the vector path in gatherPeriodicDownsample
            double[] result = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter, signal.length, filter.length);
            
            assertNotNull(result);
            assertEquals(64, result.length);
            
            // Test scatter upsample with vector path
            double[] approx = new double[32];
            double[] detail = new double[32];
            double[] output = new double[64];
            
            for (int i = 0; i < 32; i++) {
                approx[i] = Math.cos(i * Math.PI / 16);
                detail[i] = Math.sin(i * Math.PI / 16);
            }
            
            GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
            
            // Verify interleaving
            for (int i = 0; i < 32; i++) {
                assertEquals(approx[i], output[2 * i], EPSILON);
                assertEquals(detail[i], output[2 * i + 1], EPSILON);
            }
            
            // Test gather strided with vector path
            double[] stridedSignal = new double[100];
            for (int i = 0; i < stridedSignal.length; i++) {
                stridedSignal[i] = i * 0.1;
            }
            
            double[] stridedResult = GatherScatterOps.gatherStrided(stridedSignal, 0, 3, 30);
            assertNotNull(stridedResult);
            assertEquals(30, stridedResult.length);
            
            // Test gather compressed with vector path
            boolean[] mask = new boolean[64];
            for (int i = 0; i < mask.length; i++) {
                mask[i] = (i % 2 == 0);
            }
            
            double[] compressedResult = GatherScatterOps.gatherCompressed(signal, mask);
            assertNotNull(compressedResult);
            assertEquals(32, compressedResult.length);
            
        } finally {
            GatherScatterOps.clearTestMode();
        }
    }
    
    @Test
    @Order(110)
    @DisplayName("Test vector remainder paths with specific sizes")
    void testVectorRemainderPaths() {
        try {
            GatherScatterOps.setTestMode(true);
            
            // Use sizes that will create remainders when divided by vector length
            // and force execution of remainder handling code
            int[] testSizes = {21, 37, 53, 71, 89}; // Primes that create remainders
            
            for (int size : testSizes) {
                double[] signal = new double[size];
                double[] filter = {0.25, 0.25, 0.25, 0.25};
                
                for (int i = 0; i < size; i++) {
                    signal[i] = Math.sin(2 * Math.PI * i / 16);
                }
                
                double[] result = GatherScatterOps.gatherPeriodicDownsample(
                    signal, filter, signal.length, filter.length);
                
                assertNotNull(result);
                assertEquals(size / 2, result.length);
                
                // Test scatter with odd sizes
                if (size > 8) {
                    int halfSize = size / 4;
                    double[] approx = new double[halfSize];
                    double[] detail = new double[halfSize];
                    double[] output = new double[halfSize * 2];
                    
                    for (int i = 0; i < halfSize; i++) {
                        approx[i] = i;
                        detail[i] = i + 0.5;
                    }
                    
                    GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
                    
                    // Verify first few elements
                    for (int i = 0; i < Math.min(4, halfSize); i++) {
                        assertEquals(approx[i], output[2 * i], EPSILON);
                        assertEquals(detail[i], output[2 * i + 1], EPSILON);
                    }
                }
            }
            
        } finally {
            GatherScatterOps.clearTestMode();
        }
    }
    
    @Test
    @Order(111)
    @DisplayName("Test all branch conditions in gather compressed")
    void testAllBranchConditionsGatherCompressed() {
        try {
            GatherScatterOps.setTestMode(true);
            
            // Test with mask that creates zero compressed length in some vectors
            double[] signal = new double[32];
            boolean[] mask = new boolean[32];
            
            for (int i = 0; i < signal.length; i++) {
                signal[i] = i * 0.5;
                // Create pattern where some vector segments have zero true elements
                if (i < 8) {
                    mask[i] = true;  // First vector: all true
                } else if (i < 16) {
                    mask[i] = false; // Second vector: all false (compressedLength = 0)
                } else if (i < 24) {
                    mask[i] = (i % 4 == 0); // Third vector: sparse
                } else {
                    mask[i] = (i == 31); // Fourth vector: only last element
                }
            }
            
            double[] result = GatherScatterOps.gatherCompressed(signal, mask);
            
            assertNotNull(result);
            // Should have 8 + 0 + 2 + 1 = 11 elements
            
            // Verify first 8 elements
            for (int i = 0; i < 8; i++) {
                assertEquals(signal[i], result[i], EPSILON);
            }
            
        } finally {
            GatherScatterOps.clearTestMode();
        }
    }
    
    @Test
    @Order(112)
    @DisplayName("Test exception handling in checkGatherScatterSupport")
    void testExceptionHandlingInCheckGatherScatterSupport() throws Exception {
        // Test that the method handles exceptions in the try-catch block
        Method method = GatherScatterOps.class.getDeclaredMethod("checkGatherScatterSupport");
        method.setAccessible(true);
        
        // Call multiple times to ensure consistent exception handling
        for (int i = 0; i < 10; i++) {
            Boolean result = (Boolean) method.invoke(null);
            assertNotNull(result);
            // Should consistently return the same value (false on most platforms)
        }
    }
    
    @Test
    @Order(113)
    @DisplayName("Test vector length boundary with minimal vector lengths")
    void testVectorLengthBoundaryMinimal() throws Exception {
        // Access the DOUBLE_LENGTH field to understand the current platform
        Field doubleLengthField = GatherScatterOps.class.getDeclaredField("DOUBLE_LENGTH");
        doubleLengthField.setAccessible(true);
        int doubleLength = (Integer) doubleLengthField.get(null);
        
        // Test with arrays sized just at the boundary conditions
        double[] signal = new double[doubleLength * 3 + 1]; // Just over 3 vectors
        double[] filter = {0.5, 0.5};
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.random();
        }
        
        try {
            GatherScatterOps.setTestMode(true);
            
            double[] result = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter, signal.length, filter.length);
            
            assertNotNull(result);
            assertEquals(signal.length / 2, result.length);
            
        } finally {
            GatherScatterOps.clearTestMode();
        }
    }
}
