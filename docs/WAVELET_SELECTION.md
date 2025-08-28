# Wavelet Selection Guide

This guide helps you choose the right wavelet for your specific application.

## Quick Selection Matrix

| Application | Recommended Wavelets | Key Considerations |
|-------------|---------------------|-------------------|
| **General Signal Analysis** | Morlet, DB4-DB8 | Balance between time and frequency resolution |
| **Financial Time Series** | Paul, Morlet, Mexican Hat | Asymmetric patterns, volatility clustering |
| **Image Processing** | DB2-DB4, Symlets, Bior | Compact support, symmetry |
| **Audio Processing** | Morlet, Shannon, DB8-DB16 | Frequency resolution, phase information |
| **Denoising** | DB4-DB8, Symlets, Coiflets | Smoothness, vanishing moments |
| **Edge Detection** | Mexican Hat (DOG2), DB2 | Zero crossings, sharp transitions |
| **Compression** | DB4-DB8, Bior | Energy compaction, perfect reconstruction |
| **Scientific Data** | Morlet, Paul, DOG | Physical interpretation, continuous analysis |
| **Smooth Signal Approximation** | BLEM3-BLEM4, Coiflets, DB8+ | High regularity, polynomial approximation |

## Continuous Wavelets (CWT)

### Morlet Wavelet
```java
MorletWavelet wavelet = new MorletWavelet(); // Default: ω₀=6, σ=1
MorletWavelet custom = new MorletWavelet(5.0, 1.5); // Custom parameters
```

**Best for:**
- General time-frequency analysis
- Musical signal analysis
- Geophysical data processing
- Neurological signal analysis (EEG, MEG)

**Characteristics:**
- Complex-valued (provides phase information)
- Gaussian envelope with sinusoidal modulation
- Excellent frequency localization
- No perfect time localization due to infinite support

**Parameters:**
- `ω₀` (omega0): Center frequency (typically 5-6)
  - Higher values → better frequency resolution
  - Lower values → better time resolution
- `σ` (sigma): Gaussian width (typically 1.0)

### Paul Wavelet
```java
PaulWavelet wavelet = new PaulWavelet(4); // Order 4 (recommended)
```

**Best for:**
- Financial market crash detection
- Asymmetric pattern recognition
- Sharp transient detection
- Directional price movements

**Characteristics:**
- Complex-valued
- Asymmetric shape
- Good for capturing one-sided features
- Order parameter controls smoothness

**Parameters:**
- `m` (order): Typically 1-20
  - m=4: Best balance for financial analysis
  - Higher m → smoother wavelet, better frequency localization

### Mexican Hat (DOG2)
```java
DOGWavelet dog2 = new DOGWavelet(2); // Second derivative
MATLABMexicanHat mexhat = new MATLABMexicanHat(); // MATLAB-compatible
```

**Best for:**
- Edge detection
- Volatility analysis
- Peak detection
- Zero-crossing analysis

**Characteristics:**
- Real-valued
- Second derivative of Gaussian
- Zero crossings at inflection points
- Symmetric shape

**MATLAB Compatibility:**
- Use `MATLABMexicanHat` for exact MATLAB reproduction
- Different normalization (σ = 5/√8 instead of 1)

### Derivative of Gaussian (DOG)
```java
DOGWavelet dog = new DOGWavelet(n); // n-th derivative
```

**Best for:**
- Smooth feature extraction
- Multi-scale edge detection
- Pattern matching at different smoothness levels

**Parameters:**
- `n` (order): Derivative order
  - n=1: First derivative (edge detection)
  - n=2: Mexican Hat (ridge detection)
  - n=3+: Smoother patterns

### Shannon Wavelet
```java
ShannonWavelet wavelet = new ShannonWavelet();
ShannonWavelet custom = new ShannonWavelet(0.5, 1.5); // Custom band
```

**Best for:**
- Band-limited signal analysis
- Theoretical studies
- Signals with known frequency bands
- Communication systems

**Characteristics:**
- Perfect frequency localization (rectangular in frequency)
- Poor time localization (sinc function in time)
- Ideal for strictly band-limited signals

## Discrete Wavelets (DWT)

### Daubechies Wavelets
```java
OrthogonalWavelet db4 = Daubechies.DB4;
OrthogonalWavelet db8 = Daubechies.DB8;
```

**Selection Guide:**
- **DB2-DB4**: Sharp features, edge detection, fast computation
- **DB4-DB8**: General purpose, good balance
- **DB10-DB20**: Smooth signals, high accuracy requirements

**Vanishing Moments:**
- DBN has N vanishing moments
- Removes polynomial trends up to degree N-1
- Higher N → better frequency localization, longer filters

### Symlets
```java
OrthogonalWavelet sym4 = Symlet.SYM4;
OrthogonalWavelet sym8 = Symlet.SYM8;
```

**Best for:**
- Symmetric/near-symmetric signals
- Image processing
- Reduced phase distortion requirements

**Characteristics:**
- Near-symmetric (least asymmetric)
- Same vanishing moments as Daubechies
- Better phase properties than Daubechies

### Coiflets
```java
OrthogonalWavelet coif2 = Coiflet.COIF2;
```

**Best for:**
- Numerical analysis
- Signals with polynomial components
- Applications requiring scaling function moments

**Characteristics:**
- Both wavelet and scaling function have vanishing moments
- More symmetric than Daubechies
- Longer support for same number of vanishing moments

### Biorthogonal Wavelets
```java
BiorthogonalWavelet bior = BiorthogonalSpline.BIOR2_2;
```

**Best for:**
- Image compression (JPEG2000 uses BIOR4_4)
- Perfect reconstruction with symmetric filters
- Applications where phase linearity matters

**Characteristics:**
- Symmetric (linear phase)
- Perfect reconstruction
- Different analysis and synthesis wavelets

### Battle-Lemarié Wavelets (B-Spline)
```java
BattleLemarieWavelet blem3 = BattleLemarieWavelet.BLEM3; // Cubic (recommended)
BattleLemarieWavelet blem2 = BattleLemarieWavelet.BLEM2; // Quadratic
```

**Best for:**
- Very smooth signal approximation
- Numerical analysis with spline methods
- Signals with polynomial-like behavior
- Applications requiring high regularity

**Characteristics:**
- Constructed from orthogonalized B-splines
- m-1 continuous derivatives for order m
- Excellent smoothness properties
- Exponential decay in time domain

**⚠️ Implementation Note:** Current implementation uses approximations with:
- Up to 6% reconstruction error (particularly BLEM3)
- Relaxed orthogonality conditions
- Good practical performance for smooth signals

**Selection Guide:**
- **BLEM1**: Linear splines, piecewise linear approximation
- **BLEM2**: Quadratic splines, C¹ continuity
- **BLEM3**: Cubic splines, C² continuity (most common)
- **BLEM4**: Quartic splines, C³ continuity
- **BLEM5**: Quintic splines, C⁴ continuity (may have normalization issues)

## Selection Criteria

### 1. Time-Frequency Resolution Trade-off
- **Need precise frequency?** → Morlet with high ω₀, Shannon
- **Need precise timing?** → Paul, DB2, Mexican Hat
- **Need balance?** → Morlet (ω₀=6), DB4-DB8

### 2. Signal Characteristics
- **Smooth signals** → Battle-Lemarié (BLEM3-4), Higher-order wavelets (DB8+, Coif)
- **Sharp transients** → Low-order wavelets (DB2-DB4, Paul)
- **Oscillatory** → Morlet, Shannon
- **Polynomial trends** → Battle-Lemarié, Coiflets, high-order Daubechies

### 3. Application Requirements
- **Real-time** → Orthogonal wavelets (Daubechies, Symlets)
- **Phase information needed** → Complex wavelets (Morlet, Paul)
- **Perfect reconstruction** → Any orthogonal or biorthogonal
- **Compression** → Wavelets with good energy compaction (DB4-DB8)

### 4. Computational Constraints
- **Fast computation** → Low-order wavelets, dyadic scales
- **Memory limited** → Compact support wavelets
- **FFT available** → CWT with any wavelet
- **Streaming** → Orthogonal wavelets with small support

## Financial Applications Special Considerations

### Market Crash Detection
```java
// Paul wavelet for asymmetric patterns
PaulWavelet paul = new PaulWavelet(4);
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer();
var crashes = analyzer.detectMarketCrashes(priceData, 0.05);
```

### Volatility Analysis
```java
// Mexican Hat for volatility clustering
DOGWavelet dog2 = new DOGWavelet(2);
CWTTransform cwt = new CWTTransform(dog2);
// Focus on scales corresponding to daily/weekly patterns
```

### Trend Analysis
```java
// Morlet for multi-scale trends
MorletWavelet morlet = new MorletWavelet(6.0, 1.0);
// Use logarithmic scales for multi-timeframe analysis
```

## Performance Tips

1. **For large signals (>10k samples)**:
   - Use FFT-accelerated CWT
   - Consider dyadic scales for efficiency
   - Use signal-adaptive scale selection

2. **For real-time processing**:
   - Use orthogonal wavelets (DWT)
   - Keep wavelet support small (DB4-DB8)
   - Pre-compute wavelet values if possible

3. **For highest accuracy**:
   - Use higher-order wavelets
   - Increase scale density
   - Consider complex wavelets for phase

## Common Pitfalls

1. **Using Shannon wavelet on non-band-limited signals**
   - Causes severe artifacts
   - Use Morlet instead for general signals

2. **Too few scales in CWT**
   - Misses important features
   - Use adaptive scale selection

3. **Wrong boundary handling**
   - Periodic for non-periodic signals causes artifacts
   - Use symmetric or zero-padding for most signals

4. **Ignoring phase information**
   - Missing important signal relationships
   - Use complex wavelets when phase matters

## Code Examples

### Automatic Wavelet Selection
```java
// For unknown signal characteristics
SignalAdaptiveScaleSelector selector = new SignalAdaptiveScaleSelector();
MorletWavelet wavelet = new MorletWavelet(); // Good general choice
double[] scales = selector.selectScales(signal, wavelet, samplingRate);
```

### Multi-Wavelet Analysis
```java
// Compare different wavelets
ContinuousWavelet[] wavelets = {
    new MorletWavelet(),
    new PaulWavelet(4),
    new DOGWavelet(2)
};

for (ContinuousWavelet w : wavelets) {
    CWTTransform cwt = new CWTTransform(w);
    CWTResult result = cwt.analyze(signal, scales);
    // Compare results...
}
```

### Optimal Parameter Selection
```java
// Grid search for best Morlet parameters
double bestOmega = 6.0;
double bestSigma = 1.0;
double minError = Double.MAX_VALUE;

for (double omega = 4.0; omega <= 8.0; omega += 0.5) {
    for (double sigma = 0.5; sigma <= 2.0; sigma += 0.25) {
        MorletWavelet wavelet = new MorletWavelet(omega, sigma);
        // Evaluate performance...
    }
}
```