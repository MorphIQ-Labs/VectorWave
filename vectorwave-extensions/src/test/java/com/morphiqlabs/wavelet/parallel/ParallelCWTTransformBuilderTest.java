package com.morphiqlabs.wavelet.extensions.parallel;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.cwt.CWTConfig;
import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.cwt.ComplexMorletWavelet;
import com.morphiqlabs.wavelet.cwt.GaussianDerivativeWavelet;
import com.morphiqlabs.wavelet.cwt.CWTResult;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test coverage for ParallelCWTTransform.Builder class.
 */
class ParallelCWTTransformBuilderTest {
    
    private ParallelCWTTransform transform;
    
    @AfterEach
    void tearDown() {
        if (transform != null) {
            transform.close();
        }
    }
    
    @Test
    void testBuilderWithAllParameters() {
        ContinuousWavelet wavelet = new MorletWavelet(6.0, 1.0);
        CWTConfig cwtConfig = CWTConfig.defaultConfig();
        ParallelConfig parallelConfig = new ParallelConfig.Builder()
            .parallelismLevel(4)
            .useVirtualThreads(false)
            .enableMetrics(true)
            .build();
        
        transform = new ParallelCWTTransform.Builder()
            .wavelet(wavelet)
            .cwtConfig(cwtConfig)
            .parallelConfig(parallelConfig)
            .build();
        
        assertNotNull(transform);
        
        // Test that the transform works
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] scales = {1.0, 2.0, 4.0};
        CWTResult result = transform.analyze(signal, scales);
        
        assertNotNull(result);
        assertEquals(3, result.getScales().length);
        assertEquals(8, result.getCoefficients()[0].length);
    }
    
    @Test
    void testBuilderWithMinimalParameters() {
        ContinuousWavelet wavelet = new ComplexMorletWavelet(6.0, 1.0);
        
        transform = new ParallelCWTTransform.Builder()
            .wavelet(wavelet)
            .build();
        
        assertNotNull(transform);
        
        // Test with default configs
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] scales = {1.0, 2.0};
        CWTResult result = transform.analyze(signal, scales);
        
        assertNotNull(result);
        assertEquals(2, result.getScales().length);
    }
    
    @Test
    void testBuilderWithoutWaveletThrows() {
        ParallelCWTTransform.Builder builder = new ParallelCWTTransform.Builder();
        
        assertThrows(IllegalStateException.class, () -> builder.build(),
            "Wavelet must be specified");
    }
    
    @Test
    void testBuilderWithCustomCWTConfig() {
        ContinuousWavelet wavelet = new GaussianDerivativeWavelet(2);
        CWTConfig cwtConfig = CWTConfig.defaultConfig();
        
        transform = new ParallelCWTTransform.Builder()
            .wavelet(wavelet)
            .cwtConfig(cwtConfig)
            .build();
        
        assertNotNull(transform);
        
        // Verify it works with custom config
        double[] signal = new double[100];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 20);
        }
        double[] scales = {0.5, 1.0, 2.0, 4.0, 8.0};
        
        CWTResult result = transform.analyze(signal, scales);
        assertNotNull(result);
        assertEquals(5, result.getScales().length);
        assertEquals(100, result.getCoefficients()[0].length);
    }
    
    @Test
    void testBuilderWithCustomParallelConfig() {
        ContinuousWavelet wavelet = new MorletWavelet(6.0, 1.0);
        ParallelConfig parallelConfig = new ParallelConfig.Builder()
            .parallelismLevel(8)
            .useVirtualThreads(true)
            .enableMetrics(false)
            .chunkSize(2048)
            .build();
        
        transform = new ParallelCWTTransform.Builder()
            .wavelet(wavelet)
            .parallelConfig(parallelConfig)
            .build();
        
        assertNotNull(transform);
        
        // Test with configuration that triggers parallelization
        double[] signal = new double[10000];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.random();
        }
        double[] scales = new double[10];
        for (int i = 0; i < scales.length; i++) {
            scales[i] = Math.pow(2, i * 0.5);
        }
        
        CWTResult result = transform.analyze(signal, scales);
        assertNotNull(result);
        assertEquals(10, result.getScales().length);
        assertEquals(10000, result.getCoefficients()[0].length);
    }
    
    @Test
    void testBuilderChaining() {
        ContinuousWavelet wavelet = new MorletWavelet(6.0, 1.0);
        CWTConfig cwtConfig = CWTConfig.defaultConfig();
        ParallelConfig parallelConfig = ParallelConfig.auto();
        
        // Test method chaining
        ParallelCWTTransform.Builder builder = new ParallelCWTTransform.Builder()
            .wavelet(wavelet)
            .cwtConfig(cwtConfig)
            .parallelConfig(parallelConfig);
        
        assertNotNull(builder);
        transform = builder.build();
        assertNotNull(transform);
    }
    
    @Test
    void testBuilderWithNullConfigs() {
        ContinuousWavelet wavelet = new MorletWavelet(6.0, 1.0);
        
        // Setting null configs should throw exception
        assertThrows(IllegalArgumentException.class, () -> 
            new ParallelCWTTransform.Builder()
                .wavelet(wavelet)
                .cwtConfig(null)
                .parallelConfig(null)
                .build());
    }
    
    @Test
    void testBuilderWithDifferentWavelets() {
        // Test with different wavelet types
        ContinuousWavelet[] wavelets = {
            new MorletWavelet(6.0, 1.0),
            new ComplexMorletWavelet(6.0, 1.0),
            new GaussianDerivativeWavelet(1),
            new GaussianDerivativeWavelet(3)
        };
        
        for (ContinuousWavelet wavelet : wavelets) {
            ParallelCWTTransform testTransform = new ParallelCWTTransform.Builder()
                .wavelet(wavelet)
                .build();
            
            assertNotNull(testTransform);
            
            // Quick test
            double[] signal = {1.0, 2.0, 3.0, 4.0};
            double[] scales = {1.0, 2.0};
            CWTResult result = testTransform.analyze(signal, scales);
            assertNotNull(result);
            
            testTransform.close();
        }
    }
    
    @Test
    void testBuilderReuseAfterBuild() {
        ContinuousWavelet wavelet1 = new MorletWavelet(6.0, 1.0);
        ContinuousWavelet wavelet2 = new ComplexMorletWavelet(6.0, 1.0);
        
        ParallelCWTTransform.Builder builder = new ParallelCWTTransform.Builder()
            .wavelet(wavelet1);
        
        // First build
        ParallelCWTTransform transform1 = builder.build();
        assertNotNull(transform1);
        
        // Reuse builder with different wavelet
        builder.wavelet(wavelet2);
        ParallelCWTTransform transform2 = builder.build();
        assertNotNull(transform2);
        
        // Both should work independently
        double[] signal = {1.0, 2.0, 3.0, 4.0};
        double[] scales = {1.0};
        
        CWTResult result1 = transform1.analyze(signal, scales);
        CWTResult result2 = transform2.analyze(signal, scales);
        
        assertNotNull(result1);
        assertNotNull(result2);
        
        // Clean up
        transform1.close();
        transform2.close();
    }
    
    @Test
    void testBuilderWithVariousParallelizationStrategies() {
        ContinuousWavelet wavelet = new MorletWavelet(6.0, 1.0);
        
        // Test different parallelization configurations
        ParallelConfig[] configs = {
            ParallelConfig.auto(),         // Auto-detect best strategy
            ParallelConfig.cpuIntensive(), // CPU-intensive configuration
            new ParallelConfig.Builder()   // Custom configuration
                .parallelismLevel(2)
                .useVirtualThreads(false)
                .build(),
            new ParallelConfig.Builder()   // High parallelism
                .parallelismLevel(16)
                .enableMetrics(true)
                .build()
        };
        
        for (ParallelConfig config : configs) {
            ParallelCWTTransform testTransform = new ParallelCWTTransform.Builder()
                .wavelet(wavelet)
                .parallelConfig(config)
                .build();
            
            assertNotNull(testTransform);
            
            // Test with varying signal sizes
            double[] signal = new double[1000];
            for (int i = 0; i < signal.length; i++) {
                signal[i] = Math.random();
            }
            double[] scales = {1.0, 2.0, 4.0, 8.0};
            
            CWTResult result = testTransform.analyze(signal, scales);
            assertNotNull(result);
            assertEquals(4, result.getScales().length);
            
            testTransform.close();
        }
    }
    
    @Test
    void testBuilderErrorHandling() {
        // Test that builder validates inputs
        ParallelCWTTransform.Builder builder = new ParallelCWTTransform.Builder();
        
        // No wavelet set
        assertThrows(IllegalStateException.class, builder::build);
        
        // Set wavelet
        builder.wavelet(new MorletWavelet(6.0, 1.0));
        
        // Now should build successfully
        transform = builder.build();
        assertNotNull(transform);
        
        // Test that transform validates inputs
        assertThrows(InvalidArgumentException.class, 
            () -> transform.analyze(null, new double[]{1.0}));
        assertThrows(InvalidArgumentException.class, 
            () -> transform.analyze(new double[]{1.0}, null));
        assertThrows(InvalidArgumentException.class, 
            () -> transform.analyze(new double[0], new double[]{1.0}));
        assertThrows(InvalidArgumentException.class, 
            () -> transform.analyze(new double[]{1.0}, new double[0]));
    }
}
