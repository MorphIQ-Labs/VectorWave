package com.morphiqlabs.wavelet.streaming;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StreamingDenoiserStrategy interface and PerformanceProfile record.
 */
class StreamingDenoiserStrategyTest {

    @Test
    @DisplayName("PerformanceProfile.fastProfile should create valid profile")
    void testFastProfileCreation() {
        int blockSize = 1024;
        StreamingDenoiserStrategy.PerformanceProfile profile = 
            StreamingDenoiserStrategy.PerformanceProfile.fastProfile(blockSize);
        
        assertNotNull(profile);
        assertEquals(0.1 * blockSize, profile.expectedLatencyMicros());
        assertEquals(6.0, profile.expectedSNRImprovement());
        assertEquals((long) blockSize * 8 * 4, profile.memoryUsageBytes());
        assertTrue(profile.isRealTimeCapable());
    }

    @Test
    @DisplayName("PerformanceProfile.qualityProfile should create valid profile")
    void testQualityProfileCreation() {
        int blockSize = 512;
        double overlap = 0.5;
        StreamingDenoiserStrategy.PerformanceProfile profile = 
            StreamingDenoiserStrategy.PerformanceProfile.qualityProfile(blockSize, overlap);
        
        assertNotNull(profile);
        
        double processingFactor = 1.0 + overlap;
        assertEquals(0.3 * blockSize * processingFactor, profile.expectedLatencyMicros());
        assertEquals(9.0, profile.expectedSNRImprovement());
        assertEquals((long) (blockSize * 8 * 6 * processingFactor), profile.memoryUsageBytes());
        assertTrue(profile.isRealTimeCapable()); // blockSize <= 512
    }

    @Test
    @DisplayName("PerformanceProfile.qualityProfile should handle large blocks")
    void testQualityProfileLargeBlocks() {
        int blockSize = 2048; // > 512
        double overlap = 0.25;
        StreamingDenoiserStrategy.PerformanceProfile profile = 
            StreamingDenoiserStrategy.PerformanceProfile.qualityProfile(blockSize, overlap);
        
        assertNotNull(profile);
        assertFalse(profile.isRealTimeCapable()); // blockSize > 512
        assertEquals(9.0, profile.expectedSNRImprovement());
    }

    @Test
    @DisplayName("PerformanceProfile should handle zero overlap")
    void testQualityProfileZeroOverlap() {
        int blockSize = 256;
        double overlap = 0.0;
        StreamingDenoiserStrategy.PerformanceProfile profile = 
            StreamingDenoiserStrategy.PerformanceProfile.qualityProfile(blockSize, overlap);
        
        double processingFactor = 1.0 + overlap; // = 1.0
        assertEquals(0.3 * blockSize, profile.expectedLatencyMicros());
        assertEquals((long) (blockSize * 8 * 6), profile.memoryUsageBytes());
        assertTrue(profile.isRealTimeCapable());
    }

    @Test
    @DisplayName("PerformanceProfile should handle high overlap")
    void testQualityProfileHighOverlap() {
        int blockSize = 128;
        double overlap = 0.75;
        StreamingDenoiserStrategy.PerformanceProfile profile = 
            StreamingDenoiserStrategy.PerformanceProfile.qualityProfile(blockSize, overlap);
        
        double processingFactor = 1.0 + overlap;
        assertEquals(0.3 * blockSize * processingFactor, profile.expectedLatencyMicros());
        assertEquals((long) (blockSize * 8 * 6 * processingFactor), profile.memoryUsageBytes());
        assertTrue(profile.isRealTimeCapable());
    }

    @Test
    @DisplayName("FastProfile should be real-time capable for various block sizes")
    void testFastProfileRealTimeCapability() {
        // Test various block sizes
        int[] blockSizes = {128, 512, 1024, 2048, 4096};
        
        for (int blockSize : blockSizes) {
            StreamingDenoiserStrategy.PerformanceProfile profile = 
                StreamingDenoiserStrategy.PerformanceProfile.fastProfile(blockSize);
            
            assertTrue(profile.isRealTimeCapable(), 
                "Fast profile should always be real-time capable for block size: " + blockSize);
            assertEquals(6.0, profile.expectedSNRImprovement(),
                "Fast profile should have consistent SNR improvement");
        }
    }

    @Test
    @DisplayName("Profile memory usage should scale with block size")
    void testProfileMemoryScaling() {
        int smallBlock = 256;
        int largeBlock = 1024;
        
        StreamingDenoiserStrategy.PerformanceProfile smallFast = 
            StreamingDenoiserStrategy.PerformanceProfile.fastProfile(smallBlock);
        StreamingDenoiserStrategy.PerformanceProfile largeFast = 
            StreamingDenoiserStrategy.PerformanceProfile.fastProfile(largeBlock);
        
        assertTrue(largeFast.memoryUsageBytes() > smallFast.memoryUsageBytes(),
            "Larger blocks should use more memory");
        
        // Memory should scale proportionally
        double expectedRatio = (double) largeBlock / smallBlock;
        double actualRatio = (double) largeFast.memoryUsageBytes() / smallFast.memoryUsageBytes();
        assertEquals(expectedRatio, actualRatio, 0.001, "Memory usage should scale linearly with block size");
    }

    @Test
    @DisplayName("Profile latency should scale with processing requirements")
    void testProfileLatencyScaling() {
        int blockSize = 512;
        
        StreamingDenoiserStrategy.PerformanceProfile fastProfile = 
            StreamingDenoiserStrategy.PerformanceProfile.fastProfile(blockSize);
        StreamingDenoiserStrategy.PerformanceProfile qualityProfile = 
            StreamingDenoiserStrategy.PerformanceProfile.qualityProfile(blockSize, 0.0);
        
        assertTrue(qualityProfile.expectedLatencyMicros() > fastProfile.expectedLatencyMicros(),
            "Quality profile should have higher latency than fast profile");
        
        assertTrue(qualityProfile.expectedSNRImprovement() > fastProfile.expectedSNRImprovement(),
            "Quality profile should provide better SNR improvement");
    }

    @Test
    @DisplayName("PerformanceProfile record should have proper equals and hashCode")
    void testPerformanceProfileEquality() {
        int blockSize = 512;
        
        StreamingDenoiserStrategy.PerformanceProfile profile1 = 
            StreamingDenoiserStrategy.PerformanceProfile.fastProfile(blockSize);
        StreamingDenoiserStrategy.PerformanceProfile profile2 = 
            StreamingDenoiserStrategy.PerformanceProfile.fastProfile(blockSize);
        StreamingDenoiserStrategy.PerformanceProfile profile3 = 
            StreamingDenoiserStrategy.PerformanceProfile.fastProfile(blockSize * 2);
        
        assertEquals(profile1, profile2, "Profiles with same parameters should be equal");
        assertEquals(profile1.hashCode(), profile2.hashCode(), "Equal profiles should have same hash code");
        assertNotEquals(profile1, profile3, "Profiles with different parameters should not be equal");
    }

    @Test
    @DisplayName("PerformanceProfile record should have meaningful toString")
    void testPerformanceProfileToString() {
        int blockSize = 256;
        StreamingDenoiserStrategy.PerformanceProfile profile = 
            StreamingDenoiserStrategy.PerformanceProfile.fastProfile(blockSize);
        
        String toString = profile.toString();
        assertNotNull(toString);
        assertTrue(toString.contains("PerformanceProfile"), "toString should contain record name");
        assertTrue(toString.contains("expectedLatencyMicros"), "toString should contain field names");
    }
}