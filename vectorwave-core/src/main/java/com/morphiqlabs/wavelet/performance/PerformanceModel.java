package com.morphiqlabs.wavelet.performance;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Empirical performance model for wavelet operations with platform-specific calibration.
 * 
 * <p>This class represents a performance model that can predict execution time based on
 * input size and system characteristics. The model is calibrated using actual measurements
 * from the target platform.</p>
 * 
 * <p>The model uses a piecewise polynomial approach where different size ranges have
 * different coefficients, allowing accurate predictions across a wide range of input sizes.</p>
 */
public class PerformanceModel implements Serializable {
    private static final long serialVersionUID = 1L;
    
    /**
     * Model coefficients for different size ranges.
     * Each range has the form: time = a + b*n + c*n^2
     */
    private final ConcurrentHashMap<SizeRange, ModelCoefficients> coefficients = new ConcurrentHashMap<>();
    
    /**
     * Platform-specific scaling factors.
     */
    private final PlatformFactors platformFactors;
    
    /**
     * Confidence intervals for predictions.
     */
    private final ConcurrentHashMap<SizeRange, ConfidenceInterval> confidenceIntervals = new ConcurrentHashMap<>();
    
    /**
     * Number of measurements used for calibration.
     */
    private int measurementCount = 0;
    
    /**
     * Timestamp of last calibration.
     */
    private long lastCalibrationTime = 0;
    
    /**
     * Model accuracy metrics.
     */
    private ModelAccuracy accuracy = new ModelAccuracy();
    
    /**
     * Creates a new performance model with platform-specific factors.
     * 
     * @param platformFactors Platform-specific scaling factors
     */
    public PerformanceModel(PlatformFactors platformFactors) {
        this.platformFactors = platformFactors;
        initializeDefaultCoefficients();
    }
    
    /**
     * Predicts execution time for a given input size.
     * 
     * @param inputSize The size of the input
     * @param hasVectorization Whether vectorization is available
     * @return Predicted execution time in milliseconds
     */
    public PredictionResult predict(int inputSize, boolean hasVectorization) {
        SizeRange range = SizeRange.forSize(inputSize);
        ModelCoefficients coeff = coefficients.get(range);
        
        if (coeff == null) {
            // Extrapolate from nearest range
            coeff = extrapolateCoefficients(inputSize);
        }
        
        // Base prediction using polynomial model
        double baseTime = coeff.evaluate(inputSize);
        
        // Ensure base time is positive - safety guard
        if (baseTime <= 0) {
            baseTime = 0.001; // 1 microsecond minimum
        }
        
        // Apply platform-specific factors
        double adjustedTime = baseTime * platformFactors.cpuSpeedFactor;
        
        // Apply vectorization speedup if available
        if (hasVectorization) {
            adjustedTime /= platformFactors.vectorSpeedup;
        }
        
        // Apply cache effects
        adjustedTime *= getCacheEffectMultiplier(inputSize);
        
        // Final safety guard to ensure positive time
        if (adjustedTime <= 0) {
            adjustedTime = 0.001; // 1 microsecond minimum
        }
        
        // Get confidence interval
        ConfidenceInterval ci = confidenceIntervals.getOrDefault(range, 
            new ConfidenceInterval(0.9, 1.1));
        
        return new PredictionResult(
            adjustedTime,
            ci.getLowerBound(adjustedTime),
            ci.getUpperBound(adjustedTime),
            accuracy.getConfidence()
        );
    }
    
    /**
     * Updates the model with a new measurement.
     * 
     * @param inputSize The input size
     * @param actualTime The actual execution time in milliseconds
     * @param hasVectorization Whether vectorization was used
     */
    public void updateWithMeasurement(int inputSize, double actualTime, boolean hasVectorization) {
        // Normalize time to base conditions
        double normalizedTime = actualTime;
        if (hasVectorization) {
            normalizedTime *= platformFactors.vectorSpeedup;
        }
        normalizedTime /= platformFactors.cpuSpeedFactor;
        normalizedTime /= getCacheEffectMultiplier(inputSize);
        
        // Update coefficients for the appropriate range
        SizeRange range = SizeRange.forSize(inputSize);
        ModelCoefficients coeff = coefficients.computeIfAbsent(range, 
            k -> new ModelCoefficients());
        
        // Use online learning to update coefficients
        coeff.updateWithMeasurement(inputSize, normalizedTime);
        
        // Update confidence intervals
        updateConfidenceInterval(range, inputSize, actualTime, normalizedTime);
        
        // Update model accuracy
        double predicted = predict(inputSize, hasVectorization).estimatedTime();
        accuracy.updateWithPrediction(predicted, actualTime);
        
        measurementCount++;
        lastCalibrationTime = System.currentTimeMillis();
    }
    
    /**
     * Performs batch calibration with multiple measurements.
     * 
     * @param measurements Array of measurements
     */
    public void calibrate(Measurement[] measurements) {
        // Clear existing model
        coefficients.clear();
        confidenceIntervals.clear();
        accuracy = new ModelAccuracy();
        
        // Group measurements by size range
        Map<SizeRange, java.util.List<Measurement>> grouped = 
            java.util.Arrays.stream(measurements)
                .collect(java.util.stream.Collectors.groupingBy(
                    m -> SizeRange.forSize(m.inputSize)));
        
        // Fit coefficients for each range
        grouped.forEach((range, rangeMeasurements) -> {
            ModelCoefficients coeff = fitCoefficients(rangeMeasurements);
            coefficients.put(range, coeff);
            
            // Calculate confidence intervals
            ConfidenceInterval ci = calculateConfidenceInterval(rangeMeasurements, coeff);
            confidenceIntervals.put(range, ci);
        });
        
        measurementCount = measurements.length;
        lastCalibrationTime = System.currentTimeMillis();
        
        // Validate model accuracy
        for (Measurement m : measurements) {
            double predicted = predict(m.inputSize, m.hasVectorization).estimatedTime();
            accuracy.updateWithPrediction(predicted, m.actualTime);
        }
    }
    
    /**
     * Gets model accuracy metrics.
     * 
     * @return Current model accuracy
     */
    public ModelAccuracy getAccuracy() {
        return accuracy;
    }
    
    /**
     * Checks if the model needs recalibration.
     * 
     * @return true if recalibration is recommended
     */
    public boolean needsRecalibration() {
        // Recalibrate if accuracy drops below threshold
        if (accuracy.getMeanAbsolutePercentageError() > 0.15) {
            return true;
        }
        
        // Recalibrate if model is old (> 30 days)
        long daysSinceCalibration = (System.currentTimeMillis() - lastCalibrationTime) 
            / (1000 * 60 * 60 * 24);
        if (daysSinceCalibration > 30) {
            return true;
        }
        
        // Recalibrate if too few measurements
        return measurementCount < 100;
    }
    
    /**
     * Exports model coefficients for persistence.
     * 
     * @return Serializable model data
     */
    public ModelData exportModel() {
        return new ModelData(
            new ConcurrentHashMap<>(coefficients),
            new ConcurrentHashMap<>(confidenceIntervals),
            platformFactors,
            accuracy,
            measurementCount,
            lastCalibrationTime
        );
    }
    
    /**
     * Imports model coefficients from persisted data.
     * 
     * @param data Previously exported model data
     */
    public void importModel(ModelData data) {
        coefficients.clear();
        coefficients.putAll(data.coefficients);
        
        confidenceIntervals.clear();
        confidenceIntervals.putAll(data.confidenceIntervals);
        
        accuracy = data.accuracy;
        measurementCount = data.measurementCount;
        lastCalibrationTime = data.lastCalibrationTime;
    }
    
    // Private helper methods
    
    private void initializeDefaultCoefficients() {
        // Initialize with reasonable defaults based on complexity analysis
        coefficients.put(SizeRange.TINY, new ModelCoefficients(0.01, 0.00001, 0));
        coefficients.put(SizeRange.SMALL, new ModelCoefficients(0.1, 0.00005, 0));
        coefficients.put(SizeRange.MEDIUM, new ModelCoefficients(0.5, 0.0001, 0));
        coefficients.put(SizeRange.LARGE, new ModelCoefficients(2.0, 0.0002, 0));
        coefficients.put(SizeRange.HUGE, new ModelCoefficients(8.0, 0.0003, 0.0000001));
    }
    
    private ModelCoefficients extrapolateCoefficients(int inputSize) {
        // Find nearest range and extrapolate
        SizeRange nearest = SizeRange.MEDIUM;
        int minDistance = Integer.MAX_VALUE;
        
        for (SizeRange range : coefficients.keySet()) {
            int distance = Math.abs(range.getMidpoint() - inputSize);
            if (distance < minDistance) {
                minDistance = distance;
                nearest = range;
            }
        }
        
        return coefficients.get(nearest);
    }
    
    private double getCacheEffectMultiplier(int inputSize) {
        // Model cache effects based on data size
        double dataSize = inputSize * 8.0; // doubles are 8 bytes
        
        if (dataSize <= platformFactors.l1CacheSize) {
            return 1.0; // Fits in L1 cache
        } else if (dataSize <= platformFactors.l2CacheSize) {
            return 1.2; // Fits in L2 cache
        } else if (dataSize <= platformFactors.l3CacheSize) {
            return 1.5; // Fits in L3 cache
        } else {
            return 2.0; // Main memory access
        }
    }
    
    private ModelCoefficients fitCoefficients(java.util.List<Measurement> measurements) {
        // Use least squares fitting for polynomial model
        // This is a simplified implementation - production code would use
        // a proper numerical library
        
        int n = measurements.size();
        if (n < 3) {
            // Not enough data for quadratic fit
            return new ModelCoefficients(0.1, 0.0001, 0);
        }
        
        // Calculate sums for least squares
        double sumX = 0, sumX2 = 0, sumX3 = 0, sumX4 = 0;
        double sumY = 0, sumXY = 0, sumX2Y = 0;
        
        for (Measurement m : measurements) {
            double x = m.inputSize;
            double y = m.normalizedTime;
            
            sumX += x;
            sumX2 += x * x;
            sumX3 += x * x * x;
            sumX4 += x * x * x * x;
            sumY += y;
            sumXY += x * y;
            sumX2Y += x * x * y;
        }
        
        // Solve normal equations (simplified for demonstration)
        // In practice, use a proper linear algebra library
        double a = sumY / n; // Simplified constant term
        double b = (n * sumXY - sumX * sumY) / (n * sumX2 - sumX * sumX);
        double c = 0; // Simplified - skip quadratic term for stability
        
        return new ModelCoefficients(a, b, c);
    }
    
    private ConfidenceInterval calculateConfidenceInterval(
            java.util.List<Measurement> measurements, ModelCoefficients coeff) {
        
        // Calculate residuals
        double sumSquaredError = 0;
        for (Measurement m : measurements) {
            double predicted = coeff.evaluate(m.inputSize);
            double error = m.normalizedTime - predicted;
            sumSquaredError += error * error;
        }
        
        // Calculate standard deviation
        double stdDev = Math.sqrt(sumSquaredError / measurements.size());
        
        // 95% confidence interval (approximately 2 standard deviations)
        double lowerMultiplier = 1.0 - 2.0 * stdDev / coeff.evaluate(1000);
        double upperMultiplier = 1.0 + 2.0 * stdDev / coeff.evaluate(1000);
        
        // Bound the multipliers to reasonable ranges
        lowerMultiplier = Math.max(0.5, Math.min(0.95, lowerMultiplier));
        upperMultiplier = Math.max(1.05, Math.min(2.0, upperMultiplier));
        
        return new ConfidenceInterval(lowerMultiplier, upperMultiplier);
    }
    
    private void updateConfidenceInterval(SizeRange range, int inputSize, 
                                        double actualTime, double normalizedTime) {
        ConfidenceInterval current = confidenceIntervals.get(range);
        if (current == null) {
            current = new ConfidenceInterval(0.9, 1.1);
            confidenceIntervals.put(range, current);
        }
        
        // Update interval based on prediction error
        double predicted = coefficients.get(range).evaluate(inputSize);
        double error = Math.abs(normalizedTime - predicted) / predicted;
        
        // Exponential moving average update
        current.updateWithError(error);
    }
    
    /**
     * Size ranges for piecewise modeling.
     */
    public enum SizeRange {
        TINY(0, 256),
        SMALL(257, 1024),
        MEDIUM(1025, 4096),
        LARGE(4097, 16384),
        HUGE(16385, Integer.MAX_VALUE);
        
        private final int minSize;
        private final int maxSize;
        
        SizeRange(int minSize, int maxSize) {
            this.minSize = minSize;
            this.maxSize = maxSize;
        }
        
        public static SizeRange forSize(int size) {
            for (SizeRange range : values()) {
                if (size >= range.minSize && size <= range.maxSize) {
                    return range;
                }
            }
            return HUGE;
        }
        
        public int getMidpoint() {
            if (this == HUGE) {
                return 32768; // Representative size for huge range
            }
            return (minSize + maxSize) / 2;
        }
    }
    
    /**
     * Measurement data for calibration.
     */
    public static class Measurement {
        public final int inputSize;
        public final double actualTime;
        public final boolean hasVectorization;
        public final double normalizedTime;
        
        public Measurement(int inputSize, double actualTime, boolean hasVectorization) {
            this.inputSize = inputSize;
            this.actualTime = actualTime;
            this.hasVectorization = hasVectorization;
            this.normalizedTime = actualTime; // Will be normalized during processing
        }
    }
    
    /**
     * Serializable model data for persistence.
     */
    public static class ModelData implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public final ConcurrentHashMap<SizeRange, ModelCoefficients> coefficients;
        public final ConcurrentHashMap<SizeRange, ConfidenceInterval> confidenceIntervals;
        public final PlatformFactors platformFactors;
        public final ModelAccuracy accuracy;
        public final int measurementCount;
        public final long lastCalibrationTime;
        
        public ModelData(ConcurrentHashMap<SizeRange, ModelCoefficients> coefficients,
                        ConcurrentHashMap<SizeRange, ConfidenceInterval> confidenceIntervals,
                        PlatformFactors platformFactors,
                        ModelAccuracy accuracy,
                        int measurementCount,
                        long lastCalibrationTime) {
            this.coefficients = coefficients;
            this.confidenceIntervals = confidenceIntervals;
            this.platformFactors = platformFactors;
            this.accuracy = accuracy;
            this.measurementCount = measurementCount;
            this.lastCalibrationTime = lastCalibrationTime;
        }
    }
}