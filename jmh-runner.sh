#!/bin/bash

# JMH Benchmark Runner for VectorWave Parallel Performance
# ========================================================

echo "VectorWave Parallel Performance Benchmarks"
echo "=========================================="
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Cores Available: $(nproc 2>/dev/null || sysctl -n hw.ncpu)"
echo "Date: $(date)"
echo ""

# Compile the project and generate JMH benchmark JAR
echo "Building project and generating benchmark JAR..."
mvn clean compile test-compile exec:java -Dexec.mainClass="org.openjdk.jmh.Main" \
    -Dexec.args="-rf json -rff jmh-results.json com.morphiqlabs.wavelet.benchmark.ParallelVsSequentialBenchmark" \
    -Dexec.classpathScope="test" \
    -Dexec.jvmArgs="--add-modules jdk.incubator.vector"

echo ""
echo "Benchmark completed. Results saved to jmh-results.json"
echo "To view results in a readable format:"
echo "cat jmh-results.json | jq '.[] | {benchmark: .benchmark, mode: .mode, score: .primaryMetric.score, unit: .primaryMetric.scoreUnit}'"