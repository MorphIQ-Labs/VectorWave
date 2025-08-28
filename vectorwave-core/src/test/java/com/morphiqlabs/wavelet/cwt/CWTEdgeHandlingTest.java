package com.morphiqlabs.wavelet.cwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for proper edge handling in CWT to verify no circular convolution artifacts.
 */
class CWTEdgeHandlingTest {
    
    private CWTTransform transform;
    private MorletWavelet wavelet;
    private static final double EPSILON = 1e-10;
    private static final double MIN_TOLERANCE = 0.001;  // Minimum absolute tolerance
    private static final double RELATIVE_TOLERANCE_FACTOR = 0.01;  // 1% relative tolerance
    
    @BeforeEach
    void setUp() {
        wavelet = new MorletWavelet(6.0, 1.0);
        transform = new CWTTransform(wavelet);
    }
    
    @Test
    @DisplayName("Should handle signal edges without circular artifacts")
    void testEdgeHandlingNoCircularArtifacts() {
        // Create a signal with a sharp edge at the beginning
        int signalLength = 256;
        double[] signal = new double[signalLength];
        
        // Step function: zeros for first half, ones for second half
        for (int i = signalLength / 2; i < signalLength; i++) {
            signal[i] = 1.0;
        }
        
        // Analyze with FFT
        double[] scales = {4.0, 8.0, 16.0};
        CWTTransform fftTransform = new CWTTransform(wavelet, CWTConfig.builder()
            .enableFFT(true)
            .build());
        CWTResult result = fftTransform.analyze(signal, scales);
        
        double[][] coefficients = result.getCoefficients();
        
        // Check edges - there should be no significant response at the very beginning
        // if circular convolution is avoided
        for (int s = 0; s < scales.length; s++) {
            double edgeValue = Math.abs(coefficients[s][0]);
            double midValue = Math.abs(coefficients[s][signalLength / 2]);
            
            // Edge should have much smaller response than the actual step location
            assertTrue(edgeValue < midValue * 0.1, 
                String.format("Scale %.1f: Edge value %.6f should be much smaller than mid value %.6f", 
                    scales[s], edgeValue, midValue));
        }
    }
    
    @Test
    @DisplayName("Should produce identical results for delta function regardless of position")
    void testDeltaFunctionPositionInvariance() {
        int signalLength = 256;
        double[] scales = {8.0};
        
        // Test delta at positions far from edges to avoid boundary effects
        // With scale 8.0 and bandwidth 1.0, wavelet support is ~32 samples
        int[] positions = {64, 128, 192};
        double[][][] results = new double[positions.length][][];
        
        for (int p = 0; p < positions.length; p++) {
            double[] signal = new double[signalLength];
            signal[positions[p]] = 1.0; // Delta function
            
            CWTTransform fftTransform = new CWTTransform(wavelet, CWTConfig.builder()
                .enableFFT(true)
                .build());
            CWTResult result = fftTransform.analyze(signal, scales);
            
            results[p] = result.getCoefficients();
        }
        
        // Compare peak values at delta positions
        double peak0 = results[0][0][positions[0]];
        double peak1 = results[1][0][positions[1]];
        double peak2 = results[2][0][positions[2]];
        
        // All peaks should be identical (position invariance away from boundaries)
        assertEquals(peak0, peak1, 0.001, 
            "Delta response peaks should be position invariant");
        assertEquals(peak1, peak2, 0.001, 
            "Delta response peaks should be position invariant");
        
        // Also check the wavelet shape around each peak
        int responseWidth = 10;
        for (int i = -responseWidth; i <= responseWidth; i++) {
            int idx0 = positions[0] + i;
            int idx1 = positions[1] + i;
            int idx2 = positions[2] + i;
            
            double val0 = results[0][0][idx0];
            double val1 = results[1][0][idx1];
            double val2 = results[2][0][idx2];
            
            // Use relative tolerance
            double avgValue = (Math.abs(val0) + Math.abs(val1) + Math.abs(val2)) / 3.0;
            double tolerance = Math.max(MIN_TOLERANCE, avgValue * RELATIVE_TOLERANCE_FACTOR);
            
            assertEquals(val0, val1, tolerance, 
                String.format("Wavelet shape should be invariant at offset %d", i));
            assertEquals(val1, val2, tolerance, 
                String.format("Wavelet shape should be invariant at offset %d", i));
        }
    }
    
    @Test
    @DisplayName("Should handle zero-padded signal correctly")
    void testZeroPaddedSignal() {
        // Short signal with explicit zero padding
        double[] shortSignal = {1.0, 2.0, 3.0, 2.0, 1.0};
        int paddedLength = 64;
        double[] paddedSignal = new double[paddedLength];
        System.arraycopy(shortSignal, 0, paddedSignal, 0, shortSignal.length);
        
        double[] scales = {2.0, 4.0};
        
        // Analyze with FFT
        CWTTransform fftTransform = new CWTTransform(wavelet, CWTConfig.builder()
            .enableFFT(true)
            .build());
        CWTResult result = fftTransform.analyze(paddedSignal, scales);
        
        double[][] coefficients = result.getCoefficients();
        
        // Check that coefficients decay to near zero in the padded region
        for (int s = 0; s < scales.length; s++) {
            // Check far from the signal (should be near zero)
            for (int i = paddedLength - 10; i < paddedLength; i++) {
                assertTrue(Math.abs(coefficients[s][i]) < 0.01,
                    String.format("Coefficients in padded region should be near zero at scale %.1f, position %d", 
                        scales[s], i));
            }
        }
    }
    
    @Test
    @DisplayName("Should match direct convolution for small signals")
    void testFFTMatchesDirectConvolution() {
        // Small signal where we can compare FFT vs direct
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16.0);
        }
        
        double[] scales = {4.0, 8.0};
        
        // Analyze with FFT
        CWTTransform fftTransform = new CWTTransform(wavelet, CWTConfig.builder()
            .enableFFT(true)
            .build());
        CWTResult fftResult = fftTransform.analyze(signal, scales);
        
        // Analyze with direct convolution
        CWTTransform directTransform = new CWTTransform(wavelet, CWTConfig.builder()
            .enableFFT(false)
            .build());
        CWTResult directResult = directTransform.analyze(signal, scales);
        
        double[][] fftCoeffs = fftResult.getCoefficients();
        double[][] directCoeffs = directResult.getCoefficients();
        
        // Compare results
        for (int s = 0; s < scales.length; s++) {
            for (int t = 10; t < signal.length - 10; t++) { // Skip edges where boundary handling differs
                assertEquals(directCoeffs[s][t], fftCoeffs[s][t], 0.01,
                    String.format("FFT and direct convolution should match at scale %.1f, time %d", 
                        scales[s], t));
            }
        }
    }
    
    @Test
    @DisplayName("Should handle real-only FFT without circular artifacts")
    void testRealFFTNoCircularArtifacts() {
        // Test that the real FFT path doesn't have circular artifacts
        // Create test signals
        int signalLength = 128;
        double[] signalBegin = new double[signalLength];
        double[] signalEnd = new double[signalLength];
        
        signalBegin[5] = 1.0;  // Impulse near beginning
        signalEnd[signalLength - 5] = 1.0;  // Impulse near end
        
        double[] scales = {8.0};
        
        CWTTransform fftTransform = new CWTTransform(wavelet, 
            CWTConfig.builder().enableFFT(true).build());
        
        CWTResult resultBegin = fftTransform.analyze(signalBegin, scales);
        CWTResult resultEnd = fftTransform.analyze(signalEnd, scales);
        
        double[][] coeffsBegin = resultBegin.getCoefficients();
        double[][] coeffsEnd = resultEnd.getCoefficients();
        
        // With linear convolution, the end should not affect the beginning
        double beginResponse = Math.abs(coeffsEnd[0][0]);
        double endResponse = Math.abs(coeffsBegin[0][signalLength - 1]);
        
        // Both should be near zero (no circular wrapping)
        assertTrue(beginResponse < 0.001, 
            "Beginning should have no response from end impulse");
        assertTrue(endResponse < 0.001, 
            "End should have no response from beginning impulse");
    }
}