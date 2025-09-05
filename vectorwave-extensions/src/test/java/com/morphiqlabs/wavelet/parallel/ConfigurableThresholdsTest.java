package com.morphiqlabs.wavelet.extensions.parallel;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.swt.VectorWaveSwtAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for configurable parallelization thresholds.
 * Verifies that hard-coded thresholds have been replaced with configurable alternatives.
 */
class ConfigurableThresholdsTest {
    
    @Test
    @DisplayName("VectorWaveSwtAdapter should use configurable parallel threshold")
    void testSwtAdapterConfigurableThreshold() {
        // Test with default threshold
        VectorWaveSwtAdapter defaultAdapter = new VectorWaveSwtAdapter(Daubechies.DB4);
        Map<String, Object> defaultStats = defaultAdapter.getCacheStatistics();
        assertEquals(4096, defaultStats.get("parallelThreshold"));
        
        // Test with custom threshold via constructor parameters
        VectorWaveSwtAdapter customAdapter = new VectorWaveSwtAdapter(
            Daubechies.DB4, BoundaryMode.PERIODIC, true, 2048);
        Map<String, Object> customStats = customAdapter.getCacheStatistics();
        assertEquals(2048, customStats.get("parallelThreshold"));
    }
    
    @Test
    @DisplayName("ParallelConfig should accept custom thresholds")
    void testParallelConfigCustomThresholds() {
        ParallelConfig config = new ParallelConfig.Builder()
            .parallelThreshold(1234)
            .build();
        
        assertEquals(1234, config.getParallelThreshold());
    }
    
    @Test
    @DisplayName("ParallelConfig should make sensible parallelization decisions")
    void testParallelizationDecisionMaking() {
        // Low threshold config - should parallelize smaller signals
        ParallelConfig lowThreshold = new ParallelConfig.Builder()
            .parallelThreshold(100)
            .build();
        
        // High threshold config - should only parallelize large signals
        ParallelConfig highThreshold = new ParallelConfig.Builder()
            .parallelThreshold(10000)
            .build();
        
        int mediumSignalSize = 1000;
        double complexity = 2.0; // Reasonable complexity factor
        
        // Low threshold should parallelize medium-sized signals
        assertTrue(lowThreshold.shouldParallelize(mediumSignalSize, complexity));
        
        // High threshold should not parallelize medium-sized signals
        assertFalse(highThreshold.shouldParallelize(mediumSignalSize, complexity));
        
        // Both should parallelize very large signals
        int largeSignalSize = 50000;
        assertTrue(lowThreshold.shouldParallelize(largeSignalSize, complexity));
        assertTrue(highThreshold.shouldParallelize(largeSignalSize, complexity));
    }
    
    @Test 
    @DisplayName("Threshold validation should reject invalid values")
    void testThresholdValidation() {
        // Zero and negative thresholds should be rejected
        assertThrows(IllegalArgumentException.class, () ->
            new ParallelConfig.Builder().parallelThreshold(0));
        
        assertThrows(IllegalArgumentException.class, () ->
            new ParallelConfig.Builder().parallelThreshold(-100));
        
        // Positive thresholds should be accepted
        assertDoesNotThrow(() ->
            new ParallelConfig.Builder().parallelThreshold(1));
        
        assertDoesNotThrow(() ->
            new ParallelConfig.Builder().parallelThreshold(1000000));
    }
}
