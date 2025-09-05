package com.morphiqlabs.wavelet.performance;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Adaptive performance estimator that learns from actual execution times.
 * 
 * <p>This class provides performance predictions that improve over time by
 * learning from actual measurements. It automatically recalibrates when
 * the model accuracy degrades.</p>
 */
public class AdaptivePerformanceEstimator {
    private static final System.Logger LOG = com.morphiqlabs.wavelet.util.Logging.getLogger(AdaptivePerformanceEstimator.class);
    
    private static final String MODEL_CACHE_DIR = ".vectorwave/performance";
    private static final String MODEL_FILE = "performance_models.dat";
    private static final int RECALIBRATION_THRESHOLD = 1000; // Measurements before checking
    private static final double ACCURACY_THRESHOLD = 0.85; // Minimum acceptable accuracy
    
    // Opt-in flags (default disabled)
    private final boolean calibrationEnabled;
    private final boolean persistenceEnabled;

    // Singleton instance
    private static final AdaptivePerformanceEstimator INSTANCE = new AdaptivePerformanceEstimator();
    
    private PerformanceCalibrator.CalibratedModels models;
    private final ConcurrentHashMap<String, PerformanceModel> operationModels;
    private final AtomicInteger measurementCount;
    private final AtomicLong lastCalibrationTime;
    private final ExecutorService calibrationExecutor;
    private final AtomicBoolean isCalibrating;
    
    private AdaptivePerformanceEstimator() {
        this.calibrationEnabled = getBoolean(
            "vectorwave.perf.calibration", "vectorwave.perf.calibration.enabled",
            "VECTORWAVE_PERF_CALIBRATION", "VECTORWAVE_PERF_CALIBRATION_ENABLED",
            false
        );
        this.persistenceEnabled = getBoolean(
            "vectorwave.perf.persist", null,
            "VECTORWAVE_PERF_PERSIST", null,
            false
        );
        this.operationModels = new ConcurrentHashMap<>();
        this.measurementCount = new AtomicInteger(0);
        this.lastCalibrationTime = new AtomicLong(System.currentTimeMillis());
        this.isCalibrating = new AtomicBoolean(false);
        
        // Create daemon thread executor for background calibration
        this.calibrationExecutor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r, "PerformanceCalibration");
                t.setDaemon(true); // Daemon thread won't prevent JVM shutdown
                return t;
            }
        });
        
        // Try to load existing models if persistence is enabled
        if (persistenceEnabled) {
            loadModels();
        }
    }
    
    /**
     * Gets the singleton instance.
     * 
     * @return The adaptive performance estimator
     */
    public static AdaptivePerformanceEstimator getInstance() {
        return INSTANCE;
    }
    
    /**
     * Estimates execution time for a MODWT operation.
     * 
     * @param signalLength Length of the input signal
     * @param waveletName Name of the wavelet
     * @param hasVectorization Whether vectorization is available
     * @return Prediction with confidence bounds
     */
    public PredictionResult estimateMODWT(int signalLength, String waveletName, 
                                         boolean hasVectorization) {
        PerformanceModel model = getOrCreateModel("MODWT");
        
        // Adjust for wavelet complexity
        double complexityFactor = getWaveletComplexityFactor(waveletName);
        
        PredictionResult base = model.predict(signalLength, hasVectorization);
        
        return new PredictionResult(
            base.estimatedTime() * complexityFactor,
            base.lowerBound() * complexityFactor,
            base.upperBound() * complexityFactor,
            base.confidence()
        );
    }
    
    /**
     * Estimates execution time for a convolution operation.
     * 
     * @param signalLength Length of the input signal
     * @param filterLength Length of the filter
     * @param hasVectorization Whether vectorization is available
     * @return Prediction with confidence bounds
     */
    public PredictionResult estimateConvolution(int signalLength, int filterLength,
                                              boolean hasVectorization) {
        PerformanceModel model = getOrCreateModel("Convolution");
        
        // Adjust for filter length
        double filterFactor = Math.sqrt(filterLength / 4.0); // Normalized to length 4
        
        PredictionResult base = model.predict(signalLength, hasVectorization);
        
        return new PredictionResult(
            base.estimatedTime() * filterFactor,
            base.lowerBound() * filterFactor,
            base.upperBound() * filterFactor,
            base.confidence()
        );
    }
    
    /**
     * Estimates execution time for batch operations.
     * 
     * @param batchSize Number of signals in the batch
     * @param signalLength Length of each signal
     * @param hasVectorization Whether vectorization is available
     * @return Prediction with confidence bounds
     */
    public PredictionResult estimateBatch(int batchSize, int signalLength,
                                        boolean hasVectorization) {
        PerformanceModel model = getOrCreateModel("Batch");
        
        // Batch efficiency improves with size but has diminishing returns
        double batchEfficiency = 1.0 + Math.log(batchSize) / Math.log(32);
        
        PredictionResult base = model.predict(signalLength, hasVectorization);
        
        return new PredictionResult(
            base.estimatedTime() * batchSize / batchEfficiency,
            base.lowerBound() * batchSize / batchEfficiency,
            base.upperBound() * batchSize / batchEfficiency,
            base.confidence()
        );
    }
    
    /**
     * Records an actual measurement to improve the model.
     * 
     * @param operation The operation type ("MODWT", "Convolution", "Batch")
     * @param inputSize The input size
     * @param actualTime The actual execution time in milliseconds
     * @param hasVectorization Whether vectorization was used
     */
    public void recordMeasurement(String operation, int inputSize, 
                                double actualTime, boolean hasVectorization) {
        if (!calibrationEnabled) {
            return; // no-op when calibration is disabled
        }
        PerformanceModel model = getOrCreateModel(operation);
        
        // Update model with measurement
        model.updateWithMeasurement(inputSize, actualTime, hasVectorization);
        
        // Check if recalibration is needed
        int count = measurementCount.incrementAndGet();
        if (count % RECALIBRATION_THRESHOLD == 0) {
            checkRecalibration();
        }
    }
    
    /**
     * Forces recalibration of all models.
     */
    public void recalibrate() {
        if (!calibrationEnabled) {
            return; // no-op when calibration is disabled
        }
        // Prevent concurrent calibrations
        if (!isCalibrating.compareAndSet(false, true)) {
            return; // Already calibrating
        }
        
        try {
            LOG.log(System.Logger.Level.INFO, "Recalibrating performance models...");
            
            PerformanceCalibrator calibrator = new PerformanceCalibrator();
            models = calibrator.calibrate();
            
            // Update operation-specific models
            operationModels.put("MODWT", models.modwtModel());
            operationModels.put("Convolution", models.convolutionModel());
            operationModels.put("Batch", models.batchModel());
            
            // Save calibrated models
            saveModels();
            
            lastCalibrationTime.set(System.currentTimeMillis());
            measurementCount.set(0);
        } finally {
            isCalibrating.set(false);
        }
    }
    
    /**
     * Gets current model accuracy statistics.
     * 
     * @return Accuracy report for all models
     */
    public String getAccuracyReport() {
        StringBuilder report = new StringBuilder();
        report.append("Performance Model Accuracy Report\n");
        report.append("=================================\n");
        
        operationModels.forEach((operation, model) -> {
            report.append("\n").append(operation).append(" Model:\n");
            report.append(model.getAccuracy().getSummary());
            report.append("\n");
        });
        
        long hoursSinceCalibration = 
            (System.currentTimeMillis() - lastCalibrationTime.get()) / (1000 * 60 * 60);
        report.append("\nLast calibration: ").append(hoursSinceCalibration).append(" hours ago\n");
        report.append("Total measurements: ").append(measurementCount.get()).append("\n");
        
        return report.toString();
    }
    
    /**
     * Shuts down the calibration executor service.
     * Should be called when the application terminates.
     */
    public void shutdown() {
        calibrationExecutor.shutdown();
        try {
            // Wait for any ongoing calibration to complete
            if (!calibrationExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                calibrationExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            calibrationExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    // Private helper methods
    
    private PerformanceModel getOrCreateModel(String operation) {
        return operationModels.computeIfAbsent(operation, k -> {
            // Create default model if not exists
            PlatformFactors factors = PlatformFactors.detectPlatform();
            return new PerformanceModel(factors);
        });
    }
    
    private double getWaveletComplexityFactor(String waveletName) {
        // Relative complexity factors based on filter length and operations
        return switch (waveletName.toLowerCase()) {
            case "haar" -> 1.0;
            case "db2", "daub2", "daubechies2" -> 1.5;
            case "db4", "daub4", "daubechies4" -> 2.0;
            case "db6", "daub6", "daubechies6" -> 2.5;
            case "db8", "daub8", "daubechies8" -> 3.0;
            default -> 2.0; // Default for unknown wavelets
        };
    }
    
    private void checkRecalibration() {
        if (!calibrationEnabled) {
            return;
        }
        boolean needsRecalibration = false;
        
        // Check if any model needs recalibration
        for (PerformanceModel model : operationModels.values()) {
            if (model.needsRecalibration()) {
                needsRecalibration = true;
                break;
            }
            
            // Also check overall accuracy
            if (model.getAccuracy().getConfidence() < ACCURACY_THRESHOLD) {
                needsRecalibration = true;
                break;
            }
        }
        
        if (needsRecalibration) {
            // Recalibrate in background to avoid blocking
            calibrationExecutor.submit(this::recalibrate);
        }
    }
    
    private void loadModels() {
        File modelFile = new File(System.getProperty("user.home"), 
            MODEL_CACHE_DIR + "/" + MODEL_FILE);
        
        if (modelFile.exists()) {
            try {
                models = PerformanceCalibrator.CalibratedModels.load(modelFile.getPath());
                
                // Populate operation models
                operationModels.put("MODWT", models.modwtModel());
                operationModels.put("Convolution", models.convolutionModel());
                operationModels.put("Batch", models.batchModel());
                
                LOG.log(System.Logger.Level.INFO, () -> "Loaded performance models from " + modelFile);
            } catch (Exception e) {
                LOG.log(System.Logger.Level.WARNING, "Failed to load performance models: {0}", e.getMessage());
                // Will use default models
            }
        }
    }
    
    private void saveModels() {
        if (models == null || !persistenceEnabled) return;
        
        File modelDir = new File(System.getProperty("user.home"), MODEL_CACHE_DIR);
        if (!modelDir.exists()) {
            modelDir.mkdirs();
        }
        
        File modelFile = new File(modelDir, MODEL_FILE);
        
        try {
            models.save(modelFile.getPath());
            LOG.log(System.Logger.Level.INFO, () -> "Saved performance models to " + modelFile);
        } catch (Exception e) {
            LOG.log(System.Logger.Level.WARNING, "Failed to save performance models: {0}", e.getMessage());
        }
    }

    // Helper: read boolean from system properties and/or env vars
    private static boolean getBoolean(String prop, String altProp, String env, String altEnv, boolean defaultVal) {
        String v = null;
        if (prop != null) v = System.getProperty(prop);
        if ((v == null || v.isBlank()) && altProp != null) v = System.getProperty(altProp);
        if ((v == null || v.isBlank()) && env != null) v = System.getenv(env);
        if ((v == null || v.isBlank()) && altEnv != null) v = System.getenv(altEnv);
        if (v == null) return defaultVal;
        v = v.trim().toLowerCase();
        return v.equals("true") || v.equals("1") || v.equals("yes") || v.equals("on");
    }

    // Package-private diagnostic helpers (primarily for tests)
    boolean isCalibrationEnabled() { return calibrationEnabled; }
    boolean isPersistenceEnabled() { return persistenceEnabled; }
    int getTotalMeasurements() { return measurementCount.get(); }
}
