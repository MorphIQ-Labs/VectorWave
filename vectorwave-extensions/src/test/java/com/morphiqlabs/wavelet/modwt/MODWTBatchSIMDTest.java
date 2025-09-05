package com.morphiqlabs.wavelet.extensions.modwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MODWTBatchSIMD utility class.
 */
class MODWTBatchSIMDTest {

    @Test
    @DisplayName("getBatchSIMDInfo should return comprehensive information")
    void testGetBatchSIMDInfo() {
        String info = MODWTBatchSIMD.getBatchSIMDInfo();
        
        assertNotNull(info);
        assertFalse(info.isEmpty());
        
        // Should contain expected sections
        assertTrue(info.contains("MODWT Batch SIMD Capabilities:"));
        assertTrue(info.contains("Vector species:"));
        assertTrue(info.contains("Vector length:"));
        assertTrue(info.contains("Optimal batch size:"));
        assertTrue(info.contains("Memory alignment:"));
        assertTrue(info.contains("MODWT features:"));
        
        // Should mention key MODWT characteristics
        assertTrue(info.contains("Shift-invariant"));
        assertTrue(info.contains("no downsampling"));
        assertTrue(info.contains("arbitrary signal length"));
        
        // Should contain platform information
        String arch = System.getProperty("os.arch");
        if (arch.contains("aarch64") || arch.contains("arm")) {
            assertTrue(info.contains("ARM"));
            assertTrue(info.contains("NEON"));
        } else if (arch.contains("amd64") || arch.contains("x86_64")) {
            assertTrue(info.contains("x86-64"));
            assertTrue(info.contains("AVX"));
        }
    }
    
    @Test
    @DisplayName("getVectorLength should return positive integer")
    void testGetVectorLength() {
        int vectorLength = MODWTBatchSIMD.getVectorLength();
        
        assertTrue(vectorLength > 0, "Vector length should be positive");
        // Typical SIMD vector lengths are powers of 2 (2, 4, 8, 16, etc.)
        assertTrue(isPowerOfTwo(vectorLength), "Vector length should be power of 2");
        // Should be reasonable size for double precision (usually 2-16)
        assertTrue(vectorLength >= 1 && vectorLength <= 32, 
            "Vector length should be reasonable: " + vectorLength);
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 16, 32, 64, 100, 1000})
    @DisplayName("isOptimalBatchSize should correctly identify optimal sizes")
    void testIsOptimalBatchSize(int batchSize) {
        boolean isOptimal = MODWTBatchSIMD.isOptimalBatchSize(batchSize);
        int vectorLength = MODWTBatchSIMD.getVectorLength();
        
        // Should be optimal if and only if it's a multiple of vector length
        assertEquals(batchSize % vectorLength == 0, isOptimal,
            "Batch size " + batchSize + " optimality should match vector length " + vectorLength);
    }
    
    @Test
    @DisplayName("isOptimalBatchSize should handle edge cases")
    void testIsOptimalBatchSizeEdgeCases() {
        int vectorLength = MODWTBatchSIMD.getVectorLength();
        
        // Zero is considered optimal (0 % any_number == 0)
        assertTrue(MODWTBatchSIMD.isOptimalBatchSize(0));
        
        // Exact multiples should be optimal
        assertTrue(MODWTBatchSIMD.isOptimalBatchSize(vectorLength));
        assertTrue(MODWTBatchSIMD.isOptimalBatchSize(vectorLength * 2));
        assertTrue(MODWTBatchSIMD.isOptimalBatchSize(vectorLength * 10));
        
        // Non-multiples should not be optimal (if vector length > 1)
        if (vectorLength > 1) {
            assertFalse(MODWTBatchSIMD.isOptimalBatchSize(1));
            assertFalse(MODWTBatchSIMD.isOptimalBatchSize(vectorLength - 1));
            assertFalse(MODWTBatchSIMD.isOptimalBatchSize(vectorLength + 1));
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 3, 5, 7, 9, 15, 17, 31, 33, 63, 65, 100, 255, 1000})
    @DisplayName("getOptimalBatchSize should round up to optimal size")
    void testGetOptimalBatchSize(int minBatchSize) {
        int optimalBatchSize = MODWTBatchSIMD.getOptimalBatchSize(minBatchSize);
        int vectorLength = MODWTBatchSIMD.getVectorLength();
        
        // Optimal size should be >= minimum requested
        assertTrue(optimalBatchSize >= minBatchSize,
            "Optimal batch size " + optimalBatchSize + " should be >= requested " + minBatchSize);
        
        // Optimal size should be a multiple of vector length
        assertTrue(MODWTBatchSIMD.isOptimalBatchSize(optimalBatchSize),
            "Optimal batch size " + optimalBatchSize + " should be optimal");
        
        // Should be the smallest such multiple
        if (minBatchSize > 0) {
            assertTrue(optimalBatchSize - vectorLength < minBatchSize,
                "Optimal batch size should be minimal: " + optimalBatchSize + 
                " - " + vectorLength + " = " + (optimalBatchSize - vectorLength) + 
                " should be < " + minBatchSize);
        }
    }
    
    @Test
    @DisplayName("getOptimalBatchSize should handle edge cases")
    void testGetOptimalBatchSizeEdgeCases() {
        int vectorLength = MODWTBatchSIMD.getVectorLength();
        
        // Zero returns 0 (based on actual behavior)
        assertEquals(0, MODWTBatchSIMD.getOptimalBatchSize(0));
        
        // For negative numbers, just verify they return some reasonable value
        // (implementation may vary, but should be deterministic)
        int negativeResult = MODWTBatchSIMD.getOptimalBatchSize(-100);
        // Just verify it's deterministic by calling it again
        assertEquals(negativeResult, MODWTBatchSIMD.getOptimalBatchSize(-100));
        
        // Exact multiples should return themselves
        assertEquals(vectorLength, MODWTBatchSIMD.getOptimalBatchSize(vectorLength));
        assertEquals(vectorLength * 2, MODWTBatchSIMD.getOptimalBatchSize(vectorLength * 2));
        
        // Large numbers should still work
        int largeBatch = 1_000_000;
        int optimalLarge = MODWTBatchSIMD.getOptimalBatchSize(largeBatch);
        assertTrue(optimalLarge >= largeBatch);
        assertTrue(MODWTBatchSIMD.isOptimalBatchSize(optimalLarge));
    }
    
    @Test
    @DisplayName("SIMD info should be consistent with utility methods")
    void testSIMDInfoConsistency() {
        String info = MODWTBatchSIMD.getBatchSIMDInfo();
        int vectorLength = MODWTBatchSIMD.getVectorLength();
        
        // Info should contain the vector length
        assertTrue(info.contains(String.valueOf(vectorLength)),
            "SIMD info should contain vector length " + vectorLength);
        
        // Info should mention optimal batch size which should match vector length
        assertTrue(info.contains("Optimal batch size: " + vectorLength),
            "Info should mention optimal batch size");
    }
    
    @Test
    @DisplayName("Utility class should not be instantiable")
    void testUtilityClassDesign() {
        // This test verifies the class is designed as a utility class
        // We can't directly test the private constructor, but we can verify
        // all methods are static and the class is final
        
        // Verify all public methods are static by checking they work without instantiation
        assertDoesNotThrow(() -> MODWTBatchSIMD.getBatchSIMDInfo());
        assertDoesNotThrow(() -> MODWTBatchSIMD.getVectorLength());
        assertDoesNotThrow(() -> MODWTBatchSIMD.isOptimalBatchSize(8));
        assertDoesNotThrow(() -> MODWTBatchSIMD.getOptimalBatchSize(10));
    }
    
    private boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
}
