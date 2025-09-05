package com.morphiqlabs.examples.cwt.optimization;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.api.ComplexContinuousWavelet;
import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.cwt.ComplexMatrix;
import jdk.incubator.vector.*;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

/**
 * Cache-aware optimizations for Continuous Wavelet Transform.
 * 
 * <p>Implements blocking, tiling, and prefetching strategies to maximize
 * cache utilization and minimize memory bandwidth requirements.</p>
 */
public final class CacheAwareOps {
    public CacheAwareOps() {}
    
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();
    
    /**
     * Platform-adaptive cache configuration.
     */
    public static class CacheConfig {
        public final int l1CacheSize;
        public final int l2CacheSize;
        public final int cacheLineSize;
        public final int l1BlockSize;
        public final int l2BlockSize;
        public final int optimalBlockSize;
        public final int tileSize;
        
        private CacheConfig(int l1Size, int l2Size, int lineSize) {
            this.l1CacheSize = l1Size;
            this.l2CacheSize = l2Size;
            this.cacheLineSize = lineSize;
            
            // Calculate optimal block sizes based on cache hierarchy
            this.l1BlockSize = Math.min(512, l1Size / (3 * Double.BYTES)); // 3 arrays in L1
            this.l2BlockSize = Math.min(4096, l2Size / (4 * Double.BYTES)); // 4 arrays in L2
            
            // Ensure block sizes are multiples of vector length and cache line
            this.optimalBlockSize = (l1BlockSize / VECTOR_LENGTH) * VECTOR_LENGTH;
            this.tileSize = lineSize / Double.BYTES;
        }
        
        /**
         * Creates a cache configuration with explicit sizes.
         */
        public static CacheConfig create(int l1Size, int l2Size, int lineSize) {
            return new CacheConfig(l1Size, l2Size, lineSize);
        }
        
        /**
         * Creates default configuration for the current platform.
         */
        public static CacheConfig detectOrDefault() {
            // Conservative defaults (in bytes): L1=32KB, L2=1MB, line=64B
            return new CacheConfig(32 * 1024, 1024 * 1024, 64);
        }
        
        // Platform detection methods removed - now using PlatformDetector utility class
        // This improves maintainability and enables better testing of platform-specific behavior
    }
    
    // Default cache configuration (lazy-initialized)
    private static volatile CacheConfig defaultConfig;
    
    /**
     * Gets the default cache configuration for this platform.
     */
    public static CacheConfig getDefaultCacheConfig() {
        CacheConfig config = defaultConfig;
        if (config == null) {
            synchronized (CacheAwareOps.class) {
                config = defaultConfig;
                if (config == null) {
                    defaultConfig = config = CacheConfig.detectOrDefault();
                }
            }
        }
        return config;
    }
    
    // Cache parameters - removed static initialization to avoid circular dependencies
    // These methods provide backward compatibility while avoiding initialization issues
    
    /**
     * Computes convolution using cache-friendly blocking with default cache configuration.
     * 
     * @param signal input signal
     * @param wavelet wavelet coefficients
     * @param scale scale factor
     * @return convolution result
     */
    public double[] blockedConvolve(double[] signal, double[] wavelet, double scale) {
        return blockedConvolve(signal, wavelet, scale, getDefaultCacheConfig());
    }
    
    /**
     * Computes convolution using cache-friendly blocking with custom cache configuration.
     * 
     * @param signal input signal
     * @param wavelet wavelet coefficients
     * @param scale scale factor
     * @param cacheConfig cache configuration to use for optimization
     * @return convolution result
     */
    public double[] blockedConvolve(double[] signal, double[] wavelet, double scale, CacheConfig cacheConfig) {
        int signalLen = signal.length;
        int waveletLen = wavelet.length;
        double[] result = new double[signalLen];
        
        if (signalLen < cacheConfig.optimalBlockSize) {
            // Small signals - use direct computation
            return directConvolve(signal, wavelet, scale);
        }
        
        double sqrtScale = Math.sqrt(scale);
        int halfWavelet = waveletLen / 2;
        
        // Pre-scale wavelet
        double[] scaledWavelet = new double[waveletLen];
        for (int i = 0; i < waveletLen; i++) {
            scaledWavelet[i] = wavelet[i] / sqrtScale;
        }
        
        // Process signal in blocks to maximize cache reuse
        for (int blockStart = 0; blockStart < signalLen; blockStart += cacheConfig.optimalBlockSize) {
            int blockEnd = Math.min(blockStart + cacheConfig.optimalBlockSize, signalLen);
            
            // Process each output position in the block
            for (int tau = blockStart; tau < blockEnd; tau++) {
                double sum = 0.0;
                
                // Process wavelet coefficients
                for (int t = 0; t < waveletLen; t++) {
                    int idx = tau - halfWavelet + t;
                    if (idx >= 0 && idx < signalLen) {
                        sum += signal[idx] * scaledWavelet[t];
                    }
                }
                
                result[tau] = sum;
            }
        }
        
        return result;
    }
    
    /**
     * Direct convolution for small signals.
     */
    private double[] directConvolve(double[] signal, double[] wavelet, double scale) {
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
    
    /**
     * Computes complex convolution with cache optimization.
     */
    public ComplexMatrix blockedComplexConvolve(double[] signal, 
                                               ContinuousWavelet wavelet, 
                                               double scale) {
        int signalLen = signal.length;
        ComplexMatrix result = new ComplexMatrix(1, signalLen);
        
        // Generate wavelet samples
        int waveletSupport = (int)(8 * scale * wavelet.bandwidth());
        int halfSupport = waveletSupport / 2;
        
        // For complex wavelets, compute complex values
        ComplexContinuousWavelet complexWavelet = wavelet instanceof ComplexContinuousWavelet cw ? cw : null;
        
        // Process in blocks
        final int optimalBlockSize = getDefaultCacheConfig().optimalBlockSize;
        for (int blockStart = 0; blockStart < signalLen; blockStart += optimalBlockSize) {
            int blockEnd = Math.min(blockStart + optimalBlockSize, signalLen);
            
            for (int tau = blockStart; tau < blockEnd; tau++) {
                double sumReal = 0.0;
                double sumImag = 0.0;
                
                for (int t = -halfSupport; t <= halfSupport; t++) {
                    int idx = tau + t;
                    if (idx >= 0 && idx < signalLen) {
                        double signalValue = signal[idx];
                        double waveletReal = wavelet.psi(-t / scale) / Math.sqrt(scale);
                        
                        sumReal += signalValue * waveletReal;
                        
                        if (complexWavelet != null) {
                            double waveletImag = complexWavelet.psiImaginary(-t / scale) / Math.sqrt(scale);
                            sumImag += signalValue * waveletImag;
                        }
                    }
                }
                
                result.set(0, tau, sumReal, sumImag);
            }
        }
        
        return result;
    }
    
    /**
     * Normalizes matrix rows using tiled access pattern.
     */
    public void tiledNormalize(double[][] matrix) {
        int rows = matrix.length;
        if (rows == 0) return;
        int cols = matrix[0].length;
        if (cols == 0) return;
        
        // Process in tiles for better cache locality
        final CacheConfig cacheConfig = getDefaultCacheConfig();
        final int tileSize = cacheConfig.tileSize;
        final int optimalBlockSize = cacheConfig.optimalBlockSize;
        
        for (int rowStart = 0; rowStart < rows; rowStart += tileSize) {
            int rowEnd = Math.min(rowStart + tileSize, rows);
            
            // First pass: compute norms
            double[] norms = new double[rowEnd - rowStart];
            
            for (int r = rowStart; r < rowEnd; r++) {
                double sum = 0.0;
                double[] row = matrix[r];
                
                // Process columns in blocks
                for (int colStart = 0; colStart < cols; colStart += optimalBlockSize) {
                    int colEnd = Math.min(colStart + optimalBlockSize, cols);
                    
                    // Vectorized sum of squares
                    int c = colStart;
                    if (colEnd - colStart >= VECTOR_LENGTH) {
                        DoubleVector vSum = DoubleVector.zero(SPECIES);
                        
                        for (; c <= colEnd - VECTOR_LENGTH; c += VECTOR_LENGTH) {
                            DoubleVector v = DoubleVector.fromArray(SPECIES, row, c);
                            vSum = vSum.add(v.mul(v));
                        }
                        
                        sum += vSum.reduceLanes(VectorOperators.ADD);
                    }
                    
                    // Handle remainder
                    for (; c < colEnd; c++) {
                        sum += row[c] * row[c];
                    }
                }
                
                norms[r - rowStart] = Math.sqrt(sum);
            }
            
            // Second pass: normalize
            for (int r = rowStart; r < rowEnd; r++) {
                double norm = norms[r - rowStart];
                if (norm > 1e-10) { // Avoid division by zero
                    double invNorm = 1.0 / norm;
                    double[] row = matrix[r];
                    
                    // Vectorized normalization
                    int c = 0;
                    for (; c <= cols - VECTOR_LENGTH; c += VECTOR_LENGTH) {
                        DoubleVector v = DoubleVector.fromArray(SPECIES, row, c);
                        v = v.mul(invNorm);
                        v.intoArray(row, c);
                    }
                    
                    // Handle remainder
                    for (; c < cols; c++) {
                        row[c] *= invNorm;
                    }
                } else {
                    // If norm is zero, set row to unit vector
                    double[] row = matrix[r];
                    if (cols > 0) {
                        row[0] = 1.0;
                        for (int c = 1; c < cols; c++) {
                            row[c] = 0.0;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Computes multi-scale CWT with cache-aware blocking.
     */
    public double[][] computeMultiScaleBlocked(double[] signal, double[] scales,
                                              ContinuousWavelet wavelet) {
        int numScales = scales.length;
        int signalLen = signal.length;
        double[][] coefficients = new double[numScales][signalLen];
        
        // Process scales in groups that fit in L2 cache
        int scalesPerBlock = getDefaultCacheConfig().l2CacheSize / (signalLen * Double.BYTES);
        scalesPerBlock = Math.max(1, Math.min(scalesPerBlock, numScales));
        
        for (int scaleBlock = 0; scaleBlock < numScales; scaleBlock += scalesPerBlock) {
            int scaleEnd = Math.min(scaleBlock + scalesPerBlock, numScales);
            
            // Pre-generate wavelets for this block of scales
            double[][] wavelets = new double[scaleEnd - scaleBlock][];
            for (int s = scaleBlock; s < scaleEnd; s++) {
                double scale = scales[s];
                int waveletSupport = (int)(8 * scale * wavelet.bandwidth());
                wavelets[s - scaleBlock] = new double[waveletSupport];
                
                for (int i = 0; i < waveletSupport; i++) {
                    double t = (i - waveletSupport / 2.0) / scale;
                    wavelets[s - scaleBlock][i] = wavelet.psi(t);
                }
            }
            
            // Process signal blocks for all scales in this group
            for (int s = scaleBlock; s < scaleEnd; s++) {
                coefficients[s] = blockedConvolve(signal, 
                    wavelets[s - scaleBlock], scales[s]);
            }
        }
        
        return coefficients;
    }
    
    /**
     * Convolution with explicit prefetching hints.
     */
    public double[] convolveWithPrefetch(double[] signal, double[] wavelet, double scale) {
        // Note: Java doesn't have explicit prefetch instructions,
        // but we can structure access patterns for hardware prefetcher
        return blockedConvolve(signal, wavelet, scale);
    }
    
    /**
     * Creates an aligned array (best effort in Java).
     */
    public double[] createAlignedArray(int size) {
        // In Java, we can't directly control memory alignment,
        // but we can allocate arrays that are likely to be aligned
        final int tileSize = getDefaultCacheConfig().tileSize;
        int alignedSize = ((size + tileSize - 1) / tileSize) * tileSize;
        return new double[alignedSize];
    }
    
    /**
     * Gets optimal block size for current platform.
     */
    public int getOptimalBlockSize() {
        return getDefaultCacheConfig().optimalBlockSize;
    }
    
    /**
     * Selects caching strategy based on problem size.
     */
    public CacheStrategy selectStrategy(int signalLength, int waveletLength) {
        long totalOps = (long)signalLength * waveletLength;
        
        if (totalOps < 10_000) {
            return new CacheStrategy(true, false, false);
        } else if (totalOps < 1_000_000) {
            return new CacheStrategy(false, true, false);
        } else {
            return new CacheStrategy(false, false, true);
        }
    }
    
    /**
     * Creates streaming cache for real-time processing.
     */
    public StreamingCache createStreamingCache(int windowSize, int overlap, double[] scales) {
        return new StreamingCache(windowSize, overlap, scales);
    }
    
    /**
     * Processes streaming chunk with cache optimization.
     */
    public double[][] processStreamingChunk(StreamingCache cache, double[] chunk,
                                           ContinuousWavelet wavelet) {
        cache.addChunk(chunk);
        
        if (cache.isReady()) {
            double[] window = cache.getWindow();
            double[][] result = computeMultiScaleBlocked(window, cache.scales, wavelet);
            cache.advance();
            return result;
        }
        
        return null;
    }
    
    // Inner classes
    
    public static class CacheStrategy {
        private final boolean directComputation;
        private final boolean blockedComputation;
        private final boolean tiledComputation;
        
        public CacheStrategy(boolean direct, boolean blocked, boolean tiled) {
            this.directComputation = direct;
            this.blockedComputation = blocked;
            this.tiledComputation = tiled;
        }
        
        public boolean useDirectComputation() { return directComputation; }
        public boolean useBlockedComputation() { return blockedComputation; }
        public boolean useTiledComputation() { return tiledComputation; }
    }
    
    public static class StreamingCache {
        private final int windowSize;
        private final int overlap;
        private final double[] scales;
        private final double[] buffer;
        private int bufferPos;
        private int processedChunks;
        
        public StreamingCache(int windowSize, int overlap, double[] scales) {
            this.windowSize = windowSize;
            this.overlap = overlap;
            this.scales = scales.clone();
            this.buffer = new double[windowSize];
            this.bufferPos = 0;
            this.processedChunks = 0;
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
            return Arrays.copyOf(buffer, windowSize);
        }
        
        public void advance() {
            // Shift buffer by (windowSize - overlap)
            int shift = windowSize - overlap;
            System.arraycopy(buffer, shift, buffer, 0, overlap);
            bufferPos = overlap;
            processedChunks++;
        }
        
        public int getProcessedChunks() {
            return processedChunks;
        }
    }
}
