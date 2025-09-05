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
        /**
         * Gets the sampling rate.
         * @return sampling rate in Hz
         */
        public double getSamplingRate() { return samplingRate; }

        /**
         * Gets the minimum frequency.
         * @return minimum frequency (Hz), or 0 if auto
         */
        public double getMinFrequency() { return minFrequency; }

        /**
         * Gets the maximum frequency.
         * @return maximum frequency (Hz), or 0 if auto
         */
        public double getMaxFrequency() { return maxFrequency; }

        /**
         * Gets the number of scales per octave.
         * @return number of scales per octave
         */
        public int getScalesPerOctave() { return scalesPerOctave; }

        /**
         * Checks if signal adaptation is enabled.
         * @return true if signal adaptation is enabled
         */
        public boolean isUseSignalAdaptation() { return useSignalAdaptation; }

        /**
         * Gets the target frequency resolution.
         * @return target frequency resolution (Hz), or 0 to auto-detect
         */
        public double getFrequencyResolution() { return frequencyResolution; }

        /**
         * Gets the maximum number of scales.
         * @return maximum number of scales
         */
        public int getMaxScales() { return maxScales; }

        /**
         * Gets the scale spacing strategy.
         * @return spacing strategy
         */
        public ScaleSpacing getSpacing() { return spacing; }
        
        /**
         * Creates a builder for {@link ScaleSelectionConfig}.
         * @param samplingRate sampling rate in Hz
         * @return new builder instance
         */
        public static Builder builder(double samplingRate) {
            return new Builder(samplingRate);
        }
        
        /**
         * Builder for {@link ScaleSelectionConfig}.
         */
        public static class Builder {
            private double samplingRate;
            private double minFrequency = 0.0; // Auto-detect if 0
            private double maxFrequency = 0.0; // Auto-detect if 0
            private int scalesPerOctave = 10;
            private boolean useSignalAdaptation = true;
            private double frequencyResolution = 0.0; // Auto-detect if 0
            private int maxScales = 200;
            private ScaleSpacing spacing = ScaleSpacing.LOGARITHMIC;
            
            /**
             * Constructs a builder with the required sampling rate.
             * @param samplingRate sampling rate in Hz
             */
            private Builder(double samplingRate) {
                this.samplingRate = samplingRate;
            }
            
            /**
             * Sets the target frequency range; leave at 0 to auto-detect.
             * @param minFreq minimum frequency (Hz)
             * @param maxFreq maximum frequency (Hz)
             * @return this builder
             */
            public Builder frequencyRange(double minFreq, double maxFreq) {
                this.minFrequency = minFreq;
                this.maxFrequency = maxFreq;
                return this;
            }
            
            /**
             * Sets number of scales per octave.
             * @param scales count per octave
             * @return this builder
             */
            public Builder scalesPerOctave(int scales) {
                this.scalesPerOctave = scales;
                return this;
            }
            
            /**
             * Enables/disables signal-adaptive refinement.
             * @param adapt true to adapt to signal properties
             * @return this builder
             */
            public Builder useSignalAdaptation(boolean adapt) {
                this.useSignalAdaptation = adapt;
                return this;
            }
            
            /**
             * Sets desired frequency resolution; 0 to auto-detect.
             * @param resolution frequency resolution (Hz)
             * @return this builder
             */
            public Builder frequencyResolution(double resolution) {
                this.frequencyResolution = resolution;
                return this;
            }
            
            /**
             * Sets the maximum number of scales to produce.
             * @param max maximum scale count
             * @return this builder
             */
            public Builder maxScales(int max) {
                this.maxScales = max;
                return this;
            }
            
            /**
             * Selects spacing strategy (e.g., LOGARITHMIC or DYADIC).
             * @param spacing spacing mode
             * @return this builder
             */
            public Builder spacing(ScaleSpacing spacing) {
                this.spacing = spacing;
                return this;
            }
            
            /**
             * Builds configuration.
             * @return new config instance
             */
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
