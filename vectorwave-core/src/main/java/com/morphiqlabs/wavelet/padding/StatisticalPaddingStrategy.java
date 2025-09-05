package com.morphiqlabs.wavelet.padding;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import java.util.Arrays;

/**
 * Statistical padding strategy that extends signals based on statistical properties.
 *
 * <p>This strategy analyzes the signal's statistical characteristics and extends
 * it accordingly. Different methods are suitable for different signal types.
 * Ideal for:</p>
 * <ul>
 *   <li>Stochastic signals with known statistical properties</li>
 *   <li>Financial time series with mean-reverting behavior</li>
 *   <li>Signals where preserving statistical properties is important</li>
 *   <li>Noisy signals where simple extrapolation may amplify noise</li>
 * </ul>
 *
 * <p>Available statistical methods:</p>
 * <ul>
 *   <li><b>MEAN</b>: Pad with signal mean (good for stationary signals)</li>
 *   <li><b>MEDIAN</b>: Pad with signal median (robust to outliers)</li>
 *   <li><b>WEIGHTED_MEAN</b>: Recent values weighted higher (trending signals)</li>
 *   <li><b>TREND</b>: Linear trend extrapolation based on entire signal</li>
 *   <li><b>VARIANCE_MATCHED</b>: Random values matching signal variance</li>
 *   <li><b>LOCAL_MEAN</b>: Mean of nearby values (smooth transitions)</li>
 * </ul>
 *
 * @param method statistical method
 * @param windowSize window size used by the method (0 = auto)
 * @param mode where to apply padding (LEFT/RIGHT/SYMMETRIC)
 */
public record StatisticalPaddingStrategy(
    StatMethod method,
    int windowSize,
    PaddingMode mode
) implements PaddingStrategy {
    
    /**
     * Statistical method for padding.
     */
    public enum StatMethod {
        /** Pad with global mean of signal */
        MEAN,
        /** Pad with global median of signal */
        MEDIAN,
        /** Pad with weighted mean (recent values weighted higher) */
        WEIGHTED_MEAN,
        /** Extrapolate linear trend from entire signal */
        TREND,
        /** Pad with random values matching signal variance */
        VARIANCE_MATCHED,
        /** Pad with mean of nearby values */
        LOCAL_MEAN
    }
    
    /**
     * Padding mode determines where padding is applied.
     */
    public enum PaddingMode {
        /** Pad only on the right side */
        RIGHT,
        /** Pad equally on both sides */
        SYMMETRIC,
        /** Pad only on the left side */
        LEFT
    }
    
    /**
     * Creates a statistical padding strategy with default MEAN method.
     */
    public StatisticalPaddingStrategy() {
        this(StatMethod.MEAN, 0, PaddingMode.RIGHT);
    }
    
    /**
     * Creates a statistical padding strategy with specified method.
     * 
     * @param method the statistical method to use
     */
    public StatisticalPaddingStrategy(StatMethod method) {
        this(method, 0, PaddingMode.RIGHT);
    }
    
    /**
     * Validates parameters and sets default window size if needed.
     */
    public StatisticalPaddingStrategy {
        if (windowSize == 0) {
            // Auto-select window size based on method
            windowSize = switch (method) {
                case LOCAL_MEAN, WEIGHTED_MEAN -> 10; // Default window
                default -> Integer.MAX_VALUE; // Use entire signal
            };
        }
        
        if (windowSize < 1) {
            throw new InvalidArgumentException("Window size must be positive, got " + windowSize);
        }
    }
    
    @Override
    public double[] pad(double[] signal, int targetLength) {
        if (signal == null) {
            throw new InvalidArgumentException("Signal cannot be null");
        }
        if (signal.length == 0) {
            throw new InvalidArgumentException("Signal cannot be empty");
        }
        if (targetLength < signal.length) {
            throw new InvalidArgumentException(
                    "Target length " + targetLength + " must be >= signal length " + signal.length);
        }
        
        if (targetLength == signal.length) {
            return signal.clone();
        }
        
        double[] padded = new double[targetLength];
        int padLength = targetLength - signal.length;
        
        switch (mode) {
            case RIGHT -> {
                System.arraycopy(signal, 0, padded, 0, signal.length);
                applyStatisticalPadding(signal, padded, signal.length, padLength, true);
            }
            case LEFT -> {
                applyStatisticalPadding(signal, padded, 0, padLength, false);
                System.arraycopy(signal, 0, padded, padLength, signal.length);
            }
            case SYMMETRIC -> {
                int leftPad = padLength / 2;
                int rightPad = padLength - leftPad;
                
                applyStatisticalPadding(signal, padded, 0, leftPad, false);
                System.arraycopy(signal, 0, padded, leftPad, signal.length);
                applyStatisticalPadding(signal, padded, leftPad + signal.length, rightPad, true);
            }
        }
        
        return padded;
    }
    
    /**
     * Apply statistical padding to the specified region.
     */
    private void applyStatisticalPadding(double[] signal, double[] padded, 
                                         int startIdx, int length, boolean rightSide) {
        switch (method) {
            case MEAN -> {
                double mean = calculateMean(signal);
                Arrays.fill(padded, startIdx, startIdx + length, mean);
            }
            case MEDIAN -> {
                double median = calculateMedian(signal);
                Arrays.fill(padded, startIdx, startIdx + length, median);
            }
            case WEIGHTED_MEAN -> {
                double weightedMean = calculateWeightedMean(signal, rightSide);
                Arrays.fill(padded, startIdx, startIdx + length, weightedMean);
            }
            case LOCAL_MEAN -> {
                double localMean = calculateLocalMean(signal, rightSide);
                Arrays.fill(padded, startIdx, startIdx + length, localMean);
            }
            case TREND -> {
                applyTrendExtrapolation(signal, padded, startIdx, length, rightSide);
            }
            case VARIANCE_MATCHED -> {
                applyVarianceMatchedPadding(signal, padded, startIdx, length);
            }
        }
    }
    
    /**
     * Calculate mean of signal.
     */
    private double calculateMean(double[] signal) {
        double sum = 0;
        for (double val : signal) {
            sum += val;
        }
        return sum / signal.length;
    }
    
    /**
     * Calculate median of signal.
     */
    private double calculateMedian(double[] signal) {
        double[] sorted = signal.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        if (n % 2 == 0) {
            return (sorted[n/2 - 1] + sorted[n/2]) / 2.0;
        } else {
            return sorted[n/2];
        }
    }
    
    /**
     * Calculate weighted mean with exponential weighting.
     */
    private double calculateWeightedMean(double[] signal, boolean rightSide) {
        int actualWindow = Math.min(windowSize, signal.length);
        double sumWeighted = 0;
        double sumWeights = 0;
        double alpha = 0.9; // Decay factor
        
        if (rightSide) {
            // Weight recent values (end of signal) more heavily
            for (int i = 0; i < actualWindow; i++) {
                int idx = signal.length - actualWindow + i;
                double weight = Math.pow(alpha, actualWindow - i - 1);
                sumWeighted += signal[idx] * weight;
                sumWeights += weight;
            }
        } else {
            // Weight early values (start of signal) more heavily
            for (int i = 0; i < actualWindow; i++) {
                double weight = Math.pow(alpha, i);
                sumWeighted += signal[i] * weight;
                sumWeights += weight;
            }
        }
        
        return sumWeights > 0 ? sumWeighted / sumWeights : calculateMean(signal);
    }
    
    /**
     * Calculate mean of local window.
     */
    private double calculateLocalMean(double[] signal, boolean rightSide) {
        int actualWindow = Math.min(windowSize, signal.length);
        double sum = 0;
        
        if (rightSide) {
            // Use last actualWindow values
            for (int i = signal.length - actualWindow; i < signal.length; i++) {
                sum += signal[i];
            }
        } else {
            // Use first actualWindow values
            for (int i = 0; i < actualWindow; i++) {
                sum += signal[i];
            }
        }
        
        return sum / actualWindow;
    }
    
    /**
     * Apply trend-based extrapolation.
     */
    private void applyTrendExtrapolation(double[] signal, double[] padded, 
                                         int startIdx, int length, boolean rightSide) {
        // Fit linear trend to entire signal
        double[] x = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            x[i] = i;
        }
        
        double[] trend = fitLinearTrend(x, signal);
        double slope = trend[1];
        double intercept = trend[0];
        
        if (rightSide) {
            // Extrapolate forward
            for (int i = 0; i < length; i++) {
                double xVal = signal.length + i;
                padded[startIdx + i] = intercept + slope * xVal;
            }
        } else {
            // Extrapolate backward
            for (int i = 0; i < length; i++) {
                double xVal = -(length - i);
                padded[startIdx + i] = intercept + slope * xVal;
            }
        }
    }
    
    /**
     * Fit linear trend using least squares.
     * Returns [intercept, slope].
     */
    private double[] fitLinearTrend(double[] x, double[] y) {
        int n = x.length;
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < n; i++) {
            sumX += x[i];
            sumY += y[i];
            sumXY += x[i] * y[i];
            sumX2 += x[i] * x[i];
        }
        
        double denominator = n * sumX2 - sumX * sumX;
        if (Math.abs(denominator) < 1e-10) {
            // Degenerate case, return mean as constant
            return new double[]{sumY / n, 0};
        }
        
        double slope = (n * sumXY - sumX * sumY) / denominator;
        double intercept = (sumY - slope * sumX) / n;
        
        return new double[]{intercept, slope};
    }
    
    /**
     * Apply variance-matched padding with pseudo-random values.
     */
    private void applyVarianceMatchedPadding(double[] signal, double[] padded, 
                                             int startIdx, int length) {
        double mean = calculateMean(signal);
        double variance = calculateVariance(signal, mean);
        double stdDev = Math.sqrt(variance);
        
        // Use deterministic pseudo-random for reproducibility
        // Based on signal properties as seed
        long seed = Double.doubleToLongBits(mean) ^ Double.doubleToLongBits(variance);
        java.util.Random random = new java.util.Random(seed);
        
        for (int i = 0; i < length; i++) {
            // Generate values with matching mean and variance
            padded[startIdx + i] = mean + stdDev * random.nextGaussian();
        }
    }
    
    /**
     * Calculate variance of signal.
     */
    private double calculateVariance(double[] signal, double mean) {
        double sumSquaredDiff = 0;
        for (double val : signal) {
            double diff = val - mean;
            sumSquaredDiff += diff * diff;
        }
        return sumSquaredDiff / signal.length;
    }
    
    @Override
    public double[] trim(double[] result, int originalLength) {
        if (result.length == originalLength) {
            return result;
        }
        if (originalLength > result.length) {
            throw new InvalidArgumentException(
                    "Original length " + originalLength + " exceeds result length " + result.length);
        }
        
        double[] trimmed = new double[originalLength];
        
        switch (mode) {
            case RIGHT -> System.arraycopy(result, 0, trimmed, 0, originalLength);
            case LEFT -> System.arraycopy(result, result.length - originalLength, trimmed, 0, originalLength);
            case SYMMETRIC -> {
                int totalPadding = result.length - originalLength;
                int leftPad = totalPadding / 2;
                System.arraycopy(result, leftPad, trimmed, 0, originalLength);
            }
        }
        
        return trimmed;
    }
    
    @Override
    public String name() {
        return String.format("statistical-%s-%s", 
                method.name().toLowerCase(), mode.name().toLowerCase());
    }
    
    @Override
    public String description() {
        String methodDesc = switch (method) {
            case MEAN -> "global mean";
            case MEDIAN -> "global median";
            case WEIGHTED_MEAN -> "weighted mean";
            case TREND -> "trend extrapolation";
            case VARIANCE_MATCHED -> "variance-matched random";
            case LOCAL_MEAN -> "local mean";
        };
        return String.format("Statistical padding (%s, window=%d, %s mode)", 
                methodDesc, windowSize == Integer.MAX_VALUE ? -1 : windowSize, 
                mode.name().toLowerCase());
    }
}
