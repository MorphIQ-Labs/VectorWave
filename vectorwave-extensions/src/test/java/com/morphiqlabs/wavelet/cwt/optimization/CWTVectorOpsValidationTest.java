package com.morphiqlabs.wavelet.extensions.cwt.optimization;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validation tests for CWTVectorOps to ensure proper error handling.
 */
class CWTVectorOpsValidationTest {
    
    @Test
    @DisplayName("Should throw exception for zero scale")
    void testZeroScaleValidation() {
        CWTVectorOps ops = new CWTVectorOps();
        
        double[] realSignal = new double[128];
        double[] imagSignal = new double[128];
        double[] realWavelet = new double[32];
        double[] imagWavelet = new double[32];
        
        // Initialize with some data
        for (int i = 0; i < realSignal.length; i++) {
            realSignal[i] = Math.sin(2 * Math.PI * i / 16);
        }
        for (int i = 0; i < realWavelet.length; i++) {
            realWavelet[i] = Math.exp(-(i - 16) * (i - 16) / 32.0);
        }
        
        // Test with zero scale
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ops.complexConvolve(realSignal, imagSignal, realWavelet, imagWavelet, 0.0),
            "Should throw exception for zero scale"
        );
        
        assertTrue(exception.getMessage().contains("Scale must be positive"));
        assertTrue(exception.getMessage().contains("0.0"));
    }
    
    @Test
    @DisplayName("Should throw exception for negative scale")
    void testNegativeScaleValidation() {
        CWTVectorOps ops = new CWTVectorOps();
        
        double[] realSignal = new double[128];
        double[] imagSignal = new double[128];
        double[] realWavelet = new double[32];
        double[] imagWavelet = new double[32];
        
        // Test with negative scale
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ops.complexConvolve(realSignal, imagSignal, realWavelet, imagWavelet, -1.5),
            "Should throw exception for negative scale"
        );
        
        assertTrue(exception.getMessage().contains("Scale must be positive"));
        assertTrue(exception.getMessage().contains("-1.5"));
    }
    
    @Test
    @DisplayName("Should accept positive scales")
    void testPositiveScaleAccepted() {
        CWTVectorOps ops = new CWTVectorOps();
        
        double[] realSignal = new double[128];
        double[] imagSignal = new double[128];
        double[] realWavelet = new double[32];
        double[] imagWavelet = new double[32];
        
        // Initialize with valid data
        for (int i = 0; i < realSignal.length; i++) {
            realSignal[i] = Math.sin(2 * Math.PI * i / 16);
        }
        for (int i = 0; i < realWavelet.length; i++) {
            double t = (i - 16) / 4.0;
            realWavelet[i] = Math.exp(-t * t / 2) * Math.cos(5 * t);
            imagWavelet[i] = Math.exp(-t * t / 2) * Math.sin(5 * t);
        }
        
        // Test with various positive scales
        double[] testScales = {0.1, 1.0, 2.5, 10.0, 100.0};
        
        for (double scale : testScales) {
            assertDoesNotThrow(() -> {
                ops.complexConvolve(realSignal, imagSignal, realWavelet, imagWavelet, scale);
            }, "Should accept positive scale: " + scale);
        }
    }
    
    @Test
    @DisplayName("Should validate scale for small signals (scalar path)")
    void testScaleValidationForScalarPath() {
        CWTVectorOps ops = new CWTVectorOps();
        
        // Use small signal to force scalar path
        double[] realSignal = new double[32];
        double[] realWavelet = new double[8];
        double[] imagWavelet = new double[8];
        
        // Test zero scale on scalar path
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> ops.complexConvolve(realSignal, null, realWavelet, imagWavelet, 0.0),
            "Should throw exception for zero scale in scalar path"
        );
        
        assertTrue(exception.getMessage().contains("Scale must be positive"));
    }
    
    @Test
    @DisplayName("Should handle very small positive scales")
    void testVerySmallPositiveScale() {
        CWTVectorOps ops = new CWTVectorOps();
        
        double[] realSignal = new double[128];
        double[] realWavelet = new double[32];
        double[] imagWavelet = new double[32];
        
        // Initialize with valid data
        for (int i = 0; i < realSignal.length; i++) {
            realSignal[i] = 1.0;
        }
        for (int i = 0; i < realWavelet.length; i++) {
            realWavelet[i] = 1.0 / realWavelet.length;
        }
        
        // Test with very small but positive scale
        double verySmallScale = Double.MIN_VALUE;
        
        assertDoesNotThrow(() -> {
            ops.complexConvolve(realSignal, null, realWavelet, imagWavelet, verySmallScale);
        }, "Should accept very small positive scale");
    }
}
