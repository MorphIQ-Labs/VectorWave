/**
 * VectorWave Performance Benchmark Suite.
 * 
 * <p>This package contains comprehensive performance benchmarks for the VectorWave library,
 * organized into several categories:</p>
 * 
 * <h2>Core vs Extensions Comparison</h2>
 * <ul>
 *   <li>{@link CoreVsExtensionsBenchmark} - Compares scalar vs SIMD implementations</li>
 * </ul>
 * 
 * <h2>Transform Benchmarks</h2>
 * <ul>
 *   <li>{@link MODWTBenchmark} - MODWT transform performance</li>
 * </ul>
 * 
 * <h2>Memory Efficiency</h2>
 * <ul>
 *   <li>{@link MemoryEfficiencyBenchmark} - Memory usage patterns</li>
 * </ul>
 * 
 * <h2>Financial Analysis</h2>
 * <ul>
 *   <li>{@code FinancialMetricsBenchmark} - Financial wavelet analysis</li>
 *   <li>{@code RealTimeProcessingBenchmark} - Tick data processing</li>
 * </ul>
 * 
 * <h2>Running Benchmarks</h2>
 * <pre>
 * // Run all benchmarks
 * mvn -q -pl vectorwave-benchmarks -am exec:java -Dexec.mainClass="org.openjdk.jmh.Main"
 * 
 * // Run specific benchmark
 * mvn -q -pl vectorwave-benchmarks -am exec:java -Dexec.mainClass="org.openjdk.jmh.Main" -Dexec.args="com.morphiqlabs.benchmark.MODWTBenchmark"
 * 
 * // Run with JMH options
 * mvn -q -pl vectorwave-benchmarks -am exec:java -Dexec.mainClass="org.openjdk.jmh.Main" -Dexec.args="-wi 3 -i 5 -f 1"
 * </pre>
 */
package com.morphiqlabs.benchmark;

