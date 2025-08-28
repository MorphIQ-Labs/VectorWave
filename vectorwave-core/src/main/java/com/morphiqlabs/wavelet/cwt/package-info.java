/**
 * Continuous Wavelet Transform (CWT) implementation and optimization components.
 * 
 * <p>This package provides comprehensive support for Continuous Wavelet Transform operations,
 * including optimized FFT-based implementations for efficient frequency domain processing.
 * The CWT is particularly useful for time-frequency analysis of non-stationary signals.</p>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>FFT-Accelerated Processing:</strong> O(N log N) complexity through frequency domain convolution</li>
 *   <li><strong>Multiple Wavelet Support:</strong> Compatible with all continuous wavelet types (Morlet, Mexican Hat, etc.)</li>
 *   <li><strong>Efficient Memory Usage:</strong> Optimized algorithms with cache-friendly access patterns</li>
 *   <li><strong>Numerical Stability:</strong> Robust handling of edge cases and numerical precision</li>
 * </ul>
 * 
 * <h2>Core Components</h2>
 * <dl>
 *   <dt><strong>Optimization Package</strong></dt>
 *   <dd>Contains high-performance FFT implementations for accelerated CWT computation.
 *   The {@link com.morphiqlabs.wavelet.cwt.optimization.FFTAcceleratedCWT} class provides
 *   optimized algorithms for frequency domain operations.</dd>
 * </dl>
 * 
 * <h2>Mathematical Foundation</h2>
 * <p>The Continuous Wavelet Transform decomposes a signal into time-frequency components using
 * a continuous family of wavelets. For a signal x(t) and wavelet ψ(t), the CWT is defined as:</p>
 * 
 * <pre>
 * CWT(a,b) = (1/√a) ∫ x(t) * ψ*((t-b)/a) dt
 * </pre>
 * 
 * <p>Where:</p>
 * <ul>
 *   <li><strong>a</strong> is the scale parameter (inversely related to frequency)</li>
 *   <li><strong>b</strong> is the translation parameter (time shift)</li>
 *   <li><strong>ψ*</strong> denotes the complex conjugate of the wavelet</li>
 * </ul>
 * 
 * <h2>Performance Characteristics</h2>
 * <p>The FFT-based implementation achieves significant performance improvements:</p>
 * <ul>
 *   <li><strong>Time Complexity:</strong> O(N log N) per scale vs O(N²) for direct convolution</li>
 *   <li><strong>Memory Efficiency:</strong> Optimized for large signal processing</li>
 *   <li><strong>Scalability:</strong> Efficient processing of signals up to millions of samples</li>
 * </ul>
 * 
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Create a continuous wavelet (e.g., Morlet)
 * ContinuousWavelet wavelet = new MorletWavelet();
 * 
 * // Initialize FFT-accelerated CWT
 * FFTAcceleratedCWT cwtProcessor = new FFTAcceleratedCWT();
 * 
 * // Process time-domain signal
 * double[] signal = {...}; // Your input signal
 * 
 * // Apply forward FFT
 * Complex[] spectrum = cwtProcessor.fft(signal);
 * 
 * // Process in frequency domain for multiple scales
 * // (wavelet convolution, scaling operations)
 * 
 * // Transform back to time domain
 * double[] result = cwtProcessor.ifft(processedSpectrum);
 * }</pre>
 * 
 * <h2>Design Decisions</h2>
 * <ul>
 *   <li><strong>FFT Optimization:</strong> Leverages Cooley-Tukey algorithm for maximum efficiency</li>
 *   <li><strong>Power-of-2 Requirements:</strong> Optimized for signals with power-of-2 lengths</li>
 *   <li><strong>Immutable Complex Numbers:</strong> Thread-safe design with immutable Complex class</li>
 *   <li><strong>Comprehensive Validation:</strong> Robust input validation for numerical stability</li>
 * </ul>
 * 
 * <h2>Integration</h2>
 * <p>This package integrates seamlessly with the broader VectorWave ecosystem:</p>
 * <ul>
 *   <li>Compatible with all {@link com.morphiqlabs.wavelet.api.ContinuousWavelet} implementations</li>
 *   <li>Supports all boundary modes from {@link com.morphiqlabs.wavelet.api.BoundaryMode}</li>
 *   <li>Follows consistent error handling patterns with {@link com.morphiqlabs.wavelet.exception}</li>
 *   <li>Utilizes shared mathematical operations from {@link com.morphiqlabs.wavelet.internal}</li>
 * </ul>
 * 
 * @see com.morphiqlabs.wavelet.api.ContinuousWavelet
 * @see com.morphiqlabs.wavelet.cwt.optimization.FFTAcceleratedCWT
 * @see com.morphiqlabs.wavelet.WaveletTransform
 */
package com.morphiqlabs.wavelet.cwt;