package com.morphiqlabs.wavelet;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for padding strategies.
 */
class PaddingStrategyTest {
    
    private static final double EPSILON = 1e-10;
    
    static Stream<Arguments> paddingStrategies() {
        return Stream.of(
            Arguments.of(new ZeroPaddingStrategy(), "Zero"),
            Arguments.of(new SymmetricPaddingStrategy(), "Symmetric"),
            Arguments.of(new ReflectPaddingStrategy(), "Reflect"),
            Arguments.of(new PeriodicPaddingStrategy(), "Periodic")
        );
    }
    
    @ParameterizedTest(name = "{1} padding strategy")
    @MethodSource("paddingStrategies")
    @DisplayName("Padding preserves original signal")
    void testPaddingPreservesOriginal(PaddingStrategy strategy, String name) {
        double[] signal = {1, 2, 3, 4, 5};
        double[] padded = strategy.pad(signal, 8);
        
        // First part should match original
        for (int i = 0; i < signal.length; i++) {
            assertEquals(signal[i], padded[i], EPSILON,
                name + " padding should preserve original values at index " + i);
        }
    }
    
    @Test
    @DisplayName("Zero padding extends with zeros")
    void testZeroPadding() {
        ZeroPaddingStrategy strategy = new ZeroPaddingStrategy();
        double[] signal = {1, 2, 3, 4};
        double[] padded = strategy.pad(signal, 8);
        
        assertArrayEquals(new double[]{1, 2, 3, 4, 0, 0, 0, 0}, padded, EPSILON);
    }
    
    @Test
    @DisplayName("Symmetric padding mirrors with boundary")
    void testSymmetricPadding() {
        SymmetricPaddingStrategy strategy = new SymmetricPaddingStrategy();
        double[] signal = {1, 2, 3, 4};
        double[] padded = strategy.pad(signal, 8);
        
        // Expected: [1, 2, 3, 4, 4, 3, 2, 1]
        assertArrayEquals(new double[]{1, 2, 3, 4, 4, 3, 2, 1}, padded, EPSILON);
    }
    
    @Test
    @DisplayName("Reflect padding mirrors without boundary")
    void testReflectPadding() {
        ReflectPaddingStrategy strategy = new ReflectPaddingStrategy();
        double[] signal = {1, 2, 3, 4};
        double[] padded = strategy.pad(signal, 8);
        
        // Expected: [1, 2, 3, 4, 3, 2, 1, 2]
        assertArrayEquals(new double[]{1, 2, 3, 4, 3, 2, 1, 2}, padded, EPSILON);
    }
    
    @Test
    @DisplayName("Periodic padding wraps cyclically")
    void testPeriodicPadding() {
        PeriodicPaddingStrategy strategy = new PeriodicPaddingStrategy();
        double[] signal = {1, 2, 3, 4};
        double[] padded = strategy.pad(signal, 10);
        
        // Expected: [1, 2, 3, 4, 1, 2, 3, 4, 1, 2]
        assertArrayEquals(new double[]{1, 2, 3, 4, 1, 2, 3, 4, 1, 2}, padded, EPSILON);
    }
    
    @Test
    @DisplayName("Reflect padding handles single element")
    void testReflectPaddingSingleElement() {
        ReflectPaddingStrategy strategy = new ReflectPaddingStrategy();
        double[] signal = {5};
        double[] padded = strategy.pad(signal, 4);
        
        // Single element should repeat
        assertArrayEquals(new double[]{5, 5, 5, 5}, padded, EPSILON);
    }
    
    @ParameterizedTest(name = "{1} padding strategy")
    @MethodSource("paddingStrategies")
    @DisplayName("Padding handles exact length (no padding needed)")
    void testNoPaddingNeeded(PaddingStrategy strategy, String name) {
        double[] signal = {1, 2, 3, 4};
        double[] padded = strategy.pad(signal, 4);
        
        assertArrayEquals(signal, padded, EPSILON);
        assertNotSame(signal, padded, "Should return a copy, not the same array");
    }
    
    @ParameterizedTest(name = "{1} padding strategy")
    @MethodSource("paddingStrategies")
    @DisplayName("Padding validates inputs")
    void testPaddingValidation(PaddingStrategy strategy, String name) {
        // Null signal
        assertThrows(InvalidArgumentException.class,
            () -> strategy.pad(null, 8));
        
        // Target length too small
        double[] signal = {1, 2, 3, 4};
        assertThrows(InvalidArgumentException.class,
            () -> strategy.pad(signal, 2));
    }
    
    @ParameterizedTest(name = "{1} padding strategy")
    @MethodSource("paddingStrategies")
    @DisplayName("Trim restores original length")
    void testTrimming(PaddingStrategy strategy, String name) {
        double[] padded = {1, 2, 3, 4, 5, 6, 7, 8};
        double[] trimmed = strategy.trim(padded, 5);
        
        assertEquals(5, trimmed.length);
        assertArrayEquals(new double[]{1, 2, 3, 4, 5}, trimmed, EPSILON);
    }
    
    @Test
    @DisplayName("Symmetric padding with longer extension")
    void testSymmetricPaddingLonger() {
        SymmetricPaddingStrategy strategy = new SymmetricPaddingStrategy();
        double[] signal = {1, 2, 3};
        double[] padded = strategy.pad(signal, 10);
        
        // Should cycle: [1, 2, 3, 3, 2, 1, 1, 2, 3, 3]
        double[] expected = {1, 2, 3, 3, 2, 1, 1, 2, 3, 3};
        assertArrayEquals(expected, padded, EPSILON);
    }
    
    @Test
    @DisplayName("Strategy names and descriptions")
    void testStrategyMetadata() {
        assertEquals("zero", new ZeroPaddingStrategy().name());
        assertEquals("symmetric", new SymmetricPaddingStrategy().name());
        assertEquals("reflect", new ReflectPaddingStrategy().name());
        assertEquals("periodic", new PeriodicPaddingStrategy().name());
        
        assertTrue(new ZeroPaddingStrategy().description().contains("Zero"));
        assertTrue(new SymmetricPaddingStrategy().description().contains("Symmetric"));
    }
}