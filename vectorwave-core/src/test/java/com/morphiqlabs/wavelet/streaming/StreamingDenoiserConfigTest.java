package com.morphiqlabs.wavelet.streaming;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for StreamingDenoiserConfig and its Builder.
 */
public class StreamingDenoiserConfigTest {
    
    @Test
    @DisplayName("Test StreamingDenoiserConfig with default configuration")
    void testDefaultConfiguration() {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder().build();
        
        assertNotNull(config);
        assertEquals(Daubechies.DB4, config.getWavelet());
        assertEquals(256, config.getBlockSize());
        assertEquals(0.5, config.getOverlapFactor());
        assertEquals(BoundaryMode.PERIODIC, config.getBoundaryMode());
        assertEquals(WaveletDenoiser.ThresholdMethod.UNIVERSAL, config.getThresholdMethod());
        assertEquals(WaveletDenoiser.ThresholdType.SOFT, config.getThresholdType());
        assertFalse(config.isAdaptiveThreshold());
        assertEquals(1.0, config.getThresholdMultiplier());
        assertEquals(128, config.getNoiseWindowSize());
    }
    
    @Test
    @DisplayName("Test StreamingDenoiserConfig with custom configuration")
    void testCustomConfiguration() {
        Wavelet wavelet = Daubechies.DB8;
        
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(wavelet)
            .blockSize(512)
            .overlapFactor(0.25)
            .boundaryMode(BoundaryMode.ZERO_PADDING)
            .thresholdMethod(WaveletDenoiser.ThresholdMethod.SURE)
            .thresholdType(WaveletDenoiser.ThresholdType.HARD)
            .adaptiveThreshold(true)
            .thresholdMultiplier(0.8)
            .noiseWindowSize(256)
            .build();
        
        assertNotNull(config);
        assertEquals(wavelet, config.getWavelet());
        assertEquals(512, config.getBlockSize());
        assertEquals(0.25, config.getOverlapFactor());
        assertEquals(BoundaryMode.ZERO_PADDING, config.getBoundaryMode());
        assertEquals(WaveletDenoiser.ThresholdMethod.SURE, config.getThresholdMethod());
        assertEquals(WaveletDenoiser.ThresholdType.HARD, config.getThresholdType());
        assertTrue(config.isAdaptiveThreshold());
        assertEquals(0.8, config.getThresholdMultiplier());
        assertEquals(256, config.getNoiseWindowSize());
    }
    
    @Test
    @DisplayName("Test defaultAudioConfig")
    void testDefaultAudioConfig() {
        StreamingDenoiserConfig config = StreamingDenoiserConfig.defaultAudioConfig();
        
        assertNotNull(config);
        assertEquals(512, config.getBlockSize());
        assertEquals(0.5, config.getOverlapFactor());
        assertEquals(WaveletDenoiser.ThresholdMethod.UNIVERSAL, config.getThresholdMethod());
        assertTrue(config.isAdaptiveThreshold());
    }
    
    @Test
    @DisplayName("Test defaultFinancialConfig")
    void testDefaultFinancialConfig() {
        StreamingDenoiserConfig config = StreamingDenoiserConfig.defaultFinancialConfig();
        
        assertNotNull(config);
        assertEquals(256, config.getBlockSize());
        assertEquals(0.25, config.getOverlapFactor());
        assertEquals(WaveletDenoiser.ThresholdMethod.SURE, config.getThresholdMethod());
        assertEquals(0.8, config.getThresholdMultiplier());
    }
    
    @Test
    @DisplayName("Test Builder with null wavelet")
    void testBuilderWithNullWavelet() {
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .wavelet(null)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder with invalid block size")
    void testBuilderWithInvalidBlockSize() {
        // Zero block size
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .blockSize(0)
                .build();
        });
        
        // Negative block size
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .blockSize(-1)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder with invalid overlap factor")
    void testBuilderWithInvalidOverlapFactor() {
        // Negative overlap
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .overlapFactor(-0.1)
                .build();
        });
        
        // Overlap >= 1
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .overlapFactor(1.0)
                .build();
        });
        
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .overlapFactor(1.5)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder with null boundary mode")
    void testBuilderWithNullBoundaryMode() {
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .boundaryMode(null)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder with null threshold method")
    void testBuilderWithNullThresholdMethod() {
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .thresholdMethod(null)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder with null threshold type")
    void testBuilderWithNullThresholdType() {
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .thresholdType(null)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder with invalid threshold multiplier")
    void testBuilderWithInvalidThresholdMultiplier() {
        // Zero multiplier
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .thresholdMultiplier(0.0)
                .build();
        });
        
        // Negative multiplier
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .thresholdMultiplier(-0.5)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder with invalid noise window size")
    void testBuilderWithInvalidNoiseWindowSize() {
        // Zero window size
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .noiseWindowSize(0)
                .build();
        });
        
        // Negative window size
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .noiseWindowSize(-1)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder with small block size and overlap")
    void testBuilderWithSmallBlockSizeAndOverlap() {
        // Block size too small for overlap
        assertThrows(InvalidArgumentException.class, () -> {
            new StreamingDenoiserConfig.Builder()
                .blockSize(32)  // Less than 64
                .overlapFactor(0.5)
                .build();
        });
    }
    
    @Test
    @DisplayName("Test Builder method chaining")
    void testBuilderMethodChaining() {
        StreamingDenoiserConfig.Builder builder = new StreamingDenoiserConfig.Builder();
        
        // Test that all builder methods return the builder
        assertSame(builder, builder.wavelet(Daubechies.DB6));
        assertSame(builder, builder.blockSize(512));
        assertSame(builder, builder.overlapFactor(0.3));
        assertSame(builder, builder.boundaryMode(BoundaryMode.PERIODIC));
        assertSame(builder, builder.thresholdMethod(WaveletDenoiser.ThresholdMethod.BAYES));
        assertSame(builder, builder.thresholdType(WaveletDenoiser.ThresholdType.SOFT));
        assertSame(builder, builder.adaptiveThreshold(true));
        assertSame(builder, builder.thresholdMultiplier(1.2));
        assertSame(builder, builder.noiseWindowSize(256));
        
        // Build should work
        StreamingDenoiserConfig config = builder.build();
        assertNotNull(config);
    }
    
    @Test
    @DisplayName("Test toString method")
    void testToString() {
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(Daubechies.DB4)
            .blockSize(256)
            .overlapFactor(0.5)
            .thresholdMethod(WaveletDenoiser.ThresholdMethod.UNIVERSAL)
            .thresholdType(WaveletDenoiser.ThresholdType.SOFT)
            .adaptiveThreshold(true)
            .thresholdMultiplier(1.0)
            .build();
        
        String str = config.toString();
        assertNotNull(str);
        assertTrue(str.contains("StreamingDenoiserConfig"));
        assertTrue(str.contains("db4"));  // Wavelet name is lowercase
        assertTrue(str.contains("256"));
        assertTrue(str.contains("50.0%"));
        assertTrue(str.contains("UNIVERSAL"));
        assertTrue(str.contains("SOFT"));
        assertTrue(str.contains("true"));
        assertTrue(str.contains("1.00"));
    }
    
    @Test
    @DisplayName("Test valid overlap factors")
    void testValidOverlapFactors() {
        // Test 0 overlap (no overlap)
        StreamingDenoiserConfig config1 = new StreamingDenoiserConfig.Builder()
            .overlapFactor(0.0)
            .build();
        assertEquals(0.0, config1.getOverlapFactor());
        
        // Test typical overlaps
        StreamingDenoiserConfig config2 = new StreamingDenoiserConfig.Builder()
            .overlapFactor(0.25)
            .build();
        assertEquals(0.25, config2.getOverlapFactor());
        
        StreamingDenoiserConfig config3 = new StreamingDenoiserConfig.Builder()
            .overlapFactor(0.5)
            .build();
        assertEquals(0.5, config3.getOverlapFactor());
        
        StreamingDenoiserConfig config4 = new StreamingDenoiserConfig.Builder()
            .overlapFactor(0.75)
            .build();
        assertEquals(0.75, config4.getOverlapFactor());
        
        // Test close to 1 but less than 1
        StreamingDenoiserConfig config5 = new StreamingDenoiserConfig.Builder()
            .overlapFactor(0.99)
            .build();
        assertEquals(0.99, config5.getOverlapFactor());
    }
    
    @Test
    @DisplayName("Test large noise window size")
    void testLargeNoiseWindowSize() {
        // Test that large window sizes are accepted (no upper bound validation)
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .blockSize(256)
            .noiseWindowSize(10000)  // Much larger than block size
            .build();
        
        assertEquals(10000, config.getNoiseWindowSize());
        assertEquals(256, config.getBlockSize());
    }
}