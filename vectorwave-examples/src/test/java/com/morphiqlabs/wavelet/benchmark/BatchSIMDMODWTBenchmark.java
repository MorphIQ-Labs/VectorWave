package com.morphiqlabs.wavelet.benchmark;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.extensions.modwt.BatchSIMDMODWT;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.util.ThreadLocalManager;

import java.util.Random;

/**
 * Benchmark for batch SIMD MODWT implementation.
 */
public class BatchSIMDMODWTBenchmark {
    
    private static final int WARMUP_ITERATIONS = 100;
    private static final int MEASURE_ITERATIONS = 1000;
    
    public static void main(String[] args) {
        System.out.println("Batch SIMD MODWT Benchmark");
        System.out.println("==========================");
        System.out.println("Vector length: " + jdk.incubator.vector.DoubleVector.SPECIES_PREFERRED.length());
        System.out.println();
        
        // Test different batch sizes and signal lengths
        int[] batchSizes = {4, 8, 16, 32, 64, 128};
        int[] signalLengths = {64, 128, 256, 512, 1024};
        
        System.out.println("Haar Wavelet Benchmarks");
        System.out.println("-----------------------");
        for (int batchSize : batchSizes) {
            for (int signalLength : signalLengths) {
                if (batchSize * signalLength <= 131072) { // Limit total memory
                    benchmarkBatch(new Haar(), "Haar", batchSize, signalLength);
                }
            }
            System.out.println();
        }
        
        System.out.println("\nDaubechies-4 Wavelet Benchmarks");
        System.out.println("--------------------------------");
        for (int batchSize : batchSizes) {
            for (int signalLength : signalLengths) {
                if (batchSize * signalLength <= 131072) { // Limit total memory
                    benchmarkBatch(Daubechies.DB4, "DB4", batchSize, signalLength);
                }
            }
            System.out.println();
        }
    }
    
    private static void benchmarkBatch(com.morphiqlabs.wavelet.api.DiscreteWavelet wavelet, 
                                      String waveletName, int batchSize, int signalLength) {
        
        // Generate test data
        double[][] signals = generateBatchSignals(batchSize, signalLength);
        
        // Allocate output arrays
        double[][] seqApprox = new double[batchSize][signalLength];
        double[][] seqDetail = new double[batchSize][signalLength];
        
        double[] soaSignals = new double[batchSize * signalLength];
        double[] soaApprox = new double[batchSize * signalLength];
        double[] soaDetail = new double[batchSize * signalLength];
        
        // Convert to SoA for SIMD
        BatchSIMDMODWT.convertToSoA(signals, soaSignals);
        
        // Warmup
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        for (int i = 0; i < WARMUP_ITERATIONS; i++) {
            // Sequential
            for (int b = 0; b < batchSize; b++) {
                MODWTResult result = transform.forward(signals[b]);
                System.arraycopy(result.approximationCoeffs(), 0, seqApprox[b], 0, signalLength);
                System.arraycopy(result.detailCoeffs(), 0, seqDetail[b], 0, signalLength);
            }
            
            // SIMD
            BatchSIMDMODWT.batchMODWTSoA(soaSignals, soaApprox, soaDetail, 
                                        wavelet, batchSize, signalLength);
        }
        
        // Measure sequential
        long seqStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            for (int b = 0; b < batchSize; b++) {
                MODWTResult result = transform.forward(signals[b]);
                System.arraycopy(result.approximationCoeffs(), 0, seqApprox[b], 0, signalLength);
                System.arraycopy(result.detailCoeffs(), 0, seqDetail[b], 0, signalLength);
            }
        }
        long seqTime = System.nanoTime() - seqStart;
        
        // Measure SIMD
        long simdStart = System.nanoTime();
        for (int i = 0; i < MEASURE_ITERATIONS; i++) {
            BatchSIMDMODWT.batchMODWTSoA(soaSignals, soaApprox, soaDetail, 
                                        wavelet, batchSize, signalLength);
        }
        long simdTime = System.nanoTime() - simdStart;
        
        // Clean up
        ThreadLocalManager.cleanupCurrentThread();
        
        // Calculate results
        double seqMs = seqTime / 1e6 / MEASURE_ITERATIONS;
        double simdMs = simdTime / 1e6 / MEASURE_ITERATIONS;
        double speedup = seqMs / simdMs;
        
        System.out.printf("%s - Batch: %3d, Signal: %4d | Seq: %6.3f ms | SIMD: %6.3f ms | Speedup: %5.2fx%n",
            waveletName, batchSize, signalLength, seqMs, simdMs, speedup);
    }
    
    private static double[][] generateBatchSignals(int batchSize, int signalLength) {
        Random random = new Random(42);
        double[][] signals = new double[batchSize][signalLength];
        
        for (int b = 0; b < batchSize; b++) {
            // Mix of different signal types
            switch (b % 4) {
                case 0 -> { // Random noise
                    for (int t = 0; t < signalLength; t++) {
                        signals[b][t] = random.nextGaussian();
                    }
                }
                case 1 -> { // Sine wave
                    for (int t = 0; t < signalLength; t++) {
                        signals[b][t] = Math.sin(2 * Math.PI * t / signalLength * (b + 1));
                    }
                }
                case 2 -> { // Step function
                    for (int t = 0; t < signalLength; t++) {
                        signals[b][t] = (t < signalLength / 2) ? 1.0 : -1.0;
                    }
                }
                case 3 -> { // Chirp signal
                    for (int t = 0; t < signalLength; t++) {
                        double freq = 0.1 + 0.4 * t / signalLength;
                        signals[b][t] = Math.sin(2 * Math.PI * freq * t);
                    }
                }
            }
        }
        
        return signals;
    }
}
