package com.morphiqlabs.benchmark;

import com.morphiqlabs.benchmark.util.MinimalFFT;
import com.morphiqlabs.wavelet.cwt.ComplexNumber;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(java.util.concurrent.TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class StockhamVsDefaultFftBenchmark {

    @Param({"1024", "4096", "16384"})
    public int n;

    @Param({"false", "true"})
    public String stockham; // "true" or "false"

    private double[] baseReal;
    private double[] baseImag;
    private double[] baseSignal;

    @Setup(Level.Trial)
    public void setup() {
        // Toggle Stockham flag for this trial
        System.setProperty("vectorwave.fft.stockham", stockham);

        Random rnd = new Random(12345);
        baseReal = new double[n];
        baseImag = new double[n];
        baseSignal = new double[n];
        for (int i = 0; i < n; i++) {
            baseReal[i] = rnd.nextDouble() * 2 - 1;
            baseImag[i] = rnd.nextDouble() * 2 - 1;
            baseSignal[i] = rnd.nextDouble() * 2 - 1;
        }
    }

    @Benchmark
    public void complexFFT(Blackhole bh) {
        // Interleave real/imag into a complex buffer
        double[] interleaved = new double[2 * n];
        for (int i = 0; i < n; i++) {
            interleaved[2 * i] = baseReal[i];
            interleaved[2 * i + 1] = baseImag[i];
        }
        MinimalFFT.fftOptimized(interleaved, n, false);
        bh.consume(interleaved);
    }

    @Benchmark
    public void realRFFT(Blackhole bh) {
        ComplexNumber[] spec = MinimalFFT.fftRealOptimized(baseSignal);
        bh.consume(spec);
    }
}
