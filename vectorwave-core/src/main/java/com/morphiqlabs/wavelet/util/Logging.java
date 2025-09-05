package com.morphiqlabs.wavelet.util;

/**
 * Lightweight helper for obtaining System.Logger instances and parsing log levels.
 *
 * <p>Usage:
 * <pre>{@code
 * private static final System.Logger LOG = Logging.getLogger(MyClass.class);
 * LOG.log(System.Logger.Level.INFO, "Hello");
 * }</pre>
 *
 * <p>Level configuration is handled by the underlying logging implementation. For simple
 * filtering, you can use the system property {@code -Dvectorwave.log.level=INFO} or the
 * environment variable {@code VECTORWAVE_LOG_LEVEL}. Libraries embedding VectorWave may
 * bridge {@link java.lang.System.Logger} to their preferred logging system.</p>
 */
public final class Logging {
    private Logging() {
        throw new AssertionError("No instances");
    }

    /** Returns a logger for the given class. */
    public static System.Logger getLogger(Class<?> cls) {
        return System.getLogger(cls.getName());
    }

    /** Returns a logger for the given name. */
    public static System.Logger getLogger(String name) {
        return System.getLogger(name);
    }

    /** Parses a textual level into a System.Logger.Level, defaulting to INFO. */
    public static System.Logger.Level parseLevel(String level) {
        if (level == null) return System.Logger.Level.INFO;
        switch (level.trim().toUpperCase()) {
            case "TRACE":
            case "FINEST": return System.Logger.Level.TRACE;
            case "DEBUG":
            case "FINER": return System.Logger.Level.DEBUG;
            case "FINE": return System.Logger.Level.DEBUG;
            case "INFO": return System.Logger.Level.INFO;
            case "WARN":
            case "WARNING": return System.Logger.Level.WARNING;
            case "ERROR":
            case "SEVERE": return System.Logger.Level.ERROR;
            case "OFF": return System.Logger.Level.OFF;
            default: return System.Logger.Level.INFO;
        }
    }

    /** Reads desired level from system property or env var (implementation-defined). */
    public static System.Logger.Level configuredLevel() {
        String prop = System.getProperty("vectorwave.log.level");
        if (prop != null) return parseLevel(prop);
        String env = System.getenv("VECTORWAVE_LOG_LEVEL");
        return parseLevel(env);
    }
}
