package com.morphiqlabs.wavelet.extensions.internal;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Final push to reach 70% coverage for GatherScatterOps.
 * This test exhaustively tests all reachable code paths.
 */
@DisplayName("GatherScatterOps Final Push Coverage Test")
class GatherScatterOpsFinalPushTest {
    
    private static final double EPSILON = 1e-10;
    private static final Random random = new Random(12345);
    
    // ==========================================
    // Stress test public methods to maximize coverage
    // ==========================================
    
    @Test
    @DisplayName("Exhaustive test of gatherPeriodicDownsample scalar path")
    void testGatherPeriodicDownsampleScalarExhaustive() {
        // Test all combinations of small signal/filter sizes
        for (int signalSize = 2; signalSize <= 20; signalSize += 2) {
            for (int filterSize = 1; filterSize <= Math.min(10, signalSize); filterSize++) {
                double[] signal = new double[signalSize];
                double[] filter = new double[filterSize];
                
                // Initialize with pattern
                for (int i = 0; i < signalSize; i++) {
                    signal[i] = i * 1.5 + Math.sin(i * 0.5);
                }
                for (int i = 0; i < filterSize; i++) {
                    filter[i] = 1.0 / filterSize;
                }
                
                double[] result = GatherScatterOps.gatherPeriodicDownsample(
                    signal, filter, signal.length, filter.length);
                
                assertNotNull(result);
                assertEquals(signalSize / 2, result.length);
                
                // Verify no NaN or Infinity
                for (double val : result) {
                    assertFalse(Double.isNaN(val));
                    assertFalse(Double.isInfinite(val));
                }
            }
        }
    }
    
    @Test
    @DisplayName("Exhaustive test of scatterUpsample scalar path")
    void testScatterUpsampleScalarExhaustive() {
        // Test various sizes
        for (int halfSize = 1; halfSize <= 50; halfSize++) {
            double[] approx = new double[halfSize];
            double[] detail = new double[halfSize];
            double[] output = new double[halfSize * 2];
            
            // Initialize with patterns
            for (int i = 0; i < halfSize; i++) {
                approx[i] = Math.cos(2 * Math.PI * i / halfSize);
                detail[i] = Math.sin(2 * Math.PI * i / halfSize);
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
    @DisplayName("Exhaustive test of batchGather scalar path")
    void testBatchGatherScalarExhaustive() {
        // Test with many different batch and signal configurations
        int[] batchSizes = {1, 2, 3, 5, 8, 13, 21, 34, 55};
        int[] signalLengths = {10, 20, 33, 47, 64, 100};
        
        for (int batchSize : batchSizes) {
            for (int signalLength : signalLengths) {
                double[][] signals = new double[batchSize][signalLength];
                
                // Create indices covering the signal
                int numIndices = Math.min(signalLength, 20);
                int[] indices = new int[numIndices];
                for (int i = 0; i < numIndices; i++) {
                    indices[i] = (i * signalLength) / numIndices;
                }
                
                double[][] results = new double[batchSize][numIndices];
                
                // Initialize signals
                for (int b = 0; b < batchSize; b++) {
                    for (int i = 0; i < signalLength; i++) {
                        signals[b][i] = b * 100.0 + i + random.nextGaussian() * 0.1;
                    }
                }
                
                GatherScatterOps.batchGather(signals, indices, results, numIndices);
                
                // Verify
                for (int b = 0; b < batchSize; b++) {
                    for (int i = 0; i < numIndices; i++) {
                        assertEquals(signals[b][indices[i]], results[b][i], EPSILON);
                    }
                }
            }
        }
    }
    
    @Test
    @DisplayName("Exhaustive test of gatherStrided")
    void testGatherStridedExhaustive() {
        double[] signal = new double[1000];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * 0.01 + Math.sin(i * 0.1);
        }
        
        // Test all combinations of parameters
        for (int stride = 1; stride <= 20; stride++) {
            for (int offset = 0; offset <= Math.min(50, signal.length - stride * 5); offset += 3) {
                for (int count = 1; count <= Math.min(50, (signal.length - offset) / stride); count += 7) {
                    double[] result = GatherScatterOps.gatherStrided(signal, offset, stride, count);
                    
                    assertNotNull(result);
                    assertEquals(count, result.length);
                    
                    // Verify values
                    for (int i = 0; i < count; i++) {
                        assertEquals(signal[offset + i * stride], result[i], EPSILON);
                    }
                }
            }
        }
    }
    
    @Test
    @DisplayName("Exhaustive test of gatherCompressed")
    void testGatherCompressedExhaustive() {
        // Test with various signal sizes and mask patterns
        int[] sizes = {16, 32, 64, 128, 256, 512};
        
        for (int size : sizes) {
            double[] signal = new double[size];
            
            // Initialize signal
            for (int i = 0; i < size; i++) {
                signal[i] = Math.exp(-i * 0.01) * Math.cos(i * 0.1);
            }
            
            // Test different mask patterns
            // Pattern 1: Random sparse
            boolean[] mask1 = new boolean[size];
            for (int i = 0; i < size; i++) {
                mask1[i] = random.nextDouble() < 0.3;
            }
            testGatherCompressedPattern(signal, mask1);
            
            // Pattern 2: Blocks
            boolean[] mask2 = new boolean[size];
            for (int i = 0; i < size; i++) {
                mask2[i] = (i / 16) % 2 == 0;
            }
            testGatherCompressedPattern(signal, mask2);
            
            // Pattern 3: Strided
            boolean[] mask3 = new boolean[size];
            for (int i = 0; i < size; i++) {
                mask3[i] = i % 3 == 0;
            }
            testGatherCompressedPattern(signal, mask3);
            
            // Pattern 4: First and last quarters
            boolean[] mask4 = new boolean[size];
            for (int i = 0; i < size; i++) {
                mask4[i] = i < size/4 || i >= 3*size/4;
            }
            testGatherCompressedPattern(signal, mask4);
        }
    }
    
    private void testGatherCompressedPattern(double[] signal, boolean[] mask) {
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
    
    // ==========================================
    // Force vector methods with extreme cases
    // ==========================================
    
    @Test
    @DisplayName("Force vector methods with boundary conditions")
    void testForceVectorBoundaryConditions() {
        // Test with signals that have special properties
        
        // All zeros
        double[] zeroSignal = new double[64];
        double[] filter = {0.5, 0.5};
        
        double[] result1 = GatherScatterOps.gatherPeriodicDownsampleForceVector(
            zeroSignal, filter, zeroSignal.length, filter.length);
        
        for (double val : result1) {
            assertEquals(0.0, val, EPSILON);
        }
        
        // All ones
        double[] oneSignal = new double[64];
        for (int i = 0; i < oneSignal.length; i++) {
            oneSignal[i] = 1.0;
        }
        
        double[] result2 = GatherScatterOps.gatherPeriodicDownsampleForceVector(
            oneSignal, filter, oneSignal.length, filter.length);
        
        for (double val : result2) {
            assertEquals(1.0, val, EPSILON); // Sum of filter = 1.0
        }
        
        // Alternating +1/-1
        double[] altSignal = new double[64];
        for (int i = 0; i < altSignal.length; i++) {
            altSignal[i] = (i % 2 == 0) ? 1.0 : -1.0;
        }
        
        double[] result3 = GatherScatterOps.gatherPeriodicDownsampleForceVector(
            altSignal, filter, altSignal.length, filter.length);
        
        // With averaging filter [0.5, 0.5], alternating signal should give 0
        for (double val : result3) {
            assertEquals(0.0, val, EPSILON);
        }
    }
    
    @Test
    @DisplayName("Force vector scatter with extreme patterns")
    void testForceVectorScatterExtreme() {
        // Test with maximum values
        int halfSize = 128;
        double[] approxMax = new double[halfSize];
        double[] detailMax = new double[halfSize];
        double[] output = new double[halfSize * 2];
        
        for (int i = 0; i < halfSize; i++) {
            approxMax[i] = Double.MAX_VALUE / 2;
            detailMax[i] = Double.MAX_VALUE / 2;
        }
        
        GatherScatterOps.scatterUpsampleForceVector(approxMax, detailMax, output, output.length);
        
        // Verify no overflow
        for (double val : output) {
            assertFalse(Double.isInfinite(val));
            assertFalse(Double.isNaN(val));
        }
        
        // Test with minimum values
        double[] approxMin = new double[halfSize];
        double[] detailMin = new double[halfSize];
        
        for (int i = 0; i < halfSize; i++) {
            approxMin[i] = -Double.MAX_VALUE / 2;
            detailMin[i] = -Double.MAX_VALUE / 2;
        }
        
        GatherScatterOps.scatterUpsampleForceVector(approxMin, detailMin, output, output.length);
        
        // Verify no underflow
        for (double val : output) {
            assertFalse(Double.isInfinite(val));
            assertFalse(Double.isNaN(val));
        }
    }
    
    @Test
    @DisplayName("Force vector batch gather with maximum batch size")
    void testForceVectorBatchGatherMaxSize() {
        // Test with large batch
        int batchSize = 256;
        int signalLength = 128;
        double[][] signals = new double[batchSize][signalLength];
        
        // Initialize with unique patterns per batch
        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < signalLength; i++) {
                signals[b][i] = b + i * 0.01 + Math.sin((b + i) * 0.1);
            }
        }
        
        // Gather every 4th element
        int[] indices = new int[32];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = i * 4;
        }
        
        double[][] results = new double[batchSize][indices.length];
        
        GatherScatterOps.batchGatherForceVector(signals, indices, results, indices.length);
        
        // Verify all batches
        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < indices.length; i++) {
                assertEquals(signals[b][indices[i]], results[b][i], EPSILON);
            }
        }
    }
    
    @Test
    @DisplayName("Force vector gather strided with all edge cases")
    void testForceVectorGatherStridedEdgeCases() {
        double[] signal = new double[1024];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * Math.PI / signal.length;
        }
        
        // Test stride = 1 (sequential access)
        double[] result1 = GatherScatterOps.gatherStridedForceVector(signal, 0, 1, 100);
        for (int i = 0; i < 100; i++) {
            assertEquals(signal[i], result1[i], EPSILON);
        }
        
        // Test maximum stride that still uses vector path (8)
        double[] result2 = GatherScatterOps.gatherStridedForceVector(signal, 0, 8, 100);
        for (int i = 0; i < 100; i++) {
            assertEquals(signal[i * 8], result2[i], EPSILON);
        }
        
        // Test with offset near end
        int offset = signal.length - 50;
        double[] result3 = GatherScatterOps.gatherStridedForceVector(signal, offset, 1, 50);
        for (int i = 0; i < 50; i++) {
            assertEquals(signal[offset + i], result3[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Force vector gather compressed with all patterns")
    void testForceVectorGatherCompressedAllPatterns() {
        int size = 512;
        double[] signal = new double[size];
        
        // Initialize with complex pattern
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(i * 0.01) * Math.cos(i * 0.02) + i * 0.001;
        }
        
        // Pattern 1: Fibonacci sequence
        boolean[] fibMask = new boolean[size];
        int a = 0, b = 1;
        while (b < size) {
            fibMask[b] = true;
            int temp = a + b;
            a = b;
            b = temp;
        }
        testCompressedPattern(signal, fibMask);
        
        // Pattern 2: Prime numbers
        boolean[] primeMask = new boolean[size];
        for (int i = 2; i < size; i++) {
            boolean isPrime = true;
            for (int j = 2; j * j <= i; j++) {
                if (i % j == 0) {
                    isPrime = false;
                    break;
                }
            }
            if (isPrime) {
                primeMask[i] = true;
            }
        }
        testCompressedPattern(signal, primeMask);
        
        // Pattern 3: Random with varying density
        for (double density : new double[]{0.01, 0.1, 0.25, 0.5, 0.75, 0.9, 0.99}) {
            boolean[] randomMask = new boolean[size];
            Random r = new Random((long)(density * 1000));
            for (int i = 0; i < size; i++) {
                randomMask[i] = r.nextDouble() < density;
            }
            testCompressedPattern(signal, randomMask);
        }
    }
    
    private void testCompressedPattern(double[] signal, boolean[] mask) {
        double[] result = GatherScatterOps.gatherCompressedForceVector(signal, mask);
        
        int expectedCount = 0;
        for (boolean m : mask) {
            if (m) expectedCount++;
        }
        
        assertEquals(expectedCount, result.length);
        
        int idx = 0;
        for (int i = 0; i < signal.length; i++) {
            if (mask[i]) {
                assertEquals(signal[i], result[idx++], EPSILON);
            }
        }
    }
    
    // ==========================================
    // Parameter source for exhaustive testing
    // ==========================================
    
    static Stream<Arguments> provideExhaustiveTestCases() {
        return Stream.of(
            // signalSize, filterSize, description
            Arguments.of(2, 1, "Minimum sizes"),
            Arguments.of(4, 2, "Small power of 2"),
            Arguments.of(8, 3, "Small with odd filter"),
            Arguments.of(16, 7, "Medium with large filter"),
            Arguments.of(32, 15, "Large with very large filter"),
            Arguments.of(64, 1, "Large with unit filter"),
            Arguments.of(128, 31, "Very large with prime filter size"),
            Arguments.of(256, 63, "Maximum practical size")
        );
    }
    
    @ParameterizedTest
    @MethodSource("provideExhaustiveTestCases")
    @DisplayName("Parameterized exhaustive test")
    void testParameterizedExhaustive(int signalSize, int filterSize, String description) {
        double[] signal = new double[signalSize];
        double[] filter = new double[filterSize];
        
        // Initialize with complex pattern
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signalSize) * 
                       Math.cos(4 * Math.PI * i / signalSize) +
                       Math.exp(-i * 0.01);
        }
        
        // Initialize filter (normalized)
        double sum = 0;
        for (int i = 0; i < filterSize; i++) {
            filter[i] = Math.exp(-Math.pow(i - filterSize/2.0, 2) / (2 * Math.pow(filterSize/4.0, 2)));
            sum += filter[i];
        }
        for (int i = 0; i < filterSize; i++) {
            filter[i] /= sum;
        }
        
        // Test both methods
        double[] publicResult = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        double[] forceResult = GatherScatterOps.gatherPeriodicDownsampleForceVector(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(publicResult, description);
        assertNotNull(forceResult, description);
        assertEquals(signalSize / 2, publicResult.length);
        assertEquals(signalSize / 2, forceResult.length);
        
        // Results should match
        assertArrayEquals(publicResult, forceResult, 1e-9, description);
    }
}
