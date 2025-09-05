package com.morphiqlabs.wavelet.cwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for real-to-complex FFT optimization in CWT.
 * Verifies that real FFT produces identical results to standard FFT
 * while providing performance benefits.
 */
class RealFFTOptimizationTest {
    
    private static final double TOLERANCE = 1e-10;
    
    @Test
    @DisplayName("FFT vs Direct CWT produce identical results")
    void testFFTvsDirectParity() {
        // Create test signal
        double[] signal = generateChirpSignal(512, 0.01, 0.4, 1.0);
        double[] scales = {1, 2, 4, 8, 16};
        
        // Create wavelet
        MorletWavelet wavelet = new MorletWavelet();
        
        // FFT-enabled
        CWTConfig fftConfig = CWTConfig.builder()
            .enableFFT(true)
            .build();
        CWTTransform fftTransform = new CWTTransform(wavelet, fftConfig);
        CWTResult fftResult = fftTransform.analyze(signal, scales);

        // Direct (no FFT)
        CWTConfig directConfig = CWTConfig.builder()
            .enableFFT(false)
            .build();
        CWTTransform directTransform = new CWTTransform(wavelet, directConfig);
        CWTResult directResult = directTransform.analyze(signal, scales);
        
        // Compare results
        double[][] fftCoeffs = fftResult.getCoefficients();
        double[][] directCoeffs = directResult.getCoefficients();
        
        assertEquals(directCoeffs.length, fftCoeffs.length, "Number of scales should match");
        
        for (int s = 0; s < fftCoeffs.length; s++) {
            assertArrayEquals(directCoeffs[s], fftCoeffs[s], TOLERANCE,
                "FFT and direct coefficients should match for scale " + scales[s]);
        }
    }
    
    @Test
    @DisplayName("FFT-enabled matches FFT-enabled (deterministic)")
    void testDeterminism() {
        // Even-length signal >= 256 samples should use real FFT
        double[] signal = generateChirpSignal(512, 0.01, 0.4, 1.0);
        double[] scales = {1, 2, 4, 8};
        
        MorletWavelet wavelet = new MorletWavelet();
        
        CWTConfig cfg = CWTConfig.builder().enableFFT(true).build();
        CWTTransform t1 = new CWTTransform(wavelet, cfg);
        CWTTransform t2 = new CWTTransform(wavelet, cfg);
        CWTResult r1 = t1.analyze(signal, scales);
        CWTResult r2 = t2.analyze(signal, scales);
        
        // Results should be identical
        double[][] c1 = r1.getCoefficients();
        double[][] c2 = r2.getCoefficients();
        
        for (int s = 0; s < c1.length; s++) {
            assertArrayEquals(c1[s], c2[s], TOLERANCE,
                "FFT analysis should be deterministic for scale " + scales[s]);
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {256, 512, 1024, 2048, 4096})
    @DisplayName("Test real FFT optimization for various signal sizes")
    void testRealFFTVariousSizes(int signalSize) {
        // Generate test signal
        double[] signal = new double[signalSize];
        for (int i = 0; i < signalSize; i++) {
            signal[i] = Math.sin(2 * Math.PI * 10 * i / signalSize) +
                       0.5 * Math.sin(2 * Math.PI * 25 * i / signalSize);
        }
        
        double[] scales = {1, 2, 4, 8, 16, 32};
        MorletWavelet wavelet = new MorletWavelet();
        
        // FFT-enabled vs direct
        CWTConfig fftConfig = CWTConfig.builder().enableFFT(true).build();
        CWTConfig directConfig = CWTConfig.builder().enableFFT(false).build();
        CWTTransform fftTransform = new CWTTransform(wavelet, fftConfig);
        CWTTransform directTransform = new CWTTransform(wavelet, directConfig);
        CWTResult fftResult = fftTransform.analyze(signal, scales);
        CWTResult directResult = directTransform.analyze(signal, scales);
        
        // Verify results match
        double[][] fftCoeffs = fftResult.getCoefficients();
        double[][] directCoeffs = directResult.getCoefficients();
        
        for (int s = 0; s < scales.length; s++) {
            double maxDiff = 0.0;
            for (int t = 0; t < signalSize; t++) {
                double diff = Math.abs(directCoeffs[s][t] - fftCoeffs[s][t]);
                maxDiff = Math.max(maxDiff, diff);
            }
            assertTrue(maxDiff < TOLERANCE, 
                String.format("Max difference %.3e exceeds tolerance for size %d, scale %.1f", 
                    maxDiff, signalSize, scales[s]));
        }
    }
    
    @Test
    @DisplayName("FFT handles odd-length signals consistently with direct")
    void testOddLengthSignals() {
        // Odd-length signal should fall back to standard FFT
        double[] signal = generateChirpSignal(511, 0.01, 0.4, 1.0);
        double[] scales = {1, 2, 4, 8};
        
        MorletWavelet wavelet = new MorletWavelet();
        
        CWTConfig fftConfig = CWTConfig.builder().enableFFT(true).build();
        CWTConfig directConfig = CWTConfig.builder().enableFFT(false).build();
        CWTTransform fftTransform = new CWTTransform(wavelet, fftConfig);
        CWTTransform directTransform = new CWTTransform(wavelet, directConfig);
        CWTResult fftResult = fftTransform.analyze(signal, scales);
        CWTResult directResult = directTransform.analyze(signal, scales);
        
        // Compare results
        double[][] fftCoeffs = fftResult.getCoefficients();
        double[][] directCoeffs = directResult.getCoefficients();
        
        for (int s = 0; s < scales.length; s++) {
            assertArrayEquals(directCoeffs[s], fftCoeffs[s], TOLERANCE,
                "Odd-length signals should match between FFT and direct");
        }
    }
    
    @Test
    @DisplayName("Small signals: FFT path equals direct path")
    void testSmallSignalFallback() {
        // Small signal (< 256) should use standard FFT even with REAL_OPTIMIZED
        double[] signal = generateChirpSignal(128, 0.01, 0.4, 1.0);
        double[] scales = {1, 2, 4};
        
        MorletWavelet wavelet = new MorletWavelet();
        
        CWTConfig fftConfig = CWTConfig.builder().enableFFT(true).build();
        CWTConfig directConfig = CWTConfig.builder().enableFFT(false).build();
        CWTTransform fftTransform = new CWTTransform(wavelet, fftConfig);
        CWTTransform directTransform = new CWTTransform(wavelet, directConfig);
        CWTResult fftResult = fftTransform.analyze(signal, scales);
        CWTResult directResult = directTransform.analyze(signal, scales);
        
        // Should produce identical results
        double[][] fftCoeffs = fftResult.getCoefficients();
        double[][] directCoeffs = directResult.getCoefficients();
        
        for (int s = 0; s < scales.length; s++) {
            assertArrayEquals(directCoeffs[s], fftCoeffs[s], TOLERANCE,
                "Small signals should match between FFT and direct");
        }
    }
    
    
    // Helper method to generate chirp signal
    private static double[] generateChirpSignal(int length, double f0, double f1, double amplitude) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double freq = f0 + (f1 - f0) * t;
            signal[i] = amplitude * Math.sin(2 * Math.PI * freq * i);
        }
        return signal;
    }
}
