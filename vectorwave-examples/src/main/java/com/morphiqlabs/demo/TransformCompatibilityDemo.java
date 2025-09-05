package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import java.util.Set;
import java.util.List;

/**
 * Demonstrates the Transform Compatibility API that shows which wavelets
 * work with which transforms, improving API discoverability and usability.
 */
public class TransformCompatibilityDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Transform Compatibility Demo ===\n");
        
        // Demo 1: Check what transforms a specific wavelet supports
        checkWaveletCompatibility();
        
        // Demo 2: Find wavelets for a specific transform
        findWaveletsForTransform();
        
        // Demo 3: Verify compatibility before use
        verifyCompatibility();
        
        // Demo 4: Get recommendations
        getRecommendations();
        
        // Demo 5: Show full compatibility matrix
        showCompatibilityMatrix();
    }
    
    private static void checkWaveletCompatibility() {
        System.out.println("1. Check what transforms a wavelet supports:");
        System.out.println("--------------------------------------------");
        
        // Check DB4 (orthogonal wavelet)
        WaveletName db4 = WaveletName.DB4;
        Set<TransformType> db4Transforms = WaveletRegistry.getSupportedTransforms(db4);
        System.out.println("DB4 (Orthogonal) supports:");
        for (TransformType transform : db4Transforms) {
            System.out.println("  ✓ " + transform.getDescription());
        }
        
        System.out.println();
        
        // Check Morlet (continuous wavelet)
        WaveletName morlet = WaveletName.MORLET;
        Set<TransformType> morletTransforms = WaveletRegistry.getSupportedTransforms(morlet);
        System.out.println("Morlet (Continuous) supports:");
        for (TransformType transform : morletTransforms) {
            System.out.println("  ✓ " + transform.getDescription());
        }
        
        System.out.println();
    }
    
    private static void findWaveletsForTransform() {
        System.out.println("2. Find wavelets compatible with a transform:");
        System.out.println("---------------------------------------------");
        
        // Find wavelets for MODWT
        List<WaveletName> modwtWavelets = WaveletRegistry.getWaveletsForTransform(TransformType.MODWT);
        System.out.println("Wavelets compatible with MODWT:");
        System.out.println("  Total: " + modwtWavelets.size() + " wavelets");
        System.out.println("  Examples: " + modwtWavelets.subList(0, Math.min(5, modwtWavelets.size())));
        
        System.out.println();
        
        // Find wavelets for CWT
        List<WaveletName> cwtWavelets = WaveletRegistry.getWaveletsForTransform(TransformType.CWT);
        System.out.println("Wavelets compatible with CWT:");
        System.out.println("  Total: " + cwtWavelets.size() + " wavelets");
        System.out.println("  Examples: " + cwtWavelets);
        
        System.out.println();
    }
    
    private static void verifyCompatibility() {
        System.out.println("3. Verify compatibility before use:");
        System.out.println("-----------------------------------");
        
        // Check if DB4 can be used with MODWT
        boolean db4WithModwt = WaveletRegistry.isCompatible(WaveletName.DB4, TransformType.MODWT);
        System.out.println("Can DB4 be used with MODWT? " + (db4WithModwt ? "✓ Yes" : "✗ No"));
        
        // Check if DB4 can be used with CWT
        boolean db4WithCwt = WaveletRegistry.isCompatible(WaveletName.DB4, TransformType.CWT);
        System.out.println("Can DB4 be used with CWT? " + (db4WithCwt ? "✓ Yes" : "✗ No"));
        
        // Check if Morlet can be used with CWT
        boolean morletWithCwt = WaveletRegistry.isCompatible(WaveletName.MORLET, TransformType.CWT);
        System.out.println("Can Morlet be used with CWT? " + (morletWithCwt ? "✓ Yes" : "✗ No"));
        
        // Check if Morlet can be used with MODWT
        boolean morletWithModwt = WaveletRegistry.isCompatible(WaveletName.MORLET, TransformType.MODWT);
        System.out.println("Can Morlet be used with MODWT? " + (morletWithModwt ? "✓ Yes" : "✗ No"));
        
        System.out.println();
    }
    
    private static void getRecommendations() {
        System.out.println("4. Get transform recommendations:");
        System.out.println("---------------------------------");
        
        WaveletName[] testWavelets = {
            WaveletName.HAAR,
            WaveletName.DB4,
            WaveletName.SYM8,
            WaveletName.MORLET,
            WaveletName.MEXICAN_HAT
        };
        
        for (WaveletName wavelet : testWavelets) {
            TransformType recommended = WaveletRegistry.getRecommendedTransform(wavelet);
            System.out.printf("%-15s → Recommended: %s\n", 
                wavelet.getCode(), 
                recommended != null ? recommended.getDescription() : "None");
        }
        
        System.out.println();
    }
    
    private static void showCompatibilityMatrix() {
        System.out.println("5. Full Compatibility Matrix:");
        System.out.println("-----------------------------");
        
        // Show a subset for demo purposes
        System.out.println("(Showing subset - call WaveletRegistry.printTransformCompatibilityMatrix() for full matrix)");
        System.out.println();
        
        System.out.printf("%-20s %-15s %-15s %-15s\n", 
            "Wavelet", "MODWT", "SWT", "CWT");
        System.out.println("-".repeat(65));
        
        WaveletName[] sampleWavelets = {
            WaveletName.HAAR,
            WaveletName.DB4,
            WaveletName.SYM8,
            WaveletName.COIF3,
            WaveletName.MORLET,
            WaveletName.MEXICAN_HAT,
            WaveletName.GAUSSIAN
        };
        
        TransformType[] sampleTransforms = {
            TransformType.MODWT,
            TransformType.SWT,
            TransformType.CWT
        };
        
        for (WaveletName wavelet : sampleWavelets) {
            System.out.printf("%-20s", wavelet.getCode());
            for (TransformType transform : sampleTransforms) {
                String compatible = WaveletRegistry.isCompatible(wavelet, transform) ? "✓" : "-";
                System.out.printf("%-15s", compatible);
            }
            System.out.println();
        }
        
        System.out.println();
    }
}