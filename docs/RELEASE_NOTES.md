# VectorWave 1.0.0 — Release Notes

Date: 2025-09-05

These notes summarize the 1.0.0 baseline release. This is the first stable release with a Java 21 scalar core and an optional Java 24 extensions module for SIMD acceleration.

## Highlights
- Core (Java 21) with stable APIs for MODWT/SWT, streaming, denoising, and rich wavelet families.
- Extensions (Java 24 + preview) with Vector API SIMD acceleration and structured concurrency.
- Clean JPMS separation: core is incubator-free; SIMD lives in extensions.
- Doclint-clean Javadocs and CI pipelines using Maven Toolchains (JDK 21/24).

## Modules
- `vectorwave-core` (Java 21)
  - MODWT single-level and multi-level transforms with PERIODIC, SYMMETRIC, and ZERO_PADDING.
  - SWT Adapter built on MODWT with clear resource lifecycle via `AutoCloseable`.
  - Streaming transforms and denoising helpers.
  - Wavelet families: Haar, Daubechies (DB2–DB38), Symlets, Coiflets, Biorthogonal, finance-specific.
  - Uses CoreFFT from `vectorwave-fft` (pure Java 21), System.Logger based diagnostics, zero external deps.
- `vectorwave-extensions` (Java 24 + preview)
  - SIMD-accelerated batch/streaming MODWT (Vector API), memory/layout utils, parallel orchestration.
- `vectorwave-fft` (Java 21)
  - Standalone FFT utilities (`com.morphiqlabs.wavelet.fft.CoreFFT`) consumed by core.
- `vectorwave-examples`
  - Demos and docs tie-ins (no JMH; named module).
- `vectorwave-benchmarks`
  - Non‑modular JMH suite (classpath), runs via `org.openjdk.jmh.Main`.

## Requirements
- Java 21+ for core; Java 24 (preview) for extensions/examples.
- Maven 3.6+.
- CI and Javadocs use Maven Toolchains to select JDK 21 and 24.

## Installation

Maven (Core only):
```xml
<dependency>
  <groupId>com.morphiqlabs</groupId>
  <artifactId>vectorwave-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

Maven (Core + Extensions):
```xml
<dependency>
  <groupId>com.morphiqlabs</groupId>
  <artifactId>vectorwave-core</artifactId>
  <version>1.0.0</version>
</dependency>
<dependency>
  <groupId>com.morphiqlabs</groupId>
  <artifactId>vectorwave-extensions</artifactId>
  <version>1.0.0</version>
</dependency>
```

Gradle (Kotlin DSL):
```kotlin
implementation("com.morphiqlabs:vectorwave-core:1.0.0")
// Optional Vector API acceleration (Java 24 preview)
implementation("com.morphiqlabs:vectorwave-extensions:1.0.0")
```

## Quick Start

```java
import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;

double[] signal = new double[777];
MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
var result = transform.forward(signal);
double[] reconstructed = transform.inverse(result);
```

## Documentation
- Javadocs (aggregated): https://morphiq-labs.github.io/VectorWave/
- Changelog: ../CHANGELOG.md
- Contributing: CONTRIBUTING.md
- Boundary Modes Matrix and level interpretation guidance are in README.

## Build & CI Summary
- Toolchains: core builds/tests on JDK 21; extensions on JDK 24 (preview).
- Javadocs: skipped by default; aggregated under JDK 24 with doclint enabled.
- Code quality: compilation warnings filtered to allow expected preview/Unsafe messages only.

## Known Limitations
- CWT is experimental: API and numerics may evolve in minor releases. Use `@Experimental` types with version pinning if used in production.
- SYMMETRIC inverse alignment uses a small wavelet/level dependent heuristic (±τj) to reduce boundary error; prefer PERIODIC when strict exactness is required.

## Compatibility & Stability
- Semantic Versioning starting at 1.0.0.
- Public core APIs (MODWT/SWT) are stable. Experimental namespaces are clearly marked and may change.

## Reporting Issues
- GitHub Issues: https://github.com/MorphIQ-Labs/VectorWave/issues
- Please include Java version, OS/arch, Maven output (with `-e -X` if build-related), and minimal reproducible examples.
