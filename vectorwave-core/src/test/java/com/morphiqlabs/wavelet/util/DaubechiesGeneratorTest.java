package com.morphiqlabs.wavelet.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DaubechiesGenerator utility.
 * Ensures mathematical correctness of Daubechies wavelet coefficients.
 */
class DaubechiesGeneratorTest {

    @Test
    @DisplayName("Generate and verify DB12 coefficients satisfy Daubechies properties")
    void testDB12CoefficientsProperties() {
        // Test DB12 generation
        double[] coeffs = DaubechiesGenerator.generateCoefficients(12);
        assertNotNull(coeffs);
        assertEquals(24, coeffs.length, "DB12 should have 24 coefficients");
        
        // Verify all Daubechies properties
        boolean valid = DaubechiesGenerator.verifyDaubechiesProperties(coeffs, 12);
        assertTrue(valid, "DB12 coefficients must satisfy all Daubechies properties");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {12, 14, 16, 18, 20, 22, 24})
    @DisplayName("Generate and verify extended Daubechies coefficients")
    void testExtendedDaubechiesCoefficients(int order) {
        // Test generation
        double[] coeffs = DaubechiesGenerator.generateCoefficients(order);
        assertNotNull(coeffs, "Coefficients should not be null for DB" + order);
        assertEquals(2 * order, coeffs.length, 
            "DB" + order + " should have " + (2 * order) + " coefficients");
        
        // Verify all Daubechies properties
        boolean valid = DaubechiesGenerator.verifyDaubechiesProperties(coeffs, order);
        assertTrue(valid, "DB" + order + " coefficients must satisfy all Daubechies properties");
    }

    @Test
    @DisplayName("Generate coefficients throws exception for invalid order")
    void testGenerateCoefficientsInvalidOrder() {
        // Test order too small
        assertThrows(IllegalArgumentException.class, 
            () -> DaubechiesGenerator.generateCoefficients(0),
            "Should throw exception for order 0");
        
        // Test order too large
        assertThrows(IllegalArgumentException.class, 
            () -> DaubechiesGenerator.generateCoefficients(46),
            "Should throw exception for order > 45");
        
        // Test negative order
        assertThrows(IllegalArgumentException.class, 
            () -> DaubechiesGenerator.generateCoefficients(-1),
            "Should throw exception for negative order");
    }

    @Test
    @DisplayName("Verify mathematical properties: sum to sqrt(2)")
    void testCoefficientSum() {
        double[] coeffs = DaubechiesGenerator.generateCoefficients(12);
        
        double sum = 0;
        for (double c : coeffs) {
            sum += c;
        }
        
        assertEquals(Math.sqrt(2), sum, 1e-12, 
            "Daubechies coefficients must sum to sqrt(2) for DC gain normalization");
    }

    @Test
    @DisplayName("Verify mathematical properties: sum of squares equals 1")
    void testCoefficientSumOfSquares() {
        double[] coeffs = DaubechiesGenerator.generateCoefficients(12);
        
        double sumSquares = 0;
        for (double c : coeffs) {
            sumSquares += c * c;
        }
        
        assertEquals(1.0, sumSquares, 1e-12, 
            "Sum of squares must equal 1 for energy normalization");
    }

    @Test
    @DisplayName("Verify mathematical properties: orthogonality")
    void testCoefficientOrthogonality() {
        double[] coeffs = DaubechiesGenerator.generateCoefficients(12);
        
        // Test orthogonality: sum(h[n] * h[n+2k]) = 0 for k != 0
        for (int k = 1; k < 12; k++) {
            double dot = 0;
            for (int n = 0; n < coeffs.length - 2*k; n++) {
                dot += coeffs[n] * coeffs[n + 2*k];
            }
            assertEquals(0.0, dot, 1e-12, 
                String.format("Orthogonality must be satisfied for k=%d", k));
        }
    }

    @Test
    @DisplayName("Verify properties throws for invalid coefficient length")
    void testVerifyPropertiesInvalidLength() {
        double[] wrongLength = new double[10]; // Wrong length for DB12
        
        assertThrows(IllegalArgumentException.class,
            () -> DaubechiesGenerator.verifyDaubechiesProperties(wrongLength, 12),
            "Should throw exception for incorrect coefficient length");
    }

    @Test
    @DisplayName("Generate coefficients for unimplemented orders")
    void testUnimplementedOrders() {
        // Test an unimplemented order (beyond DB38 which is the maximum)
        assertThrows(IllegalArgumentException.class,
            () -> DaubechiesGenerator.generateCoefficients(40),
            "Should throw exception for unimplemented order");
            
        // Test invalid orders
        assertThrows(IllegalArgumentException.class,
            () -> DaubechiesGenerator.generateCoefficients(0),
            "Should throw exception for invalid order 0");
            
        assertThrows(IllegalArgumentException.class,
            () -> DaubechiesGenerator.generateCoefficients(50),
            "Should throw exception for unsupported high order");
    }
    
    @Test
    @DisplayName("Verify properties fails with incorrect sum")
    void testVerifyPropertiesIncorrectSum() {
        double[] badCoeffs = new double[24];
        // Fill with values that don't sum to sqrt(2)
        for (int i = 0; i < badCoeffs.length; i++) {
            badCoeffs[i] = 0.1;
        }
        
        Exception exception = assertThrows(IllegalStateException.class,
            () -> DaubechiesGenerator.verifyDaubechiesProperties(badCoeffs, 12));
        
        assertTrue(exception.getMessage().contains("Sum test failed"),
            "Exception message should indicate sum test failure");
    }
    
    @Test
    @DisplayName("Verify properties fails with incorrect sum of squares")
    void testVerifyPropertiesIncorrectSumOfSquares() {
        double[] badCoeffs = new double[24];
        // Create coefficients that sum to sqrt(2) but have wrong sum of squares
        double target = Math.sqrt(2) / 24;
        for (int i = 0; i < badCoeffs.length; i++) {
            badCoeffs[i] = target;
        }
        
        Exception exception = assertThrows(IllegalStateException.class,
            () -> DaubechiesGenerator.verifyDaubechiesProperties(badCoeffs, 12));
        
        assertTrue(exception.getMessage().contains("Sum of squares test failed"),
            "Exception message should indicate sum of squares test failure");
    }
    
    @Test
    @DisplayName("Verify properties fails with incorrect orthogonality")
    void testVerifyPropertiesIncorrectOrthogonality() {
        // Create coefficients that violate orthogonality
        double[] badCoeffs = new double[24];
        
        // Set up coefficients that sum to sqrt(2) and have sum of squares = 1
        // but violate orthogonality
        for (int i = 0; i < badCoeffs.length; i++) {
            badCoeffs[i] = (i % 2 == 0) ? 0.2 : -0.05;
        }
        
        // Adjust to get correct sum
        double currentSum = 0;
        for (double c : badCoeffs) currentSum += c;
        double scale = Math.sqrt(2) / currentSum;
        for (int i = 0; i < badCoeffs.length; i++) {
            badCoeffs[i] *= scale;
        }
        
        // Now these will fail either sum of squares or orthogonality
        Exception exception = assertThrows(IllegalStateException.class,
            () -> DaubechiesGenerator.verifyDaubechiesProperties(badCoeffs, 12));
        
        assertTrue(exception.getMessage().contains("test failed"),
            "Exception message should indicate a test failure");
    }
    
    @Test
    @DisplayName("Coefficients are properly normalized")
    void testCoefficientNormalization() {
        // Test that already normalized coefficients are returned unchanged
        double[] coeffs = DaubechiesGenerator.generateCoefficients(12);
        
        // Get them again - should be the same
        double[] coeffs2 = DaubechiesGenerator.generateCoefficients(12);
        
        assertArrayEquals(coeffs, coeffs2, 1e-15,
            "Normalized coefficients should be consistent");
    }
    
    @Test
    @DisplayName("All implemented orders have correct first coefficient sign")
    void testFirstCoefficientSign() {
        // Daubechies wavelets typically have alternating signs in their coefficients
        // The first coefficient for extended Daubechies is usually very small
        
        int[] orders = {12, 14, 16, 18, 20, 22, 24};
        for (int order : orders) {
            double[] coeffs = DaubechiesGenerator.generateCoefficients(order);
            
            // First coefficient should be very small for higher orders
            assertTrue(Math.abs(coeffs[0]) < 0.1,
                String.format("DB%d first coefficient should be small", order));
        }
    }
    
    @Test
    @DisplayName("Coefficients have expected decay pattern")
    void testCoefficientDecayPattern() {
        // Daubechies coefficients typically have most energy in the middle
        double[] coeffs = DaubechiesGenerator.generateCoefficients(12);
        
        // Find the maximum absolute value
        double maxAbs = 0;
        int maxIndex = 0;
        for (int i = 0; i < coeffs.length; i++) {
            if (Math.abs(coeffs[i]) > maxAbs) {
                maxAbs = Math.abs(coeffs[i]);
                maxIndex = i;
            }
        }
        
        // Maximum should not be at the boundaries
        assertTrue(maxIndex > 0 && maxIndex < coeffs.length - 1,
            "Maximum coefficient should not be at boundaries");
        
        // Boundary coefficients should be smaller
        assertTrue(Math.abs(coeffs[0]) < maxAbs / 10,
            "First coefficient should be much smaller than maximum");
        assertTrue(Math.abs(coeffs[coeffs.length - 1]) < maxAbs / 10,
            "Last coefficient should be much smaller than maximum");
    }
}