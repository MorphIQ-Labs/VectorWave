package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.padding.AdaptivePaddingStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Method;

/**
 * Test suite for FFT-based periodicity detection in AdaptivePaddingStrategy.
 * Validates both correctness and performance improvements.
 */
@DisplayName("FFT-based Periodicity Detection Tests")
public class FFTPeriodicityTest {
    
    private static final double TOLERANCE = 0.1;
    
    /**
     * Access private calculatePeriodicity method for testing.
     */
    private double calculatePeriodicity(AdaptivePaddingStrategy strategy, double[] signal) throws Exception {
        Method method = AdaptivePaddingStrategy.class.getDeclaredMethod("calculatePeriodicity", double[].class);
        method.setAccessible(true);
        return (double) method.invoke(strategy, signal);
    }
    
    @Test
    @DisplayName("Detect simple sine wave periodicity")
    void testSineWavePeriodicity() throws Exception {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        // Create a sine wave with period 10
        int period = 10;
        int length = 100;
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / period);
        }
        
        double periodicity = calculatePeriodicity(strategy, signal);
        
        // Should detect high periodicity
        assertTrue(periodicity > 0.8, "Expected high periodicity for sine wave, got: " + periodicity);
    }
    
    @Test
    @DisplayName("Detect complex periodic signal")
    void testComplexPeriodicSignal() throws Exception {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        // Create a signal with multiple frequency components
        int length = 256;
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 8) +     // Period 8
                       0.5 * Math.sin(2 * Math.PI * i / 16) + // Period 16
                       0.25 * Math.cos(2 * Math.PI * i / 32); // Period 32
        }
        
        double periodicity = calculatePeriodicity(strategy, signal);
        
        // Should detect periodicity despite multiple components
        assertTrue(periodicity > 0.7, "Expected periodicity for multi-frequency signal, got: " + periodicity);
    }
    
    @Test
    @DisplayName("No periodicity for random signal")
    void testRandomSignal() throws Exception {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        // Create random signal
        int length = 200;
        double[] signal = new double[length];
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < length; i++) {
            signal[i] = rand.nextGaussian();
        }
        
        double periodicity = calculatePeriodicity(strategy, signal);
        
        // Should detect low periodicity for random signal
        assertTrue(periodicity < 0.3, "Expected low periodicity for random signal, got: " + periodicity);
    }
    
    @Test
    @DisplayName("Detect periodicity in noisy signal")
    void testNoisyPeriodicSignal() throws Exception {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        // Create noisy periodic signal
        int period = 12;
        int length = 144;
        double[] signal = new double[length];
        java.util.Random rand = new java.util.Random(42);
        
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / period) + 0.2 * rand.nextGaussian();
        }
        
        double periodicity = calculatePeriodicity(strategy, signal);
        
        // Should still detect periodicity despite noise
        assertTrue(periodicity > 0.5, "Expected periodicity detection in noisy signal, got: " + periodicity);
    }
    
    @Test
    @DisplayName("Handle short signals gracefully")
    void testShortSignal() throws Exception {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        // Very short signal (< 32 samples, uses direct method)
        double[] shortSignal = {1, 2, 3, 2, 1, 2, 3, 2, 1, 2, 3, 2};
        double periodicity = calculatePeriodicity(strategy, shortSignal);
        
        // Should detect the period-4 pattern
        assertTrue(periodicity > 0.4, "Expected periodicity in short signal, got: " + periodicity);
    }
    
    @Test
    @DisplayName("Handle constant signal")
    void testConstantSignal() throws Exception {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        double[] constant = new double[100];
        java.util.Arrays.fill(constant, 5.0);
        
        double periodicity = calculatePeriodicity(strategy, constant);
        
        // Constant signal has no meaningful periodicity
        assertEquals(0.0, periodicity, TOLERANCE, "Constant signal should have zero periodicity");
    }
    
    @Test
    @DisplayName("Detect square wave periodicity")
    void testSquareWave() throws Exception {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        // Create square wave with period 20
        int period = 20;
        int length = 200;
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = (i % period) < (period / 2) ? 1.0 : -1.0;
        }
        
        double periodicity = calculatePeriodicity(strategy, signal);
        
        // Should detect high periodicity for square wave
        assertTrue(periodicity > 0.8, "Expected high periodicity for square wave, got: " + periodicity);
    }
    
    @Test
    @DisplayName("Performance test: FFT vs direct method for large signals")
    void testPerformanceImprovement() throws Exception {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        // Create a large periodic signal
        int length = 4096;
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 50) + 
                       Math.cos(2 * Math.PI * i / 73);
        }
        
        // Time the FFT-based method
        long startTime = System.nanoTime();
        double periodicity = calculatePeriodicity(strategy, signal);
        long fftTime = System.nanoTime() - startTime;
        
        // Result should be valid
        assertTrue(periodicity > 0.5, "Expected periodicity detection for large signal");
        
        // Log the performance (informational)
        System.out.println("FFT-based periodicity detection for " + length + 
                          " samples took " + (fftTime / 1_000_000.0) + " ms");
        
        // For a 4096-sample signal, FFT should complete in reasonable time
        assertTrue(fftTime < 100_000_000, "FFT method took too long: " + (fftTime / 1_000_000.0) + " ms");
    }
    
    @Test
    @DisplayName("Detect periodicity with trend")
    void testPeriodicityWithTrend() throws Exception {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        // Create periodic signal with linear trend
        int period = 15;
        int length = 150;
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / period) + 0.01 * i; // Add linear trend
        }
        
        double periodicity = calculatePeriodicity(strategy, signal);
        
        // Should still detect periodicity despite trend (detrending should handle it)
        assertTrue(periodicity > 0.6, "Expected periodicity detection with trend, got: " + periodicity);
    }
    
    @Test
    @DisplayName("Test edge case: very small signal")
    void testVerySmallSignal() throws Exception {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        // Signal too short for periodicity detection
        double[] tinySignal = {1, 2, 3, 4, 5};
        double periodicity = calculatePeriodicity(strategy, tinySignal);
        
        assertEquals(0.0, periodicity, TOLERANCE, "Very small signal should return zero periodicity");
    }
}