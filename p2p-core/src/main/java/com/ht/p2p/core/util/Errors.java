// File: src/main/java/com/hoangtien/p2p/core/util/Errors.java
package com.ht.p2p.core.util;

import java.util.Objects;

/**
 * Tiny helpers for consistent exceptions / validation.
 */
public final class Errors {
    private Errors() {}

    public static IllegalStateException illegalState(String message) {
        return new IllegalStateException(Objects.requireNonNull(message, "message"));
    }

    public static IllegalArgumentException illegalArg(String message) {
        return new IllegalArgumentException(Objects.requireNonNull(message, "message"));
    }

    public static <T> T requireNonNull(T v, String name) {
        return Objects.requireNonNull(v, name);
    }
}
