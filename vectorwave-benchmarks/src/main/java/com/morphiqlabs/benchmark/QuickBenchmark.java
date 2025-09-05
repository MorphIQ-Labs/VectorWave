package com.morphiqlabs.benchmark;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MODWTResult;

import java.util.Random;

/**
 * Quick benchmark for gathering performance data without JMH.
 * Provides rapid performance estimates for documentation.
 */
public class QuickBenchmark {
    
    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASUREMENT_ITERATIONS = 1000;
    
    public static void main(String[] args) {
        System.out.println("==========================================");
        System.out.println("    VectorWave Quick Performance Test    ");
        System.out.println("==========================================");
        System.out.println();
        
        // Test different signal sizes
        int[] signalSizes = {1024, 4096, 16384, 65536};
        
        // Test different wavelets
        WaveletName[] wavelets = {
            WaveletName.HAAR,
            WaveletName.DB4, 
            WaveletName.DB8,
            WaveletName.SYM8
        };
        
        System.out.println("Configuration:");
        System.out.println("  Java Version: " + System.getProperty("java.version"));
        System.out.println("  Available Processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("  Max Memory: " + formatBytes(Runtime.getRuntime().maxMemory()));
        System.out.println();
        
        // Check if Vector API is available
        boolean vectorApiAvailable = checkVectorAPI();
        System.out.println("  Vector API: " + (vectorApiAvailable ? "ENABLED" : "DISABLED"));
        System.out.println();
        
        System.out.println("Performance Results:");
        System.out.println("--------------------------------------------");
        System.out.printf("%-10s | %-12s | %-15s | %-15s%n", 
            "Wavelet", "Signal Size", "Forward (µs)", "Round Trip (µs)");
        System.out.println("--------------------------------------------");
        
        for (WaveletName waveletName : wavelets) {
            Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
            MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            
            for (int size : signalSizes) {
                // Generate test signal
                double[] signal = generateSignal(size);
                
                // Warmup
                for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                    MODWTResult result = transform.forward(signal);
                    transform.inverse(result);
                }
                
                // Measure forward transform
                long forwardTime = 0;
                for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                    long start = System.nanoTime();
                    transform.forward(signal);
                    forwardTime += System.nanoTime() - start;
                }
                double avgForward = (forwardTime / 1000.0) / MEASUREMENT_ITERATIONS;
                
                // Measure round trip
                long roundTripTime = 0;
                for (int i = 0; i < MEASUREMENT_ITERATIONS; i++) {
                    long start = System.nanoTime();
                    MODWTResult result = transform.forward(signal);
                    transform.inverse(result);
                    roundTripTime += System.nanoTime() - start;
                }
                double avgRoundTrip = (roundTripTime / 1000.0) / MEASUREMENT_ITERATIONS;
                
                System.out.printf("%-10s | %12d | %15.2f | %15.2f%n",
                    waveletName, size, avgForward, avgRoundTrip);
            }
        }
        
        System.out.println("--------------------------------------------");
        System.out.println();
        
        // Batch processing test
        System.out.println("Batch Processing Performance:");
        System.out.println("--------------------------------------------");
        testBatchProcessing();
        
        System.out.println();
        System.out.println("Memory Efficiency Test:");
        System.out.println("--------------------------------------------");
        testMemoryEfficiency();
        
        System.out.println();
        System.out.println("==========================================");
        System.out.println("         Benchmark Complete               ");
        System.out.println("==========================================");
    }
    
    private static void testBatchProcessing() {
        Wavelet wavelet = WaveletRegistry.getWavelet(WaveletName.DB4);
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        int batchSize = 16;
        int signalSize = 4096;
        
        // Generate batch
        double[][] batch = new double[batchSize][signalSize];
        Random random = new Random(42);
        for (int i = 0; i < batchSize; i++) {
            for (int j = 0; j < signalSize; j++) {
                batch[i][j] = random.nextGaussian();
            }
        }
        
        // Warmup
        for (int i = 0; i < 10; i++) {
            transform.forwardBatch(batch);
        }
        
        // Individual processing
        long individualTime = 0;
        for (int iter = 0; iter < 100; iter++) {
            long start = System.nanoTime();
            for (int i = 0; i < batchSize; i++) {
                transform.forward(batch[i]);
            }
            individualTime += System.nanoTime() - start;
        }
        
        // Batch processing
        long batchTime = 0;
        for (int iter = 0; iter < 100; iter++) {
            long start = System.nanoTime();
            transform.forwardBatch(batch);
            batchTime += System.nanoTime() - start;
        }
        
        double individualAvg = (individualTime / 1000000.0) / 100;
        double batchAvg = (batchTime / 1000000.0) / 100;
        double speedup = individualAvg / batchAvg;
        
        System.out.printf("Individual Processing: %.2f ms%n", individualAvg);
        System.out.printf("Batch Processing:      %.2f ms%n", batchAvg);
        System.out.printf("Speedup:               %.2fx%n", speedup);
    }
    
    private static void testMemoryEfficiency() {
        Runtime runtime = Runtime.getRuntime();
        
        // Force GC before test
        System.gc();
        System.gc();
        
        long beforeMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Allocate and process large signal
        int signalSize = 131072; // 128K samples
        double[] signal = generateSignal(signalSize);
        
        Wavelet wavelet = WaveletRegistry.getWavelet(WaveletName.DB4);
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        MODWTResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        long afterMemory = runtime.totalMemory() - runtime.freeMemory();
        long usedMemory = afterMemory - beforeMemory;
        
        System.out.printf("Signal Size:        %d samples%n", signalSize);
        System.out.printf("Memory Used:        %s%n", formatBytes(usedMemory));
        System.out.printf("Bytes per Sample:   %.2f%n", (double)usedMemory / signalSize);
        
        // Verify reconstruction
        double error = 0;
        for (int i = 0; i < signalSize; i++) {
            error += Math.abs(signal[i] - reconstructed[i]);
        }
        System.out.printf("Reconstruction Error: %.2e%n", error / signalSize);
    }
    
    private static double[] generateSignal(int size) {
        Random random = new Random(42);
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            double t = 2.0 * Math.PI * i / size;
            signal[i] = Math.sin(t) + 0.5 * Math.sin(4 * t) + 0.1 * random.nextGaussian();
        }
        return signal;
    }
    
    private static boolean checkVectorAPI() {
        try {
            Class.forName("jdk.incubator.vector.VectorSpecies");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    private static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}

