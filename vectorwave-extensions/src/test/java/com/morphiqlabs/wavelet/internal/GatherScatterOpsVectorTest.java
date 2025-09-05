package com.morphiqlabs.wavelet.extensions.internal;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite specifically for testing the vector paths in GatherScatterOps.
 * This test class uses package-private force methods to test vector paths.
 */
@DisplayName("GatherScatterOps Vector Path Test Suite")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatherScatterOpsVectorTest {
    
    private static final double EPSILON = 1e-10;
    
    // ==========================================
    // Gather Periodic Downsample Vector Tests
    // ==========================================
    
    @Test
    @Order(1)
    @DisplayName("Test gather periodic downsample vector path basic")
    void testGatherPeriodicDownsampleVectorBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0, 11.0, 12.0, 13.0, 14.0, 15.0, 16.0};
        double[] filter = {0.5, 0.5};
        
        double[] result = GatherScatterOps.gatherPeriodicDownsampleForceVector(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(8, result.length);
        
        // Expected values: averaging pairs
        double[] expected = {1.5, 3.5, 5.5, 7.5, 9.5, 11.5, 13.5, 15.5};
        
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i], EPSILON);
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("Test gather periodic downsample vector path with longer filter")
    void testGatherPeriodicDownsampleVectorLongFilter() {
        double[] signal = new double[32];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length);
        }
        
        double[] filter = {0.25, 0.25, 0.25, 0.25}; // 4-tap averaging filter
        
        double[] result = GatherScatterOps.gatherPeriodicDownsampleForceVector(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(16, result.length);
        
        // Verify no NaN or Infinity
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {16, 32, 64, 128})
    @Order(3)
    @DisplayName("Test gather periodic downsample vector path various sizes")
    void testGatherPeriodicDownsampleVectorVariousSizes(int size) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = i + 1.0;
        }
        
        double[] filter = {1.0/3, 1.0/3, 1.0/3};
        
        double[] result = GatherScatterOps.gatherPeriodicDownsampleForceVector(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(size / 2, result.length);
    }
    
    // ==========================================
    // Scatter Upsample Vector Tests
    // ==========================================
    
    @Test
    @Order(4)
    @DisplayName("Test scatter upsample vector path basic")
    void testScatterUpsampleVectorBasic() {
        double[] approx = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] detail = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8};
        double[] output = new double[16];
        
        GatherScatterOps.scatterUpsampleForceVector(approx, detail, output, output.length);
        
        // Verify interleaved pattern
        for (int i = 0; i < 8; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {8, 16, 32, 64})
    @Order(5)
    @DisplayName("Test scatter upsample vector path various sizes")
    void testScatterUpsampleVectorVariousSizes(int halfSize) {
        double[] approx = new double[halfSize];
        double[] detail = new double[halfSize];
        double[] output = new double[halfSize * 2];
        
        for (int i = 0; i < halfSize; i++) {
            approx[i] = i * 10.0;
            detail[i] = i * 10.0 + 5.0;
        }
        
        GatherScatterOps.scatterUpsampleForceVector(approx, detail, output, output.length);
        
        // Verify pattern
        for (int i = 0; i < halfSize; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
    }
    
    // ==========================================
    // Batch Gather Vector Tests
    // ==========================================
    
    @Test
    @Order(6)
    @DisplayName("Test batch gather vector path")
    void testBatchGatherVector() {
        double[][] signals = new double[8][32];
        int[] indices = new int[16];
        double[][] results = new double[8][16];
        
        // Initialize signals
        for (int s = 0; s < 8; s++) {
            for (int i = 0; i < 32; i++) {
                signals[s][i] = s * 100 + i;
            }
        }
        
        // Initialize indices
        for (int i = 0; i < 16; i++) {
            indices[i] = i * 2;
        }
        
        GatherScatterOps.batchGatherForceVector(signals, indices, results, 16);
        
        // Verify gathered values
        for (int s = 0; s < 8; s++) {
            for (int i = 0; i < 16; i++) {
                assertEquals(signals[s][indices[i]], results[s][i], EPSILON);
            }
        }
    }
    
    // ==========================================
    // Gather Strided Vector Tests
    // ==========================================
    
    @Test
    @Order(7)
    @DisplayName("Test gather strided vector path with small stride")
    void testGatherStridedVectorSmallStride() {
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * 1.5;
        }
        
        // Small stride (<=8) should use vector path
        double[] result = GatherScatterOps.gatherStridedForceVector(signal, 0, 2, 20);
        
        assertNotNull(result);
        assertEquals(20, result.length);
        
        for (int i = 0; i < 20; i++) {
            assertEquals(signal[i * 2], result[i], EPSILON);
        }
    }
    
    @ParameterizedTest
    @CsvSource({
        "1, 3, 16",
        "0, 4, 12",
        "2, 2, 20",
        "3, 5, 10"
    })
    @Order(8)
    @DisplayName("Test gather strided vector path various parameters")
    void testGatherStridedVectorVariousParams(int offset, int stride, int count) {
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.cos(2 * Math.PI * i / signal.length) * 100;
        }
        
        double[] result = GatherScatterOps.gatherStridedForceVector(signal, offset, stride, count);
        
        assertNotNull(result);
        assertEquals(count, result.length);
        
        // Verify gathered values
        for (int i = 0; i < count; i++) {
            assertEquals(signal[offset + i * stride], result[i], EPSILON);
        }
    }
    
    // ==========================================
    // Gather Compressed Vector Tests
    // ==========================================
    
    @Test
    @Order(9)
    @DisplayName("Test gather compressed vector path")
    void testGatherCompressedVector() {
        double[] signal = new double[32];
        boolean[] mask = new boolean[32];
        
        for (int i = 0; i < 32; i++) {
            signal[i] = i * 2.5;
            mask[i] = (i % 3 == 0);
        }
        
        double[] result = GatherScatterOps.gatherCompressedForceVector(signal, mask);
        
        assertNotNull(result);
        
        // Count expected elements
        int expectedCount = 0;
        for (boolean m : mask) {
            if (m) expectedCount++;
        }
        assertEquals(expectedCount, result.length);
        
        // Verify values
        int idx = 0;
        for (int i = 0; i < 32; i++) {
            if (mask[i]) {
                assertEquals(signal[i], result[idx++], EPSILON);
            }
        }
    }
    
    @Test
    @Order(10)
    @DisplayName("Test gather compressed vector path with aligned patterns")
    void testGatherCompressedVectorAligned() {
        // Test with vector-aligned size
        int size = 64;
        double[] signal = new double[size];
        boolean[] mask = new boolean[size];
        
        // Create different patterns for each vector chunk
        for (int i = 0; i < size; i++) {
            signal[i] = i + 0.5;
            if (i < 8) {
                mask[i] = true; // All true in first vector
            } else if (i < 16) {
                mask[i] = false; // All false in second vector
            } else if (i < 24) {
                mask[i] = (i % 2 == 0); // Alternating
            } else {
                mask[i] = (i % 4 == 0); // Every 4th
            }
        }
        
        double[] result = GatherScatterOps.gatherCompressedForceVector(signal, mask);
        
        assertNotNull(result);
        
        // Verify compressed values
        int idx = 0;
        for (int i = 0; i < size; i++) {
            if (mask[i]) {
                assertEquals(signal[i], result[idx++], EPSILON);
            }
        }
        assertEquals(idx, result.length);
    }
    
    // ==========================================
    // Edge Cases with Vector Path
    // ==========================================
    
    @Test
    @Order(11)
    @DisplayName("Test vector path with minimum vector length")
    void testVectorPathMinimumLength() {
        // Assume minimum vector length is 8 (typical for most systems)
        int vectorLength = 8;
        
        // Test with exactly vector length
        double[] signal = new double[vectorLength * 2];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
        }
        
        double[] filter = {0.5, 0.5};
        double[] result = GatherScatterOps.gatherPeriodicDownsampleForceVector(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(vectorLength, result.length);
    }
    
    @Test
    @Order(12)
    @DisplayName("Test vector path boundary conditions")
    void testVectorPathBoundaryConditions() {
        // Test with signal length that's not perfectly divisible by vector length
        double[] signal = new double[37]; // Odd number, not aligned
        double[] filter = {1.0/3, 1.0/3, 1.0/3};
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length) * 10;
        }
        
        assertDoesNotThrow(() -> {
            double[] result = GatherScatterOps.gatherPeriodicDownsampleForceVector(
                signal, filter, signal.length, filter.length);
            
            assertNotNull(result);
            assertEquals(18, result.length); // 37/2 = 18
        });
    }
    
    // ==========================================
    // Additional tests to increase coverage
    // ==========================================
    
    @Test
    @Order(13)
    @DisplayName("Test gather periodic downsample with small vector length")
    void testGatherPeriodicDownsampleSmallVectorLength() {
        // Test when DOUBLE_LENGTH < 4 (would use scalar path)
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        double[] filter = {0.5, 0.5};
        
        double[] result = GatherScatterOps.gatherPeriodicDownsampleForceVector(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(3, result.length);
        
        // Verify scalar calculation
        assertEquals(1.5, result[0], EPSILON);
        assertEquals(3.5, result[1], EPSILON);
        assertEquals(5.5, result[2], EPSILON);
    }
    
    @Test
    @Order(14)
    @DisplayName("Test scatter upsample with small vector length")
    void testScatterUpsampleSmallVectorLength() {
        // Test when DOUBLE_LENGTH < 4 (would use scalar path)
        double[] approx = {1.0, 2.0, 3.0};
        double[] detail = {0.1, 0.2, 0.3};
        double[] output = new double[6];
        
        GatherScatterOps.scatterUpsampleForceVector(approx, detail, output, output.length);
        
        // Verify scalar result
        assertEquals(1.0, output[0], EPSILON);
        assertEquals(0.1, output[1], EPSILON);
        assertEquals(2.0, output[2], EPSILON);
        assertEquals(0.2, output[3], EPSILON);
        assertEquals(3.0, output[4], EPSILON);
        assertEquals(0.3, output[5], EPSILON);
    }
    
    @Test
    @Order(15)
    @DisplayName("Test gather strided with large stride")
    void testGatherStridedLargeStride() {
        double[] signal = new double[100];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * 2.0;
        }
        
        // Stride > 8 should use scalar path
        double[] result = GatherScatterOps.gatherStridedForceVector(signal, 5, 10, 9);
        
        assertNotNull(result);
        assertEquals(9, result.length);
        
        for (int i = 0; i < 9; i++) {
            assertEquals(signal[5 + i * 10], result[i], EPSILON);
        }
    }
    
    @Test
    @Order(16)
    @DisplayName("Test gather compressed with empty mask")
    void testGatherCompressedEmptyMask() {
        double[] signal = new double[16];
        boolean[] mask = new boolean[16]; // All false
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
        }
        
        double[] result = GatherScatterOps.gatherCompressedForceVector(signal, mask);
        
        assertNotNull(result);
        assertEquals(0, result.length);
    }
    
    @Test
    @Order(17)
    @DisplayName("Test gather compressed with all true mask")
    void testGatherCompressedAllTrueMask() {
        double[] signal = new double[16];
        boolean[] mask = new boolean[16];
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * 3.5;
            mask[i] = true;
        }
        
        double[] result = GatherScatterOps.gatherCompressedForceVector(signal, mask);
        
        assertNotNull(result);
        assertEquals(16, result.length);
        
        for (int i = 0; i < 16; i++) {
            assertEquals(signal[i], result[i], EPSILON);
        }
    }
    
    @Test
    @Order(18)
    @DisplayName("Test batch gather with single signal")
    void testBatchGatherSingleSignal() {
        double[][] signals = new double[1][20];
        int[] indices = {0, 5, 10, 15, 19};
        double[][] results = new double[1][5];
        
        for (int i = 0; i < 20; i++) {
            signals[0][i] = Math.exp(i * 0.1);
        }
        
        GatherScatterOps.batchGatherForceVector(signals, indices, results, 5);
        
        for (int i = 0; i < 5; i++) {
            assertEquals(signals[0][indices[i]], results[0][i], EPSILON);
        }
    }
    
    @Test
    @Order(19)
    @DisplayName("Test gather periodic downsample with very long filter")
    void testGatherPeriodicDownsampleVeryLongFilter() {
        double[] signal = new double[64];
        double[] filter = new double[16]; // Long filter
        
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.cos(4 * Math.PI * i / signal.length);
        }
        
        for (int i = 0; i < filter.length; i++) {
            filter[i] = 1.0 / filter.length; // Averaging filter
        }
        
        double[] result = GatherScatterOps.gatherPeriodicDownsampleForceVector(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(32, result.length);
    }
    
    @Test
    @Order(20)
    @DisplayName("Test scatter upsample with maximum size")
    void testScatterUpsampleMaxSize() {
        int halfSize = 128;
        double[] approx = new double[halfSize];
        double[] detail = new double[halfSize];
        double[] output = new double[halfSize * 2];
        
        for (int i = 0; i < halfSize; i++) {
            approx[i] = Math.sin(2 * Math.PI * i / halfSize);
            detail[i] = Math.cos(2 * Math.PI * i / halfSize);
        }
        
        GatherScatterOps.scatterUpsampleForceVector(approx, detail, output, output.length);
        
        // Verify interleaving
        for (int i = 0; i < halfSize; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
    }
    
    @Test
    @Order(21)
    @DisplayName("Test gather strided with zero offset")
    void testGatherStridedZeroOffset() {
        double[] signal = new double[50];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * i;
        }
        
        double[] result = GatherScatterOps.gatherStridedForceVector(signal, 0, 3, 15);
        
        assertNotNull(result);
        assertEquals(15, result.length);
        
        for (int i = 0; i < 15; i++) {
            assertEquals(signal[i * 3], result[i], EPSILON);
        }
    }
    
    // ==========================================
    // Additional tests for public method coverage
    // ==========================================
    
    @Test
    @Order(22)
    @DisplayName("Test public gatherPeriodicDownsample method extensively")
    void testPublicGatherPeriodicDownsampleExtensive() {
        // Test multiple sizes and filters to increase coverage
        int[] sizes = {8, 16, 32, 64, 128, 256};
        double[][] filters = {
            {0.5, 0.5},
            {0.25, 0.5, 0.25},
            {0.125, 0.25, 0.25, 0.25, 0.125},
            {1.0/6, 1.0/6, 1.0/6, 1.0/6, 1.0/6, 1.0/6}
        };
        
        for (int size : sizes) {
            double[] signal = new double[size];
            for (int i = 0; i < size; i++) {
                signal[i] = Math.sin(2 * Math.PI * i / size) + 
                           0.5 * Math.cos(4 * Math.PI * i / size);
            }
            
            for (double[] filter : filters) {
                double[] result = GatherScatterOps.gatherPeriodicDownsample(
                    signal, filter, signal.length, filter.length);
                
                assertNotNull(result);
                assertEquals(size / 2, result.length);
                
                // Verify no NaN or Infinity
                for (double val : result) {
                    assertFalse(Double.isNaN(val));
                    assertFalse(Double.isInfinite(val));
                }
            }
        }
    }
    
    @Test
    @Order(23)
    @DisplayName("Test public scatterUpsample method extensively")
    void testPublicScatterUpsampleExtensive() {
        // Test multiple sizes to increase coverage
        int[] halfSizes = {4, 8, 16, 32, 64, 128};
        
        for (int halfSize : halfSizes) {
            double[] approx = new double[halfSize];
            double[] detail = new double[halfSize];
            double[] output = new double[halfSize * 2];
            
            // Initialize with different patterns
            for (int i = 0; i < halfSize; i++) {
                approx[i] = Math.exp(-i * 0.1) * Math.cos(Math.PI * i / halfSize);
                detail[i] = Math.exp(-i * 0.1) * Math.sin(Math.PI * i / halfSize);
            }
            
            GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
            
            // Verify interleaving
            for (int i = 0; i < halfSize; i++) {
                assertEquals(approx[i], output[2 * i], EPSILON);
                assertEquals(detail[i], output[2 * i + 1], EPSILON);
            }
        }
    }
    
    @Test
    @Order(24)
    @DisplayName("Test public batchGather method extensively")
    void testPublicBatchGatherExtensive() {
        // Test with various batch sizes and signal lengths
        int[] batchSizes = {1, 2, 4, 8, 16, 32, 64};
        int[] signalLengths = {32, 64, 128, 256};
        
        for (int batchSize : batchSizes) {
            for (int signalLength : signalLengths) {
                double[][] signals = new double[batchSize][signalLength];
                int numIndices = Math.min(20, signalLength / 2);
                int[] indices = new int[numIndices];
                double[][] results = new double[batchSize][numIndices];
                
                // Initialize signals with unique patterns
                for (int b = 0; b < batchSize; b++) {
                    for (int i = 0; i < signalLength; i++) {
                        signals[b][i] = b * 1000.0 + i + Math.sin(2 * Math.PI * i / signalLength);
                    }
                }
                
                // Create indices
                for (int i = 0; i < numIndices; i++) {
                    indices[i] = (i * signalLength) / (numIndices + 1);
                }
                
                GatherScatterOps.batchGather(signals, indices, results, numIndices);
                
                // Verify results
                for (int b = 0; b < batchSize; b++) {
                    for (int i = 0; i < numIndices; i++) {
                        assertEquals(signals[b][indices[i]], results[b][i], EPSILON);
                    }
                }
            }
        }
    }
    
    @Test
    @Order(25)
    @DisplayName("Test public gatherStrided method with various strides")
    void testPublicGatherStridedVarious() {
        double[] signal = new double[512];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * 0.1 + Math.cos(i * 0.05);
        }
        
        // Test various strides including > 8
        int[] strides = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 15, 20};
        int[] offsets = {0, 1, 2, 3, 5, 8, 13};
        
        for (int stride : strides) {
            for (int offset : offsets) {
                if (offset + stride * 10 <= signal.length) {
                    double[] result = GatherScatterOps.gatherStrided(signal, offset, stride, 10);
                    
                    assertNotNull(result);
                    assertEquals(10, result.length);
                    
                    for (int i = 0; i < 10; i++) {
                        assertEquals(signal[offset + i * stride], result[i], EPSILON);
                    }
                }
            }
        }
    }
    
    @Test
    @Order(26)
    @DisplayName("Test public gatherCompressed with various patterns")
    void testPublicGatherCompressedPatterns() {
        int[] sizes = {32, 64, 128, 256, 512};
        
        for (int size : sizes) {
            double[] signal = new double[size];
            boolean[] mask = new boolean[size];
            
            // Initialize signal
            for (int i = 0; i < size; i++) {
                signal[i] = Math.sqrt(i) * Math.sin(i * 0.1);
            }
            
            // Test different mask patterns
            // Pattern 1: Every nth element
            for (int n = 2; n <= 10; n++) {
                for (int i = 0; i < size; i++) {
                    mask[i] = (i % n == 0);
                }
                
                double[] result = GatherScatterOps.gatherCompressed(signal, mask);
                assertNotNull(result);
                
                // Verify result
                int idx = 0;
                for (int i = 0; i < size; i++) {
                    if (mask[i]) {
                        assertEquals(signal[i], result[idx++], EPSILON);
                    }
                }
                assertEquals(idx, result.length);
            }
            
            // Pattern 2: Block patterns
            for (int blockSize = 8; blockSize <= 32; blockSize *= 2) {
                for (int i = 0; i < size; i++) {
                    mask[i] = (i / blockSize) % 2 == 0;
                }
                
                double[] result = GatherScatterOps.gatherCompressed(signal, mask);
                assertNotNull(result);
            }
        }
    }
}
