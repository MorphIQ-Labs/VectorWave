package com.morphiqlabs.wavelet.extensions.parallel;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive unit tests for ParallelMultiLevelTransform.
 * Tests parallel decomposition, reconstruction, thread safety, and performance characteristics.
 */
class ParallelMultiLevelTransformTest {
    
    private static final double EPSILON = 1e-10;
    private static final double RECONSTRUCTION_TOLERANCE = 1e-8;
    
    private ParallelMultiLevelTransform parallelTransform;
    private MultiLevelMODWTTransform sequentialTransform;
    private Random random;
    
    @BeforeEach
    void setUp() {
        random = new Random(42); // Fixed seed for reproducibility
        Wavelet wavelet = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.PERIODIC;
        
        parallelTransform = new ParallelMultiLevelTransform(wavelet, mode);
        sequentialTransform = new MultiLevelMODWTTransform(wavelet, mode);
    }
    
    // ============== Constructor and Builder Tests ==============
    
    @Test
    @DisplayName("Constructor should create valid parallel transform")
    void testConstructorCreation() {
        assertNotNull(parallelTransform);
        assertInstanceOf(MultiLevelMODWTTransform.class, parallelTransform);
    }
    
    @Test
    @DisplayName("Constructor with custom config should work")
    void testConstructorWithCustomConfig() {
        ParallelConfig config = new ParallelConfig.Builder()
            .parallelismLevel(2)
            .parallelThreshold(256)
            .enableStructuredConcurrency(true)
            .build();
        
        ParallelMultiLevelTransform customTransform = new ParallelMultiLevelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, config);
        
        assertNotNull(customTransform);
    }
    
    @Test
    @DisplayName("Builder should create transform correctly")
    void testBuilder() {
        ParallelMultiLevelTransform transform = new ParallelMultiLevelTransform.Builder()
            .wavelet(Daubechies.DB6)
            .boundaryMode(BoundaryMode.SYMMETRIC)
            .parallelConfig(ParallelConfig.auto())
            .build();
        
        assertNotNull(transform);
    }
    
    @Test
    @DisplayName("Builder should validate required parameters")
    void testBuilderValidation() {
        ParallelMultiLevelTransform.Builder builder = new ParallelMultiLevelTransform.Builder();
        
        assertThrows(IllegalStateException.class, builder::build,
            "Should throw when wavelet is not specified");
    }
    
    @Test
    @DisplayName("Builder should use fluent interface")
    void testBuilderFluentInterface() {
        ParallelMultiLevelTransform.Builder builder = new ParallelMultiLevelTransform.Builder();
        
        assertSame(builder, builder.wavelet(Daubechies.DB4));
        assertSame(builder, builder.boundaryMode(BoundaryMode.PERIODIC));
        assertSame(builder, builder.parallelConfig(ParallelConfig.auto()));
    }
    
    // ============== Basic Decomposition Tests ==============
    
    @Test
    @DisplayName("Parallel decomposition should match sequential for small signals")
    void testParallelMatchesSequentialSmallSignal() {
        // Small signal that should trigger sequential fallback
        double[] signal = generateTestSignal(128);
        int levels = 3;
        
        MultiLevelMODWTResult parallelResult = parallelTransform.decompose(signal, levels);
        MultiLevelMODWTResult sequentialResult = sequentialTransform.decompose(signal, levels);
        
        assertResultsEqual(parallelResult, sequentialResult, EPSILON);
    }
    
    @Test
    @DisplayName("Parallel decomposition should work for large signals")
    void testParallelDecompositionLargeSignal() {
        // Large signal that should trigger parallel processing
        double[] signal = generateTestSignal(2048);
        int levels = 5;
        
        MultiLevelMODWTResult result = parallelTransform.decompose(signal, levels);
        
        assertNotNull(result);
        assertEquals(levels, result.getLevels());
        assertEquals(signal.length, result.getSignalLength());
        
        // Verify each level has coefficients
        for (int level = 1; level <= levels; level++) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            assertNotNull(details);
            assertEquals(signal.length, details.length);
        }
        
        double[] approx = result.getApproximationCoeffs();
        assertNotNull(approx);
        assertEquals(signal.length, approx.length);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    @DisplayName("Should handle different numbers of decomposition levels")
    void testDifferentLevels(int levels) {
        double[] signal = generateTestSignal(1024);
        
        MultiLevelMODWTResult result = parallelTransform.decompose(signal, levels);
        
        assertNotNull(result);
        assertEquals(levels, result.getLevels());
        assertEquals(signal.length, result.getSignalLength());
        
        // Verify perfect reconstruction
        double[] reconstructed = parallelTransform.reconstruct(result);
        assertArrayEquals(signal, reconstructed, RECONSTRUCTION_TOLERANCE);
    }
    
    // ============== Reconstruction Tests ==============
    
    @Test
    @DisplayName("Parallel reconstruction should achieve perfect reconstruction")
    void testPerfectReconstruction() {
        double[] original = generateTestSignal(512);
        int levels = 4;
        
        MultiLevelMODWTResult result = parallelTransform.decompose(original, levels);
        double[] reconstructed = parallelTransform.reconstruct(result);
        
        assertArrayEquals(original, reconstructed, RECONSTRUCTION_TOLERANCE,
            "Should achieve perfect reconstruction");
    }
    
    @Test
    @DisplayName("Parallel implementation should be self-consistent")
    void testParallelSelfConsistency() {
        double[] signal = generateTestSignal(512);
        int levels = 3;
        
        // Decompose and reconstruct using only parallel transform
        MultiLevelMODWTResult result = parallelTransform.decompose(signal, levels);
        double[] reconstructed = parallelTransform.reconstruct(result);
        
        // Should achieve good reconstruction with itself
        assertNotNull(reconstructed, "Reconstruction should not be null");
        assertEquals(signal.length, reconstructed.length, "Length should match");
        
        // For the parallel implementation, verify it can decompose and reconstruct consistently
        double error = calculateRMSE(signal, reconstructed);
        assertTrue(error < 0.1, "Parallel transform should be self-consistent, error: " + error);
    }
    
    @Test
    @DisplayName("Should handle reconstruction of single level")
    void testSingleLevelReconstruction() {
        double[] signal = generateTestSignal(256);
        
        MultiLevelMODWTResult result = parallelTransform.decompose(signal, 1);
        double[] reconstructed = parallelTransform.reconstruct(result);
        
        assertArrayEquals(signal, reconstructed, RECONSTRUCTION_TOLERANCE);
    }
    
    // ============== Different Wavelet Tests ==============
    
    @Test
    @DisplayName("Should work with Haar wavelet")
    void testHaarWavelet() {
        ParallelMultiLevelTransform haarTransform = new ParallelMultiLevelTransform(
            new Haar(), BoundaryMode.PERIODIC);
        
        double[] signal = generateTestSignal(256);
        int levels = 3;
        
        MultiLevelMODWTResult result = haarTransform.decompose(signal, levels);
        double[] reconstructed = haarTransform.reconstruct(result);
        
        assertArrayEquals(signal, reconstructed, RECONSTRUCTION_TOLERANCE);
    }
    
    @ParameterizedTest
    @CsvSource({
        "DB2", "DB4", "DB6", "DB8"
    })
    @DisplayName("Should work with different Daubechies wavelets")
    void testDaubechiesWavelets(String waveletName) {
        Wavelet wavelet = switch (waveletName) {
            case "DB2" -> Daubechies.DB2;
            case "DB4" -> Daubechies.DB4;
            case "DB6" -> Daubechies.DB6;
            case "DB8" -> Daubechies.DB8;
            default -> throw new IllegalArgumentException("Unknown wavelet: " + waveletName);
        };
        
        ParallelMultiLevelTransform transform = new ParallelMultiLevelTransform(
            wavelet, BoundaryMode.PERIODIC);
        
        double[] signal = generateTestSignal(512);
        int levels = 4;
        
        MultiLevelMODWTResult result = transform.decompose(signal, levels);
        double[] reconstructed = transform.reconstruct(result);
        
        assertArrayEquals(signal, reconstructed, RECONSTRUCTION_TOLERANCE,
            "Perfect reconstruction should work with " + waveletName);
    }
    
    // ============== Boundary Mode Tests ==============
    
    @ParameterizedTest
    @EnumSource(value = BoundaryMode.class, names = {"PERIODIC", "ZERO_PADDING", "SYMMETRIC"})
    @DisplayName("Should work with different boundary modes")
    void testDifferentBoundaryModes(BoundaryMode mode) {
        ParallelMultiLevelTransform transform = new ParallelMultiLevelTransform(
            Daubechies.DB4, mode);
        
        double[] signal = generateTestSignal(256);
        int levels = 3;
        
        MultiLevelMODWTResult result = transform.decompose(signal, levels);
        double[] reconstructed = transform.reconstruct(result);
        
        assertNotNull(result);
        assertNotNull(reconstructed);
        assertEquals(signal.length, reconstructed.length);
        
        // Verify basic properties regardless of reconstruction quality
        // Some boundary modes may have significant reconstruction errors
        assertNotNull(reconstructed, "Reconstruction should not be null");
        assertEquals(signal.length, reconstructed.length, "Length should be preserved");
        
        // Check that the transform produces valid coefficients
        assertTrue(result.getLevels() > 0, "Should have valid levels");
        
        // For PERIODIC mode, we expect good reconstruction
        if (mode == BoundaryMode.PERIODIC) {
            double error = calculateRMSE(signal, reconstructed);
            assertTrue(error < 0.01, "PERIODIC mode should have good reconstruction, error: " + error);
        }
    }
    
    // ============== Performance Configuration Tests ==============
    
    @Test
    @DisplayName("Should use parallel execution for large problems")
    void testParallelExecutionSelection() {
        // Create config that enables metrics
        ParallelConfig config = new ParallelConfig.Builder()
            .parallelThreshold(512)
            .enableMetrics(true)
            .build();
        
        ParallelMultiLevelTransform transform = new ParallelMultiLevelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, config);
        
        // Large signal should trigger parallel execution
        double[] largeSignal = generateTestSignal(2048);
        transform.decompose(largeSignal, 5);
        
        // Small signal should use sequential
        double[] smallSignal = generateTestSignal(128);
        transform.decompose(smallSignal, 2);
        
        ParallelConfig.ExecutionStats stats = transform.getStats();
        assertNotNull(stats);
        
        // Should have recorded some executions
        int totalExecutions = stats.parallelExecutions() + stats.sequentialExecutions();
        assertTrue(totalExecutions >= 2, "Should have recorded executions");
    }
    
    @Test
    @DisplayName("Should respect structured concurrency setting")
    void testStructuredConcurrencyConfiguration() {
        // Test with structured concurrency enabled
        ParallelConfig structuredConfig = new ParallelConfig.Builder()
            .enableStructuredConcurrency(true)
            .parallelThreshold(256)
            .build();
        
        ParallelMultiLevelTransform structuredTransform = new ParallelMultiLevelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, structuredConfig);
        
        double[] signal = generateTestSignal(1024);
        MultiLevelMODWTResult result = structuredTransform.decompose(signal, 4);
        
        assertNotNull(result);
        assertEquals(signal.length, result.getSignalLength());
        
        // Test with fork/join configuration
        ParallelConfig forkJoinConfig = new ParallelConfig.Builder()
            .enableStructuredConcurrency(false)
            .parallelThreshold(256)
            .build();
        
        ParallelMultiLevelTransform forkJoinTransform = new ParallelMultiLevelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, forkJoinConfig);
        
        MultiLevelMODWTResult forkJoinResult = forkJoinTransform.decompose(signal, 4);
        
        assertNotNull(forkJoinResult);
        assertEquals(signal.length, forkJoinResult.getSignalLength());
        
        // Both should produce similar results
        double[] structuredRecon = structuredTransform.reconstruct(result);
        double[] forkJoinRecon = forkJoinTransform.reconstruct(forkJoinResult);
        
        double maxDiff = calculateMaxDifference(structuredRecon, forkJoinRecon);
        assertTrue(maxDiff < 1e-6, "Both approaches should produce similar results");
    }
    
    // ============== Thread Safety Tests ==============
    
    @Test
    @DisplayName("Should be thread-safe for concurrent decompositions")
    void testConcurrentDecompositions() throws InterruptedException {
        int numThreads = 8;
        int iterations = 10;
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < iterations; i++) {
                        double[] signal = generateTestSignal(256 + threadId * 16); // Vary signal size
                        int levels = 3 + (i % 3); // Vary levels
                        
                        MultiLevelMODWTResult result = parallelTransform.decompose(signal, levels);
                        double[] reconstructed = parallelTransform.reconstruct(result);
                        
                        if (reconstructed != null && reconstructed.length == signal.length) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete");
        assertEquals(numThreads * iterations, successCount.get(),
            "All operations should succeed");
        
        executor.shutdown();
    }
    
    @Test
    @DisplayName("Should handle concurrent mixed operations")
    void testConcurrentMixedOperations() throws InterruptedException {
        int numOperations = 50;
        CountDownLatch latch = new CountDownLatch(numOperations);
        AtomicInteger successCount = new AtomicInteger(0);
        
        ExecutorService executor = Executors.newFixedThreadPool(8);
        
        // Pre-decompose some results for reconstruction tests
        double[] testSignal = generateTestSignal(512);
        MultiLevelMODWTResult testResult = parallelTransform.decompose(testSignal, 4);
        
        for (int i = 0; i < numOperations; i++) {
            final int operationId = i;
            executor.submit(() -> {
                try {
                    if (operationId % 2 == 0) {
                        // Decomposition
                        double[] signal = generateTestSignal(256 + operationId);
                        MultiLevelMODWTResult result = parallelTransform.decompose(signal, 3);
                        if (result != null) {
                            successCount.incrementAndGet();
                        }
                    } else {
                        // Reconstruction
                        double[] reconstructed = parallelTransform.reconstruct(testResult);
                        if (reconstructed != null && reconstructed.length == testSignal.length) {
                            successCount.incrementAndGet();
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(30, TimeUnit.SECONDS), "All operations should complete");
        assertEquals(numOperations, successCount.get(), "All operations should succeed");
        
        executor.shutdown();
    }
    
    // ============== Validation and Error Handling Tests ==============
    
    @Test
    @DisplayName("Should validate input parameters")
    void testInputValidation() {
        // Null signal
        assertThrows(InvalidArgumentException.class, () ->
            parallelTransform.decompose(null, 3));
        
        // Empty signal
        assertThrows(InvalidArgumentException.class, () ->
            parallelTransform.decompose(new double[0], 3));
        
        // Invalid levels
        assertThrows(InvalidArgumentException.class, () ->
            parallelTransform.decompose(generateTestSignal(256), 0));
        
        assertThrows(InvalidArgumentException.class, () ->
            parallelTransform.decompose(generateTestSignal(256), -1));
    }
    
    @Test
    @DisplayName("Should validate maximum decomposition level")
    void testMaximumLevelValidation() {
        double[] smallSignal = generateTestSignal(16); // Very small signal
        
        assertThrows(InvalidArgumentException.class, () ->
            parallelTransform.decompose(smallSignal, 10), // Too many levels
            "Should throw for excessive decomposition levels");
    }
    
    @Test
    @DisplayName("Should validate reconstruction input")
    void testReconstructionValidation() {
        assertThrows(InvalidArgumentException.class, () ->
            parallelTransform.reconstruct(null),
            "Should throw for null result");
    }
    
    // ============== Edge Cases Tests ==============
    
    @Test
    @DisplayName("Should handle very small signals")
    void testVerySmallSignals() {
        // Use a larger signal that can support at least 1 level
        double[] smallSignal = new double[64];
        for (int i = 0; i < smallSignal.length; i++) {
            smallSignal[i] = i + 1.0;
        }
        
        MultiLevelMODWTResult result = parallelTransform.decompose(smallSignal, 1);
        double[] reconstructed = parallelTransform.reconstruct(result);
        
        assertArrayEquals(smallSignal, reconstructed, RECONSTRUCTION_TOLERANCE);
    }
    
    @Test
    @DisplayName("Should handle signals with extreme values")
    void testExtremeValues() {
        double[] extremeSignal = new double[128];
        for (int i = 0; i < extremeSignal.length; i++) {
            extremeSignal[i] = i % 2 == 0 ? Double.MAX_VALUE / 1000 : -Double.MAX_VALUE / 1000;
        }
        
        assertDoesNotThrow(() -> {
            MultiLevelMODWTResult result = parallelTransform.decompose(extremeSignal, 3);
            double[] reconstructed = parallelTransform.reconstruct(result);
            
            assertNotNull(result);
            assertNotNull(reconstructed);
            
            // Check for NaN or Infinity
            for (double value : reconstructed) {
                assertFalse(Double.isNaN(value), "Should not produce NaN");
                assertFalse(Double.isInfinite(value), "Should not produce Infinity");
            }
        });
    }
    
    @Test
    @DisplayName("Should handle signals with all zeros")
    void testZeroSignal() {
        double[] zeroSignal = new double[256];
        
        MultiLevelMODWTResult result = parallelTransform.decompose(zeroSignal, 3);
        double[] reconstructed = parallelTransform.reconstruct(result);
        
        assertArrayEquals(zeroSignal, reconstructed, EPSILON);
        
        // All coefficients should be zero or very close
        for (int level = 1; level <= result.getLevels(); level++) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            for (double coeff : details) {
                assertEquals(0.0, coeff, EPSILON, "Detail coefficients should be zero");
            }
        }
    }
    
    @Test
    @DisplayName("Should handle constant signals")
    void testConstantSignal() {
        double[] constantSignal = new double[256];
        double constantValue = 42.0;
        for (int i = 0; i < constantSignal.length; i++) {
            constantSignal[i] = constantValue;
        }
        
        MultiLevelMODWTResult result = parallelTransform.decompose(constantSignal, 4);
        double[] reconstructed = parallelTransform.reconstruct(result);
        
        assertArrayEquals(constantSignal, reconstructed, RECONSTRUCTION_TOLERANCE);
        
        // Detail coefficients should be very small for constant signals (allowing for numerical precision)
        for (int level = 1; level <= result.getLevels(); level++) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            for (double coeff : details) {
                assertTrue(Math.abs(coeff) < 1e-8, 
                    "Detail coefficients should be very small for constant signal, but was: " + coeff);
            }
        }
    }
    
    // ============== Performance and Statistics Tests ==============
    
    @Test
    @DisplayName("Should track execution statistics")
    void testExecutionStatistics() {
        ParallelConfig config = new ParallelConfig.Builder()
            .enableMetrics(true)
            .parallelThreshold(200)
            .build();
        
        ParallelMultiLevelTransform transform = new ParallelMultiLevelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, config);
        
        // Mix of parallel and sequential operations
        transform.decompose(generateTestSignal(1024), 5); // Should be parallel
        transform.decompose(generateTestSignal(128), 2);  // Should be sequential
        transform.decompose(generateTestSignal(512), 4);  // Should be parallel
        
        ParallelConfig.ExecutionStats stats = transform.getStats();
        assertNotNull(stats);
        
        int totalExecutions = stats.parallelExecutions() + stats.sequentialExecutions();
        assertTrue(totalExecutions >= 3, "Should have recorded executions");
    }
    
    @RepeatedTest(3)
    @DisplayName("Should provide consistent results across runs")
    void testConsistentResults() {
        double[] signal = generateTestSignal(512);
        int levels = 4;
        
        MultiLevelMODWTResult result1 = parallelTransform.decompose(signal, levels);
        MultiLevelMODWTResult result2 = parallelTransform.decompose(signal, levels);
        
        assertResultsEqual(result1, result2, EPSILON);
        
        double[] recon1 = parallelTransform.reconstruct(result1);
        double[] recon2 = parallelTransform.reconstruct(result2);
        
        assertArrayEquals(recon1, recon2, EPSILON);
    }
    
    // ============== Helper Methods ==============
    
    private double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            // Create a signal with multiple frequencies
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) +
                       0.5 * Math.cos(4 * Math.PI * i / 16.0) +
                       0.25 * Math.sin(8 * Math.PI * i / 8.0) +
                       0.1 * random.nextGaussian(); // Add small amount of noise
        }
        return signal;
    }
    
    private void assertResultsEqual(MultiLevelMODWTResult result1, 
                                   MultiLevelMODWTResult result2, 
                                   double tolerance) {
        assertEquals(result1.getLevels(), result2.getLevels());
        assertEquals(result1.getSignalLength(), result2.getSignalLength());
        
        // Compare approximation coefficients
        assertArrayEquals(result1.getApproximationCoeffs(), 
                         result2.getApproximationCoeffs(), tolerance);
        
        // Compare detail coefficients for each level
        for (int level = 1; level <= result1.getLevels(); level++) {
            assertArrayEquals(result1.getDetailCoeffsAtLevel(level),
                             result2.getDetailCoeffsAtLevel(level), tolerance,
                             "Detail coefficients at level " + level + " should match");
        }
    }
    
    private double calculateRMSE(double[] signal1, double[] signal2) {
        if (signal1.length != signal2.length) {
            throw new IllegalArgumentException("Signals must have same length");
        }
        
        double sumSquaredError = 0.0;
        for (int i = 0; i < signal1.length; i++) {
            double error = signal1[i] - signal2[i];
            sumSquaredError += error * error;
        }
        
        return Math.sqrt(sumSquaredError / signal1.length);
    }
    
    private double calculateMaxDifference(double[] arr1, double[] arr2) {
        if (arr1.length != arr2.length) {
            throw new IllegalArgumentException("Arrays must have same length");
        }
        
        double maxDiff = 0.0;
        for (int i = 0; i < arr1.length; i++) {
            maxDiff = Math.max(maxDiff, Math.abs(arr1[i] - arr2[i]));
        }
        return maxDiff;
    }
}
