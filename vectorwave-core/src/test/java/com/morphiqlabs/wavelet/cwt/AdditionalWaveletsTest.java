package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for additional continuous wavelets added in version 1.4.0.
 * Tests Complex Shannon, Meyer, Morse, Ricker, and Hermitian wavelets.
 */
@DisplayName("Additional Continuous Wavelets Tests")
public class AdditionalWaveletsTest {
    
    private static final double TOLERANCE = 1e-10;
    private static final double RELAXED_TOLERANCE = 1e-6;
    
    // Numerical integration parameters for inner product computation
    private static final double INTEGRATION_RANGE = 10.0;  // Integration from -tMax to +tMax
    private static final double INTEGRATION_STEP = 0.01;   // Step size for numerical integration
    
    @Test
    @DisplayName("Complex Shannon Wavelet - Construction and Properties")
    void testComplexShannonConstruction() {
        // Test with specific parameters
        ComplexShannonWavelet cshan = new ComplexShannonWavelet(2.0, 3.0);
        
        assertEquals("cshan2.0-3.0", cshan.name());
        assertEquals(3.0, cshan.centerFrequency(), TOLERANCE);
        assertEquals(2.0, cshan.bandwidth(), TOLERANCE);
        assertTrue(cshan.isComplex());
        
        // Test default constructor
        ComplexShannonWavelet cshanDefault = new ComplexShannonWavelet();
        assertEquals("cshan1.0-1.0", cshanDefault.name());
        assertEquals(1.0, cshanDefault.centerFrequency(), TOLERANCE);
        assertEquals(1.0, cshanDefault.bandwidth(), TOLERANCE);
    }
    
    @Test
    @DisplayName("Complex Shannon Wavelet - Sinc Function")
    void testComplexShannonSincFunction() {
        ComplexShannonWavelet cshan = new ComplexShannonWavelet();
        
        // At t=0, sinc should be 1
        double realAt0 = cshan.psi(0);
        double imagAt0 = cshan.psiImaginary(0);
        
        // Real part at t=0 should be 1/sqrt(fb) * cos(0) = 1/sqrt(fb)
        assertEquals(1.0 / Math.sqrt(1.0), realAt0, TOLERANCE);
        // Imaginary part at t=0 should be 1/sqrt(fb) * sin(0) = 0
        assertEquals(0.0, imagAt0, TOLERANCE);
    }
    
    @Test
    @DisplayName("Complex Shannon Wavelet - Frequency Localization")
    void testComplexShannonFrequencyLocalization() {
        ComplexShannonWavelet cshan = new ComplexShannonWavelet(1.0, 2.0);
        
        // Should have perfect frequency localization around center frequency
        ComplexNumber psiComplex = cshan.psiComplex(0.5);
        assertNotNull(psiComplex);
        
        // Verify complex nature
        assertNotEquals(0.0, psiComplex.real());
        // At t=0.5, imaginary part should be non-zero due to sin(2*pi*fc*t)
        assertNotEquals(0.0, psiComplex.imag());
    }
    
    @Test
    @DisplayName("Meyer Wavelet - Construction and Properties")
    void testMeyerConstruction() {
        ContinuousMeyerWavelet meyer = new ContinuousMeyerWavelet();
        
        assertEquals("meyr", meyer.name());
        assertEquals(0.7, meyer.centerFrequency(), RELAXED_TOLERANCE);
        assertEquals(1.5, meyer.bandwidth(), RELAXED_TOLERANCE);
        assertFalse(meyer.isComplex());
    }
    
    @Test
    @DisplayName("Meyer Wavelet - Frequency Domain Properties")
    void testMeyerFrequencyDomain() {
        ContinuousMeyerWavelet meyer = new ContinuousMeyerWavelet();
        
        // Test frequency domain values at key points
        // Should be 0 below 2π/3
        assertEquals(0.0, meyer.psiHat(Math.PI / 2), TOLERANCE);
        
        // Should be non-zero in transition regions
        double val1 = meyer.psiHat(Math.PI); // In first transition region
        assertNotEquals(0.0, val1);
        
        // Should be 0 above 8π/3
        assertEquals(0.0, meyer.psiHat(3 * Math.PI), TOLERANCE);
    }
    
    @Test
    @DisplayName("Morse Wavelet - Construction with Parameters")
    void testMorseConstruction() {
        // Test with specific parameters
        MorseWavelet morse = new MorseWavelet(3.0, 60.0);
        
        assertEquals("morse3.0-60.0", morse.name());
        assertEquals(3.0, morse.getBeta(), TOLERANCE);
        assertEquals(60.0, morse.getGamma(), TOLERANCE);
        assertTrue(morse.isComplex());
        
        // Test time-frequency product
        double tfProduct = morse.getTimeFrequencyProduct();
        assertEquals(Math.sqrt(3.0 * 60.0), tfProduct, TOLERANCE);
        
        // Test default constructor
        MorseWavelet morseDefault = new MorseWavelet();
        assertEquals("morse3.0-60.0", morseDefault.name());
    }
    
    @Test
    @DisplayName("Morse Wavelet - Analytic Properties")
    void testMorseAnalyticProperties() {
        MorseWavelet morse = new MorseWavelet(3.0, 60.0);
        
        // Morse wavelets are analytic (only positive frequencies)
        ComplexNumber psiHat = morse.psiHat(1.0);
        assertNotNull(psiHat);
        assertTrue(psiHat.real() > 0);
        assertEquals(0.0, psiHat.imag(), TOLERANCE);
        
        // Should be zero for negative frequencies
        ComplexNumber psiHatNeg = morse.psiHat(-1.0);
        assertEquals(0.0, psiHatNeg.real(), TOLERANCE);
        assertEquals(0.0, psiHatNeg.imag(), TOLERANCE);
    }
    
    @Test
    @DisplayName("Ricker Wavelet - Construction and Properties")
    void testRickerConstruction() {
        // Test with specific sigma
        RickerWavelet ricker = new RickerWavelet(2.0);
        
        assertEquals("ricker2.00", ricker.name());
        assertEquals(2.0, ricker.getSigma(), TOLERANCE);
        assertFalse(ricker.isComplex());
        
        // Test center frequency formula
        double expectedFreq = 1.0 / (2 * Math.PI * 2.0 * Math.sqrt(2));
        assertEquals(expectedFreq, ricker.centerFrequency(), TOLERANCE);
        
        // Test default constructor
        RickerWavelet rickerDefault = new RickerWavelet();
        assertEquals(1.0, rickerDefault.getSigma(), TOLERANCE);
    }
    
    @Test
    @DisplayName("Ricker Wavelet - Mexican Hat Shape")
    void testRickerMexicanHatShape() {
        RickerWavelet ricker = new RickerWavelet(1.0);
        
        // At t=0, should have maximum positive value
        double psi0 = ricker.psi(0);
        assertTrue(psi0 > 0);
        
        // Should have negative side lobes
        double psiSide = ricker.psi(1.5);
        assertTrue(psiSide < 0);
        
        // Should decay to zero at infinity
        double psiFar = ricker.psi(10.0);
        assertEquals(0.0, psiFar, RELAXED_TOLERANCE);
    }
    
    @Test
    @DisplayName("Ricker Wavelet - Frequency to Scale Conversion")
    void testRickerFrequencyToScale() {
        double frequency = 10.0; // Hz
        double samplingRate = 100.0; // Hz
        
        double scale = RickerWavelet.frequencyToScale(frequency, samplingRate);
        assertTrue(scale > 0);
        
        // Verify the relationship
        double centerFreq = 1.0 / (2 * Math.PI * Math.sqrt(2));
        double expectedScale = centerFreq * samplingRate / frequency;
        assertEquals(expectedScale, scale, TOLERANCE);
    }
    
    @Test
    @DisplayName("Hermitian Wavelet - Construction with Different Orders")
    void testHermitianConstruction() {
        // Test different orders
        for (int n = 0; n <= 5; n++) {
            HermitianWavelet herm = new HermitianWavelet(n, 1.0);
            assertEquals(String.format("herm%d-1.0", n), herm.name());
            assertEquals(n, herm.getOrder());
            assertEquals(1.0, herm.getSigma(), TOLERANCE);
            assertFalse(herm.isComplex());
        }
        
        // Test with different sigma
        HermitianWavelet herm2 = new HermitianWavelet(2, 2.0);
        assertEquals("herm2-2.0", herm2.name());
        assertEquals(2.0, herm2.getSigma(), TOLERANCE);
    }
    
    @Test
    @DisplayName("Hermitian Wavelet - Orthogonality")
    void testHermitianOrthogonality() {
        HermitianWavelet h0 = new HermitianWavelet(0, 1.0);
        HermitianWavelet h1 = new HermitianWavelet(1, 1.0);
        HermitianWavelet h2 = new HermitianWavelet(2, 1.0);
        
        // Different orders should be orthogonal
        double inner01 = computeInnerProduct(h0, h1);
        double inner02 = computeInnerProduct(h0, h2);
        double inner12 = computeInnerProduct(h1, h2);
        
        assertEquals(0.0, inner01, RELAXED_TOLERANCE);
        assertEquals(0.0, inner02, RELAXED_TOLERANCE);
        assertEquals(0.0, inner12, RELAXED_TOLERANCE);
        
        // Same wavelet should have unit norm (after normalization)
        double inner00 = computeInnerProduct(h0, h0);
        assertTrue(inner00 > 0);
    }
    
    @Test
    @DisplayName("Hermitian Wavelet - Family Creation")
    void testHermitianFamilyCreation() {
        HermitianWavelet[] family = HermitianWavelet.createFamily(4, 1.5);
        
        assertEquals(5, family.length); // Orders 0 through 4
        
        for (int i = 0; i < family.length; i++) {
            assertEquals(i, family[i].getOrder());
            assertEquals(1.5, family[i].getSigma(), TOLERANCE);
        }
    }
    
    @Test
    @DisplayName("Wavelet Registry - All New Wavelets Registered")
    void testWaveletRegistryNewWavelets() {
        // Test that all new wavelets are properly registered
        assertNotNull(WaveletRegistry.getWavelet(WaveletName.CSHAN));
        assertNotNull(WaveletRegistry.getWavelet(WaveletName.MEYER));
        assertNotNull(WaveletRegistry.getWavelet(WaveletName.MORSE));
        assertNotNull(WaveletRegistry.getWavelet(WaveletName.RICKER));
        assertNotNull(WaveletRegistry.getWavelet(WaveletName.HERMITIAN));
        
        // Verify they are the correct types
        assertTrue(WaveletRegistry.getWavelet(WaveletName.CSHAN) instanceof ComplexShannonWavelet);
        assertTrue(WaveletRegistry.getWavelet(WaveletName.MEYER) instanceof ContinuousMeyerWavelet);
        assertTrue(WaveletRegistry.getWavelet(WaveletName.MORSE) instanceof MorseWavelet);
        assertTrue(WaveletRegistry.getWavelet(WaveletName.RICKER) instanceof RickerWavelet);
        assertTrue(WaveletRegistry.getWavelet(WaveletName.HERMITIAN) instanceof HermitianWavelet);
    }
    
    @Test
    @DisplayName("All Wavelets - Discretization")
    void testAllWaveletsDiscretization() {
        // Test that all new wavelets can be discretized
        WaveletName[] newWavelets = {
            WaveletName.CSHAN, WaveletName.MEYER, WaveletName.MORSE,
            WaveletName.RICKER, WaveletName.HERMITIAN
        };
        
        for (WaveletName name : newWavelets) {
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            double[] coeffs = wavelet.lowPassDecomposition();
            
            assertNotNull(coeffs);
            assertTrue(coeffs.length > 0);
            
            // Check that coefficients are finite
            for (double c : coeffs) {
                assertTrue(Double.isFinite(c));
            }
        }
    }
    
    @Test
    @DisplayName("Complex Wavelets - Complex Values")
    void testComplexWaveletsComplexValues() {
        // Test complex wavelets return both real and imaginary parts
        ComplexShannonWavelet cshan = new ComplexShannonWavelet();
        MorseWavelet morse = new MorseWavelet();
        
        // Test at various points
        double[] testPoints = {-2.0, -1.0, 0.0, 0.5, 1.0, 2.0};
        
        for (double t : testPoints) {
            // Complex Shannon
            ComplexNumber cshanVal = cshan.psiComplex(t);
            assertNotNull(cshanVal);
            assertTrue(Double.isFinite(cshanVal.real()));
            assertTrue(Double.isFinite(cshanVal.imag()));
            
            // Morse
            ComplexNumber morseVal = morse.psiComplex(t);
            assertNotNull(morseVal);
            assertTrue(Double.isFinite(morseVal.real()));
            assertTrue(Double.isFinite(morseVal.imag()));
        }
    }
    
    // Helper method to compute inner product of two wavelets using Simpson's rule for efficiency
    private double computeInnerProduct(HermitianWavelet w1, HermitianWavelet w2) {
        // Use a moderate number of intervals for fast test execution
        int intervals = 100; // Must be even for Simpson's rule
        double a = -INTEGRATION_RANGE;
        double b = INTEGRATION_RANGE;
        double h = (b - a) / intervals;
        double sum = w1.psi(a) * w2.psi(a) + w1.psi(b) * w2.psi(b);
        for (int i = 1; i < intervals; i++) {
            double t = a + i * h;
            double factor = (i % 2 == 0) ? 2.0 : 4.0;
            sum += factor * w1.psi(t) * w2.psi(t);
        }
        return sum * h / 3.0;
    }
}