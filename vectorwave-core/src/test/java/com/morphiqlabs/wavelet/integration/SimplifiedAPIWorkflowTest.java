package com.morphiqlabs.wavelet.integration;

import com.morphiqlabs.wavelet.WaveletOperations;
import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;
import com.morphiqlabs.financial.*;
import com.morphiqlabs.wavelet.memory.MemoryPool;
import com.morphiqlabs.wavelet.modwt.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests demonstrating real-world workflows with the simplified VectorWave API.
 * Focuses on the most common use cases with the public API.
 */
@DisplayName("Simplified API Workflow Tests")
class SimplifiedAPIWorkflowTest {

    private static final double EPSILON = 1e-10;
    private static final Random random = new Random(42);

    @Test
    @DisplayName("Complete signal denoising workflow")
    void testSignalDenoisingWorkflow() {
        // 1. Generate a noisy signal
        int signalLength = 1000;
        double[] cleanSignal = generateSineWave(signalLength, 0.01, 2.0);
        double[] noise = generateGaussianNoise(signalLength, 0.0, 0.5);
        double[] noisySignal = addArrays(cleanSignal, noise);

        // 2. Check platform capabilities
        WaveletOperations.PerformanceInfo perfInfo = WaveletOperations.getPerformanceInfo();
        System.out.println("Platform: " + perfInfo.description());
        assertNotNull(perfInfo.description());

        // 3. Create denoiser
        WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);

        // 4. Denoise the signal
        double[] denoised = denoiser.denoise(noisySignal, 
            WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
            WaveletDenoiser.ThresholdType.SOFT);

        // 5. Verify denoising effectiveness
        double noisySnr = calculateSNR(cleanSignal, noisySignal);
        double denoisedSnr = calculateSNR(cleanSignal, denoised);
        assertTrue(denoisedSnr > noisySnr, 
            "Denoised SNR should be better than noisy SNR");
        System.out.printf("SNR improvement: %.2f dB -> %.2f dB%n", noisySnr, denoisedSnr);

        // 6. Use WaveletOperations for additional processing
        double threshold = 0.1;
        double[] softThresholded = WaveletOperations.softThreshold(denoised, threshold);
        double[] hardThresholded = WaveletOperations.hardThreshold(denoised, threshold);
        assertNotNull(softThresholded);
        assertNotNull(hardThresholded);
        assertEquals(denoised.length, softThresholded.length);
    }

    @Test
    @DisplayName("Financial analysis workflow")
    void testFinancialAnalysisWorkflow() {
        // 1. Generate simulated market data
        int days = 252; // One trading year
        double[] prices = generateRandomWalk(days, 100.0, 0.02);
        double[] returns = calculateReturns(prices);

        // 2. Configure analysis
        FinancialAnalysisConfig config = FinancialAnalysisConfig.builder()
            .crashAsymmetryThreshold(0.7)
            .volatilityLowThreshold(0.5)
            .volatilityHighThreshold(2.0)
            .regimeTrendThreshold(0.02)
            .anomalyDetectionThreshold(3.0)
            .windowSize(60) // Any size!
            .confidenceLevel(0.95)
            .build();

        // 3. Analyze with standard methods
        FinancialAnalyzer analyzer = new FinancialAnalyzer(config);
        double asymmetry = analyzer.analyzeCrashAsymmetry(returns);
        double volatility = analyzer.analyzeVolatility(returns);
        boolean hasAnomalies = analyzer.detectAnomalies(returns);

        // 4. Wavelet-based analysis
        FinancialConfig waveletConfig = new FinancialConfig(0.045); // 4.5% risk-free rate
        FinancialWaveletAnalyzer waveletAnalyzer = new FinancialWaveletAnalyzer(waveletConfig);
        
        double standardSharpe = waveletAnalyzer.calculateSharpeRatio(returns);
        double waveletSharpe = waveletAnalyzer.calculateWaveletSharpeRatio(returns);

        // 5. Verify results
        assertTrue(asymmetry >= 0 && asymmetry <= 1);
        assertTrue(volatility > 0);
        assertTrue(Double.isFinite(standardSharpe));
        assertTrue(Double.isFinite(waveletSharpe));
    }

    @Test
    @DisplayName("MODWT batch processing workflow")
    void testBatchProcessingWorkflow() {
        // 1. Prepare multiple signals
        int numSignals = 32;
        int signalLength = 777; // Any length!
        double[][] signals = new double[numSignals][signalLength];
        
        for (int i = 0; i < numSignals; i++) {
            signals[i] = generateSineWave(signalLength, 0.01 * (i + 1), 1.0);
        }

        // 2. Create transform
        MODWTTransform transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);

        // 3. Batch process with automatic optimization
        MODWTResult[] results = transform.forwardBatch(signals);

        // 4. Verify results
        assertEquals(numSignals, results.length);
        for (MODWTResult result : results) {
            assertNotNull(result);
            assertTrue(result.isValid());
            assertEquals(signalLength, result.getSignalLength());
        }

        // 5. Modify and reconstruct
        MODWTResult[] modified = new MODWTResult[numSignals];
        for (int i = 0; i < numSignals; i++) {
            double[] thresholded = WaveletOperations.softThreshold(
                results[i].detailCoeffs(), 0.1);
            modified[i] = MODWTResult.create(
                results[i].approximationCoeffs(), thresholded);
        }

        // 6. Inverse transform
        double[][] reconstructed = transform.inverseBatch(modified);
        assertEquals(numSignals, reconstructed.length);
        for (double[] signal : reconstructed) {
            assertEquals(signalLength, signal.length);
        }
    }

    @Test
    @DisplayName("Single-level MODWT analysis")
    void testSingleLevelMODWTAnalysis() {
        // 1. Create multi-frequency signal
        int signalLength = 1024;
        double[] signal = new double[signalLength];
        addToArray(signal, generateSineWave(signalLength, 0.005, 0.5));
        addToArray(signal, generateSineWave(signalLength, 0.05, 0.3));
        addToArray(signal, generateSineWave(signalLength, 0.2, 0.2));

        // 2. Perform MODWT
        MODWTTransform transform = new MODWTTransform(Symlet.SYM4, BoundaryMode.PERIODIC);
        MODWTResult result = transform.forward(signal);

        // 3. Analyze coefficients
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        
        assertEquals(signalLength, approx.length);
        assertEquals(signalLength, detail.length);

        // 4. Calculate energies
        double approxEnergy = calculateEnergy(approx);
        double detailEnergy = calculateEnergy(detail);
        double totalEnergy = calculateEnergy(signal);
        
        // Energy should be approximately preserved
        double reconstructedEnergy = approxEnergy + detailEnergy;
        double energyRatio = reconstructedEnergy / totalEnergy;
        assertTrue(energyRatio > 0.9 && energyRatio < 1.1, 
            "Energy should be approximately preserved");

        // 5. Reconstruct
        double[] reconstructed = transform.inverse(result);
        double rmse = calculateRMSE(signal, reconstructed);
        assertTrue(rmse < 1e-10, "Perfect reconstruction expected");
    }

    @Test
    @DisplayName("Memory-efficient processing")
    void testMemoryEfficientProcessing() {
        // 1. Create memory pool
        MemoryPool pool = new MemoryPool();
        pool.setMaxArraysPerSize(5);

        int iterations = 50;
        int signalLength = 2000;

        // 2. Process with pooling
        for (int i = 0; i < iterations; i++) {
            double[] signal = pool.borrowArray(signalLength);
            try {
                // Fill with data
                for (int j = 0; j < signalLength; j++) {
                    signal[j] = Math.sin(2 * Math.PI * j / 100.0);
                }

                // Process
                MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
                MODWTResult result = transform.forward(signal);
                
                // Apply denoising
                double[] denoised = WaveletOperations.softThreshold(
                    result.detailCoeffs(), 0.1);
                MODWTResult denoisedResult = MODWTResult.create(
                    result.approximationCoeffs(), denoised);
                
                double[] reconstructed = transform.inverse(denoisedResult);
                assertNotNull(reconstructed);

            } finally {
                pool.returnArray(signal);
            }
        }

        // 3. Check statistics
        double hitRate = pool.getHitRate();
        assertTrue(hitRate > 0.8, "Pool should be effective");
        System.out.printf("Pool hit rate: %.2f%%%n", hitRate * 100);
        
        pool.clear();
    }

    // Helper methods

    private double[] generateSineWave(int length, double frequency, double amplitude) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = amplitude * Math.sin(2 * Math.PI * frequency * i);
        }
        return signal;
    }

    private double[] generateGaussianNoise(int length, double mean, double stdDev) {
        double[] noise = new double[length];
        for (int i = 0; i < length; i++) {
            noise[i] = mean + stdDev * random.nextGaussian();
        }
        return noise;
    }

    private double[] generateRandomWalk(int length, double startPrice, double volatility) {
        double[] prices = new double[length];
        prices[0] = startPrice;
        for (int i = 1; i < length; i++) {
            double dailyReturn = volatility * random.nextGaussian();
            prices[i] = prices[i - 1] * (1 + dailyReturn);
        }
        return prices;
    }

    private double[] calculateReturns(double[] prices) {
        double[] returns = new double[prices.length - 1];
        for (int i = 0; i < returns.length; i++) {
            returns[i] = (prices[i + 1] - prices[i]) / prices[i];
        }
        return returns;
    }

    private double[] addArrays(double[] a, double[] b) {
        double[] result = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = a[i] + b[i];
        }
        return result;
    }

    private void addToArray(double[] target, double[] source) {
        for (int i = 0; i < target.length; i++) {
            target[i] += source[i];
        }
    }

    private double calculateSNR(double[] signal, double[] noisySignal) {
        double signalPower = 0;
        double noisePower = 0;
        for (int i = 0; i < signal.length; i++) {
            signalPower += signal[i] * signal[i];
            double noise = noisySignal[i] - signal[i];
            noisePower += noise * noise;
        }
        return 10 * Math.log10(signalPower / noisePower);
    }

    private double calculateRMSE(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum / a.length);
    }

    private double calculateEnergy(double[] signal) {
        double energy = 0;
        for (double value : signal) {
            energy += value * value;
        }
        return energy;
    }
}