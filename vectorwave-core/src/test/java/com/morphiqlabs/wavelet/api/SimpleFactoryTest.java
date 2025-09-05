package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the SimpleFactory interface.
 */
class SimpleFactoryTest {
    
    @Test
    void testSimpleFactoryCreate() {
        TestSimpleFactory factory = new TestSimpleFactory();
        String result = factory.create();
        assertEquals("simple", result);
    }
    
    @Test
    void testSimpleFactoryCreateWithConfigThrowsException() {
        TestSimpleFactory factory = new TestSimpleFactory();
        
        UnsupportedOperationException exception = assertThrows(
            UnsupportedOperationException.class,
            () -> factory.create(null)
        );
        
        assertTrue(exception.getMessage().contains("SimpleFactory does not support configuration"));
        assertTrue(exception.getMessage().contains("Use create() instead"));
    }
    
    @Test
    void testSimpleFactoryValidationAlwaysTrue() {
        TestSimpleFactory factory = new TestSimpleFactory();
        
        assertTrue(factory.isValidConfiguration(null));
        // Should still return true since validation is overridden for SimpleFactory
        assertTrue(factory.isValidConfiguration(null));
    }
    
    @Test
    void testSimpleFactoryInheritsDescription() {
        TestSimpleFactory factory = new TestSimpleFactory();
        assertEquals("TestSimpleFactory", factory.getDescription());
    }
    
    @Test
    void testSimpleFactoryWithCustomDescription() {
        DescriptiveSimpleFactory factory = new DescriptiveSimpleFactory();
        assertEquals("Custom Simple Factory", factory.getDescription());
    }
    
    @Test
    void testSimpleFactoryExtendsFactory() {
        TestSimpleFactory factory = new TestSimpleFactory();
        
        // Verify it can be used as a Factory
        Factory<String, Void> genericFactory = factory;
        assertNotNull(genericFactory);
        
        // Verify create() works through the Factory interface
        String result = genericFactory.create();
        assertEquals("simple", result);
        
        // Verify create(Void) throws exception through Factory interface
        assertThrows(UnsupportedOperationException.class, () -> genericFactory.create(null));
    }
    
    // Test implementation of SimpleFactory
    private static class TestSimpleFactory implements SimpleFactory<String> {
        @Override
        public String create() {
            return "simple";
        }
    }
    
    // Test implementation with custom description
    private static class DescriptiveSimpleFactory implements SimpleFactory<String> {
        @Override
        public String create() {
            return "custom";
        }
        
        @Override
        public String getDescription() {
            return "Custom Simple Factory";
        }
    }
}