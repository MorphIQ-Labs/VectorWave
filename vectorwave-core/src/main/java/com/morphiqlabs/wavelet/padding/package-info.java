/**
 * Signal padding strategies for wavelet transforms.
 * 
 * <p>This package provides various padding strategies to handle signals
 * of arbitrary length in wavelet transforms. Each strategy offers different
 * trade-offs between computational efficiency, boundary artifact minimization,
 * and signal characteristic preservation.</p>
 * 
 * <h2>Basic Strategies</h2>
 * <ul>
 *   <li>{@link com.morphiqlabs.wavelet.padding.ZeroPaddingStrategy} - Pad with zeros</li>
 *   <li>{@link com.morphiqlabs.wavelet.padding.ConstantPaddingStrategy} - Pad with edge values</li>
 *   <li>{@link com.morphiqlabs.wavelet.padding.PeriodicPaddingStrategy} - Wrap signal circularly</li>
 *   <li>{@link com.morphiqlabs.wavelet.padding.SymmetricPaddingStrategy} - Mirror at boundaries</li>
 *   <li>{@link com.morphiqlabs.wavelet.padding.ReflectPaddingStrategy} - Reflect without duplicating edge</li>
 *   <li>{@link com.morphiqlabs.wavelet.padding.AntisymmetricPaddingStrategy} - Anti-mirror (negate)</li>
 * </ul>
 * 
 * <h2>Advanced Strategies</h2>
 * <ul>
 *   <li>{@link com.morphiqlabs.wavelet.padding.LinearExtrapolationStrategy} - Linear trend extension</li>
 *   <li>{@link com.morphiqlabs.wavelet.padding.PolynomialExtrapolationStrategy} - Higher-order polynomial fitting</li>
 *   <li>{@link com.morphiqlabs.wavelet.padding.StatisticalPaddingStrategy} - Random sampling from signal statistics</li>
 *   <li>{@link com.morphiqlabs.wavelet.padding.AdaptivePaddingStrategy} - Automatic strategy selection</li>
 *   <li>{@link com.morphiqlabs.wavelet.padding.CompositePaddingStrategy} - Smooth blending of multiple strategies</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Basic zero padding
 * PaddingStrategy zeroPad = new ZeroPaddingStrategy();
 * double[] padded = zeroPad.pad(signal, targetLength);
 * 
 * // Adaptive strategy that selects based on signal characteristics
 * PaddingStrategy adaptive = new AdaptivePaddingStrategy();
 * double[] smartPadded = adaptive.pad(signal, targetLength);
 * 
 * // Polynomial extrapolation for smooth signals
 * PaddingStrategy poly = new PolynomialExtrapolationStrategy(3); // Cubic
 * double[] smoothPadded = poly.pad(signal, targetLength);
 * }</pre>
 */
package com.morphiqlabs.wavelet.padding;