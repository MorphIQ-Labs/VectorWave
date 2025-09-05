package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.internal.ScalarOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify the modulo optimization produces correct results
 * and provides performance benefits.
 */
class MODWTModuloOptimizationTest {
    
    @Test
    void testOptimizedModuloProducesCorrectResults() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.7071067811865475, 0.7071067811865475}; // Haar
        
        double[] output1 = new double[signal.length];
        double[] output2 = new double[signal.length];
        
        // Use the optimized version
        ScalarOps.circularConvolveMODWT(signal, filter, output1);
        
        // Manually compute with the old formula to verify
        int signalLen = signal.length;
        int filterLen = filter.length;
        
        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;
            for (int l = 0; l < filterLen; l++) {
                // Old formula: always add signalLen
                int signalIndex = (t - l + signalLen) % signalLen;
                sum += signal[signalIndex] * filter[l];
            }
            output2[t] = sum;
        }
        
        // Results should be identical
        assertArrayEquals(output1, output2, 1e-14,
            "Optimized and original formulas should produce identical results");
    }
    
    @Test
    void testLevelBasedConvolutionOptimization() {
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(i * 0.1);
        }
        
        double[] filter = {0.7071067811865475, 0.7071067811865475};
        double[] output1 = new double[signal.length];
        double[] output2 = new double[signal.length];
        
        int level = 3; // Test with level 3
        
        // Use the optimized version
        ScalarOps.circularConvolveMODWTLevel(signal, filter, output1, level);
        
        // Manually compute with the old formula
        int signalLen = signal.length;
        int filterLen = filter.length;
        int shift = 1 << (level - 1);
        
        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;
            for (int l = 0; l < filterLen; l++) {
                // Old formula: always add large multiple of signalLen
                int signalIndex = (t - shift * l + signalLen * filterLen) % signalLen;
                sum += signal[signalIndex] * filter[l];
            }
            output2[t] = sum;
        }
        
        // Results should be identical
        assertArrayEquals(output1, output2, 1e-14,
            "Optimized and original level-based formulas should produce identical results");
    }
    
    @Test
    @EnabledIfSystemProperty(named = "runBenchmarks", matches = "true")
    void benchmarkModuloOptimization() {
        // Large signal for performance testing
        double[] signal = new double[8192];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.random();
        }
        
        double[] filter = new double[20]; // Daubechies D10
        for (int i = 0; i < filter.length; i++) {
            filter[i] = Math.random();
        }
        
        double[] output = new double[signal.length];
        
        // Warm up
        for (int i = 0; i < 100; i++) {
            ScalarOps.circularConvolveMODWT(signal, filter, output);
        }
        
        // Benchmark optimized version
        long start = System.nanoTime();
        int iterations = 1000;
        for (int i = 0; i < iterations; i++) {
            ScalarOps.circularConvolveMODWT(signal, filter, output);
        }
        long optimizedTime = System.nanoTime() - start;
        
        System.out.println("Optimized MODWT convolution time: " + 
            (optimizedTime / 1_000_000.0) + " ms for " + iterations + " iterations");
        System.out.println("Average per iteration: " + 
            (optimizedTime / (iterations * 1_000_000.0)) + " ms");
    }
}