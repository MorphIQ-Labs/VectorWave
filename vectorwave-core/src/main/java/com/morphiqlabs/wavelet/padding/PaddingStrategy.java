package com.morphiqlabs.wavelet.padding;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Strategy for padding non-power-of-2 signals to the required length.
 *
 * <p>Each strategy defines how to extend a signal and optionally trim results.
 * This enables wavelet transforms on signals of arbitrary length by automatically
 * padding to the next power of 2.</p>
 *
 * <p>Common padding strategies:</p>
 * <ul>
 *   <li><b>Zero</b>: Extends with zeros (minimal spectral leakage)</li>
 *   <li><b>Symmetric</b>: Mirrors signal with boundary duplication (smooth extension)</li>
 *   <li><b>Reflect</b>: Mirrors signal without boundary duplication</li>
 *   <li><b>Periodic</b>: Wraps signal periodically (assumes cyclic data)</li>
 * </ul>
 */
public sealed interface PaddingStrategy
        permits ZeroPaddingStrategy, SymmetricPaddingStrategy,
        ReflectPaddingStrategy, PeriodicPaddingStrategy,
        ConstantPaddingStrategy, LinearExtrapolationStrategy, 
        AntisymmetricPaddingStrategy, PolynomialExtrapolationStrategy,
        StatisticalPaddingStrategy, AdaptivePaddingStrategy,
        CompositePaddingStrategy {

    /**
     * Pads the input signal to the target length.
     *
     * @param signal       the input signal (must not be null or empty)
     * @param targetLength the desired length (must be >= signal.length)
     * @return the padded signal of length targetLength
     * @throws IllegalArgumentException if targetLength {@literal <} signal.length
     */
    double[] pad(double[] signal, int targetLength);

    /**
     * Trims the result back to original length after inverse transform.
     *
     * <p>The default implementation simply truncates to the original length.
     * Strategies may override this to implement more sophisticated trimming.</p>
     *
     * @param result         the inverse transform result
     * @param originalLength the original signal length before padding
     * @return the trimmed result of length originalLength
     */
    default double[] trim(double[] result, int originalLength) {
        if (result.length == originalLength) {
            return result;
        }
        if (originalLength > result.length) {
            throw new InvalidArgumentException(
                    "Original length " + originalLength + " exceeds result length " + result.length);
        }
        double[] trimmed = new double[originalLength];
        System.arraycopy(result, 0, trimmed, 0, originalLength);
        return trimmed;
    }

    /**
     * Returns the name of this padding strategy.
     *
     * @return strategy name for display/configuration
     */
    String name();

    /**
     * Returns a human-readable description of this padding strategy.
     *
     * @return strategy description
     */
    default String description() {
        return name() + " padding";
    }
}
