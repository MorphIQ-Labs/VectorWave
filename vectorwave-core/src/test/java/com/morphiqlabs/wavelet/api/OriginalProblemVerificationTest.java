package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.cwt.finance.ShannonGaborWavelet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verification test to ensure the original problem from issue #196 is completely resolved.
 * This test recreates the exact scenario described in the issue.
 */
class OriginalProblemVerificationTest {
    
    @Test
    @DisplayName("Verify original problem: Both SHANNON and FBSP using ShannonGaborWavelet is fixed")
    void testOriginalProblemIsFixed() {
        // Get wavelets from registry
        Wavelet shannon = WaveletRegistry.getWavelet(WaveletName.SHANNON);
        Wavelet fbsp = WaveletRegistry.getWavelet(WaveletName.FBSP);
        
        // The original problem was:
        // register(WaveletName.SHANNON, new ShannonGaborWavelet(1.0, 1.0));
        // register(WaveletName.FBSP, new ShannonGaborWavelet(1.0, 1.5));
        
        // Verify the problem is fixed:
        assertFalse(shannon instanceof ShannonGaborWavelet, 
            "SHANNON should NOT be ShannonGaborWavelet anymore");
        assertFalse(fbsp instanceof ShannonGaborWavelet,
            "FBSP should NOT be ShannonGaborWavelet anymore");
        
        // Verify they are now different classes (not the same incorrect class)
        assertNotEquals(shannon.getClass(), fbsp.getClass(),
            "SHANNON and FBSP should use different classes");
        
        // Print for verification
        System.out.println("✓ Fixed: SHANNON is now " + shannon.getClass().getSimpleName());
        System.out.println("✓ Fixed: FBSP is now " + fbsp.getClass().getSimpleName());
    }
    
    @Test
    @DisplayName("Verify wavelets now have distinct mathematical properties")
    void testDistinctMathematicalProperties() {
        ContinuousWavelet shannon = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.SHANNON);
        ContinuousWavelet fbsp = (ContinuousWavelet) WaveletRegistry.getWavelet(WaveletName.FBSP);
        
        // Test multiple points to ensure they produce different function values
        double[] testPoints = {0.0, 0.1, 0.5, 1.0, 2.0};
        int differentCount = 0;
        
        for (double t : testPoints) {
            double shannonVal = shannon.psi(t);
            double fbspVal = fbsp.psi(t);
            
            if (Math.abs(shannonVal - fbspVal) > 1e-10) {
                differentCount++;
            }
            
            System.out.printf("t=%.1f: Shannon=%.6f, FBSP=%.6f%n", 
                t, shannonVal, fbspVal);
        }
        
        // Should have different values at most points
        assertTrue(differentCount >= 3, 
            "Shannon and FBSP should produce different values at most test points");
    }
    
    @Test
    @DisplayName("Verify Shannon-Gabor is still available if needed")
    void testShannonGaborStillExists() {
        // Verify that ShannonGaborWavelet class still exists and is functional
        // (in case some other code depends on it)
        assertDoesNotThrow(() -> {
            ShannonGaborWavelet shanGabor = new ShannonGaborWavelet(1.0, 1.5);
            double val = shanGabor.psi(0.0);
            assertTrue(Double.isFinite(val), "Shannon-Gabor should still work");
        }, "ShannonGaborWavelet should still be available for direct use");
    }
}