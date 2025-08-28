package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Symlet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test coverage for MODWTBasedInverseCWT class.
 */
public class MODWTBasedInverseCWTTest {
    
    private ContinuousWavelet cwavelet;
    private MODWTBasedInverseCWT inverseCWT;
    
    @BeforeEach
    public void setUp() {
        cwavelet = new MorletWavelet(6.0, 1.0);
        inverseCWT = new MODWTBasedInverseCWT(cwavelet);
    }
    
    @Test
    public void testConstructor_AutomaticWaveletMatching() {
        // Should automatically find matching discrete wavelet
        assertNotNull(inverseCWT);
    }
    
    @Test
    public void testConstructor_ExplicitWavelets() {
        DiscreteWavelet dwavelet = Daubechies.DB4;
        MODWTBasedInverseCWT explicit = new MODWTBasedInverseCWT(cwavelet, dwavelet, true);
        assertNotNull(explicit);
        
        // With refinement disabled
        MODWTBasedInverseCWT noRefinement = new MODWTBasedInverseCWT(cwavelet, dwavelet, false);
        assertNotNull(noRefinement);
    }
    
    @Test
    public void testConstructor_NullWavelets() {
        DiscreteWavelet dwavelet = Daubechies.DB4;
        
        // Null continuous wavelet
        assertThrows(InvalidArgumentException.class, 
            () -> new MODWTBasedInverseCWT(null, dwavelet, true));
        
        // Null discrete wavelet
        assertThrows(InvalidArgumentException.class, 
            () -> new MODWTBasedInverseCWT(cwavelet, null, true));
    }
    
    @Test
    public void testReconstruct_BasicSignal() {
        // Create test CWT result
        int signalLength = 64;
        double[] scales = {1.0, 2.0, 4.0, 8.0, 16.0};
        double[][] coefficients = new double[scales.length][signalLength];
        
        // Fill with test pattern
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = Math.sin(2 * Math.PI * j / signalLength) / scales[i];
            }
        }
        
        CWTResult cwtResult = new CWTResult(coefficients, scales, cwavelet);
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        
        assertNotNull(reconstructed);
        assertEquals(signalLength, reconstructed.length);
        
        // Check for finite values
        for (double value : reconstructed) {
            assertTrue(Double.isFinite(value));
        }
    }
    
    @Test
    public void testReconstruct_NullResult() {
        assertThrows(InvalidArgumentException.class, 
            () -> inverseCWT.reconstruct(null));
    }
    
    @Test
    public void testReconstruct_DyadicScales() {
        // Test with perfect dyadic scales (powers of 2)
        int signalLength = 128;
        double[] dyadicScales = {1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0};
        double[][] coefficients = new double[dyadicScales.length][signalLength];
        
        // Create simple pattern
        for (int i = 0; i < dyadicScales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = 0.1 * Math.cos(2 * Math.PI * j / dyadicScales[i]);
            }
        }
        
        CWTResult cwtResult = new CWTResult(coefficients, dyadicScales, cwavelet);
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        
        assertNotNull(reconstructed);
        assertEquals(signalLength, reconstructed.length);
    }
    
    @Test
    public void testReconstruct_MixedScales() {
        // Test with mix of dyadic and non-dyadic scales
        int signalLength = 100; // Non-power of 2
        double[] mixedScales = {1.0, 1.5, 2.0, 3.0, 4.0, 6.0, 8.0, 12.0, 16.0};
        double[][] coefficients = new double[mixedScales.length][signalLength];
        
        for (int i = 0; i < mixedScales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = Math.exp(-j * 0.01) * Math.sin(2 * Math.PI * j / mixedScales[i]);
            }
        }
        
        CWTResult cwtResult = new CWTResult(coefficients, mixedScales, cwavelet);
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        
        assertNotNull(reconstructed);
        assertEquals(signalLength, reconstructed.length);
    }
    
    @Test
    public void testReconstruct_LargeSignal() {
        // Test with larger signal
        int signalLength = 1024;
        double[] scales = new double[10];
        for (int i = 0; i < scales.length; i++) {
            scales[i] = Math.pow(2, i * 0.5);
        }
        
        double[][] coefficients = new double[scales.length][signalLength];
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = Math.random() * 0.01;
            }
        }
        
        CWTResult cwtResult = new CWTResult(coefficients, scales, cwavelet);
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        
        assertNotNull(reconstructed);
        assertEquals(signalLength, reconstructed.length);
    }
    
    @Test
    public void testReconstruct_SmallSignal() {
        // Test with very small signal
        int signalLength = 16;
        double[] scales = {1.0, 2.0, 4.0};
        double[][] coefficients = new double[scales.length][signalLength];
        
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = j * 0.1;
            }
        }
        
        CWTResult cwtResult = new CWTResult(coefficients, scales, cwavelet);
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        
        assertNotNull(reconstructed);
        assertEquals(signalLength, reconstructed.length);
    }
    
    @Test
    public void testDifferentDiscreteWavelets() {
        // Test with different discrete wavelets for reconstruction
        DiscreteWavelet[] wavelets = {
            Haar.INSTANCE,
            Daubechies.DB2,
            Daubechies.DB4,
            Symlet.SYM2
        };
        
        int signalLength = 64;
        double[] scales = {2.0, 4.0, 8.0};
        double[][] coefficients = new double[scales.length][signalLength];
        
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = 0.1;
            }
        }
        
        CWTResult cwtResult = new CWTResult(coefficients, scales, cwavelet);
        
        for (DiscreteWavelet dwavelet : wavelets) {
            MODWTBasedInverseCWT inverter = new MODWTBasedInverseCWT(cwavelet, dwavelet, true);
            double[] reconstructed = inverter.reconstruct(cwtResult);
            
            assertNotNull(reconstructed, "Failed for " + dwavelet.getClass().getSimpleName());
            assertEquals(signalLength, reconstructed.length);
        }
    }
    
    @Test
    public void testRefinementEnabled() {
        DiscreteWavelet dwavelet = Daubechies.DB4;
        
        // Test with refinement enabled
        MODWTBasedInverseCWT withRefinement = new MODWTBasedInverseCWT(cwavelet, dwavelet, true);
        
        // Test with refinement disabled
        MODWTBasedInverseCWT withoutRefinement = new MODWTBasedInverseCWT(cwavelet, dwavelet, false);
        
        // Create test signal
        int signalLength = 64;
        double[] scales = {1.5, 3.0, 6.0}; // Non-dyadic scales
        double[][] coefficients = new double[scales.length][signalLength];
        
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = Math.sin(2 * Math.PI * j / signalLength);
            }
        }
        
        CWTResult cwtResult = new CWTResult(coefficients, scales, cwavelet);
        
        // Both should work but may produce different results
        double[] refined = withRefinement.reconstruct(cwtResult);
        double[] notRefined = withoutRefinement.reconstruct(cwtResult);
        
        assertNotNull(refined);
        assertNotNull(notRefined);
        assertEquals(signalLength, refined.length);
        assertEquals(signalLength, notRefined.length);
    }
    
    @Test
    public void testNumericalStability() {
        // Test with very small coefficients
        int signalLength = 64;
        double[] scales = {1.0, 2.0, 4.0};
        double[][] smallCoeffs = new double[scales.length][signalLength];
        
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                smallCoeffs[i][j] = 1e-10 * Math.random();
            }
        }
        
        CWTResult smallResult = new CWTResult(smallCoeffs, scales, cwavelet);
        double[] reconstructed = inverseCWT.reconstruct(smallResult);
        
        // Should handle small values without numerical issues
        for (double value : reconstructed) {
            assertTrue(Double.isFinite(value));
        }
    }
    
    @Test
    public void testEmptyScales() {
        // Test edge case with minimal scales
        int signalLength = 32;
        double[] scales = {2.0}; // Single scale
        double[][] coefficients = new double[1][signalLength];
        
        for (int j = 0; j < signalLength; j++) {
            coefficients[0][j] = 0.5;
        }
        
        CWTResult cwtResult = new CWTResult(coefficients, scales, cwavelet);
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        
        assertNotNull(reconstructed);
        assertEquals(signalLength, reconstructed.length);
    }
}