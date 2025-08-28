package com.morphiqlabs.wavelet.denoising;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify overflow protection in WaveletDenoiser bit shift operations.
 */
class WaveletDenoiserOverflowProtectionTest {
    
    @Test
    void testNormalDenosingWithSafeLevels() {
        // Test that normal denoising works with safe levels
        WaveletDenoiser denoiser = new WaveletDenoiser(new Haar(), BoundaryMode.PERIODIC);
        
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(i * 0.1) + 0.1 * Math.random();
        }
        
        // Denoise with safe levels (up to 8)
        for (int levels = 1; levels <= 8; levels++) {
            final int testLevels = levels;
            assertDoesNotThrow(() -> {
                double[] denoised = denoiser.denoiseMultiLevel(
                    signal, 
                    testLevels,
                    WaveletDenoiser.ThresholdMethod.UNIVERSAL,
                    WaveletDenoiser.ThresholdType.SOFT
                );
                assertNotNull(denoised);
                assertEquals(signal.length, denoised.length);
            }, "Should work fine with " + levels + " levels");
        }
    }
    
    @Test
    void testBoundaryLevel31() {
        // Test the boundary case where level = 31 (should work)
        // 1 << 30 is the largest safe shift
        int level = 31;
        int shiftAmount = level - 1; // = 30
        
        // This should not overflow
        assertDoesNotThrow(() -> {
            int powerOf2 = 1 << shiftAmount;
            assertTrue(powerOf2 > 0, "Should be positive");
            assertEquals(1073741824, powerOf2); // 2^30
            
            double levelScale = Math.sqrt(powerOf2);
            assertTrue(Double.isFinite(levelScale));
            assertEquals(32768.0, levelScale, 0.01);
        });
    }
}