package com.morphiqlabs.wavelet.swt;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for SWTResult to achieve full coverage.
 */
class SWTResultTest {

    private double[] testApproximation;
    private double[][] testDetails;
    private SWTResult swtResult;

    @BeforeEach
    void setUp() {
        testApproximation = new double[]{1.0, 2.0, 3.0, 4.0};
        testDetails = new double[][]{
            {0.5, 1.0, 1.5, 2.0}, // Level 0
            {0.1, 0.2, 0.3, 0.4}  // Level 1
        };
        swtResult = new SWTResult(testApproximation, testDetails, 2);
    }

    @Test
    @DisplayName("Should create SWT result with valid parameters")
    void testValidConstruction() {
        assertNotNull(swtResult);
        assertEquals(2, swtResult.getLevels());
        assertEquals(4, swtResult.getSignalLength());
    }

    @Test
    @DisplayName("Should throw exception for null approximation")
    void testNullApproximation() {
        assertThrows(NullPointerException.class, () -> {
            new SWTResult(null, testDetails, 2);
        });
    }

    @Test
    @DisplayName("Should throw exception for null details")
    void testNullDetails() {
        assertThrows(NullPointerException.class, () -> {
            new SWTResult(testApproximation, null, 2);
        });
    }

    @Test
    @DisplayName("Should throw exception for mismatched levels and details length")
    void testMismatchedLevels() {
        assertThrows(IllegalArgumentException.class, () -> {
            new SWTResult(testApproximation, testDetails, 3); // 3 levels but only 2 detail arrays
        });
    }

    @Test
    @DisplayName("Should throw exception for null detail level")
    void testNullDetailLevel() {
        double[][] badDetails = new double[][]{
            {0.5, 1.0, 1.5, 2.0},
            null  // Null level
        };
        
        assertThrows(IllegalArgumentException.class, () -> {
            new SWTResult(testApproximation, badDetails, 2);
        });
    }

    @Test
    @DisplayName("Should throw exception for mismatched detail level length")
    void testMismatchedDetailLength() {
        double[][] badDetails = new double[][]{
            {0.5, 1.0, 1.5, 2.0},
            {0.1, 0.2, 0.3}  // Wrong length
        };
        
        assertThrows(IllegalArgumentException.class, () -> {
            new SWTResult(testApproximation, badDetails, 2);
        });
    }

    @Test
    @DisplayName("Should return defensive copy of approximation")
    void testGetApproximation() {
        double[] approximation = swtResult.getApproximation();
        
        assertArrayEquals(testApproximation, approximation);
        
        // Modify returned array - should not affect original
        approximation[0] = 999.0;
        double[] newApproximation = swtResult.getApproximation();
        assertEquals(1.0, newApproximation[0]); // Should be unchanged
    }

    @Test
    @DisplayName("Should return defensive copy of detail level")
    void testGetDetail() {
        double[] detail0 = swtResult.getDetail(0);
        double[] detail1 = swtResult.getDetail(1);
        
        assertArrayEquals(testDetails[0], detail0);
        assertArrayEquals(testDetails[1], detail1);
        
        // Modify returned array - should not affect original
        detail0[0] = 999.0;
        double[] newDetail0 = swtResult.getDetail(0);
        assertEquals(0.5, newDetail0[0]); // Should be unchanged
    }

    @Test
    @DisplayName("Should throw exception for invalid detail level")
    void testGetDetailInvalidLevel() {
        assertThrows(IllegalArgumentException.class, () -> {
            swtResult.getDetail(-1);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            swtResult.getDetail(2); // Only levels 0 and 1 exist
        });
    }

    @Test
    @DisplayName("Should return defensive copy of all details")
    void testGetAllDetails() {
        double[][] allDetails = swtResult.getAllDetails();
        
        assertEquals(2, allDetails.length);
        assertArrayEquals(testDetails[0], allDetails[0]);
        assertArrayEquals(testDetails[1], allDetails[1]);
        
        // Modify returned array - should not affect original
        allDetails[0][0] = 999.0;
        double[][] newAllDetails = swtResult.getAllDetails();
        assertEquals(0.5, newAllDetails[0][0]); // Should be unchanged
    }

    @Test
    @DisplayName("Should compute detail energy correctly")
    void testGetDetailEnergy() {
        double energy0 = swtResult.getDetailEnergy(0);
        double energy1 = swtResult.getDetailEnergy(1);
        
        // Expected: 0.5^2 + 1.0^2 + 1.5^2 + 2.0^2 = 0.25 + 1.0 + 2.25 + 4.0 = 7.5
        assertEquals(7.5, energy0, 1e-10);
        
        // Expected: 0.1^2 + 0.2^2 + 0.3^2 + 0.4^2 = 0.01 + 0.04 + 0.09 + 0.16 = 0.3
        assertEquals(0.3, energy1, 1e-10);
    }

    @Test
    @DisplayName("Should throw exception for invalid detail energy level")
    void testGetDetailEnergyInvalidLevel() {
        assertThrows(IllegalArgumentException.class, () -> {
            swtResult.getDetailEnergy(-1);
        });
        
        assertThrows(IllegalArgumentException.class, () -> {
            swtResult.getDetailEnergy(2);
        });
    }

    @Test
    @DisplayName("Should cache detail energies for performance")
    void testDetailEnergyCaching() {
        double energy1 = swtResult.getDetailEnergy(0);
        double energy2 = swtResult.getDetailEnergy(0); // Should use cached value
        
        assertEquals(energy1, energy2);
    }

    @Test
    @DisplayName("Should compute approximation energy correctly")
    void testGetApproximationEnergy() {
        double energy = swtResult.getApproximationEnergy();
        
        // Expected: 1.0^2 + 2.0^2 + 3.0^2 + 4.0^2 = 1 + 4 + 9 + 16 = 30
        assertEquals(30.0, energy, 1e-10);
    }

    @Test
    @DisplayName("Should compute total energy correctly")
    void testGetTotalEnergy() {
        double totalEnergy = swtResult.getTotalEnergy();
        
        // Approximation: 30.0, Detail 0: 7.5, Detail 1: 0.3
        // Total: 30.0 + 7.5 + 0.3 = 37.8
        assertEquals(37.8, totalEnergy, 1e-10);
    }

    @Test
    @DisplayName("Should cache total energy for performance")
    void testTotalEnergyCaching() {
        double energy1 = swtResult.getTotalEnergy();
        double energy2 = swtResult.getTotalEnergy(); // Should use cached value
        
        assertEquals(energy1, energy2);
    }

    @Test
    @DisplayName("Should create sparse representation")
    void testToSparse() {
        SWTResult.SparseSWTResult sparse = swtResult.toSparse(0.5);
        
        assertNotNull(sparse);
        assertEquals(0.5, sparse.getThreshold());
    }

    @Test
    @DisplayName("Should reconstruct from sparse representation")
    void testSparseRoundTrip() {
        SWTResult.SparseSWTResult sparse = swtResult.toSparse(0.25);
        SWTResult reconstructed = sparse.toFull();
        
        assertNotNull(reconstructed);
        assertEquals(swtResult.getLevels(), reconstructed.getLevels());
        assertEquals(swtResult.getSignalLength(), reconstructed.getSignalLength());
        
        // Approximation should be identical
        assertArrayEquals(swtResult.getApproximation(), reconstructed.getApproximation());
        
        // Details should match where above threshold
        for (int level = 0; level < swtResult.getLevels(); level++) {
            double[] original = swtResult.getDetail(level);
            double[] restored = reconstructed.getDetail(level);
            
            for (int i = 0; i < original.length; i++) {
                if (Math.abs(original[i]) > 0.25) {
                    assertEquals(original[i], restored[i], 1e-10);
                } else {
                    assertEquals(0.0, restored[i], 1e-10);
                }
            }
        }
    }

    @Test
    @DisplayName("Should calculate compression ratio correctly")
    void testCompressionRatio() {
        SWTResult.SparseSWTResult sparse = swtResult.toSparse(1.0);
        double ratio = sparse.getCompressionRatio();
        
        assertTrue(ratio > 0);
        // With threshold 1.0, many coefficients should be zeroed out, giving good compression
        assertTrue(ratio >= 1.0);
    }

    @Test
    @DisplayName("Should handle high sparsification threshold")
    void testHighSparsificationThreshold() {
        SWTResult.SparseSWTResult sparse = swtResult.toSparse(10.0); // Very high threshold
        SWTResult reconstructed = sparse.toFull();
        
        // Most coefficients should be zero
        for (int level = 0; level < swtResult.getLevels(); level++) {
            double[] restored = reconstructed.getDetail(level);
            for (double coeff : restored) {
                assertTrue(coeff == 0.0 || Math.abs(coeff) > 10.0);
            }
        }
        
        // Should achieve high compression ratio
        assertTrue(sparse.getCompressionRatio() > 2.0);
    }

    @Test
    @DisplayName("Should handle low sparsification threshold")
    void testLowSparsificationThreshold() {
        SWTResult.SparseSWTResult sparse = swtResult.toSparse(0.01); // Very low threshold
        SWTResult reconstructed = sparse.toFull();
        
        // Should be very close to original
        assertEquals(swtResult.getTotalEnergy(), reconstructed.getTotalEnergy(), 1e-6);
        
        // Should achieve low compression ratio (most values kept)
        assertTrue(sparse.getCompressionRatio() < 2.0);
    }

    @Test
    @DisplayName("Should implement equals correctly")
    void testEquals() {
        SWTResult identical = new SWTResult(testApproximation.clone(), 
            new double[][]{testDetails[0].clone(), testDetails[1].clone()}, 2);
        SWTResult different = new SWTResult(new double[]{5.0, 6.0, 7.0, 8.0}, testDetails, 2);
        
        assertEquals(swtResult, swtResult); // Reflexive
        assertEquals(swtResult, identical); // Equal objects
        assertNotEquals(swtResult, different); // Different objects
        assertNotEquals(swtResult, null); // Null comparison
        assertNotEquals(swtResult, "not an SWTResult"); // Different type
    }

    @Test
    @DisplayName("Should implement hashCode correctly")
    void testHashCode() {
        SWTResult identical = new SWTResult(testApproximation.clone(), 
            new double[][]{testDetails[0].clone(), testDetails[1].clone()}, 2);
        
        assertEquals(swtResult.hashCode(), identical.hashCode());
    }

    @Test
    @DisplayName("Should implement toString correctly")
    void testToString() {
        String str = swtResult.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("SWTResult"));
        assertTrue(str.contains("levels=2"));
        assertTrue(str.contains("signalLength=4"));
        assertTrue(str.contains("totalEnergy="));
    }

    @Test
    @DisplayName("Should handle concurrent access to cached values")
    void testConcurrentCaching() throws InterruptedException {
        int numThreads = 4;
        Thread[] threads = new Thread[numThreads];
        double[] results = new double[numThreads];
        Exception[] exceptions = new Exception[numThreads];

        for (int i = 0; i < numThreads; i++) {
            final int threadIndex = i;
            threads[i] = new Thread(() -> {
                try {
                    // Access cached values concurrently
                    results[threadIndex] = swtResult.getTotalEnergy();
                } catch (Exception e) {
                    exceptions[threadIndex] = e;
                }
            });
        }

        for (Thread thread : threads) {
            thread.start();
        }

        for (Thread thread : threads) {
            thread.join();
        }

        // Verify no exceptions and all results are identical
        for (int i = 0; i < numThreads; i++) {
            assertNull(exceptions[i]);
            assertEquals(37.8, results[i], 1e-10);
        }
    }

    @Test
    @DisplayName("Should handle zero coefficients in sparse representation")
    void testSparseWithZeroCoefficients() {
        double[][] zeroDetails = new double[][]{
            {0.0, 0.0, 0.0, 0.0}, // All zeros
            {0.1, 0.0, 0.0, 0.4}  // Some zeros
        };
        
        SWTResult zeroResult = new SWTResult(testApproximation, zeroDetails, 2);
        SWTResult.SparseSWTResult sparse = zeroResult.toSparse(0.05);
        
        assertTrue(sparse.getCompressionRatio() > 1.0);
        
        SWTResult reconstructed = sparse.toFull();
        assertArrayEquals(new double[]{0.0, 0.0, 0.0, 0.0}, reconstructed.getDetail(0));
    }
}