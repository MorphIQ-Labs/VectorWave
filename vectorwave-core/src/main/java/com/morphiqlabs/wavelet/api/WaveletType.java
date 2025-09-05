package com.morphiqlabs.wavelet.api;

/**
 * Categorizes wavelets by their mathematical properties and use cases.
 * This helps users select appropriate wavelets for their specific applications.
 * 
 * @since 1.0.0
 */
public enum WaveletType {
    /**
     * Orthogonal wavelets where the wavelet transform is orthogonal.
     * Examples: Haar, Daubechies, Symlets, Coiflets
     */
    ORTHOGONAL,

    /**
     * Biorthogonal wavelets that allow symmetric filters.
     * Examples: Biorthogonal spline wavelets (bior1.3, bior2.2, etc.)
     */
    BIORTHOGONAL,

    /**
     * Continuous wavelets used for continuous wavelet transform (CWT).
     * Examples: Morlet, Mexican Hat, Gaussian derivatives
     */
    CONTINUOUS,

    /**
     * Discrete wavelets specifically designed for discrete transforms.
     * This is a superset that includes both orthogonal and biorthogonal.
     */
    DISCRETE,

    /**
     * Complex-valued wavelets for analyzing phase information.
     * Examples: Complex Morlet, Dual-tree complex wavelets
     */
    COMPLEX,

    /**
     * Wavelets designed for specific applications or with special properties.
     * Examples: Meyer wavelet, Battle-Lemarie wavelets
     */
    SPECIALIZED
}
