# Real-World Test Data Package

This package contains tests that use actual financial tick data to validate wavelet analysis functionality.

## Test Organization

### Active Tests
- **RealWorldTickDataSimpleTest.java** - Basic tests that run as part of the standard test suite
  - Quick validation of core functionality
  - Limited data processing for fast execution
  - Suitable for CI/CD pipelines

### Disabled Tests
- **RealWorldTickDataTest.java** - Comprehensive integration tests (disabled by default)
  - Advanced wavelet analysis demonstrations
  - Performance benchmarks on large datasets
  - Integration testing across multiple components
  - Enable with: Remove `@Disabled` annotation or run specific tests

## Test Data
- **TickDataLoader.java** - Utility for loading compressed tick data
  - Uses Apache Commons Compress for platform-independent extraction
  - Fallback to system tar command if needed
  - Test data stored in: `/src/test/resources/ticks_*.tar.xz`

## Running Disabled Tests

To run the comprehensive tests:

```bash
# Run all tests including disabled ones
mvn test -DexcludeGroups=none

# Run specific disabled test
mvn test -Dtest=RealWorldTickDataTest#testSpecificMethod
```

## Adding New Tests

1. For quick validation tests, add to `RealWorldTickDataSimpleTest`
2. For comprehensive/slow tests, add to `RealWorldTickDataTest` 
3. Always consider test execution time for CI/CD pipelines