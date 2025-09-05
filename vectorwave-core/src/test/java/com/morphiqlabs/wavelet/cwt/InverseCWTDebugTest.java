package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Debug tests for Inverse CWT to understand reconstruction issues.
 */
class InverseCWTDebugTest {
    
    @Test
    @DisplayName("Debug admissibility constant")
    void testAdmissibilityConstant() {
        // Test with Morlet wavelet
        ContinuousWavelet morlet = new MorletWavelet();
        InverseCWT inverseCWT = new InverseCWT(morlet);
        
        double admissibility = inverseCWT.getAdmissibilityConstant();
        System.out.println("Morlet admissibility constant: " + admissibility);
        
        assertTrue(admissibility > 0, "Admissibility constant should be positive");
        assertTrue(admissibility < 10, "Admissibility constant seems too large: " + admissibility);
    }
    
    @Test
    @DisplayName("Debug simple reconstruction")
    void testSimpleReconstruction() {
        // Very simple test - delta function
        int N = 128;
        double[] signal = new double[N];
        signal[N/2] = 1.0; // Delta at center
        
        ContinuousWavelet morlet = new MorletWavelet();
        CWTTransform cwt = new CWTTransform(morlet);
        InverseCWT inverseCWT = new InverseCWT(morlet);
        
        // Use only a few scales
        double[] scales = {4, 8, 16, 32};
        
        CWTResult result = cwt.analyze(signal, scales);
        
        // Check coefficients
        System.out.println("\nCWT Coefficients (sample):");
        for (int s = 0; s < scales.length; s++) {
            System.out.printf("Scale %.1f: ", scales[s]);
            double[] coeffs = result.getCoefficients()[s];
            double max = 0;
            for (double c : coeffs) {
                max = Math.max(max, Math.abs(c));
            }
            System.out.printf("max=%.4f\n", max);
        }
        
        // Try reconstruction
        double[] reconstructed = inverseCWT.reconstruct(result);
        
        // Find peak in reconstructed signal
        int peakIdx = 0;
        double peakVal = reconstructed[0];
        for (int i = 1; i < N; i++) {
            if (Math.abs(reconstructed[i]) > Math.abs(peakVal)) {
                peakVal = reconstructed[i];
                peakIdx = i;
            }
        }
        
        System.out.println("\nReconstruction:");
        System.out.println("Peak at index: " + peakIdx + " (expected: " + (N/2) + ")");
        System.out.println("Peak value: " + peakVal + " (expected: ~1.0)");
        System.out.println("Mean value: " + calculateMean(reconstructed));
        System.out.println("Total energy: " + calculateEnergy(reconstructed));
    }
    
    @Test
    @DisplayName("Test reconstruction formula components")
    void testReconstructionComponents() {
        // Test individual components of reconstruction
        int N = 64;
        double[] signal = new double[N];
        
        // Simple sinusoid
        for (int i = 0; i < N; i++) {
            signal[i] = Math.sin(2 * Math.PI * 5 * i / N);
        }
        
        MorletWavelet morlet = new MorletWavelet();
        CWTTransform cwt = new CWTTransform(morlet);
        InverseCWT inverseCWT = new InverseCWT(morlet);
        
        double[] scales = {8, 16, 32};
        CWTResult result = cwt.analyze(signal, scales);
        
        // Check scale integration weights
        System.out.println("\nScale weights:");
        for (int i = 0; i < scales.length - 1; i++) {
            double da = scales[i+1] - scales[i];
            System.out.printf("da[%d] = %.4f\n", i, da);
        }
        
        // Check wavelet evaluation
        System.out.println("\nWavelet values at t=0:");
        for (double scale : scales) {
            double psi = morlet.psi(0);
            System.out.printf("ψ(0) at scale %.1f = %.4f\n", scale, psi);
        }
        
        // Check coefficient magnitudes
        System.out.println("\nCoefficient statistics:");
        double[][] coeffs = result.getCoefficients();
        for (int s = 0; s < scales.length; s++) {
            double sum = 0, max = 0;
            for (double c : coeffs[s]) {
                sum += Math.abs(c);
                max = Math.max(max, Math.abs(c));
            }
            System.out.printf("Scale %.1f: sum=%.4f, max=%.4f, avg=%.4f\n", 
                scales[s], sum, max, sum/N);
        }
    }
    
    private double calculateMean(double[] signal) {
        double sum = 0;
        for (double v : signal) {
            sum += v;
        }
        return sum / signal.length;
    }
    
    private double calculateEnergy(double[] signal) {
        double sum = 0;
        for (double v : signal) {
            sum += v * v;
        }
        return sum;
    }
}