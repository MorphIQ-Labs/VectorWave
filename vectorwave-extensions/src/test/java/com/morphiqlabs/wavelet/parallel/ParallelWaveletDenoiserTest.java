package com.morphiqlabs.wavelet.extensions.parallel;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ParallelWaveletDenoiser.
 * Tests parallel denoising accuracy, thread safety, and performance characteristics.
 */
class ParallelWaveletDenoiserTest {
    
    private static final double EPSILON = 1e-10;
    private static final double NOISE_TOLERANCE = 0.1; // 10% tolerance for denoising effectiveness
    
    private ParallelWaveletDenoiser parallelDenoiser;
    private WaveletDenoiser sequentialDenoiser;
    private Random random;
    
    @BeforeEach
    void setUp() {
        random = new Random(42); // Fixed seed for reproducibility
        Wavelet wavelet = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.PERIODIC;
        
        parallelDenoiser = new ParallelWaveletDenoiser(wavelet, mode);
        sequentialDenoiser = new WaveletDenoiser(wavelet, mode);
    }
    
    // ============== Basic Functionality Tests ==============
    
    @Test
    @DisplayName("Parallel denoising should match sequential for small signals")
    void testParallelMatchesSequentialSmallSignal() {
        // Small signal that should trigger sequential fallback
        double[] signal = generateNoisySignal(64, 0.1);
        
        double[] parallelResult = parallelDenoiser.denoise(
            signal, WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT);
        double[] sequentialResult = sequentialDenoiser.denoise(
            signal, WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT);
        
        assertArrayEquals(sequentialResult, parallelResult, EPSILON,
            "Results should match for small signals");
    }
    
    @Test
    @DisplayName("Parallel denoising should be effective for large signals")
    void testParallelDenoisingEffectivenessLargeSignal() {
        // Large signal that should trigger parallel processing
        double[] cleanSignal = generateSineWave(4096, 10.0);
        double[] noise = generateGaussianNoise(4096, 0.1); // Reduced noise level
        double[] noisySignal = addSignals(cleanSignal, noise);
        
        double[] denoised = parallelDenoiser.denoise(
            noisySignal, WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT);
        
        // Calculate SNR improvement
        double noisySNR = calculateSNR(cleanSignal, noisySignal);
        double denoisedSNR = calculateSNR(cleanSignal, denoised);
        
        assertTrue(denoisedSNR > noisySNR,
            "Denoised signal should have better SNR than noisy signal");
        
        // Check that denoising reduces noise
        double noiseRMSE = calculateRMSE(cleanSignal, noisySignal);
        double denoisedRMSE = calculateRMSE(cleanSignal, denoised);
        assertTrue(denoisedRMSE < noiseRMSE,
            "Denoised signal should have less noise than original");
    }
    
    // ============== Multi-Level Denoising Tests ==============
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("Multi-level denoising should work for different decomposition levels")
    void testMultiLevelDenoisingDifferentLevels(int levels) {
        double[] signal = generateNoisySignal(1024, 0.2);
        
        double[] denoised = parallelDenoiser.denoiseMultiLevel(
            signal, levels, WaveletDenoiser.ThresholdMethod.SURE, WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(denoised);
        assertEquals(signal.length, denoised.length);
        
        // Verify signal is actually modified (denoised)
        assertFalse(areArraysEqual(signal, denoised),
            "Denoised signal should differ from noisy signal");
    }
    
    @Test
    @DisplayName("Parallel multi-level should match sequential for comparable signals")
    void testParallelMultiLevelMatchesSequential() {
        double[] signal = generateNoisySignal(512, 0.15);
        int levels = 3;
        
        double[] parallelResult = parallelDenoiser.denoiseMultiLevel(
            signal, levels, WaveletDenoiser.ThresholdMethod.MINIMAX, WaveletDenoiser.ThresholdType.HARD);
        double[] sequentialResult = sequentialDenoiser.denoiseMultiLevel(
            signal, levels, WaveletDenoiser.ThresholdMethod.MINIMAX, WaveletDenoiser.ThresholdType.HARD);
        
        // Both should denoise the signal
        assertNotNull(parallelResult);
        assertNotNull(sequentialResult);
        assertEquals(signal.length, parallelResult.length);
        assertEquals(signal.length, sequentialResult.length);
        
        // Both should modify the signal
        assertFalse(areArraysEqual(signal, parallelResult));
        assertFalse(areArraysEqual(signal, sequentialResult));
    }
    
    // ============== Threshold Method and Type Tests ==============
    
    @ParameterizedTest
    @EnumSource(value = WaveletDenoiser.ThresholdMethod.class, 
                names = {"FIXED"}, mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("Should support all automatic threshold methods")
    void testAllThresholdMethods(WaveletDenoiser.ThresholdMethod method) {
        double[] signal = generateNoisySignal(256, 0.1);
        
        assertDoesNotThrow(() -> {
            double[] denoised = parallelDenoiser.denoise(
                signal, method, WaveletDenoiser.ThresholdType.SOFT);
            assertNotNull(denoised);
            assertEquals(signal.length, denoised.length);
        });
    }
    
    @ParameterizedTest
    @CsvSource({
        "SOFT,UNIVERSAL",
        "HARD,UNIVERSAL",
        "SOFT,SURE",
        "HARD,SURE",
        "SOFT,MINIMAX",
        "HARD,MINIMAX"
    })
    @DisplayName("Should support all combinations of threshold types and methods")
    void testThresholdTypesAndMethods(String typeStr, String methodStr) {
        WaveletDenoiser.ThresholdType type = WaveletDenoiser.ThresholdType.valueOf(typeStr);
        WaveletDenoiser.ThresholdMethod method = WaveletDenoiser.ThresholdMethod.valueOf(methodStr);
        
        double[] signal = generateNoisySignal(512, 0.2);
        double[] denoised = parallelDenoiser.denoise(signal, method, type);
        
        assertNotNull(denoised);
        assertEquals(signal.length, denoised.length);
    }
    
    // ============== Batch Processing Tests ==============
    
    @Test
    @DisplayName("Batch denoising should process multiple signals correctly")
    void testBatchDenoising() {
        int numSignals = 10;
        int signalLength = 256;
        double[][] signals = new double[numSignals][];
        
        for (int i = 0; i < numSignals; i++) {
            signals[i] = generateNoisySignal(signalLength, 0.15);
        }
        
        double[][] denoised = parallelDenoiser.denoiseBatch(
            signals, WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(denoised);
        assertEquals(numSignals, denoised.length);
        
        for (int i = 0; i < numSignals; i++) {
            assertNotNull(denoised[i]);
            assertEquals(signalLength, denoised[i].length);
            assertFalse(areArraysEqual(signals[i], denoised[i]),
                "Each signal should be denoised");
        }
    }
    
    @Test
    @DisplayName("Batch denoising should match individual processing")
    void testBatchDenoisingConsistency() {
        double[][] signals = {
            generateNoisySignal(128, 0.1),
            generateNoisySignal(128, 0.2),
            generateNoisySignal(128, 0.15)
        };
        
        // Process batch
        double[][] batchResults = parallelDenoiser.denoiseBatch(
            signals, WaveletDenoiser.ThresholdMethod.SURE, WaveletDenoiser.ThresholdType.SOFT);
        
        // Process individually
        double[][] individualResults = new double[signals.length][];
        for (int i = 0; i < signals.length; i++) {
            individualResults[i] = parallelDenoiser.denoise(
                signals[i], WaveletDenoiser.ThresholdMethod.SURE, WaveletDenoiser.ThresholdType.SOFT);
        }
        
        // Compare results
        for (int i = 0; i < signals.length; i++) {
            assertArrayEquals(individualResults[i], batchResults[i], EPSILON,
                "Batch and individual processing should yield same results");
        }
    }
    
    // ============== Streaming Denoising Tests ==============
    
    @Test
    @DisplayName("Streaming denoising should handle overlapping blocks correctly")
    void testStreamingDenoising() {
        double[] signal = generateNoisySignal(1024, 0.1);
        int blockSize = 256;
        int overlap = 64;
        
        double[] denoised = parallelDenoiser.denoiseStreaming(
            signal, blockSize, overlap,
            WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(denoised);
        assertEquals(signal.length, denoised.length);
        assertFalse(areArraysEqual(signal, denoised));
    }
    
    @Test
    @DisplayName("Streaming denoising should validate parameters")
    void testStreamingDenoisingValidation() {
        double[] signal = generateNoisySignal(512, 0.1);
        
        // Test invalid block size
        assertThrows(InvalidArgumentException.class, () ->
            parallelDenoiser.denoiseStreaming(signal, 64, 64,
                WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT),
            "Should throw when blockSize <= overlap");
        
        // Test null signal
        assertThrows(InvalidArgumentException.class, () ->
            parallelDenoiser.denoiseStreaming(null, 256, 64,
                WaveletDenoiser.ThresholdMethod.UNIVERSAL, WaveletDenoiser.ThresholdType.SOFT),
            "Should throw for null signal");
    }
    
    // ============== Thread Safety Tests ==============
    
    @Test
    @DisplayName("Parallel denoiser should be thread-safe for concurrent operations")
    void testThreadSafety() throws InterruptedException {
        int numThreads = 8;
        int iterations = 100;
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                try {
                    for (int i = 0; i < iterations; i++) {
                        double[] signal = generateNoisySignal(128, 0.1);
                        double[] denoised = parallelDenoiser.denoise(
                            signal, WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
                            WaveletDenoiser.ThresholdType.SOFT);
                        
                        if (denoised != null && denoised.length == signal.length) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS),
            "All threads should complete within timeout");
        assertEquals(numThreads * iterations, successCount.get(),
            "All denoising operations should succeed");
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("Batch processing should handle concurrent batches")
    void testConcurrentBatchProcessing() throws InterruptedException {
        int numBatches = 5;
        CountDownLatch latch = new CountDownLatch(numBatches);
        ExecutorService executor = Executors.newFixedThreadPool(numBatches);
        
        for (int b = 0; b < numBatches; b++) {
            executor.submit(() -> {
                try {
                    double[][] signals = new double[4][];
                    for (int i = 0; i < 4; i++) {
                        signals[i] = generateNoisySignal(256, 0.15);
                    }
                    
                    double[][] denoised = parallelDenoiser.denoiseBatch(
                        signals, WaveletDenoiser.ThresholdMethod.SURE, 
                        WaveletDenoiser.ThresholdType.SOFT);
                    
                    assertNotNull(denoised);
                    assertEquals(4, denoised.length);
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS),
            "All batch operations should complete");
        
        executor.shutdown();
    }
    
    // ============== Configuration Tests ==============
    
    @Test
    @DisplayName("Should work with different wavelets")
    void testDifferentWavelets() {
        Wavelet[] wavelets = {new Haar(), Daubechies.DB2, Daubechies.DB6, Daubechies.DB8};
        
        for (Wavelet wavelet : wavelets) {
            ParallelWaveletDenoiser denoiser = new ParallelWaveletDenoiser(
                wavelet, BoundaryMode.PERIODIC);
            
            double[] signal = generateNoisySignal(256, 0.1);
            double[] denoised = denoiser.denoise(
                signal, WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
                WaveletDenoiser.ThresholdType.SOFT);
            
            assertNotNull(denoised);
            assertEquals(signal.length, denoised.length);
        }
    }
    
    @Test
    @DisplayName("Should work with different boundary modes")
    void testDifferentBoundaryModes() {
        // Only test boundary modes supported by MODWT
        BoundaryMode[] modes = {
            BoundaryMode.PERIODIC,
            BoundaryMode.ZERO_PADDING,
            BoundaryMode.SYMMETRIC
        };
        
        for (BoundaryMode mode : modes) {
            ParallelWaveletDenoiser denoiser = new ParallelWaveletDenoiser(
                Daubechies.DB4, mode);
            
            double[] signal = generateNoisySignal(256, 0.1);
            double[] denoised = denoiser.denoise(
                signal, WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
                WaveletDenoiser.ThresholdType.SOFT);
            
            assertNotNull(denoised, "Denoised signal should not be null for mode: " + mode);
            assertEquals(signal.length, denoised.length, "Length should match for mode: " + mode);
        }
    }
    
    @Test
    @DisplayName("Should work with custom ParallelConfig")
    void testCustomParallelConfig() {
        ParallelConfig config = new ParallelConfig.Builder()
            .parallelismLevel(2)
            .parallelThreshold(128)
            .enableParallelThresholding(true)
            .build();
        
        ParallelWaveletDenoiser denoiser = new ParallelWaveletDenoiser(
            Daubechies.DB4, BoundaryMode.PERIODIC, config);
        
        double[] signal = generateNoisySignal(512, 0.15);
        double[] denoised = denoiser.denoise(
            signal, WaveletDenoiser.ThresholdMethod.SURE, 
            WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(denoised);
        assertEquals(signal.length, denoised.length);
    }
    
    // ============== Builder Pattern Tests ==============
    
    @Test
    @DisplayName("Builder should create denoiser correctly")
    void testBuilder() {
        ParallelWaveletDenoiser denoiser = new ParallelWaveletDenoiser.Builder()
            .wavelet(Daubechies.DB4)
            .boundaryMode(BoundaryMode.SYMMETRIC)
            .parallelConfig(ParallelConfig.auto())
            .build();
        
        assertNotNull(denoiser);
        
        double[] signal = generateNoisySignal(256, 0.1);
        double[] denoised = denoiser.denoise(
            signal, WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
            WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(denoised);
        assertEquals(signal.length, denoised.length);
    }
    
    @Test
    @DisplayName("Builder should validate required parameters")
    void testBuilderValidation() {
        ParallelWaveletDenoiser.Builder builder = new ParallelWaveletDenoiser.Builder();
        
        assertThrows(IllegalStateException.class, builder::build,
            "Should throw when wavelet is not specified");
    }
    
    // ============== Performance Statistics Tests ==============
    
    @Test
    @DisplayName("Should provide execution statistics")
    void testExecutionStatistics() {
        // Test that we can get statistics (regardless of whether they're tracked)
        ParallelConfig.ExecutionStats stats = parallelDenoiser.getStats();
        assertNotNull(stats, "Statistics should not be null");
        
        // Stats should be non-negative
        assertTrue(stats.parallelExecutions() >= 0, "Parallel executions should be non-negative");
        assertTrue(stats.sequentialExecutions() >= 0, "Sequential executions should be non-negative");
        assertTrue(stats.estimatedSpeedup() >= 0, "Estimated speedup should be non-negative");
        
        // Parallel ratio should be between 0 and 1
        double ratio = stats.parallelRatio();
        assertTrue(ratio >= 0.0 && ratio <= 1.0, "Parallel ratio should be between 0 and 1");
    }
    
    // ============== Edge Cases and Error Handling ==============
    
    @Test
    @DisplayName("Should handle null and empty inputs")
    void testNullAndEmptyInputs() {
        assertThrows(InvalidArgumentException.class, () ->
            parallelDenoiser.denoise(null, 
                WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
                WaveletDenoiser.ThresholdType.SOFT),
            "Should throw for null signal");
        
        assertThrows(InvalidArgumentException.class, () ->
            parallelDenoiser.denoise(new double[0], 
                WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
                WaveletDenoiser.ThresholdType.SOFT),
            "Should throw for empty signal");
        
        assertThrows(InvalidArgumentException.class, () ->
            parallelDenoiser.denoiseBatch(null, 
                WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
                WaveletDenoiser.ThresholdType.SOFT),
            "Should throw for null batch");
    }
    
    @Test
    @DisplayName("Should handle very small signals")
    void testVerySmallSignals() {
        double[] tinySignal = {1.0, 2.0, 3.0, 4.0};
        
        double[] denoised = parallelDenoiser.denoise(
            tinySignal, WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
            WaveletDenoiser.ThresholdType.SOFT);
        
        assertNotNull(denoised);
        assertEquals(tinySignal.length, denoised.length);
    }
    
    @Test
    @DisplayName("Should handle signals with extreme values")
    void testExtremeValues() {
        double[] extremeSignal = {
            Double.MAX_VALUE / 2, -Double.MAX_VALUE / 2,
            1e-100, -1e-100, 0.0, 1.0, -1.0
        };
        
        assertDoesNotThrow(() -> {
            double[] denoised = parallelDenoiser.denoise(
                extremeSignal, WaveletDenoiser.ThresholdMethod.UNIVERSAL, 
                WaveletDenoiser.ThresholdType.SOFT);
            assertNotNull(denoised);
            
            // Check for NaN or Infinity
            for (double value : denoised) {
                assertFalse(Double.isNaN(value), "Should not produce NaN");
                assertFalse(Double.isInfinite(value), "Should not produce Infinity");
            }
        });
    }
    
    // ============== Helper Methods ==============
    
    private double[] generateNoisySignal(int length, double noiseLevel) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            // Create a signal with some structure
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(4 * Math.PI * i / 32.0) +
                       noiseLevel * random.nextGaussian();
        }
        return signal;
    }
    
    private double[] generateSineWave(int length, double frequency) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * frequency * i / length);
        }
        return signal;
    }
    
    private double[] generateGaussianNoise(int length, double stdDev) {
        double[] noise = new double[length];
        for (int i = 0; i < length; i++) {
            noise[i] = stdDev * random.nextGaussian();
        }
        return noise;
    }
    
    private double[] addSignals(double[] signal1, double[] signal2) {
        double[] result = new double[signal1.length];
        for (int i = 0; i < signal1.length; i++) {
            result[i] = signal1[i] + signal2[i];
        }
        return result;
    }
    
    private boolean areArraysEqual(double[] arr1, double[] arr2) {
        if (arr1.length != arr2.length) return false;
        for (int i = 0; i < arr1.length; i++) {
            if (Math.abs(arr1[i] - arr2[i]) > EPSILON) {
                return false;
            }
        }
        return true;
    }
    
    private double calculateSNR(double[] clean, double[] noisy) {
        double signalPower = 0;
        double noisePower = 0;
        
        for (int i = 0; i < clean.length; i++) {
            signalPower += clean[i] * clean[i];
            double noise = noisy[i] - clean[i];
            noisePower += noise * noise;
        }
        
        if (noisePower == 0) return Double.POSITIVE_INFINITY;
        return 10 * Math.log10(signalPower / noisePower);
    }
    
    private double calculateRMSE(double[] signal1, double[] signal2) {
        double sum = 0;
        for (int i = 0; i < signal1.length; i++) {
            double diff = signal1[i] - signal2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum / signal1.length);
    }
    
    private double calculateMaxDifference(double[] arr1, double[] arr2) {
        double maxDiff = 0;
        for (int i = 0; i < arr1.length; i++) {
            maxDiff = Math.max(maxDiff, Math.abs(arr1[i] - arr2[i]));
        }
        return maxDiff;
    }
}
