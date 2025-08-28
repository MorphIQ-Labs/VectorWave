package com.morphiqlabs.financial;

import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.api.Haar;
import com.morphiqlabs.wavelet.api.BoundaryMode;

/**
 * Financial analysis using wavelet transforms.
 * Provides methods for calculating risk-adjusted returns and other financial metrics
 * using wavelet-based signal processing techniques.
 * 
 * <p><strong>⚠️ WARNING - Aggressive Denoising:</strong></p>
 * <p>The wavelet-based Sharpe ratio methods use a simple but aggressive denoising
 * approach that completely removes all high-frequency components (detail coefficients).
 * This can remove important market signals along with noise. Consider using
 * {@link com.morphiqlabs.wavelet.denoising.WaveletDenoiser} for more sophisticated
 * denoising with configurable thresholds.</p>
 * 
 * <p><strong>Note:</strong></p>
 * <ul>
 *   <li>This class now uses MODWT (Maximal Overlap Discrete Wavelet Transform) which works with any signal length</li>
 *   <li>MODWT is shift-invariant and provides better time-frequency localization for financial analysis</li>
 *   <li>The transform produces same-length coefficients, preserving temporal alignment</li>
 * </ul>
 * 
 * <p><strong>Usage Example:</strong></p>
 * <pre>{@code
 * // MODWT works with any signal length - no padding needed
 * double[] returns = calculateReturns(prices);
 * 
 * FinancialWaveletAnalyzer analyzer = new FinancialWaveletAnalyzer();
 * double waveletSharpe = analyzer.calculateWaveletSharpeRatio(returns);
 * }</pre>
 */
public class FinancialWaveletAnalyzer {
    
    private final FinancialConfig config;
    private final MODWTTransform transform;
    
    
    /**
     * Creates a financial analyzer with specified configuration and default Haar wavelet.
     * 
     * @param config the financial configuration containing risk-free rate and other parameters
     * @throws IllegalArgumentException if config is null
     */
    public FinancialWaveletAnalyzer(FinancialConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Financial configuration cannot be null");
        }
        this.config = config;
        this.transform = new MODWTTransform(new Haar(), BoundaryMode.PERIODIC);
    }
    
    /**
     * Creates a financial analyzer with specified configuration and wavelet transform.
     * 
     * @param config the financial configuration
     * @param transform the wavelet transform to use for signal processing
     * @throws IllegalArgumentException if config or transform is null
     */
    public FinancialWaveletAnalyzer(FinancialConfig config, MODWTTransform transform) {
        if (config == null) {
            throw new IllegalArgumentException("Financial configuration cannot be null");
        }
        if (transform == null) {
            throw new IllegalArgumentException("Wavelet transform cannot be null");
        }
        this.config = config;
        this.transform = transform;
    }
    
    /**
     * Calculates the Sharpe ratio for the given returns using the configured risk-free rate.
     * The Sharpe ratio is calculated as (mean_return - risk_free_rate) / std_deviation.
     * 
     * @param returns the array of returns (as decimals, e.g., 0.05 for 5% return)
     * @return the Sharpe ratio
     * @throws IllegalArgumentException if returns is null, empty, or contains insufficient data
     */
    public double calculateSharpeRatio(double[] returns) {
        return calculateSharpeRatio(returns, config.getRiskFreeRate());
    }
    
    /**
     * Calculates the Sharpe ratio for the given returns using a specified risk-free rate.
     * The Sharpe ratio is calculated as (mean_return - risk_free_rate) / std_deviation.
     * 
     * @param returns the array of returns (as decimals, e.g., 0.05 for 5% return)
     * @param riskFreeRate the risk-free rate to use (as annual decimal)
     * @return the Sharpe ratio
     * @throws IllegalArgumentException if returns is null, empty, or contains insufficient data
     */
    public double calculateSharpeRatio(double[] returns, double riskFreeRate) {
        if (returns == null) {
            throw new IllegalArgumentException("Returns array cannot be null");
        }
        if (returns.length == 0) {
            throw new IllegalArgumentException("Returns array cannot be empty");
        }
        if (returns.length == 1) {
            throw new IllegalArgumentException(
                "Cannot calculate Sharpe ratio with a single return value. " +
                "At least 2 returns are required to calculate standard deviation (risk).");
        }
        
        // Calculate mean return
        double mean = calculateMean(returns);
        
        // Calculate standard deviation
        double stdDev = calculateStandardDeviation(returns, mean);
        
        // Handle zero standard deviation case
        if (stdDev == 0.0) {
            if (mean == riskFreeRate) {
                return 0.0; // No excess return, no risk
            }
            return mean > riskFreeRate ? Double.POSITIVE_INFINITY : Double.NEGATIVE_INFINITY;
        }
        
        // Calculate Sharpe ratio
        return (mean - riskFreeRate) / stdDev;
    }
    
    /**
     * Calculates wavelet-denoised Sharpe ratio by first applying wavelet transform
     * to filter out noise from the returns series.
     * 
     * <p><strong>WARNING - Aggressive Denoising:</strong> This method uses a simple but
     * aggressive denoising approach that zeros ALL detail coefficients, keeping only the
     * approximation (low-frequency) components. While this effectively removes noise, it
     * may also remove important market signals including volatility spikes, rapid price
     * movements, and short-term trading patterns.</p>
     * 
     * <p><strong>Suitable for:</strong> Long-term investment analysis where smoothing is desired.
     * <br><strong>Not suitable for:</strong> Risk management (may hide volatility), high-frequency
     * trading, or any application requiring preservation of short-term market dynamics.</p>
     * 
     * <p><strong>Signal Length:</strong> MODWT works with signals of any length,
     * not just powers of two. This makes it more flexible than traditional DWT
     * for financial time series analysis.</p>
     * 
     * <p>For more sophisticated denoising with configurable thresholds, consider using
     * the {@link com.morphiqlabs.wavelet.denoising.WaveletDenoiser} class instead.</p>
     * 
     * @param returns the array of returns (any length)
     * @return the Sharpe ratio calculated from denoised returns
     * @throws IllegalArgumentException if returns is null or empty
     */
    public double calculateWaveletSharpeRatio(double[] returns) {
        return calculateWaveletSharpeRatio(returns, config.getRiskFreeRate());
    }
    
    /**
     * Calculates wavelet-denoised Sharpe ratio using specified risk-free rate.
     * 
     * <p><strong>WARNING:</strong> Uses aggressive denoising that zeros all detail coefficients.
     * See {@link #calculateWaveletSharpeRatio(double[])} for important limitations and warnings.</p>
     * 
     * @param returns the array of returns (any length)
     * @param riskFreeRate the risk-free rate to use
     * @return the Sharpe ratio calculated from denoised returns
     * @throws IllegalArgumentException if returns is null or empty
     */
    public double calculateWaveletSharpeRatio(double[] returns, double riskFreeRate) {
        if (returns == null) {
            throw new IllegalArgumentException("Returns array cannot be null");
        }
        if (returns.length == 0) {
            throw new IllegalArgumentException("Returns array cannot be empty");
        }
        // MODWT works with any signal length - no power-of-two restriction
        
        // Apply wavelet transform for denoising
        MODWTResult result = transform.forward(returns);
        
        // WARNING: Aggressive denoising approach - zeros ALL detail coefficients
        // This removes all high-frequency components, which may include:
        //   - Market microstructure noise (good to remove)
        //   - Short-term volatility patterns (possibly important)
        //   - Rapid market movements and jumps (definitely important)
        //   - Intraday trading signals (critical for HFT)
        //
        // TRADE-OFFS:
        // - Pro: Produces very smooth, stable Sharpe ratios
        // - Pro: Eliminates measurement noise and microstructure effects
        // - Con: May remove legitimate market signals and volatility
        // - Con: Can underestimate true risk by smoothing out volatility spikes
        // - Con: Inappropriate for high-frequency or short-term trading strategies
        //
        // RECOMMENDATIONS:
        // - For long-term investment analysis: May be appropriate
        // - For risk management: Use with caution - may hide important risks
        // - For trading signals: Not recommended - consider soft thresholding instead
        // - Alternative: Use WaveletDenoiser class with configurable thresholds
        double[] approxCoeffs = result.approximationCoeffs();
        double[] detailCoeffs = result.detailCoeffs();
        
        // Create zero-filled detail coefficients array of the same length
        // The MODWTResultImpl constructor requires matching array lengths
        double[] zeroDetails = new double[detailCoeffs.length];
        
        // Create a new MODWTResult with original approximation and zeroed details
        // Note: approxCoeffs and detailCoeffs have the same length by design from the forward transform
        MODWTResult denoisedResult = MODWTResult.create(approxCoeffs, zeroDetails);
        
        // Perform inverse transform to get denoised signal
        double[] denoisedReturns = transform.inverse(denoisedResult);
        
        return calculateSharpeRatio(denoisedReturns, riskFreeRate);
    }
    
    /**
     * Returns the current financial configuration.
     * 
     * @return the financial configuration
     */
    public FinancialConfig getConfig() {
        return config;
    }
    
    /**
     * Returns the wavelet transform being used.
     * 
     * @return the wavelet transform
     */
    public MODWTTransform getTransform() {
        return transform;
    }
    
    // Private helper methods
    
    private double calculateMean(double[] values) {
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }
    
    private double calculateStandardDeviation(double[] values, double mean) {
        // Requires at least 2 values for sample standard deviation
        if (values.length < 2) {
            throw new IllegalArgumentException(
                "Standard deviation calculation requires at least 2 values, but got " + values.length);
        }
        
        double sumSquaredDiffs = 0.0;
        for (double value : values) {
            double diff = value - mean;
            sumSquaredDiffs += diff * diff;
        }
        return Math.sqrt(sumSquaredDiffs / (values.length - 1)); // Sample standard deviation
    }
}