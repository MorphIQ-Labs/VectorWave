# VectorWave Examples

Organized examples demonstrating VectorWave capabilities.

## Structure

```
examples/
├── basic/              # Start here - fundamental concepts
├── advanced/           # Advanced features and optimizations  
└── integration/        # Integration with other systems
```

## Basic Examples

Start with these to understand core concepts:

### 1. GettingStarted.java
**The absolute minimum to get running**
```bash
mvn exec:java -Dexec.mainClass="com.morphiqlabs.examples.basic.GettingStarted"
```
- Get a wavelet
- Transform a signal
- Get coefficients
- Reconstruct

### 2. WaveletRegistryBasics.java
**Essential registry operations**
```bash
mvn exec:java -Dexec.mainClass="com.morphiqlabs.examples.basic.WaveletRegistryBasics"
```
- Discover available wavelets
- Safe wavelet selection
- Error handling
- Using with MODWT

## Integration Examples

### MotiveWaveIntegration.java
**Using VectorWave in trading platforms**
```bash
mvn exec:java -Dexec.mainClass="com.morphiqlabs.examples.integration.MotiveWaveIntegration"
```
- No ServiceLoader needed
- Dropdown menu population
- Study integration patterns

## Running Examples

All examples are self-contained and can be run with Maven:

```bash
# Compile first
mvn compile

# Run any example
mvn exec:java -Dexec.mainClass="com.morphiqlabs.examples.[category].[ExampleName]"
```

## Key Concepts Demonstrated

1. **Simple API** - No complex initialization
2. **Works Everywhere** - No ServiceLoader dependencies
3. **Safe Selection** - Always check wavelet availability
4. **MODWT Focus** - Works with any signal length
5. **Perfect Reconstruction** - Verify transform accuracy

## Common Patterns

### Safe Wavelet Selection
```java
// Enum-based approach - type-safe and compile-time checked
if (WaveletRegistry.hasWavelet(WaveletName.DB4)) {
    Wavelet w = WaveletRegistry.getWavelet(WaveletName.DB4);
}
```

### Dropdown Population (for UIs)
```java
List<WaveletName> wavelets = WaveletRegistry.getOrthogonalWavelets();
// Convert to display strings for UI
String[] displayNames = wavelets.stream()
    .map(WaveletName::getDescription)
    .toArray(String[]::new);
```

### Transform with Error Checking
```java
try {
    Wavelet w = WaveletRegistry.getWavelet(userInput);
    MODWTTransform transform = new MODWTTransform(w, BoundaryMode.PERIODIC);
    MODWTResult result = transform.forward(data);
} catch (InvalidArgumentException e) {
    // Handle invalid wavelet name
}
```

## Notes

- Examples use the simplified WaveletRegistry (no ServiceLoader)
- All examples work in constrained environments (MotiveWave, etc.)
- Focus on practical, real-world usage
- Minimal dependencies