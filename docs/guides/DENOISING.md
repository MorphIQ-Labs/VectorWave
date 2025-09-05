# Wavelet Denoising in VectorWave

This document provides a comprehensive guide to the denoising capabilities in VectorWave, including both traditional batch processing and real-time streaming approaches.

## Table of Contents
- [Overview](#overview)
- [Traditional Denoising](#traditional-denoising)
- [Streaming Denoising](#streaming-denoising)
- [Performance Characteristics](#performance-characteristics)
- [Implementation Details](#implementation-details)
- [Usage Examples](#usage-examples)
- [Best Practices](#best-practices)

## Overview

VectorWave provides two complementary approaches to wavelet-based signal denoising:

1. **Traditional Denoiser**: Batch processing of complete signals
2. **Streaming Denoiser**: Real-time processing of continuous data streams

Both approaches use wavelet decomposition to separate signal from noise, but differ in their architecture, memory usage, and application domains.

## Traditional Denoising

### Architecture

The traditional denoiser (`WaveletDenoiser`) processes entire signals in a single operation:

```java
WaveletDenoiser denoiser = new WaveletDenoiser.Builder()
    .withWavelet(wavelet)
    .withThresholdMethod(WaveletDenoiser.ThresholdMethod.UNIVERSAL)
    .withSoftThresholding(true)
    .build();

double[] denoised = denoiser.denoise(noisySignal);
```

### Key Features

- **Batch Processing**: Processes complete signal at once
- **Memory Usage**: O(n) - proportional to signal length
- **Noise Estimation**: Exact MAD (Median Absolute Deviation) calculation
- **Threshold Methods**:
  - UNIVERSAL: Universal threshold (VisuShrink)
  - SURE: Stein's Unbiased Risk Estimate
  - MINIMAX: Minimax threshold
  - BAYES: BayesShrink adaptive threshold
- **Threshold Types**:
  - SOFT: Smooth threshold function
  - HARD: Discontinuous threshold function

### Algorithm

1. Perform wavelet decomposition
2. Estimate noise level from finest detail coefficients
3. Calculate threshold based on selected method
4. Apply thresholding to detail coefficients
5. Reconstruct signal from modified coefficients

## Streaming Denoising

### Architecture

The streaming denoiser (`StreamingDenoiser`) processes signals in blocks, enabling real-time applications:

```java
// MODWT-based streaming denoiser - works with any block size!
MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
    .wavelet(wavelet)
    .boundaryMode(BoundaryMode.PERIODIC)
    .bufferSize(333)  // Any size - no power-of-2 restriction!
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .thresholdType(ThresholdType.SOFT)
    .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
    .build();

// Process blocks
double[] denoisedBlock = denoiser.denoise(noisyBlock);

// Or subscribe for continuous processing
denoiser.subscribe(new Flow.Subscriber<double[]>() {
    @Override
    public void onNext(double[] denoisedBlock) {
        // Process denoised block
    }
});
```

### Key Features

- **Block Processing**: Configurable block size (any size with MODWT!)
- **Memory Usage**: O(1) - constant regardless of stream length
- **Overlap-Add**: Smooth transitions between blocks
- **Window Functions**: Hann, Hamming, Tukey, Rectangular
- **Adaptive Noise Estimation**: P² algorithm for online quantile estimation
- **Dynamic Thresholding**: Attack/release time controls
- **Multi-level Support**: Configurable decomposition levels

### Components

1. **Overlap Buffer**: Manages block transitions
   - Configurable overlap factor (0-95%)
   - Window functions for smooth blending
   - Prevents block boundary artifacts

2. **P² Quantile Estimator**: Online noise estimation
   - O(1) memory usage
   - Maintains 5 markers for quantile tracking
   - Suitable for non-stationary noise

3. **Streaming MAD Estimator**: Noise level tracking
   - Uses P² for median estimation
   - Continuously updates noise estimates
   - Adapts to changing signal characteristics

4. **Threshold Adapter**: Dynamic threshold adjustment
   - Configurable attack/release times
   - Smooth threshold transitions
   - Prevents abrupt changes

5. **Memory Pool**: Efficient buffer management
   - Shared or dedicated pools
   - Reduces allocation overhead
   - Configurable buffer reuse

## Performance Characteristics

### Benchmark Results (128-sample blocks)

| Metric | Traditional | Streaming | Notes |
|--------|------------|-----------|-------|
| **Latency** | High (full signal) | Low (block size) | Streaming outputs as blocks complete |
| **Throughput (Haar)** | 19.7M samples/sec | 4.8M samples/sec | 4.1x difference |
| **Throughput (DB4)** | 23.0M samples/sec | 6.3M samples/sec | 3.7x difference |
| **Memory Usage** | O(n) | O(1) | Streaming uses fixed memory |
| **Processing Time** | 0.007 ms/block | 0.027 ms/block | For 128-sample blocks |

### Component Performance

- **P² Algorithm**: 0.004 ms per update (faster than buffered MAD)
- **Overlap Processing**: <0.001 ms per block
- **Memory Allocation**: 0.001-0.003 ms per instance
- **Flow API Overhead**: Included in total processing time

## Implementation Details

### Noise Estimation Methods

1. **Traditional MAD**:
   ```java
   median = exactMedian(coefficients);
   mad = exactMedian(|coefficients - median|);
   sigma = mad / 0.6745;
   ```

2. **Streaming P²**:
   ```java
   // Maintains 5 markers for online quantile estimation
   // Updates in O(1) time with O(1) space
   P2QuantileEstimator median = P2QuantileEstimator.forMedian();
   median.update(coefficient);
   ```

### Threshold Calculation

- **Universal**: `threshold = sigma * sqrt(2 * log(n))`
- **SURE**: Minimizes Stein's Unbiased Risk Estimate
- **Minimax**: Optimal minimax threshold
- **BAYES**: `threshold = sigma^2 / sigma_x` where `sigma_x = sqrt(max(0, var(coeffs) - sigma^2))`
  - Adaptive to signal characteristics
  - Minimizes Bayesian risk
  - Good for signals with varying SNR

### Multi-level Denoising

For streaming multi-level denoising:
- Level-dependent thresholds: `threshold * pow(1.2, level - 1)`
- Reconstruct from coarsest to finest level
- Higher levels (coarser details) get higher thresholds

## Usage Examples

### Example 1: Simple Batch Denoising

```java
// Load noisy signal
double[] noisySignal = loadSignal("noisy_data.csv");

// Create denoiser using builder pattern
WaveletDenoiser denoiser = new WaveletDenoiser.Builder()
    .withWavelet(Daubechies.DB4)
    .withThresholdMethod(WaveletDenoiser.ThresholdMethod.UNIVERSAL)
    .withSoftThresholding(true)
    .build();

// Denoise - the denoiser automatically uses WaveletOperations for SIMD
double[] clean = denoiser.denoise(noisySignal);
```

### Example 2: Real-time Audio Denoising

```java
// Configure MODWT streaming denoiser for audio
MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
    .wavelet(Daubechies.DB4)
    .boundaryMode(BoundaryMode.PERIODIC)
    .bufferSize(480)             // Exactly 10ms at 48kHz - no padding!
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .windowFunction(WindowFunction.HANN)
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .thresholdType(ThresholdType.SOFT)
    .adaptiveThreshold(true)
    .attackTime(10.0)            // 10ms attack
    .releaseTime(50.0)           // 50ms release
    .levels(3)                   // 3-level decomposition
    .build();

// Process audio stream
AudioInputStream audioStream = getAudioStream();
byte[] buffer = new byte[1024];
while (audioStream.read(buffer) > 0) {
    double[] samples = convertToDouble(buffer);
    denoiser.process(samples);
}
```

### Example 3: Adaptive Denoising with BayesShrink

```java
// BayesShrink adapts to signal characteristics
WaveletDenoiser bayesDenoiser = new WaveletDenoiser.Builder()
    .withWavelet(Daubechies.DB4)
    .withThresholdMethod(WaveletDenoiser.ThresholdMethod.BAYES)
    .withSoftThresholding(true)
    .build();

// Process signal with varying SNR
double[] complexSignal = loadComplexSignal();
double[] denoised = bayesDenoiser.denoise(complexSignal);

// BayesShrink automatically adapts threshold based on
// signal variance, providing better results for signals
// with non-uniform noise characteristics
```

### Example 4: Financial Data Cleaning

```java
// Streaming denoiser for tick data
StreamingDenoiser priceDenoiser = new StreamingDenoiser.Builder()
    .wavelet(new Haar())         // Simple wavelet for financial data
    .blockSize(64)               // Small blocks for low latency
    .overlapFactor(0.25)         // 25% overlap
    .thresholdMethod(ThresholdMethod.MINIMAX)
    .adaptiveThreshold(true)
    .noiseBufferFactor(8)        // Larger buffer for stable estimates
    .build();

// Subscribe to cleaned prices
priceDenoiser.subscribe(new Flow.Subscriber<double[]>() {
    @Override
    public void onNext(double[] cleanedPrices) {
        updateTradingStrategy(cleanedPrices);
    }
});

// Feed price ticks
marketDataFeed.subscribe(price -> {
    priceDenoiser.process(price);
});
```

## Best Practices

### Choosing Between Traditional and Streaming

**Use Traditional Denoiser when:**
- Processing recorded/stored signals
- Maximum denoising quality is critical
- Memory is not a constraint
- Latency is not important
- Working with small to medium datasets

**Use Streaming Denoiser when:**
- Processing live data streams
- Low latency is critical (< 50ms)
- Memory is constrained
- Noise characteristics change over time
- Need continuous, real-time output
- Working with infinite or very large streams

### Parameter Selection

1. **Block Size**:
   - Audio: 256-1024 samples (5-23ms at 44.1kHz)
   - Financial: 32-128 samples (based on tick rate)
   - Sensors: Based on sampling rate and latency requirements

2. **Overlap Factor**:
   - 0%: Fastest, may have artifacts
   - 50%: Good balance of quality and performance
   - 75%: High quality, higher computational cost

3. **Window Function**:
   - Rectangular: No smoothing (use with 0% overlap)
   - Hann: General purpose, good for 50% overlap
   - Hamming: Similar to Hann, slightly different sidelobe behavior
   - Tukey: Compromise between rectangular and Hann

4. **Threshold Method**:
   - Universal: Conservative, preserves signal features
   - SURE: Adaptive, good for varying noise levels
   - Minimax: Optimal worst-case performance
   - BAYES: Bayesian risk minimization, adaptive to signal characteristics

5. **Adaptive Parameters**:
   - Attack time: 5-20ms for responsive adaptation
   - Release time: 20-100ms to avoid pumping artifacts
   - Noise buffer factor: 4-16x block size for stable estimates

### Memory Optimization

1. **Shared Memory Pool**:
   ```java
   // Multiple denoisers share memory pool
   StreamingDenoiser denoiser1 = builder.useSharedMemoryPool(true).build();
   StreamingDenoiser denoiser2 = builder.useSharedMemoryPool(true).build();
   ```

2. **Window Caching**:
   - Windows are automatically cached with LRU eviction
   - Default cache size: 100 windows
   - Configure via system property: `-Dcom.morphiqlabs.wavelet.windowCacheSize=200`
   - Monitor cache: `OverlapBuffer.getWindowCacheSize()`
   - Clear cache if memory constrained: `OverlapBuffer.clearWindowCache()`

### Performance Optimization

1. **Wavelets**:
   - Haar: Fastest, good for piecewise constant signals
   - DB2/DB4: Good balance of quality and speed
   - Higher order: Better frequency selectivity, higher cost

2. **Levels**:
   - Single level: Fastest, suitable for high-frequency noise
   - Multi-level: Better for complex noise, higher cost
   - Rule of thumb: `log2(blockSize) - 3` maximum useful levels

3. **SIMD Optimization**:
   - Automatically enabled via VectorOps
   - Best performance with aligned data
   - Significant speedup on AVX2/AVX512 processors

## Conclusion

VectorWave's denoising capabilities provide flexible solutions for both offline and real-time signal processing. The traditional denoiser excels at batch processing with maximum quality, while the streaming denoiser enables low-latency, memory-efficient processing of continuous data streams. Choose the appropriate approach based on your specific requirements for latency, memory usage, and processing characteristics.
## SIMD Integration (Extensions)

Vector API–based SIMD acceleration lives in the optional `vectorwave-extensions` module (Java 24 + incubator). The core module is a portable scalar Java 21 implementation. For high‑throughput paths, add the extensions dependency and run with:

```
--add-modules jdk.incubator.vector --enable-preview
```
