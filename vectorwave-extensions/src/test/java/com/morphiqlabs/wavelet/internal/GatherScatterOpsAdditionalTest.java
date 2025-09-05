package com.morphiqlabs.wavelet.extensions.internal;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional tests to increase coverage for GatherScatterOps to 70%.
 * Focuses on edge cases and complex scenarios.
 */
@DisplayName("GatherScatterOps Additional Coverage Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GatherScatterOpsAdditionalTest {
    
    private static final double EPSILON = 1e-10;
    private static final Random random = new Random(42);
    
    // ==========================================
    // Tests for scalar paths with edge cases
    // ==========================================
    
    @Test
    @Order(1)
    @DisplayName("Test gather periodic downsample with prime-sized signals")
    void testGatherPeriodicDownsamplePrimeSizes() {
        int[] primeSizes = {14, 22, 26, 34, 38, 46, 58, 62, 74, 82, 86, 94};
        
        for (int size : primeSizes) {
            double[] signal = new double[size];
            for (int i = 0; i < size; i++) {
                signal[i] = Math.sin(2 * Math.PI * i / size) + 
                           0.3 * Math.cos(6 * Math.PI * i / size);
            }
            
            // Test with various filter lengths
            double[] filter2 = {0.7071, 0.7071};
            double[] filter3 = {0.5774, 0.5774, 0.5774};
            double[] filter5 = {0.4472, 0.4472, 0.4472, 0.4472, 0.4472};
            
            double[] result2 = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter2, signal.length, filter2.length);
            double[] result3 = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter3, signal.length, filter3.length);
            double[] result5 = GatherScatterOps.gatherPeriodicDownsample(
                signal, filter5, signal.length, filter5.length);
            
            assertEquals(size / 2, result2.length);
            assertEquals(size / 2, result3.length);
            assertEquals(size / 2, result5.length);
            
            // Verify results are finite
            for (double val : result2) assertFalse(Double.isNaN(val) || Double.isInfinite(val));
            for (double val : result3) assertFalse(Double.isNaN(val) || Double.isInfinite(val));
            for (double val : result5) assertFalse(Double.isNaN(val) || Double.isInfinite(val));
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("Test scatter upsample with random data patterns")
    void testScatterUpsampleRandomPatterns() {
        int[] sizes = {10, 20, 30, 40, 50, 100, 200};
        
        for (int halfSize : sizes) {
            double[] approx = new double[halfSize];
            double[] detail = new double[halfSize];
            double[] output = new double[halfSize * 2];
            
            // Fill with random data
            for (int i = 0; i < halfSize; i++) {
                approx[i] = random.nextGaussian();
                detail[i] = random.nextGaussian() * 0.5;
            }
            
            GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
            
            // Verify interleaving pattern
            for (int i = 0; i < halfSize; i++) {
                assertEquals(approx[i], output[2 * i], EPSILON);
                assertEquals(detail[i], output[2 * i + 1], EPSILON);
            }
        }
    }
    
    @Test
    @Order(3)
    @DisplayName("Test batch gather with irregular batch sizes")
    void testBatchGatherIrregularSizes() {
        int[] batchSizes = {1, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47};
        int signalLength = 100;
        
        for (int batchSize : batchSizes) {
            double[][] signals = new double[batchSize][signalLength];
            int[] indices = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90, 99};
            double[][] results = new double[batchSize][indices.length];
            
            // Initialize signals with batch-specific patterns
            for (int b = 0; b < batchSize; b++) {
                for (int i = 0; i < signalLength; i++) {
                    signals[b][i] = b * 1000.0 + i * Math.exp(-i * 0.01);
                }
            }
            
            GatherScatterOps.batchGather(signals, indices, results, indices.length);
            
            // Verify
            for (int b = 0; b < batchSize; b++) {
                for (int i = 0; i < indices.length; i++) {
                    assertEquals(signals[b][indices[i]], results[b][i], EPSILON);
                }
            }
        }
    }
    
    @Test
    @Order(4)
    @DisplayName("Test gather strided with maximum valid parameters")
    void testGatherStridedMaxParameters() {
        int signalSize = 1000;
        double[] signal = new double[signalSize];
        
        // Initialize with exponential decay pattern
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.exp(-i * 0.001) * Math.sin(i * 0.1);
        }
        
        // Test with maximum valid strides and offsets
        for (int stride = 1; stride <= 50; stride += 3) {
            for (int offset = 0; offset < Math.min(100, signalSize - stride * 10); offset += 7) {
                int maxCount = (signalSize - offset) / stride;
                int count = Math.min(maxCount, 50);
                
                double[] result = GatherScatterOps.gatherStrided(signal, offset, stride, count);
                
                assertNotNull(result);
                assertEquals(count, result.length);
                
                // Verify gathered values
                for (int i = 0; i < count; i++) {
                    assertEquals(signal[offset + i * stride], result[i], EPSILON,
                        String.format("Mismatch at i=%d, offset=%d, stride=%d", i, offset, stride));
                }
            }
        }
    }
    
    @Test
    @Order(5)
    @DisplayName("Test gather compressed with sparse patterns")
    void testGatherCompressedSparsePatterns() {
        int[] sizes = {100, 200, 500, 1000};
        double[] sparsity = {0.1, 0.05, 0.01, 0.001};
        
        for (int i = 0; i < sizes.length; i++) {
            int size = sizes[i];
            double sparsityLevel = sparsity[i];
            
            double[] signal = new double[size];
            boolean[] mask = new boolean[size];
            
            // Create sparse mask
            int numTrue = (int)(size * sparsityLevel);
            for (int j = 0; j < numTrue; j++) {
                int idx = random.nextInt(size);
                mask[idx] = true;
                signal[idx] = random.nextGaussian() * 100;
            }
            
            double[] result = GatherScatterOps.gatherCompressed(signal, mask);
            
            assertNotNull(result);
            
            // Count actual true values
            int actualCount = 0;
            for (boolean m : mask) {
                if (m) actualCount++;
            }
            assertEquals(actualCount, result.length);
            
            // Verify compressed values maintain order
            int idx = 0;
            for (int j = 0; j < size; j++) {
                if (mask[j]) {
                    assertEquals(signal[j], result[idx++], EPSILON);
                }
            }
        }
    }
    
    // ==========================================
    // Force vector path tests with edge cases
    // ==========================================
    
    @Test
    @Order(6)
    @DisplayName("Test force vector gather periodic with filter longer than signal")
    void testForceVectorGatherPeriodicLongFilter() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = new double[12]; // Filter longer than signal
        
        for (int i = 0; i < filter.length; i++) {
            filter[i] = 1.0 / filter.length;
        }
        
        double[] result = GatherScatterOps.gatherPeriodicDownsampleForceVector(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        // With periodic boundary, all elements should contribute equally
        double expectedSum = 0;
        for (double s : signal) {
            expectedSum += s;
        }
        double expectedAvg = expectedSum / signal.length;
        
        // Due to periodic wrapping, each output should be close to average
        for (double val : result) {
            assertTrue(Math.abs(val - expectedAvg) < 2.0,
                "Value " + val + " too far from expected " + expectedAvg);
        }
    }
    
    @Test
    @Order(7)
    @DisplayName("Test force vector scatter with non-aligned sizes")
    void testForceVectorScatterNonAligned() {
        int[] halfSizes = {7, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47, 53, 59, 61, 67, 71};
        
        for (int halfSize : halfSizes) {
            double[] approx = new double[halfSize];
            double[] detail = new double[halfSize];
            double[] output = new double[halfSize * 2];
            
            // Fill with sequential data for easy verification
            for (int i = 0; i < halfSize; i++) {
                approx[i] = i * 2.0;
                detail[i] = i * 2.0 + 1.0;
            }
            
            GatherScatterOps.scatterUpsampleForceVector(approx, detail, output, output.length);
            
            // Verify interleaving
            for (int i = 0; i < halfSize; i++) {
                assertEquals(i * 2.0, output[2 * i], EPSILON);
                assertEquals(i * 2.0 + 1.0, output[2 * i + 1], EPSILON);
            }
        }
    }
    
    @Test
    @Order(8)
    @DisplayName("Test force vector batch gather with mixed signal lengths")
    void testForceVectorBatchGatherMixedLengths() {
        // Create signals with different lengths (padded to max)
        int maxLength = 100;
        int batchSize = 16;
        double[][] signals = new double[batchSize][maxLength];
        int[] actualLengths = new int[batchSize];
        
        // Initialize with different patterns per signal
        for (int b = 0; b < batchSize; b++) {
            actualLengths[b] = 50 + b * 3; // Varying lengths
            for (int i = 0; i < actualLengths[b]; i++) {
                signals[b][i] = b * 100.0 + i + Math.sin(i * 0.1 + b);
            }
        }
        
        // Gather from valid indices only
        int[] indices = {0, 5, 10, 15, 20, 25, 30, 35, 40, 45};
        double[][] results = new double[batchSize][indices.length];
        
        GatherScatterOps.batchGatherForceVector(signals, indices, results, indices.length);
        
        // Verify
        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < indices.length; i++) {
                if (indices[i] < actualLengths[b]) {
                    assertEquals(signals[b][indices[i]], results[b][i], EPSILON);
                }
            }
        }
    }
    
    @Test
    @Order(9)
    @DisplayName("Test force vector gather strided with all strides 1-20")
    void testForceVectorGatherStridedAllStrides() {
        double[] signal = new double[500];
        
        // Initialize with complex pattern
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(i * 0.05) * Math.cos(i * 0.02) + 
                       Math.exp(-i * 0.001) * Math.sin(i * 0.1);
        }
        
        // Test all strides from 1 to 20
        for (int stride = 1; stride <= 20; stride++) {
            for (int offset = 0; offset <= Math.min(10, signal.length - stride * 20); offset++) {
                int count = Math.min(20, (signal.length - offset) / stride);
                
                double[] result = GatherScatterOps.gatherStridedForceVector(
                    signal, offset, stride, count);
                
                assertNotNull(result);
                assertEquals(count, result.length);
                
                // Verify
                for (int i = 0; i < count; i++) {
                    assertEquals(signal[offset + i * stride], result[i], EPSILON,
                        String.format("Stride=%d, offset=%d, i=%d", stride, offset, i));
                }
            }
        }
    }
    
    @Test
    @Order(10)
    @DisplayName("Test force vector gather compressed with pathological patterns")
    void testForceVectorGatherCompressedPathological() {
        // Pattern 1: Single true at beginning
        double[] signal1 = new double[128];
        boolean[] mask1 = new boolean[128];
        mask1[0] = true;
        for (int i = 0; i < 128; i++) signal1[i] = i;
        
        double[] result1 = GatherScatterOps.gatherCompressedForceVector(signal1, mask1);
        assertArrayEquals(new double[]{0.0}, result1, EPSILON);
        
        // Pattern 2: Single true at end
        boolean[] mask2 = new boolean[128];
        mask2[127] = true;
        
        double[] result2 = GatherScatterOps.gatherCompressedForceVector(signal1, mask2);
        assertArrayEquals(new double[]{127.0}, result2, EPSILON);
        
        // Pattern 3: True values at vector boundaries
        boolean[] mask3 = new boolean[128];
        for (int i = 7; i < 128; i += 8) {
            mask3[i] = true;
        }
        
        double[] result3 = GatherScatterOps.gatherCompressedForceVector(signal1, mask3);
        assertEquals(16, result3.length);
        for (int i = 0; i < 16; i++) {
            assertEquals(7 + i * 8, result3[i], EPSILON);
        }
        
        // Pattern 4: Clustered true values
        boolean[] mask4 = new boolean[128];
        for (int i = 32; i < 48; i++) mask4[i] = true;
        for (int i = 80; i < 96; i++) mask4[i] = true;
        
        double[] result4 = GatherScatterOps.gatherCompressedForceVector(signal1, mask4);
        assertEquals(32, result4.length);
        
        // Verify first cluster
        for (int i = 0; i < 16; i++) {
            assertEquals(32 + i, result4[i], EPSILON);
        }
        // Verify second cluster
        for (int i = 0; i < 16; i++) {
            assertEquals(80 + i, result4[16 + i], EPSILON);
        }
    }
    
    // ==========================================
    // Performance and stress tests
    // ==========================================
    
    @ParameterizedTest
    @ValueSource(ints = {1024, 2048, 4096, 8192})
    @Order(11)
    @DisplayName("Test performance with large signals")
    void testPerformanceLargeSignals(int size) {
        double[] signal = new double[size];
        double[] filter = {0.25, 0.5, 0.25};
        
        // Initialize with realistic signal
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64) + 
                       0.5 * Math.sin(2 * Math.PI * i / 16) +
                       0.25 * Math.sin(2 * Math.PI * i / 4);
        }
        
        // Time the operation
        long start = System.nanoTime();
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        long end = System.nanoTime();
        
        assertNotNull(result);
        assertEquals(size / 2, result.length);
        
        // Verify reasonable performance (should complete in < 10ms)
        long durationMs = (end - start) / 1_000_000;
        assertTrue(durationMs < 10, "Operation took too long: " + durationMs + "ms");
    }
    
    @Test
    @Order(12)
    @DisplayName("Test extreme parameter combinations")
    void testExtremeParameterCombinations() {
        // Test with minimum sizes
        double[] tinySignal = {1.0, 2.0};
        double[] tinyFilter = {1.0};
        double[] tinyResult = GatherScatterOps.gatherPeriodicDownsample(
            tinySignal, tinyFilter, tinySignal.length, tinyFilter.length);
        assertArrayEquals(new double[]{1.0}, tinyResult, EPSILON);
        
        // Test with maximum filter size
        double[] signal = new double[32];
        double[] maxFilter = new double[31];
        for (int i = 0; i < signal.length; i++) signal[i] = i;
        for (int i = 0; i < maxFilter.length; i++) maxFilter[i] = 1.0 / maxFilter.length;
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, maxFilter, signal.length, maxFilter.length);
        assertNotNull(result);
        assertEquals(16, result.length);
    }
    
    // ==========================================
    // Method source for parameterized tests
    // ==========================================
    
    static Stream<Arguments> provideComplexScenarios() {
        return Stream.of(
            Arguments.of(16, new double[]{0.5, 0.5}, "Simple averaging"),
            Arguments.of(32, new double[]{0.25, 0.5, 0.25}, "3-tap filter"),
            Arguments.of(64, new double[]{0.125, 0.25, 0.25, 0.25, 0.125}, "5-tap filter"),
            Arguments.of(128, new double[]{1.0/7, 1.0/7, 1.0/7, 1.0/7, 1.0/7, 1.0/7, 1.0/7}, "7-tap filter"),
            Arguments.of(256, new double[]{0.1, 0.2, 0.4, 0.2, 0.1}, "Gaussian-like filter")
        );
    }
    
    @ParameterizedTest
    @MethodSource("provideComplexScenarios")
    @Order(13)
    @DisplayName("Test complex scenarios with various filters")
    void testComplexScenarios(int signalSize, double[] filter, String description) {
        double[] signal = new double[signalSize];
        
        // Create multi-frequency signal
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signalSize) +
                       0.5 * Math.sin(4 * Math.PI * i / signalSize) +
                       0.25 * Math.sin(8 * Math.PI * i / signalSize) +
                       0.125 * Math.sin(16 * Math.PI * i / signalSize);
        }
        
        // Test both public and force vector methods
        double[] publicResult = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        double[] forceVectorResult = GatherScatterOps.gatherPeriodicDownsampleForceVector(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(publicResult, description + " - public method returned null");
        assertNotNull(forceVectorResult, description + " - force vector method returned null");
        assertEquals(signalSize / 2, publicResult.length);
        assertEquals(signalSize / 2, forceVectorResult.length);
        
        // Results should be very close (allowing for floating point differences)
        for (int i = 0; i < publicResult.length; i++) {
            assertEquals(publicResult[i], forceVectorResult[i], 1e-9,
                description + " - mismatch at index " + i);
        }
    }
    
    @Test
    @Order(14)
    @DisplayName("Test getGatherScatterInfo method")
    void testGetGatherScatterInfo() {
        String info = GatherScatterOps.getGatherScatterInfo();
        
        assertNotNull(info);
        assertFalse(info.isEmpty());
        assertTrue(info.contains("Gather/Scatter"));
        assertTrue(info.contains("Available") || info.contains("Not Available"));
        
        // Call multiple times to ensure consistency
        String info2 = GatherScatterOps.getGatherScatterInfo();
        assertEquals(info, info2);
    }
    
    @Test
    @Order(15)
    @DisplayName("Test isGatherScatterAvailable method")
    void testIsGatherScatterAvailable() {
        boolean available = GatherScatterOps.isGatherScatterAvailable();
        
        // Should always return false based on current implementation
        assertFalse(available);
        
        // Verify consistency
        assertEquals(available, GatherScatterOps.isGatherScatterAvailable());
    }
}
