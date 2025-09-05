package com.morphiqlabs.examples.parallel;

import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.time.Instant;

/**
 * Structured concurrency executor for VectorWave parallel operations.
 * 
 * <p>This class provides structured concurrency patterns based on Java 24's preview features,
 * ensuring proper resource management and cancellation propagation for parallel wavelet 
 * operations. It wraps Java's {@link StructuredTaskScope} to provide a high-level API
 * optimized for wavelet transform workloads.</p>
 * 
 * <h2>Key Features</h2>
 * <ul>
 *   <li><b>Automatic Resource Management:</b> All tasks are automatically cleaned up when
 *       the scope exits, whether normally or exceptionally</li>
 *   <li><b>Cancellation Propagation:</b> If the parent scope is cancelled, all child tasks
 *       are automatically cancelled</li>
 *   <li><b>Deadline Enforcement:</b> Built-in timeout support with automatic task cancellation</li>
 *   <li><b>Exception Aggregation:</b> Centralized exception handling for all submitted tasks</li>
 *   <li><b>Fork-Join Semantics:</b> Clear task lifecycle with guaranteed cleanup</li>
 *   <li><b>Performance Optimization:</b> Integrates with {@link ParallelConfig} for
 *       workload-specific optimization</li>
 * </ul>
 * 
 * <h3>Thread Safety Requirements</h3>
 * <p><b>IMPORTANT:</b> Task submission methods ({@link #submit}, {@link #submitAll}, 
 * {@link #invokeAll}, {@link #invokeAny}) must be called from the same thread that created 
 * the executor. This is a fundamental requirement of Java's structured concurrency model. 
 * Attempting to submit tasks from multiple threads will result in {@code WrongThreadException}.</p>
 * 
 * <h3>Usage Patterns</h3>
 * 
 * <h4>Basic Batch Processing:</h4>
 * <pre>{@code
 * try (var executor = new StructuredExecutor()) {
 *     List<MODWTResult> results = executor.invokeAll(signals,
 *         signal -> transform.forward(signal));
 *     // Process results...
 * } // Automatic cleanup
 * }</pre>
 * 
 * <h4>Individual Task Submission:</h4>
 * <pre>{@code
 * try (var executor = new StructuredExecutor(config)) {
 *     List<StructuredFuture<MODWTResult>> futures = new ArrayList<>();
 *     
 *     for (double[] signal : signals) {
 *         futures.add(executor.submit(() -> transform.forward(signal)));
 *     }
 *     
 *     executor.joinAll(); // Wait for all tasks
 *     
 *     // Collect results
 *     for (var future : futures) {
 *         MODWTResult result = future.get();
 *         // Process result...
 *     }
 * }
 * }</pre>
 * 
 * <h4>Timeout Handling:</h4>
 * <pre>{@code
 * long timeoutMs = 5000; // 5 second timeout
 * try (var executor = new StructuredExecutor(config, timeoutMs)) {
 *     List<MODWTResult> results = executor.invokeAll(signals,
 *         signal -> transform.forward(signal));
 * } catch (CompletionException e) {
 *     if (e.getMessage().contains("timeout")) {
 *         // Handle timeout scenario
 *     }
 * }
 * }</pre>
 * 
 * <h3>Error Handling</h3>
 * <p>The executor provides comprehensive error handling:</p>
 * <ul>
 *   <li>{@link ExecutionException} - Individual task failures</li>
 *   <li>{@link InterruptedException} - Thread interruption</li>
 *   <li>{@link CompletionException} - Timeout or coordination failures</li>
 *   <li>{@link RejectedExecutionException} - Task submission after deadline</li>
 * </ul>
 * 
 * <h3>Performance Considerations</h3>
 * <p>Use {@link ParallelConfig} to optimize for your workload:</p>
 * <ul>
 *   <li><code>targetCores</code> - Number of worker threads</li>
 *   <li><code>minParallelThreshold</code> - When to use parallel vs sequential processing</li>
 *   <li><code>maxChunkSize</code> - Maximum work unit size for cache efficiency</li>
 * </ul>
 * 
 * @see StructuredParallelTransform For high-level wavelet transform operations
 * @see AdaptiveThresholdTuner For automatic performance optimization  
 * @see ParallelConfig For configuration options
 */
@SuppressWarnings("preview")  // StructuredTaskScope is a preview feature in Java 24
public class StructuredExecutor implements AutoCloseable {
    
    private final StructuredTaskScope<Object> scope;
    private final List<Future<?>> submittedTasks;
    private final ParallelConfig config;
    private final long deadlineNanos;
    private volatile boolean closed = false;
    
    /**
     * Creates a new structured executor with default configuration.
     */
    public StructuredExecutor() {
        this(ParallelConfig.auto());
    }
    
    /**
     * Creates a new structured executor with specified configuration.
     * 
     * @param config the parallel configuration to use
     */
    public StructuredExecutor(ParallelConfig config) {
        this(config, Long.MAX_VALUE);
    }
    
    /**
     * Creates a new structured executor with deadline.
     * 
     * @param config the parallel configuration to use
     * @param timeoutMs timeout in milliseconds
     */
    public StructuredExecutor(ParallelConfig config, long timeoutMs) {
        this.config = config;
        this.scope = new StructuredTaskScope<>();
        this.submittedTasks = new CopyOnWriteArrayList<>();
        this.deadlineNanos = timeoutMs == Long.MAX_VALUE ? 
            Long.MAX_VALUE : System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
    }
    
    /**
     * Submits a single task for execution.
     * 
     * @param <T> the result type
     * @param task the task to execute
     * @return future for the task result
     */
    public <T> StructuredFuture<T> submit(Callable<T> task) {
        ensureNotClosed();
        checkDeadline();
        
        @SuppressWarnings("unchecked")
        StructuredTaskScope.Subtask<T> subtask = scope.fork(() -> {
                try {
                    return task.call();
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            });
        
        StructuredFuture<T> future = new StructuredFuture<>(subtask);
        submittedTasks.add(future);
        return future;
    }
    
    /**
     * Submits multiple tasks for parallel execution.
     * 
     * @param <T> the input type
     * @param <R> the result type
     * @param inputs the input data
     * @param mapper function to transform inputs to results
     * @return list of futures for the results
     */
    public <T, R> List<StructuredFuture<R>> submitAll(
            List<T> inputs, Function<T, R> mapper) {
        ensureNotClosed();
        checkDeadline();
        
        List<StructuredFuture<R>> futures = new ArrayList<>(inputs.size());
        
        for (T input : inputs) {
            futures.add(submit(() -> mapper.apply(input)));
        }
        
        return futures;
    }
    
    /**
     * Executes tasks in parallel and collects results.
     * 
     * @param <T> the input type
     * @param <R> the result type
     * @param inputs the input data
     * @param mapper function to transform inputs to results
     * @return list of results
     * @throws ExecutionException if any task fails
     * @throws InterruptedException if interrupted while waiting
     */
    public <T, R> List<R> invokeAll(List<T> inputs, Function<T, R> mapper) 
            throws ExecutionException, InterruptedException {
        List<StructuredFuture<R>> futures = submitAll(inputs, mapper);
        
        // Wait for all tasks to complete
        joinAll();
        
        // Collect results
        List<R> results = new ArrayList<>(futures.size());
        for (StructuredFuture<R> future : futures) {
            results.add(future.get());
        }
        
        return results;
    }
    
    /**
     * Executes tasks and returns the result of the first to complete successfully.
     * 
     * @param <T> the result type
     * @param tasks the tasks to execute
     * @return result of the first successful task
     * @throws ExecutionException if all tasks fail
     * @throws InterruptedException if interrupted while waiting
     */
    @SafeVarargs
    public final <T> T invokeAny(Callable<T>... tasks) 
            throws ExecutionException, InterruptedException {
        ensureNotClosed();
        checkDeadline();
        
        try (var firstSuccess = new StructuredTaskScope.ShutdownOnSuccess<T>()) {
            for (Callable<T> task : tasks) {
                firstSuccess.fork(() -> {
                    try {
                        return task.call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            
            firstSuccess.join();
            return firstSuccess.result();
        }
    }
    
    /**
     * Joins all submitted tasks, waiting for completion or cancellation.
     * 
     * @throws InterruptedException if interrupted while waiting
     */
    public void joinAll() throws InterruptedException {
        ensureNotClosed();
        
        try {
            if (deadlineNanos != Long.MAX_VALUE) {
                long remainingNanos = deadlineNanos - System.nanoTime();
                if (remainingNanos > 0) {
                    scope.joinUntil(Instant.now().plusNanos(remainingNanos));
                } else {
                    scope.shutdown();
                    throw new CompletionException("Deadline exceeded", null);
                }
            } else {
                scope.join();
            }
        } catch (InterruptedException e) {
            scope.shutdown();
            Thread.currentThread().interrupt();
            throw e;
        } catch (TimeoutException e) {
            scope.shutdown();
            throw new CompletionException("Operation timed out", e);
        }
    }
    
    /**
     * Cancels all running tasks.
     */
    public void cancelAll() {
        if (!closed) {
            scope.shutdown();
        }
    }
    
    /**
     * Checks if all tasks have completed.
     * 
     * @return true if all tasks are done
     */
    public boolean isAllComplete() {
        return submittedTasks.stream()
            .allMatch(Future::isDone);
    }
    
    /**
     * Gets the number of submitted tasks.
     * 
     * @return number of tasks
     */
    public int getTaskCount() {
        return submittedTasks.size();
    }
    
    /**
     * Gets the number of completed tasks.
     * 
     * @return number of completed tasks
     */
    public int getCompletedCount() {
        return (int) submittedTasks.stream()
            .filter(Future::isDone)
            .count();
    }
    
    @Override
    public void close() {
        if (!closed) {
            closed = true;
            try {
                // Shutdown the scope - this cancels any running tasks
                scope.shutdown();
                
                // Wait briefly for tasks to terminate
                try {
                    scope.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            } finally {
                scope.close();
            }
        }
    }
    
    private void ensureNotClosed() {
        if (closed) {
            throw new IllegalStateException("Executor is closed");
        }
    }
    
    private void checkDeadline() {
        if (deadlineNanos != Long.MAX_VALUE && System.nanoTime() > deadlineNanos) {
            throw new RejectedExecutionException("Deadline has passed");
        }
    }
    
    /**
     * A future that wraps a structured task scope subtask.
     * 
     * @param <T> the result type
     */
    public static class StructuredFuture<T> implements Future<T> {
        private final StructuredTaskScope.Subtask<T> subtask;
        
        StructuredFuture(StructuredTaskScope.Subtask<T> subtask) {
            this.subtask = subtask;
        }
        
        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            // Structured concurrency handles cancellation through scope
            return false;
        }
        
        @Override
        public boolean isCancelled() {
            // In structured concurrency, tasks cannot be individually cancelled
            // They can only be cancelled as a group when the scope is shut down
            // UNAVAILABLE means the result isn't available yet, not cancelled
            return false;
        }
        
        @Override
        public boolean isDone() {
            var state = subtask.state();
            return state == StructuredTaskScope.Subtask.State.SUCCESS ||
                   state == StructuredTaskScope.Subtask.State.FAILED;
        }
        
        @Override
        public T get() throws InterruptedException, ExecutionException {
            try {
                return get(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (TimeoutException e) {
                // Should never happen with MAX_VALUE timeout
                throw new ExecutionException("Unexpected timeout", e);
            }
        }
        
        @Override
        public T get(long timeout, TimeUnit unit) 
                throws InterruptedException, ExecutionException, TimeoutException {
            var state = subtask.state();
            
            if (state == StructuredTaskScope.Subtask.State.SUCCESS) {
                return subtask.get();
            } else if (state == StructuredTaskScope.Subtask.State.FAILED) {
                Throwable exception = subtask.exception();
                throw new ExecutionException(exception);
            } else {
                // UNAVAILABLE means still running or not yet joined
                // In structured concurrency we wait via scope.join()
                throw new IllegalStateException("Task not yet complete - use joinAll() first");
            }
        }
        
        /**
         * Gets the subtask state.
         * 
         * @return the state
         */
        public StructuredTaskScope.Subtask.State getState() {
            return subtask.state();
        }
    }
    
}
