/**
 * Parallel processing implementations for VectorWave using Java 24 features.
 * 
 * <p>This package provides high-performance parallel implementations of wavelet
 * transforms and related operations, leveraging modern Java concurrency features:</p>
 * 
 * <ul>
 *   <li><b>Virtual Threads:</b> Efficient handling of I/O-bound operations</li>
 *   <li><b>Structured Concurrency:</b> Safe and predictable parallel execution</li>
 *   <li><b>Fork/Join Framework:</b> Work-stealing for CPU-intensive tasks</li>
 *   <li><b>Vector API:</b> SIMD parallelism at the instruction level</li>
 * </ul>
 * 
 * <h2>Key Components</h2>
 * 
 * <h3>ParallelConfig</h3>
 * <p>Central configuration for all parallel operations, providing:</p>
 * <ul>
 *   <li>Automatic detection of optimal parallelism levels</li>
 *   <li>Adaptive threshold calculation</li>
 *   <li>Cost-based execution mode selection</li>
 *   <li>Performance metrics tracking</li>
 * </ul>
 * 
 * <h3>ParallelMultiLevelTransform</h3>
 * <p>Parallel implementation of multi-level wavelet decomposition:</p>
 * <ul>
 *   <li>3-5x speedup for 5+ levels</li>
 *   <li>Independent level processing</li>
 *   <li>Efficient memory management</li>
 * </ul>
 * 
 * <h3>ParallelWaveletDenoiser (Coming Soon)</h3>
 * <p>Parallel denoising across coefficient levels</p>
 * 
 * <h3>ParallelCWTTransform (Coming Soon)</h3>
 * <p>Fine-grained parallelization of continuous wavelet transform</p>
 * 
 * <h2>Usage Examples</h2>
 * 
 * <h3>Basic Usage</h3>
 * <pre>{@code
 * // Auto-configured parallel transform
 * ParallelMultiLevelTransform transform = new ParallelMultiLevelTransform(
 *     Daubechies.DB4, 
 *     BoundaryMode.PERIODIC
 * );
 * 
 * MultiLevelMODWTResult result = transform.forward(signal, 5);
 * }</pre>
 * 
 * <h3>Custom Configuration</h3>
 * <pre>{@code
 * ParallelConfig config = ParallelConfig.builder()
 *     .parallelismLevel(16)
 *     .parallelThreshold(512)
 *     .mode(ExecutionMode.ADAPTIVE)
 *     .enableMetrics(true)
 *     .build();
 * 
 * ParallelMultiLevelTransform transform = new ParallelMultiLevelTransform(
 *     Daubechies.DB4,
 *     BoundaryMode.PERIODIC,
 *     config
 * );
 * }</pre>
 * 
 * <h3>Performance Monitoring</h3>
 * <pre>{@code
 * // Get execution statistics
 * ParallelConfig.ExecutionStats stats = transform.getStats();
 * System.out.printf("Parallel executions: %d (%.1f%%)%n", 
 *     stats.parallelExecutions(),
 *     stats.parallelRatio() * 100);
 * System.out.printf("Estimated speedup: %.2fx%n", 
 *     stats.estimatedSpeedup());
 * }</pre>
 * 
 * <h2>Performance Guidelines</h2>
 * 
 * <h3>When to Use Parallel Processing</h3>
 * <ul>
 *   <li>Signal length > 1024 samples</li>
 *   <li>Multi-level decomposition with 3+ levels</li>
 *   <li>Batch processing of multiple signals</li>
 *   <li>Complex wavelet operations (CWT, complex arithmetic)</li>
 * </ul>
 * 
 * <h3>When to Avoid Parallel Processing</h3>
 * <ul>
 *   <li>Small signals ({@literal <} 512 samples)</li>
 *   <li>Single-level transforms</li>
 *   <li>Real-time processing with strict latency requirements</li>
 *   <li>Memory-constrained environments</li>
 * </ul>
 * 
 * <h2>Thread Safety</h2>
 * <p>All parallel implementations in this package are thread-safe and can be
 * used concurrently from multiple threads. The underlying transforms maintain
 * no mutable state between operations.</p>
 * 
 * <h2>Resource Management</h2>
 * <p>The parallel implementations automatically manage thread pools and other
 * resources. For long-running applications, consider calling
 * {@code ParallelConfig.shutdown()} when done to release resources.</p>
 */
package com.morphiqlabs.wavelet.extensions.parallel;
