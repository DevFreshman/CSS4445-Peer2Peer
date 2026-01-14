// File: src/main/java/com/hoangtien/p2p/core/protocol/Header.java
package com.ht.p2p.core.protocol;

import java.time.Instant;
import java.util.Objects;

public final class Header {
    private final MessageType type;
    private final CorrelationId correlationId;
    private final Instant timestamp;

    public Header(MessageType type, CorrelationId correlationId, Instant timestamp) {
        this.type = Objects.requireNonNull(type, "type");
        this.correlationId = Objects.requireNonNull(correlationId, "correlationId");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public MessageType type() {
        return type;
    }

    public CorrelationId correlationId() {
        return correlationId;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public static Header of(MessageType type, CorrelationId correlationId, Instant timestamp) {
        return new Header(type, correlationId, timestamp);
    }

    @Override
    public String toString() {
        return "Header{type=" + type + ", correlationId=" + correlationId + ", timestamp=" + timestamp + "}";
    }
}
