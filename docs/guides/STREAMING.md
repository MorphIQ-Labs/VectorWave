# Streaming Wavelet Transform and Denoising

This document provides detailed technical information about VectorWave's streaming implementations, including architecture, performance characteristics, and usage guidelines.

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Streaming Denoiser](#streaming-denoiser)
4. [Performance Analysis](#performance-analysis)
5. [Implementation Details](#implementation-details)
6. [Configuration Guide](#configuration-guide)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)

## Overview

VectorWave provides comprehensive streaming support for real-time signal processing applications. The streaming implementations are designed with three key principles:

- **Bounded Memory**: O(1) memory complexity regardless of stream length
- **Low Latency**: Sub-microsecond processing times for real-time applications
- **Flexible Trade-offs**: Configurable quality vs. performance characteristics

### Key Features

- Real-time wavelet transforms with configurable block sizes
- Zero-copy streaming implementation with ring buffer
- Dual-implementation streaming denoiser (Fast vs. Quality)
- Adaptive threshold and noise estimation
- Overlap-add processing with multiple window functions
- Multi-level streaming decomposition
- Memory pooling for reduced GC pressure
- Reactive streams using Java Flow API

## Architecture

### Component Hierarchy

```
StreamingWaveletTransform (interface)
├── StreamingWaveletTransformImpl (basic streaming)
├── OptimizedStreamingWaveletTransform (zero-copy with ring buffer)
├── SlidingWindowTransform (continuous sliding window)
└── MultiLevelStreamingTransform (multi-level decomposition)

StreamingDenoiserStrategy (interface)
├── FastStreamingDenoiser (real-time optimized)
└── QualityStreamingDenoiser (quality optimized)

Supporting Components:
├── RingBuffer (lock-free SPSC buffer)
├── StreamingRingBuffer (windowed ring buffer with overlap)
├── OverlapBuffer (overlap-add processing)
├── NoiseEstimator (adaptive noise estimation)
├── StreamingThresholdAdapter (adaptive thresholding)
└── SharedMemoryPoolManager (memory management)
```

### Data Flow

```
Input Samples → Buffer → Window → Transform → Threshold → Inverse → Output
                  ↑                     ↓
                  └─── Overlap-Add ←────┘
```

## Streaming Denoiser

### Implementation Comparison

| Feature | Fast Implementation | Quality Implementation |
|---------|-------------------|----------------------|
| **Latency** | 0.35-0.70 µs/sample | 0.2-11.4 µs/sample |
| **Throughput** | 1.37-2.69 M samples/s | 0.088-5.0 M samples/s |
| **Memory** | ~22 KB | ~26 KB |
| **SNR vs Batch** | -4.5 to -10.5 dB | +1.5 to +7.3 dB |
| **Real-time** | Always | Only without overlap |
| **Best For** | Audio, sensors, trading | Scientific, medical, offline |

### Selection Comparison

| Implementation | When to Use | Key Features |
|----------------|-------------|--------------|
| StreamingWaveletTransformImpl | General streaming | Standard overlap support |
| OptimizedStreamingWaveletTransform | Performance critical | Zero-copy, ring buffer |
| FastStreamingDenoiser | Real-time denoising | Ultra-low latency |
| QualityStreamingDenoiser | Quality priority | Enhanced SNR |

### Factory Pattern

The `StreamingDenoiserFactory` provides three selection modes:

```java
// Explicit selection
StreamingDenoiserStrategy fast = StreamingDenoiserFactory.create(
    StreamingDenoiserFactory.Implementation.FAST, config);

StreamingDenoiserStrategy quality = StreamingDenoiserFactory.create(
    StreamingDenoiserFactory.Implementation.QUALITY, config);

// Automatic selection based on configuration
StreamingDenoiserStrategy auto = StreamingDenoiserFactory.create(config);
```

### Automatic Selection Logic

The factory uses these criteria for AUTO mode:

1. **Overlap + Adaptive**: Selects FAST (real-time priority)
2. **Block size < 256**: Selects FAST (low latency priority)
3. **No overlap**: Selects QUALITY (both can be real-time)
4. **Default**: FAST (real-time priority)

## Zero-Copy Streaming Implementation

### OptimizedStreamingWaveletTransform

The `OptimizedStreamingWaveletTransform` provides true zero-copy processing using a lock-free ring buffer:

```java
// Basic usage
OptimizedStreamingWaveletTransform transform = new OptimizedStreamingWaveletTransform(
    wavelet, BoundaryMode.PERIODIC, blockSize
);

// With overlap configuration
OptimizedStreamingWaveletTransform transform = new OptimizedStreamingWaveletTransform(
    wavelet, 
    BoundaryMode.PERIODIC, 
    blockSize,
    0.5,  // 50% overlap
    8     // buffer capacity multiplier
);
```

### Key Benefits

1. **Zero-Copy Processing**: Uses `WaveletTransform.forward(double[], int, int)` to process array slices directly
2. **50% Memory Bandwidth Reduction**: Eliminates array copying during transforms
3. **Lock-Free Ring Buffer**: Single-producer, single-consumer design for minimal overhead
4. **Configurable Overlap**: 0-100% overlap support for time-frequency trade-offs
5. **Automatic Backpressure**: Exponential backoff when buffer is full

### Performance Characteristics

| Metric | Value |
|--------|-------|
| Latency | < 0.5 µs/sample |
| Memory bandwidth | 50% reduction vs copying |
| GC pressure | Minimal (buffer reuse) |
| Thread safety | SPSC lock-free |
| Overlap support | 0-100% |

## Performance Analysis

### Latency Breakdown

For a typical streaming denoiser processing cycle:

| Operation | Fast (µs) | Quality (µs) |
|-----------|-----------|--------------|
| Buffer management | 0.05-0.10 | 0.10-0.20 |
| Windowing | 0.00 | 0.05-2.00 |
| Forward transform | 0.15-0.30 | 0.30-0.60 |
| Thresholding | 0.05-0.10 | 0.10-0.20 |
| Inverse transform | 0.10-0.20 | 0.20-0.40 |
| **Total** | **0.35-0.70** | **0.75-3.40** |

### Memory Usage

Memory allocation per instance:

```
FastStreamingDenoiser:
- Input buffer: blockSize × 8 bytes
- Transform buffers: 2 × blockSize × 8 bytes
- Statistics: ~200 bytes
- Noise estimator: blockSize × 8 bytes
- Total: ~22 KB (for blockSize=256)

QualityStreamingDenoiser:
- Extended buffers: 1.5 × blockSize × 8 bytes
- Overlap buffers: 2 × blockSize × 8 bytes
- Window cache: ~2 KB
- Additional: ~2 KB
- Total: ~26 KB (for blockSize=256)
```

### Quality Metrics

SNR improvement compared to noisy input:

| Noise Level | Fast | Quality | Batch (reference) |
|-------------|------|---------|-------------------|
| 0.1 (low) | +2.5 dB | +7.0 dB | +12.0 dB |
| 0.3 (medium) | +1.0 dB | +5.5 dB | +8.0 dB |
| 0.5 (high) | -0.5 dB | +3.0 dB | +5.0 dB |

## Implementation Details

### Fast Implementation

```java
// Simplified processing flow
void processFast(double[] samples) {
    // 1. Buffer samples
    buffer.add(samples);
    
    // 2. When block is full
    if (buffer.isFull()) {
        double[] block = buffer.getBlock();
        
        // 3. Direct transform (no windowing)
        TransformResult result = transform.forward(block);
        
        // 4. Apply threshold
        threshold(result.getCoefficients());
        
        // 5. Inverse transform
        double[] denoised = transform.inverse(result);
        
        // 6. Emit result
        publisher.submit(denoised);
    }
}
```

### Quality Implementation

```java
// Simplified processing flow with overlap
void processQuality(double[] samples) {
    // 1. Add to overlap buffer
    overlapBuffer.add(samples);
    
    // 2. Process overlapping blocks
    while (overlapBuffer.hasBlock()) {
        double[] extendedBlock = overlapBuffer.getExtendedBlock();
        
        // 3. Apply window function
        applyWindow(extendedBlock);
        
        // 4. Transform extended block
        TransformResult result = transform.forward(extendedBlock);
        
        // 5. Apply threshold with smoothing
        adaptiveThreshold(result.getCoefficients());
        
        // 6. Inverse transform
        double[] denoised = transform.inverse(result);
        
        // 7. Overlap-add reconstruction
        double[] output = overlapBuffer.overlapAdd(denoised);
        
        // 8. Emit result
        publisher.submit(output);
    }
}
```

### Adaptive Processing

Both implementations support adaptive noise estimation and thresholding:

```java
// Noise estimation (MAD-based)
double estimateNoise(double[] coefficients) {
    double[] sorted = Arrays.copyOf(coefficients, coefficients.length);
    Arrays.sort(sorted);
    double median = sorted[sorted.length / 2];
    
    double[] deviations = new double[coefficients.length];
    for (int i = 0; i < coefficients.length; i++) {
        deviations[i] = Math.abs(coefficients[i] - median);
    }
    
    Arrays.sort(deviations);
    double mad = deviations[deviations.length / 2];
    
    return mad / 0.6745; // Gaussian normalization
}

// Adaptive threshold with smoothing
double adaptThreshold(double newEstimate) {
    double rate = isIncreasing ? attackTime : releaseTime;
    currentThreshold = currentThreshold + rate * (newEstimate - currentThreshold);
    return currentThreshold;
}
```

## Configuration Guide

### Basic Configuration

```java
StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
    .wavelet(Daubechies.DB4)           // Wavelet type
    .blockSize(256)                    // Power of 2
    .overlapFactor(0.5)                // 0.0 to 0.875
    .thresholdMethod(ThresholdMethod.UNIVERSAL)
    .thresholdType(ThresholdType.SOFT)
    .build();
```

### Advanced Configuration

```java
StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
    .wavelet(Symlet.SYM8)
    .blockSize(512)
    .overlapFactor(0.75)
    .levels(3)                         // Multi-level decomposition
    .thresholdMethod(ThresholdMethod.SURE)
    .thresholdType(ThresholdType.HARD)
    .adaptiveThreshold(true)           // Enable adaptation
    .attackTime(0.1)                   // Fast attack
    .releaseTime(0.5)                  // Slower release
    .useSharedMemoryPool(true)         // Share memory pool
    .noiseBufferFactor(4)              // Noise estimation buffer
    .build();
```

### Configuration Parameters

| Parameter | Range | Default | Description |
|-----------|-------|---------|-------------|
| `blockSize` | 32-8192 | 256 | Processing block size (must be power of 2) |
| `overlapFactor` | 0.0-0.875 | 0.0 | Overlap between blocks |
| `levels` | 1-10 | 1 | Decomposition levels |
| `adaptiveThreshold` | true/false | false | Enable adaptive thresholding |
| `attackTime` | 0.01-1.0 | 0.1 | Threshold increase rate |
| `releaseTime` | 0.01-10.0 | 0.5 | Threshold decrease rate |
| `noiseBufferFactor` | 1-32 | 4 | Buffer size for noise estimation |

## Best Practices

### 1. Choose the Right Implementation

```java
// Real-time audio processing
if (latencyRequirement < 1_000) { // < 1ms
    use(Implementation.FAST);
}

// Scientific analysis
if (qualityRequirement > latencyRequirement) {
    use(Implementation.QUALITY);
}

// General purpose
use(Implementation.AUTO); // Let factory decide
```

### 2. Optimize Block Size

- **Smaller blocks** (64-256): Lower latency, reduced frequency resolution
- **Medium blocks** (256-1024): Balanced performance
- **Larger blocks** (1024-4096): Better frequency resolution, higher latency

### 3. Configure Overlap Appropriately

```java
// No overlap for lowest latency
.overlapFactor(0.0)

// 50% overlap for smooth reconstruction
.overlapFactor(0.5)

// 75% overlap for highest quality (may prevent real-time)
.overlapFactor(0.75)
```

### 4. Memory Pool Usage

```java
// Multiple instances sharing memory
for (int i = 0; i < numChannels; i++) {
    configs[i] = new StreamingDenoiserConfig.Builder()
        .useSharedMemoryPool(true)  // Share pool
        .build();
}

// Single instance or isolation needed
config = new StreamingDenoiserConfig.Builder()
    .useSharedMemoryPool(false)  // Dedicated pool
    .build();
```

### 5. Resource Management

```java
// Always use try-with-resources
try (StreamingDenoiserStrategy denoiser = factory.create(config)) {
    // Process data
    denoiser.process(samples);
} // Automatic cleanup

// Or explicit cleanup
denoiser.close(); // Releases memory pool
```

### 6. Performance Monitoring

```java
// Monitor performance metrics
StreamingStatistics stats = denoiser.getStatistics();
if (stats.getMaxProcessingTime() > targetLatency) {
    logger.warn("Processing time exceeded target: {} > {}",
        stats.getMaxProcessingTime(), targetLatency);
}

// Check buffer levels
if (denoiser.getBufferLevel() > blockSize * 0.8) {
    logger.warn("Buffer filling up, possible overload");
}
```

## Troubleshooting

### Common Issues

#### 1. High Latency Spikes

**Symptoms**: Occasional processing delays exceeding target

**Causes**:
- GC pauses
- Thread contention
- CPU throttling

**Solutions**:
```java
// Use memory pooling
.useSharedMemoryPool(true)

// Reduce block size
.blockSize(128)

// Disable overlap
.overlapFactor(0.0)

// Use FAST implementation
Implementation.FAST
```

#### 2. Poor Denoising Quality

**Symptoms**: Insufficient noise reduction or signal distortion

**Causes**:
- Block boundary artifacts
- Inappropriate threshold
- Wrong wavelet choice

**Solutions**:
```java
// Use QUALITY implementation
Implementation.QUALITY

// Enable overlap
.overlapFactor(0.5)

// Use adaptive thresholding
.adaptiveThreshold(true)

// Choose appropriate wavelet
.wavelet(Daubechies.DB4) // Good general purpose
```

#### 3. Memory Issues

**Symptoms**: OutOfMemoryError or high GC activity

**Causes**:
- Too many instances
- Large block sizes
- Memory leaks

**Solutions**:
```java
// Share memory pools
.useSharedMemoryPool(true)

// Ensure proper cleanup
try (StreamingDenoiserStrategy denoiser = ...) {
    // Use denoiser
} // Automatic cleanup

// Monitor active instances
SharedMemoryPoolManager.getInstance().getActiveUserCount()
```

### Performance Tuning Checklist

1. **Profile First**: Use benchmarks to establish baseline
2. **Start Simple**: Begin with FAST implementation, no overlap
3. **Measure Impact**: Monitor latency and quality metrics
4. **Adjust Gradually**: Change one parameter at a time
5. **Validate Results**: Ensure quality meets requirements

### Debug Output

Enable detailed logging for troubleshooting:

```java
// Get current metrics
double latency = denoiser.getPerformanceProfile().expectedLatencyMicros();
double snr = denoiser.getPerformanceProfile().expectedSNRImprovement();
long memory = denoiser.getPerformanceProfile().memoryUsageBytes();

// Log statistics
logger.debug("Samples: {}, Blocks: {}, Avg time: {} ms, Max time: {} ms",
    stats.getSamplesProcessed(),
    stats.getBlocksEmitted(),
    stats.getAverageProcessingTime(),
    stats.getMaxProcessingTime());

// Monitor adaptive parameters
logger.debug("Noise level: {}, Threshold: {}",
    denoiser.getCurrentNoiseLevel(),
    denoiser.getCurrentThreshold());
```

## References

1. Mallat, S. (2008). A Wavelet Tour of Signal Processing (3rd ed.)
2. Donoho, D. L. (1995). De-noising by soft-thresholding
3. Johnstone, I. M., & Silverman, B. W. (1997). Wavelet threshold estimators