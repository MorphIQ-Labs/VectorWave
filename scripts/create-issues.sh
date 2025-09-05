#!/usr/bin/env bash
set -euo pipefail

# Usage: ./scripts/create-issues.sh
# Requires: gh CLI authenticated to GitHub and repo has Issues enabled.

if ! command -v gh >/dev/null 2>&1; then
  echo "gh (GitHub CLI) is not installed. Install from https://cli.github.com/" >&2
  exit 1
fi

if ! gh auth status >/dev/null 2>&1; then
  echo "gh is not authenticated. Run: gh auth login" >&2
  exit 1
fi

REPO=$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || true)
if [[ -z "${REPO}" ]]; then
  echo "Failed to detect GitHub repo. Ensure you are in a cloned GitHub repo." >&2
  exit 1
fi

echo "Creating issues in ${REPO}..."

create_issue() {
  local title="$1"; shift
  local labels="$1"; shift
  local body_file="$1"; shift

  echo "- ${title}"
  gh issue create \
    --title "${title}" \
    --label "${labels}" \
    --body-file "${body_file}" \
    >/dev/null
}

# Map: title | labels | body file
create_issue "Fix multi-level MODWT scaling to 2^(−j/2)" "bug,correctness,modwt" docs/issues/001-modwt-level-scaling.md
create_issue "Correct MODWT scaling formula in MultiLevelMODWTTransform Javadoc" "docs,correctness,modwt" docs/issues/002-javadoc-scaling-correction.md
create_issue "Remove or guard filter truncation in multi-level MODWT" "bug,correctness,modwt" docs/issues/003-filter-truncation-behavior.md
create_issue "Fix bracing/loop structure in zero-padding MODWT convolution" "bug,modwt,boundary" docs/issues/004-zeropad-bracing.md
create_issue "Standardize per-level MODWT scaling via shared utility" "enhancement,refactor,modwt" docs/issues/005-standardize-scaling-utility.md
create_issue "Add MODWT tests for reconstruction and energy preservation" "tests,correctness,modwt" docs/issues/006-tests-modwt-reconstruct-energy.md
create_issue "Add wavelet coefficient verification tests (orthogonal and coiflets)" "tests,correctness,wavelets" docs/issues/007-tests-wavelet-verification.md
create_issue "Cache scaled and upsampled per-level filters in multi-level MODWT" "performance,enhancement,modwt" docs/issues/008-cache-perlevel-filters.md
create_issue "Add FFT-based circular convolution fallback for large effective filters" "performance,enhancement,modwt" docs/issues/009-fft-convolution-fallback.md
create_issue "Extend SIMD batch implementation to multi-level MODWT" "performance,enhancement,simd,modwt" docs/issues/010-simd-multilevel.md
create_issue "Add JMH microbenchmarks for multi-level MODWT" "performance,benchmark,modwt" docs/issues/011-jmh-multilevel-benchmarks.md
create_issue "Fix SWT adapter: unused filter cache and incorrect spacing" "enhancement,swt,modwt" docs/issues/012-swt-filtercache-spacing.md
create_issue "Implement Wavelet.validatePerfectReconstruction()" "enhancement,correctness,wavelets" docs/issues/013-validate-perfect-reconstruction-method.md
create_issue "Make ParallelMultiLevelMODWT executor configurable" "enhancement,performance,parallel" docs/issues/014-parallel-executor-config.md
create_issue "Add logging/statistics when filter truncation would trigger" "observability,correctness,modwt" docs/issues/015-logging-truncation.md
create_issue "Unify docs around correct MODWT level scaling and boundary modes" "docs,modwt" docs/issues/016-docs-consistent-modwt-scaling.md
create_issue "Unify MODWT and SWT to textbook 2^(−j/2) scaling and alignment" "correctness,modwt,swt,breaking-change,docs,tests" docs/issues/019-modwt-swt-textbook-scaling-unification.md

echo "All issues created."
