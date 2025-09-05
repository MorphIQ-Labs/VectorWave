package com.morphiqlabs.wavelet.api;

/**
 * Reverse Biorthogonal spline wavelets (rbioNr.Nd) family.
 *
 * <p>These are the dual wavelets of the biorthogonal family, where the
 * decomposition and reconstruction filters are swapped compared to BIOR wavelets.</p>
 *
 * <p>In RBIO wavelets:
 * <ul>
 *   <li>Decomposition uses the shorter filter (faster analysis)</li>
 *   <li>Reconstruction uses the longer filter (smoother synthesis)</li>
 *   <li>Useful when reconstruction quality is more important than analysis precision</li>
 * </ul>
 *
 * <p>The naming convention rbioNr.Nd means:
 * <ul>
 *   <li>Nr: order of the spline for reconstruction (same as BIOR)</li>
 *   <li>Nd: order of the spline for decomposition (same as BIOR)</li>
 * </ul>
 * Note: The filters are swapped compared to BIOR, but the naming convention remains the same.
 */
public final class ReverseBiorthogonalSpline implements BiorthogonalWavelet {
    
    /** Reverse Biorthogonal 1.1 wavelet (Haar-like). */
    public static final ReverseBiorthogonalSpline RBIO1_1 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR1_1, "rbio1.1");
    
    /** Reverse Biorthogonal 1.3 wavelet. */
    public static final ReverseBiorthogonalSpline RBIO1_3 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR1_3, "rbio1.3");
    
    /** Reverse Biorthogonal 1.5 wavelet. */
    public static final ReverseBiorthogonalSpline RBIO1_5 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR1_5, "rbio1.5");
    
    /** Reverse Biorthogonal 2.2 wavelet. */
    public static final ReverseBiorthogonalSpline RBIO2_2 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR2_2, "rbio2.2");
    
    /** Reverse Biorthogonal 2.4 wavelet. */
    public static final ReverseBiorthogonalSpline RBIO2_4 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR2_4, "rbio2.4");
    
    /** Reverse Biorthogonal 2.6 wavelet. */
    public static final ReverseBiorthogonalSpline RBIO2_6 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR2_6, "rbio2.6");
    
    /** Reverse Biorthogonal 2.8 wavelet. */
    public static final ReverseBiorthogonalSpline RBIO2_8 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR2_8, "rbio2.8");
    
    /** Reverse Biorthogonal 3.1 wavelet. */
    public static final ReverseBiorthogonalSpline RBIO3_1 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR3_1, "rbio3.1");
    
    /** Reverse Biorthogonal 3.3 wavelet. */
    public static final ReverseBiorthogonalSpline RBIO3_3 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR3_3, "rbio3.3");
    
    /** Reverse Biorthogonal 3.5 wavelet. */
    public static final ReverseBiorthogonalSpline RBIO3_5 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR3_5, "rbio3.5");
    
    /** Reverse Biorthogonal 3.7 wavelet. */
    public static final ReverseBiorthogonalSpline RBIO3_7 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR3_7, "rbio3.7");
    
    /** Reverse Biorthogonal 3.9 wavelet. */
    public static final ReverseBiorthogonalSpline RBIO3_9 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR3_9, "rbio3.9");
    
    /** Reverse Biorthogonal 4.4 wavelet (CDF 9/7). */
    public static final ReverseBiorthogonalSpline RBIO4_4 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR4_4, "rbio4.4");
    
    /** Reverse Biorthogonal 5.5 wavelet. */
    public static final ReverseBiorthogonalSpline RBIO5_5 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR5_5, "rbio5.5");
    
    /** Reverse Biorthogonal 6.8 wavelet. */
    public static final ReverseBiorthogonalSpline RBIO6_8 = new ReverseBiorthogonalSpline(
            BiorthogonalSpline.BIOR6_8, "rbio6.8");
    
    private final BiorthogonalSpline originalBior;
    private final String name;
    
    private ReverseBiorthogonalSpline(BiorthogonalSpline original, String name) {
        this.originalBior = original;
        this.name = name;
    }
    
    @Override
    public String name() {
        return name;
    }
    
    @Override
    public String description() {
        String originalDesc = originalBior.description();
        return originalDesc.replace("Biorthogonal", "Reverse Biorthogonal");
    }
    
    // RBIO swaps the decomposition and reconstruction filters
    
    @Override
    public double[] lowPassDecomposition() {
        // RBIO uses BIOR's reconstruction filter for decomposition
        return originalBior.lowPassReconstruction();
    }
    
    @Override
    public double[] highPassDecomposition() {
        // RBIO uses BIOR's reconstruction high-pass for decomposition
        return originalBior.highPassReconstruction();
    }
    
    @Override
    public double[] lowPassReconstruction() {
        // RBIO uses BIOR's decomposition filter for reconstruction
        return originalBior.lowPassDecomposition();
    }
    
    @Override
    public double[] highPassReconstruction() {
        // RBIO uses BIOR's decomposition high-pass for reconstruction
        return originalBior.highPassDecomposition();
    }
    
    @Override
    public int vanishingMoments() {
        // Vanishing moments are swapped in RBIO
        return originalBior.dualVanishingMoments();
    }
    
    @Override
    public int dualVanishingMoments() {
        // Dual vanishing moments are swapped in RBIO
        return originalBior.vanishingMoments();
    }
    
    @Override
    public int splineOrder() {
        // Spline order remains the same
        return originalBior.splineOrder();
    }
    
    @Override
    public boolean isSymmetric() {
        // Symmetry property is preserved
        return originalBior.isSymmetric();
    }
    
    @Override
    public int reconstructionLength() {
        // In RBIO, reconstruction uses the original decomposition filter
        return originalBior.lowPassDecomposition().length;
    }
    
    @Override
    public WaveletType getType() {
        return WaveletType.BIORTHOGONAL;
    }
    
    /**
     * Get the reconstruction scaling factor.
     * 
     * For RBIO wavelets, the reconstruction scaling is inverted compared to BIOR
     * because the filter roles are swapped. When BIOR uses a scaling factor s
     * for reconstruction, RBIO needs 1/s to maintain perfect reconstruction
     * after the filter swap.
     *
     * @return the reconstruction scaling factor
     */
    public double getReconstructionScale() {
        double biorScale = originalBior.getReconstructionScale();
        // Invert the scaling factor for RBIO
        // Special case: if scale is 1.0, it remains 1.0
        if (Math.abs(biorScale - 1.0) < 1e-10) {
            return 1.0;
        }
        return 1.0 / biorScale;
    }
    
    /**
     * Get the group delay of the wavelet.
     * 
     * The group delay is recalculated for RBIO wavelets because the filter
     * lengths are swapped. The delay is determined by the combined length
     * of the analysis and synthesis filters.
     * 
     * Formula: delay = (length(h0_decomp) - 1)/2 + (length(h0_recon) - 1)/2 - 1
     *
     * @return the group delay in samples
     */
    public int getGroupDelay() {
        // Calculate delay for the swapped filter configuration
        int decompLength = lowPassDecomposition().length;
        int reconLength = lowPassReconstruction().length;
        
        // Apply the standard group delay formula
        int delay = ((decompLength - 1) + (reconLength - 1)) / 2 - 1;
        
        // Ensure non-negative delay
        return Math.max(0, delay);
    }
    
    /**
     * Get the original BIOR wavelet this RBIO is based on.
     *
     * @return the original BiorthogonalSpline wavelet
     */
    public BiorthogonalSpline getOriginalBior() {
        return originalBior;
    }
}
