# VectorWave Documentation

> Comprehensive documentation for the VectorWave high-performance wavelet transform library for Java 21+

## Quick Links

| Getting Started | API & Reference | Performance | Examples |
|-----------------|-----------------|-------------|----------|
| [Architecture](ARCHITECTURE.md) | [API Reference](API.md) | [Performance Guide](PERFORMANCE.md) | [Examples](examples/) |
| [Developer Guide](guides/DEVELOPER_GUIDE.md) | [Wavelet Selection](WAVELET_SELECTION.md) | [Benchmarks](BENCHMARK-RESULTS.md) | [Demos](../vectorwave-examples/) |
| [Configuration](CONFIGURATION.md) | [Transform Compatibility](TRANSFORM_COMPATIBILITY.md) | [GraalVM Optimization](GRAALVM-OPTIMIZATION-GUIDE.md) | [Tutorials](guides/) |

## Documentation Structure

### 📚 Core Documentation

| Document | Description | Status |
|----------|-------------|--------|
| [API Reference](API.md) | Complete API documentation with examples | ✅ Current |
| [Architecture Overview](ARCHITECTURE.md) | System design and modular structure | ✅ Current |
| [Configuration Guide](CONFIGURATION.md) | Performance tuning and optimization settings | ✅ Current |
| [Performance Guide](PERFORMANCE.md) | Performance characteristics and benchmarks | ✅ Current |
| [Benchmark Results](BENCHMARK-RESULTS.md) | Detailed performance measurements | ✅ Current |
| [Wavelet Selection Guide](WAVELET_SELECTION.md) | Choosing the right wavelet for your application | ✅ Current |
| [Transform Compatibility](TRANSFORM_COMPATIBILITY.md) | Transform comparison and selection guide | ✅ Current |
| [SWT Best Practices](SWT_BEST_PRACTICES.md) | Stationary Wavelet Transform usage patterns | ✅ Current |
| [Wavelet Registry Best Practices](WAVELET_REGISTRY-BEST_PRACTICES.md) | Using the wavelet registry effectively | ✅ Current |
| [Financial Wavelets](FINANCIAL_WAVELETS.md) | Specialized wavelets for market analysis | ✅ Current |
| [GraalVM Optimization](GRAALVM-OPTIMIZATION-GUIDE.md) | GraalVM-specific performance optimizations | ✅ Current |

### 📖 User Guides

**Getting Started**
- [Developer Guide](guides/DEVELOPER_GUIDE.md) - Getting started with VectorWave development
- [Wavelet Properties](guides/WAVELET_PROPERTIES.md) - Understanding wavelet characteristics

**Transform Guides**
- [SWT Usage](guides/SWT.md) - Stationary Wavelet Transform guide
- [Structured Concurrency](guides/STRUCTURED_CONCURRENCY.md) - Java 24 concurrency features

**Application Guides**
- [Financial Analysis](guides/FINANCIAL_ANALYSIS.md) - Financial market analysis with wavelets
- [Streaming Financial Analysis](guides/STREAMING_FINANCIAL_ANALYSIS.md) - Real-time financial processing
- [Batch Processing](guides/BATCH_PROCESSING.md) - SIMD-accelerated batch operations
- [Streaming](guides/STREAMING.md) - Real-time signal processing
- [Denoising](guides/DENOISING.md) - Signal denoising techniques
- [Performance Tuning](guides/PERFORMANCE_TUNING.md) - Advanced optimization strategies

### 🔧 Development Documentation

| Guide | Description | Audience |
|-------|-------------|----------|
| [Adding New Wavelets](development/ADDING_WAVELETS.md) | Implementing custom wavelet families | Contributors |
| [Benchmarking Guide](development/BENCHMARKING.md) | Running and creating performance tests | Developers |
| [Examples Organization](development/EXAMPLES.md) | Structuring demo applications | Contributors |
| [Wavelet Provider SPI](development/WAVELET_PROVIDER_SPI.md) | Service provider interface for extensions | Advanced Users |

### 🔬 Technical Specifications

**Algorithms & Verification**
- [FFT Algorithm Analysis](technical-specs/FFT_ALGORITHM_ANALYSIS.md) - FFT implementation details
- [Wavelet Verification Report](technical-specs/WAVELET_VERIFICATION_REPORT.md) - Mathematical verification
- [Vector API Compilation](technical-specs/VECTOR_API_COMPILATION.md) - SIMD compilation guide
- [Vector API Fallback](technical-specs/VECTOR_API_FALLBACK.md) - Scalar fallback mechanisms

### 🔐 Internal Documentation

**Implementation Details** (Advanced users only)
- [Thread Safety](internals/THREAD_SAFETY.md) - Concurrency guarantees and locking strategies
- [Adaptive Padding & Thread Safety](internals/ADAPTIVE_PADDING_THREAD_SAFETY.md) - Padding algorithm details
- [Memory Pool Lifecycle](internals/MEMORY_POOL_LIFECYCLE.md) - Memory management internals
- [Batch SIMD MODWT](internals/BATCH_SIMD_MODWT.md) - SIMD batch processing implementation
- [FFT Canonical References](internals/FFT_CANONICAL_REFERENCES.md) - FFT algorithm sources
- [FFT Periodicity Implementation](internals/FFT_PERIODICITY_IMPLEMENTATION.md) - Periodic boundary handling
- [Shannon Wavelets Comparison](internals/SHANNON_WAVELETS_COMPARISON.md) - Shannon vs Shannon-Gabor
- [Wavelet Coefficient Sources](internals/WAVELET_COEFFICIENT_SOURCES.md) - Coefficient derivation
- [Wavelet Normalization](internals/WAVELET_NORMALIZATION.md) - Normalization strategies

### 💡 Examples & Demos

**Documentation**
- [Structured Concurrency Examples](examples/STRUCTURED_CONCURRENCY_EXAMPLES.md) - Java 24 concurrency patterns

**Live Demos** (in `vectorwave-examples` module)
- `MODWTDemo` - Basic MODWT transforms
- `FinancialDemo` - Financial market analysis
- `StreamingDenoiserDemo` - Real-time denoising
- `BatchProcessingDemo` - SIMD batch operations
- `SWTDemo` - Stationary wavelet transforms
- `LiveTradingSimulation` - Simulated trading

## 🚀 Quick Start Paths

### For New Users
1. **Start Here** → [Architecture Overview](ARCHITECTURE.md)
2. **Learn API** → [API Reference](API.md)  
3. **Choose Wavelet** → [Selection Guide](WAVELET_SELECTION.md)
4. **Run Examples** → [Developer Guide](guides/DEVELOPER_GUIDE.md)

### For Application Developers
1. **Financial Apps** → [Financial Analysis Guide](guides/FINANCIAL_ANALYSIS.md)
2. **Signal Processing** → [Denoising Guide](guides/DENOISING.md)
3. **Real-time Systems** → [Streaming Guide](guides/STREAMING.md)
4. **High Performance** → [Batch Processing](guides/BATCH_PROCESSING.md)

### For Contributors
1. **Setup** → [Developer Guide](guides/DEVELOPER_GUIDE.md)
2. **Extend** → [Adding New Wavelets](development/ADDING_WAVELETS.md)
3. **Test** → [Benchmarking Guide](development/BENCHMARKING.md)
4. **Optimize** → [Performance Tuning](guides/PERFORMANCE_TUNING.md)

### For Performance Engineers
1. **Benchmarks** → [Performance Results](BENCHMARK-RESULTS.md)
2. **GraalVM** → [GraalVM Optimization](GRAALVM-OPTIMIZATION-GUIDE.md)
3. **SIMD** → [Batch Processing](guides/BATCH_PROCESSING.md)
4. **Tuning** → [Performance Tuning](guides/PERFORMANCE_TUNING.md)

## 📋 Documentation Standards

### Conventions
- **Code Examples**: All examples tested with Java 21+
- **Performance Data**: Benchmarked on modern hardware with GraalVM 24.0.2
- **Version Compatibility**: Documentation matches version 1.0.1-SNAPSHOT

### Document Types
| Type | Purpose | Audience |
|------|---------|----------|
| **User Guides** | How to use features | Application developers |
| **API Reference** | Detailed API documentation | All developers |
| **Technical Specs** | Algorithm & math details | Researchers, contributors |
| **Internal Docs** | Implementation details | Core contributors |
| **Examples** | Working code samples | All users |

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/MorphIQ-Labs/VectorWave/issues)
- **Discussions**: [GitHub Discussions](https://github.com/MorphIQ-Labs/VectorWave/discussions)
- **Main README**: [Project Overview](../README.md)