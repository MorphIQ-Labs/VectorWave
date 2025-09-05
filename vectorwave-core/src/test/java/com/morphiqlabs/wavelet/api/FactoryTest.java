package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for Factory interface and its default methods.
 */
public class FactoryTest {
    
    // Test implementation of Factory
    static class TestProduct {
        private final String value;
        private final int number;
        
        TestProduct(String value, int number) {
            this.value = value;
            this.number = number;
        }
        
        String getValue() { return value; }
        int getNumber() { return number; }
    }
    
    static class TestConfig {
        private final String configValue;
        private final int configNumber;
        
        TestConfig(String configValue, int configNumber) {
            this.configValue = configValue;
            this.configNumber = configNumber;
        }
        
        String getConfigValue() { return configValue; }
        int getConfigNumber() { return configNumber; }
    }
    
    static class TestFactory implements Factory<TestProduct, TestConfig> {
        private boolean validateConfig = true;
        private String description = "Test Factory";
        
        @Override
        public TestProduct create() {
            return new TestProduct("default", 42);
        }
        
        @Override
        public TestProduct create(TestConfig config) {
            if (config == null) {
                throw new NullPointerException("Config cannot be null");
            }
            return new TestProduct(config.getConfigValue(), config.getConfigNumber());
        }
        
        @Override
        public boolean isValidConfiguration(TestConfig config) {
            if (!validateConfig) {
                return true;
            }
            return config != null && 
                   config.getConfigValue() != null && 
                   config.getConfigNumber() >= 0;
        }
        
        @Override
        public String getDescription() {
            return description;
        }
        
        void setValidateConfig(boolean validateConfig) {
            this.validateConfig = validateConfig;
        }
        
        void setDescription(String description) {
            this.description = description;
        }
    }
    
    @Test
    @DisplayName("Test Factory.create() with default configuration")
    void testCreateWithDefaultConfiguration() {
        TestFactory factory = new TestFactory();
        
        TestProduct product = factory.create();
        assertNotNull(product);
        assertEquals("default", product.getValue());
        assertEquals(42, product.getNumber());
    }
    
    @Test
    @DisplayName("Test Factory.create(config) with valid configuration")
    void testCreateWithValidConfiguration() {
        TestFactory factory = new TestFactory();
        TestConfig config = new TestConfig("custom", 100);
        
        TestProduct product = factory.create(config);
        assertNotNull(product);
        assertEquals("custom", product.getValue());
        assertEquals(100, product.getNumber());
    }
    
    @Test
    @DisplayName("Test Factory.create(config) with null configuration")
    void testCreateWithNullConfiguration() {
        TestFactory factory = new TestFactory();
        
        assertThrows(NullPointerException.class, () -> {
            factory.create(null);
        });
    }
    
    @Test
    @DisplayName("Test Factory.isValidConfiguration() with valid config")
    void testIsValidConfigurationWithValidConfig() {
        TestFactory factory = new TestFactory();
        TestConfig validConfig = new TestConfig("valid", 10);
        
        assertTrue(factory.isValidConfiguration(validConfig));
    }
    
    @Test
    @DisplayName("Test Factory.isValidConfiguration() with invalid configs")
    void testIsValidConfigurationWithInvalidConfigs() {
        TestFactory factory = new TestFactory();
        
        // Null config
        assertFalse(factory.isValidConfiguration(null));
        
        // Config with null value
        TestConfig invalidConfig1 = new TestConfig(null, 10);
        assertFalse(factory.isValidConfiguration(invalidConfig1));
        
        // Config with negative number
        TestConfig invalidConfig2 = new TestConfig("valid", -1);
        assertFalse(factory.isValidConfiguration(invalidConfig2));
    }
    
    @Test
    @DisplayName("Test Factory.isValidConfiguration() when validation is disabled")
    void testIsValidConfigurationWhenDisabled() {
        TestFactory factory = new TestFactory();
        factory.setValidateConfig(false);
        
        // Should return true even for invalid configs
        assertTrue(factory.isValidConfiguration(null));
        assertTrue(factory.isValidConfiguration(new TestConfig(null, -1)));
    }
    
    @Test
    @DisplayName("Test Factory.getDescription()")
    void testGetDescription() {
        TestFactory factory = new TestFactory();
        
        assertEquals("Test Factory", factory.getDescription());
        
        factory.setDescription("Modified Factory");
        assertEquals("Modified Factory", factory.getDescription());
    }
    
    @Test
    @DisplayName("Test default isValidConfiguration implementation")
    void testDefaultIsValidConfiguration() {
        // Create a factory with default isValidConfiguration
        Factory<TestProduct, TestConfig> factory = new Factory<TestProduct, TestConfig>() {
            @Override
            public TestProduct create() {
                return new TestProduct("default", 0);
            }
            
            @Override
            public TestProduct create(TestConfig config) {
                return new TestProduct("configured", 1);
            }
            // Use default isValidConfiguration
        };
        
        // Default implementation should always return true
        assertTrue(factory.isValidConfiguration(null));
        assertTrue(factory.isValidConfiguration(new TestConfig("any", -999)));
    }
    
    @Test
    @DisplayName("Test default getDescription implementation")
    void testDefaultGetDescription() {
        // Create an anonymous factory
        Factory<TestProduct, TestConfig> factory = new Factory<TestProduct, TestConfig>() {
            @Override
            public TestProduct create() {
                return new TestProduct("default", 0);
            }
            
            @Override
            public TestProduct create(TestConfig config) {
                return new TestProduct("configured", 1);
            }
            // Use default getDescription
        };
        
        // Default implementation should return the simple class name
        String description = factory.getDescription();
        assertNotNull(description);
        // Anonymous class names may contain '$' or may be empty string
        assertTrue(description.isEmpty() || description.contains("$"));
    }
    
    @Test
    @DisplayName("Test Factory with Void configuration type")
    void testFactoryWithVoidConfiguration() {
        // Factory that doesn't need configuration
        Factory<TestProduct, Void> simpleFactory = new Factory<TestProduct, Void>() {
            @Override
            public TestProduct create() {
                return new TestProduct("simple", 1);
            }
            
            @Override
            public TestProduct create(Void config) {
                // Should just delegate to create()
                return create();
            }
        };
        
        TestProduct product1 = simpleFactory.create();
        assertNotNull(product1);
        assertEquals("simple", product1.getValue());
        
        TestProduct product2 = simpleFactory.create(null);
        assertNotNull(product2);
        assertEquals("simple", product2.getValue());
    }
    
    @Test
    @DisplayName("Test Factory that requires explicit configuration")
    void testFactoryRequiringExplicitConfiguration() {
        Factory<TestProduct, TestConfig> strictFactory = new Factory<TestProduct, TestConfig>() {
            @Override
            public TestProduct create() {
                throw new UnsupportedOperationException(
                    "This factory requires explicit configuration");
            }
            
            @Override
            public TestProduct create(TestConfig config) {
                if (config == null) {
                    throw new IllegalArgumentException("Config is required");
                }
                return new TestProduct(config.getConfigValue(), config.getConfigNumber());
            }
        };
        
        // create() without config should throw
        assertThrows(UnsupportedOperationException.class, () -> {
            strictFactory.create();
        });
        
        // create(null) should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            strictFactory.create(null);
        });
        
        // create(config) should work
        TestConfig config = new TestConfig("strict", 99);
        TestProduct product = strictFactory.create(config);
        assertNotNull(product);
        assertEquals("strict", product.getValue());
        assertEquals(99, product.getNumber());
    }
}