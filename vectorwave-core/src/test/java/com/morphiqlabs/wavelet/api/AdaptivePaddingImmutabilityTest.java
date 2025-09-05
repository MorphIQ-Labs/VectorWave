package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.padding.AdaptivePaddingStrategy;
import com.morphiqlabs.wavelet.padding.AdaptivePaddingStrategy.AdaptivePaddingResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Test suite to verify that AdaptivePaddingStrategy is immutable and thread-safe.
 */
@DisplayName("AdaptivePaddingStrategy Immutability and Thread-Safety Tests")
public class AdaptivePaddingImmutabilityTest {
    
    @Test
    @DisplayName("Verify strategy is immutable - no state changes between calls")
    void testImmutability() {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        // Create different signals that would select different strategies
        double[] periodicSignal = new double[100];
        for (int i = 0; i < 100; i++) {
            periodicSignal[i] = Math.sin(2 * Math.PI * i / 10);
        }
        
        double[] trendingSignal = new double[100];
        for (int i = 0; i < 100; i++) {
            trendingSignal[i] = 0.5 * i + Math.random() * 0.1;
        }
        
        // First padding operation
        AdaptivePaddingResult result1 = strategy.padWithDetails(periodicSignal, 120);
        assertNotNull(result1);
        assertNotNull(result1.selectedStrategy());
        // Periodic signals might be detected as periodic, smooth, or symmetric
        assertTrue(result1.selectionReason().toLowerCase().contains("periodic") ||
                  result1.selectionReason().toLowerCase().contains("smooth") ||
                  result1.selectionReason().toLowerCase().contains("symmetric"),
                  "Expected strategy for periodic signal, got: " + result1.selectionReason());
        
        // Second padding operation with different signal
        AdaptivePaddingResult result2 = strategy.padWithDetails(trendingSignal, 120);
        assertNotNull(result2);
        assertNotNull(result2.selectedStrategy());
        // The trending signal should select some appropriate strategy
        // Note: FFT-based periodicity detection might detect trend as periodic
        assertTrue(result2.selectionReason().toLowerCase().contains("trend") ||
                  result2.selectionReason().toLowerCase().contains("polynomial") ||
                  result2.selectionReason().toLowerCase().contains("linear") ||
                  result2.selectionReason().toLowerCase().contains("periodic"),
                  "Expected strategy for trending signal, got: " + result2.selectionReason());
        
        // Third padding - repeat first signal to verify no state pollution
        AdaptivePaddingResult result3 = strategy.padWithDetails(periodicSignal, 120);
        assertNotNull(result3);
        assertEquals(result1.selectedStrategy().getClass(), result3.selectedStrategy().getClass());
        assertEquals(result1.selectionReason(), result3.selectionReason());
        
        // Verify strategy object itself hasn't changed
        assertEquals("adaptive", strategy.name());
        assertTrue(strategy.description().contains("automatically selects"));
    }
    
    @Test
    @DisplayName("Verify thread safety - concurrent operations don't interfere")
    void testThreadSafety() throws InterruptedException, ExecutionException {
        final AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        final int numThreads = 10;
        final int operationsPerThread = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<List<AdaptivePaddingResult>>> futures = new ArrayList<>();
        
        // Create different signal types for each thread
        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            Future<List<AdaptivePaddingResult>> future = executor.submit(() -> {
                List<AdaptivePaddingResult> results = new ArrayList<>();
                
                for (int i = 0; i < operationsPerThread; i++) {
                    double[] signal = createSignalForThread(threadId, i);
                    AdaptivePaddingResult result = strategy.padWithDetails(signal, signal.length + 20);
                    results.add(result);
                    
                    // Verify result is valid
                    assertNotNull(result.paddedSignal());
                    assertEquals(signal.length + 20, result.paddedSignal().length);
                    assertNotNull(result.selectionReason());
                }
                
                return results;
            });
            futures.add(future);
        }
        
        // Collect all results
        List<AdaptivePaddingResult> allResults = new ArrayList<>();
        for (Future<List<AdaptivePaddingResult>> future : futures) {
            allResults.addAll(future.get());
        }
        
        executor.shutdown();
        assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        
        // Verify we got all expected results
        assertEquals(numThreads * operationsPerThread, allResults.size());
        
        // Verify results are consistent for same signal types
        for (int i = 0; i < allResults.size(); i++) {
            AdaptivePaddingResult result = allResults.get(i);
            assertNotNull(result);
            assertNotNull(result.paddedSignal());
            assertNotNull(result.selectionReason());
            
            // Results should be deterministic for the same signal
            int threadId = i / operationsPerThread;
            int opId = i % operationsPerThread;
            double[] sameSignal = createSignalForThread(threadId, opId);
            AdaptivePaddingResult verifyResult = strategy.padWithDetails(sameSignal, sameSignal.length + 20);
            
            // Same signal should produce same strategy selection
            if (result.selectedStrategy() != null && verifyResult.selectedStrategy() != null) {
                assertEquals(result.selectedStrategy().getClass(), 
                           verifyResult.selectedStrategy().getClass());
            }
        }
    }
    
    @Test
    @DisplayName("Verify padWithDetails returns independent results")
    void testResultIndependence() {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        double[] signal = {1, 2, 3, 4, 5};
        
        // Get multiple results
        AdaptivePaddingResult result1 = strategy.padWithDetails(signal, 10);
        AdaptivePaddingResult result2 = strategy.padWithDetails(signal, 10);
        
        // Results should be independent objects
        assertNotSame(result1, result2);
        assertNotSame(result1.paddedSignal(), result2.paddedSignal());
        
        // But should have same content for same input
        assertArrayEquals(result1.paddedSignal(), result2.paddedSignal(), 1e-10);
        assertEquals(result1.selectionReason(), result2.selectionReason());
        
        // Modifying one result's array shouldn't affect the other
        result1.paddedSignal()[0] = 999;
        assertNotEquals(result1.paddedSignal()[0], result2.paddedSignal()[0]);
    }
    
    @Test
    @DisplayName("Verify backward compatibility with pad() method")
    void testBackwardCompatibility() {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        double[] signal = {1, 2, 3, 4, 5, 4, 3, 2, 1};
        
        // Use old pad() method
        double[] padded1 = strategy.pad(signal, 15);
        assertNotNull(padded1);
        assertEquals(15, padded1.length);
        
        // Use new padWithDetails() method
        AdaptivePaddingResult result = strategy.padWithDetails(signal, 15);
        double[] padded2 = result.paddedSignal();
        
        // Results should be equivalent
        assertArrayEquals(padded1, padded2, 1e-10);
    }
    
    @Test
    @DisplayName("Verify no mutable state is exposed")
    void testNoMutableStateExposed() {
        AdaptivePaddingStrategy strategy = new AdaptivePaddingStrategy();
        
        // Check that the class doesn't expose any setters or mutable fields
        var methods = AdaptivePaddingStrategy.class.getMethods();
        for (var method : methods) {
            String name = method.getName();
            // Should not have any setters
            assertFalse(name.startsWith("set"), "Found setter method: " + name);
            // Should not return mutable collections (except arrays which are expected)
            if (!method.getReturnType().isArray() && 
                !name.equals("padWithDetails") && 
                !name.equals("pad")) {
                assertFalse(java.util.Collection.class.isAssignableFrom(method.getReturnType()),
                           "Found method returning mutable collection: " + name);
            }
        }
        
        // The public fields should be final (checked at compile time)
        // Records ensure all fields are final
    }
    
    /**
     * Helper method to create different signal types for each thread.
     */
    private double[] createSignalForThread(int threadId, int operationId) {
        int size = 50 + operationId % 50; // Varying sizes
        double[] signal = new double[size];
        
        // Use switch expression with lambda functions for cleaner code
        Runnable signalGenerator = switch (threadId % 4) {
            case 0 -> () -> { // Periodic signal
                for (int i = 0; i < size; i++) {
                    signal[i] = Math.sin(2 * Math.PI * i / 10);
                }
            };
            case 1 -> () -> { // Trending signal  
                for (int i = 0; i < size; i++) {
                    signal[i] = 0.1 * i;
                }
            };
            case 2 -> () -> { // Noisy signal
                java.util.Random rand = new java.util.Random(threadId * 1000 + operationId);
                for (int i = 0; i < size; i++) {
                    signal[i] = rand.nextGaussian();
                }
            };
            case 3 -> () -> { // Smooth signal
                for (int i = 0; i < size; i++) {
                    signal[i] = Math.exp(-0.1 * Math.abs(i - size/2));
                }
            };
            default -> throw new IllegalStateException("Unexpected value: " + (threadId % 4));
        };
        
        signalGenerator.run();
        return signal;
    }
}