package com.morphiqlabs.wavelet.api;

import com.morphiqlabs.wavelet.util.NullChecks;

/**
 * Abstract base class for static factory implementations.
 *
 * <p>This class provides a foundation for implementing the Factory pattern
 * with static methods. It includes common validation and error handling logic
 * that can be reused across different factory implementations.</p>
 *
 * <h2>Implementation Pattern</h2>
 * <pre>{@code
 * public final class MyStaticFactory extends AbstractStaticFactory<MyType, MyConfig> {
 *     
 *     private static final MyStaticFactory INSTANCE = new MyStaticFactory();
 *     
 *     private MyStaticFactory() {
 *         // Private constructor for singleton
 *     }
 *     
 *     public static MyType create() {
 *         return INSTANCE.createInstance();
 *     }
 *     
 *     public static MyType create(MyConfig config) {
 *         return INSTANCE.createInstance(config);
 *     }
 *     
 *     @Override
 *     protected MyType doCreate() {
 *         return new MyType(getDefaultConfiguration());
 *     }
 *     
 *     @Override
 *     protected MyType doCreate(MyConfig config) {
 *         // Implementation-specific creation logic
 *         return new MyType(config);
 *     }
 *     
 *     @Override
 *     protected MyConfig getDefaultConfiguration() {
 *         return MyConfig.defaultConfig();
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of object created by this factory
 * @param <C> the configuration type used to create objects
 */
public abstract class AbstractStaticFactory<T, C> implements Factory<T, C> {

    /**
     * Default constructor for subclasses.
     */
    protected AbstractStaticFactory() {
        // For subclasses
    }
    
    
    /**
     * Creates a new instance with default configuration.
     * 
     * <p>This method delegates to {@link #doCreate()} for the actual
     * instance creation.</p>
     *
     * @return a new instance of type T
     */
    @Override
    public final T create() {
        return doCreate();
    }
    
    /**
     * Creates a new instance with the specified configuration.
     * 
     * <p>This method performs null checking and validation before
     * delegating to {@link #doCreate(Object)} for instance creation.</p>
     *
     * @param config the configuration to use
     * @return a new instance of type T
     * @throws IllegalArgumentException if the configuration is invalid
     * @throws NullPointerException if config is null
     */
    @Override
    public final T create(C config) {
        NullChecks.requireNonNull(config, "config");
        
        if (!isValidConfiguration(config)) {
            throw new IllegalArgumentException(
                "Invalid configuration for " + getDescription());
        }
        
        return doCreate(config);
    }
    
    /**
     * Protected method for creating an instance without exposing it publicly.
     * This allows static factory methods to use the instance methods internally.
     *
     * @return a new instance created with default configuration
     */
    protected final T createInstance() {
        return create();
    }
    
    /**
     * Protected method for creating an instance without exposing it publicly.
     * This allows static factory methods to use the instance methods internally.
     *
     * @param config the configuration to use
     * @return a new instance created with the given configuration
     */
    protected final T createInstance(C config) {
        return create(config);
    }
    
    /**
     * Creates a new instance with default configuration.
     * 
     * <p>Subclasses must implement this method to provide the actual
     * instance creation logic.</p>
     *
     * @return a new instance of type T
     */
    protected abstract T doCreate();
    
    /**
     * Creates a new instance with the specified configuration.
     * 
     * <p>Subclasses must implement this method to provide the actual
     * instance creation logic. The configuration is guaranteed to be
     * non-null and valid when this method is called.</p>
     *
     * @param config the validated configuration
     * @return a new instance of type T
     */
    protected abstract T doCreate(C config);
    
    /**
     * Gets the default configuration for this factory.
     * 
     * <p>Subclasses should override this method if they support
     * creating instances with default configuration.</p>
     *
     * @return the default configuration
     * @throws UnsupportedOperationException if default configuration is not supported
     */
    protected C getDefaultConfiguration() {
        throw new UnsupportedOperationException(
            getDescription() + " does not support default configuration");
    }
}
