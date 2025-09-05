package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.api.WaveletType;

import java.util.List;
import java.util.Set;

/**
 * Demonstrates the new enum-based WaveletRegistry API.
 * This approach provides type safety, IDE support, and eliminates string typos.
 */
public class EnumBasedWaveletDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Enum-Based Wavelet Registry Demo ===\n");
        
        // Type-safe wavelet access - no string typos possible!
        demonstrateTypeSafeAccess();
        
        // Easy filtering by wavelet family
        demonstrateFamilyFiltering();
        
        // No duplicates with enum approach
        demonstrateNoDuplicates();
        
        // IDE benefits
        demonstrateIDEBenefits();
    }
    
    private static void demonstrateTypeSafeAccess() {
        System.out.println("1. Type-Safe Wavelet Access:");
        System.out.println("-----------------------------");
        
        // Direct enum access - compile-time checked
        Wavelet db4 = WaveletRegistry.getWavelet(WaveletName.DB4);
        System.out.println("  DB4: " + db4.description());
        
        Wavelet haar = WaveletRegistry.getWavelet(WaveletName.HAAR);
        System.out.println("  HAAR: " + haar.description());
        
        Wavelet morlet = WaveletRegistry.getWavelet(WaveletName.MORLET);
        System.out.println("  MORLET: " + morlet.description());
        
        System.out.println();
    }
    
    private static void demonstrateFamilyFiltering() {
        System.out.println("2. Wavelet Family Filtering:");
        System.out.println("-----------------------------");
        
        List<WaveletName> daubechies = WaveletRegistry.getDaubechiesWavelets();
        System.out.println("  Daubechies wavelets: " + daubechies);
        
        List<WaveletName> symlets = WaveletRegistry.getSymletWavelets();
        System.out.println("  Symlet wavelets: " + symlets.size() + " available");
        
        List<WaveletName> coiflets = WaveletRegistry.getCoifletWavelets();
        System.out.println("  Coiflet wavelets: " + coiflets);
        
        System.out.println();
    }
    
    private static void demonstrateNoDuplicates() {
        System.out.println("3. No Duplicates with Enum:");
        System.out.println("-----------------------------");
        
        List<WaveletName> orthogonal = WaveletRegistry.getOrthogonalWavelets();
        System.out.println("  Orthogonal wavelets count: " + orthogonal.size());
        System.out.println("  Each wavelet appears exactly once");
        System.out.println("  No 'db4' vs 'daubechies4' confusion!");
        
        System.out.println();
    }
    
    
    private static void demonstrateIDEBenefits() {
        System.out.println("5. IDE Benefits:");
        System.out.println("-----------------------------");
        System.out.println("  With enum-based approach:");
        System.out.println("  ✓ Autocomplete shows all available wavelets");
        System.out.println("  ✓ Javadoc appears for each wavelet");
        System.out.println("  ✓ Refactoring is safe and automatic");
        System.out.println("  ✓ Find usages works perfectly");
        System.out.println("  ✓ No runtime errors from typos");
        
        System.out.println("\n  Example - switch on wavelet type:");
        WaveletName selectedWavelet = WaveletName.DB4;
        
        switch (selectedWavelet) {
            case HAAR -> System.out.println("    Using Haar wavelet");
            case DB2, DB4, DB6, DB8, DB10 -> System.out.println("    Using Daubechies family");
            case SYM2, SYM3, SYM4 -> System.out.println("    Using Symlet family");
            default -> System.out.println("    Using other wavelet");
        }
        
        System.out.println();
    }
}