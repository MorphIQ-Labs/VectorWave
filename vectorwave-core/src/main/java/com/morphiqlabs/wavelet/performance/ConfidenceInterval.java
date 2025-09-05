package com.morphiqlabs.wavelet.performance;

import java.io.Serializable;

/**
 * Confidence interval for performance predictions.
 * 
 * <p>Represents the uncertainty in performance predictions as multipliers
 * on the base prediction. For example, a 95% confidence interval of [0.9, 1.1]
 * means the actual time is expected to be between 90% and 110% of the predicted time.</p>
 */
public class ConfidenceInterval implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private double lowerMultiplier;
    private double upperMultiplier;
    
    // For adaptive updates
    private double errorSum = 0;
    private int errorCount = 0;
    private static final double ALPHA = 0.1; // Exponential moving average factor
    
    /**
     * Creates a confidence interval with specified multipliers.
     * 
     * @param lowerMultiplier Lower bound multiplier (e.g., 0.9 for 90%)
     * @param upperMultiplier Upper bound multiplier (e.g., 1.1 for 110%)
     */
    public ConfidenceInterval(double lowerMultiplier, double upperMultiplier) {
        if (lowerMultiplier <= 0 || lowerMultiplier >= 1) {
            throw new IllegalArgumentException("Lower multiplier must be between 0 and 1");
        }
        if (upperMultiplier <= 1 || upperMultiplier > 3) {
            throw new IllegalArgumentException("Upper multiplier must be between 1 and 3");
        }
        if (lowerMultiplier >= upperMultiplier) {
            throw new IllegalArgumentException("Lower multiplier must be less than upper multiplier");
        }
        
        this.lowerMultiplier = lowerMultiplier;
        this.upperMultiplier = upperMultiplier;
    }
    
    /**
     * Gets the lower bound for a predicted value.
     * 
     * @param prediction The base prediction
     * @return Lower bound of the confidence interval
     */
    public double getLowerBound(double prediction) {
        return prediction * lowerMultiplier;
    }
    
    /**
     * Gets the upper bound for a predicted value.
     * 
     * @param prediction The base prediction
     * @return Upper bound of the confidence interval
     */
    public double getUpperBound(double prediction) {
        return prediction * upperMultiplier;
    }
    
    /**
     * Updates the confidence interval based on prediction error.
     * 
     * @param relativeError The relative error (|actual - predicted| / predicted)
     */
    public void updateWithError(double relativeError) {
        // Update error statistics
        errorSum = ALPHA * relativeError + (1 - ALPHA) * errorSum;
        errorCount++;
        
        // Adjust interval width based on error magnitude
        double avgError = errorSum;
        
        // Target: 95% of predictions within interval
        // If errors are large, widen the interval
        double targetWidth = 2.0 * avgError + 0.1; // Minimum 10% width
        
        // Calculate new multipliers
        double center = 1.0;
        double halfWidth = Math.min(targetWidth / 2.0, 1.0); // Cap at 100% error
        
        lowerMultiplier = Math.max(0.1, center - halfWidth);
        upperMultiplier = Math.min(3.0, center + halfWidth);
        
        // Ensure multipliers are valid
        if (lowerMultiplier >= upperMultiplier) {
            lowerMultiplier = 0.5;
            upperMultiplier = 2.0;
        }
    }
    
    /**
     * Gets the width of the confidence interval as a percentage.
     * 
     * @return Width of the interval (e.g., 0.2 for 20% width)
     */
    public double getWidth() {
        return upperMultiplier - lowerMultiplier;
    }
    
    /**
     * Gets the confidence level based on the interval width.
     * 
     * @return Estimated confidence level (0-1)
     */
    public double getConfidenceLevel() {
        // Narrow intervals indicate higher confidence
        double width = getWidth();
        if (width <= 0.1) return 0.99;  // Very high confidence
        if (width <= 0.2) return 0.95;  // High confidence
        if (width <= 0.4) return 0.90;  // Good confidence
        if (width <= 0.6) return 0.80;  // Moderate confidence
        return 0.70; // Low confidence
    }
    
    /**
     * Checks if an actual value falls within the confidence interval.
     * 
     * @param predicted The predicted value
     * @param actual The actual value
     * @return true if actual is within the interval
     */
    public boolean contains(double predicted, double actual) {
        return actual >= getLowerBound(predicted) && actual <= getUpperBound(predicted);
    }
    
    /**
     * Creates a copy of this confidence interval.
     * 
     * @return New instance with same values
     */
    public ConfidenceInterval copy() {
        ConfidenceInterval copy = new ConfidenceInterval(lowerMultiplier, upperMultiplier);
        copy.errorSum = this.errorSum;
        copy.errorCount = this.errorCount;
        return copy;
    }
    
    @Override
    public String toString() {
        return String.format("[%.1f%%, %.1f%%]", 
            (lowerMultiplier * 100 - 100), 
            (upperMultiplier * 100 - 100));
    }
}