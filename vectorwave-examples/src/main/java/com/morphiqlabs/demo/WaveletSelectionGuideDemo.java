package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;
import java.util.*;

/**
 * Interactive guide for selecting the appropriate wavelet for different applications with MODWT.
 *
 * <p>This demo helps users understand:
 * <ul>
 *   <li>Characteristics of different wavelet families</li>
 *   <li>How to match wavelets to signal properties</li>
 *   <li>Trade-offs between different wavelets</li>
 *   <li>Visual comparison of wavelet behaviors with MODWT</li>
 * </ul>
 */
public class WaveletSelectionGuideDemo {

    public static void main(String[] args) {
        System.out.println("=== VectorWave Wavelet Selection Guide (MODWT) ===\n");

        // Demo 1: Wavelet characteristics overview
        demonstrateWaveletCharacteristics();

        // Demo 2: Signal type matching
        demonstrateSignalTypeMatching();

        // Demo 3: Visual wavelet comparison
        demonstrateVisualComparison();

        // Demo 4: Application-specific recommendations
        demonstrateApplicationRecommendations();

        // Demo 5: Performance vs accuracy trade-offs
        demonstrateTradeoffs();
    }

    private static void demonstrateWaveletCharacteristics() {
        System.out.println("1. Wavelet Family Characteristics");
        System.out.println("---------------------------------\n");

        System.out.println("HAAR WAVELET");
        System.out.println("  Properties: Discontinuous, compact support");
        System.out.println("  Best for: Sharp transitions, edge detection");
        System.out.println("  Advantages: Fastest computation, simple");
        System.out.println("  Disadvantages: Poor frequency localization");
        System.out.println("  Vanishing moments: 1");
        printWaveletCoefficients(new Haar());

        System.out.println("\nDAUBECHIES WAVELETS (DB2, DB4, ...)");
        System.out.println("  Properties: Smooth, compact support");
        System.out.println("  Best for: General purpose, smooth signals");
        System.out.println("  Advantages: Good time-frequency localization");
        System.out.println("  Disadvantages: Slightly asymmetric");
        System.out.println("  Vanishing moments: N (for DBN)");
        printWaveletCoefficients(Daubechies.DB4);

        System.out.println("\nSYMLET WAVELETS (SYM2, SYM3, ...)");
        System.out.println("  Properties: Nearly symmetric, smooth");
        System.out.println("  Best for: Signal/image processing");
        System.out.println("  Advantages: Better symmetry than Daubechies");
        System.out.println("  Disadvantages: Slightly longer filters");
        System.out.println("  Vanishing moments: N (for SYMN)");
        printWaveletCoefficients(Symlet.SYM3);

        System.out.println("\nCOIFLET WAVELETS (COIF1, COIF2, ...)");
        System.out.println("  Properties: Near-symmetric, smooth");
        System.out.println("  Best for: High-precision applications");
        System.out.println("  Advantages: Vanishing moments for scaling function too");
        System.out.println("  Disadvantages: Longer computation time");
        System.out.println("  Vanishing moments: 2N (for COIFN)");
        printWaveletCoefficients(Coiflet.COIF1);

        System.out.println("\nBIORTHOGONAL WAVELETS");
        System.out.println("  Properties: Symmetric, linear phase");
        System.out.println("  Best for: Image compression (JPEG2000)");
        System.out.println("  Advantages: Perfect reconstruction, symmetric");
        System.out.println("  Disadvantages: Different analysis/synthesis filters");
        printBiorthogonalCoefficients(BiorthogonalSpline.BIOR1_3);

        System.out.println("\nMORLET WAVELET (Continuous)");
        System.out.println("  Properties: Complex-valued, Gaussian envelope");
        System.out.println("  Best for: Time-frequency analysis");
        System.out.println("  Advantages: Excellent frequency resolution");
        System.out.println("  Disadvantages: No compact support");
        System.out.println("  Note: Use CWT for Morlet, not MODWT\n");
    }

    private static void demonstrateSignalTypeMatching() {
        System.out.println("2. Matching Wavelets to Signal Types");
        System.out.println("------------------------------------\n");

        // Test different signal types
        Map<String, double[]> testSignals = createTestSignals();

        for (Map.Entry<String, double[]> entry : testSignals.entrySet()) {
            String signalType = entry.getKey();
            double[] signal = entry.getValue();

            System.out.println("Signal Type: " + signalType);
            System.out.println("Testing different wavelets...");

            // Test wavelets
            Wavelet[] wavelets = {
                new Haar(),
                Daubechies.DB2,
                Daubechies.DB4,
                Symlet.SYM3,
                Coiflet.COIF1
            };

            double bestScore = Double.MAX_VALUE;
            Wavelet bestWavelet = null;

            for (Wavelet wavelet : wavelets) {
                double score = evaluateWaveletForSignal(wavelet, signal);
                System.out.printf("  %-10s: score = %.4f\n", 
                    wavelet.getClass().getSimpleName(), score);
                
                if (score < bestScore) {
                    bestScore = score;
                    bestWavelet = wavelet;
                }
            }

            System.out.println("  Best match: " + bestWavelet.getClass().getSimpleName());
            System.out.println();
        }
    }

    private static void demonstrateVisualComparison() {
        System.out.println("3. Visual Wavelet Comparison");
        System.out.println("----------------------------\n");

        // Create a test signal with multiple features
        double[] testSignal = createComplexTestSignal(256);

        System.out.println("Analyzing complex test signal with different wavelets:");
        System.out.println("(Lower reconstruction error = better match)\n");

        Wavelet[] wavelets = {
            new Haar(),
            Daubechies.DB2,
            Daubechies.DB4,
            Symlet.SYM4,
            Coiflet.COIF2
        };

        System.out.println("Wavelet    | Recon Error | Compression (90%) | Time (μs)");
        System.out.println("-----------|-------------|-------------------|----------");

        for (Wavelet wavelet : wavelets) {
            analyzeWaveletPerformance(wavelet, testSignal);
        }
        System.out.println();
    }

    private static void demonstrateApplicationRecommendations() {
        System.out.println("4. Application-Specific Recommendations");
        System.out.println("---------------------------------------\n");

        System.out.println("FINANCIAL TIME SERIES:");
        System.out.println("  Recommended: DB4, SYM4");
        System.out.println("  Reason: Good balance of smoothness and time localization");
        System.out.println("  MODWT advantage: Shift-invariant for time series alignment\n");

        System.out.println("AUDIO SIGNAL PROCESSING:");
        System.out.println("  Recommended: DB6-DB10, COIF2-COIF3");
        System.out.println("  Reason: Better frequency resolution");
        System.out.println("  MODWT advantage: Works with any audio frame length\n");

        System.out.println("EDGE DETECTION:");
        System.out.println("  Recommended: Haar, DB2");
        System.out.println("  Reason: Sharp transitions in coefficients");
        System.out.println("  MODWT advantage: Preserves edge timing information\n");

        System.out.println("DENOISING:");
        System.out.println("  Recommended: SYM4-SYM8, COIF2-COIF3");
        System.out.println("  Reason: Smoother wavelets reduce artifacts");
        System.out.println("  MODWT advantage: Better noise statistics estimation\n");

        System.out.println("COMPRESSION:");
        System.out.println("  Recommended: BIOR wavelets, DB4-DB6");
        System.out.println("  Reason: Good energy compaction");
        System.out.println("  Note: DWT typically better than MODWT for compression\n");
    }

    private static void demonstrateTradeoffs() {
        System.out.println("5. Performance vs Accuracy Trade-offs");
        System.out.println("-------------------------------------\n");

        int signalSize = 1024;
        double[] signal = createComplexTestSignal(signalSize);

        System.out.println("Filter Length | Wavelet | Time (ms) | Accuracy | Energy Preserved");
        System.out.println("--------------|---------|-----------|----------|------------------");

        testWaveletTradeoff(new Haar(), signal, "Haar (2)");
        testWaveletTradeoff(Daubechies.DB2, signal, "DB2 (4)");
        testWaveletTradeoff(Daubechies.DB4, signal, "DB4 (8)");
        testWaveletTradeoff(Daubechies.DB8, signal, "DB8 (16)");
        testWaveletTradeoff(Coiflet.COIF3, signal, "COIF3 (18)");

        System.out.println("\nKey Insights:");
        System.out.println("- Shorter filters = faster computation");
        System.out.println("- Longer filters = better frequency localization");
        System.out.println("- MODWT preserves energy perfectly for all wavelets");
        System.out.println("- Choose based on your speed vs accuracy requirements");
    }

    // Helper methods

    private static void printWaveletCoefficients(Wavelet wavelet) {
        double[] lowPass = wavelet.lowPassDecomposition();
        double[] highPass = wavelet.highPassDecomposition();
        
        System.out.printf("  Low-pass filter length: %d\n", lowPass.length);
        System.out.printf("  High-pass filter length: %d\n", highPass.length);
    }

    private static void printBiorthogonalCoefficients(BiorthogonalWavelet wavelet) {
        System.out.printf("  Decomposition filters: %d, %d\n", 
            wavelet.lowPassDecomposition().length,
            wavelet.highPassDecomposition().length);
        System.out.printf("  Reconstruction filters: %d, %d\n",
            wavelet.lowPassReconstruction().length,
            wavelet.highPassReconstruction().length);
    }

    private static Map<String, double[]> createTestSignals() {
        Map<String, double[]> signals = new LinkedHashMap<>();
        
        // Step signal
        double[] step = new double[128];
        Arrays.fill(step, 0, 64, 1.0);
        Arrays.fill(step, 64, 128, -1.0);
        signals.put("Step/Edge", step);
        
        // Smooth signal
        double[] smooth = new double[128];
        for (int i = 0; i < 128; i++) {
            smooth[i] = Math.sin(2 * Math.PI * i / 32);
        }
        signals.put("Smooth/Sinusoidal", smooth);
        
        // Noisy signal
        double[] noisy = new double[128];
        Random rand = new Random(42);
        for (int i = 0; i < 128; i++) {
            noisy[i] = Math.sin(2 * Math.PI * i / 32) + 0.3 * rand.nextGaussian();
        }
        signals.put("Noisy", noisy);
        
        return signals;
    }

    private static double[] createComplexTestSignal(int length) {
        double[] signal = new double[length];
        for (int i = 0; i < length; i++) {
            // Multiple frequency components
            signal[i] = Math.sin(2 * Math.PI * i / 64) +      // Low freq
                       0.5 * Math.sin(2 * Math.PI * i / 16) +  // Med freq
                       0.25 * Math.sin(2 * Math.PI * i / 4);   // High freq
            
            // Add a discontinuity
            if (i > length/2 && i < length/2 + 10) {
                signal[i] += 2.0;
            }
        }
        return signal;
    }

    private static double evaluateWaveletForSignal(Wavelet wavelet, double[] signal) {
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        MODWTResult result = transform.forward(signal);
        
        // Simple sparsity measure: count small coefficients
        double threshold = 0.01;
        int sparseCount = 0;
        
        for (double coeff : result.detailCoeffs()) {
            if (Math.abs(coeff) < threshold) sparseCount++;
        }
        
        // Return inverse sparsity (lower is better)
        return (double)(result.detailCoeffs().length - sparseCount) / result.detailCoeffs().length;
    }

    private static void analyzeWaveletPerformance(Wavelet wavelet, double[] signal) {
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        // Time the transform
        long startTime = System.nanoTime();
        MODWTResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        long endTime = System.nanoTime();
        
        // Calculate reconstruction error
        double error = 0;
        for (int i = 0; i < signal.length; i++) {
            error += Math.pow(signal[i] - reconstructed[i], 2);
        }
        error = Math.sqrt(error / signal.length);
        
        // Simulate compression by zeroing small coefficients
        double[] details = result.detailCoeffs().clone();
        Arrays.sort(details);
        double threshold = Math.abs(details[(int)(details.length * 0.1)]); // Keep top 90%
        
        int compressed = 0;
        for (double d : result.detailCoeffs()) {
            if (Math.abs(d) < threshold) compressed++;
        }
        double compressionRatio = (double)compressed / details.length * 100;
        
        double microseconds = (endTime - startTime) / 1000.0;
        
        System.out.printf("%-10s | %.2e | %6.1f%%      | %8.1f\n",
            wavelet.getClass().getSimpleName(),
            error, compressionRatio, microseconds);
    }

    private static void testWaveletTradeoff(Wavelet wavelet, double[] signal, String name) {
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        // Measure time
        long totalTime = 0;
        int runs = 100;
        for (int i = 0; i < runs; i++) {
            long start = System.nanoTime();
            MODWTResult result = transform.forward(signal);
            double[] recon = transform.inverse(result);
            totalTime += System.nanoTime() - start;
        }
        double avgMs = totalTime / (runs * 1_000_000.0);
        
        // Measure accuracy (one run)
        MODWTResult result = transform.forward(signal);
        double[] reconstructed = transform.inverse(result);
        
        double maxError = 0;
        double signalEnergy = 0;
        double transformEnergy = 0;
        
        for (int i = 0; i < signal.length; i++) {
            maxError = Math.max(maxError, Math.abs(signal[i] - reconstructed[i]));
            signalEnergy += signal[i] * signal[i];
        }
        
        for (double a : result.approximationCoeffs()) transformEnergy += a * a;
        for (double d : result.detailCoeffs()) transformEnergy += d * d;
        
        double energyRatio = transformEnergy / signalEnergy;
        
        System.out.printf("%-13s | %-7s | %9.3f | %.2e | %.6f\n",
            name.substring(0, Math.min(13, name.length())),
            name.substring(name.indexOf('(') + 1, name.indexOf(')')),
            avgMs, maxError, energyRatio);
    }
}