package com.morphiqlabs.wavelet.extensions.parallel;

import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.*;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Parameterized tests for different parallelization strategies.
 * Tests various execution modes, input sizes, and configurations.
 */
class ParallelizationStrategyTest {
    
    private static final double TOLERANCE = 1e-10;
    
    @ParameterizedTest
    @EnumSource(ParallelConfig.ExecutionMode.class)
    @DisplayName("Test all execution modes with various input sizes")
    void testExecutionModes(ParallelConfig.ExecutionMode mode) {
        ParallelConfig config = new ParallelConfig.Builder()
            .mode(mode)
            .parallelThreshold(512)
            .build();
        
        // Test with different input sizes
        int[] sizes = {100, 512, 1024, 4096, 16384};
        
        for (int size : sizes) {
            boolean shouldParallelize = config.shouldParallelize(size, 1.0);
            
            switch (mode) {
                case SEQUENTIAL -> assertFalse(shouldParallelize, 
                    "Sequential mode should never parallelize");
                case PARALLEL_ALWAYS -> assertTrue(shouldParallelize, 
                    "Parallel always should always parallelize");
                case ADAPTIVE -> assertEquals(size > 512, shouldParallelize,
                    "Adaptive should parallelize based on threshold");
                case GPU_PREFERRED -> assertEquals(size > 512, shouldParallelize,
                    "GPU preferred should fall back to CPU threshold");
                case VIRTUAL_THREADS_IO -> assertEquals(size >= 256, shouldParallelize,
                    "Virtual threads should use lower threshold");
            }
        }
        
        config.shutdown();
    }
    
    @ParameterizedTest
    @CsvSource({
        "1, 1024, 1",     // Single core
        "2, 1024, 2",     // Dual core
        "4, 1024, 4",     // Quad core  
        "8, 1024, 8",     // Eight core
        "16, 1024, 16",   // Many core
        "32, 1024, 32",   // High parallelism
        "4, 100, 1",      // Small data
        "4, 50000, 4"     // Large data
    })
    @DisplayName("Test chunk calculation with different parallelism levels")
    void testChunkCalculation(int parallelismLevel, int dataSize, int expectedMaxChunks) {
        ParallelConfig config = new ParallelConfig.Builder()
            .parallelismLevel(parallelismLevel)
            .parallelThreshold(512)
            .build();
        
        int chunks = config.calculateChunks(dataSize);
        
        assertTrue(chunks >= 1, "Should have at least 1 chunk");
        assertTrue(chunks <= expectedMaxChunks, 
            "Should not exceed max chunks: " + chunks + " > " + expectedMaxChunks);
        
        // Verify minimum chunk size constraint
        if (dataSize > 512) {
            int chunkSize = dataSize / chunks;
            assertTrue(chunkSize >= 128, "Chunk size should be reasonable: " + chunkSize);
        }
        
        config.shutdown();
    }
    
    @ParameterizedTest
    @MethodSource("provideComplexityScenarios")
    @DisplayName("Test adaptive threshold with complexity factors")
    void testComplexityAdjustment(int inputSize, double complexity, boolean expectedResult) {
        ParallelConfig config = new ParallelConfig.Builder()
            .mode(ParallelConfig.ExecutionMode.ADAPTIVE)
            .parallelThreshold(1024)
            .adaptiveThreshold(true)
            .build();
        
        boolean shouldParallelize = config.shouldParallelize(inputSize, complexity);
        assertEquals(expectedResult, shouldParallelize,
            String.format("Size=%d, complexity=%.1f should%s parallelize",
                inputSize, complexity, expectedResult ? "" : " not"));
        
        config.shutdown();
    }
    
    static Stream<Arguments> provideComplexityScenarios() {
        return Stream.of(
            // inputSize, complexity, expectedResult
            Arguments.of(2048, 1.0, true),   // Large, normal complexity
            Arguments.of(2048, 2.0, true),   // Large, high complexity 
            Arguments.of(512, 1.0, false),   // Small, normal complexity
            Arguments.of(512, 0.5, false),   // Small, low complexity (still below threshold)
            Arguments.of(1025, 1.0, true),   // Just above threshold
            Arguments.of(1024, 2.0, true),   // At threshold, high complexity (adjusted is 512)
            Arguments.of(4096, 4.0, true),   // Very large, very complex
            Arguments.of(256, 0.25, false)   // Very small, very easy (still below min)
        );
    }
    
    @ParameterizedTest
    @MethodSource("provideParallelConfigs")
    @DisplayName("Test different parallel configurations")
    void testParallelConfigurationVariants(ParallelConfig config, String description) {
        assertNotNull(config, "Config should not be null: " + description);
        assertNotNull(config.getCPUExecutor(), "CPU executor should exist");
        
        if (config.isUseVirtualThreads()) {
            assertNotNull(config.getIOExecutor(), "IO executor should exist");
            assertNotEquals(config.getCPUExecutor(), config.getIOExecutor(),
                "IO executor should be different from CPU executor");
        }
        
        // Test execution recording only if metrics are enabled
        if (config.isEnableMetrics()) {
            config.recordExecution(true);
            config.recordExecution(false);
            
            var stats = config.getStats();
            assertEquals(1, stats.parallelExecutions(), "Should have 1 parallel execution");
            assertEquals(1, stats.sequentialExecutions(), "Should have 1 sequential execution");
            assertEquals(0.5, stats.parallelRatio(), 0.01, "Should have 50% parallel ratio");
        }
        
        config.shutdown();
    }
    
    static Stream<Arguments> provideParallelConfigs() {
        return Stream.of(
            Arguments.of(ParallelConfig.auto(), "Auto configuration"),
            Arguments.of(ParallelConfig.cpuIntensive(), "CPU intensive"),
            Arguments.of(ParallelConfig.ioIntensive(), "I/O intensive"),
            Arguments.of(
                new ParallelConfig.Builder()
                    .parallelismLevel(2)
                    .parallelThreshold(256)
                    .useVirtualThreads(false)
                    .mode(ParallelConfig.ExecutionMode.ADAPTIVE)
                    .build(),
                "Custom low parallelism"
            ),
            Arguments.of(
                new ParallelConfig.Builder()
                    .parallelismLevel(16)
                    .parallelThreshold(2048)
                    .enableMetrics(true)
                    .enableStructuredConcurrency(true)
                    .build(),
                "High parallelism with metrics"
            )
        );
    }
    
    @ParameterizedTest
    @CsvSource({
        "100, 1.0, SEQUENTIAL",
        "10000, 1.0, PARALLEL_ALWAYS",
        "2048, 2.0, ADAPTIVE",
        "4096, 1.0, GPU_PREFERRED",
        "1024, 1.0, VIRTUAL_THREADS_IO"
    })
    @DisplayName("Test execution mode selection consistency")
    void testExecutionModeConsistency(int inputSize, double complexity, 
                                     ParallelConfig.ExecutionMode mode) {
        ParallelConfig config = new ParallelConfig.Builder()
            .mode(mode)
            .parallelThreshold(1024)
            .build();
        
        // Call multiple times to ensure consistency
        boolean first = config.shouldParallelize(inputSize, complexity);
        boolean second = config.shouldParallelize(inputSize, complexity);
        boolean third = config.shouldParallelize(inputSize, complexity);
        
        assertEquals(first, second, "Results should be consistent");
        assertEquals(second, third, "Results should be consistent");
        
        config.shutdown();
    }
    
    @ParameterizedTest
    @MethodSource("provideConcurrentScenarios")
    @DisplayName("Test concurrent access to ParallelConfig")
    void testConcurrentConfigAccess(int threads, int operations) throws InterruptedException {
        ParallelConfig config = new ParallelConfig.Builder()
            .enableMetrics(true)
            .build();
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        ConcurrentLinkedQueue<Boolean> results = new ConcurrentLinkedQueue<>();
        
        for (int t = 0; t < threads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int op = 0; op < operations; op++) {
                        int size = 1000 + threadId * 100 + op * 10;
                        boolean shouldParallelize = config.shouldParallelize(size, 1.0);
                        results.add(shouldParallelize);
                        
                        int chunks = config.calculateChunks(size);
                        assertTrue(chunks > 0, "Chunks must be positive");
                        
                        config.recordExecution(shouldParallelize);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "Operations should complete");
        assertEquals(threads * operations, results.size(), "All operations should complete");
        
        var stats = config.getStats();
        assertTrue(stats.parallelExecutions() + stats.sequentialExecutions() > 0,
            "Should have recorded executions");
        
        executor.shutdown();
        config.shutdown();
    }
    
    static Stream<Arguments> provideConcurrentScenarios() {
        return Stream.of(
            Arguments.of(2, 100),
            Arguments.of(4, 50),
            Arguments.of(8, 25),
            Arguments.of(16, 10)
        );
    }
    
    @ParameterizedTest
    @EnumSource(ParallelConfig.ExecutionMode.class)
    @DisplayName("Test execution with actual wavelet transforms")
    void testWithActualTransforms(ParallelConfig.ExecutionMode mode) {
        ParallelConfig config = new ParallelConfig.Builder()
            .mode(mode)
            .parallelThreshold(512)
            .build();
        
        MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Test with various signal sizes
        int[] sizes = {256, 1024, 4096};
        
        for (int size : sizes) {
            double[] signal = generateTestSignal(size);
            
            boolean shouldParallelize = config.shouldParallelize(size, 1.0);
            
            // Perform transform (implementation would use config internally)
            MODWTResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            // Verify reconstruction regardless of parallelization
            assertArrayEquals(signal, reconstructed, TOLERANCE,
                "Reconstruction should be accurate for mode: " + mode);
        }
        
        config.shutdown();
    }
    
    @ParameterizedTest
    @CsvSource({
        "512, 128, 4",    // Minimum threshold, small chunk
        "1024, 512, 2",   // Default threshold, default chunk
        "2048, 1024, 2",  // Large threshold, large chunk
        "4096, 2048, 2",  // Very large threshold
        "256, 64, 4"      // Very small threshold
    })
    @DisplayName("Test threshold and chunk size relationships")
    void testThresholdChunkRelationship(int threshold, int chunkSize, int expectedChunks) {
        ParallelConfig config = new ParallelConfig.Builder()
            .parallelThreshold(threshold)
            .chunkSize(chunkSize)
            .parallelismLevel(4)
            .build();
        
        // Test at exact threshold
        boolean atThreshold = config.shouldParallelize(threshold, 1.0);
        assertFalse(atThreshold, "Should not parallelize at exact threshold");
        
        // Test above threshold
        boolean aboveThreshold = config.shouldParallelize(threshold + 1, 1.0);
        assertTrue(aboveThreshold, "Should parallelize above threshold");
        
        // Test chunk calculation
        int dataSize = threshold * 2;
        int chunks = config.calculateChunks(dataSize);
        assertTrue(chunks <= 4, "Should not exceed parallelism level");
        assertTrue(chunks >= expectedChunks, "Should have minimum expected chunks");
        
        config.shutdown();
    }
    
    @ParameterizedTest
    @MethodSource("provideEdgeCases")
    @DisplayName("Test edge cases and boundary conditions")
    void testEdgeCases(int inputSize, double complexity, int parallelismLevel) {
        assertDoesNotThrow(() -> {
            ParallelConfig config = new ParallelConfig.Builder()
                .parallelismLevel(parallelismLevel)
                .build();
            
            config.shouldParallelize(inputSize, complexity);
            config.calculateChunks(inputSize);
            
            config.shutdown();
        });
    }
    
    static Stream<Arguments> provideEdgeCases() {
        return Stream.of(
            Arguments.of(0, 1.0, 1),           // Zero size
            Arguments.of(1, 1.0, 1),           // Minimum size
            Arguments.of(Integer.MAX_VALUE, 1.0, 1), // Maximum size
            Arguments.of(1024, 0.0, 1),       // Zero complexity (invalid but should handle)
            Arguments.of(1024, Double.MAX_VALUE, 1), // Huge complexity
            Arguments.of(1024, 1.0, Integer.MAX_VALUE) // Huge parallelism
        );
    }
    
    @ParameterizedTest
    @EnumSource(ParallelConfig.ExecutionMode.class)
    @DisplayName("Test adaptive tuning integration")
    void testAdaptiveTuningIntegration(ParallelConfig.ExecutionMode mode) {
        ParallelConfig config = new ParallelConfig.Builder()
            .mode(mode)
            .enableAdaptiveTuning(true)
            .build();
        
        // Test with adaptive tuner
        if (config.isEnableAdaptiveTuning()) {
            assertNotNull(config.getAdaptiveTuner(), "Adaptive tuner should be initialized");
            
            // Test with operation types
            boolean result = config.shouldParallelize(2048, 1.0,
                AdaptiveThresholdTuner.OperationType.MODWT_DECOMPOSE);
            
            // Record feedback
            config.recordAdaptiveFeedback(
                AdaptiveThresholdTuner.OperationType.MODWT_DECOMPOSE,
                2048, 1024, 1_000_000L, 2_000_000L);
        }
        
        config.shutdown();
    }
    
    // Helper methods
    
    private static double[] generateTestSignal(int size) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(4 * Math.PI * i / 32.0);
        }
        return signal;
    }
    
    private static void assertArrayEquals(double[] expected, double[] actual, 
                                         double tolerance, String message) {
        assertEquals(expected.length, actual.length, message + " - length mismatch");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], tolerance, 
                message + " - value mismatch at index " + i);
        }
    }
}
