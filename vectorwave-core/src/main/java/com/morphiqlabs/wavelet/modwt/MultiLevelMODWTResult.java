package com.morphiqlabs.wavelet.modwt;

/**
 * Result of a multi-level MODWT (Maximal Overlap Discrete Wavelet Transform) decomposition.
 * 
 * <p>Unlike standard multi-level DWT results, MODWT preserves the original signal length
 * at each decomposition level, providing a redundant but shift-invariant representation.</p>
 * 
 * <p><strong>Structure:</strong></p>
 * <ul>
 *   <li>Detail coefficients at each level (all same length as original signal)</li>
 *   <li>Approximation coefficients at the coarsest level</li>
 *   <li>Energy distribution across scales</li>
 * </ul>
 * 
 * <p><strong>Key differences from DWT results:</strong></p>
 * <ul>
 *   <li>All coefficient arrays have the same length (no decimation)</li>
 *   <li>Direct time alignment with original signal</li>
 *   <li>Redundant representation (more memory required)</li>
 * </ul>
 * 
 * @since 1.0.0
 */
public interface MultiLevelMODWTResult {
    
    /**
     * Gets the number of decomposition levels.
     * 
     * @return number of levels
     */
    int getLevels();
    
    /**
     * Gets the original signal length.
     * 
     * @return signal length
     */
    int getSignalLength();
    
    /**
     * Gets the detail coefficients at a specific level.
     * 
     * @param level the decomposition level (1 = finest details)
     * @return detail coefficients array (same length as original signal)
     * @throws IllegalArgumentException if level is out of range
     */
    double[] getDetailCoeffsAtLevel(int level);
    
    /**
     * Gets the approximation coefficients at the coarsest level.
     * 
     * @return approximation coefficients (same length as original signal)
     */
    double[] getApproximationCoeffs();
    
    /**
     * Computes the energy (sum of squares) of detail coefficients at a level.
     * 
     * @param level the decomposition level
     * @return energy value
     * @throws IllegalArgumentException if level is out of range
     */
    double getDetailEnergyAtLevel(int level);
    
    /**
     * Computes the energy of the approximation coefficients.
     * 
     * @return energy value
     */
    double getApproximationEnergy();
    
    /**
     * Gets the total energy across all levels (should equal signal energy).
     * 
     * @return total energy
     */
    double getTotalEnergy();
    
    /**
     * Gets the relative energy distribution across levels.
     * 
     * @return array of relative energies (sums to 1.0)
     */
    double[] getRelativeEnergyDistribution();
    
    /**
     * Creates a copy of this result with defensive copying of arrays.
     * 
     * @return deep copy of the result
     */
    MultiLevelMODWTResult copy();
    
    /**
     * Checks if the result contains valid data.
     * 
     * @return true if all coefficients are finite and properly sized
     */
    boolean isValid();
}
