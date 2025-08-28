package com.morphiqlabs.wavelet;

import com.morphiqlabs.wavelet.math.Complex;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Complex record.
 */
class ComplexTest {

    @Test
    @DisplayName("Complex constructor should create complex number with real and imaginary parts")
    void testComplexConstructor() {
        Complex complex = new Complex(3.0, 4.0);
        
        assertEquals(3.0, complex.real());
        assertEquals(4.0, complex.imaginary());
    }

    @Test
    @DisplayName("Complex.real() should create complex number with zero imaginary part")
    void testComplexReal() {
        Complex complex = Complex.real(5.0);
        
        assertEquals(5.0, complex.real());
        assertEquals(0.0, complex.imaginary());
    }

    @Test
    @DisplayName("Complex.imaginary() should create complex number with zero real part")
    void testComplexImaginary() {
        Complex complex = Complex.imaginary(7.0);
        
        assertEquals(0.0, complex.real());
        assertEquals(7.0, complex.imaginary());
    }

    @Test
    @DisplayName("magnitude() should calculate correct magnitude")
    void testMagnitude() {
        Complex complex = new Complex(3.0, 4.0);
        double magnitude = complex.magnitude();
        
        assertEquals(5.0, magnitude, 1e-10); // sqrt(3^2 + 4^2) = 5
    }

    @Test
    @DisplayName("magnitude() should handle zero values")
    void testMagnitudeWithZeroValues() {
        assertEquals(0.0, new Complex(0.0, 0.0).magnitude());
        assertEquals(3.0, new Complex(3.0, 0.0).magnitude());
        assertEquals(4.0, new Complex(0.0, 4.0).magnitude());
    }

    @Test
    @DisplayName("phase() should calculate correct phase angle")
    void testPhase() {
        // Test common angles
        assertEquals(0.0, new Complex(1.0, 0.0).phase(), 1e-10);
        assertEquals(Math.PI / 2, new Complex(0.0, 1.0).phase(), 1e-10);
        assertEquals(Math.PI, new Complex(-1.0, 0.0).phase(), 1e-10);
        assertEquals(-Math.PI / 2, new Complex(0.0, -1.0).phase(), 1e-10);
        
        // Test 45-degree angle
        assertEquals(Math.PI / 4, new Complex(1.0, 1.0).phase(), 1e-10);
    }

    @Test
    @DisplayName("toString() should format complex numbers correctly")
    void testToString() {
        // Real only
        assertEquals("5.000000", new Complex(5.0, 0.0).toString());
        
        // Imaginary only
        assertEquals("3.000000i", new Complex(0.0, 3.0).toString());
        
        // Both parts positive
        assertEquals("2.000000 + 3.000000i", new Complex(2.0, 3.0).toString());
        
        // Negative imaginary
        assertEquals("2.000000 - 3.000000i", new Complex(2.0, -3.0).toString());
        
        // Both negative
        assertEquals("-2.000000 - 3.000000i", new Complex(-2.0, -3.0).toString());
        
        // Zero
        assertEquals("0.000000", new Complex(0.0, 0.0).toString());
    }

    @Test
    @DisplayName("equals() should work correctly for record")
    void testEquals() {
        Complex complex1 = new Complex(1.0, 2.0);
        Complex complex2 = new Complex(1.0, 2.0);
        Complex complex3 = new Complex(1.0, 3.0);
        
        assertEquals(complex1, complex2);
        assertNotEquals(complex1, complex3);
        assertNotEquals(complex1, null);
        assertNotEquals(complex1, "not a complex number");
    }

    @Test
    @DisplayName("hashCode() should be consistent for equal objects")
    void testHashCode() {
        Complex complex1 = new Complex(1.0, 2.0);
        Complex complex2 = new Complex(1.0, 2.0);
        
        assertEquals(complex1.hashCode(), complex2.hashCode());
    }

    @Test
    @DisplayName("Complex should handle special double values")
    void testSpecialDoubleValues() {
        // Test with NaN
        Complex nanComplex = new Complex(Double.NaN, 1.0);
        assertTrue(Double.isNaN(nanComplex.real()));
        assertEquals(1.0, nanComplex.imaginary());
        
        // Test with infinity
        Complex infComplex = new Complex(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY);
        assertTrue(Double.isInfinite(infComplex.real()));
        assertTrue(Double.isInfinite(infComplex.imaginary()));
        
        // Test magnitude with special values
        assertTrue(Double.isNaN(nanComplex.magnitude()));
        assertTrue(Double.isInfinite(infComplex.magnitude()));
    }
}