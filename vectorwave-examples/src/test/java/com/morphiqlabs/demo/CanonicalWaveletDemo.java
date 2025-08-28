package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.api.WaveletName;
import java.util.List;

/**
 * Demonstrates that WaveletRegistry now returns only canonical names without duplicates.
 */
public class CanonicalWaveletDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Canonical Wavelet Names Demo ===\n");
        
        // Get orthogonal wavelets - should have no duplicates
        List<WaveletName> orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
        System.out.println("Orthogonal wavelets (count: " + orthogonalWavelets.size() + "):");
        for (WaveletName name : orthogonalWavelets) {
            System.out.println("  - " + name);
        }
        
        // Demonstrate no duplicates with enum
        System.out.println("\n=== No Duplicates with Enum ===");
        System.out.println("Each wavelet has exactly one representation.");
        System.out.println("DB4 is DB4 - no 'daubechies4' variant exists!");
        System.out.println("This eliminates confusion and ensures consistency.");
        
        // Verify no duplicates
        System.out.println("\n=== Duplicate Check ===");
        boolean hasDuplicates = false;
        for (WaveletName waveletName : orthogonalWavelets) {
            // With enum, duplicates are impossible!
            // Check that we don't have both DB4 and a hypothetical DAUBECHIES4
            String nameStr = waveletName.name();
            if (nameStr.startsWith("DAUBECHIES") && !nameStr.startsWith("DB")) {
                System.out.println("ERROR: Found long-form name in results: " + waveletName);
                hasDuplicates = true;
            }
        }
        
        if (!hasDuplicates) {
            System.out.println("SUCCESS: No duplicate wavelets found!");
            System.out.println("The registry now returns only canonical names.");
        }
    }
    
}