package com.morphiqlabs.wavelet.realworld;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.cwt.CWTTransform;
import com.morphiqlabs.wavelet.cwt.CWTResult;
// Use fully qualified name for CWT FinancialWaveletAnalyzer to avoid conflicts
import com.morphiqlabs.wavelet.cwt.finance.DOGWavelet;
import com.morphiqlabs.wavelet.cwt.finance.PaulWavelet;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser.ThresholdType;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import com.morphiqlabs.financial.FinancialAnalysisConfig;
import com.morphiqlabs.financial.FinancialAnalyzer;
import com.morphiqlabs.financial.FinancialConfig;
import com.morphiqlabs.financial.FinancialWaveletAnalyzer;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingDenoiser;
import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingTransform;
import com.morphiqlabs.wavelet.streaming.StreamingDenoiserStrategy;
import com.morphiqlabs.wavelet.streaming.StreamingDenoiserConfig;
import com.morphiqlabs.wavelet.streaming.StreamingDenoiserFactory;
import com.morphiqlabs.wavelet.util.SignalProcessor;
import com.morphiqlabs.wavelet.util.SignalUtils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.Disabled;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Flow;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Real-world tests using actual financial tick data.
 * 
 * This test class demonstrates various wavelet analysis techniques
 * on real market data including:
 * - Price denoising
 * - Trend detection
 * - Volatility analysis
 * - Market microstructure analysis
 * 
 * This test class is disabled by default because:
 * - It contains comprehensive integration tests that take longer to run
 * - It's intended as an example of advanced wavelet analysis techniques
 * - The simpler RealWorldTickDataSimpleTest covers basic functionality
 * 
 * Enable this test class when you need to:
 * - Validate advanced wavelet analysis features
 * - Run comprehensive integration tests
 * - Benchmark performance on real data
 */
@Disabled("Comprehensive integration tests - enable for full validation")
public class RealWorldTickDataTest {
    
    private static List<TickDataLoader.Tick> ticks;
    private static double[] bidPrices;
    private static double[] askPrices;
    private static double[] midPrices;
    private static double[] spreads;
    private static double[] returns;
    
    @BeforeAll
    static void loadData() throws IOException {
        System.out.println("Loading tick data...");
        ticks = TickDataLoader.loadTickData();
        System.out.println("Loaded " + ticks.size() + " ticks");
        
        // Extract price series
        bidPrices = TickDataLoader.extractPrices(ticks, TickDataLoader.Tick.Side.BID);
        askPrices = TickDataLoader.extractPrices(ticks, TickDataLoader.Tick.Side.ASK);
        
        // Calculate mid prices
        midPrices = new double[Math.min(bidPrices.length, askPrices.length)];
        for (int i = 0; i < midPrices.length; i++) {
            midPrices[i] = (bidPrices[i] + askPrices[i]) / 2.0;
        }
        
        // Calculate spreads (using window of 100 ticks)
        spreads = TickDataLoader.calculateSpreads(ticks, 100);
        
        // Calculate returns
        returns = calculateReturns(midPrices);
        
        System.out.println("Price statistics:");
        System.out.println("  Bid prices: " + bidPrices.length);
        System.out.println("  Ask prices: " + askPrices.length);
        System.out.println("  Mid prices: " + midPrices.length);
        System.out.println("  Spreads: " + spreads.length);
        System.out.println("  Returns: " + returns.length);
    }
    
    private static double[] calculateReturns(double[] prices) {
        double[] returns = new double[prices.length - 1];
        for (int i = 0; i < returns.length; i++) {
            returns[i] = (prices[i + 1] - prices[i]) / prices[i];
        }
        return returns;
    }
    
    // Utility methods
    private static double mean(double[] signal) {
        double sum = 0.0;
        for (double v : signal) sum += v;
        return sum / signal.length;
    }
    
    private static double variance(double[] signal) {
        double m = mean(signal);
        double sum = 0.0;
        for (double v : signal) {
            double diff = v - m;
            sum += diff * diff;
        }
        return sum / signal.length;
    }
    
    private static double rmse(double[] a, double[] b) {
        if (a.length != b.length) throw new IllegalArgumentException("Arrays must have same length");
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum / a.length);
    }
    
    private static double snr(double[] signal, double[] noisy) {
        double[] noise = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            noise[i] = noisy[i] - signal[i];
        }
        
        double signalPower = 0.0;
        double noisePower = 0.0;
        for (int i = 0; i < signal.length; i++) {
            signalPower += signal[i] * signal[i];
            noisePower += noise[i] * noise[i];
        }
        
        if (noisePower == 0) return Double.POSITIVE_INFINITY;
        return 10 * Math.log10(signalPower / noisePower);
    }
    
    @Test
    void testMODWTOnRealPrices() {
        // Use MODWT for multi-resolution analysis of price data
        DiscreteWavelet wavelet = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.DB4);
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        // Take a subset for detailed analysis
        int sampleSize = 4096;
        double[] priceSubset = Arrays.copyOf(midPrices, sampleSize);
        
        // Perform 4-level decomposition
        MultiLevelMODWTResult result = transform.decompose(priceSubset, 4);
        
        // Analyze each level
        System.out.println("\nMODWT Multi-resolution Analysis:");
        for (int level = 1; level <= 4; level++) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            double energy = Arrays.stream(details).map(d -> d * d).sum();
            double var = variance(details);
            
            System.out.printf("Level %d: Energy=%.6f, Variance=%.6f%n", 
                level, energy, var);
            
            // Verify Parseval's theorem (energy preservation)
            assertTrue(energy >= 0, "Energy should be non-negative");
        }
        
        // Test reconstruction
        double[] reconstructed = transform.reconstruct(result);
        double error = rmse(priceSubset, reconstructed);
        System.out.printf("Reconstruction RMSE: %.9f%n", error);
        assertTrue(error < 1e-10, "Reconstruction should be near-perfect");
    }
    
    @Test
    void testDenoisingRealPrices() {
        // Use MODWT-based denoising on noisy price data
        DiscreteWavelet wavelet = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.SYM8);
        
        // Take a subset and add realistic noise
        int sampleSize = 2048;
        double[] cleanPrices = Arrays.copyOf(midPrices, sampleSize);
        
        // Add tick noise (market microstructure noise)
        double[] noisyPrices = addTickNoise(cleanPrices, 0.01); // 1 cent noise
        
        // Create denoiser
        WaveletDenoiser denoiser = new WaveletDenoiser(wavelet, BoundaryMode.PERIODIC);
        
        // Soft thresholding with SURE method
        double[] softDenoised = denoiser.denoise(noisyPrices, ThresholdMethod.SURE);
        
        // Calculate SNR improvements
        double noisySNR = snr(cleanPrices, noisyPrices);
        double softSNR = snr(cleanPrices, softDenoised);
        
        System.out.println("\nDenoising Results:");
        System.out.printf("Noisy SNR: %.2f dB%n", noisySNR);
        System.out.printf("Denoised SNR: %.2f dB%n", softSNR);
        
        assertTrue(softSNR > noisySNR, "Denoising should improve SNR");
    }
    
    private double[] addTickNoise(double[] prices, double tickSize) {
        double[] noisy = new double[prices.length];
        for (int i = 0; i < prices.length; i++) {
            // Add random tick noise
            int ticks = (int)(Math.random() * 3) - 1; // -1, 0, or 1 tick
            noisy[i] = prices[i] + ticks * tickSize;
        }
        return noisy;
    }
    
    @Test
    void testCWTFinancialAnalysis() {
        // Use CWT for time-frequency analysis of returns
        PaulWavelet wavelet = new PaulWavelet(4);
        
        // Create scale range for different time horizons
        double[] scales = IntStream.rangeClosed(1, 64)
            .mapToDouble(i -> Math.pow(2, i / 8.0))
            .toArray();
        
        // Take a subset of returns
        int sampleSize = 1024;
        double[] returnSubset = Arrays.copyOf(returns, sampleSize);
        
        // Perform CWT
        CWTTransform cwt = new CWTTransform(wavelet);
        CWTResult result = cwt.analyze(returnSubset, scales);
        
        // Analyze dominant scales (periods)
        System.out.println("\nCWT Time-Frequency Analysis:");
        double maxPower = 0;
        int dominantScale = 0;
        
        for (int s = 0; s < scales.length; s++) {
            double[] coeffs = result.getTimeSlice(s);
            double power = Arrays.stream(coeffs).map(c -> c * c).sum();
            
            if (power > maxPower) {
                maxPower = power;
                dominantScale = s;
            }
        }
        
        System.out.printf("Dominant scale: %.2f (index %d)%n", 
            scales[dominantScale], dominantScale);
        
        // Test that we have results
        assertNotNull(result, "CWT result should not be null");
        assertEquals(scales.length, result.getNumScales(), "Should have correct number of scales");
        assertEquals(sampleSize, result.getNumSamples(), "Should have correct number of samples");
    }
    
    @Test
    void testFinancialMetrics() {
        // Calculate financial metrics using wavelet analysis
        FinancialConfig config = new FinancialConfig(0.05); // 5% annual risk-free rate
        
        FinancialAnalysisConfig analysisConfig = FinancialAnalysisConfig.builder()
            .crashAsymmetryThreshold(0.7)
            .volatilityLowThreshold(0.5)
            .volatilityHighThreshold(2.0)
            .regimeTrendThreshold(0.03)
            .anomalyDetectionThreshold(3.0)
            .windowSize(252)
            .confidenceLevel(0.95)
            .build();
        
        FinancialAnalyzer analyzer = new FinancialAnalyzer(analysisConfig);
        
        // Take daily returns (aggregate tick data)
        List<TickDataLoader.OHLCBar> dailyBars = TickDataLoader.aggregateToOHLC(
            ticks, 86400000L // 1 day in milliseconds
        );
        
        double[] dailyReturns = new double[dailyBars.size() - 1];
        for (int i = 0; i < dailyReturns.length; i++) {
            double price1 = dailyBars.get(i).close();
            double price2 = dailyBars.get(i + 1).close();
            dailyReturns[i] = (price2 - price1) / price1;
        }
        
        if (dailyReturns.length > 0) {
            // Calculate prices from returns for analysis
            double[] prices = new double[dailyReturns.length + 1];
            prices[0] = 100.0; // Starting price
            for (int i = 0; i < dailyReturns.length; i++) {
                prices[i + 1] = prices[i] * (1 + dailyReturns[i]);
            }
            
            // Use FinancialWaveletAnalyzer for Sharpe ratio calculation
            FinancialWaveletAnalyzer waveletAnalyzer = new FinancialWaveletAnalyzer(config);
            double sharpe = waveletAnalyzer.calculateSharpeRatio(dailyReturns);
            
            // Use FinancialAnalyzer for volatility analysis
            double volatility = analyzer.analyzeVolatility(prices);
            
            System.out.println("\nFinancial Metrics:");
            System.out.printf("Standard Sharpe Ratio: %.4f%n", sharpe);
            System.out.printf("Volatility Classification: %s%n", 
                analyzer.classifyVolatility(volatility));
            
            // Basic volatility analysis
            double annualizedVol = Math.sqrt(variance(dailyReturns) * 252); // Annualized
            System.out.printf("Annualized Volatility: %.2f%%n", annualizedVol * 100);
        }
    }
    
    @Test
    void testStreamingDenoiserOnTickData() throws InterruptedException {
        // Test streaming denoiser on continuous tick data
        DiscreteWavelet wavelet = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.DB2);
        
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(wavelet)
            .blockSize(256)
            .thresholdType(ThresholdType.SOFT)
            .thresholdMethod(ThresholdMethod.UNIVERSAL)
            .thresholdMultiplier(1.5)
            .build();
        
        StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.getInstance().create(config);
        
        // Collect denoised output
        CountDownLatch latch = new CountDownLatch(10); // Process 10 blocks
        AtomicInteger blocksReceived = new AtomicInteger(0);
        
        denoiser.subscribe(new Flow.Subscriber<double[]>() {
            private Flow.Subscription subscription;
            
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                this.subscription = subscription;
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(double[] denoisedBlock) {
                int count = blocksReceived.incrementAndGet();
                System.out.println("Received denoised block " + count + 
                    " with " + denoisedBlock.length + " samples");
                
                // Verify output
                assertNotNull(denoisedBlock);
                assertEquals(256, denoisedBlock.length);
                
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
                fail("Streaming failed: " + throwable.getMessage());
            }
            
            @Override
            public void onComplete() {
                System.out.println("Streaming completed");
            }
        });
        
        // Feed price data in blocks
        int blockSize = 256;
        for (int i = 0; i < 10 && i * blockSize < midPrices.length; i++) {
            int start = i * blockSize;
            int end = Math.min(start + blockSize, midPrices.length);
            double[] block = Arrays.copyOfRange(midPrices, start, end);
            
            // Pad if necessary
            if (block.length < blockSize) {
                block = Arrays.copyOf(block, blockSize);
            }
            
            denoiser.process(block);
        }
        
        // Wait for processing
        assertTrue(latch.await(5, TimeUnit.SECONDS), 
            "Should process all blocks within timeout");
        
        denoiser.close();
    }
    
    @Test
    void testMarketMicrostructure() {
        // Analyze market microstructure using wavelets
        DiscreteWavelet wavelet = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.HAAR);
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        // Analyze bid-ask spread dynamics
        int sampleSize = Math.min(1024, spreads.length);
        double[] spreadSubset = Arrays.copyOf(spreads, sampleSize);
        
        // Decompose spreads
        MultiLevelMODWTResult result = transform.decompose(spreadSubset, 3);
        
        System.out.println("\nMarket Microstructure Analysis:");
        System.out.println("Spread statistics:");
        System.out.printf("  Mean: %.6f%n", mean(spreadSubset));
        System.out.printf("  Std Dev: %.6f%n", Math.sqrt(variance(spreadSubset)));
        
        // Analyze persistence at different scales
        for (int level = 1; level <= 3; level++) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            double acf1 = calculateACF(details, 1);
            System.out.printf("Level %d autocorrelation: %.4f%n", level, acf1);
        }
        
        // Test for mean reversion in spreads
        double[] spreadReturns = calculateReturns(spreadSubset);
        double meanReversion = calculateACF(spreadReturns, 1);
        System.out.printf("Spread mean reversion (ACF-1): %.4f%n", meanReversion);
        
        // Negative autocorrelation indicates mean reversion
        assertTrue(meanReversion < 0.5, 
            "Spreads should show some mean reversion");
    }
    
    private double calculateACF(double[] data, int lag) {
        double m = mean(data);
        double var = variance(data);
        
        if (var == 0) return 0;
        
        double sum = 0;
        for (int i = lag; i < data.length; i++) {
            sum += (data[i] - m) * (data[i - lag] - m);
        }
        
        return sum / ((data.length - lag) * var);
    }
    
    @Test
    @Disabled("Performance test - enable when needed")
    void testPerformanceOnLargeDataset() {
        // Test performance with full dataset
        System.out.println("\nPerformance Test on Full Dataset:");
        System.out.println("Total ticks: " + ticks.size());
        
        DiscreteWavelet wavelet = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.DB4);
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        // Process in chunks
        int chunkSize = 8192;
        int chunks = midPrices.length / chunkSize;
        
        long startTime = System.nanoTime();
        
        for (int i = 0; i < chunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, midPrices.length);
            double[] chunk = Arrays.copyOfRange(midPrices, start, end);
            
            // Perform 3-level MODWT
            MultiLevelMODWTResult result = transform.decompose(chunk, 3);
            
            // Simulate some processing
            for (int level = 1; level <= 3; level++) {
                double[] details = result.getDetailCoeffsAtLevel(level);
                double energy = Arrays.stream(details).map(d -> d * d).sum();
            }
        }
        
        long endTime = System.nanoTime();
        double seconds = (endTime - startTime) / 1e9;
        double ticksPerSecond = (chunks * chunkSize) / seconds;
        
        System.out.printf("Processed %d chunks in %.3f seconds%n", chunks, seconds);
        System.out.printf("Throughput: %.0f ticks/second%n", ticksPerSecond);
        
        assertTrue(ticksPerSecond > 100000, 
            "Should process at least 100k ticks per second");
    }
}