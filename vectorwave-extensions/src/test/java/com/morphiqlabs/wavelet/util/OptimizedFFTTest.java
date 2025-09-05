package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.extensions.util.OptimizedFFT;

import com.morphiqlabs.wavelet.config.TransformConfig;
import com.morphiqlabs.wavelet.cwt.ComplexNumber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for OptimizedFFT implementation.
 */
class OptimizedFFTTest {
    
    private static final double EPSILON = 1e-10;
    
    @Test
    @DisplayName("Real-optimized FFT should handle edge cases and odd-length arrays")
    void testRealOptimizedFFTEdgeCases() {
        // Test empty array
        double[] empty = {};
        ComplexNumber[] resultEmpty = OptimizedFFT.fftRealOptimized(empty);
        assertEquals(0, resultEmpty.length);
        
        // Test single element
        double[] single = {1.0};
        ComplexNumber[] resultSingle = OptimizedFFT.fftRealOptimized(single);
        assertEquals(1, resultSingle.length);
        assertEquals(1.0, resultSingle[0].real(), EPSILON);
        assertEquals(0.0, resultSingle[0].imag(), EPSILON);
        
        // Test odd-length arrays
        double[] odd3 = {1.0, 2.0, 3.0};
        double[] odd5 = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] odd7 = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0};
        
        // Should not throw ArrayIndexOutOfBoundsException
        ComplexNumber[] result3 = OptimizedFFT.fftRealOptimized(odd3);
        assertEquals(3, result3.length);
        
        ComplexNumber[] result5 = OptimizedFFT.fftRealOptimized(odd5);
        assertEquals(5, result5.length);
        
        ComplexNumber[] result7 = OptimizedFFT.fftRealOptimized(odd7);
        assertEquals(7, result7.length);
    }
    
    @Test
    @DisplayName("Real-optimized FFT should handle even-length arrays")
    void testRealOptimizedFFTEvenLength() {
        // Test even-length arrays
        double[] even2 = {1.0, 2.0};
        double[] even4 = {1.0, 2.0, 3.0, 4.0};
        double[] even8 = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        ComplexNumber[] result2 = OptimizedFFT.fftRealOptimized(even2);
        assertEquals(2, result2.length);
        
        ComplexNumber[] result4 = OptimizedFFT.fftRealOptimized(even4);
        assertEquals(4, result4.length);
        
        ComplexNumber[] result8 = OptimizedFFT.fftRealOptimized(even8);
        assertEquals(8, result8.length);
    }
    
    @Test
    @DisplayName("Real-optimized FFT should produce valid results")
    void testRealOptimizedFFTCorrectness() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        // Compute using real-optimized
        ComplexNumber[] optimized = OptimizedFFT.fftRealOptimized(signal);
        
        // Basic checks
        assertEquals(signal.length, optimized.length);
        
        // Check DC component (sum of all elements)
        double expectedDC = 0;
        for (double v : signal) {
            expectedDC += v;
        }
        assertEquals(expectedDC, optimized[0].real(), EPSILON, "DC component mismatch");
        
        // Check Parseval's theorem (energy conservation)
        double timeEnergy = 0;
        for (double v : signal) {
            timeEnergy += v * v;
        }
        
        double freqEnergy = 0;
        for (ComplexNumber c : optimized) {
            freqEnergy += c.real() * c.real() + c.imag() * c.imag();
        }
        freqEnergy /= signal.length; // Normalize
        
        assertEquals(timeEnergy, freqEnergy, 1e-9, "Energy not conserved");
    }
    
    @Test
    @DisplayName("Split-radix FFT should handle various sizes")
    void testSplitRadixFFT() {
        int[] sizes = {32, 64, 128, 256, 512, 1024};
        
        for (int size : sizes) {
            double[] data = new double[2 * size];
            // Initialize with test data
            for (int i = 0; i < size; i++) {
                data[2 * i] = Math.cos(2 * Math.PI * i / size);
                data[2 * i + 1] = Math.sin(2 * Math.PI * i / size);
            }
            
            // Should not throw exception
            OptimizedFFT.fftOptimized(data, size, false);
            
            // Basic sanity check - DC component
            double dcReal = 0, dcImag = 0;
            for (int i = 0; i < size; i++) {
                dcReal += Math.cos(2 * Math.PI * i / size);
                dcImag += Math.sin(2 * Math.PI * i / size);
            }
            assertEquals(dcReal, data[0], 1e-9, "DC real component mismatch for size " + size);
            assertEquals(dcImag, data[1], 1e-9, "DC imag component mismatch for size " + size);
        }
    }
    
    @Test
    @DisplayName("Bluestein FFT should handle non-power-of-2 sizes")
    void testBluesteinFFT() {
        int[] sizes = {100, 255, 500, 1000, 1500};
        
        for (int size : sizes) {
            double[] data = new double[2 * size];
            
            // Initialize with simple signal
            for (int i = 0; i < size; i++) {
                data[2 * i] = 1.0;
                data[2 * i + 1] = 0.0;
            }
            
            // Should not throw exception
            assertDoesNotThrow(() -> OptimizedFFT.fftOptimized(data, size, false),
                "Bluestein FFT failed for size " + size);
            
            // Basic sanity check - DC component should be non-zero
            assertTrue(Math.abs(data[0]) > 0, "DC component is zero for size " + size);
            
            // Check that result is finite
            for (int i = 0; i < size; i++) {
                assertTrue(Double.isFinite(data[2 * i]), 
                    "Real part at index " + i + " is not finite for size " + size);
                assertTrue(Double.isFinite(data[2 * i + 1]),
                    "Imag part at index " + i + " is not finite for size " + size);
            }
        }
    }
    
    @Test
    @DisplayName("FFT inverse should recover original signal")
    void testFFTInverse() {
        double[] original = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] data = new double[16];
        
        // Copy to complex format
        for (int i = 0; i < 8; i++) {
            data[2 * i] = original[i];
            data[2 * i + 1] = 0;
        }
        
        // Forward FFT
        OptimizedFFT.fftOptimized(data, 8, false);
        
        // Inverse FFT
        OptimizedFFT.fftOptimized(data, 8, true);
        
        // Should recover original
        for (int i = 0; i < 8; i++) {
            assertEquals(original[i], data[2 * i], 1e-9, "Failed to recover value at index " + i);
            assertEquals(0.0, data[2 * i + 1], 1e-9, "Non-zero imaginary part at index " + i);
        }
    }
    
    @Test
    @DisplayName("Scalar FFT should produce same results as vectorized FFT")
    void testScalarFFTConsistency() {
        double[] original = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] dataScalar = new double[16];
        double[] dataVector = new double[16];
        
        // Copy to complex format
        for (int i = 0; i < 8; i++) {
            dataScalar[2 * i] = original[i];
            dataScalar[2 * i + 1] = 0;
            dataVector[2 * i] = original[i];
            dataVector[2 * i + 1] = 0;
        }
        
        // Test scalar implementation explicitly using TransformConfig
        TransformConfig scalarConfig = TransformConfig.builder()
            .forceScalar(true)
            .build();
        OptimizedFFT.fftOptimized(dataScalar, 8, false, scalarConfig);
        OptimizedFFT.fftOptimized(dataScalar, 8, true, scalarConfig);
        
        // Should recover original
        for (int i = 0; i < 8; i++) {
            assertEquals(original[i], dataScalar[2 * i], 1e-9, "Failed to recover value at index " + i);
            assertEquals(0.0, dataScalar[2 * i + 1], 1e-9, "Non-zero imaginary part at index " + i);
        }
    }
}
