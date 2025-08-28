package com.morphiqlabs.wavelet.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for DaubechiesCoefficients utility.
 */
class DaubechiesCoefficientsTest {

    @ParameterizedTest
    @ValueSource(ints = {2, 4, 6, 8, 10})
    @DisplayName("Get coefficients for supported orders")
    void testGetCoefficientsForSupportedOrders(int order) {
        double[] coeffs = DaubechiesCoefficients.getCoefficients(order);
        assertNotNull(coeffs, "Coefficients should not be null for DB" + order);
        assertEquals(2 * order, coeffs.length, "DB" + order + " should have " + (2 * order) + " coefficients");
        
        // Verify basic properties
        double sum = 0;
        double sumSquares = 0;
        for (double c : coeffs) {
            sum += c;
            sumSquares += c * c;
        }
        
        assertEquals(Math.sqrt(2), sum, 1e-10, "Coefficients should sum to sqrt(2)");
        assertEquals(1.0, sumSquares, 1e-10, "Sum of squares should equal 1");
    }

    @Test
    @DisplayName("Get coefficients for unsupported order throws exception")
    void testGetCoefficientsForUnsupportedOrder() {
        assertThrows(IllegalArgumentException.class,
            () -> DaubechiesCoefficients.getCoefficients(12),
            "Should throw exception for unsupported order 12");
        
        assertThrows(IllegalArgumentException.class,
            () -> DaubechiesCoefficients.getCoefficients(3),
            "Should throw exception for odd order 3");
        
        assertThrows(IllegalArgumentException.class,
            () -> DaubechiesCoefficients.getCoefficients(0),
            "Should throw exception for order 0");
        
        assertThrows(IllegalArgumentException.class,
            () -> DaubechiesCoefficients.getCoefficients(-2),
            "Should throw exception for negative order");
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 4, 6, 8, 10})
    @DisplayName("Check if order is supported")
    void testIsSupportedForValidOrders(int order) {
        assertTrue(DaubechiesCoefficients.isSupported(order),
            "DB" + order + " should be supported");
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 5, 7, 9, 11, 12, 14, 16, 20, 30, 45})
    @DisplayName("Check if order is not supported")
    void testIsSupportedForInvalidOrders(int order) {
        assertFalse(DaubechiesCoefficients.isSupported(order),
            "DB" + order + " should not be supported");
    }

    @Test
    @DisplayName("Get supported orders")
    void testGetSupportedOrders() {
        int[] supportedOrders = DaubechiesCoefficients.getSupportedOrders();
        assertNotNull(supportedOrders, "Supported orders should not be null");
        assertArrayEquals(new int[]{2, 4, 6, 8, 10}, supportedOrders,
            "Should return correct supported orders");
    }

    @Test
    @DisplayName("Verify DB2 coefficients match expected values")
    void testDB2Coefficients() {
        double[] coeffs = DaubechiesCoefficients.getCoefficients(2);
        assertEquals(4, coeffs.length);
        
        // Verify first coefficient is approximately correct
        assertEquals(0.4829629131445341, coeffs[0], 1e-10,
            "First DB2 coefficient should match expected value");
    }

    @Test
    @DisplayName("Verify DB4 coefficients match expected values")
    void testDB4Coefficients() {
        double[] coeffs = DaubechiesCoefficients.getCoefficients(4);
        assertEquals(8, coeffs.length);
        
        // Verify first coefficient is approximately correct
        assertEquals(0.2303778133088964, coeffs[0], 1e-10,
            "First DB4 coefficient should match expected value");
    }

    @Test
    @DisplayName("Verify DB10 coefficients match expected values")
    void testDB10Coefficients() {
        double[] coeffs = DaubechiesCoefficients.getCoefficients(10);
        assertEquals(20, coeffs.length);
        
        // Verify first coefficient is approximately correct
        assertEquals(0.0266700579005546, coeffs[0], 1e-10,
            "First DB10 coefficient should match expected value");
    }

    @Test
    @DisplayName("Coefficients are immutable")
    void testCoefficientsAreImmutable() {
        double[] coeffs1 = DaubechiesCoefficients.getCoefficients(2);
        double[] coeffs2 = DaubechiesCoefficients.getCoefficients(2);
        
        // Modify the first array
        coeffs1[0] = 999.0;
        
        // Second array should not be affected
        assertNotEquals(coeffs1[0], coeffs2[0],
            "Modifying returned array should not affect internal coefficients");
        assertEquals(0.4829629131445341, coeffs2[0], 1e-10,
            "Original coefficient value should be preserved");
    }
    
    @Test
    @DisplayName("Test all supported Daubechies orders have correct mathematical properties")
    void testAllSupportedOrdersProperties() {
        int[] supportedOrders = DaubechiesCoefficients.getSupportedOrders();
        
        for (int order : supportedOrders) {
            double[] coeffs = DaubechiesCoefficients.getCoefficients(order);
            
            // Verify filter length
            assertEquals(2 * order, coeffs.length,
                String.format("DB%d should have %d coefficients", order, 2 * order));
            
            // Verify sum to sqrt(2)
            double sum = 0;
            for (double c : coeffs) {
                sum += c;
            }
            assertEquals(Math.sqrt(2), sum, 1e-10,
                String.format("DB%d coefficients should sum to sqrt(2)", order));
            
            // Verify sum of squares equals 1
            double sumSquares = 0;
            for (double c : coeffs) {
                sumSquares += c * c;
            }
            assertEquals(1.0, sumSquares, 1e-10,
                String.format("DB%d sum of squares should equal 1", order));
            
            // Verify orthogonality
            for (int k = 1; k < order; k++) {
                double dot = 0;
                for (int n = 0; n < coeffs.length - 2*k; n++) {
                    dot += coeffs[n] * coeffs[n + 2*k];
                }
                assertEquals(0.0, dot, 1e-10,
                    String.format("DB%d orthogonality must be satisfied for k=%d", order, k));
            }
        }
    }
    
    @Test
    @DisplayName("Coefficients maintain precision across multiple accesses")
    void testCoefficientsPrecision() {
        // Test that coefficients maintain their precision when accessed multiple times
        double[] firstAccess = DaubechiesCoefficients.getCoefficients(4);
        double[] secondAccess = DaubechiesCoefficients.getCoefficients(4);
        double[] thirdAccess = DaubechiesCoefficients.getCoefficients(4);
        
        // All accesses should return identical values
        assertArrayEquals(firstAccess, secondAccess, 0.0,
            "Coefficients should be identical on repeated access");
        assertArrayEquals(secondAccess, thirdAccess, 0.0,
            "Coefficients should be identical on repeated access");
        
        // Verify specific coefficient precision
        assertEquals(0.2303778133088964, firstAccess[0], 0.0,
            "DB4 first coefficient should maintain exact precision");
    }
    
    @Test
    @DisplayName("Test coefficients for different orders are distinct")
    void testDifferentOrdersHaveDifferentCoefficients() {
        double[] db2 = DaubechiesCoefficients.getCoefficients(2);
        double[] db4 = DaubechiesCoefficients.getCoefficients(4);
        double[] db6 = DaubechiesCoefficients.getCoefficients(6);
        
        // Different orders should have different lengths
        assertNotEquals(db2.length, db4.length);
        assertNotEquals(db4.length, db6.length);
        
        // First coefficients should be different
        assertNotEquals(db2[0], db4[0]);
        assertNotEquals(db4[0], db6[0]);
    }
    
    @Test
    @DisplayName("Verify coefficients method validates all mathematical properties")
    void testVerifyCoefficientsMethod() {
        // Test with valid coefficients
        double[] db2 = DaubechiesCoefficients.getCoefficients(2);
        assertTrue(DaubechiesCoefficients.verifyCoefficients(db2, 2),
            "Valid DB2 coefficients should pass verification");
        
        double[] db4 = DaubechiesCoefficients.getCoefficients(4);
        assertTrue(DaubechiesCoefficients.verifyCoefficients(db4, 4),
            "Valid DB4 coefficients should pass verification");
        
        // Test with wrong length
        double[] wrongLength = new double[5];
        assertFalse(DaubechiesCoefficients.verifyCoefficients(wrongLength, 2),
            "Coefficients with wrong length should fail verification");
        
        // Test with incorrect sum
        double[] badSum = new double[4];
        for (int i = 0; i < 4; i++) {
            badSum[i] = 0.25; // Sum = 1.0 instead of sqrt(2)
        }
        assertFalse(DaubechiesCoefficients.verifyCoefficients(badSum, 2),
            "Coefficients with incorrect sum should fail verification");
        
        // Test with incorrect sum of squares
        double[] badSumSquares = new double[4];
        double target = Math.sqrt(2) / 4;
        for (int i = 0; i < 4; i++) {
            badSumSquares[i] = target; // Sum = sqrt(2) but sum of squares != 1
        }
        assertFalse(DaubechiesCoefficients.verifyCoefficients(badSumSquares, 2),
            "Coefficients with incorrect sum of squares should fail verification");
        
        // Test with broken orthogonality
        double[] badOrthogonal = new double[]{
            0.5, 0.5, 0.5, Math.sqrt(2) - 1.5
        };
        // Adjust to have correct sum of squares
        double sumSq = 0;
        for (double c : badOrthogonal) sumSq += c * c;
        double scale = 1.0 / Math.sqrt(sumSq);
        for (int i = 0; i < badOrthogonal.length; i++) {
            badOrthogonal[i] *= scale;
        }
        // Now adjust to have correct sum
        double currentSum = 0;
        for (double c : badOrthogonal) currentSum += c;
        double sumScale = Math.sqrt(2) / currentSum;
        for (int i = 0; i < badOrthogonal.length; i++) {
            badOrthogonal[i] *= sumScale;
        }
        // This will fail either orthogonality or sum of squares
        assertFalse(DaubechiesCoefficients.verifyCoefficients(badOrthogonal, 2),
            "Coefficients violating orthogonality should fail verification");
    }
    
    @Test
    @DisplayName("Verify coefficients handles edge cases")
    void testVerifyCoefficientsEdgeCases() {
        // Test with null array
        assertFalse(DaubechiesCoefficients.verifyCoefficients(null, 2),
            "Null coefficients should fail verification");
        
        // Test with empty array
        assertFalse(DaubechiesCoefficients.verifyCoefficients(new double[0], 2),
            "Empty coefficients should fail verification");
        
        // Test with negative order
        double[] coeffs = DaubechiesCoefficients.getCoefficients(2);
        assertFalse(DaubechiesCoefficients.verifyCoefficients(coeffs, -1),
            "Negative order should fail verification");
        
        // Test with zero order
        assertFalse(DaubechiesCoefficients.verifyCoefficients(new double[0], 0),
            "Zero order with empty array should fail verification");
        
        // Test mismatched order
        assertFalse(DaubechiesCoefficients.verifyCoefficients(coeffs, 3),
            "Mismatched order should fail verification");
    }
}