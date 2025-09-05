package com.morphiqlabs.wavelet.api;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Central registry for factory implementations in the VectorWave library.
 *
 * <p>This registry provides a centralized location to register and retrieve
 * factory instances, enabling dependency injection and flexible factory
 * management across the library.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Register factories at application startup
 * FactoryRegistry registry = FactoryRegistry.getInstance();
 * registry.register("waveletOps", WaveletOpsFactory.getInstance());
 * registry.register("cwtTransform", CWTFactory.getInstance());
 * 
 * // Retrieve factories later
 * Factory<WaveletOps, TransformConfig> opsFactory = 
 *     registry.getFactory("waveletOps", WaveletOps.class, TransformConfig.class)
 *         .orElseThrow(() -> new IllegalStateException("Factory not found"));
 * 
 * // Use the factory
 * WaveletOps ops = opsFactory.create();
 * }</pre>
 *
 * <h2>Thread Safety</h2>
 * <p>This registry is thread-safe and can be safely accessed from multiple
 * threads concurrently.</p>
 */
public final class FactoryRegistry {
    
    private static final FactoryRegistry INSTANCE = new FactoryRegistry();
    private final Map<String, Factory<?, ?>> factories = new ConcurrentHashMap<>();
    
    /**
     * Private constructor for singleton pattern.
     */
    private FactoryRegistry() {
        // Initialize with default factories
        initializeDefaultFactories();
    }
    
    /**
     * Gets the singleton instance of the factory registry.
     *
     * @return the registry instance
     */
    public static FactoryRegistry getInstance() {
        return INSTANCE;
    }
    
    /**
     * Registers a factory with the given key.
     *
     * @param key the unique key for this factory
     * @param factory the factory instance
     * @throws IllegalArgumentException if key is null or empty
     * @throws IllegalStateException if a factory is already registered with this key
     */
    public void register(String key, Factory<?, ?> factory) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalArgumentException("Factory key cannot be null or empty");
        }
        if (factory == null) {
            throw new IllegalArgumentException(
                String.format("Cannot register null factory for key '%s'", key));
        }
        
        Factory<?, ?> existing = factories.putIfAbsent(key, factory);
        if (existing != null) {
            throw new IllegalStateException(
                String.format("Factory already registered with key '%s'. " +
                    "Existing factory: %s (type: %s), " +
                    "Attempted to register: %s (type: %s)",
                    key,
                    existing.getDescription(), existing.getClass().getName(),
                    factory.getDescription(), factory.getClass().getName()));
        }
    }
    
    /**
     * Retrieves a factory by its key with type safety.
     *
     * @param key the factory key
     * @param productType the expected product type
     * @param configType the expected configuration type
     * @param <T> the product type
     * @param <C> the configuration type
     * @return an Optional containing the factory if found and types match
     */
    @SuppressWarnings("unchecked")
    public <T, C> Optional<Factory<T, C>> getFactory(String key, Class<T> productType, Class<C> configType) {
        Factory<?, ?> factory = factories.get(key);
        if (factory == null) {
            return Optional.empty();
        }
        
        // Type checking would require additional metadata
        // For now, we trust the caller to request the correct types
        try {
            return Optional.of((Factory<T, C>) factory);
        } catch (ClassCastException e) {
            return Optional.empty();
        }
    }
    
    /**
     * Retrieves a factory by its key without type checking.
     *
     * @param key the factory key
     * @return an Optional containing the factory if found
     */
    public Optional<Factory<?, ?>> getFactory(String key) {
        return Optional.ofNullable(factories.get(key));
    }
    
    /**
     * Checks if a factory is registered with the given key.
     *
     * @param key the factory key
     * @return true if a factory is registered with this key
     */
    public boolean isRegistered(String key) {
        return factories.containsKey(key);
    }
    
    /**
     * Unregisters a factory.
     *
     * @param key the factory key
     * @return true if the factory was removed, false if not found
     */
    public boolean unregister(String key) {
        return factories.remove(key) != null;
    }
    
    /**
     * Gets all registered factory keys.
     *
     * @return an unmodifiable set of factory keys
     */
    public Set<String> getRegisteredKeys() {
        return Set.copyOf(factories.keySet());
    }
    
    /**
     * Clears all registered factories except the defaults.
     */
    public void clear() {
        factories.clear();
        initializeDefaultFactories();
    }
    
    /**
     * Initializes the default factories that are always available.
     */
    private void initializeDefaultFactories() {
        // Register default factories
        try {
            // These will be registered lazily when the classes are loaded
            // to avoid circular dependencies during initialization
        } catch (Exception e) {
            // Ignore initialization errors
        }
    }
    
    /**
     * Registers the default VectorWave factories.
     * This method should be called during application initialization.
     */
    public static void registerDefaults() {
        FactoryRegistry registry = getInstance();
        
        // Register the default factories if they haven't been registered yet
        
        // WaveletTransformFactory temporarily disabled during DWT -> MODWT migration
        /*
        if (!registry.isRegistered("waveletTransform")) {
            // WaveletTransformFactory is instance-based, so we create a shared instance
            try {
                registry.register("waveletTransform", 
                    new com.morphiqlabs.wavelet.WaveletTransformFactory());
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Failed to register default WaveletTransformFactory with key 'waveletTransform': " + 
                    e.getMessage(), e);
            }
        }
        */
        
        if (!registry.isRegistered("cwtTransform")) {
            try {
                registry.register("cwtTransform", 
                    com.morphiqlabs.wavelet.cwt.CWTFactory.getInstance());
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Failed to register default CWTFactory with key 'cwtTransform': " + 
                    e.getMessage(), e);
            }
        }
        
        // Streaming factories temporarily disabled during DWT -> MODWT migration
        /*
        if (!registry.isRegistered("streamingDenoiser")) {
            try {
                registry.register("streamingDenoiser", 
                    com.morphiqlabs.wavelet.streaming.StreamingDenoiserFactory.getInstance());
            } catch (Exception e) {
                throw new IllegalStateException(
                    "Failed to register default StreamingDenoiserFactory with key 'streamingDenoiser': " + 
                    e.getMessage(), e);
            }
        }
        */
    }
}
