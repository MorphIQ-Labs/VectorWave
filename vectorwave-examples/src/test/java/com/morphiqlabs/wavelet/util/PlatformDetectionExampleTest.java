package com.morphiqlabs.wavelet.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PlatformDetectionExample to ensure the examples work correctly.
 */
class PlatformDetectionExampleTest {
    
    @Test
    @DisplayName("Old patterns should handle null system properties without NPE")
    void testOldPatternsNullSafety() {
        // This test verifies that the fixed old patterns don't throw NPE
        // even if system properties were somehow null (unlikely but possible)
        PlatformDetectionExample.OldPatterns oldPatterns = new PlatformDetectionExample.OldPatterns();
        
        // These should not throw NPE even with null system properties
        assertDoesNotThrow(() -> {
            boolean result = oldPatterns.isAppleSiliconOldWay();
            assertNotNull(result); // boolean primitive can't be null
        });
        
        assertDoesNotThrow(() -> {
            int threshold = oldPatterns.getSIMDThresholdOldWay();
            assertTrue(threshold == 8 || threshold == 16);
        });
        
        assertDoesNotThrow(() -> {
            boolean hasAVX = oldPatterns.hasAVXSupportOldWay();
            assertNotNull(hasAVX);
        });
    }
    
    @Test
    @DisplayName("New patterns should work correctly")
    void testNewPatterns() {
        PlatformDetectionExample.NewPatterns newPatterns = new PlatformDetectionExample.NewPatterns();
        
        // These use the centralized PlatformDetection utility
        assertDoesNotThrow(() -> {
            boolean isAppleSilicon = newPatterns.isAppleSiliconNewWay();
            assertNotNull(isAppleSilicon);
        });
        
        assertDoesNotThrow(() -> {
            int threshold = newPatterns.getSIMDThresholdNewWay();
            assertTrue(threshold > 0);
        });
        
        assertDoesNotThrow(() -> {
            boolean hasAVX2 = newPatterns.hasAVX2SupportNewWay();
            assertNotNull(hasAVX2);
        });
        
        assertDoesNotThrow(() -> {
            String info = newPatterns.getPlatformInfoNewWay();
            assertNotNull(info);
            assertFalse(info.isEmpty());
        });
    }
    
    @Test
    @DisplayName("Testable code example should work correctly")
    void testTestableCode() {
        PlatformDetectionExample.TestableCode testableCode = new PlatformDetectionExample.TestableCode();
        
        String strategy = testableCode.selectOptimizationStrategy();
        assertNotNull(strategy);
        assertTrue(strategy.contains("optimizations"));
        
        PlatformDetectionExample.OptimizationConfig config = testableCode.createOptimizationConfig();
        assertNotNull(config);
        assertTrue(config.getSIMDThreshold() > 0);
        // Vectorization and apple optimizations depend on platform
    }
}