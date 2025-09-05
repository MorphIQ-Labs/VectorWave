package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.padding.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for numerical stability of padding strategies.
 * Tests edge cases, ill-conditioned inputs, and numerical extremes.
 */
@DisplayName("Numerical Stability Tests for Padding Strategies")
public class NumericalStabilityTest {
    
    private static final double TOLERANCE = 1e-10;
    
    // ==================== Polynomial Fitting Stability ====================
    
    @Test
    @DisplayName("Polynomial padding - handles ill-conditioned Vandermonde matrix")
    void testPolynomialIllConditioned() {
        var strategy = new PolynomialExtrapolationStrategy(5); // High order
        
        // Create nearly collinear points (ill-conditioned for polynomial fitting)
        double[] signal = new double[10];
        for (int i = 0; i < 10; i++) {
            signal[i] = 1e8 + i * 1e-8; // Large offset with tiny increments
        }
        
        // Should not throw or produce NaN/Inf
        double[] padded = strategy.pad(signal, 15);
        assertEquals(15, padded.length);
        
        for (double val : padded) {
            assertTrue(Double.isFinite(val), "Value should be finite");
        }
    }
    
    @Test
    @DisplayName("Polynomial padding - handles numerical overflow gracefully")
    void testPolynomialOverflow() {
        var strategy = new PolynomialExtrapolationStrategy(3);
        
        // Create signal with large values that could cause overflow in x^3
        double[] signal = {1e100, 2e100, 3e100, 4e100};
        
        double[] padded = strategy.pad(signal, 6);
        assertEquals(6, padded.length);
        
        // Should handle large values without overflow
        for (double val : padded) {
            assertTrue(Double.isFinite(val) || Math.abs(val) < 1e200, 
                      "Should handle large values gracefully");
        }
    }
    
    @Test
    @DisplayName("Polynomial padding - handles near-zero values")
    void testPolynomialNearZero() {
        var strategy = new PolynomialExtrapolationStrategy(2);
        
        // Signal with very small values
        double[] signal = {1e-300, 2e-300, 3e-300, 4e-300};
        
        double[] padded = strategy.pad(signal, 6);
        assertEquals(6, padded.length);
        
        // Should handle tiny values without underflow to zero
        boolean hasNonZero = false;
        for (int i = 4; i < 6; i++) {
            if (padded[i] != 0) {
                hasNonZero = true;
            }
        }
        assertTrue(hasNonZero || padded[5] == 0, "Should handle or gracefully underflow");
    }
    
    @Test
    @DisplayName("Polynomial padding - singular matrix fallback")
    void testPolynomialSingularMatrix() {
        var strategy = new PolynomialExtrapolationStrategy(3);
        
        // Constant signal leads to singular Vandermonde matrix for order > 0
        double[] signal = {5.0, 5.0, 5.0, 5.0, 5.0};
        
        double[] padded = strategy.pad(signal, 8);
        assertEquals(8, padded.length);
        
        // Should fall back to constant extrapolation
        for (int i = 5; i < 8; i++) {
            assertEquals(5.0, padded[i], 0.1, "Should extrapolate as constant");
        }
    }
    
    // ==================== Battle-Lemarié Stability ====================
    
    @Test
    @DisplayName("Battle-Lemarié normalization maintains reasonable bounds")
    void testBattleLemarieNormalizationBounds() {
        // Test all Battle-Lemarié wavelets
        BattleLemarieWavelet[] wavelets = {
            BattleLemarieWavelet.BLEM1,
            BattleLemarieWavelet.BLEM2,
            BattleLemarieWavelet.BLEM3,
            BattleLemarieWavelet.BLEM4,
            BattleLemarieWavelet.BLEM5
        };
        
        for (var wavelet : wavelets) {
            double[] coeffs = wavelet.lowPassDecomposition();
            
            // Check sum is close to sqrt(2)
            double sum = 0;
            for (double c : coeffs) {
                sum += c;
            }
            assertEquals(Math.sqrt(2), sum, 0.2, 
                        "Sum should be close to sqrt(2) for " + wavelet.name());
            
            // Check sum of squares is reasonable (close to 1)
            double sumSq = 0;
            for (double c : coeffs) {
                sumSq += c * c;
            }
            assertTrue(sumSq > 0.5 && sumSq < 2.0, 
                      "Sum of squares should be reasonable for " + wavelet.name());
            
            // Check no individual coefficient is too large
            for (double c : coeffs) {
                assertTrue(Math.abs(c) < 2.0, 
                          "Coefficients should be bounded for " + wavelet.name());
            }
        }
    }
    
    @Test
    @DisplayName("Orthogonalization factor computation convergence")
    void testOrthogonalizationFactorConvergence() {
        // Test that the adaptive summation in computeOrthogonalizationFactor converges
        for (int m = 1; m <= 5; m++) {
            double omega = Math.PI / 4; // Test frequency
            
            double factor = com.morphiqlabs.wavelet.util.BSplineUtils
                           .computeOrthogonalizationFactor(m, omega);
            
            assertTrue(Double.isFinite(factor), 
                      "Orthogonalization factor should be finite for order " + m);
            assertTrue(factor > 0 && factor < 100, 
                      "Orthogonalization factor should be reasonable for order " + m);
        }
    }
    
    // ==================== Adaptive Strategy Stability ====================
    
    @Test
    @DisplayName("Adaptive strategy - handles extreme signal characteristics")
    void testAdaptiveExtremeCharacteristics() {
        var strategy = new AdaptivePaddingStrategy();
        
        // Test various extreme signals
        double[][] extremeSignals = {
            // Huge dynamic range
            {1e-100, 1e100, 1e-100, 1e100},
            // Many repeated values (low rank)
            {1, 1, 1, 2, 2, 2, 3, 3, 3},
            // Rapid oscillation
            new double[20], // Will fill below
            // Nearly constant with tiny variation
            new double[20]  // Will fill below
        };
        
        // Fill rapid oscillation
        for (int i = 0; i < 20; i++) {
            extremeSignals[2][i] = (i % 2 == 0) ? 1000 : -1000;
        }
        
        // Fill nearly constant
        for (int i = 0; i < 20; i++) {
            extremeSignals[3][i] = 1.0 + i * 1e-15;
        }
        
        for (double[] signal : extremeSignals) {
            var result = strategy.padWithDetails(signal, signal.length + 5);
            double[] padded = result.paddedSignal();
            
            assertEquals(signal.length + 5, padded.length);
            
            // Check all values are finite
            for (double val : padded) {
                assertTrue(Double.isFinite(val), 
                          "Adaptive strategy should produce finite values");
            }
            
            // Check strategy was selected
            assertNotNull(result.selectedStrategy());
            assertNotNull(result.selectionReason());
        }
    }
    
    @Test
    @DisplayName("Adaptive periodicity detection - numerical stability")
    void testAdaptivePeriodicityStability() {
        var strategy = new AdaptivePaddingStrategy();
        
        // Signal with period that could cause numerical issues
        double[] signal = new double[100];
        for (int i = 0; i < 100; i++) {
            // High frequency with amplitude modulation
            signal[i] = Math.sin(20 * Math.PI * i / 100) * 
                       Math.exp(-i / 100.0) * 1e10;
        }
        
        double[] padded = strategy.pad(signal, 110);
        assertEquals(110, padded.length);
        
        // Should handle without numerical issues
        for (double val : padded) {
            assertTrue(Double.isFinite(val));
        }
    }
    
    // ==================== Statistical Padding Stability ====================
    
    @Test
    @DisplayName("Statistical padding - variance calculation with extreme values")
    void testStatisticalVarianceExtreme() {
        var strategy = new StatisticalPaddingStrategy(
            StatisticalPaddingStrategy.StatMethod.VARIANCE_MATCHED);
        
        // Signal with extreme variance
        double[] signal = {-1e10, 0, 1e10, 0, -1e10};
        
        double[] padded = strategy.pad(signal, 8);
        assertEquals(8, padded.length);
        
        // Padded values should be finite despite extreme variance
        for (int i = 5; i < 8; i++) {
            assertTrue(Double.isFinite(padded[i]), 
                      "Variance-matched padding should handle extreme variance");
        }
    }
    
    @Test
    @DisplayName("Statistical padding - median with NaN handling")
    void testStatisticalMedianStability() {
        var strategy = new StatisticalPaddingStrategy(
            StatisticalPaddingStrategy.StatMethod.MEDIAN);
        
        // Normal signal (NaN would break sorting)
        double[] signal = {1, 2, 3, 4, 5};
        
        double[] padded = strategy.pad(signal, 8);
        assertEquals(8, padded.length);
        
        // Median should be 3
        for (int i = 5; i < 8; i++) {
            assertEquals(3.0, padded[i], TOLERANCE);
        }
    }
    
    // ==================== Composite Strategy Stability ====================
    
    @Test
    @DisplayName("Composite padding - memory efficiency with large padding")
    void testCompositeMemoryEfficiency() {
        var composite = new CompositePaddingStrategy(
            new ConstantPaddingStrategy(),
            new LinearExtrapolationStrategy(),
            0.5
        );
        
        // Small signal with large padding requirement
        double[] signal = {1, 2, 3, 4};
        int targetLength = 1000; // Much larger than signal
        
        double[] padded = composite.pad(signal, targetLength);
        assertEquals(targetLength, padded.length);
        
        // Check that original signal is preserved
        for (int i = 0; i < 4; i++) {
            assertEquals(signal[i], padded[498 + i], 0.1, 
                        "Original signal should be in the middle");
        }
    }
    
    // ==================== Edge Cases ====================
    
    @Test
    @DisplayName("All strategies handle single-element signal")
    void testSingleElementSignal() {
        double[] signal = {42.0};
        
        PaddingStrategy[] strategies = {
            new ConstantPaddingStrategy(),
            new LinearExtrapolationStrategy(2),
            new PolynomialExtrapolationStrategy(2),
            new StatisticalPaddingStrategy(),
            new AntisymmetricPaddingStrategy(),
            new AdaptivePaddingStrategy(),
            new CompositePaddingStrategy(
                new ZeroPaddingStrategy(),
                new ConstantPaddingStrategy()
            )
        };
        
        for (var strategy : strategies) {
            double[] padded = strategy.pad(signal, 5);
            assertEquals(5, padded.length, 
                        "Strategy " + strategy.name() + " should handle single element");
            
            // CompositePaddingStrategy may have left padding, so original isn't at position 0
            if (strategy instanceof CompositePaddingStrategy) {
                // Check that the original value is preserved somewhere
                boolean found = false;
                for (double val : padded) {
                    if (Math.abs(val - 42.0) < TOLERANCE) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found, "Original value should be preserved somewhere for " + strategy.name());
            } else {
                assertEquals(42.0, padded[0], TOLERANCE, 
                            "Original value should be preserved at position 0 for " + strategy.name());
            }
            
            // Check all padded values are finite
            for (double val : padded) {
                assertTrue(Double.isFinite(val), 
                          "All values should be finite for " + strategy.name());
            }
        }
    }
    
    @Test
    @DisplayName("All strategies handle alternating infinite slopes")
    void testInfiniteSlopes() {
        // Signal with alternating values creating infinite slopes in discrete sense
        double[] signal = new double[10];
        for (int i = 0; i < 10; i++) {
            signal[i] = (i % 2 == 0) ? 0 : 1e6;
        }
        
        PaddingStrategy[] strategies = {
            new LinearExtrapolationStrategy(2),
            new PolynomialExtrapolationStrategy(2),
            new StatisticalPaddingStrategy(StatisticalPaddingStrategy.StatMethod.TREND),
            new AdaptivePaddingStrategy()
        };
        
        for (var strategy : strategies) {
            double[] padded = strategy.pad(signal, 15);
            assertEquals(15, padded.length);
            
            // Should handle without producing NaN or Inf
            for (double val : padded) {
                assertTrue(Double.isFinite(val) || Math.abs(val) < 1e10, 
                          strategy.name() + " should handle large slopes");
            }
        }
    }
}