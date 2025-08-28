/**
 * Custom exception classes for the VectorWave wavelet transform library.
 * 
 * <p>This package provides a hierarchy of runtime exceptions specifically designed
 * for wavelet transform operations. All exceptions extend from the base
 * {@link com.morphiqlabs.wavelet.exception.WaveletTransformException} to provide
 * consistent error handling throughout the library.</p>
 * 
 * <h2>Exception Hierarchy:</h2>
 * <pre>
 * {@link com.morphiqlabs.wavelet.exception.WaveletTransformException} (base exception)
 * ├── {@link com.morphiqlabs.wavelet.exception.InvalidArgumentException} - Invalid method arguments
 * └── {@link com.morphiqlabs.wavelet.exception.InvalidSignalException} - Invalid signal data
 * </pre>
 * 
 * <h2>Exception Types:</h2>
 * <ul>
 *   <li>{@link com.morphiqlabs.wavelet.exception.WaveletTransformException} - Base runtime exception for all wavelet-related errors</li>
 *   <li>{@link com.morphiqlabs.wavelet.exception.InvalidArgumentException} - Thrown for invalid method arguments (null parameters, invalid ranges, etc.)</li>
 *   <li>{@link com.morphiqlabs.wavelet.exception.InvalidSignalException} - Thrown for invalid signal data (wrong length, NaN/Infinity values, null signals)</li>
 * </ul>
 * 
 * <h2>Common Error Scenarios:</h2>
 * <ul>
 *   <li><strong>Signal Length Issues</strong> - MODWT accepts any positive signal length</li>
 *   <li><strong>Null Parameters</strong> - Methods reject null signals, wavelets, or configurations</li>
 *   <li><strong>Invalid Numeric Values</strong> - Signals containing NaN or Infinity values are rejected</li>
 *   <li><strong>Empty Signals</strong> - Zero-length signals are not supported</li>
 *   <li><strong>Oversized Signals</strong> - Signals exceeding maximum safe size limits</li>
 * </ul>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * try {
 *     MODWTTransform transform = new MODWTTransform(wavelet, boundaryMode);
 *     MODWTResult result = transform.forward(signal);
 * } catch (InvalidSignalException e) {
 *     // Handle invalid signal (empty, NaN values, etc.)
 *     System.err.println("Invalid signal: " + e.getMessage());
 * } catch (InvalidArgumentException e) {
 *     // Handle invalid arguments (null wavelet, invalid config, etc.)
 *     System.err.println("Invalid argument: " + e.getMessage());
 * } catch (WaveletTransformException e) {
 *     // Handle other wavelet-related errors
 *     System.err.println("Transform error: " + e.getMessage());
 * }
 * }</pre>
 * 
 * <p>All exceptions in this package are runtime exceptions, allowing for flexible
 * error handling while maintaining clean API signatures. The library provides
 * detailed error messages to help diagnose issues quickly.</p>
 * 
 * @see com.morphiqlabs.wavelet.util.ValidationUtils
 */
package com.morphiqlabs.wavelet.exception;