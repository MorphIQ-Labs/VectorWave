package com.morphiqlabs.wavelet.cwt.util;

import com.morphiqlabs.wavelet.api.ContinuousWavelet;
import com.morphiqlabs.wavelet.api.ComplexContinuousWavelet;
import com.morphiqlabs.wavelet.cwt.MorletWavelet;
import com.morphiqlabs.wavelet.cwt.finance.DOGWavelet;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class WaveletEvaluationHelperTest {
    
    @Test
    public void testComputeMagnitudesWithRealWavelet() {
        ContinuousWavelet wavelet = new DOGWavelet(2);
        double[] points = {-2.0, -1.0, 0.0, 1.0, 2.0};
        
        double[] magnitudes = WaveletEvaluationHelper.computeMagnitudes(wavelet, points);
        
        assertNotNull(magnitudes);
        assertEquals(points.length, magnitudes.length);
        
        // All magnitudes should be non-negative
        for (double mag : magnitudes) {
            assertTrue(mag >= 0);
        }
        
        // DOG wavelet has maximum at 0
        assertTrue(magnitudes[2] > magnitudes[0]);
        assertTrue(magnitudes[2] > magnitudes[4]);
    }
    
    @Test
    public void testComputeMagnitudesWithComplexWavelet() {
        ComplexContinuousWavelet wavelet = new MorletWavelet();
        double[] points = {-2.0, -1.0, 0.0, 1.0, 2.0};
        
        double[] magnitudes = WaveletEvaluationHelper.computeMagnitudes(wavelet, points);
        
        assertNotNull(magnitudes);
        assertEquals(points.length, magnitudes.length);
        
        // All magnitudes should be non-negative
        for (double mag : magnitudes) {
            assertTrue(mag >= 0);
        }
        
        // Morlet has maximum near 0
        assertTrue(magnitudes[2] > magnitudes[0]);
        assertTrue(magnitudes[2] > magnitudes[4]);
    }
    
    @Test
    public void testComputePhasesWithRealWavelet() {
        ContinuousWavelet wavelet = new DOGWavelet(2);
        double[] points = {-2.0, -1.0, 0.0, 1.0, 2.0};
        
        double[] phases = WaveletEvaluationHelper.computePhases(wavelet, points);
        
        assertNotNull(phases);
        assertEquals(points.length, phases.length);
        
        // Real wavelets have phase 0 or π
        for (double phase : phases) {
            assertTrue(Math.abs(phase) < 1e-10 || Math.abs(phase - Math.PI) < 1e-10);
        }
    }
    
    @Test
    public void testComputePhasesWithComplexWavelet() {
        ComplexContinuousWavelet wavelet = new MorletWavelet();
        double[] points = {-2.0, -1.0, 0.0, 1.0, 2.0};
        
        double[] phases = WaveletEvaluationHelper.computePhases(wavelet, points);
        
        assertNotNull(phases);
        assertEquals(points.length, phases.length);
        
        // Complex wavelets can have any phase in [-π, π]
        for (double phase : phases) {
            assertTrue(phase >= -Math.PI && phase <= Math.PI);
        }
    }
    
    @Test
    public void testHasImaginaryComponentWithRealWavelet() {
        ContinuousWavelet wavelet = new DOGWavelet(2);
        
        // Real wavelets should always return false
        assertFalse(WaveletEvaluationHelper.hasImaginaryComponent(wavelet, 0.0));
        assertFalse(WaveletEvaluationHelper.hasImaginaryComponent(wavelet, 1.0));
        assertFalse(WaveletEvaluationHelper.hasImaginaryComponent(wavelet, -1.0));
    }
    
    @Test
    public void testHasImaginaryComponentWithComplexWavelet() {
        ComplexContinuousWavelet wavelet = new MorletWavelet();
        
        // Morlet wavelet has imaginary components at most points
        assertTrue(WaveletEvaluationHelper.hasImaginaryComponent(wavelet, 1.0));
        assertTrue(WaveletEvaluationHelper.hasImaginaryComponent(wavelet, -1.0));
    }
    
    @Test
    public void testEmptyPointsArray() {
        ContinuousWavelet wavelet = new DOGWavelet(2);
        double[] points = new double[0];
        
        double[] magnitudes = WaveletEvaluationHelper.computeMagnitudes(wavelet, points);
        double[] phases = WaveletEvaluationHelper.computePhases(wavelet, points);
        
        assertNotNull(magnitudes);
        assertNotNull(phases);
        assertEquals(0, magnitudes.length);
        assertEquals(0, phases.length);
    }
}