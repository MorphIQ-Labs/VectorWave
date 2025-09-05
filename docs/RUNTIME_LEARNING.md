# Runtime Learning in VectorWave

VectorWave includes an opt‑in runtime learning subsystem that observes execution characteristics, builds lightweight performance models, and adapts a few decisions dynamically (e.g., when to parallelize or how to size chunks). This document explains what runtime learning is, how it works technically, and how you can benefit from it in real applications like financial instrument analysis.

## High‑Level Overview

- Purpose: Learn real performance on the exact hardware, JVM, and workload, then feed that back into better decisions (thresholds, chunk sizes, task splits, etc.).
- Scope today: Predictive timing models for core operations (MODWT, convolution, batch) and tuning hooks in extensions (e.g., adaptive parallel thresholds). Opt‑in persistence across runs.
- Zero friction: Disabled by default (no background work, no disk I/O). Enable with simple flags when you want adaptive behavior.

## Quick Start

Enable learning only (no disk I/O):
```bash
mvn -Dvectorwave.perf.calibration=true test
```

Enable learning + persistence to `~/.vectorwave/performance`:
```bash
mvn -Dvectorwave.perf.calibration=true -Dvectorwave.perf.persist=true test
```

Quiet logging or change verbosity (System.Logger + JUL):
```bash
mvn -Djava.util.logging.config.file=$(pwd)/docs/examples/logging.properties \
    -Dvectorwave.log.level=INFO test
```

### Try the Demo

Run the live example that queries predictions to pick sequential vs parallel on the fly:

```bash
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="com.morphiqlabs.demo.RuntimeLearningDemo" \
  -Dvectorwave.perf.calibration=true \
  -Djava.util.logging.config.file=$(pwd)/docs/examples/logging.properties \
  -Dvectorwave.log.level=INFO
```

## Concrete Use Cases

- Financial instrument analysis (batch + streaming):
  - Runtime learning improves decisions about when to parallelize decompositions and reconstructions as volatility changes. For fast markets, larger signals/blocks become common; the learner steers toward parallel paths at better thresholds for the actual CPU/OS/JVM.
  - Streaming denoising can infer optimal block sizes (e.g., for MODWT streaming) by learning the platform’s “knees” (where overhead changes behavior). This yields steadier throughput and lower end‑to‑end latency.

- Batch analytics on multi‑core servers:
  - Accumulate measurements on typical signal lengths. The estimator yields good predictions that you can use to size batches, spread work evenly across workers, or pre‑budget runtime windows.

- Heterogeneous deployments (edge vs. server):
  - On resource‑constrained hardware, disable calibration by default and ship prebuilt models; turn it on selectively in the data center to adapt.

## Technical Deep Dive

### Components

- AdaptivePerformanceEstimator (core)
  - Collects measurements (operation, input size, vectorization flag) from hot paths.
  - Maintains per‑operation models that predict: estimated time, lower/upper bounds, and confidence.
  - Recalibration: On a cadence (by count), kicks off a background calibrator to rebuild models.
  - Persistence (opt‑in): Loads/saves models to `~/.vectorwave/performance/performance_models.dat`.

- PerformanceCalibrator (core)
  - Benchmarks representative sizes and operations (MODWT, convolution, batch) on the current machine.
  - Produces calibrated models with fit coefficients and accuracy metadata.

- PerformanceModel (core)
  - Encapsulates fitted parameters and prediction logic.
  - Applies small domain adjustments (e.g., filter length factor for convolution, wavelet complexity factor for MODWT).

### Data Flow

1) Operation executes (e.g., MODWT forward) → measures elapsed time (ns) → if significant, calls `AdaptivePerformanceEstimator.recordMeasurement(...)`.
2) The estimator updates the corresponding operation model (append measurement; update fit or defer to recalibration).
3) Every N measurements, `checkRecalibration()` schedules a background recalibration (if enabled).
4) When predictions are needed (e.g., deciding parallel vs sequential), callers query the estimator/model.

### Feature Engineering

- Inputs:
  - Size metrics (e.g., signal length, filter length, batch size)
  - Capability flags (e.g., Vector API path used)
  - Operation type (MODWT, Convolution, Batch)

- Adjustments (examples):
  - MODWT wavelet complexity (heuristic factor based on filter length)
  - Convolution filter‑length scaling (sqrt factor)
  - Batch efficiency diminishing returns (log‑based dampener)

### Model Outputs

- `estimatedTime` (ms): point prediction for runtime
- `lowerBound`, `upperBound` (ms): confidence interval bounds
- `confidence` [0..1]: fit quality (roughly the model’s trust in its prediction)

### Recalibration Strategy

- Threshold: Every X measurements (default ~1000) check accuracy/fit and trigger recalibration asynchronously.
- Background thread: A daemon single‑thread executor performs calibration so the hot path remains unaffected.
- Logging: Progress and results go through System.Logger (configurable via JUL).

### Persistence and Privacy

- Persistence is opt‑in: disabled by default; enable via `-Dvectorwave.perf.persist=true`.
- Location: `~/.vectorwave/performance/performance_models.dat`.
- Contents: timing model parameters only; no signal data or application payloads are written.

## API Touchpoints

- Core uses: `MODWTTransform.forward(...)` records timing for significant calls. Other core internals may tap in as needed.
- Extensions hooks: Adaptive/parallel transforms can query predictions to steer their thresholds, chunking, and scheduling policies.

## Best Practices

- CI/Prod Defaults:
  - Disabled unless explicitly enabled to avoid churn and disk writes.
  - Set `-Dvectorwave.log.level=WARNING` (and use JUL config) to keep logs tidy.

- Warming & Stability:
  - Let the JVM warm up before trusting early measurements.
  - Recalibration is periodic and non‑blocking; it will converge as more data arrives.

- Environment Awareness:
  - Performance characteristics vary with CPU, memory, power modes, and JVM. Persisting models per environment provides the best results.

## Example: Financial Instrument Analysis

Suppose you denoise intraday returns and compute multi‑resolution indicators per symbol. Volume and volatility vary, so the “right” thresholds for parallel processing change throughout the day.

1) Enable runtime learning and (optionally) persistence:
```bash
mvn -Dvectorwave.perf.calibration=true -Dvectorwave.perf.persist=true \
    -Djava.util.logging.config.file=$(pwd)/docs/examples/logging.properties \
    -Dvectorwave.log.level=INFO \
    -pl vectorwave-examples -am test
```

2) In your engine, read predictions before choosing a path:
```java
var est = com.morphiqlabs.wavelet.performance.AdaptivePerformanceEstimator.getInstance();
var pred = est.estimateMODWT(signal.length, wavelet.name(), perfInfo.vectorizationEnabled());
boolean useParallel = pred.estimatedTime() > someThreshold && pred.confidence() > 0.6;
```

3) For streaming, adapt block sizes and overlap dynamically using predictions to smooth latency under fast markets.

Result: fewer stalls during regime shifts, better throughput, and predictable SLA for analytics and signal generation.

## Roadmap

- Broader coverage: Integrate predictions into more decision points (e.g., FFT/Direct crossover in CWT, vector vs scalar kernels in edge cases).
- Feedback loops: Combine with the extensions’ `AdaptiveThresholdTuner` to co‑learn both parallelization and algorithmic thresholds.
- Model robustness: Outlier detection and decay strategies for long‑running systems with environment drift.

## Troubleshooting

- No effect? Ensure you’ve enabled calibration: `-Dvectorwave.perf.calibration=true`.
- Too chatty logs? Use `-Dvectorwave.log.level=WARNING` and a JUL config.
- Disk writes? Enable persistence explicitly; otherwise, no models are saved.

## References

- Configuration: `docs/CONFIGURATION.md#performance-models-opt-in`
- Logging: `docs/CONFIGURATION.md#logging`
- Architecture: `docs/ARCHITECTURE.md`
