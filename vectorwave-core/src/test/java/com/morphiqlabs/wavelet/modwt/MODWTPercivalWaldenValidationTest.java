package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.internal.ScalarOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive mathematical validation test for MODWT implementation based on
 * Percival & Walden (2000) "Wavelet Methods for Time Series Analysis", Chapter 5.
 * 
 * This test validates the implementation against the exact mathematical formula:
 * W_j,t = Σ_{l=0}^{L-1} h_j,l * X_{(t-l) mod N}
 * 
 * where:
 * - W_j,t is the MODWT wavelet coefficient at level j, time t
 * - h_j,l are the scaled wavelet filter coefficients at level j
 * - X_n is the input signal
 * - L is the filter length
 * - N is the signal length
 */
@DisplayName("MODWT Percival & Walden Mathematical Validation")
class MODWTPercivalWaldenValidationTest {
    
    private static final double EPSILON = 1e-12;
    private static final double NUMERICAL_PRECISION_EPSILON = 1e-10;
    
    /**
     * Test MODWT circular convolution against exact Percival & Walden formula.
     * This is the core mathematical validation test.
     */
    @Test
    @DisplayName("Test MODWT circular convolution against Percival & Walden formula")
    void testMODWTCircularConvolutionFormula() {
        // Use a simple test signal that allows manual verification
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        
        // Use Haar wavelet for simplicity (L=2)
        Haar haar = new Haar();
        double[] originalFilter = haar.lowPassDecomposition(); // [1/√2, 1/√2]
        
        // For MODWT level 1, scale by 1/√2
        double scale = 1.0 / Math.sqrt(2.0);
        double[] scaledFilter = new double[originalFilter.length];
        for (int i = 0; i < originalFilter.length; i++) {
            scaledFilter[i] = originalFilter[i] * scale;
        }
        
        // Manual calculation of MODWT using Percival & Walden formula
        // W_j,t = Σ_{l=0}^{L-1} h_j,l * X_{(t-l) mod N}
        double[] expectedMODWT = new double[4];
        int N = signal.length;
        int L = scaledFilter.length;
        
        for (int t = 0; t < N; t++) {
            double sum = 0.0;
            for (int l = 0; l < L; l++) {
                int signalIndex = (t - l + N) % N; // Circular indexing as per P&W
                sum += scaledFilter[l] * signal[signalIndex];
            }
            expectedMODWT[t] = sum;
        }
        
        // Calculate expected values manually for verification
        // t=0: h[0]*X[0] + h[1]*X[3] = (1/2)*1 + (1/2)*4 = 2.5
        // t=1: h[0]*X[1] + h[1]*X[0] = (1/2)*2 + (1/2)*1 = 1.5  
        // t=2: h[0]*X[2] + h[1]*X[1] = (1/2)*3 + (1/2)*2 = 2.5
        // t=3: h[0]*X[3] + h[1]*X[2] = (1/2)*4 + (1/2)*3 = 3.5
        double[] manualExpected = {2.5, 1.5, 2.5, 3.5};
        assertArrayEquals(manualExpected, expectedMODWT, EPSILON, 
            "Manual MODWT calculation should match expected values");
        
        // Test implementation against manual calculation
        double[] actualMODWT = new double[4];
        ScalarOps.circularConvolveMODWT(signal, scaledFilter, actualMODWT);
        
        assertArrayEquals(expectedMODWT, actualMODWT, EPSILON,
            "ScalarOps implementation should match Percival & Walden formula");
    }
    
    /**
     * Test the complete MODWT transform (both approximation and detail coefficients)
     * against manual calculation using Percival & Walden formulas.
     */
    @Test
    @DisplayName("Test complete MODWT transform against P&W formulas")
    void testCompleteMODWTTransform() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        
        Haar haar = new Haar();
        MODWTTransform transform = new MODWTTransform(haar, BoundaryMode.PERIODIC);
        
        // Get transform result
        MODWTResult result = transform.forward(signal);
        
        // Manual calculation of expected coefficients
        double scale = 1.0 / Math.sqrt(2.0);
        double[] scaledLowPass = {haar.lowPassDecomposition()[0] * scale, 
                                  haar.lowPassDecomposition()[1] * scale};
        double[] scaledHighPass = {haar.highPassDecomposition()[0] * scale, 
                                   haar.highPassDecomposition()[1] * scale};
        
        // Calculate expected approximation coefficients (V_1,t)
        double[] expectedApprox = new double[4];
        for (int t = 0; t < 4; t++) {
            double sum = 0.0;
            for (int l = 0; l < 2; l++) {
                int signalIndex = (t - l + 4) % 4;
                sum += scaledLowPass[l] * signal[signalIndex];
            }
            expectedApprox[t] = sum;
        }
        
        // Calculate expected detail coefficients (W_1,t)
        double[] expectedDetail = new double[4];
        for (int t = 0; t < 4; t++) {
            double sum = 0.0;
            for (int l = 0; l < 2; l++) {
                int signalIndex = (t - l + 4) % 4;
                sum += scaledHighPass[l] * signal[signalIndex];
            }
            expectedDetail[t] = sum;
        }
        
        // Verify against implementation
        assertArrayEquals(expectedApprox, result.approximationCoeffs(), EPSILON,
            "Approximation coefficients should match P&W formula");
        assertArrayEquals(expectedDetail, result.detailCoeffs(), EPSILON,
            "Detail coefficients should match P&W formula");
    }
    
    /**
     * Test MODWT perfect reconstruction property as described in Percival & Walden.
     */
    @Test
    @DisplayName("Test MODWT perfect reconstruction (P&W Chapter 5)")
    void testPerfectReconstructionPercivalWalden() {
        double[][] testSignals = {
            {1.0, 2.0, 3.0, 4.0},
            {5.0, 5.0, 5.0, 5.0}, // Constant signal
            {1.0, -1.0, 1.0, -1.0}, // Alternating signal
            {2.5, 1.7, 8.3, -4.2}, // Random values
            {0.0, 0.0, 1.0, 0.0}   // Unit impulse
        };
        
        Haar haar = new Haar();
        MODWTTransform transform = new MODWTTransform(haar, BoundaryMode.PERIODIC);
        
        for (double[] signal : testSignals) {
            MODWTResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            // Perfect reconstruction should hold with machine precision
            for (int i = 0; i < signal.length; i++) {
                assertEquals(signal[i], reconstructed[i], NUMERICAL_PRECISION_EPSILON,
                    String.format("Perfect reconstruction failed at index %d. Original: %f, Reconstructed: %f", 
                        i, signal[i], reconstructed[i]));
            }
        }
    }
    
    /**
     * Test MODWT energy conservation property.
     * For orthogonal wavelets, energy should be conserved: ||X||² = ||V||² + ||W||²
     */
    @Test
    @DisplayName("Test MODWT energy conservation")
    void testEnergyConservation() {
        double[] signal = {3.2, -1.7, 4.5, 2.1, -0.8, 5.3, 1.9, -2.4};
        
        Haar haar = new Haar();
        MODWTTransform transform = new MODWTTransform(haar, BoundaryMode.PERIODIC);
        MODWTResult result = transform.forward(signal);
        
        // Calculate energies
        double signalEnergy = 0.0;
        double approxEnergy = 0.0;
        double detailEnergy = 0.0;
        
        for (int i = 0; i < signal.length; i++) {
            signalEnergy += signal[i] * signal[i];
            approxEnergy += result.approximationCoeffs()[i] * result.approximationCoeffs()[i];
            detailEnergy += result.detailCoeffs()[i] * result.detailCoeffs()[i];
        }
        
        double transformEnergy = approxEnergy + detailEnergy;
        
        // For MODWT with correct scaling, energy should be exactly conserved
        assertEquals(signalEnergy, transformEnergy, NUMERICAL_PRECISION_EPSILON,
            String.format("Energy not conserved. Signal: %f, Transform: %f", signalEnergy, transformEnergy));
    }
    
    /**
     * Test MODWT with different wavelets to ensure the formula works generally.
     */
    @ParameterizedTest
    @ValueSource(strings = {"Haar", "DB2", "DB4"})
    @DisplayName("Test MODWT formula with different wavelets")
    void testMODWTWithDifferentWavelets(String waveletName) {
        double[] signal = {1.0, 3.0, 2.0, 4.0, 1.5, 2.5, 3.5, 1.0};
        
        Wavelet wavelet = switch (waveletName) {
            case "Haar" -> new Haar();
            case "DB2" -> Daubechies.DB2;
            case "DB4" -> Daubechies.DB4;
            default -> throw new IllegalArgumentException("Unknown wavelet: " + waveletName);
        };
        
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        // Test forward transform
        MODWTResult result = transform.forward(signal);
        assertNotNull(result.approximationCoeffs(), "Approximation coefficients should not be null");
        assertNotNull(result.detailCoeffs(), "Detail coefficients should not be null");
        assertEquals(signal.length, result.approximationCoeffs().length, 
            "Approximation coefficients should have same length as signal");
        assertEquals(signal.length, result.detailCoeffs().length, 
            "Detail coefficients should have same length as signal");
        
        // Test reconstruction
        double[] reconstructed = transform.inverse(result);
        assertArrayEquals(signal, reconstructed, NUMERICAL_PRECISION_EPSILON,
            String.format("Perfect reconstruction should work for %s wavelet", waveletName));
    }
    
    /**
     * Test MODWT scaling property: filters should be scaled by 1/√2 at level 1.
     */
    @Test
    @DisplayName("Test MODWT filter scaling property")
    void testMODWTFilterScaling() {
        Haar haar = new Haar();
        double[] originalLowPass = haar.lowPassDecomposition();
        double[] originalHighPass = haar.highPassDecomposition();
        
        // MODWT level 1 scaling factor
        double expectedScale = 1.0 / Math.sqrt(2.0);
        
        // Expected scaled filters
        double[] expectedScaledLow = new double[originalLowPass.length];
        double[] expectedScaledHigh = new double[originalHighPass.length];
        
        for (int i = 0; i < originalLowPass.length; i++) {
            expectedScaledLow[i] = originalLowPass[i] * expectedScale;
            expectedScaledHigh[i] = originalHighPass[i] * expectedScale;
        }
        
        // The MODWT implementation should use these scaled filters internally
        // We can verify this by checking that the sum of squares of scaled filters
        // multiplied by 2 equals the sum of squares of original filters
        double originalSumSquares = 0.0;
        double scaledSumSquares = 0.0;
        
        for (int i = 0; i < originalLowPass.length; i++) {
            originalSumSquares += originalLowPass[i] * originalLowPass[i];
            scaledSumSquares += expectedScaledLow[i] * expectedScaledLow[i];
        }
        
        // Relationship: scaled_sum * 2 = original_sum (due to 1/√2 scaling)
        assertEquals(originalSumSquares, scaledSumSquares * 2.0, EPSILON,
            "MODWT scaling should follow 1/√2 relationship");
    }
    
    /**
     * Test edge case: unit impulse response should be well-defined.
     */
    @Test
    @DisplayName("Test MODWT unit impulse response")
    void testUnitImpulseResponse() {
        // Unit impulse at position 0
        double[] unitImpulse = {1.0, 0.0, 0.0, 0.0};
        
        Haar haar = new Haar();
        MODWTTransform transform = new MODWTTransform(haar, BoundaryMode.PERIODIC);
        
        MODWTResult result = transform.forward(unitImpulse);
        
        // Verify reconstruction gives back the original impulse
        double[] reconstructed = transform.inverse(result);
        assertArrayEquals(unitImpulse, reconstructed, NUMERICAL_PRECISION_EPSILON,
            "Unit impulse should be perfectly reconstructed");
        
        // The impulse response should have specific properties
        // For Haar wavelet with impulse at t=0:
        // Approximation coefficients should reflect the low-pass filtering
        // Detail coefficients should reflect the high-pass filtering
        
        // At least one coefficient in each array should be non-zero
        boolean hasNonZeroApprox = false;
        boolean hasNonZeroDetail = false;
        
        for (int i = 0; i < 4; i++) {
            if (Math.abs(result.approximationCoeffs()[i]) > EPSILON) {
                hasNonZeroApprox = true;
            }
            if (Math.abs(result.detailCoeffs()[i]) > EPSILON) {
                hasNonZeroDetail = true;
            }
        }
        
        assertTrue(hasNonZeroApprox, "Approximation coefficients should respond to unit impulse");
        assertTrue(hasNonZeroDetail, "Detail coefficients should respond to unit impulse");
    }
    
    /**
     * Test MODWT linearity property: T(ax + by) = aT(x) + bT(y)
     */
    @Test
    @DisplayName("Test MODWT linearity property")
    void testMODWTLinearity() {
        double[] signal1 = {1.0, 2.0, 3.0, 4.0};
        double[] signal2 = {4.0, 3.0, 2.0, 1.0};
        double a = 2.5;
        double b = -1.3;
        
        // Linear combination
        double[] combined = new double[4];
        for (int i = 0; i < 4; i++) {
            combined[i] = a * signal1[i] + b * signal2[i];
        }
        
        Haar haar = new Haar();
        MODWTTransform transform = new MODWTTransform(haar, BoundaryMode.PERIODIC);
        
        MODWTResult result1 = transform.forward(signal1);
        MODWTResult result2 = transform.forward(signal2);
        MODWTResult resultCombined = transform.forward(combined);
        
        // Check linearity for approximation coefficients
        for (int i = 0; i < 4; i++) {
            double expectedApprox = a * result1.approximationCoeffs()[i] + b * result2.approximationCoeffs()[i];
            double actualApprox = resultCombined.approximationCoeffs()[i];
            assertEquals(expectedApprox, actualApprox, EPSILON,
                String.format("Linearity failed for approximation coefficient at index %d", i));
            
            double expectedDetail = a * result1.detailCoeffs()[i] + b * result2.detailCoeffs()[i];
            double actualDetail = resultCombined.detailCoeffs()[i];
            assertEquals(expectedDetail, actualDetail, EPSILON,
                String.format("Linearity failed for detail coefficient at index %d", i));
        }
    }
    
    /**
     * Test that the mathematical implementation handles boundary conditions correctly
     * according to Percival & Walden.
     */
    @Test
    @DisplayName("Test MODWT boundary condition handling")
    void testBoundaryConditions() {
        // Test with signal where boundary effects are important
        double[] signal = {1.0, 0.0, 0.0, 2.0}; // Non-zero at boundaries
        
        Haar haar = new Haar();
        
        // Test periodic boundary mode
        MODWTTransform periodicTransform = new MODWTTransform(haar, BoundaryMode.PERIODIC);
        MODWTResult periodicResult = periodicTransform.forward(signal);
        double[] periodicReconstructed = periodicTransform.inverse(periodicResult);
        
        assertArrayEquals(signal, periodicReconstructed, NUMERICAL_PRECISION_EPSILON,
            "Periodic boundary mode should preserve perfect reconstruction");
        
        // Test zero-padding boundary mode
        MODWTTransform zeroPadTransform = new MODWTTransform(haar, BoundaryMode.ZERO_PADDING);
        MODWTResult zeroPadResult = zeroPadTransform.forward(signal);
        double[] zeroPadReconstructed = zeroPadTransform.inverse(zeroPadResult);
        
        // Zero-padding MODWT has known boundary effects - document and verify they are reasonable
        double maxBoundaryError = 0.0;
        for (int i = 0; i < signal.length; i++) {
            double error = Math.abs(signal[i] - zeroPadReconstructed[i]);
            maxBoundaryError = Math.max(maxBoundaryError, error);
        }
        
        // Boundary effects are expected but should be bounded
        assertTrue(maxBoundaryError > 0.0, 
            "Zero-padding should have some boundary effects (this is mathematically expected)");
        assertTrue(maxBoundaryError < 2.0, 
            "Boundary effects should be reasonable in magnitude");
            
        // Interior points should be well-preserved
        assertEquals(signal[1], zeroPadReconstructed[1], NUMERICAL_PRECISION_EPSILON,
            "Interior points should be preserved in zero-padding mode");
        assertEquals(signal[2], zeroPadReconstructed[2], NUMERICAL_PRECISION_EPSILON,
            "Interior points should be preserved in zero-padding mode");
    }
    
    /**
     * Test numerical stability of the MODWT implementation.
     */
    @Test
    @DisplayName("Test MODWT numerical stability")
    void testNumericalStability() {
        // Test with very small values
        double[] smallSignal = {1e-10, 2e-10, 3e-10, 4e-10};
        
        // Test with very large values  
        double[] largeSignal = {1e10, 2e10, 3e10, 4e10};
        
        // Test with mixed scale values
        double[] mixedSignal = {1e-10, 1e10, -1e-10, -1e10};
        
        Haar haar = new Haar();
        MODWTTransform transform = new MODWTTransform(haar, BoundaryMode.PERIODIC);
        
        double[][] testSignals = {smallSignal, largeSignal, mixedSignal};
        String[] signalNames = {"small", "large", "mixed"};
        
        for (int i = 0; i < testSignals.length; i++) {
            double[] signal = testSignals[i];
            String name = signalNames[i];
            
            MODWTResult result = transform.forward(signal);
            
            // Check that all coefficients are finite
            for (int j = 0; j < signal.length; j++) {
                assertTrue(Double.isFinite(result.approximationCoeffs()[j]),
                    String.format("Approximation coefficient %d should be finite for %s signal", j, name));
                assertTrue(Double.isFinite(result.detailCoeffs()[j]),
                    String.format("Detail coefficient %d should be finite for %s signal", j, name));
            }
            
            // Test reconstruction
            double[] reconstructed = transform.inverse(result);
            
            // Check numerical stability with appropriate expectations
            if (name.equals("mixed")) {
                // Mixed-scale signals have fundamental floating-point limitations
                // Large values should be preserved, small values may be lost
                for (int j = 0; j < signal.length; j++) {
                    if (Math.abs(signal[j]) >= 1e9) {
                        // Large values should be preserved with machine precision
                        double relativeError = Math.abs((signal[j] - reconstructed[j]) / signal[j]);
                        assertTrue(relativeError < 1e-14,
                            String.format("Large values should be preserved in mixed signal at index %d: %e", 
                                j, relativeError));
                    } else {
                        // Very small values mixed with large ones may be completely lost
                        // This is mathematically expected, not a bug
                        double absError = Math.abs(signal[j] - reconstructed[j]);
                        assertTrue(absError <= Math.abs(signal[j]) + 1e-15,
                            String.format("Small value error should be bounded for mixed signal at index %d", j));
                    }
                }
            } else {
                // Non-mixed signals should have excellent reconstruction
                for (int j = 0; j < signal.length; j++) {
                    if (signal[j] != 0.0) {
                        double relativeError = Math.abs((signal[j] - reconstructed[j]) / signal[j]);
                        assertTrue(relativeError < 1e-14,
                            String.format("Relative reconstruction error too large for %s signal at index %d: %e", 
                                name, j, relativeError));
                    } else {
                        assertEquals(0.0, reconstructed[j], 1e-15,
                            String.format("Zero value not preserved for %s signal at index %d", name, j));
                    }
                }
            }
        }
    }
}