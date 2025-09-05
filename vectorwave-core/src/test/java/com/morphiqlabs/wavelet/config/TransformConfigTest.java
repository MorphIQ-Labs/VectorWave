package com.morphiqlabs.wavelet.config;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for TransformConfig to achieve full coverage.
 */
class TransformConfigTest {

    @Test
    @DisplayName("Should create default configuration")
    void testDefaultConfig() {
        TransformConfig config = TransformConfig.defaultConfig();
        
        assertNotNull(config);
        assertEquals(BoundaryMode.PERIODIC, config.getBoundaryMode());
        assertFalse(config.isForceScalar());
        assertFalse(config.isForceVector());
        assertEquals(20, config.getMaxDecompositionLevels());
    }

    @Test
    @DisplayName("Should create configuration with builder")
    void testBuilderBasic() {
        TransformConfig config = TransformConfig.builder()
            .boundaryMode(BoundaryMode.ZERO_PADDING)
            .forceScalar(true)
            .maxDecompositionLevels(10)
            .build();
        
        assertEquals(BoundaryMode.ZERO_PADDING, config.getBoundaryMode());
        assertTrue(config.isForceScalar());
        assertFalse(config.isForceVector());
        assertEquals(10, config.getMaxDecompositionLevels());
    }

    @Test
    @DisplayName("Should handle force vector configuration")
    void testForceVector() {
        TransformConfig config = TransformConfig.builder()
            .forceVector(true)
            .build();
        
        assertFalse(config.isForceScalar());
        assertTrue(config.isForceVector());
    }

    @ParameterizedTest
    @EnumSource(BoundaryMode.class)
    @DisplayName("Should accept all boundary modes")
    void testAllBoundaryModes(BoundaryMode boundaryMode) {
        TransformConfig config = TransformConfig.builder()
            .boundaryMode(boundaryMode)
            .build();
        
        assertEquals(boundaryMode, config.getBoundaryMode());
    }

    @Test
    @DisplayName("Should throw exception for conflicting scalar and vector flags")
    void testConflictingScalarVector() {
        assertThrows(InvalidConfigurationException.class, () -> {
            TransformConfig.builder()
                .forceScalar(true)
                .forceVector(true)
                .build();
        });
    }

    @Test
    @DisplayName("Should throw exception for null boundary mode")
    void testNullBoundaryMode() {
        assertThrows(InvalidArgumentException.class, () -> {
            TransformConfig.builder()
                .boundaryMode(null)
                .build();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10})
    @DisplayName("Should throw exception for invalid max decomposition levels")
    void testInvalidMaxLevels(int invalidLevels) {
        assertThrows(InvalidArgumentException.class, () -> {
            TransformConfig.builder()
                .maxDecompositionLevels(invalidLevels)
                .build();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 5, 10, 15, 20, 25})
    @DisplayName("Should accept valid max decomposition levels")
    void testValidMaxLevels(int validLevels) {
        TransformConfig config = TransformConfig.builder()
            .maxDecompositionLevels(validLevels)
            .build();
        
        assertEquals(validLevels, config.getMaxDecompositionLevels());
    }

    @Test
    @DisplayName("Should support method chaining in builder")
    void testBuilderChaining() {
        TransformConfig config = TransformConfig.builder()
            .boundaryMode(BoundaryMode.SYMMETRIC)
            .forceScalar(false)
            .forceVector(false)
            .maxDecompositionLevels(8)
            .build();
        
        assertEquals(BoundaryMode.SYMMETRIC, config.getBoundaryMode());
        assertFalse(config.isForceScalar());
        assertFalse(config.isForceVector());
        assertEquals(8, config.getMaxDecompositionLevels());
    }

    @Test
    @DisplayName("Should provide both isForceScalar methods")
    void testForceScalarMethods() {
        TransformConfig scalarConfig = TransformConfig.builder()
            .forceScalar(true)
            .build();
        
        assertTrue(scalarConfig.isForceScalar());
        assertTrue(scalarConfig.isForceScalarOperations());
        
        TransformConfig nonScalarConfig = TransformConfig.builder()
            .forceScalar(false)
            .build();
        
        assertFalse(nonScalarConfig.isForceScalar());
        assertFalse(nonScalarConfig.isForceScalarOperations());
    }

    @Test
    @DisplayName("Should provide meaningful toString representation")
    void testToString() {
        TransformConfig config = TransformConfig.builder()
            .boundaryMode(BoundaryMode.SYMMETRIC)
            .forceScalar(true)
            .maxDecompositionLevels(15)
            .build();
        
        String toString = config.toString();
        
        assertNotNull(toString);
        assertTrue(toString.contains("TransformConfig"));
        assertTrue(toString.contains("boundaryMode=SYMMETRIC"));
        assertTrue(toString.contains("forceScalar=true"));
        assertTrue(toString.contains("forceVector=false"));
        assertTrue(toString.contains("maxDecompositionLevels=15"));
    }

    @Test
    @DisplayName("Should handle complex configuration scenarios")
    void testComplexScenarios() {
        // Scenario 1: Performance debugging configuration
        TransformConfig debugConfig = TransformConfig.builder()
            .boundaryMode(BoundaryMode.ZERO_PADDING)
            .forceScalar(true)
            .maxDecompositionLevels(5)
            .build();
        
        assertTrue(debugConfig.isForceScalar());
        assertEquals(BoundaryMode.ZERO_PADDING, debugConfig.getBoundaryMode());
        assertEquals(5, debugConfig.getMaxDecompositionLevels());
        
        // Scenario 2: High-performance configuration
        TransformConfig perfConfig = TransformConfig.builder()
            .boundaryMode(BoundaryMode.PERIODIC)
            .forceVector(true)
            .maxDecompositionLevels(12)
            .build();
        
        assertTrue(perfConfig.isForceVector());
        assertEquals(BoundaryMode.PERIODIC, perfConfig.getBoundaryMode());
        assertEquals(12, perfConfig.getMaxDecompositionLevels());
        
        // Scenario 3: Conservative configuration for small signals
        TransformConfig conservativeConfig = TransformConfig.builder()
            .boundaryMode(BoundaryMode.SYMMETRIC)
            .maxDecompositionLevels(3)
            .build();
        
        assertFalse(conservativeConfig.isForceScalar());
        assertFalse(conservativeConfig.isForceVector());
        assertEquals(3, conservativeConfig.getMaxDecompositionLevels());
    }

    @Test
    @DisplayName("Should create multiple independent builders")
    void testIndependentBuilders() {
        TransformConfig.Builder builder1 = TransformConfig.builder();
        TransformConfig.Builder builder2 = TransformConfig.builder();
        
        assertNotSame(builder1, builder2);
        
        // Configure builders differently
        TransformConfig config1 = builder1
            .forceScalar(true)
            .maxDecompositionLevels(5)
            .build();
            
        TransformConfig config2 = builder2
            .forceVector(true)
            .maxDecompositionLevels(10)
            .build();
        
        // Verify configurations are independent
        assertTrue(config1.isForceScalar());
        assertFalse(config1.isForceVector());
        assertEquals(5, config1.getMaxDecompositionLevels());
        
        assertFalse(config2.isForceScalar());
        assertTrue(config2.isForceVector());
        assertEquals(10, config2.getMaxDecompositionLevels());
    }

    @Test
    @DisplayName("Should validate boundary mode is preserved")
    void testBoundaryModePreservation() {
        for (BoundaryMode mode : BoundaryMode.values()) {
            TransformConfig config = TransformConfig.builder()
                .boundaryMode(mode)
                .build();
            
            assertEquals(mode, config.getBoundaryMode(), 
                "Boundary mode should be preserved: " + mode);
        }
    }

    @Test
    @DisplayName("Should handle edge case configurations")
    void testEdgeCases() {
        // Minimum valid max levels
        TransformConfig minConfig = TransformConfig.builder()
            .maxDecompositionLevels(1)
            .build();
        assertEquals(1, minConfig.getMaxDecompositionLevels());
        
        // Large max levels
        TransformConfig maxConfig = TransformConfig.builder()
            .maxDecompositionLevels(50)
            .build();
        assertEquals(50, maxConfig.getMaxDecompositionLevels());
        
        // All flags false (default behavior)
        TransformConfig defaultFlags = TransformConfig.builder()
            .forceScalar(false)
            .forceVector(false)
            .build();
        assertFalse(defaultFlags.isForceScalar());
        assertFalse(defaultFlags.isForceVector());
    }
}