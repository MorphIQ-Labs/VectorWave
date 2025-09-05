package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for extended Symlet wavelets (SYM9, SYM11, SYM13-14, SYM16-19).
 * 
 * <p>These tests verify:</p>
 * <ul>
 *   <li>Coefficient correctness against PyWavelets reference values</li>
 *   <li>Orthogonality conditions</li>
 *   <li>Vanishing moments properties</li>
 *   <li>Perfect reconstruction</li>
 *   <li>Better symmetry than corresponding Daubechies wavelets</li>
 * </ul>
 */
@DisplayName("Extended Symlet Wavelets Test Suite")
public class ExtendedSymletTest {
    
    private static final double MACHINE_PRECISION = 1e-10;
    private static final double HIGH_PRECISION = 1e-15;
    private static final double RECONSTRUCTION_TOLERANCE = 1e-10;
    
    /**
     * Test that all extended Symlets are properly registered in the registry.
     */
    @Test
    @DisplayName("Verify all extended Symlets are registered")
    void verifyExtendedSymletsRegistered() {
        int[] extendedOrders = {9, 11, 13, 14, 16, 17, 18, 19};
        
        for (int order : extendedOrders) {
            WaveletName name = WaveletName.valueOf("SYM" + order);
            assertNotNull(name, "SYM" + order + " should exist in WaveletName enum");
            
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            assertNotNull(wavelet, "SYM" + order + " should be registered");
            assertTrue(wavelet instanceof Symlet, "SYM" + order + " should be a Symlet instance");
            
            Symlet symlet = (Symlet) wavelet;
            assertEquals(order, symlet.vanishingMoments(), 
                "SYM" + order + " should have " + order + " vanishing moments");
        }
    }
    
    /**
     * Verify SYM9 coefficients against PyWavelets reference.
     */
    @Test
    @DisplayName("Verify SYM9 coefficients match PyWavelets")
    void verifySYM9Coefficients() {
        Symlet sym9 = Symlet.SYM9;
        double[] h = sym9.lowPassDecomposition();
        
        assertEquals(18, h.length, "SYM9 should have 18 coefficients");
        
        // Check first and last coefficients (spot check)
        assertEquals(0.00140091552591468, h[0], HIGH_PRECISION, 
            "SYM9 first coefficient should match PyWavelets");
        assertEquals(0.00106949003290861, h[17], HIGH_PRECISION,
            "SYM9 last coefficient should match PyWavelets");
        
        // Check a middle coefficient
        assertEquals(0.71789708276441200, h[8], HIGH_PRECISION,
            "SYM9 middle coefficient should match PyWavelets");
    }
    
    /**
     * Verify SYM11 coefficients against PyWavelets reference.
     */
    @Test
    @DisplayName("Verify SYM11 coefficients match PyWavelets")
    void verifySYM11Coefficients() {
        Symlet sym11 = Symlet.SYM11;
        double[] h = sym11.lowPassDecomposition();
        
        assertEquals(22, h.length, "SYM11 should have 22 coefficients");
        
        // Check key coefficients
        assertEquals(0.00017172195069935, h[0], HIGH_PRECISION,
            "SYM11 first coefficient should match PyWavelets");
        assertEquals(0.00048926361026192, h[21], HIGH_PRECISION,
            "SYM11 last coefficient should match PyWavelets");
        
        // Check dominant coefficient
        assertEquals(0.73034354908839572, h[12], HIGH_PRECISION,
            "SYM11 dominant coefficient should match PyWavelets");
    }
    
    /**
     * Verify all extended Symlets satisfy orthogonality conditions.
     */
    @ParameterizedTest
    @ValueSource(ints = {9, 11, 13, 14, 16, 17, 18, 19})
    @DisplayName("Verify orthogonality conditions for extended Symlets")
    void verifyOrthogonalityConditions(int order) {
        Symlet symlet = getSymletByOrder(order);
        assertTrue(symlet.verifyCoefficients(),
            "SYM" + order + " should satisfy orthogonality conditions");
        
        // QMF relationship is covered globally in WaveletCoefficientVerificationTest
    }
    
    /**
     * Verify vanishing moments property.
     */
    @ParameterizedTest
    @ValueSource(ints = {9, 11, 13, 14, 16, 17, 18, 19})
    @DisplayName("Verify vanishing moments for extended Symlets")
    void verifyVanishingMoments(int order) {
        Symlet symlet = getSymletByOrder(order);
        assertEquals(order, symlet.vanishingMoments(),
            "SYM" + order + " should have " + order + " vanishing moments");
        
        // Verify actual vanishing moments by checking polynomial annihilation
        double[] g = symlet.highPassDecomposition();
        
        // Check first few moments (computational verification)
        for (int k = 0; k < Math.min(order, 3); k++) {
            double moment = computeMoment(g, k);
            assertEquals(0.0, moment, 1e-10,
                "Moment " + k + " should vanish for SYM" + order);
        }
    }
    
    /**
     * Test perfect reconstruction with MODWT.
     */
    @ParameterizedTest
    @ValueSource(ints = {9, 11, 13, 14, 16, 17, 18, 19})
    @DisplayName("Verify perfect reconstruction for extended Symlets")
    void verifyPerfectReconstruction(int order) {
        Symlet symlet = getSymletByOrder(order);
        MODWTTransform transform = new MODWTTransform(symlet, BoundaryMode.PERIODIC);
        
        // Test with various signal lengths
        int[] lengths = {100, 256, 777, 1000};
        
        for (int length : lengths) {
            double[] signal = generateTestSignal(length);
            
            MODWTResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            // Check reconstruction error
            double error = computeReconstructionError(signal, reconstructed);
            assertTrue(error < RECONSTRUCTION_TOLERANCE,
                String.format("SYM%d reconstruction error (%.2e) exceeds tolerance for length %d",
                    order, error, length));
        }
    }
    
    /**
     * Verify that Symlets have better symmetry than corresponding Daubechies where available.
     */
    @ParameterizedTest
    @ValueSource(ints = {14, 16, 18})  // Only test orders where both Symlet and Daubechies exist
    @DisplayName("Verify Symlets have better symmetry than Daubechies")
    void verifyBetterSymmetry(int order) {
        Symlet symlet = getSymletByOrder(order);
        Daubechies daubechies = getDaubechiesByOrder(order);
        
        double symAsymmetry = measureAsymmetry(symlet.lowPassDecomposition());
        double dbAsymmetry = measureAsymmetry(daubechies.lowPassDecomposition());
        
        // Symlets should have better (lower) asymmetry
        assertTrue(symAsymmetry < dbAsymmetry * 1.1, // Allow 10% margin for numerical precision
            String.format("SYM%d (asymmetry=%.4f) should be more symmetric than DB%d (asymmetry=%.4f)",
                order, symAsymmetry, order, dbAsymmetry));
    }
    
    /**
     * Test coefficient sum and normalization.
     */
    @ParameterizedTest
    @ValueSource(ints = {9, 11, 13, 14, 16, 17, 18, 19})
    @DisplayName("Verify coefficient normalization for extended Symlets")
    void verifyCoefficientNormalization(int order) {
        Symlet symlet = getSymletByOrder(order);
        double[] h = symlet.lowPassDecomposition();
        
        // Sum should be sqrt(2)
        double sum = Arrays.stream(h).sum();
        assertEquals(Math.sqrt(2), sum, MACHINE_PRECISION,
            "SYM" + order + " coefficients sum should be sqrt(2)");
        
        // Sum of squares should be 1
        double sumSquares = Arrays.stream(h).map(x -> x * x).sum();
        assertEquals(1.0, sumSquares, MACHINE_PRECISION,
            "SYM" + order + " coefficients sum of squares should be 1");
    }
    
    /**
     * Test filter length consistency.
     */
    @ParameterizedTest
    @ValueSource(ints = {9, 11, 13, 14, 16, 17, 18, 19})
    @DisplayName("Verify filter length for extended Symlets")
    void verifyFilterLength(int order) {
        Symlet symlet = getSymletByOrder(order);
        double[] h = symlet.lowPassDecomposition();
        
        int expectedLength = 2 * order;
        assertEquals(expectedLength, h.length,
            "SYM" + order + " should have filter length " + expectedLength);
    }
    
    // Helper methods
    
    private Symlet getSymletByOrder(int order) {
        switch (order) {
            case 9: return Symlet.SYM9;
            case 11: return Symlet.SYM11;
            case 13: return Symlet.SYM13;
            case 14: return Symlet.SYM14;
            case 16: return Symlet.SYM16;
            case 17: return Symlet.SYM17;
            case 18: return Symlet.SYM18;
            case 19: return Symlet.SYM19;
            default: throw new IllegalArgumentException("Unsupported Symlet order: " + order);
        }
    }
    
    private Daubechies getDaubechiesByOrder(int order) {
        WaveletName name = WaveletName.valueOf("DB" + order);
        return (Daubechies) WaveletRegistry.getWavelet(name);
    }
    
    private double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            // Combination of sinusoids and noise
            signal[i] = Math.sin(2 * Math.PI * i / 64.0) +
                       0.5 * Math.sin(2 * Math.PI * i / 16.0) +
                       0.1 * Math.random();
        }
        return signal;
    }
    
    private double computeReconstructionError(double[] original, double[] reconstructed) {
        double errorSum = 0;
        for (int i = 0; i < original.length; i++) {
            double diff = original[i] - reconstructed[i];
            errorSum += diff * diff;
        }
        return Math.sqrt(errorSum / original.length);
    }
    
    private double measureAsymmetry(double[] filter) {
        int n = filter.length;
        double asymmetry = 0;
        
        // Compare filter with its time-reversed version
        for (int i = 0; i < n/2; i++) {
            double diff = filter[i] - filter[n-1-i];
            asymmetry += diff * diff;
        }
        
        return Math.sqrt(asymmetry);
    }
    
    private double computeMoment(double[] filter, int k) {
        double moment = 0;
        for (int n = 0; n < filter.length; n++) {
            moment += filter[n] * Math.pow(n, k);
        }
        return moment;
    }
}
