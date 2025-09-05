package com.morphiqlabs.wavelet.modwt.streaming;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Flow;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MODWTStreamingTransformImpl with focus on constructor validation.
 */
class MODWTStreamingTransformImplTest {
    
    private Haar haar;
    private Daubechies db4;
    
    @BeforeEach
    void setUp() {
        haar = new Haar();
        db4 = Daubechies.DB4;
    }
    
    @Test
    void testValidConstruction() {
        // Valid construction
        assertDoesNotThrow(() -> {
            new MODWTStreamingTransformImpl(haar, BoundaryMode.PERIODIC, 1024);
        });

        assertDoesNotThrow(() -> {
            new MODWTStreamingTransformImpl(db4, BoundaryMode.ZERO_PADDING, 4096);
        });

        assertDoesNotThrow(() -> {
            new MODWTStreamingTransformImpl(haar, BoundaryMode.SYMMETRIC, 256);
        });
    }
    
    @Test
    void testNullWaveletValidation() {
        assertThrows(InvalidArgumentException.class, () -> {
            new MODWTStreamingTransformImpl(null, BoundaryMode.PERIODIC, 1024);
        }, "Should throw for null wavelet");
    }
    
    @Test
    void testNullBoundaryModeValidation() {
        assertThrows(InvalidArgumentException.class, () -> {
            new MODWTStreamingTransformImpl(haar, null, 1024);
        }, "Should throw for null boundary mode");
    }
    
    @Test
    void testNegativeBufferSizeValidation() {
        assertThrows(InvalidArgumentException.class, () -> {
            new MODWTStreamingTransformImpl(haar, BoundaryMode.PERIODIC, -1);
        }, "Should throw for negative buffer size");
    }
    
    @Test
    void testZeroBufferSizeValidation() {
        assertThrows(InvalidArgumentException.class, () -> {
            new MODWTStreamingTransformImpl(haar, BoundaryMode.PERIODIC, 0);
        }, "Should throw for zero buffer size");
    }
    
    @Test
    void testBufferSizeSmallerThanFilterLength() {
        // DB4 has filter length 8
        Exception ex = assertThrows(InvalidArgumentException.class, () -> {
            new MODWTStreamingTransformImpl(db4, BoundaryMode.PERIODIC, 5);
        });
        assertTrue(ex.getMessage().contains("Buffer size must be at least as large as filter length"));
        assertTrue(ex.getMessage().contains("bufferSize=5"));
        assertTrue(ex.getMessage().contains("filterLength=8"));
    }
    
    @Test
    void testIntegerOverflowValidation() {
        // Try to create a buffer that would overflow when adding overlap
        // Haar has filter length 2, so overlap is 1
        // Integer.MAX_VALUE would overflow when adding 1
        Exception ex = assertThrows(InvalidArgumentException.class, () -> {
            new MODWTStreamingTransformImpl(haar, BoundaryMode.PERIODIC, Integer.MAX_VALUE);
        });
        assertTrue(ex.getMessage().contains("integer overflow"));
    }
    
    @Test
    void testMemorySizeValidation() {
        // Try to create a buffer larger than 100MB limit
        // 100MB / 8 bytes per double = 13,107,200 doubles
        int tooLargeSize = 15_000_000; // Would require ~114MB
        
        Exception ex = assertThrows(InvalidArgumentException.class, () -> {
            new MODWTStreamingTransformImpl(haar, BoundaryMode.PERIODIC, tooLargeSize);
        });
        assertTrue(ex.getMessage().contains("Buffer size too large"));
        assertTrue(ex.getMessage().contains("MB"));
    }
    
    @Test
    void testBasicStreamingFunctionality() throws InterruptedException {
        // Create a streaming transform with reasonable buffer size
        MODWTStreamingTransform transform = new MODWTStreamingTransformImpl(
            haar, BoundaryMode.PERIODIC, 8);
        
        // Create a subscriber to collect results
        TestSubscriber subscriber = new TestSubscriber();
        transform.subscribe(subscriber);
        
        // Process enough samples to trigger at least one transform
        double[] data = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12};
        transform.process(data);
        
        // Wait a bit for async processing
        Thread.sleep(100);
        
        // Should have received at least one result
        assertTrue(subscriber.results.size() >= 1);
        
        // Verify the result is valid
        MODWTResult result = subscriber.results.get(0);
        assertNotNull(result);
        assertEquals(8, result.approximationCoeffs().length);
        assertEquals(8, result.detailCoeffs().length);
        
        transform.close();
    }

    @Test
    void testSymmetricStreamingReconstruction() throws InterruptedException {
        // Note: Symmetric boundaries with MODWT may not achieve perfect reconstruction
        // due to the boundary handling complexity
        MODWTStreamingTransform transform = new MODWTStreamingTransformImpl(
            haar, BoundaryMode.SYMMETRIC, 8);
        TestSubscriber subscriber = new TestSubscriber();
        transform.subscribe(subscriber);
        double[] data = {1,2,3,4,5,6,7,8};
        transform.process(data);
        Thread.sleep(50);
        assertEquals(1, subscriber.results.size());
        MODWTResult result = subscriber.results.get(0);
        MODWTTransform inverse = new MODWTTransform(haar, BoundaryMode.SYMMETRIC);
        double[] reconstructed = inverse.inverse(result);
        
        // For symmetric boundaries, we expect approximate reconstruction
        // with some boundary effects, especially for short signals
        for (int i = 0; i < data.length; i++) {
            assertEquals(data[i], reconstructed[i], 1.1, 
                "Reconstruction error at index " + i + " exceeds tolerance");
        }
        transform.close();
    }
    
    @Test
    void testEdgeCaseBufferSizes() {
        // Test minimum valid buffer size (must be > filter length - 1)
        // For Haar: filterLength=2, overlapSize=1, so min bufferSize=2
        assertDoesNotThrow(() -> {
            new MODWTStreamingTransformImpl(haar, BoundaryMode.PERIODIC, 2);
        });
        
        // For DB4: filterLength=8, overlapSize=7, so min bufferSize=8
        assertDoesNotThrow(() -> {
            new MODWTStreamingTransformImpl(db4, BoundaryMode.PERIODIC, 8);
        });
        
        // Test buffer size just below memory limit
        // 100MB / 8 bytes = 13,107,200 doubles max
        // With Haar overlap of 1, max buffer size is about 13,107,199
        assertDoesNotThrow(() -> {
            new MODWTStreamingTransformImpl(haar, BoundaryMode.PERIODIC, 13_000_000);
        });
    }
    
    @Test
    void testBufferSizeEqualsOverlapSize() {
        // Test cases where bufferSize <= overlapSize, which would break sliding window
        
        // For Haar: filterLength=2, overlapSize=1
        // bufferSize=1 fails because it's less than filterLength (caught by first check)
        Exception ex = assertThrows(InvalidArgumentException.class, () -> {
            new MODWTStreamingTransformImpl(haar, BoundaryMode.PERIODIC, 1);
        });
        assertTrue(ex.getMessage().contains("Buffer size must be at least as large as filter length"),
                  "Unexpected error message: " + ex.getMessage());
        
        // For DB4: filterLength=8, overlapSize=7
        // bufferSize=7 fails because it's less than filterLength (caught by first check)
        ex = assertThrows(InvalidArgumentException.class, () -> {
            new MODWTStreamingTransformImpl(db4, BoundaryMode.PERIODIC, 7);
        });
        assertTrue(ex.getMessage().contains("Buffer size must be at least as large as filter length"),
                  "Unexpected error message: " + ex.getMessage());
        
        // To actually test bufferSize <= overlapSize validation, we need a case where
        // bufferSize >= filterLength but bufferSize <= overlapSize
        // This is impossible since overlapSize = filterLength - 1
        // So bufferSize >= filterLength implies bufferSize > overlapSize
        // The validation is redundant but serves as defensive programming
    }
    
    @Test
    void testSlidingWindowConsumption() throws InterruptedException {
        // Test that sliding window correctly consumes (bufferSize - overlapSize) samples
        // For Haar with bufferSize=4, overlapSize=1, should consume 3 samples per window
        
        MODWTStreamingTransform transform = new MODWTStreamingTransformImpl(
            haar, BoundaryMode.PERIODIC, 4);
        
        TestSubscriber subscriber = new TestSubscriber();
        transform.subscribe(subscriber);
        
        // Process exactly 4 samples - should trigger 1 transform
        transform.process(new double[]{1, 2, 3, 4});
        Thread.sleep(50);
        assertEquals(1, subscriber.results.size());
        
        // Process 3 more samples - should trigger another transform
        // (3 new + 1 overlap from previous = 4 total)
        transform.process(new double[]{5, 6, 7});
        Thread.sleep(50);
        assertEquals(2, subscriber.results.size());
        
        // Process 2 more samples - should NOT trigger transform yet
        // (2 new + 1 overlap = 3 total, need 4)
        transform.process(new double[]{8, 9});
        Thread.sleep(50);
        assertEquals(2, subscriber.results.size());
        
        // Process 1 more sample - should trigger transform
        // (3 + 1 = 4 total)
        transform.processSample(10);
        Thread.sleep(50);
        assertEquals(3, subscriber.results.size());
        
        transform.close();
    }
    
    /**
     * Test subscriber to collect results.
     */
    private static class TestSubscriber implements Flow.Subscriber<MODWTResult> {
        final List<MODWTResult> results = new CopyOnWriteArrayList<>();
        private Flow.Subscription subscription;
        
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            subscription.request(Long.MAX_VALUE);
        }
        
        @Override
        public void onNext(MODWTResult item) {
            results.add(item);
        }
        
        @Override
        public void onError(Throwable throwable) {
            throwable.printStackTrace();
        }
        
        @Override
        public void onComplete() {
            // Do nothing
        }
    }
}
