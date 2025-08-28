package com.morphiqlabs.wavelet.util;

/**
 * Core FFT implementation for the pure Java 21 module.
 * This provides basic FFT functionality without Vector API optimizations.
 */
public class CoreFFT {
    
    /**
     * Compute FFT of real and imaginary arrays.
     * This is a basic scalar implementation.
     */
    public static void fft(double[] real, double[] imag) {
        int n = real.length;
        if (n == 0 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("Array length must be power of 2");
        }
        
        // Bit reversal
        int halfN = n / 2;
        int j = halfN;
        for (int i = 1; i < n - 1; i++) {
            if (i < j) {
                double tempReal = real[i];
                real[i] = real[j];
                real[j] = tempReal;
                double tempImag = imag[i];
                imag[i] = imag[j];
                imag[j] = tempImag;
            }
            int k = halfN;
            while (k <= j) {
                j -= k;
                k /= 2;
            }
            j += k;
        }
        
        // FFT computation
        for (int len = 2; len <= n; len *= 2) {
            double angle = -2 * Math.PI / len;
            double wlenReal = Math.cos(angle);
            double wlenImag = Math.sin(angle);
            
            for (int i = 0; i < n; i += len) {
                double wReal = 1.0;
                double wImag = 0.0;
                
                for (int j2 = 0; j2 < len / 2; j2++) {
                    int u = i + j2;
                    int v = u + len / 2;
                    
                    double tempReal = real[v] * wReal - imag[v] * wImag;
                    double tempImag = real[v] * wImag + imag[v] * wReal;
                    
                    real[v] = real[u] - tempReal;
                    imag[v] = imag[u] - tempImag;
                    real[u] += tempReal;
                    imag[u] += tempImag;
                    
                    double nextWReal = wReal * wlenReal - wImag * wlenImag;
                    double nextWImag = wReal * wlenImag + wImag * wlenReal;
                    wReal = nextWReal;
                    wImag = nextWImag;
                }
            }
        }
    }
    
    /**
     * Compute inverse FFT.
     */
    public static void ifft(double[] real, double[] imag) {
        int n = real.length;
        // Conjugate the complex numbers
        for (int i = 0; i < n; i++) {
            imag[i] = -imag[i];
        }
        
        // Forward FFT
        fft(real, imag);
        
        // Conjugate and scale
        for (int i = 0; i < n; i++) {
            real[i] /= n;
            imag[i] = -imag[i] / n;
        }
    }
    
    /**
     * Real-valued FFT (for real signals).
     * Returns interleaved real and imaginary parts.
     */
    public static double[] rfft(double[] signal) {
        int n = signal.length;
        double[] real = new double[n];
        double[] imag = new double[n];
        System.arraycopy(signal, 0, real, 0, n);
        
        fft(real, imag);
        
        // Return interleaved for compatibility
        double[] result = new double[n * 2];
        for (int i = 0; i < n; i++) {
            result[i * 2] = real[i];
            result[i * 2 + 1] = imag[i];
        }
        return result;
    }
    
    /**
     * Inverse real-valued FFT.
     */
    public static double[] irfft(double[] interleaved) {
        int n = interleaved.length / 2;
        double[] real = new double[n];
        double[] imag = new double[n];
        
        for (int i = 0; i < n; i++) {
            real[i] = interleaved[i * 2];
            imag[i] = interleaved[i * 2 + 1];
        }
        
        ifft(real, imag);
        return real;
    }
}