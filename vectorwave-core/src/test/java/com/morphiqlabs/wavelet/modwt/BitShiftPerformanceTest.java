package com.morphiqlabs.wavelet.modwt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

/**
 * Demonstrates performance improvement of bit shifting vs Math.pow for powers of 2.
 */
class BitShiftPerformanceTest {
    
    @Test
    @Disabled("Performance test - run manually")
    void compareBitShiftVsMathPow() {
        int iterations = 10_000_000;
        
        // Warm up
        for (int i = 0; i < 1000; i++) {
            int a = 1 << 9;
            int b = (int) Math.pow(2, 9);
        }
        
        // Test bit shift
        long startBitShift = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (int level = 1; level <= 10; level++) {
                int upFactor = 1 << (level - 1);
            }
        }
        long bitShiftTime = System.nanoTime() - startBitShift;
        
        // Test Math.pow
        long startMathPow = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            for (int level = 1; level <= 10; level++) {
                int upFactor = (int) Math.pow(2, level - 1);
            }
        }
        long mathPowTime = System.nanoTime() - startMathPow;
        
        System.out.println("Bit shift time: " + bitShiftTime / 1_000_000 + " ms");
        System.out.println("Math.pow time: " + mathPowTime / 1_000_000 + " ms");
        System.out.println("Speedup: " + (double) mathPowTime / bitShiftTime + "x");
        
        // Bit shifting should be significantly faster
        assert bitShiftTime < mathPowTime;
    }
    
    @Test
    void verifyBitShiftCorrectness() {
        // Verify bit shift produces same results as Math.pow for our use case
        for (int level = 1; level <= 10; level++) {
            int bitShiftResult = 1 << (level - 1);
            int mathPowResult = (int) Math.pow(2, level - 1);
            
            assert bitShiftResult == mathPowResult : 
                "Mismatch at level " + level + ": " + bitShiftResult + " vs " + mathPowResult;
        }
        
        System.out.println("All bit shift results match Math.pow results for levels 1-10");
    }
}