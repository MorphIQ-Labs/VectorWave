package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.padding.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class PaddingStrategyTest {
    
    private static final double EPSILON = 1e-10;
    
    // Use ZeroPaddingStrategy to test the default trim implementation
    // since we can't create our own implementation of the sealed interface
    
    @Test
    void testDefaultTrimSameLength() {
        // Use ZeroPaddingStrategy to test the default trim implementation
        PaddingStrategy strategy = new ZeroPaddingStrategy();
        double[] result = {1, 2, 3, 4};
        double[] trimmed = strategy.trim(result, 4);
        
        // Should return the same array when lengths match
        assertSame(result, trimmed);
    }
    
    @Test
    void testDefaultTrimShorter() {
        PaddingStrategy strategy = new ZeroPaddingStrategy();
        double[] result = {1, 2, 3, 4, 5, 6};
        double[] trimmed = strategy.trim(result, 4);
        
        assertEquals(4, trimmed.length);
        assertArrayEquals(new double[]{1, 2, 3, 4}, trimmed, EPSILON);
    }
    
    @Test
    void testDefaultTrimInvalidLength() {
        PaddingStrategy strategy = new ZeroPaddingStrategy();
        double[] result = {1, 2, 3};
        
        assertThrows(InvalidArgumentException.class, () -> strategy.trim(result, 5));
    }
    
    @Test
    void testDefaultDescription() {
        // Test that the default description() method is being overridden
        PaddingStrategy strategy = new ZeroPaddingStrategy();
        assertEquals("Zero padding - extends signal with zeros", strategy.description());
        
        // Test other strategies too
        assertEquals("Symmetric padding - mirrors signal with boundary duplication", new SymmetricPaddingStrategy().description());
        assertEquals("Reflect padding - mirrors signal without boundary duplication", new ReflectPaddingStrategy().description());
        assertEquals("Periodic padding - wraps signal cyclically", new PeriodicPaddingStrategy().description());
    }
    
    @Test
    void testZeroPaddingStrategy() {
        PaddingStrategy strategy = new ZeroPaddingStrategy();
        
        assertEquals("zero", strategy.name());
        assertEquals("Zero padding - extends signal with zeros", strategy.description());
        
        double[] signal = {1, 2, 3};
        double[] padded = strategy.pad(signal, 8);
        
        assertEquals(8, padded.length);
        assertArrayEquals(new double[]{1, 2, 3, 0, 0, 0, 0, 0}, padded, EPSILON);
    }
    
    @Test
    void testSymmetricPaddingStrategy() {
        PaddingStrategy strategy = new SymmetricPaddingStrategy();
        
        assertEquals("symmetric", strategy.name());
        assertEquals("Symmetric padding - mirrors signal with boundary duplication", strategy.description());
        
        double[] signal = {1, 2, 3, 4};
        double[] padded = strategy.pad(signal, 8);
        
        assertEquals(8, padded.length);
        // Symmetric padding mirrors with boundary duplication
        assertArrayEquals(new double[]{1, 2, 3, 4, 4, 3, 2, 1}, padded, EPSILON);
    }
    
    @Test
    void testReflectPaddingStrategy() {
        PaddingStrategy strategy = new ReflectPaddingStrategy();
        
        assertEquals("reflect", strategy.name());
        assertEquals("Reflect padding - mirrors signal without boundary duplication", strategy.description());
        
        double[] signal = {1, 2, 3, 4};
        double[] padded = strategy.pad(signal, 8);
        
        assertEquals(8, padded.length);
        // Reflect padding mirrors without boundary duplication
        assertArrayEquals(new double[]{1, 2, 3, 4, 3, 2, 1, 2}, padded, EPSILON);
    }
    
    @Test
    void testPeriodicPaddingStrategy() {
        PaddingStrategy strategy = new PeriodicPaddingStrategy();
        
        assertEquals("periodic", strategy.name());
        assertEquals("Periodic padding - wraps signal cyclically", strategy.description());
        
        double[] signal = {1, 2, 3, 4};
        double[] padded = strategy.pad(signal, 8);
        
        assertEquals(8, padded.length);
        // Periodic padding wraps around
        assertArrayEquals(new double[]{1, 2, 3, 4, 1, 2, 3, 4}, padded, EPSILON);
    }
    
    static Stream<Arguments> paddingStrategies() {
        return Stream.of(
            Arguments.of(new ZeroPaddingStrategy()),
            Arguments.of(new SymmetricPaddingStrategy()),
            Arguments.of(new ReflectPaddingStrategy()),
            Arguments.of(new PeriodicPaddingStrategy())
        );
    }
    
    @ParameterizedTest
    @MethodSource("paddingStrategies")
    void testPaddingWithSameLength(PaddingStrategy strategy) {
        double[] signal = {1, 2, 3, 4};
        double[] padded = strategy.pad(signal, 4);
        
        assertEquals(4, padded.length);
        assertArrayEquals(signal, padded, EPSILON);
    }
    
    @ParameterizedTest
    @MethodSource("paddingStrategies")
    void testPaddingNullSignalThrows(PaddingStrategy strategy) {
        assertThrows(InvalidArgumentException.class, () -> strategy.pad(null, 8));
    }
    
    @ParameterizedTest
    @MethodSource("paddingStrategies")
    void testPaddingInvalidTargetLengthThrows(PaddingStrategy strategy) {
        double[] signal = {1, 2, 3, 4};
        assertThrows(InvalidArgumentException.class, () -> strategy.pad(signal, 2));
    }
    
    @Test
    void testComplexPaddingScenarios() {
        // Test symmetric padding with odd-length signal
        PaddingStrategy symmetric = new SymmetricPaddingStrategy();
        double[] oddSignal = {1, 2, 3};
        double[] paddedOdd = symmetric.pad(oddSignal, 8);
        assertEquals(8, paddedOdd.length);
        
        // Test reflect padding with single element
        PaddingStrategy reflect = new ReflectPaddingStrategy();
        double[] singleElement = {5};
        double[] paddedSingle = reflect.pad(singleElement, 4);
        assertArrayEquals(new double[]{5, 5, 5, 5}, paddedSingle, EPSILON);
        
        // Test periodic padding with large target
        PaddingStrategy periodic = new PeriodicPaddingStrategy();
        double[] shortSignal = {1, 2};
        double[] paddedLarge = periodic.pad(shortSignal, 10);
        assertArrayEquals(new double[]{1, 2, 1, 2, 1, 2, 1, 2, 1, 2}, paddedLarge, EPSILON);
    }
    
    @Test
    void testPaddingStrategiesAreFinal() {
        // Verify that all concrete padding strategies are final (sealed interface)
        assertTrue(java.lang.reflect.Modifier.isFinal(ZeroPaddingStrategy.class.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isFinal(SymmetricPaddingStrategy.class.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isFinal(ReflectPaddingStrategy.class.getModifiers()));
        assertTrue(java.lang.reflect.Modifier.isFinal(PeriodicPaddingStrategy.class.getModifiers()));
    }
}