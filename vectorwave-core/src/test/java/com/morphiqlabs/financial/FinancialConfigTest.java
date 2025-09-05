package com.morphiqlabs.financial;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test coverage for FinancialConfig class.
 */
public class FinancialConfigTest {
    
    @Test
    public void testConstructor_ValidRiskFreeRate() {
        double riskFreeRate = 0.045;
        FinancialConfig config = new FinancialConfig(riskFreeRate);
        
        assertEquals(riskFreeRate, config.getRiskFreeRate(), 1e-10);
    }
    
    @Test
    public void testConstructor_ZeroRiskFreeRate() {
        double riskFreeRate = 0.0;
        FinancialConfig config = new FinancialConfig(riskFreeRate);
        
        assertEquals(riskFreeRate, config.getRiskFreeRate(), 1e-10);
    }
    
    @Test
    public void testConstructor_HighRiskFreeRate() {
        double riskFreeRate = 0.15; // 15% - very high but valid
        FinancialConfig config = new FinancialConfig(riskFreeRate);
        
        assertEquals(riskFreeRate, config.getRiskFreeRate(), 1e-10);
    }
    
    @Test
    public void testConstructor_NegativeRiskFreeRate_ThrowsException() {
        double riskFreeRate = -0.01;
        
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new FinancialConfig(riskFreeRate)
        );
        
        assertTrue(exception.getMessage().contains("Risk-free rate cannot be negative"));
        assertTrue(exception.getMessage().contains("-0.01"));
    }
    
    @Test
    public void testToString() {
        FinancialConfig config = new FinancialConfig(0.0375);
        String str = config.toString();
        
        assertTrue(str.contains("FinancialConfig"));
        assertTrue(str.contains("riskFreeRate"));
        assertTrue(str.contains("0.0375"));
    }
    
    @Test
    public void testEquals_SameObject() {
        FinancialConfig config = new FinancialConfig(0.05);
        
        assertTrue(config.equals(config));
    }
    
    @Test
    public void testEquals_EqualConfigs() {
        FinancialConfig config1 = new FinancialConfig(0.045);
        FinancialConfig config2 = new FinancialConfig(0.045);
        
        assertTrue(config1.equals(config2));
        assertTrue(config2.equals(config1));
    }
    
    @Test
    public void testEquals_DifferentRates() {
        FinancialConfig config1 = new FinancialConfig(0.045);
        FinancialConfig config2 = new FinancialConfig(0.05);
        
        assertFalse(config1.equals(config2));
        assertFalse(config2.equals(config1));
    }
    
    @Test
    public void testEquals_NullObject() {
        FinancialConfig config = new FinancialConfig(0.045);
        
        assertFalse(config.equals(null));
    }
    
    @Test
    public void testEquals_DifferentClass() {
        FinancialConfig config = new FinancialConfig(0.045);
        String notAConfig = "not a config";
        
        assertFalse(config.equals(notAConfig));
    }
    
    @Test
    public void testHashCode_EqualConfigs() {
        FinancialConfig config1 = new FinancialConfig(0.045);
        FinancialConfig config2 = new FinancialConfig(0.045);
        
        assertEquals(config1.hashCode(), config2.hashCode());
    }
    
    @Test
    public void testHashCode_DifferentConfigs() {
        FinancialConfig config1 = new FinancialConfig(0.045);
        FinancialConfig config2 = new FinancialConfig(0.05);
        
        // Different rates should generally produce different hash codes
        // (though not guaranteed by contract)
        assertNotEquals(config1.hashCode(), config2.hashCode());
    }
    
    @Test
    public void testHashCode_ConsistentWithEquals() {
        // Test multiple values to ensure hashCode is consistent with equals
        double[] rates = {0.0, 0.01, 0.025, 0.045, 0.05, 0.10, 0.15};
        
        for (double rate : rates) {
            FinancialConfig config1 = new FinancialConfig(rate);
            FinancialConfig config2 = new FinancialConfig(rate);
            
            // If equal, must have same hash code
            if (config1.equals(config2)) {
                assertEquals(config1.hashCode(), config2.hashCode());
            }
        }
    }
    
    @Test
    public void testImmutability() {
        double originalRate = 0.045;
        FinancialConfig config = new FinancialConfig(originalRate);
        
        // Get rate multiple times to ensure it doesn't change
        assertEquals(originalRate, config.getRiskFreeRate(), 1e-10);
        assertEquals(originalRate, config.getRiskFreeRate(), 1e-10);
        assertEquals(originalRate, config.getRiskFreeRate(), 1e-10);
    }
    
    @Test
    public void testBoundaryValues() {
        // Test very small positive rate
        FinancialConfig config1 = new FinancialConfig(Double.MIN_VALUE);
        assertEquals(Double.MIN_VALUE, config1.getRiskFreeRate(), 0.0);
        
        // Test large rate (unrealistic but valid)
        FinancialConfig config2 = new FinancialConfig(100.0);
        assertEquals(100.0, config2.getRiskFreeRate(), 1e-10);
        
        // Test typical values
        FinancialConfig config3 = new FinancialConfig(0.0001); // 0.01%
        assertEquals(0.0001, config3.getRiskFreeRate(), 1e-10);
        
        FinancialConfig config4 = new FinancialConfig(0.9999); // 99.99%
        assertEquals(0.9999, config4.getRiskFreeRate(), 1e-10);
    }
}