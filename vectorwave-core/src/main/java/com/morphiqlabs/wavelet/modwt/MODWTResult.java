package com.morphiqlabs.wavelet.modwt;

/**
 * Represents the result of a MODWT (Maximal Overlap Discrete Wavelet Transform).
 * 
 * <p>MODWT produces non-decimated coefficients that maintain the same length as the input signal.
 * This results in a shift-invariant transform that can handle arbitrary length signals.</p>
 * 
 * <p>For a signal of length N, both approximation and detail coefficients will have length N,
 * making the transform redundant but preserving all temporal information.</p>
 * 
 * <h2>Usage Example:</h2>
 * <pre>{@code
 * MODWTTransform modwt = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
 * double[] signal = {1, 2, 3, 4, 5, 6, 7, 8};
 * 
 * MODWTResult result = modwt.forward(signal);
 * double[] approximation = result.approximationCoeffs(); // length 8
 * double[] detail = result.detailCoeffs();               // length 8
 * }</pre>
 * 
 * @see MODWTTransform
 * @since 1.0.0
 */
public interface MODWTResult {
    
    /**
     * Creates a new MODWTResult with the given coefficients.
     * This is a convenience factory method.
     *
     * @param approximationCoeffs the approximation coefficients
     * @param detailCoeffs        the detail coefficients
     * @return a new MODWTResult
     * @throws IllegalArgumentException if coefficients are invalid
     */
    static MODWTResult create(double[] approximationCoeffs, double[] detailCoeffs) {
        return new MODWTResultImpl(approximationCoeffs, detailCoeffs);
    }
    
    /**
     * Returns the approximation (low-pass) coefficients from the MODWT.
     * These coefficients represent the smooth, low-frequency components of the signal.
     * 
     * <p>For MODWT, the approximation coefficients have the same length as the 
     * original input signal.</p>
     * 
     * @return a defensive copy of the approximation coefficients
     */
    double[] approximationCoeffs();
    
    /**
     * Returns the detail (high-pass) coefficients from the MODWT.
     * These coefficients represent the edges, details, and high-frequency components.
     * 
     * <p>For MODWT, the detail coefficients have the same length as the 
     * original input signal.</p>
     * 
     * @return a defensive copy of the detail coefficients
     */
    double[] detailCoeffs();
    
    /**
     * Returns the length of the original signal that produced this MODWT result.
     * This is equal to the length of both approximation and detail coefficient arrays.
     * 
     * @return the signal length
     */
    int getSignalLength();
    
    /**
     * Validates that this MODWT result has consistent coefficient arrays.
     * Both approximation and detail coefficients should have the same length
     * and match the signal length.
     * 
     * <p>This default implementation performs basic validation.</p>
     * 
     * @return true if the result is valid, false otherwise
     */
    default boolean isValid() {
        double[] approx = approximationCoeffs();
        double[] detail = detailCoeffs();
        
        if (approx == null || detail == null) {
            return false;
        }
        
        if (approx.length != detail.length) {
            return false;
        }
        
        if (approx.length != getSignalLength()) {
            return false;
        }
        
        // Check for any NaN or infinite values
        for (double coeff : approx) {
            if (!Double.isFinite(coeff)) {
                return false;
            }
        }
        
        for (double coeff : detail) {
            if (!Double.isFinite(coeff)) {
                return false;
            }
        }
        
        return true;
    }
}
