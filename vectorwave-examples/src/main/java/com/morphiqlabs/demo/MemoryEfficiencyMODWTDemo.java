package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.*;
import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.modwt.*;
import com.morphiqlabs.wavelet.memory.*;
import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingDenoiser;

import java.util.Random;

/**
 * Demonstration of memory-efficient MODWT operations.
 * Shows how MODWT can be used with memory pooling and efficient patterns.
 */
public class MemoryEfficiencyMODWTDemo {
    
    public static void main(String[] args) {
        System.out.println("VectorWave Memory-Efficient MODWT Demo");
        System.out.println("======================================\n");
        
        System.out.println("This demo shows memory-efficient MODWT patterns.\n");
        
        // Demo 1: Basic MODWT with memory pooling
        basicMODWTWithMemoryPool();
        
        // Demo 2: Batch processing for memory efficiency
        batchProcessingDemo();
        
        // Demo 3: Streaming MODWT for large datasets
        streamingMODWTDemo();
        
        // Demo 4: Performance comparison
        performanceComparison();
        
        // Demo 5: Memory usage analysis
        memoryUsageAnalysis();
    }
    
    private static void basicMODWTWithMemoryPool() {
        System.out.println("1. Basic MODWT with Memory Pooling");
        System.out.println("----------------------------------");
        
        double[] signal = generateSignal(1024);
        Wavelet wavelet = new Haar();
        
        // Create MODWT transform
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        // Use memory pooling for efficient memory management
        // Note: AlignedMemoryPool has internal usage - for demo we'll show the concept
        try {
            // Forward transform
            MODWTResult result = transform.forward(signal);
            System.out.println("Forward transform completed:");
            System.out.println("  Signal length: " + signal.length);
            System.out.println("  Approximation coeffs: " + result.approximationCoeffs().length);
            System.out.println("  Detail coeffs: " + result.detailCoeffs().length);
            System.out.println("  Same length preserved: " + 
                (signal.length == result.approximationCoeffs().length));
            
            // Inverse transform
            double[] reconstructed = transform.inverse(result);
            System.out.println("Inverse transform completed");
            
            // Verify reconstruction
            double error = calculateRMSE(signal, reconstructed);
            System.out.printf("Reconstruction RMSE: %.2e\n", error);
            
            // Memory pooling is handled internally by MODWT for efficiency
            System.out.println("\nMemory efficiency:");
            System.out.println("  MODWT uses internal memory pooling for performance");
            System.out.println("  No manual pool management required");
            
        } finally {
            // Cleanup handled automatically
        }
        
        System.out.println();
    }
    
    private static void batchProcessingDemo() {
        System.out.println("2. Batch Processing for Memory Efficiency");
        System.out.println("-----------------------------------------");
        
        // Create batch of signals with various lengths (MODWT handles any length)
        int[] signalLengths = {100, 256, 333, 512, 777, 1024};
        double[][] signals = new double[signalLengths.length][];
        
        for (int i = 0; i < signalLengths.length; i++) {
            signals[i] = generateSignal(signalLengths[i]);
            System.out.printf("  Signal %d: %d samples (not power of 2!)\n", i, signalLengths[i]);
        }
        
        // Process batch using MODWT
        MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        long startTime = System.nanoTime();
        
        MODWTResult[] results = new MODWTResult[signals.length];
        for (int i = 0; i < signals.length; i++) {
            results[i] = transform.forward(signals[i]);
        }
        
        long batchTime = System.nanoTime() - startTime;
        System.out.printf("\nBatch processing completed in %.2f ms\n", batchTime / 1_000_000.0);
        
        // Verify all signals preserved their length
        System.out.println("\nLength preservation check:");
        for (int i = 0; i < signals.length; i++) {
            System.out.printf("  Signal %d: input=%d, output=%d (preserved: %s)\n",
                i, signals[i].length, results[i].getSignalLength(),
                signals[i].length == results[i].getSignalLength() ? "YES" : "NO");
        }
        
        System.out.println();
    }
    
    private static void streamingMODWTDemo() {
        System.out.println("3. Streaming MODWT for Large Datasets");
        System.out.println("-------------------------------------");
        
        // Use MODWTStreamingDenoiser for streaming applications
        try (MODWTStreamingDenoiser denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(new Haar())
            .boundaryMode(BoundaryMode.PERIODIC)
            .bufferSize(256)
            .build()) {
        
        // Subscribe to denoised results
        denoiser.subscribe(new java.util.concurrent.Flow.Subscriber<double[]>() {
            private int blockCount = 0;
            
            @Override
            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(double[] denoisedBlock) {
                blockCount++;
                if (blockCount == 1) {
                    System.out.println("\nFirst denoised block received:");
                    System.out.println("  Block size: " + denoisedBlock.length);
                    System.out.println("  Energy: " + calculateEnergy(denoisedBlock));
                }
            }
            
            @Override
            public void onError(Throwable throwable) {
                System.err.println("Error: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {
                System.out.println("\nStreaming completed. Total blocks: " + blockCount);
            }
        });
        
        // Simulate streaming data
        System.out.println("Simulating streaming data...");
        Random random = new Random(42);
        int totalSamples = 0;
        
        for (int i = 0; i < 10; i++) {
            // Generate varying size chunks (MODWT handles any size)
            int chunkSize = 50 + random.nextInt(200);
            double[] chunk = generateSignal(chunkSize);
            double[] denoised = denoiser.denoise(chunk);
            totalSamples += chunkSize;
        }
        
        System.out.printf("\nProcessed %d total samples\n", totalSamples);
        System.out.printf("  Noise level estimated: %.4f\n", denoiser.getEstimatedNoiseLevel());
        System.out.println();
        }
    }
    
    private static void performanceComparison() {
        System.out.println("4. Performance Comparison");
        System.out.println("-------------------------");
        
        int[] sizes = {100, 256, 777, 1024, 3333}; // MODWT handles any size!
        Wavelet wavelet = Daubechies.DB4;
        int iterations = 100;
        
        System.out.println("MODWT Performance Analysis:");
        System.out.println("Wavelet: " + wavelet.name());
        System.out.println("Iterations: " + iterations);
        System.out.println();
        
        System.out.println("Size  | Time (ms) | Time/Sample (ns) | Shift-Invariant");
        System.out.println("------|-----------|------------------|----------------");
        
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        for (int size : sizes) {
            double[] signal = generateSignal(size);
            
            // Time MODWT
            long startTime = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                MODWTResult result = transform.forward(signal);
                transform.inverse(result);
            }
            long totalTime = System.nanoTime() - startTime;
            
            double timeMs = totalTime / 1_000_000.0;
            double timePerSample = (totalTime / iterations) / (double)size;
            
            System.out.printf("%5d | %9.2f | %16.2f | YES\n",
                size, timeMs, timePerSample);
        }
        
        System.out.println("\nMODWT Advantages:");
        System.out.println("  - Works with ANY signal length (not just powers of 2)");
        System.out.println("  - Shift-invariant (translation-invariant)");
        System.out.println("  - No downsampling = same length output");
        System.out.println("  - Perfect for time series analysis");
        
        // Show performance info
        var perfInfo = transform.getPerformanceInfo();
        System.out.println("\n" + perfInfo.description());
        
        System.out.println();
    }
    
    private static void memoryUsageAnalysis() {
        System.out.println("5. Memory Usage Analysis");
        System.out.println("------------------------");
        
        // Compare memory usage for different signal sizes
        int[] sizes = {100, 1000, 10000};
        
        System.out.println("Signal Size | MODWT Memory | DWT Memory (if power of 2) | Savings");
        System.out.println("------------|--------------|----------------------------|--------");
        
        for (int size : sizes) {
            // MODWT memory: input + 2 * output (same size)
            long modwtMemory = size * 8 * 3; // 3 arrays of doubles
            
            // DWT would need next power of 2
            int dwtSize = nextPowerOfTwo(size);
            long dwtMemory = dwtSize * 8 * 3;
            
            double savings = (dwtSize > size) ? 
                ((double)(dwtMemory - modwtMemory) / dwtMemory * 100) : 0;
            
            System.out.printf("%11d | %12d | %26d | %6.1f%%\n",
                size, modwtMemory, dwtMemory, savings);
        }
        
        System.out.println("\nMODWT Memory Advantages:");
        System.out.println("  - No padding to power-of-2 required");
        System.out.println("  - Direct processing of arbitrary-length signals");
        System.out.println("  - Predictable memory usage");
        System.out.println("  - Better cache utilization for non-power-of-2 signals");
        
        // Memory pooling in MODWT
        System.out.println("\nMemory Pool Usage in MODWT:");
        System.out.println("  MODWT internally uses AlignedMemoryPool for:");
        System.out.println("    - SIMD-aligned memory allocation");
        System.out.println("    - Efficient memory reuse");
        System.out.println("    - Automatic cleanup");
        System.out.println("  Benefits:");
        System.out.println("    - Reduced allocation overhead");
        System.out.println("    - Better cache utilization");
        System.out.println("    - Improved SIMD performance");
        
        System.out.println();
    }
    
    // Helper methods
    
    private static double[] generateSignal(int size) {
        Random random = new Random(42);
        double[] signal = new double[size];
        
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64) + 
                       0.5 * Math.sin(4 * Math.PI * i / 64) +
                       0.1 * random.nextGaussian();
        }
        
        return signal;
    }
    
    private static double calculateRMSE(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum / a.length);
    }
    
    private static double calculateEnergy(double[] signal) {
        double energy = 0;
        for (double v : signal) {
            energy += v * v;
        }
        return energy;
    }
    
    private static int nextPowerOfTwo(int n) {
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        n++;
        return n;
    }
}
