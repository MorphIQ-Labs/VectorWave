# Batch Processing Guide

This guide covers the batch processing capabilities in VectorWave, which enable efficient processing of multiple signals simultaneously using SIMD instructions.

> Where SIMD lives
>
> SIMD/Vector API acceleration is provided by the optional `vectorwave-extensions` module (Java 24 + incubator). The core module remains a portable scalar Java 21 implementation. For the highest throughput, add the extensions dependency and run with `--add-modules jdk.incubator.vector --enable-preview`.

## Overview

Batch processing in VectorWave provides true parallel processing of multiple signals, leveraging SIMD (Single Instruction, Multiple Data) capabilities of modern processors. This is particularly useful for:

- Multi-channel audio processing
- Financial time series analysis (multiple stocks/currencies)
- Sensor array data processing
- Large-scale signal analysis pipelines

## Basic Usage

### Simple Batch Transform

```java
import com.morphiqlabs.wavelet.modwt.*;
import com.morphiqlabs.wavelet.api.*;

// Create a MODWT transform (core scalar path)
MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);

// Prepare multiple signals of any length
double[][] signals = new double[32][777]; // 32 signals of length 777 (any length!)
// ... populate signals ...

// Process all signals in parallel (core). For SIMD batch AoS facades,
// use BatchMODWT in the vectorwave-extensions module.
MODWTResult[] results = transform.forwardBatch(signals);

// Inverse transform
double[][] reconstructed = transform.inverseBatch(results);
```

### Using Different Wavelets

```java
// Daubechies wavelets
MODWTTransform db4Transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
MODWTResult[] db4Results = db4Transform.forwardBatch(signals);

// Symlet wavelets
MODWTTransform sym4Transform = new MODWTTransform(Symlet.SYM4, BoundaryMode.PERIODIC);
MODWTResult[] sym4Results = sym4Transform.forwardBatch(signals);
```

## Advanced Configuration

### Automatic Batch Optimization

MODWT automatically applies optimizations based on signal characteristics:

```java
// Create MODWT transform - optimizations are automatic
MODWTTransform transform = new MODWTTransform(wavelet, boundaryMode);

// Process batch - automatically uses:
// - SIMD vectorization when beneficial
// - Optimized memory layout for cache efficiency  
// - Platform-specific optimizations (ARM vs x86)
// - Specialized kernels for common wavelets
MODWTResult[] results = transform.forwardBatch(signals);
```

### Memory-Aligned Batch Processing

For optimal SIMD performance with aligned memory:

```java
import com.morphiqlabs.wavelet.memory.BatchMemoryLayout;

// Create aligned memory layout
try (BatchMemoryLayout layout = new BatchMemoryLayout(batchSize, signalLength)) {
    // Load signals with interleaving for better SIMD access
    layout.loadSignalsInterleaved(signals, true);
    
    // Perform transform
    layout.haarTransformInterleaved();
    
    // Extract results
    double[][] approxResults = new double[batchSize][signalLength / 2];
    double[][] detailResults = new double[batchSize][signalLength / 2];
    layout.extractResultsInterleaved(approxResults, detailResults);
}
```

## Performance Optimization Tips

### 1. Batch Size Selection

- **Optimal sizes**: Multiples of the SIMD vector width (typically 2, 4, or 8)
- **Sweet spot**: 8-64 signals for most applications
- **Large batches**: Use parallel processing for 64+ signals

### 2. Signal Length Considerations

- MODWT works with any signal length (no padding needed!)
- Longer signals benefit more from batch processing
- SIMD optimizations automatically applied for signals > 64 elements

### 3. Memory Layout

The batch processor supports two memory layouts:

**Array of Structures (AoS)** - Default:
```
Signal 0: [s0_0, s0_1, s0_2, ...]
Signal 1: [s1_0, s1_1, s1_2, ...]
```

**Structure of Arrays (SoA)** - Optimized:
```
Sample 0: [s0_0, s1_0, s2_0, ...]
Sample 1: [s0_1, s1_1, s2_1, ...]
```

### 4. Platform-Specific Optimization

```java
// Get information about the current platform's capabilities
MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
MODWTTransform.PerformanceInfo perfInfo = transform.getPerformanceInfo();
System.out.println(perfInfo.description());
```

## Real-World Examples

### Multi-Channel Audio Processing

```java
// Process stereo audio (2 channels)
double[][] stereoSignal = new double[2][44100]; // 1 second at 44.1kHz
// ... load audio data ...

MODWTTransform transform = new MODWTTransform(Daubechies.DB8, BoundaryMode.PERIODIC);
MODWTResult[] channelResults = transform.forwardBatch(stereoSignal);

// Apply processing to each channel
for (int ch = 0; ch < 2; ch++) {
    // Process using factory methods
    double[] modifiedDetail = processDetails(channelResults[ch].detailCoeffs());
    channelResults[ch] = MODWTResult.create(
        channelResults[ch].approximationCoeffs(), 
        modifiedDetail
    );
}

// Reconstruct
double[][] processedAudio = transform.inverseBatch(channelResults);
```

### Financial Time Series Analysis

```java
// Analyze multiple stock prices
String[] symbols = {"AAPL", "GOOGL", "MSFT", "AMZN"};
double[][] priceData = new double[symbols.length][252]; // 1 year of daily data
// ... load price data ...

// MODWT provides optimal processing for financial analysis
MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
MODWTResult[] results = transform.forwardBatch(priceData);

// Analyze each stock's wavelet coefficients
for (int i = 0; i < symbols.length; i++) {
    System.out.println("Analysis for " + symbols[i]);
    analyzeCoefficients(results[i]);
}
```

### Sensor Array Processing

```java
// Process data from sensor array
int numSensors = 16;
int samplesPerSecond = 1000;
double[][] sensorData = new double[numSensors][samplesPerSecond];
// ... collect sensor data ...

// MODWT automatically optimizes for real-time processing
MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);

// Process in real-time - MODWT automatically:
// - Uses SIMD for low-latency processing
// - Optimizes memory access patterns
// - Minimizes allocation overhead
MODWTResult[] sensorResults = transform.forwardBatch(sensorData);
```

## Performance Benchmarking

### Measuring Batch Performance

```java
import com.morphiqlabs.wavelet.benchmark.*;

// Compare sequential vs batch processing
int batchSize = 32;
int signalLength = 1024;
int iterations = 1000;

// Generate test data
double[][] testSignals = generateTestSignals(batchSize, signalLength);

// Sequential processing
long seqStart = System.nanoTime();
for (int i = 0; i < iterations; i++) {
    for (double[] signal : testSignals) {
        transform.forward(signal);
    }
}
long seqTime = System.nanoTime() - seqStart;

// Batch processing
long batchStart = System.nanoTime();
for (int i = 0; i < iterations; i++) {
    transform.forwardBatch(testSignals);
}
long batchTime = System.nanoTime() - batchStart;

// Calculate speedup
double speedup = (double) seqTime / batchTime;
System.out.printf("Batch processing speedup: %.2fx%n", speedup);
```

## Troubleshooting

### Common Issues

1. **Performance not improving**: 
   - Check batch size alignment with SIMD vector width
   - Ensure signals are properly aligned in memory
   - Verify platform supports Vector API

2. **Out of memory errors**:
   - Use memory pooling
   - Process in smaller batches
   - Enable streaming mode for very large datasets

3. **Incorrect results**:
   - Verify all signals have the same length
   - Check boundary mode compatibility
   - Ensure proper signal padding for non-power-of-2 lengths

### Debug Information

```java
// Get performance information
MODWTTransform transform = new MODWTTransform(wavelet, boundaryMode);
ScalarOps.PerformanceInfo perfInfo = transform.getPerformanceInfo();
System.out.println(perfInfo.description());

// Check SIMD capabilities
System.out.println("Vector species: " + DoubleVector.SPECIES_PREFERRED);
System.out.println("Vector length: " + DoubleVector.SPECIES_PREFERRED.length());
```

## See Also

- [Performance Guide](../PERFORMANCE.md)
- [SIMD Optimization Analysis](../performance/SIMD_OPTIMIZATION_ANALYSIS.md)
- [Streaming Guide](STREAMING.md)
