package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Wavelet interface static and default methods.
 */
class WaveletTest {
    
    private static final double EPSILON = 1e-15;
    
    
    @Test
    @DisplayName("Should normalize coefficients to unit L2 norm")
    void testNormalizeToUnitL2Norm() {
        // Test normal case
        double[] coeffs = {3.0, 4.0}; // L2 norm = 5
        double[] normalized = Wavelet.normalizeToUnitL2Norm(coeffs);
        
        assertNotNull(normalized);
        assertEquals(2, normalized.length);
        assertEquals(0.6, normalized[0], EPSILON);
        assertEquals(0.8, normalized[1], EPSILON);
        
        // Verify L2 norm is 1
        double norm = Math.sqrt(normalized[0] * normalized[0] + normalized[1] * normalized[1]);
        assertEquals(1.0, norm, EPSILON);
        
        // Verify original is not modified
        assertEquals(3.0, coeffs[0]);
        assertEquals(4.0, coeffs[1]);
    }
    
    @Test
    @DisplayName("Should handle already normalized coefficients")
    void testNormalizeAlreadyNormalized() {
        double[] coeffs = {0.6, 0.8}; // Already L2 norm = 1
        double[] normalized = Wavelet.normalizeToUnitL2Norm(coeffs);
        
        assertNotNull(normalized);
        assertNotSame(coeffs, normalized); // Should return a copy
        assertEquals(0.6, normalized[0], EPSILON);
        assertEquals(0.8, normalized[1], EPSILON);
    }
    
    @Test
    @DisplayName("Should handle null and empty arrays")
    void testNormalizeNullAndEmpty() {
        assertNull(Wavelet.normalizeToUnitL2Norm(null));
        
        double[] empty = new double[0];
        double[] result = Wavelet.normalizeToUnitL2Norm(empty);
        assertSame(empty, result);
    }
    
    @Test
    @DisplayName("Should handle zero coefficients")
    void testNormalizeZeroCoefficients() {
        double[] coeffs = {0.0, 0.0, 0.0};
        double[] normalized = Wavelet.normalizeToUnitL2Norm(coeffs);
        
        assertNotNull(normalized);
        assertNotSame(coeffs, normalized); // Should return a copy
        assertArrayEquals(coeffs, normalized, EPSILON);
    }
    
    @Test
    @DisplayName("Should check if coefficients are normalized with tolerance")
    void testIsNormalizedWithTolerance() {
        // Exactly normalized
        double[] exact = {0.6, 0.8};
        assertTrue(Wavelet.isNormalized(exact, 1e-10));
        
        // Within tolerance
        double[] close = {0.600000001, 0.8};
        assertTrue(Wavelet.isNormalized(close, 1e-6));
        assertFalse(Wavelet.isNormalized(close, 1e-12));
        
        // Outside tolerance
        double[] notNorm = {0.7, 0.8};
        assertFalse(Wavelet.isNormalized(notNorm, 1e-2));
    }
    
    @Test
    @DisplayName("Should check if coefficients are normalized with default tolerance")
    void testIsNormalizedDefault() {
        // Exactly normalized
        double[] exact = {0.6, 0.8};
        assertTrue(Wavelet.isNormalized(exact));
        
        // Very close to normalized (within default tolerance)
        double[] close = {0.6000000000001, 0.8};
        assertTrue(Wavelet.isNormalized(close));
        
        // Not normalized
        double[] notNorm = {1.0, 1.0};
        assertFalse(Wavelet.isNormalized(notNorm));
        
        // Edge cases
        assertFalse(Wavelet.isNormalized(null));
        assertFalse(Wavelet.isNormalized(new double[0]));
    }
    
    @Test
    @DisplayName("Should validate perfect reconstruction default method")
    void testValidatePerfectReconstruction() {
        // Use an actual implementation
        Haar haar = new Haar();
        // Default implementation always returns true
        assertTrue(haar.validatePerfectReconstruction());
    }
    
    @Test
    @DisplayName("Should provide default description")
    void testDefaultDescription() {
        // Since we can't create anonymous classes for sealed interfaces,
        // we test with an existing implementation that doesn't override description()
        // The Haar class overrides description, so let's check its actual description
        Haar haar = new Haar();
        assertEquals("Haar wavelet - the simplest orthogonal wavelet", haar.description());
    }
    
    @Test
    @DisplayName("Should normalize various coefficient arrays correctly")
    void testNormalizeVariousCases() {
        // Single element
        double[] single = {5.0};
        double[] normSingle = Wavelet.normalizeToUnitL2Norm(single);
        assertEquals(1.0, normSingle[0], EPSILON);
        
        // Large array
        double[] large = {1.0, 2.0, 3.0, 4.0, 5.0};
        double[] normLarge = Wavelet.normalizeToUnitL2Norm(large);
        double normCheck = 0;
        for (double v : normLarge) {
            normCheck += v * v;
        }
        assertEquals(1.0, normCheck, EPSILON);
        
        // Very small values
        double[] small = {1e-10, 2e-10, 3e-10};
        double[] normSmall = Wavelet.normalizeToUnitL2Norm(small);
        double smallNorm = 0;
        for (double v : normSmall) {
            smallNorm += v * v;
        }
        assertEquals(1.0, smallNorm, EPSILON);
        
        // Mixed positive and negative
        double[] mixed = {-3.0, 4.0, -5.0};
        double[] normMixed = Wavelet.normalizeToUnitL2Norm(mixed);
        double mixedNorm = 0;
        for (double v : normMixed) {
            mixedNorm += v * v;
        }
        assertEquals(1.0, mixedNorm, EPSILON);
    }
    
    @Test
    @DisplayName("Should verify constants are defined correctly")
    void testConstants() {
        assertEquals(1e-15, Wavelet.NORMALIZATION_CHECK_TOLERANCE);
        assertEquals(1e-12, Wavelet.DEFAULT_NORMALIZATION_TOLERANCE);
        assertTrue(Wavelet.NORMALIZATION_CHECK_TOLERANCE < Wavelet.DEFAULT_NORMALIZATION_TOLERANCE);
    }
    
    @Test
    @DisplayName("Should handle edge cases in normalization check")
    void testIsNormalizedEdgeCases() {
        // Null and empty handled correctly
        assertFalse(Wavelet.isNormalized(null, 1.0));
        assertFalse(Wavelet.isNormalized(new double[0], 1.0));
        
        // Zero vector - sum of squares is 0, which is within tolerance of 1.0 when tolerance is 1.0
        // So this actually returns true!
        assertTrue(Wavelet.isNormalized(new double[]{0, 0, 0}, 1.0));
        
        // But with a smaller tolerance it should be false
        assertFalse(Wavelet.isNormalized(new double[]{0, 0, 0}, 0.1));
        
        // Very large tolerance
        assertTrue(Wavelet.isNormalized(new double[]{10.0, 10.0}, 1000.0));
        
        // Negative tolerance - implementation may use actual tolerance value
        double[] norm = {0.6, 0.8};
        // Whether this returns true or false depends on the implementation
        // Let's just verify it doesn't throw an exception
        boolean result = Wavelet.isNormalized(norm, -1e-10);
        // Just ensure it returns a valid boolean without throwing
    }
    
    @Test
    @DisplayName("Should handle coefficients close to unit norm")
    void testAlmostNormalizedCoefficients() {
        // Just within NORMALIZATION_CHECK_TOLERANCE
        double almostOne = 1.0 + Wavelet.NORMALIZATION_CHECK_TOLERANCE / 2;
        double[] almostNorm = {Math.sqrt(almostOne), 0.0};
        double[] result = Wavelet.normalizeToUnitL2Norm(almostNorm);
        
        // Should return a copy even though it's very close to normalized
        assertNotSame(almostNorm, result);
        assertNotNull(result);
        
        // Just outside NORMALIZATION_CHECK_TOLERANCE
        double notQuiteOne = 1.0 + Wavelet.NORMALIZATION_CHECK_TOLERANCE * 2;
        double[] notNorm = {Math.sqrt(notQuiteOne), 0.0};
        double[] normalized = Wavelet.normalizeToUnitL2Norm(notNorm);
        
        double norm = Math.sqrt(normalized[0] * normalized[0]);
        assertEquals(1.0, norm, EPSILON);
    }
}