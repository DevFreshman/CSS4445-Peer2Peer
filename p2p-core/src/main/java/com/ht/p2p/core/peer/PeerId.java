// File: src/main/java/com/ht/p2p/core/peer/PeerId.java
package com.ht.p2p.core.peer;

import java.util.Objects;

public record PeerId(String value) {
    public PeerId {
        Objects.requireNonNull(value, "value");
        String v = value.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("peerId must not be empty");
        value = v;
    }

    public static PeerId of(String value) {
        return new PeerId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
