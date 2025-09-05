package com.morphiqlabs.wavelet.performance;

import java.io.Serializable;

/**
 * Polynomial coefficients for performance modeling.
 * 
 * <p>Represents a quadratic model: time = a + b*n + c*n^2</p>
 * 
 * <p>This class supports online learning through incremental updates,
 * allowing the model to adapt as new measurements are collected.</p>
 * 
 * <p>Thread-safe implementation using synchronized methods for all operations
 * that read or modify the coefficients.</p>
 */
public class ModelCoefficients implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private double a; // Constant term
    private double b; // Linear coefficient
    private double c; // Quadratic coefficient
    
    // Online learning parameters
    private double learningRate = 0.01;
    private int updateCount = 0;
    
    /**
     * Creates coefficients with default values.
     */
    public ModelCoefficients() {
        this(0.1, 0.0001, 0);
    }
    
    /**
     * Creates coefficients with specified values.
     * 
     * @param a Constant term
     * @param b Linear coefficient
     * @param c Quadratic coefficient
     */
    public ModelCoefficients(double a, double b, double c) {
        this.a = a;
        this.b = b;
        this.c = c;
    }
    
    /**
     * Evaluates the polynomial for a given input size.
     * Thread-safe evaluation that ensures consistent read of coefficients.
     * 
     * @param n Input size
     * @return Predicted time
     */
    public synchronized double evaluate(int n) {
        return a + b * n + c * n * n;
    }
    
    /**
     * Updates coefficients using gradient descent.
     * Thread-safe update that ensures atomic modification of all coefficients.
     * 
     * @param inputSize The input size
     * @param actualTime The actual execution time
     */
    public synchronized void updateWithMeasurement(int inputSize, double actualTime) {
        double predicted = evaluate(inputSize);
        double error = actualTime - predicted;
        
        // Adaptive learning rate that decreases with more updates
        double adaptiveLearningRate = learningRate / (1 + updateCount * 0.1);
        
        // Gradient descent update
        double gradA = -2 * error;
        double gradB = -2 * error * inputSize;
        double gradC = -2 * error * inputSize * inputSize;
        
        // Update coefficients
        a -= adaptiveLearningRate * gradA;
        b -= adaptiveLearningRate * gradB;
        c -= adaptiveLearningRate * gradC * 0.1; // Smaller update for quadratic term
        
        // Ensure coefficients remain in reasonable bounds
        a = Math.max(0, a); // Time cannot be negative
        b = Math.max(0, b); // Larger inputs should not be faster
        c = Math.max(0, c); // No negative quadratic effects
        
        updateCount++;
    }
    
    /**
     * Gets the constant term.
     * Thread-safe getter.
     * 
     * @return Constant coefficient
     */
    public synchronized double getA() {
        return a;
    }
    
    /**
     * Gets the linear coefficient.
     * Thread-safe getter.
     * 
     * @return Linear coefficient
     */
    public synchronized double getB() {
        return b;
    }
    
    /**
     * Gets the quadratic coefficient.
     * Thread-safe getter.
     * 
     * @return Quadratic coefficient
     */
    public synchronized double getC() {
        return c;
    }
    
    /**
     * Creates a copy of these coefficients.
     * Thread-safe copy that ensures consistent snapshot of coefficients.
     * 
     * @return New instance with same values
     */
    public synchronized ModelCoefficients copy() {
        return new ModelCoefficients(a, b, c);
    }
    
    @Override
    public synchronized String toString() {
        return String.format("%.4f + %.6f*n + %.9f*n²", a, b, c);
    }
}