package com.morphiqlabs.wavelet.extensions.modwt;

import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.BoundaryMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BatchStreamingMODWTAccessorsTest {

    @Test
    public void testMinTailAndPerLevelHist_Haar_L2() {
        try (BatchStreamingMODWT s = new BatchStreamingMODWT.Builder()
                .wavelet(new Haar())
                .boundary(BoundaryMode.SYMMETRIC)
                .levels(2)
                .build()) {
            // Filters prepared lazily; accessor should trigger
            int minTail = s.getMinFlushTailLength();
            assertEquals(1, minTail); // Haar: level1 hist=1, level2 hist=2 -> min=1
            assertEquals(1, s.getHistoryLengthForLevel(1));
            assertEquals(2, s.getHistoryLengthForLevel(2));
        }
    }

    @Test
    public void testMinTailAndPerLevelHist_DB4_L3() {
        try (BatchStreamingMODWT s = new BatchStreamingMODWT.Builder()
                .wavelet(Daubechies.DB4)
                .boundary(BoundaryMode.ZERO_PADDING)
                .levels(3)
                .build()) {
            int minTail = s.getMinFlushTailLength();
            assertEquals(7, minTail); // DB4: L1=7, L2=14, L3=28 -> min=7
            assertEquals(7, s.getHistoryLengthForLevel(1));
            assertEquals(14, s.getHistoryLengthForLevel(2));
            assertEquals(28, s.getHistoryLengthForLevel(3));
        }
    }
}
