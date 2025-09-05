package com.morphiqlabs.wavelet.exception;

import static com.morphiqlabs.wavelet.util.WaveletConstants.MAX_SAFE_POWER_OF_TWO;
import static com.morphiqlabs.wavelet.util.WaveletConstants.calculateNextPowerOfTwo;

/**
 * Exception thrown when an invalid signal is provided to wavelet operations.
 * This includes signals with invalid lengths, NaN/Infinity values, or null signals.
 */
public class InvalidSignalException extends WaveletTransformException {
    private static final long serialVersionUID = 202501150002L; // Invalid signal exception v1.0

    /**
     * Constructs a new invalid signal exception with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidSignalException(String message) {
        super(message);
    }

    /**
     * Constructs a new invalid signal exception with the specified error code and detail message.
     *
     * @param errorCode the error code
     * @param message   the detail message
     */
    public InvalidSignalException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates an exception for null signal input.
     *
     * @param parameterName the name of the parameter that was null
     * @return a new InvalidSignalException
     */
    public static InvalidSignalException nullSignal(String parameterName) {
        return new InvalidSignalException(ErrorCode.VAL_NULL_ARGUMENT,
                String.format("%s cannot be null.", parameterName));
    }

    /**
     * Creates an exception for empty signal input.
     *
     * @param parameterName the name of the parameter that was empty
     * @return a new InvalidSignalException
     */
    public static InvalidSignalException emptySignal(String parameterName) {
        return new InvalidSignalException(ErrorCode.VAL_EMPTY,
                String.format("%s cannot be empty.", parameterName));
    }

    /**
     * Creates an exception for signal length that is not a power of two.
     *
     * @param actualLength the actual length of the signal
     * @return a new InvalidSignalException
     */
    public static InvalidSignalException notPowerOfTwo(int actualLength) {
        final String baseMessage = "Signal length must be a power of two. Found length: %d.";
        String message = String.format(baseMessage, actualLength);

        if (actualLength <= 0) {
            // No suggestion for non-positive lengths
            return new InvalidSignalException(ErrorCode.VAL_NOT_POWER_OF_TWO, message);
        }

        // Calculate next power of two suggestion
        if (actualLength > 0 && actualLength <= MAX_SAFE_POWER_OF_TWO) {
            int nextPower = calculateNextPowerOfTwo(actualLength);
            message = String.format("%s Consider padding the signal to length %d.", message, nextPower);
        } else if (actualLength > MAX_SAFE_POWER_OF_TWO) {
            message = String.format("%s The signal is too large to pad to the next power of two.", message);
        }

        return new InvalidSignalException(ErrorCode.VAL_NOT_POWER_OF_TWO, message);
    }

    /**
     * Creates an exception for signal containing NaN values.
     *
     * @param parameterName the name of the parameter containing NaN
     * @param index         the index where NaN was found
     * @return a new InvalidSignalException
     */
    public static InvalidSignalException nanValue(String parameterName, int index) {
        return new InvalidSignalException(ErrorCode.VAL_NON_FINITE_VALUES,
                String.format("%s contains NaN at index %d. All values must be finite numbers.", parameterName, index));
    }

    /**
     * Creates an exception for signal containing infinity values.
     *
     * @param parameterName the name of the parameter containing infinity
     * @param index         the index where infinity was found
     * @param value         the infinite value (positive or negative infinity)
     * @return a new InvalidSignalException
     */
    public static InvalidSignalException infinityValue(String parameterName, int index, double value) {
        String sign = value > 0 ? "positive" : "negative";
        return new InvalidSignalException(ErrorCode.VAL_NON_FINITE_VALUES,
                String.format("%s contains %s infinity at index %d. All values must be finite numbers.",
                        parameterName, sign, index));
    }

    /**
     * Creates an exception for mismatched coefficient array lengths.
     *
     * @param approxLength length of approximation coefficients
     * @param detailLength length of detail coefficients
     * @return a new InvalidSignalException
     */
    public static InvalidSignalException mismatchedCoefficients(int approxLength, int detailLength) {
        return new InvalidSignalException(ErrorCode.VAL_LENGTH_MISMATCH,
                String.format("Approximation and detail coefficient arrays must have the same length. " +
                        "Found approximation length: %d, detail length: %d.", approxLength, detailLength));
    }

}