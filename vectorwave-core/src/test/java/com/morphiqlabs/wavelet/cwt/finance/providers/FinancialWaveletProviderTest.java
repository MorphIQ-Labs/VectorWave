package com.morphiqlabs.wavelet.cwt.finance.providers;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.cwt.finance.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for FinancialWaveletProvider.
 */
class FinancialWaveletProviderTest {
    
    private FinancialWaveletProvider provider;
    
    @BeforeEach
    void setUp() {
        provider = new FinancialWaveletProvider();
    }
    
    @Test
    void testGetWavelets() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        assertNotNull(wavelets);
        assertFalse(wavelets.isEmpty());
        assertEquals(4, wavelets.size()); // Paul, DOG, Shannon-Gabor, Classical Shannon
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
    void testWaveletClasses() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        // Verify we have one of each financial wavelet type
        Set<Class<?>> waveletClasses = wavelets.stream()
            .map(Object::getClass)
            .collect(Collectors.toSet());
        
        assertTrue(waveletClasses.contains(PaulWavelet.class));
        assertTrue(waveletClasses.contains(DOGWavelet.class));
        assertTrue(waveletClasses.contains(ShannonGaborWavelet.class));
        assertTrue(waveletClasses.contains(ClassicalShannonWavelet.class));
    }
    
    @Test
    void testPaulWavelet() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        Wavelet paul = wavelets.stream()
            .filter(w -> w instanceof PaulWavelet)
            .findFirst()
            .orElseThrow();
        
        assertTrue(paul.name().startsWith("paul"));
        assertTrue(paul instanceof ComplexContinuousWavelet);
        
        // Paul wavelet should have specific order in name
        assertTrue(paul.name().matches("paul\\d+"));
    }
    
    @Test
    void testDOGWavelet() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        Wavelet dog = wavelets.stream()
            .filter(w -> w instanceof DOGWavelet)
            .findFirst()
            .orElseThrow();
        
        
        // DOG wavelet includes order in name (e.g., dog2 for order 2)
        assertEquals("dog2", dog.name(), "DOG wavelet should have name 'dog2' for order 2");
        assertNotNull(dog.description());
        assertFalse(dog.description().isEmpty());
    }
    
    @Test
    void testShannonWavelets() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        // Test Shannon-Gabor
        Wavelet shannonGabor = wavelets.stream()
            .filter(w -> w instanceof ShannonGaborWavelet)
            .findFirst()
            .orElseThrow();
        
        
        // Shannon-Gabor includes parameters in name (e.g., shan-gabor0.5-1.5 for fb=0.5, fc=1.5)
        assertEquals("shan-gabor0.5-1.5", shannonGabor.name(), 
                    "Shannon-Gabor wavelet should have name 'shan-gabor0.5-1.5' for default parameters");
        
        // Test Classical Shannon
        Wavelet classicalShannon = wavelets.stream()
            .filter(w -> w instanceof ClassicalShannonWavelet)
            .findFirst()
            .orElseThrow();
        
        assertEquals("shan", classicalShannon.name());
    }
    
    @Test
    void testComplexWavelets() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        // Count complex wavelets
        long complexCount = wavelets.stream()
            .filter(w -> w instanceof ComplexContinuousWavelet)
            .count();
        
        
        // Only Paul is complex in this provider
        assertTrue(complexCount >= 1);
        
        // Test complex wavelet properties
        for (Wavelet wavelet : wavelets) {
            if (wavelet instanceof ComplexContinuousWavelet ccw) {
                // Complex wavelets have imaginary component
                double imagPart = ccw.psiImaginary(1.0);
                // Complex wavelets should have non-zero imaginary part
                assertFalse(Double.isNaN(imagPart));
            }
        }
    }
    
    @Test
    void testFinancialWaveletProperties() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        for (Wavelet wavelet : wavelets) {
            ContinuousWavelet cw = (ContinuousWavelet) wavelet;
            
            // Test wavelet properties suitable for financial analysis
            double centerFreq = cw.centerFrequency();
            assertTrue(centerFreq > 0);
            
            // Test bandwidth
            double bandwidth = cw.bandwidth();
            assertTrue(bandwidth > 0);
            
            // Test evaluation
            double psiValue = cw.psi(1.0);
            assertFalse(Double.isNaN(psiValue));
            assertFalse(Double.isInfinite(psiValue));
        }
    }
    
    @Test
    void testListImmutability() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        assertThrows(UnsupportedOperationException.class, () -> {
            wavelets.add(new DOGWavelet());
        });
    }
    
    @Test
    void testProviderConsistency() {
        List<Wavelet> wavelets1 = provider.getWavelets();
        List<Wavelet> wavelets2 = provider.getWavelets();
        
        assertEquals(wavelets1.size(), wavelets2.size());
        
        Set<String> names1 = wavelets1.stream()
            .map(Wavelet::name)
            .collect(Collectors.toSet());
        
        Set<String> names2 = wavelets2.stream()
            .map(Wavelet::name)
            .collect(Collectors.toSet());
        
        assertEquals(names1, names2);
    }
    
    @Test
    void testFinancialWaveletDescriptions() {
        List<Wavelet> wavelets = provider.getWavelets();
        
        for (Wavelet wavelet : wavelets) {
            String desc = wavelet.description();
            assertNotNull(desc);
            assertFalse(desc.isEmpty());
            
            // All financial wavelets should have meaningful descriptions
            assertTrue(desc.length() > 10, "Description should be meaningful for " + wavelet.name());
        }
    }
}