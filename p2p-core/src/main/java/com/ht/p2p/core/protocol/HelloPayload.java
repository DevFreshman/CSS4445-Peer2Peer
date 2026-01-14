// File: src/main/java/com/ht/p2p/core/protocol/HelloPayload.java
package com.ht.p2p.core.protocol;

import java.util.Objects;

public record HelloPayload(String peerId) {
    public HelloPayload {
        Objects.requireNonNull(peerId, "peerId");
        String v = peerId.trim();
        if (v.isEmpty()) throw new IllegalArgumentException("peerId must not be empty");
        peerId = v;
    }

    public static HelloPayload of(String peerId) {
        return new HelloPayload(peerId);
    }
}
