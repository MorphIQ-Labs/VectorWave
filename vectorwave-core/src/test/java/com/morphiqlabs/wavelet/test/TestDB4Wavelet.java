package com.morphiqlabs.wavelet.test;

import com.morphiqlabs.wavelet.api.BiorthogonalWavelet;
import com.morphiqlabs.wavelet.api.WaveletType;

/**
 * Test wavelet with exactly 4 filter taps to trigger DB4 optimization path.
 */
public class TestDB4Wavelet implements BiorthogonalWavelet {
    private final double[] lowPass = new double[] {0.48296, 0.83651, 0.22414, -0.12940};
    private final double[] highPass = new double[] {-0.12940, -0.22414, 0.83651, -0.48296};
    
    @Override
    public double[] lowPassDecomposition() { 
        return lowPass; 
    }
    
    @Override
    public double[] highPassDecomposition() { 
        return highPass; 
    }
    
    @Override
    public double[] lowPassReconstruction() { 
        return lowPass; 
    }
    
    @Override
    public double[] highPassReconstruction() { 
        return highPass; 
    }
    
    @Override
    public String name() { 
        return "TestDB4"; 
    }
    
    @Override
    public WaveletType getType() { 
        return WaveletType.ORTHOGONAL; 
    }
    
    @Override
    public int vanishingMoments() {
        return 4;
    }
    
    @Override
    public int dualVanishingMoments() {
        return 4;
    }
    
    @Override
    public boolean isSymmetric() {
        return false;
    }
}