package com.morphiqlabs.wavelet.util;

import com.morphiqlabs.wavelet.cwt.ComplexNumber;
import com.morphiqlabs.wavelet.fft.CoreFFT;
import java.util.Arrays;

/**
 * Signal processing utilities for frequency domain operations.
 * 
 * <p>This class provides high-level signal processing operations including
 * FFT, convolution, windowing, and spectral analysis. It serves as a
 * user-friendly API that delegates complex computations to optimized
 * implementations.</p>
 * 
 * <p>Core FFT computations are performed by {@link CoreFFT}.</p>
 */
public final class SignalProcessor {
    private SignalProcessor() {
        throw new AssertionError("No instances");
    }
    
    
    /**
     * Validates that input data is not null or empty.
     * 
     * @param data input array to validate
     * @param operationName name of the operation for error message
     * @throws IllegalArgumentException if data is null or empty
     */
    private static void validateInput(ComplexNumber[] data, String operationName) {
        if (data == null) {
            throw new IllegalArgumentException(operationName + " input cannot be null");
        }
        if (data.length == 0) {
            throw new IllegalArgumentException(operationName + " input cannot be empty");
        }
    }
    
    /**
     * Validates that input data is not null or empty.
     * 
     * @param data input array to validate
     * @param operationName name of the operation for error message
     * @throws IllegalArgumentException if data is null or empty
     */
    private static void validateInput(double[] data, String operationName) {
        if (data == null) {
            throw new IllegalArgumentException(operationName + " input cannot be null");
        }
        if (data.length == 0) {
            throw new IllegalArgumentException(operationName + " input cannot be empty");
        }
    }
    
    /**
     * Performs forward FFT on complex data.
     * 
     * <p>Note: This method requires power-of-2 input sizes. If your data length
     * is not a power of 2, pad it with zeros to the next power of 2.</p>
     * 
     * @param data complex input/output array
     * @throws IllegalArgumentException if data length is not a power of 2
     */
    public static void fft(ComplexNumber[] data) {
        validateInput(data, "FFT");
        if (!PowerOf2Utils.isPowerOf2(data.length)) {
            int nextPowerOf2 = PowerOf2Utils.nextPowerOf2(data.length);
            throw new IllegalArgumentException("FFT input length must be a power of 2, got: " + data.length +
                ". Consider padding your data to length " + nextPowerOf2 + " with zeros.");
        }
        int n = data.length;
        double[] real = new double[n];
        double[] imag = new double[n];
        for (int i = 0; i < n; i++) {
            real[i] = data[i].real();
            imag[i] = data[i].imag();
        }
        CoreFFT.fft(real, imag);
        for (int i = 0; i < n; i++) {
            data[i] = new ComplexNumber(real[i], imag[i]);
        }
    }
    
    /**
     * Performs inverse FFT on complex data.
     * 
     * <p>Note: This method requires power-of-2 input sizes. If your data length
     * is not a power of 2, pad it with zeros to the next power of 2.</p>
     * 
     * @param data complex input/output array
     * @throws IllegalArgumentException if data length is not a power of 2
     */
    public static void ifft(ComplexNumber[] data) {
        validateInput(data, "IFFT");
        if (!PowerOf2Utils.isPowerOf2(data.length)) {
            int nextPowerOf2 = PowerOf2Utils.nextPowerOf2(data.length);
            throw new IllegalArgumentException("IFFT input length must be a power of 2, got: " + data.length +
                ". Consider padding your data to length " + nextPowerOf2 + " with zeros.");
        }
        int n = data.length;
        double[] real = new double[n];
        double[] imag = new double[n];
        for (int i = 0; i < n; i++) {
            real[i] = data[i].real();
            imag[i] = data[i].imag();
        }
        CoreFFT.ifft(real, imag);
        for (int i = 0; i < n; i++) {
            data[i] = new ComplexNumber(real[i], imag[i]);
        }
    }
    
    /**
     * Performs FFT on real data, returning complex result.
     * 
     * @param real real input data
     * @return complex FFT result
     */
    public static ComplexNumber[] fftReal(double[] real) {
        validateInput(real, "FFT");
        
        int n = PowerOf2Utils.nextPowerOf2(real.length);
        ComplexNumber[] complex = new ComplexNumber[n];
        
        // Copy real data to complex array
        for (int i = 0; i < real.length; i++) {
            complex[i] = new ComplexNumber(real[i], 0);
        }
        for (int i = real.length; i < n; i++) {
            complex[i] = new ComplexNumber(0, 0);
        }
        
        fft(complex);
        return complex;
    }
    
    /**
     * Performs FFT on real data, returning only magnitude spectrum.
     * 
     * @param real real input data
     * @return magnitude spectrum (first half due to symmetry)
     */
    public static double[] fftMagnitude(double[] real) {
        ComplexNumber[] fft = fftReal(real);
        double[] magnitude = new double[fft.length / 2 + 1];
        
        for (int i = 0; i < magnitude.length; i++) {
            magnitude[i] = fft[i].magnitude();
        }
        
        return magnitude;
    }
    
    /**
     * Computes convolution using FFT.
     * 
     * @param a first signal
     * @param b second signal
     * @return convolution result
     */
    public static double[] convolveFFT(double[] a, double[] b) {
        validateInput(a, "Convolution");
        validateInput(b, "Convolution");
        
        int resultSize = a.length + b.length - 1;
        int n = PowerOf2Utils.nextPowerOf2(resultSize);
        
        // Pad signals to next power of 2
        double[] paddedA = Arrays.copyOf(a, n);
        double[] paddedB = Arrays.copyOf(b, n);
        
        // FFT of both signals
        ComplexNumber[] fftA = fftReal(paddedA);
        ComplexNumber[] fftB = fftReal(paddedB);
        
        // Multiply in frequency domain
        ComplexNumber[] product = new ComplexNumber[n];
        for (int i = 0; i < n; i++) {
            product[i] = fftA[i].multiply(fftB[i]);
        }
        
        // Inverse FFT
        ifft(product);
        
        // Extract real part of result
        double[] result = new double[resultSize];
        for (int i = 0; i < resultSize; i++) {
            result[i] = product[i].real();
        }
        
        return result;
    }
    
    
    /**
     * Applies a window function to the signal before FFT.
     */
    public static double[] applyWindow(double[] signal, WindowType window) {
        validateInput(signal, "Window");
        if (window == null) {
            throw new IllegalArgumentException("Window type cannot be null");
        }
        double[] windowed = new double[signal.length];
        int n = signal.length;
        
        switch (window) {
            case HANN -> {
                for (int i = 0; i < n; i++) {
                    double w = 0.5 * (1 - Math.cos(2 * Math.PI * i / (n - 1)));
                    windowed[i] = signal[i] * w;
                }
            }
            case HAMMING -> {
                for (int i = 0; i < n; i++) {
                    double w = 0.54 - 0.46 * Math.cos(2 * Math.PI * i / (n - 1));
                    windowed[i] = signal[i] * w;
                }
            }
            case BLACKMAN -> {
                for (int i = 0; i < n; i++) {
                    double w = 0.42 - 0.5 * Math.cos(2 * Math.PI * i / (n - 1)) 
                              + 0.08 * Math.cos(4 * Math.PI * i / (n - 1));
                    windowed[i] = signal[i] * w;
                }
            }
            case RECTANGULAR -> {
                System.arraycopy(signal, 0, windowed, 0, n);
            }
        }
        
        return windowed;
    }
    
    /**
     * Window function types.
     */
    public enum WindowType {
        RECTANGULAR, HANN, HAMMING, BLACKMAN
    }
    
    // Internal radix-2 implementation removed in favor of unified CoreFFT
    
    /**
     * Pads a complex array with zeros to the next power of 2 length.
     * 
     * <p>This is useful for preparing data for FFT operations which require
     * power-of-2 input sizes.</p>
     * 
     * @param data the complex array to pad
     * @return a new array padded with zeros to the next power of 2 length,
     *         or the original array if it's already a power of 2
     */
    public static ComplexNumber[] padToPowerOf2(ComplexNumber[] data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }
        
        if (PowerOf2Utils.isPowerOf2(data.length)) {
            return data; // Already a power of 2
        }
        
        int nextPowerOf2 = PowerOf2Utils.nextPowerOf2(data.length);
        ComplexNumber[] padded = Arrays.copyOf(data, nextPowerOf2);
        
        // Fill the rest with zeros (copyOf already does this for objects)
        for (int i = data.length; i < nextPowerOf2; i++) {
            padded[i] = ComplexNumber.ZERO;
        }
        
        return padded;
    }
    
}
