package com.morphiqlabs.wavelet.cwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple tests to verify linear convolution implementation.
 */
class CWTLinearConvolutionTest {
    
    private CWTTransform transform;
    private MorletWavelet wavelet;
    
    @BeforeEach
    void setUp() {
        wavelet = new MorletWavelet(6.0, 1.0);
        transform = new CWTTransform(wavelet, CWTConfig.builder()
            .enableFFT(true)
            .normalizeScales(false)  // Disable normalization to simplify debugging
            .build());
    }
    
    @Test
    @DisplayName("Should not have circular artifacts at signal boundaries")
    void testNoCircularArtifacts() {
        // Create a signal that's zero everywhere except at the end
        int signalLength = 128;
        double[] signal = new double[signalLength];
        signal[signalLength - 5] = 1.0;  // Impulse near the end
        
        double[] scales = {8.0};
        CWTResult result = transform.analyze(signal, scales);
        double[][] coefficients = result.getCoefficients();
        
        // Check that the beginning of the signal has no response
        // In circular convolution, the wavelet would wrap around
        double beginValue = Math.abs(coefficients[0][0]);
        double impulseValue = Math.abs(coefficients[0][signalLength - 5]);
        
        // Beginning should have negligible response compared to impulse location
        assertTrue(beginValue < impulseValue * 0.01, 
            String.format("Beginning value %.6f should be much smaller than impulse value %.6f", 
                beginValue, impulseValue));
    }
    
    @Test
    @DisplayName("FFT size should be calculated correctly")
    void testFFTSizeCalculation() {
        // Small signal to verify FFT size calculation
        double[] signal = new double[64];
        double[] scales = {16.0};  // Large scale to test padding
        
        // The wavelet support is calculated as WAVELET_SUPPORT_FACTOR * scale * bandwidth
        // For scale=16.0 and bandwidth=1.0, this gives support = 8 * 16 * 1 = 128
        // FFT size should be at least signal_length + support - 1
        // The implementation will round up to the next power of 2
        
        signal[32] = 1.0;  // Impulse in the middle
        
        CWTResult result = transform.analyze(signal, scales);
        assertNotNull(result);
        
        // If FFT size is correct, we shouldn't see wrapping artifacts
        double[][] coeffs = result.getCoefficients();
        
        // The edge values are from the wavelet transform itself, not circular artifacts
        // With proper zero padding, these should match direct convolution
        // Let's verify by comparing with direct convolution
        CWTTransform directTransform = new CWTTransform(wavelet, CWTConfig.builder()
            .enableFFT(false)
            .normalizeScales(false)
            .build());
        CWTResult directResult = directTransform.analyze(signal, scales);
        double[][] directCoeffs = directResult.getCoefficients();
        
        // FFT and direct should match
        assertEquals(directCoeffs[0][0], coeffs[0][0], 0.001, 
            "FFT result should match direct convolution at left edge");
        assertEquals(directCoeffs[0][63], coeffs[0][63], 0.001, 
            "FFT result should match direct convolution at right edge");
    }
}