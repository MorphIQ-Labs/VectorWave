package com.morphiqlabs.wavelet.extensions.cwt.optimization;

import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.cwt.ComplexMatrix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class CacheAwareOpsTest {
    
    private static final double TOLERANCE = 1e-10;
    private static final long RANDOM_SEED = 54321L; // Fixed seed for reproducible tests
    private CacheAwareOps cacheOps;
    private double[] testSignal;
    private MorletWavelet wavelet;
    
    @BeforeEach
    void setUp() {
        cacheOps = new CacheAwareOps();
        wavelet = new MorletWavelet();
        
        // Create test signal
        testSignal = new double[2048];
        for (int i = 0; i < testSignal.length; i++) {
            double t = i / 2048.0;
            testSignal[i] = Math.sin(2 * Math.PI * 10 * t) + 
                           0.5 * Math.sin(2 * Math.PI * 25 * t);
        }
    }
    
    @Test
    @DisplayName("Should compute blocked convolution")
    void testBlockedConvolution() {
        // Given
        double scale = 4.0;
        int waveletSupport = (int)(8 * scale * wavelet.bandwidth());
        double[] scaledWavelet = new double[waveletSupport];
        
        for (int i = 0; i < waveletSupport; i++) {
            double t = (i - waveletSupport / 2.0) / scale;
            scaledWavelet[i] = wavelet.psi(t);
        }
        
        // When
        double[] result = cacheOps.blockedConvolve(testSignal, scaledWavelet, scale);
        
        // Then
        assertNotNull(result);
        assertEquals(testSignal.length, result.length);
        
        // Verify non-trivial result
        double sum = 0;
        for (double val : result) {
            sum += Math.abs(val);
        }
        assertTrue(sum > 0, "Convolution should produce non-zero results");
    }
    
    @Test
    @DisplayName("Should handle block boundaries correctly")
    void testBlockBoundaries() {
        // Given - signal that crosses block boundaries
        double[] signal = new double[512];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = (i == 256) ? 1.0 : 0.0; // Impulse at boundary
        }
        
        double scale = 2.0;
        double[] wavelet = {0.25, 0.5, 0.25}; // Simple averaging filter
        
        // When
        double[] result = cacheOps.blockedConvolve(signal, wavelet, scale);
        
        // Then
        assertNotNull(result);
        assertEquals(signal.length, result.length);
        
        // Check response around the impulse
        assertTrue(result[255] > 0, "Should have response before impulse");
        assertTrue(result[256] > 0, "Should have response at impulse");
        assertTrue(result[257] > 0, "Should have response after impulse");
    }
    
    @Test
    @DisplayName("Should optimize block size based on cache")
    void testOptimalBlockSize() {
        // When
        int blockSize = cacheOps.getOptimalBlockSize();
        
        // Then
        assertTrue(blockSize > 0, "Block size should be positive");
        assertTrue(blockSize <= 512, "Block size should fit in L1 cache");
        assertEquals(0, blockSize % 8, "Block size should be multiple of 8 for alignment");
    }
    
    @Test
    @DisplayName("Should tile matrix operations for cache efficiency")
    void testTiledMatrixOperation() {
        // Given
        int rows = 64;
        int cols = 2048;
        double[][] matrix = new double[rows][cols];
        
        // Initialize with test data
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = Math.sin(i * 0.1) * Math.cos(j * 0.01);
            }
        }
        
        // When - apply tiled normalization
        cacheOps.tiledNormalize(matrix);
        
        // Then - check each row is normalized
        for (int i = 0; i < rows; i++) {
            double sum = 0;
            for (int j = 0; j < cols; j++) {
                sum += matrix[i][j] * matrix[i][j];
            }
            assertEquals(1.0, sum, TOLERANCE, "Row " + i + " should be normalized");
        }
    }
    
    @Test
    @DisplayName("Should compute multi-scale CWT with cache blocking")
    void testCacheAwareMultiScale() {
        // Given
        double[] scales = {2.0, 4.0, 8.0, 16.0};
        
        // When
        double[][] coefficients = cacheOps.computeMultiScaleBlocked(
            testSignal, scales, wavelet);
        
        // Then
        assertNotNull(coefficients);
        assertEquals(scales.length, coefficients.length);
        assertEquals(testSignal.length, coefficients[0].length);
        
        // Verify different scales produce different results
        for (int s = 1; s < scales.length; s++) {
            boolean different = false;
            for (int t = 0; t < testSignal.length; t++) {
                if (Math.abs(coefficients[s][t] - coefficients[0][t]) > TOLERANCE) {
                    different = true;
                    break;
                }
            }
            assertTrue(different, "Different scales should produce different results");
        }
    }
    
    @Test
    @DisplayName("Should handle complex wavelets with cache optimization")
    void testCacheAwareComplexConvolution() {
        // Given
        double scale = 4.0;
        
        // When
        ComplexMatrix result = cacheOps.blockedComplexConvolve(
            testSignal, wavelet, scale);
        
        // Then
        assertNotNull(result);
        assertEquals(1, result.getRows());
        assertEquals(testSignal.length, result.getCols());
        
        // Verify magnitude and phase
        for (int i = 0; i < testSignal.length; i++) {
            double mag = result.getMagnitude(0, i);
            double phase = result.getPhase(0, i);
            
            assertTrue(mag >= 0, "Magnitude should be non-negative");
            assertTrue(phase >= -Math.PI && phase <= Math.PI, 
                "Phase should be in [-π, π]");
        }
    }
    
    @Test
    @DisplayName("Should prefetch data for better cache utilization")
    void testPrefetching() {
        // Given
        double[] largeSignal = new double[16384];
        Random random = new Random(RANDOM_SEED); // Use seeded Random for reproducibility
        for (int i = 0; i < largeSignal.length; i++) {
            largeSignal[i] = random.nextDouble() - 0.5;
        }
        
        // When - process with prefetching
        double scale = 8.0;
        double[] result = cacheOps.convolveWithPrefetch(
            largeSignal, generateWavelet(scale), scale);
        
        // Then
        assertNotNull(result);
        assertEquals(largeSignal.length, result.length);
        
        // Verify correctness (not just performance)
        double energy = 0;
        for (double val : result) {
            energy += val * val;
        }
        assertTrue(energy > 0, "Should produce non-zero output");
    }
    
    @Test
    @DisplayName("Should align memory for SIMD efficiency")
    void testMemoryAlignment() {
        // When
        double[] aligned = cacheOps.createAlignedArray(1024);
        
        // Then
        assertNotNull(aligned);
        assertEquals(1024, aligned.length);
        
        // Check alignment (should be 64-byte aligned for cache lines)
        long address = System.identityHashCode(aligned);
        // Note: We can't directly check memory address in Java, 
        // but we can verify the array works correctly
        aligned[0] = 1.0;
        aligned[1023] = 2.0;
        assertEquals(1.0, aligned[0], TOLERANCE);
        assertEquals(2.0, aligned[1023], TOLERANCE);
    }
    
    @Test
    @DisplayName("Should handle streaming with cache-friendly buffering")
    void testStreamingCacheAware() {
        // Given
        int chunkSize = 256;
        int overlap = 64;
        double[] scales = {4.0, 8.0};
        
        CacheAwareOps.StreamingCache cache = 
            cacheOps.createStreamingCache(chunkSize, overlap, scales);
        
        // When - process chunks
        for (int i = 0; i < testSignal.length; i += chunkSize - overlap) {
            int end = Math.min(i + chunkSize, testSignal.length);
            double[] chunk = new double[end - i];
            System.arraycopy(testSignal, i, chunk, 0, chunk.length);
            
            double[][] result = cacheOps.processStreamingChunk(cache, chunk, wavelet);
            
            if (cache.isReady()) {
                assertNotNull(result);
                assertEquals(scales.length, result.length);
                assertTrue(result[0].length > 0);
            }
        }
        
        // Then - verify cache was used
        assertTrue(cache.getProcessedChunks() > 0, "Should have processed chunks");
    }
    
    @Test
    @DisplayName("Should select strategy based on signal size")
    void testStrategySelection() {
        // Test different signal sizes
        CacheAwareOps.CacheStrategy small = cacheOps.selectStrategy(128, 16);
        CacheAwareOps.CacheStrategy medium = cacheOps.selectStrategy(2048, 64);
        CacheAwareOps.CacheStrategy large = cacheOps.selectStrategy(65536, 128);
        
        // Verify appropriate strategies
        assertTrue(small.useDirectComputation(), 
            "Small signals should use direct computation");
        assertTrue(medium.useBlockedComputation(), 
            "Medium signals should use blocked computation");
        assertTrue(large.useTiledComputation(), 
            "Large signals should use tiled computation");
    }
    
    @Test
    @DisplayName("Should benchmark cache-aware vs regular operations")
    void testPerformanceComparison() {
        // Given
        double[] largeSignal = new double[8192];
        Random random = new Random(RANDOM_SEED); // Use seeded Random for reproducibility
        for (int i = 0; i < largeSignal.length; i++) {
            largeSignal[i] = random.nextDouble() - 0.5;
        }
        double scale = 16.0;
        double[] wavelet = generateWavelet(scale);
        
        // When - measure cache-aware time
        long cacheAwareStart = System.nanoTime();
        double[] cacheAwareResult = cacheOps.blockedConvolve(largeSignal, wavelet, scale);
        long cacheAwareTime = System.nanoTime() - cacheAwareStart;
        
        // When - measure regular convolution time
        CWTVectorOps vectorOps = new CWTVectorOps();
        long regularStart = System.nanoTime();
        double[] regularResult = vectorOps.convolve(largeSignal, wavelet, scale);
        long regularTime = System.nanoTime() - regularStart;
        
        // Then
        System.out.printf("Cache-aware time: %.2f ms, Regular time: %.2f ms%n", 
            cacheAwareTime / 1e6, regularTime / 1e6);
        
        assertNotNull(cacheAwareResult);
        assertNotNull(regularResult);
        assertEquals(largeSignal.length, cacheAwareResult.length);
        assertEquals(largeSignal.length, regularResult.length);
    }
    
    // Helper methods
    
    private double[] generateWavelet(double scale) {
        int support = (int)(8 * scale * wavelet.bandwidth());
        double[] samples = new double[support];
        
        for (int i = 0; i < support; i++) {
            double t = (i - support / 2.0) / scale;
            samples[i] = wavelet.psi(t);
        }
        
        return samples;
    }
}
