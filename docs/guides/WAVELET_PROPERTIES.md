# Wavelet Mathematical Properties and Sources

This document provides detailed information about the mathematical properties, sources, and verification methods for all wavelets implemented in VectorWave.

## Table of Contents
- [Haar Wavelet](#haar-wavelet)
- [Daubechies Wavelets](#daubechies-wavelets)
- [Symlet Wavelets](#symlet-wavelets)
- [Coiflet Wavelets](#coiflet-wavelets)
- [Mathematical Verification](#mathematical-verification)

## Haar Wavelet

### History
The Haar wavelet was introduced by Alfréd Haar in 1909, making it the first wavelet ever described. It predates formal wavelet theory by many decades.

### Properties
- **Support**: Compact support of width 2
- **Vanishing moments**: 1
- **Symmetry**: Symmetric
- **Continuity**: Discontinuous (step function)
- **Orthogonal**: Yes

### Coefficients
```
h[0] = 1/√2 ≈ 0.7071067811865476
h[1] = 1/√2 ≈ 0.7071067811865476
```

### Sources
- Haar, A. (1910). "Zur Theorie der orthogonalen Funktionensysteme", Mathematische Annalen, 69, pp. 331-371.
- Mallat, S. (2008). "A Wavelet Tour of Signal Processing", 3rd edition, Academic Press, Section 7.2.

## Daubechies Wavelets

### History
Daubechies wavelets were developed by Ingrid Daubechies in 1988. They are constructed to have the maximum number of vanishing moments for a given filter length, making them optimal for representing polynomial signals.

### Properties (DB2)
- **Support**: Compact support of width 3
- **Vanishing moments**: 2
- **Filter length**: 4
- **Orthogonal**: Yes
- **Asymmetric**: Yes (except DB1/Haar)

### Properties (DB4)
- **Support**: Compact support of width 7
- **Vanishing moments**: 4
- **Filter length**: 8
- **Orthogonal**: Yes
- **Better frequency selectivity than DB2**

### Sources
- Daubechies, I. (1988). "Orthonormal bases of compactly supported wavelets", Communications on Pure and Applied Mathematics, 41(7), pp. 909-996.
- Daubechies, I. (1992). "Ten Lectures on Wavelets", CBMS-NSF Regional Conference Series in Applied Mathematics, vol. 61, SIAM, Philadelphia.
- Numerical values verified against MATLAB Wavelet Toolbox and PyWavelets.

### Mathematical Constraints
All Daubechies wavelets satisfy:
- Σh[n] = √2 (DC gain normalization)
- Σh[n]² = 1 (energy normalization)
- Σh[n]h[n+2k] = 0 for k ≠ 0 (orthogonality)
- Σn^p h[n] = 0 for p = 0, 1, ..., N-1 (vanishing moments)

## Symlet Wavelets

### History
Symlets were designed by Ingrid Daubechies to be as symmetric as possible while maintaining the same orthogonality and compact support properties as standard Daubechies wavelets. They minimize the phase nonlinearity of the transfer function.

### Properties
- **Near-symmetric**: Better symmetry than standard Daubechies
- **Same vanishing moments as corresponding Daubechies wavelets**
- **Phase**: Nearly linear phase response
- **Popular for signal denoising**

### Special Notes
- **SYM2 = DB2**: For N=2 vanishing moments with minimal support, there is only one solution
- **SYM3**: Filter length 6, 3 vanishing moments
- **SYM4**: Filter length 8, 4 vanishing moments, popular for denoising

### Sources
- Daubechies, I. (1992). "Ten Lectures on Wavelets", Chapter 8 (Symmetry for Compactly Supported Wavelet Bases).
- Percival, D.B. and Walden, A.T. (2000). "Wavelet Methods for Time Series Analysis", Cambridge University Press, Table 114.
- Numerical values verified against MATLAB Wavelet Toolbox (wfilters('sym2')) and PyWavelets.

## Coiflet Wavelets

### History
Coiflets were designed by Ingrid Daubechies at the request of Ronald Coifman. They have better symmetry properties than standard Daubechies wavelets and are unique in having vanishing moments for both the wavelet and scaling functions.

### Properties (COIF1)
- **Vanishing moments**: 2 (for both wavelet and scaling functions)
- **Filter length**: 6
- **Near-linear phase response**

### Properties (COIF2)
- **Vanishing moments**: 4 (for both wavelet and scaling functions)
- **Filter length**: 12
- **Better frequency selectivity than COIF1**
- **Note**: Coefficients have slightly lower precision (1e-4 tolerance)

### Properties (COIF3)
- **Vanishing moments**: 6 (for both wavelet and scaling functions)
- **Filter length**: 18
- **Higher computational cost but better approximation properties**

### Sources
- Daubechies, I. (1992). "Ten Lectures on Wavelets", Table 8.3, CBMS-NSF Regional Conference Series in Applied Mathematics, vol. 61, SIAM, Philadelphia.
- Strang, G. and Nguyen, T. (1996). "Wavelets and Filter Banks", Wellesley-Cambridge Press.
- Numerical values verified against MATLAB Wavelet Toolbox and PyWavelets.

## Mathematical Verification

All wavelets in VectorWave include a `verifyCoefficients()` method that validates:

### Basic Orthogonality Conditions
1. **Normalization**: Σh[n] = √2
2. **Energy**: Σh[n]² = 1
3. **Orthogonality**: Σh[n]h[n+2k] = 0 for k ≠ 0

### Wavelet-Specific Checks
- **Daubechies**: Vanishing moment conditions
- **Symlets**: Near-symmetry metric
- **Coiflets**: Dual vanishing moments (wavelet and scaling function)

### Verification Usage
```java
// Example: Verify Haar wavelet coefficients
Haar haar = new Haar();
boolean isValid = haar.verifyCoefficients();

// Example: Verify Daubechies DB4 coefficients
boolean isValidDB4 = Daubechies.DB4.verifyCoefficients();
```

### Tolerance Notes
- Most wavelets use tolerance of 1e-10 or better
- COIF2 requires relaxed tolerance (1e-4) due to coefficient precision
- Higher-order vanishing moments may require relaxed tolerances due to numerical accumulation

## Implementation Notes

### Coefficient Storage
- All coefficients are stored with maximum available precision
- Coefficients are validated during static initialization
- Defensive copying ensures immutability

### Filter Generation
- High-pass filters are generated from low-pass using Quadrature Mirror Filter (QMF) relationship
- For orthogonal wavelets: g[n] = (-1)^n h[L-1-n]
- Reconstruction filters equal decomposition filters for orthogonal wavelets

### Performance Considerations
- Coefficients are pre-computed and cached
- No runtime coefficient generation
- Thread-safe implementation through immutability