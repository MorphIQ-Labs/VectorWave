# VectorWave Examples

Comprehensive examples, demos, and benchmarks for the VectorWave library.

## Overview

The `vectorwave-examples` module provides practical examples, performance benchmarks, and educational demos showcasing VectorWave's capabilities. All examples demonstrate both core and extension features.

## Structure

```
vectorwave-examples/
├── src/main/java/com/morphiqlabs/
│   ├── benchmark/          # Performance benchmarks
│   ├── demo/              # Interactive demonstrations
│   ├── examples/          # Code examples
│   │   ├── basic/        # Getting started examples
│   │   └── finance/      # Financial analysis examples
│   └── Main.java         # Main entry point
```

## Running Examples

### Quick Start
```bash
# Run main demo
mvn exec:java -pl vectorwave-examples

# Run specific demo
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="com.morphiqlabs.demo.SWTDemo"
```

### Basic Examples

#### Simple MODWT Transform
```bash
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="com.morphiqlabs.examples.BasicExample"
```

#### Financial Analysis
```bash
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="com.morphiqlabs.examples.finance.WaveletSharpeExample"
```

## Benchmarks

### Running Benchmarks

```bash
# Run all benchmarks
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="com.morphiqlabs.benchmark.BenchmarkRunner"

# Quick mode (fewer iterations)
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="com.morphiqlabs.benchmark.BenchmarkRunner" \
  -Dexec.args="--quick"

# Specific benchmark
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="com.morphiqlabs.benchmark.MODWTBenchmark"

# VectorOps allocation + FMA microbenchmarks (with GC profiler)
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="org.openjdk.jmh.Main" \
  -Dexec.args="com.morphiqlabs.benchmark.VectorOpsAllocationBenchmark -prof gc -f 1 -wi 5 -i 10"
```

### Available Benchmarks

| Benchmark | Description | Key Metrics |
|-----------|-------------|-------------|
| `CoreVsExtensionsBenchmark` | Compare scalar vs SIMD | Throughput, speedup |
| `MODWTBenchmark` | MODWT performance | Time per transform |
| `MemoryEfficiencyBenchmark` | Memory usage patterns | Allocation rate, GC |
| `ParallelVsSequentialBenchmark` | Parallel processing | Scaling efficiency |

### Benchmark Results

Results are saved in `benchmark-results/` directory:
- JSON format for detailed analysis
- CSV format for spreadsheet import

## Demos

### Interactive Demos

| Demo | Description |
|------|-------------|
| `SWTDemo` | Stationary Wavelet Transform demonstration |
| `StreamingFinancialDemo` | Real-time financial data processing |
| `WaveletSelectionGuideDemo` | Choosing the right wavelet |
| `MemoryEfficiencyMODWTDemo` | Memory-efficient processing |
| `ParallelDenoisingDemo` | Parallel signal denoising |

### Running Demos

```bash
# SWT demonstration
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="com.morphiqlabs.demo.SWTDemo"

# Financial streaming demo
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="com.morphiqlabs.demo.StreamingFinancialDemo"

# Runtime learning demo (enable calibration for effect)
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="com.morphiqlabs.demo.RuntimeLearningDemo" \
  -Dvectorwave.perf.calibration=true \
  -Djava.util.logging.config.file=$(pwd)/../docs/examples/logging.properties \
  -Dvectorwave.log.level=INFO
```

## Logging Quick Start

To keep example output focused, you can configure Java Util Logging (JUL) to reduce internal logs.

Use the provided config:
```bash
mvn -Djava.util.logging.config.file=$(pwd)/../docs/examples/logging.properties \
    -Dvectorwave.log.level=WARNING \
    -pl vectorwave-examples -am test
```

This sets VectorWave to WARNING and uses the console handler with a simple formatter. See the main configuration guide for more options.

## Educational Examples

### Basic Level
- Getting started with MODWT
- Wavelet selection
- Simple denoising
- Basic reconstruction

### Intermediate Level
- Multi-level decomposition
- Custom wavelets
- Batch processing
- Memory optimization

### Advanced Level
- Real-time streaming
- Financial metrics
- Performance tuning
- Custom optimizations

## Requirements

- Java 24 or higher
- Maven 3.6+
- 2GB heap for benchmarks (`-Xmx2g`)
- JVM flags: `--add-modules jdk.incubator.vector --enable-preview`

## Building JAR with Dependencies

```bash
# Build executable JAR
mvn clean package -pl vectorwave-examples

# Run JAR
java --add-modules jdk.incubator.vector --enable-preview \
  -jar target/vectorwave-examples-1.0.0-jar-with-dependencies.jar
```

## Performance Tips

1. **Enable Vector API**: Always use `--add-modules jdk.incubator.vector`
2. **Heap Size**: Use `-Xmx2g` for benchmarks
3. **GC Tuning**: G1GC recommended (`-XX:+UseG1GC`)
4. **Warm-up**: Allow JIT compilation warm-up for accurate measurements

## Resource Management

- Prefer try-with-resources for components that implement `AutoCloseable` to ensure deterministic cleanup:
  - `VectorWaveSwtAdapter` (core SWT adapter — manages lazy caches and a dedicated executor)
  - `MODWTStreamingTransform` and `MODWTStreamingDenoiser` (streaming APIs)
  - `ParallelWaveletDenoiser`, `ParallelMultiLevelTransform` (extensions — close owned `ParallelConfig` resources)

Example:
```java
try (VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC)) {
    var result = swt.forward(signal, 4);
    double[] recon = swt.inverse(result);
}

try (MODWTStreamingTransform xform = MODWTStreamingTransform.create(Daubechies.DB4, BoundaryMode.PERIODIC, 256)) {
    xform.process(chunk);
}

try (ParallelWaveletDenoiser denoiser = new ParallelWaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC)) {
    double[] y = denoiser.denoiseMultiLevel(x, 4, WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT);
}
```

Note: If you construct extension components with an explicit `ParallelConfig`, call `config.shutdown()` yourself after use (the component will not shut down external configs).

## Contributing Examples

To add new examples:

1. Place in appropriate package:
   - `benchmark/` for performance tests
   - `demo/` for interactive demos
   - `examples/basic/` for simple examples
   - `examples/finance/` for financial use cases

2. Follow naming conventions:
   - Benchmarks: `*Benchmark.java`
   - Demos: `*Demo.java`
   - Examples: `*Example.java`

3. Include documentation:
   - Javadoc with description
   - Usage instructions
   - Expected output

## Documentation

- [Main Documentation](../docs/README.md)
- [Examples Guide](../docs/development/EXAMPLES.md)
- [Benchmarking Guide](../docs/development/BENCHMARKING.md)
- [Performance Results](../docs/BENCHMARK-RESULTS.md)

## License

GPL-3.0 - See [LICENSE](../LICENSE) file for details.
#### Batch Streaming with Flush Tail (SYMMETRIC/ZERO_PADDING)
```bash
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="com.morphiqlabs.examples.BatchStreamingFlushExample"
```
Minimal example that streams blocks, then emits a synthetic tail using `BatchStreamingMODWT.flushMultiLevel(...)`.
Use `suggestFlushTailLength()` or `getMinFlushTailLength()` to select a valid tail length.
