package com.morphiqlabs.wavelet.extensions.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for VectorLengthValidator.
 * Tests validation logic, bounds checking, and error messages.
 */
public class VectorLengthValidatorTest {
    
    @Test
    @DisplayName("Test valid vector lengths")
    void testValidVectorLengths() {
        // Test various valid vector lengths
        int[] validLengths = {1, 2, 4, 8, 16, 32, 64};
        
        for (int length : validLengths) {
            assertDoesNotThrow(() -> 
                VectorLengthValidator.validateVectorLength(length, "TestContext"),
                "Length " + length + " should be valid"
            );
        }
    }
    
    @Test
    @DisplayName("Test minimum vector length boundary")
    void testMinimumVectorLength() {
        // Minimum valid length is 1
        assertDoesNotThrow(() -> 
            VectorLengthValidator.validateVectorLength(1, "TestContext"),
            "Length 1 should be valid"
        );
        
        // Below minimum should throw
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> VectorLengthValidator.validateVectorLength(0, "TestContext"),
            "Length 0 should be invalid"
        );
        
        assertTrue(exception.getMessage().contains("Unexpected vector length: 0"));
        assertTrue(exception.getMessage().contains("TestContext"));
    }
    
    @Test
    @DisplayName("Test maximum vector length boundary")
    void testMaximumVectorLength() {
        int maxLength = VectorLengthValidator.getMaxVectorLength();
        
        // Maximum valid length
        assertDoesNotThrow(() -> 
            VectorLengthValidator.validateVectorLength(maxLength, "TestContext"),
            "Max length should be valid"
        );
        
        // Above maximum should throw
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> VectorLengthValidator.validateVectorLength(maxLength + 1, "TestContext"),
            "Length above max should be invalid"
        );
        
        assertTrue(exception.getMessage().contains("Unexpected vector length: " + (maxLength + 1)));
        assertTrue(exception.getMessage().contains("expected 1-" + maxLength));
    }
    
    @Test
    @DisplayName("Test negative vector lengths")
    void testNegativeVectorLengths() {
        int[] negativeLengths = {-1, -10, -100, Integer.MIN_VALUE};
        
        for (int length : negativeLengths) {
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> VectorLengthValidator.validateVectorLength(length, "TestContext"),
                "Negative length " + length + " should be invalid"
            );
            
            assertTrue(exception.getMessage().contains("Unexpected vector length: " + length));
        }
    }
    
    @Test
    @DisplayName("Test error message format")
    void testErrorMessageFormat() {
        String context = "MySpecificContext";
        int invalidLength = 100; // Assuming this is above max
        
        IllegalStateException exception = assertThrows(
            IllegalStateException.class,
            () -> VectorLengthValidator.validateVectorLength(invalidLength, context)
        );
        
        String message = exception.getMessage();
        
        // Check all expected parts of the error message
        assertTrue(message.contains(context), "Should contain context");
        assertTrue(message.contains("Unexpected vector length: " + invalidLength), 
            "Should contain the invalid length");
        assertTrue(message.contains("expected 1-"), "Should contain expected range");
        assertTrue(message.contains("Platform:"), "Should contain platform info");
        assertTrue(message.contains(System.getProperty("os.arch")), 
            "Should contain OS architecture");
        assertTrue(message.contains("-Dcom.morphiqlabs.wavelet.max.vector.length"), 
            "Should contain override instruction");
    }
    
    @Test
    @DisplayName("Test getMinVectorLength")
    void testGetMinVectorLength() {
        int minLength = VectorLengthValidator.getMinVectorLength();
        assertEquals(1, minLength, "Minimum vector length should be 1");
    }
    
    @Test
    @DisplayName("Test getMaxVectorLength")
    void testGetMaxVectorLength() {
        int maxLength = VectorLengthValidator.getMaxVectorLength();
        
        // Default should be 64 unless overridden by system property
        String propertyValue = System.getProperty("com.morphiqlabs.wavelet.max.vector.length");
        if (propertyValue == null) {
            assertEquals(64, maxLength, "Default max vector length should be 64");
        } else {
            assertEquals(Integer.parseInt(propertyValue), maxLength, 
                "Max vector length should match system property");
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 16, 32})
    @DisplayName("Test common SIMD vector lengths")
    void testCommonSIMDVectorLengths(int length) {
        assertDoesNotThrow(() -> 
            VectorLengthValidator.validateVectorLength(length, "SIMD Test"),
            "Common SIMD length " + length + " should be valid"
        );
    }
    
    @Test
    @DisplayName("Test with different contexts")
    void testDifferentContexts() {
        String[] contexts = {
            "VectorOps",
            "CacheAwareOps",
            "GatherScatterOps",
            "com.morphiqlabs.wavelet.internal.Test",
            ""  // Empty context
        };
        
        for (String context : contexts) {
            // Test with valid length
            assertDoesNotThrow(() -> 
                VectorLengthValidator.validateVectorLength(8, context)
            );
            
            // Test with invalid length
            IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> VectorLengthValidator.validateVectorLength(1000, context)
            );
            
            if (!context.isEmpty()) {
                assertTrue(exception.getMessage().contains(context),
                    "Error message should contain context: " + context);
            }
        }
    }
    
    @Test
    @DisplayName("Test system property override")
    void testSystemPropertyOverride() throws Exception {
        // Save original value
        String originalValue = System.getProperty("com.morphiqlabs.wavelet.max.vector.length");
        
        try {
            // This test shows how the system property would work, but we can't
            // actually change it at runtime since it's read in a static initializer
            // We'll just verify the current behavior
            
            int currentMax = VectorLengthValidator.getMaxVectorLength();
            
            // Verify that values up to current max are valid
            assertDoesNotThrow(() -> 
                VectorLengthValidator.validateVectorLength(currentMax, "Test")
            );
            
            // Verify that values above current max are invalid
            assertThrows(
                IllegalStateException.class,
                () -> VectorLengthValidator.validateVectorLength(currentMax + 1, "Test")
            );
            
        } finally {
            // Restore original value if it existed
            if (originalValue != null) {
                System.setProperty("com.morphiqlabs.wavelet.max.vector.length", originalValue);
            }
        }
    }
    
    @Test
    @DisplayName("Test extreme boundary values")
    void testExtremeBoundaryValues() {
        // Test at exact boundaries
        assertDoesNotThrow(() -> 
            VectorLengthValidator.validateVectorLength(
                VectorLengthValidator.getMinVectorLength(), "MinBoundary")
        );
        
        assertDoesNotThrow(() -> 
            VectorLengthValidator.validateVectorLength(
                VectorLengthValidator.getMaxVectorLength(), "MaxBoundary")
        );
        
        // Test just outside boundaries
        assertThrows(
            IllegalStateException.class,
            () -> VectorLengthValidator.validateVectorLength(
                VectorLengthValidator.getMinVectorLength() - 1, "BelowMin")
        );
        
        assertThrows(
            IllegalStateException.class,
            () -> VectorLengthValidator.validateVectorLength(
                VectorLengthValidator.getMaxVectorLength() + 1, "AboveMax")
        );
    }
    
    @Test
    @DisplayName("Test class is final and has private constructor")
    void testClassStructure() throws Exception {
        Class<?> clazz = VectorLengthValidator.class;
        
        // Check class is final
        assertTrue(java.lang.reflect.Modifier.isFinal(clazz.getModifiers()),
            "Class should be final");
        
        // Check constructor is private
        assertEquals(1, clazz.getDeclaredConstructors().length,
            "Should have exactly one constructor");
        
        assertTrue(java.lang.reflect.Modifier.isPrivate(
            clazz.getDeclaredConstructors()[0].getModifiers()),
            "Constructor should be private");
    }
    
    @Test
    @DisplayName("Test constants are properly initialized")
    void testConstantsInitialization() throws Exception {
        // Use reflection to verify the constants
        Class<?> clazz = VectorLengthValidator.class;
        
        Field minField = clazz.getDeclaredField("MIN_VECTOR_LENGTH");
        minField.setAccessible(true);
        assertEquals(1, minField.get(null), "MIN_VECTOR_LENGTH should be 1");
        
        Field maxField = clazz.getDeclaredField("MAX_VECTOR_LENGTH");
        maxField.setAccessible(true);
        Object maxValue = maxField.get(null);
        assertNotNull(maxValue, "MAX_VECTOR_LENGTH should not be null");
        assertTrue((Integer) maxValue > 0, "MAX_VECTOR_LENGTH should be positive");
    }
}
