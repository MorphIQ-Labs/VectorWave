# VectorWave Architecture

## Overview

VectorWave is designed with a modular, layered architecture that separates core algorithms from performance optimizations, enabling platform-independent deployment with optional SIMD acceleration.

## Module Architecture

```
VectorWave/
├── vectorwave-core/          # Core algorithms (Java 21+)
│   ├── wavelet transforms
│   ├── financial analysis
│   └── scalar implementations
├── vectorwave-extensions/    # Performance optimizations
│   ├── Vector API (SIMD)
│   ├── structured concurrency
│   └── platform optimizations
├── vectorwave-fft/           # Standalone FFT utilities (CoreFFT)
└── vectorwave-examples/      # Demos (non-JMH) and interactive examples
    ├── usage examples
    ├── performance tests
    └── interactive demos
```

### Module Dependencies
- **Core**: Zero runtime dependencies, pure Java
- **Extensions**: Depends on core, provides optimizations via SPI
- **FFT**: Standalone; used by core
- **Examples**: Depends on core/extensions; demonstrates capabilities (no JMH)
 - **Benchmarks**: Non-modular `vectorwave-benchmarks` runs JMH on the classpath

## Core Components

### 1. Wavelet Type System

```
com.morphiqlabs.wavelet.api/
├── Wavelet (sealed interface)
├── DiscreteWavelet
│   ├── OrthogonalWavelet
│   └── BiorthogonalWavelet
└── ContinuousWavelet
```

**Key Features:**
- Sealed interfaces ensure compile-time type safety
- Each wavelet type defines its filter coefficients
- Automatic QMF (Quadrature Mirror Filter) generation for orthogonal wavelets

### 2. Transform Engine

**MODWTTransform**: Primary transform implementation
- Forward/inverse MODWT operations (shift-invariant)
- Works with signals of any length (no power-of-2 restriction)
- Boundary mode handling (periodic, symmetric, zero padding)
- Automatic optimization path selection
- Batch processing with SIMD acceleration

**MultiLevelMODWTTransform**: Multi-level decomposition
- Automatic maximum level calculation
- Mutable results for coefficient manipulation
- Energy distribution analysis
- Perfect reconstruction guarantee

**VectorWaveSwtAdapter**: SWT interface
- Familiar SWT API leveraging MODWT backend
- Mutable coefficient access for denoising
- Universal and custom thresholding
- Feature extraction and band isolation

**WaveletOperations**: Public facade for operations
- Automatic SIMD optimization
- Unified interface for denoising operations
- Platform capability detection

### 3. Optimization Layers

```
com.morphiqlabs.wavelet.internal/
├── ScalarOps         - Baseline implementation
├── VectorOps         - SIMD with platform detection
├── VectorOpsARM      - ARM-specific optimizations
├── CacheAwareOps     - Cache-optimized for large signals
└── SpecializedKernels - Common pattern optimizations
```

**Platform Adaptation:**
- Apple Silicon: Optimized for 128-bit NEON
- x86: AVX2/AVX512 support
- Automatic fallback to scalar operations

### 4. Streaming Architecture

**Flow API Integration:**
- Publisher/Subscriber pattern for real-time data
- Backpressure support
- Configurable block sizes
- Zero-copy ring buffer implementation

**Streaming Implementations:**
- **MODWTStreamingTransform**: Shift-invariant streaming with arbitrary block sizes
  - No power-of-2 restrictions on block size
  - True zero-copy using MODWTTransform.forward(double[], int, int)
  - Lock-free SPSC (Single Producer, Single Consumer) design
  - Configurable overlap support (0-100%)
- **MODWTStreamingDenoiser**: Real-time denoising with shift-invariance
  - Works with any buffer size
  - Adaptive noise estimation
  - < 1 µs/sample latency in fast mode

**Ring Buffer Design:**
- Lock-free atomic operations for thread safety
- Thread-local buffers for window extraction
- Configurable capacity multiplier for smooth operation
- Race condition prevention through atomic snapshots

### 5. Memory Management

**Object Pooling:**
- MODWTTransformPool: Reusable transform instances
- AlignedMemoryPool: SIMD-aligned allocations
- MemoryPool: General-purpose array pooling
- No padding required for MODWT (works with exact sizes)

## Design Principles

1. **Zero Allocation Hot Path**: Critical operations avoid allocations
2. **Platform Agnostic API**: Same code runs optimally on all platforms
3. **Fail-Fast Validation**: Early error detection with custom exceptions
4. **Extensibility**: Easy to add new wavelets and optimizations

## Data Flow

```
Signal Input (any length)
    ↓
Validation (null, NaN checks)
    ↓
Optimization Path Selection
    ├── Scalar (small signals)
    ├── SIMD (medium signals)
    └── Cache-aware (large signals)
    ↓
Boundary Handling
    ↓
Circular Convolution (MODWT)
    ↓
Multi-level Decomposition (optional)
    ↓
MODWTResult (same length as input)
```

## Thread Safety

- Transform instances are thread-safe for concurrent use
- Streaming components use atomic operations
- Memory pools use lock-free data structures

## Extension Points

1. **New Wavelets**: 
   - Implement appropriate wavelet interface (OrthogonalWavelet, BiorthogonalWavelet, or ContinuousWavelet)
   - Create a WaveletProvider implementation
   - Register via ServiceLoader in META-INF/services
2. **Custom Padding**: Implement PaddingStrategy interface
3. **Optimization Paths**: Extend VectorOps with platform-specific code
4. **Threshold Methods**: Add to ThresholdCalculator

## ServiceLoader Architecture

### Wavelet Discovery

VectorWave uses Java's ServiceLoader mechanism for automatic wavelet discovery, eliminating static initialization blocks and circular dependencies.

```
com.morphiqlabs.wavelet.api.providers/
├── OrthogonalWaveletProvider    - Haar, Daubechies, Symlets, Coiflets
├── BiorthogonalWaveletProvider  - Biorthogonal spline wavelets
└── (custom providers)           - Third-party wavelets

com.morphiqlabs.wavelet.cwt.providers/
├── ContinuousWaveletProvider    - Morlet, Gaussian derivatives
└── (custom providers)           - Third-party CWT wavelets

com.morphiqlabs.wavelet.cwt.finance.providers/
└── FinancialWaveletProvider     - Paul, DOG, Shannon variants
```

### Benefits

1. **No Circular Dependencies**: Providers are loaded lazily on first access
2. **Plugin Architecture**: Add wavelets without modifying core library
3. **Clean Separation**: Each wavelet family has its own provider
4. **Thread-Safe Loading**: Double-checked locking ensures safe initialization

### Registry Design

```java
WaveletRegistry (static facade)
    ↓
ServiceLoader.load(WaveletProvider.class)
    ↓
WaveletProvider implementations
    ↓
Individual Wavelet instances
```

The registry maintains:
- Thread-safe wavelet lookup by name (case-insensitive)
- Categorization by wavelet type
- Manual registration support for runtime additions
- Reload capability for dynamic scenarios
