package com.morphiqlabs.wavelet.exception;

import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.api.BoundaryMode;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Builder for creating enhanced error messages with debugging context.
 * 
 * <p>This class helps construct detailed error messages that include:</p>
 * <ul>
 *   <li>Contextual information about the operation being performed</li>
 *   <li>Details about the wavelet, signal, and configuration</li>
 *   <li>Remediation suggestions</li>
 *   <li>Error codes for programmatic handling</li>
 * </ul>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * throw new InvalidSignalException(
 *     ErrorCode.VAL_NOT_POWER_OF_TWO,
 *     ErrorContext.builder("Signal length must be power of two for DWT")
 *         .withSignalInfo(signal.length)
 *         .withWavelet(wavelet)
 *         .withSuggestion("Use MODWT for arbitrary length signals")
 *         .withSuggestion("Pad signal to nearest power of two: " + nextPowerOfTwo)
 *         .build()
 * );
 * }</pre>
 */
public class ErrorContext {
    
    private final String baseMessage;
    private final Map<String, Object> context = new LinkedHashMap<>();
    private final StringBuilder suggestions = new StringBuilder();
    private int suggestionCount = 0;
    
    private ErrorContext(String baseMessage) {
        this.baseMessage = baseMessage;
    }
    
    /**
     * Creates a new error context builder.
     * 
     * @param baseMessage The base error message
     * @return A new builder instance
     */
    public static ErrorContext builder(String baseMessage) {
        return new ErrorContext(baseMessage);
    }
    
    /**
     * Adds signal information to the error context.
     * 
     * @param signalLength The length of the signal
     * @return This builder for chaining
     */
    public ErrorContext withSignalInfo(int signalLength) {
        context.put("Signal length", signalLength);
        return this;
    }
    
    /**
     * Adds signal information with additional details.
     * 
     * @param signalLength The length of the signal
     * @param hasNonFinite Whether the signal contains NaN or Infinity
     * @return This builder for chaining
     */
    public ErrorContext withSignalInfo(int signalLength, boolean hasNonFinite) {
        context.put("Signal length", signalLength);
        if (hasNonFinite) {
            context.put("Signal validity", "Contains NaN or Infinity values");
        }
        return this;
    }
    
    /**
     * Adds wavelet information to the error context.
     * 
     * @param wavelet The wavelet being used
     * @return This builder for chaining
     */
    public ErrorContext withWavelet(Wavelet wavelet) {
        if (wavelet != null) {
            context.put("Wavelet", wavelet.name());
            context.put("Wavelet type", wavelet.getClass().getSimpleName());
            if (wavelet instanceof DiscreteWavelet) {
                DiscreteWavelet dw = (DiscreteWavelet) wavelet;
                context.put("Filter length", dw.lowPassDecomposition().length);
            }
        }
        return this;
    }
    
    /**
     * Adds boundary mode information.
     * 
     * @param mode The boundary mode
     * @return This builder for chaining
     */
    public ErrorContext withBoundaryMode(BoundaryMode mode) {
        if (mode != null) {
            context.put("Boundary mode", mode.name());
        }
        return this;
    }
    
    /**
     * Adds decomposition level information.
     * 
     * @param requestedLevel The requested decomposition level
     * @param maxLevel The maximum supported level
     * @return This builder for chaining
     */
    public ErrorContext withLevelInfo(int requestedLevel, int maxLevel) {
        context.put("Requested level", requestedLevel);
        context.put("Maximum level", maxLevel);
        return this;
    }
    
    /**
     * Adds arbitrary context information.
     * 
     * @param key The context key
     * @param value The context value
     * @return This builder for chaining
     */
    public ErrorContext withContext(String key, Object value) {
        context.put(key, value);
        return this;
    }
    
    /**
     * Adds a remediation suggestion.
     * 
     * @param suggestion The suggestion text
     * @return This builder for chaining
     */
    public ErrorContext withSuggestion(String suggestion) {
        if (suggestionCount > 0) {
            suggestions.append("\n   - ");
        } else {
            suggestions.append("\n\nSuggestions:\n   - ");
        }
        suggestions.append(suggestion);
        suggestionCount++;
        return this;
    }
    
    /**
     * Adds array dimension information.
     * 
     * @param expected Expected dimensions
     * @param actual Actual dimensions
     * @return This builder for chaining
     */
    public ErrorContext withArrayDimensions(String expected, String actual) {
        context.put("Expected dimensions", expected);
        context.put("Actual dimensions", actual);
        return this;
    }
    
    /**
     * Adds performance context for optimization errors.
     * 
     * @param operation The operation being performed
     * @param size The data size
     * @return This builder for chaining
     */
    public ErrorContext withPerformanceContext(String operation, int size) {
        context.put("Operation", operation);
        context.put("Data size", size);
        return this;
    }
    
    /**
     * Builds the final error message with all context.
     * 
     * @return The complete error message
     */
    public String build() {
        StringBuilder message = new StringBuilder(baseMessage);
        
        if (!context.isEmpty()) {
            message.append("\n\nContext:");
            context.forEach((key, value) -> 
                message.append("\n   ").append(key).append(": ").append(value)
            );
        }
        
        if (suggestionCount > 0) {
            message.append(suggestions);
        }
        
        return message.toString();
    }
    
    /**
     * Builds the error message and creates an exception.
     * 
     * @param errorCode The error code
     * @param exceptionClass The exception class to create
     * @param <T> The exception type
     * @return A new exception instance
     */
    public <T extends WaveletTransformException> T buildException(
            ErrorCode errorCode, Class<T> exceptionClass) {
        String message = build();
        try {
            return exceptionClass
                .getConstructor(ErrorCode.class, String.class)
                .newInstance(errorCode, message);
        } catch (Exception e) {
            // Fallback to base exception if specific type can't be created
            throw new WaveletTransformException(errorCode, message);
        }
    }
}