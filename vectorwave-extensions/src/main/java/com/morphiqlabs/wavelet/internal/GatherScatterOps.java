package com.morphiqlabs.wavelet.extensions.internal;

import com.morphiqlabs.wavelet.util.PlatformDetector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.VectorMask;
import jdk.incubator.vector.VectorSpecies;

import java.nio.ByteOrder;

/**
 * Optimized gather/scatter operations using Vector API intrinsics.
 *
 * <p>These operations are crucial for wavelet transforms which often
 * access non-contiguous memory locations due to downsampling and
 * boundary wrapping.</p>
 */
final class GatherScatterOps {

    private static final VectorSpecies<Double> DOUBLE_SPECIES = DoubleVector.SPECIES_PREFERRED;
    private static final VectorSpecies<Integer> INT_SPECIES = IntVector.SPECIES_PREFERRED;
    private static final int DOUBLE_LENGTH = DOUBLE_SPECIES.length();
    private static final int INT_LENGTH = INT_SPECIES.length();

    // Platform detection
    private static final boolean GATHER_SCATTER_AVAILABLE = checkGatherScatterSupport();
    private static final boolean IS_LITTLE_ENDIAN = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    
    // Test configuration
    private static volatile boolean testModeEnabled = false;
    private static volatile boolean testGatherScatterAvailable = false;

    private GatherScatterOps() {
        // Utility class
    }
    
    /**
     * Enable test mode with specified gather/scatter availability.
     * This is package-private and should only be used in tests.
     * 
     * @param available whether gather/scatter should be considered available
     */
    static void setTestMode(boolean available) {
        testModeEnabled = true;
        testGatherScatterAvailable = available;
    }
    
    /**
     * Disable test mode and revert to platform detection.
     * This is package-private and should only be used in tests.
     */
    static void clearTestMode() {
        testModeEnabled = false;
        testGatherScatterAvailable = false;
    }
    
    /**
     * Check if gather/scatter is available, considering test mode.
     */
    private static boolean isGatherScatterEnabled() {
        return testModeEnabled ? testGatherScatterAvailable : GATHER_SCATTER_AVAILABLE;
    }

    /**
     * Check if gather/scatter operations are available on this platform.
     * 
     * <p>Gather/scatter operations require:</p>
     * <ul>
     *   <li>Vector API support with sufficient vector length</li>
     *   <li>Platform-specific intrinsics (AVX2+ on x86, SVE on ARM)</li>
     *   <li>Not universally supported on all platforms</li>
     * </ul>
     */
    private static boolean checkGatherScatterSupport() {
        // Java 24: enable gather/scatter with manual compress implementation
        try {
            // Check basic requirements
            if (DOUBLE_SPECIES.length() < 4) {
                // Need at least 256-bit vectors for efficient gather/scatter
                return false;
            }
            
            // Platform-specific checks
            PlatformDetector.Platform platform = PlatformDetector.getPlatform();
            
            switch (platform) {
                case X86_64:
                    // x86-64 needs AVX2 or later for gather operations
                    // Check if we can perform a gather operation
                    return checkX86GatherSupport();
                    
                case APPLE_SILICON:
                    // Apple Silicon M1/M2/M3 have good gather/scatter support
                    // but through different intrinsics than x86
                    return true;
                    
                case ARM:
                    // Generic ARM may or may not have SVE/SVE2
                    // Conservative approach: disable for now
                    return false;
                    
                default:
                    return false;
            }
        } catch (Exception e) {
            // If any check fails, disable gather/scatter
            return false;
        }
    }
    
    /**
     * Check x86-64 gather support by attempting a gather operation.
     * 
     * Note: The Java Vector API's gather support is limited and may not
     * be available even on platforms with hardware gather instructions.
     */
    private static boolean checkX86GatherSupport() {
        // The Vector API's gather operation (fromArray with indices) has very
        // limited support and often throws exceptions even on capable hardware.
        // For now, we'll disable gather/scatter to ensure stability.
        // This can be revisited when the Vector API matures.
        return false;
    }

    /**
     * Optimized gather for periodic boundary convolution.
     * Gathers values from signal at computed indices for downsampling.
     */
    public static double[] gatherPeriodicDownsample(double[] signal, double[] filter,
                                                    int signalLength, int filterLength) {
        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        if (!isGatherScatterEnabled() || DOUBLE_LENGTH < 4) {
            // Fallback to scalar implementation
            return scalarPeriodicDownsample(signal, filter, signalLength, filterLength);
        }

        // Process with gather operations
        int i = 0;
        for (; i <= outputLength - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            DoubleVector sum = DoubleVector.zero(DOUBLE_SPECIES);

            for (int k = 0; k < filterLength; k++) {
                // Create index vector for gather
                int[] indices = new int[DOUBLE_LENGTH];
                for (int v = 0; v < DOUBLE_LENGTH; v++) {
                    indices[v] = (2 * (i + v) + k) % signalLength;
                }

                // Gather signal values manually (gather not available on all platforms)
                double[] gathered = new double[DOUBLE_LENGTH];
                for (int j = 0; j < DOUBLE_LENGTH; j++) {
                    gathered[j] = signal[indices[j]];
                }
                DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, gathered, 0);

                // Multiply by filter coefficient and accumulate
                DoubleVector filterVec = DoubleVector.broadcast(DOUBLE_SPECIES, filter[k]);
                sum = sum.add(values.mul(filterVec));
            }

            // Store result
            sum.intoArray(output, i);
        }

        // Handle remainder with scalar code
        for (; i < outputLength; i++) {
            double sum = 0.0;
            for (int k = 0; k < filterLength; k++) {
                int idx = (2 * i + k) % signalLength;
                sum += filter[k] * signal[idx];
            }
            output[i] = sum;
        }

        return output;
    }

    /**
     * Optimized scatter for upsampling during inverse transform.
     * Scatters values to non-contiguous locations efficiently.
     */
    public static void scatterUpsample(double[] approx, double[] detail,
                                       double[] output, int length) {
        if (!isGatherScatterEnabled() || DOUBLE_LENGTH < 4) {
            // Fallback to scalar implementation
            scalarUpsample(approx, detail, output, length);
            return;
        }

        int halfLength = length / 2;

        // Process with scatter operations
        int i = 0;
        for (; i <= halfLength - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            // Load approximation and detail coefficients
            DoubleVector approxVec = DoubleVector.fromArray(DOUBLE_SPECIES, approx, i);
            DoubleVector detailVec = DoubleVector.fromArray(DOUBLE_SPECIES, detail, i);

            // Create index vectors for scatter
            int[] evenIndices = new int[DOUBLE_LENGTH];
            int[] oddIndices = new int[DOUBLE_LENGTH];
            for (int v = 0; v < DOUBLE_LENGTH; v++) {
                evenIndices[v] = 2 * (i + v);
                oddIndices[v] = 2 * (i + v) + 1;
            }

            // Scatter manually (scatter not available on all platforms)
            double[] approxArray = new double[DOUBLE_LENGTH];
            double[] detailArray = new double[DOUBLE_LENGTH];
            approxVec.intoArray(approxArray, 0);
            detailVec.intoArray(detailArray, 0);

            for (int j = 0; j < DOUBLE_LENGTH; j++) {
                output[evenIndices[j]] = approxArray[j];
                output[oddIndices[j]] = detailArray[j];
            }
        }

        // Handle remainder with scalar code
        for (; i < halfLength; i++) {
            output[2 * i] = approx[i];
            output[2 * i + 1] = detail[i];
        }
    }

    /**
     * Batch gather operation for multiple signals.
     * Efficiently processes multiple gather operations in parallel.
     */
    public static void batchGather(double[][] signals, int[] indices,
                                   double[][] results, int count) {
        if (!isGatherScatterEnabled()) {
            batchGatherScalar(signals, indices, results, count);
            return;
        }

        int numSignals = signals.length;

        // Process multiple signals simultaneously
        for (int s = 0; s < numSignals; s++) {
            double[] signal = signals[s];
            double[] result = results[s];

            int i = 0;
            for (; i <= count - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
                // Gather values manually
                double[] gathered = new double[DOUBLE_LENGTH];
                for (int j = 0; j < DOUBLE_LENGTH; j++) {
                    gathered[j] = signal[indices[i + j]];
                }
                DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, gathered, 0);

                // Store gathered values
                values.intoArray(result, i);
            }

            // Handle remainder
            for (; i < count; i++) {
                result[i] = signal[indices[i]];
            }
        }
    }

    /**
     * Optimized gather for strided access patterns.
     * Common in multi-level wavelet decomposition.
     */
    public static double[] gatherStrided(double[] signal, int offset,
                                         int stride, int count) {
        double[] result = new double[count];

        if (!isGatherScatterEnabled() || stride > 8) {
            // Fallback for large strides or no gather support
            for (int i = 0; i < count; i++) {
                result[i] = signal[offset + i * stride];
            }
            return result;
        }

        // Use gather for strided access
        int i = 0;
        for (; i <= count - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            // Create strided indices
            int[] indices = new int[DOUBLE_LENGTH];
            for (int v = 0; v < DOUBLE_LENGTH; v++) {
                indices[v] = offset + (i + v) * stride;
            }

            // Gather values manually
            double[] gathered = new double[DOUBLE_LENGTH];
            for (int j = 0; j < DOUBLE_LENGTH; j++) {
                gathered[j] = signal[indices[j]];
            }
            DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, gathered, 0);

            // Store result
            values.intoArray(result, i);
        }

        // Handle remainder
        for (; i < count; i++) {
            result[i] = signal[offset + i * stride];
        }

        return result;
    }

    /**
     * Compressed gather operation for sparse access patterns.
     * Uses a mask to gather only selected elements.
     */
    public static double[] gatherCompressed(double[] signal, boolean[] mask) {
        // Count true elements
        int count = 0;
        for (boolean b : mask) {
            if (b) count++;
        }

        double[] result = new double[count];
        int resultIdx = 0;

        if (!isGatherScatterEnabled()) {
            // Scalar fallback
            for (int i = 0; i < mask.length; i++) {
                if (mask[i]) {
                    result[resultIdx++] = signal[i];
                }
            }
            return result;
        }

        // Process with vector mask
        int i = 0;
        for (; i <= mask.length - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            // Create mask vector
            boolean[] maskChunk = new boolean[DOUBLE_LENGTH];
            System.arraycopy(mask, i, maskChunk, 0, DOUBLE_LENGTH);
            VectorMask<Double> vectorMask = VectorMask.fromArray(DOUBLE_SPECIES, maskChunk, 0);

            // Load values under mask
            DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, signal, i, vectorMask);

            // Compress values under mask
            // DoubleVector compressed = values.compress(vectorMask); // Manual compress implementation for consistency
            int compressedLength = vectorMask.trueCount();
            
            // Only copy the valid compressed elements
            if (compressedLength > 0) {
                double[] temp = new double[DOUBLE_LENGTH];
                values.intoArray(temp, 0);  // Extract all values from vector
                
                // Manually compress by selecting only masked values
                int writeIdx = 0;
                for (int j = 0; j < DOUBLE_LENGTH && writeIdx < compressedLength; j++) {
                    if (maskChunk[j]) {
                        result[resultIdx + writeIdx] = temp[j];
                        writeIdx++;
                    }
                }
            }
            
            resultIdx += compressedLength;
        }

        // Handle remainder
        for (; i < mask.length; i++) {
            if (mask[i]) {
                result[resultIdx++] = signal[i];
            }
        }

        return result;
    }

    // Scalar fallback implementations

    private static double[] scalarPeriodicDownsample(double[] signal, double[] filter,
                                                     int signalLength, int filterLength) {
        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        for (int i = 0; i < outputLength; i++) {
            double sum = 0.0;
            for (int k = 0; k < filterLength; k++) {
                int idx = (2 * i + k) % signalLength;
                sum += filter[k] * signal[idx];
            }
            output[i] = sum;
        }

        return output;
    }

    private static void scalarUpsample(double[] approx, double[] detail,
                                       double[] output, int length) {
        int halfLength = length / 2;
        for (int i = 0; i < halfLength; i++) {
            output[2 * i] = approx[i];
            output[2 * i + 1] = detail[i];
        }
    }

    private static void batchGatherScalar(double[][] signals, int[] indices,
                                          double[][] results, int count) {
        for (int s = 0; s < signals.length; s++) {
            for (int i = 0; i < count; i++) {
                results[s][i] = signals[s][indices[i]];
            }
        }
    }

    /**
     * Check if gather/scatter operations are available.
     */
    public static boolean isGatherScatterAvailable() {
        return isGatherScatterEnabled();
    }
    
    /**
     * Package-private method for testing vector paths.
     * This allows tests to execute vector code even when gather/scatter is not available.
     */
    static double[] gatherPeriodicDownsampleForceVector(double[] signal, double[] filter,
                                                         int signalLength, int filterLength) {
        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];
        
        // Force vector path regardless of GATHER_SCATTER_AVAILABLE
        if (DOUBLE_LENGTH < 4) {
            // Still fall back if vector length is too small
            return scalarPeriodicDownsample(signal, filter, signalLength, filterLength);
        }
        
        // Vector implementation (copied from main method)
        int i = 0;
        for (; i <= outputLength - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            DoubleVector sum = DoubleVector.zero(DOUBLE_SPECIES);

            for (int k = 0; k < filterLength; k++) {
                // Create index vector for gather
                int[] indices = new int[DOUBLE_LENGTH];
                for (int v = 0; v < DOUBLE_LENGTH; v++) {
                    indices[v] = (2 * (i + v) + k) % signalLength;
                }

                // Gather signal values manually (gather not available on all platforms)
                double[] gathered = new double[DOUBLE_LENGTH];
                for (int j = 0; j < DOUBLE_LENGTH; j++) {
                    gathered[j] = signal[indices[j]];
                }
                DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, gathered, 0);

                // Multiply by filter coefficient and accumulate
                DoubleVector filterVec = DoubleVector.broadcast(DOUBLE_SPECIES, filter[k]);
                sum = sum.add(values.mul(filterVec));
            }

            // Store result
            sum.intoArray(output, i);
        }

        // Handle remainder with scalar code
        for (; i < outputLength; i++) {
            double sum = 0.0;
            for (int k = 0; k < filterLength; k++) {
                int idx = (2 * i + k) % signalLength;
                sum += filter[k] * signal[idx];
            }
            output[i] = sum;
        }

        return output;
    }
    
    /**
     * Package-private method for testing vector scatter paths.
     */
    static void scatterUpsampleForceVector(double[] approx, double[] detail,
                                          double[] output, int length) {
        if (DOUBLE_LENGTH < 4) {
            scalarUpsample(approx, detail, output, length);
            return;
        }
        
        int halfLength = length / 2;

        // Process with scatter operations
        int i = 0;
        for (; i <= halfLength - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            // Load approximation and detail coefficients
            DoubleVector approxVec = DoubleVector.fromArray(DOUBLE_SPECIES, approx, i);
            DoubleVector detailVec = DoubleVector.fromArray(DOUBLE_SPECIES, detail, i);

            // Create index vectors for scatter
            int[] evenIndices = new int[DOUBLE_LENGTH];
            int[] oddIndices = new int[DOUBLE_LENGTH];
            for (int v = 0; v < DOUBLE_LENGTH; v++) {
                evenIndices[v] = 2 * (i + v);
                oddIndices[v] = 2 * (i + v) + 1;
            }

            // Scatter manually (scatter not available on all platforms)
            double[] approxArray = new double[DOUBLE_LENGTH];
            double[] detailArray = new double[DOUBLE_LENGTH];
            approxVec.intoArray(approxArray, 0);
            detailVec.intoArray(detailArray, 0);

            for (int j = 0; j < DOUBLE_LENGTH; j++) {
                output[evenIndices[j]] = approxArray[j];
                output[oddIndices[j]] = detailArray[j];
            }
        }

        // Handle remainder with scalar code
        for (; i < halfLength; i++) {
            output[2 * i] = approx[i];
            output[2 * i + 1] = detail[i];
        }
    }
    
    /**
     * Package-private method for testing vector batch gather.
     */
    static void batchGatherForceVector(double[][] signals, int[] indices,
                                      double[][] results, int count) {
        int numSignals = signals.length;

        // Process multiple signals simultaneously
        for (int s = 0; s < numSignals; s++) {
            double[] signal = signals[s];
            double[] result = results[s];

            int i = 0;
            for (; i <= count - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
                // Gather values manually
                double[] gathered = new double[DOUBLE_LENGTH];
                for (int j = 0; j < DOUBLE_LENGTH; j++) {
                    gathered[j] = signal[indices[i + j]];
                }
                DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, gathered, 0);

                // Store gathered values
                values.intoArray(result, i);
            }

            // Handle remainder
            for (; i < count; i++) {
                result[i] = signal[indices[i]];
            }
        }
    }
    
    /**
     * Package-private method for testing vector strided gather.
     */
    static double[] gatherStridedForceVector(double[] signal, int offset,
                                            int stride, int count) {
        double[] result = new double[count];
        
        if (stride > 8) {
            // Fallback for large strides
            for (int i = 0; i < count; i++) {
                result[i] = signal[offset + i * stride];
            }
            return result;
        }

        // Use gather for strided access
        int i = 0;
        for (; i <= count - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            // Create strided indices
            int[] indices = new int[DOUBLE_LENGTH];
            for (int v = 0; v < DOUBLE_LENGTH; v++) {
                indices[v] = offset + (i + v) * stride;
            }

            // Gather values manually
            double[] gathered = new double[DOUBLE_LENGTH];
            for (int j = 0; j < DOUBLE_LENGTH; j++) {
                gathered[j] = signal[indices[j]];
            }
            DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, gathered, 0);

            // Store result
            values.intoArray(result, i);
        }

        // Handle remainder
        for (; i < count; i++) {
            result[i] = signal[offset + i * stride];
        }

        return result;
    }
    
    /**
     * Package-private method for testing vector compressed gather.
     */
    static double[] gatherCompressedForceVector(double[] signal, boolean[] mask) {
        // Count true elements
        int count = 0;
        for (boolean b : mask) {
            if (b) count++;
        }

        double[] result = new double[count];
        int resultIdx = 0;

        // Process with vector mask
        int i = 0;
        for (; i <= mask.length - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            // Create mask vector
            boolean[] maskChunk = new boolean[DOUBLE_LENGTH];
            System.arraycopy(mask, i, maskChunk, 0, DOUBLE_LENGTH);
            VectorMask<Double> vectorMask = VectorMask.fromArray(DOUBLE_SPECIES, maskChunk, 0);

            // Load values under mask
            DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, signal, i, vectorMask);

            // Compress values under mask
            // DoubleVector compressed = values.compress(vectorMask); // Manual compress implementation for consistency
            int compressedLength = vectorMask.trueCount();
            
            // Only copy the valid compressed elements
            if (compressedLength > 0) {
                double[] temp = new double[DOUBLE_LENGTH];
                values.intoArray(temp, 0);  // Extract all values from vector
                
                // Manually compress by selecting only masked values
                int writeIdx = 0;
                for (int j = 0; j < DOUBLE_LENGTH && writeIdx < compressedLength; j++) {
                    if (maskChunk[j]) {
                        result[resultIdx + writeIdx] = temp[j];
                        writeIdx++;
                    }
                }
            }
            
            resultIdx += compressedLength;
        }

        // Handle remainder
        for (; i < mask.length; i++) {
            if (mask[i]) {
                result[resultIdx++] = signal[i];
            }
        }

        return result;
    }

    /**
     * Get information about gather/scatter support.
     */
    public static String getGatherScatterInfo() {
        return String.format(
                "Gather/Scatter Support: %s%nVector Length: %d doubles%nPlatform: %s",
                isGatherScatterEnabled() ? "Available" : "Not Available",
                DOUBLE_LENGTH,
                IS_LITTLE_ENDIAN ? "Little Endian" : "Big Endian"
        );
    }
}
