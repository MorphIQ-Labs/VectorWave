package com.morphiqlabs.wavelet.modwt.streaming;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.exception.InvalidArgumentException;
import com.morphiqlabs.wavelet.exception.InvalidSignalException;
import com.morphiqlabs.wavelet.modwt.MODWTResult;

import java.util.concurrent.Flow;

/**
 * Streaming MODWT transform for processing continuous data streams.
 *
 * <p>This interface enables real-time MODWT analysis of streaming data without
 * requiring the entire signal to be available in memory. Key advantages over
 * streaming DWT:</p>
 * <ul>
 *   <li>No block size restrictions (works with any length)</li>
 *   <li>Shift-invariant processing</li>
 *   <li>Better continuity across block boundaries</li>
 *   <li>Maintains time alignment with input</li>
 * </ul>
 *
 * <p>Particularly useful for:</p>
 * <ul>
 *   <li>Live audio processing with precise timing</li>
 *   <li>Real-time financial data analysis</li>
 *   <li>Continuous sensor monitoring</li>
 *   <li>Edge detection in streaming video</li>
 * </ul>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * MODWTStreamingTransform transform = MODWTStreamingTransform.create(
 *     Daubechies.DB4,
 *     BoundaryMode.PERIODIC,
 *     256  // buffer size (can be any size, not just power of 2)
 * );
 *
 * // Subscribe to transform results
 * transform.subscribe(new Flow.Subscriber<MODWTResult>() {
 *     // Handle results...
 * });
 *
 * // Feed data into the transform
 * transform.process(dataChunk);
 * }</pre>
 * 
 * @since 1.0.0
 */
public interface MODWTStreamingTransform extends Flow.Publisher<MODWTResult>, AutoCloseable {
    
    /**
     * Closes this streaming transform and releases any resources.
     * This method does not throw InterruptedException.
     */
    @Override
    void close();

    /**
     * Create a streaming MODWT transform with default buffer size.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary handling mode
     * @return a new streaming MODWT transform
     */
    static MODWTStreamingTransform create(Wavelet wavelet, BoundaryMode boundaryMode) {
        return create(wavelet, boundaryMode, 256); // Default buffer size
    }

    /**
     * Create a streaming MODWT transform.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary handling mode
     * @param bufferSize   the processing buffer size (any positive value)
     * @return a new streaming MODWT transform
     * @throws InvalidArgumentException if bufferSize is not positive
     */
    static MODWTStreamingTransform create(Wavelet wavelet, BoundaryMode boundaryMode, int bufferSize) {
        return new MODWTStreamingTransformImpl(wavelet, boundaryMode, bufferSize);
    }

    /**
     * Create a multi-level streaming MODWT transform.
     *
     * @param wavelet      the wavelet to use
     * @param boundaryMode the boundary handling mode
     * @param bufferSize   the processing buffer size
     * @param levels       number of decomposition levels
     * @return a new multi-level streaming MODWT transform
     */
    static MODWTStreamingTransform createMultiLevel(
            Wavelet wavelet, BoundaryMode boundaryMode, int bufferSize, int levels) {
        return new MultiLevelMODWTStreamingTransform(wavelet, boundaryMode, bufferSize, levels);
    }

    /**
     * Process a chunk of streaming data.
     *
     * @param data the data chunk to process
     * @throws InvalidSignalException if data is null or empty
     * @throws IllegalStateException  if the transform is closed
     */
    void process(double[] data);

    /**
     * Process a single sample.
     *
     * @param sample the sample value
     * @throws IllegalStateException if the transform is closed
     */
    void processSample(double sample);

    /**
     * Flush any buffered data and emit final results.
     * Call this when the stream ends to process remaining samples.
     *
     * @throws IllegalStateException if the transform is closed
     */
    void flush();

    /**
     * Get current streaming statistics.
     *
     * @return statistics about the streaming performance
     */
    StreamingStatistics getStatistics();

    /**
     * Reset the transform state, clearing buffers and statistics.
     *
     * @throws IllegalStateException if the transform is closed
     */
    void reset();

    /**
     * Get the current buffer fill level.
     *
     * @return number of samples currently buffered
     */
    int getBufferLevel();

    /**
     * Check if the transform is closed.
     *
     * @return true if closed, false otherwise
     */
    boolean isClosed();

    /**
     * Streaming statistics for monitoring performance.
     */
    interface StreamingStatistics {
        /**
         * Total number of samples processed.
         */
        long getSamplesProcessed();

        /**
         * Total number of blocks processed.
         */
        long getBlocksProcessed();

        /**
         * Average processing time per block in nanoseconds.
         */
        long getAverageProcessingTimeNanos();

        /**
         * Maximum processing time for a single block.
         */
        long getMaxProcessingTimeNanos();

        /**
         * Minimum processing time for a single block.
         */
        long getMinProcessingTimeNanos();

        /**
         * Get throughput in samples per second.
         */
        double getThroughputSamplesPerSecond();

        /**
         * Reset all statistics.
         */
        void reset();
    }
}
