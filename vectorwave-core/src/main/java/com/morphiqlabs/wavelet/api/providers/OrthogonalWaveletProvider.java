package com.morphiqlabs.wavelet.api.providers;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import java.util.List;

/**
 * Provider for orthogonal wavelets in the VectorWave library.
 * 
 * <p>This provider registers all standard orthogonal wavelets including:</p>
 * <ul>
 *   <li>Haar wavelet - The simplest orthogonal wavelet</li>
 *   <li>Daubechies wavelets (DB2, DB4) - Compact support with maximum vanishing moments</li>
 *   <li>Symlets (SYM2, SYM3, SYM4) - Near-symmetric modifications of Daubechies wavelets</li>
 *   <li>Coiflets (COIF1, COIF2, COIF3) - Wavelets with vanishing moments for both scaling and wavelet functions</li>
 * </ul>
 * 
 * <p>This provider is automatically discovered by the ServiceLoader mechanism
 * and registered with the WaveletRegistry on application startup.</p>
 * 
 * @see WaveletProvider
 * @see OrthogonalWavelet
 * @see WaveletRegistry
 * @since 1.0.0
 */
public class OrthogonalWaveletProvider implements WaveletProvider {
    /**
     * Creates a provider for orthogonal wavelets.
     */
    public OrthogonalWaveletProvider() {}
    
    @Override
    public List<Wavelet> getWavelets() {
        return List.of(
            new Haar(),
            Daubechies.DB2,
            Daubechies.DB4,
            Symlet.SYM2,
            Symlet.SYM3,
            Symlet.SYM4,
            Coiflet.COIF1,
            Coiflet.COIF2,
            Coiflet.COIF3
        );
    }
}
