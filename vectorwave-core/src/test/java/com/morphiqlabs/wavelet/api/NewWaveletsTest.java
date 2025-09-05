package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.api.WaveletName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for newly added wavelets.
 * Validates mathematical correctness, registration, and basic functionality.
 */
class NewWaveletsTest {
    
    private static final double TOLERANCE = 1e-10;
    
    // Provide all new Daubechies wavelets for parameterized testing
    static Stream<Daubechies> newDaubechiesWavelets() {
        return Stream.of(
            Daubechies.DB6,
            Daubechies.DB8,
            Daubechies.DB10
        );
    }
    
    // Provide all new Symlet wavelets for parameterized testing
    static Stream<Symlet> newSymletWavelets() {
        return Stream.of(
            Symlet.SYM5,
            Symlet.SYM6,
            Symlet.SYM7,
            Symlet.SYM8,
            Symlet.SYM10,
            Symlet.SYM12,
            Symlet.SYM15,
            Symlet.SYM20
        );
    }
    
    // Provide all new Coiflet wavelets for parameterized testing
    static Stream<Coiflet> newCoifletWavelets() {
        return Stream.of(
            Coiflet.COIF4,
            Coiflet.COIF5
        );
    }
    
    
    @ParameterizedTest
    @MethodSource("newDaubechiesWavelets")
    @DisplayName("Daubechies wavelets should be registered in WaveletRegistry")
    void testDaubechiesRegistration(Daubechies wavelet) {
        // Map wavelet instance to enum
        WaveletName waveletName = getWaveletNameForInstance(wavelet);
        assertTrue(WaveletRegistry.hasWavelet(waveletName),
            wavelet.name() + " should be registered");
        
        Wavelet retrieved = WaveletRegistry.getWavelet(waveletName);
        assertNotNull(retrieved);
        assertEquals(wavelet.name(), retrieved.name());
        assertArrayEquals(wavelet.lowPassDecomposition(), 
                         retrieved.lowPassDecomposition(), 
                         TOLERANCE);
    }
    
    
    @ParameterizedTest
    @MethodSource("newSymletWavelets")
    @DisplayName("Symlet wavelets should be registered in WaveletRegistry")
    void testSymletRegistration(Symlet wavelet) {
        // Map wavelet instance to enum
        WaveletName waveletName = getWaveletNameForInstance(wavelet);
        assertTrue(WaveletRegistry.hasWavelet(waveletName),
            wavelet.name() + " should be registered");
        
        Wavelet retrieved = WaveletRegistry.getWavelet(waveletName);
        assertNotNull(retrieved);
        assertEquals(wavelet.name(), retrieved.name());
        assertArrayEquals(wavelet.lowPassDecomposition(), 
                         retrieved.lowPassDecomposition(), 
                         TOLERANCE);
    }
    
    
    @ParameterizedTest
    @MethodSource("newCoifletWavelets")
    @DisplayName("Coiflet wavelets should be registered in WaveletRegistry")
    void testCoifletRegistration(Coiflet wavelet) {
        // Map wavelet instance to enum
        WaveletName waveletName = getWaveletNameForInstance(wavelet);
        assertTrue(WaveletRegistry.hasWavelet(waveletName),
            wavelet.name() + " should be registered");
        
        Wavelet retrieved = WaveletRegistry.getWavelet(waveletName);
        assertNotNull(retrieved);
        assertEquals(wavelet.name(), retrieved.name());
        assertArrayEquals(wavelet.lowPassDecomposition(), 
                         retrieved.lowPassDecomposition(), 
                         TOLERANCE);
    }
    
    // Orthogonality, filter length, and sum/energy checks are covered by
    // WaveletCoefficientVerificationTest for all orders; avoid duplication here.
    
    @Test
    @DisplayName("All new wavelets should be listed in getOrthogonalWavelets()")
    void testAllNewWaveletsInOrthogonalList() {
        var orthogonalWavelets = WaveletRegistry.getOrthogonalWavelets();
        
        // Check new Daubechies wavelets
        assertTrue(orthogonalWavelets.contains(WaveletName.DB6));
        assertTrue(orthogonalWavelets.contains(WaveletName.DB8));
        assertTrue(orthogonalWavelets.contains(WaveletName.DB10));
        
        // Check new Symlet wavelets
        assertTrue(orthogonalWavelets.contains(WaveletName.SYM5));
        assertTrue(orthogonalWavelets.contains(WaveletName.SYM6));
        assertTrue(orthogonalWavelets.contains(WaveletName.SYM7));
        assertTrue(orthogonalWavelets.contains(WaveletName.SYM8));
        assertTrue(orthogonalWavelets.contains(WaveletName.SYM10));
        assertTrue(orthogonalWavelets.contains(WaveletName.SYM12));
        assertTrue(orthogonalWavelets.contains(WaveletName.SYM15));
        assertTrue(orthogonalWavelets.contains(WaveletName.SYM20));
        
        // Check new Coiflet wavelets
        assertTrue(orthogonalWavelets.contains(WaveletName.COIF4));
        assertTrue(orthogonalWavelets.contains(WaveletName.COIF5));
    }
    
    @Test
    @DisplayName("Registry should provide correct descriptions for new wavelets")
    void testWaveletDescriptions() {
        // Test Daubechies descriptions
        assertEquals("Daubechies wavelet of order 6", 
                    WaveletRegistry.getWavelet(WaveletName.DB6).description());
        assertEquals("Daubechies wavelet of order 8", 
                    WaveletRegistry.getWavelet(WaveletName.DB8).description());
        assertEquals("Daubechies wavelet of order 10", 
                    WaveletRegistry.getWavelet(WaveletName.DB10).description());
        
        // Test Symlet descriptions
        assertEquals("Symlet wavelet of order 5", 
                    WaveletRegistry.getWavelet(WaveletName.SYM5).description());
        assertEquals("Symlet wavelet of order 20", 
                    WaveletRegistry.getWavelet(WaveletName.SYM20).description());
        
        // Test Coiflet descriptions
        assertEquals("Coiflet wavelet of order 4", 
                    WaveletRegistry.getWavelet(WaveletName.COIF4).description());
        assertEquals("Coiflet wavelet of order 5", 
                    WaveletRegistry.getWavelet(WaveletName.COIF5).description());
    }
    
    // QMF relationship for orthogonal wavelets is validated in
    // WaveletCoefficientVerificationTest to avoid duplication here.
    
    @Test
    @DisplayName("Verify specific coefficient values for DB6")
    void testDB6SpecificCoefficients() {
        double[] coeffs = Daubechies.DB6.lowPassDecomposition();
        
        // Verify a few key coefficients against known values
        assertEquals(0.1115407433501094, coeffs[0], 1e-15);
        assertEquals(0.4946238903984530, coeffs[1], 1e-15);
        assertEquals(0.7511339080210954, coeffs[2], 1e-15);
        assertEquals(-0.0010773010853085, coeffs[11], 1e-15);
    }
    
    @Test
    @DisplayName("Verify specific coefficient values for SYM10")
    void testSYM10SpecificCoefficients() {
        double[] coeffs = Symlet.SYM10.lowPassDecomposition();
        
        assertEquals(20, coeffs.length, "SYM10 should have 20 coefficients");
        
        // Verify a few key coefficients (updated with PyWavelets values)
        assertEquals(0.0007701598091030, coeffs[0], 1e-15);
        assertEquals(0.0057649120335782, coeffs[14], 1e-15);
        assertEquals(-0.0004593294205334, coeffs[19], 1e-15);
    }
    
    @Test
    @DisplayName("Verify specific coefficient values for COIF5")
    void testCOIF5SpecificCoefficients() {
        double[] coeffs = Coiflet.COIF5.lowPassDecomposition();
        
        assertEquals(30, coeffs.length, "COIF5 should have 30 coefficients");
        
        // Verify a few key coefficients (updated with correct PyWavelets values)
        assertEquals(-0.0000000960401011, coeffs[0], 1e-15);
        assertEquals(0.7742936228603274, coeffs[19], 1e-10);
        assertEquals(-0.0002120818620675, coeffs[29], 1e-10);
    }
    
    /**
     * Helper method to map wavelet instances to their corresponding WaveletName enum.
     * This is needed for test parameterization since we pass wavelet instances
     * but need enum values for the registry.
     */
    private WaveletName getWaveletNameForInstance(Wavelet wavelet) {
        String name = wavelet.name();
        switch (name) {
            // Daubechies
            case "db6": return WaveletName.DB6;
            case "db8": return WaveletName.DB8;
            case "db10": return WaveletName.DB10;
            // Symlets
            case "sym5": return WaveletName.SYM5;
            case "sym6": return WaveletName.SYM6;
            case "sym7": return WaveletName.SYM7;
            case "sym8": return WaveletName.SYM8;
            case "sym10": return WaveletName.SYM10;
            case "sym12": return WaveletName.SYM12;
            case "sym15": return WaveletName.SYM15;
            case "sym20": return WaveletName.SYM20;
            // Coiflets
            case "coif4": return WaveletName.COIF4;
            case "coif5": return WaveletName.COIF5;
            default:
                throw new IllegalArgumentException("Unknown wavelet: " + name);
        }
    }
}
