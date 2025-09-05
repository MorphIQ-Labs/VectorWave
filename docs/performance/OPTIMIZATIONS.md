# VectorWave Small Signal Optimizations

## Overview

This document describes the performance optimizations integrated into the canonical VectorWave implementation for small signal sizes (<1024 samples), specifically targeted at financial time series analysis. All optimizations are transparently applied - no API changes are required.

## Latest Performance Updates

- **Nanosecond Latencies**: Achieved 107-294 ns for 64-sample signals
- **SIMD Integration**: Configurable scalar/SIMD paths via TransformConfig
- **Thread Safety**: Fixed concurrent access issues with atomic operations
- **Streaming Support**: Optimized for real-time processing without buffering

## Implemented Optimizations

### 1. Bitwise Modulo Operations
- **Location**: `ScalarOps.java`
- **Impact**: ~10-15% performance improvement
- **Description**: Automatically uses bitwise AND for power-of-2 signal lengths
- **Example**: `(index % length)` → `(index & (length - 1))` when length is power-of-2

### 2. Specialized Wavelet Implementations
- **Haar Wavelet**: Fully unrolled 2-tap convolution
- **DB2 Wavelet**: Unrolled 4-tap convolution  
- **DB4 Wavelet**: Combined transform for cache efficiency
- **Impact**: Up to 40% improvement for Haar, 20% for DB2

### 3. Fused Validation
- **Location**: `ValidationUtils.java`
- **Impact**: Reduced validation overhead by 50%
- **Description**: Automatically combines validation checks into single pass for small signals

### 4. Memory-Efficient Transform Results
- **Location**: `ImmutableTransformResult.java`
- **Impact**: Reduced memory allocations for result access
- **Description**: Provides read-only views instead of defensive copies for internal operations

### 5. Cache-Friendly Combined Transform
- **Location**: `ScalarOps.combinedTransformPeriodic()` used by `WaveletTransform`
- **Impact**: Better cache utilization for small signals
- **Description**: Automatically processes both filters in single pass for periodic boundary mode

### 6. SIMD/Vector API Support
- **Location**: `VectorOps.java` with configurable paths
- **Impact**: Performance improvements for larger signals
- **Description**: Optional hardware acceleration with minimal overhead for small signals. Vector paths now:
  - Reuse per-thread scratch buffers (indices, lanes) to eliminate temporary allocations in hot loops
  - Use fused multiply-add (FMA) where available to reduce instruction count and improve numerical stability
- **Configuration**: Use `TransformConfig.forceScalar()` or `forceVector()` for explicit control

#### Microbenchmarks (allocations and FMA)

- See `VectorOpsAllocationBenchmark` in `vectorwave-benchmarks` for scalar vs vector comparisons and allocation profiling.
- Run with GC profiler:
  ```bash
  mvn -q -pl vectorwave-benchmarks -am exec:java \
    -Dexec.mainClass=org.openjdk.jmh.Main \
    -Dexec.args="com.morphiqlabs.benchmark.VectorOpsAllocationBenchmark -prof gc -f 1 -wi 5 -i 10"
  ```
  Expect near-zero allocations/op for vector paths in steady state.

## Performance Results

### Benchmark Results (1000 iterations)
```
Signal Size | Wavelet | Standard Time | Optimized Time | Speedup
------------|---------|---------------|----------------|--------
256         | Haar    | 10,012 ns     | 8,016 ns       | 1.25x
512         | Haar    | 16,836 ns     | 9,550 ns       | 1.76x
1024        | Haar    | 18,751 ns     | 11,651 ns      | 1.61x
256         | DB4     | 16,548 ns     | 11,799 ns      | 1.40x
512         | DB4     | 10,309 ns     | 7,077 ns       | 1.46x
1024        | DB4     | 24,004 ns     | 13,902 ns      | 1.73x
```

### Mathematical Correctness
- Maximum reconstruction error: < 1e-10 for all wavelets
- All optimization tests pass
- Perfect reconstruction maintained

## Usage

### Basic Usage
```java
// Optimizations are automatically applied
Wavelet wavelet = WaveletRegistry.getWavelet(WaveletName.HAAR);
MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);

// Forward transform (uses optimizations transparently)
TransformResult result = transform.forward(signal);

// Inverse transform
double[] reconstructed = transform.inverse(result);
```

### Batch Processing
```java
// Process multiple signals efficiently
WaveletTransform transform = new WaveletTransform(wavelet, BoundaryMode.PERIODIC);

TransformResult[] results = new TransformResult[signals.length];
for (int i = 0; i < signals.length; i++) {
    results[i] = transform.forward(signals[i]);
}
```

### Financial Time Series Example
```java
// Convert prices to log returns
double[] logReturns = computeLogReturns(prices);

// Pad to next power of 2 if needed
double[] padded = padToPowerOfTwo(logReturns);

// Analyze with automatic optimizations
WaveletTransform transform = new WaveletTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
TransformResult result = transform.forward(padded);

// Extract volatility from detail coefficients
double volatility = computeVolatility(result.detailCoeffs());
```

## Architecture

### Integration Strategy
- **ScalarOps**: Automatically selects optimized implementations based on signal/filter characteristics
- **ValidationUtils**: Fast path for small signal validation
- **WaveletTransform**: Uses combined transform for periodic boundary mode
- **ImmutableTransformResult**: Available as memory-efficient alternative

### Design Decisions
1. **Transparent Integration**: All optimizations in canonical classes
2. **Automatic Selection**: Runtime detection of optimization opportunities
3. **No API Changes**: Existing code benefits without modification
4. **Maintainability**: Single implementation to maintain

## Future Optimizations

1. **SIMD/Vector API**: Implement Java 21 Vector API for further speedups
2. **GPU Acceleration**: For batch processing of many signals
3. **Specialized Financial Wavelets**: Wavelets optimized for return series
4. **Streaming Support**: Process data in chunks for real-time analysis

## Testing

Run optimization tests:
```bash
mvn test -Dtest=ScalarOpsOptimizationTest
```

Run benchmarks:
```bash
./jmh-runner.sh SmallSignalBenchmark
```

Run demonstrations:
```bash
java -cp target/classes com.morphiqlabs.FinancialOptimizationDemo
```
