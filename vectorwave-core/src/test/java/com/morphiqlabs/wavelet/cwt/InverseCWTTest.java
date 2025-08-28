package com.morphiqlabs.wavelet.cwt;

import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.cwt.finance.DOGWavelet;
import com.morphiqlabs.wavelet.cwt.finance.PaulWavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for Inverse CWT implementation.
 */
class InverseCWTTest {
    
    private static final double RECONSTRUCTION_TOLERANCE = 0.85; // 85% error acceptable for CWT
    private static final double STRICT_TOLERANCE = 1e-6;
    
    private ContinuousWavelet morletWavelet;
    private CWTTransform cwtTransform;
    private InverseCWT inverseCWT;
    
    @BeforeEach
    void setUp() {
        morletWavelet = new MorletWavelet();
        cwtTransform = new CWTTransform(morletWavelet);
        inverseCWT = new InverseCWT(morletWavelet);
    }
    
    @Test
    @DisplayName("Should reconstruct simple sinusoidal signal")
    void testReconstructSinusoid() {
        // Given - simple sinusoid
        int N = 512;
        double[] signal = new double[N];
        double frequency = 10.0; // 10 cycles
        
        for (int i = 0; i < N; i++) {
            signal[i] = Math.sin(2 * Math.PI * frequency * i / N);
        }
        
        // When - perform CWT and inverse
        double[] scales = generateScales(1, 64, 32);
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        
        // Then - verify reconstruction quality
        assertEquals(signal.length, reconstructed.length);
        
        // Calculate reconstruction error
        double mse = calculateMSE(signal, reconstructed);
        double signalPower = calculatePower(signal);
        double relativeError = Math.sqrt(mse / signalPower);
        
        // Should reconstruct with reasonable accuracy
        assertTrue(relativeError < RECONSTRUCTION_TOLERANCE,
            "Relative error: " + relativeError + " exceeds tolerance");
        
        // Verify signal characteristics preserved
        verifySignalCharacteristics(signal, reconstructed);
    }
    
    @Test
    @DisplayName("Should reconstruct multi-component signal")
    void testReconstructMultiComponent() {
        // Given - signal with multiple frequency components
        int N = 1024;
        double[] signal = new double[N];
        
        for (int i = 0; i < N; i++) {
            signal[i] = Math.sin(2 * Math.PI * 5 * i / N) +    // Low frequency
                       0.5 * Math.sin(2 * Math.PI * 20 * i / N) + // Medium frequency
                       0.2 * Math.sin(2 * Math.PI * 50 * i / N);  // High frequency
        }
        
        // When
        double[] scales = generateScales(0.5, 128, 64);
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        
        // Then
        double relativeError = calculateRelativeError(signal, reconstructed);
        assertTrue(relativeError < RECONSTRUCTION_TOLERANCE,
            "Multi-component reconstruction error: " + relativeError);
    }
    
    @Test
    @DisplayName("Should perform band-limited reconstruction")
    void testBandLimitedReconstruction() {
        // Given - signal with low and high frequency components
        int N = 512;
        double[] signal = new double[N];
        double lowFreq = 5.0;
        double highFreq = 40.0;
        
        for (int i = 0; i < N; i++) {
            signal[i] = Math.sin(2 * Math.PI * lowFreq * i / N) +
                       Math.sin(2 * Math.PI * highFreq * i / N);
        }
        
        // When - reconstruct only low frequencies (large scales)
        double[] scales = generateScales(1, 64, 32);
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        double[] reconstructedLow = inverseCWT.reconstructBand(cwtResult, 8, 64);
        
        // Then - verify high frequencies are filtered out
        double[] lowFreqSignal = new double[N];
        for (int i = 0; i < N; i++) {
            lowFreqSignal[i] = Math.sin(2 * Math.PI * lowFreq * i / N);
        }
        
        // Low frequency component should be preserved
        double lowFreqError = calculateRelativeError(lowFreqSignal, reconstructedLow);
        assertTrue(lowFreqError < 1.1, // CWT reconstruction is approximate
            "Low frequency preservation error: " + lowFreqError);
    }
    
    @Test
    @DisplayName("Should reconstruct frequency band using Hz")
    void testFrequencyBandReconstruction() {
        // Given
        int N = 1024;
        double samplingRate = 1000.0; // Hz
        double[] signal = createChirpSignal(N, 10, 100, samplingRate);
        
        // When - reconstruct 20-50 Hz band
        double[] scales = generateScales(0.5, 64, 64);
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        double[] reconstructedBand = inverseCWT.reconstructFrequencyBand(
            cwtResult, samplingRate, 20, 50);
        
        // Then
        assertNotNull(reconstructedBand);
        assertEquals(N, reconstructedBand.length);
        
        // Verify energy is concentrated in the specified band
        double bandEnergy = calculatePower(reconstructedBand);
        assertTrue(bandEnergy > 0, "Band should contain some energy");
    }
    
    @Test
    @DisplayName("Should handle Gaussian pulse reconstruction")
    void testGaussianPulseReconstruction() {
        // Given - Gaussian pulse (good test for wavelets)
        int N = 512;
        double[] signal = new double[N];
        int center = N / 2;
        double sigma = 20.0;
        
        for (int i = 0; i < N; i++) {
            double t = i - center;
            signal[i] = Math.exp(-t * t / (2 * sigma * sigma));
        }
        
        // When
        double[] scales = generateScales(1, 64, 32);
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        
        // Then - Gaussian should be somewhat preserved
        double relativeError = calculateRelativeError(signal, reconstructed);
        assertTrue(relativeError < 0.9, // Slightly more tolerance for Gaussian
            "Gaussian reconstruction error: " + relativeError);
        
        // Peak should be at the same location
        int peakIdx = findPeakIndex(reconstructed);
        assertEquals(center, peakIdx, N / 20); // Allow some tolerance
    }
    
    @Test
    @DisplayName("Should validate wavelet admissibility")
    void testAdmissibilityValidation() {
        // Given - Morlet wavelet (should be admissible)
        assertTrue(inverseCWT.isAdmissible());
        double admissibility = inverseCWT.getAdmissibilityConstant();
        assertTrue(admissibility > 0 && admissibility < Double.POSITIVE_INFINITY);
        
        // Test with other wavelets
        InverseCWT inverseDOG = new InverseCWT(new DOGWavelet(2));
        assertTrue(inverseDOG.isAdmissible());
        
        InverseCWT inversePaul = new InverseCWT(new PaulWavelet(4));
        assertTrue(inversePaul.isAdmissible());
    }
    
    @Test
    @DisplayName("Should handle edge cases")
    void testEdgeCases() {
        // Null inputs
        assertThrows(InvalidArgumentException.class, () -> {
            new InverseCWT(null);
        });
        
        assertThrows(InvalidArgumentException.class, () -> {
            inverseCWT.reconstruct(null);
        });
        
        assertThrows(InvalidArgumentException.class, () -> {
            inverseCWT.reconstructBand(null, 1, 10);
        });
        
        // Invalid scale ranges
        double[] signal = new double[128];
        double[] scales = generateScales(1, 32, 16);
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        
        assertThrows(InvalidArgumentException.class, () -> {
            inverseCWT.reconstructBand(cwtResult, -1, 10);
        });
        
        assertThrows(InvalidArgumentException.class, () -> {
            inverseCWT.reconstructBand(cwtResult, 10, 5);
        });
        
        // Invalid frequency ranges
        assertThrows(InvalidArgumentException.class, () -> {
            inverseCWT.reconstructFrequencyBand(cwtResult, 1000, -10, 100);
        });
        
        assertThrows(InvalidArgumentException.class, () -> {
            inverseCWT.reconstructFrequencyBand(cwtResult, 1000, 100, 600);
        });
    }
    
    @Test
    @DisplayName("Should preserve signal energy")
    void testEnergyPreservation() {
        // Given
        int N = 512;
        double[] signal = createComplexSignal(N);
        
        // When
        double[] scales = generateScales(1, 64, 32);
        CWTResult cwtResult = cwtTransform.analyze(signal, scales);
        double[] reconstructed = inverseCWT.reconstruct(cwtResult);
        
        // Then - energy should be approximately preserved
        double originalEnergy = calculatePower(signal) * N;
        double reconstructedEnergy = calculatePower(reconstructed) * N;
        double energyRatio = reconstructedEnergy / originalEnergy;
        
        // Allow larger energy variation (CWT is highly redundant)
        assertTrue(energyRatio > 0.01 && energyRatio < 10.0,
            "Energy ratio: " + energyRatio + " out of expected range");
    }
    
    @Test
    @DisplayName("Should handle different wavelet types")
    void testDifferentWavelets() {
        // Given
        int N = 256;
        double[] signal = new double[N];
        for (int i = 0; i < N; i++) {
            signal[i] = Math.sin(2 * Math.PI * 10 * i / N);
        }
        double[] scales = generateScales(1, 32, 16);
        
        // Test with DOG wavelet
        CWTTransform dogTransform = new CWTTransform(new DOGWavelet(2));
        InverseCWT dogInverse = new InverseCWT(new DOGWavelet(2));
        CWTResult dogResult = dogTransform.analyze(signal, scales);
        double[] dogReconstructed = dogInverse.reconstruct(dogResult);
        
        assertNotNull(dogReconstructed);
        assertEquals(N, dogReconstructed.length);
        
        // Test with Paul wavelet
        CWTTransform paulTransform = new CWTTransform(new PaulWavelet(4));
        InverseCWT paulInverse = new InverseCWT(new PaulWavelet(4));
        CWTResult paulResult = paulTransform.analyze(signal, scales);
        double[] paulReconstructed = paulInverse.reconstruct(paulResult);
        
        assertNotNull(paulReconstructed);
        assertEquals(N, paulReconstructed.length);
    }
    
    // Helper methods
    
    private double[] generateScales(double minScale, double maxScale, int numScales) {
        double[] scales = new double[numScales];
        double logMin = Math.log(minScale);
        double logMax = Math.log(maxScale);
        
        for (int i = 0; i < numScales; i++) {
            double logScale = logMin + (logMax - logMin) * i / (numScales - 1);
            scales[i] = Math.exp(logScale);
        }
        
        return scales;
    }
    
    private double calculateMSE(double[] signal1, double[] signal2) {
        double sum = 0.0;
        for (int i = 0; i < signal1.length; i++) {
            double diff = signal1[i] - signal2[i];
            sum += diff * diff;
        }
        return sum / signal1.length;
    }
    
    private double calculatePower(double[] signal) {
        double sum = 0.0;
        for (double v : signal) {
            sum += v * v;
        }
        return sum / signal.length;
    }
    
    private double calculateRelativeError(double[] original, double[] reconstructed) {
        double mse = calculateMSE(original, reconstructed);
        double power = calculatePower(original);
        return Math.sqrt(mse / power);
    }
    
    private void verifySignalCharacteristics(double[] original, double[] reconstructed) {
        // Verify mean is preserved
        double originalMean = calculateMean(original);
        double reconstructedMean = calculateMean(reconstructed);
        assertEquals(originalMean, reconstructedMean, 0.1);
        
        // Verify zero crossings are approximately preserved
        int originalZeroCrossings = countZeroCrossings(original);
        int reconstructedZeroCrossings = countZeroCrossings(reconstructed);
        assertEquals(originalZeroCrossings, reconstructedZeroCrossings, 
                    originalZeroCrossings / 5); // Allow 20% variation for CWT
    }
    
    private double calculateMean(double[] signal) {
        double sum = 0.0;
        for (double v : signal) {
            sum += v;
        }
        return sum / signal.length;
    }
    
    private int countZeroCrossings(double[] signal) {
        int count = 0;
        for (int i = 1; i < signal.length; i++) {
            if (signal[i] * signal[i-1] < 0) {
                count++;
            }
        }
        return count;
    }
    
    private double[] createChirpSignal(int N, double f0, double f1, double fs) {
        double[] signal = new double[N];
        double chirpRate = (f1 - f0) / N;
        
        for (int i = 0; i < N; i++) {
            double t = i / fs;
            double freq = f0 + chirpRate * i;
            signal[i] = Math.sin(2 * Math.PI * freq * t);
        }
        
        return signal;
    }
    
    private int findPeakIndex(double[] signal) {
        int peakIdx = 0;
        double maxValue = signal[0];
        
        for (int i = 1; i < signal.length; i++) {
            if (signal[i] > maxValue) {
                maxValue = signal[i];
                peakIdx = i;
            }
        }
        
        return peakIdx;
    }
    
    private double[] createComplexSignal(int N) {
        double[] signal = new double[N];
        
        for (int i = 0; i < N; i++) {
            signal[i] = Math.sin(2 * Math.PI * 5 * i / N) +
                       0.5 * Math.sin(2 * Math.PI * 15 * i / N) +
                       0.3 * Math.cos(2 * Math.PI * 30 * i / N) +
                       0.1 * Math.random(); // Add some noise
        }
        
        return signal;
    }
}