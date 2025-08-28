package com.morphiqlabs.wavelet.modwt;

import java.util.Arrays;

/**
 * Mutable implementation of MultiLevelMODWTResult that allows direct coefficient modification.
 * 
 * <p>This implementation extends the existing MultiLevelMODWTResultImpl to add mutability
 * features while preserving the original functionality.</p>
 * 
 * <p><strong>Thread Safety:</strong> This class is not thread-safe. External synchronization
 * is required for concurrent access.</p>
 */
public class MutableMultiLevelMODWTResultImpl implements MutableMultiLevelMODWTResult {
    
    private final int signalLength;
    private final int levels;
    private final double[][] detailCoeffs;
    private double[] approximationCoeffs;
    
    // Cached values
    private Double totalEnergy;
    private double[] relativeEnergyDistribution;
    private Double[] detailEnergies;
    private Double approximationEnergy;
    
    /**
     * Creates a new mutable multi-level MODWT result container.
     * 
     * @param signalLength original signal length
     * @param levels number of decomposition levels
     * @throws IllegalArgumentException if signalLength or levels is not positive
     */
    public MutableMultiLevelMODWTResultImpl(int signalLength, int levels) {
        if (signalLength <= 0) {
            throw new IllegalArgumentException("Signal length must be positive");
        }
        if (levels <= 0) {
            throw new IllegalArgumentException("Number of levels must be positive");
        }
        
        this.signalLength = signalLength;
        this.levels = levels;
        this.detailCoeffs = new double[levels][];
        this.detailEnergies = new Double[levels];
    }
    
    /**
     * Creates a mutable copy from an existing result.
     * 
     * @param source the result to copy from
     * @throws NullPointerException if source is null
     */
    public MutableMultiLevelMODWTResultImpl(MultiLevelMODWTResult source) {
        if (source == null) {
            throw new NullPointerException("Source result cannot be null");
        }
        
        this.signalLength = source.getSignalLength();
        this.levels = source.getLevels();
        this.detailCoeffs = new double[levels][];
        this.detailEnergies = new Double[levels];
        
        // Copy detail coefficients
        for (int level = 1; level <= levels; level++) {
            this.detailCoeffs[level - 1] = source.getDetailCoeffsAtLevel(level).clone();
        }
        
        // Copy approximation coefficients
        this.approximationCoeffs = source.getApproximationCoeffs().clone();
    }
    
    @Override
    public int getLevels() {
        return levels;
    }
    
    @Override
    public int getSignalLength() {
        return signalLength;
    }
    
    @Override
    public double[] getDetailCoeffsAtLevel(int level) {
        validateLevel(level);
        double[] coeffs = detailCoeffs[level - 1];
        if (coeffs == null) {
            throw new IllegalStateException("Detail coefficients not set for level " + level);
        }
        return coeffs.clone();
    }
    
    @Override
    public double[] getMutableDetailCoeffs(int level) {
        validateLevel(level);
        double[] coeffs = detailCoeffs[level - 1];
        if (coeffs == null) {
            throw new IllegalStateException("Detail coefficients not set for level " + level);
        }
        return coeffs;
    }
    
    @Override
    public void setDetailCoeffs(int level, double[] coeffs) {
        validateLevel(level);
        validateCoefficients(coeffs, "detail coefficients at level " + level);
        this.detailCoeffs[level - 1] = coeffs.clone();
        clearCaches();
    }
    
    @Override
    public double[] getApproximationCoeffs() {
        if (approximationCoeffs == null) {
            throw new IllegalStateException("Approximation coefficients not set");
        }
        return approximationCoeffs.clone();
    }
    
    @Override
    public double[] getMutableApproximationCoeffs() {
        if (approximationCoeffs == null) {
            throw new IllegalStateException("Approximation coefficients not set");
        }
        return approximationCoeffs;
    }
    
    @Override
    public void setApproximationCoeffs(double[] coeffs) {
        validateCoefficients(coeffs, "approximation coefficients");
        this.approximationCoeffs = coeffs.clone();
        clearCaches();
    }
    
    @Override
    public void clearCaches() {
        this.totalEnergy = null;
        this.relativeEnergyDistribution = null;
        this.approximationEnergy = null;
        Arrays.fill(this.detailEnergies, null);
    }
    
    @Override
    public double getDetailEnergyAtLevel(int level) {
        validateLevel(level);
        
        // Check cache first
        if (detailEnergies[level - 1] != null) {
            return detailEnergies[level - 1];
        }
        
        double[] coeffs = detailCoeffs[level - 1];
        if (coeffs == null) {
            return 0.0;
        }
        
        double energy = computeEnergy(coeffs);
        detailEnergies[level - 1] = energy;
        return energy;
    }
    
    @Override
    public double getApproximationEnergy() {
        if (approximationEnergy != null) {
            return approximationEnergy;
        }
        
        if (approximationCoeffs == null) {
            return 0.0;
        }
        
        approximationEnergy = computeEnergy(approximationCoeffs);
        return approximationEnergy;
    }
    
    @Override
    public double getTotalEnergy() {
        if (totalEnergy != null) {
            return totalEnergy;
        }
        
        double total = getApproximationEnergy();
        for (int level = 1; level <= levels; level++) {
            total += getDetailEnergyAtLevel(level);
        }
        
        totalEnergy = total;
        return total;
    }
    
    @Override
    public double[] getRelativeEnergyDistribution() {
        if (relativeEnergyDistribution != null) {
            return relativeEnergyDistribution.clone();
        }
        
        double[] distribution = new double[levels + 1];
        double total = getTotalEnergy();
        
        if (total > 0) {
            // Detail levels
            for (int level = 1; level <= levels; level++) {
                distribution[level - 1] = getDetailEnergyAtLevel(level) / total;
            }
            // Approximation
            distribution[levels] = getApproximationEnergy() / total;
        }
        
        relativeEnergyDistribution = distribution;
        return distribution.clone();
    }
    
    @Override
    public MultiLevelMODWTResult copy() {
        return new MutableMultiLevelMODWTResultImpl(this);
    }
    
    @Override
    public MultiLevelMODWTResult toImmutable() {
        // Create an immutable copy using the standard implementation
        MultiLevelMODWTResultImpl immutable = new MultiLevelMODWTResultImpl(signalLength, levels);
        
        // Copy detail coefficients
        for (int level = 1; level <= levels; level++) {
            if (detailCoeffs[level - 1] != null) {
                immutable.setDetailCoeffsAtLevel(level, detailCoeffs[level - 1]);
            }
        }
        
        // Copy approximation coefficients
        if (approximationCoeffs != null) {
            immutable.setApproximationCoeffs(approximationCoeffs);
        }
        
        return immutable;
    }
    
    @Override
    public boolean isValid() {
        // Check approximation coefficients
        if (approximationCoeffs != null) {
            if (approximationCoeffs.length != signalLength) {
                return false;
            }
            for (double value : approximationCoeffs) {
                if (!Double.isFinite(value)) {
                    return false;
                }
            }
        }
        
        // Check detail coefficients at each level
        for (int level = 1; level <= levels; level++) {
            double[] coeffs = detailCoeffs[level - 1];
            if (coeffs != null) {
                if (coeffs.length != signalLength) {
                    return false;
                }
                for (double value : coeffs) {
                    if (!Double.isFinite(value)) {
                        return false;
                    }
                }
            }
        }
        
        return true;
    }
    
    /**
     * Internal method to set detail coefficients without cloning.
     * Used for initial setup to avoid unnecessary copying.
     */
    void setDetailCoeffsAtLevelDirect(int level, double[] coeffs) {
        validateLevel(level);
        this.detailCoeffs[level - 1] = coeffs;
        clearCaches();
    }
    
    /**
     * Internal method to set approximation coefficients without cloning.
     * Used for initial setup to avoid unnecessary copying.
     */
    void setApproximationCoeffsDirect(double[] coeffs) {
        this.approximationCoeffs = coeffs;
        clearCaches();
    }
    
    private void validateLevel(int level) {
        if (level < 1 || level > levels) {
            throw new IllegalArgumentException(
                "Level must be between 1 and " + levels + ", got: " + level);
        }
    }
    
    private void validateCoefficients(double[] coeffs, String description) {
        if (coeffs == null) {
            throw new NullPointerException(description + " cannot be null");
        }
        if (coeffs.length != signalLength) {
            throw new IllegalArgumentException(
                description + " length must be " + signalLength + ", got: " + coeffs.length);
        }
    }
    
    private static double computeEnergy(double[] coeffs) {
        double energy = 0.0;
        for (double c : coeffs) {
            energy += c * c;
        }
        return energy;
    }
}