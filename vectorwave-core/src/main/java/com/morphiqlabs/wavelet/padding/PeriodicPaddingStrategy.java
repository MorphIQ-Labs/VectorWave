package com.morphiqlabs.wavelet.padding;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Periodic padding strategy that wraps the signal cyclically.
 *
 * <p>This strategy treats the signal as one period of a periodic function
 * and extends it by repeating. It is ideal for:</p>
 * <ul>
 *   <li>Naturally periodic signals (e.g., seasonal financial data)</li>
 *   <li>Signals extracted from cyclic phenomena</li>
 *   <li>Maintaining continuity at boundaries</li>
 * </ul>
 *
 * <p>Example: {@code [1, 2, 3, 4]} padded to length 8 becomes {@code [1, 2, 3, 4, 1, 2, 3, 4]}</p>
 */
public record PeriodicPaddingStrategy() implements PaddingStrategy {

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

        // Efficiently copy full periods
        int fullCopies = targetLength / signal.length;
        for (int i = 0; i < fullCopies; i++) {
            System.arraycopy(signal, 0, padded, i * signal.length, signal.length);
        }

        // Copy remaining partial period
        int remaining = targetLength % signal.length;
        if (remaining > 0) {
            System.arraycopy(signal, 0, padded, fullCopies * signal.length, remaining);
        }

        return padded;
    }

    @Override
    public String name() {
        return "periodic";
    }

    @Override
    public String description() {
        return "Periodic padding - wraps signal cyclically";
    }
}
