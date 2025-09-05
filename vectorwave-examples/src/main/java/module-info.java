module com.morphiqlabs.examples {
    requires transitive com.morphiqlabs.vectorwave.core;
    requires com.morphiqlabs.vectorwave.extensions;
    requires transitive jdk.incubator.vector;
    requires java.management;

    // Export example packages for demos and benchmarks
    exports com.morphiqlabs.examples;
    // basic and finance packages are excluded from build (use public APIs only)
    // Note: demo package intentionally excluded from compilation via maven-compiler excludes
    exports com.morphiqlabs.examples.parallel;
    exports com.morphiqlabs.examples.cwt.optimization;
    exports com.morphiqlabs.examples.util;
}
