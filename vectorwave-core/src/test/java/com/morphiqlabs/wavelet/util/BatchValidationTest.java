package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.exception.InvalidSignalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for BatchValidation.
 */
@DisplayName("BatchValidation Tests")
class BatchValidationTest {
    
    // === validateMultiLevelSignals Tests ===
    
    @Test
    @DisplayName("validateMultiLevelSignals should accept valid signals")
    void testValidateMultiLevelSignalsValid() {
        // For a 16-length signal:
        // Level 0: 16/2 = 8
        // Level 1: 8/2 = 4  
        // Level 2: 4/2 = 2
        double[][] signals = {
            new double[8],   // level 0
            new double[4],   // level 1
            new double[2]    // level 2
        };
        String[] names = {"level0", "level1", "level2"};
        int[] expectedLengths = {8, 4, 2};
        
        assertDoesNotThrow(() -> 
            BatchValidation.validateMultiLevelSignals(signals, names, expectedLengths));
    }
    
    @Test
    @DisplayName("validateMultiLevelSignals should reject null signals array")
    void testValidateMultiLevelSignalsNullArray() {
        String[] names = {"level0"};
        int[] expectedLengths = {4};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> BatchValidation.validateMultiLevelSignals(null, names, expectedLengths));
        
        assertTrue(exception.getMessage().contains("Signals array cannot be null"));
    }
    
    @Test
    @DisplayName("validateMultiLevelSignals should reject mismatched parameter names")
    void testValidateMultiLevelSignalsMismatchedNames() {
        double[][] signals = {{1.0, 2.0}, {3.0, 4.0}};
        String[] names = {"level0"}; // Wrong length
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> BatchValidation.validateMultiLevelSignals(signals, names, null));
        
        assertTrue(exception.getMessage().contains("Parameter names must match signals length"));
    }
    
    @Test
    @DisplayName("validateMultiLevelSignals should reject null signal in array")
    void testValidateMultiLevelSignalsNullSignal() {
        double[][] signals = {{1.0, 2.0}, null, {3.0, 4.0}};
        String[] names = {"level0", "level1", "level2"};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> BatchValidation.validateMultiLevelSignals(signals, names, null));
        
        assertTrue(exception.getMessage().contains("level1"));
        assertTrue(exception.getMessage().contains("null"));
    }
    
    @Test
    @DisplayName("validateMultiLevelSignals should reject empty signal")
    void testValidateMultiLevelSignalsEmptySignal() {
        double[][] signals = {{1.0, 2.0}, {}, {3.0, 4.0}};
        String[] names = {"level0", "level1", "level2"};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> BatchValidation.validateMultiLevelSignals(signals, names, null));
        
        assertTrue(exception.getMessage().contains("level1"));
        assertTrue(exception.getMessage().contains("empty"));
    }
    
    @Test
    @DisplayName("validateMultiLevelSignals should validate expected lengths")
    void testValidateMultiLevelSignalsCheckLengths() {
        double[][] signals = {{1.0, 2.0}, {3.0, 4.0, 5.0}, {6.0, 7.0}};
        String[] names = {"level0", "level1", "level2"};
        int[] expectedLengths = {2, 4, 2}; // level1 expects 4 but has 3
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> BatchValidation.validateMultiLevelSignals(signals, names, expectedLengths));
        
        assertTrue(exception.getMessage().contains("level1"));
        assertTrue(exception.getMessage().contains("Expected: 4, actual: 3"));
    }
    
    @Test
    @DisplayName("validateMultiLevelSignals should check finite values")
    void testValidateMultiLevelSignalsNonFinite() {
        double[][] signals = {{1.0, 2.0}, {3.0, Double.NaN}, {5.0, 6.0}};
        String[] names = {"level0", "level1", "level2"};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> BatchValidation.validateMultiLevelSignals(signals, names, null));
        
        assertTrue(exception.getMessage().contains("level1"));
        assertTrue(exception.getMessage().contains("NaN") || exception.getMessage().contains("index 1"));
    }
    
    // === validateMultiLevelCoefficients Tests ===
    
    @Test
    @DisplayName("validateMultiLevelCoefficients should accept valid coefficients")
    void testValidateMultiLevelCoefficientsValid() {
        double[][] approx = {{1.0, 2.0}, {3.0, 4.0}};
        double[][] detail = {{5.0, 6.0}, {7.0, 8.0}};
        String[] levelNames = {"Level 0", "Level 1"};
        
        assertDoesNotThrow(() -> 
            BatchValidation.validateMultiLevelCoefficients(approx, detail, levelNames));
    }
    
    @Test
    @DisplayName("validateMultiLevelCoefficients should reject null arrays")
    void testValidateMultiLevelCoefficientsNullArrays() {
        double[][] detail = {{1.0, 2.0}};
        String[] levelNames = {"Level 0"};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> BatchValidation.validateMultiLevelCoefficients(null, detail, levelNames));
        
        assertTrue(exception.getMessage().contains("Coefficient arrays cannot be null"));
    }
    
    @Test
    @DisplayName("validateMultiLevelCoefficients should reject mismatched levels")
    void testValidateMultiLevelCoefficientsMismatchedLevels() {
        double[][] approx = {{1.0, 2.0}, {3.0, 4.0}};
        double[][] detail = {{5.0, 6.0}}; // One level instead of two
        String[] levelNames = {"Level 0", "Level 1"};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> BatchValidation.validateMultiLevelCoefficients(approx, detail, levelNames));
        
        assertTrue(exception.getMessage().contains("same number of levels"));
    }
    
    @Test
    @DisplayName("validateMultiLevelCoefficients should reject mismatched coefficient lengths")
    void testValidateMultiLevelCoefficientsMismatchedLengths() {
        double[][] approx = {{1.0, 2.0}, {3.0, 4.0, 5.0}}; // Second level has 3 elements
        double[][] detail = {{5.0, 6.0}, {7.0, 8.0}}; // Second level has 2 elements
        String[] levelNames = {"Level 0", "Level 1"};
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> BatchValidation.validateMultiLevelCoefficients(approx, detail, levelNames));
        
        assertTrue(exception.getMessage().contains("Approximation and detail coefficient arrays must have the same length") ||
                   exception.getMessage().contains("Level 1"));
    }
    
    // === validateLevelConsistency Tests ===
    
    @Test
    @DisplayName("validateLevelConsistency should accept valid level progression")
    void testValidateLevelConsistencyValid() {
        double[][] levels = {
            new double[8],  // Level 0: 16/2 = 8
            new double[4],  // Level 1: 8/2 = 4
            new double[2]   // Level 2: 4/2 = 2
        };
        
        assertDoesNotThrow(() -> 
            BatchValidation.validateLevelConsistency(levels, 16));
    }
    
    @Test
    @DisplayName("validateLevelConsistency should reject incorrect level length")
    void testValidateLevelConsistencyIncorrectLength() {
        double[][] levels = {
            new double[8],  // Level 0: correct (16/2 = 8)
            new double[3],  // Level 1: incorrect (should be 4)
            new double[2]   // Level 2: would be correct if level 1 was correct
        };
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> BatchValidation.validateLevelConsistency(levels, 16));
        
        assertTrue(exception.getMessage().contains("Level 1"));
        assertTrue(exception.getMessage().contains("Expected: 4, actual: 3"));
    }
    
    @Test
    @DisplayName("validateLevelConsistency should reject too many levels")
    void testValidateLevelConsistencyTooManyLevels() {
        double[][] levels = {
            new double[4],  // Level 0: 8/2 = 4
            new double[2],  // Level 1: 4/2 = 2
            new double[1],  // Level 2: 2/2 = 1 (below MIN_DECOMPOSITION_SIZE)
            new double[1]   // Level 3: would require 0.5 length
        };
        
        InvalidSignalException exception = assertThrows(InvalidSignalException.class,
            () -> BatchValidation.validateLevelConsistency(levels, 8));
        
        assertTrue(exception.getMessage().contains("Too many decomposition levels"));
    }
    
    // === computeExpectedLengths Tests ===
    
    @Test
    @DisplayName("computeExpectedLengths should compute correct lengths")
    void testComputeExpectedLengths() {
        int[] lengths = BatchValidation.computeExpectedLengths(32, 4);
        
        assertArrayEquals(new int[]{16, 8, 4, 2}, lengths);
    }
    
    @Test
    @DisplayName("computeExpectedLengths should handle single level")
    void testComputeExpectedLengthsSingleLevel() {
        int[] lengths = BatchValidation.computeExpectedLengths(64, 1);
        
        assertArrayEquals(new int[]{32}, lengths);
    }
    
    @Test
    @DisplayName("Utility class should not be instantiable")
    void testPrivateConstructor() throws Exception {
        // Use reflection to verify private constructor
        java.lang.reflect.Constructor<BatchValidation> constructor = 
            BatchValidation.class.getDeclaredConstructor();
        
        assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()),
            "Constructor should be private");
    }
}