package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify bit shift overflow protection in MultiLevelMODWTTransform.
 */
class MultiLevelMODWTBitShiftOverflowTest {
    
    @Test
    void testCalculateMaxLevelsDoesNotOverflow() throws Exception {
        Haar haar = new Haar();
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(haar, BoundaryMode.PERIODIC);
        
        // Access private calculateMaxLevels method via reflection
        Method calculateMaxLevels = MultiLevelMODWTTransform.class
            .getDeclaredMethod("calculateMaxLevels", int.class);
        calculateMaxLevels.setAccessible(true);
        
        // Test with various signal lengths
        // Should not overflow even with very large signal
        int maxLevels = (int) calculateMaxLevels.invoke(transform, Integer.MAX_VALUE);
        assertTrue(maxLevels > 0 && maxLevels <= 10, 
            "Max levels should be reasonable even for MAX_VALUE signal length");
        
        // Test edge case where we might approach 31 levels
        // This should be safely capped without overflow
        maxLevels = (int) calculateMaxLevels.invoke(transform, 1_000_000_000);
        assertTrue(maxLevels > 0 && maxLevels <= 10, 
            "Max levels should be capped at MAX_DECOMPOSITION_LEVELS");
    }
    
    @Test
    void testUpsampleFiltersForLevelWithHighLevel() throws Exception {
        Haar haar = new Haar();
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(haar, BoundaryMode.PERIODIC);
        
        // Access private upsampleFiltersForLevel method via reflection
        Method upsampleFiltersForLevel = MultiLevelMODWTTransform.class
            .getDeclaredMethod("upsampleFiltersForLevel", double[].class, double[].class, int.class);
        upsampleFiltersForLevel.setAccessible(true);
        
        double[] lowFilter = {0.7071, 0.7071};
        double[] highFilter = {0.7071, -0.7071};
        
        // Test with level that would cause overflow (32 or higher)
        assertThrows(Exception.class, () -> {
            upsampleFiltersForLevel.invoke(transform, lowFilter, highFilter, 32);
        }, "Should throw exception for level >= 32");
        
        // Test with maximum safe level (31)
        // This should also throw because it would create massive arrays
        assertThrows(Exception.class, () -> {
            upsampleFiltersForLevel.invoke(transform, lowFilter, highFilter, 31);
        }, "Should throw exception for level 31");
    }
    
    @Test
    void testScaleFiltersForLevelWithHighLevel() throws Exception {
        Haar haar = new Haar();
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(haar, BoundaryMode.PERIODIC);
        
        // Access private scaleFiltersForLevel method via reflection
        Method scaleFiltersForLevel = MultiLevelMODWTTransform.class
            .getDeclaredMethod("scaleFiltersForLevel", double[].class, double[].class, int.class);
        scaleFiltersForLevel.setAccessible(true);
        
        double[] lowFilter = {0.7071, 0.7071};
        double[] highFilter = {0.7071, -0.7071};
        
        // Test with level that would cause overflow
        assertThrows(Exception.class, () -> {
            scaleFiltersForLevel.invoke(transform, lowFilter, highFilter, 32);
        }, "Should throw exception for level >= 32");
    }
    
    @Test
    void testDecomposeWithReasonableLevels() {
        Haar haar = new Haar();
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(haar, BoundaryMode.PERIODIC);
        
        // Create a signal
        double[] signal = new double[1024];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(i * 0.1);
        }
        
        // Should work fine with reasonable levels
        MultiLevelMODWTResult result = transform.decompose(signal, 5);
        assertNotNull(result);
        assertEquals(5, result.getLevels());
        
        // Should reject levels that are too high
        int maxLevels = transform.getMaximumLevels(signal.length);
        assertThrows(InvalidArgumentException.class, () -> {
            transform.decompose(signal, maxLevels + 1);
        }, "Should reject levels exceeding maximum");
    }
}