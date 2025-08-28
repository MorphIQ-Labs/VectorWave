# Wavelet Transform Testing Framework

This package provides a comprehensive testing framework for the VectorWave wavelet transform library.

## Key Features

- **Thread Safety**: Concurrent testing with proper thread isolation using AtomicInteger
- **Streaming Tests**: Comprehensive streaming wavelet transform validation
- **Performance**: Benchmarks showing 107-294 ns latency for 64-sample signals
- **SIMD Testing**: Support for testing both scalar and vector implementations

## Overview

The testing framework consists of three main components:

### 1. WaveletTestUtils
Utility class providing common test helpers:
- **Signal Generation**: Create test signals with known properties (sine waves, step functions, random signals)
- **Mathematical Verification**: Verify wavelet properties like energy preservation, orthogonality, and normalization
- **Comparison Methods**: Compare arrays with tolerance, compute RMSE, find maximum differences
- **Utility Methods**: Compute signal energy, create simple test patterns

### 2. WaveletAssertions
Custom assertion methods for wavelet-specific testing:
- **Array Assertions**: Compare arrays with tolerance
- **Transform Assertions**: Verify energy preservation, perfect reconstruction
- **Filter Assertions**: Check normalization, orthogonality, vanishing moments
- **Validation Assertions**: Ensure transform results are valid and finite

### 3. BaseWaveletTest
Base class that other test classes can extend:
- **Common Setup**: Initializes standard test signals and transform factory
- **Transform Creation**: Helper methods to create transforms with different configurations
- **Test Patterns**: Support for testing both scalar and vector implementations
- **Signal Generation**: Convenient methods for generating various signal types

## Usage Examples

### Basic Test Class
```java
public class MyWaveletTest extends BaseWaveletTest {
    
    @Test
    void testPerfectReconstruction() {
        // Use pre-initialized test signal
        WaveletTransform transform = createTransform(new Haar());
        
        // Test perfect reconstruction
        assertTrue(WaveletTestUtils.verifyPerfectReconstruction(
            transform, smallTestSignal, WaveletTestUtils.DEFAULT_TOLERANCE));
    }
}
```

### Using Custom Assertions
```java
@Test
void testEnergyPreservation() {
    double[] signal = WaveletTestUtils.generateSineWave(64, 0.1, 1.0, 0.0);
    WaveletTransform transform = createTransform(new Daubechies.DB4());
    
    TransformResult result = transform.forward(signal);
    
    // Use custom assertion
    WaveletAssertions.assertEnergyPreserved(
        signal, result, WaveletTestUtils.ENERGY_TOLERANCE);
}
```

### Testing Both Implementations
```java
@Test
void testBothImplementations() {
    testBothImplementations(new Haar(), (transform, implType) -> {
        double[] signal = generateTestSignal(SignalType.SINE, 128);
        TransformResult result = transform.forward(signal);
        
        WaveletAssertions.assertValidTransformResult(result);
        // Test runs for both scalar and vector implementations
    });
}
```

## Test Coverage

To run tests with coverage:
```bash
mvn clean test
```

Coverage reports are generated in `target/site/jacoco/index.html`

## Configuration

The framework is configured to:
- Require 80% overall line coverage
- Require 70% line coverage per class (excluding Main and Benchmarks)
- Use JaCoCo for coverage reporting
- Support the Java Vector API (incubator module)

## Best Practices

1. **Extend BaseWaveletTest** for wavelet-specific tests to get common setup
2. **Use WaveletAssertions** instead of standard JUnit assertions for better error messages
3. **Test both implementations** when verifying mathematical properties
4. **Use appropriate tolerances** - different operations may need different precision
5. **Generate reproducible test signals** using seeded random generators

## Common Tolerances

- `DEFAULT_TOLERANCE`: 1e-10 - General floating-point comparisons
- `ENERGY_TOLERANCE`: 1e-12 - Energy preservation checks
- `ORTHOGONALITY_TOLERANCE`: 1e-14 - Filter orthogonality verification