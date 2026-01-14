// File: src/main/java/com/hoangtien/p2p/core/protocol/CorrelationId.java
package com.ht.p2p.core.protocol;

import java.util.Objects;
import java.util.UUID;

public final class CorrelationId {
    private final String value;

    private CorrelationId(String value) {
        this.value = Objects.requireNonNull(value, "value");
        if (this.value.isBlank()) {
            throw new IllegalArgumentException("CorrelationId value must not be blank");
        }
    }

    public static CorrelationId random() {
        return new CorrelationId(UUID.randomUUID().toString());
    }

    public static CorrelationId of(String value) {
        return new CorrelationId(value);
    }

    public String value() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return (this == o) || (o instanceof CorrelationId other && value.equals(other.value));
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
