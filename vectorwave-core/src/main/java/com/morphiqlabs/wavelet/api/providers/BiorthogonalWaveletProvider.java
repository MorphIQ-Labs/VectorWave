package com.morphiqlabs.wavelet.api.providers;

import com.morphiqlabs.wavelet.api.BiorthogonalSpline;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletProvider;
import java.util.List;

/**
 * Provider for biorthogonal wavelets in the VectorWave library.
 * 
 * <p>This provider registers biorthogonal spline wavelets which use different
 * filters for decomposition and reconstruction, allowing for symmetric filters
 * that are important for image processing applications.</p>
 * 
 * <p><strong>Note</strong>: Biorthogonal wavelets now include automatic phase compensation 
 * to correct for inherent circular shifts. Perfect reconstruction is achieved for simple 
 * signals with PERIODIC boundary mode. Complex signals may have small reconstruction errors, 
 * which is normal behavior for biorthogonal wavelets.</p>
 * 
 * <p>This provider is automatically discovered by the ServiceLoader mechanism
 * and registered with the WaveletRegistry on application startup.</p>
 * 
 * @see com.morphiqlabs.wavelet.api.WaveletProvider
 * @see com.morphiqlabs.wavelet.api.BiorthogonalWavelet
 * @see com.morphiqlabs.wavelet.api.WaveletRegistry
 * @since 1.0.0
 */
public class BiorthogonalWaveletProvider implements WaveletProvider {
    /**
     * Creates a provider for biorthogonal spline wavelets.
     */
    public BiorthogonalWaveletProvider() {}
    
    @Override
    public List<Wavelet> getWavelets() {
        return List.of(
            BiorthogonalSpline.BIOR1_3
        );
    }
}
