package com.morphiqlabs.wavelet.modwt.streaming;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidSignalException;
import com.morphiqlabs.wavelet.exception.InvalidStateException;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.Flow;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for MultiLevelMODWTStreamingTransform to improve coverage from 62% to 70%.
 */
class MultiLevelMODWTStreamingTransformTest {
    
    private Wavelet wavelet;
    private BoundaryMode boundaryMode;
    
    @BeforeEach
    void setUp() {
        wavelet = new Haar();
        boundaryMode = BoundaryMode.PERIODIC;
    }
    
    @Test
    @DisplayName("Should throw exception for null wavelet")
    void testNullWavelet() {
        assertThrows(InvalidArgumentException.class,
            () -> new MultiLevelMODWTStreamingTransform(null, boundaryMode, 128, 3),
            "Should throw exception for null wavelet");
    }
    
    @Test
    @DisplayName("Should throw exception for null boundary mode")
    void testNullBoundaryMode() {
        assertThrows(InvalidArgumentException.class,
            () -> new MultiLevelMODWTStreamingTransform(wavelet, null, 128, 3),
            "Should throw exception for null boundary mode");
    }
    
    @Test
    @DisplayName("Should throw exception for invalid buffer size")
    void testInvalidBufferSize() {
        assertThrows(InvalidArgumentException.class,
            () -> new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, 0, 3),
            "Should throw exception for zero buffer size");
        
        assertThrows(InvalidArgumentException.class,
            () -> new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, -1, 3),
            "Should throw exception for negative buffer size");
    }
    
    @Test
    @DisplayName("Should throw exception for invalid levels")
    void testInvalidLevels() {
        assertThrows(InvalidArgumentException.class,
            () -> new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, 128, 0),
            "Should throw exception for zero levels");
        
        assertThrows(InvalidArgumentException.class,
            () -> new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, 128, -1),
            "Should throw exception for negative levels");
    }
    
    @Test
    @DisplayName("Should process data through process method")
    void testProcessData() throws InterruptedException {
        MultiLevelMODWTStreamingTransform transform = 
            new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, 64, 2);
        
        // Set up subscriber to collect results
        CountDownLatch latch = new CountDownLatch(1);
        List<MODWTResult> results = new CopyOnWriteArrayList<>();
        
        transform.subscribe(new Flow.Subscriber<MODWTResult>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(MODWTResult result) {
                results.add(result);
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not have error: " + throwable);
            }
            
            @Override
            public void onComplete() {
            }
        });
        
        // Process enough data to trigger buffer processing
        double[] data = new double[64];
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.sin(2 * Math.PI * i / 16);
        }
        
        transform.process(data);
        
        // Wait for results
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertFalse(results.isEmpty());
        
        transform.close();
    }
    
    @Test
    @DisplayName("Should throw exception for null or empty data in process")
    void testProcessInvalidData() {
        MultiLevelMODWTStreamingTransform transform = 
            new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, 64, 2);
        
        assertThrows(InvalidSignalException.class,
            () -> transform.process(null),
            "Should throw exception for null data");
        
        assertThrows(InvalidSignalException.class,
            () -> transform.process(new double[0]),
            "Should throw exception for empty data");
        
        transform.close();
    }
    
    @Test
    @DisplayName("Should throw exception when processing after close")
    void testProcessAfterClose() {
        MultiLevelMODWTStreamingTransform transform = 
            new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, 64, 2);
        
        transform.close();
        
        assertThrows(InvalidStateException.class,
            () -> transform.process(new double[]{1.0, 2.0}),
            "Should throw exception when processing after close");
    }
    
    @Test
    @DisplayName("Should process single samples through processSample")
    void testProcessSample() throws InterruptedException {
        MultiLevelMODWTStreamingTransform transform = 
            new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, 32, 2);
        
        // Set up subscriber
        CountDownLatch latch = new CountDownLatch(1);
        List<MODWTResult> results = new CopyOnWriteArrayList<>();
        
        transform.subscribe(new Flow.Subscriber<MODWTResult>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(MODWTResult result) {
                results.add(result);
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not have error: " + throwable);
            }
            
            @Override
            public void onComplete() {
            }
        });
        
        // Process individual samples
        for (int i = 0; i < 32; i++) {
            transform.processSample(Math.sin(2 * Math.PI * i / 8));
        }
        
        // Wait for results
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertFalse(results.isEmpty());
        
        transform.close();
    }
    
    @Test
    @DisplayName("Should throw exception when processSample called after close")
    void testProcessSampleAfterClose() {
        MultiLevelMODWTStreamingTransform transform = 
            new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, 64, 2);
        
        transform.close();
        
        assertThrows(InvalidStateException.class,
            () -> transform.processSample(1.0),
            "Should throw exception when processSample called after close");
    }
    
    @Test
    @DisplayName("Should flush remaining data")
    void testFlush() throws InterruptedException {
        MultiLevelMODWTStreamingTransform transform = 
            new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, 64, 2);
        
        // Set up subscriber
        CountDownLatch latch = new CountDownLatch(1);
        List<MODWTResult> results = new CopyOnWriteArrayList<>();
        
        transform.subscribe(new Flow.Subscriber<MODWTResult>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(MODWTResult result) {
                results.add(result);
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not have error: " + throwable);
            }
            
            @Override
            public void onComplete() {
            }
        });
        
        // Process partial buffer
        double[] partialData = new double[30]; // Less than buffer size
        for (int i = 0; i < partialData.length; i++) {
            partialData[i] = Math.cos(2 * Math.PI * i / 10);
        }
        
        transform.process(partialData);
        
        // Flush to process remaining data
        transform.flush();
        
        // Should have results from flush
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertFalse(results.isEmpty());
        
        transform.close();
    }
    
    @Test
    @DisplayName("Should get statistics")
    void testGetStatistics() {
        MultiLevelMODWTStreamingTransform transform = 
            new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, 64, 2);
        
        // Get initial statistics
        MODWTStreamingTransform.StreamingStatistics stats = transform.getStatistics();
        assertNotNull(stats);
        assertEquals(0, stats.getSamplesProcessed());
        assertEquals(0, stats.getBlocksProcessed());
        
        // Process some data
        double[] data = new double[64];
        for (int i = 0; i < data.length; i++) {
            data[i] = i * 0.1;
        }
        transform.process(data);
        
        // Check updated statistics
        stats = transform.getStatistics();
        assertEquals(64, stats.getSamplesProcessed());
        assertTrue(stats.getBlocksProcessed() > 0);
        
        transform.close();
    }
    
    @Test
    @DisplayName("Should handle multiple decomposition levels")
    void testMultipleLevels() throws InterruptedException {
        // Test with 4 levels
        MultiLevelMODWTStreamingTransform transform = 
            new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, 128, 4);
        
        CountDownLatch latch = new CountDownLatch(4); // Expect results for 4 levels
        List<MODWTResult> results = new CopyOnWriteArrayList<>();
        
        transform.subscribe(new Flow.Subscriber<MODWTResult>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(MODWTResult result) {
                results.add(result);
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not have error: " + throwable);
            }
            
            @Override
            public void onComplete() {
            }
        });
        
        // Process full buffer
        double[] data = new double[128];
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.sin(2 * Math.PI * i / 32) + 
                      0.5 * Math.sin(2 * Math.PI * i / 8);
        }
        
        transform.process(data);
        
        // Should receive results for all levels
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertTrue(results.size() >= 4);
        
        transform.close();
    }
    
    @Test
    @DisplayName("Should handle different wavelets")
    void testDifferentWavelets() {
        // Test with DB4 wavelet
        Wavelet db4 = WaveletRegistry.getWavelet(WaveletName.DB4);
        MultiLevelMODWTStreamingTransform transform = 
            new MultiLevelMODWTStreamingTransform(db4, boundaryMode, 64, 3);
        
        assertNotNull(transform);
        
        // Process some data
        double[] data = new double[64];
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.random();
        }
        
        assertDoesNotThrow(() -> transform.process(data));
        
        transform.close();
    }
    
    @Test
    @DisplayName("Should handle different boundary modes")
    void testDifferentBoundaryModes() {
        // Test with SYMMETRIC boundary mode
        MultiLevelMODWTStreamingTransform transform = 
            new MultiLevelMODWTStreamingTransform(wavelet, BoundaryMode.SYMMETRIC, 64, 2);
        
        assertNotNull(transform);
        
        double[] data = new double[64];
        for (int i = 0; i < data.length; i++) {
            data[i] = i * 0.01;
        }
        
        assertDoesNotThrow(() -> transform.process(data));
        
        transform.close();
    }
    
    @Test
    @DisplayName("Should handle small buffer sizes")
    void testSmallBufferSize() throws InterruptedException {
        // Test with small buffer
        MultiLevelMODWTStreamingTransform transform = 
            new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, 8, 2);
        
        CountDownLatch latch = new CountDownLatch(1);
        List<MODWTResult> results = new ArrayList<>();
        
        transform.subscribe(new Flow.Subscriber<MODWTResult>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }
            
            @Override
            public void onNext(MODWTResult result) {
                results.add(result);
                latch.countDown();
            }
            
            @Override
            public void onError(Throwable throwable) {
                fail("Should not have error: " + throwable);
            }
            
            @Override
            public void onComplete() {
            }
        });
        
        // Process exactly buffer size
        double[] data = new double[8];
        for (int i = 0; i < data.length; i++) {
            data[i] = Math.sin(Math.PI * i / 4);
        }
        
        transform.process(data);
        
        assertTrue(latch.await(1, TimeUnit.SECONDS));
        assertFalse(results.isEmpty());
        
        transform.close();
    }
}
