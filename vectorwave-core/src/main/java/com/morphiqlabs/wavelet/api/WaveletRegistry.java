package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.cwt.*;
import com.morphiqlabs.wavelet.cwt.finance.*;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Registry for wavelets using type-safe enum lookups.
 * Provides efficient wavelet access through the WaveletName enum.
 *
 * @since 1.0.0
 */
public final class WaveletRegistry {
    
    private static final Map<WaveletName, Wavelet> WAVELETS = new ConcurrentHashMap<>();
    
    static {
        // Register all wavelets using enum keys
        // Orthogonal wavelets
        register(WaveletName.HAAR, Haar.INSTANCE);
        
        // Daubechies wavelets
        register(WaveletName.DB2, Daubechies.DB2);
        register(WaveletName.DB4, Daubechies.DB4);
        register(WaveletName.DB6, Daubechies.DB6);
        register(WaveletName.DB8, Daubechies.DB8);
        register(WaveletName.DB10, Daubechies.DB10);
        
        // Extended Daubechies wavelets (DB12-DB20)
        register(WaveletName.DB12, Daubechies.DB12);
        register(WaveletName.DB14, Daubechies.DB14);
        register(WaveletName.DB16, Daubechies.DB16);
        register(WaveletName.DB18, Daubechies.DB18);
        register(WaveletName.DB20, Daubechies.DB20);
        
        // Advanced Extended Daubechies wavelets (DB22-DB38)
        register(WaveletName.DB22, Daubechies.DB22);
        register(WaveletName.DB24, Daubechies.DB24);
        register(WaveletName.DB26, Daubechies.DB26);
        register(WaveletName.DB28, Daubechies.DB28);
        register(WaveletName.DB30, Daubechies.DB30);
        register(WaveletName.DB32, Daubechies.DB32);
        register(WaveletName.DB34, Daubechies.DB34);
        register(WaveletName.DB36, Daubechies.DB36);
        register(WaveletName.DB38, Daubechies.DB38);
        // DB38 is the maximum supported (PyWavelets limit)
        
        // Symlet wavelets
        register(WaveletName.SYM2, Symlet.SYM2);
        register(WaveletName.SYM3, Symlet.SYM3);
        register(WaveletName.SYM4, Symlet.SYM4);
        register(WaveletName.SYM5, Symlet.SYM5);
        register(WaveletName.SYM6, Symlet.SYM6);
        register(WaveletName.SYM7, Symlet.SYM7);
        register(WaveletName.SYM8, Symlet.SYM8);
        register(WaveletName.SYM9, Symlet.SYM9);
        register(WaveletName.SYM10, Symlet.SYM10);
        register(WaveletName.SYM11, Symlet.SYM11);
        register(WaveletName.SYM12, Symlet.SYM12);
        register(WaveletName.SYM13, Symlet.SYM13);
        register(WaveletName.SYM14, Symlet.SYM14);
        register(WaveletName.SYM15, Symlet.SYM15);
        register(WaveletName.SYM16, Symlet.SYM16);
        register(WaveletName.SYM17, Symlet.SYM17);
        register(WaveletName.SYM18, Symlet.SYM18);
        register(WaveletName.SYM19, Symlet.SYM19);
        register(WaveletName.SYM20, Symlet.SYM20);
        
        // Coiflet wavelets
        register(WaveletName.COIF1, Coiflet.COIF1);
        register(WaveletName.COIF2, Coiflet.COIF2);
        register(WaveletName.COIF3, Coiflet.COIF3);
        register(WaveletName.COIF4, Coiflet.COIF4);
        register(WaveletName.COIF5, Coiflet.COIF5);
        register(WaveletName.COIF6, Coiflet.COIF6);
        register(WaveletName.COIF7, Coiflet.COIF7);
        register(WaveletName.COIF8, Coiflet.COIF8);
        register(WaveletName.COIF9, Coiflet.COIF9);
        register(WaveletName.COIF10, Coiflet.COIF10);
        register(WaveletName.COIF11, Coiflet.COIF11);
        register(WaveletName.COIF12, Coiflet.COIF12);
        register(WaveletName.COIF13, Coiflet.COIF13);
        register(WaveletName.COIF14, Coiflet.COIF14);
        register(WaveletName.COIF15, Coiflet.COIF15);
        register(WaveletName.COIF16, Coiflet.COIF16);
        register(WaveletName.COIF17, Coiflet.COIF17);
        
        // Discrete Meyer wavelet
        register(WaveletName.DMEY, DiscreteMeyer.DMEY);
        
        // Battle-Lemarié wavelets
        register(WaveletName.BLEM1, BattleLemarieWavelet.BLEM1);
        register(WaveletName.BLEM2, BattleLemarieWavelet.BLEM2);
        register(WaveletName.BLEM3, BattleLemarieWavelet.BLEM3);
        register(WaveletName.BLEM4, BattleLemarieWavelet.BLEM4);
        register(WaveletName.BLEM5, BattleLemarieWavelet.BLEM5);
        
        // Biorthogonal wavelets
        register(WaveletName.BIOR1_1, BiorthogonalSpline.BIOR1_1);
        register(WaveletName.BIOR1_3, BiorthogonalSpline.BIOR1_3);
        register(WaveletName.BIOR1_5, BiorthogonalSpline.BIOR1_5);
        register(WaveletName.BIOR2_2, BiorthogonalSpline.BIOR2_2);
        register(WaveletName.BIOR2_4, BiorthogonalSpline.BIOR2_4);
        register(WaveletName.BIOR2_6, BiorthogonalSpline.BIOR2_6);
        register(WaveletName.BIOR2_8, BiorthogonalSpline.BIOR2_8);
        register(WaveletName.BIOR3_1, BiorthogonalSpline.BIOR3_1);
        register(WaveletName.BIOR3_3, BiorthogonalSpline.BIOR3_3);
        register(WaveletName.BIOR3_5, BiorthogonalSpline.BIOR3_5);
        register(WaveletName.BIOR3_7, BiorthogonalSpline.BIOR3_7);
        register(WaveletName.BIOR3_9, BiorthogonalSpline.BIOR3_9);
        register(WaveletName.BIOR4_4, BiorthogonalSpline.BIOR4_4);
        register(WaveletName.BIOR5_5, BiorthogonalSpline.BIOR5_5);
        register(WaveletName.BIOR6_8, BiorthogonalSpline.BIOR6_8);
        
        // Reverse Biorthogonal wavelets
        register(WaveletName.RBIO1_1, ReverseBiorthogonalSpline.RBIO1_1);
        register(WaveletName.RBIO1_3, ReverseBiorthogonalSpline.RBIO1_3);
        register(WaveletName.RBIO1_5, ReverseBiorthogonalSpline.RBIO1_5);
        register(WaveletName.RBIO2_2, ReverseBiorthogonalSpline.RBIO2_2);
        register(WaveletName.RBIO2_4, ReverseBiorthogonalSpline.RBIO2_4);
        register(WaveletName.RBIO2_6, ReverseBiorthogonalSpline.RBIO2_6);
        register(WaveletName.RBIO2_8, ReverseBiorthogonalSpline.RBIO2_8);
        register(WaveletName.RBIO3_1, ReverseBiorthogonalSpline.RBIO3_1);
        register(WaveletName.RBIO3_3, ReverseBiorthogonalSpline.RBIO3_3);
        register(WaveletName.RBIO3_5, ReverseBiorthogonalSpline.RBIO3_5);
        register(WaveletName.RBIO3_7, ReverseBiorthogonalSpline.RBIO3_7);
        register(WaveletName.RBIO3_9, ReverseBiorthogonalSpline.RBIO3_9);
        register(WaveletName.RBIO4_4, ReverseBiorthogonalSpline.RBIO4_4);
        register(WaveletName.RBIO5_5, ReverseBiorthogonalSpline.RBIO5_5);
        register(WaveletName.RBIO6_8, ReverseBiorthogonalSpline.RBIO6_8);
        
        // Continuous wavelets
        register(WaveletName.MORLET, new MorletWavelet());  // Uses default params (omega0=6, sigma=1)
        register(WaveletName.MEXICAN_HAT, new MATLABMexicanHat());  // MATLAB-compatible Mexican Hat
        register(WaveletName.GAUSSIAN, new GaussianDerivativeWavelet(1));  // Gaussian derivative (1st order)
        register(WaveletName.PAUL, new PaulWavelet(4));  // Paul wavelet with default order 4
        register(WaveletName.DOG, new DOGWavelet(2));  // Derivative of Gaussian with default order 2
        register(WaveletName.SHANNON, new com.morphiqlabs.wavelet.cwt.finance.ClassicalShannonWavelet());  // Classical Shannon wavelet
        register(WaveletName.FBSP, new FrequencyBSplineWavelet(2, 1.0, 1.0));  // Frequency B-Spline with default params
        register(WaveletName.CMOR, new ComplexMorletWavelet(1.0, 1.0));  // Complex Morlet with default params
        register(WaveletName.CGAU, new ComplexGaussianWavelet(1, 1.0, 5.0));  // Complex Gaussian wavelet
        
        // Additional continuous wavelets (added in 1.4.0)
        register(WaveletName.CSHAN, new ComplexShannonWavelet());  // Complex Shannon with default params (fb=1, fc=1)
        register(WaveletName.MEYER, new ContinuousMeyerWavelet());  // Meyer wavelet
        register(WaveletName.MORSE, new MorseWavelet());  // Morse wavelet with default params (beta=3, gamma=60)
        register(WaveletName.RICKER, new RickerWavelet());  // Ricker wavelet with default sigma=1
        register(WaveletName.HERMITIAN, new HermitianWavelet(2));  // Hermitian wavelet with default order 2
    }
    
    private static void register(WaveletName name, Wavelet wavelet) {
        WAVELETS.put(name, wavelet);
    }
    
    /**
     * Get a wavelet by its enum name.
     * @param name the wavelet name enum
     * @return the corresponding Wavelet instance
     * @throws InvalidArgumentException if the wavelet is not registered
     */
    public static Wavelet getWavelet(WaveletName name) {
        if (name == null) {
            throw new InvalidArgumentException("Wavelet name cannot be null");
        }
        
        Wavelet w = WAVELETS.get(name);
        if (w == null) {
            throw new InvalidArgumentException("Wavelet not registered: " + name);
        }
        return w;
    }
    
    /**
     * Check if a wavelet is available.
     * @param name the wavelet name enum
     * @return true if the wavelet is registered, false otherwise
     */
    public static boolean hasWavelet(WaveletName name) {
        return name != null && WAVELETS.containsKey(name);
    }
    
    /**
     * Get all available wavelet names.
     * @return set of available wavelet names
     */
    public static Set<WaveletName> getAvailableWavelets() {
        return EnumSet.copyOf(WAVELETS.keySet());
    }
    
    /**
     * Get all orthogonal wavelet names.
     * @return list of orthogonal wavelet names
     */
    public static List<WaveletName> getOrthogonalWavelets() {
        return WAVELETS.entrySet().stream()
            .filter(e -> e.getValue().getType() == WaveletType.ORTHOGONAL)
            .map(Map.Entry::getKey)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get all continuous wavelet names.
     * @return list of continuous wavelet names
     */
    public static List<WaveletName> getContinuousWavelets() {
        return WAVELETS.entrySet().stream()
            .filter(e -> e.getValue().getType() == WaveletType.CONTINUOUS)
            .map(Map.Entry::getKey)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get all biorthogonal wavelet names.
     * @return list of biorthogonal wavelet names
     */
    public static List<WaveletName> getBiorthogonalWavelets() {
        return WAVELETS.entrySet().stream()
            .filter(e -> e.getValue().getType() == WaveletType.BIORTHOGONAL)
            .map(Map.Entry::getKey)
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get wavelets by their type category.
     * @param type the wavelet type
     * @return set of wavelet names matching the type
     */
    public static Set<WaveletName> getWaveletsByType(WaveletType type) {
        if (type == null) {
            return Collections.emptySet();
        }
        
        return WAVELETS.entrySet().stream()
            .filter(e -> e.getValue().getType() == type)
            .map(Map.Entry::getKey)
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(WaveletName.class)));
    }
    
    /**
     * Check if a wavelet is available.
     * @param name the wavelet name enum
     * @return true if the wavelet is registered, false otherwise
     */
    public static boolean isWaveletAvailable(WaveletName name) {
        return hasWavelet(name);
    }
    
    /**
     * Get Daubechies wavelets by order.
     * @return list of Daubechies wavelet names
     */
    public static List<WaveletName> getDaubechiesWavelets() {
        return Stream.of(WaveletName.values())
            .filter(name -> name.name().startsWith("DB"))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get Symlet wavelets by order.
     * @return list of Symlet wavelet names
     */
    public static List<WaveletName> getSymletWavelets() {
        return Stream.of(WaveletName.values())
            .filter(name -> name.name().startsWith("SYM"))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get Coiflet wavelets by order.
     * @return list of Coiflet wavelet names
     */
    public static List<WaveletName> getCoifletWavelets() {
        return Stream.of(WaveletName.values())
            .filter(name -> name.name().startsWith("COIF"))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Print all available wavelets with their descriptions.
     */
    public static void printAvailableWavelets() {
        System.Logger log = com.morphiqlabs.wavelet.util.Logging.getLogger(WaveletRegistry.class);
        log.log(System.Logger.Level.INFO, "Available Wavelets:");
        for (WaveletName name : getAvailableWavelets()) {
            Wavelet w = getWavelet(name);
            final String line = "  " + name + " (" + name.getCode() + ") - " + w.description();
            log.log(System.Logger.Level.INFO, line);
        }
    }
    
    // ============================================================
    // Transform Compatibility API
    // ============================================================
    
    /**
     * Get all transforms supported by a specific wavelet.
     * This provides explicit information about which transforms can be used
     * with the given wavelet, improving API discoverability.
     * 
     * @param waveletName the wavelet to check
     * @return set of compatible transform types
     */
    public static Set<TransformType> getSupportedTransforms(WaveletName waveletName) {
        if (waveletName == null) {
            return EnumSet.noneOf(TransformType.class);
        }
        
        WaveletType type = waveletName.getType();
        Set<TransformType> supported = EnumSet.noneOf(TransformType.class);
        
        for (TransformType transform : TransformType.values()) {
            if (transform.isCompatibleWith(type)) {
                supported.add(transform);
            }
        }
        
        return supported;
    }
    
    /**
     * Check if a specific wavelet can be used with a specific transform.
     * 
     * @param waveletName the wavelet to check
     * @param transformType the transform type
     * @return true if the wavelet can be used with the transform
     */
    public static boolean isCompatible(WaveletName waveletName, TransformType transformType) {
        if (waveletName == null || transformType == null) {
            return false;
        }
        return transformType.isCompatibleWith(waveletName.getType());
    }
    
    /**
     * Get all wavelets that can be used with a specific transform.
     * Useful for populating UI dropdowns or validating user input.
     * 
     * @param transformType the transform type
     * @return list of compatible wavelet names
     */
    public static List<WaveletName> getWaveletsForTransform(TransformType transformType) {
        if (transformType == null) {
            return List.of();
        }
        
        return Stream.of(WaveletName.values())
            .filter(name -> transformType.isCompatibleWith(name.getType()))
            .sorted()
            .collect(Collectors.toList());
    }
    
    /**
     * Get recommended transform for a specific wavelet based on its type.
     * This helps users choose the most appropriate transform.
     * 
     * @param waveletName the wavelet
     * @return recommended transform type, or null if wavelet not found
     */
    public static TransformType getRecommendedTransform(WaveletName waveletName) {
        if (waveletName == null) {
            return null;
        }
        
        return switch (waveletName.getType()) {
            case ORTHOGONAL, BIORTHOGONAL -> TransformType.MODWT;
            case CONTINUOUS -> TransformType.CWT;
            case COMPLEX -> TransformType.CWT;
            default -> TransformType.MODWT; // Default fallback
        };
    }
    
    /**
     * Print transform compatibility matrix for all wavelets.
     * Useful for documentation and debugging.
     */
    public static void printTransformCompatibilityMatrix() {
        System.Logger log = com.morphiqlabs.wavelet.util.Logging.getLogger(WaveletRegistry.class);
        log.log(System.Logger.Level.INFO, "\nWavelet-Transform Compatibility Matrix:");
        log.log(System.Logger.Level.INFO, "=========================================");

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%-20s", "Wavelet"));
        for (TransformType transform : TransformType.values()) {
            sb.append(String.format("%-15s", transform.getCode()));
        }
        log.log(System.Logger.Level.INFO, sb.toString());

        log.log(System.Logger.Level.INFO, "-".repeat(20 + TransformType.values().length * 15));

        for (WaveletName wavelet : WaveletName.values()) {
            StringBuilder row = new StringBuilder();
            row.append(String.format("%-20s", wavelet.getCode()));
            for (TransformType transform : TransformType.values()) {
                String compatible = isCompatible(wavelet, transform) ? "✓" : "-";
                row.append(String.format("%-15s", compatible));
            }
            log.log(System.Logger.Level.INFO, row.toString());
        }
    }
    
    private WaveletRegistry() {}
}
