package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidSignalException;

import static com.morphiqlabs.wavelet.util.WaveletConstants.MAX_SAFE_POWER_OF_TWO;
import static com.morphiqlabs.wavelet.util.WaveletConstants.calculateNextPowerOfTwo;

/**
 * Utility class for validating wavelet transform inputs.
 *
 * <p><strong>Performance Note:</strong> This class is designed with performance as a priority.
 * Some methods have preconditions that must be met by callers to avoid redundant checks.
 * Methods clearly document when they do NOT perform null/empty checks for performance reasons.
 * Callers are responsible for ensuring preconditions are met or calling appropriate validation
 * methods in the correct order.</p>
 */
public final class ValidationUtils {

    /**
     * Maximum signal length for optimized validation path.
     * Signals up to this size use a single-pass validation for better performance.
     * This threshold is chosen based on typical cache sizes and performance characteristics.
     */
    private static final int SMALL_SIGNAL_THRESHOLD = 1024;

    private ValidationUtils() {
        throw new AssertionError("No instances");
    }

    /**
     * Returns the largest power of 2 that can be represented as a positive int in Java.
     * This is useful for validation logic that requires checking against this limit.
     *
     * @return the largest safe power of 2 as a positive int
     */
    public static int getMaxSafePowerOfTwo() {
        return MAX_SAFE_POWER_OF_TWO;
    }

    /**
     * Validates that a signal is suitable for wavelet transform.
     * Checks for null, empty, minimum length, power-of-two length, and finite values.
     *
     * <p>This method performs validation in the following order:
     * <ol>
     *   <li>Null and empty checks via {@link #validateNotNullOrEmpty(double[], String)}</li>
     *   <li>Minimum length check (must be at least 2)</li>
     *   <li>Power-of-two length validation</li>
     *   <li>Finite value checks via {@link #validateFiniteValues(double[], String)}</li>
     * </ol>
     * The ordering ensures that {@code validateFiniteValues}'s precondition (non-null, non-empty array)
     * is satisfied by the prior null/empty validation.
     *
     * @param signal        the signal to validate
     * @param parameterName the name of the parameter for error messages
     * @throws InvalidSignalException if the signal is invalid
     */
    public static void validateSignal(double[] signal, String parameterName) {
        // Optimized path for small signals: combine all checks in single pass
        if (signal != null && isPowerOfTwo(signal.length) && signal.length >= 2 && signal.length <= SMALL_SIGNAL_THRESHOLD) {
            // Fast path: single pass validation for small power-of-2 signals
            for (int i = 0; i < signal.length; i++) {
                double value = signal[i];
                if (Double.isNaN(value)) {
                    throw InvalidSignalException.nanValue(parameterName, i);
                }
                if (Double.isInfinite(value)) {
                    throw InvalidSignalException.infinityValue(parameterName, i, value);
                }
            }
            return;
        }

        // Standard validation path for large signals or when fast path conditions not met
        // Check null and empty using common method
        validateNotNullOrEmpty(signal, parameterName);

        // Check minimum length (wavelet transform requires at least 2 samples)
        if (signal.length < 2) {
            throw new InvalidSignalException("Signal must have at least 2 samples for wavelet transform, but has " + signal.length);
        }

        // Check power of two
        if (!isPowerOfTwo(signal.length)) {
            throw InvalidSignalException.notPowerOfTwo(signal.length);
        }

        // Check for NaN and infinity (precondition satisfied by validateNotNullOrEmpty above)
        validateFiniteValues(signal, parameterName);
    }

    /**
     * Validates that all values in an array are finite (not NaN or infinity).
     *
     * <strong>Precondition:</strong> The caller MUST ensure the array is not null or empty.
     * This method does NOT check for null/empty arrays for performance reasons.
     * Call validateNotNullOrEmpty() before this method if the array hasn't been validated.
     *
     * @param values        the array to validate (must not be null or empty)
     * @param parameterName the name of the parameter for error messages
     * @throws InvalidSignalException         if any value is NaN or infinity
     * @throws NullPointerException           if values is null (unchecked)
     * @throws ArrayIndexOutOfBoundsException if values is empty (unchecked)
     */
    public static void validateFiniteValues(double[] values, String parameterName) {
        int index = 0;
        for (double value : values) {
            if (Double.isNaN(value)) {
                throw InvalidSignalException.nanValue(parameterName, index);
            }
            if (Double.isInfinite(value)) {
                throw InvalidSignalException.infinityValue(parameterName, index, value);
            }
            index++;
        }
    }

    /**
     * Validates that an array is not null and not empty.
     *
     * @param array         the array to validate
     * @param parameterName the name of the parameter for error messages
     * @throws InvalidSignalException if the array is null or empty
     */
    public static void validateNotNullOrEmpty(double[] array, String parameterName) {
        if (array == null) {
            throw InvalidSignalException.nullSignal(parameterName);
        }
        if (array.length == 0) {
            throw InvalidSignalException.emptySignal(parameterName);
        }
    }

    /**
     * Validates that two arrays have matching lengths.
     *
     * <strong>Precondition:</strong> The caller MUST ensure both arrays are not null.
     * This method does NOT check for null arrays for performance reasons.
     * Call validateNotNullOrEmpty() on both arrays before this method if they haven't been validated.
     *
     * @param approxCoeffs approximation coefficients (must not be null)
     * @param detailCoeffs detail coefficients (must not be null)
     * @throws InvalidSignalException if the arrays have different lengths
     * @throws NullPointerException   if either array is null (unchecked)
     */
    public static void validateMatchingLengths(double[] approxCoeffs, double[] detailCoeffs) {
        if (approxCoeffs.length != detailCoeffs.length) {
            throw InvalidSignalException.mismatchedCoefficients(approxCoeffs.length, detailCoeffs.length);
        }
    }

    /**
     * Checks if a number is a power of two.
     *
     * <p>This method uses a bit manipulation algorithm to determine if the input
     * number {@code n} is a power of two. The algorithm works as follows:
     * <ul>
     *   <li>{@code n > 0} ensures that the number is positive, as negative numbers
     *       and zero cannot be powers of two.</li>
     *   <li>{@code (n & (n - 1)) == 0} checks if {@code n} has exactly one bit set
     *       in its binary representation. For powers of two, this condition is true
     *       because subtracting 1 from a power of two flips all the bits after the
     *       single set bit, resulting in no overlap when ANDed with the original number.</li>
     * </ul>
     * For example:
     * <pre>
     * n = 4 (binary 100), n - 1 = 3 (binary 011), n {@literal &} (n - 1) = 0
     * n = 5 (binary 101), n - 1 = 4 (binary 100), n {@literal &} (n - 1) = 4 (not 0)
     * </pre>
     *
     * @param n the number to check
     * @return true if {@code n} is a power of two, false otherwise
     */
    public static boolean isPowerOfTwo(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    /**
     * Checks if the next power of two calculation would overflow for the given input.
     *
     * @param n the input number
     * @return true if nextPowerOfTwo(n) would overflow, false otherwise
     */
    public static boolean wouldNextPowerOfTwoOverflow(int n) {
        return n > MAX_SAFE_POWER_OF_TWO;
    }

    /**
     * Calculates the next power of two greater than or equal to n.
     *
     * @param n the input number (must be positive)
     * @return the next power of two >= n
     * @throws InvalidArgumentException if n {@literal <} 1 or n {@literal >} 2^30 (1,073,741,824)
     */
    public static int nextPowerOfTwo(int n) {
        if (n < 1) {
            throw InvalidArgumentException.notPositive(n);
        }

        try {
            return calculateNextPowerOfTwo(n);
        } catch (IllegalArgumentException e) {
            // Convert to our exception type
            throw InvalidArgumentException.tooLarge(n, MAX_SAFE_POWER_OF_TWO,
                    "Next power of two would be 2^31, which cannot be represented as a positive int.");
        }
    }

    /**
     * Validates that a block size is a power of two, as required for wavelet transforms.
     * This method provides a centralized validation with consistent error messaging
     * for streaming wavelet components.
     *
     * @param blockSize     the block size to validate
     * @param componentName the name of the component for error messages (e.g., "StreamingDenoiser")
     * @throws InvalidArgumentException if the block size is not a power of two
     */
    public static void validateBlockSizeForWavelet(int blockSize, String componentName) {
        if (!isPowerOfTwo(blockSize)) {
            throw new InvalidArgumentException(
                    String.format("%s requires block size to be a power of 2, got: %d",
                            componentName, blockSize));
        }
    }
}
