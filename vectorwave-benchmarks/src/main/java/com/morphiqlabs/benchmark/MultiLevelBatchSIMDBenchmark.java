package com.morphiqlabs.benchmark;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.extensions.modwt.BatchSIMDMODWT;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * JMH benchmark comparing SIMD batch multi-level MODWT vs scalar multi-level.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class MultiLevelBatchSIMDBenchmark {

    @State(Scope.Thread)
    public static class BenchState {
        @Param({"8", "16", "32"})
        public int batchSize;
        @Param({"4096", "8192"})
        public int n;
        @Param({"db4", "haar"})
        public String waveletCode;
        @Param({"3", "5"})
        public int levels;

        public double[][] signalsAoS;
        public double[] soaSignals;
        public double[][] soaDetails;
        public double[] soaApprox;
        public MultiLevelMODWTTransform scalarTransform;
        public DiscreteWavelet wavelet;

        @Setup(Level.Trial)
        public void setup() {
            // Select wavelet
            if ("haar".equalsIgnoreCase(waveletCode)) {
                wavelet = (DiscreteWavelet) new Haar();
            } else {
                wavelet = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.valueOf(waveletCode.toUpperCase()));
            }

            // Signals
            signalsAoS = new double[batchSize][n];
            Random rnd = new Random(42);
            for (int b = 0; b < batchSize; b++) {
                for (int i = 0; i < n; i++) {
                    signalsAoS[b][i] = rnd.nextGaussian();
                }
            }

            // SoA buffers
            soaSignals = new double[batchSize * n];
            soaDetails = new double[levels][batchSize * n];
            soaApprox = new double[batchSize * n];
            BatchSIMDMODWT.convertToSoA(signalsAoS, soaSignals);

            // Scalar transform (periodic)
            scalarTransform = new MultiLevelMODWTTransform(wavelet, BoundaryMode.PERIODIC);
            int max = scalarTransform.getMaximumLevels(n);
            if (levels > max) levels = max;
        }
    }

    @Benchmark
    public void simd_multiLevel(BenchState s, Blackhole bh) {
        // Reuse input SoA, write per-level details and final approx
        BatchSIMDMODWT.batchMultiLevelMODWTSoA(s.soaSignals, s.soaDetails, s.soaApprox,
                s.wavelet, s.batchSize, s.n, s.levels);
        bh.consume(s.soaApprox);
        bh.consume(s.soaDetails);
    }

    @Benchmark
    public void scalar_multiLevel(BenchState s, Blackhole bh) {
        for (int b = 0; b < s.batchSize; b++) {
            MultiLevelMODWTResult r = s.scalarTransform.decompose(s.signalsAoS[b], s.levels);
            bh.consume(r);
        }
    }
}

