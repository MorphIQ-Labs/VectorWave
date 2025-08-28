package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class CWTConfigTest {
    
    @Test
    @DisplayName("Should create default config")
    void testDefaultConfig() {
        // When
        CWTConfig config = CWTConfig.defaultConfig();
        
        // Then
        assertNotNull(config);
        assertEquals(BoundaryMode.PERIODIC, config.getBoundaryMode());
        assertTrue(config.isFFTEnabled());
        assertTrue(config.isNormalizeAcrossScales());
        assertEquals(CWTConfig.PaddingStrategy.REFLECT, config.getPaddingStrategy());
        assertEquals(0, config.getFFTSize()); // Auto-determined
        assertFalse(config.isUseScopedValues());
        assertTrue(config.isUseStructuredConcurrency());
        assertTrue(config.isUseStreamGatherers());
    }
    
    @Test
    @DisplayName("Should create config with builder")
    void testConfigBuilder() {
        // When
        CWTConfig config = CWTConfig.builder()
            .boundaryMode(BoundaryMode.ZERO_PADDING)
            .enableFFT(false)
            .normalizeScales(false)
            .paddingStrategy(CWTConfig.PaddingStrategy.ZERO)
            .fftSize(1024)
            .useScopedValues(true)
            .useStructuredConcurrency(false)
            .useStreamGatherers(false)
            .build();
        
        // Then
        assertEquals(BoundaryMode.ZERO_PADDING, config.getBoundaryMode());
        assertFalse(config.isFFTEnabled());
        assertFalse(config.isNormalizeAcrossScales());
        assertEquals(CWTConfig.PaddingStrategy.ZERO, config.getPaddingStrategy());
        assertEquals(1024, config.getFFTSize());
        assertTrue(config.isUseScopedValues());
        assertFalse(config.isUseStructuredConcurrency());
        assertFalse(config.isUseStreamGatherers());
    }
    
    @Test
    @DisplayName("Should create optimized config for Java 21")
    void testOptimizedForJava21() {
        // When
        CWTConfig config = CWTConfig.optimizedForJava21();
        
        // Then
        assertNotNull(config);
        assertTrue(config.isFFTEnabled());
        assertTrue(config.isNormalizeAcrossScales());
        assertTrue(config.isUseScopedValues());
        assertTrue(config.isUseStructuredConcurrency());
        assertTrue(config.isUseStreamGatherers());
    }
    
    @Test
    @DisplayName("Should create config for real-time processing")
    void testRealTimeConfig() {
        // When
        CWTConfig config = CWTConfig.forRealTimeProcessing();
        
        // Then
        assertNotNull(config);
        assertFalse(config.isFFTEnabled()); // Direct convolution for low latency
        assertTrue(config.isNormalizeAcrossScales());
        assertTrue(config.isUseStreamGatherers()); // For efficient streaming
        assertFalse(config.isUseScopedValues()); // Avoid overhead
    }
    
    @Test
    @DisplayName("Should create config for batch processing")
    void testBatchProcessingConfig() {
        // When
        CWTConfig config = CWTConfig.forBatchProcessing();
        
        // Then
        assertNotNull(config);
        assertTrue(config.isFFTEnabled()); // FFT for large batches
        assertTrue(config.isNormalizeAcrossScales());
        assertTrue(config.isUseStructuredConcurrency()); // Parallel processing
        assertTrue(config.isUseScopedValues()); // Shared context
    }
    
    @Test
    @DisplayName("Should validate FFT size")
    void testFFTSizeValidation() {
        // Valid power of 2
        CWTConfig config1 = CWTConfig.builder().fftSize(1024).build();
        assertEquals(1024, config1.getFFTSize());
        
        // Invalid FFT size should throw exception
        assertThrows(IllegalArgumentException.class, 
            () -> CWTConfig.builder().fftSize(1023).build());
        
        assertThrows(IllegalArgumentException.class, 
            () -> CWTConfig.builder().fftSize(-1024).build());
        
        // 0 is valid (auto-determine)
        CWTConfig config2 = CWTConfig.builder().fftSize(0).build();
        assertEquals(0, config2.getFFTSize());
    }
    
    @Test
    @DisplayName("Should determine if FFT is beneficial")
    void testShouldUseFFT() {
        // FFT enabled with auto size
        CWTConfig config1 = CWTConfig.builder()
            .enableFFT(true)
            .fftSize(0)
            .build();
        assertTrue(config1.shouldUseFFT(2048)); // Large signal
        assertTrue(config1.shouldUseFFT(64)); // At threshold (64)
        assertFalse(config1.shouldUseFFT(32)); // Below threshold
        
        // FFT disabled
        CWTConfig config2 = CWTConfig.builder()
            .enableFFT(false)
            .build();
        assertFalse(config2.shouldUseFFT(2048));
        assertFalse(config2.shouldUseFFT(64));
        
        // FFT with fixed size
        CWTConfig config3 = CWTConfig.builder()
            .enableFFT(true)
            .fftSize(512)
            .build();
        assertTrue(config3.shouldUseFFT(512));
        assertTrue(config3.shouldUseFFT(1024));
    }
    
    @Test
    @DisplayName("Should calculate optimal FFT size")
    void testOptimalFFTSize() {
        CWTConfig config = CWTConfig.defaultConfig();
        
        // Test various signal sizes
        assertEquals(128, config.getOptimalFFTSize(100));    // Next power of 2
        assertEquals(256, config.getOptimalFFTSize(200));
        assertEquals(512, config.getOptimalFFTSize(500));
        assertEquals(1024, config.getOptimalFFTSize(1000));
        assertEquals(2048, config.getOptimalFFTSize(1500));
        
        // Already power of 2
        assertEquals(1024, config.getOptimalFFTSize(1024));
        assertEquals(2048, config.getOptimalFFTSize(2048));
    }
    
    @Test
    @DisplayName("Should create immutable config")
    void testConfigImmutability() {
        // Given
        CWTConfig.Builder builder = CWTConfig.builder()
            .boundaryMode(BoundaryMode.PERIODIC);
        
        // When
        CWTConfig config1 = builder.build();
        builder.boundaryMode(BoundaryMode.ZERO_PADDING);
        CWTConfig config2 = builder.build();
        
        // Then - configs should be independent
        assertEquals(BoundaryMode.PERIODIC, config1.getBoundaryMode());
        assertEquals(BoundaryMode.ZERO_PADDING, config2.getBoundaryMode());
    }
    
    @Test
    @DisplayName("Should copy config")
    void testConfigCopy() {
        // Given
        CWTConfig original = CWTConfig.builder()
            .boundaryMode(BoundaryMode.SYMMETRIC)
            .enableFFT(false)
            .fftSize(2048)
            .normalizeScales(false)
            .build();
        
        // When
        CWTConfig copy = original.toBuilder()
            .enableFFT(true)
            .build();
        
        // Then
        assertEquals(BoundaryMode.SYMMETRIC, copy.getBoundaryMode());
        assertTrue(copy.isFFTEnabled()); // Changed
        assertEquals(2048, copy.getFFTSize());
        assertFalse(copy.isNormalizeAcrossScales());
        
        // Original unchanged
        assertFalse(original.isFFTEnabled());
    }
}