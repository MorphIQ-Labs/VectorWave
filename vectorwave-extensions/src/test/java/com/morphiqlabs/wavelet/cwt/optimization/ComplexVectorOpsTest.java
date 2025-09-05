package com.morphiqlabs.wavelet.extensions.cwt.optimization;

import com.morphiqlabs.wavelet.cwt.ComplexMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for ComplexVectorOps.
 * Tests SIMD-optimized complex number operations for CWT computations.
 */
class ComplexVectorOpsTest {
    
    private static final double EPSILON = 1e-12;
    private final ComplexVectorOps ops = new ComplexVectorOps();
    
    // ==========================================
    // Complex Multiplication Tests
    // ==========================================
    
    @Test
    @DisplayName("Test complex multiplication with small arrays (scalar path)")
    void testComplexMultiplyScalar() {
        double[] real1 = {1.0, 2.0, 3.0};
        double[] imag1 = {1.0, 0.0, -1.0};
        double[] real2 = {2.0, 1.0, 0.0};
        double[] imag2 = {0.0, 1.0, 2.0};
        double[] resultReal = new double[3];
        double[] resultImag = new double[3];
        
        ops.complexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
        
        // (1+i) * (2+0i) = 2+2i
        assertEquals(2.0, resultReal[0], EPSILON);
        assertEquals(2.0, resultImag[0], EPSILON);
        
        // (2+0i) * (1+i) = 2+2i
        assertEquals(2.0, resultReal[1], EPSILON);
        assertEquals(2.0, resultImag[1], EPSILON);
        
        // (3-i) * (0+2i) = 2+6i
        assertEquals(2.0, resultReal[2], EPSILON);
        assertEquals(6.0, resultImag[2], EPSILON);
    }
    
    @Test
    @DisplayName("Test complex multiplication with large arrays (vector path)")
    void testComplexMultiplyVector() {
        int size = 128;
        double[] real1 = new double[size];
        double[] imag1 = new double[size];
        double[] real2 = new double[size];
        double[] imag2 = new double[size];
        double[] resultReal = new double[size];
        double[] resultImag = new double[size];
        
        // Initialize with test data
        for (int i = 0; i < size; i++) {
            real1[i] = Math.cos(2 * Math.PI * i / 16);
            imag1[i] = Math.sin(2 * Math.PI * i / 16);
            real2[i] = Math.cos(2 * Math.PI * i / 32);
            imag2[i] = Math.sin(2 * Math.PI * i / 32);
        }
        
        ops.complexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
        
        // Verify results are valid
        for (int i = 0; i < size; i++) {
            assertFalse(Double.isNaN(resultReal[i]));
            assertFalse(Double.isInfinite(resultReal[i]));
            assertFalse(Double.isNaN(resultImag[i]));
            assertFalse(Double.isInfinite(resultImag[i]));
        }
        
        // Spot check: first element
        double expectedReal = real1[0] * real2[0] - imag1[0] * imag2[0];
        double expectedImag = real1[0] * imag2[0] + imag1[0] * real2[0];
        assertEquals(expectedReal, resultReal[0], EPSILON);
        assertEquals(expectedImag, resultImag[0], EPSILON);
    }
    
    @Test
    @DisplayName("Test complex multiplication with unit complex numbers")
    void testComplexMultiplyUnitComplex() {
        double[] real1 = {1.0, 0.0, -1.0, 0.0};
        double[] imag1 = {0.0, 1.0, 0.0, -1.0};
        double[] real2 = {0.0, 1.0, 0.0, -1.0};
        double[] imag2 = {1.0, 0.0, -1.0, 0.0};
        double[] resultReal = new double[4];
        double[] resultImag = new double[4];
        
        ops.complexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
        
        // 1 * i = i
        assertEquals(0.0, resultReal[0], EPSILON);
        assertEquals(1.0, resultImag[0], EPSILON);
        
        // i * 1 = i
        assertEquals(0.0, resultReal[1], EPSILON);
        assertEquals(1.0, resultImag[1], EPSILON);
        
        // -1 * (-i) = i
        assertEquals(0.0, resultReal[2], EPSILON);
        assertEquals(1.0, resultImag[2], EPSILON);
        
        // -i * (-1) = i
        assertEquals(0.0, resultReal[3], EPSILON);
        assertEquals(1.0, resultImag[3], EPSILON);
    }
    
    // ==========================================
    // Complex Scalar Multiplication Tests
    // ==========================================
    
    @Test
    @DisplayName("Test complex scalar multiplication small arrays")
    void testComplexScalarMultiplySmall() {
        double[] real = {1.0, 2.0, 0.0};
        double[] imag = {0.0, 1.0, -1.0};
        double scalarReal = 2.0;
        double scalarImag = 1.0;
        double[] resultReal = new double[3];
        double[] resultImag = new double[3];
        
        ops.complexScalarMultiply(real, imag, scalarReal, scalarImag, resultReal, resultImag);
        
        // (1+0i) * (2+i) = 2+i
        assertEquals(2.0, resultReal[0], EPSILON);
        assertEquals(1.0, resultImag[0], EPSILON);
        
        // (2+i) * (2+i) = 3+4i
        assertEquals(3.0, resultReal[1], EPSILON);
        assertEquals(4.0, resultImag[1], EPSILON);
        
        // (0-i) * (2+i) = 1-2i
        assertEquals(1.0, resultReal[2], EPSILON);
        assertEquals(-2.0, resultImag[2], EPSILON);
    }
    
    @Test
    @DisplayName("Test complex scalar multiplication large arrays")
    void testComplexScalarMultiplyLarge() {
        int size = 256;
        double[] real = new double[size];
        double[] imag = new double[size];
        double[] resultReal = new double[size];
        double[] resultImag = new double[size];
        
        for (int i = 0; i < size; i++) {
            real[i] = i + 1.0;
            imag[i] = (i % 2 == 0) ? 1.0 : -1.0;
        }
        
        double scalarReal = 0.5;
        double scalarImag = 0.5;
        
        ops.complexScalarMultiply(real, imag, scalarReal, scalarImag, resultReal, resultImag);
        
        // Verify results
        for (int i = 0; i < size; i++) {
            double expectedReal = real[i] * scalarReal - imag[i] * scalarImag;
            double expectedImag = real[i] * scalarImag + imag[i] * scalarReal;
            assertEquals(expectedReal, resultReal[i], EPSILON);
            assertEquals(expectedImag, resultImag[i], EPSILON);
        }
    }
    
    @ParameterizedTest
    @CsvSource({
        "1.0, 0.0",    // Real scalar
        "0.0, 1.0",    // Imaginary scalar
        "1.0, 1.0",    // Complex scalar
        "-1.0, 0.0",   // Negative real
        "0.0, -1.0",   // Negative imaginary
        "2.5, -1.5"    // Arbitrary complex
    })
    @DisplayName("Test complex scalar multiplication with various scalars")
    void testComplexScalarMultiplyVariousScalars(double scalarReal, double scalarImag) {
        double[] real = {1.0, 0.0, -1.0, 0.0};
        double[] imag = {0.0, 1.0, 0.0, -1.0};
        double[] resultReal = new double[4];
        double[] resultImag = new double[4];
        
        ops.complexScalarMultiply(real, imag, scalarReal, scalarImag, resultReal, resultImag);
        
        // Verify manual calculation for first element
        double expectedReal = real[0] * scalarReal - imag[0] * scalarImag;
        double expectedImag = real[0] * scalarImag + imag[0] * scalarReal;
        assertEquals(expectedReal, resultReal[0], EPSILON);
        assertEquals(expectedImag, resultImag[0], EPSILON);
    }
    
    // ==========================================
    // Complex Conjugate Tests
    // ==========================================
    
    @Test
    @DisplayName("Test complex conjugate")
    void testComplexConjugate() {
        double[] real = {1.0, 2.0, -3.0, 0.0};
        double[] imag = {2.0, -1.0, 4.0, 5.0};
        double[] resultReal = new double[4];
        double[] resultImag = new double[4];
        
        ops.complexConjugate(real, imag, resultReal, resultImag);
        
        // Real parts should be unchanged
        assertArrayEquals(real, resultReal, EPSILON);
        
        // Imaginary parts should be negated
        assertEquals(-2.0, resultImag[0], EPSILON);
        assertEquals(1.0, resultImag[1], EPSILON);
        assertEquals(-4.0, resultImag[2], EPSILON);
        assertEquals(-5.0, resultImag[3], EPSILON);
    }
    
    @Test
    @DisplayName("Test complex conjugate with large arrays")
    void testComplexConjugateLarge() {
        int size = 128;
        double[] real = new double[size];
        double[] imag = new double[size];
        double[] resultReal = new double[size];
        double[] resultImag = new double[size];
        
        for (int i = 0; i < size; i++) {
            real[i] = Math.cos(i * 0.1);
            imag[i] = Math.sin(i * 0.1);
        }
        
        ops.complexConjugate(real, imag, resultReal, resultImag);
        
        assertArrayEquals(real, resultReal, EPSILON);
        
        for (int i = 0; i < size; i++) {
            assertEquals(-imag[i], resultImag[i], EPSILON);
        }
    }
    
    // ==========================================
    // Complex Addition Tests
    // ==========================================
    
    @Test
    @DisplayName("Test complex addition")
    void testComplexAdd() {
        double[] real1 = {1.0, 2.0, -1.0};
        double[] imag1 = {1.0, -1.0, 0.0};
        double[] real2 = {2.0, -1.0, 1.0};
        double[] imag2 = {-1.0, 2.0, 3.0};
        double[] resultReal = new double[3];
        double[] resultImag = new double[3];
        
        ops.complexAdd(real1, imag1, real2, imag2, resultReal, resultImag);
        
        assertEquals(3.0, resultReal[0], EPSILON);
        assertEquals(0.0, resultImag[0], EPSILON);
        assertEquals(1.0, resultReal[1], EPSILON);
        assertEquals(1.0, resultImag[1], EPSILON);
        assertEquals(0.0, resultReal[2], EPSILON);
        assertEquals(3.0, resultImag[2], EPSILON);
    }
    
    @Test
    @DisplayName("Test complex addition with large arrays")
    void testComplexAddLarge() {
        int size = 128;
        double[] real1 = new double[size];
        double[] imag1 = new double[size];
        double[] real2 = new double[size];
        double[] imag2 = new double[size];
        double[] resultReal = new double[size];
        double[] resultImag = new double[size];
        
        for (int i = 0; i < size; i++) {
            real1[i] = i * 0.1;
            imag1[i] = -i * 0.1;
            real2[i] = i * 0.2;
            imag2[i] = i * 0.2;
        }
        
        ops.complexAdd(real1, imag1, real2, imag2, resultReal, resultImag);
        
        for (int i = 0; i < size; i++) {
            assertEquals(real1[i] + real2[i], resultReal[i], EPSILON);
            assertEquals(imag1[i] + imag2[i], resultImag[i], EPSILON);
        }
    }
    
    // ==========================================
    // Complex Subtraction Tests
    // ==========================================
    
    @Test
    @DisplayName("Test complex subtraction")
    void testComplexSubtract() {
        double[] real1 = {3.0, 1.0, 0.0};
        double[] imag1 = {2.0, -1.0, 5.0};
        double[] real2 = {1.0, 2.0, -1.0};
        double[] imag2 = {1.0, -2.0, 2.0};
        double[] resultReal = new double[3];
        double[] resultImag = new double[3];
        
        ops.complexSubtract(real1, imag1, real2, imag2, resultReal, resultImag);
        
        assertEquals(2.0, resultReal[0], EPSILON);
        assertEquals(1.0, resultImag[0], EPSILON);
        assertEquals(-1.0, resultReal[1], EPSILON);
        assertEquals(1.0, resultImag[1], EPSILON);
        assertEquals(1.0, resultReal[2], EPSILON);
        assertEquals(3.0, resultImag[2], EPSILON);
    }
    
    @Test
    @DisplayName("Test complex subtraction with large arrays")
    void testComplexSubtractLarge() {
        int size = 256;
        double[] real1 = new double[size];
        double[] imag1 = new double[size];
        double[] real2 = new double[size];
        double[] imag2 = new double[size];
        double[] resultReal = new double[size];
        double[] resultImag = new double[size];
        
        for (int i = 0; i < size; i++) {
            real1[i] = i;
            imag1[i] = i * 2;
            real2[i] = i * 0.5;
            imag2[i] = i;
        }
        
        ops.complexSubtract(real1, imag1, real2, imag2, resultReal, resultImag);
        
        for (int i = 0; i < size; i++) {
            assertEquals(real1[i] - real2[i], resultReal[i], EPSILON);
            assertEquals(imag1[i] - imag2[i], resultImag[i], EPSILON);
        }
    }
    
    // ==========================================
    // Complex Magnitude Tests
    // ==========================================
    
    @Test
    @DisplayName("Test complex magnitude")
    void testComplexMagnitude() {
        double[] real = {3.0, 0.0, 5.0, 1.0};
        double[] imag = {4.0, 1.0, 12.0, 1.0};
        double[] magnitude = new double[4];
        
        ops.complexMagnitude(real, imag, magnitude);
        
        assertEquals(5.0, magnitude[0], EPSILON);     // sqrt(3²+4²) = 5
        assertEquals(1.0, magnitude[1], EPSILON);     // sqrt(0²+1²) = 1
        assertEquals(13.0, magnitude[2], EPSILON);    // sqrt(5²+12²) = 13
        assertEquals(Math.sqrt(2), magnitude[3], EPSILON); // sqrt(1²+1²) = √2
    }
    
    @Test
    @DisplayName("Test complex magnitude with large arrays")
    void testComplexMagnitudeLarge() {
        int size = 128;
        double[] real = new double[size];
        double[] imag = new double[size];
        double[] magnitude = new double[size];
        
        for (int i = 0; i < size; i++) {
            real[i] = Math.cos(i * 0.1);
            imag[i] = Math.sin(i * 0.1);
        }
        
        ops.complexMagnitude(real, imag, magnitude);
        
        // Unit circle complex numbers should have magnitude 1
        for (int i = 0; i < size; i++) {
            assertEquals(1.0, magnitude[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test complex magnitude with zero values")
    void testComplexMagnitudeZero() {
        double[] real = {0.0, 0.0, 1.0, 0.0};
        double[] imag = {0.0, 2.0, 0.0, 0.0};
        double[] magnitude = new double[4];
        
        ops.complexMagnitude(real, imag, magnitude);
        
        assertEquals(0.0, magnitude[0], EPSILON);
        assertEquals(2.0, magnitude[1], EPSILON);
        assertEquals(1.0, magnitude[2], EPSILON);
        assertEquals(0.0, magnitude[3], EPSILON);
    }
    
    // ==========================================
    // Complex Phase Tests
    // ==========================================
    
    @Test
    @DisplayName("Test complex phase")
    void testComplexPhase() {
        double[] real = {1.0, 0.0, -1.0, 0.0, 1.0};
        double[] imag = {0.0, 1.0, 0.0, -1.0, 1.0};
        double[] phase = new double[5];
        
        ops.complexPhase(real, imag, phase);
        
        assertEquals(0.0, phase[0], EPSILON);            // atan2(0, 1) = 0
        assertEquals(Math.PI / 2, phase[1], EPSILON);    // atan2(1, 0) = π/2
        assertEquals(Math.PI, phase[2], EPSILON);        // atan2(0, -1) = π
        assertEquals(-Math.PI / 2, phase[3], EPSILON);   // atan2(-1, 0) = -π/2
        assertEquals(Math.PI / 4, phase[4], EPSILON);    // atan2(1, 1) = π/4
    }
    
    @Test
    @DisplayName("Test complex phase with various quadrants")
    void testComplexPhaseQuadrants() {
        double[] real = {1.0, -1.0, -1.0, 1.0};
        double[] imag = {1.0, 1.0, -1.0, -1.0};
        double[] phase = new double[4];
        
        ops.complexPhase(real, imag, phase);
        
        // First quadrant
        assertEquals(Math.PI / 4, phase[0], EPSILON);
        
        // Second quadrant
        assertEquals(3 * Math.PI / 4, phase[1], EPSILON);
        
        // Third quadrant
        assertEquals(-3 * Math.PI / 4, phase[2], EPSILON);
        
        // Fourth quadrant
        assertEquals(-Math.PI / 4, phase[3], EPSILON);
    }
    
    // ==========================================
    // Complex Division Tests
    // ==========================================
    
    @Test
    @DisplayName("Test complex division")
    void testComplexDivide() {
        double[] real1 = {1.0, 3.0, 2.0};
        double[] imag1 = {1.0, 4.0, 1.0};
        double[] real2 = {1.0, 1.0, 1.0};
        double[] imag2 = {1.0, 1.0, -1.0};
        double[] resultReal = new double[3];
        double[] resultImag = new double[3];
        
        ops.complexDivide(real1, imag1, real2, imag2, resultReal, resultImag);
        
        // (1+i) / (1+i) = 1
        assertEquals(1.0, resultReal[0], EPSILON);
        assertEquals(0.0, resultImag[0], EPSILON);
        
        // (3+4i) / (1+i) = 3.5 + 0.5i
        assertEquals(3.5, resultReal[1], EPSILON);
        assertEquals(0.5, resultImag[1], EPSILON);
        
        // (2+i) / (1-i) = 0.5 + 1.5i
        assertEquals(0.5, resultReal[2], EPSILON);
        assertEquals(1.5, resultImag[2], EPSILON);
    }
    
    @Test
    @DisplayName("Test complex division with large arrays")
    void testComplexDivideLarge() {
        int size = 128;
        double[] real1 = new double[size];
        double[] imag1 = new double[size];
        double[] real2 = new double[size];
        double[] imag2 = new double[size];
        double[] resultReal = new double[size];
        double[] resultImag = new double[size];
        
        for (int i = 0; i < size; i++) {
            real1[i] = i + 1.0;
            imag1[i] = i + 1.0;
            real2[i] = 1.0;
            imag2[i] = 0.0; // Divide by real numbers
        }
        
        ops.complexDivide(real1, imag1, real2, imag2, resultReal, resultImag);
        
        for (int i = 0; i < size; i++) {
            assertEquals(real1[i], resultReal[i], EPSILON);
            assertEquals(imag1[i], resultImag[i], EPSILON);
        }
    }
    
    // ==========================================
    // Matrix Multiplication Tests
    // ==========================================
    
    @Test
    @DisplayName("Test matrix multiplication basic")
    void testMatrixMultiplyBasic() {
        ComplexMatrix a = new ComplexMatrix(2, 2);
        ComplexMatrix b = new ComplexMatrix(2, 2);
        
        // A = [[1+i, 0], [0, 1-i]]
        a.set(0, 0, 1.0, 1.0);
        a.set(0, 1, 0.0, 0.0);
        a.set(1, 0, 0.0, 0.0);
        a.set(1, 1, 1.0, -1.0);
        
        // B = [[1, 0], [0, 1]] (identity)
        b.set(0, 0, 1.0, 0.0);
        b.set(0, 1, 0.0, 0.0);
        b.set(1, 0, 0.0, 0.0);
        b.set(1, 1, 1.0, 0.0);
        
        ComplexMatrix result = ops.matrixMultiply(a, b);
        
        assertNotNull(result);
        assertEquals(2, result.getRows());
        assertEquals(2, result.getCols());
        
        // A * I = A
        assertEquals(1.0, result.getReal()[0][0], EPSILON);
        assertEquals(1.0, result.getImaginary()[0][0], EPSILON);
        assertEquals(0.0, result.getReal()[0][1], EPSILON);
        assertEquals(0.0, result.getImaginary()[0][1], EPSILON);
        assertEquals(0.0, result.getReal()[1][0], EPSILON);
        assertEquals(0.0, result.getImaginary()[1][0], EPSILON);
        assertEquals(1.0, result.getReal()[1][1], EPSILON);
        assertEquals(-1.0, result.getImaginary()[1][1], EPSILON);
    }
    
    @Test
    @DisplayName("Test matrix multiplication with mismatched dimensions")
    void testMatrixMultiplyMismatchedDimensions() {
        ComplexMatrix a = new ComplexMatrix(2, 3);
        ComplexMatrix b = new ComplexMatrix(2, 2); // Wrong dimensions
        
        assertThrows(IllegalArgumentException.class, () -> {
            ops.matrixMultiply(a, b);
        });
    }
    
    @Test
    @DisplayName("Test matrix multiplication transposed")
    void testMatrixMultiplyTransposed() {
        ComplexMatrix a = new ComplexMatrix(2, 3);
        ComplexMatrix bT = new ComplexMatrix(2, 3); // Transposed dimensions
        
        // Initialize with simple values
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                a.set(i, j, i + j + 1.0, 0.0);
                bT.set(i, j, (i + 1) * (j + 1), 0.0);
            }
        }
        
        ComplexMatrix result = ops.matrixMultiplyTransposed(a, bT);
        
        assertNotNull(result);
        assertEquals(2, result.getRows());
        assertEquals(2, result.getCols());
        
        // Verify results are valid
        double[][] resultReal = result.getReal();
        double[][] resultImag = result.getImaginary();
        
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                assertFalse(Double.isNaN(resultReal[i][j]));
                assertFalse(Double.isInfinite(resultReal[i][j]));
                assertFalse(Double.isNaN(resultImag[i][j]));
                assertFalse(Double.isInfinite(resultImag[i][j]));
            }
        }
    }
    
    @Test
    @DisplayName("Test matrix multiplication transposed with mismatched dimensions")
    void testMatrixMultiplyTransposedMismatch() {
        ComplexMatrix a = new ComplexMatrix(2, 3);
        ComplexMatrix bT = new ComplexMatrix(2, 2); // Wrong transposed dimensions
        
        assertThrows(IllegalArgumentException.class, () -> {
            ops.matrixMultiplyTransposed(a, bT);
        });
    }
    
    // ==========================================
    // Layout Conversion Tests
    // ==========================================
    
    @Test
    @DisplayName("Test convert to split layout small arrays")
    void testConvertToSplitSmall() {
        double[] interleaved = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        double[] real = new double[3];
        double[] imag = new double[3];
        
        ops.convertToSplit(interleaved, real, imag);
        
        assertEquals(1.0, real[0], EPSILON);
        assertEquals(2.0, imag[0], EPSILON);
        assertEquals(3.0, real[1], EPSILON);
        assertEquals(4.0, imag[1], EPSILON);
        assertEquals(5.0, real[2], EPSILON);
        assertEquals(6.0, imag[2], EPSILON);
    }
    
    @Test
    @DisplayName("Test convert to split layout large arrays")
    void testConvertToSplitLarge() {
        int size = 128;
        double[] interleaved = new double[size * 2];
        double[] real = new double[size];
        double[] imag = new double[size];
        
        for (int i = 0; i < size; i++) {
            interleaved[2 * i] = i + 1.0;
            interleaved[2 * i + 1] = i + 1.5;
        }
        
        ops.convertToSplit(interleaved, real, imag);
        
        for (int i = 0; i < size; i++) {
            assertEquals(i + 1.0, real[i], EPSILON);
            assertEquals(i + 1.5, imag[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test convert to interleaved layout small arrays")
    void testConvertToInterleavedSmall() {
        double[] real = {1.0, 3.0, 5.0};
        double[] imag = {2.0, 4.0, 6.0};
        double[] interleaved = new double[6];
        
        ops.convertToInterleaved(real, imag, interleaved);
        
        assertEquals(1.0, interleaved[0], EPSILON);
        assertEquals(2.0, interleaved[1], EPSILON);
        assertEquals(3.0, interleaved[2], EPSILON);
        assertEquals(4.0, interleaved[3], EPSILON);
        assertEquals(5.0, interleaved[4], EPSILON);
        assertEquals(6.0, interleaved[5], EPSILON);
    }
    
    @Test
    @DisplayName("Test convert to interleaved layout large arrays")
    void testConvertToInterleavedLarge() {
        int size = 256;
        double[] real = new double[size];
        double[] imag = new double[size];
        double[] interleaved = new double[size * 2];
        
        for (int i = 0; i < size; i++) {
            real[i] = Math.cos(i * 0.1);
            imag[i] = Math.sin(i * 0.1);
        }
        
        ops.convertToInterleaved(real, imag, interleaved);
        
        for (int i = 0; i < size; i++) {
            assertEquals(real[i], interleaved[2 * i], EPSILON);
            assertEquals(imag[i], interleaved[2 * i + 1], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test round-trip conversion")
    void testRoundTripConversion() {
        int size = 64;
        double[] originalReal = new double[size];
        double[] originalImag = new double[size];
        
        for (int i = 0; i < size; i++) {
            originalReal[i] = i * 0.1;
            originalImag[i] = i * 0.2;
        }
        
        // Convert to interleaved
        double[] interleaved = new double[size * 2];
        ops.convertToInterleaved(originalReal, originalImag, interleaved);
        
        // Convert back to split
        double[] real = new double[size];
        double[] imag = new double[size];
        ops.convertToSplit(interleaved, real, imag);
        
        // Should match original arrays
        assertArrayEquals(originalReal, real, EPSILON);
        assertArrayEquals(originalImag, imag, EPSILON);
    }
    
    // ==========================================
    // Optimization Statistics Tests
    // ==========================================
    
    @Test
    @DisplayName("Test optimization statistics")
    void testGetStats() {
        ComplexVectorOps.OptimizationStats stats = ops.getStats();
        
        assertNotNull(stats);
        assertNotNull(stats.vectorSpecies());
        assertTrue(stats.vectorLength() > 0);
        assertTrue(stats.simdThreshold() > 0);
        assertTrue(stats.availableProcessors() > 0);
    }
    
    // ==========================================
    // Layout Enum Tests
    // ==========================================
    
    @Test
    @DisplayName("Test Layout enum values")
    void testLayoutEnum() {
        ComplexVectorOps.Layout[] layouts = ComplexVectorOps.Layout.values();
        
        assertEquals(2, layouts.length);
        assertEquals(ComplexVectorOps.Layout.SPLIT, layouts[0]);
        assertEquals(ComplexVectorOps.Layout.INTERLEAVED, layouts[1]);
        
        assertEquals("SPLIT", ComplexVectorOps.Layout.SPLIT.name());
        assertEquals("INTERLEAVED", ComplexVectorOps.Layout.INTERLEAVED.name());
    }
    
    // ==========================================
    // Edge Cases and Error Conditions
    // ==========================================
    
    @Test
    @DisplayName("Test operations with zero-length arrays")
    void testZeroLengthArrays() {
        double[] empty = {};
        double[] resultReal = {};
        double[] resultImag = {};
        
        assertDoesNotThrow(() -> {
            ops.complexMultiply(empty, empty, empty, empty, resultReal, resultImag);
            ops.complexAdd(empty, empty, empty, empty, resultReal, resultImag);
            ops.complexSubtract(empty, empty, empty, empty, resultReal, resultImag);
        });
    }
    
    @Test
    @DisplayName("Test operations with single element arrays")
    void testSingleElementArrays() {
        double[] real1 = {2.0};
        double[] imag1 = {3.0};
        double[] real2 = {1.0};
        double[] imag2 = {-1.0};
        double[] resultReal = new double[1];
        double[] resultImag = new double[1];
        
        ops.complexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
        
        // (2+3i) * (1-i) = 2-2i+3i-3i² = 2+i+3 = 5+i
        assertEquals(5.0, resultReal[0], EPSILON);
        assertEquals(1.0, resultImag[0], EPSILON);
    }
    
    @Test
    @DisplayName("Test magnitude with extreme values")
    void testMagnitudeExtremeValues() {
        double[] real = {Double.MAX_VALUE / 1e10, 0.0, 1e-100};
        double[] imag = {0.0, Double.MAX_VALUE / 1e10, 1e-100};
        double[] magnitude = new double[3];
        
        ops.complexMagnitude(real, imag, magnitude);
        
        assertEquals(Double.MAX_VALUE / 1e10, magnitude[0], magnitude[0] * 1e-10);
        assertEquals(Double.MAX_VALUE / 1e10, magnitude[1], magnitude[1] * 1e-10);
        assertEquals(Math.sqrt(2) * 1e-100, magnitude[2], 1e-110);
    }
    
    @Test
    @DisplayName("Test division by very small numbers")
    void testDivisionBySmallNumbers() {
        double[] real1 = {1.0};
        double[] imag1 = {0.0};
        double[] real2 = {1e-100};
        double[] imag2 = {0.0};
        double[] resultReal = new double[1];
        double[] resultImag = new double[1];
        
        ops.complexDivide(real1, imag1, real2, imag2, resultReal, resultImag);
        
        assertEquals(1e100, resultReal[0], 1e90);
        assertEquals(0.0, resultImag[0], EPSILON);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 4, 8, 16, 32, 64, 128, 256})
    @DisplayName("Test operations with various array sizes")
    void testVariousArraySizes(int size) {
        double[] real1 = new double[size];
        double[] imag1 = new double[size];
        double[] real2 = new double[size];
        double[] imag2 = new double[size];
        double[] resultReal = new double[size];
        double[] resultImag = new double[size];
        
        for (int i = 0; i < size; i++) {
            real1[i] = Math.cos(i * 0.1);
            imag1[i] = Math.sin(i * 0.1);
            real2[i] = 1.0;
            imag2[i] = 0.0;
        }
        
        assertDoesNotThrow(() -> {
            ops.complexMultiply(real1, imag1, real2, imag2, resultReal, resultImag);
            
            // Multiplying by (1,0) should give original values
            for (int i = 0; i < size; i++) {
                assertEquals(real1[i], resultReal[i], EPSILON);
                assertEquals(imag1[i], resultImag[i], EPSILON);
            }
        });
    }
    
    @Test
    @DisplayName("Test static assertion verification")
    void testStaticAssertion() {
        // This test verifies that the static assertion in ComplexVectorOps works
        // This test verifies that the static assertion in ComplexVectorOps works.
        // The static assertion in ComplexVectorOps requires UNROLL_FACTOR to be 4.
        // If UNROLL_FACTOR is not 4, the class would fail to load and this test would fail.
        // (Note: UNROLL_FACTOR is not visible in this test context, but its value is checked in the implementation.)
        assertDoesNotThrow(() -> {
            ComplexVectorOps testOps = new ComplexVectorOps();
            assertNotNull(testOps);
        });
    }
    
    @Test
    @DisplayName("Test layout conversion with boundary sizes")
    void testLayoutConversionBoundary() {
        // Test with size exactly at SIMD threshold boundary
        int size = 64; // Assuming SIMD_THRESHOLD = 64
        
        double[] real = new double[size];
        double[] imag = new double[size];
        double[] interleaved = new double[size * 2];
        
        for (int i = 0; i < size; i++) {
            real[i] = i;
            imag[i] = i + 0.5;
        }
        
        ops.convertToInterleaved(real, imag, interleaved);
        
        double[] convertedReal = new double[size];
        double[] convertedImag = new double[size];
        ops.convertToSplit(interleaved, convertedReal, convertedImag);
        
        assertArrayEquals(real, convertedReal, EPSILON);
        assertArrayEquals(imag, convertedImag, EPSILON);
    }
    
    @Test
    @DisplayName("Test matrix operations with empty matrices")
    void testMatrixOperationsEmpty() {
        // ComplexMatrix constructor may not allow 0x0 matrices
        // Test with smallest valid size instead
        ComplexMatrix a = new ComplexMatrix(1, 1);
        ComplexMatrix b = new ComplexMatrix(1, 1);
        
        a.set(0, 0, 1.0, 0.0);
        b.set(0, 0, 1.0, 0.0);
        
        ComplexMatrix result = ops.matrixMultiply(a, b);
        
        assertNotNull(result);
        assertEquals(1, result.getRows());
        assertEquals(1, result.getCols());
        assertEquals(1.0, result.getReal()[0][0], EPSILON);
        assertEquals(0.0, result.getImaginary()[0][0], EPSILON);
    }
    
    @Test
    @DisplayName("Test matrix operations with large matrices")
    void testMatrixOperationsLarge() {
        int size = 10;
        ComplexMatrix a = new ComplexMatrix(size, size);
        ComplexMatrix b = new ComplexMatrix(size, size);
        
        // Initialize with identity-like pattern
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    a.set(i, j, 1.0, 0.0);
                    b.set(i, j, 1.0, 0.0);
                } else {
                    a.set(i, j, 0.0, 0.0);
                    b.set(i, j, 0.0, 0.0);
                }
            }
        }
        
        ComplexMatrix result = ops.matrixMultiply(a, b);
        
        assertNotNull(result);
        assertEquals(size, result.getRows());
        assertEquals(size, result.getCols());
        
        // I * I = I
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (i == j) {
                    assertEquals(1.0, result.getReal()[i][j], EPSILON);
                    assertEquals(0.0, result.getImaginary()[i][j], EPSILON);
                } else {
                    assertEquals(0.0, result.getReal()[i][j], EPSILON);
                    assertEquals(0.0, result.getImaginary()[i][j], EPSILON);
                }
            }
        }
    }
}
