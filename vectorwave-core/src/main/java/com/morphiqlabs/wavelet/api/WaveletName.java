package com.morphiqlabs.wavelet.api;

/**
 * Enum of all supported wavelets in VectorWave.
 * Provides type-safe wavelet selection and lookup.
 * 
 * @since 1.0.0
 */
public enum WaveletName {
    /** Haar wavelet. */
    HAAR("haar", "Haar wavelet", WaveletType.ORTHOGONAL),
    
    /** Daubechies 2 wavelet. */
    DB2("db2", "Daubechies 2", WaveletType.ORTHOGONAL),
    /** Daubechies 4 wavelet. */
    DB4("db4", "Daubechies 4", WaveletType.ORTHOGONAL),
    /** Daubechies 6 wavelet. */
    DB6("db6", "Daubechies 6", WaveletType.ORTHOGONAL),
    /** Daubechies 8 wavelet. */
    DB8("db8", "Daubechies 8", WaveletType.ORTHOGONAL),
    /** Daubechies 10 wavelet. */
    DB10("db10", "Daubechies 10", WaveletType.ORTHOGONAL),
    
    /** Daubechies 12 wavelet. */
    DB12("db12", "Daubechies 12", WaveletType.ORTHOGONAL),
    /** Daubechies 14 wavelet. */
    DB14("db14", "Daubechies 14", WaveletType.ORTHOGONAL),
    /** Daubechies 16 wavelet. */
    DB16("db16", "Daubechies 16", WaveletType.ORTHOGONAL),
    /** Daubechies 18 wavelet. */
    DB18("db18", "Daubechies 18", WaveletType.ORTHOGONAL),
    /** Daubechies 20 wavelet. */
    DB20("db20", "Daubechies 20", WaveletType.ORTHOGONAL),
    
    /** Daubechies 22 wavelet. */
    DB22("db22", "Daubechies 22", WaveletType.ORTHOGONAL),
    /** Daubechies 24 wavelet. */
    DB24("db24", "Daubechies 24", WaveletType.ORTHOGONAL),
    /** Daubechies 26 wavelet. */
    DB26("db26", "Daubechies 26", WaveletType.ORTHOGONAL),
    /** Daubechies 28 wavelet. */
    DB28("db28", "Daubechies 28", WaveletType.ORTHOGONAL),
    /** Daubechies 30 wavelet. */
    DB30("db30", "Daubechies 30", WaveletType.ORTHOGONAL),
    
    /** Daubechies 32 wavelet. */
    DB32("db32", "Daubechies 32", WaveletType.ORTHOGONAL),
    /** Daubechies 34 wavelet. */
    DB34("db34", "Daubechies 34", WaveletType.ORTHOGONAL),
    /** Daubechies 36 wavelet. */
    DB36("db36", "Daubechies 36", WaveletType.ORTHOGONAL),
    /** Daubechies 38 wavelet. */
    DB38("db38", "Daubechies 38", WaveletType.ORTHOGONAL), // PyWavelets maximum
    
    /** Symlet 2 wavelet. */
    SYM2("sym2", "Symlet 2", WaveletType.ORTHOGONAL),
    /** Symlet 3 wavelet. */
    SYM3("sym3", "Symlet 3", WaveletType.ORTHOGONAL),
    /** Symlet 4 wavelet. */
    SYM4("sym4", "Symlet 4", WaveletType.ORTHOGONAL),
    /** Symlet 5 wavelet. */
    SYM5("sym5", "Symlet 5", WaveletType.ORTHOGONAL),
    /** Symlet 6 wavelet. */
    SYM6("sym6", "Symlet 6", WaveletType.ORTHOGONAL),
    /** Symlet 7 wavelet. */
    SYM7("sym7", "Symlet 7", WaveletType.ORTHOGONAL),
    /** Symlet 8 wavelet. */
    SYM8("sym8", "Symlet 8", WaveletType.ORTHOGONAL),
    /** Symlet 9 wavelet. */
    SYM9("sym9", "Symlet 9", WaveletType.ORTHOGONAL),
    /** Symlet 10 wavelet. */
    SYM10("sym10", "Symlet 10", WaveletType.ORTHOGONAL),
    /** Symlet 11 wavelet. */
    SYM11("sym11", "Symlet 11", WaveletType.ORTHOGONAL),
    /** Symlet 12 wavelet. */
    SYM12("sym12", "Symlet 12", WaveletType.ORTHOGONAL),
    /** Symlet 13 wavelet. */
    SYM13("sym13", "Symlet 13", WaveletType.ORTHOGONAL),
    /** Symlet 14 wavelet. */
    SYM14("sym14", "Symlet 14", WaveletType.ORTHOGONAL),
    /** Symlet 15 wavelet. */
    SYM15("sym15", "Symlet 15", WaveletType.ORTHOGONAL),
    /** Symlet 16 wavelet. */
    SYM16("sym16", "Symlet 16", WaveletType.ORTHOGONAL),
    /** Symlet 17 wavelet. */
    SYM17("sym17", "Symlet 17", WaveletType.ORTHOGONAL),
    /** Symlet 18 wavelet. */
    SYM18("sym18", "Symlet 18", WaveletType.ORTHOGONAL),
    /** Symlet 19 wavelet. */
    SYM19("sym19", "Symlet 19", WaveletType.ORTHOGONAL),
    /** Symlet 20 wavelet. */
    SYM20("sym20", "Symlet 20", WaveletType.ORTHOGONAL),
    
    /** Coiflet 1 wavelet. */
    COIF1("coif1", "Coiflet 1", WaveletType.ORTHOGONAL),
    /** Coiflet 2 wavelet. */
    COIF2("coif2", "Coiflet 2", WaveletType.ORTHOGONAL),
    /** Coiflet 3 wavelet. */
    COIF3("coif3", "Coiflet 3", WaveletType.ORTHOGONAL),
    /** Coiflet 4 wavelet. */
    COIF4("coif4", "Coiflet 4", WaveletType.ORTHOGONAL),
    /** Coiflet 5 wavelet. */
    COIF5("coif5", "Coiflet 5", WaveletType.ORTHOGONAL),
    /** Coiflet 6 wavelet. */
    COIF6("coif6", "Coiflet 6", WaveletType.ORTHOGONAL),
    /** Coiflet 7 wavelet. */
    COIF7("coif7", "Coiflet 7", WaveletType.ORTHOGONAL),
    /** Coiflet 8 wavelet. */
    COIF8("coif8", "Coiflet 8", WaveletType.ORTHOGONAL),
    /** Coiflet 9 wavelet. */
    COIF9("coif9", "Coiflet 9", WaveletType.ORTHOGONAL),
    /** Coiflet 10 wavelet. */
    COIF10("coif10", "Coiflet 10", WaveletType.ORTHOGONAL),
    /** Coiflet 11 wavelet. */
    COIF11("coif11", "Coiflet 11", WaveletType.ORTHOGONAL),
    /** Coiflet 12 wavelet. */
    COIF12("coif12", "Coiflet 12", WaveletType.ORTHOGONAL),
    /** Coiflet 13 wavelet. */
    COIF13("coif13", "Coiflet 13", WaveletType.ORTHOGONAL),
    /** Coiflet 14 wavelet. */
    COIF14("coif14", "Coiflet 14", WaveletType.ORTHOGONAL),
    /** Coiflet 15 wavelet. */
    COIF15("coif15", "Coiflet 15", WaveletType.ORTHOGONAL),
    /** Coiflet 16 wavelet. */
    COIF16("coif16", "Coiflet 16", WaveletType.ORTHOGONAL),
    /** Coiflet 17 wavelet. */
    COIF17("coif17", "Coiflet 17", WaveletType.ORTHOGONAL),
    
    /** Discrete Meyer wavelet. */
    DMEY("dmey", "Discrete Meyer wavelet", WaveletType.ORTHOGONAL),
    
    /** Linear Battle-Lemarié wavelet. */
    BLEM1("blem1", "Linear Battle-Lemarié", WaveletType.ORTHOGONAL),
    /** Quadratic Battle-Lemarié wavelet. */
    BLEM2("blem2", "Quadratic Battle-Lemarié", WaveletType.ORTHOGONAL),
    /** Cubic Battle-Lemarié wavelet. */
    BLEM3("blem3", "Cubic Battle-Lemarié", WaveletType.ORTHOGONAL),
    /** Quartic Battle-Lemarié wavelet. */
    BLEM4("blem4", "Quartic Battle-Lemarié", WaveletType.ORTHOGONAL),
    /** Quintic Battle-Lemarié wavelet. */
    BLEM5("blem5", "Quintic Battle-Lemarié", WaveletType.ORTHOGONAL),
    
    /** Biorthogonal 1.1 wavelet. */
    BIOR1_1("bior1.1", "Biorthogonal 1.1", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 1.3 wavelet. */
    BIOR1_3("bior1.3", "Biorthogonal 1.3", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 1.5 wavelet. */
    BIOR1_5("bior1.5", "Biorthogonal 1.5", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 2.2 wavelet. */
    BIOR2_2("bior2.2", "Biorthogonal 2.2", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 2.4 wavelet. */
    BIOR2_4("bior2.4", "Biorthogonal 2.4", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 2.6 wavelet. */
    BIOR2_6("bior2.6", "Biorthogonal 2.6", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 2.8 wavelet. */
    BIOR2_8("bior2.8", "Biorthogonal 2.8", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 3.1 wavelet. */
    BIOR3_1("bior3.1", "Biorthogonal 3.1", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 3.3 wavelet. */
    BIOR3_3("bior3.3", "Biorthogonal 3.3", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 3.5 wavelet. */
    BIOR3_5("bior3.5", "Biorthogonal 3.5", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 3.7 wavelet. */
    BIOR3_7("bior3.7", "Biorthogonal 3.7", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 3.9 wavelet. */
    BIOR3_9("bior3.9", "Biorthogonal 3.9", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 4.4 wavelet (JPEG2000). */
    BIOR4_4("bior4.4", "Biorthogonal 4.4 (JPEG2000)", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 5.5 wavelet. */
    BIOR5_5("bior5.5", "Biorthogonal 5.5", WaveletType.BIORTHOGONAL),
    /** Biorthogonal 6.8 wavelet. */
    BIOR6_8("bior6.8", "Biorthogonal 6.8", WaveletType.BIORTHOGONAL),
    
    /** Reverse Biorthogonal 1.1 wavelet. */
    RBIO1_1("rbio1.1", "Reverse Biorthogonal 1.1", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 1.3 wavelet. */
    RBIO1_3("rbio1.3", "Reverse Biorthogonal 1.3", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 1.5 wavelet. */
    RBIO1_5("rbio1.5", "Reverse Biorthogonal 1.5", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 2.2 wavelet. */
    RBIO2_2("rbio2.2", "Reverse Biorthogonal 2.2", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 2.4 wavelet. */
    RBIO2_4("rbio2.4", "Reverse Biorthogonal 2.4", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 2.6 wavelet. */
    RBIO2_6("rbio2.6", "Reverse Biorthogonal 2.6", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 2.8 wavelet. */
    RBIO2_8("rbio2.8", "Reverse Biorthogonal 2.8", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 3.1 wavelet. */
    RBIO3_1("rbio3.1", "Reverse Biorthogonal 3.1", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 3.3 wavelet. */
    RBIO3_3("rbio3.3", "Reverse Biorthogonal 3.3", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 3.5 wavelet. */
    RBIO3_5("rbio3.5", "Reverse Biorthogonal 3.5", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 3.7 wavelet. */
    RBIO3_7("rbio3.7", "Reverse Biorthogonal 3.7", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 3.9 wavelet. */
    RBIO3_9("rbio3.9", "Reverse Biorthogonal 3.9", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 4.4 wavelet. */
    RBIO4_4("rbio4.4", "Reverse Biorthogonal 4.4", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 5.5 wavelet. */
    RBIO5_5("rbio5.5", "Reverse Biorthogonal 5.5", WaveletType.BIORTHOGONAL),
    /** Reverse Biorthogonal 6.8 wavelet. */
    RBIO6_8("rbio6.8", "Reverse Biorthogonal 6.8", WaveletType.BIORTHOGONAL),
    
    /** Morlet wavelet. */
    MORLET("morl", "Morlet wavelet", WaveletType.CONTINUOUS),
    /** Mexican Hat wavelet. */
    MEXICAN_HAT("mexh", "Mexican Hat wavelet", WaveletType.CONTINUOUS),
    /** Gaussian wavelet. */
    GAUSSIAN("gaus", "Gaussian wavelet", WaveletType.CONTINUOUS),
    /** Paul wavelet. */
    PAUL("paul", "Paul wavelet", WaveletType.CONTINUOUS),
    /** Derivative of Gaussian wavelet. */
    DOG("dog", "Derivative of Gaussian", WaveletType.CONTINUOUS),
    /** Shannon wavelet. */
    SHANNON("shan", "Shannon wavelet", WaveletType.CONTINUOUS),
    /** Frequency B-Spline wavelet. */
    FBSP("fbsp", "Frequency B-Spline", WaveletType.CONTINUOUS),
    /** Complex Morlet wavelet. */
    CMOR("cmor", "Complex Morlet", WaveletType.COMPLEX),
    /** Complex Gaussian wavelet. */
    CGAU("cgau", "Complex Gaussian", WaveletType.COMPLEX),
    
    /** Complex Shannon wavelet. */
    CSHAN("cshan", "Complex Shannon wavelet", WaveletType.COMPLEX),
    /** Meyer wavelet. */
    MEYER("meyr", "Meyer wavelet", WaveletType.CONTINUOUS),
    /** Morse wavelet. */
    MORSE("morse", "Morse wavelet", WaveletType.COMPLEX),
    /** Ricker wavelet. */
    RICKER("ricker", "Ricker wavelet", WaveletType.CONTINUOUS),
    /** Hermitian wavelet. */
    HERMITIAN("herm", "Hermitian wavelet", WaveletType.CONTINUOUS);
    
    private final String code;
    private final String description;
    private final WaveletType type;
    
    WaveletName(String code, String description, WaveletType type) {
        this.code = code;
        this.description = description;
        this.type = type;
    }
    
    /**
     * Get the wavelet code (e.g., "db4", "sym2", "morl").
     * @return the wavelet code
     */
    public String getCode() {
        return code;
    }
    
    /**
     * Get the human-readable description of the wavelet.
     * @return the wavelet description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Get the type category of this wavelet.
     * @return the wavelet type
     */
    public WaveletType getType() {
        return type;
    }
    
    
    /**
     * Get the display name suitable for UI presentation.
     * This is an alias for getDescription() to improve clarity.
     * @return the display name (e.g., "Symlet 11")
     */
    public String getDisplayName() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
}
