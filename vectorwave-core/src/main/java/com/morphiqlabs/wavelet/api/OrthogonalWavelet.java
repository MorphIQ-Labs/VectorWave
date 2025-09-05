package com.morphiqlabs.wavelet.api;

/**
 * Interface for orthogonal wavelets where the wavelet transform is orthogonal.
 *
 * <p>In orthogonal wavelets, the reconstruction filters are time-reversed
 * versions of the decomposition filters. This provides perfect reconstruction
 * and computational efficiency.</p>
 *
 * <p>Examples include: Haar, Daubechies, Symlets, Coiflets</p>
 * 
 * @since 1.0.0
 */
public sealed interface OrthogonalWavelet extends DiscreteWavelet
        permits Haar, Daubechies, Symlet, Coiflet, DiscreteMeyer, BattleLemarieWavelet {

    /**
     * For orthogonal wavelets, the low-pass reconstruction filter
     * is the same as the decomposition filter (no time reversal needed
     * as it's handled in the convolution operation).
     */
    @Override
    default double[] lowPassReconstruction() {
        return lowPassDecomposition();
    }

    /**
     * For orthogonal wavelets, the high-pass reconstruction filter
     * is the same as the decomposition filter (no time reversal needed
     * as it's handled in the convolution operation).
     */
    @Override
    default double[] highPassReconstruction() {
        return highPassDecomposition();
    }

    @Override
    default WaveletType getType() {
        return WaveletType.ORTHOGONAL;
    }
}
