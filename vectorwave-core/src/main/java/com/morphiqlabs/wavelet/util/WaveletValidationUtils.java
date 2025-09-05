package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidConfigurationException;

/**
 * Utility class for wavelet-specific validation operations.
 * Provides centralized validation methods for wavelet parameters and configurations.
 */
public final class WaveletValidationUtils {

    /**
     * Private constructor to prevent instantiation.
     */
    private WaveletValidationUtils() {
        throw new AssertionError("Utility class - do not instantiate");
    }

    /**
     * Validates that a wavelet is not null.
     *
     * @param wavelet       the wavelet to validate
     * @param parameterName the parameter name for error messages
     * @throws InvalidArgumentException if wavelet is null
     */
    public static void validateWaveletNotNull(Wavelet wavelet, String parameterName) {
        if (wavelet == null) {
            throw InvalidArgumentException.nullArgument(parameterName);
        }
    }

    /**
     * Validates that a wavelet is discrete (not continuous).
     *
     * @param wavelet the wavelet to validate
     * @throws InvalidConfigurationException if wavelet is not discrete
     */
    public static void validateDiscreteWavelet(Wavelet wavelet) {
        validateWaveletNotNull(wavelet, "wavelet");

        if (!(wavelet instanceof DiscreteWavelet)) {
            throw InvalidConfigurationException.unsupportedOperation(
                    wavelet.getClass().getSimpleName(),
                    "discrete wavelet transform operations"
            );
        }
    }

    /**
     * Validates decomposition level bounds.
     *
     * @param level    the level to validate
     * @param maxLevel the maximum allowed level
     * @param context  additional context for error message
     * @throws InvalidArgumentException if level is out of bounds
     */
    public static void validateDecompositionLevel(int level, int maxLevel, String context) {
        if (level < 1) {
            throw new InvalidArgumentException(
                    String.format("Decomposition level must be at least 1, got: %d. %s", level, context)
            );
        }

        if (level > maxLevel) {
            throw new InvalidArgumentException(
                    String.format("Decomposition level %d exceeds maximum %d. %s", level, maxLevel, context)
            );
        }
    }

    /**
     * Validates coefficient arrays have matching lengths.
     *
     * @param approxLength length of approximation coefficients
     * @param detailLength length of detail coefficients
     * @param context      additional context for error message
     * @throws InvalidArgumentException if lengths don't match
     */
    public static void validateCoefficientLengths(int approxLength, int detailLength, String context) {
        if (approxLength != detailLength) {
            throw new InvalidArgumentException(
                    String.format("Coefficient arrays must have same length. " +
                            "Approximation: %d, Detail: %d. %s", approxLength, detailLength, context)
            );
        }
    }

    /**
     * Calculates the maximum decomposition levels for a given signal length and wavelet.
     *
     * @param signalLength the length of the signal
     * @param wavelet      the wavelet to use
     * @param maxAllowed   the maximum allowed levels from configuration
     * @return the maximum feasible decomposition levels
     */
    public static int calculateMaxDecompositionLevels(int signalLength, Wavelet wavelet, int maxAllowed) {
        validateDiscreteWavelet(wavelet);

        DiscreteWavelet discreteWavelet = (DiscreteWavelet) wavelet;
        int filterLength = discreteWavelet.lowPassDecomposition().length;

        int maxLevels = 0;
        int currentLength = signalLength;

        // Each level halves the length; stop when length < filter length
        while (currentLength >= filterLength && maxLevels < maxAllowed) {
            currentLength = (currentLength + 1) / 2;
            maxLevels++;
        }

        return Math.max(1, maxLevels);
    }
}