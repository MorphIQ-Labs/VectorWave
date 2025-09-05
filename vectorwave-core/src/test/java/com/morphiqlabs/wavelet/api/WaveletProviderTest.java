package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the WaveletProvider interface and its default behavior.
 */
class WaveletProviderTest {
    
    /**
     * Mock implementation for testing.
     */
    static class MockWaveletProvider implements WaveletProvider {
        private final List<Wavelet> wavelets;
        
        MockWaveletProvider(Wavelet... wavelets) {
            this.wavelets = Arrays.asList(wavelets);
        }
        
        @Override
        public List<Wavelet> getWavelets() {
            return wavelets;
        }
    }
    
    /**
     * Provider that returns null (error case).
     */
    static class NullWaveletProvider implements WaveletProvider {
        @Override
        public List<Wavelet> getWavelets() {
            return null;
        }
    }
    
    /**
     * Provider that returns an empty list.
     */
    static class EmptyWaveletProvider implements WaveletProvider {
        @Override
        public List<Wavelet> getWavelets() {
            return List.of();
        }
    }
    
    /**
     * Provider that throws an exception.
     */
    static class ExceptionThrowingProvider implements WaveletProvider {
        @Override
        public List<Wavelet> getWavelets() {
            throw new RuntimeException("Provider error");
        }
    }
    
    @Test
    void testMockProvider() {
        Wavelet haar = new Haar();
        MockWaveletProvider provider = new MockWaveletProvider(haar);
        
        List<Wavelet> wavelets = provider.getWavelets();
        assertNotNull(wavelets);
        assertEquals(1, wavelets.size());
        assertEquals(haar, wavelets.get(0));
    }
    
    @Test
    void testNullProvider() {
        NullWaveletProvider provider = new NullWaveletProvider();
        assertNull(provider.getWavelets());
    }
    
    @Test
    void testEmptyProvider() {
        EmptyWaveletProvider provider = new EmptyWaveletProvider();
        List<Wavelet> wavelets = provider.getWavelets();
        
        assertNotNull(wavelets);
        assertTrue(wavelets.isEmpty());
    }
    
    @Test
    void testExceptionProvider() {
        ExceptionThrowingProvider provider = new ExceptionThrowingProvider();
        
        assertThrows(RuntimeException.class, provider::getWavelets);
    }
    
    @Test
    void testProviderInterface() {
        // Test that the interface is properly defined
        assertTrue(WaveletProvider.class.isInterface());
        
        // Should have exactly one method
        assertEquals(1, WaveletProvider.class.getDeclaredMethods().length);
        
        // The method should be getWavelets
        assertEquals("getWavelets", 
                    WaveletProvider.class.getDeclaredMethods()[0].getName());
    }
    
    @Test
    void testMultipleWaveletsProvider() {
        Wavelet haar = new Haar();
        Wavelet db2 = Daubechies.DB2;
        Wavelet db4 = Daubechies.DB4;
        
        MockWaveletProvider provider = new MockWaveletProvider(haar, db2, db4);
        
        List<Wavelet> wavelets = provider.getWavelets();
        assertNotNull(wavelets);
        assertEquals(3, wavelets.size());
        assertTrue(wavelets.contains(haar));
        assertTrue(wavelets.contains(db2));
        assertTrue(wavelets.contains(db4));
    }
}