package com.morphiqlabs.wavelet.padding;

import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.fft.CoreFFT;
import com.morphiqlabs.wavelet.cwt.ComplexNumber;
import java.util.Set;
import java.util.HashSet;

/**
 * Adaptive padding strategy that automatically selects the best padding method.
 * 
 * <p>This intelligent strategy analyzes signal characteristics and automatically
 * selects the most appropriate padding method. It considers factors such as:</p>
 * <ul>
 *   <li>Signal smoothness and differentiability</li>
 *   <li>Presence of trends or periodicity</li>
 *   <li>Statistical properties (stationarity, variance)</li>
 *   <li>Discontinuities and edge behavior</li>
 *   <li>Noise characteristics</li>
 * </ul>
 * 
 * <p><strong>Thread Safety:</strong> This class is immutable and thread-safe.
 * Multiple threads can safely use the same instance concurrently. The selection
 * metadata is returned as part of the result rather than stored as mutable state.
 * See {@code docs/ADAPTIVE_PADDING_THREAD_SAFETY.md} for detailed thread safety
 * guarantees and usage patterns.</p>
 * 
 * <p>Ideal for:</p>
 * <ul>
 *   <li>Unknown or varying signal types</li>
 *   <li>Automated processing pipelines</li>
 *   <li>Situations where optimal padding is critical</li>
 *   <li>Adaptive algorithms that process diverse signals</li>
 *   <li>Concurrent processing environments</li>
 * </ul>
 */
public final class AdaptivePaddingStrategy implements PaddingStrategy {
    
    private final Set<PaddingStrategy> candidateStrategies;
    
    /**
     * Result of adaptive padding operation containing padded signal and metadata.
     * This allows the strategy to remain immutable while providing selection details.
     *
     * @param paddedSignal padded output signal
     * @param selectedStrategy strategy selected by the analyzer
     * @param selectionReason human-readable reason for selection
     * @param characteristics measured signal characteristics
     */
    public record AdaptivePaddingResult(
        double[] paddedSignal,
        PaddingStrategy selectedStrategy,
        String selectionReason,
        SignalCharacteristics characteristics
    ) {}
    
    /**
     * Signal characteristics used for strategy selection.
     */
    private record SignalCharacteristics(
        double smoothness,        // 0=rough, 1=very smooth
        double trendStrength,     // 0=no trend, 1=strong trend
        double periodicity,       // 0=aperiodic, 1=strongly periodic
        double noiseLevel,        // 0=clean, 1=very noisy
        double stationarity,      // 0=non-stationary, 1=stationary
        boolean hasDiscontinuity, // Edge discontinuities detected
        int signalLength         // Original signal length
    ) {}
    
    /**
     * Creates an adaptive padding strategy with default candidate strategies.
     */
    public AdaptivePaddingStrategy() {
        this.candidateStrategies = createDefaultCandidates();
    }
    
    /**
     * Creates an adaptive padding strategy with custom candidate strategies.
     * 
     * @param candidateStrategies set of strategies to choose from
     */
    public AdaptivePaddingStrategy(Set<PaddingStrategy> candidateStrategies) {
        if (candidateStrategies == null || candidateStrategies.isEmpty()) {
            throw new InvalidArgumentException("Candidate strategies cannot be null or empty");
        }
        this.candidateStrategies = new HashSet<>(candidateStrategies);
    }
    
    /**
     * Create default set of candidate strategies.
     */
    private Set<PaddingStrategy> createDefaultCandidates() {
        Set<PaddingStrategy> candidates = new HashSet<>();
        
        // Basic strategies
        candidates.add(new ZeroPaddingStrategy());
        candidates.add(new ConstantPaddingStrategy());
        candidates.add(new SymmetricPaddingStrategy());
        candidates.add(new PeriodicPaddingStrategy());
        
        // Advanced strategies
        candidates.add(new LinearExtrapolationStrategy(3));
        candidates.add(new PolynomialExtrapolationStrategy(3));
        candidates.add(new StatisticalPaddingStrategy(StatisticalPaddingStrategy.StatMethod.MEAN));
        candidates.add(new StatisticalPaddingStrategy(StatisticalPaddingStrategy.StatMethod.TREND));
        candidates.add(new AntisymmetricPaddingStrategy());
        
        return candidates;
    }
    
    @Override
    public double[] pad(double[] signal, int targetLength) {
        // Use the detailed method and return just the padded signal for compatibility
        AdaptivePaddingResult result = padWithDetails(signal, targetLength);
        return result.paddedSignal();
    }
    
    /**
     * Performs adaptive padding and returns detailed result with selection metadata.
     * This method provides full transparency into the adaptive selection process.
     * 
     * @param signal the input signal to pad
     * @param targetLength the desired length after padding
     * @return result containing padded signal and selection metadata
     * @throws InvalidArgumentException if parameters are invalid
     */
    public AdaptivePaddingResult padWithDetails(double[] signal, int targetLength) {
        if (signal == null) {
            throw new InvalidArgumentException("Signal cannot be null");
        }
        if (signal.length == 0) {
            throw new InvalidArgumentException("Signal cannot be empty");
        }
        if (targetLength < signal.length) {
            throw new InvalidArgumentException(
                    "Target length " + targetLength + " must be >= signal length " + signal.length);
        }
        
        if (targetLength == signal.length) {
            return new AdaptivePaddingResult(
                signal.clone(),
                null,
                "No padding needed - signal already at target length",
                null
            );
        }
        
        // Analyze signal characteristics
        SignalCharacteristics characteristics = analyzeSignal(signal);
        
        // Select optimal strategy with reason
        StrategySelection selection = selectOptimalStrategyWithReason(characteristics);
        
        // Apply selected strategy
        double[] paddedSignal = selection.strategy().pad(signal, targetLength);
        
        return new AdaptivePaddingResult(
            paddedSignal,
            selection.strategy(),
            selection.reason(),
            characteristics
        );
    }
    
    /**
     * Internal record to hold strategy selection result.
     */
    private record StrategySelection(PaddingStrategy strategy, String reason) {}
    
    /**
     * Analyze signal to determine its characteristics.
     * Optimized single-pass computation where possible.
     */
    private SignalCharacteristics analyzeSignal(double[] signal) {
        int n = signal.length;
        
        // Single pass to compute basic statistics
        double sum = 0, sumSq = 0, sumX = 0, sumXY = 0, sumX2 = 0;
        double min = signal[0], max = signal[0];
        double sumFirstDiff = 0, sumSecondDiff = 0;
        double[] firstDiffs = new double[n - 1];
        
        // First pass: basic statistics and differences
        for (int i = 0; i < n; i++) {
            double val = signal[i];
            sum += val;
            sumSq += val * val;
            min = Math.min(min, val);
            max = Math.max(max, val);
            
            // For trend analysis
            sumX += i;
            sumXY += i * val;
            sumX2 += i * i;
            
            // First differences
            if (i > 0) {
                firstDiffs[i - 1] = val - signal[i - 1];
                sumFirstDiff += Math.abs(firstDiffs[i - 1]);
                
                // Second differences for smoothness
                if (i > 1) {
                    double secondDiff = Math.abs(firstDiffs[i - 1] - firstDiffs[i - 2]);
                    sumSecondDiff += secondDiff;
                }
            }
        }
        
        double mean = sum / n;
        double variance = (sumSq / n) - (mean * mean);
        double range = max - min;
        
        // Calculate smoothness from collected differences
        double smoothness = (sumFirstDiff < 1e-10) ? 1.0 : 
                           Math.exp(-2 * sumSecondDiff / sumFirstDiff);
        
        // Calculate trend strength from collected sums
        double trendStrength = 0;
        if (variance > 1e-10) {
            double denominator = n * sumX2 - sumX * sumX;
            if (Math.abs(denominator) > 1e-10) {
                double slope = (n * sumXY - sumX * sum) / denominator;
                double intercept = (sum - slope * sumX) / n;
                
                // Calculate R²
                double ssResidual = 0;
                for (int i = 0; i < n; i++) {
                    double predicted = intercept + slope * i;
                    double residual = signal[i] - predicted;
                    ssResidual += residual * residual;
                }
                double ssTotal = variance * n;
                trendStrength = Math.max(0, Math.min(1, 1 - (ssResidual / ssTotal)));
            }
        }
        
        // Noise level from first differences MAD
        double noiseLevel = 0;
        if (range > 1e-10) {
            java.util.Arrays.sort(firstDiffs);
            double mad = firstDiffs[firstDiffs.length / 2];
            noiseLevel = Math.min(1, mad / range * 5);
        }
        
        // Check stationarity using segment analysis
        double stationarity = calculateStationarityOptimized(signal, mean, variance);
        
        // Check for discontinuities at edges
        boolean hasDiscontinuity = false;
        if (n >= 3) {
            double typicalDiff = sumFirstDiff / (n - 1);
            double firstEdgeDiff = Math.abs(signal[1] - signal[0]);
            double lastEdgeDiff = Math.abs(signal[n - 1] - signal[n - 2]);
            hasDiscontinuity = firstEdgeDiff > 3 * typicalDiff || 
                              lastEdgeDiff > 3 * typicalDiff;
        }
        
        // Periodicity still needs separate computation due to complexity
        double periodicity = calculatePeriodicity(signal);
        
        return new SignalCharacteristics(
            smoothness, trendStrength, periodicity,
            noiseLevel, stationarity, hasDiscontinuity, n
        );
    }
    
    /**
     * Optimized stationarity calculation using pre-computed mean and variance.
     */
    private double calculateStationarityOptimized(double[] signal, double globalMean, double globalVar) {
        if (signal.length < 20) {
            return 0.5; // Default for short signals
        }
        
        // Divide signal into segments
        int numSegments = 4;
        int segmentSize = signal.length / numSegments;
        
        double meanVariation = 0;
        double varVariation = 0;
        
        for (int seg = 0; seg < numSegments; seg++) {
            int start = seg * segmentSize;
            int end = (seg == numSegments - 1) ? signal.length : start + segmentSize;
            
            // Calculate segment statistics
            double segSum = 0, segSumSq = 0;
            for (int i = start; i < end; i++) {
                segSum += signal[i];
                segSumSq += signal[i] * signal[i];
            }
            
            int n = end - start;
            double segMean = segSum / n;
            double segVar = (segSumSq / n) - (segMean * segMean);
            
            meanVariation += Math.abs(segMean - globalMean);
            varVariation += Math.abs(segVar - globalVar);
        }
        
        meanVariation /= (numSegments * Math.abs(globalMean) + 1e-10);
        varVariation /= (numSegments * globalVar + 1e-10);
        
        // Stationarity inversely related to variation in statistics
        double nonStationarity = (meanVariation + varVariation) / 2;
        return Math.exp(-2 * nonStationarity);
    }
    
    
    /**
     * Calculate periodicity using FFT-based autocorrelation for O(n log n) performance.
     * 
     * <p>This method uses the Wiener-Khinchin theorem: the autocorrelation function
     * is the inverse Fourier transform of the power spectral density. This reduces
     * the complexity from O(n²) to O(n log n).</p>
     */
    private double calculatePeriodicity(double[] signal) {
        if (signal.length < 10) {
            return 0; // Too short for periodicity detection
        }
        
        // For very short signals, use the direct method as FFT overhead isn't worth it
        if (signal.length < 32) {
            return calculatePeriodicityDirect(signal);
        }
        
        // Step 1: Detrend the signal
        double mean = 0;
        for (double val : signal) {
            mean += val;
        }
        mean /= signal.length;
        
        double[] detrended = new double[signal.length];
        double variance = 0;
        for (int i = 0; i < signal.length; i++) {
            detrended[i] = signal[i] - mean;
            variance += detrended[i] * detrended[i];
        }
        variance /= signal.length;
        
        if (variance < 1e-10) {
            return 0; // Constant signal
        }
        
        // Step 2: Compute autocorrelation using FFT
        // Autocorrelation = IFFT(|FFT(signal)|²)
        double[] autocorr = computeFFTAutocorrelation(detrended);
        
        // Step 3: Find the dominant period from autocorrelation peaks
        int minPeriod = 2;
        int maxPeriod = Math.min(signal.length / 2, 50);
        double maxScore = 0;
        int bestPeriod = 0;
        
        // Normalize by the zero-lag autocorrelation (signal energy)
        double energy = autocorr[0];
        if (energy < 1e-10) {
            return 0;
        }
        
        // Find peaks in the autocorrelation function
        for (int lag = minPeriod; lag <= maxPeriod && lag < autocorr.length; lag++) {
            double normalizedCorr = autocorr[lag] / energy;
            
            // Check if this is a local maximum (peak)
            boolean isPeak = lag > 0 && lag < autocorr.length - 1 &&
                           autocorr[lag] > autocorr[lag - 1] &&
                           autocorr[lag] > autocorr[lag + 1];
            
            if (isPeak || lag == minPeriod) {
                // Weight by number of complete periods that would fit
                int numPeriods = signal.length / lag;
                double weight = Math.min(1.0, numPeriods / 3.0);
                double score = Math.abs(normalizedCorr) * weight;
                
                if (score > maxScore) {
                    maxScore = score;
                    bestPeriod = lag;
                }
            }
        }
        
        // Step 4: Validate the detected period
        if (bestPeriod > 0 && maxScore > 0.3) {
            // Additional validation using time-domain check
            double validationScore = validatePeriod(signal, bestPeriod, variance);
            return Math.max(maxScore, validationScore);
        }
        
        return maxScore; // In [0,1]
    }
    
    /**
     * Compute autocorrelation using FFT for O(n log n) performance.
     * Uses the Wiener-Khinchin theorem: R(τ) = IFFT(|FFT(x)|²)
     */
    private double[] computeFFTAutocorrelation(double[] signal) {
        int n = signal.length;
        
        // Pad to next power of 2 for optimal FFT performance
        int paddedLength = 1;
        while (paddedLength < 2 * n) {
            paddedLength <<= 1;
        }
        
        // Prepare padded signal for FFT (zero-padding)
        double[] padded = new double[paddedLength * 2]; // Complex format: [re, im, re, im, ...]
        for (int i = 0; i < n; i++) {
            padded[2 * i] = signal[i];     // Real part
            padded[2 * i + 1] = 0;         // Imaginary part
        }
        
        // Forward FFT
        performInterleavedFFT(padded, paddedLength, false);
        
        // Compute power spectral density |FFT(x)|²
        for (int i = 0; i < paddedLength; i++) {
            double re = padded[2 * i];
            double im = padded[2 * i + 1];
            double magnitude = re * re + im * im;
            padded[2 * i] = magnitude;     // Store magnitude squared
            padded[2 * i + 1] = 0;         // Zero imaginary part
        }
        
        // Inverse FFT to get autocorrelation
        performInterleavedFFT(padded, paddedLength, true);
        
        // Extract normalized autocorrelation values
        double[] autocorr = new double[n];
        double scale = 1.0 / paddedLength; // FFT normalization
        for (int i = 0; i < n; i++) {
            autocorr[i] = padded[2 * i] * scale;
        }
        
        return autocorr;
    }
    
    /**
     * Direct (non-FFT) periodicity calculation for small signals.
     * Used when signal length < 32 where FFT overhead isn't justified.
     */
    private double calculatePeriodicityDirect(double[] signal) {
        // Detrend
        double mean = 0;
        for (double val : signal) {
            mean += val;
        }
        mean /= signal.length;
        
        double variance = 0;
        for (int i = 0; i < signal.length; i++) {
            double diff = signal[i] - mean;
            variance += diff * diff;
        }
        variance /= signal.length;
        
        if (variance < 1e-10) {
            return 0;
        }
        
        // Simple autocorrelation for small signals
        int maxLag = Math.min(signal.length / 2, 10);
        double maxCorr = 0;
        
        for (int lag = 2; lag <= maxLag; lag++) {
            double corr = 0;
            int count = 0;
            for (int i = 0; i < signal.length - lag; i++) {
                corr += (signal[i] - mean) * (signal[i + lag] - mean);
                count++;
            }
            if (count > 0) {
                corr = Math.abs(corr / (count * variance));
                maxCorr = Math.max(maxCorr, corr);
            }
        }
        
        return maxCorr;
    }
    
    /**
     * Validate detected period by checking signal repetition in time domain.
     */
    private double validatePeriod(double[] signal, int period, double variance) {
        if (period <= 0 || period >= signal.length) {
            return 0;
        }
        
        double diffVariance = 0;
        int count = 0;
        
        for (int i = 0; i + period < signal.length; i++) {
            double diff = signal[i] - signal[i + period];
            diffVariance += diff * diff;
            count++;
        }
        
        if (count > 0 && variance > 1e-10) {
            diffVariance /= count;
            // If differences are small relative to signal variance, it's periodic
            return 1.0 - Math.min(1.0, diffVariance / (2 * variance));
        }
        
        return 0;
    }
    
    
    /**
     * Select optimal padding strategy based on signal characteristics.
     * Returns both the strategy and the reasoning for the selection.
     */
    private StrategySelection selectOptimalStrategyWithReason(SignalCharacteristics chars) {
        // Decision tree for strategy selection
        
        // Check for special cases first
        if (chars.signalLength < 5) {
            return new StrategySelection(
                findStrategy(ConstantPaddingStrategy.class),
                "Very short signal - using constant padding for stability"
            );
        }
        
        if (chars.periodicity > 0.7) {
            return new StrategySelection(
                findStrategy(PeriodicPaddingStrategy.class),
                String.format("Strong periodicity detected (%.2f) - using periodic padding", 
                    chars.periodicity)
            );
        }
        
        if (chars.hasDiscontinuity && chars.smoothness < 0.3) {
            return new StrategySelection(
                findStrategy(ZeroPaddingStrategy.class),
                "Edge discontinuities with rough signal - using zero padding"
            );
        }
        
        if (chars.trendStrength > 0.8) {
            if (chars.noiseLevel < 0.2) {
                return new StrategySelection(
                    findStrategy(PolynomialExtrapolationStrategy.class),
                    String.format("Strong trend (R²=%.2f) with low noise - using polynomial extrapolation",
                        chars.trendStrength)
                );
            } else {
                return new StrategySelection(
                    findStatisticalStrategy(StatisticalPaddingStrategy.StatMethod.TREND),
                    String.format("Strong trend (R²=%.2f) with noise - using statistical trend padding",
                        chars.trendStrength)
                );
            }
        }
        
        if (chars.smoothness > 0.7 && chars.noiseLevel < 0.3) {
            if (chars.trendStrength > 0.5) {
                return new StrategySelection(
                    findStrategy(LinearExtrapolationStrategy.class),
                    String.format("Smooth signal (%.2f) with moderate trend - using linear extrapolation",
                        chars.smoothness)
                );
            } else {
                return new StrategySelection(
                    findStrategy(PolynomialExtrapolationStrategy.class),
                    String.format("Very smooth signal (%.2f) - using polynomial extrapolation",
                        chars.smoothness)
                );
            }
        }
        
        if (chars.stationarity > 0.7) {
            if (chars.noiseLevel > 0.5) {
                return new StrategySelection(
                    findStatisticalStrategy(StatisticalPaddingStrategy.StatMethod.MEAN),
                    String.format("Stationary signal (%.2f) with noise - using statistical mean padding",
                        chars.stationarity)
                );
            } else {
                return new StrategySelection(
                    findStrategy(ConstantPaddingStrategy.class),
                    String.format("Stationary signal (%.2f) - using constant padding",
                        chars.stationarity)
                );
            }
        }
        
        if (chars.noiseLevel > 0.6) {
            return new StrategySelection(
                findStrategy(SymmetricPaddingStrategy.class),
                String.format("High noise level (%.2f) - using symmetric padding for robustness",
                    chars.noiseLevel)
            );
        }
        
        // Default fallback
        return new StrategySelection(
            findStrategy(SymmetricPaddingStrategy.class),
            "Mixed characteristics - using symmetric padding as safe default"
        );
    }
    
    /**
     * Find a strategy of the given type in candidates.
     */
    private PaddingStrategy findStrategy(Class<? extends PaddingStrategy> strategyClass) {
        for (PaddingStrategy strategy : candidateStrategies) {
            if (strategyClass.isInstance(strategy)) {
                return strategy;
            }
        }
        // Fallback to first available strategy
        return candidateStrategies.iterator().next();
    }
    
    /**
     * Find a statistical strategy with specific method.
     */
    private PaddingStrategy findStatisticalStrategy(StatisticalPaddingStrategy.StatMethod method) {
        for (PaddingStrategy strategy : candidateStrategies) {
            if (strategy instanceof StatisticalPaddingStrategy stat) {
                if (stat.method() == method) {
                    return strategy;
                }
            }
        }
        // Fallback to any statistical strategy
        return findStrategy(StatisticalPaddingStrategy.class);
    }
    
    /**
     * Calculate priority score for a given strategy type based on signal characteristics.
     * Demonstrates modern Java 21 switch expressions for clean, functional code.
     * 
     * @param strategyType the type of padding strategy to score
     * @param characteristics the analyzed signal characteristics
     * @return priority score (0.0 = not suitable, 1.0 = perfect match)
     */
    private double calculateStrategyPriority(Class<? extends PaddingStrategy> strategyType, 
                                           SignalCharacteristics characteristics) {
        // Use switch expressions for elegant strategy scoring
        return switch (strategyType.getSimpleName()) {
            case "PeriodicPaddingStrategy" -> {
                double p = characteristics.periodicity();
                yield p > 0.8 ? 1.0 : p > 0.5 ? 0.7 : p > 0.3 ? 0.3 : 0.1;
            }
            
            case "PolynomialExtrapolationStrategy" -> 
                characteristics.trendStrength() > 0.8 && characteristics.noiseLevel() < 0.2 ? 1.0 :
                characteristics.smoothness() > 0.8 && characteristics.trendStrength() > 0.5 ? 0.8 :
                characteristics.smoothness() > 0.6 ? 0.5 : 0.2;
            
            case "LinearExtrapolationStrategy" -> 
                characteristics.trendStrength() > 0.6 && characteristics.smoothness() > 0.5 ? 0.9 :
                characteristics.trendStrength() > 0.4 ? 0.6 : 0.3;
            
            case "StatisticalPaddingStrategy" -> 
                characteristics.stationarity() > 0.8 && characteristics.noiseLevel() > 0.3 ? 0.9 :
                characteristics.stationarity() > 0.6 ? 0.6 : 0.4;
            
            case "SymmetricPaddingStrategy" -> 
                characteristics.smoothness() > 0.7 && !characteristics.hasDiscontinuity() ? 0.8 :
                characteristics.noiseLevel() < 0.3 ? 0.6 : 0.5; // Good default choice
            
            case "ZeroPaddingStrategy" -> 
                characteristics.hasDiscontinuity() && characteristics.smoothness() < 0.3 ? 0.9 :
                characteristics.noiseLevel() > 0.8 ? 0.7 : 0.2;
            
            case "ConstantPaddingStrategy" -> {
                int len = characteristics.signalLength();
                yield len < 5 ? 1.0 : // Best for very short signals
                      len < 10 ? 0.7 :
                      characteristics.stationarity() > 0.8 ? 0.6 : 0.3;
            }
            
            default -> 0.1; // Unknown strategy type gets low priority
        };
    }
    
    /**
     * Helper to perform FFT on interleaved complex data.
     * Converts [re, im, re, im, ...] to separate arrays for CoreFFT, then packs back.
     *
     * @param interleavedData array containing interleaved real/imag parts [re, im, re, im, ...]
     * @param n number of complex points
     * @param inverse true for inverse FFT, false for forward FFT
     */
    private static void performInterleavedFFT(double[] interleavedData, int n, boolean inverse) {
        // Extract real and imaginary parts
        double[] real = new double[n];
        double[] imag = new double[n];

        for (int i = 0; i < n; i++) {
            real[i] = interleavedData[2 * i];
            imag[i] = interleavedData[2 * i + 1];
        }

        // Perform FFT using CoreFFT
        if (inverse) {
            CoreFFT.ifft(real, imag);
        } else {
            CoreFFT.fft(real, imag);
        }

        // Pack results back into interleaved format
        for (int i = 0; i < n; i++) {
            interleavedData[2 * i] = real[i];
            interleavedData[2 * i + 1] = imag[i];
        }
    }
    
    @Override
    public String name() {
        return "adaptive";
    }
    
    @Override
    public String description() {
        return "Adaptive padding - automatically selects optimal strategy based on signal characteristics";
    }
}
