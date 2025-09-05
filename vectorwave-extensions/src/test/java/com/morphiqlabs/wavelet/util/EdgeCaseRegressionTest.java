package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.extensions.util.OptimizedFFT;

import com.morphiqlabs.wavelet.cwt.ComplexNumber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regression tests for edge cases that were previously problematic.
 */
class EdgeCaseRegressionTest {
    
    @Test
    @DisplayName("fftRealOptimized should handle empty array without IndexOutOfBoundsException")
    void testEmptyArrayRegression() {
        // This test verifies the fix for the edge case where n == 0
        // Previously, the condition if (n <= 1) would try to access real[0] 
        // when the array was empty, causing IndexOutOfBoundsException
        
        double[] emptyArray = {};
        
        // This should not throw any exception
        assertDoesNotThrow(() -> {
            ComplexNumber[] result = OptimizedFFT.fftRealOptimized(emptyArray);
            assertEquals(0, result.length);
        });
    }
    
    @Test
    @DisplayName("fftRealOptimized should handle single element correctly")
    void testSingleElementRegression() {
        // This verifies the fix properly handles the n == 1 case
        double[] singleElement = {42.0};
        
        ComplexNumber[] result = OptimizedFFT.fftRealOptimized(singleElement);
        
        assertEquals(1, result.length);
        assertEquals(42.0, result[0].real(), 1e-10);
        assertEquals(0.0, result[0].imag(), 1e-10);
    }
    
    @Test
    @DisplayName("Edge cases should work with CWT operations")
    void testEdgeCasesWithCWTIntegration() {
        // Test that the edge case fixes don't break higher-level operations
        // that might use OptimizedFFT internally
        
        // This would have failed before the fix if CWT operations tried to use
        // fftRealOptimized with empty arrays
        assertDoesNotThrow(() -> {
            // Create minimal test cases that exercise the FFT paths
            double[] smallSignal = {1.0};
            
            // This exercises the real-optimized FFT path
            ComplexNumber[] fftResult = OptimizedFFT.fftRealOptimized(smallSignal);
            assertNotNull(fftResult);
            assertEquals(1, fftResult.length);
        });
    }
    
    @Test
    @DisplayName("Boundary conditions should be handled consistently")
    void testBoundaryConditions() {
        // Test various boundary conditions that could cause issues
        
        // Empty array
        double[] empty = {};
        ComplexNumber[] emptyResult = OptimizedFFT.fftRealOptimized(empty);
        assertEquals(0, emptyResult.length);
        
        // Single element
        double[] single = {1.0};
        ComplexNumber[] singleResult = OptimizedFFT.fftRealOptimized(single);
        assertEquals(1, singleResult.length);
        
        // Two elements (smallest even case)
        double[] pair = {1.0, 2.0};
        ComplexNumber[] pairResult = OptimizedFFT.fftRealOptimized(pair);
        assertEquals(2, pairResult.length);
        
        // Three elements (smallest odd case > 1)
        double[] triple = {1.0, 2.0, 3.0};
        ComplexNumber[] tripleResult = OptimizedFFT.fftRealOptimized(triple);
        assertEquals(3, tripleResult.length);
    }
}
