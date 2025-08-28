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
```

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