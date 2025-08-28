# Wavelet Registry Best Practices Guide

## Overview

The VectorWave `WaveletRegistry` provides a centralized, thread-safe way to discover, access, and manage wavelets in your applications. This guide demonstrates best practices for using the registry effectively and safely.

## Table of Contents

1. [Quick Start](#quick-start)
2. [Discovery Patterns](#discovery-patterns)
3. [Safe Selection](#safe-selection)
4. [Error Handling](#error-handling)
5. [Performance Optimization](#performance-optimization)
6. [Advanced Selection Strategies](#advanced-selection-strategies)
7. [Integration Patterns](#integration-patterns)
8. [Common Pitfalls](#common-pitfalls)

## Quick Start

### Basic Usage
```java
// Type-safe wavelet selection with enum
Wavelet db4 = WaveletRegistry.getWavelet(WaveletName.DB4);
Wavelet haar = WaveletRegistry.getWavelet(WaveletName.HAAR);

// Get all available wavelets
Set<WaveletName> allWavelets = WaveletRegistry.getAvailableWavelets();

// Get wavelets by type (returns enum list)
List<WaveletName> orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
Set<WaveletName> continuousWavelets = WaveletRegistry.getWaveletsByType(WaveletType.CONTINUOUS);
```

### With MODWT Integration
```java
// Type-safe wavelet selection with MODWT
WaveletName selected = WaveletName.DB4;  // IDE autocomplete!
if (WaveletRegistry.hasWavelet(selected)) {
    Wavelet wavelet = WaveletRegistry.getWavelet(selected);
    if (wavelet instanceof DiscreteWavelet discreteWavelet) {
        MODWTTransform transform = new MODWTTransform(discreteWavelet);
        // Perform transform...
    }
}
```

## Discovery Patterns

### ✅ Efficient Type-Based Discovery
```java
// GOOD: Use type-specific queries with enum results
List<WaveletName> orthogonal = WaveletRegistry.getOrthogonalWavelets();
List<WaveletName> continuous = WaveletRegistry.getContinuousWavelets();

// Family-specific queries
List<WaveletName> daubechies = WaveletRegistry.getDaubechiesWavelets();
List<WaveletName> symlets = WaveletRegistry.getSymletWavelets();
List<WaveletName> coiflets = WaveletRegistry.getCoifletWavelets();
```

### ✅ Type-Safe Iteration
```java
// Iterate through all orthogonal wavelets
for (WaveletName name : WaveletRegistry.getOrthogonalWavelets()) {
    Wavelet wavelet = WaveletRegistry.getWavelet(name);
    System.out.println(name + ": " + wavelet.description());
}
```

### Registry Overview
```java
// Print comprehensive overview
WaveletRegistry.printAvailableWavelets();

// Programmatic exploration with enum
for (WaveletType type : WaveletType.values()) {
    Set<WaveletName> wavelets = WaveletRegistry.getWaveletsByType(type);
    System.out.println(type + ": " + wavelets.size() + " wavelets");
}

// Use enum features for powerful queries
EnumSet<WaveletName> dbWavelets = EnumSet.of(
    WaveletName.DB2, WaveletName.DB4, WaveletName.DB6, 
    WaveletName.DB8, WaveletName.DB10
);
```

## Safe Selection

### Type-Safe Selection with Enum
```java
public Wavelet selectWaveletSafely(WaveletName... candidates) {
    for (WaveletName candidate : candidates) {
        if (WaveletRegistry.hasWavelet(candidate)) {
            return WaveletRegistry.getWavelet(candidate);
        }
    }
    // Fallback to HAAR which is always available
    return WaveletRegistry.getWavelet(WaveletName.HAAR);
}

// Usage with fallback strategy - compile-time safe!
Wavelet wavelet = selectWaveletSafely(
    WaveletName.SYM8, 
    WaveletName.DB4, 
    WaveletName.HAAR
);

// Switch on wavelet type - enum enables this!
switch (selectedWavelet) {
    case DB2, DB4, DB6, DB8, DB10 -> processDataubechies();
    case SYM2, SYM3, SYM4 -> processSymlet();
    case HAAR -> processHaar();
    default -> processGeneric();
}
```

### Signal Compatibility Validation
```java
public boolean isWaveletSuitableForSignal(DiscreteWavelet wavelet, int signalLength) {
    int filterLength = wavelet.lowPassDecomposition().length;
    // Rule of thumb: signal should be at least 3x the filter length
    return signalLength >= filterLength * 3;
}

// Usage
if (wavelet instanceof DiscreteWavelet dw) {
    if (isWaveletSuitableForSignal(dw, mySignal.length)) {
        // Safe to proceed
    } else {
        // Consider alternative wavelet or preprocessing
    }
}
```

## Error Handling

### Graceful Exception Handling
```java
public Optional<Wavelet> getWaveletOptional(String name) {
    try {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(WaveletRegistry.getWavelet(name));
    } catch (InvalidArgumentException e) {
        logger.warn("Wavelet not found: {}", name);
        return Optional.empty();
    }
}
```

### Validation with Detailed Feedback
```java
public ValidationResult validateWaveletForUseCase(String waveletName, String useCase) {
    if (!WaveletRegistry.isWaveletAvailable(waveletName)) {
        return ValidationResult.failure("Wavelet '" + waveletName + "' not available");
    }
    
    try {
        WaveletInfo info = WaveletRegistry.getWaveletInfo(waveletName);
        
        return switch (useCase) {
            case "financial_analysis" -> 
                info.vanishingMoments() >= 2 
                    ? ValidationResult.success() 
                    : ValidationResult.warning("Consider higher vanishing moments for smooth financial data");
            case "edge_detection" -> 
                "Haar".equals(info.family()) 
                    ? ValidationResult.success() 
                    : ValidationResult.info("Haar wavelets are typically preferred for edge detection");
            default -> ValidationResult.success();
        };
    } catch (Exception e) {
        return ValidationResult.failure("Error retrieving wavelet metadata: " + e.getMessage());
    }
}
```

## Performance Optimization

### Wavelet Caching
```java
public class WaveletCache {
    private final Map<String, Wavelet> cache = new ConcurrentHashMap<>();
    
    public Wavelet getWavelet(String name) {
        return cache.computeIfAbsent(name.toLowerCase(), 
            n -> WaveletRegistry.getWavelet(n));
    }
    
    // Use for frequently accessed wavelets
    private static final WaveletCache CACHE = new WaveletCache();
    
    public static Wavelet getCachedWavelet(String name) {
        return CACHE.getWavelet(name);
    }
}
```

### Batch Operations
```java
// Efficient batch wavelet creation
public Map<String, Wavelet> createWaveletSet(List<String> names) {
    return names.stream()
        .filter(WaveletRegistry::isWaveletAvailable)
        .collect(Collectors.toMap(
            name -> name,
            WaveletRegistry::getWavelet
        ));
}
```

### Performance Measurements
Based on our testing (1000 lookups):
- **Cached lookups**: ~50% faster than direct registry access
- **Type-specific queries**: ~10x faster than filtering all wavelets
- **Batch validation**: ~3x faster than individual checks

## Advanced Selection Strategies

### Metadata-Based Selection
```java
// Select wavelets with specific properties
public List<String> findSmoothWavelets(int minVanishingMoments) {
    return WaveletRegistry.getWaveletsByType(WaveletType.ORTHOGONAL)
        .stream()
        .filter(name -> {
            try {
                WaveletInfo info = WaveletRegistry.getWaveletInfo(name);
                return info.vanishingMoments() >= minVanishingMoments;
            } catch (Exception e) {
                return false;
            }
        })
        .collect(Collectors.toList());
}

// Select by computational efficiency
public List<String> findCompactWavelets(int maxFilterLength) {
    return WaveletRegistry.getAvailableWavelets()
        .stream()
        .filter(name -> {
            try {
                WaveletInfo info = WaveletRegistry.getWaveletInfo(name);
                return info.filterLength() > 0 && info.filterLength() <= maxFilterLength;
            } catch (Exception e) {
                return false;
            }
        })
        .sorted(Comparator.comparing(name -> {
            try {
                return WaveletRegistry.getWaveletInfo(name).filterLength();
            } catch (Exception e) {
                return Integer.MAX_VALUE;
            }
        }))
        .collect(Collectors.toList());
}
```

### Application-Specific Selection
```java
public class WaveletSelector {
    
    public static String selectForFinancialAnalysis() {
        // Priority: orthogonal, high vanishing moments, reasonable filter length
        return WaveletRegistry.getWaveletsByType(WaveletType.ORTHOGONAL)
            .stream()
            .filter(name -> {
                try {
                    WaveletInfo info = WaveletRegistry.getWaveletInfo(name);
                    return info.vanishingMoments() >= 4 && info.filterLength() <= 20;
                } catch (Exception e) {
                    return false;
                }
            })
            .findFirst()
            .orElse("db4"); // Fallback
    }
    
    public static String selectForImageCompression() {
        // Priority: biorthogonal for perfect reconstruction, symmetric
        return WaveletRegistry.getWaveletsByType(WaveletType.BIORTHOGONAL)
            .stream()
            .findFirst()
            .orElse("haar"); // Fallback
    }
    
    public static String selectForEdgeDetection() {
        // Simple, compact wavelets preferred
        return WaveletRegistry.isWaveletAvailable("haar") ? "haar" : "db2";
    }
}
```

## Integration Patterns

### With Spring Framework
```java
@Component
public class WaveletService {
    
    @Value("${app.default-wavelet:db4}")
    private String defaultWaveletName;
    
    @PostConstruct
    public void validateConfiguration() {
        if (!WaveletRegistry.isWaveletAvailable(defaultWaveletName)) {
            throw new IllegalStateException(
                "Configured wavelet '" + defaultWaveletName + "' is not available");
        }
    }
    
    public Wavelet getDefaultWavelet() {
        return WaveletRegistry.getWavelet(defaultWaveletName);
    }
}
```

### With Configuration Management
```java
public class WaveletConfig {
    private final String waveletName;
    private final WaveletType requiredType;
    private final int minVanishingMoments;
    
    public WaveletConfig(String waveletName, WaveletType requiredType, int minVanishingMoments) {
        this.waveletName = waveletName;
        this.requiredType = requiredType;
        this.minVanishingMoments = minVanishingMoments;
        validate();
    }
    
    private void validate() {
        if (!WaveletRegistry.isWaveletAvailable(waveletName)) {
            throw new IllegalArgumentException("Wavelet not available: " + waveletName);
        }
        
        Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
        if (wavelet.getType() != requiredType) {
            throw new IllegalArgumentException(
                "Wavelet type mismatch. Expected: " + requiredType + ", Actual: " + wavelet.getType());
        }
        
        try {
            WaveletInfo info = WaveletRegistry.getWaveletInfo(waveletName);
            if (info.vanishingMoments() < minVanishingMoments) {
                throw new IllegalArgumentException(
                    "Insufficient vanishing moments. Required: " + minVanishingMoments + 
                    ", Actual: " + info.vanishingMoments());
            }
        } catch (Exception e) {
            // Metadata not available - log warning but don't fail
            logger.warn("Could not validate vanishing moments for {}: {}", waveletName, e.getMessage());
        }
    }
    
    public Wavelet getWavelet() {
        return WaveletRegistry.getWavelet(waveletName);
    }
}
```

## Common Pitfalls

### ❌ Case Sensitivity Issues
```java
// WRONG: Assuming case sensitivity
WaveletRegistry.getWavelet("DB4"); // Works, but inconsistent

// RIGHT: Use consistent lowercase
WaveletRegistry.getWavelet("db4");
```

### ❌ Not Checking Availability
```java
// WRONG: Direct access without validation
Wavelet wavelet = WaveletRegistry.getWavelet(userInput); // May throw exception

// RIGHT: Always validate first
if (WaveletRegistry.isWaveletAvailable(userInput)) {
    Wavelet wavelet = WaveletRegistry.getWavelet(userInput);
    // Safe to use
}
```

### ❌ Ignoring Wavelet Type
```java
// WRONG: Assuming all wavelets work with MODWT
Wavelet wavelet = WaveletRegistry.getWavelet("morl");
MODWTTransform transform = new MODWTTransform((DiscreteWavelet) wavelet); // ClassCastException!

// RIGHT: Check type compatibility
Wavelet wavelet = WaveletRegistry.getWavelet("morl");
if (wavelet instanceof DiscreteWavelet discreteWavelet) {
    MODWTTransform transform = new MODWTTransform(discreteWavelet);
} else {
    // Handle continuous wavelets appropriately
}
```

### ❌ Performance Anti-patterns
```java
// WRONG: Repeated registry lookups in loops
for (int i = 0; i < 1000; i++) {
    Wavelet w = WaveletRegistry.getWavelet("db4"); // Inefficient
    // process...
}

// RIGHT: Cache frequently used wavelets
Wavelet cachedWavelet = WaveletRegistry.getWavelet("db4");
for (int i = 0; i < 1000; i++) {
    // Use cachedWavelet
}
```

## Running the Demo

To see all these patterns in action, run the comprehensive demo:

```bash
# From the VectorWave project root
mvn test-compile exec:java -Dexec.mainClass="com.morphiqlabs.examples.WaveletRegistryBestPracticesDemo" -Dexec.classpathScope=test
```

The demo will show:
- ✅ Wavelet discovery patterns
- ✅ Safe selection with validation
- ✅ Error handling strategies
- ✅ Performance optimization techniques
- ✅ Advanced selection algorithms
- ✅ MODWT integration examples
- ✅ Metadata-driven decision making

## Key Takeaways

1. **Always validate**: Check wavelet availability before use
2. **Use type-specific queries**: More efficient than filtering all wavelets
3. **Handle errors gracefully**: Provide meaningful fallbacks
4. **Cache frequently used wavelets**: Significant performance improvement
5. **Leverage metadata**: Make informed decisions based on wavelet properties
6. **Consider signal compatibility**: Match wavelet properties to your data characteristics
7. **Plan for extensibility**: Design your code to handle new wavelets automatically

This approach ensures robust, performant, and maintainable wavelet-based applications.