package com.morphiqlabs.wavelet.denoising;

import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidSignalException;
import com.morphiqlabs.wavelet.exception.InvalidStateException;
import com.morphiqlabs.wavelet.exception.ErrorCode;
import com.morphiqlabs.wavelet.exception.ErrorContext;
import com.morphiqlabs.wavelet.WaveletOperations;

/**
 * Wavelet-based signal denoising using various thresholding strategies.
 *
 * <p>This class provides comprehensive denoising capabilities for signals
 * corrupted by noise, particularly effective for financial time series data
 * where preserving important features while removing noise is critical.</p>
 *
 * <p>Now uses MODWT (Maximal Overlap Discrete Wavelet Transform) which provides:</p>
 * <ul>
 *   <li>Shift-invariant denoising (better for time series)</li>
 *   <li>Works with any signal length (not just power-of-2)</li>
 *   <li>Same-length coefficients preserve temporal alignment</li>
 *   <li>Multiple threshold selection methods (Universal, SURE, Minimax)</li>
 *   <li>Soft and hard thresholding</li>
 *   <li>Level-dependent thresholding</li>
 *   <li>Multi-level decomposition support</li>
 *   <li>SIMD-optimized thresholding operations</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * WaveletDenoiser denoiser = new WaveletDenoiser(Daubechies.DB4, BoundaryMode.PERIODIC);
 * double[] noisySignal = ...;
 * double[] denoised = denoiser.denoise(noisySignal, ThresholdMethod.UNIVERSAL);
 * }</pre>
 * 
 * @since 1.0.0
 */
public class WaveletDenoiser {
    
    /**
     * Maximum safe level for bit shift scaling operations.
     * 
     * <p>When calculating level-dependent scaling using bit shifts (1 << (level - 1)),
     * we need to ensure the shift amount doesn't exceed 30 to avoid integer overflow.
     * Since level - 1 must be <= 30, the maximum safe level is 31.</p>
     * 
     * <p>In practice, wavelet decomposition rarely exceeds 10-15 levels due to
     * signal length constraints and numerical stability, so this limit provides
     * a large safety margin.</p>
     */
    private static final int MAX_SAFE_LEVEL_FOR_SCALING = 31;
    
    /**
     * Small positive value added to prevent division by zero in BayesShrink calculation.
     * 
     * <p>When the signal standard deviation (sigma_x) is zero or very small,
     * this epsilon prevents numerical instability in the threshold calculation
     * T = sigma^2 / sigma_x. The value is chosen to be negligible compared to
     * typical signal magnitudes while ensuring numerical stability.</p>
     */
    private static final double BAYES_EPSILON = 1e-10;

    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final boolean useVectorOps;

    /**
     * Creates a wavelet denoiser with the specified wavelet and boundary mode.
     *
     * @param wavelet      the wavelet to use for decomposition
     * @param boundaryMode the boundary handling mode
     * @throws InvalidArgumentException if wavelet or boundaryMode is null
     */
    public WaveletDenoiser(Wavelet wavelet, BoundaryMode boundaryMode) {
        if (wavelet == null) {
            throw InvalidArgumentException.nullArgument("wavelet");
        }
        if (boundaryMode == null) {
            throw InvalidArgumentException.nullArgument("boundaryMode");
        }

        this.wavelet = wavelet;
        this.boundaryMode = boundaryMode;
        this.useVectorOps = WaveletOperations.getPerformanceInfo().vectorizationEnabled();
    }

    /**
     * Creates a denoiser optimized for financial time series.
     * Uses DB4 wavelet with periodic boundaries and SURE thresholding.
     *
     * @return a new WaveletDenoiser instance
     */
    public static WaveletDenoiser forFinancialData() {
        return new WaveletDenoiser(com.morphiqlabs.wavelet.api.Daubechies.DB4, BoundaryMode.PERIODIC);
    }

    /**
     * Denoises a signal using single-level wavelet transform with automatic threshold selection.
     *
     * @param signal the noisy signal to denoise
     * @param method the threshold selection method
     * @return the denoised signal
     * @throws InvalidSignalException if signal is invalid
     */
    public double[] denoise(double[] signal, ThresholdMethod method) {
        return denoise(signal, method, ThresholdType.SOFT);
    }

    /**
     * Denoises a signal using single-level wavelet transform.
     *
     * @param signal the noisy signal to denoise
     * @param method the threshold selection method
     * @param type   the thresholding type (soft or hard)
     * @return the denoised signal
     * @throws InvalidSignalException if signal is invalid
     */
    public double[] denoise(double[] signal, ThresholdMethod method, ThresholdType type) {
        MODWTTransform transform = new MODWTTransform(wavelet, boundaryMode);
        MODWTResult result = transform.forward(signal);

        // Estimate noise level from detail coefficients
        double sigma = estimateNoiseSigma(result.detailCoeffs());

        // Calculate threshold
        double threshold = calculateThreshold(result.detailCoeffs(), sigma, method);

        // Apply thresholding to detail coefficients
        double[] denoisedDetails = applyThreshold(result.detailCoeffs(), threshold, type);

        // Reconstruct with denoised coefficients
        MODWTResult denoisedResult = MODWTResult.create(
                result.approximationCoeffs(), denoisedDetails);

        return transform.inverse(denoisedResult);
    }

    /**
     * Denoises a signal using multi-level wavelet transform with level-dependent thresholding.
     *
     * @param signal the noisy signal to denoise
     * @param levels the number of decomposition levels
     * @param method the threshold selection method
     * @param type   the thresholding type
     * @return the denoised signal
     * @throws InvalidSignalException   if signal is invalid
     * @throws InvalidArgumentException if levels is invalid
     */
    public double[] denoiseMultiLevel(double[] signal, int levels,
                                      ThresholdMethod method, ThresholdType type) {
        // Use proper multi-level MODWT decomposition
        MultiLevelMODWTTransform multiTransform = new MultiLevelMODWTTransform(wavelet, boundaryMode);
        MultiLevelMODWTResult multiResult = multiTransform.decompose(signal, levels);
        
        // Estimate noise from the finest scale (level 1) detail coefficients
        double sigma = estimateNoiseSigma(multiResult.getDetailCoeffsAtLevel(1));
        
        // Create a wrapper that applies denoising on-the-fly
        MultiLevelMODWTResult denoisedResult = new DenoisedMultiLevelResult(
            multiResult, sigma, method, type);
        
        // Reconstruct the denoised signal
        return multiTransform.reconstruct(denoisedResult);
    }
    
    /**
     * Wrapper class that applies denoising to multi-level MODWT coefficients on-the-fly.
     */
    private class DenoisedMultiLevelResult implements MultiLevelMODWTResult {
        private final MultiLevelMODWTResult original;
        private final double sigma;
        private final ThresholdMethod method;
        private final ThresholdType type;
        private final double[][] denoisedDetails;
        
        DenoisedMultiLevelResult(MultiLevelMODWTResult original, double sigma,
                                ThresholdMethod method, ThresholdType type) {
            this.original = original;
            this.sigma = sigma;
            this.method = method;
            this.type = type;
            this.denoisedDetails = new double[original.getLevels()][];
            
            // Validate maximum level before processing to prevent overflow
            if (original.getLevels() > MAX_SAFE_LEVEL_FOR_SCALING) {
                throw new InvalidArgumentException(
                    ErrorCode.VAL_TOO_LARGE,
                    ErrorContext.builder("Decomposition level exceeds safe limit for scale-dependent thresholds")
                        .withContext("Operation", "Multi-level denoising")
                        .withLevelInfo(original.getLevels(), MAX_SAFE_LEVEL_FOR_SCALING)
                        .withContext("Threshold method", method.name())
                        .withContext("Threshold type", type.name())
                        .withSuggestion("Reduce decomposition levels to " + MAX_SAFE_LEVEL_FOR_SCALING + " or less")
                        .withSuggestion("Use level-independent threshold methods")
                        .build()
                );
            }
            
            // Pre-compute denoised details for all levels
            for (int level = 1; level <= original.getLevels(); level++) {
                double[] levelDetails = original.getDetailCoeffsAtLevel(level);
                
                // Calculate threshold with level-dependent scaling
                // Use bit shift for efficient power of 2 calculation
                // Safety guarantee: Constructor validation ensures original.getLevels() <= MAX_SAFE_LEVEL_FOR_SCALING (31)
                // Therefore: level <= 31, so (level - 1) <= 30, making 1 << (level - 1) safe from overflow
                if (level > MAX_SAFE_LEVEL_FOR_SCALING) {
                    throw new InvalidStateException(
                        ErrorCode.STATE_INVALID,
                        ErrorContext.builder("Internal error: Level exceeds bit shift safety limit")
                            .withContext("Operation", "Scale factor calculation")
                            .withLevelInfo(level, MAX_SAFE_LEVEL_FOR_SCALING)
                            .withContext("This should have been caught earlier", "Internal consistency check")
                            .withSuggestion("This is an internal error - please report this as a bug")
                            .build()
                    );
                }
                // With textbook MODWT scaling h_j,l = h_l / 2^(j/2), noise std at level j scales by 1/sqrt(2^j)
                double levelScale = Math.sqrt(1 << level); // sqrt(2^j)
                double threshold = calculateThreshold(levelDetails, sigma / levelScale, method);
                
                // Apply thresholding and store
                denoisedDetails[level - 1] = applyThreshold(levelDetails, threshold, type);
            }
        }
        
        @Override
        public int getSignalLength() {
            return original.getSignalLength();
        }
        
        @Override
        public int getLevels() {
            return original.getLevels();
        }
        
        @Override
        public double[] getApproximationCoeffs() {
            // Return original approximation coefficients (not denoised)
            return original.getApproximationCoeffs();
        }
        
        @Override
        public double[] getDetailCoeffsAtLevel(int level) {
            if (level < 1 || level > getLevels()) {
                throw new InvalidArgumentException(
                    ErrorCode.CFG_INVALID_DECOMPOSITION_LEVEL,
                    ErrorContext.builder("Invalid level for detail coefficient access")
                        .withContext("Operation", "getDetailCoeffsAtLevel")
                        .withLevelInfo(level, getLevels())
                        .withContext("Result type", "DenoisedMultiLevelResult")
                        .withSuggestion("Level must be between 1 and " + getLevels())
                        .build()
                );
            }
            // Return denoised detail coefficients
            return denoisedDetails[level - 1].clone();
        }
        
        @Override
        public double getDetailEnergyAtLevel(int level) {
            if (level < 1 || level > getLevels()) {
                throw new InvalidArgumentException(
                    ErrorCode.CFG_INVALID_DECOMPOSITION_LEVEL,
                    ErrorContext.builder("Invalid level for detail energy calculation")
                        .withContext("Operation", "getDetailEnergyAtLevel")
                        .withLevelInfo(level, getLevels())
                        .withContext("Result type", "DenoisedMultiLevelResult")
                        .withSuggestion("Level must be between 1 and " + getLevels())
                        .build()
                );
            }
            double energy = 0.0;
            double[] details = denoisedDetails[level - 1];
            for (double val : details) {
                energy += val * val;
            }
            return energy;
        }
        
        @Override
        public double getApproximationEnergy() {
            double energy = 0.0;
            double[] approx = getApproximationCoeffs();
            for (double val : approx) {
                energy += val * val;
            }
            return energy;
        }
        
        @Override
        public double getTotalEnergy() {
            // Recalculate based on denoised coefficients
            double energy = getApproximationEnergy();
            for (int level = 1; level <= getLevels(); level++) {
                energy += getDetailEnergyAtLevel(level);
            }
            return energy;
        }
        
        @Override
        public double[] getRelativeEnergyDistribution() {
            // Recalculate based on denoised coefficients
            double totalEnergy = getTotalEnergy();
            double[] distribution = new double[getLevels() + 1];
            
            // Approximation energy
            double[] approx = getApproximationCoeffs();
            double approxEnergy = 0.0;
            for (double val : approx) {
                approxEnergy += val * val;
            }
            distribution[0] = approxEnergy / totalEnergy;
            
            // Detail energies
            for (int level = 1; level <= getLevels(); level++) {
                double[] details = denoisedDetails[level - 1];
                double levelEnergy = 0.0;
                for (double val : details) {
                    levelEnergy += val * val;
                }
                distribution[level] = levelEnergy / totalEnergy;
            }
            
            return distribution;
        }
        
        @Override
        public boolean isValid() {
            return original.isValid();
        }
        
        @Override
        public MultiLevelMODWTResult copy() {
            // Return a new wrapper with the same parameters
            return new DenoisedMultiLevelResult(original.copy(), sigma, method, type);
        }
    }

    /**
     * Denoises a signal using a fixed threshold value.
     *
     * @param signal    the noisy signal
     * @param threshold the threshold value
     * @param type      the thresholding type
     * @return the denoised signal
     */
    public double[] denoiseFixed(double[] signal, double threshold, ThresholdType type) {
        MODWTTransform transform = new MODWTTransform(wavelet, boundaryMode);
        MODWTResult result = transform.forward(signal);

        double[] denoisedDetails = applyThreshold(result.detailCoeffs(), threshold, type);

        MODWTResult denoisedResult = MODWTResult.create(
                result.approximationCoeffs(), denoisedDetails);

        return transform.inverse(denoisedResult);
    }

    /**
     * Estimates the noise standard deviation using the Median Absolute Deviation (MAD)
     * of the detail coefficients at the finest scale.
     *
     * @param detailCoeffs the detail coefficients
     * @return estimated noise standard deviation
     */
    protected double estimateNoiseSigma(double[] detailCoeffs) {
        // Calculate median absolute deviation
        double[] absCoeffs = new double[detailCoeffs.length];
        for (int i = 0; i < detailCoeffs.length; i++) {
            absCoeffs[i] = Math.abs(detailCoeffs[i]);
        }

        double median = calculateMedian(absCoeffs);

        // Scale factor for Gaussian noise
        return median / 0.6745;
    }

    /**
     * Calculates the threshold value based on the selected method.
     *
     * @param coeffs the wavelet coefficients
     * @param sigma the noise standard deviation
     * @param method the threshold selection method
     * @return the calculated threshold value
     */
    protected double calculateThreshold(double[] coeffs, double sigma, ThresholdMethod method) {
        int n = coeffs.length;

        switch (method) {
            case UNIVERSAL:
                // Universal threshold (VisuShrink)
                return sigma * Math.sqrt(2.0 * Math.log(n));

            case SURE:
                // SURE threshold
                return calculateSUREThreshold(coeffs, sigma);

            case MINIMAX:
                // Minimax threshold
                return calculateMinimaxThreshold(n, sigma);

            case BAYES:
                // BayesShrink threshold
                return calculateBayesThreshold(coeffs, sigma);

            case FIXED:
                // Should not reach here for automatic threshold selection
                throw new InvalidArgumentException(
                    ErrorCode.CFG_UNSUPPORTED_OPERATION,
                    ErrorContext.builder("Fixed threshold method requires explicit threshold value")
                        .withContext("Operation", "denoise")
                        .withContext("Threshold method", "FIXED")
                        .withSuggestion("Use denoiseFixed() method with explicit threshold value")
                        .withSuggestion("Or select an automatic threshold method: UNIVERSAL, SURE, MINIMAX, or BAYES")
                        .build()
                );

            default:
                throw new InvalidArgumentException(
                    ErrorCode.CFG_UNSUPPORTED_OPERATION,
                    ErrorContext.builder("Unknown threshold selection method")
                        .withContext("Operation", "selectThreshold")
                        .withContext("Unknown method", method.toString())
                        .withSuggestion("Supported methods: UNIVERSAL, SURE, MINIMAX, BAYES, FIXED")
                        .build()
                );
        }
    }

    /**
     * Calculates SURE (Stein's Unbiased Risk Estimate) threshold.
     */
    private double calculateSUREThreshold(double[] coeffs, double sigma) {
        int n = coeffs.length;

        // Sort coefficients by absolute value
        double[] sortedAbs = new double[n];
        for (int i = 0; i < n; i++) {
            sortedAbs[i] = Math.abs(coeffs[i]);
        }
        java.util.Arrays.sort(sortedAbs);

        // Calculate SURE for each possible threshold
        double minRisk = Double.POSITIVE_INFINITY;
        double bestThreshold = 0;

        for (int k = 0; k < n; k++) {
            double t = sortedAbs[k];
            double risk = calculateSURERisk(coeffs, t, sigma);

            if (risk < minRisk) {
                minRisk = risk;
                bestThreshold = t;
            }
        }

        // Compare with universal threshold
        double universalThreshold = sigma * Math.sqrt(2.0 * Math.log(n));
        if (bestThreshold > universalThreshold) {
            bestThreshold = universalThreshold;
        }

        return bestThreshold;
    }

    /**
     * Calculates SURE risk for a given threshold.
     */
    private double calculateSURERisk(double[] coeffs, double threshold, double sigma) {
        int n = coeffs.length;
        double sigma2 = sigma * sigma;
        double risk = -n * sigma2;

        for (double c : coeffs) {
            double absC = Math.abs(c);
            if (absC <= threshold) {
                risk += c * c;
            } else {
                risk += sigma2 + (absC - threshold) * (absC - threshold);
            }
        }

        return risk / n;
    }

    /**
     * Calculates Minimax threshold.
     */
    private double calculateMinimaxThreshold(int n, double sigma) {
        // Minimax threshold approximation
        double logN = Math.log(n);

        if (n <= 32) {
            return 0;
        } else if (n <= 64) {
            return sigma * 0.3936 + 0.1829 * sigma * logN;
        } else {
            return sigma * (0.4745 + 0.1148 * logN);
        }
    }

    /**
     * Calculates the BayesShrink threshold.
     * 
     * <p>BayesShrink threshold is computed as: T = sigma^2 / sigma_x
     * where sigma_x = sqrt(max(0, sigma_y^2 - sigma^2))
     * and sigma_y^2 is the variance of the wavelet coefficients.</p>
     * 
     * @param coeffs the wavelet coefficients
     * @param sigma the noise standard deviation
     * @return the BayesShrink threshold
     */
    private double calculateBayesThreshold(double[] coeffs, double sigma) {
        int n = coeffs.length;
        double sigma2 = sigma * sigma;
        
        // Calculate variance of coefficients (sigma_y^2)
        double mean = 0.0;
        for (double c : coeffs) {
            mean += c;
        }
        mean /= n;
        
        double variance = 0.0;
        for (double c : coeffs) {
            double diff = c - mean;
            variance += diff * diff;
        }
        variance /= n;
        
        // Calculate signal standard deviation (sigma_x)
        // sigma_x^2 = max(0, sigma_y^2 - sigma^2)
        double sigmaX2 = Math.max(0.0, variance - sigma2);
        
        // Avoid division by zero using epsilon constant
        double sigmaX = Math.sqrt(sigmaX2 + BAYES_EPSILON);
        
        // BayesShrink threshold: T = sigma^2 / sigma_x
        return sigma2 / sigmaX;
    }

    /**
     * Applies thresholding to coefficients.
     *
     * @param coeffs the wavelet coefficients
     * @param threshold the threshold value
     * @param type the thresholding type
     * @return the thresholded coefficients
     */
    protected double[] applyThreshold(double[] coeffs, double threshold, ThresholdType type) {
        if (useVectorOps) {
            // Use SIMD-optimized thresholding
            return type == ThresholdType.SOFT
                    ? WaveletOperations.softThreshold(coeffs, threshold)
                    : WaveletOperations.hardThreshold(coeffs, threshold);
        } else {
            // Scalar implementation
            double[] result = new double[coeffs.length];

            if (type == ThresholdType.SOFT) {
                for (int i = 0; i < coeffs.length; i++) {
                    double absCoeff = Math.abs(coeffs[i]);
                    result[i] = absCoeff <= threshold ? 0.0
                            : Math.signum(coeffs[i]) * (absCoeff - threshold);
                }
            } else { // HARD
                for (int i = 0; i < coeffs.length; i++) {
                    result[i] = Math.abs(coeffs[i]) <= threshold ? 0.0 : coeffs[i];
                }
            }

            return result;
        }
    }

    /**
     * Calculates the median of an array.
     */
    private double calculateMedian(double[] values) {
        double[] sorted = values.clone();
        java.util.Arrays.sort(sorted);

        int n = sorted.length;
        if (n % 2 == 0) {
            return (sorted[n / 2 - 1] + sorted[n / 2]) / 2.0;
        } else {
            return sorted[n / 2];
        }
    }

    /**
     * Threshold selection methods.
     */
    public enum ThresholdMethod {
        /**
         * Universal threshold (VisuShrink): sqrt(2 * log(N)) * sigma
         * Conservative, tends to oversmooth but ensures noise removal.
         */
        UNIVERSAL,

        /**
         * SURE (Stein's Unbiased Risk Estimate) threshold.
         * Adapts to signal characteristics, good for smooth signals.
         */
        SURE,

        /**
         * Minimax threshold: optimal for worst-case MSE.
         * Good compromise between smoothing and feature preservation.
         */
        MINIMAX,

        /**
         * BayesShrink threshold: sigma^2 / sqrt(max(var(X) - sigma^2, 0))
         * Adaptive method that minimizes Bayesian risk, good for signals with varying SNR.
         */
        BAYES,

        /**
         * Fixed threshold: user-specified value.
         */
        FIXED
    }

    /**
     * Thresholding function types.
     */
    public enum ThresholdType {
        /**
         * Soft thresholding: shrinks coefficients towards zero.
         * Produces smoother results with less artifacts.
         */
        SOFT,

        /**
         * Hard thresholding: keeps or kills coefficients.
         * Better preserves signal features but may introduce artifacts.
         */
        HARD
    }
}
