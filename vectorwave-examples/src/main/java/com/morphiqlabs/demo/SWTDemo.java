package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.swt.VectorWaveSwtAdapter;
import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResult;

import java.util.Arrays;

/**
 * Demonstration of Stationary Wavelet Transform (SWT) capabilities in VectorWave.
 * 
 * This demo shows how to use the SWT adapter for signal denoising and analysis,
 * leveraging the mutable coefficient functionality for custom processing.
 */
public class SWTDemo {
    
    public static void main(String[] args) {
        System.out.println("=====================================");
        System.out.println("   VectorWave SWT Demonstration");
        System.out.println("=====================================\n");
        
        // Create test signal with noise
        double[] cleanSignal = createTestSignal(256);
        double[] noisySignal = addNoise(cleanSignal, 0.2);
        
        System.out.printf("Signal length: %d\n", cleanSignal.length);
        System.out.printf("Noise level: 0.2 (SNR: %.2f dB)\n\n", 
            calculateSNR(cleanSignal, noisySignal));
        
        // Demonstrate different SWT applications
        demonstrateBasicSWT(noisySignal);
        demonstrateDenoising(cleanSignal, noisySignal);
        demonstrateLevelExtraction(cleanSignal);
        demonstrateCustomThresholding(noisySignal);
    }
    
    private static void demonstrateBasicSWT(double[] signal) {
        System.out.println("1. Basic SWT Decomposition and Reconstruction");
        System.out.println("----------------------------------------------");
        
        try (VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(
            Daubechies.DB4, BoundaryMode.PERIODIC)) {
            // Forward transform
            MutableMultiLevelMODWTResult result = swt.forward(signal, 4);
        
        System.out.printf("Decomposition levels: %d\n", result.getLevels());
        System.out.println("Energy distribution:");
        
        double[] energyDist = result.getRelativeEnergyDistribution();
        for (int i = 0; i < energyDist.length - 1; i++) {
            System.out.printf("  Level %d: %.2f%%\n", i + 1, energyDist[i] * 100);
        }
        System.out.printf("  Approximation: %.2f%%\n", 
            energyDist[energyDist.length - 1] * 100);
        
            // Inverse transform
            double[] reconstructed = swt.inverse(result);
            double error = calculateRMSE(signal, reconstructed);
            System.out.printf("Reconstruction error: %.2e\n\n", error);
        }
    }
    
    private static void demonstrateDenoising(double[] clean, double[] noisy) {
        System.out.println("2. Signal Denoising with SWT");
        System.out.println("-----------------------------");
        
        try (VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Symlet.SYM8)) {
        // Method 1: Automatic universal threshold
        double[] denoised1 = swt.denoise(noisy, 4);
        double snr1 = calculateSNR(clean, denoised1);
        System.out.printf("Universal threshold: SNR = %.2f dB\n", snr1);
        
        // Method 2: Custom threshold
        double[] denoised2 = swt.denoise(noisy, 4, 0.3, true);
        double snr2 = calculateSNR(clean, denoised2);
        System.out.printf("Custom threshold (0.3): SNR = %.2f dB\n", snr2);
        
        // Method 3: Level-dependent thresholding
        MutableMultiLevelMODWTResult result = swt.forward(noisy, 4);
        swt.applyThreshold(result, 1, 0.4, true);  // High for fine details
        swt.applyThreshold(result, 2, 0.3, true);
        swt.applyThreshold(result, 3, 0.2, true);
        swt.applyThreshold(result, 4, 0.1, true);  // Low for coarse details
        double[] denoised3 = swt.inverse(result);
        double snr3 = calculateSNR(clean, denoised3);
        System.out.printf("Level-dependent: SNR = %.2f dB\n\n", snr3);
        }
    }
    
    private static void demonstrateLevelExtraction(double[] signal) {
        System.out.println("3. Multi-Resolution Feature Extraction");
        System.out.println("---------------------------------------");
        
        try (VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB6)) {
        // Extract different frequency bands
        double[] highFreq = swt.extractLevel(signal, 4, 1);  // Finest details
        double[] midFreq = swt.extractLevel(signal, 4, 2);   // Mid frequency
        double[] lowFreq = swt.extractLevel(signal, 4, 0);   // Approximation
        
        System.out.println("Extracted component energies:");
        System.out.printf("  High frequency (Level 1): %.4f\n", computeEnergy(highFreq));
        System.out.printf("  Mid frequency (Level 2): %.4f\n", computeEnergy(midFreq));
        System.out.printf("  Low frequency (Approx): %.4f\n", computeEnergy(lowFreq));
        System.out.printf("  Original signal: %.4f\n\n", computeEnergy(signal));
        }
    }
    
    private static void demonstrateCustomThresholding(double[] noisy) {
        System.out.println("4. Custom Coefficient Processing");
        System.out.println("---------------------------------");
        
        try (VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Coiflet.COIF3)) {
        MutableMultiLevelMODWTResult result = swt.forward(noisy, 3);
        
        // Custom processing: Keep only significant coefficients
        int totalCoeffs = 0;
        int keptCoeffs = 0;
        
        for (int level = 1; level <= 3; level++) {
            double[] coeffs = result.getMutableDetailCoeffs(level);
            double threshold = computeAdaptiveThreshold(coeffs);
            
            for (int i = 0; i < coeffs.length; i++) {
                totalCoeffs++;
                if (Math.abs(coeffs[i]) < threshold) {
                    coeffs[i] = 0;
                } else {
                    keptCoeffs++;
                }
            }
        }
        
        result.clearCaches();
        double compressionRatio = (double) keptCoeffs / totalCoeffs;
        System.out.printf("Compression ratio: %.1f%% coefficients kept\n", 
            compressionRatio * 100);
        
        // Reconstruct sparse representation
        double[] sparse = swt.inverse(result);
        double sparsity = countZeros(result) / (double) (noisy.length * 4);
        System.out.printf("Sparsity achieved: %.1f%% zeros\n\n", sparsity * 100);
        }
    }
    
    // Helper methods
    
    private static double[] createTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            // Combination of smooth and oscillatory components
            signal[i] = Math.sin(2 * Math.PI * i / 32.0) + 
                       0.5 * Math.cos(8 * Math.PI * i / 32.0) +
                       0.2 * Math.sin(16 * Math.PI * i / 32.0);
            
            // Add a discontinuity
            if (i > length / 2 && i < length * 3 / 4) {
                signal[i] += 1.0;
            }
        }
        return signal;
    }
    
    private static double[] addNoise(double[] signal, double noiseLevel) {
        java.util.Random rng = new java.util.Random(42);
        double[] noisy = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            noisy[i] = signal[i] + noiseLevel * rng.nextGaussian();
        }
        return noisy;
    }
    
    private static double calculateSNR(double[] signal, double[] noisy) {
        double signalPower = 0;
        double noisePower = 0;
        for (int i = 0; i < signal.length; i++) {
            signalPower += signal[i] * signal[i];
            double noise = noisy[i] - signal[i];
            noisePower += noise * noise;
        }
        return 10 * Math.log10(signalPower / noisePower);
    }
    
    private static double calculateRMSE(double[] a, double[] b) {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            double diff = a[i] - b[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum / a.length);
    }
    
    private static double computeEnergy(double[] signal) {
        double energy = 0;
        for (double val : signal) {
            energy += val * val;
        }
        return energy;
    }
    
    private static double computeAdaptiveThreshold(double[] coeffs) {
        // Simple adaptive threshold based on coefficient statistics
        double sum = 0;
        for (double c : coeffs) {
            sum += Math.abs(c);
        }
        double mean = sum / coeffs.length;
        return mean * 2.5; // Threshold at 2.5 times mean absolute value
    }
    
    private static int countZeros(MutableMultiLevelMODWTResult result) {
        int zeros = 0;
        for (int level = 1; level <= result.getLevels(); level++) {
            double[] coeffs = result.getDetailCoeffsAtLevel(level);
            for (double c : coeffs) {
                if (c == 0.0) zeros++;
            }
        }
        return zeros;
    }
}
