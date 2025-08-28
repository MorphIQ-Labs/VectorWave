package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple tests to debug MODWT implementation issues.
 */
class SimpleMODWTTest {
    
    @Test
    void testSingleLevelMODWT() {
        // Test single-level MODWT first
        MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        double[] signal = {1, 2, 3, 4};
        MODWTResult result = modwt.forward(signal);
        
        System.out.println("Signal: [1, 2, 3, 4]");
        System.out.println("Approx: " + java.util.Arrays.toString(result.approximationCoeffs()));
        System.out.println("Detail: " + java.util.Arrays.toString(result.detailCoeffs()));
        
        double[] reconstructed = modwt.inverse(result);
        System.out.println("Reconstructed: " + java.util.Arrays.toString(reconstructed));
        
        // Check reconstruction
        for (int i = 0; i < signal.length; i++) {
            assertEquals(signal[i], reconstructed[i], 1e-10,
                "Single-level reconstruction failed at index " + i);
        }
    }
    
    @Test
    void testTwoLevelMODWT() {
        // Test two-level MODWT
        MultiLevelMODWTTransform mwt = new MultiLevelMODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        MultiLevelMODWTResult result = mwt.decompose(signal, 2);
        
        System.out.println("\nSignal: [1, 2, 3, 4, 5, 6, 7, 8]");
        System.out.println("Level 1 Detail: " + java.util.Arrays.toString(result.getDetailCoeffsAtLevel(1)));
        System.out.println("Level 2 Detail: " + java.util.Arrays.toString(result.getDetailCoeffsAtLevel(2)));
        System.out.println("Approximation: " + java.util.Arrays.toString(result.getApproximationCoeffs()));
        
        double[] reconstructed = mwt.reconstruct(result);
        System.out.println("Reconstructed: " + java.util.Arrays.toString(reconstructed));
        
        // Check reconstruction
        for (int i = 0; i < signal.length; i++) {
            double diff = Math.abs(signal[i] - reconstructed[i]);
            System.out.println("Index " + i + ": expected=" + signal[i] + 
                             ", actual=" + reconstructed[i] + ", diff=" + diff);
        }
    }
    
    @Test
    void testHaarFilters() {
        Haar haar = new Haar();
        System.out.println("\nHaar Filters:");
        System.out.println("Low-pass decomp: " + java.util.Arrays.toString(haar.lowPassDecomposition()));
        System.out.println("High-pass decomp: " + java.util.Arrays.toString(haar.highPassDecomposition()));
        System.out.println("Low-pass recon: " + java.util.Arrays.toString(haar.lowPassReconstruction()));
        System.out.println("High-pass recon: " + java.util.Arrays.toString(haar.highPassReconstruction()));
    }
}