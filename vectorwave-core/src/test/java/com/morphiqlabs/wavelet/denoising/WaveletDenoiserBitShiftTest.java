package com.morphiqlabs.wavelet.denoising;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify bit shift optimization produces correct results in WaveletDenoiser.
 */
class WaveletDenoiserBitShiftTest {
    
    @Test
    void testBitShiftProducesSameResultAsMathPow() {
        // Verify the mathematical equivalence for relevant levels
        for (int level = 1; level <= 10; level++) {
            double powResult = Math.sqrt(Math.pow(2, level - 1));
            double bitShiftResult = Math.sqrt(1 << (level - 1));
            
            assertEquals(powResult, bitShiftResult, 1e-15,
                "Bit shift should produce same result as Math.pow for level " + level);
        }
    }
    
    @Test
    void testMultiLevelDenoisingWithBitShift() {
        // Create a noisy signal
        int signalLength = 256;
        double[] signal = new double[signalLength];
        double[] cleanSignal = new double[signalLength];
        double noiseLevel = 0.5;  // Higher noise level for more significant effect
        
        // Generate signal with noise
        for (int i = 0; i < signalLength; i++) {
            cleanSignal[i] = Math.sin(2 * Math.PI * i / 32);  // Clean signal
            signal[i] = cleanSignal[i] + noiseLevel * (Math.random() - 0.5);  // Add noise
        }
        
        // Create denoiser
        WaveletDenoiser denoiser = new WaveletDenoiser(
            Daubechies.DB4, 
            BoundaryMode.PERIODIC
        );
        
        // Perform multi-level denoising
        double[] denoised = denoiser.denoiseMultiLevel(
            signal, 
            4,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdType.SOFT
        );
        
        // Verify denoising worked
        assertNotNull(denoised);
        assertEquals(signalLength, denoised.length);
        
        // Calculate SNR improvement
        double inputSNR = calculateSNR(cleanSignal, signal);
        double outputSNR = calculateSNR(cleanSignal, denoised);
        
        // Should have improved SNR
        assertTrue(outputSNR > inputSNR,
            "Denoising should improve SNR. Input SNR: " + inputSNR + ", Output SNR: " + outputSNR);
    }
    
    @Test
    void testLevelScalingCorrectness() {
        // Create a signal
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.random();
        }
        
        WaveletDenoiser denoiser = new WaveletDenoiser(
            Daubechies.DB4,
            BoundaryMode.PERIODIC
        );
        
        // Denoise at different levels and verify scaling
        for (int levels = 1; levels <= 5; levels++) {
            double[] denoised = denoiser.denoiseMultiLevel(
                signal, 
                levels, 
                WaveletDenoiser.ThresholdMethod.UNIVERSAL,
                WaveletDenoiser.ThresholdType.SOFT
            );
            
            assertNotNull(denoised);
            assertEquals(signal.length, denoised.length);
            
            // Verify no NaN or infinite values (which could indicate overflow)
            for (double value : denoised) {
                assertTrue(Double.isFinite(value),
                    "All denoised values should be finite");
            }
        }
    }
    
    @Test
    void testBitShiftOverflowProtection() {
        // Test that we don't overflow with high levels
        // For level 31, (1 << 30) is still valid
        // For level 32, (1 << 31) would overflow to negative
        
        for (int level = 1; level <= 30; level++) {
            int powerOf2 = 1 << (level - 1);
            assertTrue(powerOf2 > 0, 
                "Bit shift should not overflow for level " + level);
            
            double levelScale = Math.sqrt(powerOf2);
            assertTrue(Double.isFinite(levelScale) && levelScale > 0,
                "Level scale should be positive and finite for level " + level);
        }
    }
    
    private double calculateSNR(double[] clean, double[] noisy) {
        // Calculate signal-to-noise ratio in dB
        double signalPower = 0;
        double noisePower = 0;
        
        for (int i = 0; i < clean.length; i++) {
            signalPower += clean[i] * clean[i];
            double noise = noisy[i] - clean[i];
            noisePower += noise * noise;
        }
        
        signalPower /= clean.length;
        noisePower /= clean.length;
        
        if (noisePower == 0) {
            return Double.POSITIVE_INFINITY;
        }
        
        return 10 * Math.log10(signalPower / noisePower);
    }
}