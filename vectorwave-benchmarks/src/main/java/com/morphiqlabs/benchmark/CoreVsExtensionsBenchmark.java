package com.morphiqlabs.benchmark;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MODWTResult;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark comparing core (scalar) vs extensions (SIMD) module performance.
 * 
 * <p>This benchmark demonstrates the performance benefits of including the
 * vectorwave-extensions module with Vector API optimizations.</p>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Fork(value = 2, jvmArgs = {
    "--add-modules", "jdk.incubator.vector",
    "-XX:+UseG1GC",
    "-Xmx2g"
})
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
public class CoreVsExtensionsBenchmark {
    
    @Param({"1024", "4096", "16384", "65536"})
    private int signalSize;
    
    @Param({"DB4", "DB8", "SYM8"})
    private String waveletName;
    
    private double[] signal;
    private MODWTTransform transform;
    private Wavelet wavelet;
    
    @Setup
    public void setup() {
        // Generate test signal
        Random random = new Random(42);
        signal = new double[signalSize];
        for (int i = 0; i < signalSize; i++) {
            double t = 2.0 * Math.PI * i / signalSize;
            signal[i] = Math.sin(t) + 0.5 * Math.sin(4 * t) + 0.1 * random.nextGaussian();
        }
        
        // Setup transform - will use SIMD if extensions module is available
        wavelet = WaveletRegistry.getWavelet(WaveletName.valueOf(waveletName));
        transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
    }
    
    @Benchmark
    public MODWTResult forward() {
        return transform.forward(signal);
    }
    
    @Benchmark
    public double[] roundTrip() {
        MODWTResult result = transform.forward(signal);
        return transform.inverse(result);
    }
    
    @Benchmark
    public MODWTResult[] forwardBatch() {
        double[][] batch = new double[8][signalSize];
        for (int i = 0; i < 8; i++) {
            System.arraycopy(signal, 0, batch[i], 0, signalSize);
        }
        return transform.forwardBatch(batch);
    }
    
    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(CoreVsExtensionsBenchmark.class.getSimpleName())
            .result("core-vs-extensions-results.csv")
            .resultFormat(org.openjdk.jmh.results.format.ResultFormatType.CSV)
            .build();
        
        new Runner(opt).run();
    }
}

