package com.morphiqlabs.wavelet.extensions.internal;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class that uses the new test mode functionality to enable
 * vector paths in GatherScatterOps for testing.
 */
@DisplayName("GatherScatterOps Test Mode Coverage Test")
class GatherScatterOpsTestModeTest {
    
    private static final double EPSILON = 1e-10;
    private static final Random random = new Random(42);
    
    @BeforeEach
    void setUp() {
        // Clear any previous test mode
        GatherScatterOps.clearTestMode();
    }
    
    @AfterEach
    void tearDown() {
        // Always clear test mode after each test
        GatherScatterOps.clearTestMode();
    }
    
    @Test
    @DisplayName("Test setTestMode and clearTestMode")
    void testTestModeConfiguration() {
        // Initially should use platform detection
        String initialInfo = GatherScatterOps.getGatherScatterInfo();
        assertNotNull(initialInfo);
        
        // Enable test mode with gather/scatter available
        GatherScatterOps.setTestMode(true);
        String enabledInfo = GatherScatterOps.getGatherScatterInfo();
        assertTrue(enabledInfo.contains("Available"));
        assertTrue(GatherScatterOps.isGatherScatterAvailable());
        
        // Disable gather/scatter in test mode
        GatherScatterOps.setTestMode(false);
        String disabledInfo = GatherScatterOps.getGatherScatterInfo();
        assertTrue(disabledInfo.contains("Not Available"));
        assertFalse(GatherScatterOps.isGatherScatterAvailable());
        
        // Clear test mode
        GatherScatterOps.clearTestMode();
        String clearedInfo = GatherScatterOps.getGatherScatterInfo();
        assertEquals(initialInfo, clearedInfo);
    }
    
    @Test
    @DisplayName("Test gatherPeriodicDownsample with vector path enabled")
    void testGatherPeriodicDownsampleVectorEnabled() {
        GatherScatterOps.setTestMode(true);
        
        double[] signal = new double[64];
        double[] filter = {0.25, 0.25, 0.25, 0.25}; // Moving average
        
        // Initialize signal with sine wave
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length);
        }
        
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(32, result.length);
        
        // Verify values are reasonable
        for (double val : result) {
            assertTrue(Math.abs(val) <= 1.0 + EPSILON);
            assertFalse(Double.isNaN(val));
            assertFalse(Double.isInfinite(val));
        }
    }
    
    @Test
    @DisplayName("Test scatterUpsample with vector path enabled")
    void testScatterUpsampleVectorEnabled() {
        GatherScatterOps.setTestMode(true);
        
        int halfSize = 32;
        double[] approx = new double[halfSize];
        double[] detail = new double[halfSize];
        double[] output = new double[halfSize * 2];
        
        // Initialize with test pattern
        for (int i = 0; i < halfSize; i++) {
            approx[i] = i;
            detail[i] = -i;
        }
        
        GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
        
        // Verify interleaving
        for (int i = 0; i < halfSize; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test batchGather with vector path enabled")
    void testBatchGatherVectorEnabled() {
        GatherScatterOps.setTestMode(true);
        
        int batchSize = 8;
        int signalLength = 100;
        double[][] signals = new double[batchSize][signalLength];
        
        // Initialize signals
        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < signalLength; i++) {
                signals[b][i] = b * 1000 + i;
            }
        }
        
        // Create indices
        int[] indices = {0, 10, 20, 30, 40, 50, 60, 70, 80, 90};
        double[][] results = new double[batchSize][indices.length];
        
        GatherScatterOps.batchGather(signals, indices, results, indices.length);
        
        // Verify
        for (int b = 0; b < batchSize; b++) {
            for (int i = 0; i < indices.length; i++) {
                assertEquals(signals[b][indices[i]], results[b][i], EPSILON);
            }
        }
    }
    
    @Test
    @DisplayName("Test gatherStrided with vector path enabled")
    void testGatherStridedVectorEnabled() {
        GatherScatterOps.setTestMode(true);
        
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * i; // Quadratic pattern
        }
        
        // Test various strides
        for (int stride = 1; stride <= 8; stride++) {
            int count = 20;
            int offset = 5;
            
            double[] result = GatherScatterOps.gatherStrided(signal, offset, stride, count);
            
            assertNotNull(result);
            assertEquals(count, result.length);
            
            // Verify values
            for (int i = 0; i < count; i++) {
                assertEquals(signal[offset + i * stride], result[i], EPSILON);
            }
        }
    }
    
    @Test
    @DisplayName("Test gatherCompressed with vector path enabled")
    void testGatherCompressedVectorEnabled() {
        GatherScatterOps.setTestMode(true);
        
        int size = 128;
        double[] signal = new double[size];
        boolean[] mask = new boolean[size];
        
        // Initialize signal and mask
        int trueCount = 0;
        for (int i = 0; i < size; i++) {
            signal[i] = Math.exp(-i * 0.01);
            mask[i] = (i % 3 == 0 || i % 5 == 0); // Fizz-buzz pattern
            if (mask[i]) trueCount++;
        }
        
        double[] result = GatherScatterOps.gatherCompressed(signal, mask);
        
        assertNotNull(result);
        assertEquals(trueCount, result.length);
        
        // Verify compressed values
        int idx = 0;
        for (int i = 0; i < size; i++) {
            if (mask[i]) {
                assertEquals(signal[i], result[idx++], EPSILON);
            }
        }
    }
    
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("Test all public methods with test mode enabled/disabled")
    void testAllMethodsWithTestMode(boolean enabled) {
        GatherScatterOps.setTestMode(enabled);
        
        // Test gatherPeriodicDownsample
        double[] signal1 = new double[16];
        Arrays.fill(signal1, 1.0);
        double[] filter = {0.5, 0.5};
        double[] result1 = GatherScatterOps.gatherPeriodicDownsample(
            signal1, filter, signal1.length, filter.length);
        assertNotNull(result1);
        assertEquals(8, result1.length);
        for (double val : result1) {
            assertEquals(1.0, val, EPSILON); // Should be 1.0 for constant signal
        }
        
        // Test scatterUpsample
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.1, 0.2, 0.3, 0.4};
        double[] output = new double[8];
        GatherScatterOps.scatterUpsample(approx, detail, output, output.length);
        for (int i = 0; i < 4; i++) {
            assertEquals(approx[i], output[2 * i], EPSILON);
            assertEquals(detail[i], output[2 * i + 1], EPSILON);
        }
        
        // Test batchGather
        double[][] signals2 = {{1.0, 2.0, 3.0, 4.0}, {5.0, 6.0, 7.0, 8.0}};
        int[] indices = {0, 2};
        double[][] results2 = new double[2][2];
        GatherScatterOps.batchGather(signals2, indices, results2, 2);
        assertEquals(1.0, results2[0][0], EPSILON);
        assertEquals(3.0, results2[0][1], EPSILON);
        assertEquals(5.0, results2[1][0], EPSILON);
        assertEquals(7.0, results2[1][1], EPSILON);
        
        // Test gatherStrided
        double[] signal3 = {0.0, 1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0};
        double[] result3 = GatherScatterOps.gatherStrided(signal3, 1, 2, 3);
        assertNotNull(result3);
        assertEquals(3, result3.length);
        assertEquals(1.0, result3[0], EPSILON);
        assertEquals(3.0, result3[1], EPSILON);
        assertEquals(5.0, result3[2], EPSILON);
        
        // Test gatherCompressed
        double[] signal4 = {1.0, 2.0, 3.0, 4.0};
        boolean[] mask = {true, false, true, false};
        double[] result4 = GatherScatterOps.gatherCompressed(signal4, mask);
        assertNotNull(result4);
        assertEquals(2, result4.length);
        assertEquals(1.0, result4[0], EPSILON);
        assertEquals(3.0, result4[1], EPSILON);
    }
    
    @Test
    @DisplayName("Test vector path with large signals")
    void testVectorPathLargeSignals() {
        GatherScatterOps.setTestMode(true);
        
        // Test with large signal to ensure vector paths are exercised
        int signalSize = 1024;
        double[] signal = new double[signalSize];
        double[] filter = new double[16]; // Large filter
        
        // Initialize with complex pattern
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64) * 
                       Math.cos(2 * Math.PI * i / 128) +
                       random.nextGaussian() * 0.1;
        }
        
        // Initialize filter (Gaussian-like)
        double sum = 0;
        for (int i = 0; i < filter.length; i++) {
            filter[i] = Math.exp(-Math.pow(i - filter.length/2.0, 2) / 8.0);
            sum += filter[i];
        }
        for (int i = 0; i < filter.length; i++) {
            filter[i] /= sum; // Normalize
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
    
    @Test
    @DisplayName("Test edge cases with test mode")
    void testEdgeCasesWithTestMode() {
        GatherScatterOps.setTestMode(true);
        
        // Test minimum size
        double[] signal = {1.0, 2.0};
        double[] filter = {1.0};
        double[] result = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        assertNotNull(result);
        assertEquals(1, result.length);
        assertEquals(1.0, result[0], EPSILON);
        
        // Test empty mask
        double[] signal2 = {1.0, 2.0, 3.0, 4.0};
        boolean[] emptyMask = {false, false, false, false};
        double[] compressed = GatherScatterOps.gatherCompressed(signal2, emptyMask);
        assertNotNull(compressed);
        assertEquals(0, compressed.length);
        
        // Test all true mask
        boolean[] fullMask = {true, true, true, true};
        double[] compressed2 = GatherScatterOps.gatherCompressed(signal2, fullMask);
        assertNotNull(compressed2);
        assertArrayEquals(signal2, compressed2, EPSILON);
    }
    
    @Test
    @DisplayName("Test that test mode affects behavior")
    void testTestModeAffectsBehavior() {
        // Compare behavior with test mode on vs off
        double[] signal = new double[64];
        double[] filter = {0.5, 0.5};
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i;
        }
        
        // Test with test mode disabled
        GatherScatterOps.setTestMode(false);
        double[] resultDisabled = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        // Test with test mode enabled
        GatherScatterOps.setTestMode(true);
        double[] resultEnabled = GatherScatterOps.gatherPeriodicDownsample(
            signal, filter, signal.length, filter.length);
        
        // Results should be mathematically equivalent
        assertArrayEquals(resultDisabled, resultEnabled, EPSILON);
        
        // Info should reflect the test mode state
        assertTrue(GatherScatterOps.getGatherScatterInfo().contains("Available"));
        
        GatherScatterOps.setTestMode(false);
        assertTrue(GatherScatterOps.getGatherScatterInfo().contains("Not Available"));
    }
}
