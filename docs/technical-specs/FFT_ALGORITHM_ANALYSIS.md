# FFT Algorithm Analysis and Consolidation Plan

## Current Implementation

The OptimizedFFT class currently has 4 different FFT implementations:

### 1. Split-radix FFT
- **Used for**: Power-of-2 sizes where n >= 32
- **Advantages**: ~25% fewer operations than radix-2
- **Disadvantages**: More complex, higher overhead for small sizes

### 2. Vectorized Radix-2 FFT  
- **Used for**: Power-of-2 sizes where n < 32 and Vector API is available
- **Advantages**: SIMD acceleration
- **Disadvantages**: Requires Vector API

### 3. Scalar Radix-2 FFT
- **Used for**: Power-of-2 sizes where n < 32 and Vector API is not available
- **Advantages**: Simple, portable
- **Disadvantages**: No SIMD acceleration

### 4. Bluestein FFT
- **Used for**: Non-power-of-2 sizes
- **Advantages**: Works for any size
- **Disadvantages**: Higher computational cost

## Consolidation Options

### Option 1: Keep All (Current)
- **Pros**: Maximum performance for each case
- **Cons**: Complex code, maintenance burden, testing complexity

### Option 2: Unify Radix-2 and Split-radix
- Use split-radix for all power-of-2 sizes
- **Pros**: Simpler code, still good performance
- **Cons**: Slightly slower for small sizes (n < 32)

### Option 3: Use Only Radix-2 for Power-of-2
- Remove split-radix entirely
- **Pros**: Much simpler code
- **Cons**: ~25% performance loss for large sizes

### Option 4: Smart Consolidation (Recommended)
- Keep Bluestein for non-power-of-2 (required)
- Merge radix-2 implementations but keep vector optimization
- Remove split-radix if benchmarks show minimal benefit

## Recommendation

Based on the principle of simplicity while maintaining reasonable performance:

1. **Keep Bluestein** - Required for non-power-of-2 support
2. **Unify radix-2 implementations** - One implementation with optional vectorization
3. **Remove split-radix** - Unless benchmarks show significant (>2x) speedup

This would reduce from 4 implementations to 2, making the code much more maintainable while keeping the essential functionality.