package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.api.*;
import com.morphiqlabs.wavelet.padding.*;import com.morphiqlabs.wavelet.config.TransformConfig;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;

import java.util.Objects;

/**
 * Factory for creating MODWT (Maximal Overlap Discrete Wavelet Transform) instances.
 * 
 * <p>This factory provides convenient methods for creating MODWT transforms with
 * various configurations. It implements the standard Factory interface for consistency
 * with other wavelet transform factories.</p>
 * 
 * <p><strong>Key features:</strong></p>
 * <ul>
 *   <li>Simple creation methods for common use cases</li>
 *   <li>Support for all wavelet types</li>
 *   <li>Integration with WaveletRegistry for name-based lookup</li>
 *   <li>Multi-level MODWT support</li>
 * </ul>
 * 
 * <p><strong>Usage examples:</strong></p>
 * <pre>{@code
 * // Create single-level MODWT with Haar wavelet
 * MODWTTransform modwt = MODWTTransformFactory.create(new Haar());
 * 
 * // Create with specific boundary mode
 * MODWTTransform modwt = MODWTTransformFactory.create(
 *     Daubechies.DB4, BoundaryMode.PERIODIC);
 * 
 * // Create from wavelet name
 * MODWTTransform modwt = MODWTTransformFactory.create(WaveletName.DB4);
 * 
 * // Create multi-level MODWT
 * MultiLevelMODWTTransform mlModwt = MODWTTransformFactory.createMultiLevel(
 *     new Haar(), BoundaryMode.PERIODIC);
 * 
 * // Register with FactoryRegistry for global access
 * FactoryRegistry.getInstance().register(
 *     "modwt", new MODWTTransformFactory());
 * }</pre>
 * 
 * @since 1.0.0
 */
public class MODWTTransformFactory implements Factory<MODWTTransform, MODWTTransformFactory.Config> {

    /**
     * Private constructor to enforce singleton pattern.
     */
    private MODWTTransformFactory() {
        // Enforce singleton
    }
    
    
    /**
     * Creates a MODWT transform with default configuration (Haar wavelet, PERIODIC boundary).
     * 
     * @return A new MODWT transform instance with default settings
     */
    @Override
    public MODWTTransform create() {
        return create(new Haar());
    }
    
    /**
     * Configuration for MODWT transform creation.
     */
    public static class Config {
        private final Wavelet wavelet;
        private final BoundaryMode boundaryMode;
        
        /**
         * Creates a configuration with specified wavelet and boundary mode.
         * 
         * @param wavelet The wavelet to use
         * @param boundaryMode The boundary mode
         */
        public Config(Wavelet wavelet, BoundaryMode boundaryMode) {
            this.wavelet = Objects.requireNonNull(wavelet, "wavelet cannot be null");
            this.boundaryMode = Objects.requireNonNull(boundaryMode, "boundaryMode cannot be null");
        }
        
        /**
         * Creates a configuration with specified wavelet and default PERIODIC boundary mode.
         * 
         * @param wavelet The wavelet to use
         */
        public Config(Wavelet wavelet) {
            this(wavelet, BoundaryMode.PERIODIC);
        }
        
        /**
         * Gets the wavelet configured for this factory config.
         * @return the wavelet
         */
        public Wavelet getWavelet() {
            return wavelet;
        }
        
        /**
         * Gets the boundary mode configured for this factory config.
         * @return the boundary mode
         */
        public BoundaryMode getBoundaryMode() {
            return boundaryMode;
        }
    }
    
    /**
     * Creates a MODWT transform with the given configuration.
     * 
     * @param config The configuration
     * @return A new MODWT transform instance
     * @throws NullPointerException if config is null
     * @throws IllegalArgumentException if boundary mode is not supported
     */
    @Override
    public MODWTTransform create(Config config) {
        Objects.requireNonNull(config, "config cannot be null");
        return new MODWTTransform(config.getWavelet(), config.getBoundaryMode());
    }
    
    /**
     * Creates a MODWT transform with the specified wavelet and PERIODIC boundary mode.
     * 
     * @param wavelet The wavelet to use
     * @return A new MODWT transform instance
     * @throws NullPointerException if wavelet is null
     */
    public static MODWTTransform create(Wavelet wavelet) {
        return new MODWTTransform(wavelet, BoundaryMode.PERIODIC);
    }
    
    /**
     * Creates a MODWT transform with the specified wavelet and boundary mode.
     * 
     * @param wavelet The wavelet to use
     * @param boundaryMode The boundary mode
     * @return A new MODWT transform instance
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if boundary mode is not supported
     */
    public static MODWTTransform create(Wavelet wavelet, BoundaryMode boundaryMode) {
        return new MODWTTransform(wavelet, boundaryMode);
    }
    
    /**
     * Creates a MODWT transform using a wavelet name from the registry.
     * 
     * @param waveletName The wavelet name enum
     * @return A new MODWT transform instance
     * @throws InvalidArgumentException if wavelet name is not found
     */
    public static MODWTTransform create(WaveletName waveletName) {
        Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
        return create(wavelet);
    }
    
    /**
     * Creates a MODWT transform using a wavelet name and boundary mode.
     * 
     * @param waveletName The wavelet name enum
     * @param boundaryMode The boundary mode
     * @return A new MODWT transform instance
     * @throws InvalidArgumentException if wavelet name is not found
     * @throws IllegalArgumentException if boundary mode is not supported
     */
    public static MODWTTransform create(WaveletName waveletName, BoundaryMode boundaryMode) {
        Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
        return create(wavelet, boundaryMode);
    }
    
    /**
     * Creates a multi-level MODWT transform with the specified wavelet.
     * 
     * @param wavelet The wavelet to use
     * @return A new multi-level MODWT transform instance
     * @throws NullPointerException if wavelet is null
     */
    public static MultiLevelMODWTTransform createMultiLevel(Wavelet wavelet) {
        return new MultiLevelMODWTTransform(wavelet, BoundaryMode.PERIODIC);
    }
    
    /**
     * Creates a multi-level MODWT transform with the specified wavelet and boundary mode.
     * 
     * @param wavelet The wavelet to use
     * @param boundaryMode The boundary mode
     * @return A new multi-level MODWT transform instance
     * @throws NullPointerException if any parameter is null
     * @throws IllegalArgumentException if boundary mode is not supported
     */
    public static MultiLevelMODWTTransform createMultiLevel(Wavelet wavelet, BoundaryMode boundaryMode) {
        return new MultiLevelMODWTTransform(wavelet, boundaryMode);
    }
    
    /**
     * Creates a multi-level MODWT transform using a wavelet name.
     * 
     * @param waveletName The wavelet name enum
     * @return A new multi-level MODWT transform instance
     * @throws InvalidArgumentException if wavelet name is not found
     */
    public static MultiLevelMODWTTransform createMultiLevel(WaveletName waveletName) {
        Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
        return createMultiLevel(wavelet);
    }
    
    /**
     * Creates a multi-level MODWT transform using a wavelet name and boundary mode.
     * 
     * @param waveletName The wavelet name enum
     * @param boundaryMode The boundary mode
     * @return A new multi-level MODWT transform instance
     * @throws InvalidArgumentException if wavelet name is not found
     * @throws IllegalArgumentException if boundary mode is not supported
     */
    public static MultiLevelMODWTTransform createMultiLevel(WaveletName waveletName, BoundaryMode boundaryMode) {
        Wavelet wavelet = WaveletRegistry.getWavelet(waveletName);
        return createMultiLevel(wavelet, boundaryMode);
    }
    
    /**
     * Gets a singleton instance of this factory.
     * 
     * @return The factory instance
     */
    public static MODWTTransformFactory getInstance() {
        return InstanceHolder.INSTANCE;
    }
    
    // Lazy initialization holder
    private static class InstanceHolder {
        private static final MODWTTransformFactory INSTANCE = new MODWTTransformFactory();
    }
}
