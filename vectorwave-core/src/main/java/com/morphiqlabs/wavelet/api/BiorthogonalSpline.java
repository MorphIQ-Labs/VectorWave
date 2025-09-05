package com.morphiqlabs.wavelet.api;

/**
 * Biorthogonal spline wavelets (biorNr.Nd) family.
 *
 * <p>These wavelets use different filters for decomposition and reconstruction,
 * allowing for symmetric filters which are important in image processing.</p>
 *
 * <p>The naming convention biorNr.Nd means:
 * <ul>
 *   <li>Nr: order of the spline for reconstruction</li>
 *   <li>Nd: order of the spline for decomposition</li>
 * </ul>
 *
 * <p>Common variants: bior1.3, bior2.2, bior3.3, bior4.4</p>
 */
public final class BiorthogonalSpline implements BiorthogonalWavelet {

    // Common mathematical constants for improved readability
    private static final double ONE_EIGHTH = 1.0 / 8.0;
    private static final double MINUS_ONE_EIGHTH = -1.0 / 8.0;
    private static final double ONE = 1.0;
    private static final double SQRT_2_INVERSE = 1.0 / Math.sqrt(2.0);  // 0.7071067811865476
    private static final double SQRT_2 = Math.sqrt(2.0);  // 1.4142135623730951
    private static final double HALF_SQRT_2_INVERSE = 0.5 / Math.sqrt(2.0);  // 0.3535533905932738
    private static final double QUARTER_SQRT_2_INVERSE = 0.25 / Math.sqrt(2.0);  // 0.1767766952966369
    private static final double THREE_QUARTER_SQRT_2_INVERSE = 0.75 / Math.sqrt(2.0);  // 0.5303300858899107
    private static final double THREE_HALF_SQRT_2_INVERSE = 1.5 / Math.sqrt(2.0);  // THREE_HALF_SQRT_2_INVERSE

    /** Biorthogonal 1.3 wavelet. */
    public static final BiorthogonalSpline BIOR1_3 = new BiorthogonalSpline(
            "bior1.3", 1, 3,
            // Decomposition low-pass filter (analysis filter h0_tilde)
            new double[]{MINUS_ONE_EIGHTH, ONE_EIGHTH, ONE, ONE, ONE_EIGHTH, MINUS_ONE_EIGHTH},
            // Reconstruction low-pass filter (synthesis filter h0)
            new double[]{ONE, ONE},
            true,
            0.5,  // Reconstruction scaling factor: For CDF 1,3 wavelets, the perfect
                  // reconstruction condition requires scaling by 1/2. This compensates
                  // for the energy distribution between the analysis and synthesis filters
                  // to ensure that ||reconstructed|| = ||original||.
            2     // Group delay: For CDF 1,3 wavelets, the combined analysis-synthesis
                  // filter bank introduces a delay of 2 samples. This is calculated as:
                  // delay = (length(h0_tilde) - 1)/2 + (length(h0) - 1)/2 - 1
                  //       = (6 - 1)/2 + (2 - 1)/2 - 1 = 2.5 + 0.5 - 1 = 2
                  // This delay must be compensated during reconstruction to achieve
                  // perfect alignment with the original signal.
    );
    
    /** Biorthogonal 1.1 wavelet (Haar-like). */
    // BIOR1.1 - Haar-like, simplest biorthogonal wavelet
    public static final BiorthogonalSpline BIOR1_1 = new BiorthogonalSpline(
            "bior1.1", 1, 1,
            // Decomposition low-pass filter
            new double[]{SQRT_2_INVERSE, SQRT_2_INVERSE},
            // Reconstruction low-pass filter  
            new double[]{SQRT_2_INVERSE, SQRT_2_INVERSE},
            true,
            1.0,  // No scaling needed for orthogonal Haar
            0     // No group delay for symmetric Haar
    );
    
    /** Biorthogonal 1.5 wavelet. */
    // BIOR1.5 - Higher order for smoother reconstruction
    public static final BiorthogonalSpline BIOR1_5 = new BiorthogonalSpline(
            "bior1.5", 1, 5,
            // Decomposition low-pass filter (10 coefficients)
            new double[]{0.016387336463, -0.041464936782, -0.067372554722, 0.386110066823,
                        0.812723635450, 0.417005184424, -0.076488599078, -0.059434418646,
                        0.023680171947, 0.005611434819},
            // Reconstruction low-pass filter (2 coefficients)
            new double[]{SQRT_2_INVERSE, SQRT_2_INVERSE},
            true,
            SQRT_2_INVERSE,  // Reconstruction scaling
            4     // Group delay
    );
    
    /** Biorthogonal 2.2 wavelet (linear spline). */
    // BIOR2.2 - Linear spline, good for edge preservation
    // CDF 2,2 coefficients - popular in image processing
    public static final BiorthogonalSpline BIOR2_2 = new BiorthogonalSpline(
            "bior2.2", 2, 2,
            // Decomposition low-pass filter (5 coefficients)
            new double[]{-QUARTER_SQRT_2_INVERSE, HALF_SQRT_2_INVERSE, THREE_HALF_SQRT_2_INVERSE,
                        HALF_SQRT_2_INVERSE, -QUARTER_SQRT_2_INVERSE},
            // Reconstruction low-pass filter (3 coefficients)
            new double[]{HALF_SQRT_2_INVERSE, SQRT_2_INVERSE, HALF_SQRT_2_INVERSE},
            true,
            1.0,  // No additional scaling needed
            2     // Group delay
    );
    
    /** Biorthogonal 2.4 wavelet. */
    // BIOR2.4 - Popular for image processing
    public static final BiorthogonalSpline BIOR2_4 = new BiorthogonalSpline(
            "bior2.4", 2, 4,
            // Decomposition low-pass filter (9 coefficients)
            new double[]{0.03782845550726, -0.02384946501956, -0.11062440441844, 0.37740285561138,
                        0.85269867900889, 0.37740285561138, -0.11062440441844, -0.02384946501956,
                        0.03782845550726},
            // Reconstruction low-pass filter (3 coefficients)
            new double[]{HALF_SQRT_2_INVERSE, SQRT_2_INVERSE, HALF_SQRT_2_INVERSE},
            true,
            1.0,  // No additional scaling needed
            4     // Group delay
    );
    
    /** Biorthogonal 2.6 wavelet. */
    // BIOR2.6 - Better frequency localization
    public static final BiorthogonalSpline BIOR2_6 = new BiorthogonalSpline(
            "bior2.6", 2, 6,
            // Decomposition low-pass filter (13 coefficients)
            new double[]{-0.00679744371334, 0.00193588752085, 0.03307121615827, -0.06650878107113,
                        -0.17185392490960, 0.42063224422510, 0.99438208967163, 0.42063224422510,
                        -0.17185392490960, -0.06650878107113, 0.03307121615827, 0.00193588752085,
                        -0.00679744371334},
            // Reconstruction low-pass filter (3 coefficients)
            new double[]{HALF_SQRT_2_INVERSE, SQRT_2_INVERSE, HALF_SQRT_2_INVERSE},
            true,
            0.7071067811865476,  // Reconstruction scaling
            6     // Group delay
    );
    
    /** Biorthogonal 2.8 wavelet. */
    // BIOR2.8 - Highest order in family 2
    public static final BiorthogonalSpline BIOR2_8 = new BiorthogonalSpline(
            "bior2.8", 2, 8,
            // Decomposition low-pass filter (17 coefficients)
            new double[]{0.00133639669065, 0.00020703315896, -0.00750912736790, 0.00513451063436,
                        0.03968708834741, -0.05514273381420, -0.15999329944587, 0.29774790265169,
                        1.13158823408280, 0.29774790265169, -0.15999329944587, -0.05514273381420,
                        0.03968708834741, 0.00513451063436, -0.00750912736790, 0.00020703315896,
                        0.00133639669065},
            // Reconstruction low-pass filter (3 coefficients)
            new double[]{HALF_SQRT_2_INVERSE, SQRT_2_INVERSE, HALF_SQRT_2_INVERSE},
            true,
            0.6298942083656,  // Reconstruction scaling
            8     // Group delay
    );
    
    /** Biorthogonal 3.1 wavelet. */
    // BIOR3.1 - Minimal phase distortion
    public static final BiorthogonalSpline BIOR3_1 = new BiorthogonalSpline(
            "bior3.1", 3, 1,
            // Decomposition low-pass filter (4 coefficients)
            new double[]{-HALF_SQRT_2_INVERSE, THREE_HALF_SQRT_2_INVERSE, THREE_HALF_SQRT_2_INVERSE, -HALF_SQRT_2_INVERSE},
            // Reconstruction low-pass filter (4 coefficients)
            new double[]{QUARTER_SQRT_2_INVERSE, THREE_QUARTER_SQRT_2_INVERSE, THREE_QUARTER_SQRT_2_INVERSE, QUARTER_SQRT_2_INVERSE},
            true,
            1.0,  // No additional scaling needed
            2     // Group delay
    );
    
    /** Biorthogonal 3.3 wavelet. */
    // BIOR3.3 - Balanced properties, commonly used
    public static final BiorthogonalSpline BIOR3_3 = new BiorthogonalSpline(
            "bior3.3", 3, 3,
            // Decomposition low-pass filter (8 coefficients)
            new double[]{0.06629126073624, -0.19887378220871, -0.15467960838456, 0.99436891104358,
                        0.99436891104358, -0.15467960838456, -0.19887378220871, 0.06629126073624},
            // Reconstruction low-pass filter (4 coefficients)
            new double[]{QUARTER_SQRT_2_INVERSE, THREE_QUARTER_SQRT_2_INVERSE, THREE_QUARTER_SQRT_2_INVERSE, QUARTER_SQRT_2_INVERSE},
            true,
            1.0,  // No additional scaling needed
            3     // Group delay
    );
    
    /** Biorthogonal 3.5 wavelet. */
    // BIOR3.5 - Good for texture analysis
    public static final BiorthogonalSpline BIOR3_5 = new BiorthogonalSpline(
            "bior3.5", 3, 5,
            // Decomposition low-pass filter (12 coefficients)
            new double[]{-0.01380930374353, 0.04145791438182, 0.05224175305576, -0.26792357880567,
                        -0.07181553374457, 0.96674755240348, 0.96674755240348, -0.07181553374457,
                        -0.26792357880567, 0.05224175305576, 0.04145791438182, -0.01380930374353},
            // Reconstruction low-pass filter (4 coefficients)
            new double[]{QUARTER_SQRT_2_INVERSE, THREE_QUARTER_SQRT_2_INVERSE, THREE_QUARTER_SQRT_2_INVERSE, QUARTER_SQRT_2_INVERSE},
            true,
            1.0,  // No additional scaling needed
            5     // Group delay
    );
    
    /** Biorthogonal 3.7 wavelet. */
    // BIOR3.7 - Higher vanishing moments
    public static final BiorthogonalSpline BIOR3_7 = new BiorthogonalSpline(
            "bior3.7", 3, 7,
            // Decomposition low-pass filter (16 coefficients)
            new double[]{0.00353713800152, -0.00709427600303, -0.00881784307093, 0.06207776192089,
                        0.00123255138768, -0.24072663312710, 0.03892321448596, 0.97875960941423,
                        0.97875960941423, 0.03892321448596, -0.24072663312710, 0.00123255138768,
                        0.06207776192089, -0.00881784307093, -0.00709427600303, 0.00353713800152},
            // Reconstruction low-pass filter (4 coefficients)
            new double[]{QUARTER_SQRT_2_INVERSE, THREE_QUARTER_SQRT_2_INVERSE, THREE_QUARTER_SQRT_2_INVERSE, QUARTER_SQRT_2_INVERSE},
            true,
            1.0,  // No additional scaling needed
            7     // Group delay
    );
    
    /** Biorthogonal 3.9 wavelet. */
    // BIOR3.9 - Maximum smoothness in family 3
    public static final BiorthogonalSpline BIOR3_9 = new BiorthogonalSpline(
            "bior3.9", 3, 9,
            // Decomposition low-pass filter (20 coefficients)
            new double[]{-0.00088388347648, 0.00141682627301, 0.00294786709734, -0.01721018024336,
                        -0.00471408733933, 0.06375562764545, -0.01636499682386, -0.17520070369698,
                        0.11175187708906, 1.01284028851077, 1.01284028851077, 0.11175187708906,
                        -0.17520070369698, -0.01636499682386, 0.06375562764545, -0.00471408733933,
                        -0.01721018024336, 0.00294786709734, 0.00141682627301, -0.00088388347648},
            // Reconstruction low-pass filter (4 coefficients)
            new double[]{QUARTER_SQRT_2_INVERSE, THREE_QUARTER_SQRT_2_INVERSE, THREE_QUARTER_SQRT_2_INVERSE, QUARTER_SQRT_2_INVERSE},
            true,
            0.9808930729813,  // Reconstruction scaling
            9     // Group delay
    );
    
    /** Biorthogonal 4.4 wavelet (CDF 9/7). */
    // BIOR4.4 - Critical: Used in JPEG2000 standard (CDF 9/7 wavelet)
    // This is the most important biorthogonal wavelet for image compression
    public static final BiorthogonalSpline BIOR4_4 = new BiorthogonalSpline(
            "bior4.4", 4, 4,
            // Decomposition low-pass filter (9 coefficients) - CDF 9/7 analysis filter
            new double[]{0.03782845550699, -0.02384946501938, -0.11062440441842, 0.37740285561265,
                        0.85269867900940, 0.37740285561265, -0.11062440441842, -0.02384946501938,
                        0.03782845550699},
            // Reconstruction low-pass filter (7 coefficients) - CDF 9/7 synthesis filter
            new double[]{-0.06453888262876, -0.04068941760916, 0.41809227322162, 0.78848561640558,
                        0.41809227322162, -0.04068941760916, -0.06453888262876},
            true,
            1.0,  // No additional scaling needed for CDF 9/7
            4     // Group delay
    );
    
    /** Biorthogonal 5.5 wavelet. */
    // BIOR5.5 - High order for smooth signals
    public static final BiorthogonalSpline BIOR5_5 = new BiorthogonalSpline(
            "bior5.5", 5, 5,
            // Decomposition low-pass filter (12 coefficients)
            new double[]{0.01388101643146, -0.04166303571439, -0.06739205471574, 0.38611006682117,
                        0.81272363544987, 0.41700518442165, -0.07648488313481, -0.05943441806447,
                        0.02368017193823, 0.00561143481953, -0.00182320886991, -0.00071799821619},
            // Reconstruction low-pass filter (8 coefficients)
            new double[]{-0.01514974064694, -0.01514974064694, 0.30298948129389, 0.60597896258778,
                        0.60597896258778, 0.30298948129389, -0.01514974064694, -0.01514974064694},
            true,
            1.0,  // No additional scaling needed
            5     // Group delay
    );
    
    /** Biorthogonal 6.8 wavelet. */
    // BIOR6.8 - Maximum order commonly used
    public static final BiorthogonalSpline BIOR6_8 = new BiorthogonalSpline(
            "bior6.8", 6, 8,
            // Decomposition low-pass filter (17 coefficients)
            new double[]{0.00199043589843, 0.00033522297333, -0.01259715653306, 0.00620544193220,
                        0.06776863310747, -0.08602163816209, -0.31275875600911, 0.22783840537710,
                        1.35158010213994, 0.22783840537710, -0.31275875600911, -0.08602163816209,
                        0.06776863310747, 0.00620544193220, -0.01259715653306, 0.00033522297333,
                        0.00199043589843},
            // Reconstruction low-pass filter (16 coefficients)
            new double[]{0.00044174303816, -0.00132354366062, -0.00048809535103, 0.01141610211580,
                        -0.01159170830368, -0.03158210553249, 0.14270775786152, 0.72656452844835,
                        0.72656452844835, 0.14270775786152, -0.03158210553249, -0.01159170830368,
                        0.01141610211580, -0.00048809535103, -0.00132354366062, 0.00044174303816},
            true,
            0.46978742858291,  // Reconstruction scaling
            8     // Group delay
    );
    private final String name;
    private final int reconstructionOrder;
    private final int decompositionOrder;
    private final double[] lowPassDecomp;
    private final double[] lowPassRecon;
    private final boolean symmetric;
    private final double reconstructionScale;
    private final int groupDelay;

    private BiorthogonalSpline(String name, int reconOrder, int decompOrder,
                               double[] lowPassDecomp, double[] lowPassRecon,
                               boolean symmetric, double reconstructionScale, int groupDelay) {
        this.name = name;
        this.reconstructionOrder = reconOrder;
        this.decompositionOrder = decompOrder;
        // For biorthogonal wavelets, do NOT normalize the filters
        // The filters must satisfy the biorthogonality conditions exactly
        this.lowPassDecomp = lowPassDecomp.clone();
        this.lowPassRecon = lowPassRecon.clone();
        this.symmetric = symmetric;
        this.reconstructionScale = reconstructionScale;
        this.groupDelay = groupDelay;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String description() {
        return String.format("Biorthogonal spline wavelet %s (reconstruction order %d, decomposition order %d)",
                name, reconstructionOrder, decompositionOrder);
    }

    @Override
    public double[] lowPassDecomposition() {
        return lowPassDecomp.clone();
    }

    @Override
    public double[] highPassDecomposition() {
        // For biorthogonal wavelets, generate high-pass decomposition from low-pass reconstruction
        // Using the correct biorthogonal relationship: g[n] = (-1)^n * h[-n]
        double[] h = lowPassRecon;
        double[] g = new double[h.length];
        for (int i = 0; i < h.length; i++) {
            // Reverse the filter and apply alternating signs
            int sign = ((h.length - 1 - i) % 2 == 0) ? 1 : -1;
            g[i] = sign * h[h.length - 1 - i];
        }
        return g;
    }

    @Override
    public double[] lowPassReconstruction() {
        return lowPassRecon.clone();
    }

    @Override
    public double[] highPassReconstruction() {
        // For biorthogonal wavelets, generate high-pass reconstruction from low-pass decomposition
        // Using the correct biorthogonal relationship
        double[] h = lowPassDecomp;
        double[] g = new double[h.length];
        for (int i = 0; i < h.length; i++) {
            // Reverse the filter and apply alternating signs
            int sign = ((h.length - 1 - i) % 2 == 0) ? 1 : -1;
            g[i] = sign * h[h.length - 1 - i];
        }
        return g;
    }

    @Override
    public int vanishingMoments() {
        return decompositionOrder;
    }

    @Override
    public int dualVanishingMoments() {
        return reconstructionOrder;
    }

    @Override
    public int splineOrder() {
        return Math.max(reconstructionOrder, decompositionOrder);
    }

    @Override
    public boolean isSymmetric() {
        return symmetric;
    }
    
    /**
     * Returns the reconstruction scaling factor.
     * For CDF wavelets, this is typically 0.5 to satisfy perfect reconstruction.
     *
     * @return the reconstruction scaling factor
     */
    public double getReconstructionScale() {
        return reconstructionScale;
    }
    
    /**
     * Returns the group delay (phase shift) of the wavelet.
     * This is the number of samples the reconstruction is shifted.
     *
     * @return the group delay in samples
     */
    public int getGroupDelay() {
        return groupDelay;
    }
}
