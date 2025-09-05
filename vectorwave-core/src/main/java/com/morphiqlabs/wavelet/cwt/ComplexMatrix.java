package com.morphiqlabs.wavelet.cwt;

/**
 * A matrix of complex numbers for CWT computations.
 * 
 * <p>Stores complex numbers in separate real and imaginary arrays
 * for better cache locality and SIMD optimization.</p>
 */
public final class ComplexMatrix {
    
    private final double[][] real;
    private final double[][] imaginary;
    private final int rows;
    private final int cols;
    
    /**
     * Creates a new complex matrix with given dimensions.
     * 
     * @param rows number of rows
     * @param cols number of columns
     */
    public ComplexMatrix(int rows, int cols) {
        if (rows <= 0 || cols <= 0) {
            throw new IllegalArgumentException("Matrix dimensions must be positive");
        }
        this.rows = rows;
        this.cols = cols;
        this.real = new double[rows][cols];
        this.imaginary = new double[rows][cols];
    }
    
    /**
     * Creates a complex matrix from real and imaginary parts.
     * 
     * @param real real part of the matrix
     * @param imaginary imaginary part of the matrix
     */
    public ComplexMatrix(double[][] real, double[][] imaginary) {
        if (real == null || imaginary == null) {
            throw new IllegalArgumentException("Real and imaginary parts cannot be null");
        }
        if (real.length == 0 || real[0].length == 0) {
            throw new IllegalArgumentException("Matrix cannot be empty");
        }
        if (real.length != imaginary.length || real[0].length != imaginary[0].length) {
            throw new IllegalArgumentException("Real and imaginary parts must have same dimensions");
        }
        
        this.rows = real.length;
        this.cols = real[0].length;
        this.real = new double[rows][cols];
        this.imaginary = new double[rows][cols];
        
        // Deep copy
        for (int i = 0; i < rows; i++) {
            System.arraycopy(real[i], 0, this.real[i], 0, cols);
            System.arraycopy(imaginary[i], 0, this.imaginary[i], 0, cols);
        }
    }
    
    /**
     * Sets a complex value at given position.
     * 
     * @param row row index
     * @param col column index
     * @param realValue real part
     * @param imagValue imaginary part
     */
    public void set(int row, int col, double realValue, double imagValue) {
        validateIndices(row, col);
        real[row][col] = realValue;
        imaginary[row][col] = imagValue;
    }
    
    /**
     * Gets the real part at given position.
     * 
     * @param row row index
     * @param col column index
     * @return real part
     */
    public double getReal(int row, int col) {
        validateIndices(row, col);
        return real[row][col];
    }
    
    /**
     * Gets the imaginary part at given position.
     * 
     * @param row row index
     * @param col column index
     * @return imaginary part
     */
    public double getImaginary(int row, int col) {
        validateIndices(row, col);
        return imaginary[row][col];
    }
    
    /**
     * Computes magnitude at given position.
     * 
     * @param row row index
     * @param col column index
     * @return magnitude |z| = sqrt(real² + imag²)
     */
    public double getMagnitude(int row, int col) {
        validateIndices(row, col);
        double r = real[row][col];
        double i = imaginary[row][col];
        return Math.sqrt(r * r + i * i);
    }
    
    /**
     * Computes phase at given position.
     * 
     * @param row row index
     * @param col column index
     * @return phase angle in radians
     */
    public double getPhase(int row, int col) {
        validateIndices(row, col);
        return Math.atan2(imaginary[row][col], real[row][col]);
    }
    
    /**
     * Gets all real parts as a matrix.
     * 
     * @return copy of real parts
     */
    public double[][] getReal() {
        double[][] copy = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(real[i], 0, copy[i], 0, cols);
        }
        return copy;
    }
    
    /**
     * Gets all imaginary parts as a matrix.
     * 
     * @return copy of imaginary parts
     */
    public double[][] getImaginary() {
        double[][] copy = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            System.arraycopy(imaginary[i], 0, copy[i], 0, cols);
        }
        return copy;
    }
    
    /**
     * Computes magnitude matrix.
     * 
     * @return magnitude matrix
     */
    public double[][] getMagnitude() {
        double[][] magnitude = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                double r = real[i][j];
                double im = imaginary[i][j];
                magnitude[i][j] = Math.sqrt(r * r + im * im);
            }
        }
        return magnitude;
    }
    
    /**
     * Computes phase matrix.
     * 
     * @return phase matrix in radians
     */
    public double[][] getPhase() {
        double[][] phase = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                phase[i][j] = Math.atan2(imaginary[i][j], real[i][j]);
            }
        }
        return phase;
    }
    
    /**
     * Gets the number of rows in the matrix.
     *
     * @return the number of rows
     */
    public int getRows() {
        return rows;
    }
    
    /**
     * Gets the number of columns in the matrix.
     *
     * @return the number of columns
     */
    public int getCols() {
        return cols;
    }
    
    private void validateIndices(int row, int col) {
        if (row < 0 || row >= rows) {
            throw new IndexOutOfBoundsException("Row index out of bounds: " + row);
        }
        if (col < 0 || col >= cols) {
            throw new IndexOutOfBoundsException("Column index out of bounds: " + col);
        }
    }
}