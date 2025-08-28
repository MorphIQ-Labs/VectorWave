package com.morphiqlabs.wavelet.exception;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the error code functionality in exceptions.
 */
@DisplayName("Error Code Tests")
class ErrorCodeTest {
    
    @Test
    @DisplayName("Exception should contain error code when created with code")
    void testExceptionWithErrorCode() {
        InvalidArgumentException ex = InvalidArgumentException.nullArgument("signal");
        
        assertTrue(ex.hasErrorCode());
        assertEquals(ErrorCode.VAL_NULL_ARGUMENT, ex.getErrorCode());
        assertEquals("VAL_001", ex.getErrorCode().getCode());
        assertEquals("signal cannot be null", ex.getMessage());
    }
    
    @Test
    @DisplayName("Exception without error code should return null")
    void testExceptionWithoutErrorCode() {
        InvalidArgumentException ex = new InvalidArgumentException("Custom message");
        
        assertFalse(ex.hasErrorCode());
        assertNull(ex.getErrorCode());
        assertEquals("Custom message", ex.getMessage());
    }
    
    @Test
    @DisplayName("InvalidSignalException should have correct error codes")
    void testInvalidSignalExceptionCodes() {
        InvalidSignalException nullEx = InvalidSignalException.nullSignal("data");
        assertEquals(ErrorCode.VAL_NULL_ARGUMENT, nullEx.getErrorCode());
        
        InvalidSignalException emptyEx = InvalidSignalException.emptySignal("data");
        assertEquals(ErrorCode.VAL_EMPTY, emptyEx.getErrorCode());
        
        InvalidSignalException powerEx = InvalidSignalException.notPowerOfTwo(100);
        assertEquals(ErrorCode.VAL_NOT_POWER_OF_TWO, powerEx.getErrorCode());
        
        InvalidSignalException nanEx = InvalidSignalException.nanValue("signal", 5);
        assertEquals(ErrorCode.VAL_NON_FINITE_VALUES, nanEx.getErrorCode());
        
        InvalidSignalException infEx = InvalidSignalException.infinityValue("signal", 10, Double.POSITIVE_INFINITY);
        assertEquals(ErrorCode.VAL_NON_FINITE_VALUES, infEx.getErrorCode());
        
        InvalidSignalException mismatchEx = InvalidSignalException.mismatchedCoefficients(10, 5);
        assertEquals(ErrorCode.VAL_LENGTH_MISMATCH, mismatchEx.getErrorCode());
    }
    
    @Test
    @DisplayName("InvalidStateException should have correct error codes")
    void testInvalidStateExceptionCodes() {
        InvalidStateException closedEx = InvalidStateException.closed("Transform");
        assertEquals(ErrorCode.STATE_CLOSED, closedEx.getErrorCode());
        
        InvalidStateException notInitEx = InvalidStateException.notInitialized("Pool");
        assertEquals(ErrorCode.STATE_INVALID, notInitEx.getErrorCode());
    }
    
    @Test
    @DisplayName("InvalidConfigurationException should have correct error codes")
    void testInvalidConfigurationExceptionCodes() {
        InvalidConfigurationException conflictEx = InvalidConfigurationException.conflictingOptions("forceScalar", "forceVector");
        assertEquals(ErrorCode.CFG_CONFLICTING_OPTIONS, conflictEx.getErrorCode());
        
        InvalidConfigurationException unsupportedEx = InvalidConfigurationException.unsupportedOperation("ContinuousWavelet", "discrete transform");
        assertEquals(ErrorCode.CFG_UNSUPPORTED_OPERATION, unsupportedEx.getErrorCode());
        
        InvalidConfigurationException boundaryEx = InvalidConfigurationException.unsupportedBoundaryMode("REFLECT");
        assertEquals(ErrorCode.CFG_UNSUPPORTED_BOUNDARY_MODE, boundaryEx.getErrorCode());
    }
    
    @Test
    @DisplayName("Error codes should have unique identifiers")
    void testErrorCodeUniqueness() {
        // Check a few error codes
        assertNotEquals(ErrorCode.VAL_NULL_ARGUMENT.getCode(), ErrorCode.VAL_NOT_POWER_OF_TWO.getCode());
        assertNotEquals(ErrorCode.CFG_CONFLICTING_OPTIONS.getCode(), ErrorCode.STATE_CLOSED.getCode());
        assertNotEquals(ErrorCode.SIG_TOO_SHORT.getCode(), ErrorCode.POOL_FULL.getCode());
    }
    
    @Test
    @DisplayName("Error code toString should include code and description")
    void testErrorCodeToString() {
        String str = ErrorCode.VAL_NULL_ARGUMENT.toString();
        assertTrue(str.contains("VAL_001"));
        assertTrue(str.contains("Null argument"));
    }
    
    @Test
    @DisplayName("Programmatic error handling should work with error codes")
    void testProgrammaticErrorHandling() {
        try {
            throw InvalidArgumentException.nullArgument("test");
        } catch (InvalidArgumentException e) {
            // Can check error code programmatically
            if (e.hasErrorCode() && e.getErrorCode() == ErrorCode.VAL_NULL_ARGUMENT) {
                // Handle null argument case specifically
                assertEquals(ErrorCode.VAL_NULL_ARGUMENT, e.getErrorCode());
            } else {
                fail("Should have caught null argument error");
            }
        }
    }
}