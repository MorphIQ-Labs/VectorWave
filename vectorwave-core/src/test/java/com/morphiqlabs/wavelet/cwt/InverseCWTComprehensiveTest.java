package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidConfigurationException;
import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.cwt.finance.PaulWavelet;
import com.morphiqlabs.wavelet.cwt.finance.DOGWavelet;
import com.morphiqlabs.wavelet.cwt.ComplexMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test coverage for InverseCWT class.
 */
public class InverseCWTComprehensiveTest {
    
    private static final double TOLERANCE = 1e-6;
    private InverseCWT inverseCWT;
    private ContinuousWavelet morletWavelet;
    
    @BeforeEach
    public void setUp() {
        morletWavelet = new MorletWavelet(6.0, 1.0);
        inverseCWT = new InverseCWT(morletWavelet);
    }
    
    @Test
    public void testConstructor_ValidWavelet() {
        assertNotNull(inverseCWT);
        assertDoesNotThrow(() -> new InverseCWT(new PaulWavelet(4)));
        assertDoesNotThrow(() -> new InverseCWT(new DOGWavelet(2)));
    }
    
    @Test
    public void testConstructor_NullWavelet() {
        assertThrows(InvalidArgumentException.class, () -> new InverseCWT(null));
    }
    
    @Test
    public void testConstructor_SingleParameter() {
        // The constructor only takes one parameter - the wavelet
        // FFT usage is automatic based on signal size
        InverseCWT inverseCWT = new InverseCWT(morletWavelet);
        assertNotNull(inverseCWT);
    }
    
    @Test
    public void testReconstruct_BasicSignal() {
        // Create a simple CWT result
        int signalLength = 64;
        double[] scales = {1.0, 2.0, 4.0, 8.0};
        double[][] coefficients = new double[scales.length][signalLength];
        
        // Fill with test pattern
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = Math.sin(2 * Math.PI * j / signalLength) / scales[i];
            }
        }
        
        CWTResult cwtResult = new CWTResult(coefficients, scales, morletWavelet);
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
        assertThrows(InvalidArgumentException.class, () -> inverseCWT.reconstruct(null));
    }
    
    @Test
    public void testReconstruct_EmptyCoefficients() {
        // Test that CWTResult constructor validates empty coefficients
        double[][] emptyCoeffs = new double[0][0];
        double[] emptyScales = new double[0];
        
        assertThrows(IllegalArgumentException.class, 
            () -> new CWTResult(emptyCoeffs, emptyScales, morletWavelet));
    }
    
    @Test
    public void testReconstructBand_FrequencyBand() {
        int signalLength = 128;
        double[] scales = {1.0, 2.0, 4.0, 8.0, 16.0};
        double[][] coefficients = new double[scales.length][signalLength];
        
        // Create coefficients
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = Math.cos(2 * Math.PI * j / scales[i]);
            }
        }
        
        CWTResult cwtResult = new CWTResult(coefficients, scales, morletWavelet);
        
        // Reconstruct only low frequencies (large scales)
        double minScale = 4.0;
        double maxScale = 16.0;
        double[] partialReconstructed = inverseCWT.reconstructBand(cwtResult, minScale, maxScale);
        
        assertNotNull(partialReconstructed);
        assertEquals(signalLength, partialReconstructed.length);
    }
    
    @Test
    public void testReconstructBand_InvalidScaleRange() {
        int signalLength = 64;
        double[] scales = {1.0, 2.0, 4.0, 8.0};
        double[][] coefficients = new double[scales.length][signalLength];
        CWTResult cwtResult = new CWTResult(coefficients, scales, morletWavelet);
        
        // Min scale greater than max scale
        assertThrows(InvalidArgumentException.class, 
            () -> inverseCWT.reconstructBand(cwtResult, 10.0, 5.0));
        
        // Negative scales
        assertThrows(InvalidArgumentException.class, 
            () -> inverseCWT.reconstructBand(cwtResult, -1.0, 5.0));
    }
    
    @Test
    public void testReconstructFrequencyBand() {
        int signalLength = 64;
        double[] scales = {1.0, 2.0, 4.0, 8.0, 16.0, 32.0};
        double[][] coefficients = new double[scales.length][signalLength];
        
        // Fill with test data
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = Math.exp(-j * 0.1) * Math.sin(2 * Math.PI * i * j / signalLength);
            }
        }
        
        CWTResult cwtResult = new CWTResult(coefficients, scales, morletWavelet);
        
        // Test frequency band reconstruction
        double samplingRate = 100.0;
        double minFreq = 5.0;
        double maxFreq = 25.0;
        
        double[] bandLimited = inverseCWT.reconstructFrequencyBand(cwtResult, samplingRate, minFreq, maxFreq);
        assertNotNull(bandLimited);
        assertEquals(signalLength, bandLimited.length);
        
        // Check all values are finite
        for (double value : bandLimited) {
            assertTrue(Double.isFinite(value));
        }
    }
    
    @Test
    public void testReconstructFrequencyBand_InvalidParameters() {
        int signalLength = 32;
        double[] scales = {1.0, 2.0, 4.0};
        double[][] coefficients = new double[scales.length][signalLength];
        CWTResult cwtResult = new CWTResult(coefficients, scales, morletWavelet);
        
        // Zero sampling rate
        assertThrows(InvalidArgumentException.class, 
            () -> inverseCWT.reconstructFrequencyBand(cwtResult, 0, 10.0, 20.0));
        
        // Negative frequency
        assertThrows(InvalidArgumentException.class, 
            () -> inverseCWT.reconstructFrequencyBand(cwtResult, 100.0, -5.0, 20.0));
        
        // Min freq greater than max freq
        assertThrows(InvalidArgumentException.class, 
            () -> inverseCWT.reconstructFrequencyBand(cwtResult, 100.0, 30.0, 20.0));
    }
    
    @Test
    public void testGetAdmissibilityConstant() {
        double admissibility = inverseCWT.getAdmissibilityConstant();
        assertTrue(admissibility > 0, "Admissibility constant should be positive");
        assertTrue(Double.isFinite(admissibility), "Admissibility constant should be finite");
    }
    
    @Test
    public void testIsAdmissible() {
        assertTrue(inverseCWT.isAdmissible());
        
        PaulWavelet paulWavelet = new PaulWavelet(4);
        InverseCWT paulInverse = new InverseCWT(paulWavelet);
        assertTrue(paulInverse.isAdmissible());
    }
    
    @Test
    public void testLargeScaleReconstruction() {
        // Test with larger signal
        int signalLength = 512;
        double[] scales = new double[32];
        for (int i = 0; i < scales.length; i++) {
            scales[i] = Math.pow(2, i * 0.25);
        }
        
        double[][] coefficients = new double[scales.length][signalLength];
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = Math.random() * 0.1;
            }
        }
        
        CWTResult largeResult = new CWTResult(coefficients, scales, morletWavelet);
        
        // FFT acceleration is automatic for large signals
        double[] reconstructed = inverseCWT.reconstruct(largeResult);
        
        assertNotNull(reconstructed);
        assertEquals(signalLength, reconstructed.length);
    }
    
    @Test
    public void testDifferentWavelets() {
        // Test with different wavelet types
        ContinuousWavelet[] wavelets = {
            new MorletWavelet(6.0, 1.0),
            new PaulWavelet(4),
            new DOGWavelet(2)
        };
        
        int signalLength = 64;
        double[] scales = {2.0, 4.0, 8.0};
        double[][] coefficients = new double[scales.length][signalLength];
        
        // Simple coefficient pattern
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = 0.1;
            }
        }
        
        CWTResult result = new CWTResult(coefficients, scales, morletWavelet);
        
        for (ContinuousWavelet wavelet : wavelets) {
            InverseCWT inverse = new InverseCWT(wavelet);
            double[] reconstructed = inverse.reconstruct(result);
            
            assertNotNull(reconstructed, "Reconstruction failed for " + wavelet.getClass().getSimpleName());
            assertEquals(signalLength, reconstructed.length);
        }
    }
    
    @Test
    public void testEdgeCases_SingleScale() {
        int signalLength = 32;
        double[] scales = {4.0};  // Single scale
        double[][] coefficients = new double[1][signalLength];
        
        for (int j = 0; j < signalLength; j++) {
            coefficients[0][j] = Math.sin(2 * Math.PI * j / signalLength);
        }
        
        CWTResult result = new CWTResult(coefficients, scales, morletWavelet);
        double[] reconstructed = inverseCWT.reconstruct(result);
        
        assertNotNull(reconstructed);
        assertEquals(signalLength, reconstructed.length);
    }
    
    @Test
    public void testReconstructBand_EdgeCases() {
        int signalLength = 64;
        double[] scales = {1.0, 2.0, 4.0, 8.0, 16.0};
        double[][] coefficients = new double[scales.length][signalLength];
        
        // Fill with test data
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = Math.sin(2 * Math.PI * j / signalLength);
            }
        }
        
        CWTResult cwtResult = new CWTResult(coefficients, scales, morletWavelet);
        
        // Test with scale range outside actual scales
        double[] result1 = inverseCWT.reconstructBand(cwtResult, 0.5, 0.9);
        assertNotNull(result1);
        assertEquals(signalLength, result1.length);
        // Should have very small values since no exact scales in range
        for (double v : result1) {
            assertTrue(Math.abs(v) < 0.1, "Value should be near zero: " + v);
        }
        
        // Test with scale range beyond max
        double[] result2 = inverseCWT.reconstructBand(cwtResult, 20.0, 30.0);
        assertNotNull(result2);
        assertEquals(signalLength, result2.length);
        // Should have small values since no exact scales in range
        for (double v : result2) {
            assertTrue(Math.abs(v) < 0.2, "Value should be small: " + v);
        }
        
        // Test with partial overlap
        double[] result3 = inverseCWT.reconstructBand(cwtResult, 3.0, 10.0);
        assertNotNull(result3);
        assertEquals(signalLength, result3.length);
    }
    
    @Test
    public void testReconstructFrequencyBand_EdgeCases() {
        int signalLength = 64;
        double[] scales = {1.0, 2.0, 4.0, 8.0};
        double[][] coefficients = new double[scales.length][signalLength];
        
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                coefficients[i][j] = 0.1;
            }
        }
        
        CWTResult cwtResult = new CWTResult(coefficients, scales, morletWavelet);
        double samplingRate = 100.0;
        
        // Test frequency band reconstruction with valid range
        double[] result = inverseCWT.reconstructFrequencyBand(cwtResult, samplingRate, 5.0, 20.0);
        assertNotNull(result);
        assertEquals(signalLength, result.length);
        
        // Check values are finite
        for (double v : result) {
            assertTrue(Double.isFinite(v));
        }
    }
    
    @Test
    public void testReconstructFrequencyBand_InvalidFrequencies() {
        int signalLength = 32;
        double[] scales = {2.0, 4.0};
        double[][] coefficients = new double[scales.length][signalLength];
        CWTResult cwtResult = new CWTResult(coefficients, scales, morletWavelet);
        
        // Invalid sampling rate
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructFrequencyBand(cwtResult, 0, 10.0, 20.0));
        
        // Negative frequency
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructFrequencyBand(cwtResult, 100.0, -5.0, 20.0));
        
        // Min freq greater than max freq
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructFrequencyBand(cwtResult, 100.0, 30.0, 20.0));
        
        // Max freq greater than Nyquist
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstructFrequencyBand(cwtResult, 100.0, 10.0, 60.0));
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
        
        CWTResult smallResult = new CWTResult(smallCoeffs, scales, morletWavelet);
        double[] reconstructed = inverseCWT.reconstruct(smallResult);
        
        // Should handle small values without numerical issues
        for (double value : reconstructed) {
            assertTrue(Double.isFinite(value));
            assertTrue(Math.abs(value) < 1.0); // Should remain small
        }
    }
    
    @Test
    public void testReconstructWithComplexCoefficients() {
        // Test reconstruction with complex CWT result
        int signalLength = 64;
        double[] scales = {2.0, 4.0, 8.0};
        
        // Create complex coefficients
        ComplexMatrix complexCoeffs = new ComplexMatrix(scales.length, signalLength);
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                double real = Math.cos(2 * Math.PI * j / signalLength);
                double imag = Math.sin(2 * Math.PI * j / signalLength);
                complexCoeffs.set(i, j, real, imag);
            }
        }
        
        CWTResult complexResult = new CWTResult(complexCoeffs, scales, morletWavelet);
        double[] reconstructed = inverseCWT.reconstruct(complexResult);
        
        assertNotNull(reconstructed);
        assertEquals(signalLength, reconstructed.length);
        
        // Check for finite values
        for (double value : reconstructed) {
            assertTrue(Double.isFinite(value));
        }
    }
    
    @Test
    public void testReconstructBandWithComplexCoefficients() {
        // Test band reconstruction with complex CWT result
        int signalLength = 32;
        double[] scales = {1.0, 2.0, 4.0, 8.0};
        
        ComplexMatrix complexCoeffs = new ComplexMatrix(scales.length, signalLength);
        for (int i = 0; i < scales.length; i++) {
            for (int j = 0; j < signalLength; j++) {
                complexCoeffs.set(i, j, 0.1, 0.05);
            }
        }
        
        CWTResult complexResult = new CWTResult(complexCoeffs, scales, morletWavelet);
        double[] reconstructed = inverseCWT.reconstructBand(complexResult, 1.5, 5.0);
        
        assertNotNull(reconstructed);
        assertEquals(signalLength, reconstructed.length);
    }
}