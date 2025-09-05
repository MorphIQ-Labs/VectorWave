package com.morphiqlabs.wavelet.extensions.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for SpecializedKernels class.
 * Tests optimized SIMD kernels for specific wavelets.
 */
@DisplayName("SpecializedKernels Test Suite")
class SpecializedKernelsTest {
    
    private static final double EPSILON = 1e-10;
    
    // ==========================================
    // DB4 Forward Transform Tests
    // ==========================================
    
    @Test
    @DisplayName("Test DB4 forward optimized with basic signal")
    void testDB4ForwardOptimizedBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        SpecializedKernels.db4ForwardOptimized(signal, approx, detail, signal.length);
        
        // Verify results are reasonable (not NaN/Infinity)
        assertNotNull(approx);
        assertNotNull(detail);
        assertEquals(4, approx.length);
        assertEquals(4, detail.length);
        
        for (int i = 0; i < 4; i++) {
            assertFalse(Double.isNaN(approx[i]), "Approx coefficient " + i + " should not be NaN");
            assertFalse(Double.isInfinite(approx[i]), "Approx coefficient " + i + " should not be infinite");
            assertFalse(Double.isNaN(detail[i]), "Detail coefficient " + i + " should not be NaN");
            assertFalse(Double.isInfinite(detail[i]), "Detail coefficient " + i + " should not be infinite");
        }
    }
    
    @Test
    @DisplayName("Test DB4 forward optimized with zeros")
    void testDB4ForwardOptimizedWithZeros() {
        double[] signal = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        SpecializedKernels.db4ForwardOptimized(signal, approx, detail, signal.length);
        
        // Zero input should produce zero output
        for (int i = 0; i < 4; i++) {
            assertEquals(0.0, approx[i], EPSILON, "Zero signal should produce zero approx");
            assertEquals(0.0, detail[i], EPSILON, "Zero signal should produce zero detail");
        }
    }
    
    @Test
    @DisplayName("Test DB4 forward optimized with constant signal")
    void testDB4ForwardOptimizedConstant() {
        double[] signal = {5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0, 5.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        SpecializedKernels.db4ForwardOptimized(signal, approx, detail, signal.length);
        
        // Constant signal should have zero detail coefficients
        for (int i = 0; i < 4; i++) {
            assertTrue(Math.abs(detail[i]) < 0.1, "Detail coefficients should be near zero for constant signal");
            assertTrue(approx[i] > 0, "Approx coefficients should be positive for positive constant");
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {8, 16, 32, 64, 128})
    @DisplayName("Test DB4 forward optimized with various signal lengths")
    void testDB4ForwardOptimizedVariousLengths(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / length) + 1.0;
        }
        
        double[] approx = new double[length / 2];
        double[] detail = new double[length / 2];
        
        assertDoesNotThrow(() -> {
            SpecializedKernels.db4ForwardOptimized(signal, approx, detail, length);
        });
        
        // Verify all coefficients are reasonable
        for (int i = 0; i < length / 2; i++) {
            assertFalse(Double.isNaN(approx[i]));
            assertFalse(Double.isNaN(detail[i]));
            assertFalse(Double.isInfinite(approx[i]));
            assertFalse(Double.isInfinite(detail[i]));
        }
    }
    
    // ==========================================
    // Symlet 4 Forward Transform Tests
    // ==========================================
    
    @Test
    @DisplayName("Test Sym4 forward optimized with basic signal")
    void testSym4ForwardOptimizedBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        SpecializedKernels.sym4ForwardOptimized(signal, approx, detail, signal.length);
        
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
    @DisplayName("Test Sym4 forward optimized with zeros")
    void testSym4ForwardOptimizedWithZeros() {
        double[] signal = {0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        SpecializedKernels.sym4ForwardOptimized(signal, approx, detail, signal.length);
        
        // Zero input should produce zero output
        for (int i = 0; i < 4; i++) {
            assertEquals(0.0, approx[i], EPSILON);
            assertEquals(0.0, detail[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test Sym4 forward optimized with constant signal")
    void testSym4ForwardOptimizedConstant() {
        double[] signal = {3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0, 3.0};
        double[] approx = new double[4];
        double[] detail = new double[4];
        
        SpecializedKernels.sym4ForwardOptimized(signal, approx, detail, signal.length);
        
        // Constant signal should have small detail coefficients
        for (int i = 0; i < 4; i++) {
            assertTrue(Math.abs(detail[i]) < 0.1, "Detail should be near zero for constant signal");
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {8, 16, 32, 64})
    @DisplayName("Test Sym4 forward optimized with various lengths")
    void testSym4ForwardOptimizedVariousLengths(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.cos(2 * Math.PI * i / length);
        }
        
        double[] approx = new double[length / 2];
        double[] detail = new double[length / 2];
        
        assertDoesNotThrow(() -> {
            SpecializedKernels.sym4ForwardOptimized(signal, approx, detail, length);
        });
        
        for (int i = 0; i < length / 2; i++) {
            assertFalse(Double.isNaN(approx[i]));
            assertFalse(Double.isNaN(detail[i]));
        }
    }
    
    // ==========================================
    // Haar Batch Optimized Tests
    // ==========================================
    
    @Test
    @DisplayName("Test Haar batch optimized with 4 signals")
    void testHaarBatchOptimized4Signals() {
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0},
            {2.0, 4.0, 6.0, 8.0},
            {1.0, 1.0, 2.0, 2.0},
            {0.0, 1.0, 0.0, 1.0}
        };
        
        double[][] approx = new double[4][2];
        double[][] detail = new double[4][2];
        
        SpecializedKernels.haarBatchOptimized(signals, approx, detail);
        
        // Verify all results
        for (int s = 0; s < 4; s++) {
            for (int i = 0; i < 2; i++) {
                assertFalse(Double.isNaN(approx[s][i]));
                assertFalse(Double.isNaN(detail[s][i]));
                assertFalse(Double.isInfinite(approx[s][i]));
                assertFalse(Double.isInfinite(detail[s][i]));
            }
        }
        
        // Verify Haar transform properties
        double sqrt2Inv = 1.0 / Math.sqrt(2.0);
        assertEquals((1.0 + 2.0) * sqrt2Inv, approx[0][0], EPSILON);
        assertEquals((1.0 - 2.0) * sqrt2Inv, detail[0][0], EPSILON);
    }
    
    @Test
    @DisplayName("Test Haar batch optimized with remainder signals")
    void testHaarBatchOptimizedRemainder() {
        // 5 signals (not divisible by 4)
        double[][] signals = {
            {1.0, 2.0},
            {3.0, 4.0},
            {5.0, 6.0},
            {7.0, 8.0},
            {9.0, 10.0}
        };
        
        double[][] approx = new double[5][1];
        double[][] detail = new double[5][1];
        
        assertDoesNotThrow(() -> {
            SpecializedKernels.haarBatchOptimized(signals, approx, detail);
        });
        
        // Verify all signals were processed
        for (int s = 0; s < 5; s++) {
            assertFalse(Double.isNaN(approx[s][0]));
            assertFalse(Double.isNaN(detail[s][0]));
        }
    }
    
    @Test
    @DisplayName("Test Haar batch optimized with single signal")
    void testHaarBatchOptimizedSingle() {
        double[][] signals = {{1.0, 2.0, 3.0, 4.0}};
        double[][] approx = new double[1][2];
        double[][] detail = new double[1][2];
        
        assertDoesNotThrow(() -> {
            SpecializedKernels.haarBatchOptimized(signals, approx, detail);
        });
        
        assertFalse(Double.isNaN(approx[0][0]));
        assertFalse(Double.isNaN(detail[0][0]));
    }
    
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 4, 5, 8, 12})
    @DisplayName("Test Haar batch optimized with various batch sizes")
    void testHaarBatchOptimizedVariousBatchSizes(int numSignals) {
        double[][] signals = new double[numSignals][8];
        double[][] approx = new double[numSignals][4];
        double[][] detail = new double[numSignals][4];
        
        // Initialize signals
        for (int s = 0; s < numSignals; s++) {
            for (int i = 0; i < 8; i++) {
                signals[s][i] = s + i + 1.0;
            }
        }
        
        assertDoesNotThrow(() -> {
            SpecializedKernels.haarBatchOptimized(signals, approx, detail);
        });
        
        // Verify all results
        for (int s = 0; s < numSignals; s++) {
            for (int i = 0; i < 4; i++) {
                assertFalse(Double.isNaN(approx[s][i]));
                assertFalse(Double.isNaN(detail[s][i]));
            }
        }
    }
    
    // ==========================================
    // Convolution with Prefetch Tests
    // ==========================================
    
    @Test
    @DisplayName("Test convolution with prefetch basic")
    void testConvolveWithPrefetchBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.5, 0.5};
        
        double[] result = SpecializedKernels.convolveWithPrefetch(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test convolution with prefetch larger filter")
    void testConvolveWithPrefetchLargerFilter() {
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length);
        }
        
        double[] filter = {0.25, 0.5, 0.25, 0.1, 0.05, 0.05};
        
        double[] result = SpecializedKernels.convolveWithPrefetch(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(32, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @ParameterizedTest
    @CsvSource({
        "16, 2",
        "32, 4",
        "64, 6",
        "128, 8"
    })
    @DisplayName("Test convolution with prefetch various sizes")
    void testConvolveWithPrefetchVariousSizes(int signalLength, int filterLength) {
        double[] signal = new double[signalLength];
        double[] filter = new double[filterLength];
        
        // Initialize with test data
        for (int i = 0; i < signalLength; i++) {
            signal[i] = Math.cos(2 * Math.PI * i / signalLength) + 0.5;
        }
        for (int i = 0; i < filterLength; i++) {
            filter[i] = 1.0 / filterLength;
        }
        
        assertDoesNotThrow(() -> {
            double[] result = SpecializedKernels.convolveWithPrefetch(
                signal, filter, signalLength, filterLength);
            
            assertNotNull(result);
            assertEquals(signalLength / 2, result.length);
        });
    }
    
    // ==========================================
    // Convolution with Precomputed Indices Tests
    // ==========================================
    
    @Test
    @DisplayName("Test convolution with precomputed indices basic")
    void testConvolvePrecomputedIndicesBasic() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.7071067811865475, 0.7071067811865475}; // Haar
        
        double[] result = SpecializedKernels.convolvePrecomputedIndices(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(4, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test convolution with precomputed indices unrolled filter")
    void testConvolvePrecomputedIndicesUnrolled() {
        double[] signal = new double[32];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
        }
        
        // 8-tap filter to test unrolling by 4
        double[] filter = {0.1, 0.2, 0.15, 0.1, 0.1, 0.15, 0.15, 0.05};
        
        double[] result = SpecializedKernels.convolvePrecomputedIndices(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(16, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @Test
    @DisplayName("Test convolution with precomputed indices remainder handling")
    void testConvolvePrecomputedIndicesRemainder() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        // 3-tap filter (not divisible by 4 for unrolling)
        double[] filter = {0.33, 0.34, 0.33};
        
        double[] result = SpecializedKernels.convolvePrecomputedIndices(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(result);
        assertEquals(3, result.length);
        
        for (double value : result) {
            assertFalse(Double.isNaN(value));
            assertFalse(Double.isInfinite(value));
        }
    }
    
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 8, 12})
    @DisplayName("Test convolution with precomputed indices various filter lengths")
    void testConvolvePrecomputedIndicesVariousFilterLengths(int filterLength) {
        double[] signal = new double[64];
        double[] filter = new double[filterLength];
        
        // Initialize test data
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / signal.length);
        }
        for (int i = 0; i < filterLength; i++) {
            filter[i] = 1.0 / filterLength;
        }
        
        assertDoesNotThrow(() -> {
            double[] result = SpecializedKernels.convolvePrecomputedIndices(
                signal, filter, signal.length, filterLength);
            
            assertNotNull(result);
            assertEquals(32, result.length);
            
            for (double value : result) {
                assertFalse(Double.isNaN(value));
                assertFalse(Double.isInfinite(value));
            }
        });
    }
    
    // ==========================================
    // Edge Cases and Consistency Tests
    // ==========================================
    
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
            
            SpecializedKernels.db4ForwardOptimized(signal, approx, detail, signal.length);
            SpecializedKernels.sym4ForwardOptimized(signal, approx, detail, signal.length);
            
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
    @DisplayName("Test consistency between convolution methods")
    void testConvolutionMethodConsistency() {
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.25, 0.5, 0.25};
        
        double[] prefetchResult = SpecializedKernels.convolveWithPrefetch(
            signal, filter, signal.length, filter.length);
        
        double[] precomputedResult = SpecializedKernels.convolvePrecomputedIndices(
            signal, filter, signal.length, filter.length);
        
        assertNotNull(prefetchResult);
        assertNotNull(precomputedResult);
        assertEquals(prefetchResult.length, precomputedResult.length);
        
        // Results should be similar (within tolerance)
        for (int i = 0; i < prefetchResult.length; i++) {
            assertEquals(prefetchResult[i], precomputedResult[i], EPSILON,
                "Convolution methods should produce consistent results at index " + i);
        }
    }
    
    @Test
    @DisplayName("Test minimal signal lengths")
    void testMinimalSignalLengths() {
        // Test with minimal valid signal (8 samples for 8-tap filters)
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        
        assertDoesNotThrow(() -> {
            double[] approx = new double[4];
            double[] detail = new double[4];
            
            SpecializedKernels.db4ForwardOptimized(signal, approx, detail, signal.length);
            SpecializedKernels.sym4ForwardOptimized(signal, approx, detail, signal.length);
            
            double[] convResult = SpecializedKernels.convolveWithPrefetch(
                signal, new double[]{0.5, 0.5}, signal.length, 2);
            
            assertNotNull(convResult);
            assertEquals(4, convResult.length);
        });
    }
}
