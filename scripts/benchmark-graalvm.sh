#!/bin/bash

# VectorWave Benchmark Runner for GraalVM
# Requires GraalVM 24.0.2 or later

set -e

# Color output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=========================================="
echo "   VectorWave GraalVM Benchmark Suite    "
echo "=========================================="
echo ""

# Check if GRAALVM_HOME is set
if [ -z "$GRAALVM_HOME" ]; then
    echo -e "${YELLOW}Warning: GRAALVM_HOME not set${NC}"
    echo "Looking for GraalVM in standard locations..."
    
    # Try to find GraalVM
    if [ -d "/usr/lib/jvm/graalvm-24" ]; then
        export GRAALVM_HOME="/usr/lib/jvm/graalvm-24"
    elif [ -d "/opt/graalvm-24" ]; then
        export GRAALVM_HOME="/opt/graalvm-24"
    elif [ -d "$HOME/.sdkman/candidates/java/24.0.2-graal" ]; then
        export GRAALVM_HOME="$HOME/.sdkman/candidates/java/24.0.2-graal"
    else
        echo -e "${RED}Error: GraalVM not found${NC}"
        echo "Please install GraalVM 24.0.2 and set GRAALVM_HOME"
        echo ""
        echo "Installation options:"
        echo "1. Using SDKMAN: sdk install java 24.0.2-graal"
        echo "2. Download from: https://www.graalvm.org/downloads/"
        echo "3. Set GRAALVM_HOME=/path/to/graalvm"
        exit 1
    fi
fi

JAVA_CMD="$GRAALVM_HOME/bin/java"

# Verify GraalVM version
echo "Checking GraalVM version..."
$JAVA_CMD -version 2>&1 | head -3
echo ""

# Check if it's actually GraalVM
if ! $JAVA_CMD -version 2>&1 | grep -q "GraalVM"; then
    echo -e "${YELLOW}Warning: This may not be GraalVM${NC}"
    echo "For best performance, use Oracle GraalVM 24.0.2"
fi

# Parse command line arguments
MODE="full"
BENCHMARK="all"
EXEC_ARGS=""
PGO_ENABLED=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --quick)
            MODE="quick"
            shift
            ;;
        --pgo)
            PGO_ENABLED=true
            shift
            ;;
        --benchmark)
            BENCHMARK="$2"
            shift 2
            ;;
        --help)
            echo "Usage: $0 [options]"
            echo ""
            echo "Options:"
            echo "  --quick           Run quick benchmark (fewer iterations)"
            echo "  --pgo            Enable Profile-Guided Optimization"
            echo "  --benchmark NAME  Run specific benchmark (MODWT, Core, Memory, etc.)"
            echo "  --help           Show this help message"
            echo ""
            echo "Environment:"
            echo "  GRAALVM_HOME     Path to GraalVM installation"
            echo ""
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

# Set up JVM flags
JVM_FLAGS="-Xmx2g -Xms2g"
JVM_FLAGS="$JVM_FLAGS --add-modules jdk.incubator.vector"
JVM_FLAGS="$JVM_FLAGS --enable-preview"

# GraalVM-specific optimizations
GRAAL_FLAGS="-XX:+UseGraalJIT"
GRAAL_FLAGS="$GRAAL_FLAGS -XX:+EscapeAnalysis"
GRAAL_FLAGS="$GRAAL_FLAGS -XX:+PartialEscapeAnalysis"
GRAAL_FLAGS="$GRAAL_FLAGS -Dgraal.OptDuplication=true"
GRAAL_FLAGS="$GRAAL_FLAGS -Dgraal.LoopPeeling=true"
GRAAL_FLAGS="$GRAAL_FLAGS -Dgraal.VectorizeLoops=true"
GRAAL_FLAGS="$GRAAL_FLAGS -Dgraal.OptimizeLoopAccesses=true"
GRAAL_FLAGS="$GRAAL_FLAGS -Dgraal.AlignCallTargetInstructions=true"

# Profile-Guided Optimization
if [ "$PGO_ENABLED" = true ]; then
    echo -e "${GREEN}Profile-Guided Optimization enabled${NC}"
    GRAAL_FLAGS="$GRAAL_FLAGS -Dgraal.ProfileCompiledMethods=true"
    GRAAL_FLAGS="$GRAAL_FLAGS -Dgraal.UseProfileInformation=true"
    
    # First pass: collect profile
    echo "Phase 1: Collecting profile data..."
    GRAAL_FLAGS="$GRAAL_FLAGS -XX:ProfiledCodeHeapSize=256M"
fi

# Combine all flags
ALL_FLAGS="$JVM_FLAGS $GRAAL_FLAGS"

echo "Configuration:"
echo "  Mode: $MODE"
echo "  Benchmark: $BENCHMARK"
echo "  PGO: $PGO_ENABLED"
echo "  JVM Flags: $ALL_FLAGS"
echo ""

# Change to project directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
cd "$PROJECT_DIR"

# Build project if needed
if [ ! -d "target" ]; then
    echo "Building project..."
    mvn clean compile -DskipTests -q
fi

# Create results directory
RESULTS_DIR="benchmark-results-graalvm"
mkdir -p "$RESULTS_DIR"

# Generate timestamp
TIMESTAMP=$(date +"%Y%m%d-%H%M%S")
RESULT_FILE="$RESULTS_DIR/benchmark-$TIMESTAMP"

echo "Starting benchmarks..."
echo "Results will be saved to: $RESULT_FILE"
echo ""

# Select benchmark class based on mode and type
if [ "$MODE" = "quick" ]; then
    # Quick benchmark
    echo "Running quick benchmark..."
    $JAVA_CMD $ALL_FLAGS \
        -cp "vectorwave-benchmarks/target/classes:vectorwave-core/target/classes:vectorwave-extensions/target/classes" \
        com.morphiqlabs.benchmark.QuickBenchmark \
        > "$RESULT_FILE.txt"
else
    # Full JMH benchmark
    MAIN_CLASS="com.morphiqlabs.benchmark.BenchmarkRunner"
    
    case $BENCHMARK in
        MODWT)
            MAIN_CLASS="com.morphiqlabs.benchmark.MODWTBenchmark"
            ;;
        Core)
            MAIN_CLASS="com.morphiqlabs.benchmark.CoreVsExtensionsBenchmark"
            ;;
        Memory)
            MAIN_CLASS="com.morphiqlabs.benchmark.MemoryEfficiencyBenchmark"
            ;;
        SIMDMulti)
            MAIN_CLASS="com.morphiqlabs.benchmark.MultiLevelBatchSIMDBenchmark"
            ;;
        Cache)
            MAIN_CLASS="com.morphiqlabs.benchmark.BenchmarkRunner"
            EXEC_ARGS="Cache"
            ;;
        Conv)
            MAIN_CLASS="com.morphiqlabs.benchmark.FftConvolutionBenchmark"
            ;;
    esac
    
    echo "Running JMH benchmark: $MAIN_CLASS"
    
    # Run with Maven exec plugin for proper classpath
    mvn -q -pl vectorwave-benchmarks -am exec:java \
        -Dexec.mainClass="$MAIN_CLASS" \
        -Dexec.classpathScope="compile" \
        -Dexec.args="$EXEC_ARGS" \
        -Dexec.commandLineArgs="$ALL_FLAGS" \
        2>&1 | tee "$RESULT_FILE.log"
fi

# If PGO is enabled, run second pass
if [ "$PGO_ENABLED" = true ] && [ "$MODE" != "quick" ]; then
    echo ""
    echo "Phase 2: Running with profile-guided optimization..."
    
    # Second pass with profile data
    mvn -q -pl vectorwave-benchmarks -am exec:java \
        -Dexec.mainClass="$MAIN_CLASS" \
        -Dexec.classpathScope="compile" \
        -Dexec.args="" \
        -Dexec.commandLineArgs="$ALL_FLAGS -XX:+UseProfileInformation" \
        2>&1 | tee "$RESULT_FILE-pgo.log"
fi

echo ""
echo -e "${GREEN}Benchmark complete!${NC}"
echo ""
echo "Results saved to:"
echo "  $RESULT_FILE.txt (quick mode)"
echo "  $RESULT_FILE.log (full mode)"
if [ "$PGO_ENABLED" = true ]; then
    echo "  $RESULT_FILE-pgo.log (PGO results)"
fi

# Display summary
echo ""
echo "Performance Summary:"
if [ "$MODE" = "quick" ]; then
    # Extract key metrics from quick benchmark
    grep -E "MODWT Forward|Speedup|Memory Used" "$RESULT_FILE.txt" | head -10
else
    # Extract JMH results
    grep -E "Benchmark|Score|Error" "$RESULT_FILE.log" | tail -20
fi

echo ""
echo "To analyze results in detail:"
echo "  cat $RESULT_FILE.txt"
echo "  less $RESULT_FILE.log"

# Compare with OpenJDK if available
if command -v java &> /dev/null; then
    OPENJDK_VERSION=$(java -version 2>&1 | head -1)
    echo ""
    echo "For comparison, system OpenJDK version:"
    echo "  $OPENJDK_VERSION"
    echo ""
    echo "Run with OpenJDK for comparison:"
    echo "  java $JVM_FLAGS -cp ... com.morphiqlabs.benchmark.QuickBenchmark"
fi
