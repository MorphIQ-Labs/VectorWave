package com.morphiqlabs.wavelet.extensions;

import com.morphiqlabs.wavelet.api.WaveletTransformOptimizer;
import jdk.incubator.vector.*;

/**
 * Vector API-based optimizer for wavelet transforms.
 * Provides SIMD acceleration on supported platforms.
 */
public class VectorAPIOptimizer implements WaveletTransformOptimizer {

    /**
     * Creates a Vector API optimizer instance.
     * <p>
     * This optimizer leverages the Java Vector API to provide SIMD acceleration
     * for wavelet transforms. It is configured to use the preferred {@link jdk.incubator.vector.VectorSpecies}
     * for {@code double} values, and is only supported on platforms where the Vector API
     * is available. No additional configuration is required.
     * </p>
     */
    public VectorAPIOptimizer() {}
    
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    
    @Override
    public boolean isSupported() {
        try {
            // Test vector operations to verify support
            double[] test = new double[SPECIES.length()];
            DoubleVector vector = DoubleVector.fromArray(SPECIES, test, 0);
            return vector != null;
        } catch (Exception | Error e) {
            return false;
        }
    }
    
    @Override
    public int getPriority() {
        return 100; // Highest priority for Vector API optimizations
    }
    
    @Override
    public String getName() {
        return "Vector API Optimizer (" + SPECIES + ")";
    }
    
    @Override
    public OptimizationType getType() {
        return OptimizationType.VECTOR_API;
    }
}
