// File: src/main/java/com/hoangtien/p2p/core/router/routes/RouteKey.java
package com.ht.p2p.core.router.routes;

import com.ht.p2p.core.protocol.MessageType;

import java.util.Objects;

public final class RouteKey {
    private final MessageType type;

    public RouteKey(MessageType type) {
        this.type = Objects.requireNonNull(type, "type");
    }

    public MessageType type() {
        return type;
    }

    @Override
    public String toString() {
        return "RouteKey{type=" + type + "}";
    }

    @Override
    public boolean equals(Object o) {
        return (this == o) || (o instanceof RouteKey other && type == other.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }
}
