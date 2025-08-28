package com.morphiqlabs.wavelet.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for PowerOf2Utils utility class.
 */
class PowerOf2UtilsTest {
    
    @Test
    @DisplayName("nextPowerOf2 should handle basic cases correctly")
    void testNextPowerOf2BasicCases() {
        assertEquals(1, PowerOf2Utils.nextPowerOf2(0));
        assertEquals(1, PowerOf2Utils.nextPowerOf2(1));
        assertEquals(2, PowerOf2Utils.nextPowerOf2(2));
        assertEquals(4, PowerOf2Utils.nextPowerOf2(3));
        assertEquals(4, PowerOf2Utils.nextPowerOf2(4));
        assertEquals(8, PowerOf2Utils.nextPowerOf2(5));
        assertEquals(8, PowerOf2Utils.nextPowerOf2(6));
        assertEquals(8, PowerOf2Utils.nextPowerOf2(7));
        assertEquals(8, PowerOf2Utils.nextPowerOf2(8));
        assertEquals(16, PowerOf2Utils.nextPowerOf2(9));
    }
    
    @Test
    @DisplayName("nextPowerOf2 should handle large values")
    void testNextPowerOf2LargeValues() {
        assertEquals(1024, PowerOf2Utils.nextPowerOf2(1023));
        assertEquals(1024, PowerOf2Utils.nextPowerOf2(1024));
        assertEquals(2048, PowerOf2Utils.nextPowerOf2(1025));
        
        assertEquals(1 << 20, PowerOf2Utils.nextPowerOf2((1 << 20) - 1));
        assertEquals(1 << 20, PowerOf2Utils.nextPowerOf2(1 << 20));
        assertEquals(1 << 21, PowerOf2Utils.nextPowerOf2((1 << 20) + 1));
    }
    
    @Test
    @DisplayName("nextPowerOf2 should handle negative input")
    void testNextPowerOf2NegativeInput() {
        assertEquals(1, PowerOf2Utils.nextPowerOf2(-1));
        assertEquals(1, PowerOf2Utils.nextPowerOf2(-100));
    }
    
    @Test
    @DisplayName("nextPowerOf2 should throw exception for overflow")
    void testNextPowerOf2Overflow() {
        assertThrows(IllegalArgumentException.class, 
            () -> PowerOf2Utils.nextPowerOf2((1 << 30) + 1));
    }
    
    @Test
    @DisplayName("isPowerOf2 should identify powers of 2 correctly")
    void testIsPowerOf2() {
        // Powers of 2
        assertTrue(PowerOf2Utils.isPowerOf2(1));
        assertTrue(PowerOf2Utils.isPowerOf2(2));
        assertTrue(PowerOf2Utils.isPowerOf2(4));
        assertTrue(PowerOf2Utils.isPowerOf2(8));
        assertTrue(PowerOf2Utils.isPowerOf2(16));
        assertTrue(PowerOf2Utils.isPowerOf2(1024));
        assertTrue(PowerOf2Utils.isPowerOf2(1 << 20));
        
        // Not powers of 2
        assertFalse(PowerOf2Utils.isPowerOf2(0));
        assertFalse(PowerOf2Utils.isPowerOf2(-1));
        assertFalse(PowerOf2Utils.isPowerOf2(3));
        assertFalse(PowerOf2Utils.isPowerOf2(5));
        assertFalse(PowerOf2Utils.isPowerOf2(6));
        assertFalse(PowerOf2Utils.isPowerOf2(7));
        assertFalse(PowerOf2Utils.isPowerOf2(1023));
    }
    
    @Test
    @DisplayName("log2 should compute correct logarithms")
    void testLog2() {
        assertEquals(0, PowerOf2Utils.log2(1));
        assertEquals(1, PowerOf2Utils.log2(2));
        assertEquals(2, PowerOf2Utils.log2(4));
        assertEquals(3, PowerOf2Utils.log2(8));
        assertEquals(10, PowerOf2Utils.log2(1024));
        assertEquals(20, PowerOf2Utils.log2(1 << 20));
        
        // For non-powers of 2, returns position of highest bit
        assertEquals(1, PowerOf2Utils.log2(3)); // binary 11
        assertEquals(2, PowerOf2Utils.log2(5)); // binary 101
        assertEquals(2, PowerOf2Utils.log2(7)); // binary 111
    }
    
    @Test
    @DisplayName("log2 should throw exception for non-positive input")
    void testLog2NonPositive() {
        assertThrows(IllegalArgumentException.class, () -> PowerOf2Utils.log2(0));
        assertThrows(IllegalArgumentException.class, () -> PowerOf2Utils.log2(-1));
    }
    
    @Test
    @DisplayName("previousPowerOf2 should find floor power of 2")
    void testPreviousPowerOf2() {
        assertEquals(1, PowerOf2Utils.previousPowerOf2(1));
        assertEquals(2, PowerOf2Utils.previousPowerOf2(2));
        assertEquals(2, PowerOf2Utils.previousPowerOf2(3));
        assertEquals(4, PowerOf2Utils.previousPowerOf2(4));
        assertEquals(4, PowerOf2Utils.previousPowerOf2(5));
        assertEquals(4, PowerOf2Utils.previousPowerOf2(6));
        assertEquals(4, PowerOf2Utils.previousPowerOf2(7));
        assertEquals(8, PowerOf2Utils.previousPowerOf2(8));
        assertEquals(8, PowerOf2Utils.previousPowerOf2(15));
        assertEquals(16, PowerOf2Utils.previousPowerOf2(16));
        assertEquals(16, PowerOf2Utils.previousPowerOf2(31));
        assertEquals(1024, PowerOf2Utils.previousPowerOf2(2047));
    }
    
    @Test
    @DisplayName("previousPowerOf2 should throw exception for non-positive input")
    void testPreviousPowerOf2NonPositive() {
        assertThrows(IllegalArgumentException.class, () -> PowerOf2Utils.previousPowerOf2(0));
        assertThrows(IllegalArgumentException.class, () -> PowerOf2Utils.previousPowerOf2(-1));
    }
    
    @Test
    @DisplayName("moduloPowerOf2 should compute modulo efficiently")
    void testModuloPowerOf2() {
        // x % 4
        assertEquals(0, PowerOf2Utils.moduloPowerOf2(0, 4));
        assertEquals(1, PowerOf2Utils.moduloPowerOf2(1, 4));
        assertEquals(2, PowerOf2Utils.moduloPowerOf2(2, 4));
        assertEquals(3, PowerOf2Utils.moduloPowerOf2(3, 4));
        assertEquals(0, PowerOf2Utils.moduloPowerOf2(4, 4));
        assertEquals(1, PowerOf2Utils.moduloPowerOf2(5, 4));
        
        // x % 8
        assertEquals(7, PowerOf2Utils.moduloPowerOf2(15, 8));
        assertEquals(0, PowerOf2Utils.moduloPowerOf2(16, 8));
        
        // x % 1024
        assertEquals(123, PowerOf2Utils.moduloPowerOf2(1147, 1024)); // 1147 = 1024 + 123
    }
    
    @Test
    @DisplayName("moduloPowerOf2 should throw exception for non-power-of-2 divisor")
    void testModuloPowerOf2InvalidDivisor() {
        assertThrows(IllegalArgumentException.class, () -> PowerOf2Utils.moduloPowerOf2(10, 3));
        assertThrows(IllegalArgumentException.class, () -> PowerOf2Utils.moduloPowerOf2(10, 5));
        assertThrows(IllegalArgumentException.class, () -> PowerOf2Utils.moduloPowerOf2(10, 0));
    }
    
    @Test
    @DisplayName("Verify nextPowerOf2 using alternative bit manipulation formula")
    void testNextPowerOf2AlternativeFormula() {
        // Verify our implementation against an alternative bit manipulation approach
        // This provides an independent verification of correctness
        for (int n = 1; n <= 1000; n++) {
            // Alternative formula: shift 1 left by the bit position after the highest bit in (n-1)
            int expected = 1 << (32 - Integer.numberOfLeadingZeros(n - 1));
            int actual = PowerOf2Utils.nextPowerOf2(n);
            assertEquals(expected, actual, "Mismatch for n=" + n);
        }
    }
}