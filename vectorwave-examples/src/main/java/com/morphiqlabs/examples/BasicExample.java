package com.morphiqlabs.examples;

import com.morphiqlabs.wavelet.api.WaveletTransformOptimizer;
import java.util.ServiceLoader;

/**
 * Basic example demonstrating optimizer discovery via SPI.
 */
public class BasicExample {
    public BasicExample() {}
    
    public static void main(String[] args) {
        System.out.println("=== VectorWave Module Example ===");
        System.out.println("Discovering available optimizers...");
        
        ServiceLoader<WaveletTransformOptimizer> optimizers = 
            ServiceLoader.load(WaveletTransformOptimizer.class);
        
        int count = 0;
        for (WaveletTransformOptimizer optimizer : optimizers) {
            if (optimizer.isSupported()) {
                System.out.printf("  Found: %s (priority: %d, type: %s)%n",
                    optimizer.getName(),
                    optimizer.getPriority(),
                    optimizer.getType());
                count++;
            }
        }
        
        if (count == 0) {
            System.out.println("  No optimizers found (running with core module only)");
        }
        
        System.out.println("\nModule structure working correctly!");
    }
}
