package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.api.Factory;
import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class CWTFactoryTest {
    
    @Test
    @DisplayName("Should create transform with default config")
    void testCreateWithDefaultConfig() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        
        // When
        CWTTransform transform = CWTFactory.create(wavelet);
        
        // Then
        assertNotNull(transform);
        assertEquals(wavelet, transform.getWavelet());
        assertNotNull(transform.getConfig());
        assertTrue(transform.getConfig().isFFTEnabled());
        assertTrue(transform.getConfig().isNormalizeAcrossScales());
    }
    
    @Test
    @DisplayName("Should create transform with custom config")
    void testCreateWithCustomConfig() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        CWTConfig config = CWTConfig.builder()
            .enableFFT(false)
            .normalizeScales(false)
            .build();
        
        // When
        CWTTransform transform = CWTFactory.create(wavelet, config);
        
        // Then
        assertNotNull(transform);
        assertEquals(wavelet, transform.getWavelet());
        assertEquals(config, transform.getConfig());
        assertFalse(transform.getConfig().isFFTEnabled());
        assertFalse(transform.getConfig().isNormalizeAcrossScales());
    }
    
    @Test
    @DisplayName("Should create transform for real-time processing")
    void testCreateForRealTime() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        
        // When
        CWTTransform transform = CWTFactory.createForRealTime(wavelet);
        
        // Then
        assertNotNull(transform);
        assertEquals(wavelet, transform.getWavelet());
        assertFalse(transform.getConfig().isFFTEnabled()); // Direct convolution for low latency
        assertTrue(transform.getConfig().isUseStreamGatherers());
    }
    
    @Test
    @DisplayName("Should create transform for batch processing")
    void testCreateForBatchProcessing() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        
        // When
        CWTTransform transform = CWTFactory.createForBatchProcessing(wavelet);
        
        // Then
        assertNotNull(transform);
        assertEquals(wavelet, transform.getWavelet());
        assertTrue(transform.getConfig().isFFTEnabled()); // FFT for efficiency
        assertTrue(transform.getConfig().isUseStructuredConcurrency());
        assertTrue(transform.getConfig().isUseScopedValues());
    }
    
    @Test
    @DisplayName("Should create transform optimized for Java 21")
    void testCreateOptimizedForJava21() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        
        // When
        CWTTransform transform = CWTFactory.createOptimizedForJava21(wavelet);
        
        // Then
        assertNotNull(transform);
        assertEquals(wavelet, transform.getWavelet());
        assertTrue(transform.getConfig().isFFTEnabled());
        assertTrue(transform.getConfig().isUseScopedValues());
        assertTrue(transform.getConfig().isUseStructuredConcurrency());
        assertTrue(transform.getConfig().isUseStreamGatherers());
    }
    
    @Test
    @DisplayName("Should create builder")
    void testCreateBuilder() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        
        // When
        CWTFactory.Builder builder = CWTFactory.builder()
            .wavelet(wavelet)
            .enableFFT(false)
            .normalizeScales(false)
            .fftSize(1024);
        
        CWTTransform transform = builder.build();
        
        // Then
        assertNotNull(transform);
        assertEquals(wavelet, transform.getWavelet());
        assertFalse(transform.getConfig().isFFTEnabled());
        assertFalse(transform.getConfig().isNormalizeAcrossScales());
        assertEquals(1024, transform.getConfig().getFFTSize());
    }
    
    @Test
    @DisplayName("Should validate null wavelet")
    void testValidateNullWavelet() {
        // Then
        assertThrows(IllegalArgumentException.class, 
            () -> CWTFactory.create(null));
        
        assertThrows(IllegalArgumentException.class, 
            () -> CWTFactory.create(null, CWTConfig.defaultConfig()));
        
        assertThrows(IllegalStateException.class,
            () -> CWTFactory.builder().build());
    }
    
    @Test
    @DisplayName("Should create multiple independent transforms")
    void testCreateMultipleIndependentTransforms() {
        // Given
        MorletWavelet wavelet1 = new MorletWavelet(5.0, 1.0);
        MorletWavelet wavelet2 = new MorletWavelet(6.0, 1.5);
        
        // When
        CWTTransform transform1 = CWTFactory.create(wavelet1);
        CWTTransform transform2 = CWTFactory.create(wavelet2);
        
        // Then
        assertNotNull(transform1);
        assertNotNull(transform2);
        assertNotSame(transform1, transform2);
        assertNotSame(transform1.getConfig(), transform2.getConfig());
        assertEquals(wavelet1, transform1.getWavelet());
        assertEquals(wavelet2, transform2.getWavelet());
    }
    
    @Test
    @DisplayName("Should build transform with all options")
    void testBuilderWithAllOptions() {
        // Given
        MorletWavelet wavelet = new MorletWavelet();
        
        // When
        CWTTransform transform = CWTFactory.builder()
            .wavelet(wavelet)
            .boundaryMode(com.morphiqlabs.wavelet.api.BoundaryMode.ZERO_PADDING)
            .enableFFT(true)
            .normalizeScales(true)
            .paddingStrategy(CWTConfig.PaddingStrategy.SYMMETRIC)
            .fftSize(2048)
            .useScopedValues(true)
            .useStructuredConcurrency(false)
            .useStreamGatherers(true)
            .build();
        
        // Then
        CWTConfig config = transform.getConfig();
        assertEquals(com.morphiqlabs.wavelet.api.BoundaryMode.ZERO_PADDING, config.getBoundaryMode());
        assertTrue(config.isFFTEnabled());
        assertTrue(config.isNormalizeAcrossScales());
        assertEquals(CWTConfig.PaddingStrategy.SYMMETRIC, config.getPaddingStrategy());
        assertEquals(2048, config.getFFTSize());
        assertTrue(config.isUseScopedValues());
        assertFalse(config.isUseStructuredConcurrency());
        assertTrue(config.isUseStreamGatherers());
    }
    
    @Test
    @DisplayName("Test CWTFactory.Instance singleton")
    void testInstanceSingleton() {
        CWTFactory.Instance instance1 = CWTFactory.getInstance();
        CWTFactory.Instance instance2 = CWTFactory.getInstance();
        
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertSame(instance1, instance2, "Should return the same instance");
    }
    
    @Test
    @DisplayName("Test Instance.create() without wavelet")
    void testInstanceCreateWithoutWavelet() {
        Factory<CWTTransform, ContinuousWavelet> factory = CWTFactory.getInstance();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            factory.create();
        });
    }
    
    @Test
    @DisplayName("Test Instance.create(wavelet)")
    void testInstanceCreateWithWavelet() {
        Factory<CWTTransform, ContinuousWavelet> factory = CWTFactory.getInstance();
        ContinuousWavelet wavelet = new MorletWavelet();
        
        CWTTransform transform = factory.create(wavelet);
        assertNotNull(transform);
        assertEquals(wavelet, transform.getWavelet());
    }
    
    @Test
    @DisplayName("Test Instance.isValidConfiguration")
    void testInstanceIsValidConfiguration() {
        Factory<CWTTransform, ContinuousWavelet> factory = CWTFactory.getInstance();
        
        assertTrue(factory.isValidConfiguration(new MorletWavelet()));
        assertFalse(factory.isValidConfiguration(null));
    }
    
    @Test
    @DisplayName("Test Instance.getDescription")
    void testInstanceGetDescription() {
        Factory<CWTTransform, ContinuousWavelet> factory = CWTFactory.getInstance();
        
        String description = factory.getDescription();
        assertNotNull(description);
        assertEquals("Factory for creating CWT transform instances", description);
    }
}