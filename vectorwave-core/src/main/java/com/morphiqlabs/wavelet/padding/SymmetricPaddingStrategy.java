package com.morphiqlabs.wavelet.padding;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Symmetric padding strategy that mirrors the signal at boundaries.
 *
 * <p>This strategy reflects the signal at its boundaries, including the
 * boundary points. It provides smooth extensions and is ideal for:</p>
 * <ul>
 *   <li>Image processing applications</li>
 *   <li>Signals with smooth boundaries</li>
 *   <li>Reducing edge artifacts in transforms</li>
 * </ul>
 *
 * <p>Example: {@code [1, 2, 3, 4]} padded to length 8 becomes {@code [1, 2, 3, 4, 4, 3, 2, 1]}</p>
 */
public record SymmetricPaddingStrategy() implements PaddingStrategy {

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
        System.arraycopy(signal, 0, padded, 0, signal.length);

        int padLength = targetLength - signal.length;
        for (int i = 0; i < padLength; i++) {
            // Mirror including boundary: [a, b, c, d] -> [a, b, c, d, d, c, b, a, a, b, c, d, ...]
            int mirrorIndex = i % (2 * signal.length);
            if (mirrorIndex < signal.length) {
                // Forward part of the mirror
                padded[signal.length + i] = signal[signal.length - 1 - mirrorIndex];
            } else {
                // Backward part of the mirror
                padded[signal.length + i] = signal[mirrorIndex - signal.length];
            }
        }

        return padded;
    }

    @Override
    public String name() {
        return "symmetric";
    }

    @Override
    public String description() {
        return "Symmetric padding - mirrors signal with boundary duplication";
    }
}
