package com.morphiqlabs.wavelet.cwt.finance;

import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Test coverage for IncrementalFinancialAnalyzer inner classes.
 */
class IncrementalFinancialAnalyzerTest {
    
    @Test
    @DisplayName("CircularBuffer should handle add and get operations")
    void testCircularBufferAddAndGet() throws Exception {
        // Use reflection to access private inner class
        Class<?> bufferClass = Class.forName(
            "com.morphiqlabs.wavelet.cwt.finance.IncrementalFinancialAnalyzer$CircularBuffer"
        );
        
        Constructor<?> constructor = bufferClass.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
        Object buffer = constructor.newInstance(5);
        
        // Get methods
        Method addMethod = bufferClass.getDeclaredMethod("add", Object.class);
        Method getMethod = bufferClass.getDeclaredMethod("get", int.class);
        Method sizeMethod = bufferClass.getDeclaredMethod("size");
        addMethod.setAccessible(true);
        getMethod.setAccessible(true);
        sizeMethod.setAccessible(true);
        
        // Test adding elements
        addMethod.invoke(buffer, "item1");
        addMethod.invoke(buffer, "item2");
        addMethod.invoke(buffer, "item3");
        
        // Test getting elements
        assertEquals("item1", getMethod.invoke(buffer, 0));
        assertEquals("item2", getMethod.invoke(buffer, 1));
        assertEquals("item3", getMethod.invoke(buffer, 2));
        assertNull(getMethod.invoke(buffer, 3)); // Beyond size
        
        // Test size
        assertEquals(3, sizeMethod.invoke(buffer));
    }
    
    @Test
    @DisplayName("CircularBuffer should wrap around when full")
    void testCircularBufferWrapAround() throws Exception {
        Class<?> bufferClass = Class.forName(
            "com.morphiqlabs.wavelet.cwt.finance.IncrementalFinancialAnalyzer$CircularBuffer"
        );
        
        Constructor<?> constructor = bufferClass.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
        Object buffer = constructor.newInstance(3);
        
        Method addMethod = bufferClass.getDeclaredMethod("add", Object.class);
        Method getMethod = bufferClass.getDeclaredMethod("get", int.class);
        Method sizeMethod = bufferClass.getDeclaredMethod("size");
        addMethod.setAccessible(true);
        getMethod.setAccessible(true);
        sizeMethod.setAccessible(true);
        
        // Fill buffer
        addMethod.invoke(buffer, "item1");
        addMethod.invoke(buffer, "item2");
        addMethod.invoke(buffer, "item3");
        
        // Add more items - should wrap around
        addMethod.invoke(buffer, "item4");
        addMethod.invoke(buffer, "item5");
        
        // Buffer should contain: item3, item4, item5 (oldest items replaced)
        assertEquals("item3", getMethod.invoke(buffer, 0));
        assertEquals("item4", getMethod.invoke(buffer, 1));
        assertEquals("item5", getMethod.invoke(buffer, 2));
        assertEquals(3, sizeMethod.invoke(buffer)); // Size stays at capacity
    }
    
    @Test
    @DisplayName("CircularBuffer should handle clear operation")
    void testCircularBufferClear() throws Exception {
        Class<?> bufferClass = Class.forName(
            "com.morphiqlabs.wavelet.cwt.finance.IncrementalFinancialAnalyzer$CircularBuffer"
        );
        
        Constructor<?> constructor = bufferClass.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
        Object buffer = constructor.newInstance(5);
        
        Method addMethod = bufferClass.getDeclaredMethod("add", Object.class);
        Method clearMethod = bufferClass.getDeclaredMethod("clear");
        Method sizeMethod = bufferClass.getDeclaredMethod("size");
        Method getMethod = bufferClass.getDeclaredMethod("get", int.class);
        addMethod.setAccessible(true);
        clearMethod.setAccessible(true);
        sizeMethod.setAccessible(true);
        getMethod.setAccessible(true);
        
        // Add items
        addMethod.invoke(buffer, "item1");
        addMethod.invoke(buffer, "item2");
        addMethod.invoke(buffer, "item3");
        
        assertEquals(3, sizeMethod.invoke(buffer));
        
        // Clear buffer
        clearMethod.invoke(buffer);
        
        // Check cleared state
        assertEquals(0, sizeMethod.invoke(buffer));
        assertNull(getMethod.invoke(buffer, 0));
        
        // Should be able to add items again
        addMethod.invoke(buffer, "newItem");
        assertEquals(1, sizeMethod.invoke(buffer));
        assertEquals("newItem", getMethod.invoke(buffer, 0));
    }
    
    @Test
    @DisplayName("CircularBuffer edge cases")
    void testCircularBufferEdgeCases() throws Exception {
        Class<?> bufferClass = Class.forName(
            "com.morphiqlabs.wavelet.cwt.finance.IncrementalFinancialAnalyzer$CircularBuffer"
        );
        
        Constructor<?> constructor = bufferClass.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
        
        // Test with capacity 1
        Object singleBuffer = constructor.newInstance(1);
        Method addMethod = bufferClass.getDeclaredMethod("add", Object.class);
        Method getMethod = bufferClass.getDeclaredMethod("get", int.class);
        Method sizeMethod = bufferClass.getDeclaredMethod("size");
        addMethod.setAccessible(true);
        getMethod.setAccessible(true);
        sizeMethod.setAccessible(true);
        
        addMethod.invoke(singleBuffer, "only");
        assertEquals("only", getMethod.invoke(singleBuffer, 0));
        assertEquals(1, sizeMethod.invoke(singleBuffer));
        
        // Replace the single item
        addMethod.invoke(singleBuffer, "new");
        assertEquals("new", getMethod.invoke(singleBuffer, 0));
        assertEquals(1, sizeMethod.invoke(singleBuffer));
        
        // Test empty buffer
        Object emptyBuffer = constructor.newInstance(10);
        assertEquals(0, sizeMethod.invoke(emptyBuffer));
        assertNull(getMethod.invoke(emptyBuffer, 0));
        assertNull(getMethod.invoke(emptyBuffer, 5));
    }
    
    @Test
    @DisplayName("CircularBuffer should handle get with various indices")
    void testCircularBufferGetWithIndices() throws Exception {
        Class<?> bufferClass = Class.forName(
            "com.morphiqlabs.wavelet.cwt.finance.IncrementalFinancialAnalyzer$CircularBuffer"
        );
        
        Constructor<?> constructor = bufferClass.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
        Object buffer = constructor.newInstance(4);
        
        Method addMethod = bufferClass.getDeclaredMethod("add", Object.class);
        Method getMethod = bufferClass.getDeclaredMethod("get", int.class);
        addMethod.setAccessible(true);
        getMethod.setAccessible(true);
        
        // Add some items
        addMethod.invoke(buffer, "a");
        addMethod.invoke(buffer, "b");
        addMethod.invoke(buffer, "c");
        
        // Test valid indices
        assertEquals("a", getMethod.invoke(buffer, 0));
        assertEquals("b", getMethod.invoke(buffer, 1));
        assertEquals("c", getMethod.invoke(buffer, 2));
        
        // Test index at size boundary
        assertNull(getMethod.invoke(buffer, 3));
        
        // Test index beyond size
        assertNull(getMethod.invoke(buffer, 10));
        
        // Add more to wrap around
        addMethod.invoke(buffer, "d");
        addMethod.invoke(buffer, "e"); // This overwrites 'a'
        
        // Now buffer contains: b, c, d, e
        assertEquals("b", getMethod.invoke(buffer, 0));
        assertEquals("c", getMethod.invoke(buffer, 1));
        assertEquals("d", getMethod.invoke(buffer, 2));
        assertEquals("e", getMethod.invoke(buffer, 3));
    }
    
    @Test
    @DisplayName("CircularBuffer with complex wrap-around scenario")
    void testCircularBufferComplexWrapAround() throws Exception {
        Class<?> bufferClass = Class.forName(
            "com.morphiqlabs.wavelet.cwt.finance.IncrementalFinancialAnalyzer$CircularBuffer"
        );
        
        Constructor<?> constructor = bufferClass.getDeclaredConstructor(int.class);
        constructor.setAccessible(true);
        Object buffer = constructor.newInstance(3);
        
        Method addMethod = bufferClass.getDeclaredMethod("add", Object.class);
        Method getMethod = bufferClass.getDeclaredMethod("get", int.class);
        Method sizeMethod = bufferClass.getDeclaredMethod("size");
        Method clearMethod = bufferClass.getDeclaredMethod("clear");
        addMethod.setAccessible(true);
        getMethod.setAccessible(true);
        sizeMethod.setAccessible(true);
        clearMethod.setAccessible(true);
        
        // Scenario 1: Fill, wrap, and check
        for (int i = 1; i <= 7; i++) {
            addMethod.invoke(buffer, "item" + i);
        }
        
        // Should contain: item5, item6, item7
        assertEquals(3, sizeMethod.invoke(buffer));
        assertEquals("item5", getMethod.invoke(buffer, 0));
        assertEquals("item6", getMethod.invoke(buffer, 1));
        assertEquals("item7", getMethod.invoke(buffer, 2));
        
        // Scenario 2: Clear and refill
        clearMethod.invoke(buffer);
        assertEquals(0, sizeMethod.invoke(buffer));
        
        addMethod.invoke(buffer, "new1");
        addMethod.invoke(buffer, "new2");
        
        assertEquals(2, sizeMethod.invoke(buffer));
        assertEquals("new1", getMethod.invoke(buffer, 0));
        assertEquals("new2", getMethod.invoke(buffer, 1));
        assertNull(getMethod.invoke(buffer, 2));
    }
    
}