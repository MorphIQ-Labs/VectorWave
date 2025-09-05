package com.morphiqlabs.wavelet.api;

/**
 * Base interface for discrete wavelets used in Discrete Wavelet Transform (DWT).
 * This includes both orthogonal and biorthogonal wavelets.
 *
 * <p>Discrete wavelets are characterized by their filter coefficients used
 * for decomposition and reconstruction.</p>
 * 
 * @since 1.0.0
 */
public sealed interface DiscreteWavelet extends Wavelet
        permits OrthogonalWavelet, BiorthogonalWavelet {

    /**
     * Returns the number of vanishing moments of the wavelet.
     * This determines the wavelet's ability to represent polynomials.
     *
     * @return the number of vanishing moments
     */
    int vanishingMoments();

    /**
     * Returns the support width of the wavelet.
     * This is the length of the filter coefficients.
     *
     * @return the support width
     */
    default int supportWidth() {
        return lowPassDecomposition().length;
    }

    /**
     * Indicates if this wavelet has compact support.
     * Most discrete wavelets have compact support.
     *
     * @return true if the wavelet has compact support
     */
    default boolean hasCompactSupport() {
        return true;
    }
}
