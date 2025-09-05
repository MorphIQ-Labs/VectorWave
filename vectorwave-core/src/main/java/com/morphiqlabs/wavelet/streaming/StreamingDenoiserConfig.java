package com.morphiqlabs.wavelet.streaming;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser.ThresholdType;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.ErrorContext;

/**
 * Configuration for streaming wavelet denoisers.
 * 
 * <p>This class provides a fluent builder API for configuring streaming denoisers
 * with support for both MODWT-based implementations.</p>
 */
public final class StreamingDenoiserConfig {
    
    private final Wavelet wavelet;
    private final int blockSize;
    private final double overlapFactor;
    private final BoundaryMode boundaryMode;
    private final ThresholdMethod thresholdMethod;
    private final ThresholdType thresholdType;
    private final boolean adaptiveThreshold;
    private final double thresholdMultiplier;
    
    /**
     * Size of the window used for noise estimation.
     * 
     * <p>The noise window can be larger than the block size, which allows
     * for more stable noise estimates across multiple blocks. Different
     * implementations handle this differently:</p>
     * <ul>
     *   <li>MODWTStreamingDenoiser uses stratified sampling for large windows</li>
     *   <li>FastStreamingDenoiser may clip the window size as needed</li>
     * </ul>
     */
    private final int noiseWindowSize;
    
    private StreamingDenoiserConfig(Builder builder) {
        this.wavelet = builder.wavelet;
        this.blockSize = builder.blockSize;
        this.overlapFactor = builder.overlapFactor;
        this.boundaryMode = builder.boundaryMode;
        this.thresholdMethod = builder.thresholdMethod;
        this.thresholdType = builder.thresholdType;
        this.adaptiveThreshold = builder.adaptiveThreshold;
        this.thresholdMultiplier = builder.thresholdMultiplier;
        this.noiseWindowSize = builder.noiseWindowSize;
    }
    
    // Getters
    public Wavelet getWavelet() { return wavelet; }
    public int getBlockSize() { return blockSize; }
    public double getOverlapFactor() { return overlapFactor; }
    public BoundaryMode getBoundaryMode() { return boundaryMode; }
    public ThresholdMethod getThresholdMethod() { return thresholdMethod; }
    public ThresholdType getThresholdType() { return thresholdType; }
    public boolean isAdaptiveThreshold() { return adaptiveThreshold; }
    public double getThresholdMultiplier() { return thresholdMultiplier; }
    
    /**
     * Gets the size of the window used for noise estimation.
     * 
     * <p>The noise window can be larger than the block size, which allows
     * for more stable noise estimates across multiple blocks. Different
     * implementations handle this differently:</p>
     * <ul>
     *   <li>MODWTStreamingDenoiser uses stratified sampling for large windows</li>
     *   <li>FastStreamingDenoiser may clip the window size as needed</li>
     * </ul>
     * 
     * <p>Larger windows generally provide more stable noise estimates but may
     * increase latency. The optimal size depends on the signal characteristics
     * and application requirements.</p>
     * 
     * @return the noise estimation window size in samples
     */
    public int getNoiseWindowSize() { return noiseWindowSize; }
    
    /**
     * Creates a default configuration suitable for real-time audio processing.
     */
    public static StreamingDenoiserConfig defaultAudioConfig() {
        return new Builder()
            .blockSize(512)
            .overlapFactor(0.5)
            .thresholdMethod(ThresholdMethod.UNIVERSAL)
            .adaptiveThreshold(true)
            .build();
    }
    
    /**
     * Creates a default configuration suitable for financial data processing.
     */
    public static StreamingDenoiserConfig defaultFinancialConfig() {
        return new Builder()
            .blockSize(256)
            .overlapFactor(0.25)
            .thresholdMethod(ThresholdMethod.SURE)
            .thresholdMultiplier(0.8)
            .build();
    }
    
    /**
     * Builder for StreamingDenoiserConfig.
     */
    public static class Builder {
        private Wavelet wavelet = com.morphiqlabs.wavelet.api.Daubechies.DB4;
        private int blockSize = 256;
        private double overlapFactor = 0.5;
        private BoundaryMode boundaryMode = BoundaryMode.PERIODIC;
        private ThresholdMethod thresholdMethod = ThresholdMethod.UNIVERSAL;
        private ThresholdType thresholdType = ThresholdType.SOFT;
        private boolean adaptiveThreshold = false;
        private double thresholdMultiplier = 1.0;
        private int noiseWindowSize = 128;
        
        public Builder wavelet(Wavelet wavelet) {
            if (wavelet == null) {
                throw new InvalidArgumentException(
                    ErrorContext.builder("Wavelet cannot be null")
                        .withContext("field", "wavelet")
                        .withContext("value", "null")
                        .withContext("constraint", "non-null")
                        .withSuggestion("Use a valid wavelet like Daubechies.DB4")
                        .build()
                );
            }
            this.wavelet = wavelet;
            return this;
        }
        
        public Builder blockSize(int blockSize) {
            if (blockSize <= 0) {
                throw new InvalidArgumentException(
                    ErrorContext.builder("Block size must be positive")
                        .withContext("field", "blockSize")
                        .withContext("value", blockSize)
                        .withContext("constraint", "positive")
                        .withSuggestion("Use a block size like 256 or 512")
                        .build()
                );
            }
            this.blockSize = blockSize;
            return this;
        }
        
        public Builder overlapFactor(double overlapFactor) {
            if (overlapFactor < 0 || overlapFactor >= 1) {
                throw new InvalidArgumentException(
                    ErrorContext.builder("Overlap factor must be between 0 and 1")
                        .withContext("field", "overlapFactor")
                        .withContext("value", overlapFactor)
                        .withContext("constraint", "[0, 1)")
                        .withSuggestion("Use 0.5 for 50% overlap")
                        .build()
                );
            }
            this.overlapFactor = overlapFactor;
            return this;
        }
        
        public Builder boundaryMode(BoundaryMode boundaryMode) {
            if (boundaryMode == null) {
                throw new InvalidArgumentException(
                    ErrorContext.builder("Boundary mode cannot be null")
                        .withContext("field", "boundaryMode")
                        .withContext("value", "null")
                        .withContext("constraint", "non-null")
                        .withSuggestion("Use BoundaryMode.PERIODIC for streaming")
                        .build()
                );
            }
            this.boundaryMode = boundaryMode;
            return this;
        }
        
        public Builder thresholdMethod(ThresholdMethod method) {
            if (method == null) {
                throw new InvalidArgumentException(
                    ErrorContext.builder("Threshold method cannot be null")
                        .withContext("field", "thresholdMethod")
                        .withContext("value", "null")
                        .withContext("constraint", "non-null")
                        .withSuggestion("Use ThresholdMethod.UNIVERSAL or SURE")
                        .build()
                );
            }
            this.thresholdMethod = method;
            return this;
        }
        
        public Builder thresholdType(ThresholdType type) {
            if (type == null) {
                throw new InvalidArgumentException(
                    ErrorContext.builder("Threshold type cannot be null")
                        .withContext("field", "thresholdType")
                        .withContext("value", "null")
                        .withContext("constraint", "non-null")
                        .withSuggestion("Use ThresholdType.SOFT for smoother results")
                        .build()
                );
            }
            this.thresholdType = type;
            return this;
        }
        
        public Builder adaptiveThreshold(boolean adaptive) {
            this.adaptiveThreshold = adaptive;
            return this;
        }
        
        public Builder thresholdMultiplier(double multiplier) {
            if (multiplier <= 0) {
                throw new InvalidArgumentException(
                    ErrorContext.builder("Threshold multiplier must be positive")
                        .withContext("field", "thresholdMultiplier")
                        .withContext("value", multiplier)
                        .withContext("constraint", "positive")
                        .withSuggestion("Use 1.0 for standard threshold, 0.8 for less aggressive")
                        .build()
                );
            }
            this.thresholdMultiplier = multiplier;
            return this;
        }
        
        /**
         * Sets the size of the window used for noise estimation.
         * 
         * <p>The noise window can be larger than the block size, which allows
         * for more stable noise estimates across multiple blocks. Different
         * implementations handle this differently:</p>
         * <ul>
         *   <li>MODWTStreamingDenoiser uses stratified sampling for large windows</li>
         *   <li>FastStreamingDenoiser clips the window size as needed for performance</li>
         * </ul>
         * 
         * <p><strong>Implementation Notes:</strong></p>
         * <ul>
         *   <li>No upper bound validation is enforced (unlike previous versions)</li>
         *   <li>Algorithms benefit from larger windows that span multiple blocks for stability</li>
         *   <li>MODWTStreamingDenoiser uses stratified sampling when window exceeds data size</li>
         *   <li>FastStreamingDenoiser automatically clips to maintain low latency</li>
         *   <li>Each implementation handles oversized windows gracefully based on their requirements</li>
         * </ul>
         * 
         * @param windowSize the noise estimation window size (must be positive)
         * @return this builder
         */
        public Builder noiseWindowSize(int windowSize) {
            if (windowSize <= 0) {
                throw new InvalidArgumentException(
                    ErrorContext.builder("Noise window size must be positive")
                        .withContext("field", "noiseWindowSize")
                        .withContext("value", windowSize)
                        .withContext("constraint", "positive")
                        .withSuggestion("Use at least 64 samples for stable noise estimation")
                        .build()
                );
            }
            this.noiseWindowSize = windowSize;
            return this;
        }
        
        public StreamingDenoiserConfig build() {
            // Validate configuration
            if (overlapFactor > 0 && blockSize < 64) {
                throw new InvalidArgumentException(
                    ErrorContext.builder("Block size too small for overlap")
                        .withContext("field", "blockSize")
                        .withContext("value", blockSize)
                        .withContext("constraint", "≥ 64 when using overlap")
                        .withContext("overlapFactor", overlapFactor)
                        .withSuggestion("Increase block size or disable overlap")
                        .build()
                );
            }
            
            // Note: No upper bound validation on noiseWindowSize (see noiseWindowSize() JavaDoc for details)
            
            return new StreamingDenoiserConfig(this);
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "StreamingDenoiserConfig[wavelet=%s, blockSize=%d, overlap=%.1f%%, " +
            "threshold=%s/%s, adaptive=%s, multiplier=%.2f]",
            wavelet.name(), blockSize, overlapFactor * 100,
            thresholdMethod, thresholdType, adaptiveThreshold, thresholdMultiplier
        );
    }
}