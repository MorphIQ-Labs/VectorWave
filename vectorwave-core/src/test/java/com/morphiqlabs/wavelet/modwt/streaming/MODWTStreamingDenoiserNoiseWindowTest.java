package com.morphiqlabs.wavelet.modwt.streaming;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for noise window update logic in MODWTStreamingDenoiser.
 */
class MODWTStreamingDenoiserNoiseWindowTest {
    
    private Haar haar;
    
    @BeforeEach
    void setUp() {
        haar = new Haar();
    }
    
    @Test
    void testNoiseWindowUpdateWithSmallDetailArray() {
        // Test case where detail array is smaller than noise window
        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(haar)
            .boundaryMode(BoundaryMode.PERIODIC)
            .bufferSize(8)  // Small buffer
            .noiseWindowSize(100)  // Large noise window
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
            .build();
        
        // Process some data
        double[] data = new double[8];
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.sin(i * 0.1) + 0.1 * Math.random();
        }
        
        double[] result = denoiser.denoise(data);
        assertNotNull(result);
        assertEquals(data.length, result.length);
        
        // Should not throw any exceptions
        // All detail coefficients should be added to the noise window
    }
    
    @Test
    void testNoiseWindowUpdateWithLargeDetailArray() {
        // Test case where detail array is larger than noise window
        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(haar)
            .boundaryMode(BoundaryMode.PERIODIC)
            .bufferSize(256)  // Large buffer
            .noiseWindowSize(50)  // Smaller noise window
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
            .build();
        
        // Process multiple blocks to test window wrapping
        for (int block = 0; block < 5; block++) {
            double[] data = new double[256];
            for (int i = 0; i < data.length; i++) {
                data[i] = Math.sin(i * 0.1) + 0.1 * Math.random();
            }
            
            double[] result = denoiser.denoise(data);
            assertNotNull(result);
            assertEquals(data.length, result.length);
        }
        
        // Should handle large detail arrays without overwriting issues
    }
    
    @Test
    void testUniformSamplingStrategy() {
        // Test that uniform sampling maintains diversity
        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(haar)
            .boundaryMode(BoundaryMode.PERIODIC)
            .bufferSize(200)
            .noiseWindowSize(20)  // Much smaller than buffer
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
            .build();
        
        // Create data with distinct patterns at different positions
        double[] data = new double[200];
        // First part: low frequency
        for (int i = 0; i < 50; i++) {
            data[i] = Math.sin(i * 0.02);
        }
        // Middle part: high frequency
        for (int i = 50; i < 150; i++) {
            data[i] = Math.sin(i * 0.5);
        }
        // Last part: noise
        for (int i = 150; i < 200; i++) {
            data[i] = Math.random() - 0.5;
        }
        
        double[] result = denoiser.denoise(data);
        assertNotNull(result);
        assertEquals(data.length, result.length);
        
        // The noise window should have sampled from different parts of the signal
        // Not just the last 20 values
    }
    
    @Test
    void testNoiseEstimationConsistency() {
        // Test that noise estimation remains consistent across multiple runs
        int bufferSize = 128;
        int noiseWindowSize = 32;
        
        MODWTStreamingDenoiser denoiser1 = new MODWTStreamingDenoiser.Builder()
            .wavelet(haar)
            .boundaryMode(BoundaryMode.PERIODIC)
            .bufferSize(bufferSize)
            .noiseWindowSize(noiseWindowSize)
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
            .thresholdType(WaveletDenoiser.ThresholdType.SOFT)
            .thresholdMultiplier(3.0)
            .build();
        
        MODWTStreamingDenoiser denoiser2 = new MODWTStreamingDenoiser.Builder()
            .wavelet(haar)
            .boundaryMode(BoundaryMode.PERIODIC)
            .bufferSize(bufferSize)
            .noiseWindowSize(noiseWindowSize)
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
            .thresholdType(WaveletDenoiser.ThresholdType.SOFT)
            .thresholdMultiplier(3.0)
            .build();
        
        // Process same data through both denoisers
        double[] testData = new double[bufferSize];
        for (int i = 0; i < testData.length; i++) {
            testData[i] = Math.sin(i * 0.1) + 0.05 * Math.random();
        }
        
        double[] result1 = denoiser1.denoise(testData.clone());
        double[] result2 = denoiser2.denoise(testData.clone());
        
        // Results should be identical
        assertArrayEquals(result1, result2, 1e-10);
    }
    
    @Test
    void testEdgeCaseExactMultiple() {
        // Test when detail array length is exact multiple of noise window size
        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(haar)
            .boundaryMode(BoundaryMode.PERIODIC)
            .bufferSize(100)
            .noiseWindowSize(25)  // Exactly divides 100
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.STD)
            .build();
        
        double[] data = new double[100];
        Arrays.fill(data, 1.0);
        
        // Should handle this edge case without issues
        assertDoesNotThrow(() -> {
            double[] result = denoiser.denoise(data);
            assertNotNull(result);
            assertEquals(data.length, result.length);
        });
    }
}