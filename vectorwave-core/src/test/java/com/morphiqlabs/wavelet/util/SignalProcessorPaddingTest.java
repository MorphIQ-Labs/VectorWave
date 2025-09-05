package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.cwt.ComplexNumber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SignalProcessor padding functionality.
 */
class SignalProcessorPaddingTest {
    
    @Test
    @DisplayName("Should pad non-power-of-2 arrays correctly")
    void testPadToPowerOf2() {
        // Test case 1: Length 5 should pad to 8
        ComplexNumber[] data5 = new ComplexNumber[5];
        for (int i = 0; i < 5; i++) {
            data5[i] = new ComplexNumber(i, 0);
        }
        
        ComplexNumber[] padded5 = SignalProcessor.padToPowerOf2(data5);
        assertEquals(8, padded5.length);
        
        // Verify original data is preserved
        for (int i = 0; i < 5; i++) {
            assertEquals(data5[i], padded5[i]);
        }
        
        // Verify padding is zeros
        for (int i = 5; i < 8; i++) {
            assertEquals(ComplexNumber.ZERO, padded5[i]);
        }
        
        // Test case 2: Length 10 should pad to 16
        ComplexNumber[] data10 = new ComplexNumber[10];
        for (int i = 0; i < 10; i++) {
            data10[i] = new ComplexNumber(i, i);
        }
        
        ComplexNumber[] padded10 = SignalProcessor.padToPowerOf2(data10);
        assertEquals(16, padded10.length);
        
        // Test case 3: Length 100 should pad to 128
        ComplexNumber[] data100 = new ComplexNumber[100];
        for (int i = 0; i < 100; i++) {
            data100[i] = new ComplexNumber(Math.random(), Math.random());
        }
        
        ComplexNumber[] padded100 = SignalProcessor.padToPowerOf2(data100);
        assertEquals(128, padded100.length);
    }
    
    @Test
    @DisplayName("Should not pad arrays that are already power of 2")
    void testNoPaddingNeeded() {
        // Test various power-of-2 sizes
        int[] sizes = {1, 2, 4, 8, 16, 32, 64, 128};
        
        for (int size : sizes) {
            ComplexNumber[] data = new ComplexNumber[size];
            for (int i = 0; i < size; i++) {
                data[i] = new ComplexNumber(i, 0);
            }
            
            ComplexNumber[] result = SignalProcessor.padToPowerOf2(data);
            assertSame(data, result, "Should return same array for power-of-2 size " + size);
        }
    }
    
    @Test
    @DisplayName("Should handle null input")
    void testNullInput() {
        assertThrows(IllegalArgumentException.class, 
            () -> SignalProcessor.padToPowerOf2(null),
            "Should throw exception for null input");
    }
    
    @Test
    @DisplayName("Should work with FFT after padding")
    void testFFTWithPadding() {
        // Create non-power-of-2 data with DC offset
        ComplexNumber[] data = new ComplexNumber[10];
        for (int i = 0; i < 10; i++) {
            // Add DC offset of 1.0 to ensure non-zero DC component
            data[i] = new ComplexNumber(1.0 + Math.sin(2 * Math.PI * i / 10), 0);
        }
        
        // Pad and perform FFT
        ComplexNumber[] padded = SignalProcessor.padToPowerOf2(data);
        assertDoesNotThrow(() -> SignalProcessor.fft(padded));
        
        // Verify FFT was performed (DC component should be non-zero due to offset)
        assertTrue(padded[0].magnitude() > 0.5, 
            "DC component should be significant due to DC offset");
    }
    
    @Test
    @DisplayName("Error messages should suggest correct padding")
    void testErrorMessageSuggestion() {
        // Test with length 10
        ComplexNumber[] data = new ComplexNumber[10];
        for (int i = 0; i < 10; i++) {
            data[i] = ComplexNumber.ZERO;
        }
        
        Exception exception = assertThrows(IllegalArgumentException.class,
            () -> SignalProcessor.fft(data));
        
        String message = exception.getMessage();
        assertTrue(message.contains("must be a power of 2"));
        assertTrue(message.contains("got: 10"));
        assertTrue(message.contains("Consider padding your data to length 16"));
    }
}