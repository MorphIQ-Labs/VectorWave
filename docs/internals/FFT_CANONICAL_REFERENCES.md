# FFT Implementation - Canonical References

This document provides the canonical references for all FFT algorithms implemented in VectorWave.

## Core FFT Algorithm

### Cooley-Tukey FFT (Implemented)

The implementation in `FFTAcceleratedCWT.java` follows the classic Cooley-Tukey radix-2 decimation-in-time (DIT) algorithm.

**Canonical References:**
- Cooley, J. W., & Tukey, J. W. (1965). "An algorithm for the machine calculation of complex Fourier series." Mathematics of computation, 19(90), 297-301.
- Oppenheim, A. V., & Schafer, R. W. (2009). "Discrete-Time Signal Processing" (3rd ed., Chapter 9). Pearson.

**Algorithm verification against canonical source:**
```
X[k] = Σ(n=0 to N-1) x[n] * exp(-j*2π*k*n/N)
```

Our implementation uses the standard butterfly operations:
- Even/odd decomposition
- Twiddle factor multiplication: W_N^k = exp(-j*2π*k/N)
- Bit-reversal permutation for in-place computation

### Linear Convolution via FFT

To avoid circular convolution artifacts, we implement proper zero-padding as described in:

**Canonical References:**
- Oppenheim & Schafer (2009), Section 8.7: "Linear Convolution using the DFT"
- Proakis, J. G., & Manolakis, D. G. (2006). "Digital Signal Processing" (4th ed., pp. 430-437).

**Implementation details:**
- FFT size = signal_length + wavelet_support - 1
- Zero-padding to next power of 2
- Proper scaling in inverse FFT

## Real-to-Complex FFT (Future Enhancement)

The optimized real-to-complex FFT will follow:

**Canonical Reference:**
- Sorensen, H. V., Jones, D. L., Heideman, M. T., & Burrus, C. S. (1987). "Real-valued fast Fourier transform algorithms." IEEE Transactions on Acoustics, Speech, and Signal Processing, 35(6), 849-863.

**Key optimization:**
- Exploit Hermitian symmetry: X[k] = X*[N-k] for real signals
- Compute only N/2+1 complex values
- ~2x speedup over complex FFT

## Wavelet Transform via FFT

The FFT-accelerated CWT implementation follows:

**Canonical References:**
- Torrence, C., & Compo, G. P. (1998). "A practical guide to wavelet analysis." Bulletin of the American Meteorological society, 79(1), 61-78.
- Mallat, S. (2008). "A Wavelet Tour of Signal Processing" (3rd ed., Chapter 4.3).

**Implementation formula:**
```
CWT(a,b) = FFT^(-1)[FFT(signal) * conj(FFT(wavelet_a))]
```
where wavelet_a is the scaled wavelet at scale a.

## Verification Methodology

All algorithms have been verified against:
1. Mathematical definitions in canonical sources
2. Reference implementations (when available)
3. Numerical accuracy tests
4. Edge case handling

## Testing Against Canonical Sources

Our test suite (`RealFFTOptimizationTest.java`, `CWTLinearConvolutionTest.java`) verifies:
- Parseval's theorem: Σ|x[n]|² = (1/N)Σ|X[k]|²
- Convolution theorem: FFT(conv(x,y)) = FFT(x) * FFT(y)
- Hermitian symmetry for real signals
- Linear vs circular convolution behavior