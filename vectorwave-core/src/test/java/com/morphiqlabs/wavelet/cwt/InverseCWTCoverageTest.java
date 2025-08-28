package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.api.ComplexContinuousWavelet;
import com.morphiqlabs.wavelet.cwt.finance.DOGWavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Additional coverage tests for InverseCWT class.
 * Focuses on edge cases and error conditions.
 */
class InverseCWTCoverageTest {
    
    private ContinuousWavelet morletWavelet;
    private ContinuousWavelet dogWavelet;
    
    @BeforeEach
    void setUp() {
        morletWavelet = new MorletWavelet();
        dogWavelet = new DOGWavelet(2); // Mexican hat
    }
    
    @Test
    @DisplayName("Should throw exception for null wavelet")
    void testNullWaveletConstructor() {
        assertThrows(InvalidArgumentException.class, 
            () -> new InverseCWT(null),
            "Should throw exception for null wavelet");
    }
    
    @Test
    @DisplayName("Should throw exception for null CWT result")
    void testNullCWTResult() {
        InverseCWT inverseCWT = new InverseCWT(morletWavelet);
        assertThrows(InvalidArgumentException.class,
            () -> inverseCWT.reconstruct(null),
            "Should throw exception for null CWT result");
    }
    
    @Test
    @DisplayName("Should handle wavelet with valid admissibility")
    void testValidAdmissibilityWavelet() {
        // Test with valid wavelets
        assertDoesNotThrow(() -> new InverseCWT(morletWavelet),
            "Morlet wavelet should be valid");
        
        assertDoesNotThrow(() -> new InverseCWT(dogWavelet),
            "DOG wavelet should be valid");
    }
    
    @Test
    @DisplayName("Should handle reconstruction from different wavelet types")
    void testDifferentWaveletTypes() {
        // Test with real wavelet (DOG)
        InverseCWT dogInverse = new InverseCWT(dogWavelet);
        assertNotNull(dogInverse);
        
        // Create simple signal
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 5 * i / signal.length);
        }
        
        // Create CWT result
        CWTTransform transform = new CWTTransform(dogWavelet);
        double[] scales = {1.0, 2.0, 4.0, 8.0, 16.0};
        CWTResult cwtResult = transform.analyze(signal, scales);
        
        // Reconstruct
        double[] reconstructed = dogInverse.reconstruct(cwtResult);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
    }
    
    @Test
    @DisplayName("Should handle small scale arrays")
    void testSmallScaleArrays() {
        InverseCWT inverseCWT = new InverseCWT(morletWavelet);
        
        // Small signal
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.cos(2 * Math.PI * 2 * i / signal.length);
        }
        
        // Single scale
        CWTTransform transform = new CWTTransform(morletWavelet);
        double[] scales = {2.0};
        CWTResult cwtResult = transform.analyze(signal, scales);
        
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
    }
    
    @Test
    @DisplayName("Should handle large scale ranges")
    void testLargeScaleRanges() {
        InverseCWT inverseCWT = new InverseCWT(morletWavelet);
        
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 3 * i / signal.length);
        }
        
        // Wide range of scales
        double[] scales = new double[50];
        for (int i = 0; i < scales.length; i++) {
            scales[i] = 0.5 + i * 2.0;
        }
        
        CWTTransform transform = new CWTTransform(morletWavelet);
        CWTResult cwtResult = transform.analyze(signal, scales);
        
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
    }
    
    @Test
    @DisplayName("Should handle band-limited reconstruction")
    void testBandLimitedReconstruction() {
        InverseCWT inverseCWT = new InverseCWT(morletWavelet);
        
        // Signal with multiple frequency components
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 5 * i / signal.length) +
                       0.5 * Math.sin(2 * Math.PI * 15 * i / signal.length);
        }
        
        // Scales targeting specific frequency bands
        double[] scales = {2.0, 4.0, 8.0};
        
        CWTTransform transform = new CWTTransform(morletWavelet);
        CWTResult cwtResult = transform.analyze(signal, scales);
        
        // Standard reconstruction
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
    }
    
    @Test
    @DisplayName("Should handle zero signal")
    void testZeroSignal() {
        InverseCWT inverseCWT = new InverseCWT(morletWavelet);
        
        // All zeros signal
        double[] signal = new double[128];
        
        CWTTransform transform = new CWTTransform(morletWavelet);
        double[] scales = {1.0, 2.0, 4.0};
        CWTResult cwtResult = transform.analyze(signal, scales);
        
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Reconstructed should also be near zero
        for (double value : reconstructed) {
            assertTrue(Math.abs(value) < 1e-10,
                "Reconstructed zero signal should be near zero");
        }
    }
    
    @Test
    @DisplayName("Should handle impulse signal")
    void testImpulseSignal() {
        InverseCWT inverseCWT = new InverseCWT(morletWavelet);
        
        // Impulse signal
        double[] signal = new double[128];
        signal[64] = 1.0;
        
        CWTTransform transform = new CWTTransform(morletWavelet);
        double[] scales = {1.0, 2.0, 4.0, 8.0};
        CWTResult cwtResult = transform.analyze(signal, scales);
        
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Peak should be preserved around the impulse location
        double maxValue = 0;
        int maxIndex = 0;
        for (int i = 0; i < reconstructed.length; i++) {
            if (Math.abs(reconstructed[i]) > maxValue) {
                maxValue = Math.abs(reconstructed[i]);
                maxIndex = i;
            }
        }
        
        // Peak should be near original impulse location
        assertTrue(Math.abs(maxIndex - 64) < 10,
            "Impulse peak should be preserved near original location");
    }
    
    
    @Test
    @DisplayName("Should test progressive reconstruction")
    void testProgressiveReconstruction() {
        InverseCWT inverseCWT = new InverseCWT(morletWavelet);
        
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 8 * i / signal.length);
        }
        
        CWTTransform transform = new CWTTransform(morletWavelet);
        double[] scales = new double[10];
        for (int i = 0; i < scales.length; i++) {
            scales[i] = 1.0 + i * 0.5;
        }
        
        CWTResult cwtResult = transform.analyze(signal, scales);
        
        // Test reconstruction with different scale configurations
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Verify reconstruction preserves signal energy (approximately)
        double originalEnergy = 0, reconstructedEnergy = 0;
        for (int i = 0; i < signal.length; i++) {
            originalEnergy += signal[i] * signal[i];
            reconstructedEnergy += reconstructed[i] * reconstructed[i];
        }
        
        // CWT reconstruction doesn't perfectly preserve energy
        // but should be within a reasonable range
        double energyRatio = reconstructedEnergy / originalEnergy;
        assertTrue(energyRatio > 0.01 && energyRatio < 100,
            "Energy ratio should be reasonable: " + energyRatio);
    }
    
    @Test
    @DisplayName("Should test reconstruction with edge cases")
    void testReconstructionEdgeCases() {
        InverseCWT inverseCWT = new InverseCWT(morletWavelet);
        
        // Very small signal to test edge cases
        double[] signal = new double[8];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length);
        }
        
        CWTTransform transform = new CWTTransform(morletWavelet);
        
        // Edge case: single scale 
        double[] singleScale = {1.0};
        CWTResult singleScaleResult = transform.analyze(signal, singleScale);
        double[] singleReconstructed = inverseCWT.reconstruct(singleScaleResult);
        assertNotNull(singleReconstructed);
        assertEquals(signal.length, singleReconstructed.length);
        
        // Edge case: two scales
        double[] twoScales = {1.0, 2.0};
        CWTResult twoScaleResult = transform.analyze(signal, twoScales);
        double[] twoReconstructed = inverseCWT.reconstruct(twoScaleResult);
        assertNotNull(twoReconstructed);
        assertEquals(signal.length, twoReconstructed.length);
        
        // Edge case: band reconstruction with valid scale range
        double[] bandReconstructed = inverseCWT.reconstructBand(twoScaleResult, 1.0, 2.0);
        assertNotNull(bandReconstructed);
        assertEquals(signal.length, bandReconstructed.length);
    }
    
    @Test
    @DisplayName("Should test admissibility calculation methods")
    void testAdmissibilityCalculations() {
        // Test with different wavelet types to trigger admissibility calculations
        InverseCWT morletInverse = new InverseCWT(morletWavelet);
        InverseCWT dogInverse = new InverseCWT(dogWavelet);
        
        // Verify admissibility constant is calculated
        assertNotNull(morletInverse);
        assertNotNull(dogInverse);
        
        // Test that reconstruction works (which internally calls admissibility methods)
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 4 * i / signal.length);
        }
        
        // For Morlet wavelet
        CWTTransform morletTransform = new CWTTransform(morletWavelet);
        double[] scales = {2.0, 4.0};
        CWTResult morletResult = morletTransform.analyze(signal, scales);
        double[] morletReconstructed = morletInverse.reconstruct(morletResult);
        assertNotNull(morletReconstructed);
        assertEquals(signal.length, morletReconstructed.length);
        
        // For DOG wavelet - different code path
        CWTTransform dogTransform = new CWTTransform(dogWavelet);
        CWTResult dogResult = dogTransform.analyze(signal, scales);
        double[] dogReconstructed = dogInverse.reconstruct(dogResult);
        assertNotNull(dogReconstructed);
        assertEquals(signal.length, dogReconstructed.length);
    }
    
    @Test
    @DisplayName("Should test different reconstruction pathways")
    void testReconstructionPathways() {
        InverseCWT inverseCWT = new InverseCWT(morletWavelet);
        
        // Test with different signal sizes to trigger different reconstruction pathways
        int[] sizes = {16, 32, 128, 256};
        
        for (int size : sizes) {
            double[] signal = new double[size];
            for (int i = 0; i < signal.length; i++) {
                signal[i] = Math.sin(2 * Math.PI * 3 * i / signal.length) + 
                           0.5 * Math.cos(2 * Math.PI * 7 * i / signal.length);
            }
            
            CWTTransform transform = new CWTTransform(morletWavelet);
            double[] scales = {1.5, 3.0, 6.0};
            CWTResult result = transform.analyze(signal, scales);
            
            // This should trigger different internal reconstruction methods
            double[] reconstructed = inverseCWT.reconstruct(result);
            assertNotNull(reconstructed);
            assertEquals(signal.length, reconstructed.length);
            
            // Test band reconstruction as well
            double[] bandReconstructed = inverseCWT.reconstructBand(result, 1.5, 6.0);
            assertNotNull(bandReconstructed);
            assertEquals(signal.length, bandReconstructed.length);
        }
    }
    
    @Test
    @DisplayName("Should test complex matrix reconstruction")
    void testComplexMatrixReconstruction() {
        // Test with complex wavelets to trigger complex matrix code paths
        ContinuousWavelet complexWavelet = new MorletWavelet();
        InverseCWT inverseCWT = new InverseCWT(complexWavelet);
        
        // Create signal with specific characteristics
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 6 * i / signal.length) * 
                       Math.exp(-Math.pow(i - 64, 2) / (2 * 20 * 20)); // Gaussian envelope
        }
        
        // Use scales that will trigger complex calculations
        CWTTransform transform = new CWTTransform(complexWavelet);
        double[] scales = {2.0, 4.0, 8.0, 16.0};
        CWTResult result = transform.analyze(signal, scales);
        
        // This should exercise complex matrix reconstruction paths
        double[] reconstructed = inverseCWT.reconstruct(result);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Verify reconstruction has reasonable properties
        double maxValue = 0;
        for (double value : reconstructed) {
            maxValue = Math.max(maxValue, Math.abs(value));
        }
        assertTrue(maxValue > 0.001, "Reconstructed signal should have meaningful values");
    }
    
    @Test
    @DisplayName("Should test utility methods for increased coverage")
    void testUtilityMethods() {
        InverseCWT inverseCWT = new InverseCWT(morletWavelet);
        
        // Test isAdmissible method
        assertTrue(inverseCWT.isAdmissible(), "Morlet wavelet should be admissible");
        
        InverseCWT dogInverseCWT = new InverseCWT(dogWavelet);  
        assertTrue(dogInverseCWT.isAdmissible(), "DOG wavelet should be admissible");
        
        // Test getAdmissibilityConstant method
        double admissibilityConstant = inverseCWT.getAdmissibilityConstant();
        assertTrue(admissibilityConstant > 0, "Admissibility constant should be positive");
        
        // Test nextPowerOfTwo method by triggering reconstruction that uses it
        double[] signal = new double[100]; // Non-power-of-2 to trigger nextPowerOfTwo
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 4 * i / signal.length);
        }
        
        CWTTransform transform = new CWTTransform(morletWavelet);
        double[] scales = {2.0, 4.0};
        CWTResult result = transform.analyze(signal, scales);
        
        double[] reconstructed = inverseCWT.reconstruct(result);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
    }
    
    @Test
    @DisplayName("Should test complex reconstruction pathways to improve coverage")  
    void testComplexReconstructionCoverage() {
        // Test with a wavelet that might trigger different internal pathways
        InverseCWT inverseCWT = new InverseCWT(morletWavelet);
        
        // Create a more complex signal to exercise different reconstruction paths
        double[] signal = new double[512]; // Power of 2
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 10 * i / signal.length) +
                       0.5 * Math.sin(2 * Math.PI * 20 * i / signal.length) +
                       0.3 * Math.sin(2 * Math.PI * 30 * i / signal.length) +
                       0.2 * Math.random(); // Add some noise
        }
        
        // Use logarithmic scales to trigger log scale weight calculation
        CWTTransform transform = new CWTTransform(morletWavelet);
        ScaleSpace scales = ScaleSpace.logarithmic(1.0, 32.0, 20); // 20 logarithmic scales
        CWTResult result = transform.analyze(signal, scales);
        
        // This should trigger various internal methods including:
        // - calculateLogScaleWeights (already partially covered)
        // - reconstructInternal methods
        // - calculateIntegrationWeights
        double[] reconstructed = inverseCWT.reconstruct(result);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Also test band reconstruction to trigger more pathways
        double[] bandReconstructed = inverseCWT.reconstructBand(result, 2.0, 16.0);
        assertNotNull(bandReconstructed);
        assertEquals(signal.length, bandReconstructed.length);
    }
    
    @Test
    @DisplayName("Should test edge cases to increase coverage")
    void testEdgeCasesForCoverage() {
        InverseCWT inverseCWT = new InverseCWT(morletWavelet);
        
        // Test with very small signal
        double[] tinySignal = new double[4];
        for (int i = 0; i < tinySignal.length; i++) {
            tinySignal[i] = Math.sin(2 * Math.PI * i / tinySignal.length);
        }
        
        CWTTransform transform = new CWTTransform(morletWavelet);
        double[] scales = {0.5, 1.0};
        CWTResult tinyResult = transform.analyze(tinySignal, scales);
        
        double[] tinyReconstructed = inverseCWT.reconstruct(tinyResult);
        assertNotNull(tinyReconstructed);
        assertEquals(tinySignal.length, tinyReconstructed.length);
        
        // Test with larger power-of-2 signal to trigger FFT path  
        double[] largeSignal = new double[1024];
        for (int i = 0; i < largeSignal.length; i++) {
            largeSignal[i] = Math.sin(2 * Math.PI * 5 * i / largeSignal.length) * 
                           Math.exp(-Math.pow(i - 512, 2) / 10000.0);
        }
        
        double[] largeScales = {0.5, 1.0, 2.0, 4.0, 8.0, 16.0, 32.0, 64.0};
        CWTResult largeResult = transform.analyze(largeSignal, largeScales);
        
        double[] largeReconstructed = inverseCWT.reconstruct(largeResult);
        assertNotNull(largeReconstructed);
        assertEquals(largeSignal.length, largeReconstructed.length);
        
        // Test with very specific band to ensure edge cases in band reconstruction
        double[] bandResult = inverseCWT.reconstructBand(largeResult, 8.0, 32.0);
        assertNotNull(bandResult);
        assertEquals(largeSignal.length, bandResult.length);
    }
    
    @Test
    @DisplayName("Should trigger numerical admissibility calculation with unknown wavelet")
    void testNumericalAdmissibilityCalculation() {
        // Use a wavelet whose name doesn't match hardcoded patterns
        // This should trigger calculateAdmissibilityNumerical and waveletFourierTransform
        ContinuousWavelet rickerWavelet = new RickerWavelet();
        InverseCWT inverseCWT = new InverseCWT(rickerWavelet);
        
        // Verify that admissibility constant was calculated (should trigger numerical path)
        double admissibilityConstant = inverseCWT.getAdmissibilityConstant();
        assertTrue(admissibilityConstant > 0, "Admissibility constant should be positive");
        
        // Create a test signal
        double[] signal = new double[128];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 6 * i / signal.length) * 
                       Math.exp(-Math.pow(i - 64, 2) / 500.0);
        }
        
        // Perform CWT and reconstruction to exercise the numerical paths
        CWTTransform transform = new CWTTransform(rickerWavelet);
        double[] scales = {2.0, 4.0, 8.0, 16.0};
        CWTResult result = transform.analyze(signal, scales);
        
        // This reconstruction should use the numerically calculated admissibility constant
        double[] reconstructed = inverseCWT.reconstruct(result);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Also test with other wavelets that should trigger numerical calculation
        ContinuousWavelet gaussianWavelet = new GaussianDerivativeWavelet(1);
        InverseCWT gaussianInverse = new InverseCWT(gaussianWavelet);
        
        double gaussianAdmissibility = gaussianInverse.getAdmissibilityConstant();
        assertTrue(gaussianAdmissibility > 0, "Gaussian wavelet admissibility should be positive");
        
        CWTTransform gaussianTransform = new CWTTransform(gaussianWavelet);
        CWTResult gaussianResult = gaussianTransform.analyze(signal, scales);
        double[] gaussianReconstructed = gaussianInverse.reconstruct(gaussianResult);
        assertNotNull(gaussianReconstructed);
        assertEquals(signal.length, gaussianReconstructed.length);
    }
}