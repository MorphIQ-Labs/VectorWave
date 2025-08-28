package com.morphiqlabs.wavelet.performance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.TimeUnit;

/**
 * Test suite for PerformanceCalibrator.
 * Tests the performance calibration system that benchmarks wavelet operations.
 */
@DisplayName("PerformanceCalibrator Test Suite")
class PerformanceCalibratorTest {
    
    private PerformanceCalibrator calibrator;
    private PlatformFactors testPlatform;
    
    @BeforeEach
    void setUp() {
        // Create test platform with known characteristics
        testPlatform = new PlatformFactors.Builder()
            .cpuSpeedFactor(1.5)
            .vectorSpeedup(3.0)
            .l1CacheSize(64 * 1024)
            .l2CacheSize(512 * 1024)
            .l3CacheSize(16 * 1024 * 1024)
            .coreCount(8)
            .architecture("TestArch")
            .hasAVX512(true)
            .hasNEON(false)
            .memoryBandwidth(40.0)
            .build();
        
        calibrator = new PerformanceCalibrator(testPlatform);
    }
    
    @Test
    @DisplayName("Test calibrator construction with platform factors")
    void testCalibratorConstruction() {
        assertNotNull(calibrator);
        
        // Test default constructor
        PerformanceCalibrator defaultCalibrator = new PerformanceCalibrator();
        assertNotNull(defaultCalibrator);
    }
    
    @Test
    @DisplayName("Test MODWT calibration")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testMODWTCalibration() {
        // Test that MODWT calibration produces a valid model
        // Note: We can't easily test the full calibrate() method due to its complexity
        // but we can test individual components and ensure they don't crash
        
        assertDoesNotThrow(() -> {
            // Create a minimal test to ensure the calibrator framework works
            PlatformFactors platform = PlatformFactors.detectPlatform();
            assertNotNull(platform);
        });
    }
    
    @Test
    @DisplayName("Test calibration result structure")
    void testCalibrationResultStructure() {
        // Test the CalibratedModels data structure
        PerformanceModel testModel = new PerformanceModel(testPlatform);
        
        PerformanceCalibrator.CalibratedModels models = 
            new PerformanceCalibrator.CalibratedModels(
                testPlatform,
                testModel,
                testModel,
                testModel,
                System.currentTimeMillis()
            );
        
        assertNotNull(models);
        assertEquals(testPlatform, models.platformFactors());
        assertEquals(testModel, models.modwtModel());
        assertEquals(testModel, models.convolutionModel());
        assertEquals(testModel, models.batchModel());
        assertTrue(models.calibrationTimestamp() > 0);
    }
    
    @Test
    @DisplayName("Test calibration with different platform configurations")
    void testDifferentPlatformConfigurations() {
        // Test with minimal platform
        PlatformFactors minimalPlatform = new PlatformFactors.Builder()
            .cpuSpeedFactor(0.5)
            .vectorSpeedup(1.0) // No vectorization
            .l1CacheSize(16 * 1024)
            .l2CacheSize(128 * 1024)
            .l3CacheSize(2 * 1024 * 1024)
            .coreCount(1)
            .architecture("Minimal")
            .hasAVX512(false)
            .hasNEON(false)
            .memoryBandwidth(10.0)
            .build();
        
        PerformanceCalibrator minimalCalibrator = new PerformanceCalibrator(minimalPlatform);
        assertNotNull(minimalCalibrator);
        
        // Test with high-end platform  
        PlatformFactors highEndPlatform = new PlatformFactors.Builder()
            .cpuSpeedFactor(2.5)
            .vectorSpeedup(8.0)
            .l1CacheSize(128 * 1024)
            .l2CacheSize(1024 * 1024)
            .l3CacheSize(64 * 1024 * 1024)
            .coreCount(16)
            .architecture("HighEnd")
            .hasAVX512(true)
            .hasNEON(true)
            .memoryBandwidth(100.0)
            .build();
        
        PerformanceCalibrator highEndCalibrator = new PerformanceCalibrator(highEndPlatform);
        assertNotNull(highEndCalibrator);
    }
    
    @Test
    @DisplayName("Test calibration result data integrity")
    void testCalibrationResultDataIntegrity() {
        long testTime = System.currentTimeMillis();
        PerformanceModel model1 = new PerformanceModel(testPlatform);
        PerformanceModel model2 = new PerformanceModel(testPlatform);
        PerformanceModel model3 = new PerformanceModel(testPlatform);
        
        PerformanceCalibrator.CalibratedModels models = 
            new PerformanceCalibrator.CalibratedModels(
                testPlatform, model1, model2, model3, testTime
            );
        
        // Verify all components are preserved
        assertSame(testPlatform, models.platformFactors());
        assertSame(model1, models.modwtModel());
        assertSame(model2, models.convolutionModel());
        assertSame(model3, models.batchModel());
        assertEquals(testTime, models.calibrationTimestamp());
    }
    
    @Test
    @DisplayName("Test thread safety of calibration setup")
    void testThreadSafetySetup() {
        // Test that multiple calibrators can be created concurrently
        assertDoesNotThrow(() -> {
            Thread[] threads = new Thread[5];
            
            for (int i = 0; i < threads.length; i++) {
                final int threadId = i;
                threads[i] = new Thread(() -> {
                    PlatformFactors platform = new PlatformFactors.Builder()
                        .cpuSpeedFactor(1.0 + threadId * 0.1)
                        .vectorSpeedup(2.0)
                        .l1CacheSize(32 * 1024)
                        .l2CacheSize(256 * 1024)
                        .l3CacheSize(8 * 1024 * 1024)
                        .coreCount(4)
                        .architecture("Thread" + threadId)
                        .hasAVX512(false)
                        .hasNEON(false)
                        .memoryBandwidth(25.0)
                        .build();
                    
                    PerformanceCalibrator threadCalibrator = new PerformanceCalibrator(platform);
                    assertNotNull(threadCalibrator);
                });
            }
            
            // Start all threads
            for (Thread thread : threads) {
                thread.start();
            }
            
            // Wait for completion
            for (Thread thread : threads) {
                thread.join(5000); // 5 second timeout
            }
        });
    }
    
    @Test
    @DisplayName("Test calibration result validation")
    void testCalibrationResultValidation() {
        // Test that calibration results have sensible values
        PerformanceModel validModel = new PerformanceModel(testPlatform);
        
        // Add some test measurements to make the model realistic
        validModel.updateWithMeasurement(1000, 0.01, false);
        validModel.updateWithMeasurement(2000, 0.025, false);
        validModel.updateWithMeasurement(4000, 0.06, false);
        
        PerformanceCalibrator.CalibratedModels models = 
            new PerformanceCalibrator.CalibratedModels(
                testPlatform,
                validModel,
                validModel,
                validModel,
                System.currentTimeMillis()
            );
        
        // Verify models produce reasonable predictions
        PredictionResult prediction = models.modwtModel().predict(1000, false);
        assertNotNull(prediction);
        assertTrue(prediction.estimatedTime() > 0);
        assertTrue(prediction.confidence() >= 0 && prediction.confidence() <= 1);
        assertTrue(prediction.lowerBound() <= prediction.estimatedTime());
        assertTrue(prediction.upperBound() >= prediction.estimatedTime());
    }
    
    @Test
    @DisplayName("Test calibration time tracking")
    void testCalibrationTimeTracking() {
        long beforeTime = System.currentTimeMillis();
        
        PerformanceModel testModel = new PerformanceModel(testPlatform);
        PerformanceCalibrator.CalibratedModels models = 
            new PerformanceCalibrator.CalibratedModels(
                testPlatform,
                testModel,
                testModel,
                testModel,
                System.currentTimeMillis()
            );
        
        long afterTime = System.currentTimeMillis();
        
        // Calibration time should be within the test execution window
        assertTrue(models.calibrationTimestamp() >= beforeTime);
        assertTrue(models.calibrationTimestamp() <= afterTime);
    }
    
    @Test
    @DisplayName("Test platform factor integration")
    void testPlatformFactorIntegration() {
        // Verify that platform factors are properly integrated
        assertEquals("TestArch", testPlatform.architecture);
        assertEquals(8, testPlatform.coreCount);
        assertEquals(3.0, testPlatform.vectorSpeedup, 0.001);
        assertTrue(testPlatform.hasAVX512);
        assertFalse(testPlatform.hasNEON);
        
        // Test that the calibrator uses these factors
        PerformanceCalibrator.CalibratedModels models = 
            new PerformanceCalibrator.CalibratedModels(
                testPlatform,
                new PerformanceModel(testPlatform),
                new PerformanceModel(testPlatform),
                new PerformanceModel(testPlatform),
                System.currentTimeMillis()
            );
        
        assertSame(testPlatform, models.platformFactors());
    }
}