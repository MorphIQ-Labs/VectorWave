package com.morphiqlabs.wavelet.api;

import java.util.Set;
import java.util.HashSet;
import java.util.Collections;

/**
 * Metadata information about a wavelet.
 * Provides comprehensive information about a wavelet's properties,
 * family, aliases, and characteristics.
 * 
 * <p>This class is immutable and thread-safe.</p>
 * 
 * <p><strong>Usage:</strong> Use the Builder pattern to create instances:
 * <pre>{@code
 * WaveletInfo info = new WaveletInfo.Builder("db4", WaveletType.ORTHOGONAL)
 *     .displayName("Daubechies 4")
 *     .family("Daubechies")
 *     .order(4)
 *     .build();
 * }</pre>
 */
public final class WaveletInfo {
    private final String name;
    private final String displayName;
    private final WaveletType type;
    private final String family;
    private final int order;
    private final Set<String> aliases;
    private final String description;
    private final int vanishingMoments;
    private final int filterLength;
    
    /**
     * Creates a new WaveletInfo instance.
     * Package-private constructor to enforce use of Builder pattern for public API.
     * 
     * @param name The canonical name of the wavelet (e.g., "db4")
     * @param displayName The display name (e.g., "Daubechies 4")
     * @param type The wavelet type
     * @param family The wavelet family (e.g., "Daubechies")
     * @param order The order of the wavelet (if applicable)
     * @param aliases Alternative names for this wavelet
     * @param description A brief description of the wavelet
     * @param vanishingMoments Number of vanishing moments (for discrete wavelets)
     * @param filterLength Length of the wavelet filter (for discrete wavelets)
     */
    WaveletInfo(String name, String displayName, WaveletType type, 
                String family, int order, Set<String> aliases,
                String description, int vanishingMoments, int filterLength) {
        this.name = name;
        this.displayName = displayName;
        this.type = type;
        this.family = family;
        this.order = order;
        // Make a defensive copy of the aliases set to ensure immutability
        this.aliases = aliases == null ? Collections.emptySet() : 
                       Collections.unmodifiableSet(new HashSet<>(aliases));
        this.description = description;
        this.vanishingMoments = vanishingMoments;
        this.filterLength = filterLength;
    }
    
    /**
     * Gets the canonical name of the wavelet.
     * This is the primary name used with WaveletRegistry.getWavelet().
     * 
     * @return The wavelet name (e.g., "db4")
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the display name suitable for UI presentation.
     * 
     * @return The display name (e.g., "Daubechies 4")
     */
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Gets the wavelet type.
     * 
     * @return The wavelet type (ORTHOGONAL, BIORTHOGONAL, or CONTINUOUS)
     */
    public WaveletType getType() {
        return type;
    }
    
    /**
     * Gets the wavelet family name.
     * 
     * @return The family name (e.g., "Daubechies", "Symlet", "Coiflet")
     */
    public String getFamily() {
        return family;
    }
    
    /**
     * Gets the order of the wavelet.
     * For wavelets without a specific order, returns 0.
     * 
     * @return The wavelet order
     */
    public int getOrder() {
        return order;
    }
    
    /**
     * Gets alternative names (aliases) for this wavelet.
     * 
     * @return Immutable set of aliases (may be empty)
     */
    public Set<String> getAliases() {
        return aliases;
    }
    
    /**
     * Gets a brief description of the wavelet.
     * 
     * @return The wavelet description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the number of vanishing moments.
     * Only applicable for discrete wavelets (ORTHOGONAL and BIORTHOGONAL).
     * 
     * @return Number of vanishing moments, or 0 for continuous wavelets
     */
    public int getVanishingMoments() {
        return vanishingMoments;
    }
    
    /**
     * Gets the filter length.
     * Only applicable for discrete wavelets (ORTHOGONAL and BIORTHOGONAL).
     * 
     * @return Filter length, or 0 for continuous wavelets
     */
    public int getFilterLength() {
        return filterLength;
    }
    
    /**
     * Builder for creating WaveletInfo instances.
     */
    public static class Builder {
        private String name;
        private String displayName;
        private WaveletType type;
        private String family;
        private int order = 0;
        private Set<String> aliases = Collections.emptySet();
        private String description = "";
        private int vanishingMoments = 0;
        private int filterLength = 0;
        
        /**
         * Creates a new Builder.
         *
         * @param name the canonical name of the wavelet
         * @param type the wavelet type
         */
        public Builder(String name, WaveletType type) {
            this.name = name;
            this.type = type;
            this.displayName = name; // Default to name
        }
        
        /**
         * Sets the display name.
         *
         * @param displayName the display name
         * @return this builder
         */
        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        /**
         * Sets the wavelet family.
         *
         * @param family the wavelet family
         * @return this builder
         */
        public Builder family(String family) {
            this.family = family;
            return this;
        }
        
        /**
         * Sets the wavelet order.
         *
         * @param order the wavelet order
         * @return this builder
         */
        public Builder order(int order) {
            this.order = order;
            return this;
        }
        
        /**
         * Sets the aliases for the wavelet.
         *
         * @param aliases the aliases
         * @return this builder
         */
        public Builder aliases(Set<String> aliases) {
            this.aliases = aliases;
            return this;
        }
        
        /**
         * Sets the description of the wavelet.
         *
         * @param description the description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets the number of vanishing moments.
         *
         * @param vanishingMoments the number of vanishing moments
         * @return this builder
         */
        public Builder vanishingMoments(int vanishingMoments) {
            this.vanishingMoments = vanishingMoments;
            return this;
        }
        
        /**
         * Sets the filter length.
         *
         * @param filterLength the filter length
         * @return this builder
         */
        public Builder filterLength(int filterLength) {
            this.filterLength = filterLength;
            return this;
        }
        
        /**
         * Builds the WaveletInfo object.
         *
         * @return the new WaveletInfo object
         */
        public WaveletInfo build() {
            return new WaveletInfo(name, displayName, type, family, order,
                                 aliases, description, vanishingMoments, filterLength);
        }
    }
    
    @Override
    public String toString() {
        return String.format("WaveletInfo{name='%s', displayName='%s', type=%s, family='%s', order=%d}",
                           name, displayName, type, family, order);
    }
}