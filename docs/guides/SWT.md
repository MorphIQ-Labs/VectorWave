# Stationary Wavelet Transform (SWT) Guide

## Overview

The Stationary Wavelet Transform (SWT), also known as the Undecimated Wavelet Transform or À Trous Algorithm, is a shift-invariant wavelet transform that maintains the same data length at each decomposition level. VectorWave provides SWT functionality through the `VectorWaveSwtAdapter` class, which leverages the optimized MODWT implementation.

## Key Properties

- **Shift-invariant**: Pattern detection is consistent regardless of signal position
- **Redundant representation**: All levels have the same length as the original signal
- **Perfect reconstruction**: Signal can be exactly reconstructed from coefficients
- **Arbitrary signal length**: No power-of-2 restriction unlike standard DWT

## Relationship to MODWT

SWT and MODWT (Maximal Overlap Discrete Wavelet Transform) are mathematically equivalent transforms with different historical origins. Both provide shift-invariant, redundant wavelet decompositions. VectorWave's SWT adapter provides a familiar SWT interface while leveraging the optimized MODWT implementation with SIMD acceleration.

## Basic Usage

### Creating an SWT Adapter

```java
import com.morphiqlabs.wavelet.swt.VectorWaveSwtAdapter;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.BoundaryMode;

// Create with specific boundary mode
VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(
    Daubechies.DB4, BoundaryMode.PERIODIC);

// Or use default periodic boundaries
VectorWaveSwtAdapter swt2 = new VectorWaveSwtAdapter(Daubechies.DB4);
```

### Forward and Inverse Transform

```java
// Forward SWT decomposition
double[] signal = getSignalData();
MutableMultiLevelMODWTResult swtResult = swt.forward(signal, 3); // 3 levels

// Access coefficients
double[] level1Details = swtResult.getDetailCoeffsAtLevel(1);
double[] approximation = swtResult.getApproximationCoeffs();

// Inverse SWT reconstruction
double[] reconstructed = swt.inverse(swtResult);
```

## Coefficient Modification

The SWT adapter returns mutable results, allowing direct coefficient modification for applications like denoising or feature extraction.

### Direct Modification

```java
// Get mutable coefficients
double[] details = swtResult.getMutableDetailCoeffs(1);

// Modify coefficients directly
for (int i = 0; i < details.length; i++) {
    if (Math.abs(details[i]) < threshold) {
        details[i] = 0; // Zero small coefficients
    }
}

// Clear caches after modification
swtResult.clearCaches();

// Reconstruct with modified coefficients
double[] processed = swt.inverse(swtResult);
```

### Thresholding Methods

```java
// Hard thresholding at level 2
swt.applyThreshold(swtResult, 2, 0.5, false);

// Soft thresholding at level 1
swt.applyThreshold(swtResult, 1, 0.3, true);

// Universal threshold (automatic threshold calculation)
swt.applyUniversalThreshold(swtResult, true); // soft thresholding
```

## Denoising Applications

### Basic Denoising

```java
// Simple denoising with universal threshold
double[] noisy = getNoisySignal();
double[] denoised = swt.denoise(noisy, 4); // 4 levels, universal threshold

// Custom threshold denoising
double customThreshold = 0.5;
double[] denoised2 = swt.denoise(noisy, 4, customThreshold, true); // soft
```

### Advanced Denoising

```java
// Manual control over denoising process
MutableMultiLevelMODWTResult result = swt.forward(noisySignal, 5);

// Different thresholds for different levels
swt.applyThreshold(result, 1, 0.8, true);  // High threshold for finest details
swt.applyThreshold(result, 2, 0.5, true);  // Medium threshold
swt.applyThreshold(result, 3, 0.3, true);  // Lower threshold
// Keep levels 4 and 5 unchanged (lower frequencies)

double[] denoised = swt.inverse(result);
```

## Feature Extraction

### Single Level Extraction

```java
// Extract only level 2 details (bandpass filtering)
double[] level2Features = swt.extractLevel(signal, 4, 2);

// Extract only approximation (lowpass filtering)
double[] smoothed = swt.extractLevel(signal, 4, 0);
```

### Multi-Resolution Analysis

```java
// Decompose signal
MutableMultiLevelMODWTResult result = swt.forward(signal, 5);

// Analyze energy distribution
for (int level = 1; level <= 5; level++) {
    double energy = result.getDetailEnergyAtLevel(level);
    System.out.printf("Level %d energy: %.4f\n", level, energy);
}

// Extract dominant frequency band
double[] energies = result.getRelativeEnergyDistribution();
int dominantLevel = findMaxIndex(energies);
double[] dominant = swt.extractLevel(signal, 5, dominantLevel);
```

## Performance Optimization

### Batch Processing

```java
// Process multiple signals efficiently
double[][] signals = getMultipleSignals();
MutableMultiLevelMODWTResult[] results = new MutableMultiLevelMODWTResult[signals.length];

for (int i = 0; i < signals.length; i++) {
    results[i] = swt.forward(signals[i], 3);
    swt.applyUniversalThreshold(results[i], true);
}

// Reconstruct all
double[][] denoised = new double[signals.length][];
for (int i = 0; i < signals.length; i++) {
    denoised[i] = swt.inverse(results[i]);
}
```

### Memory Management

```java
// Convert to immutable for thread-safe sharing
MultiLevelMODWTResult immutable = mutableResult.toImmutable();

// Create mutable copy when needed
MutableMultiLevelMODWTResult mutableCopy = 
    new MutableMultiLevelMODWTResultImpl(immutable);
```

## Complete Example: Signal Denoising Pipeline

```java
import com.morphiqlabs.wavelet.swt.VectorWaveSwtAdapter;
import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResult;

public class SWTDenoisingPipeline {
    
    public static double[] processSignal(double[] noisySignal) {
        // 1. Create SWT adapter with appropriate wavelet
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(
            Symlet.SYM8, BoundaryMode.SYMMETRIC);
        
        // 2. Determine optimal decomposition depth
        int levels = (int) (Math.log(noisySignal.length) / Math.log(2)) - 2;
        levels = Math.min(levels, 6); // Cap at 6 levels
        
        // 3. Perform SWT decomposition
        MutableMultiLevelMODWTResult swtResult = swt.forward(noisySignal, levels);
        
        // 4. Estimate noise level from finest details
        double[] finestDetails = swtResult.getDetailCoeffsAtLevel(1);
        double noiseEstimate = estimateNoiseMAD(finestDetails);
        
        // 5. Apply level-dependent thresholding
        for (int level = 1; level <= levels; level++) {
            // Decrease threshold for coarser levels
            double levelThreshold = noiseEstimate * Math.sqrt(2 * Math.log(noisySignal.length));
            levelThreshold *= Math.pow(0.7, level - 1);
            
            // Use soft thresholding for better continuity
            swt.applyThreshold(swtResult, level, levelThreshold, true);
        }
        
        // 6. Reconstruct denoised signal
        return swt.inverse(swtResult);
    }
    
    private static double estimateNoiseMAD(double[] coeffs) {
        // Median Absolute Deviation estimator
        double[] absCoeffs = new double[coeffs.length];
        for (int i = 0; i < coeffs.length; i++) {
            absCoeffs[i] = Math.abs(coeffs[i]);
        }
        Arrays.sort(absCoeffs);
        double median = absCoeffs[absCoeffs.length / 2];
        return median / 0.6745; // Scale for Gaussian noise
    }
}
```

## Best Practices

1. **Wavelet Selection**: 
   - Use smoother wavelets (DB4-DB8, SYM4-SYM8) for denoising
   - Use Haar or DB2 for edge detection
   - Use Coiflets for numerical analysis

2. **Level Selection**:
   - For denoising: 4-6 levels typically sufficient
   - For analysis: log₂(N) - 2 levels maximum
   - Consider signal characteristics and sampling rate

3. **Thresholding**:
   - Soft thresholding for smooth reconstructions
   - Hard thresholding to preserve signal features
   - Universal threshold as starting point, adjust based on results

4. **Performance**:
   - Reuse SWT adapter instances when processing multiple signals
   - Use mutable results for in-place modifications
   - Convert to immutable for thread-safe operations

## Implementation Details

The SWT adapter provides:

1. **Mutable multi-level results** for coefficient manipulation
2. **SWT-style interface** familiar to users of other wavelet libraries  
3. **Efficient implementation** leveraging VectorWave's optimized MODWT
4. **Comprehensive denoising capabilities** with various thresholding options
5. **Feature extraction methods** for multi-resolution analysis

The adapter serves as a bridge between SWT terminology and VectorWave's MODWT implementation, providing the best of both worlds: familiar interface with optimized performance.
## SIMD Integration (Extensions)

For SIMD acceleration, use the optional `vectorwave-extensions` module (Java 24 + incubator). The core module is scalar Java 21. To enable Vector API execution, add the extensions dependency and run with:

```
--add-modules jdk.incubator.vector --enable-preview
```
