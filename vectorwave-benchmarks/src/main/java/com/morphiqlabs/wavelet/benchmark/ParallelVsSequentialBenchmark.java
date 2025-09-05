package com.morphiqlabs.wavelet.benchmark;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.cwt.RickerWavelet;
import com.morphiqlabs.wavelet.cwt.CWTConfig;
import com.morphiqlabs.wavelet.cwt.CWTResult;
import com.morphiqlabs.wavelet.cwt.CWTTransform;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.extensions.parallel.*;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark comparing parallel vs sequential performance for wavelet transforms.
 * 
 * <p>This benchmark evaluates the performance characteristics of VectorWave's parallel
 * processing framework against sequential implementations across different scenarios:</p>
 * <ul>
 *   <li>Multi-level MODWT decomposition and reconstruction</li>
 *   <li>CWT analysis with different scale ranges</li>
 *   <li>Wavelet denoising with various threshold methods</li>
 *   <li>Signal size scaling behavior</li>
 *   <li>Level count scaling behavior</li>
 * </ul>
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {
    "--add-modules", "jdk.incubator.vector",
    "-XX:+UseG1GC",
    "-Xmx2g"
})
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class ParallelVsSequentialBenchmark {
    
    // Test signal sizes
    @Param({"1024", "4096", "16384"})
    private int signalSize;
    
    // Decomposition levels
    @Param({"3", "5", "7"})
    private int levels;
    
    // Test signals
    private double[] signal;
    private double[] largeCWTSignal;
    private double[] noisySignal;
    
    // Wavelet transforms
    private MultiLevelMODWTTransform sequentialMODWT;
    private ParallelMultiLevelTransform parallelMODWT;
    
    // CWT transforms
    private CWTTransform sequentialCWT;
    private ParallelCWTTransform parallelCWT;
    
    // Denoising
    private WaveletDenoiser sequentialDenoiser;
    private ParallelWaveletDenoiser parallelDenoiser;
    
    // CWT scales
    private double[] cwtScales;
    
    @Setup
    public void setup() {
        // Generate reproducible test signals
        Random random = new Random(42);
        signal = generateSignal(signalSize, random);
        largeCWTSignal = generateSignal(Math.max(signalSize, 2048), random);
        noisySignal = generateNoisySignal(signalSize, 0.3, random);
        
        // Setup MODWT transforms
        Wavelet db4 = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.PERIODIC;
        
        sequentialMODWT = new MultiLevelMODWTTransform(db4, mode);
        parallelMODWT = new ParallelMultiLevelTransform(db4, mode, 
            new ParallelConfig.Builder()
                .parallelThreshold(512) // Lower threshold for benchmarking
                .enableMetrics(true)
                .build());
        
        // Setup CWT transforms
        RickerWavelet ricker = new RickerWavelet();
        CWTConfig cwtConfig = CWTConfig.defaultConfig();
        
        sequentialCWT = new CWTTransform(ricker, cwtConfig);
        parallelCWT = new ParallelCWTTransform(ricker, cwtConfig,
            new ParallelConfig.Builder()
                .parallelThreshold(512) // Lower threshold for benchmarking
                .enableMetrics(true)
                .build());
        
        // Setup denoising
        sequentialDenoiser = new WaveletDenoiser(db4, mode);
        parallelDenoiser = new ParallelWaveletDenoiser(db4, mode,
            new ParallelConfig.Builder()
                .parallelThreshold(512) // Lower threshold for benchmarking
                .enableParallelThresholding(true)
                .enableMetrics(true)
                .build());
        
        // Generate CWT scales
        cwtScales = generateLogScales(1.0, 32.0, 32);
    }
    
    @TearDown
    public void tearDown() {
        // Cleanup parallel resources
        if (parallelCWT != null) {
            parallelCWT.close();
        }
    }
    
    // ===============================================
    // MODWT Benchmarks
    // ===============================================
    
    @Benchmark
    public MultiLevelMODWTResult modwtSequentialDecompose() {
        return sequentialMODWT.decompose(signal, levels);
    }
    
    @Benchmark
    public MultiLevelMODWTResult modwtParallelDecompose() {
        return parallelMODWT.decompose(signal, levels);
    }
    
    @Benchmark
    public double[] modwtSequentialRoundTrip() {
        MultiLevelMODWTResult result = sequentialMODWT.decompose(signal, levels);
        return sequentialMODWT.reconstruct(result);
    }
    
    @Benchmark
    public double[] modwtParallelRoundTrip() {
        MultiLevelMODWTResult result = parallelMODWT.decompose(signal, levels);
        return parallelMODWT.reconstruct(result);
    }
    
    // ===============================================
    // CWT Benchmarks
    // ===============================================
    
    @Benchmark
    public CWTResult cwtSequentialAnalyze() {
        return sequentialCWT.analyze(largeCWTSignal, cwtScales);
    }
    
    @Benchmark
    public CWTResult cwtParallelAnalyze() {
        return parallelCWT.analyze(largeCWTSignal, cwtScales);
    }
    
    // ===============================================
    // Denoising Benchmarks
    // ===============================================
    
    @Benchmark
    public double[] denoiseSequential() {
        return sequentialDenoiser.denoiseMultiLevel(noisySignal, levels,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT);
    }
    
    @Benchmark
    public double[] denoiseParallel() {
        return parallelDenoiser.denoiseMultiLevel(noisySignal, levels,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT);
    }
    
    // ===============================================
    // Batch Processing Benchmarks
    // ===============================================
    
    @Benchmark
    public double[][] batchDenoiseSequential() {
        double[][] batch = generateSignalBatch(8, signalSize);
        double[][] results = new double[batch.length][];
        for (int i = 0; i < batch.length; i++) {
            results[i] = sequentialDenoiser.denoiseMultiLevel(batch[i], levels,
                WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT);
        }
        return results;
    }
    
    @Benchmark
    public double[][] batchDenoiseParallel() {
        double[][] batch = generateSignalBatch(8, signalSize);
        return parallelDenoiser.denoiseBatch(batch,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT);
    }
    
    // ===============================================
    // Helper Methods
    // ===============================================
    
    private static double[] generateSignal(int length, Random random) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            // Generate a signal with multiple frequency components
            double t = 2.0 * Math.PI * i / length;
            signal[i] = Math.sin(t) + 0.5 * Math.sin(4 * t) + 0.25 * Math.sin(8 * t);
            signal[i] += 0.1 * random.nextGaussian(); // Add small amount of noise
        }
        return signal;
    }
    
    private static double[] generateNoisySignal(int length, double noiseLevel, Random random) {
        double[] signal = generateSignal(length, random);
        for (int i = 0; i < length; i++) {
            signal[i] += noiseLevel * random.nextGaussian();
        }
        return signal;
    }
    
    private static double[] generateLogScales(double minScale, double maxScale, int count) {
        double[] scales = new double[count];
        double logMin = Math.log(minScale);
        double logMax = Math.log(maxScale);
        for (int i = 0; i < count; i++) {
            double logScale = logMin + (logMax - logMin) * i / (count - 1);
            scales[i] = Math.exp(logScale);
        }
        return scales;
    }
    
    private double[][] generateSignalBatch(int batchSize, int signalLength) {
        Random random = new Random(42);
        double[][] batch = new double[batchSize][];
        for (int i = 0; i < batchSize; i++) {
            batch[i] = generateNoisySignal(signalLength, 0.3, random);
        }
        return batch;
    }
    
    /**
     * Main method to run the benchmark.
     */
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(ParallelVsSequentialBenchmark.class.getSimpleName())
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .jvmArgs("--add-modules", "jdk.incubator.vector")
            .build();
        
        new Runner(opt).run();
    }
}

