package com.morphiqlabs.wavelet.extensions.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;

/**
 * Test suite for BatchMemoryLayout.
 * Tests batch memory management, alignment, and SIMD operations.
 */
@DisplayName("BatchMemoryLayout Test Suite")
class BatchMemoryLayoutTest {
    
    private BatchMemoryLayout layout;
    private static final double EPSILON = 1e-12;
    
    @AfterEach
    void tearDown() {
        if (layout != null) {
            layout.close();
        }
    }
    
    @Test
    @DisplayName("Test basic construction")
    void testBasicConstruction() {
        layout = new BatchMemoryLayout(4, 16);
        
        assertNotNull(layout);
        assertNotNull(layout.getInputData());
        assertNotNull(layout.getApproxData());
        assertNotNull(layout.getDetailData());
        
        // Check that padded batch size is at least as large as original
        assertTrue(layout.getPaddedBatchSize() >= 4);
        
        // Check array sizes are appropriate
        assertTrue(layout.getInputData().length >= 4 * 16);
        assertTrue(layout.getApproxData().length >= 4 * 8); // Half length for approx
        assertTrue(layout.getDetailData().length >= 4 * 8); // Half length for detail
    }
    
    @Test
    @DisplayName("Test optimized construction")
    void testOptimizedConstruction() {
        layout = BatchMemoryLayout.createOptimized(3, 32);
        
        assertNotNull(layout);
        assertTrue(layout.getPaddedBatchSize() >= 3);
        
        // Optimized layout may use larger batch size for SIMD alignment
        int paddedSize = layout.getPaddedBatchSize();
        assertTrue(paddedSize >= 3, "Padded size should accommodate original batch size");
    }
    
    @Test
    @DisplayName("Test signal loading and extraction")
    void testSignalLoadingAndExtraction() {
        layout = new BatchMemoryLayout(2, 8);
        
        // Create test signals
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0},
            {8.0, 7.0, 6.0, 5.0, 4.0, 3.0, 2.0, 1.0}
        };
        
        // Load signals
        layout.loadSignals(signals);
        
        // Verify input data was loaded correctly
        double[] inputData = layout.getInputData();
        assertNotNull(inputData);
        
        // Check first signal
        for (int i = 0; i < 8; i++) {
            assertEquals(signals[0][i], inputData[i], EPSILON, 
                        "First signal sample " + i + " should match");
        }
    }
    
    @Test
    @DisplayName("Test interleaved signal loading")
    void testInterleavedSignalLoading() {
        layout = new BatchMemoryLayout(3, 4);
        
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0},
            {5.0, 6.0, 7.0, 8.0},
            {9.0, 10.0, 11.0, 12.0}
        };
        
        // Load with interleaving
        layout.loadSignalsInterleaved(signals, true);
        
        double[] inputData = layout.getInputData();
        int paddedBatchSize = layout.getPaddedBatchSize();
        
        // Check interleaved layout: [s0[0], s1[0], s2[0], s0[1], s1[1], s2[1], ...]
        assertEquals(1.0, inputData[0], EPSILON); // s0[0]
        assertEquals(5.0, inputData[1], EPSILON); // s1[0]
        assertEquals(9.0, inputData[2], EPSILON); // s2[0]
        assertEquals(2.0, inputData[paddedBatchSize], EPSILON); // s0[1]
        assertEquals(6.0, inputData[paddedBatchSize + 1], EPSILON); // s1[1]
        assertEquals(10.0, inputData[paddedBatchSize + 2], EPSILON); // s2[1]
    }
    
    @Test
    @DisplayName("Test non-interleaved fallback")
    void testNonInterleavedFallback() {
        layout = new BatchMemoryLayout(2, 4);
        
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0},
            {5.0, 6.0, 7.0, 8.0}
        };
        
        // Load without interleaving (should fall back to standard loading)
        layout.loadSignalsInterleaved(signals, false);
        
        double[] inputData = layout.getInputData();
        
        // Should be loaded in standard layout
        for (int i = 0; i < 4; i++) {
            assertEquals(signals[0][i], inputData[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test Haar transform aligned")
    void testHaarTransformAligned() {
        layout = new BatchMemoryLayout(1, 4);
        
        double[][] signals = {{1.0, 2.0, 3.0, 4.0}};
        layout.loadSignals(signals);
        
        // Perform Haar transform
        layout.haarTransformAligned();
        
        // Extract results
        double[][] approxResults = new double[1][2];
        double[][] detailResults = new double[1][2];
        layout.extractResults(approxResults, detailResults);
        
        // Check Haar transform results (approximation and detail coefficients)
        double sqrt2Inv = 1.0 / Math.sqrt(2.0);
        
        // Expected: approx[0] = (1+2)/sqrt(2), approx[1] = (3+4)/sqrt(2)
        // Expected: detail[0] = (1-2)/sqrt(2), detail[1] = (3-4)/sqrt(2)
        assertEquals((1.0 + 2.0) * sqrt2Inv, approxResults[0][0], EPSILON);
        assertEquals((3.0 + 4.0) * sqrt2Inv, approxResults[0][1], EPSILON);
        assertEquals((1.0 - 2.0) * sqrt2Inv, detailResults[0][0], EPSILON);
        assertEquals((3.0 - 4.0) * sqrt2Inv, detailResults[0][1], EPSILON);
    }
    
    @Test
    @DisplayName("Test Haar transform interleaved")
    void testHaarTransformInterleaved() {
        layout = new BatchMemoryLayout(2, 4);
        
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0},
            {5.0, 6.0, 7.0, 8.0}
        };
        layout.loadSignalsInterleaved(signals, true);
        
        // Perform interleaved Haar transform
        layout.haarTransformInterleaved();
        
        // Extract results with interleaved extraction
        double[][] approxResults = new double[2][2];
        double[][] detailResults = new double[2][2];
        layout.extractResultsInterleaved(approxResults, detailResults);
        
        // Verify results for both signals
        double sqrt2Inv = 1.0 / Math.sqrt(2.0);
        
        // First signal
        assertEquals((1.0 + 2.0) * sqrt2Inv, approxResults[0][0], EPSILON);
        assertEquals((3.0 + 4.0) * sqrt2Inv, approxResults[0][1], EPSILON);
        
        // Second signal
        assertEquals((5.0 + 6.0) * sqrt2Inv, approxResults[1][0], EPSILON);
        assertEquals((7.0 + 8.0) * sqrt2Inv, approxResults[1][1], EPSILON);
    }
    
    @Test
    @DisplayName("Test result extraction")
    void testResultExtraction() {
        layout = new BatchMemoryLayout(2, 8);
        
        // Set up some test data in the approximation and detail arrays
        double[] approxData = layout.getApproxData();
        double[] detailData = layout.getDetailData();
        
        // Fill with test values
        for (int i = 0; i < 4; i++) {
            approxData[i] = i + 1.0; // First signal approx: 1,2,3,4
            detailData[i] = i + 5.0; // First signal detail: 5,6,7,8
            approxData[i + 4] = i + 10.0; // Second signal approx: 10,11,12,13
            detailData[i + 4] = i + 20.0; // Second signal detail: 20,21,22,23
        }
        
        // Extract results
        double[][] approxResults = new double[2][4];
        double[][] detailResults = new double[2][4];
        layout.extractResults(approxResults, detailResults);
        
        // Verify extraction
        for (int i = 0; i < 4; i++) {
            assertEquals(i + 1.0, approxResults[0][i], EPSILON);
            assertEquals(i + 5.0, detailResults[0][i], EPSILON);
            assertEquals(i + 10.0, approxResults[1][i], EPSILON);
            assertEquals(i + 20.0, detailResults[1][i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test layout info")
    void testLayoutInfo() {
        layout = new BatchMemoryLayout(3, 16);
        
        String info = layout.getLayoutInfo();
        assertNotNull(info);
        assertFalse(info.isEmpty());
        
        // Should contain key information
        assertTrue(info.contains("3"), "Should contain original batch size");
        assertTrue(info.contains("16"), "Should contain signal length");
        assertTrue(info.contains("MB"), "Should contain memory usage");
    }
    
    @Test
    @DisplayName("Test array access methods")
    void testArrayAccessMethods() {
        layout = new BatchMemoryLayout(2, 8);
        
        double[] inputData = layout.getInputData();
        double[] approxData = layout.getApproxData();
        double[] detailData = layout.getDetailData();
        
        assertNotNull(inputData);
        assertNotNull(approxData);
        assertNotNull(detailData);
        
        // Arrays should be writable
        inputData[0] = 42.0;
        assertEquals(42.0, inputData[0], EPSILON);
        
        approxData[0] = 3.14;
        assertEquals(3.14, approxData[0], EPSILON);
        
        detailData[0] = 2.71;
        assertEquals(2.71, detailData[0], EPSILON);
    }
    
    @Test
    @DisplayName("Test resource cleanup")
    void testResourceCleanup() {
        layout = new BatchMemoryLayout(4, 16);
        
        // Should not throw when closing
        assertDoesNotThrow(() -> layout.close());
        
        // Should be safe to close multiple times
        assertDoesNotThrow(() -> layout.close());
    }
    
    @Test
    @DisplayName("Test memory alignment properties")
    void testMemoryAlignmentProperties() {
        layout = new BatchMemoryLayout(7, 13); // Non-power-of-2 sizes
        
        // Padded batch size should be aligned to vector boundaries
        int paddedSize = layout.getPaddedBatchSize();
        assertTrue(paddedSize >= 7, "Padded size should accommodate original batch");
        
        // Arrays should be properly sized
        assertTrue(layout.getInputData().length >= paddedSize * 13);
        assertTrue(layout.getApproxData().length >= paddedSize * 6); // Half of 13 rounded down
        assertTrue(layout.getDetailData().length >= paddedSize * 6);
    }
    
    @Test
    @DisplayName("Test large batch handling")
    void testLargeBatchHandling() {
        layout = new BatchMemoryLayout(100, 512);
        
        assertNotNull(layout);
        assertTrue(layout.getPaddedBatchSize() >= 100);
        
        // Should handle large batches without issues
        double[][] signals = new double[100][512];
        Random rand = new Random(42);
        
        // Fill with random data
        for (int i = 0; i < 100; i++) {
            for (int j = 0; j < 512; j++) {
                signals[i][j] = rand.nextGaussian();
            }
        }
        
        assertDoesNotThrow(() -> layout.loadSignals(signals));
        
        // Should be able to extract results
        double[][] approxResults = new double[100][256];
        double[][] detailResults = new double[100][256];
        assertDoesNotThrow(() -> layout.extractResults(approxResults, detailResults));
    }
    
    @Test
    @DisplayName("Test edge case with minimum sizes")
    void testMinimumSizes() {
        layout = new BatchMemoryLayout(1, 2);
        
        assertNotNull(layout);
        assertTrue(layout.getPaddedBatchSize() >= 1);
        
        double[][] signals = {{1.0, 2.0}};
        assertDoesNotThrow(() -> layout.loadSignals(signals));
        
        double[][] approxResults = new double[1][1];
        double[][] detailResults = new double[1][1];
        assertDoesNotThrow(() -> layout.extractResults(approxResults, detailResults));
    }
    
    @Test
    @DisplayName("Test performance characteristics")
    void testPerformanceCharacteristics() {
        // Test that operations complete reasonably quickly
        long startTime = System.nanoTime();
        
        layout = new BatchMemoryLayout(32, 1024);
        
        double[][] signals = new double[32][1024];
        Random rand = new Random(42);
        
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 1024; j++) {
                signals[i][j] = rand.nextGaussian();
            }
        }
        
        layout.loadSignals(signals);
        layout.haarTransformAligned();
        
        double[][] approxResults = new double[32][512];
        double[][] detailResults = new double[32][512];
        layout.extractResults(approxResults, detailResults);
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        // Should complete within reasonable time (generous limit for CI environments)
        assertTrue(durationMs < 1000, "Batch operations should complete within 1 second");
    }
    
    @Test
    @DisplayName("Test resource close exception handling")
    void testResourceCloseExceptionHandling() {
        layout = new BatchMemoryLayout(2, 8);
        
        // Close once normally
        assertDoesNotThrow(() -> layout.close());
        
        // Close again (should handle null resources gracefully)
        assertDoesNotThrow(() -> layout.close());
    }
    
    @Test
    @DisplayName("Test small batch optimization")
    void testSmallBatchOptimization() {
        // Test with very small batch that should trigger padding
        layout = BatchMemoryLayout.createOptimized(1, 8);
        
        assertNotNull(layout);
        
        // Should pad for better SIMD performance
        int paddedSize = layout.getPaddedBatchSize();
        assertTrue(paddedSize >= 1, "Should accommodate original batch size");
        
        // Test operations work with padded layout
        double[][] signals = {{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0}};
        assertDoesNotThrow(() -> layout.loadSignals(signals));
        assertDoesNotThrow(() -> layout.haarTransformAligned());
        
        double[][] approxResults = new double[1][4];
        double[][] detailResults = new double[1][4];
        assertDoesNotThrow(() -> layout.extractResults(approxResults, detailResults));
    }
    
    @Test
    @DisplayName("Test complex interleaved operations")
    void testComplexInterleavedOperations() {
        // Use a batch size that's not a power of 2 and test full interleaved workflow
        layout = new BatchMemoryLayout(5, 16);
        
        double[][] signals = new double[5][16];
        Random rand = new Random(123);
        
        // Fill with test data
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 16; j++) {
                signals[i][j] = rand.nextGaussian();
            }
        }
        
        // Full interleaved workflow
        layout.loadSignalsInterleaved(signals, true);
        layout.haarTransformInterleaved();
        
        double[][] approxResults = new double[5][8];
        double[][] detailResults = new double[5][8];
        layout.extractResultsInterleaved(approxResults, detailResults);
        
        // Results should be valid (non-zero for non-zero input)
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 8; j++) {
                assertTrue(Double.isFinite(approxResults[i][j]), "Approx results should be finite");
                assertTrue(Double.isFinite(detailResults[i][j]), "Detail results should be finite");
            }
        }
    }
    
    @Test
    @DisplayName("Test memory layout with odd signal lengths")
    void testOddSignalLengths() {
        // Test with odd signal length (should handle gracefully)
        layout = new BatchMemoryLayout(3, 7);
        
        assertNotNull(layout);
        
        double[][] signals = new double[3][7];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 7; j++) {
                signals[i][j] = i * 7 + j;
            }
        }
        
        assertDoesNotThrow(() -> layout.loadSignals(signals));
        
        // Since signal length is odd, transform operations might behave differently
        // but should not crash
        assertDoesNotThrow(() -> layout.haarTransformAligned());
    }
    
    @Test
    @DisplayName("Test array bounds safety")
    void testArrayBoundsSafety() {
        layout = new BatchMemoryLayout(2, 4);
        
        double[][] signals = {{1, 2, 3, 4}, {5, 6, 7, 8}};
        layout.loadSignals(signals);
        
        // Get direct access to arrays
        double[] inputData = layout.getInputData();
        double[] approxData = layout.getApproxData();
        double[] detailData = layout.getDetailData();
        
        // Verify we can safely write to the arrays within bounds
        assertTrue(inputData.length >= 8, "Input array should be large enough");
        assertTrue(approxData.length >= 4, "Approx array should be large enough");
        assertTrue(detailData.length >= 4, "Detail array should be large enough");
        
        // Test safe access
        inputData[0] = 99.0;
        assertEquals(99.0, inputData[0], EPSILON);
    }
}
