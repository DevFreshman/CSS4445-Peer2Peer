// File: src/main/java/com/ht/p2p/core/config/Timeouts.java
package com.ht.p2p.core.config;

public final class Timeouts {
    private final long startTimeoutMs;
    private final long stopTimeoutMs;
    private final long shutdownTimeoutMs;

    // ✅ FIX: add no-arg ctor so "new Timeouts()" works
    public Timeouts() {
        this(2_000, 2_000, 2_000);
    }

    public Timeouts(long startTimeoutMs, long stopTimeoutMs, long shutdownTimeoutMs) {
        if (startTimeoutMs < 0 || stopTimeoutMs < 0 || shutdownTimeoutMs < 0) {
            throw new IllegalArgumentException("Timeouts must be >= 0");
        }
        this.startTimeoutMs = startTimeoutMs;
        this.stopTimeoutMs = stopTimeoutMs;
        this.shutdownTimeoutMs = shutdownTimeoutMs;
    }

    public long startTimeoutMs() { return startTimeoutMs; }
    public long stopTimeoutMs() { return stopTimeoutMs; }
    public long shutdownTimeoutMs() { return shutdownTimeoutMs; }

    public static Timeouts defaults() {
        return new Timeouts(2_000, 2_000, 2_000);
    }
}
