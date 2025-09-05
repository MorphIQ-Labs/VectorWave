# Repository Guidelines

## Project Structure & Module Organization
- `vectorwave-core`: Core algorithms (Java 21). Code in `src/main/java`, tests in `src/test/java`.
- `vectorwave-extensions`: SIMD/Vector API optimizations (Java 24 preview). Requires `--enable-preview` and `jdk.incubator.vector`.
- `vectorwave-examples`: Demos and runnable mains (no JMH).
- `vectorwave-benchmarks`: Non‑modular JMH benchmarks (classpath‑based).
  See packages under `com.morphiqlabs.benchmark`.
- `docs/`: Guides and performance notes. `scripts/`: benchmarking helpers (`jmh-runner.sh`, `scripts/benchmark-graalvm.sh`).

## Build, Test, and Development Commands
- Build all: `mvn clean install` (runs tests, generates JaCoCo coverage).
- Fast build: `mvn -DskipTests package`.
- Module tests: `mvn -pl vectorwave-core -am test` (replace module as needed).
- Run a demo: `mvn -q -pl vectorwave-examples exec:java -Dexec.mainClass=com.morphiqlabs.examples.BasicExample`.
- Run JMH: `./jmh-runner.sh` or `mvn -q -pl vectorwave-benchmarks -am exec:java -Dexec.mainClass=org.openjdk.jmh.Main` (see scripts/benchmark-graalvm.sh).

## Coding Style & Naming Conventions
- Java: 21 for `core`; 24 + preview for `extensions`/`examples` (plugins already pass flags).
- Indentation: 4 spaces; avoid wildcard imports; enable `-Xlint:all` cleanliness (no new warnings).
- Naming: classes `PascalCase`, methods/fields `camelCase`, constants `UPPER_SNAKE_CASE`.
- Packages: `com.morphiqlabs.wavelet.*` (library), `com.morphiqlabs.examples.*` (examples).
- Tests mirror source packages; filenames end with `*Test.java` (e.g., `MODWTTransformTest`).

## Testing Guidelines
- Framework: JUnit 5. Place tests in `src/test/java` within the same module.
- Coverage: JaCoCo reports at `target/site/jacoco/index.html`. Keep or improve coverage for changed code.
- Performance: Use JMH benchmarks in `vectorwave-benchmarks` for perf claims; do not add flaky timing asserts to unit tests.

## Commit & Pull Request Guidelines
- Commits: imperative mood, concise summary; reference issues like `(#123)`; include scope when helpful (e.g., `[core]`, `[extensions]`).
    - Example: `Fix Paul wavelet normalization (#14)`
- PRs: clear description, linked issues, tests for new behavior, updated docs, and benchmark evidence for performance changes. Include logs/screenshots where relevant. Ensure CI passes.

## Security & Configuration Tips
- JDKs: Java 21+ required; Java 24 for Vector API previews. GraalVM 24.0.2 recommended for benchmarks.
- Publishing: see `settings-template.xml` for Maven server credentials and GitHub Packages (`GITHUB_TOKEN`).
