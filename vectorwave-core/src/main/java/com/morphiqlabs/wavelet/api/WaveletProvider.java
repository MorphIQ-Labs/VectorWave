package com.morphiqlabs.wavelet.api;

import java.util.List;

/**
 * Service Provider Interface for wavelet discovery using Java's ServiceLoader mechanism.
 * 
 * <p>This interface enables automatic discovery and registration of wavelets at runtime,
 * eliminating the need for static initialization blocks and reducing circular dependencies.</p>
 * 
 * <p>Implementation classes must:</p>
 * <ul>
 *   <li>Have a public no-args constructor</li>
 *   <li>Be listed in META-INF/services/com.morphiqlabs.wavelet.api.WaveletProvider</li>
 *   <li>Return non-null, non-empty list of wavelets</li>
 * </ul>
 * 
 * <h2>Example Implementation</h2>
 * <pre>{@code
 * public class HaarWaveletProvider implements WaveletProvider {
 *     @Override
 *     public List<Wavelet> getWavelets() {
 *         return List.of(new Haar());
 *     }
 * }
 * }</pre>
 * 
 * <p>This design supports plugin architecture where third-party wavelets can be added
 * without modifying the core library.</p>
 * 
 * @see java.util.ServiceLoader
 * @see WaveletRegistry
 * @since 1.0.0
 */
public interface WaveletProvider {
    
    /**
     * Returns the wavelets provided by this implementation.
     * 
     * <p>Implementations should return immutable lists to prevent external modification.
     * The returned list must not be null or empty.</p>
     * 
     * @return list of wavelets provided by this implementation
     */
    List<Wavelet> getWavelets();
}
