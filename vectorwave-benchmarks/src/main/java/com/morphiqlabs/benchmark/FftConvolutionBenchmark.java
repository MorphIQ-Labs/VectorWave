package com.morphiqlabs.benchmark;

import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.WaveletOperations;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

/**
 * Benchmarks scalar vs FFT circular convolution paths for MODWT periodic convolution.
 * Uses upsampled DB8 analysis filters at high levels to produce large effective L.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@Fork(1)
public class FftConvolutionBenchmark {

    @State(Scope.Thread)
    public static class BenchState {
        @Param({"8192", "16384", "32768"})
        public int n;
        @Param({"9", "10"}) // levels leading to large L for DB8
        public int level;
        public double[] signal;
        public double[] filter;
        public double[] out;

        @Setup(Level.Trial)
        public void setup() {
            signal = new double[n];
            for (int i = 0; i < n; i++) {
                // Mixed tones
                signal[i] = Math.sin(2 * Math.PI * i / 64.0) + 0.5 * Math.sin(2 * Math.PI * i / 7.0);
            }
            // Upsample DB8 analysis filter to target level (public reimplementation)
            filter = upsampleAndScaleForMODWTAnalysis(Daubechies.DB8.lowPassDecomposition(), level);
            out = new double[n];
        }
    }

    @Benchmark
    public void scalar_convolution(BenchState s, Blackhole bh) {
        WaveletOperations.circularConvolveMODWT(s.signal, s.filter, s.out);
        bh.consume(s.out);
    }

    // Local helper: upsample filter by inserting 2^(level-1)-1 zeros and scale by 2^(-level/2)
    private static double[] upsampleAndScaleForMODWTAnalysis(double[] base, int level) {
        if (level <= 1) {
            double[] scaled = new double[base.length];
            double scale = 1.0 / Math.sqrt(2.0); // level 1 scaling
            for (int i = 0; i < base.length; i++) scaled[i] = base[i] * scale;
            return scaled;
        }
        int zeros = (1 << (level - 1)) - 1;
        int outLen = base.length + (base.length - 1) * zeros;
        double[] up = new double[outLen];
        int idx = 0;
        for (int i = 0; i < base.length; i++) {
            up[idx++] = base[i];
            if (i < base.length - 1) {
                for (int z = 0; z < zeros; z++) up[idx++] = 0.0;
            }
        }
        double scale = Math.pow(2.0, -level / 2.0);
        for (int i = 0; i < up.length; i++) up[i] *= scale;
        return up;
    }
}

