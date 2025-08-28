package com.morphiqlabs.wavelet.util;

import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import java.util.concurrent.TimeUnit;

/**
 * Performance benchmark to demonstrate the impact of removing redundant validation checks.
 * 
 * <p>This test is controlled by a system property to avoid running during regular test execution.
 * To run this benchmark manually, you have several options:
 * 
 * <p>Option 1 - Use system property with Maven:
 * <pre>
 * mvn test -Dtest=ValidationPerformanceTest -Drun.benchmarks=true
 * </pre>
 * 
 * <p>Option 2 - Use the JMH benchmarks instead (recommended):
 * <pre>
 * For Unix-like systems:
 * java -cp target/test-classes:target/classes --add-modules jdk.incubator.vector \
 *   com.morphiqlabs.wavelet.benchmark.ValidationBenchmark
 * 
 * For Windows:
 * java -cp target/test-classes;target/classes --add-modules jdk.incubator.vector ^
 *   com.morphiqlabs.wavelet.benchmark.ValidationBenchmark
 * </pre>
 * 
 * <p>For production benchmarking, consider using JMH (Java Microbenchmark Harness)
 * which provides more accurate measurements with proper warm-up, statistical analysis,
 * and protection against JVM optimizations that could skew results.
 */
@EnabledIfSystemProperty(named = "run.benchmarks", matches = "true")
@Timeout(value = 60, unit = TimeUnit.SECONDS)
class ValidationPerformanceTest {

    private static final int ARRAY_SIZE = 1024;
    private static final int ITERATIONS = 1_000_000;

    @Test
    @DisplayName("Benchmark validation performance - manual execution only")
    void benchmarkValidation() {
        // Create test arrays
        double[] approx = new double[ARRAY_SIZE];
        double[] detail = new double[ARRAY_SIZE];
        
        // Fill with valid data
        for (int i = 0; i < ARRAY_SIZE; i++) {
            approx[i] = i * 0.5;
            detail[i] = i * 0.5;
        }

        // Warm up JVM
        for (int i = 0; i < 10_000; i++) {
            performValidation(approx, detail);
        }

        // Benchmark
        long startTime = System.nanoTime();
        for (int i = 0; i < ITERATIONS; i++) {
            performValidation(approx, detail);
        }
        long endTime = System.nanoTime();
        
        double elapsedMs = (endTime - startTime) / 1_000_000.0;
        double validationsPerMs = ITERATIONS / elapsedMs;
        
        System.out.printf("%n=== Validation Performance Benchmark ===%n");
        System.out.printf("Array size: %d%n", ARRAY_SIZE);
        System.out.printf("Iterations: %,d%n", ITERATIONS);
        System.out.printf("Total time: %.2f ms%n", elapsedMs);
        System.out.printf("Validations per millisecond: %,.0f%n", validationsPerMs);
        System.out.printf("Average time per validation: %.3f microseconds%n", 
                         (elapsedMs * 1000) / ITERATIONS);
    }
    
    private void performValidation(double[] approx, double[] detail) {
        // This simulates the validation pattern used in TransformResult
        // With our optimization, we avoid redundant null checks
        
        // 1. Check null/empty once for each array
        ValidationUtils.validateNotNullOrEmpty(approx, "approx");
        ValidationUtils.validateNotNullOrEmpty(detail, "detail");
        
        // 2. Check matching lengths (no redundant null checks)
        ValidationUtils.validateMatchingLengths(approx, detail);
        
        // 3. Check finite values (no redundant null/empty checks)
        ValidationUtils.validateFiniteValues(approx, "approx");
        ValidationUtils.validateFiniteValues(detail, "detail");
    }
}