package com.morphiqlabs.wavelet.cwt;

/**
 * FFT algorithm selection for CWT operations.
 * 
 * <p>Allows choosing between different FFT implementations based on
 * performance requirements and signal characteristics.</p>
 */
public enum FFTAlgorithm {
    /**
     * Automatic selection based on signal size and characteristics.
     * <ul>
     *   <li>Uses split-radix for power-of-2 sizes ≥ 32</li>
     *   <li>Uses radix-2 with vectorization for smaller sizes</li>
     *   <li>Uses Bluestein for non-power-of-2 sizes</li>
     * </ul>
     */
    AUTO,
    
    /**
     * Basic radix-2 Cooley-Tukey algorithm.
     * <ul>
     *   <li>Simple and reliable</li>
     *   <li>Good for small to medium sizes</li>
     *   <li>Requires power-of-2 size</li>
     * </ul>
     */
    RADIX2,
    
    /**
     * Split-radix algorithm for optimal performance.
     * <ul>
     *   <li>~25% fewer operations than radix-2</li>
     *   <li>Best for large power-of-2 sizes</li>
     *   <li>More complex implementation</li>
     * </ul>
     */
    SPLIT_RADIX,
    
    /**
     * Bluestein's algorithm for arbitrary sizes.
     * <ul>
     *   <li>Handles any size (not just power-of-2)</li>
     *   <li>Converts DFT to convolution</li>
     *   <li>Higher overhead but more flexible</li>
     * </ul>
     */
    BLUESTEIN,
    
    /**
     * Vectorized radix-2 using SIMD operations.
     * <ul>
     *   <li>Uses Java Vector API</li>
     *   <li>Best for modern CPUs with AVX/AVX2</li>
     *   <li>Requires power-of-2 size</li>
     * </ul>
     */
    RADIX2_VECTOR,
    
    /**
     * Optimized real-to-complex FFT.
     * <ul>
     *   <li>Exploits Hermitian symmetry</li>
     *   <li>~50% computation savings for real signals</li>
     *   <li>Ideal for CWT with real wavelets</li>
     * </ul>
     */
    REAL_OPTIMIZED
}