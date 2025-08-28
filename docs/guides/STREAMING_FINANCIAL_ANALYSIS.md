# Streaming Wavelet Analysis for Financial Markets

## Table of Contents
1. [Introduction](#introduction)
2. [Why Streaming Wavelets for Finance](#why-streaming-wavelets-for-finance)
3. [Core Concepts](#core-concepts)
4. [Implementation Guide](#implementation-guide)
5. [Trading Applications](#trading-applications)
6. [Real-World Examples](#real-world-examples)
7. [Performance Considerations](#performance-considerations)
8. [Best Practices](#best-practices)

## Introduction

Streaming wavelet analysis enables real-time decomposition of financial time series data, providing traders and analysts with multi-resolution insights into market dynamics as they unfold. Unlike batch processing, streaming analysis processes data incrementally, making it ideal for live trading systems.

### Key Advantages for Trading

- **Real-time signal decomposition**: Identify trends and anomalies as they occur
- **Multi-timeframe analysis**: Simultaneous analysis across multiple time scales
- **Low latency**: Process market data with minimal delay (~100-300ns per transform)
- **Memory efficiency**: Fixed memory footprint regardless of data history
- **Adaptive filtering**: Remove market noise while preserving important features

## Why Streaming Wavelets for Finance

### Traditional Technical Analysis Limitations

Traditional indicators like moving averages and oscillators have inherent limitations:
- Fixed time windows
- Lag in signal detection
- Poor noise handling
- Single timeframe focus

### Wavelet Advantages

Wavelets overcome these limitations by providing:

1. **Time-Frequency Localization**: See both when and at what frequency/scale events occur
2. **Adaptive Resolution**: High time resolution for high-frequency events, high frequency resolution for low-frequency trends
3. **Noise Separation**: Natural separation of signal and noise components
4. **Scale Invariance**: Detect patterns regardless of timeframe

## Core Concepts

### Streaming Architecture

VectorWave's streaming implementation uses a block-based approach:

```java
// Basic streaming setup
StreamingWaveletTransform transform = StreamingWaveletTransform.create(
    Daubechies.DB4,           // Wavelet choice
    BoundaryMode.PERIODIC,    // Boundary handling
    256                       // Block size (must be power of 2)
);

// Subscribe to results
transform.subscribe(new Flow.Subscriber<TransformResult>() {
    @Override
    public void onNext(TransformResult result) {
        // Process decomposed block
        analyzeMarketConditions(result);
    }
});
```

### Block Size Selection

Block size affects both latency and frequency resolution:

| Block Size | Latency | Best For |
|------------|---------|----------|
| 64 | ~100ns | HFT, tick data |
| 128 | ~150ns | Scalping (seconds) |
| 256 | ~200ns | Day trading (minutes) |
| 512 | ~300ns | Swing trading (hours) |
| 1024 | ~500ns | Position trading (days) |

### Wavelet Selection for Finance

Different wavelets suit different market analysis needs:

- **Haar**: Sharp price movements, breakouts
- **Daubechies DB4**: Smooth trends, general purpose
- **Symlet SYM4**: Symmetric patterns, reversal detection
- **Coiflet COIF2**: Smooth analysis with good frequency localization

## Implementation Guide

### 1. Basic Price Stream Analysis

```java
public class PriceStreamAnalyzer {
    private final StreamingWaveletTransform transform;
    private final int blockSize = 256;
    
    public PriceStreamAnalyzer() {
        this.transform = StreamingWaveletTransform.create(
            Daubechies.DB4,
            BoundaryMode.PERIODIC,
            blockSize
        );
        
        transform.subscribe(new PriceAnalysisSubscriber());
    }
    
    public void processTick(double price, long timestamp) {
        // Add price to stream
        transform.process(price);
    }
    
    private class PriceAnalysisSubscriber implements Flow.Subscriber<TransformResult> {
        @Override
        public void onNext(TransformResult result) {
            // Extract trend (approximation) and volatility (details)
            double[] trend = result.approximationCoeffs();
            double[] volatility = result.detailCoeffs();
            
            // Analyze market state
            MarketState state = analyzeState(trend, volatility);
            
            // Generate trading signals
            if (state.isTrending() && state.lowVolatility()) {
                signalTrendFollowing();
            } else if (state.isRanging() && state.highVolatility()) {
                signalMeanReversion();
            }
        }
    }
}
```

### 2. Multi-Resolution Volatility Analysis

```java
public class VolatilityAnalyzer {
    private final StreamingWaveletTransform[] transforms;
    private final int[] blockSizes = {64, 128, 256, 512}; // Multiple timeframes
    
    public VolatilityAnalyzer() {
        transforms = new StreamingWaveletTransform[blockSizes.length];
        
        for (int i = 0; i < blockSizes.length; i++) {
            final int timeframe = i;
            transforms[i] = StreamingWaveletTransform.create(
                Daubechies.DB4,
                BoundaryMode.PERIODIC,
                blockSizes[i]
            );
            
            transforms[i].subscribe(new Flow.Subscriber<TransformResult>() {
                @Override
                public void onNext(TransformResult result) {
                    updateVolatilityProfile(timeframe, result);
                }
            });
        }
    }
    
    public void processReturn(double logReturn) {
        // Feed return to all timeframes
        for (StreamingWaveletTransform transform : transforms) {
            transform.process(logReturn);
        }
    }
    
    private void updateVolatilityProfile(int timeframe, TransformResult result) {
        // Calculate instantaneous volatility from detail coefficients
        double[] details = result.detailCoeffs();
        double instantVol = calculateRMS(details);
        
        // Update volatility term structure
        volProfile[timeframe] = instantVol;
        
        // Detect volatility regime changes
        if (detectRegimeChange()) {
            adjustPositionSizing();
            updateRiskLimits();
        }
    }
}
```

### 3. Microstructure Noise Filtering

```java
public class NoiseFilteredPriceStream {
    private final StreamingWaveletTransform transform;
    private final WaveletDenoiser denoiser;
    private double[] priceBuffer;
    private int bufferIndex = 0;
    
    public NoiseFilteredPriceStream() {
        this.transform = StreamingWaveletTransform.create(
            Symlet.SYM4,  // Good for noise reduction
            BoundaryMode.PERIODIC,
            128  // Small block for low latency
        );
        
        this.denoiser = new WaveletDenoiser(
            Symlet.SYM4,
            ThresholdMethod.SURE,  // Optimal threshold selection
            ThresholdType.SOFT      // Smooth denoising
        );
        
        this.priceBuffer = new double[128];
        
        transform.subscribe(new DenoisingSubscriber());
    }
    
    private class DenoisingSubscriber implements Flow.Subscriber<TransformResult> {
        @Override
        public void onNext(TransformResult result) {
            // Apply denoising threshold to detail coefficients
            double[] details = result.detailCoeffs();
            double threshold = denoiser.calculateThreshold(details);
            
            // Soft threshold the details
            for (int i = 0; i < details.length; i++) {
                details[i] = softThreshold(details[i], threshold);
            }
            
            // Reconstruct clean price
            TransformResult filtered = TransformResult.create(
                result.approximationCoeffs(),
                details
            );
            
            double[] cleanPrices = transform.inverse(filtered);
            publishCleanPrices(cleanPrices);
        }
    }
}
```

### 4. Order Flow Imbalance Detection

```java
public class OrderFlowAnalyzer {
    private final StreamingWaveletTransform bidTransform;
    private final StreamingWaveletTransform askTransform;
    private final int blockSize = 64; // Fast for HFT
    
    public OrderFlowAnalyzer() {
        // Separate transforms for bid and ask volume
        bidTransform = createTransform();
        askTransform = createTransform();
        
        // Synchronized analysis
        Flow.Subscriber<TransformResult> imbalanceDetector = 
            new ImbalanceSubscriber();
            
        bidTransform.subscribe(imbalanceDetector);
        askTransform.subscribe(imbalanceDetector);
    }
    
    public void processOrderBookUpdate(double bidVolume, double askVolume) {
        bidTransform.process(Math.log1p(bidVolume)); // Log transform for stability
        askTransform.process(Math.log1p(askVolume));
    }
    
    private class ImbalanceSubscriber implements Flow.Subscriber<TransformResult> {
        private TransformResult lastBidResult;
        private TransformResult lastAskResult;
        
        @Override
        public synchronized void onNext(TransformResult result) {
            // Store results and analyze when both are available
            if (result == lastBidResult || lastAskResult == null) {
                return;
            }
            
            // Calculate flow imbalance at multiple scales
            double[] bidTrend = lastBidResult.approximationCoeffs();
            double[] askTrend = lastAskResult.approximationCoeffs();
            
            double[] bidMomentum = lastBidResult.detailCoeffs();
            double[] askMomentum = lastAskResult.detailCoeffs();
            
            // Detect directional pressure
            double trendImbalance = calculateImbalance(bidTrend, askTrend);
            double momentumImbalance = calculateImbalance(bidMomentum, askMomentum);
            
            if (trendImbalance > 0.7 && momentumImbalance > 0.5) {
                signalBuyPressure();
            } else if (trendImbalance < -0.7 && momentumImbalance < -0.5) {
                signalSellPressure();
            }
        }
    }
}
```

## Trading Applications

### 1. Trend-Following Systems

Wavelets excel at separating trend from noise:

```java
public class WaveletTrendFollower {
    private final StreamingWaveletTransform transform;
    private TrendState currentTrend = TrendState.NEUTRAL;
    
    public void analyzeTrend(TransformResult result) {
        double[] approximation = result.approximationCoeffs();
        
        // Calculate trend strength from smooth component
        double trendSlope = calculateSlope(approximation);
        double trendStrength = calculateAutoCorrelation(approximation);
        
        // Multi-scale confirmation
        if (trendSlope > 0 && trendStrength > 0.8) {
            if (currentTrend != TrendState.BULLISH) {
                enterLongPosition();
                currentTrend = TrendState.BULLISH;
            }
        } else if (trendSlope < 0 && trendStrength > 0.8) {
            if (currentTrend != TrendState.BEARISH) {
                enterShortPosition();
                currentTrend = TrendState.BEARISH;
            }
        } else if (trendStrength < 0.3) {
            closePositions();
            currentTrend = TrendState.NEUTRAL;
        }
    }
}
```

### 2. Mean Reversion Strategies

Detail coefficients capture deviations from trend:

```java
public class WaveletMeanReversion {
    private final double entryThreshold = 2.0; // Standard deviations
    private final double exitThreshold = 0.5;
    
    public void analyzeReversion(TransformResult result) {
        double[] details = result.detailCoeffs();
        
        // Calculate z-score of current deviation
        double mean = calculateMean(details);
        double std = calculateStd(details);
        double currentDeviation = details[details.length - 1];
        double zScore = (currentDeviation - mean) / std;
        
        // Generate signals
        if (Math.abs(zScore) > entryThreshold) {
            // Extreme deviation - expect reversion
            if (zScore > 0) {
                signalShortEntry(); // Price too high
            } else {
                signalLongEntry();  // Price too low
            }
        } else if (Math.abs(zScore) < exitThreshold) {
            // Deviation normalized - close position
            closeReversionPosition();
        }
    }
}
```

### 3. Volatility Trading

```java
public class WaveletVolatilityTrader {
    private final StreamingWaveletTransform transform;
    private final double[] historicalVol = new double[100];
    private int volIndex = 0;
    
    public void analyzeVolatility(TransformResult result) {
        // Extract volatility from multiple scales
        double[] details = result.detailCoeffs();
        double instantVol = 0;
        
        // Sum squared details (energy = volatility proxy)
        for (double d : details) {
            instantVol += d * d;
        }
        instantVol = Math.sqrt(instantVol / details.length);
        
        // Update historical volatility
        historicalVol[volIndex % historicalVol.length] = instantVol;
        volIndex++;
        
        // Calculate volatility percentile
        double volPercentile = calculatePercentile(historicalVol, instantVol);
        
        // Trading logic
        if (volPercentile > 0.9) {
            // High volatility regime
            buyVolatility(); // Long straddles/strangles
            reducePositionSize(); // Risk management
        } else if (volPercentile < 0.1) {
            // Low volatility regime
            sellVolatility(); // Short premium
            searchForBreakouts(); // Prepare for vol expansion
        }
    }
}
```

### 4. Market Microstructure Analysis

```java
public class MicrostructureAnalyzer {
    private final int tickBlockSize = 64; // Process every 64 ticks
    private final StreamingWaveletTransform spreadTransform;
    private final StreamingWaveletTransform volumeTransform;
    
    public void analyzeMarketQuality(double spread, double volume) {
        spreadTransform.process(spread);
        volumeTransform.process(Math.log1p(volume));
    }
    
    private class SpreadAnalyzer implements Flow.Subscriber<TransformResult> {
        @Override
        public void onNext(TransformResult result) {
            double[] spreadTrend = result.approximationCoeffs();
            double[] spreadNoise = result.detailCoeffs();
            
            // Detect liquidity conditions
            double avgSpread = calculateMean(spreadTrend);
            double spreadVolatility = calculateStd(spreadNoise);
            
            if (avgSpread > historicalAvg * 1.5) {
                // Wide spreads - reduce aggression
                adjustToPassiveExecution();
            } else if (spreadVolatility > historicalVol * 2) {
                // Unstable market - avoid market orders
                useOnlyLimitOrders();
            }
        }
    }
}
```

## Real-World Examples

### Example 1: EUR/USD Intraday Trading

```java
public class EURUSDIntradaySystem {
    private final StreamingWaveletTransform shortTerm;  // 64-tick blocks
    private final StreamingWaveletTransform mediumTerm; // 256-tick blocks
    private final StreamingWaveletTransform longTerm;   // 1024-tick blocks
    
    public EURUSDIntradaySystem() {
        // Multi-timeframe setup
        shortTerm = StreamingWaveletTransform.create(
            Haar(), BoundaryMode.PERIODIC, 64
        );
        mediumTerm = StreamingWaveletTransform.create(
            Daubechies.DB4, BoundaryMode.PERIODIC, 256
        );
        longTerm = StreamingWaveletTransform.create(
            Symlet.SYM4, BoundaryMode.PERIODIC, 1024
        );
    }
    
    public void processTick(FXTick tick) {
        double midPrice = (tick.bid + tick.ask) / 2.0;
        double logReturn = Math.log(midPrice / lastMidPrice);
        
        // Feed to all timeframes
        shortTerm.process(logReturn);
        mediumTerm.process(logReturn);
        longTerm.process(logReturn);
    }
    
    // Trading logic based on timeframe alignment
    private void generateSignals() {
        if (longTermTrend == BULLISH && 
            mediumTermMomentum == POSITIVE && 
            shortTermEntry == TRIGGERED) {
            executeBuyOrder();
        }
    }
}
```

### Example 2: S&P 500 Futures Scalping

```java
public class ES_ScalpingSystem {
    private final StreamingWaveletTransform transform;
    private final int TICK_BLOCK = 32; // Ultra-low latency
    private final double PROFIT_TARGET = 2.0; // ticks
    private final double STOP_LOSS = 1.5; // ticks
    
    public ES_ScalpingSystem() {
        // Haar for sharp movement detection
        transform = StreamingWaveletTransform.create(
            new Haar(), 
            BoundaryMode.ZERO_PADDING, 
            TICK_BLOCK
        );
        
        // Configure for minimum latency
        TransformConfig config = TransformConfig.builder()
            .forceVector(true)  // Force vector operations
            .build();
    }
    
    private class ScalpSubscriber implements Flow.Subscriber<TransformResult> {
        @Override
        public void onNext(TransformResult result) {
            // Fast feature extraction
            double[] details = result.detailCoeffs();
            
            // Detect micro-breakouts
            double lastDetail = details[details.length - 1];
            double detailMagnitude = Math.abs(lastDetail);
            
            if (detailMagnitude > breakoutThreshold) {
                // Sharp movement detected
                if (lastDetail > 0) {
                    buyMarket();
                    setStopLoss(entryPrice - STOP_LOSS);
                    setProfitTarget(entryPrice + PROFIT_TARGET);
                } else {
                    sellMarket();
                    setStopLoss(entryPrice + STOP_LOSS);
                    setProfitTarget(entryPrice - PROFIT_TARGET);
                }
            }
        }
    }
}
```

### Example 3: Crypto Market Making

```java
public class CryptoMarketMaker {
    private final StreamingWaveletTransform orderFlowTransform;
    private final StreamingWaveletTransform priceTransform;
    private final int FLOW_BLOCK = 128;
    
    private double fairValue;
    private double bidDistance;
    private double askDistance;
    
    public void updateOrderBook(OrderBookSnapshot snapshot) {
        // Analyze order flow imbalance
        double flowImbalance = snapshot.bidVolume - snapshot.askVolume;
        orderFlowTransform.process(flowImbalance);
        
        // Analyze price dynamics
        double midPrice = (snapshot.bestBid + snapshot.bestAsk) / 2.0;
        priceTransform.process(midPrice);
    }
    
    private class MarketMakingSubscriber implements Flow.Subscriber<TransformResult> {
        @Override
        public void onNext(TransformResult result) {
            if (source == orderFlowTransform) {
                // Adjust quotes based on flow
                double[] flowTrend = result.approximationCoeffs();
                double flowPressure = calculateTrend(flowTrend);
                
                if (flowPressure > 0) {
                    // Buy pressure - widen ask, tighten bid
                    bidDistance *= 0.9;
                    askDistance *= 1.1;
                } else {
                    // Sell pressure - widen bid, tighten ask
                    bidDistance *= 1.1;
                    askDistance *= 0.9;
                }
            } else if (source == priceTransform) {
                // Update fair value estimate
                double[] priceTrend = result.approximationCoeffs();
                fairValue = priceTrend[priceTrend.length - 1];
                
                // Update quotes
                updateQuotes(
                    fairValue - bidDistance,
                    fairValue + askDistance
                );
            }
        }
    }
}
```

## Performance Considerations

### Latency Optimization

1. **Block Size**: Smaller blocks = lower latency but less frequency resolution
2. **Wavelet Choice**: Haar is fastest (~100ns), Daubechies/Symlets slower (~200-300ns)
3. **Configuration**: Force SIMD for best performance on supporting hardware

```java
// Ultra-low latency configuration
TransformConfig config = TransformConfig.builder()
    .forceVector(true)
    .build();

StreamingWaveletTransform transform = StreamingWaveletTransform.create(
    new Haar(),              // Fastest wavelet
    BoundaryMode.PERIODIC,   // Fastest boundary mode
    64,                      // Smallest practical block
    config
);
```

### Memory Management

```java
public class MemoryEfficientStreaming {
    private final MemoryPool pool;
    private final StreamingWaveletTransform transform;
    
    public MemoryEfficientStreaming() {
        // Pre-allocate memory pool
        pool = new MemoryPool(
            10,    // Number of buffers
            256    // Buffer size
        );
        
        transform = StreamingWaveletTransform.create(
            Daubechies.DB4,
            BoundaryMode.PERIODIC,
            256
        );
    }
    
    public void processData(double[] data) {
        double[] buffer = pool.borrowArray(data.length);
        try {
            System.arraycopy(data, 0, buffer, 0, data.length);
            transform.process(buffer);
        } finally {
            pool.returnArray(buffer);
        }
    }
}
```

### Throughput Optimization

For high-throughput scenarios, use parallel processing:

```java
public class ParallelStreamProcessor {
    private final int NUM_STREAMS = 4;
    private final StreamingWaveletTransform[] transforms;
    private final AtomicInteger router = new AtomicInteger(0);
    
    public ParallelStreamProcessor() {
        transforms = new StreamingWaveletTransform[NUM_STREAMS];
        for (int i = 0; i < NUM_STREAMS; i++) {
            transforms[i] = StreamingWaveletTransform.create(
                Daubechies.DB4,
                BoundaryMode.PERIODIC,
                256
            );
        }
    }
    
    public void processTick(double price) {
        // Round-robin distribution
        int stream = router.getAndIncrement() % NUM_STREAMS;
        transforms[stream].process(price);
    }
}
```

## Best Practices

### 1. Wavelet Selection Strategy

```java
public class AdaptiveWaveletSelector {
    public Wavelet selectWavelet(MarketConditions conditions) {
        if (conditions.isHighVolatility()) {
            // Haar for sharp movements
            return new Haar();
        } else if (conditions.isTrending()) {
            // Daubechies for smooth trends
            return Daubechies.DB4;
        } else if (conditions.isRanging()) {
            // Symlet for symmetric patterns
            return Symlet.SYM4;
        } else {
            // Default general-purpose
            return Daubechies.DB2;
        }
    }
}
```

### 2. Risk Management Integration

```java
public class WaveletRiskManager {
    private final StreamingWaveletTransform transform;
    private double maxDrawdown = 0.02; // 2%
    private double currentExposure = 0;
    
    public void adjustPositionSize(TransformResult result) {
        // Calculate volatility from wavelets
        double instantVol = calculateVolatility(result.detailCoeffs());
        double trendStrength = calculateTrendStrength(result.approximationCoeffs());
        
        // Kelly-inspired position sizing
        double optimalSize = (expectedReturn - riskFreeRate) / (instantVol * instantVol);
        
        // Apply safety factor based on trend strength
        double safetyFactor = 0.25 + 0.5 * trendStrength; // 25-75% of Kelly
        double targetSize = optimalSize * safetyFactor;
        
        // Respect maximum drawdown
        targetSize = Math.min(targetSize, maxDrawdown / instantVol);
        
        adjustPosition(targetSize);
    }
}
```

### 3. Backtesting Considerations

When backtesting streaming wavelet strategies:

1. **Respect Causality**: Only use past data in transforms
2. **Realistic Latency**: Add appropriate delays
3. **Transaction Costs**: Include spread and commissions
4. **Market Impact**: Model slippage for larger orders

```java
public class WaveletBacktester {
    public BacktestResult runBacktest(HistoricalData data) {
        StreamingWaveletTransform transform = StreamingWaveletTransform.create(
            Daubechies.DB4,
            BoundaryMode.PERIODIC,
            256
        );
        
        BacktestResult results = new BacktestResult();
        
        for (Tick tick : data.getTicks()) {
            // Process tick with realistic delay
            Thread.sleep(PROCESSING_DELAY_MS);
            
            transform.process(tick.price);
            
            // Apply transaction costs
            if (signal.isEntry()) {
                results.addCost(COMMISSION + SPREAD/2);
            }
        }
        
        return results;
    }
}
```

### 4. Production Deployment

Key considerations for production systems:

1. **Monitoring**: Track transform latency and accuracy
2. **Failover**: Redundant processing paths
3. **State Management**: Persist transform state for recovery
4. **Validation**: Sanity check all signals

```java
public class ProductionStreamingSystem {
    private final StreamingWaveletTransform primary;
    private final StreamingWaveletTransform backup;
    private final MetricsCollector metrics;
    
    public void processTick(MarketTick tick) {
        long startTime = System.nanoTime();
        
        try {
            // Process with monitoring
            primary.process(tick.price);
            
            // Record metrics
            long latency = System.nanoTime() - startTime;
            metrics.recordLatency(latency);
            
            // Validate output
            if (!isValidSignal(lastSignal)) {
                logger.error("Invalid signal detected: {}", lastSignal);
                switchToBackup();
            }
            
        } catch (Exception e) {
            logger.error("Primary transform failed", e);
            backup.process(tick.price);
        }
    }
}
```

## Conclusion

Streaming wavelet analysis provides a powerful framework for real-time financial market analysis. The key advantages are:

1. **Multi-resolution analysis** without lag
2. **Natural noise filtering** while preserving signal
3. **Adaptive feature extraction** across timeframes
4. **Low latency** suitable for HFT (~100-300ns)
5. **Memory efficiency** for continuous operation

Success depends on:
- Appropriate wavelet and block size selection
- Proper risk management integration
- Realistic backtesting and validation
- Robust production deployment

The VectorWave library provides all the tools needed to implement sophisticated streaming wavelet analysis systems for modern electronic trading.