package com.morphiqlabs.examples.basic;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.modwt.*;

/**
 * Getting Started with VectorWave - Minimal example to get you running.
 * 
 * Run: mvn exec:java -Dexec.mainClass="com.morphiqlabs.examples.basic.GettingStarted"
 */
public class GettingStarted {
    
    public static void main(String[] args) {
        // 1. Get a wavelet
        Wavelet wavelet = WaveletRegistry.getWavelet(WaveletName.DB4);
        System.out.println("Using: " + wavelet.name());
        
        // 2. Create some data
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = Math.sin(2 * Math.PI * i / 16);
        }
        
        // 3. Transform it
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        MODWTResult result = transform.forward(signal);
        
        // 4. Get coefficients
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        System.out.println("Transformed " + signal.length + " samples");
        System.out.println("Approximation coefficients: " + approx.length);
        System.out.println("Detail coefficients: " + detail.length);
        
        // 5. Reconstruct
        double[] reconstructed = transform.inverse(result);
        System.out.println("Reconstructed signal");
        
        // That's it! You're using wavelets.
    }
}