package com.morphiqlabs.wavelet.api;

/**
 * Enum of all supported wavelets in VectorWave.
 * Provides type-safe wavelet selection and lookup.
 */
public enum WaveletName {
    // Haar wavelet
    HAAR("haar", "Haar wavelet", WaveletType.ORTHOGONAL),
    
    // Daubechies wavelets
    DB2("db2", "Daubechies 2", WaveletType.ORTHOGONAL),
    DB4("db4", "Daubechies 4", WaveletType.ORTHOGONAL),
    DB6("db6", "Daubechies 6", WaveletType.ORTHOGONAL),
    DB8("db8", "Daubechies 8", WaveletType.ORTHOGONAL),
    DB10("db10", "Daubechies 10", WaveletType.ORTHOGONAL),
    
    // Extended Daubechies wavelets (DB12-DB20)
    DB12("db12", "Daubechies 12", WaveletType.ORTHOGONAL),
    DB14("db14", "Daubechies 14", WaveletType.ORTHOGONAL),
    DB16("db16", "Daubechies 16", WaveletType.ORTHOGONAL),
    DB18("db18", "Daubechies 18", WaveletType.ORTHOGONAL),
    DB20("db20", "Daubechies 20", WaveletType.ORTHOGONAL),
    
    // Advanced Extended Daubechies wavelets (DB22-DB38)
    // DB22-DB30 set
    DB22("db22", "Daubechies 22", WaveletType.ORTHOGONAL),
    DB24("db24", "Daubechies 24", WaveletType.ORTHOGONAL),
    DB26("db26", "Daubechies 26", WaveletType.ORTHOGONAL),
    DB28("db28", "Daubechies 28", WaveletType.ORTHOGONAL),
    DB30("db30", "Daubechies 30", WaveletType.ORTHOGONAL),
    
    // DB32-DB38 set
    DB32("db32", "Daubechies 32", WaveletType.ORTHOGONAL),
    DB34("db34", "Daubechies 34", WaveletType.ORTHOGONAL),
    DB36("db36", "Daubechies 36", WaveletType.ORTHOGONAL),
    DB38("db38", "Daubechies 38", WaveletType.ORTHOGONAL), // PyWavelets maximum
    
    // Symlet wavelets
    SYM2("sym2", "Symlet 2", WaveletType.ORTHOGONAL),
    SYM3("sym3", "Symlet 3", WaveletType.ORTHOGONAL),
    SYM4("sym4", "Symlet 4", WaveletType.ORTHOGONAL),
    SYM5("sym5", "Symlet 5", WaveletType.ORTHOGONAL),
    SYM6("sym6", "Symlet 6", WaveletType.ORTHOGONAL),
    SYM7("sym7", "Symlet 7", WaveletType.ORTHOGONAL),
    SYM8("sym8", "Symlet 8", WaveletType.ORTHOGONAL),
    SYM9("sym9", "Symlet 9", WaveletType.ORTHOGONAL),
    SYM10("sym10", "Symlet 10", WaveletType.ORTHOGONAL),
    SYM11("sym11", "Symlet 11", WaveletType.ORTHOGONAL),
    SYM12("sym12", "Symlet 12", WaveletType.ORTHOGONAL),
    SYM13("sym13", "Symlet 13", WaveletType.ORTHOGONAL),
    SYM14("sym14", "Symlet 14", WaveletType.ORTHOGONAL),
    SYM15("sym15", "Symlet 15", WaveletType.ORTHOGONAL),
    SYM16("sym16", "Symlet 16", WaveletType.ORTHOGONAL),
    SYM17("sym17", "Symlet 17", WaveletType.ORTHOGONAL),
    SYM18("sym18", "Symlet 18", WaveletType.ORTHOGONAL),
    SYM19("sym19", "Symlet 19", WaveletType.ORTHOGONAL),
    SYM20("sym20", "Symlet 20", WaveletType.ORTHOGONAL),
    
    // Coiflet wavelets
    COIF1("coif1", "Coiflet 1", WaveletType.ORTHOGONAL),
    COIF2("coif2", "Coiflet 2", WaveletType.ORTHOGONAL),
    COIF3("coif3", "Coiflet 3", WaveletType.ORTHOGONAL),
    COIF4("coif4", "Coiflet 4", WaveletType.ORTHOGONAL),
    COIF5("coif5", "Coiflet 5", WaveletType.ORTHOGONAL),
    COIF6("coif6", "Coiflet 6", WaveletType.ORTHOGONAL),
    COIF7("coif7", "Coiflet 7", WaveletType.ORTHOGONAL),
    COIF8("coif8", "Coiflet 8", WaveletType.ORTHOGONAL),
    COIF9("coif9", "Coiflet 9", WaveletType.ORTHOGONAL),
    COIF10("coif10", "Coiflet 10", WaveletType.ORTHOGONAL),
    COIF11("coif11", "Coiflet 11", WaveletType.ORTHOGONAL),
    COIF12("coif12", "Coiflet 12", WaveletType.ORTHOGONAL),
    COIF13("coif13", "Coiflet 13", WaveletType.ORTHOGONAL),
    COIF14("coif14", "Coiflet 14", WaveletType.ORTHOGONAL),
    COIF15("coif15", "Coiflet 15", WaveletType.ORTHOGONAL),
    COIF16("coif16", "Coiflet 16", WaveletType.ORTHOGONAL),
    COIF17("coif17", "Coiflet 17", WaveletType.ORTHOGONAL),
    
    // Discrete Meyer wavelet
    DMEY("dmey", "Discrete Meyer wavelet", WaveletType.ORTHOGONAL),
    
    // Battle-Lemarié spline wavelets
    BLEM1("blem1", "Linear Battle-Lemarié", WaveletType.ORTHOGONAL),
    BLEM2("blem2", "Quadratic Battle-Lemarié", WaveletType.ORTHOGONAL),
    BLEM3("blem3", "Cubic Battle-Lemarié", WaveletType.ORTHOGONAL),
    BLEM4("blem4", "Quartic Battle-Lemarié", WaveletType.ORTHOGONAL),
    BLEM5("blem5", "Quintic Battle-Lemarié", WaveletType.ORTHOGONAL),
    
    // Biorthogonal wavelets - BIOR family
    BIOR1_1("bior1.1", "Biorthogonal 1.1", WaveletType.BIORTHOGONAL),
    BIOR1_3("bior1.3", "Biorthogonal 1.3", WaveletType.BIORTHOGONAL),
    BIOR1_5("bior1.5", "Biorthogonal 1.5", WaveletType.BIORTHOGONAL),
    BIOR2_2("bior2.2", "Biorthogonal 2.2", WaveletType.BIORTHOGONAL),
    BIOR2_4("bior2.4", "Biorthogonal 2.4", WaveletType.BIORTHOGONAL),
    BIOR2_6("bior2.6", "Biorthogonal 2.6", WaveletType.BIORTHOGONAL),
    BIOR2_8("bior2.8", "Biorthogonal 2.8", WaveletType.BIORTHOGONAL),
    BIOR3_1("bior3.1", "Biorthogonal 3.1", WaveletType.BIORTHOGONAL),
    BIOR3_3("bior3.3", "Biorthogonal 3.3", WaveletType.BIORTHOGONAL),
    BIOR3_5("bior3.5", "Biorthogonal 3.5", WaveletType.BIORTHOGONAL),
    BIOR3_7("bior3.7", "Biorthogonal 3.7", WaveletType.BIORTHOGONAL),
    BIOR3_9("bior3.9", "Biorthogonal 3.9", WaveletType.BIORTHOGONAL),
    BIOR4_4("bior4.4", "Biorthogonal 4.4 (JPEG2000)", WaveletType.BIORTHOGONAL),
    BIOR5_5("bior5.5", "Biorthogonal 5.5", WaveletType.BIORTHOGONAL),
    BIOR6_8("bior6.8", "Biorthogonal 6.8", WaveletType.BIORTHOGONAL),
    
    // Reverse Biorthogonal wavelets - RBIO family
    RBIO1_1("rbio1.1", "Reverse Biorthogonal 1.1", WaveletType.BIORTHOGONAL),
    RBIO1_3("rbio1.3", "Reverse Biorthogonal 1.3", WaveletType.BIORTHOGONAL),
    RBIO1_5("rbio1.5", "Reverse Biorthogonal 1.5", WaveletType.BIORTHOGONAL),
    RBIO2_2("rbio2.2", "Reverse Biorthogonal 2.2", WaveletType.BIORTHOGONAL),
    RBIO2_4("rbio2.4", "Reverse Biorthogonal 2.4", WaveletType.BIORTHOGONAL),
    RBIO2_6("rbio2.6", "Reverse Biorthogonal 2.6", WaveletType.BIORTHOGONAL),
    RBIO2_8("rbio2.8", "Reverse Biorthogonal 2.8", WaveletType.BIORTHOGONAL),
    RBIO3_1("rbio3.1", "Reverse Biorthogonal 3.1", WaveletType.BIORTHOGONAL),
    RBIO3_3("rbio3.3", "Reverse Biorthogonal 3.3", WaveletType.BIORTHOGONAL),
    RBIO3_5("rbio3.5", "Reverse Biorthogonal 3.5", WaveletType.BIORTHOGONAL),
    RBIO3_7("rbio3.7", "Reverse Biorthogonal 3.7", WaveletType.BIORTHOGONAL),
    RBIO3_9("rbio3.9", "Reverse Biorthogonal 3.9", WaveletType.BIORTHOGONAL),
    RBIO4_4("rbio4.4", "Reverse Biorthogonal 4.4", WaveletType.BIORTHOGONAL),
    RBIO5_5("rbio5.5", "Reverse Biorthogonal 5.5", WaveletType.BIORTHOGONAL),
    RBIO6_8("rbio6.8", "Reverse Biorthogonal 6.8", WaveletType.BIORTHOGONAL),
    
    // Continuous wavelets
    MORLET("morl", "Morlet wavelet", WaveletType.CONTINUOUS),
    MEXICAN_HAT("mexh", "Mexican Hat wavelet", WaveletType.CONTINUOUS),
    GAUSSIAN("gaus", "Gaussian wavelet", WaveletType.CONTINUOUS),
    PAUL("paul", "Paul wavelet", WaveletType.CONTINUOUS),
    DOG("dog", "Derivative of Gaussian", WaveletType.CONTINUOUS),
    SHANNON("shan", "Shannon wavelet", WaveletType.CONTINUOUS),
    FBSP("fbsp", "Frequency B-Spline", WaveletType.CONTINUOUS),
    CMOR("cmor", "Complex Morlet", WaveletType.COMPLEX),
    CGAU("cgau", "Complex Gaussian", WaveletType.COMPLEX),
    
    // Additional continuous wavelets (added in 1.4.0)
    CSHAN("cshan", "Complex Shannon wavelet", WaveletType.COMPLEX),
    MEYER("meyr", "Meyer wavelet", WaveletType.CONTINUOUS),
    MORSE("morse", "Morse wavelet", WaveletType.COMPLEX),
    RICKER("ricker", "Ricker wavelet", WaveletType.CONTINUOUS),
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
    
    
    @Override
    public String toString() {
        return code;
    }
}