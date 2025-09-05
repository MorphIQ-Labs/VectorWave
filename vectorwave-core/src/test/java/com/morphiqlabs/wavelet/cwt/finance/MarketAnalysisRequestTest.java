package com.morphiqlabs.wavelet.cwt.finance;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test coverage for MarketAnalysisRequest and its builders.
 * Focus on getting AnalysisOptions.Builder from 69% to 70% coverage.
 */
public class MarketAnalysisRequestTest {
    
    @Test
    public void testAnalysisOptionsBuilder_AllMethods() {
        // Test all builder methods to ensure coverage
        MarketAnalysisRequest.AnalysisOptions options = 
            MarketAnalysisRequest.AnalysisOptions.builder()
                .detectCrashes(false)
                .analyzeVolatility(false)
                .findCycles(false)
                .generateSignals(false)
                .crashThreshold(15.0)
                .volatilityWindow(60.0)
                .maxCycles(10)
                .signalConfidence(0.95)
                .build();
        
        assertNotNull(options);
        assertFalse(options.detectCrashes());
        assertFalse(options.analyzeVolatility());
        assertFalse(options.findCycles());
        assertFalse(options.generateSignals());
        assertEquals(15.0, options.crashThreshold());
        assertEquals(60.0, options.volatilityWindow());
        assertEquals(10, options.maxCycles());
        assertEquals(0.95, options.signalConfidence());
    }
    
    @Test
    public void testAnalysisOptionsBuilder_DefaultValues() {
        // Test builder with default values
        MarketAnalysisRequest.AnalysisOptions options = 
            MarketAnalysisRequest.AnalysisOptions.builder().build();
        
        assertNotNull(options);
        assertTrue(options.detectCrashes());
        assertTrue(options.analyzeVolatility());
        assertTrue(options.findCycles());
        assertTrue(options.generateSignals());
        assertEquals(10.0, options.crashThreshold());
        assertEquals(30.0, options.volatilityWindow());
        assertEquals(5, options.maxCycles());
        assertEquals(0.8, options.signalConfidence());
    }
    
    @Test
    public void testAnalysisOptionsBuilder_PartialCustomization() {
        // Test builder with partial customization
        MarketAnalysisRequest.AnalysisOptions options = 
            MarketAnalysisRequest.AnalysisOptions.builder()
                .crashThreshold(20.0)
                .maxCycles(8)
                .build();
        
        assertNotNull(options);
        assertTrue(options.detectCrashes()); // default
        assertTrue(options.analyzeVolatility()); // default
        assertEquals(20.0, options.crashThreshold()); // custom
        assertEquals(8, options.maxCycles()); // custom
        assertEquals(0.8, options.signalConfidence()); // default
    }
    
    @Test
    public void testAnalysisOptions_Defaults() {
        MarketAnalysisRequest.AnalysisOptions options = 
            MarketAnalysisRequest.AnalysisOptions.defaults();
        
        assertNotNull(options);
        assertTrue(options.detectCrashes());
        assertTrue(options.analyzeVolatility());
        assertTrue(options.findCycles());
        assertTrue(options.generateSignals());
        assertEquals(10.0, options.crashThreshold());
        assertEquals(30.0, options.volatilityWindow());
        assertEquals(5, options.maxCycles());
        assertEquals(0.8, options.signalConfidence());
    }
    
    @Test
    public void testMarketAnalysisRequest_Builder() {
        double[] priceData = {100.0, 101.0, 99.0, 102.0};
        double[] volumeData = {1000, 1100, 900, 1200};
        
        MarketAnalysisRequest request = MarketAnalysisRequest.builder()
            .priceData(priceData)
            .volumeData(volumeData)
            .samplingRate(24.0) // hourly
            .options(MarketAnalysisRequest.AnalysisOptions.defaults())
            .build();
        
        assertNotNull(request);
        assertArrayEquals(priceData, request.priceData());
        assertArrayEquals(volumeData, request.volumeData());
        assertEquals(24.0, request.samplingRate());
        assertNotNull(request.options());
    }
    
    @Test
    public void testMarketAnalysisRequest_FactoryMethods() {
        double[] priceData = {100.0, 101.0, 99.0};
        double[] volumeData = {1000, 1100, 900};
        
        // Test with price data only
        MarketAnalysisRequest request1 = MarketAnalysisRequest.of(priceData, 1.0);
        assertNotNull(request1);
        assertArrayEquals(priceData, request1.priceData());
        assertNull(request1.volumeData());
        assertEquals(1.0, request1.samplingRate());
        assertNotNull(request1.options());
        
        // Test with price and volume data
        MarketAnalysisRequest request2 = MarketAnalysisRequest.of(priceData, volumeData, 2.0);
        assertNotNull(request2);
        assertArrayEquals(priceData, request2.priceData());
        assertArrayEquals(volumeData, request2.volumeData());
        assertEquals(2.0, request2.samplingRate());
        assertNotNull(request2.options());
    }
    
    @Test
    public void testAnalysisOptionsBuilder_ChainedCalls() {
        // Test method chaining works correctly
        MarketAnalysisRequest.AnalysisOptions.Builder builder = 
            MarketAnalysisRequest.AnalysisOptions.builder();
        
        MarketAnalysisRequest.AnalysisOptions options = builder
            .detectCrashes(true)
            .analyzeVolatility(true)
            .findCycles(true)
            .generateSignals(true)
            .crashThreshold(5.0)
            .volatilityWindow(15.0)
            .maxCycles(3)
            .signalConfidence(0.9)
            .build();
        
        assertNotNull(options);
        assertEquals(5.0, options.crashThreshold());
        assertEquals(15.0, options.volatilityWindow());
        assertEquals(3, options.maxCycles());
        assertEquals(0.9, options.signalConfidence());
    }
    
    @Test
    public void testMarketAnalysisRequestBuilder_ConfigureOptions() {
        double[] priceData = {100.0, 101.0, 99.0};
        
        MarketAnalysisRequest request = MarketAnalysisRequest.builder()
            .priceData(priceData)
            .configureOptions(opts -> opts
                .detectCrashes(false)
                .crashThreshold(25.0)
                .maxCycles(7))
            .build();
        
        assertNotNull(request);
        assertNotNull(request.options());
        assertFalse(request.options().detectCrashes());
        assertEquals(25.0, request.options().crashThreshold());
        assertEquals(7, request.options().maxCycles());
        // Other options should have defaults
        assertTrue(request.options().analyzeVolatility());
    }
    
    @Test
    public void testMarketAnalysisRequestBuilder_ValidationErrors() {
        // Test null price data
        assertThrows(IllegalStateException.class, () -> 
            MarketAnalysisRequest.builder().build()
        );
        
        // Test empty price data
        assertThrows(IllegalStateException.class, () -> 
            MarketAnalysisRequest.builder()
                .priceData(new double[0])
                .build()
        );
        
        // Test invalid sampling rate
        assertThrows(IllegalStateException.class, () -> 
            MarketAnalysisRequest.builder()
                .priceData(new double[]{100.0})
                .samplingRate(0)
                .build()
        );
        
        assertThrows(IllegalStateException.class, () -> 
            MarketAnalysisRequest.builder()
                .priceData(new double[]{100.0})
                .samplingRate(-1)
                .build()
        );
    }
    
    @Test
    public void testMarketAnalysisRequest_ConvenienceMethods() {
        double[] priceData = {100.0, 101.0};
        double[] volumeData = {1000, 1100};
        
        // Test with volume data
        MarketAnalysisRequest request1 = MarketAnalysisRequest.of(priceData, volumeData, 1.0);
        assertTrue(request1.hasVolumeData());
        assertTrue(request1.getVolumeData().isPresent());
        assertArrayEquals(volumeData, request1.getVolumeData().get());
        
        // Test without volume data
        MarketAnalysisRequest request2 = MarketAnalysisRequest.of(priceData, 1.0);
        assertFalse(request2.hasVolumeData());
        assertFalse(request2.getVolumeData().isPresent());
    }
}