package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for extended Coiflet wavelets (COIF6-COIF10).
 * Verifies mathematical properties and implementation correctness.
 */
class ExtendedCoifletTest {
    
    private static final double TOLERANCE = 1e-10;
    private static final double ORTHOGONALITY_TOLERANCE = 1e-12;
    
    @ParameterizedTest
    @ValueSource(ints = {6, 7, 8, 9, 10})
    @DisplayName("Verify filter lengths match 6N formula")
    void testFilterLength(int order) {
        Coiflet coif = getCoiflet(order);
        double[] coeffs = coif.lowPassDecomposition();
        
        int expectedLength = 6 * order;
        assertEquals(expectedLength, coeffs.length,
            String.format("COIF%d should have %d coefficients", order, expectedLength));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {6, 7, 8, 9, 10})
    @DisplayName("Verify vanishing moments equal 2N")
    void testVanishingMoments(int order) {
        Coiflet coif = getCoiflet(order);
        
        int expectedMoments = 2 * order;
        assertEquals(expectedMoments, coif.vanishingMoments(),
            String.format("COIF%d should have %d vanishing moments", order, expectedMoments));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {6, 7, 8, 9, 10})
    @DisplayName("Verify sum of coefficients equals sqrt(2)")
    void testCoefficientSum(int order) {
        Coiflet coif = getCoiflet(order);
        double[] h = coif.lowPassDecomposition();
        
        double sum = Arrays.stream(h).sum();
        double expectedSum = Math.sqrt(2.0);
        
        assertEquals(expectedSum, sum, TOLERANCE,
            String.format("COIF%d coefficient sum should be √2", order));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {6, 7, 8, 9, 10})
    @DisplayName("Verify sum of squared coefficients equals 1")
    void testCoefficientNormalization(int order) {
        Coiflet coif = getCoiflet(order);
        double[] h = coif.lowPassDecomposition();
        
        double sumSquares = Arrays.stream(h).map(x -> x * x).sum();
        
        assertEquals(1.0, sumSquares, TOLERANCE,
            String.format("COIF%d squared coefficient sum should be 1", order));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {6, 7, 8, 9, 10})
    @DisplayName("Verify orthogonality conditions")
    void testOrthogonalityConditions(int order) {
        Coiflet coif = getCoiflet(order);
        double[] h = coif.lowPassDecomposition();
        
        // Test orthogonality for even shifts
        for (int shift = 2; shift < h.length; shift += 2) {
            double dotProduct = 0.0;
            for (int i = 0; i < h.length - shift; i++) {
                dotProduct += h[i] * h[i + shift];
            }
            
            assertEquals(0.0, dotProduct, ORTHOGONALITY_TOLERANCE,
                String.format("COIF%d orthogonality violated for shift %d", order, shift));
        }
    }
    
    @Test
    @DisplayName("Verify COIF6 specific properties")
    void testCOIF6Properties() {
        Coiflet coif6 = Coiflet.COIF6;
        double[] h = coif6.lowPassDecomposition();
        
        // Check filter length
        assertEquals(36, h.length, "COIF6 should have 36 coefficients");
        
        // Check first few coefficients match expected values
        assertEquals(-5.3090884171968937E-09, h[0], 1e-20, "First coefficient mismatch");
        assertEquals(-8.4871433962624369E-09, h[1], 1e-20, "Second coefficient mismatch");
        
        // Check largest coefficients (should be in the middle)
        double maxCoeff = Arrays.stream(h).max().orElse(0);
        assertTrue(maxCoeff > 0.7, "COIF6 should have large central coefficients");
        assertTrue(maxCoeff < 0.8, "COIF6 max coefficient should be around 0.76");
    }
    
    @Test
    @DisplayName("Verify COIF10 specific properties")
    void testCOIF10Properties() {
        Coiflet coif10 = Coiflet.COIF10;
        double[] h = coif10.lowPassDecomposition();
        
        // Check filter length
        assertEquals(60, h.length, "COIF10 should have 60 coefficients");
        
        // Check vanishing moments
        assertEquals(20, coif10.vanishingMoments(), "COIF10 should have 20 vanishing moments");
        
        // Verify near-symmetry (Coiflets are nearly symmetric)
        double asymmetry = computeAsymmetry(h);
        assertTrue(asymmetry < 1.5, 
            String.format("COIF10 should be nearly symmetric, asymmetry: %.4f", asymmetry));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {6, 7, 8, 9, 10})
    @DisplayName("Test perfect reconstruction with MODWT")
    void testPerfectReconstruction(int order) {
        Coiflet coif = getCoiflet(order);
        MODWTTransform transform = new MODWTTransform(coif, BoundaryMode.PERIODIC);
        
        // Create test signal
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(2 * Math.PI * i / 16.0);
        }
        
        // Forward and inverse transform
        MODWTResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        // Check reconstruction error
        double maxError = 0.0;
        for (int i = 0; i < signal.length; i++) {
            maxError = Math.max(maxError, Math.abs(signal[i] - reconstructed[i]));
        }
        
        assertTrue(maxError < 1e-10,
            String.format("COIF%d reconstruction error too large: %.2e", order, maxError));
    }
    
    @Test
    @DisplayName("Compare approximation power: COIF3 vs DB10")
    void testApproximationPower() {
        // COIF3 has 18 coefficients, DB10 has 20 coefficients (similar lengths)
        // Coiflet should approximate polynomials better due to vanishing moments
        Coiflet coif3 = Coiflet.COIF3;
        Wavelet db10 = WaveletRegistry.getWavelet(WaveletName.DB10);
        
        // Create smooth polynomial signal
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            double x = (i - 128.0) / 64.0;
            signal[i] = x * x * x - 3 * x * x + 2 * x + 1; // Cubic polynomial
        }
        
        // Transform with both wavelets
        MODWTTransform coifTransform = new MODWTTransform(coif3, BoundaryMode.PERIODIC);
        MODWTTransform dbTransform = new MODWTTransform(db10, BoundaryMode.PERIODIC);
        
        MODWTResult coifResult = coifTransform.forward(signal);
        MODWTResult dbResult = dbTransform.forward(signal);
        
        // Coiflet should have more energy in approximation (less in details)
        double coifDetailEnergy = computeEnergy(coifResult.detailCoeffs());
        double dbDetailEnergy = computeEnergy(dbResult.detailCoeffs());
        
        // Coiflet should have less detail energy for smooth signals
        assertTrue(coifDetailEnergy <= dbDetailEnergy * 1.1, // Allow 10% margin
            String.format("COIF3 should approximate smooth signals better than DB10: %.4f vs %.4f",
                coifDetailEnergy, dbDetailEnergy));
    }
    
    @Test
    @DisplayName("Test registry access for extended Coiflets")
    void testRegistryAccess() {
        // Verify all extended Coiflets are accessible through registry
        for (int order = 6; order <= 10; order++) {
            WaveletName name = WaveletName.valueOf("COIF" + order);
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            
            assertNotNull(wavelet, String.format("COIF%d should be in registry", order));
            assertTrue(wavelet instanceof Coiflet,
                String.format("COIF%d should be Coiflet instance", order));
            
            Coiflet coif = (Coiflet) wavelet;
            assertEquals(order, coif.getOrder(),
                String.format("COIF%d order mismatch", order));
        }
    }
    
    @Test
    @DisplayName("Verify Coiflet coefficients are properly ordered")
    void testCoefficientOrdering() {
        // Coiflet coefficients should have small values at ends, large in middle
        for (int order = 6; order <= 10; order++) {
            Coiflet coif = getCoiflet(order);
            double[] h = coif.lowPassDecomposition();
            
            // Find index of maximum absolute value
            int maxIndex = 0;
            double maxAbs = 0.0;
            for (int i = 0; i < h.length; i++) {
                if (Math.abs(h[i]) > maxAbs) {
                    maxAbs = Math.abs(h[i]);
                    maxIndex = i;
                }
            }
            
            // Maximum should be roughly in the middle third
            int third = h.length / 3;
            assertTrue(maxIndex > third && maxIndex < 2 * third,
                String.format("COIF%d maximum coefficient should be near center, found at %d/%d",
                    order, maxIndex, h.length));
            
            // Edge coefficients should be small
            assertTrue(Math.abs(h[0]) < 1e-4,
                String.format("COIF%d first coefficient should be small: %.2e", order, h[0]));
            assertTrue(Math.abs(h[h.length-1]) < 1e-4,
                String.format("COIF%d last coefficient should be small: %.2e", order, h[h.length-1]));
        }
    }
    
    // Helper methods
    
    private Coiflet getCoiflet(int order) {
        switch (order) {
            case 6: return Coiflet.COIF6;
            case 7: return Coiflet.COIF7;
            case 8: return Coiflet.COIF8;
            case 9: return Coiflet.COIF9;
            case 10: return Coiflet.COIF10;
            default: throw new IllegalArgumentException("Unsupported Coiflet order: " + order);
        }
    }
    
    private double computeAsymmetry(double[] h) {
        int n = h.length;
        double asymmetry = 0.0;
        
        for (int i = 0; i < n / 2; i++) {
            double diff = Math.abs(h[i] - h[n - 1 - i]);
            asymmetry += diff * diff;
        }
        
        return Math.sqrt(asymmetry);
    }
    
    private double computeEnergy(double[] signal) {
        return Arrays.stream(signal).map(x -> x * x).sum();
    }
}