/**
 * VectorWave Extensions module.
 * <p>
 * Provides high-performance extensions for VectorWave using Java 24 preview
 * features such as the Vector API and structured concurrency utilities.
 * Public packages include SIMD optimizations, batch MODWT utilities, memory
 * layouts optimized for vectorized processing, and parallel execution helpers.
 * </p>
 */
module com.morphiqlabs.vectorwave.extensions {
    requires transitive com.morphiqlabs.vectorwave.core;
    requires jdk.incubator.vector;
    requires java.management;

    exports com.morphiqlabs.wavelet.extensions;
    exports com.morphiqlabs.wavelet.extensions.modwt;
    exports com.morphiqlabs.wavelet.extensions.parallel;
    exports com.morphiqlabs.wavelet.extensions.memory;
    // No internal exports; examples should use public APIs only
}
