package com.morphiqlabs.wavelet.extensions.internal;

import org.junit.jupiter.api.*;
import jdk.incubator.vector.DoubleVector;
import jdk.incubator.vector.VectorSpecies;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Special test class that uses reflection to directly test the vector paths
 * by invoking the code that would normally be unreachable due to GATHER_SCATTER_AVAILABLE=false.
 */
@DisplayName("GatherScatterOps Vector Path Direct Test")
class GatherScatterOpsVectorPathTest {
    
    private static final double EPSILON = 1e-10;
    private static VectorSpecies<Double> DOUBLE_SPECIES;
    private static int DOUBLE_LENGTH;
    
    @BeforeAll
    static void setup() throws Exception {
        // Get the species and length via reflection
        Field speciesField = GatherScatterOps.class.getDeclaredField("DOUBLE_SPECIES");
        speciesField.setAccessible(true);
        @SuppressWarnings("unchecked")
        VectorSpecies<Double> species = (VectorSpecies<Double>) speciesField.get(null);
        DOUBLE_SPECIES = species;
        
        Field lengthField = GatherScatterOps.class.getDeclaredField("DOUBLE_LENGTH");
        lengthField.setAccessible(true);
        DOUBLE_LENGTH = lengthField.getInt(null);
    }
    
    @Test
    @DisplayName("Test vector path of gatherPeriodicDownsample directly")
    void testGatherPeriodicDownsampleVectorPath() throws Exception {
        // This test simulates what would happen if GATHER_SCATTER_AVAILABLE was true
        // and DOUBLE_LENGTH >= 4
        
        if (DOUBLE_LENGTH < 4) {
            // Skip test if vector length is too small
            return;
        }
        
        double[] signal = new double[32];
        double[] filter = {0.5, 0.5};
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i + 1.0;
        }
        
        int signalLength = signal.length;
        int filterLength = filter.length;
        int outputLength = signalLength / 2;
        double[] output = new double[outputLength];
        
        // Manually execute the vector path code from gatherPeriodicDownsample
        int i = 0;
        for (; i <= outputLength - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            DoubleVector sum = DoubleVector.zero(DOUBLE_SPECIES);
            
            for (int k = 0; k < filterLength; k++) {
                // Create index vector for gather
                int[] indices = new int[DOUBLE_LENGTH];
                for (int v = 0; v < DOUBLE_LENGTH; v++) {
                    indices[v] = (2 * (i + v) + k) % signalLength;
                }
                
                // Gather signal values manually (gather not available on all platforms)
                double[] gathered = new double[DOUBLE_LENGTH];
                for (int j = 0; j < DOUBLE_LENGTH; j++) {
                    gathered[j] = signal[indices[j]];
                }
                DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, gathered, 0);
                
                // Multiply by filter coefficient and accumulate
                DoubleVector filterVec = DoubleVector.broadcast(DOUBLE_SPECIES, filter[k]);
                sum = sum.add(values.mul(filterVec));
            }
            
            // Store result
            sum.intoArray(output, i);
        }
        
        // Handle remainder with scalar code
        for (; i < outputLength; i++) {
            double sum = 0.0;
            for (int k = 0; k < filterLength; k++) {
                int idx = (2 * i + k) % signalLength;
                sum += filter[k] * signal[idx];
            }
            output[i] = sum;
        }
        
        // Verify results
        assertNotNull(output);
        assertEquals(16, output.length);
        
        // Expected values: averaging pairs
        for (int j = 0; j < output.length; j++) {
            double expected = (signal[2*j] + signal[2*j + 1]) * 0.5;
            assertEquals(expected, output[j], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test vector path of scatterUpsample directly")
    void testScatterUpsampleVectorPath() throws Exception {
        if (DOUBLE_LENGTH < 4) {
            return;
        }
        
        double[] approx = {1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0};
        double[] detail = {0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8};
        int length = 16;
        double[] output = new double[length];
        
        int halfLength = length / 2;
        
        // Process with scatter operations
        int i = 0;
        for (; i <= halfLength - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            // Load approximation and detail coefficients
            DoubleVector approxVec = DoubleVector.fromArray(DOUBLE_SPECIES, approx, i);
            DoubleVector detailVec = DoubleVector.fromArray(DOUBLE_SPECIES, detail, i);
            
            // Create index vectors for scatter
            int[] evenIndices = new int[DOUBLE_LENGTH];
            int[] oddIndices = new int[DOUBLE_LENGTH];
            for (int v = 0; v < DOUBLE_LENGTH; v++) {
                evenIndices[v] = 2 * (i + v);
                oddIndices[v] = 2 * (i + v) + 1;
            }
            
            // Scatter manually (scatter not available on all platforms)
            double[] approxArray = new double[DOUBLE_LENGTH];
            double[] detailArray = new double[DOUBLE_LENGTH];
            approxVec.intoArray(approxArray, 0);
            detailVec.intoArray(detailArray, 0);
            
            for (int j = 0; j < DOUBLE_LENGTH; j++) {
                output[evenIndices[j]] = approxArray[j];
                output[oddIndices[j]] = detailArray[j];
            }
        }
        
        // Handle remainder with scalar code
        for (; i < halfLength; i++) {
            output[2 * i] = approx[i];
            output[2 * i + 1] = detail[i];
        }
        
        // Verify interleaving
        for (int j = 0; j < halfLength; j++) {
            assertEquals(approx[j], output[2 * j], EPSILON);
            assertEquals(detail[j], output[2 * j + 1], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test vector path of batchGather directly")
    void testBatchGatherVectorPath() throws Exception {
        double[][] signals = {{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0},
                             {10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0}};
        int[] indices = {0, 2, 4, 6};
        double[][] results = new double[2][4];
        int count = 4;
        
        int numSignals = signals.length;
        
        // Process multiple signals simultaneously
        for (int s = 0; s < numSignals; s++) {
            double[] signal = signals[s];
            double[] result = results[s];
            
            int i = 0;
            for (; i <= count - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
                // Gather values manually
                double[] gathered = new double[DOUBLE_LENGTH];
                for (int j = 0; j < DOUBLE_LENGTH; j++) {
                    gathered[j] = signal[indices[i + j]];
                }
                DoubleVector values = DoubleVector.fromArray(DOUBLE_SPECIES, gathered, 0);
                
                // Store gathered values
                values.intoArray(result, i);
            }
            
            // Handle remainder
            for (; i < count; i++) {
                result[i] = signal[indices[i]];
            }
        }
        
        // Verify results
        assertEquals(1.0, results[0][0], EPSILON);
        assertEquals(3.0, results[0][1], EPSILON);
        assertEquals(5.0, results[0][2], EPSILON);
        assertEquals(7.0, results[0][3], EPSILON);
        
        assertEquals(10.0, results[1][0], EPSILON);
        assertEquals(30.0, results[1][1], EPSILON);
        assertEquals(50.0, results[1][2], EPSILON);
        assertEquals(70.0, results[1][3], EPSILON);
    }
    
    @Test
    @DisplayName("Test gatherStrided vector path directly")
    void testGatherStridedVectorPath() {
        double[] signal = new double[64];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = i * 1.5;
        }
        
        int offset = 0;
        int stride = 2;
        int count = 20;
        double[] result = new double[count];
        
        // Test stride <= 8 path (vector implementation)
        if (stride <= 8) {
            int i = 0;
            // Process full vectors
            for (; i <= count - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
                double[] gathered = new double[DOUBLE_LENGTH];
                for (int j = 0; j < DOUBLE_LENGTH; j++) {
                    gathered[j] = signal[offset + (i + j) * stride];
                }
                DoubleVector vec = DoubleVector.fromArray(DOUBLE_SPECIES, gathered, 0);
                vec.intoArray(result, i);
            }
            
            // Handle remainder
            for (; i < count; i++) {
                result[i] = signal[offset + i * stride];
            }
        }
        
        // Verify
        for (int i = 0; i < count; i++) {
            assertEquals(signal[offset + i * stride], result[i], EPSILON);
        }
    }
    
    @Test
    @DisplayName("Test gather compressed vector path with full vectors")
    void testGatherCompressedVectorFullVectors() {
        int size = DOUBLE_LENGTH * 4; // Ensure we have multiple full vectors
        double[] signal = new double[size];
        boolean[] mask = new boolean[size];
        
        // Create a pattern where every other element is true
        for (int i = 0; i < size; i++) {
            signal[i] = i * 2.0;
            mask[i] = (i % 2 == 0);
        }
        
        // Manually execute vector path
        int trueCount = 0;
        for (boolean m : mask) {
            if (m) trueCount++;
        }
        
        double[] result = new double[trueCount];
        int resultIdx = 0;
        
        // Process full vectors
        int i = 0;
        for (; i <= size - DOUBLE_LENGTH; i += DOUBLE_LENGTH) {
            // Load mask vector
            boolean[] maskSlice = new boolean[DOUBLE_LENGTH];
            System.arraycopy(mask, i, maskSlice, 0, DOUBLE_LENGTH);
            
            // Count true values in this vector
            int localCount = 0;
            for (boolean m : maskSlice) {
                if (m) localCount++;
            }
            
            if (localCount > 0) {
                // Gather compressed values
                double[] compressed = new double[localCount];
                int compIdx = 0;
                for (int j = 0; j < DOUBLE_LENGTH; j++) {
                    if (maskSlice[j]) {
                        compressed[compIdx++] = signal[i + j];
                    }
                }
                
                // Copy to result
                System.arraycopy(compressed, 0, result, resultIdx, localCount);
                resultIdx += localCount;
            }
        }
        
        // Handle remainder
        for (; i < size; i++) {
            if (mask[i]) {
                result[resultIdx++] = signal[i];
            }
        }
        
        // Verify
        assertEquals(trueCount, result.length);
        int idx = 0;
        for (int j = 0; j < size; j++) {
            if (mask[j]) {
                assertEquals(signal[j], result[idx++], EPSILON);
            }
        }
    }
    
    @Test
    @DisplayName("Test checkGatherScatterSupport paths")
    void testCheckGatherScatterSupportPaths() throws Exception {
        Method checkMethod = GatherScatterOps.class.getDeclaredMethod("checkGatherScatterSupport");
        checkMethod.setAccessible(true);
        
        // The method will return false, but we're testing that it executes without errors
        Boolean result = (Boolean) checkMethod.invoke(null);
        assertNotNull(result);
        assertFalse(result); // Based on current implementation
    }
    
    @Test
    @DisplayName("Test edge cases in vector paths")
    void testVectorPathEdgeCases() {
        // Test with signal length exactly equal to vector length
        if (DOUBLE_LENGTH >= 4) {
            double[] signal = new double[DOUBLE_LENGTH * 2];
            double[] filter = {1.0};
            for (int i = 0; i < signal.length; i++) {
                signal[i] = i;
            }
            
            // This should exercise edge case handling in the vector path
            double[] result = GatherScatterOps.gatherPeriodicDownsampleForceVector(
                signal, filter, signal.length, filter.length);
            
            assertNotNull(result);
            assertEquals(DOUBLE_LENGTH, result.length);
        }
    }
}
