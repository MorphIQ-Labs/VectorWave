package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.util.ValidationUtils;
import java.util.Arrays;
import java.util.Objects;

/**
 * Default implementation of the MODWTResult interface.
 * 
 * <p>This implementation provides defensive copying of coefficient arrays
 * and validation to ensure data integrity.</p>
 * 
 * <h2>Thread Safety:</h2>
 * <p>This class is immutable and thread-safe after construction.</p>
 */
final class MODWTResultImpl implements MODWTResult {
    
    private final double[] approximationCoeffs;
    private final double[] detailCoeffs;
    private final int signalLength;
    
    /**
     * Constructs a new MODWT result with the given coefficient arrays.
     * 
     * @param approximationCoeffs the approximation (low-pass) coefficients
     * @param detailCoeffs        the detail (high-pass) coefficients
     * @throws NullPointerException     if any parameter is null
     * @throws IllegalArgumentException if coefficient arrays have different lengths,
     *                                  are empty, or contain invalid values
     */
    public MODWTResultImpl(double[] approximationCoeffs, double[] detailCoeffs) {
        Objects.requireNonNull(approximationCoeffs, "approximationCoeffs cannot be null");
        Objects.requireNonNull(detailCoeffs, "detailCoeffs cannot be null");
        
        if (approximationCoeffs.length != detailCoeffs.length) {
            throw new IllegalArgumentException(
                "Approximation and detail coefficients must have the same length. " +
                "Got approximation length: " + approximationCoeffs.length + 
                ", detail length: " + detailCoeffs.length);
        }
        
        if (approximationCoeffs.length == 0) {
            throw new IllegalArgumentException("Coefficient arrays cannot be empty");
        }
        
        // Validate coefficient values
        ValidationUtils.validateFiniteValues(approximationCoeffs, "approximationCoeffs");
        ValidationUtils.validateFiniteValues(detailCoeffs, "detailCoeffs");
        
        // Store defensive copies
        this.approximationCoeffs = Arrays.copyOf(approximationCoeffs, approximationCoeffs.length);
        this.detailCoeffs = Arrays.copyOf(detailCoeffs, detailCoeffs.length);
        this.signalLength = approximationCoeffs.length;
    }
    
    @Override
    public double[] approximationCoeffs() {
        return Arrays.copyOf(approximationCoeffs, approximationCoeffs.length);
    }
    
    @Override
    public double[] detailCoeffs() {
        return Arrays.copyOf(detailCoeffs, detailCoeffs.length);
    }
    
    @Override
    public int getSignalLength() {
        return signalLength;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        MODWTResultImpl that = (MODWTResultImpl) obj;
        return signalLength == that.signalLength &&
               Arrays.equals(approximationCoeffs, that.approximationCoeffs) &&
               Arrays.equals(detailCoeffs, that.detailCoeffs);
    }
    
    @Override
    public int hashCode() {
        int result = Objects.hash(signalLength);
        result = 31 * result + Arrays.hashCode(approximationCoeffs);
        result = 31 * result + Arrays.hashCode(detailCoeffs);
        return result;
    }
    
    @Override
    public String toString() {
        return String.format("MODWTResult{signalLength=%d, approximation=%s, detail=%s}",
                signalLength,
                Arrays.toString(Arrays.copyOf(approximationCoeffs, Math.min(5, approximationCoeffs.length))),
                Arrays.toString(Arrays.copyOf(detailCoeffs, Math.min(5, detailCoeffs.length))));
    }
}