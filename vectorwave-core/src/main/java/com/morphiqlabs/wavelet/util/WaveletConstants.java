package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Constants used throughout the wavelet transform implementation.
 *
 * <p>This class centralizes important constants to avoid duplication
 * and improve maintainability across the codebase.</p>
 */
public final class WaveletConstants {

    /**
     * The largest power of 2 that can be represented as a positive int in Java.
     * This is due to the two's complement representation of integers, where the
     * most significant bit is used as the sign bit. For a 32-bit signed integer,
     * 2^30 (1,073,741,824) is the largest power of two that remains positive.
     * The next power, 2^31, would overflow and be interpreted as a negative value.
     */
    public static final int MAX_SAFE_POWER_OF_TWO = 1 << 30;

    /**
     * Minimum size for wavelet decomposition. A signal cannot be decomposed
     * further once it reaches this size.
     */
    public static final int MIN_DECOMPOSITION_SIZE = 1;

    private WaveletConstants() {
        // Utility class, prevent instantiation
    }

    /**
     * Calculates the next power of two greater than or equal to n.
     * This method is used internally for validation and error messages.
     *
     * @param n the input number (must be positive and {@literal <=} MAX_SAFE_POWER_OF_TWO)
     * @return the next power of two >= n
     */
    public static int calculateNextPowerOfTwo(int n) {
        if (n <= 1) return 1;
        if (n > MAX_SAFE_POWER_OF_TWO) {
            throw new InvalidArgumentException("Input exceeds maximum safe power of two.");
        }
        // Find the next power of two using bit manipulation.
        // The formula works as follows:
        // 1. Subtract 1 from the input `n` to handle cases where `n` is already a power of two.
        // 2. Use `Integer.numberOfLeadingZeros(n - 1)` to count the number of leading zero bits in the 32-bit binary representation of `n - 1`.
        // 3. Subtract this count from 32 to determine the position of the highest set bit in `n - 1`.
        // 4. Shift the bit `1` left by this position to compute the next power of two.
        //
        // Example:
        // If n = 19:
        // Step 1: n - 1 = 18 (binary: 00000000 00000000 00000000 00010010)
        // Step 2: Integer.numberOfLeadingZeros(18) = 27 (since there are 27 leading zeros in the 32-bit representation)
        // Step 3: 32 - 27 = 5 (position of the highest set bit)
        // Step 4: 1 << 5 = 32 (binary: 00000000 00000000 00000000 00100000)
        // Result: The next power of two greater than or equal to 19 is 32.
        return 1 << (32 - Integer.numberOfLeadingZeros(n - 1));
    }
}
