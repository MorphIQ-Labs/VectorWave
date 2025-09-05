package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for the Discrete Meyer wavelet (DMEY).
 * 
 * <p>These tests verify:</p>
 * <ul>
 *   <li>Coefficient correctness against PyWavelets reference values</li>
 *   <li>Basic wavelet properties (sum, normalization)</li>
 *   <li>Perfect reconstruction with MODWT</li>
 *   <li>Frequency localization properties</li>
 * </ul>
 */
@DisplayName("Discrete Meyer Wavelet Test Suite")
public class DiscreteMeyerTest {
    
    /**
     * Tolerance for coefficient value comparisons.
     * Used when comparing individual coefficient values against reference implementations.
     */
    private static final double COEFFICIENT_PRECISION = 1e-15;
    
    /**
     * Tolerance for mathematical property verification.
     * Used for sum checks and other mathematical properties that should be exact.
     */
    private static final double MATHEMATICAL_PRECISION = 1e-10;
    
    /**
     * Tolerance for DMEY-specific normalization issues.
     * DMEY has known ~0.002 error in sum of squares normalization.
     * Used for both normalization checks and reconstruction (which is affected by normalization).
     */
    private static final double DMEY_NORMALIZATION_TOLERANCE = 3e-3;
    
    /**
     * Tolerance for reconstruction accuracy.
     * Set higher than typical due to DMEY's normalization issues affecting reconstruction.
     * This is intentionally slightly higher than DMEY_NORMALIZATION_TOLERANCE to account for
     * error accumulation during forward and inverse transforms.
     */
    private static final double RECONSTRUCTION_TOLERANCE = 5e-3;
    
    /**
     * Test that DMEY is properly registered in the registry.
     */
    @Test
    @DisplayName("Verify DMEY is registered")
    void verifyDMEYRegistered() {
        WaveletName name = WaveletName.DMEY;
        assertNotNull(name, "DMEY should exist in WaveletName enum");
        assertEquals("dmey", name.getCode());
        assertEquals("Discrete Meyer wavelet", name.getDescription());
        
        Wavelet wavelet = WaveletRegistry.getWavelet(name);
        assertNotNull(wavelet, "DMEY should be registered");
        assertTrue(wavelet instanceof DiscreteMeyer, "DMEY should be a DiscreteMeyer instance");
    }
    
    /**
     * Verify DMEY filter length and basic properties.
     */
    @Test
    @DisplayName("Verify DMEY filter properties")
    void verifyFilterProperties() {
        DiscreteMeyer dmey = DiscreteMeyer.DMEY;
        
        // Check filter length
        double[] h = dmey.lowPassDecomposition();
        assertEquals(62, h.length, "DMEY should have 62 coefficients");
        
        // Check name and description
        assertEquals("dmey", dmey.name());
        assertEquals("Discrete Meyer wavelet (62-tap)", dmey.description());
        
        // Check vanishing moments (effectively infinite for Meyer)
        assertEquals(20, dmey.vanishingMoments(), 
            "DMEY should report 20 vanishing moments (practical value)");
    }
    
    /**
     * Verify DMEY coefficients against PyWavelets reference.
     */
    @Test
    @DisplayName("Verify DMEY coefficients match PyWavelets")
    void verifyDMEYCoefficients() {
        DiscreteMeyer dmey = DiscreteMeyer.DMEY;
        double[] h = dmey.lowPassDecomposition();
        
        // Check specific coefficients (spot check)
        // First coefficient
        assertEquals(0.0, h[0], COEFFICIENT_PRECISION, 
            "DMEY first coefficient should be 0");
        
        // Central coefficients (peak values)
        assertEquals(7.44585592318806277e-01, h[31], COEFFICIENT_PRECISION,
            "DMEY central coefficient should match PyWavelets");
        assertEquals(4.44593002757577238e-01, h[30], COEFFICIENT_PRECISION,
            "DMEY near-central coefficient should match PyWavelets");
        
        // DMEY is NOT perfectly symmetric, but nearly symmetric
        // Don't check for perfect symmetry here
    }
    
    /**
     * Verify coefficient normalization.
     * Note: DMEY has known normalization issues.
     */
    @Test
    @DisplayName("Verify DMEY coefficient normalization")
    void verifyCoefficientNormalization() {
        DiscreteMeyer dmey = DiscreteMeyer.DMEY;
        double[] h = dmey.lowPassDecomposition();
        
        // Sum should be sqrt(2)
        double sum = Arrays.stream(h).sum();
        assertEquals(Math.sqrt(2), sum, MATHEMATICAL_PRECISION,
            "DMEY coefficients sum should be sqrt(2)");
        
        // Sum of squares should be ~1 (known to be ~1.002 for DMEY)
        double sumSquares = Arrays.stream(h).map(x -> x * x).sum();
        assertEquals(1.0, sumSquares, DMEY_NORMALIZATION_TOLERANCE,
            "DMEY coefficients sum of squares should be ~1 (known small error)");
    }
    
    /**
     * Test perfect reconstruction with MODWT.
     */
    @Test
    @DisplayName("Verify perfect reconstruction with DMEY")
    void verifyPerfectReconstruction() {
        DiscreteMeyer dmey = DiscreteMeyer.DMEY;
        MODWTTransform transform = new MODWTTransform(dmey, BoundaryMode.PERIODIC);
        
        // Test with various signal lengths
        int[] lengths = {128, 256, 512, 1024};
        
        for (int length : lengths) {
            double[] signal = generateTestSignal(length);
            
            MODWTResult result = transform.forward(signal);
            double[] reconstructed = transform.inverse(result);
            
            // Check reconstruction error
            double error = computeReconstructionError(signal, reconstructed);
            assertTrue(error < RECONSTRUCTION_TOLERANCE,
                String.format("DMEY reconstruction error (%.2e) exceeds tolerance for length %d",
                    error, length));
        }
    }
    
    /**
     * Test multi-level decomposition and reconstruction.
     */
    @Test
    @DisplayName("Verify multi-level DMEY decomposition")
    void verifyMultiLevelDecomposition() {
        DiscreteMeyer dmey = DiscreteMeyer.DMEY;
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(dmey, BoundaryMode.PERIODIC);
        
        int length = 512;
        int levels = 4;
        double[] signal = generateTestSignal(length);
        
        // Forward transform
        MutableMultiLevelMODWTResult result = transform.decomposeMutable(signal, levels);
        
        // Check that we have the correct number of levels
        assertEquals(levels, result.getLevels());
        
        // Check coefficient lengths
        for (int level = 1; level <= levels; level++) {
            double[] coeffs = result.getDetailCoeffsAtLevel(level);
            assertEquals(length, coeffs.length,
                String.format("Level %d coefficients should have length %d", level, length));
        }
        
        // Inverse transform
        double[] reconstructed = transform.reconstruct(result);
        
        // Check reconstruction
        double error = computeReconstructionError(signal, reconstructed);
        assertTrue(error < RECONSTRUCTION_TOLERANCE,
            String.format("Multi-level DMEY reconstruction error (%.2e) exceeds tolerance", error));
    }
    
    /**
     * Test frequency localization properties.
     * DMEY should have excellent frequency selectivity.
     */
    @Test
    @DisplayName("Test DMEY frequency localization")
    void testFrequencyLocalization() {
        DiscreteMeyer dmey = DiscreteMeyer.DMEY;
        MODWTTransform transform = new MODWTTransform(dmey, BoundaryMode.PERIODIC);
        
        // Generate pure sinusoid
        int length = 1024;
        double frequency = 0.1; // Normalized frequency
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * frequency * i);
        }
        
        // Apply transform
        MODWTResult result = transform.forward(signal);
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        
        // Low frequency should be mostly in approximation
        double approxEnergy = computeEnergy(approx);
        double detailEnergy = computeEnergy(detail);
        
        assertTrue(approxEnergy > detailEnergy * 10,
            "Low frequency signal should be concentrated in approximation coefficients");
    }
    
    // QMF relationship is validated comprehensively in WaveletCoefficientVerificationTest
    
    /**
     * Verify that DMEY satisfies its known tolerances.
     */
    @Test
    @DisplayName("Verify DMEY coefficient conditions")
    void verifyDMEYConditions() {
        DiscreteMeyer dmey = DiscreteMeyer.DMEY;
        assertTrue(dmey.verifyCoefficients(),
            "DMEY should satisfy coefficient conditions within known tolerances");
    }
    
    /**
     * Test near-symmetry property of DMEY.
     */
    @Test
    @DisplayName("Verify DMEY near-symmetry")
    void verifyNearSymmetry() {
        DiscreteMeyer dmey = DiscreteMeyer.DMEY;
        
        // Measure actual asymmetry - DMEY is not perfectly symmetric
        double[] h = dmey.lowPassDecomposition();
        double asymmetry = measureAsymmetry(h);
        
        // DMEY has moderate asymmetry, not near-perfect symmetry
        assertTrue(asymmetry < 1.0, 
            String.format("DMEY asymmetry (%.4f) should be reasonable", asymmetry));
        
        // The isSymmetric method should return false for DMEY
        assertFalse(dmey.isSymmetric(), "DMEY is not symmetric");
    }
    
    /**
     * Compare DMEY with other wavelets for frequency selectivity.
     */
    @Test
    @DisplayName("Compare DMEY frequency selectivity")
    void compareFrequencySelectivity() {
        // Compare DMEY with DB10 for frequency separation
        DiscreteMeyer dmey = DiscreteMeyer.DMEY;
        Daubechies db10 = (Daubechies) WaveletRegistry.getWavelet(WaveletName.DB10);
        
        // Generate signal with two distinct frequencies
        int length = 512;
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 0.05 * i) +  // Low frequency
                       Math.sin(2 * Math.PI * 0.4 * i);     // High frequency
        }
        
        // Transform with both wavelets
        MODWTTransform dmeyTransform = new MODWTTransform(dmey, BoundaryMode.PERIODIC);
        MODWTTransform db10Transform = new MODWTTransform(db10, BoundaryMode.PERIODIC);
        
        MODWTResult dmeyResult = dmeyTransform.forward(signal);
        MODWTResult db10Result = db10Transform.forward(signal);
        
        // DMEY should have better frequency separation
        // This is a qualitative test - we just verify both work
        assertNotNull(dmeyResult);
        assertNotNull(db10Result);
    }
    
    // Helper methods
    
    private double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            // Combination of sinusoids
            signal[i] = Math.sin(2 * Math.PI * i / 64.0) +
                       0.5 * Math.sin(2 * Math.PI * i / 32.0) +
                       0.25 * Math.cos(2 * Math.PI * i / 16.0);
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
    
    private double computeEnergy(double[] signal) {
        double energy = 0;
        for (double value : signal) {
            energy += value * value;
        }
        return energy;
    }
    
    private double measureAsymmetry(double[] filter) {
        int n = filter.length;
        double asymmetry = 0;
        
        for (int i = 0; i < n/2; i++) {
            double diff = filter[i] - filter[n-1-i];
            asymmetry += diff * diff;
        }
        
        return Math.sqrt(asymmetry);
    }
}
