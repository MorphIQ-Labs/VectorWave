package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that WaveletRegistry with enum-based approach has no duplicates.
 */
class WaveletRegistryDuplicateTest {
    
    @Test
    @DisplayName("getOrthogonalWavelets returns unique wavelets")
    void testNoDuplicatesInOrthogonalWavelets() {
        List<WaveletName> orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
        
        // Check uniqueness by converting to set
        Set<WaveletName> uniqueWavelets = new HashSet<>(orthogonalWavelets);
        assertEquals(uniqueWavelets.size(), orthogonalWavelets.size(),
            "Orthogonal wavelets list should not contain duplicates");
    }
    
    @Test
    @DisplayName("Enum ensures no duplicate wavelet names")
    void testEnumEnsuresNoDuplicates() {
        // By using enum, duplicates are impossible at compile time
        Set<WaveletName> allNames = EnumSet.allOf(WaveletName.class);
        
        // Check that each enum value has a unique code
        Set<String> codes = new HashSet<>();
        for (WaveletName name : allNames) {
            String code = name.getCode();
            assertFalse(codes.contains(code),
                "Duplicate code found: " + code + " for " + name);
            codes.add(code);
        }
    }
    
    @Test
    @DisplayName("Registry contains exactly one instance per wavelet type")
    void testOneInstancePerWaveletType() {
        Set<WaveletName> availableWavelets = WaveletRegistry.getAvailableWavelets();
        
        // Map to track wavelet instances
        Map<String, WaveletName> waveletNameMap = new HashMap<>();
        
        for (WaveletName name : availableWavelets) {
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            String waveletName = wavelet.name().toLowerCase();
            
            // Check if we've seen this wavelet name before
            if (waveletNameMap.containsKey(waveletName)) {
                fail(String.format("Duplicate wavelet found: %s maps to both %s and %s",
                    waveletName, waveletNameMap.get(waveletName), name));
            }
            waveletNameMap.put(waveletName, name);
        }
    }
    
    @Test
    @DisplayName("Print enum-based registry analysis")
    void analyzeEnumBasedRegistry() {
        Set<WaveletName> availableWavelets = WaveletRegistry.getAvailableWavelets();
        
        System.out.println("\n=== Enum-Based Registry Analysis ===");
        System.out.println("Total wavelets registered: " + availableWavelets.size());
        
        // Group by type
        Map<WaveletType, List<WaveletName>> byType = new HashMap<>();
        for (WaveletName name : availableWavelets) {
            Wavelet wavelet = WaveletRegistry.getWavelet(name);
            byType.computeIfAbsent(wavelet.getType(), k -> new ArrayList<>()).add(name);
        }
        
        System.out.println("\nWavelets by type:");
        for (Map.Entry<WaveletType, List<WaveletName>> entry : byType.entrySet()) {
            System.out.println("  " + entry.getKey() + ": " + entry.getValue().size() + " wavelets");
            for (WaveletName name : entry.getValue()) {
                System.out.println("    - " + name + " (" + name.getCode() + ")");
            }
        }
        
        System.out.println("\nType-safe access benefits:");
        System.out.println("  - No string typos possible");
        System.out.println("  - IDE autocomplete support");
        System.out.println("  - Compile-time checking");
        System.out.println("  - No duplicate registrations possible");
    }
}