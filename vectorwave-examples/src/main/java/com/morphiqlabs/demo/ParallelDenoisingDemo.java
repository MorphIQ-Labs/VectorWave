package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.extensions.parallel.*;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;
import java.util.Random;
import java.util.Arrays;

/**
 * Demonstrates the massive performance improvements of parallel denoising.
 * 
 * <p>This demo showcases:</p>
 * <ul>
 *   <li>Multi-level parallel denoising</li>
 *   <li>Batch denoising of multiple signals</li>
 *   <li>Streaming denoising with parallel block processing</li>
 *   <li>Performance comparison with sequential implementation</li>
 *   <li>Various threshold methods and types</li>
 * </ul>
 */
public class ParallelDenoisingDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Parallel Denoising Performance Demo ===\n");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Available processors: " + Runtime.getRuntime().availableProcessors());
        System.out.println("Max memory: " + Runtime.getRuntime().maxMemory() / (1024 * 1024) + " MB\n");
        
        // Demo 1: Multi-level denoising
        demoMultiLevelDenoising();
        
        // Demo 2: Batch denoising
        demoBatchDenoising();
        
        // Demo 3: Streaming denoising
        demoStreamingDenoising();
        
        // Demo 4: Different threshold methods
        demoThresholdMethods();
        
        // Demo 5: Real-world financial denoising
        demoFinancialDenoising();
    }
    
    private static void demoMultiLevelDenoising() {
        System.out.println("1. Multi-Level Denoising Demo");
        System.out.println("-----------------------------");
        System.out.println("Denoising signal with 6 decomposition levels...\n");
        
        // Create noisy signal
        int signalLength = 8192;
        double[] cleanSignal = createSineWave(signalLength, 10.0, 50.0);
        double noiseLevel = 0.5;
        double[] noisySignal = addNoise(cleanSignal, noiseLevel);
        
        // Create denoisers
        Wavelet wavelet = Daubechies.DB4;
        BoundaryMode boundaryMode = BoundaryMode.PERIODIC;
        
        WaveletDenoiser sequentialDenoiser = new WaveletDenoiser(wavelet, boundaryMode);
        // Sequential denoising
        long startSeq = System.nanoTime();
        double[] denoisedSeq = sequentialDenoiser.denoiseMultiLevel(
            noisySignal, 6, WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT
        );
        long timeSeq = System.nanoTime() - startSeq;
        
        double[] denoisedPar;
        long timePar;
        try (ParallelWaveletDenoiser parallelDenoiser = new ParallelWaveletDenoiser(wavelet, boundaryMode)) {
            // Parallel denoising with auto configuration
            long startPar = System.nanoTime();
            denoisedPar = parallelDenoiser.denoiseMultiLevel(
                noisySignal, 6, WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT
            );
            timePar = System.nanoTime() - startPar;
        }
        
        // Custom parallel configuration for maximum performance
        ParallelConfig customConfig = new ParallelConfig.Builder()
            .parallelismLevel(Runtime.getRuntime().availableProcessors())
            .parallelThreshold(256)
            .useVirtualThreads(true)
            .enableParallelThresholding(true)
            .mode(ParallelConfig.ExecutionMode.PARALLEL_ALWAYS)
            .build();
        
        double[] denoisedCustom;
        long timeCustom;
        try (ParallelWaveletDenoiser customDenoiser = new ParallelWaveletDenoiser(
                wavelet, boundaryMode, customConfig)) {
            long startCustom = System.nanoTime();
            denoisedCustom = customDenoiser.denoiseMultiLevel(
                noisySignal, 6, WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT
            );
            timeCustom = System.nanoTime() - startCustom;
        }
        // Explicitly shutdown external config
        customConfig.shutdown();
        
        // Calculate SNR improvements
        double snrNoisy = calculateSNR(cleanSignal, noisySignal);
        double snrSeq = calculateSNR(cleanSignal, denoisedSeq);
        double snrPar = calculateSNR(cleanSignal, denoisedPar);
        
        // Display results
        System.out.printf("Sequential time:     %8.2f ms\n", timeSeq / 1e6);
        System.out.printf("Parallel (auto):     %8.2f ms (%.2fx speedup)\n", 
            timePar / 1e6, (double) timeSeq / timePar);
        System.out.printf("Parallel (custom):   %8.2f ms (%.2fx speedup)\n", 
            timeCustom / 1e6, (double) timeSeq / timeCustom);
        
        System.out.printf("\nSNR Improvement:\n");
        System.out.printf("Noisy signal:        %6.2f dB\n", snrNoisy);
        System.out.printf("After denoising:     %6.2f dB (+%.2f dB)\n", snrSeq, snrSeq - snrNoisy);
        
        // Verify results are similar
        double maxDiff = calculateMaxDifference(denoisedSeq, denoisedPar);
        System.out.printf("\nMax difference between sequential and parallel: %.2e\n", maxDiff);
        
        System.out.println();
    }
    
    private static void demoBatchDenoising() {
        System.out.println("2. Batch Denoising Demo");
        System.out.println("-----------------------");
        System.out.println("Denoising 32 signals simultaneously...\n");
        
        // Create batch of noisy signals
        int batchSize = 32;
        int signalLength = 2048;
        double[][] noisySignals = new double[batchSize][signalLength];
        
        for (int i = 0; i < batchSize; i++) {
            double[] clean = createSineWave(signalLength, 5.0 + i, 30.0);
            noisySignals[i] = addNoise(clean, 0.3);
        }
        
        // Create parallel denoiser
        try (ParallelWaveletDenoiser denoiser = new ParallelWaveletDenoiser(
            Daubechies.DB6, BoundaryMode.SYMMETRIC
        )) {
        
        // Sequential batch processing
        long startSeq = System.nanoTime();
        double[][] denoisedSeq = new double[batchSize][];
        for (int i = 0; i < batchSize; i++) {
            denoisedSeq[i] = denoiser.denoise(
                noisySignals[i], WaveletDenoiser.ThresholdMethod.SURE, WaveletDenoiser.ThresholdType.SOFT
            );
        }
        long timeSeq = System.nanoTime() - startSeq;
        
        // Parallel batch processing
        long startPar = System.nanoTime();
        double[][] denoisedPar = denoiser.denoiseBatch(
            noisySignals, WaveletDenoiser.ThresholdMethod.SURE, WaveletDenoiser.ThresholdType.SOFT
        );
        long timePar = System.nanoTime() - startPar;
        
        System.out.printf("Sequential batch:    %8.2f ms\n", timeSeq / 1e6);
        System.out.printf("Parallel batch:      %8.2f ms (%.2fx speedup)\n", 
            timePar / 1e6, (double) timeSeq / timePar);
        System.out.printf("Average per signal:  %8.2f ms\n", timePar / (1e6 * batchSize));

        System.out.println();
        }
    }
    
    private static void demoStreamingDenoising() {
        System.out.println("3. Streaming Denoising Demo");
        System.out.println("---------------------------");
        System.out.println("Processing long signal in overlapping blocks...\n");
        
        // Create very long noisy signal
        int signalLength = 65536;
        double[] cleanSignal = createChirpSignal(signalLength);
        double[] noisySignal = addNoise(cleanSignal, 0.4);
        
        // Streaming parameters
        int blockSize = 4096;
        int overlap = 512;
        
        // Create denoiser
        try (ParallelWaveletDenoiser denoiser = new ParallelWaveletDenoiser(
            Symlet.SYM8, BoundaryMode.PERIODIC
        )) {
        
        // Standard denoising (entire signal at once)
        long startStandard = System.nanoTime();
        double[] denoisedStandard = denoiser.denoise(
            noisySignal, WaveletDenoiser.ThresholdMethod.MINIMAX, WaveletDenoiser.ThresholdType.HARD
        );
        long timeStandard = System.nanoTime() - startStandard;
        
        // Streaming denoising
        long startStreaming = System.nanoTime();
        double[] denoisedStreaming = denoiser.denoiseStreaming(
            noisySignal, blockSize, overlap, 
            WaveletDenoiser.ThresholdMethod.MINIMAX, WaveletDenoiser.ThresholdType.HARD
        );
        long timeStreaming = System.nanoTime() - startStreaming;
        
        // Calculate SNR
        double snrStandard = calculateSNR(cleanSignal, denoisedStandard);
        double snrStreaming = calculateSNR(cleanSignal, denoisedStreaming);
        
        System.out.printf("Standard denoising:  %8.2f ms (SNR: %.2f dB)\n", 
            timeStandard / 1e6, snrStandard);
        System.out.printf("Streaming denoising: %8.2f ms (SNR: %.2f dB)\n", 
            timeStreaming / 1e6, snrStreaming);
        System.out.printf("Speedup:             %.2fx\n", (double) timeStandard / timeStreaming);

        System.out.println();
        }
    }
    
    private static void demoThresholdMethods() {
        System.out.println("4. Threshold Methods Comparison");
        System.out.println("--------------------------------");
        System.out.println("Comparing different threshold calculation methods...\n");
        
        // Create noisy signal
        int signalLength = 4096;
        double[] cleanSignal = createComplexSignal(signalLength);
        double[] noisySignal = addNoise(cleanSignal, 0.35);
        
        // Create parallel denoiser with aggressive parallelization
        ParallelConfig config = new ParallelConfig.Builder()
            .parallelismLevel(Runtime.getRuntime().availableProcessors())
            .mode(ParallelConfig.ExecutionMode.PARALLEL_ALWAYS)
            .enableParallelThresholding(true)
            .build();
        
        try (ParallelWaveletDenoiser denoiser = new ParallelWaveletDenoiser(
            Coiflet.COIF3, BoundaryMode.SYMMETRIC, config
        )) {
        
        // Test different methods
        WaveletDenoiser.ThresholdMethod[] methods = {
            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
            WaveletDenoiser.ThresholdMethod.SURE,
            WaveletDenoiser.ThresholdMethod.MINIMAX,
            WaveletDenoiser.ThresholdMethod.BAYES
        };
        
        System.out.println("Method      | Time (ms) | SNR (dB)  | RMSE");
        System.out.println("------------|-----------|-----------|----------");
        
        for (WaveletDenoiser.ThresholdMethod method : methods) {
            long start = System.nanoTime();
            double[] denoised = denoiser.denoiseMultiLevel(
                noisySignal, 5, method, WaveletDenoiser.ThresholdType.SOFT
            );
            long time = System.nanoTime() - start;
            
            double snr = calculateSNR(cleanSignal, denoised);
            double rmse = calculateRMSE(cleanSignal, denoised);
            
            System.out.printf("%-11s | %9.2f | %9.2f | %8.6f\n", 
                method, time / 1e6, snr, rmse);
        }

        System.out.println();
        }
        config.shutdown();
    }
    
    private static void demoFinancialDenoising() {
        System.out.println("5. Financial Signal Denoising");
        System.out.println("------------------------------");
        System.out.println("Denoising simulated financial returns...\n");
        
        // Create financial returns with microstructure noise
        int days = 252 * 2; // 2 years
        double[] returns = createFinancialReturns(days);
        double[] noisyReturns = addMicrostructureNoise(returns);
        
        // Create optimized denoiser for financial data
        try (ParallelWaveletDenoiser denoiser = new ParallelWaveletDenoiser(
            Daubechies.DB4, BoundaryMode.PERIODIC
        )) {
        
        // Denoise with different methods
        long start = System.nanoTime();
        double[] denoisedSURE = denoiser.denoiseMultiLevel(
            noisyReturns, 4, WaveletDenoiser.ThresholdMethod.SURE, WaveletDenoiser.ThresholdType.SOFT
        );
        long time = System.nanoTime() - start;
        
        // Calculate statistics
        double volOriginal = calculateVolatility(returns);
        double volNoisy = calculateVolatility(noisyReturns);
        double volDenoised = calculateVolatility(denoisedSURE);
        
        double sharpeOriginal = calculateSharpeRatio(returns);
        double sharpeNoisy = calculateSharpeRatio(noisyReturns);
        double sharpeDenoised = calculateSharpeRatio(denoisedSURE);
        
        System.out.printf("Processing time:     %.2f ms\n", time / 1e6);
        System.out.println("\nVolatility Analysis:");
        System.out.printf("Original:            %.4f\n", volOriginal);
        System.out.printf("Noisy:               %.4f (+%.1f%%)\n", 
            volNoisy, (volNoisy/volOriginal - 1) * 100);
        System.out.printf("Denoised:            %.4f (+%.1f%%)\n", 
            volDenoised, (volDenoised/volOriginal - 1) * 100);
        
        System.out.println("\nSharpe Ratio:");
        System.out.printf("Original:            %.4f\n", sharpeOriginal);
        System.out.printf("Noisy:               %.4f\n", sharpeNoisy);
        System.out.printf("Denoised:            %.4f (%.1f%% improvement)\n", 
            sharpeDenoised, (sharpeDenoised/sharpeNoisy - 1) * 100);
        
        // Show parallelization efficiency
        ParallelConfig.ExecutionStats stats = denoiser.getStats();
        System.out.printf("\nParallelization efficiency: %.1f%%\n", 
            stats.parallelRatio() * 100);
        
        System.out.println("\n=== Demo Complete ===");
        }
    }
    
    // Helper methods for signal generation
    
    private static double[] createSineWave(int length, double freq1, double freq2) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            signal[i] = Math.sin(2 * Math.PI * freq1 * t) + 
                       0.5 * Math.sin(2 * Math.PI * freq2 * t);
        }
        return signal;
    }
    
    private static double[] createChirpSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            double freq = 5 + 100 * t; // Frequency increases from 5 to 105
            signal[i] = Math.sin(2 * Math.PI * freq * t);
        }
        return signal;
    }
    
    private static double[] createComplexSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            double t = (double) i / length;
            signal[i] = Math.sin(2 * Math.PI * 5 * t) +
                       0.5 * Math.sin(2 * Math.PI * 25 * t) +
                       0.3 * Math.sin(2 * Math.PI * 50 * t) +
                       0.2 * Math.cos(2 * Math.PI * 100 * t);
        }
        return signal;
    }
    
    private static double[] createFinancialReturns(int length) {
        double[] returns = new double[length];
        Random rand = new Random(42);
        
        // GARCH-like model
        double volatility = 0.01;
        for (int i = 0; i < length; i++) {
            if (i > 0) {
                double shock = Math.abs(returns[i-1]);
                volatility = 0.9 * volatility + 0.1 * shock;
            }
            returns[i] = volatility * rand.nextGaussian();
        }
        return returns;
    }
    
    private static double[] addNoise(double[] signal, double noiseLevel) {
        double[] noisy = signal.clone();
        Random rand = new Random(42);
        
        for (int i = 0; i < noisy.length; i++) {
            noisy[i] += noiseLevel * rand.nextGaussian();
        }
        return noisy;
    }
    
    private static double[] addMicrostructureNoise(double[] returns) {
        double[] noisy = returns.clone();
        Random rand = new Random(42);
        
        for (int i = 0; i < noisy.length; i++) {
            // Add bid-ask bounce and rounding effects
            double microNoise = 0.0001 * rand.nextGaussian();
            if (rand.nextDouble() < 0.1) { // 10% chance of larger noise
                microNoise *= 5;
            }
            noisy[i] += microNoise;
        }
        return noisy;
    }
    
    private static double calculateSNR(double[] clean, double[] noisy) {
        double signalPower = 0.0;
        double noisePower = 0.0;
        
        for (int i = 0; i < clean.length; i++) {
            signalPower += clean[i] * clean[i];
            double noise = noisy[i] - clean[i];
            noisePower += noise * noise;
        }
        
        if (noisePower == 0) return Double.POSITIVE_INFINITY;
        return 10 * Math.log10(signalPower / noisePower);
    }
    
    private static double calculateRMSE(double[] clean, double[] noisy) {
        double mse = 0.0;
        for (int i = 0; i < clean.length; i++) {
            double diff = clean[i] - noisy[i];
            mse += diff * diff;
        }
        return Math.sqrt(mse / clean.length);
    }
    
    private static double calculateMaxDifference(double[] a, double[] b) {
        double maxDiff = 0.0;
        for (int i = 0; i < a.length; i++) {
            maxDiff = Math.max(maxDiff, Math.abs(a[i] - b[i]));
        }
        return maxDiff;
    }
    
    private static double calculateVolatility(double[] returns) {
        double mean = Arrays.stream(returns).average().orElse(0.0);
        double sumSquares = 0.0;
        
        for (double r : returns) {
            double diff = r - mean;
            sumSquares += diff * diff;
        }
        
        return Math.sqrt(sumSquares / (returns.length - 1));
    }
    
    private static double calculateSharpeRatio(double[] returns) {
        double mean = Arrays.stream(returns).average().orElse(0.0);
        double vol = calculateVolatility(returns);
        
        // Annualized Sharpe ratio (assuming daily returns)
        return (mean * 252) / (vol * Math.sqrt(252));
    }
}
