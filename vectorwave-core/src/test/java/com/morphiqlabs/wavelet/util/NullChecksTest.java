package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NullChecksTest {

    @Test
    void testRequireNonNull_WithNonNullObject_ReturnsObject() {
        String testObj = "test";
        String result = NullChecks.requireNonNull(testObj, "testObj");
        assertSame(testObj, result);
    }

    @Test
    void testRequireNonNull_WithNullObject_ThrowsException() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> NullChecks.requireNonNull(null, "testParam")
        );
        assertTrue(exception.getMessage().contains("testParam"));
    }

    @Test
    void testRequireNonEmpty_DoubleArray_WithValidArray_ReturnsArray() {
        double[] array = {1.0, 2.0, 3.0};
        double[] result = NullChecks.requireNonEmpty(array, "array");
        assertSame(array, result);
    }

    @Test
    void testRequireNonEmpty_DoubleArray_WithNullArray_ThrowsException() {
        assertThrows(
                InvalidArgumentException.class,
                () -> NullChecks.requireNonEmpty((double[]) null, "array")
        );
    }

    @Test
    void testRequireNonEmpty_DoubleArray_WithEmptyArray_ThrowsException() {
        double[] emptyArray = new double[0];
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> NullChecks.requireNonEmpty(emptyArray, "array")
        );
        assertEquals("array cannot be empty", exception.getMessage());
    }

    @Test
    void testRequireNonEmpty_GenericArray_WithValidArray_ReturnsArray() {
        String[] array = {"a", "b", "c"};
        String[] result = NullChecks.requireNonEmpty(array, "array");
        assertSame(array, result);
    }

    @Test
    void testRequireNonEmpty_GenericArray_WithNullArray_ThrowsException() {
        assertThrows(
                InvalidArgumentException.class,
                () -> NullChecks.requireNonEmpty((String[]) null, "array")
        );
    }

    @Test
    void testRequireNonEmpty_GenericArray_WithEmptyArray_ThrowsException() {
        String[] emptyArray = new String[0];
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> NullChecks.requireNonEmpty(emptyArray, "array")
        );
        assertEquals("array cannot be empty", exception.getMessage());
    }

    @Test
    void testRequireBothNonNull_WithBothNonNull_DoesNotThrow() {
        assertDoesNotThrow(() -> 
            NullChecks.requireBothNonNull("obj1", "param1", "obj2", "param2")
        );
    }

    @Test
    void testRequireBothNonNull_WithFirstNull_ThrowsException() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> NullChecks.requireBothNonNull(null, "param1", "obj2", "param2")
        );
        assertTrue(exception.getMessage().contains("param1"));
    }

    @Test
    void testRequireBothNonNull_WithSecondNull_ThrowsException() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> NullChecks.requireBothNonNull("obj1", "param1", null, "param2")
        );
        assertTrue(exception.getMessage().contains("param2"));
    }

    @Test
    void testRequireBothNonNull_WithBothNull_ThrowsExceptionForFirst() {
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> NullChecks.requireBothNonNull(null, "param1", null, "param2")
        );
        assertTrue(exception.getMessage().contains("param1"));
    }

    @Test
    void testRequireNoNullElements_WithValidArray_ReturnsArray() {
        String[] array = {"a", "b", "c"};
        String[] result = NullChecks.requireNoNullElements(array, "array");
        assertSame(array, result);
    }

    @Test
    void testRequireNoNullElements_WithNullArray_ThrowsException() {
        assertThrows(
                InvalidArgumentException.class,
                () -> NullChecks.requireNoNullElements(null, "array")
        );
    }

    @Test
    void testRequireNoNullElements_WithNullElement_ThrowsException() {
        String[] array = {"a", null, "c"};
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> NullChecks.requireNoNullElements(array, "array")
        );
        assertEquals("array[1] cannot be null", exception.getMessage());
    }

    @Test
    void testRequireNoNullElements_WithMultipleNullElements_ThrowsExceptionForFirst() {
        String[] array = {"a", null, "c", null};
        InvalidArgumentException exception = assertThrows(
                InvalidArgumentException.class,
                () -> NullChecks.requireNoNullElements(array, "array")
        );
        assertEquals("array[1] cannot be null", exception.getMessage());
    }

    @Test
    void testRequireNoNullElements_WithEmptyArray_ReturnsArray() {
        String[] emptyArray = new String[0];
        String[] result = NullChecks.requireNoNullElements(emptyArray, "array");
        assertSame(emptyArray, result);
    }

    @Test
    void testDifferentTypes() {
        // Test with different types to ensure generics work correctly
        Integer intObj = 42;
        assertEquals(intObj, NullChecks.requireNonNull(intObj, "intObj"));
        
        Double[] doubleArray = {1.0, 2.0};
        assertSame(doubleArray, NullChecks.requireNonEmpty(doubleArray, "doubleArray"));
        
        Object[] mixedArray = {"string", 123, true};
        assertSame(mixedArray, NullChecks.requireNoNullElements(mixedArray, "mixedArray"));
    }
}