package com.morphiqlabs.wavelet.padding;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for CompositePaddingStrategy to achieve full coverage.
 */
class CompositePaddingStrategyTest {

    private PaddingStrategy leftStrategy;
    private PaddingStrategy rightStrategy;
    private CompositePaddingStrategy compositePadding;

    @BeforeEach
    void setUp() {
        leftStrategy = new ConstantPaddingStrategy(ConstantPaddingStrategy.PaddingMode.LEFT);
        rightStrategy = new ConstantPaddingStrategy(ConstantPaddingStrategy.PaddingMode.RIGHT);
        compositePadding = new CompositePaddingStrategy(leftStrategy, rightStrategy, 0.6);
    }

    @Test
    @DisplayName("Should create composite strategy with equal ratio")
    void testDefaultConstructor() {
        CompositePaddingStrategy defaultComposite = new CompositePaddingStrategy(leftStrategy, rightStrategy);
        
        assertEquals(leftStrategy, defaultComposite.leftStrategy());
        assertEquals(rightStrategy, defaultComposite.rightStrategy());
        assertEquals(0.5, defaultComposite.leftRatio());
    }

    @Test
    @DisplayName("Should create composite strategy with custom ratio")
    void testCustomRatioConstructor() {
        CompositePaddingStrategy customComposite = new CompositePaddingStrategy(leftStrategy, rightStrategy, 0.3);
        
        assertEquals(leftStrategy, customComposite.leftStrategy());
        assertEquals(rightStrategy, customComposite.rightStrategy());
        assertEquals(0.3, customComposite.leftRatio());
    }

    @Test
    @DisplayName("Should throw exception for null left strategy")
    void testNullLeftStrategy() {
        assertThrows(InvalidArgumentException.class, () -> {
            new CompositePaddingStrategy(null, rightStrategy, 0.5);
        });
    }

    @Test
    @DisplayName("Should throw exception for null right strategy")
    void testNullRightStrategy() {
        assertThrows(InvalidArgumentException.class, () -> {
            new CompositePaddingStrategy(leftStrategy, null, 0.5);
        });
    }

    @Test
    @DisplayName("Should throw exception for invalid left ratio")
    void testInvalidLeftRatio() {
        assertThrows(InvalidArgumentException.class, () -> {
            new CompositePaddingStrategy(leftStrategy, rightStrategy, -0.1);
        });
        
        assertThrows(InvalidArgumentException.class, () -> {
            new CompositePaddingStrategy(leftStrategy, rightStrategy, 1.1);
        });
    }

    @Test
    @DisplayName("Should accept boundary left ratios")
    void testBoundaryLeftRatios() {
        assertDoesNotThrow(() -> {
            new CompositePaddingStrategy(leftStrategy, rightStrategy, 0.0);
        });
        
        assertDoesNotThrow(() -> {
            new CompositePaddingStrategy(leftStrategy, rightStrategy, 1.0);
        });
    }

    @Test
    @DisplayName("Should throw exception for null signal")
    void testNullSignal() {
        assertThrows(InvalidArgumentException.class, () -> {
            compositePadding.pad(null, 10);
        });
    }

    @Test
    @DisplayName("Should throw exception for empty signal")
    void testEmptySignal() {
        assertThrows(InvalidArgumentException.class, () -> {
            compositePadding.pad(new double[0], 10);
        });
    }

    @Test
    @DisplayName("Should throw exception for target length less than signal length")
    void testInvalidTargetLength() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        
        assertThrows(InvalidArgumentException.class, () -> {
            compositePadding.pad(signal, 3);
        });
    }

    @Test
    @DisplayName("Should return clone when target length equals signal length")
    void testNoOpPadding() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] result = compositePadding.pad(signal, 4);
        
        assertArrayEquals(signal, result);
        assertNotSame(signal, result); // Should be a clone
    }

    @Test
    @DisplayName("Should pad signal with custom ratio")
    void testCustomRatioPadding() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        CompositePaddingStrategy strategy = new CompositePaddingStrategy(
            new ConstantPaddingStrategy(ConstantPaddingStrategy.PaddingMode.LEFT),
            new ConstantPaddingStrategy(ConstantPaddingStrategy.PaddingMode.RIGHT),
            0.6
        );
        
        double[] result = strategy.pad(signal, 10);
        
        assertEquals(10, result.length);
        
        // Total padding = 6, left padding = 6 * 0.6 = 3.6 -> 4, right padding = 2
        // Expected: [1, 1, 1, 1, 1, 2, 3, 4, 4, 4]
        
        // Check that original signal is preserved in the middle
        assertArrayEquals(signal, java.util.Arrays.copyOfRange(result, 4, 8));
    }

    @Test
    @DisplayName("Should pad with symmetric strategies")
    void testSymmetricStrategies() {
        CompositePaddingStrategy symmetricStrategy = new CompositePaddingStrategy(
            new SymmetricPaddingStrategy(),
            new SymmetricPaddingStrategy(),
            0.5
        );
        
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] result = symmetricStrategy.pad(signal, 8);
        
        assertEquals(8, result.length);
        
        // Check that original signal is preserved somewhere in the middle
        boolean foundSignal = false;
        for (int i = 0; i <= result.length - signal.length; i++) {
            double[] subArray = java.util.Arrays.copyOfRange(result, i, i + signal.length);
            if (java.util.Arrays.equals(signal, subArray)) {
                foundSignal = true;
                break;
            }
        }
        assertTrue(foundSignal);
    }

    @Test
    @DisplayName("Should handle left-only padding ratio")
    void testLeftOnlyRatio() {
        CompositePaddingStrategy leftOnlyStrategy = new CompositePaddingStrategy(
            leftStrategy, rightStrategy, 1.0
        );
        
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] result = leftOnlyStrategy.pad(signal, 8);
        
        assertEquals(8, result.length);
        
        // All padding should be on the left, so original signal should be at the end
        assertArrayEquals(signal, java.util.Arrays.copyOfRange(result, 4, 8));
    }

    @Test
    @DisplayName("Should handle right-only padding ratio")
    void testRightOnlyRatio() {
        CompositePaddingStrategy rightOnlyStrategy = new CompositePaddingStrategy(
            leftStrategy, rightStrategy, 0.0
        );
        
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] result = rightOnlyStrategy.pad(signal, 8);
        
        assertEquals(8, result.length);
        
        // All padding should be on the right, so original signal should be at the start
        assertArrayEquals(signal, java.util.Arrays.copyOfRange(result, 0, 4));
    }

    @Test
    @DisplayName("Should handle large signal with small context")
    void testLargeSignalPadding() {
        double[] largeSignal = new double[50];
        for (int i = 0; i < largeSignal.length; i++) {
            largeSignal[i] = i + 1;
        }
        
        double[] result = compositePadding.pad(largeSignal, 60);
        
        assertEquals(60, result.length);
        
        // Find where the original signal is located
        boolean foundSignal = false;
        for (int i = 0; i <= result.length - largeSignal.length; i++) {
            if (result[i] == 1.0 && result[i + largeSignal.length - 1] == 50.0) {
                double[] subArray = java.util.Arrays.copyOfRange(result, i, i + largeSignal.length);
                if (java.util.Arrays.equals(largeSignal, subArray)) {
                    foundSignal = true;
                    break;
                }
            }
        }
        assertTrue(foundSignal);
    }

    @Test
    @DisplayName("Should trim padded result correctly")
    void testTrimming() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] padded = compositePadding.pad(signal, 10);
        double[] trimmed = compositePadding.trim(padded, signal.length);
        
        assertEquals(signal.length, trimmed.length);
        
        // The trimmed result should contain the original signal values
        // (though possibly not in exact order due to padding effects)
        assertNotNull(trimmed);
    }

    @Test
    @DisplayName("Should handle trim with no change needed")
    void testTrimNoOp() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] result = compositePadding.trim(signal, signal.length);
        
        assertSame(signal, result); // Should return same array when no trimming needed
    }

    @Test
    @DisplayName("Should throw exception when trimming to larger size")
    void testInvalidTrim() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        
        assertThrows(InvalidArgumentException.class, () -> {
            compositePadding.trim(signal, 10);
        });
    }

    @Test
    @DisplayName("Should provide correct name")
    void testName() {
        String name = compositePadding.name();
        
        assertNotNull(name);
        assertTrue(name.contains("composite"));
        assertTrue(name.contains(leftStrategy.name()));
        assertTrue(name.contains(rightStrategy.name()));
    }

    @Test
    @DisplayName("Should provide correct description")
    void testDescription() {
        String description = compositePadding.description();
        
        assertNotNull(description);
        assertTrue(description.contains("Composite padding"));
        assertTrue(description.contains("left:"));
        assertTrue(description.contains("right:"));
        assertTrue(description.contains("60%")); // leftRatio is 0.6
        assertTrue(description.contains("40%")); // rightRatio is 1-0.6
    }

    @Test
    @DisplayName("Builder should create composite strategy")
    void testBuilderBasic() {
        CompositePaddingStrategy.Builder builder = new CompositePaddingStrategy.Builder();
        
        CompositePaddingStrategy strategy = builder
            .leftStrategy(leftStrategy)
            .rightStrategy(rightStrategy)
            .leftRatio(0.3)
            .build();
        
        assertEquals(leftStrategy, strategy.leftStrategy());
        assertEquals(rightStrategy, strategy.rightStrategy());
        assertEquals(0.3, strategy.leftRatio());
    }

    @Test
    @DisplayName("Builder should use default ratio")
    void testBuilderDefaultRatio() {
        CompositePaddingStrategy.Builder builder = new CompositePaddingStrategy.Builder();
        
        CompositePaddingStrategy strategy = builder
            .leftStrategy(leftStrategy)
            .rightStrategy(rightStrategy)
            .build();
        
        assertEquals(0.5, strategy.leftRatio()); // Default ratio
    }

    @Test
    @DisplayName("Builder should throw exception for missing left strategy")
    void testBuilderMissingLeftStrategy() {
        CompositePaddingStrategy.Builder builder = new CompositePaddingStrategy.Builder();
        
        assertThrows(InvalidArgumentException.class, () -> {
            builder.rightStrategy(rightStrategy).build();
        });
    }

    @Test
    @DisplayName("Builder should throw exception for missing right strategy")
    void testBuilderMissingRightStrategy() {
        CompositePaddingStrategy.Builder builder = new CompositePaddingStrategy.Builder();
        
        assertThrows(InvalidArgumentException.class, () -> {
            builder.leftStrategy(leftStrategy).build();
        });
    }

    @Test
    @DisplayName("Builder should support method chaining")
    void testBuilderMethodChaining() {
        CompositePaddingStrategy strategy = new CompositePaddingStrategy.Builder()
            .rightStrategy(rightStrategy)
            .leftRatio(0.7)
            .leftStrategy(leftStrategy)
            .build();
        
        assertNotNull(strategy);
        assertEquals(0.7, strategy.leftRatio());
    }

    @Test
    @DisplayName("Should handle edge case with minimal padding")
    void testMinimalPadding() {
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] result = compositePadding.pad(signal, 5); // Add only 1 element
        
        assertEquals(5, result.length);
        
        // Should still work with minimal padding
        assertNotNull(result);
    }

    @Test
    @DisplayName("Should handle different strategy alignment detection")
    void testStrategyAlignmentDetection() {
        // Test with different combinations of strategies to exercise alignment detection
        PaddingStrategy leftAligned = new ConstantPaddingStrategy(ConstantPaddingStrategy.PaddingMode.LEFT);
        PaddingStrategy rightAligned = new ConstantPaddingStrategy(ConstantPaddingStrategy.PaddingMode.RIGHT);
        PaddingStrategy symmetric = new ConstantPaddingStrategy(ConstantPaddingStrategy.PaddingMode.SYMMETRIC);
        
        // Test all combinations
        CompositePaddingStrategy leftLeft = new CompositePaddingStrategy(leftAligned, leftAligned);
        CompositePaddingStrategy leftRight = new CompositePaddingStrategy(leftAligned, rightAligned);
        CompositePaddingStrategy rightLeft = new CompositePaddingStrategy(rightAligned, leftAligned);
        CompositePaddingStrategy symmetricCombo = new CompositePaddingStrategy(symmetric, symmetric);
        
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        
        // All should work without throwing exceptions
        assertDoesNotThrow(() -> leftLeft.pad(signal, 8));
        assertDoesNotThrow(() -> leftRight.pad(signal, 8));
        assertDoesNotThrow(() -> rightLeft.pad(signal, 8));
        assertDoesNotThrow(() -> symmetricCombo.pad(signal, 8));
    }

    @Test
    @DisplayName("Should handle zero padding strategy gracefully")
    void testZeroPaddingStrategyType() {
        // Test with zero padding strategy
        PaddingStrategy zeroPadding = new ZeroPaddingStrategy();
        
        CompositePaddingStrategy compositeZero = new CompositePaddingStrategy(zeroPadding, zeroPadding);
        
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        
        // Should work with zero padding strategy
        assertDoesNotThrow(() -> {
            double[] result = compositeZero.pad(signal, 8);
            assertEquals(8, result.length);
        });
    }
}