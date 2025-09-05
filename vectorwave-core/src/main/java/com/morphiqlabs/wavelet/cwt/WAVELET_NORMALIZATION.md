# Wavelet Normalization Documentation

This document explains the normalization conventions used in VectorWave and how they compare to other implementations.

## Overview

Different wavelet implementations use different normalization conventions. While the wavelet shapes are mathematically equivalent, the amplitude scaling can differ. This is important to understand when comparing results across different software packages.

## Mexican Hat (DOG2) Wavelet

### Mathematical Definition
The Mexican Hat wavelet is the second derivative of a Gaussian:
```
ψ(t) = C * (1 - t²) * exp(-t²/2)
```

### Normalization Conventions

1. **MATLAB mexihat**: Uses C = 2/(√3 * π^(1/4)) ≈ 0.8673
2. **PyWavelets**: Uses a different normalization factor
3. **VectorWave**: We match MATLAB's convention for compatibility

### Verification
At t=0: ψ(0) = 0.8673250706 (MATLAB value)

## Paul Wavelet

### Mathematical Definition
```
ψ(t) = (2^m * i^m * m!) / √(π(2m)!) * (1 - it)^(-(m+1))
```

### Complex Number Handling
The Paul wavelet requires careful handling of complex arithmetic:
- (1 - it)^(-(m+1)) must be computed using proper complex exponentiation
- The phase calculation affects the real and imaginary parts

### Normalization Conventions
1. **PyWavelets paul-4**: ψ(0) ≈ 0.7518
2. **VectorWave**: We apply a correction factor to match PyWavelets

## Shannon Wavelet

### Classical vs Shannon-Gabor
There are two common formulations:

1. **Classical Shannon**: ψ(t) = 2*sinc(2t) - sinc(t)
2. **Shannon-Gabor**: ψ(t) = √fb * sinc(fb*t) * exp(2πi*fc*t)

VectorWave provides both:
- `ClassicalShannonWavelet`: Matches the standard definition
- `ShannonWavelet`: Shannon-Gabor formulation with configurable parameters

## Recommendations

1. **When comparing results**: Always check the normalization convention used
2. **For compatibility**: Use the wavelet that matches your reference implementation
3. **For new projects**: Choose based on your specific requirements

## Testing Against Reference Implementations

The `WaveletReferenceTest` class verifies our implementations against:
- MATLAB R2023b Wavelet Toolbox
- PyWavelets 1.4.1
- Published literature values

Due to normalization differences, we use a relative tolerance of 15% for cross-implementation comparisons.