package com.morphiqlabs.wavelet.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FftHeuristicsTest {

    @AfterEach
    void reset() {
        // Reset any property-based overrides to avoid test interference
        System.clearProperty(FftHeuristics.PROP_MODWT_MIN_N);
        System.clearProperty(FftHeuristics.PROP_MODWT_MIN_FILTER_RATIO);
        FftHeuristics.resetToDefaults();
    }

    @Test
    @DisplayName("Heuristic: defaults prefer scalar for small N")
    void defaultsPreferScalarForSmallN() {
        int n = 128; // below default 1024 threshold
        int L = 8;   // ratio 0.0625
        assertFalse(FftHeuristics.shouldUseModwtFFT(n, L));
    }

    @Test
    @DisplayName("Heuristic: property overrides can force FFT path")
    void overridesForceFFT() {
        // Force very permissive thresholds
        System.setProperty(FftHeuristics.PROP_MODWT_MIN_N, "1");
        System.setProperty(FftHeuristics.PROP_MODWT_MIN_FILTER_RATIO, "0.01");
        FftHeuristics.resetToDefaults();

        int n = 128; // now above overridden minN
        int L = 8;   // ratio 0.0625 > 0.01
        assertTrue(FftHeuristics.shouldUseModwtFFT(n, L));
    }

    @Test
    @DisplayName("Heuristic: property overrides can force scalar path")
    void overridesForceScalar() {
        // Force extremely strict thresholds
        System.setProperty(FftHeuristics.PROP_MODWT_MIN_N, "100000");
        System.setProperty(FftHeuristics.PROP_MODWT_MIN_FILTER_RATIO, "0.9");
        FftHeuristics.resetToDefaults();

        int n = 4096;
        int L = 32; // ratio ~0.0078
        assertFalse(FftHeuristics.shouldUseModwtFFT(n, L));
    }
}

