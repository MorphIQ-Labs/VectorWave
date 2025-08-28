package com.morphiqlabs.wavelet.performance;

import java.io.Serializable;

/**
 * Tracks accuracy metrics for the performance model.
 * 
 * <p>This class maintains various accuracy statistics to evaluate how well
 * the model predicts actual performance.</p>
 * 
 * <p>Thread-safe implementation using synchronized methods for all operations
 * that read or modify accuracy metrics.</p>
 */
public class ModelAccuracy implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private double sumAbsoluteError = 0;
    private double sumSquaredError = 0;
    private double sumAbsolutePercentageError = 0;
    private double sumActual = 0;
    private int count = 0;
    
    // Track worst predictions
    private double maxOverPrediction = 0;
    private double maxUnderPrediction = 0;
    
    // Track confidence interval hits
    private int withinIntervalCount = 0;
    
    /**
     * Updates accuracy metrics with a new prediction.
     * Thread-safe update that ensures atomic modification of all metrics.
     * 
     * @param predicted The predicted execution time
     * @param actual The actual execution time
     */
    public synchronized void updateWithPrediction(double predicted, double actual) {
        double error = actual - predicted;
        double absoluteError = Math.abs(error);
        double percentageError = absoluteError / actual;
        
        sumAbsoluteError += absoluteError;
        sumSquaredError += error * error;
        sumAbsolutePercentageError += percentageError;
        sumActual += actual;
        count++;
        
        // Track worst predictions
        if (error > 0) {
            // Under-prediction (actual > predicted)
            maxUnderPrediction = Math.max(maxUnderPrediction, error / predicted);
        } else {
            // Over-prediction (actual < predicted)
            maxOverPrediction = Math.max(maxOverPrediction, -error / predicted);
        }
    }
    
    /**
     * Records whether a prediction fell within its confidence interval.
     * Thread-safe recording of interval hits.
     * 
     * @param withinInterval true if the actual value was within the predicted interval
     */
    public synchronized void recordIntervalHit(boolean withinInterval) {
        if (withinInterval) {
            withinIntervalCount++;
        }
    }
    
    /**
     * Gets the mean absolute error.
     * Thread-safe getter.
     * 
     * @return MAE in milliseconds
     */
    public synchronized double getMeanAbsoluteError() {
        return count > 0 ? sumAbsoluteError / count : 0;
    }
    
    /**
     * Gets the root mean squared error.
     * Thread-safe getter.
     * 
     * @return RMSE in milliseconds
     */
    public synchronized double getRootMeanSquaredError() {
        return count > 0 ? Math.sqrt(sumSquaredError / count) : 0;
    }
    
    /**
     * Calculates the mean absolute percentage error (MAPE).
     * This is a private helper method to avoid code duplication.
     * Must be called from a synchronized method since it accesses shared state.
     * 
     * @return MAPE as a fraction (0.1 = 10% error)
     */
    private double calculateMAPE() {
        return count > 0 ? sumAbsolutePercentageError / count : 0;
    }
    
    /**
     * Gets the mean absolute percentage error.
     * Thread-safe getter.
     * 
     * @return MAPE as a fraction (0.1 = 10% error)
     */
    public synchronized double getMeanAbsolutePercentageError() {
        return calculateMAPE();
    }
    
    /**
     * Gets the coefficient of determination (R²).
     * Thread-safe getter.
     * 
     * @return R² value between 0 and 1
     */
    public synchronized double getRSquared() {
        if (count < 2) return 0;
        
        double meanActual = sumActual / count;
        double totalVariance = sumSquaredError / count;
        double actualVariance = (sumActual * sumActual) / (count * count);
        
        if (actualVariance == 0) return 0;
        
        return Math.max(0, 1 - totalVariance / actualVariance);
    }
    
    /**
     * Gets the confidence level of the model.
     * Thread-safe getter.
     * 
     * @return Confidence between 0 and 1
     */
    public synchronized double getConfidence() {
        double mape = calculateMAPE();
        
        // Convert MAPE to confidence
        // 0% error = 100% confidence
        // 20% error = 80% confidence
        // 50% error = 50% confidence
        return Math.max(0, Math.min(1, 1 - mape));
    }
    
    /**
     * Gets the percentage of predictions within confidence intervals.
     * Thread-safe getter.
     * 
     * @return Hit rate as a percentage (0-100)
     */
    public synchronized double getIntervalHitRate() {
        return count > 0 ? (100.0 * withinIntervalCount / count) : 0;
    }
    
    /**
     * Gets the worst over-prediction percentage.
     * Thread-safe getter.
     * 
     * @return Maximum over-prediction as a fraction
     */
    public synchronized double getMaxOverPrediction() {
        return maxOverPrediction;
    }
    
    /**
     * Gets the worst under-prediction percentage.
     * Thread-safe getter.
     * 
     * @return Maximum under-prediction as a fraction
     */
    public synchronized double getMaxUnderPrediction() {
        return maxUnderPrediction;
    }
    
    /**
     * Gets the number of predictions tracked.
     * Thread-safe getter.
     * 
     * @return Number of predictions
     */
    public synchronized int getCount() {
        return count;
    }
    
    /**
     * Resets all accuracy metrics.
     * Thread-safe reset operation.
     */
    public synchronized void reset() {
        sumAbsoluteError = 0;
        sumSquaredError = 0;
        sumAbsolutePercentageError = 0;
        sumActual = 0;
        count = 0;
        maxOverPrediction = 0;
        maxUnderPrediction = 0;
        withinIntervalCount = 0;
    }
    
    /**
     * Gets a summary of the accuracy metrics.
     * Thread-safe summary generation.
     * 
     * @return Human-readable summary
     */
    public synchronized String getSummary() {
        if (count == 0) {
            return "No predictions recorded";
        }
        
        return String.format(
            "Accuracy Summary (n=%d):\n" +
            "  MAE: %.2f ms\n" +
            "  RMSE: %.2f ms\n" +
            "  MAPE: %.1f%%\n" +
            "  R²: %.3f\n" +
            "  Confidence: %.1f%%\n" +
            "  Interval hit rate: %.1f%%\n" +
            "  Max over-prediction: %.1f%%\n" +
            "  Max under-prediction: %.1f%%",
            count,
            getMeanAbsoluteError(),
            getRootMeanSquaredError(),
            getMeanAbsolutePercentageError() * 100,
            getRSquared(),
            getConfidence() * 100,
            getIntervalHitRate(),
            maxOverPrediction * 100,
            maxUnderPrediction * 100
        );
    }
    
    @Override
    public synchronized String toString() {
        double mape = calculateMAPE();
        double confidence = Math.max(0, Math.min(1, 1 - mape));
        return String.format("ModelAccuracy{MAPE=%.1f%%, confidence=%.1f%%, n=%d}",
            mape * 100,
            confidence * 100,
            count);
    }
}