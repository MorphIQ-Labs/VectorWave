package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.exception.InvalidSignalException;

import static com.morphiqlabs.wavelet.util.WaveletConstants.MIN_DECOMPOSITION_SIZE;

/**
 * Batch validation utilities for multi-level wavelet transforms.
 *
 * <p>This class provides optimized validation for scenarios where multiple
 * signals or coefficient arrays need to be validated together, such as in
 * multi-level decomposition or reconstruction operations.</p>
 *
 * <p><strong>Performance Note:</strong> Batch validation reduces overhead by
 * performing common checks once and leveraging data locality for cache efficiency.</p>
 */
public final class BatchValidation {

    private BatchValidation() {
        // Utility class, prevent instantiation
    }

    /**
     * Validates multiple signals for multi-level transform operations.
     * This method is optimized for validating a hierarchy of coefficient arrays
     * produced by successive wavelet decompositions.
     *
     * @param signals         array of signals to validate
     * @param parameterNames  names for error reporting (must match signals length)
     * @param expectedLengths expected length for each signal (null to skip length check)
     * @throws InvalidSignalException if any validation fails
     */
    public static void validateMultiLevelSignals(double[][] signals,
                                                 String[] parameterNames,
                                                 int[] expectedLengths) {
        if (signals == null) {
            throw new InvalidSignalException("Signals array cannot be null.");
        }
        if (parameterNames == null || parameterNames.length != signals.length) {
            throw new InvalidSignalException("Parameter names must match signals length.");
        }

        // Batch null/empty checks
        for (int i = 0; i < signals.length; i++) {
            if (signals[i] == null) {
                throw InvalidSignalException.nullSignal(parameterNames[i]);
            }
            if (signals[i].length == 0) {
                throw InvalidSignalException.emptySignal(parameterNames[i]);
            }
        }

        // Batch length validation if specified
        if (expectedLengths != null) {
            if (expectedLengths.length != signals.length) {
                throw new InvalidSignalException("Expected lengths must match signals length.");
            }
            for (int i = 0; i < signals.length; i++) {
                if (signals[i].length != expectedLengths[i]) {
                    throw new InvalidSignalException(
                            String.format("%s has incorrect length. Expected: %d, actual: %d.",
                                    parameterNames[i], expectedLengths[i], signals[i].length));
                }
            }
        }

        // Batch finite value checks - optimized for cache locality
        for (int i = 0; i < signals.length; i++) {
            ValidationUtils.validateFiniteValues(signals[i], parameterNames[i]);
        }
    }

    /**
     * Validates coefficient pairs for multi-level transforms.
     * Optimized for validating approximation and detail coefficient pairs
     * at multiple decomposition levels.
     *
     * @param approxCoeffs array of approximation coefficients at each level
     * @param detailCoeffs array of detail coefficients at each level
     * @param levelNames   names of each level for error reporting
     * @throws InvalidSignalException if validation fails
     */
    public static void validateMultiLevelCoefficients(double[][] approxCoeffs,
                                                      double[][] detailCoeffs,
                                                      String[] levelNames) {
        if (approxCoeffs == null || detailCoeffs == null) {
            throw new InvalidSignalException("Coefficient arrays cannot be null.");
        }
        if (approxCoeffs.length != detailCoeffs.length) {
            throw new InvalidSignalException("Approximation and detail arrays must have same number of levels.");
        }
        if (levelNames == null || levelNames.length != approxCoeffs.length) {
            throw new InvalidSignalException("Level names must match number of levels.");
        }

        // Validate each level
        for (int level = 0; level < approxCoeffs.length; level++) {
            String approxName = levelNames[level] + " approximation";
            String detailName = levelNames[level] + " detail";

            // Null/empty checks
            if (approxCoeffs[level] == null) {
                throw InvalidSignalException.nullSignal(approxName);
            }
            if (detailCoeffs[level] == null) {
                throw InvalidSignalException.nullSignal(detailName);
            }
            if (approxCoeffs[level].length == 0) {
                throw InvalidSignalException.emptySignal(approxName);
            }
            if (detailCoeffs[level].length == 0) {
                throw InvalidSignalException.emptySignal(detailName);
            }

            // Length matching
            if (approxCoeffs[level].length != detailCoeffs[level].length) {
                throw InvalidSignalException.mismatchedCoefficients(
                        approxCoeffs[level].length, detailCoeffs[level].length);
            }

            // Finite values - batch check both arrays
            ValidationUtils.validateFiniteValues(approxCoeffs[level], approxName);
            ValidationUtils.validateFiniteValues(detailCoeffs[level], detailName);
        }
    }

    /**
     * Validates transform levels for consistency.
     * Ensures that coefficient arrays follow the expected halving pattern
     * for multi-level decomposition.
     *
     * @param levels        array where levels[i] contains coefficients at level i
     * @param initialLength the length of the original signal
     * @throws InvalidSignalException if levels don't follow expected pattern
     */
    public static void validateLevelConsistency(double[][] levels, int initialLength) {
        if (levels == null || levels.length == 0) {
            throw new InvalidSignalException("Levels array cannot be null or empty.");
        }

        int expectedLength = initialLength / 2;
        for (int i = 0; i < levels.length; i++) {
            if (levels[i] == null) {
                throw InvalidSignalException.nullSignal("Level " + i);
            }
            if (levels[i].length != expectedLength) {
                throw new InvalidSignalException(
                        String.format("Level %d has incorrect length. Expected: %d, actual: %d.",
                                i, expectedLength, levels[i].length));
            }
            expectedLength /= 2;

            // Ensure we don't go below minimum decomposition size
            if (expectedLength < MIN_DECOMPOSITION_SIZE && i < levels.length - 1) {
                throw new InvalidSignalException(
                        "Too many decomposition levels for signal length " + initialLength + ".");
            }
        }
    }

    /**
     * Creates expected lengths array for multi-level decomposition.
     *
     * @param initialLength the original signal length
     * @param levels        number of decomposition levels
     * @return array of expected lengths at each level
     */
    public static int[] computeExpectedLengths(int initialLength, int levels) {
        int[] lengths = new int[levels];
        int currentLength = initialLength;

        for (int i = 0; i < levels; i++) {
            currentLength /= 2;
            lengths[i] = currentLength;
        }

        return lengths;
    }
}