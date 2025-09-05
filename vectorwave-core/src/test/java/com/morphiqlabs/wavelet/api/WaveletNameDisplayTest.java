package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that WaveletName displays human-readable names.
 * Addresses the issue where consuming applications see raw enum names
 * instead of proper display names.
 */
class WaveletNameDisplayTest {
    
    @Test
    @DisplayName("toString() returns display name instead of code")
    void testToStringReturnsDisplayName() {
        // Test specific wavelets mentioned in the issue
        assertEquals("Symlet 11", WaveletName.SYM11.toString());
        assertEquals("Symlet 12", WaveletName.SYM12.toString());
        assertEquals("Symlet 13", WaveletName.SYM13.toString());
        assertEquals("Symlet 14", WaveletName.SYM14.toString());
        assertEquals("Symlet 15", WaveletName.SYM15.toString());
        assertEquals("Symlet 16", WaveletName.SYM16.toString());
        assertEquals("Symlet 17", WaveletName.SYM17.toString());
        assertEquals("Symlet 18", WaveletName.SYM18.toString());
        assertEquals("Symlet 19", WaveletName.SYM19.toString());
        
        // Test other wavelet types for consistency
        assertEquals("Daubechies 2", WaveletName.DB2.toString());
        assertEquals("Daubechies 4", WaveletName.DB4.toString());
        assertEquals("Morlet wavelet", WaveletName.MORLET.toString());
    }
    
    @Test
    @DisplayName("getDisplayName() returns same as toString()")
    void testGetDisplayNameConsistency() {
        for (WaveletName wavelet : WaveletName.values()) {
            assertEquals(wavelet.toString(), wavelet.getDisplayName(),
                "getDisplayName() should return same as toString() for " + wavelet.name());
        }
    }
    
    @Test
    @DisplayName("getDisplayName() returns same as getDescription()")
    void testGetDisplayNameMatchesDescription() {
        for (WaveletName wavelet : WaveletName.values()) {
            assertEquals(wavelet.getDescription(), wavelet.getDisplayName(),
                "getDisplayName() should return same as getDescription() for " + wavelet.name());
        }
    }
    
    @Test
    @DisplayName("Symlet wavelets show consistent display format")
    void testSymletDisplayFormat() {
        List<WaveletName> symlets = WaveletRegistry.getSymletWavelets();
        
        for (WaveletName symlet : symlets) {
            String displayName = symlet.toString();
            assertTrue(displayName.startsWith("Symlet "),
                "Symlet wavelet " + symlet.name() + " should have display name starting with 'Symlet ', got: " + displayName);
            
            // Verify it contains a number
            assertTrue(displayName.matches("Symlet \\d+"),
                "Symlet wavelet " + symlet.name() + " should match pattern 'Symlet N', got: " + displayName);
        }
    }
    
    @Test
    @DisplayName("No wavelet toString() returns raw enum name")
    void testNoRawEnumNamesInToString() {
        for (WaveletName wavelet : WaveletName.values()) {
            String enumName = wavelet.name();
            String displayName = wavelet.toString();
            
            assertNotEquals(enumName, displayName,
                "Wavelet " + enumName + " toString() should not return raw enum name");
        }
    }
    
    @Test
    @DisplayName("Code property still accessible for technical use")
    void testCodeStillAvailable() {
        // Ensure getCode() still works for technical/API usage
        assertEquals("sym11", WaveletName.SYM11.getCode());
        assertEquals("sym12", WaveletName.SYM12.getCode());
        assertEquals("db4", WaveletName.DB4.getCode());
        assertEquals("morl", WaveletName.MORLET.getCode());
    }
}