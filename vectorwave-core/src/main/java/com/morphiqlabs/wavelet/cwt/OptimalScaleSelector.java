package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Optimal scale selector implementing various mathematically-principled scale spacing strategies.
 * 
 * <p>Provides multiple scale generation methods:
 * <ul>
 *   <li><strong>Logarithmic:</strong> Constant ratio between scales (geometric progression)</li>
 *   <li><strong>Linear:</strong> Constant difference between scales (arithmetic progression)</li>
 *   <li><strong>Mel-scale:</strong> Perceptually uniform frequency spacing</li>
 *   <li><strong>Critical sampling:</strong> Nyquist-rate sampling in scale domain</li>
 *   <li><strong>Wavelet-optimized:</strong> Spacing optimized for specific wavelet properties</li>
 * </ul>
 */
public class OptimalScaleSelector implements AdaptiveScaleSelector {
    
    private static final double GOLDEN_RATIO = (1.0 + Math.sqrt(5.0)) / 2.0;
    private static final double MEL_SCALE_FACTOR = 1127.01048; // Coefficient in mel scale formula: mel = 1127.01048 * ln(1 + f/700)
    private static final int DEFAULT_MAX_SCALES = 200; // Default maximum number of scales
    
    // Cache for adaptive ratio caps based on wavelet characteristics
    private static final Map<String, Double> RATIO_CAP_CACHE = new ConcurrentHashMap<>();
    
    // Instance fields for pre-computed values when using a specific wavelet
    private final ContinuousWavelet precomputedWavelet;
    private final Double precomputedRatioCap;
    
    /**
     * Creates an OptimalScaleSelector that can work with any wavelet.
     */
    public OptimalScaleSelector() {
        this.precomputedWavelet = null;
        this.precomputedRatioCap = null;
    }
    
    /**
     * Creates an OptimalScaleSelector optimized for a specific wavelet.
     * Pre-computes the adaptive ratio cap for better performance in hot paths.
     * 
     * @param wavelet the wavelet to optimize for
     */
    public OptimalScaleSelector(ContinuousWavelet wavelet) {
        this.precomputedWavelet = wavelet;
        double centerFreq = wavelet.centerFrequency();
        double bandwidth = wavelet.bandwidth();
        this.precomputedRatioCap = calculateAdaptiveRatioCap(wavelet, bandwidth, centerFreq);
    }
    
    @Override
    public double[] selectScales(double[] signal, ContinuousWavelet wavelet, double samplingRate) {
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("Sampling rate must be positive");
        }
        ScaleSelectionConfig config = ScaleSelectionConfig.builder(samplingRate)
            .spacing(ScaleSpacing.LOGARITHMIC)
            .scalesPerOctave(10)
            .useSignalAdaptation(false)
            .build();
        return selectScales(signal, wavelet, config);
    }
    
    @Override
    public double[] selectScales(double[] signal, ContinuousWavelet wavelet, ScaleSelectionConfig config) {
        if (signal == null || signal.length == 0) {
            throw new IllegalArgumentException("Signal cannot be null or empty");
        }
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        if (config.getSamplingRate() <= 0) {
            throw new IllegalArgumentException("Sampling rate must be positive");
        }
        
        // Determine scale range
        double[] scaleRange = computeOptimalScaleRange(signal, wavelet, config);
        double minScale = scaleRange[0];
        double maxScale = scaleRange[1];
        
        // Generate scales based on spacing strategy
        return switch (config.getSpacing()) {
            case LINEAR -> generateLinearScales(minScale, maxScale, config);
            case LOGARITHMIC -> generateLogarithmicScales(minScale, maxScale, config);
            case DYADIC -> generateDyadicScales(minScale, maxScale, config);
            case MEL_SCALE -> generateMelScales(minScale, maxScale, wavelet, config);
            case ADAPTIVE -> generateWaveletOptimizedScales(minScale, maxScale, signal, wavelet, config);
        };
    }
    
    /**
     * Computes optimal scale range based on signal and wavelet properties.
     */
    private double[] computeOptimalScaleRange(double[] signal, ContinuousWavelet wavelet, 
                                            ScaleSelectionConfig config) {
        double samplingRate = config.getSamplingRate();
        int signalLength = signal.length;
        
        double minScale, maxScale;
        
        if (config.getMinFrequency() > 0 && config.getMaxFrequency() > 0) {
            // Use specified frequency range
            double centerFreq = wavelet.centerFrequency();
            minScale = centerFreq * samplingRate / config.getMaxFrequency();
            maxScale = centerFreq * samplingRate / config.getMinFrequency();
        } else {
            // Auto-determine optimal range
            minScale = computeMinimalScale(wavelet, samplingRate);
            maxScale = computeMaximalScale(wavelet, samplingRate, signalLength);
        }
        
        // Apply wavelet-specific adjustments
        double[] adjustedRange = adjustScaleRangeForWavelet(minScale, maxScale, wavelet);
        
        return adjustedRange;
    }
    
    /**
     * Computes minimal scale based on Nyquist criterion and wavelet bandwidth.
     */
    private double computeMinimalScale(ContinuousWavelet wavelet, double samplingRate) {
        double nyquistFreq = samplingRate / 2.0;
        double centerFreq = wavelet.centerFrequency();
        double bandwidth = wavelet.bandwidth();
        
        // Ensure wavelet doesn't exceed Nyquist frequency
        // Add safety factor based on wavelet bandwidth
        double safetyFactor = 1.0 + bandwidth / centerFreq;
        
        return safetyFactor * centerFreq / nyquistFreq;
    }
    
    /**
     * Computes maximal scale based on signal length and frequency resolution requirements.
     */
    private double computeMaximalScale(ContinuousWavelet wavelet, double samplingRate, int signalLength) {
        double centerFreq = wavelet.centerFrequency();
        
        // Maximum scale limited by signal length (need at least one wavelet period)
        double maxScaleFromLength = signalLength / (4.0 * Math.PI / centerFreq);
        
        // Maximum scale for meaningful frequency analysis
        double minMeaningfulFreq = 2.0 * samplingRate / signalLength;
        double maxScaleFromFreq = centerFreq / minMeaningfulFreq;
        
        return Math.min(maxScaleFromLength, maxScaleFromFreq);
    }
    
    /**
     * Adjusts scale range based on specific wavelet characteristics.
     */
    private double[] adjustScaleRangeForWavelet(double minScale, double maxScale, ContinuousWavelet wavelet) {
        // Get wavelet-specific properties
        double bandwidth = wavelet.bandwidth();
        double centerFreq = wavelet.centerFrequency();
        
        // For wavelets with narrow bandwidth, allow larger scale range
        double bandwidthFactor = Math.max(0.5, Math.min(2.0, 2.0 / bandwidth));
        
        double adjustedMinScale = minScale / bandwidthFactor;
        double adjustedMaxScale = maxScale * bandwidthFactor;
        
        // Wavelet-specific optimizations
        String waveletName = wavelet.name().toLowerCase();
        if (waveletName.contains("morlet")) {
            // Morlet wavelets benefit from finer scale resolution
            adjustedMinScale *= 0.8;
            adjustedMaxScale *= 1.2;
        } else if (waveletName.contains("paul")) {
            // Paul wavelets are asymmetric, adjust accordingly
            adjustedMaxScale *= 1.5;
        } else if (waveletName.contains("dog") || waveletName.contains("mexican")) {
            // DOG wavelets have specific scale characteristics
            adjustedMinScale *= 0.9;
        }
        
        return new double[]{adjustedMinScale, adjustedMaxScale};
    }
    
    /**
     * Generates linearly spaced scales.
     */
    private double[] generateLinearScales(double minScale, double maxScale, ScaleSelectionConfig config) {
        int numScales = estimateOptimalScaleCount(minScale, maxScale, config);
        double[] scales = new double[numScales];
        
        for (int i = 0; i < numScales; i++) {
            scales[i] = minScale + (maxScale - minScale) * i / (numScales - 1);
        }
        
        return scales;
    }
    
    /**
     * Generates logarithmically spaced scales (geometric progression).
     */
    private double[] generateLogarithmicScales(double minScale, double maxScale, ScaleSelectionConfig config) {
        int numScales = estimateOptimalScaleCount(minScale, maxScale, config);
        double[] scales = new double[numScales];
        
        double logMin = Math.log(minScale);
        double logMax = Math.log(maxScale);
        
        for (int i = 0; i < numScales; i++) {
            double logScale = logMin + (logMax - logMin) * i / (numScales - 1);
            scales[i] = Math.exp(logScale);
        }
        
        return scales;
    }
    
    /**
     * Generates dyadic scales (powers of 2).
     */
    private double[] generateDyadicScales(double minScale, double maxScale, ScaleSelectionConfig config) {
        List<Double> scales = new ArrayList<>();
        
        // Find the range of powers of 2
        double log2Min = Math.log(minScale) / Math.log(2);
        double log2Max = Math.log(maxScale) / Math.log(2);
        
        // Generate sub-dyadic scales based on scalesPerOctave
        int subdivisionsPerOctave = config.getScalesPerOctave();
        
        for (double j = Math.ceil(log2Min * subdivisionsPerOctave); 
             j <= Math.floor(log2Max * subdivisionsPerOctave); j++) {
            double scale = Math.pow(2, j / subdivisionsPerOctave);
            if (scale >= minScale && scale <= maxScale) {
                scales.add(scale);
            }
        }
        
        return scales.stream().mapToDouble(Double::doubleValue).toArray();
    }
    
    /**
     * Generates Mel-scale spaced scales for perceptually uniform frequency spacing.
     */
    private double[] generateMelScales(double minScale, double maxScale, ContinuousWavelet wavelet, 
                                     ScaleSelectionConfig config) {
        double centerFreq = wavelet.centerFrequency();
        double samplingRate = config.getSamplingRate();
        
        // Convert scales to frequencies
        double maxFreq = centerFreq * samplingRate / minScale;
        double minFreq = centerFreq * samplingRate / maxScale;
        
        // Convert to Mel scale
        double minMel = frequencyToMel(minFreq);
        double maxMel = frequencyToMel(maxFreq);
        
        int numScales = estimateOptimalScaleCount(minScale, maxScale, config);
        double[] scales = new double[numScales];
        
        for (int i = 0; i < numScales; i++) {
            double mel = minMel + (maxMel - minMel) * i / (numScales - 1);
            double freq = melToFrequency(mel);
            scales[numScales - 1 - i] = centerFreq * samplingRate / freq; // Reverse order for scales
        }
        
        return scales;
    }
    
    /**
     * Generates wavelet-optimized scales based on wavelet properties and signal characteristics.
     */
    private double[] generateWaveletOptimizedScales(double minScale, double maxScale, double[] signal,
                                                   ContinuousWavelet wavelet, ScaleSelectionConfig config) {
        // Use golden ratio spacing for optimal coverage
        List<Double> scales = new ArrayList<>();
        
        double currentScale = minScale;
        double ratio = Math.pow(GOLDEN_RATIO, 1.0 / config.getScalesPerOctave());
        
        while (currentScale <= maxScale && scales.size() < config.getMaxScales()) {
            scales.add(currentScale);
            currentScale *= ratio;
        }
        
        // Add critical scales based on wavelet properties
        addCriticalScales(scales, minScale, maxScale, wavelet, config);
        
        // Remove duplicates and sort
        return scales.stream()
            .distinct()
            .sorted()
            .mapToDouble(Double::doubleValue)
            .toArray();
    }
    
    /**
     * Adds critical scales based on wavelet-specific considerations.
     */
    private void addCriticalScales(List<Double> scales, double minScale, double maxScale,
                                  ContinuousWavelet wavelet, ScaleSelectionConfig config) {
        double centerFreq = wavelet.centerFrequency();
        double bandwidth = wavelet.bandwidth();
        
        // Add scales at wavelet characteristic frequencies
        double characteristicScale = centerFreq; // Scale where wavelet is naturally centered
        if (characteristicScale >= minScale && characteristicScale <= maxScale) {
            scales.add(characteristicScale);
        }
        
        // Add scales at multiples of characteristic scale
        for (int k = 2; k <= 4; k++) {
            double multipleScale = characteristicScale * k;
            if (multipleScale >= minScale && multipleScale <= maxScale) {
                scales.add(multipleScale);
            }
            
            double fractionalScale = characteristicScale / k;
            if (fractionalScale >= minScale && fractionalScale <= maxScale) {
                scales.add(fractionalScale);
            }
        }
        
        // Add scales based on bandwidth considerations
        double bandwidthScale = centerFreq / bandwidth;
        if (bandwidthScale >= minScale && bandwidthScale <= maxScale) {
            scales.add(bandwidthScale);
        }
    }
    
    /**
     * Calculates adaptive ratio cap based on wavelet characteristics.
     * 
     * <p>Different wavelets have different optimal sampling densities:
     * <ul>
     *   <li>Narrow-band wavelets (high Q-factor): Need denser sampling (lower ratio)</li>
     *   <li>Broad-band wavelets (low Q-factor): Can use sparser sampling (higher ratio)</li>
     *   <li>Complex wavelets: May need special handling for phase information</li>
     * </ul>
     * 
     * @param wavelet the wavelet being used
     * @param bandwidth wavelet bandwidth parameter
     * @param centerFreq wavelet center frequency
     * @return adaptive maximum ratio for scale progression
     */
    private static double calculateAdaptiveRatioCap(ContinuousWavelet wavelet, 
                                                   double bandwidth, 
                                                   double centerFreq) {
        // Q-factor (quality factor) indicates frequency selectivity
        double qFactor = centerFreq / bandwidth;
        
        // Base cap varies with Q-factor
        // High Q (narrow-band): ratio 2.0-2.5
        // Medium Q: ratio 2.5-3.5  
        // Low Q (broad-band): ratio 3.5-5.0
        double baseCap;
        if (qFactor > 5.0) {
            // Narrow-band wavelet (e.g., high-order Morlet)
            baseCap = 2.0 + 0.5 * Math.min((qFactor - 5.0) / 5.0, 1.0);
        } else if (qFactor > 2.0) {
            // Medium selectivity
            baseCap = 2.5 + Math.min((5.0 - qFactor) / 3.0, 1.0);
        } else {
            // Broad-band wavelet (e.g., Mexican Hat, low-order wavelets)
            baseCap = 3.5 + 1.5 * Math.min((2.0 - qFactor) / 2.0, 1.0);
        }
        
        // Adjust for specific wavelet types
        String waveletName = wavelet.name().toLowerCase();
        if (waveletName.contains("morlet")) {
            // Morlet benefits from finer sampling
            baseCap *= 0.9;
        } else if (waveletName.contains("shannon")) {
            // Shannon has sharp frequency cutoff, can use coarser sampling
            baseCap *= 1.1;
        } else if (waveletName.contains("paul")) {
            // Paul wavelets are asymmetric, use standard sampling
            baseCap *= 1.0;
        }
        
        // Ensure reasonable bounds
        return Math.max(1.5, Math.min(baseCap, 5.0));
    }
    
    /**
     * Creates a cache key for wavelet characteristics.
     * 
     * @param wavelet the wavelet
     * @param bandwidth wavelet bandwidth
     * @param centerFreq wavelet center frequency
     * @return cache key string
     */
    private static String createWaveletCacheKey(ContinuousWavelet wavelet, 
                                              double bandwidth, 
                                              double centerFreq) {
        // Include wavelet name and key characteristics in the cache key
        return String.format("%s_%.6f_%.6f", 
            wavelet.name().toLowerCase(), bandwidth, centerFreq);
    }
    
    /**
     * Estimates optimal number of scales for given range and configuration.
     */
    private int estimateOptimalScaleCount(double minScale, double maxScale, ScaleSelectionConfig config) {
        if (config.getFrequencyResolution() > 0) {
            // Use specified frequency resolution
            double octaves = Math.log(maxScale / minScale) / Math.log(2);
            return Math.max(1, (int) Math.ceil(octaves * config.getScalesPerOctave()));
        } else {
            // Use default heuristic
            double scaleRatio = maxScale / minScale;
            int baseCount = (int) Math.ceil(Math.log(scaleRatio) * config.getScalesPerOctave() / Math.log(2));
            return Math.min(baseCount, config.getMaxScales());
        }
    }
    
    /**
     * Converts frequency to Mel scale.
     */
    private double frequencyToMel(double frequency) {
        return MEL_SCALE_FACTOR * Math.log(1 + frequency / 700.0);
    }
    
    /**
     * Converts Mel scale to frequency.
     */
    private double melToFrequency(double mel) {
        return 700.0 * (Math.exp(mel / MEL_SCALE_FACTOR) - 1);
    }
    
    // Factory methods for common scale selection strategies
    
    /**
     * Creates a logarithmic scale selector with specified scales per octave.
     *
     * @param scalesPerOctave the number of scales per octave
     * @return a new OptimalScaleSelector instance
     */
    public static OptimalScaleSelector logarithmic(int scalesPerOctave) {
        return new OptimalScaleSelector();
    }
    
    /**
     * Creates a linear scale selector.
     *
     * @return a new OptimalScaleSelector instance
     */
    public static OptimalScaleSelector linear() {
        return new OptimalScaleSelector();
    }
    
    /**
     * Creates a Mel-scale selector for perceptual uniformity.
     *
     * @return a new OptimalScaleSelector instance
     */
    public static OptimalScaleSelector melScale() {
        return new OptimalScaleSelector();
    }
    
    /**
     * Creates a wavelet-optimized selector using golden ratio spacing.
     *
     * @return a new OptimalScaleSelector instance
     */
    public static OptimalScaleSelector waveletOptimized() {
        return new OptimalScaleSelector();
    }
    
    /**
     * Generates scales with specified frequency resolution.
     * 
     * @param minFreq minimum frequency (Hz)
     * @param maxFreq maximum frequency (Hz)
     * @param wavelet the wavelet to use
     * @param samplingRate sampling rate (Hz)
     * @param frequencyResolution desired frequency resolution (Hz)
     * @return array of scales
     */
    public static double[] generateScalesForFrequencyResolution(double minFreq, double maxFreq,
                                                               ContinuousWavelet wavelet,
                                                               double samplingRate,
                                                               double frequencyResolution) {
        if (minFreq <= 0 || maxFreq <= minFreq || frequencyResolution <= 0) {
            throw new IllegalArgumentException("Invalid frequency parameters");
        }
        
        double centerFreq = wavelet.centerFrequency();
        List<Double> scales = new ArrayList<>();
        
        for (double freq = minFreq; freq <= maxFreq; freq += frequencyResolution) {
            double scale = centerFreq * samplingRate / freq;
            scales.add(scale);
        }
        
        // Sort in ascending order of scales (descending frequency)
        scales.sort((a, b) -> Double.compare(a, b));
        
        return scales.stream().mapToDouble(Double::doubleValue).toArray();
    }
    
    /**
     * Generates critical sampling scales for perfect reconstruction.
     * 
     * @param wavelet the wavelet to use
     * @param signalLength length of the signal
     * @param samplingRate sampling rate (Hz)
     * @return array of critically sampled scales
     */
    public static double[] generateCriticalSamplingScales(ContinuousWavelet wavelet,
                                                         int signalLength,
                                                         double samplingRate) {
        // Create a temporary instance and use its method
        OptimalScaleSelector selector = new OptimalScaleSelector();
        return selector.generateCriticalSamplingScales(wavelet, signalLength, samplingRate, 
                                                       DEFAULT_MAX_SCALES);
    }
    
    /**
     * Generates critical sampling scales for perfect reconstruction with configurable maximum.
     * Uses pre-computed values when the selector was created for a specific wavelet.
     * 
     * @param wavelet the wavelet to use
     * @param signalLength length of the signal
     * @param samplingRate sampling rate (Hz)
     * @param maxScales maximum number of scales to generate
     * @return array of critically sampled scales
     */
    public double[] generateCriticalSamplingScales(ContinuousWavelet wavelet,
                                                  int signalLength,
                                                  double samplingRate,
                                                  int maxScales) {
        double centerFreq = wavelet.centerFrequency();
        double bandwidth = wavelet.bandwidth();
        
        // Critical sampling rate in scale domain with adaptive capping
        double rawRatio = Math.exp(Math.PI * bandwidth / centerFreq);
        
        // Use pre-computed value if this selector was created for a specific wavelet
        double maxRatio;
        if (precomputedWavelet != null && precomputedWavelet.equals(wavelet)) {
            maxRatio = precomputedRatioCap;
        } else {
            // Fall back to cache for other wavelets
            String cacheKey = createWaveletCacheKey(wavelet, bandwidth, centerFreq);
            maxRatio = RATIO_CAP_CACHE.computeIfAbsent(cacheKey, 
                k -> calculateAdaptiveRatioCap(wavelet, bandwidth, centerFreq));
        }
        double criticalRatio = Math.min(rawRatio, maxRatio);
        
        List<Double> scales = new ArrayList<>();
        double minScale = centerFreq / (samplingRate / 2.0);
        double maxScale = centerFreq * signalLength / (4.0 * samplingRate);
        
        double currentScale = minScale;
        while (currentScale <= maxScale && scales.size() < maxScales) {
            scales.add(currentScale);
            currentScale *= criticalRatio;
        }
        
        return scales.stream().mapToDouble(Double::doubleValue).toArray();
    }
    
}
