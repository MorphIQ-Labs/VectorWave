# VectorWave API Reference

## Module Structure

VectorWave is organized into three Maven modules:

### vectorwave-core
**Package**: `com.morphiqlabs.wavelet`
- Core wavelet transforms and algorithms
- Scalar implementations with cache optimization
- Zero runtime dependencies
- Works on any Java 21+ platform

### vectorwave-extensions
**Package**: `com.morphiqlabs.wavelet.extensions`
- SIMD optimizations via Vector API
- Structured concurrency support
- Automatic service discovery
- 2-5x performance improvement

### vectorwave-examples
**Package**: `com.morphiqlabs`
- Demonstrations and benchmarks
- Usage examples
- Performance tests

## Quick Reference

### MODWT Transform (Primary)
```java
// Basic transform - works with ANY signal length
MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
double[] signal = new double[777]; // Any length!
MODWTResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result);

// Batch processing with automatic SIMD
double[][] signals = new double[32][1000];
MODWTResult[] results = transform.forwardBatch(signals);

// Multi-level decomposition
MultiLevelMODWTTransform mlTransform = new MultiLevelMODWTTransform(wavelet, boundaryMode);
MultiLevelMODWTResult mlResult = mlTransform.decompose(signal, levels);
double[] reconstructed = mlTransform.reconstruct(mlResult);
```

### SWT (Stationary Wavelet Transform)
```java
// SWT adapter with familiar interface
VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC);

// Forward transform with mutable results
MutableMultiLevelMODWTResult result = swt.forward(signal, 4);

// Apply thresholding for denoising
swt.applyUniversalThreshold(result, true); // soft thresholding
double[] denoised = swt.inverse(result);

// Convenience denoising method
double[] clean = swt.denoise(noisySignal, 4, -1, true);

// Extract specific frequency bands
double[] highFreq = swt.extractLevel(signal, 4, 1);
```

### CWT Transform
```java
// Basic CWT analysis
CWTTransform cwt = new CWTTransform(new MorletWavelet());
double[] scales = {1.0, 2.0, 4.0, 8.0, 16.0};
CWTResult result = cwt.analyze(signal, scales);

// Complex analysis with phase information
ComplexCWTResult complexResult = cwt.analyzeComplex(signal, scales);
double[][] magnitude = complexResult.getMagnitude();
double[][] phase = complexResult.getPhase();
```

### Financial Analysis
```java
// Configure financial parameters explicitly
FinancialConfig config = new FinancialConfig(0.045); // 4.5% risk-free rate
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);

// Wavelet-based Sharpe ratio
double sharpeRatio = analyzer.calculateWaveletSharpeRatio(returns);

// Financial wavelet analysis
PaulWavelet paulWavelet = new PaulWavelet(4); // Asymmetric crash detection
CWTTransform financialCWT = new CWTTransform(paulWavelet);
CWTResult crashAnalysis = financialCWT.analyze(priceReturns, scales);
```

### Streaming Processing
```java
// Real-time streaming with arbitrary block sizes
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

### Wavelet Registry
```java
// Discover available wavelets - type-safe enum approach
Set<WaveletName> available = WaveletRegistry.getAvailableWavelets();
List<WaveletName> orthogonal = WaveletRegistry.getOrthogonalWavelets();
List<WaveletName> continuous = WaveletRegistry.getContinuousWavelets();

// Get wavelets using enum - compile-time type safety!
Wavelet db4 = WaveletRegistry.getWavelet(WaveletName.DB4);
Wavelet morlet = WaveletRegistry.getWavelet(WaveletName.MORLET);
Wavelet haar = WaveletRegistry.getWavelet(WaveletName.HAAR);

// Get wavelets by family
List<WaveletName> daubechies = WaveletRegistry.getDaubechiesWavelets();
List<WaveletName> symlets = WaveletRegistry.getSymletWavelets();
List<WaveletName> coiflets = WaveletRegistry.getCoifletWavelets();

// Type-safe wavelet selection
WaveletName selected = WaveletName.DB4;  // IDE autocomplete shows all options
if (WaveletRegistry.hasWavelet(selected)) {
    Wavelet wavelet = WaveletRegistry.getWavelet(selected);
}
```

## Core Classes

### MODWT Package (`com.morphiqlabs.wavelet.modwt`)

#### MODWTTransform
- `forward(double[] signal)` - Forward transform
- `inverse(MODWTResult result)` - Inverse transform  
- `forwardBatch(double[][] signals)` - Batch processing
- `inverseBatch(MODWTResult[] results)` - Batch reconstruction
- `getPerformanceInfo()` - Platform capabilities
- `estimateProcessingTime(int length)` - Performance estimation

#### MODWTResult
- `create(double[] approx, double[] detail)` - Factory method
- `approximationCoeffs()` - Get approximation coefficients
- `detailCoeffs()` - Get detail coefficients
- `getSignalLength()` - Original signal length
- `isValid()` - Check for finite values

#### MultiLevelMODWTTransform
- `decompose(double[] signal)` - Automatic max levels
- `decompose(double[] signal, int levels)` - Specify levels
- `decomposeMutable(double[] signal, int levels)` - Returns mutable result
- `reconstruct(MultiLevelMODWTResult result)` - Full reconstruction
- `reconstructFromLevel(result, int level)` - Partial reconstruction
- `getMaximumLevels(int signalLength)` - Calculate max levels

#### MutableMultiLevelMODWTResult
- `getMutableDetailCoeffs(int level)` - Direct coefficient access
- `getMutableApproximationCoeffs()` - Mutable approximation
- `applyThreshold(int level, double threshold, boolean soft)` - Thresholding
- `clearCaches()` - Clear cached computations after modification
- `toImmutable()` - Create immutable copy
- `isValid()` - Check for NaN/Inf

### SWT Package (`com.morphiqlabs.wavelet.swt`)

#### VectorWaveSwtAdapter
- `forward(double[] signal)` - Max levels decomposition
- `forward(double[] signal, int levels)` - Specified levels
- `inverse(MutableMultiLevelMODWTResult result)` - Reconstruction
- `applyThreshold(result, level, threshold, soft)` - Level thresholding
- `applyUniversalThreshold(result, soft)` - Auto threshold
- `denoise(signal, levels)` - Universal threshold denoising
- `denoise(signal, levels, threshold, soft)` - Custom threshold
- `extractLevel(signal, levels, targetLevel)` - Band extraction

### CWT Package (`com.morphiqlabs.wavelet.cwt`)

#### CWTTransform
- `analyze(double[] signal, double[] scales)` - Basic analysis
- `analyzeComplex(double[] signal, double[] scales)` - Complex analysis

#### Wavelets
- `MorletWavelet` - Time-frequency analysis
- `PaulWavelet(int order)` - Financial crash detection
- `GaussianDerivativeWavelet(int derivative)` - Edge detection

### Financial Package (`com.morphiqlabs.financial`)

#### FinancialWaveletAnalyzer
- Constructor requires `FinancialConfig(double riskFreeRate)`
- `calculateSharpeRatio(double[] returns)` - Standard Sharpe ratio
- `calculateWaveletSharpeRatio(double[] returns)` - Wavelet-based Sharpe

## Wavelet Families

### Orthogonal Wavelets
```java
new Haar()                    // Simplest wavelet
Daubechies.DB2                // 2 vanishing moments
Daubechies.DB4                // 4 vanishing moments (popular)
Daubechies.DB8                // 8 vanishing moments
Symlet.SYM4                   // Nearly symmetric
Coiflet.COIF2                 // Both functions have vanishing moments
```

### Biorthogonal Wavelets
```java
BiorthogonalSpline.BIOR1_3    // CDF 1,3 wavelet
```

### Continuous Wavelets
```java
new MorletWavelet()           // Complex Morlet
new PaulWavelet(4)            // Paul order 4
new GaussianDerivativeWavelet(2)  // Mexican Hat (2nd derivative)
```

### Financial Wavelets
```java
new PaulWavelet(4)            // Crash detection
new ShannonGaborWavelet()     // Reduced artifacts
new DOGWavelet(2)             // Difference of Gaussians
```

## Performance Features

### Automatic Optimization
- **SIMD Detection**: Automatically uses Vector API when available
- **Platform Adaptive**: x86 (AVX2/AVX512), ARM (NEON)
- **Scalar Fallback**: Graceful degradation on unsupported platforms
- **Batch Optimization**: 2-4x speedup for multiple signals

### Performance Monitoring
```java
// Check platform capabilities
WaveletOperations.PerformanceInfo info = WaveletOperations.getPerformanceInfo();
System.out.println(info.description());
// Output: "Vectorized operations enabled on aarch64 with S_128_BIT"

// Estimate processing time
MODWTTransform.ProcessingEstimate estimate = transform.estimateProcessingTime(signalLength);
System.out.println(estimate.description());
```

## Configuration

### Boundary Modes
- `BoundaryMode.PERIODIC` - Circular/periodic boundary (recommended for MODWT)
- `BoundaryMode.ZERO_PADDING` - Zero padding at boundaries

### Streaming Configuration
```java
MODWTStreamingDenoiser.Builder()
    .wavelet(Daubechies.DB4)
    .boundaryMode(BoundaryMode.PERIODIC)
    .bufferSize(1024)  // Any size - no power-of-2 restriction
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .thresholdType(ThresholdType.SOFT)
    .build();
```

## Error Handling

### Exception Types
- `InvalidSignalException` - Invalid input signals
- `InvalidArgumentException` - Invalid parameters
- `InvalidConfigurationException` - Invalid configuration
- `WaveletTransformException` - Transform-specific errors

### Common Patterns
```java
try {
    MODWTResult result = transform.forward(signal);
} catch (InvalidSignalException e) {
    // Handle invalid signal (NaN, Infinity, empty)
    ErrorContext context = e.getErrorContext();
    System.err.println("Signal error: " + context.getDescription());
}
```

## Key Differences from Standard Wavelet Libraries

1. **No Power-of-2 Restriction**: MODWT works with signals of any length
2. **Shift Invariance**: MODWT provides translation-invariant analysis
3. **Automatic SIMD**: Platform-specific optimizations without manual configuration
4. **Financial Focus**: Specialized wavelets and analysis tools for market data
5. **Explicit Configuration**: No default values for financial parameters
6. **Java 24 Features**: Modern Java with Vector API integration

## Requirements

### Core Module
- **Java 21+** - Required for modern language features
- **No runtime dependencies** - Pure Java implementation
- **Maven**: Add `vectorwave-core` dependency

### Extensions Module (Optional)
- **Java 24+** with Vector API support
- **Compilation**: `--add-modules jdk.incubator.vector --enable-preview`
- **Runtime**: Automatic detection and fallback
- **Maven**: Add `vectorwave-extensions` as runtime dependency

### Recommended Setup
```xml
<dependencies>
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
</dependencies>
```
