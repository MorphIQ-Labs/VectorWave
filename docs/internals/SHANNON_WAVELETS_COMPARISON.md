# Shannon Wavelets: Classical vs Shannon-Gabor for Financial Analysis

## Overview

VectorWave provides two Shannon wavelet variants, each optimized for different financial analysis scenarios:

1. **Classical Shannon Wavelet** (`ClassicalShannonWavelet`)
2. **Shannon-Gabor Wavelet** (`ShannonGaborWavelet`)

## Mathematical Definitions

### Classical Shannon Wavelet
```
ψ(t) = 2*sinc(2t) - sinc(t)
where sinc(x) = sin(πx)/(πx)
```

- **Frequency support**: [π/2, π]
- **Time decay**: 1/t (slow decay)
- **Key property**: Perfect frequency localization

### Shannon-Gabor Wavelet
```
ψ(t) = √fb * sinc(fb*t) * cos(2π*fc*t)
```

- **fb**: Bandwidth parameter (controls frequency resolution)
- **fc**: Center frequency parameter (controls oscillation rate)
- **Key property**: Balanced time-frequency localization

## Comparison Table

| Property | Classical Shannon | Shannon-Gabor |
|----------|------------------|---------------|
| **Frequency Resolution** | Perfect | Very Good |
| **Time Resolution** | Poor | Good |
| **Gibbs Phenomenon** | Significant | Minimal |
| **Parameter Control** | None | fb, fc adjustable |
| **Computational Cost** | Low | Moderate |
| **Best For** | Stationary signals | Non-stationary signals |

## Financial Use Cases

### Classical Shannon Wavelet

**When to Use:**
- Analyzing **fixed trading cycles** (weekly, monthly patterns)
- **Fourier-like analysis** where frequency precision is critical
- **Long-term trend decomposition** with stable periods
- **Backtesting strategies** based on fixed-frequency components

**Example Applications:**
```java
// Detecting weekly trading patterns
ClassicalShannonWavelet shannon = new ClassicalShannonWavelet();
CWTTransform transform = new CWTTransform(shannon);

// Analyze for 5-day trading cycle
double tradingDaysPerYear = 252;
double weeklyFrequency = tradingDaysPerYear / 5; // ~50 cycles/year
```

**Limitations:**
- Poor localization of market events (crashes, news impacts)
- Ringing artifacts around volatility spikes
- Not suitable for regime change detection

### Shannon-Gabor Wavelet

**When to Use:**
- **Intraday analysis** with changing market conditions
- **Event detection** (earnings releases, economic data)
- **Volatility clustering** analysis
- **High-frequency trading** pattern recognition
- **Market microstructure** studies

**Example Applications:**
```java
// Intraday volatility analysis with custom parameters
double bandwidth = 0.5;        // Moderate frequency resolution
double centerFreq = 1.5;       // Focus on rapid changes
ShannonGaborWavelet shanGabor = new ShannonGaborWavelet(bandwidth, centerFreq);

// For HFT analysis - narrow band, high frequency
ShannonGaborWavelet hftWavelet = new ShannonGaborWavelet(0.2, 5.0);
```

**Advantages:**
- Smooth coefficient transitions
- Better time localization of events
- Reduced artifacts around discontinuities
- Adjustable time-frequency trade-off

## Practical Guidelines

### Choosing the Right Wavelet

1. **Market Regime Analysis**
   - Stable markets → Classical Shannon
   - Volatile/changing markets → Shannon-Gabor

2. **Time Horizon**
   - Long-term (months/years) → Classical Shannon
   - Short-term (days/hours) → Shannon-Gabor

3. **Signal Characteristics**
   - Periodic, stationary → Classical Shannon
   - Transient, non-stationary → Shannon-Gabor

### Parameter Selection for Shannon-Gabor

```java
// For different market conditions:

// 1. Broad market analysis (capture multiple frequencies)
ShannonGaborWavelet broad = new ShannonGaborWavelet(1.0, 1.0);

// 2. Focused frequency analysis (e.g., daily patterns)
ShannonGaborWavelet daily = new ShannonGaborWavelet(0.3, 2.0);

// 3. High-frequency microstructure
ShannonGaborWavelet micro = new ShannonGaborWavelet(0.1, 10.0);

// 4. Volatility regime detection
ShannonGaborWavelet regime = new ShannonGaborWavelet(0.5, 0.5);
```

## Performance Considerations

### Classical Shannon
- **Pros**: Fast computation, minimal parameters
- **Cons**: May require post-processing to remove artifacts

### Shannon-Gabor
- **Pros**: Cleaner results, flexible analysis
- **Cons**: Slightly higher computational cost, parameter tuning needed

## Example: Comparing Both Wavelets

```java
public class ShannonComparison {
    public static void compareWavelets(double[] priceData) {
        // Classical Shannon - for cycle extraction
        ClassicalShannonWavelet classical = new ClassicalShannonWavelet();
        CWTTransform classicalTransform = new CWTTransform(classical);
        
        // Shannon-Gabor - for event detection
        ShannonGaborWavelet shanGabor = new ShannonGaborWavelet(0.5, 1.5);
        CWTTransform gaborTransform = new CWTTransform(shanGabor);
        
        // Same scales for comparison
        double[] scales = ScaleSpace.logarithmic(1, 100, 50).getScales();
        
        // Analyze
        CWTResult classicalResult = classicalTransform.analyze(priceData, scales);
        CWTResult gaborResult = gaborTransform.analyze(priceData, scales);
        
        // Classical: Better for identifying exact frequencies
        int[] dominantScales = findDominantScales(classicalResult);
        
        // Shannon-Gabor: Better for localizing when changes occur
        int[] eventLocations = findEventLocations(gaborResult);
    }
}
```

## Recommendations

1. **Start with Shannon-Gabor** for general financial analysis
   - More versatile and forgiving
   - Better for real-world noisy data

2. **Use Classical Shannon** when you specifically need:
   - Exact frequency measurements
   - Compatibility with Fourier-based methods
   - Analysis of known periodic components

3. **Consider using both** in a complementary fashion:
   - Classical for frequency identification
   - Shannon-Gabor for time localization

## References

1. Shannon, C.E. (1949). "Communication in the presence of noise"
2. Gabor, D. (1946). "Theory of communication"
3. Mallat, S. (2008). "A Wavelet Tour of Signal Processing"
4. Gençay, R., Selçuk, F., & Whitcher, B. (2001). "An Introduction to Wavelets and Other Filtering Methods in Finance and Economics"