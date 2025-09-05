package com.morphiqlabs.wavelet.extensions.parallel;

import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.api.BoundaryMode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Arrays;

/**
 * High-level parallel wavelet transform using structured concurrency.
 * 
 * <p>This class provides a convenient, high-level API for parallel wavelet transforms
 * built on Java 24's structured concurrency features. It automatically handles resource
 * management, error propagation, and performance optimization for wavelet transform
 * workloads.</p>
 * 
 * <h2>Key Advantages Over Traditional Parallelism</h2>
 * <ul>
 *   <li><b>Automatic Resource Management:</b> All worker threads and tasks are 
 *       automatically cleaned up, eliminating thread leaks</li>
 *   <li><b>Cancellation Propagation:</b> Cancelling a batch operation automatically
 *       cancels all individual transform tasks</li>
 *   <li><b>Timeout Support:</b> Built-in timeout with automatic task cancellation</li>
 *   <li><b>Simplified Error Handling:</b> Centralized exception handling with
 *       clear error propagation</li>
 *   <li><b>Performance Optimization:</b> Integrates with adaptive threshold tuning
 *       and workload-specific configuration</li>
 *   <li><b>Memory Safety:</b> No dangling tasks or resource leaks</li>
 * </ul>
 * 
 * <h2>Usage Patterns</h2>
 * 
 * <h3>Simple Batch Processing</h3>
 * <pre>{@code
 * // Configure for your workload
 * ParallelConfig config = ParallelConfig.builder()
 *     .targetCores(Runtime.getRuntime().availableProcessors())
 *     .minParallelThreshold(512)
 *     .build();
 * 
 * StructuredParallelTransform transform = new StructuredParallelTransform(
 *     Daubechies.DB4, BoundaryMode.PERIODIC, config);
 * 
 * // Process batch of signals
 * double[][] signals = loadSignals();
 * MODWTResult[] results = transform.forwardBatch(signals);
 * }</pre>
 * 
 * <h3>Timeout-Aware Processing</h3>
 * <pre>{@code
 * try {
 *     // 5-second timeout for batch processing
 *     MODWTResult[] results = transform.forwardBatchWithTimeout(signals, 5000);
 * } catch (ComputationException e) {
 *     if (e.getMessage().contains("timeout")) {
 *         // Handle timeout - all tasks automatically cancelled
 *         System.err.println("Processing timed out");
 *     } else {
 *         // Handle other errors
 *         System.err.println("Processing failed: " + e.getMessage());
 *     }
 * }
 * }</pre>
 * 
 * <h3>Progress Monitoring</h3>
 * <pre>{@code
 * MODWTResult[] results = transform.forwardBatchWithProgress(signals,
 *     (completed, total) -> {
 *         double percentage = (100.0 * completed) / total;
 *         System.out.printf("Progress: %.1f%% complete%n", percentage);
 *     });
 * }</pre>
 * 
 * <h3>Asynchronous Processing</h3>
 * <pre>{@code
 * // Single signal async processing
 * CompletableFuture<MODWTResult> future = transform.forwardAsync(signal);
 * 
 * // Can cancel if needed
 * if (shouldCancel) {
 *     future.cancel(true);
 * }
 * 
 * // Get result with timeout
 * try {
 *     MODWTResult result = future.get(30, TimeUnit.SECONDS);
 * } catch (TimeoutException e) {
 *     future.cancel(true);
 *     System.err.println("Transform timed out");
 * }
 * }</pre>
 * 
 * <h4>Large Signal Processing:</h4>
 * <pre>{@code
 * // For very large signals, use chunked processing
 * double[] largeSignal = loadLargeSignal(); // e.g., 1M samples
 * int chunkSize = 8192; // Process in 8K chunks
 * 
 * MODWTResult result = transform.forwardChunked(largeSignal, chunkSize);
 * }</pre>
 * 
 * <h3>Performance Optimization</h3>
 * <p>The class automatically optimizes performance based on:</p>
 * <ul>
 *   <li><b>Signal Size:</b> Small signals processed sequentially to avoid overhead</li>
 *   <li><b>Batch Size:</b> Large batches parallelized across available cores</li>
 *   <li><b>Hardware:</b> Adapts to CPU architecture and core count</li>
 *   <li><b>Workload:</b> Uses adaptive thresholding for optimal performance</li>
 * </ul>
 * 
 * <h3>Error Handling</h3>
 * <p>All methods may throw {@link ComputationException} for:</p>
 * <ul>
 *   <li>Individual transform failures</li>
 *   <li>Timeout exceeded</li>
 *   <li>Resource exhaustion</li>
 *   <li>Thread interruption</li>
 * </ul>
 * 
 * <h3>Thread Safety</h3>
 * <p>This class is thread-safe and can be safely shared between threads. However,
 * individual operations create their own structured scopes and should not be
 * called concurrently on the same instance for optimal performance.</p>
 * 
 * @see StructuredExecutor For low-level structured concurrency operations
 * @see AdaptiveThresholdTuner For automatic performance optimization
 * @see ParallelConfig For configuration options
 */
public class StructuredParallelTransform {
    
    private final MODWTTransform transform;
    private final ParallelConfig config;
    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    
    /**
     * Creates a new structured parallel transform.
     * 
     * @param wavelet the wavelet to use
     * @param boundaryMode the boundary handling mode
     */
    public StructuredParallelTransform(Wavelet wavelet, BoundaryMode boundaryMode) {
        this(wavelet, boundaryMode, ParallelConfig.auto());
    }
    
    /**
     * Creates a new structured parallel transform with configuration.
     * 
     * @param wavelet the wavelet to use
     * @param boundaryMode the boundary handling mode
     * @param config the parallel configuration
     */
    public StructuredParallelTransform(Wavelet wavelet, BoundaryMode boundaryMode, 
                                      ParallelConfig config) {
        this.wavelet = wavelet;
        this.boundaryMode = boundaryMode;
        this.transform = new MODWTTransform(wavelet, boundaryMode);
        this.config = config;
    }
    
    /**
     * Performs parallel forward transform on a batch of signals.
     * 
     * @param signals array of input signals
     * @return array of transform results
     * @throws ComputationException if any transform fails
     */
    public MODWTResult[] forwardBatch(double[][] signals) {
        return forwardBatchWithTimeout(signals, Long.MAX_VALUE);
    }
    
    /**
     * Performs parallel forward transform with timeout.
     * 
     * @param signals array of input signals
     * @param timeoutMs timeout in milliseconds
     * @return array of transform results
     * @throws ComputationException if any transform fails or timeout occurs
     */
    public MODWTResult[] forwardBatchWithTimeout(double[][] signals, long timeoutMs) {
        if (signals == null || signals.length == 0) {
            return new MODWTResult[0];
        }
        
        // Check if parallelization is worthwhile
        boolean shouldParallelize = signals.length > 1 && 
            config.shouldParallelize(signals.length * signals[0].length, 1.0);
        
        if (!shouldParallelize) {
            // Sequential processing
            return sequentialTransform(signals);
        }
        
        // Parallel processing with structured concurrency
        try (var executor = new StructuredExecutor(config, timeoutMs)) {
            List<double[]> signalList = Arrays.asList(signals);
            List<MODWTResult> results = executor.invokeAll(signalList, 
                signal -> transform.forward(signal));
            
            return results.toArray(new MODWTResult[0]);
        } catch (ExecutionException e) {
            throw new ComputationException("Transform failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ComputationException("Transform interrupted", e);
        } catch (CompletionException e) {
            // Handle timeout or other completion exceptions
            throw new ComputationException("Transform failed: " + e.getMessage(), e.getCause());
        } catch (RejectedExecutionException e) {
            // Handle deadline exceeded
            throw new ComputationException("Transform rejected: " + e.getMessage(), e);
        }
    }
    
    /**
     * Performs parallel inverse transform on a batch of results.
     * 
     * @param results array of transform results
     * @return array of reconstructed signals
     * @throws ComputationException if any inverse transform fails
     */
    public double[][] inverseBatch(MODWTResult[] results) {
        return inverseBatchWithTimeout(results, Long.MAX_VALUE);
    }
    
    /**
     * Performs parallel inverse transform with timeout.
     * 
     * @param results array of transform results
     * @param timeoutMs timeout in milliseconds
     * @return array of reconstructed signals
     * @throws ComputationException if any inverse transform fails or timeout occurs
     */
    public double[][] inverseBatchWithTimeout(MODWTResult[] results, long timeoutMs) {
        if (results == null || results.length == 0) {
            return new double[0][];
        }
        
        // Check if parallelization is worthwhile
        boolean shouldParallelize = results.length > 1 &&
            config.shouldParallelize(results.length * results[0].approximationCoeffs().length, 1.0);
        
        if (!shouldParallelize) {
            // Sequential processing
            return sequentialInverse(results);
        }
        
        // Parallel processing with structured concurrency
        try (var executor = new StructuredExecutor(config, timeoutMs)) {
            List<MODWTResult> resultList = Arrays.asList(results);
            List<double[]> reconstructed = executor.invokeAll(resultList,
                result -> transform.inverse(result));
            
            return reconstructed.toArray(new double[0][]);
        } catch (ExecutionException e) {
            throw new ComputationException("Inverse transform failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ComputationException("Inverse transform interrupted", e);
        }
    }
    
    /**
     * Performs asynchronous forward transform.
     * 
     * @param signal the input signal
     * @return future for the transform result
     */
    public CompletableFuture<MODWTResult> forwardAsync(double[] signal) {
        return CompletableFuture.supplyAsync(
            () -> transform.forward(signal),
            config.getCPUExecutor()
        );
    }
    
    /**
     * Performs asynchronous inverse transform.
     * 
     * @param result the transform result
     * @return future for the reconstructed signal
     */
    public CompletableFuture<double[]> inverseAsync(MODWTResult result) {
        return CompletableFuture.supplyAsync(
            () -> transform.inverse(result),
            config.getCPUExecutor()
        );
    }
    
    /**
     * Performs streaming transform with back-pressure support.
     * 
     * @param signalStream stream of input signals
     * @param maxConcurrency maximum concurrent transforms
     * @return stream of transform results
     */
    public Flow.Publisher<MODWTResult> transformStream(Flow.Publisher<double[]> signalStream,
                                                       int maxConcurrency) {
        return new TransformProcessor(signalStream, maxConcurrency);
    }
    
    /**
     * Performs chunked parallel transform for very large signals.
     * 
     * @param signal large input signal
     * @param chunkSize size of each chunk
     * @return combined transform result
     */
    public MODWTResult forwardChunked(double[] signal, int chunkSize) {
        if (signal.length <= chunkSize) {
            return transform.forward(signal);
        }
        
        int numChunks = (signal.length + chunkSize - 1) / chunkSize;
        
        try (var executor = new StructuredExecutor(config)) {
            List<Integer> chunkIndices = new ArrayList<>();
            for (int i = 0; i < numChunks; i++) {
                chunkIndices.add(i);
            }
            
            List<MODWTResult> chunkResults = executor.invokeAll(chunkIndices, i -> {
                int start = i * chunkSize;
                int end = Math.min(start + chunkSize, signal.length);
                double[] chunk = Arrays.copyOfRange(signal, start, end);
                return transform.forward(chunk);
            });
            
            // Combine chunk results
            return combineChunkResults(chunkResults);
        } catch (ExecutionException e) {
            throw new ComputationException("Chunked transform failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ComputationException("Chunked transform interrupted", e);
        }
    }
    
    /**
     * Performs transform with progress reporting.
     * 
     * @param signals array of input signals
     * @param progressCallback callback for progress updates
     * @return array of transform results
     */
    public MODWTResult[] forwardBatchWithProgress(double[][] signals,
                                                  ProgressCallback progressCallback) {
        if (signals == null || signals.length == 0) {
            return new MODWTResult[0];
        }
        
        MODWTResult[] results = new MODWTResult[signals.length];
        AtomicInteger completedCount = new AtomicInteger(0);
        
        try (var executor = new StructuredExecutor(config)) {
            List<StructuredExecutor.StructuredFuture<MODWTResult>> futures = new ArrayList<>();
            
            for (int i = 0; i < signals.length; i++) {
                final int index = i;
                futures.add(executor.submit(() -> {
                    MODWTResult result = transform.forward(signals[index]);
                    int completed = completedCount.incrementAndGet();
                    progressCallback.onProgress(completed, signals.length);
                    return result;
                }));
            }
            
            executor.joinAll();
            
            for (int i = 0; i < futures.size(); i++) {
                results[i] = futures.get(i).get();
            }
            
            // Always ensure final progress is reported after all tasks complete
            progressCallback.onProgress(signals.length, signals.length);
            
            return results;
        } catch (ExecutionException e) {
            throw new ComputationException("Transform failed", e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ComputationException("Transform interrupted", e);
        }
    }
    
    // Private helper methods
    
    private MODWTResult[] sequentialTransform(double[][] signals) {
        MODWTResult[] results = new MODWTResult[signals.length];
        for (int i = 0; i < signals.length; i++) {
            results[i] = transform.forward(signals[i]);
        }
        return results;
    }
    
    private double[][] sequentialInverse(MODWTResult[] results) {
        double[][] signals = new double[results.length][];
        for (int i = 0; i < results.length; i++) {
            signals[i] = transform.inverse(results[i]);
        }
        return signals;
    }
    
    private MODWTResult combineChunkResults(List<MODWTResult> chunkResults) {
        // Combine approximation and detail coefficients from all chunks
        int totalLength = chunkResults.stream()
            .mapToInt(r -> r.approximationCoeffs().length)
            .sum();
        
        double[] combinedApprox = new double[totalLength];
        double[] combinedDetail = new double[totalLength];
        
        int offset = 0;
        for (MODWTResult chunk : chunkResults) {
            System.arraycopy(chunk.approximationCoeffs(), 0, 
                           combinedApprox, offset, chunk.approximationCoeffs().length);
            System.arraycopy(chunk.detailCoeffs(), 0,
                           combinedDetail, offset, chunk.detailCoeffs().length);
            offset += chunk.approximationCoeffs().length;
        }
        
        return MODWTResult.create(combinedApprox, combinedDetail);
    }
    
    /**
     * Reactive streams processor for transform operations.
     */
    private class TransformProcessor implements Flow.Processor<double[], MODWTResult> {
        private final Flow.Publisher<double[]> upstream;
        private final int maxConcurrency;
        private Flow.Subscriber<? super MODWTResult> downstream;
        private Flow.Subscription upstreamSubscription;
        private final StructuredExecutor executor;
        private final Semaphore concurrencyLimit;
        
        TransformProcessor(Flow.Publisher<double[]> upstream, int maxConcurrency) {
            this.upstream = upstream;
            this.maxConcurrency = maxConcurrency;
            this.executor = new StructuredExecutor(config);
            this.concurrencyLimit = new Semaphore(maxConcurrency);
        }
        
        @Override
        public void subscribe(Flow.Subscriber<? super MODWTResult> subscriber) {
            this.downstream = subscriber;
            upstream.subscribe(this);
        }
        
        @Override
        public void onSubscribe(Flow.Subscription subscription) {
            this.upstreamSubscription = subscription;
            downstream.onSubscribe(new Flow.Subscription() {
                @Override
                public void request(long n) {
                    upstreamSubscription.request(n);
                }
                
                @Override
                public void cancel() {
                    upstreamSubscription.cancel();
                    executor.cancelAll();
                }
            });
        }
        
        @Override
        public void onNext(double[] signal) {
            try {
                concurrencyLimit.acquire();
                executor.submit(() -> {
                    try {
                        MODWTResult result = transform.forward(signal);
                        downstream.onNext(result);
                    } finally {
                        concurrencyLimit.release();
                    }
                    return null;
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                downstream.onError(e);
            }
        }
        
        @Override
        public void onError(Throwable throwable) {
            downstream.onError(throwable);
            executor.close();
        }
        
        @Override
        public void onComplete() {
            try {
                executor.joinAll();
                downstream.onComplete();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                downstream.onError(e);
            } finally {
                executor.close();
            }
        }
    }
    
    /**
     * Callback interface for progress reporting.
     */
    @FunctionalInterface
    public interface ProgressCallback {
        /**
         * Called when progress is made.
         * 
         * @param completed number of completed items
         * @param total total number of items
         */
        void onProgress(int completed, int total);
    }
    
    /**
     * Exception thrown when a computation fails.
     */
    public static class ComputationException extends RuntimeException {
        private static final long serialVersionUID = 1L;
        
        /**
         * Creates a computation exception.
         * @param message error message
         * @param cause root cause
         */
        public ComputationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
