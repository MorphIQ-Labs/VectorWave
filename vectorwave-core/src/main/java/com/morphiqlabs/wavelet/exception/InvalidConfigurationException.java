package com.morphiqlabs.wavelet.exception;

/**
 * Exception thrown when an invalid configuration is provided.
 * This includes conflicting settings, unsupported combinations, or invalid parameter values.
 */
public class InvalidConfigurationException extends WaveletTransformException {
    private static final long serialVersionUID = 202501150004L; // Invalid configuration exception v1.0

    /**
     * Constructs a new invalid configuration exception with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new invalid configuration exception with the specified error code and detail message.
     *
     * @param errorCode the error code
     * @param message   the detail message
     */
    public InvalidConfigurationException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     * Creates an exception for conflicting configuration options.
     *
     * @param option1 the first conflicting option
     * @param option2 the second conflicting option
     * @return a new InvalidConfigurationException
     */
    public static InvalidConfigurationException conflictingOptions(String option1, String option2) {
        return new InvalidConfigurationException(ErrorCode.CFG_CONFLICTING_OPTIONS,
                String.format("Conflicting configuration: both %s and %s cannot be set", option1, option2));
    }

    /**
     * Creates an exception for unsupported wavelet operations.
     *
     * @param waveletType the type of wavelet
     * @param operation   the unsupported operation
     * @return a new InvalidConfigurationException
     */
    public static InvalidConfigurationException unsupportedOperation(String waveletType, String operation) {
        return new InvalidConfigurationException(ErrorCode.CFG_UNSUPPORTED_OPERATION,
                String.format("%s wavelets do not support %s", waveletType, operation));
    }

    /**
     * Creates an exception for unsupported boundary modes.
     *
     * @param boundaryMode the unsupported boundary mode
     * @return a new InvalidConfigurationException
     */
    public static InvalidConfigurationException unsupportedBoundaryMode(String boundaryMode) {
        return new InvalidConfigurationException(ErrorCode.CFG_UNSUPPORTED_BOUNDARY_MODE,
                String.format("Boundary mode %s is not supported", boundaryMode));
    }
}