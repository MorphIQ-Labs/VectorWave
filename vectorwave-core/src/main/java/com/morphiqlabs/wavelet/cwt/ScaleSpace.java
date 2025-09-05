package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;

import java.util.Arrays;

/**
 * Represents the scale space for Continuous Wavelet Transform.
 * 
 * <p>ScaleSpace defines the scales at which the wavelet transform is computed.
 * It provides various factory methods for creating commonly used scale distributions
 * (linear, logarithmic, dyadic) and supports conversion between scales and frequencies.</p>
 * 
 * <h2>Mathematical Foundation</h2>
 * 
 * <p>In the Continuous Wavelet Transform, the scale parameter 'a' controls the dilation
 * of the mother wavelet ψ(t):</p>
 * 
 * <pre>
 * ψ_{a,b}(t) = (1/√a) ψ((t-b)/a)
 * </pre>
 * 
 * <p>Where:</p>
 * <ul>
 *   <li><b>a</b> - scale parameter (dilation): larger scales detect lower frequencies</li>
 *   <li><b>b</b> - translation parameter: position in time</li>
 * </ul>
 * 
 * <h2>Scale-Frequency Relationship</h2>
 * 
 * <p>The relationship between scale and frequency is inverse:</p>
 * 
 * <pre>
 * frequency = (center_frequency × sampling_rate) / scale
 * </pre>
 * 
 * <p>Where center_frequency is a wavelet-specific constant (e.g., ~0.8 for Morlet).</p>
 * 
 * <h2>Scale Selection Guidelines</h2>
 * 
 * <h2>1. Linear Spacing (LINEAR)</h2>
 * <p><b>Mathematical form:</b> a_i = a_min + i × Δa</p>
 * <p><b>Use when:</b></p>
 * <ul>
 *   <li>Analyzing signals with uniformly distributed frequency content</li>
 *   <li>Equal importance across all frequencies</li>
 *   <li>Simple time-frequency analysis</li>
 * </ul>
 * <p><b>Drawbacks:</b> Poor resolution at high frequencies, oversampling at low frequencies</p>
 * 
 * <h2>2. Logarithmic Spacing (LOGARITHMIC) - Recommended</h2>
 * <p><b>Mathematical form:</b> a_i = a_min × r^i, where r = (a_max/a_min)^(1/(n-1))</p>
 * <p><b>Use when:</b></p>
 * <ul>
 *   <li>Natural signals (speech, music, seismic data)</li>
 *   <li>Financial time series analysis</li>
 *   <li>Multi-scale phenomena</li>
 *   <li>Need constant Q-factor (quality factor) analysis</li>
 * </ul>
 * <p><b>Benefits:</b> Mimics human perception, efficient coverage of frequency spectrum</p>
 * 
 * <h2>3. Dyadic Spacing (DYADIC)</h2>
 * <p><b>Mathematical form:</b> a_i = 2^j, j ∈ ℤ</p>
 * <p><b>Use when:</b></p>
 * <ul>
 *   <li>Compatibility with discrete wavelet transform</li>
 *   <li>Octave-band analysis</li>
 *   <li>Fast algorithms (reduced redundancy)</li>
 *   <li>Multiresolution analysis</li>
 * </ul>
 * <p><b>Benefits:</b> Efficient computation, natural for binary systems</p>
 * 
 * <h2>4. Custom Spacing (CUSTOM)</h2>
 * <p><b>Use when:</b></p>
 * <ul>
 *   <li>Domain-specific requirements</li>
 *   <li>Non-uniform frequency regions of interest</li>
 *   <li>Adaptive analysis based on signal characteristics</li>
 * </ul>
 * 
 * <h2>Practical Considerations</h2>
 * 
 * <p><b>Number of scales:</b> Typically 10-100 scales, depending on:</p>
 * <ul>
 *   <li>Signal length: longer signals support more scales</li>
 *   <li>Frequency range of interest</li>
 *   <li>Computational resources</li>
 *   <li>Required time-frequency resolution</li>
 * </ul>
 * 
 * <p><b>Scale bounds:</b></p>
 * <ul>
 *   <li>Min scale: Limited by Nyquist frequency (typically 2-4 samples)</li>
 *   <li>Max scale: Limited by signal length (typically length/4)</li>
 * </ul>
 * 
 * @see CWTConfig for configuration options
 * @see AdaptiveScaleSelector for automatic scale selection
 */
public final class ScaleSpace {
    
    /**
     * Types of scale spacing for wavelet transform analysis.
     * 
     * <p>Each type represents a different mathematical distribution of scales,
     * affecting the time-frequency resolution trade-off.</p>
     */
    public enum ScaleType {
        /**
         * Linear scale spacing: a_i = a_min + i × Δa
         * 
         * <p>Uniform spacing in scale domain. Best for signals with
         * uniform frequency distribution or when analyzing a narrow
         * frequency band.</p>
         */
        LINEAR,
        
        /**
         * Logarithmic scale spacing: a_i = a_min × r^i (RECOMMENDED)
         * 
         * <p>Geometric progression of scales. Provides constant Q-factor
         * (ratio of center frequency to bandwidth) across all scales.
         * Most suitable for natural signals and multi-scale phenomena.</p>
         */
        LOGARITHMIC,
        
        /**
         * Dyadic (powers of 2) scale spacing: a_i = 2^j
         * 
         * <p>Octave-band analysis compatible with discrete wavelet transform.
         * Efficient for multiresolution analysis and fast algorithms.</p>
         */
        DYADIC,
        
        /**
         * Custom user-defined scales.
         * 
         * <p>For specialized applications requiring non-uniform scale
         * distribution based on domain knowledge or signal characteristics.</p>
         */
        CUSTOM
    }
    
    private final double[] scales;
    private final ScaleType type;
    private final double minScale;
    private final double maxScale;
    
    private ScaleSpace(double[] scales, ScaleType type) {
        if (scales == null || scales.length == 0) {
            throw new IllegalArgumentException("Scales array cannot be null or empty");
        }
        
        // Validate scales are positive and sorted
        for (int i = 0; i < scales.length; i++) {
            if (scales[i] <= 0) {
                throw new IllegalArgumentException("All scales must be positive, found: " + scales[i]);
            }
            if (i > 0 && scales[i] <= scales[i - 1]) {
                throw new IllegalArgumentException("Scales must be in ascending order");
            }
        }
        
        this.scales = scales.clone();
        this.type = type;
        this.minScale = scales[0];
        this.maxScale = scales[scales.length - 1];
    }
    
    /**
     * Creates a linear scale space.
     * 
     * @param minScale minimum scale
     * @param maxScale maximum scale
     * @param numScales number of scales
     * @return linear scale space
     */
    public static ScaleSpace linear(double minScale, double maxScale, int numScales) {
        validateScaleRange(minScale, maxScale, numScales);
        
        double[] scales = new double[numScales];
        double step = (maxScale - minScale) / (numScales - 1);
        
        for (int i = 0; i < numScales; i++) {
            scales[i] = minScale + i * step;
        }
        
        return new ScaleSpace(scales, ScaleType.LINEAR);
    }
    
    /**
     * Creates a logarithmic scale space (RECOMMENDED for most applications).
     * 
     * <p>Scales follow geometric progression: a_i = a_min × r^i</p>
     * 
     * <p><b>Example:</b> For speech analysis (8 kHz sampling):</p>
     * <pre>
     * // Cover 50 Hz to 4 kHz with 64 log-spaced scales
     * double minScale = 0.8 * 8000 / 4000;  // For 4 kHz
     * double maxScale = 0.8 * 8000 / 50;    // For 50 Hz
     * ScaleSpace scales = ScaleSpace.logarithmic(minScale, maxScale, 64);
     * </pre>
     * 
     * @param minScale minimum scale (higher frequencies)
     * @param maxScale maximum scale (lower frequencies)
     * @param numScales number of scales (typically 10-100)
     * @return logarithmic scale space
     * @throws IllegalArgumentException if parameters are invalid
     */
    public static ScaleSpace logarithmic(double minScale, double maxScale, int numScales) {
        validateScaleRange(minScale, maxScale, numScales);
        
        double[] scales = new double[numScales];
        double logMin = Math.log(minScale);
        double logMax = Math.log(maxScale);
        double logStep = (logMax - logMin) / (numScales - 1);
        
        for (int i = 0; i < numScales; i++) {
            scales[i] = Math.exp(logMin + i * logStep);
        }
        
        return new ScaleSpace(scales, ScaleType.LOGARITHMIC);
    }
    
    /**
     * Creates a dyadic (powers of 2) scale space.
     * 
     * <p>Pure dyadic scales without subdivisions: a = 2^j</p>
     * 
     * <p><b>Example:</b> For fast multiresolution analysis:</p>
     * <pre>
     * // Create scales: 2, 4, 8, 16, 32, 64, 128
     * ScaleSpace scales = ScaleSpace.dyadic(1, 7);
     * </pre>
     * 
     * @param minLevel minimum level (scale = 2^minLevel)
     * @param maxLevel maximum level (scale = 2^maxLevel)
     * @return dyadic scale space
     * @throws IllegalArgumentException if minLevel > maxLevel
     */
    public static ScaleSpace dyadic(int minLevel, int maxLevel) {
        if (minLevel > maxLevel) {
            throw new IllegalArgumentException("minLevel must be <= maxLevel");
        }
        
        int numScales = maxLevel - minLevel + 1;
        double[] scales = new double[numScales];
        
        for (int i = 0; i < numScales; i++) {
            scales[i] = Math.pow(2, minLevel + i);
        }
        
        return new ScaleSpace(scales, ScaleType.DYADIC);
    }
    
    /**
     * Creates a scale space for a given frequency range.
     * 
     * @param minFreq minimum frequency in Hz
     * @param maxFreq maximum frequency in Hz
     * @param samplingRate sampling rate in Hz
     * @param wavelet continuous wavelet
     * @param numScales number of scales
     * @return scale space covering the frequency range
     */
    public static ScaleSpace forFrequencyRange(double minFreq, double maxFreq, 
                                              double samplingRate, ContinuousWavelet wavelet, 
                                              int numScales) {
        if (minFreq <= 0 || maxFreq <= 0) {
            throw new IllegalArgumentException("Frequencies must be positive");
        }
        if (minFreq >= maxFreq) {
            throw new IllegalArgumentException("minFreq must be < maxFreq");
        }
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("Sampling rate must be positive");
        }
        if (numScales <= 0) {
            throw new IllegalArgumentException("Number of scales must be positive");
        }
        
        // Convert frequencies to scales using:
        // scale = (centerFreq * samplingRate) / frequency
        double centerFreq = wavelet.centerFrequency();
        double maxScale = (centerFreq * samplingRate) / minFreq;
        double minScale = (centerFreq * samplingRate) / maxFreq;
        
        // Use logarithmic spacing for frequency range
        return logarithmic(minScale, maxScale, numScales);
    }
    
    /**
     * Creates a custom scale space from user-defined scales.
     * The scales will be automatically sorted in ascending order.
     * 
     * <p><b>Example:</b> For focusing on specific frequencies:</p>
     * <pre>
     * // Focus on 50Hz, 60Hz (power line), and harmonics
     * double fs = 1000;  // Sampling rate
     * double cf = 0.8;   // Morlet center frequency
     * double[] customScales = {
     *     cf * fs / 200,  // 200 Hz (4th harmonic)
     *     cf * fs / 120,  // 120 Hz (2nd harmonic)
     *     cf * fs / 100,  // 100 Hz
     *     cf * fs / 60,   // 60 Hz (US power)
     *     cf * fs / 50    // 50 Hz (EU power)
     * };
     * ScaleSpace scales = ScaleSpace.custom(customScales);
     * </pre>
     * 
     * @param scales array of scales (must be positive, will be sorted)
     * @return custom scale space
     * @throws IllegalArgumentException if scales are invalid
     */
    public static ScaleSpace custom(double[] scales) {
        if (scales == null || scales.length == 0) {
            throw new IllegalArgumentException("Scales array cannot be null or empty");
        }
        
        // Make a sorted copy
        double[] sortedScales = scales.clone();
        Arrays.sort(sortedScales);
        
        return new ScaleSpace(sortedScales, ScaleType.CUSTOM);
    }
    
    /**
     * Creates a custom scale space from pre-sorted scales.
     * This method is more efficient than {@link #custom(double[])} when
     * the scales are already sorted in ascending order.
     * 
     * @param sortedScales array of scales in ascending order (must be positive)
     * @return custom scale space
     * @throws IllegalArgumentException if scales are not in ascending order
     */
    public static ScaleSpace customSorted(double[] sortedScales) {
        if (sortedScales == null || sortedScales.length == 0) {
            throw new IllegalArgumentException("Scales array cannot be null or empty");
        }
        
        // Verify scales are sorted and positive
        double previousScale = 0;
        for (int i = 0; i < sortedScales.length; i++) {
            if (sortedScales[i] <= 0) {
                throw new IllegalArgumentException("All scales must be positive");
            }
            if (sortedScales[i] <= previousScale) {
                throw new IllegalArgumentException("Scales must be in strictly ascending order");
            }
            previousScale = sortedScales[i];
        }
        
        // Use the array directly (clone for safety)
        return new ScaleSpace(sortedScales.clone(), ScaleType.CUSTOM);
    }
    
    /**
     * Converts scales to frequencies.
     * 
     * @param wavelet continuous wavelet
     * @param samplingRate sampling rate in Hz
     * @return array of frequencies corresponding to each scale
     */
    public double[] toFrequencies(ContinuousWavelet wavelet, double samplingRate) {
        double centerFreq = wavelet.centerFrequency();
        double[] frequencies = new double[scales.length];
        
        for (int i = 0; i < scales.length; i++) {
            frequencies[i] = (centerFreq * samplingRate) / scales[i];
        }
        
        return frequencies;
    }
    
    /**
     * Converts a single scale to frequency using the wavelet's center frequency.
     * 
     * <p><b>Formula:</b> frequency = (center_frequency × sampling_rate) / scale</p>
     * 
     * <p><b>Example:</b></p>
     * <pre>
     * MorletWavelet morlet = new MorletWavelet();  // cf ≈ 0.8
     * double scale = 16;
     * double fs = 1000;  // 1 kHz sampling
     * double freq = scaleSpace.scaleToFrequency(scale, morlet, fs);
     * // freq ≈ 50 Hz
     * </pre>
     * 
     * @param scale the scale value
     * @param wavelet continuous wavelet
     * @param samplingRate sampling rate in Hz
     * @return corresponding frequency in Hz
     */
    public double scaleToFrequency(double scale, ContinuousWavelet wavelet, double samplingRate) {
        return (wavelet.centerFrequency() * samplingRate) / scale;
    }
    
    /**
     * Finds the scale index closest to a target frequency.
     * 
     * @param targetFreq target frequency in Hz
     * @param wavelet continuous wavelet
     * @param samplingRate sampling rate in Hz
     * @return index of the closest scale
     */
    public int findScaleIndexForFrequency(double targetFreq, ContinuousWavelet wavelet, 
                                         double samplingRate) {
        double[] frequencies = toFrequencies(wavelet, samplingRate);
        
        int closestIndex = 0;
        double minDiff = Math.abs(frequencies[0] - targetFreq);
        
        for (int i = 1; i < frequencies.length; i++) {
            double diff = Math.abs(frequencies[i] - targetFreq);
            if (diff < minDiff) {
                minDiff = diff;
                closestIndex = i;
            }
        }
        
        return closestIndex;
    }
    
    // Getters
    
    /**
     * Gets a copy of the scales array.
     *
     * @return a copy of the scales array
     */
    public double[] getScales() {
        return scales.clone();
    }
    
    /**
     * Gets the number of scales.
     *
     * @return the number of scales
     */
    public int getNumScales() {
        return scales.length;
    }
    
    /**
     * Gets the minimum scale.
     *
     * @return the minimum scale
     */
    public double getMinScale() {
        return minScale;
    }
    
    /**
     * Gets the maximum scale.
     *
     * @return the maximum scale
     */
    public double getMaxScale() {
        return maxScale;
    }
    
    /**
     * Gets the scale type.
     *
     * @return the scale type
     */
    public ScaleType getType() {
        return type;
    }
    
    /**
     * Gets the scale at a specific index.
     *
     * @param index the index of the scale
     * @return the scale at the specified index
     */
    public double getScale(int index) {
        if (index < 0 || index >= scales.length) {
            throw new IndexOutOfBoundsException("Scale index out of bounds: " + index);
        }
        return scales[index];
    }
    
    // Private helper methods
    
    private static void validateScaleRange(double minScale, double maxScale, int numScales) {
        if (minScale <= 0 || maxScale <= 0) {
            throw new IllegalArgumentException("Scales must be positive");
        }
        if (minScale >= maxScale) {
            throw new IllegalArgumentException("minScale must be < maxScale");
        }
        if (numScales <= 0) {
            throw new IllegalArgumentException("Number of scales must be positive");
        }
    }
}
