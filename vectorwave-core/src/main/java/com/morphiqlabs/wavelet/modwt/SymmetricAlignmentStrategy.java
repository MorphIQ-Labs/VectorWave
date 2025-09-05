package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Symlet;
import com.morphiqlabs.wavelet.api.Coiflet;

/**
 * Internal strategy for symmetric-boundary centering/orientation in IMODWT.
 *
 * Heuristic mapping (from diagnostic sweep):
 * - Detail branch (G): plus orientation with Δτ_G = 0 consistently minimized RMSE
 *   across Haar and DB4 for N ∈ {129, 257, 512} and multiple levels.
 * - Approx branch (H):
 *   • Haar (L0 = 2): best with plus orientation and Δτ_H = -1
 *   • DB4 (L0 = 8): best with minus orientation and Δτ_H in {0, -1}; we choose -1 for stability
 *
 * This strategy returns a compact decision describing which branch uses
 * plus/minus orientation and which Δτ offsets to apply to τ_j.
 *
 * The cascade still uses per-level upsampled reconstruction filters with 1/√2 scaling.
 */
final class SymmetricAlignmentStrategy {

    static final class Decision {
        final boolean approxPlus;
        final int deltaApprox;
        final boolean detailPlus;
        final int deltaDetail;

        Decision(boolean approxPlus, int deltaApprox, boolean detailPlus, int deltaDetail) {
            this.approxPlus = approxPlus;
            this.deltaApprox = deltaApprox;
            this.detailPlus = detailPlus;
            this.deltaDetail = deltaDetail;
        }
    }

    /**
     * Decide symmetric centering/orientation for a given wavelet and level.
     * Currently depends only on base filter length; can be refined by level if needed.
     */
    static Decision decide(Wavelet wavelet, int level) {
        int baseL0Low = wavelet.lowPassReconstruction().length;

        // Detail: plus orientation; small level-conditional Δτ_G helps tighten worst-case RMSE.
        boolean detailPlus = true;
        int deltaG;

        // Approx branch refinement (tighten RMSE toward 0.70):
        // - Haar (L0=2): plus orientation; Δτ_H = 0 for level 1, −1 for level ≥ 2
        // - DB4 and longer (L0≥8): minus orientation; Δτ_H = 0 on odd levels, −1 on even levels
        // This low-cost rule reduces worst-case phase mismatch across levels/sizes.
        boolean isHaar = (baseL0Low <= 2);
        boolean approxPlus = isHaar;
        int deltaH;
        if (isHaar) {
            deltaG = 0;
            deltaH = (level <= 1) ? 0 : -1;
        } else {
            // Longer asymmetric filters
            approxPlus = false; // minus orientation for DB family >= DB4
            int L0 = baseL0Low;
            // Family-specific tweaks from sweep diagnostics
            if (wavelet == Daubechies.DB6) {
                // DB6: best overall with H:minus dH=-1 | G:plus dG=+1 (large N may prefer dG=0)
                deltaH = (level <= 1) ? 0 : -1;
                deltaG = (level >= 3) ? 1 : 0;
            } else if (wavelet == Daubechies.DB8) {
                // DB8: stable choice H:minus dH=+1 | G:plus dG=+1
                deltaH = (level <= 1) ? 0 : 1;
                deltaG = (level >= 2) ? 1 : 0;
            } else if (wavelet == Symlet.SYM4) {
                // SYM4: near symmetric; detail branch prefers minus orientation with zero offsets
                approxPlus = true;
                detailPlus = false;
                deltaH = 0;
                deltaG = 0;
            } else if (wavelet == Symlet.SYM8) {
                // SYM8: choose stable small positive shifts at coarser levels
                approxPlus = false; // minus gives slightly better centering in sweep
                if (level <= 1) { deltaH = 0; deltaG = 0; }
                else if (level == 2) { deltaH = 1; deltaG = 0; }
                else { deltaH = 1; deltaG = 1; }
            } else if (wavelet == Coiflet.COIF2) {
                // COIF2: H:plus dH=+1 | G:minus dG=0 performed best overall
                approxPlus = true;
                deltaH = (level <= 1) ? 0 : 1;
                detailPlus = false;
                deltaG = 0;
            } else if (wavelet == Coiflet.COIF3) {
                // COIF3: stable mapping favors minus on both with small positive dG
                approxPlus = false; detailPlus = false;
                if (level <= 1) { deltaH = 0; deltaG = 0; }
                else { deltaH = -1; deltaG = 1; }
            } else if (L0 >= 12) { // Other longer families
                // Parity-based small shifts help reduce worst-case RMSE on DB6/DB8
                if (level <= 1) {
                    deltaH = 0;  deltaG = 0;
                } else {
                    boolean even = (level % 2 == 0);
                    deltaH = even ? 0 : -1;
                    deltaG = even ? 0 : -1;
                }
            } else { // DB4 (L0=8)
                if (level <= 1) {
                    deltaH = 0;  deltaG = 0;
                } else if (level == 2) {
                    deltaH = -1; deltaG = 0;
                } else {
                    deltaH = -1; deltaG = 0; // empirical: keep G plus with dG=0
                }
            }
        }

        return new Decision(approxPlus, deltaH, detailPlus, deltaG);
    }
}
