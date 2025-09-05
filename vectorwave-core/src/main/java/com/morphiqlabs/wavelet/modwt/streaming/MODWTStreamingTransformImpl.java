package com.morphiqlabs.wavelet.modwt.streaming;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidSignalException;
import com.morphiqlabs.wavelet.exception.InvalidStateException;
import com.morphiqlabs.wavelet.modwt.MODWTResult;
import com.morphiqlabs.wavelet.modwt.MODWTTransform;
import com.morphiqlabs.wavelet.util.ValidationUtils;

import java.util.Arrays;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Implementation of streaming MODWT transform using circular buffering.
 *
 * <p>This implementation uses a circular buffer strategy that takes advantage
 * of MODWT's shift-invariant properties for seamless block processing.</p>
 *
 * <p><strong>Sliding Window Overlap Strategy:</strong></p>
 * <p>To ensure continuity across transform blocks, this implementation uses a sliding
 * window approach with overlap. The overlap size is typically set to the filter length
 * minus 1, which guarantees that boundary effects from one block don't affect the next.</p>
 * 
 * <p>For example, with a buffer size of 1024 and overlap of 256:</p>
 * <ul>
 *   <li>Each transform processes 1024 samples</li>
 *   <li>The last 256 samples are retained for the next window</li>
 *   <li>Only 768 new samples are consumed per transform</li>
 *   <li>This ensures filter state continuity across blocks</li>
 * </ul>
 *
 * <p>Key features:</p>
 * <ul>
 *   <li>Circular buffer for continuous processing</li>
 *   <li>Overlap handling for filter continuity</li>
 *   <li>Thread-safe operation</li>
 *   <li>Flexible buffer size (not limited to powers of 2)</li>
 * </ul>
 */
class MODWTStreamingTransformImpl extends SubmissionPublisher<MODWTResult>
        implements MODWTStreamingTransform {

    private final Wavelet wavelet;
    private final BoundaryMode boundaryMode;
    private final int bufferSize;
    private final MODWTTransform transform;
    
    // Circular buffer for streaming
    // Note: The circular buffer inherently maintains overlap samples by only consuming
    // (bufferSize - overlapSize) samples per transform, leaving the overlap samples
    // in place for the next window. No separate overlap array is needed.
    private final double[] circularBuffer;
    private int writePosition = 0;
    private int samplesInBuffer = 0;
    
    // Overlap handling for filter continuity
    private final int filterLength;
    private final int overlapSize;
    
    // State management
    private final AtomicBoolean isClosed = new AtomicBoolean(false);
    
    // Statistics
    private final StreamingStatisticsImpl statistics = new StreamingStatisticsImpl();

    /**
     * Creates a new streaming MODWT transform.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary mode
     * @param bufferSize   the buffer size (must be positive)
     * @throws InvalidArgumentException if parameters are invalid
     */
    public MODWTStreamingTransformImpl(Wavelet wavelet, BoundaryMode boundaryMode, int bufferSize) {
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

        // Get filter length for overlap calculation
        this.filterLength = wavelet.lowPassDecomposition().length;
        this.overlapSize = filterLength - 1;
        
        // Validate against integer overflow and unreasonable sizes
        if (bufferSize > Integer.MAX_VALUE - overlapSize) {
            throw new InvalidArgumentException(
                "Buffer size too large, would cause integer overflow: bufferSize=" + bufferSize +
                ", overlapSize=" + overlapSize);
        }
        
        // Check for reasonable memory allocation (e.g., max 100MB for doubles)
        long totalSize = (long) bufferSize + overlapSize;
        long memorySizeBytes = totalSize * 8; // 8 bytes per double
        long maxReasonableSize = 100L * 1024 * 1024; // 100MB
        
        if (memorySizeBytes > maxReasonableSize) {
            throw new InvalidArgumentException(
                "Buffer size too large, would require " + (memorySizeBytes / (1024 * 1024)) + "MB. " +
                "Maximum allowed is " + (maxReasonableSize / (1024 * 1024)) + "MB");
        }
        
        // Ensure buffer size is larger than filter length for meaningful processing
        if (bufferSize < filterLength) {
            throw new InvalidArgumentException(
                "Buffer size must be at least as large as filter length: bufferSize=" + bufferSize +
                ", filterLength=" + filterLength);
        }
        
        // Additional defensive check: ensure the sliding window overlap mechanism works correctly
        // We need bufferSize > overlapSize to consume at least 1 new sample per window
        // Note: This is technically redundant since overlapSize = filterLength - 1 and we already
        // checked bufferSize >= filterLength, but we keep it for defensive programming and clarity
        if (bufferSize <= overlapSize) {
            throw new InvalidArgumentException(
                "Buffer size must be larger than overlap size for sliding window to work: " +
                "bufferSize=" + bufferSize + ", overlapSize=" + overlapSize + 
                " (filterLength=" + filterLength + ")");
        }

        this.wavelet = wavelet;
        this.boundaryMode = boundaryMode;
        this.bufferSize = bufferSize;
        
        // Initialize circular buffer with extra space for overlap
        this.circularBuffer = new double[bufferSize + overlapSize];
        
        // Create MODWT transform
        this.transform = new MODWTTransform(wavelet, boundaryMode);
    }

    @Override
    public synchronized void process(double[] data) {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }
        if (data == null || data.length == 0) {
            throw new InvalidSignalException("Data cannot be null or empty");
        }

        // Process data sample by sample
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
        // Add sample to circular buffer
        circularBuffer[writePosition] = sample;
        writePosition = (writePosition + 1) % circularBuffer.length;
        samplesInBuffer++;
        
        // Check if we have enough samples to process
        if (samplesInBuffer >= bufferSize) {
            processBuffer();
        }
        
        statistics.incrementSamplesProcessed();
    }

    private void processBuffer() {
        long startTime = System.nanoTime();
        
        // Extract buffer contents in correct order
        double[] processingBuffer = new double[bufferSize];
        int readPos = (writePosition - bufferSize + circularBuffer.length) % circularBuffer.length;
        
        for (int i = 0; i < bufferSize; i++) {
            processingBuffer[i] = circularBuffer[(readPos + i) % circularBuffer.length];
        }
        
        // Apply MODWT transform
        MODWTResult result = transform.forward(processingBuffer);
        
        // Publish result to subscribers
        submit(result);
        
        // Update statistics
        long processingTime = System.nanoTime() - startTime;
        statistics.recordBlockProcessed(processingTime);
        
        // Sliding window overlap mechanism:
        // - We processed a full buffer of 'bufferSize' samples
        // - The last 'overlapSize' samples need to be included in the next window
        // - Therefore, we only consumed (bufferSize - overlapSize) new samples
        // 
        // Example with bufferSize=1024, overlapSize=256:
        // - Window 1: samples [0...1023]     (processes 1024 samples)
        // - Window 2: samples [768...1791]   (overlaps last 256 samples)
        // - Effective consumption: 1024 - 256 = 768 new samples per window
        //
        // The circular buffer naturally maintains the overlap samples without copying:
        // - Read position stays at the beginning of unprocessed samples
        // - The overlap samples remain in the buffer for the next window
        int samplesConsumed = bufferSize - overlapSize;
        
        // Defensive check - this should never happen with proper validation
        if (samplesConsumed <= 0 || samplesConsumed > samplesInBuffer) {
            throw new InvalidStateException(
                "Invalid sliding window state: samplesConsumed=" + samplesConsumed +
                ", samplesInBuffer=" + samplesInBuffer + 
                ", bufferSize=" + bufferSize + ", overlapSize=" + overlapSize);
        }
        
        samplesInBuffer -= samplesConsumed;
    }

    @Override
    public synchronized void flush() {
        if (isClosed.get()) {
            throw InvalidStateException.closed("Transform");
        }
        
        if (samplesInBuffer > 0) {
            // Process remaining samples with zero padding if needed
            double[] finalBuffer = new double[bufferSize];
            int readPos = (writePosition - samplesInBuffer + circularBuffer.length) % circularBuffer.length;
            
            for (int i = 0; i < samplesInBuffer; i++) {
                finalBuffer[i] = circularBuffer[(readPos + i) % circularBuffer.length];
            }
            
            // Zero pad the rest
            Arrays.fill(finalBuffer, samplesInBuffer, bufferSize, 0.0);
            
            // Apply MODWT transform
            MODWTResult result = transform.forward(finalBuffer);
            submit(result);
            
            // Reset buffer
            samplesInBuffer = 0;
            writePosition = 0;
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
        
        // Clear buffer
        Arrays.fill(circularBuffer, 0.0);
        writePosition = 0;
        samplesInBuffer = 0;
        
        // Reset statistics
        statistics.reset();
    }

    @Override
    public int getBufferLevel() {
        return samplesInBuffer;
    }

    @Override
    public boolean isClosed() {
        return isClosed.get();
    }

    @Override
    public synchronized void close() {
        // Atomically mark as closed to prevent concurrent execution
        if (isClosed.compareAndSet(false, true)) {
            // Process any remaining samples without checking closed state
            if (samplesInBuffer > 0) {
                // Process remaining samples with zero padding if needed
                double[] finalBuffer = new double[bufferSize];
                int readPos = (writePosition - samplesInBuffer + circularBuffer.length) % circularBuffer.length;
                
                for (int i = 0; i < samplesInBuffer; i++) {
                    finalBuffer[i] = circularBuffer[(readPos + i) % circularBuffer.length];
                }
                
                // Zero pad the rest
                Arrays.fill(finalBuffer, samplesInBuffer, bufferSize, 0.0);
                
                // Apply final transform
                MODWTResult result = transform.forward(finalBuffer);
                submit(result);
                
                samplesInBuffer = 0;
            }
            
            // Close the publisher
            super.close();
        }
    }

    /**
     * Implementation of streaming statistics.
     */
    private static class StreamingStatisticsImpl implements StreamingStatistics {
        private final AtomicLong samplesProcessed = new AtomicLong();
        private final AtomicLong blocksProcessed = new AtomicLong();
        private final LongAdder totalProcessingTime = new LongAdder();
        private final AtomicLong maxProcessingTime = new AtomicLong();
        private final AtomicLong minProcessingTime = new AtomicLong(Long.MAX_VALUE);
        private final AtomicLong startTime = new AtomicLong(System.nanoTime());

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