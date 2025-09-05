package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;

/**
 * Demonstration of extended Symlet wavelets (SYM9, SYM11, SYM13-14, SYM16-19).
 * 
 * <p>This demo shows:</p>
 * <ul>
 *   <li>How to access the new extended Symlet wavelets</li>
 *   <li>Comparison of symmetry properties vs Daubechies</li>
 *   <li>Perfect reconstruction using MODWT</li>
 * </ul>
 */
public class ExtendedSymletDemo {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Extended Symlet Wavelets Demonstration");
        System.out.println("========================================\n");
        
        demonstrateAvailableSymlets();
        demonstrateSymmetryComparison();
        demonstratePerfectReconstruction();
    }
    
    /**
     * Show all available Symlet wavelets including the new extended ones.
     */
    private static void demonstrateAvailableSymlets() {
        System.out.println("Available Symlet Wavelets:");
        System.out.println("--------------------------");
        
        // Get all Symlet wavelets from the registry
        var symlets = WaveletRegistry.getSymletWavelets();
        
        for (WaveletName name : symlets) {
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            if (wavelet instanceof OrthogonalWavelet) {
                OrthogonalWavelet ortho = (OrthogonalWavelet) wavelet;
                System.out.printf("  %s: %d vanishing moments, %d coefficients\n",
                    name.getCode(),
                    ortho.vanishingMoments(),
                    ortho.lowPassDecomposition().length);
            }
        }
        
        System.out.println("\nNewly added extended Symlets:");
        int[] newOrders = {9, 11, 13, 14, 16, 17, 18, 19};
        for (int order : newOrders) {
            WaveletName name = WaveletName.valueOf("SYM" + order);
            System.out.println("  - " + name.getDescription());
        }
        System.out.println();
    }
    
    /**
     * Compare symmetry properties of Symlets vs Daubechies.
     */
    private static void demonstrateSymmetryComparison() {
        System.out.println("Symmetry Comparison (Lower is Better):");
        System.out.println("---------------------------------------");
        
        // Compare SYM14 vs DB14, SYM16 vs DB16, SYM18 vs DB18
        int[] orders = {14, 16, 18};
        
        for (int order : orders) {
            // Get Symlet
            WaveletName symName = WaveletName.valueOf("SYM" + order);
            Symlet symlet = (Symlet) WaveletRegistry.getWavelet(symName);
            
            // Get Daubechies
            WaveletName dbName = WaveletName.valueOf("DB" + order);
            Daubechies daubechies = (Daubechies) WaveletRegistry.getWavelet(dbName);
            
            // Measure asymmetry
            double symAsymmetry = measureAsymmetry(symlet.lowPassDecomposition());
            double dbAsymmetry = measureAsymmetry(daubechies.lowPassDecomposition());
            
            System.out.printf("  Order %d:\n", order);
            System.out.printf("    SYM%d asymmetry: %.6f\n", order, symAsymmetry);
            System.out.printf("    DB%d asymmetry:  %.6f\n", order, dbAsymmetry);
            System.out.printf("    Improvement: %.1f%%\n\n", 
                (1 - symAsymmetry/dbAsymmetry) * 100);
        }
    }
    
    /**
     * Demonstrate perfect reconstruction with extended Symlets.
     */
    private static void demonstratePerfectReconstruction() {
        System.out.println("Perfect Reconstruction Test:");
        System.out.println("----------------------------");
        
        // Test with SYM9 and SYM17 (one lower order, one higher order)
        int[] testOrders = {9, 17};
        
        for (int order : testOrders) {
            WaveletName name = WaveletName.valueOf("SYM" + order);
            Symlet symlet = (Symlet) WaveletRegistry.getWavelet(name);
            
            // Create transform
            MODWTTransform transform = new MODWTTransform(symlet, BoundaryMode.PERIODIC);
            
            // Generate test signal
            int length = 256;
            double[] signal = generateTestSignal(length);
            
            // Forward transform
            MODWTResult result = transform.forward(signal);
            
            // Inverse transform
            double[] reconstructed = transform.inverse(result);
            
            // Calculate reconstruction error
            double maxError = 0;
            for (int i = 0; i < length; i++) {
                double error = Math.abs(signal[i] - reconstructed[i]);
                maxError = Math.max(maxError, error);
            }
            
            System.out.printf("  SYM%d:\n", order);
            System.out.printf("    Signal length: %d\n", length);
            System.out.printf("    Max reconstruction error: %.2e\n", maxError);
            System.out.printf("    Status: %s\n\n", 
                maxError < 1e-10 ? "PERFECT" : "FAILED");
        }
    }
    
    /**
     * Generate a test signal with mixed frequencies.
     */
    private static double[] generateTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 64.0) +
                       0.5 * Math.sin(2 * Math.PI * i / 16.0) +
                       0.25 * Math.cos(2 * Math.PI * i / 8.0);
        }
        return signal;
    }
    
    /**
     * Measure asymmetry of a filter by comparing with its time-reversed version.
     */
    private static double measureAsymmetry(double[] filter) {
        int n = filter.length;
        double asymmetry = 0;
        
        for (int i = 0; i < n/2; i++) {
            double diff = filter[i] - filter[n-1-i];
            asymmetry += diff * diff;
        }
        
        return Math.sqrt(asymmetry);
    }
}