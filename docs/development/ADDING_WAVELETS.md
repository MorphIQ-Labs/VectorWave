# Adding New Wavelets to VectorWave

This guide explains how to add new wavelet types to the VectorWave library.

## Understanding the Wavelet Hierarchy

VectorWave uses a sealed interface hierarchy to ensure type safety:

```
Wavelet (base interface)
├── DiscreteWavelet
│   ├── OrthogonalWavelet
│   └── BiorthogonalWavelet
└── ContinuousWavelet
    └── ComplexContinuousWavelet
```

## ServiceLoader Architecture

VectorWave uses Java's ServiceLoader mechanism for wavelet discovery, providing several benefits:

- **No Circular Dependencies**: Wavelets are loaded dynamically, avoiding static initialization issues
- **Plugin Support**: Add wavelets without modifying the core library
- **Clean Separation**: Each wavelet family can have its own provider
- **Runtime Flexibility**: Load wavelets conditionally based on configuration

## Steps to Add a New Wavelet

### 1. Choose the Appropriate Interface

- **OrthogonalWavelet**: For wavelets where reconstruction filters are the same as decomposition filters (e.g., Haar, Daubechies, Symlets, Coiflets)
- **BiorthogonalWavelet**: For wavelets with different decomposition and reconstruction filters (e.g., Biorthogonal Splines)
- **ContinuousWavelet**: For wavelets defined by mathematical functions (e.g., Morlet, Mexican Hat)

### 2. Implement the Wavelet

#### Example: Adding an Orthogonal Wavelet

```java
package com.morphiqlabs.wavelet.api;

public final class Meyer implements OrthogonalWavelet {
    
    private static final double[] LOW_PASS_COEFFS = {
        // Add Meyer wavelet coefficients here
        0.0, 0.0, // ... actual coefficients
    };
    
    @Override
    public String name() {
        return "meyer";
    }
    
    @Override
    public String description() {
        return "Meyer wavelet";
    }
    
    @Override
    public double[] lowPassDecomposition() {
        return LOW_PASS_COEFFS.clone();
    }
    
    @Override
    public double[] highPassDecomposition() {
        // Generate using quadrature mirror filter
        double[] h = LOW_PASS_COEFFS;
        double[] g = new double[h.length];
        for (int i = 0; i < h.length; i++) {
            g[i] = (i % 2 == 0 ? 1 : -1) * h[h.length - 1 - i];
        }
        return g;
    }
    
    @Override
    public int vanishingMoments() {
        return 4; // Meyer specific value
    }
}
```

#### Example: Adding a Biorthogonal Wavelet

```java
package com.morphiqlabs.wavelet.api;

public final class BiorthogonalSpline22 implements BiorthogonalWavelet {
    
    private static final double[] DECOMP_LOW_PASS = {
        // Decomposition low-pass filter coefficients
    };
    
    private static final double[] RECON_LOW_PASS = {
        // Reconstruction low-pass filter coefficients
    };
    
    @Override
    public String name() {
        return "bior2.2";
    }
    
    @Override
    public double[] lowPassDecomposition() {
        return DECOMP_LOW_PASS.clone();
    }
    
    @Override
    public double[] highPassDecomposition() {
        // Generate from reconstruction low-pass
        return generateHighPass(RECON_LOW_PASS);
    }
    
    @Override
    public double[] lowPassReconstruction() {
        return RECON_LOW_PASS.clone();
    }
    
    @Override
    public double[] highPassReconstruction() {
        // Generate from decomposition low-pass
        return generateHighPass(DECOMP_LOW_PASS);
    }
    
    @Override
    public int vanishingMoments() {
        return 2;
    }
    
    @Override
    public int dualVanishingMoments() {
        return 2;
    }
    
    @Override
    public boolean isSymmetric() {
        return true;
    }
    
    private double[] generateHighPass(double[] lowPass) {
        double[] g = new double[lowPass.length];
        for (int i = 0; i < lowPass.length; i++) {
            g[i] = (i % 2 == 0 ? 1 : -1) * lowPass[lowPass.length - 1 - i];
        }
        return g;
    }
}
```

#### Example: Adding a Continuous Wavelet

```java
package com.morphiqlabs.wavelet.api;

public final class MexicanHatWavelet implements ContinuousWavelet {
    
    private final double sigma;
    
    public MexicanHatWavelet(double sigma) {
        this.sigma = sigma;
    }
    
    @Override
    public String name() {
        return "mexh";
    }
    
    @Override
    public double psi(double t) {
        double t2 = t * t;
        double sigma2 = sigma * sigma;
        double norm = 2.0 / (Math.sqrt(3 * sigma) * Math.pow(Math.PI, 0.25));
        return norm * (1.0 - t2 / sigma2) * Math.exp(-t2 / (2 * sigma2));
    }
    
    @Override
    public double centerFrequency() {
        return 0.25; // Mexican Hat center frequency
    }
    
    @Override
    public double bandwidth() {
        return sigma;
    }
    
    @Override
    public boolean isComplex() {
        return false;
    }
    
    @Override
    public double[] discretize(int numCoeffs) {
        // Discretize the continuous wavelet
        double[] coeffs = new double[numCoeffs];
        double t0 = -5.0 * sigma;
        double dt = 10.0 * sigma / (numCoeffs - 1);
        
        for (int i = 0; i < numCoeffs; i++) {
            coeffs[i] = psi(t0 + i * dt);
        }
        
        // Normalize
        double sum = 0;
        for (double c : coeffs) {
            sum += c * c;
        }
        double norm = Math.sqrt(sum);
        for (int i = 0; i < numCoeffs; i++) {
            coeffs[i] /= norm;
        }
        
        return coeffs;
    }
}
```

### 3. Register the Wavelet with ServiceLoader

Starting from version 1.0, VectorWave uses ServiceLoader for automatic wavelet discovery. This eliminates circular dependencies and enables a plugin architecture.

#### Option A: Add to an Existing Provider (Core Library Contributors)

If you're contributing to the core library, add your wavelet to the appropriate existing provider:

```java
// In OrthogonalWaveletProvider.java
@Override
public List<Wavelet> getWavelets() {
    return List.of(
        new Haar(),
        // ... existing wavelets ...
        new Meyer()  // Add your new wavelet here
    );
}
```

#### Option B: Create a New Provider (Recommended for Plugins)

For third-party wavelets or plugins, create your own provider:

1. **Create a Provider Class**:
```java
package com.example.wavelets;

import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletProvider;
import java.util.List;

public class MyWaveletProvider implements WaveletProvider {
    
    @Override
    public List<Wavelet> getWavelets() {
        return List.of(
            new Meyer(),
            new BiorthogonalSpline22(),
            new MexicanHatWavelet(1.0)
        );
    }
}
```

2. **Register with ServiceLoader**:
Create a file `src/main/resources/META-INF/services/com.morphiqlabs.wavelet.api.WaveletProvider`:
```
com.example.wavelets.MyWaveletProvider
```

3. **For Java Modules** (optional):
Add to your `module-info.java`:
```java
module com.example.wavelets {
    requires com.morphiqlabs.vectorwave;
    
    provides com.morphiqlabs.wavelet.api.WaveletProvider 
        with com.example.wavelets.MyWaveletProvider;
}
```

Your wavelets will be automatically discovered when the JAR is on the classpath!

#### Manual Registration (Runtime)

For dynamic scenarios, you can also register wavelets at runtime:

```java
// Register a single wavelet
WaveletRegistry.registerWavelet(new Meyer());

// Reload all providers (useful for plugin systems)
WaveletRegistry.reload();
```

### 4. Consider Optimization Paths

For optimal performance, consider implementing specialized optimizations:

1. **SIMD Optimizations**: If your wavelet has specific patterns, add optimized paths in `VectorOps`
2. **Cache-aware Operations**: For wavelets with large filters, consider cache-aware implementations
3. **Specialized Kernels**: Add wavelet-specific kernels in `SpecializedKernels` for common operations

Example:
```java
// In SpecializedKernels.java
public static class MeyerKernel {
    public static double[] optimizedTransform(double[] signal, Meyer wavelet) {
        // Optimized implementation specific to Meyer wavelet
    }
}
```

### 5. Add Tests

Create comprehensive tests for your wavelet:

```java
@Test
void testMeyerWaveletProperties() {
    Meyer meyer = new Meyer();
    
    assertEquals("meyer", meyer.name());
    assertNotNull(meyer.lowPassDecomposition());
    assertNotNull(meyer.highPassDecomposition());
    
    // Test perfect reconstruction
    WaveletTransform transform = new WaveletTransform(meyer, BoundaryMode.PERIODIC);
    double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
    TransformResult result = transform.forward(signal);
    double[] reconstructed = transform.inverse(result);
    
    assertArrayEquals(signal, reconstructed, 1e-10);
}
```

### 6. Update Documentation

- Add your wavelet to the README.md features list
- Update any relevant examples
- Document the wavelet's properties and use cases

## Wavelet Coefficient Sources

When implementing wavelets, you'll need the filter coefficients. Good sources include:

1. **PyWavelets**: Python library with extensive wavelet coefficients
2. **MATLAB Wavelet Toolbox**: Comprehensive wavelet documentation
3. **Academic Papers**: Original papers defining the wavelets
4. **"Ten Lectures on Wavelets" by Ingrid Daubechies**: Classic reference

## Best Practices

1. **Coefficient Precision**: Use high-precision coefficients (at least 15 decimal places)
2. **Normalization**: Ensure filters are properly normalized
3. **Perfect Reconstruction**: Test that forward + inverse transform preserves the signal
4. **Memory Efficiency**: Clone arrays when returning to ensure immutability
5. **Documentation**: Include references to the source of coefficients
6. **Thread Safety**: Ensure your wavelet implementation is thread-safe (typically by being immutable)
7. **Validation**: Implement proper coefficient verification in `verifyCoefficients()`
8. **Performance**: Consider platform-specific optimizations, especially for Apple Silicon and ARM

## Common Pitfalls

1. **Sign Conventions**: Different sources may use different sign conventions for high-pass filters
2. **Normalization**: Some sources use different normalization factors
3. **Filter Length**: Ensure filter lengths are appropriate for the wavelet
4. **Boundary Effects**: Test with different boundary modes

## Testing Your Wavelet

Use the provided test utilities and follow the existing test patterns:

```java
// Test perfect reconstruction
double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
WaveletTransform transform = new WaveletTransform(myWavelet, BoundaryMode.PERIODIC);
TransformResult result = transform.forward(signal);
double[] reconstructed = transform.inverse(result);
assertArrayEquals(signal, reconstructed, 1e-10);

// Test coefficient verification
Wavelet wavelet = new MyWavelet();
boolean valid = wavelet.verifyCoefficients();
assertTrue(valid, "Wavelet coefficients should be valid");

// Test with different boundary modes
for (BoundaryMode mode : BoundaryMode.values()) {
    transform = new WaveletTransform(myWavelet, mode);
    // Test transform
}

// Test with various signal sizes
int[] sizes = {8, 16, 32, 64, 128, 256, 512, 1024};
for (int size : sizes) {
    double[] testSignal = new double[size];
    // Initialize and test
}
```

### Performance Testing

Add benchmarks for your wavelet:

```java
@Benchmark
public TransformResult benchmarkMyWavelet(BenchmarkState state) {
    return state.transform.forward(state.signal);
}
```

## Integration with VectorWave Features

Ensure your wavelet works with all VectorWave features:

### Multi-Level Transforms
```java
MultiLevelWaveletTransform mlTransform = new MultiLevelWaveletTransform(
    new MyWavelet(), 
    BoundaryMode.PERIODIC
);
MultiLevelTransformResult result = mlTransform.decompose(signal, 3);
```

### Denoising
```java
WaveletDenoiser denoiser = new WaveletDenoiser(new MyWavelet());
double[] denoised = denoiser.denoise(noisySignal, ThresholdMethod.UNIVERSAL);
```

### Streaming Support
```java
StreamingWaveletTransform stream = new StreamingWaveletTransformImpl(
    new MyWavelet(), 
    BoundaryMode.PERIODIC
);
```

### Parallel Processing
```java
ParallelWaveletEngine engine = new ParallelWaveletEngine(new MyWavelet(), 4);
List<TransformResult> results = engine.transformBatch(signals);
```

## Contributing

When contributing a new wavelet:

1. Ensure all tests pass: `mvn test`
2. Check code coverage: `mvn clean test jacoco:report`
3. Include benchmark results comparing to existing wavelets
4. Provide references for coefficient sources
5. Update README.md to include your wavelet in the features list
6. Consider adding a demo showcasing your wavelet's use cases

For questions or assistance, please open an issue on the project repository.