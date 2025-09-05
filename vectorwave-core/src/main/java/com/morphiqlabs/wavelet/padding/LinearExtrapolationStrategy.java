package com.morphiqlabs.wavelet.padding;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

 
/**
 * Linear extrapolation padding strategy.
 *
 * @param fitPoints number of points used for linear fit
 * @param mode padding mode applied to extrapolated values
 */
public record LinearExtrapolationStrategy(int fitPoints, PaddingMode mode) implements PaddingStrategy {
    
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
     * Creates a linear extrapolation strategy with default parameters.
     * Uses 2 fit points and RIGHT padding mode.
     */
    public LinearExtrapolationStrategy() {
        this(2, PaddingMode.RIGHT);
    }
    
    /**
     * Creates a linear extrapolation strategy with specified fit points.
     * Uses RIGHT padding mode.
     * 
     * @param fitPoints number of points to use for slope calculation (minimum 2)
     */
    public LinearExtrapolationStrategy(int fitPoints) {
        this(fitPoints, PaddingMode.RIGHT);
    }
    
    /**
     * Validates and adjusts fit points if necessary.
     */
    public LinearExtrapolationStrategy {
        if (fitPoints < 2) {
            throw new InvalidArgumentException("Fit points must be at least 2, got " + fitPoints);
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
        
        // Adjust fit points if signal is too short
        int actualFitPoints = Math.min(fitPoints, signal.length);
        
        switch (mode) {
            case RIGHT -> {
                // Copy original signal to the start
                System.arraycopy(signal, 0, padded, 0, signal.length);
                
                // Calculate slope from last fitPoints
                double slope = calculateSlope(signal, signal.length - actualFitPoints, actualFitPoints);
                double lastValue = signal[signal.length - 1];
                
                // Extrapolate to the right
                for (int i = 0; i < padLength; i++) {
                    padded[signal.length + i] = lastValue + slope * (i + 1);
                }
            }
            case LEFT -> {
                // Calculate slope from first fitPoints
                double slope = calculateSlope(signal, 0, actualFitPoints);
                double firstValue = signal[0];
                
                // Extrapolate to the left
                for (int i = 0; i < padLength; i++) {
                    padded[padLength - 1 - i] = firstValue - slope * (i + 1);
                }
                
                // Copy original signal after padding
                System.arraycopy(signal, 0, padded, padLength, signal.length);
            }
            case SYMMETRIC -> {
                // Calculate left and right padding
                int leftPad = padLength / 2;
                int rightPad = padLength - leftPad;
                
                // Calculate left slope
                double leftSlope = calculateSlope(signal, 0, actualFitPoints);
                double firstValue = signal[0];
                
                // Extrapolate to the left
                for (int i = 0; i < leftPad; i++) {
                    padded[leftPad - 1 - i] = firstValue - leftSlope * (i + 1);
                }
                
                // Copy original signal
                System.arraycopy(signal, 0, padded, leftPad, signal.length);
                
                // Calculate right slope
                double rightSlope = calculateSlope(signal, signal.length - actualFitPoints, actualFitPoints);
                double lastValue = signal[signal.length - 1];
                
                // Extrapolate to the right
                for (int i = 0; i < rightPad; i++) {
                    padded[leftPad + signal.length + i] = lastValue + rightSlope * (i + 1);
                }
            }
        }
        
        return padded;
    }
    
    /**
     * Calculate slope using least-squares fitting.
     * 
     * @param signal the signal data
     * @param start starting index for fitting
     * @param length number of points to fit
     * @return the calculated slope
     */
    private double calculateSlope(double[] signal, int start, int length) {
        if (length == 2) {
            // Simple slope for 2 points
            return signal[start + 1] - signal[start];
        }
        
        // Least-squares fitting for more points
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        
        for (int i = 0; i < length; i++) {
            double x = i;
            double y = signal[start + i];
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        
        // Calculate slope: (n*sumXY - sumX*sumY) / (n*sumX2 - sumX*sumX)
        double n = length;
        double denominator = n * sumX2 - sumX * sumX;
        
        if (Math.abs(denominator) < 1e-10) {
            // Points are collinear vertically, no slope
            return 0.0;
        }
        
        return (n * sumXY - sumX * sumY) / denominator;
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
            case RIGHT -> {
                // Trim from the end
                System.arraycopy(result, 0, trimmed, 0, originalLength);
            }
            case LEFT -> {
                // Trim from the beginning
                System.arraycopy(result, result.length - originalLength, trimmed, 0, originalLength);
            }
            case SYMMETRIC -> {
                // Trim equally from both sides
                int totalPadding = result.length - originalLength;
                int leftPad = totalPadding / 2;
                System.arraycopy(result, leftPad, trimmed, 0, originalLength);
            }
        }
        
        return trimmed;
    }
    
    @Override
    public String name() {
        return String.format("linear-%d-%s", fitPoints, mode.name().toLowerCase());
    }
    
    @Override
    public String description() {
        return String.format("Linear extrapolation padding (%d fit points, %s mode) - extrapolates based on edge slopes", 
                fitPoints, mode.name().toLowerCase());
    }
}
