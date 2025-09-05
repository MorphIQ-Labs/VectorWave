package com.morphiqlabs.wavelet.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ToleranceConstants.
 */
@DisplayName("ToleranceConstants Tests")
class ToleranceConstantsTest {

    @Test
    @DisplayName("Should provide correct tolerance values")
    void testToleranceValues() {
        assertEquals(1e-10, ToleranceConstants.DEFAULT_TOLERANCE);
        assertEquals(1e-12, ToleranceConstants.ENERGY_TOLERANCE);
        assertEquals(1e-14, ToleranceConstants.ORTHOGONALITY_TOLERANCE);
        assertEquals(0.01, ToleranceConstants.EXTREME_VALUE_RELATIVE_TOLERANCE);
        assertEquals(0.2, ToleranceConstants.BOUNDARY_EFFECT_TOLERANCE);
        assertEquals(0.01, ToleranceConstants.PEAK_SHARPNESS_TOLERANCE);
    }

    @Test
    @DisplayName("Should provide meaningful explanations for tolerance values")
    void testExplainTolerance() {
        String explanation = ToleranceConstants.explainTolerance(ToleranceConstants.DEFAULT_TOLERANCE);
        assertTrue(explanation.contains("Standard floating-point comparison"));
        
        explanation = ToleranceConstants.explainTolerance(ToleranceConstants.EXTREME_VALUE_RELATIVE_TOLERANCE);
        assertTrue(explanation.contains("1% relative error tolerance"));
        
        explanation = ToleranceConstants.explainTolerance(ToleranceConstants.BOUNDARY_EFFECT_TOLERANCE);
        assertTrue(explanation.contains("20% tolerance for boundary"));
        
        explanation = ToleranceConstants.explainTolerance(0.123);
        assertTrue(explanation.contains("Custom tolerance value: 0.123"));
    }

}