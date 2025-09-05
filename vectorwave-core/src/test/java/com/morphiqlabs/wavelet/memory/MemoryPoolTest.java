package com.morphiqlabs.wavelet.memory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MemoryPoolTest {

    private MemoryPool memoryPool;

    @BeforeEach
    void setUp() {
        memoryPool = new MemoryPool();
    }

    @Test
    void testBorrowArray_CreatesNewArrayWhenPoolEmpty() {
        double[] array = memoryPool.borrowArray(10);
        assertNotNull(array);
        assertEquals(10, array.length);
        assertArrayEquals(new double[10], array); // Should be initialized with zeros
    }

    @Test
    void testBorrowArray_ReusesPreviouslyReturnedArray() {
        // Borrow and return an array
        double[] firstArray = memoryPool.borrowArray(10);
        firstArray[0] = 123.456; // Mark it
        memoryPool.returnArray(firstArray);

        // Borrow again - should get a cleared array (not the same values)
        double[] secondArray = memoryPool.borrowArray(10);
        assertNotNull(secondArray);
        assertEquals(10, secondArray.length);
        assertEquals(0.0, secondArray[0]); // Should be cleared
    }

    @Test
    void testReturnArray_ClearsArrayBeforePooling() {
        double[] array = new double[]{1.0, 2.0, 3.0, 4.0, 5.0};
        memoryPool.returnArray(array);
        
        // Verify array was cleared
        assertArrayEquals(new double[5], array);
        
        // Verify it gets reused
        double[] borrowed = memoryPool.borrowArray(5);
        assertArrayEquals(new double[5], borrowed);
    }

    @Test
    void testReturnArray_HandlesNull() {
        assertDoesNotThrow(() -> memoryPool.returnArray(null));
        assertEquals(0, memoryPool.getTotalPooledCount());
    }

    @Test
    void testMaxArraysPerSize_LimitsPoolSize() {
        memoryPool.setMaxArraysPerSize(3);
        
        // Return 5 arrays of size 10
        for (int i = 0; i < 5; i++) {
            memoryPool.returnArray(new double[10]);
        }
        
        // Only 3 should be pooled
        assertEquals(3, memoryPool.getPooledCount(10));
    }

    @Test
    void testClear_RemovesAllPooledArrays() {
        // Add arrays of different sizes
        memoryPool.returnArray(new double[10]);
        memoryPool.returnArray(new double[20]);
        memoryPool.returnArray(new double[30]);
        
        assertTrue(memoryPool.getTotalPooledCount() > 0);
        
        memoryPool.clear();
        
        assertEquals(0, memoryPool.getTotalPooledCount());
        assertEquals(0, memoryPool.getPooledCount(10));
        assertEquals(0, memoryPool.getPooledCount(20));
        assertEquals(0, memoryPool.getPooledCount(30));
    }

    @Test
    void testGetPooledCount_ForSpecificSize() {
        assertEquals(0, memoryPool.getPooledCount(10));
        
        memoryPool.returnArray(new double[10]);
        assertEquals(1, memoryPool.getPooledCount(10));
        
        memoryPool.returnArray(new double[10]);
        assertEquals(2, memoryPool.getPooledCount(10));
        
        memoryPool.returnArray(new double[20]);
        assertEquals(2, memoryPool.getPooledCount(10)); // Size 10 count unchanged
        assertEquals(1, memoryPool.getPooledCount(20));
    }

    @Test
    void testGetPooledCount_ForNonExistentSize() {
        assertEquals(0, memoryPool.getPooledCount(999));
    }

    @Test
    void testGetTotalPooledCount() {
        assertEquals(0, memoryPool.getTotalPooledCount());
        
        memoryPool.returnArray(new double[10]);
        memoryPool.returnArray(new double[10]);
        memoryPool.returnArray(new double[20]);
        memoryPool.returnArray(new double[30]);
        
        assertEquals(4, memoryPool.getTotalPooledCount());
    }

    @Test
    void testDifferentSizePools() {
        // Test that different sizes are pooled separately
        double[] array10 = memoryPool.borrowArray(10);
        double[] array20 = memoryPool.borrowArray(20);
        
        memoryPool.returnArray(array10);
        memoryPool.returnArray(array20);
        
        assertEquals(1, memoryPool.getPooledCount(10));
        assertEquals(1, memoryPool.getPooledCount(20));
        
        // Borrowing size 10 shouldn't affect size 20
        memoryPool.borrowArray(10);
        assertEquals(0, memoryPool.getPooledCount(10));
        assertEquals(1, memoryPool.getPooledCount(20));
    }

    @Test
    void testReuseEfficiency() {
        // Simulate a pattern of borrow and return
        for (int i = 0; i < 10; i++) {
            double[] array = memoryPool.borrowArray(100);
            memoryPool.returnArray(array);
        }
        
        // After the first creation, all subsequent borrows should reuse
        // Check by borrowing one more time
        double[] array = memoryPool.borrowArray(100);
        assertNotNull(array);
        assertEquals(100, array.length);
    }

    @Test
    void testPrintStatistics() {
        // Redirect System.out to capture output
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        
        try {
            System.setOut(new PrintStream(outContent));
            
            // Perform some operations
            double[] array1 = memoryPool.borrowArray(10);
            double[] array2 = memoryPool.borrowArray(20);
            memoryPool.returnArray(array1);
            memoryPool.borrowArray(10); // Reuse
            
            memoryPool.printStatistics();
            
            String output = outContent.toString();
            assertTrue(output.contains("Memory Pool Statistics"));
            assertTrue(output.contains("Total borrowed: 3"));
            assertTrue(output.contains("Total returned: 1"));
            assertTrue(output.contains("Total created: 2"));
            assertTrue(output.contains("Reuse rate:"));
            assertTrue(output.contains("Currently pooled:"));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testPrintStatistics_EmptyPool() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        
        try {
            System.setOut(new PrintStream(outContent));
            
            memoryPool.printStatistics();
            
            String output = outContent.toString();
            assertTrue(output.contains("Total borrowed: 0"));
            assertTrue(output.contains("Total returned: 0"));
            assertTrue(output.contains("Total created: 0"));
            assertTrue(output.contains("Reuse rate: 0.0%"));
            
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testConcurrentAccess() throws InterruptedException {
        int threadCount = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        double[] array = memoryPool.borrowArray(50);
                        // Simulate some work
                        Arrays.fill(array, j);
                        memoryPool.returnArray(array);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        assertTrue(latch.await(5, TimeUnit.SECONDS));
        executor.shutdown();
        
        // Verify pool is still consistent
        assertTrue(memoryPool.getPooledCount(50) <= 10); // Default max arrays per size
    }

    @Test
    void testGetMaxArraysPerSize() {
        // Test default value through behavior
        for (int i = 0; i < 15; i++) {
            memoryPool.returnArray(new double[10]);
        }
        
        // Default limit is 10
        assertTrue(memoryPool.getPooledCount(10) <= 10);
    }

    @Test
    void testSetMaxArraysPerSize_UpdatesLimit() {
        memoryPool.setMaxArraysPerSize(5);
        
        for (int i = 0; i < 10; i++) {
            memoryPool.returnArray(new double[10]);
        }
        
        assertEquals(5, memoryPool.getPooledCount(10));
    }

    @Test
    void testArrayIndependence() {
        // Ensure borrowed arrays are independent
        double[] array1 = memoryPool.borrowArray(5);
        double[] array2 = memoryPool.borrowArray(5);
        
        assertNotSame(array1, array2);
        
        array1[0] = 123.0;
        assertEquals(0.0, array2[0]);
    }

    @Test
    void testZeroSizeArray() {
        double[] array = memoryPool.borrowArray(0);
        assertNotNull(array);
        assertEquals(0, array.length);
        
        memoryPool.returnArray(array);
        assertEquals(1, memoryPool.getPooledCount(0));
    }

    // Helper method to access maxArraysPerSize (since there's no getter)
    private int getMaxArraysPerSize() {
        // We can infer this by testing behavior
        return 10; // Default value from the code
    }
}