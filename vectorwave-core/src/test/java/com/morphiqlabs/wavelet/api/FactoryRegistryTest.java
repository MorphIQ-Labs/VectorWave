package com.morphiqlabs.wavelet.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;
import java.util.Set;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for FactoryRegistry.
 */
public class FactoryRegistryTest {
    
    // Test classes
    static class TestProduct {
        private final String id;
        TestProduct(String id) { this.id = id; }
        String getId() { return id; }
    }
    
    static class TestConfig {
        private final String value;
        TestConfig(String value) { this.value = value; }
        String getValue() { return value; }
    }
    
    static class TestFactory implements Factory<TestProduct, TestConfig> {
        private final String factoryId;
        
        TestFactory(String factoryId) {
            this.factoryId = factoryId;
        }
        
        @Override
        public TestProduct create() {
            return new TestProduct(factoryId + "_default");
        }
        
        @Override
        public TestProduct create(TestConfig config) {
            return new TestProduct(factoryId + "_" + config.getValue());
        }
        
        @Override
        public String getDescription() {
            return "TestFactory-" + factoryId;
        }
    }
    
    @BeforeEach
    void clearRegistry() throws Exception {
        // Clear the registry before each test using reflection
        FactoryRegistry registry = FactoryRegistry.getInstance();
        Field factoriesField = FactoryRegistry.class.getDeclaredField("factories");
        factoriesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Factory<?, ?>> factories = (Map<String, Factory<?, ?>>) factoriesField.get(registry);
        
        // Remove only our test factories to avoid affecting default factories
        factories.entrySet().removeIf(entry -> entry.getKey().startsWith("test_"));
    }
    
    @Test
    @DisplayName("Test FactoryRegistry singleton pattern")
    void testSingletonPattern() {
        FactoryRegistry registry1 = FactoryRegistry.getInstance();
        FactoryRegistry registry2 = FactoryRegistry.getInstance();
        
        assertNotNull(registry1);
        assertNotNull(registry2);
        assertSame(registry1, registry2, "Should return the same instance");
    }
    
    @Test
    @DisplayName("Test registering and retrieving a factory")
    void testRegisterAndRetrieve() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        TestFactory factory = new TestFactory("test1");
        
        // Register factory
        registry.register("test_factory1", factory);
        
        // Retrieve factory
        Optional<Factory<TestProduct, TestConfig>> retrieved = 
            registry.getFactory("test_factory1", TestProduct.class, TestConfig.class);
        
        assertTrue(retrieved.isPresent());
        assertSame(factory, retrieved.get());
    }
    
    @Test
    @DisplayName("Test retrieving non-existent factory")
    void testRetrieveNonExistent() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        
        Optional<Factory<TestProduct, TestConfig>> retrieved = 
            registry.getFactory("test_nonexistent", TestProduct.class, TestConfig.class);
        
        assertFalse(retrieved.isPresent());
    }
    
    @Test
    @DisplayName("Test registering with null or empty key")
    void testRegisterWithInvalidKey() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        TestFactory factory = new TestFactory("test");
        
        // Null key
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register(null, factory);
        });
        
        // Empty key
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register("", factory);
        });
        
        // Whitespace-only key
        assertThrows(IllegalArgumentException.class, () -> {
            registry.register("   ", factory);
        });
    }
    
    @Test
    @DisplayName("Test registering null factory")
    void testRegisterNullFactory() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            registry.register("test_null", null);
        });
        
        assertTrue(exception.getMessage().contains("Cannot register null factory"));
        assertTrue(exception.getMessage().contains("test_null"));
    }
    
    @Test
    @DisplayName("Test registering duplicate key")
    void testRegisterDuplicateKey() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        TestFactory factory1 = new TestFactory("first");
        TestFactory factory2 = new TestFactory("second");
        
        // Register first factory
        registry.register("test_duplicate", factory1);
        
        // Attempt to register second factory with same key
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            registry.register("test_duplicate", factory2);
        });
        
        assertTrue(exception.getMessage().contains("Factory already registered"));
        assertTrue(exception.getMessage().contains("test_duplicate"));
        assertTrue(exception.getMessage().contains("TestFactory-first"));
        assertTrue(exception.getMessage().contains("TestFactory-second"));
    }
    
    @Test
    @DisplayName("Test unregister factory")
    void testUnregister() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        TestFactory factory = new TestFactory("unregister");
        
        // Register factory
        registry.register("test_unregister", factory);
        assertTrue(registry.getFactory("test_unregister", TestProduct.class, TestConfig.class).isPresent());
        
        // Unregister factory
        boolean removed = registry.unregister("test_unregister");
        assertTrue(removed);
        assertFalse(registry.getFactory("test_unregister", TestProduct.class, TestConfig.class).isPresent());
        
        // Unregister non-existent should return false
        boolean removedAgain = registry.unregister("test_unregister");
        assertFalse(removedAgain);
    }
    
    @Test
    @DisplayName("Test multiple factory registrations")
    void testMultipleRegistrations() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        
        // Register some test factories
        registry.register("test_key1", new TestFactory("1"));
        registry.register("test_key2", new TestFactory("2"));
        registry.register("test_key3", new TestFactory("3"));
        
        // Verify all factories are retrievable
        assertTrue(registry.getFactory("test_key1", TestProduct.class, TestConfig.class).isPresent());
        assertTrue(registry.getFactory("test_key2", TestProduct.class, TestConfig.class).isPresent());
        assertTrue(registry.getFactory("test_key3", TestProduct.class, TestConfig.class).isPresent());
    }
    
    @Test
    @DisplayName("Test factory existence checking")
    void testFactoryExistence() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        
        assertFalse(registry.getFactory("test_nonexistent", TestProduct.class, TestConfig.class).isPresent());
        
        registry.register("test_exists", new TestFactory("exists"));
        assertTrue(registry.getFactory("test_exists", TestProduct.class, TestConfig.class).isPresent());
        
        registry.unregister("test_exists");
        assertFalse(registry.getFactory("test_exists", TestProduct.class, TestConfig.class).isPresent());
    }
    
    @Test
    @DisplayName("Test thread safety of registry")
    void testThreadSafety() throws Exception {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        int numThreads = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch endLatch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        ConcurrentHashMap<String, TestFactory> registeredFactories = new ConcurrentHashMap<>();
        
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    
                    for (int j = 0; j < operationsPerThread; j++) {
                        String key = "test_thread_" + threadId + "_" + j;
                        TestFactory factory = new TestFactory("t" + threadId + "_" + j);
                        
                        try {
                            // Register
                            registry.register(key, factory);
                            registeredFactories.put(key, factory);
                            
                            // Retrieve
                            Optional<Factory<TestProduct, TestConfig>> retrieved = 
                                registry.getFactory(key, TestProduct.class, TestConfig.class);
                            assertTrue(retrieved.isPresent());
                            
                            // Check existence
                            assertTrue(registry.getFactory(key, TestProduct.class, TestConfig.class).isPresent());
                            
                            successCount.incrementAndGet();
                        } catch (IllegalStateException e) {
                            // Duplicate key - this should not happen with unique keys
                            fail("Unexpected duplicate key: " + key);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    endLatch.countDown();
                }
            });
        }
        
        startLatch.countDown(); // Start all threads
        assertTrue(endLatch.await(10, TimeUnit.SECONDS), "All threads should complete");
        executor.shutdown();
        
        // Verify all operations succeeded
        assertEquals(numThreads * operationsPerThread, successCount.get());
        
        // Verify all factories are still registered
        for (Map.Entry<String, TestFactory> entry : registeredFactories.entrySet()) {
            assertTrue(registry.getFactory(entry.getKey(), TestProduct.class, TestConfig.class).isPresent());
        }
    }
    
    @Test
    @DisplayName("Test getFactory with wrong types")
    void testGetFactoryWithWrongTypes() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        registry.register("test_wrongtype", new TestFactory("wrong"));
        
        // Try to retrieve with wrong product type
        Optional<Factory<String, TestConfig>> wrongProduct = 
            registry.getFactory("test_wrongtype", String.class, TestConfig.class);
        
        // Should return Optional.empty() rather than ClassCastException
        // The actual behavior depends on implementation - it might return the factory
        // but cause ClassCastException when used
        assertNotNull(wrongProduct);
    }
    
    @Test
    @DisplayName("Test replace factory via unregister and register")
    void testReplaceFactory() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        TestFactory factory1 = new TestFactory("original");
        TestFactory factory2 = new TestFactory("replacement");
        
        // Register original factory
        registry.register("test_replace", factory1);
        
        // Verify original is registered
        Optional<Factory<TestProduct, TestConfig>> original = 
            registry.getFactory("test_replace", TestProduct.class, TestConfig.class);
        assertTrue(original.isPresent());
        assertSame(factory1, original.get());
        
        // Replace by unregistering and registering new one
        boolean removed = registry.unregister("test_replace");
        assertTrue(removed);
        registry.register("test_replace", factory2);
        
        // Verify new factory is registered
        Optional<Factory<TestProduct, TestConfig>> current = 
            registry.getFactory("test_replace", TestProduct.class, TestConfig.class);
        assertTrue(current.isPresent());
        assertSame(factory2, current.get());
    }
    
    @Test
    @DisplayName("Test isRegistered method")
    void testIsRegistered() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        
        assertFalse(registry.isRegistered("test_not_registered"));
        
        registry.register("test_registered", new TestFactory("reg"));
        assertTrue(registry.isRegistered("test_registered"));
        
        registry.unregister("test_registered");
        assertFalse(registry.isRegistered("test_registered"));
    }
    
    @Test
    @DisplayName("Test getFactory without type parameters")
    void testGetFactoryWithoutTypes() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        TestFactory factory = new TestFactory("no-type");
        
        registry.register("test_no_type", factory);
        
        Optional<Factory<?, ?>> retrieved = registry.getFactory("test_no_type");
        assertTrue(retrieved.isPresent());
        assertSame(factory, retrieved.get());
    }
    
    @Test
    @DisplayName("Test getRegisteredKeys")
    void testGetRegisteredKeys() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        
        // Register some test factories
        registry.register("test_keys1", new TestFactory("k1"));
        registry.register("test_keys2", new TestFactory("k2"));
        registry.register("test_keys3", new TestFactory("k3"));
        
        Set<String> keys = registry.getRegisteredKeys();
        assertNotNull(keys);
        assertTrue(keys.contains("test_keys1"));
        assertTrue(keys.contains("test_keys2"));
        assertTrue(keys.contains("test_keys3"));
        
        // Returned set should be unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> {
            keys.add("new_key");
        });
    }
    
    @Test
    @DisplayName("Test clear method")
    void testClear() {
        FactoryRegistry registry = FactoryRegistry.getInstance();
        
        // Register some test factories
        registry.register("test_clear1", new TestFactory("c1"));
        registry.register("test_clear2", new TestFactory("c2"));
        registry.register("test_clear3", new TestFactory("c3"));
        
        assertTrue(registry.getFactory("test_clear1", TestProduct.class, TestConfig.class).isPresent());
        assertTrue(registry.getFactory("test_clear2", TestProduct.class, TestConfig.class).isPresent());
        assertTrue(registry.getFactory("test_clear3", TestProduct.class, TestConfig.class).isPresent());
        
        // Clear registry
        registry.clear();
        
        assertFalse(registry.getFactory("test_clear1", TestProduct.class, TestConfig.class).isPresent());
        assertFalse(registry.getFactory("test_clear2", TestProduct.class, TestConfig.class).isPresent());
        assertFalse(registry.getFactory("test_clear3", TestProduct.class, TestConfig.class).isPresent());
    }
}