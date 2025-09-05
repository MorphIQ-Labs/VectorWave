# Core FFT Optimizations

This document summarizes the optimizations and configuration flags around the canonical `CoreFFT` implementation in `vectorwave-fft` (consumed by `vectorwave-core`).

## Overview

- Unified FFT implementation (`CoreFFT`) provided by `vectorwave-fft` and used across the core module.
- Opt-in performance features with strict parity against the default path.

## Optimizations

- Thread-local scratch buffers:
  - Reuse arrays for temporary real/imag storage and interleaved conversions.
  - Reduces per-call allocations in `rfft/irfft` and interleaved helpers.

- Real-optimized RFFT (flagged):
  - `-Dvectorwave.fft.realOptimized=true`
  - Uses even/odd split and two half-size complex FFTs with a combine step.
  - Parity-validated vs full-complex path within 1e-12.

- Stockham autosort kernel (flagged):
  - `-Dvectorwave.fft.stockham=true`
  - DIF Stockham radix-2 with ping-pong buffers (auto-sorts per stage, no bit reversal).
  - Parity-validated vs Cooley–Tukey path (round-trip and randomized inputs).

- Stage twiddle caching:
  - Per-call (thread-local) precompute of W\_N^(j·r) using a single-angle recurrence for each stage.
  - Eliminates inner-loop trig calls.

- Global twiddle cache (flagged):
  - `-Dvectorwave.fft.twiddleCache.enabled=true|false` (default: `true`)
  - Range: `minN=1024`, `maxN=65536` (override via system properties).
  - Caches per-stage twiddles for power-of-two sizes in range to amortize trig cost across calls.

## Configuration

See `docs/CONFIGURATION.md` for detailed flags and examples:

- Stockham: `-Dvectorwave.fft.stockham=true`
- Real-optimized RFFT: `-Dvectorwave.fft.realOptimized=true`
- Twiddle cache controls:
  - `-Dvectorwave.fft.twiddleCache.enabled=true|false`
  - `-Dvectorwave.fft.twiddleCache.minN=1024`
  - `-Dvectorwave.fft.twiddleCache.maxN=65536`

## Benchmarks

- JMH benchmark: `vectorwave-benchmarks/src/main/java/com/morphiqlabs/benchmark/StockhamVsDefaultFftBenchmark.java`
- Run:
```
mvn -q -pl vectorwave-benchmarks -am clean compile \
  && mvn -q -pl vectorwave-benchmarks exec:java \
     -Dexec.mainClass=org.openjdk.jmh.Main \
     -Dexec.classpathScope=compile \
     -Dexec.args="com.morphiqlabs.benchmark.StockhamVsDefaultFftBenchmark -wi 5 -i 10 -f 2 -rf csv -rff target/jmh-stockham.csv"
```

Notes:
- On some environments, JMH may require the JVM args to be set via `-Dexec.jvmArgs` (not as program args). The examples POM is set up accordingly.
- `CoreFFT.rfft` calls the complex FFT internally, so enabling Stockham affects real FFT timings too.

## Correctness

- Parity and round-trip tests ensure strict numerical equivalence across flags:
  - Complex FFT parity with Stockham on/off.
  - Complex FFT round-trip on power-of-two sizes.
  - Real-optimized RFFT parity vs full-complex path.
