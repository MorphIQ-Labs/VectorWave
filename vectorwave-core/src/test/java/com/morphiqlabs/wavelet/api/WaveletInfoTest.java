package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for WaveletInfo and WaveletInfo.Builder.
 * Tests all metadata fields, builder methods, and immutability.
 */
public class WaveletInfoTest {
    
    @Test
    @DisplayName("Test WaveletInfo.Builder with minimal configuration")
    void testBuilderMinimalConfiguration() {
        WaveletInfo info = new WaveletInfo.Builder("db4", WaveletType.ORTHOGONAL)
            .build();
        
        assertNotNull(info);
        assertEquals("db4", info.getName());
        assertEquals("db4", info.getDisplayName()); // Defaults to name
        assertEquals(WaveletType.ORTHOGONAL, info.getType());
        assertNull(info.getFamily());
        assertEquals(0, info.getOrder());
        assertNotNull(info.getAliases());
        assertTrue(info.getAliases().isEmpty());
        assertEquals("", info.getDescription());
        assertEquals(0, info.getVanishingMoments());
        assertEquals(0, info.getFilterLength());
    }
    
    @Test
    @DisplayName("Test WaveletInfo.Builder with full configuration")
    void testBuilderFullConfiguration() {
        Set<String> aliases = new HashSet<>();
        aliases.add("daubechies4");
        aliases.add("D4");
        
        WaveletInfo info = new WaveletInfo.Builder("db4", WaveletType.ORTHOGONAL)
            .displayName("Daubechies 4")
            .family("Daubechies")
            .order(4)
            .aliases(aliases)
            .description("4th order Daubechies orthogonal wavelet")
            .vanishingMoments(4)
            .filterLength(8)
            .build();
        
        assertNotNull(info);
        assertEquals("db4", info.getName());
        assertEquals("Daubechies 4", info.getDisplayName());
        assertEquals(WaveletType.ORTHOGONAL, info.getType());
        assertEquals("Daubechies", info.getFamily());
        assertEquals(4, info.getOrder());
        assertEquals(2, info.getAliases().size());
        assertTrue(info.getAliases().contains("daubechies4"));
        assertTrue(info.getAliases().contains("D4"));
        assertEquals("4th order Daubechies orthogonal wavelet", info.getDescription());
        assertEquals(4, info.getVanishingMoments());
        assertEquals(8, info.getFilterLength());
    }
    
    @Test
    @DisplayName("Test Builder method chaining")
    void testBuilderMethodChaining() {
        WaveletInfo.Builder builder = new WaveletInfo.Builder("sym3", WaveletType.ORTHOGONAL);
        
        // Test that all builder methods return the builder
        assertSame(builder, builder.displayName("Symlet 3"));
        assertSame(builder, builder.family("Symlet"));
        assertSame(builder, builder.order(3));
        assertSame(builder, builder.aliases(new HashSet<>()));
        assertSame(builder, builder.description("Symlet wavelet of order 3"));
        assertSame(builder, builder.vanishingMoments(3));
        assertSame(builder, builder.filterLength(6));
        
        WaveletInfo info = builder.build();
        assertNotNull(info);
        assertEquals("sym3", info.getName());
        assertEquals("Symlet 3", info.getDisplayName());
    }
    
    @Test
    @DisplayName("Test WaveletInfo immutability of aliases")
    void testAliasesImmutability() {
        Set<String> aliases = new HashSet<>();
        aliases.add("alias1");
        aliases.add("alias2");
        
        WaveletInfo info = new WaveletInfo.Builder("test", WaveletType.CONTINUOUS)
            .aliases(aliases)
            .build();
        
        // Get the aliases from the built info
        Set<String> infoAliases = info.getAliases();
        assertEquals(2, infoAliases.size());
        
        // Original set modification should not affect the WaveletInfo
        aliases.add("alias3");
        assertEquals(2, info.getAliases().size());
        assertFalse(info.getAliases().contains("alias3"));
        
        // Returned set should be unmodifiable
        Set<String> returnedAliases = info.getAliases();
        assertThrows(UnsupportedOperationException.class, () -> {
            returnedAliases.add("newAlias");
        });
    }
    
    @Test
    @DisplayName("Test different WaveletType configurations")
    void testDifferentWaveletTypes() {
        // Test ORTHOGONAL
        WaveletInfo orthogonal = new WaveletInfo.Builder("haar", WaveletType.ORTHOGONAL)
            .family("Haar")
            .vanishingMoments(1)
            .filterLength(2)
            .build();
        assertEquals(WaveletType.ORTHOGONAL, orthogonal.getType());
        assertEquals(1, orthogonal.getVanishingMoments());
        assertEquals(2, orthogonal.getFilterLength());
        
        // Test BIORTHOGONAL
        WaveletInfo biorthogonal = new WaveletInfo.Builder("bior1.3", WaveletType.BIORTHOGONAL)
            .family("Biorthogonal")
            .vanishingMoments(1)
            .filterLength(6)
            .build();
        assertEquals(WaveletType.BIORTHOGONAL, biorthogonal.getType());
        assertEquals(1, biorthogonal.getVanishingMoments());
        assertEquals(6, biorthogonal.getFilterLength());
        
        // Test CONTINUOUS
        WaveletInfo continuous = new WaveletInfo.Builder("morlet", WaveletType.CONTINUOUS)
            .family("Morlet")
            .description("Complex Morlet wavelet")
            .build();
        assertEquals(WaveletType.CONTINUOUS, continuous.getType());
        assertEquals(0, continuous.getVanishingMoments()); // Should be 0 for continuous
        assertEquals(0, continuous.getFilterLength()); // Should be 0 for continuous
    }
    
    @Test
    @DisplayName("Test Builder with null and empty values")
    void testBuilderWithNullAndEmptyValues() {
        // Test with null family (should be allowed)
        WaveletInfo info1 = new WaveletInfo.Builder("test1", WaveletType.ORTHOGONAL)
            .family(null)
            .build();
        assertNull(info1.getFamily());
        
        // Test with null description (should accept null)
        WaveletInfo info2 = new WaveletInfo.Builder("test2", WaveletType.ORTHOGONAL)
            .description(null)
            .build();
        // Description might be null or empty string, depending on implementation
        assertTrue(info2.getDescription() == null || info2.getDescription().isEmpty());
        
        // Test with null aliases (should use empty set)
        WaveletInfo info3 = new WaveletInfo.Builder("test3", WaveletType.ORTHOGONAL)
            .aliases(null)
            .build();
        assertNotNull(info3.getAliases());
        
        // Test with empty aliases set
        WaveletInfo info4 = new WaveletInfo.Builder("test4", WaveletType.ORTHOGONAL)
            .aliases(new HashSet<>())
            .build();
        assertTrue(info4.getAliases().isEmpty());
    }
    
    @Test
    @DisplayName("Test multiple builds from same builder")
    void testMultipleBuilds() {
        WaveletInfo.Builder builder = new WaveletInfo.Builder("coif2", WaveletType.ORTHOGONAL)
            .displayName("Coiflet 2")
            .family("Coiflet")
            .order(2);
        
        WaveletInfo info1 = builder.build();
        WaveletInfo info2 = builder.build();
        
        assertNotSame(info1, info2); // Should be different instances
        assertEquals(info1.getName(), info2.getName());
        assertEquals(info1.getDisplayName(), info2.getDisplayName());
        assertEquals(info1.getFamily(), info2.getFamily());
        assertEquals(info1.getOrder(), info2.getOrder());
    }
    
    @Test
    @DisplayName("Test builder modification after build")
    void testBuilderModificationAfterBuild() {
        WaveletInfo.Builder builder = new WaveletInfo.Builder("db2", WaveletType.ORTHOGONAL)
            .displayName("Daubechies 2")
            .order(2);
        
        WaveletInfo info1 = builder.build();
        assertEquals("Daubechies 2", info1.getDisplayName());
        assertEquals(2, info1.getOrder());
        
        // Modify builder after first build
        builder.displayName("Modified Daubechies 2")
            .order(4);
        
        WaveletInfo info2 = builder.build();
        assertEquals("Modified Daubechies 2", info2.getDisplayName());
        assertEquals(4, info2.getOrder());
        
        // First info should remain unchanged
        assertEquals("Daubechies 2", info1.getDisplayName());
        assertEquals(2, info1.getOrder());
    }
    
    @Test
    @DisplayName("Test all getter methods")
    void testAllGetterMethods() {
        Set<String> aliases = Set.of("sym4", "S4");
        
        WaveletInfo info = new WaveletInfo.Builder("symlet4", WaveletType.ORTHOGONAL)
            .displayName("Symlet 4")
            .family("Symlet")
            .order(4)
            .aliases(aliases)
            .description("4th order Symlet wavelet")
            .vanishingMoments(4)
            .filterLength(8)
            .build();
        
        // Test all getters
        assertEquals("symlet4", info.getName());
        assertEquals("Symlet 4", info.getDisplayName());
        assertEquals(WaveletType.ORTHOGONAL, info.getType());
        assertEquals("Symlet", info.getFamily());
        assertEquals(4, info.getOrder());
        assertEquals(2, info.getAliases().size());
        assertTrue(info.getAliases().containsAll(aliases));
        assertEquals("4th order Symlet wavelet", info.getDescription());
        assertEquals(4, info.getVanishingMoments());
        assertEquals(8, info.getFilterLength());
    }
    
    @Test
    @DisplayName("Test edge cases for numeric fields")
    void testNumericFieldEdgeCases() {
        // Test with zero values
        WaveletInfo info1 = new WaveletInfo.Builder("test1", WaveletType.CONTINUOUS)
            .order(0)
            .vanishingMoments(0)
            .filterLength(0)
            .build();
        assertEquals(0, info1.getOrder());
        assertEquals(0, info1.getVanishingMoments());
        assertEquals(0, info1.getFilterLength());
        
        // Test with negative values (should be allowed, though unusual)
        WaveletInfo info2 = new WaveletInfo.Builder("test2", WaveletType.ORTHOGONAL)
            .order(-1)
            .vanishingMoments(-1)
            .filterLength(-1)
            .build();
        assertEquals(-1, info2.getOrder());
        assertEquals(-1, info2.getVanishingMoments());
        assertEquals(-1, info2.getFilterLength());
        
        // Test with large values
        WaveletInfo info3 = new WaveletInfo.Builder("test3", WaveletType.ORTHOGONAL)
            .order(Integer.MAX_VALUE)
            .vanishingMoments(Integer.MAX_VALUE)
            .filterLength(Integer.MAX_VALUE)
            .build();
        assertEquals(Integer.MAX_VALUE, info3.getOrder());
        assertEquals(Integer.MAX_VALUE, info3.getVanishingMoments());
        assertEquals(Integer.MAX_VALUE, info3.getFilterLength());
    }
    
    @Test
    @DisplayName("Test WaveletInfo constructor directly")
    void testDirectConstructor() {
        Set<String> aliases = Set.of("test_alias");
        
        // The constructor is package-private, so we test it through the builder
        // But we can verify all fields are properly set
        WaveletInfo info = new WaveletInfo.Builder("direct_test", WaveletType.BIORTHOGONAL)
            .displayName("Direct Test")
            .family("Test Family")
            .order(5)
            .aliases(aliases)
            .description("Test description")
            .vanishingMoments(5)
            .filterLength(10)
            .build();
        
        // Verify all fields are correctly initialized
        assertNotNull(info);
        assertEquals("direct_test", info.getName());
        assertEquals("Direct Test", info.getDisplayName());
        assertEquals(WaveletType.BIORTHOGONAL, info.getType());
        assertEquals("Test Family", info.getFamily());
        assertEquals(5, info.getOrder());
        assertEquals(1, info.getAliases().size());
        assertTrue(info.getAliases().contains("test_alias"));
        assertEquals("Test description", info.getDescription());
        assertEquals(5, info.getVanishingMoments());
        assertEquals(10, info.getFilterLength());
    }
}