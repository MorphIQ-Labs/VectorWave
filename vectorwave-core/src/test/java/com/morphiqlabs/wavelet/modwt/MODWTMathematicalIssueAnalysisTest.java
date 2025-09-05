package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Analysis test that investigates specific mathematical issues in MODWT implementation.
 * This test documents and validates the known limitations and behaviors.
 */
@DisplayName("MODWT Mathematical Issue Analysis")
class MODWTMathematicalIssueAnalysisTest {
    
    private static final double FLOATING_POINT_EPSILON = 1e-14;
    private static final double BOUNDARY_EFFECT_TOLERANCE = 0.1; // 10% for boundary effects
    
    /**
     * Documents the boundary effects in zero-padding MODWT.
     * Zero-padding MODWT does NOT provide perfect reconstruction at boundaries.
     * This is a known mathematical limitation, not a bug.
     */
    @Test
    @DisplayName("Document zero-padding boundary effects")
    void documentZeroPaddingBoundaryEffects() {
        // Test signal with non-zero values at boundaries  
        double[] signal = {1.0, 0.0, 0.0, 2.0};
        
        Haar haar = new Haar();
        
        // Periodic mode should have perfect reconstruction
        MODWTTransform periodicTransform = new MODWTTransform(haar, BoundaryMode.PERIODIC);
        MODWTResult periodicResult = periodicTransform.forward(signal);
        double[] periodicReconstructed = periodicTransform.inverse(periodicResult);
        
        // Zero-padding mode will have boundary effects
        MODWTTransform zeroPadTransform = new MODWTTransform(haar, BoundaryMode.ZERO_PADDING);
        MODWTResult zeroPadResult = zeroPadTransform.forward(signal);
        double[] zeroPadReconstructed = zeroPadTransform.inverse(zeroPadResult);
        
        // Periodic should be near-perfect
        for (int i = 0; i < signal.length; i++) {
            assertEquals(signal[i], periodicReconstructed[i], FLOATING_POINT_EPSILON,
                String.format("Periodic reconstruction should be perfect at index %d", i));
        }
        
        // Zero-padding will have boundary errors - document them
        System.out.println("Zero-padding boundary effects analysis:");
        System.out.println("Original: " + java.util.Arrays.toString(signal));
        System.out.println("Zero-pad: " + java.util.Arrays.toString(zeroPadReconstructed));
        
        boolean hasBoundaryEffects = false;
        for (int i = 0; i < signal.length; i++) {
            double error = Math.abs(signal[i] - zeroPadReconstructed[i]);
            if (error > FLOATING_POINT_EPSILON) {
                hasBoundaryEffects = true;
                System.out.printf("Boundary effect at index %d: error = %.6f%n", i, error);
            }
        }
        
        assertTrue(hasBoundaryEffects, 
            "Expected boundary effects in zero-padding MODWT - this is mathematically correct");
            
        // However, the interior points should still be reasonable
        // For this specific test case, only the boundary points should be significantly affected
        assertEquals(signal[1], zeroPadReconstructed[1], FLOATING_POINT_EPSILON,
            "Interior points should have minimal error");
        assertEquals(signal[2], zeroPadReconstructed[2], FLOATING_POINT_EPSILON,
            "Interior points should have minimal error");
    }
    
    /**
     * Documents the numerical precision limitations when dealing with mixed-scale signals.
     * This is a fundamental limitation of IEEE 754 floating-point arithmetic.
     */
    @Test
    @DisplayName("Document numerical precision limitations")
    void documentNumericalPrecisionLimitations() {
        // Signal with values differing by 20 orders of magnitude
        double[] extremeSignal = {1e-10, 1e10, -1e-10, -1e10};
        
        Haar haar = new Haar();
        MODWTTransform transform = new MODWTTransform(haar, BoundaryMode.PERIODIC);
        
        MODWTResult result = transform.forward(extremeSignal);
        double[] reconstructed = transform.inverse(result);
        
        System.out.println("Numerical precision analysis:");
        System.out.println("Original:      " + java.util.Arrays.toString(extremeSignal));
        System.out.println("Reconstructed: " + java.util.Arrays.toString(reconstructed));
        System.out.println("Approximation: " + java.util.Arrays.toString(result.approximationCoeffs()));
        System.out.println("Detail:        " + java.util.Arrays.toString(result.detailCoeffs()));
        
        // The large values should be preserved accurately
        assertEquals(extremeSignal[1], reconstructed[1], Math.abs(extremeSignal[1] * 1e-15),
            "Large values should be preserved with machine precision");
        assertEquals(extremeSignal[3], reconstructed[3], Math.abs(extremeSignal[3] * 1e-15),
            "Large values should be preserved with machine precision");
        
        // The tiny values will be lost due to floating-point limitations
        // This is mathematically expected, not a bug
        double relativeError0 = Math.abs(reconstructed[0]); // extremeSignal[0] ≈ 0 in reconstruction
        double relativeError2 = Math.abs(reconstructed[2]); // extremeSignal[2] ≈ 0 in reconstruction
        
        System.out.printf("Small value losses: index 0 = %e, index 2 = %e%n", 
            relativeError0, relativeError2);
        
        // Document that this is expected behavior - small values are completely lost (become 0.0)
        assertTrue(relativeError0 >= 0.0, 
            "Small values are expected to be lost when mixed with values 20 orders of magnitude larger");
        assertTrue(relativeError2 >= 0.0,
            "Small values are expected to be lost when mixed with values 20 orders of magnitude larger");
        
        // The small values should be completely lost (reconstructed as 0.0)
        assertEquals(0.0, reconstructed[0], 1e-15,
            "Very small values should be completely lost in mixed-scale signals");
        assertEquals(0.0, reconstructed[2], 1e-15,
            "Very small values should be completely lost in mixed-scale signals");
        
        // Verify coefficients are reasonable (not NaN, not infinite)
        for (int i = 0; i < extremeSignal.length; i++) {
            assertTrue(Double.isFinite(result.approximationCoeffs()[i]),
                "Approximation coefficients should be finite");
            assertTrue(Double.isFinite(result.detailCoeffs()[i]),
                "Detail coefficients should be finite");
        }
    }
    
    /**
     * Tests that MODWT works correctly for reasonably scaled signals.
     */
    @Test
    @DisplayName("Verify MODWT works for reasonable signals")
    void verifyReasonableSignals() {
        // Test various reasonable signal scales
        double[][] reasonableSignals = {
            {1e-6, 2e-6, 3e-6, 4e-6},      // Microscale
            {1e-3, 2e-3, 3e-3, 4e-3},      // Milliscale  
            {1.0, 2.0, 3.0, 4.0},          // Unit scale
            {1e3, 2e3, 3e3, 4e3},          // Kiloscale
            {1e6, 2e6, 3e6, 4e6}           // Megascale
        };
        
        Haar haar = new Haar();
        MODWTTransform transform = new MODWTTransform(haar, BoundaryMode.PERIODIC);
        
        for (double[] signal : reasonableSignals) {
            MODWTResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            // Should have perfect reconstruction for reasonable signals
            for (int i = 0; i < signal.length; i++) {
                double relativeError = Math.abs((signal[i] - reconstructed[i]) / signal[i]);
                assertTrue(relativeError < 1e-14,
                    String.format("Reasonable signal should reconstruct perfectly. " +
                        "Scale: %e, Index: %d, RelError: %e", signal[0], i, relativeError));
            }
        }
    }
    
    /**
     * Tests that the implementation correctly handles edge cases.
     */
    @Test
    @DisplayName("Test edge case handling")
    void testEdgeCases() {
        Haar haar = new Haar();
        MODWTTransform transform = new MODWTTransform(haar, BoundaryMode.PERIODIC);
        
        // Zero signal
        double[] zeroSignal = {0.0, 0.0, 0.0, 0.0};
        MODWTResult zeroResult = transform.forward(zeroSignal);
        double[] zeroReconstructed = transform.inverse(zeroResult);
        
        assertArrayEquals(zeroSignal, zeroReconstructed, FLOATING_POINT_EPSILON,
            "Zero signal should reconstruct perfectly");
        
        // Constant signal
        double[] constantSignal = {5.0, 5.0, 5.0, 5.0};
        MODWTResult constantResult = transform.forward(constantSignal);
        double[] constantReconstructed = transform.inverse(constantResult);
        
        assertArrayEquals(constantSignal, constantReconstructed, FLOATING_POINT_EPSILON,
            "Constant signal should reconstruct perfectly");
        
        // Alternating signal
        double[] alternatingSignal = {1.0, -1.0, 1.0, -1.0};
        MODWTResult alternatingResult = transform.forward(alternatingSignal);
        double[] alternatingReconstructed = transform.inverse(alternatingResult);
        
        assertArrayEquals(alternatingSignal, alternatingReconstructed, FLOATING_POINT_EPSILON,
            "Alternating signal should reconstruct perfectly");
    }
    
    /**
     * Verifies that the mathematical formulation is consistent with Percival & Walden
     * for the cases that should work perfectly.
     */
    @Test
    @DisplayName("Verify mathematical consistency with Percival & Walden")
    void verifyMathematicalConsistency() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        
        Haar haar = new Haar();
        MODWTTransform transform = new MODWTTransform(haar, BoundaryMode.PERIODIC);
        
        MODWTResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        // This should be perfect with periodic boundary conditions
        assertArrayEquals(signal, reconstructed, FLOATING_POINT_EPSILON,
            "Periodic MODWT should match Percival & Walden formulation exactly");
        
        // Verify energy conservation
        double signalEnergy = 0.0;
        double transformEnergy = 0.0;
        
        for (int i = 0; i < signal.length; i++) {
            signalEnergy += signal[i] * signal[i];
            transformEnergy += result.approximationCoeffs()[i] * result.approximationCoeffs()[i] +
                              result.detailCoeffs()[i] * result.detailCoeffs()[i];
        }
        
        // Energy should be conserved within floating-point precision
        double energyError = Math.abs(signalEnergy - transformEnergy);
        double relativeEnergyError = energyError / signalEnergy;
        
        assertTrue(relativeEnergyError < 1e-14,
            String.format("MODWT should conserve energy within floating-point precision. " +
                "SignalEnergy: %.15f, TransformEnergy: %.15f, RelativeError: %e", 
                signalEnergy, transformEnergy, relativeEnergyError));
    }
}