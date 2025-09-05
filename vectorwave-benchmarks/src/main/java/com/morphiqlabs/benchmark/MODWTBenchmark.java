package com.morphiqlabs.benchmark;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Comprehensive MODWT performance benchmark.
 * 
 * <p>Measures MODWT performance across different scenarios:</p>
 * <ul>
 *   <li>Various signal lengths (including non-power-of-2)</li>
 *   <li>Different wavelets and boundary modes</li>
 *   <li>Single vs multi-level decomposition</li>
 *   <li>Batch processing efficiency</li>
 * </ul>
 */
@BenchmarkMode({Mode.AverageTime, Mode.Throughput})
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
@Fork(value = 1, jvmArgs = {
    "--add-modules", "jdk.incubator.vector",
    "-XX:+UseG1GC",
    "-Xmx2g",
    "-XX:+UnlockDiagnosticVMOptions",
    "-XX:+PrintInlining",
    "-XX:+PrintCompilation"
})
@Warmup(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 10, time = 3, timeUnit = TimeUnit.SECONDS)
public class MODWTBenchmark {
    
    @Param({"777", "1024", "2048", "4096", "8192", "12345"})
    private int signalSize;
    
    @Param({"DB4", "DB8", "HAAR", "SYM8", "COIF4"})
    private String waveletName;
    
    @Param({"PERIODIC", "SYMMETRIC", "ZERO"})
    private String boundaryMode;
    
    @Param({"3", "5", "7"})
    private int levels;
    
    private double[] signal;
    private double[][] batchSignals;
    private MODWTTransform singleLevel;
    private MultiLevelMODWTTransform multiLevel;
    
    @Setup
    public void setup() {
        // Generate test signals
        Random random = new Random(42);
        signal = generateComplexSignal(signalSize, random);
        
        // Generate batch for batch processing tests
        batchSignals = new double[16][];
        for (int i = 0; i < 16; i++) {
            batchSignals[i] = generateComplexSignal(signalSize, random);
        }
        
        // Setup transforms
        Wavelet wavelet = WaveletRegistry.getWavelet(WaveletName.valueOf(waveletName));
        BoundaryMode mode = BoundaryMode.valueOf(boundaryMode);
        
        singleLevel = new MODWTTransform(wavelet, mode);
        multiLevel = new MultiLevelMODWTTransform(wavelet, mode);
    }
    
    @Benchmark
    public MODWTResult singleLevelForward() {
        return singleLevel.forward(signal);
    }
    
    @Benchmark
    public double[] singleLevelRoundTrip() {
        MODWTResult result = singleLevel.forward(signal);
        return singleLevel.inverse(result);
    }
    
    @Benchmark
    public MultiLevelMODWTResult multiLevelDecompose() {
        return multiLevel.decompose(signal, levels);
    }
    
    @Benchmark
    public double[] multiLevelRoundTrip() {
        MultiLevelMODWTResult result = multiLevel.decompose(signal, levels);
        return multiLevel.reconstruct(result);
    }
    
    @Benchmark
    public MODWTResult[] batchProcessing() {
        return singleLevel.forwardBatch(batchSignals);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    @Warmup(iterations = 0)
    @Measurement(iterations = 20)
    public double[] coldStart() {
        MODWTTransform coldTransform = new MODWTTransform(
            WaveletRegistry.getWavelet(WaveletName.valueOf(waveletName)),
            BoundaryMode.valueOf(boundaryMode)
        );
        MODWTResult result = coldTransform.forward(signal);
        return coldTransform.inverse(result);
    }
    
    private static double[] generateComplexSignal(int length, Random random) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            double t = 2.0 * Math.PI * i / length;
            // Multiple frequency components
            signal[i] = Math.sin(t) + 0.5 * Math.sin(4 * t) + 0.25 * Math.sin(8 * t);
            // Add chirp
            double chirp = Math.sin(t * (1 + 0.5 * i / length));
            signal[i] += 0.3 * chirp;
            // Add noise
            signal[i] += 0.1 * random.nextGaussian();
        }
        return signal;
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(MODWTBenchmark.class.getSimpleName())
            .result("modwt-benchmark-results.csv")
            .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.CSV)
            .build();
        
        new Runner(opt).run();
    }
}

