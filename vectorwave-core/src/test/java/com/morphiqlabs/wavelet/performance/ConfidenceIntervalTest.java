package com.morphiqlabs.wavelet.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for ConfidenceInterval.
 * Tests statistical confidence interval calculations and properties.
 */
@DisplayName("ConfidenceInterval Test Suite")
class ConfidenceIntervalTest {
    
    private ConfidenceInterval interval;
    private static final double EPSILON = 1e-9;
    
    @BeforeEach
    void setUp() {
        interval = new ConfidenceInterval(0.9, 1.1);
    }
    
    @Test
    @DisplayName("Test basic construction and accessors")
    void testBasicConstructionAndAccessors() {
        double prediction = 10.0;
        assertEquals(9.0, interval.getLowerBound(prediction), EPSILON);
        assertEquals(11.0, interval.getUpperBound(prediction), EPSILON);
        assertTrue(interval.getConfidenceLevel() >= 0.7 && interval.getConfidenceLevel() <= 0.99);
    }
    
    @Test
    @DisplayName("Test confidence interval width")
    void testConfidenceIntervalWidth() {
        assertEquals(0.2, interval.getWidth(), EPSILON);
        
        // Test with different intervals
        ConfidenceInterval narrow = new ConfidenceInterval(0.98, 1.02);
        assertEquals(0.04, narrow.getWidth(), EPSILON);
        
        ConfidenceInterval wide = new ConfidenceInterval(0.5, 2.0);
        assertEquals(1.5, wide.getWidth(), EPSILON);
    }
    
    @Test
    @DisplayName("Test prediction with confidence bounds")
    void testPredictionWithConfidenceBounds() {
        double prediction = 5.0;
        double lowerBound = interval.getLowerBound(prediction);
        double upperBound = interval.getUpperBound(prediction);
        
        assertTrue(lowerBound < prediction, "Lower bound should be less than prediction");
        assertTrue(upperBound > prediction, "Upper bound should be greater than prediction");
        assertTrue(lowerBound < upperBound, "Lower bound should be less than upper bound");
    }
    
    @Test
    @DisplayName("Test value containment within bounds")
    void testValueContainmentWithinBounds() {
        double prediction = 10.0;
        double actual1 = 9.5; // Within bounds
        double actual2 = 8.0; // Below bounds
        double actual3 = 12.0; // Above bounds
        
        assertTrue(interval.contains(prediction, actual1), "Should contain value within bounds");
        assertFalse(interval.contains(prediction, actual2), "Should not contain value below bounds");
        assertFalse(interval.contains(prediction, actual3), "Should not contain value above bounds");
    }
    
    @Test
    @DisplayName("Test error-based updates")
    void testErrorBasedUpdates() {
        ConfidenceInterval updateInterval = new ConfidenceInterval(0.9, 1.1);
        
        // Test updating with small error
        updateInterval.updateWithError(0.05); // 5% error
        assertTrue(updateInterval.getWidth() > 0, "Width should be positive after update");
        
        // Test updating with large error
        updateInterval.updateWithError(0.2); // 20% error
        assertTrue(updateInterval.getWidth() > 0.1, "Width should be reasonable after large error");
    }
    
    @Test
    @DisplayName("Test confidence level calculation")
    void testConfidenceLevelCalculation() {
        // Test narrow interval (high confidence)
        ConfidenceInterval narrow = new ConfidenceInterval(0.95, 1.05);
        assertTrue(narrow.getConfidenceLevel() >= 0.95, "Narrow interval should have high confidence");
        
        // Test wide interval (low confidence)
        ConfidenceInterval wide = new ConfidenceInterval(0.5, 2.0);
        assertTrue(wide.getConfidenceLevel() <= 0.80, "Wide interval should have lower confidence");
    }
    
    @Test
    @DisplayName("Test copy functionality")
    void testCopyFunctionality() {
        ConfidenceInterval copy = interval.copy();
        
        assertNotNull(copy);
        assertNotSame(interval, copy, "Copy should be a different instance");
        assertEquals(interval.getWidth(), copy.getWidth(), EPSILON);
        assertEquals(interval.getConfidenceLevel(), copy.getConfidenceLevel(), EPSILON);
    }
    
    @Test
    @DisplayName("Test multiplier constraints")
    void testMultiplierConstraints() {
        // Test valid multipliers
        assertDoesNotThrow(() -> new ConfidenceInterval(0.8, 1.2));
        assertDoesNotThrow(() -> new ConfidenceInterval(0.5, 2.0));
        
        // Test invalid multipliers
        assertThrows(IllegalArgumentException.class, () -> new ConfidenceInterval(0.0, 1.1));
        assertThrows(IllegalArgumentException.class, () -> new ConfidenceInterval(1.1, 0.9));
        assertThrows(IllegalArgumentException.class, () -> new ConfidenceInterval(0.9, 3.5));
    }
    
    @Test
    @DisplayName("Test adaptive learning behavior")
    void testAdaptiveLearningBehavior() {
        ConfidenceInterval learningInterval = new ConfidenceInterval(0.9, 1.1);
        double initialWidth = learningInterval.getWidth();
        
        // Simulate consistent small errors
        for (int i = 0; i < 10; i++) {
            learningInterval.updateWithError(0.02); // 2% error
        }
        
        // Width should remain reasonable
        assertTrue(learningInterval.getWidth() >= initialWidth * 0.5);
        assertTrue(learningInterval.getWidth() <= initialWidth * 2.0);
    }
    
    @Test
    @DisplayName("Test toString output")
    void testToStringOutput() {
        String output = interval.toString();
        assertNotNull(output);
        assertTrue(output.contains("%"), "Output should contain percentage symbols");
        assertFalse(output.isEmpty(), "Output should not be empty");
    }
    
    @Test
    @DisplayName("Test serialization properties")
    void testSerializationProperties() {
        // Test that the class has serialization support
        assertTrue(interval instanceof java.io.Serializable, "Should implement Serializable");
        
        // Test basic properties after construction
        assertNotNull(interval);
        assertTrue(interval.getWidth() > 0, "Width should be positive");
        assertTrue(interval.getConfidenceLevel() > 0, "Confidence level should be positive");
    }
    
    @Test
    @DisplayName("Test extreme error scenarios")
    void testExtremeErrorScenarios() {
        ConfidenceInterval extremeInterval = new ConfidenceInterval(0.9, 1.1);
        
        // Test with very large error
        extremeInterval.updateWithError(1.0); // 100% error
        assertTrue(extremeInterval.getWidth() > 0.2, "Should handle large errors gracefully");
        
        // Test with zero error
        extremeInterval.updateWithError(0.0);
        assertTrue(extremeInterval.getWidth() > 0, "Width should remain positive even with zero error");
    }
    
    @Test
    @DisplayName("Test different prediction scenarios")
    void testDifferentPredictionScenarios() {
        // Test with small predictions
        double smallPrediction = 0.001;
        assertTrue(interval.getLowerBound(smallPrediction) > 0, "Lower bound should be positive for small predictions");
        assertTrue(interval.getUpperBound(smallPrediction) > smallPrediction, "Upper bound should exceed prediction");
        
        // Test with large predictions
        double largePrediction = 1000.0;
        assertTrue(interval.getLowerBound(largePrediction) < largePrediction, "Lower bound should be less than large prediction");
        assertTrue(interval.getUpperBound(largePrediction) > largePrediction, "Upper bound should exceed large prediction");
    }
    
    @Test
    @DisplayName("Test performance characteristics")
    void testPerformanceCharacteristics() {
        // Test that operations complete quickly
        long startTime = System.nanoTime();
        
        for (int i = 0; i < 1000; i++) {
            double prediction = i * 0.1;
            interval.getLowerBound(prediction);
            interval.getUpperBound(prediction);
            interval.contains(prediction, prediction * 1.05);
        }
        
        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1_000_000;
        
        assertTrue(durationMs < 100, "Operations should complete quickly (within 100ms)");
    }
    
    @Test
    @DisplayName("Test boundary multiplier values")
    void testBoundaryMultiplierValues() {
        // Test minimum valid multipliers
        ConfidenceInterval minInterval = new ConfidenceInterval(0.1, 1.1);
        assertNotNull(minInterval);
        assertEquals(1.0, minInterval.getWidth(), EPSILON);
        
        // Test maximum valid multipliers
        ConfidenceInterval maxInterval = new ConfidenceInterval(0.9, 3.0);
        assertNotNull(maxInterval);
        assertEquals(2.1, maxInterval.getWidth(), EPSILON);
    }
    
    @Test
    @DisplayName("Test edge cases and boundary conditions")
    void testEdgeCasesAndBoundaryConditions() {
        // Test interval with very small multipliers close to boundaries
        ConfidenceInterval closeToOne = new ConfidenceInterval(0.99, 1.01);
        assertEquals(0.02, closeToOne.getWidth(), EPSILON);
        assertTrue(closeToOne.getConfidenceLevel() >= 0.95, "Very narrow interval should have high confidence");
        
        // Test interval with large spread
        ConfidenceInterval largeSpread = new ConfidenceInterval(0.2, 2.5);
        assertEquals(2.3, largeSpread.getWidth(), EPSILON);
        assertTrue(largeSpread.getConfidenceLevel() <= 0.8, "Wide interval should have lower confidence");
        
        // Test multiple error updates don't break the interval
        ConfidenceInterval updateTest = new ConfidenceInterval(0.8, 1.2);
        for (int i = 0; i < 100; i++) {
            updateTest.updateWithError(Math.random() * 0.1);
            assertTrue(updateTest.getWidth() > 0, "Width should remain positive after update " + i);
        }
    }
}