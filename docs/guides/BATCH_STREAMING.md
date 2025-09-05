# Batch Streaming MODWT (Extensions)

This guide shows how to stream-process batches of signals using the SIMD-optimized SoA kernels under the hood, while keeping a user-friendly Array-of-Arrays (AoS) API.

> Where SIMD lives
>
> Batch streaming SIMD support is part of the optional `vectorwave-extensions` module (Java 24 + incubator). The core module provides scalar streaming; enable extensions and run with `--add-modules jdk.incubator.vector --enable-preview` to use SIMD paths.

## When to use

- You process many signals in fixed-size blocks (e.g., real-time audio, finance ticks in windows).
- You want predictable per-block latency and high throughput on mid/large blocks.
- You accept per-block boundary semantics (see below), or you use PERIODIC mode.

## API Overview

- Single level (AoS I/O):
  - `BatchMODWT.singleLevelAoS(wavelet, double[][] signals)` -> `SingleLevelResult { double[][] approx, detail }`
- Multi level (AoS I/O):
  - `BatchMODWT.multiLevelAoS(wavelet, double[][] signals, int levels)` -> `MultiLevelResult { double[][][] detailPerLevel, double[][] finalApprox }`
- Streaming facade (per-block):
  - `BatchStreamingMODWT.Builder().wavelet(...).boundary(PERIODIC|ZERO_PADDING|SYMMETRIC).levels(L).build()`
  - `processSingleLevel(double[][] block)` or `processMultiLevel(double[][] block)`

All arrays are AoS: `[batch][length]` for inputs/outputs and `[levels][batch][length]` for per-level details.

## Boundary Modes

- PERIODIC: Uses SIMD SoA kernels (`BatchSIMDMODWT`) for maximum throughput; no per-instance state is required across blocks. Each block is treated as circular.
- ZERO_PADDING and SYMMETRIC: Now backed by SIMD streaming with a left-history (ring buffer) per level. The facade maintains per-level SoA history across calls and applies the same upsampled, 1/√2-scaled analysis filters as core. Parity matches whole-signal core transforms when concatenating block outputs in order.
  - First block initialization: history is zero-filled (ZERO_PADDING) or synthesized via symmetric reflection from the current block (SYMMETRIC).
  - Subsequent blocks: history is updated from the previous block’s tail; no right-context is required for MODWT’s (t − l) convolution.

## Usage

```java
try (var streaming = new BatchStreamingMODWT.Builder()
        .wavelet(com.morphiqlabs.wavelet.api.Daubechies.DB4)
        .boundary(com.morphiqlabs.wavelet.api.BoundaryMode.PERIODIC) // or ZERO_PADDING/SYMMETRIC (SIMD streaming)
        .levels(3)
        .build()) {
    for (double[][] block : blocks) { // AoS [batch][blockSize]
        var out = streaming.processMultiLevel(block);
        double[][][] dpl = out.detailPerLevel();
        double[][] approx = out.finalApprox();
        // ... consume outputs ...
    }
    // Optional: emit a synthetic tail at end-of-stream for non-PERIODIC
    // ZERO_PADDING/SYMMETRIC only. Use accessors to choose a valid tail length:
    int minTail = streaming.getMinFlushTailLength();
    var tail = streaming.flushMultiLevel(minTail);
}
```

## Performance Notes

- SIMD speedups are realized in all modes here; PERIODIC has the least overhead, while ZERO_PADDING/SYMMETRIC add small per-level ring-buffer maintenance.
- For best SIMD utilization, choose batch sizes that are multiples of the platform vector length:
  - Query `MODWTBatchSIMD.getVectorLength()` or `MODWTBatchSIMD.getBatchSIMDInfo()`.
- Use larger block sizes when possible to amortize per-call overhead.

## Correctness and Parity

- Multi-level AoS batch functions are parity-aligned with core `MultiLevelMODWTTransform` (periodic), using identical scaling and à trous upsampling.
- Unit tests validate:
  - Single-level and multi-level streaming parity for ZERO_PADDING and SYMMETRIC vs core whole-signal transforms (concatenated blocks).
  - Single-level parity (Haar, DB4) vs core for AoS batches.
  - Multi-level per-level parity and final approximation vs core.

## Inverse APIs

- Single-level inverse (AoS):
  - `BatchMODWT.inverseSingleLevelAoS(wavelet, approxAoS, detailAoS)` -> `reconstructedAoS`
- Multi-level inverse (AoS):
  - `BatchMODWT.inverseMultiLevelAoS(wavelet, detailPerLevelAoS, finalApproxAoS)` -> `reconstructedAoS`

These use core inverse transforms internally for correctness.

## Tips

- If you need cross-block continuity for non-PERIODIC modes, consider overlap-and-add at the application layer or use larger windows.
- For peak throughput with many levels, pre-size arrays and reuse builders.
- For end-of-stream padding, use `flushSingleLevel(tailLen)` or `flushMultiLevel(tailLen)` to get a final tail under ZERO_PADDING/SYMMETRIC.
- Use `getMinFlushTailLength()` to query the largest universally valid tail length across levels, or
  `getHistoryLengthForLevel(level)` for the per-level maximum in single-level modes.
