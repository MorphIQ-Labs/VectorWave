# Changelog

All notable changes to this project are documented here. This project follows
Keep a Changelog and Semantic Versioning.

## [1.0.0] - 2025-09-05

Baseline 1.0.0 release with a stable scalar core, a separate SIMD extensions
module, and examples. Core targets Java 21; extensions target Java 24 with
preview (Vector API).

### Added
- Core (vectorwave-core)
  - MODWT: single-level and multi-level transforms with PERIODIC, SYMMETRIC,
    and ZERO_PADDING boundary modes; SWT adapter with lifecycle management.
  - Streaming transforms and denoising helpers built on MODWT.
  - Wavelet families: Haar, Daubechies (DB2–DB38), Symlets, Coiflets,
    Biorthogonal, and specialized financial wavelets.
  - Error handling with ErrorCode + ErrorContext; zero-deps logging via
    JDK System.Logger.
  - CoreFFT: pure Java 21 FFT with Stockham and real-optimized RFFT options.
  - CWT (Experimental): continuous transforms and finance-specific wavelets;
    package-level experimental banner and annotations.

- Extensions (vectorwave-extensions)
  - Java 24 + preview Vector API acceleration for batch/streaming MODWT.
  - SIMD kernels (Haar/DB4/general), masked tails, memory layout utilities.
  - Parallel orchestration (structured concurrency) and configuration knobs.

- Examples (vectorwave-examples)
  - Demos, benchmarks, and documentation tie-ins on the classpath.

<!-- Initial release: no prior versions to compare; no Changed/Fixed sections. -->

### Build/CI
- Maven Toolchains: core builds/tests on JDK 21; extensions on JDK 24 preview.
- Javadocs skipped by default; generated/aggregated only under JDK 24.
- CI jobs for core, extensions, examples, integration, code quality, and docs.

### Licensing
- License alignment: repository LICENSE and POMs set to GPL-3.0.

### Notes
- CWT APIs are experimental and may change in minor releases; pin exact versions
  in production if you depend on them.

[1.0.0]: https://github.com/MorphIQ-Labs/VectorWave/releases/tag/v1.0.0
