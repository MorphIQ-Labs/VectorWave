package com.morphiqlabs.wavelet.modwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for MutableMultiLevelMODWTResultImpl.
 */
class MutableMultiLevelMODWTResultTest {
    
    private static final double TOLERANCE = 1e-10;
    
    @Test
    @DisplayName("Constructor should validate parameters")
    void testConstructorValidation() {
        // Valid construction
        MutableMultiLevelMODWTResultImpl result = new MutableMultiLevelMODWTResultImpl(100, 3);
        assertEquals(100, result.getSignalLength());
        assertEquals(3, result.getLevels());
        
        // Invalid signal length
        assertThrows(IllegalArgumentException.class, () -> 
            new MutableMultiLevelMODWTResultImpl(0, 3),
            "Should reject zero signal length");
        
        assertThrows(IllegalArgumentException.class, () -> 
            new MutableMultiLevelMODWTResultImpl(-1, 3),
            "Should reject negative signal length");
        
        // Invalid levels
        assertThrows(IllegalArgumentException.class, () -> 
            new MutableMultiLevelMODWTResultImpl(100, 0),
            "Should reject zero levels");
        
        assertThrows(IllegalArgumentException.class, () -> 
            new MutableMultiLevelMODWTResultImpl(100, -1),
            "Should reject negative levels");
    }
    
    @Test
    @DisplayName("Copy constructor should create deep copy")
    void testCopyConstructor() {
        // Create original
        MutableMultiLevelMODWTResultImpl original = new MutableMultiLevelMODWTResultImpl(50, 2);
        original.setDetailCoeffs(1, new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                                                  11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
                                                  21, 22, 23, 24, 25, 26, 27, 28, 29, 30,
                                                  31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
                                                  41, 42, 43, 44, 45, 46, 47, 48, 49, 50});
        original.setDetailCoeffs(2, new double[50]);
        original.setApproximationCoeffs(new double[50]);
        
        // Create copy
        MutableMultiLevelMODWTResultImpl copy = new MutableMultiLevelMODWTResultImpl(original);
        
        // Verify structure
        assertEquals(original.getSignalLength(), copy.getSignalLength());
        assertEquals(original.getLevels(), copy.getLevels());
        
        // Verify independence
        double[] originalDetails = original.getMutableDetailCoeffs(1);
        double[] copyDetails = copy.getMutableDetailCoeffs(1);
        originalDetails[0] = 999;
        assertNotEquals(originalDetails[0], copyDetails[0],
            "Copy should be independent");
        
        // Null source
        assertThrows(NullPointerException.class, () -> 
            new MutableMultiLevelMODWTResultImpl(null),
            "Should reject null source");
    }
    
    @Test
    @DisplayName("Detail coefficient operations should work correctly")
    void testDetailCoefficientOperations() {
        MutableMultiLevelMODWTResultImpl result = new MutableMultiLevelMODWTResultImpl(10, 3);
        
        // Initially not set
        assertThrows(IllegalStateException.class, () -> 
            result.getDetailCoeffsAtLevel(1),
            "Should throw when coefficients not set");
        
        // Set coefficients
        double[] coeffs = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        result.setDetailCoeffs(1, coeffs);
        
        // Get immutable copy
        double[] retrieved = result.getDetailCoeffsAtLevel(1);
        assertArrayEquals(coeffs, retrieved, TOLERANCE);
        coeffs[0] = 999; // Modify original
        assertNotEquals(coeffs[0], retrieved[0], "Should return copy");
        
        // Get mutable reference
        double[] mutable = result.getMutableDetailCoeffs(1);
        assertEquals(1.0, mutable[0], TOLERANCE);
        mutable[0] = 42;
        assertEquals(42.0, result.getMutableDetailCoeffs(1)[0], TOLERANCE,
            "Changes should persist");
        
        // Level validation
        assertThrows(IllegalArgumentException.class, () -> 
            result.setDetailCoeffs(0, coeffs),
            "Should reject level 0");
        
        assertThrows(IllegalArgumentException.class, () -> 
            result.setDetailCoeffs(4, coeffs),
            "Should reject level > max");
        
        // Length validation
        assertThrows(IllegalArgumentException.class, () -> 
            result.setDetailCoeffs(1, new double[5]),
            "Should reject wrong length");
        
        // Null validation
        assertThrows(NullPointerException.class, () -> 
            result.setDetailCoeffs(1, null),
            "Should reject null coefficients");
    }
    
    @Test
    @DisplayName("Approximation coefficient operations should work correctly")
    void testApproximationCoefficientOperations() {
        MutableMultiLevelMODWTResultImpl result = new MutableMultiLevelMODWTResultImpl(10, 2);
        
        // Initially not set
        assertThrows(IllegalStateException.class, () -> 
            result.getApproximationCoeffs(),
            "Should throw when coefficients not set");
        
        // Set coefficients
        double[] coeffs = {10, 20, 30, 40, 50, 60, 70, 80, 90, 100};
        result.setApproximationCoeffs(coeffs);
        
        // Get immutable copy
        double[] retrieved = result.getApproximationCoeffs();
        assertArrayEquals(coeffs, retrieved, TOLERANCE);
        coeffs[0] = 999; // Modify original
        assertNotEquals(coeffs[0], retrieved[0], "Should return copy");
        
        // Get mutable reference
        double[] mutable = result.getMutableApproximationCoeffs();
        assertEquals(10.0, mutable[0], TOLERANCE);
        mutable[0] = 42;
        assertEquals(42.0, result.getMutableApproximationCoeffs()[0], TOLERANCE,
            "Changes should persist");
        
        // Length validation
        assertThrows(IllegalArgumentException.class, () -> 
            result.setApproximationCoeffs(new double[5]),
            "Should reject wrong length");
        
        // Null validation
        assertThrows(NullPointerException.class, () -> 
            result.setApproximationCoeffs(null),
            "Should reject null coefficients");
    }
    
    @Test
    @DisplayName("Energy calculations should be correct and cached")
    void testEnergyCalculations() {
        MutableMultiLevelMODWTResultImpl result = new MutableMultiLevelMODWTResultImpl(4, 2);
        
        // Set up coefficients
        result.setDetailCoeffs(1, new double[]{1, 2, 3, 4}); // Energy = 30
        result.setDetailCoeffs(2, new double[]{2, 2, 2, 2}); // Energy = 16
        result.setApproximationCoeffs(new double[]{3, 3, 3, 3}); // Energy = 36
        
        // Test individual energies
        assertEquals(30.0, result.getDetailEnergyAtLevel(1), TOLERANCE);
        assertEquals(16.0, result.getDetailEnergyAtLevel(2), TOLERANCE);
        assertEquals(36.0, result.getApproximationEnergy(), TOLERANCE);
        
        // Test total energy
        assertEquals(82.0, result.getTotalEnergy(), TOLERANCE);
        
        // Test relative distribution
        double[] distribution = result.getRelativeEnergyDistribution();
        assertEquals(3, distribution.length);
        assertEquals(30.0/82.0, distribution[0], TOLERANCE); // Level 1
        assertEquals(16.0/82.0, distribution[1], TOLERANCE); // Level 2
        assertEquals(36.0/82.0, distribution[2], TOLERANCE); // Approximation
        
        // Modify and verify cache clearing
        double[] details = result.getMutableDetailCoeffs(1);
        details[0] = 10; // Change from 1 to 10
        result.clearCaches();
        
        double newEnergy = result.getDetailEnergyAtLevel(1);
        assertEquals(30 - 1 + 100, newEnergy, TOLERANCE,
            "Energy should be recalculated after cache clear");
    }
    
    @Test
    @DisplayName("Direct setter methods should avoid cloning")
    void testDirectSetters() {
        MutableMultiLevelMODWTResultImpl result = new MutableMultiLevelMODWTResultImpl(10, 2);
        
        // Use direct setters
        double[] coeffs = new double[10];
        result.setDetailCoeffsAtLevelDirect(1, coeffs);
        result.setApproximationCoeffsDirect(coeffs);
        
        // Verify same reference (no cloning)
        assertSame(coeffs, result.getMutableDetailCoeffs(1));
        assertSame(coeffs, result.getMutableApproximationCoeffs());
        
        // Level validation still applies
        assertThrows(IllegalArgumentException.class, () -> 
            result.setDetailCoeffsAtLevelDirect(0, coeffs),
            "Should still validate level");
    }
    
    @Test
    @DisplayName("Validation should detect invalid states")
    void testValidation() {
        MutableMultiLevelMODWTResultImpl result = new MutableMultiLevelMODWTResultImpl(5, 2);
        
        // Initially invalid (no coefficients set)
        assertTrue(result.isValid(), "Empty result should be valid");
        
        // Set valid coefficients
        result.setDetailCoeffs(1, new double[]{1, 2, 3, 4, 5});
        result.setDetailCoeffs(2, new double[]{1, 2, 3, 4, 5});
        result.setApproximationCoeffs(new double[]{1, 2, 3, 4, 5});
        assertTrue(result.isValid());
        
        // Introduce NaN
        result.getMutableDetailCoeffs(1)[0] = Double.NaN;
        assertFalse(result.isValid(), "NaN should make result invalid");
        
        // Fix NaN
        result.getMutableDetailCoeffs(1)[0] = 1.0;
        assertTrue(result.isValid());
        
        // Introduce Infinity
        result.getMutableApproximationCoeffs()[2] = Double.POSITIVE_INFINITY;
        assertFalse(result.isValid(), "Infinity should make result invalid");
    }
    
    @Test
    @DisplayName("Immutable conversion should create independent copy")
    void testToImmutable() {
        MutableMultiLevelMODWTResultImpl mutable = new MutableMultiLevelMODWTResultImpl(8, 2);
        mutable.setDetailCoeffs(1, new double[]{1, 2, 3, 4, 5, 6, 7, 8});
        mutable.setDetailCoeffs(2, new double[]{8, 7, 6, 5, 4, 3, 2, 1});
        mutable.setApproximationCoeffs(new double[]{1, 1, 1, 1, 1, 1, 1, 1});
        
        // Convert to immutable
        MultiLevelMODWTResult immutable = mutable.toImmutable();
        
        // Verify structure
        assertEquals(mutable.getSignalLength(), immutable.getSignalLength());
        assertEquals(mutable.getLevels(), immutable.getLevels());
        
        // Verify independence
        mutable.getMutableDetailCoeffs(1)[0] = 999;
        assertNotEquals(999, immutable.getDetailCoeffsAtLevel(1)[0],
            "Immutable should be independent");
        
        // Verify immutable is truly immutable (returns copies)
        double[] coeffs = immutable.getDetailCoeffsAtLevel(1);
        coeffs[0] = 888;
        assertNotEquals(888, immutable.getDetailCoeffsAtLevel(1)[0],
            "Immutable should return copies");
    }
    
    @Test
    @DisplayName("Thresholding should work correctly")
    void testThresholding() {
        MutableMultiLevelMODWTResultImpl result = new MutableMultiLevelMODWTResultImpl(10, 2);
        result.setDetailCoeffs(1, new double[]{-2, -1, -0.5, -0.1, 0, 0.1, 0.5, 1, 2, 3});
        result.setDetailCoeffs(2, new double[]{-3, -2, -1, 0, 1, 2, 3, 4, 5, 6});
        result.setApproximationCoeffs(new double[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        
        // Hard thresholding on level 1
        result.applyThreshold(1, 0.5, false);
        double[] level1 = result.getDetailCoeffsAtLevel(1);
        assertEquals(0, level1[2], TOLERANCE); // -0.5 -> 0
        assertEquals(0, level1[3], TOLERANCE); // -0.1 -> 0
        assertEquals(-2, level1[0], TOLERANCE); // -2 unchanged
        assertEquals(1, level1[7], TOLERANCE); // 1 unchanged
        
        // Soft thresholding on level 2
        result.applyThreshold(2, 2.0, true);
        double[] level2 = result.getDetailCoeffsAtLevel(2);
        assertEquals(0, level2[1], TOLERANCE); // -2 -> 0
        assertEquals(0, level2[2], TOLERANCE); // -1 -> 0
        assertEquals(-1, level2[0], TOLERANCE); // -3 -> -1
        assertEquals(2, level2[7], TOLERANCE); // 4 -> 2
        
        // Threshold approximation
        result.applyThreshold(0, 5.0, false);
        double[] approx = result.getApproximationCoeffs();
        assertEquals(0, approx[0], TOLERANCE); // 1 -> 0
        assertEquals(0, approx[3], TOLERANCE); // 4 -> 0
        assertEquals(6, approx[5], TOLERANCE); // 6 unchanged
        assertEquals(10, approx[9], TOLERANCE); // 10 unchanged
    }
    
    @Test
    @DisplayName("Copy method should create mutable copy")
    void testCopyMethod() {
        MutableMultiLevelMODWTResultImpl original = new MutableMultiLevelMODWTResultImpl(6, 1);
        original.setDetailCoeffs(1, new double[]{1, 2, 3, 4, 5, 6});
        original.setApproximationCoeffs(new double[]{6, 5, 4, 3, 2, 1});
        
        // Create copy
        MultiLevelMODWTResult copy = original.copy();
        
        // Verify it's also mutable
        assertTrue(copy instanceof MutableMultiLevelMODWTResult);
        
        // Verify independence
        original.getMutableDetailCoeffs(1)[0] = 999;
        assertNotEquals(999, copy.getDetailCoeffsAtLevel(1)[0]);
    }
    
    @Test
    @DisplayName("Energy for unset coefficients should be zero")
    void testEnergyForUnsetCoefficients() {
        MutableMultiLevelMODWTResultImpl result = new MutableMultiLevelMODWTResultImpl(10, 3);
        
        // Only set level 2
        result.setDetailCoeffs(2, new double[10]);
        
        // Energy for unset levels should be 0
        assertEquals(0.0, result.getDetailEnergyAtLevel(1), TOLERANCE);
        assertEquals(0.0, result.getDetailEnergyAtLevel(3), TOLERANCE);
        assertEquals(0.0, result.getApproximationEnergy(), TOLERANCE);
    }
}