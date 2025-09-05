package com.morphiqlabs.wavelet.api;

/**
 * Simplified factory interface for implementations that don't require configuration.
 *
 * <p>This interface extends {@link Factory} with {@code Void} as the configuration type,
 * providing a cleaner API for factories that only need simple creation methods.</p>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * public class DefaultWaveletFactory implements SimpleFactory<Wavelet> {
 *     @Override
 *     public Wavelet create() {
 *         return new Haar(); // Return default wavelet
 *     }
 * }
 * }</pre>
 *
 * @param <T> the type of object created by this factory
 * @since 1.0.0
 */
public interface SimpleFactory<T> extends Factory<T, Void> {
    
    /**
     * Creates a new instance with default settings.
     *
     * @return a new instance of type T
     */
    @Override
    T create();
    
    /**
     * This method is not supported for SimpleFactory.
     * Use {@link #create()} instead.
     *
     * @param config ignored
     * @return never returns
     * @throws UnsupportedOperationException always
     */
    @Override
    default T create(Void config) {
        throw new UnsupportedOperationException(
            "SimpleFactory does not support configuration. Use create() instead.");
    }
    
    /**
     * Configuration validation is not applicable for SimpleFactory.
     *
     * @param config ignored
     * @return always returns true
     */
    @Override
    default boolean isValidConfiguration(Void config) {
        return true;
    }
}
