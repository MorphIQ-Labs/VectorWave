# VectorWave

[![Java 21+](https://img.shields.io/badge/Java-21%2B-blue.svg)](https://openjdk.org/projects/jdk/21/)
[![Maven](https://img.shields.io/badge/Maven-3.6%2B-green.svg)](https://maven.apache.org/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-yellow.svg)](https://www.gnu.org/licenses/gpl-3.0)

High-performance wavelet transform library for Java 21+ featuring MODWT (Maximal Overlap Discrete Wavelet Transform) as the primary transform. Offers shift-invariance, arbitrary signal length support, SIMD acceleration, and comprehensive wavelet families for financial analysis, signal processing, and real-time applications.

## Project Structure

VectorWave is organized as a modular Maven project:

- **vectorwave-core**: Core wavelet transforms, algorithms, and base functionality
- **vectorwave-extensions**: SIMD optimizations, performance enhancements, and platform-specific code
- **vectorwave-examples**: Demo applications, benchmarks, and usage examples

## Features

### Core Capabilities
- **MODWT**: Shift-invariant transform for ANY signal length
- **SWT Adapter**: Stationary Wavelet Transform interface with mutable coefficients
- **Structured Concurrency**: Java 24 preview features for guaranteed resource cleanup
- **Wavelet Families**: Haar, Daubechies, Symlets, Coiflets, Biorthogonal, Financial wavelets
- **CWT**: FFT-accelerated continuous transforms with complex analysis
- **SIMD Acceleration**: Automatic vectorization with scalar fallback
- **Financial Analysis**: Specialized wavelets and configurable parameters
- **Streaming**: Real-time processing with arbitrary block sizes
- **Zero Dependencies**: Pure Java implementation

### Performance
- **SIMD Acceleration**: Platform-adaptive Vector API (x86: AVX2/AVX512, ARM: NEON)
  - 2-4x speedup on MODWT transforms
  - 3-5x speedup on batch processing
  - Automatic fallback to optimized scalar code
- **Structured Concurrency**: Automatic resource management with guaranteed cleanup
- **Adaptive Thresholding**: Machine learning optimization of parallel execution
- **FFT Optimization**: Real-to-complex FFT with 2x speedup for real signals
- **Batch Processing**: SIMD parallel processing of multiple signals
  - 16 signals (4K samples): 4.3x speedup vs sequential
  - Near-linear scaling up to 8 cores
- **Memory Efficiency**: 24 bytes/sample with object pooling
- **Benchmark Results**: [Detailed performance data](docs/BENCHMARK-RESULTS.md)

### Key Applications
- **Financial Analysis**: Crash detection, volatility analysis, regime identification
- **Signal Processing**: Denoising, time-frequency analysis, feature extraction
- **Real-time Systems**: Streaming transforms with microsecond latency
- **Scientific Computing**: Multi-level decomposition and reconstruction

## Requirements

- Java 21+ for core module, Java 24 for extensions (GraalVM 24.0.2 recommended)
- Maven 3.6+
- Compilation: `--add-modules jdk.incubator.vector --enable-preview`
- Runtime: `--enable-preview` for structured concurrency
- Vector API optional (automatic scalar fallback)
- **Recommended**: GraalVM 24.0.2 for 10-20% additional performance

## Quick Start

### Building from Source

```bash
# Clone the repository
git clone https://github.com/MorphIQ-Labs/VectorWave.git
cd VectorWave

# Build all modules
mvn clean install

# Run tests with Vector API enabled
mvn test -Dtest.args="--add-modules jdk.incubator.vector --enable-preview"

# Run interactive demos
cd vectorwave-examples
mvn exec:java -Dexec.mainClass="com.morphiqlabs.Main"
```

### Maven Dependency

```xml
<dependency>
    <groupId>com.morphiqlabs</groupId>
    <artifactId>vectorwave-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- For SIMD optimizations (optional) -->
<dependency>
    <groupId>com.morphiqlabs</groupId>
    <artifactId>vectorwave-extensions</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Basic Usage

```java
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.wavelets.Haar;
import com.morphiqlabs.wavelet.BoundaryMode;

MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
double[] signal = {1, 2, 3, 4, 5, 6, 7}; // Any length!
MODWTResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result);
```

## Key Examples

### 1. Basic MODWT Transform
```java
// Works with ANY signal length - no power-of-2 restriction!
MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
double[] signal = new double[777]; // Any length
MODWTResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result); // Perfect reconstruction
```

### 2. High-Performance Batch Processing
```java
// Process multiple signals with automatic SIMD optimization
double[][] signals = new double[32][1000]; // 32 signals of any length
MODWTResult[] results = transform.forwardBatch(signals); // 2-4x speedup
double[][] reconstructed = transform.inverseBatch(results);
```

### 3. Financial Analysis
```java
// Configure financial analysis parameters
FinancialConfig config = new FinancialConfig(0.045); // 4.5% risk-free rate
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);

// Wavelet-based Sharpe ratio calculation
double sharpeRatio = analyzer.calculateWaveletSharpeRatio(returns);

// Crash asymmetry detection using Paul wavelet
PaulWavelet paulWavelet = new PaulWavelet(4);
CWTTransform cwt = new CWTTransform(paulWavelet);
CWTResult crashAnalysis = cwt.analyze(priceReturns, scales);
```

### 4. Real-time Streaming
```java
// Streaming denoiser with arbitrary block sizes
MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
    .wavelet(Daubechies.DB4)
    .bufferSize(480) // 10ms at 48kHz - no padding needed!
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .build();

// Process continuous stream
for (double[] chunk : audioStream) {
    double[] denoised = denoiser.denoise(chunk);
}
```

### 5. SWT (Stationary Wavelet Transform)
```java
// SWT adapter for shift-invariant denoising and analysis
VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC);

// Decompose with mutable coefficients for custom processing
MutableMultiLevelMODWTResult result = swt.forward(signal, 4);

// Apply universal threshold for denoising
swt.applyUniversalThreshold(result, true); // soft thresholding

// Or use convenience denoising method
double[] denoised = swt.denoise(noisySignal, 4, -1, true); // auto threshold

// Extract specific frequency bands
double[] highFreq = swt.extractLevel(signal, 4, 1); // finest details
```

### 6. Structured Concurrency (Java 24 Preview)
```java
// Automatic resource management with guaranteed cleanup
try (var executor = new StructuredExecutor()) {
    // Submit multiple tasks with automatic cancellation propagation
    List<Future<MODWTResult>> futures = executor.submitAll(
        List.of(signal1, signal2, signal3),
        signal -> transform.forward(signal)
    );
    
    executor.joinAll(); // Wait for all tasks to complete
    
    // Collect results - all tasks complete or none do
    List<MODWTResult> results = new ArrayList<>();
    for (var future : futures) {
        results.add(future.get());
    }
} // Automatic cleanup - all tasks cancelled if scope exits early

// Parallel batch processing with timeout
ParallelConfig config = ParallelConfig.builder()
    .targetCores(4)
    .minParallelThreshold(256)
    .build();

StructuredParallelTransform parallelTransform = new StructuredParallelTransform(
    Daubechies.DB4, BoundaryMode.PERIODIC, config);

// Process with timeout - throws ComputationException if exceeded
double[][] signals = generateLargeDataset();
MODWTResult[] results = parallelTransform.forwardBatchWithTimeout(signals, 5000); // 5s timeout
```

### 7. Performance Monitoring
```java
// Check platform capabilities
WaveletOperations.PerformanceInfo info = WaveletOperations.getPerformanceInfo();
System.out.println(info.description());
// Output: "Vectorized operations enabled on aarch64 with S_128_BIT"

// Estimate processing time
MODWTTransform.ProcessingEstimate estimate = transform.estimateProcessingTime(signalLength);
System.out.println(estimate.description());
```

## Wavelet Selection Guide

| Wavelet | Best For | Key Properties |
|---------|----------|----------------|
| **Haar** | Fast processing, edge detection | Simplest, compact support |
| **Daubechies** | General purpose, compression | Orthogonal, good frequency localization |
| **Paul** | Financial crash detection | Asymmetric, captures sharp rises/falls |
| **Morlet** | Time-frequency analysis | Complex, good time-frequency balance |
| **Mexican Hat** | Edge detection, volatility | Second derivative of Gaussian |
| **Shannon-Gabor** | Spectral analysis | Reduced artifacts vs classical Shannon |

## Performance Highlights

Based on extensive benchmarking with GraalVM 24.0.2 on modern hardware:

| Operation | Signal Size | Time (Core) | Time (Extensions) | Speedup |
|-----------|------------|-------------|-------------------|---------|
| MODWT Forward | 16K samples | 1.62ms | 0.47ms | **3.5x** |
| MODWT Round-trip | 16K samples | 2.84ms | 0.76ms | **3.7x** |
| Batch (16 signals) | 4K each | 5.73ms | 0.98ms | **5.8x** |
| 5-Level Decomposition | 16K samples | 8.12ms | 2.32ms | **3.5x** |
| Denoising | 16K samples | 14.26ms | 3.68ms | **3.9x** |

**Key Performance Metrics (with GraalVM):**
- Throughput: 35M+ samples/second with SIMD
- Latency: Sub-millisecond for 4K signals  
- Memory: 24 bytes per sample overhead
- Scaling: Near-linear up to 8 cores
- GraalVM Boost: Additional 10-20% over OpenJDK

See [detailed benchmarks](docs/BENCHMARK-RESULTS.md) and [GraalVM optimization guide](docs/GRAALVM-OPTIMIZATION-GUIDE.md).

## Documentation

### Core Documentation
- [API Reference](docs/API.md) - Complete API documentation  
- [Architecture Overview](docs/ARCHITECTURE.md) - System design and module structure
- [Configuration Guide](docs/CONFIGURATION.md) - Configuration options and parameters

### User Guides
- [Developer Guide](docs/guides/DEVELOPER_GUIDE.md) - Getting started with development
- [Wavelet Selection Guide](docs/WAVELET_SELECTION.md) - Choosing the right wavelet
- [SWT Guide](docs/guides/SWT.md) - Stationary Wavelet Transform usage
- [Denoising Guide](docs/guides/DENOISING.md) - Signal denoising techniques
- [Financial Analysis Guide](docs/guides/FINANCIAL_ANALYSIS.md) - Market analysis applications
- [Streaming Guide](docs/guides/STREAMING.md) - Real-time signal processing
- [Batch Processing Guide](docs/guides/BATCH_PROCESSING.md) - SIMD optimization for bulk operations

### Performance
- [Performance Guide](docs/PERFORMANCE.md) - Performance characteristics and tuning
- [Benchmark Results](docs/BENCHMARK-RESULTS.md) - Detailed benchmark data
- [GraalVM Optimization](docs/GRAALVM-OPTIMIZATION-GUIDE.md) - GraalVM-specific optimizations
- [Performance Tuning](docs/guides/PERFORMANCE_TUNING.md) - Advanced performance optimization

### Technical Specifications
- [Transform Compatibility](docs/TRANSFORM_COMPATIBILITY.md) - Transform comparison and compatibility
- [Wavelet Registry Best Practices](docs/WAVELET_REGISTRY-BEST_PRACTICES.md) - Using the wavelet registry
- [SWT Best Practices](docs/SWT_BEST_PRACTICES.md) - Best practices for SWT usage
- [Financial Wavelets](docs/FINANCIAL_WAVELETS.md) - Specialized wavelets for finance

### Development
- [Adding New Wavelets](docs/development/ADDING_WAVELETS.md) - Extending wavelet families
- [Benchmarking Guide](docs/development/BENCHMARKING.md) - Running and creating benchmarks
- [Examples Guide](docs/development/EXAMPLES.md) - Creating demo applications
- [Wavelet Provider SPI](docs/development/WAVELET_PROVIDER_SPI.md) - Service provider interface

## Demos and Examples

Run interactive demos from the vectorwave-examples module:

```bash
cd vectorwave-examples

# Interactive menu with all demos
mvn exec:java -Dexec.mainClass="com.morphiqlabs.Main"

# Specific demos
mvn exec:java -Dexec.mainClass="com.morphiqlabs.demo.MODWTDemo"
mvn exec:java -Dexec.mainClass="com.morphiqlabs.demo.FinancialDemo"
mvn exec:java -Dexec.mainClass="com.morphiqlabs.demo.LiveTradingSimulation"
mvn exec:java -Dexec.mainClass="com.morphiqlabs.demo.StreamingDenoiserDemo"
mvn exec:java -Dexec.mainClass="com.morphiqlabs.demo.BatchProcessingDemo"
mvn exec:java -Dexec.mainClass="com.morphiqlabs.demo.SWTDemo"

# Run benchmarks
mvn exec:java -Dexec.mainClass="com.morphiqlabs.benchmarks.MODWTBenchmark"
```

**Available Demos**:
- `MODWTDemo` - Basic MODWT transforms and reconstruction
- `FinancialDemo` - Financial analysis with specialized wavelets
- `StreamingDenoiserDemo` - Real-time signal denoising
- `BatchProcessingDemo` - SIMD-accelerated batch processing
- `SWTDemo` - Stationary Wavelet Transform operations
- `LiveTradingSimulation` - Simulated trading with wavelet analysis

## Contributing

Contributions are welcome! Please read the [Developer Guide](docs/guides/DEVELOPER_GUIDE.md) and ensure:

1. All tests pass: `mvn test`
2. Code follows existing patterns and conventions
3. New features include appropriate tests
4. Documentation is updated for API changes

## Support

- **Issues**: [GitHub Issues](https://github.com/MorphIQ-Labs/VectorWave/issues)
- **Discussions**: [GitHub Discussions](https://github.com/MorphIQ-Labs/VectorWave/discussions)
- **Documentation**: [Full Documentation](docs/README.md)

## License

GPL-3.0 - See [LICENSE](LICENSE) file for details.

## Citation

If you use VectorWave in your research, please cite:

```bibtex
@software{vectorwave2025,
  title = {VectorWave: High-Performance Wavelet Transform Library for Java},
  year = {2025},
  url = {https://github.com/MorphIQ-Labs/VectorWave}
}
```

