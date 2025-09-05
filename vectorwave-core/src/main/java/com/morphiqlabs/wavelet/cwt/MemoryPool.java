package com.morphiqlabs.wavelet.cwt;

/**
 * Memory pool abstraction for CWT operations.
 *
 * <p>Allows callers to provide custom allocation strategies for coefficient
 * storage to reduce allocation overhead and GC pressure during transform
 * computation.</p>
 */
public interface MemoryPool {

    /**
     * Allocates a 2D coefficient matrix of at least the given dimensions.
     *
     * @param rows number of rows (scales)
     * @param cols number of columns (time samples)
     * @return a matrix suitable for storing coefficients
     */
    double[][] allocateCoefficients(int rows, int cols);
}

