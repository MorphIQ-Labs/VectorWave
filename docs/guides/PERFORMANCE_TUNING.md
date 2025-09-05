# Performance Tuning Guide: Structured Concurrency

This guide covers performance optimization strategies for VectorWave's structured concurrency implementation, helping you achieve optimal performance for your specific workloads.

## Table of Contents

1. [Performance Overview](#performance-overview)
2. [Configuration Parameters](#configuration-parameters)
3. [Adaptive Threshold Tuning](#adaptive-threshold-tuning)
4. [Workload-Specific Optimization](#workload-specific-optimization)
5. [Monitoring and Profiling](#monitoring-and-profiling)
6. [Hardware Considerations](#hardware-considerations)
7. [Common Performance Issues](#common-performance-issues)
8. [Benchmarking Guidelines](#benchmarking-guidelines)

## Performance Overview

### Key Performance Factors

1. **Parallelization Threshold**: When to switch from sequential to parallel processing
2. **Thread Pool Sizing**: Optimal number of worker threads
3. **Chunk Size**: How to divide work among threads
4. **Memory Layout**: Cache-friendly data organization
5. **SIMD Integration**: Vector API optimization coordination

### Performance Characteristics

| Workload Type | Optimal Configuration | Expected Speedup |
|---------------|----------------------|------------------|
| **Small Signals (< 512 samples)** | Sequential processing | 1.0x (no overhead) |
| **Medium Signals (512-4096 samples)** | Moderate parallelization | 1.5-2.5x |
| **Large Signals (> 4096 samples)** | Full parallelization | 2.0-4.0x |
| **Batch Processing (many signals)** | High parallelization | 3.0-6.0x |

## Configuration Parameters

### ParallelConfig Builder

```java
ParallelConfig config = ParallelConfig.builder()
    .targetCores(cores)              // Number of worker threads
    .minParallelThreshold(threshold) // Minimum work size for parallelization
    .maxChunkSize(chunkSize)        // Maximum work unit size
    .build();
```

### Parameter Guidelines

#### targetCores
- **CPU-bound tasks**: `Runtime.getRuntime().availableProcessors()`
- **I/O-bound tasks**: `availableProcessors() * 2`
- **Mixed workloads**: `availableProcessors() * 1.5`
- **NUMA systems**: Consider topology-aware allocation

```java
// Automatic detection
int optimalCores = Math.min(
    Runtime.getRuntime().availableProcessors(),
    estimateOptimalThreads(workloadCharacteristics)
);

ParallelConfig config = ParallelConfig.builder()
    .targetCores(optimalCores)
    .build();
```

#### minParallelThreshold
Controls when to use parallel vs sequential processing:

```java
// For different signal characteristics
int threshold = switch (signalType) {
    case AUDIO_SAMPLES -> 256;      // Small audio chunks
    case FINANCIAL_TICKS -> 512;    // Financial time series
    case SCIENTIFIC_DATA -> 1024;   // Large scientific datasets
    case REAL_TIME -> 128;          // Low-latency processing
};
```

#### maxChunkSize
Limits work unit size for better cache utilization:

```java
// Cache-aware chunk sizing
int l3CacheSize = estimateL3CacheSize(); // e.g., 32MB
int sampleSize = Double.BYTES;
int optimalChunkSize = Math.min(
    8192, // Default maximum
    l3CacheSize / (sampleSize * 4) // Account for working set
);
```

### Workload-Specific Configurations

#### High-Throughput Batch Processing
```java
ParallelConfig batchConfig = ParallelConfig.builder()
    .targetCores(Runtime.getRuntime().availableProcessors())
    .minParallelThreshold(128)  // Low threshold for high parallelization
    .maxChunkSize(4096)
    .build();

StructuredParallelTransform transform = new StructuredParallelTransform(
    wavelet, boundaryMode, batchConfig);
```

#### Low-Latency Real-Time Processing
```java
ParallelConfig realTimeConfig = ParallelConfig.builder()
    .targetCores(Math.min(4, Runtime.getRuntime().availableProcessors()))
    .minParallelThreshold(256)  // Higher threshold to avoid overhead
    .maxChunkSize(2048)
    .build();
```

#### Memory-Constrained Environments
```java
ParallelConfig memoryConfig = ParallelConfig.builder()
    .targetCores(Runtime.getRuntime().availableProcessors() / 2)
    .minParallelThreshold(1024) // Higher threshold
    .maxChunkSize(2048)         // Smaller chunks
    .build();
```

## Adaptive Threshold Tuning

### Multi-Armed Bandit Optimization

VectorWave includes an adaptive threshold tuner that uses machine learning to optimize parallel thresholds based on actual performance:

```java
AdaptiveThresholdTuner tuner = new AdaptiveThresholdTuner();

// Process multiple batches with automatic optimization
for (int batch = 0; batch < numBatches; batch++) {
    // Get adaptive threshold for current workload
    int adaptiveThreshold = tuner.getAdaptiveThreshold(
        AdaptiveThresholdTuner.OperationType.MODWT_DECOMPOSE,
        signals[0].length,      // Data size
        1.0                     // Complexity factor
    );
    
    ParallelConfig adaptiveConfig = ParallelConfig.builder()
        .minParallelThreshold(adaptiveThreshold)
        .build();
    
    long startTime = System.nanoTime();
    
    try (var executor = new StructuredExecutor(adaptiveConfig)) {
        List<MODWTResult> results = executor.invokeAll(signals,
            signal -> transform.forward(signal));
        
        long elapsed = System.nanoTime() - startTime;
        
        // Record performance for future optimization
        tuner.recordMeasurement(
            AdaptiveThresholdTuner.OperationType.MODWT_DECOMPOSE,
            signals[0].length,
            adaptiveThreshold,
            elapsed,
            estimatedSequentialTime
        );
    }
}
```

### Tuning Parameters

#### Operation Types
```java
// Different operations have different optimal thresholds
AdaptiveThresholdTuner.OperationType operationType = switch (transformType) {
    case "MODWT_FORWARD" -> AdaptiveThresholdTuner.OperationType.MODWT_DECOMPOSE;
    case "MODWT_INVERSE" -> AdaptiveThresholdTuner.OperationType.MODWT_RECONSTRUCT;
    case "CWT_FORWARD" -> AdaptiveThresholdTuner.OperationType.CWT_ANALYSIS;
    case "DENOISING" -> AdaptiveThresholdTuner.OperationType.MODWT_DECOMPOSE;
};
```

#### Complexity Factors
Adjust complexity based on wavelet and signal characteristics:

```java
double complexityFactor = calculateComplexity(wavelet, boundaryMode, signalNoise);

// More complex operations benefit from higher thresholds
double complexity = switch (wavelet.getName()) {
    case "HAAR" -> 0.5;           // Simple wavelet
    case "DB4", "DB8" -> 1.0;     // Standard complexity
    case "BIOR_4_4" -> 1.2;       // Biorthogonal (more complex)
    case "COIF4" -> 1.3;          // Coiflet (highest complexity)
    default -> 1.0;
};
```

### Convergence Monitoring

```java
// Enable debug output to monitor tuner convergence
System.setProperty("debug.tuner", "true");

// Check convergence after warm-up period
if (batch > WARMUP_BATCHES) {
    double confidence = tuner.getConfidenceLevel(operationType, dataSize);
    if (confidence > 0.95) {
        System.out.println("Threshold tuning has converged with high confidence");
    }
}
```

## Workload-Specific Optimization

### Financial Time Series Analysis

```java
// Configure for financial data characteristics
ParallelConfig financialConfig = ParallelConfig.builder()
    .targetCores(Runtime.getRuntime().availableProcessors())
    .minParallelThreshold(512)  // Financial ticks are typically small
    .maxChunkSize(2048)
    .build();

// Use Paul wavelet for asymmetric crash detection
StructuredParallelTransform financialTransform = new StructuredParallelTransform(
    new PaulWavelet(4), BoundaryMode.PERIODIC, financialConfig);

// Process with progress monitoring for large datasets
MODWTResult[] results = financialTransform.forwardBatchWithProgress(priceSignals,
    (completed, total) -> {
        if (completed % 100 == 0) { // Update every 100 signals
            System.out.printf("Financial analysis: %d/%d signals processed%n", 
                completed, total);
        }
    });
```

### Scientific Computing

```java
// High-precision scientific computing configuration
ParallelConfig scientificConfig = ParallelConfig.builder()
    .targetCores(Runtime.getRuntime().availableProcessors())
    .minParallelThreshold(1024) // Larger thresholds for complex computations
    .maxChunkSize(8192)
    .build();

// Use Daubechies wavelets for good frequency localization
StructuredParallelTransform scientificTransform = new StructuredParallelTransform(
    Daubechies.DB8, BoundaryMode.ZERO_PADDING, scientificConfig);
```

### Real-Time Audio Processing

```java
// Low-latency configuration for audio processing
ParallelConfig audioConfig = ParallelConfig.builder()
    .targetCores(4) // Limit cores to avoid thread scheduling overhead
    .minParallelThreshold(256)  // Audio frames are small
    .maxChunkSize(1024)
    .build();

// Use Haar for fast processing
StructuredParallelTransform audioTransform = new StructuredParallelTransform(
    new Haar(), BoundaryMode.PERIODIC, audioConfig);

// Process with timeout for real-time constraints
try {
    MODWTResult[] results = audioTransform.forwardBatchWithTimeout(audioFrames, 10); // 10ms deadline
} catch (StructuredParallelTransform.ComputationException e) {
    // Fallback to sequential processing for missed deadline
    MODWTResult[] results = sequentialTransform.forwardBatch(audioFrames);
}
```

## Monitoring and Profiling

### Built-in Performance Monitoring

```java
// Enable performance monitoring
System.setProperty("debug.executor", "true");

try (var executor = new StructuredExecutor(config)) {
    long startTime = System.nanoTime();
    
    List<MODWTResult> results = executor.invokeAll(signals,
        signal -> transform.forward(signal));
    
    long elapsed = System.nanoTime() - startTime;
    
    // Calculate performance metrics
    double signalsPerSecond = (signals.size() * 1e9) / elapsed;
    double samplesPerSecond = (totalSamples * 1e9) / elapsed;
    double parallelEfficiency = calculateEfficiency(elapsed, sequentialTime, coreCount);
    
    System.out.printf("Performance: %.1f signals/sec, %.1f samples/sec, %.1f%% efficiency%n",
        signalsPerSecond, samplesPerSecond, parallelEfficiency * 100);
}
```

### JMH Benchmarking

Use JMH for accurate performance measurements:

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class StructuredConcurrencyBenchmark {
    
    private StructuredParallelTransform transform;
    private double[][] signals;
    
    @Setup
    public void setup() {
        ParallelConfig config = ParallelConfig.builder()
            .targetCores(Runtime.getRuntime().availableProcessors())
            .minParallelThreshold(512)
            .build();
            
        transform = new StructuredParallelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, config);
        signals = generateTestSignals(100, 1024);
    }
    
    @Benchmark
    public MODWTResult[] benchmarkParallelTransform() {
        return transform.forwardBatch(signals);
    }
    
    @Benchmark
    @Param({"128", "256", "512", "1024", "2048"})
    public MODWTResult[] benchmarkThresholdSweep(int threshold) {
        ParallelConfig config = ParallelConfig.builder()
            .minParallelThreshold(threshold)
            .build();
            
        StructuredParallelTransform thresholdTransform = new StructuredParallelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, config);
        
        return thresholdTransform.forwardBatch(signals);
    }
}
```

### Memory Profiling

```java
// Monitor memory usage during batch processing
MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();

long beforeMemory = memoryBean.getHeapMemoryUsage().getUsed();

try (var executor = new StructuredExecutor(config)) {
    List<MODWTResult> results = executor.invokeAll(signals,
        signal -> transform.forward(signal));
    
    long afterMemory = memoryBean.getHeapMemoryUsage().getUsed();
    long memoryUsed = afterMemory - beforeMemory;
    
    System.out.printf("Memory usage: %.2f MB for %d signals%n",
        memoryUsed / (1024.0 * 1024.0), signals.size());
}
```

## Hardware Considerations

### CPU Architecture Optimization

#### x86-64 Systems
```java
// Optimize for x86 systems with AVX2/AVX512
ParallelConfig x86Config = ParallelConfig.builder()
    .targetCores(Runtime.getRuntime().availableProcessors())
    .minParallelThreshold(256)  // Take advantage of wide SIMD
    .maxChunkSize(8192)         // Larger chunks for vector processing
    .build();
```

#### ARM Systems (Apple Silicon, AWS Graviton)
```java
// Optimize for ARM systems with NEON
ParallelConfig armConfig = ParallelConfig.builder()
    .targetCores(Runtime.getRuntime().availableProcessors())
    .minParallelThreshold(512)  // NEON has narrower SIMD than AVX512
    .maxChunkSize(4096)
    .build();
```

### NUMA Awareness

For NUMA systems, consider thread affinity:

```java
// Check NUMA topology
boolean isNUMASystem = Runtime.getRuntime().availableProcessors() > 16;

if (isNUMASystem) {
    // Use fewer cores per NUMA node to reduce memory latency
    int coresPerNode = Runtime.getRuntime().availableProcessors() / 2;
    
    ParallelConfig numaConfig = ParallelConfig.builder()
        .targetCores(coresPerNode)
        .minParallelThreshold(1024) // Higher threshold for NUMA
        .build();
}
```

### Memory Hierarchy Optimization

#### Cache-Aware Processing
```java
// Estimate cache sizes and configure accordingly
long l3CacheSize = estimateL3CacheSize(); // Platform-specific estimation
int optimalChunkSize = (int) Math.min(
    8192,
    l3CacheSize / (Double.BYTES * 8) // Account for coefficient working set
);

ParallelConfig cacheConfig = ParallelConfig.builder()
    .maxChunkSize(optimalChunkSize)
    .build();
```

#### Prefetching Strategies
```java
// Configure for different memory access patterns
ParallelConfig prefetchConfig = switch (accessPattern) {
    case SEQUENTIAL -> ParallelConfig.builder()
        .maxChunkSize(8192)    // Large chunks for sequential access
        .build();
    case RANDOM -> ParallelConfig.builder()
        .maxChunkSize(2048)    // Smaller chunks for random access
        .build();
    case STRIDED -> ParallelConfig.builder()
        .maxChunkSize(4096)    // Medium chunks for strided access
        .build();
};
```

## Common Performance Issues

### Issue 1: Over-Parallelization

**Symptoms:**
- Parallel processing slower than sequential
- High CPU usage with low throughput
- Thread contention in profiler

**Diagnosis:**
```java
// Enable debug mode to see threshold decisions
System.setProperty("debug.tuner", "true");

// Monitor thread utilization
ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
long[] threadIds = threadBean.getAllThreadIds();
System.out.println("Active threads: " + threadIds.length);
```

**Solutions:**
```java
// Increase threshold to reduce parallelization
ParallelConfig fixedConfig = ParallelConfig.builder()
    .minParallelThreshold(2048) // Higher threshold
    .targetCores(Math.min(4, Runtime.getRuntime().availableProcessors()))
    .build();

// Or use adaptive tuning
AdaptiveThresholdTuner tuner = new AdaptiveThresholdTuner();
int adaptiveThreshold = tuner.getAdaptiveThreshold(operationType, dataSize, 1.0);
```

### Issue 2: Memory Pressure

**Symptoms:**
- Increasing garbage collection frequency
- OutOfMemoryError exceptions
- Degraded performance over time

**Diagnosis:**
```java
// Monitor memory usage patterns
MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
long heapMax = memoryBean.getHeapMemoryUsage().getMax();
double heapUtilization = (double) heapUsed / heapMax;

if (heapUtilization > 0.8) {
    System.out.println("WARNING: High heap utilization: " + heapUtilization);
}
```

**Solutions:**
```java
// Process in smaller batches
int batchSize = Math.min(originalBatchSize, 
    (int) (availableMemory / estimatedSignalMemory));

for (int i = 0; i < signals.length; i += batchSize) {
    double[][] batch = Arrays.copyOfRange(signals, i, 
        Math.min(i + batchSize, signals.length));
    
    try (var executor = new StructuredExecutor(config)) {
        List<MODWTResult> batchResults = executor.invokeAll(Arrays.asList(batch),
            signal -> transform.forward(signal));
        // Process results immediately
    }
    
    // Optional: suggest GC between batches for large datasets
    if (batchSize < originalBatchSize) {
        System.gc();
    }
}
```

### Issue 3: Timeout Issues

**Symptoms:**
- ComputionException with timeout messages
- Inconsistent completion times
- Tasks completing after timeout

**Solutions:**
```java
// Use adaptive timeout based on workload
long estimatedTime = estimateProcessingTime(signals);
long adaptiveTimeout = Math.max(
    estimatedTime * 2,      // 2x buffer
    5000                    // Minimum 5 second timeout
);

// Implement graceful degradation
try {
    return transform.forwardBatchWithTimeout(signals, adaptiveTimeout);
} catch (StructuredParallelTransform.ComputationException e) {
    if (e.getMessage().contains("timeout")) {
        // Fall back to smaller batch or sequential processing
        return fallbackProcessing(signals);
    }
    throw e;
}
```

## Benchmarking Guidelines

### Establishing Baselines

```java
// Sequential baseline
long sequentialStart = System.nanoTime();
List<MODWTResult> sequentialResults = new ArrayList<>();
for (double[] signal : signals) {
    sequentialResults.add(transform.forward(signal));
}
long sequentialTime = System.nanoTime() - sequentialStart;

// Parallel implementation
long parallelStart = System.nanoTime();
List<MODWTResult> parallelResults = executor.invokeAll(signals,
    signal -> transform.forward(signal));
long parallelTime = System.nanoTime() - parallelStart;

// Calculate speedup
double speedup = (double) sequentialTime / parallelTime;
double efficiency = speedup / Runtime.getRuntime().availableProcessors();

System.out.printf("Speedup: %.2fx, Efficiency: %.2f%%\n", 
    speedup, efficiency * 100);
```

### Comprehensive Performance Testing

```java
public class PerformanceSuite {
    
    @Test
    public void benchmarkScaling() {
        int[] signalCounts = {1, 5, 10, 25, 50, 100, 200, 500};
        int[] signalLengths = {256, 512, 1024, 2048, 4096};
        
        for (int count : signalCounts) {
            for (int length : signalLengths) {
                double[][] signals = generateTestSignals(count, length);
                
                // Measure performance
                long startTime = System.nanoTime();
                MODWTResult[] results = transform.forwardBatch(signals);
                long elapsed = System.nanoTime() - startTime;
                
                double throughput = (count * 1e9) / elapsed;
                
                System.out.printf("Count: %d, Length: %d, Throughput: %.1f signals/sec%n",
                    count, length, throughput);
            }
        }
    }
    
    @Test
    public void benchmarkThresholdSweep() {
        int[] thresholds = {64, 128, 256, 512, 1024, 2048, 4096};
        double[][] signals = generateTestSignals(100, 1024);
        
        for (int threshold : thresholds) {
            ParallelConfig config = ParallelConfig.builder()
                .minParallelThreshold(threshold)
                .build();
                
            StructuredParallelTransform testTransform = new StructuredParallelTransform(
                Daubechies.DB4, BoundaryMode.PERIODIC, config);
            
            // Warm up
            for (int i = 0; i < 5; i++) {
                testTransform.forwardBatch(signals);
            }
            
            // Measure
            long startTime = System.nanoTime();
            for (int i = 0; i < 10; i++) {
                testTransform.forwardBatch(signals);
            }
            long elapsed = System.nanoTime() - startTime;
            
            double avgTime = elapsed / (10.0 * 1e6); // ms
            System.out.printf("Threshold: %d, Avg Time: %.2f ms%n", threshold, avgTime);
        }
    }
}
```

### Performance Regression Testing

```java
// Automated performance regression detection
public void testPerformanceRegression() {
    double[][] signals = generateTestSignals(100, 1024);
    
    // Establish baseline (store in version control or database)
    double baselineTime = getBaselineTime("structured_parallel_forward_batch");
    
    // Current performance
    long startTime = System.nanoTime();
    MODWTResult[] results = transform.forwardBatch(signals);
    long elapsed = System.nanoTime() - startTime;
    double currentTime = elapsed / 1e6; // ms
    
    // Check for regression (more than 10% slower)
    double regressionThreshold = 1.1;
    if (currentTime > baselineTime * regressionThreshold) {
        fail(String.format("Performance regression detected: %.2f ms > %.2f ms (%.1f%% slower)",
            currentTime, baselineTime, 
            ((currentTime / baselineTime) - 1) * 100));
    }
    
    // Update baseline if significantly faster
    if (currentTime < baselineTime * 0.95) {
        updateBaseline("structured_parallel_forward_batch", currentTime);
    }
}
```

## Conclusion

Performance tuning structured concurrency requires understanding both your workload characteristics and the underlying hardware. Key strategies include:

1. **Start with appropriate defaults** based on workload type
2. **Use adaptive tuning** for automatic optimization
3. **Monitor performance continuously** during development
4. **Profile systematically** to identify bottlenecks
5. **Test across different hardware configurations**
6. **Establish regression testing** to maintain performance

The structured concurrency implementation in VectorWave provides both automatic optimizations and fine-grained control for performance-critical applications. Combine built-in adaptive tuning with workload-specific configuration for optimal results.
## SIMD Integration (Extensions)

SIMD optimizations via the Java Vector API are available in the optional `vectorwave-extensions` module (Java 24 + incubator). Core paths are scalar Java 21. To enable SIMD:

```
--add-modules jdk.incubator.vector --enable-preview
```
