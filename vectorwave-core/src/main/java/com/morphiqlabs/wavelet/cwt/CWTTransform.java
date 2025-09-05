package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.api.ComplexContinuousWavelet;
import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.fft.CoreFFT;
import com.morphiqlabs.wavelet.annotations.Experimental;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * Main engine for Continuous Wavelet Transform computation.
 *
 * <p><strong>Experimental:</strong> This API is not yet stable and may change.
 * Use with caution in production and pin to a specific version if needed.</p>
 *
 * <p>Provides methods to analyze signals using continuous wavelets,
 * supporting both direct convolution and FFT-accelerated computation.</p>
 */
@Experimental
public final class CWTTransform {
    
    /**
     * Factor used to calculate wavelet support from scale and bandwidth.
     * The support is calculated as: WAVELET_SUPPORT_FACTOR * scale * bandwidth.
     * This ensures adequate coverage of the wavelet's significant values.
     */
    private static final int WAVELET_SUPPORT_FACTOR = 8;
    
    private final ContinuousWavelet wavelet;
    private final CWTConfig config;
    
    // Cache for wavelet support calculations
    private final Map<Double, Integer> waveletSupportCache = new ConcurrentHashMap<>();
    
    /**
     * Creates a CWT transform with default configuration.
     * 
     * @param wavelet the continuous wavelet to use
     */
    public CWTTransform(ContinuousWavelet wavelet) {
        this(wavelet, CWTConfig.defaultConfig());
    }
    
    /**
     * Creates a CWT transform with custom configuration.
     * 
     * @param wavelet the continuous wavelet to use
     * @param config the configuration
     */
    public CWTTransform(ContinuousWavelet wavelet, CWTConfig config) {
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }
        
        this.wavelet = wavelet;
        this.config = config;
    }
    
    /**
     * Analyzes a signal using the specified scales.
     * 
     * @param signal the input signal
     * @param scales the scales to analyze
     * @return CWT result
     */
    public CWTResult analyze(double[] signal, double[] scales) {
        validateInputs(signal, scales);
        
        if (config.shouldUseFFT(signal.length) && !wavelet.isComplex()) {
            return analyzeFFT(signal, scales);
        } else {
            return analyzeDirect(signal, scales);
        }
    }
    
    /**
     * Analyzes a signal using complex wavelet transform.
     * 
     * @param signal the input signal
     * @param scales the scales to analyze
     * @return complex CWT coefficients preserving magnitude and phase
     */
    public ComplexCWTResult analyzeComplex(double[] signal, double[] scales) {
        validateInputs(signal, scales);
        
        if (!wavelet.isComplex() && !(wavelet instanceof ComplexContinuousWavelet)) {
            // For real wavelets, compute analytic signal using Hilbert transform
            return analyzeRealAsComplex(signal, scales);
        }
        
        if (config.shouldUseFFT(signal.length)) {
            return analyzeFFTComplex(signal, scales);
        } else {
            return analyzeDirectComplexFull(signal, scales);
        }
    }
    
    /**
     * Analyzes a signal using a scale space.
     * 
     * @param signal the input signal
     * @param scaleSpace the scale space
     * @return CWT result
     */
    public CWTResult analyze(double[] signal, ScaleSpace scaleSpace) {
        if (scaleSpace == null) {
            throw new IllegalArgumentException("ScaleSpace cannot be null");
        }
        return analyze(signal, scaleSpace.getScales());
    }
    
    /**
     * Direct convolution implementation.
     */
    private CWTResult analyzeDirect(double[] signal, double[] scales) {
        if (wavelet.isComplex()) {
            return analyzeDirectComplex(signal, scales);
        }
        
        // Allocate coefficients array using memory pool if available
        double[][] coefficients;
        if (config.getMemoryPool() != null) {
            coefficients = config.getMemoryPool().allocateCoefficients(scales.length, signal.length);
        } else {
            coefficients = new double[scales.length][signal.length];
        }
        
        // Direct computation for each scale
        for (int s = 0; s < scales.length; s++) {
            double scale = scales[s];
            double sqrtScale = config.isNormalizeAcrossScales() ? Math.sqrt(scale) : 1.0;
            
            int halfSupport = getHalfSupport(scale);
            
            for (int tau = 0; tau < signal.length; tau++) {
                double sum = 0.0;
                
                for (int t = -halfSupport; t <= halfSupport; t++) {
                    int idx = tau + t;
                    double signalValue;
                    
                    if (idx >= 0 && idx < signal.length) {
                        signalValue = signal[idx];
                    } else {
                        signalValue = getBoundaryValue(signal, idx);
                    }
                    
                    double waveletValue = wavelet.psi(-t / scale) / sqrtScale;
                    sum += signalValue * waveletValue;
                }
                
                coefficients[s][tau] = sum;
            }
        }
        
        // Normalization is already applied during computation
        
        // Apply boundary handling if not periodic
        if (config.getBoundaryMode() != com.morphiqlabs.wavelet.api.BoundaryMode.PERIODIC) {
            coefficients = applyBoundaryHandling(signal, scales, coefficients);
        }
        
        return new CWTResult(coefficients, scales, wavelet);
    }
    
    /**
     * Direct convolution for complex wavelets.
     */
    private CWTResult analyzeDirectComplex(double[] signal, double[] scales) {
        int signalLength = signal.length;
        int numScales = scales.length;
        ComplexMatrix complexCoeffs = new ComplexMatrix(numScales, signalLength);
        
        // Check if wavelet implements ComplexContinuousWavelet interface
        ComplexContinuousWavelet complexWavelet = wavelet instanceof ComplexContinuousWavelet cw ? cw : null;
        
        for (int s = 0; s < numScales; s++) {
            double scale = scales[s];
            double sqrtScale = config.isNormalizeAcrossScales() ? Math.sqrt(scale) : 1.0;
            
            int halfSupport = getHalfSupport(scale);
            
            for (int tau = 0; tau < signalLength; tau++) {
                double sumReal = 0.0;
                double sumImag = 0.0;
                
                for (int t = -halfSupport; t <= halfSupport; t++) {
                    int idx = tau + t;
                    double signalValue;
                    
                    if (idx >= 0 && idx < signalLength) {
                        signalValue = signal[idx];
                    } else {
                        signalValue = getBoundaryValue(signal, idx);
                    }
                    
                    // Real part
                    double waveletReal = wavelet.psi(-t / scale) / sqrtScale;
                    sumReal += signalValue * waveletReal;
                    
                    // Imaginary part (if complex wavelet)
                    if (complexWavelet != null) {
                        double waveletImag = complexWavelet.psiImaginary(-t / scale) / sqrtScale;
                        sumImag += signalValue * waveletImag;
                    }
                }
                
                complexCoeffs.set(s, tau, sumReal, sumImag);
            }
        }
        
        return new CWTResult(complexCoeffs, scales, wavelet);
    }
    
    /**
     * FFT-accelerated implementation.
     */
    private CWTResult analyzeFFT(double[] signal, double[] scales) {
        int signalLength = signal.length;
        int numScales = scales.length;
        
        // Calculate maximum wavelet support across all scales
        int maxWaveletSupport = 0;
        for (double scale : scales) {
            int support = getWaveletSupport(scale);
            maxWaveletSupport = Math.max(maxWaveletSupport, support);
        }
        
        // FFT size for linear convolution: signal_length + wavelet_support - 1
        int minFFTSize = signalLength + maxWaveletSupport - 1;
        int fftSize = config.getFFTSize() > 0 ? 
            Math.max(config.getFFTSize(), nextPowerOfTwo(minFFTSize)) : 
            nextPowerOfTwo(minFFTSize);
        
        // Pad signal to FFT size
        double[] paddedSignal = new double[fftSize];
        System.arraycopy(signal, 0, paddedSignal, 0, signalLength);
        
        // Apply FFT to signal
        Complex[] signalFFT = fft(paddedSignal);
        
        double[][] coefficients = new double[numScales][signalLength];
        
        if (config.isUseStructuredConcurrency() && numScales >= 4) {
            // Parallel processing for FFT-based CWT
            IntStream.range(0, numScales).parallel().forEach(s -> {
                coefficients[s] = computeFFTScale(signalFFT, scales[s], fftSize, signalLength);
            });
        } else {
            // Sequential processing
            for (int s = 0; s < numScales; s++) {
                coefficients[s] = computeFFTScale(signalFFT, scales[s], fftSize, signalLength);
            }
        }
        
        return new CWTResult(coefficients, scales, wavelet);
    }
    
    /**
     * Computes CWT coefficients for a single scale using FFT.
     * Uses linear convolution to avoid circular artifacts.
     */
    private double[] computeFFTScale(Complex[] signalFFT, double scale, int fftSize, int signalLength) {
        if (signalFFT == null || signalFFT.length == 0) {
            throw new IllegalStateException("Cannot compute FFT scale with null or empty signal FFT");
        }
        if (scale <= 0) {
            throw new IllegalArgumentException("Scale must be positive, got: " + scale);
        }
        
        try {
            double sqrtScale = config.isNormalizeAcrossScales() ? Math.sqrt(scale) : 1.0;
            
            // Generate scaled wavelet for linear convolution
            double[] scaledWavelet = generateScaledWaveletLinear(scale, fftSize);
            
            // FFT of wavelet
            Complex[] waveletFFT = fft(scaledWavelet);
            
            // Multiply in frequency domain (convolution theorem)
            Complex[] product = new Complex[fftSize];
            for (int i = 0; i < fftSize; i++) {
                // Conjugate of wavelet for correlation
                product[i] = signalFFT[i].multiply(waveletFFT[i].conjugate());
            }
            
            // Inverse FFT
            double[] convResult = ifft(product);
            
            // Extract valid portion with proper offset
            // The wavelet was placed at the beginning of the array with its center at index halfSupport
            // In the convolution result:
            //   - Index halfSupport corresponds to the wavelet centered at signal position 0
            //   - Index halfSupport + i corresponds to the wavelet centered at signal position i
            // Therefore, we extract starting from index halfSupport
            double[] result = new double[signalLength];
            int halfSupport = getHalfSupport(scale);
            
            for (int i = 0; i < signalLength; i++) {
                int idx = i + halfSupport;
                if (idx < fftSize) {
                    result[i] = convResult[idx] / sqrtScale;
                } else {
                    result[i] = 0.0;
                }
            }
            
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("FFT-based CWT computation failed for scale " + scale + 
                " (FFT size: " + fftSize + ", signal length: " + signalLength + "): " + e.getMessage(), e);
        }
    }
    
    
    /**
     * Generates a scaled wavelet for linear convolution.
     * Places the wavelet at the beginning of the array to avoid circular artifacts.
     */
    private double[] generateScaledWaveletLinear(double scale, int length) {
        double[] waveletArray = new double[length];
        int halfSupport = getHalfSupport(scale);
        
        // Place wavelet at the beginning of the array
        // This ensures no wrap-around occurs
        for (int i = 0; i <= 2 * halfSupport; i++) {
            if (i < length) {
                double t = (i - halfSupport) / scale;
                waveletArray[i] = wavelet.psi(t);
            }
        }
        
        return waveletArray;
    }
    
    /**
     * Apply boundary handling to coefficients.
     */
    private double[][] applyBoundaryHandling(double[] signal, double[] scales, 
                                           double[][] coefficients) {
        // For non-periodic boundaries, the coefficients are already computed
        // with proper boundary handling in the main computation loop
        return coefficients;
    }
    
    /**
     * Gets boundary value based on configuration.
     */
    private double getBoundaryValue(double[] signal, int index) {
        int length = signal.length;
        
        return switch (config.getPaddingStrategy()) {
            case ZERO -> 0.0;
                
            case REFLECT -> {
                if (index < 0) {
                    int reflectedIndex = -index;
                    if (reflectedIndex >= length) {
                        reflectedIndex = length - 1;
                    }
                    yield signal[reflectedIndex];
                } else if (index >= length) {
                    int reflectedIndex = 2 * length - index - 2;
                    if (reflectedIndex < 0) {
                        reflectedIndex = 0;
                    }
                    yield signal[reflectedIndex];
                }
                yield 0.0; // fallback
            }
                
            case SYMMETRIC -> {
                if (index < 0) {
                    int symmetricIndex = -index - 1;
                    if (symmetricIndex >= length) {
                        symmetricIndex = length - 1;
                    }
                    yield signal[symmetricIndex];
                } else if (index >= length) {
                    int symmetricIndex = 2 * length - index - 1;
                    if (symmetricIndex < 0) {
                        symmetricIndex = 0;
                    }
                    yield signal[symmetricIndex];
                }
                yield 0.0; // fallback
            }
                
            case PERIODIC -> signal[(index % length + length) % length];
        };
    }
    
    
    /**
     * FFT implementation using the optimized FFT based on configuration.
     * Automatically detects real signals and uses real-optimized FFT when beneficial.
     */
    private Complex[] fft(double[] x) {
        if (x == null || x.length == 0) {
            throw new IllegalStateException("Cannot perform FFT on null or empty signal");
        }
        
        try {
            // Use CoreFFT
            double[] complexData = CoreFFT.rfft(x);

            // Convert to Complex array
            Complex[] X = new Complex[x.length];
            for (int i = 0; i < x.length; i++) {
                X[i] = new Complex(complexData[2 * i], complexData[2 * i + 1]);
            }
            return X;
        } catch (Exception e) {
            throw new IllegalStateException("FFT computation failed for signal of length " + x.length + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Inverse FFT implementation using the optimized FFT.
     */
    private double[] ifft(Complex[] X) {
        if (X == null || X.length == 0) {
            throw new IllegalStateException("Cannot perform IFFT on null or empty spectrum");
        }
        
        try {
            // Convert Complex array to interleaved format
            double[] complexData = new double[2 * X.length];
            for (int i = 0; i < X.length; i++) {
                complexData[2 * i] = X[i].real;
                complexData[2 * i + 1] = X[i].imag;
            }
            
            // Use CoreFFT inverse real FFT
            return CoreFFT.irfft(complexData);
        } catch (Exception e) {
            throw new IllegalStateException("IFFT computation failed for spectrum of length " + X.length + 
                ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Validates input parameters.
     */
    private void validateInputs(double[] signal, double[] scales) {
        if (signal == null) {
            throw new IllegalArgumentException("Signal cannot be null");
        }
        if (signal.length == 0) {
            throw new IllegalArgumentException("Signal cannot be empty");
        }
        if (scales == null) {
            throw new IllegalArgumentException("Scales cannot be null");
        }
        if (scales.length == 0) {
            throw new IllegalArgumentException("Scales cannot be empty");
        }
        
        for (double scale : scales) {
            if (scale <= 0) {
                throw new IllegalArgumentException("All scales must be positive");
            }
        }
    }
    
    /**
     * Analyzes real wavelet as complex using Hilbert transform.
     */
    private ComplexCWTResult analyzeRealAsComplex(double[] signal, double[] scales) {
        // First compute real CWT
        CWTResult realResult = analyze(signal, scales);
        double[][] realCoeffs = realResult.getCoefficients();
        
        // Convert to complex using Hilbert transform on each scale
        ComplexNumber[][] complexCoeffs = new ComplexNumber[scales.length][signal.length];
        
        for (int s = 0; s < scales.length; s++) {
            double[] hilbert = computeHilbertTransform(realCoeffs[s]);
            for (int t = 0; t < signal.length; t++) {
                complexCoeffs[s][t] = new ComplexNumber(realCoeffs[s][t], hilbert[t]);
            }
        }
        
        return new ComplexCWTResult(complexCoeffs, scales, wavelet);
    }
    
    /**
     * Direct complex convolution implementation.
     */
    private ComplexCWTResult analyzeDirectComplexFull(double[] signal, double[] scales) {
        int signalLength = signal.length;
        int numScales = scales.length;
        ComplexNumber[][] coefficients = new ComplexNumber[numScales][signalLength];
        
        // Check if wavelet is already complex
        ComplexContinuousWavelet complexWavelet = wavelet instanceof ComplexContinuousWavelet cw ? cw : null;
        
        for (int s = 0; s < numScales; s++) {
            double scale = scales[s];
            double sqrtScale = config.isNormalizeAcrossScales() ? Math.sqrt(scale) : 1.0;
            
            int halfSupport = getHalfSupport(scale);
            
            for (int tau = 0; tau < signalLength; tau++) {
                double sumReal = 0.0;
                double sumImag = 0.0;
                
                for (int t = -halfSupport; t <= halfSupport; t++) {
                    int index = tau - t;
                    
                    if (index >= 0 && index < signalLength) {
                        double waveletArg = t / scale;
                        
                        if (complexWavelet != null) {
                            // Use complex wavelet directly
                            ComplexNumber psi = complexWavelet.psiComplex(waveletArg);
                            // Correlation requires conjugate
                            sumReal += signal[index] * psi.real() / sqrtScale;
                            sumImag -= signal[index] * psi.imag() / sqrtScale;  // Conjugate
                        } else {
                            // Real wavelet
                            double psiValue = wavelet.psi(waveletArg) / sqrtScale;
                            sumReal += signal[index] * psiValue;
                        }
                    } else {
                        // Apply boundary handling
                        double boundaryValue = getBoundaryValue(signal, index);
                        double waveletArg = t / scale;
                        
                        if (complexWavelet != null) {
                            ComplexNumber psi = complexWavelet.psiComplex(waveletArg);
                            sumReal += boundaryValue * psi.real() / sqrtScale;
                            sumImag -= boundaryValue * psi.imag() / sqrtScale;
                        } else {
                            double psiValue = wavelet.psi(waveletArg) / sqrtScale;
                            sumReal += boundaryValue * psiValue;
                        }
                    }
                }
                
                coefficients[s][tau] = new ComplexNumber(sumReal, sumImag);
            }
        }
        
        return new ComplexCWTResult(coefficients, scales, wavelet);
    }
    
    /**
     * FFT-based complex analysis.
     */
    private ComplexCWTResult analyzeFFTComplex(double[] signal, double[] scales) {
        int signalLength = signal.length;
        int numScales = scales.length;
        ComplexNumber[][] coefficients = new ComplexNumber[numScales][signalLength];
        
        // Calculate maximum wavelet support across all scales
        int maxWaveletSupport = 0;
        for (double scale : scales) {
            int support = getWaveletSupport(scale);
            maxWaveletSupport = Math.max(maxWaveletSupport, support);
        }
        
        // FFT size for linear convolution
        int minFFTSize = signalLength + maxWaveletSupport - 1;
        int fftSize = nextPowerOfTwo(minFFTSize);
        // For complex analysis, derive complex FFT directly to avoid any real-FFT optimization differences
        Complex[] signalFFT = computeFFTComplexFromReal(signal, fftSize);
        
        for (int s = 0; s < numScales; s++) {
            double scale = scales[s];
            double sqrtScale = config.isNormalizeAcrossScales() ? Math.sqrt(scale) : 1.0;
            
            // Generate scaled wavelet
            Complex[] waveletFFT = computeWaveletFFTComplex(scale, fftSize);
            
            // Multiply in frequency domain
            Complex[] product = new Complex[fftSize];
            for (int i = 0; i < fftSize; i++) {
                product[i] = signalFFT[i].multiply(waveletFFT[i].conjugate());
            }
            
            // Inverse FFT gives complex convolution result
            ComplexNumber[] convResult = ifftComplex(product);
            
            // Extract valid portion with proper offset for linear convolution
            int waveletSupport = getWaveletSupport(scale);
            int halfSupport = getHalfSupport(scale);
            
            for (int t = 0; t < signalLength; t++) {
                int idx = t + halfSupport;
                if (idx < convResult.length) {
                    coefficients[s][t] = new ComplexNumber(
                        convResult[idx].real() / sqrtScale,
                        convResult[idx].imag() / sqrtScale
                    );
                } else {
                    coefficients[s][t] = new ComplexNumber(0, 0);
                }
            }
        }
        
        return new ComplexCWTResult(coefficients, scales, wavelet);
    }

    /**
     * Computes complex FFT of a real signal by building complex buffer and applying complex FFT.
     */
    private Complex[] computeFFTComplexFromReal(double[] signal, int fftSize) {
        double[] real = new double[fftSize];
        double[] imag = new double[fftSize];
        System.arraycopy(signal, 0, real, 0, Math.min(signal.length, fftSize));
        // Use CoreFFT complex FFT
        CoreFFT.fft(real, imag);
        Complex[] out = new Complex[fftSize];
        for (int i = 0; i < fftSize; i++) {
            out[i] = new Complex(real[i], imag[i]);
        }
        return out;
    }

    /**
     * Computes wavelet FFT for complex wavelets using linear placement.
     */
    private Complex[] computeWaveletFFTComplex(double scale, int fftSize) {
        ComplexNumber[] waveletArray = new ComplexNumber[fftSize];
        int halfSupport = getHalfSupport(scale);
        
        ComplexContinuousWavelet complexWavelet = wavelet instanceof ComplexContinuousWavelet cw ? cw : null;
        
        // Initialize array with zeros
        for (int i = 0; i < fftSize; i++) {
            waveletArray[i] = ComplexNumber.ZERO;
        }
        
        // Place wavelet at the start of the array for linear convolution
        for (int i = 0; i <= 2 * halfSupport; i++) {
            if (i < fftSize) {
                double t = (i - halfSupport) / scale;
                if (complexWavelet != null) {
                    waveletArray[i] = complexWavelet.psiComplex(t);
                } else {
                    waveletArray[i] = ComplexNumber.ofReal(wavelet.psi(t));
                }
            }
        }
        
        // Convert to internal Complex type and compute FFT
        Complex[] complexArray = new Complex[fftSize];
        for (int i = 0; i < fftSize; i++) {
            complexArray[i] = new Complex(waveletArray[i].real(), waveletArray[i].imag());
        }
        
        return fftComplex(complexArray);
    }
    
    /**
     * Inverse FFT returning ComplexNumber array.
     */
    private ComplexNumber[] ifftComplex(Complex[] spectrum) {
        // Take conjugate of spectrum
        Complex[] conjugate = new Complex[spectrum.length];
        for (int i = 0; i < spectrum.length; i++) {
            conjugate[i] = spectrum[i].conjugate();
        }
        
        // Forward FFT of conjugate
        Complex[] result = fftComplex(conjugate);
        
        // Take conjugate and scale
        ComplexNumber[] output = new ComplexNumber[spectrum.length];
        double scale = 1.0 / spectrum.length;
        for (int i = 0; i < spectrum.length; i++) {
            output[i] = new ComplexNumber(
                result[i].real * scale,
                -result[i].imag * scale
            );
        }
        
        return output;
    }
    
    /**
     * Simple Hilbert transform using FFT.
     */
    private double[] computeHilbertTransform(double[] signal) {
        if (signal == null || signal.length == 0) {
            throw new IllegalStateException("Cannot compute Hilbert transform of null or empty signal");
        }
        
        try {
            int n = signal.length;
            int fftSize = nextPowerOfTwo(n);
            
            // Compute FFT
            Complex[] fft = computeFFT(signal, fftSize);
            
            // Apply Hilbert filter in frequency domain
            // H(f) = -i*sgn(f) = {-i for f>0, 0 for f=0, i for f<0}
            for (int i = 1; i < fftSize/2; i++) {
                // Positive frequencies: multiply by -i
                double temp = fft[i].real;
                fft[i] = new Complex(fft[i].imag, -temp);
            }
            
            // Negative frequencies: multiply by i
            for (int i = fftSize/2 + 1; i < fftSize; i++) {
                double temp = fft[i].real;
                fft[i] = new Complex(-fft[i].imag, temp);
            }
            
            // DC and Nyquist are zero
            fft[0] = new Complex(0, 0);
            if (fftSize > 1) {
                fft[fftSize/2] = new Complex(0, 0);
            }
            
            // Inverse FFT
            double[] result = ifft(fft);
            
            // Extract valid portion
            double[] hilbert = new double[n];
            System.arraycopy(result, 0, hilbert, 0, n);
            
            return hilbert;
        } catch (Exception e) {
            throw new IllegalStateException("Hilbert transform computation failed for signal of length " + 
                signal.length + ": " + e.getMessage(), e);
        }
    }
    
    // Getters
    
    /**
     * Gets the wavelet used by this transform instance.
     * @return the continuous wavelet
     */
    public ContinuousWavelet getWavelet() {
        return wavelet;
    }
    
    /**
     * Gets the configuration parameters used for this transform.
     * @return the CWT configuration
     */
    public CWTConfig getConfig() {
        return config;
    }
    
    /**
     * Finds the next power of two greater than or equal to n.
     */
    private static int nextPowerOfTwo(int n) {
        if (n <= 1) return 1;
        n--;
        n |= n >> 1;
        n |= n >> 2;
        n |= n >> 4;
        n |= n >> 8;
        n |= n >> 16;
        return n + 1;
    }
    
    /**
     * Gets the wavelet support for a given scale, using cache for efficiency.
     * 
     * @param scale the scale parameter
     * @return the wavelet support size
     */
    private int getWaveletSupport(double scale) {
        // Ensure a minimum practical support to avoid degenerate kernels at
        // very small bandwidths or scales. Use ceil to avoid truncation to 0.
        return waveletSupportCache.computeIfAbsent(scale, s -> {
            double raw = WAVELET_SUPPORT_FACTOR * s * wavelet.bandwidth();
            int support = (int) Math.ceil(raw);
            // Clamp to a small minimum to stabilize power estimates
            return Math.max(16, support);
        });
    }
    
    /**
     * Gets half of the wavelet support for a given scale.
     * This is commonly used for centering wavelets in convolution operations.
     * 
     * @param scale the scale parameter
     * @return half of the wavelet support size
     */
    private int getHalfSupport(double scale) {
        return getWaveletSupport(scale) / 2;
    }
    
    /**
     * Computes FFT of real signal.
     * Automatically uses real-optimized FFT when beneficial.
     */
    private Complex[] computeFFT(double[] signal, int fftSize) {
        if (signal == null || signal.length == 0) {
            throw new IllegalStateException("Cannot compute FFT of null or empty signal");
        }
        if (fftSize <= 0) {
            throw new IllegalArgumentException("FFT size must be positive, got: " + fftSize);
        }
        
        try {
            // Prepare padded signal
            double[] paddedSignal = new double[fftSize];
            System.arraycopy(signal, 0, paddedSignal, 0, Math.min(signal.length, fftSize));
            
            // Use CoreFFT
            double[] result = CoreFFT.rfft(paddedSignal);
            
            // Convert to internal Complex type
            Complex[] X = new Complex[fftSize];
            for (int i = 0; i < fftSize; i++) {
                X[i] = new Complex(result[2 * i], result[2 * i + 1]);
            }
            return X;
        } catch (Exception e) {
            throw new IllegalStateException("FFT computation failed for signal of length " + signal.length + 
                " with FFT size " + fftSize + ": " + e.getMessage(), e);
        }
    }
    
    /**
     * FFT implementation for Complex arrays.
     */
    private Complex[] fftComplex(Complex[] x) {
        if (x == null || x.length == 0) {
            throw new IllegalStateException("Cannot perform FFT on null or empty complex signal");
        }
        
        int n = x.length;
        
        try {
            // Convert Complex array to separate real and imaginary arrays
            double[] real = new double[n];
            double[] imag = new double[n];
            for (int i = 0; i < n; i++) {
                real[i] = x[i].real;
                imag[i] = x[i].imag;
            }
            
            // Use CoreFFT
            CoreFFT.fft(real, imag);
            
            // Convert back to Complex array
            Complex[] result = new Complex[n];
            for (int i = 0; i < n; i++) {
                result[i] = new Complex(real[i], imag[i]);
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Complex FFT computation failed for signal of length " + n + 
                ": " + e.getMessage(), e);
        }
    }
    
    /**
     * Simple complex number class.
     */
    private static class Complex {
        final double real;
        final double imag;
        
        Complex(double real, double imag) {
            this.real = real;
            this.imag = imag;
        }
        
        Complex multiply(Complex other) {
            double r = real * other.real - imag * other.imag;
            double i = real * other.imag + imag * other.real;
            return new Complex(r, i);
        }
        
        Complex conjugate() {
            return new Complex(real, -imag);
        }
    }
}
