// File: src/main/java/com/ht/p2p/core/protocol/Envelope.java
package com.ht.p2p.core.protocol;

import java.util.Objects;

public final class Envelope {
    private final Header header;
    private final Object payload;

    public Envelope(Header header, Object payload) {
        this.header = Objects.requireNonNull(header, "header");
        this.payload = payload; // allowed null
    }

    public Header header() {
        return header;
    }

    public Object payload() {
        return payload;
    }

    public static Envelope of(Header header, Object payload) {
        return new Envelope(header, payload);
    }

    @Override
    public String toString() {
        return "Envelope{header=" + header + ", payload=" + payload + "}";
    }
}
