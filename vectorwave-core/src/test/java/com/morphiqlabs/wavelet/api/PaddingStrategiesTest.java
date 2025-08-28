package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.padding.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for new padding strategies.
 */
@DisplayName("Padding Strategies Test Suite")
public class PaddingStrategiesTest {
    
    private static final double TOLERANCE = 1e-10;
    
    // ==================== ConstantPaddingStrategy Tests ====================
    
    @Test
    @DisplayName("Constant padding - RIGHT mode")
    void testConstantPaddingRight() {
        var strategy = new ConstantPaddingStrategy(ConstantPaddingStrategy.PaddingMode.RIGHT);
        double[] signal = {1, 2, 3, 4};
        double[] padded = strategy.pad(signal, 8);
        
        assertArrayEquals(new double[]{1, 2, 3, 4, 4, 4, 4, 4}, padded, TOLERANCE);
        assertEquals("constant-right", strategy.name());
    }
    
    @Test
    @DisplayName("Constant padding - LEFT mode")
    void testConstantPaddingLeft() {
        var strategy = new ConstantPaddingStrategy(ConstantPaddingStrategy.PaddingMode.LEFT);
        double[] signal = {1, 2, 3, 4};
        double[] padded = strategy.pad(signal, 8);
        
        assertArrayEquals(new double[]{1, 1, 1, 1, 1, 2, 3, 4}, padded, TOLERANCE);
    }
    
    @Test
    @DisplayName("Constant padding - SYMMETRIC mode")
    void testConstantPaddingSymmetric() {
        var strategy = new ConstantPaddingStrategy(ConstantPaddingStrategy.PaddingMode.SYMMETRIC);
        double[] signal = {1, 2, 3, 4};
        double[] padded = strategy.pad(signal, 8);
        
        assertArrayEquals(new double[]{1, 1, 1, 2, 3, 4, 4, 4}, padded, TOLERANCE);
    }
    
    @Test
    @DisplayName("Constant padding - default constructor uses RIGHT mode")
    void testConstantPaddingDefault() {
        var strategy = new ConstantPaddingStrategy();
        double[] signal = {5, 10};
        double[] padded = strategy.pad(signal, 4);
        
        assertArrayEquals(new double[]{5, 10, 10, 10}, padded, TOLERANCE);
    }
    
    @Test
    @DisplayName("Constant padding - trim operations")
    void testConstantPaddingTrim() {
        var rightStrategy = new ConstantPaddingStrategy(ConstantPaddingStrategy.PaddingMode.RIGHT);
        var leftStrategy = new ConstantPaddingStrategy(ConstantPaddingStrategy.PaddingMode.LEFT);
        var symStrategy = new ConstantPaddingStrategy(ConstantPaddingStrategy.PaddingMode.SYMMETRIC);
        
        double[] result = {1, 2, 3, 4, 5, 6, 7, 8};
        
        assertArrayEquals(new double[]{1, 2, 3, 4}, rightStrategy.trim(result, 4), TOLERANCE);
        assertArrayEquals(new double[]{5, 6, 7, 8}, leftStrategy.trim(result, 4), TOLERANCE);
        assertArrayEquals(new double[]{3, 4, 5, 6}, symStrategy.trim(result, 4), TOLERANCE);
    }
    
    // ==================== LinearExtrapolationStrategy Tests ====================
    
    @Test
    @DisplayName("Linear extrapolation - simple ascending sequence")
    void testLinearExtrapolationAscending() {
        var strategy = new LinearExtrapolationStrategy(2);
        double[] signal = {1, 2, 3, 4};
        double[] padded = strategy.pad(signal, 8);
        
        // Slope is 1, so continues as 5, 6, 7, 8
        assertArrayEquals(new double[]{1, 2, 3, 4, 5, 6, 7, 8}, padded, TOLERANCE);
    }
    
    @Test
    @DisplayName("Linear extrapolation - descending sequence")
    void testLinearExtrapolationDescending() {
        var strategy = new LinearExtrapolationStrategy(2);
        double[] signal = {10, 8, 6, 4};
        double[] padded = strategy.pad(signal, 6);
        
        // Slope is -2, so continues as 2, 0
        assertArrayEquals(new double[]{10, 8, 6, 4, 2, 0}, padded, TOLERANCE);
    }
    
    @Test
    @DisplayName("Linear extrapolation - LEFT mode")
    void testLinearExtrapolationLeft() {
        var strategy = new LinearExtrapolationStrategy(2, LinearExtrapolationStrategy.PaddingMode.LEFT);
        double[] signal = {4, 6, 8, 10};
        double[] padded = strategy.pad(signal, 6);
        
        // Slope is 2, extrapolate backwards: 0, 2, 4, 6, 8, 10
        assertArrayEquals(new double[]{0, 2, 4, 6, 8, 10}, padded, TOLERANCE);
    }
    
    @Test
    @DisplayName("Linear extrapolation - SYMMETRIC mode")
    void testLinearExtrapolationSymmetric() {
        var strategy = new LinearExtrapolationStrategy(2, LinearExtrapolationStrategy.PaddingMode.SYMMETRIC);
        double[] signal = {2, 4, 6, 8};
        double[] padded = strategy.pad(signal, 8);
        
        // Left slope is 2, right slope is 2
        // Left extrapolation: 0, then signal, then right extrapolation: 10, 12
        assertArrayEquals(new double[]{-2, 0, 2, 4, 6, 8, 10, 12}, padded, TOLERANCE);
    }
    
    @Test
    @DisplayName("Linear extrapolation - least squares fitting with more points")
    void testLinearExtrapolationLeastSquares() {
        var strategy = new LinearExtrapolationStrategy(4);
        double[] signal = {1, 2.1, 2.9, 4.2, 5.1}; // Approximately linear with noise
        double[] padded = strategy.pad(signal, 7);
        
        // Should fit a line through last 4 points and extrapolate
        assertEquals(7, padded.length);
        // Check that extrapolation continues the trend
        assertTrue(padded[5] > padded[4]);
        assertTrue(padded[6] > padded[5]);
    }
    
    @Test
    @DisplayName("Linear extrapolation - constant signal")
    void testLinearExtrapolationConstant() {
        var strategy = new LinearExtrapolationStrategy(3);
        double[] signal = {5, 5, 5, 5};
        double[] padded = strategy.pad(signal, 6);
        
        // Slope is 0, so continues as 5, 5
        assertArrayEquals(new double[]{5, 5, 5, 5, 5, 5}, padded, TOLERANCE);
    }
    
    @Test
    @DisplayName("Linear extrapolation - trim operations")
    void testLinearExtrapolationTrim() {
        var rightStrategy = new LinearExtrapolationStrategy(2, LinearExtrapolationStrategy.PaddingMode.RIGHT);
        var leftStrategy = new LinearExtrapolationStrategy(2, LinearExtrapolationStrategy.PaddingMode.LEFT);
        var symStrategy = new LinearExtrapolationStrategy(2, LinearExtrapolationStrategy.PaddingMode.SYMMETRIC);
        
        double[] result = {1, 2, 3, 4, 5, 6, 7, 8};
        
        assertArrayEquals(new double[]{1, 2, 3, 4}, rightStrategy.trim(result, 4), TOLERANCE);
        assertArrayEquals(new double[]{5, 6, 7, 8}, leftStrategy.trim(result, 4), TOLERANCE);
        assertArrayEquals(new double[]{3, 4, 5, 6}, symStrategy.trim(result, 4), TOLERANCE);
    }
    
    // ==================== AntisymmetricPaddingStrategy Tests ====================
    
    @Test
    @DisplayName("Antisymmetric padding - HALF_POINT simple case")
    void testAntisymmetricHalfPoint() {
        var strategy = new AntisymmetricPaddingStrategy(AntisymmetricPaddingStrategy.SymmetryType.HALF_POINT);
        double[] signal = {1, 2, 3};
        double[] padded = strategy.pad(signal, 7);
        
        // Half-point: reflects between boundaries
        assertEquals(7, padded.length);
        
        // Verify original signal is preserved at correct position
        // The padding extends on the right, so original signal starts at index 0
        assertEquals(signal[0], padded[0], TOLERANCE); // Original signal preserved
        assertEquals(signal[1], padded[1], TOLERANCE);
        assertEquals(signal[2], padded[2], TOLERANCE);
        
        // Verify antisymmetric extensions exist
        // Right padding should have sign-flipped values
        assertTrue(Math.abs(padded[3]) > 0); // Non-zero extension
        assertTrue(Math.abs(padded[4]) > 0); // Non-zero extension
    }
    
    @Test
    @DisplayName("Antisymmetric padding - WHOLE_POINT simple case")
    void testAntisymmetricWholePoint() {
        var strategy = new AntisymmetricPaddingStrategy(AntisymmetricPaddingStrategy.SymmetryType.WHOLE_POINT);
        double[] signal = {1, 2, 3};
        double[] padded = strategy.pad(signal, 7);
        
        assertEquals(7, padded.length);
        
        // Verify original signal is preserved
        assertEquals(signal[0], padded[0], TOLERANCE);
        assertEquals(signal[1], padded[1], TOLERANCE);
        assertEquals(signal[2], padded[2], TOLERANCE);
    }
    
    @Test
    @DisplayName("Antisymmetric padding - single element signal")
    void testAntisymmetricSingleElement() {
        var strategy = new AntisymmetricPaddingStrategy();
        double[] signal = {5};
        double[] padded = strategy.pad(signal, 3);
        
        assertEquals(3, padded.length);
        assertEquals(5, padded[0], TOLERANCE); // Original value
        assertEquals(-5, padded[1], TOLERANCE); // Antisymmetric extension
        assertEquals(-5, padded[2], TOLERANCE); // Antisymmetric extension
    }
    
    @Test
    @DisplayName("Antisymmetric padding - preserves antisymmetry")
    void testAntisymmetricPreservesProperty() {
        var strategy = new AntisymmetricPaddingStrategy();
        double[] signal = {-2, -1, 0, 1, 2}; // Already antisymmetric around center
        double[] padded = strategy.pad(signal, 9);
        
        assertEquals(9, padded.length);
        
        // Original signal preserved
        for (int i = 0; i < signal.length; i++) {
            assertEquals(signal[i], padded[i], TOLERANCE);
        }
    }
    
    @Test
    @DisplayName("Antisymmetric padding - default constructor uses HALF_POINT")
    void testAntisymmetricDefault() {
        var strategy = new AntisymmetricPaddingStrategy();
        assertEquals("antisymmetric-half-point", strategy.name());
        assertTrue(strategy.description().contains("half-point"));
    }
    
    // ==================== Error Handling Tests ====================
    
    @Test
    @DisplayName("Null signal throws exception")
    void testNullSignalThrows() {
        var constant = new ConstantPaddingStrategy();
        var linear = new LinearExtrapolationStrategy();
        var antisym = new AntisymmetricPaddingStrategy();
        
        assertThrows(InvalidArgumentException.class, () -> constant.pad(null, 10));
        assertThrows(InvalidArgumentException.class, () -> linear.pad(null, 10));
        assertThrows(InvalidArgumentException.class, () -> antisym.pad(null, 10));
    }
    
    @Test
    @DisplayName("Empty signal throws exception")
    void testEmptySignalThrows() {
        var constant = new ConstantPaddingStrategy();
        var linear = new LinearExtrapolationStrategy();
        var antisym = new AntisymmetricPaddingStrategy();
        
        double[] empty = new double[0];
        assertThrows(InvalidArgumentException.class, () -> constant.pad(empty, 10));
        assertThrows(InvalidArgumentException.class, () -> linear.pad(empty, 10));
        assertThrows(InvalidArgumentException.class, () -> antisym.pad(empty, 10));
    }
    
    @Test
    @DisplayName("Target length less than signal length throws exception")
    void testInvalidTargetLengthThrows() {
        var constant = new ConstantPaddingStrategy();
        var linear = new LinearExtrapolationStrategy();
        var antisym = new AntisymmetricPaddingStrategy();
        
        double[] signal = {1, 2, 3, 4};
        assertThrows(InvalidArgumentException.class, () -> constant.pad(signal, 2));
        assertThrows(InvalidArgumentException.class, () -> linear.pad(signal, 2));
        assertThrows(InvalidArgumentException.class, () -> antisym.pad(signal, 2));
    }
    
    @Test
    @DisplayName("Linear extrapolation with invalid fit points throws exception")
    void testLinearInvalidFitPoints() {
        assertThrows(InvalidArgumentException.class, () -> new LinearExtrapolationStrategy(1));
        assertThrows(InvalidArgumentException.class, () -> new LinearExtrapolationStrategy(0));
        assertThrows(InvalidArgumentException.class, () -> new LinearExtrapolationStrategy(-1));
    }
    
    @Test
    @DisplayName("Trim with invalid original length throws exception")
    void testTrimInvalidLength() {
        var strategy = new ConstantPaddingStrategy();
        double[] result = {1, 2, 3, 4};
        
        assertThrows(InvalidArgumentException.class, () -> strategy.trim(result, 5));
    }
    
    // ==================== No-op Tests ====================
    
    @Test
    @DisplayName("Padding to same length returns clone")
    void testPaddingNoOp() {
        var constant = new ConstantPaddingStrategy();
        var linear = new LinearExtrapolationStrategy();
        var antisym = new AntisymmetricPaddingStrategy();
        
        double[] signal = {1, 2, 3, 4};
        
        double[] padded1 = constant.pad(signal, 4);
        double[] padded2 = linear.pad(signal, 4);
        double[] padded3 = antisym.pad(signal, 4);
        
        assertArrayEquals(signal, padded1, TOLERANCE);
        assertArrayEquals(signal, padded2, TOLERANCE);
        assertArrayEquals(signal, padded3, TOLERANCE);
        
        // Verify it's a clone, not the same array
        assertNotSame(signal, padded1);
        assertNotSame(signal, padded2);
        assertNotSame(signal, padded3);
    }
    
    @Test
    @DisplayName("Trim to same length returns same array")
    void testTrimNoOp() {
        var strategy = new ConstantPaddingStrategy();
        double[] result = {1, 2, 3, 4};
        double[] trimmed = strategy.trim(result, 4);
        
        assertArrayEquals(result, trimmed, TOLERANCE);
        assertSame(result, trimmed); // No need to clone when length is same
    }
    
    // ==================== Mode Coverage Tests ====================
    
    @ParameterizedTest
    @EnumSource(ConstantPaddingStrategy.PaddingMode.class)
    @DisplayName("All constant padding modes work correctly")
    void testAllConstantModes(ConstantPaddingStrategy.PaddingMode mode) {
        var strategy = new ConstantPaddingStrategy(mode);
        double[] signal = {10, 20};
        double[] padded = strategy.pad(signal, 4);
        
        assertEquals(4, padded.length);
        assertNotNull(strategy.name());
        assertNotNull(strategy.description());
    }
    
    @ParameterizedTest
    @EnumSource(LinearExtrapolationStrategy.PaddingMode.class)
    @DisplayName("All linear extrapolation modes work correctly")
    void testAllLinearModes(LinearExtrapolationStrategy.PaddingMode mode) {
        var strategy = new LinearExtrapolationStrategy(3, mode);
        double[] signal = {1, 2, 3};
        double[] padded = strategy.pad(signal, 5);
        
        assertEquals(5, padded.length);
        assertNotNull(strategy.name());
        assertNotNull(strategy.description());
    }
    
    @ParameterizedTest
    @EnumSource(AntisymmetricPaddingStrategy.SymmetryType.class)
    @DisplayName("All antisymmetric types work correctly")
    void testAllAntisymmetricTypes(AntisymmetricPaddingStrategy.SymmetryType type) {
        var strategy = new AntisymmetricPaddingStrategy(type);
        double[] signal = {1, 2, 3};
        double[] padded = strategy.pad(signal, 5);
        
        assertEquals(5, padded.length);
        assertNotNull(strategy.name());
        assertNotNull(strategy.description());
    }
}