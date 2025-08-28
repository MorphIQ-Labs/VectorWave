# VectorWave Demo Suite

This directory contains comprehensive demonstrations of the VectorWave wavelet transform library's capabilities, featuring MODWT (Maximal Overlap Discrete Wavelet Transform) as the primary transform. Each demo is a standalone Java application that showcases specific features and best practices.

## Running the Demos

To run any demo, first compile the project:

```bash
mvn clean compile
```

Then run a specific demo:

```bash
java -cp target/classes com.morphiqlabs.demo.DemoName
```

## Available Demos

### 1. BasicUsageDemo

**Purpose**: Introduction to fundamental MODWT operations

**What it covers**:

- Creating MODWT transforms
- Forward and inverse transforms with shift-invariance
- Using the factory pattern
- Working with ANY signal length (no power-of-2 restriction!)
- Perfect reconstruction verification
- Coefficient analysis with same-length outputs

**Run**: `java -cp target/classes com.morphiqlabs.demo.BasicUsageDemo`

### 2. BoundaryModesDemo

**Purpose**: Understanding boundary handling in wavelet transforms

**What it covers**:

- Effects of PERIODIC vs ZERO_PADDING modes
- Edge artifact analysis
- Reconstruction accuracy comparison
- Mode selection guidelines
- Real-world application scenarios

**Run**: `java -cp target/classes com.morphiqlabs.demo.BoundaryModesDemo`

### 3. PerformanceOptimizationDemo

**Purpose**: Maximizing transform performance

**What it covers**:

- SIMD vectorization benefits
- Memory pooling strategies
- Batch processing techniques
- Parallel processing patterns
- Cache-friendly access patterns
- Performance comparison across wavelets

**Run**: `java -cp target/classes com.morphiqlabs.demo.PerformanceOptimizationDemo`

### 4. WaveletSelectionGuideDemo

**Purpose**: Choosing the right wavelet for your application

**What it covers**:

- Characteristics of each wavelet family
- Matching wavelets to signal types
- Application-specific recommendations
- Performance vs accuracy trade-offs
- Interactive decision flowchart

**Run**: `java -cp target/classes com.morphiqlabs.demo.WaveletSelectionGuideDemo`

### 5. SignalAnalysisDemo

**Purpose**: Advanced signal analysis using wavelets

**What it covers**:

- Time-frequency analysis
- Feature extraction for machine learning
- Anomaly detection techniques
- Signal classification
- Pattern recognition

**Run**: `java -cp target/classes com.morphiqlabs.demo.SignalAnalysisDemo`

### 6. MemoryEfficiencyDemo

**Purpose**: Memory-efficient usage patterns

**What it covers**:

- Memory pooling benefits
- In-place transformations
- Streaming with minimal memory footprint
- GC-friendly coding patterns
- Large dataset handling strategies

**Run**: `java -cp target/classes com.morphiqlabs.demo.MemoryEfficiencyDemo`

### 7. MultiLevelDemo

**Purpose**: Multi-level wavelet decomposition

**What it covers**:

- Full and partial decomposition
- Energy distribution analysis
- Adaptive decomposition
- Multi-scale volatility analysis
- Financial time series analysis

**Run**: `java -cp target/classes --add-modules jdk.incubator.vector com.morphiqlabs.demo.MultiLevelDemo`

### 8. DenoisingDemo

**Purpose**: Signal denoising using wavelets

**What it covers**:

- Different threshold methods (Universal, SURE, Minimax)
- Soft vs hard thresholding
- Multi-level denoising
- Financial data denoising
- Performance benchmarks

**Run**: `java -cp target/classes --add-modules jdk.incubator.vector com.morphiqlabs.demo.DenoisingDemo`

### 9. StreamingDenoiserDemo

**Purpose**: Real-time streaming denoising with dual implementations

**What it covers**:

- Fast vs Quality implementation comparison
- Real-time audio signal denoising
- Financial data stream cleaning
- Adaptive threshold adjustment
- Multi-channel stream processing
- Performance metrics and latency analysis

**Run**: `java -cp target/classes com.morphiqlabs.demo.StreamingDenoiserDemo`

### 10. StreamingDenoiserFactoryDemo

**Purpose**: Factory pattern for streaming denoiser selection

**What it covers**:

- Manual implementation selection (FAST/QUALITY)
- Automatic implementation selection
- Performance profile comparison
- Configuration-based optimization
- Real-time capability assessment
- Memory usage analysis

**Run**: `java -cp target/classes com.morphiqlabs.demo.StreamingDenoiserFactoryDemo`

### 11. ScalarVsVectorDemo

**Purpose**: Understanding scalar vs SIMD/vector optimization paths

**What it covers**:

- Forcing scalar operations for debugging/compatibility
- Forcing SIMD operations for maximum performance
- Auto-detection of optimal path
- Performance comparison between modes
- Configuration validation and best practices

**Run**: `java -cp target/classes --add-modules jdk.incubator.vector com.morphiqlabs.demo.ScalarVsVectorDemo`

### Additional Demos (Main Package)

**FinancialOptimizationDemo** (`com.morphiqlabs.FinancialOptimizationDemo`)

- High-frequency trading signal analysis
- Market microstructure noise filtering
- Real-time volatility estimation
- Optimized for financial time series

**OptimizationDemo** (`com.morphiqlabs.OptimizationDemo`)

- Comprehensive optimization showcase
- Platform-specific optimizations
- Memory and performance tuning
- Best practices for production use

## Demo Categories by Use Case

### Getting Started

- BasicUsageDemo - Start here for fundamentals
- WaveletSelectionGuideDemo - Choose the right wavelet

### Performance Optimization

- PerformanceOptimizationDemo - Speed optimization
- MemoryEfficiencyDemo - Memory optimization

### Signal Processing

- SignalAnalysisDemo - Analysis techniques
- DenoisingDemo - Noise reduction
- MultiLevelDemo - Multi-resolution analysis

### Real-time Applications

- StreamingDenoiserDemo - Real-time stream processing
- StreamingDenoiserFactoryDemo - Implementation selection
- MemoryEfficiencyDemo - Low-latency patterns
- ScalarVsVectorDemo - SIMD optimization control

### Algorithm Selection

- BoundaryModesDemo - Boundary handling
- WaveletSelectionGuideDemo - Wavelet selection

## Best Practices Demonstrated

1. **Error Handling**: All demos show proper exception handling
2. **Resource Management**: Proper cleanup and resource disposal
3. **Performance**: Efficient patterns for high-performance applications
4. **Memory Usage**: Techniques to minimize memory footprint
5. **Scalability**: Patterns that scale from small to large datasets

## Common Patterns

### Transform Creation

```java
// Using factory
MODWTTransform transform = new MODWTTransformFactory()
                .create(Daubechies.DB4, BoundaryMode.PERIODIC);

// Direct instantiation
MODWTTransform transform = new MODWTTransform(
        new Haar(), BoundaryMode.ZERO_PADDING);
```

### Basic Transform

```java
double[] signal = new double[777]; // Any length - no power-of-2 restriction!
MODWTResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result);
// Coefficients have same length as input signal
```

### Memory-Efficient Processing

```java
MemoryPool pool = new MemoryPool();
double[] buffer = pool.borrowArray(size);
try{
        // Use buffer
        }finally{
        pool.

returnArray(buffer);
}
```

## Notes

- All demos use seeded random numbers for reproducibility
- Performance measurements may vary based on hardware
- SIMD optimizations require Java 21+ with Vector API support
- Some demos create temporary data for visualization
- StreamingDemo is located in the test directory for testing streaming functionality
- Platform-specific optimizations (Apple Silicon, ARM, x86) are automatically detected

## Further Reading

- See the main README.md for library overview
- Check BENCHMARKING.md for performance testing
- Review the API documentation for detailed usage