package com.morphiqlabs.wavelet.extensions.modwt;

import com.morphiqlabs.wavelet.api.DiscreteWavelet;

/**
 * Public, user-friendly facade for high-throughput batch MODWT using SIMD and
 * Structure-of-Arrays (SoA) layout under the hood.
 *
 * <p>This API accepts conventional Array-of-Arrays (AoS) inputs
 * (i.e., {@code double[batch][length]}) and returns results in AoS form while
 * internally converting to SoA for optimal vectorized processing.</p>
 *
 * <p>Usage (single-level):</p>
 * <pre>{@code
 * double[][] signals = ...; // [batch][length]
 * var out = BatchMODWT.singleLevelAoS(wavelet, signals);
 * double[][] approx = out.approx();
 * double[][] detail = out.detail();
 * }</pre>
 *
 * <p>Usage (multi-level):</p>
 * <pre>{@code
 * double[][] signals = ...; // [batch][length]
 * int levels = 3;
 * var out = BatchMODWT.multiLevelAoS(wavelet, signals, levels);
 * double[][][] details = out.detailPerLevel(); // [levels][batch][length]
 * double[][] approxFinal = out.finalApprox();  // [batch][length]
 * }</pre>
 *
 * <p>Advanced: You can work directly with SoA using
 * {@link BatchSIMDMODWT#convertToSoA(double[][], double[])} and
 * {@link BatchSIMDMODWT#convertFromSoA(double[], double[][])} helpers.</p>
 */
public final class BatchMODWT {

    private BatchMODWT() {}

    /** Result container for single-level outputs.
     * <p>Arrays are AoS: {@code [batch][length]}.</p>
     *
     * @param approx AoS approximation coefficients [batch][length]
     * @param detail AoS detail coefficients [batch][length]
     */
    public record SingleLevelResult(double[][] approx, double[][] detail) {}

    /** Result container for multi-level outputs.
     * <p>Arrays are AoS: details {@code [levels][batch][length]}, final approx {@code [batch][length]}.</p>
     *
     * @param detailPerLevel per-level details [levels][batch][length]
     * @param finalApprox final approximation [batch][length]
     */
    public record MultiLevelResult(double[][][] detailPerLevel, double[][] finalApprox) {}

    /**
     * Runs a single-level MODWT for a batch of signals (AoS input/outputs).
     * Periodic convolution is used internally (circular boundary).
     *
     * @param wavelet discrete wavelet
     * @param signals AoS signals [batch][length]
     * @return AoS results: approximation and detail per signal
     */
    public static SingleLevelResult singleLevelAoS(DiscreteWavelet wavelet, double[][] signals) {
        validateAoS(signals);
        int batch = signals.length;
        int n = signals[0].length;

        double[] soaIn = new double[batch * n];
        double[] soaApprox = new double[batch * n];
        double[] soaDetail = new double[batch * n];

        BatchSIMDMODWT.convertToSoA(signals, soaIn);
        BatchSIMDMODWT.batchMODWTSoA(soaIn, soaApprox, soaDetail, wavelet, batch, n);

        double[][] approx = new double[batch][n];
        double[][] detail = new double[batch][n];
        BatchSIMDMODWT.convertFromSoA(soaApprox, approx);
        BatchSIMDMODWT.convertFromSoA(soaDetail, detail);
        return new SingleLevelResult(approx, detail);
    }

    /**
     * Runs a multi-level MODWT for a batch of signals (AoS input/outputs).
     * Periodic convolution is used internally (circular boundary).
     *
     * @param wavelet discrete wavelet
     * @param signals AoS signals [batch][length]
     * @param levels number of decomposition levels (≥1)
     * @return AoS results: per-level details and final approximation per signal
     */
    public static MultiLevelResult multiLevelAoS(DiscreteWavelet wavelet, double[][] signals, int levels) {
        if (levels < 1) throw new IllegalArgumentException("levels must be >= 1");
        validateAoS(signals);
        int batch = signals.length;
        int n = signals[0].length;

        double[] soaIn = new double[batch * n];
        BatchSIMDMODWT.convertToSoA(signals, soaIn);

        double[][] soaDetailPerLevel = new double[levels][batch * n];
        double[] soaApproxOut = new double[batch * n];

        BatchSIMDMODWT.batchMultiLevelMODWTSoA(soaIn, soaDetailPerLevel, soaApproxOut, wavelet, batch, n, levels);

        double[][][] detailsAoS = new double[levels][batch][n];
        for (int l = 0; l < levels; l++) {
            BatchSIMDMODWT.convertFromSoA(soaDetailPerLevel[l], detailsAoS[l]);
        }
        double[][] approxAoS = new double[batch][n];
        BatchSIMDMODWT.convertFromSoA(soaApproxOut, approxAoS);
        return new MultiLevelResult(detailsAoS, approxAoS);
    }

    /**
     * Inverse single-level MODWT for a batch of signals (AoS input/outputs).
     * Uses periodic reconstruction via the core {@link com.morphiqlabs.wavelet.modwt.MODWTTransform} per signal.
     *
     * @param wavelet discrete wavelet
     * @param approx AoS approximation coefficients [batch][length]
     * @param detail AoS detail coefficients [batch][length]
     * @return reconstructed AoS signals [batch][length]
     */
    public static double[][] inverseSingleLevelAoS(DiscreteWavelet wavelet, double[][] approx, double[][] detail) {
        validateAoS(approx);
        validateAoS(detail);
        int batch = approx.length;
        if (detail.length != batch || detail[0].length != approx[0].length) {
            throw new IllegalArgumentException("approx/detail shapes must match");
        }
        int n = approx[0].length;
        double[][] out = new double[batch][n];
        com.morphiqlabs.wavelet.modwt.MODWTTransform core = new com.morphiqlabs.wavelet.modwt.MODWTTransform(
                wavelet, com.morphiqlabs.wavelet.api.BoundaryMode.PERIODIC);
        for (int b = 0; b < batch; b++) {
            com.morphiqlabs.wavelet.modwt.MODWTResult res = com.morphiqlabs.wavelet.modwt.MODWTResult.create(
                    approx[b], detail[b]);
            out[b] = core.inverse(res);
        }
        return out;
    }

    /**
     * Inverse multi-level MODWT for a batch (AoS input/outputs).
     * Internally constructs a lightweight {@link com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult}
     * per signal and calls core {@link com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform#reconstruct}.
     *
     * @param wavelet discrete wavelet
     * @param detailPerLevel AoS 3D detail coefficients [levels][batch][length]
     * @param finalApprox AoS final approximation [batch][length]
     * @return reconstructed AoS signals [batch][length]
     */
    public static double[][] inverseMultiLevelAoS(DiscreteWavelet wavelet,
                                                  double[][][] detailPerLevel,
                                                  double[][] finalApprox) {
        if (detailPerLevel == null || detailPerLevel.length == 0) throw new IllegalArgumentException("levels must be > 0");
        int levels = detailPerLevel.length;
        validateAoS(finalApprox);
        int batch = finalApprox.length;
        int n = finalApprox[0].length;
        for (int L = 0; L < levels; L++) {
            if (detailPerLevel[L] == null || detailPerLevel[L].length != batch) {
                throw new IllegalArgumentException("detailPerLevel[L] must be non-null and length=batch for all L");
            }
            for (int b = 0; b < batch; b++) {
                if (detailPerLevel[L][b] == null || detailPerLevel[L][b].length != n) {
                    throw new IllegalArgumentException("all detail rows must have consistent length");
                }
            }
        }
        double[][] out = new double[batch][n];
        com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform core =
                new com.morphiqlabs.wavelet.modwt.MultiLevelMODWTTransform(
                        wavelet, com.morphiqlabs.wavelet.api.BoundaryMode.PERIODIC);
        for (int b = 0; b < batch; b++) {
            com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult res = new ExtMultiLevelResult(detailPerLevel, finalApprox, b);
            out[b] = core.reconstruct(res);
        }
        return out;
    }

    // Lightweight per-signal view implementing core multi-level result interface
    private static final class ExtMultiLevelResult implements com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult {
        private final double[][][] dpl; // [levels][batch][n]
        private final double[][] approx; // [batch][n]
        private final int b;

        ExtMultiLevelResult(double[][][] dpl, double[][] approx, int batchIndex) {
            this.dpl = dpl; this.approx = approx; this.b = batchIndex;
        }
        @Override public int getLevels() { return dpl.length; }
        @Override public int getSignalLength() { return approx[b].length; }
        @Override public double[] getDetailCoeffsAtLevel(int level) { return dpl[level-1][b].clone(); }
        @Override public double[] getApproximationCoeffs() { return approx[b].clone(); }
        @Override public double getDetailEnergyAtLevel(int level) { double s=0; for(double v: dpl[level-1][b]) s+=v*v; return s; }
        @Override public double getApproximationEnergy() { double s=0; for(double v: approx[b]) s+=v*v; return s; }
        @Override public double getTotalEnergy() { double t=getApproximationEnergy(); for(int L=1;L<=getLevels();L++) t+=getDetailEnergyAtLevel(L); return t; }
        @Override public double[] getRelativeEnergyDistribution() { double tot=getTotalEnergy(); double[] r=new double[getLevels()+1]; if(tot!=0){ r[0]=getApproximationEnergy()/tot; for(int L=1;L<=getLevels();L++) r[L]=getDetailEnergyAtLevel(L)/tot; } return r; }
        @Override public com.morphiqlabs.wavelet.modwt.MultiLevelMODWTResult copy() { return this; }
        @Override public boolean isValid() { return true; }
    }

    private static void validateAoS(double[][] signals) {
        if (signals == null || signals.length == 0) {
            throw new IllegalArgumentException("signals must be non-null and non-empty");
        }
        int n = signals[0].length;
        if (n == 0) throw new IllegalArgumentException("signal length must be > 0");
        for (int i = 1; i < signals.length; i++) {
            if (signals[i] == null || signals[i].length != n) {
                throw new IllegalArgumentException("all signals must be non-null and same length");
            }
        }
    }
}
