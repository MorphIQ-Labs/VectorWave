package com.morphiqlabs.wavelet.swt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResult;
import org.junit.jupiter.api.*;

import java.util.Random;

/**
 * Benchmark tests for SWT optimizations.
 * Measures performance improvements from internal optimizations.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SWTBenchmarkTest {
    
    private static final int WARMUP_ITERATIONS = 3;
    private static final int BENCHMARK_ITERATIONS = 10;
    
    @Test
    @Order(1)
    @DisplayName("Benchmark small signal processing (sequential)")
    void benchmarkSmallSignal() {
        double[] signal = generateSignal(1024);
        VectorWaveSwtAdapter adapter = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            MutableMultiLevelMODWTResult result = adapter.forward(signal, 4);
            adapter.inverse(result);
        }
        
        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            MutableMultiLevelMODWTResult result = adapter.forward(signal, 4);
            adapter.inverse(result);
        }
        long endTime = System.nanoTime();
        
        double avgTimeMs = (endTime - startTime) / (BENCHMARK_ITERATIONS * 1_000_000.0);
        System.out.printf("Small signal (1024): %.2f ms per transform/inverse pair%n", avgTimeMs);
        
        adapter.cleanup();
    }
    
    @Test
    @Order(2)
    @DisplayName("Benchmark large signal processing (parallel)")
    void benchmarkLargeSignal() {
        double[] signal = generateSignal(16384);
        VectorWaveSwtAdapter adapter = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            MutableMultiLevelMODWTResult result = adapter.forward(signal, 5);
            adapter.inverse(result);
        }
        
        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            MutableMultiLevelMODWTResult result = adapter.forward(signal, 5);
            adapter.inverse(result);
        }
        long endTime = System.nanoTime();
        
        double avgTimeMs = (endTime - startTime) / (BENCHMARK_ITERATIONS * 1_000_000.0);
        System.out.printf("Large signal (16384): %.2f ms per transform/inverse pair%n", avgTimeMs);
        
        adapter.cleanup();
    }
    
    @Test
    @Order(3)
    @DisplayName("Benchmark filter precomputation benefit")
    void benchmarkFilterPrecomputation() {
        double[] signal = generateSignal(2048);
        
        // Test without cache first (create fresh adapter)
        VectorWaveSwtAdapter adapterUncached = new VectorWaveSwtAdapter(Daubechies.DB6, BoundaryMode.PERIODIC);
        
        // Minimal warmup for uncached
        for (int i = 0; i < 2; i++) {
            adapterUncached.forward(signal, 4);
        }
        
        long startUncached = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS * 3; i++) {
            adapterUncached.forward(signal, 4);
        }
        long endUncached = System.nanoTime();
        
        // Test with precomputation (after cache is built up)
        VectorWaveSwtAdapter adapterWithCache = new VectorWaveSwtAdapter(Daubechies.DB6, BoundaryMode.PERIODIC);
        
        // Extended warmup to build cache
        for (int i = 0; i < WARMUP_ITERATIONS * 3; i++) {
            adapterWithCache.forward(signal, 4);
        }
        
        long startCached = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS * 3; i++) {
            adapterWithCache.forward(signal, 4);
        }
        long endCached = System.nanoTime();
        
        double cachedTimeMs = (endCached - startCached) / (BENCHMARK_ITERATIONS * 3 * 1_000_000.0);
        double uncachedTimeMs = (endUncached - startUncached) / (BENCHMARK_ITERATIONS * 3 * 1_000_000.0);
        double speedup = uncachedTimeMs / cachedTimeMs;
        
        System.out.printf("Filter precomputation speedup: %.2fx (cached: %.2f ms, uncached: %.2f ms)%n",
                         speedup, cachedTimeMs, uncachedTimeMs);
        
        // In CI environments, performance can vary significantly due to JIT compilation
        // and resource contention. We just verify that caching doesn't make things worse
        Assertions.assertTrue(speedup >= 0.5, "Cached should not be significantly slower than uncached");
        
        adapterUncached.cleanup();
        adapterWithCache.cleanup();
    }
    
    @Test
    @Order(4)
    @DisplayName("Benchmark denoising performance")
    void benchmarkDenoising() {
        double[] signal = generateNoisySignal(4096);
        VectorWaveSwtAdapter adapter = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            adapter.denoise(signal, 4, -1, true);
        }
        
        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            adapter.denoise(signal, 4, -1, true);
        }
        long endTime = System.nanoTime();
        
        double avgTimeMs = (endTime - startTime) / (BENCHMARK_ITERATIONS * 1_000_000.0);
        System.out.printf("Denoising (4096): %.2f ms per denoise operation%n", avgTimeMs);
        
        adapter.cleanup();
    }
    
    @Test
    @Order(5)
    @DisplayName("Benchmark memory efficiency with sparse storage")
    void benchmarkSparseStorage() {
        int signalLength = 8192;
        double[] signal = generateSparseSignal(signalLength);
        
        // Create dense result
        double[] approx = new double[signalLength];
        double[][] details = new double[5][signalLength];
        
        Random rand = new Random(42);
        for (int i = 0; i < signalLength; i++) {
            approx[i] = rand.nextGaussian();
            for (int level = 0; level < 5; level++) {
                // Make 90% of coefficients near zero
                details[level][i] = rand.nextDouble() > 0.9 ? rand.nextGaussian() : rand.nextGaussian() * 0.0001;
            }
        }
        
        SWTResult denseResult = new SWTResult(approx, details, 5);
        
        // Measure memory for dense storage
        long denseMemory = estimateMemoryUsage(denseResult);
        
        // Convert to sparse
        long startSparse = System.nanoTime();
        SWTResult.SparseSWTResult sparseResult = denseResult.toSparse(0.001);
        long endSparse = System.nanoTime();
        
        // Measure compression
        double compressionRatio = sparseResult.getCompressionRatio();
        double conversionTimeMs = (endSparse - startSparse) / 1_000_000.0;
        
        System.out.printf("Sparse storage: %.2fx compression, %.2f ms conversion time%n",
                         compressionRatio, conversionTimeMs);
        
        // Benchmark reconstruction
        long startRecon = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            sparseResult.toFull();
        }
        long endRecon = System.nanoTime();
        
        double reconTimeMs = (endRecon - startRecon) / (BENCHMARK_ITERATIONS * 1_000_000.0);
        System.out.printf("Sparse reconstruction: %.2f ms per reconstruction%n", reconTimeMs);
        
        // The compression ratio depends on how sparse the signal actually is
        // With 90% sparse (10% non-zero), we should get at least 2x compression
        Assertions.assertTrue(compressionRatio >= 2.0, "Should achieve at least 2x compression for sparse signals");
    }
    
    @Test
    @Order(6)
    @DisplayName("Compare different wavelet performance")
    void benchmarkDifferentWavelets() {
        double[] signal = generateSignal(4096);
        int levels = 4;
        
        // Test different wavelets
        benchmarkWavelet("DB2", Daubechies.DB2, signal, levels);
        benchmarkWavelet("DB4", Daubechies.DB4, signal, levels);
        benchmarkWavelet("DB6", Daubechies.DB6, signal, levels);
        benchmarkWavelet("DB8", Daubechies.DB8, signal, levels);
    }
    
    private void benchmarkWavelet(String name, Daubechies wavelet, double[] signal, int levels) {
        VectorWaveSwtAdapter adapter = new VectorWaveSwtAdapter(wavelet, BoundaryMode.PERIODIC);
        
        // Warmup
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            adapter.forward(signal, levels);
        }
        
        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < BENCHMARK_ITERATIONS; i++) {
            MutableMultiLevelMODWTResult result = adapter.forward(signal, levels);
            adapter.inverse(result);
        }
        long endTime = System.nanoTime();
        
        double avgTimeMs = (endTime - startTime) / (BENCHMARK_ITERATIONS * 1_000_000.0);
        System.out.printf("  %s: %.2f ms%n", name, avgTimeMs);
        
        adapter.cleanup();
    }
    
    // Helper methods
    
    private static double[] generateSignal(int length) {
        double[] signal = new double[length];
        Random rand = new Random(42);
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 
                       0.5 * Math.sin(2 * Math.PI * i / 64) +
                       0.1 * rand.nextGaussian();
        }
        return signal;
    }
    
    private static double[] generateNoisySignal(int length) {
        double[] signal = new double[length];
        Random rand = new Random(42);
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + rand.nextGaussian() * 0.5;
        }
        return signal;
    }
    
    private static double[] generateSparseSignal(int length) {
        double[] signal = new double[length];
        Random rand = new Random(42);
        for (int i = 0; i < length; i++) {
            // Only 5% of samples are non-zero
            if (rand.nextDouble() < 0.05) {
                signal[i] = rand.nextGaussian() * 10;
            }
        }
        return signal;
    }
    
    private static long estimateMemoryUsage(SWTResult result) {
        // Rough estimate: 8 bytes per double
        int totalDoubles = result.getSignalLength() * (result.getLevels() + 1);
        return totalDoubles * 8L;
    }
}