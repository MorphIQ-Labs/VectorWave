
/**
 * Defines the core module of the VectorWave library.
 * <p>
 * This module provides the primary public APIs for performing wavelet transforms
 * such as MODWT and SWT. Implementations here are pure scalar Java 21 for
 * maximum portability; SIMD/Vector API optimizations live exclusively in the
 * optional vectorwave-extensions module.
 * <p>
 * It also defines the Service Provider Interface (SPI) for wavelet providers,
 * allowing other modules to extend the library with additional wavelets or
 * optimized implementations.
 */
module com.morphiqlabs.vectorwave.core {
    // --- Module Dependencies ---
    requires com.morphiqlabs.vectorwave.fft;

    // --- Public API Exports ---

    // Core API interfaces, builders, and exceptions.
    exports com.morphiqlabs.wavelet.api;

    // Public utility operations facade used by examples and docs.
    exports com.morphiqlabs.wavelet;

    // Annotations (e.g., @Experimental) used on exported APIs.
    exports com.morphiqlabs.wavelet.annotations;

    // Continuous Wavelet Transform (EXPERIMENTAL).
    exports com.morphiqlabs.wavelet.cwt;
    // Keep CWT memory implementation internal (unexported); API uses interface in cwt.

    // Denoising algorithms.
    exports com.morphiqlabs.wavelet.denoising;

    // Maximal Overlap Discrete Wavelet Transform (MODWT).
    exports com.morphiqlabs.wavelet.modwt;

    // Stationary Wavelet Transform (SWT).
    exports com.morphiqlabs.wavelet.swt;

    // Qualified exports for extensions module (SIMD helpers and internals used there)
    exports com.morphiqlabs.wavelet.util to com.morphiqlabs.vectorwave.extensions;
    exports com.morphiqlabs.wavelet.memory to com.morphiqlabs.vectorwave.extensions;
    exports com.morphiqlabs.wavelet.exception to com.morphiqlabs.vectorwave.extensions;
    exports com.morphiqlabs.wavelet.internal to com.morphiqlabs.vectorwave.extensions;
    exports com.morphiqlabs.wavelet.config to com.morphiqlabs.vectorwave.extensions;

    // --- Service Consumption ---

    // Specifies that this module uses implementations of the WaveletTransformOptimizer SPI.
    uses com.morphiqlabs.wavelet.api.WaveletTransformOptimizer;
}
