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
    @DisplayName("Test real FFT vs standard FFT produces identical results")
    void testRealFFTvsStandardFFT() {
        // Create test signal
        double[] signal = generateChirpSignal(512, 0.01, 0.4, 1.0);
        double[] scales = {1, 2, 4, 8, 16};
        
        // Create wavelet
        MorletWavelet wavelet = new MorletWavelet();
        
        // Test with standard FFT (force complex FFT)
        CWTConfig standardConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.RADIX2)
            .build();
        CWTTransform standardTransform = new CWTTransform(wavelet, standardConfig);
        CWTResult standardResult = standardTransform.analyze(signal, scales);
        
        // Test with real-optimized FFT
        CWTConfig realConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.REAL_OPTIMIZED)
            .build();
        CWTTransform realTransform = new CWTTransform(wavelet, realConfig);
        CWTResult realResult = realTransform.analyze(signal, scales);
        
        // Compare results
        double[][] standardCoeffs = standardResult.getCoefficients();
        double[][] realCoeffs = realResult.getCoefficients();
        
        assertEquals(standardCoeffs.length, realCoeffs.length, "Number of scales should match");
        
        for (int s = 0; s < standardCoeffs.length; s++) {
            assertArrayEquals(standardCoeffs[s], realCoeffs[s], TOLERANCE,
                "Coefficients should match for scale " + scales[s]);
        }
    }
    
    @Test
    @DisplayName("Test AUTO algorithm selection uses real FFT for appropriate signals")
    void testAutoAlgorithmSelection() {
        // Even-length signal >= 256 samples should use real FFT
        double[] signal = generateChirpSignal(512, 0.01, 0.4, 1.0);
        double[] scales = {1, 2, 4, 8};
        
        MorletWavelet wavelet = new MorletWavelet();
        
        // AUTO should select real FFT for this signal
        CWTConfig autoConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.AUTO)
            .build();
        CWTTransform autoTransform = new CWTTransform(wavelet, autoConfig);
        CWTResult autoResult = autoTransform.analyze(signal, scales);
        
        // Compare with explicit real FFT
        CWTConfig realConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.REAL_OPTIMIZED)
            .build();
        CWTTransform realTransform = new CWTTransform(wavelet, realConfig);
        CWTResult realResult = realTransform.analyze(signal, scales);
        
        // Results should be identical
        double[][] autoCoeffs = autoResult.getCoefficients();
        double[][] realCoeffs = realResult.getCoefficients();
        
        for (int s = 0; s < autoCoeffs.length; s++) {
            assertArrayEquals(autoCoeffs[s], realCoeffs[s], TOLERANCE,
                "AUTO should produce same results as REAL_OPTIMIZED for scale " + scales[s]);
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
        
        // Standard FFT
        CWTConfig standardConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.RADIX2)
            .build();
        CWTTransform standardTransform = new CWTTransform(wavelet, standardConfig);
        CWTResult standardResult = standardTransform.analyze(signal, scales);
        
        // Real FFT
        CWTConfig realConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.REAL_OPTIMIZED)
            .build();
        CWTTransform realTransform = new CWTTransform(wavelet, realConfig);
        CWTResult realResult = realTransform.analyze(signal, scales);
        
        // Verify results match
        double[][] standardCoeffs = standardResult.getCoefficients();
        double[][] realCoeffs = realResult.getCoefficients();
        
        for (int s = 0; s < scales.length; s++) {
            double maxDiff = 0.0;
            for (int t = 0; t < signalSize; t++) {
                double diff = Math.abs(standardCoeffs[s][t] - realCoeffs[s][t]);
                maxDiff = Math.max(maxDiff, diff);
            }
            assertTrue(maxDiff < TOLERANCE, 
                String.format("Max difference %.3e exceeds tolerance for size %d, scale %.1f", 
                    maxDiff, signalSize, scales[s]));
        }
    }
    
    @Test
    @DisplayName("Test real FFT handles odd-length signals correctly")
    void testOddLengthSignals() {
        // Odd-length signal should fall back to standard FFT
        double[] signal = generateChirpSignal(511, 0.01, 0.4, 1.0);
        double[] scales = {1, 2, 4, 8};
        
        MorletWavelet wavelet = new MorletWavelet();
        
        // Both should produce same results since odd-length falls back
        CWTConfig standardConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.RADIX2)
            .build();
        CWTTransform standardTransform = new CWTTransform(wavelet, standardConfig);
        CWTResult standardResult = standardTransform.analyze(signal, scales);
        
        CWTConfig realConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.REAL_OPTIMIZED)
            .build();
        CWTTransform realTransform = new CWTTransform(wavelet, realConfig);
        CWTResult realResult = realTransform.analyze(signal, scales);
        
        // Compare results
        double[][] standardCoeffs = standardResult.getCoefficients();
        double[][] realCoeffs = realResult.getCoefficients();
        
        for (int s = 0; s < scales.length; s++) {
            assertArrayEquals(standardCoeffs[s], realCoeffs[s], TOLERANCE,
                "Odd-length signals should produce identical results");
        }
    }
    
    @Test
    @DisplayName("Test real FFT with small signals uses standard FFT")
    void testSmallSignalFallback() {
        // Small signal (< 256) should use standard FFT even with REAL_OPTIMIZED
        double[] signal = generateChirpSignal(128, 0.01, 0.4, 1.0);
        double[] scales = {1, 2, 4};
        
        MorletWavelet wavelet = new MorletWavelet();
        
        CWTConfig standardConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.RADIX2)
            .build();
        CWTTransform standardTransform = new CWTTransform(wavelet, standardConfig);
        CWTResult standardResult = standardTransform.analyze(signal, scales);
        
        CWTConfig realConfig = CWTConfig.builder()
            .enableFFT(true)
            .fftAlgorithm(FFTAlgorithm.REAL_OPTIMIZED)
            .build();
        CWTTransform realTransform = new CWTTransform(wavelet, realConfig);
        CWTResult realResult = realTransform.analyze(signal, scales);
        
        // Should produce identical results
        double[][] standardCoeffs = standardResult.getCoefficients();
        double[][] realCoeffs = realResult.getCoefficients();
        
        for (int s = 0; s < scales.length; s++) {
            assertArrayEquals(standardCoeffs[s], realCoeffs[s], TOLERANCE,
                "Small signals should use standard FFT");
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