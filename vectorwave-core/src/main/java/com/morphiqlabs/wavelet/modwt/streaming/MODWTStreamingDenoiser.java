package com.morphiqlabs.wavelet.modwt.streaming;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser.ThresholdType;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.util.MathUtils;

import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Streaming denoiser based on MODWT for real-time signal denoising.
 * 
 * <p>This class provides real-time denoising capabilities using the MODWT transform,
 * which offers several advantages over DWT-based streaming denoisers:</p>
 * <ul>
 *   <li>Shift-invariance prevents artifacts at block boundaries</li>
 *   <li>Works with any buffer size (not restricted to powers of 2)</li>
 *   <li>Better preservation of signal features</li>
 *   <li>Improved noise estimation accuracy</li>
 * </ul>
 * 
 * <p>The denoiser processes data in blocks and can optionally publish results
 * to subscribers for further processing.</p>
 * 
 * @since 1.0.0
 */
public class MODWTStreamingDenoiser implements Flow.Publisher<double[]>, AutoCloseable {
    
    private final MODWTTransform transform;
    private final WaveletDenoiser denoiser;
    private final int bufferSize;
    private final ThresholdType thresholdType;
    private final ThresholdMethod thresholdMethod;
    private final double thresholdMultiplier;
    private final NoiseEstimation noiseEstimation;
    private final int noiseWindowSize;
    
    private final SubmissionPublisher<double[]> publisher;
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicLong samplesProcessed = new AtomicLong(0);
    
    // Noise estimation
    private double[] noiseWindow;
    private int noiseWindowIndex = 0;
    private double estimatedNoiseLevel = 0.0;
    
    /**
     * Noise estimation method for streaming denoising.
     */
    public enum NoiseEstimation {
        /** Median Absolute Deviation estimation */
        MAD,
        /** Standard deviation estimation */
        STD,
        /** Fixed noise level */
        FIXED
    }
    
    private MODWTStreamingDenoiser(Builder builder) {
        this.transform = new MODWTTransform(builder.wavelet, builder.boundaryMode);
        this.denoiser = new WaveletDenoiser(builder.wavelet, builder.boundaryMode);
        
        this.bufferSize = builder.bufferSize;
        this.thresholdType = builder.thresholdType;
        this.thresholdMethod = builder.thresholdMethod;
        this.thresholdMultiplier = builder.thresholdMultiplier;
        this.noiseEstimation = builder.noiseEstimation;
        this.noiseWindowSize = builder.noiseWindowSize;
        
        this.publisher = new SubmissionPublisher<>();
        
        if (noiseEstimation != NoiseEstimation.FIXED) {
            this.noiseWindow = new double[noiseWindowSize];
        }
    }
    
    /**
     * Process a block of samples and return the denoised result.
     * 
     * @param samples the input samples
     * @return the denoised samples
     * @throws IllegalStateException if the denoiser is closed
     * @throws InvalidArgumentException if samples is null or empty
     */
    public double[] denoise(double[] samples) {
        if (closed.get()) {
            throw new IllegalStateException("Denoiser is closed");
        }
        if (samples == null || samples.length == 0) {
            throw new InvalidArgumentException("Samples cannot be null or empty");
        }
        
        // Update noise estimation if needed
        if (noiseEstimation != NoiseEstimation.FIXED) {
            updateNoiseEstimation(samples);
        }
        
        // Denoise using WaveletDenoiser with threshold multiplier applied
        double[] denoised;
        if (Math.abs(thresholdMultiplier - 1.0) < 1e-10) {
            // No multiplier adjustment needed - use standard denoising
            denoised = denoiser.denoise(samples, thresholdMethod, thresholdType);
        } else {
            // Calculate threshold using the selected method, then apply multiplier
            double baseThreshold = calculateThreshold(samples);
            double adjustedThreshold = baseThreshold * thresholdMultiplier;
            denoised = denoiser.denoiseFixed(samples, adjustedThreshold, thresholdType);
        }
        
        // Update statistics
        samplesProcessed.addAndGet(samples.length);
        
        // Publish to subscribers if any
        if (publisher.hasSubscribers()) {
            publisher.submit(denoised.clone());
        }
        
        return denoised;
    }
    
    /**
     * Update noise estimation based on new samples.
     */
    private void updateNoiseEstimation(double[] samples) {
        // Transform to get detail coefficients
        MODWTResult result = transform.forward(samples);
        double[] details = result.detailCoeffs();
        
        // Update noise window with detail coefficients
        // Strategy: If we have more details than window size, use stratified sampling
        // to maintain temporal diversity in noise estimation
        if (details.length <= noiseWindowSize) {
            // Case 1: Fewer details than window size - add all
            for (double detail : details) {
                noiseWindow[noiseWindowIndex] = Math.abs(detail);
                noiseWindowIndex = (noiseWindowIndex + 1) % noiseWindowSize;
            }
        } else {
            // Case 2: More details than window size - use stratified sampling
            // Divide the signal into equal strata and sample from each
            // This ensures we capture temporal variations across the entire signal
            
            // Calculate number of samples per stratum
            int strataCount = Math.min(noiseWindowSize, 10); // Use at most 10 strata
            int samplesPerStratum = noiseWindowSize / strataCount;
            int extraSamples = noiseWindowSize % strataCount;
            int strataSize = details.length / strataCount;
            
            int sampleIndex = 0;
            
            // Sample from each stratum
            for (int stratum = 0; stratum < strataCount; stratum++) {
                int strataStart = stratum * strataSize;
                int strataEnd = (stratum == strataCount - 1) ? details.length : (stratum + 1) * strataSize;
                int samplesToTake = samplesPerStratum + (stratum < extraSamples ? 1 : 0);
                
                // Within each stratum, sample uniformly
                if (samplesToTake > 0) {
                    int strataLength = strataEnd - strataStart;
                    int strataStep = Math.max(1, strataLength / samplesToTake);
                    
                    for (int i = 0; i < samplesToTake && sampleIndex < noiseWindowSize; i++) {
                        int idx = strataStart + (i * strataStep) % strataLength;
                        if (idx < details.length) {
                            noiseWindow[noiseWindowIndex] = Math.abs(details[idx]);
                            noiseWindowIndex = (noiseWindowIndex + 1) % noiseWindowSize;
                            sampleIndex++;
                        }
                    }
                }
            }
            
            // Fill any remaining slots with samples from the end (recent data)
            int remaining = noiseWindowSize - sampleIndex;
            if (remaining > 0) {
                int startIdx = Math.max(0, details.length - remaining);
                for (int i = startIdx; i < details.length && sampleIndex < noiseWindowSize; i++) {
                    noiseWindow[noiseWindowIndex] = Math.abs(details[i]);
                    noiseWindowIndex = (noiseWindowIndex + 1) % noiseWindowSize;
                    sampleIndex++;
                }
            }
        }
        
        // Calculate noise level based on method
        switch (noiseEstimation) {
            case MAD -> estimatedNoiseLevel = calculateMAD(noiseWindow) / 0.6745;
            case STD -> estimatedNoiseLevel = calculateSTD(noiseWindow);
            default -> {
                // Fixed noise level, no update needed
            }
        }
    }
    
    /**
     * Calculates the Median Absolute Deviation (MAD) of the given values.
     * MAD is a robust measure of variability based on the median of absolute deviations.
     * 
     * @param values the array of values (must not be empty)
     * @return the median absolute deviation, or 0 if all values are zero/invalid
     * @throws IllegalArgumentException if values array is empty
     */
    private double calculateMAD(double[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Values array cannot be null or empty");
        }
        
        // Check if we have any non-zero, finite values
        boolean hasValidData = false;
        int validCount = 0;
        
        for (double value : values) {
            if (Double.isFinite(value)) {
                validCount++;
                if (value != 0.0) {
                    hasValidData = true;
                }
            }
        }
        
        // If no valid finite values, return 0
        if (validCount == 0) {
            return 0.0;
        }
        
        // If all valid values are zero, MAD is 0
        if (!hasValidData) {
            return 0.0;
        }
        
        return MathUtils.medianAbsoluteDeviation(values);
    }
    
    /**
     * Calculates the standard deviation of the given values.
     * 
     * @param values the array of values
     * @return the standard deviation, or 0 if insufficient valid data
     */
    private double calculateSTD(double[] values) {
        if (values == null || values.length == 0) {
            return 0.0;
        }
        
        // Count valid finite values
        int validCount = 0;
        for (double value : values) {
            if (Double.isFinite(value)) {
                validCount++;
            }
        }
        
        // Need at least 2 valid values for standard deviation
        if (validCount < 2) {
            return 0.0;
        }
        
        return MathUtils.standardDeviation(values);
    }
    
    /**
     * Calculates the base threshold using the selected method and current noise estimation.
     * This threshold can then be adjusted by the threshold multiplier.
     * 
     * @param samples the input samples to analyze
     * @return the calculated base threshold
     */
    private double calculateThreshold(double[] samples) {
        // Get current noise level estimate
        double sigma = estimatedNoiseLevel;
        
        // If no noise estimation available yet, estimate from current samples
        if (sigma <= 0.0 || noiseEstimation == NoiseEstimation.FIXED) {
            // Transform samples to get detail coefficients for noise estimation
            MODWTResult result = transform.forward(samples);
            double[] details = result.detailCoeffs();
            
            // Estimate noise using MAD (consistent with default noise estimation)
            double[] absDetails = new double[details.length];
            for (int i = 0; i < details.length; i++) {
                absDetails[i] = Math.abs(details[i]);
            }
            sigma = calculateMAD(absDetails) / 0.6745;
        }
        
        // Calculate threshold based on selected method
        int n = samples.length;
        return switch (thresholdMethod) {
            case UNIVERSAL -> 
                // Universal threshold (VisuShrink): sigma * sqrt(2 * log(n))
                sigma * Math.sqrt(2.0 * Math.log(n));
                
            case SURE -> 
                // For SURE threshold, we need the actual coefficients
                // This is more complex, so we'll approximate with a conservative factor
                sigma * Math.sqrt(2.0 * Math.log(n)) * 0.8;
                
            case MINIMAX -> {
                // Minimax threshold approximation
                double logN = Math.log(n);
                if (n <= 32) {
                    yield 0.0;
                } else if (n <= 64) {
                    yield sigma * (0.3936 + 0.1829 * logN);
                } else {
                    yield sigma * (0.4745 + 0.1148 * logN);
                }
            }
                
            case FIXED -> 
                // For fixed method, return a reasonable default that will be multiplied
                sigma;
                
            default -> throw new IllegalArgumentException("Unknown threshold method: " + thresholdMethod);
        };
    }
    
    
    /**
     * Get the current estimated noise level.
     * 
     * @return the estimated noise level
     */
    public double getEstimatedNoiseLevel() {
        return estimatedNoiseLevel;
    }
    
    /**
     * Get the total number of samples processed.
     * 
     * @return the number of samples processed
     */
    public long getSamplesProcessed() {
        return samplesProcessed.get();
    }
    
    @Override
    public void subscribe(Flow.Subscriber<? super double[]> subscriber) {
        publisher.subscribe(subscriber);
    }
    
    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            publisher.close();
        }
    }
    
    /**
     * Check if the denoiser is closed.
     * 
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed.get();
    }
    
    /**
     * Builder for creating MODWTStreamingDenoiser instances.
     */
    public static class Builder {
        private Wavelet wavelet = Daubechies.DB4;
        private BoundaryMode boundaryMode = BoundaryMode.PERIODIC;
        private int bufferSize = 256;
        private ThresholdType thresholdType = ThresholdType.SOFT;
        private ThresholdMethod thresholdMethod = ThresholdMethod.UNIVERSAL;
        private double thresholdMultiplier = 1.0;
        private NoiseEstimation noiseEstimation = NoiseEstimation.MAD;
        private int noiseWindowSize = 1024;
        
        /**
         * Set the wavelet to use for denoising.
         * 
         * @param wavelet the wavelet
         * @return this builder
         */
        public Builder wavelet(Wavelet wavelet) {
            if (wavelet == null) {
                throw new InvalidArgumentException("Wavelet cannot be null");
            }
            this.wavelet = wavelet;
            return this;
        }
        
        /**
         * Set the boundary mode.
         * 
         * @param boundaryMode the boundary mode
         * @return this builder
         */
        public Builder boundaryMode(BoundaryMode boundaryMode) {
            if (boundaryMode == null) {
                throw new InvalidArgumentException("Boundary mode cannot be null");
            }
            this.boundaryMode = boundaryMode;
            return this;
        }
        
        /**
         * Set the buffer size.
         * 
         * @param bufferSize the buffer size (must be positive)
         * @return this builder
         */
        public Builder bufferSize(int bufferSize) {
            if (bufferSize <= 0) {
                throw new InvalidArgumentException("Buffer size must be positive");
            }
            this.bufferSize = bufferSize;
            return this;
        }
        
        /**
         * Set the threshold type.
         * 
         * @param thresholdType the threshold type
         * @return this builder
         */
        public Builder thresholdType(ThresholdType thresholdType) {
            if (thresholdType == null) {
                throw new InvalidArgumentException("Threshold type cannot be null");
            }
            this.thresholdType = thresholdType;
            return this;
        }
        
        /**
         * Set the threshold method.
         * 
         * @param thresholdMethod the threshold method
         * @return this builder
         */
        public Builder thresholdMethod(ThresholdMethod thresholdMethod) {
            if (thresholdMethod == null) {
                throw new InvalidArgumentException("Threshold method cannot be null");
            }
            this.thresholdMethod = thresholdMethod;
            return this;
        }
        
        /**
         * Set the threshold multiplier for fine-tuning denoising aggressiveness.
         * 
         * <p>The multiplier is applied to the automatically calculated threshold:</p>
         * <ul>
         *   <li>Values {@literal >} 1.0: More aggressive denoising (removes more noise but may lose signal details)</li>
         *   <li>Values {@literal <} 1.0: Less aggressive denoising (preserves more signal details but may retain noise)</li>
         *   <li>Value = 1.0: Uses the standard threshold calculation without adjustment</li>
         * </ul>
         * 
         * @param thresholdMultiplier the threshold multiplier (must be positive)
         * @return this builder
         */
        public Builder thresholdMultiplier(double thresholdMultiplier) {
            if (thresholdMultiplier <= 0) {
                throw new InvalidArgumentException("Threshold multiplier must be positive");
            }
            this.thresholdMultiplier = thresholdMultiplier;
            return this;
        }
        
        /**
         * Set the noise estimation method.
         * 
         * @param noiseEstimation the noise estimation method
         * @return this builder
         */
        public Builder noiseEstimation(NoiseEstimation noiseEstimation) {
            if (noiseEstimation == null) {
                throw new InvalidArgumentException("Noise estimation cannot be null");
            }
            this.noiseEstimation = noiseEstimation;
            return this;
        }
        
        /**
         * Set the noise window size for estimation.
         * 
         * @param noiseWindowSize the window size (must be positive)
         * @return this builder
         */
        public Builder noiseWindowSize(int noiseWindowSize) {
            if (noiseWindowSize <= 0) {
                throw new InvalidArgumentException("Noise window size must be positive");
            }
            this.noiseWindowSize = noiseWindowSize;
            return this;
        }
        
        /**
         * Build the streaming denoiser.
         * 
         * @return a new MODWTStreamingDenoiser instance
         */
        public MODWTStreamingDenoiser build() {
            return new MODWTStreamingDenoiser(this);
        }
    }
}
