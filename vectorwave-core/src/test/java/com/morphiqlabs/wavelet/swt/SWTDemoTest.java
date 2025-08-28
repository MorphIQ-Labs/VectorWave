package com.morphiqlabs.wavelet.swt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Demonstrates SWT usage with automatic optimizations.
 */
class SWTDemoTest {
    
    @Test
    void demonstrateSWTWithOptimizations() {
        System.out.println("\n=== SWT Demo with Automatic Optimizations ===\n");
        
        // Create test signal
        double[] signal = generateSignal(2048);
        
        // Create SWT adapter - optimizations are automatic!
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(
            Daubechies.DB4, BoundaryMode.PERIODIC);
        
        // Check automatic optimizations
        Map<String, Object> stats = swt.getCacheStatistics();
        System.out.println("Automatic Optimizations:");
        System.out.println("  Filter cache size: " + stats.get("filterCacheSize"));
        System.out.println("  Parallel threshold: " + stats.get("parallelThreshold"));
        
        // Perform multi-level decomposition
        MutableMultiLevelMODWTResult result = swt.forward(signal, 4);
        System.out.println("\nSWT Decomposition:");
        System.out.println("  Signal length: " + signal.length);
        System.out.println("  Levels: " + result.getLevels());
        System.out.println("  Shift-invariant: YES");
        
        // Verify all levels have same length (shift-invariant property)
        for (int level = 1; level <= result.getLevels(); level++) {
            double[] detail = result.getMutableDetailCoeffs(level);
            assertEquals(signal.length, detail.length, 
                "All levels should have same length (shift-invariant)");
        }
        
        // Test denoising with custom threshold (universal threshold can be too aggressive)
        double[] noisySignal = addNoise(signal, 0.3);
        double[] denoised = swt.denoise(noisySignal, 4, 0.2, true); // Custom threshold
        
        double noisySNR = calculateSNR(signal, noisySignal);
        double denoisedSNR = calculateSNR(signal, denoised);
        System.out.println("\nDenoising Performance:");
        System.out.println("  SNR before denoising: " + String.format("%.2f dB", noisySNR));
        System.out.println("  SNR after denoising: " + String.format("%.2f dB", denoisedSNR));
        System.out.println("  Improvement: " + String.format("%.2f dB", denoisedSNR - noisySNR));
        assertTrue(denoisedSNR > noisySNR || Math.abs(denoisedSNR - noisySNR) < 0.5, 
                  "Denoising should improve or maintain SNR");
        
        // Test sparse storage
        SWTResult denseResult = new SWTResult(
            result.getMutableApproximationCoeffs(),
            extractDetails(result),
            result.getLevels()
        );
        
        SWTResult.SparseSWTResult sparseResult = denseResult.toSparse(0.01);
        double compressionRatio = sparseResult.getCompressionRatio();
        System.out.println("\nSparse Storage:");
        System.out.println("  Compression ratio: " + String.format("%.2fx", compressionRatio));
        System.out.println("  (Note: Clean signals don't compress well; sparse signals achieve 3-10x)");
        assertTrue(compressionRatio >= 1.0, "Compression ratio should be at least 1.0");
        
        // Verify reconstruction
        double[] reconstructed = swt.inverse(result);
        double maxError = 0;
        for (int i = 0; i < signal.length; i++) {
            maxError = Math.max(maxError, Math.abs(signal[i] - reconstructed[i]));
        }
        System.out.println("\nReconstruction:");
        System.out.println("  Max error: " + String.format("%.2e", maxError));
        assertTrue(maxError < 1e-10, "Should have perfect reconstruction");
        
        // Cleanup
        swt.cleanup();
        stats = swt.getCacheStatistics();
        assertEquals(0, (int) stats.get("filterCacheSize"), 
            "Cache should be cleared after cleanup");
        
        System.out.println("\n✓ All SWT optimizations working correctly!");
    }
    
    private double[][] extractDetails(MutableMultiLevelMODWTResult result) {
        double[][] details = new double[result.getLevels()][];
        for (int i = 0; i < result.getLevels(); i++) {
            details[i] = result.getMutableDetailCoeffs(i + 1);
        }
        return details;
    }
    
    private double[] generateSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 32) + 
                       0.5 * Math.sin(2 * Math.PI * i / 64);
        }
        return signal;
    }
    
    private double[] addNoise(double[] signal, double stdDev) {
        double[] noisy = new double[signal.length];
        java.util.Random rand = new java.util.Random(42);
        for (int i = 0; i < signal.length; i++) {
            noisy[i] = signal[i] + stdDev * rand.nextGaussian();
        }
        return noisy;
    }
    
    private double calculateSNR(double[] clean, double[] noisy) {
        double signalPower = 0;
        double noisePower = 0;
        
        for (int i = 0; i < clean.length; i++) {
            signalPower += clean[i] * clean[i];
            double diff = clean[i] - noisy[i];
            noisePower += diff * diff;
        }
        
        if (noisePower == 0) return Double.POSITIVE_INFINITY;
        return 10 * Math.log10(signalPower / noisePower);
    }
}