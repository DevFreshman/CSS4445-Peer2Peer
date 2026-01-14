// File: src/main/java/com/ht/p2p/core/peer/SessionManager.java
package com.ht.p2p.core.peer;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionManager {
    private final ConcurrentHashMap<String, Session> byChannelId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Session> byPeerId = new ConcurrentHashMap<>();

    public void register(Session session) {
        Objects.requireNonNull(session, "session");
        byChannelId.put(session.channelId(), session);
        PeerId pid = session.peerId();
        if (pid != null) byPeerId.put(pid.value(), session);
    }

    public void unregister(Session session) {
        Objects.requireNonNull(session, "session");
        byChannelId.remove(session.channelId(), session);
        PeerId pid = session.peerId();
        if (pid != null) byPeerId.remove(pid.value(), session);
    }

    public Optional<Session> getByChannelId(String channelId) {
        return Optional.ofNullable(byChannelId.get(channelId));
    }

    public Optional<Session> getByPeerId(PeerId peerId) {
        Objects.requireNonNull(peerId, "peerId");
        return Optional.ofNullable(byPeerId.get(peerId.value()));
    }

    public void bindPeer(Session session, PeerId peerId) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(peerId, "peerId");
        byPeerId.put(peerId.value(), session);
    }

    public int size() {
        return byChannelId.size();
    }

    public Collection<Session> sessions() {
        return java.util.List.copyOf(byChannelId.values());
    }

    public Map<String, Session> snapshotByChannelId() {
        return Map.copyOf(byChannelId);
    }
}
