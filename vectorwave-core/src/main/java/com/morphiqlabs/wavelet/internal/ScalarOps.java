package com.morphiqlabs.wavelet.internal;

import com.morphiqlabs.wavelet.util.MathUtils;
import com.morphiqlabs.wavelet.fft.CoreFFT;
import com.morphiqlabs.wavelet.util.ValidationUtils;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.ErrorCode;
import com.morphiqlabs.wavelet.exception.ErrorContext;
import static java.util.Arrays.fill;

/**
 * Scalar implementation of the core wavelet transform operations.
 * <p>
 * This implementation includes optimizations for small signal sizes:
 * - Bitwise operations for power-of-2 modulo arithmetic
 * - Specialized implementations for small filter sizes (Haar, DB2)
 * - Cache-friendly memory access patterns
 * <p>
 * The optimizations are automatically applied based on signal and filter characteristics.
 * <p>
 * <strong>Note on zero coefficient handling:</strong>
 * Throughout this class, exact zero coefficients (coeff == 0.0) are skipped for performance.
 * This is intentional as wavelet transforms preserve exact zeros from sparse or
 * zero-padded signals. Very small coefficients are NOT treated as zero to maintain
 * numerical precision in reconstruction.
 */
public final class ScalarOps {

    private static final int SMALL_SIGNAL_THRESHOLD = 1024;
    
    /**
     * Maximum safe bit shift amount to prevent integer overflow.
     * When performing bit shift operations like (1 << n), n must be <= 30
     * to avoid overflow in 32-bit integers.
     */
    private static final int MAX_SAFE_SHIFT_BITS = 31;
    
    // Vectorization threshold constants
    private static final int MIN_VECTORIZATION_LENGTH = 32;
    private static final int MEDIUM_ARRAY_THRESHOLD = 128;
    private static final int MAX_FILTER_LENGTH_FOR_MEDIUM_ARRAYS = 8;
    
    /**
     * Flag indicating whether Vector API is available and enabled.
     * Detected at class loading time for optimal performance.
     */
    private static final boolean VECTORIZATION_ENABLED;
    
    private static final System.Logger LOG = com.morphiqlabs.wavelet.util.Logging.getLogger(ScalarOps.class);
    
    static {
        // Core module uses pure Java 21 - no Vector API dependencies
        VECTORIZATION_ENABLED = false;
        LOG.log(System.Logger.Level.DEBUG, "Core module: Vector API disabled (pure Java 21 implementation)");
    }

// (Removed the internal comment as it has been moved to the class-level Javadoc)

    /**
     * Performs convolution followed by downsampling by 2 with periodic boundary handling.
     * This is the core operation of the Fast Wavelet Transform.
     *
     * @param signal The input signal.
     * @param filter The filter (wavelet coefficients) to apply.
     * @param output The array to store the results.
     */
    public static void convolveAndDownsamplePeriodic(double[] signal, double[] filter, double[] output) {
        convolveAndDownsamplePeriodic(signal, 0, signal.length, filter, output);
    }
    
    /**
     * Performs convolution followed by downsampling by 2 with periodic boundary handling on a slice.
     * This zero-copy version enables streaming processing without array copying.
     *
     * @param signal The input signal array.
     * @param offset The starting index in the signal array.
     * @param length The number of elements to process.
     * @param filter The filter (wavelet coefficients) to apply.
     * @param output The array to store the results.
     * @throws IllegalArgumentException if signal, filter, or output is null
     * @throws IndexOutOfBoundsException if offset or length are invalid
     */
    public static void convolveAndDownsamplePeriodic(double[] signal, int offset, int length, 
                                                     double[] filter, double[] output) {
        // Validate parameters
        if (signal == null) {
            throw new InvalidArgumentException(
                ErrorCode.VAL_NULL_ARGUMENT,
                ErrorContext.builder("Signal array cannot be null")
                    .withContext("Operation", "convolveAndDownsamplePeriodic")
                    .withContext("Parameter", "signal")
                    .withSuggestion("Ensure signal array is initialized before calling this method")
                    .build()
            );
        }
        if (filter == null) {
            throw new InvalidArgumentException(
                ErrorCode.VAL_NULL_ARGUMENT,
                ErrorContext.builder("Filter array cannot be null")
                    .withContext("Operation", "convolveAndDownsamplePeriodic")
                    .withContext("Parameter", "filter")
                    .withSuggestion("Ensure wavelet filter is properly initialized")
                    .build()
            );
        }
        if (output == null) {
            throw new InvalidArgumentException(
                ErrorCode.VAL_NULL_ARGUMENT,
                ErrorContext.builder("Output array cannot be null")
                    .withContext("Operation", "convolveAndDownsamplePeriodic")
                    .withContext("Parameter", "output")
                    .withSuggestion("Pre-allocate output array with length = signal.length / 2")
                    .build()
            );
        }
        if (offset < 0 || length < 0 || offset + length > signal.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length: offset=" + offset + 
                ", length=" + length + ", array length=" + signal.length);
        }
        if (output.length != length / 2) {
            throw new InvalidArgumentException(
                ErrorCode.VAL_LENGTH_MISMATCH,
                ErrorContext.builder("Output array has incorrect length for downsampling operation")
                    .withContext("Operation", "convolveAndDownsamplePeriodic")
                    .withArrayDimensions("length / 2 = " + (length / 2), "output.length = " + output.length)
                    .withContext("Signal length", length)
                    .withContext("Filter length", filter.length)
                    .withSuggestion("Allocate output array with exactly " + (length / 2) + " elements")
                    .withSuggestion("For MODWT (no downsampling), use circularConvolveMODWT instead")
                    .build()
            );
        }
        
        int filterLen = filter.length;

        // Use specialized implementations for small filters and signals
        if (length <= SMALL_SIGNAL_THRESHOLD && ValidationUtils.isPowerOfTwo(length)) {
            if (filterLen == 2) {
                convolveAndDownsamplePeriodicHaar(signal, offset, length, filter, output);
                return;
            } else if (filterLen == 4) {
                convolveAndDownsamplePeriodicDB2(signal, offset, length, filter, output);
                return;
            }
            // For other filter sizes, use optimized version with bitwise modulo
            convolveAndDownsamplePeriodicOptimized(signal, offset, length, filter, output);
            return;
        }

        // Standard implementation for large signals or non-power-of-2
        for (int i = 0; i < output.length; i++) {
            double sum = 0.0;

            for (int j = 0; j < filterLen; j++) {
                // Calculate index relative to the slice, wrap within [0, length), then add offset
                int localIndex = (2 * i + j) % length;
                sum += signal[offset + localIndex] * filter[j];
            }

            output[i] = sum;
        }
    }

    /**
     * Performs convolution followed by downsampling by 2 with zero padding.
     *
     * @param signal The input signal.
     * @param filter The filter (wavelet coefficients) to apply.
     * @param output The array to store the results.
     */
    public static void convolveAndDownsampleDirect(double[] signal, double[] filter, double[] output) {
        convolveAndDownsampleDirect(signal, 0, signal.length, filter, output);
    }
    
    /**
     * Performs convolution followed by downsampling by 2 with zero padding on a slice.
     * This zero-copy version enables streaming processing without array copying.
     *
     * @param signal The input signal array.
     * @param offset The starting index in the signal array.
     * @param length The number of elements to process.
     * @param filter The filter (wavelet coefficients) to apply.
     * @param output The array to store the results.
     * @throws IllegalArgumentException if signal, filter, or output is null
     * @throws IndexOutOfBoundsException if offset or length are invalid
     */
    public static void convolveAndDownsampleDirect(double[] signal, int offset, int length,
                                                   double[] filter, double[] output) {
        // Validate parameters
        if (signal == null) {
            throw new InvalidArgumentException(
                ErrorCode.VAL_NULL_ARGUMENT,
                ErrorContext.builder("Signal array cannot be null")
                    .withContext("Operation", "convolveAndDownsamplePeriodic")
                    .withContext("Parameter", "signal")
                    .withSuggestion("Ensure signal array is initialized before calling this method")
                    .build()
            );
        }
        if (filter == null) {
            throw new InvalidArgumentException(
                ErrorCode.VAL_NULL_ARGUMENT,
                ErrorContext.builder("Filter array cannot be null")
                    .withContext("Operation", "convolveAndDownsamplePeriodic")
                    .withContext("Parameter", "filter")
                    .withSuggestion("Ensure wavelet filter is properly initialized")
                    .build()
            );
        }
        if (output == null) {
            throw new InvalidArgumentException(
                ErrorCode.VAL_NULL_ARGUMENT,
                ErrorContext.builder("Output array cannot be null")
                    .withContext("Operation", "convolveAndDownsamplePeriodic")
                    .withContext("Parameter", "output")
                    .withSuggestion("Pre-allocate output array with length = signal.length / 2")
                    .build()
            );
        }
        if (offset < 0 || length < 0 || offset + length > signal.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length: offset=" + offset + 
                ", length=" + length + ", array length=" + signal.length);
        }
        if (output.length != length / 2) {
            throw new InvalidArgumentException(
                ErrorCode.VAL_LENGTH_MISMATCH,
                ErrorContext.builder("Output array has incorrect length for downsampling operation")
                    .withContext("Operation", "convolveAndDownsamplePeriodic")
                    .withArrayDimensions("length / 2 = " + (length / 2), "output.length = " + output.length)
                    .withContext("Signal length", length)
                    .withContext("Filter length", filter.length)
                    .withSuggestion("Allocate output array with exactly " + (length / 2) + " elements")
                    .withSuggestion("For MODWT (no downsampling), use circularConvolveMODWT instead")
                    .build()
            );
        }
        
        int filterLen = filter.length;

        for (int i = 0; i < output.length; i++) {
            double sum = 0.0;
            int kStart = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int localIndex = kStart + j;
                if (localIndex < length) {
                    sum += signal[offset + localIndex] * filter[j];
                }
            }

            output[i] = sum;
        }
    }

    /**
     * Performs upsampling by 2 followed by convolution with periodic boundary handling.
     * This is the core operation of the inverse Fast Wavelet Transform.
     *
     * @param coeffs The coefficients to upsample and convolve.
     * @param filter The reconstruction filter.
     * @param output The output array.
     */
    public static void upsampleAndConvolvePeriodic(double[] coeffs, double[] filter, double[] output) {
        int outputLen = output.length;
        int filterLen = filter.length;

        // Use optimized version for small power-of-2 signals
        if (outputLen <= SMALL_SIGNAL_THRESHOLD && ValidationUtils.isPowerOfTwo(outputLen)) {
            if (filterLen == 2) {
                upsampleAndConvolvePeriodicHaar(coeffs, filter, output);
                return;
            }
            // For other filter sizes, use optimized version with bitwise modulo
            upsampleAndConvolvePeriodicOptimized(coeffs, filter, output);
            return;
        }

        // Standard implementation
        // Clear output array
        fill(output, 0.0);

        // Upsample and convolve
        for (int i = 0; i < coeffs.length; i++) {
            double coeff = coeffs[i];
            if (coeff == 0.0) continue; // Skip zero coefficients (see class-level note)

            int baseIndex = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int outputIndex = (baseIndex + j) % outputLen;
                output[outputIndex] += coeff * filter[j];
            }
        }
    }

    /**
     * Performs upsampling by 2 followed by convolution with zero padding.
     *
     * @param coeffs The coefficients to upsample and convolve.
     * @param filter The reconstruction filter.
     * @param output The output array.
     */
    public static void upsampleAndConvolveDirect(double[] coeffs, double[] filter, double[] output) {
        int outputLen = output.length;
        int filterLen = filter.length;

        // Clear output array
        for (int i = 0; i < outputLen; i++) {
            output[i] = 0.0;
        }

        // Upsample and convolve
        for (int i = 0; i < coeffs.length; i++) {
            double coeff = coeffs[i];
            if (coeff == 0.0) continue; // Skip zero coefficients (see class-level note)

            int baseIndex = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int outputIndex = baseIndex + j;
                if (outputIndex < outputLen) {
                    output[outputIndex] += coeff * filter[j];
                }
            }
        }
    }

    // ========== Optimized implementations for small signals ==========


    /**
     * Optimized convolution for power-of-2 signal lengths using bitwise modulo.
     */
    private static void convolveAndDownsamplePeriodicOptimized(double[] signal, double[] filter, double[] output) {
        convolveAndDownsamplePeriodicOptimized(signal, 0, signal.length, filter, output);
    }
    
    /**
     * Optimized convolution for power-of-2 signal lengths using bitwise modulo on a slice.
     */
    private static void convolveAndDownsamplePeriodicOptimized(double[] signal, int offset, int length,
                                                               double[] filter, double[] output) {
        int filterLen = filter.length;
        int lengthMask = length - 1; // For bitwise modulo

        for (int i = 0; i < output.length; i++) {
            double sum = 0.0;
            int kStart = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int localIndex = (kStart + j) & lengthMask;
                sum += signal[offset + localIndex] * filter[j];
            }

            output[i] = sum;
        }
    }

    /**
     * Specialized implementation for Haar wavelet (2 coefficients).
     */
    private static void convolveAndDownsamplePeriodicHaar(double[] signal, double[] filter, double[] output) {
        convolveAndDownsamplePeriodicHaar(signal, 0, signal.length, filter, output);
    }
    
    /**
     * Specialized implementation for Haar wavelet (2 coefficients) on a slice.
     */
    private static void convolveAndDownsamplePeriodicHaar(double[] signal, int offset, int length, 
                                                          double[] filter, double[] output) {
        int lengthMask = length - 1;
        double f0 = filter[0];
        double f1 = filter[1];

        for (int i = 0; i < output.length; i++) {
            int idx0 = (2 * i) & lengthMask;
            int idx1 = (2 * i + 1) & lengthMask;
            output[i] = signal[offset + idx0] * f0 + signal[offset + idx1] * f1;
        }
    }

    /**
     * Specialized implementation for DB2 wavelet (4 coefficients).
     */
    private static void convolveAndDownsamplePeriodicDB2(double[] signal, double[] filter, double[] output) {
        convolveAndDownsamplePeriodicDB2(signal, 0, signal.length, filter, output);
    }
    
    /**
     * Specialized implementation for DB2 wavelet (4 coefficients) on a slice.
     */
    private static void convolveAndDownsamplePeriodicDB2(double[] signal, int offset, int length,
                                                         double[] filter, double[] output) {
        int lengthMask = length - 1;
        double f0 = filter[0];
        double f1 = filter[1];
        double f2 = filter[2];
        double f3 = filter[3];

        for (int i = 0; i < output.length; i++) {
            int base = 2 * i;
            int idx0 = base & lengthMask;
            int idx1 = (base + 1) & lengthMask;
            int idx2 = (base + 2) & lengthMask;
            int idx3 = (base + 3) & lengthMask;

            output[i] = signal[offset + idx0] * f0 + signal[offset + idx1] * f1 +
                    signal[offset + idx2] * f2 + signal[offset + idx3] * f3;
        }
    }

    /**
     * Optimized upsampling for power-of-2 output lengths.
     */
    private static void upsampleAndConvolvePeriodicOptimized(double[] coeffs, double[] filter, double[] output) {
        int outputLen = output.length;
        int filterLen = filter.length;
        int outputMask = outputLen - 1; // For bitwise modulo

        // Clear output array
        fill(output, 0.0);

        // Upsample and convolve
        for (int i = 0; i < coeffs.length; i++) {
            double coeff = coeffs[i];
            if (coeff == 0.0) continue; // Skip zero coefficients (see class-level note)

            int baseIndex = 2 * i;

            for (int j = 0; j < filterLen; j++) {
                int outputIndex = (baseIndex + j) & outputMask;
                output[outputIndex] += coeff * filter[j];
            }
        }
    }

    /**
     * Specialized upsampling for Haar wavelet.
     */
    private static void upsampleAndConvolvePeriodicHaar(double[] coeffs, double[] filter, double[] output) {
        int outputMask = output.length - 1;
        double f0 = filter[0];
        double f1 = filter[1];

        // Clear output array
        fill(output, 0.0);

        for (int i = 0; i < coeffs.length; i++) {
            double coeff = coeffs[i];
            if (coeff == 0.0) continue; // Skip zero coefficients (see class-level note)

            int base = 2 * i;
            int idx0 = base & outputMask;
            int idx1 = (base + 1) & outputMask;

            output[idx0] += coeff * f0;
            output[idx1] += coeff * f1;
        }
    }

    /**
     * Cache-friendly combined transform for small signals.
     * Processes both low-pass and high-pass filters in a single pass.
     *
     * @param signal       The input signal (must be power-of-2 length).
     * @param lowFilter    The low-pass filter.
     * @param highFilter   The high-pass filter.
     * @param approxCoeffs Output array for approximation coefficients.
     * @param detailCoeffs Output array for detail coefficients.
     */
    public static void combinedTransformPeriodic(double[] signal, double[] lowFilter,
                                                 double[] highFilter, double[] approxCoeffs,
                                                 double[] detailCoeffs) {
        combinedTransformPeriodic(signal, 0, signal.length, lowFilter, highFilter, approxCoeffs, detailCoeffs);
    }
    
    /**
     * Cache-friendly combined transform for small signals on a slice.
     * Processes both low-pass and high-pass filters in a single pass for zero-copy streaming.
     *
     * @param signal       The input signal array.
     * @param offset       The starting index in the signal array.
     * @param length       The number of elements to process (must be power-of-2).
     * @param lowFilter    The low-pass filter.
     * @param highFilter   The high-pass filter.
     * @param approxCoeffs Output array for approximation coefficients.
     * @param detailCoeffs Output array for detail coefficients.
     * @throws IllegalArgumentException if any array is null or output arrays have wrong length
     * @throws IndexOutOfBoundsException if offset or length are invalid
     */
    public static void combinedTransformPeriodic(double[] signal, int offset, int length,
                                                 double[] lowFilter, double[] highFilter,
                                                 double[] approxCoeffs, double[] detailCoeffs) {
        // Validate parameters
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (lowFilter == null) {
            throw new IllegalArgumentException("Low filter cannot be null");
        }
        if (highFilter == null) {
            throw new IllegalArgumentException("High filter cannot be null");
        }
        if (approxCoeffs == null) {
            throw new IllegalArgumentException("Approximation coefficients array cannot be null");
        }
        if (detailCoeffs == null) {
            throw new IllegalArgumentException("Detail coefficients array cannot be null");
        }
        if (offset < 0 || length < 0 || offset + length > signal.length) {
            throw new IndexOutOfBoundsException("Invalid offset or length: offset=" + offset + 
                ", length=" + length + ", array length=" + signal.length);
        }
        if (approxCoeffs.length != length / 2) {
            throw new IllegalArgumentException("Approximation coefficients array must have length " + 
                (length / 2) + ", but has length " + approxCoeffs.length);
        }
        if (detailCoeffs.length != length / 2) {
            throw new IllegalArgumentException("Detail coefficients array must have length " + 
                (length / 2) + ", but has length " + detailCoeffs.length);
        }
        // Note: For biorthogonal wavelets, low and high filters may have different lengths
        int lowFilterLen = lowFilter.length;
        int highFilterLen = highFilter.length;

        if (length <= SMALL_SIGNAL_THRESHOLD && ValidationUtils.isPowerOfTwo(length)) {
            int lengthMask = length - 1;

            for (int i = 0; i < approxCoeffs.length; i++) {
                double sumLow = 0.0;
                double sumHigh = 0.0;
                int kStart = 2 * i;

                // Process low-pass filter
                for (int j = 0; j < lowFilterLen; j++) {
                    int localIndex = (kStart + j) & lengthMask;
                    double signalValue = signal[offset + localIndex];
                    sumLow += signalValue * lowFilter[j];
                }

                // Process high-pass filter
                for (int j = 0; j < highFilterLen; j++) {
                    int localIndex = (kStart + j) & lengthMask;
                    double signalValue = signal[offset + localIndex];
                    sumHigh += signalValue * highFilter[j];
                }

                approxCoeffs[i] = sumLow;
                detailCoeffs[i] = sumHigh;
            }
        } else {
            // Fall back to separate transforms for large signals
            convolveAndDownsamplePeriodic(signal, offset, length, lowFilter, approxCoeffs);
            convolveAndDownsamplePeriodic(signal, offset, length, highFilter, detailCoeffs);
        }
    }

    // ========== Wrapper methods for WaveletOpsFactory compatibility ==========

    /**
     * Wrapper method for periodic convolution and downsampling that returns the result.
     * Used by WaveletOpsFactory.
     */
    public static double[] convolveAndDownsamplePeriodic(double[] signal, double[] filter,
                                                         int signalLength, int filterLength) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        double[] output = new double[signalLength / 2];
        convolveAndDownsamplePeriodic(signal, filter, output);
        return output;
    }

    /**
     * Wrapper method for zero-padding convolution and downsampling that returns the result.
     * Used by WaveletOpsFactory.
     */
    public static double[] convolveAndDownsampleZeroPadding(double[] signal, double[] filter,
                                                            int signalLength, int filterLength) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        double[] output = new double[signalLength / 2];
        convolveAndDownsampleDirect(signal, filter, output);
        return output;
    }

    /**
     * Wrapper method for periodic upsampling and convolution that returns the result.
     * Used by WaveletOpsFactory.
     */
    public static double[] upsampleAndConvolvePeriodic(double[] coeffs, double[] filter,
                                                       int coeffsLength, int filterLength) {
        if (coeffs == null) {
            throw new IllegalArgumentException("Coefficients cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        double[] output = new double[coeffsLength * 2];
        upsampleAndConvolvePeriodic(coeffs, filter, output);
        return output;
    }

    /**
     * Wrapper method for zero-padding upsampling and convolution that returns the result.
     * Used by WaveletOpsFactory.
     */
    public static double[] upsampleAndConvolveZeroPadding(double[] coeffs, double[] filter,
                                                          int coeffsLength, int filterLength) {
        if (coeffs == null) {
            throw new IllegalArgumentException("Coefficients cannot be null");
        }
        if (filter == null) {
            throw new IllegalArgumentException("Filter cannot be null");
        }
        double[] output = new double[coeffsLength * 2];
        upsampleAndConvolveDirect(coeffs, filter, output);
        return output;
    }
    
    // ==========================================
    // MODWT-specific Operations
    // ==========================================
    
    /**
     * Performs circular convolution for MODWT (Maximal Overlap Discrete Wavelet Transform).
     * This is different from standard DWT convolution as it doesn't downsample.
     * 
     * @param signal The input signal of length N.
     * @param filter The filter coefficients of length L.
     * @param output The output array of length N (same as input).
     */
    public static void circularConvolveMODWT(double[] signal, double[] filter, double[] output) {
        // Core module always uses scalar implementation
        circularConvolveMODWTScalar(signal, filter, output);
    }

    /**
     * FFT-based circular convolution for MODWT (periodic) path.
     * Uses zero-padding to next power-of-two and complex FFT multiplication.
     * Output length equals signal length (first N samples of circular convolution).
     */
    public static void circularConvolveMODWTFFT(double[] signal, double[] filter, double[] output) {
        int n = signal.length;
        int m = nextPow2(n);
        double[] xr = new double[m];
        double[] xi = new double[m];
        double[] hr = new double[m];
        double[] hi = new double[m];
        System.arraycopy(signal, 0, xr, 0, n);
        System.arraycopy(filter, 0, hr, 0, Math.min(filter.length, m));
        // FFT X and H using unified CoreFFT
        CoreFFT.fft(xr, xi);
        CoreFFT.fft(hr, hi);
        // Multiply in frequency domain: Y = X * H
        for (int k = 0; k < m; k++) {
            double r = xr[k] * hr[k] - xi[k] * hi[k];
            double im = xr[k] * hi[k] + xi[k] * hr[k];
            xr[k] = r;
            xi[k] = im;
        }
        // IFFT (CoreFFT performs 1/m scaling internally)
        CoreFFT.ifft(xr, xi);
        // Copy first N samples
        for (int i = 0; i < n; i++) {
            output[i] = xr[i];
        }
    }

    private static int nextPow2(int n) {
        int p = 1;
        while (p < n) p <<= 1;
        return p;
    }

    // Removed duplicated FFT implementation in favor of CoreFFT
    
    /**
     * Scalar implementation of circular convolution for MODWT.
     * 
     * Mathematical justification for (t - l) indexing:
     * In standard wavelet literature, the MODWT uses time-reversed filters where:
     * W_j,t = Σ_{l=0}^{L-1} h_j,l * X_{t-l mod N}
     * 
     * This differs from standard convolution (t + l) because:
     * 1. Wavelet filters are defined as h_l = h(-l) in continuous time
     * 2. The MODWT preserves the time-ordering of coefficients
     * 3. Using (t - l) ensures causality: coefficient at time t depends on past values
     * 
     * Reference: Percival {@literal &} Walden (2000), "Wavelet Methods for Time Series Analysis", 
     * Chapter 5, equation 5.4
     */
    private static void circularConvolveMODWTScalar(double[] signal, double[] filter, double[] output) {
        int signalLen = signal.length;
        int filterLen = filter.length;
        
        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;
            
            for (int l = 0; l < filterLen; l++) {
                // MODWT convolution form: X_{(t-l) mod N}
                int idx = t - l;
                int signalIndex;
                if (idx >= 0 && idx < signalLen) {
                    signalIndex = idx;
                } else if (idx < 0 && idx >= -signalLen) {
                    signalIndex = idx + signalLen;
                } else {
                    signalIndex = ((idx % signalLen) + signalLen) % signalLen;
                }
                sum += signal[signalIndex] * filter[l];
            }
            
            output[t] = sum;
        }
    }

    /**
     * Performs circular convolution with level-based shift for multi-level MODWT.
     * 
     * Mathematical foundation:
     * At decomposition level j, the MODWT uses upsampled filters where zeros are inserted
     * between filter coefficients. The convolution formula becomes:
     * W_j,t = Σ_{l=0}^{L-1} h_j,l * X_{t - 2^(j-1) * l mod N}
     * 
     * The factor 2^(j-1) represents the upsampling factor at level j, effectively
     * spreading the filter coefficients across a wider time range. This allows the
     * MODWT to capture features at different time scales.
     * 
     * @param signal    The input signal of length N.
     * @param filter    The filter coefficients (not upsampled).
     * @param output    The output array of length N.
     * @param level     The decomposition level (1-based, where level 1 = j=1).
     */
    public static void circularConvolveMODWTLevel(double[] signal, double[] filter, double[] output, int level) {
        int signalLen = signal.length;
        int filterLen = filter.length;
        
        // Check for potential overflow before bit shift
        if (level - 1 >= MAX_SAFE_SHIFT_BITS) {
            throw new InvalidArgumentException(
                ErrorCode.VAL_TOO_LARGE,
                ErrorContext.builder("Decomposition level would cause integer overflow")
                    .withContext("Operation", "circularConvolveMODWTLevel")
                    .withLevelInfo(level, MAX_SAFE_SHIFT_BITS)
                    .withContext("Shift calculation", "2^(" + (level-1) + ")")
                    .withSuggestion("Maximum safe level is " + MAX_SAFE_SHIFT_BITS)
                    .withSuggestion("For extreme decomposition levels, consider using alternative algorithms")
                    .build()
            );
        }
        
        int shift = 1 << (level - 1); // 2^(j-1) where j = level
        
        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;
            
            for (int l = 0; l < filterLen; l++) {
                int idx = t - shift * l;
                int signalIndex;
                if (idx >= 0 && idx < signalLen) {
                    signalIndex = idx;
                } else if (idx < 0 && idx >= -signalLen) {
                    signalIndex = idx + signalLen;
                } else {
                    signalIndex = ((idx % signalLen) + signalLen) % signalLen;
                }
                sum += signal[signalIndex] * filter[l];
            }
            
            output[t] = sum;
        }
    }

    /**
     * Performs zero-padding convolution for MODWT (without downsampling).
     * This treats values outside the signal boundaries as zeros.
     * 
     * @param signal The input signal of length N.
     * @param filter The filter coefficients of length L.
     * @param output The output array of length N (same as input).
     */
    public static void zeroPaddingConvolveMODWT(double[] signal, double[] filter, double[] output) {
        int signalLen = signal.length;
        int filterLen = filter.length;
        
        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;
            
            for (int l = 0; l < filterLen; l++) {
                // Zero-padding convolution indexing: X_{t - l}
                int signalIndex = t - l;
                if (signalIndex >= 0 && signalIndex < signalLen) {
                    sum += signal[signalIndex] * filter[l];
                }
                // else: signal value is zero, no contribution to sum
            }

            output[t] = sum;
        }
    }

    /**
     * Performs symmetric-extension convolution for MODWT (without downsampling).
     * This mirrors the signal at the boundaries to reduce edge artifacts.
     *
     * @param signal The input signal of length N.
     * @param filter The filter coefficients of length L.
     * @param output The output array of length N (same as input).
     */
    public static void symmetricConvolveMODWT(double[] signal, double[] filter, double[] output) {
        int signalLen = signal.length;
        int filterLen = filter.length;

        for (int t = 0; t < signalLen; t++) {
            double sum = 0.0;

            for (int l = 0; l < filterLen; l++) {
                int idx = t - l;
                // Apply symmetric boundary extension using utility method
                idx = MathUtils.symmetricBoundaryExtension(idx, signalLen);
                
                sum += signal[idx] * filter[l];
            }

            output[t] = sum;
        }
    }

    /**
     * Scales wavelet filter coefficients for MODWT at a specific level.
     * MODWT uses scaled filters: h_j,l = h_l / 2^(j/2) for level j.
     *
     * @param originalFilter The original filter coefficients.
     * @param level The decomposition level (1-based).
     * @return The scaled filter coefficients.
     */
    public static double[] scaleFilterForMODWT(double[] originalFilter, int level) {
        double scaleFactor = 1.0 / Math.sqrt(Math.pow(2.0, level));
        double[] scaledFilter = new double[originalFilter.length];
        
        for (int i = 0; i < originalFilter.length; i++) {
            scaledFilter[i] = originalFilter[i] * scaleFactor;
        }
        
        return scaledFilter;
    }

    /**
     * Generic upsample-by-spacing helper (insert zeros) without scaling.
     * Produces length (L0-1)*spacing + 1 with original taps at multiples of spacing.
     *
     * @param baseFilter Original filter taps (length L0)
     * @param spacing Spacing factor (>= 1)
     * @return Upsampled filter with zeros inserted
     */
    public static double[] upsampleInsertZeros(double[] baseFilter, int spacing) {
        if (spacing <= 1) {
            return baseFilter.clone();
        }
        int L = (baseFilter.length - 1) * spacing + 1;
        double[] out = new double[L];
        for (int i = 0; i < baseFilter.length; i++) out[i * spacing] = baseFilter[i];
        return out;
    }

    /**
     * Upsamples (by inserting 2^(j-1)-1 zeros) and scales a filter for MODWT analysis at level j.
     * Scaling uses the textbook MODWT convention: 2^(-j/2).
     *
     * @param baseFilter The original (level-1) filter
     * @param level The decomposition level (1-based)
     * @return The upsampled and scaled filter for analysis
     */
    public static double[] upsampleAndScaleForMODWTAnalysis(double[] baseFilter, int level) {
        if (level <= 1) {
            return scaleFilterForMODWT(baseFilter, 1);
        }
        if (level - 1 >= MAX_SAFE_SHIFT_BITS) {
            throw new InvalidArgumentException(
                ErrorCode.VAL_TOO_LARGE,
                ErrorContext.builder("Decomposition level would cause integer overflow")
                    .withContext("Operation", "upsampleAndScaleForMODWTAnalysis")
                    .withLevelInfo(level, MAX_SAFE_SHIFT_BITS)
                    .withSuggestion("Reduce decomposition level")
                    .build()
            );
        }
        int up = 1 << (level - 1);
        double scale = 1.0 / Math.sqrt(Math.pow(2.0, level));
        int L = (baseFilter.length - 1) * up + 1;
        double[] out = new double[L];
        for (int i = 0; i < baseFilter.length; i++) out[i * up] = baseFilter[i] * scale;
        return out;
    }

    /**
     * Upsamples (by inserting 2^(j-1)-1 zeros) and scales a filter for IMODWT synthesis at level j.
     * Cascade synthesis uses level-independent 1/√2 scaling per stage, which inverts
     * the multi-level analysis when combined across levels.
     */
    public static double[] upsampleAndScaleForIMODWTSynthesis(double[] baseFilter, int level) {
        int up = (level <= 1) ? 1 : (1 << (level - 1));
        double scale = 1.0 / Math.sqrt(2.0);
        int L = (baseFilter.length - 1) * up + 1;
        double[] out = new double[L];
        for (int i = 0; i < baseFilter.length; i++) out[i * up] = baseFilter[i] * scale;
        return out;
    }
    
    // ==========================================
    // Java 21 Performance Optimizations
    // ==========================================
    
    /**
     * Determines if vectorization should be used based on array characteristics.
     * Uses efficient decision making optimized for Java 21.
     * 
     * @param signalLength The length of the signal array
     * @param filterLength The length of the filter array
     * @return true if vectorization is beneficial
     */
    private static boolean shouldUseVectorization(int signalLength, int filterLength) {
        // Too small for vectorization overhead
        if (signalLength < MIN_VECTORIZATION_LENGTH) {
            return false;
        }
        
        // Large arrays always benefit from vectorization
        if (signalLength >= MEDIUM_ARRAY_THRESHOLD) {
            return true;
        }
        
        // Medium arrays: vectorize only with small filters
        return filterLength <= MAX_FILTER_LENGTH_FOR_MEDIUM_ARRAYS;
    }
    
    /**
     * Efficiently clears an array using the best available method.
     * Chooses between vectorized and scalar clearing based on array size.
     * 
     * @param array The array to clear
     */
    private static void clearArray(double[] array) {
        // Core module uses traditional array clearing
        for (int i = 0; i < array.length; i++) {
            array[i] = 0.0;
        }
    }
    
    /**
     * Gets performance characteristics for the current system configuration.
     * Useful for monitoring and optimization decisions.
     * 
     * @return Performance information record
     */
    public static PerformanceInfo getPerformanceInfo() {
        return new PerformanceInfo(
            VECTORIZATION_ENABLED,
            null, // Core module has no vector capabilities
            Runtime.getRuntime().availableProcessors()
        );
    }
    
    /**
     * Performance info record for core scalar implementation.
     * Uses Java 21 record pattern for clean data representation.
     *
     * @param vectorizationEnabled whether vectorization is enabled (always false in core)
     * @param vectorCapabilities placeholder for vector capabilities (always null in core)
     * @param availableProcessors number of available processors
     */
    public record PerformanceInfo(
        boolean vectorizationEnabled,
        Object vectorCapabilities, // Placeholder for vector capabilities (always null in core)
        int availableProcessors
    ) {
        
        /**
         * Returns a human-readable description of the performance configuration.
         */
        public String description() {
            return "Core module scalar mode: " + 
                   availableProcessors + " CPU cores";
        }
        
        /**
         * Estimates performance improvement for a given workload.
         * 
         * @param arraySize The size of arrays being processed
         * @return Estimated speedup factor
         */
        public double estimateSpeedup(int arraySize) {
            return 1.0;  // Core module always uses scalar mode
        }
    }
    
    /**
     * Applies soft thresholding to wavelet coefficients.
     * Core module uses scalar implementation.
     * 
     * @param coefficients the wavelet coefficients to threshold
     * @param threshold the threshold value
     * @return new array with thresholded coefficients
     */
    public static double[] softThreshold(double[] coefficients, double threshold) {
        double[] result = new double[coefficients.length];
        for (int i = 0; i < coefficients.length; i++) {
            double abs = Math.abs(coefficients[i]);
            if (abs > threshold) {
                result[i] = Math.signum(coefficients[i]) * (abs - threshold);
            } else {
                result[i] = 0.0;
            }
        }
        return result;
    }
    
    /**
     * Applies hard thresholding to wavelet coefficients.
     * Core module uses scalar implementation.
     * 
     * @param coefficients the wavelet coefficients to threshold
     * @param threshold the threshold value
     * @return new array with thresholded coefficients
     */
    public static double[] hardThreshold(double[] coefficients, double threshold) {
        double[] result = new double[coefficients.length];
        for (int i = 0; i < coefficients.length; i++) {
            if (Math.abs(coefficients[i]) > threshold) {
                result[i] = coefficients[i];
            } else {
                result[i] = 0.0;
            }
        }
        return result;
    }
}
