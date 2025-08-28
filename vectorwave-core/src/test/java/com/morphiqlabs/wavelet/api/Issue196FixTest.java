package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.cwt.FrequencyBSplineWavelet;
import com.morphiqlabs.wavelet.cwt.finance.ClassicalShannonWavelet;
import com.morphiqlabs.wavelet.cwt.finance.ShannonGaborWavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the specific issue mentioned in GitHub issue #196 is fixed.
 * Previously, both SHANNON and FBSP wavelets incorrectly used ShannonGaborWavelet.
 */
class Issue196FixTest {
    
    @Test
    @DisplayName("Shannon and FBSP wavelets should no longer both use ShannonGaborWavelet")
    void testShannonAndFBSPNoDifferentImplementations() {
        Wavelet shannon = WaveletRegistry.getWavelet(WaveletName.SHANNON);
        Wavelet fbsp = WaveletRegistry.getWavelet(WaveletName.FBSP);
        
        // Verify they are NOT both ShannonGaborWavelet instances
        assertFalse(shannon instanceof ShannonGaborWavelet && fbsp instanceof ShannonGaborWavelet,
            "SHANNON and FBSP should not both use ShannonGaborWavelet");
        
        // Verify correct types
        assertTrue(shannon instanceof ClassicalShannonWavelet, 
            "SHANNON should use ClassicalShannonWavelet implementation");
        assertTrue(fbsp instanceof FrequencyBSplineWavelet,
            "FBSP should use FrequencyBSplineWavelet implementation");
        
        // Verify they are different classes
        assertNotEquals(shannon.getClass(), fbsp.getClass(),
            "SHANNON and FBSP should use different wavelet classes");
    }
    
    @Test
    @DisplayName("Verify mathematical correctness of separated wavelets")
    void testMathematicalCorrectness() {
        ContinuousWavelet shannon = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.SHANNON);
        ContinuousWavelet fbsp = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.FBSP);
        
        // Test at t=0 where both should have defined values
        double shannonAt0 = shannon.psi(0);
        double fbspAt0 = fbsp.psi(0);
        
        // Values should be finite and non-zero
        assertTrue(Double.isFinite(shannonAt0), "Shannon value at t=0 should be finite");
        assertTrue(Double.isFinite(fbspAt0), "FBSP value at t=0 should be finite");
        
        // Values should be different
        assertNotEquals(shannonAt0, fbspAt0, 1e-10, 
            "Shannon and FBSP should produce different values at t=0");
    }
    
    @Test
    @DisplayName("Verify wavelets have expected characteristics")
    void testWaveletCharacteristics() {
        ContinuousWavelet shannon = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.SHANNON);
        ContinuousWavelet fbsp = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.FBSP);
        
        // Shannon should be real
        assertFalse(shannon.isComplex(), "Shannon wavelet should be real-valued");
        
        // FBSP should be complex
        assertTrue(fbsp.isComplex(), "FBSP wavelet should be complex-valued");
        
        // Both should have positive bandwidth and center frequency
        assertTrue(shannon.bandwidth() > 0, "Shannon bandwidth should be positive");
        assertTrue(shannon.centerFrequency() > 0, "Shannon center frequency should be positive");
        assertTrue(fbsp.bandwidth() > 0, "FBSP bandwidth should be positive");
        assertTrue(fbsp.centerFrequency() > 0, "FBSP center frequency should be positive");
    }
    
    @Test
    @DisplayName("Verify comprehensive mathematical separation")
    void testComprehensiveMathematicalSeparation() {
        com.morphiqlabs.wavelet.cwt.finance.ClassicalShannonWavelet shannon = 
            (com.morphiqlabs.wavelet.cwt.finance.ClassicalShannonWavelet) WaveletRegistry.getWavelet(WaveletName.SHANNON);
        FrequencyBSplineWavelet fbsp = (FrequencyBSplineWavelet) WaveletRegistry.getWavelet(WaveletName.FBSP);
        
        // Verify registry configuration parameters 
        assertEquals(0.375, shannon.centerFrequency(), 1e-10);
        assertEquals(0.25, shannon.bandwidth(), 1e-10);
        assertEquals(2, fbsp.getOrder());
        assertEquals(1.0, fbsp.bandwidth(), 1e-10);
        assertEquals(1.0, fbsp.centerFrequency(), 1e-10);
        
        // Mathematical definitions should produce different values at multiple points
        // Use points where both wavelets are non-zero
        double[] testPoints = {0.0, 0.1, 0.3, 0.7, 1.5, 2.5};
        for (double t : testPoints) {
            double shannonVal = shannon.psi(t);
            double fbspVal = fbsp.psi(t);
            
            // Both should be finite
            assertTrue(Double.isFinite(shannonVal), 
                String.format("Shannon value at t=%.1f should be finite", t));
            assertTrue(Double.isFinite(fbspVal), 
                String.format("FBSP value at t=%.1f should be finite", t));
            
            // Values should be mathematically different (except when both are zero)
            if (Math.abs(shannonVal) > 1e-6 || Math.abs(fbspVal) > 1e-6) {
                assertNotEquals(shannonVal, fbspVal, 1e-3,
                    String.format("Shannon and FBSP should produce different values at t=%.1f", t));
            }
        }
        
        // Verify different discretizations
        double[] shannonSamples = shannon.discretize(64);
        double[] fbspSamples = fbsp.discretize(64);
        
        boolean foundSignificantDifference = false;
        for (int i = 0; i < 64; i++) {
            if (Math.abs(shannonSamples[i] - fbspSamples[i]) > 0.1) {
                foundSignificantDifference = true;
                break;
            }
        }
        assertTrue(foundSignificantDifference, 
            "Shannon and FBSP discretizations should have significant differences");
    }
}