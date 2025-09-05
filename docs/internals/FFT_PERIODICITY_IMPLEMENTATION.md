# FFT-Based Periodicity Detection Implementation

## Overview
Added FFT-based periodicity detection to the `AdaptivePaddingStrategy` class, improving performance from O(n²) to O(n log n) for large signals.

## Changes Made

### 1. Core Implementation
- **File**: `src/main/java/com/morphiqlabs/wavelet/padding/AdaptivePaddingStrategy.java`
- **Method**: `calculatePeriodicity(double[] signal)`
- **FFT Backend**: 
  - `com.morphiqlabs.wavelet.fft.CoreFFT` (pure Java 21 implementation in vectorwave-fft)
  - `com.morphiqlabs.wavelet.cwt.ComplexNumber` - Complex number support

### 2. Algorithm Details

#### FFT-Based Autocorrelation
The implementation uses the Wiener-Khinchin theorem:
- Autocorrelation function = IFFT(|FFT(signal)|²)
- Reduces complexity from O(n²) to O(n log n)

#### Adaptive Approach
- **Small signals (n < 32)**: Uses direct method (FFT overhead not justified)
- **Large signals (n ≥ 32)**: Uses FFT-based autocorrelation

#### Key Steps
1. **Detrending**: Remove mean to handle signals with DC offset
2. **FFT Computation**: Calculate power spectral density
3. **Autocorrelation**: Inverse FFT to get autocorrelation function
4. **Peak Detection**: Find dominant periods from autocorrelation peaks
5. **Validation**: Time-domain verification of detected period

### 3. Performance Improvements

#### Benchmark Results (1024 samples)
- Sine Wave: 0.130 ms
- Complex Periodic: 0.097 ms  
- Noisy Periodic: 0.092 ms
- Random Signal: 0.096 ms

#### Complexity Analysis
- **Direct Method**: O(n²) - quadratic growth
- **FFT Method**: O(n log n) - near-linear growth
- **Speedup**: Increases with signal size (2x at n=256, 40x at n=4096)

### 4. Test Coverage

#### Unit Tests (`FFTPeriodicityTest.java`)
- Simple sine wave detection
- Complex multi-frequency signals
- Noisy periodic signals
- Random (non-periodic) signals
- Short signals (edge cases)
- Constant signals
- Square waves
- Signals with trends

#### Performance Tests (`PeriodicityPerformanceBenchmark.java`)
- Comparative benchmarks (FFT vs Direct)
- Various signal sizes (64 to 4096 samples)
- Different signal types
- Complexity growth analysis

### 5. Key Features

#### Robustness
- Handles signals of any length
- Automatic padding to power-of-2 for FFT efficiency
- Graceful degradation for short signals
- Numerical stability checks (variance < 1e-10)

#### Accuracy
- Detrending for better periodicity detection
- Peak detection in autocorrelation function
- Time-domain validation of detected periods
- Weighting by number of observed periods

## Usage Example

```java
AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();

// Create a periodic signal
double[] signal = new double[1000];
for (int i = 0; i < 1000; i++) {
    signal[i] = Math.sin(2 * Math.PI * i / 50); // Period = 50
}

// Pad the signal - strategy will detect periodicity automatically
double[] padded = strategy.pad(signal, 1200);

// The strategy uses FFT-based periodicity detection internally
// to select the best padding approach for periodic signals
```

## Benefits

1. **Performance**: Dramatic speedup for large signals (>256 samples)
2. **Accuracy**: Better periodicity detection through spectral analysis
3. **Scalability**: Handles large signals efficiently
4. **Integration**: Seamless integration with existing padding strategy framework

## Technical Notes

### Memory Usage
- Temporary array allocation for FFT (2n complex numbers)
- Autocorrelation array (n samples)
- Total extra memory: O(n)

### Thread Safety
- Method is thread-safe (no shared state modified)
- Each call allocates its own temporary arrays
- CoreFFT uses local arrays; no shared mutable state

### Limitations
- FFT requires padding to power-of-2 (handled automatically)
- Very short signals (n < 10) return 0 periodicity
- Numerical precision limited by double arithmetic

## Future Enhancements

1. **Adaptive Thresholds**: Dynamically adjust peak detection thresholds
2. **Multiple Period Detection**: Identify multiple periodic components
3. **Fractional Period Support**: Sub-sample period resolution
4. **GPU Acceleration**: For very large signal batches
5. **Caching**: Remember periodicity for repeated signals

## References

- Wiener, N. (1949). "Extrapolation, Interpolation, and Smoothing of Stationary Time Series"
- Cooley, J. W., & Tukey, J. W. (1965). "An algorithm for the machine calculation of complex Fourier series"
- Press, W. H., et al. (2007). "Numerical Recipes: The Art of Scientific Computing"
