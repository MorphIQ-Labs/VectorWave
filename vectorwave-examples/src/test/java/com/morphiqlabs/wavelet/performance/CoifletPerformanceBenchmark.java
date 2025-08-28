package com.morphiqlabs.wavelet.performance;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Coiflet;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;

import java.util.Random;

/**
 * Performance benchmarks for extended Coiflet wavelets.
 * Compares standard vs FFT-optimized implementations.
 */
class CoifletPerformanceBenchmark {
    
    private static final int SIGNAL_LENGTH = 8192;
    private static final int WARMUP_ITERATIONS = 10;
    private static final int BENCHMARK_ITERATIONS = 100;
    
    @Test
    @DisplayName("Benchmark COIF8 (48 taps)")
    void benchmarkCOIF8() {
        Coiflet coif8 = Coiflet.COIF8;
        double[] signal = generateSignal(SIGNAL_LENGTH);
        
        System.out.println("\n=== COIF8 Performance (48 taps) ===");
        System.out.println("Signal length: " + SIGNAL_LENGTH);
        System.out.println("Filter length: " + coif8.lowPassDecomposition().length);
        
        // Single implementation with internal optimizations
        MODWTTransform transform = new MODWTTransform(coif8, BoundaryMode.PERIODIC);
        double avgTime = benchmarkTransform(transform, signal, "MODWT with automatic optimization");
        
        System.out.printf("Average time: %.2f ms\n", avgTime);
    }
    
    @Test
    @DisplayName("Benchmark COIF10 (60 taps)")
    void benchmarkCOIF10() {
        Coiflet coif10 = Coiflet.COIF10;
        double[] signal = generateSignal(SIGNAL_LENGTH);
        
        System.out.println("\n=== COIF10 Performance (60 taps) ===");
        System.out.println("Signal length: " + SIGNAL_LENGTH);
        System.out.println("Filter length: " + coif10.lowPassDecomposition().length);
        
        // Single implementation with internal optimizations
        MODWTTransform transform = new MODWTTransform(coif10, BoundaryMode.PERIODIC);
        double avgTime = benchmarkTransform(transform, signal, "MODWT with automatic optimization");
        
        System.out.printf("Average time: %.2f ms\n", avgTime);
    }
    
    @Test
    @DisplayName("Compare all Coiflet orders")
    void compareAllCoiflets() {
        double[] signal = generateSignal(SIGNAL_LENGTH);
        
        System.out.println("\n=== Coiflet Performance Comparison ===");
        System.out.println("Signal length: " + SIGNAL_LENGTH);
        System.out.printf("%-8s %-12s %-15s\n", 
            "Order", "Filter Len", "Time (ms)");
        System.out.println("-".repeat(35));
        
        for (int order = 1; order <= 10; order++) {
            Coiflet coif = Coiflet.get(order);
            int filterLen = 6 * order;
            
            // Single transform with automatic optimization
            MODWTTransform transform = new MODWTTransform(coif, BoundaryMode.PERIODIC);
            double time = quickBenchmark(transform, signal);
            
            System.out.printf("COIF%-4d %-12d %-15.2f\n",
                order, filterLen, time);
        }
    }
    
    @Test
    @DisplayName("Batch processing performance")
    void benchmarkBatchProcessing() {
        System.out.println("\n=== Batch Processing Performance ===");
        
        int batchSize = 32;
        int signalLength = 1024;
        double[][] batch = new double[batchSize][signalLength];
        Random random = new Random(42);
        
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < signalLength; j++) {
                batch[i][j] = random.nextGaussian();
            }
        }
        
        System.out.printf("Batch size: %d, Signal length: %d\n", batchSize, signalLength);
        System.out.printf("%-8s %-15s %-20s\n", "Wavelet", "Time (ms)", "Optimization");
        System.out.println("-".repeat(45));
        
        // Test with different Coiflets
        for (int order : new int[]{3, 6, 8, 10}) {
            Coiflet coif = Coiflet.get(order);
            
            // Single implementation with automatic optimization
            MODWTTransform transform = new MODWTTransform(coif, BoundaryMode.PERIODIC);
            long startTime = System.nanoTime();
            for (int iter = 0; iter < 10; iter++) {
                transform.forwardBatch(batch);
            }
            double avgTime = (System.nanoTime() - startTime) / 1e6 / 10;
            
            System.out.printf("COIF%-4d %-15.2f\n",
                order, avgTime);
        }
    }
    
    @Test
    @DisplayName("Memory efficiency comparison")
    void testMemoryEfficiency() {
        System.out.println("\n=== Memory Efficiency Analysis ===");
        
        // Test with very long signal
        int longSignalLength = 100000;
        double[] signal = generateSignal(longSignalLength);
        
        System.out.println("Signal length: " + longSignalLength);
        
        // Measure memory usage
        Runtime runtime = Runtime.getRuntime();
        
        // COIF10 with automatic optimization
        runtime.gc();
        long memBefore = runtime.totalMemory() - runtime.freeMemory();
        
        Coiflet coif10 = Coiflet.COIF10;
        MODWTTransform transform = new MODWTTransform(coif10, BoundaryMode.PERIODIC);
        MODWTResult result = transform.forward(signal);
        
        long memAfter = runtime.totalMemory() - runtime.freeMemory();
        long memUsage = (memAfter - memBefore) / 1024 / 1024; // MB
        
        System.out.printf("Memory usage: ~%d MB\n", memUsage);
        
        // Test inverse
        double[] reconstructed = transform.inverse(result);
        double maxError = 0.0;
        for (int i = 0; i < signal.length; i++) {
            maxError = Math.max(maxError, Math.abs(signal[i] - reconstructed[i]));
        }
        System.out.printf("Reconstruction error: %.2e\n", maxError);
    }
    
    @Test
    @DisplayName("Scaling with signal length")
    @Disabled("Long running test - enable for detailed benchmarking")
    void testScalingBehavior() {
        System.out.println("\n=== Scaling Behavior Analysis ===");
        
        Coiflet coif8 = Coiflet.COIF8;
        int[] signalLengths = {1024, 2048, 4096, 8192, 16384, 32768};
        
        System.out.printf("%-12s %-15s\n", "Signal Len", "Time (ms)");
        System.out.println("-".repeat(30));
        
        for (int length : signalLengths) {
            double[] signal = generateSignal(length);
            
            MODWTTransform transform = new MODWTTransform(coif8, BoundaryMode.PERIODIC);
            double time = quickBenchmark(transform, signal);
            
            System.out.printf("%-12d %-15.2f\n", length, time);
        }
    }
    
    // Helper methods
    
    private double benchmarkTransform(MODWTTransform transform, double[] signal, String name) {
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            MODWTResult result = transform.forward(signal);
            transform.inverse(result);
        }
        
        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            MODWTResult result = transform.forward(signal);
            transform.inverse(result);
        }
        long endTime = System.nanoTime();
        
        double avgTime = (endTime - startTime) / 1e6 / BENCHMARK_ITERATIONS;
        System.out.printf("%s: %.2f ms per transform\n", name, avgTime);
        
        return avgTime;
    }
    
    private double quickBenchmark(MODWTTransform transform, double[] signal) {
        // Quick benchmark with fewer iterations
        for (int i = 0; i < 5; i++) {
            transform.forward(signal);
        }
        
        long startTime = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            transform.forward(signal);
        }
        long endTime = System.nanoTime();
        
        return (endTime - startTime) / 1e6 / 20;
    }
    
    private double[] generateSignal(int length) {
        double[] signal = new double[length];
        Random random = new Random(42);
        
        // Generate a signal with multiple frequency components
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i * 5 / length) +
                       0.5 * Math.cos(2 * Math.PI * i * 20 / length) +
                       0.1 * random.nextGaussian();
        }
        
        return signal;
    }
}