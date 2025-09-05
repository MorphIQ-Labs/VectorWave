package com.morphiqlabs.wavelet.cwt.finance.providers;

import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.WaveletProvider;
import com.morphiqlabs.wavelet.cwt.finance.*;
import java.util.List;

/**
 * Provider for financial analysis wavelets in the VectorWave library.
 * 
 * <p>This provider registers specialized wavelets optimized for financial time series
 * analysis, market pattern detection, and economic signal processing. These wavelets
 * have been specifically chosen for their effectiveness in analyzing financial data
 * characteristics such as volatility clusters, trend changes, and market crashes.</p>
 * 
 * <p>Wavelets provided:</p>
 * <ul>
 *   <li><strong>Paul wavelet (order 4)</strong> - Asymmetric complex wavelet ideal for 
 *       detecting market crashes, recoveries, and directional price movements. Its 
 *       asymmetry makes it particularly sensitive to sharp rises and falls.</li>
 *   <li><strong>DOG wavelet (order 2)</strong> - Second derivative of Gaussian, excellent 
 *       for edge detection and volatility analysis. Commonly used for identifying price 
 *       breakouts and support/resistance levels.</li>
 *   <li><strong>Shannon-Gabor wavelet</strong> - Provides optimal time-frequency 
 *       localization for analyzing periodic market cycles and oscillations. Useful for 
 *       identifying trading ranges and cyclic patterns.</li>
 *   <li><strong>Classical Shannon wavelet</strong> - Band-limited wavelet for analyzing 
 *       specific frequency components in financial data. Ideal for filtering market noise 
 *       and extracting trend components.</li>
 * </ul>
 * 
 * <p>These wavelets integrate with the FinancialWaveletAnalyzer for comprehensive
 * market analysis including volatility regime detection, trend analysis, and risk
 * metrics calculation.</p>
 * 
 * <p>This provider is automatically discovered by the ServiceLoader mechanism
 * and registered with the WaveletRegistry on application startup.</p>
 * 
 * @see WaveletProvider
 * @see ComplexContinuousWavelet
 * @see FinancialWaveletAnalyzer
 * @see WaveletRegistry
 */
public class FinancialWaveletProvider implements WaveletProvider {
    
    @Override
    public List<Wavelet> getWavelets() {
        return List.of(
            new PaulWavelet(),
            new DOGWavelet(),
            new ShannonGaborWavelet(),
            new ClassicalShannonWavelet()
        );
    }
}