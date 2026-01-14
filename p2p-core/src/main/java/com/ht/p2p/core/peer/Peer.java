// File: src/main/java/com/ht/p2p/core/peer/Peer.java
package com.ht.p2p.core.peer;

import java.time.Instant;
import java.util.Objects;

public final class Peer {
    private final PeerId peerId;
    private volatile PeerAddress address;
    private volatile Instant lastSeen;

    public Peer(PeerId peerId, PeerAddress address, Instant lastSeen) {
        this.peerId = Objects.requireNonNull(peerId, "peerId");
        this.address = Objects.requireNonNull(address, "address");
        this.lastSeen = Objects.requireNonNull(lastSeen, "lastSeen");
    }

    public PeerId peerId() {
        return peerId;
    }

    public PeerAddress address() {
        return address;
    }

    public Instant lastSeen() {
        return lastSeen;
    }

    public void update(PeerAddress address, Instant lastSeen) {
        this.address = Objects.requireNonNull(address, "address");
        this.lastSeen = Objects.requireNonNull(lastSeen, "lastSeen");
    }
}
