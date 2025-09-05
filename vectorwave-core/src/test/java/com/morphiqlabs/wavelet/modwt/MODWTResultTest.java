package com.morphiqlabs.wavelet.modwt;

import com.morphiqlabs.wavelet.exception.InvalidSignalException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for MODWTResult interface using the public API.
 */
class MODWTResultTest {

    @Test
    void testMODWTResultCreation() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.5, 1.5, 2.5, 3.5};
        
        MODWTResult result = MODWTResult.create(approx, detail);
        
        assertEquals(4, result.getSignalLength());
        assertArrayEquals(approx, result.approximationCoeffs());
        assertArrayEquals(detail, result.detailCoeffs());
        assertTrue(result.isValid());
    }

    @Test
    void testMODWTResultDefensiveCopies() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.5, 1.5, 2.5, 3.5};
        
        MODWTResult result = MODWTResult.create(approx, detail);
        
        // Modify original arrays
        approx[0] = 999.0;
        detail[0] = 888.0;
        
        // Result should be unchanged
        assertEquals(1.0, result.approximationCoeffs()[0]);
        assertEquals(0.5, result.detailCoeffs()[0]);
        
        // Modify returned arrays
        double[] returnedApprox = result.approximationCoeffs();
        double[] returnedDetail = result.detailCoeffs();
        returnedApprox[0] = 777.0;
        returnedDetail[0] = 666.0;
        
        // Result should still be unchanged
        assertEquals(1.0, result.approximationCoeffs()[0]);
        assertEquals(0.5, result.detailCoeffs()[0]);
    }

    @Test
    void testMODWTResultValidation() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.5, 1.5, 2.5, 3.5};
        
        // Valid construction
        assertDoesNotThrow(() -> MODWTResult.create(approx, detail));
        
        // Null approximation
        assertThrows(NullPointerException.class, 
            () -> MODWTResult.create(null, detail));
        
        // Null detail
        assertThrows(NullPointerException.class, 
            () -> MODWTResult.create(approx, null));
        
        // Mismatched lengths
        double[] shortDetail = {0.5, 1.5};
        assertThrows(IllegalArgumentException.class, 
            () -> MODWTResult.create(approx, shortDetail));
        
        // Empty arrays
        assertThrows(IllegalArgumentException.class, 
            () -> MODWTResult.create(new double[0], new double[0]));
        
        // NaN values
        double[] nanApprox = {1.0, Double.NaN, 3.0, 4.0};
        assertThrows(InvalidSignalException.class, 
            () -> MODWTResult.create(nanApprox, detail));
        
        // Infinite values
        double[] infDetail = {0.5, Double.POSITIVE_INFINITY, 2.5, 3.5};
        assertThrows(InvalidSignalException.class, 
            () -> MODWTResult.create(approx, infDetail));
    }

    @Test
    void testMODWTResultEqualsAndHashCode() {
        double[] approx1 = {1.0, 2.0, 3.0, 4.0};
        double[] detail1 = {0.5, 1.5, 2.5, 3.5};
        double[] approx2 = {1.0, 2.0, 3.0, 4.0};
        double[] detail2 = {0.5, 1.5, 2.5, 3.5};
        double[] approx3 = {1.0, 2.0, 3.0, 5.0}; // Different value
        
        MODWTResult result1 = MODWTResult.create(approx1, detail1);
        MODWTResult result2 = MODWTResult.create(approx2, detail2);
        MODWTResult result3 = MODWTResult.create(approx3, detail1);
        
        // Test equality
        assertEquals(result1, result2);
        assertNotEquals(result1, result3);
        assertEquals(result1, result1); // reflexive
        
        // Test hash code consistency
        assertEquals(result1.hashCode(), result2.hashCode());
        
        // Test equals with null and different class
        assertNotEquals(result1, null);
        assertNotEquals(result1, "not a MODWTResult");
    }

    @Test
    void testMODWTResultToString() {
        double[] approx = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0};
        double[] detail = {0.5, 1.5, 2.5, 3.5, 4.5, 5.5};
        
        MODWTResult result = MODWTResult.create(approx, detail);
        String str = result.toString();
        
        assertNotNull(str);
        assertTrue(str.contains("signalLength=6"));
        assertTrue(str.contains("MODWTResult"));
        // Should only show first 5 elements due to truncation
        assertTrue(str.contains("1.0") && str.contains("2.0"));
    }

    @Test
    void testIsValidMethod() {
        double[] approx = {1.0, 2.0, 3.0, 4.0};
        double[] detail = {0.5, 1.5, 2.5, 3.5};
        
        MODWTResult validResult = MODWTResult.create(approx, detail);
        assertTrue(validResult.isValid());
        
        // The factory method ensures all created results are valid
        // For now, just verify that the valid result passes the validation
        MODWTResult result = MODWTResult.create(approx, detail);
        assertTrue(result.isValid());
    }
    
    @Test
    void testIsValidWithEdgeCases() {
        // Test the isValid() method edge cases using a custom implementation
        // This is needed because the factory method validates input
        
        // Test 1: Null approximation coefficients
        MODWTResult nullApproxResult = new MODWTResult() {
            @Override
            public double[] approximationCoeffs() { return null; }
            @Override
            public double[] detailCoeffs() { return new double[]{1.0}; }
            @Override
            public int getSignalLength() { return 1; }
        };
        assertFalse(nullApproxResult.isValid());
        
        // Test 2: Null detail coefficients
        MODWTResult nullDetailResult = new MODWTResult() {
            @Override
            public double[] approximationCoeffs() { return new double[]{1.0}; }
            @Override
            public double[] detailCoeffs() { return null; }
            @Override
            public int getSignalLength() { return 1; }
        };
        assertFalse(nullDetailResult.isValid());
        
        // Test 3: Mismatched lengths
        MODWTResult mismatchedLengthResult = new MODWTResult() {
            @Override
            public double[] approximationCoeffs() { return new double[]{1.0, 2.0}; }
            @Override
            public double[] detailCoeffs() { return new double[]{1.0}; }
            @Override
            public int getSignalLength() { return 2; }
        };
        assertFalse(mismatchedLengthResult.isValid());
        
        // Test 4: Signal length doesn't match arrays
        MODWTResult wrongSignalLengthResult = new MODWTResult() {
            @Override
            public double[] approximationCoeffs() { return new double[]{1.0, 2.0}; }
            @Override
            public double[] detailCoeffs() { return new double[]{1.0, 2.0}; }
            @Override
            public int getSignalLength() { return 3; }
        };
        assertFalse(wrongSignalLengthResult.isValid());
        
        // Test 5: NaN in approximation coefficients
        MODWTResult nanApproxResult = new MODWTResult() {
            @Override
            public double[] approximationCoeffs() { return new double[]{1.0, Double.NaN}; }
            @Override
            public double[] detailCoeffs() { return new double[]{1.0, 2.0}; }
            @Override
            public int getSignalLength() { return 2; }
        };
        assertFalse(nanApproxResult.isValid());
        
        // Test 6: Infinity in detail coefficients  
        MODWTResult infDetailResult = new MODWTResult() {
            @Override
            public double[] approximationCoeffs() { return new double[]{1.0, 2.0}; }
            @Override
            public double[] detailCoeffs() { return new double[]{1.0, Double.POSITIVE_INFINITY}; }
            @Override
            public int getSignalLength() { return 2; }
        };
        assertFalse(infDetailResult.isValid());
        
        // Test 7: Valid result through custom implementation
        MODWTResult validCustomResult = new MODWTResult() {
            @Override
            public double[] approximationCoeffs() { return new double[]{1.0, 2.0, 3.0}; }
            @Override
            public double[] detailCoeffs() { return new double[]{0.5, 1.5, 2.5}; }
            @Override
            public int getSignalLength() { return 3; }
        };
        assertTrue(validCustomResult.isValid());
    }
}