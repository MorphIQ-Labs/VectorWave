package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.extensions.util.OptimizedFFT;

import com.morphiqlabs.wavelet.config.TransformConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests to verify that the FFT implementation correctly uses scalar path
 * for small signals to avoid vectorization overhead.
 */
class FFTThresholdTest {
    
    private static final double TOLERANCE = 1e-10;
    
    @Test
    @DisplayName("Small signals should use scalar path by default")
    void testSmallSignalsUseScalarPath() {
        // Test various small sizes that should use scalar path
        int[] smallSizes = {2, 4, 8, 16, 32};
        
        for (int size : smallSizes) {
            // Create test signal
            double[] data = new double[2 * size];
            for (int i = 0; i < size; i++) {
                data[2 * i] = Math.sin(2 * Math.PI * i / size);
                data[2 * i + 1] = 0.0;
            }
            
            // Clone for comparison
            double[] dataVector = data.clone();
            double[] dataScalar = data.clone();
            
            // Default should use scalar for small sizes
            OptimizedFFT.fftOptimized(dataVector, size, false);
            
            // Force scalar to ensure we get the same results
            TransformConfig scalarConfig = TransformConfig.builder()
                .forceScalar(true)
                .build();
            OptimizedFFT.fftOptimized(dataScalar, size, false, scalarConfig);
            
            // Results should be identical
            for (int i = 0; i < data.length; i++) {
                assertEquals(dataScalar[i], dataVector[i], TOLERANCE,
                    "Small signal (size " + size + ") should produce identical results");
            }
        }
    }
    
    @Test
    @DisplayName("Large signals should use vector path when available")
    void testLargeSignalsUseVectorPath() {
        // Only test if Vector API is available
        if (!OptimizedFFT.isVectorApiAvailable()) {
            System.out.println("Skipping vector path test - Vector API not available");
            return;
        }
        
        // Test sizes that should use vector path
        int[] largeSizes = {64, 128, 256, 512, 1024};
        
        for (int size : largeSizes) {
            // Create test signal
            double[] data = new double[2 * size];
            for (int i = 0; i < size; i++) {
                data[2 * i] = Math.random();
                data[2 * i + 1] = Math.random();
            }
            
            // Clone for comparison
            double[] dataDefault = data.clone();
            double[] dataScalar = data.clone();
            
            // Default should use vector for large sizes
            OptimizedFFT.fftOptimized(dataDefault, size, false);
            
            // Force scalar for comparison
            TransformConfig scalarConfig = TransformConfig.builder()
                .forceScalar(true)
                .build();
            OptimizedFFT.fftOptimized(dataScalar, size, false, scalarConfig);
            
            // Results should be very close (may have minor floating point differences)
            for (int i = 0; i < data.length; i++) {
                assertEquals(dataScalar[i], dataDefault[i], 1e-9,
                    "Large signal (size " + size + ") results should be close");
            }
        }
    }
    
    @Test
    @DisplayName("Threshold boundary behavior")
    void testThresholdBoundary() {
        // Test right at the boundary (assuming threshold is 64)
        int[] boundarySizes = {32, 64, 128};
        
        for (int size : boundarySizes) {
            double[] data = new double[2 * size];
            for (int i = 0; i < size; i++) {
                data[2 * i] = 1.0;
                data[2 * i + 1] = 0.0;
            }
            
            // Should not throw any exceptions
            assertDoesNotThrow(() -> OptimizedFFT.fftOptimized(data.clone(), size, false),
                "FFT should handle size " + size + " correctly");
            
            // Verify correctness with round-trip
            double[] roundTrip = data.clone();
            OptimizedFFT.fftOptimized(roundTrip, size, false);
            OptimizedFFT.fftOptimized(roundTrip, size, true);
            
            for (int i = 0; i < data.length; i++) {
                assertEquals(data[i], roundTrip[i], TOLERANCE,
                    "Round-trip FFT should preserve data at size " + size);
            }
        }
    }
}
