package com.morphiqlabs.wavelet.swt;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.DiscreteWavelet;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResult;
// ParallelConfig removed - using simple parallel execution in core module

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Adapter providing Stationary Wavelet Transform (SWT) functionality using MODWT.
 *
 * <p>The Stationary Wavelet Transform (SWT), also known as the Undecimated Wavelet Transform
 * or À Trous Algorithm, is a shift-invariant wavelet transform that maintains the same data
 * length at each decomposition level. This adapter leverages VectorWave's MODWT implementation,
 * which shares the same mathematical properties as SWT.</p>
 *
 * <p><strong>Scaling and upsampling:</strong> At level j, analysis filters are upsampled with
 * à trous spacing (insert 2^(j−1)−1 zeros between taps) and each stage uses per-level scaling
 * of 1/√2. Over j levels, this composes to the textbook factor 2^(−j/2).</p>
 *
 * <p><strong>Boundary modes:</strong> PERIODIC achieves exact reconstruction; ZERO_PADDING and
 * SYMMETRIC apply linear convolution with the respective extensions. For SYMMETRIC, alignment
 * heuristics match the MODWT cascade semantics (see docs/guides/SYMMETRIC_ALIGNMENT.md).</p>
 * 
 * <p><strong>Key Properties:</strong></p>
 * <ul>
 *   <li><strong>Shift-invariant:</strong> Pattern detection is consistent regardless of signal position</li>
 *   <li><strong>Redundant representation:</strong> All levels have the same length as the original signal</li>
 *   <li><strong>Perfect reconstruction:</strong> Signal can be exactly reconstructed from coefficients</li>
 *   <li><strong>Arbitrary signal length:</strong> No power-of-2 restriction unlike standard DWT</li>
 * </ul>
 * 
 * <p><strong>Usage Example (Lifecycle):</strong></p>
 * <pre>{@code
 * // Create SWT adapter
 * VectorWaveSwtAdapter swt = new VectorWaveSwtAdapter(
 *     Daubechies.DB4, BoundaryMode.PERIODIC);
 * 
 * // Perform SWT decomposition
 * MutableMultiLevelMODWTResult swtResult = swt.forward(signal, 3);
 * 
 * // Modify coefficients (e.g., thresholding for denoising)
 * swt.applyThreshold(swtResult, 2, 0.5, true);  // Soft threshold level 2
 * 
 * // Reconstruct modified signal
 * double[] denoised = swt.inverse(swtResult);
 * 
 * // Or use convenience denoising method
 * double[] denoised2 = swt.denoise(signal, 3, 0.5);
 * }</pre>
 *
 * <p><strong>Resource Management:</strong> This class implements {@link AutoCloseable}.
 * Internally, it may lazily initialize a dedicated {@link java.util.concurrent.ExecutorService}
 * for parallel execution and small filter caches for accelerated multi-level analysis.
 *
 * <ul>
 *   <li><strong>Lazy caches:</strong> Analysis filters are populated on first use and reused per instance.</li>
 *   <li><strong>Dedicated executor:</strong> A private executor is initialized only when needed and
 *   shut down on {@link #close()} or {@link #cleanup()} to avoid impacting shared pools.</li>
 *   <li><strong>Try-with-resources:</strong> Prefer scoped usage to deterministically release resources:</li>
 * </ul>
 *
 * <pre>{@code
 * try (var swt = new VectorWaveSwtAdapter(Daubechies.DB4, BoundaryMode.PERIODIC)) {
 *     var res = swt.forward(signal, 4);
 *     var recon = swt.inverse(res);
 * }
 * }</pre>
 * 
 * <p><strong>Relationship to MODWT:</strong></p>
 * <p>SWT and MODWT are mathematically equivalent transforms with different historical origins.
 * Both provide shift-invariant, redundant wavelet decompositions. This adapter provides an
 * SWT-style interface for users familiar with that terminology while leveraging VectorWave's
 * optimized MODWT implementation.</p>
 * 
 * @see MultiLevelMODWTTransform
 * @see MutableMultiLevelMODWTResult
 * @since 1.0.0
 */
public class VectorWaveSwtAdapter implements AutoCloseable {
    
    private final MultiLevelMODWTTransform modwtTransform;
    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final boolean enableParallel;
    private final int parallelThreshold;
    
    // Internal optimizations - not exposed in public API
    private static final int DEFAULT_PARALLEL_THRESHOLD = 4096;
    
    // Optional parallel executor for large signals
    private final AtomicReference<ExecutorService> parallelExecutorRef = new AtomicReference<>();
    
    // Lightweight analysis filter cache for SWT (populated lazily)
    private final ConcurrentHashMap<Integer, double[]> cachedLowPassAnalysis = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, double[]> cachedHighPassAnalysis = new ConcurrentHashMap<>();
    
    /**
     * Creates a new SWT adapter with specified wavelet and boundary handling.
     *
     * <p>This adapter implements {@link AutoCloseable}. Prefer try-with-resources to
     * ensure resources (internal executor and caches) are released deterministically.</p>
     *
     * <p><strong>Symmetric Boundary Policy:</strong> When using {@link BoundaryMode#SYMMETRIC},
     * inverse alignment applies a small wavelet/level‑specific offset (±τ<sub>j</sub>) to reduce
     * boundary error. Prefer asserting interior‑region equality; use PERIODIC for exactness.
     * See {@code docs/guides/SYMMETRIC_ALIGNMENT.md} for the full policy.</p>
     *
     * @param wavelet the wavelet to use for decomposition
     * @param boundaryMode the boundary handling mode
     * @throws NullPointerException if wavelet or boundaryMode is null
     */
    public VectorWaveSwtAdapter(Wavelet wavelet, BoundaryMode boundaryMode) {
        this(wavelet, boundaryMode, true, DEFAULT_PARALLEL_THRESHOLD);
    }
    
    /**
     * Creates a new SWT adapter with custom parallel configuration.
     *
     * <p>When {@code enableParallel} is true and the signal length is at least
     * {@code parallelThreshold} (and levels {@literal >} 2), {@link #forward(double[], int)} uses
     * chunked parallel convolution. Resources (executor and caches) are initialized lazily
     * and released on {@link #close()} or {@link #cleanup()}.</p>
     *
     * @param wavelet the wavelet to use for decomposition
     * @param boundaryMode the boundary handling mode
     * @param enableParallel whether to enable parallel execution
     * @param parallelThreshold minimum signal size for parallel execution
     * @throws NullPointerException if wavelet or boundaryMode is null
     */
    public VectorWaveSwtAdapter(Wavelet wavelet, BoundaryMode boundaryMode, boolean enableParallel, int parallelThreshold) {
        this.wavelet = Objects.requireNonNull(wavelet, "Wavelet cannot be null");
        this.boundaryMode = Objects.requireNonNull(boundaryMode, "Boundary mode cannot be null");
        this.enableParallel = enableParallel;
        this.parallelThreshold = parallelThreshold;
        this.modwtTransform = new MultiLevelMODWTTransform(wavelet, boundaryMode);
        // Start with empty caches; populate lazily during forward()
    }
    
    // Precompute upsampled-and-scaled analysis filters using à trous spacing 2^(j-1)
    private void precomputeCommonFilters(int levelsToPrecompute) {
        for (int idx = 0; idx < Math.max(0, levelsToPrecompute); idx++) {
            final int level = idx + 1; // 1-based decomposition level
            cachedLowPassAnalysis.computeIfAbsent(level, l ->
                com.morphiqlabs.wavelet.internal.ScalarOps
                    .upsampleAndScaleForIMODWTSynthesis(wavelet.lowPassDecomposition(), l)
            );
            cachedHighPassAnalysis.computeIfAbsent(level, l ->
                com.morphiqlabs.wavelet.internal.ScalarOps
                    .upsampleAndScaleForIMODWTSynthesis(wavelet.highPassDecomposition(), l)
            );
        }
    }
    
    /**
     * Creates a new SWT adapter with periodic boundary handling.
     *
     * <p>See class docs for try-with-resources usage. Call {@link #close()} or
     * use try-with-resources to cleanup.</p>
     *
     * @param wavelet the wavelet to use for decomposition
     * @throws NullPointerException if wavelet is null
     */
    public VectorWaveSwtAdapter(Wavelet wavelet) {
        this(wavelet, BoundaryMode.PERIODIC);
    }
    
    /**
     * Performs forward SWT decomposition to the maximum number of levels.
     * 
     * @param signal the input signal
     * @return mutable SWT decomposition result
     * @throws com.morphiqlabs.wavelet.exception.InvalidSignalException if signal is invalid
     */
    public MutableMultiLevelMODWTResult forward(double[] signal) {
        int maxLevels = modwtTransform.getMaximumLevels(signal.length);
        return forward(signal, maxLevels);
    }
    
    /**
     * Performs forward SWT decomposition to specified number of levels.
     * 
     * @param signal the input signal
     * @param levels number of decomposition levels
     * @return mutable SWT decomposition result
     * @throws com.morphiqlabs.wavelet.exception.InvalidSignalException if signal is invalid
     * @throws com.morphiqlabs.wavelet.exception.InvalidArgumentException if levels is invalid
     */
    public MutableMultiLevelMODWTResult forward(double[] signal, int levels) {
        // Use parallel processing for large signals
        if (enableParallel && signal.length >= parallelThreshold && levels > 2) {
            return forwardParallel(signal, levels);
        }
        return decomposeSWT(signal, levels);
    }
    
    /**
     * Internal parallel implementation for large signal decomposition.
     * Automatically activated based on ParallelConfig settings.
     */
    private MutableMultiLevelMODWTResult forwardParallel(double[] signal, int levels) {
        ensureParallelExecutor();
        
        try {
            // Use parallel executor for multi-level decomposition
            // This is an internal optimization - the public API remains unchanged
            ExecutorService executor = parallelExecutorRef.get();
            if (executor != null && !executor.isShutdown()) {
                int n = signal.length;
                double[] current = signal.clone();
                var result = new com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResultImpl(n, levels);

                // Chunking strategy
                int cores = Math.min(4, Runtime.getRuntime().availableProcessors());
                int chunks = Math.max(cores, 2);
                int baseChunk = Math.max(1, n / chunks);

                for (int level = 1; level <= levels; level++) {
                    double[] h = getOrComputeAnalysisLow(level);
                    double[] g = getOrComputeAnalysisHigh(level);
                    double[] approx = new double[n];
                    double[] detail = new double[n];
                    // Capture current level's input as effectively final for lambda use
                    final double[] signalAtLevel = current;

                    java.util.List<Future<?>> futures = new java.util.ArrayList<>();
                    for (int c = 0; c < chunks; c++) {
                        final int start = c * baseChunk;
                        final int end = (c == chunks - 1) ? n : Math.min(n, (c + 1) * baseChunk);
                        if (start >= end) break;
                        futures.add(executor.submit(() -> {
                            if (boundaryMode == BoundaryMode.PERIODIC) {
                                convolvePeriodicChunk(signalAtLevel, h, g, approx, detail, start, end);
                            } else if (boundaryMode == BoundaryMode.ZERO_PADDING) {
                                convolveZeroPadChunk(signalAtLevel, h, g, approx, detail, start, end);
                            } else {
                                convolveSymmetricChunk(signalAtLevel, h, g, approx, detail, start, end);
                            }
                        }));
                    }
                    for (Future<?> f : futures) f.get();

                    result.setDetailCoeffs(level, detail);
                    current = approx; // Pyramid for next level
                }

                result.setApproximationCoeffs(current);
                return result;
            } else {
                // Fallback to sequential if executor unavailable
                return decomposeSWT(signal, levels);
            }
        } catch (Exception e) {
            // Fallback to sequential on any parallel processing error
            return decomposeSWT(signal, levels);
        }
    }

    // Periodic chunk: compute approx and detail for t in [start,end)
    private static void convolvePeriodicChunk(double[] signal, double[] low, double[] high,
                                              double[] approx, double[] detail,
                                              int start, int end) {
        int n = signal.length;
        int Lh = low.length, Lg = high.length;
        for (int t = start; t < end; t++) {
            double a = 0.0, d = 0.0;
            for (int l = 0; l < Lh; l++) {
                int idx = t - l;
                // robust modulo for negative values
                if (idx < 0) idx = ((idx % n) + n) % n;
                a += low[l] * signal[idx];
            }
            for (int l = 0; l < Lg; l++) {
                int idx = t - l;
                if (idx < 0) idx = ((idx % n) + n) % n;
                d += high[l] * signal[idx];
            }
            approx[t] = a; detail[t] = d;
        }
    }

    // Zero-padding chunk
    private static void convolveZeroPadChunk(double[] signal, double[] low, double[] high,
                                             double[] approx, double[] detail,
                                             int start, int end) {
        int n = signal.length;
        int Lh = low.length, Lg = high.length;
        for (int t = start; t < end; t++) {
            double a = 0.0, d = 0.0;
            for (int l = 0; l < Lh; l++) {
                int idx = t - l;
                if (idx >= 0 && idx < n) a += low[l] * signal[idx];
            }
            for (int l = 0; l < Lg; l++) {
                int idx = t - l;
                if (idx >= 0 && idx < n) d += high[l] * signal[idx];
            }
            approx[t] = a; detail[t] = d;
        }
    }

    // Symmetric extension chunk
    private static void convolveSymmetricChunk(double[] signal, double[] low, double[] high,
                                               double[] approx, double[] detail,
                                               int start, int end) {
        int n = signal.length;
        int Lh = low.length, Lg = high.length;
        for (int t = start; t < end; t++) {
            double a = 0.0, d = 0.0;
            for (int l = 0; l < Lh; l++) {
                int idx = t - l;
                idx = com.morphiqlabs.wavelet.util.MathUtils.symmetricBoundaryExtension(idx, n);
                a += low[l] * signal[idx];
            }
            for (int l = 0; l < Lg; l++) {
                int idx = t - l;
                idx = com.morphiqlabs.wavelet.util.MathUtils.symmetricBoundaryExtension(idx, n);
                d += high[l] * signal[idx];
            }
            approx[t] = a; detail[t] = d;
        }
    }

    /**
     * Performs multi-level SWT (MODWT) decomposition using cached analysis filters.
     * Applies à trous spacing (2^(j-1)) and level scaling (2^(-j/2)).
     */
    private MutableMultiLevelMODWTResult decomposeSWT(double[] signal, int levels) {
        // Basic validation mirrored from MODWT path
        com.morphiqlabs.wavelet.util.ValidationUtils.validateFiniteValues(signal, "signal");
        if (signal.length == 0) {
            throw new com.morphiqlabs.wavelet.exception.InvalidSignalException(
                com.morphiqlabs.wavelet.exception.ErrorCode.VAL_EMPTY,
                com.morphiqlabs.wavelet.exception.ErrorContext.builder("Signal cannot be empty for SWT")
                    .withContext("Transform type", "SWT (MODWT analysis)")
                    .withWavelet(wavelet)
                    .withBoundaryMode(boundaryMode)
                    .withContext("Requested levels", levels)
                    .withSuggestion("Provide a signal with at least one sample")
                    .build()
            );
        }
        int maxLevels = modwtTransform.getMaximumLevels(signal.length);
        if (levels < 1 || levels > maxLevels) {
            throw new com.morphiqlabs.wavelet.exception.InvalidArgumentException(
                com.morphiqlabs.wavelet.exception.ErrorCode.CFG_INVALID_DECOMPOSITION_LEVEL,
                com.morphiqlabs.wavelet.exception.ErrorContext.builder("Invalid SWT decomposition levels")
                    .withLevelInfo(levels, maxLevels)
                    .withSignalInfo(signal.length)
                    .withWavelet(wavelet)
                    .withBoundaryMode(boundaryMode)
                    .withSuggestion("Choose a level between 1 and " + maxLevels)
                    .build()
            );
        }

        int n = signal.length;
        double[] current = signal.clone();
        var result = new com.morphiqlabs.wavelet.modwt.MutableMultiLevelMODWTResultImpl(n, levels);

        for (int level = 1; level <= levels; level++) {
            double[] h = getOrComputeAnalysisLow(level);
            double[] g = getOrComputeAnalysisHigh(level);
            double[] approx = new double[n];
            double[] detail = new double[n];

            // Apply boundary-specific convolution without downsampling
            if (boundaryMode == BoundaryMode.PERIODIC) {
                com.morphiqlabs.wavelet.internal.ScalarOps.circularConvolveMODWT(current, h, approx);
                com.morphiqlabs.wavelet.internal.ScalarOps.circularConvolveMODWT(current, g, detail);
            } else if (boundaryMode == BoundaryMode.ZERO_PADDING) {
                com.morphiqlabs.wavelet.internal.ScalarOps.zeroPaddingConvolveMODWT(current, h, approx);
                com.morphiqlabs.wavelet.internal.ScalarOps.zeroPaddingConvolveMODWT(current, g, detail);
            } else {
                com.morphiqlabs.wavelet.internal.ScalarOps.symmetricConvolveMODWT(current, h, approx);
                com.morphiqlabs.wavelet.internal.ScalarOps.symmetricConvolveMODWT(current, g, detail);
            }

            result.setDetailCoeffs(level, detail);
            current = approx; // Pyramid for next level
        }

        result.setApproximationCoeffs(current);
        return result;
    }

    private double[] getOrComputeAnalysisLow(int level) {
        return cachedLowPassAnalysis.computeIfAbsent(level, l ->
            com.morphiqlabs.wavelet.internal.ScalarOps
                .upsampleAndScaleForIMODWTSynthesis(wavelet.lowPassDecomposition(), l)
        );
    }

    private double[] getOrComputeAnalysisHigh(int level) {
        return cachedHighPassAnalysis.computeIfAbsent(level, l ->
            com.morphiqlabs.wavelet.internal.ScalarOps
                .upsampleAndScaleForIMODWTSynthesis(wavelet.highPassDecomposition(), l)
        );
    }
    
    /**
     * Ensures parallel executor is initialized for large signal processing.
     * Uses modern atomic operations for thread-safe lazy initialization.
     */
    private void ensureParallelExecutor() {
        if (parallelExecutorRef.get() == null) {
            int cores = Math.min(4, Runtime.getRuntime().availableProcessors());
            ExecutorService newExecutor = Executors.newFixedThreadPool(cores);
            if (!parallelExecutorRef.compareAndSet(null, newExecutor)) {
                // Another thread beat us to it, shutdown the one we created
                newExecutor.shutdown();
            }
        }
    }
    
    /**
     * Performs inverse SWT reconstruction from decomposition result.
     * 
     * <p>This method reconstructs the signal from potentially modified coefficients,
     * making it suitable for applications like denoising or feature extraction.</p>
     * 
     * @param result the SWT decomposition result (possibly modified)
     * @return reconstructed signal
     * @throws NullPointerException if result is null
     */
    public double[] inverse(MutableMultiLevelMODWTResult result) {
        Objects.requireNonNull(result, "Result cannot be null");
        if (boundaryMode == BoundaryMode.PERIODIC) {
            return reconstructPeriodic(result);
        }
        // For non-periodic modes, delegate to core MODWT reconstruction
        return modwtTransform.reconstruct(result);
    }

    private double[] reconstructPeriodic(MutableMultiLevelMODWTResult result) {
        int n = result.getSignalLength();
        int J = result.getLevels();
        double[] current = result.getApproximationCoeffs().clone();

        for (int level = J; level >= 1; level--) {
            double[] details = result.getDetailCoeffsAtLevel(level);
            double[] h = com.morphiqlabs.wavelet.internal.ScalarOps
                .upsampleAndScaleForIMODWTSynthesis(wavelet.lowPassReconstruction(), level);
            double[] g = com.morphiqlabs.wavelet.internal.ScalarOps
                .upsampleAndScaleForIMODWTSynthesis(wavelet.highPassReconstruction(), level);

            double[] next = new double[n];
            int Lh = h.length;
            int Lg = g.length;
            for (int t = 0; t < n; t++) {
                double sum = 0.0;
                for (int l = 0; l < Lh; l++) {
                    int idx = (t + l) % n;
                    sum += h[l] * current[idx];
                }
                for (int l = 0; l < Lg; l++) {
                    int idx = (t + l) % n;
                    sum += g[l] * details[idx];
                }
                next[t] = sum;
            }
            current = next;
        }
        return current;
    }
    
    /**
     * Applies hard or soft thresholding to coefficients at a specific level.
     * 
     * <p>This is a convenience method for coefficient thresholding, commonly used
     * in wavelet denoising applications.</p>
     * 
     * @param result the SWT result to modify
     * @param level the decomposition level (0 for approximation, 1+ for details)
     * @param threshold the threshold value
     * @param soft if true, applies soft thresholding; if false, applies hard thresholding
     * @throws NullPointerException if result is null
     * @throws IllegalArgumentException if level is out of range
     */
    public void applyThreshold(MutableMultiLevelMODWTResult result, int level, 
                               double threshold, boolean soft) {
        Objects.requireNonNull(result, "Result cannot be null");
        result.applyThreshold(level, threshold, soft);
    }
    
    /**
     * Applies universal threshold to all detail levels for denoising.
     * 
     * <p>The universal threshold is calculated as σ√(2log(N)) where σ is the
     * noise standard deviation estimated from the finest detail level.</p>
     * 
     * @param result the SWT result to denoise
     * @param soft if true, applies soft thresholding; if false, applies hard thresholding
     * @throws NullPointerException if result is null
     */
    public void applyUniversalThreshold(MutableMultiLevelMODWTResult result, boolean soft) {
        Objects.requireNonNull(result, "Result cannot be null");
        
        // Estimate noise from finest detail level
        double[] finestDetails = result.getMutableDetailCoeffs(1);
        double sigma = estimateNoiseSigma(finestDetails);
        
        // Calculate universal threshold
        int n = result.getSignalLength();
        double threshold = sigma * Math.sqrt(2 * Math.log(n));
        
        // Apply to all detail levels
        for (int level = 1; level <= result.getLevels(); level++) {
            applyThreshold(result, level, threshold, soft);
        }
    }
    
    /**
     * Convenience method for signal denoising using SWT.
     * 
     * <p>Performs decomposition, applies universal soft thresholding, and reconstructs.</p>
     * 
     * @param signal the noisy signal
     * @param levels number of decomposition levels
     * @return denoised signal
     * @throws IllegalArgumentException if signal or levels is invalid
     */
    public double[] denoise(double[] signal, int levels) {
        return denoise(signal, levels, -1, true);
    }
    
    /**
     * Convenience method for signal denoising with custom threshold.
     * 
     * @param signal the noisy signal
     * @param levels number of decomposition levels
     * @param threshold custom threshold value (use -1 for universal threshold)
     * @param soft if true, applies soft thresholding; if false, applies hard thresholding
     * @return denoised signal
     * @throws IllegalArgumentException if signal or levels is invalid
     */
    public double[] denoise(double[] signal, int levels, double threshold, boolean soft) {
        // Decompose signal
        MutableMultiLevelMODWTResult result = forward(signal, levels);
        
        if (threshold < 0) {
            // Use universal threshold
            applyUniversalThreshold(result, soft);
        } else {
            // Apply custom threshold to all detail levels
            for (int level = 1; level <= levels; level++) {
                applyThreshold(result, level, threshold, soft);
            }
        }
        
        // Reconstruct denoised signal
        return inverse(result);
    }
    
    /**
     * Extracts features at a specific decomposition level.
     * 
     * <p>This method zeros out all other levels and reconstructs, effectively
     * extracting features present only at the specified scale.</p>
     * 
     * @param signal the input signal
     * @param levels total number of decomposition levels
     * @param targetLevel the level to extract (0 for approximation, 1+ for details)
     * @return signal containing only features at the target level
     * @throws IllegalArgumentException if parameters are invalid
     */
    public double[] extractLevel(double[] signal, int levels, int targetLevel) {
        // Decompose signal
        MutableMultiLevelMODWTResult result = forward(signal, levels);
        
        // Zero out all levels except target
        for (int level = 1; level <= levels; level++) {
            if (level != targetLevel) {
                double[] coeffs = result.getMutableDetailCoeffs(level);
                java.util.Arrays.fill(coeffs, 0.0);
            }
        }
        
        // Zero approximation if not target
        if (targetLevel != 0) {
            double[] approx = result.getMutableApproximationCoeffs();
            java.util.Arrays.fill(approx, 0.0);
        }
        
        result.clearCaches();
        
        // Reconstruct with only target level
        return inverse(result);
    }
    
    /**
     * Gets the wavelet used by this adapter.
     * 
     * @return the wavelet
     */
    public Wavelet getWavelet() {
        return wavelet;
    }
    
    /**
     * Gets the boundary mode used by this adapter.
     * 
     * @return the boundary mode
     */
    public BoundaryMode getBoundaryMode() {
        return boundaryMode;
    }
    
    /**
     * Estimates noise standard deviation using median absolute deviation (MAD).
     * 
     * <p>This robust estimator is commonly used in wavelet denoising:
     * σ = median(|coeffs|) / 0.6745</p>
     * 
     * @param coeffs wavelet coefficients (typically finest detail level)
     * @return estimated noise standard deviation
     */
    private static double estimateNoiseSigma(double[] coeffs) {
        // Calculate absolute values
        double[] absCoeffs = new double[coeffs.length];
        for (int i = 0; i < coeffs.length; i++) {
            absCoeffs[i] = Math.abs(coeffs[i]);
        }
        
        // Find median
        java.util.Arrays.sort(absCoeffs);
        double median;
        if (absCoeffs.length % 2 == 0) {
            median = (absCoeffs[absCoeffs.length / 2 - 1] + absCoeffs[absCoeffs.length / 2]) / 2.0;
        } else {
            median = absCoeffs[absCoeffs.length / 2];
        }
        
        // MAD estimator for Gaussian noise
        return median / 0.6745;
    }
    
    /**
     * Releases internal resources used for optimization.
     * Call this method when done with the adapter to free up resources.
     * The adapter remains functional after cleanup but may be slower.
     */
    public void cleanup() {
        // Shutdown parallel executor if initialized
        ExecutorService executor = parallelExecutorRef.getAndSet(null);
        if (executor != null) {
            executor.shutdown();
        }
        // Clear cached filters
        cachedLowPassAnalysis.clear();
        cachedHighPassAnalysis.clear();
    }

    /**
     * Closes this adapter and releases resources. Safe to call multiple times.
     */
    @Override
    public void close() {
        cleanup();
    }
    
    /**
     * Gets cache statistics for monitoring optimization effectiveness.
     * This is primarily for internal diagnostics and testing.
     * 
     * @return map with cache statistics
     */
    public Map<String, Object> getCacheStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        // Report number of precomputed levels (min of both caches)
        int levelsCached = Math.min(cachedLowPassAnalysis.size(), cachedHighPassAnalysis.size());
        stats.put("filterCacheSize", levelsCached);
        stats.put("parallelExecutorActive", parallelExecutorRef.get() != null);
        stats.put("parallelThreshold", parallelThreshold);
        return stats;
    }
}
