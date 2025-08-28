package com.morphiqlabs.wavelet.streaming;

import com.morphiqlabs.wavelet.api.Factory;
import com.morphiqlabs.wavelet.api.FactoryRegistry;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for StreamingDenoiserFactory.
 */
public class StreamingDenoiserFactoryTest {
    
    @Test
    @DisplayName("Test getInstance returns singleton")
    void testGetInstanceSingleton() {
        StreamingDenoiserFactory instance1 = StreamingDenoiserFactory.getInstance();
        StreamingDenoiserFactory instance2 = StreamingDenoiserFactory.getInstance();
        
        assertNotNull(instance1);
        assertNotNull(instance2);
        assertSame(instance1, instance2, "Should return the same instance");
    }
    
    @Test
    @DisplayName("Test factory is registered in FactoryRegistry")
    void testFactoryRegistration() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        
        Optional<Factory<StreamingDenoiserStrategy, StreamingDenoiserConfig>> factory = 
            registry.getFactory("streamingDenoiser", StreamingDenoiserStrategy.class, StreamingDenoiserConfig.class);
        
        assertTrue(factory.isPresent(), "streamingDenoiser should be registered");
        assertSame(StreamingDenoiserFactory.getInstance(), factory.get());
    }
    
    @Test
    @DisplayName("Test create with default configuration")
    void testCreateWithDefaultConfig() {
        StreamingDenoiserFactory factory = StreamingDenoiserFactory.getInstance();
        StreamingDenoiserStrategy denoiser = factory.create();
        
        assertNotNull(denoiser);
        // Default should create with audio config
    }
    
    @Test
    @DisplayName("Test create with custom configuration")
    void testCreateWithCustomConfig() {
        StreamingDenoiserFactory factory = StreamingDenoiserFactory.getInstance();
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(Daubechies.DB8)
            .blockSize(512)
            .overlapFactor(0.25)
            .build();
        
        StreamingDenoiserStrategy denoiser = factory.create(config);
        
        assertNotNull(denoiser);
    }
    
    @Test
    @DisplayName("Test create with null configuration")
    void testCreateWithNullConfig() {
        StreamingDenoiserFactory factory = StreamingDenoiserFactory.getInstance();
        
        assertThrows(InvalidArgumentException.class, () -> {
            factory.create((StreamingDenoiserConfig) null);
        });
    }
    
    @Test
    @DisplayName("Test create with explicit FAST implementation")
    void testCreateWithFastImplementation() {
        StreamingDenoiserConfig config = StreamingDenoiserConfig.defaultAudioConfig();
        
        StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
            StreamingDenoiserFactory.Implementation.FAST, config);
        
        assertNotNull(denoiser);
        assertTrue(denoiser instanceof FastStreamingDenoiser);
    }
    
    @Test
    @DisplayName("Test create with explicit QUALITY implementation")
    void testCreateWithQualityImplementation() {
        StreamingDenoiserConfig config = StreamingDenoiserConfig.defaultFinancialConfig();
        
        StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.create(
            StreamingDenoiserFactory.Implementation.QUALITY, config);
        
        assertNotNull(denoiser);
        assertTrue(denoiser instanceof QualityStreamingDenoiser);
    }
    
    @Test
    @DisplayName("Test create with null implementation")
    void testCreateWithNullImplementation() {
        StreamingDenoiserConfig config = StreamingDenoiserConfig.defaultAudioConfig();
        
        assertThrows(InvalidArgumentException.class, () -> {
            StreamingDenoiserFactory.create(null, config);
        });
    }
    
    @Test
    @DisplayName("Test auto-selection chooses FAST for small blocks")
    void testAutoSelectionForSmallBlocks() {
        StreamingDenoiserFactory factory = StreamingDenoiserFactory.getInstance();
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .blockSize(256)  // Small block
            .overlapFactor(0.1)
            .adaptiveThreshold(false)
            .build();
        
        StreamingDenoiserStrategy denoiser = factory.create(config);
        
        assertNotNull(denoiser);
        assertTrue(denoiser instanceof FastStreamingDenoiser,
            "Should select FAST implementation for small block size");
    }
    
    @Test
    @DisplayName("Test auto-selection chooses FAST for high overlap with adaptive")
    void testAutoSelectionForHighOverlapAdaptive() {
        StreamingDenoiserFactory factory = StreamingDenoiserFactory.getInstance();
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .blockSize(512)  // Large block
            .overlapFactor(0.5)  // High overlap
            .adaptiveThreshold(true)  // Adaptive enabled
            .build();
        
        StreamingDenoiserStrategy denoiser = factory.create(config);
        
        assertNotNull(denoiser);
        assertTrue(denoiser instanceof FastStreamingDenoiser,
            "Should select FAST implementation for high overlap with adaptive threshold");
    }
    
    @Test
    @DisplayName("Test auto-selection chooses QUALITY for large blocks with low overlap")
    void testAutoSelectionForLargeBlocks() {
        StreamingDenoiserFactory factory = StreamingDenoiserFactory.getInstance();
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .blockSize(512)  // Large block
            .overlapFactor(0.25)  // Low overlap
            .adaptiveThreshold(false)
            .build();
        
        StreamingDenoiserStrategy denoiser = factory.create(config);
        
        assertNotNull(denoiser);
        assertTrue(denoiser instanceof QualityStreamingDenoiser,
            "Should select QUALITY implementation for large blocks with low overlap");
    }
    
    @Test
    @DisplayName("Test auto-selection chooses QUALITY for high overlap without adaptive")
    void testAutoSelectionForHighOverlapNonAdaptive() {
        StreamingDenoiserFactory factory = StreamingDenoiserFactory.getInstance();
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .blockSize(512)  // Large block
            .overlapFactor(0.5)  // High overlap
            .adaptiveThreshold(false)  // Adaptive disabled
            .build();
        
        StreamingDenoiserStrategy denoiser = factory.create(config);
        
        assertNotNull(denoiser);
        assertTrue(denoiser instanceof QualityStreamingDenoiser,
            "Should select QUALITY implementation when adaptive is disabled");
    }
    
    @Test
    @DisplayName("Test isValidConfiguration with valid config")
    void testIsValidConfigurationWithValidConfig() {
        StreamingDenoiserFactory factory = StreamingDenoiserFactory.getInstance();
        StreamingDenoiserConfig config = StreamingDenoiserConfig.defaultAudioConfig();
        
        assertTrue(factory.isValidConfiguration(config));
    }
    
    @Test
    @DisplayName("Test isValidConfiguration with null config")
    void testIsValidConfigurationWithNullConfig() {
        StreamingDenoiserFactory factory = StreamingDenoiserFactory.getInstance();
        
        assertFalse(factory.isValidConfiguration(null));
    }
    
    @Test
    @DisplayName("Test getDescription")
    void testGetDescription() {
        StreamingDenoiserFactory factory = StreamingDenoiserFactory.getInstance();
        
        String description = factory.getDescription();
        assertNotNull(description);
        assertTrue(description.contains("streaming wavelet denoisers"));
        assertTrue(description.contains("FAST"));
        assertTrue(description.contains("QUALITY"));
    }
    
    @Test
    @DisplayName("Test boundary case: block size exactly 256")
    void testBoundaryCaseBlockSize256() {
        StreamingDenoiserFactory factory = StreamingDenoiserFactory.getInstance();
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .blockSize(256)  // Exactly at boundary
            .overlapFactor(0.3)
            .adaptiveThreshold(false)
            .build();
        
        StreamingDenoiserStrategy denoiser = factory.create(config);
        
        assertNotNull(denoiser);
        assertTrue(denoiser instanceof FastStreamingDenoiser,
            "Should select FAST for block size = 256");
    }
    
    @Test
    @DisplayName("Test boundary case: overlap exactly 0.5")
    void testBoundaryCaseOverlap05() {
        StreamingDenoiserFactory factory = StreamingDenoiserFactory.getInstance();
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .blockSize(512)
            .overlapFactor(0.5)  // Exactly at boundary
            .adaptiveThreshold(true)
            .build();
        
        StreamingDenoiserStrategy denoiser = factory.create(config);
        
        assertNotNull(denoiser);
        assertTrue(denoiser instanceof FastStreamingDenoiser,
            "Should select FAST for overlap = 0.5 with adaptive");
    }
    
    @Test
    @DisplayName("Test different threshold methods don't affect selection")
    void testThresholdMethodDoesNotAffectSelection() {
        StreamingDenoiserFactory factory = StreamingDenoiserFactory.getInstance();
        
        // Test with UNIVERSAL
        StreamingDenoiserConfig config1 = new StreamingDenoiserConfig.Builder()
            .blockSize(512)
            .thresholdMethod(WaveletDenoiser.ThresholdMethod.UNIVERSAL)
            .build();
        StreamingDenoiserStrategy denoiser1 = factory.create(config1);
        
        // Test with SURE
        StreamingDenoiserConfig config2 = new StreamingDenoiserConfig.Builder()
            .blockSize(512)
            .thresholdMethod(WaveletDenoiser.ThresholdMethod.SURE)
            .build();
        StreamingDenoiserStrategy denoiser2 = factory.create(config2);
        
        // Test with BAYES
        StreamingDenoiserConfig config3 = new StreamingDenoiserConfig.Builder()
            .blockSize(512)
            .thresholdMethod(WaveletDenoiser.ThresholdMethod.BAYES)
            .build();
        StreamingDenoiserStrategy denoiser3 = factory.create(config3);
        
        // All should select the same implementation
        assertEquals(denoiser1.getClass(), denoiser2.getClass());
        assertEquals(denoiser2.getClass(), denoiser3.getClass());
    }
}