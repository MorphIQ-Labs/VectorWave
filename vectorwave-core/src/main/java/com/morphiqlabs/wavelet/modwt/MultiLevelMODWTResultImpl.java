package com.morphiqlabs.wavelet.modwt;

import java.util.Arrays;

/**
 * Implementation of MultiLevelMODWTResult with defensive copying and validation.
 */
class MultiLevelMODWTResultImpl implements MultiLevelMODWTResult {
    
    private final int signalLength;
    private final int levels;
    private final double[][] detailCoeffs;
    private double[] approximationCoeffs;
    
    // Cached values
    private Double totalEnergy;
    private double[] relativeEnergyDistribution;
    
    /**
     * Creates a new multi-level MODWT result container.
     * 
     * @param signalLength original signal length
     * @param levels number of decomposition levels
     */
    MultiLevelMODWTResultImpl(int signalLength, int levels) {
        if (signalLength <= 0) {
            throw new IllegalArgumentException("Signal length must be positive");
        }
        if (levels <= 0) {
            throw new IllegalArgumentException("Number of levels must be positive");
        }
        
        this.signalLength = signalLength;
        this.levels = levels;
        this.detailCoeffs = new double[levels][];
    }
    
    /**
     * Sets detail coefficients for a specific level.
     * 
     * @param level the level (1-based)
     * @param coeffs the detail coefficients
     */
    void setDetailCoeffsAtLevel(int level, double[] coeffs) {
        validateLevel(level);
        validateCoefficients(coeffs, "detail coefficients at level " + level);
        this.detailCoeffs[level - 1] = coeffs.clone();
        clearCache();
    }
    
    /**
     * Sets the approximation coefficients.
     * 
     * @param coeffs the approximation coefficients
     */
    void setApproximationCoeffs(double[] coeffs) {
        validateCoefficients(coeffs, "approximation coefficients");
        this.approximationCoeffs = coeffs.clone();
        clearCache();
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
    public double[] getApproximationCoeffs() {
        if (approximationCoeffs == null) {
            throw new IllegalStateException("Approximation coefficients not set");
        }
        return approximationCoeffs.clone();
    }
    
    @Override
    public double getDetailEnergyAtLevel(int level) {
        validateLevel(level);
        double[] coeffs = detailCoeffs[level - 1];
        if (coeffs == null) {
            return 0.0;
        }
        return computeEnergy(coeffs);
    }
    
    @Override
    public double getApproximationEnergy() {
        if (approximationCoeffs == null) {
            return 0.0;
        }
        return computeEnergy(approximationCoeffs);
    }
    
    @Override
    public double getTotalEnergy() {
        if (totalEnergy == null) {
            double total = getApproximationEnergy();
            for (int level = 1; level <= levels; level++) {
                total += getDetailEnergyAtLevel(level);
            }
            totalEnergy = total;
        }
        return totalEnergy;
    }
    
    @Override
    public double[] getRelativeEnergyDistribution() {
        if (relativeEnergyDistribution == null) {
            double total = getTotalEnergy();
            if (total == 0.0) {
                relativeEnergyDistribution = new double[levels + 1];
            } else {
                relativeEnergyDistribution = new double[levels + 1];
                
                // Approximation energy
                relativeEnergyDistribution[0] = getApproximationEnergy() / total;
                
                // Detail energies
                for (int level = 1; level <= levels; level++) {
                    relativeEnergyDistribution[level] = getDetailEnergyAtLevel(level) / total;
                }
            }
        }
        return relativeEnergyDistribution.clone();
    }
    
    @Override
    public MultiLevelMODWTResult copy() {
        MultiLevelMODWTResultImpl copy = new MultiLevelMODWTResultImpl(signalLength, levels);
        
        // Copy detail coefficients
        for (int level = 1; level <= levels; level++) {
            if (detailCoeffs[level - 1] != null) {
                copy.setDetailCoeffsAtLevel(level, detailCoeffs[level - 1]);
            }
        }
        
        // Copy approximation coefficients
        if (approximationCoeffs != null) {
            copy.setApproximationCoeffs(approximationCoeffs);
        }
        
        return copy;
    }
    
    @Override
    public boolean isValid() {
        // Check approximation coefficients
        if (approximationCoeffs == null || approximationCoeffs.length != signalLength) {
            return false;
        }
        if (!isFinite(approximationCoeffs)) {
            return false;
        }
        
        // Check detail coefficients at each level
        for (int level = 1; level <= levels; level++) {
            double[] coeffs = detailCoeffs[level - 1];
            if (coeffs == null || coeffs.length != signalLength) {
                return false;
            }
            if (!isFinite(coeffs)) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Validates level is in valid range.
     */
    private void validateLevel(int level) {
        if (level < 1 || level > levels) {
            throw new IllegalArgumentException(
                "Level " + level + " out of range [1, " + levels + "]");
        }
    }
    
    /**
     * Validates coefficient array.
     */
    private void validateCoefficients(double[] coeffs, String name) {
        if (coeffs == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        if (coeffs.length != signalLength) {
            throw new IllegalArgumentException(
                name + " length (" + coeffs.length + 
                ") must match signal length (" + signalLength + ")");
        }
    }
    
    /**
     * Computes energy (sum of squares) of coefficients.
     */
    private double computeEnergy(double[] coeffs) {
        double energy = 0.0;
        for (double c : coeffs) {
            energy += c * c;
        }
        return energy;
    }
    
    /**
     * Checks if all values in array are finite.
     */
    private boolean isFinite(double[] array) {
        for (double value : array) {
            if (!Double.isFinite(value)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Clears cached values when coefficients change.
     */
    private void clearCache() {
        totalEnergy = null;
        relativeEnergyDistribution = null;
    }
    
    @Override
    public String toString() {
        return String.format("MultiLevelMODWTResult[levels=%d, signalLength=%d, valid=%s]",
            levels, signalLength, isValid());
    }
}