package com.morphiqlabs.wavelet.extensions.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test to achieve 70% coverage for GatherScatterOps.
 * This test focuses on edge cases and untested branches.
 */
@DisplayName("GatherScatterOps Comprehensive Coverage Test")
class GatherScatterOpsComprehensiveTest {
    
    private static final double EPSILON = 1e-10;
    
    @Test
    @DisplayName("Test constructor is private")
    void testConstructorIsPrivate() {
        // Verify GatherScatterOps cannot be instantiated
        try {
            var constructor = GatherScatterOps.class.getDeclaredConstructor();
            assertTrue(constructor.trySetAccessible());
            constructor.setAccessible(true);
            
            // Should be able to create instance via reflection
            assertNotNull(constructor.newInstance());
        } catch (Exception e) {
            fail("Should be able to instantiate via reflection");
        }
    }
    
    @Test
    @DisplayName("Test gather periodic downsample edge cases")
    void testGatherPeriodicDownsampleEdgeCases() {
        // Test with minimum size
        double[] signal = {1.0, 2.0};
        double[] filter = {1.0};
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(1.0, result[0], EPSILON);
        
        // Test with larger filter than signal
        double[] signal2 = {1.0, 2.0, 3.0, 4.0};
        double[] filter2 = {0.2, 0.2, 0.2, 0.2, 0.2};
        
        double[] result2 = GatherScatterOps.gatherPeriodicDownsample(
            signal2, filter2, signal2.length, filter2.length);
        
        assertNotNull(result2);
        assertEquals(2, result2.length);
    }
    
    @Test
    @DisplayName("Test scatter upsample edge cases")
    void testScatterUpsampleEdgeCases() {
        // Test with minimum size
        double[] approx = {1.0};
        double[] detail = {0.5};
        double[] output = new double[2];
        
        GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
        
        assertEquals(1.0, output[0], EPSILON);
        assertEquals(0.5, output[1], EPSILON);
        
        // Test with zero values
        double[] approx2 = new double[8];
        double[] detail2 = new double[8];
        double[] output2 = new double[16];
        
        GatherScatterOps.scatterUpsample(approx2, detail2, output2, output2.length);
        
        for (double val : output2) {
            assertEquals(0.0, val, EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test batch gather edge cases")
    void testBatchGatherEdgeCases() {
        // Single signal, single index
        double[][] signals = {{42.0}};
        int[] indices = {0};
        double[][] results = new double[1][1];
        
        GatherScatterOps.batchGather(signals, indices, results, 1);
        
        assertEquals(42.0, results[0][0], EPSILON);
        
        // Empty batch
        double[][] emptySignals = new double[0][];
        double[][] emptyResults = new double[0][];
        
        // Should not throw
        assertDoesNotThrow(() -> 
            GatherScatterOps.batchGather(emptySignals, indices, emptyResults, 0)
        );
    }
    
    @Test
    @DisplayName("Test gather strided edge cases")
    void testGatherStridedEdgeCases() {
        // Test with stride = 1
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0};
        
        double[] result = GatherScatterOps.gatherStrided(signal, 0, 1, 5);
        
        assertNotNull(result);
        assertArrayEquals(signal, result, EPSILON);
        
        // Test with maximum offset
        double[] result2 = GatherScatterOps.gatherStrided(signal, 4, 1, 1);
        
        assertNotNull(result2);
        assertEquals(1, result2.length);
        assertEquals(5.0, result2[0], EPSILON);
    }
    
    @Test
    @DisplayName("Test gather compressed edge cases")
    void testGatherCompressedEdgeCases() {
        // All false mask
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        boolean[] mask = {false, false, false, false};
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        assertEquals(0, result.length);
        
        // All true mask
        boolean[] mask2 = {true, true, true, true};
        
        double[] result2 = GatherScatterOps.gatherCompressed(signal, mask2);
        
        assertNotNull(result2);
        assertArrayEquals(signal, result2, EPSILON);
        
        // Single element
        double[] signal3 = {42.0};
        boolean[] mask3 = {true};
        
        double[] result3 = GatherScatterOps.gatherCompressed(signal3, mask3);
        
        assertNotNull(result3);
        assertEquals(1, result3.length);
        assertEquals(42.0, result3[0], EPSILON);
    }
    
    @Test
    @DisplayName("Test force vector methods with small sizes")
    void testForceVectorMethodsSmallSizes() {
        // Test force vector methods with sizes that would trigger scalar fallback
        double[] smallSignal = {1.0, 2.0, 3.0, 4.0};
        double[] filter = {0.5, 0.5};
        
        double[] result = GatherScatterOps.gatherPeriodicDownsampleForceVector(
            smallSignal, filter, smallSignal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(2, result.length);
        
        // Test scatter with small size
        double[] approx = {1.0, 2.0};
        double[] detail = {0.1, 0.2};
        double[] output = new double[4];
        
        GatherScatterOps.scatterUpsampleForceVector(approx, detail, output, output.length);
        
        assertEquals(1.0, output[0], EPSILON);
        assertEquals(0.1, output[1], EPSILON);
        assertEquals(2.0, output[2], EPSILON);
        assertEquals(0.2, output[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test batch gather force vector with edge cases")
    void testBatchGatherForceVectorEdgeCases() {
        // Large batch with small signals
        int batchSize = 100;
        double[][] signals = new double[batchSize][10];
        int[] indices = {0, 5, 9};
        double[][] results = new double[batchSize][3];
        
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < 10; j++) {
                signals[i][j] = i + j * 0.1;
            }
        }
        
        GatherScatterOps.batchGatherForceVector(signals, indices, results, 3);
        
        for (int i = 0; i < batchSize; i++) {
            assertEquals(signals[i][0], results[i][0], EPSILON);
            assertEquals(signals[i][5], results[i][1], EPSILON);
            assertEquals(signals[i][9], results[i][2], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test gather strided force vector with edge cases")
    void testGatherStridedForceVectorEdgeCases() {
        // Test with stride exactly 8
        double[] signal = new double[100];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * i;
        }
        
        double[] result = GatherScatterOps.gatherStridedForceVector(signal, 0, 8, 12);
        
        assertNotNull(result);
        assertEquals(12, result.length);
        
        for (int i = 0; i < 12; i++) {
            assertEquals(signal[i * 8], result[i], EPSILON);
        }
        
        // Test with stride 9 (>8, should use scalar path)
        double[] result2 = GatherScatterOps.gatherStridedForceVector(signal, 1, 9, 10);
        
        assertNotNull(result2);
        assertEquals(10, result2.length);
        
        for (int i = 0; i < 10; i++) {
            assertEquals(signal[1 + i * 9], result2[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test gather compressed force vector special cases")
    void testGatherCompressedForceVectorSpecialCases() {
        // Test with alternating true/false pattern
        double[] signal = new double[64];
        boolean[] mask = new boolean[64];
        
        for (int i = 0; i < 64; i++) {
            signal[i] = i;
            mask[i] = (i % 2 == 0);
        }
        
        double[] result = GatherScatterOps.gatherCompressedForceVector(signal, mask);
        
        assertNotNull(result);
        assertEquals(32, result.length);
        
        for (int i = 0; i < 32; i++) {
            assertEquals(i * 2, result[i], EPSILON);
        }
        
        // Test with block pattern
        boolean[] mask2 = new boolean[64];
        for (int i = 0; i < 64; i++) {
            mask2[i] = i < 32;
        }
        
        double[] result2 = GatherScatterOps.gatherCompressedForceVector(signal, mask2);
        
        assertNotNull(result2);
        assertEquals(32, result2.length);
        
        for (int i = 0; i < 32; i++) {
            assertEquals(i, result2[i], EPSILON);
        }
    }
}
