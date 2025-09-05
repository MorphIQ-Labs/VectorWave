package com.morphiqlabs.wavelet.exception;

/**
 * Exception thrown when an invalid argument is provided to utility methods.
 * This provides consistency with the wavelet transform exception hierarchy
 * while maintaining semantic clarity for argument validation errors.
 */
public class InvalidArgumentException extends WaveletTransformException {
    private static final long serialVersionUID = 202501150003L; // Invalid argument exception v1.0

    /**
     * Constructs a new invalid argument exception with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidArgumentException(String message) {
        super(message);
    }

    /**
     * Constructs a new invalid argument exception with the specified error code and detail message.
     *
     * @param errorCode the error code
     * @param message   the detail message
     */
    public InvalidArgumentException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates an exception for null arguments.
     *
     * @param parameterName the name of the null parameter
     * @return a new InvalidArgumentException
     */
    public static InvalidArgumentException nullArgument(String parameterName) {
        return new InvalidArgumentException(ErrorCode.VAL_NULL_ARGUMENT,
                parameterName + " cannot be null");
    }

    /**
     * Creates an exception for negative or zero input where positive is required.
     *
     * @param value the invalid value
     * @return a new InvalidArgumentException
     */
    public static InvalidArgumentException notPositive(int value) {
        return new InvalidArgumentException(ErrorCode.VAL_NOT_POSITIVE,
                String.format("Input must be positive, but was: %d", value));
    }

    /**
     * Creates an exception for input that is too large.
     *
     * @param value    the value that is too large
     * @param maxValue the maximum allowed value
     * @param context  additional context about why the limit exists
     * @return a new InvalidArgumentException
     */
    public static InvalidArgumentException tooLarge(int value, int maxValue, String context) {
        return new InvalidArgumentException(ErrorCode.VAL_TOO_LARGE,
                String.format("Input too large: %d. Maximum supported value is %d (2^30). %s",
                        value, maxValue, context));
    }
}