package com.morphiqlabs.wavelet.exception;

/**
 * Error codes for wavelet transform exceptions.
 *
 * <p>These codes provide a programmatic way to identify specific error conditions
 * without parsing error messages. Each error code has a unique identifier and
 * category prefix:</p>
 *
 * <ul>
 *   <li>VAL_xxx - Validation errors</li>
 *   <li>CFG_xxx - Configuration errors</li>
 *   <li>SIG_xxx - Signal processing errors</li>
 *   <li>STATE_xxx - State-related errors</li>
 *   <li>POOL_xxx - Resource pool errors</li>
 * </ul>
 */
public enum ErrorCode {

    // Validation errors (VAL_xxx)
    /**
     * Null argument provided where non-null expected
     */
    VAL_NULL_ARGUMENT("VAL_001", "Null argument"),

    /**
     * Signal length is not a power of two
     */
    VAL_NOT_POWER_OF_TWO("VAL_002", "Signal length not power of two"),

    /**
     * Signal contains non-finite values (NaN or Infinity)
     */
    VAL_NON_FINITE_VALUES("VAL_003", "Signal contains non-finite values"),

    /**
     * Value is not positive where positive required
     */
    VAL_NOT_POSITIVE("VAL_004", "Value must be positive"),

    /**
     * Value exceeds maximum allowed
     */
    VAL_TOO_LARGE("VAL_005", "Value too large"),

    /**
     * Empty array or collection
     */
    VAL_EMPTY("VAL_006", "Empty array or collection"),

    /**
     * Array length mismatch
     */
    VAL_LENGTH_MISMATCH("VAL_007", "Array length mismatch"),

    // Configuration errors (CFG_xxx)
    /**
     * Unsupported operation for given configuration
     */
    CFG_UNSUPPORTED_OPERATION("CFG_001", "Unsupported operation"),

    /**
     * Conflicting configuration options
     */
    CFG_CONFLICTING_OPTIONS("CFG_002", "Conflicting options"),

    /**
     * Unsupported boundary mode
     */
    CFG_UNSUPPORTED_BOUNDARY_MODE("CFG_003", "Unsupported boundary mode"),

    /**
     * Invalid decomposition level
     */
    CFG_INVALID_DECOMPOSITION_LEVEL("CFG_004", "Invalid decomposition level"),

    // Signal processing errors (SIG_xxx)
    /**
     * Signal too short for operation
     */
    SIG_TOO_SHORT("SIG_001", "Signal too short"),

    /**
     * Maximum decomposition level exceeded
     */
    SIG_MAX_LEVEL_EXCEEDED("SIG_002", "Maximum decomposition level exceeded"),

    /**
     * Invalid threshold value
     */
    SIG_INVALID_THRESHOLD("SIG_003", "Invalid threshold value"),

    // State errors (STATE_xxx)
    /**
     * Resource is closed
     */
    STATE_CLOSED("STATE_001", "Resource closed"),

    /**
     * Invalid state for operation
     */
    STATE_INVALID("STATE_002", "Invalid state"),

    /**
     * Operation already in progress
     */
    STATE_IN_PROGRESS("STATE_003", "Operation in progress"),

    // Pool errors (POOL_xxx)
    /**
     * Pool is full
     */
    POOL_FULL("POOL_001", "Pool full"),

    /**
     * Pool is exhausted
     */
    POOL_EXHAUSTED("POOL_002", "Pool exhausted");

    private final String code;
    private final String description;

    ErrorCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Gets the error code string.
     *
     * @return the error code (e.g., "VAL_001")
     */
    public String getCode() {
        return code;
    }

    /**
     * Gets a brief description of the error.
     *
     * @return the error description
     */
    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return code + ": " + description;
    }
}