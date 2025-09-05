package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;

/**
 * Result of a Continuous Wavelet Transform.
 * 
 * <p>Contains the time-scale representation of the signal, providing access to
 * coefficients, magnitude, phase (for complex wavelets), and various analysis methods.</p>
 */
public final class CWTResult {
    
    private final double[][] coefficients;    // For real wavelets
    private final ComplexMatrix complexCoeffs; // For complex wavelets
    private final double[] scales;
    private final ContinuousWavelet wavelet;
    private final boolean isComplex;
    
    // Cached computations
    private double[][] magnitudeCache;
    private double[][] phaseCache;
    private double[][] powerCache;
    
    /**
     * Creates a CWT result with real coefficients.
     * 
     * @param coefficients real-valued coefficients [scale][time]
     * @param scales scale values
     * @param wavelet the wavelet used
     */
    public CWTResult(double[][] coefficients, double[] scales, ContinuousWavelet wavelet) {
        validateInputs(coefficients, scales, wavelet);
        
        // Deep copy coefficients
        this.coefficients = new double[coefficients.length][];
        for (int i = 0; i < coefficients.length; i++) {
            this.coefficients[i] = coefficients[i].clone();
        }
        
        this.scales = scales.clone();
        this.wavelet = wavelet;
        this.isComplex = false;
        this.complexCoeffs = null;
    }
    
    /**
     * Creates a CWT result with complex coefficients.
     * 
     * @param complexCoeffs complex-valued coefficients
     * @param scales scale values
     * @param wavelet the wavelet used
     */
    public CWTResult(ComplexMatrix complexCoeffs, double[] scales, ContinuousWavelet wavelet) {
        if (complexCoeffs == null) {
            throw new IllegalArgumentException("Complex coefficients cannot be null");
        }
        if (scales == null) {
            throw new IllegalArgumentException("Scales cannot be null");
        }
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        if (complexCoeffs.getRows() != scales.length) {
            throw new IllegalArgumentException("Number of scales must match coefficient rows");
        }
        
        this.complexCoeffs = complexCoeffs;
        this.scales = scales.clone();
        this.wavelet = wavelet;
        this.isComplex = true;
        this.coefficients = null;
    }
    
    /**
     * Gets the magnitude (absolute value) of coefficients.
     * 
     * @return magnitude matrix [scale][time]
     */
    public double[][] getMagnitude() {
        if (magnitudeCache == null) {
            if (isComplex) {
                magnitudeCache = complexCoeffs.getMagnitude();
            } else {
                // For real coefficients, magnitude = |coefficient|
                magnitudeCache = new double[coefficients.length][];
                for (int i = 0; i < coefficients.length; i++) {
                    magnitudeCache[i] = new double[coefficients[i].length];
                    for (int j = 0; j < coefficients[i].length; j++) {
                        magnitudeCache[i][j] = Math.abs(coefficients[i][j]);
                    }
                }
            }
        }
        return cloneMatrix(magnitudeCache);
    }
    
    /**
     * Gets the phase of coefficients (only for complex wavelets).
     * 
     * @return phase matrix [scale][time] in radians, or null for real wavelets
     */
    public double[][] getPhase() {
        if (!isComplex) {
            return null;
        }
        
        if (phaseCache == null) {
            phaseCache = complexCoeffs.getPhase();
        }
        return cloneMatrix(phaseCache);
    }
    
    /**
     * Gets the power spectrum (magnitude squared).
     * 
     * @return power spectrum [scale][time]
     */
    public double[][] getPowerSpectrum() {
        if (powerCache == null) {
            double[][] magnitude = getMagnitude();
            powerCache = new double[magnitude.length][];
            for (int i = 0; i < magnitude.length; i++) {
                powerCache[i] = new double[magnitude[i].length];
                for (int j = 0; j < magnitude[i].length; j++) {
                    powerCache[i][j] = magnitude[i][j] * magnitude[i][j];
                }
            }
        }
        return cloneMatrix(powerCache);
    }
    
    /**
     * Gets the scalogram at a specific time index.
     * 
     * @param timeIndex time index
     * @return coefficient values across all scales at given time
     */
    public double[] getScalogram(int timeIndex) {
        int numScales = getNumScales();
        int numSamples = getNumSamples();
        
        if (timeIndex < 0 || timeIndex >= numSamples) {
            throw new IndexOutOfBoundsException("Time index out of bounds: " + timeIndex);
        }
        
        double[] scalogram = new double[numScales];
        double[][] magnitude = getMagnitude();
        
        for (int i = 0; i < numScales; i++) {
            scalogram[i] = magnitude[i][timeIndex];
        }
        
        return scalogram;
    }
    
    /**
     * Gets a time slice at a specific scale index.
     * 
     * @param scaleIndex scale index
     * @return coefficient values across all time points at given scale
     */
    public double[] getTimeSlice(int scaleIndex) {
        if (scaleIndex < 0 || scaleIndex >= getNumScales()) {
            throw new IndexOutOfBoundsException("Scale index out of bounds: " + scaleIndex);
        }
        
        if (isComplex) {
            return complexCoeffs.getReal()[scaleIndex].clone();
        } else {
            return coefficients[scaleIndex].clone();
        }
    }
    
    /**
     * Finds the maximum coefficient location.
     * 
     * @return maximum coefficient information
     */
    public MaxCoefficient findMaxCoefficient() {
        double[][] magnitude = getMagnitude();
        double maxValue = Double.NEGATIVE_INFINITY;
        int maxScaleIdx = -1;
        int maxTimeIdx = -1;
        
        for (int i = 0; i < magnitude.length; i++) {
            for (int j = 0; j < magnitude[i].length; j++) {
                if (magnitude[i][j] > maxValue) {
                    maxValue = magnitude[i][j];
                    maxScaleIdx = i;
                    maxTimeIdx = j;
                }
            }
        }
        
        return new MaxCoefficient(maxValue, maxScaleIdx, maxTimeIdx, scales[maxScaleIdx]);
    }
    
    /**
     * Converts scales to frequencies.
     * 
     * @param samplingRate sampling rate in Hz
     * @return array of frequencies corresponding to each scale
     */
    public double[] getFrequencies(double samplingRate) {
        double centerFreq = wavelet.centerFrequency();
        double[] frequencies = new double[scales.length];
        
        for (int i = 0; i < scales.length; i++) {
            frequencies[i] = (centerFreq * samplingRate) / scales[i];
        }
        
        return frequencies;
    }
    
    /**
     * Computes time-averaged spectrum.
     * 
     * @return average magnitude across time for each scale
     */
    public double[] getTimeAveragedSpectrum() {
        double[][] magnitude = getMagnitude();
        double[] avgSpectrum = new double[magnitude.length];
        
        for (int i = 0; i < magnitude.length; i++) {
            double sum = 0.0;
            for (int j = 0; j < magnitude[i].length; j++) {
                sum += magnitude[i][j];
            }
            avgSpectrum[i] = sum / magnitude[i].length;
        }
        
        return avgSpectrum;
    }
    
    /**
     * Gets the raw coefficients (real-valued).
     * 
     * @return copy of coefficients or real part if complex
     */
    public double[][] getCoefficients() {
        if (isComplex) {
            return complexCoeffs.getReal();
        } else {
            return cloneMatrix(coefficients);
        }
    }
    
    // Getters
    
    /**
     * Gets the scale array used in this transform.
     * @return a copy of the scales array
     */
    public double[] getScales() {
        return scales.clone();
    }
    
    /**
     * Gets the wavelet used to produce this result.
     * @return the continuous wavelet
     */
    public ContinuousWavelet getWavelet() {
        return wavelet;
    }
    
    /**
     * Indicates if the underlying coefficients are complex-valued.
     * @return true if complex, false if real-valued
     */
    public boolean isComplex() {
        return isComplex;
    }
    
    /**
     * Gets the number of scales in this result.
     * @return number of scales
     */
    public int getNumScales() {
        return scales.length;
    }
    
    /**
     * Gets the number of time samples in each scale row.
     * @return number of samples per scale
     */
    public int getNumSamples() {
        if (isComplex) {
            return complexCoeffs.getCols();
        } else {
            return coefficients[0].length;
        }
    }
    
    // Helper methods
    
    private void validateInputs(double[][] coefficients, double[] scales, ContinuousWavelet wavelet) {
        if (coefficients == null) {
            throw new IllegalArgumentException("Coefficients cannot be null");
        }
        if (scales == null) {
            throw new IllegalArgumentException("Scales cannot be null");
        }
        if (wavelet == null) {
            throw new IllegalArgumentException("Wavelet cannot be null");
        }
        if (coefficients.length == 0 || coefficients[0].length == 0) {
            throw new IllegalArgumentException("Coefficients cannot be empty");
        }
        if (coefficients.length != scales.length) {
            throw new IllegalArgumentException("Number of scales must match coefficient rows");
        }
    }
    
    private double[][] cloneMatrix(double[][] matrix) {
        double[][] clone = new double[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            clone[i] = matrix[i].clone();
        }
        return clone;
    }
    
    /**
     * Information about maximum coefficient location.
     *
     * @param value coefficient magnitude at the maximum
     * @param scaleIndex index of the scale (row)
     * @param timeIndex index in time (column)
     * @param scale physical scale corresponding to {@code scaleIndex}
     */
    public record MaxCoefficient(
        double value,
        int scaleIndex,
        int timeIndex,
        double scale
    ) {}
}
