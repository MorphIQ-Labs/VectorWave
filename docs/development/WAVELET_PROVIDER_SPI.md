# WaveletProvider Service Provider Interface (SPI)

## Overview

The `WaveletProvider` interface enables automatic discovery and registration of wavelets using Java's ServiceLoader mechanism. This provides a plugin architecture where third-party wavelets can be added without modifying the core VectorWave library.

## Interface Definition

```java
package com.morphiqlabs.wavelet.api;

public interface WaveletProvider {
    /**
     * Returns the wavelets provided by this implementation.
     * 
     * @return list of wavelets (must not be null or empty)
     */
    List<Wavelet> getWavelets();
}
```

## Implementation Guide

### Step 1: Create Your Wavelet Implementation

```java
package com.example.wavelets;

import com.morphiqlabs.wavelet.api.OrthogonalWavelet;

public record MyCustomWavelet() implements OrthogonalWavelet {
    
    @Override
    public String name() {
        return "custom1";
    }
    
    @Override
    public String description() {
        return "My custom orthogonal wavelet";
    }
    
    @Override
    public double[] lowPassDecomposition() {
        return new double[]{0.5, 0.5, 0.5, 0.5}; // Example coefficients
    }
    
    @Override
    public double[] highPassDecomposition() {
        // Generate using QMF or provide directly
        return OrthogonalWavelet.computeHighPassFromLowPass(lowPassDecomposition());
    }
    
    @Override
    public int vanishingMoments() {
        return 2;
    }
}
```

### Step 2: Create a Provider Implementation

```java
package com.example.wavelets;

import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletProvider;
import java.util.List;

public class CustomWaveletProvider implements WaveletProvider {
    
    @Override
    public List<Wavelet> getWavelets() {
        return List.of(
            new MyCustomWavelet(),
            new AnotherCustomWavelet()
        );
    }
}
```

### Step 3: Register the Provider

Create a file `META-INF/services/com.morphiqlabs.wavelet.api.WaveletProvider` in your resources directory:

```
com.example.wavelets.CustomWaveletProvider
```

### Step 4: Package and Use

Once packaged in a JAR and added to the classpath, your wavelets will be automatically discovered:

```java
// Your wavelets are now available
Wavelet custom = WaveletRegistry.getWavelet("custom1");
```

## Best Practices

### 1. Naming Conventions

- Use lowercase names for consistency
- Avoid conflicts with existing wavelets
- Consider prefixing with organization/project name

```java
@Override
public String name() {
    return "acme-fast1"; // Prefixed to avoid conflicts
}
```

### 2. Lazy Initialization

For wavelets with expensive initialization:

```java
public class LazyWaveletProvider implements WaveletProvider {
    private volatile List<Wavelet> wavelets;
    
    @Override
    public List<Wavelet> getWavelets() {
        if (wavelets == null) {
            synchronized (this) {
                if (wavelets == null) {
                    wavelets = createWavelets();
                }
            }
        }
        return wavelets;
    }
    
    private List<Wavelet> createWavelets() {
        // Expensive initialization here
        return List.of(...);
    }
}
```

### 3. Error Handling

The WaveletRegistry gracefully handles provider errors:

```java
public class RobustProvider implements WaveletProvider {
    @Override
    public List<Wavelet> getWavelets() {
        try {
            return loadWavelets();
        } catch (Exception e) {
            // Log error
            System.err.println("Failed to load custom wavelets: " + e);
            // Return empty list or partial results
            return List.of();
        }
    }
}
```

### 4. Conditional Loading

Load wavelets based on configuration or environment:

```java
public class ConditionalProvider implements WaveletProvider {
    @Override
    public List<Wavelet> getWavelets() {
        List<Wavelet> wavelets = new ArrayList<>();
        
        if (System.getProperty("enable.experimental.wavelets") != null) {
            wavelets.add(new ExperimentalWavelet());
        }
        
        wavelets.add(new StandardCustomWavelet());
        return wavelets;
    }
}
```

## Testing Your Provider

```java
@Test
void testProviderDiscovery() {
    // Verify provider is discovered
    ServiceLoader<WaveletProvider> loader = ServiceLoader.load(WaveletProvider.class);
    
    boolean found = false;
    for (WaveletProvider provider : loader) {
        if (provider instanceof CustomWaveletProvider) {
            found = true;
            break;
        }
    }
    assertTrue(found);
}

@Test
void testWaveletRegistration() {
    // Verify wavelets are registered
    assertTrue(WaveletRegistry.hasWavelet("custom1"));
    
    Wavelet wavelet = WaveletRegistry.getWavelet("custom1");
    assertNotNull(wavelet);
    assertEquals("custom1", wavelet.name());
}
```

## Module System Support

For Java 9+ modules, declare the service in `module-info.java`:

```java
module com.example.wavelets {
    requires com.morphiqlabs.vectorwave;
    
    provides com.morphiqlabs.wavelet.api.WaveletProvider 
        with com.example.wavelets.CustomWaveletProvider;
}
```

## Troubleshooting

### Wavelets Not Found

1. Verify `META-INF/services` file is in the correct location
2. Check the provider class name is fully qualified
3. Ensure the JAR is on the classpath
4. Call `WaveletRegistry.reload()` if loading dynamically

### ClassNotFoundException

- Ensure all dependencies are available
- Check for typos in the service file
- Verify the provider has a public no-args constructor

### Performance Issues

- Avoid heavy computation in `getWavelets()`
- Cache wavelet instances if they're expensive to create
- Consider lazy initialization for large wavelet sets

## Built-in Providers

VectorWave includes several built-in providers as examples:

- `OrthogonalWaveletProvider`: Haar, Daubechies, Symlets, Coiflets
- `BiorthogonalWaveletProvider`: Biorthogonal spline wavelets
- `ContinuousWaveletProvider`: Morlet, Gaussian derivatives
- `FinancialWaveletProvider`: Paul, DOG, Shannon wavelets

Study these implementations for best practices and patterns.