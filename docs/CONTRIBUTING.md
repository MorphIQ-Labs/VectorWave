# Contributing Guidelines

Thanks for contributing to VectorWave! This document captures a few decisions to keep the codebase consistent and easy to maintain.

## Exception Policy (Public APIs)

Use specific, actionable exceptions with structured context. Prefer these categories for public entry points:

- Validation errors: `InvalidArgumentException` (bad parameters), `InvalidSignalException` (bad input data).
- Configuration/state errors: `IllegalStateException` (internal invariant broken or invalid lifecycle), with a clear message.
- Capability errors: `UnsupportedOperationException` for unimplemented modes (e.g., CONSTANT boundaries).

Attach context using `ErrorCode` and `ErrorContext` where it improves diagnosis (IDs, sizes, boundary mode, wavelet).

Guidelines:
- Fail fast at the API boundary; do not defer obvious validation errors to deep internals.
- Error messages should be brief, specific, and include the failing parameter and expected range.
- Keep exception types stable across minor releases.

## Utility Class Pattern

For static-only utility types, make construction impossible and explicit:

```java
public final class FooUtils {
    private FooUtils() { throw new AssertionError("No instances"); }
    // static helpers …
}
```

This communicates intent, avoids accidental instantiation, and surfaces misuse early.

## API Hygiene

- Add `@since 1.0.0` to public top-level types and newly added public methods.
- Keep Javadoc doclint-clean (`-Xdoclint:all`). If a doclint rule is noisy, fix the docs rather than disabling lint.
- Avoid exposing internals across modules; prefer qualified exports for friend modules (extensions) when necessary.

## Coding Style (delta over defaults)

- Prefer descriptive variable names; avoid single-letter names outside tight loops.
- No wildcard imports. Order imports by package depth.
- Keep methods short and focused; extract helpers for clarity.
- Use `try-with-resources` for anything `AutoCloseable`.

## Tests

- Co-locate focused unit tests with the code they validate.
- Prefer deterministic tests; use fixed seeds where randomness is involved.
- When behavior is heuristic (e.g., SYMMETRIC alignment), assert on interior regions or robust metrics rather than brittle global equality.

## Docs

- CWT is experimental: mark packages/types with `@Experimental` and state that semantics may change.
- README: Core = Java 21 scalar; SIMD and structured concurrency live in extensions.

