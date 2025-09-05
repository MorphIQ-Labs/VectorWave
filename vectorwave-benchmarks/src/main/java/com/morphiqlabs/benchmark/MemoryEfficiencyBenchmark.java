package com.morphiqlabs.benchmark;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MODWTResult;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Memory efficiency benchmark for VectorWave operations.
 * 
 * <p>Measures memory allocation patterns and GC pressure for:</p>
 * <ul>
 *   <li>Standard vs streaming processing</li>
 *   <li>Object pooling effectiveness</li>
 *   <li>Large signal processing</li>
 *   <li>Batch processing memory patterns</li>
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {
    "--add-modules", "jdk.incubator.vector",
    "-XX:+UseG1GC",
    "-Xmx1g",
    "-Xms1g",
    "-XX:+UnlockExperimentalVMOptions",
    "-XX:G1NewSizePercent=10",
    "-XX:G1MaxNewSizePercent=10",
    "-XX:+AlwaysPreTouch"
})
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class MemoryEfficiencyBenchmark {
    
    @Param({"1024", "16384", "131072"})
    private int signalSize;
    
    @Param({"512", "2048", "8192"})
    private int blockSize;
    
    private double[] largeSignal;
    private double[][] signalBatch;
    private MODWTTransform transform;
    private Wavelet wavelet;
    
    @Setup
    public void setup() {
        // Generate test data
        Random random = new Random(42);
        largeSignal = new double[signalSize];
        for (int i = 0; i < signalSize; i++) {
            largeSignal[i] = random.nextGaussian();
        }
        
        // Generate batch
        signalBatch = new double[32][blockSize];
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < blockSize; j++) {
                signalBatch[i][j] = random.nextGaussian();
            }
        }
        
        // Setup transforms
        wavelet = WaveletRegistry.getWavelet(WaveletName.DB4);
        transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
    }
    
    @TearDown
    public void tearDown() {
        // Cleanup if needed
    }
    
    @Benchmark
    public void standardProcessing(Blackhole bh) {
        MODWTResult result = transform.forward(largeSignal);
        double[] reconstructed = transform.inverse(result);
        bh.consume(reconstructed);
    }
    
    @Benchmark
    public void blockProcessing(Blackhole bh) {
        // Process signal in blocks to simulate streaming
        int numBlocks = (signalSize + blockSize - 1) / blockSize;
        for (int i = 0; i < numBlocks; i++) {
            int start = i * blockSize;
            int end = Math.min(start + blockSize, signalSize);
            int currentBlockSize = end - start;
            
            double[] block = new double[currentBlockSize];
            System.arraycopy(largeSignal, start, block, 0, currentBlockSize);
            
            // Process each block independently
            MODWTResult blockResult = transform.forward(block);
            double[] reconstructed = transform.inverse(blockResult);
            bh.consume(reconstructed);
        }
    }
    
    @Benchmark
    public void batchProcessingWithReuse(Blackhole bh) {
        // Reuse arrays to minimize allocation
        MODWTResult[] results = new MODWTResult[signalBatch.length];
        
        for (int i = 0; i < signalBatch.length; i++) {
            results[i] = transform.forward(signalBatch[i]);
        }
        
        for (int i = 0; i < signalBatch.length; i++) {
            double[] reconstructed = transform.inverse(results[i]);
            bh.consume(reconstructed);
        }
    }
    
    @Benchmark
    public void batchProcessingOptimized(Blackhole bh) {
        MODWTResult[] results = transform.forwardBatch(signalBatch);
        double[][] reconstructed = transform.inverseBatch(results);
        bh.consume(reconstructed);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Measurement(iterations = 100)
    public void allocationRate(Blackhole bh) {
        // Measure allocation rate for single transform
        MODWTResult result = transform.forward(largeSignal);
        bh.consume(result);
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(MemoryEfficiencyBenchmark.class.getSimpleName())
            .addProfiler(GCProfiler.class)
            .result("memory-efficiency-results.csv")
            .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.CSV)
            .build();
        
        new Runner(opt).run();
    }
}

