package com.morphiqlabs.wavelet.util;

/**
 * Utility class providing general-purpose mathematical algorithms.
 * 
 * <p>This class contains static methods for various mathematical operations
 * that are used throughout the wavelet library but are general enough to be
 * useful in other contexts.</p>
 */
public final class MathUtils {
    
    // Private constructor to prevent instantiation
    private MathUtils() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    /**
     * Applies symmetric boundary extension for a given index.
     * 
     * <p>This method implements whole-sample symmetry reflection used in wavelet transforms.
     * For a signal [a, b, c, d], the extension is: ... b a | a b c d | d c b a | a b c d | d c ...</p>
     * 
     * <p>The algorithm efficiently handles any index using modular arithmetic, avoiding
     * inefficient while loops for long filter supports.</p>
     * 
     * @param idx the index to reflect
     * @param signalLength the length of the signal
     * @return the reflected index within [0, signalLength)
     */
    public static int symmetricBoundaryExtension(int idx, int signalLength) {
        if (signalLength <= 0) {
            throw new IllegalArgumentException("Signal length must be positive");
        }
        
        // If index is already within bounds, return it
        if (idx >= 0 && idx < signalLength) {
            return idx;
        }
        
        // Efficient calculation using modular arithmetic
        // Map to positive index in the extended symmetric domain [0, 2*signalLength)
        int period = 2 * signalLength;
        idx = ((idx % period) + period) % period;
        
        // Map back to signal range with reflection
        if (idx >= signalLength) {
            idx = period - idx - 1;
        }
        
        return idx;
    }
    
    /**
     * Finds the kth smallest element in an array using the QuickSelect algorithm.
     * This is more efficient than full sorting when only a specific order statistic is needed.
     * 
     * <p>Time complexity:</p>
     * <ul>
     *   <li>Average case: O(n)</li>
     *   <li>Worst case: O(n²)</li>
     * </ul>
     * 
     * <p>The algorithm modifies the input array. If the original array must be preserved,
     * pass a copy of the array.</p>
     * 
     * @param arr the array to select from (will be modified)
     * @param k the index of the desired element (0-based)
     * @return the kth smallest element
     * @throws IllegalArgumentException if k is out of bounds or array is null/empty
     */
    public static double quickSelect(double[] arr, int k) {
        if (arr == null) {
            throw new IllegalArgumentException("Array cannot be null");
        }
        if (arr.length == 0) {
            throw new IllegalArgumentException("Array cannot be empty");
        }
        if (k < 0 || k >= arr.length) {
            throw new IllegalArgumentException(
                String.format("k=%d is out of bounds [0, %d)", k, arr.length));
        }
        
        return quickSelectInternal(arr, 0, arr.length - 1, k);
    }
    
    /**
     * Finds the median of an array using QuickSelect.
     * More efficient than sorting for just finding the median.
     * 
     * @param arr the array (will be modified)
     * @return the median value
     * @throws IllegalArgumentException if array is null or empty
     */
    public static double median(double[] arr) {
        if (arr == null || arr.length == 0) {
            throw new IllegalArgumentException("Array cannot be null or empty");
        }
        
        int n = arr.length;
        if (n % 2 == 1) {
            // Odd length: return middle element
            return quickSelect(arr, n / 2);
        } else {
            // Even length: return average of two middle elements
            // We need to find both (n/2 - 1) and (n/2) elements
            double[] copy = arr.clone();
            double lower = quickSelect(copy, n / 2 - 1);
            double upper = quickSelect(arr, n / 2);
            return (lower + upper) / 2.0;
        }
    }
    
    /**
     * Calculates the Median Absolute Deviation (MAD) of an array.
     * MAD is a robust measure of variability: median(|x_i - median(x)|)
     * 
     * @param values the input values
     * @return the median absolute deviation
     * @throws IllegalArgumentException if array is null or empty
     */
    public static double medianAbsoluteDeviation(double[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Array cannot be null or empty");
        }
        
        // First, find median (on a copy to preserve original)
        double[] work = values.clone();
        double median = median(work);
        
        // Calculate absolute deviations
        for (int i = 0; i < work.length; i++) {
            work[i] = Math.abs(values[i] - median);
        }
        
        // Return median of absolute deviations
        return median(work);
    }
    
    /**
     * Internal QuickSelect implementation with explicit bounds.
     * 
     * @param arr the array to select from (will be modified)
     * @param left the left boundary of the search range (inclusive)
     * @param right the right boundary of the search range (inclusive)
     * @param k the index of the desired element (0-based, must be within [left, right])
     * @return the kth smallest element in the range [left, right]
     */
    private static double quickSelectInternal(double[] arr, int left, int right, int k) {
        // Base case: single element
        if (left == right) {
            return arr[left];
        }
        
        // Choose pivot and partition
        int pivotIndex = partition(arr, left, right);
        
        // Recursively search the appropriate partition
        if (k == pivotIndex) {
            return arr[k];
        } else if (k < pivotIndex) {
            return quickSelectInternal(arr, left, pivotIndex - 1, k);
        } else {
            return quickSelectInternal(arr, pivotIndex + 1, right, k);
        }
    }
    
    /**
     * Partitions the array around a pivot element.
     * Uses the "median-of-three" strategy to choose a good pivot.
     * 
     * @param arr the array to partition
     * @param left the left boundary (inclusive)
     * @param right the right boundary (inclusive)
     * @return the final position of the pivot
     */
    private static int partition(double[] arr, int left, int right) {
        // Use median-of-three to choose pivot for better performance
        int mid = left + (right - left) / 2;
        
        // Sort left, middle, and right elements
        if (arr[left] > arr[mid]) {
            swap(arr, left, mid);
        }
        if (arr[mid] > arr[right]) {
            swap(arr, mid, right);
            if (arr[left] > arr[mid]) {
                swap(arr, left, mid);
            }
        }
        
        // Use middle element as pivot
        double pivot = arr[mid];
        
        // Move pivot to end
        swap(arr, mid, right);
        
        // Partition around pivot
        int storeIndex = left;
        for (int i = left; i < right; i++) {
            if (arr[i] < pivot) {
                swap(arr, storeIndex, i);
                storeIndex++;
            }
        }
        
        // Move pivot to its final position
        swap(arr, storeIndex, right);
        return storeIndex;
    }
    
    /**
     * Swaps two elements in an array.
     * 
     * @param arr the array
     * @param i first index
     * @param j second index
     */
    private static void swap(double[] arr, int i, int j) {
        if (i != j) {
            double temp = arr[i];
            arr[i] = arr[j];
            arr[j] = temp;
        }
    }
    
    /**
     * Computes the standard deviation of an array of values.
     * 
     * @param values the input values
     * @return the standard deviation
     * @throws IllegalArgumentException if array is null or has less than 2 elements
     */
    public static double standardDeviation(double[] values) {
        if (values == null) {
            throw new IllegalArgumentException("Array cannot be null");
        }
        if (values.length < 2) {
            throw new IllegalArgumentException("Need at least 2 values for standard deviation");
        }
        
        // Calculate mean
        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        double mean = sum / values.length;
        
        // Calculate variance
        double sumSquaredDiff = 0.0;
        for (double value : values) {
            double diff = value - mean;
            sumSquaredDiff += diff * diff;
        }
        
        // Return standard deviation
        return Math.sqrt(sumSquaredDiff / (values.length - 1));
    }
}