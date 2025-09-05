package com.morphiqlabs.wavelet.api.spi;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.morphiqlabs.wavelet.util.Logging;

/**
 * Registry for discovering and managing wavelet transform optimizers via SPI.
 * This class provides fallback to scalar implementations when optimizations are not available.
 *
 * @since 1.0.0
 */
public class OptimizerRegistry {
    private static final System.Logger LOG = Logging.getLogger(OptimizerRegistry.class);
    private static final OptimizerRegistry INSTANCE = new OptimizerRegistry();
    
    private final Map<Class<?>, Object> optimizerCache = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;
    
    private OptimizerRegistry() {
        initialize();
    }
    
    /**
     * Returns the singleton registry instance.
     * @return global {@code OptimizerRegistry}
     */
    public static OptimizerRegistry getInstance() {
        return INSTANCE;
    }
    
    private synchronized void initialize() {
        if (initialized) {
            return;
        }
        
        LOG.log(System.Logger.Level.INFO, "Initializing optimizer registry...");
        
        // Discover MODWT optimizers
        discoverOptimizers(MODWTOptimizer.class);
        
        // Discover FFT optimizers
        discoverOptimizers(FFTOptimizer.class);
        
        // Discover Convolution optimizers
        discoverOptimizers(ConvolutionOptimizer.class);
        
        initialized = true;
        LOG.log(System.Logger.Level.INFO, () -> "Optimizer registry initialized with " + optimizerCache.size() + " optimizers");
    }
    
    private <T> void discoverOptimizers(Class<T> optimizerClass) {
        ServiceLoader<T> loader = ServiceLoader.load(optimizerClass);
        List<T> candidates = new ArrayList<>();
        
        for (T optimizer : loader) {
            try {
                if (isSupported(optimizer)) {
                    candidates.add(optimizer);
                    LOG.log(System.Logger.Level.INFO, () -> "Found " + optimizerClass.getSimpleName() + ": " +
                              getName(optimizer) + " (priority: " + getPriority(optimizer) + ")");
                }
            } catch (Exception e) {
                LOG.log(System.Logger.Level.WARNING, "Error checking optimizer support: " + optimizer, e);
            }
        }
        
        if (!candidates.isEmpty()) {
            // Sort by priority (highest first)
            candidates.sort((a, b) -> Integer.compare(getPriority(b), getPriority(a)));
            T best = candidates.get(0);
            optimizerCache.put(optimizerClass, best);
            LOG.log(System.Logger.Level.INFO, () -> "Selected " + optimizerClass.getSimpleName() + ": " + getName(best));
        } else {
            LOG.log(System.Logger.Level.INFO, () -> "No " + optimizerClass.getSimpleName() + " optimizers found, will use scalar fallback");
        }
    }
    
    private boolean isSupported(Object optimizer) {
        try {
            return (boolean) optimizer.getClass().getMethod("isSupported").invoke(optimizer);
        } catch (Exception e) {
            return false;
        }
    }
    
    private int getPriority(Object optimizer) {
        try {
            return (int) optimizer.getClass().getMethod("getPriority").invoke(optimizer);
        } catch (Exception e) {
            return 0;
        }
    }
    
    private String getName(Object optimizer) {
        try {
            return (String) optimizer.getClass().getMethod("getName").invoke(optimizer);
        } catch (Exception e) {
            return optimizer.getClass().getSimpleName();
        }
    }
    
    /**
     * Get the best available MODWT optimizer, or null if none available.
     * @return MODWT optimizer or null
     */
    public Optional<MODWTOptimizer> getMODWTOptimizer() {
        return Optional.ofNullable((MODWTOptimizer) optimizerCache.get(MODWTOptimizer.class));
    }
    
    /**
     * Get the best available FFT optimizer, or null if none available.
     * @return FFT optimizer or null
     */
    public Optional<FFTOptimizer> getFFTOptimizer() {
        return Optional.ofNullable((FFTOptimizer) optimizerCache.get(FFTOptimizer.class));
    }
    
    /**
     * Get the best available convolution optimizer, or null if none available.
     * @return convolution optimizer or null
     */
    public Optional<ConvolutionOptimizer> getConvolutionOptimizer() {
        return Optional.ofNullable((ConvolutionOptimizer) optimizerCache.get(ConvolutionOptimizer.class));
    }
    
    /**
     * Check if any optimizations are available.
     * @return true if at least one optimizer is available
     */
    public boolean hasOptimizations() {
        return !optimizerCache.isEmpty();
    }
    
    /**
     * Get information about available optimizers.
     * @return map of optimizer types to their names
     */
    public Map<String, String> getAvailableOptimizers() {
        Map<String, String> result = new HashMap<>();
        optimizerCache.forEach((clazz, optimizer) -> {
            result.put(clazz.getSimpleName(), getName(optimizer));
        });
        return result;
    }
}
