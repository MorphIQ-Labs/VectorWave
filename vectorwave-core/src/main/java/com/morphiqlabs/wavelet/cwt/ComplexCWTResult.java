package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;

/**
 * Result of a complex-valued Continuous Wavelet Transform.
 * 
 * <p>Contains complex coefficients that preserve both magnitude and phase
 * information, enabling advanced analysis such as:</p>
 * <ul>
 *   <li>Phase synchronization analysis</li>
 *   <li>Instantaneous frequency estimation</li>
 *   <li>Ridge extraction</li>
 *   <li>Wavelet coherence computation</li>
 * </ul>
 */
public final class ComplexCWTResult {
    
    private final ComplexNumber[][] coefficients;
    private final double[] scales;
    private final ContinuousWavelet wavelet;
    private final int numScales;
    private final int numSamples;
    
    // Cached derived data
    private double[][] magnitudeCache;
    private double[][] phaseCache;
    
    /**
     * Creates a complex CWT result.
     * 
     * @param coefficients complex coefficients [scale][time]
     * @param scales the scales used
     * @param wavelet the wavelet used
     */
    public ComplexCWTResult(ComplexNumber[][] coefficients, double[] scales, 
                           ContinuousWavelet wavelet) {
        if (coefficients == null || scales == null || wavelet == null) {
            throw new IllegalArgumentException("Arguments cannot be null");
        }
        if (coefficients.length != scales.length) {
            throw new IllegalArgumentException(
                "Coefficients rows must match scales length");
        }
        
        this.coefficients = coefficients;
        this.scales = scales.clone();
        this.wavelet = wavelet;
        this.numScales = scales.length;
        this.numSamples = coefficients.length > 0 ? coefficients[0].length : 0;
    }
    
    /**
     * Gets the complex coefficients.
     * 
     * @return complex coefficients [scale][time]
     */
    public ComplexNumber[][] getCoefficients() {
        // Return defensive copy to maintain immutability
        ComplexNumber[][] copy = new ComplexNumber[numScales][];
        for (int i = 0; i < numScales; i++) {
            copy[i] = coefficients[i].clone();
        }
        return copy;
    }
    
    /**
     * Gets a specific complex coefficient.
     * 
     * @param scaleIndex the scale index
     * @param timeIndex the time index
     * @return the complex coefficient
     */
    public ComplexNumber getCoefficient(int scaleIndex, int timeIndex) {
        return coefficients[scaleIndex][timeIndex];
    }
    
    /**
     * Gets the magnitude (modulus) scalogram.
     * 
     * @return magnitude values [scale][time]
     */
    public double[][] getMagnitude() {
        if (magnitudeCache == null) {
            magnitudeCache = new double[numScales][numSamples];
            for (int s = 0; s < numScales; s++) {
                for (int t = 0; t < numSamples; t++) {
                    magnitudeCache[s][t] = coefficients[s][t].magnitude();
                }
            }
        }
        
        // Return defensive copy
        double[][] copy = new double[numScales][];
        for (int i = 0; i < numScales; i++) {
            copy[i] = magnitudeCache[i].clone();
        }
        return copy;
    }
    
    /**
     * Gets the phase scalogram.
     * 
     * @return phase values [scale][time] in radians [-π, π]
     */
    public double[][] getPhase() {
        if (phaseCache == null) {
            phaseCache = new double[numScales][numSamples];
            for (int s = 0; s < numScales; s++) {
                for (int t = 0; t < numSamples; t++) {
                    phaseCache[s][t] = coefficients[s][t].phase();
                }
            }
        }
        
        // Return defensive copy
        double[][] copy = new double[numScales][];
        for (int i = 0; i < numScales; i++) {
            copy[i] = phaseCache[i].clone();
        }
        return copy;
    }
    
    /**
     * Gets the real part of coefficients.
     * 
     * @return real values [scale][time]
     */
    public double[][] getReal() {
        double[][] real = new double[numScales][numSamples];
        for (int s = 0; s < numScales; s++) {
            for (int t = 0; t < numSamples; t++) {
                real[s][t] = coefficients[s][t].real();
            }
        }
        return real;
    }
    
    /**
     * Gets the imaginary part of coefficients.
     * 
     * @return imaginary values [scale][time]
     */
    public double[][] getImaginary() {
        double[][] imag = new double[numScales][numSamples];
        for (int s = 0; s < numScales; s++) {
            for (int t = 0; t < numSamples; t++) {
                imag[s][t] = coefficients[s][t].imag();
            }
        }
        return imag;
    }
    
    /**
     * Computes the power (magnitude squared) scalogram.
     * 
     * @return power values [scale][time]
     */
    public double[][] getPower() {
        double[][] power = new double[numScales][numSamples];
        for (int s = 0; s < numScales; s++) {
            for (int t = 0; t < numSamples; t++) {
                ComplexNumber c = coefficients[s][t];
                power[s][t] = c.real() * c.real() + c.imag() * c.imag();
            }
        }
        return power;
    }
    
    /**
     * Converts to a real-valued CWT result using magnitude.
     * 
     * @return standard CWT result with magnitude coefficients
     */
    public CWTResult toRealResult() {
        return new CWTResult(getMagnitude(), scales, wavelet);
    }
    
    /**
     * Extracts coefficients at a specific scale.
     * 
     * @param scaleIndex the scale index
     * @return complex coefficients at that scale
     */
    public ComplexNumber[] getScaleCoefficients(int scaleIndex) {
        if (scaleIndex < 0 || scaleIndex >= numScales) {
            throw new IndexOutOfBoundsException(
                "Scale index out of bounds: " + scaleIndex);
        }
        return coefficients[scaleIndex].clone();
    }
    
    /**
     * Extracts coefficients at a specific time.
     * 
     * @param timeIndex the time index
     * @return complex coefficients at that time across all scales
     */
    public ComplexNumber[] getTimeCoefficients(int timeIndex) {
        if (timeIndex < 0 || timeIndex >= numSamples) {
            throw new IndexOutOfBoundsException(
                "Time index out of bounds: " + timeIndex);
        }
        
        ComplexNumber[] timeCoeffs = new ComplexNumber[numScales];
        for (int s = 0; s < numScales; s++) {
            timeCoeffs[s] = coefficients[s][timeIndex];
        }
        return timeCoeffs;
    }
    
    /**
     * Computes instantaneous frequency at each point.
     * 
     * @return instantaneous frequency [scale][time] in normalized units
     */
    public double[][] getInstantaneousFrequency() {
        double[][] instFreq = new double[numScales][numSamples - 1];
        
        for (int s = 0; s < numScales; s++) {
            for (int t = 0; t < numSamples - 1; t++) {
                // Instantaneous frequency from phase derivative
                double phase1 = coefficients[s][t].phase();
                double phase2 = coefficients[s][t + 1].phase();
                
                // Handle phase wrapping
                double phaseDiff = phase2 - phase1;
                if (phaseDiff > Math.PI) {
                    phaseDiff -= 2 * Math.PI;
                } else if (phaseDiff < -Math.PI) {
                    phaseDiff += 2 * Math.PI;
                }
                
                // Convert to frequency (normalize by 2π)
                instFreq[s][t] = phaseDiff / (2 * Math.PI);
            }
        }
        
        return instFreq;
    }
    
    /**
     * Gets the scales used in the transform.
     * 
     * @return array of scales
     */
    public double[] getScales() {
        return scales.clone();
    }
    
    /**
     * Gets the wavelet used in the transform.
     * 
     * @return the continuous wavelet
     */
    public ContinuousWavelet getWavelet() {
        return wavelet;
    }
    
    /**
     * Gets the number of scales.
     * 
     * @return number of scales
     */
    public int getNumScales() {
        return numScales;
    }
    
    /**
     * Gets the number of time samples.
     * 
     * @return number of samples
     */
    public int getNumSamples() {
        return numSamples;
    }
}