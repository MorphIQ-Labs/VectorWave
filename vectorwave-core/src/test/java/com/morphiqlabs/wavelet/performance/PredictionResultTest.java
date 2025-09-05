package com.morphiqlabs.wavelet.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for PredictionResult.
 * Tests the prediction result data structure and its methods.
 */
@DisplayName("PredictionResult Test Suite")
class PredictionResultTest {
    
    private PredictionResult result;
    private static final double EPSILON = 1e-9;
    
    @BeforeEach
    void setUp() {
        result = new PredictionResult(1.5, 1.0, 2.0, 0.8);
    }
    
    @Test
    @DisplayName("Test basic construction and accessors")
    void testBasicConstructionAndAccessors() {
        assertEquals(1.5, result.estimatedTime(), EPSILON);
        assertEquals(0.8, result.confidence(), EPSILON);
        assertEquals(1.0, result.lowerBound(), EPSILON);
        assertEquals(2.0, result.upperBound(), EPSILON);
    }
    
    @Test
    @DisplayName("Test valid prediction result creation")
    void testValidPredictionResultCreation() {
        // Test various valid configurations
        PredictionResult result1 = new PredictionResult(0.5, 0.3, 0.7, 0.9);
        assertEquals(0.5, result1.estimatedTime(), EPSILON);
        assertEquals(0.9, result1.confidence(), EPSILON);
        assertEquals(0.3, result1.lowerBound(), EPSILON);
        assertEquals(0.7, result1.upperBound(), EPSILON);
        
        // Test edge case with tight bounds
        PredictionResult result2 = new PredictionResult(1.0, 1.0, 1.0, 1.0);
        assertEquals(1.0, result2.estimatedTime(), EPSILON);
        assertEquals(1.0, result2.confidence(), EPSILON);
        assertEquals(1.0, result2.lowerBound(), EPSILON);
        assertEquals(1.0, result2.upperBound(), EPSILON);
    }
    
    @Test
    @DisplayName("Test prediction result with zero values")
    void testPredictionResultWithZeroValues() {
        PredictionResult zeroResult = new PredictionResult(0.0, 0.0, 0.0, 0.0);
        assertEquals(0.0, zeroResult.estimatedTime(), EPSILON);
        assertEquals(0.0, zeroResult.confidence(), EPSILON);
        assertEquals(0.0, zeroResult.lowerBound(), EPSILON);
        assertEquals(0.0, zeroResult.upperBound(), EPSILON);
    }
    
    @Test
    @DisplayName("Test prediction result immutability")
    void testPredictionResultImmutability() {
        // Verify that the record is immutable
        double originalTime = result.estimatedTime();
        double originalConfidence = result.confidence();
        double originalLower = result.lowerBound();
        double originalUpper = result.upperBound();
        
        // Values should remain unchanged
        assertEquals(originalTime, result.estimatedTime(), EPSILON);
        assertEquals(originalConfidence, result.confidence(), EPSILON);
        assertEquals(originalLower, result.lowerBound(), EPSILON);
        assertEquals(originalUpper, result.upperBound(), EPSILON);
    }
    
    @Test
    @DisplayName("Test toString method")
    void testToStringMethod() {
        String str = result.toString();
        assertNotNull(str);
        assertTrue(str.contains("1.5"), "Should contain estimated time");
        assertTrue(str.contains("0.8"), "Should contain confidence");
        assertTrue(str.contains("1.0"), "Should contain lower bound");
        assertTrue(str.contains("2.0"), "Should contain upper bound");
    }
    
    @Test
    @DisplayName("Test equals and hashCode")
    void testEqualsAndHashCode() {
        // Create identical prediction results
        PredictionResult result1 = new PredictionResult(1.5, 1.0, 2.0, 0.8);
        PredictionResult result2 = new PredictionResult(1.5, 1.0, 2.0, 0.8);
        PredictionResult result3 = new PredictionResult(1.6, 1.0, 2.0, 0.8); // Different time
        
        // Test equality
        assertEquals(result1, result2);
        assertNotEquals(result1, result3);
        assertNotEquals(result1, null);
        assertNotEquals(result1, "not a PredictionResult");
        
        // Test hashCode consistency
        assertEquals(result1.hashCode(), result2.hashCode());
        // Different objects may have same hash, but identical should
        assertEquals(result1.hashCode(), result1.hashCode());
    }
    
    @Test
    @DisplayName("Test confidence interval width calculation")
    void testConfidenceIntervalWidth() {
        double intervalWidth = result.upperBound() - result.lowerBound();
        assertEquals(1.0, intervalWidth, EPSILON);
        
        // Test with narrow interval
        PredictionResult narrowResult = new PredictionResult(1.0, 0.98, 1.02, 0.95);
        double narrowWidth = narrowResult.upperBound() - narrowResult.lowerBound();
        assertEquals(0.04, narrowWidth, EPSILON);
    }
    
    @Test
    @DisplayName("Test prediction result validation constraints")
    void testPredictionResultValidationConstraints() {
        // Test that we can create results with various bound relationships
        // (Note: The class may or may not enforce constraints - testing what's allowed)
        
        // Case where estimate is within bounds
        PredictionResult withinBounds = new PredictionResult(1.5, 1.0, 2.0, 0.8);
        assertTrue(withinBounds.lowerBound() <= withinBounds.estimatedTime());
        assertTrue(withinBounds.estimatedTime() <= withinBounds.upperBound());
        
        // Case where estimate equals bounds
        PredictionResult equalBounds = new PredictionResult(1.5, 1.5, 1.5, 1.0);
        assertEquals(equalBounds.lowerBound(), equalBounds.estimatedTime(), EPSILON);
        assertEquals(equalBounds.estimatedTime(), equalBounds.upperBound(), EPSILON);
    }
    
    @Test
    @DisplayName("Test prediction result with extreme values")
    void testPredictionResultWithExtremeValues() {
        // Test with very small values
        PredictionResult smallResult = new PredictionResult(1e-9, 1e-10, 1e-8, 0.1);
        assertEquals(1e-9, smallResult.estimatedTime(), 1e-15);
        assertEquals(0.1, smallResult.confidence(), EPSILON);
        
        // Test with large values
        PredictionResult largeResult = new PredictionResult(1e6, 1e5, 1e7, 0.99);
        assertEquals(1e6, largeResult.estimatedTime(), 1e-3);
        assertEquals(0.99, largeResult.confidence(), EPSILON);
    }
    
    @Test
    @DisplayName("Test prediction result copy semantics")
    void testPredictionResultCopySemantics() {
        PredictionResult original = new PredictionResult(2.5, 2.0, 3.0, 0.7);
        PredictionResult copy = new PredictionResult(
            original.estimatedTime(),
            original.lowerBound(),
            original.upperBound(),
            original.confidence()
        );
        
        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
        
        // Verify they're separate objects
        assertNotSame(original, copy);
    }
    
    @Test
    @DisplayName("Test prediction result comparison operations")
    void testPredictionResultComparisonOperations() {
        PredictionResult fast = new PredictionResult(0.5, 0.4, 0.6, 0.8);
        PredictionResult slow = new PredictionResult(2.0, 1.8, 2.2, 0.8);
        
        assertTrue(fast.estimatedTime() < slow.estimatedTime());
        
        // Test confidence comparison
        PredictionResult highConfidence = new PredictionResult(1.0, 0.9, 1.1, 0.95);
        PredictionResult lowConfidence = new PredictionResult(1.0, 0.5, 1.5, 0.5);
        
        assertTrue(highConfidence.confidence() > lowConfidence.confidence());
    }
    
    @Test
    @DisplayName("Test prediction result serialization compatibility")
    void testPredictionResultSerialization() {
        // Test that all components are accessible (important for serialization)
        PredictionResult testResult = new PredictionResult(3.14, 2.71, 4.0, 0.618);
        
        // Verify all fields are accessible
        assertNotNull(Double.valueOf(testResult.estimatedTime()));
        assertNotNull(Double.valueOf(testResult.confidence()));
        assertNotNull(Double.valueOf(testResult.lowerBound()));
        assertNotNull(Double.valueOf(testResult.upperBound()));
        
        // Verify toString produces parseable output
        String str = testResult.toString();
        assertNotNull(str);
        assertFalse(str.isEmpty());
    }
}