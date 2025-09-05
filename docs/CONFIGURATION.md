# VectorWave Configuration Guide

VectorWave provides several configurable parameters to optimize performance for different environments and use cases.

## Parallelization Thresholds

### 1. VectorWaveSwtAdapter Parallel Threshold

Controls when the SWT adapter switches to parallel processing for multi-level decomposition.

**Default:** 4096

Recommended usage (core adapter; lazy caches + private executor):
```java
// Prefer try-with-resources; enable parallel and set threshold
try (VectorWaveSwtAdapter adapter = new VectorWaveSwtAdapter(
        Daubechies.DB4, BoundaryMode.PERIODIC,
        /* enableParallel */ true,
        /* parallelThreshold */ 2048)) {
    var result = adapter.forward(signal, 4);
    double[] recon = adapter.inverse(result);
}
```
Notes:
- Analysis filters are cached lazily per adapter instance.
- A dedicated executor is initialized on first large transform and shut down on `close()`.

### 2. FFT Vector Threshold (OptimizedFFT)

Controls when FFT operations use vectorized SIMD instructions vs scalar fallback.

**Default:** 256

**System Property:** `-Dvectorwave.fft.vector.threshold=512`

**Environment Variable:** `VECTORWAVE_FFT_VECTOR_THRESHOLD=512`

**Example Usage:**
```bash
# Via system property
java -Dvectorwave.fft.vector.threshold=512 -cp ... MyApp

# Via environment variable
export VECTORWAVE_FFT_VECTOR_THRESHOLD=512
java -cp ... MyApp
```

### 3. CWT FFT Threshold

Controls when CWT transforms use FFT acceleration vs direct convolution.

**Default:** 64

**System Property:** `-Dvectorwave.cwt.fft.threshold=128`

**Environment Variable:** `VECTORWAVE_CWT_FFT_THRESHOLD=128`

**Example Usage:**
```bash
# Via system property  
java -Dvectorwave.cwt.fft.threshold=128 -cp ... MyApp

# Via environment variable
export VECTORWAVE_CWT_FFT_THRESHOLD=128
java -cp ... MyApp
```

### 4. MODWT FFT Heuristic

Controls when MODWT periodic convolution switches to FFT-based circular convolution.

- Decision helper: `com.morphiqlabs.wavelet.util.FftHeuristics.shouldUseModwtFFT(N, L)`
- Used by: `WaveletOperations.circularConvolveMODWT(..)`

Defaults:
- `minN = 1024` (require reasonably large signals)
- `minFilterToSignalRatio = 1/8` (use FFT when `L > N * 1/8`)

Override via system properties:
- `-Dvectorwave.fft.modwt.minN=2048`
- `-Dvectorwave.fft.modwt.minFilterToSignalRatio=0.2`

Example:
```bash
# Favor scalar path more often
java -Dvectorwave.fft.modwt.minN=4096 \
     -Dvectorwave.fft.modwt.minFilterToSignalRatio=0.25 \
     -cp ... MyApp
```

Programmatic tuning (e.g., in benchmarks/tests):
```java
FftHeuristics.setMinNForModwtFFT(512);
FftHeuristics.setMinFilterToSignalRatio(0.25);
// ... run workload ...
FftHeuristics.resetToDefaults();
```

### 5. Stockham FFT (Opt-in)

Enable a Stockham autosort radix-2 FFT kernel in the core FFT implementation.

- System property: `-Dvectorwave.fft.stockham=true`
- Default: `false` (uses the stable Cooley–Tukey path)

Notes:
- Parity: The Stockham kernel is validated against the default FFT with strict tests (round-trip and randomized inputs) and should match within tight tolerances.
- Use cases: Mid-to-large power-of-two sizes where improved cache locality can reduce runtime; FFT-heavy workflows (e.g., MODWT periodic FFT path) may benefit.
- Interactions: Real-valued RFFT (`com.morphiqlabs.wavelet.fft.CoreFFT#rfft`) calls the complex FFT internally; enabling Stockham affects those internal FFTs as well.

### 6. Stockham Twiddle Cache (Opt-in, default on)

Enable a global per-size twiddle-factor cache used by the Stockham kernel to reduce trigonometric overhead.

- System property: `-Dvectorwave.fft.twiddleCache.enabled=true|false` (default: `true`)
- Range bounds:
  - `-Dvectorwave.fft.twiddleCache.minN=1024` (default: 1024)
  - `-Dvectorwave.fft.twiddleCache.maxN=65536` (default: 65536)

Notes:
- Applies only when `-Dvectorwave.fft.stockham=true`.
- For sizes within `[minN, maxN]` and power-of-two N, a per-stage twiddle table is cached and reused across calls.
- For sizes outside range or when disabled, per-call thread-local twiddles are computed to avoid allocations.

## ParallelConfig Options

The ParallelConfig class provides comprehensive control over parallel execution:

```java
ParallelConfig config = new ParallelConfig.Builder()
    .parallelThreshold(1024)           // Signal size threshold for parallelization
    .parallelismLevel(8)               // Number of parallel threads
    .useVirtualThreads(true)           // Use virtual threads (Java 21+)
    .enableMetrics(true)               // Enable performance metrics
    .enableParallelThresholding(true)  // Use parallel coefficient thresholding
    .chunkSize(512)                    // Chunk size for parallel processing
    .build();
```

## Configuration Priority

For system properties and environment variables:

1. **System Property** (highest priority)
2. **Environment Variable** (medium priority)  
3. **Default Value** (lowest priority)

## Performance Tuning Guidelines

### Small Embedded Systems
```java
// Reduce parallelization to save memory
ParallelConfig embedded = new ParallelConfig.Builder()
    .parallelThreshold(8192)      // Higher threshold - less parallelization
    .parallelismLevel(2)          // Fewer threads
    .useVirtualThreads(false)     // Use platform threads
    .enableMetrics(false)         // Disable overhead
    .build();
```

### High-Performance Servers
```java  
// Maximize parallelization for throughput
ParallelConfig server = new ParallelConfig.Builder()
    .parallelThreshold(256)       // Lower threshold - more parallelization
    .parallelismLevel(16)         // More threads
    .useVirtualThreads(true)      // Use virtual threads for I/O
    .enableMetrics(true)          // Enable monitoring
    .chunkSize(1024)              // Larger chunks
    .build();
```

### Desktop Applications
```java
// Balanced configuration
ParallelConfig desktop = new ParallelConfig.Builder()
    .parallelThreshold(1024)      // Standard threshold
    .parallelismLevel(Runtime.getRuntime().availableProcessors())
    .useVirtualThreads(true)      // Modern Java features
    .enableMetrics(false)         // Reduce overhead
    .build();
```

## Environment Variable Examples

```bash
# Configure all thresholds via environment
export VECTORWAVE_FFT_VECTOR_THRESHOLD=512
export VECTORWAVE_CWT_FFT_THRESHOLD=128

# Run application
java --add-modules jdk.incubator.vector -cp ... MyApp
```

## Monitoring and Diagnostics

Enable metrics to monitor parallelization effectiveness:

```java
ParallelConfig config = new ParallelConfig.Builder()
    .enableMetrics(true)
    .build();

// Use the config...

// Check statistics
ParallelConfig.ExecutionStats stats = config.getStats();
System.out.printf("Parallel executions: %d, Sequential: %d%n", 
    stats.getParallelExecutions(), stats.getSequentialExecutions());
```

## Logging

VectorWave uses Java's `System.Logger` for internal diagnostics. By default, logging is conservative.

- Configure level via system property or environment variable:
  - `-Dvectorwave.log.level=WARNING` (preferred)
  - `VECTORWAVE_LOG_LEVEL=WARNING`
- Bridge `System.Logger` to your logging stack if desired (e.g., JUL → SLF4J).

Examples:
```bash
# Quiet CI runs
mvn -Dvectorwave.log.level=WARNING test

# Local debugging
mvn -Dvectorwave.log.level=INFO test
```

### JUL (java.util.logging) configuration

`System.Logger` routes to JUL by default. You can configure handlers and levels with a standard `logging.properties` file.

Minimal `logging.properties`:
```properties
# Global log level
.level=INFO

# Console handler
handlers= java.util.logging.ConsoleHandler
java.util.logging.ConsoleHandler.level=INFO
java.util.logging.ConsoleHandler.formatter=java.util.logging.SimpleFormatter

# Reduce VectorWave noise in CI
com.morphiqlabs.wavelet.level=WARNING
```

Run with explicit JUL config:
```bash
mvn -Djava.util.logging.config.file=$(pwd)/logging.properties test
# or
java -Djava.util.logging.config.file=./logging.properties -cp ... MyApp
```

An example config is provided at `docs/examples/logging.properties`:
```bash
mvn -Djava.util.logging.config.file=$(pwd)/docs/examples/logging.properties \
    -Dvectorwave.log.level=WARNING test
```

Maven Surefire example:
```xml
<plugin>
  <groupId>org.apache.maven.plugins</groupId>
  <artifactId>maven-surefire-plugin</artifactId>
  <configuration>
    <systemPropertyVariables>
      <java.util.logging.config.file>${project.basedir}/logging.properties</java.util.logging.config.file>
      <vectorwave.log.level>WARNING</vectorwave.log.level>
    </systemPropertyVariables>
  </configuration>
  <!-- version omitted; use your project’s configured version -->
  <version>3.2.5</version>
  </plugin>
```

Programmatic JUL setup (optional):
```java
import java.nio.file.*;
import java.util.logging.LogManager;

try (var in = Files.newInputStream(Paths.get("logging.properties"))) {
    LogManager.getLogManager().readConfiguration(in);
}
```

## Performance Models (Opt‑In)

VectorWave includes an adaptive performance estimator that can learn from runtime measurements and optionally persist models. To keep the library quiet and avoid home‑directory writes by default, calibration and persistence are disabled unless explicitly enabled.

- Enable calibration (learn from measurements):
  - System property: `-Dvectorwave.perf.calibration=true`
  - Environment variable: `VECTORWAVE_PERF_CALIBRATION=true`
- Enable persistence (load/save models under `~/.vectorwave/performance`):
  - System property: `-Dvectorwave.perf.persist=true`
  - Environment variable: `VECTORWAVE_PERF_PERSIST=true`

Examples:
```bash
# Enable calibration only (no disk I/O)
mvn -Dvectorwave.perf.calibration=true test

# Enable calibration + persistence
mvn -Dvectorwave.perf.calibration=true -Dvectorwave.perf.persist=true test

# With JUL logging configured and quiet INFO level
mvn -Dvectorwave.perf.calibration=true \
    -Dvectorwave.perf.persist=true \
    -Djava.util.logging.config.file=$(pwd)/docs/examples/logging.properties \
    -Dvectorwave.log.level=INFO \
    test
```

## Best Practices

1. **Profile First:** Use JMH benchmarks to measure actual performance impact
2. **Environment-Specific:** Tune thresholds for your specific hardware
3. **Monitor Metrics:** Enable metrics in development to understand behavior
4. **Conservative Defaults:** Start with higher thresholds and lower gradually
5. **Test Thoroughly:** Verify correctness across all configuration combinations
