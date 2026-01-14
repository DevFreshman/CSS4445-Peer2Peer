// File: src/main/java/com/ht/p2p/core/peer/Session.java
package com.ht.p2p.core.peer;

import java.time.Instant;
import java.util.Objects;

public final class Session {
    private final SessionId sessionId;
    private final String channelId;
    private final String remoteAddress;
    private final Instant createdAt;

    private volatile Instant lastSeen;
    private volatile SessionState state;
    private volatile PeerId peerId; // nullable until HELLO

    public Session(SessionId sessionId, String channelId, String remoteAddress, Instant now) {
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.channelId = Objects.requireNonNull(channelId, "channelId");
        this.remoteAddress = Objects.requireNonNull(remoteAddress, "remoteAddress");
        this.createdAt = Objects.requireNonNull(now, "now");
        this.lastSeen = now;
        this.state = SessionState.NEW;
    }

    public SessionId sessionId() {
        return sessionId;
    }

    public String channelId() {
        return channelId;
    }

    public String remoteAddress() {
        return remoteAddress;
    }

    public Instant createdAt() {
        return createdAt;
    }

    public Instant lastSeen() {
        return lastSeen;
    }

    public SessionState state() {
        return state;
    }

    public PeerId peerId() {
        return peerId;
    }

    public void activate(Instant now) {
        this.state = SessionState.ACTIVE;
        touch(now);
    }

    public void close(Instant now) {
        this.state = SessionState.CLOSED;
        touch(now);
    }

    public void touch(Instant now) {
        this.lastSeen = Objects.requireNonNull(now, "now");
    }

    public void setPeerId(PeerId peerId, Instant now) {
        this.peerId = Objects.requireNonNull(peerId, "peerId");
        touch(now);
    }
}
