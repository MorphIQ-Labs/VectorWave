package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for filter truncation validation in MultiLevelMODWTTransform.
 */
class MultiLevelMODWTFilterTruncationTest {
    
    @Test
    void testGetTruncatedFilterValidation() throws Exception {
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(
            new Haar(), BoundaryMode.PERIODIC);
        
        // Access private getTruncatedFilter method via reflection
        Method getTruncatedFilter = MultiLevelMODWTTransform.class
            .getDeclaredMethod("getTruncatedFilter", double[].class, int.class, 
                             Class.forName("com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform$FilterType"));
        getTruncatedFilter.setAccessible(true);
        
        // Get FilterType enum
        Class<?> filterTypeClass = Class.forName("com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform$FilterType");
        Object lowFilterType = filterTypeClass.getEnumConstants()[0]; // LOW
        
        double[] filter = {1.0, 2.0, 3.0, 4.0, 5.0};
        
        // Test valid truncation
        double[] truncated = (double[]) getTruncatedFilter.invoke(transform, filter, 3, lowFilterType);
        assertEquals(3, truncated.length);
        assertArrayEquals(new double[]{1.0, 2.0, 3.0}, truncated);
        
        // Test truncation to same length (should work)
        truncated = (double[]) getTruncatedFilter.invoke(transform, filter, 5, lowFilterType);
        assertEquals(5, truncated.length);
        assertArrayEquals(filter, truncated);
        
        // Test invalid cases
        
        // Negative target length
        Exception ex = assertThrows(Exception.class, () -> {
            getTruncatedFilter.invoke(transform, filter, -1, lowFilterType);
        });
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("Target length must be positive"));
        
        // Zero target length
        ex = assertThrows(Exception.class, () -> {
            getTruncatedFilter.invoke(transform, filter, 0, lowFilterType);
        });
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("Target length must be positive"));
        
        // Target length exceeds filter length
        ex = assertThrows(Exception.class, () -> {
            getTruncatedFilter.invoke(transform, filter, 10, lowFilterType);
        });
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("exceeds filter length"));
    }
    
    @Test
    void testFilterTruncationInPractice() {
        // Create a scenario where filter truncation would occur
        // This happens when the scaled filter at high levels exceeds signal length
        
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(
            new Haar(), BoundaryMode.PERIODIC);
        
        // Very short signal
        double[] shortSignal = {1.0, 2.0, 3.0, 4.0};
        
        // This should work without throwing exceptions
        // The filters will be truncated internally when needed
        assertDoesNotThrow(() -> {
            MultiLevelMODWTResult result = transform.decompose(shortSignal);
            double[] reconstructed = transform.reconstruct(result);
            
            // Check reconstruction accuracy
            assertArrayEquals(shortSignal, reconstructed, 1e-10);
        });
    }
    
    @Test
    void testCacheEfficiency() throws Exception {
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(
            new Haar(), BoundaryMode.PERIODIC);
        
        // Access private method and cache via reflection
        Method getTruncatedFilter = MultiLevelMODWTTransform.class
            .getDeclaredMethod("getTruncatedFilter", double[].class, int.class, 
                             Class.forName("com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform$FilterType"));
        getTruncatedFilter.setAccessible(true);
        
        Class<?> filterTypeClass = Class.forName("com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform$FilterType");
        Object lowFilterType = filterTypeClass.getEnumConstants()[0]; // LOW
        
        double[] filter = {1.0, 2.0, 3.0, 4.0, 5.0};
        
        // Call multiple times with same parameters
        double[] result1 = (double[]) getTruncatedFilter.invoke(transform, filter, 3, lowFilterType);
        double[] result2 = (double[]) getTruncatedFilter.invoke(transform, filter, 3, lowFilterType);
        
        // Should return the same cached instance
        assertSame(result1, result2, "Cache should return the same instance");
        
        // Different target length should create new instance
        double[] result3 = (double[]) getTruncatedFilter.invoke(transform, filter, 4, lowFilterType);
        assertNotSame(result1, result3);
        assertEquals(4, result3.length);
    }
}