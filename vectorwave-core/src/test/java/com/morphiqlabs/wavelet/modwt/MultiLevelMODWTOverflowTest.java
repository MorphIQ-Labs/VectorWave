package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for overflow protection in MultiLevelMODWTTransform.
 */
class MultiLevelMODWTOverflowTest {
    
    @Test
    void testCalculateMaxLevelsWithOverflowProtection() throws Exception {
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(
            new Haar(), BoundaryMode.PERIODIC);
        
        // Access private calculateMaxLevels method via reflection
        Method calculateMaxLevels = MultiLevelMODWTTransform.class
            .getDeclaredMethod("calculateMaxLevels", int.class);
        calculateMaxLevels.setAccessible(true);
        
        // Test normal case
        int maxLevels = (int) calculateMaxLevels.invoke(transform, 1024);
        assertTrue(maxLevels > 0 && maxLevels <= 10);
        
        // Test with very large signal that would cause overflow without protection
        // For Haar (filterLength=2), at level 31, scaleFactor = 1 << 30 = 1073741824
        // scaledFilterLength = (2-1) * 1073741824 + 1 = 1073741825
        // This is still within int range, but level 32 would overflow
        maxLevels = (int) calculateMaxLevels.invoke(transform, Integer.MAX_VALUE);
        assertTrue(maxLevels <= 10); // Should be capped at MAX_DECOMPOSITION_LEVELS
    }
    
    @Test
    void testScaleFiltersForLevelOverflowProtection() throws Exception {
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(
            new Haar(), BoundaryMode.PERIODIC);
        
        // Access private method via reflection
        Method scaleFiltersForLevel = MultiLevelMODWTTransform.class
            .getDeclaredMethod("scaleFiltersForLevel", double[].class, double[].class, int.class);
        scaleFiltersForLevel.setAccessible(true);
        
        double[] lowFilter = {1.0, 2.0};
        double[] highFilter = {1.0, -2.0};
        
        // Test normal levels
        assertDoesNotThrow(() -> {
            scaleFiltersForLevel.invoke(transform, lowFilter, highFilter, 1);
            scaleFiltersForLevel.invoke(transform, lowFilter, highFilter, 5);
            scaleFiltersForLevel.invoke(transform, lowFilter, highFilter, 10);
        });
        
        // Test level that would cause overflow
        // At level 31, upFactor = 1 << 30 = 1073741824
        // With a longer filter, this could overflow
        double[] longLowFilter = new double[1000];
        double[] longHighFilter = new double[1000];
        assertThrows(Exception.class, () -> {
            scaleFiltersForLevel.invoke(transform, longLowFilter, longHighFilter, 31);
        });
    }
    
    @Test
    void testUpsampleFiltersForLevelOverflowProtection() throws Exception {
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(
            new Haar(), BoundaryMode.PERIODIC);
        
        // Access private method via reflection
        Method upsampleFiltersForLevel = MultiLevelMODWTTransform.class
            .getDeclaredMethod("upsampleFiltersForLevel", double[].class, double[].class, int.class);
        upsampleFiltersForLevel.setAccessible(true);
        
        double[] lowFilter = {1.0, 2.0};
        double[] highFilter = {1.0, -2.0};
        
        // Test normal levels
        assertDoesNotThrow(() -> {
            upsampleFiltersForLevel.invoke(transform, lowFilter, highFilter, 1);
            upsampleFiltersForLevel.invoke(transform, lowFilter, highFilter, 5);
            upsampleFiltersForLevel.invoke(transform, lowFilter, highFilter, 10);
        });
        
        // Test level that would cause overflow with a long filter
        double[] longLowFilter = new double[1000];
        double[] longHighFilter = new double[1000];
        assertThrows(Exception.class, () -> {
            upsampleFiltersForLevel.invoke(transform, longLowFilter, longHighFilter, 31);
        });
    }
    
    @Test
    void testDecompositionWithMaxLevels() {
        // Test that decomposition works correctly up to MAX_DECOMPOSITION_LEVELS
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(
            new Haar(), BoundaryMode.PERIODIC);
        
        // Create a very large signal that would theoretically support many levels
        double[] signal = new double[1_000_000];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.random();
        }
        
        // Decompose to maximum possible levels
        MultiLevelMODWTResult result = transform.decompose(signal);
        
        // Should be capped at 10 (MAX_DECOMPOSITION_LEVELS)
        assertTrue(result.getLevels() <= 10);
        
        // Verify reconstruction still works
        double[] reconstructed = transform.reconstruct(result);
        assertEquals(signal.length, reconstructed.length);
        
        // Check reconstruction accuracy (allowing for numerical errors)
        double maxError = 0;
        for (int i = 0; i < signal.length; i++) {
            maxError = Math.max(maxError, Math.abs(signal[i] - reconstructed[i]));
        }
        assertTrue(maxError < 1e-10, "Max reconstruction error: " + maxError);
    }
}