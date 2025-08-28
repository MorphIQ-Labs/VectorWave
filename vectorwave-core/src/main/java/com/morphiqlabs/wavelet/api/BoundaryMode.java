package com.morphiqlabs.wavelet.api;

/**
 * Enumeration of supported boundary handling modes for wavelet transforms.
 *
 * <p>The boundary mode determines how the signal is extended beyond its edges
 * during convolution operations. Different modes are suitable for different
 * types of signals and applications.</p>
 *
 * <p><strong>Implementation Status:</strong></p>
 * <ul>
 *   <li>PERIODIC - ✓ Fully implemented</li>
 *   <li>ZERO_PADDING - ✓ Fully implemented</li>
 *   <li>SYMMETRIC - ✓ Fully implemented</li>
 *   <li>CONSTANT - ✗ Not yet implemented</li>
 * </ul>
 */
public enum BoundaryMode {
    /**
     * Periodic extension - the signal is treated as periodic.
     * Best for naturally periodic signals.
     * Example: [a b c d] → [c d | a b c d | a b]
     */
    PERIODIC,

    /**
     * Symmetric extension - the signal is mirrored at boundaries.
     * Good for smooth signals, preserves continuity.
     * Example: [a b c d] → [b a | a b c d | d c]
     */
    SYMMETRIC,

    /**
     * Zero padding - extends with zeros.
     * Simple but can introduce artifacts at boundaries.
     * Example: [a b c d] → [0 0 | a b c d | 0 0]
     */
    ZERO_PADDING,

    /**
     * Constant extension - extends with edge values.
     * Preserves signal level at boundaries.
     * Example: [a b c d] → [a a | a b c d | d d]
     *
     * @apiNote Not yet implemented. Using this mode will throw
     * {@link UnsupportedOperationException} at runtime.
     */
    CONSTANT
}