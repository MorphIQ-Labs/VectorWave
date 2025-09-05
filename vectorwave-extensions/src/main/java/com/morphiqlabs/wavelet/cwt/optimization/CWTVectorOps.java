package com.morphiqlabs.wavelet.extensions.cwt.optimization;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.cwt.ComplexMatrix;
import com.morphiqlabs.wavelet.util.PlatformDetector;
import jdk.incubator.vector.*;
import java.util.stream.IntStream;

/**
 * SIMD-optimized operations for Continuous Wavelet Transform.
 * 
 * <p>Leverages Java 24's Vector API for significant performance improvements
 * in CWT computations including convolution, multi-scale analysis, and
 * complex arithmetic.</p>
 * 
 * <p>Platform detection combines CPU capabilities with JVM Vector API support
 * to ensure optimal performance. The Vector API's preferred species length
 * indicates what the JVM can efficiently use, which we validate against
 * actual CPU instruction set support.</p>
 */
public final class CWTVectorOps {
    
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();
    
    // Platform detection
    private static final boolean IS_APPLE_SILICON = PlatformDetector.isAppleSilicon();
    
    // Proper AVX detection based on platform capabilities
    // Note: Vector API's SPECIES length indicates what the JVM can use, not necessarily CPU support
    private static final boolean HAS_AVX512 = PlatformDetector.hasAVX512Support() && SPECIES.length() >= 8;
    private static final boolean HAS_AVX2 = PlatformDetector.hasAVX2Support() && SPECIES.length() >= 4;
    
    // Optimization thresholds
    private static final int SIMD_THRESHOLD = PlatformDetector.getRecommendedSIMDThreshold();
    private static final int FFT_THRESHOLD = 256;
    private static final int BLOCK_SIZE = 64; // Cache-friendly block size
    
    /**
     * Padding modes for boundary handling.
     */
    public enum PaddingMode {
        ZERO, REFLECT, PERIODIC, SYMMETRIC
    }
    
    /**
     * Computes convolution of signal with scaled wavelet using SIMD.
     * 
     * @param signal input signal
     * @param wavelet wavelet coefficients
     * @param scale scale factor
     * @return convolution result
     */
    public double[] convolve(double[] signal, double[] wavelet, double scale) {
        int signalLen = signal.length;
        int waveletLen = wavelet.length;
        
        if (signalLen < SIMD_THRESHOLD || waveletLen < SIMD_THRESHOLD) {
            return scalarConvolve(signal, wavelet, scale);
        }
        
        return vectorConvolve(signal, wavelet, scale);
    }
    
    /**
     * Vectorized convolution implementation.
     */
    private double[] vectorConvolve(double[] signal, double[] wavelet, double scale) {
        int signalLen = signal.length;
        int waveletLen = wavelet.length;
        double[] result = new double[signalLen];
        
        double sqrtScale = Math.sqrt(scale);
        int halfWavelet = waveletLen / 2;
        
        // Pre-scale wavelet
        double[] scaledWavelet = new double[waveletLen];
        for (int i = 0; i < waveletLen; i++) {
            scaledWavelet[i] = wavelet[i] / sqrtScale;
        }
        
        // Main convolution loop
        for (int tau = 0; tau < signalLen; tau++) {
            double scalarSum = 0.0;
            
            // Process in chunks when beneficial
            if (waveletLen >= VECTOR_LENGTH) {
                DoubleVector sum = DoubleVector.zero(SPECIES);
                
                // Find valid range for vectorization
                int startIdx = Math.max(0, tau - halfWavelet);
                int endIdx = Math.min(signalLen, tau - halfWavelet + waveletLen);
                int waveletOffset = Math.max(0, halfWavelet - tau);
                
                int validStart = startIdx;
                int validEnd = Math.min(endIdx, startIdx + (waveletLen - waveletOffset));
                
                // Vectorized processing of valid range
                int i = validStart;
                int w = waveletOffset + (validStart - startIdx);
                
                for (; i <= validEnd - VECTOR_LENGTH && w <= waveletLen - VECTOR_LENGTH; 
                     i += VECTOR_LENGTH, w += VECTOR_LENGTH) {
                    DoubleVector sig = DoubleVector.fromArray(SPECIES, signal, i);
                    DoubleVector wav = DoubleVector.fromArray(SPECIES, scaledWavelet, w);
                    sum = sum.add(sig.mul(wav));
                }
                
                scalarSum = sum.reduceLanes(VectorOperators.ADD);
                
                // Handle remaining elements
                for (; i < validEnd && w < waveletLen; i++, w++) {
                    scalarSum += signal[i] * scaledWavelet[w];
                }
            } else {
                // Scalar fallback for small wavelets
                for (int t = 0; t < waveletLen; t++) {
                    int idx = tau - halfWavelet + t;
                    if (idx >= 0 && idx < signalLen) {
                        scalarSum += signal[idx] * scaledWavelet[t];
                    }
                }
            }
            
            result[tau] = scalarSum;
        }
        
        return result;
    }
    
    /**
     * Scalar convolution fallback.
     */
    private double[] scalarConvolve(double[] signal, double[] wavelet, double scale) {
        int signalLen = signal.length;
        int waveletLen = wavelet.length;
        double[] result = new double[signalLen];
        
        double sqrtScale = Math.sqrt(scale);
        int halfWavelet = waveletLen / 2;
        
        for (int tau = 0; tau < signalLen; tau++) {
            double sum = 0.0;
            
            for (int t = 0; t < waveletLen; t++) {
                int idx = tau - halfWavelet + t;
                if (idx >= 0 && idx < signalLen) {
                    sum += signal[idx] * wavelet[t] / sqrtScale;
                }
            }
            
            result[tau] = sum;
        }
        
        return result;
    }
    
    // Complex operations handler - static since it contains no mutable state
    private static final ComplexVectorOps complexOps = new ComplexVectorOps();
    
    /**
     * Computes complex convolution for complex wavelets using direct vectorization.
     * 
     * <p>This implementation avoids temporary array allocations by processing
     * data directly from the source arrays using a sliding window approach.</p>
     */
    public ComplexMatrix complexConvolve(double[] realSignal, double[] imagSignal,
                                       double[] realWavelet, double[] imagWavelet,
                                       double scale) {
        if (scale <= 0) {
            throw new IllegalArgumentException("Scale must be positive, got: " + scale);
        }
        
        int signalLen = realSignal.length;
        int waveletLen = realWavelet.length;
        
        ComplexMatrix result = new ComplexMatrix(1, signalLen);
        
        if (signalLen < SIMD_THRESHOLD) {
            return scalarComplexConvolve(realSignal, imagSignal, 
                                       realWavelet, imagWavelet, scale);
        }
        
        double sqrtScale = Math.sqrt(scale);
        double invSqrtScale = 1.0 / sqrtScale;
        int halfWavelet = waveletLen / 2;
        
        // Pre-scale wavelets using vectorized operations
        double[] scaledRealWav = new double[waveletLen];
        double[] scaledImagWav = new double[waveletLen];
        complexOps.complexScalarMultiply(realWavelet, imagWavelet, 
                                        invSqrtScale, 0.0,
                                        scaledRealWav, scaledImagWav);
        
        // Process convolution directly without temporary arrays
        for (int tau = 0; tau < signalLen; tau++) {
            // Find valid range for this position
            int startIdx = Math.max(0, tau - halfWavelet);
            int endIdx = Math.min(signalLen, tau + halfWavelet + 1);
            int waveletStart = Math.max(0, halfWavelet - tau);
            
            // Direct vectorized accumulation
            DoubleVector vRealSum = DoubleVector.zero(SPECIES);
            DoubleVector vImagSum = DoubleVector.zero(SPECIES);
            
            int sigIdx = startIdx;
            int wavIdx = waveletStart;
            int remaining = endIdx - startIdx;
            
            // Process with SIMD where possible
            while (remaining >= VECTOR_LENGTH && wavIdx + VECTOR_LENGTH <= waveletLen) {
                // Load signal values directly from arrays
                DoubleVector sigReal = DoubleVector.fromArray(SPECIES, realSignal, sigIdx);
                DoubleVector sigImag = (imagSignal != null) ? 
                    DoubleVector.fromArray(SPECIES, imagSignal, sigIdx) : 
                    DoubleVector.zero(SPECIES);
                
                // Load wavelet values directly from arrays
                DoubleVector wavReal = DoubleVector.fromArray(SPECIES, scaledRealWav, wavIdx);
                DoubleVector wavImag = DoubleVector.fromArray(SPECIES, scaledImagWav, wavIdx);
                
                // Complex multiplication and accumulation in one step
                // real += sigReal * wavReal - sigImag * wavImag
                // imag += sigReal * wavImag + sigImag * wavReal
                vRealSum = vRealSum.add(sigReal.mul(wavReal).sub(sigImag.mul(wavImag)));
                vImagSum = vImagSum.add(sigReal.mul(wavImag).add(sigImag.mul(wavReal)));
                
                sigIdx += VECTOR_LENGTH;
                wavIdx += VECTOR_LENGTH;
                remaining -= VECTOR_LENGTH;
            }
            
            // Reduce vector accumulators
            double realSum = vRealSum.reduceLanes(VectorOperators.ADD);
            double imagSum = vImagSum.reduceLanes(VectorOperators.ADD);
            
            // Handle remainder with scalar operations
            while (remaining > 0 && wavIdx < waveletLen) {
                double sigR = realSignal[sigIdx];
                double sigI = (imagSignal != null) ? imagSignal[sigIdx] : 0.0;
                double wavR = scaledRealWav[wavIdx];
                double wavI = scaledImagWav[wavIdx];
                
                realSum += sigR * wavR - sigI * wavI;
                imagSum += sigR * wavI + sigI * wavR;
                
                sigIdx++;
                wavIdx++;
                remaining--;
            }
            
            result.set(0, tau, realSum, imagSum);
        }
        
        return result;
    }
    
    /**
     * Scalar complex convolution fallback.
     */
    private ComplexMatrix scalarComplexConvolve(double[] realSignal, double[] imagSignal,
                                               double[] realWavelet, double[] imagWavelet,
                                               double scale) {
        int signalLen = realSignal.length;
        int waveletLen = realWavelet.length;
        ComplexMatrix result = new ComplexMatrix(1, signalLen);
        
        if (scale <= 0) {
            throw new IllegalArgumentException("Scale must be positive, got: " + scale);
        }
        
        double sqrtScale = Math.sqrt(scale);
        int halfWavelet = waveletLen / 2;
        
        for (int tau = 0; tau < signalLen; tau++) {
            double realSum = 0.0;
            double imagSum = 0.0;
            
            for (int t = 0; t < waveletLen; t++) {
                int idx = tau - halfWavelet + t;
                if (idx >= 0 && idx < signalLen) {
                    double sigR = realSignal[idx];
                    double sigI = (imagSignal != null) ? imagSignal[idx] : 0.0;
                    double wavR = realWavelet[t] / sqrtScale;
                    double wavI = imagWavelet[t] / sqrtScale;
                    
                    realSum += sigR * wavR - sigI * wavI;
                    imagSum += sigR * wavI + sigI * wavR;
                }
            }
            
            result.set(0, tau, realSum, imagSum);
        }
        
        return result;
    }
    
    /**
     * Computes CWT for multiple scales in parallel.
     */
    public double[][] computeMultiScale(double[] signal, double[] scales, 
                                      ContinuousWavelet wavelet) {
        return computeMultiScale(signal, scales, wavelet, true);
    }
    
    /**
     * Computes CWT for multiple scales with optional parallel processing.
     */
    public double[][] computeMultiScale(double[] signal, double[] scales, 
                                      ContinuousWavelet wavelet, boolean useParallel) {
        int numScales = scales.length;
        int signalLen = signal.length;
        double[][] coefficients = new double[numScales][signalLen];
        
        if (useParallel && numScales >= 4) {
            // Parallel processing for multiple scales
            IntStream.range(0, numScales).parallel().forEach(s -> {
                coefficients[s] = computeSingleScale(signal, scales[s], wavelet);
            });
        } else {
            // Sequential processing for few scales or when parallel disabled
            for (int s = 0; s < numScales; s++) {
                coefficients[s] = computeSingleScale(signal, scales[s], wavelet);
            }
        }
        
        return coefficients;
    }
    
    /**
     * Computes CWT coefficients for a single scale.
     */
    private double[] computeSingleScale(double[] signal, double scale, ContinuousWavelet wavelet) {
        int waveletSupport = (int)(8 * scale * wavelet.bandwidth());
        double[] scaledWavelet = new double[waveletSupport];
        
        // Sample wavelet at scale
        for (int i = 0; i < waveletSupport; i++) {
            double t = (i - waveletSupport / 2.0) / scale;
            scaledWavelet[i] = wavelet.psi(t);
        }
        
        // Convolve with signal
        return convolve(signal, scaledWavelet, scale);
    }
    
    /**
     * Normalizes coefficients by scale factor.
     */
    public void normalizeByScale(double[][] coefficients, double[] scales) {
        int numScales = coefficients.length;
        int signalLen = coefficients[0].length;
        
        for (int s = 0; s < numScales; s++) {
            double normFactor = 1.0 / Math.sqrt(scales[s]);
            double[] row = coefficients[s];
            
            // Vectorized normalization
            int t = 0;
            for (; t < signalLen - VECTOR_LENGTH + 1; t += VECTOR_LENGTH) {
                DoubleVector vec = DoubleVector.fromArray(SPECIES, row, t);
                vec = vec.mul(normFactor);
                vec.intoArray(row, t);
            }
            
            // Handle remainder
            for (; t < signalLen; t++) {
                row[t] *= normFactor;
            }
        }
    }
    
    /**
     * Computes magnitude from complex coefficients using SIMD.
     */
    public double[][] computeMagnitude(ComplexMatrix complex) {
        int rows = complex.getRows();
        int cols = complex.getCols();
        double[][] magnitude = new double[rows][cols];
        
        double[][] real = complex.getReal();
        double[][] imag = complex.getImaginary();
        
        // Use optimized complex magnitude computation
        for (int r = 0; r < rows; r++) {
            complexOps.complexMagnitude(real[r], imag[r], magnitude[r]);
        }
        
        return magnitude;
    }
    
    /**
     * Computes power spectrum (magnitude squared) using SIMD.
     */
    public double[][] computePowerSpectrum(double[][] coefficients) {
        int rows = coefficients.length;
        int cols = coefficients[0].length;
        double[][] power = new double[rows][cols];
        
        for (int r = 0; r < rows; r++) {
            double[] row = coefficients[r];
            double[] powerRow = power[r];
            
            int c = 0;
            for (; c < cols - VECTOR_LENGTH + 1; c += VECTOR_LENGTH) {
                DoubleVector vec = DoubleVector.fromArray(SPECIES, row, c);
                DoubleVector pow = vec.mul(vec);
                pow.intoArray(powerRow, c);
            }
            
            for (; c < cols; c++) {
                powerRow[c] = row[c] * row[c];
            }
        }
        
        return power;
    }
    
    /**
     * Convolution with specified padding mode.
     */
    public double[] convolveWithPadding(double[] signal, double[] wavelet, 
                                      double scale, PaddingMode padding) {
        // Extend signal based on padding mode
        int padSize = wavelet.length;
        double[] paddedSignal = new double[signal.length + 2 * padSize];
        
        // Copy original signal to center
        System.arraycopy(signal, 0, paddedSignal, padSize, signal.length);
        
        // Apply padding
        switch (padding) {
            case ZERO -> {
                // Already zeros - no action needed
            }
                
            case REFLECT -> {
                for (int i = 0; i < padSize; i++) {
                    paddedSignal[i] = signal[Math.min(padSize - i, signal.length - 1)];
                    paddedSignal[paddedSignal.length - 1 - i] = 
                        signal[Math.max(signal.length - padSize + i - 1, 0)];
                }
            }
                
            case PERIODIC -> {
                for (int i = 0; i < padSize; i++) {
                    paddedSignal[i] = signal[signal.length - padSize + i];
                    paddedSignal[paddedSignal.length - padSize + i] = signal[i];
                }
            }
                
            case SYMMETRIC -> {
                for (int i = 0; i < padSize; i++) {
                    paddedSignal[i] = signal[padSize - i - 1];
                    paddedSignal[paddedSignal.length - 1 - i] = 
                        signal[signal.length - padSize + i];
                }
            }
        }
        
        // Convolve padded signal
        double[] fullResult = convolve(paddedSignal, wavelet, scale);
        
        // Extract valid portion
        double[] result = new double[signal.length];
        System.arraycopy(fullResult, padSize, result, 0, signal.length);
        
        return result;
    }
    
    /**
     * Optimization strategy selection.
     */
    public OptimizationStrategy selectStrategy(int signalLength, int waveletLength) {
        if (signalLength * waveletLength < SIMD_THRESHOLD * SIMD_THRESHOLD) {
            return new OptimizationStrategy(true, false, false);
        } else if (signalLength > FFT_THRESHOLD && waveletLength > 32) {
            return new OptimizationStrategy(false, false, true);
        } else {
            return new OptimizationStrategy(false, true, false);
        }
    }
    
    /**
     * Streaming context for processing signals in chunks.
     */
    public StreamingContext createStreamingContext(int windowSize, int hopSize, 
                                                  double[] scales) {
        return new StreamingContext(windowSize, hopSize, scales);
    }
    
    /**
     * Process a chunk in streaming mode.
     * 
     * @param context streaming context containing buffer and parameters
     * @param chunk data chunk to process
     * @param wavelet wavelet coefficients
     * @return CWT coefficients for all scales if enough data is available, null otherwise
     */
    public double[][] processStreamingChunk(StreamingContext context, 
                                          double[] chunk, double[] wavelet) {
        context.addChunk(chunk);
        
        if (context.isReady()) {
            double[] window = context.getWindow();
            double[][] result = new double[context.scales.length][];
            
            for (int s = 0; s < context.scales.length; s++) {
                result[s] = convolve(window, wavelet, context.scales[s]);
            }
            
            context.advance();
            return result;
        }
        
        return null; // Not enough data yet
    }
    
    /**
     * Get platform information.
     */
    public PlatformInfo getPlatformInfo() {
        return new PlatformInfo(
            SPECIES,
            SPECIES.length() * Double.BYTES,
            IS_APPLE_SILICON,
            HAS_AVX512,
            HAS_AVX2,
            true // SIMD support with Vector API
        );
    }
    
    // Inner classes
    
    public static class OptimizationStrategy {
        private final boolean directComputation;
        private final boolean blockedComputation;
        private final boolean useFFT;
        
        public OptimizationStrategy(boolean direct, boolean blocked, boolean fft) {
            this.directComputation = direct;
            this.blockedComputation = blocked;
            this.useFFT = fft;
        }
        
        public boolean useDirectComputation() { return directComputation; }
        public boolean useBlockedComputation() { return blockedComputation; }
        public boolean useFFT() { return useFFT; }
    }
    
    public static class StreamingContext {
        private final int windowSize;
        private final int hopSize;
        private final double[] scales;
        private final double[] buffer;
        private int bufferPos;
        
        public StreamingContext(int windowSize, int hopSize, double[] scales) {
            this.windowSize = windowSize;
            this.hopSize = hopSize;
            this.scales = scales;
            this.buffer = new double[windowSize];
            this.bufferPos = 0;
        }
        
        public void addChunk(double[] chunk) {
            int copySize = Math.min(chunk.length, buffer.length - bufferPos);
            System.arraycopy(chunk, 0, buffer, bufferPos, copySize);
            bufferPos += copySize;
        }
        
        public boolean isReady() {
            return bufferPos >= windowSize;
        }
        
        public double[] getWindow() {
            return buffer.clone();
        }
        
        public void advance() {
            // Shift buffer by hop size
            System.arraycopy(buffer, hopSize, buffer, 0, windowSize - hopSize);
            bufferPos = windowSize - hopSize;
        }
    }
    
    public record PlatformInfo(
        VectorSpecies<Double> vectorSpecies,
        int vectorLength,
        boolean isAppleSilicon,
        boolean hasAVX512,
        boolean hasAVX2,
        boolean supportsSIMD
    ) {}
}
