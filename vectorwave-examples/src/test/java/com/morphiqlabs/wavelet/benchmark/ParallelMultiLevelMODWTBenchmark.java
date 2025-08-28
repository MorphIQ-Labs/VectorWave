package com.morphiqlabs.wavelet.benchmark;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.modwt.ParallelMultiLevelMODWT;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;

/**
 * Benchmark for parallel multi-level MODWT implementation.
 */
public class ParallelMultiLevelMODWTBenchmark {
    
    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASURE_ITERATIONS = 1000;
    
    /**
     * Safety margin for maximum decomposition levels.
     * For MODWT, the theoretical maximum is approximately log2(N/(L-1)) where N is signal length
     * and L is filter length. We subtract 2 as a safety margin to ensure:
     * 1. We don't approach the theoretical limit where numerical issues may arise
     * 2. We leave room for filters longer than Haar (L=2)
     * 3. We avoid edge cases where the scaled filter might exceed signal boundaries
     */
    private static final int MAX_LEVEL_SAFETY_MARGIN = 2;
    
    public static void main(String[] args) {
        System.out.println("Parallel Multi-Level MODWT Benchmark");
        System.out.println("====================================");
        System.out.println("Cores available: " + Runtime.getRuntime().availableProcessors());
        System.out.println();
        
        // Test different signal sizes and decomposition levels
        int[] signalSizes = {256, 512, 1024, 2048, 4096, 8192};
        int[] levelCounts = {2, 3, 4, 5, 6};
        
        for (int signalSize : signalSizes) {
            System.out.println("\nSignal Size: " + signalSize);
            System.out.println("--------------------------------");
            
            // Generate test signal
            double[] signal = generateRandomSignal(signalSize);
            
            for (int levels : levelCounts) {
                // Skip if too many levels for signal size
                int maxSafeLevels = (int)(Math.log(signalSize) / Math.log(2)) - MAX_LEVEL_SAFETY_MARGIN;
                if (levels > maxSafeLevels) {
                    continue;
                }
                
                benchmarkDecomposition(signal, levels);
            }
        }
        
        // Test with different thread pools
        System.out.println("\n\nThread Pool Size Comparison (Signal: 4096, Levels: 5)");
        System.out.println("====================================================");
        double[] largeSignal = generateRandomSignal(4096);
        
        for (int threads = 1; threads <= Runtime.getRuntime().availableProcessors(); threads *= 2) {
            benchmarkWithThreadPool(largeSignal, 5, threads);
        }
    }
    
    private static void benchmarkDecomposition(double[] signal, int levels) {
        var wavelet = Daubechies.DB4;
        var mode = BoundaryMode.PERIODIC;
        
        // Create transforms
        MultiLevelMODWTTransform sequential = new MultiLevelMODWTTransform(wavelet, mode);
        ParallelMultiLevelMODWT parallel = new ParallelMultiLevelMODWT();
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            sequential.decompose(signal, levels);
            parallel.decompose(signal, wavelet, mode, levels);
        }
        
        // Measure sequential
        long seqStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            sequential.decompose(signal, levels);
        }
        long seqTime = System.nanoTime() - seqStart;
        
        // Measure parallel
        long parStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            parallel.decompose(signal, wavelet, mode, levels);
        }
        long parTime = System.nanoTime() - parStart;
        
        // Calculate results
        double seqMs = seqTime / 1e6 / MEASURE_ITERATIONS;
        double parMs = parTime / 1e6 / MEASURE_ITERATIONS;
        double speedup = seqMs / parMs;
        
        System.out.printf("  Levels: %d | Sequential: %.3f ms | Parallel: %.3f ms | Speedup: %.2fx%n",
            levels, seqMs, parMs, speedup);
    }
    
    private static void benchmarkWithThreadPool(double[] signal, int levels, int threads) {
        var wavelet = Daubechies.DB4;
        var mode = BoundaryMode.PERIODIC;
        
        ForkJoinPool pool = new ForkJoinPool(threads);
        try {
            ParallelMultiLevelMODWT parallel = new ParallelMultiLevelMODWT(pool);
            
            // Warmup
            for (int i = 0; i < WARMUP_ITERATIONS; i++) {
                parallel.decompose(signal, wavelet, mode, levels);
            }
            
            // Measure
            long start = System.nanoTime();
            for (int i = 0; i < MEASURE_ITERATIONS; i++) {
                parallel.decompose(signal, wavelet, mode, levels);
            }
            long time = System.nanoTime() - start;
            
            double ms = time / 1e6 / MEASURE_ITERATIONS;
            System.out.printf("  Threads: %2d | Time: %.3f ms%n", threads, ms);
            
        } finally {
            pool.shutdown();
        }
    }
    
    private static double[] generateRandomSignal(int length) {
        Random random = new Random(42); // Fixed seed for reproducibility
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = random.nextGaussian();
        }
        return signal;
    }
}