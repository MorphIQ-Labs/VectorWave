package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for MODWTTransformFactory.
 */
class MODWTTransformFactoryTest {
    
    @Test
    void testCreateWithWavelet() {
        MODWTTransform transform = MODWTTransformFactory.create(new Haar());
        assertNotNull(transform);
        assertEquals("Haar", transform.getWavelet().name());
        assertEquals(BoundaryMode.PERIODIC, transform.getBoundaryMode());
    }
    
    @Test
    void testCreateWithWaveletAndBoundaryMode() {
        MODWTTransform transform = MODWTTransformFactory.create(
            Daubechies.DB4, BoundaryMode.PERIODIC);
        assertNotNull(transform);
        assertEquals("db4", transform.getWavelet().name());
        assertEquals(BoundaryMode.PERIODIC, transform.getBoundaryMode());
    }
    
    @Test
    void testCreateWithWaveletName() {
        MODWTTransform transform = MODWTTransformFactory.create(WaveletName.HAAR);
        assertNotNull(transform);
        assertEquals("Haar", transform.getWavelet().name());
    }
    
    @Test
    void testCreateWithNullWaveletName() {
        assertThrows(InvalidArgumentException.class, () -> {
            MODWTTransformFactory.create((WaveletName) null);
        });
    }
    
    @Test
    void testCreateMultiLevel() {
        MultiLevelMODWTTransform transform = MODWTTransformFactory.createMultiLevel(new Haar());
        assertNotNull(transform);
        assertEquals("Haar", transform.getWavelet().name());
        assertEquals(BoundaryMode.PERIODIC, transform.getBoundaryMode());
    }
    
    @Test
    void testCreateMultiLevelWithBoundaryMode() {
        MultiLevelMODWTTransform transform = MODWTTransformFactory.createMultiLevel(
            Symlet.SYM4, BoundaryMode.PERIODIC);
        assertNotNull(transform);
        assertEquals("sym4", transform.getWavelet().name());
        assertEquals(BoundaryMode.PERIODIC, transform.getBoundaryMode());
    }
    
    @Test
    void testCreateMultiLevelWithWaveletName() {
        MultiLevelMODWTTransform transform = MODWTTransformFactory.createMultiLevel(WaveletName.DB4);
        assertNotNull(transform);
        assertEquals("db4", transform.getWavelet().name());
    }
    
    @Test
    void testFactoryInterface() {
        MODWTTransformFactory factory = MODWTTransformFactory.getInstance();
        MODWTTransformFactory.Config config = new MODWTTransformFactory.Config(
            new Haar(), BoundaryMode.PERIODIC);
        
        MODWTTransform transform = factory.create(config);
        assertNotNull(transform);
        assertEquals("Haar", transform.getWavelet().name());
        assertEquals(BoundaryMode.PERIODIC, transform.getBoundaryMode());
    }
    
    @Test
    void testConfigWithDefaultBoundaryMode() {
        MODWTTransformFactory.Config config = new MODWTTransformFactory.Config(new Haar());
        assertEquals(BoundaryMode.PERIODIC, config.getBoundaryMode());
    }
    
    @Test
    void testSingleton() {
        MODWTTransformFactory factory1 = MODWTTransformFactory.getInstance();
        MODWTTransformFactory factory2 = MODWTTransformFactory.getInstance();
        assertSame(factory1, factory2);
    }
    
    @Test
    void testNullParameters() {
        assertThrows(NullPointerException.class, () -> {
            MODWTTransformFactory.create((Wavelet) null);
        });
        
        assertThrows(NullPointerException.class, () -> {
            MODWTTransformFactory.create(new Haar(), null);
        });
        
        assertThrows(NullPointerException.class, () -> {
            MODWTTransformFactory factory = MODWTTransformFactory.getInstance();
            factory.create((MODWTTransformFactory.Config) null);
        });
    }
    
    @Test
    void testWithDifferentWavelets() {
        // Test with various wavelet types
        Wavelet[] wavelets = {
            new Haar(),
            Daubechies.DB2,
            Daubechies.DB4,
            Symlet.SYM2,
            Coiflet.COIF1
        };
        
        for (Wavelet wavelet : wavelets) {
            MODWTTransform transform = MODWTTransformFactory.create(wavelet);
            assertNotNull(transform);
            assertEquals(wavelet.name(), transform.getWavelet().name());
            
            // Test that transform works
            double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
            MODWTResult result = transform.forward(signal);
            assertNotNull(result);
            assertTrue(result.isValid());
        }
    }
}