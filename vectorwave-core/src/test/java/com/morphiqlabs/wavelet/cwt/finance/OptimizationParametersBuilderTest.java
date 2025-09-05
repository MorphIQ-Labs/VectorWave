package com.morphiqlabs.wavelet.cwt.finance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test coverage for OptimizationParameters.Builder class.
 */
class OptimizationParametersBuilderTest {
    
    @Test
    @DisplayName("Builder should set crash detection parameters")
    void testCrashDetectionParameters() {
        OptimizationParameters params = new OptimizationParameters.Builder()
            .crashPaulOrder(5)
            .crashDogOrder(3)
            .crashParameters(0.6, 2.0)
            .crashScaleRange(2.0, 15.0)
            .build();
        
        assertNotNull(params);
        assertEquals(5, params.getCrashPaulOrder());
        assertEquals(3, params.getCrashDogOrder());
        assertEquals(0.6, params.getCrashThresholdFactor());
        assertEquals(2.0, params.getCrashSeverityExponent());
        assertArrayEquals(new double[]{2.0, 15.0}, params.getCrashScaleRange());
    }
    
    @Test
    @DisplayName("Builder should validate crash parameters")
    void testCrashParameterValidation() {
        OptimizationParameters.Builder builder = new OptimizationParameters.Builder();
        
        // Test invalid Paul order
        assertThrows(IllegalArgumentException.class, () -> builder.crashPaulOrder(0));
        assertThrows(IllegalArgumentException.class, () -> builder.crashPaulOrder(-1));
        
        // Test invalid DOG order
        assertThrows(IllegalArgumentException.class, () -> builder.crashDogOrder(0));
        assertThrows(IllegalArgumentException.class, () -> builder.crashDogOrder(-5));
        
        // Test invalid threshold factor
        assertThrows(IllegalArgumentException.class, () -> builder.crashParameters(0, 1.5));
        assertThrows(IllegalArgumentException.class, () -> builder.crashParameters(1.0, 1.5));
        assertThrows(IllegalArgumentException.class, () -> builder.crashParameters(-0.5, 1.5));
        
        // Test invalid severity exponent
        assertThrows(IllegalArgumentException.class, () -> builder.crashParameters(0.5, 0));
        assertThrows(IllegalArgumentException.class, () -> builder.crashParameters(0.5, -1));
        
        // Test invalid scale range
        assertThrows(IllegalArgumentException.class, () -> builder.crashScaleRange(0, 10));
        assertThrows(IllegalArgumentException.class, () -> builder.crashScaleRange(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> builder.crashScaleRange(10, 10));
        assertThrows(IllegalArgumentException.class, () -> builder.crashScaleRange(10, 5));
    }
    
    @Test
    @DisplayName("Builder should set volatility analysis parameters")
    void testVolatilityParameters() {
        OptimizationParameters params = new OptimizationParameters.Builder()
            .volatilityPaulOrder(4)
            .volatilityDogOrder(3)
            .volatilityParameters(0.4, 1.2)
            .volatilityScaleRange(2.0, 35.0)
            .build();
        
        assertNotNull(params);
        assertEquals(4, params.getVolatilityPaulOrder());
        assertEquals(3, params.getVolatilityDogOrder());
        assertEquals(0.4, params.getVolatilityThresholdFactor());
        assertEquals(1.2, params.getVolatilityExponent());
        assertArrayEquals(new double[]{2.0, 35.0}, params.getVolatilityScaleRange());
    }
    
    @Test
    @DisplayName("Builder should validate volatility parameters")
    void testVolatilityParameterValidation() {
        OptimizationParameters.Builder builder = new OptimizationParameters.Builder();
        
        // Test invalid Paul order
        assertThrows(IllegalArgumentException.class, () -> builder.volatilityPaulOrder(0));
        assertThrows(IllegalArgumentException.class, () -> builder.volatilityPaulOrder(-1));
        
        // Test invalid DOG order
        assertThrows(IllegalArgumentException.class, () -> builder.volatilityDogOrder(0));
        assertThrows(IllegalArgumentException.class, () -> builder.volatilityDogOrder(-3));
        
        // Test invalid threshold and exponent
        assertThrows(IllegalArgumentException.class, () -> builder.volatilityParameters(0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> builder.volatilityParameters(1.0, 1.0));
        assertThrows(IllegalArgumentException.class, () -> builder.volatilityParameters(0.5, 0));
        assertThrows(IllegalArgumentException.class, () -> builder.volatilityParameters(0.5, -1));
        
        // Test invalid scale range
        assertThrows(IllegalArgumentException.class, () -> builder.volatilityScaleRange(0, 20));
        assertThrows(IllegalArgumentException.class, () -> builder.volatilityScaleRange(-1, 20));
        assertThrows(IllegalArgumentException.class, () -> builder.volatilityScaleRange(20, 20));
        assertThrows(IllegalArgumentException.class, () -> builder.volatilityScaleRange(20, 10));
    }
    
    @Test
    @DisplayName("Builder should set cycle detection parameters")
    void testCycleDetectionParameters() {
        OptimizationParameters params = new OptimizationParameters.Builder()
            .cycleShannonParameters(3, 4)
            .cycleParameters(0.25, 2.5)
            .cycleScaleRange(10.0, 60.0)
            .build();
        
        assertNotNull(params);
        assertEquals(3, params.getCycleShannonFb());
        assertEquals(4, params.getCycleShannonFc());
        assertEquals(0.25, params.getCycleThresholdFactor());
        assertEquals(2.5, params.getCycleExponent());
        assertArrayEquals(new double[]{10.0, 60.0}, params.getCycleScaleRange());
    }
    
    @Test
    @DisplayName("Builder should validate cycle parameters")
    void testCycleParameterValidation() {
        OptimizationParameters.Builder builder = new OptimizationParameters.Builder();
        
        // Test invalid Shannon parameters
        assertThrows(IllegalArgumentException.class, () -> builder.cycleShannonParameters(0, 3));
        assertThrows(IllegalArgumentException.class, () -> builder.cycleShannonParameters(3, 0));
        assertThrows(IllegalArgumentException.class, () -> builder.cycleShannonParameters(-1, 3));
        assertThrows(IllegalArgumentException.class, () -> builder.cycleShannonParameters(3, -1));
        
        // Test invalid threshold and exponent
        assertThrows(IllegalArgumentException.class, () -> builder.cycleParameters(0, 2.0));
        assertThrows(IllegalArgumentException.class, () -> builder.cycleParameters(1.0, 2.0));
        assertThrows(IllegalArgumentException.class, () -> builder.cycleParameters(0.3, 0));
        assertThrows(IllegalArgumentException.class, () -> builder.cycleParameters(0.3, -1));
        
        // Test invalid scale range
        assertThrows(IllegalArgumentException.class, () -> builder.cycleScaleRange(0, 50));
        assertThrows(IllegalArgumentException.class, () -> builder.cycleScaleRange(-5, 50));
        assertThrows(IllegalArgumentException.class, () -> builder.cycleScaleRange(50, 50));
        assertThrows(IllegalArgumentException.class, () -> builder.cycleScaleRange(50, 30));
    }
    
    @Test
    @DisplayName("Builder should set signal generation parameters")
    void testSignalGenerationParameters() {
        OptimizationParameters params = new OptimizationParameters.Builder()
            .signalPaulOrder(6)
            .signalDogOrder(4)
            .signalParameters(0.35, 1.8)
            .signalScaleRange(3.0, 25.0)
            .build();
        
        assertNotNull(params);
        assertEquals(6, params.getSignalPaulOrder());
        assertEquals(4, params.getSignalDogOrder());
        assertEquals(0.35, params.getSignalThresholdFactor());
        assertEquals(1.8, params.getSignalExponent());
        assertArrayEquals(new double[]{3.0, 25.0}, params.getSignalScaleRange());
    }
    
    @Test
    @DisplayName("Builder should validate signal parameters")
    void testSignalParameterValidation() {
        OptimizationParameters.Builder builder = new OptimizationParameters.Builder();
        
        // Test invalid Paul order
        assertThrows(IllegalArgumentException.class, () -> builder.signalPaulOrder(0));
        assertThrows(IllegalArgumentException.class, () -> builder.signalPaulOrder(-2));
        
        // Test invalid DOG order
        assertThrows(IllegalArgumentException.class, () -> builder.signalDogOrder(0));
        assertThrows(IllegalArgumentException.class, () -> builder.signalDogOrder(-4));
        
        // Test invalid threshold and exponent
        assertThrows(IllegalArgumentException.class, () -> builder.signalParameters(0, 1.5));
        assertThrows(IllegalArgumentException.class, () -> builder.signalParameters(1.0, 1.5));
        assertThrows(IllegalArgumentException.class, () -> builder.signalParameters(0.4, 0));
        assertThrows(IllegalArgumentException.class, () -> builder.signalParameters(0.4, -2));
        
        // Test invalid scale range
        assertThrows(IllegalArgumentException.class, () -> builder.signalScaleRange(0, 30));
        assertThrows(IllegalArgumentException.class, () -> builder.signalScaleRange(-2, 30));
        assertThrows(IllegalArgumentException.class, () -> builder.signalScaleRange(30, 30));
        assertThrows(IllegalArgumentException.class, () -> builder.signalScaleRange(30, 15));
    }
    
    @Test
    @DisplayName("Builder should handle fluent API chaining")
    void testFluentAPIChaining() {
        // Test that all methods return the builder for chaining
        OptimizationParameters params = new OptimizationParameters.Builder()
            .crashPaulOrder(5)
            .crashDogOrder(2)
            .crashParameters(0.5, 1.5)
            .crashScaleRange(1.0, 10.0)
            .volatilityPaulOrder(3)
            .volatilityDogOrder(2)
            .volatilityParameters(0.3, 1.0)
            .volatilityScaleRange(1.0, 30.0)
            .cycleShannonParameters(2, 3)
            .cycleParameters(0.2, 2.0)
            .cycleScaleRange(5.0, 50.0)
            .signalPaulOrder(4)
            .signalDogOrder(2)
            .signalParameters(0.4, 1.5)
            .signalScaleRange(2.0, 20.0)
            .build();
        
        assertNotNull(params);
        
        // Verify all parameters were set
        assertEquals(5, params.getCrashPaulOrder());
        assertEquals(3, params.getVolatilityPaulOrder());
        assertEquals(2, params.getCycleShannonFb());
        assertEquals(4, params.getSignalPaulOrder());
    }
    
    @Test
    @DisplayName("Builder should preserve defaults when not overridden")
    void testDefaultValues() {
        // Create with defaults
        OptimizationParameters defaultParams = new OptimizationParameters.Builder().build();
        
        assertNotNull(defaultParams);
        
        // Check some default values
        assertEquals(4, defaultParams.getCrashPaulOrder()); // default
        assertEquals(2, defaultParams.getCrashDogOrder()); // default
        assertEquals(0.5, defaultParams.getCrashThresholdFactor()); // default
        assertEquals(1.5, defaultParams.getCrashSeverityExponent()); // default
        assertArrayEquals(new double[]{1.0, 10.0}, defaultParams.getCrashScaleRange()); // default
        
        // Create with partial overrides
        OptimizationParameters partialParams = new OptimizationParameters.Builder()
            .crashPaulOrder(6)
            .volatilityDogOrder(3)
            .build();
        
        assertEquals(6, partialParams.getCrashPaulOrder()); // overridden
        assertEquals(2, partialParams.getCrashDogOrder()); // default
        assertEquals(3, partialParams.getVolatilityDogOrder()); // overridden
        assertEquals(3, partialParams.getVolatilityPaulOrder()); // default
    }
    
    @Test
    @DisplayName("Builder copy constructor should duplicate source")
    void testCopyConstructor() {
        // Create original
        OptimizationParameters original = new OptimizationParameters.Builder()
            .crashPaulOrder(7)
            .crashDogOrder(3)
            .crashParameters(0.7, 2.5)
            .crashScaleRange(2.0, 12.0)
            .volatilityPaulOrder(5)
            .volatilityDogOrder(4)
            .build();
        
        // Create copy and modify
        OptimizationParameters modified = new OptimizationParameters.Builder(original)
            .crashPaulOrder(8)  // Override one value
            .signalPaulOrder(6) // Add new value
            .build();
        
        // Check original values are preserved except overridden ones
        assertEquals(7, original.getCrashPaulOrder());
        assertEquals(8, modified.getCrashPaulOrder()); // overridden
        assertEquals(3, modified.getCrashDogOrder()); // copied
        assertEquals(0.7, modified.getCrashThresholdFactor()); // copied
        assertEquals(2.5, modified.getCrashSeverityExponent()); // copied
        assertArrayEquals(new double[]{2.0, 12.0}, modified.getCrashScaleRange()); // copied
        assertEquals(5, modified.getVolatilityPaulOrder()); // copied
        assertEquals(6, modified.getSignalPaulOrder()); // new
    }
}