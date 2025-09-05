package com.morphiqlabs.wavelet.api;

/**
 * Interface for biorthogonal wavelets that use different filters for
 * decomposition and reconstruction.
 *
 * <p>Biorthogonal wavelets allow for symmetric filters, which is important
 * for image processing applications to avoid phase distortion. They use
 * dual filter pairs for perfect reconstruction.</p>
 *
 * <p>Examples include: Biorthogonal spline wavelets (bior1.3, bior2.2, etc.)</p>
 */
public non-sealed interface BiorthogonalWavelet extends DiscreteWavelet {

    /**
     * Returns the number of vanishing moments for the dual wavelet.
     * This may differ from the primary wavelet's vanishing moments.
     *
     * @return the number of vanishing moments for the dual
     */
    int dualVanishingMoments();

    /**
     * Returns the order of the spline used in construction (if applicable).
     *
     * @return the spline order, or -1 if not applicable
     */
    default int splineOrder() {
        return -1;
    }

    /**
     * Indicates if the wavelet filters are symmetric.
     * Symmetry is a key advantage of biorthogonal wavelets.
     *
     * @return true if filters are symmetric
     */
    boolean isSymmetric();

    /**
     * Returns the reconstruction filter length, which may differ
     * from the decomposition filter length in biorthogonal wavelets.
     *
     * @return the reconstruction filter length
     */
    default int reconstructionLength() {
        return lowPassReconstruction().length;
    }

    @Override
    default WaveletType getType() {
        return WaveletType.BIORTHOGONAL;
    }
}