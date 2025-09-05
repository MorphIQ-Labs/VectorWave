package com.morphiqlabs.wavelet.cwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for SignalAnalysisConfig and its Builder.
 */
class SignalAnalysisConfigTest {
    
    @Test
    @DisplayName("Test SignalAnalysisConfig with valid configuration")
    void testValidConfiguration() {
        SignalAnalysisConfig config = SignalAnalysisConfig.builder()
            .energyThreshold(0.01)
            .spectralAnalysisSize(1024)
            .scaleDensityFactor(1.5)
            .build();
        
        assertNotNull(config);
        assertEquals(0.01, config.getEnergyThreshold());
        assertEquals(1024, config.getSpectralAnalysisSize());
        assertEquals(1.5, config.getScaleDensityFactor());
    }
    
    @Test
    @DisplayName("Test Builder with different energy thresholds")
    void testDifferentEnergyThresholds() {
        // High sensitivity
        SignalAnalysisConfig config1 = SignalAnalysisConfig.builder()
            .energyThreshold(0.001)
            .spectralAnalysisSize(512)
            .scaleDensityFactor(1.0)
            .build();
        assertEquals(0.001, config1.getEnergyThreshold());
        
        // Standard
        SignalAnalysisConfig config2 = SignalAnalysisConfig.builder()
            .energyThreshold(0.01)
            .spectralAnalysisSize(512)
            .scaleDensityFactor(1.0)
            .build();
        assertEquals(0.01, config2.getEnergyThreshold());
        
        // Noise suppression
        SignalAnalysisConfig config3 = SignalAnalysisConfig.builder()
            .energyThreshold(0.05)
            .spectralAnalysisSize(512)
            .scaleDensityFactor(1.0)
            .build();
        assertEquals(0.05, config3.getEnergyThreshold());
    }
    
    @Test
    @DisplayName("Test Builder with different spectral analysis sizes")
    void testDifferentSpectralAnalysisSizes() {
        // Minimum valid size
        SignalAnalysisConfig config1 = SignalAnalysisConfig.builder()
            .energyThreshold(0.01)
            .spectralAnalysisSize(64)
            .scaleDensityFactor(1.0)
            .build();
        assertEquals(64, config1.getSpectralAnalysisSize());
        
        // Common sizes
        SignalAnalysisConfig config2 = SignalAnalysisConfig.builder()
            .energyThreshold(0.01)
            .spectralAnalysisSize(512)
            .scaleDensityFactor(1.0)
            .build();
        assertEquals(512, config2.getSpectralAnalysisSize());
        
        SignalAnalysisConfig config3 = SignalAnalysisConfig.builder()
            .energyThreshold(0.01)
            .spectralAnalysisSize(1024)
            .scaleDensityFactor(1.0)
            .build();
        assertEquals(1024, config3.getSpectralAnalysisSize());
        
        SignalAnalysisConfig config4 = SignalAnalysisConfig.builder()
            .energyThreshold(0.01)
            .spectralAnalysisSize(2048)
            .scaleDensityFactor(1.0)
            .build();
        assertEquals(2048, config4.getSpectralAnalysisSize());
    }
    
    @Test
    @DisplayName("Test Builder with different scale density factors")
    void testDifferentScaleDensityFactors() {
        // Sparse
        SignalAnalysisConfig config1 = SignalAnalysisConfig.builder()
            .energyThreshold(0.01)
            .spectralAnalysisSize(512)
            .scaleDensityFactor(1.0)
            .build();
        assertEquals(1.0, config1.getScaleDensityFactor());
        
        // Standard
        SignalAnalysisConfig config2 = SignalAnalysisConfig.builder()
            .energyThreshold(0.01)
            .spectralAnalysisSize(512)
            .scaleDensityFactor(1.5)
            .build();
        assertEquals(1.5, config2.getScaleDensityFactor());
        
        // Dense
        SignalAnalysisConfig config3 = SignalAnalysisConfig.builder()
            .energyThreshold(0.01)
            .spectralAnalysisSize(512)
            .scaleDensityFactor(2.0)
            .build();
        assertEquals(2.0, config3.getScaleDensityFactor());
    }
    
    @Test
    @DisplayName("Test Builder with invalid energy threshold")
    void testInvalidEnergyThreshold() {
        // Zero threshold
        assertThrows(IllegalArgumentException.class, () -> {
            SignalAnalysisConfig.builder()
                .energyThreshold(0.0)
                .spectralAnalysisSize(512)
                .scaleDensityFactor(1.0)
                .build();
        });
        
        // Negative threshold
        assertThrows(IllegalArgumentException.class, () -> {
            SignalAnalysisConfig.builder()
                .energyThreshold(-0.1)
                .spectralAnalysisSize(512)
                .scaleDensityFactor(1.0)
                .build();
        });
        
        // Threshold equal to 1
        assertThrows(IllegalArgumentException.class, () -> {
            SignalAnalysisConfig.builder()
                .energyThreshold(1.0)
                .spectralAnalysisSize(512)
                .scaleDensityFactor(1.0)
                .build();
        });
        
        // Threshold greater than 1
        assertThrows(IllegalArgumentException.class, () -> {
            SignalAnalysisConfig.builder()
                .energyThreshold(1.5)
                .spectralAnalysisSize(512)
                .scaleDensityFactor(1.0)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder with invalid spectral analysis size")
    void testInvalidSpectralAnalysisSize() {
        // Too small
        assertThrows(IllegalArgumentException.class, () -> {
            SignalAnalysisConfig.builder()
                .energyThreshold(0.01)
                .spectralAnalysisSize(32)
                .scaleDensityFactor(1.0)
                .build();
        });
        
        // Not power of 2
        assertThrows(IllegalArgumentException.class, () -> {
            SignalAnalysisConfig.builder()
                .energyThreshold(0.01)
                .spectralAnalysisSize(100)
                .scaleDensityFactor(1.0)
                .build();
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            SignalAnalysisConfig.builder()
                .energyThreshold(0.01)
                .spectralAnalysisSize(1000)
                .scaleDensityFactor(1.0)
                .build();
        });
        
        // Zero
        assertThrows(IllegalArgumentException.class, () -> {
            SignalAnalysisConfig.builder()
                .energyThreshold(0.01)
                .spectralAnalysisSize(0)
                .scaleDensityFactor(1.0)
                .build();
        });
        
        // Negative
        assertThrows(IllegalArgumentException.class, () -> {
            SignalAnalysisConfig.builder()
                .energyThreshold(0.01)
                .spectralAnalysisSize(-512)
                .scaleDensityFactor(1.0)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder with invalid scale density factor")
    void testInvalidScaleDensityFactor() {
        // Zero factor
        assertThrows(IllegalArgumentException.class, () -> {
            SignalAnalysisConfig.builder()
                .energyThreshold(0.01)
                .spectralAnalysisSize(512)
                .scaleDensityFactor(0.0)
                .build();
        });
        
        // Negative factor
        assertThrows(IllegalArgumentException.class, () -> {
            SignalAnalysisConfig.builder()
                .energyThreshold(0.01)
                .spectralAnalysisSize(512)
                .scaleDensityFactor(-1.0)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder with missing energy threshold")
    void testMissingEnergyThreshold() {
        assertThrows(IllegalStateException.class, () -> {
            SignalAnalysisConfig.builder()
                .spectralAnalysisSize(512)
                .scaleDensityFactor(1.0)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder with missing spectral analysis size")
    void testMissingSpectralAnalysisSize() {
        assertThrows(IllegalStateException.class, () -> {
            SignalAnalysisConfig.builder()
                .energyThreshold(0.01)
                .scaleDensityFactor(1.0)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder with missing scale density factor")
    void testMissingScaleDensityFactor() {
        assertThrows(IllegalStateException.class, () -> {
            SignalAnalysisConfig.builder()
                .energyThreshold(0.01)
                .spectralAnalysisSize(512)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder method chaining")
    void testBuilderMethodChaining() {
        SignalAnalysisConfig.Builder builder = SignalAnalysisConfig.builder();
        
        // Test that all builder methods return the builder
        assertSame(builder, builder.energyThreshold(0.01));
        assertSame(builder, builder.spectralAnalysisSize(1024));
        assertSame(builder, builder.scaleDensityFactor(1.5));
        
        // Build should work
        SignalAnalysisConfig config = builder.build();
        assertNotNull(config);
        assertEquals(0.01, config.getEnergyThreshold());
        assertEquals(1024, config.getSpectralAnalysisSize());
        assertEquals(1.5, config.getScaleDensityFactor());
    }
    
    @Test
    @DisplayName("Test valid boundary values")
    void testValidBoundaryValues() {
        // Very small but valid energy threshold
        SignalAnalysisConfig config1 = SignalAnalysisConfig.builder()
            .energyThreshold(0.00001)
            .spectralAnalysisSize(512)
            .scaleDensityFactor(1.0)
            .build();
        assertEquals(0.00001, config1.getEnergyThreshold());
        
        // Large but valid energy threshold (just under 1)
        SignalAnalysisConfig config2 = SignalAnalysisConfig.builder()
            .energyThreshold(0.999)
            .spectralAnalysisSize(512)
            .scaleDensityFactor(1.0)
            .build();
        assertEquals(0.999, config2.getEnergyThreshold());
        
        // Very small scale density factor
        SignalAnalysisConfig config3 = SignalAnalysisConfig.builder()
            .energyThreshold(0.01)
            .spectralAnalysisSize(512)
            .scaleDensityFactor(0.001)
            .build();
        assertEquals(0.001, config3.getScaleDensityFactor());
        
        // Large scale density factor
        SignalAnalysisConfig config4 = SignalAnalysisConfig.builder()
            .energyThreshold(0.01)
            .spectralAnalysisSize(512)
            .scaleDensityFactor(100.0)
            .build();
        assertEquals(100.0, config4.getScaleDensityFactor());
    }
    
    @Test
    @DisplayName("Test all valid powers of 2 for spectral analysis size")
    void testAllValidPowersOfTwo() {
        int[] validSizes = {64, 128, 256, 512, 1024, 2048, 4096, 8192};
        
        for (int size : validSizes) {
            SignalAnalysisConfig config = SignalAnalysisConfig.builder()
                .energyThreshold(0.01)
                .spectralAnalysisSize(size)
                .scaleDensityFactor(1.0)
                .build();
            assertEquals(size, config.getSpectralAnalysisSize(), 
                "Should accept power of 2: " + size);
        }
    }
}
