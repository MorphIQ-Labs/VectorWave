package com.morphiqlabs.wavelet.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for exception classes.
 */
@DisplayName("Exception Classes Tests")
class ExceptionTest {
    
    // === WaveletTransformException Tests ===
    
    @Test
    @DisplayName("WaveletTransformException with message only")
    void testWaveletTransformExceptionMessage() {
        String message = "Test error message";
        WaveletTransformException exception = new WaveletTransformException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }
    
    @Test
    @DisplayName("WaveletTransformException with message and cause")
    void testWaveletTransformExceptionMessageAndCause() {
        String message = "Test error message";
        Exception cause = new RuntimeException("Root cause");
        WaveletTransformException exception = new WaveletTransformException(message, cause);
        
        assertEquals(message, exception.getMessage());
        assertEquals(cause, exception.getCause());
    }
    
    @Test
    @DisplayName("WaveletTransformException with cause only")
    void testWaveletTransformExceptionCause() {
        Exception cause = new RuntimeException("Root cause");
        // WaveletTransformException doesn't have a constructor that takes only a Throwable
        // Using the (String, Throwable) constructor instead
        WaveletTransformException exception = new WaveletTransformException(cause.toString(), cause);
        
        assertEquals(cause, exception.getCause());
        // Message should contain cause's class name
        assertTrue(exception.getMessage().contains("RuntimeException"));
    }
    
    @Test
    @DisplayName("WaveletTransformException should be throwable")
    void testWaveletTransformExceptionThrowable() {
        assertThrows(WaveletTransformException.class, () -> {
            throw new WaveletTransformException("Test throw");
        });
    }
    
    // === InvalidSignalException Tests ===
    
    @Test
    @DisplayName("InvalidSignalException with message only")
    void testInvalidSignalExceptionMessage() {
        String message = "Invalid signal: too short";
        InvalidSignalException exception = new InvalidSignalException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }
    
    @Test
    @DisplayName("InvalidSignalException with message only - no cause constructor")
    void testInvalidSignalExceptionMessage2() {
        // InvalidSignalException only has a constructor that takes a String message
        String message = "Invalid signal: contains NaN";
        InvalidSignalException exception = new InvalidSignalException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }
    
    @Test
    @DisplayName("InvalidSignalException inheritance")
    void testInvalidSignalExceptionInheritance() {
        InvalidSignalException exception = new InvalidSignalException("Test");
        
        // Should be instance of parent class
        assertTrue(exception instanceof WaveletTransformException);
        
        // Can be caught as parent exception
        try {
            throw exception;
        } catch (WaveletTransformException e) {
            assertEquals("Test", e.getMessage());
        }
    }
    
    @Test
    @DisplayName("InvalidSignalException factory method for length errors")
    void testInvalidSignalExceptionLengthError() {
        int invalidLength = 7;
        // Using the correct static factory method
        InvalidSignalException exception = InvalidSignalException.notPowerOfTwo(invalidLength);
        
        assertTrue(exception.getMessage().contains("7"));
        assertTrue(exception.getMessage().contains("power of two"));
    }
    
    @Test
    @DisplayName("InvalidSignalException factory method for NaN values")
    void testInvalidSignalExceptionNanValue() {
        String paramName = "signal";
        int index = 5;
        // Using the correct static factory method for NaN
        InvalidSignalException exception = InvalidSignalException.nanValue(paramName, index);
        
        assertTrue(exception.getMessage().contains("index 5"));
        assertTrue(exception.getMessage().contains("NaN"));
    }
    
    @Test
    @DisplayName("InvalidSignalException factory method for infinity")
    void testInvalidSignalExceptionInfinity() {
        String paramName = "signal";
        int index = 10;
        double value = Double.POSITIVE_INFINITY;
        // Using the correct static factory method for infinity
        InvalidSignalException exception = InvalidSignalException.infinityValue(paramName, index, value);
        
        assertTrue(exception.getMessage().contains("index 10"));
        assertTrue(exception.getMessage().contains("positive infinity"));
    }
    
    // === InvalidArgumentException Tests ===
    
    @Test
    @DisplayName("InvalidArgumentException with message only")
    void testInvalidArgumentExceptionMessage() {
        String message = "Argument cannot be negative";
        InvalidArgumentException exception = new InvalidArgumentException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }
    
    @Test
    @DisplayName("InvalidArgumentException with message only - no cause constructor")
    void testInvalidArgumentExceptionMessage2() {
        // InvalidArgumentException only has a constructor that takes a String message
        String message = "Invalid configuration";
        InvalidArgumentException exception = new InvalidArgumentException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getCause());
    }
    
    @Test
    @DisplayName("InvalidArgumentException inheritance")
    void testInvalidArgumentExceptionInheritance() {
        InvalidArgumentException exception = new InvalidArgumentException("Test");
        
        // Should be instance of parent class
        assertTrue(exception instanceof WaveletTransformException);
        
        // Can be caught as parent exception
        try {
            throw exception;
        } catch (WaveletTransformException e) {
            assertEquals("Test", e.getMessage());
        }
    }
    
    @Test
    @DisplayName("InvalidArgumentException factory method for non-positive value")
    void testInvalidArgumentExceptionNotPositive() {
        int value = -5;
        // Using the correct static factory method
        InvalidArgumentException exception = InvalidArgumentException.notPositive(value);
        
        assertTrue(exception.getMessage().contains("-5"));
        assertTrue(exception.getMessage().contains("must be positive"));
    }
    
    @Test
    @DisplayName("InvalidArgumentException factory method for value too large")
    void testInvalidArgumentExceptionTooLarge() {
        int value = Integer.MAX_VALUE;
        int maxValue = 1073741824; // 2^30
        String context = "Next power of two would overflow";
        // Using the correct static factory method
        InvalidArgumentException exception = InvalidArgumentException.tooLarge(value, maxValue, context);
        
        assertTrue(exception.getMessage().contains(String.valueOf(value)));
        assertTrue(exception.getMessage().contains("2^30"));
        assertTrue(exception.getMessage().contains(context));
    }
    
    @Test
    @DisplayName("InvalidSignalException factory method for null signal")
    void testInvalidSignalExceptionNullSignal() {
        String paramName = "inputSignal";
        InvalidSignalException exception = InvalidSignalException.nullSignal(paramName);
        
        assertTrue(exception.getMessage().contains("inputSignal"));
        assertTrue(exception.getMessage().contains("cannot be null"));
    }
    
    @Test
    @DisplayName("InvalidSignalException factory method for empty signal")
    void testInvalidSignalExceptionEmptySignal() {
        String paramName = "coefficients";
        InvalidSignalException exception = InvalidSignalException.emptySignal(paramName);
        
        assertTrue(exception.getMessage().contains("coefficients"));
        assertTrue(exception.getMessage().contains("cannot be empty"));
    }
    
    @Test
    @DisplayName("InvalidSignalException factory method for mismatched coefficients")
    void testInvalidSignalExceptionMismatchedCoefficients() {
        int approxLength = 64;
        int detailLength = 32;
        InvalidSignalException exception = InvalidSignalException.mismatchedCoefficients(approxLength, detailLength);
        
        assertTrue(exception.getMessage().contains("64"));
        assertTrue(exception.getMessage().contains("32"));
        assertTrue(exception.getMessage().contains("same length"));
    }
    
    // === Exception Hierarchy Tests ===
    
    @Test
    @DisplayName("All custom exceptions should extend WaveletTransformException")
    void testExceptionHierarchy() {
        WaveletTransformException baseException = new WaveletTransformException("Base");
        InvalidSignalException signalException = new InvalidSignalException("Signal");
        InvalidArgumentException argException = new InvalidArgumentException("Arg");
        
        // All should be instances of the base exception
        assertTrue(baseException instanceof WaveletTransformException);
        assertTrue(signalException instanceof WaveletTransformException);
        assertTrue(argException instanceof WaveletTransformException);
        
        // They are different exception types (siblings in the hierarchy)
        assertNotEquals(signalException.getClass(), argException.getClass());
    }
    
    @Test
    @DisplayName("Null messages should be handled")
    void testNullMessages() {
        // These should not throw NPE
        WaveletTransformException e1 = new WaveletTransformException((String) null);
        InvalidSignalException e2 = new InvalidSignalException(null);
        InvalidArgumentException e3 = new InvalidArgumentException(null);
        
        // getMessage() may return null, that's ok
        assertDoesNotThrow(() -> e1.getMessage());
        assertDoesNotThrow(() -> e2.getMessage());
        assertDoesNotThrow(() -> e3.getMessage());
    }
}