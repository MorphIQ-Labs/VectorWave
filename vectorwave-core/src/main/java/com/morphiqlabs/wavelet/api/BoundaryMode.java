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
 * 
 * @since 1.0.0
 */
public enum BoundaryMode {
    /**
     * Periodic extension - the signal is treated as periodic.
     * Best for naturally periodic signals. Enables exact reconstruction for MODWT/SWT.
     * Example: [a b c d] → [c d | a b c d | a b]
     */
    PERIODIC,

    /**
     * Symmetric extension - the signal is mirrored at boundaries.
     * Good for smooth signals, preserves continuity. Reconstruction uses
     * documented alignment heuristics to minimize boundary error.
     * Example: [a b c d] → [b a | a b c d | d c]
     */
    SYMMETRIC,

    /**
     * Zero padding - extends with zeros.
     * Simple but can introduce boundary attenuation; reconstruction is approximate near edges.
     * Example: [a b c d] → [0 0 | a b c d | 0 0]
     */
    ZERO_PADDING,

    /**
     * Constant extension - extends with edge values.
     * Preserves signal level at boundaries.
     * Example: [a b c d] → [a a | a b c d | d d]
     * <p>Note: Not yet implemented. Using this mode will throw
     * {@link UnsupportedOperationException} at runtime.</p>
     */
    CONSTANT
}
