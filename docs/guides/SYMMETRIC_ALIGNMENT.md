# Symmetric Boundary Alignment (MODWT)

This note documents the practical centering/orientation used for symmetric-boundary MODWT inverse (IMODWT) in VectorWave, based on an empirical sweep across Haar and DB4.

Overview
- Boundary mode: `SYMMETRIC` with centered, level-aware alignment.
- τ_j: computed from base filter length via τ_j = floor(((L0 − 1) · 2^(j − 1)) / 2).
- Orientation: branch-specific (approx/detail) to balance reflection phase.
- Filters: cascade IMODWT uses per‑level upsampled reconstruction filters with 1/√2 scaling (same as periodic cascade synthesis).

Heuristic (current)
- Detail branch (G): plus orientation always; level‑conditional Δτ_G:
  - j ≤ 2: Δτ_G = 0
  - j ≥ 3: Δτ_G = −1
- Approx branch (H): level‑ and family‑conditional
  - Haar (L0 = 2): plus orientation; Δτ_H = 0 for j = 1, and Δτ_H = −1 for j ≥ 2
  - DB4 and longer (L0 ≥ 8): minus orientation; Δτ_H = 0 for j = 1, and Δτ_H = −1 for j ≥ 2

Rationale and Evidence
- We introduced a sweep diagnostic (`MultiLevelMODWTSymmetricAlignmentSweepTest`) that tries:
  - Orientations: plus/minus per branch
  - Offsets: Δτ ∈ {−1, 0, +1}
  - Wavelets: Haar, DB4
  - Sizes: N ∈ {129, 257, 512}
  - Levels: up to maximum allowed
- Observed best configurations:
  - Haar: `H:plus dH=−1`, `G:plus dG=0` (best RMSE ≈ 0.25–0.29)
  - DB4: `H:minus dH∈{0,−1}`, `G:plus dG=0` (best RMSE ≈ 0.65–0.70)
- We codified these findings in an internal strategy class: `SymmetricAlignmentStrategy`.

Practical Accuracy
- Periodic: near machine precision (RMSE ≤ 1e−8) with Haar/DB4 across sizes/levels.
- Zero-padding: expected boundary loss; periodic remains the reference for strict invariants.
- Symmetric: RMSE < 0.70 on Haar/DB4 across {129, 257, 512} and levels up to max (test: `MultiLevelMODWTSymmetricRMSETest`).

Implementation
- Strategy class: `com.morphiqlabs.wavelet.modwt.SymmetricAlignmentStrategy` returns a per-level decision:
  - `approxPlus` (orientation), `deltaApprox` (Δτ_H), `detailPlus`, `deltaDetail` (Δτ_G).
- The inverse synthesis applies:
  - Approx (H): `idx = sym(t ± l ∓ τ_H)` depending on orientation
  - Detail (G): `idx = sym(t + l − τ_G)` (plus)
- τ_H, τ_G = `computeTauJ(L0, j) + Δτ`.

Diagnostics
- Periodic diagnostic: `PeriodicCascadeInverseDiagnosticTest` asserts RMSE ≤ 1e−8 (Haar/DB4) at small N.
- Symmetric sweep: `MultiLevelMODWTSymmetricAlignmentSweepTest` prints best RMSE/config.

Future Work
- If applications require stricter symmetric accuracy, consider:
  - Choosing Δτ_H = 0 for DB4 at specific levels/sizes (seen as near-equal in the sweep).
  - Family‑specific tables for additional orthogonal wavelets (DB6/DB8) with the same mechanism.
