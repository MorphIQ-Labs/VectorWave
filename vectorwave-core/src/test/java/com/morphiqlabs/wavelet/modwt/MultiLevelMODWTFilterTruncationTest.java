package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.Daubechies;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for level guard behavior (no truncation) in MultiLevelMODWTTransform.
 */
class MultiLevelMODWTFilterTruncationTest {

    @Test
    void testNoTruncationAtMaxLevel_Periodic_Haar() {
        MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        double[] x = {1,2,3,4};
        int J = t.getMaximumLevels(x.length);
        assertTrue(J >= 1);
        MultiLevelMODWTResult r = t.decompose(x, J);
        double[] xr = t.reconstruct(r);
        assertArrayEquals(x, xr, 1e-10);
    }

    @Test
    void testInvalidLevelsAreRejected_DB4() {
        MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        double[] x = new double[8];
        int J = t.getMaximumLevels(x.length); // expected 0 at N=8 for DB4
        assertEquals(0, J);
        assertThrows(com.morphiqlabs.wavelet.exception.InvalidArgumentException.class, () -> t.decompose(x, 1));
    }
}
