package com.morphiqlabs.benchmark.util;

import com.morphiqlabs.wavelet.cwt.ComplexNumber;

/**
 * Minimal FFT helper for benchmarks. Implements radix-2 Cooley–Tukey
 * for complex FFT and a simple real-FFT wrapper. It also supports a
 * Stockham-like path toggled by system property "vectorwave.fft.stockham".
 * This is intentionally lightweight and optimized for clarity over speed.
 */
public final class MinimalFFT {
    private MinimalFFT() {}

    public static void fftOptimized(double[] interleaved, int n, boolean inverse) {
        boolean stockham = Boolean.parseBoolean(System.getProperty("vectorwave.fft.stockham", "false"));
        if (stockham) fftStockham(interleaved, n, inverse);
        else fftCooleyTukey(interleaved, n, inverse);
    }

    public static ComplexNumber[] fftRealOptimized(double[] real) {
        int n = real.length;
        double[] interleaved = new double[2 * n];
        for (int i = 0; i < n; i++) {
            interleaved[2 * i] = real[i];
            interleaved[2 * i + 1] = 0.0;
        }
        fftOptimized(interleaved, n, false);
        ComplexNumber[] out = new ComplexNumber[n];
        for (int i = 0; i < n; i++) {
            out[i] = new ComplexNumber(interleaved[2 * i], interleaved[2 * i + 1]);
        }
        return out;
    }

    // In-place iterative radix-2 Cooley–Tukey with bit-reversal
    private static void fftCooleyTukey(double[] a, int n, boolean inverse) {
        if (n <= 1) return;
        int bits = Integer.numberOfTrailingZeros(n);
        bitReverse(a, n, bits);
        for (int len = 2; len <= n; len <<= 1) {
            double ang = (inverse ? 2 : -2) * Math.PI / len;
            double wlenR = Math.cos(ang), wlenI = Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                double wr = 1.0, wi = 0.0;
                for (int j = 0; j < len / 2; j++) {
                    int i0 = 2 * (i + j);
                    int i1 = 2 * (i + j + len / 2);
                    double ur = a[i0], ui = a[i0 + 1];
                    double vr = a[i1] * wr - a[i1 + 1] * wi;
                    double vi = a[i1] * wi + a[i1 + 1] * wr;
                    a[i0] = ur + vr;
                    a[i0 + 1] = ui + vi;
                    a[i1] = ur - vr;
                    a[i1 + 1] = ui - vi;
                    // w *= wlen
                    double nwr = wr * wlenR - wi * wlenI;
                    double nwi = wr * wlenI + wi * wlenR;
                    wr = nwr; wi = nwi;
                }
            }
        }
        if (inverse) {
            // Normalize
            double invN = 1.0 / n;
            for (int i = 0; i < 2 * n; i++) a[i] *= invN;
        }
    }

    // Simple Stockham autosort-style using ping-pong buffers.
    // For simplicity, this uses the same butterflies but writes to a temporary
    // buffer each stage to avoid a separate bit-reversal pass.
    private static void fftStockham(double[] a, int n, boolean inverse) {
        if (n <= 1) return;
        double[] src = a.clone();
        double[] dst = new double[a.length];
        int stages = Integer.numberOfTrailingZeros(n);
        for (int s = 0; s < stages; s++) {
            int m = 1 << (s + 1);
            int mh = m >> 1;
            double baseAng = (inverse ? 2 : -2) * Math.PI / m;
            for (int k = 0; k < n; k += m) {
                for (int j = 0; j < mh; j++) {
                    double wr = Math.cos(baseAng * j);
                    double wi = Math.sin(baseAng * j);
                    int i0 = k + j;
                    int i1 = k + j + mh;
                    double ur = src[2 * i0];
                    double ui = src[2 * i0 + 1];
                    double vr = src[2 * i1] * wr - src[2 * i1 + 1] * wi;
                    double vi = src[2 * i1] * wi + src[2 * i1 + 1] * wr;
                    // Write results interleaved in dst without bit reversal
                    int o0 = 2 * (k + 2 * j);
                    int o1 = 2 * (k + 2 * j + 1);
                    dst[o0] = ur + vr;      dst[o0 + 1] = ui + vi;
                    dst[o1] = ur - vr;      dst[o1 + 1] = ui - vi;
                }
            }
            // swap
            double[] tmp = src; src = dst; dst = tmp;
        }
        // If stages was odd, results are in src; even -> in dst. Ensure copied to a.
        double[] res = (stages % 2 == 0) ? a : src;
        if (res != a) System.arraycopy(res, 0, a, 0, a.length);
        if (inverse) {
            double invN = 1.0 / n;
            for (int i = 0; i < 2 * n; i++) a[i] *= invN;
        }
    }

    private static void bitReverse(double[] a, int n, int bits) {
        for (int i = 0; i < n; i++) {
            int j = Integer.reverse(i) >>> (32 - bits);
            if (j > i) {
                int i0 = 2 * i, j0 = 2 * j;
                double tr = a[i0]; a[i0] = a[j0]; a[j0] = tr;
                double ti = a[i0 + 1]; a[i0 + 1] = a[j0 + 1]; a[j0 + 1] = ti;
            }
        }
    }
}

