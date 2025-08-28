# VectorWave Performance Benchmarking Guide

This guide explains how to run and interpret performance benchmarks for the VectorWave library using JMH (Java Microbenchmark Harness).

## Quick Start

Run all benchmarks:
```bash
./jmh-runner.sh
```

## Prerequisites

- Java 21+
- Maven 3.6+
- Sufficient heap memory (4GB+ recommended)
- For SIMD benchmarks: 
  - x86: CPU with AVX2/AVX512 support
  - ARM: NEON support (standard on modern ARM)
  - Apple Silicon: Automatic optimization for M-series chips
- Optional: Java Vector API support (Java 16+ as incubator module)
  - Automatically detected by jmh-runner.sh
  - Falls back to scalar implementation if not available

## Vector API Configuration

### Automatic Detection

The `jmh-runner.sh` script automatically detects if the Vector API module is available:

```bash
# Run all benchmarks
./jmh-runner.sh

# Run specific benchmark
./jmh-runner.sh OptimizedFFTBenchmark
```

### Manual Configuration

#### With Vector API (Java 16+ with incubator modules)
```bash
JAVA_OPTS="-Xmx2G --add-modules=jdk.incubator.vector" ./jmh-runner.sh
```

#### Without Vector API (Scalar fallback)
```bash
JAVA_OPTS="-Xmx2G" ./jmh-runner.sh
```

### Checking Vector API Status

To verify if benchmarks are using Vector API:

```bash
# Check if module is available
java --list-modules | grep jdk.incubator.vector

# Check runtime status
mvn compile && java -cp target/classes --add-modules=jdk.incubator.vector \
  -cp target/classes com.morphiqlabs.wavelet.util.OptimizedFFT
```

## Available Benchmarks

### 1. SIMD Performance Comparison

Compares scalar vs SIMD-optimized operations across different signal sizes.

```bash
./jmh-runner.sh SIMDBenchmark
```

**Parameters:**
- Signal sizes: 64, 128, 256, 512, 1024, 2048, 4096
- Boundary modes: PERIODIC, ZERO_PADDING
- Measures: Performance difference between scalar and vector operations
- Includes financial signal benchmarks
- **Note**: Platform-adaptive thresholds - Apple Silicon benefits from SIMD with signals ≥ 8 elements

### 2. Signal Size Scaling

Measures performance across different signal sizes to understand scaling characteristics.

```bash
./jmh-runner.sh SignalSizeBenchmark
```

**Parameters:**
- Signal sizes: 256, 512, 1024, 2048, 4096, 8192, 16384
- Measures: Throughput and average time
- Includes cold-start and batch processing tests

### 3. Wavelet Type Comparison

Compares performance across different wavelet families.

```bash
./jmh-runner.sh WaveletTypeBenchmark
```

**Parameters:**
- Wavelet types: Haar, Daubechies-2 (DB2), Daubechies-4 (DB4)
- Measures: Transform time for each wavelet type
- Fixed signal size: 4096 samples

### 4. Validation Performance

Measures the overhead of input validation.

```bash
./jmh-runner.sh ValidationBenchmark
```

**Parameters:**
- Various validation scenarios
- Measures validation overhead in nanoseconds

### 5. Batch Validation Performance

Measures batch validation efficiency.

```bash
./jmh-runner.sh BatchValidationBenchmark
```

**Parameters:**
- Batch sizes: 10, 100, 1000 signals
- Signal sizes: 256, 1024, 4096
- Measures: Throughput of batch validation

### 6. Quick Performance Test

A lightweight benchmark for quick performance checks.

```bash
./jmh-runner.sh QuickPerformanceTest
```

**Parameters:**
- Limited iterations for fast results
- Good for regression testing

### 7. Vector Optimization Comparison

Compares original VectorOps vs optimized VectorOps implementations.

```bash
./jmh-runner.sh VectorOptimizationBenchmark
```

**Parameters:**
- Signal sizes: 128, 256, 512, 1024, 2048, 4096
- Filter lengths: 4, 8 (DB2 and DB4)
- Measures: Performance of convolution, combined transforms, and Haar optimization
- Uses 3 forks for statistical reliability

### 8. Real-Time Application Benchmarks

Measures performance for real-time use cases like audio processing and financial tick data.

```bash
./jmh-runner.sh RealTimeBenchmark
```

**Parameters:**
- Audio buffer sizes: 64, 128, 256, 512 samples
- Measures: Latency, throughput, and memory allocation overhead
- Scenarios: Audio processing, financial tick batches, sensor data filtering
- Includes real-time denoising benchmarks

### 9. Latency-Focused Benchmarks

Detailed latency analysis for real-time constraints.

```bash
./jmh-runner.sh LatencyBenchmark
```

**Parameters:**
- Signal sizes: 16, 32, 64, 128, 256 samples
- Measures: Percentile latencies (50th, 90th, 95th, 99th, 99.9th)
- Tests: Jitter, GC impact, thread contention, allocation overhead
- Wavelet comparison: Haar vs DB2 vs DB4 latency
- **Recent Results**: Haar ~107 ns/op, DB2 ~193 ns/op, DB4 ~294 ns/op (64 samples)
- **Thread Safety**: Fixed indexing collision with AtomicInteger

### 10. Cache Prefetch Optimization Benchmarks

Measures the impact of cache prefetching optimizations on large signal processing.

```bash
./jmh-runner.sh PrefetchBenchmark
```

**Parameters:**
- Signal sizes: 256, 1024, 4096, 16384, 65536 samples
- Wavelets: Haar, DB4, Sym8
- Compares: Standard vs prefetch-optimized transforms
- Measures: Impact of cache-friendly access patterns
- Includes: Multi-level transform prefetch benefits
- Baseline: Random access pattern to show cache miss impact

### 11. Small Signal Optimization

Focused benchmarks for very small signals common in real-time applications.

```bash
./jmh-runner.sh SmallSignalBenchmark
```

**Parameters:**
- Signal sizes: 8, 16, 32, 64, 128 samples
- Wavelets: Haar, DB2, DB4
- Measures: Optimizations for small buffer processing
- Platform-specific: Apple Silicon optimizations for 8-element signals

### 12. Phase 4 Optimization Benchmarks

Measures advanced optimization strategies.

```bash
./jmh-runner.sh Phase4OptimizationBenchmark
```

**Parameters:**
- Various optimization techniques
- Memory pooling efficiency
- Cache-aware transformations

### 13. General Optimization Benchmarks

Comprehensive optimization comparison.

```bash
./jmh-runner.sh OptimizationBenchmark
```

**Parameters:**
- Full optimization suite comparison
- Scalar vs SIMD vs cache-aware
- Memory allocation patterns

## JMH Parameters

Customize benchmark execution with these parameters:

```bash
# Example: Run with custom warmup and measurement iterations
./jmh-runner.sh -wi 10 -i 20

# Example: Run specific benchmark with fork count
./jmh-runner.sh SignalSizeBenchmark -f 3

# Example: Override parameter values
./jmh-runner.sh SignalSizeBenchmark -p signalSize=2048,8192
```

### Common Parameters:
- `-wi N`: Warmup iterations (default: 5)
- `-i N`: Measurement iterations (default: 10)
- `-f N`: Fork count (default: 2)
- `-t N`: Thread count (default: 1)
- `-p param=value`: Override @Param values
- `-rf format`: Result format (json, csv, text)
- `-rff file`: Result file

## Interpreting Results

### Throughput Metrics
- **ops/s**: Operations per second (higher is better)
- **ms/op**: Milliseconds per operation (lower is better)
- **samples/sec**: Signal samples processed per second

### Understanding Scores
```
Benchmark                       Mode  Cnt     Score    Error  Units
SignalSizeBenchmark.forward    thrpt   10  1234.567 ± 12.345  ops/s
                                           ^^^^^^^^^   ^^^^^^
                                           throughput  std dev
```

### Performance Guidelines

Expected performance characteristics:
- **Linear scaling**: Transform time should scale linearly with signal size
- **Wavelet complexity**: Haar < DB2 < DB4 in terms of computation time
- **Memory efficiency**: No significant GC pressure during normal operation

## Advanced Usage

### Running from Source

```bash
# Using the JMH runner script (recommended)
./jmh-runner.sh SignalSizeBenchmark

# Or manually compile and run
mvn test-compile
java -cp target/test-classes:target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q) \
    org.openjdk.jmh.Main SignalSizeBenchmark
```

### Profiling Integration

```bash
# Run with async profiler
./jmh-runner.sh SignalSizeBenchmark -prof async

# Run with GC profiler
./jmh-runner.sh ValidationBenchmark -prof gc
```

### Custom Benchmark Development

Create new benchmarks by extending the base benchmark class:

```java
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class MyBenchmark {
    @Benchmark
    public void myTest() {
        // benchmark code
    }
}
```

## Troubleshooting

### Out of Memory
The JMH runner script already sets heap size, but you can modify it in the benchmark's @Fork annotation or run manually:
```bash
java -Xmx8G -cp target/test-classes:target/classes:$(mvn dependency:build-classpath -Dmdep.outputFile=/dev/stdout -q) \
    org.openjdk.jmh.Main SignalSizeBenchmark
```

### Inconsistent Results
- Ensure no other CPU-intensive processes are running
- Use more warmup iterations: `-wi 10`
- Increase fork count: `-f 3`

### Slow Execution
- Reduce iteration count for quick tests: `-wi 1 -i 1`
- Run specific benchmarks instead of all

## Best Practices

1. **Isolation**: Run benchmarks on a quiet system
2. **Warmup**: Allow sufficient warmup for JIT compilation
3. **Multiple runs**: Use forks to ensure consistency
4. **Baseline**: Always compare against a known baseline
5. **Documentation**: Record environment details with results