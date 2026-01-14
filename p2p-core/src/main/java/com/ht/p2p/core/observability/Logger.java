// File: src/main/java/com/hoangtien/p2p/core/observability/Logger.java
package com.ht.p2p.core.observability;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal structured stdout logger: key=value ... (no slf4j/logback).
 */
public interface Logger {
    void info(String msg, Map<String, Object> fields);
    void warn(String msg, Map<String, Object> fields);
    void error(String msg, Map<String, Object> fields);

    default void info(String msg) { info(msg, Map.of()); }
    default void warn(String msg) { warn(msg, Map.of()); }
    default void error(String msg) { error(msg, Map.of()); }

    static Logger stdout(Clock clock) {
        return new StdoutLogger(Objects.requireNonNull(clock, "clock"));
    }

    static Map<String, Object> fields(Object... kv) {
        if (kv == null || kv.length == 0) return Map.of();
        if (kv.length % 2 != 0) {
            throw new IllegalArgumentException("fields() requires even number of arguments: key,value,...");
        }
        Map<String, Object> out = new LinkedHashMap<>(kv.length / 2);
        for (int i = 0; i < kv.length; i += 2) {
            Object k = kv[i];
            Object v = kv[i + 1];
            if (!(k instanceof String s) || s.isBlank()) {
                throw new IllegalArgumentException("Key must be non-blank String at index " + i);
            }
            out.put(s, v);
        }
        return out;
    }
}

/**
 * Package-private implementation: single-line structured logs to stdout.
 */
final class StdoutLogger implements Logger {
    private final Clock clock;

    StdoutLogger(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void info(String msg, Map<String, Object> fields) {
        log("INFO", msg, fields, null);
    }

    @Override
    public void warn(String msg, Map<String, Object> fields) {
        log("WARN", msg, fields, null);
    }

    @Override
    public void error(String msg, Map<String, Object> fields) {
        log("ERROR", msg, fields, null);
    }

    private void log(String level, String msg, Map<String, Object> fields, Throwable t) {
        Instant now = clock.instant();
        String thread = Thread.currentThread().getName();

        // Preserve insertion order for readability
        Map<String, Object> merged = new LinkedHashMap<>();
        merged.put(LogKeys.TS, now.toString());
        merged.put(LogKeys.LVL, level);
        merged.put(LogKeys.THREAD, thread);
        merged.put(LogKeys.MSG, msg);

        if (fields != null && !fields.isEmpty()) {
            merged.putAll(fields);
        }
        if (t != null) {
            merged.put(LogKeys.ERROR, t.getClass().getSimpleName() + ":" + t.getMessage());
        }

        System.out.println(formatKeyValues(merged));
    }

    private static String formatKeyValues(Map<String, Object> fields) {
        StringBuilder sb = new StringBuilder(128);
        boolean first = true;
        for (Map.Entry<String, Object> e : fields.entrySet()) {
            if (!first) sb.append(' ');
            first = false;
            sb.append(e.getKey()).append('=').append(escape(String.valueOf(e.getValue())));
        }
        return sb.toString();
    }

    private static String escape(String s) {
        // Keep it simple: quote if contains spaces or '='
        if (s.indexOf(' ') >= 0 || s.indexOf('=') >= 0) {
            return "\"" + s.replace("\"", "\\\"") + "\"";
        }
        return s;
    }
}
