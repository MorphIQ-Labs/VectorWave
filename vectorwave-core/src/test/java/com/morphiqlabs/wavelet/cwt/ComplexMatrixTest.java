package com.morphiqlabs.wavelet.cwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ComplexMatrix to achieve full coverage.
 */
class ComplexMatrixTest {

    private ComplexMatrix matrix;
    private static final double EPSILON = 1e-10;

    @BeforeEach
    void setUp() {
        matrix = new ComplexMatrix(3, 4);
    }

    @Test
    @DisplayName("Should create matrix with valid dimensions")
    void testValidConstruction() {
        ComplexMatrix m = new ComplexMatrix(5, 10);
        
        assertNotNull(m);
        assertEquals(5, m.getRows());
        assertEquals(10, m.getCols());
    }

    @Test
    @DisplayName("Should create matrix from real and imaginary arrays")
    void testConstructionFromArrays() {
        double[][] real = {
            {1.0, 2.0, 3.0},
            {4.0, 5.0, 6.0}
        };
        double[][] imaginary = {
            {0.1, 0.2, 0.3},
            {0.4, 0.5, 0.6}
        };
        
        ComplexMatrix m = new ComplexMatrix(real, imaginary);
        
        assertEquals(2, m.getRows());
        assertEquals(3, m.getCols());
        assertEquals(1.0, m.getReal(0, 0), EPSILON);
        assertEquals(0.1, m.getImaginary(0, 0), EPSILON);
        assertEquals(6.0, m.getReal(1, 2), EPSILON);
        assertEquals(0.6, m.getImaginary(1, 2), EPSILON);
    }

    @Test
    @DisplayName("Should deep copy arrays in constructor")
    void testDeepCopyInConstructor() {
        double[][] real = {{1.0, 2.0}, {3.0, 4.0}};
        double[][] imaginary = {{0.1, 0.2}, {0.3, 0.4}};
        
        ComplexMatrix m = new ComplexMatrix(real, imaginary);
        
        // Modify original arrays
        real[0][0] = 999.0;
        imaginary[0][0] = 999.0;
        
        // Matrix should not be affected
        assertEquals(1.0, m.getReal(0, 0), EPSILON);
        assertEquals(0.1, m.getImaginary(0, 0), EPSILON);
    }

    @ParameterizedTest
    @CsvSource({
        "0, 5",
        "-1, 5",
        "5, 0",
        "5, -1",
        "-1, -1"
    })
    @DisplayName("Should throw exception for invalid dimensions")
    void testInvalidDimensions(int rows, int cols) {
        assertThrows(IllegalArgumentException.class, () -> {
            new ComplexMatrix(rows, cols);
        });
    }

    @Test
    @DisplayName("Should throw exception for null arrays")
    void testNullArrays() {
        double[][] real = {{1.0}};
        double[][] imaginary = {{1.0}};
        
        assertThrows(IllegalArgumentException.class, () -> {
            new ComplexMatrix(null, imaginary);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            new ComplexMatrix(real, null);
        });
    }

    @Test
    @DisplayName("Should throw exception for empty arrays")
    void testEmptyArrays() {
        double[][] empty = new double[0][0];
        double[][] real = {{1.0}};
        
        assertThrows(IllegalArgumentException.class, () -> {
            new ComplexMatrix(empty, real);
        });
        
        double[][] emptyCol = new double[1][0];
        assertThrows(IllegalArgumentException.class, () -> {
            new ComplexMatrix(emptyCol, emptyCol);
        });
    }

    @Test
    @DisplayName("Should throw exception for mismatched dimensions")
    void testMismatchedDimensions() {
        double[][] real = {{1.0, 2.0}, {3.0, 4.0}};
        double[][] imaginary = {{1.0}, {2.0}};
        
        assertThrows(IllegalArgumentException.class, () -> {
            new ComplexMatrix(real, imaginary);
        });
        
        double[][] differentRows = {{1.0, 2.0}};
        assertThrows(IllegalArgumentException.class, () -> {
            new ComplexMatrix(real, differentRows);
        });
    }

    @Test
    @DisplayName("Should set and get complex values")
    void testSetAndGet() {
        matrix.set(0, 0, 3.0, 4.0);
        matrix.set(1, 2, -2.5, 1.5);
        matrix.set(2, 3, 0.0, -1.0);
        
        assertEquals(3.0, matrix.getReal(0, 0), EPSILON);
        assertEquals(4.0, matrix.getImaginary(0, 0), EPSILON);
        
        assertEquals(-2.5, matrix.getReal(1, 2), EPSILON);
        assertEquals(1.5, matrix.getImaginary(1, 2), EPSILON);
        
        assertEquals(0.0, matrix.getReal(2, 3), EPSILON);
        assertEquals(-1.0, matrix.getImaginary(2, 3), EPSILON);
    }

    @Test
    @DisplayName("Should calculate magnitude correctly")
    void testGetMagnitude() {
        matrix.set(0, 0, 3.0, 4.0);
        matrix.set(1, 1, 5.0, 12.0);
        matrix.set(2, 2, 0.0, 0.0);
        
        assertEquals(5.0, matrix.getMagnitude(0, 0), EPSILON); // sqrt(3² + 4²) = 5
        assertEquals(13.0, matrix.getMagnitude(1, 1), EPSILON); // sqrt(5² + 12²) = 13
        assertEquals(0.0, matrix.getMagnitude(2, 2), EPSILON);
    }

    @Test
    @DisplayName("Should calculate phase correctly")
    void testGetPhase() {
        matrix.set(0, 0, 1.0, 1.0);
        matrix.set(1, 1, 1.0, 0.0);
        matrix.set(2, 2, 0.0, 1.0);
        matrix.set(0, 1, -1.0, 1.0);
        matrix.set(1, 2, -1.0, -1.0);
        
        assertEquals(Math.PI / 4, matrix.getPhase(0, 0), EPSILON); // 45 degrees
        assertEquals(0.0, matrix.getPhase(1, 1), EPSILON); // 0 degrees
        assertEquals(Math.PI / 2, matrix.getPhase(2, 2), EPSILON); // 90 degrees
        assertEquals(3 * Math.PI / 4, matrix.getPhase(0, 1), EPSILON); // 135 degrees
        assertEquals(-3 * Math.PI / 4, matrix.getPhase(1, 2), EPSILON); // -135 degrees
    }

    @Test
    @DisplayName("Should return full real matrix copy")
    void testGetRealMatrix() {
        matrix.set(0, 0, 1.0, 0.1);
        matrix.set(1, 1, 2.0, 0.2);
        matrix.set(2, 3, 3.0, 0.3);
        
        double[][] real = matrix.getReal();
        
        assertEquals(3, real.length);
        assertEquals(4, real[0].length);
        assertEquals(1.0, real[0][0], EPSILON);
        assertEquals(2.0, real[1][1], EPSILON);
        assertEquals(3.0, real[2][3], EPSILON);
        
        // Verify it's a copy
        real[0][0] = 999.0;
        assertEquals(1.0, matrix.getReal(0, 0), EPSILON);
    }

    @Test
    @DisplayName("Should return full imaginary matrix copy")
    void testGetImaginaryMatrix() {
        matrix.set(0, 0, 1.0, 0.1);
        matrix.set(1, 1, 2.0, 0.2);
        matrix.set(2, 3, 3.0, 0.3);
        
        double[][] imaginary = matrix.getImaginary();
        
        assertEquals(3, imaginary.length);
        assertEquals(4, imaginary[0].length);
        assertEquals(0.1, imaginary[0][0], EPSILON);
        assertEquals(0.2, imaginary[1][1], EPSILON);
        assertEquals(0.3, imaginary[2][3], EPSILON);
        
        // Verify it's a copy
        imaginary[0][0] = 999.0;
        assertEquals(0.1, matrix.getImaginary(0, 0), EPSILON);
    }

    @Test
    @DisplayName("Should return full magnitude matrix")
    void testGetMagnitudeMatrix() {
        matrix.set(0, 0, 3.0, 4.0);
        matrix.set(1, 1, 5.0, 12.0);
        matrix.set(2, 2, 8.0, 15.0);
        
        double[][] magnitude = matrix.getMagnitude();
        
        assertEquals(3, magnitude.length);
        assertEquals(4, magnitude[0].length);
        assertEquals(5.0, magnitude[0][0], EPSILON);
        assertEquals(13.0, magnitude[1][1], EPSILON);
        assertEquals(17.0, magnitude[2][2], EPSILON);
        assertEquals(0.0, magnitude[0][1], EPSILON); // Unset element
    }

    @Test
    @DisplayName("Should return full phase matrix")
    void testGetPhaseMatrix() {
        matrix.set(0, 0, 1.0, 1.0);
        matrix.set(1, 1, 1.0, 0.0);
        matrix.set(2, 2, 0.0, 1.0);
        
        double[][] phase = matrix.getPhase();
        
        assertEquals(3, phase.length);
        assertEquals(4, phase[0].length);
        assertEquals(Math.PI / 4, phase[0][0], EPSILON);
        assertEquals(0.0, phase[1][1], EPSILON);
        assertEquals(Math.PI / 2, phase[2][2], EPSILON);
    }

    @Test
    @DisplayName("Should validate row indices")
    void testRowIndexValidation() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            matrix.getReal(-1, 0);
        });
        
        assertThrows(IndexOutOfBoundsException.class, () -> {
            matrix.getReal(3, 0);
        });
        
        assertThrows(IndexOutOfBoundsException.class, () -> {
            matrix.set(-1, 0, 1.0, 0.0);
        });
        
        assertThrows(IndexOutOfBoundsException.class, () -> {
            matrix.getMagnitude(3, 0);
        });
    }

    @Test
    @DisplayName("Should validate column indices")
    void testColumnIndexValidation() {
        assertThrows(IndexOutOfBoundsException.class, () -> {
            matrix.getReal(0, -1);
        });
        
        assertThrows(IndexOutOfBoundsException.class, () -> {
            matrix.getReal(0, 4);
        });
        
        assertThrows(IndexOutOfBoundsException.class, () -> {
            matrix.set(0, -1, 1.0, 0.0);
        });
        
        assertThrows(IndexOutOfBoundsException.class, () -> {
            matrix.getPhase(0, 4);
        });
    }

    @Test
    @DisplayName("Should handle edge cases for magnitude and phase")
    void testEdgeCases() {
        // Zero complex number
        matrix.set(0, 0, 0.0, 0.0);
        assertEquals(0.0, matrix.getMagnitude(0, 0), EPSILON);
        assertEquals(0.0, matrix.getPhase(0, 0), EPSILON);
        
        // Very large numbers
        matrix.set(1, 1, 1e10, 1e10);
        assertEquals(Math.sqrt(2e20), matrix.getMagnitude(1, 1), 1e5);
        assertEquals(Math.PI / 4, matrix.getPhase(1, 1), EPSILON);
        
        // Very small numbers
        matrix.set(2, 2, 1e-10, 1e-10);
        assertEquals(Math.sqrt(2e-20), matrix.getMagnitude(2, 2), EPSILON);
        assertEquals(Math.PI / 4, matrix.getPhase(2, 2), EPSILON);
    }

    @Test
    @DisplayName("Should handle complex numbers in all quadrants")
    void testAllQuadrants() {
        // First quadrant
        matrix.set(0, 0, 1.0, 1.0);
        assertTrue(matrix.getPhase(0, 0) > 0 && matrix.getPhase(0, 0) < Math.PI / 2);
        
        // Second quadrant
        matrix.set(0, 1, -1.0, 1.0);
        assertTrue(matrix.getPhase(0, 1) > Math.PI / 2 && matrix.getPhase(0, 1) < Math.PI);
        
        // Third quadrant
        matrix.set(0, 2, -1.0, -1.0);
        assertTrue(matrix.getPhase(0, 2) > -Math.PI && matrix.getPhase(0, 2) < -Math.PI / 2);
        
        // Fourth quadrant
        matrix.set(0, 3, 1.0, -1.0);
        assertTrue(matrix.getPhase(0, 3) > -Math.PI / 2 && matrix.getPhase(0, 3) < 0);
    }

    @Test
    @DisplayName("Should work with large matrices")
    void testLargeMatrix() {
        ComplexMatrix large = new ComplexMatrix(100, 200);
        
        assertEquals(100, large.getRows());
        assertEquals(200, large.getCols());
        
        // Set and get corner elements
        large.set(0, 0, 1.0, 2.0);
        large.set(99, 199, 3.0, 4.0);
        
        assertEquals(1.0, large.getReal(0, 0), EPSILON);
        assertEquals(2.0, large.getImaginary(0, 0), EPSILON);
        assertEquals(3.0, large.getReal(99, 199), EPSILON);
        assertEquals(4.0, large.getImaginary(99, 199), EPSILON);
        
        // Test bulk operations
        double[][] magnitude = large.getMagnitude();
        assertEquals(100, magnitude.length);
        assertEquals(200, magnitude[0].length);
    }
}