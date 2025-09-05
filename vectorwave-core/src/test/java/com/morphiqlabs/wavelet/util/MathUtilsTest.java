package com.morphiqlabs.wavelet.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MathUtilsTest {
    
    private static final double DELTA = 1e-10;
    
    @Test
    void testSymmetricBoundaryExtension() {
        int signalLength = 4;
        
        // Test within bounds - should return unchanged
        assertEquals(0, MathUtils.symmetricBoundaryExtension(0, signalLength));
        assertEquals(1, MathUtils.symmetricBoundaryExtension(1, signalLength));
        assertEquals(2, MathUtils.symmetricBoundaryExtension(2, signalLength));
        assertEquals(3, MathUtils.symmetricBoundaryExtension(3, signalLength));
        
        // Test negative indices - should reflect
        assertEquals(0, MathUtils.symmetricBoundaryExtension(-1, signalLength));
        assertEquals(1, MathUtils.symmetricBoundaryExtension(-2, signalLength));
        assertEquals(2, MathUtils.symmetricBoundaryExtension(-3, signalLength));
        assertEquals(3, MathUtils.symmetricBoundaryExtension(-4, signalLength));
        
        // Test indices >= signalLength - should reflect
        assertEquals(3, MathUtils.symmetricBoundaryExtension(4, signalLength));
        assertEquals(2, MathUtils.symmetricBoundaryExtension(5, signalLength));
        assertEquals(1, MathUtils.symmetricBoundaryExtension(6, signalLength));
        assertEquals(0, MathUtils.symmetricBoundaryExtension(7, signalLength));
        
        // Test large negative indices (multiple reflections)
        assertEquals(0, MathUtils.symmetricBoundaryExtension(-8, signalLength));
        assertEquals(0, MathUtils.symmetricBoundaryExtension(-9, signalLength));
        
        // Test large positive indices (multiple reflections)
        assertEquals(0, MathUtils.symmetricBoundaryExtension(8, signalLength));
        assertEquals(0, MathUtils.symmetricBoundaryExtension(15, signalLength));
    }
    
    @Test
    void testSymmetricBoundaryExtensionEdgeCases() {
        // Test with signal length 1
        assertEquals(0, MathUtils.symmetricBoundaryExtension(0, 1));
        assertEquals(0, MathUtils.symmetricBoundaryExtension(-1, 1));
        assertEquals(0, MathUtils.symmetricBoundaryExtension(1, 1));
        assertEquals(0, MathUtils.symmetricBoundaryExtension(5, 1));
        
        // Test with signal length 2
        assertEquals(0, MathUtils.symmetricBoundaryExtension(0, 2));
        assertEquals(1, MathUtils.symmetricBoundaryExtension(1, 2));
        assertEquals(1, MathUtils.symmetricBoundaryExtension(2, 2));
        assertEquals(0, MathUtils.symmetricBoundaryExtension(3, 2));
    }
    
    @Test
    void testSymmetricBoundaryExtensionErrors() {
        // Test with invalid signal length
        assertThrows(IllegalArgumentException.class, 
            () -> MathUtils.symmetricBoundaryExtension(0, 0));
        assertThrows(IllegalArgumentException.class, 
            () -> MathUtils.symmetricBoundaryExtension(0, -1));
    }
    
    @Test
    void testQuickSelectBasic() {
        double[] arr = {3.0, 1.0, 4.0, 1.0, 5.0, 9.0, 2.0, 6.0};
        
        // Find minimum (k=0)
        assertEquals(1.0, MathUtils.quickSelect(arr.clone(), 0), DELTA);
        
        // Find maximum (k=n-1)
        assertEquals(9.0, MathUtils.quickSelect(arr.clone(), 7), DELTA);
        
        // Find median for even length
        assertEquals(3.0, MathUtils.quickSelect(arr.clone(), 3), DELTA);
        assertEquals(4.0, MathUtils.quickSelect(arr.clone(), 4), DELTA);
    }
    
    @Test
    void testQuickSelectEdgeCases() {
        // Single element
        double[] single = {42.0};
        assertEquals(42.0, MathUtils.quickSelect(single, 0), DELTA);
        
        // Two elements
        double[] two = {2.0, 1.0};
        assertEquals(1.0, MathUtils.quickSelect(two.clone(), 0), DELTA);
        assertEquals(2.0, MathUtils.quickSelect(two.clone(), 1), DELTA);
    }
    
    @Test
    void testQuickSelectErrors() {
        double[] arr = {1.0, 2.0, 3.0};
        
        // Null array
        assertThrows(IllegalArgumentException.class, 
            () -> MathUtils.quickSelect(null, 0));
        
        // Empty array
        assertThrows(IllegalArgumentException.class, 
            () -> MathUtils.quickSelect(new double[0], 0));
        
        // k out of bounds
        assertThrows(IllegalArgumentException.class, 
            () -> MathUtils.quickSelect(arr, -1));
        assertThrows(IllegalArgumentException.class, 
            () -> MathUtils.quickSelect(arr, 3));
    }
    
    @Test
    void testMedian() {
        // Odd length
        double[] odd = {3.0, 1.0, 4.0, 1.0, 5.0};
        assertEquals(3.0, MathUtils.median(odd.clone()), DELTA);
        
        // Even length
        double[] even = {3.0, 1.0, 4.0, 1.0, 5.0, 9.0};
        assertEquals(3.5, MathUtils.median(even.clone()), DELTA); // (3.0 + 4.0) / 2
        
        // Single element
        double[] single = {42.0};
        assertEquals(42.0, MathUtils.median(single), DELTA);
    }
    
    @Test
    void testMedianAbsoluteDeviation() {
        // Example: [1, 1, 2, 2, 4, 6, 9]
        // Median = 2
        // Absolute deviations: [1, 1, 0, 0, 2, 4, 7]
        // MAD = median of deviations = 1
        double[] data = {1.0, 1.0, 2.0, 2.0, 4.0, 6.0, 9.0};
        assertEquals(1.0, MathUtils.medianAbsoluteDeviation(data), DELTA);
        
        // Constant array should have MAD = 0
        double[] constant = {5.0, 5.0, 5.0, 5.0};
        assertEquals(0.0, MathUtils.medianAbsoluteDeviation(constant), DELTA);
    }
    
    @Test
    void testStandardDeviation() {
        // Known example: [2, 4, 4, 4, 5, 5, 7, 9]
        // Mean = 40/8 = 5
        // Sample variance = sum((x-mean)^2)/(n-1) = 30/7 ≈ 4.286
        // Sample SD = sqrt(30/7) ≈ 2.138
        double[] data = {2.0, 4.0, 4.0, 4.0, 5.0, 5.0, 7.0, 9.0};
        assertEquals(2.138089935299395, MathUtils.standardDeviation(data), 0.0001);
        
        // Constant array should have SD = 0
        double[] constant = {5.0, 5.0, 5.0};
        assertEquals(0.0, MathUtils.standardDeviation(constant), DELTA);
        
        // Two values
        double[] two = {1.0, 3.0};
        assertEquals(Math.sqrt(2.0), MathUtils.standardDeviation(two), DELTA);
    }
    
    @Test
    void testStandardDeviationErrors() {
        // Null array
        assertThrows(IllegalArgumentException.class, 
            () -> MathUtils.standardDeviation(null));
        
        // Too few elements
        assertThrows(IllegalArgumentException.class, 
            () -> MathUtils.standardDeviation(new double[0]));
        assertThrows(IllegalArgumentException.class, 
            () -> MathUtils.standardDeviation(new double[]{1.0}));
    }
}