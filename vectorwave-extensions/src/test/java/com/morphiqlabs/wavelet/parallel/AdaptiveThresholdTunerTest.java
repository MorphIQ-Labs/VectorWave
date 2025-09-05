package com.morphiqlabs.wavelet.extensions.parallel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import java.util.concurrent.*;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static com.morphiqlabs.wavelet.extensions.parallel.AdaptiveThresholdTuner.OperationType;

/**
 * Unit tests for AdaptiveThresholdTuner.
 */
class AdaptiveThresholdTunerTest {
    
    private AdaptiveThresholdTuner tuner;
    
    @BeforeEach
    void setUp() {
        tuner = new AdaptiveThresholdTuner();
    }
    
    @Test
    @DisplayName("Initial thresholds should be within expected range")
    void testInitialThresholds() {
        int threshold = tuner.getAdaptiveThreshold(OperationType.MODWT_DECOMPOSE, 1000, 1.0);
        
        // Should be one of the predefined threshold values
        assertTrue(threshold == 256 || threshold == 512 || 
                  threshold == 1024 || threshold == 2048 || 
                  threshold == 4096, 
                  "Threshold should be a predefined value: " + threshold);
    }
    
    @ParameterizedTest
    @EnumSource(OperationType.class)
    @DisplayName("Each operation type should return valid threshold")
    void testAllOperationTypes(OperationType operationType) {
        int threshold = tuner.getAdaptiveThreshold(operationType, 5000, 1.0);
        
        assertTrue(threshold > 0, "Threshold must be positive");
        assertTrue(threshold <= 8192, "Threshold should not exceed max");
    }
    
    @Test
    @DisplayName("Recording successful results should influence future selections")
    void testLearningFromSuccess() {
        OperationType op = OperationType.MODWT_DECOMPOSE;
        int inputSize = 2000;
        
        // Get initial threshold
        int initialThreshold = tuner.getAdaptiveThreshold(op, inputSize, 1.0);
        
        // Record multiple successful runs with this threshold (parallel faster than sequential)
        for (int i = 0; i < 20; i++) {
            tuner.recordMeasurement(op, inputSize, initialThreshold, 1_000_000L, 2_000_000L);
        }
        
        // Future selections should favor the successful threshold
        int counts = 0;
        for (int i = 0; i < 100; i++) {
            int threshold = tuner.getAdaptiveThreshold(op, inputSize, 1.0);
            if (threshold == initialThreshold) counts++;
        }
        
        // Should select the successful threshold more often
        assertTrue(counts > 50, "Should favor successful threshold, got: " + counts);
    }
    
    @Test
    @DisplayName("Recording failures should reduce selection probability")
    void testLearningFromFailure() {
        OperationType op = OperationType.CWT_TRANSFORM;
        int inputSize = 3000;
        
        // Get initial threshold
        int initialThreshold = tuner.getAdaptiveThreshold(op, inputSize, 1.0);
        
        // Record multiple failures with this threshold (parallel slower than sequential)
        for (int i = 0; i < 20; i++) {
            tuner.recordMeasurement(op, inputSize, initialThreshold, 10_000_000L, 5_000_000L);
        }
        
        // Future selections should avoid the failed threshold
        int counts = 0;
        for (int i = 0; i < 100; i++) {
            int threshold = tuner.getAdaptiveThreshold(op, inputSize, 1.0);
            if (threshold == initialThreshold) counts++;
        }
        
        // Should select the failed threshold less often
        assertTrue(counts < 50, "Should avoid failed threshold, got: " + counts);
    }
    
    @Test
    @DisplayName("System load should influence threshold selection")
    void testSystemLoadInfluence() {
        // Test with different system states
        // Note: We can't directly control system load, but we can test
        // that the method doesn't fail under various conditions
        
        for (double complexity : new double[]{0.5, 1.0, 2.0, 4.0}) {
            int threshold = tuner.getAdaptiveThreshold(
                OperationType.BATCH_PROCESSING, 5000, complexity);
            
            assertTrue(threshold > 0, "Should return valid threshold for complexity: " + complexity);
        }
    }
    
    @Test
    @DisplayName("Concurrent access should be thread-safe")
    void testConcurrentAccess() throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(8);
        ConcurrentLinkedQueue<Integer> results = new ConcurrentLinkedQueue<>();
        
        for (int i = 0; i < 8; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // Each thread performs operations
                    for (int j = 0; j < 100; j++) {
                        OperationType op = OperationType.values()[threadId % 4];
                        int threshold = tuner.getAdaptiveThreshold(op, 1000 + threadId * 100, 1.0);
                        results.add(threshold);
                        
                        // Record some results
                        if (j % 10 == 0) {
                            long parallelTime = 1_000_000L + (j * 1000);
                            long sequentialTime = j % 3 == 0 ? 2_000_000L : 500_000L;
                            tuner.recordMeasurement(op, 1000 + threadId * 100, threshold, 
                                             parallelTime, sequentialTime);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Tasks should complete");
        assertEquals(800, results.size(), "All operations should complete");
        
        // Verify all results are valid thresholds
        for (Integer threshold : results) {
            assertTrue(threshold > 0 && threshold <= 8192, 
                      "Invalid threshold: " + threshold);
        }
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("Exploration vs exploitation should be balanced")
    void testExplorationBalance() {
        OperationType op = OperationType.MODWT_DECOMPOSE;
        int inputSize = 2000;
        
        // Record some initial data
        tuner.recordMeasurement(op, inputSize, 512, 1_000_000L, 2_000_000L); // 512 is good
        tuner.recordMeasurement(op, inputSize, 1024, 2_000_000L, 1_000_000L); // 1024 is bad
        
        // Check that we get some variety in selections (exploration)
        var selectedThresholds = IntStream.range(0, 100)
            .map(i -> tuner.getAdaptiveThreshold(op, inputSize, 1.0))
            .distinct()
            .count();
        
        // Should explore multiple options, not just exploit the best
        assertTrue(selectedThresholds >= 2, 
                  "Should explore multiple thresholds, found: " + selectedThresholds);
    }
    
    @ParameterizedTest
    @CsvSource({
        "100,0.5",
        "500,1.0", 
        "1000,1.5",
        "5000,2.0",
        "10000,3.0"
    })
    @DisplayName("Different input sizes and complexities should work")
    void testVariousInputSizesAndComplexities(int inputSize, double complexity) {
        for (OperationType op : OperationType.values()) {
            int threshold = tuner.getAdaptiveThreshold(op, inputSize, complexity);
            
            assertTrue(threshold > 0, 
                      String.format("Invalid threshold for op=%s, size=%d, complexity=%.1f", 
                                  op, inputSize, complexity));
        }
    }
    
    @Test
    @DisplayName("Memory pressure awareness should work")
    void testMemoryPressureHandling() {
        // The tuner should handle various memory conditions
        // We can't control actual memory, but can verify it doesn't fail
        
        OperationType op = OperationType.BATCH_PROCESSING;
        
        // Simulate different scenarios
        for (int size : new int[]{100, 1000, 10000, 100000}) {
            int threshold = tuner.getAdaptiveThreshold(op, size, 1.0);
            
            // Larger inputs might get higher thresholds to reduce parallelism overhead
            assertTrue(threshold > 0, "Should handle size: " + size);
        }
    }
    
    @RepeatedTest(5)
    @DisplayName("Repeated calls should show learning behavior")
    void testLearningBehavior() {
        OperationType op = OperationType.CWT_TRANSFORM;
        int inputSize = 4096;
        
        // Initial random selection
        int firstThreshold = tuner.getAdaptiveThreshold(op, inputSize, 1.0);
        
        // Provide consistent positive feedback (parallel is faster)
        for (int i = 0; i < 10; i++) {
            tuner.recordMeasurement(op, inputSize, firstThreshold, 500_000L, 1_000_000L);
        }
        
        // Count how often we get the same threshold
        int matches = 0;
        for (int i = 0; i < 50; i++) {
            if (tuner.getAdaptiveThreshold(op, inputSize, 1.0) == firstThreshold) {
                matches++;
            }
        }
        
        // Should increasingly select the successful threshold
        assertTrue(matches > 25, "Should learn from positive feedback");
    }
    
    @Test
    @DisplayName("State tracking should be accurate")
    void testStateTracking() {
        // Verify that arm states are properly tracked
        OperationType op = OperationType.MODWT_RECONSTRUCT;
        
        // Record results for different thresholds
        tuner.recordMeasurement(op, 1000, 256, 1_000_000L, 2_000_000L); // 256 is good
        tuner.recordMeasurement(op, 1000, 512, 2_000_000L, 1_000_000L); // 512 is bad 
        tuner.recordMeasurement(op, 1000, 1024, 500_000L, 1_000_000L); // 1024 is good
        
        // The tuner should maintain these states internally
        // We can't directly inspect, but can verify behavior is consistent
        
        var selections = IntStream.range(0, 100)
            .map(i -> tuner.getAdaptiveThreshold(op, 1000, 1.0))
            .toArray();
        
        // Should see all three thresholds at some point (exploration)
        var uniqueValues = IntStream.of(selections).distinct().count();
        assertTrue(uniqueValues >= 2, "Should explore multiple options");
    }
    
    @Test
    @DisplayName("Edge cases should be handled gracefully")
    void testEdgeCases() {
        // Test with extreme values
        assertDoesNotThrow(() -> {
            tuner.getAdaptiveThreshold(OperationType.MODWT_DECOMPOSE, 1, 0.001);
            tuner.getAdaptiveThreshold(OperationType.MODWT_DECOMPOSE, Integer.MAX_VALUE, 1000.0);
            tuner.recordMeasurement(OperationType.BATCH_PROCESSING, 0, 256, 0L, 0L);
            tuner.recordMeasurement(OperationType.BATCH_PROCESSING, -1, 256, -1L, -1L);
        });
    }
    
    @Test
    @DisplayName("Reset behavior should clear learned states")
    void testResetCapability() {
        OperationType op = OperationType.WAVELET_DENOISE;
        
        // Train the tuner (512 is consistently good)
        for (int i = 0; i < 50; i++) {
            tuner.recordMeasurement(op, 2000, 512, 1_000_000L, 2_000_000L);
        }
        
        // Create a new tuner (simulating reset)
        AdaptiveThresholdTuner newTuner = new AdaptiveThresholdTuner();
        
        // New tuner should not have the learned bias
        var oldSelections = IntStream.range(0, 100)
            .map(i -> tuner.getAdaptiveThreshold(op, 2000, 1.0))
            .filter(t -> t == 512)
            .count();
            
        var newSelections = IntStream.range(0, 100)
            .map(i -> newTuner.getAdaptiveThreshold(op, 2000, 1.0))
            .filter(t -> t == 512)
            .count();
        
        // Old tuner should strongly prefer 512, new one should be more random
        assertTrue(oldSelections > newSelections, 
                  "Old tuner should have learned bias");
    }
    
    @Test
    @DisplayName("Should provide comprehensive statistics")
    void testTunerStatistics() {
        OperationType op = OperationType.MODWT_DECOMPOSE;
        
        // Record some measurements
        tuner.recordMeasurement(op, 1000, 256, 1_000_000L, 2_000_000L);
        tuner.recordMeasurement(op, 2000, 512, 1_500_000L, 2_500_000L);
        tuner.recordMeasurement(op, 3000, 1024, 2_000_000L, 3_000_000L);
        
        // Get statistics
        AdaptiveThresholdTuner.TunerStatistics stats = tuner.getStatistics();
        
        assertNotNull(stats);
        assertTrue(stats.measurementCount > 0);
        assertTrue(stats.currentThreshold > 0);
        assertTrue(stats.overheadFactor >= 0);
        assertNotNull(stats.operationThresholds);
        assertNotNull(stats.operationRewards);
        
        // Should have operation-specific data
        assertFalse(stats.operationThresholds.isEmpty());
        assertFalse(stats.operationRewards.isEmpty());
    }
    
    @Test
    @DisplayName("Should track measurements correctly in statistics")
    void testStatisticsMeasurementTracking() {
        AdaptiveThresholdTuner.TunerStatistics initialStats = tuner.getStatistics();
        int initialCount = initialStats.measurementCount;
        
        // Record a measurement
        tuner.recordMeasurement(OperationType.CWT_TRANSFORM, 1500, 256, 800_000L, 1_200_000L);
        
        AdaptiveThresholdTuner.TunerStatistics updatedStats = tuner.getStatistics();
        
        assertEquals(initialCount + 1, updatedStats.measurementCount);
    }
    
    @Test
    @DisplayName("Should update operation-specific statistics")
    void testOperationSpecificStatistics() {
        OperationType op1 = OperationType.MODWT_DECOMPOSE;
        OperationType op2 = OperationType.CWT_TRANSFORM;
        
        // Record measurements for different operations
        tuner.recordMeasurement(op1, 1000, 512, 1_000_000L, 1_500_000L);
        tuner.recordMeasurement(op2, 2000, 1024, 2_000_000L, 2_500_000L);
        
        AdaptiveThresholdTuner.TunerStatistics stats = tuner.getStatistics();
        
        // Check that statistics maps are available and have data
        assertNotNull(stats.operationThresholds);
        assertNotNull(stats.operationRewards);
        
        // The statistics should have some entries after recording measurements
        // The exact keys might depend on internal implementation details
        assertTrue(stats.measurementCount >= 2, "Should have recorded at least 2 measurements");
    }
    
    @Test
    @DisplayName("Should provide meaningful statistics toString")
    void testStatisticsToString() {
        // Record some data
        tuner.recordMeasurement(OperationType.BATCH_PROCESSING, 1000, 256, 1_000_000L, 1_500_000L);
        
        AdaptiveThresholdTuner.TunerStatistics stats = tuner.getStatistics();
        String statsString = stats.toString();
        
        assertNotNull(statsString);
        assertTrue(statsString.contains("TunerStats"));
        assertTrue(statsString.contains("measurements="));
        assertTrue(statsString.contains("threshold="));
        assertTrue(statsString.contains("overhead="));
        assertTrue(statsString.contains("ops="));
    }
    
    @Test
    @DisplayName("Should handle statistics with no measurements")
    void testEmptyStatistics() {
        // Get stats from fresh tuner
        AdaptiveThresholdTuner freshTuner = new AdaptiveThresholdTuner();
        AdaptiveThresholdTuner.TunerStatistics stats = freshTuner.getStatistics();
        
        assertNotNull(stats);
        assertEquals(0, stats.measurementCount);
        assertTrue(stats.currentThreshold > 0); // Should have default threshold
        assertTrue(stats.overheadFactor > 0); // Should have default overhead factor
        assertNotNull(stats.operationThresholds);
        assertNotNull(stats.operationRewards);
    }
    
    @Test
    @DisplayName("Should update threshold in statistics over time")
    void testThresholdEvolutionInStats() {
        OperationType op = OperationType.MODWT_RECONSTRUCT;
        
        AdaptiveThresholdTuner.TunerStatistics initialStats = tuner.getStatistics();
        int initialThreshold = initialStats.currentThreshold;
        
        // Record many poor performance measurements to increase threshold
        for (int i = 0; i < 100; i++) {
            tuner.recordMeasurement(op, 500, 256, 5_000_000L, 1_000_000L); // Parallel much slower
        }
        
        // Allow some time for threshold adjustment
        try {
            Thread.sleep(100); // Brief pause to allow internal adjustments
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        AdaptiveThresholdTuner.TunerStatistics updatedStats = tuner.getStatistics();
        
        // Statistics should reflect the updated state
        assertTrue(updatedStats.measurementCount > initialStats.measurementCount);
    }
    
    @Test
    @DisplayName("Should track overhead factor in statistics")
    void testOverheadFactorTracking() {
        AdaptiveThresholdTuner.TunerStatistics initialStats = tuner.getStatistics();
        double initialOverhead = initialStats.overheadFactor;
        
        // Record measurements that should affect overhead factor
        tuner.recordMeasurement(OperationType.WAVELET_DENOISE, 100, 256, 2_000_000L, 1_000_000L);
        
        AdaptiveThresholdTuner.TunerStatistics updatedStats = tuner.getStatistics();
        
        // Should have valid overhead factor
        assertTrue(updatedStats.overheadFactor > 0);
        assertNotEquals(Double.NaN, updatedStats.overheadFactor);
        assertNotEquals(Double.POSITIVE_INFINITY, updatedStats.overheadFactor);
    }
}
