/**
 * Utility classes providing common functionality for the VectorWave library.
 * 
 * <p>This package contains utility classes that support validation, constants,
 * and batch operations used throughout the wavelet transform library. These
 * utilities are designed for both performance and correctness, providing
 * reusable components for the core transform operations.</p>
 * 
 * <h2>Key Utility Classes:</h2>
 * <ul>
 *   <li>{@link com.morphiqlabs.wavelet.util.ValidationUtils} - Input validation for signals and parameters</li>
 *   <li>{@link com.morphiqlabs.wavelet.util.BatchValidation} - Optimized validation for multiple signals</li>
 *   <li>{@link com.morphiqlabs.wavelet.util.WaveletConstants} - Important constants used throughout the library</li>
 * </ul>
 * 
 * <h2>Validation Features:</h2>
 * <ul>
 *   <li><strong>Signal Validation</strong> - Null checks, length validation, power-of-2 requirements</li>
 *   <li><strong>Numeric Validation</strong> - NaN/Infinity detection, range checking</li>
 *   <li><strong>Batch Operations</strong> - Efficient validation of multiple signals at once</li>
 *   <li><strong>Performance Optimization</strong> - Fast-fail validation with minimal overhead</li>
 * </ul>
 * 
 * <h2>Constants:</h2>
 * <ul>
 *   <li><strong>MAX_SAFE_POWER_OF_TWO</strong> - Largest power of 2 representable as positive int</li>
 *   <li><strong>MIN_DECOMPOSITION_SIZE</strong> - Minimum signal size for decomposition</li>
 *   <li><strong>Mathematical Constants</strong> - Precision tolerances and limits</li>
 * </ul>
 * 
 * <h2>Usage Examples:</h2>
 * <pre>{@code
 * // Validate a single signal
 * ValidationUtils.validateSignal(signal);
 * ValidationUtils.validatePowerOfTwoLength(signal.length);
 * 
 * // Batch validate multiple signals
 * double[][] signals = {approxCoeffs, detailCoeffs};
 * String[] names = {"approximation", "detail"};
 * BatchValidation.validateMultipleSignals(signals, names, null);
 * 
 * // Use constants
 * if (signalLength > WaveletConstants.MAX_SAFE_POWER_OF_TWO) {
 *     throw new InvalidSignalException("Signal too large");
 * }
 * }</pre>
 * 
 * <h2>Performance Notes:</h2>
 * <p>The validation utilities are designed with performance in mind:</p>
 * <ul>
 *   <li>Fast-fail approach minimizes computational overhead</li>
 *   <li>Batch validation reduces redundant checks</li>
 *   <li>Some methods have preconditions to avoid redundant validation</li>
 *   <li>Optimized for common use cases in wavelet transforms</li>
 * </ul>
 * 
 * <h2>Error Handling:</h2>
 * <p>Validation methods throw specific exceptions from the
 * exception types in the error package:</p>
 * <ul>
 *   <li>{@link com.morphiqlabs.wavelet.exception.InvalidSignalException} - Invalid signal data</li>
 *   <li>{@link com.morphiqlabs.wavelet.exception.InvalidArgumentException} - Invalid method arguments</li>
 * </ul>
 * 
 * @see com.morphiqlabs.wavelet.exception.InvalidArgumentException
*/
package com.morphiqlabs.wavelet.util;
