package com.morphiqlabs.wavelet.performance;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests opt-in behavior for runtime learning (Issue 027).
 * Ensures calibration and persistence default to disabled and that
 * measurement recording is a no-op when calibration is off.
 */
class AdaptivePerformanceEstimatorOptInTest {

    @BeforeAll
    static void clearPerfFlags() {
        // Ensure defaults for this JVM run
        System.setProperty("vectorwave.perf.calibration", "false");
        System.setProperty("vectorwave.perf.persist", "false");
    }

    @AfterEach
    void cleanup() {
        // Keep flags disabled for the rest of the test run
        System.setProperty("vectorwave.perf.calibration", "false");
        System.setProperty("vectorwave.perf.persist", "false");
    }

    @Test
    @DisplayName("Defaults: calibration and persistence disabled; recordMeasurement is no-op")
    void testDefaultsDisabled() {
        AdaptivePerformanceEstimator est = AdaptivePerformanceEstimator.getInstance();
        assertFalse(est.isCalibrationEnabled(), "Calibration should be disabled by default");
        assertFalse(est.isPersistenceEnabled(), "Persistence should be disabled by default");

        int before = est.getTotalMeasurements();
        est.recordMeasurement("MODWT", 2048, 0.05, false);
        int after = est.getTotalMeasurements();
        assertEquals(before, after, "recordMeasurement should be a no-op when calibration is disabled");

        // Predictions should still work
        PredictionResult pred = est.estimateMODWT(1024, "Haar", false);
        assertNotNull(pred);
        assertTrue(pred.estimatedTime() > 0);
    }
}

