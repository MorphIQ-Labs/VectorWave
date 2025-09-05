package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidSignalException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;

import java.util.Random;
import java.util.concurrent.ForkJoinPool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for parallel multi-level MODWT implementation.
 */
class ParallelMultiLevelMODWTTest {
    
    private static final double EPSILON = 1e-7;
    private ParallelMultiLevelMODWT parallelTransform;
    private MultiLevelMODWTTransform sequentialTransform;
    
    @BeforeEach
    void setUp() {
        parallelTransform = new ParallelMultiLevelMODWT();
    }
    
    @Test
    void testParallelMatchesSequential_Haar() {
        // Simple test signal
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        Haar wavelet = new Haar();
        BoundaryMode mode = BoundaryMode.PERIODIC;
        int levels = 3;
        
        // Sequential transform
        sequentialTransform = new MultiLevelMODWTTransform(wavelet, mode);
        MultiLevelMODWTResult sequentialResult = sequentialTransform.decompose(signal, levels);
        
        // Parallel transform
        MultiLevelMODWTResult parallelResult = parallelTransform.decompose(signal, wavelet, mode, levels);
        
        // Compare results
        assertArrayEquals(
            sequentialResult.getApproximationCoeffs(),
            parallelResult.getApproximationCoeffs(),
            EPSILON,
            "Approximation coefficients should match"
        );
        
        for (int level = 1; level <= levels; level++) {
            assertArrayEquals(
                sequentialResult.getDetailCoeffsAtLevel(level),
                parallelResult.getDetailCoeffsAtLevel(level),
                EPSILON,
                "Detail coefficients at level " + level + " should match"
            );
        }
    }
    
    @Test
    void testParallelMatchesSequential_DB4() {
        // Larger test signal
        double[] signal = new double[128];
        Random random = new Random(42);
        for (int i = 0; i < signal.length; i++) {
            signal[i] = random.nextGaussian();
        }
        
        var wavelet = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.PERIODIC;
        int levels = 4;
        
        // Sequential transform
        sequentialTransform = new MultiLevelMODWTTransform(wavelet, mode);
        MultiLevelMODWTResult sequentialResult = sequentialTransform.decompose(signal, levels);
        
        // Parallel transform
        MultiLevelMODWTResult parallelResult = parallelTransform.decompose(signal, wavelet, mode, levels);
        
        // Compare results
        assertArrayEquals(
            sequentialResult.getApproximationCoeffs(),
            parallelResult.getApproximationCoeffs(),
            EPSILON,
            "Approximation coefficients should match for DB4"
        );
        
        for (int level = 1; level <= levels; level++) {
            assertArrayEquals(
                sequentialResult.getDetailCoeffsAtLevel(level),
                parallelResult.getDetailCoeffsAtLevel(level),
                EPSILON,
                "Detail coefficients at level " + level + " should match for DB4"
            );
        }
    }
    
    @Test
    void testParallelWithZeroPadding() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        Haar wavelet = new Haar();
        BoundaryMode mode = BoundaryMode.ZERO_PADDING;
        int levels = 2;
        
        // Sequential transform
        sequentialTransform = new MultiLevelMODWTTransform(wavelet, mode);
        MultiLevelMODWTResult sequentialResult = sequentialTransform.decompose(signal, levels);
        
        // Parallel transform
        MultiLevelMODWTResult parallelResult = parallelTransform.decompose(signal, wavelet, mode, levels);
        
        // Compare results
        assertArrayEquals(
            sequentialResult.getApproximationCoeffs(),
            parallelResult.getApproximationCoeffs(),
            EPSILON,
            "Approximation coefficients should match with zero padding"
        );
        
        for (int level = 1; level <= levels; level++) {
            assertArrayEquals(
                sequentialResult.getDetailCoeffsAtLevel(level),
                parallelResult.getDetailCoeffsAtLevel(level),
                EPSILON,
                "Detail coefficients at level " + level + " should match with zero padding"
            );
        }
    }
    
    @Test
    @org.junit.jupiter.api.Disabled("Performance test - results vary by environment")
    void testParallelPerformance() {
        // Use larger signal to better demonstrate parallel benefits
        double[] signal = new double[16384];
        Random random = new Random(42);
        for (int i = 0; i < signal.length; i++) {
            signal[i] = random.nextGaussian();
        }
        
        var wavelet = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.PERIODIC;
        int levels = 6;
        
        // Create transforms once
        sequentialTransform = new MultiLevelMODWTTransform(wavelet, mode);
        
        // Warm up
        for (int i = 0; i < 20; i++) {
            sequentialTransform.decompose(signal, levels);
            parallelTransform.decompose(signal, wavelet, mode, levels);
        }
        
        // Time sequential
        long sequentialStart = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            sequentialTransform.decompose(signal, levels);
        }
        long sequentialTime = System.nanoTime() - sequentialStart;
        
        // Time parallel
        long parallelStart = System.nanoTime();
        for (int i = 0; i < 20; i++) {
            parallelTransform.decompose(signal, wavelet, mode, levels);
        }
        long parallelTime = System.nanoTime() - parallelStart;
        
        double speedup = (double) sequentialTime / parallelTime;
        System.out.printf("Sequential: %.2f ms, Parallel: %.2f ms, Speedup: %.2fx%n",
            sequentialTime / 1e6, parallelTime / 1e6, speedup);
        
        // Parallel should be at least as fast (allowing some margin for overhead)
        // For smaller signals (4096), parallel overhead might make it slightly slower
        assertTrue(speedup > 0.7, 
            String.format("Parallel should not be significantly slower than sequential. Speedup: %.2fx", speedup));
    }
    
    @Test
    void testCustomExecutor() {
        // Test with custom executor
        ForkJoinPool customPool = new ForkJoinPool(2);
        try {
            ParallelMultiLevelMODWT customTransform = new ParallelMultiLevelMODWT(customPool);
            
            double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
            Haar wavelet = new Haar();
            
            MultiLevelMODWTResult result = customTransform.decompose(
                signal, wavelet, BoundaryMode.PERIODIC, 2);
            
            assertNotNull(result);
            assertEquals(2, result.getLevels());
            assertEquals(signal.length, result.getSignalLength());
            
        } finally {
            customPool.shutdown();
        }
    }

    @Test
    void testEdgeCases() {
        ParallelMultiLevelMODWT transform = new ParallelMultiLevelMODWT();
        
        // Test single level
        double[] signal = {1, 2, 3, 4};
        MultiLevelMODWTResult result = transform.decompose(
            signal, new Haar(), BoundaryMode.PERIODIC, 1);
        
        assertNotNull(result);
        assertEquals(1, result.getLevels());
        
        // Test invalid inputs
        assertThrows(InvalidSignalException.class, () ->
            transform.decompose(new double[0], new Haar(), BoundaryMode.PERIODIC, 1));
        
        assertThrows(InvalidArgumentException.class, () ->
            transform.decompose(signal, new Haar(), BoundaryMode.PERIODIC, 0));
        
        assertThrows(InvalidArgumentException.class, () ->
            transform.decompose(signal, new Haar(), BoundaryMode.PERIODIC, 10));
    }

    @Test
    void testBuilderAndParallelismConstructor() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        Haar wavelet = new Haar();
        BoundaryMode mode = BoundaryMode.PERIODIC;

        // Builder with explicit parallelism and minParallelSignalLength forces sequential path here
        try (ParallelMultiLevelMODWT transform = new ParallelMultiLevelMODWT.Builder()
                .parallelism(2)
                .minParallelSignalLength(1000)
                .build()) {
            MultiLevelMODWTResult result1 = transform.decompose(signal, wavelet, mode, 2);
            assertNotNull(result1);
            assertEquals(2, result1.getLevels());
        }

        // Convenience constructor with parallelism
        try (ParallelMultiLevelMODWT transform = new ParallelMultiLevelMODWT(2)) {
            MultiLevelMODWTResult result2 = transform.decompose(signal, wavelet, mode, 2);
            assertNotNull(result2);
            assertEquals(signal.length, result2.getSignalLength());
        }
    }
}
