package com.morphiqlabs.wavelet.modwt.streaming;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidSignalException;
import com.morphiqlabs.wavelet.exception.InvalidStateException;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.util.ValidationUtils;

import java.util.Arrays;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Multi-level streaming MODWT transform implementation.
 *
 * <p>This implementation performs multi-level MODWT decomposition on streaming data,
 * providing hierarchical time-frequency analysis in real-time.</p>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Multi-resolution analysis of streaming data</li>
 *   <li>Maintains continuity across all decomposition levels</li>
 *   <li>Efficient cascaded filtering</li>
 *   <li>Flexible buffer management per level</li>
 * </ul>
 */
class MultiLevelMODWTStreamingTransform extends SubmissionPublisher<MODWTResult>
        implements MODWTStreamingTransform {

    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final int bufferSize;
    private final int levels;
    private final MultiLevelMODWTTransform multiLevelTransform;
    
    // Level-specific buffers and state
    private final double[][] levelBuffers;
    private final int[] levelPositions;
    private final int[] levelSamplesCount;
    
    // State management
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    
    // Statistics
    private final StreamingStatisticsImpl statistics = new StreamingStatisticsImpl();
    
    // Shared empty array to avoid unnecessary allocations
    private static final double[] EMPTY_ARRAY = new double[0];

    /**
     * Creates a new multi-level streaming MODWT transform.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary mode
     * @param bufferSize   the buffer size
     * @param levels       number of decomposition levels
     * @throws InvalidArgumentException if parameters are invalid
     */
    public MultiLevelMODWTStreamingTransform(Wavelet wavelet, BoundaryMode boundaryMode, 
                                           int bufferSize, int levels) {
        super();

        if (wavelet == null) {
            throw new InvalidArgumentException("Wavelet cannot be null");
        }
        if (boundaryMode == null) {
            throw new InvalidArgumentException("Boundary mode cannot be null");
        }
        if (bufferSize <= 0) {
            throw new InvalidArgumentException("Buffer size must be positive, got: " + bufferSize);
        }
        if (levels < 1) {
            throw new InvalidArgumentException("Levels must be at least 1, got: " + levels);
        }

        this.wavelet = wavelet;
        this.boundaryMode = boundaryMode;
        this.bufferSize = bufferSize;
        this.levels = levels;
        
        // Initialize level-specific buffers
        this.levelBuffers = new double[levels][];
        this.levelPositions = new int[levels];
        this.levelSamplesCount = new int[levels];
        
        for (int level = 0; level < levels; level++) {
            // Each level needs its own buffer
            levelBuffers[level] = new double[bufferSize];
        }
        
        // Create multi-level MODWT transform
        this.multiLevelTransform = new MultiLevelMODWTTransform(wavelet, boundaryMode);
    }

    @Override
    public synchronized void process(double[] data) {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }
        if (data == null || data.length == 0) {
            throw new InvalidSignalException("Data cannot be null or empty");
        }

        // Process data through the first level buffer
        for (double sample : data) {
            processSampleInternal(sample);
        }
    }

    @Override
    public synchronized void processSample(double sample) {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }
        
        processSampleInternal(sample);
    }

    private void processSampleInternal(double sample) {
        // Add sample to first level buffer
        levelBuffers[0][levelPositions[0]] = sample;
        levelPositions[0] = (levelPositions[0] + 1) % bufferSize;
        levelSamplesCount[0]++;
        
        // Check if first level buffer is full
        if (levelSamplesCount[0] >= bufferSize) {
            processMultiLevel();
        }
        
        statistics.incrementSamplesProcessed();
    }

    private void processMultiLevel() {
        long startTime = System.nanoTime();
        
        // Extract buffer contents for processing
        double[] processingBuffer = new double[bufferSize];
        int startPos = (levelPositions[0] - bufferSize + bufferSize) % bufferSize;
        
        for (int i = 0; i < bufferSize; i++) {
            processingBuffer[i] = levelBuffers[0][(startPos + i) % bufferSize];
        }
        
        // Apply multi-level MODWT transform
        MultiLevelMODWTResult multiResult = multiLevelTransform.decompose(processingBuffer, levels);
        
        // Convert to single-level results and publish each level
        for (int level = 1; level <= levels; level++) {
            double[] details = multiResult.getDetailCoeffsAtLevel(level);
            double[] approx = getApproximationForLevel(multiResult, level);
            
            MODWTResult levelResult = new MODWTResultWrapper(approx, details);
            submit(levelResult);
        }
        
        // Update statistics
        long processingTime = System.nanoTime() - startTime;
        statistics.recordBlockProcessed(processingTime);
        
        // Reset first level counter
        levelSamplesCount[0] = 0;
    }

    @Override
    public synchronized void flush() {
        if (levelSamplesCount[0] > 0) {
            // Process remaining samples with zero padding
            double[] finalBuffer = new double[bufferSize];
            int startPos = (levelPositions[0] - levelSamplesCount[0] + bufferSize) % bufferSize;
            
            for (int i = 0; i < levelSamplesCount[0]; i++) {
                finalBuffer[i] = levelBuffers[0][(startPos + i) % bufferSize];
            }
            
            // Apply multi-level transform
            MultiLevelMODWTResult multiResult = multiLevelTransform.decompose(finalBuffer, levels);
            
            // Publish results for each level
            for (int level = 1; level <= levels; level++) {
                double[] details = multiResult.getDetailCoeffsAtLevel(level);
                double[] approx = getApproximationForLevel(multiResult, level);
                
                MODWTResult levelResult = new MODWTResultWrapper(approx, details);
                submit(levelResult);
            }
            
            // Reset all levels
            Arrays.fill(levelSamplesCount, 0);
            Arrays.fill(levelPositions, 0);
        }
    }

    @Override
    public StreamingStatistics getStatistics() {
        return statistics;
    }

    @Override
    public synchronized void reset() {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }
        
        // Clear all buffers
        for (double[] buffer : levelBuffers) {
            Arrays.fill(buffer, 0.0);
        }
        Arrays.fill(levelPositions, 0);
        Arrays.fill(levelSamplesCount, 0);
        
        // Reset statistics
        statistics.reset();
    }

    @Override
    public int getBufferLevel() {
        return levelSamplesCount[0];
    }

    @Override
    public boolean isClosed() {
        return isClosed.get();
    }
    
    /**
     * Gets the appropriate approximation coefficients for a given level.
     * Only the final level contains actual approximation coefficients;
     * intermediate levels use an empty array.
     * 
     * @param multiResult the multi-level MODWT result
     * @param currentLevel the current level being processed
     * @return approximation coefficients for the final level, empty array otherwise
     */
    private double[] getApproximationForLevel(MultiLevelMODWTResult multiResult, int currentLevel) {
        return currentLevel == levels ? multiResult.getApproximationCoeffs() : EMPTY_ARRAY;
    }

    @Override
    public void close() {
        if (isClosed.compareAndSet(false, true)) {
            // Flush any remaining data
            flush();
            
            // Close the publisher
            super.close();
        }
    }

    /**
     * Wrapper class to convert arrays to MODWTResult.
     */
    private static class MODWTResultWrapper implements MODWTResult {
        private final double[] approx;
        private final double[] details;

        MODWTResultWrapper(double[] approx, double[] details) {
            this.approx = approx;
            this.details = details;
        }

        @Override
        public double[] approximationCoeffs() {
            return approx.clone();
        }

        @Override
        public double[] detailCoeffs() {
            return details.clone();
        }

        @Override
        public int getSignalLength() {
            return approx.length;
        }

        @Override
        public boolean isValid() {
            // Check if arrays are valid
            if (approx == null || details == null) {
                return false;
            }
            
            // Check for finite values in approximation coefficients
            for (double value : approx) {
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    return false;
                }
            }
            
            // Check for finite values in detail coefficients
            for (double value : details) {
                if (Double.isNaN(value) || Double.isInfinite(value)) {
                    return false;
                }
            }
            
            return true;
        }
    }
    
    /**
     * Implementation of streaming statistics.
     */
    private static class StreamingStatisticsImpl implements MODWTStreamingTransform.StreamingStatistics {
        private final java.util.concurrent.atomic.AtomicLong samplesProcessed = new java.util.concurrent.atomic.AtomicLong();
        private final java.util.concurrent.atomic.AtomicLong blocksProcessed = new java.util.concurrent.atomic.AtomicLong();
        private final java.util.concurrent.atomic.LongAdder totalProcessingTime = new java.util.concurrent.atomic.LongAdder();
        private final java.util.concurrent.atomic.AtomicLong maxProcessingTime = new java.util.concurrent.atomic.AtomicLong();
        private final java.util.concurrent.atomic.AtomicLong minProcessingTime = new java.util.concurrent.atomic.AtomicLong(Long.MAX_VALUE);
        private final java.util.concurrent.atomic.AtomicLong startTime = new java.util.concurrent.atomic.AtomicLong(System.nanoTime());

        void incrementSamplesProcessed() {
            samplesProcessed.incrementAndGet();
        }

        void recordBlockProcessed(long processingTimeNanos) {
            blocksProcessed.incrementAndGet();
            totalProcessingTime.add(processingTimeNanos);
            
            // Update max
            long currentMax = maxProcessingTime.get();
            while (processingTimeNanos > currentMax) {
                if (maxProcessingTime.compareAndSet(currentMax, processingTimeNanos)) {
                    break;
                }
                currentMax = maxProcessingTime.get();
            }
            
            // Update min
            long currentMin = minProcessingTime.get();
            while (processingTimeNanos < currentMin) {
                if (minProcessingTime.compareAndSet(currentMin, processingTimeNanos)) {
                    break;
                }
                currentMin = minProcessingTime.get();
            }
        }

        @Override
        public long getSamplesProcessed() {
            return samplesProcessed.get();
        }

        @Override
        public long getBlocksProcessed() {
            return blocksProcessed.get();
        }

        @Override
        public long getAverageProcessingTimeNanos() {
            long blocks = blocksProcessed.get();
            return blocks > 0 ? totalProcessingTime.sum() / blocks : 0;
        }

        @Override
        public long getMaxProcessingTimeNanos() {
            return maxProcessingTime.get();
        }

        @Override
        public long getMinProcessingTimeNanos() {
            long min = minProcessingTime.get();
            return min == Long.MAX_VALUE ? 0 : min;
        }

        @Override
        public double getThroughputSamplesPerSecond() {
            long elapsedNanos = System.nanoTime() - startTime.get();
            double elapsedSeconds = elapsedNanos / 1_000_000_000.0;
            return elapsedSeconds > 0 ? samplesProcessed.get() / elapsedSeconds : 0;
        }

        @Override
        public void reset() {
            samplesProcessed.set(0);
            blocksProcessed.set(0);
            totalProcessingTime.reset();
            maxProcessingTime.set(0);
            minProcessingTime.set(Long.MAX_VALUE);
            startTime.set(System.nanoTime());
        }
    }
}