package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;

/**
 * Interface for adaptive scale selection strategies in Continuous Wavelet Transform.
 * 
 * <p>Automatically determines optimal scale ranges and spacing based on:
 * <ul>
 *   <li>Signal characteristics (length, frequency content, sampling rate)</li>
 *   <li>Wavelet properties (bandwidth, center frequency, support)</li>
 *   <li>Analysis requirements (resolution, computational constraints)</li>
 * </ul>
 * </p>
 */
public interface AdaptiveScaleSelector {
    
    /**
     * Selects optimal scales for CWT analysis.
     * 
     * @param signal the input signal to analyze
     * @param wavelet the wavelet to use for analysis
     * @param samplingRate the sampling rate of the signal (Hz)
     * @return array of optimal scales
     */
    double[] selectScales(double[] signal, ContinuousWavelet wavelet, double samplingRate);
    
    /**
     * Selects scales with additional configuration parameters.
     * 
     * @param signal the input signal to analyze
     * @param wavelet the wavelet to use for analysis
     * @param config configuration for scale selection
     * @return array of optimal scales
     */
    double[] selectScales(double[] signal, ContinuousWavelet wavelet, ScaleSelectionConfig config);
    
    /**
     * Gets the frequency range that would be analyzed with the selected scales.
     * 
     * @param scales the selected scales
     * @param wavelet the wavelet used
     * @param samplingRate the sampling rate
     * @return frequency range [minFreq, maxFreq] in Hz
     */
    default double[] getFrequencyRange(double[] scales, ContinuousWavelet wavelet, double samplingRate) {
        if (scales.length == 0) {
            return new double[]{0, 0};
        }
        
        double centerFreq = wavelet.centerFrequency();
        double minFreq = centerFreq * samplingRate / scales[scales.length - 1];
        double maxFreq = centerFreq * samplingRate / scales[0];
        
        return new double[]{minFreq, maxFreq};
    }
    
    /**
     * Estimates the number of scales needed for a given frequency range.
     * 
     * @param minFreq minimum frequency (Hz)
     * @param maxFreq maximum frequency (Hz)
     * @param wavelet the wavelet to use
     * @param samplingRate sampling rate (Hz)
     * @param scalesPerOctave number of scales per octave
     * @return estimated number of scales
     */
    default int estimateScaleCount(double minFreq, double maxFreq, ContinuousWavelet wavelet, 
                                  double samplingRate, int scalesPerOctave) {
        if (minFreq <= 0 || maxFreq <= minFreq) {
            throw new IllegalArgumentException("Invalid frequency range");
        }
        
        double octaves = Math.log(maxFreq / minFreq) / Math.log(2);
        return Math.max(1, (int) Math.ceil(octaves * scalesPerOctave));
    }
    
    /**
     * Configuration class for adaptive scale selection.
     */
    class ScaleSelectionConfig {
        private final double samplingRate;
        private final double minFrequency;
        private final double maxFrequency;
        private final int scalesPerOctave;
        private final boolean useSignalAdaptation;
        private final double frequencyResolution;
        private final int maxScales;
        private final ScaleSpacing spacing;
        
        private ScaleSelectionConfig(Builder builder) {
            this.samplingRate = builder.samplingRate;
            this.minFrequency = builder.minFrequency;
            this.maxFrequency = builder.maxFrequency;
            this.scalesPerOctave = builder.scalesPerOctave;
            this.useSignalAdaptation = builder.useSignalAdaptation;
            this.frequencyResolution = builder.frequencyResolution;
            this.maxScales = builder.maxScales;
            this.spacing = builder.spacing;
        }
        
        // Getters
        public double getSamplingRate() { return samplingRate; }
        public double getMinFrequency() { return minFrequency; }
        public double getMaxFrequency() { return maxFrequency; }
        public int getScalesPerOctave() { return scalesPerOctave; }
        public boolean isUseSignalAdaptation() { return useSignalAdaptation; }
        public double getFrequencyResolution() { return frequencyResolution; }
        public int getMaxScales() { return maxScales; }
        public ScaleSpacing getSpacing() { return spacing; }
        
        public static Builder builder(double samplingRate) {
            return new Builder(samplingRate);
        }
        
        public static class Builder {
            private double samplingRate;
            private double minFrequency = 0.0; // Auto-detect if 0
            private double maxFrequency = 0.0; // Auto-detect if 0
            private int scalesPerOctave = 10;
            private boolean useSignalAdaptation = true;
            private double frequencyResolution = 0.0; // Auto-detect if 0
            private int maxScales = 200;
            private ScaleSpacing spacing = ScaleSpacing.LOGARITHMIC;
            
            private Builder(double samplingRate) {
                this.samplingRate = samplingRate;
            }
            
            public Builder frequencyRange(double minFreq, double maxFreq) {
                this.minFrequency = minFreq;
                this.maxFrequency = maxFreq;
                return this;
            }
            
            public Builder scalesPerOctave(int scales) {
                this.scalesPerOctave = scales;
                return this;
            }
            
            public Builder useSignalAdaptation(boolean adapt) {
                this.useSignalAdaptation = adapt;
                return this;
            }
            
            public Builder frequencyResolution(double resolution) {
                this.frequencyResolution = resolution;
                return this;
            }
            
            public Builder maxScales(int max) {
                this.maxScales = max;
                return this;
            }
            
            public Builder spacing(ScaleSpacing spacing) {
                this.spacing = spacing;
                return this;
            }
            
            public ScaleSelectionConfig build() {
                return new ScaleSelectionConfig(this);
            }
        }
    }
    
    /**
     * Different scale spacing strategies.
     */
    enum ScaleSpacing {
        /** Linear spacing between scales */
        LINEAR,
        
        /** Logarithmic spacing (constant ratio between scales) */
        LOGARITHMIC,
        
        /** Dyadic spacing (powers of 2) */
        DYADIC,
        
        /** Mel-scale spacing (perceptually uniform) */
        MEL_SCALE,
        
        /** Signal-adaptive spacing based on local frequency content */
        ADAPTIVE
    }
}