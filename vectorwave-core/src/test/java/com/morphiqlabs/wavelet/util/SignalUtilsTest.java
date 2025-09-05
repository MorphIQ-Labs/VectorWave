package com.morphiqlabs.wavelet.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for SignalUtils.
 */
@DisplayName("SignalUtils Tests")
class SignalUtilsTest {
    
    @Test
    @DisplayName("Should throw NullPointerException for null signal")
    void testCircularShiftNullSignal() {
        assertThrows(NullPointerException.class, 
            () -> SignalUtils.circularShift(null, 1),
            "Should throw NullPointerException for null signal");
    }
    
    @Test
    @DisplayName("Should throw IllegalArgumentException for empty signal")
    void testCircularShiftEmptySignal() {
        assertThrows(IllegalArgumentException.class, 
            () -> SignalUtils.circularShift(new double[0], 1),
            "Should throw IllegalArgumentException for empty signal");
    }
    
    @Test
    @DisplayName("Should handle zero shift")
    void testCircularShiftZero() {
        double[] signal = {1, 2, 3, 4, 5};
        double[] result = SignalUtils.circularShift(signal, 0);
        
        assertArrayEquals(signal, result, "Zero shift should return identical array");
        assertNotSame(signal, result, "Should return a new array");
    }
    
    @ParameterizedTest
    @DisplayName("Should handle positive shifts correctly")
    @CsvSource({
        "1, '5,1,2,3,4'",  // Shift right by 1
        "2, '4,5,1,2,3'",  // Shift right by 2
        "3, '3,4,5,1,2'",  // Shift right by 3
        "4, '2,3,4,5,1'",  // Shift right by 4
        "5, '1,2,3,4,5'",  // Full rotation
        "6, '5,1,2,3,4'"   // Shift > length (same as shift 1)
    })
    void testCircularShiftPositive(int shift, String expectedStr) {
        double[] signal = {1, 2, 3, 4, 5};
        double[] expected = parseArray(expectedStr);
        
        double[] result = SignalUtils.circularShift(signal, shift);
        
        assertArrayEquals(expected, result, 
            "Positive shift " + shift + " should produce correct result");
    }
    
    @ParameterizedTest
    @DisplayName("Should handle negative shifts correctly")
    @CsvSource({
        "-1, '2,3,4,5,1'",  // Shift left by 1
        "-2, '3,4,5,1,2'",  // Shift left by 2
        "-3, '4,5,1,2,3'",  // Shift left by 3
        "-4, '5,1,2,3,4'",  // Shift left by 4
        "-5, '1,2,3,4,5'",  // Full rotation
        "-6, '2,3,4,5,1'"   // Shift < -length (same as shift -1)
    })
    void testCircularShiftNegative(int shift, String expectedStr) {
        double[] signal = {1, 2, 3, 4, 5};
        double[] expected = parseArray(expectedStr);
        
        double[] result = SignalUtils.circularShift(signal, shift);
        
        assertArrayEquals(expected, result, 
            "Negative shift " + shift + " should produce correct result");
    }
    
    @Test
    @DisplayName("Should handle single element array")
    void testCircularShiftSingleElement() {
        double[] signal = {42.0};
        
        double[] result1 = SignalUtils.circularShift(signal, 0);
        double[] result2 = SignalUtils.circularShift(signal, 5);
        double[] result3 = SignalUtils.circularShift(signal, -3);
        
        assertArrayEquals(signal, result1, "Single element array should be unchanged");
        assertArrayEquals(signal, result2, "Single element array should be unchanged");
        assertArrayEquals(signal, result3, "Single element array should be unchanged");
    }
    
    @Test
    @DisplayName("Should handle power-of-two length arrays")
    void testCircularShiftPowerOfTwo() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] expected = {7, 8, 1, 2, 3, 4, 5, 6}; // Shift right by 2
        
        double[] result = SignalUtils.circularShift(signal, 2);
        
        assertArrayEquals(expected, result, 
            "Should correctly shift power-of-two length arrays");
    }
    
    @Test
    @DisplayName("Should match biorthogonal wavelet use case")
    void testCircularShiftBiorthogonalUseCase() {
        // Test the specific use case for biorthogonal wavelets
        // where a phase shift of 2 is commonly used
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i; // Sequential signal
        }
        
        double[] shifted = SignalUtils.circularShift(signal, 2);
        
        // Check first few elements
        assertEquals(62.0, shifted[0], "First element should be from end");
        assertEquals(63.0, shifted[1], "Second element should be from end");
        assertEquals(0.0, shifted[2], "Third element should be original first");
        assertEquals(1.0, shifted[3], "Fourth element should be original second");
        
        // Check last few elements
        assertEquals(60.0, shifted[62], "Second-to-last should be 60");
        assertEquals(61.0, shifted[63], "Last element should be 61");
    }
    
    @Test
    @DisplayName("Should verify direction convention explicitly")
    void testDirectionConvention() {
        double[] signal = {10, 20, 30, 40, 50};
        
        // Test positive shift (right) - elements move to higher indices
        double[] rightShift1 = SignalUtils.circularShift(signal, 1);
        assertEquals(50, rightShift1[0], "Index 0 should have element from index 4");
        assertEquals(10, rightShift1[1], "Index 1 should have element from index 0");
        assertEquals(20, rightShift1[2], "Index 2 should have element from index 1");
        assertEquals(30, rightShift1[3], "Index 3 should have element from index 2");
        assertEquals(40, rightShift1[4], "Index 4 should have element from index 3");
        
        // Test negative shift (left) - elements move to lower indices
        double[] leftShift1 = SignalUtils.circularShift(signal, -1);
        assertEquals(20, leftShift1[0], "Index 0 should have element from index 1");
        assertEquals(30, leftShift1[1], "Index 1 should have element from index 2");
        assertEquals(40, leftShift1[2], "Index 2 should have element from index 3");
        assertEquals(50, leftShift1[3], "Index 3 should have element from index 4");
        assertEquals(10, leftShift1[4], "Index 4 should have element from index 0");
    }
    
    @Test
    @DisplayName("Should handle various shift normalizations correctly")
    void testShiftNormalization() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        
        // Test that equivalent shifts produce the same result
        double[] shift2 = SignalUtils.circularShift(signal, 2);
        double[] shift10 = SignalUtils.circularShift(signal, 10);  // 10 % 8 = 2
        double[] shiftNeg6 = SignalUtils.circularShift(signal, -6); // -6 → 2
        
        assertArrayEquals(shift2, shift10, "shift 10 should equal shift 2");
        assertArrayEquals(shift2, shiftNeg6, "shift -6 should equal shift 2");
        
        // Test full rotations
        double[] shift0 = SignalUtils.circularShift(signal, 0);
        double[] shift8 = SignalUtils.circularShift(signal, 8);
        double[] shiftNeg8 = SignalUtils.circularShift(signal, -8);
        double[] shift16 = SignalUtils.circularShift(signal, 16);
        
        assertArrayEquals(signal, shift0, "Zero shift should return identical array");
        assertArrayEquals(signal, shift8, "Full rotation should return identical array");
        assertArrayEquals(signal, shiftNeg8, "Negative full rotation should return identical array");
        assertArrayEquals(signal, shift16, "Double rotation should return identical array");
    }
    
    private double[] parseArray(String str) {
        String[] parts = str.split(",");
        double[] result = new double[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Double.parseDouble(parts[i]);
        }
        return result;
    }
}