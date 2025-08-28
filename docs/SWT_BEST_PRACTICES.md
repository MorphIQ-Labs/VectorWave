# SWT (Stationary Wavelet Transform) Best Practices Guide

## Overview

The Stationary Wavelet Transform (SWT), also known as the Undecimated Wavelet Transform or À Trous Algorithm, provides shift-invariant wavelet analysis through VectorWave's `VectorWaveSwtAdapter`. This guide covers best practices for using SWT with automatic internal optimizations.

## Key Features

### Automatic Optimizations (No API Changes Required)

1. **Filter Precomputation**: Upsampled filters for à trous algorithm are automatically cached
2. **Parallel Processing**: Large signals (≥4096 samples) automatically use multi-threading
3. **Memory Efficiency**: Sparse storage available for mostly-zero coefficients
4. **Resource Management**: Built-in cleanup for long-running applications

## Quick Start

```java
// Basic SWT usage - optimizations are automatic!
VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(
    Daubechies.DB4, BoundaryMode.PERIODIC);

// Forward transform
MutableMultiLevelMODWTResult result = swt.forward(signal, 4);

// Inverse transform
double[] reconstructed = swt.inverse(result);

// Clean up when done
swt.cleanup();
```

## Best Practices

### 1. Choosing Decomposition Levels

The optimal number of decomposition levels depends on signal length and wavelet filter length:

```java
int calculateOptimalLevels(int signalLength, int filterLength) {
    // Rule: (filterLength - 1) * (2^level - 1) < signalLength
    int maxLevel = 1;
    while ((filterLength - 1) * (Math.pow(2, maxLevel) - 1) < signalLength) {
        maxLevel++;
    }
    
    // Practical limit: 3-6 levels is usually sufficient
    return Math.min(maxLevel - 1, 6);
}
```

**Guidelines:**
- **Short signals (< 512)**: 2-3 levels
- **Medium signals (512-4096)**: 3-5 levels
- **Long signals (> 4096)**: 4-6 levels
- More levels = more computational cost with diminishing returns

### 2. Denoising Strategies

#### Universal Threshold (Automatic)
Best for general-purpose denoising when noise characteristics are unknown:

```java
// Universal threshold calculated as σ√(2log(N))
double[] denoised = swt.denoise(noisySignal, 4, -1, true);
```

#### Custom Threshold
When you know the noise level:

```java
double threshold = 0.2; // Based on known noise characteristics
double[] denoised = swt.denoise(noisySignal, 4, threshold, true);
```

#### Level-Specific Thresholding
For optimal results when different scales have different noise levels:

```java
MutableMultiLevelMODWTResult result = swt.forward(noisySignal, 4);

// Apply different thresholds at different levels
swt.applyThreshold(result, 1, 0.3, true);  // Finest details - higher threshold
swt.applyThreshold(result, 2, 0.2, true);
swt.applyThreshold(result, 3, 0.1, true);
swt.applyThreshold(result, 4, 0.05, true); // Coarsest details - lower threshold

double[] denoised = swt.inverse(result);
```

### 3. Memory Management

#### For Dense Signals
Standard usage is fine for most signals:

```java
VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(wavelet);
MutableMultiLevelMODWTResult result = swt.forward(signal, levels);
```

#### For Sparse Signals
Use sparse storage for signals with many near-zero coefficients:

```java
// Create SWT result
SWTResult denseResult = new SWTResult(approx, details, levels);

// Convert to sparse (3x compression typical)
SWTResult.SparseSWTResult sparseResult = denseResult.toSparse(0.01);

// Reconstruct when needed
SWTResult reconstructed = sparseResult.toFull();
```

### 4. Performance Optimization

#### Reuse Adapters
Create once, use multiple times:

```java
// Good: Reuse adapter
VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4);
for (double[] signal : signals) {
    MutableMultiLevelMODWTResult result = swt.forward(signal, 3);
    // Process result
}
swt.cleanup();

// Bad: Creating new adapter each time
for (double[] signal : signals) {
    VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4);
    // ... inefficient
}
```

#### Batch Processing
Process multiple signals efficiently:

```java
VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(wavelet);

// Process batch
List<MutableMultiLevelMODWTResult> results = new ArrayList<>();
for (double[] signal : signalBatch) {
    results.add(swt.forward(signal, levels));
}

// Clean up after batch
swt.cleanup();
```

### 5. Resource Management

Always clean up in long-running applications:

```java
VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(wavelet);
try {
    // Use SWT for processing
    processSignals(swt);
} finally {
    // Always cleanup
    swt.cleanup();
}
```

The adapter remains functional after cleanup but without optimizations.

### 6. Wavelet Selection

Different wavelets suit different applications:

| Wavelet | Best For | Characteristics |
|---------|----------|-----------------|
| **Haar** | Sharp transitions | Shortest filter, fastest |
| **DB2-DB4** | General purpose | Good time-frequency balance |
| **DB6-DB8** | Smooth signals | Better frequency resolution |
| **SYM4-SYM8** | Symmetric features | Near-linear phase |
| **COIF1-COIF3** | Vanishing moments | Good for polynomial trends |

### 7. Boundary Handling

Choose appropriate boundary mode:

```java
// Periodic - best for periodic signals
new VectorWaveSwtAdapter(wavelet, BoundaryMode.PERIODIC);

// Symmetric - best for finite signals
new VectorWaveSwtAdapter(wavelet, BoundaryMode.SYMMETRIC);

// Zero padding - simple but can introduce artifacts
new VectorWaveSwtAdapter(wavelet, BoundaryMode.ZERO);
```

## Common Use Cases

### Signal Denoising

```java
public double[] denoiseSignal(double[] noisySignal) {
    VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(
        Daubechies.DB6, BoundaryMode.SYMMETRIC);
    
    try {
        // Automatic universal threshold
        return swt.denoise(noisySignal, 4, -1, true);
    } finally {
        swt.cleanup();
    }
}
```

### Feature Extraction

```java
public double[] extractFeatures(double[] signal, int targetLevel) {
    VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(
        Symlet.SYM4, BoundaryMode.PERIODIC);
    
    try {
        // Extract features at specific scale
        return swt.extractLevel(signal, 5, targetLevel);
    } finally {
        swt.cleanup();
    }
}
```

### Multi-Resolution Analysis

```java
public void analyzeSignal(double[] signal) {
    VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4);
    
    try {
        MutableMultiLevelMODWTResult result = swt.forward(signal, 4);
        
        // Analyze each level
        for (int level = 1; level <= 4; level++) {
            double[] details = result.getMutableDetailCoeffs(level);
            double energy = calculateEnergy(details);
            System.out.printf("Level %d energy: %.4f\n", level, energy);
        }
    } finally {
        swt.cleanup();
    }
}
```

## Performance Characteristics

### Automatic Optimization Triggers

| Signal Size | Processing Mode | Expected Performance |
|------------|-----------------|---------------------|
| < 1024 | Sequential | Fast, low overhead |
| 1024-4096 | Sequential + Cache | Optimal for repeated transforms |
| ≥ 4096 | Parallel + Cache | 2-4x speedup on multi-core |

### Memory Usage

- **Base overhead**: O(signal_length × levels)
- **With sparse storage**: Reduced by compression ratio (typically 3-10x)
- **Filter cache**: Negligible (< 1KB per level)

## Troubleshooting

### Issue: Out of Memory

**Solution**: Use fewer decomposition levels or sparse storage:

```java
// Reduce levels
swt.forward(signal, 3); // Instead of 6

// Or use sparse storage
SWTResult.SparseSWTResult sparse = result.toSparse(0.01);
```

### Issue: Slow Performance

**Solution**: Check signal size and reuse adapters:

```java
// Ensure signal is large enough for parallel processing
if (signal.length < 4096) {
    // Consider batching smaller signals
}

// Reuse adapter for multiple transforms
VectorWaveSwtAdapter swt = createSharedAdapter();
```

### Issue: Poor Denoising Results

**Solution**: Tune threshold strategy:

```java
// Try different threshold approaches
// 1. Adjust threshold value
double customThreshold = estimateNoiseLevel(signal) * 3;

// 2. Use soft vs hard thresholding
swt.denoise(signal, levels, threshold, true);  // soft
swt.denoise(signal, levels, threshold, false); // hard

// 3. Level-specific thresholds
applyAdaptiveThresholds(result);
```

## Advanced Topics

### Custom Filter Wavelets

While the library handles optimization automatically, you can create custom wavelets:

```java
public class CustomWavelet implements DiscreteWavelet {
    // Implementation details
}

// SWT adapter automatically optimizes custom wavelets
VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(new CustomWavelet());
```

### Monitoring Optimization Status

For debugging and performance tuning:

```java
Map<String, Object> stats = swt.getCacheStatistics();
System.out.println("Filter cache size: " + stats.get("filterCacheSize"));
System.out.println("Parallel active: " + stats.get("parallelExecutorActive"));
```

## Summary

The SWT implementation in VectorWave provides:

1. **Automatic optimizations** - No special API needed
2. **Shift-invariant analysis** - Better for pattern detection
3. **Perfect reconstruction** - Numerically stable
4. **Flexible denoising** - Multiple threshold strategies
5. **Memory efficiency** - Sparse storage options
6. **Resource management** - Built-in cleanup

Follow these best practices to get optimal performance and results from SWT in your applications.