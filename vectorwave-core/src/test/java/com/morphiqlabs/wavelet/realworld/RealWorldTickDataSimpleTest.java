package com.morphiqlabs.wavelet.realworld;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.api.WaveletRegistry;
import com.morphiqlabs.wavelet.api.WaveletName;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Simple real-world tests using actual financial tick data.
 * Focuses on MODWT analysis of real market data.
 */
public class RealWorldTickDataSimpleTest {
    
    private static List<TickDataLoader.Tick> ticks;
    private static double[] midPrices;
    
    @BeforeAll
    static void loadData() throws IOException {
        System.out.println("Loading tick data...");
        ticks = TickDataLoader.loadTickData();
        System.out.println("Loaded " + ticks.size() + " ticks");
        
        // Extract price series
        double[] bidPrices = TickDataLoader.extractPrices(ticks, TickDataLoader.Tick.Side.BID);
        double[] askPrices = TickDataLoader.extractPrices(ticks, TickDataLoader.Tick.Side.ASK);
        
        // Calculate mid prices
        int minLength = Math.min(bidPrices.length, askPrices.length);
        midPrices = new double[minLength];
        for (int i = 0; i < minLength; i++) {
            midPrices[i] = (bidPrices[i] + askPrices[i]) / 2.0;
        }
        
        System.out.println("Price statistics:");
        System.out.println("  Bid prices: " + bidPrices.length);
        System.out.println("  Ask prices: " + askPrices.length);
        System.out.println("  Mid prices: " + midPrices.length);
    }
    
    @Test
    void testSingleLevelMODWT() {
        // Test single-level MODWT
        DiscreteWavelet wavelet = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.HAAR);
        MODWTTransform transform = new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        // Take a subset
        int sampleSize = 1024;
        double[] priceSubset = Arrays.copyOf(midPrices, sampleSize);
        
        // Perform single-level transform
        MODWTResult result = transform.forward(priceSubset);
        
        // Get coefficients
        double[] approx = result.approximationCoeffs();
        double[] detail = result.detailCoeffs();
        
        System.out.println("\nSingle-level MODWT:");
        System.out.printf("  Input length: %d%n", priceSubset.length);
        System.out.printf("  Approx coeffs length: %d%n", approx.length);
        System.out.printf("  Detail coeffs length: %d%n", detail.length);
        
        // MODWT should preserve length
        assertEquals(priceSubset.length, approx.length);
        assertEquals(priceSubset.length, detail.length);
        
        // Test reconstruction
        double[] reconstructed = transform.inverse(result);
        assertEquals(priceSubset.length, reconstructed.length);
        
        // Check reconstruction accuracy
        double maxError = 0;
        for (int i = 0; i < priceSubset.length; i++) {
            double error = Math.abs(priceSubset[i] - reconstructed[i]);
            maxError = Math.max(maxError, error);
        }
        
        System.out.printf("  Max reconstruction error: %.12f%n", maxError);
        assertTrue(maxError < 1e-10, "Reconstruction should be accurate");
    }
    
    @Test
    void testMultiLevelMODWT() {
        // Test multi-level MODWT
        DiscreteWavelet wavelet = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.DB4);
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        // Take a subset
        int sampleSize = 2048;
        double[] priceSubset = Arrays.copyOf(midPrices, sampleSize);
        
        // Perform 4-level decomposition
        int levels = 4;
        MultiLevelMODWTResult result = transform.decompose(priceSubset, levels);
        
        System.out.println("\nMulti-level MODWT Analysis:");
        
        // Analyze each level
        for (int level = 1; level <= levels; level++) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            double energy = 0;
            for (double d : details) {
                energy += d * d;
            }
            
            System.out.printf("  Level %d: length=%d, energy=%.6f%n", 
                level, details.length, energy);
            
            // All levels should have same length as input
            assertEquals(priceSubset.length, details.length);
        }
        
        // Get approximation at final level
        double[] finalApprox = result.getApproximationCoeffs();
        assertEquals(priceSubset.length, finalApprox.length);
        
        // Test reconstruction
        double[] reconstructed = transform.reconstruct(result);
        assertEquals(priceSubset.length, reconstructed.length);
        
        // Check reconstruction accuracy
        double maxError = 0;
        for (int i = 0; i < priceSubset.length; i++) {
            double error = Math.abs(priceSubset[i] - reconstructed[i]);
            maxError = Math.max(maxError, error);
        }
        
        System.out.printf("  Max reconstruction error: %.12f%n", maxError);
        assertTrue(maxError < 1e-6, "Multi-level reconstruction should be accurate within reasonable tolerance");
    }
    
    @Test
    void testMODWTWithRealPriceMovements() {
        // Analyze real price movements
        DiscreteWavelet wavelet = (DiscreteWavelet) WaveletRegistry.getWavelet(WaveletName.SYM4);
        MultiLevelMODWTTransform transform = new MultiLevelMODWTTransform(wavelet, BoundaryMode.PERIODIC);
        
        // Use 4096 samples for good resolution
        int sampleSize = Math.min(4096, midPrices.length);
        double[] priceSubset = Arrays.copyOf(midPrices, sampleSize);
        
        // Calculate basic statistics
        double mean = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        
        for (double price : priceSubset) {
            mean += price;
            min = Math.min(min, price);
            max = Math.max(max, price);
        }
        mean /= sampleSize;
        
        System.out.println("\nPrice Data Statistics:");
        System.out.printf("  Samples: %d%n", sampleSize);
        System.out.printf("  Mean: %.4f%n", mean);
        System.out.printf("  Min: %.4f%n", min);
        System.out.printf("  Max: %.4f%n", max);
        System.out.printf("  Range: %.4f%n", max - min);
        
        // Perform 5-level decomposition
        int levels = 5;
        MultiLevelMODWTResult result = transform.decompose(priceSubset, levels);
        
        System.out.println("\nMulti-resolution Price Analysis:");
        
        // Analyze energy distribution across scales
        double totalEnergy = 0;
        double[] levelEnergies = new double[levels + 1];
        
        for (int level = 1; level <= levels; level++) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            double energy = 0;
            for (double d : details) {
                energy += d * d;
            }
            levelEnergies[level] = energy;
            totalEnergy += energy;
            
            // Estimate frequency content
            double scale = Math.pow(2, level);
            System.out.printf("  Level %d (scale %.0f): energy=%.2f%n", 
                level, scale, energy);
        }
        
        // Add approximation energy
        double[] approx = result.getApproximationCoeffs();
        double approxEnergy = 0;
        for (double a : approx) {
            approxEnergy += a * a;
        }
        levelEnergies[0] = approxEnergy;
        totalEnergy += approxEnergy;
        
        System.out.printf("  Approximation: energy=%.2f%n", approxEnergy);
        System.out.printf("  Total energy: %.2f%n", totalEnergy);
        
        // Energy distribution percentages
        System.out.println("\nEnergy Distribution:");
        for (int level = 1; level <= levels; level++) {
            double percentage = (levelEnergies[level] / totalEnergy) * 100;
            System.out.printf("  Level %d: %.1f%%%n", level, percentage);
        }
        double approxPercentage = (approxEnergy / totalEnergy) * 100;
        System.out.printf("  Approximation: %.1f%%%n", approxPercentage);
        
        // The approximation should contain most energy (trend)
        assertTrue(approxPercentage > 50, 
            "Approximation should contain majority of energy for price data");
    }
}