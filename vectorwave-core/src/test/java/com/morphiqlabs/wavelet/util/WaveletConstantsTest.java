package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for WaveletConstants.
 */
@DisplayName("WaveletConstants Tests")
class WaveletConstantsTest {
    
    @Test
    @DisplayName("MAX_SAFE_POWER_OF_TWO should be 2^30")
    void testMaxSafePowerOfTwo() {
        // 2^30 is the maximum safe power of 2 for array sizes to avoid integer overflow
        final int EXPECTED_MAX_POWER_OF_TWO = 1 << 30;  // 2^30 = 1,073,741,824
        
        assertEquals(EXPECTED_MAX_POWER_OF_TWO, WaveletConstants.MAX_SAFE_POWER_OF_TWO);
        
        // Verify it's actually 2^30
        assertEquals(Math.pow(2, 30), WaveletConstants.MAX_SAFE_POWER_OF_TWO);
    }
    
    // === calculateNextPowerOfTwo Tests ===
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024})
    @DisplayName("calculateNextPowerOfTwo should return same value for powers of 2")
    void testCalculateNextPowerOfTwoSame(int value) {
        assertEquals(value, WaveletConstants.calculateNextPowerOfTwo(value));
    }
    
    @Test
    @DisplayName("calculateNextPowerOfTwo should round up for non-powers of 2")
    void testCalculateNextPowerOfTwoRoundUp() {
        assertEquals(4, WaveletConstants.calculateNextPowerOfTwo(3));
        assertEquals(8, WaveletConstants.calculateNextPowerOfTwo(5));
        assertEquals(8, WaveletConstants.calculateNextPowerOfTwo(6));
        assertEquals(8, WaveletConstants.calculateNextPowerOfTwo(7));
        assertEquals(16, WaveletConstants.calculateNextPowerOfTwo(9));
        assertEquals(16, WaveletConstants.calculateNextPowerOfTwo(15));
        assertEquals(32, WaveletConstants.calculateNextPowerOfTwo(17));
        assertEquals(32, WaveletConstants.calculateNextPowerOfTwo(31));
        assertEquals(64, WaveletConstants.calculateNextPowerOfTwo(33));
        assertEquals(128, WaveletConstants.calculateNextPowerOfTwo(65));
        assertEquals(256, WaveletConstants.calculateNextPowerOfTwo(129));
    }
    
    @Test
    @DisplayName("calculateNextPowerOfTwo edge cases near 2^30")
    void testCalculateNextPowerOfTwoNearMax() {
        // 2^29 = 536870912
        assertEquals(536870912, WaveletConstants.calculateNextPowerOfTwo(536870912));
        assertEquals(1073741824, WaveletConstants.calculateNextPowerOfTwo(536870913)); // 2^29 + 1
        
        // 2^30 = 1073741824
        assertEquals(1073741824, WaveletConstants.calculateNextPowerOfTwo(1073741824));
    }
    
    @Test
    @DisplayName("calculateNextPowerOfTwo should throw for values > 2^30")
    void testCalculateNextPowerOfTwoOverflow() {
        int tooLarge = 1073741825; // 2^30 + 1
        
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
            () -> WaveletConstants.calculateNextPowerOfTwo(tooLarge));
        
        assertTrue(exception.getMessage().contains("exceeds maximum"));
    }
    
    @Test
    @DisplayName("calculateNextPowerOfTwo should handle 1")
    void testCalculateNextPowerOfTwoOne() {
        assertEquals(1, WaveletConstants.calculateNextPowerOfTwo(1));
    }
    
    @Test
    @DisplayName("calculateNextPowerOfTwo performance for various ranges")
    void testCalculateNextPowerOfTwoRanges() {
        // Small values
        for (int i = 1; i <= 16; i++) {
            int result = WaveletConstants.calculateNextPowerOfTwo(i);
            assertTrue(ValidationUtils.isPowerOfTwo(result));
            assertTrue(result >= i);
            if (i > 1 && ValidationUtils.isPowerOfTwo(i)) {
                assertEquals(i, result);
            }
        }
        
        // Medium values
        int[] testValues = {100, 200, 500, 1000, 5000, 10000};
        int[] expectedResults = {128, 256, 512, 1024, 8192, 16384};
        
        for (int i = 0; i < testValues.length; i++) {
            assertEquals(expectedResults[i], 
                WaveletConstants.calculateNextPowerOfTwo(testValues[i]));
        }
        
        // Large values
        assertEquals(1 << 20, WaveletConstants.calculateNextPowerOfTwo(1_000_000)); // 2^20 = 1,048,576
        assertEquals(1 << 24, WaveletConstants.calculateNextPowerOfTwo(10_000_000)); // 2^24 = 16,777,216
        assertEquals(1 << 27, WaveletConstants.calculateNextPowerOfTwo(100_000_000)); // 2^27 = 134,217,728
    }
    
    @Test
    @DisplayName("Utility class should not be instantiable")
    void testPrivateConstructor() throws Exception {
        // Use reflection to verify private constructor
        java.lang.reflect.Constructor<WaveletConstants> constructor = 
            WaveletConstants.class.getDeclaredConstructor();
        
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
            "Constructor should be private");
    }
}