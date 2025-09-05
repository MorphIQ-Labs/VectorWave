package com.morphiqlabs.wavelet.util;

/**
 * Utility class for detecting platform characteristics.
 * 
 * <p>Provides centralized platform detection logic to enable better testing
 * and maintainability across the wavelet transform library.</p>
 */
public final class PlatformDetector {
    
    /**
     * Platform types supported by the library.
     */
    public enum Platform {
        /** Apple Silicon (M1, M2, etc.) */
        APPLE_SILICON,
        /** x86-64 (Intel/AMD) */
        X86_64,
        /** ARM (non-Apple) */
        ARM,
        /** Unknown platform */
        UNKNOWN
    }
    
    /**
     * Operating system types.
     */
    public enum OperatingSystem {
        /** macOS */
        MACOS,
        /** Microsoft Windows */
        WINDOWS,
        /** Linux distributions */
        LINUX,
        /** Other/unknown OS */
        OTHER
    }
    
    /**
     * Cache configuration for the platform.
     *
     * @param l1DataCacheSize L1 data cache size in bytes
     * @param l2CacheSize L2 cache size in bytes
     * @param cacheLineSize Cache line size in bytes
     */
    public record CacheInfo(
        int l1DataCacheSize,
        int l2CacheSize,
        int cacheLineSize
    ) {
        public static final CacheInfo APPLE_SILICON = new CacheInfo(128 * 1024, 4 * 1024 * 1024, 64);
        public static final CacheInfo X86_64 = new CacheInfo(32 * 1024, 256 * 1024, 64);
        public static final CacheInfo ARM_DEFAULT = new CacheInfo(64 * 1024, 1024 * 1024, 64);
        public static final CacheInfo DEFAULT = new CacheInfo(32 * 1024, 256 * 1024, 64);
    }
    
    private static final Platform CURRENT_PLATFORM = detectPlatform();
    private static final OperatingSystem CURRENT_OS = detectOperatingSystem();
    private static final CacheInfo CACHE_INFO = detectCacheInfo();
    
    // Prevent instantiation
    private PlatformDetector() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Gets the detected platform type.
     * 
     * @return the current platform
     */
    public static Platform getPlatform() {
        return CURRENT_PLATFORM;
    }
    
    /**
     * Gets the cache configuration for the current platform.
     * 
     * @return cache information
     */
    public static CacheInfo getCacheInfo() {
        return CACHE_INFO;
    }
    
    /**
     * Checks if the current platform is Apple Silicon.
     * 
     * @return true if running on Apple Silicon
     */
    public static boolean isAppleSilicon() {
        return CURRENT_PLATFORM == Platform.APPLE_SILICON;
    }
    
    /**
     * Checks if the current platform is x86-64.
     * 
     * @return true if running on x86-64
     */
    public static boolean isX86_64() {
        return CURRENT_PLATFORM == Platform.X86_64;
    }
    
    /**
     * Checks if the current platform is ARM-based.
     * 
     * @return true if running on any ARM platform (including Apple Silicon)
     */
    public static boolean isARM() {
        return CURRENT_PLATFORM == Platform.APPLE_SILICON || CURRENT_PLATFORM == Platform.ARM;
    }
    
    /**
     * Detects the current platform.
     * 
     * <p>This method can be overridden for testing by setting system properties:
     * <ul>
     *   <li>{@code com.morphiqlabs.test.platform} - Override platform detection</li>
     *   <li>{@code com.morphiqlabs.test.arch} - Override architecture detection</li>
     *   <li>{@code com.morphiqlabs.test.os} - Override OS detection</li>
     * </ul>
     * 
     * @return the detected platform
     */
    private static Platform detectPlatform() {
        // Check for test overrides first
        String testPlatform = System.getProperty("com.morphiqlabs.test.platform");
        if (testPlatform != null) {
            try {
                return Platform.valueOf(testPlatform.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // Fall through to normal detection
            }
        }
        
        // Get architecture and OS information
        String arch = System.getProperty("com.morphiqlabs.test.arch", 
                                       System.getProperty("os.arch", "")).toLowerCase();
        String osName = System.getProperty("com.morphiqlabs.test.os",
                                         System.getProperty("os.name", "")).toLowerCase();
        
        // Detect platform based on architecture and OS
        if (arch.contains("aarch64") || arch.contains("arm64")) {
            if (osName.contains("mac") || osName.contains("darwin")) {
                return Platform.APPLE_SILICON;
            } else {
                return Platform.ARM;
            }
        } else if (arch.contains("amd64") || arch.contains("x86_64")) {
            return Platform.X86_64;
        } else if (arch.contains("arm")) {
            return Platform.ARM;
        }
        
        return Platform.UNKNOWN;
    }
    
    /**
     * Detects cache configuration for the current platform.
     * 
     * <p>Cache sizes can be overridden using system properties:
     * <ul>
     *   <li>{@code com.morphiqlabs.cache.l1.size} - L1 data cache size in bytes</li>
     *   <li>{@code com.morphiqlabs.cache.l2.size} - L2 cache size in bytes</li>
     *   <li>{@code com.morphiqlabs.cache.line.size} - Cache line size in bytes</li>
     * </ul>
     * 
     * @return cache configuration
     */
    private static CacheInfo detectCacheInfo() {
        // Check for property overrides
        Integer l1Size = getSystemPropertyInt("com.morphiqlabs.cache.l1.size");
        Integer l2Size = getSystemPropertyInt("com.morphiqlabs.cache.l2.size");
        Integer lineSize = getSystemPropertyInt("com.morphiqlabs.cache.line.size");
        
        // If all properties are set, use them
        if (l1Size != null && l2Size != null && lineSize != null) {
            return new CacheInfo(l1Size, l2Size, lineSize);
        }
        
        // Otherwise, use platform defaults with property overrides
        CacheInfo platformDefault = switch (CURRENT_PLATFORM) {
            case APPLE_SILICON -> CacheInfo.APPLE_SILICON;
            case X86_64 -> CacheInfo.X86_64;
            case ARM -> CacheInfo.ARM_DEFAULT;
            case UNKNOWN -> CacheInfo.DEFAULT;
        };
        
        return new CacheInfo(
            l1Size != null ? l1Size : platformDefault.l1DataCacheSize,
            l2Size != null ? l2Size : platformDefault.l2CacheSize,
            lineSize != null ? lineSize : platformDefault.cacheLineSize
        );
    }
    
    /**
     * Gets an integer system property value.
     * 
     * @param key the property key
     * @return the integer value, or null if not set or invalid
     */
    private static Integer getSystemPropertyInt(String key) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
                // Return null for invalid values
            }
        }
        return null;
    }
    
    /**
     * Gets the detected operating system.
     * 
     * @return the current operating system
     */
    public static OperatingSystem getOperatingSystem() {
        return CURRENT_OS;
    }
    
    /**
     * Detects the current operating system.
     * 
     * @return the detected operating system
     */
    private static OperatingSystem detectOperatingSystem() {
        String osName = System.getProperty("com.morphiqlabs.test.os",
                                         System.getProperty("os.name", "")).toLowerCase();
        
        if (osName.contains("mac") || osName.contains("darwin")) {
            return OperatingSystem.MACOS;
        } else if (osName.contains("win")) {
            return OperatingSystem.WINDOWS;
        } else if (osName.contains("nux") || osName.contains("nix") || osName.contains("aix")) {
            return OperatingSystem.LINUX;
        }
        
        return OperatingSystem.OTHER;
    }
    
    /**
     * Checks if the platform likely supports AVX2 instructions.
     * This is a heuristic based on platform type.
     * 
     * @return true if AVX2 is likely supported
     */
    public static boolean hasAVX2Support() {
        return switch (CURRENT_PLATFORM) {
            case X86_64 -> true; // Most modern x86-64 CPUs have AVX2
            case APPLE_SILICON -> true; // Apple Silicon has NEON which is comparable
            default -> false;
        };
    }
    
    /**
     * Checks if the platform likely supports AVX-512 instructions.
     * This is a heuristic based on platform type.
     * 
     * @return true if AVX-512 is likely supported
     */
    public static boolean hasAVX512Support() {
        // AVX-512 is only on some x86-64 processors, not on ARM
        return CURRENT_PLATFORM == Platform.X86_64;
    }
    
    /**
     * Gets the recommended SIMD threshold for the current platform.
     * 
     * @return recommended minimum array size for SIMD operations
     */
    public static int getRecommendedSIMDThreshold() {
        return switch (CURRENT_PLATFORM) {
            case APPLE_SILICON -> 8; // Apple Silicon benefits from SIMD with smaller arrays
            case ARM -> 8; // ARM NEON also efficient with smaller arrays
            case X86_64 -> 16; // x86-64 needs larger arrays to amortize overhead
            case UNKNOWN -> 32; // Conservative default
        };
    }
    
    /**
     * Gets platform-specific optimization hints.
     * 
     * @return optimization hints as a formatted string
     */
    public static String getPlatformOptimizationHints() {
        StringBuilder hints = new StringBuilder();
        hints.append("Platform: ").append(CURRENT_PLATFORM).append("\n");
        hints.append("OS: ").append(CURRENT_OS).append("\n");
        hints.append("SIMD Threshold: ").append(getRecommendedSIMDThreshold()).append(" elements\n");
        
        if (CURRENT_PLATFORM == Platform.APPLE_SILICON) {
            hints.append("Optimizations: NEON SIMD, unified memory architecture\n");
            hints.append("Recommendations: Use smaller block sizes, leverage cache-friendly access patterns\n");
        } else if (CURRENT_PLATFORM == Platform.X86_64) {
            hints.append("Optimizations: ");
            if (hasAVX512Support()) hints.append("AVX-512 ");
            if (hasAVX2Support()) hints.append("AVX2 ");
            hints.append("SSE\n");
            hints.append("Recommendations: Use larger block sizes for SIMD, consider NUMA effects\n");
        }
        
        hints.append("Cache: L1=").append(CACHE_INFO.l1DataCacheSize / 1024).append("KB, ");
        hints.append("L2=").append(CACHE_INFO.l2CacheSize / 1024).append("KB, ");
        hints.append("Line=").append(CACHE_INFO.cacheLineSize).append("B");
        
        return hints.toString();
    }
    
    /**
     * Gets a human-readable description of the current platform.
     * 
     * @return platform description
     */
    public static String getDescription() {
        return String.format("Platform: %s, OS: %s, L1 Cache: %d KB, L2 Cache: %d MB, Cache Line: %d bytes",
            CURRENT_PLATFORM,
            CURRENT_OS,
            CACHE_INFO.l1DataCacheSize / 1024,
            CACHE_INFO.l2CacheSize / (1024 * 1024),
            CACHE_INFO.cacheLineSize
        );
    }
}
