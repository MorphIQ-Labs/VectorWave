package com.morphiqlabs.wavelet.api;

/**
 * Common interface for factory implementations in the VectorWave library.
 *
 * <p>This interface provides a standardized contract for all factory classes,
 * ensuring consistency across different factory implementations. It supports
 * both simple and configurable factory patterns.</p>
 *
 * <h2>Implementation Guidelines</h2>
 * <ul>
 *   <li>Use {@code create()} for simple, default instances</li>
 *   <li>Use {@code create(C config)} for configurable instances</li>
 *   <li>Provide static factory methods for common use cases</li>
 *   <li>Consider adding a Builder inner class for complex configurations</li>
 * </ul>
 *
 * <h2>Usage Examples</h2>
 * <pre>{@code
 * // Simple factory implementation
 * public class TransformFactory implements Factory<Transform, TransformConfig> {
 *     @Override
 *     public Transform create() {
 *         return new Transform(defaultConfig());
 *     }
 *     
 *     @Override
 *     public Transform create(TransformConfig config) {
 *         return new Transform(config);
 *     }
 * }
 * 
 * // Static factory implementation
 * public final class OptimizedFactory implements Factory<Processor, ProcessorConfig> {
 *     private static final OptimizedFactory INSTANCE = new OptimizedFactory();
 *     
 *     public static OptimizedFactory getInstance() {
 *         return INSTANCE;
 *     }
 *     
 *     @Override
 *     public Processor create() {
 *         return create(ProcessorConfig.defaultConfig());
 *     }
 *     
 *     @Override
 *     public Processor create(ProcessorConfig config) {
 *         // Select implementation based on config
 *         return config.isRealTime() ? 
 *             new FastProcessor(config) : 
 *             new QualityProcessor(config);
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of object created by this factory
 * @param <C> the configuration type used to create objects (use Void if not needed)
 * @since 1.0.0
 */
public interface Factory<T, C> {
    
    /**
     * Creates a new instance with default configuration.
     *
     * <p>The definition of "default configuration" is implementation-specific and should be
     * documented by each factory implementation. Common interpretations include:</p>
     * <ul>
     *   <li>Most commonly used settings for general-purpose use</li>
     *   <li>Performance-optimized settings for the current platform</li>
     *   <li>Conservative settings that work across all platforms</li>
     *   <li>Settings that match the library's historical behavior</li>
     * </ul>
     *
     * <p>Implementations should clearly document their default configuration choices
     * in their class-level documentation.</p>
     *
     * @return a new instance of type T with implementation-specific default configuration
     * @throws UnsupportedOperationException if this factory requires explicit configuration
     *         and cannot provide meaningful defaults
     */
    T create();
    
    /**
     * Creates a new instance with the specified configuration.
     *
     * @param config the configuration to use
     * @return a new instance of type T
     * @throws IllegalArgumentException if the configuration is invalid
     * @throws NullPointerException if config is null and required
     */
    T create(C config);
    
    /**
     * Validates if the given configuration is valid for this factory.
     * 
     * <p>This default implementation always returns true. Factories should
     * override this method to provide specific validation logic.</p>
     *
     * @param config the configuration to validate
     * @return true if the configuration is valid, false otherwise
     */
    default boolean isValidConfiguration(C config) {
        return true;
    }
    
    /**
     * Gets a description of what this factory creates.
     * 
     * <p>This default implementation returns the simple class name.
     * Factories should override this to provide more descriptive information.</p>
     *
     * @return a description of the factory's purpose
     */
    default String getDescription() {
        return getClass().getSimpleName();
    }
}
