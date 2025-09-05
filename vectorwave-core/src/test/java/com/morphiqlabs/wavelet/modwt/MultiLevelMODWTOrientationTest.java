package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MultiLevelMODWTOrientationTest {
    @Test
    void singleLevelPeriodicHaarReconstructs() {
        double[] x = {1,2,3,4};
        MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(new Haar(), BoundaryMode.PERIODIC);
        MultiLevelMODWTResult r = t.decompose(x, 1);
        double[] y = t.reconstruct(r);
        assertArrayEquals(x, y, 1e-12);
    }
}

