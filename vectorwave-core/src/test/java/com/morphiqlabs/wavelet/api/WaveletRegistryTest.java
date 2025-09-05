package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the enum-based WaveletRegistry.
 */
class WaveletRegistryTest {
    
    @Test
    @DisplayName("Get wavelet by enum name")
    void testGetWavelet() {
        Wavelet haar = WaveletRegistry.getWavelet(WaveletName.HAAR);
        assertNotNull(haar);
        assertEquals("Haar", haar.name());
        
        Wavelet db4 = WaveletRegistry.getWavelet(WaveletName.DB4);
        assertNotNull(db4);
        assertEquals("db4", db4.name());
    }
    
    @Test
    @DisplayName("Get wavelet with null throws exception")
    void testGetWaveletNull() {
        assertThrows(InvalidArgumentException.class, () -> 
            WaveletRegistry.getWavelet(null));
    }
    
    @Test
    @DisplayName("Check wavelet existence")
    void testHasWavelet() {
        assertTrue(WaveletRegistry.hasWavelet(WaveletName.HAAR));
        assertTrue(WaveletRegistry.hasWavelet(WaveletName.DB4));
        assertTrue(WaveletRegistry.hasWavelet(WaveletName.MORLET));
        
        assertFalse(WaveletRegistry.hasWavelet(null));
    }
    
    @Test
    @DisplayName("Get available wavelets")
    void testGetAvailableWavelets() {
        Set<WaveletName> wavelets = WaveletRegistry.getAvailableWavelets();
        
        assertNotNull(wavelets);
        assertFalse(wavelets.isEmpty());
        
        // Check core wavelets are present
        assertTrue(wavelets.contains(WaveletName.HAAR));
        assertTrue(wavelets.contains(WaveletName.DB2));
        assertTrue(wavelets.contains(WaveletName.DB4));
        assertTrue(wavelets.contains(WaveletName.SYM2));
        assertTrue(wavelets.contains(WaveletName.COIF1));
        assertTrue(wavelets.contains(WaveletName.MORLET));
    }
    
    @Test
    @DisplayName("Get orthogonal wavelets")
    void testGetOrthogonalWavelets() {
        List<WaveletName> orthogonal = WaveletRegistry.getOrthogonalWavelets();
        
        assertNotNull(orthogonal);
        assertFalse(orthogonal.isEmpty());
        
        // Should contain orthogonal wavelets
        assertTrue(orthogonal.contains(WaveletName.HAAR));
        assertTrue(orthogonal.contains(WaveletName.DB2));
        assertTrue(orthogonal.contains(WaveletName.DB4));
        assertTrue(orthogonal.contains(WaveletName.SYM2));
        assertTrue(orthogonal.contains(WaveletName.COIF1));
        
        // Should NOT contain continuous wavelets
        assertFalse(orthogonal.contains(WaveletName.MORLET));
    }
    
    @Test
    @DisplayName("Get continuous wavelets")
    void testGetContinuousWavelets() {
        List<WaveletName> continuous = WaveletRegistry.getContinuousWavelets();
        
        assertNotNull(continuous);
        assertFalse(continuous.isEmpty());
        
        // Should contain continuous wavelets
        assertTrue(continuous.contains(WaveletName.MORLET));
        
        // Should NOT contain orthogonal wavelets
        assertFalse(continuous.contains(WaveletName.HAAR));
        assertFalse(continuous.contains(WaveletName.DB4));
        assertFalse(continuous.contains(WaveletName.SYM2));
    }
    
    @Test
    @DisplayName("All registered wavelets are valid")
    void testAllWaveletsValid() {
        Set<WaveletName> wavelets = WaveletRegistry.getAvailableWavelets();
        
        for (WaveletName name : wavelets) {
            Wavelet w = WaveletRegistry.getWavelet(name);
            assertNotNull(w, "Wavelet " + name + " should not be null");
            assertNotNull(w.name(), "Wavelet " + name + " should have a name");
            assertNotNull(w.getType(), "Wavelet " + name + " should have a type");
            assertNotNull(w.description(), "Wavelet " + name + " should have a description");
        }
    }
    
    @Test
    @DisplayName("Get Daubechies wavelets")
    void testGetDaubechiesWavelets() {
        List<WaveletName> daubechies = WaveletRegistry.getDaubechiesWavelets();
        
        assertNotNull(daubechies);
        // Now includes extended Daubechies wavelets up to PyWavelets maximum
        // Original: DB2, DB4, DB6, DB8, DB10 (5)
        // First Extended: DB12, DB14, DB16, DB18, DB20 (5)
        // Advanced Extended: DB22, DB24, DB26, DB28, DB30, DB32, DB34, DB36, DB38 (9)
        // Total: 19 wavelets
        assertEquals(19, daubechies.size());
        
        // Original Daubechies wavelets
        assertTrue(daubechies.contains(WaveletName.DB2));
        assertTrue(daubechies.contains(WaveletName.DB4));
        assertTrue(daubechies.contains(WaveletName.DB6));
        assertTrue(daubechies.contains(WaveletName.DB8));
        assertTrue(daubechies.contains(WaveletName.DB10));
        
        // First Extended Set (implemented via DaubechiesGenerator)
        assertTrue(daubechies.contains(WaveletName.DB12));
        assertTrue(daubechies.contains(WaveletName.DB14));
        assertTrue(daubechies.contains(WaveletName.DB16));
        assertTrue(daubechies.contains(WaveletName.DB18));
        assertTrue(daubechies.contains(WaveletName.DB20));
        
        // Advanced Extended Set (implemented via DaubechiesGenerator with PyWavelets coefficients)
        assertTrue(daubechies.contains(WaveletName.DB22));
        assertTrue(daubechies.contains(WaveletName.DB24));
        assertTrue(daubechies.contains(WaveletName.DB26));
        assertTrue(daubechies.contains(WaveletName.DB28));
        assertTrue(daubechies.contains(WaveletName.DB30));
        assertTrue(daubechies.contains(WaveletName.DB32));
        assertTrue(daubechies.contains(WaveletName.DB34));
        assertTrue(daubechies.contains(WaveletName.DB36));
        assertTrue(daubechies.contains(WaveletName.DB38)); // PyWavelets maximum
    }
    
    @Test
    @DisplayName("Get Symlet wavelets")
    void testGetSymletWavelets() {
        List<WaveletName> symlets = WaveletRegistry.getSymletWavelets();
        
        assertNotNull(symlets);
        assertEquals(19, symlets.size()); // SYM2-SYM20 including new ones
        assertTrue(symlets.contains(WaveletName.SYM2));
        assertTrue(symlets.contains(WaveletName.SYM3));
        assertTrue(symlets.contains(WaveletName.SYM20));
    }
    
    @Test
    @DisplayName("Get Coiflet wavelets")
    void testGetCoifletWavelets() {
        List<WaveletName> coiflets = WaveletRegistry.getCoifletWavelets();
        
        assertNotNull(coiflets);
        assertEquals(17, coiflets.size());
        
        // Test all 17 Coiflets are present
        assertTrue(coiflets.contains(WaveletName.COIF1));
        assertTrue(coiflets.contains(WaveletName.COIF2));
        assertTrue(coiflets.contains(WaveletName.COIF3));
        assertTrue(coiflets.contains(WaveletName.COIF4));
        assertTrue(coiflets.contains(WaveletName.COIF5));
        assertTrue(coiflets.contains(WaveletName.COIF6));
        assertTrue(coiflets.contains(WaveletName.COIF7));
        assertTrue(coiflets.contains(WaveletName.COIF8));
        assertTrue(coiflets.contains(WaveletName.COIF9));
        assertTrue(coiflets.contains(WaveletName.COIF10));
        assertTrue(coiflets.contains(WaveletName.COIF11));
        assertTrue(coiflets.contains(WaveletName.COIF12));
        assertTrue(coiflets.contains(WaveletName.COIF13));
        assertTrue(coiflets.contains(WaveletName.COIF14));
        assertTrue(coiflets.contains(WaveletName.COIF15));
        assertTrue(coiflets.contains(WaveletName.COIF16));
        assertTrue(coiflets.contains(WaveletName.COIF17));
    }
    
    @Test
    @DisplayName("Registry is thread-safe")
    void testThreadSafety() throws InterruptedException {
        int threads = 10;
        int iterations = 1000;
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger errors = new AtomicInteger(0);
        
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                try {
                    for (int j = 0; j < iterations; j++) {
                        // Concurrent reads
                        WaveletRegistry.getWavelet(WaveletName.HAAR);
                        WaveletRegistry.hasWavelet(WaveletName.DB4);
                        WaveletRegistry.getAvailableWavelets();
                        WaveletRegistry.getOrthogonalWavelets();
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }
        
        latch.await();
        assertEquals(0, errors.get(), "No errors should occur during concurrent access");
    }
    
    @Test
    @DisplayName("WaveletName enum properties")
    void testWaveletNameEnumProperties() {
        // Test getCode method returns correct codes
        assertEquals("db4", WaveletName.DB4.getCode());
        assertEquals("haar", WaveletName.HAAR.getCode());
        assertEquals("morl", WaveletName.MORLET.getCode());
        assertEquals("sym8", WaveletName.SYM8.getCode());
        assertEquals("coif3", WaveletName.COIF3.getCode());
        
        // Test getDescription method
        assertEquals("Daubechies 4", WaveletName.DB4.getDescription());
        assertEquals("Haar wavelet", WaveletName.HAAR.getDescription());
        assertEquals("Morlet wavelet", WaveletName.MORLET.getDescription());
        
        // Test getType method
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.DB4.getType());
        assertEquals(WaveletType.ORTHOGONAL, WaveletName.HAAR.getType());
        assertEquals(WaveletType.CONTINUOUS, WaveletName.MORLET.getType());
        assertEquals(WaveletType.COMPLEX, WaveletName.CMOR.getType());
        
        // Test toString returns the description (display name)
        assertEquals("Daubechies 4", WaveletName.DB4.toString());
        assertEquals("Haar wavelet", WaveletName.HAAR.toString());
    }
}