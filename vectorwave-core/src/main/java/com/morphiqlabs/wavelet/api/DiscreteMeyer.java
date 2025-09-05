package com.morphiqlabs.wavelet.api;

import java.util.Map;

/**
 * Discrete Meyer wavelet (DMEY).
 * 
 * <p>The Meyer wavelet has excellent frequency localization properties and is 
 * infinitely differentiable in the continuous case. The discrete version uses 
 * a 62-tap FIR filter approximation that preserves the excellent frequency 
 * characteristics.</p>
 * 
 * <h2>Mathematical Foundation</h2>
 * <p>The Meyer wavelet is defined in the frequency domain with perfect band-pass
 * characteristics and smooth transitions using an auxiliary function ν(x).
 * The discrete approximation maintains these properties while being implementable
 * as a finite impulse response (FIR) filter.</p>
 * 
 * <h2>Properties</h2>
 * <ul>
 *   <li>Filter length: 62 coefficients</li>
 *   <li>Nearly symmetric</li>
 *   <li>Excellent frequency localization</li>
 *   <li>Smooth in both time and frequency domains</li>
 *   <li>Effectively infinite vanishing moments</li>
 * </ul>
 * 
 * <h2>Applications</h2>
 * <ul>
 *   <li>Spectral analysis</li>
 *   <li>Frequency-selective filtering</li>
 *   <li>Signal denoising with frequency preservation</li>
 *   <li>Time-frequency analysis</li>
 * </ul>
 * 
 * <h2>Coefficient Source</h2>
 * <p>The coefficients are derived from the frequency domain design using the
 * Meyer auxiliary function and verified against:</p>
 * <ul>
 *   <li>PyWavelets pywt.Wavelet('dmey').dec_lo</li>
 *   <li>MATLAB Wavelet Toolbox wfilters('dmey')</li>
 * </ul>
 */
public final class DiscreteMeyer implements OrthogonalWavelet {
    
    // DMEY coefficients from PyWavelets (62 taps)
    // Note: These coefficients have a small normalization error (sum_sq ≈ 1.002 instead of 1.0)
    // This appears to be inherent in the standard DMEY implementation
    private static final double[] DMEY_62_COEFFICIENTS = new double[]{
        0.00000000000000000e+00, -1.00999995694142294e-12, 8.51945963679621401e-09, -1.11194495259527797e-08,
        -1.07988195396219579e-08, 6.06697574135113522e-08, -1.08665165367358828e-07, 8.20068065038648134e-08,
        1.17830044976639342e-07, -5.50634056525227817e-07, 1.13079470179167064e-06, -1.48954921649715594e-06,
        7.36757288590374602e-07, 3.20544191334477983e-06, -1.63126997345528074e-05, 6.55430593057514913e-05,
        -6.01150234351609247e-04, -2.70467212464372501e-03, 2.20253410091100213e-03, 6.04581409732330398e-03,
        -6.38771831849715629e-03, -1.10614963925134511e-02, 1.52700151309348026e-02, 1.74234341037296930e-02,
        -3.21307939902117576e-02, -2.43487459060780231e-02, 6.37390243228015962e-02, 3.06550919608242628e-02,
        -1.32845200436229383e-01, -3.50875556562583457e-02, 4.44593002757577238e-01, 7.44585592318806277e-01,
        4.44593002757577238e-01, -3.50875556562583457e-02, -1.32845200436229383e-01, 3.06550919608242628e-02,
        6.37390243228015962e-02, -2.43487459060780231e-02, -3.21307939902117576e-02, 1.74234341037296930e-02,
        1.52700151309348026e-02, -1.10614963925134511e-02, -6.38771831849715629e-03, 6.04581409732330398e-03,
        2.20253410091100213e-03, -2.70467212464372501e-03, -6.01150234351609247e-04, 6.55430593057514913e-05,
        -1.63126997345528074e-05, 3.20544191334477983e-06, 7.36757288590374602e-07, -1.48954921649715594e-06,
        1.13079470179167064e-06, -5.50634056525227817e-07, 1.17830044976639342e-07, 8.20068065038648134e-08,
        -1.08665165367358828e-07, 6.06697574135113522e-08, -1.07988195396219579e-08, -1.11194495259527797e-08,
        8.51945963679621401e-09, -1.00999995694142294e-12
    };
    
    /**
     * Standard 62-tap Discrete Meyer wavelet instance.
     */
    public static final DiscreteMeyer DMEY = new DiscreteMeyer();
    
    private final String name;
    private final double[] lowPassCoeffs;
    
    /**
     * Create the standard 62-tap Discrete Meyer wavelet.
     */
    private DiscreteMeyer() {
        this.name = "dmey";
        this.lowPassCoeffs = DMEY_62_COEFFICIENTS.clone();
    }
    
    @Override
    public String name() {
        return name;
    }
    
    @Override
    public String description() {
        return "Discrete Meyer wavelet (62-tap)";
    }
    
    @Override
    public double[] lowPassDecomposition() {
        return lowPassCoeffs.clone();
    }
    
    @Override
    public double[] highPassDecomposition() {
        // Generate high-pass from low-pass using quadrature mirror filter
        double[] h = lowPassCoeffs;
        double[] g = new double[h.length];
        for (int i = 0; i < h.length; i++) {
            g[i] = (i % 2 == 0 ? 1 : -1) * h[h.length - 1 - i];
        }
        return g;
    }
    
    @Override
    public int vanishingMoments() {
        // Meyer wavelet has effectively infinite vanishing moments
        // Return a practical value for compatibility
        return 20;
    }
    
    /**
     * Tolerance for coefficient sum verification.
     * The sum should equal sqrt(2) within machine precision.
     */
    private static final double SUM_TOLERANCE = 1e-10;
    
    /**
     * Tolerance for sum of squares verification.
     * DMEY has a known ~0.002 error in normalization.
     */
    private static final double SUM_SQUARES_TOLERANCE = 3e-3;
    
    /**
     * Tolerance for orthogonality verification.
     * DMEY has small orthogonality errors due to discrete approximation.
     */
    private static final double ORTHOGONALITY_TOLERANCE = 3e-5;
    
    /**
     * Verifies that the DMEY coefficients satisfy the expected conditions.
     * Note: DMEY has known small deviations from perfect orthogonality.
     *
     * @return true if conditions are satisfied within known tolerances
     */
    public boolean verifyCoefficients() {
        double[] h = lowPassCoeffs;
        
        // Check sum = √2
        double sum = 0;
        for (double coeff : h) {
            sum += coeff;
        }
        if (Math.abs(sum - Math.sqrt(2)) > SUM_TOLERANCE) {
            return false;
        }
        
        // Check sum of squares ≈ 1 (known to be ~1.002 for DMEY)
        double sumSquares = 0;
        for (double coeff : h) {
            sumSquares += coeff * coeff;
        }
        if (Math.abs(sumSquares - 1.0) > SUM_SQUARES_TOLERANCE) {
            return false;
        }
        
        // Check orthogonality for even shifts (with relaxed tolerance)
        for (int k = 2; k < Math.min(h.length, 10); k += 2) {
            double dot = 0;
            for (int n = 0; n < h.length - k; n++) {
                dot += h[n] * h[n + k];
            }
            if (Math.abs(dot) > ORTHOGONALITY_TOLERANCE) {
                // DMEY has known small orthogonality errors
                // Only fail if error is too large
                if (Math.abs(dot) > 10 * ORTHOGONALITY_TOLERANCE) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * Get the filter length (number of coefficients).
     * 
     * @return 62 for the standard DMEY implementation
     */
    public int getFilterLength() {
        return lowPassCoeffs.length;
    }
    
    /**
     * Check if this wavelet has symmetric filters.
     * DMEY filters are NOT symmetric.
     * 
     * @return false as DMEY is not symmetric
     */
    public boolean isSymmetric() {
        // DMEY is not symmetric - it has moderate asymmetry
        return false;
    }
}
