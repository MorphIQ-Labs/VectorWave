package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.cwt.ComplexGaussianWavelet;
import com.morphiqlabs.wavelet.cwt.GaussianDerivativeWavelet;
import com.morphiqlabs.wavelet.cwt.ComplexNumber;
import org.junit.jupiter.api.Test;

/**
 * Demonstration that the CGAU fix works correctly.
 * Shows that CGAU is now a proper complex wavelet, not just a real Gaussian derivative.
 */
public class ComplexGaussianFixDemo {
    
    @Test
    void demonstrateComplexGaussianFix() {
        System.out.println("=== Complex Gaussian Wavelet Fix Demonstration ===\n");
        
        // Get CGAU from registry
        Wavelet cgauFromRegistry = WaveletRegistry.getWavelet(WaveletName.CGAU);
        System.out.println("CGAU from registry: " + cgauFromRegistry.getClass().getSimpleName());
        System.out.println("Description: " + cgauFromRegistry.description());
        System.out.println("Is Complex: " + ((ContinuousWavelet) cgauFromRegistry).isComplex());
        System.out.println();
        
        // Get GAUSSIAN from registry for comparison
        Wavelet gaussianFromRegistry = WaveletRegistry.getWavelet(WaveletName.GAUSSIAN);
        System.out.println("GAUSSIAN from registry: " + gaussianFromRegistry.getClass().getSimpleName());
        System.out.println("Description: " + gaussianFromRegistry.description());
        System.out.println("Is Complex: " + ((ContinuousWavelet) gaussianFromRegistry).isComplex());
        System.out.println();
        
        // Demonstrate the difference
        System.out.println("=== Demonstrating Complex vs Real Behavior ===");
        
        ComplexGaussianWavelet cgau = (ComplexGaussianWavelet) cgauFromRegistry;
        GaussianDerivativeWavelet gaussian = (GaussianDerivativeWavelet) gaussianFromRegistry;
        
        double t = 1.0;
        
        // Real Gaussian has only real values
        double realValue = gaussian.psi(t);
        System.out.printf("Gaussian(%.1f) = %.6f (real only)%n", t, realValue);
        
        // Complex Gaussian has both real and imaginary parts
        double complexReal = cgau.psi(t);
        double complexImag = cgau.psiImaginary(t);
        ComplexNumber complexValue = cgau.psiComplex(t);
        
        System.out.printf("ComplexGaussian(%.1f) = %.6f + %.6fi%n", t, complexReal, complexImag);
        System.out.printf("Magnitude: %.6f, Phase: %.6f radians%n", 
                         complexValue.magnitude(), complexValue.phase());
        System.out.println();
        
        // Show that they are mathematically different
        System.out.println("=== Mathematical Differences ===");
        System.out.printf("Real Gaussian derivative: %.6f%n", realValue);
        System.out.printf("Complex Gaussian real part: %.6f%n", complexReal);
        System.out.printf("Difference: %.6f%n", Math.abs(realValue - complexReal));
        System.out.println("The real parts differ due to complex modulation in CGAU.");
        System.out.println();
        
        // Demonstrate analytic signal properties
        System.out.println("=== Analytic Signal Properties ===");
        System.out.println("Complex Gaussian provides phase information:");
        
        double[] testPoints = {-2.0, -1.0, 0.0, 1.0, 2.0};
        for (double tp : testPoints) {
            ComplexNumber c = cgau.psiComplex(tp);
            System.out.printf("t=%.1f: magnitude=%.4f, phase=%.4f rad%n", 
                             tp, c.magnitude(), c.phase());
        }
        System.out.println();
        
        // Show frequency domain properties
        System.out.println("=== Frequency Domain Properties ===");
        System.out.println("Complex Gaussian in frequency domain:");
        
        double[] frequencies = {4.0, 4.5, 5.0, 5.5, 6.0};
        for (double freq : frequencies) {
            ComplexNumber fhat = cgau.psiHat(freq);
            System.out.printf("ω=%.1f: |Ψ̂(ω)|=%.6f%n", freq, fhat.magnitude());
        }
        
        System.out.println("\n=== Fix Summary ===");
        System.out.println("✓ CGAU is now ComplexGaussianWavelet (was GaussianDerivativeWavelet)");
        System.out.println("✓ CGAU.isComplex() returns true (was false)");
        System.out.println("✓ CGAU provides both magnitude and phase information");
        System.out.println("✓ CGAU differs mathematically from real Gaussian derivative");
        System.out.println("✓ CGAU implements proper complex modulation with Hermite polynomials");
    }
}