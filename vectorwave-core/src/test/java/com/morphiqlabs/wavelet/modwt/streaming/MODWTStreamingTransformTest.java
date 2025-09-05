package com.morphiqlabs.wavelet.modwt.streaming;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidSignalException;
import com.morphiqlabs.wavelet.exception.InvalidStateException;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test coverage for MODWTStreamingTransform interface and factory methods.
 */
class MODWTStreamingTransformTest {
    
    private MODWTStreamingTransform transform;
    
    @BeforeEach
    void setUp() {
        transform = null;
    }
    
    @AfterEach
    void tearDown() {
        if (transform != null && !transform.isClosed()) {
            transform.close();
        }
    }
    
    @Test
    void testCreateWithDefaultBufferSize() {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC);
        assertNotNull(transform);
        assertFalse(transform.isClosed());
    }
    
    @Test
    void testCreateWithCustomBufferSize() {
        transform = MODWTStreamingTransform.create(Daubechies.DB4, BoundaryMode.SYMMETRIC, 512);
        assertNotNull(transform);
        assertFalse(transform.isClosed());
    }
    
    @Test
    void testCreateMultiLevel() {
        transform = MODWTStreamingTransform.createMultiLevel(
            Haar.INSTANCE, BoundaryMode.PERIODIC, 256, 3
        );
        assertNotNull(transform);
        assertFalse(transform.isClosed());
    }
    
    @Test
    void testCreateWithInvalidBufferSize() {
        assertThrows(InvalidArgumentException.class, () -> 
            MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC, 0)
        );
        
        assertThrows(InvalidArgumentException.class, () -> 
            MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC, -1)
        );
    }
    
    @Test
    void testProcessDataChunk() throws InterruptedException {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC, 4);
        
        // Setup subscriber to collect results
        TestSubscriber subscriber = new TestSubscriber();
        transform.subscribe(subscriber);
        
        // Process data
        double[] data = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        transform.process(data);
        
        // Wait for results
        assertTrue(subscriber.latch.await(1, TimeUnit.SECONDS));
        assertFalse(subscriber.results.isEmpty());
        
        // Verify results
        for (MODWTResult result : subscriber.results) {
            assertNotNull(result);
            assertNotNull(result.approximationCoeffs());
            assertNotNull(result.detailCoeffs());
            assertEquals(4, result.getSignalLength()); // Buffer size
        }
    }
    
    @Test
    void testProcessSingleSample() throws InterruptedException {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC, 2);
        
        TestSubscriber subscriber = new TestSubscriber();
        transform.subscribe(subscriber);
        
        // Process samples one by one
        transform.processSample(1.0);
        transform.processSample(2.0);
        transform.processSample(3.0);
        transform.processSample(4.0);
        
        // Should have triggered processing when buffer filled
        assertTrue(subscriber.latch.await(1, TimeUnit.SECONDS));
        assertFalse(subscriber.results.isEmpty());
    }
    
    @Test
    void testProcessNullData() {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC);
        assertThrows(InvalidSignalException.class, () -> transform.process(null));
    }
    
    @Test
    void testProcessEmptyData() {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC);
        assertThrows(InvalidSignalException.class, () -> transform.process(new double[0]));
    }
    
    @Test
    void testFlush() throws InterruptedException {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC, 4);
        
        TestSubscriber subscriber = new TestSubscriber();
        transform.subscribe(subscriber);
        
        // Process partial data (less than buffer size)
        double[] data = {1.0, 2.0};
        transform.process(data);
        
        // Should not have results yet
        assertEquals(0, subscriber.results.size());
        
        // Flush to force processing
        transform.flush();
        
        // Should have results now
        assertTrue(subscriber.latch.await(1, TimeUnit.SECONDS));
        assertFalse(subscriber.results.isEmpty());
    }
    
    @Test
    void testReset() {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC, 4);
        
        // Process some data
        double[] data = {1.0, 2.0, 3.0};
        transform.process(data);
        
        // Check buffer has data
        assertEquals(3, transform.getBufferLevel());
        
        // Reset
        transform.reset();
        
        // Buffer should be empty
        assertEquals(0, transform.getBufferLevel());
        
        // Statistics should be reset
        MODWTStreamingTransform.StreamingStatistics stats = transform.getStatistics();
        assertEquals(0, stats.getSamplesProcessed());
        assertEquals(0, stats.getBlocksProcessed());
    }
    
    @Test
    void testGetStatistics() {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC, 4);
        
        // Process data
        double[] data = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        transform.process(data);
        
        // Check statistics
        MODWTStreamingTransform.StreamingStatistics stats = transform.getStatistics();
        assertNotNull(stats);
        assertTrue(stats.getSamplesProcessed() > 0);
        assertTrue(stats.getBlocksProcessed() > 0);
        assertTrue(stats.getAverageProcessingTimeNanos() >= 0);
        assertTrue(stats.getThroughputSamplesPerSecond() > 0);
    }
    
    @Test
    void testStatisticsReset() {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC, 4);
        
        // Process data
        double[] data = {1.0, 2.0, 3.0, 4.0};
        transform.process(data);
        
        MODWTStreamingTransform.StreamingStatistics stats = transform.getStatistics();
        assertTrue(stats.getSamplesProcessed() > 0);
        
        // Reset statistics
        stats.reset();
        assertEquals(0, stats.getSamplesProcessed());
        assertEquals(0, stats.getBlocksProcessed());
    }
    
    @Test
    void testClose() {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC);
        assertFalse(transform.isClosed());
        
        transform.close();
        assertTrue(transform.isClosed());
        
        // Operations after close should throw
        assertThrows(InvalidStateException.class, () -> transform.process(new double[]{1.0}));
        assertThrows(InvalidStateException.class, () -> transform.processSample(1.0));
        assertThrows(InvalidStateException.class, () -> transform.flush());
        assertThrows(InvalidStateException.class, () -> transform.reset());
    }
    
    @Test
    void testMultipleSubscribers() throws InterruptedException {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC, 4);
        
        TestSubscriber subscriber1 = new TestSubscriber();
        TestSubscriber subscriber2 = new TestSubscriber();
        
        transform.subscribe(subscriber1);
        transform.subscribe(subscriber2);
        
        // Process data
        double[] data = {1.0, 2.0, 3.0, 4.0};
        transform.process(data);
        
        // Both subscribers should receive results
        assertTrue(subscriber1.latch.await(1, TimeUnit.SECONDS));
        assertTrue(subscriber2.latch.await(1, TimeUnit.SECONDS));
        
        assertFalse(subscriber1.results.isEmpty());
        assertFalse(subscriber2.results.isEmpty());
    }
    
    @Test
    void testBackpressure() throws InterruptedException {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC, 2);
        
        SlowSubscriber subscriber = new SlowSubscriber();
        transform.subscribe(subscriber);
        
        // Process multiple chunks
        for (int i = 0; i < 5; i++) {
            double[] data = {i * 2.0, i * 2.0 + 1.0};
            transform.process(data);
        }
        
        // Should handle backpressure gracefully
        assertTrue(subscriber.latch.await(5, TimeUnit.SECONDS));
        assertTrue(subscriber.processedCount.get() > 0);
    }
    
    @Test
    void testProcessingTimeMetrics() {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC, 4);
        
        // Process multiple blocks
        for (int i = 0; i < 10; i++) {
            double[] data = {i, i+1, i+2, i+3};
            transform.process(data);
        }
        
        MODWTStreamingTransform.StreamingStatistics stats = transform.getStatistics();
        
        // Check timing metrics
        assertTrue(stats.getMinProcessingTimeNanos() > 0);
        assertTrue(stats.getMaxProcessingTimeNanos() >= stats.getMinProcessingTimeNanos());
        assertTrue(stats.getAverageProcessingTimeNanos() >= stats.getMinProcessingTimeNanos());
        assertTrue(stats.getAverageProcessingTimeNanos() <= stats.getMaxProcessingTimeNanos());
    }
    
    @Test
    void testBufferLevel() {
        transform = MODWTStreamingTransform.create(Haar.INSTANCE, BoundaryMode.PERIODIC, 10);
        
        assertEquals(0, transform.getBufferLevel());
        
        transform.processSample(1.0);
        assertEquals(1, transform.getBufferLevel());
        
        transform.processSample(2.0);
        assertEquals(2, transform.getBufferLevel());
        
        double[] chunk = {3.0, 4.0, 5.0};
        transform.process(chunk);
        assertEquals(5, transform.getBufferLevel());
    }
    
    @Test
    void testMultiLevelStreaming() throws InterruptedException {
        transform = MODWTStreamingTransform.createMultiLevel(
            Haar.INSTANCE, BoundaryMode.PERIODIC, 8, 3
        );
        
        TestSubscriber subscriber = new TestSubscriber();
        transform.subscribe(subscriber);
        
        // Process data
        double[] data = new double[16];
        for (int i = 0; i < 16; i++) {
            data[i] = Math.sin(2 * Math.PI * i / 8);
        }
        transform.process(data);
        
        // Should receive multi-level results
        assertTrue(subscriber.latch.await(1, TimeUnit.SECONDS));
        assertFalse(subscriber.results.isEmpty());
    }
    
    /**
     * Test subscriber for collecting results.
     */
    private static class TestSubscriber implements Flow.Subscriber<MODWTResult> {
        final List<MODWTResult> results = new CopyOnWriteArrayList<>();
        final CountDownLatch latch = new CountDownLatch(1);
        private Flow.Subscription subscription;
        
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(MODWTResult item) {
            results.add(item);
            latch.countDown();
        }
        
        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
            latch.countDown();
        }
        
        @Override
        public void onComplete() {
            latch.countDown();
        }
    }
    
    /**
     * Slow subscriber for testing backpressure.
     */
    private static class SlowSubscriber implements Flow.Subscriber<MODWTResult> {
        final AtomicInteger processedCount = new AtomicInteger(0);
        final CountDownLatch latch = new CountDownLatch(1);
        private Flow.Subscription subscription;
        
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(1);
        }
        
        @Override
        public void onNext(MODWTResult item) {
            try {
                Thread.sleep(100); // Simulate slow processing
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            processedCount.incrementAndGet();
            subscription.request(1);
            if (processedCount.get() >= 3) {
                latch.countDown();
            }
        }
        
        @Override
        public void onError(Throwable throwable) {
            latch.countDown();
        }
        
        @Override
        public void onComplete() {
            latch.countDown();
        }
    }
}
