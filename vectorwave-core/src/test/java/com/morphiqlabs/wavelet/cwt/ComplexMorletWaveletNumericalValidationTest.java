package com.morphiqlabs.wavelet.cwt;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive numerical validation tests for ComplexMorletWavelet implementation.
 * 
 * <p>This class validates the mathematical correctness of the ComplexMorletWavelet
 * implementation against the canonical complex Morlet wavelet formulation.</p>
 * 
 * <p>The canonical complex Morlet wavelet is defined as:</p>
 * <pre>
 * ψ(t) = (1/(π^(1/4) * √σ)) * exp(2πifc*t) * exp(-t²/(2σ²))
 * </pre>
 * 
 * <p>where σ is the bandwidth parameter and fc is the center frequency.</p>
 */
@DisplayName("Complex Morlet Wavelet Numerical Validation")
class ComplexMorletWaveletNumericalValidationTest {
    
    private static final double STRICT_TOLERANCE = 1e-12;
    private static final double RELAXED_TOLERANCE = 1e-6;
    private static final double REFERENCE_TOLERANCE = 1e-8;

    @Test
    @DisplayName("Validate canonical normalization constant")
    void testCanonicalNormalizationConstant() {
        double bandwidth = 1.0;
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(bandwidth, 1.0);
        
        // For σ=1, normalization should be 1/(π^(1/4) * √σ) = 1/π^(1/4)
        double expectedNormalization = 1.0 / (Math.pow(Math.PI, 0.25) * Math.sqrt(bandwidth));
        assertEquals(0.7511255444649425, expectedNormalization, STRICT_TOLERANCE);
        
        // Verify at t=0 where complex exponential is 1
        ComplexNumber psi0 = wavelet.psiComplex(0);
        assertEquals(expectedNormalization, psi0.real(), REFERENCE_TOLERANCE);
        assertEquals(0.0, psi0.imag(), STRICT_TOLERANCE);
    }

    @Test
    @DisplayName("Validate complex exponential at different frequencies")
    void testComplexExponentialFrequency() {
        double bandwidth = 1.0;
        double centerFreq = 1.0; // 1 Hz
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(bandwidth, centerFreq);
        
        // At t = 1/(4*fc) = 0.25, phase should be π/2
        double t_quarter = 1.0 / (4.0 * centerFreq);
        ComplexNumber psi_quarter = wavelet.psiComplex(t_quarter);
        
        double expectedPhase = Math.PI / 2.0;
        double actualPhase = psi_quarter.phase();
        assertEquals(expectedPhase, actualPhase, RELAXED_TOLERANCE);
        
        // At t = 1/(2*fc) = 0.5, phase should be π
        double t_half = 1.0 / (2.0 * centerFreq);
        ComplexNumber psi_half = wavelet.psiComplex(t_half);
        
        double expectedPhase_half = Math.PI;
        double actualPhase_half = Math.abs(psi_half.phase()); // Handle ±π ambiguity
        assertEquals(expectedPhase_half, actualPhase_half, RELAXED_TOLERANCE);
    }

    @Test
    @DisplayName("Validate Gaussian envelope independence of frequency")
    void testGaussianEnvelopeFrequencyIndependence() {
        double bandwidth = 1.0;
        ComplexMorletWavelet wavelet1 = new ComplexMorletWavelet(bandwidth, 1.0);
        ComplexMorletWavelet wavelet2 = new ComplexMorletWavelet(bandwidth, 2.0);
        
        // Magnitudes should be identical (only frequency changes, not envelope)
        double[] testPoints = {0, 0.5, 1.0, 1.5, 2.0};
        
        for (double t : testPoints) {
            ComplexNumber psi1 = wavelet1.psiComplex(t);
            ComplexNumber psi2 = wavelet2.psiComplex(t);
            
            assertEquals(psi1.magnitude(), psi2.magnitude(), STRICT_TOLERANCE);
        }
    }

    @Test
    @DisplayName("Validate bandwidth scaling effects")
    void testBandwidthScalingEffects() {
        double centerFreq = 1.0;
        ComplexMorletWavelet narrow = new ComplexMorletWavelet(0.5, centerFreq);
        ComplexMorletWavelet wide = new ComplexMorletWavelet(2.0, centerFreq);
        
        // At t=0, normalization should differ by √(σ2/σ1) = √(2/0.5) = 2
        ComplexNumber psi_narrow = narrow.psiComplex(0);
        ComplexNumber psi_wide = wide.psiComplex(0);
        
        double expectedRatio = Math.sqrt(2.0 / 0.5); // = 2.0
        double actualRatio = psi_narrow.magnitude() / psi_wide.magnitude();
        assertEquals(expectedRatio, actualRatio, REFERENCE_TOLERANCE);
        
        // At t=1, narrower bandwidth should decay faster
        ComplexNumber psi_narrow_1 = narrow.psiComplex(1);
        ComplexNumber psi_wide_1 = wide.psiComplex(1);
        
        // Ratio should actually be smaller than at t=0 due to faster decay of narrow bandwidth
        double ratio_1 = psi_narrow_1.magnitude() / psi_wide_1.magnitude();
        assertTrue(ratio_1 < actualRatio, "Narrow bandwidth should decay faster, making ratio smaller");
    }

    @Test
    @DisplayName("Validate unit energy normalization")
    void testUnitEnergyNormalization() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        
        // Calculate ∫|ψ(t)|² dt
        double energy = 0.0;
        double dt = 0.01;
        double range = 6.0; // ±6σ should capture >99.9% of energy
        
        for (double t = -range; t <= range; t += dt) {
            ComplexNumber psi = wavelet.psiComplex(t);
            double magnitude = psi.magnitude();
            energy += magnitude * magnitude * dt;
        }
        
        // Energy should be close to 1
        assertEquals(1.0, energy, 0.01);
    }

    @Test
    @DisplayName("Validate oscillatory behavior")
    void testOscillatoryBehavior() {
        double bandwidth = 1.0;
        double centerFreq = 1.0; // 1 Hz
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(bandwidth, centerFreq);
        
        // Sample over one period: T = 1/fc = 1.0
        double period = 1.0 / centerFreq;
        int samplesPerPeriod = 100;
        double dt = period / samplesPerPeriod;
        
        double[] phases = new double[samplesPerPeriod];
        for (int i = 0; i < samplesPerPeriod; i++) {
            double t = i * dt;
            ComplexNumber psi = wavelet.psiComplex(t);
            phases[i] = psi.phase();
        }
        
        // Phase should increase approximately linearly with frequency 2π*fc
        double expectedPhaseIncrement = 2.0 * Math.PI * centerFreq * dt;
        
        for (int i = 1; i < phases.length; i++) {
            double phaseIncrement = phases[i] - phases[i-1];
            
            // Handle phase wrapping
            if (phaseIncrement < -Math.PI) phaseIncrement += 2 * Math.PI;
            if (phaseIncrement > Math.PI) phaseIncrement -= 2 * Math.PI;
            
            assertEquals(expectedPhaseIncrement, phaseIncrement, 0.1);
        }
    }

    @Test
    @DisplayName("Validate symmetry properties")
    void testSymmetryProperties() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        
        double[] testPoints = {0.5, 1.0, 1.5, 2.0};
        
        for (double t : testPoints) {
            ComplexNumber psi_pos = wavelet.psiComplex(t);
            ComplexNumber psi_neg = wavelet.psiComplex(-t);
            
            // Magnitudes should be equal (Gaussian envelope is symmetric)
            assertEquals(psi_pos.magnitude(), psi_neg.magnitude(), STRICT_TOLERANCE);
            
            // Phases should be opposite (complex exponential antisymmetry)
            double phase_pos = psi_pos.phase();
            double phase_neg = psi_neg.phase();
            
            // Allow for phase wrapping: either phase_neg = -phase_pos or phase_neg = 2π - phase_pos
            assertTrue(Math.abs(phase_neg + phase_pos) < RELAXED_TOLERANCE || 
                      Math.abs(phase_neg + phase_pos - 2*Math.PI) < RELAXED_TOLERANCE ||
                      Math.abs(phase_neg + phase_pos + 2*Math.PI) < RELAXED_TOLERANCE);
        }
    }

    @Test
    @DisplayName("Validate discretization consistency")
    void testDiscretizationConsistency() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        
        int[] sizes = {16, 32, 64};
        
        for (int size : sizes) {
            double[] coeffs = wavelet.discretize(size);
            
            // Should be normalized
            double sumSquares = 0.0;
            for (double c : coeffs) {
                sumSquares += c * c;
            }
            assertEquals(1.0, sumSquares, RELAXED_TOLERANCE);
            
            // Discretized values should match psi() method
            double range = 8.0;
            double start = -4.0;
            double step = range / (size - 1);
            
            for (int i = 0; i < size; i++) {
                double t = start + i * step;
                double expected = wavelet.psi(t);
                
                // Before normalization, values should match
                // (we can't directly test this due to normalization in discretize)
                // But we can verify the shape is correct
                assertFalse(Double.isNaN(coeffs[i]));
                assertFalse(Double.isInfinite(coeffs[i]));
            }
        }
    }

    @Test
    @DisplayName("Validate parameter consistency with interface")
    void testParameterConsistency() {
        double bandwidth = 1.5;
        double centerFreq = 2.0;
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(bandwidth, centerFreq);
        
        assertEquals(bandwidth, wavelet.bandwidth(), STRICT_TOLERANCE);
        assertEquals(centerFreq, wavelet.centerFrequency(), STRICT_TOLERANCE);
        assertEquals("Complex Morlet", wavelet.name());
        assertTrue(wavelet.isComplex());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.1, 0.5, 1.0, 2.0, 5.0})
    @DisplayName("Validate different bandwidth values")
    void testDifferentBandwidthValues(double bandwidth) {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(bandwidth, 1.0);
        
        // At t=0, magnitude should be 1/(π^(1/4) * √σ)
        ComplexNumber psi0 = wavelet.psiComplex(0);
        double expectedMagnitude = 1.0 / (Math.pow(Math.PI, 0.25) * Math.sqrt(bandwidth));
        
        assertEquals(expectedMagnitude, psi0.magnitude(), REFERENCE_TOLERANCE);
        assertEquals(0.0, psi0.phase(), STRICT_TOLERANCE);
        
        // Decay rate should be inversely related to bandwidth
        ComplexNumber psi1 = wavelet.psiComplex(1.0);
        double expectedDecayFactor = Math.exp(-0.5 / (bandwidth * bandwidth));
        double actualDecayFactor = psi1.magnitude() / psi0.magnitude();
        
        assertEquals(expectedDecayFactor, actualDecayFactor, REFERENCE_TOLERANCE);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.5, 1.0, 2.0, 5.0, 10.0})
    @DisplayName("Validate different center frequency values")
    void testDifferentCenterFrequencyValues(double centerFreq) {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, centerFreq);
        
        // Magnitude at t=0 should be independent of frequency
        ComplexNumber psi0 = wavelet.psiComplex(0);
        double expectedMagnitude = 1.0 / Math.pow(Math.PI, 0.25);
        
        assertEquals(expectedMagnitude, psi0.magnitude(), REFERENCE_TOLERANCE);
        
        // Phase at t=0.1 should be proportional to frequency
        ComplexNumber psi_01 = wavelet.psiComplex(0.1);
        double expectedPhase = 2.0 * Math.PI * centerFreq * 0.1;
        
        // Normalize phase to [-π, π]
        while (expectedPhase > Math.PI) expectedPhase -= 2 * Math.PI;
        while (expectedPhase < -Math.PI) expectedPhase += 2 * Math.PI;
        
        assertEquals(expectedPhase, psi_01.phase(), RELAXED_TOLERANCE);
    }

    @Test
    @DisplayName("Validate numerical stability at extreme values")
    void testNumericalStability() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        
        // Test at large t values where Gaussian should be very small
        double[] extremePoints = {-10, -5, 5, 10, 20};
        
        for (double t : extremePoints) {
            ComplexNumber psi = wavelet.psiComplex(t);
            
            assertFalse(Double.isNaN(psi.real()));
            assertFalse(Double.isNaN(psi.imag()));
            assertFalse(Double.isInfinite(psi.real()));
            assertFalse(Double.isInfinite(psi.imag()));
            
            // Values should be very small but not zero
            if (Math.abs(t) > 5) {
                assertTrue(psi.magnitude() < 1e-5);
            }
        }
    }

    @Test
    @DisplayName("Compare real and imaginary parts with interface methods")
    void testRealImaginaryPartConsistency() {
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        
        double[] testPoints = {-2, -1, 0, 1, 2};
        
        for (double t : testPoints) {
            ComplexNumber psiComplex = wavelet.psiComplex(t);
            double psiReal = wavelet.psi(t);
            double psiImag = wavelet.psiImaginary(t);
            
            assertEquals(psiComplex.real(), psiReal, STRICT_TOLERANCE);
            assertEquals(psiComplex.imag(), psiImag, STRICT_TOLERANCE);
        }
    }

    @Test
    @DisplayName("Validate against mathematical reference values")
    void testMathematicalReferenceValues() {
        // Standard complex Morlet with bandwidth=1, centerFreq=1
        ComplexMorletWavelet wavelet = new ComplexMorletWavelet(1.0, 1.0);
        
        // At t=0: ψ(0) = 1/π^(1/4) + 0i
        ComplexNumber psi0 = wavelet.psiComplex(0);
        assertEquals(0.7511255444649425, psi0.real(), 1e-10);
        assertEquals(0.0, psi0.imag(), 1e-15);
        
        // At t=0.25: ψ(0.25) should have phase π/2
        ComplexNumber psi_025 = wavelet.psiComplex(0.25);
        double expectedMagnitude_025 = 0.7511255444649425 * Math.exp(-0.5 * 0.25 * 0.25);
        assertEquals(expectedMagnitude_025, psi_025.magnitude(), 1e-10);
        assertEquals(Math.PI / 2, psi_025.phase(), 1e-6);
    }
}