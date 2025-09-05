package com.morphiqlabs.wavelet.util;

/**
 * Example demonstrating how to use PlatformDetection utility to replace
 * scattered platform detection code throughout the codebase.
 * 
 * <p>This class shows before/after examples of platform detection patterns
 * and how they can be refactored using the centralized utility.</p>
 * 
 * <h2>⚠️ IMPORTANT WARNING ⚠️</h2>
 * <p>This file contains ANTI-PATTERNS for educational purposes only!</p>
 * <p>The {@link OldPatterns} class shows BAD CODE that should NEVER be copied.</p>
 * <p>Always use the patterns shown in {@link NewPatterns} instead.</p>
 */
public final class PlatformDetectionExample {
    
    /**
     * Example of OLD platform detection pattern (scattered throughout codebase).
     * This is the type of code that should be replaced.
     * 
     * ⚠️ WARNING: ANTI-PATTERNS - DO NOT COPY THIS CODE! ⚠️
     * 
     * These examples demonstrate BAD PRACTICES that should NEVER be used:
     * - Direct system property access (hard to test)
     * - Scattered, duplicated platform detection logic
     * - Error-prone string comparisons
     * - Difficult to maintain across the codebase
     * 
     * ❌ DO NOT USE THESE PATTERNS IN PRODUCTION CODE ❌
     * 
     * These are shown ONLY for educational purposes to demonstrate what
     * to avoid. Always use PlatformDetector instead (see NewPatterns below).
     * 
     * Even though these examples include null-safety fixes, they still
     * represent poor design that should be refactored.
     */
    public static class OldPatterns {
        
        /**
         * Example of hardcoded Apple Silicon detection.
         * This pattern was found in the CWT implementation code.
         * 
         * ⚠️ ANTI-PATTERN: DO NOT COPY THIS CODE! ⚠️
         * Problems with this approach:
         * - Originally had NullPointerException risk
         * - Duplicated logic across codebase
         * - Hard to mock for testing
         * - Platform detection logic scattered
         * 
         * ✅ USE INSTEAD: PlatformDetector.isAppleSilicon()
         */
        public boolean isAppleSiliconOldWay() {
            // ❌ BAD: Direct system property access
            // Even with null-safety fixes, this is still an anti-pattern!
            String arch = System.getProperty("os.arch");
            String osName = System.getProperty("os.name");
            return arch != null && arch.equals("aarch64") && 
                   osName != null && osName.toLowerCase().contains("mac");
        }
        
        /**
         * Example of platform-specific SIMD threshold selection.
         * 
         * ⚠️ ANTI-PATTERN: DO NOT COPY THIS CODE! ⚠️
         * Problems:
         * - Platform detection duplicated from above method
         * - Magic numbers (8, 16) with no explanation
         * - Hard to update when adding new platforms
         * 
         * ✅ USE INSTEAD: PlatformDetector.getRecommendedSIMDThreshold()
         */
        public int getSIMDThresholdOldWay() {
            // ❌ BAD: Duplicated platform detection logic
            // ❌ BAD: Hard-coded thresholds scattered in code
            String arch = System.getProperty("os.arch");
            String osName = System.getProperty("os.name");
            boolean isAppleSilicon = arch != null && arch.equals("aarch64") && 
                                   osName != null && osName.toLowerCase().contains("mac");
            return isAppleSilicon ? 8 : 16;
        }
        
        /**
         * Example of AVX capability detection.
         * 
         * ⚠️ ANTI-PATTERN: DO NOT COPY THIS CODE! ⚠️
         * Problems:
         * - Incomplete detection (doesn't check CPU capabilities)
         * - Assumes all x86_64 CPUs have AVX (false!)
         * - No way to override for testing
         * 
         * ✅ USE INSTEAD: PlatformDetector.hasAVX2Support()
         */
        public boolean hasAVXSupportOldWay() {
            // ❌ BAD: Overly simplistic platform detection
            String arch = System.getProperty("os.arch");
            // This is already null-safe due to short-circuit evaluation
            return arch != null && (arch.equals("x86_64") || arch.equals("amd64"));
        }
    }
    
    /**
     * Example of NEW platform detection pattern using the utility.
     * 
     * ✅ RECOMMENDED PATTERNS - USE THESE! ✅
     * 
     * These examples show the correct way to handle platform detection:
     * - Centralized detection logic
     * - Easy to test (can override with system properties)
     * - Consistent across the codebase
     * - Maintainable and extensible
     */
    public static class NewPatterns {
        
        /**
         * Example of centralized Apple Silicon detection.
         */
        public boolean isAppleSiliconNewWay() {
            // NEW: Clean, testable, centralized detection
            return PlatformDetector.isAppleSilicon();
        }
        
        /**
         * Example of platform-specific SIMD threshold selection.
         */
        public int getSIMDThresholdNewWay() {
            // NEW: Single method call with platform-optimized defaults
            return PlatformDetector.getRecommendedSIMDThreshold();
        }
        
        /**
         * Example of AVX capability detection.
         */
        public boolean hasAVX2SupportNewWay() {
            // NEW: Accurate, heuristic-based detection
            return PlatformDetector.hasAVX2Support();
        }
        
        /**
         * Example of comprehensive platform information.
         */
        public String getPlatformInfoNewWay() {
            // NEW: Rich platform information for debugging/optimization
            return PlatformDetector.getPlatformOptimizationHints();
        }
    }
    
    /**
     * Example of how the utility enables better testing.
     */
    public static class TestableCode {
        
        /**
         * Method that uses platform detection and can be easily tested.
         */
        public String selectOptimizationStrategy() {
            if (PlatformDetector.isAppleSilicon()) {
                return "Apple Silicon NEON optimizations";
            } else if (PlatformDetector.hasAVX512Support()) {
                return "AVX-512 optimizations"; 
            } else if (PlatformDetector.hasAVX2Support()) {
                return "AVX2 optimizations";
            } else {
                return "Scalar optimizations";
            }
        }
        
        /**
         * Configuration method that adapts to platform characteristics.
         */
        public OptimizationConfig createOptimizationConfig() {
            return new OptimizationConfig(
                PlatformDetector.getRecommendedSIMDThreshold(),
                PlatformDetector.hasAVX2Support(),
                PlatformDetector.isAppleSilicon()
            );
        }
    }
    
    /**
     * Example configuration class that uses platform detection.
     */
    public static class OptimizationConfig {
        private final int simdThreshold;
        private final boolean vectorizationEnabled;
        private final boolean appleOptimizations;
        
        public OptimizationConfig(int simdThreshold, boolean vectorizationEnabled, 
                                boolean appleOptimizations) {
            this.simdThreshold = simdThreshold;
            this.vectorizationEnabled = vectorizationEnabled;
            this.appleOptimizations = appleOptimizations;
        }
        
        public int getSIMDThreshold() { return simdThreshold; }
        public boolean isVectorizationEnabled() { return vectorizationEnabled; }
        public boolean hasAppleOptimizations() { return appleOptimizations; }
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private PlatformDetectionExample() {
        throw new AssertionError("Example class should not be instantiated");
    }
}