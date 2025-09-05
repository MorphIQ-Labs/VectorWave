# Interpreting Detail Levels (DB4/DB6/COIF)

This quick guide helps map MODWT/SWT levels to qualitative frequency bands for common wavelet families. Exact passbands depend on sampling rate and wavelet response; treat these as practical heuristics rather than brick‑wall filters.

Key ideas
- Level j captures features roughly around 2^(j−1)–2^j sample scales (dyadic bands).
- Higher j → coarser content (slower trends); lower j → finer detail (edges/noise).
- Family choice affects bandwidth/leakage: DB6/COIF are smoother than DB4 and attenuate high‑frequency noise more aggressively.

DB4 (Daubechies 4)
- Level 1: Sharp transients, high‑frequency noise; good edge localization; may keep more fine noise than DB6/COIF.
- Levels 2–3: Short cycles, edges; balances time/frequency localization.
- Levels 4+: Medium/coarse trends; suitable for baseline drift and regime shifts.

DB6 (Daubechies 6)
- Level 1: Fine detail with slightly stronger noise suppression than DB4; smoother impulse response.
- Levels 2–3: Mid‑band structure with cleaner separation; good for denoising/detail extraction.
- Levels 4+: Coarse trends with reduced ringing vs DB4.

COIF (e.g., COIF2/COIF4/COIF6)
- Level 1: Conservative high‑frequency capture; more smoothing (good for noisy data).
- Levels 2–3: Mid‑band with strong smoothness; effective for denoising while preserving structure.
- Levels 4+: Coarse trends with minimal ripples; preferred for long‑horizon components.

Tips
- Denoising: Start with DB6 or COIF2, soft threshold on levels 1–2, optional level‑dependent thresholds.
- Feature extraction: Use DB4 for sharper edges; DB6/COIF to reduce ringing.
- Validation: Inspect energy per level and reconstruct bands of interest to confirm interpretation.

See also
- README “Boundary Modes Matrix” for edge handling choices.
- docs/guides/SYMMETRIC_ALIGNMENT.md for the symmetric inverse policy.
