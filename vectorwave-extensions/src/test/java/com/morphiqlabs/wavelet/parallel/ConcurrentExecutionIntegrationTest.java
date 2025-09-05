package com.morphiqlabs.wavelet.extensions.parallel;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.cwt.CWTConfig;
import com.morphiqlabs.wavelet.cwt.CWTResult;
import com.morphiqlabs.wavelet.cwt.RickerWavelet;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for concurrent execution paths in the parallel framework.
 * Verifies thread safety, resource management, and correct behavior under concurrent load.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ConcurrentExecutionIntegrationTest {
    
    private static final int NUM_THREADS = 8;
    private static final int OPERATIONS_PER_THREAD = 10;
    private static final int SIGNAL_SIZE = 2048;
    private static final int LEVELS = 3;
    
    private ExecutorService testExecutor;
    private ParallelConfig sharedConfig;
    private CountDownLatch startLatch;
    private AtomicInteger successCount;
    private AtomicInteger errorCount;
    private AtomicReference<Exception> firstError;
    
    @BeforeEach
    void setUp() {
        testExecutor = Executors.newFixedThreadPool(NUM_THREADS);
        sharedConfig = new ParallelConfig.Builder()
            .parallelThreshold(512)
            .enableMetrics(true)
            .build();
        startLatch = new CountDownLatch(1);
        successCount = new AtomicInteger(0);
        errorCount = new AtomicInteger(0);
        firstError = new AtomicReference<>();
    }
    
    @AfterEach
    void tearDown() throws InterruptedException {
        testExecutor.shutdown();
        assertTrue(testExecutor.awaitTermination(10, TimeUnit.SECONDS));
    }
    
    @Test
    @Order(1)
    @DisplayName("Concurrent CWT transforms should not interfere")
    void testConcurrentCWTTransforms() throws Exception {
        // Create shared CWT transform
        try (ParallelCWTTransform sharedTransform = new ParallelCWTTransform(
                new RickerWavelet(), CWTConfig.defaultConfig(), sharedConfig)) {
            
            List<Future<CWTResult>> futures = new ArrayList<>();
            
            // Submit concurrent tasks
            for (int i = 0; i < NUM_THREADS; i++) {
                final int threadId = i;
                Future<CWTResult> future = testExecutor.submit(() -> {
                    try {
                        // Wait for all threads to be ready
                        startLatch.await();
                        
                        // Generate unique signal for this thread
                        double[] signal = generateSignal(SIGNAL_SIZE, threadId);
                        double[] scales = generateScales(8 + threadId % 4);
                        
                        // Perform multiple operations
                        for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                            CWTResult result = sharedTransform.analyze(signal, scales);
                            assertNotNull(result);
                            assertEquals(scales.length, result.getCoefficients().length);
                            successCount.incrementAndGet();
                        }
                        
                        return sharedTransform.analyze(signal, scales);
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        firstError.compareAndSet(null, e);
                        throw new RuntimeException(e);
                    }
                });
                futures.add(future);
            }
            
            // Start all threads simultaneously
            startLatch.countDown();
            
            // Wait for all tasks to complete
            List<CWTResult> results = new ArrayList<>();
            for (Future<CWTResult> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS));
            }
            
            // Verify results
            assertEquals(NUM_THREADS, results.size());
            assertEquals(NUM_THREADS * OPERATIONS_PER_THREAD, successCount.get());
            assertEquals(0, errorCount.get());
            assertNull(firstError.get());
            
            // Each result should be unique (different signals)
            for (int i = 0; i < results.size(); i++) {
                for (int j = i + 1; j < results.size(); j++) {
                    assertNotSame(results.get(i), results.get(j));
                }
            }
        }
    }
    
    @Test
    @Order(2)
    @DisplayName("Concurrent multi-level transforms should work correctly")
    void testConcurrentMultiLevelTransforms() throws Exception {
        Wavelet wavelet = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.PERIODIC;
        
        ParallelMultiLevelTransform sharedTransform = new ParallelMultiLevelTransform(
            wavelet, mode, sharedConfig);
        
        List<Future<Double>> futures = new ArrayList<>();
        
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            Future<Double> future = testExecutor.submit(() -> {
                try {
                    startLatch.await();
                    
                    double[] signal = generateSignal(SIGNAL_SIZE, threadId);
                    double sumSquaredError = 0.0;
                    
                    for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                        // Decompose
                        MultiLevelMODWTResult decomposition = sharedTransform.decompose(signal, LEVELS);
                        assertNotNull(decomposition);
                        
                        // Reconstruct
                        double[] reconstructed = sharedTransform.reconstruct(decomposition);
                        assertNotNull(reconstructed);
                        
                        // Calculate error
                        for (int j = 0; j < signal.length; j++) {
                            double error = signal[j] - reconstructed[j];
                            sumSquaredError += error * error;
                        }
                        
                        successCount.incrementAndGet();
                    }
                    
                    return sumSquaredError;
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    firstError.compareAndSet(null, e);
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }
        
        startLatch.countDown();
        
        // Collect results
        double totalError = 0.0;
        for (Future<Double> future : futures) {
            totalError += future.get(30, TimeUnit.SECONDS);
        }
        
        // Verify
        assertEquals(NUM_THREADS * OPERATIONS_PER_THREAD, successCount.get());
        assertEquals(0, errorCount.get());
        assertNull(firstError.get());
        
        // Reconstruction error should be minimal
        double avgError = totalError / (NUM_THREADS * OPERATIONS_PER_THREAD * SIGNAL_SIZE);
        assertTrue(avgError < 1e-10, "Average reconstruction error too high: " + avgError);
    }
    
    @Test
    @Order(3)
    @DisplayName("Concurrent denoising operations should not interfere")
    void testConcurrentDenoising() throws Exception {
        ParallelWaveletDenoiser sharedDenoiser = new ParallelWaveletDenoiser(
            Daubechies.DB4, BoundaryMode.PERIODIC, sharedConfig);
        
        CyclicBarrier barrier = new CyclicBarrier(NUM_THREADS);
        List<Future<Double>> futures = new ArrayList<>();
        
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            Future<Double> future = testExecutor.submit(() -> {
                try {
                    barrier.await(); // Synchronize start
                    
                    // Generate noisy signal
                    double[] cleanSignal = generateSignal(SIGNAL_SIZE, threadId);
                    double[] noisySignal = addNoise(cleanSignal, 0.1 * (threadId + 1));
                    
                    double totalSNR = 0.0;
                    
                    for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                        // Denoise
                        double[] denoised = sharedDenoiser.denoiseMultiLevel(
                            noisySignal, LEVELS,
                            WaveletDenoiser.ThresholdMethod.UNIVERSAL,
                            WaveletDenoiser.ThresholdType.SOFT
                        );
                        
                        // Calculate SNR improvement
                        double noisePower = calculatePower(noisySignal, cleanSignal);
                        double denoisedPower = calculatePower(denoised, cleanSignal);
                        double snrImprovement = 10 * Math.log10(noisePower / denoisedPower);
                        totalSNR += snrImprovement;
                        
                        successCount.incrementAndGet();
                    }
                    
                    return totalSNR / OPERATIONS_PER_THREAD;
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    firstError.compareAndSet(null, e);
                    throw new RuntimeException(e);
                }
            });
            futures.add(future);
        }
        
        // Collect results
        List<Double> snrImprovements = new ArrayList<>();
        for (Future<Double> future : futures) {
            snrImprovements.add(future.get(30, TimeUnit.SECONDS));
        }
        
        // Verify
        assertEquals(NUM_THREADS * OPERATIONS_PER_THREAD, successCount.get());
        assertEquals(0, errorCount.get());
        assertNull(firstError.get());
        
        // All threads should achieve positive SNR improvement
        for (Double snr : snrImprovements) {
            assertTrue(snr > 0, "SNR improvement should be positive");
        }
    }
    
    @Test
    @Order(4)
    @DisplayName("Mixed concurrent operations should work correctly")
    void testMixedConcurrentOperations() throws Exception {
        // Create multiple transform types
        ParallelCWTTransform cwtTransform = new ParallelCWTTransform(
            new RickerWavelet(), CWTConfig.defaultConfig(), sharedConfig);
        ParallelMultiLevelTransform modwtTransform = new ParallelMultiLevelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, sharedConfig);
        ParallelWaveletDenoiser denoiser = new ParallelWaveletDenoiser(
            Daubechies.DB4, BoundaryMode.PERIODIC, sharedConfig);
        
        try {
            List<Future<?>> futures = new ArrayList<>();
            
            // Mix different operation types
            for (int i = 0; i < NUM_THREADS; i++) {
                final int threadId = i;
                final int operationType = i % 3;
                
                Future<?> future = testExecutor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        double[] signal = generateSignal(SIGNAL_SIZE, threadId);
                        
                        for (int op = 0; op < OPERATIONS_PER_THREAD; op++) {
                            switch (operationType) {
                                case 0 -> {
                                    // CWT operation
                                    double[] scales = generateScales(10);
                                    CWTResult result = cwtTransform.analyze(signal, scales);
                                    assertNotNull(result);
                                }
                                case 1 -> {
                                    // MODWT operation
                                    MultiLevelMODWTResult result = modwtTransform.decompose(signal, LEVELS);
                                    double[] reconstructed = modwtTransform.reconstruct(result);
                                    assertNotNull(reconstructed);
                                }
                                case 2 -> {
                                    // Denoising operation
                                    double[] noisy = addNoise(signal, 0.1);
                                    double[] denoised = denoiser.denoiseMultiLevel(
                                        noisy, LEVELS,
                                        WaveletDenoiser.ThresholdMethod.UNIVERSAL,
                                        WaveletDenoiser.ThresholdType.SOFT
                                    );
                                    assertNotNull(denoised);
                                }
                            }
                            successCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        firstError.compareAndSet(null, e);
                        throw new RuntimeException(e);
                    }
                });
                futures.add(future);
            }
            
            startLatch.countDown();
            
            // Wait for all operations
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
            
            // Verify
            assertEquals(NUM_THREADS * OPERATIONS_PER_THREAD, successCount.get());
            assertEquals(0, errorCount.get());
            assertNull(firstError.get());
            
        } finally {
            cwtTransform.close();
        }
    }
    
    @Test
    @Order(5)
    @DisplayName("Resource cleanup during concurrent execution")
    void testResourceCleanupUnderLoad() throws Exception {
        List<ParallelCWTTransform> transforms = new ArrayList<>();
        AtomicInteger activeTransforms = new AtomicInteger(0);
        
        try {
            List<Future<?>> futures = new ArrayList<>();
            
            for (int i = 0; i < NUM_THREADS * 2; i++) {
                Future<?> future = testExecutor.submit(() -> {
                    try {
                        startLatch.await();
                        
                        // Create and use transform in try-with-resources
                        try (ParallelCWTTransform transform = new ParallelCWTTransform(
                                new RickerWavelet(), CWTConfig.defaultConfig(), sharedConfig)) {
                            
                            activeTransforms.incrementAndGet();
                            transforms.add(transform);
                            
                            double[] signal = generateSignal(1024, ThreadLocalRandom.current().nextInt());
                            double[] scales = generateScales(5);
                            
                            for (int op = 0; op < 5; op++) {
                                CWTResult result = transform.analyze(signal, scales);
                                assertNotNull(result);
                                Thread.sleep(10); // Simulate work
                            }
                            
                            successCount.incrementAndGet();
                        } finally {
                            activeTransforms.decrementAndGet();
                        }
                        
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        firstError.compareAndSet(null, e);
                    }
                });
                futures.add(future);
            }
            
            startLatch.countDown();
            
            // Wait for all
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
            
            // Verify all transforms were closed
            assertEquals(0, activeTransforms.get());
            assertEquals(0, errorCount.get());
            
            // Verify all transforms are closed
            for (ParallelCWTTransform transform : transforms) {
                assertTrue(transform.isClosed());
            }
            
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        }
    }
    
    @Test
    @Order(6) 
    @DisplayName("Thread pool saturation handling")
    void testThreadPoolSaturation() throws Exception {
        // Use smaller thread pool to force saturation
        ParallelConfig limitedConfig = new ParallelConfig.Builder()
            .parallelismLevel(2)
            .parallelThreshold(100)
            .enableMetrics(true)
            .build();
        
        ParallelMultiLevelTransform transform = new ParallelMultiLevelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, limitedConfig);
        
        // Submit many tasks to saturate the pool
        int numTasks = 50;
        List<CompletableFuture<MultiLevelMODWTResult>> futures = new ArrayList<>();
        
        for (int i = 0; i < numTasks; i++) {
            final int taskId = i;
            CompletableFuture<MultiLevelMODWTResult> future = CompletableFuture.supplyAsync(() -> {
                double[] signal = generateSignal(1024, taskId);
                return transform.decompose(signal, 2);
            }, testExecutor);
            futures.add(future);
        }
        
        // Wait for all with timeout
        CompletableFuture<Void> allOf = CompletableFuture.allOf(
            futures.toArray(new CompletableFuture<?>[0]));
        allOf.get(60, TimeUnit.SECONDS);
        
        // Verify all completed successfully
        for (CompletableFuture<MultiLevelMODWTResult> future : futures) {
            assertTrue(future.isDone());
            assertFalse(future.isCompletedExceptionally());
            assertNotNull(future.get());
        }
    }
    
    // Helper methods
    
    private double[] generateSignal(int size, int seed) {
        Random random = new Random(seed);
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            double t = 2.0 * Math.PI * i / size;
            signal[i] = Math.sin(t) + 0.5 * Math.sin(3 * t) + 0.1 * random.nextGaussian();
        }
        return signal;
    }
    
    private double[] generateScales(int count) {
        double[] scales = new double[count];
        for (int i = 0; i < count; i++) {
            scales[i] = Math.pow(2, i * 0.5);
        }
        return scales;
    }
    
    private double[] addNoise(double[] signal, double noiseLevel) {
        Random random = new Random();
        double[] noisy = signal.clone();
        for (int i = 0; i < noisy.length; i++) {
            noisy[i] += noiseLevel * random.nextGaussian();
        }
        return noisy;
    }
    
    private double calculatePower(double[] signal1, double[] signal2) {
        double power = 0.0;
        for (int i = 0; i < signal1.length; i++) {
            double diff = signal1[i] - signal2[i];
            power += diff * diff;
        }
        return power / signal1.length;
    }
}
