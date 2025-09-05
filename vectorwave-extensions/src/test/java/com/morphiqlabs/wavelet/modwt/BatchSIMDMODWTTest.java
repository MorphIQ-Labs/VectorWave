package com.morphiqlabs.wavelet.extensions.modwt;

import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.util.ThreadLocalManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for batch SIMD MODWT implementation.
 */
class BatchSIMDMODWTTest {
    
    private static final double EPSILON = 1e-10;
    
    @AfterEach
    void cleanup() {
        // Use ThreadLocalManager directly for cleanup
        ThreadLocalManager.cleanupCurrentThread();
    }
    
    @Test
    void testSoAConversion() {
        // Test data
        double[][] signals = {
            {1.0, 2.0, 3.0, 4.0},
            {5.0, 6.0, 7.0, 8.0},
            {9.0, 10.0, 11.0, 12.0}
        };
        
        // Convert to SoA
        double[] soa = new double[12];
        BatchSIMDMODWT.convertToSoA(signals, soa);
        
        // Verify SoA layout: [1,5,9, 2,6,10, 3,7,11, 4,8,12]
        double[] expected = {1.0, 5.0, 9.0, 2.0, 6.0, 10.0, 3.0, 7.0, 11.0, 4.0, 8.0, 12.0};
        assertArrayEquals(expected, soa, EPSILON);
        
        // Convert back
        double[][] reconstructed = new double[3][4];
        BatchSIMDMODWT.convertFromSoA(soa, reconstructed);
        
        // Verify reconstruction
        for (int i = 0; i < signals.length; i++) {
            assertArrayEquals(signals[i], reconstructed[i], EPSILON);
        }
    }
    
    @Test
    void testHaarBatchMODWT() {
        // Create test batch
        int batchSize = 8;
        int signalLength = 16;
        double[][] signals = new double[batchSize][signalLength];
        
        // Initialize with simple patterns
        for (int b = 0; b < batchSize; b++) {
            for (int t = 0; t < signalLength; t++) {
                signals[b][t] = (b + 1) * Math.sin(2 * Math.PI * t / signalLength);
            }
        }
        
        // Sequential reference
        MODWTTransform sequentialTransform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        double[][] expectedApprox = new double[batchSize][signalLength];
        double[][] expectedDetail = new double[batchSize][signalLength];
        
        for (int b = 0; b < batchSize; b++) {
            MODWTResult result = sequentialTransform.forward(signals[b]);
            expectedApprox[b] = result.approximationCoeffs();
            expectedDetail[b] = result.detailCoeffs();
        }
        
        // SIMD batch processing
        double[] soaSignals = new double[batchSize * signalLength];
        double[] soaApprox = new double[batchSize * signalLength];
        double[] soaDetail = new double[batchSize * signalLength];
        
        BatchSIMDMODWT.convertToSoA(signals, soaSignals);
        BatchSIMDMODWT.batchMODWTSoA(soaSignals, soaApprox, soaDetail, 
                                    new Haar(), batchSize, signalLength);
        
        double[][] actualApprox = new double[batchSize][signalLength];
        double[][] actualDetail = new double[batchSize][signalLength];
        
        BatchSIMDMODWT.convertFromSoA(soaApprox, actualApprox);
        BatchSIMDMODWT.convertFromSoA(soaDetail, actualDetail);
        
        // Compare results
        for (int b = 0; b < batchSize; b++) {
            assertArrayEquals(expectedApprox[b], actualApprox[b], EPSILON,
                "Approximation coefficients should match for signal " + b);
            assertArrayEquals(expectedDetail[b], actualDetail[b], EPSILON,
                "Detail coefficients should match for signal " + b);
        }
    }
    
    @Test
    void testDB4BatchMODWT() {
        // Create test batch with random signals
        int batchSize = 16;
        int signalLength = 64;
        double[][] signals = new double[batchSize][signalLength];
        
        Random random = new Random(42);
        for (int b = 0; b < batchSize; b++) {
            for (int t = 0; t < signalLength; t++) {
                signals[b][t] = random.nextGaussian();
            }
        }
        
        // Sequential reference
        MODWTTransform sequentialTransform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        double[][] expectedApprox = new double[batchSize][signalLength];
        double[][] expectedDetail = new double[batchSize][signalLength];
        
        for (int b = 0; b < batchSize; b++) {
            MODWTResult result = sequentialTransform.forward(signals[b]);
            expectedApprox[b] = result.approximationCoeffs();
            expectedDetail[b] = result.detailCoeffs();
        }
        
        // SIMD batch processing
        double[] soaSignals = new double[batchSize * signalLength];
        double[] soaApprox = new double[batchSize * signalLength];
        double[] soaDetail = new double[batchSize * signalLength];
        
        BatchSIMDMODWT.convertToSoA(signals, soaSignals);
        BatchSIMDMODWT.batchMODWTSoA(soaSignals, soaApprox, soaDetail, 
                                    Daubechies.DB4, batchSize, signalLength);
        
        double[][] actualApprox = new double[batchSize][signalLength];
        double[][] actualDetail = new double[batchSize][signalLength];
        
        BatchSIMDMODWT.convertFromSoA(soaApprox, actualApprox);
        BatchSIMDMODWT.convertFromSoA(soaDetail, actualDetail);
        
        // Compare results
        for (int b = 0; b < batchSize; b++) {
            assertArrayEquals(expectedApprox[b], actualApprox[b], EPSILON,
                "DB4 approximation coefficients should match for signal " + b);
            assertArrayEquals(expectedDetail[b], actualDetail[b], EPSILON,
                "DB4 detail coefficients should match for signal " + b);
        }
    }
    
    @Test
    void testNonAlignedBatchSize() {
        // Test with batch size that's not a multiple of vector length
        int batchSize = 7; // Not a power of 2
        int signalLength = 32;
        double[][] signals = new double[batchSize][signalLength];
        
        // Initialize signals
        for (int b = 0; b < batchSize; b++) {
            for (int t = 0; t < signalLength; t++) {
                signals[b][t] = b + t * 0.1;
            }
        }
        
        // SIMD processing
        double[] soaSignals = new double[batchSize * signalLength];
        double[] soaApprox = new double[batchSize * signalLength];
        double[] soaDetail = new double[batchSize * signalLength];
        
        BatchSIMDMODWT.convertToSoA(signals, soaSignals);
        BatchSIMDMODWT.batchMODWTSoA(soaSignals, soaApprox, soaDetail, 
                                    new Haar(), batchSize, signalLength);
        
        // Should not throw and produce valid results
        double[][] actualApprox = new double[batchSize][signalLength];
        BatchSIMDMODWT.convertFromSoA(soaApprox, actualApprox);
        
        // Basic validation - check first few values are non-zero
        assertNotEquals(0.0, actualApprox[0][0]);
        assertNotEquals(0.0, actualApprox[batchSize-1][0]);
    }
    
    @Test
    void testBatchTransformIntegration() {
        // Test integration with MODWTTransform batch processing
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        int batchSize = 8;
        int signalLength = 128;
        double[][] signals = new double[batchSize][signalLength];
        
        Random random = new Random(42);
        for (int b = 0; b < batchSize; b++) {
            for (int t = 0; t < signalLength; t++) {
                signals[b][t] = random.nextGaussian();
            }
        }
        
        // Process batch through transform
        MODWTResult[] results = transform.forwardBatch(signals);
        
        assertNotNull(results);
        assertEquals(batchSize, results.length);
        
        for (int b = 0; b < batchSize; b++) {
            assertEquals(signalLength, results[b].approximationCoeffs().length);
            assertEquals(signalLength, results[b].detailCoeffs().length);
        }
    }
    
    @Test
    void testDB4SpecificPath() {
        // Test DB4 specific optimization path
        int batchSize = 16;
        int signalLength = 64;
        
        // Get DB4 wavelet
        com.morphiqlabs.wavelet.api.DiscreteWavelet db4 = 
            (com.morphiqlabs.wavelet.api.DiscreteWavelet) 
            com.morphiqlabs.wavelet.api.WaveletRegistry.getWavelet(
                com.morphiqlabs.wavelet.api.WaveletName.DB4);
        
        // DB4 filter (should trigger specific path if length == 4)
        int filterLength = db4.lowPassDecomposition().length;
        
        double[][] signals = new double[batchSize][signalLength];
        for (int b = 0; b < batchSize; b++) {
            for (int t = 0; t < signalLength; t++) {
                // Create varied signals
                signals[b][t] = Math.sin(2 * Math.PI * (b + 1) * t / signalLength) +
                                0.5 * Math.cos(4 * Math.PI * t / signalLength);
            }
        }
        
        // Convert to SoA
        double[] soaSignals = new double[batchSize * signalLength];
        double[] soaApprox = new double[batchSize * signalLength];
        double[] soaDetail = new double[batchSize * signalLength];
        
        BatchSIMDMODWT.convertToSoA(signals, soaSignals);
        
        // This should trigger the DB4-specific path
        BatchSIMDMODWT.batchMODWTSoA(soaSignals, soaApprox, soaDetail,
                                     db4, batchSize, signalLength);
        
        // Verify results are non-trivial
        double sumApprox = 0;
        double sumDetail = 0;
        for (int i = 0; i < soaApprox.length; i++) {
            sumApprox += Math.abs(soaApprox[i]);
            sumDetail += Math.abs(soaDetail[i]);
        }
        
        assertTrue(sumApprox > 0, "DB4 approximation should have non-zero values");
        assertTrue(sumDetail > 0, "DB4 detail should have non-zero values");
    }
    
    @Test
    void testGeneralWaveletPath() {
        // Test general wavelet path with DB6 (6-tap filter)
        int batchSize = 12;
        int signalLength = 48;
        
        com.morphiqlabs.wavelet.api.DiscreteWavelet db6 = 
            (com.morphiqlabs.wavelet.api.DiscreteWavelet) 
            com.morphiqlabs.wavelet.api.WaveletRegistry.getWavelet(
                com.morphiqlabs.wavelet.api.WaveletName.DB6);
        
        // DB6 filter (triggers general path)
        int filterLength = db6.lowPassDecomposition().length;
        
        double[][] signals = new double[batchSize][signalLength];
        Random random = new Random(123);
        for (int b = 0; b < batchSize; b++) {
            for (int t = 0; t < signalLength; t++) {
                signals[b][t] = random.nextGaussian() * 0.5;
            }
        }
        
        double[] soaSignals = new double[batchSize * signalLength];
        double[] soaApprox = new double[batchSize * signalLength];
        double[] soaDetail = new double[batchSize * signalLength];
        
        BatchSIMDMODWT.convertToSoA(signals, soaSignals);
        
        // This should trigger the general wavelet path
        BatchSIMDMODWT.batchMODWTSoA(soaSignals, soaApprox, soaDetail,
                                     db6, batchSize, signalLength);
        
        // Convert back and verify
        double[][] approx = new double[batchSize][signalLength];
        double[][] detail = new double[batchSize][signalLength];
        
        BatchSIMDMODWT.convertFromSoA(soaApprox, approx);
        BatchSIMDMODWT.convertFromSoA(soaDetail, detail);
        
        // Verify each signal was processed
        for (int b = 0; b < batchSize; b++) {
            boolean hasNonZero = false;
            for (int t = 0; t < signalLength; t++) {
                if (approx[b][t] != 0 || detail[b][t] != 0) {
                    hasNonZero = true;
                    break;
                }
            }
            assertTrue(hasNonZero, "Signal " + b + " should have been processed");
        }
    }
    
    @Test
    void testRemainderHandling() {
        // Test remainder handling with batch size = 5 (not a power of 2)
        // This will test the masked vector operations
        int batchSize = 5;
        int signalLength = 32;
        
        double[][] signals = new double[batchSize][signalLength];
        for (int b = 0; b < batchSize; b++) {
            for (int t = 0; t < signalLength; t++) {
                signals[b][t] = (b + 1) * (t + 1);
            }
        }
        
        double[] soaSignals = new double[batchSize * signalLength];
        double[] soaApprox = new double[batchSize * signalLength];
        double[] soaDetail = new double[batchSize * signalLength];
        
        BatchSIMDMODWT.convertToSoA(signals, soaSignals);
        
        // Test with DB4 to ensure remainder handling in DB4 path
        com.morphiqlabs.wavelet.api.DiscreteWavelet db4 = 
            (com.morphiqlabs.wavelet.api.DiscreteWavelet) 
            com.morphiqlabs.wavelet.api.WaveletRegistry.getWavelet(
                com.morphiqlabs.wavelet.api.WaveletName.DB4);
        
        BatchSIMDMODWT.batchMODWTSoA(soaSignals, soaApprox, soaDetail,
                                     db4, batchSize, signalLength);
        
        // Verify all signals were processed
        double[][] approx = new double[batchSize][signalLength];
        BatchSIMDMODWT.convertFromSoA(soaApprox, approx);
        
        // Check last signal (remainder part) was processed correctly
        double sumLastSignal = 0;
        for (int t = 0; t < signalLength; t++) {
            sumLastSignal += Math.abs(approx[batchSize - 1][t]);
        }
        assertTrue(sumLastSignal > 0, "Last signal in remainder should be processed");
    }

    @Test
    void testDB4SpecificOptimization() {
        // Use test wavelet with exactly 4 taps to trigger DB4 path
        DiscreteWavelet testDB4 = Daubechies.DB4;
        
        int batchSize = 16;
        int signalLength = 128;
        
        // Create SoA layout
        double[] soaSignals = new double[batchSize * signalLength];
        double[] soaApprox = new double[batchSize * signalLength];
        double[] soaDetail = new double[batchSize * signalLength];
        
        // Initialize with test data
        for (int t = 0; t < signalLength; t++) {
            for (int b = 0; b < batchSize; b++) {
                soaSignals[t * batchSize + b] = Math.cos(2 * Math.PI * t / signalLength) * (b + 1);
            }
        }
        
        // This should trigger the DB4 optimization path
        BatchSIMDMODWT.batchMODWTSoA(soaSignals, soaApprox, soaDetail,
                                     testDB4, batchSize, signalLength);
        
        // Verify results are non-zero
        double approxSum = 0;
        double detailSum = 0;
        for (int i = 0; i < soaApprox.length; i++) {
            approxSum += Math.abs(soaApprox[i]);
            detailSum += Math.abs(soaDetail[i]);
        }
        assertTrue(approxSum > 0, "DB4 approx should be non-zero");
        assertTrue(detailSum > 0, "DB4 detail should be non-zero");
    }

    @Test
    void testDB4WithRemainder() {
        // Use test wavelet with exactly 4 taps to trigger DB4 path
        DiscreteWavelet testDB4 = Daubechies.DB4;
        
        int oddBatchSize = 5;  // Not aligned to vector length for remainder path
        int signalLength = 32;
        
        // Create SoA layout
        double[] soaSignals = new double[oddBatchSize * signalLength];
        double[] soaApprox = new double[oddBatchSize * signalLength];
        double[] soaDetail = new double[oddBatchSize * signalLength];
        
        // Initialize with test data
        for (int t = 0; t < signalLength; t++) {
            for (int b = 0; b < oddBatchSize; b++) {
                soaSignals[t * oddBatchSize + b] = t * 0.1 + b;
            }
        }
        
        // This should trigger the DB4 optimization path with remainder
        BatchSIMDMODWT.batchMODWTSoA(soaSignals, soaApprox, soaDetail,
                                     testDB4, oddBatchSize, signalLength);
        
        // Verify results
        double approxSum = 0;
        double detailSum = 0;
        for (int i = 0; i < soaApprox.length; i++) {
            approxSum += Math.abs(soaApprox[i]);
            detailSum += Math.abs(soaDetail[i]);
        }
        assertTrue(approxSum > 0, "DB4 with remainder approx should be non-zero");
        assertTrue(detailSum > 0, "DB4 with remainder detail should be non-zero");
    }

    @Test
    void testBatchMODWTWithCleanup() {
        // Test the cleanup method for thread-local management
        int batchSize = 8;
        int signalLength = 64;
        
        double[] soaSignals = new double[batchSize * signalLength];
        double[] soaApprox = new double[batchSize * signalLength];
        double[] soaDetail = new double[batchSize * signalLength];
        
        // Initialize with test data
        for (int t = 0; t < signalLength; t++) {
            for (int b = 0; b < batchSize; b++) {
                soaSignals[t * batchSize + b] = Math.random();
            }
        }
        
        com.morphiqlabs.wavelet.api.DiscreteWavelet haar = 
            (com.morphiqlabs.wavelet.api.DiscreteWavelet) 
            com.morphiqlabs.wavelet.api.WaveletRegistry.getWavelet(
                com.morphiqlabs.wavelet.api.WaveletName.HAAR);
        
        // This should work with automatic cleanup
        BatchSIMDMODWT.batchMODWTWithCleanup(soaSignals, soaApprox, soaDetail,
                                              haar, batchSize, signalLength);
        
        // Verify results are computed
        double approxSum = 0;
        for (double val : soaApprox) {
            approxSum += Math.abs(val);
        }
        assertTrue(approxSum > 0, "Cleanup method should produce results");
    }
}
