package com.morphiqlabs.wavelet.padding;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Reflect padding strategy that mirrors the signal without boundary duplication.
 *
 * <p>This strategy reflects the signal at its boundaries, excluding the
 * boundary points. It avoids discontinuities and is ideal for:</p>
 * <ul>
 *   <li>Smooth signal extensions</li>
 *   <li>Avoiding boundary point duplication</li>
 *   <li>Signals where boundary values should not be emphasized</li>
 * </ul>
 *
 * <p>Example: {@code [1, 2, 3, 4]} padded to length 8 becomes {@code [1, 2, 3, 4, 3, 2, 1, 2]}</p>
 */
public record ReflectPaddingStrategy() implements PaddingStrategy {

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

        // Handle special case of single-element signal
        if (signal.length == 1) {
            double[] padded = new double[targetLength];
            for (int i = 0; i < targetLength; i++) {
                padded[i] = signal[0];
            }
            return padded;
        }

        double[] padded = new double[targetLength];
        System.arraycopy(signal, 0, padded, 0, signal.length);

        int padLength = targetLength - signal.length;
        int period = 2 * (signal.length - 1); // Period of reflection pattern

        for (int i = 0; i < padLength; i++) {
            int pos = i % period;
            if (pos < signal.length - 1) {
                // Forward part: reflect from end (excluding last element)
                padded[signal.length + i] = signal[signal.length - 2 - pos];
            } else {
                // Backward part: reflect from start (excluding first element)
                padded[signal.length + i] = signal[pos - signal.length + 2];
            }
        }

        return padded;
    }

    @Override
    public String name() {
        return "reflect";
    }

    @Override
    public String description() {
        return "Reflect padding - mirrors signal without boundary duplication";
    }
}
