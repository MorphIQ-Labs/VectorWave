package com.morphiqlabs.wavelet.extensions.parallel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static com.morphiqlabs.wavelet.extensions.parallel.AdaptiveThresholdTuner.OperationType;

class DebugTunerTest {
    
    private AdaptiveThresholdTuner tuner;
    
    @BeforeEach
    void setUp() {
        tuner = new AdaptiveThresholdTuner();
    }
    
    @Test
    void debugLearningFromFailure() {
        OperationType op = OperationType.CWT_TRANSFORM;
        int inputSize = 3000;
        
        // Get initial threshold
        int initialThreshold = tuner.getAdaptiveThreshold(op, inputSize, 1.0);
        System.out.println("Initial threshold: " + initialThreshold);
        
        // Record multiple failures with this threshold (parallel slower than sequential)
        for (int i = 0; i < 20; i++) {
            tuner.recordMeasurement(op, inputSize, initialThreshold, 10_000_000L, 5_000_000L);
        }
        System.out.println("Recorded 20 failures for threshold " + initialThreshold);
        
        // Future selections should avoid the failed threshold
        int counts = 0;
        System.out.println("\nSelections:");
        for (int i = 0; i < 100; i++) {
            int threshold = tuner.getAdaptiveThreshold(op, inputSize, 1.0);
            if (threshold == initialThreshold) {
                counts++;
                if (counts <= 10 || counts % 10 == 0) {
                    System.out.println("  Selection " + (i+1) + ": picked bad threshold " + initialThreshold + " (count=" + counts + ")");
                }
            }
        }
        
        System.out.println("\nSelected bad threshold " + counts + " times out of 100");
        
        // Should select the failed threshold less often
        assertTrue(counts < 50, "Should avoid failed threshold, got: " + counts);
    }
}
