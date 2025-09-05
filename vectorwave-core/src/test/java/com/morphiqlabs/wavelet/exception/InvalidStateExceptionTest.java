package com.morphiqlabs.wavelet.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for InvalidStateException to achieve full coverage.
 */
class InvalidStateExceptionTest {

    @Test
    @DisplayName("Should create exception with message only")
    void testConstructorWithMessage() {
        String message = "Invalid state occurred";
        InvalidStateException exception = new InvalidStateException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getErrorCode()); // No error code set with this constructor
    }

    @Test
    @DisplayName("Should create exception with error code and message")
    void testConstructorWithErrorCodeAndMessage() {
        ErrorCode errorCode = ErrorCode.STATE_INVALID;
        String message = "The object is in an invalid state";
        InvalidStateException exception = new InvalidStateException(errorCode, message);
        
        assertEquals(message, exception.getMessage());
        assertEquals(errorCode, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should create exception for closed resource using factory method")
    void testClosedFactory() {
        String resourceName = "StreamProcessor";
        InvalidStateException exception = InvalidStateException.closed(resourceName);
        
        assertEquals("StreamProcessor is closed", exception.getMessage());
        assertEquals(ErrorCode.STATE_CLOSED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should create exception for uninitialized object using factory method")
    void testNotInitializedFactory() {
        String objectName = "WaveletTransform";
        InvalidStateException exception = InvalidStateException.notInitialized(objectName);
        
        assertEquals("WaveletTransform has not been initialized", exception.getMessage());
        assertEquals(ErrorCode.STATE_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should handle empty resource names in factory methods")
    void testFactoryMethodsWithEmptyNames() {
        // Test closed with empty name
        InvalidStateException closedException = InvalidStateException.closed("");
        assertEquals(" is closed", closedException.getMessage());
        assertEquals(ErrorCode.STATE_CLOSED, closedException.getErrorCode());
        
        // Test notInitialized with empty name
        InvalidStateException notInitException = InvalidStateException.notInitialized("");
        assertEquals(" has not been initialized", notInitException.getMessage());
        assertEquals(ErrorCode.STATE_INVALID, notInitException.getErrorCode());
    }

    @Test
    @DisplayName("Should handle null messages appropriately")
    void testNullMessage() {
        // Constructor with null message
        InvalidStateException exception = new InvalidStateException((String) null);
        assertNull(exception.getMessage());
        assertNull(exception.getErrorCode());
        
        // Constructor with error code and null message
        InvalidStateException exceptionWithCode = new InvalidStateException(ErrorCode.STATE_CLOSED, null);
        assertNull(exceptionWithCode.getMessage());
        assertEquals(ErrorCode.STATE_CLOSED, exceptionWithCode.getErrorCode());
    }

    @Test
    @DisplayName("Should be throwable and catchable as WaveletTransformException")
    void testExceptionHierarchy() {
        InvalidStateException exception = new InvalidStateException("test");
        
        // Should be instance of WaveletTransformException
        assertTrue(exception instanceof WaveletTransformException);
        
        // Should be throwable and catchable
        assertThrows(InvalidStateException.class, () -> {
            throw exception;
        });
        
        assertThrows(WaveletTransformException.class, () -> {
            throw exception;
        });
    }

    @Test
    @DisplayName("Should have proper serialization version")
    void testSerializationVersion() {
        // Just ensure the exception is serializable
        InvalidStateException exception = new InvalidStateException("test");
        assertTrue(exception instanceof java.io.Serializable);
    }

    @Test
    @DisplayName("Should chain exceptions properly through factory methods")
    void testFactoryMethodChaining() {
        // Test that factory methods return proper exception type for chaining
        assertThrows(InvalidStateException.class, () -> {
            throw InvalidStateException.closed("MyResource");
        });
        
        assertThrows(InvalidStateException.class, () -> {
            throw InvalidStateException.notInitialized("MyObject");
        });
    }

    @Test
    @DisplayName("Should handle complex resource names in factory methods")
    void testComplexResourceNames() {
        String complexName = "com.morphiqlabs.wavelet.StreamingProcessor[id=123]";
        
        InvalidStateException closedException = InvalidStateException.closed(complexName);
        assertEquals(complexName + " is closed", closedException.getMessage());
        
        InvalidStateException notInitException = InvalidStateException.notInitialized(complexName);
        assertEquals(complexName + " has not been initialized", notInitException.getMessage());
    }
}