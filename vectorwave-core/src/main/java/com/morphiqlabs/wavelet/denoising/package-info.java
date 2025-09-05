/**
 * Wavelet-based signal denoising algorithms and utilities.
 * 
 * <p>This package provides comprehensive signal denoising capabilities using wavelet transforms,
 * implementing state-of-the-art algorithms for noise reduction while preserving signal features.
 * Wavelet denoising is particularly effective for signals with non-stationary characteristics
 * and localized features that need to be preserved during noise removal.</p>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li><strong>Multiple Thresholding Methods:</strong> Soft, hard, and adaptive thresholding techniques</li>
 *   <li><strong>Automatic Threshold Selection:</strong> SURE, Bayes, and minimax threshold estimation</li>
 *   <li><strong>Multi-level Denoising:</strong> Support for wavelet decomposition at multiple scales</li>
 *   <li><strong>Feature Preservation:</strong> Algorithms designed to maintain signal characteristics</li>
 *   <li><strong>Performance Optimized:</strong> Efficient implementations for real-time processing</li>
 * </ul>
 * 
 * <h2>Planned Components</h2>
 * <dl>
 *   <dt><strong>Threshold Estimation</strong></dt>
 *   <dd>Automatic threshold selection algorithms including SURE (Stein's Unbiased Risk Estimator),
 *   Bayes shrinkage, and minimax methods for optimal noise reduction.</dd>
 *   
 *   <dt><strong>Thresholding Functions</strong></dt>
 *   <dd>Implementation of soft thresholding, hard thresholding, and adaptive threshold functions
 *   with configurable parameters for different noise characteristics.</dd>
 *   
 *   <dt><strong>Multi-scale Denoising</strong></dt>
 *   <dd>Advanced algorithms that apply different denoising strategies at different wavelet scales
 *   to optimize noise reduction while preserving signal features.</dd>
 *   
 *   <dt><strong>Noise Estimation</strong></dt>
 *   <dd>Robust noise level estimation from wavelet coefficients using median absolute deviation
 *   and other statistical methods.</dd>
 * </dl>
 * 
 * <h2>Mathematical Foundation</h2>
 * <p>Wavelet denoising works by decomposing the noisy signal into wavelet coefficients, applying
 * thresholding to remove noise-dominated coefficients, and reconstructing the signal:</p>
 * 
 * <pre>
 * 1. Decomposition: W = DWT(signal + noise)
 * 2. Thresholding: W' = T(W, threshold)
 * 3. Reconstruction: clean_signal = IDWT(W')
 * </pre>
 * 
 * <p>The soft thresholding function is defined as:</p>
 * <pre>
 * T_soft(w, λ) = sign(w) * max(|w| - λ, 0)
 * </pre>
 * 
 * <p>Where λ is the threshold parameter, optimally chosen based on noise characteristics.</p>
 * 
 * <h2>Algorithm Categories</h2>
 * <ul>
 *   <li><strong>Linear Methods:</strong> Simple threshold-based approaches for basic denoising</li>
 *   <li><strong>Adaptive Methods:</strong> Data-driven threshold selection for optimal results</li>
 *   <li><strong>Multi-scale Methods:</strong> Scale-dependent processing for complex signals</li>
 *   <li><strong>Statistical Methods:</strong> Bayesian and SURE-based optimal threshold estimation</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * <pre>{@code
 * // Future API design concept:
 * 
 * // Automatic denoising with optimal parameters
 * WaveletDenoiser denoiser = WaveletDenoiser.builder()
 *     .wavelet(Daubechies.DB4)
 *     .thresholdMethod(ThresholdMethod.SURE)
 *     .thresholdFunction(ThresholdFunction.SOFT)
 *     .levels(5)
 *     .build();
 * 
 * double[] noisySignal = {...};
 * double[] cleanSignal = denoiser.denoise(noisySignal);
 * 
 * // Manual threshold control
 * double[] coefficients = dwt.forward(noisySignal);
 * double threshold = NoiseEstimator.estimateFromMAD(coefficients);
 * double[] thresholded = ThresholdFunction.soft(coefficients, threshold);
 * double[] denoised = dwt.inverse(thresholded);
 * }</pre>
 * 
 * <h2>Performance Considerations</h2>
 * <ul>
 *   <li><strong>Memory Efficiency:</strong> In-place thresholding operations where possible</li>
 *   <li><strong>Computational Speed:</strong> Optimized threshold estimation algorithms</li>
 *   <li><strong>Scalability:</strong> Support for large signals through streaming processing</li>
 *   <li><strong>Parallel Processing:</strong> Multi-threaded implementations for enhanced performance</li>
 * </ul>
 * 
 * <h2>Integration with VectorWave</h2>
 * <p>This package integrates seamlessly with the core VectorWave framework:</p>
 * <ul>
 *   <li>Compatible with all discrete wavelet types from {@link com.morphiqlabs.wavelet.api}</li>
 *   <li>Utilizes core transform implementations ({@link com.morphiqlabs.wavelet.modwt.MODWTTransform}
 *       and {@link com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform})</li>
 *   <li>Memory-optimized implementations using internal memory utilities</li>
 * </ul>
 * 
 * <h2>Research Applications</h2>
 * <ul>
 *   <li><strong>Biomedical Signals:</strong> ECG, EEG, and medical imaging denoising</li>
 *   <li><strong>Audio Processing:</strong> Speech enhancement and audio restoration</li>
 *   <li><strong>Image Processing:</strong> Image denoising and artifact removal</li>
 *   <li><strong>Financial Data:</strong> Time series noise reduction for analysis</li>
 *   <li><strong>Scientific Data:</strong> Sensor data cleaning and feature extraction</li>
 * </ul>
 * 
 * @see com.morphiqlabs.wavelet.api.DiscreteWavelet
 * @see com.morphiqlabs.wavelet.modwt.MODWTTransform
 * @see com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform
 * @since 1.0.0
 */
package com.morphiqlabs.wavelet.denoising;
