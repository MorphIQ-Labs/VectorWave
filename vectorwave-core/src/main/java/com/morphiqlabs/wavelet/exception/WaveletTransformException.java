package com.morphiqlabs.wavelet.exception;

/**
 * Base exception for all wavelet transform related errors.
 * This is the parent class for more specific wavelet exceptions.
 *
 * <p>Supports optional error codes for programmatic error handling.</p>
 */
public class WaveletTransformException extends RuntimeException {
    private static final long serialVersionUID = 202501150001L; // Base exception class v1.0

    private final ErrorCode errorCode;

    /**
     * Constructs a new wavelet transform exception with the specified detail message.
     *
     * @param message the detail message
     */
    public WaveletTransformException(String message) {
        super(message);
        this.errorCode = null;
    }

    /**
     * Constructs a new wavelet transform exception with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public WaveletTransformException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    /**
     * Constructs a new wavelet transform exception with the specified error code and detail message.
     *
     * @param errorCode the error code
     * @param message   the detail message
     */
    public WaveletTransformException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a new wavelet transform exception with the specified error code, detail message and cause.
     *
     * @param errorCode the error code
     * @param message   the detail message
     * @param cause     the cause
     */
    public WaveletTransformException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Gets the error code associated with this exception.
     *
     * @return the error code, or null if no code was specified
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }

    /**
     * Checks if this exception has an error code.
     *
     * @return true if an error code is present
     */
    public boolean hasErrorCode() {
        return errorCode != null;
    }
}