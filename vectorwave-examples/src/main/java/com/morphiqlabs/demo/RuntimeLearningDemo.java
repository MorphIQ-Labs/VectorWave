package com.morphiqlabs.demo;

import com.morphiqlabs.wavelet.api.BoundaryMode;
import com.morphiqlabs.wavelet.api.Daubechies;
import com.morphiqlabs.wavelet.api.Wavelet;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult;
import com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform;
import com.morphiqlabs.wavelet.performance.AdaptivePerformanceEstimator;
import com.morphiqlabs.wavelet.WaveletOperations;
import com.morphiqlabs.wavelet.extensions.parallel.ParallelMultiLevelTransform;

import java.util.Random;

/**
 * Demonstrates runtime learning: query the AdaptivePerformanceEstimator to decide
 * between sequential and parallel multi‑level transforms at runtime based on input size
 * and current platform characteristics.
 *
 * To see learning in action, enable calibration (and optionally persistence):
 *   -Dvectorwave.perf.calibration=true [-Dvectorwave.perf.persist=true]
 *
 * For quiet logs, use the provided JUL config:
 *   -Djava.util.logging.config.file=../docs/examples/logging.properties
 */
public class RuntimeLearningDemo {

    public static void main(String[] args) {
        Wavelet wavelet = Daubechies.DB4;
        BoundaryMode mode = BoundaryMode.PERIODIC;
        AdaptivePerformanceEstimator est = AdaptivePerformanceEstimator.getInstance();

        int[] sizes = { 1024, 2048, 4096, 8192, 16384 };
        int levels = 4;

        System.out.println("=== Runtime Learning Demo ===\n");
        System.out.println("Wavelet: " + wavelet.name() + ", Boundary: " + mode);
        System.out.println("Calibration enabled: set -Dvectorwave.perf.calibration=true to learn\n");

        for (int n : sizes) {
            double[] x = generateSignal(n);
            boolean vectorized = WaveletOperations.getPerformanceInfo().vectorizationEnabled();

            var pred = est.estimateMODWT(n, wavelet.name(), vectorized);
            boolean useParallel = shouldUseParallel(pred, n, levels);

            String decision = useParallel ? "PARALLEL" : "SEQUENTIAL";
            System.out.printf("N=%d, predicted=%.3f ms (conf=%.2f) -> %s\n",
                    n, pred.estimatedTime(), pred.confidence(), decision);

            if (useParallel) {
                try (ParallelMultiLevelTransform t = new ParallelMultiLevelTransform(wavelet, mode)) {
                    timeAndVerify(t, x, levels);
                }
            } else {
                MultiLevelMODWTTransform t = new MultiLevelMODWTTransform(wavelet, mode);
                timeAndVerify(t, x, levels);
            }

            System.out.println();
        }
    }

    private static boolean shouldUseParallel(com.morphiqlabs.wavelet.performance.PredictionResult pred,
                                             int n, int levels) {
        // Simple heuristic: for larger signals or when predicted time exceeds a small budget
        // and the model is reasonably confident, choose parallel.
        double budgetMs = 0.5; // illustrative; actual threshold depends on workload
        boolean sizeGate = (n >= 4096) && levels >= 3;
        boolean timeGate = pred.estimatedTime() >= budgetMs && pred.confidence() >= 0.5;
        return sizeGate || timeGate;
    }

    private static void timeAndVerify(MultiLevelMODWTTransform t, double[] x, int levels) {
        long start = System.nanoTime();
        MultiLevelMODWTResult r = t.decompose(x, levels);
        double[] y = t.reconstruct(r);
        long end = System.nanoTime();
        double ms = (end - start) / 1_000_000.0;
        double rmse = rmse(x, y);
        System.out.printf("  time=%.3f ms, rmse=%.2e\n", ms, rmse);
    }

    private static double[] generateSignal(int n) {
        Random rand = new Random(42 + n);
        double[] s = new double[n];
        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            s[i] = Math.sin(2 * Math.PI * 5 * t) + 0.5 * Math.cos(2 * Math.PI * 17 * t) + 0.1 * rand.nextGaussian();
        }
        return s;
    }

    private static double rmse(double[] a, double[] b) {
        double sum = 0.0; int n = a.length;
        for (int i = 0; i < n; i++) { double d = a[i] - b[i]; sum += d * d; }
        return Math.sqrt(sum / n);
    }
}
