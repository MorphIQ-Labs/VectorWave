package com.morphiqlabs.wavelet.util;

/**
 * Centralized FFT heuristics for MODWT and related operations.
 *
 * <p>Exposes configurable thresholds (via setters or system properties)
 * to decide when to use FFT-based convolution paths.</p>
 */
public final class FftHeuristics {

    // Defaults derived from Issue 009 heuristic
    private static final int DEFAULT_MIN_N_FOR_MODWT_FFT = 1024;
    private static final double DEFAULT_MIN_FILTER_TO_SIGNAL_RATIO = 1.0 / 8.0; // L > N/8

    // System property keys for optional overrides
    public static final String PROP_MODWT_MIN_N = "vectorwave.fft.modwt.minN";
    public static final String PROP_MODWT_MIN_FILTER_RATIO = "vectorwave.fft.modwt.minFilterToSignalRatio";

    private static volatile int minNForModwtFft = getIntProperty(PROP_MODWT_MIN_N, DEFAULT_MIN_N_FOR_MODWT_FFT);
    private static volatile double minFilterToSignalRatio = getDoubleProperty(PROP_MODWT_MIN_FILTER_RATIO, DEFAULT_MIN_FILTER_TO_SIGNAL_RATIO);

    private FftHeuristics() { throw new AssertionError("No instances"); }

    /**
     * Returns whether FFT-based circular convolution should be used for MODWT.
     *
     * @param signalLength signal length N
     * @param filterLength effective filter length L
     */
    public static boolean shouldUseModwtFFT(int signalLength, int filterLength) {
        if (signalLength <= 0 || filterLength <= 0) return false;
        if (signalLength < minNForModwtFft) return false;
        return filterLength > signalLength * minFilterToSignalRatio;
    }

    /**
     * Gets the minimum N threshold for MODWT FFT decision.
     */
    public static int getMinNForModwtFFT() {
        return minNForModwtFft;
    }

    /**
     * Sets the minimum N threshold for MODWT FFT decision (must be >= 1).
     */
    public static void setMinNForModwtFFT(int minN) {
        if (minN < 1) throw new IllegalArgumentException("minN must be >= 1");
        minNForModwtFft = minN;
    }

    /**
     * Gets the minimum filter-to-signal ratio for MODWT FFT decision.
     */
    public static double getMinFilterToSignalRatio() {
        return minFilterToSignalRatio;
    }

    /**
     * Sets the minimum filter-to-signal ratio for MODWT FFT decision (0 {@literal <} ratio {@literal <=} 1).
     */
    public static void setMinFilterToSignalRatio(double ratio) {
        if (!(ratio > 0.0 && ratio <= 1.0)) {
            throw new IllegalArgumentException("ratio must be in (0, 1]");
        }
        minFilterToSignalRatio = ratio;
    }

    /**
     * Resets thresholds to defaults (and re-reads system property overrides).
     */
    public static void resetToDefaults() {
        minNForModwtFft = getIntProperty(PROP_MODWT_MIN_N, DEFAULT_MIN_N_FOR_MODWT_FFT);
        minFilterToSignalRatio = getDoubleProperty(PROP_MODWT_MIN_FILTER_RATIO, DEFAULT_MIN_FILTER_TO_SIGNAL_RATIO);
    }

    private static int getIntProperty(String key, int def) {
        try {
            String v = System.getProperty(key);
            if (v == null) return def;
            int parsed = Integer.parseInt(v.trim());
            return parsed >= 1 ? parsed : def;
        } catch (Exception e) {
            return def;
        }
    }

    private static double getDoubleProperty(String key, double def) {
        try {
            String v = System.getProperty(key);
            if (v == null) return def;
            double parsed = Double.parseDouble(v.trim());
            return (parsed > 0.0 && parsed <= 1.0) ? parsed : def;
        } catch (Exception e) {
            return def;
        }
    }
}
