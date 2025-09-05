# MODWT Upsampling + Scaling Helpers (Developer Note)

This note documents the single source of truth for MODWT/SWT upsampling and scaling in VectorWave. The goal is to avoid duplicating level math across classes and to keep conventions consistent.

## Helpers (ScalarOps)

- `ScalarOps.upsampleAndScaleForIMODWTSynthesis(double[] baseFilter, int level)`
  - Purpose: Cascade stage scaling at level j (used for both analysis and synthesis in the multi‑level cascade)
  - Behavior: inserts `2^(j−1)−1` zeros and applies per‑stage `1/√2` scaling
  - Usage: multi‑level analysis and inverse cascade in `MultiLevelMODWTTransform`, and parallel path

- `ScalarOps.upsampleAndScaleForMODWTAnalysis(double[] baseFilter, int level)`
  - Purpose: Direct analysis at level j (single‑level or non‑cascade use)
  - Behavior: inserts `2^(j−1)−1` zeros and applies `2^(−j/2)` scaling (overall textbook factor)
  - Usage: direct analysis utilities and SIMD paths that are not composing stages

- `ScalarOps.upsampleInsertZeros(double[] baseFilter, int spacing)`
  - Purpose: generic upsample‑by‑spacing (no scaling)
  - Behavior: inserts zeros to produce length `(L0−1)*spacing + 1` with taps at multiples of `spacing`
  - Usage: SWT à trous precomputation (`VectorWaveSwtAdapter.FilterCache`)

## Conventions

- Effective analysis scaling at level j: `2^(−j/2)` (Percival & Walden). In the multi‑level cascade we realize this by applying `1/√2` at each stage; composing j stages yields the textbook factor.
- Cascade stage scaling (analysis and synthesis): `1/√2` per level with à trous upsampling.
- Upsampling: always `2^(j−1)−1` zeros inserted between taps → length `(L0−1)*2^(j−1) + 1`.
- Overflow safety: `ScalarOps` keeps the level shift bounded via `MAX_SAFE_SHIFT_BITS` checks.

## Where it is used

- `MultiLevelMODWTTransform`
  - Analysis (cascade): `scaleFiltersForLevel(...)` → `ScalarOps.upsampleAndScaleForIMODWTSynthesis(...)` (per‑stage `1/√2`)
  - Synthesis (cascade): `upsampleFiltersForLevel(...)` → `ScalarOps.upsampleAndScaleForIMODWTSynthesis(...)`
- `ParallelMultiLevelMODWT`
  - Cascade stages: `upsampleFiltersForLevel(...)` → `ScalarOps.upsampleAndScaleForIMODWTSynthesis(...)`
- `VectorWaveSwtAdapter`
  - à trous analysis cache: `ScalarOps.upsampleAndScaleForIMODWTSynthesis(...)`

## Contributor Guidance

- Do not inline the upsampling/scaling math in transform classes. Always call the relevant `ScalarOps` helper.
- If conventions change (e.g., analysis scaling), update the helper in one place and add/adjust tests that validate PR and energy invariants.
- For symmetric boundary alignment in IMODWT, see `SymmetricAlignmentStrategy` and `docs/guides/SYMMETRIC_ALIGNMENT.md`.
- Level bounds: ensure calls respect `MultiLevelMODWTTransform.getMaximumLevels(N)`; never truncate filters.

## Related Docs

- `docs/guides/SYMMETRIC_ALIGNMENT.md` (symmetric IMODWT heuristic)
- `docs/issues/001-modwt-level-scaling.md` (scaling rationale)
- `docs/issues/003-filter-truncation-behavior.md` (no truncation; throw on invalid levels)
