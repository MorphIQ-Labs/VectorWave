package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test coverage for WaveletValidationUtils utility class.
 * Currently has 0% coverage - targeting 100% to improve overall project coverage.
 */
class WaveletValidationUtilsTest {

    @Test
    @DisplayName("Should prevent instantiation with AssertionError")
    void testConstructorThrowsAssertionError() throws Exception {
        Constructor<WaveletValidationUtils> constructor = 
            WaveletValidationUtils.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        
        Exception exception = assertThrows(Exception.class, constructor::newInstance);
        assertTrue(exception.getCause() instanceof AssertionError,
            "Constructor should throw AssertionError wrapped in InvocationTargetException");
    }

    @Test
    @DisplayName("Should validate non-null wavelets successfully")
    void testValidateWaveletNotNull() {
        Wavelet wavelet = WaveletRegistry.getWavelet(WaveletName.HAAR);
        
        // Should not throw for valid wavelet
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateWaveletNotNull(wavelet, "testWavelet"));
    }

    @Test
    @DisplayName("Should throw InvalidArgumentException for null wavelet")
    void testValidateWaveletNotNullThrows() {
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
            () -> WaveletValidationUtils.validateWaveletNotNull(null, "myWavelet"));
        
        assertTrue(exception.getMessage().contains("myWavelet"),
            "Exception message should contain parameter name");
    }

    @Test
    @DisplayName("Should validate discrete wavelets successfully")
    void testValidateDiscreteWavelet() {
        DiscreteWavelet haar = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.HAAR);
        DiscreteWavelet db4 = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.DB4);
        
        // Should not throw for valid discrete wavelets
        assertDoesNotThrow(() -> WaveletValidationUtils.validateDiscreteWavelet(haar));
        assertDoesNotThrow(() -> WaveletValidationUtils.validateDiscreteWavelet(db4));
    }

    @Test
    @DisplayName("Should throw InvalidArgumentException for null wavelet in discrete validation")
    void testValidateDiscreteWaveletNullThrows() {
        assertThrows(InvalidArgumentException.class,
            () -> WaveletValidationUtils.validateDiscreteWavelet(null),
            "Should throw InvalidArgumentException for null wavelet");
    }

    @Test
    @DisplayName("Should throw InvalidConfigurationException for continuous wavelet")
    void testValidateDiscreteWaveletContinuousThrows() {
        Wavelet continuousWavelet = new MorletWavelet();
        
        InvalidConfigurationException exception = assertThrows(InvalidConfigurationException.class,
            () -> WaveletValidationUtils.validateDiscreteWavelet(continuousWavelet));
        
        assertTrue(exception.getMessage().contains("MorletWavelet"),
            "Exception message should contain wavelet class name");
        assertTrue(exception.getMessage().contains("discrete wavelet transform"),
            "Exception message should mention discrete wavelet transform operations");
    }

    @Test
    @DisplayName("Should validate valid decomposition levels")
    void testValidateDecompositionLevel() {
        // Valid levels should not throw
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateDecompositionLevel(1, 5, "test context"));
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateDecompositionLevel(3, 5, "test context"));
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateDecompositionLevel(5, 5, "test context"));
    }

    @Test
    @DisplayName("Should throw InvalidArgumentException for level less than 1")
    void testValidateDecompositionLevelTooLow() {
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
            () -> WaveletValidationUtils.validateDecompositionLevel(0, 5, "test context"));
        
        assertTrue(exception.getMessage().contains("at least 1"),
            "Exception should mention minimum level requirement");
        assertTrue(exception.getMessage().contains("got: 0"),
            "Exception should include actual invalid level");
        assertTrue(exception.getMessage().contains("test context"),
            "Exception should include provided context");
    }

    @Test
    @DisplayName("Should throw InvalidArgumentException for negative level")
    void testValidateDecompositionLevelNegative() {
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
            () -> WaveletValidationUtils.validateDecompositionLevel(-2, 5, "negative test"));
        
        assertTrue(exception.getMessage().contains("at least 1"),
            "Exception should mention minimum level requirement");
        assertTrue(exception.getMessage().contains("got: -2"),
            "Exception should include actual invalid level");
    }

    @Test
    @DisplayName("Should throw InvalidArgumentException for level exceeding maximum")
    void testValidateDecompositionLevelTooHigh() {
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
            () -> WaveletValidationUtils.validateDecompositionLevel(10, 5, "max level test"));
        
        assertTrue(exception.getMessage().contains("exceeds maximum 5"),
            "Exception should mention the maximum level");
        assertTrue(exception.getMessage().contains("level 10"),
            "Exception should include the excessive level");
        assertTrue(exception.getMessage().contains("max level test"),
            "Exception should include provided context");
    }

    @Test
    @DisplayName("Should validate matching coefficient lengths")
    void testValidateCoefficientLengths() {
        // Matching lengths should not throw
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateCoefficientLengths(100, 100, "matching test"));
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateCoefficientLengths(0, 0, "zero length test"));
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateCoefficientLengths(1024, 1024, "large arrays"));
    }

    @Test
    @DisplayName("Should throw InvalidArgumentException for mismatched coefficient lengths")
    void testValidateCoefficientLengthsMismatch() {
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
            () -> WaveletValidationUtils.validateCoefficientLengths(100, 50, "mismatch test"));
        
        assertTrue(exception.getMessage().contains("same length"),
            "Exception should mention length requirement");
        assertTrue(exception.getMessage().contains("Approximation: 100"),
            "Exception should include approximation length");
        assertTrue(exception.getMessage().contains("Detail: 50"),
            "Exception should include detail length");
        assertTrue(exception.getMessage().contains("mismatch test"),
            "Exception should include provided context");
    }

    @Test
    @DisplayName("Should calculate maximum decomposition levels correctly")
    void testCalculateMaxDecompositionLevels() {
        DiscreteWavelet haar = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.HAAR); // Filter length: 2
        
        // Test various signal lengths with Haar wavelet
        assertEquals(1, WaveletValidationUtils.calculateMaxDecompositionLevels(2, haar, 10),
            "Signal length 2 should allow 1 level");
        assertEquals(2, WaveletValidationUtils.calculateMaxDecompositionLevels(4, haar, 10),
            "Signal length 4 should allow 2 levels");
        assertEquals(3, WaveletValidationUtils.calculateMaxDecompositionLevels(8, haar, 10),
            "Signal length 8 should allow 3 levels");
        assertEquals(7, WaveletValidationUtils.calculateMaxDecompositionLevels(128, haar, 10),
            "Signal length 128 should allow 7 levels");
        assertEquals(10, WaveletValidationUtils.calculateMaxDecompositionLevels(1024, haar, 10),
            "Signal length 1024 should allow 10 levels");
    }

    @Test
    @DisplayName("Should respect maximum allowed levels constraint")
    void testCalculateMaxDecompositionLevelsConstraint() {
        DiscreteWavelet haar = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.HAAR);
        
        // Test with restrictive maximum
        assertEquals(3, WaveletValidationUtils.calculateMaxDecompositionLevels(1024, haar, 3),
            "Should be limited by maxAllowed parameter");
        assertEquals(1, WaveletValidationUtils.calculateMaxDecompositionLevels(1024, haar, 1),
            "Should be limited to 1 level when maxAllowed is 1");
    }

    @Test
    @DisplayName("Should handle wavelets with longer filter lengths")
    void testCalculateMaxDecompositionLevelsLongerFilter() {
        DiscreteWavelet db4 = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.DB4); // Filter length: 8
        
        // With longer filter, fewer levels are possible
        assertEquals(1, WaveletValidationUtils.calculateMaxDecompositionLevels(8, db4, 10),
            "Signal length 8 with DB4 should allow 1 level");
        assertEquals(2, WaveletValidationUtils.calculateMaxDecompositionLevels(16, db4, 10),
            "Signal length 16 with DB4 should allow 2 levels");
        assertEquals(8, WaveletValidationUtils.calculateMaxDecompositionLevels(1024, db4, 10),
            "Signal length 1024 with DB4 should allow 8 levels");
    }

    @Test
    @DisplayName("Should return at least 1 level for very small signals")
    void testCalculateMaxDecompositionLevelsMinimum() {
        DiscreteWavelet haar = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.HAAR);
        
        // Even with very small signals, should return at least 1
        assertEquals(1, WaveletValidationUtils.calculateMaxDecompositionLevels(1, haar, 10),
            "Should return at least 1 level for any signal");
    }

    @Test
    @DisplayName("Should throw InvalidConfigurationException for continuous wavelet in max levels calculation")
    void testCalculateMaxDecompositionLevelsInvalidWavelet() {
        Wavelet continuousWavelet = new MorletWavelet();
        
        assertThrows(InvalidConfigurationException.class,
            () -> WaveletValidationUtils.calculateMaxDecompositionLevels(100, continuousWavelet, 5),
            "Should throw exception for continuous wavelet");
    }

    @Test
    @DisplayName("Should throw InvalidArgumentException for null wavelet in max levels calculation")
    void testCalculateMaxDecompositionLevelsNullWavelet() {
        assertThrows(InvalidArgumentException.class,
            () -> WaveletValidationUtils.calculateMaxDecompositionLevels(100, null, 5),
            "Should throw exception for null wavelet");
    }

    @Test
    @DisplayName("Should handle edge cases in decomposition level validation")
    void testDecompositionLevelEdgeCases() {
        // Test boundary conditions
        assertDoesNotThrow(() -> 
            WaveletValidationUtils.validateDecompositionLevel(1, 1, "boundary test"));
        
        InvalidArgumentException exception = assertThrows(InvalidArgumentException.class,
            () -> WaveletValidationUtils.validateDecompositionLevel(2, 1, "boundary test"));
        
        assertTrue(exception.getMessage().contains("exceeds maximum 1"),
            "Should handle case where level equals max + 1");
    }
}