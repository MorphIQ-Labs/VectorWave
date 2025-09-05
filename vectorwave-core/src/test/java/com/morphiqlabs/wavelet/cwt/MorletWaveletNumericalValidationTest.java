package com.morphiqlabs.wavelet.cwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive numerical validation tests for Morlet wavelet implementations.
 * 
 * <p>This class validates the mathematical correctness of the Morlet wavelet
 * implementations against canonical formulations from literature and
 * reference implementations.</p>
 * 
 * <p><strong>Canonical References:</strong></p>
 * <ul>
 *   <li>Mallat, S. (2008). "A Wavelet Tour of Signal Processing" (3rd ed., pp. 88-91)</li>
 *   <li>Addison, P. S. (2002). "The Illustrated Wavelet Transform Handbook" (pp. 50-58)</li>
 *   <li>Morlet, J. et al. (1982). "Wave propagation and sampling theory." Geophysics, 47(2), 203-221</li>
 * </ul>
 */
@DisplayName("Morlet Wavelet Numerical Validation")
class MorletWaveletNumericalValidationTest {
    
    private static final double STRICT_TOLERANCE = 1e-12;
    private static final double RELAXED_TOLERANCE = 1e-6;
    private static final double REFERENCE_TOLERANCE = 1e-8;

    @Test
    @DisplayName("Validate canonical normalization constant")
    void testCanonicalNormalizationConstant() {
        // For standard Morlet with σ=1, normalization should be 1/π^(1/4)
        double expectedNormalization = 1.0 / Math.pow(Math.PI, 0.25);
        assertEquals(0.7511255444649425, expectedNormalization, STRICT_TOLERANCE);
        
        // For σ ≠ 1, check that the wavelet at t=0 follows expected scaling
        double sigma = 2.0;
        MorletWavelet waveletSigma2 = new MorletWavelet(6.0, sigma);
        double actualValue = waveletSigma2.psi(0);
        assertEquals(0.531125966013598, actualValue, REFERENCE_TOLERANCE);
    }

    @Test
    @DisplayName("Validate standard Morlet wavelet value at t=0")
    void testStandardMorletAtZero() {
        MorletWavelet wavelet = new MorletWavelet(); // ω₀=6, σ=1
        double psi0 = wavelet.psi(0);
        
        // At t=0: ψ(0) = C * (1 - K) where C = 1/π^(1/4), K = exp(-18)
        double C = 1.0 / Math.pow(Math.PI, 0.25);
        double K = Math.exp(-0.5 * 6.0 * 6.0 * 1.0 * 1.0); // exp(-18)
        double expected = C * (1.0 - K);
        
        assertEquals(expected, psi0, STRICT_TOLERANCE);
        
        // Verify against computed reference value
        assertEquals(0.751125533025316, psi0, REFERENCE_TOLERANCE);
    }

    @Test
    @DisplayName("Validate admissibility condition (zero mean)")
    void testAdmissibilityCondition() {
        MorletWavelet wavelet = new MorletWavelet();
        
        // Sample the wavelet over a wide range
        double integral = 0.0;
        double dt = 0.01;
        double range = 10.0; // ±10σ should be sufficient
        
        for (double t = -range; t <= range; t += dt) {
            integral += wavelet.psi(t) * dt;
        }
        
        // The integral should be very close to zero (admissibility condition)
        assertEquals(0.0, integral, 1e-4);
    }

    @Test
    @DisplayName("Validate real part energy (should be 0.5 for real part of complex wavelet)")
    void testRealPartEnergyNormalization() {
        MorletWavelet wavelet = new MorletWavelet();
        
        // Calculate ∫|ψ_real(t)|² dt for the real part of a complex wavelet
        double energy = 0.0;
        double dt = 0.01;
        double range = 8.0; // ±8σ should capture >99.99% of energy
        
        for (double t = -range; t <= range; t += dt) {
            double psi = wavelet.psi(t);
            energy += psi * psi * dt;
        }
        
        // For the real part of a unit-energy complex wavelet, energy should be ~0.5
        // (the imaginary part would have the other 0.5)
        assertEquals(0.5, energy, 0.01);
    }

    @Test
    @DisplayName("Validate Gaussian envelope decay")
    void testGaussianEnvelopeDecay() {
        MorletWavelet wavelet = new MorletWavelet(); // σ=1
        
        // Test decay at standard deviations
        double psi0 = Math.abs(wavelet.psi(0));
        double psi1 = Math.abs(wavelet.psi(1)); // 1σ
        double psi2 = Math.abs(wavelet.psi(2)); // 2σ
        double psi3 = Math.abs(wavelet.psi(3)); // 3σ
        
        // Should follow approximately exp(-t²/2) decay
        assertTrue(psi0 > psi1);
        assertTrue(psi1 > psi2);
        assertTrue(psi2 > psi3);
        
        // Verify approximate decay ratios
        double ratio1 = psi1 / psi0;
        double expectedRatio1 = Math.exp(-0.5); // e^(-1/2) for 1σ
        assertEquals(expectedRatio1, ratio1, 0.1); // Allow 10% tolerance for oscillations
    }

    @Test
    @DisplayName("Validate center frequency property")
    void testCenterFrequencyProperty() {
        double omega0 = 5.0;
        MorletWavelet wavelet = new MorletWavelet(omega0, 1.0);
        
        double expectedCenterFreq = omega0 / (2.0 * Math.PI);
        assertEquals(expectedCenterFreq, wavelet.centerFrequency(), STRICT_TOLERANCE);
    }

    @Test
    @DisplayName("Validate complex part properties")
    void testComplexPartProperties() {
        MorletWavelet wavelet = new MorletWavelet();
        
        // Imaginary part at t=0 should be 0
        assertEquals(0.0, wavelet.psiImaginary(0), STRICT_TOLERANCE);
        
        // Complex magnitude should follow Gaussian envelope
        for (double t : new double[]{0, 0.5, 1.0, 1.5, 2.0}) {
            double real = wavelet.psi(t);
            double imag = wavelet.psiImaginary(t);
            double magnitude = Math.sqrt(real * real + imag * imag);
            
            // Compare with pure Gaussian envelope (without correction)
            double gaussianEnvelope = (1.0 / Math.pow(Math.PI, 0.25)) * Math.exp(-0.5 * t * t);
            
            // Should be close but not exact due to correction term
            assertTrue(Math.abs(magnitude - gaussianEnvelope) < 0.1);
        }
    }

    @Test
    @DisplayName("Validate symmetry properties")
    void testSymmetryProperties() {
        MorletWavelet wavelet = new MorletWavelet();
        
        double[] testPoints = {0.5, 1.0, 1.5, 2.0, 2.5};
        
        for (double t : testPoints) {
            // Real part should be symmetric
            assertEquals(wavelet.psi(t), wavelet.psi(-t), RELAXED_TOLERANCE);
            
            // Imaginary part should be antisymmetric
            assertEquals(-wavelet.psiImaginary(t), wavelet.psiImaginary(-t), RELAXED_TOLERANCE);
        }
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 1.0, 1.5, 2.0, 3.0})
    @DisplayName("Validate different sigma values")
    void testDifferentSigmaValues(double sigma) {
        MorletWavelet wavelet = new MorletWavelet(6.0, sigma);
        
        // Normalization factor should scale with σ^(-1/2)
        double expectedNormFactor = 1.0 / (Math.pow(Math.PI, 0.25) * Math.sqrt(sigma));
        
        // Test at t=0 to avoid oscillation effects
        double psi0 = wavelet.psi(0);
        double correction = Math.exp(-0.5 * 36.0 * sigma * sigma);
        double expectedPsi0 = expectedNormFactor * (1.0 - correction);
        
        assertEquals(expectedPsi0, psi0, REFERENCE_TOLERANCE);
    }

    @ParameterizedTest
    @ValueSource(doubles = {3.0, 4.0, 5.0, 6.0, 7.0, 8.0})
    @DisplayName("Validate different omega0 values")
    void testDifferentOmega0Values(double omega0) {
        MorletWavelet wavelet = new MorletWavelet(omega0, 1.0);
        
        // Center frequency should be ω₀/(2π)
        double expectedCenterFreq = omega0 / (2.0 * Math.PI);
        assertEquals(expectedCenterFreq, wavelet.centerFrequency(), STRICT_TOLERANCE);
        
        // Correction term should be exp(-ω₀²σ²/2)
        double expectedCorrection = Math.exp(-0.5 * omega0 * omega0);
        
        // At t=0, can verify correction term indirectly
        double psi0 = wavelet.psi(0);
        double normFactor = 1.0 / Math.pow(Math.PI, 0.25);
        double expectedPsi0 = normFactor * (1.0 - expectedCorrection);
        
        assertEquals(expectedPsi0, psi0, REFERENCE_TOLERANCE);
    }

    @Test
    @DisplayName("Validate discretization properties")
    void testDiscretizationProperties() {
        MorletWavelet wavelet = new MorletWavelet();
        
        int[] sizes = {16, 32, 64, 128};
        
        for (int size : sizes) {
            double[] coeffs = wavelet.discretize(size);
            
            // Length should match
            assertEquals(size, coeffs.length);
            
            // Should be normalized (sum of squares = 1)
            double sumSquares = 0.0;
            for (double c : coeffs) {
                sumSquares += c * c;
            }
            assertEquals(1.0, sumSquares, RELAXED_TOLERANCE);
            
            // Should be symmetric (real part)
            for (int i = 0; i < size / 2; i++) {
                assertEquals(coeffs[i], coeffs[size - 1 - i], RELAXED_TOLERANCE);
            }
        }
    }

    @Test
    @DisplayName("Compare with reference implementation values")
    void testReferenceImplementationValues() {
        // These values are computed using the exact mathematical formula
        // from the current implementation (which is mathematically correct)
        
        MorletWavelet wavelet = new MorletWavelet(6.0, 1.0);
        
        // Reference values computed with high precision from the implementation
        assertEquals(0.751125533025316, wavelet.psi(0), 1e-12);
        assertEquals(0.0, wavelet.psiImaginary(0), 1e-15);
        
        // Values at t=0.5
        assertEquals(-0.656232343125293, wavelet.psi(0.5), 1e-12);
        assertEquals(0.093543650526974, wavelet.psiImaginary(0.5), 1e-12);
        
        // Values at t=1.0
        assertEquals(0.437435017499003, wavelet.psi(1.0), 1e-12);
        assertEquals(-0.127296300439848, wavelet.psiImaginary(1.0), 1e-12);
    }

    @Test
    @DisplayName("Validate extreme parameter robustness")
    void testExtremeParameterRobustness() {
        // Test very small omega0
        MorletWavelet w1 = new MorletWavelet(0.1, 1.0);
        assertFalse(Double.isNaN(w1.psi(0)));
        assertFalse(Double.isInfinite(w1.psi(0)));
        assertNotEquals(0.0, w1.psi(0), "Very small omega0 should not produce zero");
        
        // Test very large omega0 (correction term becomes important)
        MorletWavelet w2 = new MorletWavelet(20.0, 1.0);
        assertFalse(Double.isNaN(w2.psi(0)));
        assertFalse(Double.isInfinite(w2.psi(0)));
        assertNotEquals(0.0, w2.psi(0), "Very large omega0 should not produce zero");
        
        // Test very small sigma (very narrow)
        MorletWavelet w3 = new MorletWavelet(6.0, 0.01);
        assertFalse(Double.isNaN(w3.psi(0)));
        assertFalse(Double.isInfinite(w3.psi(0)));
        assertNotEquals(0.0, w3.psi(0), "Very small sigma should not produce zero");
        
        // Test very large sigma (very wide)
        MorletWavelet w4 = new MorletWavelet(6.0, 100.0);
        assertFalse(Double.isNaN(w4.psi(0)));
        assertFalse(Double.isInfinite(w4.psi(0)));
        assertNotEquals(0.0, w4.psi(0), "Very large sigma should not produce zero");
    }

    @Test
    @DisplayName("Validate correction term effectiveness for different omega0*sigma products")
    void testCorrectionTermEffectiveness() {
        // For large omega0*sigma, correction term should be negligible
        double omega0Large = 10.0;
        double sigmaLarge = 2.0;
        MorletWavelet w1 = new MorletWavelet(omega0Large, sigmaLarge);
        double correctionTermLarge = Math.exp(-0.5 * omega0Large * omega0Large * sigmaLarge * sigmaLarge);
        
        // Correction should be negligible for large omega0*sigma
        assertTrue(correctionTermLarge < 1e-10, 
            String.format("Correction term (%.2e) should be negligible for omega0*sigma=%.1f", 
                correctionTermLarge, omega0Large * sigmaLarge));
        
        // For small omega0*sigma, correction term becomes important
        double omega0Small = 1.0;
        double sigmaSmall = 1.0;
        MorletWavelet w2 = new MorletWavelet(omega0Small, sigmaSmall);
        double correctionTermSmall = Math.exp(-0.5 * omega0Small * omega0Small * sigmaSmall * sigmaSmall);
        
        // Correction should be significant for small omega0*sigma
        assertTrue(correctionTermSmall > 0.1, 
            String.format("Correction term (%.2e) should be significant for omega0*sigma=%.1f", 
                correctionTermSmall, omega0Small * sigmaSmall));
        
        // Verify that the correction term actually affects the wavelet values
        double psi0WithCorrection = w2.psi(0);
        double normFactor = 1.0 / (Math.pow(Math.PI, 0.25) * Math.sqrt(sigmaSmall));
        double psi0WithoutCorrection = normFactor; // What it would be without correction
        
        // The difference should be noticeable for small omega0*sigma
        double difference = Math.abs(psi0WithoutCorrection - psi0WithCorrection);
        assertTrue(difference > 0.01, "Correction term should have noticeable effect for small omega0*sigma");
    }

    @Test
    @DisplayName("Validate complex magnitude decay properties")
    void testComplexMagnitudeDecayProperties() {
        MorletWavelet wavelet = new MorletWavelet();
        
        double[] testPoints = {-3, -2, -1, 0, 1, 2, 3};
        
        for (double t : testPoints) {
            double real = wavelet.psi(t);
            double imag = wavelet.psiImaginary(t);
            double magnitude = Math.sqrt(real * real + imag * imag);
            
            // Magnitude should be finite and non-zero
            assertFalse(Double.isNaN(magnitude), "Magnitude should not be NaN at t=" + t);
            assertFalse(Double.isInfinite(magnitude), "Magnitude should not be infinite at t=" + t);
            assertTrue(magnitude > 0, "Magnitude should be positive at t=" + t);
            
            // For verification, compute expected magnitude from Gaussian envelope
            double gaussianMagnitude = (1.0 / Math.pow(Math.PI, 0.25)) * Math.exp(-0.5 * t * t);
            
            // With correction term, magnitude will be different from pure Gaussian
            // but should still be of the same order
            assertTrue(magnitude < gaussianMagnitude * 2.0, 
                String.format("Magnitude at t=%.1f should be reasonable", t));
            
            // Magnitude should decay with distance
            if (Math.abs(t) > 2) {
                assertTrue(magnitude < 0.1, 
                    String.format("t=%.1f: magnitude=%.3f should be small", t, magnitude));
            }
        }
    }
}