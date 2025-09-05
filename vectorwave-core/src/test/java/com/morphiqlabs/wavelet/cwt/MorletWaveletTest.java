package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class MorletWaveletTest {
    
    private static final double EPSILON = 1e-10;
    
    @Test
    void testDefaultConstructor() {
        MorletWavelet wavelet = new MorletWavelet();
        assertEquals("morl", wavelet.name());
        assertEquals("Morlet wavelet (ω₀=6.0, σ=1.0)", wavelet.description());
        assertEquals(6.0 / (2 * Math.PI), wavelet.centerFrequency(), EPSILON);
        assertEquals(1.0, wavelet.bandwidth(), EPSILON);
        assertTrue(wavelet.isComplex());
    }
    
    @Test
    void testParameterizedConstructor() {
        double omega0 = 5.0;
        double sigma = 2.0;
        MorletWavelet wavelet = new MorletWavelet(omega0, sigma);
        
        assertEquals("morl", wavelet.name());
        assertEquals("Morlet wavelet (ω₀=5.0, σ=2.0)", wavelet.description());
        assertEquals(omega0 / (2 * Math.PI), wavelet.centerFrequency(), EPSILON);
        assertEquals(sigma, wavelet.bandwidth(), EPSILON);
        assertTrue(wavelet.isComplex());
    }
    
    @Test
    void testPsiAtZero() {
        MorletWavelet wavelet = new MorletWavelet();
        double psi0 = wavelet.psi(0);
        
        // At t=0, the Gaussian envelope is 1, carrier is 1
        double expected = (1.0 / Math.pow(Math.PI, 0.25)) * 
                         (1.0 - Math.exp(-0.5 * 36)); // omega0^2 * sigma^2 = 36
        assertEquals(expected, psi0, EPSILON);
    }
    
    @Test
    void testPsiSymmetry() {
        MorletWavelet wavelet = new MorletWavelet();
        
        // Real part should be symmetric
        assertEquals(wavelet.psi(1.0), wavelet.psi(-1.0), EPSILON);
        assertEquals(wavelet.psi(2.5), wavelet.psi(-2.5), EPSILON);
    }
    
    @Test
    void testPsiImaginary() {
        MorletWavelet wavelet = new MorletWavelet();
        
        // Imaginary part at t=0 should be 0 (sin(0) = 0)
        assertEquals(0.0, wavelet.psiImaginary(0), EPSILON);
        
        // Imaginary part should be antisymmetric
        assertEquals(-wavelet.psiImaginary(1.0), wavelet.psiImaginary(-1.0), EPSILON);
        assertEquals(-wavelet.psiImaginary(2.5), wavelet.psiImaginary(-2.5), EPSILON);
    }
    
    @Test
    void testPsiDecay() {
        MorletWavelet wavelet = new MorletWavelet();
        
        // Wavelet should decay as we move away from center
        double psi0 = Math.abs(wavelet.psi(0));
        double psi1 = Math.abs(wavelet.psi(1));
        double psi2 = Math.abs(wavelet.psi(2));
        double psi4 = Math.abs(wavelet.psi(4));
        
        assertTrue(psi0 > psi1);
        assertTrue(psi1 > psi2);
        assertTrue(psi2 > psi4);
        
        // Should be very small at 4 sigma
        assertTrue(Math.abs(wavelet.psi(4)) < 0.01);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {8, 16, 32, 64, 128})
    void testDiscretize(int numCoeffs) {
        MorletWavelet wavelet = new MorletWavelet();
        double[] coeffs = wavelet.discretize(numCoeffs);
        
        assertEquals(numCoeffs, coeffs.length);
        
        // Check normalization: sum of squares should be 1
        double sumSquares = 0;
        for (double c : coeffs) {
            sumSquares += c * c;
        }
        assertEquals(1.0, sumSquares, 1e-6);
        
        // Check symmetry (real part is symmetric)
        for (int i = 0; i < numCoeffs / 2; i++) {
            assertEquals(coeffs[i], coeffs[numCoeffs - 1 - i], 1e-6);
        }
    }
    
    @Test
    void testDiscretizeOddNumberThrows() {
        MorletWavelet wavelet = new MorletWavelet();
        
        assertThrows(InvalidArgumentException.class, () -> wavelet.discretize(7));
        assertThrows(InvalidArgumentException.class, () -> wavelet.discretize(15));
        assertThrows(InvalidArgumentException.class, () -> wavelet.discretize(31));
    }
    
    @Test
    void testDiscretizeCentering() {
        MorletWavelet wavelet = new MorletWavelet();
        double[] coeffs = wavelet.discretize(64);
        
        // Find the index of maximum absolute value - should be near center
        int maxIndex = 0;
        double maxValue = 0;
        for (int i = 0; i < coeffs.length; i++) {
            if (Math.abs(coeffs[i]) > maxValue) {
                maxValue = Math.abs(coeffs[i]);
                maxIndex = i;
            }
        }
        
        // Maximum should be near the center
        int center = coeffs.length / 2;
        assertTrue(Math.abs(maxIndex - center) <= 1);
    }
    
    @Test
    void testDifferentParameters() {
        // Test with different omega0 and sigma values
        MorletWavelet w1 = new MorletWavelet(4.0, 0.5);
        MorletWavelet w2 = new MorletWavelet(8.0, 2.0);
        
        // Different parameters should give different values
        assertNotEquals(w1.psi(1.0), w2.psi(1.0));
        assertNotEquals(w1.centerFrequency(), w2.centerFrequency());
        assertNotEquals(w1.bandwidth(), w2.bandwidth());
        
        // But both should still be complex
        assertTrue(w1.isComplex());
        assertTrue(w2.isComplex());
    }
    
    @Test
    void testAdmissibilityCondition() {
        MorletWavelet wavelet = new MorletWavelet();
        double[] coeffs = wavelet.discretize(256);
        
        // For a valid wavelet, the mean should be close to zero
        // (admissibility condition)
        double mean = 0;
        for (double c : coeffs) {
            mean += c;
        }
        mean /= coeffs.length;
        
        // With correction term, mean should be very close to zero
        assertEquals(0.0, mean, 1e-3);
    }
}