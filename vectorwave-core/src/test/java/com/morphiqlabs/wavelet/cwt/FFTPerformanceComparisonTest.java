package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;

import static org.junit.jupiter.api.Assertions.*;

class FFTPerformanceComparisonTest {
    
    @Test
    @DisplayName("Should show FFT performance improvement")
    @Disabled("Manual performance test - enable to see improvement")
    void testFFTPerformanceImprovement() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        CWTConfig config = CWTConfig.builder()
            .enableFFT(true)
            .build();
        CWTTransform transform = new CWTTransform(wavelet, config);
        
        // Test with different signal sizes
        int[] sizes = {256, 512, 1024, 2048, 4096};
        double[] scales = {1.0, 2.0, 4.0, 8.0};
        
        System.out.println("\n=== FFT Performance Comparison ===");
        System.out.println("Signal Size | Time (ms) | Complexity");
        System.out.println("------------|-----------|------------");
        
        for (int size : sizes) {
            // Create test signal
            double[] signal = new double[size];
            for (int i = 0; i < size; i++) {
                signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                           0.5 * Math.sin(2 * Math.PI * i / 16.0);
            }
            
            // Time the transform
            long startTime = System.nanoTime();
            CWTResult result = transform.analyze(signal, scales);
            long endTime = System.nanoTime();
            
            double timeMs = (endTime - startTime) / 1_000_000.0;
            
            // With proper FFT, time should scale as O(n log n)
            // With DFT, it would scale as O(n²)
            double theoreticalFFT = size * Math.log(size) / Math.log(2);
            double theoreticalDFT = size * size;
            
            System.out.printf("%-11d | %9.2f | O(n log n)%n", size, timeMs);
            
            // Basic validation
            assertNotNull(result);
            assertEquals(scales.length, result.getCoefficients().length);
            assertEquals(size, result.getCoefficients()[0].length);
        }
        
        System.out.println("\nNote: With the old DFT implementation, times would scale as O(n²)");
        System.out.println("making large transforms impractically slow.\n");
    }
    
    @Test
    @DisplayName("Should produce same results as direct convolution")
    void testFFTAccuracy() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        CWTConfig configDirect = CWTConfig.builder()
            .enableFFT(false)
            .build();
        CWTConfig configFFT = CWTConfig.builder()
            .enableFFT(true)
            .build();
            
        CWTTransform transformDirect = new CWTTransform(wavelet, configDirect);
        CWTTransform transformFFT = new CWTTransform(wavelet, configFFT);
        
        // Small signal for accuracy comparison
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16.0);
        }
        double[] scales = {2.0, 4.0};
        
        // When
        CWTResult resultDirect = transformDirect.analyze(signal, scales);
        CWTResult resultFFT = transformFFT.analyze(signal, scales);
        
        // Then - results should be very close
        double[][] coeffDirect = resultDirect.getCoefficients();
        double[][] coeffFFT = resultFFT.getCoefficients();
        
        for (int s = 0; s < scales.length; s++) {
            for (int t = 0; t < signal.length; t++) {
                assertEquals(coeffDirect[s][t], coeffFFT[s][t], 1e-10,
                    String.format("Mismatch at scale %d, time %d", s, t));
            }
        }
    }
}