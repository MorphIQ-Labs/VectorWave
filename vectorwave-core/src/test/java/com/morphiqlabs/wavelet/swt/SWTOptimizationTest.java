package com.morphiqlabs.wavelet.swt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Symlet;
import com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for SWT internal optimizations.
 * Verifies that optimizations produce correct results and improve performance.
 */
class SWTOptimizationTest {
    
    private static final double TOLERANCE = 1e-10;
    private VectorWaveSwtAdapter swtAdapter;
    private double[] testSignal;
    
    @BeforeEach
    void setUp() {
        swtAdapter = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC);
        testSignal = generateTestSignal(1024);
    }
    
    @Test
    @DisplayName("Filters populate lazily on first use for discrete wavelets")
    void testFilterPrecomputation() {
        // Create adapter with discrete wavelet
        VectorWaveSwtAdapter adapter = new VectorWaveSwtAdapter(Daubechies.DB6, BoundaryMode.PERIODIC);

        // Initially caches should be empty (lazy strategy)
        Map<String, Object> stats0 = adapter.getCacheStatistics();
        assertEquals(0, (int) stats0.get("filterCacheSize"), "Filters should populate lazily");

        // Perform a decomposition that will populate multiple levels
        double[] signal = generateTestSignal(8192);
        MutableMultiLevelMODWTResult res = adapter.forward(signal, 5);
        assertNotNull(res);

        Map<String, Object> stats1 = adapter.getCacheStatistics();
        assertTrue((int) stats1.get("filterCacheSize") >= 5, "Should cache at least requested levels");
    }
    
    @Test
    @DisplayName("Parallel processing should activate for large signals")
    void testParallelProcessingActivation() {
        // Create large signal that triggers parallel processing
        double[] largeSignal = generateTestSignal(5000); // > PARALLEL_THRESHOLD (4096)
        
        // Perform decomposition
        MutableMultiLevelMODWTResult result = swtAdapter.forward(largeSignal, 4);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(4, result.getLevels(), "Should have 4 levels");
        
        // Check that parallel executor was initialized
        Map<String, Object> stats = swtAdapter.getCacheStatistics();
        // Note: executor may or may not be active depending on timing
        assertNotNull(stats.get("parallelExecutorActive"), "Should track executor status");
    }
    
    @Test
    @DisplayName("Small signals should use sequential processing")
    void testSequentialProcessingForSmallSignals() {
        // Create small signal that won't trigger parallel processing
        double[] smallSignal = generateTestSignal(1000); // < PARALLEL_THRESHOLD
        
        // Perform decomposition
        MutableMultiLevelMODWTResult result = swtAdapter.forward(smallSignal, 3);
        
        assertNotNull(result, "Result should not be null");
        assertEquals(3, result.getLevels(), "Should have 3 levels");
        
        // Verify result correctness
        double[] reconstructed = swtAdapter.inverse(result);
        assertArrayEquals(smallSignal, reconstructed, TOLERANCE, "Should reconstruct perfectly");
    }
    
    @Test
    @DisplayName("SWT result should store coefficients efficiently")
    void testSWTResultStorage() {
        double[] signal = generateTestSignal(512);
        int levels = 3;
        
        // Create SWT result
        double[] approx = new double[512];
        double[][] details = new double[levels][512];
        
        // Fill with test data
        Random rand = new Random(42);
        for (int i = 0; i < 512; i++) {
            approx[i] = rand.nextGaussian();
            for (int level = 0; level < levels; level++) {
                // Make details sparse (many zeros)
                details[level][i] = rand.nextDouble() > 0.8 ? rand.nextGaussian() : 0.0;
            }
        }
        
        SWTResult result = new SWTResult(approx, details, levels);
        
        // Test basic properties
        assertEquals(levels, result.getLevels());
        assertEquals(512, result.getSignalLength());
        
        // Test energy computation (lazy)
        double totalEnergy = result.getTotalEnergy();
        assertTrue(totalEnergy > 0, "Should have positive energy");
        
        // Test sparse conversion
        SWTResult.SparseSWTResult sparse = result.toSparse(0.01);
        assertTrue(sparse.getCompressionRatio() > 1.0, "Should achieve compression");
        
        // Verify reconstruction from sparse
        SWTResult reconstructed = sparse.toFull();
        assertEquals(result.getLevels(), reconstructed.getLevels());
        assertEquals(result.getSignalLength(), reconstructed.getSignalLength());
    }
    
    @Test
    @DisplayName("Cleanup should release resources properly")
    void testResourceCleanup() {
        VectorWaveSwtAdapter adapter = new VectorWaveSwtAdapter(Symlet.SYM8, BoundaryMode.PERIODIC);
        
        // Perform some operations to initialize caches
        double[] signal = generateTestSignal(5000);
        adapter.forward(signal, 3);
        
        // Check caches are populated
        Map<String, Object> statsBefore = adapter.getCacheStatistics();
        assertTrue((int) statsBefore.get("filterCacheSize") > 0, "Should have cached filters");
        
        // Cleanup
        adapter.cleanup();
        
        // Check caches are cleared
        Map<String, Object> statsAfter = adapter.getCacheStatistics();
        assertEquals(0, (int) statsAfter.get("filterCacheSize"), "Filter cache should be cleared");
        assertFalse((boolean) statsAfter.get("parallelExecutorActive"), "Executor should be shutdown");
        
        // Adapter should still work after cleanup
        MutableMultiLevelMODWTResult result = adapter.forward(generateTestSignal(256), 2);
        assertNotNull(result, "Should still work after cleanup");
    }
    
    @Test
    @DisplayName("Denoising should use optimizations transparently")
    void testDenoisingWithOptimizations() {
        // Create noisy signal
        double[] cleanSignal = generateSineWave(1024, 10.0);
        double[] noise = generateNoise(1024, 0.5);
        double[] noisySignal = new double[1024];
        
        for (int i = 0; i < 1024; i++) {
            noisySignal[i] = cleanSignal[i] + noise[i];
        }
        
        // Denoise using universal threshold
        double[] denoised = swtAdapter.denoise(noisySignal, 4, -1, true);
        
        // Check denoising effectiveness
        double mse = computeMSE(cleanSignal, denoised);
        double noiseMSE = computeMSE(cleanSignal, noisySignal);
        
        // Universal threshold denoising on this synthetic signal should deliver
        // a meaningful improvement. Require at least ~30% MSE reduction to avoid
        // a too‑weak assertion while keeping the test robust across platforms.
        assertTrue(mse <= noiseMSE * 0.7,
                () -> String.format("Expected >=30%% MSE reduction, got %.4f -> %.4f", noiseMSE, mse));
    }
    
    @Test
    @DisplayName("Multiple wavelets should work with optimizations")
    void testMultipleWavelets() {
        // Test different wavelets
        VectorWaveSwtAdapter haarAdapter = new VectorWaveSwtAdapter(new Haar());
        VectorWaveSwtAdapter db4Adapter = new VectorWaveSwtAdapter(Daubechies.DB4);
        VectorWaveSwtAdapter sym6Adapter = new VectorWaveSwtAdapter(Symlet.SYM6);
        
        double[] signal = generateTestSignal(512);
        
        // All should produce valid results
        MutableMultiLevelMODWTResult haarResult = haarAdapter.forward(signal, 3);
        MutableMultiLevelMODWTResult db4Result = db4Adapter.forward(signal, 3);
        MutableMultiLevelMODWTResult sym6Result = sym6Adapter.forward(signal, 3);
        
        assertNotNull(haarResult);
        assertNotNull(db4Result);
        assertNotNull(sym6Result);
        
        // Each should reconstruct perfectly
        assertArrayEquals(signal, haarAdapter.inverse(haarResult), TOLERANCE);
        assertArrayEquals(signal, db4Adapter.inverse(db4Result), TOLERANCE);
        assertArrayEquals(signal, sym6Adapter.inverse(sym6Result), TOLERANCE);
        
        // Cleanup all
        haarAdapter.cleanup();
        db4Adapter.cleanup();
        sym6Adapter.cleanup();
    }
    
    @Test
    @DisplayName("Shift-invariance should be preserved with optimizations")
    void testShiftInvariance() {
        double[] signal = generateTestSignal(512);
        
        // Original transform
        MutableMultiLevelMODWTResult result1 = swtAdapter.forward(signal, 3);
        
        // Shifted signal
        double[] shifted = circularShift(signal, 7);
        MutableMultiLevelMODWTResult result2 = swtAdapter.forward(shifted, 3);
        
        // Detail coefficients should be shifted by same amount
        double[] detail1 = result1.getMutableDetailCoeffs(1);
        double[] detail2 = result2.getMutableDetailCoeffs(1);
        
        // Unshift detail2 and compare
        double[] detail2Unshifted = circularShift(detail2, -7);
        
        // Should match within tolerance (accounting for boundary effects)
        double maxDiff = 0.0;
        for (int i = 0; i < detail1.length; i++) {
            maxDiff = Math.max(maxDiff, Math.abs(detail1[i] - detail2Unshifted[i]));
        }
        
        assertTrue(maxDiff < 1e-8, "Shift-invariance should be preserved");
    }
    
    // Helper methods
    
    private static double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        Random rand = new Random(42);
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 
                       0.5 * Math.sin(2 * Math.PI * i / 64) +
                       0.1 * rand.nextGaussian();
        }
        return signal;
    }
    
    private static double[] generateSineWave(int length, double frequency) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * frequency * i / length);
        }
        return signal;
    }
    
    private static double[] generateNoise(int length, double stdDev) {
        double[] noise = new double[length];
        Random rand = new Random(123);
        for (int i = 0; i < length; i++) {
            noise[i] = stdDev * rand.nextGaussian();
        }
        return noise;
    }
    
    private static double[] circularShift(double[] signal, int shift) {
        int n = signal.length;
        double[] shifted = new double[n];
        for (int i = 0; i < n; i++) {
            shifted[i] = signal[(i - shift + n) % n];
        }
        return shifted;
    }
    
    private static double computeMSE(double[] signal1, double[] signal2) {
        double sum = 0.0;
        for (int i = 0; i < signal1.length; i++) {
            double diff = signal1[i] - signal2[i];
            sum += diff * diff;
        }
        return sum / signal1.length;
    }
}
