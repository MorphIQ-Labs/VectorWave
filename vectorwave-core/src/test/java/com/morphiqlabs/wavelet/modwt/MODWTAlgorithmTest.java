package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.Haar;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify MODWT algorithm implementation details.
 */
class MODWTAlgorithmTest {
    
    @Test
    void testMODWTFilterScaling() {
        Haar haar = new Haar();
        double[] h = haar.lowPassDecomposition();
        double[] g = haar.highPassDecomposition();
        
        System.out.println("Original Haar filters:");
        System.out.println("h = " + java.util.Arrays.toString(h));
        System.out.println("g = " + java.util.Arrays.toString(g));
        
        // At level 1, MODWT scales by 1/sqrt(2)
        double scale1 = 1.0 / Math.sqrt(2.0);
        System.out.println("\nLevel 1 scale: " + scale1);
        
        // At level 2, MODWT scales by 1/2
        double scale2 = 1.0 / 2.0;
        System.out.println("Level 2 scale: " + scale2);
        
        // Test manual MODWT computation
        double[] signal = {1, 2, 3, 4};
        double[] w1 = manualMODWT(signal, h, g, 1);
        System.out.println("\nLevel 1 MODWT of [1,2,3,4]:");
        System.out.println("Wavelet coeffs: " + java.util.Arrays.toString(w1));
        
        // Manual reconstruction
        double[] recon = manualInverseMODWT(w1, h, g, 1, signal.length);
        System.out.println("Reconstructed: " + java.util.Arrays.toString(recon));
    }
    
    /**
     * Manual MODWT implementation for testing.
     */
    private double[] manualMODWT(double[] signal, double[] h, double[] g, int level) {
        int N = signal.length;
        double[] result = new double[N * 2]; // Both W and V coefficients
        
        // Scale filters for MODWT
        double scale = Math.pow(2, -level/2.0);
        double[] h_scaled = new double[h.length];
        double[] g_scaled = new double[g.length];
        for (int i = 0; i < h.length; i++) {
            h_scaled[i] = h[i] * scale;
            g_scaled[i] = g[i] * scale;
        }
        
        // Apply circular convolution (no downsampling)
        for (int t = 0; t < N; t++) {
            double w_t = 0; // Detail coefficient
            double v_t = 0; // Approximation coefficient
            
            for (int k = 0; k < h.length; k++) {
                int idx = (t - k + N) % N;
                v_t += h_scaled[k] * signal[idx];
                w_t += g_scaled[k] * signal[idx];
            }
            
            result[t] = w_t; // Details first
            result[t + N] = v_t; // Then approximations
        }
        
        return result;
    }
    
    /**
     * Manual inverse MODWT for testing.
     */
    private double[] manualInverseMODWT(double[] coeffs, double[] h, double[] g, int level, int N) {
        double[] result = new double[N];
        
        // Extract W and V coefficients
        double[] W = new double[N];
        double[] V = new double[N];
        for (int i = 0; i < N; i++) {
            W[i] = coeffs[i];
            V[i] = coeffs[i + N];
        }
        
        // Scale filters for reconstruction
        double scale = Math.pow(2, -level/2.0);
        double[] h_scaled = new double[h.length];
        double[] g_scaled = new double[g.length];
        for (int i = 0; i < h.length; i++) {
            h_scaled[i] = h[i] * scale;
            g_scaled[i] = g[i] * scale;
        }
        
        // Reconstruct using both V and W
        for (int t = 0; t < N; t++) {
            double sum = 0;
            for (int k = 0; k < h.length; k++) {
                int idx = (t + k) % N; // Note: different indexing for reconstruction
                sum += h_scaled[k] * V[idx] + g_scaled[k] * W[idx];
            }
            result[t] = sum;
        }
        
        return result;
    }
    
    @Test
    void testMultiLevelMODWT() {
        double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
        Haar haar = new Haar();
        
        System.out.println("\nMulti-level MODWT test:");
        System.out.println("Original signal: " + java.util.Arrays.toString(signal));
        
        // Level 1
        double[] level1 = manualMODWT(signal, haar.lowPassDecomposition(), 
                                     haar.highPassDecomposition(), 1);
        double[] V1 = new double[8];
        double[] W1 = new double[8];
        for (int i = 0; i < 8; i++) {
            W1[i] = level1[i];
            V1[i] = level1[i + 8];
        }
        System.out.println("\nLevel 1:");
        System.out.println("W1 (details): " + java.util.Arrays.toString(W1));
        System.out.println("V1 (approx): " + java.util.Arrays.toString(V1));
        
        // Level 2 - apply to V1
        // For level 2, we need to upsample the filter by inserting zeros
        double[] h2 = upsampleFilter(haar.lowPassDecomposition(), 2);
        double[] g2 = upsampleFilter(haar.highPassDecomposition(), 2);
        
        double[] level2 = manualMODWT(V1, h2, g2, 2);
        double[] W2 = new double[8];
        double[] V2 = new double[8];
        for (int i = 0; i < 8; i++) {
            W2[i] = level2[i];
            V2[i] = level2[i + 8];
        }
        System.out.println("\nLevel 2:");
        System.out.println("W2 (details): " + java.util.Arrays.toString(W2));
        System.out.println("V2 (approx): " + java.util.Arrays.toString(V2));
    }
    
    private double[] upsampleFilter(double[] filter, int level) {
        int factor = (int) Math.pow(2, level - 1);
        int newLength = (filter.length - 1) * factor + 1;
        double[] upsampled = new double[newLength];
        
        for (int i = 0; i < filter.length; i++) {
            upsampled[i * factor] = filter[i];
        }
        
        return upsampled;
    }
    
    @Test
    void testPerfectReconstructionCondition() {
        Haar haar = new Haar();
        double[] h = haar.lowPassDecomposition();
        double[] g = haar.highPassDecomposition();
        
        // For perfect reconstruction, we need:
        // sum(h[k] * h[k]) + sum(g[k] * g[k]) = 2
        double sumH = 0, sumG = 0;
        for (int i = 0; i < h.length; i++) {
            sumH += h[i] * h[i];
            sumG += g[i] * g[i];
        }
        
        System.out.println("\nPerfect reconstruction condition:");
        System.out.println("sum(h^2) = " + sumH);
        System.out.println("sum(g^2) = " + sumG);
        System.out.println("Total = " + (sumH + sumG));
        
        assertEquals(2.0, sumH + sumG, 1e-10);
    }
}