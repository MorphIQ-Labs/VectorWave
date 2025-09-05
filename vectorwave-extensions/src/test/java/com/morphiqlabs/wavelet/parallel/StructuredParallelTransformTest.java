package com.morphiqlabs.wavelet.extensions.parallel;

import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.BoundaryMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StructuredParallelTransform.
 */
class StructuredParallelTransformTest {
    
    private static final double TOLERANCE = 1e-10;
    private StructuredParallelTransform transform;
    private ParallelConfig config;
    
    @BeforeEach
    void setUp() {
        config = new ParallelConfig.Builder()
            .parallelThreshold(256)
            .build();
        transform = new StructuredParallelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, config);
    }
    
    @Test
    @DisplayName("Forward batch transform should process signals correctly")
    void testForwardBatch() {
        double[][] signals = generateTestSignals(5, 512);
        
        MODWTResult[] results = transform.forwardBatch(signals);
        
        assertEquals(signals.length, results.length, "Should return result for each signal");
        
        for (MODWTResult result : results) {
            assertNotNull(result, "Result should not be null");
            assertNotNull(result.approximationCoeffs(), "Approximation should not be null");
            assertNotNull(result.detailCoeffs(), "Details should not be null");
        }
    }
    
    @Test
    @DisplayName("Inverse batch transform should reconstruct signals")
    void testInverseBatch() {
        double[][] originalSignals = generateTestSignals(3, 512);
        
        // Forward transform
        MODWTResult[] results = transform.forwardBatch(originalSignals);
        
        // Inverse transform
        double[][] reconstructed = transform.inverseBatch(results);
        
        assertEquals(originalSignals.length, reconstructed.length, 
            "Should reconstruct same number of signals");
        
        for (int i = 0; i < originalSignals.length; i++) {
            assertArrayEquals(originalSignals[i], reconstructed[i], TOLERANCE,
                "Signal " + i + " should be reconstructed accurately");
        }
    }
    
    @Test
    @DisplayName("Transform with timeout should respect deadline")
    @Timeout(2)
    void testTransformWithTimeout() {
        // Use a very low threshold to force parallelization
        var timeoutConfig = new ParallelConfig.Builder()
            .parallelThreshold(1)  // Force parallel processing
            .build();
        var timeoutTransform = new StructuredParallelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, timeoutConfig);
            
        // Create signals that would take long to process
        double[][] signals = generateTestSignals(100, 4096);
        
        // Use very short timeout
        assertThrows(Exception.class, () -> {
            timeoutTransform.forwardBatchWithTimeout(signals, 0); // 0ms timeout
        });
    }
    
    @Test
    @DisplayName("Empty input should return empty output")
    void testEmptyInput() {
        double[][] emptySignals = new double[0][];
        MODWTResult[] results = transform.forwardBatch(emptySignals);
        
        assertNotNull(results, "Results should not be null");
        assertEquals(0, results.length, "Should return empty array");
        
        MODWTResult[] emptyResults = new MODWTResult[0];
        double[][] reconstructed = transform.inverseBatch(emptyResults);
        
        assertNotNull(reconstructed, "Reconstructed should not be null");
        assertEquals(0, reconstructed.length, "Should return empty array");
    }
    
    @Test
    @DisplayName("Sequential processing for small batches")
    void testSequentialProcessing() {
        // Create small batch that should process sequentially
        double[][] signals = generateTestSignals(1, 100);
        
        MODWTResult[] results = transform.forwardBatch(signals);
        
        assertEquals(1, results.length, "Should process single signal");
        assertNotNull(results[0], "Result should not be null");
    }
    
    @Test
    @DisplayName("Async forward transform should complete")
    void testAsyncForward() throws Exception {
        double[] signal = generateTestSignal(512);
        
        CompletableFuture<MODWTResult> future = transform.forwardAsync(signal);
        
        assertNotNull(future, "Future should not be null");
        
        MODWTResult result = future.get(5, TimeUnit.SECONDS);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(512, result.approximationCoeffs().length, 
            "Should have correct length");
    }
    
    @Test
    @DisplayName("Async inverse transform should complete")
    void testAsyncInverse() throws Exception {
        double[] signal = generateTestSignal(512);
        
        // First do forward transform
        CompletableFuture<MODWTResult> forwardFuture = transform.forwardAsync(signal);
        MODWTResult result = forwardFuture.get(5, TimeUnit.SECONDS);
        
        // Then async inverse
        CompletableFuture<double[]> inverseFuture = transform.inverseAsync(result);
        double[] reconstructed = inverseFuture.get(5, TimeUnit.SECONDS);
        
        assertArrayEquals(signal, reconstructed, TOLERANCE,
            "Should reconstruct signal accurately");
    }
    
    @Test
    @DisplayName("Chunked transform for large signals")
    void testChunkedTransform() {
        double[] largeSignal = generateTestSignal(4096);
        
        MODWTResult result = transform.forwardChunked(largeSignal, 1024);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(4096, result.approximationCoeffs().length,
            "Should process entire signal");
    }
    
    @Test
    @DisplayName("Progress reporting during batch transform")
    void testProgressReporting() {
        double[][] signals = generateTestSignals(10, 256);
        AtomicInteger progressCount = new AtomicInteger(0);
        AtomicInteger lastCompleted = new AtomicInteger(0);
        
        MODWTResult[] results = transform.forwardBatchWithProgress(signals, 
            (completed, total) -> {
                progressCount.incrementAndGet();
                lastCompleted.set(completed);
                assertEquals(10, total, "Total should be 10");
                assertTrue(completed <= total, "Completed should not exceed total");
            });
        
        assertEquals(signals.length, results.length, "Should process all signals");
        assertTrue(progressCount.get() > 0, "Progress should be reported");
        assertEquals(10, lastCompleted.get(), "Should report all completed");
    }
    
    @Test
    @DisplayName("Cancellation of async operations")
    void testAsyncCancellation() {
        double[] signal = generateTestSignal(2048);
        
        CompletableFuture<MODWTResult> future = transform.forwardAsync(signal);
        
        // Cancel the future
        boolean cancelled = future.cancel(true);
        
        assertTrue(cancelled || future.isDone(), 
            "Should be cancelled or already done");
        
        if (cancelled) {
            assertTrue(future.isCancelled(), "Should be marked as cancelled");
            assertThrows(CancellationException.class, () -> future.get(),
                "Should throw CancellationException");
        }
    }
    
    @Test
    @DisplayName("Concurrent batch operations")
    void testConcurrentBatches() throws Exception {
        int numBatches = 4;
        CountDownLatch latch = new CountDownLatch(numBatches);
        ConcurrentLinkedQueue<MODWTResult[]> allResults = new ConcurrentLinkedQueue<>();
        
        for (int b = 0; b < numBatches; b++) {
            final int batchId = b;
            new Thread(() -> {
                try {
                    double[][] signals = generateTestSignals(3, 512);
                    MODWTResult[] results = transform.forwardBatch(signals);
                    allResults.add(results);
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        assertTrue(latch.await(10, TimeUnit.SECONDS), "All batches should complete");
        assertEquals(numBatches, allResults.size(), "Should have all batch results");
        
        for (MODWTResult[] batchResults : allResults) {
            assertEquals(3, batchResults.length, "Each batch should have 3 results");
        }
    }
    
    @Test
    @DisplayName("Exception handling in batch processing")
    void testExceptionHandling() {
        // Create invalid signal (null)
        double[][] signals = new double[3][];
        signals[0] = generateTestSignal(512);
        signals[1] = null; // This will cause an exception
        signals[2] = generateTestSignal(512);
        
        assertThrows(StructuredParallelTransform.ComputationException.class, () -> {
            transform.forwardBatch(signals);
        });
    }
    
    @Test
    @DisplayName("Different boundary modes produce different results")
    void testDifferentBoundaryModes() {
        double[] signal = generateTestSignal(512);
        
        var periodicTransform = new StructuredParallelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, config);
        var zeroTransform = new StructuredParallelTransform(
            Daubechies.DB4, BoundaryMode.ZERO_PADDING, config);
        
        MODWTResult periodicResult = periodicTransform.forwardAsync(signal).join();
        MODWTResult zeroResult = zeroTransform.forwardAsync(signal).join();
        
        // Results should be different due to boundary handling
        assertFalse(Arrays.equals(periodicResult.approximationCoeffs(), 
                                 zeroResult.approximationCoeffs()),
            "Different boundary modes should produce different results");
    }
    
    @Test
    @DisplayName("Resource cleanup after batch processing")
    void testResourceCleanup() {
        // Process multiple batches to test resource cleanup
        for (int i = 0; i < 10; i++) {
            double[][] signals = generateTestSignals(5, 256);
            MODWTResult[] results = transform.forwardBatch(signals);
            assertNotNull(results, "Batch " + i + " should complete");
        }
        
        // Should not have resource leaks after multiple batches
        // This is verified by successful completion without OutOfMemoryError
    }
    
    @Test
    @DisplayName("Timeout should not affect subsequent operations")
    void testTimeoutRecovery() {
        // Use a very low threshold to force parallelization
        var timeoutConfig = new ParallelConfig.Builder()
            .parallelThreshold(1)  // Force parallel processing
            .build();
        var timeoutTransform = new StructuredParallelTransform(
            Daubechies.DB4, BoundaryMode.PERIODIC, timeoutConfig);
            
        // Create tasks that will definitely take longer than timeout
        // Use many more signals to ensure parallel execution takes significant time
        double[][] largeSignals = generateTestSignals(200, 4096);
        
        // First operation with extremely short timeout - should definitely fail
        assertThrows(StructuredParallelTransform.ComputationException.class, () -> {
            timeoutTransform.forwardBatchWithTimeout(largeSignals, 0); // 0ms timeout
        });
        
        // Subsequent operation should work normally
        double[][] smallSignals = generateTestSignals(2, 256);
        MODWTResult[] results = transform.forwardBatch(smallSignals);
        
        assertEquals(2, results.length, "Should process subsequent batch normally");
    }
    
    @Test
    @DisplayName("Transform processor should handle streaming correctly")
    void testTransformProcessor() throws InterruptedException {
        // Create test signal publisher
        TestPublisher publisher = new TestPublisher();
        
        // Create transform processor
        Flow.Publisher<MODWTResult> processor = transform.transformStream(publisher, 2);
        
        // Subscribe to results
        TestSubscriber subscriber = new TestSubscriber();
        processor.subscribe(subscriber);
        
        // Send test data
        double[] signal1 = generateTestSignal(256);
        double[] signal2 = generateTestSignal(256);
        
        publisher.sendNext(signal1);
        publisher.sendNext(signal2);
        publisher.complete();
        
        // Wait for processing
        assertTrue(subscriber.awaitCompletion(5, TimeUnit.SECONDS), "Should complete processing");
        
        assertEquals(2, subscriber.getResults().size(), "Should receive 2 results");
        assertNull(subscriber.getError(), "Should not have errors");
        assertTrue(subscriber.isCompleted(), "Should be completed");
    }
    
    @Test
    @DisplayName("Transform processor should handle errors correctly")
    void testTransformProcessorError() throws InterruptedException {
        TestPublisher publisher = new TestPublisher();
        Flow.Publisher<MODWTResult> processor = transform.transformStream(publisher, 2);
        
        TestSubscriber subscriber = new TestSubscriber();
        processor.subscribe(subscriber);
        
        // Send an error
        RuntimeException testError = new RuntimeException("Test error");
        publisher.sendError(testError);
        
        // Wait for error handling
        assertTrue(subscriber.awaitCompletion(5, TimeUnit.SECONDS), "Should handle error");
        
        assertNotNull(subscriber.getError(), "Should have error");
        assertEquals(testError, subscriber.getError(), "Should propagate error");
    }
    
    @Test
    @DisplayName("Transform processor should handle cancellation")
    void testTransformProcessorCancellation() throws InterruptedException {
        TestPublisher publisher = new TestPublisher();
        Flow.Publisher<MODWTResult> processor = transform.transformStream(publisher, 2);
        
        TestSubscriber subscriber = new TestSubscriber();
        processor.subscribe(subscriber);
        
        // Start sending data
        double[] signal = generateTestSignal(256);
        publisher.sendNext(signal);
        
        // Cancel subscription
        subscriber.cancel();
        
        // Publisher should receive cancellation
        Thread.sleep(100); // Brief wait for cancellation to propagate
        assertTrue(publisher.isCancelled(), "Publisher should be cancelled");
    }
    
    @Test
    @DisplayName("Transform processor should respect concurrency limits")
    void testTransformProcessorConcurrencyLimits() throws InterruptedException {
        TestPublisher publisher = new TestPublisher();
        int maxConcurrency = 1; // Limit to 1 concurrent transform
        Flow.Publisher<MODWTResult> processor = transform.transformStream(publisher, maxConcurrency);
        
        TestSubscriber subscriber = new TestSubscriber();
        processor.subscribe(subscriber);
        
        // Send multiple signals quickly
        for (int i = 0; i < 5; i++) {
            publisher.sendNext(generateTestSignal(256));
        }
        publisher.complete();
        
        // Should still process all signals despite concurrency limit
        assertTrue(subscriber.awaitCompletion(10, TimeUnit.SECONDS), "Should complete processing");
        assertEquals(5, subscriber.getResults().size(), "Should process all signals");
    }
    
    @Test
    @DisplayName("Transform processor subscription should handle requests")
    void testTransformProcessorSubscription() throws InterruptedException {
        TestPublisher publisher = new TestPublisher();
        Flow.Publisher<MODWTResult> processor = transform.transformStream(publisher, 2);
        
        TestSubscriber subscriber = new TestSubscriber();
        processor.subscribe(subscriber);
        
        // Request specific number of items
        subscriber.request(1);
        publisher.sendNext(generateTestSignal(256));
        
        // Wait briefly for processing
        Thread.sleep(100);
        
        // Should have received the requested item
        assertEquals(1, subscriber.getResults().size(), "Should receive requested item");
        
        // Request more items
        subscriber.request(2);
        publisher.sendNext(generateTestSignal(256));
        publisher.sendNext(generateTestSignal(256));
        publisher.complete();
        
        assertTrue(subscriber.awaitCompletion(5, TimeUnit.SECONDS), "Should complete");
        assertEquals(3, subscriber.getResults().size(), "Should receive all items");
    }
    
    // Helper classes for testing reactive streams
    
    private static class TestPublisher implements Flow.Publisher<double[]> {
        private Flow.Subscriber<? super double[]> subscriber;
        private boolean cancelled = false;
        
        @Override
        public void subscribe(Flow.Subscriber<? super double[]> subscriber) {
            this.subscriber = subscriber;
            subscriber.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    // Simple implementation - no backpressure handling
                }
                
                @Override
                public void cancel() {
                    cancelled = true;
                }
            });
        }
        
        public void sendNext(double[] signal) {
            if (subscriber != null && !cancelled) {
                subscriber.onNext(signal);
            }
        }
        
        public void sendError(Throwable error) {
            if (subscriber != null && !cancelled) {
                subscriber.onError(error);
            }
        }
        
        public void complete() {
            if (subscriber != null && !cancelled) {
                subscriber.onComplete();
            }
        }
        
        public boolean isCancelled() {
            return cancelled;
        }
    }
    
    private static class TestSubscriber implements Flow.Subscriber<MODWTResult> {
        private final ConcurrentLinkedQueue<MODWTResult> results = new ConcurrentLinkedQueue<>();
        private final CountDownLatch completionLatch = new CountDownLatch(1);
        private Flow.Subscription subscription;
        private volatile Throwable error;
        private volatile boolean completed = false;
        
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE); // Request all items by default
        }
        
        @Override
        public void onNext(MODWTResult item) {
            results.add(item);
        }
        
        @Override
        public void onError(Throwable throwable) {
            this.error = throwable;
            completionLatch.countDown();
        }
        
        @Override
        public void onComplete() {
            this.completed = true;
            completionLatch.countDown();
        }
        
        public void request(long n) {
            if (subscription != null) {
                subscription.request(n);
            }
        }
        
        public void cancel() {
            if (subscription != null) {
                subscription.cancel();
            }
        }
        
        public boolean awaitCompletion(long timeout, TimeUnit unit) throws InterruptedException {
            return completionLatch.await(timeout, unit);
        }
        
        public ConcurrentLinkedQueue<MODWTResult> getResults() {
            return results;
        }
        
        public Throwable getError() {
            return error;
        }
        
        public boolean isCompleted() {
            return completed;
        }
    }
    
    // Helper methods
    
    private double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(4 * Math.PI * i / 32.0) +
                       0.1 * Math.random();
        }
        return signal;
    }
    
    private double[][] generateTestSignals(int count, int length) {
        double[][] signals = new double[count][];
        for (int i = 0; i < count; i++) {
            signals[i] = generateTestSignal(length);
        }
        return signals;
    }
    
    private void assertArrayEquals(double[] expected, double[] actual, 
                                  double tolerance, String message) {
        assertEquals(expected.length, actual.length, message + " - length mismatch");
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], actual[i], tolerance,
                message + " - mismatch at index " + i);
        }
    }
}
