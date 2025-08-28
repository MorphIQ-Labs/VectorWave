/**
 * VectorWave - A comprehensive MODWT (Maximal Overlap Discrete Wavelet Transform) library for Java.
 * 
 * <p>This package contains the main demonstration application for the VectorWave library,
 * showcasing the capabilities of various wavelet types and transform operations using MODWT.</p>
 * 
 * <p>VectorWave is a pure Java implementation providing:</p>
 * <ul>
 *   <li>MODWT - shift-invariant wavelet transform for any signal length</li>
 *   <li>Multiple wavelet families (orthogonal, biorthogonal, continuous)</li>
 *   <li>Type-safe API with sealed interface hierarchy</li>
 *   <li>Support for different boundary modes</li>
 *   <li>Zero external dependencies</li>
 *   <li>Java 24 optimizations with Vector API</li>
 * </ul>
 * 
 * <p>The main entry point is {@link com.morphiqlabs.Main}, which demonstrates
 * transforms using various wavelet types including Haar, Daubechies, Symlets,
 * Coiflets, Biorthogonal Splines, and Morlet wavelets.</p>
 * 
 * <h2>Quick Start Example:</h2>
 * <pre>{@code
 * // Run the demonstration
 * java -cp target/classes com.morphiqlabs.Main
 * 
 * // Or use the library programmatically
 * MODWTTransform transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
 * double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0};  // Any length!
 * MODWTResult result = transform.forward(signal);
 * double[] reconstructed = transform.inverse(result);
 * }</pre>
 * 
 * <p>For detailed API usage, see {@link com.morphiqlabs.wavelet.modwt} and related packages.</p>
 * 
 * @see com.morphiqlabs.wavelet.modwt
 * @see com.morphiqlabs.wavelet.api
 */
package com.morphiqlabs;