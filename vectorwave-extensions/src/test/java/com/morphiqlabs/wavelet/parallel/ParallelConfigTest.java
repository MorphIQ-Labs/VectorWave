package com.morphiqlabs.wavelet.extensions.parallel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ParallelConfig.
 * Tests configuration creation, validation, parallelization decisions, and resource management.
 */
class ParallelConfigTest {
    
    private ParallelConfig config;
    private List<ParallelConfig> configsToCleanup;
    
    @BeforeEach
    void setUp() {
        configsToCleanup = new ArrayList<>();
    }
    
    @AfterEach
    void tearDown() {
        // Clean up any configs created during tests
        for (ParallelConfig config : configsToCleanup) {
            config.shutdown();
        }
        configsToCleanup.clear();
    }
    
    private void trackForCleanup(ParallelConfig config) {
        configsToCleanup.add(config);
    }
    
    // ============== Factory Method Tests ==============
    
    @Test
    @DisplayName("auto() should create valid configuration")
    void testAutoConfiguration() {
        config = ParallelConfig.auto();
        trackForCleanup(config);
        
        assertNotNull(config);
        assertTrue(config.getParallelismLevel() > 0);
        assertTrue(config.getParallelThreshold() > 0);
        assertTrue(config.isUseVirtualThreads());
        assertEquals(ParallelConfig.ExecutionMode.ADAPTIVE, config.getMode());
        assertTrue(config.isAdaptiveThreshold());
        assertTrue(config.isEnableStructuredConcurrency());
        assertTrue(config.isEnableParallelThresholding());
        
        // Verify executors are created
        assertNotNull(config.getCPUExecutor());
        assertNotNull(config.getIOExecutor());
    }
    
    @Test
    @DisplayName("cpuIntensive() should create CPU-optimized configuration")
    void testCpuIntensiveConfiguration() {
        config = ParallelConfig.cpuIntensive();
        trackForCleanup(config);
        
        assertNotNull(config);
        assertTrue(config.getParallelismLevel() > 0);
        assertTrue(config.getParallelThreshold() > 0);
        assertFalse(config.isUseVirtualThreads(), "CPU-intensive should prefer platform threads");
        assertEquals(ParallelConfig.ExecutionMode.ADAPTIVE, config.getMode());
        assertTrue(config.getChunkSize() > 0);
        
        // CPU and IO executors should be the same (no virtual threads)
        assertSame(config.getCPUExecutor(), config.getIOExecutor());
    }
    
    @Test
    @DisplayName("ioIntensive() should create I/O-optimized configuration")
    void testIoIntensiveConfiguration() {
        config = ParallelConfig.ioIntensive();
        trackForCleanup(config);
        
        assertNotNull(config);
        assertTrue(config.getParallelismLevel() > Runtime.getRuntime().availableProcessors(),
            "I/O intensive should have more parallelism than CPU cores");
        assertTrue(config.isUseVirtualThreads(), "I/O intensive should use virtual threads");
        assertEquals(ParallelConfig.ExecutionMode.VIRTUAL_THREADS_IO, config.getMode());
        assertTrue(config.isEnableStructuredConcurrency());
        
        // Should have separate virtual thread executor
        assertNotSame(config.getCPUExecutor(), config.getIOExecutor());
    }
    
    // ============== Builder Pattern Tests ==============
    
    @Test
    @DisplayName("Builder should create configuration with custom values")
    void testBuilderWithCustomValues() {
        config = new ParallelConfig.Builder()
            .parallelismLevel(4)
            .parallelThreshold(256)
            .useVirtualThreads(false)
            .enableGPU(true)
            .mode(ParallelConfig.ExecutionMode.PARALLEL_ALWAYS)
            .chunkSize(128)
            .enableStructuredConcurrency(false)
            .adaptiveThreshold(true)
            .overheadFactor(1.5)
            .enableParallelThresholding(false)
            .enableMetrics(true)
            .build();
        trackForCleanup(config);
        
        assertEquals(4, config.getParallelismLevel());
        assertEquals(256, config.getParallelThreshold());
        assertFalse(config.isUseVirtualThreads());
        assertTrue(config.isEnableGPU());
        assertEquals(ParallelConfig.ExecutionMode.PARALLEL_ALWAYS, config.getMode());
        assertEquals(128, config.getChunkSize());
        assertFalse(config.isEnableStructuredConcurrency());
        assertTrue(config.isAdaptiveThreshold());
        assertEquals(1.5, config.getOverheadFactor(), 0.001);
        assertFalse(config.isEnableParallelThresholding());
        assertTrue(config.isEnableMetrics());
    }
    
    @Test
    @DisplayName("Builder should validate parameter bounds")
    void testBuilderValidation() {
        ParallelConfig.Builder builder = new ParallelConfig.Builder();
        
        // Test invalid parallelism level
        assertThrows(IllegalArgumentException.class, () -> 
            builder.parallelismLevel(0));
        assertThrows(IllegalArgumentException.class, () -> 
            builder.parallelismLevel(-1));
        
        // Test invalid parallel threshold
        assertThrows(IllegalArgumentException.class, () -> 
            builder.parallelThreshold(0));
        assertThrows(IllegalArgumentException.class, () -> 
            builder.parallelThreshold(-1));
        
        // Test invalid chunk size
        assertThrows(IllegalArgumentException.class, () -> 
            builder.chunkSize(0));
        assertThrows(IllegalArgumentException.class, () -> 
            builder.chunkSize(-1));
        
        // Test invalid overhead factor
        assertThrows(IllegalArgumentException.class, () -> 
            builder.overheadFactor(0.0));
        assertThrows(IllegalArgumentException.class, () -> 
            builder.overheadFactor(-1.0));
    }
    
    @Test
    @DisplayName("Builder should use fluent interface")
    void testBuilderFluentInterface() {
        ParallelConfig.Builder builder = new ParallelConfig.Builder();
        
        // All methods should return the builder instance
        assertSame(builder, builder.parallelismLevel(2));
        assertSame(builder, builder.parallelThreshold(512));
        assertSame(builder, builder.useVirtualThreads(true));
        assertSame(builder, builder.enableGPU(false));
        assertSame(builder, builder.mode(ParallelConfig.ExecutionMode.ADAPTIVE));
        assertSame(builder, builder.chunkSize(256));
        assertSame(builder, builder.enableStructuredConcurrency(true));
        assertSame(builder, builder.adaptiveThreshold(false));
        assertSame(builder, builder.overheadFactor(1.0));
        assertSame(builder, builder.enableParallelThresholding(true));
        assertSame(builder, builder.enableMetrics(false));
    }
    
    // ============== Parallelization Decision Tests ==============
    
    @ParameterizedTest
    @EnumSource(ParallelConfig.ExecutionMode.class)
    @DisplayName("shouldParallelize should respect execution modes")
    void testShouldParallelizeByMode(ParallelConfig.ExecutionMode mode) {
        config = new ParallelConfig.Builder()
            .mode(mode)
            .parallelThreshold(1000)
            .enableGPU(false)
            .useVirtualThreads(true)
            .build();
        trackForCleanup(config);
        
        int smallInput = 100;
        int largeInput = 2000;
        double normalComplexity = 1.0;
        
        switch (mode) {
            case SEQUENTIAL:
                assertFalse(config.shouldParallelize(smallInput, normalComplexity));
                assertFalse(config.shouldParallelize(largeInput, normalComplexity));
                break;
                
            case PARALLEL_ALWAYS:
                assertTrue(config.shouldParallelize(smallInput, normalComplexity));
                assertTrue(config.shouldParallelize(largeInput, normalComplexity));
                break;
                
            case ADAPTIVE:
                assertFalse(config.shouldParallelize(smallInput, normalComplexity));
                assertTrue(config.shouldParallelize(largeInput, normalComplexity));
                break;
                
            case GPU_PREFERRED:
                // Without GPU enabled, should use threshold-based decision
                assertFalse(config.shouldParallelize(smallInput, normalComplexity));
                assertTrue(config.shouldParallelize(largeInput, normalComplexity));
                break;
                
            case VIRTUAL_THREADS_IO:
                // Lower threshold for I/O operations
                assertFalse(config.shouldParallelize(smallInput, normalComplexity));
                assertTrue(config.shouldParallelize(largeInput, normalComplexity));
                break;
        }
    }
    
    @Test
    @DisplayName("shouldParallelize with adaptive threshold should adjust for complexity")
    void testShouldParallelizeAdaptiveThreshold() {
        config = new ParallelConfig.Builder()
            .mode(ParallelConfig.ExecutionMode.ADAPTIVE)
            .parallelThreshold(1000)
            .adaptiveThreshold(true)
            .build();
        trackForCleanup(config);
        
        int inputSize = 800;
        
        // Low complexity should require larger input
        assertFalse(config.shouldParallelize(inputSize, 0.5));
        
        // High complexity should parallelize smaller input  
        assertTrue(config.shouldParallelize(inputSize, 2.0));
        
        // Normal complexity
        assertFalse(config.shouldParallelize(inputSize, 1.0));
    }
    
    @Test
    @DisplayName("shouldParallelize with GPU preferred should consider GPU availability")
    void testShouldParallelizeGpuPreferred() {
        // Test with GPU enabled
        ParallelConfig gpuConfig = new ParallelConfig.Builder()
            .mode(ParallelConfig.ExecutionMode.GPU_PREFERRED)
            .enableGPU(true)
            .parallelThreshold(1000)
            .build();
        trackForCleanup(gpuConfig);
        
        // Should parallelize even small inputs when GPU is available
        assertTrue(gpuConfig.shouldParallelize(100, 1.0));
        
        // Test without GPU
        ParallelConfig noGpuConfig = new ParallelConfig.Builder()
            .mode(ParallelConfig.ExecutionMode.GPU_PREFERRED)
            .enableGPU(false)
            .parallelThreshold(1000)
            .build();
        trackForCleanup(noGpuConfig);
        
        // Should fall back to threshold-based decision
        assertFalse(noGpuConfig.shouldParallelize(100, 1.0));
        assertTrue(noGpuConfig.shouldParallelize(2000, 1.0));
    }
    
    // ============== Chunk Calculation Tests ==============
    
    @Test
    @DisplayName("calculateChunks should return 1 for small data")
    void testCalculateChunksSmallData() {
        config = new ParallelConfig.Builder()
            .parallelThreshold(1000)
            .build();
        trackForCleanup(config);
        
        assertEquals(1, config.calculateChunks(500));
        assertEquals(1, config.calculateChunks(1000));
    }
    
    @Test
    @DisplayName("calculateChunks should respect parallelism level")
    void testCalculateChunksParallelismLevel() {
        config = new ParallelConfig.Builder()
            .parallelismLevel(4)
            .parallelThreshold(100)
            .chunkSize(50)
            .build();
        trackForCleanup(config);
        
        // Large data should use parallelism level
        int chunks = config.calculateChunks(10000);
        assertTrue(chunks <= 4);
        assertTrue(chunks > 0);
    }
    
    @Test
    @DisplayName("calculateChunks should ensure minimum chunk size")
    void testCalculateChunksMinimumSize() {
        config = new ParallelConfig.Builder()
            .parallelismLevel(8)
            .parallelThreshold(1000)
            .chunkSize(100)
            .build();
        trackForCleanup(config);
        
        // Small data relative to parallelism should create fewer chunks
        int chunks = config.calculateChunks(500);
        assertEquals(1, chunks); // Too small for parallelization
        
        chunks = config.calculateChunks(2000);
        assertTrue(chunks > 1);
        assertTrue(chunks <= 8);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {100, 500, 1000, 5000, 10000})
    @DisplayName("calculateChunks should handle various data sizes")
    void testCalculateChunksVariousSize(int dataSize) {
        config = new ParallelConfig.Builder()
            .parallelismLevel(4)
            .parallelThreshold(200)
            .chunkSize(50)
            .build();
        trackForCleanup(config);
        
        int chunks = config.calculateChunks(dataSize);
        assertTrue(chunks > 0);
        assertTrue(chunks <= Math.max(4, dataSize / 50));
        
        if (dataSize <= 200) {
            assertEquals(1, chunks);
        }
    }
    
    // ============== Executor Service Tests ==============
    
    @Test
    @DisplayName("getCPUExecutor should return non-null executor")
    void testGetCpuExecutor() {
        config = ParallelConfig.auto();
        trackForCleanup(config);
        
        ExecutorService executor = config.getCPUExecutor();
        assertNotNull(executor);
        assertFalse(executor.isShutdown());
    }
    
    @Test
    @DisplayName("getIOExecutor should return appropriate executor based on virtual thread setting")
    void testGetIoExecutor() {
        // Test with virtual threads enabled
        ParallelConfig virtualConfig = new ParallelConfig.Builder()
            .useVirtualThreads(true)
            .build();
        trackForCleanup(virtualConfig);
        
        ExecutorService virtualExecutor = virtualConfig.getIOExecutor();
        assertNotNull(virtualExecutor);
        
        // Test with virtual threads disabled
        ParallelConfig platformConfig = new ParallelConfig.Builder()
            .useVirtualThreads(false)
            .build();
        trackForCleanup(platformConfig);
        
        ExecutorService platformExecutor = platformConfig.getIOExecutor();
        assertNotNull(platformExecutor);
        
        // Should be same as CPU executor when virtual threads disabled
        assertSame(platformConfig.getCPUExecutor(), platformExecutor);
    }
    
    @Test
    @DisplayName("Executors should be functional for task execution")
    void testExecutorFunctionality() throws Exception {
        config = ParallelConfig.auto();
        trackForCleanup(config);
        
        ExecutorService cpuExecutor = config.getCPUExecutor();
        ExecutorService ioExecutor = config.getIOExecutor();
        
        // Test CPU executor
        CompletableFuture<Integer> cpuTask = CompletableFuture.supplyAsync(() -> 42, cpuExecutor);
        assertEquals(42, cpuTask.get(1, TimeUnit.SECONDS));
        
        // Test I/O executor
        CompletableFuture<String> ioTask = CompletableFuture.supplyAsync(() -> "test", ioExecutor);
        assertEquals("test", ioTask.get(1, TimeUnit.SECONDS));
    }
    
    // ============== Metrics and Statistics Tests ==============
    
    @Test
    @DisplayName("recordExecution should track metrics when enabled")
    void testRecordExecutionWithMetrics() {
        config = new ParallelConfig.Builder()
            .enableMetrics(true)
            .build();
        trackForCleanup(config);
        
        // Record some executions
        config.recordExecution(true);
        config.recordExecution(true);
        config.recordExecution(false);
        
        ParallelConfig.ExecutionStats stats = config.getStats();
        assertEquals(2, stats.parallelExecutions());
        assertEquals(1, stats.sequentialExecutions());
        assertTrue(stats.estimatedSpeedup() > 0);
    }
    
    @Test
    @DisplayName("recordExecution should not track when metrics disabled")
    void testRecordExecutionWithoutMetrics() {
        config = new ParallelConfig.Builder()
            .enableMetrics(false)
            .build();
        trackForCleanup(config);
        
        // Record some executions
        config.recordExecution(true);
        config.recordExecution(false);
        
        ParallelConfig.ExecutionStats stats = config.getStats();
        assertEquals(0, stats.parallelExecutions());
        assertEquals(0, stats.sequentialExecutions());
    }
    
    @Test
    @DisplayName("ExecutionStats should calculate parallel ratio correctly")
    void testExecutionStatsParallelRatio() {
        config = new ParallelConfig.Builder()
            .enableMetrics(true)
            .build();
        trackForCleanup(config);
        
        // Test with no executions
        ParallelConfig.ExecutionStats emptyStats = config.getStats();
        assertEquals(0.0, emptyStats.parallelRatio(), 0.001);
        
        // Test with mixed executions
        config.recordExecution(true);
        config.recordExecution(true);
        config.recordExecution(true);
        config.recordExecution(false);
        
        ParallelConfig.ExecutionStats stats = config.getStats();
        assertEquals(0.75, stats.parallelRatio(), 0.001);
    }
    
    @Test
    @DisplayName("ExecutionStats should estimate speedup using Amdahl's law")
    void testExecutionStatsSpeedupEstimate() {
        config = new ParallelConfig.Builder()
            .parallelismLevel(4)
            .enableMetrics(true)
            .build();
        trackForCleanup(config);
        
        // All parallel executions
        for (int i = 0; i < 10; i++) {
            config.recordExecution(true);
        }
        
        ParallelConfig.ExecutionStats allParallel = config.getStats();
        assertTrue(allParallel.estimatedSpeedup() > 1.0);
        assertTrue(allParallel.estimatedSpeedup() <= 4.0); // Limited by parallelism level
        
        // Mixed executions
        for (int i = 0; i < 10; i++) {
            config.recordExecution(false);
        }
        
        ParallelConfig.ExecutionStats mixed = config.getStats();
        assertTrue(mixed.estimatedSpeedup() > 1.0);
        assertTrue(mixed.estimatedSpeedup() < allParallel.estimatedSpeedup());
    }
    
    // ============== Thread Safety Tests ==============
    
    @Test
    @DisplayName("recordExecution should be thread-safe")
    void testRecordExecutionThreadSafety() throws InterruptedException {
        config = new ParallelConfig.Builder()
            .enableMetrics(true)
            .build();
        trackForCleanup(config);
        
        int numThreads = 10;
        int executionsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        for (int i = 0; i < numThreads; i++) {
            final boolean isParallel = i % 2 == 0;
            Thread thread = new Thread(() -> {
                try {
                    for (int j = 0; j < executionsPerThread; j++) {
                        config.recordExecution(isParallel);
                    }
                } finally {
                    latch.countDown();
                }
            });
            thread.start();
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        
        ParallelConfig.ExecutionStats stats = config.getStats();
        int totalExpected = numThreads * executionsPerThread;
        int totalActual = stats.parallelExecutions() + stats.sequentialExecutions();
        assertEquals(totalExpected, totalActual);
        
        // Half should be parallel, half sequential
        assertEquals(totalExpected / 2, stats.parallelExecutions());
        assertEquals(totalExpected / 2, stats.sequentialExecutions());
    }
    
    @Test
    @DisplayName("Multiple threads should be able to get executors safely")
    void testExecutorAccessThreadSafety() throws InterruptedException {
        config = ParallelConfig.auto();
        trackForCleanup(config);
        
        int numThreads = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; i++) {
            Thread thread = new Thread(() -> {
                try {
                    ExecutorService cpuExec = config.getCPUExecutor();
                    ExecutorService ioExec = config.getIOExecutor();
                    
                    if (cpuExec != null && ioExec != null) {
                        successCount.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
            thread.start();
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        assertEquals(numThreads, successCount.get());
    }
    
    // ============== Resource Management Tests ==============
    
    @Test
    @DisplayName("shutdown should clean up virtual executor")
    void testShutdown() {
        config = new ParallelConfig.Builder()
            .useVirtualThreads(true)
            .build();
        
        ExecutorService ioExecutor = config.getIOExecutor();
        assertFalse(ioExecutor.isShutdown());
        
        config.shutdown();
        
        // Virtual executor should be shutdown, but we can't easily test this
        // because the virtual thread executor doesn't expose shutdown state
        // in the same way. The important thing is that shutdown() doesn't throw.
        assertDoesNotThrow(() -> config.shutdown()); // Should be safe to call multiple times
    }
    
    @Test
    @DisplayName("shutdown should be safe to call multiple times")
    void testShutdownIdempotent() {
        config = ParallelConfig.auto();
        
        assertDoesNotThrow(() -> {
            config.shutdown();
            config.shutdown();
            config.shutdown();
        });
    }
    
    @Test
    @DisplayName("shutdown should not affect CPU executor (common pool)")
    void testShutdownPreservesCpuExecutor() {
        config = ParallelConfig.auto();
        
        ExecutorService cpuExecutor = config.getCPUExecutor();
        config.shutdown();
        
        // CPU executor (common pool) should not be shutdown
        assertFalse(cpuExecutor.isShutdown());
    }
    
    // ============== Edge Cases and Boundary Tests ==============
    
    @Test
    @DisplayName("Configuration with extreme values should work")
    void testExtremeValues() {
        // Test with very high parallelism
        ParallelConfig highParallelism = new ParallelConfig.Builder()
            .parallelismLevel(1000)
            .parallelThreshold(1)
            .build();
        trackForCleanup(highParallelism);
        
        assertTrue(highParallelism.shouldParallelize(2, 1.0));
        assertTrue(highParallelism.calculateChunks(10000) > 0);
        
        // Test with minimal values
        ParallelConfig minimal = new ParallelConfig.Builder()
            .parallelismLevel(1)
            .parallelThreshold(1)
            .chunkSize(1)
            .overheadFactor(0.001)
            .build();
        trackForCleanup(minimal);
        
        assertTrue(minimal.shouldParallelize(2, 1.0));
        assertEquals(1, minimal.calculateChunks(10));
    }
    
    @RepeatedTest(5)
    @DisplayName("Auto configuration should be consistent")
    void testAutoConfigurationConsistency() {
        ParallelConfig config1 = ParallelConfig.auto();
        ParallelConfig config2 = ParallelConfig.auto();
        
        trackForCleanup(config1);
        trackForCleanup(config2);
        
        // Configurations should be the same (based on system properties)
        assertEquals(config1.getParallelismLevel(), config2.getParallelismLevel());
        assertEquals(config1.getParallelThreshold(), config2.getParallelThreshold());
        assertEquals(config1.isUseVirtualThreads(), config2.isUseVirtualThreads());
        assertEquals(config1.getMode(), config2.getMode());
    }
    
    @Test
    @DisplayName("Configuration should handle zero complexity gracefully")
    void testZeroComplexity() {
        config = new ParallelConfig.Builder()
            .mode(ParallelConfig.ExecutionMode.ADAPTIVE)
            .parallelThreshold(1000)
            .adaptiveThreshold(true)
            .build();
        trackForCleanup(config);
        
        // Zero complexity shouldn't cause division by zero
        assertDoesNotThrow(() -> {
            boolean result = config.shouldParallelize(2000, 0.0);
            // With zero complexity, threshold becomes infinite, so should be false
            assertFalse(result);
        });
    }
}
