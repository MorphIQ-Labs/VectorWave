# VectorWave Developer Guide

## Overview

This guide provides essential information for developers working with or contributing to VectorWave.

## Build Requirements

- **Java 24 or later** (must include jdk.incubator.vector module)
- **Maven 3.6+**
- **Git** for version control

### Java Version Notes

- **Compilation**: Requires Java 24+ with Vector API module
- **Runtime**: Vector API is optional (graceful fallback to scalar implementation)
- **Structured Concurrency**: Uses preview features (requires `--enable-preview`)

## Building the Project

```bash
# Clean build
mvn clean compile

# Run tests
mvn test

# Build with all tests
mvn clean install

# Run specific test
mvn test -Dtest=MODWTTransformTest

# Generate documentation
mvn javadoc:javadoc
```

## Code Style and Conventions

### General Guidelines

1. **Follow existing patterns**: When modifying code, match the existing style
2. **Use existing libraries**: Check if functionality exists before adding dependencies
3. **Prefer composition over inheritance**: Use interfaces and composition
4. **Immutable objects**: Prefer immutable data structures where possible

### Suppressing Warnings

When using try-with-resources with auto-closeable resources that may throw `InterruptedException`:

```java
@Test
@SuppressWarnings("try")  // close() may throw InterruptedException
void testMethod() throws Exception {
    try (StreamingResource resource = new StreamingResource()) {
        // Test code
    }
}
```

For explicit close() calls within try-with-resources:

```java
@SuppressWarnings({"resource", "try"})  // Explicit close needed, may throw InterruptedException
```

## Testing Guidelines

### Test Structure

```java
@Test
void testFeatureName() {
    // Given - setup test data
    double[] signal = {1, 2, 3, 4, 5, 6, 7}; // MODWT works with any length!
    MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
    
    // When - execute operation
    MODWTResult result = transform.forward(signal);
    
    // Then - verify results
    assertNotNull(result);
    assertEquals(signal.length, result.approximationCoeffs().length);
    assertEquals(signal.length, result.detailCoeffs().length);
}
```

### Performance Tests

- Use `@Disabled` annotation for performance tests
- Include warmup iterations
- Report statistics (min, max, average, standard deviation)

## Common Tasks

### Adding a New Wavelet

1. Implement the appropriate interface (`OrthogonalWavelet`, `BiorthogonalWavelet`, or `ContinuousWavelet`)
2. Register in `WaveletRegistry`
3. Add comprehensive tests
4. Update documentation

### Optimizing Performance

1. **Check Vector API availability**: Use `VectorOps.isVectorApiSupported()`
2. **Validate signal lengths**: Use power-of-2 for best performance
3. **Use memory pools**: For repeated operations
4. **Profile first**: Use JMH benchmarks before optimizing

## Biorthogonal Wavelets

### Phase Compensation

Biorthogonal wavelets include automatic phase compensation to correct for inherent circular shifts:

- Perfect reconstruction for simple signals (constant, sequential)
- Small reconstruction errors for complex signals (normal behavior)
- Best results with PERIODIC boundary mode

### Expected Behavior

```java
// Perfect reconstruction
double[] constant = {1, 1, 1, 1};  // RMSE = 0

// Small errors expected
double[] random = generateRandom();  // RMSE ≈ 1.2
double[] sine = generateSine();      // RMSE ≈ 0.2
```

## Memory Management

### Memory Pool Usage

```java
MemoryPool pool = new MemoryPool();
pool.setMaxArraysPerSize(10);

try {
    // Borrow arrays for MODWT operations
    double[] signal = pool.borrowArray(1777); // Any size!
    MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
    MODWTResult result = transform.forward(signal);
    // Operations...
} finally {
    pool.returnArray(signal);
}
```

### Thread-Local Cleanup

In server applications or thread pools:

```java
try {
    BatchSIMDTransform.haarBatchTransformSIMD(signals, approx, detail);
} finally {
    BatchSIMDTransform.cleanupThreadLocals();
}
```

## Debugging Tips

### Performance Issues

1. Check Vector API is enabled: `--add-modules jdk.incubator.vector`
2. Verify signal length is power-of-2
3. Use appropriate wavelet for signal type
4. Enable JVM logging: `-Xlog:compilation`

### Accuracy Issues

1. Check boundary mode compatibility
2. Verify coefficient normalization
3. Test with known signals first
4. Compare with reference implementations

## Contributing

### Pull Request Process

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Write tests for new functionality
4. Ensure all tests pass: `mvn test`
5. Update documentation
6. Submit pull request with clear description

### Commit Messages

Follow conventional commits:
- `feat:` New feature
- `fix:` Bug fix
- `docs:` Documentation changes
- `test:` Test additions/modifications
- `perf:` Performance improvements
- `refactor:` Code refactoring

## Resources

- [API Documentation](../API.md)
- [Architecture Overview](../ARCHITECTURE.md)
- [Performance Guide](../performance/PERFORMANCE_SUMMARY.md)
- [FFM API Guide](../FFM_API.md)
## SIMD Integration (Extensions)

SIMD/Vector API acceleration is part of the optional `vectorwave-extensions` module (Java 24 + incubator). The core module targets Java 21 scalar portability. To develop with SIMD:

```
mvn -q -pl vectorwave-extensions -am test \
  -DargLine="--add-modules jdk.incubator.vector --enable-preview"
```
