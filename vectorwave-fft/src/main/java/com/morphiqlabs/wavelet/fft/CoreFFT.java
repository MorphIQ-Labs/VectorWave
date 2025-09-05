package com.morphiqlabs.wavelet.fft;

/**
 * Core FFT implementation for the pure Java 21 module.
 * This provides basic FFT functionality without Vector API optimizations.
 */
public class CoreFFT {
    
    /**
     * Thread-local scratch buffers to reduce per-call allocations in rfft/irfft.
     * The returned arrays are never exposed directly to callers; results are copied
     * into fresh arrays when returned, preserving thread-safety and immutability
     * of outputs across calls.
     */
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    // Global twiddle-factor cache for power-of-two sizes (opt-in by default)
    private static final class TwiddleCache {
        private static final java.util.concurrent.ConcurrentHashMap<Integer, TwiddleSet> CACHE = new java.util.concurrent.ConcurrentHashMap<>();
        private static final int MIN_N = Integer.getInteger("vectorwave.fft.twiddleCache.minN", 1024);
        private static final int MAX_N = Integer.getInteger("vectorwave.fft.twiddleCache.maxN", 65536);
        private static final boolean ENABLED = Boolean.parseBoolean(System.getProperty("vectorwave.fft.twiddleCache.enabled", "true"));

        static TwiddleSet getForSize(int n) {
            if (!ENABLED) return null;
            if (n < MIN_N || n > MAX_N) return null;
            if ((n & (n - 1)) != 0) return null;
            return CACHE.computeIfAbsent(n, TwiddleCache::buildForSize);
        }

        private static TwiddleSet buildForSize(int n) {
            int stages = 31 - Integer.numberOfLeadingZeros(n);
            double[][] cos = new double[stages][];
            double[][] sin = new double[stages][];
            for (int stage = 0; stage < stages; stage++) {
                int L = n >> stage;
                int halfL = L >> 1;
                cos[stage] = new double[halfL];
                sin[stage] = new double[halfL];
                int r = 1 << stage;
                double stepAngle = -2.0 * Math.PI * r / n;
                double stepCr = Math.cos(stepAngle);
                double stepCi = Math.sin(stepAngle);
                double wr = 1.0, wi = 0.0;
                for (int j = 0; j < halfL; j++) {
                    cos[stage][j] = wr;
                    sin[stage][j] = wi;
                    double nwr = wr * stepCr - wi * stepCi;
                    double nwi = wr * stepCi + wi * stepCr;
                    wr = nwr; wi = nwi;
                }
            }
            return new TwiddleSet(cos, sin);
        }
    }

    private static final class TwiddleSet {
        final double[][] cos;
        final double[][] sin;
        TwiddleSet(double[][] cos, double[][] sin) { this.cos = cos; this.sin = sin; }
    }

    private static final class Scratch {
        double[] real;
        double[] imag;
        double[] tmpR1;
        double[] tmpI1;
        double[] tmpR2;
        double[] tmpI2;
        double[] twR;
        double[] twI;

        double[] real(int n) {
            if (real == null || real.length < n) real = new double[n];
            return real;
        }

        double[] imag(int n) {
            if (imag == null || imag.length < n) imag = new double[n];
            return imag;
        }

        double[] tmpR1(int n) {
            if (tmpR1 == null || tmpR1.length < n) tmpR1 = new double[n];
            return tmpR1;
        }

        double[] tmpI1(int n) {
            if (tmpI1 == null || tmpI1.length < n) tmpI1 = new double[n];
            return tmpI1;
        }

        double[] tmpR2(int n) {
            if (tmpR2 == null || tmpR2.length < n) tmpR2 = new double[n];
            return tmpR2;
        }

        double[] tmpI2(int n) {
            if (tmpI2 == null || tmpI2.length < n) tmpI2 = new double[n];
            return tmpI2;
        }

        double[] stageTwR(int halfN) {
            if (twR == null || twR.length < halfN) twR = new double[halfN];
            return twR;
        }

        double[] stageTwI(int halfN) {
            if (twI == null || twI.length < halfN) twI = new double[halfN];
            return twI;
        }
    }

    private static boolean isRealOptimizedEnabled() {
        return Boolean.getBoolean("vectorwave.fft.realOptimized");
    }

    private static boolean isStockhamEnabled() {
        return Boolean.getBoolean("vectorwave.fft.stockham");
    }
    
    /**
     * Compute FFT of real and imaginary arrays.
     * This is a basic scalar implementation.
     */
    public static void fft(double[] real, double[] imag) {
        int n = real.length;
        if (n == 0 || (n & (n - 1)) != 0) {
            throw new IllegalArgumentException("Array length must be a power of 2");
        }
        if (n == 1) {
            return; // nothing to do for single element array
        }

        // Special case for n=2
        if (n == 2) {
            double r0 = real[0] + real[1];
            double i0 = imag[0] + imag[1];
            double r1 = real[0] - real[1];
            double i1 = imag[0] - imag[1];
            real[0] = r0; imag[0] = i0;
            real[1] = r1; imag[1] = i1;
            return;
        }

        if (isStockhamEnabled()) {
            // Use Stockham auto-sort kernel (ping-pong, no explicit bit reversal)
            fftStockham(real, imag);
            return;
        }
        // no further small-size specialization
        
        // Bit reversal (Cooley–Tukey path)
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

    // Stockham auto-sort FFT using ping-pong buffers (no explicit bit reversal)
    private static void fftStockham(double[] real, double[] imag) {
        final int n = real.length;
        Scratch s = SCRATCH.get();
        double[] srcR = s.tmpR1(n);
        double[] srcI = s.tmpI1(n);
        double[] dstR = s.tmpR2(n);
        double[] dstI = s.tmpI2(n);

        System.arraycopy(real, 0, srcR, 0, n);
        System.arraycopy(imag, 0, srcI, 0, n);

        // Stockham autosort DIF variant with twiddle caching (global or per-call)
        TwiddleSet globalTw = TwiddleCache.getForSize(n);
        for (int stage = 0, r = 1; (1 << stage) < n; stage++, r <<= 1) {
            int L = n >> stage;      // Current sequence length
            int halfL = L >> 1;      // Half length
            final double[] twr, twi;
            if (globalTw != null && globalTw.cos.length > stage && globalTw.cos[stage].length == halfL) {
                twr = globalTw.cos[stage];
                twi = globalTw.sin[stage];
            } else {
                double[] tr = s.stageTwR(halfL);
                double[] ti = s.stageTwI(halfL);
                double stepAngle = -2.0 * Math.PI * r / n;
                double stepCr = Math.cos(stepAngle);
                double stepCi = Math.sin(stepAngle);
                double wr = 1.0, wi = 0.0;
                for (int j = 0; j < halfL; j++) {
                    tr[j] = wr;
                    ti[j] = wi;
                    double nwr = wr * stepCr - wi * stepCi;
                    double nwi = wr * stepCi + wi * stepCr;
                    wr = nwr; wi = nwi;
                }
                twr = tr; twi = ti;
            }

            for (int k = 0; k < r; k++) { // sequence index
                int base = k * L;
                for (int j = 0; j < halfL; j++) {
                    int idx0 = base + j;
                    int idx1 = idx0 + halfL;

                    double ur = srcR[idx0];
                    double ui = srcI[idx0];
                    double vr = srcR[idx1];
                    double vi = srcI[idx1];

                    double sumR = ur + vr;
                    double sumI = ui + vi;
                    double diffR = ur - vr;
                    double diffI = ui - vi;

                    double wcr = twr[j];
                    double wci = twi[j];
                    double tr = diffR * wcr - diffI * wci;
                    double ti = diffR * wci + diffI * wcr;

                    int o0 = k * halfL + j;
                    int o1 = o0 + (n >> 1);
                    dstR[o0] = sumR;
                    dstI[o0] = sumI;
                    dstR[o1] = tr;
                    dstI[o1] = ti;
                }
            }
            // swap: results of this stage become inputs of the next
            double[] tmpR = srcR; srcR = dstR; dstR = tmpR;
            double[] tmpI = srcI; srcI = dstI; dstI = tmpI;
        }
        // After the final swap, results reside in srcR/srcI
        System.arraycopy(srcR, 0, real, 0, n);
        System.arraycopy(srcI, 0, imag, 0, n);
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
        // Use cached twiddles for inverse by calling fft with inverse twiddles
        // We mimic by using standard fft; twiddle sign is handled via conjugation above
        fft(real, imag);
        
        // Conjugate and scale
        for (int i = 0; i < n; i++) {
            real[i] /= n;
            imag[i] = -imag[i] / n;
        }
    }

    /**
     * In-place FFT on interleaved complex data [re, im, re, im, ...].
     * Length of the complex sequence is {@code n}, the array length is {@code 2*n}.
     */
    public static void fftInterleaved(double[] interleaved, int n) {
        if (interleaved == null || interleaved.length < 2 * n) {
            throw new IllegalArgumentException("Interleaved array length must be 2*n");
        }
        Scratch s = SCRATCH.get();
        double[] real = s.real(n);
        double[] imag = s.imag(n);
        for (int i = 0; i < n; i++) {
            real[i] = interleaved[2 * i];
            imag[i] = interleaved[2 * i + 1];
        }
        fft(real, imag);
        for (int i = 0; i < n; i++) {
            interleaved[2 * i] = real[i];
            interleaved[2 * i + 1] = imag[i];
        }
    }

    /**
     * In-place inverse FFT on interleaved complex data [re, im, re, im, ...].
     * Length of the complex sequence is {@code n}, the array length is {@code 2*n}.
     */
    public static void ifftInterleaved(double[] interleaved, int n) {
        if (interleaved == null || interleaved.length < 2 * n) {
            throw new IllegalArgumentException("Interleaved array length must be 2*n");
        }
        Scratch s = SCRATCH.get();
        double[] real = s.real(n);
        double[] imag = s.imag(n);
        for (int i = 0; i < n; i++) {
            real[i] = interleaved[2 * i];
            imag[i] = interleaved[2 * i + 1];
        }
        ifft(real, imag);
        for (int i = 0; i < n; i++) {
            interleaved[2 * i] = real[i];
            interleaved[2 * i + 1] = imag[i];
        }
    }
    
    /**
     * Real-valued FFT (for real signals).
     * Returns interleaved real and imaginary parts.
     */
    public static double[] rfft(double[] signal) {
        int n = signal.length;
        Scratch s = SCRATCH.get();
        if (n == 0) return new double[0];
        if ((n & (n - 1)) != 0) {
            // Keep same behavior as fft: require power-of-2 sizes
            throw new IllegalArgumentException("Signal length must be power of 2");
        }

        if (!isRealOptimizedEnabled() || n == 1) {
            // Fallback to full complex FFT on real signal
            double[] real = s.real(n);
            double[] imag = s.imag(n);
            System.arraycopy(signal, 0, real, 0, n);
            java.util.Arrays.fill(imag, 0, n, 0.0);
            fft(real, imag);
            double[] result = new double[n * 2];
            for (int i = 0; i < n; i++) {
                result[2 * i] = real[i];
                result[2 * i + 1] = imag[i];
            }
            return result;
        }

        // Real-optimized path: two length-m FFTs on even/odd parts, then combine
        int m = n / 2;
        double[] eR = new double[m];
        double[] eI = new double[m];
        double[] oR = new double[m];
        double[] oI = new double[m];

        for (int j = 0; j < m; j++) {
            eR[j] = signal[2 * j];
            eI[j] = 0.0;
            oR[j] = signal[2 * j + 1];
            oI[j] = 0.0;
        }

        fft(eR, eI);
        fft(oR, oI);

        double[] out = new double[2 * n];

        for (int k = 0; k < m; k++) {
            double c = Math.cos(2.0 * Math.PI * k / n);
            double sVal = Math.sin(2.0 * Math.PI * k / n);
            double wr = c * oR[k] + sVal * oI[k];
            double wi = c * oI[k] - sVal * oR[k];

            double xr = eR[k] + wr;
            double xi = eI[k] + wi;
            out[2 * k] = xr;
            out[2 * k + 1] = xi;

            double xr2 = eR[k] - wr;
            double xi2 = eI[k] - wi;
            int idx = 2 * (k + m);
            out[idx] = xr2;
            out[idx + 1] = xi2;
        }

        return out;
    }
    
    /**
     * Inverse real-valued FFT.
     */
    public static double[] irfft(double[] interleaved) {
        int n = interleaved.length / 2;
        Scratch s = SCRATCH.get();
        double[] real = s.real(n);
        double[] imag = s.imag(n);

        for (int i = 0; i < n; i++) {
            real[i] = interleaved[i * 2];
            imag[i] = interleaved[i * 2 + 1];
        }

        ifft(real, imag);
        // Copy result into a fresh array to ensure immutability for callers
        double[] out = new double[n];
        System.arraycopy(real, 0, out, 0, n);
        return out;
    }

    /**
     * Inverse FFT without 1/N scaling (internal use for code paths that apply custom scaling).
     */
    public static void ifftNoScale(double[] real, double[] imag) {
        int n = real.length;
        for (int i = 0; i < n; i++) imag[i] = -imag[i];
        fft(real, imag);
        for (int i = 0; i < n; i++) imag[i] = -imag[i];
    }

    /**
     * Inverse FFT on interleaved complex data without 1/N scaling (internal use).
     */
    public static void ifftInterleavedNoScale(double[] interleaved, int n) {
        if (interleaved == null || interleaved.length < 2 * n) {
            throw new IllegalArgumentException("Interleaved array length must be 2*n");
        }
        Scratch s = SCRATCH.get();
        double[] real = s.real(n);
        double[] imag = s.imag(n);
        for (int i = 0; i < n; i++) {
            real[i] = interleaved[2 * i];
            imag[i] = interleaved[2 * i + 1];
        }
        ifftNoScale(real, imag);
        for (int i = 0; i < n; i++) {
            interleaved[2 * i] = real[i];
            interleaved[2 * i + 1] = imag[i];
        }
    }
} 
