package com.morphiqlabs.financial;

import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.BoundaryMode;

/**
 * Financial analyzer that uses wavelet transforms to analyze financial time series.
 * 
 * <p>This class provides configurable financial analysis capabilities including
 * market crash detection, volatility analysis, and regime change detection using
 * wavelet-based methods.</p>
 * 
 * <p>All analysis methods use configurable thresholds provided through the
 * {@link FinancialAnalysisConfig} rather than hardcoded values, allowing
 * customization for different markets and instruments.</p>
 */
public final class FinancialAnalyzer {
    
    private static final double EPSILON = 1e-10; // Tolerance for zero comparison
    
    private final FinancialAnalysisConfig config;
    private final MODWTTransform transform;
    
    /**
     * Creates a new FinancialAnalyzer with the specified configuration.
     * 
     * @param config the analysis configuration (not null)
     * @throws IllegalArgumentException if config is null
     */
    public FinancialAnalyzer(FinancialAnalysisConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        this.config = config;
        this.transform = new MODWTTransform(Daubechies.DB4, BoundaryMode.PERIODIC);
    }
    
    
    /**
     * Analyzes crash asymmetry in the financial time series.
     * 
     * <p>Uses wavelet analysis to detect asymmetric behavior that may indicate
     * market crashes. The analysis compares positive and negative movements
     * in the detail coefficients.</p>
     * 
     * @param prices the price time series (must not be null)
     * @return the asymmetry score; values above the configured threshold indicate potential crashes
     * @throws IllegalArgumentException if prices is null or invalid length
     */
    public double analyzeCrashAsymmetry(double[] prices) {
        validatePrices(prices);
        
        // Convert prices to returns
        double[] returns = calculateReturns(prices);
        
        // Perform wavelet transform
        MODWTResult result = transform.forward(returns);
        double[] details = result.detailCoeffs();
        
        // Calculate asymmetry between positive and negative movements
        double positiveSum = 0.0;
        double negativeSum = 0.0;
        int positiveCount = 0;
        int negativeCount = 0;
        
        for (double detail : details) {
            if (detail > 0) {
                positiveSum += detail;
                positiveCount++;
            } else if (detail < 0) {
                negativeSum += Math.abs(detail);
                negativeCount++;
            }
        }
        
        if (positiveCount == 0 || negativeCount == 0) {
            return 0.0; // No asymmetry if all movements are in one direction
        }
        
        double positiveAvg = positiveSum / positiveCount;
        double negativeAvg = negativeSum / negativeCount;
        
        // Calculate asymmetry ratio
        double maxAvg = Math.max(positiveAvg, negativeAvg);
        if (maxAvg < EPSILON) {
            return 0.0; // No asymmetry if both averages are effectively zero
        }
        return Math.abs(negativeAvg - positiveAvg) / maxAvg;
    }
    
    /**
     * Analyzes volatility in the financial time series.
     * 
     * <p>Uses wavelet analysis to measure volatility by analyzing the energy
     * in the detail coefficients, which capture high-frequency fluctuations.</p>
     * 
     * @param prices the price time series (must not be null)
     * @return the volatility measure; compare against configured thresholds for classification
     * @throws IllegalArgumentException if prices is null or invalid length
     */
    public double analyzeVolatility(double[] prices) {
        validatePrices(prices);
        
        // Convert prices to returns
        double[] returns = calculateReturns(prices);
        
        // Perform wavelet transform
        MODWTResult result = transform.forward(returns);
        double[] details = result.detailCoeffs();
        
        // Calculate energy in detail coefficients (volatility measure)
        double energy = 0.0;
        for (double detail : details) {
            energy += detail * detail;
        }
        
        return Math.sqrt(energy / details.length);
    }
    
    /**
     * Analyzes regime trends in the financial time series.
     * 
     * <p>Uses wavelet analysis to detect potential regime changes by analyzing
     * the trend in approximation coefficients over time.</p>
     * 
     * @param prices the price time series (must not be null)
     * @return the trend change measure; values above the configured threshold indicate regime shifts
     * @throws IllegalArgumentException if prices is null or invalid length
     */
    public double analyzeRegimeTrend(double[] prices) {
        validatePrices(prices);
        
        // Convert prices to returns
        double[] returns = calculateReturns(prices);
        
        // Perform wavelet transform
        MODWTResult result = transform.forward(returns);
        double[] approx = result.approximationCoeffs();
        
        if (approx.length < 2) {
            return 0.0;
        }
        
        // Calculate the maximum change in approximation coefficients
        double maxChange = 0.0;
        for (int i = 1; i < approx.length; i++) {
            double change = Math.abs(approx[i] - approx[i-1]);
            maxChange = Math.max(maxChange, change);
        }
        
        return maxChange;
    }
    
    /**
     * Detects anomalies in the financial time series.
     * 
     * <p>Uses statistical analysis of wavelet coefficients to identify
     * outliers that deviate significantly from normal behavior.</p>
     * 
     * @param prices the price time series (must not be null)
     * @return true if anomalies are detected above the configured threshold
     * @throws IllegalArgumentException if prices is null or invalid length
     */
    public boolean detectAnomalies(double[] prices) {
        validatePrices(prices);
        
        // Convert prices to returns
        double[] returns = calculateReturns(prices);
        
        // Perform wavelet transform
        MODWTResult result = transform.forward(returns);
        double[] details = result.detailCoeffs();
        
        // Calculate mean and standard deviation of detail coefficients
        double mean = 0.0;
        for (double detail : details) {
            mean += detail;
        }
        mean /= details.length;
        
        double variance = 0.0;
        for (double detail : details) {
            double diff = detail - mean;
            variance += diff * diff;
        }
        double stdDev = Math.sqrt(variance / details.length);
        
        // Check for outliers beyond configured threshold
        for (double detail : details) {
            if (Math.abs(detail - mean) > config.getAnomalyDetectionThreshold() * stdDev) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Classifies volatility as low, normal, or high based on configured thresholds.
     * 
     * @param volatility the volatility measure from {@link #analyzeVolatility(double[])}
     * @return the volatility classification
     */
    public VolatilityClassification classifyVolatility(double volatility) {
        if (volatility < config.getVolatilityLowThreshold()) {
            return VolatilityClassification.LOW;
        } else if (volatility > config.getVolatilityHighThreshold()) {
            return VolatilityClassification.HIGH;
        } else {
            return VolatilityClassification.NORMAL;
        }
    }
    
    /**
     * Checks if crash asymmetry exceeds the configured threshold.
     * 
     * @param asymmetry the asymmetry measure from {@link #analyzeCrashAsymmetry(double[])}
     * @return true if asymmetry indicates potential crash conditions
     */
    public boolean isCrashRisk(double asymmetry) {
        return asymmetry > config.getCrashAsymmetryThreshold();
    }
    
    /**
     * Checks if regime trend change exceeds the configured threshold.
     * 
     * @param trendChange the trend change measure from {@link #analyzeRegimeTrend(double[])}
     * @return true if trend change indicates potential regime shift
     */
    public boolean isRegimeShift(double trendChange) {
        return trendChange > config.getRegimeTrendThreshold();
    }
    
    /**
     * Gets the configuration used by this analyzer.
     * 
     * @return the analysis configuration
     */
    public FinancialAnalysisConfig getConfig() {
        return config;
    }
    
    /**
     * Converts price series to return series.
     */
    private double[] calculateReturns(double[] prices) {
        if (prices.length < 2) {
            return new double[0];
        }
        
        double[] returns = new double[prices.length - 1];
        for (int i = 1; i < prices.length; i++) {
            if (Math.abs(prices[i-1]) > EPSILON) {
                returns[i-1] = (prices[i] - prices[i-1]) / prices[i-1];
            } else {
                // Price is effectively zero, cannot calculate percentage return
                returns[i-1] = 0.0;
            }
        }
        
        // MODWT works with any signal length - no padding needed
        return returns;
    }
    
    /**
     * Validates the input price array.
     */
    private void validatePrices(double[] prices) {
        if (prices == null) {
            throw new IllegalArgumentException("Prices cannot be null");
        }
        if (prices.length < 2) {
            throw new IllegalArgumentException("Prices must contain at least 2 elements");
        }
        
        // Check for NaN or infinite values
        for (int i = 0; i < prices.length; i++) {
            if (!Double.isFinite(prices[i])) {
                throw new IllegalArgumentException("Prices must contain only finite values");
            }
        }
    }
    
    // nextPowerOfTwo method removed - MODWT doesn't need power-of-2 lengths
    
    /**
     * Enumeration for volatility classification.
     */
    public enum VolatilityClassification {
        LOW, NORMAL, HIGH
    }
}