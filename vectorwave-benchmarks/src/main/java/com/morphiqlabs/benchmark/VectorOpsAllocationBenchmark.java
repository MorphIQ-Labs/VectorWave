package com.morphiqlabs.benchmark;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.extensions.modwt.BatchMODWT;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Microbenchmarks comparing public scalar core vs public SIMD extensions paths.
 *
 * Run with GC profiler to observe allocation rate:
 * mvn -q -pl vectorwave-examples -am exec:java \
 *   -Dexec.mainClass=org.openjdk.jmh.Main \
 *   -Dexec.args="com.morphiqlabs.benchmark.VectorOpsAllocationBenchmark -prof gc -f 1 -wi 5 -i 10"
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 5)
@Measurement(iterations = 10)
@Fork(value = 1)
public class VectorOpsAllocationBenchmark {

    @State(Scope.Thread)
    public static class Data {
        @Param({"4096"})
        public int n;

        double[] signal;
        double[][] batch1;
        DiscreteWavelet wavelet;
        MODWTTransform core;

        @Setup(Level.Trial)
        public void setup() {
            wavelet = (DiscreteWavelet) Daubechies.DB4;
            core = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
            signal = new double[n];
            batch1 = new double[1][n];
            Random rnd = new Random(42);
            for (int i = 0; i < n; i++) {
                double v = Math.sin(0.01 * i) + 0.1 * rnd.nextGaussian();
                signal[i] = v;
                batch1[0][i] = v;
            }
        }
    }

    // Core scalar path via public MODWTTransform
    @Benchmark
    public void modwtForward_core(Data d, Blackhole bh) {
        MODWTResult r = d.core.forward(d.signal);
        bh.consume(r.approximationCoeffs());
        bh.consume(r.detailCoeffs());
    }

    // Extensions SIMD path via public BatchMODWT (single-entry batch)
    @Benchmark
    public void modwtForward_extensions_batch1(Data d, Blackhole bh) {
        BatchMODWT.SingleLevelResult out = BatchMODWT.singleLevelAoS(d.wavelet, d.batch1);
        bh.consume(out.approx());
        bh.consume(out.detail());
    }
}
