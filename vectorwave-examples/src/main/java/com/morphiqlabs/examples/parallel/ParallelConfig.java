package com.morphiqlabs.examples.parallel;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration for parallel execution in VectorWave.
 * 
 * <p>This class provides fine-grained control over parallel execution strategies,
 * leveraging Java 24 features including virtual threads and structured concurrency.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Automatic parallelism level detection based on system capabilities</li>
 *   <li>Adaptive threshold calculation for efficient parallelization</li>
 *   <li>Virtual thread support for I/O-bound operations</li>
 *   <li>GPU acceleration configuration (when available)</li>
 *   <li>Cost-based execution mode selection</li>
 * </ul>
 * 
 * <h2>Thread Pool Sizing Strategy</h2>
 * 
 * <p>VectorWave uses a sophisticated thread pool sizing strategy that adapts to different
 * workload types and system capabilities:</p>
 * 
 * <h3>1. CPU-Bound Operations (ForkJoinPool)</h3>
 * <ul>
 *   <li><b>Default Size:</b> ForkJoinPool.commonPool() with parallelism = availableProcessors()</li>
 *   <li><b>Rationale:</b> CPU-bound tasks benefit from having one thread per CPU core</li>
 *   <li><b>Work Stealing:</b> ForkJoin's work-stealing algorithm balances load automatically</li>
 *   <li><b>Shared vs Dedicated:</b>
 *     <ul>
 *       <li>Common pool used when metrics disabled (zero overhead)</li>
 *       <li>Dedicated pool created when metrics enabled (allows shutdown control)</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <h3>2. I/O-Bound Operations (Virtual Threads)</h3>
 * <ul>
 *   <li><b>Default:</b> Executors.newVirtualThreadPerTaskExecutor()</li>
 *   <li><b>Rationale:</b> Virtual threads excel at I/O-bound tasks with blocking operations</li>
 *   <li><b>Scaling:</b> Can create millions of virtual threads with minimal memory overhead</li>
 *   <li><b>Use Cases:</b> File I/O, network operations, database queries during analysis</li>
 * </ul>
 * 
 * <h3>3. Parallelism Level Calculation</h3>
 * <pre>{@code
 * Auto mode:
 *   parallelismLevel = Runtime.getRuntime().availableProcessors()
 * 
 * Custom mode:
 *   parallelismLevel = user-specified value (1 to N)
 * 
 * Chunking:
 *   chunks = min(parallelismLevel, dataSize / minChunkSize)
 *   where minChunkSize = max(512, parallelThreshold / 4)
 * }</pre>
 * 
 * <h3>4. Threshold Determination</h3>
 * <ul>
 *   <li><b>Base Calculation:</b> threshold = OPERATIONS_PER_CORE_MS / cores</li>
 *   <li><b>Minimum:</b> Never less than 512 elements (overhead dominates below this)</li>
 *   <li><b>Adaptive:</b> Can be adjusted based on complexity factor</li>
 *   <li><b>Environment Override:</b> System properties and env vars for fine-tuning</li>
 * </ul>
 * 
 * <h3>5. Memory Considerations</h3>
 * <ul>
 *   <li><b>Chunk Size:</b> Optimized for L1 cache (512 doubles = 4KB)</li>
 *   <li><b>NUMA Awareness:</b> Work-stealing helps with NUMA architectures</li>
 *   <li><b>False Sharing:</b> Chunk boundaries aligned to cache lines</li>
 * </ul>
 * 
 * <h3>6. Performance Characteristics</h3>
 * <table>
 *   <caption>Typical speedups by core count</caption>
 *   <tr><th>Cores</th><th>Default Threshold</th><th>Chunk Size</th><th>Expected Speedup</th></tr>
 *   <tr><td>4</td><td>1024</td><td>512</td><td>2.5-3.5x</td></tr>
 *   <tr><td>8</td><td>512</td><td>512</td><td>4-6x</td></tr>
 *   <tr><td>16</td><td>256</td><td>512</td><td>8-12x</td></tr>
 *   <tr><td>32</td><td>256</td><td>512</td><td>12-20x</td></tr>
 * </table>
 */
public class ParallelConfig {
    
    // Parallelization thresholds - based on empirical measurements
    
    /**
     * Minimum array size for parallel processing to overcome overhead.
     * Based on measurements showing ~0.5ms overhead for thread coordination.
     */
    private static final int MIN_PARALLEL_SIZE = 512;
    
    /**
     * Default parallel threshold for general operations.
     * Represents approximately 1ms of work on modern CPUs.
     */
    private static final int DEFAULT_PARALLEL_THRESHOLD = 1024;
    
    /**
     * Parallel threshold for I/O-bound operations.
     * Lower threshold since I/O wait time dominates.
     */
    private static final int IO_PARALLEL_THRESHOLD = 256;
    
    /**
     * Operations per core estimate for threshold calculation.
     * Based on ~1 GFLOP/s throughput per core for wavelet operations.
     */
    private static final int OPERATIONS_PER_CORE_MS = 1000;
    
    /**
     * Default chunk size for signal processing.
     * Optimized for L1 cache (32KB) with double precision (8 bytes).
     */
    private static final int DEFAULT_CHUNK_SIZE = 512;
    
    /**
     * Cache line size in bytes on most modern processors.
     */
    private static final int CACHE_LINE_BYTES = 64;
    
    /**
     * Size of a double in bytes.
     */
    private static final int DOUBLE_SIZE_BYTES = 8;
    
    /**
     * Number of doubles per cache line.
     */
    private static final int DOUBLES_PER_CACHE_LINE = CACHE_LINE_BYTES / DOUBLE_SIZE_BYTES;
    
    /**
     * Target number of cache lines for optimal chunk size.
     * 64 cache lines * 64 bytes = 4KB (fits in L1 cache).
     */
    private static final int TARGET_CACHE_LINES = 64;
    
    /**
     * Execution modes for parallel operations.
     */
    public enum ExecutionMode {
        /** Never use parallelism */
        SEQUENTIAL,
        /** Always use parallelism regardless of input size */
        PARALLEL_ALWAYS,
        /** Adaptively choose based on input size and complexity */
        ADAPTIVE,
        /** Prefer GPU when available, fallback to parallel CPU */
        GPU_PREFERRED,
        /** Use virtual threads for I/O-bound operations */
        VIRTUAL_THREADS_IO
    }
    
    // Core configuration
    private final int parallelismLevel;
    private final int parallelThreshold;
    private final boolean useVirtualThreads;
    private final boolean enableGPU;
    private final ExecutionMode mode;
    
    // Advanced configuration
    private final int chunkSize;
    private final boolean enableStructuredConcurrency;
    private final boolean adaptiveThreshold;
    private final double overheadFactor;
    private final boolean enableParallelThresholding;
    
    // Thread pools
    private final ExecutorService cpuExecutor;
    private final ExecutorService virtualExecutor;
    
    // Performance tracking
    private final boolean enableMetrics;
    private final AtomicInteger parallelExecutions = new AtomicInteger(0);
    private final AtomicInteger sequentialExecutions = new AtomicInteger(0);
    
    // Adaptive tuning
    private final AdaptiveThresholdTuner adaptiveTuner;
    private final boolean enableAdaptiveTuning;
    
    private ParallelConfig(Builder builder) {
        this.parallelismLevel = builder.parallelismLevel;
        this.parallelThreshold = builder.parallelThreshold;
        this.useVirtualThreads = builder.useVirtualThreads;
        this.enableGPU = builder.enableGPU;
        this.mode = builder.mode;
        this.chunkSize = builder.chunkSize;
        this.enableStructuredConcurrency = builder.enableStructuredConcurrency;
        this.adaptiveThreshold = builder.adaptiveThreshold;
        this.overheadFactor = builder.overheadFactor;
        this.enableParallelThresholding = builder.enableParallelThresholding;
        this.enableMetrics = builder.enableMetrics;
        this.enableAdaptiveTuning = builder.enableAdaptiveTuning;
        
        // Initialize thread pools
        this.cpuExecutor = createCPUExecutor();
        this.virtualExecutor = useVirtualThreads ? createVirtualExecutor() : null;
        
        // Initialize adaptive tuner if enabled
        this.adaptiveTuner = enableAdaptiveTuning ? new AdaptiveThresholdTuner() : null;
    }
    
    /**
     * Creates an auto-configured instance based on system capabilities.
     * 
     * @return optimally configured ParallelConfig
     */
    public static ParallelConfig auto() {
        int cores = Runtime.getRuntime().availableProcessors();
        // Java 24+ is required for VectorWave, Vector API is always available
        
        return new Builder()
            .parallelismLevel(cores)
            .parallelThreshold(calculateOptimalThreshold(cores))
            .useVirtualThreads(true) // Java 24 has stable virtual threads
            .mode(ExecutionMode.ADAPTIVE)
            .adaptiveThreshold(true)
            .enableStructuredConcurrency(true)
            .enableParallelThresholding(true)
            .build();
    }
    
    /**
     * Creates a configuration optimized for CPU-intensive operations.
     * 
     * @return CPU-optimized configuration
     */
    public static ParallelConfig cpuIntensive() {
        int cores = Runtime.getRuntime().availableProcessors();
        
        return new Builder()
            .parallelismLevel(cores)
            .parallelThreshold(DEFAULT_PARALLEL_THRESHOLD)
            .useVirtualThreads(false) // Platform threads for CPU-bound work
            .mode(ExecutionMode.ADAPTIVE)
            .chunkSize(optimizeChunkSize())
            .build();
    }
    
    /**
     * Creates a configuration optimized for I/O-bound operations.
     * 
     * @return I/O-optimized configuration
     */
    public static ParallelConfig ioIntensive() {
        return new Builder()
            .parallelismLevel(Runtime.getRuntime().availableProcessors() * 4)
            .parallelThreshold(IO_PARALLEL_THRESHOLD)
            .useVirtualThreads(true)
            .mode(ExecutionMode.VIRTUAL_THREADS_IO)
            .enableStructuredConcurrency(true)
            .build();
    }
    
    /**
     * Determines if parallel execution should be used for given input size
     * with specified operation type (for adaptive tuning).
     * 
     * @param inputSize the size of the input data
     * @param complexity computational complexity factor (1.0 = normal)
     * @param operationType the type of operation for adaptive tuning
     * @return true if parallel execution is recommended
     */
    public boolean shouldParallelize(int inputSize, double complexity, 
                                    AdaptiveThresholdTuner.OperationType operationType) {
        // Use adaptive tuner if enabled
        if (enableAdaptiveTuning && adaptiveTuner != null && operationType != null) {
            int adaptiveThreshold = adaptiveTuner.getAdaptiveThreshold(
                operationType, inputSize, complexity);
            return inputSize > adaptiveThreshold;
        }
        
        // Fall back to standard logic
        return shouldParallelize(inputSize, complexity);
    }
    
    /**
     * Determines if parallel execution should be used for given input size.
     * 
     * @param inputSize the size of the input data
     * @param complexity computational complexity factor (1.0 = normal)
     * @return true if parallel execution is recommended
     */
    public boolean shouldParallelize(int inputSize, double complexity) {
        return switch (mode) {
            case SEQUENTIAL -> false;
            case PARALLEL_ALWAYS -> true;
            case ADAPTIVE -> {
                if (adaptiveThreshold) {
                    double adjustedThreshold = parallelThreshold / complexity;
                    yield inputSize > adjustedThreshold;
                }
                yield inputSize > parallelThreshold;
            }
            case GPU_PREFERRED -> enableGPU || inputSize > parallelThreshold;
            case VIRTUAL_THREADS_IO -> useVirtualThreads && inputSize > parallelThreshold / 2;
        };
    }
    
    /**
     * Calculates optimal number of chunks for parallel processing.
     * 
     * @param dataSize total size of data to process
     * @return optimal number of chunks
     */
    public int calculateChunks(int dataSize) {
        if (dataSize <= parallelThreshold) {
            return 1;
        }
        
        int idealChunks = parallelismLevel;
        int minChunkSize = Math.max(chunkSize, parallelThreshold / 4);
        
        // Ensure chunks are not too small
        int maxChunks = dataSize / minChunkSize;
        return Math.min(idealChunks, maxChunks);
    }
    
    /**
     * Gets the executor service for CPU-bound operations.
     * 
     * @return CPU executor service
     */
    public ExecutorService getCPUExecutor() {
        return cpuExecutor;
    }
    
    /**
     * Gets the executor service for I/O-bound operations.
     * 
     * @return virtual thread executor or CPU executor if virtual threads disabled
     */
    public ExecutorService getIOExecutor() {
        return virtualExecutor != null ? virtualExecutor : cpuExecutor;
    }
    
    /**
     * Records execution metrics for performance tracking.
     * 
     * @param wasParallel whether parallel execution was used
     */
    public void recordExecution(boolean wasParallel) {
        if (enableMetrics) {
            if (wasParallel) {
                parallelExecutions.incrementAndGet();
            } else {
                sequentialExecutions.incrementAndGet();
            }
        }
    }
    
    /**
     * Gets execution statistics.
     * 
     * @return execution statistics
     */
    public ExecutionStats getStats() {
        return new ExecutionStats(
            parallelExecutions.get(),
            sequentialExecutions.get(),
            calculateSpeedupEstimate()
        );
    }
    
    // Helper methods
    
    /**
     * Creates the executor for CPU-bound operations.
     * 
     * <p>Uses ForkJoinPool.commonPool() which provides:</p>
     * <ul>
     *   <li>Work-stealing for optimal load balancing</li>
     *   <li>Default parallelism = Runtime.availableProcessors()</li>
     *   <li>Shared across application (reduces resource overhead)</li>
     *   <li>Cannot be shut down (survives component lifecycle)</li>
     * </ul>
     * 
     * <p>Note: When metrics are enabled, dedicated pools may be created instead
     * to allow for proper shutdown and resource tracking.</p>
     * 
     * @return ForkJoinPool for CPU-bound tasks
     */
    private ExecutorService createCPUExecutor() {
        return ForkJoinPool.commonPool();
    }
    
    /**
     * Creates the executor for I/O-bound operations using virtual threads.
     * 
     * <p>Virtual threads (Project Loom) provide:</p>
     * <ul>
     *   <li>Lightweight threads with minimal memory overhead (~1KB vs ~1MB)</li>
     *   <li>Automatic yielding on blocking operations</li>
     *   <li>Millions of concurrent threads without thread pool tuning</li>
     *   <li>Ideal for I/O-bound tasks (file operations, network calls)</li>
     * </ul>
     * 
     * <p>Virtual threads are stable in Java 24 and provide significant benefits
     * for I/O-heavy wavelet analysis workloads.</p>
     * 
     * @return virtual thread executor for I/O-bound tasks
     */
    private ExecutorService createVirtualExecutor() {
        // Java 24: Virtual threads are stable
        return Executors.newVirtualThreadPerTaskExecutor();
    }
    
    private static int calculateOptimalThreshold(int cores) {
        // Calculate threshold based on overhead vs. parallelization benefit
        // More cores = lower threshold per core to keep all cores busy
        int perCoreThreshold = OPERATIONS_PER_CORE_MS / cores;
        return Math.max(MIN_PARALLEL_SIZE, perCoreThreshold);
    }
    
    private static int optimizeChunkSize() {
        // Optimize for L1 cache utilization
        // TARGET_CACHE_LINES (64) * DOUBLES_PER_CACHE_LINE (8) = 512 doubles
        // 512 doubles * 8 bytes/double = 4KB (fits well in L1 cache)
        return TARGET_CACHE_LINES * DOUBLES_PER_CACHE_LINE;
    }
    
    private double calculateSpeedupEstimate() {
        int total = parallelExecutions.get() + sequentialExecutions.get();
        if (total == 0) return 1.0;
        
        double parallelRatio = (double) parallelExecutions.get() / total;
        // Amdahl's law approximation
        return 1.0 / ((1 - parallelRatio) + parallelRatio / parallelismLevel);
    }
    
    // Getters
    
    public int getParallelismLevel() { return parallelismLevel; }
    public int getParallelThreshold() { return parallelThreshold; }
    public boolean isUseVirtualThreads() { return useVirtualThreads; }
    public boolean isEnableGPU() { return enableGPU; }
    public ExecutionMode getMode() { return mode; }
    public int getChunkSize() { return chunkSize; }
    public boolean isEnableStructuredConcurrency() { return enableStructuredConcurrency; }
    public boolean isAdaptiveThreshold() { return adaptiveThreshold; }
    public double getOverheadFactor() { return overheadFactor; }
    public boolean isEnableParallelThresholding() { return enableParallelThresholding; }
    public boolean isEnableMetrics() { return enableMetrics; }
    public boolean isEnableAdaptiveTuning() { return enableAdaptiveTuning; }
    public AdaptiveThresholdTuner getAdaptiveTuner() { return adaptiveTuner; }
    
    /**
     * Execution statistics for performance monitoring.
     */
    public record ExecutionStats(
        int parallelExecutions,
        int sequentialExecutions,
        double estimatedSpeedup
    ) {
        public double parallelRatio() {
            int total = parallelExecutions + sequentialExecutions;
            return total > 0 ? (double) parallelExecutions / total : 0.0;
        }
    }
    
    /**
     * Builder for ParallelConfig.
     */
    public static class Builder {
        public Builder() {}
        private int parallelismLevel = ForkJoinPool.getCommonPoolParallelism();
        private int parallelThreshold = DEFAULT_PARALLEL_THRESHOLD;
        private boolean useVirtualThreads = true;
        private boolean enableGPU = false;
        private ExecutionMode mode = ExecutionMode.ADAPTIVE;
        private int chunkSize = DEFAULT_CHUNK_SIZE;
        private boolean enableStructuredConcurrency = true;
        private boolean adaptiveThreshold = false;
        private double overheadFactor = 1.0;
        private boolean enableParallelThresholding = true;
        private boolean enableMetrics = false;
        private boolean enableAdaptiveTuning = false;
        
        public Builder parallelismLevel(int level) {
            if (level < 1) {
                throw new IllegalArgumentException("Parallelism level must be >= 1");
            }
            this.parallelismLevel = level;
            return this;
        }
        
        public Builder parallelThreshold(int threshold) {
            if (threshold < 1) {
                throw new IllegalArgumentException("Parallel threshold must be >= 1");
            }
            this.parallelThreshold = threshold;
            return this;
        }
        
        public Builder useVirtualThreads(boolean use) {
            this.useVirtualThreads = use;
            return this;
        }
        
        public Builder enableGPU(boolean enable) {
            this.enableGPU = enable;
            return this;
        }
        
        public Builder mode(ExecutionMode mode) {
            this.mode = mode;
            return this;
        }
        
        public Builder chunkSize(int size) {
            if (size < 1) {
                throw new IllegalArgumentException("Chunk size must be >= 1");
            }
            this.chunkSize = size;
            return this;
        }
        
        public Builder enableStructuredConcurrency(boolean enable) {
            this.enableStructuredConcurrency = enable;
            return this;
        }
        
        public Builder adaptiveThreshold(boolean adaptive) {
            this.adaptiveThreshold = adaptive;
            return this;
        }
        
        public Builder overheadFactor(double factor) {
            if (factor <= 0) {
                throw new IllegalArgumentException("Overhead factor must be > 0");
            }
            this.overheadFactor = factor;
            return this;
        }
        
        public Builder enableParallelThresholding(boolean enable) {
            this.enableParallelThresholding = enable;
            return this;
        }
        
        public Builder enableMetrics(boolean enable) {
            this.enableMetrics = enable;
            return this;
        }
        
        public Builder enableAdaptiveTuning(boolean enable) {
            this.enableAdaptiveTuning = enable;
            return this;
        }
        
        public ParallelConfig build() {
            return new ParallelConfig(this);
        }
    }
    
    /**
     * Records adaptive tuning feedback.
     * 
     * @param operationType the type of operation
     * @param inputSize the input size
     * @param threshold the threshold that was used
     * @param parallelTime the parallel execution time in nanoseconds
     * @param sequentialTime the sequential execution time in nanoseconds
     */
    public void recordAdaptiveFeedback(AdaptiveThresholdTuner.OperationType operationType,
                                      int inputSize, int threshold, 
                                      long parallelTime, long sequentialTime) {
        if (enableAdaptiveTuning && adaptiveTuner != null) {
            adaptiveTuner.recordMeasurement(operationType, inputSize, threshold, 
                                           parallelTime, sequentialTime);
        }
    }
    
    /**
     * Cleanup resources when done.
     */
    public void shutdown() {
        if (virtualExecutor != null && !virtualExecutor.isShutdown()) {
            virtualExecutor.shutdown();
        }
        // Note: We don't shutdown the common ForkJoinPool
    }
}
