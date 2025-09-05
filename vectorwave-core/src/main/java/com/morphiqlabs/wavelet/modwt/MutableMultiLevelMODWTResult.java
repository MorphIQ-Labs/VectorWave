package com.morphiqlabs.wavelet.modwt;

/**
 * Mutable extension of MultiLevelMODWTResult that allows direct coefficient modification.
 * 
 * <p>This interface is designed for applications that need to manipulate wavelet coefficients
 * directly, such as thresholding for denoising, coefficient filtering, or implementing
 * custom processing algorithms like those used in the Stationary Wavelet Transform (SWT).</p>
 * 
 * <p><strong>Warning:</strong> Direct coefficient modification can break the wavelet
 * representation's mathematical properties. Ensure modifications are appropriate for
 * your use case and consider the impact on reconstruction accuracy.</p>
 * 
 * <p><strong>Thread Safety:</strong> Implementations are not thread-safe. External
 * synchronization is required for concurrent access.</p>
 * 
 * @since 1.0.0
 */
public interface MutableMultiLevelMODWTResult extends MultiLevelMODWTResult {
    
    /**
     * Gets a mutable reference to the detail coefficients at a specific level.
     * 
     * <p>Changes to the returned array directly affect the result. Use with caution.</p>
     * 
     * @param level the decomposition level (1 = finest details)
     * @return mutable detail coefficients array
     * @throws IllegalArgumentException if level is out of range
     */
    double[] getMutableDetailCoeffs(int level);
    
    /**
     * Sets new detail coefficients at a specific level.
     * 
     * <p>The provided array is copied internally to prevent external modifications.</p>
     * 
     * @param level the decomposition level (1 = finest details)
     * @param coeffs new detail coefficients (must match signal length)
     * @throws IllegalArgumentException if level is out of range or coeffs has wrong length
     * @throws NullPointerException if coeffs is null
     */
    void setDetailCoeffs(int level, double[] coeffs);
    
    /**
     * Gets a mutable reference to the approximation coefficients.
     * 
     * <p>Changes to the returned array directly affect the result. Use with caution.</p>
     * 
     * @return mutable approximation coefficients array
     */
    double[] getMutableApproximationCoeffs();
    
    /**
     * Sets new approximation coefficients.
     * 
     * <p>The provided array is copied internally to prevent external modifications.</p>
     * 
     * @param coeffs new approximation coefficients (must match signal length)
     * @throws IllegalArgumentException if coeffs has wrong length
     * @throws NullPointerException if coeffs is null
     */
    void setApproximationCoeffs(double[] coeffs);
    
    /**
     * Clears any cached values (e.g., energy calculations) after coefficient modification.
     * 
     * <p>This method should be called after directly modifying coefficients through
     * getMutableDetailCoeffs() or getMutableApproximationCoeffs() to ensure
     * cached values are recalculated.</p>
     */
    void clearCaches();
    
    /**
     * Applies a threshold to coefficients at a specific level.
     * 
     * <p>This is a convenience method for coefficient thresholding, commonly used
     * in denoising applications.</p>
     * 
     * @param level the decomposition level (0 for approximation, 1+ for details)
     * @param threshold the threshold value
     * @param soft if true, applies soft thresholding; if false, applies hard thresholding
     */
    default void applyThreshold(int level, double threshold, boolean soft) {
        if (level == 0) {
            double[] coeffs = getMutableApproximationCoeffs();
            applyThresholdToArray(coeffs, threshold, soft);
        } else {
            double[] coeffs = getMutableDetailCoeffs(level);
            applyThresholdToArray(coeffs, threshold, soft);
        }
        clearCaches();
    }
    
    /**
     * Helper method to apply thresholding to an array.
     */
    private static void applyThresholdToArray(double[] coeffs, double threshold, boolean soft) {
        for (int i = 0; i < coeffs.length; i++) {
            double absValue = Math.abs(coeffs[i]);
            if (soft) {
                // Soft thresholding: shrink towards zero
                if (absValue > threshold) {
                    coeffs[i] = Math.signum(coeffs[i]) * (absValue - threshold);
                } else {
                    coeffs[i] = 0.0;
                }
            } else {
                // Hard thresholding: set to zero if below threshold
                if (absValue <= threshold) {
                    coeffs[i] = 0.0;
                }
            }
        }
    }
    
    /**
     * Creates an immutable copy of this result.
     * 
     * <p>The returned result cannot be modified and is safe to share across threads.</p>
     * 
     * @return immutable copy of this result
     */
    MultiLevelMODWTResult toImmutable();
}
