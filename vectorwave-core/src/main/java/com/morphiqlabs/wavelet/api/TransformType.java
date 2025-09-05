package com.morphiqlabs.wavelet.api;

import java.util.Set;
import java.util.EnumSet;

/**
 * Enumeration of available wavelet transform types in VectorWave.
 * Each transform has specific characteristics and use cases.
 * 
 * @since 1.0.0
 */
public enum TransformType {
    /**
     * Maximal Overlap Discrete Wavelet Transform (MODWT).
     * - Shift-invariant (translation-invariant)
     * - Works with any signal length
     * - Non-decimated (redundant representation)
     * - Best for: time series analysis, feature detection, denoising
     */
    MODWT("MODWT", "Maximal Overlap Discrete Wavelet Transform", 
          Set.of(WaveletType.ORTHOGONAL, WaveletType.BIORTHOGONAL)),
    
    /**
     * Stationary Wavelet Transform (SWT).
     * - Shift-invariant
     * - Non-decimated
     * - Implemented via MODWT backend in VectorWave
     * - Best for: denoising, pattern recognition
     */
    SWT("SWT", "Stationary Wavelet Transform",
        Set.of(WaveletType.ORTHOGONAL, WaveletType.BIORTHOGONAL)),
    
    /**
     * Continuous Wavelet Transform (CWT).
     * - Analyzes signals at multiple scales continuously
     * - Provides time-frequency representation
     * - Best for: spectral analysis, singularity detection, financial analysis
     */
    CWT("CWT", "Continuous Wavelet Transform",
        Set.of(WaveletType.CONTINUOUS, WaveletType.COMPLEX)),
    
    /**
     * Multi-level MODWT for hierarchical decomposition.
     * - Decomposes signal into multiple resolution levels
     * - Each level captures different frequency bands
     * - Best for: multi-resolution analysis, long-term trends
     */
    MULTI_LEVEL_MODWT("MultiLevel-MODWT", "Multi-level MODWT",
                      Set.of(WaveletType.ORTHOGONAL, WaveletType.BIORTHOGONAL)),
    
    /**
     * Streaming MODWT for real-time processing.
     * - Processes data in chunks as it arrives
     * - Maintains state between chunks
     * - Best for: real-time signal processing, online denoising
     */
    STREAMING_MODWT("Streaming-MODWT", "Streaming MODWT Transform",
                    Set.of(WaveletType.ORTHOGONAL, WaveletType.BIORTHOGONAL));
    
    private final String code;
    private final String description;
    private final Set<WaveletType> compatibleWaveletTypes;
    
    TransformType(String code, String description, Set<WaveletType> compatibleTypes) {
        this.code = code;
        this.description = description;
        this.compatibleWaveletTypes = EnumSet.copyOf(compatibleTypes);
    }
    
    /**
     * Get the short code for this transform type.
     * @return the transform code
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Get the human-readable description.
     * @return the transform description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Check if this transform is compatible with a given wavelet type.
     * @param waveletType the wavelet type to check
     * @return true if compatible, false otherwise
     */
    public boolean isCompatibleWith(WaveletType waveletType) {
        return compatibleWaveletTypes.contains(waveletType);
    }
    
    /**
     * Get all wavelet types compatible with this transform.
     * @return set of compatible wavelet types
     */
    public Set<WaveletType> getCompatibleWaveletTypes() {
        return EnumSet.copyOf(compatibleWaveletTypes);
    }
    
    @Override
    public String toString() {
        return code;
    }
}
