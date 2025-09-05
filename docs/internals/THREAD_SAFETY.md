# Thread Safety Guide

This document details the thread safety characteristics of VectorWave classes, particularly important for financial applications where concurrent processing is common.

## Thread Safety Classifications

### ✅ Thread-Safe (Immutable)

These classes are immutable and safe to share between threads without synchronization:

| Class | Package | Notes |
|-------|---------|-------|
| `MODWTResult` | `com.morphiqlabs.wavelet.modwt` | Immutable result container with defensive copying |
| `FinancialConfig` | `com.morphiqlabs.financial` | Immutable configuration |
| `FinancialAnalysisConfig` | `com.morphiqlabs.financial` | Immutable configuration with builder |
| `WaveletOperations.PerformanceInfo` | `com.morphiqlabs.wavelet` | Immutable performance data |
| All `Wavelet` implementations | `com.morphiqlabs.wavelet.api` | Wavelets are immutable |

### ✅ Thread-Safe (Stateless)

These classes have no mutable state and are safe for concurrent use:

| Class | Package | Notes |
|-------|---------|-------|
| `WaveletOperations` | `com.morphiqlabs.wavelet` | Static utility methods only |
| `WaveletRegistry` | `com.morphiqlabs.wavelet.api` | Thread-safe singleton with concurrent collections |

### ⚠️ Thread-Safe with Conditions

These classes are thread-safe under specific conditions:

| Class | Package | Condition |
|-------|---------|-----------|
| `MemoryPool` | `com.morphiqlabs.wavelet.memory` | Thread-safe for borrow/return operations via ConcurrentHashMap |
| `MODWTTransform` | `com.morphiqlabs.wavelet.modwt` | Safe for concurrent reads; create separate instances for writes |
| `FinancialAnalyzer` | `com.morphiqlabs.financial` | Stateless after construction; safe for concurrent analysis |
| `FinancialWaveletAnalyzer` | `com.morphiqlabs.financial` | Contains MODWTTransform; safe if not shared or if synchronized |

### ❌ Not Thread-Safe

These classes maintain mutable state and require external synchronization:

| Class | Package | Reason |
|-------|---------|--------|
| `WaveletDenoiser` | `com.morphiqlabs.wavelet.denoising` | Contains mutable transform instance |
| `MODWTStreamingDenoiser` | `com.morphiqlabs.wavelet.modwt.streaming` | Maintains streaming state |
| `MODWTStreamingTransform` | `com.morphiqlabs.wavelet.modwt.streaming` | Maintains buffer state |

## Best Practices for Concurrent Financial Applications

### 1. Immutable Results Pattern
```java
// Thread-safe: MODWTResult is immutable
public class ConcurrentAnalyzer {
    private final MODWTTransform transform = new MODWTTransform(
        Daubechies.DB4, BoundaryMode.PERIODIC);
    
    public MODWTResult analyzePrice(double[] prices) {
        // Safe: returns immutable result
        return transform.forward(prices);
    }
}
```

### 2. Thread-Local Transforms
```java
// For performance-critical concurrent processing
public class HighFrequencyAnalyzer {
    private final ThreadLocal<MODWTTransform> transforms = 
        ThreadLocal.withInitial(() -> 
            new MODWTTransform(new Haar(), BoundaryMode.PERIODIC));
    
    public double[] processTickData(double[] ticks) {
        MODWTTransform transform = transforms.get();
        MODWTResult result = transform.forward(ticks);
        return result.detailCoeffs();
    }
}
```

### 3. Concurrent Batch Processing
```java
// Safe parallel processing of multiple assets
public class PortfolioAnalyzer {
    private final ExecutorService executor = ForkJoinPool.commonPool();
    
    public Map<String, Double> analyzePortfolio(Map<String, double[]> assetPrices) {
        return assetPrices.entrySet().parallelStream()
            .collect(Collectors.toConcurrentMap(
                Map.Entry::getKey,
                entry -> {
                    // Each thread gets its own analyzer
                    FinancialWaveletAnalyzer analyzer = 
                        new FinancialWaveletAnalyzer(new FinancialConfig(0.045));
                    return analyzer.calculateWaveletSharpeRatio(entry.getValue());
                }
            ));
    }
}
```

### 4. Shared Memory Pool
```java
// Thread-safe memory pooling for high-frequency trading
public class TradingSystem {
    private final MemoryPool pool = new MemoryPool();
    
    public void processTrades(List<Trade> trades) {
        trades.parallelStream().forEach(trade -> {
            double[] buffer = pool.borrowArray(1000);
            try {
                // Process trade data
                analyzeTrade(trade, buffer);
            } finally {
                pool.returnArray(buffer);
            }
        });
    }
}
```

## Financial-Specific Considerations

### Market Data Processing
When processing real-time market data streams:
- Use separate `MODWTTransform` instances per thread
- Share immutable `FinancialConfig` across threads
- Return immutable `MODWTResult` objects

### Risk Calculations
For portfolio risk calculations:
- `FinancialAnalyzer` is thread-safe after construction
- Results can be safely aggregated across threads
- Use concurrent collections for result aggregation

### Backtesting
For parallel backtesting scenarios:
- Create one analyzer per thread/backtest run
- Share configuration objects (they're immutable)
- Use thread-safe collections for results

## Example: Thread-Safe Market Monitor

```java
public class ThreadSafeMarketMonitor {
    private final FinancialConfig config;
    private final ConcurrentHashMap<String, MODWTTransform> transforms;
    private final ConcurrentHashMap<String, FinancialWaveletAnalyzer> analyzers;
    
    public ThreadSafeMarketMonitor(double riskFreeRate) {
        this.config = new FinancialConfig(riskFreeRate);
        this.transforms = new ConcurrentHashMap<>();
        this.analyzers = new ConcurrentHashMap<>();
    }
    
    public double analyzeAsset(String symbol, double[] prices) {
        // Lazy initialization with thread-safe compute
        FinancialWaveletAnalyzer analyzer = analyzers.computeIfAbsent(
            symbol, 
            k -> new FinancialWaveletAnalyzer(config)
        );
        
        // FinancialWaveletAnalyzer.calculateSharpeRatio is stateless
        return analyzer.calculateWaveletSharpeRatio(prices);
    }
    
    public Map<String, MODWTResult> analyzePortfolio(Map<String, double[]> portfolio) {
        // Safe parallel processing
        return portfolio.entrySet().parallelStream()
            .collect(Collectors.toConcurrentMap(
                Map.Entry::getKey,
                entry -> {
                    MODWTTransform transform = transforms.computeIfAbsent(
                        entry.getKey(),
                        k -> new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC)
                    );
                    return transform.forward(entry.getValue());
                }
            ));
    }
}
```

## Summary

For financial applications:
1. **Prefer immutable objects** - All results and configurations are immutable
2. **Use thread-local or per-thread instances** for transforms and analyzers
3. **Share configurations** - They're immutable and thread-safe
4. **Leverage parallel streams** - Safe with proper instance management
5. **Use MemoryPool** - It's thread-safe and reduces GC pressure