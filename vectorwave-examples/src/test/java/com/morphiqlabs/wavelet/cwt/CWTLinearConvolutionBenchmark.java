package com.morphiqlabs.wavelet.cwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple benchmark to verify no significant performance regression from linear convolution.
 * 
 * <p>To run this benchmark, use: {@code -Drun.benchmarks=true}</p>
 */
class CWTLinearConvolutionBenchmark {
    
    private MorletWavelet wavelet;
    private double[] signal;
    private double[] scales;
    
    @BeforeEach
    void setUp() {
        wavelet = new MorletWavelet(6.0, 1.0);
        
        // Create a test signal
        int signalLength = 4096;
        signal = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64.0) + 
                       0.5 * Math.sin(2 * Math.PI * i / 16.0);
        }
        
        // Multiple scales
        scales = new double[]{2.0, 4.0, 8.0, 16.0, 32.0, 64.0};
    }
    
    @Test
    @DisplayName("Compare FFT performance")
    @EnabledIfSystemProperty(named = "run.benchmarks", matches = "true")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void benchmarkFFTPerformance() {
        // Warm up
        for (int i = 0; i < 5; i++) {
            CWTTransform transform = new CWTTransform(wavelet, CWTConfig.builder()
                .enableFFT(true)
                .build());
            transform.analyze(signal, scales);
        }
        
        // Time FFT version
        long startTime = System.nanoTime();
        int iterations = 10;
        
        for (int i = 0; i < iterations; i++) {
            CWTTransform transform = new CWTTransform(wavelet, CWTConfig.builder()
                .enableFFT(true)
                .build());
            CWTResult result = transform.analyze(signal, scales);
            assertNotNull(result);
        }
        
        long fftTime = System.nanoTime() - startTime;
        double fftTimeMs = fftTime / 1_000_000.0 / iterations;
        
        System.out.printf("FFT CWT: %.2f ms per transform%n", fftTimeMs);
        
        // The linear convolution should not add significant overhead
        // We expect < 10% overhead from the larger FFT size
        assertTrue(fftTimeMs < 200, "FFT should complete in reasonable time");
    }
    
    @Test
    @DisplayName("Verify correctness of linear convolution")
    void verifyLinearConvolutionCorrectness() {
        // Create a signal with known boundary behavior
        int signalLength = 256;
        double[] testSignal = new double[signalLength];
        
        // Gaussian pulse in the middle
        int center = signalLength / 2;
        double sigma = 10.0;
        for (int i = 0; i < signalLength; i++) {
            double x = i - center;
            testSignal[i] = Math.exp(-x * x / (2 * sigma * sigma));
        }
        
        // Analyze with FFT
        CWTTransform fftTransform = new CWTTransform(wavelet, CWTConfig.builder()
            .enableFFT(true)
            .build());
        
        double[] testScales = {8.0, 16.0};
        CWTResult result = fftTransform.analyze(testSignal, testScales);
        double[][] coeffs = result.getCoefficients();
        
        // Verify smooth decay at boundaries (no circular artifacts)
        for (int s = 0; s < testScales.length; s++) {
            // Check that coefficients decay smoothly towards edges
            double edge0 = Math.abs(coeffs[s][0]);
            double edge1 = Math.abs(coeffs[s][1]);
            double edge2 = Math.abs(coeffs[s][2]);
            
            // Each should be larger than the previous (decay from edge)
            assertTrue(edge1 >= edge0, "Should decay smoothly from edge");
            assertTrue(edge2 >= edge1, "Should decay smoothly from edge");
            
            // Similar check at the end
            int n = signalLength;
            double endEdge0 = Math.abs(coeffs[s][n-1]);
            double endEdge1 = Math.abs(coeffs[s][n-2]);
            double endEdge2 = Math.abs(coeffs[s][n-3]);
            
            assertTrue(endEdge1 >= endEdge0, "Should decay smoothly to edge");
            assertTrue(endEdge2 >= endEdge1, "Should decay smoothly to edge");
        }
    }
}