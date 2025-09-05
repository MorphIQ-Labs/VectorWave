package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.internal.ScalarOps;
import com.morphiqlabs.wavelet.extensions.internal.VectorOps;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test to verify that the time-reversed filter implementation is correct
 * in both ScalarOps and VectorOps for MODWT.
 */
class TimeReversedFilterTest {
    
    @Test
    void testTimeReversedFilterConsistency() {
        // Create a simple signal
        double[] signal = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] filter = {0.7071067811865475, 0.7071067811865475}; // Haar low-pass
        
        // Create output arrays
        double[] outputScalar = new double[signal.length];
        double[] outputVector = new double[signal.length];
        
        // Perform MODWT convolution using ScalarOps
        ScalarOps.circularConvolveMODWT(signal, filter, outputScalar);
        
        // Perform MODWT convolution using VectorOps
        VectorOps.circularConvolveMODWTVectorized(signal, filter, outputVector);
        
        // Results should be identical
        assertArrayEquals(outputScalar, outputVector, 1e-10,
            "Scalar and vector implementations should produce identical results");
        
        // Verify the convolution manually for the first few elements
        // For MODWT with time-reversed filter: W_t = Σ h_l * X_{(t-l) mod N}
        // With Haar filter h = [0.707..., 0.707...]
        // W_0 = h_0 * X_0 + h_1 * X_7 = 0.707 * 1 + 0.707 * 8 = 6.364
        // W_1 = h_0 * X_1 + h_1 * X_0 = 0.707 * 2 + 0.707 * 1 = 2.121
        
        assertEquals(0.7071067811865475 * 1 + 0.7071067811865475 * 8, 
                    outputScalar[0], 1e-10, 
                    "First element should match manual calculation");
        
        assertEquals(0.7071067811865475 * 2 + 0.7071067811865475 * 1, 
                    outputScalar[1], 1e-10, 
                    "Second element should match manual calculation");
    }
    
    @Test
    void testMODWTWithTimeReversedFilter() {
        // Test that MODWT produces expected results with time-reversed filter
        Haar haar = new Haar();
        MODWTTransform transform = new MODWTTransform(haar, BoundaryMode.PERIODIC);
        
        // Simple test signal
        double[] signal = {1, 2, 3, 4};
        
        // Perform forward transform
        MODWTResult result = transform.forward(signal);
        
        // The approximation coefficients should be the low-pass filtered result
        // Using time-reversed convolution
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        
        // Verify reconstruction
        double[] reconstructed = transform.inverse(result);
        assertArrayEquals(signal, reconstructed, 1e-10,
            "Perfect reconstruction should be maintained");
        
        // Verify shift-invariance property of MODWT
        // Shift the signal by 1
        double[] shiftedSignal = {2, 3, 4, 1};
        MODWTResult shiftedResult = transform.forward(shiftedSignal);
        
        // The coefficients should be circularly shifted versions
        double[] shiftedApprox = shiftedResult.approximationCoeffs();
        double[] shiftedDetail = shiftedResult.detailCoeffs();
        
        // Check that shifted approximation matches circular shift of original
        assertEquals(approx[1], shiftedApprox[0], 1e-10);
        assertEquals(approx[2], shiftedApprox[1], 1e-10);
        assertEquals(approx[3], shiftedApprox[2], 1e-10);
        assertEquals(approx[0], shiftedApprox[3], 1e-10);
    }
}
