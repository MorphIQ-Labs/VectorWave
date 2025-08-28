package com.morphiqlabs.wavelet;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WaveletOperationsTest {

    @Test
    void softThresholdNullCoefficientsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> WaveletOperations.softThreshold(null, 0.5));
    }

    @Test
    void hardThresholdNullCoefficientsThrows() {
        assertThrows(IllegalArgumentException.class,
                () -> WaveletOperations.hardThreshold(null, 0.5));
    }

    @Test
    void estimateSpeedupNegativeSignalLengthThrows() {
        WaveletOperations.PerformanceInfo info =
                new WaveletOperations.PerformanceInfo(false, "test", "N/A", "");
        assertThrows(IllegalArgumentException.class, () -> info.estimateSpeedup(-1));
    }
}
