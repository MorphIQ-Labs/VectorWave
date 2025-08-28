package com.morphiqlabs.wavelet.modwt.streaming;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for validation in MODWTStreamingDenoiser noise estimation methods.
 */
class MODWTStreamingDenoiserValidationTest {
    
    private Haar haar;
    private MODWTStreamingDenoiser denoiser;
    
    @BeforeEach
    void setUp() {
        haar = new Haar();
        denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(haar)
            .boundaryMode(BoundaryMode.PERIODIC)
            .bufferSize(64)
            .noiseWindowSize(32)
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
            .build();
    }
    
    @Test
    void testCalculateMADValidation() throws Exception {
        // Access private calculateMAD method via reflection
        Method calculateMAD = MODWTStreamingDenoiser.class
            .getDeclaredMethod("calculateMAD", double[].class);
        calculateMAD.setAccessible(true);
        
        // Test null array
        assertThrows(Exception.class, () -> {
            calculateMAD.invoke(denoiser, (Object) null);
        });
        
        // Test empty array
        assertThrows(Exception.class, () -> {
            calculateMAD.invoke(denoiser, new double[0]);
        });
        
        // Test array with all zeros
        double result = (double) calculateMAD.invoke(denoiser, new double[]{0.0, 0.0, 0.0, 0.0});
        assertEquals(0.0, result, "MAD of all zeros should be 0");
        
        // Test array with NaN values
        result = (double) calculateMAD.invoke(denoiser, 
            new double[]{Double.NaN, Double.NaN, Double.NaN});
        assertEquals(0.0, result, "MAD of all NaN should be 0");
        
        // Test array with infinite values
        result = (double) calculateMAD.invoke(denoiser, 
            new double[]{Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY});
        assertEquals(0.0, result, "MAD of all infinite values should be 0");
        
        // Test mixed valid and invalid values
        result = (double) calculateMAD.invoke(denoiser, 
            new double[]{1.0, 2.0, Double.NaN, 3.0, Double.POSITIVE_INFINITY});
        assertTrue(result > 0, "MAD with some valid values should be positive");
        
        // Test normal case
        result = (double) calculateMAD.invoke(denoiser, 
            new double[]{1.0, 2.0, 3.0, 4.0, 5.0});
        assertTrue(result > 0, "MAD of normal values should be positive");
    }
    
    @Test
    void testCalculateSTDValidation() throws Exception {
        // Access private calculateSTD method via reflection
        Method calculateSTD = MODWTStreamingDenoiser.class
            .getDeclaredMethod("calculateSTD", double[].class);
        calculateSTD.setAccessible(true);
        
        // Test null array
        double result = (double) calculateSTD.invoke(denoiser, (Object) null);
        assertEquals(0.0, result, "STD of null should be 0");
        
        // Test empty array
        result = (double) calculateSTD.invoke(denoiser, new double[0]);
        assertEquals(0.0, result, "STD of empty array should be 0");
        
        // Test single value
        result = (double) calculateSTD.invoke(denoiser, new double[]{5.0});
        assertEquals(0.0, result, "STD of single value should be 0");
        
        // Test all NaN values
        result = (double) calculateSTD.invoke(denoiser, 
            new double[]{Double.NaN, Double.NaN, Double.NaN});
        assertEquals(0.0, result, "STD of all NaN should be 0");
        
        // Test mixed valid and invalid with less than 2 valid values
        result = (double) calculateSTD.invoke(denoiser, 
            new double[]{5.0, Double.NaN, Double.POSITIVE_INFINITY});
        assertEquals(0.0, result, "STD with < 2 valid values should be 0");
        
        // Test normal case
        result = (double) calculateSTD.invoke(denoiser, 
            new double[]{1.0, 2.0, 3.0, 4.0, 5.0});
        assertTrue(result > 0, "STD of normal values should be positive");
    }
    
    @Test
    void testDenoisingWithInvalidData() {
        // Test that denoising with NaN data throws appropriate exception
        double[] dataWithNaN = new double[64];
        for (int i = 0; i < 32; i++) {
            dataWithNaN[i] = Math.sin(i * 0.1);
        }
        for (int i = 32; i < 64; i++) {
            dataWithNaN[i] = Double.NaN;
        }
        
        // Should throw InvalidSignalException due to NaN values
        assertThrows(Exception.class, () -> {
            denoiser.denoise(dataWithNaN);
        });
        
        // Test with infinite values
        double[] dataWithInf = new double[64];
        for (int i = 0; i < 64; i++) {
            dataWithInf[i] = (i % 2 == 0) ? Double.POSITIVE_INFINITY : 1.0;
        }
        
        // Should throw exception due to infinite values
        assertThrows(Exception.class, () -> {
            denoiser.denoise(dataWithInf);
        });
    }
    
    @Test
    void testDenoisingWithZeroSignal() {
        // Test denoising when signal is all zeros
        double[] zeroSignal = new double[64];
        
        double[] result = denoiser.denoise(zeroSignal);
        assertNotNull(result);
        assertEquals(zeroSignal.length, result.length);
        
        // Result should also be all zeros or very close to zero
        for (double value : result) {
            assertEquals(0.0, value, 1e-10);
        }
    }
    
    @Test
    void testNoiseWindowUpdateWithSmallDetailSize() {
        // Test the edge case where details.length is slightly larger than noiseWindowSize
        // This verifies the step calculation doesn't result in 0
        MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(haar)
            .boundaryMode(BoundaryMode.PERIODIC)
            .bufferSize(64)
            .noiseWindowSize(50)  // Large window size
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
            .build();
        
        // Process a small signal that produces details.length slightly > noiseWindowSize
        // This would cause step = 0 without the Math.max fix
        double[] smallSignal = new double[52];  // Will produce 52 detail coefficients
        for (int i = 0; i < smallSignal.length; i++) {
            smallSignal[i] = Math.sin(i * 0.1) + 0.1 * Math.random();
        }
        
        // Should not hang or throw exception
        double[] result = denoiser.denoise(smallSignal);
        assertNotNull(result);
        assertEquals(smallSignal.length, result.length);
        
        // Process multiple times to ensure noise window updates properly
        for (int i = 0; i < 5; i++) {
            result = denoiser.denoise(smallSignal);
            assertNotNull(result);
        }
    }
    
    @Test
    void testNoiseEstimationConvergence() {
        // Test that noise estimation converges to a stable value
        MODWTStreamingDenoiser madDenoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(haar)
            .boundaryMode(BoundaryMode.PERIODIC)
            .bufferSize(64)
            .noiseWindowSize(128)
            .noiseEstimation(MODWTStreamingDenoiser.NoiseEstimation.MAD)
            .build();
        
        // Process multiple blocks of consistent noise
        double noiseLevel = 0.1;
        for (int block = 0; block < 10; block++) {
            double[] data = new double[64];
            for (int i = 0; i < data.length; i++) {
                data[i] = Math.sin(i * 0.1) + noiseLevel * (Math.random() - 0.5);
            }
            madDenoiser.denoise(data);
        }
        
        // Get estimated noise level
        double estimatedNoise = madDenoiser.getEstimatedNoiseLevel();
        
        // Should have converged to a reasonable estimate
        assertTrue(estimatedNoise > 0, "Noise estimate should be positive");
        assertTrue(estimatedNoise < 1.0, "Noise estimate should be reasonable");
    }
}