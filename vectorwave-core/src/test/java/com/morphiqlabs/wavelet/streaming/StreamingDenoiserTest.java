package com.morphiqlabs.wavelet.streaming;

import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.denoising.WaveletDenoiser.ThresholdMethod;
import com.morphiqlabs.wavelet.modwt.streaming.MODWTStreamingTransform;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

/**
 * Test for streaming denoiser functionality.
 */
class StreamingDenoiserTest {
    
    @Test
    void testBasicStreamingDenoising() throws Exception {
        // Create configuration
        StreamingDenoiserConfig config = new StreamingDenoiserConfig.Builder()
            .wavelet(Daubechies.DB4)
            .blockSize(256)
            .overlapFactor(0.0)
            .thresholdMethod(ThresholdMethod.UNIVERSAL)
            .build();
        
        // Create denoiser
        try (StreamingDenoiserStrategy denoiser = StreamingDenoiserFactory.getInstance().create(config)) {
            // Collect results
            List<double[]> results = new CopyOnWriteArrayList<>();
            CountDownLatch latch = new CountDownLatch(1);
            
            denoiser.subscribe(new Flow.Subscriber<double[]>() {
                Flow.Subscription subscription;
                
                @Override
                public void onSubscribe(Flow.Subscription subscription) {
                    this.subscription = subscription;
                    subscription.request(Long.MAX_VALUE);
                }
                
                @Override
                public void onNext(double[] item) {
                    results.add(item);
                }
                
                @Override
                public void onError(Throwable throwable) {
                    throwable.printStackTrace();
                    latch.countDown();
                }
                
                @Override
                public void onComplete() {
                    latch.countDown();
                }
            });
            
            // Generate test signal with noise
            double[] signal = new double[256];
            for (int i = 0; i < signal.length; i++) {
                signal[i] = Math.sin(2 * Math.PI * i / 32) + 0.3 * Math.random();
            }
            
            // Process signal
            denoiser.process(signal);
            denoiser.flush();
            
            // Wait for completion
            assertTrue(latch.await(1, TimeUnit.SECONDS));
            
            // Verify results
            assertFalse(results.isEmpty());
            assertEquals(1, results.size());
            assertEquals(signal.length, results.get(0).length);
            
            // Get statistics
            MODWTStreamingTransform.StreamingStatistics stats = denoiser.getStatistics();
            assertTrue(stats.getSamplesProcessed() > 0);
        }
    }
    
    @Test
    void testFactoryImplementationSelection() {
        // Fast configuration
        StreamingDenoiserConfig fastConfig = new StreamingDenoiserConfig.Builder()
            .blockSize(128)
            .overlapFactor(0.5)
            .adaptiveThreshold(true)
            .build();
        
        try (StreamingDenoiserStrategy fast = StreamingDenoiserFactory.getInstance().create(fastConfig)) {
            StreamingDenoiserStrategy.PerformanceProfile profile = fast.getPerformanceProfile();
            assertTrue(profile.isRealTimeCapable());
            assertTrue(profile.expectedLatencyMicros() < 100);
        } catch (Exception e) {
            fail("Should create fast denoiser: " + e.getMessage());
        }
        
        // Quality configuration
        StreamingDenoiserConfig qualityConfig = new StreamingDenoiserConfig.Builder()
            .blockSize(512)
            .overlapFactor(0.0)
            .build();
        
        try (StreamingDenoiserStrategy quality = StreamingDenoiserFactory.getInstance().create(qualityConfig)) {
            StreamingDenoiserStrategy.PerformanceProfile profile = quality.getPerformanceProfile();
            assertTrue(profile.expectedSNRImprovement() > 8.0);
        } catch (Exception e) {
            fail("Should create quality denoiser: " + e.getMessage());
        }
    }
    
    @Test
    void testDefaultConfigurations() {
        // Test audio config
        StreamingDenoiserConfig audioConfig = StreamingDenoiserConfig.defaultAudioConfig();
        assertEquals(512, audioConfig.getBlockSize());
        assertEquals(0.5, audioConfig.getOverlapFactor());
        assertEquals(ThresholdMethod.UNIVERSAL, audioConfig.getThresholdMethod());
        assertTrue(audioConfig.isAdaptiveThreshold());
        
        // Test financial config
        StreamingDenoiserConfig financialConfig = StreamingDenoiserConfig.defaultFinancialConfig();
        assertEquals(256, financialConfig.getBlockSize());
        assertEquals(0.25, financialConfig.getOverlapFactor());
        assertEquals(ThresholdMethod.SURE, financialConfig.getThresholdMethod());
        assertEquals(0.8, financialConfig.getThresholdMultiplier());
    }
}
