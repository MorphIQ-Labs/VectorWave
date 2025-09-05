# SIMD Integration (Vector API)

This page consolidates everything you need to know to use VectorWave with SIMD acceleration.

## Where SIMD Lives

- Core (`vectorwave-core`): Portable scalar Java 21 implementation.
- Extensions (`vectorwave-extensions`): Optional Java 24 module that provides SIMD/Vector API acceleration and high‑throughput facades (batch AoS, batch streaming).

Core works everywhere; add the extensions module when you want hardware SIMD acceleration.

## Requirements

- JDK: Java 24 (or compatible) for the extensions module (uses incubator Vector API).
- JVM flags at compile/run time:
  - `--add-modules jdk.incubator.vector`
  - `--enable-preview`
- Recommended: GraalVM 24.0.2 for +10–20% throughput.

## Enabling SIMD

1) Add the extensions dependency

```xml
<dependency>
  <groupId>com.morphiqlabs</groupId>
  <artifactId>vectorwave-extensions</artifactId>
  <version>1.0.0</version>
</dependency>
```

2) Pass Vector API flags when running

```bash
java --add-modules jdk.incubator.vector --enable-preview -jar yourapp.jar
```

With Maven Surefire/Exec you can supply:

```xml
<argLine>
  --add-modules=jdk.incubator.vector
  --enable-preview
</argLine>
```

## What You Get (Extensions)

- Batch MODWT (AoS facade, SoA under the hood): `com.morphiqlabs.wavelet.modwt.BatchMODWT`
- Batch Streaming MODWT (AoS facade): `com.morphiqlabs.wavelet.modwt.BatchStreamingMODWT`
- SIMD kernels and utilities: `com.morphiqlabs.wavelet.internal.VectorOps`, batch SIMD in `BatchSIMDMODWT`
- Optimizations: reduced temporary allocations, SoA layouts, FMA in hot loops, thread‑local scratch buffers

## Quick Usage Examples

Batch (AoS facade):

```java
double[][] signals = ...; // [batch][length]
int levels = 3;
var out = com.morphiqlabs.wavelet.modwt.BatchMODWT
    .multiLevelAoS(com.morphiqlabs.wavelet.api.Daubechies.DB4, signals, levels);
double[][][] dpl = out.detailPerLevel();
double[][] approx = out.finalApprox();
```

Batch Streaming (AoS facade):

```java
try (var streaming = new com.morphiqlabs.wavelet.modwt.BatchStreamingMODWT.Builder()
         .wavelet(com.morphiqlabs.wavelet.api.Daubechies.DB4)
         .boundary(com.morphiqlabs.wavelet.api.BoundaryMode.PERIODIC)
         .levels(3)
         .build()) {
  var block = nextBlock(); // [batch][blockLen]
  var res = streaming.processMultiLevel(block);
}
```

## Detecting SIMD at Runtime

The simplest check is attempting to access the preferred species:

```java
boolean simdAvailable;
try {
  simdAvailable = jdk.incubator.vector.DoubleVector.SPECIES_PREFERRED.length() > 1;
} catch (Throwable t) {
  simdAvailable = false;
}
``;

Core API also exposes a performance hint (always scalar in core):

```java
boolean vectorization = com.morphiqlabs.wavelet.WaveletOperations
  .getPerformanceInfo().vectorizationEnabled();
```

## Benchmarks and Validation

- JMH microbenchmarks (allocation/FMA):

```bash
mvn -q -pl vectorwave-examples -am exec:java \
  -Dexec.mainClass=org.openjdk.jmh.Main \
  -Dexec.args="com.morphiqlabs.benchmark.VectorOpsAllocationBenchmark -prof gc -f 1 -wi 5 -i 10"
```

Expect near‑zero allocations/op for vector paths and higher throughput than scalar.

## Related Documentation

- Guides
  - Batch Processing: docs/guides/BATCH_PROCESSING.md
  - Batch Streaming (Extensions): docs/guides/BATCH_STREAMING.md
  - Streaming: docs/guides/STREAMING.md
  - Performance Tuning: docs/guides/PERFORMANCE_TUNING.md
  - Developer Guide: docs/guides/DEVELOPER_GUIDE.md

- Performance
  - Performance Guide: docs/PERFORMANCE.md
  - Optimizations: docs/performance/OPTIMIZATIONS.md

- Technical Specs
  - Vector API Compilation: docs/technical-specs/VECTOR_API_COMPILATION.md
  - Vector API Fallback: docs/technical-specs/VECTOR_API_FALLBACK.md
  - FFT Algorithm Notes: docs/technical-specs/FFT_ALGORITHM_ANALYSIS.md

- Examples
  - Examples README: vectorwave-examples/README.md
  - BatchStreamingFlushExample: vectorwave-examples/src/main/java/com/morphiqlabs/examples/BatchStreamingFlushExample.java
  - VectorOpsAllocationBenchmark: vectorwave-benchmarks/src/main/java/com/morphiqlabs/benchmark/VectorOpsAllocationBenchmark.java

## Notes and Caveats

- Vector API is incubator; interfaces may change across JDKs.
- Always pass `--enable-preview` and module flags in dev/test/prod when using extensions.
- Core and extensions maintain strict parity of results; SIMD affects speed, not correctness.
