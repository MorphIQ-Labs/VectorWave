# VectorWave Extensions

SIMD optimizations and performance enhancements for VectorWave using Java 24 Vector API.

## Overview

The `vectorwave-extensions` module provides SIMD acceleration and advanced concurrency features for VectorWave. It leverages Java 24's Vector API (incubator) and structured concurrency (preview) to deliver 2-5x performance improvements over scalar implementations.

## Features

### Vector API (SIMD) Optimizations
- **Platform-specific acceleration**
  - x86: AVX2, AVX512
  - ARM: NEON, SVE
- **Automatic feature detection**
- **Graceful fallback to scalar operations**

### Optimized Implementations
- Vectorized MODWT transforms
- SIMD-accelerated FFT
- Batch processing with aligned memory
- Cache-aware algorithms
- Batch streaming facade (PERIODIC, ZERO_PADDING, SYMMETRIC with SIMD ring-buffer)

### Structured Concurrency
- StructuredTaskScope for reliable parallelism
- Guaranteed resource cleanup
- Timeout and cancellation support

## Installation

```xml
<dependency>
    <groupId>com.morphiqlabs</groupId>
    <artifactId>vectorwave-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.morphiqlabs</groupId>
    <artifactId>vectorwave-extensions</artifactId>
    <version>1.0.0</version>
    <scope>runtime</scope>
</dependency>
```

## Requirements

- Java 24 or higher
- JVM flags for Vector API: `--add-modules jdk.incubator.vector`
- JVM flags for structured concurrency: `--enable-preview`
- Recommended: GraalVM 24.0.2 for additional 10-20% performance

## Performance Benefits

### Measured Speedups

Real-world benchmarks on modern hardware show significant performance improvements:

| Operation | Signal Size | Core Only | Extensions | Speedup |
|-----------|------------|-----------|------------|---------|
| MODWT Forward | 1,024 | 72µs | 28µs | **2.6x** |
| MODWT Forward | 16,384 | 1.62ms | 0.47ms | **3.5x** |
| MODWT Forward | 65,536 | 7.26ms | 1.86ms | **3.9x** |
| Round-trip | 16,384 | 2.84ms | 0.76ms | **3.7x** |
| Batch (16×4K) | 65,536 total | 5.73ms | 0.98ms | **5.8x** |
| 5-Level Decomp | 16,384 | 8.12ms | 2.32ms | **3.5x** |
| Denoising | 16,384 | 14.26ms | 3.68ms | **3.9x** |
| CWT (32 scales) | 8,192 | 92.6ms | 28.4ms | **3.3x** |

### Performance Scaling

Extensions provide better scaling with signal size:
- Small signals (< 1K): 2-2.5x speedup
- Medium signals (1K-16K): 3-3.5x speedup  
- Large signals (> 16K): 3.5-4.5x speedup
- Batch processing: Up to 5.8x speedup

### SIMD Utilization

Vector API automatically selects optimal SIMD width:
- **x86_64**: AVX2 (256-bit) or AVX512 (512-bit)
- **ARM64**: NEON (128-bit) or SVE (variable)
- **Efficiency**: 85-95% SIMD unit utilization

## Automatic Optimization

Extensions are automatically detected and used when available:

```java
// Same API - automatically uses SIMD if extensions present
MODWTTransform transform = new MODWTTransform(wavelet, mode);
MODWTResult result = transform.forward(signal); // Uses SIMD internally
```

## Verifying Extensions

Check if optimizations are active:

```java
// Check Vector API support
if (VectorAPI.isSupported()) {
    System.out.println("SIMD acceleration enabled");
    System.out.println("Vector length: " + VectorAPI.getPreferredSpecies());
}

// Service discovery
ServiceLoader<TransformOptimizer> optimizers = 
    ServiceLoader.load(TransformOptimizer.class);
for (TransformOptimizer opt : optimizers) {
    System.out.println("Found optimizer: " + opt.getClass().getName());
}
```

## Platform Support

| Platform | SIMD | Status |
|----------|------|--------|
| x86_64 (Intel/AMD) | AVX2, AVX512 | ✅ Full support |
| ARM64 (Apple M1/M2) | NEON | ✅ Full support |
| ARM64 (Server) | NEON, SVE | ✅ Full support |

## Adding Extensions to Your Project

No code changes required! Simply add the extensions module as a dependency:

```xml
<!-- Before: Core only -->
<dependency>
    <groupId>com.morphiqlabs</groupId>
    <artifactId>vectorwave-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- After: Core + Extensions -->
<dependency>
    <groupId>com.morphiqlabs</groupId>
    <artifactId>vectorwave-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.morphiqlabs</groupId>
    <artifactId>vectorwave-extensions</artifactId>
    <version>1.0.0</version>
    <scope>runtime</scope>
</dependency>
```

## Documentation

- [Performance Guide](../docs/PERFORMANCE.md)
- [Benchmark Results](../docs/BENCHMARK-RESULTS.md)
- [GraalVM Optimization](../docs/GRAALVM-OPTIMIZATION-GUIDE.md)
- [Batch Processing Guide](../docs/guides/BATCH_PROCESSING.md)

## License

GPL-3.0 - See [LICENSE](../LICENSE) file for details.
