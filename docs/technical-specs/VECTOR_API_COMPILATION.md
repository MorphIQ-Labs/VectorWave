# Vector API Compilation Requirements

## Overview

The VectorWave project uses Java's Vector API (jdk.incubator.vector) for performance optimization in certain operations. This document explains the compilation requirements and how the fallback mechanism works.

## Compilation Requirements

### Required JDK Version
- **Minimum**: JDK 16 (when Vector API was introduced)
- **Recommended**: JDK 21+ (for better Vector API stability)

### Important Note
The project **requires** compilation with a JDK that includes the `jdk.incubator.vector` module. This is because:

1. The `OptimizedFFT` class has direct imports of Vector API types:
   ```java
   import jdk.incubator.vector.*;
   ```

2. The code uses Vector API types directly in method signatures and fields:
   ```java
   private static final VectorSpecies<Double> SPECIES;
   DoubleVector.fromArray(SPECIES, data, offset);
   ```

## Runtime Behavior

While compilation requires the Vector API module, at runtime the code includes fallback mechanisms:

1. **Vector API Available**: Uses optimized SIMD operations
2. **Vector API Not Available**: Gracefully falls back to scalar implementations

The detection happens in the static initializer:
```java
static {
    boolean vectorAvailable = false;
    VectorSpecies<Double> speciesTemp = null;
    
    try {
        speciesTemp = DoubleVector.SPECIES_PREFERRED;
        DoubleVector.zero(speciesTemp);
        vectorAvailable = true;
    } catch (Throwable e) {
        // Vector API not available - use scalar fallback
    }
    
    VECTOR_API_AVAILABLE = vectorAvailable;
    SPECIES = speciesTemp;
}
```

## Building Without Vector API

Currently, there is no way to build the project without a Vector API-capable JDK. Future improvements could include:

1. **Conditional Compilation**: Using build profiles to exclude Vector API code
2. **Reflection-Based Access**: Accessing Vector API through reflection (complex but portable)
3. **Separate Modules**: Moving Vector API code to a separate optional module

## Compiler Flags

When building with Maven, the following flags are automatically added:
```xml
<compilerArgs>
    <arg>--add-modules=jdk.incubator.vector</arg>
</compilerArgs>
```

## IDE Configuration

### IntelliJ IDEA
1. Ensure Project SDK is set to JDK 16+
2. Enable preview features if needed
3. Add `--add-modules=jdk.incubator.vector` to compiler options

### Eclipse
1. Set Java Build Path to JDK 16+
2. Add the incubator module in Java Build Path → Libraries → Modulepath

### VS Code
1. Set `java.configuration.runtimes` to include JDK 16+
2. Ensure the Java extension pack recognizes the incubator module

## Verification

To verify your JDK supports Vector API:
```bash
java --list-modules | grep jdk.incubator.vector
```

If the module is present, you'll see:
```
jdk.incubator.vector@21.0.1
```

## Performance Impact

When Vector API is available:
- 2-4x performance improvement for large FFT operations
- Reduced memory bandwidth usage
- Better CPU cache utilization

When using scalar fallback:
- No performance penalty compared to standard implementations
- Fully functional with identical results
- Suitable for all platforms

## Future Considerations

The Vector API is still in incubator status as of JDK 21. When it becomes a standard API:
1. The import statements will change from `jdk.incubator.vector` to a standard package
2. The compilation requirements may become more flexible
3. The `--add-modules` flag will no longer be needed

## Related Documentation

- [VECTOR_API_FALLBACK.md](VECTOR_API_FALLBACK.md) - Runtime fallback mechanism details
- [BENCHMARKING.md](../BENCHMARKING.md) - Performance comparison with and without Vector API