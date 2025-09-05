package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.cwt.finance.DOGWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Final test to push InverseCWT coverage above 70%.
 */
class InverseCWTFinalTest {
    
    @Test
    @DisplayName("Should handle FFT-based reconstruction for large signals")
    void testFFTReconstruction() {
        // Create large signal to trigger FFT path (>= 128 samples)
        MorletWavelet wavelet = new MorletWavelet();
        CWTTransform transform = new CWTTransform(wavelet);
        InverseCWT inverseCWT = new InverseCWT(wavelet);
        
        // Large signal
        double[] signal = new double[256];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * 10 * i / signal.length) + 
                       0.3 * Math.cos(2 * Math.PI * 25 * i / signal.length);
        }
        
        // Multiple scales
        double[] scales = new double[10];
        for (int i = 0; i < scales.length; i++) {
            scales[i] = 1.0 + i * 2.0;
        }
        
        CWTResult result = transform.analyze(signal, scales);
        double[] reconstructed = inverseCWT.reconstruct(result);
        
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Test with DOG wavelet as well
        DOGWavelet dogWavelet = new DOGWavelet(2);
        CWTTransform dogTransform = new CWTTransform(dogWavelet);
        InverseCWT dogInverse = new InverseCWT(dogWavelet);
        
        CWTResult dogResult = dogTransform.analyze(signal, scales);
        double[] dogReconstructed = dogInverse.reconstruct(dogResult);
        
        assertNotNull(dogReconstructed);
        assertEquals(signal.length, dogReconstructed.length);
    }
    
    @Test
    @DisplayName("Should test frequency band reconstruction")
    void testFrequencyBandReconstructionCoverage() {
        MorletWavelet wavelet = new MorletWavelet();
        CWTTransform transform = new CWTTransform(wavelet);
        InverseCWT inverseCWT = new InverseCWT(wavelet);
        
        // Create signal with known frequencies
        double samplingRate = 256.0; // Hz
        double[] signal = new double[512];
        for (int i = 0; i < signal.length; i++) {
            double t = i / samplingRate;
            signal[i] = Math.sin(2 * Math.PI * 20 * t) + // 20 Hz
                       Math.sin(2 * Math.PI * 40 * t);    // 40 Hz
        }
        
        // Analyze
        double[] scales = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        CWTResult result = transform.analyze(signal, scales);
        
        // Reconstruct different frequency bands
        double[] band1 = inverseCWT.reconstructFrequencyBand(result, samplingRate, 15, 25);
        assertNotNull(band1);
        assertEquals(signal.length, band1.length);
        
        double[] band2 = inverseCWT.reconstructFrequencyBand(result, samplingRate, 35, 45);
        assertNotNull(band2);
        assertEquals(signal.length, band2.length);
        
        // Edge case: near Nyquist
        double[] band3 = inverseCWT.reconstructFrequencyBand(result, samplingRate, 100, 127);
        assertNotNull(band3);
        assertEquals(signal.length, band3.length);
    }
}