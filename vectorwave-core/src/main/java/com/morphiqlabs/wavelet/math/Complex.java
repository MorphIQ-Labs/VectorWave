package com.morphiqlabs.wavelet.math;

/**
 * A record representing a complex number with real and imaginary parts.
 *
 * <p>This immutable record provides basic complex number operations
 * and is used for representing complex-valued wavelet evaluations.</p>
 *
 * @param real the real part
 * @param imaginary the imaginary part
 */
public record Complex(double real, double imaginary) {
    
    /**
     * Creates a new complex number from real and imaginary parts.
     * 
     * @param real the real part
     * @param imaginary the imaginary part
     */
    public Complex {
        // Record constructor validates inputs automatically
    }
    
    /**
     * Creates a complex number with only a real part (imaginary = 0).
     * 
     * @param real the real part
     * @return a new complex number with zero imaginary part
     */
    public static Complex real(double real) {
        return new Complex(real, 0.0);
    }
    
    /**
     * Creates a complex number with only an imaginary part (real = 0).
     * 
     * @param imaginary the imaginary part
     * @return a new complex number with zero real part
     */
    public static Complex imaginary(double imaginary) {
        return new Complex(0.0, imaginary);
    }
    
    /**
     * Returns the magnitude (absolute value) of this complex number.
     * 
     * @return the magnitude sqrt(real^2 + imaginary^2)
     */
    public double magnitude() {
        return Math.sqrt(real * real + imaginary * imaginary);
    }
    
    /**
     * Returns the phase angle of this complex number in radians.
     * 
     * @return the phase angle in radians
     */
    public double phase() {
        return Math.atan2(imaginary, real);
    }
    
    @Override
    public String toString() {
        if (imaginary == 0.0) {
            return String.format("%.6f", real);
        } else if (real == 0.0) {
            return String.format("%.6fi", imaginary);
        } else if (imaginary > 0) {
            return String.format("%.6f + %.6fi", real, imaginary);
        } else {
            return String.format("%.6f - %.6fi", real, -imaginary);
        }
    }
}
