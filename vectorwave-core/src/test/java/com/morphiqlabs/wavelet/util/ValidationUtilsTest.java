package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.exception.InvalidSignalException;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ValidationUtils.
 */
@DisplayName("ValidationUtils Tests")
class ValidationUtilsTest {
    
    // === validateSignal Tests ===
    
    @Test
    @DisplayName("validateSignal should accept valid signal")
    void testValidateSignalValid() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        assertDoesNotThrow(() -> ValidationUtils.validateSignal(signal, "signal"));
    }
    
    @Test
    @DisplayName("validateSignal should reject null signal")
    void testValidateSignalNull() {
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> ValidationUtils.validateSignal(null, "signal"));
        
        assertTrue(exception.getMessage().contains("null"));
    }
    
    @Test
    @DisplayName("validateSignal should reject empty signal")
    void testValidateSignalEmpty() {
        double[] emptySignal = {};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> ValidationUtils.validateSignal(emptySignal, "signal"));
        
        assertTrue(exception.getMessage().contains("empty"));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {3, 5, 7, 9, 15, 17, 31, 33, 63, 65, 127, 129})
    @DisplayName("validateSignal should reject non-power-of-2 lengths")
    void testValidateSignalNonPowerOfTwo(int length) {
        double[] signal = new double[length];
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> ValidationUtils.validateSignal(signal, "signal"));
        
        assertTrue(exception.getMessage().contains(String.valueOf(length)));
        assertTrue(exception.getMessage().contains("power of two"));
    }
    
    @Test
    @DisplayName("validateSignal should reject signal with NaN")
    void testValidateSignalWithNaN() {
        double[] signal = {1.0, 2.0, Double.NaN, 4.0};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> ValidationUtils.validateSignal(signal, "signal"));
        
        assertTrue(exception.getMessage().contains("NaN"));
        assertTrue(exception.getMessage().contains("index 2"));
    }
    
    @Test
    @DisplayName("validateSignal should reject signal with positive infinity")
    void testValidateSignalWithPositiveInfinity() {
        double[] signal = {1.0, 2.0, 3.0, Double.POSITIVE_INFINITY, 5.0, 6.0, 7.0, 8.0};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> ValidationUtils.validateSignal(signal, "signal"));
        
        assertTrue(exception.getMessage().contains("infinity"));
        assertTrue(exception.getMessage().contains("index 3"));
    }
    
    @Test
    @DisplayName("validateSignal should reject signal with negative infinity")
    void testValidateSignalWithNegativeInfinity() {
        double[] signal = {1.0, Double.NEGATIVE_INFINITY};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> ValidationUtils.validateSignal(signal, "signal"));
        
        assertTrue(exception.getMessage().contains("infinity"));
        assertTrue(exception.getMessage().contains("index 1"));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {2, 4, 8, 16, 32, 64, 128, 256, 512, 1024})
    @DisplayName("validateSignal should accept various power-of-2 lengths")
    void testValidateSignalPowerOfTwo(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = i + 1.0;
        }
        
        assertDoesNotThrow(() -> ValidationUtils.validateSignal(signal, "signal"));
    }
    
    // === validateNotNullOrEmpty Tests ===
    
    @Test
    @DisplayName("validateNotNullOrEmpty should accept non-empty array")
    void testValidateNotNullOrEmptyValid() {
        double[] array = {1.0, 2.0, 3.0};
        
        assertDoesNotThrow(() -> 
            ValidationUtils.validateNotNullOrEmpty(array, "testArray"));
    }
    
    @Test
    @DisplayName("validateNotNullOrEmpty should reject null array")
    void testValidateNotNullOrEmptyNull() {
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> ValidationUtils.validateNotNullOrEmpty(null, "testArray"));
        
        assertTrue(exception.getMessage().contains("testArray"));
        assertTrue(exception.getMessage().contains("null"));
    }
    
    @Test
    @DisplayName("validateNotNullOrEmpty should reject empty array")
    void testValidateNotNullOrEmptyEmpty() {
        double[] emptyArray = {};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> ValidationUtils.validateNotNullOrEmpty(emptyArray, "testArray"));
        
        assertTrue(exception.getMessage().contains("testArray"));
        assertTrue(exception.getMessage().contains("empty"));
    }
    
    // === validateMatchingLengths Tests ===
    
    @Test
    @DisplayName("validateMatchingLengths should accept arrays with same length")
    void testValidateMatchingLengthsValid() {
        double[] array1 = {1.0, 2.0, 3.0, 4.0};
        double[] array2 = {5.0, 6.0, 7.0, 8.0};
        
        assertDoesNotThrow(() -> 
            ValidationUtils.validateMatchingLengths(array1, array2));
    }
    
    @Test
    @DisplayName("validateMatchingLengths should reject arrays with different lengths")
    void testValidateMatchingLengthsDifferent() {
        double[] array1 = {1.0, 2.0, 3.0};
        double[] array2 = {4.0, 5.0};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> ValidationUtils.validateMatchingLengths(array1, array2));
        
        assertTrue(exception.getMessage().contains("Approximation and detail coefficient arrays must have the same length"));
        assertTrue(exception.getMessage().contains("approximation length: 3"));
        assertTrue(exception.getMessage().contains("detail length: 2"));
    }
    
    @Test
    @DisplayName("validateMatchingLengths with empty arrays")
    void testValidateMatchingLengthsEmpty() {
        double[] array1 = {};
        double[] array2 = {};
        
        // Empty arrays have matching lengths (both 0)
        assertDoesNotThrow(() -> 
            ValidationUtils.validateMatchingLengths(array1, array2));
    }
    
    // === validateFiniteValues Tests ===
    
    @Test
    @DisplayName("validateFiniteValues should accept array with finite values")
    void testValidateFiniteValuesValid() {
        double[] values = {1.0, -2.5, 0.0, 1e10, -1e-10, Double.MIN_VALUE, -Double.MAX_VALUE};
        
        assertDoesNotThrow(() -> 
            ValidationUtils.validateFiniteValues(values, "testValues"));
    }
    
    @Test
    @DisplayName("validateFiniteValues should reject array with NaN")
    void testValidateFiniteValuesNaN() {
        double[] values = {1.0, 2.0, Double.NaN, 4.0};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> ValidationUtils.validateFiniteValues(values, "testValues"));
        
        assertTrue(exception.getMessage().contains("testValues"));
        assertTrue(exception.getMessage().contains("NaN"));
        assertTrue(exception.getMessage().contains("index 2"));
    }
    
    @Test
    @DisplayName("validateFiniteValues should reject array with infinity")
    void testValidateFiniteValuesInfinity() {
        double[] values = {1.0, Double.POSITIVE_INFINITY};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> ValidationUtils.validateFiniteValues(values, "testValues"));
        
        assertTrue(exception.getMessage().contains("testValues"));
        assertTrue(exception.getMessage().contains("infinity"));
        assertTrue(exception.getMessage().contains("index 1"));
    }
    
    @Test
    @DisplayName("validateFiniteValues with multiple non-finite values should report first")
    void testValidateFiniteValuesMultipleNonFinite() {
        double[] values = {1.0, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> ValidationUtils.validateFiniteValues(values, "testValues"));
        
        // Should report the first non-finite value (at index 1)
        assertTrue(exception.getMessage().contains("index 1"));
    }
    
    // === isPowerOfTwo Tests ===
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096})
    @DisplayName("isPowerOfTwo should return true for powers of 2")
    void testIsPowerOfTwoTrue(int value) {
        assertTrue(ValidationUtils.isPowerOfTwo(value));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {0, 3, 5, 6, 7, 9, 10, 15, 17, 31, 33, 63, 65, 127, 129})
    @DisplayName("isPowerOfTwo should return false for non-powers of 2")
    void testIsPowerOfTwoFalse(int value) {
        assertFalse(ValidationUtils.isPowerOfTwo(value));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {-1, -2, -4, -8, -16})
    @DisplayName("isPowerOfTwo should return false for negative numbers")
    void testIsPowerOfTwoNegative(int value) {
        assertFalse(ValidationUtils.isPowerOfTwo(value));
    }
    
    @Test
    @DisplayName("isPowerOfTwo edge cases")
    void testIsPowerOfTwoEdgeCases() {
        assertFalse(ValidationUtils.isPowerOfTwo(0));
        assertTrue(ValidationUtils.isPowerOfTwo(1));
        assertFalse(ValidationUtils.isPowerOfTwo(Integer.MAX_VALUE));
        // 1073741824 is 2^30
        assertTrue(ValidationUtils.isPowerOfTwo(1073741824));
    }
    
    // === getMaxSafePowerOfTwo Tests ===
    
    @Test
    @DisplayName("getMaxSafePowerOfTwo should return 2^30")
    void testGetMaxSafePowerOfTwo() {
        int maxSafe = ValidationUtils.getMaxSafePowerOfTwo();
        assertEquals(1073741824, maxSafe); // 2^30
        assertTrue(ValidationUtils.isPowerOfTwo(maxSafe));
    }
    
    // === wouldNextPowerOfTwoOverflow Tests ===
    
    @Test
    @DisplayName("wouldNextPowerOfTwoOverflow should return false for safe values")
    void testWouldNextPowerOfTwoOverflowSafe() {
        assertFalse(ValidationUtils.wouldNextPowerOfTwoOverflow(1));
        assertFalse(ValidationUtils.wouldNextPowerOfTwoOverflow(512));
        assertFalse(ValidationUtils.wouldNextPowerOfTwoOverflow(1073741824)); // 2^30
    }
    
    @Test
    @DisplayName("wouldNextPowerOfTwoOverflow should return true for values > 2^30")
    void testWouldNextPowerOfTwoOverflowTrue() {
        assertTrue(ValidationUtils.wouldNextPowerOfTwoOverflow(1073741825)); // 2^30 + 1
        assertTrue(ValidationUtils.wouldNextPowerOfTwoOverflow(Integer.MAX_VALUE));
    }
    
    // === nextPowerOfTwo Tests ===
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024})
    @DisplayName("nextPowerOfTwo should return same value for powers of 2")
    void testNextPowerOfTwoSame(int value) {
        assertEquals(value, ValidationUtils.nextPowerOfTwo(value));
    }
    
    @Test
    @DisplayName("nextPowerOfTwo should round up for non-powers of 2")
    void testNextPowerOfTwoRoundUp() {
        assertEquals(4, ValidationUtils.nextPowerOfTwo(3));
        assertEquals(8, ValidationUtils.nextPowerOfTwo(5));
        assertEquals(8, ValidationUtils.nextPowerOfTwo(6));
        assertEquals(8, ValidationUtils.nextPowerOfTwo(7));
        assertEquals(16, ValidationUtils.nextPowerOfTwo(9));
        assertEquals(32, ValidationUtils.nextPowerOfTwo(17));
        assertEquals(64, ValidationUtils.nextPowerOfTwo(33));
        assertEquals(128, ValidationUtils.nextPowerOfTwo(65));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {0, -1, -10, -100})
    @DisplayName("nextPowerOfTwo should throw exception for non-positive values")
    void testNextPowerOfTwoNonPositive(int value) {
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
            () -> ValidationUtils.nextPowerOfTwo(value));
        
        assertTrue(exception.getMessage().contains("positive"));
    }
    
    @Test
    @DisplayName("nextPowerOfTwo should throw exception for values that would overflow")
    void testNextPowerOfTwoOverflow() {
        int tooLarge = 1073741825; // 2^30 + 1
        
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
            () -> ValidationUtils.nextPowerOfTwo(tooLarge));
        
        assertTrue(exception.getMessage().contains("exceeds maximum safe power"));
    }
    
    @Test
    @DisplayName("nextPowerOfTwo edge cases")
    void testNextPowerOfTwoEdgeCases() {
        assertEquals(1, ValidationUtils.nextPowerOfTwo(1));
        assertEquals(1073741824, ValidationUtils.nextPowerOfTwo(1073741824)); // 2^30
        assertEquals(1073741824, ValidationUtils.nextPowerOfTwo(536870913)); // Just over 2^29
    }
}