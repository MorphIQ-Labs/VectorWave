package com.morphiqlabs.wavelet.padding;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import java.util.Arrays;

/**
 * Constant (edge) padding strategy that extends signals by repeating edge values.
 *
 * <p>This strategy extends the signal by replicating the first and last values.
 * It provides better continuity than zero padding for signals with non-zero
 * baseline or DC offset. Ideal for:</p>
 * <ul>
 *   <li>Financial time series with non-zero baseline</li>
 *   <li>Signals with significant DC offset</li>
 *   <li>Preserving signal mean near boundaries</li>
 *   <li>Reducing edge discontinuities</li>
 * </ul>
 *
 * <p>Example: {@code [1, 2, 3, 4]} padded to length 8 with RIGHT mode becomes
 * {@code [1, 2, 3, 4, 4, 4, 4, 4]}</p>
 *
 * <p>Example: {@code [1, 2, 3, 4]} padded to length 8 with SYMMETRIC mode becomes
 * {@code [1, 1, 1, 2, 3, 4, 4, 4]}</p>
 *
 * @param mode constant padding mode
 */
public record ConstantPaddingStrategy(PaddingMode mode) implements PaddingStrategy {
    
    /**
     * Padding mode determines where padding is applied.
     */
    public enum PaddingMode {
        /** Pad only on the right side */
        RIGHT,
        /** Pad equally on both sides (or favor right if odd) */
        SYMMETRIC,
        /** Pad only on the left side */
        LEFT
    }
    
    /**
     * Creates a constant padding strategy with default RIGHT mode.
     */
    public ConstantPaddingStrategy() {
        this(PaddingMode.RIGHT);
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
                // Copy original signal to the start
                System.arraycopy(signal, 0, padded, 0, signal.length);
                // Fill remaining with last value
                Arrays.fill(padded, signal.length, targetLength, signal[signal.length - 1]);
            }
            case LEFT -> {
                // Fill beginning with first value
                Arrays.fill(padded, 0, padLength, signal[0]);
                // Copy original signal after padding
                System.arraycopy(signal, 0, padded, padLength, signal.length);
            }
            case SYMMETRIC -> {
                // Calculate left and right padding
                int leftPad = padLength / 2;
                int rightPad = padLength - leftPad;
                
                // Fill left padding with first value
                Arrays.fill(padded, 0, leftPad, signal[0]);
                // Copy original signal
                System.arraycopy(signal, 0, padded, leftPad, signal.length);
                // Fill right padding with last value
                Arrays.fill(padded, leftPad + signal.length, targetLength, signal[signal.length - 1]);
            }
        }
        
        return padded;
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
        return "constant-" + mode.name().toLowerCase();
    }
    
    @Override
    public String description() {
        return String.format("Constant padding (%s mode) - extends signal by repeating edge values", 
                mode.name().toLowerCase());
    }
}
