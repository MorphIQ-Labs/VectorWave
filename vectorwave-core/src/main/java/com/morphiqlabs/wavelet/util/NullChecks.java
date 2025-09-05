package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Utility class for consistent null checking across the codebase.
 *
 * <p>This class provides a standardized approach to null validation,
 * ensuring consistent error messages and exception types throughout
 * the wavelet transform library.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * public void process(Wavelet wavelet, double[] signal) {
 *     NullChecks.requireNonNull(wavelet, "wavelet");
 *     NullChecks.requireNonNull(signal, "signal");
 *     // ... rest of method
 * }
 * }</pre>
 */
public final class NullChecks {
    private NullChecks() {
        throw new AssertionError("No instances");
    }

    /**
     * Checks that the specified object reference is not {@code null}.
     *
     * <p>This method is designed to be a drop-in replacement for
     * {@code Objects.requireNonNull} but throws our custom exception
     * with error codes.</p>
     *
     * @param obj           the object reference to check for nullity
     * @param parameterName the name of the parameter (used in error message)
     * @param <T>           the type of the reference
     * @return {@code obj} if not {@code null}
     * @throws InvalidArgumentException if {@code obj} is {@code null}
     */
    public static <T> T requireNonNull(T obj, String parameterName) {
        if (obj == null) {
            throw InvalidArgumentException.nullArgument(parameterName);
        }
        return obj;
    }

    /**
     * Checks that the specified array reference is not {@code null} and not empty.
     *
     * @param array         the array to check
     * @param parameterName the name of the parameter (used in error message)
     * @return the array if not null and not empty
     * @throws InvalidArgumentException if array is null or empty
     */
    public static double[] requireNonEmpty(double[] array, String parameterName) {
        requireNonNull(array, parameterName);
        if (array.length == 0) {
            throw new InvalidArgumentException(parameterName + " cannot be empty");
        }
        return array;
    }

    /**
     * Checks that the specified array reference is not {@code null} and not empty.
     *
     * @param array         the array to check
     * @param parameterName the name of the parameter (used in error message)
     * @param <T>           the component type of the array
     * @return the array if not null and not empty
     * @throws InvalidArgumentException if array is null or empty
     */
    public static <T> T[] requireNonEmpty(T[] array, String parameterName) {
        requireNonNull(array, parameterName);
        if (array.length == 0) {
            throw new InvalidArgumentException(parameterName + " cannot be empty");
        }
        return array;
    }

    /**
     * Checks that two object references are both non-null.
     *
     * @param obj1       the first object reference to check
     * @param paramName1 the name of the first parameter
     * @param obj2       the second object reference to check
     * @param paramName2 the name of the second parameter
     * @param <T>        the type of the first reference
     * @param <U>        the type of the second reference
     * @throws InvalidArgumentException if either object is null
     */
    public static <T, U> void requireBothNonNull(T obj1, String paramName1, U obj2, String paramName2) {
        requireNonNull(obj1, paramName1);
        requireNonNull(obj2, paramName2);
    }

    /**
     * Checks that all elements in an array are non-null.
     *
     * @param array     the array to check
     * @param arrayName the name of the array parameter
     * @param <T>       the component type of the array
     * @return the array if all elements are non-null
     * @throws InvalidArgumentException if array is null or contains null elements
     */
    public static <T> T[] requireNoNullElements(T[] array, String arrayName) {
        requireNonNull(array, arrayName);
        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                throw new InvalidArgumentException(
                        String.format("%s[%d] cannot be null", arrayName, i));
            }
        }
        return array;
    }
}
