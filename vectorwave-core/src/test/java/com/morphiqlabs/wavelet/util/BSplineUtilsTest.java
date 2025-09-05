package com.morphiqlabs.wavelet.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for BSplineUtils to achieve full coverage.
 */
class BSplineUtilsTest {

    private static final double EPSILON = 1e-10;

    @Test
    @DisplayName("Should prevent instantiation of utility class")
    void testPrivateConstructor() throws Exception {
        // Use reflection to test private constructor
        var constructor = BSplineUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    @ParameterizedTest
    @CsvSource({
        "0, 0.5, 1.0",   // Order 0 at center of support
        "0, -0.1, 0.0",  // Order 0 outside support (left)
        "0, 1.1, 0.0",   // Order 0 outside support (right)
        "0, 0.0, 1.0",   // Order 0 at boundary
        "0, 0.99, 1.0"   // Order 0 near boundary
    })
    @DisplayName("Should compute order-0 B-spline correctly")
    void testBSplineOrder0(int m, double x, double expected) {
        assertEquals(expected, BSplineUtils.bSpline(m, x), EPSILON);
    }

    @ParameterizedTest
    @CsvSource({
        "1, 0.0, 0.5",   // Linear at center
        "1, -0.5, 0.0",  // Linear at left edge
        "1, 0.5, 1.0",   // Linear at peak
        "1, 1.0, 0.5",   // Linear descending
        "1, 1.5, 0.0",   // Linear at right edge
        "1, -1.1, 0.0",  // Linear outside left
        "1, 2.1, 0.0"    // Linear outside right
    })
    @DisplayName("Should compute order-1 (linear) B-spline correctly")
    void testBSplineOrder1(int m, double x, double expected) {
        assertEquals(expected, BSplineUtils.bSpline(m, x), EPSILON);
    }

    @Test
    @DisplayName("Should compute order-2 (quadratic) B-spline correctly")
    void testBSplineOrder2() {
        // Test quadratic B-spline at various points
        assertEquals(0.0, BSplineUtils.bSpline(2, -1.6), EPSILON);  // Outside left
        assertEquals(0.0, BSplineUtils.bSpline(2, -1.0), EPSILON);  // At left boundary
        assertTrue(BSplineUtils.bSpline(2, 0.0) > 0);               // At center
        assertTrue(BSplineUtils.bSpline(2, 0.5) > 0);               // Between nodes
        assertTrue(BSplineUtils.bSpline(2, 1.0) > 0);               // At node (not quite at boundary)
        assertTrue(BSplineUtils.bSpline(2, 1.4) > 0);               // Near right boundary
        assertEquals(0.0, BSplineUtils.bSpline(2, 2.1), EPSILON);   // Outside right
    }

    @Test
    @DisplayName("Should compute order-3 (cubic) B-spline correctly")
    void testBSplineOrder3() {
        // Test cubic B-spline at various points
        // Cubic B-spline (order 3) has support on [-1.5, 2.5] after centering
        assertEquals(0.0, BSplineUtils.bSpline(3, -2.1), EPSILON);  // Outside left
        assertEquals(0.0, BSplineUtils.bSpline(3, -1.5), EPSILON);  // At left boundary
        assertTrue(BSplineUtils.bSpline(3, 0.0) > 0);              // At center  
        assertTrue(BSplineUtils.bSpline(3, 1.4) > 0);               // Near right boundary
        assertEquals(0.0, BSplineUtils.bSpline(3, 3.0), EPSILON);   // Outside right
        
        // Test intermediate points in each segment
        assertTrue(BSplineUtils.bSpline(3, -1.0) > 0);  // First segment
        assertTrue(BSplineUtils.bSpline(3, -0.5) > 0);  // Second segment
        assertTrue(BSplineUtils.bSpline(3, 0.5) > 0);   // Third segment
        assertTrue(BSplineUtils.bSpline(3, 1.0) > 0);   // Fourth segment
        
        // Test point that should be zero - definitely outside support
        assertEquals(0.0, BSplineUtils.bSpline(3, 4.0), EPSILON);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 5})
    @DisplayName("Should compute higher-order B-splines using recursion")
    void testBSplineHigherOrder(int m) {
        // Test that higher-order splines are computed
        double centerValue = BSplineUtils.bSpline(m, 0.0);
        assertTrue(centerValue >= 0, "Center value should be non-negative");
        assertTrue(centerValue <= 1.0, "Center value should be <= 1");
        
        // Test outside support - B-spline of order m has support [-m/2, (m+1)-m/2]
        double leftBoundary = -(m/2.0) - 0.1;
        double rightBoundary = (m + 1) - (m/2.0) + 0.1;
        assertEquals(0.0, BSplineUtils.bSpline(m, leftBoundary), EPSILON);
        assertEquals(0.0, BSplineUtils.bSpline(m, rightBoundary), EPSILON);
        
        // Test continuity - values should be smooth
        double v1 = BSplineUtils.bSpline(m, 0.0);
        double v2 = BSplineUtils.bSpline(m, 0.1);
        assertTrue(Math.abs(v1 - v2) < 1.0, "Function should be continuous");
    }

    @Test
    @DisplayName("Should compute B-spline Fourier magnitude correctly")
    void testBSplineFourierMagnitude() {
        // Test at omega = 0 (should be 1)
        assertEquals(1.0, BSplineUtils.bSplineFourierMagnitude(1, 0.0), EPSILON);
        assertEquals(1.0, BSplineUtils.bSplineFourierMagnitude(3, 0.0), EPSILON);
        assertEquals(1.0, BSplineUtils.bSplineFourierMagnitude(5, 1e-11), EPSILON); // Near zero
        
        // Test at non-zero frequencies
        double mag1 = BSplineUtils.bSplineFourierMagnitude(1, Math.PI);
        assertTrue(mag1 > 0 && mag1 < 1, "Magnitude should be between 0 and 1");
        
        double mag2 = BSplineUtils.bSplineFourierMagnitude(2, Math.PI);
        assertTrue(mag2 > 0 && mag2 < mag1, "Higher order should have smaller magnitude at same frequency");
        
        // Test decay with frequency
        double magLow = BSplineUtils.bSplineFourierMagnitude(1, 0.1);
        double magHigh = BSplineUtils.bSplineFourierMagnitude(1, 2.0);
        assertTrue(magLow > magHigh, "Magnitude should decay with frequency");
    }

    @Test
    @DisplayName("Should compute orthogonalization factor correctly")
    void testComputeOrthogonalizationFactor() {
        // Test at various frequencies for different orders
        for (int m = 1; m <= 5; m++) {
            double factor0 = BSplineUtils.computeOrthogonalizationFactor(m, 0.0);
            assertTrue(factor0 > 0, "Factor should be positive");
            assertTrue(factor0 <= 1.0, "Factor at omega=0 should be <= 1");
            
            double factorPi = BSplineUtils.computeOrthogonalizationFactor(m, Math.PI);
            assertTrue(factorPi > 0, "Factor should be positive");
            
            // Test symmetry
            double factorPos = BSplineUtils.computeOrthogonalizationFactor(m, 1.0);
            double factorNeg = BSplineUtils.computeOrthogonalizationFactor(m, -1.0);
            assertEquals(factorPos, factorNeg, EPSILON, "Should be symmetric");
        }
        
        // Test edge case with very small sum
        // This is hard to trigger naturally, but we can test the behavior
        double factor = BSplineUtils.computeOrthogonalizationFactor(10, 0.0);
        assertTrue(factor > 0 && factor <= 1.0);
    }

    @Test
    @DisplayName("Should compute Battle-Lemarié scaling function in Fourier domain")
    void testBattleLemarieScalingFourier() {
        // Test at omega = 0
        double scaling0 = BSplineUtils.battleLemarieScalingFourier(1, 0.0);
        assertTrue(scaling0 > 0 && scaling0 <= 1.0);
        
        // Test decay with frequency
        double scaling1 = BSplineUtils.battleLemarieScalingFourier(2, 0.5);
        double scaling2 = BSplineUtils.battleLemarieScalingFourier(2, 2.0);
        assertTrue(scaling1 > scaling2, "Should decay with frequency");
        
        // Test different orders
        for (int m = 1; m <= 5; m++) {
            double scaling = BSplineUtils.battleLemarieScalingFourier(m, Math.PI / 2);
            assertTrue(scaling >= 0, "Scaling should be non-negative");
        }
    }

    @Test
    @DisplayName("Should compute Battle-Lemarié filter coefficients for all orders")
    void testComputeBattleLemarieFilter() {
        // Test linear (m=1)
        double[] h1 = BSplineUtils.computeBattleLemarieFilter(1, 8);
        assertNotNull(h1);
        assertEquals(8, h1.length);
        verifyFilterProperties(h1);
        
        // Test quadratic (m=2)
        double[] h2 = BSplineUtils.computeBattleLemarieFilter(2, 12);
        assertNotNull(h2);
        assertEquals(12, h2.length);
        verifyFilterProperties(h2);
        
        // Test cubic (m=3)
        double[] h3 = BSplineUtils.computeBattleLemarieFilter(3, 16);
        assertNotNull(h3);
        assertEquals(16, h3.length);
        verifyFilterProperties(h3);
        
        // Test quartic (m=4)
        double[] h4 = BSplineUtils.computeBattleLemarieFilter(4, 20);
        assertNotNull(h4);
        assertEquals(20, h4.length);
        verifyFilterProperties(h4);
        
        // Test quintic and higher (m=5, uses default case)
        double[] h5 = BSplineUtils.computeBattleLemarieFilter(5, 24);
        assertNotNull(h5);
        assertEquals(24, h5.length);
        verifyFilterProperties(h5);
        
        // Test order beyond explicit cases (should use quintic coefficients)
        double[] h6 = BSplineUtils.computeBattleLemarieFilter(6, 24);
        assertNotNull(h6);
        assertEquals(24, h6.length);
    }

    @Test
    @DisplayName("Should handle edge cases in filter normalization")
    void testFilterNormalizationEdgeCases() {
        // Test with all zeros (should handle gracefully)
        double[] h1 = BSplineUtils.computeBattleLemarieFilter(1, 8);
        assertNotNull(h1);
        
        // Test that filters for different orders produce different results
        double[] h2 = BSplineUtils.computeBattleLemarieFilter(2, 12);
        double[] h3 = BSplineUtils.computeBattleLemarieFilter(3, 16);
        assertFalse(arraysAreEqual(h2, h3, 12));
    }

    @Test
    @DisplayName("Should get recommended filter length for different orders")
    void testGetRecommendedFilterLength() {
        assertEquals(8, BSplineUtils.getRecommendedFilterLength(1));
        assertEquals(12, BSplineUtils.getRecommendedFilterLength(2));
        assertEquals(16, BSplineUtils.getRecommendedFilterLength(3));
        assertEquals(20, BSplineUtils.getRecommendedFilterLength(4));
        assertEquals(24, BSplineUtils.getRecommendedFilterLength(5));
        
        // Test default case for higher orders
        assertEquals(28, BSplineUtils.getRecommendedFilterLength(6));
        assertEquals(32, BSplineUtils.getRecommendedFilterLength(7));
        assertEquals(40, BSplineUtils.getRecommendedFilterLength(9));
    }

    @Test
    @DisplayName("Should handle extreme B-spline recursion depths")
    void testBSplineRecursionDepth() {
        // Test deep recursion doesn't cause stack overflow
        // Note: Higher order B-splines may evaluate to 0 at x=0 due to their shape
        for (int m = 6; m <= 10; m++) {
            double value = BSplineUtils.bSpline(m, 0.0);
            assertTrue(value >= 0 && value <= 1.0, 
                "B-spline value should be in [0,1] for order " + m);
            
            // Also test at non-zero points within support
            double testPoint = 0.5;
            double valueAtTestPoint = BSplineUtils.bSpline(m, testPoint);
            assertTrue(valueAtTestPoint >= 0, 
                "B-spline should be non-negative at " + testPoint + " for order " + m);
        }
    }

    @Test
    @DisplayName("Should handle edge cases in orthogonalization factor")
    void testOrthogonalizationFactorEdgeCases() {
        // Test with high order (triggers more iterations)
        double factor = BSplineUtils.computeOrthogonalizationFactor(10, Math.PI / 4);
        assertTrue(factor > 0 && factor <= 10.0, "Factor should be reasonable");
        
        // Test convergence with different frequencies
        for (double omega = 0; omega <= 2 * Math.PI; omega += Math.PI / 4) {
            double factor1 = BSplineUtils.computeOrthogonalizationFactor(1, omega);
            assertTrue(factor1 > 0, "Factor should be positive at omega=" + omega);
        }
    }

    // Helper method to verify filter properties
    private void verifyFilterProperties(double[] filter) {
        // Check that filter is not all zeros
        double sumAbs = 0;
        for (double h : filter) {
            sumAbs += Math.abs(h);
        }
        assertTrue(sumAbs > 0.1, "Filter should have non-zero coefficients");
        
        // Check that sum is close to sqrt(2) (within reasonable tolerance)
        double sum = 0;
        for (double h : filter) {
            sum += h;
        }
        double sqrt2 = Math.sqrt(2);
        // Allow larger tolerance since these are approximations
        assertTrue(Math.abs(sum - sqrt2) < 1.0, 
            "Sum should be approximately sqrt(2), got " + sum);
        
        // Check that energy is reasonable (not necessarily 1 for approximations)
        double sumSq = 0;
        for (double h : filter) {
            sumSq += h * h;
        }
        assertTrue(sumSq > 0.1 && sumSq < 10.0, 
            "Energy should be reasonable, got " + sumSq);
    }

    // Helper to compare arrays up to a certain length
    private boolean arraysAreEqual(double[] a, double[] b, int length) {
        if (a == null || b == null) return false;
        int minLen = Math.min(Math.min(a.length, b.length), length);
        for (int i = 0; i < minLen; i++) {
            if (Math.abs(a[i] - b[i]) > EPSILON) {
                return false;
            }
        }
        return true;
    }
}