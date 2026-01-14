// File: src/main/java/com/ht/p2p/core/peer/PeerAddress.java
package com.ht.p2p.core.peer;

import java.util.Objects;

public record PeerAddress(String host, int port) {
    public PeerAddress {
        Objects.requireNonNull(host, "host");
        String h = host.trim();
        if (h.isEmpty()) throw new IllegalArgumentException("host must not be empty");
        if (port < 0 || port > 65535) throw new IllegalArgumentException("port out of range: " + port);
        host = h;
    }

    public static PeerAddress of(String host, int port) {
        return new PeerAddress(host, port);
    }

    @Override
    public String toString() {
        return host + ":" + port;
    }
}
