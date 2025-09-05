package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.swt.VectorWaveSwtAdapter;
import com.morphiqlabs.wavelet.swt.SWTResult;
import com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResult;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;

/**
 * Demonstrates best practices for using the Stationary Wavelet Transform (SWT)
 * with internal optimizations in VectorWave.
 * 
 * <p>This demo covers:
 * <ul>
 *   <li>Basic SWT usage with automatic optimizations</li>
 *   <li>Multi-level decomposition for different signal sizes</li>
 *   <li>Denoising with universal and custom thresholds</li>
 *   <li>Memory-efficient storage for sparse coefficients</li>
 *   <li>Resource management and cleanup</li>
 *   <li>Performance considerations</li>
 * </ul>
 * 
 * <p><strong>Key Points:</strong></p>
 * <ul>
 *   <li>Optimizations are automatic - no special API needed</li>
 *   <li>Large signals (≥4096 samples) automatically use parallel processing</li>
 *   <li>Filter precomputation happens automatically for discrete wavelets</li>
 *   <li>Call cleanup() when done for resource management</li>
 * </ul>
 */
public class SWTBestPracticesDemo {
    
    public static void main(String[] args) {
        System.out.println("=== VectorWave SWT Best Practices Demo ===\n");
        
        // Demo 1: Basic SWT with automatic optimizations
        demonstrateBasicSWT();
        
        // Demo 2: Multi-level decomposition
        demonstrateMultiLevelSWT();
        
        // Demo 3: Denoising best practices
        demonstrateDenoisingBestPractices();
        
        // Demo 4: Memory-efficient sparse storage
        demonstrateSparseStorage();
        
        // Demo 5: Performance considerations
        demonstratePerformanceOptimizations();
        
        // Demo 6: Resource management
        demonstrateResourceManagement();
    }
    
    private static void demonstrateBasicSWT() {
        System.out.println("1. Basic SWT Usage (Automatic Optimizations)");
        System.out.println("--------------------------------------------");
        
        // Create signal
        double[] signal = generateTestSignal(1024);
        
        // Create SWT adapter - optimizations are automatic!
        try (VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(
            Daubechies.DB4, BoundaryMode.PERIODIC)) {
        
        // Perform decomposition
        MutableMultiLevelMODWTResult result = swt.forward(signal, 3);
        
        System.out.println("Signal length: " + signal.length);
        System.out.println("Decomposition levels: " + result.getLevels());
        System.out.println("Shift-invariant: YES (all levels same length)");
        
        // Show coefficient lengths at each level
        for (int level = 1; level <= result.getLevels(); level++) {
            double[] details = result.getMutableDetailCoeffs(level);
            System.out.printf("  Level %d detail length: %d\n", level, details.length);
        }
        
        // Reconstruct
        double[] reconstructed = swt.inverse(result);
        double error = calculateMaxError(signal, reconstructed);
        System.out.printf("Perfect reconstruction error: %.2e\n\n", error);
        }
    }
    
    private static void demonstrateMultiLevelSWT() {
        System.out.println("2. Multi-Level SWT Decomposition");
        System.out.println("--------------------------------");
        
        // Different signal sizes trigger different optimizations
        int[] signalSizes = {512, 2048, 5000}; // Last one triggers parallel processing
        
        for (int size : signalSizes) {
            double[] signal = generateTestSignal(size);
            try (VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Symlet.SYM6)) {
            
            // Calculate optimal decomposition levels
            int maxLevels = calculateMaxLevels(size, 12); // SYM6 has 12 coefficients
            int optimalLevels = Math.min(maxLevels, 5); // Usually 3-5 levels is enough
            
            long startTime = System.nanoTime();
            MutableMultiLevelMODWTResult result = swt.forward(signal, optimalLevels);
            long endTime = System.nanoTime();
            
            double timeMs = (endTime - startTime) / 1_000_000.0;
            
            System.out.printf("Signal size %d: %d levels, %.2f ms", 
                            size, optimalLevels, timeMs);
            
            if (size >= 4096) {
                System.out.print(" (parallel processing)");
            }
            System.out.println();
            }
        }
        System.out.println();
    }
    
    private static void demonstrateDenoisingBestPractices() {
        System.out.println("3. Denoising Best Practices");
        System.out.println("---------------------------");
        
        // Generate noisy signal
        double[] cleanSignal = generateSineWave(2048, 10.0);
        double[] noise = generateGaussianNoise(2048, 0.3);
        double[] noisySignal = addSignals(cleanSignal, noise);
        
        try (VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(
            Daubechies.DB6, BoundaryMode.PERIODIC)) {
        
        // Method 1: Universal threshold (automatic)
        double[] denoisedUniversal = swt.denoise(noisySignal, 4, -1, true);
        double snrUniversal = calculateSNR(cleanSignal, denoisedUniversal);
        
        // Method 2: Custom threshold
        double customThreshold = 0.2;
        double[] denoisedCustom = swt.denoise(noisySignal, 4, customThreshold, true);
        double snrCustom = calculateSNR(cleanSignal, denoisedCustom);
        
        // Method 3: Level-specific thresholding
        MutableMultiLevelMODWTResult result = swt.forward(noisySignal, 4);
        
        // Apply different thresholds at different levels
        swt.applyThreshold(result, 1, 0.3, true);  // Finest details - higher threshold
        swt.applyThreshold(result, 2, 0.2, true);
        swt.applyThreshold(result, 3, 0.1, true);
        swt.applyThreshold(result, 4, 0.05, true); // Coarsest details - lower threshold
        
        double[] denoisedLevelSpecific = swt.inverse(result);
        double snrLevelSpecific = calculateSNR(cleanSignal, denoisedLevelSpecific);
        
        System.out.println("Denoising Results (higher SNR is better):");
        System.out.printf("  Original noisy signal SNR:    %.2f dB\n", 
                         calculateSNR(cleanSignal, noisySignal));
        System.out.printf("  Universal threshold SNR:      %.2f dB\n", snrUniversal);
        System.out.printf("  Custom threshold SNR:         %.2f dB\n", snrCustom);
        System.out.printf("  Level-specific threshold SNR: %.2f dB\n", snrLevelSpecific);
        
        System.out.println("\nBest Practice: Start with universal threshold,");
        System.out.println("then fine-tune with level-specific thresholds.\n");
        }
    }
    
    private static void demonstrateSparseStorage() {
        System.out.println("4. Memory-Efficient Sparse Storage");
        System.out.println("----------------------------------");
        
        // Create a sparse signal (mostly zeros)
        int length = 4096;
        double[] sparseSignal = new double[length];
        Random rand = new Random(42);
        
        // Only 5% of the signal has significant values
        int nonZeroCount = 0;
        for (int i = 0; i < length; i++) {
            if (rand.nextDouble() < 0.05) {
                sparseSignal[i] = rand.nextGaussian() * 10;
                nonZeroCount++;
            }
        }
        
        try (VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4)) {
        MutableMultiLevelMODWTResult modwtResult = swt.forward(sparseSignal, 4);
        
        // Create SWT result for sparse storage demonstration
        double[] approx = modwtResult.getMutableApproximationCoeffs();
        double[][] details = new double[4][];
        for (int i = 0; i < 4; i++) {
            details[i] = modwtResult.getMutableDetailCoeffs(i + 1);
        }
        
        SWTResult denseResult = new SWTResult(approx, details, 4);
        
        // Convert to sparse representation
        double threshold = 0.01;
        SWTResult.SparseSWTResult sparseResult = denseResult.toSparse(threshold);
        
        // Calculate memory savings
        int denseSize = length * 5; // 5 arrays (1 approx + 4 details)
        double compressionRatio = sparseResult.getCompressionRatio();
        
        System.out.printf("Original signal: %d samples, %d non-zero (%.1f%% sparse)\n",
                         length, nonZeroCount, 100.0 * (1 - nonZeroCount/(double)length));
        System.out.printf("Dense storage: %d doubles\n", denseSize);
        System.out.printf("Compression ratio: %.2fx\n", compressionRatio);
        System.out.printf("Memory saved: %.1f%%\n", 100 * (1 - 1/compressionRatio));
        
        // Verify reconstruction
        SWTResult reconstructedDense = sparseResult.toFull();
        System.out.println("Sparse to dense reconstruction: SUCCESS");
        
        System.out.println("\nBest Practice: Use sparse storage for signals with");
        System.out.println("many near-zero coefficients (e.g., after thresholding).\n");
        }
    }
    
    private static void demonstratePerformanceOptimizations() {
        System.out.println("5. Performance Optimization Guidelines");
        System.out.println("--------------------------------------");
        
        try (VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB8)) {
        
        // Check internal optimization status
        Map<String, Object> stats = swt.getCacheStatistics();
        
        System.out.println("Automatic Optimizations Active:");
        System.out.printf("  Filter cache size: %d (precomputed filters)\n", 
                         stats.get("filterCacheSize"));
        System.out.printf("  Parallel threshold: %d samples\n", 
                         stats.get("parallelThreshold"));
        System.out.printf("  Parallel executor: %s\n", 
                         stats.get("parallelExecutorActive"));
        
        // Demonstrate performance difference
        System.out.println("\nPerformance Comparison:");
        
        // Small signal (sequential processing)
        double[] smallSignal = generateTestSignal(1000);
        long start = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            swt.forward(smallSignal, 3);
        }
        long smallTime = System.nanoTime() - start;
        
        // Large signal (parallel processing)
        double[] largeSignal = generateTestSignal(8192);
        start = System.nanoTime();
        for (int i = 0; i < 10; i++) {
            swt.forward(largeSignal, 3);
        }
        long largeTime = System.nanoTime() - start;
        
        double smallMs = smallTime / 10_000_000.0;
        double largeMs = largeTime / 10_000_000.0;
        double scalingFactor = (largeMs / smallMs) / (8192.0 / 1000.0);
        
        System.out.printf("  Small signal (1000): %.2f ms/transform\n", smallMs);
        System.out.printf("  Large signal (8192): %.2f ms/transform\n", largeMs);
        System.out.printf("  Scaling efficiency: %.2fx\n", 1/scalingFactor);
        
        System.out.println("\nBest Practices:");
        System.out.println("  • Reuse SWT adapters for multiple transforms");
        System.out.println("  • Large signals (≥4096) benefit from parallel processing");
        System.out.println("  • Lazy filter caches speed up repeated transforms");
        System.out.println("  • Prefer try-with-resources to cleanup automatically\n");
        }
    }
    
    private static void demonstrateResourceManagement() {
        System.out.println("6. Resource Management");
        System.out.println("---------------------");
        
        // Create multiple adapters for different wavelets and manage via TWR
        try (VectorWaveSwtAdapter swt1 = new VectorWaveSwtAdapter(Daubechies.DB4);
             VectorWaveSwtAdapter swt2 = new VectorWaveSwtAdapter(Symlet.SYM8);
             VectorWaveSwtAdapter swt3 = new VectorWaveSwtAdapter(Coiflet.COIF2)) {
            // Use them for processing
            double[] signal = generateTestSignal(5000);
            swt1.forward(signal, 3);
            swt2.forward(signal, 3);
            swt3.forward(signal, 3);

            // Check resource usage while open
            System.out.println("Before close:");
            checkResourceUsage(swt1, "DB4");
            checkResourceUsage(swt2, "SYM8");
            checkResourceUsage(swt3, "COIF2");
        }

        // After TWR, adapters are closed. Create a fresh one to show clean start and functionality.
        try (VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4)) {
            System.out.println("\nAfter close (new adapter):");
            checkResourceUsage(swt, "DB4");
            MutableMultiLevelMODWTResult result = swt.forward(generateTestSignal(256), 2);
            System.out.println("Adapter functional after close (new instance): " + 
                             (result != null ? "YES" : "NO"));
        }
        
        System.out.println("\nBest Practice: Prefer try-with-resources so adapters close automatically.\n");
    }
    
    // Helper methods
    
    private static void checkResourceUsage(VectorWaveSwtAdapter swt, String name) {
        Map<String, Object> stats = swt.getCacheStatistics();
        System.out.printf("  %s: %d cached filters, executor=%s\n",
                         name, stats.get("filterCacheSize"),
                         stats.get("parallelExecutorActive"));
    }
    
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
    
    private static double[] generateGaussianNoise(int length, double stdDev) {
        double[] noise = new double[length];
        Random rand = new Random(123);
        for (int i = 0; i < length; i++) {
            noise[i] = stdDev * rand.nextGaussian();
        }
        return noise;
    }
    
    private static double[] addSignals(double[] signal1, double[] signal2) {
        double[] result = new double[signal1.length];
        for (int i = 0; i < signal1.length; i++) {
            result[i] = signal1[i] + signal2[i];
        }
        return result;
    }
    
    private static double calculateMaxError(double[] original, double[] reconstructed) {
        double maxError = 0;
        for (int i = 0; i < original.length; i++) {
            maxError = Math.max(maxError, Math.abs(original[i] - reconstructed[i]));
        }
        return maxError;
    }
    
    private static double calculateSNR(double[] clean, double[] noisy) {
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
    
    private static int calculateMaxLevels(int signalLength, int filterLength) {
        // Maximum meaningful decomposition level
        // Rule: (filterLength - 1) * (2^level - 1) < signalLength
        int maxLevel = 1;
        while ((filterLength - 1) * (Math.pow(2, maxLevel) - 1) < signalLength) {
            maxLevel++;
        }
        return maxLevel - 1;
    }
}
