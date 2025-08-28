# VectorWave Configuration Guide

VectorWave provides several configurable parameters to optimize performance for different environments and use cases.

## Parallelization Thresholds

### 1. VectorWaveSwtAdapter Parallel Threshold

Controls when the SWT adapter switches to parallel processing for multi-level decomposition.

**Default:** 4096

**Configuration via ParallelConfig:**
```java
ParallelConfig config = new ParallelConfig.Builder()
    .parallelThreshold(2048)  // Use parallel processing for signals >= 2048
    .build();

VectorWaveSwtAdapter adapter = new VectorWaveSwtAdapter(
    Daubechies.DB4, BoundaryMode.PERIODIC, config);
```

### 2. FFT Vector Threshold (OptimizedFFT)

Controls when FFT operations use vectorized SIMD instructions vs scalar fallback.

**Default:** 256

**System Property:** `-Dvectorwave.fft.vector.threshold=512`

**Environment Variable:** `VECTORWAVE_FFT_VECTOR_THRESHOLD=512`

**Example Usage:**
```bash
# Via system property
java -Dvectorwave.fft.vector.threshold=512 -cp ... MyApp

# Via environment variable
export VECTORWAVE_FFT_VECTOR_THRESHOLD=512
java -cp ... MyApp
```

### 3. CWT FFT Threshold

Controls when CWT transforms use FFT acceleration vs direct convolution.

**Default:** 64

**System Property:** `-Dvectorwave.cwt.fft.threshold=128`

**Environment Variable:** `VECTORWAVE_CWT_FFT_THRESHOLD=128`

**Example Usage:**
```bash
# Via system property  
java -Dvectorwave.cwt.fft.threshold=128 -cp ... MyApp

# Via environment variable
export VECTORWAVE_CWT_FFT_THRESHOLD=128
java -cp ... MyApp
```

## ParallelConfig Options

The ParallelConfig class provides comprehensive control over parallel execution:

```java
ParallelConfig config = new ParallelConfig.Builder()
    .parallelThreshold(1024)           // Signal size threshold for parallelization
    .parallelismLevel(8)               // Number of parallel threads
    .useVirtualThreads(true)           // Use virtual threads (Java 21+)
    .enableMetrics(true)               // Enable performance metrics
    .enableParallelThresholding(true)  // Use parallel coefficient thresholding
    .chunkSize(512)                    // Chunk size for parallel processing
    .build();
```

## Configuration Priority

For system properties and environment variables:

1. **System Property** (highest priority)
2. **Environment Variable** (medium priority)  
3. **Default Value** (lowest priority)

## Performance Tuning Guidelines

### Small Embedded Systems
```java
// Reduce parallelization to save memory
ParallelConfig embedded = new ParallelConfig.Builder()
    .parallelThreshold(8192)      // Higher threshold - less parallelization
    .parallelismLevel(2)          // Fewer threads
    .useVirtualThreads(false)     // Use platform threads
    .enableMetrics(false)         // Disable overhead
    .build();
```

### High-Performance Servers
```java  
// Maximize parallelization for throughput
ParallelConfig server = new ParallelConfig.Builder()
    .parallelThreshold(256)       // Lower threshold - more parallelization
    .parallelismLevel(16)         // More threads
    .useVirtualThreads(true)      // Use virtual threads for I/O
    .enableMetrics(true)          // Enable monitoring
    .chunkSize(1024)              // Larger chunks
    .build();
```

### Desktop Applications
```java
// Balanced configuration
ParallelConfig desktop = new ParallelConfig.Builder()
    .parallelThreshold(1024)      // Standard threshold
    .parallelismLevel(Runtime.getRuntime().availableProcessors())
    .useVirtualThreads(true)      // Modern Java features
    .enableMetrics(false)         // Reduce overhead
    .build();
```

## Environment Variable Examples

```bash
# Configure all thresholds via environment
export VECTORWAVE_FFT_VECTOR_THRESHOLD=512
export VECTORWAVE_CWT_FFT_THRESHOLD=128

# Run application
java --add-modules jdk.incubator.vector -cp ... MyApp
```

## Monitoring and Diagnostics

Enable metrics to monitor parallelization effectiveness:

```java
ParallelConfig config = new ParallelConfig.Builder()
    .enableMetrics(true)
    .build();

// Use the config...

// Check statistics
ParallelConfig.ExecutionStats stats = config.getStats();
System.out.printf("Parallel executions: %d, Sequential: %d%n", 
    stats.getParallelExecutions(), stats.getSequentialExecutions());
```

## Best Practices

1. **Profile First:** Use JMH benchmarks to measure actual performance impact
2. **Environment-Specific:** Tune thresholds for your specific hardware
3. **Monitor Metrics:** Enable metrics in development to understand behavior
4. **Conservative Defaults:** Start with higher thresholds and lower gradually
5. **Test Thoroughly:** Verify correctness across all configuration combinations