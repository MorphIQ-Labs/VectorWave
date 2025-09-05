package com.morphiqlabs.wavelet.extensions.internal;

import com.morphiqlabs.wavelet.util.PlatformDetector;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorSpecies;

/**
 * ARM/Apple Silicon optimized wavelet operations.
 *
 * <p>This class provides specialized implementations for ARM NEON (128-bit) vectors,
 * which have only 2 double-precision elements. These optimizations are crucial for
 * Apple M1/M2/M3 processors.</p>
 *
 * <p>Key optimizations:</p>
 * <ul>
 *   <li>Manually unrolled loops for 2-element vectors</li>
 *   <li>Reduced temporary allocations</li>
 *   <li>Direct array access patterns optimized for ARM</li>
 *   <li>Specialized kernels for common filter sizes</li>
 * </ul>
 */
final class VectorOpsARM {

    private static final VectorSpecies<Double> SPECIES = DoubleVector.SPECIES_128;
    private static final int VECTOR_LENGTH = 2; // ARM NEON has 128-bit vectors = 2 doubles

    private VectorOpsARM() {
        // Utility class
    }

    /**
     * Optimized Haar transform for ARM - the simplest and most common wavelet.
     */
    public static void haarTransformARM(double[] signal, double[] approx, double[] detail, int length) {
        // Haar coefficients
        final double SQRT2_INV = 1.0 / Math.sqrt(2.0);

        // Process pairs of elements
        for (int i = 0; i < length; i += 2) {
            double a = signal[i];
            double b = signal[i + 1];

            // Haar transform: sum and difference
            approx[i / 2] = (a + b) * SQRT2_INV;
            detail[i / 2] = (a - b) * SQRT2_INV;
        }
    }

    /**
     * Optimized DB2 transform for ARM.
     */
    public static double[] db2TransformARM(double[] signal, int signalLength, boolean lowPass) {
        // DB2 coefficients
        final double h0 = 0.48296291314453414;
        final double h1 = 0.83651630373780772;
        final double h2 = 0.22414386804201339;
        final double h3 = -0.12940952255126037;

        // Detail coefficients (QMF)
        final double g0 = h3;
        final double g1 = -h2;
        final double g2 = h1;
        final double g3 = -h0;

        double[] filter = lowPass ? new double[]{h0, h1, h2, h3} : new double[]{g0, g1, g2, g3};
        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        // Optimized for 2-element vectors
        DoubleVector filter01 = DoubleVector.fromArray(SPECIES, filter, 0);
        DoubleVector filter23 = DoubleVector.fromArray(SPECIES, filter, 2);

        // Process in pairs (utilizing 2-element vectors)
        for (int i = 0; i < outputLength - 1; i += 2) {
            // Load 6 signal values needed for 2 outputs
            int idx0 = (2 * i) % signalLength;
            int idx1 = (2 * i + 1) % signalLength;
            int idx2 = (2 * i + 2) % signalLength;
            int idx3 = (2 * i + 3) % signalLength;
            int idx4 = (2 * i + 4) % signalLength;
            int idx5 = (2 * i + 5) % signalLength;

            // First output: convolve signal[2i:2i+3] with filter
            DoubleVector sig01 = DoubleVector.zero(SPECIES)
                    .withLane(0, signal[idx0])
                    .withLane(1, signal[idx1]);
            DoubleVector sig23 = DoubleVector.zero(SPECIES)
                    .withLane(0, signal[idx2])
                    .withLane(1, signal[idx3]);

            double out0 = sig01.mul(filter01).reduceLanes(VectorOperators.ADD) +
                    sig23.mul(filter23).reduceLanes(VectorOperators.ADD);

            // Second output: convolve signal[2i+2:2i+5] with filter
            DoubleVector sig23_2 = DoubleVector.zero(SPECIES)
                    .withLane(0, signal[idx2])
                    .withLane(1, signal[idx3]);
            DoubleVector sig45 = DoubleVector.zero(SPECIES)
                    .withLane(0, signal[idx4])
                    .withLane(1, signal[idx5]);

            double out1 = sig23_2.mul(filter01).reduceLanes(VectorOperators.ADD) +
                    sig45.mul(filter23).reduceLanes(VectorOperators.ADD);

            output[i] = out0;
            output[i + 1] = out1;
        }

        // Handle last element if outputLength is odd
        if (outputLength % 2 == 1) {
            int i = outputLength - 1;
            double sum = 0.0;
            for (int k = 0; k < 4; k++) {
                int idx = (2 * i + k) % signalLength;
                sum += filter[k] * signal[idx];
            }
            output[i] = sum;
        }

        return output;
    }

    /**
     * Specialized convolution for 2-element vectors (ARM NEON).
     * This version minimizes temporary allocations and uses direct indexing.
     */
    public static double[] convolveAndDownsampleARM(
            double[] signal, double[] filter, int signalLength, int filterLength) {

        // Null checks
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }

        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        // For very small filters (Haar, DB2), use specialized implementations
        if (filterLength == 2) {
            return convolveAndDownsampleHaarARM(signal, filter, signalLength);
        } else if (filterLength == 4) {
            return convolveAndDownsampleDB2ARM(signal, filter, signalLength);
        }

        // General case for arbitrary filter lengths
        // Process in pairs to utilize 2-element vectors
        for (int i = 0; i < outputLength - 1; i += 2) {
            DoubleVector sum0 = DoubleVector.zero(SPECIES);
            DoubleVector sum1 = DoubleVector.zero(SPECIES);

            // Process filter coefficients in pairs
            for (int k = 0; k < filterLength - 1; k += 2) {
                // Indices for first output
                int idx00 = (2 * i + k) & (signalLength - 1);
                int idx01 = (2 * i + k + 1) & (signalLength - 1);

                // Indices for second output
                int idx10 = (2 * (i + 1) + k) & (signalLength - 1);
                int idx11 = (2 * (i + 1) + k + 1) & (signalLength - 1);

                // Load filter coefficients
                DoubleVector filterVec = DoubleVector.fromArray(SPECIES, filter, k);

                // Load and accumulate for both outputs
                DoubleVector sig0 = DoubleVector.zero(SPECIES)
                        .withLane(0, signal[idx00])
                        .withLane(1, signal[idx01]);
                DoubleVector sig1 = DoubleVector.zero(SPECIES)
                        .withLane(0, signal[idx10])
                        .withLane(1, signal[idx11]);

                sum0 = sum0.add(filterVec.mul(sig0));
                sum1 = sum1.add(filterVec.mul(sig1));
            }

            // Handle odd filter length
            if (filterLength % 2 == 1) {
                int k = filterLength - 1;
                double fk = filter[k];
                int idx0 = (2 * i + k) & (signalLength - 1);
                int idx1 = (2 * (i + 1) + k) & (signalLength - 1);

                output[i] = sum0.reduceLanes(VectorOperators.ADD) + fk * signal[idx0];
                output[i + 1] = sum1.reduceLanes(VectorOperators.ADD) + fk * signal[idx1];
            } else {
                output[i] = sum0.reduceLanes(VectorOperators.ADD);
                output[i + 1] = sum1.reduceLanes(VectorOperators.ADD);
            }
        }

        // Handle last element if outputLength is odd
        if (outputLength % 2 == 1) {
            int i = outputLength - 1;
            double sum = 0.0;
            for (int k = 0; k < filterLength; k++) {
                int idx = (2 * i + k) & (signalLength - 1);
                sum += filter[k] * signal[idx];
            }
            output[i] = sum;
        }

        return output;
    }

    /**
     * Specialized Haar convolution for ARM (2 coefficients).
     */
    private static double[] convolveAndDownsampleHaarARM(
            double[] signal, double[] filter, int signalLength) {

        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        double f0 = filter[0];
        double f1 = filter[1];

        // Unrolled by 4 for better performance
        int i = 0;
        for (; i < outputLength - 3; i += 4) {
            int idx0 = (2 * i) & (signalLength - 1);
            int idx1 = (2 * i + 1) & (signalLength - 1);
            int idx2 = (2 * (i + 1)) & (signalLength - 1);
            int idx3 = (2 * (i + 1) + 1) & (signalLength - 1);
            int idx4 = (2 * (i + 2)) & (signalLength - 1);
            int idx5 = (2 * (i + 2) + 1) & (signalLength - 1);
            int idx6 = (2 * (i + 3)) & (signalLength - 1);
            int idx7 = (2 * (i + 3) + 1) & (signalLength - 1);

            output[i] = f0 * signal[idx0] + f1 * signal[idx1];
            output[i + 1] = f0 * signal[idx2] + f1 * signal[idx3];
            output[i + 2] = f0 * signal[idx4] + f1 * signal[idx5];
            output[i + 3] = f0 * signal[idx6] + f1 * signal[idx7];
        }

        // Handle remainder
        for (; i < outputLength; i++) {
            int idx0 = (2 * i) & (signalLength - 1);
            int idx1 = (2 * i + 1) & (signalLength - 1);
            output[i] = f0 * signal[idx0] + f1 * signal[idx1];
        }

        return output;
    }

    /**
     * Specialized DB2 convolution for ARM (4 coefficients).
     */
    private static double[] convolveAndDownsampleDB2ARM(
            double[] signal, double[] filter, int signalLength) {

        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];

        double f0 = filter[0], f1 = filter[1], f2 = filter[2], f3 = filter[3];

        // Process in pairs for better vector utilization
        for (int i = 0; i < outputLength; i++) {
            int idx0 = (2 * i) & (signalLength - 1);
            int idx1 = (2 * i + 1) & (signalLength - 1);
            int idx2 = (2 * i + 2) & (signalLength - 1);
            int idx3 = (2 * i + 3) & (signalLength - 1);

            output[i] = f0 * signal[idx0] + f1 * signal[idx1] +
                    f2 * signal[idx2] + f3 * signal[idx3];
        }

        return output;
    }

    /**
     * Check if this is an ARM platform that can benefit from these optimizations.
     */
    public static boolean isARMPlatform() {
        return PlatformDetector.isARM();
    }

    /**
     * Check if running on Apple Silicon (M1/M2/M3).
     */
    public static boolean isAppleSilicon() {
        return PlatformDetector.isAppleSilicon();
    }

    /**
     * ARM-optimized upsampling and convolution for periodic boundary mode.
     * Inserts zeros between samples and convolves with filter.
     * 
     * @param signal Input signal (downsampled)
     * @param filter Wavelet filter coefficients
     * @param signalLength Length of input signal
     * @param filterLength Length of filter
     * @return Upsampled and convolved result
     */
    public static double[] upsampleAndConvolvePeriodicARM(
            double[] signal, double[] filter, int signalLength, int filterLength) {
        
        if (signal == null || filter == null) {
            throw new IllegalArgumentException("Signal and filter cannot be null");
        }
        
        int outputLength = signalLength * 2;
        double[] output = new double[outputLength];
        
        // Special case for Haar (2-tap filter)
        if (filterLength == 2) {
            return upsampleAndConvolveHaarARM(signal, filter, signalLength);
        }
        
        // Special case for DB2 (4-tap filter)
        if (filterLength == 4) {
            return upsampleAndConvolveDB2ARM(signal, filter, signalLength);
        }
        
        // General case - follows the standard upsampling algorithm:
        // For each input coefficient at position i, it contributes to output
        // starting at position 2*i, convolved with the filter
        
        // Process input coefficients in pairs for ARM 2-element vectors
        for (int i = 0; i < signalLength - 1; i += 2) {
            double coeff0 = signal[i];
            double coeff1 = signal[i + 1];
            
            int baseIndex0 = 2 * i;
            int baseIndex1 = 2 * (i + 1);
            
            // Process filter coefficients in pairs using 2-element vectors
            int j = 0;
            for (; j < filterLength - 1; j += 2) {
                // Load filter pair
                DoubleVector filterVec = DoubleVector.fromArray(SPECIES, filter, j);
                
                // Apply coeff0's contribution
                int idx0_0 = (baseIndex0 + j) % outputLength;
                int idx0_1 = (baseIndex0 + j + 1) % outputLength;
                output[idx0_0] += coeff0 * filter[j];
                output[idx0_1] += coeff0 * filter[j + 1];
                
                // Apply coeff1's contribution
                int idx1_0 = (baseIndex1 + j) % outputLength;
                int idx1_1 = (baseIndex1 + j + 1) % outputLength;
                output[idx1_0] += coeff1 * filter[j];
                output[idx1_1] += coeff1 * filter[j + 1];
            }
            
            // Handle remaining filter coefficient if filterLength is odd
            if (j < filterLength) {
                int idx0 = (baseIndex0 + j) % outputLength;
                int idx1 = (baseIndex1 + j) % outputLength;
                output[idx0] += coeff0 * filter[j];
                output[idx1] += coeff1 * filter[j];
            }
        }
        
        // Handle last coefficient if signalLength is odd
        if (signalLength % 2 == 1) {
            int i = signalLength - 1;
            double coeff = signal[i];
            int baseIndex = 2 * i;
            
            for (int j = 0; j < filterLength; j++) {
                int outputIndex = (baseIndex + j) % outputLength;
                output[outputIndex] += coeff * filter[j];
            }
        }
        
        return output;
    }

    /**
     * ARM-optimized upsampling and convolution for zero-padding boundary mode.
     * 
     * @param signal Input signal (downsampled)
     * @param filter Wavelet filter coefficients
     * @param signalLength Length of input signal
     * @param filterLength Length of filter
     * @return Upsampled and convolved result
     */
    public static double[] upsampleAndConvolveZeroPaddingARM(
            double[] signal, double[] filter, int signalLength, int filterLength) {
        
        if (signal == null || filter == null) {
            throw new IllegalArgumentException("Signal and filter cannot be null");
        }
        
        int outputLength = signalLength * 2;
        double[] output = new double[outputLength];
        
        // Process in pairs for ARM 2-element vectors
        for (int i = 0; i < outputLength - 1; i += 2) {
            double sum0 = 0.0;
            double sum1 = 0.0;
            
            // For even output index i
            for (int k = 0; k < filterLength; k++) {
                int idx = i - k;
                if (idx >= 0 && idx < outputLength && idx % 2 == 0) {
                    int srcIdx = idx / 2;
                    if (srcIdx < signalLength) {
                        sum0 += filter[k] * signal[srcIdx];
                    }
                }
            }
            
            // For odd output index i+1
            for (int k = 0; k < filterLength; k++) {
                int idx = i + 1 - k;
                if (idx >= 0 && idx < outputLength && idx % 2 == 0) {
                    int srcIdx = idx / 2;
                    if (srcIdx < signalLength) {
                        sum1 += filter[k] * signal[srcIdx];
                    }
                }
            }
            
            output[i] = sum0;
            output[i + 1] = sum1;
        }
        
        // Handle last element if outputLength is odd
        if (outputLength % 2 == 1) {
            int i = outputLength - 1;
            double sum = 0.0;
            for (int k = 0; k < filterLength; k++) {
                int idx = i - k;
                if (idx >= 0 && idx < outputLength && idx % 2 == 0) {
                    int srcIdx = idx / 2;
                    if (srcIdx < signalLength) {
                        sum += filter[k] * signal[srcIdx];
                    }
                }
            }
            output[i] = sum;
        }
        
        return output;
    }

    /**
     * Specialized Haar upsampling for ARM (2 coefficients).
     */
    private static double[] upsampleAndConvolveHaarARM(
            double[] signal, double[] filter, int signalLength) {
        
        int outputLength = signalLength * 2;
        double[] output = new double[outputLength];
        
        double f0 = filter[0];
        double f1 = filter[1];
        
        // Unrolled by 4 outputs (2 input samples) for better performance
        int i = 0;
        for (; i < signalLength - 1; i += 2) {
            double coeff0 = signal[i];
            double coeff1 = signal[i + 1];
            
            // For coefficient at position i, contribute to positions 2*i and 2*i+1
            int base0 = 2 * i;
            int base1 = 2 * (i + 1);
            
            // Apply Haar filter with periodic boundary
            output[base0 % outputLength] += coeff0 * f0;
            output[(base0 + 1) % outputLength] += coeff0 * f1;
            
            output[base1 % outputLength] += coeff1 * f0;
            output[(base1 + 1) % outputLength] += coeff1 * f1;
        }
        
        // Handle last element if signalLength is odd
        if (i < signalLength) {
            double coeff = signal[i];
            int base = 2 * i;
            output[base % outputLength] += coeff * f0;
            output[(base + 1) % outputLength] += coeff * f1;
        }
        
        return output;
    }

    /**
     * Specialized DB2 upsampling for ARM (4 coefficients).
     */
    private static double[] upsampleAndConvolveDB2ARM(
            double[] signal, double[] filter, int signalLength) {
        
        int outputLength = signalLength * 2;
        double[] output = new double[outputLength];
        
        double f0 = filter[0], f1 = filter[1], f2 = filter[2], f3 = filter[3];
        
        // Process each input coefficient
        // Each coefficient at position i contributes to output starting at position 2*i
        for (int i = 0; i < signalLength; i++) {
            double coeff = signal[i];
            int baseIndex = 2 * i;
            
            // Apply all 4 filter coefficients with periodic boundary
            output[(baseIndex) % outputLength] += coeff * f0;
            output[(baseIndex + 1) % outputLength] += coeff * f1;
            output[(baseIndex + 2) % outputLength] += coeff * f2;
            output[(baseIndex + 3) % outputLength] += coeff * f3;
        }
        
        return output;
    }
}
