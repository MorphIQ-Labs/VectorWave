package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.performance.*;

/**
 * Demonstrates the empirical performance model calibration and adaptive estimation.
 * 
 * <p>This demo shows how the performance models learn from actual measurements
 * and provide increasingly accurate predictions.</p>
 */
public class PerformanceCalibrationDemo {
    
    public static void main(String[] args) {
        System.out.println("=== Performance Model Calibration Demo ===\n");
        
        // Detect platform characteristics
        PlatformFactors platform = PlatformFactors.detectPlatform();
        System.out.println("Detected Platform:");
        System.out.println(platform);
        System.out.println();
        
        // Run different demonstrations
        demonstrateCalibration();
        demonstrateAdaptiveLearning();
        demonstratePredictionAccuracy();
    }
    
    /**
     * Demonstrates full performance model calibration.
     */
    private static void demonstrateCalibration() {
        System.out.println("\n=== Full Calibration Demo ===");
        System.out.println("This calibrates performance models for the current platform.");
        System.out.println("Note: This may take a few minutes...\n");
        
        // Perform calibration
        PerformanceCalibrator calibrator = new PerformanceCalibrator();
        PerformanceCalibrator.CalibratedModels models = calibrator.calibrate();
        
        // Show model accuracy
        System.out.println("\nCalibration Results:");
        System.out.println("MODWT Model: " + models.modwtModel().getAccuracy());
        System.out.println("Convolution Model: " + models.convolutionModel().getAccuracy());
        System.out.println("Batch Model: " + models.batchModel().getAccuracy());
        
        // Save models for future use
        String modelFile = System.getProperty("user.home") + "/.vectorwave/performance/demo_models.dat";
        models.save(modelFile);
        System.out.println("\nModels saved to: " + modelFile);
    }
    
    /**
     * Demonstrates adaptive learning from actual measurements.
     */
    private static void demonstrateAdaptiveLearning() {
        System.out.println("\n=== Adaptive Learning Demo ===");
        System.out.println("Shows how predictions improve with more measurements.\n");
        
        MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        AdaptivePerformanceEstimator estimator = AdaptivePerformanceEstimator.getInstance();
        
        // Test sizes
        int[] testSizes = {256, 512, 1024, 2048, 4096};
        
        // Initial predictions
        System.out.println("Initial Predictions:");
        for (int size : testSizes) {
            MODWTTransform.ProcessingEstimate estimate = transform.estimateProcessingTime(size);
            System.out.printf("Size %5d: %s\n", size, estimate.description());
        }
        
        // Run transforms and let the system learn
        System.out.println("\nRunning transforms for learning...");
        for (int iter = 0; iter < 5; iter++) {
            for (int size : testSizes) {
                double[] signal = generateSignal(size);
                
                // This will automatically record measurements
                transform.forward(signal);
            }
        }
        
        // Updated predictions after learning
        System.out.println("\nUpdated Predictions (after learning):");
        for (int size : testSizes) {
            MODWTTransform.ProcessingEstimate estimate = transform.estimateProcessingTime(size);
            System.out.printf("Size %5d: %s\n", size, estimate.description());
        }
        
        // Show accuracy report
        System.out.println("\n" + estimator.getAccuracyReport());
    }
    
    /**
     * Demonstrates prediction accuracy across different scenarios.
     */
    private static void demonstratePredictionAccuracy() {
        System.out.println("\n=== Prediction Accuracy Demo ===");
        System.out.println("Compares predictions with actual measurements.\n");
        
        AdaptivePerformanceEstimator estimator = AdaptivePerformanceEstimator.getInstance();
        
        // Test different wavelets and sizes
        testPredictionAccuracy("Haar", new Haar());
        testPredictionAccuracy("Daubechies-4", Daubechies.DB4);
    }
    
    private static void testPredictionAccuracy(String waveletName, 
                                             com.morphiqlabs.wavelet.api.Wavelet wavelet) {
        System.out.println("\nTesting " + waveletName + " wavelet:");
        System.out.println("Size    | Predicted  | Actual     | Error  | Within CI?");
        System.out.println("--------|------------|------------|--------|------------");
        
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        AdaptivePerformanceEstimator estimator = AdaptivePerformanceEstimator.getInstance();
        
        int[] sizes = {777, 1337, 3333, 7777};
        
        for (int size : sizes) {
            // Get prediction
            PredictionResult prediction = estimator.estimateMODWT(
                size, waveletName, 
                com.morphiqlabs.wavelet.WaveletOperations.getPerformanceInfo().vectorizationEnabled()
            );
            
            // Measure actual time
            double[] signal = generateSignal(size);
            
            // Warm up
            for (int i = 0; i < 10; i++) {
                transform.forward(signal);
            }
            
            // Measure
            long startTime = System.nanoTime();
            int iterations = 20;
            for (int i = 0; i < iterations; i++) {
                transform.forward(signal);
            }
            long endTime = System.nanoTime();
            
            double actualTimeMs = (endTime - startTime) / (iterations * 1_000_000.0);
            
            // Check if within confidence interval
            boolean withinCI = actualTimeMs >= prediction.lowerBound() && 
                              actualTimeMs <= prediction.upperBound();
            
            // Calculate error percentage
            double errorPct = Math.abs(actualTimeMs - prediction.estimatedTime()) / 
                             actualTimeMs * 100;
            
            System.out.printf("%7d | %10.3f | %10.3f | %6.1f%% | %s\n",
                size, prediction.estimatedTime(), actualTimeMs, errorPct,
                withinCI ? "Yes" : "No");
        }
    }
    
    /**
     * Generates a test signal with mixed frequencies.
     */
    private static double[] generateSignal(int size) {
        double[] signal = new double[size];
        for (int i = 0; i < size; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / size) * 10 +
                       Math.sin(8 * Math.PI * i / size) * 5 +
                       Math.random() * 0.5;
        }
        return signal;
    }
}
