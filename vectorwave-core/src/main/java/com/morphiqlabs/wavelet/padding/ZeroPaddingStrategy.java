package com.morphiqlabs.wavelet.padding;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Zero padding strategy that extends signals with zeros.
 *
 * <p>This is the simplest and most common padding strategy. It introduces
 * minimal spectral leakage and is computationally efficient. Ideal for:</p>
 * <ul>
 *   <li>Signals that naturally decay to zero</li>
 *   <li>Finite-duration signals</li>
 *   <li>When computational efficiency is paramount</li>
 * </ul>
 *
 * <p>Example: {@code [1, 2, 3, 4]} padded to length 8 becomes {@code [1, 2, 3, 4, 0, 0, 0, 0]}</p>
 */
public record ZeroPaddingStrategy() implements PaddingStrategy {

    @Override
    public double[] pad(double[] signal, int targetLength) {
        if (signal == null) {
            throw new InvalidArgumentException("Signal cannot be null");
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
        // Remaining elements are already zero (Java array initialization)
        return padded;
    }

    @Override
    public String name() {
        return "zero";
    }

    @Override
    public String description() {
        return "Zero padding - extends signal with zeros";
    }
}
