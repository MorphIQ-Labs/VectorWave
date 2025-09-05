# Financial Analysis with VectorWave

VectorWave now includes financial analysis capabilities that leverage wavelet transforms for advanced signal processing in financial data analysis.

## Features

### Sharpe Ratio Calculation
- **Configurable Risk-Free Rate**: Users must explicitly set appropriate risk-free rates
- **Multiple Calculation Methods**: Standard and wavelet-denoised Sharpe ratio calculations
- **Robust Input Validation**: Comprehensive error handling and edge case management

### Wavelet-Based Denoising
- **Signal Noise Reduction**: Remove high-frequency noise from financial time series
- **Improved Risk Metrics**: Calculate more stable risk-adjusted returns using denoised data

## Quick Start

### Basic Usage

```java
import com.morphiqlabs.financial.*;

// Create analyzer with explicit risk-free rate (required - no defaults)
FinancialConfig config = new FinancialConfig(0.045); // 4.5% annual
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config);

// Calculate Sharpe ratio for monthly returns
double[] monthlyReturns = {0.05, 0.02, -0.01, 0.08, 0.04, 0.06, -0.02, 0.09};
double sharpeRatio = analyzer.calculateSharpeRatio(monthlyReturns);
```

### Different Risk-Free Rates

```java
// Use different risk-free rates for different scenarios
// Short-term analysis with T-Bill rate
FinancialConfig shortTermConfig = new FinancialConfig(0.025); // 2.5%
FinancialWaveletAnalyzer shortTermAnalyzer = new FinancialWaveletAnalyzer(shortTermConfig);

// Long-term analysis with 10-year Treasury rate
FinancialConfig longTermConfig = new FinancialConfig(0.045); // 4.5%
FinancialWaveletAnalyzer longTermAnalyzer = new FinancialWaveletAnalyzer(longTermConfig);

// International markets with different rates
FinancialConfig euroConfig = new FinancialConfig(0.03); // 3% ECB rate
FinancialWaveletAnalyzer euroAnalyzer = new FinancialWaveletAnalyzer(euroConfig);
```

### Wavelet-Based Analysis

⚠️ **WARNING**: The wavelet-based Sharpe ratio uses aggressive denoising that removes ALL high-frequency components. This may remove important market signals along with noise.

```java
// For power-of-2 length data, use wavelet denoising
double[] returns = {0.05, 0.02, -0.01, 0.08, 0.04, 0.06, -0.02, 0.09}; // Length: 8
double waveletSharpe = analyzer.calculateWaveletSharpeRatio(returns);

// Note: This method zeros all detail coefficients, which:
// - Removes market microstructure noise (good)
// - Also removes volatility spikes and rapid movements (potentially bad)
// - May underestimate risk for risk management purposes
// - Is inappropriate for high-frequency trading strategies
```

**When to use wavelet-based Sharpe ratio:**
- ✅ Long-term investment analysis where smoothing is desired
- ✅ Comparing strategies where noise reduction improves clarity
- ❌ Risk management (may hide important volatility)
- ❌ High-frequency or short-term trading
- ❌ Any application requiring accurate volatility measurement

**Alternative**: For more sophisticated denoising with configurable thresholds, use the `WaveletDenoiser` class which supports soft/hard thresholding.

### Advanced Configuration

```java
// Use custom wavelet for analysis
WaveletTransform transform = new WaveletTransformFactory()
    .withBoundaryMode(BoundaryMode.PERIODIC)
    .create(Daubechies.DB4);

FinancialConfig config = new FinancialConfig(0.025); // Explicit rate required
FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer(config, transform);
```

## API Reference

### FinancialConfig

**Constructor**
- `FinancialConfig(double riskFreeRate)` - Specify risk-free rate (annual decimal) - REQUIRED

**Methods**
- `getRiskFreeRate()` - Returns the configured risk-free rate

### FinancialWaveletAnalyzer

**Constructors**
- `FinancialWaveletAnalyzer(FinancialConfig config)` - Custom config with Haar wavelet
- `FinancialWaveletAnalyzer(FinancialConfig config, WaveletTransform transform)` - Full customization

**Sharpe Ratio Methods**
- `calculateSharpeRatio(double[] returns)` - Uses configured risk-free rate
- `calculateSharpeRatio(double[] returns, double riskFreeRate)` - Custom risk-free rate
- `calculateWaveletSharpeRatio(double[] returns)` - Wavelet-denoised, configured rate
- `calculateWaveletSharpeRatio(double[] returns, double riskFreeRate)` - Wavelet-denoised, custom rate

## Important: No Default Values

As of version 2.0, VectorWave requires explicit configuration of all financial parameters:

| Parameter | Required | Description |
|-----------|----------|-------------|
| Risk-Free Rate | YES | Must be explicitly set based on current market conditions |

### Why No Defaults?

Risk-free rates vary significantly:
- **By Time Period**: 2020-2021 near 0%, 2023-2024 around 4-5%
- **By Geography**: US, EU, Japan all have different rates
- **By Duration**: 3-month T-Bills vs 10-year Treasury Notes
- **By Application**: Corporate finance vs government analysis

Users must choose appropriate rates for their specific:
- Market (US, EU, Asia, etc.)
- Time period (current rates, historical analysis)
- Asset class (equities, bonds, commodities)
- Analysis duration (short-term vs long-term)

## Input Requirements

### Return Data Format
- **Data Type**: `double[]` array
- **Values**: Decimal format (e.g., 0.05 for 5% return)
- **Minimum Length**: 2 data points (for standard deviation calculation)
- **Wavelet Analysis**: Power-of-2 length required (2, 4, 8, 16, 32, ...)

### Risk-Free Rate Format
- **Data Type**: `double`
- **Range**: Non-negative values (≥ 0.0)
- **Format**: Annual decimal (e.g., 0.045 for 4.5%)

## Error Handling

The library provides comprehensive error handling:

```java
// Invalid inputs throw IllegalArgumentException
analyzer.calculateSharpeRatio(null);           // → IllegalArgumentException
analyzer.calculateSharpeRatio(new double[0]);  // → IllegalArgumentException
analyzer.calculateSharpeRatio(new double[]{0.05}); // → IllegalArgumentException

// Wavelet analysis requires power-of-2 length
double[] invalidLength = {0.05, 0.02, 0.08}; // Length 3
analyzer.calculateWaveletSharpeRatio(invalidLength); // → IllegalArgumentException
```

## Mathematical Notes

### Sharpe Ratio Formula
```
Sharpe Ratio = (Mean Return - Risk-Free Rate) / Standard Deviation
```

### Special Cases
- **Zero Standard Deviation**: Returns ±∞ based on excess return sign
- **Zero Excess Return with Zero Std Dev**: Returns 0.0
- **Negative Excess Return**: Returns negative Sharpe ratio

### Wavelet Denoising

The current implementation uses a **simple but aggressive** denoising approach:

1. Applies forward wavelet transform to decompose the signal
2. **Completely zeros ALL detail coefficients** (high-frequency components)
3. Reconstructs using only approximation (low-frequency) coefficients
4. Calculates Sharpe ratio on the denoised signal

**Mathematical Impact:**
- Original signal: `f(t) = A(t) + D(t)` where A = approximation, D = details
- Denoised signal: `f'(t) = A(t)` (all D components removed)
- This is equivalent to applying an aggressive low-pass filter

**Limitations of this approach:**
- No distinction between noise and signal in high frequencies
- All rapid changes are removed, including legitimate market events
- Variance is significantly reduced, potentially understating risk
- The resulting Sharpe ratio may be artificially inflated

**Better alternatives:**
- Soft thresholding: `D'(i) = sign(D(i)) * max(|D(i)| - λ, 0)`
- Hard thresholding: `D'(i) = D(i) if |D(i)| > λ, else 0`
- Level-dependent thresholding with different λ for each scale
- Use the `WaveletDenoiser` class for these more sophisticated approaches

## Running the Demo

```bash
# Compile the project
mvn compile

# Run the financial analysis demo
java -cp target/classes com.morphiqlabs.demo.FinancialDemo
```

The demo showcases:
- Standard configuration usage with explicit rates
- Multiple risk-free rate scenarios
- Comparison between regular and wavelet-based Sharpe ratios
## SIMD Integration (Extensions)

SIMD acceleration using the Java Vector API is available via the optional `vectorwave-extensions` module (Java 24 + incubator). Core remains scalar and portable. Enable SIMD by adding the extensions dependency and running with:

```
--add-modules jdk.incubator.vector --enable-preview
```
