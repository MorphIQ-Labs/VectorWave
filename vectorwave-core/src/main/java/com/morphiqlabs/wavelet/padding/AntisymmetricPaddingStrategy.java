package com.morphiqlabs.wavelet.padding;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

/**
 * Antisymmetric padding strategy that extends signals by reflection with sign change.
 *
 * <p>This strategy reflects the signal at boundaries with a sign change, preserving
 * odd symmetry properties. It's particularly useful for signals that should maintain
 * antisymmetric properties at boundaries. Ideal for:</p>
 * <ul>
 *   <li>Signals with natural antisymmetry</li>
 *   <li>Preserving derivative continuity at boundaries</li>
 *   <li>Compatibility with antisymmetric wavelets</li>
 *   <li>Reducing Gibbs phenomena at boundaries</li>
 * </ul>
 *
 * <p>Two symmetry types are supported:</p>
 * <ul>
 *   <li><b>WHOLE_POINT</b>: Reflects around boundary point with sign change.
 *       Example: {@code [a,b,c]} → {@code [-c,-b,-a,a,b,c,-c,-b,-a]}</li>
 *   <li><b>HALF_POINT</b>: Reflects between boundary points with sign change.
 *       Example: {@code [a,b,c]} → {@code [-b,-a,a,b,c,-c,-b]}</li>
 * </ul>
 *
 * @param type symmetry type for reflection behavior
 */
public record AntisymmetricPaddingStrategy(SymmetryType type) implements PaddingStrategy {
    
    /**
     * Type of antisymmetric reflection.
     */
    public enum SymmetryType {
        /** Reflect around boundary point (includes boundary in reflection) */
        WHOLE_POINT,
        /** Reflect between boundary points (excludes boundary from reflection) */
        HALF_POINT
    }
    
    /**
     * Creates an antisymmetric padding strategy with default HALF_POINT type.
     */
    public AntisymmetricPaddingStrategy() {
        this(SymmetryType.HALF_POINT);
    }
    
    @Override
    public double[] pad(double[] signal, int targetLength) {
        if (signal == null) {
            throw new InvalidArgumentException("Signal cannot be null");
        }
        if (signal.length == 0) {
            throw new InvalidArgumentException("Signal cannot be empty");
        }
        if (targetLength < signal.length) {
            throw new InvalidArgumentException(
                    "Target length " + targetLength + " must be >= signal length " + signal.length);
        }
        
        if (targetLength == signal.length) {
            return signal.clone();
        }
        
        double[] padded = new double[targetLength];
        int n = signal.length;
        
        // Fill the padded array
        for (int i = 0; i < targetLength; i++) {
            padded[i] = getAntisymmetricValue(signal, i, n);
        }
        
        return padded;
    }
    
    /**
     * Get the value at position i using antisymmetric extension.
     * 
     * @param signal original signal
     * @param i position in extended signal
     * @param n length of original signal
     * @return the antisymmetrically extended value
     */
    private double getAntisymmetricValue(double[] signal, int i, int n) {
        if (i >= 0 && i < n) {
            // Within original signal
            return signal[i];
        }
        
        if (type == SymmetryType.WHOLE_POINT) {
            // Whole-point antisymmetry: reflect around boundary points
            if (i < 0) {
                // Left extension
                int period = 2 * n;
                int idx = (-i - 1) % period;
                if (idx < n) {
                    return -signal[idx]; // Reflected with sign change
                } else {
                    return signal[period - idx - 1]; // Back to original
                }
            } else {
                // Right extension (i >= n)
                int period = 2 * n;
                int offset = i - n;
                int idx = offset % period;
                if (idx < n) {
                    return -signal[n - 1 - idx]; // Reflected with sign change
                } else {
                    return signal[idx - n]; // Back to original
                }
            }
        } else { // HALF_POINT
            // Half-point antisymmetry: reflect between boundary points
            if (i < 0) {
                // Left extension
                int period = 2 * n - 2; // Exclude boundaries from period
                if (n == 1) {
                    return -signal[0]; // Special case for single element
                }
                int idx = -i - 1;
                idx = idx % period;
                if (idx < n - 1) {
                    return -signal[idx + 1]; // Reflected with sign change, skip first
                } else {
                    return signal[period - idx]; // Back to original
                }
            } else {
                // Right extension (i >= n)
                int period = 2 * n - 2; // Exclude boundaries from period
                if (n == 1) {
                    return -signal[0]; // Special case for single element
                }
                int offset = i - n;
                int idx = offset % period;
                if (idx < n - 1) {
                    return -signal[n - 2 - idx]; // Reflected with sign change, skip last
                } else {
                    return signal[idx - n + 2]; // Back to original
                }
            }
        }
    }
    
    @Override
    public String name() {
        return "antisymmetric-" + type.name().toLowerCase().replace('_', '-');
    }
    
    @Override
    public String description() {
        String typeDesc = type == SymmetryType.WHOLE_POINT 
                ? "whole-point reflection" 
                : "half-point reflection";
        return String.format("Antisymmetric padding (%s) - reflects signal with sign change", typeDesc);
    }
}
