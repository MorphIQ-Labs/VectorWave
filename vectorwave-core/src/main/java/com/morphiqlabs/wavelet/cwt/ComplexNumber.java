package com.morphiqlabs.wavelet.cwt;

/**
 * Immutable complex number representation for CWT analysis.
 * 
 * <p>Provides basic complex arithmetic operations needed for
 * complex wavelet transforms and phase analysis.</p>
 *
 * @param real the real component
 * @param imag the imaginary component
 */
public record ComplexNumber(double real, double imag) {
    
    /**
     * Complex zero constant.
     */
    public static final ComplexNumber ZERO = new ComplexNumber(0, 0);
    
    /**
     * Complex one constant.
     */
    public static final ComplexNumber ONE = new ComplexNumber(1, 0);
    
    /**
     * Complex i constant.
     */
    public static final ComplexNumber I = new ComplexNumber(0, 1);
    
    /**
     * Creates a complex number from polar coordinates.
     * 
     * @param magnitude the magnitude (modulus)
     * @param phase the phase angle in radians
     * @return complex number
     */
    public static ComplexNumber fromPolar(double magnitude, double phase) {
        return new ComplexNumber(
            magnitude * Math.cos(phase),
            magnitude * Math.sin(phase)
        );
    }
    
    /**
     * Creates a real-valued complex number.
     * 
     * @param real the real value
     * @return complex number with zero imaginary part
     */
    public static ComplexNumber ofReal(double real) {
        return new ComplexNumber(real, 0);
    }
    
    /**
     * Creates a purely imaginary complex number.
     * 
     * @param imag the imaginary value
     * @return complex number with zero real part
     */
    public static ComplexNumber ofImaginary(double imag) {
        return new ComplexNumber(0, imag);
    }
    
    /**
     * Computes the magnitude (modulus) of this complex number.
     * 
     * @return |z| = sqrt(real² + imag²)
     */
    public double magnitude() {
        return Math.sqrt(real * real + imag * imag);
    }
    
    /**
     * Computes the phase (argument) of this complex number.
     * 
     * @return arg(z) in radians, range [-π, π]
     */
    public double phase() {
        return Math.atan2(imag, real);
    }
    
    /**
     * Computes the complex conjugate.
     * 
     * @return z* = real - i*imag
     */
    public ComplexNumber conjugate() {
        return new ComplexNumber(real, -imag);
    }
    
    /**
     * Adds another complex number to this one.
     * 
     * @param other the other complex number
     * @return this + other
     */
    public ComplexNumber add(ComplexNumber other) {
        return new ComplexNumber(
            real + other.real,
            imag + other.imag
        );
    }
    
    /**
     * Subtracts another complex number from this one.
     * 
     * @param other the other complex number
     * @return this - other
     */
    public ComplexNumber subtract(ComplexNumber other) {
        return new ComplexNumber(
            real - other.real,
            imag - other.imag
        );
    }
    
    /**
     * Multiplies this complex number by another.
     * 
     * @param other the other complex number
     * @return this * other
     */
    public ComplexNumber multiply(ComplexNumber other) {
        return new ComplexNumber(
            real * other.real - imag * other.imag,
            real * other.imag + imag * other.real
        );
    }
    
    /**
     * Multiplies this complex number by a real scalar.
     * 
     * @param scalar the real scalar
     * @return this * scalar
     */
    public ComplexNumber multiply(double scalar) {
        return new ComplexNumber(real * scalar, imag * scalar);
    }
    
    /**
     * Divides this complex number by another.
     * 
     * @param other the other complex number
     * @return this / other
     * @throws ArithmeticException if other is zero
     */
    public ComplexNumber divide(ComplexNumber other) {
        double denominator = other.real * other.real + other.imag * other.imag;
        if (denominator == 0) {
            throw new ArithmeticException("Division by zero");
        }
        
        return new ComplexNumber(
            (real * other.real + imag * other.imag) / denominator,
            (imag * other.real - real * other.imag) / denominator
        );
    }
    
    /**
     * Computes the complex exponential e^z.
     * 
     * @return exp(z) = exp(real) * (cos(imag) + i*sin(imag))
     */
    public ComplexNumber exp() {
        double expReal = Math.exp(real);
        return new ComplexNumber(
            expReal * Math.cos(imag),
            expReal * Math.sin(imag)
        );
    }
    
    /**
     * Checks if this complex number is real (imaginary part is zero).
     * 
     * @return true if imaginary part is effectively zero
     */
    public boolean isReal() {
        return Math.abs(imag) < 1e-15;
    }
    
    /**
     * Checks if this complex number is purely imaginary (real part is zero).
     * 
     * @return true if real part is effectively zero
     */
    public boolean isImaginary() {
        return Math.abs(real) < 1e-15;
    }
    
    @Override
    public String toString() {
        if (isReal()) {
            return String.format("%.4f", real);
        } else if (isImaginary()) {
            return String.format("%.4fi", imag);
        } else if (imag >= 0) {
            return String.format("%.4f + %.4fi", real, imag);
        } else {
            return String.format("%.4f - %.4fi", real, -imag);
        }
    }
}
