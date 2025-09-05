package com.morphiqlabs.benchmark;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.modwt.ParallelMultiLevelMODWT;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark to quantify the benefit of caching per-level scaled/upsampled filters (Issue 008).
 * Compares repeated multi-level MODWT decompositions with a cached transformer versus constructing
 * a new transformer per invocation (effectively disabling cache reuse).
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class MultiLevelFilterCacheBenchmark {

    @State(Scope.Thread)
    public static class BenchState {
        @Param({"4096", "8192", "16384", "32768"})
        public int n;
        @Param({"db4", "db8", "db16", "sym4", "sym8", "coif4", "coif8"})
        public String waveletCode;

        public double[] signal;
        public MultiLevelMODWTTransform cached;
        public int levels;
        public ParallelMultiLevelMODWT parallelCached;

        @Setup(Level.Trial)
        public void setup() {
            signal = new double[n];
            for (int i = 0; i < n; i++) {
                // Mixed-frequency test signal
                signal[i] = Math.sin(2 * Math.PI * i / 64.0) + 0.3 * Math.sin(2 * Math.PI * i / 7.0);
            }
            var wname = com.morphiqlabs.wavelet.api.WaveletName.valueOf(waveletCode.toUpperCase());
            var wavelet = com.morphiqlabs.wavelet.api.WaveletRegistry.getWavelet(wname);
            cached = new MultiLevelMODWTTransform(wavelet, BoundaryMode.PERIODIC);
            levels = cached.getMaximumLevels(n);

            // Prime caches once
            cached.decompose(signal, levels);
            parallelCached = new ParallelMultiLevelMODWT();
            parallelCached.decompose(signal, wavelet, BoundaryMode.PERIODIC, levels);
        }
    }

    @Benchmark
    public void cached_multiLevel_decompose(BenchState s, Blackhole bh) {
        MultiLevelMODWTResult r = s.cached.decompose(s.signal, s.levels);
        bh.consume(r);
    }

    @Benchmark
    public void noCache_newInstanceEachCall(BenchState s, Blackhole bh) {
        var wname = com.morphiqlabs.wavelet.api.WaveletName.valueOf(s.waveletCode.toUpperCase());
        var wavelet = com.morphiqlabs.wavelet.api.WaveletRegistry.getWavelet(wname);
        MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(wavelet, BoundaryMode.PERIODIC);
        MultiLevelMODWTResult r = t.decompose(s.signal, s.levels);
        bh.consume(r);
    }

    @Benchmark
    public void parallel_cached(BenchState s, Blackhole bh) {
        var wname = com.morphiqlabs.wavelet.api.WaveletName.valueOf(s.waveletCode.toUpperCase());
        var wavelet = com.morphiqlabs.wavelet.api.WaveletRegistry.getWavelet(wname);
        MultiLevelMODWTResult r = s.parallelCached.decompose(s.signal, wavelet, BoundaryMode.PERIODIC, s.levels);
        bh.consume(r);
    }

    @Benchmark
    public void parallel_noCache_newInstanceEachCall(BenchState s, Blackhole bh) {
        // New instance prevents reuse of the internal per-instance cache
        var wname = com.morphiqlabs.wavelet.api.WaveletName.valueOf(s.waveletCode.toUpperCase());
        var wavelet = com.morphiqlabs.wavelet.api.WaveletRegistry.getWavelet(wname);
        ParallelMultiLevelMODWT p = new ParallelMultiLevelMODWT();
        MultiLevelMODWTResult r = p.decompose(s.signal, wavelet, BoundaryMode.PERIODIC, s.levels);
        bh.consume(r);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
            .include(MultiLevelFilterCacheBenchmark.class.getSimpleName())
            .forks(1)
            .warmupIterations(3)
            .measurementIterations(5)
            .build();
        new Runner(opt).run();
    }
}

