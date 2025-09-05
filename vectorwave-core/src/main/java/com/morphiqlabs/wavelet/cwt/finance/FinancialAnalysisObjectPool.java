package com.morphiqlabs.wavelet.cwt.finance;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Object pool for financial analysis to reduce GC pressure.
 * 
 * <p>This pool manages reusable objects and arrays used in hot paths
 * of financial analysis, significantly reducing memory allocation overhead.</p>
 */
public final class FinancialAnalysisObjectPool {
    
    // Pool configuration
    private static final int DEFAULT_MAX_POOL_SIZE = 16;
    private static final int DEFAULT_ARRAY_SIZE = 1024;
    
    // Pools for different object types
    private final ConcurrentLinkedQueue<ArrayHolder> arrayPool;
    private final ConcurrentLinkedQueue<TradingSignalBuilder> signalBuilderPool;
    private final ConcurrentLinkedQueue<IntListBuilder> intListPool;
    private final ConcurrentLinkedQueue<DoubleListBuilder> doubleListPool;
    
    // Pool statistics
    private final AtomicInteger arrayPoolHits = new AtomicInteger(0);
    private final AtomicInteger arrayPoolMisses = new AtomicInteger(0);
    
    private final int maxPoolSize;
    private final int defaultArraySize;
    
    /**
     * Creates a pool with default configuration.
     */
    public FinancialAnalysisObjectPool() {
        this(DEFAULT_MAX_POOL_SIZE, DEFAULT_ARRAY_SIZE);
    }
    
    /**
     * Creates a pool with custom configuration.
     * 
     * @param maxPoolSize maximum objects to keep in each pool
     * @param defaultArraySize default size for array allocations
     */
    public FinancialAnalysisObjectPool(int maxPoolSize, int defaultArraySize) {
        this.maxPoolSize = maxPoolSize;
        this.defaultArraySize = defaultArraySize;
        
        this.arrayPool = new ConcurrentLinkedQueue<>();
        this.signalBuilderPool = new ConcurrentLinkedQueue<>();
        this.intListPool = new ConcurrentLinkedQueue<>();
        this.doubleListPool = new ConcurrentLinkedQueue<>();
        
        // Pre-populate pools
        for (int i = 0; i < maxPoolSize / 2; i++) {
            arrayPool.offer(new ArrayHolder(defaultArraySize));
            signalBuilderPool.offer(new TradingSignalBuilder());
            intListPool.offer(new IntListBuilder());
            doubleListPool.offer(new DoubleListBuilder());
        }
    }
    
    /**
     * Borrows a double array from the pool.
     * 
     * @param minSize minimum required size
     * @return array holder that must be returned to pool
     */
    public ArrayHolder borrowArray(int minSize) {
        ArrayHolder holder = arrayPool.poll();
        if (holder == null) {
            arrayPoolMisses.incrementAndGet();
            holder = new ArrayHolder(Math.max(minSize, defaultArraySize));
        } else {
            arrayPoolHits.incrementAndGet();
            if (holder.array.length < minSize) {
                holder.array = new double[Math.max(minSize, defaultArraySize)];
            }
            holder.clear();
        }
        holder.setPool(this);
        return holder;
    }
    
    /**
     * Returns an array to the pool.
     * 
     * @param holder the array holder to return
     */
    public void returnArray(ArrayHolder holder) {
        if (holder != null && arrayPool.size() < maxPoolSize) {
            holder.clear();
            arrayPool.offer(holder);
        }
    }
    
    /**
     * Borrows a trading signal builder from the pool.
     */
    public TradingSignalBuilder borrowSignalBuilder() {
        TradingSignalBuilder builder = signalBuilderPool.poll();
        if (builder == null) {
            return new TradingSignalBuilder();
        }
        builder.clear();
        return builder;
    }
    
    /**
     * Returns a signal builder to the pool.
     */
    public void returnSignalBuilder(TradingSignalBuilder builder) {
        if (builder != null && signalBuilderPool.size() < maxPoolSize) {
            builder.clear();
            signalBuilderPool.offer(builder);
        }
    }
    
    /**
     * Borrows an int list builder from the pool.
     */
    public IntListBuilder borrowIntList() {
        IntListBuilder builder = intListPool.poll();
        if (builder == null) {
            return new IntListBuilder();
        }
        builder.clear();
        return builder;
    }
    
    /**
     * Returns an int list builder to the pool.
     */
    public void returnIntList(IntListBuilder builder) {
        if (builder != null && intListPool.size() < maxPoolSize) {
            builder.clear();
            intListPool.offer(builder);
        }
    }
    
    /**
     * Borrows a double list builder from the pool.
     */
    public DoubleListBuilder borrowDoubleList() {
        DoubleListBuilder builder = doubleListPool.poll();
        if (builder == null) {
            return new DoubleListBuilder();
        }
        builder.clear();
        return builder;
    }
    
    /**
     * Returns a double list builder to the pool.
     */
    public void returnDoubleList(DoubleListBuilder builder) {
        if (builder != null && doubleListPool.size() < maxPoolSize) {
            builder.clear();
            doubleListPool.offer(builder);
        }
    }
    
    /**
     * Gets pool statistics.
     */
    public PoolStatistics getStatistics() {
        return new PoolStatistics(
            arrayPoolHits.get(),
            arrayPoolMisses.get(),
            arrayPool.size(),
            signalBuilderPool.size(),
            intListPool.size(),
            doubleListPool.size()
        );
    }
    
    /**
     * Holder for reusable arrays.
     */
    public static final class ArrayHolder implements AutoCloseable {
        public double[] array;
        private FinancialAnalysisObjectPool pool;
        
        ArrayHolder(int size) {
            this.array = new double[size];
        }
        
        void clear() {
            // Arrays.fill(array, 0.0); // Optional - only if needed for correctness
        }
        
        void setPool(FinancialAnalysisObjectPool pool) {
            this.pool = pool;
        }
        
        @Override
        public void close() {
            if (pool != null) {
                pool.returnArray(this);
            }
        }
    }
    
    /**
     * Efficient builder for trading signals using primitive lists.
     */
    public static final class TradingSignalBuilder {
        private IntListBuilder indices = new IntListBuilder();
        private IntListBuilder types = new IntListBuilder();
        private DoubleListBuilder confidences = new DoubleListBuilder();
        private java.util.List<String> reasons = new java.util.ArrayList<>();
        
        public void addSignal(int index, FinancialWaveletAnalyzer.SignalType type, 
                             double confidence, String reason) {
            indices.add(index);
            types.add(type.ordinal());
            confidences.add(confidence);
            reasons.add(reason);
        }
        
        public int size() {
            return indices.size();
        }
        
        public java.util.List<FinancialWaveletAnalyzer.TradingSignal> build() {
            java.util.List<FinancialWaveletAnalyzer.TradingSignal> signals = 
                new java.util.ArrayList<>(size());
            
            for (int i = 0; i < size(); i++) {
                signals.add(new FinancialWaveletAnalyzer.TradingSignal(
                    indices.get(i),
                    FinancialWaveletAnalyzer.SignalType.values()[types.get(i)],
                    confidences.get(i),
                    reasons.get(i)
                ));
            }
            
            return signals;
        }
        
        void clear() {
            indices.clear();
            types.clear();
            confidences.clear();
            reasons.clear();
        }
    }
    
    /**
     * Primitive list for int values to avoid boxing.
     */
    public static final class IntListBuilder {
        private int[] data;
        private int size;
        
        IntListBuilder() {
            this(16);
        }
        
        IntListBuilder(int capacity) {
            this.data = new int[capacity];
            this.size = 0;
        }
        
        public void add(int value) {
            ensureCapacity(size + 1);
            data[size++] = value;
        }
        
        public int get(int index) {
            if (index >= size) {
                throw new IndexOutOfBoundsException();
            }
            return data[index];
        }
        
        public int size() {
            return size;
        }
        
        public int[] toArray() {
            return java.util.Arrays.copyOf(data, size);
        }
        
        public java.util.List<Integer> toList() {
            java.util.List<Integer> list = new java.util.ArrayList<>(size);
            for (int i = 0; i < size; i++) {
                list.add(data[i]);
            }
            return list;
        }
        
        void clear() {
            size = 0;
        }
        
        private void ensureCapacity(int minCapacity) {
            if (minCapacity > data.length) {
                int newCapacity = Math.max(data.length * 2, minCapacity);
                data = java.util.Arrays.copyOf(data, newCapacity);
            }
        }
    }
    
    /**
     * Primitive list for double values to avoid boxing.
     */
    public static final class DoubleListBuilder {
        private double[] data;
        private int size;
        
        DoubleListBuilder() {
            this(16);
        }
        
        DoubleListBuilder(int capacity) {
            this.data = new double[capacity];
            this.size = 0;
        }
        
        public void add(double value) {
            ensureCapacity(size + 1);
            data[size++] = value;
        }
        
        public double get(int index) {
            if (index >= size) {
                throw new IndexOutOfBoundsException();
            }
            return data[index];
        }
        
        public int size() {
            return size;
        }
        
        public double[] toArray() {
            return java.util.Arrays.copyOf(data, size);
        }
        
        void clear() {
            size = 0;
        }
        
        private void ensureCapacity(int minCapacity) {
            if (minCapacity > data.length) {
                int newCapacity = Math.max(data.length * 2, minCapacity);
                data = java.util.Arrays.copyOf(data, newCapacity);
            }
        }
    }
    
    /**
     * Pool statistics for monitoring.
     *
     * @param arrayHits number of array allocations served from pool
     * @param arrayMisses number of array allocations requiring new allocation
     * @param arrayPoolSize current array pool size
     * @param signalPoolSize current signal pool size
     * @param intListPoolSize current int list pool size
     * @param doubleListPoolSize current double list pool size
     */
    public record PoolStatistics(
        int arrayHits,
        int arrayMisses,
        int arrayPoolSize,
        int signalPoolSize,
        int intListPoolSize,
        int doubleListPoolSize
    ) {
        public double hitRate() {
            int total = arrayHits + arrayMisses;
            return total == 0 ? 0.0 : (double) arrayHits / total;
        }
    }
}
