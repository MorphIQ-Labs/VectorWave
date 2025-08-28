package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Haar wavelet to achieve full coverage.
 */
class HaarTest {
    
    private static final double EPSILON = 1e-15;
    private static final double SQRT2 = Math.sqrt(2);
    private static final double SQRT2_INV = 1.0 / Math.sqrt(2);
    
    private Haar haar;
    
    @BeforeEach
    void setUp() {
        haar = new Haar();
    }
    
    @Test
    @DisplayName("Should have correct name")
    void testName() {
        assertEquals("Haar", haar.name());
    }
    
    @Test
    @DisplayName("Should have correct description")
    void testDescription() {
        assertEquals("Haar wavelet - the simplest orthogonal wavelet", haar.description());
    }
    
    @Test
    @DisplayName("Should return correct low-pass decomposition coefficients")
    void testLowPassDecomposition() {
        double[] coeffs = haar.lowPassDecomposition();
        
        assertNotNull(coeffs);
        assertEquals(2, coeffs.length);
        assertEquals(SQRT2_INV, coeffs[0], EPSILON);
        assertEquals(SQRT2_INV, coeffs[1], EPSILON);
        
        // Verify it returns a copy, not the original array
        double[] coeffs2 = haar.lowPassDecomposition();
        assertNotSame(coeffs, coeffs2);
        coeffs[0] = 999.0;
        assertEquals(SQRT2_INV, haar.lowPassDecomposition()[0], EPSILON);
    }
    
    @Test
    @DisplayName("Should return correct high-pass decomposition coefficients")
    void testHighPassDecomposition() {
        double[] coeffs = haar.highPassDecomposition();
        
        assertNotNull(coeffs);
        assertEquals(2, coeffs.length);
        assertEquals(SQRT2_INV, coeffs[0], EPSILON);
        assertEquals(-SQRT2_INV, coeffs[1], EPSILON);
        
        // Verify it returns a copy, not the original array
        double[] coeffs2 = haar.highPassDecomposition();
        assertNotSame(coeffs, coeffs2);
        coeffs[0] = 999.0;
        assertEquals(SQRT2_INV, haar.highPassDecomposition()[0], EPSILON);
    }
    
    @Test
    @DisplayName("Should have correct vanishing moments")
    void testVanishingMoments() {
        assertEquals(1, haar.vanishingMoments());
    }
    
    @Test
    @DisplayName("Should verify coefficients correctly")
    void testVerifyCoefficients() {
        assertTrue(haar.verifyCoefficients());
    }
    
    @Test
    @DisplayName("Should satisfy mathematical properties")
    void testMathematicalProperties() {
        double[] lowPass = haar.lowPassDecomposition();
        double[] highPass = haar.highPassDecomposition();
        
        // Property 1: Sum of low-pass coefficients equals sqrt(2)
        double sumLowPass = lowPass[0] + lowPass[1];
        assertEquals(SQRT2, sumLowPass, EPSILON);
        
        // Property 2: Sum of squares of low-pass coefficients equals 1 (normalization)
        double sumSquaresLowPass = lowPass[0] * lowPass[0] + lowPass[1] * lowPass[1];
        assertEquals(1.0, sumSquaresLowPass, EPSILON);
        
        // Property 3: Sum of squares of high-pass coefficients equals 1 (normalization)
        double sumSquaresHighPass = highPass[0] * highPass[0] + highPass[1] * highPass[1];
        assertEquals(1.0, sumSquaresHighPass, EPSILON);
        
        // Property 4: Orthogonality between low-pass and high-pass filters
        double dotProduct = lowPass[0] * highPass[0] + lowPass[1] * highPass[1];
        assertEquals(0.0, dotProduct, EPSILON);
        
        // Property 5: High-pass sum should be 0 (high-pass nature)
        double sumHighPass = highPass[0] + highPass[1];
        assertEquals(0.0, sumHighPass, EPSILON);
    }
    
    @Test
    @DisplayName("Should have static instance available")
    void testStaticInstance() {
        assertNotNull(Haar.INSTANCE);
        assertEquals("Haar", Haar.INSTANCE.name());
        assertTrue(Haar.INSTANCE.verifyCoefficients());
    }
    
    @Test
    @DisplayName("Should implement OrthogonalWavelet interface")
    void testImplementsOrthogonalWavelet() {
        assertTrue(haar instanceof OrthogonalWavelet);
        assertTrue(haar instanceof DiscreteWavelet);
        assertTrue(haar instanceof Wavelet);
    }
    
    @Test
    @DisplayName("Should be a record with proper equality")
    void testRecordEquality() {
        Haar haar1 = new Haar();
        Haar haar2 = new Haar();
        
        assertEquals(haar1, haar2);
        assertEquals(haar1.hashCode(), haar2.hashCode());
        assertEquals(haar1.toString(), haar2.toString());
    }
    
    @Test
    @DisplayName("Should maintain coefficient precision")
    void testCoefficientPrecision() {
        // Test that coefficients maintain high precision
        double[] lowPass = haar.lowPassDecomposition();
        
        // Verify exact value matches expected constant (with appropriate tolerance)
        double expected = 0.7071067811865476; // Value from documentation
        assertEquals(expected, lowPass[0], 1e-15);
        assertEquals(expected, lowPass[1], 1e-15);
        
        // Verify mathematical relationship
        assertEquals(1.0 / Math.sqrt(2), lowPass[0], EPSILON);
    }
    
    @Test
    @DisplayName("Should verify coefficient properties internally")
    void testInternalCoefficientVerification() {
        // This tests the verifyCoefficients method more thoroughly
        // by ensuring all three checks in the method are exercised
        
        Haar haarInstance = new Haar();
        
        // The verifyCoefficients method checks:
        // 1. Sum equals sqrt(2)
        // 2. Sum of squares equals 1
        // 3. Orthogonality (dot product = 0)
        
        // All should pass for correctly implemented Haar
        assertTrue(haarInstance.verifyCoefficients());
        
        // Create multiple instances to ensure consistency
        for (int i = 0; i < 10; i++) {
            assertTrue(new Haar().verifyCoefficients());
        }
    }
    
    @Test
    @DisplayName("Should work correctly with wavelet operations")
    void testWaveletOperations() {
        // Test that the Haar wavelet can be used as a Wavelet
        Wavelet wavelet = haar;
        
        assertEquals("Haar", wavelet.name());
        assertEquals("Haar wavelet - the simplest orthogonal wavelet", wavelet.description());
        
        // Test as DiscreteWavelet
        DiscreteWavelet discreteWavelet = haar;
        assertEquals(1, discreteWavelet.vanishingMoments());
        
        // Test as OrthogonalWavelet
        OrthogonalWavelet orthogonalWavelet = haar;
        assertArrayEquals(haar.lowPassDecomposition(), orthogonalWavelet.lowPassDecomposition());
        assertArrayEquals(haar.highPassDecomposition(), orthogonalWavelet.highPassDecomposition());
    }
}