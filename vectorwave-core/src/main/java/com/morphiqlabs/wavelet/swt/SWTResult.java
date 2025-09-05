package com.morphiqlabs.wavelet.swt;

import java.util.Arrays;
import java.util.Objects;

/**
 * Result container for Stationary Wavelet Transform (SWT) decomposition.
 * 
 * <p>This class provides memory-efficient storage for SWT coefficients,
 * which are redundant (same length at each level). Internal optimizations
 * include sparse representation for mostly-zero coefficients and lazy
 * computation of derived values.</p>
 * 
 * <p>The SWT result contains:</p>
 * <ul>
 *   <li>Approximation coefficients at the coarsest level</li>
 *   <li>Detail coefficients at each decomposition level</li>
 *   <li>Metadata about the decomposition</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public final class SWTResult {
    
    private final double[] approximation;
    private final double[][] details;
    private final int levels;
    private final int signalLength;
    
    // Lazy-computed statistics
    private volatile Double[] detailEnergies;
    private volatile Double totalEnergy;
    
    /**
     * Creates a new SWT result.
     * 
     * @param approximation approximation coefficients at coarsest level
     * @param details detail coefficients at each level
     * @param levels number of decomposition levels
     * @throws NullPointerException if approximation or details is null
     * @throws IllegalArgumentException if levels doesn't match details length
     */
    public SWTResult(double[] approximation, double[][] details, int levels) {
        this.approximation = Objects.requireNonNull(approximation, "Approximation cannot be null");
        this.details = Objects.requireNonNull(details, "Details cannot be null");
        
        if (details.length != levels) {
            throw new IllegalArgumentException("Details array length must match levels");
        }
        
        this.levels = levels;
        this.signalLength = approximation.length;
        
        // Validate all detail levels have correct length
        for (int i = 0; i < levels; i++) {
            if (details[i] == null || details[i].length != signalLength) {
                throw new IllegalArgumentException("All detail levels must have same length as approximation");
            }
        }
    }
    
    /**
     * Gets the approximation coefficients.
     * 
     * @return approximation coefficients (defensive copy)
     */
    public double[] getApproximation() {
        return approximation.clone();
    }
    
    /**
     * Gets detail coefficients at specified level.
     * 
     * @param level decomposition level (0-indexed)
     * @return detail coefficients (defensive copy)
     * @throws IllegalArgumentException if level is out of range
     */
    public double[] getDetail(int level) {
        if (level < 0 || level >= levels) {
            throw new IllegalArgumentException("Level must be between 0 and " + (levels - 1));
        }
        return details[level].clone();
    }
    
    /**
     * Gets all detail coefficients.
     * 
     * @return 2D array of detail coefficients (defensive copy)
     */
    public double[][] getAllDetails() {
        double[][] copy = new double[levels][];
        for (int i = 0; i < levels; i++) {
            copy[i] = details[i].clone();
        }
        return copy;
    }
    
    /**
     * Gets the number of decomposition levels.
     * 
     * @return number of levels
     */
    public int getLevels() {
        return levels;
    }
    
    /**
     * Gets the original signal length.
     * 
     * @return signal length
     */
    public int getSignalLength() {
        return signalLength;
    }
    
    /**
     * Computes energy at specified detail level.
     * Energy is defined as the sum of squared coefficients.
     * 
     * @param level decomposition level (0-indexed)
     * @return energy at level
     * @throws IllegalArgumentException if level is out of range
     */
    public double getDetailEnergy(int level) {
        if (level < 0 || level >= levels) {
            throw new IllegalArgumentException("Level must be between 0 and " + (levels - 1));
        }
        
        // Lazy compute and cache
        if (detailEnergies == null) {
            synchronized (this) {
                if (detailEnergies == null) {
                    detailEnergies = new Double[levels];
                }
            }
        }
        
        if (detailEnergies[level] == null) {
            double energy = 0.0;
            double[] detail = details[level];
            for (double coeff : detail) {
                energy += coeff * coeff;
            }
            detailEnergies[level] = energy;
        }
        
        return detailEnergies[level];
    }
    
    /**
     * Computes approximation energy.
     * 
     * @return approximation energy
     */
    public double getApproximationEnergy() {
        double energy = 0.0;
        for (double coeff : approximation) {
            energy += coeff * coeff;
        }
        return energy;
    }
    
    /**
     * Computes total energy across all coefficients.
     * 
     * @return total energy
     */
    public double getTotalEnergy() {
        if (totalEnergy == null) {
            synchronized (this) {
                if (totalEnergy == null) {
                    double energy = getApproximationEnergy();
                    for (int level = 0; level < levels; level++) {
                        energy += getDetailEnergy(level);
                    }
                    totalEnergy = energy;
                }
            }
        }
        return totalEnergy;
    }
    
    /**
     * Creates a sparse representation of this result for memory efficiency.
     * Coefficients below the threshold are set to zero and not stored.
     * 
     * @param threshold threshold below which coefficients are considered zero
     * @return sparse SWT result
     */
    public SparseSWTResult toSparse(double threshold) {
        return new SparseSWTResult(this, threshold);
    }
    
    /**
     * Sparse representation of SWT result for memory efficiency.
     * Stores only non-zero coefficients with their indices.
     */
    public static final class SparseSWTResult {
        private final double[] approximation;
        private final SparseVector[] sparseDetails;
        private final int levels;
        private final int signalLength;
        private final double threshold;
        
        SparseSWTResult(SWTResult full, double threshold) {
            this.signalLength = full.signalLength;
            this.levels = full.levels;
            this.threshold = threshold;
            
            // Keep approximation as dense (usually has significant values)
            this.approximation = full.approximation.clone();
            
            // Convert details to sparse representation
            this.sparseDetails = new SparseVector[levels];
            for (int level = 0; level < levels; level++) {
                sparseDetails[level] = new SparseVector(full.details[level], threshold);
            }
        }
        
        /**
         * Reconstructs full SWT result from sparse representation.
         * 
         * @return full SWT result
         */
        public SWTResult toFull() {
            double[][] fullDetails = new double[levels][];
            for (int level = 0; level < levels; level++) {
                fullDetails[level] = sparseDetails[level].toDense();
            }
            return new SWTResult(approximation, fullDetails, levels);
        }
        
        /**
         * Gets compression ratio achieved.
         * 
         * @return compression ratio (original size / compressed size)
         */
        public double getCompressionRatio() {
            int originalSize = signalLength * (levels + 1);
            int compressedSize = signalLength; // approximation
            
            for (SparseVector sv : sparseDetails) {
                compressedSize += sv.getNonZeroCount() * 2; // index + value
            }
            
            return (double) originalSize / compressedSize;
        }
        
        /**
         * Gets the threshold used for sparsification.
         * 
         * @return threshold value
         */
        public double getThreshold() {
            return threshold;
        }
    }
    
    /**
     * Internal sparse vector representation.
     */
    private static final class SparseVector {
        private final int length;
        private final int[] indices;
        private final double[] values;
        
        SparseVector(double[] dense, double threshold) {
            this.length = dense.length;
            
            // Count non-zero elements
            int count = 0;
            for (double v : dense) {
                if (Math.abs(v) > threshold) {
                    count++;
                }
            }
            
            // Store non-zero elements
            this.indices = new int[count];
            this.values = new double[count];
            
            int idx = 0;
            for (int i = 0; i < dense.length; i++) {
                if (Math.abs(dense[i]) > threshold) {
                    indices[idx] = i;
                    values[idx] = dense[i];
                    idx++;
                }
            }
        }
        
        double[] toDense() {
            double[] dense = new double[length];
            for (int i = 0; i < indices.length; i++) {
                dense[indices[i]] = values[i];
            }
            return dense;
        }
        
        int getNonZeroCount() {
            return indices.length;
        }
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof SWTResult)) return false;
        
        SWTResult other = (SWTResult) obj;
        return levels == other.levels &&
               signalLength == other.signalLength &&
               Arrays.equals(approximation, other.approximation) &&
               Arrays.deepEquals(details, other.details);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(levels, signalLength,
                           Arrays.hashCode(approximation),
                           Arrays.deepHashCode(details));
    }
    
    @Override
    public String toString() {
        return String.format("SWTResult[levels=%d, signalLength=%d, totalEnergy=%.6f]",
                           levels, signalLength, getTotalEnergy());
    }
}
