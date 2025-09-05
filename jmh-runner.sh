#!/bin/bash

# JMH Benchmark Runner for VectorWave Parallel Performance
# ========================================================

echo "VectorWave Parallel Performance Benchmarks"
echo "=========================================="
echo "Java Version: $(java -version 2>&1 | head -n 1)"
echo "Cores Available: $(nproc 2>/dev/null || sysctl -n hw.ncpu)"
echo "Date: $(date)"
echo ""

# Build and run JMH on the classpath from the non-modular benchmarks module
echo "Building benchmarks module and running JMH..."
JMH_ARGS=${JMH_ARGS:-"-rf json -rff jmh-results.json"}
mvn -q -pl vectorwave-benchmarks -am clean compile exec:java \
    -Dexec.mainClass="org.openjdk.jmh.Main" \
    -Dexec.classpathScope="compile" \
    -Dexec.jvmArgs="--add-modules jdk.incubator.vector --enable-preview" \
    -Dexec.args="$JMH_ARGS"

echo ""
echo "Benchmark completed. Results saved to jmh-results.json"
echo "To view results in a readable format:"
echo "cat jmh-results.json | jq '.[] | {benchmark: .benchmark, mode: .mode, score: .primaryMetric.score, unit: .primaryMetric.scoreUnit}'"
