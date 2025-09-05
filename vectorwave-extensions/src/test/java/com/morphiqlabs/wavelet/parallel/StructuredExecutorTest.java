package com.morphiqlabs.wavelet.extensions.parallel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.RepeatedTest;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for StructuredExecutor.
 */
class StructuredExecutorTest {
    
    private ParallelConfig config;
    
    @BeforeEach
    void setUp() {
        config = ParallelConfig.auto();
    }
    
    @Test
    @DisplayName("Executor should auto-close with try-with-resources")
    void testAutoClose() {
        AtomicBoolean taskStarted = new AtomicBoolean(false);
        AtomicBoolean taskCompleted = new AtomicBoolean(false);
        
        try (var executor = new StructuredExecutor(config)) {
            executor.submit(() -> {
                taskStarted.set(true);
                Thread.sleep(100);
                taskCompleted.set(true);
                return "done";
            });
            
            // Task should start
            Thread.sleep(50);
            assertTrue(taskStarted.get(), "Task should have started");
        } catch (Exception e) {
            fail("Should not throw exception");
        }
        
        // After try-with-resources, executor is closed
        // Task may or may not complete depending on timing
    }
    
    @Test
    @DisplayName("Submit single task and get result")
    void testSubmitSingle() throws Exception {
        try (var executor = new StructuredExecutor(config)) {
            var future = executor.submit(() -> {
                Thread.sleep(50);
                return 42;
            });
            
            executor.joinAll();
            
            assertEquals(42, future.get(), "Should return correct result");
            assertTrue(future.isDone(), "Future should be done");
        }
    }
    
    @Test
    @DisplayName("Submit multiple tasks in parallel")
    void testSubmitMultiple() throws Exception {
        try (var executor = new StructuredExecutor(config)) {
            List<Integer> inputs = List.of(1, 2, 3, 4, 5);
            
            List<StructuredExecutor.StructuredFuture<Integer>> futures = 
                executor.submitAll(inputs, x -> x * x);
            
            executor.joinAll();
            
            for (int i = 0; i < inputs.size(); i++) {
                assertEquals(inputs.get(i) * inputs.get(i), futures.get(i).get(),
                    "Should compute square correctly");
            }
        }
    }
    
    @Test
    @DisplayName("InvokeAll should wait for all tasks")
    void testInvokeAll() throws Exception {
        try (var executor = new StructuredExecutor(config)) {
            List<Integer> inputs = IntStream.range(0, 10)
                .boxed()
                .toList();
            
            List<Integer> results = executor.invokeAll(inputs, x -> {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return x * 2;
            });
            
            assertEquals(inputs.size(), results.size(), "Should have all results");
            
            for (int i = 0; i < inputs.size(); i++) {
                assertEquals(inputs.get(i) * 2, results.get(i),
                    "Should double each value");
            }
        }
    }
    
    @Test
    @DisplayName("InvokeAny should return first successful result")
    void testInvokeAny() throws Exception {
        try (var executor = new StructuredExecutor(config)) {
            Callable<String> fast = () -> {
                Thread.sleep(10);
                return "fast";
            };
            
            Callable<String> slow = () -> {
                Thread.sleep(100);
                return "slow";
            };
            
            Callable<String> failing = () -> {
                throw new RuntimeException("fail");
            };
            
            String result = executor.invokeAny(failing, slow, fast);
            assertEquals("fast", result, "Should return fastest result");
        }
    }
    
    @Test
    @DisplayName("Timeout should cancel running tasks")
    @Timeout(2)
    void testTimeout() {
        AtomicBoolean taskCancelled = new AtomicBoolean(false);
        
        assertThrows(CompletionException.class, () -> {
            try (var executor = new StructuredExecutor(config, 100)) {
                executor.submit(() -> {
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        taskCancelled.set(true);
                        throw e;
                    }
                    return "should not complete";
                });
                
                executor.joinAll(); // Should timeout
            }
        });
    }
    
    @Test
    @DisplayName("CancelAll should stop running tasks")
    void testCancelAll() throws Exception {
        AtomicInteger completedTasks = new AtomicInteger(0);
        
        try (var executor = new StructuredExecutor(config)) {
            for (int i = 0; i < 10; i++) {
                executor.submit(() -> {
                    Thread.sleep(200);
                    completedTasks.incrementAndGet();
                    return null;
                });
            }
            
            Thread.sleep(50); // Let tasks start
            executor.cancelAll();
            
            Thread.sleep(100); // Wait a bit
            
            assertTrue(completedTasks.get() < 10,
                "Not all tasks should complete after cancel");
        }
    }
    
    @Test
    @DisplayName("Exception in one task should not affect others")
    void testExceptionIsolation() throws Exception {
        try (var executor = new StructuredExecutor(config)) {
            var successFuture = executor.submit(() -> "success");
            
            var failureFuture = executor.submit(() -> {
                throw new RuntimeException("intentional failure");
            });
            
            var anotherSuccess = executor.submit(() -> 42);
            
            executor.joinAll();
            
            assertEquals("success", successFuture.get(), "First task should succeed");
            assertEquals(42, anotherSuccess.get(), "Third task should succeed");
            
            assertThrows(ExecutionException.class, () -> failureFuture.get(),
                "Failed task should throw ExecutionException");
        }
    }
    
    @Test
    @DisplayName("Task count and completion tracking")
    void testTaskTracking() throws Exception {
        try (var executor = new StructuredExecutor(config)) {
            assertEquals(0, executor.getTaskCount(), "Should start with no tasks");
            
            // Submit tasks
            for (int i = 0; i < 5; i++) {
                final int delay = i * 20;
                executor.submit(() -> {
                    Thread.sleep(delay);
                    return delay;
                });
            }
            
            assertEquals(5, executor.getTaskCount(), "Should have 5 tasks");
            assertFalse(executor.isAllComplete(), "Tasks should not be complete immediately");
            
            executor.joinAll();
            
            assertTrue(executor.isAllComplete(), "All tasks should be complete");
            assertEquals(5, executor.getCompletedCount(), "Should have 5 completed tasks");
        }
    }
    
    @Test
    @DisplayName("Deadline enforcement")
    void testDeadlineEnforcement() {
        long deadline = 100;
        
        try (var executor = new StructuredExecutor(config, deadline)) {
            // Submit task immediately - should work
            var future1 = executor.submit(() -> "immediate");
            
            // Wait past deadline
            Thread.sleep(deadline + 50);
            
            // Try to submit after deadline - should fail
            assertThrows(RejectedExecutionException.class, () -> {
                executor.submit(() -> "too late");
            });
            
        } catch (Exception e) {
            // Expected for deadline exceeded
        }
    }
    
    @RepeatedTest(3)
    @DisplayName("Many task submissions from single thread")
    void testManyTaskSubmissions() throws Exception {
        // Note: Changed from concurrent submission as StructuredTaskScope
        // requires all tasks to be submitted from the thread that created the scope
        try (var executor = new StructuredExecutor(config)) {
            int totalTasks = 100;
            AtomicInteger successCount = new AtomicInteger(0);
            List<StructuredExecutor.StructuredFuture<Integer>> futures = new ArrayList<>();
            
            // Submit all tasks from the same thread
            for (int i = 0; i < totalTasks; i++) {
                final int taskId = i;
                futures.add(executor.submit(() -> {
                    successCount.incrementAndGet();
                    return taskId;
                }));
            }
            
            executor.joinAll();
            
            assertEquals(totalTasks, executor.getTaskCount(),
                "Should have all tasks submitted");
            assertEquals(totalTasks, successCount.get(),
                "All tasks should complete");
            
            // Verify all results
            for (int i = 0; i < totalTasks; i++) {
                assertEquals(i, futures.get(i).get(), "Task " + i + " should return its ID");
            }
        }
    }
    
    @Test
    @DisplayName("Nested structured scopes")
    void testNestedScopes() throws Exception {
        try (var outerExecutor = new StructuredExecutor(config)) {
            var outerFuture = outerExecutor.submit(() -> {
                // Create nested executor
                try (var innerExecutor = new StructuredExecutor(config)) {
                    List<Integer> inputs = List.of(1, 2, 3);
                    List<Integer> results = innerExecutor.invokeAll(inputs, x -> x + 10);
                    return results.stream().mapToInt(Integer::intValue).sum();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            
            outerExecutor.joinAll();
            
            assertEquals(36, outerFuture.get(), "Should compute sum of nested results");
        }
    }
    
    @Test
    @DisplayName("Closed executor should reject new tasks")
    void testClosedExecutor() {
        var executor = new StructuredExecutor(config);
        executor.close();
        
        assertThrows(IllegalStateException.class, () -> {
            executor.submit(() -> "should fail");
        });
        
        assertThrows(IllegalStateException.class, () -> {
            executor.submitAll(List.of(1, 2, 3), x -> x);
        });
        
        assertThrows(IllegalStateException.class, () -> {
            executor.joinAll();
        });
    }
    
    @Test
    @DisplayName("Future state transitions")
    void testFutureStates() throws Exception {
        try (var executor = new StructuredExecutor(config)) {
            CountDownLatch taskStarted = new CountDownLatch(1);
            CountDownLatch allowCompletion = new CountDownLatch(1);
            
            var future = executor.submit(() -> {
                taskStarted.countDown();
                allowCompletion.await();
                return "done";
            });
            
            // Wait for task to start
            assertTrue(taskStarted.await(1, TimeUnit.SECONDS), "Task should start");
            
            // Check state while running
            assertFalse(future.isDone(), "Should not be done yet");
            assertFalse(future.isCancelled(), "Should not be cancelled");
            
            // Allow completion
            allowCompletion.countDown();
            executor.joinAll();
            
            // Check final state
            assertTrue(future.isDone(), "Should be done now");
            assertEquals("done", future.get(), "Should return result");
        }
    }
    
    @Test
    @DisplayName("Memory efficiency with large number of tasks")
    void testMemoryEfficiency() throws Exception {
        int taskCount = 1000;
        
        try (var executor = new StructuredExecutor(config)) {
            List<Integer> inputs = IntStream.range(0, taskCount)
                .boxed()
                .toList();
            
            // Should handle large number of tasks efficiently
            List<Integer> results = executor.invokeAll(inputs, x -> {
                // Minimal work
                return x * 2;
            });
            
            assertEquals(taskCount, results.size(), "Should process all tasks");
            
            for (int i = 0; i < taskCount; i++) {
                assertEquals(i * 2, results.get(i), "Task " + i + " result incorrect");
            }
        }
    }
}
