/**
 * VectorWave Performance Benchmark Suite.
 * 
 * <p>This package contains comprehensive performance benchmarks for the VectorWave library,
 * organized into several categories:</p>
 * 
 * <h2>Core vs Extensions Comparison</h2>
 * <ul>
 *   <li>{@link CoreVsExtensionsBenchmark} - Compares scalar vs SIMD implementations</li>
 *   <li>{@link VectorAPISpeedupBenchmark} - Measures Vector API acceleration</li>
 * </ul>
 * 
 * <h2>Transform Benchmarks</h2>
 * <ul>
 *   <li>{@link MODWTBenchmark} - MODWT transform performance</li>
 *   <li>{@link CWTBenchmark} - CWT analysis performance</li>
 *   <li>{@link SWTBenchmark} - SWT adapter performance</li>
 * </ul>
 * 
 * <h2>Memory Efficiency</h2>
 * <ul>
 *   <li>{@link MemoryEfficiencyBenchmark} - Memory usage patterns</li>
 *   <li>{@link StreamingBenchmark} - Streaming vs batch processing</li>
 * </ul>
 * 
 * <h2>Financial Analysis</h2>
 * <ul>
 *   <li>{@link FinancialMetricsBenchmark} - Financial wavelet analysis</li>
 *   <li>{@link RealTimeProcessingBenchmark} - Tick data processing</li>
 * </ul>
 * 
 * <h2>Running Benchmarks</h2>
 * <pre>
 * // Run all benchmarks
 * mvn exec:java -Dexec.mainClass="com.morphiqlabs.benchmark.BenchmarkRunner"
 * 
 * // Run specific benchmark
 * mvn exec:java -Dexec.mainClass="com.morphiqlabs.benchmark.MODWTBenchmark"
 * 
 * // Run with JMH options
 * java -jar target/benchmarks.jar -wi 3 -i 5 -f 1
 * </pre>
 */
package com.morphiqlabs.benchmark;