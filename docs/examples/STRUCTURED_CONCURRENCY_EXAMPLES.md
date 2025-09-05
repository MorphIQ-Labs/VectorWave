# Structured Concurrency Examples

Complete code examples demonstrating VectorWave's structured concurrency features for various use cases.

## Table of Contents

1. [Basic Examples](#basic-examples)
2. [Financial Analysis Examples](#financial-analysis-examples)
3. [Real-Time Processing Examples](#real-time-processing-examples)
4. [Performance Optimization Examples](#performance-optimization-examples)
5. [Error Handling Examples](#error-handling-examples)
6. [Integration Examples](#integration-examples)

## Basic Examples

### Simple Batch Processing

```java
import com.morphiqlabs.wavelet.parallel.StructuredParallelTransform;
import com.morphiqlabs.wavelet.parallel.ParallelConfig;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.BoundaryMode;

public class BasicBatchExample {
    
    public void processSignalBatch(double[][] signals) {
        // Configure parallel processing
        ParallelConfig config = ParallelConfig.builder()
            .targetCores(Runtime.getRuntime().availableProcessors())
            .minParallelThreshold(512)
            .build();
        
        // Create structured parallel transform
        StructuredParallelTransform transform = new StructuredParallelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, config);
        
        try {
            // Process entire batch with automatic resource management
            MODWTResult[] results = transform.forwardBatch(signals);
            
            // Process results
            for (int i = 0; i < results.length; i++) {
                processTransformResult(results[i], i);
            }
            
            // Reconstruct signals if needed
            double[][] reconstructed = transform.inverseBatch(results);
            
        } catch (StructuredParallelTransform.ComputationException e) {
            System.err.println("Batch processing failed: " + e.getMessage());
            // Handle error appropriately
        }
    }
    
    private void processTransformResult(MODWTResult result, int index) {
        double[] approximation = result.approximationCoeffs();
        double[][] details = result.detailCoeffs();
        
        System.out.printf("Signal %d: %d approximation coeffs, %d detail levels%n",
            index, approximation.length, details.length);
        
        // Extract features, apply thresholding, etc.
        double energy = calculateEnergy(approximation);
        double[] bandEnergies = calculateBandEnergies(details);
        
        System.out.printf("  Total energy: %.3f%n", energy);
        for (int level = 0; level < bandEnergies.length; level++) {
            System.out.printf("  Level %d energy: %.3f%n", level + 1, bandEnergies[level]);
        }
    }
    
    private double calculateEnergy(double[] coefficients) {
        return Arrays.stream(coefficients)
            .map(x -> x * x)
            .sum();
    }
    
    private double[] calculateBandEnergies(double[][] details) {
        return Arrays.stream(details)
            .mapToDouble(this::calculateEnergy)
            .toArray();
    }
}
```

### Low-Level Executor Usage

```java
import com.morphiqlabs.wavelet.parallel.StructuredExecutor;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MODWTResult;

public class LowLevelExecutorExample {
    
    private final MODWTTransform transform;
    
    public LowLevelExecutorExample() {
        this.transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
    }
    
    public List<MODWTResult> processWithProgressTracking(List<double[]> signals) {
        ParallelConfig config = ParallelConfig.builder()
            .targetCores(4)
            .minParallelThreshold(256)
            .build();
        
        AtomicInteger completed = new AtomicInteger(0);
        List<MODWTResult> results = new ArrayList<>();
        
        try (var executor = new StructuredExecutor(config)) {
            // Submit all tasks
            List<StructuredExecutor.StructuredFuture<MODWTResult>> futures = new ArrayList<>();
            
            for (double[] signal : signals) {
                futures.add(executor.submit(() -> {
                    MODWTResult result = transform.forward(signal);
                    int done = completed.incrementAndGet();
                    System.out.printf("Progress: %d/%d signals completed%n", 
                        done, signals.size());
                    return result;
                }));
            }
            
            // Wait for all tasks to complete
            executor.joinAll();
            
            // Collect results
            for (var future : futures) {
                results.add(future.get());
            }
            
        } catch (ExecutionException e) {
            System.err.println("Task execution failed: " + e.getCause().getMessage());
            throw new RuntimeException("Processing failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Processing interrupted", e);
        }
        
        return results;
    }
    
    public void demonstrateAsyncProcessing() {
        double[] signal = generateTestSignal(1024);
        
        // Start async processing
        CompletableFuture<MODWTResult> forwardFuture = 
            CompletableFuture.supplyAsync(() -> transform.forward(signal));
        
        // Do other work while processing...
        System.out.println("Doing other work while transform runs...");
        
        // Wait for completion and get result
        try {
            MODWTResult result = forwardFuture.get(30, TimeUnit.SECONDS);
            System.out.println("Transform completed successfully");
            
            // Async inverse transform
            CompletableFuture<double[]> inverseFuture = 
                CompletableFuture.supplyAsync(() -> transform.inverse(result));
            
            double[] reconstructed = inverseFuture.get(30, TimeUnit.SECONDS);
            System.out.println("Reconstruction completed");
            
        } catch (TimeoutException e) {
            System.err.println("Transform timed out");
            forwardFuture.cancel(true);
        } catch (Exception e) {
            System.err.println("Transform failed: " + e.getMessage());
        }
    }
    
    private double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(4 * Math.PI * i / 32.0) +
                       0.1 * (Math.random() - 0.5);
        }
        return signal;
    }
}
```

## Financial Analysis Examples

### High-Frequency Trading Analysis

```java
import com.morphiqlabs.wavelet.cwt.PaulWavelet;
import com.morphiqlabs.wavelet.parallel.StructuredParallelTransform;
import com.morphiqlabs.financial.FinancialWaveletAnalyzer;
import com.morphiqlabs.financial.FinancialConfig;

public class HighFrequencyTradingExample {
    
    private final StructuredParallelTransform paulTransform;
    private final FinancialWaveletAnalyzer analyzer;
    private final AdaptiveThresholdTuner tuner;
    
    public HighFrequencyTradingExample(double riskFreeRate) {
        // Configure for high-frequency financial data
        ParallelConfig config = ParallelConfig.builder()
            .targetCores(Runtime.getRuntime().availableProcessors())
            .minParallelThreshold(256)  // HFT data comes in small batches
            .maxChunkSize(2048)
            .build();
        
        // Paul wavelet is excellent for detecting asymmetric price movements
        this.paulTransform = new StructuredParallelTransform(
            new PaulWavelet(4), BoundaryMode.PERIODIC, config);
        
        // Configure financial analyzer
        FinancialConfig financialConfig = new FinancialConfig(riskFreeRate);
        this.analyzer = new FinancialWaveletAnalyzer(financialConfig);
        
        // Adaptive tuning for varying market conditions
        this.tuner = new AdaptiveThresholdTuner();
    }
    
    public TradingSignals analyzePriceMovements(double[][] priceReturns, long timeoutMs) {
        try {
            // Process with timeout for real-time constraints
            MODWTResult[] results = paulTransform.forwardBatchWithTimeout(
                priceReturns, timeoutMs);
            
            // Extract trading signals from wavelet decomposition
            return extractTradingSignals(results, priceReturns);
            
        } catch (StructuredParallelTransform.ComputationException e) {
            if (e.getMessage().contains("timeout")) {
                System.err.println("Analysis timed out - using cached signals");
                return getCachedSignals();
            }
            throw new RuntimeException("Price analysis failed", e);
        }
    }
    
    private TradingSignals extractTradingSignals(MODWTResult[] results, 
                                               double[][] priceReturns) {
        List<CrashPrediction> crashPredictions = new ArrayList<>();
        List<VolatilityForecast> volatilityForecasts = new ArrayList<>();
        
        for (int i = 0; i < results.length; i++) {
            MODWTResult result = results[i];
            double[] returns = priceReturns[i];
            
            // Detect crash patterns using detail coefficients
            CrashPrediction crash = detectCrashPattern(result.detailCoeffs(), returns);
            if (crash.probability > 0.7) {
                crashPredictions.add(crash);
            }
            
            // Forecast volatility using approximation coefficients
            VolatilityForecast volatility = forecastVolatility(
                result.approximationCoeffs(), returns);
            volatilityForecasts.add(volatility);
        }
        
        return new TradingSignals(crashPredictions, volatilityForecasts);
    }
    
    private CrashPrediction detectCrashPattern(double[][] details, double[] returns) {
        // Use finest scale details (level 1) for high-frequency crash detection
        double[] highFreqDetails = details[0];
        
        // Calculate asymmetry in wavelet coefficients
        double asymmetry = calculateAsymmetry(highFreqDetails);
        double magnitude = calculateMagnitude(highFreqDetails);
        
        // Crash probability based on asymmetric spikes
        double probability = Math.tanh(Math.abs(asymmetry) * magnitude);
        
        // Direction: negative asymmetry = crash down, positive = crash up
        CrashDirection direction = asymmetry < -0.1 ? CrashDirection.DOWN :
                                  asymmetry > 0.1 ? CrashDirection.UP :
                                  CrashDirection.NEUTRAL;
        
        return new CrashPrediction(probability, direction, System.currentTimeMillis());
    }
    
    private VolatilityForecast forecastVolatility(double[] approximation, double[] returns) {
        // Use approximation coefficients for volatility estimation
        double currentVolatility = analyzer.calculateVolatility(returns);
        
        // Wavelet-based volatility prediction
        double waveletEnergy = Arrays.stream(approximation)
            .map(x -> x * x)
            .sum();
        
        double predictedVolatility = currentVolatility * Math.sqrt(waveletEnergy / returns.length);
        
        return new VolatilityForecast(
            currentVolatility, 
            predictedVolatility, 
            System.currentTimeMillis() + 60000 // 1 minute forecast
        );
    }
    
    private double calculateAsymmetry(double[] coefficients) {
        double positiveSum = Arrays.stream(coefficients)
            .filter(x -> x > 0)
            .sum();
        double negativeSum = Math.abs(Arrays.stream(coefficients)
            .filter(x -> x < 0)
            .sum());
        
        return (positiveSum - negativeSum) / (positiveSum + negativeSum + 1e-10);
    }
    
    private double calculateMagnitude(double[] coefficients) {
        return Math.sqrt(Arrays.stream(coefficients)
            .map(x -> x * x)
            .sum() / coefficients.length);
    }
    
    // Supporting classes
    public static class TradingSignals {
        public final List<CrashPrediction> crashPredictions;
        public final List<VolatilityForecast> volatilityForecasts;
        
        public TradingSignals(List<CrashPrediction> crashes, 
                            List<VolatilityForecast> volatility) {
            this.crashPredictions = crashes;
            this.volatilityForecasts = volatility;
        }
    }
    
    public static class CrashPrediction {
        public final double probability;
        public final CrashDirection direction;
        public final long timestamp;
        
        public CrashPrediction(double probability, CrashDirection direction, long timestamp) {
            this.probability = probability;
            this.direction = direction;
            this.timestamp = timestamp;
        }
    }
    
    public static class VolatilityForecast {
        public final double currentVolatility;
        public final double predictedVolatility;
        public final long forecastTime;
        
        public VolatilityForecast(double current, double predicted, long forecastTime) {
            this.currentVolatility = current;
            this.predictedVolatility = predicted;
            this.forecastTime = forecastTime;
        }
    }
    
    public enum CrashDirection {
        DOWN, UP, NEUTRAL
    }
    
    private TradingSignals getCachedSignals() {
        // Return cached/default signals for timeout scenarios
        return new TradingSignals(Collections.emptyList(), Collections.emptyList());
    }
}
```

### Portfolio Risk Analysis

```java
import com.morphiqlabs.wavelet.parallel.StructuredParallelTransform;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.financial.FinancialWaveletAnalyzer;

public class PortfolioRiskAnalysisExample {
    
    private final StructuredParallelTransform transform;
    private final FinancialWaveletAnalyzer analyzer;
    
    public PortfolioRiskAnalysisExample(double riskFreeRate) {
        // Configure for portfolio-level analysis
        ParallelConfig config = ParallelConfig.builder()
            .targetCores(Runtime.getRuntime().availableProcessors())
            .minParallelThreshold(512)
            .build();
        
        this.transform = new StructuredParallelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, config);
        
        FinancialConfig financialConfig = new FinancialConfig(riskFreeRate);
        this.analyzer = new FinancialWaveletAnalyzer(financialConfig);
    }
    
    public PortfolioRiskMetrics analyzePortfolioRisk(Map<String, double[]> assetReturns) {
        // Convert to array format for batch processing
        List<String> assetNames = new ArrayList<>(assetReturns.keySet());
        double[][] returnsMatrix = assetNames.stream()
            .map(assetReturns::get)
            .toArray(double[][]::new);
        
        try {
            // Parallel wavelet analysis of all assets
            MODWTResult[] results = transform.forwardBatchWithProgress(returnsMatrix,
                (completed, total) -> {
                    System.out.printf("Risk analysis: %d/%d assets processed%n", 
                        completed, total);
                });
            
            // Calculate risk metrics for each asset
            Map<String, AssetRiskMetrics> assetRiskMetrics = new HashMap<>();
            
            for (int i = 0; i < assetNames.size(); i++) {
                String assetName = assetNames.get(i);
                double[] returns = returnsMatrix[i];
                MODWTResult result = results[i];
                
                AssetRiskMetrics metrics = calculateAssetRiskMetrics(
                    assetName, returns, result);
                assetRiskMetrics.put(assetName, metrics);
            }
            
            // Calculate portfolio-level risk metrics
            PortfolioRiskMetrics portfolioMetrics = calculatePortfolioRisk(
                assetRiskMetrics, returnsMatrix);
            
            return portfolioMetrics;
            
        } catch (StructuredParallelTransform.ComputationException e) {
            throw new RuntimeException("Portfolio risk analysis failed", e);
        }
    }
    
    private AssetRiskMetrics calculateAssetRiskMetrics(String assetName, 
                                                      double[] returns, 
                                                      MODWTResult result) {
        // Traditional risk metrics
        double volatility = analyzer.calculateVolatility(returns);
        double sharpeRatio = analyzer.calculateWaveletSharpeRatio(returns);
        
        // Wavelet-based risk metrics
        WaveletRiskMetrics waveletMetrics = calculateWaveletRiskMetrics(result, returns);
        
        return new AssetRiskMetrics(
            assetName,
            volatility,
            sharpeRatio,
            waveletMetrics.valueAtRisk95,
            waveletMetrics.expectedShortfall,
            waveletMetrics.tailRisk,
            waveletMetrics.downSideVolatility
        );
    }
    
    private WaveletRiskMetrics calculateWaveletRiskMetrics(MODWTResult result, 
                                                          double[] returns) {
        // Extract risk components from different frequency bands
        double[][] details = result.detailCoeffs();
        
        // High-frequency risk (daily fluctuations)
        double highFreqRisk = calculateBandRisk(details[0]);
        
        // Medium-frequency risk (weekly/monthly patterns)  
        double mediumFreqRisk = details.length > 1 ? calculateBandRisk(details[1]) : 0.0;
        
        // Low-frequency risk (long-term trends)
        double lowFreqRisk = details.length > 2 ? calculateBandRisk(details[2]) : 0.0;
        
        // Calculate percentile-based risk metrics
        double[] sortedReturns = Arrays.stream(returns)
            .sorted()
            .toArray();
        
        int var95Index = (int) Math.ceil(0.05 * sortedReturns.length) - 1;
        double valueAtRisk95 = -sortedReturns[var95Index]; // VaR is positive
        
        // Expected shortfall (average of losses beyond VaR)
        double expectedShortfall = -Arrays.stream(sortedReturns, 0, var95Index + 1)
            .average()
            .orElse(0.0);
        
        // Tail risk based on extreme detail coefficients
        double tailRisk = calculateTailRisk(details[0]);
        
        // Downside volatility (volatility of negative returns only)
        double downSideVolatility = Math.sqrt(Arrays.stream(returns)
            .filter(r -> r < 0)
            .map(r -> r * r)
            .average()
            .orElse(0.0));
        
        return new WaveletRiskMetrics(
            valueAtRisk95,
            expectedShortfall,
            tailRisk,
            downSideVolatility,
            highFreqRisk,
            mediumFreqRisk,
            lowFreqRisk
        );
    }
    
    private double calculateBandRisk(double[] bandCoefficients) {
        // Risk as energy in frequency band
        return Math.sqrt(Arrays.stream(bandCoefficients)
            .map(x -> x * x)
            .sum() / bandCoefficients.length);
    }
    
    private double calculateTailRisk(double[] coefficients) {
        // Focus on extreme coefficients (tails of distribution)
        double[] sortedCoeffs = Arrays.stream(coefficients)
            .map(Math::abs)
            .sorted()
            .toArray();
        
        int tailStart = (int) (0.95 * sortedCoeffs.length);
        return Arrays.stream(sortedCoeffs, tailStart, sortedCoeffs.length)
            .average()
            .orElse(0.0);
    }
    
    private PortfolioRiskMetrics calculatePortfolioRisk(
            Map<String, AssetRiskMetrics> assetMetrics,
            double[][] returnsMatrix) {
        
        // Calculate correlation matrix
        double[][] correlationMatrix = calculateCorrelationMatrix(returnsMatrix);
        
        // Portfolio volatility (simplified equal weighting)
        double portfolioVolatility = calculatePortfolioVolatility(assetMetrics, correlationMatrix);
        
        // Portfolio VaR
        double portfolioVaR = calculatePortfolioVaR(assetMetrics);
        
        // Concentration risk
        double concentrationRisk = calculateConcentrationRisk(assetMetrics);
        
        return new PortfolioRiskMetrics(
            portfolioVolatility,
            portfolioVaR,
            concentrationRisk,
            assetMetrics,
            correlationMatrix
        );
    }
    
    private double[][] calculateCorrelationMatrix(double[][] returnsMatrix) {
        int numAssets = returnsMatrix.length;
        double[][] correlations = new double[numAssets][numAssets];
        
        for (int i = 0; i < numAssets; i++) {
            for (int j = 0; j <= i; j++) {
                double correlation = calculateCorrelation(returnsMatrix[i], returnsMatrix[j]);
                correlations[i][j] = correlation;
                correlations[j][i] = correlation;
            }
        }
        
        return correlations;
    }
    
    private double calculateCorrelation(double[] returns1, double[] returns2) {
        double mean1 = Arrays.stream(returns1).average().orElse(0.0);
        double mean2 = Arrays.stream(returns2).average().orElse(0.0);
        
        double numerator = 0.0;
        double sumSq1 = 0.0;
        double sumSq2 = 0.0;
        
        for (int i = 0; i < returns1.length; i++) {
            double diff1 = returns1[i] - mean1;
            double diff2 = returns2[i] - mean2;
            
            numerator += diff1 * diff2;
            sumSq1 += diff1 * diff1;
            sumSq2 += diff2 * diff2;
        }
        
        double denominator = Math.sqrt(sumSq1 * sumSq2);
        return denominator > 1e-10 ? numerator / denominator : 0.0;
    }
    
    private double calculatePortfolioVolatility(Map<String, AssetRiskMetrics> assetMetrics,
                                               double[][] correlationMatrix) {
        // Simplified calculation assuming equal weights
        int numAssets = assetMetrics.size();
        double equalWeight = 1.0 / numAssets;
        
        double portfolioVariance = 0.0;
        List<AssetRiskMetrics> assets = new ArrayList<>(assetMetrics.values());
        
        for (int i = 0; i < numAssets; i++) {
            for (int j = 0; j < numAssets; j++) {
                double vol_i = assets.get(i).volatility;
                double vol_j = assets.get(j).volatility;
                double correlation = correlationMatrix[i][j];
                
                portfolioVariance += equalWeight * equalWeight * vol_i * vol_j * correlation;
            }
        }
        
        return Math.sqrt(portfolioVariance);
    }
    
    private double calculatePortfolioVaR(Map<String, AssetRiskMetrics> assetMetrics) {
        // Simplified portfolio VaR (assuming independence for demonstration)
        double equalWeight = 1.0 / assetMetrics.size();
        
        return assetMetrics.values().stream()
            .mapToDouble(metrics -> equalWeight * metrics.valueAtRisk95)
            .sum();
    }
    
    private double calculateConcentrationRisk(Map<String, AssetRiskMetrics> assetMetrics) {
        // Herfindahl-Hirschman Index for risk concentration
        double equalWeight = 1.0 / assetMetrics.size();
        return assetMetrics.size() * equalWeight * equalWeight; // HHI for equal weights
    }
    
    // Supporting classes
    private static class WaveletRiskMetrics {
        final double valueAtRisk95;
        final double expectedShortfall;
        final double tailRisk;
        final double downSideVolatility;
        final double highFreqRisk;
        final double mediumFreqRisk;
        final double lowFreqRisk;
        
        WaveletRiskMetrics(double var95, double es, double tail, double downside,
                          double highFreq, double mediumFreq, double lowFreq) {
            this.valueAtRisk95 = var95;
            this.expectedShortfall = es;
            this.tailRisk = tail;
            this.downSideVolatility = downside;
            this.highFreqRisk = highFreq;
            this.mediumFreqRisk = mediumFreq;
            this.lowFreqRisk = lowFreq;
        }
    }
    
    public static class AssetRiskMetrics {
        public final String assetName;
        public final double volatility;
        public final double sharpeRatio;
        public final double valueAtRisk95;
        public final double expectedShortfall;
        public final double tailRisk;
        public final double downSideVolatility;
        
        AssetRiskMetrics(String name, double vol, double sharpe, double var95, 
                        double es, double tail, double downside) {
            this.assetName = name;
            this.volatility = vol;
            this.sharpeRatio = sharpe;
            this.valueAtRisk95 = var95;
            this.expectedShortfall = es;
            this.tailRisk = tail;
            this.downSideVolatility = downside;
        }
    }
    
    public static class PortfolioRiskMetrics {
        public final double portfolioVolatility;
        public final double portfolioVaR;
        public final double concentrationRisk;
        public final Map<String, AssetRiskMetrics> assetMetrics;
        public final double[][] correlationMatrix;
        
        PortfolioRiskMetrics(double portfolioVol, double portfolioVaR, double concentration,
                           Map<String, AssetRiskMetrics> assets, double[][] correlations) {
            this.portfolioVolatility = portfolioVol;
            this.portfolioVaR = portfolioVaR;
            this.concentrationRisk = concentration;
            this.assetMetrics = assets;
            this.correlationMatrix = correlations;
        }
    }
}
```

## Real-Time Processing Examples

### Audio Stream Processing

```java
import com.morphiqlabs.wavelet.streaming.MODWTStreamingDenoiser;
import com.morphiqlabs.wavelet.parallel.StructuredParallelTransform;
import com.morphiqlabs.wavelet.api.Haar;

public class RealTimeAudioProcessingExample {
    
    private final MODWTStreamingDenoiser denoiser;
    private final StructuredParallelTransform batchTransform;
    private final BlockingQueue<double[]> audioQueue;
    private final ExecutorService processingService;
    
    public RealTimeAudioProcessingExample() {
        // Configure for low-latency audio processing
        this.denoiser = new MODWTStreamingDenoiser.Builder()
            .wavelet(new Haar())
            .bufferSize(1024)      // ~20ms at 48kHz
            .thresholdMethod(ThresholdMethod.UNIVERSAL)
            .softThresholding(true)
            .build();
        
        // Configure batch processing for non-real-time analysis
        ParallelConfig config = ParallelConfig.builder()
            .targetCores(Math.min(4, Runtime.getRuntime().availableProcessors()))
            .minParallelThreshold(512)
            .build();
        
        this.batchTransform = new StructuredParallelTransform(
            new Haar(), BoundaryMode.PERIODIC, config);
        
        this.audioQueue = new LinkedBlockingQueue<>(100); // 100-frame buffer
        this.processingService = Executors.newSingleThreadExecutor();
    }
    
    public void startRealTimeProcessing(AudioInputStream audioInput, 
                                       AudioOutputStream audioOutput) {
        // Start real-time processing thread
        Thread realTimeProcessor = new Thread(() -> {
            try {
                processRealTimeAudio(audioInput, audioOutput);
            } catch (Exception e) {
                System.err.println("Real-time processing error: " + e.getMessage());
            }
        });
        realTimeProcessor.setPriority(Thread.MAX_PRIORITY); // High priority for real-time
        realTimeProcessor.start();
        
        // Start batch analysis thread for quality monitoring
        processingService.submit(this::analyzeBatchQuality);
    }
    
    private void processRealTimeAudio(AudioInputStream input, AudioOutputStream output) 
            throws Exception {
        double[] buffer = new double[1024];
        
        while (!Thread.currentThread().isInterrupted()) {
            // Read audio frame
            int samplesRead = readAudioSamples(input, buffer);
            if (samplesRead <= 0) break;
            
            // Real-time denoising with timeout constraint
            long startTime = System.nanoTime();
            double[] denoisedFrame = denoiser.denoise(Arrays.copyOf(buffer, samplesRead));
            long processingTime = System.nanoTime() - startTime;
            
            // Monitor real-time performance
            double processingTimeMs = processingTime / 1e6;
            double frameTimeMs = (samplesRead * 1000.0) / 48000.0; // Assuming 48kHz
            
            if (processingTimeMs > frameTimeMs * 0.8) { // Using >80% of available time
                System.err.printf("WARNING: Processing time %.2fms exceeds %.2f%% of frame time %.2fms%n",
                    processingTimeMs, 80.0, frameTimeMs);
            }
            
            // Output processed audio
            writeAudioSamples(output, denoisedFrame);
            
            // Queue for batch analysis (non-blocking)
            audioQueue.offer(Arrays.copyOf(buffer, samplesRead));
        }
    }
    
    private void analyzeBatchQuality() {
        List<double[]> batchFrames = new ArrayList<>();
        
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Collect frames for batch analysis
                batchFrames.clear();
                
                // Collect up to 32 frames (about 0.7 seconds at 1024 samples/frame, 48kHz)
                for (int i = 0; i < 32; i++) {
                    double[] frame = audioQueue.poll(100, TimeUnit.MILLISECONDS);
                    if (frame != null) {
                        batchFrames.add(frame);
                    }
                    
                    if (Thread.currentThread().isInterrupted()) return;
                }
                
                if (batchFrames.isEmpty()) continue;
                
                // Batch analysis for quality assessment
                analyzeBatchAudioQuality(batchFrames);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void analyzeBatchAudioQuality(List<double[]> frames) {
        try {
            double[][] frameArray = frames.toArray(new double[0][]);
            
            // Batch transform for frequency analysis
            MODWTResult[] results = batchTransform.forwardBatchWithTimeout(frameArray, 1000);
            
            // Analyze frequency content
            AudioQualityMetrics quality = calculateAudioQuality(results);
            
            System.out.printf("Audio Quality: SNR=%.1fdB, Spectral_Flatness=%.3f, Dynamic_Range=%.1fdB%n",
                quality.signalToNoiseRatio, quality.spectralFlatness, quality.dynamicRange);
            
            // Adjust denoising parameters based on quality
            adaptDenoisingParameters(quality);
            
        } catch (StructuredParallelTransform.ComputationException e) {
            System.err.println("Batch audio analysis failed: " + e.getMessage());
        }
    }
    
    private AudioQualityMetrics calculateAudioQuality(MODWTResult[] results) {
        double totalEnergy = 0.0;
        double noiseEnergy = 0.0;
        double[] frequencyEnergies = new double[results[0].detailCoeffs().length + 1];
        
        for (MODWTResult result : results) {
            // Signal energy from approximation
            double signalEnergy = Arrays.stream(result.approximationCoeffs())
                .map(x -> x * x)
                .sum();
            totalEnergy += signalEnergy;
            frequencyEnergies[0] += signalEnergy;
            
            // Noise estimation from finest detail coefficients
            double[][] details = result.detailCoeffs();
            if (details.length > 0) {
                double frameNoiseEnergy = Arrays.stream(details[0])
                    .map(x -> x * x)
                    .sum();
                noiseEnergy += frameNoiseEnergy;
                frequencyEnergies[1] += frameNoiseEnergy;
                
                // Energy in each frequency band
                for (int level = 1; level < details.length && level < frequencyEnergies.length - 1; level++) {
                    double bandEnergy = Arrays.stream(details[level])
                        .map(x -> x * x)
                        .sum();
                    frequencyEnergies[level + 1] += bandEnergy;
                }
            }
        }
        
        // Calculate quality metrics
        double snr = 10.0 * Math.log10((totalEnergy - noiseEnergy) / (noiseEnergy + 1e-10));
        
        // Spectral flatness (measure of how noise-like vs tone-like)
        double geometricMean = Math.exp(Arrays.stream(frequencyEnergies)
            .filter(e -> e > 0)
            .mapToDouble(Math::log)
            .average()
            .orElse(0.0));
        double arithmeticMean = Arrays.stream(frequencyEnergies)
            .filter(e -> e > 0)
            .average()
            .orElse(1.0);
        double spectralFlatness = geometricMean / arithmeticMean;
        
        // Dynamic range
        double maxEnergy = Arrays.stream(frequencyEnergies).max().orElse(1.0);
        double minEnergy = Arrays.stream(frequencyEnergies)
            .filter(e -> e > 0)
            .min().orElse(1.0);
        double dynamicRange = 10.0 * Math.log10(maxEnergy / minEnergy);
        
        return new AudioQualityMetrics(snr, spectralFlatness, dynamicRange);
    }
    
    private void adaptDenoisingParameters(AudioQualityMetrics quality) {
        // Adapt denoising based on audio quality
        if (quality.signalToNoiseRatio < 20.0) {
            // High noise - increase denoising aggressiveness
            denoiser.setThresholdMultiplier(1.5);
        } else if (quality.signalToNoiseRatio > 40.0) {
            // Low noise - reduce denoising to preserve signal
            denoiser.setThresholdMultiplier(0.8);
        } else {
            // Normal noise level
            denoiser.setThresholdMultiplier(1.0);
        }
    }
    
    private int readAudioSamples(AudioInputStream input, double[] buffer) {
        // Placeholder for actual audio reading
        // In practice, convert byte samples to double
        try {
            byte[] byteBuffer = new byte[buffer.length * 2]; // 16-bit samples
            int bytesRead = input.read(byteBuffer);
            if (bytesRead <= 0) return bytesRead;
            
            int samplesRead = bytesRead / 2;
            for (int i = 0; i < samplesRead; i++) {
                // Convert 16-bit PCM to double (-1.0 to 1.0)
                short sample = (short) ((byteBuffer[i * 2 + 1] << 8) | (byteBuffer[i * 2] & 0xFF));
                buffer[i] = sample / 32768.0;
            }
            
            return samplesRead;
        } catch (Exception e) {
            return -1;
        }
    }
    
    private void writeAudioSamples(AudioOutputStream output, double[] samples) {
        // Placeholder for actual audio writing
        try {
            byte[] byteBuffer = new byte[samples.length * 2];
            for (int i = 0; i < samples.length; i++) {
                // Convert double (-1.0 to 1.0) to 16-bit PCM
                short sample = (short) (Math.max(-1.0, Math.min(1.0, samples[i])) * 32767.0);
                byteBuffer[i * 2] = (byte) (sample & 0xFF);
                byteBuffer[i * 2 + 1] = (byte) ((sample >> 8) & 0xFF);
            }
            output.write(byteBuffer);
        } catch (Exception e) {
            System.err.println("Audio output error: " + e.getMessage());
        }
    }
    
    public void shutdown() {
        processingService.shutdown();
        try {
            if (!processingService.awaitTermination(5, TimeUnit.SECONDS)) {
                processingService.shutdownNow();
            }
        } catch (InterruptedException e) {
            processingService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    private static class AudioQualityMetrics {
        final double signalToNoiseRatio;
        final double spectralFlatness;
        final double dynamicRange;
        
        AudioQualityMetrics(double snr, double flatness, double dynamicRange) {
            this.signalToNoiseRatio = snr;
            this.spectralFlatness = flatness;
            this.dynamicRange = dynamicRange;
        }
    }
    
    // Mock interfaces for demonstration
    interface AudioInputStream {
        int read(byte[] buffer) throws Exception;
    }
    
    interface AudioOutputStream {
        void write(byte[] buffer) throws Exception;
    }
}
```

## Performance Optimization Examples

### Adaptive Threshold Optimization

```java
import com.morphiqlabs.wavelet.parallel.AdaptiveThresholdTuner;
import com.morphiqlabs.wavelet.parallel.StructuredParallelTransform;

public class AdaptiveOptimizationExample {
    
    private final AdaptiveThresholdTuner tuner;
    private final Map<String, PerformanceHistory> performanceHistory;
    
    public AdaptiveOptimizationExample() {
        this.tuner = new AdaptiveThresholdTuner();
        this.performanceHistory = new ConcurrentHashMap<>();
    }
    
    public ProcessingResults processWithOptimization(WorkloadBatch workload) {
        String workloadId = workload.getId();
        
        // Get or create performance history for this workload type
        PerformanceHistory history = performanceHistory.computeIfAbsent(
            workloadId, k -> new PerformanceHistory());
        
        // Determine operation type based on workload
        AdaptiveThresholdTuner.OperationType operationType = 
            determineOperationType(workload);
        
        // Calculate complexity factor based on workload characteristics
        double complexityFactor = calculateComplexityFactor(workload);
        
        ProcessingResults results = null;
        Exception lastException = null;
        
        // Try multiple threshold strategies with fallback
        for (ThresholdStrategy strategy : ThresholdStrategy.values()) {
            try {
                int threshold = selectThreshold(strategy, workload, operationType, complexityFactor);
                
                // Configure with selected threshold
                ParallelConfig config = ParallelConfig.builder()
                    .targetCores(Runtime.getRuntime().availableProcessors())
                    .minParallelThreshold(threshold)
                    .build();
                
                StructuredParallelTransform transform = new StructuredParallelTransform(
                    workload.getWavelet(), workload.getBoundaryMode(), config);
                
                // Measure performance
                long startTime = System.nanoTime();
                
                MODWTResult[] transformResults = transform.forwardBatchWithTimeout(
                    workload.getSignals(), workload.getTimeoutMs());
                
                long elapsed = System.nanoTime() - startTime;
                
                // Record successful measurement
                if (strategy == ThresholdStrategy.ADAPTIVE) {
                    tuner.recordMeasurement(
                        operationType,
                        workload.getAverageSignalLength(),
                        threshold,
                        elapsed,
                        history.getEstimatedSequentialTime()
                    );
                }
                
                // Update performance history
                history.recordMeasurement(threshold, elapsed, transformResults.length);
                
                results = new ProcessingResults(
                    transformResults, 
                    elapsed, 
                    threshold, 
                    strategy,
                    calculateEfficiency(elapsed, history.getEstimatedSequentialTime(), 
                                      Runtime.getRuntime().availableProcessors())
                );
                
                break; // Success - exit retry loop
                
            } catch (Exception e) {
                lastException = e;
                System.err.printf("Threshold strategy %s failed: %s%n", 
                    strategy, e.getMessage());
                
                // Continue to next strategy
                continue;
            }
        }
        
        if (results == null) {
            throw new RuntimeException("All threshold strategies failed", lastException);
        }
        
        // Log optimization results
        logOptimizationResults(workloadId, results, history);
        
        return results;
    }
    
    private int selectThreshold(ThresholdStrategy strategy, WorkloadBatch workload,
                               AdaptiveThresholdTuner.OperationType operationType,
                               double complexityFactor) {
        return switch (strategy) {
            case ADAPTIVE -> {
                // Use machine learning optimization
                int adaptiveThreshold = tuner.getAdaptiveThreshold(
                    operationType, workload.getAverageSignalLength(), complexityFactor);
                
                // Validate threshold is reasonable
                yield Math.max(64, Math.min(8192, adaptiveThreshold));
            }
            
            case CONSERVATIVE -> {
                // Higher threshold - less parallelization, lower overhead
                yield Math.max(1024, workload.getAverageSignalLength() / 4);
            }
            
            case AGGRESSIVE -> {
                // Lower threshold - more parallelization
                yield Math.max(128, workload.getAverageSignalLength() / 16);
            }
            
            case DEFAULT -> {
                // Standard threshold based on signal characteristics
                if (workload.getAverageSignalLength() < 512) {
                    yield 256;
                } else if (workload.getAverageSignalLength() < 2048) {
                    yield 512;
                } else {
                    yield 1024;
                }
            }
            
            case SEQUENTIAL -> {
                // Force sequential processing
                yield Integer.MAX_VALUE;
            }
        };
    }
    
    private AdaptiveThresholdTuner.OperationType determineOperationType(WorkloadBatch workload) {
        // Determine operation type based on workload characteristics
        if (workload.getDescription().toLowerCase().contains("financial")) {
            return AdaptiveThresholdTuner.OperationType.CWT_ANALYSIS;
        } else if (workload.getDescription().toLowerCase().contains("denoising")) {
            return AdaptiveThresholdTuner.OperationType.MODWT_DECOMPOSE;
        } else {
            return AdaptiveThresholdTuner.OperationType.MODWT_DECOMPOSE;
        }
    }
    
    private double calculateComplexityFactor(WorkloadBatch workload) {
        double baseFactor = 1.0;
        
        // Adjust for wavelet complexity
        String waveletName = workload.getWavelet().getName().toLowerCase();
        if (waveletName.contains("haar")) {
            baseFactor *= 0.5; // Haar is simple
        } else if (waveletName.contains("bior")) {
            baseFactor *= 1.2; // Biorthogonal more complex
        } else if (waveletName.contains("coif")) {
            baseFactor *= 1.3; // Coiflets most complex
        }
        
        // Adjust for boundary mode
        if (workload.getBoundaryMode() == BoundaryMode.ZERO_PADDING) {
            baseFactor *= 1.1; // Zero padding slightly more work
        }
        
        // Adjust for signal characteristics
        double signalComplexity = estimateSignalComplexity(workload.getSignals());
        baseFactor *= (0.8 + 0.4 * signalComplexity); // Scale between 0.8-1.2
        
        return baseFactor;
    }
    
    private double estimateSignalComplexity(double[][] signals) {
        // Estimate complexity based on signal variability
        if (signals.length == 0) return 1.0;
        
        double totalVariance = 0.0;
        for (double[] signal : signals) {
            double mean = Arrays.stream(signal).average().orElse(0.0);
            double variance = Arrays.stream(signal)
                .map(x -> Math.pow(x - mean, 2))
                .average()
                .orElse(0.0);
            totalVariance += variance;
        }
        
        double avgVariance = totalVariance / signals.length;
        
        // Normalize complexity to 0-1 range (higher variance = higher complexity)
        return Math.tanh(avgVariance); // tanh provides smooth 0-1 mapping
    }
    
    private double calculateEfficiency(long actualTime, long estimatedSequentialTime, int cores) {
        if (estimatedSequentialTime <= 0) return 0.0;
        
        double speedup = (double) estimatedSequentialTime / actualTime;
        return speedup / cores; // Efficiency = Speedup / Cores
    }
    
    private void logOptimizationResults(String workloadId, ProcessingResults results, 
                                       PerformanceHistory history) {
        System.out.printf("Optimization Results for %s:%n", workloadId);
        System.out.printf("  Strategy: %s, Threshold: %d%n", 
            results.strategy, results.threshold);
        System.out.printf("  Processing Time: %.2f ms%n", 
            results.processingTime / 1e6);
        System.out.printf("  Efficiency: %.1f%%%n", 
            results.efficiency * 100);
        System.out.printf("  Historical Performance: %d measurements, avg efficiency %.1f%%%n",
            history.getMeasurementCount(), history.getAverageEfficiency() * 100);
        
        // Check if this is a new best performance
        if (results.efficiency > history.getBestEfficiency()) {
            System.out.printf("  *** NEW BEST EFFICIENCY: %.1f%% ***%n", 
                results.efficiency * 100);
            history.updateBest(results.threshold, results.efficiency);
        }
    }
    
    // Supporting classes
    public enum ThresholdStrategy {
        ADAPTIVE,    // Use machine learning optimization
        CONSERVATIVE, // High threshold, low overhead
        AGGRESSIVE,  // Low threshold, high parallelization
        DEFAULT,     // Standard threshold
        SEQUENTIAL   // Force sequential processing
    }
    
    public static class ProcessingResults {
        public final MODWTResult[] results;
        public final long processingTime;
        public final int threshold;
        public final ThresholdStrategy strategy;
        public final double efficiency;
        
        ProcessingResults(MODWTResult[] results, long time, int threshold, 
                         ThresholdStrategy strategy, double efficiency) {
            this.results = results;
            this.processingTime = time;
            this.threshold = threshold;
            this.strategy = strategy;
            this.efficiency = efficiency;
        }
    }
    
    private static class PerformanceHistory {
        private final List<PerformanceMeasurement> measurements = new ArrayList<>();
        private long estimatedSequentialTime = 0L;
        private int bestThreshold = 512;
        private double bestEfficiency = 0.0;
        
        void recordMeasurement(int threshold, long time, int resultsCount) {
            synchronized (measurements) {
                measurements.add(new PerformanceMeasurement(threshold, time, resultsCount));
                
                // Update estimated sequential time (use worst parallel time as conservative estimate)
                if (estimatedSequentialTime == 0L) {
                    estimatedSequentialTime = time * 2; // Conservative estimate
                }
            }
        }
        
        long getEstimatedSequentialTime() {
            return estimatedSequentialTime;
        }
        
        int getMeasurementCount() {
            synchronized (measurements) {
                return measurements.size();
            }
        }
        
        double getAverageEfficiency() {
            synchronized (measurements) {
                if (measurements.isEmpty()) return 0.0;
                
                return measurements.stream()
                    .mapToDouble(m -> calculateEfficiency(m.time, estimatedSequentialTime, 
                                                        Runtime.getRuntime().availableProcessors()))
                    .average()
                    .orElse(0.0);
            }
        }
        
        double getBestEfficiency() {
            return bestEfficiency;
        }
        
        void updateBest(int threshold, double efficiency) {
            if (efficiency > bestEfficiency) {
                bestThreshold = threshold;
                bestEfficiency = efficiency;
            }
        }
        
        private static class PerformanceMeasurement {
            final int threshold;
            final long time;
            final int resultsCount;
            
            PerformanceMeasurement(int threshold, long time, int count) {
                this.threshold = threshold;
                this.time = time;
                this.resultsCount = count;
            }
        }
    }
    
    public static class WorkloadBatch {
        private final String id;
        private final String description;
        private final double[][] signals;
        private final Wavelet wavelet;
        private final BoundaryMode boundaryMode;
        private final long timeoutMs;
        
        public WorkloadBatch(String id, String description, double[][] signals, 
                           Wavelet wavelet, BoundaryMode boundaryMode, long timeoutMs) {
            this.id = id;
            this.description = description;
            this.signals = signals;
            this.wavelet = wavelet;
            this.boundaryMode = boundaryMode;
            this.timeoutMs = timeoutMs;
        }
        
        public String getId() { return id; }
        public String getDescription() { return description; }
        public double[][] getSignals() { return signals; }
        public Wavelet getWavelet() { return wavelet; }
        public BoundaryMode getBoundaryMode() { return boundaryMode; }
        public long getTimeoutMs() { return timeoutMs; }
        
        public int getAverageSignalLength() {
            return signals.length > 0 ? 
                Arrays.stream(signals).mapToInt(s -> s.length).sum() / signals.length : 0;
        }
    }
}
```

## Error Handling Examples

### Comprehensive Error Recovery

```java
import com.morphiqlabs.wavelet.parallel.StructuredParallelTransform;
import com.morphiqlabs.wavelet.exception.WaveletException;

public class ErrorHandlingExample {
    
    private final StructuredParallelTransform primaryTransform;
    private final StructuredParallelTransform fallbackTransform;
    private final ExecutorService fallbackExecutor;
    
    public ErrorHandlingExample() {
        // Primary configuration - optimized for performance
        ParallelConfig primaryConfig = ParallelConfig.builder()
            .targetCores(Runtime.getRuntime().availableProcessors())
            .minParallelThreshold(256)
            .build();
        
        // Fallback configuration - conservative, reliable
        ParallelConfig fallbackConfig = ParallelConfig.builder()
            .targetCores(Math.min(2, Runtime.getRuntime().availableProcessors()))
            .minParallelThreshold(2048) // Higher threshold, less parallelization
            .build();
        
        this.primaryTransform = new StructuredParallelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, primaryConfig);
        
        this.fallbackTransform = new StructuredParallelTransform(
            new Haar(), BoundaryMode.PERIODIC, fallbackConfig); // Simpler wavelet
        
        this.fallbackExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "Fallback-Executor");
            t.setDaemon(true);
            return t;
        });
    }
    
    public ProcessingResult processWithRecovery(ProcessingRequest request) {
        List<RecoveryAttempt> attempts = new ArrayList<>();
        
        // Strategy 1: Primary parallel processing
        RecoveryAttempt primaryAttempt = attemptPrimaryProcessing(request);
        attempts.add(primaryAttempt);
        if (primaryAttempt.success) {
            return primaryAttempt.result;
        }
        
        // Strategy 2: Fallback parallel processing
        RecoveryAttempt fallbackAttempt = attemptFallbackProcessing(request);
        attempts.add(fallbackAttempt);
        if (fallbackAttempt.success) {
            return fallbackAttempt.result;
        }
        
        // Strategy 3: Sequential processing
        RecoveryAttempt sequentialAttempt = attemptSequentialProcessing(request);
        attempts.add(sequentialAttempt);
        if (sequentialAttempt.success) {
            return sequentialAttempt.result;
        }
        
        // Strategy 4: Partial processing (best effort)
        RecoveryAttempt partialAttempt = attemptPartialProcessing(request);
        attempts.add(partialAttempt);
        
        if (partialAttempt.success && partialAttempt.result.getResultCount() > 0) {
            // Return partial results with warning
            return new ProcessingResult(
                partialAttempt.result.getResults(),
                ProcessingStatus.PARTIAL_SUCCESS,
                "Partial processing completed: " + partialAttempt.result.getResultCount() + 
                "/" + request.getSignals().length + " signals processed",
                attempts
            );
        }
        
        // All strategies failed
        return new ProcessingResult(
            new MODWTResult[0],
            ProcessingStatus.COMPLETE_FAILURE,
            "All recovery strategies failed",
            attempts
        );
    }
    
    private RecoveryAttempt attemptPrimaryProcessing(ProcessingRequest request) {
        try {
            System.out.println("Attempting primary parallel processing...");
            
            long startTime = System.nanoTime();
            MODWTResult[] results = primaryTransform.forwardBatchWithTimeout(
                request.getSignals(), request.getTimeoutMs());
            long elapsed = System.nanoTime() - startTime;
            
            return new RecoveryAttempt(
                "Primary Parallel",
                true,
                null,
                new ProcessingResult(results, ProcessingStatus.SUCCESS, 
                    String.format("Primary processing completed in %.2fms", elapsed / 1e6),
                    Collections.emptyList()),
                elapsed
            );
            
        } catch (StructuredParallelTransform.ComputationException e) {
            System.err.println("Primary processing failed: " + e.getMessage());
            
            return new RecoveryAttempt(
                "Primary Parallel",
                false,
                e,
                null,
                0L
            );
        }
    }
    
    private RecoveryAttempt attemptFallbackProcessing(ProcessingRequest request) {
        try {
            System.out.println("Attempting fallback parallel processing...");
            
            // Use longer timeout for fallback
            long fallbackTimeout = Math.max(request.getTimeoutMs() * 2, 30000L);
            
            long startTime = System.nanoTime();
            MODWTResult[] results = fallbackTransform.forwardBatchWithTimeout(
                request.getSignals(), fallbackTimeout);
            long elapsed = System.nanoTime() - startTime;
            
            return new RecoveryAttempt(
                "Fallback Parallel",
                true,
                null,
                new ProcessingResult(results, ProcessingStatus.SUCCESS,
                    String.format("Fallback processing completed in %.2fms", elapsed / 1e6),
                    Collections.emptyList()),
                elapsed
            );
            
        } catch (Exception e) {
            System.err.println("Fallback processing failed: " + e.getMessage());
            
            return new RecoveryAttempt(
                "Fallback Parallel",
                false,
                e,
                null,
                0L
            );
        }
    }
    
    private RecoveryAttempt attemptSequentialProcessing(ProcessingRequest request) {
        try {
            System.out.println("Attempting sequential processing...");
            
            // Create sequential transform (single-threaded)
            MODWTTransform sequentialTransform = new MODWTTransform(
                new Haar(), BoundaryMode.PERIODIC);
            
            List<MODWTResult> results = new ArrayList<>();
            long startTime = System.nanoTime();
            
            for (double[] signal : request.getSignals()) {
                // Check for cancellation/timeout
                long elapsed = System.nanoTime() - startTime;
                if (elapsed > request.getTimeoutMs() * 1_000_000L) {
                    throw new RuntimeException("Sequential processing timeout");
                }
                
                try {
                    MODWTResult result = sequentialTransform.forward(signal);
                    results.add(result);
                } catch (Exception e) {
                    System.err.printf("Failed to process signal %d sequentially: %s%n", 
                        results.size(), e.getMessage());
                    // Continue with remaining signals
                }
            }
            
            long totalElapsed = System.nanoTime() - startTime;
            
            if (results.isEmpty()) {
                throw new RuntimeException("No signals processed successfully");
            }
            
            return new RecoveryAttempt(
                "Sequential",
                true,
                null,
                new ProcessingResult(results.toArray(new MODWTResult[0]), 
                    results.size() == request.getSignals().length ? 
                        ProcessingStatus.SUCCESS : ProcessingStatus.PARTIAL_SUCCESS,
                    String.format("Sequential processing: %d/%d signals in %.2fms", 
                        results.size(), request.getSignals().length, totalElapsed / 1e6),
                    Collections.emptyList()),
                totalElapsed
            );
            
        } catch (Exception e) {
            System.err.println("Sequential processing failed: " + e.getMessage());
            
            return new RecoveryAttempt(
                "Sequential",
                false,
                e,
                null,
                0L
            );
        }
    }
    
    private RecoveryAttempt attemptPartialProcessing(ProcessingRequest request) {
        try {
            System.out.println("Attempting partial processing (best effort)...");
            
            List<MODWTResult> results = new ArrayList<>();
            List<Exception> errors = new ArrayList<>();
            
            // Process signals individually with error isolation
            MODWTTransform isolatedTransform = new MODWTTransform(
                new Haar(), BoundaryMode.PERIODIC);
            
            long startTime = System.nanoTime();
            
            for (int i = 0; i < request.getSignals().length; i++) {
                double[] signal = request.getSignals()[i];
                
                try {
                    // Individual timeout per signal
                    CompletableFuture<MODWTResult> future = CompletableFuture
                        .supplyAsync(() -> isolatedTransform.forward(signal), fallbackExecutor);
                    
                    MODWTResult result = future.get(5, TimeUnit.SECONDS); // 5s per signal
                    results.add(result);
                    
                } catch (Exception e) {
                    errors.add(new RuntimeException("Signal " + i + " failed: " + e.getMessage(), e));
                    System.err.printf("Signal %d failed: %s%n", i, e.getMessage());
                    // Continue processing other signals
                }
                
                // Check overall timeout
                long elapsed = System.nanoTime() - startTime;
                if (elapsed > request.getTimeoutMs() * 1_000_000L * 2) { // 2x timeout for partial
                    System.out.println("Partial processing timeout - stopping");
                    break;
                }
            }
            
            long totalElapsed = System.nanoTime() - startTime;
            
            String message = String.format(
                "Partial processing: %d/%d signals successful, %d errors in %.2fms",
                results.size(), request.getSignals().length, errors.size(), totalElapsed / 1e6);
            
            return new RecoveryAttempt(
                "Partial",
                !results.isEmpty(), // Success if we got at least some results
                errors.isEmpty() ? null : errors.get(0), // Report first error
                new ProcessingResult(results.toArray(new MODWTResult[0]),
                    results.isEmpty() ? ProcessingStatus.COMPLETE_FAILURE :
                    results.size() == request.getSignals().length ? ProcessingStatus.SUCCESS :
                    ProcessingStatus.PARTIAL_SUCCESS,
                    message,
                    Collections.emptyList()),
                totalElapsed
            );
            
        } catch (Exception e) {
            System.err.println("Partial processing failed: " + e.getMessage());
            
            return new RecoveryAttempt(
                "Partial",
                false,
                e,
                null,
                0L
            );
        }
    }
    
    public void shutdown() {
        fallbackExecutor.shutdown();
        try {
            if (!fallbackExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                fallbackExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            fallbackExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Supporting classes
    public static class ProcessingRequest {
        private final double[][] signals;
        private final long timeoutMs;
        private final String description;
        
        public ProcessingRequest(double[][] signals, long timeoutMs, String description) {
            this.signals = signals;
            this.timeoutMs = timeoutMs;
            this.description = description;
        }
        
        public double[][] getSignals() { return signals; }
        public long getTimeoutMs() { return timeoutMs; }
        public String getDescription() { return description; }
    }
    
    public static class ProcessingResult {
        private final MODWTResult[] results;
        private final ProcessingStatus status;
        private final String message;
        private final List<RecoveryAttempt> recoveryAttempts;
        
        ProcessingResult(MODWTResult[] results, ProcessingStatus status, String message,
                        List<RecoveryAttempt> attempts) {
            this.results = results;
            this.status = status;
            this.message = message;
            this.recoveryAttempts = new ArrayList<>(attempts);
        }
        
        public MODWTResult[] getResults() { return results; }
        public ProcessingStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public List<RecoveryAttempt> getRecoveryAttempts() { return recoveryAttempts; }
        public int getResultCount() { return results.length; }
    }
    
    public enum ProcessingStatus {
        SUCCESS,
        PARTIAL_SUCCESS,
        COMPLETE_FAILURE
    }
    
    private static class RecoveryAttempt {
        final String strategy;
        final boolean success;
        final Exception error;
        final ProcessingResult result;
        final long elapsedTime;
        
        RecoveryAttempt(String strategy, boolean success, Exception error, 
                       ProcessingResult result, long elapsedTime) {
            this.strategy = strategy;
            this.success = success;
            this.error = error;
            this.result = result;
            this.elapsedTime = elapsedTime;
        }
        
        @Override
        public String toString() {
            return String.format("%s: %s in %.2fms%s", 
                strategy, success ? "SUCCESS" : "FAILED", elapsedTime / 1e6,
                error != null ? " (" + error.getMessage() + ")" : "");
        }
    }
}
```

## Integration Examples

### Spring Boot Integration

```java
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

@Configuration
@EnableConfigurationProperties(WaveletProperties.class)
public class WaveletConfiguration {
    
    @Bean
    @ConditionalOnProperty(name = "wavelet.structured-concurrency.enabled", havingValue = "true")
    public StructuredParallelTransformService structuredTransformService(WaveletProperties properties) {
        return new StructuredParallelTransformService(properties);
    }
    
    @Bean
    public AdaptiveThresholdTuner adaptiveThresholdTuner() {
        return new AdaptiveThresholdTuner();
    }
}

@ConfigurationProperties(prefix = "wavelet")
public class WaveletProperties {
    
    private StructuredConcurrency structuredConcurrency = new StructuredConcurrency();
    private Performance performance = new Performance();
    
    // Getters and setters...
    
    public static class StructuredConcurrency {
        private boolean enabled = true;
        private int targetCores = Runtime.getRuntime().availableProcessors();
        private int minParallelThreshold = 512;
        private int maxChunkSize = 4096;
        private long defaultTimeoutMs = 30000L;
        
        // Getters and setters...
    }
    
    public static class Performance {
        private boolean adaptiveThresholdingEnabled = true;
        private boolean performanceMonitoringEnabled = false;
        private int measurementHistorySize = 100;
        
        // Getters and setters...
    }
}

@Service
public class StructuredParallelTransformService {
    
    private final StructuredParallelTransform defaultTransform;
    private final AdaptiveThresholdTuner tuner;
    private final WaveletProperties properties;
    private final MeterRegistry meterRegistry;
    
    public StructuredParallelTransformService(WaveletProperties properties,
                                            AdaptiveThresholdTuner tuner,
                                            MeterRegistry meterRegistry) {
        this.properties = properties;
        this.tuner = tuner;
        this.meterRegistry = meterRegistry;
        
        ParallelConfig config = ParallelConfig.builder()
            .targetCores(properties.getStructuredConcurrency().getTargetCores())
            .minParallelThreshold(properties.getStructuredConcurrency().getMinParallelThreshold())
            .maxChunkSize(properties.getStructuredConcurrency().getMaxChunkSize())
            .build();
        
        this.defaultTransform = new StructuredParallelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, config);
    }
    
    @Async
    public CompletableFuture<ProcessingResponse> processSignalsAsync(ProcessingRequest request) {
        Timer.Sample sample = Timer.start(meterRegistry);
        
        try {
            MODWTResult[] results;
            
            if (properties.getPerformance().isAdaptiveThresholdingEnabled()) {
                results = processWithAdaptiveThresholding(request);
            } else {
                results = defaultTransform.forwardBatchWithTimeout(
                    request.getSignals(), 
                    request.getTimeoutMs().orElse(properties.getStructuredConcurrency().getDefaultTimeoutMs())
                );
            }
            
            meterRegistry.counter("wavelet.processing.success", 
                "signals", String.valueOf(request.getSignals().length))
                .increment();
            
            return CompletableFuture.completedFuture(
                new ProcessingResponse(results, ProcessingStatus.SUCCESS, "Processing completed"));
            
        } catch (Exception e) {
            meterRegistry.counter("wavelet.processing.failure", 
                "error", e.getClass().getSimpleName())
                .increment();
            
            return CompletableFuture.completedFuture(
                new ProcessingResponse(new MODWTResult[0], ProcessingStatus.FAILURE, e.getMessage()));
        } finally {
            sample.stop(Timer.builder("wavelet.processing.duration")
                .register(meterRegistry));
        }
    }
    
    private MODWTResult[] processWithAdaptiveThresholding(ProcessingRequest request) {
        // Implementation similar to AdaptiveOptimizationExample
        // ... (adaptive threshold selection and processing)
        return new MODWTResult[0]; // Placeholder
    }
}

@RestController
@RequestMapping("/api/wavelet")
public class WaveletController {
    
    private final StructuredParallelTransformService transformService;
    
    @PostMapping("/transform")
    public ResponseEntity<ProcessingResponse> transformSignals(
            @RequestBody TransformRequest request) {
        
        try {
            // Convert request to internal format
            ProcessingRequest processingRequest = convertRequest(request);
            
            // Process asynchronously
            CompletableFuture<ProcessingResponse> future = 
                transformService.processSignalsAsync(processingRequest);
            
            // Wait for completion with timeout
            ProcessingResponse response = future.get(60, TimeUnit.SECONDS);
            
            return ResponseEntity.ok(response);
            
        } catch (TimeoutException e) {
            return ResponseEntity.status(HttpStatus.REQUEST_TIMEOUT)
                .body(new ProcessingResponse(new MODWTResult[0], 
                    ProcessingStatus.TIMEOUT, "Processing timed out"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ProcessingResponse(new MODWTResult[0], 
                    ProcessingStatus.FAILURE, e.getMessage()));
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<HealthStatus> healthCheck() {
        // Check system health and performance
        WaveletOperations.PerformanceInfo perfInfo = WaveletOperations.getPerformanceInfo();
        
        return ResponseEntity.ok(new HealthStatus(
            true,
            "Wavelet processing service is healthy",
            perfInfo.description(),
            Runtime.getRuntime().availableProcessors()
        ));
    }
    
    private ProcessingRequest convertRequest(TransformRequest request) {
        // Convert REST request to internal processing request
        return new ProcessingRequest(
            request.getSignals(),
            request.getTimeoutMs(),
            request.getDescription()
        );
    }
}
```

This comprehensive example collection demonstrates the full range of structured concurrency capabilities in VectorWave, from basic batch processing to sophisticated error recovery and integration with enterprise frameworks.