/**
 * Experimental Continuous Wavelet Transform (CWT) APIs.
 *
 * <p><strong>Experimental:</strong> The CWT API surface and numerics are still
 * evolving. Names, signatures, and behaviors may change in minor releases.
 * Use with caution in production and pin to an exact library version if you
 * depend on these types. Feedback and issues are welcome to help stabilize
 * the design.</p>
 *
 * <p>Notes:</p>
 * <ul>
 *   <li>Core implementations target Java 21 and avoid the Vector API.
 *       SIMD and structured concurrency live in the extensions module.</li>
 *   <li>Doclint is enabled; please keep public docs accurate and concise.</li>
 *   <li>Algorithmic defaults (scales, thresholds) are intended for demos;
 *       tune for your workload.</li>
 * </ul>
 */
@com.morphiqlabs.wavelet.annotations.Experimental
package com.morphiqlabs.wavelet.cwt;

