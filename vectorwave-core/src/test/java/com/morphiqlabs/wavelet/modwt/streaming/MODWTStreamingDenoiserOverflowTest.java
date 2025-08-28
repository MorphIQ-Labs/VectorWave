package com.morphiqlabs.wavelet.modwt.streaming;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import org.junit.jupiter.api.Test;


import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify overflow protection in MODWTStreamingDenoiser step size calculation.
 */
class MODWTStreamingDenoiserOverflowTest {
    
    @Test
    void testStepSizeCalculationWithLargeArrays() {
        // Create a denoiser with a small noise window
        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(new Haar())
            .boundaryMode(BoundaryMode.PERIODIC)
            .noiseWindowSize(10)
            .build();
        
        // Test with various array sizes
        int[] testSizes = {100, 1000, 10000, 100000};
        
        for (int size : testSizes) {
            double[] largeSignal = new double[size];
            for (int i = 0; i < size; i++) {
                largeSignal[i] = Math.random();
            }
            
            // Should not throw any overflow exceptions
            assertDoesNotThrow(() -> {
                double[] denoised = denoiser.denoise(largeSignal);
                assertNotNull(denoised);
                assertEquals(size, denoised.length);
            }, "Should handle array of size " + size + " without overflow");
        }
        
        denoiser.close();
    }
    
    @Test
    void testStepSizeWithMinimumWindowSize() {
        // Test edge case with window size of 1
        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(new Haar())
            .boundaryMode(BoundaryMode.PERIODIC)
            .noiseWindowSize(1)
            .build();
        
        double[] signal = new double[1000];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(i * 0.1) + Math.random() * 0.1;
        }
        
        assertDoesNotThrow(() -> {
            double[] denoised = denoiser.denoise(signal);
            assertNotNull(denoised);
            assertEquals(signal.length, denoised.length);
        });
        
        denoiser.close();
    }
    
    @Test
    void testNoiseEstimationConsistency() {
        // Verify that noise estimation remains consistent across multiple calls
        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(new Haar())
            .boundaryMode(BoundaryMode.PERIODIC)
            .noiseWindowSize(100)
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
            .build();
        
        // Process multiple chunks
        for (int chunk = 0; chunk < 10; chunk++) {
            double[] signal = new double[512];
            for (int i = 0; i < signal.length; i++) {
                // Consistent noise level
                signal[i] = Math.sin(i * 0.1) + 0.1 * (Math.random() - 0.5);
            }
            
            double[] denoised = denoiser.denoise(signal);
            assertNotNull(denoised);
            
            // Noise level should stabilize after a few chunks
            if (chunk > 5) {
                double noiseLevel = denoiser.getEstimatedNoiseLevel();
                assertTrue(noiseLevel > 0, "Noise level should be positive");
                assertTrue(noiseLevel < 0.2, "Noise level should be reasonable for the added noise");
            }
        }
        
        denoiser.close();
    }
    
    @Test
    void testExtremeArraySizeHandling() {
        // Test with array size close to integer division limits
        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(new Haar())
            .boundaryMode(BoundaryMode.PERIODIC)
            .noiseWindowSize(1000)
            .build();
        
        // Create a moderately large signal that would cause issues with naive step calculation
        int size = 50000;
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = i % 100; // Simple pattern
        }
        
        assertDoesNotThrow(() -> {
            double[] denoised = denoiser.denoise(signal);
            assertNotNull(denoised);
            assertEquals(size, denoised.length);
            
            // Verify some denoising occurred
            boolean someChanged = false;
            for (int i = 0; i < size; i++) {
                if (Math.abs(denoised[i] - signal[i]) > 1e-10) {
                    someChanged = true;
                    break;
                }
            }
            assertTrue(someChanged, "Denoising should modify the signal");
        });
        
        denoiser.close();
    }
    
}