package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for AbstractStaticFactory.
 */
public class AbstractStaticFactoryTest {
    
    // Test implementation classes
    static class TestProduct {
        private final String value;
        TestProduct(String value) { this.value = value; }
        String getValue() { return value; }
    }
    
    static class TestConfig {
        private final String configValue;
        TestConfig(String configValue) { this.configValue = configValue; }
        String getConfigValue() { return configValue; }
    }
    
    static class TestStaticFactory extends AbstractStaticFactory<TestProduct, TestConfig> {
        private boolean throwOnCreate = false;
        private String description = "TestStaticFactory";
        
        @Override
        protected TestProduct doCreate() {
            if (throwOnCreate) {
                throw new UnsupportedOperationException("Cannot create without config");
            }
            return new TestProduct("default");
        }
        
        @Override
        protected TestProduct doCreate(TestConfig config) {
            return new TestProduct(config.getConfigValue());
        }
        
        @Override
        protected TestConfig getDefaultConfiguration() {
            return new TestConfig("default-config");
        }
        
        @Override
        public String getDescription() {
            return description;
        }
        
        void setThrowOnCreate(boolean throwOnCreate) {
            this.throwOnCreate = throwOnCreate;
        }
        
        void setDescription(String description) {
            this.description = description;
        }
    }
    
    @Test
    @DisplayName("Test create with default configuration")
    void testCreateWithDefaultConfiguration() {
        TestStaticFactory factory = new TestStaticFactory();
        
        TestProduct product = factory.create();
        assertNotNull(product);
        assertEquals("default", product.getValue());
    }
    
    @Test
    @DisplayName("Test create with valid configuration")
    void testCreateWithValidConfiguration() {
        TestStaticFactory factory = new TestStaticFactory();
        TestConfig config = new TestConfig("custom");
        
        TestProduct product = factory.create(config);
        assertNotNull(product);
        assertEquals("custom", product.getValue());
    }
    
    @Test
    @DisplayName("Test create with null configuration")
    void testCreateWithNullConfiguration() {
        TestStaticFactory factory = new TestStaticFactory();
        
        // NullChecks throws InvalidArgumentException for null values
        assertThrows(Exception.class, () -> {
            factory.create(null);
        });
    }
    
    @Test
    @DisplayName("Test createInstance method")
    void testCreateInstance() {
        TestStaticFactory factory = new TestStaticFactory();
        
        TestProduct product = factory.createInstance();
        assertNotNull(product);
        assertEquals("default", product.getValue());
    }
    
    @Test
    @DisplayName("Test createInstance with config")
    void testCreateInstanceWithConfig() {
        TestStaticFactory factory = new TestStaticFactory();
        TestConfig config = new TestConfig("instance-custom");
        
        TestProduct product = factory.createInstance(config);
        assertNotNull(product);
        assertEquals("instance-custom", product.getValue());
    }
    
    @Test
    @DisplayName("Test isValidConfiguration with default implementation")
    void testIsValidConfigurationDefault() {
        TestStaticFactory factory = new TestStaticFactory();
        
        // Default implementation should return true for non-null configs
        assertTrue(factory.isValidConfiguration(new TestConfig("any")));
    }
    
    @Test
    @DisplayName("Test create with invalid configuration")
    void testCreateWithInvalidConfiguration() {
        TestStaticFactory factory = new TestStaticFactory() {
            @Override
            public boolean isValidConfiguration(TestConfig config) {
                // Only accept configs with value "valid"
                return config != null && "valid".equals(config.getConfigValue());
            }
        };
        
        TestConfig invalidConfig = new TestConfig("invalid");
        
        assertThrows(IllegalArgumentException.class, () -> {
            factory.create(invalidConfig);
        });
    }
    
    @Test
    @DisplayName("Test create with valid configuration after validation")
    void testCreateWithValidConfigurationAfterValidation() {
        TestStaticFactory factory = new TestStaticFactory() {
            @Override
            public boolean isValidConfiguration(TestConfig config) {
                // Only accept configs with value "valid"
                return config != null && "valid".equals(config.getConfigValue());
            }
        };
        
        TestConfig validConfig = new TestConfig("valid");
        TestProduct product = factory.create(validConfig);
        
        assertNotNull(product);
        assertEquals("valid", product.getValue());
    }
    
    @Test
    @DisplayName("Test getDescription")
    void testGetDescription() {
        TestStaticFactory factory = new TestStaticFactory();
        
        assertEquals("TestStaticFactory", factory.getDescription());
        
        factory.setDescription("Modified Factory");
        assertEquals("Modified Factory", factory.getDescription());
    }
    
    @Test
    @DisplayName("Test factory that requires configuration")
    void testFactoryRequiringConfiguration() {
        TestStaticFactory factory = new TestStaticFactory();
        factory.setThrowOnCreate(true);
        
        // create() without config should throw
        assertThrows(UnsupportedOperationException.class, () -> {
            factory.create();
        });
        
        // create(config) should work
        TestConfig config = new TestConfig("required");
        TestProduct product = factory.create(config);
        assertNotNull(product);
        assertEquals("required", product.getValue());
    }
    
    @Test
    @DisplayName("Test error message includes description")
    void testErrorMessageIncludesDescription() {
        TestStaticFactory factory = new TestStaticFactory() {
            @Override
            public boolean isValidConfiguration(TestConfig config) {
                return false; // Always invalid
            }
        };
        factory.setDescription("SpecialFactory");
        
        TestConfig config = new TestConfig("any");
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            factory.create(config);
        });
        
        assertTrue(exception.getMessage().contains("SpecialFactory"));
    }
    
    @Test
    @DisplayName("Test protected methods delegation")
    void testProtectedMethodsDelegation() {
        TestStaticFactory factory = new TestStaticFactory();
        
        // Test that createInstance delegates to create()
        TestProduct product1 = factory.createInstance();
        TestProduct product2 = factory.create();
        
        assertNotNull(product1);
        assertNotNull(product2);
        assertEquals(product1.getValue(), product2.getValue());
        
        // Test that createInstance(config) delegates to create(config)
        TestConfig config = new TestConfig("delegated");
        TestProduct product3 = factory.createInstance(config);
        TestProduct product4 = factory.create(config);
        
        assertNotNull(product3);
        assertNotNull(product4);
        assertEquals(product3.getValue(), product4.getValue());
    }
}