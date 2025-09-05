# VectorWave Core

Core wavelet transform implementations for Java 21+ without Vector API dependencies.

## Overview

The `vectorwave-core` module provides the foundational wavelet transform algorithms and base functionality. It includes optimized scalar implementations that work on any Java 21+ platform, with automatic enhancement when paired with the `vectorwave-extensions` module for SIMD acceleration.

## Features

- **MODWT (Maximal Overlap Discrete Wavelet Transform)**
  - Arbitrary signal length support
  - Shift-invariant processing
  - Perfect reconstruction
  
- **SWT (Stationary Wavelet Transform) Adapter**
  - Familiar SWT API leveraging MODWT backend
  - Full denoising capabilities
  
- **CWT (Continuous Wavelet Transform)**
  - FFT-accelerated implementation
  - Complex wavelet analysis
  - Automatic scale selection

- **Financial Analysis**
  - Wavelet-based Sharpe ratio
  - Risk metrics
  - Market indicators

## Installation

```xml
<dependency>
    <groupId>com.morphiqlabs</groupId>
    <artifactId>vectorwave-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Quick Start

```java
import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.modwt.*;

// Get a wavelet
Wavelet db4 = WaveletRegistry.getWavelet(WaveletName.DB4);

// Create transform
MODWTTransform transform = new MODWTTransform(db4, BoundaryMode.PERIODIC);

// Process signal
double[] signal = new double[1000]; // Any length!
MODWTResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result);
```

## Performance

While this module provides scalar implementations, it includes:
- Efficient algorithms optimized for cache locality
- Parallel processing using ExecutorService
- Memory-efficient streaming capabilities

For SIMD acceleration, add the `vectorwave-extensions` module as a runtime dependency.

## Requirements

- Java 21 or higher
- Maven 3.6+
- No runtime dependencies (zero-dependency library)


## Documentation

- [Main Documentation](../docs/README.md)
- [API Reference](../docs/API.md)
- [Architecture Overview](../docs/ARCHITECTURE.md)
- [Developer Guide](../docs/guides/DEVELOPER_GUIDE.md)
- [Wavelet Selection Guide](../docs/WAVELET_SELECTION.md)

## Key Classes

| Class | Description |
|-------|-------------|
| `MODWTTransform` | Primary wavelet transform implementation |
| `VectorWaveSwtAdapter` | SWT interface with mutable coefficients |
| `CWTTransform` | Continuous wavelet transform |
| `WaveletRegistry` | Central registry for all wavelets |
| `FinancialWaveletAnalyzer` | Financial analysis utilities |

## License

GPL-3.0 - See [LICENSE](../LICENSE) file for details.
