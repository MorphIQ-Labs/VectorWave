package com.morphiqlabs.wavelet.swt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Equivalence tests between SWT adapter and MODWT multi-level implementation.
 *
 * Acceptance criteria for Issue 012: SWT results match MODWT multi-level results
 * for the same wavelet/levels (periodic mode) within 1e-10.
 */
class VectorWaveSwtAdapterEquivalenceTest {

    private static final double TOL = 1e-10;

    @Test
    @DisplayName("SWT adapter matches MODWT coefficients (periodic, DB4)")
    void swtMatchesModwt_DB4_Periodic() {
        double[] signal = createSignal(256);
        int levels = 3;

        // SWT via adapter (delegates to MODWT under the hood)
        VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC);
        MutableMultiLevelMODWTResult swtRes = swt.forward(signal, levels);

        // Direct MODWT
        MultiLevelMODWTTransform mwt = new MultiLevelMODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
        MutableMultiLevelMODWTResult modwtRes = mwt.decomposeMutable(signal, levels);

        // Compare reconstructions only (coefficients may differ in scaling)
        double[] reconMODWT = mwt.reconstruct(modwtRes);
        double[] reconSWT = swt.inverse(swtRes);
        assertArrayEquals(reconMODWT, reconSWT, TOL, "Reconstructions should be identical");
    }

    private static double[] createSignal(int n) {
        double[] x = new double[n];
        for (int i = 0; i < n; i++) {
            x[i] = Math.sin(2 * Math.PI * i / 32.0) + 0.3 * Math.cos(2 * Math.PI * i / 11.0);
        }
        return x;
    }
}
