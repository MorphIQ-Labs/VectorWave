# VectorWave CWT Demos

This directory contains comprehensive demonstrations of VectorWave's Continuous Wavelet Transform (CWT) capabilities, showcasing the new wavelets and performance optimizations implemented in this branch.

## Demo Overview

### 🚀 [CWTBasicsDemo.java](CWTBasicsDemo.java)
**Introduction to CWT concepts and basic usage**

- Basic CWT analysis with Morlet wavelets
- FFT-accelerated vs direct convolution comparison
- Scale space analysis for different frequency ranges
- Configuration options (padding strategies, normalization)

**Key Features Demonstrated:**
- O(n log n) FFT acceleration
- Scale space construction
- Performance comparison
- Configuration flexibility

### 💰 [FinancialWaveletsDemo.java](FinancialWaveletsDemo.java)
**Specialized wavelets for financial market analysis**

- **Paul Wavelet**: Volatility detection and market timing
- **Shannon Wavelet**: Frequency analysis with excellent localization
- **DOG Wavelet**: Edge detection for price movements
- **FinancialWaveletAnalyzer**: High-level market analysis API

**Real-World Applications:**
- Market crash detection
- Volatility analysis
- Trend identification
- Volume-price correlation analysis

### 🔍 [GaussianDerivativeDemo.java](GaussianDerivativeDemo.java)
**Feature detection with Gaussian derivative wavelets**

- **Order 1**: Edge detection (first derivative)
- **Order 2**: Ridge/valley detection (Mexican Hat)
- **Order 3+**: Higher-order feature detection
- Multi-scale feature analysis
- WaveletRegistry integration ("gaus1", "gaus2", etc.)

**Feature Detection Capabilities:**
- Step edge detection
- Ridge and valley identification
- Inflection point analysis
- Multi-scale feature characterization

### ⚡ [CWTPerformanceDemo.java](CWTPerformanceDemo.java)
**Performance optimization and platform adaptivity**

- Platform detection (Apple Silicon vs x86)
- Cache configuration (auto-detection and custom)
- FFT vs direct convolution scaling analysis
- Memory efficiency measurement

**Performance Features:**
- Apple Silicon: 128KB L1, 4MB L2 cache optimization
- x86-64: 32KB L1, 256KB L2 cache optimization
- Configurable via system properties
- O(n log n) complexity verification

### 🎯 [ComprehensiveCWTDemo.java](ComprehensiveCWTDemo.java)
**Real-world applications combining all features**

- Biomedical signal analysis (simulated ECG)
- Time-frequency analysis (chirp signals)
- Multi-wavelet comparison
- Advanced configuration showcase

**Realistic Use Cases:**
- ECG heart rate detection
- Chirp signal analysis
- Multi-component signal decomposition
- Configuration optimization

## Quick Start

Run any demo independently:

```bash
# Compile
mvn clean compile

# Run basic CWT introduction
java -cp target/classes com.morphiqlabs.demo.cwt.CWTBasicsDemo

# Run financial analysis demo
java -cp target/classes com.morphiqlabs.demo.cwt.FinancialWaveletsDemo

# Run feature detection demo
java -cp target/classes com.morphiqlabs.demo.cwt.GaussianDerivativeDemo

# Run performance analysis
java -cp target/classes com.morphiqlabs.demo.cwt.CWTPerformanceDemo

# Run comprehensive showcase
java -cp target/classes com.morphiqlabs.demo.cwt.ComprehensiveCWTDemo
```

## Key Technologies Demonstrated

### 🧮 **FFT-Accelerated CWT**
- **Algorithm**: Cooley-Tukey FFT with conjugate-based IFFT
- **Complexity**: O(n log n) vs O(n²) direct convolution
- **Performance**: 2-10x speedup for signals >1024 samples
- **Implementation**: `FFTAcceleratedCWT` class

### 📊 **Financial Wavelets**
```java
// Market crash detection
PaulWavelet paulWavelet = new PaulWavelet(4);
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer();
var crashResult = analyzer.detectMarketCrashes(priceData, threshold);

// Frequency analysis
ShannonWavelet shannon = new ShannonWavelet();
// Edge detection in price movements
DOGWavelet dog = new DOGWavelet(2.0);
```

### 🎯 **Gaussian Derivative Wavelets**
```java
// Mexican Hat (2nd order) for ridge detection
GaussianDerivativeWavelet mexicanHat = new GaussianDerivativeWavelet(2);

// From registry using enum for type safety
var gaussian = WaveletRegistry.getWavelet(WaveletName.GAUSSIAN);
```

### 🚄 **Platform-Adaptive Performance**
```java
// Auto-detected cache configuration
CacheAwareOps.CacheConfig config = CacheAwareOps.getDefaultCacheConfig();
// Apple Silicon: 128KB L1, 4MB L2
// x86-64: 32KB L1, 256KB L2

// Custom configuration
CacheAwareOps.CacheConfig custom = CacheAwareOps.CacheConfig.create(
    64 * 1024,  // L1 size
    512 * 1024, // L2 size  
    64          // cache line size
);
```

## Performance Characteristics

### **Signal Size Scaling** (FFT-accelerated)
| Signal Size | Time (ms) | Complexity | Notes |
|-------------|-----------|------------|-------|
| 256         | ~1-2      | O(n log n) | Direct competitive |
| 512         | ~2-4      | O(n log n) | FFT advantage begins |
| 1024        | ~5-8      | O(n log n) | Clear FFT benefit |
| 2048        | ~10-15    | O(n log n) | Significant speedup |
| 4096        | ~20-30    | O(n log n) | 5-10x vs direct |

### **Platform Optimizations**
- **Apple Silicon**: Leverages unified memory architecture and larger caches
- **x86-64**: Optimized for traditional cache hierarchy
- **Auto-detection**: Based on `os.arch` and `os.name` system properties
- **Configurable**: Via `-Dcom.morphiqlabs.cache.l1.size=65536` etc.

## Mathematical Background

### **Continuous Wavelet Transform**
```
CWT(a,b) = (1/√a) ∫ x(t) ψ*((t-b)/a) dt
```
Where:
- `a` = scale parameter (inversely related to frequency)
- `b` = translation parameter (time localization)  
- `ψ` = mother wavelet
- `*` = complex conjugate

### **FFT Acceleration**
- **Convolution Theorem**: `CWT = IFFT(FFT(signal) * conj(FFT(wavelet)))`
- **Complexity**: O(n log n) vs O(n²) for direct convolution
- **IFFT Formula**: `IFFT(X) = (1/N) * conj(FFT(conj(X)))`

## Error Handling & Robustness

All demos include:
- ✅ **Input validation**: Signal length, scale parameters
- ✅ **Error recovery**: Graceful handling of edge cases
- ✅ **Performance monitoring**: Timing and memory measurement
- ✅ **Result verification**: Sanity checks and MSE comparison
- ✅ **Platform compatibility**: Works on Apple Silicon and x86-64

## Next Steps

After exploring these demos:

1. **Try your own signals**: Modify signal generation methods
2. **Experiment with parameters**: Adjust scales, wavelets, configurations
3. **Benchmark your platform**: Run performance demos to see optimization benefits
4. **Integrate into applications**: Use patterns from ComprehensiveCWTDemo
5. **Explore streaming**: Check out streaming CWT capabilities in the main library

## Dependencies

All demos use only:
- ✅ **Pure Java 21+**: No external dependencies
- ✅ **VectorWave library**: Complete CWT implementation
- ✅ **JDK Vector API**: For SIMD optimizations (optional)

## Contributing

Found an interesting use case or optimization? Consider:
- Adding a new demo scenario
- Improving signal generation methods
- Contributing platform-specific optimizations
- Enhancing visualization capabilities