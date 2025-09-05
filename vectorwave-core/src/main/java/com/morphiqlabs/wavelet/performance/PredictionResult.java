package com.morphiqlabs.wavelet.performance;

/**
 * Result of a performance prediction including confidence bounds.
 *
 * <p>This record encapsulates the predicted execution time along with
 * confidence intervals and the model's confidence level.</p>
 *
 * @param estimatedTime predicted execution time (ms)
 * @param lowerBound lower bound of confidence interval (ms)
 * @param upperBound upper bound of confidence interval (ms)
 * @param confidence confidence level in [0,1]
 */
public record PredictionResult(
    double estimatedTime,
    double lowerBound,
    double upperBound,
    double confidence
) {
    
    /**
     * Validates the prediction result.
     */
    public PredictionResult {
        if (estimatedTime < 0) {
            throw new IllegalArgumentException("Estimated time cannot be negative");
        }
        if (lowerBound < 0 || lowerBound > estimatedTime) {
            throw new IllegalArgumentException("Invalid lower bound");
        }
        if (upperBound < estimatedTime) {
            throw new IllegalArgumentException("Upper bound must be >= estimated time");
        }
        if (confidence < 0 || confidence > 1) {
            throw new IllegalArgumentException("Confidence must be between 0 and 1");
        }
    }
    
    /**
     * Gets the uncertainty range as a percentage.
     * 
     * @return Uncertainty as a fraction (0.2 = ±20%)
     */
    public double getUncertainty() {
        if (estimatedTime == 0) return 0;
        return (upperBound - lowerBound) / (2 * estimatedTime);
    }
    
    /**
     * Checks if this is a high-confidence prediction.
     * 
     * @return true if confidence >= 90%
     */
    public boolean isHighConfidence() {
        return confidence >= 0.9;
    }
    
    /**
     * Formats the prediction as a human-readable string.
     * 
     * @return Formatted prediction with bounds
     */
    public String format() {
        return String.format("%.2f ms [%.2f - %.2f ms] (%.0f%% confidence)",
            estimatedTime, lowerBound, upperBound, confidence * 100);
    }
    
    /**
     * Gets a brief summary suitable for logging.
     * 
     * @return Brief summary
     */
    public String summary() {
        return String.format("%.2f±%.0f%% ms", 
            estimatedTime, getUncertainty() * 100);
    }
}
