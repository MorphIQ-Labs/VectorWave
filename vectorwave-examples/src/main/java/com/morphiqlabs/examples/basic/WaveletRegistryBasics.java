package com.morphiqlabs.examples.basic;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MODWTResult;

/**
 * Basic WaveletRegistry usage - the essentials you need to know.
 * 
 * Shows:
 * - Discovering available wavelets
 * - Safe wavelet selection
 * - Basic MODWT transforms
 * 
 * Run: mvn exec:java -Dexec.mainClass="com.morphiqlabs.examples.basic.WaveletRegistryBasics"
 */
public class WaveletRegistryBasics {
    
    public static void main(String[] args) {
        System.out.println("Simple Wavelet Registry Example");
        System.out.println("===============================\n");
        
        // 1. Basic wavelet discovery
        exploreWavelets();
        
        // 2. Safe wavelet selection
        demonstrateSafeSelection();
        
        // 3. Using wavelets with MODWT
        demonstrateBasicTransform();
    }
    
    /**
     * Shows basic wavelet exploration.
     */
    public static void exploreWavelets() {
        System.out.println("1. Exploring Available Wavelets");
        System.out.println("-------------------------------");
        
        // Check how many wavelets are available
        int totalWavelets = WaveletRegistry.getAvailableWavelets().size();
        System.out.println("Total wavelets available: " + totalWavelets);
        
        // Show wavelets by type
        System.out.println("\nWavelets by type:");
        System.out.println("  Orthogonal: " + WaveletRegistry.getOrthogonalWavelets().size());
        System.out.println("  Biorthogonal: " + WaveletRegistry.getBiorthogonalWavelets().size());
        System.out.println("  Continuous: " + WaveletRegistry.getContinuousWavelets().size());
        
        // Show first few orthogonal wavelets
        System.out.println("\nFirst 5 orthogonal wavelets:");
        WaveletRegistry.getOrthogonalWavelets()
            .stream()
            .limit(5)
            .forEach(name -> System.out.println("  - " + name + " (" + name.getCode() + ")"));
        
        System.out.println();
    }
    
    /**
     * Demonstrates safe wavelet selection patterns.
     */
    public static void demonstrateSafeSelection() {
        System.out.println("2. Safe Wavelet Selection");
        System.out.println("-------------------------");
        
        // Always check availability first
        WaveletName[] candidates = {WaveletName.DB4, WaveletName.HAAR, WaveletName.SYM8};
        
        for (WaveletName name : candidates) {
            if (WaveletRegistry.isWaveletAvailable(name)) {
                Wavelet wavelet = WaveletRegistry.getWavelet(name);
                System.out.println("✓ " + name + " - Available (" + wavelet.getType() + ")");
            } else {
                System.out.println("✗ " + name + " - Not available");
            }
        }
        
        // Select with fallback
        Wavelet selectedWavelet = selectWaveletWithFallback(WaveletName.SYM8, WaveletName.DB4, WaveletName.HAAR);
        System.out.println("\nSelected wavelet: " + selectedWavelet.name());
        System.out.println();
    }
    
    /**
     * Shows basic MODWT integration.
     */
    public static void demonstrateBasicTransform() {
        System.out.println("3. Basic MODWT Transform");
        System.out.println("------------------------");
        
        // Create a simple test signal
        double[] signal = createTestSignal(128);
        System.out.println("Created test signal with length: " + signal.length);
        
        // Select a discrete wavelet
        WaveletName waveletName = WaveletName.DB4;
        if (WaveletRegistry.isWaveletAvailable(waveletName)) {
            Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
            
            // Ensure it's a discrete wavelet (needed for MODWT)
            if (wavelet instanceof DiscreteWavelet discreteWavelet) {
                System.out.println("Using wavelet: " + wavelet.name());
                
                // Create MODWT transform
                MODWTTransform transform = new MODWTTransform(discreteWavelet, BoundaryMode.PERIODIC);
                
                // Perform forward transform
                MODWTResult result = transform.forward(signal);
                System.out.println("Transform complete. Coefficients computed.");
                
                // Perform inverse transform
                double[] reconstructed = transform.inverse(result);
                
                // Check reconstruction quality
                double maxError = 0.0;
                for (int i = 0; i < signal.length; i++) {
                    double error = Math.abs(signal[i] - reconstructed[i]);
                    maxError = Math.max(maxError, error);
                }
                
                System.out.printf("Reconstruction max error: %.2e\n", maxError);
                System.out.println("Perfect reconstruction: " + (maxError < 1e-10 ? "✓" : "✗"));
                
            } else {
                System.out.println("Selected wavelet is not discrete - cannot use with MODWT");
            }
        } else {
            System.out.println("Wavelet " + waveletName + " is not available");
        }
    }
    
    /**
     * Selects first available wavelet from candidates.
     */
    private static Wavelet selectWaveletWithFallback(WaveletName... candidates) {
        for (WaveletName candidate : candidates) {
            if (WaveletRegistry.isWaveletAvailable(candidate)) {
                return WaveletRegistry.getWavelet(candidate);
            }
        }
        // If we get here, use the first available orthogonal wavelet
        WaveletName firstOrthogonal = WaveletRegistry.getOrthogonalWavelets().get(0);
        return WaveletRegistry.getWavelet(firstOrthogonal);
    }
    
    /**
     * Creates a simple test signal combining sine waves.
     */
    private static double[] createTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            // Combine multiple frequency components
            signal[i] = Math.sin(2 * Math.PI * i / 16) +     // High frequency
                       0.5 * Math.sin(2 * Math.PI * i / 64) + // Low frequency
                       0.1 * (Math.random() - 0.5);           // Small amount of noise
        }
        return signal;
    }
}