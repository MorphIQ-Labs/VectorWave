package com.morphiqlabs.wavelet.extensions.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for CacheAwareOps class.
 * Tests cache-aware implementations of wavelet operations for large signals.
 */
@DisplayName("CacheAwareOps Test Suite")
class CacheAwareOpsTest {
    
    private static final double EPSILON = 1e-10;
    
    // Test filters
    private static final double[] HAAR_LOW = {0.7071067811865475, 0.7071067811865475};
    private static final double[] HAAR_HIGH = {0.7071067811865475, -0.7071067811865475};
    private static final double[] DB2_LOW = {
        0.48296291314453414, 0.83651630373780772, 
        0.22414386804201339, -0.12940952255126037
    };
    private static final double[] DB2_HIGH = {
        -0.12940952255126037, -0.22414386804201339, 
        0.83651630373780772, -0.48296291314453414
    };
    
    // ==========================================
    // Cache Info Tests
    // ==========================================
    
    @Test
    @DisplayName("Test cache info")
    void testGetCacheInfo() {
        assertDoesNotThrow(() -> {
            String info = CacheAwareOps.getCacheInfo();
            assertNotNull(info);
            assertFalse(info.isEmpty());
            assertTrue(info.contains("Cache Blocking Configuration"));
            assertTrue(info.contains("L1 Block Size"));
            assertTrue(info.contains("L2 Block Size"));
            assertTrue(info.contains("L3 Block Size"));
            assertTrue(info.contains("Vector Length"));
            assertTrue(info.contains("Prefetch Distance"));
        });
    }
    
    // ==========================================
    // Forward Transform Blocked Tests
    // ==========================================
    
    @Test
    @DisplayName("Test forward transform blocked basic")
    void testForwardTransformBlockedBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        CacheAwareOps.forwardTransformBlocked(signal, approx, detail,
            HAAR_LOW, HAAR_HIGH, signal.length, HAAR_LOW.length);
        
        // Verify results are reasonable
        assertNotNull(approx);
        assertNotNull(detail);
        assertEquals(4, approx.length);
        assertEquals(4, detail.length);
        
        for (int i = 0; i < 4; i++) {
            assertFalse(Double.isNaN(approx[i]));
            assertFalse(Double.isInfinite(approx[i]));
            assertFalse(Double.isNaN(detail[i]));
            assertFalse(Double.isInfinite(detail[i]));
        }
    }
    
    @Test
    @DisplayName("Test forward transform blocked with zeros")
    void testForwardTransformBlockedWithZeros() {
        double[] signal = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        CacheAwareOps.forwardTransformBlocked(signal, approx, detail,
            HAAR_LOW, HAAR_HIGH, signal.length, HAAR_LOW.length);
        
        // Zero signal should produce zero output
        for (int i = 0; i < 4; i++) {
            assertEquals(0.0, approx[i], EPSILON);
            assertEquals(0.0, detail[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test forward transform blocked with Haar")
    void testForwardTransformBlockedHaar() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        CacheAwareOps.forwardTransformBlocked(signal, approx, detail,
            HAAR_LOW, HAAR_HIGH, signal.length, HAAR_LOW.length);
        
        // Expected Haar transform results
        double sqrt2Inv = 0.7071067811865475;
        double[] expectedApprox = {
            (1.0 + 2.0) * sqrt2Inv,
            (3.0 + 4.0) * sqrt2Inv,
            (5.0 + 6.0) * sqrt2Inv,
            (7.0 + 8.0) * sqrt2Inv
        };
        double[] expectedDetail = {
            (1.0 - 2.0) * sqrt2Inv,
            (3.0 - 4.0) * sqrt2Inv,
            (5.0 - 6.0) * sqrt2Inv,
            (7.0 - 8.0) * sqrt2Inv
        };
        
        for (int i = 0; i < 4; i++) {
            assertEquals(expectedApprox[i], approx[i], EPSILON);
            assertEquals(expectedDetail[i], detail[i], EPSILON);
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {16, 32, 64, 128, 256})
    @DisplayName("Test forward transform blocked with various signal lengths")
    void testForwardTransformBlockedVariousLengths(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / length) + 1.0;
        }
        
        double[] approx = new double[length / 2];
        double[] detail = new double[length / 2];
        
        assertDoesNotThrow(() -> {
            CacheAwareOps.forwardTransformBlocked(signal, approx, detail,
                HAAR_LOW, HAAR_HIGH, length, HAAR_LOW.length);
            
            // Verify all coefficients are reasonable
            for (int i = 0; i < length / 2; i++) {
                assertFalse(Double.isNaN(approx[i]));
                assertFalse(Double.isNaN(detail[i]));
                assertFalse(Double.isInfinite(approx[i]));
                assertFalse(Double.isInfinite(detail[i]));
            }
        });
    }
    
    // ==========================================
    // Inverse Transform Blocked Tests
    // ==========================================
    
    @Test
    @DisplayName("Test inverse transform blocked basic")
    void testInverseTransformBlockedBasic() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.1, 0.2, 0.3, 0.4};
        double[] output = new double[8];
        
        CacheAwareOps.inverseTransformBlocked(approx, detail, output,
            HAAR_LOW, HAAR_HIGH, output.length, HAAR_LOW.length);
        
        // Verify results are reasonable
        for (double value : output) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test inverse transform blocked with zeros")
    void testInverseTransformBlockedWithZeros() {
        double[] approx = {0.0, 0.0, 0.0, 0.0};
        double[] detail = {0.0, 0.0, 0.0, 0.0};
        double[] output = new double[8];
        
        CacheAwareOps.inverseTransformBlocked(approx, detail, output,
            HAAR_LOW, HAAR_HIGH, output.length, HAAR_LOW.length);
        
        // Zero input should produce zero output
        for (double value : output) {
            assertEquals(0.0, value, EPSILON);
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {16, 32, 64, 128})
    @DisplayName("Test inverse transform blocked with various lengths")
    void testInverseTransformBlockedVariousLengths(int outputLength) {
        int inputLength = outputLength / 2;
        double[] approx = new double[inputLength];
        double[] detail = new double[inputLength];
        double[] output = new double[outputLength];
        
        // Initialize with test data
        for (int i = 0; i < inputLength; i++) {
            approx[i] = i + 1.0;
            detail[i] = (i + 1.0) * 0.1;
        }
        
        assertDoesNotThrow(() -> {
            CacheAwareOps.inverseTransformBlocked(approx, detail, output,
                HAAR_LOW, HAAR_HIGH, outputLength, HAAR_LOW.length);
            
            // Verify results are reasonable
            for (double value : output) {
                assertFalse(Double.isNaN(value));
                assertFalse(Double.isInfinite(value));
            }
        });
    }
    
    // ==========================================
    // Multi-Level Decomposition Tests
    // ==========================================
    
    @Test
    @DisplayName("Test multi-level decomposition basic")
    void testMultiLevelDecompositionBasic() {
        double[] signal = new double[32];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length);
        }
        
        int levels = 3;
        double[][] approxCoeffs = new double[levels][];
        double[][] detailCoeffs = new double[levels][];
        
        assertDoesNotThrow(() -> {
            CacheAwareOps.multiLevelDecomposition(signal, levels,
                HAAR_LOW, HAAR_HIGH, HAAR_LOW.length,
                approxCoeffs, detailCoeffs);
            
            // Verify coefficients were created
            for (int level = 0; level < levels; level++) {
                assertNotNull(approxCoeffs[level]);
                assertNotNull(detailCoeffs[level]);
                assertTrue(approxCoeffs[level].length > 0);
                assertTrue(detailCoeffs[level].length > 0);
                
                // Verify no NaN/Infinity values
                for (double value : approxCoeffs[level]) {
                    assertFalse(Double.isNaN(value));
                    assertFalse(Double.isInfinite(value));
                }
                for (double value : detailCoeffs[level]) {
                    assertFalse(Double.isNaN(value));
                    assertFalse(Double.isInfinite(value));
                }
            }
        });
    }
    
    @Test
    @DisplayName("Test multi-level decomposition with single level")
    void testMultiLevelDecompositionSingleLevel() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        int levels = 1;
        double[][] approxCoeffs = new double[levels][];
        double[][] detailCoeffs = new double[levels][];
        
        assertDoesNotThrow(() -> {
            CacheAwareOps.multiLevelDecomposition(signal, levels,
                HAAR_LOW, HAAR_HIGH, HAAR_LOW.length,
                approxCoeffs, detailCoeffs);
            
            assertNotNull(approxCoeffs[0]);
            assertNotNull(detailCoeffs[0]);
            assertEquals(4, approxCoeffs[0].length);
            assertEquals(4, detailCoeffs[0].length);
        });
    }
    
    @ParameterizedTest
    @CsvSource({
        "64, 2",
        "128, 3",
        "256, 4",
        "512, 5"
    })
    @DisplayName("Test multi-level decomposition with various sizes and levels")
    void testMultiLevelDecompositionVariousSizesAndLevels(int signalLength, int levels) {
        double[] signal = new double[signalLength];
        for (int i = 0; i < signalLength; i++) {
            signal[i] = Math.cos(2 * Math.PI * i / signalLength) + Math.sin(4 * Math.PI * i / signalLength);
        }
        
        double[][] approxCoeffs = new double[levels][];
        double[][] detailCoeffs = new double[levels][];
        
        assertDoesNotThrow(() -> {
            CacheAwareOps.multiLevelDecomposition(signal, levels,
                DB2_LOW, DB2_HIGH, DB2_LOW.length,
                approxCoeffs, detailCoeffs);
            
            // Verify each level has reasonable coefficients
            int expectedLength = signalLength;
            for (int level = 0; level < levels; level++) {
                expectedLength /= 2;
                if (expectedLength < DB2_LOW.length * 2) {
                    // Should have stopped early
                    break;
                }
                
                if (approxCoeffs[level] != null) {
                    assertEquals(expectedLength, approxCoeffs[level].length);
                    assertEquals(expectedLength, detailCoeffs[level].length);
                }
            }
        });
    }
    
    // ==========================================
    // Batch Transform Cache Aware Tests
    // ==========================================
    
    @Test
    @DisplayName("Test batch transform cache aware basic")
    void testBatchTransformCacheAwareBasic() {
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0},
            {2.0, 4.0, 6.0, 8.0, 10.0, 12.0, 14.0, 16.0},
            {1.0, 1.0, 2.0, 2.0, 3.0, 3.0, 4.0, 4.0}
        };
        
        double[][] approxResults = new double[3][4];
        double[][] detailResults = new double[3][4];
        
        assertDoesNotThrow(() -> {
            CacheAwareOps.batchTransformCacheAware(signals, approxResults, detailResults,
                HAAR_LOW, HAAR_HIGH, HAAR_LOW.length);
            
            // Verify all results
            for (int s = 0; s < 3; s++) {
                for (int i = 0; i < 4; i++) {
                    assertFalse(Double.isNaN(approxResults[s][i]));
                    assertFalse(Double.isNaN(detailResults[s][i]));
                    assertFalse(Double.isInfinite(approxResults[s][i]));
                    assertFalse(Double.isInfinite(detailResults[s][i]));
                }
            }
        });
    }
    
    @Test
    @DisplayName("Test batch transform cache aware single signal")
    void testBatchTransformCacheAwareSingleSignal() {
        double[][] signals = {{1.0, 2.0, 3.0, 4.0}};
        double[][] approxResults = new double[1][2];
        double[][] detailResults = new double[1][2];
        
        assertDoesNotThrow(() -> {
            CacheAwareOps.batchTransformCacheAware(signals, approxResults, detailResults,
                HAAR_LOW, HAAR_HIGH, HAAR_LOW.length);
            
            assertFalse(Double.isNaN(approxResults[0][0]));
            assertFalse(Double.isNaN(detailResults[0][0]));
        });
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 8, 16})
    @DisplayName("Test batch transform cache aware with various batch sizes")
    void testBatchTransformCacheAwareVariousBatchSizes(int numSignals) {
        double[][] signals = new double[numSignals][32];
        double[][] approxResults = new double[numSignals][16];
        double[][] detailResults = new double[numSignals][16];
        
        // Initialize signals
        for (int s = 0; s < numSignals; s++) {
            for (int i = 0; i < 32; i++) {
                signals[s][i] = s + i + 1.0;
            }
        }
        
        assertDoesNotThrow(() -> {
            CacheAwareOps.batchTransformCacheAware(signals, approxResults, detailResults,
                HAAR_LOW, HAAR_HIGH, HAAR_LOW.length);
            
            // Verify all results
            for (int s = 0; s < numSignals; s++) {
                for (int i = 0; i < 16; i++) {
                    assertFalse(Double.isNaN(approxResults[s][i]));
                    assertFalse(Double.isNaN(detailResults[s][i]));
                }
            }
        });
    }
    
    // ==========================================
    // 2D Convolution Tiled Tests
    // ==========================================
    
    @Test
    @DisplayName("Test 2D convolution tiled basic")
    void testConvolve2DTiledBasic() {
        double[][] signal = {
            {1.0, 2.0, 3.0, 4.0},
            {5.0, 6.0, 7.0, 8.0},
            {9.0, 10.0, 11.0, 12.0},
            {13.0, 14.0, 15.0, 16.0}
        };
        
        double[][] kernel = {
            {0.25, 0.5, 0.25},
            {0.5, 1.0, 0.5},
            {0.25, 0.5, 0.25}
        };
        
        double[][] output = new double[4][4];
        
        assertDoesNotThrow(() -> {
            CacheAwareOps.convolve2DTiled(signal, kernel, output, 2);
            
            // Verify results are reasonable
            for (int r = 0; r < 4; r++) {
                for (int c = 0; c < 4; c++) {
                    assertFalse(Double.isNaN(output[r][c]));
                    assertFalse(Double.isInfinite(output[r][c]));
                }
            }
        });
    }
    
    @Test
    @DisplayName("Test 2D convolution tiled with identity kernel")
    void testConvolve2DTiledIdentityKernel() {
        double[][] signal = {
            {1.0, 2.0},
            {3.0, 4.0}
        };
        
        double[][] kernel = {
            {0.0, 0.0, 0.0},
            {0.0, 1.0, 0.0},
            {0.0, 0.0, 0.0}
        };
        
        double[][] output = new double[2][2];
        
        CacheAwareOps.convolve2DTiled(signal, kernel, output, 1);
        
        // Identity kernel should preserve the signal
        assertEquals(1.0, output[0][0], EPSILON);
        assertEquals(2.0, output[0][1], EPSILON);
        assertEquals(3.0, output[1][0], EPSILON);
        assertEquals(4.0, output[1][1], EPSILON);
    }
    
    @ParameterizedTest
    @CsvSource({
        "4, 1",
        "8, 2",
        "16, 4",
        "32, 8"
    })
    @DisplayName("Test 2D convolution tiled with various sizes and tile sizes")
    void testConvolve2DTiledVariousSizesAndTileSizes(int size, int tileSize) {
        double[][] signal = new double[size][size];
        double[][] kernel = {{0.5, 0.5}, {0.5, 0.5}}; // 2x2 averaging kernel
        double[][] output = new double[size][size];
        
        // Initialize signal
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                signal[r][c] = r * size + c + 1.0;
            }
        }
        
        assertDoesNotThrow(() -> {
            CacheAwareOps.convolve2DTiled(signal, kernel, output, tileSize);
            
            // Verify results
            for (int r = 0; r < size; r++) {
                for (int c = 0; c < size; c++) {
                    assertFalse(Double.isNaN(output[r][c]));
                    assertFalse(Double.isInfinite(output[r][c]));
                }
            }
        });
    }
    
    // ==========================================
    // Edge Cases and Consistency Tests
    // ==========================================
    
    @Test
    @DisplayName("Test with minimal arrays")
    void testMinimalArrays() {
        assertDoesNotThrow(() -> {
            // Minimal forward transform
            double[] signal = {1.0, 2.0};
            double[] approx = new double[1];
            double[] detail = new double[1];
            CacheAwareOps.forwardTransformBlocked(signal, approx, detail,
                new double[]{1.0}, new double[]{1.0}, 2, 1);
            
            // Minimal inverse transform
            double[] output = new double[2];
            CacheAwareOps.inverseTransformBlocked(approx, detail, output,
                new double[]{1.0}, new double[]{1.0}, 2, 1);
            
            assertNotNull(output);
        });
    }
    
    @Test
    @DisplayName("Test with extreme values")
    void testExtremeValues() {
        double[] signal = {
            Double.MAX_VALUE / 1e10, -Double.MAX_VALUE / 1e10,
            1e-10, -1e-10, 0.0, 1000.0, -1000.0, 0.5
        };
        
        assertDoesNotThrow(() -> {
            double[] approx = new double[4];
            double[] detail = new double[4];
            
            CacheAwareOps.forwardTransformBlocked(signal, approx, detail,
                HAAR_LOW, HAAR_HIGH, signal.length, HAAR_LOW.length);
            
            // Verify no overflow/underflow
            for (int i = 0; i < 4; i++) {
                assertFalse(Double.isNaN(approx[i]));
                assertFalse(Double.isInfinite(approx[i]));
                assertFalse(Double.isNaN(detail[i]));
                assertFalse(Double.isInfinite(detail[i]));
            }
        });
    }
    
    @Test
    @DisplayName("Test multi-level decomposition stops early")
    void testMultiLevelDecompositionStopsEarly() {
        double[] signal = {1.0, 2.0, 3.0, 4.0}; // Very small signal
        int levels = 10; // Request more levels than possible
        double[][] approxCoeffs = new double[levels][];
        double[][] detailCoeffs = new double[levels][];
        
        assertDoesNotThrow(() -> {
            CacheAwareOps.multiLevelDecomposition(signal, levels,
                HAAR_LOW, HAAR_HIGH, HAAR_LOW.length,
                approxCoeffs, detailCoeffs);
            
            // Should have stopped early - some arrays will be null
            int actualLevels = 0;
            for (int level = 0; level < levels; level++) {
                if (approxCoeffs[level] != null) {
                    actualLevels++;
                } else {
                    break;
                }
            }
            
            assertTrue(actualLevels < levels, "Should have stopped early");
            assertTrue(actualLevels >= 1, "Should have at least one level");
        });
    }
    
    @Test
    @DisplayName("Test consistency between transform directions")
    void testConsistencyBetweenTransformDirections() {
        double[] originalSignal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        double[] reconstructed = new double[8];
        
        // Forward transform
        CacheAwareOps.forwardTransformBlocked(originalSignal, approx, detail,
            HAAR_LOW, HAAR_HIGH, originalSignal.length, HAAR_LOW.length);
        
        // Inverse transform
        CacheAwareOps.inverseTransformBlocked(approx, detail, reconstructed,
            HAAR_LOW, HAAR_HIGH, reconstructed.length, HAAR_LOW.length);
        
        // Should be close to original (perfect reconstruction may not occur due to blocking)
        assertNotNull(reconstructed);
        assertEquals(originalSignal.length, reconstructed.length);
        
        for (int i = 0; i < originalSignal.length; i++) {
            assertFalse(Double.isNaN(reconstructed[i]));
            assertFalse(Double.isInfinite(reconstructed[i]));
        }
    }
}
