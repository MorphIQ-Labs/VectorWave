# Vector API Fallback Mechanism

## Overview

VectorWave includes a sophisticated fallback mechanism that allows the library to run on any Java 21+ JVM, regardless of whether the Vector API incubator module is available. This ensures maximum portability while still providing optimal performance when Vector API is available.

## Architecture

### Runtime Detection

The Vector API availability is detected at class initialization time using reflection:

```java
static {
    boolean vectorAvailable = false;
    VectorSpecies<Double> speciesTemp = null;
    
    try {
        // Try to access Vector API
        speciesTemp = DoubleVector.SPECIES_PREFERRED;
        // Verify it works by creating a zero vector
        DoubleVector.zero(speciesTemp);
        vectorAvailable = true;
    } catch (Throwable e) {
        // Vector API not available or not functional
        // Use scalar fallback
    }
    
    VECTOR_API_AVAILABLE = vectorAvailable;
    SPECIES = speciesTemp;
}
```

### Fallback Strategy

1. **Compile-time**: The code includes both Vector API imports and implementations
2. **Runtime**: A static initializer attempts to load Vector API classes
3. **Graceful degradation**: If Vector API is unavailable, scalar implementations are used
4. **Transparent switching**: The same public API works with both implementations

### Implementation Pattern

```java
private static void fftRadix2Vector(double[] data, int n, boolean inverse) {
    if (!VECTOR_API_AVAILABLE) {
        fftRadix2Scalar(data, n, inverse);
        return;
    }
    
    try {
        // Vector implementation
        // ...
    } catch (Throwable e) {
        // Fall back to scalar implementation
        fftRadix2Scalar(data, n, inverse);
    }
}
```

## Affected Classes

### OptimizedFFT

The main FFT implementation includes:
- `fftRadix2Vector()` - Vectorized implementation with fallback
- `fftRadix2Scalar()` - Pure scalar implementation
- `isVectorApiAvailable()` - Public API to check Vector API status
- `getVectorApiInfo()` - Returns human-readable status information

### WaveletOpsFactory

Already included Vector API detection with:
- `checkVectorApiAvailable()` - Runtime detection method
- Automatic selection between scalar and vector implementations

## Benchmark Configuration

### JMH Benchmarks

All benchmarks have been updated to work with or without Vector API:
- Changed from `@Fork(jvmArgs = {...})` to `@Fork(jvmArgsAppend = {...})`
- Removed hard-coded `--add-modules=jdk.incubator.vector`

### jmh-runner.sh

The benchmark runner script automatically detects Vector API:

```bash
# Check if Vector API module is available
VECTOR_MODULE=""
if java --list-modules 2>/dev/null | grep -q "jdk.incubator.vector"; then
    VECTOR_MODULE="--add-modules=jdk.incubator.vector"
    echo "Vector API module detected and will be enabled"
else
    echo "Vector API module not available - benchmarks will use scalar fallback"
fi
```

## Performance Implications

### With Vector API
- 2-8x speedup on vectorizable operations
- Optimal performance on modern CPUs
- Platform-specific optimizations (AVX2/AVX512, ARM NEON)

### Without Vector API
- Falls back to optimized scalar code
- Still benefits from other optimizations (cache-aware, algorithmic)
- No functionality loss, only performance difference

## Testing

### Unit Tests

`VectorApiFallbackTest` provides comprehensive testing:
- Vector API availability detection
- Fallback behavior verification
- Performance consistency checks
- Edge case handling

### Running Tests

```bash
# With Vector API
mvn test

# Without Vector API (simulate)
mvn test -DargLine=""
```

## Usage

### Checking Vector API Status

```java
// Check if Vector API is available
boolean available = OptimizedFFT.isVectorApiAvailable();

// Get human-readable status
String info = OptimizedFFT.getVectorApiInfo();
// Returns: "Vector API available with vector length: 2" or
//          "Vector API not available - using scalar fallback"
```

### Configuration

No configuration needed - the library automatically detects and uses the best available implementation.

## Compatibility

- **Minimum Java Version**: Java 21
- **Vector API Support**: Java 16+ (as incubator module)
- **Platforms**: All Java platforms (x86, ARM, etc.)
- **No External Dependencies**: Pure Java implementation

## Future Considerations

As the Vector API graduates from incubator status:
1. The fallback mechanism will continue to work
2. Import statements may need updating for the final module name
3. The runtime detection will adapt to the stable API