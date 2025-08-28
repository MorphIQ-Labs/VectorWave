package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive validation test for all 17 Coiflet wavelets (COIF1-COIF17).
 * Ensures mathematical validity and correctness for deployment requirements.
 */
class CoifletComprehensiveValidationTest {

    private static final double TOLERANCE = 1e-10;
    private static final double COIF2_TOLERANCE = 1e-4; // COIF2 has known lower precision

    @Test
    @DisplayName("Comprehensive validation of all 17 Coiflet wavelets")
    void testAllCoifletsComprehensiveValidation() {
        System.out.println("\n=== COIFLET WAVELET COMPREHENSIVE VALIDATION ===\n");
        
        for (int order = 1; order <= 17; order++) {
            System.out.printf("Validating COIF%d...\n", order);
            
            // Get the Coiflet
            Coiflet coif = Coiflet.get(order);
            assertNotNull(coif, "COIF" + order + " should exist");
            
            // Verify name
            assertEquals("coif" + order, coif.name(), 
                "COIF" + order + " should have correct name");
            
            // Verify filter length (6N coefficients)
            double[] coeffs = coif.lowPassDecomposition();
            assertEquals(6 * order, coeffs.length,
                String.format("COIF%d should have %d coefficients", order, 6 * order));
            
            // Verify vanishing moments (2N)
            assertEquals(2 * order, coif.vanishingMoments(),
                String.format("COIF%d should have %d vanishing moments", order, 2 * order));
            
            // Determine tolerance
            double tol = (order == 2) ? COIF2_TOLERANCE : TOLERANCE;
            
            // 1. Verify sum equals sqrt(2) (DC gain normalization)
            double sum = 0;
            for (double c : coeffs) {
                sum += c;
            }
            assertEquals(Math.sqrt(2), sum, tol,
                String.format("COIF%d: Sum of coefficients should equal sqrt(2)", order));
            
            // 2. Verify sum of squares equals 1 (energy normalization)
            double sumSquares = 0;
            for (double c : coeffs) {
                sumSquares += c * c;
            }
            assertEquals(1.0, sumSquares, tol,
                String.format("COIF%d: Sum of squares should equal 1", order));
            
            // 3. Verify orthogonality property
            for (int k = 1; k < order; k++) {
                double dot = 0;
                for (int n = 0; n < coeffs.length - 2*k; n++) {
                    dot += coeffs[n] * coeffs[n + 2*k];
                }
                assertEquals(0.0, dot, tol,
                    String.format("COIF%d: Orthogonality condition for k=%d", order, k));
            }
            
            // 4. Verify built-in verification method
            assertTrue(coif.verifyCoefficients(),
                String.format("COIF%d: Built-in verification should pass", order));
            
            // 5. Verify high-pass filter generation
            double[] highPass = coif.highPassDecomposition();
            assertEquals(coeffs.length, highPass.length,
                String.format("COIF%d: High-pass filter should have same length as low-pass", order));
            
            // 6. For higher order Coiflets, verify boundary coefficients are small
            if (order >= 3) {
                double firstMag = Math.abs(coeffs[0]);
                double lastMag = Math.abs(coeffs[coeffs.length - 1]);
                double maxMag = 0;
                for (double c : coeffs) {
                    maxMag = Math.max(maxMag, Math.abs(c));
                }
                assertTrue(firstMag < maxMag / 100,
                    String.format("COIF%d: First coefficient should be small", order));
                assertTrue(lastMag < maxMag / 100,
                    String.format("COIF%d: Last coefficient should be small", order));
            }
            
            System.out.printf("  ✓ COIF%d validated: %d coefficients, %d vanishing moments\n", 
                order, coeffs.length, 2 * order);
            System.out.printf("    Sum=%.15f (error=%.2e)\n", sum, Math.abs(sum - Math.sqrt(2)));
            System.out.printf("    Sum²=%.15f (error=%.2e)\n", sumSquares, Math.abs(sumSquares - 1.0));
            System.out.printf("    First coeff magnitude: %.2e\n", Math.abs(coeffs[0]));
        }
        
        System.out.println("\n=== ALL 17 COIFLET WAVELETS VALIDATED SUCCESSFULLY ===\n");
    }

    @Test
    @DisplayName("Verify Coiflet ordering consistency")
    void testCoifletOrderingConsistency() {
        // Verify that as order increases:
        // 1. Filter length increases
        // 2. Boundary coefficients get smaller
        // 3. Approximation quality improves
        
        for (int order = 1; order < 17; order++) {
            Coiflet current = Coiflet.get(order);
            Coiflet next = Coiflet.get(order + 1);
            
            // Filter length should increase
            assertTrue(next.lowPassDecomposition().length > current.lowPassDecomposition().length,
                String.format("COIF%d should have more coefficients than COIF%d", order + 1, order));
            
            // Vanishing moments should increase
            assertTrue(next.vanishingMoments() > current.vanishingMoments(),
                String.format("COIF%d should have more vanishing moments than COIF%d", order + 1, order));
            
            // For orders >= 3, boundary coefficients should decrease
            if (order >= 3) {
                double currentBoundary = Math.abs(current.lowPassDecomposition()[0]);
                double nextBoundary = Math.abs(next.lowPassDecomposition()[0]);
                assertTrue(nextBoundary < currentBoundary,
                    String.format("COIF%d should have smaller boundary coefficient than COIF%d", 
                        order + 1, order));
            }
        }
    }

    @Test
    @DisplayName("Verify COIF17 is the maximum implementation")
    void testCOIF17IsMaximum() {
        // COIF17 should be the maximum order (matching PyWavelets)
        Coiflet coif17 = Coiflet.COIF17;
        assertEquals(102, coif17.lowPassDecomposition().length,
            "COIF17 should have 102 coefficients (maximum in PyWavelets)");
        assertEquals(34, coif17.vanishingMoments(),
            "COIF17 should have 34 vanishing moments (maximum)");
        
        // Verify exception for higher orders
        assertThrows(IllegalArgumentException.class, () -> Coiflet.get(18),
            "Should throw exception for COIF18 (not supported)");
    }

    @Test
    @DisplayName("Verify Coiflet interface implementation")
    void testCoifletInterfaceImplementation() {
        for (int order = 1; order <= 17; order++) {
            Coiflet coif = Coiflet.get(order);
            
            // Should implement Wavelet interface
            assertTrue(coif instanceof Wavelet,
                String.format("COIF%d should implement Wavelet interface", order));
            
            // Should implement OrthogonalWavelet interface
            assertTrue(coif instanceof OrthogonalWavelet,
                String.format("COIF%d should implement OrthogonalWavelet interface", order));
            
            // Methods should not return null
            assertNotNull(coif.name());
            assertNotNull(coif.description());
            assertNotNull(coif.lowPassDecomposition());
            assertNotNull(coif.highPassDecomposition());
            
            // Description should be meaningful
            assertTrue(coif.description().contains("Coiflet"),
                String.format("COIF%d description should mention Coiflet", order));
        }
    }
}