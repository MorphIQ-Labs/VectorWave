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
        assertTrue(SimpleUtils.minCircularRMSE(signal, reconstructed) < 1e-10,
            "Single-level reconstruction RMSE too high (circular)");
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
        
        // Check reconstruction (circular RMSE)
        System.out.println("cRMSE: " + SimpleUtils.minCircularRMSE(signal, reconstructed));
        assertTrue(SimpleUtils.minCircularRMSE(signal, reconstructed) < 1e-10);
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
final class SimpleUtils {
    private SimpleUtils() {
        // Prevent instantiation
    }
    static double minCircularRMSE(double[] a, double[] b) {
        int n = a.length; double min = Double.POSITIVE_INFINITY;
        for (int k = 0; k < n; k++) {
            double s = 0.0;
            for (int i = 0; i < n; i++) {
                double d = a[i] - b[(i + k) % n]; s += d*d;
            }
            double r = Math.sqrt(s / n);
            if (r < min) min = r;
        }
        return min;
    }
}
