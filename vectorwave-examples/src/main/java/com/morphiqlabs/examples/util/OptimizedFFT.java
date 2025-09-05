package com.morphiqlabs.examples.util;

import com.morphiqlabs.wavelet.cwt.ComplexNumber;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Arrays;

// IMPORTANT: This import requires JDK with Vector API support.
// The Vector API was introduced as an incubating feature in JDK 16 and became stable in JDK 21.
// At runtime, the code gracefully falls back to scalar implementation if Vector API
// is not available or not functional.
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

/**
 * Highly optimized FFT implementation with advanced algorithms.
 * 
 * <p><strong>Compilation Requirements:</strong></p>
 * <p>This class requires compilation with a JDK that includes the jdk.incubator.vector
 * module (JDK 16 or later). However, at runtime it will gracefully fall back to
 * scalar implementations if the Vector API is not available.</p>
 * 
 * <p>This class provides optimized FFT implementations:</p>
 * <ul>
 *   <li>Radix-2 FFT for power-of-2 sizes with automatic SIMD vectorization</li>
 *   <li>Bluestein's algorithm for arbitrary (non-power-of-2) sizes</li>
 *   <li>Extended twiddle factor caching for performance</li>
 *   <li>Real-to-complex FFT optimization using Hermitian symmetry</li>
 *   <li>Automatic scalar fallback for small signals (n < 256) to avoid vectorization overhead</li>
 * </ul>
 * 
 * <p><strong>References:</strong></p>
 * <ul>
 *   <li>Cooley, J. W., & Tukey, J. W. (1965). "An algorithm for the machine calculation 
 *       of complex Fourier series." Mathematics of computation, 19(90), 297-301.</li>
 *   <li>Bluestein, L. (1970). "A linear filtering approach to the computation 
 *       of discrete Fourier transform." IEEE Transactions on Audio and Electroacoustics.</li>
 * </ul>
 */
public final class OptimizedFFT {
    
    // Extended twiddle factor cache
    private static final int MAX_CACHED_SIZE = 65536; // Up to 64K
    private static final ConcurrentHashMap<Integer, TwiddleFactors> TWIDDLE_CACHE = 
        new ConcurrentHashMap<>();
    
    // Vector API availability check
    private static final boolean VECTOR_API_AVAILABLE;
    private static final VectorSpecies<Double> SPECIES;
    
    // Performance threshold - use scalar for small signals
    // Benchmark results on various platforms:
    // - macOS ARM64 (vector length 2): scalar faster for all sizes
    // - Linux x86_64 AVX2 (vector length 4): crossover around 256-512
    // - Linux x86_64 AVX-512 (vector length 8): crossover around 128
    // Conservative default: 256 (can be tuned per platform)
    // Can be overridden via system property: -Dvectorwave.fft.vector.threshold=<value>
    // or environment variable: VECTORWAVE_FFT_VECTOR_THRESHOLD=<value>
    private static final int VECTOR_THRESHOLD = getVectorThreshold();
    
    // Thread-local storage for temporary arrays to avoid repeated allocations
    // Each thread gets its own set of temporary arrays, sized for the vector length
    private static final ThreadLocal<VectorTempArrays> VECTOR_TEMP_ARRAYS;
    
    /**
     * Determines the vector threshold from configuration sources.
     * Priority order: system property, environment variable, default (256).
     * 
     * @return the vector threshold to use
     */
    private static int getVectorThreshold() {
        // Try system property first
        String sysProp = System.getProperty("vectorwave.fft.vector.threshold");
        if (sysProp != null) {
            try {
                int value = Integer.parseInt(sysProp.trim());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // Fall through to next option
            }
        }
        
        // Try environment variable
        String envVar = System.getenv("VECTORWAVE_FFT_VECTOR_THRESHOLD");
        if (envVar != null) {
            try {
                int value = Integer.parseInt(envVar.trim());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException e) {
                // Fall through to default
            }
        }
        
        // Default value
        return 256;
    }
    
    static {
        boolean vectorAvailable = false;
        VectorSpecies<Double> speciesTemp = null;
        
        try {
            // Try to access Vector API
            speciesTemp = DoubleVector.SPECIES_PREFERRED;
            // Verify it works by creating a zero vector
            DoubleVector.zero(speciesTemp);
            vectorAvailable = true;
        } catch (RuntimeException | LinkageError e) {
            // Vector API not available or not functional
            // This catches:
            // - RuntimeException: UnsupportedOperationException, etc.
            // - LinkageError: NoClassDefFoundError, UnsatisfiedLinkError,
            //   and also ExceptionInInitializerError (which extends LinkageError)
            // Use scalar fallback
        }
        // Note: If OutOfMemoryError or other serious errors occur during
        // static initialization, we let them propagate to fail fast
        
        VECTOR_API_AVAILABLE = vectorAvailable;
        SPECIES = speciesTemp;
        // Initialize thread-local arrays now that SPECIES is known
        VECTOR_TEMP_ARRAYS = ThreadLocal.withInitial(() -> (VECTOR_API_AVAILABLE && SPECIES != null)
            ? new VectorTempArrays(SPECIES.length())
            : null);
    }
    
    /**
     * Container for thread-local temporary arrays used in vectorized FFT.
     * This avoids repeated allocations in the hot path.
     */
    private static class VectorTempArrays {
        final double[] tempReal1;
        final double[] tempImag1;
        final double[] tempReal2;
        final double[] tempImag2;
        
        VectorTempArrays(int vecLen) {
            tempReal1 = new double[vecLen];
            tempImag1 = new double[vecLen];
            tempReal2 = new double[vecLen];
            tempImag2 = new double[vecLen];
        }
    }
    
    /**
     * Cached twiddle factors for a specific FFT size.
     */
    private static class TwiddleFactors {
        final double[] cos;
        final double[] sin;
        
        TwiddleFactors(int n) {
            int halfN = n / 2;
            cos = new double[halfN];
            sin = new double[halfN];
            
            for (int k = 0; k < halfN; k++) {
                double angle = -2.0 * Math.PI * k / n;
                cos[k] = Math.cos(angle);
                sin[k] = Math.sin(angle);
            }
        }
    }
    
    /**
     * Performs optimized FFT with automatic algorithm selection.
     * 
     * @param data input data (real and imaginary interleaved)
     * @param n number of complex samples
     * @param inverse true for inverse FFT
     */
    public static void fftOptimized(double[] data, int n, boolean inverse) {
        fftOptimized(data, n, inverse, null);
    }
    
    /**
     * Performs optimized FFT with configuration control.
     * 
     * @param data input data (real and imaginary interleaved)
     * @param n number of complex samples
     * @param inverse true for inverse FFT
     * @param config transform configuration (null for auto-detection)
     */
    public static void fftOptimized(double[] data, int n, boolean inverse, TransformConfig config) {
        if (n <= 1) return;
        
        if (isPowerOf2(n)) {
            // Use radix-2 for all power-of-2 sizes
            // The implementation will automatically use vectorization if available
            fftRadix2(data, n, inverse, config);
        } else {
            // Use Bluestein for arbitrary sizes
            fftBluestein(data, n, inverse);
        }
        
        // Note: Normalization for inverse FFT is handled within the specific implementations
        // (fftRadix2 and fftBluestein) to avoid double normalization
    }
    
    /**
     * Unified radix-2 FFT implementation with automatic vectorization.
     * 
     * <p>This method automatically uses SIMD operations when available,
     * falling back to scalar operations otherwise.</p>
     * 
     * @param data input data (real and imaginary interleaved)
     * @param n number of complex samples (must be power of 2)
     * @param inverse true for inverse FFT
     */
    private static void fftRadix2(double[] data, int n, boolean inverse) {
        fftRadix2(data, n, inverse, null);
    }
    
    private static void fftRadix2(double[] data, int n, boolean inverse, TransformConfig config) {
        boolean forceScalar = config != null && config.isForceScalar();
        boolean forceVector = config != null && config.isForceVector();
        
        // Determine which implementation to use
        if (forceScalar) {
            // Explicitly forced to use scalar
            fftRadix2Scalar(data, n, inverse);
        } else if (forceVector && VECTOR_API_AVAILABLE) {
            // Explicitly forced to use Vector API (ignore threshold)
            fftRadix2Vector(data, n, inverse);
        } else if (!VECTOR_API_AVAILABLE || n < VECTOR_THRESHOLD) {
            // Use scalar for small signals or when vector API not available
            fftRadix2Scalar(data, n, inverse);
        } else {
            // Use vector for large signals when available
            fftRadix2Vector(data, n, inverse);
        }
    }

    private static boolean isPowerOf2(int n) {
        return n > 0 && (n & (n - 1)) == 0;
    }

    // Minimal local configuration to avoid depending on core internals
    public static final class TransformConfig {
        private final boolean forceScalar;
        private final boolean forceVector;

        private TransformConfig(boolean forceScalar, boolean forceVector) {
            this.forceScalar = forceScalar;
            this.forceVector = forceVector;
        }

        public static TransformConfig forceScalar() { return new TransformConfig(true, false); }
        public static TransformConfig forceVector() { return new TransformConfig(false, true); }
        public boolean isForceScalar() { return forceScalar; }
        public boolean isForceVector() { return forceVector; }
    }
    
    
    /**
     * Scalar radix-2 FFT implementation (fallback when Vector API is not available).
     * Package-private for testing.
     */
    static void fftRadix2Scalar(double[] data, int n, boolean inverse) {
        bitReverseVector(data, n);
        
        int sign = inverse ? 1 : -1;
        
        for (int len = 2; len <= n; len <<= 1) {
            int halfLen = len >> 1;
            TwiddleFactors twiddle = getTwiddleFactors(len);
            
            for (int i = 0; i < n; i += len) {
                for (int j = 0; j < halfLen; j++) {
                    int idx1 = 2 * (i + j);
                    int idx2 = 2 * (i + j + halfLen);
                    
                    double wr = twiddle.cos[j];
                    double wi = sign * twiddle.sin[j];
                    
                    double xr = data[idx2] * wr - data[idx2 + 1] * wi;
                    double xi = data[idx2] * wi + data[idx2 + 1] * wr;
                    
                    data[idx2] = data[idx1] - xr;
                    data[idx2 + 1] = data[idx1 + 1] - xi;
                    data[idx1] += xr;
                    data[idx1 + 1] += xi;
                }
            }
        }
        
        // Normalize for inverse transform to match fftOptimized behavior
        if (inverse) {
            double norm = 1.0 / n;
            for (int i = 0; i < 2 * n; i++) {
                data[i] *= norm;
            }
        }
    }
    
    /**
     * Vectorized radix-2 FFT using SIMD operations.
     */
    private static void fftRadix2Vector(double[] data, int n, boolean inverse) {
        if (!VECTOR_API_AVAILABLE) {
            fftRadix2Scalar(data, n, inverse);
            return;
        }
        
        try {
            bitReverseVector(data, n);
            
            int sign = inverse ? 1 : -1;
            int vecLen = SPECIES.length();
            
            // Get thread-local temporary arrays to avoid allocations
            VectorTempArrays tempArrays = VECTOR_TEMP_ARRAYS.get();
            if (tempArrays == null) {
                // Fallback if thread-local initialization failed
                fftRadix2Scalar(data, n, inverse);
                return;
            }
            
            double[] tempReal1 = tempArrays.tempReal1;
            double[] tempImag1 = tempArrays.tempImag1;
            double[] tempReal2 = tempArrays.tempReal2;
            double[] tempImag2 = tempArrays.tempImag2;
            
            for (int len = 2; len <= n; len <<= 1) {
                int halfLen = len >> 1;
                TwiddleFactors twiddle = getTwiddleFactors(len);
                
                for (int i = 0; i < n; i += len) {
                    // Vectorized loop for j - process elements in chunks
                    int j = 0;
                    
                    for (; j + vecLen <= halfLen; j += vecLen) {
                        // Load twiddle factors
                        DoubleVector wr = DoubleVector.fromArray(SPECIES, twiddle.cos, j);
                        DoubleVector wi = DoubleVector.fromArray(SPECIES, twiddle.sin, j)
                            .mul(sign);
                        
                        // Gather complex data - unroll for better performance
                        // This manual unrolling can help the JIT compiler optimize better
                        if (vecLen == 2) {
                            // Common case for many platforms
                            int idx1_0 = 2 * (i + j);
                            int idx2_0 = 2 * (i + j + halfLen);
                            int idx1_1 = 2 * (i + j + 1);
                            int idx2_1 = 2 * (i + j + 1 + halfLen);
                            
                            tempReal1[0] = data[idx1_0];
                            tempImag1[0] = data[idx1_0 + 1];
                            tempReal2[0] = data[idx2_0];
                            tempImag2[0] = data[idx2_0 + 1];
                            tempReal1[1] = data[idx1_1];
                            tempImag1[1] = data[idx1_1 + 1];
                            tempReal2[1] = data[idx2_1];
                            tempImag2[1] = data[idx2_1 + 1];
                        } else {
                            // General case
                            for (int k = 0; k < vecLen; k++) {
                                int idx1 = 2 * (i + j + k);
                                int idx2 = 2 * (i + j + k + halfLen);
                                
                                tempReal1[k] = data[idx1];
                                tempImag1[k] = data[idx1 + 1];
                                tempReal2[k] = data[idx2];
                                tempImag2[k] = data[idx2 + 1];
                            }
                        }
                        
                        // Vectorized butterfly computation
                        DoubleVector vReal1 = DoubleVector.fromArray(SPECIES, tempReal1, 0);
                        DoubleVector vImag1 = DoubleVector.fromArray(SPECIES, tempImag1, 0);
                        DoubleVector vReal2 = DoubleVector.fromArray(SPECIES, tempReal2, 0);
                        DoubleVector vImag2 = DoubleVector.fromArray(SPECIES, tempImag2, 0);
                        
                        // Complex multiplication: t = (real2 + i*imag2) * (wr + i*wi)
                        DoubleVector tReal = vReal2.mul(wr).sub(vImag2.mul(wi));
                        DoubleVector tImag = vReal2.mul(wi).add(vImag2.mul(wr));
                        
                        // Butterfly operation
                        DoubleVector resultReal1 = vReal1.add(tReal);
                        DoubleVector resultImag1 = vImag1.add(tImag);
                        DoubleVector resultReal2 = vReal1.sub(tReal);
                        DoubleVector resultImag2 = vImag1.sub(tImag);
                        
                        // Store results - unroll for common case
                        if (vecLen == 2) {
                            // Store directly without temporary arrays for common case
                            int idx1_0 = 2 * (i + j);
                            int idx2_0 = 2 * (i + j + halfLen);
                            int idx1_1 = 2 * (i + j + 1);
                            int idx2_1 = 2 * (i + j + 1 + halfLen);
                            
                            data[idx1_0] = resultReal1.lane(0);
                            data[idx1_0 + 1] = resultImag1.lane(0);
                            data[idx2_0] = resultReal2.lane(0);
                            data[idx2_0 + 1] = resultImag2.lane(0);
                            data[idx1_1] = resultReal1.lane(1);
                            data[idx1_1 + 1] = resultImag1.lane(1);
                            data[idx2_1] = resultReal2.lane(1);
                            data[idx2_1 + 1] = resultImag2.lane(1);
                        } else {
                            // General case - store to temp arrays first
                            resultReal1.intoArray(tempReal1, 0);
                            resultImag1.intoArray(tempImag1, 0);
                            resultReal2.intoArray(tempReal2, 0);
                            resultImag2.intoArray(tempImag2, 0);
                            
                            for (int k = 0; k < vecLen; k++) {
                                int idx1 = 2 * (i + j + k);
                                int idx2 = 2 * (i + j + k + halfLen);
                                
                                data[idx1] = tempReal1[k];
                                data[idx1 + 1] = tempImag1[k];
                                data[idx2] = tempReal2[k];
                                data[idx2 + 1] = tempImag2[k];
                            }
                        }
                    }
                    
                    // Handle remaining elements
                    for (; j < halfLen; j++) {
                        int idx1 = 2 * (i + j);
                        int idx2 = 2 * (i + j + halfLen);
                        
                        double wr = twiddle.cos[j];
                        double wi = sign * twiddle.sin[j];
                        
                        double xr = data[idx2] * wr - data[idx2 + 1] * wi;
                        double xi = data[idx2] * wi + data[idx2 + 1] * wr;
                        
                        data[idx2] = data[idx1] - xr;
                        data[idx2 + 1] = data[idx1 + 1] - xi;
                        data[idx1] += xr;
                        data[idx1 + 1] += xi;
                    }
                }
            }
            
            // Normalize for inverse transform to match fftRadix2Scalar behavior
            if (inverse) {
                double norm = 1.0 / n;
                for (int i = 0; i < 2 * n; i++) {
                    data[i] *= norm;
                }
            }
        } catch (RuntimeException e) {
            // For runtime exceptions (e.g., Vector API not available at runtime),
            // fall back to scalar implementation
            // This includes UnsupportedOperationException, IllegalArgumentException, etc.
            // that might be thrown by Vector API operations
            fftRadix2Scalar(data, n, inverse);
        } catch (LinkageError e) {
            // Handle specific errors that might occur during Vector API initialization:
            // - LinkageError hierarchy includes:
            //   - NoClassDefFoundError: class not found at runtime
            //   - UnsatisfiedLinkError: native library issues
            //   - ExceptionInInitializerError: static initialization failures
            // These are recoverable by falling back to scalar implementation
            fftRadix2Scalar(data, n, inverse);
        }
        // Note: We intentionally do NOT catch:
        // - OutOfMemoryError: JVM is out of memory
        // - StackOverflowError: Stack overflow
        // - ThreadDeath: Thread termination (deprecated)
        // - VirtualMachineError: Internal JVM errors
        // These indicate serious JVM issues that should propagate
    }
    
    /**
     * Bluestein's algorithm for arbitrary-size FFT.
     * 
     * <p>Converts arbitrary-size DFT to convolution, which can be
     * computed using power-of-2 FFT.</p>
     * 
     * <p><strong>Implementation Note on Inverse FFT:</strong></p>
     * <p>This method uses forward FFT (inverse=false) for the convolution step
     * and manually applies the inverse transformation. This design decision is based on:</p>
     * <ol>
     *   <li><strong>Avoiding automatic normalization:</strong> The fftRadix2 method applies 
     *       automatic normalization when inverse=true, which is not suitable for the Bluestein
     *       algorithm. Bluestein requires a specific scaling factor (1/m) that differs from
     *       the standard inverse FFT normalization (1/n).</li>
     *   <li><strong>Consistency and control:</strong> Manual inverse transformation ensures
     *       that scaling and normalization are explicitly defined and consistent, regardless
     *       of future changes to fftRadix2's internal logic.</li>
     *   <li><strong>Algorithmic requirements:</strong> The Bluestein algorithm's convolution
     *       in frequency domain requires precise control over scaling and conjugation steps.</li>
     * </ol>
     * <p><strong>WARNING:</strong> Modifying this behavior without understanding the implications
     * could introduce subtle bugs. Any changes must be thoroughly tested.</p>
     */
    private static void fftBluestein(double[] data, int n, boolean inverse) {
        // Find next power of 2 >= 2n - 1 using bit operations for optimal performance
        int target = 2 * n - 1;
        int m = nextPowerOf2(target);
        
        // Chirp sequence
        double[] chirp = new double[2 * n];
        for (int i = 0; i < n; i++) {
            double angle = Math.PI * i * i / n;
            if (inverse) angle = -angle;
            chirp[2 * i] = Math.cos(angle);
            chirp[2 * i + 1] = Math.sin(angle);
        }
        
        // Prepare sequences for convolution
        double[] a = new double[2 * m];
        double[] b = new double[2 * m];
        
        // a[k] = x[k] * chirp[k]
        for (int i = 0; i < n; i++) {
            a[2 * i] = data[2 * i] * chirp[2 * i] - data[2 * i + 1] * chirp[2 * i + 1];
            a[2 * i + 1] = data[2 * i] * chirp[2 * i + 1] + data[2 * i + 1] * chirp[2 * i];
        }
        
        // b[k] = conjugate(chirp[k]) for k = 0..n-1
        // b[k] = conjugate(chirp[m-k]) for k = m-n+1..m-1
        for (int i = 0; i < n; i++) {
            b[2 * i] = chirp[2 * i];
            b[2 * i + 1] = -chirp[2 * i + 1];
        }
        for (int i = 1; i < n; i++) {
            b[2 * (m - i)] = chirp[2 * i];
            b[2 * (m - i) + 1] = -chirp[2 * i + 1];
        }
        
        // Convolve using FFT
        // Note: We use forward FFT (inverse=false) for both sequences
        fftRadix2(a, m, false);
        fftRadix2(b, m, false);
        
        // Multiply in frequency domain
        for (int i = 0; i < m; i++) {
            int idx = 2 * i;
            double ar = a[idx];
            double ai = a[idx + 1];
            double br = b[idx];
            double bi = b[idx + 1];
            a[idx] = ar * br - ai * bi;
            a[idx + 1] = ar * bi + ai * br;
        }
        
        // Apply inverse FFT for convolution result
        // Use forward FFT and manual inverse (see method JavaDoc for rationale)
        fftRadix2(a, m, false);
        // Manually apply conjugation and Bluestein-specific scaling (1/m)
        double scale = 1.0 / m;
        for (int i = 0; i < m; i++) {
            double temp = a[2 * i + 1];
            a[2 * i] *= scale;
            a[2 * i + 1] = -temp * scale;
        }
        
        // Extract result: y[k] = a[k] * chirp[k]
        for (int i = 0; i < n; i++) {
            data[2 * i] = a[2 * i] * chirp[2 * i] - a[2 * i + 1] * chirp[2 * i + 1];
            data[2 * i + 1] = a[2 * i] * chirp[2 * i + 1] + a[2 * i + 1] * chirp[2 * i];
        }
    }

    // Local next power-of-two utility to avoid depending on core internals
    private static int nextPowerOf2(int x) {
        if (x <= 1) return 1;
        x--;
        x |= x >>> 1;
        x |= x >>> 2;
        x |= x >>> 4;
        x |= x >>> 8;
        x |= x >>> 16;
        return x + 1;
    }
    
    /**
     * Optimized bit-reversal using vectorization.
     */
    private static void bitReverseVector(double[] data, int n) {
        int shift = 32 - Integer.numberOfTrailingZeros(n);
        
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> shift;
            if (i < j) {
                // Swap complex numbers
                int idx1 = 2 * i;
                int idx2 = 2 * j;
                double tr = data[idx1];
                double ti = data[idx1 + 1];
                data[idx1] = data[idx2];
                data[idx1 + 1] = data[idx2 + 1];
                data[idx2] = tr;
                data[idx2 + 1] = ti;
            }
        }
    }
    
    /**
     * Real-to-complex FFT using half the operations.
     * 
     * <p>Exploits Hermitian symmetry to compute FFT of real signal
     * using N/2 complex FFT.</p>
     * 
     * <p><strong>Implementation Note:</strong></p>
     * <p>The unpacking loop uses k &lt; halfN (not k &lt;= halfN) as the bound.
     * This is mathematically necessary because when k = halfN, the indices
     * k and (halfN - k) become equal, resulting in packed[2 * k] and 
     * packed[2 * (halfN - k)] referring to the same element. This would
     * violate the FFT butterfly operation requirements, which rely on
     * distinct pairs of elements for correct computation.</p>
     * 
     * @param real the real-valued input signal
     * @return the complex FFT result
     */
    public static ComplexNumber[] fftRealOptimized(double[] real) {
        int n = real.length;
        if (n == 0) {
            return new ComplexNumber[0];
        }
        if (n == 1) {
            return new ComplexNumber[]{new ComplexNumber(real[0], 0)};
        }
        
        // For odd-length arrays, use standard FFT
        if (n % 2 != 0) {
            double[] complexData = new double[2 * n];
            for (int i = 0; i < n; i++) {
                complexData[2 * i] = real[i];
                complexData[2 * i + 1] = 0;
            }
            fftOptimized(complexData, n, false);
            ComplexNumber[] result = new ComplexNumber[n];
            for (int i = 0; i < n; i++) {
                result[i] = new ComplexNumber(complexData[2 * i], complexData[2 * i + 1]);
            }
            return result;
        }
        
        // Pack real data into complex array (even in real, odd in imag)
        int halfN = n / 2;
        double[] packed = new double[2 * halfN];
        for (int i = 0; i < halfN; i++) {
            packed[2 * i] = real[2 * i];
            packed[2 * i + 1] = real[2 * i + 1];
        }
        
        // Compute FFT of half-size
        fftOptimized(packed, halfN, false);
        
        // Unpack using symmetry properties
        ComplexNumber[] result = new ComplexNumber[n];
        result[0] = new ComplexNumber(
            packed[0] + packed[1], 
            0
        );
        result[halfN] = new ComplexNumber(
            packed[0] - packed[1], 
            0
        );
        
        // Note: packed array is created with exactly 2 * halfN elements above,
        // so bounds checking is not needed here
        
        // For n=2 (halfN=1), the loop doesn't execute, which is correct
        // since we already handled DC and Nyquist components above
        for (int k = 1; k < halfN; k++) { // Use k < halfN to ensure indices k and halfN - k are distinct, as required by FFT butterfly operations
            // Compute indices for the butterfly operation
            int idx1 = 2 * k;
            int idx2 = 2 * (halfN - k);
            
            // Mathematical invariant: these indices are always valid due to loop bounds
            // k ranges from 1 to halfN-1, so:
            // - idx1 ranges from 2 to 2*(halfN-1) < packed.length
            // - idx2 ranges from 2 to 2*(halfN-1) < packed.length
            assert idx1 + 1 < packed.length && idx2 + 1 < packed.length : 
                "Invalid indices: k=" + k + ", halfN=" + halfN;
            
            double wr = Math.cos(Math.PI * k / halfN);
            double wi = -Math.sin(Math.PI * k / halfN);
            
            // G[k] = FFT(even indexed)
            // H[k] = FFT(odd indexed)
            double gr = 0.5 * (packed[2 * k] + packed[2 * (halfN - k)]);
            double gi = 0.5 * (packed[2 * k + 1] - packed[2 * (halfN - k) + 1]);
            double hr = 0.5 * (packed[2 * k + 1] + packed[2 * (halfN - k) + 1]);
            double hi = -0.5 * (packed[2 * k] - packed[2 * (halfN - k)]);
            
            // Apply twiddle factor to H[k]
            double tr = hr * wr - hi * wi;
            double ti = hr * wi + hi * wr;
            
            // X[k] = G[k] + W * H[k]
            result[k] = new ComplexNumber(gr + tr, gi + ti);
            // X[n-k] = conj(G[k] - W * H[k])
            result[n - k] = new ComplexNumber(gr - tr, -(gi - ti));
        }
        
        return result;
    }
    
    /**
     * Get cached twiddle factors or compute if not cached.
     */
    private static TwiddleFactors getTwiddleFactors(int n) {
        if (n <= MAX_CACHED_SIZE) {
            return TWIDDLE_CACHE.computeIfAbsent(n, TwiddleFactors::new);
        }
        return new TwiddleFactors(n);
    }
    
    
    /**
     * Returns true if Vector API is available and functional on this platform.
     * 
     * @return true if Vector API is available, false otherwise
     */
    public static boolean isVectorApiAvailable() {
        return VECTOR_API_AVAILABLE;
    }
    
    /**
     * Returns information about Vector API availability.
     * 
     * @return a string describing the Vector API status
     */
    public static String getVectorApiInfo() {
        if (VECTOR_API_AVAILABLE && SPECIES != null) {
            return "Vector API available with vector length: " + SPECIES.length();
        } else {
            return "Vector API not available - using scalar fallback";
        }
    }
    
    private OptimizedFFT() {}
}
