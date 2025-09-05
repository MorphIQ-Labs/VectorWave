package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.extensions.util.OptimizedFFT;

import com.morphiqlabs.wavelet.cwt.ComplexNumber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify array bounds correctness in the fftRealOptimized implementation.
 * 
 * <p>This test analyzes the bounds of array accesses in OptimizedFFT.fftRealOptimized
 * to ensure that the packed array indexing is correct and doesn't cause out-of-bounds
 * exceptions. The fftRealOptimized method uses a packed array where even-indexed
 * real values are stored in the real parts and odd-indexed real values are stored
 * in the imaginary parts of a half-size complex array.</p>
 */
class FFTBoundsAnalysisTest {
    
    @Test
    @DisplayName("Verify array bounds in fftRealOptimized implementation")
    void testArrayBoundsInActualImplementation() {
        // Test with various even-sized arrays to check bounds
        int[] testSizes = {2, 4, 8, 16, 32, 64, 128};
        
        for (int n : testSizes) {
            double[] real = new double[n];
            
            // Fill with test data
            for (int i = 0; i < n; i++) {
                real[i] = Math.sin(2 * Math.PI * i / n);
            }
            
            // This should not throw any bounds exceptions
            assertDoesNotThrow(() -> {
                ComplexNumber[] result = OptimizedFFT.fftRealOptimized(real);
                assertEquals(n, result.length, 
                    "Result length should match input length for size " + n);
                
                // Verify the result is valid (non-null elements)
                for (int i = 0; i < n; i++) {
                    assertNotNull(result[i], 
                        "Result element at index " + i + " should not be null for size " + n);
                }
            }, "fftRealOptimized failed for size " + n);
        }
    }
    
    /**
     * Helper method to calculate packed array indices for FFT operations.
     * 
     * @param k the loop index
     * @param halfN half the array size
     * @param useHalfNMinusK whether to use (halfN - k) instead of k
     * @param addOne whether to add 1 to the final index (for imaginary parts)
     * @return the calculated index
     */
    private int calculateIndex(int k, int halfN, boolean useHalfNMinusK, boolean addOne) {
        int base = useHalfNMinusK ? (halfN - k) : k;
        int index = 2 * base;
        return addOne ? index + 1 : index;
    }
    
    @Test
    @DisplayName("Analyze theoretical bounds of packed array indexing in fftRealOptimized")
    void analyzePackedArrayIndexing() {
        // This test analyzes the theoretical bounds of the packed array indexing
        // as implemented in OptimizedFFT.fftRealOptimized
        
        int[] testSizes = {4, 8, 16, 32};
        
        for (int n : testSizes) {
            int halfN = n / 2;
            int packedArrayLength = 2 * halfN; // Complex array has 2 * halfN doubles
            
            // Verify the relationship between n, halfN, and packed array length
            assertEquals(n, 2 * halfN, "n should equal 2 * halfN");
            assertEquals(n, packedArrayLength, "Packed array length should equal n");
            
            // The loop must use k < halfN (not k <= halfN) to avoid out-of-bounds access.
            // When k = halfN, the calculation of indices like 2 * halfN would exceed
            // the bounds of the packed array, leading to incorrect behavior.
            
            // Verify array bounds for the actual loop indices
            int validIndicesCount = 0;
            for (int k = 1; k < halfN; k++) {
                // Use helper method to calculate indices
                int idx1 = calculateIndex(k, halfN, false, false);  // packed[2 * k]
                int idx2 = calculateIndex(k, halfN, true, false);   // packed[2 * (halfN - k)]
                int idx3 = calculateIndex(k, halfN, false, true);   // packed[2 * k + 1]
                int idx4 = calculateIndex(k, halfN, true, true);    // packed[2 * (halfN - k) + 1]
                
                // All indices must be within [0, packedArrayLength)
                assertTrue(idx1 >= 0 && idx1 < packedArrayLength, 
                    String.format("For n=%d, k=%d: packed[2*k=%d] out of bounds", n, k, idx1));
                assertTrue(idx2 >= 0 && idx2 < packedArrayLength, 
                    String.format("For n=%d, k=%d: packed[2*(halfN-k)=%d] out of bounds", n, k, idx2));
                assertTrue(idx3 >= 0 && idx3 < packedArrayLength, 
                    String.format("For n=%d, k=%d: packed[2*k+1=%d] out of bounds", n, k, idx3));
                assertTrue(idx4 >= 0 && idx4 < packedArrayLength, 
                    String.format("For n=%d, k=%d: packed[2*(halfN-k)+1=%d] out of bounds", n, k, idx4));
                
                // Verify we never access the same indices (except at center for odd halfN)
                if (k != halfN - k) {
                    assertNotEquals(idx1, idx2, 
                        String.format("For n=%d, k=%d: Should not access same real part", n, k));
                    assertNotEquals(idx3, idx4, 
                        String.format("For n=%d, k=%d: Should not access same imaginary part", n, k));
                }
                validIndicesCount++;
            }
            
            // Verify the loop iterates the expected number of times
            assertEquals(halfN - 1, validIndicesCount, 
                String.format("For n=%d: Loop should iterate %d times", n, halfN - 1));
            
            // Demonstrate why k = halfN would be problematic
            if (halfN > 0) {
                int problematicK = halfN;
                int problematicIdx = 2 * (halfN - problematicK); // = 2 * 0 = 0
                int expectedIdx = 2 * halfN; // What we would want to access
                
                // Assert the problematic condition
                assertEquals(0, problematicIdx, 
                    String.format("For n=%d: k=halfN would incorrectly compute index 0", n));
                assertEquals(n, expectedIdx, 
                    String.format("For n=%d: k=halfN would need to access index %d (out of bounds)", n, expectedIdx));
                
                // Verify that using k=halfN would be out of bounds
                assertTrue(expectedIdx >= packedArrayLength,
                    String.format("For n=%d: k=halfN would require accessing index %d which is >= array length %d", 
                        n, expectedIdx, packedArrayLength));
            }
        }
    }
    
    @Test
    @DisplayName("Test edge cases for fftRealOptimized")
    void testEdgeCases() {
        // Test empty array
        assertDoesNotThrow(() -> {
            ComplexNumber[] result = OptimizedFFT.fftRealOptimized(new double[0]);
            assertEquals(0, result.length);
        });
        
        // Test single element
        assertDoesNotThrow(() -> {
            ComplexNumber[] result = OptimizedFFT.fftRealOptimized(new double[]{1.0});
            assertEquals(1, result.length);
            assertEquals(1.0, result[0].real());
            assertEquals(0.0, result[0].imag());
        });
        
        // Test odd-length arrays (should fall back to standard FFT)
        int[] oddSizes = {3, 5, 7, 9, 11};
        for (int n : oddSizes) {
            double[] real = new double[n];
            for (int i = 0; i < n; i++) {
                real[i] = i + 1.0; // Simple test data
            }
            
            assertDoesNotThrow(() -> {
                ComplexNumber[] result = OptimizedFFT.fftRealOptimized(real);
                assertEquals(n, result.length);
            }, "fftRealOptimized failed for odd size " + n);
        }
    }
}
