# VectorWave Performance Charts

Visual representation of VectorWave performance characteristics.

## Speedup by Signal Size

```
Speedup Factor (Extensions vs Core)
5.0x │                                    ╱─────
4.5x │                              ╱─────
4.0x │                        ╱─────
3.5x │                  ╱─────
3.0x │            ╱─────
2.5x │      ╱─────
2.0x │╱─────
1.5x │
1.0x ├────┬────┬────┬────┬────┬────┬────┬────
     0    1K   4K   8K   16K  32K  64K  128K
                  Signal Size (samples)
```

## MODWT Transform Performance

```
Time (milliseconds)
10.0 │                                    ■ Core
 9.0 │                              ■─────
 8.0 │                        ■─────       □ Extensions
 7.0 │                  ■─────
 6.0 │            ■─────
 5.0 │      ■─────
 4.0 │■─────
 3.0 │                                    □
 2.0 │                              □─────
 1.0 │                        □─────
 0.0 ├────┬────┬────┬────┬────┬────┬────┬────
      1K   2K   4K   8K   16K  32K  64K  128K
                  Signal Size (samples)
```

## Batch Processing Efficiency

```
Throughput (signals/second)
1000 │                                    ▲ Batch+SIMD
 900 │                              ▲─────
 800 │                        ▲─────
 700 │                  ▲─────             ● Batch
 600 │            ▲─────                   
 500 │      ▲─────                  ●─────
 400 │▲─────                  ●─────
 300 │                  ●─────              ■ Sequential
 200 │            ●─────                    
 100 │      ●─────                    ■─────
   0 ├────┬────┬────┬────┬────┬────┬────┬────
      1    2    4    8    16   32   64   128
                  Batch Size
```

## Multi-Level Decomposition Scaling

```
Time (ms) - 5 Level Decomposition
40.0 │                                    ■
35.0 │                              ■─────  Core
30.0 │                        ■─────
25.0 │                  ■─────
20.0 │            ■─────
15.0 │      ■─────
10.0 │■─────                              □
 8.0 │                              □─────  Extensions
 6.0 │                        □─────
 4.0 │                  □─────
 2.0 │            □─────
 0.0 ├────┬────┬────┬────┬────┬────┬────┬────
      1K   2K   4K   8K   16K  32K  64K  128K
                  Signal Size (samples)
```

## Memory Usage Profile

```
Memory (MB)
32.0 │                                    ╱
28.0 │                              ╱─────
24.0 │                        ╱─────
20.0 │                  ╱─────              Linear scaling
16.0 │            ╱─────                    24 bytes/sample
12.0 │      ╱─────
 8.0 │╱─────
 4.0 │
 0.0 ├────┬────┬────┬────┬────┬────┬────┬────
      16K  32K  64K  128K 256K 512K 1M   2M
                  Signal Size (samples)
```

## Parallel Scaling (Strong)

```
Speedup Factor
16.0 │                                    ╱ Ideal
14.0 │                              ╱─────
12.0 │                        ╱─────
10.0 │                  ╱─────              ● Actual
 8.0 │            ●─────
 6.0 │      ●─────
 4.0 │●─────
 2.0 │●
 0.0 ├────┬────┬────┬────┬────┬────┬────┬────
      1    2    4    8    12   16   20   24
                  Number of Threads
```

## Wavelet Complexity Impact

```
Relative Performance (normalized to Haar)
4.0x │                                    
3.5x │                              ■ DB16
3.0x │                        ■ DB12       
2.5x │                  ■ DB8              
2.0x │            ■ DB6                    
1.5x │      ■ DB4                          
1.0x │■ DB2                                
0.5x │■ Haar (baseline)                    
0.0x ├────┬────┬────┬────┬────┬────┬────┬────
      0    2    4    6    8    10   12   14   16
                  Filter Length
```

## Platform Comparison

```
Operations/Second (millions) - 16K MODWT with GraalVM
45.0 │ ■ x86 AVX512 + GraalVM
40.0 │ ■ x86 AVX2 + GraalVM
35.0 │ ■ x86 AVX512 + OpenJDK     
32.0 │ ■ ARM SVE + GraalVM       
30.0 │ ■ x86 AVX2 + OpenJDK      
28.0 │ ■ ARM NEON + GraalVM      
25.0 │ ■ ARM SVE + OpenJDK       
22.0 │ ■ x86 SSE4 + GraalVM      
20.0 │ ■ ARM NEON + OpenJDK      
18.0 │ ■ Scalar (GraalVM)
15.0 │ ■ Scalar (OpenJDK)
10.0 │
 5.0 │
 0.0 └─────────────────────────────────────
      Platform/JVM Combination
```

## JVM Comparison

```
Relative Performance (GraalVM = 100%)
120% │                                    
110% │                              ■ OpenJDK
100% │────────────────────────■ GraalVM──
 90% │                  ■ OpenJDK         
 80% │            ■ OpenJDK               
 70% │      ■ OpenJDK                     
 60% │■ OpenJDK                           
 50% │                                    
     └────┬────┬────┬────┬────┬────┬────
      Small Medium Large Batch Stream CWT
         Workload Type
```

## CWT Performance

```
Time (ms) - 32 scales
500 │                                    ■
400 │                              ■─────  Core
300 │                        ■─────
200 │                  ■─────
150 │            ■─────                   □
100 │      ■─────                   □─────  Extensions  
 50 │■─────                   □─────
 25 │                  □─────
  0 ├────┬────┬────┬────┬────┬────┬────┬────
     1K   2K   4K   8K   16K  32K  64K  128K
                  Signal Size (samples)
```

## Denoising Performance

```
Time (ms) - Universal Threshold, 5 levels
70.0 │                                    ■
60.0 │                              ■─────  Core
50.0 │                        ■─────
40.0 │                  ■─────
30.0 │            ■─────
20.0 │      ■─────                        □
15.0 │■─────                        □─────  Extensions
10.0 │                        □─────
 5.0 │                  □─────
 0.0 ├────┬────┬────┬────┬────┬────┬────┬────
      1K   2K   4K   8K   16K  32K  64K  128K
                  Signal Size (samples)
```

## Key Observations

### 1. Scaling Characteristics
- **Sub-linear**: Performance improves with larger signals
- **Cache-friendly**: Optimal at L2/L3 cache boundaries
- **SIMD efficiency**: Better utilization with longer vectors

### 2. Bottleneck Analysis
| Signal Size | Limiting Factor | Optimization |
|------------|-----------------|--------------|
| < 1K | Fixed overhead | Batch processing |
| 1K-16K | Compute bound | SIMD acceleration |
| 16K-128K | Memory bandwidth | Cache blocking |
| > 128K | Memory latency | Streaming/chunking |

### 3. Optimal Operating Points
- **Real-time**: 4K-8K samples (< 1ms latency)
- **Batch processing**: 16K samples (best efficiency)
- **Large signals**: 64K chunks (memory/compute balance)

### 4. Platform-Specific Performance
- **x86 AVX2**: 3-4x speedup, widely available
- **x86 AVX512**: 4-5x speedup, server CPUs
- **ARM NEON**: 2-3x speedup, all ARM64
- **ARM SVE**: 3-4x speedup, newer ARM servers

## Benchmark Configuration

These charts are based on:
- **Hardware**: 24-core AMD/Intel CPU, 64GB RAM
- **Software**: GraalVM 24.0.2, Ubuntu 25.04
- **JVM**: Oracle GraalVM with Graal JIT compiler
- **Settings**: `-Xmx2g --add-modules jdk.incubator.vector -XX:+UseGraalJIT`
- **Methodology**: JMH 1.37, 5 warmup, 10 measurement iterations
- **Optimizations**: Escape analysis, loop vectorization, SIMD auto-vectorization

## Reproducing Results

Generate your own performance charts:

```bash
# Run full benchmark suite
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="com.morphiqlabs.benchmark.BenchmarkRunner"

# Generate CSV data
mvn exec:java -pl vectorwave-examples \
  -Dexec.mainClass="com.morphiqlabs.benchmark.BenchmarkRunner" \
  -Dexec.args="--format csv"

# Plot results (requires Python + matplotlib)
python3 scripts/plot-benchmarks.py benchmark-results/*.csv
```

---

*Note: ASCII charts are representative. Actual performance varies by hardware.*
