package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import java.util.ArrayList;
import java.util.List;

/**
 * Dyadic scale selector that generates scales as powers of 2.
 * 
 * <p>This selector is particularly well-suited for:
 * <ul>
 *   <li>Orthogonal wavelets and perfect reconstruction</li>
 *   <li>Multi-resolution analysis</li>
 *   <li>Fast wavelet transform compatibility</li>
 *   <li>Computational efficiency with FFT-based algorithms</li>
 * </ul>
 * 
 * <p>Scale generation follows: s_j = s_0 * 2^j where j ranges from 0 to J-1</p>
 */
public class DyadicScaleSelector implements AdaptiveScaleSelector {

    /**
     * Creates a new DyadicScaleSelector.
     */
    public DyadicScaleSelector() {
        // Default constructor
    }
    
    
    private static final double DEFAULT_MIN_SCALE = 1.0;
    private static final double DEFAULT_MAX_SCALE_FACTOR = 0.25; // Max scale = signal_length / 4
    private static final int DEFAULT_MAX_OCTAVES = 10;
    
    @Override
    public double[] selectScales(double[] signal, ContinuousWavelet wavelet, double samplingRate) {
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("Sampling rate must be positive");
        }
        ScaleSelectionConfig config = ScaleSelectionConfig.builder(samplingRate)
            .spacing(ScaleSpacing.DYADIC)
            .useSignalAdaptation(true)
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
        
        int signalLength = signal.length;
        double samplingRate = config.getSamplingRate();
        
        // Determine scale range
        double[] scaleRange = determineScaleRange(signal, wavelet, config);
        double minScale = scaleRange[0];
        double maxScale = scaleRange[1];
        
        // Generate dyadic scales
        List<Double> scales = new ArrayList<>();
        
        // Find the starting power of 2
        double baseScale = findOptimalBaseScale(minScale, maxScale);
        
        // Generate powers of 2 within the range
        double currentScale = baseScale;
        
        // Go down to cover minimum scale
        while (currentScale >= minScale && scales.size() < config.getMaxScales()) {
            if (currentScale <= maxScale) {
                scales.add(currentScale);
            }
            currentScale /= 2.0;
        }
        
        // Go up to cover maximum scale
        currentScale = baseScale * 2.0;
        while (currentScale <= maxScale && scales.size() < config.getMaxScales()) {
            scales.add(currentScale);
            currentScale *= 2.0;
        }
        
        // Sort scales in ascending order
        scales.sort(Double::compareTo);
        
        // Apply signal-dependent refinement if requested
        if (config.isUseSignalAdaptation()) {
            scales = refineScalesBasedOnSignal(signal, scales, wavelet, config);
        }
        
        // Convert to array
        return scales.stream().mapToDouble(Double::doubleValue).toArray();
    }
    
    /**
     * Determines the appropriate scale range based on signal properties.
     */
    private double[] determineScaleRange(double[] signal, ContinuousWavelet wavelet, 
                                       ScaleSelectionConfig config) {
        int signalLength = signal.length;
        double samplingRate = config.getSamplingRate();
        
        double minScale, maxScale;
        
        if (config.getMinFrequency() > 0 && config.getMaxFrequency() > 0) {
            // Use specified frequency range
            double centerFreq = wavelet.centerFrequency();
            minScale = centerFreq * samplingRate / config.getMaxFrequency();
            maxScale = centerFreq * samplingRate / config.getMinFrequency();
        } else {
            // Auto-determine based on signal properties
            minScale = determineMinScale(wavelet, samplingRate);
            maxScale = determineMaxScale(signal, wavelet, samplingRate);
        }
        
        // Ensure reasonable bounds
        minScale = Math.max(minScale, DEFAULT_MIN_SCALE);
        maxScale = Math.min(maxScale, signalLength * DEFAULT_MAX_SCALE_FACTOR);
        
        // Ensure min < max
        if (minScale >= maxScale) {
            maxScale = minScale * 8; // At least 3 octaves
        }
        
        return new double[]{minScale, maxScale};
    }
    
    /**
     * Determines minimum scale based on Nyquist frequency and wavelet properties.
     */
    private double determineMinScale(ContinuousWavelet wavelet, double samplingRate) {
        // Minimum scale should resolve up to Nyquist frequency
        double nyquistFreq = samplingRate / 2.0;
        double centerFreq = wavelet.centerFrequency();
        
        // Scale that corresponds to Nyquist frequency
        double nyquistScale = centerFreq / nyquistFreq;
        
        // Add some margin for numerical stability
        return Math.max(nyquistScale * 0.5, DEFAULT_MIN_SCALE);
    }
    
    /**
     * Determines maximum scale based on signal length and desired frequency resolution.
     */
    private double determineMaxScale(double[] signal, ContinuousWavelet wavelet, double samplingRate) {
        int signalLength = signal.length;
        
        // Maximum scale limited by signal length
        double maxScaleFromLength = signalLength * DEFAULT_MAX_SCALE_FACTOR;
        
        // Maximum scale for meaningful frequency analysis (at least 2 cycles)
        double minAnalysisFreq = 2.0 * samplingRate / signalLength;
        double centerFreq = wavelet.centerFrequency();
        double maxScaleFromFreq = centerFreq / minAnalysisFreq;
        
        return Math.min(maxScaleFromLength, maxScaleFromFreq);
    }
    
    /**
     * Finds the optimal base scale for dyadic decomposition.
     */
    private double findOptimalBaseScale(double minScale, double maxScale) {
        // Find the power of 2 that's closest to the geometric mean
        double geometricMean = Math.sqrt(minScale * maxScale);
        
        // Find the nearest power of 2
        double log2Mean = Math.log(geometricMean) / Math.log(2);
        int nearestPower = (int) Math.round(log2Mean);
        
        return Math.pow(2, nearestPower);
    }
    
    /**
     * Refines scale selection based on signal characteristics.
     */
    private List<Double> refineScalesBasedOnSignal(double[] signal, List<Double> initialScales, 
                                                  ContinuousWavelet wavelet, ScaleSelectionConfig config) {
        List<Double> refinedScales = new ArrayList<>(initialScales);
        
        // Analyze signal frequency content using simple FFT
        double[] powerSpectrum = computePowerSpectrum(signal);
        double[] frequencies = computeFrequencies(signal.length, config.getSamplingRate());
        
        // Find dominant frequency bands
        List<Double> dominantFreqs = findDominantFrequencies(powerSpectrum, frequencies);
        
        // Add scales corresponding to dominant frequencies
        double centerFreq = wavelet.centerFrequency();
        for (double freq : dominantFreqs) {
            double scale = centerFreq * config.getSamplingRate() / freq;
            
            // Round to nearest dyadic scale
            double log2Scale = Math.log(scale) / Math.log(2);
            double dyadicScale = Math.pow(2, Math.round(log2Scale));
            
            // Add if not already present and within bounds
            if (!refinedScales.contains(dyadicScale) && 
                dyadicScale >= refinedScales.get(0) && 
                dyadicScale <= refinedScales.get(refinedScales.size() - 1)) {
                refinedScales.add(dyadicScale);
            }
        }
        
        // Sort and limit
        refinedScales.sort(Double::compareTo);
        if (refinedScales.size() > config.getMaxScales()) {
            refinedScales = refinedScales.subList(0, config.getMaxScales());
        }
        
        return refinedScales;
    }
    
    /**
     * Computes power spectrum using simple FFT.
     */
    private double[] computePowerSpectrum(double[] signal) {
        int n = signal.length;
        int fftSize = nextPowerOfTwo(n);
        
        // Simple DFT for power spectrum estimation
        double[] powerSpectrum = new double[fftSize / 2];
        
        for (int k = 0; k < fftSize / 2; k++) {
            double real = 0, imag = 0;
            
            for (int t = 0; t < n; t++) {
                double angle = -2 * Math.PI * k * t / fftSize;
                real += signal[t] * Math.cos(angle);
                imag += signal[t] * Math.sin(angle);
            }
            
            powerSpectrum[k] = real * real + imag * imag;
        }
        
        return powerSpectrum;
    }
    
    /**
     * Computes frequency bins for power spectrum.
     */
    private double[] computeFrequencies(int signalLength, double samplingRate) {
        int fftSize = nextPowerOfTwo(signalLength);
        double[] frequencies = new double[fftSize / 2];
        
        for (int i = 0; i < frequencies.length; i++) {
            frequencies[i] = i * samplingRate / fftSize;
        }
        
        return frequencies;
    }
    
    /**
     * Finds dominant frequencies in the power spectrum.
     */
    private List<Double> findDominantFrequencies(double[] powerSpectrum, double[] frequencies) {
        List<Double> dominantFreqs = new ArrayList<>();
        
        // Find peaks in power spectrum
        double maxPower = 0;
        for (double power : powerSpectrum) {
            maxPower = Math.max(maxPower, power);
        }
        
        double threshold = maxPower * 0.1; // 10% of maximum
        
        for (int i = 1; i < powerSpectrum.length - 1; i++) {
            if (powerSpectrum[i] > threshold &&
                powerSpectrum[i] > powerSpectrum[i-1] &&
                powerSpectrum[i] > powerSpectrum[i+1]) {
                dominantFreqs.add(frequencies[i]);
            }
        }
        
        // Limit to top 5 frequencies
        dominantFreqs.sort((a, b) -> {
            int idxA = findFrequencyIndex(frequencies, a);
            int idxB = findFrequencyIndex(frequencies, b);
            return Double.compare(powerSpectrum[idxB], powerSpectrum[idxA]);
        });
        
        return dominantFreqs.size() > 5 ? dominantFreqs.subList(0, 5) : dominantFreqs;
    }
    
    /**
     * Finds index of frequency in frequency array.
     */
    private int findFrequencyIndex(double[] frequencies, double targetFreq) {
        int bestIndex = 0;
        double minDiff = Math.abs(frequencies[0] - targetFreq);
        
        for (int i = 1; i < frequencies.length; i++) {
            double diff = Math.abs(frequencies[i] - targetFreq);
            if (diff < minDiff) {
                minDiff = diff;
                bestIndex = i;
            }
        }
        
        return bestIndex;
    }
    
    /**
     * Finds next power of 2.
     */
    private static int nextPowerOfTwo(int n) {
        if (n <= 1) return 1;
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return n + 1;
    }
    
    /**
     * Creates a default dyadic scale selector.
     * @return a new DyadicScaleSelector instance
     */
    public static DyadicScaleSelector create() {
        return new DyadicScaleSelector();
    }
    
    /**
     * Generates simple dyadic scales for a given range.
     * 
     * @param minScale minimum scale
     * @param maxScale maximum scale
     * @return array of dyadic scales
     */
    public static double[] generateDyadicScales(double minScale, double maxScale) {
        if (minScale <= 0 || maxScale <= minScale) {
            throw new IllegalArgumentException("Invalid scale range");
        }
        
        List<Double> scales = new ArrayList<>();
        
        // Find starting power of 2
        double log2Min = Math.log(minScale) / Math.log(2);
        double log2Max = Math.log(maxScale) / Math.log(2);
        
        int startPower = (int) Math.ceil(log2Min);
        int endPower = (int) Math.floor(log2Max);
        
        for (int j = startPower; j <= endPower; j++) {
            scales.add(Math.pow(2, j));
        }
        
        return scales.stream().mapToDouble(Double::doubleValue).toArray();
    }
}
