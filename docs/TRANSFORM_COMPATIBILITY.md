# Transform Compatibility Guide

VectorWave provides an explicit Transform Compatibility API that clearly shows which wavelets work with which transforms, improving discoverability and preventing runtime errors.

> SIMD Integration (Extensions)
>
> SIMD/Vector API acceleration is provided by the optional `vectorwave-extensions` module (Java 24 + incubator). The core transform implementations are scalar Java 21. For SIMD paths, add the extensions dependency and run with Vector API flags.

## Overview

The Transform Compatibility API provides:
- **Explicit compatibility checking** - Know which wavelets work with which transforms
- **Type-safe validation** - Verify compatibility before creating transforms
- **Discoverability** - Find appropriate wavelets for your chosen transform
- **Recommendations** - Get suggested transforms for specific wavelets

## Available Transforms

VectorWave supports five main transform types:

| Transform | Description | Compatible Wavelets | Use Cases |
|-----------|-------------|-------------------|-----------|
| **MODWT** | Maximal Overlap Discrete Wavelet Transform | Orthogonal, Biorthogonal | Time series analysis, denoising, feature detection |
| **SWT** | Stationary Wavelet Transform | Orthogonal, Biorthogonal | Denoising, pattern recognition |
| **CWT** | Continuous Wavelet Transform | Continuous, Complex | Spectral analysis, singularity detection |
| **Multi-Level MODWT** | Hierarchical MODWT decomposition | Orthogonal, Biorthogonal | Multi-resolution analysis, trend extraction |
| **Streaming MODWT** | Real-time MODWT processing | Orthogonal, Biorthogonal | Online processing, real-time denoising |

## API Usage

### Check Wavelet Compatibility

Find which transforms a specific wavelet supports:

```java
import com.morphiqlabs.wavelet.api.*;

// Check what transforms DB4 supports
Set<TransformType> supported = WaveletRegistry.getSupportedTransforms(WaveletName.DB4);
for (TransformType transform : supported) {
    System.out.println(transform.getDescription());
}
// Output: MODWT, SWT, Multi-Level MODWT, Streaming MODWT

// Check what transforms Morlet supports
Set<TransformType> morletTransforms = WaveletRegistry.getSupportedTransforms(WaveletName.MORLET);
// Output: CWT only
```

### Verify Compatibility

Check if a specific wavelet-transform pair is compatible:

```java
// Check compatibility before creating transform
if (WaveletRegistry.isCompatible(WaveletName.DB4, TransformType.MODWT)) {
    Wavelet db4 = WaveletRegistry.getWavelet(WaveletName.DB4);
    MODWTTransform transform = new MODWTTransform(db4, BoundaryMode.PERIODIC);
}

// This would return false - DB4 cannot be used with CWT
boolean invalid = WaveletRegistry.isCompatible(WaveletName.DB4, TransformType.CWT);
```

### Find Wavelets for Transform

Get all wavelets compatible with a specific transform:

```java
// Find all wavelets that work with MODWT
List<WaveletName> modwtWavelets = WaveletRegistry.getWaveletsForTransform(TransformType.MODWT);
// Returns: HAAR, DB2, DB4, DB6, DB8, DB10, SYM2, SYM3, ... etc

// Find all wavelets that work with CWT
List<WaveletName> cwtWavelets = WaveletRegistry.getWaveletsForTransform(TransformType.CWT);
// Returns: MORLET, MEXICAN_HAT, GAUSSIAN, PAUL, DOG, etc
```

### Get Recommendations

Get the recommended transform for a wavelet based on its type:

```java
// Get recommendation for DB4
TransformType recommended = WaveletRegistry.getRecommendedTransform(WaveletName.DB4);
// Returns: MODWT (best for discrete orthogonal wavelets)

// Get recommendation for Morlet
TransformType morletRec = WaveletRegistry.getRecommendedTransform(WaveletName.MORLET);
// Returns: CWT (only option for continuous wavelets)
```

## Compatibility Matrix

### Discrete Wavelets (Orthogonal/Biorthogonal)

| Wavelet | MODWT | SWT | Multi-Level | Streaming | CWT |
|---------|-------|-----|-------------|-----------|-----|
| HAAR | ✓ | ✓ | ✓ | ✓ | - |
| DB2-DB10 | ✓ | ✓ | ✓ | ✓ | - |
| SYM2-SYM20 | ✓ | ✓ | ✓ | ✓ | - |
| COIF1-COIF5 | ✓ | ✓ | ✓ | ✓ | - |

### Continuous Wavelets

| Wavelet | MODWT | SWT | Multi-Level | Streaming | CWT |
|---------|-------|-----|-------------|-----------|-----|
| MORLET | - | - | - | - | ✓ |
| MEXICAN_HAT | - | - | - | - | ✓ |
| GAUSSIAN | - | - | - | - | ✓ |
| PAUL | - | - | - | - | ✓ |
| DOG | - | - | - | - | ✓ |
| SHANNON | - | - | - | - | ✓ |

### Complex Wavelets

| Wavelet | MODWT | SWT | Multi-Level | Streaming | CWT |
|---------|-------|-----|-------------|-----------|-----|
| CMOR | - | - | - | - | ✓ |
| CGAU | - | - | - | - | ✓ |

## Usage Examples

### Example 1: Validate Before Transform Creation

```java
public void processSignal(double[] signal, WaveletName waveletName) {
    // Check if the wavelet can be used with MODWT
    if (!WaveletRegistry.isCompatible(waveletName, TransformType.MODWT)) {
        throw new IllegalArgumentException(
            waveletName + " cannot be used with MODWT. Supported transforms: " +
            WaveletRegistry.getSupportedTransforms(waveletName)
        );
    }
    
    // Safe to proceed
    Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
    MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
    MODWTResult result = transform.forward(signal);
}
```

### Example 2: UI Dropdown Population

```java
public class WaveletSelector {
    private JComboBox<WaveletName> waveletDropdown;
    private TransformType selectedTransform;
    
    public void updateWaveletDropdown(TransformType transform) {
        // Get only compatible wavelets for the selected transform
        List<WaveletName> compatible = WaveletRegistry.getWaveletsForTransform(transform);
        
        waveletDropdown.removeAllItems();
        for (WaveletName wavelet : compatible) {
            waveletDropdown.addItem(wavelet);
        }
    }
}
```

### Example 3: Automatic Transform Selection

```java
public void analyzeWithBestTransform(double[] signal, WaveletName waveletName) {
    // Get recommended transform for the wavelet
    TransformType bestTransform = WaveletRegistry.getRecommendedTransform(waveletName);
    
    switch (bestTransform) {
        case MODWT -> {
            Wavelet w = WaveletRegistry.getWavelet(waveletName);
            MODWTTransform transform = new MODWTTransform(w, BoundaryMode.PERIODIC);
            MODWTResult result = transform.forward(signal);
            // Process MODWT result...
        }
        case CWT -> {
            // Note: CWT wavelets need to be created directly, not from registry
            if (waveletName == WaveletName.MORLET) {
                CWTTransform transform = new CWTTransform(new MorletWavelet());
                CWTResult result = transform.analyze(signal, ScaleSpace.logarithmic(1, 100, 50));
                // Process CWT result...
            }
        }
        default -> throw new UnsupportedOperationException(
            "Transform " + bestTransform + " not yet implemented"
        );
    }
}
```

### Example 4: Display Compatibility Information

```java
public void showWaveletInfo(WaveletName waveletName) {
    Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
    
    System.out.println("Wavelet: " + waveletName.getDescription());
    System.out.println("Type: " + waveletName.getType());
    System.out.println("Code: " + waveletName.getCode());
    
    System.out.println("\nCompatible Transforms:");
    Set<TransformType> transforms = WaveletRegistry.getSupportedTransforms(waveletName);
    for (TransformType transform : transforms) {
        System.out.println("  - " + transform.getDescription());
    }
    
    System.out.println("\nRecommended: " + 
        WaveletRegistry.getRecommendedTransform(waveletName).getDescription());
}
```

## Benefits

1. **Compile-Time Safety**: The enum-based approach ensures type safety
2. **Runtime Validation**: Verify compatibility before attempting operations
3. **Clear Documentation**: Explicit API makes capabilities discoverable
4. **Error Prevention**: Avoid runtime errors from incompatible combinations
5. **UI Support**: Easy to build dynamic UIs that only show valid options

## Migration Guide

If you have existing code that assumes wavelet-transform compatibility:

```java
// Old approach - might fail at runtime
Wavelet wavelet = WaveletRegistry.getWavelet(userSelectedWavelet);
CWTTransform cwt = new CWTTransform(wavelet); // May fail if not continuous!

// New approach - validate first
if (WaveletRegistry.isCompatible(userSelectedWavelet, TransformType.CWT)) {
    // Safe to proceed
    Wavelet wavelet = WaveletRegistry.getWavelet(userSelectedWavelet);
    CWTTransform cwt = new CWTTransform((ContinuousWavelet) wavelet);
} else {
    // Handle incompatibility
    Set<TransformType> supported = WaveletRegistry.getSupportedTransforms(userSelectedWavelet);
    throw new IllegalArgumentException(
        userSelectedWavelet + " cannot be used with CWT. Supported: " + supported
    );
}
```

## Summary

The Transform Compatibility API provides:
- **Clear compatibility rules** between wavelets and transforms
- **Runtime validation** to prevent errors
- **Discovery methods** to find appropriate wavelets
- **Recommendations** for optimal transform selection

This explicit API removes ambiguity and makes VectorWave easier to use correctly.
