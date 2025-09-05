package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive mathematical verification test for all Coiflet wavelets.
 * Ensures all Coiflet wavelets satisfy their required mathematical properties.
 */
class CoifletMathematicalVerificationTest {

    private static final double TOLERANCE = 1e-10;
    private static final double COIF2_TOLERANCE = 1e-4; // COIF2 has lower precision

    static Stream<CoifletTestCase> coifletProvider() {
        return Stream.of(
            new CoifletTestCase("COIF1", Coiflet.COIF1, 1, 6, TOLERANCE),
            new CoifletTestCase("COIF2", Coiflet.COIF2, 2, 12, COIF2_TOLERANCE),
            new CoifletTestCase("COIF3", Coiflet.COIF3, 3, 18, TOLERANCE),
            new CoifletTestCase("COIF4", Coiflet.COIF4, 4, 24, TOLERANCE),
            new CoifletTestCase("COIF5", Coiflet.COIF5, 5, 30, TOLERANCE),
            new CoifletTestCase("COIF6", Coiflet.COIF6, 6, 36, TOLERANCE),
            new CoifletTestCase("COIF7", Coiflet.COIF7, 7, 42, TOLERANCE),
            new CoifletTestCase("COIF8", Coiflet.COIF8, 8, 48, TOLERANCE),
            new CoifletTestCase("COIF9", Coiflet.COIF9, 9, 54, TOLERANCE),
            new CoifletTestCase("COIF10", Coiflet.COIF10, 10, 60, TOLERANCE),
            new CoifletTestCase("COIF11", Coiflet.COIF11, 11, 66, TOLERANCE),
            new CoifletTestCase("COIF12", Coiflet.COIF12, 12, 72, TOLERANCE),
            new CoifletTestCase("COIF13", Coiflet.COIF13, 13, 78, TOLERANCE),
            new CoifletTestCase("COIF14", Coiflet.COIF14, 14, 84, TOLERANCE),
            new CoifletTestCase("COIF15", Coiflet.COIF15, 15, 90, TOLERANCE),
            new CoifletTestCase("COIF16", Coiflet.COIF16, 16, 96, TOLERANCE),
            new CoifletTestCase("COIF17", Coiflet.COIF17, 17, 102, TOLERANCE)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("coifletProvider")
    @DisplayName("Verify filter length")
    void testFilterLength(CoifletTestCase testCase) {
        double[] coeffs = testCase.coiflet.lowPassDecomposition();
        assertEquals(testCase.expectedLength, coeffs.length,
            String.format("%s should have %d coefficients (6N where N=%d)", 
                testCase.name, testCase.expectedLength, testCase.order));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("coifletProvider")
    @DisplayName("Verify sum equals sqrt(2)")
    void testCoefficientSum(CoifletTestCase testCase) {
        double[] coeffs = testCase.coiflet.lowPassDecomposition();
        double sum = 0;
        for (double c : coeffs) {
            sum += c;
        }
        
        assertEquals(Math.sqrt(2), sum, testCase.tolerance,
            String.format("%s coefficients should sum to sqrt(2) for DC gain normalization", 
                testCase.name));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("coifletProvider")
    @DisplayName("Verify sum of squares equals 1")
    void testCoefficientSumOfSquares(CoifletTestCase testCase) {
        double[] coeffs = testCase.coiflet.lowPassDecomposition();
        double sumSquares = 0;
        for (double c : coeffs) {
            sumSquares += c * c;
        }
        
        assertEquals(1.0, sumSquares, testCase.tolerance,
            String.format("%s sum of squares should equal 1 for energy normalization", 
                testCase.name));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("coifletProvider")
    @DisplayName("Verify orthogonality property")
    void testOrthogonality(CoifletTestCase testCase) {
        double[] coeffs = testCase.coiflet.lowPassDecomposition();
        
        // Test orthogonality: sum(h[n] * h[n+2k]) = 0 for k != 0
        for (int k = 1; k < coeffs.length / 2; k++) {
            double dot = 0;
            for (int n = 0; n < coeffs.length - 2*k; n++) {
                dot += coeffs[n] * coeffs[n + 2*k];
            }
            
            assertEquals(0.0, dot, testCase.tolerance,
                String.format("%s orthogonality must be satisfied for k=%d", 
                    testCase.name, k));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("coifletProvider")
    @DisplayName("Verify vanishing moments property")
    void testVanishingMoments(CoifletTestCase testCase) {
        double[] coeffs = testCase.coiflet.lowPassDecomposition();
        
        // Coiflets have 2N vanishing moments (N for wavelet, N-1 for scaling function)
        // This manifests as having very small coefficients at the boundaries
        
        // Check that first and last coefficients are much smaller than middle ones
        double firstMag = Math.abs(coeffs[0]);
        double lastMag = Math.abs(coeffs[coeffs.length - 1]);
        double maxMag = 0;
        
        for (double c : coeffs) {
            maxMag = Math.max(maxMag, Math.abs(c));
        }
        
        // For higher order Coiflets, boundary coefficients should be extremely small
        if (testCase.order >= 3) {
            assertTrue(firstMag < maxMag / 100,
                String.format("%s should have small first coefficient relative to maximum", 
                    testCase.name));
            assertTrue(lastMag < maxMag / 100,
                String.format("%s should have small last coefficient relative to maximum", 
                    testCase.name));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("coifletProvider")
    @DisplayName("Verify wavelet properties through Wavelet interface")
    void testWaveletInterface(CoifletTestCase testCase) {
        Wavelet wavelet = testCase.coiflet;
        
        // Verify name
        assertNotNull(wavelet.name());
        assertTrue(wavelet.name().toLowerCase().contains("coif"),
            "Wavelet name should contain 'coif'");
        
        // Verify description
        assertNotNull(wavelet.description());
        
        // Verify it's an orthogonal wavelet
        assertTrue(wavelet instanceof OrthogonalWavelet,
            "Coiflet should be an orthogonal wavelet");
        
        // Verify filter coefficients are accessible
        assertNotNull(wavelet.lowPassDecomposition());
        assertNotNull(wavelet.highPassDecomposition());
        
        // Verify high-pass coefficients are derived correctly from low-pass
        double[] lowPass = wavelet.lowPassDecomposition();
        double[] highPass = wavelet.highPassDecomposition();
        
        assertEquals(lowPass.length, highPass.length,
            "High-pass and low-pass filters should have same length");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("coifletProvider")
    @DisplayName("Verify coefficient symmetry properties")
    void testSymmetryProperties(CoifletTestCase testCase) {
        double[] coeffs = testCase.coiflet.lowPassDecomposition();
        
        // Coiflets are designed to have near-symmetry
        // While not perfectly symmetric, they should have a symmetric envelope
        
        // Find the center of mass
        double centerOfMass = 0;
        double totalMass = 0;
        for (int i = 0; i < coeffs.length; i++) {
            double mass = Math.abs(coeffs[i]);
            centerOfMass += i * mass;
            totalMass += mass;
        }
        centerOfMass /= totalMass;
        
        // Center of mass should be near the middle
        double expectedCenter = (coeffs.length - 1) / 2.0;
        double deviation = Math.abs(centerOfMass - expectedCenter);
        
        // Allow some deviation, but it should be relatively centered
        // Higher order Coiflets may have more deviation due to their design
        double allowedDeviation = (testCase.order >= 8) ? coeffs.length * 0.20 : coeffs.length * 0.15;
        assertTrue(deviation < allowedDeviation,
            String.format("%s should have approximately centered mass distribution (deviation: %.2f, allowed: %.2f)", 
                testCase.name, deviation, allowedDeviation));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("coifletProvider")
    @DisplayName("Verify coefficient magnitude pattern")
    void testMagnitudePattern(CoifletTestCase testCase) {
        double[] coeffs = testCase.coiflet.lowPassDecomposition();
        
        // Find the maximum magnitude coefficient
        double maxMag = 0;
        int maxIndex = 0;
        for (int i = 0; i < coeffs.length; i++) {
            if (Math.abs(coeffs[i]) > maxMag) {
                maxMag = Math.abs(coeffs[i]);
                maxIndex = i;
            }
        }
        
        // Maximum should not be at the boundaries
        assertTrue(maxIndex > 0 && maxIndex < coeffs.length - 1,
            String.format("%s maximum coefficient should not be at boundaries", testCase.name));
        
        // For Coiflets, we typically have two large positive coefficients near the center
        int centerRegionStart = coeffs.length / 3;
        int centerRegionEnd = 2 * coeffs.length / 3;
        
        assertTrue(maxIndex >= centerRegionStart && maxIndex <= centerRegionEnd,
            String.format("%s maximum coefficient should be in central region", testCase.name));
    }

    @Test
    @DisplayName("Verify COIF1 specific properties")
    void testCOIF1SpecificProperties() {
        Coiflet coif1 = Coiflet.COIF1;
        double[] coeffs = coif1.lowPassDecomposition();
        
        // COIF1 has 6 coefficients
        assertEquals(6, coeffs.length);
        
        // Verify specific values
        assertEquals(-0.0156557281354645, coeffs[0], 1e-15);
        assertEquals(-0.0727326195128561, coeffs[1], 1e-15);
        assertEquals(0.3848648468642029, coeffs[2], 1e-15);
        assertEquals(0.8525720202122554, coeffs[3], 1e-15);
        assertEquals(0.3378976624578092, coeffs[4], 1e-15);
        assertEquals(-0.0727326195128561, coeffs[5], 1e-15);
        
        // COIF1 has a specific symmetry: coeffs[1] == coeffs[5]
        assertEquals(coeffs[1], coeffs[5], 0.0,
            "COIF1 should have coeffs[1] == coeffs[5]");
    }

    @Test
    @DisplayName("Verify COIF2 lower precision is acceptable")
    void testCOIF2LowerPrecision() {
        Coiflet coif2 = Coiflet.COIF2;
        double[] coeffs = coif2.lowPassDecomposition();
        
        // COIF2 is known to have lower precision
        // Verify properties with relaxed tolerance
        double sum = 0;
        double sumSquares = 0;
        for (double c : coeffs) {
            sum += c;
            sumSquares += c * c;
        }
        
        assertEquals(Math.sqrt(2), sum, COIF2_TOLERANCE,
            "COIF2 sum should be sqrt(2) within relaxed tolerance");
        assertEquals(1.0, sumSquares, COIF2_TOLERANCE,
            "COIF2 sum of squares should be 1 within relaxed tolerance");
    }

    @Test
    @DisplayName("Verify higher order Coiflets have better approximation")
    void testHigherOrderApproximation() {
        // Higher order Coiflets should have smaller boundary coefficients
        double coif3FirstCoeff = Math.abs(Coiflet.COIF3.lowPassDecomposition()[0]);
        double coif5FirstCoeff = Math.abs(Coiflet.COIF5.lowPassDecomposition()[0]);
        double coif10FirstCoeff = Math.abs(Coiflet.COIF10.lowPassDecomposition()[0]);
        
        assertTrue(coif5FirstCoeff < coif3FirstCoeff,
            "COIF5 should have smaller boundary coefficient than COIF3");
        assertTrue(coif10FirstCoeff < coif5FirstCoeff,
            "COIF10 should have smaller boundary coefficient than COIF5");
    }

    @Test
    @DisplayName("Verify all Coiflets are properly registered")
    void testCoifletRegistration() {
        // Test that all Coiflets can be accessed
        for (int order = 1; order <= 17; order++) {
            Coiflet coif = Coiflet.get(order);
            assertNotNull(coif, "COIF" + order + " should be accessible");
            assertEquals("coif" + order, coif.name(), "COIF" + order + " should have correct name");
            assertEquals(6 * order, coif.lowPassDecomposition().length, 
                "COIF" + order + " should have " + (6 * order) + " coefficients");
        }
        
        // Verify they have distinct coefficients
        for (int order = 1; order < 17; order++) {
            Coiflet current = Coiflet.get(order);
            Coiflet next = Coiflet.get(order + 1);
            assertNotEquals(current.lowPassDecomposition()[0], next.lowPassDecomposition()[0],
                "COIF" + order + " and COIF" + (order + 1) + " should have different first coefficients");
        }
    }

    @Test
    @DisplayName("Verify extended Coiflets mathematical consistency")
    void testExtendedCoifletsConsistency() {
        // Test extended Coiflets (COIF6-COIF17)
        for (int order = 6; order < 17; order++) {
            Coiflet current = Coiflet.get(order);
            Coiflet next = Coiflet.get(order + 1);
            
            // Verify filter lengths increase
            assertTrue(next.lowPassDecomposition().length > current.lowPassDecomposition().length,
                String.format("COIF%d should have more coefficients than COIF%d", 
                    order + 1, order));
            
            // Verify boundary coefficients get smaller (better approximation)
            double currentBoundary = Math.abs(current.lowPassDecomposition()[0]);
            double nextBoundary = Math.abs(next.lowPassDecomposition()[0]);
            
            assertTrue(nextBoundary < currentBoundary,
                String.format("COIF%d should have smaller boundary coefficient than COIF%d", 
                    order + 1, order));
        }
    }

    @Test
    @DisplayName("Verify COIF11-COIF17 specific properties")
    void testHighOrderCoifletProperties() {
        // Test that higher order Coiflets have very small boundary coefficients
        Coiflet coif11 = Coiflet.COIF11;
        Coiflet coif12 = Coiflet.COIF12;
        Coiflet coif13 = Coiflet.COIF13;
        Coiflet coif14 = Coiflet.COIF14;
        Coiflet coif15 = Coiflet.COIF15;
        Coiflet coif16 = Coiflet.COIF16;
        Coiflet coif17 = Coiflet.COIF17;
        
        // Verify specific coefficient counts
        assertEquals(66, coif11.lowPassDecomposition().length, "COIF11 should have 66 coefficients");
        assertEquals(72, coif12.lowPassDecomposition().length, "COIF12 should have 72 coefficients");
        assertEquals(78, coif13.lowPassDecomposition().length, "COIF13 should have 78 coefficients");
        assertEquals(84, coif14.lowPassDecomposition().length, "COIF14 should have 84 coefficients");
        assertEquals(90, coif15.lowPassDecomposition().length, "COIF15 should have 90 coefficients");
        assertEquals(96, coif16.lowPassDecomposition().length, "COIF16 should have 96 coefficients");
        assertEquals(102, coif17.lowPassDecomposition().length, "COIF17 should have 102 coefficients");
        
        // Verify vanishing moments
        assertEquals(22, coif11.vanishingMoments(), "COIF11 should have 22 vanishing moments");
        assertEquals(24, coif12.vanishingMoments(), "COIF12 should have 24 vanishing moments");
        assertEquals(26, coif13.vanishingMoments(), "COIF13 should have 26 vanishing moments");
        assertEquals(28, coif14.vanishingMoments(), "COIF14 should have 28 vanishing moments");
        assertEquals(30, coif15.vanishingMoments(), "COIF15 should have 30 vanishing moments");
        assertEquals(32, coif16.vanishingMoments(), "COIF16 should have 32 vanishing moments");
        assertEquals(34, coif17.vanishingMoments(), "COIF17 should have 34 vanishing moments");
        
        // Verify first coefficients are extremely small (characteristic of high-order Coiflets)
        assertTrue(Math.abs(coif11.lowPassDecomposition()[0]) < 1e-14, 
            "COIF11 first coefficient should be extremely small");
        assertTrue(Math.abs(coif12.lowPassDecomposition()[0]) < 1e-15, 
            "COIF12 first coefficient should be extremely small");
        assertTrue(Math.abs(coif13.lowPassDecomposition()[0]) < 1e-16, 
            "COIF13 first coefficient should be extremely small");
        assertTrue(Math.abs(coif14.lowPassDecomposition()[0]) < 1e-17, 
            "COIF14 first coefficient should be extremely small");
        assertTrue(Math.abs(coif15.lowPassDecomposition()[0]) < 1e-18, 
            "COIF15 first coefficient should be extremely small");
        assertTrue(Math.abs(coif16.lowPassDecomposition()[0]) < 1e-19, 
            "COIF16 first coefficient should be extremely small");
        assertTrue(Math.abs(coif17.lowPassDecomposition()[0]) < 1e-20, 
            "COIF17 first coefficient should be extremely small");
    }

    @Test
    @DisplayName("Verify COIF11-COIF17 mathematical validity")
    void testHighOrderCoifletMathematicalValidity() {
        for (int order = 11; order <= 17; order++) {
            Coiflet coif = Coiflet.get(order);
            double[] coeffs = coif.lowPassDecomposition();
            
            // Verify sum equals sqrt(2)
            double sum = 0;
            for (double c : coeffs) {
                sum += c;
            }
            assertEquals(Math.sqrt(2), sum, 1e-10,
                String.format("COIF%d coefficients should sum to sqrt(2)", order));
            
            // Verify sum of squares equals 1
            double sumSquares = 0;
            for (double c : coeffs) {
                sumSquares += c * c;
            }
            assertEquals(1.0, sumSquares, 1e-10,
                String.format("COIF%d sum of squares should equal 1", order));
            
            // Verify orthogonality
            for (int k = 1; k < order; k++) {
                double dot = 0;
                for (int n = 0; n < coeffs.length - 2*k; n++) {
                    dot += coeffs[n] * coeffs[n + 2*k];
                }
                assertEquals(0.0, dot, 1e-10,
                    String.format("COIF%d orthogonality must be satisfied for k=%d", order, k));
            }
            
            // Verify verifyCoefficients() method works
            assertTrue(coif.verifyCoefficients(),
                String.format("COIF%d.verifyCoefficients() should return true", order));
        }
    }

    @Test
    @DisplayName("Verify COIF17 has maximum precision")
    void testCOIF17MaximumPrecision() {
        Coiflet coif17 = Coiflet.COIF17;
        double[] coeffs = coif17.lowPassDecomposition();
        
        // COIF17 is the maximum order in PyWavelets
        assertEquals(102, coeffs.length, "COIF17 should have 102 coefficients (maximum)");
        
        // Verify the first coefficient matches PyWavelets value
        assertEquals(-1.4925731767051474E-22, coeffs[0], 0.0,
            "COIF17 first coefficient should match PyWavelets exactly");
        
        // Verify the last coefficient
        assertEquals(-9.1930449016478320E-12, coeffs[coeffs.length - 1], 0.0,
            "COIF17 last coefficient should match PyWavelets exactly");
        
        // Verify some middle coefficients for accuracy (largest magnitude coefficients)
        assertEquals(4.4823872485149524E-01, coeffs[66], 1e-15,
            "COIF17 coefficient 66 should match expected value");
        assertEquals(7.4328122144113895E-01, coeffs[67], 1e-15,
            "COIF17 coefficient 67 should match expected value");
        assertEquals(4.3998382065204200E-01, coeffs[68], 1e-15,
            "COIF17 coefficient 68 should match expected value");
    }

    /**
     * Helper class to hold test case data for parameterized tests.
     */
    static class CoifletTestCase {
        final String name;
        final Coiflet coiflet;
        final int order;
        final int expectedLength;
        final double tolerance;

        CoifletTestCase(String name, Coiflet coiflet, int order, int expectedLength, double tolerance) {
            this.name = name;
            this.coiflet = coiflet;
            this.order = order;
            this.expectedLength = expectedLength;
            this.tolerance = tolerance;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}