// File: src/main/java/com/ht/p2p/core/peer/SessionId.java
package com.ht.p2p.core.peer;

import java.util.Objects;
import java.util.UUID;

public record SessionId(String value) {
    public SessionId {
        Objects.requireNonNull(value, "value");
        String v = value.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("sessionId must not be empty");
        value = v;
    }

    public static SessionId random() {
        return new SessionId(UUID.randomUUID().toString());
    }

    @Override
    public String toString() {
        return value;
    }
}
