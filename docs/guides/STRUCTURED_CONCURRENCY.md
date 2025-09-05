# Structured Concurrency Guide

This guide covers VectorWave's structured concurrency implementation, built on Java 24's preview features. Structured concurrency provides guaranteed resource cleanup, automatic cancellation propagation, and simplified concurrent programming patterns.

## Table of Contents

1. [Overview](#overview)
2. [Core Concepts](#core-concepts)
3. [Basic Usage](#basic-usage)
4. [Advanced Patterns](#advanced-patterns)
5. [Error Handling](#error-handling)
6. [Performance Considerations](#performance-considerations)
7. [Migration from Traditional Concurrency](#migration-from-traditional-concurrency)
8. [Troubleshooting](#troubleshooting)

## Overview

Structured concurrency treats groups of related concurrent tasks as a single unit of work, providing:

- **Automatic Resource Management**: All tasks are automatically cleaned up when the scope exits
- **Cancellation Propagation**: If the parent scope is cancelled, all child tasks are cancelled
- **Exception Safety**: Centralized exception handling for all tasks
- **Clear Hierarchy**: Parent-child relationships between tasks

### Key Classes

- **`StructuredExecutor`**: High-level wrapper around Java's `StructuredTaskScope`
- **`StructuredParallelTransform`**: Parallel wavelet operations using structured concurrency
- **`AdaptiveThresholdTuner`**: Machine learning optimization of parallel execution

## Core Concepts

### Scope-based Resource Management

```java
try (var executor = new StructuredExecutor()) {
    // All tasks submitted within this scope
    var future1 = executor.submit(() -> computeTransform(signal1));
    var future2 = executor.submit(() -> computeTransform(signal2));
    
    executor.joinAll(); // Wait for completion
    
    // Process results
    MODWTResult result1 = future1.get();
    MODWTResult result2 = future2.get();
} // Automatic cleanup - all tasks cancelled if not complete
```

### Task Lifecycle

1. **Fork**: Tasks are submitted to the structured scope
2. **Execute**: Tasks run concurrently within the scope
3. **Join**: Wait for all tasks to complete or timeout
4. **Cleanup**: Automatic cancellation of incomplete tasks when scope exits

### Thread Safety Requirements

⚠️ **Important**: Task submission methods (`submit`, `submitAll`, `invokeAll`) must be called from the same thread that created the executor. This is a fundamental requirement of Java's structured concurrency model.

```java
// ✅ CORRECT - Single thread submission
try (var executor = new StructuredExecutor()) {
    for (double[] signal : signals) {
        executor.submit(() -> transform.forward(signal));
    }
    executor.joinAll();
}

// ❌ INCORRECT - Multi-threaded submission will throw WrongThreadException
try (var executor = new StructuredExecutor()) {
    signals.parallelStream().forEach(signal -> 
        executor.submit(() -> transform.forward(signal)) // Will fail!
    );
}
```

## Basic Usage

### Simple Task Submission

```java
import com.morphiqlabs.wavelet.parallel.StructuredExecutor;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;

// Create executor with default configuration
try (var executor = new StructuredExecutor()) {
    // Submit individual tasks
    var future = executor.submit(() -> {
        return transform.forward(signal);
    });
    
    // Wait for completion
    executor.joinAll();
    
    // Get result
    MODWTResult result = future.get();
}
```

### Batch Processing

```java
// Process multiple signals with automatic parallelization
List<double[]> signals = Arrays.asList(signal1, signal2, signal3);

try (var executor = new StructuredExecutor()) {
    List<MODWTResult> results = executor.invokeAll(signals,
        signal -> transform.forward(signal)
    );
    
    // All results are ready
    for (MODWTResult result : results) {
        processResult(result);
    }
}
```

### Progress Monitoring

```java
AtomicInteger completed = new AtomicInteger(0);
AtomicInteger total = new AtomicInteger(signals.size());

try (var executor = new StructuredExecutor()) {
    List<StructuredExecutor.StructuredFuture<MODWTResult>> futures = 
        executor.submitAll(signals, signal -> {
            MODWTResult result = transform.forward(signal);
            int done = completed.incrementAndGet();
            System.out.printf("Progress: %d/%d completed%n", done, total.get());
            return result;
        });
    
    executor.joinAll();
    
    // Collect results
    List<MODWTResult> results = new ArrayList<>();
    for (var future : futures) {
        results.add(future.get());
    }
}
```

## Advanced Patterns

### Timeout Management

```java
// Configure timeout during executor creation
long timeoutMs = 5000; // 5 seconds
try (var executor = new StructuredExecutor(config, timeoutMs)) {
    List<MODWTResult> results = executor.invokeAll(signals,
        signal -> transform.forward(signal)
    );
} catch (CompletionException e) {
    if (e.getMessage().contains("timeout")) {
        System.err.println("Operation timed out");
        // Handle timeout scenario
    }
}
```

### Custom Parallel Configuration

```java
// Configure parallel execution parameters
ParallelConfig config = ParallelConfig.builder()
    .targetCores(8)                    // Use 8 cores
    .minParallelThreshold(512)         // Parallelize if input > 512 elements
    .maxChunkSize(4096)               // Maximum chunk size
    .build();

try (var executor = new StructuredExecutor(config)) {
    // Tasks will use the configured parallel settings
    List<MODWTResult> results = executor.invokeAll(signals,
        signal -> transform.forward(signal)
    );
}
```

### Adaptive Threshold Optimization

```java
// The executor automatically tunes parallel thresholds based on performance
AdaptiveThresholdTuner tuner = new AdaptiveThresholdTuner();

for (int batch = 0; batch < numBatches; batch++) {
    // Get adaptive threshold for current workload
    int threshold = tuner.getAdaptiveThreshold(
        AdaptiveThresholdTuner.OperationType.MODWT_DECOMPOSE,
        signals[0].length,
        1.0 // complexity factor
    );
    
    ParallelConfig adaptiveConfig = ParallelConfig.builder()
        .minParallelThreshold(threshold)
        .build();
    
    long startTime = System.nanoTime();
    
    try (var executor = new StructuredExecutor(adaptiveConfig)) {
        List<MODWTResult> results = executor.invokeAll(signals,
            signal -> transform.forward(signal)
        );
        
        long elapsed = System.nanoTime() - startTime;
        
        // Record performance for future optimization
        tuner.recordMeasurement(
            AdaptiveThresholdTuner.OperationType.MODWT_DECOMPOSE,
            signals[0].length,
            threshold,
            elapsed,
            estimatedSequentialTime
        );
    }
}
```

### High-Level Parallel Transform API

```java
// Use the high-level parallel transform API
StructuredParallelTransform parallelTransform = new StructuredParallelTransform(
    Daubechies.DB4, BoundaryMode.PERIODIC, config);

// Simple batch processing
double[][] signals = loadSignals();
MODWTResult[] results = parallelTransform.forwardBatch(signals);

// With timeout
try {
    MODWTResult[] results = parallelTransform.forwardBatchWithTimeout(signals, 10000);
} catch (StructuredParallelTransform.ComputationException e) {
    System.err.println("Batch processing failed: " + e.getMessage());
}

// With progress monitoring
MODWTResult[] results = parallelTransform.forwardBatchWithProgress(signals,
    (completed, total) -> {
        double percentage = (100.0 * completed) / total;
        System.out.printf("Processing: %.1f%% complete%n", percentage);
    }
);
```

## Error Handling

### Exception Types

Structured concurrency operations can throw several types of exceptions:

```java
try (var executor = new StructuredExecutor(config, timeoutMs)) {
    List<MODWTResult> results = executor.invokeAll(signals,
        signal -> transform.forward(signal)
    );
} catch (ExecutionException e) {
    // Task threw an exception
    Throwable cause = e.getCause();
    System.err.println("Task failed: " + cause.getMessage());
} catch (InterruptedException e) {
    // Thread was interrupted
    Thread.currentThread().interrupt();
    System.err.println("Operation was interrupted");
} catch (CompletionException e) {
    // Timeout or other completion issue
    System.err.println("Operation failed to complete: " + e.getMessage());
} catch (RejectedExecutionException e) {
    // Deadline exceeded before task submission
    System.err.println("Task submission rejected: " + e.getMessage());
}
```

### Task-Specific Error Handling

```java
try (var executor = new StructuredExecutor()) {
    List<StructuredExecutor.StructuredFuture<MODWTResult>> futures = new ArrayList<>();
    
    for (double[] signal : signals) {
        futures.add(executor.submit(() -> {
            try {
                return transform.forward(signal);
            } catch (Exception e) {
                // Log error but don't propagate immediately
                System.err.println("Failed to process signal: " + e.getMessage());
                return null; // or return a default/error result
            }
        }));
    }
    
    executor.joinAll();
    
    // Check individual results
    List<MODWTResult> validResults = new ArrayList<>();
    for (var future : futures) {
        try {
            MODWTResult result = future.get();
            if (result != null) {
                validResults.add(result);
            }
        } catch (ExecutionException e) {
            System.err.println("Task execution failed: " + e.getCause().getMessage());
        }
    }
}
```

### Graceful Degradation

```java
public List<MODWTResult> processWithFallback(List<double[]> signals, long timeoutMs) {
    // Try parallel processing first
    try (var executor = new StructuredExecutor(parallelConfig, timeoutMs)) {
        return executor.invokeAll(signals, signal -> transform.forward(signal));
    } catch (Exception e) {
        // Fall back to sequential processing
        System.err.println("Parallel processing failed, falling back to sequential: " + e.getMessage());
        
        List<MODWTResult> results = new ArrayList<>();
        for (double[] signal : signals) {
            try {
                results.add(transform.forward(signal));
            } catch (Exception ex) {
                System.err.println("Sequential processing also failed for signal: " + ex.getMessage());
                // Could add null or default result
            }
        }
        return results;
    }
}
```

## Performance Considerations

### When to Use Structured Concurrency

✅ **Good Use Cases:**
- Processing multiple independent signals
- Batch operations with similar computational cost
- Operations that can benefit from parallelization
- When you need guaranteed resource cleanup

❌ **Avoid For:**
- Very small datasets (overhead > benefit)
- Operations with significant synchronization requirements
- Sequential algorithms that don't parallelize well
- Single-threaded workloads

### Optimal Configuration

```java
// For CPU-bound tasks (like wavelet transforms)
ParallelConfig cpuConfig = ParallelConfig.builder()
    .targetCores(Runtime.getRuntime().availableProcessors())
    .minParallelThreshold(512)  // Adjust based on signal size
    .maxChunkSize(8192)
    .build();

// For I/O-bound tasks (like file processing)
ParallelConfig ioConfig = ParallelConfig.builder()
    .targetCores(Runtime.getRuntime().availableProcessors() * 2)  // More threads for I/O
    .minParallelThreshold(1)    // Parallelize even small workloads
    .build();
```

### Memory Considerations

```java
// For large datasets, consider processing in batches
List<double[]> allSignals = loadLargeDataset();
int batchSize = 100; // Process 100 signals at a time

List<MODWTResult> allResults = new ArrayList<>();
for (int i = 0; i < allSignals.size(); i += batchSize) {
    List<double[]> batch = allSignals.subList(i, 
        Math.min(i + batchSize, allSignals.size()));
    
    try (var executor = new StructuredExecutor(config)) {
        List<MODWTResult> batchResults = executor.invokeAll(batch,
            signal -> transform.forward(signal)
        );
        allResults.addAll(batchResults);
    }
    
    // Optional: Force garbage collection between batches for large datasets
    if (batch.size() == batchSize) {
        System.gc();
    }
}
```

## Migration from Traditional Concurrency

### From ExecutorService

**Before:**
```java
ExecutorService executor = Executors.newFixedThreadPool(4);
try {
    List<Future<MODWTResult>> futures = new ArrayList<>();
    for (double[] signal : signals) {
        futures.add(executor.submit(() -> transform.forward(signal)));
    }
    
    List<MODWTResult> results = new ArrayList<>();
    for (Future<MODWTResult> future : futures) {
        results.add(future.get());
    }
} finally {
    executor.shutdown();
    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
        executor.shutdownNow();
    }
}
```

**After:**
```java
try (var executor = new StructuredExecutor()) {
    List<MODWTResult> results = executor.invokeAll(signals,
        signal -> transform.forward(signal)
    );
}
```

### From CompletableFuture

**Before:**
```java
List<CompletableFuture<MODWTResult>> futures = signals.stream()
    .map(signal -> CompletableFuture.supplyAsync(() -> transform.forward(signal)))
    .collect(Collectors.toList());

List<MODWTResult> results = futures.stream()
    .map(CompletableFuture::join)
    .collect(Collectors.toList());
```

**After:**
```java
try (var executor = new StructuredExecutor()) {
    List<MODWTResult> results = executor.invokeAll(signals,
        signal -> transform.forward(signal)
    );
}
```

### From Parallel Streams

**Before:**
```java
List<MODWTResult> results = signals.parallelStream()
    .map(signal -> transform.forward(signal))
    .collect(Collectors.toList());
```

**After:**
```java
try (var executor = new StructuredExecutor()) {
    List<MODWTResult> results = executor.invokeAll(signals,
        signal -> transform.forward(signal)
    );
}
```

**Benefits of Migration:**
- Guaranteed resource cleanup
- Better error handling and propagation
- Configurable timeout and cancellation
- Adaptive performance optimization
- Clear task lifecycle management

## Troubleshooting

### Common Issues

#### WrongThreadException

**Problem:** Tasks must be submitted from the thread that created the executor.

```java
// ❌ This will fail
try (var executor = new StructuredExecutor()) {
    ExecutorService threadPool = Executors.newFixedThreadPool(4);
    threadPool.submit(() -> {
        executor.submit(() -> processSignal(signal)); // WrongThreadException!
    });
}
```

**Solution:** Submit all tasks from the main thread:

```java
// ✅ This works
try (var executor = new StructuredExecutor()) {
    for (double[] signal : signals) {
        executor.submit(() -> processSignal(signal));
    }
    executor.joinAll();
}
```

#### IllegalStateException: Task not yet complete

**Problem:** Trying to get results before calling `joinAll()`.

```java
// ❌ This will fail
try (var executor = new StructuredExecutor()) {
    var future = executor.submit(() -> transform.forward(signal));
    MODWTResult result = future.get(); // IllegalStateException!
}
```

**Solution:** Always call `joinAll()` first:

```java
// ✅ This works
try (var executor = new StructuredExecutor()) {
    var future = executor.submit(() -> transform.forward(signal));
    executor.joinAll();
    MODWTResult result = future.get();
}
```

#### Performance Issues

**Problem:** Parallel execution is slower than sequential.

**Diagnosis:**
```java
// Enable adaptive threshold logging
System.setProperty("debug.tuner", "true");

AdaptiveThresholdTuner tuner = new AdaptiveThresholdTuner();
// Monitor threshold recommendations
```

**Solutions:**
- Increase `minParallelThreshold` for small datasets
- Use `AdaptiveThresholdTuner` for automatic optimization
- Consider sequential processing for small workloads
- Profile with JMH benchmarks

### Debug Mode

Enable debug output to understand executor behavior:

```java
// Set system property for debug output
System.setProperty("debug.executor", "true");

try (var executor = new StructuredExecutor(config)) {
    // Debug output will show:
    // - Task submission details
    // - Execution timing
    // - Resource cleanup
    List<MODWTResult> results = executor.invokeAll(signals,
        signal -> transform.forward(signal)
    );
}
```

### Performance Monitoring

```java
// Monitor task execution metrics
try (var executor = new StructuredExecutor(config)) {
    long startTime = System.nanoTime();
    
    List<MODWTResult> results = executor.invokeAll(signals,
        signal -> transform.forward(signal)
    );
    
    long elapsed = System.nanoTime() - startTime;
    double tasksPerSecond = (signals.size() * 1e9) / elapsed;
    
    System.out.printf("Processed %d tasks in %.2f ms (%.1f tasks/sec)%n",
        signals.size(), elapsed / 1e6, tasksPerSecond);
}
```

## Best Practices

1. **Use try-with-resources**: Always use structured executors in try-with-resources blocks
2. **Single-thread submission**: Submit all tasks from the thread that created the executor
3. **Call joinAll()**: Always call `joinAll()` before accessing results
4. **Handle exceptions**: Properly handle `ExecutionException`, `InterruptedException`, and `CompletionException`
5. **Configure appropriately**: Use `ParallelConfig` to tune performance for your workload
6. **Monitor performance**: Use `AdaptiveThresholdTuner` for automatic optimization
7. **Consider batch size**: For large datasets, process in manageable batches
8. **Profile regularly**: Use JMH benchmarks to verify performance gains

---

For more information, see:
- [API Reference](../API.md)
- [Performance Guide](../PERFORMANCE.md)
- [Migration Guide](MIGRATION_GUIDE.md)
## SIMD Integration (Extensions)

Vector API–based SIMD acceleration is available via the optional `vectorwave-extensions` module (Java 24 + incubator). The concurrency patterns in this guide apply to both core (scalar) and extensions (SIMD) modules. To enable SIMD:

```
--add-modules jdk.incubator.vector --enable-preview
```
