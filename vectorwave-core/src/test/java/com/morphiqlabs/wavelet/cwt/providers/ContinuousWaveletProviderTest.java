package com.morphiqlabs.wavelet.cwt.providers;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.cwt.GaussianDerivativeWavelet;
import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for ContinuousWaveletProvider.
 */
class ContinuousWaveletProviderTest {
    
    private ContinuousWaveletProvider provider;
    
    @BeforeEach
    void setUp() {
        provider = new ContinuousWaveletProvider();
    }
    
    @Test
    void testGetWavelets() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        assertNotNull(wavelets);
        assertFalse(wavelets.isEmpty());
        assertEquals(5, wavelets.size()); // Morlet + 4 Gaussian derivatives
    }
    
    @Test
    void testWaveletTypes() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        for (Wavelet wavelet : wavelets) {
            assertEquals(WaveletType.CONTINUOUS, wavelet.getType());
            assertTrue(wavelet instanceof ContinuousWavelet);
        }
    }
    
    @Test
    void testWaveletNames() {
        List<Wavelet> wavelets = provider.getWavelets();
        Set<String> names = wavelets.stream()
            .map(Wavelet::name)
            .collect(Collectors.toSet());
        
        // Should contain Morlet and Gaussian derivatives
        assertTrue(names.contains("morl"));
        assertTrue(names.contains("gaus1"));
        assertTrue(names.contains("gaus2"));
        assertTrue(names.contains("gaus3"));
        assertTrue(names.contains("gaus4"));
    }
    
    @Test
    void testMorletWavelet() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        Wavelet morlet = wavelets.stream()
            .filter(w -> w.name().equals("morl"))
            .findFirst()
            .orElseThrow();
        
        assertTrue(morlet instanceof MorletWavelet);
        assertNotNull(morlet.description());
        assertTrue(morlet.description().contains("Morlet"));
        
        // Test that filters are provided (even though they're discretized)
        assertNotNull(morlet.lowPassDecomposition());
        assertNotNull(morlet.highPassDecomposition());
    }
    
    @Test
    void testGaussianDerivativeWavelets() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        // Test each order of Gaussian derivative
        for (int order = 1; order <= 4; order++) {
            final int testOrder = order;
            Wavelet gaus = wavelets.stream()
                .filter(w -> w.name().equals("gaus" + testOrder))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing gaus" + testOrder));
            
            assertTrue(gaus instanceof GaussianDerivativeWavelet);
            assertEquals("gaus" + order, gaus.name());
            
            // Verify the name contains the order
            assertTrue(gaus.name().endsWith(String.valueOf(order)));
        }
    }
    
    @Test
    void testContinuousWaveletProperties() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        for (Wavelet wavelet : wavelets) {
            ContinuousWavelet cw = (ContinuousWavelet) wavelet;
            
            // Test center frequency
            double centerFreq = cw.centerFrequency();
            assertTrue(centerFreq > 0);
            
            // Test bandwidth
            double bandwidth = cw.bandwidth();
            assertTrue(bandwidth > 0);
            
            // Test psi function
            double psiValue = cw.psi(0.0);
            assertFalse(Double.isNaN(psiValue));
            assertFalse(Double.isInfinite(psiValue));
        }
    }
    
    @Test
    void testListImmutability() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            wavelets.add(new MorletWavelet());
        });
    }
    
    @Test
    void testProviderConsistency() {
        List<Wavelet> wavelets1 = provider.getWavelets();
        List<Wavelet> wavelets2 = provider.getWavelets();
        
        assertEquals(wavelets1.size(), wavelets2.size());
        
        // Verify same wavelets in same order
        for (int i = 0; i < wavelets1.size(); i++) {
            assertEquals(wavelets1.get(i).name(), wavelets2.get(i).name());
            assertEquals(wavelets1.get(i).getClass(), wavelets2.get(i).getClass());
        }
    }
    
    @Test
    void testFilterDiscretization() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        for (Wavelet wavelet : wavelets) {
            // Continuous wavelets should provide discretized filters
            double[] lowPass = wavelet.lowPassDecomposition();
            double[] highPass = wavelet.highPassDecomposition();
            
            assertNotNull(lowPass);
            assertNotNull(highPass);
            
            // Should have reasonable length
            assertTrue(lowPass.length > 0);
            assertTrue(highPass.length > 0);
            
            // For continuous wavelets, these are typically the same
            assertArrayEquals(lowPass, wavelet.lowPassReconstruction());
            assertArrayEquals(highPass, wavelet.highPassReconstruction());
        }
    }
}