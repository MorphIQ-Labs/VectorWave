package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Signal-adaptive scale selector that analyzes signal characteristics to optimize scale selection.
 * 
 * <p>This selector performs signal analysis to determine:
 * <ul>
 *   <li>Dominant frequency components and their strengths</li>
 *   <li>Spectral density distribution</li>
 *   <li>Time-frequency localization requirements</li>
 *   <li>Optimal resolution trade-offs</li>
 * </ul>
 * 
 * <p>Scale allocation is weighted by signal energy distribution, ensuring more scales
 * are placed where the signal has significant frequency content.</p>
 */
public class SignalAdaptiveScaleSelector implements AdaptiveScaleSelector {

    /**
     * Creates a new SignalAdaptiveScaleSelector.
     */
    public SignalAdaptiveScaleSelector() {
        // Default constructor
    }
    
    
    private static final double DEFAULT_ENERGY_THRESHOLD = 0.01; // 1% of total energy
    private static final int DEFAULT_SPECTRAL_ANALYSIS_SIZE = 1024;
    private static final double DEFAULT_SCALE_DENSITY_FACTOR = 1.5;
    
    // Default scale parameters for zero signals
    // These provide reasonable coverage for typical signal analysis scenarios
    private static final double ZERO_SIGNAL_MIN_SCALE = 1.0;    // Covers high frequencies up to Nyquist
    private static final double ZERO_SIGNAL_MAX_SCALE = 100.0;  // Covers low frequencies down to ~1% of sampling rate
    private static final int ZERO_SIGNAL_NUM_SCALES = 32;       // Sufficient resolution for most applications
    
    @Override
    public double[] selectScales(double[] signal, ContinuousWavelet wavelet, double samplingRate) {
        if (samplingRate <= 0) {
            throw new IllegalArgumentException("Sampling rate must be positive");
        }
        ScaleSelectionConfig config = ScaleSelectionConfig.builder(samplingRate)
            .spacing(ScaleSpacing.ADAPTIVE)
            .useSignalAdaptation(true)
            .scalesPerOctave(12) // Higher density for adaptive selection
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
        
        // Early check for zero signal to avoid expensive analysis
        boolean isZeroSignal = Arrays.stream(signal).noneMatch(value -> value != 0.0);
        
        if (isZeroSignal) {
            // For zero signal, return default logarithmic scales
            return ScaleSpace.logarithmic(
                ZERO_SIGNAL_MIN_SCALE, 
                ZERO_SIGNAL_MAX_SCALE, 
                Math.min(ZERO_SIGNAL_NUM_SCALES, config.getMaxScales())
            ).getScales();
        }
        
        // Analyze signal characteristics
        SignalCharacteristics characteristics = analyzeSignal(signal, config.getSamplingRate());
        
        // Determine frequency range of interest
        double[] frequencyRange = determineFrequencyRange(characteristics, config);
        double minFreq = frequencyRange[0];
        double maxFreq = frequencyRange[1];
        
        // Convert to scale range
        double centerFreq = wavelet.centerFrequency();
        double minScale = centerFreq * config.getSamplingRate() / maxFreq;
        double maxScale = centerFreq * config.getSamplingRate() / minFreq;
        
        // Generate adaptive scales based on signal energy distribution
        List<Double> scales = generateAdaptiveScales(
            minScale, maxScale, characteristics, config, wavelet);
        
        // Ensure we don't exceed maximum scale count
        if (scales.size() > config.getMaxScales()) {
            scales = prioritizeScales(scales, characteristics, config.getMaxScales(), wavelet);
        }
        
        // Sort and return
        scales.sort(Double::compareTo);
        return scales.stream().mapToDouble(Double::doubleValue).toArray();
    }
    
    /**
     * Analyzes signal to extract key characteristics for scale selection.
     * Uses a fixed-size window from the signal center for consistent performance.
     */
    private SignalCharacteristics analyzeSignal(double[] signal, double samplingRate) {
        // Use a fixed-size analysis window for consistent performance
        double[] analysisSegment;
        
        if (signal.length <= DEFAULT_SPECTRAL_ANALYSIS_SIZE) {
            // Signal is small enough - use it all
            analysisSegment = signal;
        } else {
            // Extract a window from the signal center
            analysisSegment = new double[DEFAULT_SPECTRAL_ANALYSIS_SIZE];
            int startIdx = (signal.length - DEFAULT_SPECTRAL_ANALYSIS_SIZE) / 2;
            System.arraycopy(signal, startIdx, analysisSegment, 0, DEFAULT_SPECTRAL_ANALYSIS_SIZE);
        }
        
        // Compute power spectral density
        SpectralAnalysis spectral = computeSpectralAnalysis(analysisSegment, samplingRate);
        
        // Find dominant frequencies
        List<DominantFrequency> dominantFreqs = findDominantFrequencies(spectral);
        
        // Compute signal statistics
        double[] stats = computeSignalStatistics(signal);
        
        // Estimate bandwidth and frequency spread
        double[] bandwidthInfo = estimateBandwidth(spectral);
        
        return new SignalCharacteristics(
            spectral,
            dominantFreqs,
            stats[0], // mean
            stats[1], // variance
            stats[2], // skewness
            stats[3], // kurtosis
            bandwidthInfo[0], // effective bandwidth
            bandwidthInfo[1], // spectral centroid
            bandwidthInfo[2]  // spectral spread
        );
    }
    
    /**
     * Computes spectral analysis of signal.
     */
    private SpectralAnalysis computeSpectralAnalysis(double[] signal, double samplingRate) {
        int n = signal.length;
        int fftSize = nextPowerOfTwo(n);
        
        // Apply window function (Hann window)
        double[] windowed = applyHannWindow(signal);
        
        // Compute FFT
        Complex[] fft = computeFFT(windowed, fftSize);
        
        // Compute power spectral density
        double[] psd = new double[fftSize / 2];
        double[] frequencies = new double[fftSize / 2];
        
        for (int i = 0; i < psd.length; i++) {
            psd[i] = fft[i].magnitude2() / (samplingRate * n);
            frequencies[i] = i * samplingRate / fftSize;
        }
        
        return new SpectralAnalysis(frequencies, psd, samplingRate);
    }
    
    /**
     * Applies Hann window to reduce spectral leakage.
     */
    private double[] applyHannWindow(double[] signal) {
        int n = signal.length;
        double[] windowed = new double[n];
        
        for (int i = 0; i < n; i++) {
            double window = 0.5 * (1 - Math.cos(2 * Math.PI * i / (n - 1)));
            windowed[i] = signal[i] * window;
        }
        
        return windowed;
    }
    
    /**
     * Finds dominant frequency components in the spectrum.
     */
    private List<DominantFrequency> findDominantFrequencies(SpectralAnalysis spectral) {
        double[] psd = spectral.psd;
        double[] frequencies = spectral.frequencies;
        
        List<DominantFrequency> dominantFreqs = new ArrayList<>();
        
        // Find total energy
        double totalEnergy = Arrays.stream(psd).sum();
        
        // Handle zero energy case
        if (totalEnergy <= 0) {
            return dominantFreqs; // Return empty list for zero signal
        }
        
        double energyThreshold = totalEnergy * DEFAULT_ENERGY_THRESHOLD;
        
        // Find local maxima above threshold
        for (int i = 2; i < psd.length - 2; i++) {
            if (psd[i] > energyThreshold &&
                psd[i] > psd[i-1] && psd[i] > psd[i+1] &&
                psd[i] > psd[i-2] && psd[i] > psd[i+2]) {
                
                // Estimate bandwidth around peak
                double bandwidth = estimateLocalBandwidth(psd, i);
                double relativeStrength = psd[i] / totalEnergy;
                
                dominantFreqs.add(new DominantFrequency(
                    frequencies[i], psd[i], bandwidth, relativeStrength));
            }
        }
        
        // Sort by energy (descending)
        dominantFreqs.sort((a, b) -> Double.compare(b.energy, a.energy));
        
        // Keep top frequencies that account for 90% of energy
        double cumulativeEnergy = 0;
        List<DominantFrequency> significantFreqs = new ArrayList<>();
        
        for (DominantFrequency freq : dominantFreqs) {
            significantFreqs.add(freq);
            cumulativeEnergy += freq.relativeStrength;
            if (cumulativeEnergy >= 0.9) break;
        }
        
        return significantFreqs;
    }
    
    /**
     * Estimates local bandwidth around a spectral peak.
     */
    private double estimateLocalBandwidth(double[] psd, int peakIndex) {
        double peakValue = psd[peakIndex];
        double halfPeak = peakValue / 2.0;
        
        // Find half-power points
        int leftIdx = peakIndex;
        while (leftIdx > 0 && psd[leftIdx] > halfPeak) {
            leftIdx--;
        }
        
        int rightIdx = peakIndex;
        while (rightIdx < psd.length - 1 && psd[rightIdx] > halfPeak) {
            rightIdx++;
        }
        
        return rightIdx - leftIdx; // In frequency bins
    }
    
    /**
     * Computes basic signal statistics.
     */
    private double[] computeSignalStatistics(double[] signal) {
        int n = signal.length;
        
        // Mean
        double mean = Arrays.stream(signal).average().orElse(0.0);
        
        // Variance
        double variance = Arrays.stream(signal)
            .map(x -> Math.pow(x - mean, 2))
            .average().orElse(0.0);
        
        double stdDev = Math.sqrt(variance);
        
        // Skewness
        double skewness = 0;
        if (stdDev > 0) {
            skewness = Arrays.stream(signal)
                .map(x -> Math.pow((x - mean) / stdDev, 3))
                .average().orElse(0.0);
        }
        
        // Kurtosis
        double kurtosis = 0;
        if (stdDev > 0) {
            kurtosis = Arrays.stream(signal)
                .map(x -> Math.pow((x - mean) / stdDev, 4))
                .average().orElse(0.0) - 3.0; // Excess kurtosis
        }
        
        return new double[]{mean, variance, skewness, kurtosis};
    }
    
    /**
     * Estimates signal bandwidth and spectral characteristics.
     */
    private double[] estimateBandwidth(SpectralAnalysis spectral) {
        double[] psd = spectral.psd;
        double[] frequencies = spectral.frequencies;
        
        // Compute spectral centroid (center of mass)
        double totalEnergy = Arrays.stream(psd).sum();
        
        // Handle zero energy case (e.g., zero signal)
        if (totalEnergy <= 0) {
            // Return default values for zero signal
            double defaultBandwidth = spectral.samplingRate / 4; // Nyquist / 2
            double defaultCentroid = spectral.samplingRate / 4;
            double defaultSpread = spectral.samplingRate / 8;
            return new double[]{defaultBandwidth, defaultCentroid, defaultSpread};
        }
        
        double centroid = 0;
        for (int i = 0; i < psd.length; i++) {
            centroid += frequencies[i] * psd[i];
        }
        centroid /= totalEnergy;
        
        // Compute spectral spread (second moment)
        double spread = 0;
        for (int i = 0; i < psd.length; i++) {
            spread += Math.pow(frequencies[i] - centroid, 2) * psd[i];
        }
        spread = Math.sqrt(spread / totalEnergy);
        
        // Estimate effective bandwidth (90% energy)
        double[] sortedPsd = psd.clone();
        Arrays.sort(sortedPsd);
        
        double targetEnergy = totalEnergy * 0.9; // Top 90% of energy
        double cumulativeEnergy = 0;
        double powerThreshold = 0;
        
        // Find power level that captures 90% of total energy
        for (int i = sortedPsd.length - 1; i >= 0; i--) {
            cumulativeEnergy += sortedPsd[i];
            if (cumulativeEnergy >= targetEnergy) {
                powerThreshold = sortedPsd[i];
                break;
            }
        }
        
        // Count frequency bins above threshold
        int significantBins = 0;
        for (double power : psd) {
            if (power >= powerThreshold) significantBins++;
        }
        
        double effectiveBandwidth = significantBins * spectral.samplingRate / (2 * psd.length);
        
        return new double[]{effectiveBandwidth, centroid, spread};
    }
    
    /**
     * Determines the frequency range of interest based on signal characteristics.
     */
    private double[] determineFrequencyRange(SignalCharacteristics characteristics, 
                                           ScaleSelectionConfig config) {
        double minFreq, maxFreq;
        
        if (config.getMinFrequency() > 0 && config.getMaxFrequency() > 0) {
            minFreq = config.getMinFrequency();
            maxFreq = config.getMaxFrequency();
        } else {
            // Auto-determine from signal characteristics
            SpectralAnalysis spectral = characteristics.spectralAnalysis;
            
            // Find frequency range containing significant energy
            double totalEnergy = Arrays.stream(spectral.psd).sum();
            
            // Handle zero energy case - use default frequency range
            if (totalEnergy <= 0) {
                minFreq = 1.0;
                maxFreq = spectral.samplingRate / 2;
                return new double[]{minFreq, maxFreq};
            }
            
            double cumulativeEnergy = 0;
            
            int minIdx = 0, maxIdx = spectral.psd.length - 1;
            
            // Find minimum frequency (5% of energy)
            for (int i = 0; i < spectral.psd.length; i++) {
                cumulativeEnergy += spectral.psd[i];
                if (cumulativeEnergy >= totalEnergy * 0.05) {
                    minIdx = i;
                    break;
                }
            }
            
            // Find maximum frequency (95% of energy)
            cumulativeEnergy = 0;
            for (int i = spectral.psd.length - 1; i >= 0; i--) {
                cumulativeEnergy += spectral.psd[i];
                if (cumulativeEnergy >= totalEnergy * 0.05) {
                    maxIdx = i;
                    break;
                }
            }
            
            minFreq = Math.max(spectral.frequencies[minIdx], 1.0);
            maxFreq = Math.min(spectral.frequencies[maxIdx], spectral.samplingRate / 2);
        }
        
        return new double[]{minFreq, maxFreq};
    }
    
    /**
     * Generates adaptive scales based on signal energy distribution.
     */
    private List<Double> generateAdaptiveScales(double minScale, double maxScale,
                                               SignalCharacteristics characteristics,
                                               ScaleSelectionConfig config,
                                               ContinuousWavelet wavelet) {
        List<Double> scales = new ArrayList<>();
        
        // Base logarithmic scale distribution
        int baseScaleCount = Math.min(config.getMaxScales(), 
            estimateScaleCount(characteristics.spectralCentroid * 0.5, 
                             characteristics.spectralCentroid * 2.0,
                             wavelet, characteristics.spectralAnalysis.samplingRate, 
                             config.getScalesPerOctave()));
        
        // Generate base scales
        for (int i = 0; i < baseScaleCount; i++) {
            double logScale = Math.log(minScale) + 
                (Math.log(maxScale) - Math.log(minScale)) * i / (baseScaleCount - 1);
            scales.add(Math.exp(logScale));
        }
        
        // Add extra scales around dominant frequencies
        for (DominantFrequency domFreq : characteristics.dominantFrequencies) {
            double centerFreq = wavelet.centerFrequency();
            double samplingRate = characteristics.spectralAnalysis.samplingRate;
            double scale = (centerFreq * samplingRate) / domFreq.frequency;
            
            if (scale >= minScale && scale <= maxScale) {
                // Add scales around this frequency with density proportional to energy
                int extraScales = (int) Math.ceil(domFreq.relativeStrength * 
                    DEFAULT_SCALE_DENSITY_FACTOR * config.getScalesPerOctave());
                
                for (int j = -extraScales/2; j <= extraScales/2; j++) {
                    if (j == 0) continue; // Skip center (already have base scales)
                    
                    double factor = Math.pow(2.0, j / (double)config.getScalesPerOctave());
                    double adaptiveScale = scale * factor;
                    
                    if (adaptiveScale >= minScale && adaptiveScale <= maxScale) {
                        scales.add(adaptiveScale);
                    }
                }
            }
        }
        
        // Remove duplicates and sort
        return scales.stream()
            .distinct()
            .sorted()
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }
    
    /**
     * Prioritizes scales when we have too many, keeping the most important ones.
     */
    private List<Double> prioritizeScales(List<Double> scales, SignalCharacteristics characteristics,
                                         int maxScales, ContinuousWavelet wavelet) {
        // Assign priority scores to each scale
        double[] priorities = new double[scales.size()];
        
        for (int i = 0; i < scales.size(); i++) {
            double scale = scales.get(i);
            priorities[i] = computeScalePriority(scale, characteristics, wavelet);
        }
        
        // Sort by priority and keep top scales
        Integer[] indices = new Integer[scales.size()];
        for (int i = 0; i < indices.length; i++) indices[i] = i;
        
        Arrays.sort(indices, (a, b) -> Double.compare(priorities[b], priorities[a]));
        
        List<Double> prioritizedScales = new ArrayList<>();
        for (int i = 0; i < Math.min(maxScales, indices.length); i++) {
            prioritizedScales.add(scales.get(indices[i]));
        }
        
        return prioritizedScales;
    }
    
    /**
     * Computes priority score for a scale based on signal characteristics.
     */
    private double computeScalePriority(double scale, SignalCharacteristics characteristics, 
                                      ContinuousWavelet wavelet) {
        double priority = 1.0; // Base priority
        
        // Higher priority for scales corresponding to dominant frequencies
        for (DominantFrequency domFreq : characteristics.dominantFrequencies) {
            double centerFreq = wavelet.centerFrequency();
            double samplingRate = characteristics.spectralAnalysis.samplingRate;
            double freqScale = centerFreq * samplingRate / domFreq.frequency;
            
            // Gaussian weighting around dominant frequency
            double distance = Math.abs(Math.log(scale) - Math.log(freqScale));
            double weight = Math.exp(-distance * distance / (2 * 0.5 * 0.5)); // σ = 0.5
            
            priority += domFreq.relativeStrength * weight;
        }
        
        return priority;
    }
    
    /**
     * FFT computation for spectral analysis.
     */
    private Complex[] computeFFT(double[] signal, int fftSize) {
        Complex[] data = new Complex[fftSize];
        
        // Initialize with signal data
        for (int i = 0; i < signal.length && i < fftSize; i++) {
            data[i] = new Complex(signal[i], 0);
        }
        
        // Pad with zeros
        for (int i = signal.length; i < fftSize; i++) {
            data[i] = new Complex(0, 0);
        }
        
        return fftRecursive(data);
    }
    
    /**
     * Recursive FFT implementation.
     */
    private Complex[] fftRecursive(Complex[] x) {
        int n = x.length;
        if (n <= 1) return x;
        
        // Divide
        Complex[] even = new Complex[n/2];
        Complex[] odd = new Complex[n/2];
        for (int k = 0; k < n/2; k++) {
            even[k] = x[2*k];
            odd[k] = x[2*k + 1];
        }
        
        // Conquer
        Complex[] evenFFT = fftRecursive(even);
        Complex[] oddFFT = fftRecursive(odd);
        
        // Combine
        Complex[] result = new Complex[n];
        for (int k = 0; k < n/2; k++) {
            double theta = -2 * Math.PI * k / n;
            Complex w = new Complex(Math.cos(theta), Math.sin(theta));
            Complex t = w.multiply(oddFFT[k]);
            result[k] = evenFFT[k].add(t);
            result[k + n/2] = evenFFT[k].subtract(t);
        }
        
        return result;
    }
    
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
    
    // Helper classes
    
    private static class SignalCharacteristics {
        final SpectralAnalysis spectralAnalysis;
        final List<DominantFrequency> dominantFrequencies;
        final double mean;
        final double variance;
        final double skewness;
        final double kurtosis;
        final double effectiveBandwidth;
        final double spectralCentroid;
        final double spectralSpread;
        
        SignalCharacteristics(SpectralAnalysis spectralAnalysis,
                            List<DominantFrequency> dominantFrequencies,
                            double mean, double variance, double skewness, double kurtosis,
                            double effectiveBandwidth, double spectralCentroid, double spectralSpread) {
            this.spectralAnalysis = spectralAnalysis;
            this.dominantFrequencies = dominantFrequencies;
            this.mean = mean;
            this.variance = variance;
            this.skewness = skewness;
            this.kurtosis = kurtosis;
            this.effectiveBandwidth = effectiveBandwidth;
            this.spectralCentroid = spectralCentroid;
            this.spectralSpread = spectralSpread;
        }
    }
    
    private static class SpectralAnalysis {
        final double[] frequencies;
        final double[] psd;
        final double samplingRate;
        
        SpectralAnalysis(double[] frequencies, double[] psd, double samplingRate) {
            this.frequencies = frequencies;
            this.psd = psd;
            this.samplingRate = samplingRate;
        }
    }
    
    private static class DominantFrequency {
        final double frequency;
        final double energy;
        final double bandwidth;
        final double relativeStrength;
        
        DominantFrequency(double frequency, double energy, double bandwidth, double relativeStrength) {
            this.frequency = frequency;
            this.energy = energy;
            this.bandwidth = bandwidth;
            this.relativeStrength = relativeStrength;
        }
    }
    
    private static class Complex {
        final double real, imag;
        
        Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }
        
        Complex add(Complex other) {
            return new Complex(real + other.real, imag + other.imag);
        }
        
        Complex subtract(Complex other) {
            return new Complex(real - other.real, imag - other.imag);
        }
        
        Complex multiply(Complex other) {
            return new Complex(real * other.real - imag * other.imag,
                             real * other.imag + imag * other.real);
        }
        
        double magnitude2() {
            return real * real + imag * imag;
        }
    }
}
