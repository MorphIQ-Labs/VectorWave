# VectorWave Development Guide

Development guide for working with the VectorWave repository.

## Library Scope

**VectorWave is strictly a wavelet analysis library.** It provides:
- Wavelet transforms (MODWT, CWT, SWT)
- Signal processing and denoising
- Mathematical operations on wavelets
- Performance optimizations (SIMD, Vector API)

**NOT in scope:**
- UI components or helpers
- Platform-specific integrations (MotiveWave, MetaTrader, etc.)
- Trading platform adapters
- Display/presentation logic

Any UI adapters or platform integrations should be implemented as separate projects that depend on VectorWave.

## Current State (January 2025)

### MODWT as Primary Transform
- **PRIMARY**: MODWT (Maximal Overlap Discrete Wavelet Transform) is the main discrete transform
- **KEY BENEFITS**:
  - Works with ANY signal length (no power-of-2 restriction)
  - Shift-invariant processing - critical for pattern detection
  - Perfect reconstruction with machine precision
  - Ideal for real-time and financial applications

### Core Architecture
- **MODWT Package**: `com.morphiqlabs.wavelet.modwt`
  - `MODWTTransform`: Main transform with automatic SIMD optimization
  - `MODWTResult`: Interface with factory method `MODWTResult.create()`
  - `MultiLevelMODWTTransform`: Multi-level decomposition and reconstruction
  - `MutableMultiLevelMODWTResult`: Mutable coefficients for in-place processing
- **SWT Package**: `com.morphiqlabs.wavelet.swt`
  - `VectorWaveSwtAdapter`: SWT interface leveraging MODWT backend
  - Provides familiar SWT API for users migrating from other libraries
  - Full denoising and feature extraction capabilities
- **Performance**: Automatic vectorization using Vector API with scalar fallback
- **Use Cases**: Financial analysis, real-time processing, pattern detection, denoising

### Financial Analysis
- **Configuration-Based**: All financial parameters must be explicitly configured
- **No Default Values**: Removed all default configurations to ensure explicit parameter setting
- **Risk-Free Rate**: Must be provided based on current market conditions and geographic region
- **Specialized Wavelets**: Paul, Shannon-Gabor, DOG wavelets for financial analysis

### Performance Optimizations
- **Automatic SIMD**: Vector API with platform detection (x86: AVX2/AVX512, ARM: NEON)
- **Batch Processing**: `forwardBatch()` and `inverseBatch()` methods with 2-4x speedup
- **Memory Efficiency**: Object pooling and aligned allocation
- **Streaming**: Real-time processing with arbitrary block sizes

### CWT (Continuous Wavelet Transform)
- **FFT Acceleration**: Real-to-complex optimization for 2x speedup
- **Complex Analysis**: Full phase and magnitude information
- **Automatic Scale Selection**: Signal-adaptive scale placement
- **Financial Wavelets**: Paul, Morlet, DOG wavelets for market analysis

## Testing
- **Test Coverage**: 214+ test classes with comprehensive coverage
- **Module Testing**: Each module has independent test suites
- **Real-world data**: Tick data tests with actual financial data
- **Performance tests**: SIMD optimization verification
- **Memory efficiency**: Streaming and batch processing validation

## Key APIs

### Wavelet Selection (Type-Safe Enum)
```java
// Get wavelets using enum - compile-time safety
Wavelet db4 = WaveletRegistry.getWavelet(WaveletName.DB4);
Wavelet haar = WaveletRegistry.getWavelet(WaveletName.HAAR);

// Discover available wavelets
List<WaveletName> orthogonal = WaveletRegistry.getOrthogonalWavelets();
List<WaveletName> daubechies = WaveletRegistry.getDaubechiesWavelets();
```

### Basic MODWT
```java
Wavelet wavelet = WaveletRegistry.getWavelet(WaveletName.DB4);
MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
double[] signal = new double[777]; // Any length!
MODWTResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result);
```

### SWT (Stationary Wavelet Transform)
```java
// SWT adapter for familiar API
VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC);
MutableMultiLevelMODWTResult result = swt.forward(signal, 4);

// Direct coefficient manipulation
swt.applyThreshold(result, 1, 0.5, true); // soft threshold level 1
double[] denoised = swt.inverse(result);

// Convenience denoising
double[] cleanSignal = swt.denoise(noisySignal, 4, -1, true); // universal threshold
```

### Financial Analysis
```java
FinancialConfig config = new FinancialConfig(0.045); // 4.5% risk-free rate
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);
double sharpeRatio = analyzer.calculateWaveletSharpeRatio(returns);
```

### Batch Processing
```java
double[][] signals = new double[32][1000];
MODWTResult[] results = transform.forwardBatch(signals); // 2-4x speedup
```

## Development Guidelines

### API Design Principles
1. **Single Clean API**: Never create "optimized" versions as separate classes (e.g., no SWTOptimizedWavelet)
2. **Internal Optimizations**: All optimizations must be private, internal, and automatic
3. **Transparent Performance**: Users get optimizations automatically without API changes
4. **No Implementation Leakage**: Implementation details should never appear in public APIs

### When Adding Features
1. **Use MODWT**: Primary transform for new features
2. **Explicit Configuration**: No default financial parameters
3. **Test with Real Data**: Use tick data in `src/test/resources/`
4. **Performance**: Leverage automatic SIMD optimization
5. **Thread Safety**: Follow existing patterns for concurrent access

### When Fixing Issues
1. **Run Full Test Suite**: `mvn test`
2. **Check SIMD**: Verify performance on target platforms
3. **Financial Parameters**: Ensure explicit configuration
4. **Documentation**: Update relevant guides in `docs/`

### Testing Commands
```bash
# Run all tests
mvn test

# Run specific test pattern
mvn test -Dtest=*MODWT*
mvn test -Dtest=*SWT*

# Run with Vector API
mvn test -Dtest.args="--add-modules jdk.incubator.vector"

# Run demos
mvn exec:java -Dexec.mainClass="com.morphiqlabs.Main"
mvn exec:java -Dexec.mainClass="com.morphiqlabs.demo.SWTDemo"
```

## Important Notes

- **GitHub Issue #150**: Not required - MODWT already provides better arbitrary-length support
- **No DWT**: Removed all discrete wavelet transform classes - use MODWT instead
- **Java 21+**: Required for core module, Java 24 for Vector API in extensions
- **Clean API**: Use public facades like `WaveletOperations`, avoid internal classes