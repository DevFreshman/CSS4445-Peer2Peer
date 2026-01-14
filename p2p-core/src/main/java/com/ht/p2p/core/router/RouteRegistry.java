// File: src/main/java/com/ht/p2p/core/router/RouteRegistry.java
package com.ht.p2p.core.router;

import com.ht.p2p.core.protocol.MessageType;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class RouteRegistry {
    private final Map<MessageType, EnvelopeHandler> routes = new EnumMap<>(MessageType.class);

    public void register(MessageType type, EnvelopeHandler handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");
        EnvelopeHandler existing = routes.putIfAbsent(type, handler);
        if (existing != null) {
            throw new RouterException("Route already registered for type=" + type);
        }
    }

    public EnvelopeHandler getOrThrow(MessageType type) {
        Objects.requireNonNull(type, "type");
        EnvelopeHandler handler = routes.get(type);
        if (handler == null) {
            throw new RouterException("No route registered for type=" + type);
        }
        return handler;
    }
}
