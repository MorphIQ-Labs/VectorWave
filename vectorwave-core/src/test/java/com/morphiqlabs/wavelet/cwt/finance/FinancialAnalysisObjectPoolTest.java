package com.morphiqlabs.wavelet.cwt.finance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for FinancialAnalysisObjectPool and its inner classes.
 */
class FinancialAnalysisObjectPoolTest {
    
    private FinancialAnalysisObjectPool pool;
    
    @BeforeEach
    void setUp() {
        pool = new FinancialAnalysisObjectPool();
    }
    
    @Test
    @DisplayName("Should test ArrayHolder functionality")
    void testArrayHolder() {
        // Get an array from the pool
        FinancialAnalysisObjectPool.ArrayHolder holder = pool.borrowArray(100);
        assertNotNull(holder);
        assertNotNull(holder.array);
        assertTrue(holder.array.length >= 100, "Array should be at least requested size");
        
        // Test array operations
        holder.array[0] = 10.0;
        holder.array[99] = 20.0;
        assertEquals(10.0, holder.array[0]);
        assertEquals(20.0, holder.array[99]);
        
        // Test clear method
        holder.clear();
        // Note: clear() is currently empty, so values remain
        assertEquals(10.0, holder.array[0]);
        
        // Test AutoCloseable
        assertDoesNotThrow(() -> holder.close());
        
        // Get another array of same size (may or may not reuse the same holder)
        FinancialAnalysisObjectPool.ArrayHolder holder2 = pool.borrowArray(100);
        assertNotNull(holder2);
        assertTrue(holder2.array.length >= 100);
        
        // Test with try-with-resources
        try (FinancialAnalysisObjectPool.ArrayHolder autoHolder = pool.borrowArray(50)) {
            assertNotNull(autoHolder.array);
            assertTrue(autoHolder.array.length >= 50, "Array should be at least requested size");
        }
    }
    
    @Test
    @DisplayName("Should test ArrayHolder with different sizes")
    void testArrayHolderDifferentSizes() {
        // Borrow arrays of different sizes
        FinancialAnalysisObjectPool.ArrayHolder holder1 = pool.borrowArray(50);
        FinancialAnalysisObjectPool.ArrayHolder holder2 = pool.borrowArray(100);
        FinancialAnalysisObjectPool.ArrayHolder holder3 = pool.borrowArray(200);
        
        assertNotSame(holder1, holder2);
        assertNotSame(holder2, holder3);
        assertNotSame(holder1, holder3);
        
        assertTrue(holder1.array.length >= 50, "Array should be at least 50");
        assertTrue(holder2.array.length >= 100, "Array should be at least 100");
        assertTrue(holder3.array.length >= 200, "Array should be at least 200");
        
        // Return them
        holder1.close();
        holder2.close();
        holder3.close();
        
        // Borrow again - pool may reuse holders
        FinancialAnalysisObjectPool.ArrayHolder reused1 = pool.borrowArray(50);
        FinancialAnalysisObjectPool.ArrayHolder reused2 = pool.borrowArray(100);
        
        assertNotNull(reused1);
        assertNotNull(reused2);
        assertTrue(reused1.array.length >= 50);
        assertTrue(reused2.array.length >= 100);
    }
    
    @Test
    @DisplayName("Should test ArrayHolder without pool")
    void testArrayHolderWithoutPool() {
        // Create ArrayHolder directly
        FinancialAnalysisObjectPool.ArrayHolder directHolder = 
            new FinancialAnalysisObjectPool.ArrayHolder(75);
        
        assertNotNull(directHolder.array);
        assertEquals(75, directHolder.array.length);
        
        // Close without pool should not throw
        assertDoesNotThrow(() -> directHolder.close());
        
        // Set pool after creation
        directHolder.setPool(pool);
        assertDoesNotThrow(() -> directHolder.close());
    }
    
    @Test
    @DisplayName("Should test TradingSignalBuilder")
    void testTradingSignalBuilder() {
        FinancialAnalysisObjectPool.TradingSignalBuilder builder = 
            new FinancialAnalysisObjectPool.TradingSignalBuilder();
        
        // Initially empty
        assertEquals(0, builder.size());
        
        // Add some signals
        builder.addSignal(10, FinancialWaveletAnalyzer.SignalType.BUY, 0.8, "Strong trend");
        builder.addSignal(20, FinancialWaveletAnalyzer.SignalType.SELL, 0.6, "Reversal");
        builder.addSignal(30, FinancialWaveletAnalyzer.SignalType.HOLD, 0.5, "Uncertain");
        
        assertEquals(3, builder.size());
        
        // Build the signals
        var signals = builder.build();
        assertNotNull(signals);
        assertEquals(3, signals.size());
        
        // Verify first signal
        var signal1 = signals.get(0);
        assertEquals(10, signal1.timeIndex());
        assertEquals(FinancialWaveletAnalyzer.SignalType.BUY, signal1.type());
        assertEquals(0.8, signal1.confidence());
        assertEquals("Strong trend", signal1.rationale());
        
        // Verify second signal
        var signal2 = signals.get(1);
        assertEquals(20, signal2.timeIndex());
        assertEquals(FinancialWaveletAnalyzer.SignalType.SELL, signal2.type());
        assertEquals(0.6, signal2.confidence());
        assertEquals("Reversal", signal2.rationale());
        
        // Clear and reuse
        builder.clear();
        assertEquals(0, builder.size());
        
        builder.addSignal(40, FinancialWaveletAnalyzer.SignalType.BUY, 0.9, "New signal");
        assertEquals(1, builder.size());
    }
    
    @Test
    @DisplayName("Should test IntListBuilder")
    void testIntListBuilder() {
        FinancialAnalysisObjectPool.IntListBuilder intList = 
            new FinancialAnalysisObjectPool.IntListBuilder();
        
        // Initially empty
        assertEquals(0, intList.size());
        
        // Add values
        for (int i = 0; i < 20; i++) {
            intList.add(i * 10);
        }
        
        assertEquals(20, intList.size());
        
        // Get values
        for (int i = 0; i < 20; i++) {
            assertEquals(i * 10, intList.get(i));
        }
        
        // Test bounds checking
        assertThrows(IndexOutOfBoundsException.class, 
            () -> intList.get(20));
        
        // Clear
        intList.clear();
        assertEquals(0, intList.size());
        
        // Test with custom capacity
        FinancialAnalysisObjectPool.IntListBuilder customList = 
            new FinancialAnalysisObjectPool.IntListBuilder(5);
        
        // Add more than initial capacity to test growth
        for (int i = 0; i < 10; i++) {
            customList.add(i);
        }
        assertEquals(10, customList.size());
    }
    
    @Test
    @DisplayName("Should test DoubleListBuilder")
    void testDoubleListBuilder() {
        FinancialAnalysisObjectPool.DoubleListBuilder doubleList = 
            new FinancialAnalysisObjectPool.DoubleListBuilder();
        
        // Initially empty
        assertEquals(0, doubleList.size());
        
        // Add values
        doubleList.add(1.5);
        doubleList.add(2.7);
        doubleList.add(3.9);
        
        assertEquals(3, doubleList.size());
        
        // Get values
        assertEquals(1.5, doubleList.get(0));
        assertEquals(2.7, doubleList.get(1));
        assertEquals(3.9, doubleList.get(2));
        
        // Test bounds checking
        assertThrows(IndexOutOfBoundsException.class, 
            () -> doubleList.get(3));
        
        // Clear
        doubleList.clear();
        assertEquals(0, doubleList.size());
        
        // Test capacity growth
        for (int i = 0; i < 100; i++) {
            doubleList.add(i * 0.1);
        }
        assertEquals(100, doubleList.size());
        assertEquals(5.0, doubleList.get(50), 1e-10);
    }
    
    @Test
    @DisplayName("Should test pool statistics")
    void testPoolStatistics() {
        var stats = pool.getStatistics();
        assertNotNull(stats);
        
        // Initially pre-populated with some arrays
        int initialHits = stats.arrayHits();
        int initialMisses = stats.arrayMisses();
        
        // Borrow some arrays
        var h1 = pool.borrowArray(100);
        stats = pool.getStatistics();
        // Should either hit or miss
        assertTrue((stats.arrayHits() + stats.arrayMisses()) > (initialHits + initialMisses));
        
        var h2 = pool.borrowArray(200);
        stats = pool.getStatistics();
        assertTrue((stats.arrayHits() + stats.arrayMisses()) > (initialHits + initialMisses + 1));
        
        // Return arrays
        h1.close();
        stats = pool.getStatistics();
        assertTrue(stats.arrayPoolSize() >= 1);
        
        h2.close();
        stats = pool.getStatistics();
        assertTrue(stats.arrayPoolSize() >= 2);
        
        // Reuse should count as hit
        var h3 = pool.borrowArray(100);
        stats = pool.getStatistics();
        int newHits = stats.arrayHits();
        assertTrue(newHits > initialHits, "Should have hits after reuse");
        
        // Test hit rate
        double hitRate = stats.hitRate();
        assertTrue(hitRate >= 0 && hitRate <= 1.0);
    }
    
    @Test
    @DisplayName("Should test borrowing and returning signal builders")
    void testSignalBuilderPool() {
        // Borrow a signal builder
        var builder1 = pool.borrowSignalBuilder();
        assertNotNull(builder1);
        
        // Add some data
        builder1.addSignal(1, FinancialWaveletAnalyzer.SignalType.BUY, 0.9, "Test");
        assertEquals(1, builder1.size());
        
        // Return it
        pool.returnSignalBuilder(builder1);
        
        // Borrow again - should be cleared
        var builder2 = pool.borrowSignalBuilder();
        assertEquals(0, builder2.size(), "Builder should be cleared when returned");
    }
    
    @Test
    @DisplayName("Should test borrowing and returning list builders")
    void testListBuilderPools() {
        // Test IntListBuilder pool
        var intList1 = pool.borrowIntList();
        assertNotNull(intList1);
        intList1.add(42);
        assertEquals(1, intList1.size());
        
        pool.returnIntList(intList1);
        
        var intList2 = pool.borrowIntList();
        assertEquals(0, intList2.size(), "IntList should be cleared when returned");
        
        // Test DoubleListBuilder pool
        var doubleList1 = pool.borrowDoubleList();
        assertNotNull(doubleList1);
        doubleList1.add(3.14);
        assertEquals(1, doubleList1.size());
        
        pool.returnDoubleList(doubleList1);
        
        var doubleList2 = pool.borrowDoubleList();
        assertEquals(0, doubleList2.size(), "DoubleList should be cleared when returned");
    }
    
    @Test
    @DisplayName("Should test array resizing on borrow")
    void testArrayResizing() {
        // Default size is 1024
        var holder1 = pool.borrowArray(512); 
        assertTrue(holder1.array.length >= 512);
        holder1.close();
        
        // Ask for larger than default (1024)
        var holder2 = pool.borrowArray(2048);
        assertEquals(2048, holder2.array.length, "Should resize to requested size when larger than default");
        holder2.close();
        
        // Borrow smaller - may get a holder with larger array
        var holder3 = pool.borrowArray(512);
        assertNotNull(holder3);
        assertTrue(holder3.array.length >= 512, "Should have at least requested size");
    }
}