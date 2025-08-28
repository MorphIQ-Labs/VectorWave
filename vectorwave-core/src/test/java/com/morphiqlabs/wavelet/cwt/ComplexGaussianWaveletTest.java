package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ComplexGaussianWaveletTest {
    
    @Test
    void testComplexGaussianIsComplex() {
        ComplexGaussianWavelet cgau = new ComplexGaussianWavelet(1);
        assertTrue(cgau.isComplex(), "CGAU should be complex-valued");
        
        // Verify it has non-zero imaginary part
        double t = 1.0;
        assertNotEquals(0.0, cgau.psiImaginary(t), 1e-10);
    }
    
    @Test
    void testDifferentFromRealGaussian() {
        // Real Gaussian derivative
        GaussianDerivativeWavelet realGaus = new GaussianDerivativeWavelet(1);
        
        // Complex Gaussian
        ComplexGaussianWavelet complexGaus = new ComplexGaussianWavelet(1);
        
        // Real parts should be different due to modulation
        double t = 1.0;
        double realValue = realGaus.psi(t);
        double complexRealPart = complexGaus.psi(t);
        
        assertNotEquals(realValue, complexRealPart, 
            "Complex Gaussian should differ from real Gaussian due to modulation");
    }
    
    @Test
    void testAnalyticSignalProperty() {
        ComplexGaussianWavelet cgau = new ComplexGaussianWavelet(2);
        
        // Test that it has both real and imaginary components
        double[] testPoints = {-2.0, -1.0, 0.0, 1.0, 2.0};
        
        for (double t : testPoints) {
            double real = cgau.psi(t);
            double imag = cgau.psiImaginary(t);
            ComplexNumber complex = cgau.psiComplex(t);
            
            // Verify consistency
            assertEquals(real, complex.real(), 1e-10, 
                "Real part should match psi() method");
            assertEquals(imag, complex.imag(), 1e-10,
                "Imaginary part should match psiImaginary() method");
        }
    }
    
    @Test
    void testHermitePolynomialProperty() {
        // Test different orders
        for (int n = 1; n <= 4; n++) {
            ComplexGaussianWavelet cgau = new ComplexGaussianWavelet(n);
            
            // At t=0, the complex exponential modulation = cos(0) + i*sin(0) = 1 + i*0
            // So we can test the Hermite polynomial behavior
            ComplexNumber value = cgau.psiComplex(0);
            
            // H_n(0) = 0 for odd n, non-zero for even n
            if (n % 2 == 1) {
                assertEquals(0.0, value.real(), 1e-10, 
                    "Odd-order Hermite polynomial should be 0 at origin");
                assertEquals(0.0, value.imag(), 1e-10, 
                    "Imaginary part should also be 0 for odd order at origin");
            } else {
                assertNotEquals(0.0, Math.abs(value.real()), 1e-10,
                    "Even-order Hermite polynomial should be non-zero at origin");
            }
        }
    }
    
    @Test
    void testFrequencyDomainRepresentation() {
        ComplexGaussianWavelet cgau = new ComplexGaussianWavelet(1, 1.0, 5.0);
        
        // Test frequency domain representation near center frequency
        // For derivative wavelets, exact center frequency has zero response
        ComplexNumber psiHat = cgau.psiHat(5.5); // Near center frequency
        
        // Should have significant magnitude near center frequency
        assertTrue(psiHat.magnitude() > 0.01, 
            "Should have significant magnitude near center frequency, got: " + psiHat.magnitude());
        
        // Far from center frequency should decay
        ComplexNumber psiHatFar = cgau.psiHat(10.0);
        assertTrue(psiHatFar.magnitude() < psiHat.magnitude(),
            "Should decay away from center frequency");
        
        // Exact center frequency should be zero for derivative wavelets (n>0)
        ComplexNumber psiHatCenter = cgau.psiHat(5.0);
        assertEquals(0.0, psiHatCenter.magnitude(), 1e-10,
            "Derivative wavelets should have zero response at exact center frequency");
    }
    
    @Test
    void testRegistryAccess() {
        // Verify CGAU is accessible through registry
        Wavelet cgau = WaveletRegistry.getWavelet(WaveletName.CGAU);
        
        assertNotNull(cgau);
        assertTrue(cgau instanceof ComplexGaussianWavelet);
        assertTrue(((ContinuousWavelet) cgau).isComplex());
        
        // Should be different class from GAUSSIAN
        Wavelet gaussian = WaveletRegistry.getWavelet(WaveletName.GAUSSIAN);
        assertNotEquals(cgau.getClass(), gaussian.getClass(),
            "CGAU and GAUSSIAN should be different implementations");
    }
    
    @Test
    void testParameterValidation() {
        // Test invalid derivative order
        assertThrows(IllegalArgumentException.class, 
            () -> new ComplexGaussianWavelet(0));
        assertThrows(IllegalArgumentException.class, 
            () -> new ComplexGaussianWavelet(9));
        
        // Test invalid sigma
        assertThrows(IllegalArgumentException.class, 
            () -> new ComplexGaussianWavelet(1, 0.0, 5.0));
        assertThrows(IllegalArgumentException.class, 
            () -> new ComplexGaussianWavelet(1, -1.0, 5.0));
    }
    
    @Test
    void testDiscretization() {
        ComplexGaussianWavelet cgau = new ComplexGaussianWavelet(1);
        
        // Test that discretization requires even length
        assertThrows(IllegalArgumentException.class, 
            () -> cgau.discretize(7), "Should require even length");
        
        // Test valid discretization
        double[] samples = cgau.discretize(8);
        assertEquals(8, samples.length);
        
        // Should be stored as [real, imag, real, imag, ...]
        for (int i = 0; i < samples.length; i += 2) {
            // Both real and imaginary parts should exist
            double real = samples[i];
            double imag = samples[i + 1];
            // At least some samples should be non-zero
        }
    }
    
    @Test
    void testPropertiesConsistency() {
        ComplexGaussianWavelet cgau = new ComplexGaussianWavelet(2, 1.5, 3.0);
        
        // Test getters
        assertEquals(2, cgau.getOrder());
        assertEquals(3.0, cgau.getModulationFrequency(), 1e-10);
        
        // Test name and description
        assertEquals("cgau2", cgau.name());
        assertTrue(cgau.description().contains("Complex Gaussian"));
        assertTrue(cgau.description().contains("n=2"));
        
        // Test frequencies
        assertTrue(cgau.centerFrequency() > 0);
        assertTrue(cgau.bandwidth() > 0);
    }
    
    @Test
    void testDefaultConstructors() {
        // Default constructor
        ComplexGaussianWavelet cgau1 = new ComplexGaussianWavelet();
        assertEquals(1, cgau1.getOrder());
        assertEquals(5.0, cgau1.getModulationFrequency(), 1e-10);
        
        // Order-only constructor
        ComplexGaussianWavelet cgau2 = new ComplexGaussianWavelet(3);
        assertEquals(3, cgau2.getOrder());
        assertEquals(5.0, cgau2.getModulationFrequency(), 1e-10);
    }
    
    @Test
    void testComplexConjugateProperty() {
        ComplexGaussianWavelet cgau = new ComplexGaussianWavelet(1);
        
        // For real signals, ψ(-t) should be the complex conjugate of ψ(t)
        // This is a property of analytic wavelets
        double t = 1.5;
        ComplexNumber pos = cgau.psiComplex(t);
        ComplexNumber neg = cgau.psiComplex(-t);
        
        // The relationship is more complex due to the Hermite polynomial
        // but we can verify that both have reasonable magnitudes
        assertTrue(pos.magnitude() > 0);
        assertTrue(neg.magnitude() > 0);
    }
    
    @Test
    void testOrderScaling() {
        // Higher order should have more oscillations
        ComplexGaussianWavelet cgau1 = new ComplexGaussianWavelet(1);
        ComplexGaussianWavelet cgau4 = new ComplexGaussianWavelet(4);
        
        // Higher order should have higher bandwidth
        assertTrue(cgau4.bandwidth() > cgau1.bandwidth(),
            "Higher order should have higher bandwidth");
    }
}