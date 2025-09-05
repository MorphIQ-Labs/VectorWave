package com.morphiqlabs.benchmark;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.results.format.ResultFormatType;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Main runner for VectorWave benchmark suite.
 * 
 * <p>Provides convenient execution of all benchmarks with appropriate configuration.</p>
 * 
 * <h2>Usage</h2>
 * <pre>
 * // Run all benchmarks
 * mvn exec:java -Dexec.mainClass="com.morphiqlabs.benchmark.BenchmarkRunner"
 * 
 * // Run specific benchmark pattern
 * mvn exec:java -Dexec.mainClass="com.morphiqlabs.benchmark.BenchmarkRunner" -Dexec.args="MODWT"
 * 
 * // Quick mode (fewer iterations)
 * mvn exec:java -Dexec.mainClass="com.morphiqlabs.benchmark.BenchmarkRunner" -Dexec.args="--quick"
 * </pre>
 */
public class BenchmarkRunner {
    
    public static void main(String[] args) throws RunnerException {
        String pattern = ".*";
        boolean quickMode = false;
        boolean csv = false;
        
        // Parse arguments
        if (args.length > 0) {
            if ("--quick".equals(args[0])) {
                quickMode = true;
            } else if ("--csv".equals(args[0])) {
                csv = true;
            } else if ("--help".equals(args[0])) {
                printHelp();
                return;
            } else {
                pattern = ".*" + args[0] + ".*";
            }
        }
        
        // Create results directory
        File resultsDir = new File("benchmark-results");
        if (!resultsDir.exists()) {
            resultsDir.mkdirs();
        }
        
        // Generate timestamp for result files
        String timestamp = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date());
        String resultFile = String.format("benchmark-results/vectorwave-benchmarks-%s", timestamp);
        
        System.out.println("==========================================");
        System.out.println("    VectorWave Benchmark Suite v2.0.0    ");
        System.out.println("==========================================");
        System.out.println();
        System.out.println("Configuration:");
        System.out.println("  Pattern: " + pattern);
        System.out.println("  Mode: " + (quickMode ? "Quick" : "Full"));
        System.out.println("  Results: " + resultFile + (csv ? ".csv" : ".json"));
        System.out.println();
        
        // Build options
        OptionsBuilder optBuilder = new OptionsBuilder();
        optBuilder.include(pattern)
            .shouldFailOnError(true)
            .shouldDoGC(true)
            .jvmArgs(
                "--add-modules", "jdk.incubator.vector",
                "-XX:+UseG1GC",
                "-Xmx2g",
                "-Xms2g"
            );
        
        if (quickMode) {
            // Quick mode for development/testing
            optBuilder.warmupIterations(1)
                .warmupTime(TimeValue.seconds(1))
                .measurementIterations(3)
                .measurementTime(TimeValue.seconds(2))
                .forks(1);
        } else {
            // Full mode for accurate results
            optBuilder.warmupIterations(5)
                .warmupTime(TimeValue.seconds(3))
                .measurementIterations(10)
                .measurementTime(TimeValue.seconds(5))
                .forks(2);
        }
        
        // Add result files (note: JMH only supports one result file per run)
        if (csv) {
            optBuilder.result(resultFile + ".csv").resultFormat(ResultFormatType.CSV);
        } else {
            optBuilder.result(resultFile + ".json").resultFormat(ResultFormatType.JSON);
        }
        
        Options opt = optBuilder.build();
        
        // Run benchmarks
        System.out.println("Starting benchmarks...");
        System.out.println();
        
        long startTime = System.currentTimeMillis();
        new Runner(opt).run();
        long endTime = System.currentTimeMillis();
        
        // Print summary
        System.out.println();
        System.out.println("==========================================");
        System.out.println("         Benchmark Suite Complete         ");
        System.out.println("==========================================");
        System.out.println("Total time: " + formatDuration(endTime - startTime));
        System.out.println("Results saved to:");
        System.out.println("  - " + resultFile + (csv ? ".csv" : ".json"));
        System.out.println();
        System.out.println("View results with:");
        System.out.println("  python3 scripts/analyze-benchmarks.py " + resultFile + (csv ? ".csv" : ".json"));
    }
    
    private static void printHelp() {
        System.out.println("VectorWave Benchmark Runner");
        System.out.println();
        System.out.println("Usage: java -jar benchmarks.jar [options] [pattern]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --quick    Run in quick mode (fewer iterations)");
        System.out.println("  --csv      Output results in CSV format");
        System.out.println("  --help     Show this help message");
        System.out.println();
        System.out.println("Patterns:");
        System.out.println("  MODWT      Run MODWT benchmarks only");
        System.out.println("  Cache      Run multi-level filter cache benchmarks");
        System.out.println("  SIMDMulti  Run SIMD batch multi-level benchmarks");
        System.out.println("  Core       Run core vs extensions benchmarks");
        System.out.println("  Memory     Run memory efficiency benchmarks");
        System.out.println("  Conv       Run FFT vs scalar convolution benchmarks");
        System.out.println("  <empty>    Run all benchmarks");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar benchmarks.jar");
        System.out.println("  java -jar benchmarks.jar --quick");
        System.out.println("  java -jar benchmarks.jar MODWT");
        System.out.println("  java -jar benchmarks.jar --quick Core");
    }
    
    private static String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        
        if (hours > 0) {
            return String.format("%d hours %d minutes", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%d minutes %d seconds", minutes, seconds % 60);
        } else {
            return String.format("%d seconds", seconds);
        }
    }
}

