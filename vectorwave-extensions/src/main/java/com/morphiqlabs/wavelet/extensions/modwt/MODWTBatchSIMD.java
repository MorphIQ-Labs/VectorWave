package com.morphiqlabs.wavelet.extensions.modwt;

import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * SIMD-optimized batch operations for MODWT transforms.
 * 
 * <p>This class provides utility methods and information about SIMD capabilities
 * specifically for MODWT batch processing. Unlike DWT operations, MODWT preserves
 * signal length and does not perform downsampling.</p>
 */
public final class MODWTBatchSIMD {
    
    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final int VECTOR_LENGTH = SPECIES.length();
    
    private MODWTBatchSIMD() {
        // Utility class
    }
    
    /**
     * Get information about SIMD capabilities for MODWT batch processing.
     * 
     * @return String describing SIMD capabilities
     */
    public static String getBatchSIMDInfo() {
        StringBuilder info = new StringBuilder();
        info.append("MODWT Batch SIMD Capabilities:\n");
        info.append("  Vector species: ").append(SPECIES).append("\n");
        info.append("  Vector length: ").append(VECTOR_LENGTH).append(" doubles\n");
        info.append("  Optimal batch size: ").append(VECTOR_LENGTH).append(" signals\n");
        info.append("  Memory alignment: ").append(SPECIES.vectorByteSize()).append(" bytes\n");
        info.append("  MODWT features: Shift-invariant, no downsampling, arbitrary signal length\n");
        
        // Platform-specific info
        String arch = System.getProperty("os.arch");
        if (arch.contains("aarch64") || arch.contains("arm")) {
            info.append("  Platform: ARM (NEON optimizations)\n");
        } else if (arch.contains("amd64") || arch.contains("x86_64")) {
            info.append("  Platform: x86-64 (AVX optimizations)\n");
        }
        
        return info.toString();
    }
    
    /**
     * Check if batch size is optimal for SIMD processing.
     * 
     * @param batchSize the batch size to check
     * @return true if batch size is a multiple of vector length
     */
    public static boolean isOptimalBatchSize(int batchSize) {
        return batchSize % VECTOR_LENGTH == 0;
    }
    
    /**
     * Get the optimal batch size for SIMD processing.
     * Rounds up to the nearest multiple of vector length.
     * 
     * @param minBatchSize minimum desired batch size
     * @return optimal batch size for SIMD
     */
    public static int getOptimalBatchSize(int minBatchSize) {
        return ((minBatchSize + VECTOR_LENGTH - 1) / VECTOR_LENGTH) * VECTOR_LENGTH;
    }
    
    /**
     * Get the SIMD vector length for the current platform.
     * 
     * @return number of doubles that fit in a SIMD vector
     */
    public static int getVectorLength() {
        return VECTOR_LENGTH;
    }
}
