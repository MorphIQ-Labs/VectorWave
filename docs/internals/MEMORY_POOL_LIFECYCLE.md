# Memory Pool Lifecycle Management Guide

## Overview

VectorWave provides several memory pool implementations to reduce garbage collection pressure and improve performance. This guide covers best practices for managing memory pool lifecycles, thread safety considerations, and performance implications.

## Memory Pool Types

### 1. MemoryPool (Standard Java Arrays)
- **Use Case**: General-purpose array pooling for wavelet transforms
- **Thread Safety**: Fully thread-safe using ConcurrentHashMap
- **Lifecycle**: Manual management required

### 2. AlignedMemoryPool (SIMD-Aligned Arrays)
- **Use Case**: High-performance SIMD operations
- **Thread Safety**: Thread-safe with alignment guarantees
- **Lifecycle**: Manual with alignment preservation

### 3. FFMMemoryPool (Foreign Memory)
- **Use Case**: Zero-copy operations, native interop
- **Thread Safety**: Thread-safe with Arena-based lifecycle
- **Lifecycle**: Automatic with try-with-resources

### 4. CWTMemoryPool (CWT-Specific)
- **Use Case**: Optimized for CWT coefficient matrices
- **Thread Safety**: Thread-safe for concurrent transforms
- **Lifecycle**: Tied to CWT operations

## Lifecycle Best Practices

### 1. Pool Creation and Initialization

```java
// Short-lived operations (recommended)
try (FFMMemoryPool pool = new FFMMemoryPool()) {
    // Pool is automatically closed when done
    performOperations(pool);
}

// Long-lived application pools
public class Application {
    private final MemoryPool globalPool = new MemoryPool();
    
    public Application() {
        // Configure pool at startup
        globalPool.setMaxArraysPerSize(20);
    }
    
    public void shutdown() {
        // Clear pool on shutdown
        globalPool.clear();
    }
}

// Thread-local pools for isolation
ThreadLocal<MemoryPool> threadLocalPool = ThreadLocal.withInitial(() -> {
    MemoryPool pool = new MemoryPool();
    pool.setMaxArraysPerSize(10);
    return pool;
});
```

### 2. When to Clear Pools

#### Clear Immediately After:
- **Phase Transitions**: Between different processing phases
- **Memory Pressure**: When approaching memory limits
- **Data Sensitivity**: After processing sensitive financial data

```java
// Example: Phase transition
public void processDataset(Dataset data) {
    MemoryPool pool = new MemoryPool();
    
    // Phase 1: Preprocessing
    preprocessData(data, pool);
    pool.clear(); // Clear between phases
    
    // Phase 2: Transform
    transformData(data, pool);
    pool.clear(); // Clear between phases
    
    // Phase 3: Analysis
    analyzeData(data, pool);
    pool.clear(); // Final cleanup
}
```

#### Periodic Clearing:
```java
public class StreamProcessor {
    private final MemoryPool pool = new MemoryPool();
    private final ScheduledExecutorService cleaner = 
        Executors.newSingleThreadScheduledExecutor();
    
    public StreamProcessor() {
        // Clear pool every 5 minutes to prevent unbounded growth
        cleaner.scheduleAtFixedRate(
            () -> pool.clear(),
            5, 5, TimeUnit.MINUTES
        );
    }
}
```

### 3. Thread Safety Patterns

#### Shared Pool Pattern
```java
// Single pool shared across threads (safe)
public class SharedPoolService {
    private final MemoryPool sharedPool = new MemoryPool();
    
    public void processInParallel(List<Signal> signals) {
        signals.parallelStream().forEach(signal -> {
            double[] workspace = sharedPool.borrowArray(signal.length());
            try {
                processSignal(signal, workspace);
            } finally {
                sharedPool.returnArray(workspace);
            }
        });
    }
}
```

#### Pool-per-Thread Pattern
```java
// Separate pool per thread (better for high contention)
public class ThreadLocalPoolService {
    private final ThreadLocal<MemoryPool> pools = 
        ThreadLocal.withInitial(MemoryPool::new);
    
    public void processSignal(Signal signal) {
        MemoryPool pool = pools.get();
        double[] workspace = pool.borrowArray(signal.length());
        try {
            processSignal(signal, workspace);
        } finally {
            pool.returnArray(workspace);
        }
    }
    
    public void cleanup() {
        pools.remove(); // Important: prevent memory leaks
    }
}
```

#### Scoped Pool Pattern (FFM)
```java
// Scoped lifetime with automatic cleanup
public TransformResult processWithScope(double[] signal) {
    return FFMMemoryPool.withScope(pool -> {
        FFMWaveletTransform transform = new FFMWaveletTransform(wavelet, pool);
        return transform.forward(signal);
    }); // Pool automatically closed here
}
```

### 4. Performance Considerations

#### Pool Sizing
```java
public class PoolSizingStrategy {
    public static MemoryPool createOptimalPool(int expectedConcurrency, 
                                               int typicalArraySize) {
        MemoryPool pool = new MemoryPool();
        
        // Size based on concurrency and working set
        int maxArraysPerSize = Math.max(10, expectedConcurrency * 2);
        pool.setMaxArraysPerSize(maxArraysPerSize);
        
        // Pre-warm pool for common sizes
        for (int i = 0; i < expectedConcurrency; i++) {
            double[] array = pool.borrowArray(typicalArraySize);
            pool.returnArray(array);
        }
        
        return pool;
    }
}
```

#### Memory Usage Monitoring
```java
import java.util.logging.Logger;

public class PoolMonitor {
    private static final Logger LOGGER = Logger.getLogger(PoolMonitor.class.getName());
    private final MemoryPool pool;
    private final long maxMemoryBytes;
    
    public PoolMonitor(MemoryPool pool, long maxMemoryBytes) {
        this.pool = pool;
        this.maxMemoryBytes = maxMemoryBytes;
    }
    
    public void checkAndClearIfNeeded() {
        long estimatedUsage = estimateMemoryUsage();
        if (estimatedUsage > maxMemoryBytes * 0.8) {
            LOGGER.warning("Memory pool using " + estimatedUsage + " bytes, clearing");
            pool.clear();
        }
    }
    
    private long estimateMemoryUsage() {
        // Estimate based on pool statistics
        return pool.getTotalPooledCount() * 8 * 1024; // Rough estimate
    }
}
```

## Common Patterns and Anti-Patterns

### ✅ Good Patterns

#### 1. Borrow-Return in Try-Finally
```java
double[] array = pool.borrowArray(size);
try {
    // Use array
} finally {
    pool.returnArray(array); // Always return
}
```

#### 2. Scoped Pools for Batch Operations
```java
public void processBatch(List<Signal> batch) {
    try (FFMMemoryPool batchPool = new FFMMemoryPool()) {
        batch.forEach(signal -> processWithPool(signal, batchPool));
    } // Pool cleaned up automatically
}
```

#### 3. Pool Metrics for Production
```java
import org.springframework.scheduling.annotation.Scheduled;
import io.micrometer.core.instrument.MeterRegistry;

public class PoolMetricsReporter {
    private final MeterRegistry metricsRegistry;
    private final MemoryPool pool;
    
    public PoolMetricsReporter(MeterRegistry metricsRegistry, MemoryPool pool) {
        this.metricsRegistry = metricsRegistry;
        this.pool = pool;
    }
    
    @Scheduled(fixedDelay = 60000)
    public void reportPoolMetrics() {
        metricsRegistry.gauge("memory.pool.hit.rate", pool, MemoryPool::getHitRate);
        metricsRegistry.gauge("memory.pool.size", pool, MemoryPool::getTotalPooledCount);
    }
}
```

### ❌ Anti-Patterns to Avoid

#### 1. Forgetting to Return Arrays
```java
// BAD: Memory leak
double[] array = pool.borrowArray(size);
processArray(array);
// Forgot to return!
```

#### 2. Returning Modified Arrays Without Clearing
```java
// BAD: Data leak
double[] sensitiveData = pool.borrowArray(size);
processSensitiveData(sensitiveData);
pool.returnArray(sensitiveData); // Still contains sensitive data!
```

#### 3. Unbounded Pool Growth
```java
// BAD: No size limits
MemoryPool pool = new MemoryPool();
// Never set maxArraysPerSize or clear
```

#### 4. Mixing Pools from Different Arenas (FFM)
```java
// BAD: Will throw exception
FFMMemoryPool pool1 = new FFMMemoryPool();
FFMMemoryPool pool2 = new FFMMemoryPool();
MemorySegment seg = pool1.acquire(100);
pool2.release(seg); // Error: different arenas
```

## Lifecycle Recommendations by Use Case

### 1. Real-Time Signal Processing
- Use thread-local pools to avoid contention
- Clear pools between processing windows
- Monitor pool size and clear proactively

### 2. Batch Financial Analysis
- Create pool per batch
- Use try-with-resources for automatic cleanup
- Clear sensitive data immediately

### 3. Long-Running Services
- Implement periodic clearing
- Monitor memory usage
- Use metrics for production visibility

### 4. High-Frequency Trading
- Pre-allocate pools at startup
- Never clear during trading hours
- Use separate pools per strategy

## Integration with VectorWave Components

### CWT with Memory Pools
```java
CWTConfig config = CWTConfig.builder()
    .memoryPool(new CWTMemoryPool())
    .build();

CWTTransform transform = new CWTTransform(wavelet, config);
// Pool managed by CWT internally
```

### Streaming with Pools
```java
StreamingConfig config = StreamingConfig.builder()
    .memoryPool(new AlignedMemoryPool())
    .bufferSize(1024)
    .build();
```

### FFM Integration
```java
try (FFMMemoryPool pool = new FFMMemoryPool()) {
    FFMWaveletTransform transform = new FFMWaveletTransform(wavelet, pool);
    // All FFM operations share the same pool
}
```

## Performance Tips

1. **Pool Size**: Start with 10-20 arrays per size, adjust based on metrics
2. **Clear Frequency**: Balance between memory usage and allocation overhead
3. **Thread Safety**: Use shared pools for low contention, thread-local for high
4. **Monitoring**: Always monitor pool efficiency in production
5. **Pre-warming**: Pre-allocate common sizes during initialization

## Summary

Proper memory pool lifecycle management is crucial for optimal VectorWave performance. Follow these guidelines:

1. Choose the right pool type for your use case
2. Always use try-finally or try-with-resources
3. Clear pools at appropriate boundaries
4. Monitor pool efficiency and memory usage
5. Use thread-local pools for high-contention scenarios
6. Implement automatic lifecycle management where possible