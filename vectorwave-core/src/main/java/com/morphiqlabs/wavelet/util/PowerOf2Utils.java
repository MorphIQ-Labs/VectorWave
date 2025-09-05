package com.morphiqlabs.wavelet.util;

/**
 * Utility class for power-of-2 operations commonly used in FFT and signal processing.
 * 
 * <p>This class consolidates power-of-2 calculations that were previously duplicated
 * across FFT and signal utilities. All methods use efficient
 * bit manipulation operations for optimal performance.</p>
 */
public final class PowerOf2Utils {
    
    /**
     * Returns the smallest power of 2 that is greater than or equal to n.
     * 
     * <p>Uses efficient bit manipulation instead of loops. The algorithm works by:
     * <ul>
     * <li>For n ≤ 1, returns 1</li>
     * <li>For n {@literal >} 1, finds the highest set bit in (n-1) and shifts left by 1</li>
     * </ul>
     * 
     * <p>Examples:
     * <ul>
     * <li>nextPowerOf2(1) = 1</li>
     * <li>nextPowerOf2(2) = 2</li>
     * <li>nextPowerOf2(3) = 4</li>
     * <li>nextPowerOf2(5) = 8</li>
     * <li>nextPowerOf2(16) = 16</li>
     * <li>nextPowerOf2(17) = 32</li>
     * </ul>
     * 
     * @param n the input value
     * @return the smallest power of 2 ≥ n
     * @throws IllegalArgumentException if n > 2^30 (would overflow)
     */
    public static int nextPowerOf2(int n) {
        if (n <= 0) {
            return 1;
        }
        if (n > (1 << 30)) {
            throw new IllegalArgumentException("Input too large, would overflow: " + n);
        }
        return n <= 1 ? 1 : Integer.highestOneBit(n - 1) << 1;
    }
    
    /**
     * Checks if n is a power of 2.
     * 
     * <p>Uses the bit trick that powers of 2 have exactly one bit set,
     * so n {@literal &} (n-1) == 0 for all powers of 2.</p>
     * 
     * @param n the value to check
     * @return true if n is a power of 2, false otherwise
     */
    public static boolean isPowerOf2(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }
    
    /**
     * Returns the log base 2 of n, assuming n is a power of 2.
     * 
     * <p>This is equivalent to finding the position of the single set bit.
     * For non-powers of 2, returns the position of the highest set bit.</p>
     * 
     * @param n a power of 2
     * @return log2(n)
     * @throws IllegalArgumentException if n ≤ 0
     */
    public static int log2(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Input must be positive: " + n);
        }
        return 31 - Integer.numberOfLeadingZeros(n);
    }
    
    /**
     * Returns the largest power of 2 that is less than or equal to n.
     * 
     * <p>This is the "floor" power of 2, useful for finding the highest
     * power-of-2 factor of a number.</p>
     * 
     * @param n the input value
     * @return the largest power of 2 ≤ n
     * @throws IllegalArgumentException if n ≤ 0
     */
    public static int previousPowerOf2(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("Input must be positive: " + n);
        }
        return Integer.highestOneBit(n);
    }
    
    /**
     * Performs modulo operation optimized for power-of-2 divisors.
     * 
     * <p>When the divisor is a power of 2, x % powerOf2 can be computed
     * as x {@literal &} (powerOf2 - 1), which is much faster than division.</p>
     * 
     * @param x the dividend
     * @param powerOf2 the divisor (must be a power of 2)
     * @return x % powerOf2
     * @throws IllegalArgumentException if powerOf2 is not a power of 2
     */
    public static int moduloPowerOf2(int x, int powerOf2) {
        if (!isPowerOf2(powerOf2)) {
            throw new IllegalArgumentException("Divisor must be a power of 2: " + powerOf2);
        }
        return x & (powerOf2 - 1);
    }
    
    // Private constructor to prevent instantiation
    private PowerOf2Utils() {
        throw new AssertionError("Utility class should not be instantiated");
    }
}
