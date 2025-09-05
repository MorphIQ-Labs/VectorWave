package com.morphiqlabs.wavelet.extensions.parallel;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;

import static org.junit.jupiter.api.Assertions.*;

class ParallelLifecycleTest {

    @Test
    @DisplayName("Parallel transforms and denoisers implement AutoCloseable")
    void autoCloseableImplemented() {
        assertInstanceOf(AutoCloseable.class,
            new ParallelMultiLevelTransform(Daubechies.DB4, BoundaryMode.PERIODIC));
        assertInstanceOf(AutoCloseable.class,
            new ParallelWaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC));
    }

    @Test
    @DisplayName("Try-with-resources works for convenience constructors")
    void tryWithResourcesConvenience() {
        assertDoesNotThrow(() -> {
            try (var t = new ParallelMultiLevelTransform(Daubechies.DB4, BoundaryMode.PERIODIC)) {
                double[] x = new double[1024];
                var r = t.decompose(x, 3);
                assertNotNull(r);
            }
            try (var d = new ParallelWaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC)) {
                double[] x = new double[1024];
                double[] y = d.denoise(x, com.morphiqlabs.wavelet.denoising.WaveletDenoiser.ThresholdMethod.UNIVERSAL,
                    com.morphiqlabs.wavelet.denoising.WaveletDenoiser.ThresholdType.SOFT);
                assertNotNull(y);
            }
        });
    }

    @Test
    @DisplayName("Closing transform does not shutdown external ParallelConfig")
    void closeDoesNotShutdownExternalConfig() {
        ParallelConfig external = new ParallelConfig.Builder()
            .useVirtualThreads(true)
            .enableMetrics(true)
            .build();

        ParallelMultiLevelTransform t = new ParallelMultiLevelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, external);
        ParallelWaveletDenoiser d = new ParallelWaveletDenoiser(
            Daubechies.DB4, BoundaryMode.PERIODIC, external);

        // Close both; external config should remain usable by design
        t.close();
        d.close();

        ExecutorService ioExec = external.getIOExecutor();
        assertNotNull(ioExec);
        assertFalse(ioExec.isShutdown(), "External config should not be shutdown by close()");

        // Clean up explicitly
        external.shutdown();
    }
}
